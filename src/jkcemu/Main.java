/*
 * (c) 2008 Jens Mueller
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
  private static Map<String,File>  lastPaths = new Hashtable<String,File>();

  private static File                     profileDir        = null;
  private static File                     profileFile       = null;
  private static Properties               properties        = null;
  private static Image                    iconImage         = null;
  private static boolean                  printPageNum      = true;
  private static boolean                  printFileName     = true;
  private static int                      printFontSize     = 12;
  private static PrintRequestAttributeSet printRequestAttrs = null;
  private static ScreenFrm                screenFrm         = null;
  private static Image                    defaultIconImage  = null;
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
      profileDir = new File( baseDirName, subDirName );
    } else {
      profileDir = new File( subDirName );
    }
    screenFrm = new ScreenFrm();

    // Icons
    addIconImage( "/images/icon/jkcemu16x16.png" );
    addIconImage( "/images/icon/jkcemu20x20.png" );
    addIconImage( "/images/icon/jkcemu32x32.png" );
    addIconImage( "/images/icon/jkcemu50x50.png" );
    updIcon( screenFrm );

    // Profil laden
    String prfName = "standard";
    if( args.length > 0 ) {
      if( args[ 0 ].length() > 0 )
	prfName = args[ 0 ];
    }
    boolean done = false;
    File    file = new File( profileDir, prfName + ".prf" );
    if( file.exists() ) {
      Properties props = loadProperties( file );
      if( props != null ) {
	screenFrm.getEmuThread().applySettings( props );
	applyProfileToFrames( file, props, true, null );
	EmuUtil.applyWindowSettings(
				props,
				screenFrm,
				screenFrm.isResizable() );
	SwingUtilities.updateComponentTreeUI( screenFrm );
	done = true;
      }
    }
    if( !done ) {
      screenFrm.pack();
      screenFrm.setScreenCentered();
      screenFrm.applySettings( null, screenFrm.isResizable() );
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
    if( props != null ) {
      profileFile = file;
      properties  = props;

      // Erscheinungsbild
      boolean lafChanged = false;
      if( checkLAF ) {
	String  className = props.getProperty(
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
  }


  public static Image getImage( String imgName )
  {
    Image img = images.get( imgName );
    if( img == null ) {
      URL url = screenFrm.getClass().getResource( imgName );
      if( url != null ) {
	img = screenFrm.getToolkit().createImage( url );
	if( img != null )
	  images.put( imgName, img );
      }
    }
    return img;
  }


  public static File getLastPathFile( String category )
  {
    return lastPaths.get( category != null ? category : "" );
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


  public static File getProfileDir()
  {
    return profileDir;
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
	if( s.length() > 0 ) {
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


  public static String getVersion()
  {
    return "JKCEMU Version 0.3";
  }


  public static Properties loadProperties( File file )
  {
    Properties props = null;
    if( file != null ) {
      InputStream in = null;
      try {
	in    = new FileInputStream( file );
	props = new Properties();
	props.load( in );
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


  public static void setLastFile( File file, String category )
  {
    if( file != null ) {
      File pathFile = file.getParentFile();
      if( pathFile != null )
	lastPaths.put( category != null ? category : "", pathFile );
    }
  }


  public static void setLastFile( String fileName, String category )
  {
    setLastFile( new File( fileName ), category );
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
      if( text.length() < 1 )
	text = null;
    }
    return text;
  }
}

