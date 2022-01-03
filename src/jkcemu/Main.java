/*
 * (c) 2008-2022 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Programmstart
 */

package jkcemu;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import jkcemu.audio.AudioFrm;
import jkcemu.audio.AudioRecorderFrm;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.DesktopHelper;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.FontMngr;
import jkcemu.base.GUIFactory;
import jkcemu.base.ProfileDlg;
import jkcemu.base.ScreenFrm;
import jkcemu.disk.DiskImgCreateFrm;
import jkcemu.disk.DiskImgViewFrm;
import jkcemu.disk.FloppyDiskStationFrm;
import jkcemu.image.ImageFrm;
import jkcemu.programming.assembler.CmdLineAssembler;
import jkcemu.programming.basic.CmdLineBasicCompiler;
import jkcemu.text.TextEditFrm;
import jkcemu.text.TextUtil;
import jkcemu.tools.calculator.CalculatorFrm;
import jkcemu.tools.filebrowser.FileBrowserFrm;
import jkcemu.tools.fileconverter.FileConvertFrm;
import jkcemu.tools.findfiles.FindFilesFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexedit.HexEditFrm;


public class Main
{
  public static final String APPNAME   = "JKCEMU";
  public static final String VERSION   = "0.9.8.2";
  public static final String APPINFO   = APPNAME + " Version " + VERSION;
  public static final String COPYRIGHT = "(c) 2008-2022 Jens M\u00FCller";

  public static final String FILE_GROUP_AUDIO       = "audio";
  public static final String FILE_GROUP_DEBUG_BREAK = "debug.breakpoints";
  public static final String FILE_GROUP_DEBUG_TRACE = "debug.log";
  public static final String FILE_GROUP_DISK        = "disk";
  public static final String FILE_GROUP_FC_IN       = "fileconverter.in";
  public static final String FILE_GROUP_FC_OUT      = "fileconverter.out";
  public static final String FILE_GROUP_FIND        = "find";
  public static final String FILE_GROUP_HEXDIFF     = "hexdiff";
  public static final String FILE_GROUP_HEXEDIT     = "hexedit";
  public static final String FILE_GROUP_IMAGE       = "image";
  public static final String FILE_GROUP_LABEL       = "label";
  public static final String FILE_GROUP_LOG         = "log";
  public static final String FILE_GROUP_PRINT       = "print";
  public static final String FILE_GROUP_PROFILE     = "profile";
  public static final String FILE_GROUP_PROJECT     = "project";
  public static final String FILE_GROUP_RF          = "ramfloppy";
  public static final String FILE_GROUP_ROM         = "rom";
  public static final String FILE_GROUP_SCREEN      = "screen";
  public static final String FILE_GROUP_SECTOR      = "sector";
  public static final String FILE_GROUP_SOFTWARE    = "software";
  public static final String FILE_GROUP_TEXT        = "text";
  public static final String FILE_GROUP_USB         = "usb";

  public static final String DEFAULT_PROFILE     = "standard";
  public static final String LASTDIRS_FILE       = "lastdirs.xml";
  public static final String PROP_COUNT          = "count";
  public static final String PROP_CURRENT        = "current";
  public static final String PROP_LAF_CLASSNAME  = "jkcemu.laf.classname";
  public static final String PROP_SCREEN_HEIGHT  = "jkcemu.screen.height";
  public static final String PROP_SCREEN_WIDTH   = "jkcemu.screen.width";
  public static final String PROP_SCREEN_MENUBAR = "jkcemu.screen.menubar";
  public static final String PROP_VERSION        = "jkcemu.version";
  public static final String PROP_UI_SCALE       = "jkcemu.ui.scale";
  public static final String VALUE_UI_SCALE_NONE = "none";

  public static final int WINDOW_MASK_SCREEN           = 0x01;
  public static final int WINDOW_MASK_SECONDARY_SCREEN = 0x02;
  public static final int WINDOW_MASK_JOYSTICK         = 0x04;
  public static final int WINDOW_MASK_KEYBOARD         = 0x08;

  public static enum OS { LINUX, MAC, UNIX, WINDOWS, UNKNOWN };

  private static final ThreadGroup threadGroup
			= new ThreadGroup( APPNAME + " thread group" );

  private static final String SYSPROP_UI_SCALE_VALUE = "sun.java2d.uiScale";
  private static final String SYSPROP_UI_SCALE_ENABLED
					= "sun.java2d.uiScale.enabled";

