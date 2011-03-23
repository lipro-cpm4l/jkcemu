/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Zugriff auf physische Geraete
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import javax.swing.JOptionPane;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class DeviceIO
{
  private static boolean libChecked = false;
  private static boolean libLoaded  = false;


	/* --- Zugriff auf einen Joystick --- */

  public static class Joystick implements Closeable
  {
    private int                 joyNum;
    private java.io.InputStream in;
    private long                xMin;
    private long                xMax;
    private long                yMin;
    private long                yMax;
    private long                xLastRaw;
    private long                yLastRaw;
    private float               xAxis;
    private float               yAxis;
    private int                 pressedBtns;
    private boolean             active;
    private byte[]              evtBuf;
    private long[]              resultBuf;

    private Joystick(
		int joyNum,
		java.io.InputStream in,
		long                xMin,
		long                xMax,
		long                yMin,
		long                yMax )
    {
      this.joyNum         = joyNum;
      this.in             = in;
      this.xMin           = xMin;
      this.xMax           = xMax;
      this.yMin           = yMin;
      this.yMax           = yMax;
      this.xLastRaw       = 0;
      this.yLastRaw       = 0;
      this.xAxis          = 0F;
      this.yAxis          = 0F;
      this.pressedBtns    = 0;
      this.active         = true;
      if( this.in != null ) {
	this.evtBuf    = new byte[ 8 ];
	this.resultBuf = null;
      } else {
	this.evtBuf    = null;
	this.resultBuf = new long[ 3 ];
      }
    }

    @Override
    public void close() throws IOException
    {
      if( this.in != null ) {
	EmuUtil.doClose( this.in );
	this.in = null;
      }
      this.active = false;
    }

    public float getXAxis()
    {
      return this.xAxis;
    }

    public float getYAxis()
    {
      return this.yAxis;
    }

    public int getPressedButtons()
    {
      return this.pressedBtns;
    }

    public boolean waitForEvent()
    {
      boolean rv = false;
      if( this.active ) {
	if( this.in != null ) {
	  try {
	    do {
	      if( EmuUtil.read(
			this.in,
			this.evtBuf ) == this.evtBuf.length )
	      {
		short evtValue = (short) (((this.evtBuf[ 5 ] << 8) & 0xFF00)
						| (this.evtBuf[ 4 ] & 0xFF));
		int   evtType    = evtBuf[ 6 ] & 0xFF;
		int   evtSubType = evtBuf[ 7 ] & 0xFF;
		if( (evtType & 0x3) == 1 ) {
		  this.pressedBtns = (int) evtValue & 0xFFFF;
		  rv               = true;
		}
		else if( (evtType & 0x3) == 2 ) {
		  if( evtSubType == 0 ) {
		    this.xAxis = (float) evtValue / (float) Short.MAX_VALUE;
		    rv         = true;
		  }
		  else if( evtSubType == 1 ) {
		    this.yAxis = (float) evtValue / (float) Short.MAX_VALUE;
		    rv         = true;
		  }
		}
	      }
	    } while( this.active && !rv );
	  }
	  catch( IOException ex ) {
	    this.active = false;
	  }
	} else {
	  if( checkUseLib() ) {
	    do {
	      if( getJoystickPos( this.joyNum, this.resultBuf ) == 0 ) {
		int  b = (int) this.resultBuf[ 0 ];
		long x = this.resultBuf[ 1 ];
		long y = this.resultBuf[ 2 ];
		if( (b == this.pressedBtns)
		    && (x == this.xLastRaw)
		    && (y == this.yLastRaw) )
		{
		  try {
		    Thread.sleep( 20 );
		  }
		  catch( InterruptedException ex ) {
		    this.active = false;
		  }
		} else {
		  this.pressedBtns = b;
		  this.xLastRaw    = x;
		  this.yLastRaw    = y;
		  this.xAxis       = adjustToFloat( x, this.xMin, this.xMax );
		  this.yAxis       = adjustToFloat( y, this.yMin, this.yMax );
		  rv               = true;
		}
	      } else {
		this.active = false;
	      }
	    } while( this.active && !rv );
	  }
	}
      }
      return rv;
    }
  };


	/* --- InputStream zum Lesen von Geraetedateien --- */

  public static class InputStream extends java.io.InputStream
  {
    private long   handle;
    private byte[] singleByteBuf;

    private InputStream( long handle ) throws IOException
    {
      this.handle        = handle;
      this.singleByteBuf = null;
    }

    @Override
    public void close() throws IOException
    {
      int errCode = closeDevice( this.handle );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
    }

    @Override
    public int read() throws IOException
    {
      if( this.singleByteBuf == null ) {
	this.singleByteBuf = new byte[ 1 ];
      }
      return read( this.singleByteBuf, 0, 1 ) == 1 ?
			((int) this.singleByteBuf[ 0 ] & 0xFF)
			: -1;
    }

    @Override
    public int read( byte[] buf ) throws IOException
    {
      return read( buf, 0, buf.length );
    }

    @Override
    public int read( byte[] buf, int offs, int len ) throws IOException
    {
      int[] lenOut  = new int[ 1 ];
      int   errCode = readDevice( this.handle, buf, offs, len, lenOut );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
      return lenOut[ 0 ] > 0 ? lenOut[ 0 ] : -1;
    }

    @Override
    public long skip( long n ) throws IOException
    {
      throw new IOException(
		"DeviceIO.InputStream.skip(...) nicht implementiert" );
    }
  };


	/* --- OutputStream zum Schreiben auf Geraetedateien --- */

  public static class OutputStream extends java.io.OutputStream
  {
    private long   handle;
    private byte[] singleByteBuf;

    private OutputStream( long handle ) throws IOException
    {
      this.handle        = handle;
      this.singleByteBuf = null;
    }

    @Override
    public void close() throws IOException
    {
      int errCode = closeDevice( this.handle );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
    }

    @Override
    public void flush() throws IOException
    {
      int errCode = flushDevice( this.handle );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
    }

    @Override
    public void write( int b ) throws IOException
    {
      if( this.singleByteBuf == null ) {
	this.singleByteBuf      = new byte[ 1 ];
	this.singleByteBuf[ 0 ] = (byte) b;
      }
      write( this.singleByteBuf, 0, 1 );
    }

    @Override
    public void write( byte[] buf ) throws IOException
    {
      write( buf, 0, buf.length );
    }

    @Override
    public void write( byte[] buf, int offs, int len ) throws IOException
    {
      if( len > 0 ) {
	int[] lenOut = new int[ 1 ];
	do {
	  int errCode = writeDevice( this.handle, buf, offs, len, lenOut );
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	  offs += lenOut[ 0 ];
	  len  -= lenOut[ 0 ];
	} while( len > 0 );
      }
    }
  };


	/* --- Klasse fuer wahlfreien Zugriff auf Geraetedateien --- */

  public static class RandomAccessDevice implements Closeable
  {
    private long             handle;
    private RandomAccessFile raf;

    private RandomAccessDevice( long handle )
    {
      this.handle = handle;
      this.raf    = null;
    }

    private RandomAccessDevice( RandomAccessFile raf )
    {
      this.handle = -1;
      this.raf    = raf;
    }

    @Override
    public void close() throws IOException
    {
      if( this.raf != null ) {
	this.raf.close();
      } else {
	int errCode = closeDevice( this.handle );
	if( errCode != 0 ) {
	  throwErrMsg( errCode );
	}
      }
    }

    public int read( byte[] buf, int offs, int len ) throws IOException
    {
      int rv = -1;
      if( this.raf != null ) {
	rv = this.raf.read( buf, offs, len );
      } else {
	int[] lenOut  = new int[ 1 ];
	int   errCode = readDevice( this.handle, buf, 0, buf.length, lenOut );
	if( errCode != 0 ) {
	  throwErrMsg( errCode );
	}
	rv = lenOut[ 0 ];
      }
      return rv;
    }

    public void seek( long pos ) throws IOException
    {
      if( this.raf != null ) {
	this.raf.seek( pos );
      } else {
	int errCode = seekDevice( this.handle, pos );
	if( errCode != 0 ) {
	  throwErrMsg( errCode );
	}
      }
    }

    public void write( byte[] buf, int offs, int len ) throws IOException
    {
      if( this.raf != null ) {
	this.raf.write( buf, offs, len );
      } else {
	if( len > 0 ) {
	  int[] lenOut = new int[ 1 ];
	  do {
	    int errCode = writeDevice( this.handle, buf, offs, len, lenOut );
	    if( errCode != 0 ) {
	      throwErrMsg( errCode );
	    }
	    offs += lenOut[ 0 ];
	    len  -= lenOut[ 0 ];
	  } while( len > 0 );
	}
      }
    }
  };


	/* --- oeffentliche Methoden --- */

  public static DeviceIO.RandomAccessDevice openDeviceForRandomAccess(
				String  deviceName,
				boolean readOnly ) throws IOException
  {
    DeviceIO.RandomAccessDevice rad = null;
    if( checkUseLib() ) {
      long[] handleOut = new long[ 1 ];
      int    errCode   = openDevice( deviceName, readOnly, true, handleOut );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
      rad = new DeviceIO.RandomAccessDevice( handleOut[ 0 ] );
    } else {
      rad = new DeviceIO.RandomAccessDevice(
		new RandomAccessFile( deviceName, readOnly ? "r" : "rw" ) );
    }
    return rad;
  }


  public static java.io.InputStream openDeviceForSequentialRead(
				String deviceName ) throws IOException
  {
    java.io.InputStream in = null;
    if( checkUseLib() ) {
      long[] handleOut = new long[ 1 ];
      int    errCode   = openDevice( deviceName, true, false, handleOut );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
      in = new DeviceIO.InputStream( handleOut[ 0 ] );
    } else {
      in = new FileInputStream( deviceName );
    }
    return in;
  }


  public static java.io.OutputStream openDeviceForSequentialWrite(
				String deviceName ) throws IOException
  {
    java.io.OutputStream out = null;
    if( checkUseLib() ) {
      long[] handleOut = new long[ 1 ];
      int    errCode   = openDevice( deviceName, false, false, handleOut );
      if( errCode != 0 ) {
	throwErrMsg( errCode );
      }
      out = new DeviceIO.OutputStream( handleOut[ 0 ] );
    } else {
      out = new FileOutputStream( deviceName );
    }
    return out;
  }


  public static DeviceIO.Joystick openJoystick( int joyNum )
  {
    DeviceIO.Joystick joystick = null;
    if( checkUseLib() ) {
      long[] resultBuf = new long[ 4 ];
      /*
       * getJoystickBounds(...) liefert auch dann noch gueltige Werte,
       * nachdem der Joystick abgezogen wurde.
       * Aus diesem Grund wird zuerst getJoystickPos(...) aufgerufen,
       * um zu testen, ob der Joystick angeschlossen ist.
       */
      if( getJoystickPos( joyNum, resultBuf ) == 0 ) {
	if( getJoystickBounds( joyNum, resultBuf ) == 0 ) {
	  joystick = new DeviceIO.Joystick(
				joyNum,
				null,
				resultBuf[ 0 ],		// Xmin
				resultBuf[ 1 ],		// Xmax
				resultBuf[ 2 ],		// Ymin
				resultBuf[ 3 ] );	// Ymax
	}
      }
    } else {
      try {
	final String[] devPrefixes = {
			"/dev/js",
			"/dev/input/js",
			"/dev/usb/js" };
	for( int i = 0; i < devPrefixes.length; i++ ) {
	  File devFile = new File( String.format(
					"%s%d",
					devPrefixes[ i ],
					joyNum ) );
	  if( devFile.exists() ) {
	    joystick = new DeviceIO.Joystick(
				joyNum, 
				new FileInputStream( devFile ),
				0,
				0,
				0,
				0 );
	    if( joystick != null ) {
	      break;
	    }
	  }
	}
      }
      catch( IOException ex ) {}
    }
    return joystick;
  }


	/* --- native Methoden --- */

  private static native int openDevice(
				String  deviceName,
				boolean readOnly,
				boolean randomAccess,
				long[]  handleOut );

  private static native int readDevice(
				long   handle,
				byte[] buf,
				int    offs,
				int    len,
				int[]  lenOut );

  private static native int writeDevice(
				long   handle,
				byte[] buf,
				int    offs,
				int    len,
				int[]  lenOut );

  private static native int    seekDevice( long handle, long pos );
  private static native int    closeDevice( long handle );
  private static native int    flushDevice( long handle );
  private static native String getErrorMsg( int errCode );
  private static native int    getJoystickBounds( int joyNum, long[] result );
  private static native int    getJoystickPos( int joyNum, long[] result );


	/* --- private Methoden --- */

  private static float adjustToFloat( long v, long vMin, long vMax )
  {
    float rv = 0F;
    if( vMax > vMin ) {
      if( v < vMin ) {
	v = vMin;
      } else if( v > vMax ) {
	v = vMax;
      }
      rv = (((float) (v - vMin) / (float) (vMax - vMin)) * 2F) - 1F;
    }
    return rv;
  }


  private synchronized static boolean checkUseLib()
  {
    if( !libChecked ) {
      if( File.separatorChar == '\\' ) {
	String arch = System.getProperty( "os.arch" );
	if( arch != null ) {
	  String libName = null;
	  if( arch.indexOf( "64" ) >= 0 ) {
	    libName = "jkcemu_win64.dll";
	  } else if( arch.indexOf( "86" ) >= 0 ) {
	    libName = "jkcemu_win32.dll";
	  }
	  if( libName != null ) {
	    File libFile = null;
	    URL  url     = DeviceIO.class.getResource( "/lib/" + libName );
	    if(  url != null ) {
	      /*
	       * Wenn die URL auf eine lokale Datei zeigt,
	       * dann diese laden
	       */
	      String protocol = url.getProtocol();
	      String fileName = url.getFile();
	      if( (protocol != null) && (fileName != null) ) {
		if( protocol.equals( "file" ) && (fileName.length() > 3) ) {
		  char ch1 = Character.toUpperCase( fileName.charAt( 1 ) );
		  if( (fileName.charAt( 0 ) == '/')
		      && ((ch1 >= 'A') && (ch1 <= 'Z'))
		      && (fileName.charAt( 2 ) == ':') )
		  {
		    fileName = fileName.substring( 1 );
		  }
		  if( fileName.indexOf( '/' ) >= 0 ) {
		    fileName.replace( '/', '\\' );
		  }
		  File file = new File( fileName );
		  if( file.exists() ) {
		    libFile = file;
		  }
		}
	      }
	    }
	    /*
	     * Wenn die Bibliothek nicht direkt von der URL geladen werden kann,
	     * dann schauen, ob sie bereits im JKCEMU-Verzeichnis steht.
	     * Wenn nicht, dann wird sie dorthin kopiert.
	     */
	    if( libFile == null ) {
	      File configDir = Main.getConfigDir();
	      if( configDir != null ) {
		libFile = new File( configDir, libName );
		if( !libFile.exists() && (url != null) ) {
		  try {
		    java.io.InputStream  in = null;
		    java.io.OutputStream out = null;
		    try {
		      in = url.openStream();
		      out = new FileOutputStream( libFile );
		      int b = in.read();
		      while( b >= 0 ) {
			out.write( b );
			b = in.read();
		      }
		      out.close();
		      out = null;
		    }
		    finally {
		      EmuUtil.doClose( in );
		      EmuUtil.doClose( out );
		    }
		  }
		  catch( Exception ex ) {
		    showLibError(
			"Die Bibliothek \'" + libFile.getPath()
				+ "\' konnte nicht angelegt werden.",
			ex );
		    libFile = null;
		  }
		  if( libFile != null ) {
		    JOptionPane.showMessageDialog(
			Main.getScreenFrm(),
			"Es wurde soeben im JKCEMU-Konfigurationsverzeichnis"
				+ " eine DLL f\u00FCr den Zugriff\n"
				+ "auf physische Diskettenlaufwerke, Joysticks"
				+ " und andere am Emulatorrechner\n"
				+ "angeschlossene Ger\u00E4te installiert.\n"
				+ "Die Benutzung dieser DLL und somit"
				+ " der Zugriff auf solche Ger\u00E4te"
				+ " ist aber erst\n"
				+ "nach Schlie\u00DFen und erneutem Starten"
				+ " des Emulators m\u00F6glich.",
			"Hinweis",
			JOptionPane.INFORMATION_MESSAGE );
		  }
		}
	      }
	    }
	    if( libFile != null ) {
	      if( !libFile.isAbsolute() ) {
		libFile = libFile.getAbsoluteFile();
	      }
	    }
	    if( libFile != null ) {
	      try {
		System.load( libFile.getPath() );
		libLoaded = true;
	      }
	      catch( Exception ex ) {
		showLibErrorLoadFailed( libFile, ex );
	      }
	      catch( UnsatisfiedLinkError e ) {
		showLibErrorLoadFailed( libFile, e );
	      }
	    }
	  }
	}
      }
      libChecked = true;
    }
    return libLoaded;
  }


  private static void showLibError( String msg, Throwable t )
  {
    StringBuilder msgBuf = new StringBuilder( 1024 );
    msgBuf.append( msg );
    msgBuf.append( "\nDadurch ist der Zugriff auf physische"
	+ " Diskettenlaufwerke, Joysticks und andere\n"
	+ "am Emulatorrechner angeschlossene Ger\u00E4te nicht"
	+ " oder nur eingeschr\u00E4nkt m\u00F6glich.\n"
	+ "Ansonsten ist JKCEMU voll funktionsf\u00E4hig.\n\n"
	+ "Das Problem l\u00E4sst sich m\u00F6glicherweise l\u00F6sen,"
	+ " indem Sie in den Einstellungen,\n"
	+ "Bereich Sonstiges, das Verzeichnis f\u00FCr Einstellungen"
	+ " und Profile l\u00F6schen\n"
	+ "und danach JKCEMU erneut starten." );

    String exMsg = t.getMessage();
    if( exMsg != null ) {
      if( !exMsg.isEmpty() ) {
	msgBuf.append( "\n\nDetaillierte Fehlermeldung:\n" );
	msgBuf.append( exMsg );
      }
    }
    JOptionPane.showMessageDialog(
			Main.getScreenFrm(),
			msgBuf.toString(),
			"Fehler",
			JOptionPane.ERROR_MESSAGE );
  }


  private static void showLibErrorLoadFailed( File libFile, Throwable t )
  {
    showLibError(
		"Die Bibliothek \'" + libFile.getPath()
			+ "\' konnte nicht geladen werden.",
		t );
  }


  private static void throwErrMsg( int errCode ) throws IOException
  {
    String errMsg = getErrorMsg( errCode );
    if( errMsg != null ) {
      if( errMsg.isEmpty() ) {
	errMsg = null;
      }
    }
    throw new IOException( errMsg != null ? errMsg : "Ein-/Ausgabefehler" );
  }
}
