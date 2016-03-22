/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Programmstart
 */

package jkcemu;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import jkcemu.audio.AudioRecorderFrm;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.filebrowser.*;
import jkcemu.image.ImageFrm;
import jkcemu.programming.assembler.CmdLineAssembler;
import jkcemu.programming.basic.CmdLineBasicCompiler;
import jkcemu.text.*;
import jkcemu.tools.calculator.CalculatorFrm;
import jkcemu.tools.fileconverter.FileConvertFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexedit.HexEditFrm;


public class Main
{
  public static final String APPNAME = "JKCEMU";
  public static final String VERSION = "0.9.4";
  public static final String APPINFO = APPNAME + " Version " + VERSION;

  public static final int WINDOW_MASK_SCREEN   = 0x01;
  public static final int WINDOW_MASK_JOYSTICK = 0x02;
  public static final int WINDOW_MASK_KEYBOARD = 0x04;

  public static PrintWriter consoleWriter = null;

  private static ThreadGroup threadGroup
			= new ThreadGroup( "JKCEMU thread group" );

  private static String[] iconResources = {
				"/images/icon/jkcemu16x16.png",
				"/images/icon/jkcemu20x20.png",
				"/images/icon/jkcemu32x32.png",
				"/images/icon/jkcemu50x50.png" };

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
	"  -v oder --version            Versionsnummer anzeigen",
	"  --ar oder --audiorecorder    Audio-Recorder starten",
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
	"  --iv oder --imageviewer      Bildbetrachter starten",
	"  --te oder --texteditor       Texteditor starten",
	"" };

  private static Map<String,Image> images   = new HashMap<>();
  private static Properties        lastDirs = new Properties();

  private static String                   lastDriveFileName = null;
  private static File                     configDir         = null;
  private static File                     lastDirsFile      = null;
  private static File                     profileFile       = null;
  private static Properties               properties        = null;
  private static Integer                  nProcessors       = null;
  private static boolean                  firstExec         = true;
  private static boolean                  printPageNum      = true;
  private static boolean                  printFileName     = true;
  private static int                      printFontSize     = 12;
  private static PrintRequestAttributeSet printRequestAttrs = null;
  private static BasicFrm                 topFrm            = null;
  private static ScreenFrm                screenFrm         = null;
  private static java.util.List<Image>    iconImages        = null;

  private static volatile int activeWindowMask = 0;


  public static void main( String[] args )
  {
    /*
     * In der DOS-Box von Windows ist der DOS-Zeichensatz installiert.
     * Deshalb werden ueber System.out und System.err ausgegebene Umlaute
     * dort falsch angezeigt.
     * Ueber System.console() erfolgt die Ausgabe dagegen richtig.
     * Aus diesem Grund wird unter Windows die Console verwendet,
     * wenn sie vorhanden ist.
     */
    consoleWriter = null;
    if( File.separatorChar == '\\' ) {
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
    lastDirsFile = new File( configDir, "lastdirs.xml" );
    if( lastDirsFile.exists() ) {
      InputStream in = null;
      try {
	in = new FileInputStream( lastDirsFile );
	lastDirs.loadFromXML( in );
      }
      catch( Exception ex ) {}
      finally {
	EmuUtil.doClose( in );
      }
    }

    // Kommenadozeile auswerten
    boolean done    = false;
    String  prfName = "standard";
    int    argIdx  = 0;
    if( argIdx < args.length ) {
      String arg = args[ argIdx++ ];
      if( arg.equals( "-?" )
	  || arg.equalsIgnoreCase( "-h" )
	  || arg.equalsIgnoreCase( "--help" ) )
      {
	EmuUtil.printlnOut();
	EmuUtil.printlnOut( APPINFO );
	for( String s : usageLines ) {
	  EmuUtil.printlnOut( s );
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
	properties = loadProfileAndSetLAF( prfName );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    AudioRecorderFrm.open();
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
	properties = loadProfileAndSetLAF( prfName );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    CalculatorFrm.open();
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--dc" )
	       || arg.equalsIgnoreCase( "--diskcreator" ) )
      {
	properties = loadProfileAndSetLAF( prfName );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    DiskImgCreateFrm.open();
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--dv" )
	       || arg.equalsIgnoreCase( "--diskviewer" ) )
      {
	properties = loadProfileAndSetLAF( prfName );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    DiskImgViewFrm.open( file );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--fb" )
	       || arg.equalsIgnoreCase( "--filebrowser" ) )
      {
	properties = loadProfileAndSetLAF( prfName );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    FileBrowserFrm.open( null );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--fc" )
	       || arg.equalsIgnoreCase( "--fileconverter" ) )
      {
	properties      = loadProfileAndSetLAF( prfName );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    FileConvertFrm.open( file );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--ff" )
	       || arg.equalsIgnoreCase( "--findfiles" ) )
      {
	properties = loadProfileAndSetLAF( prfName );
	Path path  = null;
	File file  = getArgFile( args, argIdx );
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
		  public void run()
		  {
		    FindFilesFrm.open( null, path2 );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--hd" )
	       || arg.equalsIgnoreCase( "--hexdiff" ) )
      {
	properties                       = loadProfileAndSetLAF( prfName );
	final java.util.List<File> files = getArgFileList( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    HexDiffFrm frm = HexDiffFrm.open();
		    if( files != null ) {
		      frm.addFiles( files );
		    }
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--he" )
	       || arg.equalsIgnoreCase( "--hexeditor" ) )
      {
	properties      = loadProfileAndSetLAF( prfName );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    HexEditFrm.open( file );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--iv" )
	       || arg.equalsIgnoreCase( "--imageviewer" ) )
      {
	properties      = loadProfileAndSetLAF( prfName );
	final File file = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    ImageFrm.open( file );
		  }
		} );
	done = true;
      }
      else if( arg.equalsIgnoreCase( "--te" )
	       || arg.equalsIgnoreCase( "--texteditor" ) )
      {
	properties = loadProfileAndSetLAF( prfName );
	final File file  = getArgFile( args, argIdx );
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    TextEditFrm.open( null, file );
		  }
		} );
	done = true;
      } else {
	if( arg.startsWith( "-" ) ) {
	  EmuUtil.printlnErr();
	  EmuUtil.printErr( APPINFO );
	  EmuUtil.printlnErr( ":" );
	  EmuUtil.printErr( arg );
	  EmuUtil.printlnErr( ": Unbekannte Option" );
	  for( String s : usageLines ) {
	    EmuUtil.printlnErr( s );
	  }
	  exitFailure();
	}
	if( !arg.isEmpty() ) {
	  prfName = arg;
	}
      }
    }

    // Emulator starten
    if( !done ) {
      screenFrm = new ScreenFrm();
      if( configDir.exists() ) {
	firstExec = false;
      } else {
	if( !configDir.mkdirs() ) {
	  BasicDlg.showErrorDlg(
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

      // Icon
      updIcon( screenFrm );

      // Profil laden
      Properties props = null;
      File       file  = buildProfileFile( prfName );
      if( file != null ) {
	if( file.exists() ) {
	  props = loadProperties( file );
	}
      }

      /*
       * Standard-Erscheinungsbild einstellen, aber nur,
       * wenn im, Profil keins zu finden ist
       */
      boolean lafState = false;
      if( props != null ) {
	String className = props.getProperty(
				  "jkcemu.lookandfeel.classname" );
	if( className != null ) {
	  if( !className.isEmpty() ) {
	    lafState = true;
	  }
	}
      }
      if( !lafState ) {
	setDefaultLAF();
      }

      // Profil anwenden
      screenFrm.setEmuThread( new EmuThread( screenFrm, props ) );
      applyProfileToFrames( file, props, true, null );
      screenFrm.lookAndFeelChanged();
      if( props != null ) {
	FloppyDiskStationFrm.getSharedInstance(
					screenFrm ).openDisks( props );
      } else {
	screenFrm.applySettings( null, screenFrm.isResizable() );
	screenFrm.pack();
	screenFrm.setScreenCentered();
      }

      // Emulations-Thread starten
      screenFrm.startEmulationThread();

      // Fenster anzeigen
      screenFrm.setVisible( true );
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

    // Erscheinungsbild
    boolean lafChanged = false;
    if( props != null ) {
      if( checkLAF ) {
	String className = props.getProperty(
				"jkcemu.lookandfeel.classname" );
	if( className != null ) {
	  if( !className.isEmpty() ) {
	    lafChanged = !EmuUtil.equalsLookAndFeel( className );
	    if( lafChanged ) {
	      try {
		UIManager.setLookAndFeel( className );
	      }
	      catch( Exception ex ) {
		props.remove( "jkcemu.lookandfeel.classname" );
	      }
	    }
	  }
	}
      }
    }

    // Frames aktualisieren
    Frame[] frms = Frame.getFrames();
    if( frms != null ) {
      for( int i = 0; i < frms.length; i++ ) {
	Frame f = frms[ i ];
	if( f != null ) {
	  if( lafChanged ) {
	    SwingUtilities.updateComponentTreeUI( f );
	    if( f instanceof BasicFrm ) {
	      ((BasicFrm) f).lookAndFeelChanged();
	    }
	    if( !f.isResizable() ) {
	      f.pack();
	    }
	  }
	  if( (f instanceof BasicFrm)
	      && ((frameToIgnore == null) || (frameToIgnore != f)) )
	  {
	    ((BasicFrm) f).applySettings( props, f.isResizable() );
	  }
	}
      }
    }
  }


  public static boolean checkQuit( Frame frm )
  {
    boolean rv = false;
    if( frm == topFrm ) {
      Frame[] frms = Frame.getFrames();
      if( frms != null ) {
	for( Frame f : frms ) {
	  if( (f != frm) && (f instanceof BasicFrm) ) {
	    ((BasicFrm) f).doClose();
	  } else {
	    f.setVisible( false );
	    f.dispose();
	  }
	}
      }
      exitSuccess();
      rv = true;
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


  public static void frameCreated( BasicFrm frm )
  {
    if( topFrm == null )
      topFrm = frm;
  }


  public static File getConfigDir()
  {
    return configDir;
  }


  public static Image getImage( Component owner, String imgName )
  {
    Image img = images.get( imgName );
    if( img == null ) {
      URL url = owner.getClass().getResource( imgName );
      if( url != null ) {
	img = owner.getToolkit().createImage( url );
	if( img != null ) {
	  try {
	    MediaTracker mt = new MediaTracker( owner);
	    mt.addImage( img, 0 );
	    mt.waitForAll();
	  }
	  catch( InterruptedException ex ) {}
	  images.put( imgName, img );
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
    File file = null;
    if( category != null ) {
      if( category.isEmpty() ) {
	category = null;
      }
    }
    if( category == null ) {
      category = "*";
    }
    String s = lastDirs.getProperty( category );
    if( s != null ) {
      if( !s.isEmpty() ) {
	file = new File( s );
      }
    }
    return file;
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
    boolean rv = defaultValue;
    if( properties != null ) {
      String s = properties.getProperty( keyword );
      if( s != null ) {
	s = s.trim().toUpperCase();
	if( s.equals( "1" )
	    || s.equals( "Y" )
	    || s.equals( "TRUE" )
	    || Boolean.parseBoolean( s ) )
	{
	  rv = true;
	}
	if( s.equals( "0" ) || s.equals( "N" ) || s.equals( "FALSE" ) ) {
	  rv = false;
	}
      }
    }
    return rv;
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


  public static boolean isFirstExecution()
  {
    return firstExec;
  }


  public static boolean isUnixLikeOS()
  {
    return File.separatorChar == '/';
  }


  public static boolean isWindowActive()
  {
    return (activeWindowMask != 0);
  }


  public static Properties loadProperties( File file )
  {
    Properties props = null;
    if( file != null ) {
      InputStream in = null;
      try {
	in    = new FileInputStream( file );
	props = new Properties();
	props.loadFromXML( in );
	in.close();
	in = null;
      }
      catch( Exception ex ) {
	props = null;
	if( screenFrm != null ) {
	  BasicDlg.showErrorDlg(
		screenFrm,
		"Profildatei \'" + file.getPath()
			+ "\'\nkann nicht geladen werden." );
	}
      }
      finally {
	EmuUtil.doClose( in );
      }
    }
    return props;
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
	    if( category != null ) {
	      if( category.isEmpty() ) {
		category = null;
	      }
	    }
	    if( category == null ) {
	      category = "*";
	    }
	    lastDirs.setProperty( category, path );
	    if( configDir.exists() ) {
	      OutputStream out = null;
	      try {
		out = new FileOutputStream( lastDirsFile );
		lastDirs.storeToXML(
			out,
			APPNAME + " zuletzt verwendete Verzeichnisse" );
	      }
	      catch( Exception ex ) {}
	      finally {
		EmuUtil.doClose( out );
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


  public static void setProperty( String keyword, String value )
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
    if( iconImages == null ) {
      iconImages = new ArrayList<>();
      for( String resource : iconResources ) {
	URL url = window.getClass().getResource( resource );
	if( url != null ) {
	  Image img = window.getToolkit().createImage( url );
	  if( img != null ) {
	    iconImages.add( img );
	  }
	}
      }
    }
    if( !iconImages.isEmpty() ) {
      window.setIconImages( iconImages );
    }
  }


	/* --- private Methoden --- */

  private static File buildProfileFile( String prfName )
  {
    File file = null;
    if( configDir != null ) {
      file = new File( configDir, "prf_" + prfName + ".xml" );
    }
    return file;
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


  private static Properties loadProfileAndSetLAF( String prfName )
  {
    Properties props = null;
    File       file  = buildProfileFile( prfName );
    if( file != null ) {
      props = loadPropertiesAndSetLAF( file );
    }
    return props;
  }


  private static Properties loadPropertiesAndSetLAF( File file )
  {
    Properties props = null;
    boolean    done  = false;
    if( file != null ) {
      if( file.exists() ) {
	props = loadProperties( file );
	if( props != null ) {
	  String cn = props.getProperty( "jkcemu.lookandfeel.classname" );
	  if( cn != null ) {
	    if( !cn.isEmpty() ) {
	      try {
		UIManager.setLookAndFeel( cn );
		done = true;
	      }
	      catch( Exception ex ) {}
	    }
	  }
	}
      }
    }
    if( !done ) {
      setDefaultLAF();
    }
    return props;
  }


  private static void setDefaultLAF()
  {
    String className = null;
    String lafText   = null;
    String osName    = System.getProperty( "os.name" );
    if( osName != null ) {
      osName = osName.toLowerCase();
      if( osName.startsWith( "windows" ) ) {
	lafText = "windows";
      }
      else if( osName.startsWith( "mac" ) ) {
	lafText = "mac";
      }
    }
    if( lafText != null ) {
      UIManager.LookAndFeelInfo[] a = UIManager.getInstalledLookAndFeels();
      if( a != null ) {
	for( int i = 0; i < a.length; i++ ) {
	  String s = a[ i ].getName();
	  if( s != null ) {
	    s = s.toLowerCase();
	    if( s.equals( lafText ) ) {
	      className = a[ i ].getClassName();
	      break;
	    }
	    if( s.startsWith( lafText ) ) {
	      className = a[ i ].getClassName();
	    }
	  }
	}
      }
    }
    if( className != null ) {
      try {
	UIManager.setLookAndFeel( className );
	if( screenFrm != null ) {
	  SwingUtilities.updateComponentTreeUI( screenFrm );
	}
      }
      catch( Exception ex ) {}
    }
  }
}