  private static final String[] iconImageResources = {
					"/images/icon/jkcemu_16x16.png",
					"/images/icon/jkcemu_20x20.png",
					"/images/icon/jkcemu_24x24.png",
					"/images/icon/jkcemu_32x32.png",
					"/images/icon/jkcemu_48x48.png" };

  private static String[] usageLines = {
	"",
	"Aufruf:",
	"  java -jar jkcemu.jar <Argumente>",
	"",
	"Argumente:",
	"  <kein Argument>              Emulator mit Profil \"standard\""
								+ " starten",
	"  <Profil>                     Emulator mit dem angegebenen Profil"
								+ " starten",
	"  -h oder --help               diese Hilfe anzeigen",
	"  -l oder --list               Liste der Profile anzeigen",
	"  -v oder --version            Versionsnummer anzeigen",
	"  --ar oder --audiorecorder    Audiorecorder starten",
	"  --as oder --assembler        Assembler starten",
	"  --as -h                      Hilfe zum Assembler anzeigen",
	"  --bc oder --basiccompiler    BASIC-Compiler starten",
	"  --bc -h                      Hilfe zum BASIC-Compiler anzeigen",
	"  --ca oder --calculator       Rechner starten",
	"  --dc oder --diskcreator      Diskettenabbilddatei erstellen",
	"  --dv oder --diskviewer       Diskettenabbilddatei-Inspector"
								+ " starten",
	"  --fb oder --filebrowser      Datei-Browser starten",
	"  --fc oder --fileconverter    Dateikonverter starten",
	"  --ff oder --findfiles        Dateisuche starten",
	"  --hd oder --hexdiff          Hex-Dateivergeicher starten",
	"  --he oder --hexeditor        Hex-Editor starten",
	"  --iv oder --imageviewer      Bildbetrachter/Bildbearbeitung"
								+ " starten",
	"  --te oder --texteditor       Texteditor starten",
	"" };

  private static OS                os       = null;
  private static Map<String,Image> images   = new HashMap<>();
  private static Properties        lastDirs = new Properties();

  private static String                   appName           = APPNAME;
  private static String                   lastDriveFileName = null;
  private static PrintWriter              consoleWriter     = null;
  private static File                     configDir         = null;
  private static File                     lastDirsFile      = null;
  private static File                     profileFile       = null;
  private static Properties               properties        = null;
  private static boolean                  screenMenuBar     = false;
  private static boolean                  prfDlgFlag        = false;
  private static boolean                  firstExec         = true;
  private static boolean                  printPageNum      = true;
  private static boolean                  printFileName     = true;
  private static int                      printFontSize     = 12;
  private static PrintRequestAttributeSet printRequestAttrs = null;
  private static volatile BaseFrm         topFrm            = null;
  private static volatile ScreenFrm       screenFrm         = null;
  private static java.util.List<Image>    iconImages        = null;

  private static volatile int activeWindowMask = 0;


