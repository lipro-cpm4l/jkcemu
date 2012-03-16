/*
 * (c) 2010-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation von GIDE
 */

package jkcemu.disk;

import java.awt.Component;
import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.etc.RTC7242X;


public class GIDE implements Runnable
{
  private static final int STATUS_ERROR         = 0x01;
  private static final int STATUS_DATA_REQUEST  = 0x08;
  private static final int STATUS_SEEK_COMPLETE = 0x10;
  private static final int STATUS_WRITE_FAULT   = 0x20;
  private static final int STATUS_DRIVE_READY   = 0x40;
  private static final int STATUS_BUSY          = 0x80;

  private static final int ERROR_TRACK0_NOT_FOUND   = 0x02;
  private static final int ERROR_CMD_ABORTED        = 0x04;
  private static final int ERROR_WRONG_SECTOR       = 0x10;
  private static final int ERROR_UNCORRECTABLE_DATA = 0x40;

  private static final int SECTOR_SIZE = 512;

  private enum Command {
		NONE,
		FORMAT_TRACK,
		IDENTIFY_DISK,
		READ_BUFFER,
		READ_SECTORS,
		WRITE_BUFFER,
		WRITE_SECTORS };

  private static final String[] propKeys = {
					"model",
					"cylinders",
					"heads",
					"sectors_per_track",
					"file" };

  private Component        owner;
  private String           propPrefix;
  private HardDisk[]       disks;
  private RTC7242X         rtc;
  private volatile Command pendingCmd;
  private volatile File    ioFile;
  private volatile long    ioFilePos;
  private volatile int     ioByteCnt;
  private volatile boolean ioTaskEnabled;
  private volatile boolean ioTaskNoWait;
  private volatile Thread  ioTaskThread;
  private byte[]           ioBuf;
  private int              ioBufPos;
  private int              debugLevel;
  private int              sectorCnt;
  private int              sectorNum;
  private int              cylNum;
  private int              sdhReg;
  private volatile int     statusReg;
  private volatile int     errorReg;
  private int              powerMode;
  private volatile int     curCmd;
  private volatile int     curDiskIdx;
  private HardDisk         curDisk;
  private int[]            cylinders;
  private int[]            heads;
  private int[]            sectorsPerTrack;
  private long[]           totalSectors;
  private boolean          interruptEnabled;
  private volatile boolean interruptRequest;
  private boolean          resetFlag;
  private boolean          readErrShown;
  private boolean          writeErrShown;


  public static boolean complies(
				GIDE       gide,
				Properties props,
				String     propPrefix )
  {
    boolean rv = false;
    if( emulatesGIDE( props, propPrefix ) ) {
      if( gide != null ) {
	rv = gide.sameDisks( getHardDisks( props, propPrefix ) );
      }
    } else {
      if( gide == null ) {
	rv = true;
      }
    }
    return rv;
  }


  public void die()
  {
    this.ioTaskEnabled = false;
    this.ioTaskThread.interrupt();
    synchronized( this.ioTaskThread ) {
      try {
	this.ioTaskThread.notify();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }


  public static GIDE getGIDE(
			Component  owner,
			Properties props,
			String     propPrefix )
  {
    GIDE       gide  = null;
    HardDisk[] disks = getHardDisks( props, propPrefix );
    if( disks != null ) {
      if( disks.length > 0 ) {
	gide = new GIDE( owner, propPrefix, disks );
      }
    }
    return gide;
  }


  public boolean isInterruptRequest()
  {
    return this.interruptRequest;
  }


  public synchronized int read( int port )
  {
    int rv = -1;
    if( this.disks != null ) {
      switch( port & 0x0F ) {

	// Real Time Clock
	case 0x05:
	  rv = this.rtc.read( port >> 8 );
	  if( this.debugLevel > 0 ) {
	    System.out.printf(
			"GIDE: read rtc reg %d: %02X\n",
			(port >> 8) & 0xFF,
			rv );
	  }
	  break;

	/*
	 * Alternatives Status-Register
	 * Im Gegensatz zum regulaeren Status-Register fuehrt
	 * ein Lesezugriff nicht zu einer Interrupt-Bestaetigung.
	 */
	case 0x06:
	  rv = this.statusReg;
	  if( this.debugLevel > 2 ) {
	    System.out.printf( "GIDE: read alternate status: %02X\n", rv );
	  }
	  break;

	/*
	 * Drive Address Register:
	 *   Laufwerks- und Kopfnummer werden widergespiegelt.
	 */
	case 0x07:
	  rv = this.sdhReg & 0x1F;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: read drive+head: %02X\n", rv );
	  }
	  break;

	// Datenregister
	case 0x08:
	  this.interruptRequest = false;
	  rv                    = readDataReg();
	  if( this.debugLevel > 1 ) {
	    System.out.printf( "GIDE: read data: %02X\n", rv );
	  }
	  break;

	// Fehlerregister
	case 0x09:
	  rv = this.errorReg;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: read error: %02X\n", rv );
	  }
	  break;

	// Sektoranzahl
	case 0x0A:
	  rv = this.sectorCnt;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: read sector count: %02X\n", rv );
	  }
	  break;

	// Sektornummer
	case 0x0B:
	  rv = this.sectorNum;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: read sector number: %02X\n", rv );
	  }
	  break;

	// Zylindernummer (L-Byte)
	case 0x0C:
	  rv = this.cylNum & 0xFF;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: cyl number (low byte): %02X\n", rv );
	  }
	  break;

