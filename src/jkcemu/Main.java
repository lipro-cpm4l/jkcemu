/*
 * (c) 2008-2011 Jens Mueller
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
import java.util.*;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import jkcemu.base.*;


public class Main
{
  private static Map<String,Image> images    = new Hashtable<String,Image>();
  private static Map<String,File>  lastFiles = new Hashtable<String,File>();
  private static Properties        lastDirs  = new Properties();

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
  private static ScreenFrm                screenFrm         = null;
  private static java.util.List<Image>    iconImages        = null;


  public static void main( String[] args )
  {
    // Initialisierungen
    String subDirName  = "jkcemu";
    String baseDirName = null;
    try {
      baseDirName = emptyToNull( System.getenv( "APPDATA" ) );
      if( baseDirName == null ) {
	baseDirName = emptyToNull( System.getProperty( "user.home" ) );
	if( File.separatorChar == '/' ) {
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
    screenFrm    = new ScreenFrm();
    if( configDir.exists() ) {
      firstExec      = false;
      InputStream in = null;
      try {
	in = new FileInputStream( lastDirsFile );
	lastDirs.loadFromXML( in );
      }
      catch( Exception ex ) {}
      finally {
	EmuUtil.doClose( in );
      }
    } else {
      if( !configDir.mkdirs() ) {
	BasicDlg.showErrorDlg(
		screenFrm,
		"Das Verzeichnis " + configDir.getPath()
			+ "\nkonnte nicht angelegt werden."
			+ "\nDadurch ist JKCEMU nur mit einigen"
			+ " Einschr\u00E4nkungen lauff\u00E4hig."
			+ "\nInsbesondere k\u00F6nnen keine Einstellungen"
			+ " und Profile gespeichert werden." );
	configDir = null;
      }
    }

    // Icons
    addIconImage( "/images/icon/jkcemu16x16.png" );
    addIconImage( "/images/icon/jkcemu20x20.png" );
    addIconImage( "/images/icon/jkcemu32x32.png" );
    addIconImage( "/images/icon/jkcemu50x50.png" );
    updIcon( screenFrm );

    // Profil laden
    String prfName = "standard";
    if( args.length > 0 ) {
      if( !args[ 0 ].isEmpty() ) {
	prfName = args[ 0 ];
      }
    }
    Properties props = null;
    File       file  = null;
    if( configDir != null ) {
      file = new File( configDir, "prf_" + prfName + ".xml" );
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
    if( props != null ) {
      screenFrm.getFloppyDiskStationFrm().openDisks( props );
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
	  if( className.length() > 0 ) {
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


  /*
   * Da nicht sicher ist, ob sich zur Laufzeit die Anzahl der fuer die JavaVM
   * zur Verfuegung stehenden Prozessoren bzw. -kerne aendern kann,
   * wird der Wert nur einmal ermittelt und dann gemerkt.
   */
  public static int availableProcessors()
  {
    if( nProcessors == null ) {
      nProcessors = new Integer( Runtime.getRuntime().availableProcessors() );
    }
    return nProcessors.intValue();
  }


  public static File getConfigDir()
  {
    return configDir;
  }


  public static Image getImage( String imgName )
  {
    Image img = images.get( imgName );
    if( img == null ) {
      URL url = screenFrm.getClass().getResource( imgName );
      if( url != null ) {
	img = screenFrm.getToolkit().createImage( url );
	if( img != null ) {
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


  public static File getLastPathFile( String category )
  {
    if( category == null ) {
      category = "";
    }
    File file = lastFiles.get( category );
    if( file == null ) {
      String s = lastDirs.getProperty( category );
      if( s != null ) {
	if( !s.isEmpty() ) {
	  file = new File( s );
	  lastFiles.put( category, file );
	}
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


  public static String getVersion()
  {
    return "JKCEMU Version 0.8";
  }


  public static boolean isFirstExecution()
  {
    return firstExec;
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
	BasicDlg.showErrorDlg(
		screenFrm,
		"Profildatei \'" + file.getPath()
			+ "\'\nkann nicht geladen werden." );
      }
      finally {
	if( in != null ) {
	  try {
	    in.close();
	  }
	  catch( IOException ex ) {}
	}
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
      File pathFile = file.getParentFile();
      if( pathFile != null ) {
	if( category == null ) {
	  category = "";
	}
	lastFiles.put( category, pathFile );
	if( configDir.exists() ) {
	  String path = pathFile.getPath();
	  if( path != null ) {
	    lastDirs.setProperty( category, path );
	    OutputStream out = null;
	    try {
	      out = new FileOutputStream( lastDirsFile );
	      lastDirs.storeToXML( out, "JKCEMU Zuletzt verwendete Pfade" );
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


  public static void updIcon( Window window )
  {
    if( iconImages != null )
      window.setIconImages( iconImages );
  }


	/* --- private Methoden --- */

  private static void addIconImage( String imgName )
  {
    URL url = screenFrm.getClass().getResource( imgName );
    if( url != null ) {
      Image img = screenFrm.getToolkit().createImage( url );
      if( img != null ) {
	if( iconImages == null ) {
	  iconImages = new ArrayList<Image>();
	}
	iconImages.add( img );
      }
    }
  }


  private static String emptyToNull( String text )
  {
    if( text != null ) {
      if( text.isEmpty() ) {
	text = null;
      }
    }
    return text;
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
	SwingUtilities.updateComponentTreeUI( screenFrm );
      }
      catch( Exception ex ) {}
    }
  }
}