  public static void main( String[] args )
  {
    setAppName( APPNAME );

    /*
     * In der Eingabeaufforderung von Windows
     * ist der DOS-Zeichensatz installiert.
     * Deshalb werden ueber System.out und System.err ausgegebene Umlaute
     * dort falsch angezeigt.
     * Ueber System.console() erfolgt die Ausgabe dagegen richtig.
     * Aus diesem Grund wird unter Windows die Console verwendet,
     * wenn sie vorhanden ist.
     */
    if( !isUnixLikeOS() ) {
      Console console = System.console();
      if( console != null ) {
	consoleWriter = console.writer();
      }
    }

    // Initialisierungen
    String subDirName  = "jkcemu";
    String baseDirName = null;
    try {
      baseDirName = TextUtil.emptyToNull( System.getenv( "APPDATA" ) );
      if( baseDirName == null ) {
	baseDirName = TextUtil.emptyToNull(
				System.getProperty( "user.home" ) );
	if( isUnixLikeOS() ) {
	  subDirName = ".jkcemu";
	}
      }
    }
    catch( SecurityException ex ) {}
    if( baseDirName != null ) {
      configDir = new File( baseDirName, subDirName );
    } else {
      configDir = new File( subDirName );
    }
    if( configDir.exists() ) {
      firstExec = false;
    }
    lastDirsFile = new File( configDir, LASTDIRS_FILE );
    if( lastDirsFile.exists() ) {
      InputStream in = null;
      try {
	in = new FileInputStream( lastDirsFile );
	lastDirs.loadFromXML( in );
      }
      catch( ClassCastException ex ) {}
      catch( Exception ex ) {}
      finally {
	EmuUtil.closeSilently( in );
      }
    }

    // Kommenadozeile auswerten
    boolean done    = false;
    String  prfName = null;
    int     argIdx  = 0;
    if( argIdx < args.length ) {
      String arg = args[ argIdx++ ];
      if( arg.equals( "-?" )
	  || arg.equalsIgnoreCase( "-h" )
	  || arg.equalsIgnoreCase( "--help" ) )
      {
	printlnOut();
	printlnOut( APPINFO );
	printlnOut( COPYRIGHT );
	for( String s : usageLines ) {
	  printlnOut( s );
	}
	exitSuccess();
      }
      else if( arg.equals( "-v" ) || arg.equalsIgnoreCase( "--version" ) ) {
	System.out.println( APPINFO );
	exitSuccess();
      }
      else if( arg.equalsIgnoreCase( "--ar" )
	       || arg.equalsIgnoreCase( "--audiorecorder" ) )
      {
	setAppName( AudioRecorderFrm.TITLE );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( AudioRecorderFrm.open() );
		  }
		} );
	done = true;
      }
      else if( arg.equals( "--as" )
	       || arg.equalsIgnoreCase( "--assembler" ) )
      {
	if( CmdLineAssembler.execute( args, argIdx ) ) {
	  exitSuccess();
	} else {
	  exitFailure();
	}
      }
      else if( arg.equals( "--bc" )
	       || arg.equalsIgnoreCase( "--basiccompiler" ) )
      {
	if( CmdLineBasicCompiler.execute( args, argIdx ) ) {
	  exitSuccess();
	} else {
	  exitFailure();
	}
      }
      else if( arg.equalsIgnoreCase( "--ca" )
	       || arg.equalsIgnoreCase( "--calculator" ) )
      {
	setAppName( CalculatorFrm.TITLE );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( CalculatorFrm.open() );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--dc" )
	       || arg.equalsIgnoreCase( "--diskcreator" ) )
      {
	setAppName( DiskImgCreateFrm.TITLE );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( DiskImgCreateFrm.open() );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--dv" )
	       || arg.equalsIgnoreCase( "--diskviewer" ) )
      {
	setAppName( DiskImgViewFrm.TITLE );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( DiskImgViewFrm.open( file ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--fb" )
	       || arg.equalsIgnoreCase( "--filebrowser" ) )
      {
	setAppName( FileBrowserFrm.TITLE );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( FileBrowserFrm.open( null ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--fc" )
	       || arg.equalsIgnoreCase( "--fileconverter" ) )
      {
	setAppName( FileConvertFrm.TITLE );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( FileConvertFrm.open( file ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--ff" )
	       || arg.equalsIgnoreCase( "--findfiles" ) )
      {
	setAppName( FindFilesFrm.TITLE );
	Path path = null;
	File file = getArgFile( args, argIdx );
	if( file != null ) {
	  try {
	    path = file.toPath().toAbsolutePath().normalize();
	    if( !Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) ) {
	      path = null;
	    }
	  }
	  catch( Exception ex ) {}
	}
	final Path path2 = path;
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install(
				FindFilesFrm.open( null, path2 ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--hd" )
	       || arg.equalsIgnoreCase( "--hexdiff" ) )
      {
	setAppName( HexDiffFrm.TITLE );
	final java.util.List<File> files = getArgFileList( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    HexDiffFrm frm = HexDiffFrm.open();
		    if( files != null ) {
		      frm.addFiles( files );
		    }
		    DesktopHelper.install( frm );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--he" )
	       || arg.equalsIgnoreCase( "--hexeditor" ) )
      {
	setAppName( HexEditFrm.TITLE );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( HexEditFrm.open( file ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--iv" )
	       || arg.equalsIgnoreCase( "--imageviewer" ) )
      {
	setAppName( ImageFrm.TITLE );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( ImageFrm.open( file ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--te" )
	       || arg.equalsIgnoreCase( "--texteditor" ) )
      {
	setAppName( TextEditFrm.TITLE );
	final File file  = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadAndApplyDefaultProfile();
		    DesktopHelper.install( TextEditFrm.open( null, file ) );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "-l" )
	       || arg.equalsIgnoreCase( "--list" ) )
      {
	prfDlgFlag = true;
      } else {
	if( arg.startsWith( "-" ) ) {
	  printlnErr();
	  printErr( APPINFO );
	  printlnErr( ":" );
	  printErr( arg );
	  printlnErr( ": Unbekannte Option" );
	  for( String s : usageLines ) {
	    printlnErr( s );
	  }
	  exitFailure();
	}
	if( !arg.isEmpty() ) {
	  prfName = arg;
	}
      }
    }

    if( !done ) {

      /*
       * Emulator starten
       *
       * Das Profil muss vor der Benutzung von GUI-Funktionen
       * geladen werden, damit darin enthaltene systemweite
       * GUI-Einstellungen wirksam werden koennen.
       */
      Properties  props     = null;
      IOException propsEx   = null;
      final File  propsFile = buildProfileFile(
				(prfName == null) && !prfDlgFlag ?
					DEFAULT_PROFILE
					: prfName );
      try {
	props = loadProperties( propsFile );
	updSysProps( props );
      }
      catch( IOException ex ) {
	propsEx = ex;
      }
      final boolean     prfDlgFlag1 = prfDlgFlag;
      final String      prfName1    = prfName;
      final Properties  props1      = props;
      final IOException propsEx1    = propsEx;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    startEmu(
			prfDlgFlag1,
			prfName1,
			propsFile,
			props1,
			propsEx1 );
		  }
		} );
    }
  }


  public static void applyProfileToFrames(
				File       file,
				Properties props,
				boolean    checkLAF,
				Frame      frameToIgnore )
  {
    profileFile = file;
    properties  = props;

    // Frames aktualisieren
    Frame[] frms = Frame.getFrames();
    for( int i = 0; i < frms.length; i++ ) {
      Frame f = frms[ i ];
      if( f != null ) {
	if( (f instanceof BaseFrm)
	    && ((frameToIgnore == null) || (frameToIgnore != f)) )
	{
	  ((BaseFrm) f).applySettings( props );
	}
      }
    }
  }


  public static boolean checkAndConfirmProfileCompatibility(
						Component  owner,
						Properties props )
  {
    boolean rv = true;
    if( props != null ) {
      if( !EmuUtil.getProperty( props, PROP_VERSION ).equals( VERSION ) ) {
	String[]    options = new String[] { "Ja", "Nein" };
	String      title   = APPNAME + "-Profil laden";
	Window      dlg     = null;
	JOptionPane pane    = new JOptionPane(
		"Das zu ladende Profil wurde mit einer anderen "
			+ APPNAME + "-Version gespeichert,\n"
			+ "deren Profilformat nicht unbedingt"
			+ " kompatibel zur dieser Version ist.\n"
			+ "Es kann somit sein, dass die im Profil"
			+ " gespeicherten Einstellungen nicht korrekt\n"
			+ "\u00FCbernommen oder dass in einigen Fenstern"
			+ " nicht alles richtig angezeigt wird.\n"
			+ "\nSollte das bei Ihnen der Fall sein, dann"
			+ " schlie\u00DFen Sie " + APPNAME
			+ " und starten erneut.\n"
			+ "Erscheint dabei dieser Dialog, so brechen Sie"
			+ " ihn bitte ab,\n"
			+ "damit " + APPNAME + " mit Standardeinstellungen"
			+ " startet.\n"
			+ "Anschlie\u00DFend stellen Sie alles nach Ihren"
			+ " W\u00FCnschen ein und speichern\n"
			+ "die Einstellungen als Profil erneut ab.\n"
			+ "\nM\u00F6chten Sie das eventuell inkompatible"
			+ " Profil jetzt laden?",
		JOptionPane.WARNING_MESSAGE );
	pane.setOptions( options );
	if( owner != null ) {
	  dlg = pane.createDialog( owner, title );
	} else {
	  dlg = pane.createDialog( title );
	}
	if( dlg != null ) {
	  updIcon( dlg );
	  dlg.setVisible( true );
	  Object selectedOption = pane.getValue();
	  if( selectedOption != null ) {
	    rv = selectedOption.equals( options[ 0 ] );
	  } else {
	    rv = false;
	  }
	}
      }
    }
    return rv;
  }


  public static void exitFailure()
  {
    System.exit( -1 );
  }


  public static void exitSuccess()
  {
    System.exit( 0 );
  }


  public static void frameCreated( BaseFrm frm )
  {
    if( topFrm == null )
      topFrm = frm;
  }


  public static String getAppName()
  {
    return appName;
  }


  public static File getConfigDir()
  {
    return configDir;
  }


  public static int getMainJavaVersion()
  {
    int    rv = -1;
    String s  = System.getProperty( "java.specification.version" );
    if( s != null ) {
      s       = s.trim();
      int len = s.length();
      int idx = 0;
      if( s.startsWith( "1." ) ) {
	idx += 2;
      }
      if( idx < len ) {
	char ch = s.charAt( idx++ );
	if( (ch >= '0') && (ch <= '9') ) {
	  int v = ch - '0';
	  while( idx < len ) {
	    ch = s.charAt( idx++ );
	    if( (ch < '0') || (ch > '9') ) {
	      break;
	    }
	    v = (v * 10) + (ch - '0');
	  }
	  rv = v;
	}
      }
    }
    return rv;
  }


  public static String[] getIconImageResources()
  {
    return iconImageResources;
  }


  public static Image getLoadedImage( Component owner, String imgName )
  {
    Image img = images.get( imgName );
    if( img == null ) {
      URL url = Main.class.getResource( imgName );
      if( url != null ) {
	if( owner == null ) {
	  owner = new Frame();
	}
	Toolkit tk = EmuUtil.getToolkit( owner );
	if( tk != null ) {
	  img = tk.createImage( url );
	  if( img != null ) {
	    try {
	      MediaTracker mt = new MediaTracker( owner );
	      mt.addImage( img, 0 );
	      mt.waitForAll();
	      if( !mt.isErrorAny() ) {
		images.put( imgName, img );
	      }
	    }
	    catch( InterruptedException ex ) {}
	  }
	}
      }
    }
    return img;
  }


  public static String getLastDriveFileName()
  {
    return lastDriveFileName;
  }


  public static File getLastDirFile( String category )
  {
    Properties savedLastDirs = readLastDirs();
    if( savedLastDirs != null ) {
      lastDirs.putAll( savedLastDirs );
    }
    File   file = null;
    String s    = lastDirs.getProperty( prepareFileCategory( category ) );
    if( s != null ) {
      if( !s.isEmpty() ) {
	file = new File( s );
      }
    }
    return file;
  }


  public static OS getOS()
  {
    synchronized( OS.class ) {
      if( os == null ) {
	String osName = System.getProperty( "os.name" );
	if( osName != null ) {
	  osName = osName.toUpperCase();
	  if( osName.indexOf( "LINUX" ) >= 0 ) {
	    os = OS.LINUX;
	  } else if( osName.indexOf( "MAC" ) >= 0 ) {
	    os = OS.MAC;
	  } else if( osName.indexOf( "WIN" ) >= 0 ) {
	    os = OS.WINDOWS;
	  }
	}
      }
      if( os == null ) {
	if( File.separatorChar == '\\' ) {
	  os = OS.WINDOWS;
	} else if( File.separatorChar == '/' ) {
	  os = OS.UNIX;
	}
      }
      if( os == null ) {
	os = OS.UNKNOWN;
      }
    }
    return os;
  }


  public static boolean getPrintFileName()
  {
    return printFileName;
  }


  public static int getPrintFontSize()
  {
    return printFontSize;
  }


  public static boolean getPrintPageNum()
  {
    return printPageNum;
  }


  public static PrintRequestAttributeSet getPrintRequestAttributeSet()
  {
    return printRequestAttrs;
  }


  public static File getProfileFile()
  {
    return profileFile;
  }


  public static Properties getProperties()
  {
    return properties;
  }


  public static String getProperty( String name )
  {
    return properties != null ? properties.getProperty( name ) : null;
  }


  public static boolean getBooleanProperty(
				String  keyword,
				boolean defaultValue )
  {
    return EmuUtil.getBooleanProperty( properties, keyword, defaultValue );
  }


  public synchronized static java.util.List<Image> getIconImages(
							Component owner )
  {
    if( iconImages == null ) {
      iconImages = new ArrayList<>();
      for( String resource : iconImageResources ) {
	URL url = Main.class.getResource( resource );
	if( url != null ) {
	  Toolkit tk = EmuUtil.getToolkit( owner );
	  if( tk != null ) {
	    Image img = tk.createImage( url );
	    if( img != null ) {
	      iconImages.add( img );
	    }
	  }
	}
      }
    }
    return iconImages;
  }


  public static Integer getIntegerProperty( String keyword )
  {
    Integer rv = null;
    if( properties != null ) {
      String s = properties.getProperty( keyword );
      if( s != null ) {
	s = s.trim();
	if( !s.isEmpty() ) {
	  try {
	    rv = Integer.valueOf( s );
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }


  public static int getIntProperty( String keyword, int defaultValue )
  {
    Integer v = getIntegerProperty( keyword );
    return v != null ? v.intValue() : defaultValue;
  }


  public static ScreenFrm getScreenFrm()
  {
    return screenFrm;
  }


  public static ThreadGroup getThreadGroup()
  {
    return threadGroup;
  }


  public static BaseFrm getTopFrm()
  {
    return topFrm;
  }


  public static boolean isEmuWindowActive()
  {
    return (activeWindowMask != 0);
  }


  public static boolean isFirstExecution()
  {
    return firstExec;
  }


  public static boolean isMacOS()
  {
    return (getOS() == OS.MAC);
  }


  public static boolean isScreenMenuBarEnabled()
  {
    return screenMenuBar;
  }


  public static boolean isTopFrm( Frame frm )
  {
    return (frm == topFrm);
  }


  public static boolean isUnixLikeOS()
  {
    return File.separatorChar == '/';
  }


  public static Properties loadProperties( File file ) throws IOException
  {
    Properties props = null;
    if( file != null ) {
      boolean     found = false;
      InputStream in    = null;
      try {
	in    = new FileInputStream( file );
	props = new Properties();
	props.loadFromXML( in );
	in.close();
	in = null;
	for( Object o : props.keySet() ) {
	  if( o.toString().startsWith( "jkcemu." ) ) {
	    found = true;
	    break;
	  }
	}
	if( !found ) {
	  throw new IOException(
		"Datei ist keine " + APPNAME + "-Profildatei" );
	}
      }
      catch( IOException ex ) {
	props = null;
	throw ex;
      }
      finally {
	EmuUtil.closeSilently( in );
      }
    }
    return props;
  }


  public static Integer parseUIScalePercentText( String text )
  {
    Integer rv = null;
    if( text != null ) {
      int pos = text.indexOf( '%' );
      if( pos >= 0 ) {
	text = text.substring( 0, pos );
      }
      try {
	int v = Integer.parseInt( text.trim() );
	if( (v >= 50) && (v <= 400) ) {
	  rv = Integer.valueOf( v );
	}
      }
      catch( NumberFormatException ex ) {}
    }
    return rv;
  }


  public static void printErr( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.print( text );
      consoleWriter.flush();
    } else {
      System.err.print( text );
    }
  }


  public static void printOut( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.print( text );
      consoleWriter.flush();
    } else {
      System.out.print( text );
    }
  }


  public static void printlnErr()
  {
    if( consoleWriter != null ) {
      consoleWriter.println();
      consoleWriter.flush();
    } else {
      System.err.println();
    }
  }


  public static void printlnErr( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.println( text );
      consoleWriter.flush();
    } else {
      System.err.println( text );
    }
  }


  public static void printlnOut()
  {
    if( consoleWriter != null ) {
      consoleWriter.println();
      consoleWriter.flush();
    } else {
      System.out.println();
    }
  }


  public static void printlnOut( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.println( text );
      consoleWriter.flush();
    } else {
      System.out.println( text );
    }
  }


  public static void putImage( String imgName, Image img )
  {
    if( (imgName != null) && (img != null) )
      images.put( imgName, img );
  }


  public static void setLastDriveFileName( String drvFileName )
  {
    lastDriveFileName = drvFileName;
  }


  public static void setLastFile( File file, String category )
  {
    if( file != null ) {
      if( !file.isDirectory() ) {
	file = file.getParentFile();
      }
      if( file != null ) {
	String path = file.getPath();
	if( path != null ) {
	  if( !path.isEmpty() ) {
	    Properties savedLastDirs = readLastDirs();
	    if( savedLastDirs != null ) {
	      lastDirs.putAll( savedLastDirs );
	    }
	    lastDirs.setProperty( prepareFileCategory( category ), path );
	    if( !lastDirs.equals( savedLastDirs )
		&& (lastDirsFile != null)
		&& configDir.exists() )
	    {
	      OutputStream out = null;
	      try {
		out = new FileOutputStream( lastDirsFile );
		lastDirs.storeToXML(
			out,
			APPNAME + " zuletzt verwendete Verzeichnisse" );
	      }
	      catch( Exception ex ) {}
	      finally {
		EmuUtil.closeSilently( out );
	      }
	    }
	  }
	}
      }
    }
  }


  public static void setPrintFileName( boolean state )
  {
    printFileName = state;
  }


  public static void setPrintFontSize( int value )
  {
    printFontSize = value;
  }


  public static void setPrintPageNum( boolean state)
  {
    printPageNum = state;
  }


  public static void setPrintRequestAttributeSet(
				PrintRequestAttributeSet attrs )
  {
    printRequestAttrs = attrs;
  }


  public static void setProfile( File file, Properties props )
  {
    if( props != null ) {
      profileFile = file;
      properties  = props;
    }
  }


  public synchronized static void setProperty( String keyword, String value )
  {
    if( properties == null ) {
      properties = new Properties();
    }
    properties.setProperty( keyword, value );
  }


  public static void setWindowActivated( int windowMask )
  {
    activeWindowMask |= windowMask;
  }


  public static void setWindowDeactivated( int windowMask )
  {
    activeWindowMask &= ~windowMask;
  }


  public static void updIcon( Window window )
  {
    getIconImages( window );
    if( !iconImages.isEmpty() ) {
      window.setIconImages( iconImages );
    }
  }


	/* --- private Methoden --- */

  private static File buildProfileFile( String prfName )
  {
    File file = null;
    if( (configDir != null) && (prfName != null) ) {
      file = new File( configDir, "prf_" + prfName + ".xml" );
    }
    return file;
  }


  private static boolean containsSysProperty( String propName )
  {
    String s = System.getProperty( propName );
    return s != null ? !s.trim().isEmpty() : false;
  }


  private static File getArgFile( String[] args, int pos )
  {
    File file = null;
    if( (pos >= 0) && (pos < args.length) ) {
      String arg = args[ pos ];
      if( arg != null ) {
	if( !arg.isEmpty() ) {
	  file = new File( arg );
	}
      }
    }
    return file;
  }


  private static java.util.List<File> getArgFileList( String[] args, int pos )
  {
    java.util.List<File> list = null;
    if( (pos >= 0) && (pos < args.length) ) {
      list = new ArrayList<>( args.length - pos );
      for( int i = pos; i < args.length; i++ ) {
	String arg = args[ i ];
	if( arg != null ) {
	  if( !arg.isEmpty() ) {
	    list.add( new File( arg ) );
	  }
	}
      }
    }
    return list;
  }


  private static void loadAndApplyDefaultProfile()
  {
    boolean lafDone = false;
    setSwingPropsToDefault();
    try {
      File file = buildProfileFile( DEFAULT_PROFILE );
      if( file != null ) {
	if( file.exists() ) {
	  properties = loadProperties( file );
	  if( properties != null ) {
	    updSysProps( properties );
	    String className = properties.getProperty( PROP_LAF_CLASSNAME );
	    if( className != null ) {
	      if( !className.isEmpty() ) {
		UIManager.setLookAndFeel( className );
		lafDone = true;
	      }
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}
    if( !lafDone ) {
      setDefaultLAF();
    }
  }


  private static String prepareFileCategory( String category )
  {
    if( category != null ) {
      if( category.isEmpty() ) {
	category = null;
      }
    }
    return category != null ? category : "*";
  }


  private static Properties readLastDirs()
  {
    Properties props = null;
    if( lastDirsFile != null ) {
      InputStream in = null;
      try {
	in    = new FileInputStream( lastDirsFile );
        props = new Properties();
	props.loadFromXML( in );
	in.close();
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilently( in );
      }
    }
    return props;
  }


  private static void setAppName( String aAppName )
  {
    appName = aAppName;

    // Unter MacOS Applikationsname setzen
    if( isMacOS() ) {
      System.setProperty( "apple.awt.application.name", aAppName );
    }
  }


  private static void setDefaultLAF()
  {
    try {
      UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
      if( screenFrm != null ) {
	SwingUtilities.updateComponentTreeUI( screenFrm );
      }
    }
    catch( Exception ex ) {}
  }


  private static void setSwingPropsToDefault()
  {
    // Bei Metal L&F fette Schrift ausschalten
    UIManager.put( "swing.boldMetal", Boolean.FALSE );
  }


  private static void startEmu(
			boolean    prfDlgFlag1,
			String     prfName,
			File       propsFile,
			Properties props,
			Exception  propsEx )
  {
    setSwingPropsToDefault();
    if( !firstExec ) {
      if( prfDlgFlag1 ) {
	// Auswahl der Profile anzeigen
	setDefaultLAF();
	ProfileDlg dlg = new ProfileDlg(
				null,
				APPNAME + " Profile",
				EmuUtil.TEXT_SELECT,
				propsFile,
				false );
	dlg.setVisible( true );
	props = dlg.getSelectedProfileProps();
	if( props == null ) {
	  exitSuccess();
	}
	updSysProps( props );
	prfName   = dlg.getSelectedProfileName();
	propsFile = dlg.getSelectedProfileFile();
	propsEx   = null;
      } else {
	// Profilkompatibilitaet pruefen
	if( !checkAndConfirmProfileCompatibility( null, props ) ) {
	  props = null;
	}
      }
    }

    // Hauptfenster
    screenFrm = new ScreenFrm( props );
    DesktopHelper.install( screenFrm );

    // beim erstmaligen Start Konfigurationsverzeichnis anlegen
    if( firstExec ) {
      if( !configDir.mkdirs() ) {
	BaseDlg.showErrorDlg(
		screenFrm,
		"Das Verzeichnis " + configDir.getPath()
			+ "\nkonnte nicht angelegt werden."
			+ "\nDadurch ist "
			+ APPNAME
			+ " nur mit einigen"
			+ " Einschr\u00E4nkungen lauff\u00E4hig."
			+ "\nInsbesondere k\u00F6nnen keine Einstellungen"
			+ " und Profile gespeichert werden." );
	configDir = null;
      }
    }

    // Erscheinungsbild einstellen
    try {
      boolean lafFromPrf   = false;
      String  lafClassName = null;
      if( props != null ) {
	lafClassName = props.getProperty( PROP_LAF_CLASSNAME );
	if( lafClassName != null ) {
	  if( lafClassName.isEmpty() ) {
	    lafClassName = null;
	  } else {
	    lafFromPrf = true;
	  }
	}
      }
      if( lafClassName == null ) {
	lafClassName = UIManager.getSystemLookAndFeelClassName();
      }
      if( lafClassName != null ) {
	UIManager.setLookAndFeel( lafClassName );
	EmuUtil.updComponentTreeUI(
				screenFrm,
				props,
				true,
				lafFromPrf,
				lafFromPrf );
      }
    }
    catch( Exception ex ) {}

    // ggf. Fehlermeldung, dass das Profil nicht geladen werden konnte
    if( !firstExec && (propsEx != null) && (prfName != null) ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "Profil \'" );
      buf.append( prfName );
      buf.append( "\' kann nicht geladen werden.\n" );
      buf.append( APPNAME );
      buf.append( " wird ohne benutzerdefinierte Einstellungen gestartet" );
      String msg = propsEx.getMessage();
      if( msg != null ) {
	msg = msg.trim();
	if( !msg.isEmpty() ) {
	  buf.append( "\n\nDetails:\n" );
	  buf.append( msg );
	}
      }
      EmuUtil.fireShowErrorDlg( screenFrm, buf.toString(), null );
    }

    // Profil anwenden
    applyProfileToFrames( propsFile, props, true, null );
    if( props != null ) {
      AudioFrm.checkEnableAudio( screenFrm, props );
      FloppyDiskStationFrm.checkOpenDisks( screenFrm, props );
    }

    // Emulations-Thread starten
    screenFrm.startEmulationThread();

    // Fenster anzeigen
    EmuUtil.showFrame( screenFrm );
  }


  private static void updSysProps( Properties props )
  {
    if( props != null ) {

      // Systemeinstellung fuer Mac
      if( isMacOS() ) {
	screenMenuBar = EmuUtil.getBooleanProperty(
					props,
					PROP_SCREEN_MENUBAR,
					false );
	System.setProperty(
		"apple.laf.useScreenMenuBar",
		Boolean.toString( screenMenuBar ) );
      }

      /*
       * UI-Skalierung
       *
       * Wenn in den Systemeigenschaften Einstellungen zur UI-Skalierung
       * zu finden sind, d.h. wenn also beim Aufruf von JKCEMU
       * welche explizit angegeben wurden
       * sollen die im Profil befindlichen Einstellungen
       * nicht angewendet werden.
       */
      if( !containsSysProperty( SYSPROP_UI_SCALE_VALUE )
	  && !containsSysProperty( SYSPROP_UI_SCALE_ENABLED )
	  && !containsSysProperty( "sun.java2d.win.uiScaleX" )
	  && !containsSysProperty( "sun.java2d.win.uiScaleY" ) )
      {
	String text = props.getProperty( PROP_UI_SCALE );
	if( text != null ) {
	  text = text.trim();
	  if( text.equalsIgnoreCase( VALUE_UI_SCALE_NONE ) ) {
	    System.setProperty( SYSPROP_UI_SCALE_ENABLED, "false" );
	  } else {
	    Integer v = parseUIScalePercentText( text );
	    if( v != null ) {
	      System.setProperty( SYSPROP_UI_SCALE_ENABLED, "true" );
	      System.setProperty(
			SYSPROP_UI_SCALE_VALUE,
			String.valueOf( (float) v / 100F ) );
	    }
	  }
	}
      }
    }

    // sonstiges
    FontMngr.putProperties( props );
    GUIFactory.putProperties( props );
  }
}
