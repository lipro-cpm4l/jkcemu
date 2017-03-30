/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines an einer PIO angeschlossenen Vinculum VDIP-Moduls
 *
 * Anschlussbelegung PIO Port B:
 *  B0: RXF     - Daten koennen von VDIP gelesen werden (L-aktiv)
 *  B1: TXE     - Daten koennen in VDIP geschrieben werden (L-aktiv)
 *  B2: RD      - Datenbyte von VDIP lesen (L-aktiv)
 *  B3: WR      - Datenbyte in VDIP schreiben (H-aktiv)
 *  B4: DATAREQ - VDIP-Datenmodus anfordern (L-aktiv)
 *  B5: DATAACK - VDIP ist im Datenmodus (L-aktiv)
 *  B6: RS      - RESET (L-aktiv)
 *  B7: PG      - Programmieren (L-aktiv)
 */

package jkcemu.etc;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.*;
import java.util.Calendar;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileTimesView;
import jkcemu.base.FileTimesViewFactory;
import z80emu.Z80InterruptSource;
import z80emu.Z80PIO;
import z80emu.Z80PIOPortListener;


public class VDIP implements
			Runnable,
			Z80InterruptSource,
			Z80PIOPortListener
{
  public static final String PROP_USB_DIR
			= "jkcemu.usb.memstick.directory";

  public static final String PROP_USB_FORCE_CURRENT_TIMESTAMP
			= "jkcemu.usb.memstick.force_current_timestamp";

  public static final String PROP_USB_FORCE_LOWERCASE_FILENAMES
			= "jkcemu.usb.memstick.force_lowercase_filenames";

  public static final String PROP_USB_READONLY
			= "jkcemu.usb.memstick.readonly";

  public static enum VdipErr {
			BAD_COMMAND,
			COMMAND_FAILED,
			DIR_NOT_EMPTY,
			DISK_FULL,
			FILE_OPEN,
			FILENAME_INVALID,
			INVALID,
			NO_DISK,
			NO_UPGRADE,
			READ_ONLY };


  private class IncompleteCmdException extends Exception
  {
    private IncompleteCmdException()
    {
      // leer
    }
  };


  private class VdipException extends Exception
  {
    private VdipErr vdipErr;

    private VdipException( VdipErr vdipErr )
    {
      int debugLevel = getDebugLevel();
      if( debugLevel > 0 ) {
	System.out.println( vdipErr );
      }
      if( debugLevel > 1 ) {
	printStackTrace( System.out );
      }
      this.vdipErr = vdipErr;
    }
  };


  private static enum IOCmd {
			NONE,
			CLOSE_FILE,
			DELETE_DIR,
			DELETE_FILE,
			DIR,
			OPEN_FILE_FOR_READ,
			OPEN_FILE_FOR_WRITE,
			MAKE_DIR,
			READ_FILE,
			READ_FROM_FILE,
			SECTOR_WRITE,
			SEND_DATA_FAILED,
			SET_BAUD_RATE,
			WRITE_TO_FILE };

  // Standard-Zeitstempel entsprechend VDIP Firmware: 2004-12-04 00:00:00
  private static final int DEFAULT_DATETIME_VALUE = 0x31940000;

  private FileTimesViewFactory      fileTimesViewFactory;
  private String                    title;
  private int                       debugLevel;
  private int                       bitMode;
  private boolean                   rootDirChanged;
  private boolean                   binaryMode;
  private boolean                   extendedMode;
  private boolean                   forceCurTimestamp;
  private boolean                   forceLowerCase;
  private boolean                   readOnly;
  private boolean                   readState;
  private boolean                   resetState;
  private boolean                   writeState;
  private boolean                   writeEnabled;
  private volatile boolean          fileWrite;
  private volatile File             file;
  private volatile Long             fileMillis;
  private volatile RandomAccessFile raf;
  private volatile ByteQueue        ioOut;
  private volatile IOCmd            ioCmd;
  private volatile File             ioFile;
  private volatile Long             ioFileMillis;
  private volatile String           ioFileName;
  private volatile long             ioCount;
  private volatile boolean          ioTaskEnabled;
  private volatile Thread           ioTaskThread;
  private Long                      freeDiskSpace;
  private volatile File             curDir;
  private volatile File             rootDir;
  private File                      newRootDir;
  private Object                    lockObj;
  private Calendar                  calendar;
  private byte[]                    cmdLineBytes;
  private int                       cmdLineLen;
  private int                       cmdArgPos;
  private ByteQueue                 resultQueue;
  private Z80PIO                    pio;


  public VDIP( FileTimesViewFactory fileTimesViewFactory, String title )
  {
    this.fileTimesViewFactory = fileTimesViewFactory;
    this.title                = title;
    this.debugLevel           = 0;
    this.binaryMode           = true;
    this.extendedMode         = true;
    this.forceCurTimestamp    = true;
    this.forceLowerCase       = false;
    this.readOnly             = false;
    this.readState            = false;
    this.resetState           = false;
    this.writeState           = false;
    this.writeEnabled         = false;
    this.fileWrite            = false;
    this.file                 = null;
    this.fileMillis           = null;
    this.raf                  = null;
    this.freeDiskSpace        = null;
    this.curDir               = null;
    this.rootDir              = null;
    this.rootDirChanged       = false;
    this.lockObj              = new Object();
    this.calendar             = Calendar.getInstance();
    this.cmdLineBytes         = new byte[ 256 ];
    this.cmdLineLen           = 0;
    this.cmdArgPos            = 0;
    this.resultQueue          = new ByteQueue( 1024 );
    this.pio                  = new Z80PIO( title );
    this.ioOut                = null;
    this.ioCmd                = IOCmd.NONE;
    this.ioFile               = null;
    this.ioFileMillis         = null;
    this.ioFileName           = null;
    this.ioCount              = 0;
    this.ioTaskEnabled        = true;
    this.ioTaskThread         = new Thread( this, "JKCEMU VDIP" );
    this.ioTaskThread.start();
    this.pio.addPIOPortListener( this, Z80PIO.PortInfo.B );

    String text = System.getProperty( "jkcemu.debug.usb" );
    if( text != null ) {
      try {
	this.debugLevel = Integer.parseInt( text );
      }
      catch( NumberFormatException ex ) {}
    }
  }


  public void applySettings( Properties props )
  {
    String dirText = EmuUtil.getProperty( props, PROP_USB_DIR );
    setMemStickDirectory( dirText.isEmpty() ? null : new File( dirText ) );
    setMemStickForceCurrentTimestamp(
		EmuUtil.getBooleanProperty(
			props,
			PROP_USB_FORCE_CURRENT_TIMESTAMP,
			true ) );
    setMemStickForceLowerCaseFileNames(
		EmuUtil.getBooleanProperty(
			props,
			PROP_USB_FORCE_LOWERCASE_FILENAMES,
			false ) );
    setMemStickReadOnly(
		EmuUtil.getBooleanProperty(
			props,
			PROP_USB_READONLY,
			true ) );
  }


  public void die()
  {
    this.pio.removePIOPortListener( this, Z80PIO.PortInfo.B );
    this.ioCmd         = IOCmd.NONE;
    this.ioTaskEnabled = false;
    this.ioTaskThread.interrupt();
    fireRunIOTask();
    closeFile();
  }


  public File getMemStickDirectory()
  {
    return this.rootDir;
  }


  public boolean getMemStickForceCurrentTimestamp()
  {
    return this.forceCurTimestamp;
  }


  public boolean getMemStickForceLowerCaseFileNames()
  {
    return this.forceLowerCase;
  }


  public boolean getMemStickReadOnly()
  {
    return this.readOnly;
  }


  public int read( int port )
  {
    int rv = -1;
    switch( port & 0x03 ) {
      case 0x00:
	rv = this.pio.readDataA();
	if( this.debugLevel > 1 ) {
	  System.out.printf( "VDIP: read PIO Port A: %02X\n", rv );
	}
	break;

      case 0x01:
	rv = this.pio.readDataB();
	if( this.debugLevel > 3 ) {
	  System.out.printf( "VDIP: read PIO Port B: %02X\n", rv );
	}
	break;

      case 0x02:
	rv = this.pio.readControlA();
	if( this.debugLevel > 2 ) {
	  System.out.printf( "VDIP: read PIO Ctrl A: %02X\n", rv );
	}
	break;

      case 0x03:
	rv = this.pio.readControlB();
	if( this.debugLevel > 2 ) {
	  System.out.printf( "VDIP: read PIO Ctrl B: %02X\n", rv );
	}
	break;
    }
    return rv;
  }


  public synchronized void setMemStickDirectory( File dirFile )
  {
    if( dirFile != null ) {
      if( !dirFile.isDirectory() ) {
	dirFile = null;
      }
    }
    this.newRootDir     = dirFile;
    this.rootDirChanged = true;
    fireRunIOTask();
  }


  public void setMemStickForceCurrentTimestamp( boolean state )
  {
    this.forceCurTimestamp = state;
  }


  public void setMemStickForceLowerCaseFileNames( boolean state )
  {
    this.forceLowerCase = state;
  }


  public void setMemStickReadOnly( boolean state )
  {
    this.readOnly = state;
  }


  public void write( int port, int value )
  {
    switch( port & 0x03 ) {
      case 0x00:
	if( this.debugLevel > 1 ) {
	  System.out.printf( "VDIP: write PIO Port A: %02X\n", value );
	}
	this.pio.writeDataA( value );
	break;

      case 0x01:
	{
	  if( this.debugLevel > 2 ) {
	    System.out.printf( "VDIP: write PIO Port B: %02X\n", value );
	  }
	  this.pio.writeDataB( value );

	  int     tmpValue = this.pio.fetchOutValuePortB( false );
	  boolean rdState  = ((tmpValue & 0x04) == 0);	// L-aktiv
	  boolean wrState  = ((tmpValue & 0x08) != 0);	// H-aktiv
	  boolean resState = ((tmpValue & 0x40) == 0);	// L-aktiv
	  if( resState != this.resetState ) {
	    this.readState  = rdState;
	    this.writeState = wrState;
	    this.resetState = resState;
	    if( resState ) {
	      if( this.debugLevel > 0 ) {
		System.out.println( "VDIP soft reset" );
	      }
	      resetVDIP();
	    }
	  } else {
	    if( rdState != this.readState ) {
	      this.readState = rdState;
	      if( rdState ) {
		// Beginn Leseimpuls
		synchronized( this.resultQueue ) {
		  int b = this.resultQueue.poll();
		  this.pio.putInValuePortA( b >= 0 ? b : 0, false );
		}
	      } else {
		// Ende Leseimpuls
		synchronized( this.resultQueue ) {
		  if( this.resultQueue.isEmpty() ) {
		    // keine weiteren Bytes verfuegbar
		    this.pio.putInValuePortB( 0x01, 0x01 );
		    setWriteEnabled( true );
		  } else {
		    // Weitere Bytes koennen gelesen werden.
		    this.pio.putInValuePortB( 0x00, 0x01 );
		    this.pio.strobePortB();
		  }
		}
	      }
	    }
	    if( wrState != this.writeState ) {
	      this.writeState = wrState;
	      if( wrState ) {
		// Beginn Schreibimpuls
		if( this.writeEnabled ) {
		  setWriteEnabled( false );
		  int b = this.pio.fetchOutValuePortA( false );
		  ByteQueue ioOut = this.ioOut;
		  if( ioOut != null ) {
		    ioOut.add( (byte) b );
		  } else {
		    boolean done = false;
		    if( b == '\r' ) {
		      try {
			doCmdExecute();
			this.cmdLineLen = 0;
			done            = true;
		      }
		      catch( IncompleteCmdException ex ) {
			if( this.debugLevel > 0 ) {
			  System.out.print( "    Command line incomplete"
				+ " -> wait for more bytes" );
			}
		      }
		    }
		    if( !done ) {
		      if( this.cmdLineLen < this.cmdLineBytes.length ) {
			this.cmdLineBytes[ this.cmdLineLen++ ] = (byte) b;
		      }
		      setWriteEnabled( true );
		    }
		  }
		}
	      } else {
		// Ende Schreibimpuls
		this.pio.strobePortA();
	      }
	    }
	  }
	}
	break;

      case 0x02:
	if( this.debugLevel > 2 ) {
	  System.out.printf( "VDIP: write PIO Ctrl A: %02X\n", value );
	}
	this.pio.writeControlA( value );
	break;

      case 0x03:
	if( this.debugLevel > 2 ) {
	  System.out.printf( "VDIP: write PIO Ctrl B: %02X\n", value );
	}
	this.pio.writeControlB( value );
	break;
    }
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    this.pio.appendInterruptStatusHTMLTo( buf );
  }


  @Override
  public synchronized int interruptAccept()
  {
    return this.pio.interruptAccept();
  }


  @Override
  public synchronized void interruptFinish()
  {
    if( this.pio.isInterruptAccepted() ) {
      this.pio.interruptFinish();
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.pio.isInterruptAccepted();
  }


  @Override
  public boolean isInterruptRequested()
  {
    return this.pio.isInterruptRequested();
  }


  @Override
  public void reset( boolean powerOn )
  {
    if( this.debugLevel > 0 ) {
      System.out.printf( "VDIP reset: power_on=%b\n", powerOn );
    }
    this.pio.reset( powerOn );
    this.readState  = false;
    this.writeState = false;
    resetVDIP();
  }


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo portInfo,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio)
	&& (portInfo == Z80PIO.PortInfo.B)
	&& (status == Z80PIO.Status.INTERRUPT_ENABLED) )
    {
      if( !this.resultQueue.isEmpty() ) {
	this.pio.strobePortB();
      }
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      while( this.ioTaskEnabled ) {
	synchronized( this.ioTaskThread ) {
	  try {
	    this.ioTaskThread.wait();
	  }
	  catch( InterruptedException ex ) {}
	  catch( IllegalMonitorStateException ex ) {}
	}
	if( this.ioTaskEnabled ) {
	  boolean prompt         = true;
	  boolean rootDirChanged = false;
	  File    newRootDir     = null;
	  synchronized( this ) {
	    rootDirChanged      = this.rootDirChanged;
	    newRootDir          = this.newRootDir;
	    this.rootDirChanged = false;
	    this.newRootDir     = null;
	  }
	  if( rootDirChanged ) {
	    synchronized( this.lockObj ) {
	      closeFile();
	      if( this.rootDir != null ) {
		putDiskRemoved();
	      }
	      this.freeDiskSpace = null;
	      this.rootDir       = newRootDir;
	      this.curDir        = newRootDir;
	      if( this.rootDir != null ) {
		putDiskDetected();
	      }
	    }
	  }
	  try {
	    switch( this.ioCmd ) {
	      case NONE:
		prompt = false;
		break;

	      case CLOSE_FILE:
		execCloseFileIO();
		break;

	      case DELETE_DIR:
		execDeleteDirIO();
		break;

	      case DELETE_FILE:
		execDeleteFileIO();
		break;

	      case DIR:
		execDirIO();
		break;

	      case MAKE_DIR:
		execMakeDirIO();
		break;

	      case OPEN_FILE_FOR_READ:
		execOpenFileForReadIO();
		break;

	      case OPEN_FILE_FOR_WRITE:
		execOpenFileForWriteIO();
		break;

	      case READ_FILE:
		execReadFileIO();
		break;

	      case READ_FROM_FILE:
		execReadFromFileIO();
		break;

	      case SECTOR_WRITE:
		execSectorWriteIO();
		break;

	      case SEND_DATA_FAILED:
		execSendDataFailed();
		break;

	      case SET_BAUD_RATE:
		Thread.sleep( 50 );
		break;

	      case WRITE_TO_FILE:
		execWriteToFileIO();
		break;

	      default:
		throwCommandFailed();
	    }
	  }
	  catch( VdipException ex ) {
	    putError( ex.vdipErr );
	    prompt = false;
	  }
	  doCmdFinish( prompt );
	}
      }
    }
    catch( InterruptedException ex ) {}
  }


	/* --- ueberschriebene Methoden --- */

  public String toString()
  {
    return this.title;
  }


	/* --- private Methoden --- */

  private void checkDisk() throws VdipException
  {
    if( this.rootDir == null )
      throwNoDisk();
  }


  private void closeFile()
  {
    synchronized( this.lockObj ) {
      if( this.raf != null ) {
	EmuUtil.closeSilent( this.raf );
	updLastModified();
	this.fileWrite  = false;
	this.file       = null;
	this.fileMillis = null;
	this.raf        = null;
      }
    }
  }


  private void closeReadOnlyFile()
  {
    if( !this.fileWrite )
      closeFile();
  }


  private void doCmdExecute() throws IncompleteCmdException
  {
    if( this.debugLevel > 0 ) {
      System.out.print( "  Command: " );
      int idx = 0;
      if( this.extendedMode ) {
	while( idx < this.cmdLineLen ) {
	  int b = (int) this.cmdLineBytes[ idx++ ] & 0xFF;
	  if( b == 0x20 ) {
	    break;
	  }
	  if( (b > 0x20) && (b < 0x7F) ) {
	    System.out.print( (char) b );
	  } else {
	    System.out.printf( "\\u%04X", b );
	  }
	}
      } else {
	if( idx < this.cmdLineLen ) {
	  System.out.printf(
			"0x%02X",
			(int) this.cmdLineBytes[ idx++ ] & 0xFF );
	}
      }
      if( this.binaryMode ) {
	while( idx < this.cmdLineLen ) {
	  System.out.printf( " 0x%02X", this.cmdLineBytes[ idx++ ] & 0xFF );
	}
      } else {
	while( idx < this.cmdLineLen ) {
	  System.out.print( (char) (this.cmdLineBytes[ idx++ ] & 0xFF) );
	}
      }
      System.out.println();
    }
    String extCmd   = "";
    int    shortCmd = -1;
    int    pos      = 0;
    this.cmdArgPos  = -1;
    if( this.extendedMode ) {
      StringBuilder buf = new StringBuilder();
      while( pos < this.cmdLineLen ) {
	int b = (int) this.cmdLineBytes[ pos++ ] & 0xFF;
	if( b == 0x20 ) {
	  break;
	}
	buf.append( (char) b );
      }
      extCmd = buf.toString();
      if( pos < this.cmdLineLen ) {
	this.cmdArgPos = pos;
      }
    } else {
      if( pos < this.cmdLineLen ) {
	shortCmd = (int) this.cmdLineBytes[ pos++ ] & 0xFF;
        if( pos < this.cmdLineLen ) {
	  if( (this.cmdLineBytes[ pos ] == (byte) 0x20)
	      && ((pos + 1) < this.cmdLineLen) )
	  {
	    this.cmdArgPos = pos + 1;
	  } else {
	    shortCmd = -1;
	  }
	}
      }
    }
    try {
      if( this.cmdLineLen == 0 ) {
	// leeres Kommando
	checkDisk();
	doCmdFinish( true );
      } else if( (shortCmd == 0x01) || extCmd.equals( "DIR" ) ) {
	execDir();
      } else if( (shortCmd == 0x02) || extCmd.equals( "CD" ) ) {
	execChangeDir();
      } else if( (shortCmd == 0x03) || extCmd.equals( "SD" ) ) {
	execSectorDump();
      } else if( (shortCmd == 0x04) || extCmd.equals( "RD" ) ) {
	execReadFile();
      } else if( (shortCmd == 0x05) || extCmd.equals( "DLD" ) ) {
	execDeleteDir();
      } else if( (shortCmd == 0x06) || extCmd.equals( "MKD" ) ) {
	execMakeDir();
      } else if( (shortCmd == 0x07) || extCmd.equals( "DLF" ) ) {
	execDeleteFile();
      } else if( (shortCmd == 0x08) || extCmd.equals( "WRF" ) ) {
	execWriteToFile();
      } else if( (shortCmd == 0x09) || extCmd.equals( "OPW" ) ) {
	execOpenFileForWrite();
      } else if( (shortCmd == 0x0A) || extCmd.equals( "CLF" ) ) {
	execCloseFile();
      } else if( (shortCmd == 0x0B) || extCmd.equals( "RDF" ) ) {
	execReadFromFile();
      } else if( (shortCmd == 0x0C) || extCmd.equals( "REN" ) ) {
	execRename();
      } else if( (shortCmd == 0x0E) || extCmd.equals( "OPR" ) ) {
	execOpenFileForRead();
      } else if( (shortCmd == 0x0F) || extCmd.equals( "IDD" ) ) {
	execIdentifyDiskDrive( false );
      } else if( (shortCmd == 0x12) || extCmd.equals( "FS" ) ) {
	execFreeSpace( false );
      } else if( (shortCmd == 0x13) || extCmd.equals( "FWV" ) ) {
	putString( "\rMAIN 03.69VDAPF\rRPRG 1.00R\r" );
	doCmdFinish( true );
      } else if( (shortCmd == 0x14) || extCmd.equals( "SBD" ) ) {
	putPrompt();
	startIOTask( IOCmd.SET_BAUD_RATE );
      } else if( (shortCmd == 0x15) || extCmd.equals( "SUD" ) ) {
	execSuspendDisk();
      } else if( (shortCmd == 0x16) || extCmd.equals( "WKD" ) ) {
	// Wake Disk: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x17) || extCmd.equals( "SUM" ) ) {
	// Suspend Monitor: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x18) || extCmd.equals( "FBD" ) ) {
	// Set Baud Rate: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x19) || extCmd.equals( "FMC" ) ) {
	// Set Modem Control: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x1A) || extCmd.equals( "FSD" ) ) {
	// Set Data Characteristics: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x1B) || extCmd.equals( "FFC" ) ) {
	// Set Flow Control: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x1C) || extCmd.equals( "FGM" ) ) {
	// Get Modem Status: 2 Nullbytes zurueckliefern
	execGetNullBytes( 2 );
      } else if( (shortCmd == 0x22) || extCmd.equals( "FSL" ) ) {
	// Set Latency Timer: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x23) || extCmd.equals( "FSB" ) ) {
	execSetBitMode();
      } else if( (shortCmd == 0x24) || extCmd.equals( "FGB" ) ) {
	execGetBitMode();
      } else if( (shortCmd == 0x28) || extCmd.equals( "SEK" ) ) {
	execSeek();
      } else if( (shortCmd == 0x29) || extCmd.equals( "IOR" ) ) {
	// IO-Byte lesen: so tun, als ob ein Nullbyte gelesen wurde
	execGetNullBytes( 1 );
      } else if( (shortCmd == 0x2A) || extCmd.equals( "IOW" ) ) {
	// IO-Byte schreiben: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x2B) || extCmd.equals( "QP1" ) ) {
	execQueryPort( 1 );
      } else if( (shortCmd == 0x2C) || extCmd.equals( "QP2" ) ) {
	execQueryPort( 2 );
      } else if( (shortCmd == 0x2D) || extCmd.equals( "DSN" ) ) {
	execDiskSerialNumber();
      } else if( (shortCmd == 0x2E) || extCmd.equals( "DVL" ) ) {
	execDiskVolumeLabel();
      } else if( (shortCmd == 0x2F) || extCmd.equals( "DIRT" ) ) {
	execDirT();
      } else if( (shortCmd == 0x81) || extCmd.equals( "PGS" ) ) {
	// Get Printer Status: Nullbyte (kein Drucker) zurueckliefern
	execGetNullBytes( 1 );
      } else if( (shortCmd == 0x82) || extCmd.equals( "PSR" ) ) {
	// Printer Soft Reset: nichts tun
	doCmdFinish( true );
      } else if( (shortCmd == 0x83) || extCmd.equals( "DSD" ) ) {
	execDeviceSendData();
      } else if( (shortCmd == 0x84) || extCmd.equals( "DRD" ) ) {
	execDeviceReadData();
      } else if( (shortCmd == 0x85) || extCmd.equals( "QD" ) ) {
	execQueryDevice();
      } else if( (shortCmd == 0x86) || extCmd.equals( "SC" ) ) {
	// Set Current: nur Geraetenummer pruefen
	execCheckDeviceNum();
      } else if( (shortCmd == 0x87) || extCmd.equals( "SF" ) ) {
	// Set Device as an FTDI device: nur Geraetenummer pruefen
	execCheckDeviceNum();
      } else if( (shortCmd == 0x90) || extCmd.equals( "IPA" ) ) {
	this.binaryMode = false;
	doCmdFinish( true );
      } else if( (shortCmd == 0x91) || extCmd.equals( "IPH" ) ) {
	this.binaryMode = true;
	doCmdFinish( true );
      } else if( (shortCmd == 0x92) || extCmd.equals( "SW" ) ) {
	startIOTask( IOCmd.SECTOR_WRITE );
      } else if( (shortCmd == 0x93) || extCmd.equals( "FSE" ) ) {
	execFreeSpace( true );
      } else if( (shortCmd == 0x94) || extCmd.equals( "IDDE" ) ) {
	execIdentifyDiskDrive( true );
      } else if( (shortCmd == 0x95) || extCmd.equals( "FWU" ) ) {
	// Firmware Upgrade: in der Emulation nicht moeglich
	throw new VdipException( VdipErr.NO_UPGRADE );
      } else if( (shortCmd == 0x9A) || extCmd.equals( "SSU" ) ) {
	execDeviceSendSetupData();
      } else if( (shortCmd == 'E') || extCmd.equals( "E" ) ) {
	putString( "E\r" );
	doCmdFinish( false );
      } else if( (shortCmd == 'e') || extCmd.equals( "e" ) ) {
	putString( "e\r" );
	doCmdFinish( false );
      } else {
	/*
	 * Die Umschaltung zwischen den beiden Command Sets
	 * ist in beiden Modi mit jeweils beiden Syntax-Varianten moeglich.
	 */
	if( ((this.cmdLineLen == 1)
			&& (this.cmdLineBytes[ 0 ] == (byte) 0x10))
	    || ((this.cmdLineLen > 1)
			&& (this.cmdLineBytes[ 0 ] == (byte) 0x10)
			&& (this.cmdLineBytes[ 1 ] == (byte) 0x20))
	    || ((this.cmdLineLen == 3)
			&& (this.cmdLineBytes[ 0 ] == (byte) 'S')
			&& (this.cmdLineBytes[ 1 ] == (byte) 'C')
			&& (this.cmdLineBytes[ 2 ] == (byte) 'S'))
	    || ((this.cmdLineLen > 3)
			&& (this.cmdLineBytes[ 0 ] == (byte) 'S')
			&& (this.cmdLineBytes[ 1 ] == (byte) 'C')
			&& (this.cmdLineBytes[ 2 ] == (byte) 'S')
			&& (this.cmdLineBytes[ 2 ] == (byte) 0x20)) )
	{
	  this.extendedMode = false;
	  doCmdFinish( true );
	} else if( ((this.cmdLineLen == 1)
			&& (this.cmdLineBytes[ 0 ] == (byte) 0x11))
		|| ((this.cmdLineLen > 1)
			&& (this.cmdLineBytes[ 0 ] == (byte) 0x11)
			&& (this.cmdLineBytes[ 1 ] == (byte) 0x20))
		|| ((this.cmdLineLen == 3)
			&& (this.cmdLineBytes[ 0 ] == (byte) 'E')
			&& (this.cmdLineBytes[ 1 ] == (byte) 'C')
			&& (this.cmdLineBytes[ 2 ] == (byte) 'S'))
		|| ((this.cmdLineLen > 3)
			&& (this.cmdLineBytes[ 0 ] == (byte) 'E')
			&& (this.cmdLineBytes[ 1 ] == (byte) 'C')
			&& (this.cmdLineBytes[ 2 ] == (byte) 'S')
			&& (this.cmdLineBytes[ 2 ] == (byte) 0x20)) )
	{
	  this.extendedMode = true;
	  doCmdFinish( true );
	} else {
	  throwBadCommand();
	}
      }
    }
    catch( VdipException ex ) {
      putError( ex.vdipErr );
      doCmdFinish( false );
    }
  }


  private void doCmdFinish( boolean prompt )
  {
    if( prompt ) {
      putPrompt();
    }
    this.ioCmd = IOCmd.NONE;
    setWriteEnabled( true );
  }


  private void execChangeDir() throws VdipException
  {
    String dirName = null;
    if( this.cmdArgPos >= 0 ) {
      // Sonderfall "CD \" bzw. "CD /" behandlen
      while( this.cmdArgPos < this.cmdLineLen ) {
	if( this.cmdLineBytes[ this.cmdArgPos ] != (byte) 0x20 ) {
	  break;
	}
	this.cmdArgPos++;
      }
      if( (this.cmdArgPos + 1) == this.cmdLineLen ) {
	int b = (int) this.cmdLineBytes[ this.cmdArgPos ] & 0xFF;
	if( (b == '/') || (b == '\\') ) {
	  dirName = "\\";
	}
      }
    }
    if( dirName == null ) {
      dirName = nextArgFileName( true );
    }
    synchronized( this.lockObj ) {
      File curDir  = this.curDir;
      File rootDir = this.rootDir;
      if( (curDir == null) || (rootDir == null) ) {
	throwNoDisk();
      }
      if( this.raf != null ) {
	throwFileOpen();
      }
      boolean done = false;
      if( dirName != null ) {
	if( dirName.equals( "\\" ) ) {
	  this.curDir = rootDir;
	  done        = true;
	}
      }
      if( !done ) {
	File subDir = findFile( dirName );
	if( subDir == null ) {
	  throwCommandFailed();
	}
	if( !curDir.isDirectory() ) {
	  throwInvalid();
	}
	this.curDir = subDir;
      }
    }
    doCmdFinish( true );
  }


  private void execCheckDeviceNum() throws
					IncompleteCmdException,
					VdipException
  {
    long device = nextArgByte();
    if( (device < 0) || (device > 15) ) {
      throwCommandFailed();
    }
    doCmdFinish( true );
  }


  /*
   * Laut Dokumentation muss der Name der zu schliessenden Datei
   * uebergeben werden.
   * In der Praxis kommt aber kein Fehler,
   * wenn kein Dateiname uebergeben wurde,
   * der uebergebene Dateiname nicht stimmt
   * oder gar keine Datei offen ist.
   * Es wird das tatsaechliche Verhalten emuliert.
   */
  private void execCloseFile() throws VdipException
  {
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf != null ) {
	startIOTask( IOCmd.CLOSE_FILE );
      } else {
	doCmdFinish( true );
      }
    }
  }


  private void execCloseFileIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      if( this.raf != null ) {
	if( !this.readOnly && this.fileWrite ) {
	  // Datei am aktuellen Dateizeiger abschneiden
	  try {
	    long len = this.raf.length();
	    if( len > 0 ) {
	      long pos = this.raf.getFilePointer();
	      if( (pos >= 0) && (pos < len) && (pos != len) ) {
		this.raf.setLength( pos );
	      }
	    }
	  }
	  catch( IOException ex ) {}
	}
	try {
	  this.raf.close();
	  updLastModified();
	  this.raf        = null;
	  this.file       = null;
	  this.fileMillis = null;
	  this.fileWrite  = false;
	}
	catch( IOException ex ) {}
      }
    }
  }


  private void execDeleteDir() throws VdipException
  {
    String dirName = nextArgFileName( false );
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf != null ) {
	throwFileOpen();
      }
      File file = findFile( dirName );
      if( file != null ) {
	if( this.readOnly ) {
	  /*
	   * Fehlermeldung "Read Only" ist fuer dieses Kommando
	   * nicht dokumentiert, deshalb "Command Failed".
	   */
	  throwCommandFailed();
	}
	if( file.isDirectory() ) {
	  startIOTask( IOCmd.DELETE_DIR, file );
	} else {
	  throwInvalid();
	}
      } else {
	throwCommandFailed();
      }
    }
  }


  private void execDeleteDirIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      closeReadOnlyFile();
      File file = this.ioFile;
      if( file == null ) {
	throwCommandFailed();
      }
      if( this.readOnly || !file.isDirectory() ) {
	/*
	 * Fehlermeldung "Read Only" ist fuer dieses Kommando
	 * nicht dokumentiert, deshalb "Command Failed".
	 */
	throwCommandFailed();
      }
      String[] items = file.list();
      if( items != null ) {
	for( String item : items ) {
	  if( item != null ) {
	    if( !item.equals( "." ) && !item.equals( ".." ) ) {
	      throw new VdipException( VdipErr.DIR_NOT_EMPTY );
	    }
	  }
	}
      }
      if( !file.delete() ) {
	throwCommandFailed();
      }
    }
  }


  private void execDeleteFile() throws VdipException
  {
    String fName = nextArgFileName( false );
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf != null ) {
	throwFileOpen();
      }
      File file = findFile( fName );
      if( file != null ) {
	if( this.readOnly || !file.canWrite() ) {
	  throwReadOnly();
	}
	if( file.isFile() ) {
	  startIOTask( IOCmd.DELETE_FILE, file );
	} else {
	  throwInvalid();
	}
      } else {
	throwCommandFailed();
      }
    }
  }


  private void execDeleteFileIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      closeReadOnlyFile();
      File file = this.ioFile;
      if( file == null ) {
	throwCommandFailed();
      }
      if( !file.isFile() ) {
	throwCommandFailed();
      }
      if( this.readOnly || !file.canWrite() ) {
	throwReadOnly();
      }
      if( !file.delete() ) {
	throwCommandFailed();
      }
    }
  }


  private void execDeviceReadData() throws VdipException
  {
    putNumber( 0, 1 );
    putResultByte( '\r' );
    throwCommandFailed();
  }


  private void execDeviceSendData() throws
					IncompleteCmdException,
					VdipException
  {
    long nBytes = nextArgByte();
    if( nBytes > 0 ) {
      startIOTask( IOCmd.SEND_DATA_FAILED, nBytes );
    } else {
      throwCommandFailed();
    }
  }


  private void execDeviceSendSetupData() throws
					IncompleteCmdException,
					VdipException
  {
    /*
     * QWord ist das Datenpaket (8 Bytes),
     * welches in den letzten Bytes die Anzahl der noch nachfolgend
     * extra zu lesenden Bytes enthaelt (Little Endian)
     */
    long v = nextArgQWord();
    long n = (((v << 8) & 0xFF00L) | ((v >> 8) & 0x00FFL));
    if( n > 0 ) {
      startIOTask( IOCmd.SEND_DATA_FAILED, n );
    } else {
      throwCommandFailed();
    }
  }


  /*
   * Kommando DIR:
   *   Entgegen der Dokumentation kommt bei einer geoeffneten Datei
   *   kein Fehler.
   */
  private void execDir() throws VdipException
  {
    String fName = null;
    if( (this.cmdArgPos >= 2) && (this.cmdArgPos < this.cmdLineLen) ) {
      fName = nextArgFileName( true );
      if( fName == null ) {
	putResultByte( '\r' );		// Leerzeile vor Fehlermeldung
	throwCommandFailed();
      }
    }
    synchronized( this.lockObj ) {
      checkDisk();
      File file = null;
      if( fName != null ) {
	file = findFile( fName );
	if( file == null ) {
	  putResultByte( '\r' );	// Leerzeile vor Fehlermeldung
	  throwCommandFailed();
	}
      }
      startIOTask( IOCmd.DIR, file, fName, null, 0 );
    }
  }


  private void execDirIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      putResultByte( '\r' );	// Leerzeile, auch bei Fehlermeldung
      closeReadOnlyFile();
      File file = this.ioFile;
      if( file != null ) {
	if( this.ioFileName == null ) {
	  this.ioFileName = file.getName();
	}
	putString( this.ioFileName );
	if( file.isFile() ) {
	  long fSize = file.length();
	  if( fSize > 0xFFFFFFFFL ) {
	    fSize = 0xFFFFFFFFL;
	  }
	  putResultByte( '\u0020' );
	  putNumber( fSize, 4 );
	}
	putResultByte( '\r' );
      } else {
	File curDir  = this.curDir;
	File rootDir = this.rootDir;
	if( (rootDir == null) || (curDir == null) ) {
	  throwCommandFailed();
	}
	File[] files = curDir.listFiles();
	if( files != null ) {
	  // ggf. zuerst den Punkt und die zwei Punkte ausgeben
	  if( !curDir.equals( rootDir ) ) {
	    putString( ". DIR\r" );
	    putString( ".. DIR\r" );
	  }
	  for( File f : files ) {
	    String fName = f.getName();
	    if( fName != null ) {
	      if( isValidFileName( fName )
		  && !fName.equals( "." )
		  && !fName.equals( ".." ) )
	      {
		fName = fName.toUpperCase();
		if( f.isFile() ) {
		  putString( fName );
		  putResultByte( '\r' );
		} else if( f.isDirectory() ) {
		  putString( fName );
		  putString( " DIR\r" );
		}
	      }
	    }
	  }
	}
      }
    }
  }


  /*
   * Kommando DIRT:
   *   Entgegen der Dokumentation kommt bei einer geoeffneten Datei
   *   kein Fehler.
   */
  private void execDirT() throws VdipException
  {
    String fName = nextArgFileName( true );
    synchronized( this.lockObj ) {
      putResultByte( '\r' );	// Leerzeile, auch bei Fehlermeldung
      checkDisk();
      File file = findFile( fName );
      if( file == null ) {
	throwCommandFailed();
      }
      long creationDateTime     = DEFAULT_DATETIME_VALUE;
      long lastAccessDateTime   = DEFAULT_DATETIME_VALUE;
      long lastModifiedDateTime = DEFAULT_DATETIME_VALUE;
      FileTimesView ftv = this.fileTimesViewFactory.getFileTimesView( file );
      if( ftv != null ) {
	Long v = ftv.getLastModifiedMillis();
	if( v != null ) {
	  lastModifiedDateTime = getDateTimeByMillis( v.longValue() );
	  /*
	   * Wenn die anderen beiden Zeitstempel nicht ermittelt
	   * werden konnten,
	   * sollen sie den Wert der letzten Aenderung haben.
	   */
	  lastAccessDateTime   = lastModifiedDateTime;
	  lastModifiedDateTime = lastModifiedDateTime;
	}
	v = ftv.getCreationMillis();
	if( v != null ) {
	  creationDateTime = getDateTimeByMillis( v.longValue() );
	}
	v = ftv.getLastAccessMillis();
	if( v != null ) {
	  lastAccessDateTime = getDateTimeByMillis( v.longValue() );
	}
      }
      putString( fName );
      putResultByte( '\u0020' );
      putNumber( creationDateTime, 4 );
      putNumber( lastAccessDateTime >> 16, 2 );
      putNumber( lastModifiedDateTime, 4 );
      putResultByte( '\r' );
    }
    doCmdFinish( true );
  }


  private void execDiskSerialNumber() throws VdipException
  {
    File rootDir = this.rootDir;
    if( rootDir == null ) {
      throwNoDisk();
    }
    putNumber( rootDir.hashCode(), 4 );
    putResultByte( '\r' );
    doCmdFinish( true );
  }


  private void execDiskVolumeLabel() throws VdipException
  {
    checkDisk();
    putString( "NO NAME    \r" );
    doCmdFinish( true );
  }


  private void execFreeSpace( boolean extendedOutput ) throws VdipException
  {
    long    freeSpace = 0;
    boolean found     = false;
    File    file      = this.curDir;
    if( file == null ) {
      file = this.rootDir;
    }
    if( file == null ) {
      throwNoDisk();
    }
    while( file != null ) {
      long v1 = file.getFreeSpace();
      long v2 = file.getUsableSpace();
      if( v1 >= 0 ) {
	if( v2 >= 0 ) {
	  freeSpace = Math.min( v1, v2 );
	} else {
	  freeSpace = v1;
	}
      } else {
	if( v2 >= 0 ) {
	  freeSpace = v2;
	}
      }
      if( (freeSpace > 0) || (file.getTotalSpace() > 0) ) {
	found = true;
	break;
      }
      file = file.getParentFile();
    }
    if( !found ) {
      throwCommandFailed();
    }
    this.freeDiskSpace = freeSpace;
    if( extendedOutput ) {
      if( freeSpace > 0xFFFFFFFFFFFFL ) {
	freeSpace = 0xFFFFFFFFFFFFL;
      }
      putNumber( freeSpace, 6 );
    } else {
      if( freeSpace > 0xFFFFFFFFL ) {
	freeSpace = 0xFFFFFFFFL;
      }
      putNumber( freeSpace, 4 );
    }
    putResultByte( '\r' );
    doCmdFinish( true );
  }


  private void execGetBitMode()
  {
    putNumber( this.bitMode, 1 );
    putResultByte( '\r' );
    doCmdFinish( true );
  }


  private void execGetNullBytes( int n )
  {
    for( int i = 0; i < n; i++ ) {
      putNumber( 0, 1 );
    }
    putResultByte( '\r' );
    doCmdFinish( true );
  }


  private void execIdentifyDiskDrive( boolean extendedOutput )
						throws VdipException
  {
    long capacity = 0;
    File file     = this.curDir;
    if( file == null ) {
      file = this.rootDir;
    }
    if( file == null ) {
      throwNoDisk();
    }
    while( file != null ) {
      capacity = file.getTotalSpace();
      if( capacity > 0 ) {
	break;
      }
      file = file.getParentFile();
    }
    putString( "\r"
	+ "USB VID = $05DC\r"
	+ "USB PID = $A560\r"
	+ "Vendor Id = JKCEMU  \r"		// 8 Zeichen
	+ "Product Id = Virtual Disk    \r"	// 16 Zeichen
	+ "Revision Level = 1\r"
	+ "I/F = SCSI\r"
	+ "FAT32\r"
	+ "Bytes/Sector = $0200\r"
	+ "Bytes/Cluster = $001000\r"
	+ "Capacity = $" );
    if( extendedOutput ) {
      if( capacity > 0xFFFFFFFFFFFFL ) {
	capacity = 0xFFFFFFFFFFFFL;
      }
      putString( String.format(
			"%02X%02X",
			(capacity >> 40) & 0xFF,
			(capacity >> 32) & 0xFF ) );
    } else {
      if( capacity > 0xFFFFFFFFL ) {
	capacity = 0xFFFFFFFFL;
      }
    }
    putString( String.format(
			"%02X%02X%02X%02X",
			(capacity >> 24) & 0xFF,
			(capacity >> 16) & 0xFF,
			(capacity >> 8) & 0xFF,
			capacity & 0xFF ) );
    putString( " Bytes\r"
	+ "Free Space = $" );
    Long freeDiskSpace = this.freeDiskSpace;
    if( freeDiskSpace != null ) {
      long freeSpace = freeDiskSpace.longValue();
      if( freeSpace < 0 ) {
	freeSpace = 0;
      }
      if( freeSpace > capacity ) {
	freeSpace = capacity;
      }
      if( extendedOutput ) {
	putString( String.format(
			"%02X%02X",
			(freeSpace >> 40) & 0xFF,
			(freeSpace >> 32) & 0xFF ) );
      }
      putString( String.format(
			"%02X%02X%02X%02X",
			(freeSpace >> 24) & 0xFF,
			(freeSpace >> 16) & 0xFF,
			(freeSpace >> 8) & 0xFF,
			freeSpace & 0xFF ) );
    }
    putString( " Bytes\r\r" );
    doCmdFinish( true );
  }


  private void execMakeDir() throws IncompleteCmdException, VdipException
  {
    String dirName = nextArgFileName( false );
    Long   millis  = nextOptArgDatetimeInMillis();
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf != null ) {
	throwFileOpen();
      }
      File curDir = this.curDir;
      if( this.readOnly || (curDir == null) ) {
	/*
	 * Fehlermeldung "Read Only" ist fuer dieses Kommando
	 * nicht dokumentiert, deshalb "Command Failed".
	 */
	throwCommandFailed();
      }
      if( this.forceLowerCase ) {
	dirName = dirName.toLowerCase();
      }
      startIOTask(
		IOCmd.MAKE_DIR,
		new File( curDir, dirName ),
		null,
		millis,
		0 );
    }
  }


  private void execMakeDirIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      closeReadOnlyFile();
      File file = this.ioFile;
      if( this.readOnly || (file == null) ) {
	/*
	 * Fehlermeldung "Read Only" ist fuer dieses Kommando
	 * nicht dokumentiert, deshalb "Command Failed".
	 */
	throwCommandFailed();
      }
      if( file.mkdir() ) {
	if( !this.forceCurTimestamp ) {
	  Long millis = this.ioFileMillis;
	  if( millis != null ) {
	    file.setLastModified( millis.longValue() );
	  }
	}
      } else {
	throwCommandFailed();
      }
    }
  }


  private void execOpenFileForRead() throws
					IncompleteCmdException,
					VdipException
  {
    String fName = nextArgFileName( false );
    if( (this.cmdArgPos >= 2) && (this.cmdArgPos < this.cmdLineLen) ) {
      /*
       * Optional kann ein Datum zur Protokollierung des letzten Zugriffs
       * angegeben werden.
       * In der Emulation wird dieses Argument inhaltlich ignoriert,
       * allerdings wird es trotzdem gelesen,
       * um das vollstaendige Einlesen der Kommandozeile sicherzustellen.
       */
      try {
	long v = nextArgNumber( 4 );
      }
      catch( VdipException ex ) {}
    }
    synchronized( this.lockObj ) {
      checkDisk();
      if( (this.raf != null) && this.fileWrite ) {
	throwFileOpen();
      }
      File file = findFile( fName );
      if( file != null ) {
	if( file.isFile() ) {
	  startIOTask( IOCmd.OPEN_FILE_FOR_READ, file, fName, null, 0 );
	} else {
	  throwInvalid();
	}
      } else {
	throwCommandFailed();
      }
    }
  }


  private void execOpenFileForReadIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      closeReadOnlyFile();
      File file = this.ioFile;
      if( file != null ) {
	try {
	  this.raf       = new RandomAccessFile( file, "r" );
	  this.fileWrite = false;
	}
	catch( IOException ex ) {}
      }
      if( this.raf == null ) {
	throwCommandFailed();
      }
    }
  }


  private void execOpenFileForWrite() throws
					IncompleteCmdException,
					VdipException
  {
    String fName  = nextArgFileName( false );
    Long   millis = nextOptArgDatetimeInMillis();
    synchronized( this.lockObj ) {
      checkDisk();
      if( (this.raf != null) && this.fileWrite ) {
	throwFileOpen();
      }
      File curDir = this.curDir;
      if( curDir != null ) {
	File file = new File( curDir, fName );
	if( !file.exists() && this.forceLowerCase ) {
	  file = new File( curDir, fName.toLowerCase() );
	}
	if( this.readOnly || (file.exists() && !file.canWrite()) ) {
	  throwReadOnly();
	}
	startIOTask(
		IOCmd.OPEN_FILE_FOR_WRITE,
		file,
		fName,
		millis,
		0 );
      } else {
	throwNoDisk();
      }
    }
  }


  private void execOpenFileForWriteIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      closeReadOnlyFile();
      File file = this.ioFile;
      if( file != null ) {
	if( this.readOnly || (file.exists() && !file.canWrite()) ) {
	  throwReadOnly();
	}
	RandomAccessFile raf = null;
	try {
	  raf = new RandomAccessFile( file, "rw" );
	  raf.seek( raf.length() );
	  this.raf        = raf;
	  this.file       = file;
	  this.fileMillis = this.ioFileMillis;
	  this.fileWrite  = true;
	}
	catch( IOException ex ) {
	  EmuUtil.closeSilent( raf );
	}
      }
      if( this.raf == null ) {
	throwCommandFailed();
      }
    }
  }


  private void execQueryDevice() throws IncompleteCmdException, VdipException
  {
    long device = nextArgByte();
    if( (device < 0) || (device > 15) ) {
      throwCommandFailed();
    }
    if( (device == 0) && (this.rootDir != null) ) {
      putNumber( 0x01, 1 );	// USB Address
      putNumber( 0, 1 );	// Control End Point 0 Size
      putNumber( 0, 1 );	// Pipe In End Point Number
      putNumber( 0, 1 );	// Pipe In End Point Size
      putNumber( 0, 1 );	// Pipe Out End Point Number
      putNumber( 0, 1 );	// Pipe Out End Point Size
      putNumber( 0, 1 );	// Data Toggles
      putNumber( 0x20, 1 );	// Device Type
      putNumber( 0, 1 );	// Reserved
      putNumber( 2, 1 );	// Location (Port 2)
      putNumber( 0, 1 );	// MI Index
      putNumber( 0x08, 1 );	// Device Class
      putNumber( 0x06, 1 );	// Device Sub Class
      putNumber( 0x50, 1 );	// Device Protocol
      putNumber( 0x08EC, 2 );	// VID
      putNumber( 0x0008, 2 );	// PID
      putNumber( 0x0100, 2 );	// BCD
      putNumber( 0x01, 1 );	// Device Speed
      for( int i = 0; i < 11; i++ ) {
	putNumber( 0, 1 );	// Reserved
      }
    } else {
      for( int i = 0; i < 32; i++ ) {
	putNumber( 0, 1 );
      }
    }
    putResultByte( '\r' );
    doCmdFinish( true );
  }


  private void execQueryPort( int portNum )
  {
    putNumber(
	((portNum == 2) && (this.rootDir != null)) ? 0x20 : 0x00,
	1 );
    putNumber( 0, 1 );
    putResultByte( '\r' );
    doCmdFinish( true );
  }


  private void execReadFile() throws VdipException
  {
    String fName = nextArgFileName( false );
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf != null ) {
	throwFileOpen();
      }
      File file = findFile( fName );
      if( file != null ) {
	if( file.isFile() ) {
	  startIOTask( IOCmd.READ_FILE, file );
	} else {
	  throwInvalid();
	}
      } else {
	throwCommandFailed();
      }
    }
  }


  private void execReadFileIO() throws VdipException
  {
    synchronized( this.lockObj ) {
      closeReadOnlyFile();
      File file = this.ioFile;
      if( file == null ) {
	throwCommandFailed();
      }
      InputStream in = null;
      try {
	in = new FileInputStream( file );
	int b = in.read();
	while( b >= 0 ) {
	  writeResultByte( b );
	  b = in.read();
	}
      }
      catch( Exception ex ) {
	throwCommandFailed();
      }
      finally {
	EmuUtil.closeSilent( in );
      }
    }
  }


  private void execReadFromFile() throws IncompleteCmdException, VdipException
  {
    long nBytes = nextArgDWord();
    synchronized( this.lockObj ) {
      checkDisk();
      startIOTask( IOCmd.READ_FROM_FILE, nBytes );
    }
  }


  private void execReadFromFileIO() throws VdipException
  {
    boolean err = false;
    synchronized( this.lockObj ) {
      long n = this.ioCount;
      try {
	while( !err && (n > 0) ) {
	  int b = -1;
	  try {
	    b = this.raf.read();
	  }
	  catch( EOFException ex ) {
	    b = -1;
	  }
	  if( b < 0 ) {
	    err = true;
	    break;
	  }
	  writeResultByte( b );
	  --n;
	}
      }
      catch( Exception ex ) {
	err = true;
      }
      while( n > 0 ) {
	putResultByte( 0 );
	--n;
      }
    }
    if( err ) {
      throwCommandFailed();
    }
  }


  private void execRename() throws VdipException
  {
    String srcName = nextArgFileName( false );
    String dstName = nextArgFileName( false );
    synchronized( this.lockObj ) {
      checkDisk();

      // Pruefen, dass keine Datei offen ist
      if( (this.raf != null) && this.fileWrite ) {
	throwFileOpen();
      }

      /*
       * Schreinschutz pruefen,
       * Die Fehlermeldung "Read Only" ist fuer dieses Kommando
       * nicht dokumentiert, deshalb "Command Failed" ausgeben.
       */
      if( this.readOnly ) {
	throwCommandFailed();
      }

      // Argumente auswerten
      File curDir  = this.curDir;
      File srcFile = findFile( srcName );
      File dstFile = findFile( dstName );
      if( (curDir == null) || (srcFile == null) || (dstFile != null) ) {
	throwCommandFailed();
      }
      if( this.forceLowerCase ) {
	dstName = dstName.toLowerCase();
      }
      if( !srcFile.renameTo( new File( curDir, dstName ) ) ) {
	throwCommandFailed();
      }
    }
    doCmdFinish( true );
  }


  private void execSectorDump()
  {
    for( int i = 0; i < 512; i++ ) {
      putResultByte( 0 );
    }
    doCmdFinish( true );
  }


  private void execSectorWriteIO()
  {
    synchronized( this.lockObj ) {
      try {
	int n = 512;
	this.ioOut = new ByteQueue( n );
	setWriteEnabled( true );
	while( n > 0 ) {
	  this.ioOut.read();
	  --n;
	  setWriteEnabled( true );
	}
      }
      catch( IOException ex ) {}
      finally {
	this.ioOut = null;
      }
    }
    doCmdFinish( true );
  }


  private void execSeek() throws IncompleteCmdException, VdipException
  {
    long pos = nextArgDWord();
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf == null ) {
	throwInvalid();
      }
      try {
	if( (pos < 0) || (pos > this.raf.length()) ) {
	  throwCommandFailed();
	}
	this.raf.seek( pos );
      }
      catch( Exception ex ) {
	throwCommandFailed();
      }
    }
    doCmdFinish( true );
  }


  private void execSendDataFailed() throws InterruptedException, VdipException
  {
    synchronized( this.lockObj ) {
      try {
	long n = this.ioCount;
	if( n > 0 ) {
	  this.ioOut = new ByteQueue( 1 );
	  setWriteEnabled( true );
	  while( n > 0 ) {
	    this.ioOut.read();
	    --n;
	    setWriteEnabled( true );
	  }
	}
      }
      catch( IOException ex ) {}
      finally {
	this.ioOut = null;
      }
    }
    throwCommandFailed();
  }


  private void execSetBitMode() throws IncompleteCmdException
  {
    // Fuer dieses Kommando sind keine Fehlercodes dokumentiert.
    try {
      this.bitMode = (int) nextArgNumber( 2 );
    }
    catch( VdipException ex ) {}
    doCmdFinish( true );
  }


  private void execSuspendDisk() throws VdipException
  {
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf != null ) {
	throwFileOpen();
      }
    }
    doCmdFinish( true );
  }


  private void execWriteToFile() throws IncompleteCmdException, VdipException
  {
    long    nBytes = nextArgDWord();
    boolean done   = false;
    synchronized( this.lockObj ) {
      checkDisk();
      if( this.raf == null ) {
	throwInvalid();
      }
      if( nBytes > 0 ) {
	startIOTask( IOCmd.WRITE_TO_FILE, nBytes );
	done = true;
      }
    }
    if( !done ) {
      doCmdFinish( true );
    }
  }


  private void execWriteToFileIO() throws InterruptedException, VdipException
  {
    boolean err = false;
    synchronized( this.lockObj ) {
      try {
	long n = this.ioCount;
	if( n > 0 ) {
	  this.ioOut = new ByteQueue( 1 );
	  setWriteEnabled( true );
	  while( n > 0 ) {
	    int b = this.ioOut.read();
	    if( this.readOnly ) {
	      err = true;
	    }
	    if( !err ) {
	      try {
		this.raf.write( b );
		updLastModified();
	      }
	      catch( IOException ex ) {
		err = true;
	      }
	    }
	    --n;
	    setWriteEnabled( true );
	  }
	}
      }
      catch( IOException ex ) {
	err = true;
      }
      finally {
	this.ioOut = null;
      }
    }
    if( err ) {
      /*
       * Fehlermeldung "Command Failed" ist fuer dieses Kommando
       * nicht dokumentiert, deshalb wird "Disk Full" gemeldet.
       */
      throw new VdipException( VdipErr.DISK_FULL );
    }
  }


  private File findFile( String fileName ) throws VdipException
  {
    File file    = null;
    File curDir  = this.curDir;
    File rootDir = this.rootDir;
    if( (curDir != null) && (rootDir != null) ) {
      if( fileName.equals( "." ) || fileName.equals( ".." ) ) {
	/*
	 * Im emulierten Wurzelverzeichnis gibt es "." und ".." nicht,
	 * d.h., in dem Fall wird "Bad Command" zurueckgemeldet.
	 */
	if( !curDir.equals( rootDir ) ) {
	  if( fileName.equals( "." ) ) {
	    file = curDir;
	  }
	  if( fileName.equals( ".." ) ) {
	    file = curDir.getParentFile();
	  }
	}
	if( file == null ) {
	  throwBadCommand();
	}
      } else {
	file = new File( curDir, fileName );
	if( !file.exists() ) {
	  file = null;
	  if( Main.isUnixLikeOS() ) {
	    File[] files = curDir.listFiles();
	    if( files != null ) {
	      for( File f : files ) {
		String s = f.getName();
		if( s.equalsIgnoreCase( fileName ) ) {
		  file = f;
		  break;
		}
	      }
	    }
	  }
	}
      }
    }
    return file;
  }


  private void fireRunIOTask()
  {
    synchronized( this.ioTaskThread ) {
      try {
	this.ioTaskThread.notify();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }


  private long getDateTimeByMillis( long millis )
  {
    long v = 0;
    synchronized( this.calendar ) {
      this.calendar.clear();
      this.calendar.setTimeInMillis( millis );
      int year = this.calendar.get( Calendar.YEAR ) - 1980;
      if( year > 0 ) {
	v |= ((year << 25) & 0xFE000000L);
      }
      v |= (((this.calendar.get( Calendar.MONTH ) + 1) << 21) & 0x01E00000L);
      v |= ((this.calendar.get( Calendar.DAY_OF_MONTH ) << 16) & 0x001F0000L);
      v |= ((this.calendar.get( Calendar.HOUR_OF_DAY ) << 11) & 0x0000F800L);
      v |= ((this.calendar.get( Calendar.MINUTE ) << 5) & 0x000007E0L);
      v |= ((this.calendar.get( Calendar.SECOND ) / 2) & 0x0000001FL);
    }
    return v;
  }


  private int getDebugLevel()
  {
    return this.debugLevel;
  }


  private static boolean isValidFileName( String fName )
  {
    boolean rv = false;
    if( fName != null ) {
      boolean dot   = false;
      int     nMain = 0;
      int     nExt  = 0;
      int     len   = fName.length();
      for( int i = 0; i < len; i++ ) {
	char ch = fName.charAt( i );
	if( ch == '.' ) {
	  if( dot ) {
	    nMain = 0;
	    break;
	  }
	  dot = true;
	} else if( isValidFileNameChar( ch ) ) {
	  if( dot ) {
	    nExt++;
	  } else {
	    nMain++;
	  }
	} else {
	  nMain = 0;
	  break;
	}
      }
      if( (nMain >= 1) && (nMain <= 8) ) {
	if( dot ) {
	  rv = ((nExt >= 1) && (nExt <= 3));
	} else {
	  rv = (nExt == 0);
	}
      }
    }
    return rv;
  }


  private static boolean isValidFileNameChar( char ch )
  {
    return ((ch >= 'A') && (ch <= 'Z'))
	      || ((ch >= 'a') && (ch <= 'z'))
	      || ((ch >= '0') && (ch <= '9'))
	      || (("$%\'-_@~\u0060!(){}^#&").indexOf( ch ) >= 0);
  }


  private long nextArgByte() throws IncompleteCmdException, VdipException
  {
    return nextArgNumber( 1 );
  }


  private long nextArgDWord() throws IncompleteCmdException, VdipException
  {
    return nextArgNumber( 4 );
  }


  private long nextArgQWord() throws IncompleteCmdException, VdipException
  {
    return nextArgNumber( 8 );
  }


  private long nextArgNumber( int nBytes ) throws
					IncompleteCmdException,
					VdipException
  {
    if( this.cmdArgPos < 2 ) {
      throwBadCommand();
    }
    long rv = 0;
    if( this.binaryMode ) {
      if( (this.cmdArgPos + nBytes - 1) >= this.cmdLineLen ) {
	throw new IncompleteCmdException();
      }
      for( int i = 0; i < nBytes; i++ ) {
	rv <<= 8;
	rv |= ((int) this.cmdLineBytes[ this.cmdArgPos++ ] & 0xFF);
      }
      if( this.cmdArgPos < this.cmdLineLen ) {
	if( this.cmdLineBytes[ this.cmdArgPos ] == (byte) 0x20 ) {
	  this.cmdArgPos++;
	}
      }
    } else {
      if( this.cmdArgPos >= this.cmdLineLen ) {
	throwBadCommand();
      }
      int b = (int) this.cmdLineBytes[ this.cmdArgPos++ ] & 0xFF;
      while( (b == 0x20) && (this.cmdArgPos < this.cmdLineLen) ) {
	b = (int) this.cmdLineBytes[ this.cmdArgPos++ ] & 0xFF;
      }
      boolean isNum = false;
      boolean isHex = false;
      if( b == '$' ) {
	isHex = true;
      } else if( b == '0' ) {
	if( this.cmdArgPos < this.cmdLineLen ) {
	  int b1 = (int) this.cmdLineBytes[ this.cmdArgPos ] & 0xFF;
	  if( (b1 == 'x') || (b1 == 'X') ) {
	    this.cmdArgPos++;
	    isHex = true;
	  }
	}
	if( !isHex ) {
	  isNum = true;
	}
      } else if( (b >= '1') && (b <= '9') ) {
	rv    = b - '0';
	isNum = true;
      } else {
	throwBadCommand();
      }
      if( isHex ) {
	while( this.cmdArgPos < this.cmdLineLen ) {
	  b = (int) this.cmdLineBytes[ this.cmdArgPos++ ] & 0xFF;
	  if( b == 0x20 ) {
	    break;
	  }
	  if( (b >= '0') && (b <= '9') ) {
	    rv <<= 4;
	    rv |= (b - '0');
	  } else if( (b >= 'A') && (b <= 'F') ) {
	    rv <<= 4;
	    rv |= (b - 'A' + 10);
	  } else if( (b >= 'a') && (b <= 'f') ) {
	    rv <<= 4;
	    rv |= (b - 'a' + 10);
	  } else {
	    throwBadCommand();
	  }
	  isNum = true;
	}
      } else {
	while( this.cmdArgPos < this.cmdLineLen ) {
	  b = (int) this.cmdLineBytes[ this.cmdArgPos++ ] & 0xFF;
	  if( b == 0x20 ) {
	    break;
	  }
	  if( (b < '0') || (b > '9') ) {
	    throwBadCommand();
	  }
	  rv *= 10;
	  rv += (b - '0');
	  isNum = true;
	}
      }
      if( !isNum ) {
	throwBadCommand();
      }
    }
    return rv;
  }


  private String nextArgFileName( boolean allowDotDirs ) throws VdipException
  {
    String rv = null;
    if( this.cmdArgPos >= 2 ) {
      // Es ist nur ein Leerzeichen vor dem Dateinamen erlaubt.
      if( this.cmdArgPos < this.cmdLineLen ) {
	if( this.cmdLineBytes[ this.cmdArgPos ] == (byte) 0x20 ) {
	  this.cmdArgPos++;
	}
      }
      if( this.cmdArgPos >= this.cmdLineLen ) {
	throwBadCommand();
      }
      if( this.cmdLineBytes[ this.cmdArgPos ] == (byte) 0x20 ) {
	throwCommandFailed();
      }
      if( ((this.cmdArgPos + 1) == this.cmdLineLen)
	  && (this.cmdLineBytes[ this.cmdArgPos ] == '.') )
      {
	rv = ".";
      }
      else if( ((this.cmdArgPos + 2) == this.cmdLineLen)
	       && (this.cmdLineBytes[ this.cmdArgPos ] == '.')
	       && (this.cmdLineBytes[ this.cmdArgPos + 1 ] == '.') )
      {
	rv = "..";
      }
      if( rv != null ) {
	if( !allowDotDirs ) {
	  throwBadCommand();
	}
      } else {
	/*
	 * Uebergebener Dateiname in Basisname und Erweiterung zerlegen,
	 * Ueberschuessige Zeichen werden ignoriert.
	 */
	char[]  baseBuf = new char[ 12 ];
	char[]  extBuf  = new char[ 3 ];
	int     basePos = 0;
	int     extPos  = 0;
	boolean dot     = false;
	while( this.cmdArgPos < this.cmdLineLen ) {
	  char ch = (char) ((int) this.cmdLineBytes[ this.cmdArgPos++ ]
								& 0xFF);
	  if( ch == '\u0020' ) {
	    break;
	  }
	  if( ch == '.' ) {
	    if( dot ) {
	      throwFilenameInvalid();
	    }
	    dot = true;
	  } else {
	    ch = Character.toUpperCase( ch );
	    if( dot ) {
	      if( extPos < extBuf.length ) {
		extBuf[ extPos++ ] = ch;
	      }
	    } else {
	      if( basePos < baseBuf.length ) {
		baseBuf[ basePos++ ] = ch;
	      }
	    }
	  }
	}
	/*
	 * Wenn keine Dateinamenserweiterung angegeben wurde
	 * und die Laenge des Basisnames groesser 8 ist,
	 * wird aus der Ueberlaenge des Basisnamens die Erweiterung gebildet.
	 */
	if( (extPos == 0) && (basePos > 8) ) {
	  for( int i = 8; i < basePos; i++ ) {
	    if( (i < baseBuf.length) && (extPos < extBuf.length) ) {
	      extBuf[ extPos++ ] = baseBuf[ i ];
	    }
	  }
	}
	/*
	 * Basisname und Erweiterung zusammenfuehren
	 * und dabei die Gueltigkeit des Namen ueberpruefen
	 */
	if( basePos > 0 ) {
	  if( basePos > 8 ) {
	    basePos = 8;
	  }
	  for( int i = 0; i < basePos; i++ ) {
	    if( !isValidFileNameChar( baseBuf[ i ] ) ) {
	      throwFilenameInvalid();
	    }
	  }
	  if( extPos > 0 ) {
	    baseBuf[ basePos++ ] = '.';
	    for( int i = 0; i < extPos; i++ ) {
	      if( basePos < baseBuf.length ) {
		char ch = extBuf[ i ];
		if( isValidFileNameChar( ch ) ) {
		  baseBuf[ basePos++ ] = ch;
		} else {
		  throwFilenameInvalid();
		}
	      }
	    }
	  }
	  rv = new String( baseBuf, 0, basePos );
	} else {
	  throwFilenameInvalid();
	}
      }
    } else {
      throwBadCommand();
    }
    return rv;
  }


  /*
   * Die Methode prueft, ob ein optionaler Zeitstempel angegeben ist
   * und wandelt diesen in Millisekunden um.
   * Ist kein oder ein ungueltiger Zeitstempel angegeben,
   * wird der Standardzeitstempel zurueckgeliefert.
   * Bei forceCurTimestamp == true wird immer null zurueckgeliefert,
   * allerdings wird auch in diesem Fall das Zeitstempelargument geparst,
   * um sicherzustellen, dass die Kommandozeile vollstaendig eingelesen wurde.
   */
  private Long nextOptArgDatetimeInMillis() throws IncompleteCmdException
  {
    Long millis = null;
    if( (this.cmdArgPos >= 2) && (this.cmdArgPos < this.cmdLineLen) ) {
      long value = DEFAULT_DATETIME_VALUE;
      try {
	long v = nextArgNumber( 4 );
	if( (v & 0x00FF0000) != 0 ) {
	  /*
	   * Zeitstempel ist gueltig, wenn mindestens 1 Bit
	   * von 16:23 ungleich 0 ist
	   */
	  value = v;
	}
      }
      catch( VdipException ex ) {}
      if( !this.forceCurTimestamp ) {
	synchronized( this.calendar ) {
	  this.calendar.clear();
	  this.calendar.set(
		(int) ((value >> 25) & 0x7F) + 1980,
		(int) ((value >> 21) & 0x0F) - 1,
		(int) (value >> 16) & 0x1F,
		(int) (value >> 11) & 0x1F,
		(int) (value >> 5) & 0x3F,
		(int) (value & 0x1F) * 2 );
	  millis = this.calendar.getTimeInMillis();
	}
      }
    }
    return millis;
  }


  private void putDiskDetected()
  {
    putResult( "Disk Detected P2\r", "DD2" );
    putResult( "No Upgrade\r", "NU\r" );
    putPrompt();
  }


  private void putDiskRemoved()
  {
    putResult( "Disk Removed P2\r", "DR2" );
    putResult( "No Disk\r", "ND\r" );
  }


  private void putError( VdipErr vdipErr )
  {
    String msg = null;
    if( this.extendedMode ) {
      msg = "Command Failed\r";
      switch( vdipErr ) {
	case BAD_COMMAND:
	  msg = "Bad Command\r";
	  break;
	case DIR_NOT_EMPTY:
	  msg = "Dir Not Empty\r";
	  break;
	case DISK_FULL:
	  msg = "Disk Full\r";
	  break;
	case FILE_OPEN:
	  msg = "File Open\r";
	  break;
	case FILENAME_INVALID:
	  msg = "Filename Invalid\r";
	  break;
	case INVALID:
	  msg = "Invalid\r";
	  break;
	case NO_DISK:
	  msg = "No Disk\r";
	  break;
	case NO_UPGRADE:
	  msg = "No Upgrade\r";
	  break;
	case READ_ONLY:
	  msg = "Read Only\r";
	  break;
      }
    } else {
      msg = "CF\r";
      switch( vdipErr ) {
	case BAD_COMMAND:
	  msg = "BC\r";
	  break;
	case DIR_NOT_EMPTY:
	  msg = "NE";
	  break;
	case DISK_FULL:
	  msg = "DF\r";
	  break;
	case FILE_OPEN:
	  msg = "FO\r";
	  break;
	case FILENAME_INVALID:
	  msg = "FN\r";
	  break;
	case INVALID:
	  msg = "FI\r";
	  break;
	case NO_DISK:
	  msg = "ND\r";
	  break;
	case NO_UPGRADE:
	  msg = "NU\r";
	  break;
	case READ_ONLY:
	  msg = "RO\r";
	  break;
      }
    }
    putString( msg );
  }


  public void putNumber( long value, int nBytes )
  {
    for( int i = 0; i < nBytes; i++ ) {
      if( this.binaryMode ) {
	putResultByte( (int) value );
      } else {
	putString( String.format( "$%02X ", value & 0xFF ) );
      }
      value >>= 8;
    }
  }


  private void putPrompt()
  {
    putResult( "D:\\>\r", ">\r" );
  }


  private void putResult( String extText, String shortText )
  {
    putString( this.extendedMode ? extText : shortText );
  }


  private void putResultByte( int b )
  {
    synchronized( this.resultQueue ) {
      boolean empty = this.resultQueue.isEmpty();
      this.resultQueue.add( (byte) b );
      if( empty ) {
	this.pio.putInValuePortB( 0x00, 0x01 );
	this.pio.strobePortB();
      }
    }
  }


  private void putString( String text )
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	synchronized( this.resultQueue ) {
	  for( int i = 0; i < len; i++ ) {
	    putResultByte( text.charAt( i ) );
	  }
	}
      }
    }
  }


  public void resetVDIP()
  {
    this.resultQueue.clear();
    this.cmdLineLen   = 0;
    this.cmdArgPos    = 0;
    this.bitMode      = 0;
    this.binaryMode   = true;
    this.extendedMode = true;
    this.writeEnabled = false;
    this.fileWrite    = false;
    this.file         = null;
    this.fileMillis   = null;
    this.curDir       = this.rootDir;
    this.ioOut        = null;
    this.ioCmd        = IOCmd.NONE;
    this.ioFile       = null;
    this.ioFileMillis = null;
    this.ioFileName   = null;
    this.ioCount      = 0;
    this.ioTaskThread.interrupt();
    fireRunIOTask();
    closeFile();
    this.pio.putInValuePortB( 0xF7, 0xFF );
    putString( "\rVer 03.69VDAPF On-Line:\r" );
    if( this.rootDir != null ) {
      putDiskDetected();
    }
    setWriteEnabled( true );
  }


  private void updLastModified()
  {
    if( this.fileWrite && !this.forceCurTimestamp) {
      File file   = this.file;
      Long millis = this.fileMillis;
      if( (file != null) && (millis != null) ) {
	file.setLastModified( millis.longValue() );
      }
    }
  }


  private void setWriteEnabled( boolean state )
  {
    this.writeEnabled = state;
    this.pio.putInValuePortB( state ? 0x00 : 0x02, 0x02 );
  }


  private void startIOTask(
			IOCmd  cmd,
			File   file,
			String text,
			Long   millis,
			long   value )
  {
    this.ioCmd        = cmd;
    this.ioFile       = file;
    this.ioFileName   = text;
    this.ioFileMillis = millis;
    this.ioCount      = value;
    fireRunIOTask();
  }


  private void startIOTask(
			IOCmd  cmd,
			File   file,
			String text,
			long   value )
  {
    startIOTask( cmd, null, null, null, 0 );
  }


  private void startIOTask( IOCmd cmd )
  {
    startIOTask( cmd, null, null, null, 0 );
  }


  private void startIOTask( IOCmd cmd, File file )
  {
    startIOTask( cmd, file, null, null, 0 );
  }


  private void startIOTask( IOCmd cmd, long value )
  {
    startIOTask( cmd, null, null, null, value );
  }


  private void throwBadCommand() throws VdipException
  {
    throw new VdipException( VdipErr.BAD_COMMAND );
  }


  private void throwCommandFailed() throws VdipException
  {
    throw new VdipException( VdipErr.COMMAND_FAILED );
  }


  private void throwFileOpen() throws VdipException
  {
    throw new VdipException( VdipErr.FILE_OPEN );
  }


  private void throwFilenameInvalid() throws VdipException
  {
    throw new VdipException( VdipErr.FILENAME_INVALID );
  }


  private void throwInvalid() throws VdipException
  {
    throw new VdipException( VdipErr.INVALID );
  }


  private void throwNoDisk() throws VdipException
  {
    throw new VdipException( VdipErr.NO_DISK );
  }


  private void throwReadOnly() throws VdipException
  {
    throw new VdipException( VdipErr.READ_ONLY );
  }


  private void writeResultByte( int b ) throws IOException
  {
    boolean empty = this.resultQueue.isEmpty();
    this.resultQueue.write( (byte) b );
    if( empty ) {
      this.pio.putInValuePortB( 0x00, 0x01 );
      this.pio.strobePortB();
    }
  }
}