	// Zylindernummer (H-Byte)
	case 0x0D:
	  rv = (this.cylNum >> 8) & 0xFF;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: cyl number (high byte): %02X\n", rv );
	  }
	  break;

	/*
	 * SDH-Register:
	 *    Bit 0-3: Kopfnummer
	 *    Bit 4:   Laufwerksauswahl (0: Master, 1: Slave)
	 *    Bit 5-6: Sektorgroesse,
	 *             muss immer auf 01 gesetzt sein (512 Bytes)
	 *    Bit 8:   keine CRC-Daten an Sektor anhaengen,
	 *             muss immer gesetzt sein
	 */
	case 0x0E:
	  rv = this.sdhReg;
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: read sdh: %02X\n", rv );
	  }
	  break;

	// Statusregister
	case 0x0F:
	  rv                    = this.statusReg;
	  this.interruptRequest = false;
	  if( this.debugLevel > 2 ) {
	    System.out.printf( "GIDE: read status: %02X\n", rv );
	  }
	  break;
      }
    }
    return rv;
  }


  public synchronized void reset()
  {
    if( this.debugLevel > 0 ) {
      System.out.println( "GIDE: reset" );
    }
    this.pendingCmd   = Command.NONE;
    this.resetFlag    = false;
    this.ioTaskNoWait = false;
    this.ioTaskThread.interrupt();
    if( this.disks != null ) {
      boolean sizeOK = false;
      if( (this.cylinders != null)
	  && (this.heads != null)
	  && (this.sectorsPerTrack != null)
	  && (this.totalSectors != null) )
      {
	if( (this.cylinders.length >= this.disks.length)
	    && (this.heads.length >= this.disks.length)
	    && (this.sectorsPerTrack.length >= this.disks.length)
	    && (this.totalSectors.length >= this.disks.length) )
	{
	  sizeOK = false;
	}
      }
      if( !sizeOK ) {
	this.cylinders       = new int[ this.disks.length ];
	this.heads           = new int[ this.disks.length ];
	this.sectorsPerTrack = new int[ this.disks.length ];
	this.totalSectors    = new long[ this.disks.length ];
      }
      int maxSectsPerTrack = 0;
      for( int i = 0; i < this.disks.length; i++ ) {
	this.cylinders[ i ]       = disks[ i ].getCylinders();
	this.heads[ i ]           = disks[ i ].getHeads();
	this.sectorsPerTrack[ i ] = disks[ i ].getSectorsPerTrack();
	this.totalSectors[ i ]    = (long) this.cylinders[ i ]
					* (long) this.heads[ i ]
					* (long) this.sectorsPerTrack[ i ];
	if( this.sectorsPerTrack[ i ] > maxSectsPerTrack ) {
	  maxSectsPerTrack = this.sectorsPerTrack[ i ];
	}
      }
      int bufSize = Math.max( maxSectsPerTrack, 1 ) * SECTOR_SIZE;
      if( this.ioBuf != null ) {
	if( this.ioBuf.length < bufSize ) {
	  this.ioBuf = null;
	}
      }
      if( this.ioBuf == null ) {
	this.ioBuf = new byte[ bufSize ];
      }
    }
    if( this.ioBuf != null ) {
      Arrays.fill( this.ioBuf, (byte) 0 );
    }
    softReset();
  }


  public synchronized void write( int port, int value )
  {
    value &= 0xFF;
    if( this.disks != null ) {
      switch( port & 0x0F ) {

	// Real Time Clock
	case 0x05:
	  if( this.debugLevel > 0 ) {
	    System.out.printf(
			"GIDE: write rtc reg %d: %02X\n",
			(port >> 8) & 0xFF,
			value );
	  }
	  this.rtc.write( port >> 8, value );
	  break;

	// Digital Output
	case 0x06:
	  {
	    if( this.debugLevel > 0 ) {
	      System.out.printf( "GIDE: write digital output: %02X\n", value );
	    }
	    boolean b = ((value & 0x04) != 0);
	    if( b && !this.resetFlag ) {
	      if( this.debugLevel > 0 ) {
		System.out.println( "GIDE: soft reset" );
	      }
	      softReset();
	    }
	    this.resetFlag = b;
	  }
	  this.interruptEnabled = ((value & 0x02) != 0);
	  fireInterrupt();
	  break;

	// Datenregister
	case 0x08:
	  if( this.debugLevel > 1 ) {
	    System.out.printf( "GIDE: write data: %02X\n", value );
	  }
	  writeDataReg( value );
	  break;

	// Sektoranzahl
	case 0x0A:
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: write sector count: %02X\n", value );
	  }
	  this.sectorCnt = value;
	  break;

	// Sektornummer
	case 0x0B:
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: write sector number: %02X\n", value );
	  }
	  this.sectorNum = value;
	  break;

	// Zylindernummer (L-Byte)
	case 0x0C:
	  if( this.debugLevel > 0 ) {
	    System.out.printf(
			"GIDE: write cyl number (low byte): %02X\n",
			value );
	  }
	  this.cylNum = (this.cylNum & 0xFF00) | value;
	  break;

	// Zylindernummer (H-Byte)
	case 0x0D:
	  if( this.debugLevel > 0 ) {
	    System.out.printf(
			"GIDE: write cyl number (high byte): %02X\n",
			value );
	  }
	  this.cylNum = ((value << 8) & 0xFF00) | (this.cylNum & 0x00FF);
	  break;

	// SDH
	case 0x0E:
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: write sdh: %02X\n", value );
	  }
	  this.sdhReg = value;
	  break;

	// Kommando
	case 0x0F:
	  if( this.debugLevel > 0 ) {
	    System.out.printf( "GIDE: write command: %02X\n", value );
	  }
	  execCmd( value );
	  break;
      }
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    while( this.ioTaskEnabled ) {
      synchronized( this.ioTaskThread ) {
	if( this.ioTaskNoWait ) {
	  this.ioTaskNoWait = false;
	} else {
	  try {
	    this.ioTaskThread.wait();
	  }
	  catch( InterruptedException ex ) {}
	  catch( IllegalMonitorStateException ex ) {}
	}
      }
      if( this.ioTaskEnabled ) {
	switch( this.pendingCmd ) {
	  case READ_SECTORS:
	    execReadSectorsTask();
	    break;

	  case WRITE_SECTORS:
	    execWriteSectorsTask();
	    break;

	  case FORMAT_TRACK:
	    execFormatTrackTask();
	    break;
	}
	this.statusReg &= ~STATUS_BUSY;
      }
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private GIDE( Component owner, String propPrefix, HardDisk[] disks )
  {
    this.owner           = owner;
    this.propPrefix      = propPrefix;
    this.disks           = disks;
    this.rtc             = new RTC7242X();
    this.cylinders       = null;
    this.heads           = null;
    this.sectorsPerTrack = null;
    this.totalSectors    = null;
    this.ioBuf           = null;
    this.ioTaskEnabled   = true;
    this.ioTaskNoWait    = false;
    this.ioTaskThread    = new Thread( this, "JKCEMU GIDE" );
    this.debugLevel      = 0;

    String text = System.getProperty( "jkcemu.debug.gide" );
    if( text != null ) {
      try {
	this.debugLevel = Integer.parseInt( text );
      }
      catch( NumberFormatException ex ) {}
    }
    this.ioTaskThread.start();
    reset();
  }


  private long calcFilePos()
  {
    long rv = -1L;
    if( this.curDiskIdx >= 0 ) {
      /*
       * Wenn mit SetDriveParameters die Festplatte logisch verkleinert
       * oder vergroessert wurde, soll trotzdem auf den physischen Bereich
       * komplett aber nicht darueber hinaus zugegriffen werden koennen.
       * Aus diesem Grund wird nicht die obere Grenze der Zylindernummer
       * geprueft, sondern der absolute Sektorindex mit der Gesamtanzahl
       * der Sektoren.
       */
      long spt     = this.sectorsPerTrack[ this.curDiskIdx ];
      long heads   = this.heads[ this.curDiskIdx ];
      long headNum = this.sdhReg & 0x0F;
      if( (this.cylNum >= 0)
	  && (headNum >= 0) && (headNum < heads)
	  && (this.sectorNum >= 1) && (this.sectorNum <= spt) )
      {
	long nSec = (this.cylNum * heads * spt)
					+ (headNum * spt)
					+ (this.sectorNum - 1);
	if( (nSec >= 0) && (nSec <= this.totalSectors[ this.curDiskIdx ]) ) {
	  rv = 0x0100 + (nSec * SECTOR_SIZE);
	}
      }
      if( (rv < 0) && (this.debugLevel > 3) ) {
	System.out.printf(
		"GIDE calc file pos error: cyl=%d head=%d sector=%d"
			+ " cyls=%d heads=%d sectors_per_track=%d\n",
		this.cylNum,
		headNum,
		this.sectorNum,
		this.cylinders[ this.curDiskIdx ],
		heads,
		spt );
      }
    } else {
      if( this.debugLevel > 3 ) {
	System.out.println( "GIDE calc file pos error: curDiskIdx < 0" );
      }
    }
    return rv;
  }


  /*
   * Sektor eins weiter zaehlen
   *
   * Rueckgabewert:
   *  -1: Kommando beendet
   *   0: Sektornummer weitergezaehlt
   *   1: Uebergang zur naechsten Spur
   */
  private int countSector()
  {
    int rv = 0;
    this.sectorCnt = (this.sectorCnt - 1) & 0xFF;
    if( this.curDiskIdx >= 0 ) {
      int r = this.sectorNum + 1;
      if( r <= this.sectorsPerTrack[ this.curDiskIdx ] ) {
	this.sectorNum = r & 0xFF;
      } else {
	int heads = this.heads[ this.curDiskIdx ];
	int h     = (this.sdhReg & 0x0F) + 1;
	if( this.sectorCnt > 0 ) {
	  if( h <= heads ) {
	    this.sdhReg    = (this.sdhReg & 0xF0) | (h & 0x0F);
	    this.sectorNum = 1;
	    rv = 1;
	  } else {
	    // kein automatischer Uebergang zum naechsten Zylinder
	    this.pendingCmd = Command.NONE;
	    this.errorReg   = ERROR_CMD_ABORTED;
	    this.statusReg  = STATUS_ERROR;
	    rv              = 0;
	  }
	} else {
	  if( h <= heads ) {
	    this.sdhReg = (this.sdhReg & 0xF0) | (h & 0x0F);
	  } else {
	    this.sdhReg &= 0xF0;
	    if( this.cylNum < this.cylinders[ this.curDiskIdx ] ) {
	      this.cylNum++;
	    } else {
	      this.cylNum = 0;
	    }
	  }
	  this.sectorNum  = 1;
	  this.pendingCmd = Command.NONE;
	  this.errorReg   = 0;
	  this.statusReg  = STATUS_SEEK_COMPLETE | STATUS_DRIVE_READY;
	  rv              = 0;
	}
      }
    }
    return rv;
  }


  private static boolean emulatesGIDE(
				Properties props,
				String     propPrefix )
  {
    boolean rv = true;
    for( int i = 0; i < propKeys.length; i++ ) {
      if( EmuUtil.getProperty(
		props,
		propPrefix + "harddisk.1." + propKeys[ i ] ).isEmpty() )
      {
	rv = false;
	break;
      }
    }
    return rv;
  }


  private void execCmd( int cmd )
  {
    this.curCmd     = cmd;
    this.errorReg   = 0;
    this.curDiskIdx = -1;
    this.curDisk    = null;
    if( this.disks != null ) {
      int idx = (this.sdhReg >> 4) & 0x01;
      if( idx < this.disks.length ) {
	this.curDiskIdx = idx;
	this.curDisk    = this.disks[ idx ];
      }
    }
    if( this.curDisk != null ) {
      this.statusReg = STATUS_SEEK_COMPLETE | STATUS_DRIVE_READY;
    } else {
      this.statusReg = 0;
    }
    switch( this.curCmd ) {
      case 0x40:
      case 0x41:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  verify sectors" );
	}
	execCmdVerifySectors();
	break;
      case 0x50:
	execCmdFormatTrack();
	if( this.debugLevel > 0 ) {
	  System.out.println( "  format track" );
	}
	break;
      case 0x90:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  diagnostics" );
	}
	execCmdDiagnostics();
	break;
      case 0x91:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  set drive parameters" );
	}
	execCmdSetDriveParameters();
	break;
      case 0xE0:
      case 0xE1:
      case 0xE2:
      case 0xE3:
      case 0xE6:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  set power mode" );
	}
	execCmdSetPowerMode();
	break;
      case 0xE4:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  read buffer" );
	}
	execCmdReadBuffer();
	break;
      case 0xE5:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  read power mode" );
	}
	execCmdReadPowerMode();
	break;
      case 0xE8:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  write buffer" );
	}
	execCmdWriteBuffer();
	break;
      case 0xEC:
	if( this.debugLevel > 0 ) {
	  System.out.println( "  identify drive" );
	}
	execCmdIdentifyDrive();
	break;
      default:
	if( (cmd & 0xF8) == 0x10 ) {
	  if( this.debugLevel > 0 ) {
	    System.out.println( "  recalibrate" );
	  }
	  execCmdRecalibrate();
	} else if( (cmd & 0xF8) == 0x20 ) {
	  if( this.debugLevel > 0 ) {
	    System.out.println( "  read sectors" );
	  }
	  execCmdReadSectors();
	} else if( (cmd & 0xF8) == 0x30 ) {
	  if( this.debugLevel > 0 ) {
	    System.out.println( "  write sectors" );
	  }
	  execCmdWriteSectors();
	} else if( (cmd & 0xF0) == 0x70 ) {
	  if( this.debugLevel > 0 ) {
	    System.out.println( "  seek" );
	  }
	  /*
	   * Seek-Kommando bewirkt in der Emulation
	   * nur das Ausloesen eines Interrupts.
	   */
	  fireInterrupt();
	}
    }
  }


  private void execCmdDiagnostics()
  {
    if( this.curDisk != null ) {
      this.errorReg = 0x01;			// kein Fehler
      // Wenn Master getestet wird, dann zusaetzlich Slave testen
      if( (this.curDiskIdx == 0) && (this.disks.length < 2) ) {
	this.errorReg |= 0x80;		// Slave nicht bereit
      }
    }
  }


  private void execCmdFormatTrack()
  {
    this.ioBufPos   = 0;
    this.pendingCmd = Command.FORMAT_TRACK;
    this.statusReg |= STATUS_DATA_REQUEST;
  }


  private void execCmdIdentifyDrive()
  {
    if( this.curDisk != null ) {
      Arrays.fill( this.ioBuf, 0, SECTOR_SIZE, (byte) 0 );
      setIOBufWord( 0, 0x015A );
      setIOBufWord( 2, this.curDisk.getCylinders() );
      setIOBufWord( 6, this.curDisk.getHeads() );
      setIOBufWord( 8, this.curDisk.getSectorsPerTrack() * SECTOR_SIZE );
      setIOBufWord( 10, SECTOR_SIZE );
      setIOBufWord( 12, this.curDisk.getSectorsPerTrack() );
      setIOBufASCII( 20, Main.VERSION, 20 );
      setIOBufWord( 42, 1 );		// 1 Sektor Puffer
      setIOBufASCII( 46, "JKCEMU", 8 );

      String model = this.curDisk.getDiskModel();
      if( model != null ) {
	if( model.isEmpty() ) {
	  model = null;
	}
      }
      if( model == null ) {
	model = String.format(
			"Sonstige (%dx%dx%d)",
			this.curDisk.getCylinders(),
			this.curDisk.getHeads(),
			this.curDisk.getSectorsPerTrack() );
      }
      setIOBufASCII( 54, model, 40 );
      File file = this.curDisk.getFile();
      if( file != null ) {
	if( !file.canWrite() ) {
	  setIOBufWord( 98, 1 );	// schreibgeschuetzt
	}
      }
      this.ioBufPos   = 0;
      this.pendingCmd = Command.IDENTIFY_DISK;
      this.statusReg |= STATUS_DATA_REQUEST;
      fireInterrupt();
    }
  }


  private void execCmdReadBuffer()
  {
    this.ioBufPos   = 0;
    this.pendingCmd = Command.READ_BUFFER;
    this.statusReg |= STATUS_DATA_REQUEST;
    fireInterrupt();
  }


  private void execCmdReadPowerMode()
  {
    this.sectorCnt = ((this.powerMode & 0x01) != 0 ? 0xFF : 0);
  }


  private void execCmdReadSectors()
  {
    if( (this.curCmd & 0x06) != 0 ) {	// M- und L-Flag nicht unterstuetzt
      this.errorReg  = ERROR_CMD_ABORTED;
      this.statusReg |= STATUS_ERROR;
    } else {
      this.pendingCmd = Command.READ_SECTORS;
      fireFetchSectors();
    }
  }


  private void execCmdRecalibrate()
  {
    if( this.curDisk != null ) {
      this.cylNum   = 0;
      this.errorReg = 0;
      fireInterrupt();
    } else {
      this.errorReg  = ERROR_TRACK0_NOT_FOUND;
      this.statusReg |= STATUS_ERROR;
    }
  }


  private void execCmdSetDriveParameters()
  {
    if( this.curDiskIdx >= 0 ) {
      this.cylinders[ this.curDiskIdx ]       = this.cylNum;
      this.heads[ this.curDiskIdx ]           = (this.sdhReg & 0x0F) + 1;
      this.sectorsPerTrack[ this.curDiskIdx ] = this.sectorCnt;
    }
  }


  private void execCmdSetPowerMode()
  {
    int mode = this.curCmd & 0x07;
    if( mode != this.powerMode ) {
      this.powerMode = mode;
      fireInterrupt();
    }
  }


  private void execCmdVerifySectors()
  {
    if( (this.curCmd & 0x06) != 0 ) {	// M- und L-Flag nicht unterstuetzt
      this.errorReg  = ERROR_CMD_ABORTED;
      this.statusReg |= STATUS_ERROR;
    } else {
      /*
       * Das Kommando prueft Sektoren auf der Festplatte
       * und meldet ggf. einen fehlerhaften Sektor.
       * In der Emulation erfolgt keine Pruefung,
       * sodass einfach nur die Sektoren weitergezaehlt werden.
       */
      do {
  	countSector();
      } while( this.sectorCnt > 0 );
      fireInterrupt();
    }
  }


  private void execCmdWriteBuffer()
  {
    this.ioBufPos   = 0;
    this.pendingCmd = Command.WRITE_BUFFER;
    this.statusReg |= STATUS_DATA_REQUEST;
  }


  private void execCmdWriteSectors()
  {
    if( (this.curCmd & 0x06) != 0 ) {	// M- und L-Flag nicht unterstuetzt
      this.errorReg  = ERROR_CMD_ABORTED;
      this.statusReg |= STATUS_ERROR;
    } else {
      this.ioBufPos   = 0;
      this.pendingCmd = Command.WRITE_SECTORS;
      this.statusReg |= STATUS_DATA_REQUEST;
    }
  }


  private void execFormatTrackTask()
  {
    File file = this.ioFile;
    long pos  = this.ioFilePos;
    int  cnt  = this.ioByteCnt;
    if( (file != null) && (pos >= 0) && (cnt > 0) ) {
      boolean          err  = false;
      RandomAccessFile raf = null;
      try {
	raf = new RandomAccessFile( file, "rw" );
	raf.seek( pos );
	while( cnt > 0 ) {
	  raf.write( 0 );
	  --cnt;
	}
	raf.close();
	raf = null;
      }
      catch( IOException ex ) {
	err = true;
	if( !this.writeErrShown ) {
	  this.writeErrShown = true;
	  EmuUtil.fireShowError( this.owner, null, ex );
	}
      }
      finally {
	EmuUtil.doClose( raf );
      }
      if( err ) {
	this.errorReg = ERROR_UNCORRECTABLE_DATA;
	this.statusReg |= STATUS_ERROR;
      }
      fireInterrupt();
    }
  }


  private void execReadSectorsTask()
  {
    File file = this.ioFile;
    long pos  = this.ioFilePos;
    if( this.debugLevel > 3 ) {
      System.out.printf( "GIDE io task: read track, pos=%d", pos );
    }
    if( (file != null) && (pos >= 0) ) {
      if( file.exists() ) {
	RandomAccessFile raf = null;
	try {
	  raf = new RandomAccessFile( file, "r" );
	  raf.seek( pos );
	  this.ioByteCnt = (int) raf.read( this.ioBuf, 0, this.ioByteCnt );
	  if( this.debugLevel > 3 ) {
	    System.out.printf(
			"GIDE io task: read sector: %d read\n",
			this.ioByteCnt );
	  }
	}
	catch( IOException ex ) {
	  if( this.debugLevel > 3 ) {
	    System.out.println( "GIDE io task: read sector: error" );
	  }
	  if( !this.readErrShown ) {
	    this.readErrShown = true;
	    EmuUtil.fireShowError(
		this.owner,
		"Die Festplattenabbilddatei kann nicht gelesen"
			+ " werden.\n"
			+ "Gegen\u00FCber dem emulierten System"
			+ " wird jedoch kein Fehler signalisiert.",
		ex );
	  }
	}
	finally {
	  EmuUtil.doClose( raf );
	}
      } else {
	EmuUtil.fireShowError(
		this.owner,
		"Die Festplattenabbilddatei existiert nicht und"
			+ " kann deshalb auch nicht gelesen werden.\n"
			+ "Gegen\u00FCber dem emulierten System"
			+ " wird jedoch kein Fehler signalisiert.\n"
			+ "Mit dem ersten Schreibzugriff auf das"
			+ " emulierte Laufwerk wird die Abbilddatei"
			+ " angelegt.",
		null );
      }
      this.ioBufPos = 0;
      this.statusReg |= STATUS_DATA_REQUEST;
      fireInterrupt();
    }
  }


  private void execWriteSectorsTask()
  {
    File file = this.ioFile;
    long pos  = this.ioFilePos;
    if( this.debugLevel > 3 ) {
      System.out.printf( "GIDE io task: write sector, pos=%d", pos );
    }
    if( (file != null) && (pos >= 0) ) {
      boolean          err  = false;
      RandomAccessFile raf = null;
      try {
	raf = new RandomAccessFile( file, "rw" );
	raf.seek( pos );
	raf.write( this.ioBuf, 0, SECTOR_SIZE );
	raf.close();
	raf = null;
      }
      catch( IOException ex ) {
	err = true;
	if( !this.writeErrShown ) {
	  this.writeErrShown = true;
	  EmuUtil.fireShowError( this.owner, null, ex );
	}
      }
      finally {
	EmuUtil.doClose( raf );
      }
      if( err ) {
	this.errorReg = ERROR_UNCORRECTABLE_DATA;
	this.statusReg |= STATUS_ERROR;
	if( this.debugLevel > 3 ) {
	  System.out.println( "GIDE io task: write sector: error" );
	}
      } else {
	this.ioBufPos = 0;
	countSector();
	if( this.sectorCnt > 0 ) {
	  fireInterrupt();
	} else {
	  this.statusReg = STATUS_SEEK_COMPLETE | STATUS_DRIVE_READY;
	}
	if( this.debugLevel > 3 ) {
	  System.out.println( "GIDE io task: write sector: ok" );
	}
      }
    }
  }


  private void fireFetchSectors()
  {
    if( (this.curDisk != null) && (this.curDiskIdx >= 0) ) {
      long pos = calcFilePos();
      if( pos < 0 ) {
	this.errorReg = ERROR_CMD_ABORTED;
	this.statusReg |= STATUS_ERROR;
      } else {
	Arrays.fill( this.ioBuf, (byte) 0xE5 );
	File file = this.curDisk.getFile();
	if( file != null ) {
	  int nSec = Math.min(
			this.sectorsPerTrack[ this.curDiskIdx ]
						- this.sectorNum + 1,
			this.sectorCnt );
	  startIOTask( file, pos, nSec * SECTOR_SIZE );
	}
      }
    }
  }


  private void fireInterrupt()
  {
    if( this.interruptEnabled && !this.interruptRequest )
      this.interruptRequest = true;
  }


  private static HardDisk[] getHardDisks(
				Properties props,
				String     propPrefix )
  {
    HardDisk[] rv = null;
    if( props != null ) {
      for( int i = 0; i < 2; i++ ) {
	HardDisk disk   = null;
	String   prefix = String.format(
				"%sharddisk.%d.",
				propPrefix,
				i + 1 );
	String diskModel = EmuUtil.getProperty( props, prefix + "model" );
	String fileName  = EmuUtil.getProperty( props, prefix + "file" );
	if( !diskModel.isEmpty() && !fileName.isEmpty() ) {
	  int c = EmuUtil.getIntProperty( props, prefix + "cylinders", 0 );
	  int h = EmuUtil.getIntProperty( props, prefix + "heads", 0 );
	  int n = EmuUtil.getIntProperty(
					props,
					prefix + "sectors_per_track",
					0 );
	  if( (c > 0) && (h > 0) && (n > 0) ) {
	    disk = new HardDisk( diskModel, c, h, n, fileName );
	  }
	}
	if( disk != null ) {
	  if( rv != null ) {
	    HardDisk[] a = new HardDisk[ rv.length + 1 ];
	    System.arraycopy( rv, 0, a, 0, rv.length );
	    rv = a;
	  } else {
	    rv = new HardDisk[ 1 ];
	  }
	  rv[ rv.length - 1 ] = disk;
	}
      }
    }
    return rv;
  }


  private int readDataReg()
  {
    int rv = 0;
    if( (this.statusReg & STATUS_DATA_REQUEST) != 0 ) {
      switch( this.pendingCmd ) {
	case IDENTIFY_DISK:
	case READ_BUFFER:
	  if( this.ioBufPos < SECTOR_SIZE ) {
	    rv = (int) this.ioBuf[ this.ioBufPos++ ] & 0xFF;
	  }
	  if( this.ioBufPos >= SECTOR_SIZE ) {
	    this.pendingCmd = Command.NONE;
	    this.errorReg   = 0;
	    this.statusReg  = STATUS_SEEK_COMPLETE | STATUS_DRIVE_READY;
	  }
	  break;

	case READ_SECTORS:
	  if( (this.ioBufPos >= 0)
	      && (this.ioBufPos < this.ioByteCnt)
	      && (this.ioBufPos < this.ioBuf.length) )
	  {
	    rv = (int) this.ioBuf[ this.ioBufPos ] & 0xFF;
	  }
	  this.ioBufPos++;
	  if( this.ioBufPos >= this.ioByteCnt ) {
	    this.statusReg &= ~STATUS_DATA_REQUEST;
	  }
	  if( (this.ioBufPos % SECTOR_SIZE) == 0 ) {
	    if( countSector() > 0 ) {
	      fireFetchSectors();
	    }
	  }
	  break;
      }
    }
    return rv;
  }


  private boolean sameDisks( HardDisk[] disks )
  {
    boolean rv = false;
    if( (disks != null) && (this.disks != null) ) {
      if( disks.length == this.disks.length ) {
	rv = true;
	for( int i = 0; i < disks.length; i++ ) {
	  if( !disks[ i ].isSameDisk( this.disks[ i ] ) ) {
	    rv = false;
	    break;
	  }
	}
      }
    } else {
      if( (disks == null) && (this.disks == null) ) {
	rv = true;
      }
    }
    return rv;
  }


  private void setIOBufASCII( int pos, String text, int nMax )
  {
    if( (pos >= 0) && (text != null) ) {
      int len  = text.length();
      int pSrc = 0;
      while( (pSrc < len)
	     && ((pos + 1) < this.ioBuf.length)
	     && (nMax > 1) )
      {
	if( (pSrc + 1) < len ) {
	  this.ioBuf[ pos++ ] = (byte) text.charAt( pSrc + 1 );
	} else {
	  this.ioBuf[ pos++ ] = (byte) 0x20;
	}
	this.ioBuf[ pos++ ] = (byte) text.charAt( pSrc );
	pSrc += 2;
	nMax -= 2;
      }
    }
  }


  private void setIOBufWord( int pos, int value )
  {
    if( (pos >= 0) && (pos < (this.ioBuf.length - 1)) ) {
      this.ioBuf[ pos++ ] = (byte) value;
      this.ioBuf[ pos ]   = (byte) (value >> 8);
    }
  }


  private void softReset()
  {
    this.pendingCmd       = Command.NONE;
    this.interruptEnabled = false;
    this.interruptRequest = false;
    this.readErrShown     = false;
    this.writeErrShown    = false;
    this.ioTaskNoWait     = false;
    this.ioFile           = null;
    this.ioFilePos        = -1;
    this.ioByteCnt        = -1;
    this.ioBufPos         = 0;
    this.curCmd           = -1;
    this.curDiskIdx       = -1;
    this.curDisk          = null;
    this.powerMode        = 0x01;	// Bit 0: Idle
    this.sectorCnt        = 0;
    this.sectorNum        = 0;
    this.cylNum           = 0;
    this.sdhReg           = 0;
    this.errorReg         = 0;
    this.statusReg        = 0;
    if( this.disks != null ) {
      this.statusReg = STATUS_SEEK_COMPLETE | STATUS_DRIVE_READY;
    }
  }


  private void startIOTask( File file, long filePos, int byteCnt )
  {
    this.statusReg |= STATUS_BUSY;

    this.ioFile    = file;
    this.ioFilePos = filePos;
    this.ioByteCnt = byteCnt;

    synchronized( this.ioTaskThread ) {
      try {
	this.ioTaskThread.notify();
      }
      catch( IllegalMonitorStateException ex ) {
	this.ioTaskNoWait = true;
      }
    }
  }


  private void writeDataReg( int value )
  {
    if( (this.statusReg & STATUS_DATA_REQUEST) != 0 ) {
      switch( this.pendingCmd ) {
	case FORMAT_TRACK:
	  writeFormatByte( value );
	  break;

	case WRITE_BUFFER:
	  if( this.ioBufPos < SECTOR_SIZE ) {
	    this.ioBuf[ this.ioBufPos++ ] = (byte) value;
	  }
	  if( this.ioBufPos >= this.ioBuf.length ) {
	    this.statusReg = STATUS_SEEK_COMPLETE | STATUS_DRIVE_READY;
	  }
	  break;

	case WRITE_SECTORS:
	  writeSectorByte( value );
	  break;
      }
    }
  }


  private void writeFormatByte( int value )
  {
    if( (this.curDisk != null) && (this.curDiskIdx >= 0) ) {
      if( this.ioBufPos < SECTOR_SIZE ) {
	this.ioBuf[ this.ioBufPos++ ] = (byte) value;
	if( this.ioBufPos == this.ioBuf.length ) {
	  boolean cmdFinished = true;
	  /*
	   * Da das verwendete Dateiformat die Sektornummern nicht speichert,
	   * muessen diese mit eins beginnen und fortlaufend sein.
	   * Nachfolgend wird geprueft, ob diese Bedingung erfuellt ist.
	   */
	  int       spt     = this.sectorsPerTrack[ this.curDiskIdx ];
	  boolean[] sectors = new boolean[ spt ];
	  Arrays.fill( sectors, false );
	  int     ptr = 0;
	  boolean err = false;
	  for( int i = 0; !err && (i < sectors.length); i++ ) {
	    if( (ptr + 1) < SECTOR_SIZE ) {
	      // jeweils 1. Byte muss 00 (good sector) sein
	      if( this.ioBuf[ ptr++ ] != 0 ) {
		err = true;
	      }
	      // jeweils 2. Byte gibt Sektornummer an
	      int v = (int) this.ioBuf[ ptr++ ] & 0xFF;
	      if( v < sectors.length ) {
		if( sectors[ v ] ) {
		  err = true;		// Sektornummer zweimal angegeben
		} else {
		  sectors[ v ] = true;
		}
	      } else {
		err = true;		// Sktornummer ausserhalb des Bereichs
	      }
	    } else {
	      err = true;		// sollte niemals vorkommen
	    }
	  }
	  if( !err ) {
	    for( int i = 0; i < sectors.length; i++ ) {
	      if( !sectors[ i ] ) {
		err = true;		// Sektornummer fehlt
		break;
	      }
	    }
	  }
	  if( err ) {
	    this.errorReg = ERROR_UNCORRECTABLE_DATA;
	    this.statusReg |= STATUS_ERROR;
	  } else {
	    // Sektoren loeschen
	    int  heads   = this.heads[ this.curDiskIdx ];
	    long headNum = this.sdhReg & 0x0F;
	    if( (this.cylNum >= 0)
		&& (this.cylNum < this.cylinders[ this.curDiskIdx ])
		&& (headNum >= 0) && (headNum < heads) )
	    {
	      long sectOffs = (this.cylNum * heads * spt) + (headNum * spt);
	      startIOTask(
		      this.curDisk.getFile(),
		      0x0100 + (sectOffs * ((long) SECTOR_SIZE)),
		      spt * SECTOR_SIZE );
	      cmdFinished = false;
	    }
	  }
	  if( cmdFinished ) {
	    fireInterrupt();
	  }
	}
      }
    }
  }


  private void writeSectorByte( int value )
  {
    if( (this.curDisk != null) && (this.ioBufPos < this.ioBuf.length) ) {
      this.ioBuf[ this.ioBufPos++ ] = (byte) value;
      if( this.ioBufPos >= this.ioBuf.length ) {
	this.statusReg &= ~STATUS_DATA_REQUEST;
      }
      if( this.ioBufPos == SECTOR_SIZE ) {
	long pos = calcFilePos();
	if( (pos >= 0) || (this.sectorNum >= 1) ) {
	  startIOTask( this.curDisk.getFile(), pos, SECTOR_SIZE );
	} else {
	  this.errorReg = ERROR_CMD_ABORTED;
	  this.statusReg |= STATUS_ERROR;
	}
      }
    }
  }
}
