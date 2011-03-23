/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Floppy-Disk-Controllers U8272
 * (kompatibel zu Intel 8272A)
 */

package jkcemu.disk;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;
import z80emu.*;


public class FDC8272 implements Z80TStatesListener, Z80MaxSpeedListener
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
		null,					// 0 0 0 0 0
		null,					// 0 0 0 0 1
		Command.READ_TRACK,			// 0 0 0 1 0
		Command.SPECIFY,			// 0 0 0 1 1
		Command.SENSE_DRIVE_STATUS,		// 0 0 1 0 0
		Command.WRITE_DATA,			// 0 0 1 0 1
		Command.READ_DATA,			// 0 0 1 1 0
		Command.RECALIBRATE,			// 0 0 1 1 1
		Command.SENSE_INTERRUPT_STATUS,		// 0 1 0 0 0
		Command.WRITE_DELETED_DATA,		// 0 1 0 0 1
		Command.READ_ID,			// 0 1 0 1 0
		null,					// 0 1 0 1 1
		Command.READ_DELETED_DATA,		// 0 1 1 0 0
		Command.FORMAT_TRACK,			// 0 1 1 0 1
		null,					// 0 1 1 1 0
		Command.SEEK,				// 0 1 1 1 1
		null,					// 1 0 0 0 0
		Command.SCAN_EQUAL,			// 1 0 0 0 1
		null,					// 1 0 0 1 0
		null,					// 1 0 0 1 1
		null,					// 1 0 1 0 0
		null,					// 1 0 1 0 1
		null,					// 1 0 1 1 0
		null,					// 1 0 1 1 1
		null,					// 1 1 0 0 0
		Command.SCAN_LOW_OR_EQUAL,		// 1 1 0 0 1
		null,					// 1 1 0 1 0
		null,					// 1 1 0 1 1
		null,					// 1 1 1 0 0
		Command.SCAN_HIGH_OR_EQUAL,		// 1 1 1 0 1
		null,					// 1 1 1 1 0
		null };					// 1 1 1 1 1

  private DriveSelector     driveSelector;
  private FloppyDiskDrive   executingDrive;
  private Command           curCmd;
  private volatile boolean  tcFired;
  private volatile boolean  interruptReq;
  private volatile boolean  dmaReq;
  private boolean           dmaMode;
  private boolean           seekMode;
  private boolean           scanMode;
  private boolean           cancelable;
  private boolean           debugEnabled;
  private volatile boolean  waitForIndexHole;
  private int[]             args;
  private int               argIdx;
  private int[]             results;
  private int               resultIdx;
  private int               sectorIdCyl;
  private int               sectorIdHead;
  private int               sectorIdRec;
  private int               sectorIdSizeCode;
  private int               mhz;
  private int               statusRegMain;
  private int               statusReg0;
  private int               statusReg1;
  private int               statusReg2;
  private int               statusReg3;
  private int               stepRateMillis;
  private int               headLoadMillis;
  private int               tStatesTillIOReq;
  private int               tStatesTillOverrun;
  private int               tStateRotationCounter;
  private int               tStateStepCounter;
  private volatile int      tStatesPerMilli;
  private volatile int      tStatesPerRotation;
  private volatile int      tStatesPerStep;
  private int[]             seekStatus;
  private int[]             remainSeekSteps;
  private byte[]            dataBuf;
  private int               dataPos;
  private int               dataLen;
  private int               remainBytes;
  private int               curSectorIdx;
  private SectorData        curSector;
  private SectorData.Reader curSectorReader;


  public FDC8272( DriveSelector driveSelector, int mhz )
  {
    this.driveSelector      = driveSelector;
    this.mhz                = mhz;
    this.debugEnabled       = false;
    this.dmaMode            = false;	// wird von RESET nicht beeinflusst
    this.stepRateMillis     = 0;	// wird von RESET nicht beeinflusst
    this.headLoadMillis     = 0;	// wird von RESET nicht beeinflusst
    this.tStatesPerMilli    = 0;
    this.tStatesPerRotation = 0;
    this.tStatesPerStep     = 0;
    this.dataBuf            = null;
    this.args               = new int[ 9 ];
    this.results            = new int[ 7 ];
    this.remainSeekSteps    = new int[ 4 ];
    this.seekStatus         = new int[ 4 ];

    String text = System.getProperty( "jkcemu.debug.fdc" );
    if( text != null ) {
      if( Boolean.parseBoolean( text ) ) {
	this.debugEnabled = true;
      }
    }
    reset( true );
  }


  public void dmaAcknowledge()
  {
    this.dmaReq = false;
  }


  public void fireTC()
  {
    if( this.debugEnabled ) {
      System.out.println( "FDC: TC" );
    }
    if( this.cancelable ) {
      setIdle();
    } else {
      this.tcFired = true;
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


  public void processTillNextIORequest()
  {
    if( this.tStatesTillIOReq >= 0 ) {
      processTStates( this.tStatesTillIOReq + 1 );
    } else {
      if( this.waitForIndexHole ) {
	int tStates = this.tStatesPerRotation - this.tStateRotationCounter;
	processTStates( tStates > 0 ? tStates : 1 );
	if( this.tStatesTillIOReq >= 0 ) {
	  processTStates( this.tStatesTillIOReq + 1 );
	}
      }
    }
  }


  public int readMainStatusReg()
  {
    if( this.debugEnabled ) {
      System.out.printf( "FDC: read status: %02X\n", this.statusRegMain );
    }
    this.interruptReq = false;
    return this.statusRegMain;
  }


  public int readData()
  {
    int rv = readFromDisk();
    if( rv == -1 ) {
      rv = this.statusRegMain;
      if( (this.resultIdx >= 0) && (this.resultIdx < this.results.length) ) {
	rv = this.results[ this.resultIdx ];
	--this.resultIdx;
	if( this.resultIdx >= 0 ) {
	  this.tcFired    = false;
	  this.cancelable = true;
	} else {
	  setIdle();
	}
      }
    }
    this.interruptReq = false;
    if( this.debugEnabled ) {
      System.out.printf( "FDC: read data: %02X\n", rv );
    }
    return rv;
  }


  public void reset( boolean powerOn )
  {
    if( powerOn ) {
      this.dmaMode        = false;
      this.stepRateMillis = 0;
      this.headLoadMillis = 0;
    }
    this.executingDrive        = null;
    this.seekMode              = false;
    this.scanMode              = false;
    this.tcFired               = false;
    this.dmaReq                = false;
    this.interruptReq          = false;
    this.waitForIndexHole      = false;
    this.statusRegMain         = 0x80;
    this.statusReg3            = 0;
    this.curSector             = null;
    this.curSectorReader       = null;
    this.curSectorIdx          = -1;
    this.dataPos               = 0;
    this.dataLen               = 0;
    this.remainBytes           = 0;
    this.tStatesTillIOReq      = -1;
    this.tStatesTillOverrun    = -1;
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
    if( this.debugEnabled ) {
      System.out.printf( "FDC: write: %02X\n", value );
    }
    if( this.executingDrive != null ) {
      writeData( value );
    } else {
      writeCmd( value );
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    setTStatesPerMilli( cpu.getMaxSpeedKHz() );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    processTStates( tStates );
  }


	/* --- private Methoden --- */

  private void calcTStatesPerStep()
  {
    this.tStatesPerStep = this.stepRateMillis * tStatesPerMilli * 8 / this.mhz;
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


  private void execReadDataOrScanCycle()
  {
    FloppyDiskDrive drive = this.executingDrive;
    if( drive != null ) {
      boolean abort = false;
      if( this.tcFired ) {
	abort                = true;
	this.curSectorReader = null;
      } else {
	boolean firstSector = false;
	boolean changeHead  = false;
	if( this.curSectorReader != null ) {
	  if( (this.remainBytes <= 0)
	      || !this.curSectorReader.isByteAvailable() )
	  {
	    if( this.sectorIdCyl > this.args[ 2 ] ) {
	      /*
	       * Wenn der gerade abgeschlossene Sektor EOT entsprach,
	       * dann wurde die Zylindernummer in der Sektor-ID hochgezaehlt.
	       * In dem Fall keinen neuen Sektor mehr lesen
	       */
	      abort = true;
	    } else {
	      if( this.scanMode && ((this.statusReg2 & 0x04) == 0) ) {
		/*
		 * Wenn bei einem Scan-Kommando nach dem Vergleichen
		 * eines Sektors das Scan Not Satified Bit nicht gesetzt ist,
		 * ist die Scan-Bedingung erfuellt -> Kommando beenden
		 */
		abort = true;
	      } else {
		if( this.sectorIdRec == this.args[ 6 ] ) {
		  // EOT erreicht
		  if( (this.args[ 0 ] & 0x80) != 0 ) {
		    /*
		     * Multi Track: Nach dem Lesen des letzten Sektors
		     * der Spur muss der Kopf gewechselt werden.
		     * Des weiteren wird das MT-Bit im Kommando-Byte
		     * zurueckgesetzt, um einen nochmaligen Kopfwechsel
		     * zu verhindern.
		     */
		    changeHead = true;
		    this.args[ 0 ] &= 0x7F;
		  } else {
		    /*
		     * kein Multi Track
		     * -> Zylinder hochzaehlen und Lesen beenden
		     */
		    this.sectorIdCyl++;
		    abort = true;
		  }
		  this.sectorIdRec = 1;
		} else {
		  if( this.scanMode ) {
		    if( this.args[ 8 ] > 0 ) {
		      this.sectorIdRec += this.args[ 8 ];
		    } else {
		      abort = true;	// Schrittweite 0
		    }
		  } else {
		    this.sectorIdRec++;
		  }
		}
	      }
	    }
	    this.curSectorReader = null;
	  }
	} else {
	  firstSector = true;
	}
	if( !abort && (this.curSectorReader == null) ) {
	  SectorData sector = null;
	  boolean    sk     = ((this.args[ 0 ] & 0x20) != 0);
	  if( sk || ((this.statusReg2 & 0x40) == 0) ) {		// CM=0 
	    int startIdx = 0;
	    for(;;) {
	      sector = drive.readSectorByID( 
				(this.args[ 1 ] >> 2) & 0x01,
				startIdx,
				this.sectorIdCyl,
				this.sectorIdHead,
				this.sectorIdRec,
				this.sectorIdSizeCode );
	      if( sector != null ) {
		boolean cm = sector.isDeleted();
		if( this.curCmd == Command.READ_DELETED_DATA ) {
		  cm = !cm;
		}
		if( cm ) {
		  this.statusReg2 |= 0x40;	// Control Mark
		  if( sk ) {
		    startIdx = sector.getIndexOnCylinder() + 1;
		    sector   = null;
		    continue;
		  }
		}
		if( sector.checkError() ) {
		  this.statusReg1 |= 0x20;	// Data Error
		  this.statusReg2 |= 0x20;	// Data Error
		} else if( sector.isEmpty() ) {
		  this.statusReg1 |= 0x01;	// Missing Address Mark
		  this.statusReg2 |= 0x01;	// Missing Data Address Mark
		}
	      }
	      break;
	    }
	  }
	  if( sector != null ) {
	    this.curSectorReader = sector.reader();
	    this.remainBytes     = this.dataLen;
	    if( this.scanMode && this.curSectorReader.isByteAvailable() ) {
	      this.statusReg2 &= ~0x04;  // Scan Not Satified zuruecksetzen
	      this.statusReg2 |= ~0x08;  // Scan Equal Hit setzen
	    }
	  } else {
	    if( firstSector ) {
	      this.statusReg0 |= 0x40;	// Abnormal Termination
	      this.statusReg1 |= 0x05;	// No Data, Missing Address Mark
	    } else {
	      this.statusReg1 |= 0x80;	// End Of Cylinder
	      if( this.scanMode && (this.args[ 8 ] > 1) ) {
		this.statusReg0 |= 0x40;	// Abnormal Termination
	      }
	    }
	  }
	  if( changeHead ) {
	    /*
	     * weitere Sektoren vom anderen Kopf lesen
	     * (Negieren von Bit 0 in den Kopffeldern)
	     */
	    if( (this.args[ 1 ] & 0x40) != 0 ) {
	      this.args[ 1 ] &= 0xFB;
	    } else {
	      this.args[ 1 ] |= 0x40;
	    }
	    if( (this.sectorIdHead & 0x01) != 0 ) {
	      this.sectorIdHead &= ~0x01;
	    } else {
	      this.sectorIdHead |= 0x01;
	    }
	  }
	}
      }
      if( !abort && this.curSectorReader != null ) {
	if( (this.remainBytes > 0)
	    && this.curSectorReader.isByteAvailable() )
	{
	  if( (this.curCmd == Command.SCAN_EQUAL)
	      || (this.curCmd == Command.SCAN_LOW_OR_EQUAL)
	      || (this.curCmd == Command.SCAN_LOW_OR_EQUAL) )
	  {
	    setByteWritable();
	  } else {
	    setByteReadable();
	  }
	} else {
	  this.tStatesTillIOReq = this.tStatesPerMilli * 7 / 1000;
	}
      } else {
	stopExecution();
      }
    }
  }


  private void execReadID()
  {
    FloppyDiskDrive drive  = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      SectorData         sector = null;
      AbstractFloppyDisk disk   = drive.getDisk();
      if( disk != null ) {
	int head = (this.args[ 0 ] >> 2) & 0x01;
	int cyl  = drive.getCylinder();
	int spc  = disk.getSectorsOfCylinder( cyl, head );
	int tpr  = this.tStatesPerRotation;
	if( (tpr > 0)
	    && (head < disk.getSides())
	    && (cyl < disk.getCylinders()) )
	{
	  int idx = Math.round( (float) this.tStateRotationCounter
							/ (float) tpr
							* (float) spc );
	  sector = drive.readSectorByIndex( head, idx );
	  if( (sector == null) && (idx != 0) ) {
	    sector = drive.readSectorByIndex( head, 0 );
	  }
	}
      }
      if( sector != null ) {
	this.sectorIdCyl      = sector.getCylinder();
	this.sectorIdHead     = sector.getHead();
	this.sectorIdRec      = sector.getSectorNum();
	this.sectorIdSizeCode = sector.getSizeCode();
	if( sector.isEmpty() ) {
	  this.statusReg1 |= 0x01;	// Missing Address Mark
	  this.statusReg1 |= 0x01;	// Missing Data Address Mark
	}
      } else {
	this.statusReg1 |= 0x05;	// No Data, Missing Address Mark
      }
      stopExecution();
    }
  }


  private void execReadTrackCycle( boolean firstCycle )
  {
    FloppyDiskDrive drive = this.executingDrive;
    if( drive != null ) {
      if( this.tcFired ) {
	this.curSectorReader = null;
      } else {
	boolean changeSector = false;
	if( firstCycle ) {
	  // No Data und Missing Address Mark erstmal setzen
	  this.statusReg1 |= 0x05;
	  this.curSectorIdx = 0;
	  changeSector      = true;
	} else {
	  if( this.curSectorReader != null ) {
	    if( (this.remainBytes <= 0)
		|| !this.curSectorReader.isByteAvailable() )
	    {
	      if( this.sectorIdRec == this.args[ 6 ] ) {
		this.curSectorReader = null;	// EOT erreicht
	      } else {
		this.sectorIdRec++;
		changeSector = true;
	      }
	    }
	  }
	}
	if( changeSector ) {
	  SectorData sector = null;
	  for(;;) {
	    sector = drive.readSectorByIndex(
					(this.args[ 1 ] >> 2) & 0x01,
					this.curSectorIdx++ );
	    if( sector != null ) {
	      /*
	       * Missing Address Mark zuruecksetzen, aber nur,
	       * wenn Missing Data Address Mark nicht gesetzt ist
	       */
	      if( (this.statusReg1 & 0x01) == 0 ) {
		this.statusReg2 &= ~0x01;
	      }
	      /*
	       * Die Sektornummer wird nicht verglichen,
	       * da in physischer und nicht in logischer
	       * Reihenfolge gelesen wird.
	       */
	      if( (sector.getCylinder() != this.sectorIdCyl)
		  || (sector.getHead() != this.sectorIdHead)
		  || (sector.getSizeCode() != this.sectorIdSizeCode) )
	      {
		continue;
	      }
	      this.statusReg1 &= ~0x04;		// No Data zuruecksetzen
	      if( sector.isDeleted() ) {
	        this.statusReg2 |= 0x40;	// Control Mark
	      }
	      if( sector.checkError() ) {
	        this.statusReg1 |= 0x20;	// Data Error
	        this.statusReg2 |= 0x20;	// Data Error
	      }
	      if( sector.isEmpty() ) {
		this.statusReg1 |= 0x01;	// Missing Address Mark
		this.statusReg2 |= 0x01;	// Missing Data Address Mark
		continue;
	      }
	      if( sector != null ) {
		this.curSectorReader = sector.reader();
		this.remainBytes     = this.dataLen;
	      }
	    } else {
	      if( this.sectorIdRec > 1 ) {
	        this.statusReg1 |= 0x80;	// End Of Cylinder
	      }
	    }
	    break;
	  }
	}
      }
      if( this.curSectorReader != null ) {
	if( (this.remainBytes > 0)
	    && this.curSectorReader.isByteAvailable() )
	{
	  setByteReadable();
	} else {
	  this.tStatesTillIOReq = this.tStatesPerMilli * 7 / 1000;
	}
      } else {
	stopExecution();
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
	    this.seekStatus[ i ] |= 0x20;	// Seek End
	    this.interruptReq = true;
	  } else {
	    if( this.remainSeekSteps[ i ] > 0 ) {
	      driveSeekMode = true;
	    } else {
	      // Abnormal termination, Seek End, Equiment Check
	      this.seekStatus[ i ] |= 0x70;
	      this.interruptReq = true;
	    }
	  }
	} else {
	  // Abnormal termination, Seek End, Not Ready
	  this.seekStatus[ i ] |= 0xE8;
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
    this.statusReg3       = (this.args[ 1 ] & 0x07);;
    FloppyDiskDrive drive = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      this.statusReg3 |= 0x08;		// doppelseitiges Laufwerk
      if( drive.getCylinder() == 0 ) {
	this.statusReg3 |= 0x10;
      }
      if( drive.isReady() ) {
	this.statusReg3 |= 0x20;
      }
      if( drive.isReadOnly() ) {
	this.statusReg3 |= 0x40;
      }
    }
    this.cancelable   = true;
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
    this.resultIdx  = 1;
    this.cancelable = true;
    setResultMode();
  }


  private void execWriteDataCycle()
  {
    FloppyDiskDrive drive = this.executingDrive;
    if( drive != null ) {
      boolean abort = this.tcFired;
      if( this.dataBuf != null ) {
	if( this.curSector != null ) {
	  if( this.tcFired && (this.dataPos > 0) ) {
	    while( this.dataPos < this.dataLen ) {
	      this.dataBuf[ this.dataPos++ ] = (byte) 0;
	    }
	  }
	  if( this.dataPos >= this.dataLen ) {
	    if( drive.writeSector(
			(this.args[ 1 ] >> 2) & 0x01,
			this.curSector,
			this.dataBuf,
			this.dataLen,
			this.curCmd == Command.WRITE_DELETED_DATA ) )
	    {
	      if( this.sectorIdRec == this.args[ 6 ] ) {
		// EOT erreicht
		if( (this.args[ 0 ] & 0x80) != 0 ) {
		  // Multi Track -> Bit 0 in Head negieren
		  if( (this.sectorIdHead & 0x01) != 0 ) {
		    this.sectorIdHead &= 0xFE;
		  } else {
		    this.sectorIdHead |= 0x01;
		  }
		  if( (this.args[ 1 ] & 0x04) != 0 ) {
		    this.args[ 1 ] &= ~0x04;
		  } else {
		    this.args[ 1 ] |= 0x04;
		  }
		  this.args[ 0 ] &= 0x7F;	// kein weiterer Track-Wechsel
		} else {
		  /*
		   * kein Multi Track
		   * -> Zylinder hochzaehlen und Schreiben beenden
		   */
		  this.sectorIdCyl++;
		  abort = true;
		}
		this.sectorIdRec = 1;
	      } else {
		this.sectorIdRec++;
	      }
	    } else {
	      this.statusReg0 |= 0x40;		// Abnormal Termination
	      if( this.executingDrive.isReadOnly() ) {
		this.statusReg1 |= 0x02;	// Not Writeable
	      } else {
		this.statusReg1 |= 0x20;	// Data Error
		this.statusReg2 |= 0x20;	// Data Error
	      }
	      this.curSector = null;
	      abort          = true;
	    }
	    this.dataPos = -1;
	  }
	}
	if( !abort
	    && ((this.curSector == null)
		|| (this.dataPos < 0)
		|| (this.dataPos >= this.dataBuf.length)) )
	{
	  SectorData sector = drive.readSectorByID(
					(this.args[ 1 ] >> 2) & 0x01,
					0,
					this.sectorIdCyl,
					this.sectorIdHead,
					this.sectorIdRec,
					this.sectorIdSizeCode );
	  if( sector != null ) {
	    this.dataPos   = 0;
	    this.curSector = sector;
	    if( sector.isEmpty() ) {
	      this.statusReg1 |= 0x01;	// Missing Address Mark
	      this.statusReg1 |= 0x01;	// Missing Data Address Mark
	    }
	  } else {
	    if( (this.sectorIdCyl == this.args[ 2 ])
		&& (this.sectorIdRec == this.args[ 4 ]) )
	    {
	      this.statusReg0 |= 0x40;	// Abnormal Termination
	      this.statusReg1 |= 0x05;	// No Data, Missing Address Mark
	    } else {
	      this.statusReg1 |= 0x80;	// End Of Cylinder
	    }
	    abort = true;
	  }
	}
      }
      if( !abort
	  && (this.curSector != null)
	  && (this.dataBuf != null)
	  && (this.dataPos >= 0) )
      {
	setByteWritable();
      } else {
	stopExecution();
      }
    }
  }


  private void finishFormatTrack()
  {
    FloppyDiskDrive drive = this.executingDrive;
    if( drive != null ) {
      if( this.dataBuf != null ) {
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
	  int        srcIdx  = 0;
	  int        dstIdx  = 0;
	  while( ((srcIdx + 3) < this.dataBuf.length)
		 && (dstIdx < sectors.length) )
	  {
	    int cyl             = this.dataBuf[ srcIdx++ ];
	    int head            = this.dataBuf[ srcIdx++ ];
	    int rec             = this.dataBuf[ srcIdx++ ];
	    int sizeCode        = this.dataBuf[ srcIdx++ ];
	    sectors[ dstIdx++ ] = new SectorID( cyl, head, rec, sizeCode );
	  }
	  if( !drive.formatTrack(
			  (this.args[ 1 ] >> 2) & 0x01,
			  sectors,
			  contentBuf ) )
	  {
	    this.statusReg0 |= 0x40;		// Abnormal Termination
	    if( this.executingDrive.isReadOnly() ) {
	      this.statusReg1 |= 0x02;		// Not Writeable
	    } else {
	      this.statusReg1 |= 0x20;		// Data Error
	      this.statusReg2 |= 0x20;		// Data Error
	    }
	  }
	}
      }
      stopExecution();
    }
  }


  private int getDataLen()
  {
    int sizeCode = (this.args[ 5 ] & 0x0F);
    int dataLen  = this.args[ 8 ];
    if( sizeCode > 0 ) {
      dataLen = (128 << sizeCode);
    }
    return dataLen;
  }


  private FloppyDiskDrive getDrive( int driveNum )
  {
    return this.driveSelector.getFloppyDiskDrive( driveNum );
  }


  private void processTStates( int tStates )
  {
    this.tStateRotationCounter += tStates;
    if( (this.tStatesPerRotation > 0)
	&& (this.tStateRotationCounter >= this.tStatesPerRotation) )
    {
      this.tStateRotationCounter = 0;
      if( this.waitForIndexHole ) {
	this.waitForIndexHole = false;
	if( this.curCmd == Command.FORMAT_TRACK ) {
	  // Formatieren beginnen
	  this.dataPos          = 0;
	  this.tStatesTillIOReq = this.tStatesPerRotation / (this.dataLen + 2);
	}
	else if( this.curCmd == Command.READ_TRACK ) {
	  execReadTrackCycle( true );
	}
      }
      else if( this.curCmd == Command.FORMAT_TRACK ) {
	if( this.executingDrive != null ) {
	  finishFormatTrack();
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
    boolean ioCycle = false;
    if( this.tStatesTillIOReq >= 0 ) {
      this.tStatesTillIOReq -= tStates;
      if( this.tStatesTillIOReq < 0 ) {
	ioCycle = true;
      }
    }
    if( this.tcFired || ioCycle ) {
      if( this.curCmd != null ) {
	switch( this.curCmd ) {
	  case FORMAT_TRACK:
	    if( !this.tcFired ) {
	      FloppyDiskDrive drive = this.executingDrive;
	      if( drive != null ) {
		setByteWritable();
	      }
	    }
	    break;

	  case READ_DATA:
	  case READ_DELETED_DATA:
	  case SCAN_EQUAL:
	  case SCAN_LOW_OR_EQUAL:
	  case SCAN_HIGH_OR_EQUAL:
	    execReadDataOrScanCycle();
	    break;

	  case READ_ID:
	    execReadID();
	    break;

	  case READ_TRACK:
	    execReadTrackCycle( false );
	    break;

	  case WRITE_DATA:
	  case WRITE_DELETED_DATA:
	    execWriteDataCycle();
	    break;
	}
      }
    }
    if( this.tStatesTillOverrun >= 0 ) {
      this.tStatesTillOverrun -= tStates;
      if( this.tStatesTillOverrun < 0 ) {
	if( !this.tcFired ) {
	  this.statusReg1 |= 0x10;
	}
	if( this.executingDrive != null ) {
	  stopExecution();
	}
      }
    }
  }


  private int readFromDisk()
  {
    int rv = -1;
    if( !this.tcFired ) {
      FloppyDiskDrive drive = this.executingDrive;
      if( drive != null ) {
	if( (this.curCmd == Command.READ_DATA)
	    || (this.curCmd == Command.READ_DELETED_DATA)
	    || (this.curCmd == Command.READ_TRACK) )
	{
	  this.tStatesTillOverrun = -1;
	  if( this.curSectorReader != null ) {
	    if( this.remainBytes > 0 ) {
	      rv = this.curSectorReader.read();
	      --this.remainBytes;
	    }
	    if( rv == -1 ) {
	      stopExecution();
	    } else {
	      this.statusRegMain &= 0x7F;
	      if( (this.remainBytes > 0)
		  && this.curSectorReader.isByteAvailable() )
	      {
		this.tStatesTillIOReq = this.tStatesPerMilli * 7 / 1000;
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
		this.tStatesTillIOReq = this.tStatesPerRotation / 10;
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
    this.statusRegMain &= 0xEF;			// Busy=0
    this.statusReg0 = 0;
    if( (driveNum >= 0) && (driveNum < this.seekStatus.length) ) {
      this.seekStatus[ driveNum ] = ((head << 2) & 0x04) | driveNum;
      FloppyDiskDrive drive = getDrive( driveNum );
      if( drive != null ) {
	if( drive.getCylinder() == cyl ) {
	  this.seekStatus[ driveNum ] |= 0x20;	// Seek End
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
	// Not Ready, Seek End, Abnormal Termination
	this.seekStatus[ driveNum ] |= 0xE8;
	this.interruptReq = true;
      }
    } else {
      this.statusReg0 = 0xE8 | ((driveNum << 2) & 0x04) | (driveNum & 0x03);
      this.interruptReq = true;
    }
  }


  private void setByteReadable()
  {
    if( this.dmaMode ) {
      this.dmaReq = true;
    } else {
      this.statusReg0 &= 0xF8;
      this.statusReg0 |= (this.args[ 1 ] & 0x07);
      this.interruptReq = true;
    }
    this.statusRegMain |= 0xC0;
    startOverrunTimer();
  }


  private void setByteWritable()
  {
    if( this.dmaMode ) {
      this.dmaReq = true;
    } else {
      this.statusReg0 &= 0xF8;
      this.statusReg0 |= (this.args[ 1 ] & 0x07);
      this.interruptReq = true;
    }
    this.statusRegMain &= 0x3F;
    this.statusRegMain |= 0x80;
    startOverrunTimer();
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
    this.statusRegMain &= 0x3F;		// kein Datentransfer moeglich
    this.statusRegMain |= 0x10;		// Busy
    if( !this.dmaMode ) {
      this.statusRegMain |= 0x20;
    }
    this.tcFired = false;
  }


  private void setIdle()
  {
    this.statusRegMain  = (this.statusRegMain & 0x0F) | 0x80;
    this.argIdx         = 0;
    this.resultIdx      = -1;
    this.cancelable     = false;
    this.tcFired        = false;
    this.executingDrive = null;
    this.curCmd         = null;
  }


  private void setResultMode()
  {
    this.dmaReq             = false;
    this.tcFired            = false;
    this.tStatesTillIOReq   = -1;
    this.tStatesTillOverrun = -1;
    this.statusRegMain &= 0x0F;
    this.statusRegMain |= 0xD0;		// Data Read, Busy
  }


  private void startFormatTrack()
  {
    setExecutionMode();
    clearRegs012();
    clearSectorID();
    boolean         done  = false;
    FloppyDiskDrive drive = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      if( drive.isReady() ) {
	setDataBuf( this.args[ 3 ] * 4 );
	this.dataPos          = -1;	// kleiner Null!
	this.executingDrive   = drive;
	this.waitForIndexHole = true;
	done                  = true;
      }
    }
    if( !done ) {
      // Abnormal Termination, Equipment Check, Not Ready
      this.statusReg0 = 0xD8 | (this.args[ 1 ] & 0x07);
      stopExecution();
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
    boolean         done  = false;
    FloppyDiskDrive drive = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      if( drive.isReady() ) {
	if( (this.curCmd == Command.SCAN_EQUAL)
	    || (this.curCmd == Command.SCAN_LOW_OR_EQUAL)
	    || (this.curCmd == Command.SCAN_HIGH_OR_EQUAL) )
	{
	  this.scanMode = true;
	  this.dataLen  = 128;
	  this.args[ 5 ] &= 0x0F;
	  if( this.args[ 5 ] > 0 ) {
	    this.dataLen = (128 << this.args[ 5 ]);
	  }
	  this.statusReg2 |= 0x08;	// Scan Equal Hit
	} else {
	  this.scanMode = false;
	  this.dataLen  = getDataLen();
	}
	this.executingDrive   = drive;
	this.tStatesTillIOReq = this.tStatesPerMilli * 8 / 1000 / this.mhz;
	done                  = true;
      }
    }
    if( !done ) {
      // Abnormal Termination, Equipment Check, Not Ready
      this.statusReg0 = 0xD8 | (this.args[ 1 ] & 0x07);
      stopExecution();
    }
  }


  private void startReadID()
  {
    setExecutionMode();
    clearRegs012();
    clearSectorID();
    boolean         done  = false;
    FloppyDiskDrive drive = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      if( drive.isReady() ) {
	this.executingDrive   = drive;
	this.tStatesTillIOReq = this.tStatesPerMilli * 8 / 1000 / this.mhz;
	done                  = true;
      }
    }
    if( !done ) {
      // Abnormal Termination, Equipment Check, Not Ready
      this.statusReg0 = 0xD8 | (this.args[ 1 ] & 0x07);
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
    boolean         done  = false;
    FloppyDiskDrive drive = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      if( drive.isReady() ) {
	this.dataLen          = getDataLen();
	this.executingDrive   = drive;
	this.waitForIndexHole = true;
	done                  = true;
      }
    }
    if( !done ) {
      // Abnormal Termination, Equipment Check, Not Ready
      this.statusReg0 = 0xD8 | (this.args[ 1 ] & 0x07);
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
    boolean         done  = false;
    FloppyDiskDrive drive = getDrive( this.args[ 1 ] & 0x03 );
    if( drive != null ) {
      if( drive.isReady() ) {
	if( drive.isReadOnly() ) {
	  this.statusReg1 |= 0x02;	// Not Writeable
	} else {
	  if( !this.dmaMode ) {
	    this.statusRegMain |= 0x20;
	  }
	  setDataBuf( getDataLen() );
	  this.dataPos          = 0;
	  this.executingDrive   = drive;
	  this.tStatesTillIOReq = this.tStatesPerMilli * 8 / 1000 / this.mhz;
	  done                  = true;
	}
      }
    }
    if( !done ) {
      // Abnormal Termination, Equipment Check, Not Ready
      this.statusReg0 = 0xD8 | (this.args[ 1 ] & 0x07);
      stopExecution();
    }
  }


  private void stopExecution()
  {
    this.executingDrive = null;
    this.results[ 0 ]   = this.sectorIdSizeCode;
    this.results[ 1 ]   = this.sectorIdRec;
    this.results[ 2 ]   = this.sectorIdHead;
    this.results[ 3 ]   = this.sectorIdCyl;
    this.results[ 4 ]   = this.statusReg2;
    this.results[ 5 ]   = this.statusReg1;
    this.results[ 6 ]   = this.statusReg0;
    this.resultIdx      = 6;
    this.statusReg0 &= 0xF8;
    this.statusReg0 |= (this.args[ 1 ] & 0x07);
    this.interruptReq = true;
    setResultMode();
  }


  private void writeCmd( int value )
  {
    this.statusRegMain |= 0x10;		// Busy
    if( this.argIdx == 0 ) {
      this.curCmd                = null;
      this.executingDrive        = null;
      this.resultIdx             = -1;
      this.args[ this.argIdx++ ] = value;
      value &= 0x1F;
      if( value < cmds.length ) {
	this.curCmd = cmds[ value ];
      }
      if( this.curCmd != null ) {
	if( this.debugEnabled ) {
	  System.out.println( "FDC: " + this.curCmd );
	}
	if( this.curCmd == Command.SENSE_INTERRUPT_STATUS ) {
	  execSenseInterruptStatus();
	}
      } else {
	if( this.debugEnabled ) {
	  System.out.println( "FDC: INVALID" );
	}
	this.curCmd       = Command.INVALID;
	this.interruptReq = false;
	this.statusReg0   = 0x80;
	this.results[ 0 ] = this.statusReg0;
	this.resultIdx    = 0;
	setResultMode();
      }
    } else {
      if( this.curCmd != null ) {
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
		int srt = (this.args[ 1 ] >> 4) & 0x0F;
		if( srt > 0 ) {
		  this.stepRateMillis = srt;
		} else {
		  this.stepRateMillis = 16;
		}
		this.dmaMode        = ((this.args[ 2 ] & 0x01) == 0);
		this.headLoadMillis = (this.args[ 2 ] & 0xFE);
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
      } else {
	setIdle();
      }
    }
  }


  private void writeData( int value )
  {
    value &= 0xFF;
    if( this.dataBuf != null ) {
      if( (this.dataPos >= 0)
	  && (this.dataPos < this.dataBuf.length)
	  && (this.dataPos < this.dataLen) )
      {
	if( this.curCmd == Command.FORMAT_TRACK ) {
	  this.tStatesTillOverrun        = -1;
	  this.dataBuf[ this.dataPos++ ] = (byte) value;
	  this.statusRegMain &= 0x7F;
	  if( (this.dataPos < this.dataBuf.length)
	      && (this.dataPos < this.dataLen) )
	  {
	    int t = (this.tStatesPerRotation * this.dataPos / this.dataLen)
						- this.tStateRotationCounter;
	    this.tStatesTillIOReq = (t > 1 ? t : 1);
	  }
	}
	else if( (this.curCmd == Command.WRITE_DATA)
		 || (this.curCmd == Command.WRITE_DELETED_DATA) )
	{
	  this.tStatesTillOverrun        = -1;
	  this.dataBuf[ this.dataPos++ ] = (byte) value;
	  this.statusRegMain &= 0x7F;
	  if( (this.dataPos < this.dataBuf.length)
	      && (this.dataPos < this.dataLen) )
	  {
	    this.tStatesTillIOReq = this.tStatesPerMilli * 7 / 1000;
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
	    this.tStatesTillIOReq = this.tStatesPerRotation / 10;
	  }
	}
	else if( this.scanMode ) {
	  this.tStatesTillOverrun = -1;
	  if( this.curSectorReader != null ) {
	    int b = -1;
	    if( this.remainBytes > 0 ) {
	      b = this.curSectorReader.read();
	      --this.remainBytes;
	    }
	    if( b == -1 ) {
	      if( this.executingDrive != null ) {
		stopExecution();
	      }
	    } else {
	      if( b != value ) {
		this.statusReg2 &= ~0x08;	// Scan Equal Hit zuruecketzen
	      }
	      if( this.curCmd == Command.SCAN_LOW_OR_EQUAL ) {
		if( b > value ) {
		  this.statusReg2 |= 0x04;	// Scan Not Satisfied
		}
	      } else if( this.curCmd == Command.SCAN_HIGH_OR_EQUAL ) {
		if( b < value ) {
		  this.statusReg2 |= 0x04;	// Scan Not Satisfied
		}
	      } else {
		this.statusReg2 |= 0x04;	// Scan Not Satisfied
	      }
	    }
	    this.statusRegMain &= 0x7F;
	    this.tStatesTillIOReq = this.tStatesPerMilli * 7 / 1000;
	  }
	}
      }
    }
  }
}
