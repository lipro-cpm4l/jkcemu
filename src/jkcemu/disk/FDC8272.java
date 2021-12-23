/*
 * (c) 2009-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Floppy-Disk-Controllers U8272
 * (kompatibel zu Intel 8272A)
 */

package jkcemu.disk;

import java.util.Arrays;
import jkcemu.Main;
import jkcemu.base.EmuThread;
import z80emu.Z80CPU;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class FDC8272 implements
			Runnable,
			Z80MaxSpeedListener,
			Z80TStatesListener
{
  public interface DriveSelector
  {
    public FloppyDiskDrive getFloppyDiskDrive( int driveNum );
  };


  private enum Command {
		FORMAT_TRACK,
		READ_DATA,
		READ_DELETED_DATA,
		READ_ID,
		READ_TRACK,
		RECALIBRATE,
		SCAN_EQUAL,
		SCAN_LOW_OR_EQUAL,
		SCAN_HIGH_OR_EQUAL,
		SEEK,
		SENSE_DRIVE_STATUS,
		SENSE_INTERRUPT_STATUS,
		SPECIFY,
		WRITE_DATA,
		WRITE_DELETED_DATA,
		INVALID };

  private static Command[] cmds = {
		Command.INVALID,			// 0 0 0 0 0
		Command.INVALID,			// 0 0 0 0 1
		Command.READ_TRACK,			// 0 0 0 1 0
		Command.SPECIFY,			// 0 0 0 1 1
		Command.SENSE_DRIVE_STATUS,		// 0 0 1 0 0
		Command.WRITE_DATA,			// 0 0 1 0 1
		Command.READ_DATA,			// 0 0 1 1 0
		Command.RECALIBRATE,			// 0 0 1 1 1
		Command.SENSE_INTERRUPT_STATUS,		// 0 1 0 0 0
		Command.WRITE_DELETED_DATA,		// 0 1 0 0 1
		Command.READ_ID,			// 0 1 0 1 0
		Command.INVALID,			// 0 1 0 1 1
		Command.READ_DELETED_DATA,		// 0 1 1 0 0
		Command.FORMAT_TRACK,			// 0 1 1 0 1
		Command.INVALID,			// 0 1 1 1 0
		Command.SEEK,				// 0 1 1 1 1
		Command.INVALID,			// 1 0 0 0 0
		Command.SCAN_EQUAL,			// 1 0 0 0 1
		Command.INVALID,			// 1 0 0 1 0
		Command.INVALID,			// 1 0 0 1 1
		Command.INVALID,			// 1 0 1 0 0
		Command.INVALID,			// 1 0 1 0 1
		Command.INVALID,			// 1 0 1 1 0
		Command.INVALID,			// 1 0 1 1 1
		Command.INVALID,			// 1 1 0 0 0
		Command.SCAN_LOW_OR_EQUAL,		// 1 1 0 0 1
		Command.INVALID,			// 1 1 0 1 0
		Command.INVALID,			// 1 1 0 1 1
		Command.INVALID,			// 1 1 1 0 0
		Command.SCAN_HIGH_OR_EQUAL,		// 1 1 1 0 1
		Command.INVALID,			// 1 1 1 1 0
		Command.INVALID };			// 1 1 1 1 1

  private enum FormatStatus {
			IDLE,
			WAIT_FOR_HOLE,
			RECEIVE_DATA,
			BUSY };

  private enum IOTaskCmd {
			IDLE,
			FORMAT_TRACK,
			READ_SECTOR_BY_ID,
			READ_SECTOR_BY_INDEX,
			READ_SECTOR_FOR_WRITE,
			WRITE_SECTOR };

  private static final int ARG0_SK_MASK    = 0x20;
  private static final int ARG0_MT_MASK    = 0x80;
  private static final int DRIVE_MASK      = 0x03;
  private static final int HEAD_MASK       = 0x04;
  private static final int HEAD_DRIVE_MASK = HEAD_MASK | DRIVE_MASK;

  private static final int STM_REQUEST_FOR_MASTER          = 0x80;
  private static final int STM_DATA_INPUT                  = 0x40;
  private static final int STM_NON_DMA_MODE                = 0x20;
  private static final int STM_BUSY                        = 0x10;
  private static final int STM_DRIVE_MASK                  = 0x0F;
  private static final int ST0_ERROR_MASK                  = 0xC0;
  private static final int ST0_ABORT_BECAUSE_READY_CHANGED = 0xC0;
  private static final int ST0_INVALID_COMMAND_ISSUE       = 0x80;
  private static final int ST0_ABNORMAL_TERMINATION        = 0x40;
  private static final int ST0_SEEK_END                    = 0x20;
  private static final int ST0_EQUIPMENT_CHECK             = 0x10;
  private static final int ST0_NOT_READY                   = 0x08;
  private static final int ST1_END_OF_CYLINDER             = 0x80;
  private static final int ST1_DATA_ERROR                  = 0x20;
  private static final int ST1_OVERRUN                     = 0x10;
  private static final int ST1_NO_DATA                     = 0x04;
  private static final int ST1_NOT_WRITABLE                = 0x02;
  private static final int ST1_MISSING_ADDRESS_MARK        = 0x01;
  private static final int ST2_CONTROL_MARK                = 0x40;
  private static final int ST2_DATA_ERROR_IN_DATA_FIELD    = 0x20;
  private static final int ST2_SCAN_EQUAL_HIT              = 0x08;
  private static final int ST2_SCAN_NOT_SATISFIED          = 0x04;
  private static final int ST2_MISSING_DATA_ADDRESS_MARK   = 0x01;
  private static final int ST3_WRITE_PROTECTED             = 0x40;
  private static final int ST3_READY                       = 0x20;
  private static final int ST3_TRACK_0                     = 0x10;
  private static final int ST3_TWO_SIDE                    = 0x08;

  private DriveSelector       driveSelector;
  private FloppyDiskDrive     executingDrive;
  private volatile Command    curCmd;
  private FormatStatus        formatStatus;
  private Object              ioLock;
  private volatile IOTaskCmd  ioTaskCmd;
  private volatile Thread     ioTaskThread;
  private volatile boolean    ioTaskEnabled;
  private volatile boolean    ioTaskNoWait;
  private volatile boolean    tcEnabled;
  private volatile boolean    tcFired;
  private volatile boolean    interruptReq;
  private volatile boolean    dmaReq;
  private boolean             dmaMode;
  private boolean             hdMode;
  private boolean             hdPossible;
  private boolean             seekMode;
  private boolean             eotReached;
  private volatile int[]      args;
  private int                 argIdx;
  private int[]               results;
  private int                 resultIdx;
  private int                 sectorIdCyl;
  private int                 sectorIdHead;
  private int                 sectorIdRec;
  private int                 sectorIdSizeCode;
  private int                 mhz;
  private volatile int        statusRegMain;
  private int                 statusReg0;
  private int                 statusReg1;
  private int                 statusReg2;
  private int                 statusReg3;
  private int                 stepRateMillis;
  private int                 tStatesTillIOReq;
  private int                 tStatesTillIOStart;
  private int                 tStatesTillOverrun;
  private int                 tStateRotationCounter;
  private int                 tStateStepCounter;
  private volatile int        tStatesPerMilli;
  private volatile int        tStatesPerRotation;
  private volatile int        tStatesPerStep;
  private volatile int        debugLevel;
  private int[]               seekStatus;
  private int[]               remainSeekSteps;
  private byte[]              dataBuf;
  private int                 dataPos;
  private int                 dataLen;
  private int                 remainBytes;
  private int                 curSectorIdx;
  private volatile SectorData curSector;
  private SectorData.Reader   curSectorReader;


  public FDC8272( DriveSelector driveSelector, int mhz )
  {
    this.driveSelector      = driveSelector;
    this.mhz                = mhz;
    this.curCmd             = Command.INVALID;
    this.executingDrive     = null;
    this.dmaMode            = false;	// wird von RESET nicht beeinflusst
    this.hdMode             = false;
    this.hdPossible         = false;
    this.stepRateMillis     = 16;	// wird von RESET nicht beeinflusst
    this.tStatesPerMilli    = 0;
    this.tStatesPerRotation = 0;
    this.tStatesPerStep     = 0;
    this.debugLevel         = 0;
    this.dataBuf            = null;
    this.args               = new int[ 9 ];
    this.results            = new int[ 7 ];
    this.remainSeekSteps    = new int[ 4 ];
    this.seekStatus         = new int[ 4 ];
    this.ioLock             = new Object();
    this.ioTaskCmd          = IOTaskCmd.IDLE;
    this.ioTaskEnabled      = true;
    this.ioTaskNoWait       = false;
    this.ioTaskThread       = new Thread(
					Main.getThreadGroup(),
					this,
					"JKCEMU FDC" );

    String text = System.getProperty( "jkcemu.debug.fdc" );
    if( text != null ) {
      try {
	this.debugLevel = Integer.parseInt( text );
      }
      catch( NumberFormatException ex ) {}
    }
    this.ioTaskThread.start();
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


  public void fireTC()
  {
    if( this.debugLevel > 1 ) {
      System.out.println( "FDC: TC" );
    }
    this.tStatesTillIOReq   = 0;
    this.tStatesTillIOStart = 0;
    this.tStatesTillOverrun = 0;
    switch( this.curCmd ) {
      case FORMAT_TRACK:
	this.statusRegMain &= ~STM_REQUEST_FOR_MASTER;
	switch( this.formatStatus ) {
	  case WAIT_FOR_HOLE:
	    stopExecution();
	    break;
	  case RECEIVE_DATA:
	    // Kommando wird gerade zu Ende gebracht -> nichts tun
	    break;
	  default:
	    setIdle();
	    break;
	}
	break;
      case READ_DATA:
      case READ_DELETED_DATA:
      case READ_TRACK:
      case SCAN_EQUAL:
      case SCAN_LOW_OR_EQUAL:
      case SCAN_HIGH_OR_EQUAL:
	if( this.tcEnabled ) {
	  stopExecution();
	}
	break;

      case WRITE_DATA:
      case WRITE_DELETED_DATA:
	synchronized( this.ioTaskThread ) {
	  if( this.tcEnabled ) {
	    this.tcFired   = true;
	    this.tcEnabled = false;
	    this.dmaReq    = false;
	    this.statusRegMain &= ~STM_REQUEST_FOR_MASTER;
	    this.statusRegMain &= ~STM_DATA_INPUT;
	    this.statusRegMain &= ~STM_NON_DMA_MODE;
	    /*
	     * Die folgenden Werte zur Sicherheit nochmal zuruecksetzen.
	     * Sie koennten kurz vor Eintritt in den synchronized-Block
	     * von dem FDC-Thread wieder gesetzt worden sein.
	     */
	    this.tStatesTillIOReq   = 0;
	    this.tStatesTillIOStart = 0;
	    this.tStatesTillOverrun = 0;
	    /*
	     * FDC-Thread aufwachen, um das Kommando zu beenden.
	     * Dabei muss ggf. noch der letzte Sektor geschrieben werden.
	     */
	    this.ioTaskCmd = IOTaskCmd.WRITE_SECTOR;
	    try {
	      this.ioTaskThread.notify();
	    }
	    catch( IllegalMonitorStateException ex ) {
	      this.ioTaskNoWait = true;
	    }
	  }
	}
	break;
    }
  }


  /*
   * Der Index-Loch-Impuls wird hier im Zeitverhaeltnis
   * von 1:100 nachgebildet.
   */
  public boolean getIndexHoleState()
  {
    return this.tStateRotationCounter < (this.tStatesPerRotation / 100);
  }


  public boolean isDMARequest()
  {
    return this.dmaReq;
  }


  public boolean isInterruptRequest()
  {
    return this.interruptReq;
  }


  public int readMainStatusReg()
  {
    int rv = this.statusRegMain;
    if( this.debugLevel > 2 ) {
      System.out.printf( "FDC: read status: %02X\n", rv );
    }
    this.interruptReq = false;
    return rv;
  }


  public int readData()
  {
    int rv = -1;
    if( !this.dmaMode ) {
      rv = readFromDisk();
    }
    if( rv == -1 ) {
      rv = this.statusRegMain;
      if( (this.resultIdx >= 0) && (this.resultIdx < this.results.length) ) {
	rv = this.results[ this.resultIdx ];
	--this.resultIdx;
	if( this.resultIdx < 0 ) {
	  setIdle();
	}
      }
    }
    this.interruptReq = false;
    if( this.debugLevel > 1 ) {
      System.out.printf( "FDC: read data: %02X\n", rv );
    }
    return rv;
  }


  public int readDMA()
  {
    int rv = -1;
    this.dmaReq = false;
    if( this.dmaMode ) {
      rv = readFromDisk();
    }
    if( this.debugLevel > 1 ) {
      System.out.printf( "FDC: read dma: %02X\n", rv );
    }
    return rv;
  }


  public void reset( boolean powerOn )
  {
    if( this.debugLevel > 0 ) {
      System.out.println( "FDC: reset" );
    }
    if( powerOn ) {
      this.dmaMode        = false;
      this.stepRateMillis = 16;
    }
    this.executingDrive        = null;
    this.hdPossible            = false;
    this.hdMode                = false;
    this.seekMode              = false;
    this.ioTaskNoWait          = false;
    this.tcEnabled             = false;
    this.tcFired               = false;
    this.dmaReq                = false;
    this.interruptReq          = false;
    this.statusRegMain         = STM_REQUEST_FOR_MASTER;
    this.statusReg3            = 0;
    this.formatStatus          = FormatStatus.IDLE;
    this.ioTaskCmd             = IOTaskCmd.IDLE;
    this.curSector             = null;
    this.curSectorReader       = null;
    this.curSectorIdx          = -1;
    this.dataPos               = -1;
    this.dataLen               = 0;
    this.remainBytes           = 0;
    this.tStatesTillIOReq      = 0;
    this.tStatesTillIOStart    = 0;
    this.tStatesTillOverrun    = 0;
    this.tStateRotationCounter = 0;
    this.tStateStepCounter     = 0;
    Arrays.fill( this.seekStatus, -1 );
    clearSectorID();
    clearRegs012();
    Arrays.fill( this.args, 0 );
    Arrays.fill( this.results, 0 );
    Arrays.fill( this.remainSeekSteps, 0 );
    setIdle();
  }


  public void setHDMode( boolean state )
  {
    this.hdMode = (state && this.hdPossible);
  }


  public void setTStatesPerMilli( int tStatesPerMilli )
  {
    this.tStatesPerMilli = tStatesPerMilli;

    /*
     * Emulation der Indexlochimpulse,
     * 300 Umdrehungen pro Minute ergeben 50 Umdrehungen pro Sekunde,
     * -> (Frequenz_in_kHz * 1000 / 50)
     *    und (Frequenz_in_kHz == tStatesPerMilli)
     * -> tStates_pro_Umdrehung == (tStatesPerMilli * 20)
     */
    this.tStatesPerRotation = tStatesPerMilli * 20;
    calcTStatesPerStep();
  }


  public void write( int value )
  {
    if( this.debugLevel > 1 ) {
      System.out.printf( "FDC: write: %02X\n", value );
    }
    if( !this.dmaMode && (this.executingDrive != null) ) {
      writeToDrive( value );
    } else {
      writeCmd( value );
    }
  }


  public void writeDMA( int value )
  {
    if( this.debugLevel > 1 ) {
      System.out.printf( "FDC: write dma: %02X\n", value );
    }
    this.dmaReq = false;
    if( this.dmaMode ) {
      writeToDrive( value );
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    while( this.ioTaskEnabled ) {
      synchronized( this.ioTaskThread ) {
	if( ((this.curCmd == Command.WRITE_DATA)
		|| (this.curCmd == Command.WRITE_DELETED_DATA) )
	    && this.tcFired )
	{
	  stopExecution();
	}
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
	IOTaskCmd ioTaskCmd = IOTaskCmd.IDLE;
	synchronized( this.ioTaskThread ) {
	  ioTaskCmd      = this.ioTaskCmd;
	  this.ioTaskCmd = IOTaskCmd.IDLE;
	}
	switch( ioTaskCmd ) {
	  case FORMAT_TRACK:
	    execIOFormatTrack();
	    break;
	  case READ_SECTOR_BY_ID:
	    execIOReadSectorByID();
	    break;
	  case READ_SECTOR_BY_INDEX:
	    execIOReadSectorByIndex();
	    break;
	  case READ_SECTOR_FOR_WRITE:
	    execIOReadSectorForWrite();
	    break;
	  case WRITE_SECTOR:
	    execIOWriteSector();
	    break;
	}
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    int maxSpeedKHz = cpu.getMaxSpeedKHz();
    this.hdPossible = (maxSpeedKHz > 5000);
    if( !this.hdPossible ) {
      this.hdMode = false;
    }
    setTStatesPerMilli( maxSpeedKHz );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    synchronized( this.ioTaskThread ) {
      if( this.tStatesTillIOStart > 0 ) {
	this.tStatesTillIOStart -= tStates;
	if( (this.tStatesTillIOStart <= 0)
	    && (this.ioTaskCmd != IOTaskCmd.IDLE) )
	{
	  if( this.eotReached ) {
	    this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	    this.statusReg1 |= ST1_END_OF_CYLINDER;
	    stopExecution();
	    this.eotReached = false;
	  } else {
	    try {
	      this.ioTaskThread.notify();
	    }
	    catch( IllegalMonitorStateException ex ) {
	      this.ioTaskNoWait = true;
	    }
	  }
	}
      }
    }
    if( this.tStatesTillIOReq > 0 ) {
      this.tStatesTillIOReq -= tStates;
      if( this.tStatesTillIOReq <= 0 ) {
	switch( this.curCmd ) {
	  case FORMAT_TRACK:
	    setByteWritable( false );
	    break;
	  case READ_DATA:
	  case READ_DELETED_DATA:
	  case READ_TRACK:
	    setByteReadable();
	    break;
	  case SCAN_EQUAL:
	  case SCAN_LOW_OR_EQUAL:
	  case SCAN_HIGH_OR_EQUAL:
	  case WRITE_DATA:
	  case WRITE_DELETED_DATA:
	    setByteWritable( true );
	    break;
	}
      }
    }
    this.tStateRotationCounter += tStates;
    if( (this.tStatesPerRotation > 0)
	&& (this.tStateRotationCounter >= this.tStatesPerRotation) )
    {
      this.tStateRotationCounter = 0;
      if( this.curCmd == Command.FORMAT_TRACK ) {
	switch( this.formatStatus ) {
	  case WAIT_FOR_HOLE:
	    this.formatStatus = FormatStatus.RECEIVE_DATA;
	    this.dataPos = 0;
	    setByteWritable( false );
	    break;
	  case RECEIVE_DATA:
	    startIOTask( IOTaskCmd.FORMAT_TRACK, 0 );
	    this.formatStatus = FormatStatus.BUSY;
	    break;
	}
      }
    }
    if( this.seekMode ) {
      this.tStateStepCounter += tStates;
      if( this.tStateStepCounter >= this.tStatesPerStep ) {
	this.tStateStepCounter = 0;
	execSeekStep();
      }
    }
    if( this.tStatesTillOverrun > 0 ) {
      this.tStatesTillOverrun -= tStates;
      if( (this.tStatesTillOverrun <= 0) && (getExecutingDrive() != null) ) {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	this.statusReg1 |= ST1_OVERRUN;
      }
    }
  }


	/* --- private Methoden --- */

  private void addSectorNum( int value )
  {
    if( this.sectorIdRec == this.args[ 6 ] ) {
      if( (this.args[ 0 ] & ARG0_MT_MASK) != 0 ) {
	// Multi Track
	if( (this.args[ 1 ] & HEAD_MASK) == 0 ) {
	  // Seite 0
	  this.sectorIdRec = 1;
	  this.args[ 1 ] |= HEAD_MASK;
	} else {
	  // Seite 1
	  this.eotReached  = true;
	  this.sectorIdRec = 1;
	  this.sectorIdCyl++;
	  this.args[ 1 ] &= ~HEAD_MASK;
	}
      } else {
	this.eotReached  = true;
	this.sectorIdRec = 1;
	this.sectorIdCyl++;
      }
    } else {
      this.sectorIdRec += value;
    }
  }


  private void calcTStatesPerStep()
  {
    this.tStatesPerStep = this.stepRateMillis
				* this.tStatesPerMilli
				/ this.mhz;
  }


  private void clearRegs012()
  {
    this.statusReg0 = 0;
    this.statusReg1 = 0;
    this.statusReg2 = 0;
  }


  private void clearSectorID()
  {
    this.sectorIdCyl      = 0;
    this.sectorIdHead     = 0;
    this.sectorIdRec      = 0;
    this.sectorIdSizeCode = 0;
    this.statusReg0       = 0;
    this.statusReg1       = 0;
    this.statusReg2       = 0;
  }


  private void execIOFormatTrack()
  {
    boolean         done   = false;
    int             srcIdx = 0;
    FloppyDiskDrive drive  = getExecutingDrive();
    if( drive != null ) {
      AbstractFloppyDisk disk = drive.getDisk();
      if( disk != null ) {
	if( (disk.isHD() == this.hdMode)
	    && (this.dataBuf != null) )
	{
	  int nSectors = this.dataPos / 4;
	  if( (nSectors > 0) && ((nSectors * 4) <= this.dataBuf.length) ) {
	    int n          = this.args[ 2 ] & 0x0F;
	    int sectorSize = 128;
	    if( n > 0 ) {
	      sectorSize = (128 << n);
	    }
	    byte[] contentBuf = new byte[ sectorSize ];
	    Arrays.fill( contentBuf, (byte) this.args[ 5 ] );

	    SectorID[] sectors = new SectorID[ nSectors ];
	    int        dstIdx  = 0;
	    while( ((srcIdx + 3) < this.dataBuf.length)
		   && (dstIdx < sectors.length) )
	    {
	      int cyl             = (int) this.dataBuf[ srcIdx++ ] & 0xFF;
	      int head            = (int) this.dataBuf[ srcIdx++ ] & 0xFF;
	      int rec             = (int) this.dataBuf[ srcIdx++ ] & 0xFF;
	      int sizeCode        = (int) this.dataBuf[ srcIdx++ ] & 0xFF;
	      sectors[ dstIdx++ ] = new SectorID( cyl, head, rec, sizeCode );
	    }
	    done = drive.formatTrack(
			  (this.args[ 1 ] >> 2) & 0x01,
			  sectors,
			  contentBuf );
	  }
	}
      }
    }
    drive = getExecutingDrive();
    if( drive != null ) {
      if( this.dataBuf.length >= 4 ) {
	this.sectorIdCyl      = (int) this.dataBuf[ 0 ] & 0xFF;
	this.sectorIdHead     = (int) this.dataBuf[ 0 ] & 0xFF;
	this.sectorIdRec      = (int) this.dataBuf[ 0 ] & 0xFF;
	this.sectorIdSizeCode = (int) this.dataBuf[ 0 ] & 0xFF;
      }
      if( done ) {
	srcIdx -= 4;
	if( (srcIdx + 3) < this.dataBuf.length ) {
	  this.sectorIdCyl      = (int) this.dataBuf[ srcIdx++ ] & 0xFF;
	  this.sectorIdHead     = (int) this.dataBuf[ srcIdx++ ] & 0xFF;
	  this.sectorIdRec      = ((int) this.dataBuf[ srcIdx++ ] & 0xFF) + 1;
	  this.sectorIdSizeCode = (int) this.dataBuf[ srcIdx ] & 0xFF;
	}
      } else {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	if( this.executingDrive.isReadOnly() ) {
	  this.statusReg1 |= ST1_NOT_WRITABLE;
	} else {
	  this.statusReg1 |= ST1_DATA_ERROR;
	  this.statusReg2 |= ST2_DATA_ERROR_IN_DATA_FIELD;
	}
      }
      stopExecution();
    }
  }


  private void execIOReadSectorByID()
  {
    boolean         cmAbort     = false;
    boolean         cylReadable = false;
    SectorData      sector      = null;
    FloppyDiskDrive drive       = getExecutingDrive();
    if( drive != null ) {
      AbstractFloppyDisk disk = drive.getDisk();
      if( disk != null ) {
	int head = getArgHead();
	if( (disk.isHD() == this.hdMode)
	    && (disk.getSectorsOfTrack( drive.getCylinder(), head ) > 0) )
	{
	  cylReadable = true;
	  if( disk.supportsDeletedDataSectors() ) {
	    int startIdx = getSectorIndexByCurHeadPos( drive );
	    if( startIdx >= 0 ) {
	      boolean loop   = true;
	      int     endIdx = -1;
	      int     curIdx = startIdx;
	      while( loop && ((curIdx < endIdx) || (endIdx < 0)) ) {
		sector = drive.readSectorByIndex( head, curIdx );
		if( (sector == null) && (startIdx > 0) ) {
		  // zum Spuranfang springen
		  endIdx   = startIdx;
		  startIdx = 0;
		  curIdx   = 0;
		} else {
		  loop = false;
		  if( sector != null ) {
		    if( this.curCmd == Command.READ_DELETED_DATA ) {
		      if( !sector.getDataDeleted() ) {
			this.statusReg2 |= ST2_CONTROL_MARK;
			if( (this.args[ 0 ] & ARG0_SK_MASK) != 0 ) {
			  curIdx++;
			  loop = true;
			} else {
			  cmAbort = true;
			}
		      }
		    } else {
		      if( sector.getDataDeleted() ) {
			this.statusReg2 |= ST2_CONTROL_MARK;
			if( (this.args[ 0 ] & ARG0_SK_MASK) != 0 ) {
			  curIdx++;
			  loop = true;
			} else {
			  cmAbort = true;
			}
		      }
		    }
		    if( !loop
			&& ((sector.getCylinder() != this.sectorIdCyl)
			    || (sector.getHead() != this.sectorIdHead)
			    || (sector.getSectorNum() != this.sectorIdRec)
			    || (sector.getSizeCode()
					!= this.sectorIdSizeCode)) )
		    {
		      curIdx++;
		      loop = true;
		    }
		  }
		}
	      }
	    }
	  } else {
	    sector = drive.readSectorByID( 
				head,
				0,
				this.sectorIdCyl,
				this.sectorIdHead,
				this.sectorIdRec,
				this.sectorIdSizeCode );
	  }
	}
      }
    }
    drive = getExecutingDrive();
    if( drive != null ) {
      if( sector != null ) {
	if( sector.checkError() ) {
	  this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	  this.statusReg1 |= ST1_DATA_ERROR;
	  this.statusReg2 |= ST2_DATA_ERROR_IN_DATA_FIELD;
	}
	this.curSector       = sector;
	this.curSectorReader = sector.reader();
	this.remainBytes     = this.dataLen;
	startIOReqTimer();
      } else {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	if( !cmAbort ) {
	  if( cylReadable ) {
	    if( (this.sectorIdCyl == this.args[ 2 ])
		&& (this.sectorIdHead == this.args[ 3 ])
		&& (this.sectorIdRec == this.args[ 4 ]) )
	    {
	      this.statusReg1 |= ST1_NO_DATA;
	    } else {
	      this.statusReg1 |= ST1_END_OF_CYLINDER;
	    }
	  } else {
	    this.statusReg1 |= ST1_MISSING_ADDRESS_MARK;
	  }
	}
	stopExecution();
      }
    }
  }


  private void execIOReadSectorByIndex()
  {
    boolean         cylReadable = false;
    SectorData      sector      = null;
    FloppyDiskDrive drive       = this.executingDrive;
    if( drive != null ) {
      AbstractFloppyDisk disk = drive.getDisk();
      if( disk != null ) {
	int head = getArgHead();
	if( (disk.isHD() == this.hdMode)
	    && (disk.getSectorsOfTrack( drive.getCylinder(), head ) > 0) )
	{
	  cylReadable = true;
	  sector      = drive.readSectorByIndex( head, this.sectorIdRec - 1 );
	}
      }
    }
    drive = getExecutingDrive();
    if( drive != null ) {
      if( sector != null ) {
	this.curSector       = sector;
	this.curSectorReader = sector.reader();
	this.remainBytes     = this.dataLen;
	if( this.curCmd == Command.READ_TRACK ) {
	  setByteReadable();
	} else {
	  this.sectorIdCyl      = sector.getCylinder();
	  this.sectorIdHead     = sector.getHead();
	  this.sectorIdRec      = sector.getSectorNum();
	  this.sectorIdSizeCode = sector.getSizeCode();
	  stopExecution();
	}
      } else {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	if( cylReadable ) {
	  if( this.sectorIdRec == 1 ) {
	    this.statusReg1 |= ST1_NO_DATA;
	  } else {
	    this.statusReg1 |= ST1_END_OF_CYLINDER;
	  }
	} else {
	  this.statusReg1 |= ST1_MISSING_ADDRESS_MARK;
	}
	stopExecution();
      }
    }
  }


  private void execIOReadSectorForWrite()
  {
    boolean         cylReadable = false;
    SectorData      sector      = null;
    FloppyDiskDrive drive       = this.executingDrive;
    if( drive != null ) {
      AbstractFloppyDisk disk = drive.getDisk();
      if( disk != null ) {
	int head = getArgHead();
	if( (disk.isHD() == this.hdMode)
	    && (disk.getSectorsOfTrack( drive.getCylinder(), head ) > 0) )
	{
	  cylReadable = true;
	  sector      = drive.readSectorByID( 
				head,
				0,
				this.sectorIdCyl,
				this.sectorIdHead,
				this.sectorIdRec,
				this.sectorIdSizeCode );
	}
      }
    }
    drive = getExecutingDrive();
    if( drive != null ) {
      if( sector != null ) {
	this.curSector = sector;
	this.dataPos   = 0;
	setByteWritable( true );
      } else {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	if( cylReadable ) {
	  this.statusReg1 |= ST1_NO_DATA;
	} else {
	  this.statusReg1 |= ST1_MISSING_ADDRESS_MARK;
	}
	stopExecution();
      }
    }
  }


  private void execIOWriteSector()
  {
    FloppyDiskDrive drive  = this.executingDrive;
    boolean         done   = false;
    SectorData      sector = this.curSector;
    if( drive != null ) {
      AbstractFloppyDisk disk = drive.getDisk();
      if( disk != null ) {
	if( (disk.isHD() == this.hdMode)
	    && (sector != null)
	    && (this.dataBuf != null)
	    && (this.dataPos >= 0) )
	{
	  this.curSector = null;
	  while( (this.dataPos < this.dataLen)
		 && (this.dataPos < this.dataBuf.length) )
	  {
	    this.dataBuf[ this.dataPos++ ] = (byte) 0;
	  }
	  done = drive.writeSector(
			getArgHead(),
			sector,
			this.dataBuf,
			this.dataLen,
			this.curCmd == Command.WRITE_DELETED_DATA );
	  this.dataPos = -1;
	  drive        = this.executingDrive;
	  if( drive != null ) {
	    if( done ) {
	      incSectorNum();
	      synchronized( this.ioTaskThread ) {
		if( this.tcFired ) {
		  stopExecution();
		} else {
		  startIOTask(
			IOTaskCmd.READ_SECTOR_FOR_WRITE,
			this.tStatesPerRotation );
		}
	      }
	    } else {
	      this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	      if( drive.isReadOnly() ) {
		this.statusReg1 |= ST1_NOT_WRITABLE;
	      } else {
		this.statusReg1 |= ST1_DATA_ERROR;
		this.statusReg2 |= ST2_DATA_ERROR_IN_DATA_FIELD;
	      }
	      stopExecution();
	    }
	  }
	} else {
	  if( this.tcFired ) {
	    stopExecution();
	  }
	}
      }
    }
  }


  /*
   * Die Methode fuehrt einen Schrittimpuls aus.
   * Bei Erreichen des Zielzylinders werden die entsprechenden Bits
   * im Hauptstatusregister hier noch nicht zurueckgesetzt,
   * sondern erst beim Sense-Interrupt-Status-Befehl.
   */
  private void execSeekStep()
  {
    boolean seekMode  = false;
    int     driveMask = 0x01;
    for( int i = 0; i < this.remainSeekSteps.length; i++ ) {
      boolean driveSeekMode = false;
      if( this.remainSeekSteps[ i ] > 0 ) {
	FloppyDiskDrive drive = getDrive( i );
	if( drive != null ) {
	  --this.remainSeekSteps[ i ];
	  if( drive.seekStep() ) {
	    this.seekStatus[ i ] |= ST0_SEEK_END;
	    this.interruptReq = true;
	  } else {
	    if( this.remainSeekSteps[ i ] > 0 ) {
	      driveSeekMode = true;
	    } else {
	      this.seekStatus[ i ] |= ST0_ABNORMAL_TERMINATION;
	      this.seekStatus[ i ] |= ST0_SEEK_END;
	      this.seekStatus[ i ] |= ST0_EQUIPMENT_CHECK;
	      this.interruptReq = true;
	    }
	  }
	} else {
	  this.seekStatus[ i ] |= ST0_ABNORMAL_TERMINATION;
	  this.seekStatus[ i ] |= ST0_SEEK_END;
	  this.seekStatus[ i ] |= ST0_NOT_READY;
	  this.interruptReq = true;
	}
      }
      if( driveSeekMode ) {
	seekMode = true;
      } else {
	this.remainSeekSteps[ i ] = 0;
      }
      driveMask <<= 1;
    }
    this.seekMode = seekMode;
  }


  private void execSenseDriveStatus()
  {
    this.statusReg3       = (this.args[ 1 ] & HEAD_DRIVE_MASK);
    FloppyDiskDrive drive = getArgDrive();
    if( drive != null ) {
      this.statusReg3 |= ST3_TWO_SIDE;
      if( drive.getCylinder() == 0 ) {
	this.statusReg3 |= ST3_TRACK_0;
      }
      if( drive.isReady() ) {
	this.statusReg3 |= ST3_READY;
      }
      if( drive.isReadOnly() ) {
	this.statusReg3 |= ST3_WRITE_PROTECTED;
      }
    }
    this.results[ 0 ] = this.statusReg3;
    this.resultIdx    = 0;
    setResultMode();
  }


  private void execSenseInterruptStatus()
  {
    this.results[ 0 ] = 0;		// Zylinder
    this.results[ 1 ] = 0x80;		// Invalid Command Issue

    int driveMask = 0x01;
    for( int i = 0; i < this.seekStatus.length; i++ ) {
      int v = this.seekStatus[ i ];
      if( v >= 0 ) {
	if( (v & 0xF8) != 0 ) {
	  this.results[ 1 ] = v | i;
	  FloppyDiskDrive drive = getDrive( i );
	  if( drive != null ) {
	    this.results[ 0 ] = drive.getCylinder();
	  }
	  this.statusRegMain &= ~driveMask;
	  this.seekStatus[ i ] = -1;
	  break;
	}
	this.results[ 1 ] = 0;
      }
      driveMask <<= 1;
    }
    this.resultIdx = 1;
    setResultMode();
  }


  private int getSectorIndexByCurHeadPos( FloppyDiskDrive drive )
  {
    int idx = -1;
    if( drive != null ) {
      AbstractFloppyDisk disk = drive.getDisk();
      if( disk != null ) {
	int head = getArgHead();
	int cyl  = drive.getCylinder();
	int spc  = disk.getSectorsOfTrack( cyl, head );
	int tpr  = this.tStatesPerRotation;
	if( (spc > 0) && (tpr > 0)
	    && (head < disk.getSides())
	    && (cyl < disk.getCylinders()) )
	{
	  idx = Math.round( (float) this.tStateRotationCounter
							/ (float) tpr
							* (float) spc );
	  if( idx >= spc ) {
	    idx = spc - 1;
	  }
	}
	if( idx < 0 ) {
	  idx = 0;
	}
      }
    }
    return idx;
  }


  private int getArgDataLen()
  {
    int sizeCode = (this.args[ 5 ] & 0x0F);
    int dataLen  = this.args[ 8 ];
    if( sizeCode > 0 ) {
      dataLen = (128 << sizeCode);
    }
    return dataLen;
  }


  private FloppyDiskDrive getArgDrive()
  {
    return getDrive( this.args[ 1 ] & 0x03 );
  }


  private int getArgHead()
  {
    return ((this.args[ 1 ] >> 2) & 0x01);
  }


  private FloppyDiskDrive getDrive( int driveNum )
  {
    return this.driveSelector.getFloppyDiskDrive( driveNum );
  }


  private FloppyDiskDrive getExecutingDrive()
  {
    FloppyDiskDrive drive = null;
    synchronized( this.ioLock ) {
      drive = this.executingDrive;
    }
    return drive;
  }


  private void incSectorNum()
  {
    addSectorNum( 1 );
  }


  private int readFromDisk()
  {
    int rv = -1;
    FloppyDiskDrive drive = this.executingDrive;
    if( drive != null ) {
      if( (this.curCmd == Command.READ_DATA)
	  || (this.curCmd == Command.READ_DELETED_DATA)
	  || (this.curCmd == Command.READ_TRACK) )
      {
	this.tStatesTillOverrun  = 0;
	SectorData sector        = this.curSector;
	SectorData.Reader reader = this.curSectorReader;
	if( (sector != null) && (reader != null) ) {
	  if( this.remainBytes > 0 ) {
	    rv = reader.read();
	    --this.remainBytes;
	  }
	  if( (rv < 0)
	      || ((this.statusReg0 & ST0_ERROR_MASK) != 0)
	      || (((this.statusReg2 & ST2_CONTROL_MARK) != 0)
		  && ((this.args[ 0 ] & ARG0_SK_MASK) == 0)) )
	  {
	    stopExecution();
	  } else {
	    this.statusRegMain &= ~STM_REQUEST_FOR_MASTER;
	    if( (this.remainBytes > 0)
		&& this.curSectorReader.isByteAvailable() )
	    {
	      startIOReqTimer();
	    } else {
	      /*
	       * Die Zeit bis zum ersten Byte des naechsten Sektors
	       * ist groesser als innerhalb eines Sektors.
	       * Das muss auch so emuliert werden,
	       * um nicht vor einem evtl. TC bereits mit dem Lesen
	       * des naechsten Sektors zu beginnen,
	       * was dann zum falschen Setzen des End Of Cylinder Bits
	       * fuehren koennte.
	       */
	      this.curSector       = null;
	      this.curSectorReader = null;
	      incSectorNum();
	      if( this.curCmd == Command.READ_TRACK ) {
		startIOTask(
			IOTaskCmd.READ_SECTOR_BY_INDEX,
			this.tStatesPerRotation / 5 );
	      } else {
		startIOTask(
			IOTaskCmd.READ_SECTOR_BY_ID,
			this.tStatesPerRotation / 5 );
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void seek( int driveNum, int head, int cyl )
  {
    this.statusRegMain &= ~STM_BUSY;
    this.statusReg0 = 0;
    if( (driveNum >= 0) && (driveNum < this.seekStatus.length) ) {
      this.seekStatus[ driveNum ] = ((head << 2) & HEAD_MASK) | driveNum;
      FloppyDiskDrive drive = getDrive( driveNum );
      if( drive != null ) {
	if( drive.getCylinder() == cyl ) {
	  this.seekStatus[ driveNum ] |= ST0_SEEK_END;
	  this.interruptReq = true;
	} else {
	  if( driveNum > 0 ) {
	    this.statusRegMain |= (1 << driveNum);
	  } else {
	    this.statusRegMain |= 0x01;
	  }
	  drive.setSeekMode( head, cyl );
	  this.remainSeekSteps[ driveNum ] = 77;
	  if( !this.seekMode ) {
	    this.tStateStepCounter = 0;
	    this.seekMode          = true;
	  }
	}
      } else {
	this.seekStatus[ driveNum ] |= ST0_ABNORMAL_TERMINATION;
	this.seekStatus[ driveNum ] |= ST0_NOT_READY;
	this.seekStatus[ driveNum ] |= ST0_SEEK_END;
	this.interruptReq = true;
      }
    } else {
      this.statusReg0 |= ST0_ABORT_BECAUSE_READY_CHANGED;
      this.statusReg0 |= ST0_SEEK_END;
      this.statusReg0 |= ST0_NOT_READY;
      this.statusReg0 |= ((head << 2) & HEAD_MASK);
      this.statusReg0 |= (driveNum & DRIVE_MASK);
      this.interruptReq = true;
    }
  }


  private void setByteReadable()
  {
    if( this.dmaMode ) {
      this.dmaReq = true;
    } else {
      this.statusReg0 &= ~HEAD_DRIVE_MASK;
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      this.interruptReq = true;
    }
    this.statusRegMain |= STM_DATA_INPUT;
    this.statusRegMain |= STM_REQUEST_FOR_MASTER;
    startOverrunTimer();
  }


  private void setByteWritable( boolean enableOverrun )
  {
    if( this.dmaMode ) {
      this.dmaReq = true;
    } else {
      this.statusReg0 &= ~HEAD_DRIVE_MASK;
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      this.interruptReq = true;
    }
    this.statusRegMain &= ~STM_DATA_INPUT;
    this.statusRegMain |= STM_REQUEST_FOR_MASTER;
    if( enableOverrun ) {
      startOverrunTimer();
    }
  }


  private void setDataBuf( int dataLen )
  {
    this.dataLen = dataLen;
    if( this.dataBuf != null ) {
      if( this.dataBuf.length < dataLen ) {
	this.dataBuf = null;
      }
    }
    if( this.dataBuf == null ) {
      this.dataBuf = new byte[ dataLen ];
    }
  }


  private void setExecutionMode()
  {
    this.statusRegMain &= ~STM_REQUEST_FOR_MASTER;
    this.statusRegMain &= ~STM_DATA_INPUT;
    this.statusRegMain |= STM_BUSY;
    if( !this.dmaMode ) {
      this.statusRegMain |= STM_NON_DMA_MODE;
    }
  }


  private void setIdle()
  {
    this.statusRegMain &= STM_DRIVE_MASK;
    this.statusRegMain |= STM_REQUEST_FOR_MASTER;
    this.argIdx         = 0;
    this.resultIdx      = -1;
    this.eotReached     = false;
    this.tcEnabled      = false;
    this.tcFired        = false;
    this.executingDrive = null;
    this.curCmd         = Command.INVALID;
  }


  private void setResultMode()
  {
    this.dmaReq             = false;
    this.tStatesTillIOReq   = 0;
    this.tStatesTillOverrun = 0;
    this.statusRegMain &= STM_DRIVE_MASK;
    this.statusRegMain |= STM_BUSY;
    this.statusRegMain |= STM_DATA_INPUT;
    this.statusRegMain |= STM_REQUEST_FOR_MASTER;
  }


  private void startFormatTrack()
  {
    setExecutionMode();
    clearRegs012();
    clearSectorID();
    boolean         done  = false;
    FloppyDiskDrive drive = getArgDrive();
    if( drive != null ) {
      if( drive.isReady() ) {
	if( drive.isReadOnly() ) {
	  this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	  this.statusReg1 |= ST1_NOT_WRITABLE;
	} else {
	  setDataBuf( this.args[ 3 ] * 4 );
	  this.dataPos        = -1;
	  this.formatStatus   = FormatStatus.WAIT_FOR_HOLE;
	  this.executingDrive = drive;
	  done                = true;
	}
      }
    }
    if( !done ) {
      if( (this.statusReg0 & ST0_ERROR_MASK) == 0 ) {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	this.statusReg0 |= ST0_EQUIPMENT_CHECK;
	this.statusReg0 |= ST0_NOT_READY;
      }
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      stopExecution();
    }
  }


  private void startIOReqTimer()
  {
    this.tStatesTillIOReq = Math.min( this.tStatesPerMilli / 100, 1 );
  }


  private void startIOTask( IOTaskCmd ioTaskCmd, int delayTStates )
  {
    synchronized( this.ioTaskThread ) {
      this.ioTaskCmd          = ioTaskCmd;
      this.tStatesTillIOStart = delayTStates;
      if( delayTStates == 0 ) {
	try {
	  this.ioTaskThread.notify();
	}
	catch( IllegalMonitorStateException ex ) {
	  this.ioTaskNoWait = true;
	}
      }
    }
  }


  private void startOverrunTimer()
  {
    /*
     * Die Overrun-Zeit wird der Einfachheit halber
     * hier sehr grosszuegig auf eine Diskettenumdrehung gesetzt,
     * damit auf keinen Fall in der Emulation ein Abbruch geschieht,
     * der auf realer Hardware noch nicht passiert waere.
     * Das gilt insbesondere auch beim Formatieren von Disketten.
     */
    this.tStatesTillOverrun = this.tStatesPerRotation;
  }


  private void startReadDataOrScan()
  {
    setExecutionMode();
    clearRegs012();
    this.sectorIdCyl      = this.args[ 2 ];
    this.sectorIdHead     = this.args[ 3 ];
    this.sectorIdRec      = this.args[ 4 ];
    this.sectorIdSizeCode = this.args[ 5 ];
    this.curSectorReader  = null;
    this.tcEnabled        = true;
    boolean         done  = false;
    FloppyDiskDrive drive = getArgDrive();
    if( drive != null ) {
      if( drive.isReady() ) {
	if( (this.curCmd == Command.SCAN_EQUAL)
	    || (this.curCmd == Command.SCAN_LOW_OR_EQUAL)
	    || (this.curCmd == Command.SCAN_HIGH_OR_EQUAL) )
	{
	  this.dataLen = 128;
	  this.args[ 5 ] &= 0x0F;
	  if( this.args[ 5 ] > 0 ) {
	    this.dataLen = (128 << this.args[ 5 ]);
	  }
	  this.statusReg2 |= ST2_SCAN_EQUAL_HIT;
	} else {
	  this.dataLen = getArgDataLen();
	}
	this.executingDrive = drive;
	startIOTask( IOTaskCmd.READ_SECTOR_BY_ID, 0 );
	done = true;
      }
    }
    if( !done ) {
      this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
      this.statusReg0 |= ST0_EQUIPMENT_CHECK;
      this.statusReg0 |= ST0_NOT_READY;
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      stopExecution();
    }
  }


  private void startReadID()
  {
    setExecutionMode();
    clearRegs012();
    clearSectorID();
    boolean         done  = false;
    FloppyDiskDrive drive = getArgDrive();
    if( drive != null ) {
      if( drive.isReady() ) {
	int idx = getSectorIndexByCurHeadPos( drive );
	if( idx >= 0 ) {
	  this.executingDrive = drive;
	  this.sectorIdRec    = idx + 1;
	  startIOTask( IOTaskCmd.READ_SECTOR_BY_INDEX, 0 );
	  done = true;
	}
      }
    }
    if( !done ) {
      this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
      this.statusReg0 |= ST0_EQUIPMENT_CHECK;
      this.statusReg0 |= ST0_NOT_READY;
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      stopExecution();
    }
  }


  private void startReadTrack()
  {
    setExecutionMode();
    clearRegs012();
    this.sectorIdCyl      = this.args[ 2 ];
    this.sectorIdHead     = this.args[ 3 ];
    this.sectorIdRec      = 1;
    this.sectorIdSizeCode = this.args[ 5 ];
    this.curSectorReader  = null;
    this.curSectorIdx     = 0;
    this.tcEnabled        = true;
    boolean         done  = false;
    FloppyDiskDrive drive = getArgDrive();
    if( drive != null ) {
      if( drive.isReady() ) {
	this.dataLen        = getArgDataLen();
	this.executingDrive = drive;
	startIOTask(
		IOTaskCmd.READ_SECTOR_BY_INDEX,
		Math.max(
			this.tStatesPerRotation - this.tStateRotationCounter,
			0 ) );
	done = true;
      }
    }
    if( !done ) {
      this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
      this.statusReg0 |= ST0_EQUIPMENT_CHECK;
      this.statusReg0 |= ST0_NOT_READY;
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      stopExecution();
    }
  }


  private void startWriteData()
  {
    setExecutionMode();
    clearRegs012();
    this.sectorIdCyl      = this.args[ 2 ];
    this.sectorIdHead     = this.args[ 3 ];
    this.sectorIdRec      = this.args[ 4 ];
    this.sectorIdSizeCode = this.args[ 5 ];
    this.curSector        = null;
    this.tcEnabled        = true;
    boolean         done  = false;
    FloppyDiskDrive drive = getArgDrive();
    if( drive != null ) {
      if( drive.isReady() ) {
	if( drive.isReadOnly() ) {
	  this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	  this.statusReg1 |= ST1_NOT_WRITABLE;
	} else {
	  if( !this.dmaMode ) {
	    this.statusRegMain |= STM_NON_DMA_MODE;
	  }
	  setDataBuf( getArgDataLen() );
	  this.dataPos        = -1;
	  this.executingDrive = drive;
	  startIOTask( IOTaskCmd.READ_SECTOR_FOR_WRITE, 0 );
	  done = true;
	}
      }
    }
    if( !done ) {
      if( (this.statusReg0 & ST0_ERROR_MASK) == 0 ) {
	this.statusReg0 |= ST0_ABNORMAL_TERMINATION;
	this.statusReg0 |= ST0_EQUIPMENT_CHECK;
	this.statusReg0 |= ST0_NOT_READY;
      }
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      stopExecution();
    }
  }


  private void stopExecution()
  {
    synchronized( this.ioLock ) {
      if( !this.eotReached ) {
	this.tcEnabled = false;
      }
      this.ioTaskCmd      = IOTaskCmd.IDLE;
      this.ioTaskNoWait   = false;
      this.tcFired        = false;
      this.executingDrive = null;
      this.formatStatus   = FormatStatus.IDLE;
      this.results[ 0 ]   = this.sectorIdSizeCode;
      this.results[ 1 ]   = this.sectorIdRec;
      this.results[ 2 ]   = this.sectorIdHead;
      this.results[ 3 ]   = this.sectorIdCyl;
      this.results[ 4 ]   = this.statusReg2;
      this.results[ 5 ]   = this.statusReg1;
      this.results[ 6 ]   = this.statusReg0;
      this.resultIdx      = 6;
      this.statusReg0 &= ~HEAD_DRIVE_MASK;
      this.statusReg0 |= (this.args[ 1 ] & HEAD_DRIVE_MASK);
      this.interruptReq = true;
      setResultMode();
    }
  }


  private void writeCmd( int value )
  {
    this.statusRegMain |= STM_BUSY;
    if( this.argIdx == 0 ) {
      this.executingDrive        = null;
      this.resultIdx             = -1;
      this.args[ this.argIdx++ ] = value;
      value &= 0x1F;
      if( value < cmds.length ) {
	this.curCmd = cmds[ value ];
      } else {
	this.curCmd = Command.INVALID;
      }
      if( this.curCmd == Command.INVALID ) {
	if( this.debugLevel > 0 ) {
	  System.out.println( "FDC: INVALID" );
	}
	this.curCmd       = Command.INVALID;
	this.interruptReq = false;
	this.statusReg0   = ST0_INVALID_COMMAND_ISSUE;
	this.results[ 0 ] = this.statusReg0;
	this.resultIdx    = 0;
	setResultMode();
      } else {
	if( this.debugLevel > 0 ) {
	  System.out.println( "FDC: " + this.curCmd );
	}
	if( this.curCmd == Command.SENSE_INTERRUPT_STATUS ) {
	  execSenseInterruptStatus();
	}
      }
    } else {
      if( (this.executingDrive == null) && (this.resultIdx < 0) ) {
	if( this.argIdx < this.args.length ) {
	  this.args[ this.argIdx++ ] = value;
	}
	switch( this.curCmd ) {
	  case FORMAT_TRACK:
	    if( this.argIdx == 6 ) {
	      startFormatTrack();
	    }
	    break;

	  case READ_DATA:
	  case READ_DELETED_DATA:
	  case SCAN_EQUAL:
	  case SCAN_LOW_OR_EQUAL:
	  case SCAN_HIGH_OR_EQUAL:
	    if( this.argIdx == 9 ) {
	      startReadDataOrScan();
	    }
	    break;

	  case READ_ID:
	    if( this.argIdx == 2 ) {
	      startReadID();
	    }
	    break;

	  case READ_TRACK:
	    if( this.argIdx == 9 ) {
	      startReadTrack();
	    }
	    break;

	  case RECALIBRATE:
	    if( this.argIdx == 2 ) {
	      seek( this.args[ 1 ] & 0x03, 0, 0 );
	      setIdle();
	    }
	    break;

	  case SEEK:
	    if( this.argIdx == 3 ) {
	      seek(
		this.args[ 1 ] & 0x03,
		(args[ 1 ] >> 2) & 0x01,
		this.args[ 2 ] );
	      setIdle();
	    }
	    break;

	  case SENSE_DRIVE_STATUS:
	    if( this.argIdx == 2 ) {
	      execSenseDriveStatus();
	    }
	    break;

	  case SPECIFY:
	    if( this.argIdx == 3 ) {
	      this.stepRateMillis = 16 - ((this.args[ 1 ] >> 4) & 0x0F);
	      this.dmaMode        = ((this.args[ 2 ] & 0x01) == 0);
	      calcTStatesPerStep();
	      setIdle();
	    }
	    break;

	  case WRITE_DATA:
	  case WRITE_DELETED_DATA:
	    if( this.argIdx == 9 ) {
	      startWriteData();
	    }
	    break;

	  default:
	    setIdle();
	}
      }
    }
  }


  private void writeToDrive( int value )
  {
    value              &= 0xFF;
    this.statusRegMain &= ~STM_REQUEST_FOR_MASTER;
    FloppyDiskDrive drive = this.executingDrive;
    if( drive != null ) {
      if( (this.curCmd == Command.SCAN_EQUAL)
	  || (this.curCmd == Command.SCAN_LOW_OR_EQUAL)
	  || (this.curCmd == Command.SCAN_HIGH_OR_EQUAL) )
      {
	this.tStatesTillOverrun  = 0;
	int        b             = -1;
	SectorData sector        = this.curSector;
	SectorData.Reader reader = this.curSectorReader;
	if( (sector != null) && (reader != null) ) {
	  if( this.remainBytes > 0 ) {
	    b = reader.read();
	    --this.remainBytes;
	  }
	  if( (b < 0) || ((this.statusReg0 & ST0_ERROR_MASK) != 0) ) {
	    stopExecution();
	  } else {
	    if( b != value ) {
	      this.statusReg2 &= ~ST2_SCAN_EQUAL_HIT;
	    }
	    if( this.curCmd == Command.SCAN_LOW_OR_EQUAL ) {
	      if( b > value ) {
		this.statusReg2 |= ST2_SCAN_NOT_SATISFIED;
	      }
	    } else if( this.curCmd == Command.SCAN_HIGH_OR_EQUAL ) {
	      if( b < value ) {
		this.statusReg2 |= ST2_SCAN_NOT_SATISFIED;
	      }
	    } else {
	      if( b != value ) {
		this.statusReg2 |= ST2_SCAN_NOT_SATISFIED;
	      }
	    }
	    if( (this.remainBytes > 0)
		&& this.curSectorReader.isByteAvailable() )
	    {
	      startIOReqTimer();
	    } else {
	      if( (this.statusReg0 & ST0_ERROR_MASK) != 0 ) {
		stopExecution();
	      } else {
		/*
		 * Die Zeit bis zum ersten Byte des naechsten Sektors
		 * ist groesser als innerhalb eines Sektors.
		 * Das muss auch so emuliert werden,
		 * um nicht vor einem evtl. TC bereits mit dem Lesen
		 * des naechsten Sektors zu beginnen,
		 * was dann zum falschen Setzen des End Of Cylinder Bits
		 * fuehren wuerde.
		 */
		this.curSector       = null;
		this.curSectorReader = null;
		addSectorNum( this.args[ 8 ] );
		startIOTask(
			IOTaskCmd.READ_SECTOR_BY_ID,
			this.tStatesPerRotation / 5 );
	      }
	    }
	  }
	}
      } else {
	if( this.dataBuf != null ) {
	  if( (this.dataPos >= 0)
	      && (this.dataPos < this.dataBuf.length)
	      && (this.dataPos < this.dataLen) )
	  {
	    if( this.curCmd == Command.FORMAT_TRACK ) {
	      this.dataBuf[ this.dataPos++ ] = (byte) value;
	      startIOReqTimer();
	    }
	    else if( (this.curCmd == Command.WRITE_DATA)
		     || (this.curCmd == Command.WRITE_DELETED_DATA) )
	    {
	      this.tStatesTillOverrun        = 0;
	      this.dataBuf[ this.dataPos++ ] = (byte) value;
	      if( (this.dataPos < this.dataBuf.length)
		  && (this.dataPos < this.dataLen) )
	      {
		startIOReqTimer();
	      } else {
		startIOTask( IOTaskCmd.WRITE_SECTOR, 0 );
	      }
	    }
	  }
	}
      }
    }
  }
}
