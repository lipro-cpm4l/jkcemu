/*
 * (c) 2009-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des D004-Prozessorsystems
 */

package jkcemu.emusys.kc85;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.base.ErrorMsg;
import jkcemu.base.ScreenFrm;
import jkcemu.emusys.KC85;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.GIDE;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80IOSystem;
import z80emu.Z80Memory;
import z80emu.Z80TStatesListener;


public class D004ProcSys implements
				FDC8272.DriveSelector,
				Runnable,
				Z80IOSystem,
				Z80Memory,
				Z80TStatesListener
{
  protected KC85   kc85;
  protected String propPrefix;
  protected byte[] ramBytes;
  protected Z80CPU cpu;

  private static final int DOWN     = 0;
  private static final int START_UP = 1;
  private static final int RUNNING  = 2;

  private D004              d004;
  private ScreenFrm         screenFrm;
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;
  private int               ctcTStatesCounter;
  private volatile int      gideTStatesCounter;
  private int               gideTStatesInit;
  private volatile int      runLevel;
  private Object            runLock;
  private GIDE              gide;
  private FDC8272           fdc;
  private Z80CTC            ctc;


  public D004ProcSys(
		D004       d004,
		ScreenFrm  screenFrm,
		Properties props,
		String     propPrefix )
  {
    this.d004               = d004;
    this.kc85               = d004.getKC85();
    this.screenFrm          = screenFrm;
    this.propPrefix         = propPrefix;
    this.ctcTStatesCounter  = 0;
    this.gideTStatesCounter = 0;
    this.gideTStatesInit    = 0;
    this.runLevel           = DOWN;
    this.runLock            = new Object();
    this.curFDDrive         = null;
    this.fdDrives           = new FloppyDiskDrive[ 4 ];
    this.ramBytes           = new byte[ 0x10000 ];
    clearRAM( props );
    Arrays.fill( this.fdDrives, null );

    if( alwaysEmulatesGIDE() ) {
      this.gide = GIDE.createGIDE( screenFrm, props, propPrefix );
    } else {
      this.gide = GIDE.getGIDE( screenFrm, props, propPrefix );
    }
    this.fdc = new FDC8272( this, 4 );
    this.cpu = new Z80CPU( this, this );
    this.ctc = new Z80CTC( "CTC (FCh-FFh)" );
    this.ctc.setTimerConnection( 0, 1 );
    this.ctc.setTimerConnection( 1, 2 );
    this.ctc.setTimerConnection( 2, 3 );
    this.cpu.setMaxSpeedKHz( 4000 );
    this.cpu.setInterruptSources( this.ctc );
    this.cpu.addMaxSpeedListener( this.fdc );
    this.cpu.addTStatesListener( this );
    this.fdc.setTStatesPerMilli( this.cpu.getMaxSpeedKHz() );
    maxSpeedChanged();
  }


  protected boolean alwaysEmulatesGIDE()
  {
    return false;
  }


  public void applySettings( Properties props )
  {
    setMaxSpeedKHz(
	EmuUtil.getIntProperty(
		props,
		this.propPrefix + KC85.PROP_DISKSTATION_MAXSPEED_KHZ,
		4000 ) );
  }


  public boolean canApplySettings( Properties props )
  {
    return GIDE.complies(
			this.gide,
			props,
			this.propPrefix,
			alwaysEmulatesGIDE() );
  }


  public void clearRAM( Properties props )
  {
    EmuUtil.initDRAM( this.ramBytes );
    if( EmuUtil.isSRAMInit00( props ) ) {
      Arrays.fill( this.ramBytes, 0xFC00, 0x10000, (byte) 0x00 );
    } else {
      EmuUtil.fillRandom( this.ramBytes, 0xFC00 );
    }
  }


  public void die()
  {
    if( this.gide != null ) {
      this.gide.die();
    }
    this.cpu.removeTStatesListener( this );
    this.cpu.removeMaxSpeedListener( this.fdc );
    this.fdc.die();
  }


  public void fireNMI()
  {
    this.cpu.fireNMI();
  }


  public void fireReset()
  {
    this.cpu.fireExit();
  }


  public void fireStop()
  {
    this.runLevel = DOWN;
    this.cpu.fireExit();
  }


  public int getSupportedFloppyDiskDriveCount()
  {
    return this.fdDrives.length;
  }


  public Z80CPU getZ80CPU()
  {
    return this.cpu;
  }


  public boolean isLEDofGIDEon()
  {
    return (this.gideTStatesCounter > 0);
  }


  public void loadIntoRAM(
			byte[] dataBytes,
			int    dataOffs,
			int    addr )
  {
    if( (dataBytes != null) && (addr >= 0) ) {
      while( (dataOffs < dataBytes.length)
	     && (addr < this.ramBytes.length) )
      {
	this.ramBytes[ addr++ ] = dataBytes[ dataOffs++ ];
      }
    }
  }


  protected void maxSpeedChanged()
  {
    // LED-Nachleuchtzeit 100 ms
    this.gideTStatesInit = this.cpu.getMaxSpeedKHz() * 100;
  }


  protected void reset()
  {
    this.curFDDrive = null;
    this.fdc.reset( false );
    if( this.gide != null ) {
      this.gide.reset();
    }
    this.ctc.reset( false );
    this.cpu.reset( false );
  }


  public void setDrive( int idx, FloppyDiskDrive drive )
  {
    if( (idx >= 0) && (idx < this.fdDrives.length) )
      this.fdDrives[ idx ] = drive;
  }


  protected void setMaxSpeedKHz( int maxSpeedKHz )
  {
    this.cpu.setMaxSpeedKHz( maxSpeedKHz );
    this.fdc.setTStatesPerMilli( this.cpu.getMaxSpeedKHz() );
    maxSpeedChanged();
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    return this.curFDDrive;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    this.runLevel = START_UP;
    /*
     * Sicherstellen, dass nicht zwei Threads gleichzeitig laufen;
     * Es koennte sein, dass durch Aenderung der Einstellungen
     * von Emulation mit D004 auf Emulation ohne D004 und
     * danach wieder auf Emulation mit D004 der erste D004-Thread
     * zwar das Abbruchsignal erhalten hat, aber noch nicht zu Ende ist,
     * weil z.B. eine IO-Operation des FDC haengt.
     */
    synchronized( this.runLock ) {
      try {
	boolean powerOn = true;
	while( this.runLevel > DOWN ) {
	  reset();
	  this.cpu.run();
	  powerOn = false;
	}
      }
      catch( Exception ex ) {
	ErrorMsg.showLater(
		this.screenFrm,
		this.d004.getModuleName() + " aufgrund eines Fehlers"
			+ " in Dauer-RESET gegangen",
		ex );
      }
      finally {
	this.runLevel = DOWN;
      }
    }
  }


	/* --- Z80IOSystem --- */

  @Override
  public int  readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( ((port & 0xF0) == 0) && (this.gide != null) ) {
      setLEDofGIDEon();
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv = value;
      }
    } else {
      switch( port & 0xFF ) {
	case 0xF0:
	  rv = this.fdc.readMainStatusReg();
	  break;

	case 0xF1:
	  rv = this.fdc.readData();
	  break;

	case 0xF2:
	case 0xF3:
	  rv = this.fdc.readDMA();
	  break;

	case 0xF4:		// Input Gate
	  {
	    rv = 0xDF;		// Bit 0..3 nicht benutzt, 4..7 invertiert
	    FloppyDiskDrive fdd = this.curFDDrive;
	    if( fdd != null ) {
	      if( fdd.isReady() ) {
		rv |= 0x20;
		if( this.fdc.getIndexHoleState() ) {
		  rv &= ~0x10;
		}
	      }
	    }
	    if( this.fdc.isInterruptRequest() ) {
	      rv &= ~0x40;
	    }
	    if( this.fdc.isDMARequest() ) {
	      rv &= ~0x80;
	    }
	  }
	  break;

	case 0xF8:
	case 0xF9:
	  this.fdc.fireTC();
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  rv = this.ctc.read( port & 0x03, tStates );
	  break;
      }
    }
    return rv;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( ((port & 0xF0) == 0) && (this.gide != null) ) {
      setLEDofGIDEon();
      this.gide.write( port, value );
    } else {
      switch( port & 0xFF ) {
	case 0xF1:
	  this.fdc.write( value );
	  break;

	case 0xF2:
	case 0xF3:
	  this.fdc.writeDMA( value );
	  break;

	case 0xF6:
	case 0xF7:
	  {
	    FloppyDiskDrive fdd  = null;
	    int             mask = 0x01;
	    for( int i = 0; i < this.fdDrives.length; i++ ) {
	      if( (value & mask) != 0 ) {
		fdd = this.fdDrives[ i ];
		break;
	      }
	      mask <<= 1;
	    }
	    this.curFDDrive = fdd;
	    this.fdc.setHDMode( (value & 0x40) != 0 );
	  }
	  break;

	case 0xF8:
	case 0xF9:
	  this.fdc.fireTC();
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  this.ctc.write( port & 0x03, value, tStates );
	  break;
      }
    }
  }


	/* --- Z80Memory --- */

  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;
    return (addr >= 0) && (addr < this.ramBytes.length) ?
			(int) this.ramBytes[ addr ] & 0xFF
			: 0;
  }


  @Override
  public int getMemWord( int addr )
  {
    return (getMemByte( addr + 1, false ) << 8) | getMemByte( addr, false );
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;
    if( this.runLevel == START_UP ) {
      if( addr < 0xFC00 ) {
	this.ramBytes[ addr ] = (byte) 0;
      } else {
	this.runLevel = RUNNING;
      }
    }
    return getMemByte( addr, m1 );
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( (addr >= 0) && (addr < this.ramBytes.length) ) {
      this.ramBytes[ addr ] = (byte) value;
      rv          = true;
    }
    return rv;
  }


  @Override
  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctcTStatesCounter += tStates;
    while( this.ctcTStatesCounter >= 8 ) {
      ctc.externalUpdate( 0, 1 );
      this.ctcTStatesCounter -= 8;
    }
    if( this.gideTStatesCounter > 0 ) {
      this.gideTStatesCounter -= tStates;
      if( this.gideTStatesCounter <= 0 ) {
	this.kc85.setFrontFldDirty();
      }
    }
    this.ctc.z80TStatesProcessed( cpu, tStates );
    this.fdc.z80TStatesProcessed( cpu, tStates );
  }


	/* --- private Methoden --- */

  private void setLEDofGIDEon()
  {
    int tStates             = this.gideTStatesCounter;
    this.gideTStatesCounter = this.gideTStatesInit;
    if( (tStates <= 0) && (this.gideTStatesCounter > 0) ) {
      this.kc85.setFrontFldDirty();
    }
  }
}
