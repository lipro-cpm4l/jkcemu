/*
 * (c) 2009-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des D004-Prozessorsystems
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.*;
import javax.swing.SwingUtilities;
import jkcemu.base.*;
import jkcemu.disk.*;
import z80emu.*;


public class D004ProcSys implements
				FDC8272.DriveSelector,
				Runnable,
				Z80CTCListener,
				Z80IOSystem,
				Z80Memory,
				Z80TStatesListener
{
  private ScreenFrm         screenFrm;
  private String            propPrefix;
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;
  private byte[]            ram;
  private byte[]            loadData;
  private int               loadAddr;
  private int               startAddr;
  private int               ctcTStateCounter;
  private volatile int      runLevel;
  private Object            loadLock;
  private Object            runLock;
  private GIDE              gide;
  private FDC8272           fdc;
  private Z80CPU            cpu;
  private Z80CTC            ctc;


  public D004ProcSys(
		ScreenFrm  screenFrm,
		Properties props,
		String     propPrefix )
  {
    this.screenFrm        = screenFrm;
    this.propPrefix       = propPrefix;
    this.ctcTStateCounter = 0;
    this.runLevel         = 0;
    this.startAddr        = -1;
    this.loadAddr         = -1;
    this.loadData         = null;
    this.curFDDrive       = null;
    this.fdDrives         = new FloppyDiskDrive[ 4 ];
    this.ram              = new byte[ 0x10000 ];
    clearRAM();
    Arrays.fill( this.fdDrives, null );

    this.loadLock = new Object();
    this.runLock  = new Object();
    this.gide     = GIDE.getGIDE( this.screenFrm, props, propPrefix );
    this.fdc      = new FDC8272( this, 4 );
    this.cpu      = new Z80CPU( this, this );
    this.ctc      = new Z80CTC( "CTC (FCh-FFh)" );
    this.ctc.addCTCListener( this );
    this.cpu.setMaxSpeedKHz( 4000 );
    this.cpu.setInterruptSources( this.ctc );
    this.cpu.addMaxSpeedListener( this.fdc );
    this.cpu.addTStatesListener( this );
    this.fdc.setTStatesPerMilli( this.cpu.getMaxSpeedKHz() );
  }


  public void applySettings( Properties props )
  {
    this.cpu.setMaxSpeedKHz(
		EmuUtil.getIntProperty(
				props,
				this.propPrefix + "d004.maxspeed.khz",
				4000 ) );
    this.fdc.setTStatesPerMilli( this.cpu.getMaxSpeedKHz() );
  }


  public boolean canApplySettings( Properties props )
  {
    return GIDE.complies( this.gide, props, this.propPrefix );
  }


  public void clearRAM()
  {
    Arrays.fill( this.ram, (byte) 0 );
  }


  public void die()
  {
    if( this.gide != null ) {
      this.gide.die();
    }
    this.cpu.removeTStatesListener( this );
    this.cpu.removeMaxSpeedListener( this.fdc );
    this.ctc.removeCTCListener( this );
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
    this.runLevel = 0;
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


  public void loadIntoRAM( byte[] loadData, int loadAddr, int startAddr )
  {
    if( loadData != null ) {
      if( (this.runLevel > 0) && (startAddr >= 0) ) {
	synchronized( this.loadLock ) {
	  this.loadData  = loadData;
	  this.loadAddr  = loadAddr;
	  this.startAddr = startAddr;
	  this.cpu.fireExit();
	}
      } else {
	loadIntoRAM( loadData, loadAddr );
      }
    }
  }


  public void setDrive( int idx, FloppyDiskDrive drive )
  {
    if( (idx >= 0) && (idx < this.fdDrives.length) )
      this.fdDrives[ idx ] = drive;
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
    this.runLevel = 1;
    /*
     * Sicherstellen, dass nicht zwei Threads gleichzeitig laufen;
     * Es koennte sein, dass durch Aenderung der Einstellungen
     * von Emulation mit D004 auf Emulation ohne D004 und
     * danach wieder auf Emulation mit D004 der erste D004-Thread
     * zwar das Abbruchsignal erhalten hat, aber noch nicht zu Ende ist,
     * weil z.B. eine IO-Operation des FDC haengt.
     */
    synchronized( this.runLock ) {
      boolean powerOn = true;
      while( this.runLevel > 0 ) {
	try {
	  byte[] loadData  = null;
	  int    loadAddr  = -1;
	  int    startAddr = -1;
	  synchronized( this.loadLock ) {
	    loadData       = this.loadData;
	    loadAddr       = this.loadAddr;
	    startAddr      = this.startAddr;
	    this.loadData  = null;
	    this.loadAddr  = -1;
	    this.startAddr = -1;
	  }
	  if( (loadData != null) && (loadAddr >= 0) ) {
	    loadIntoRAM( loadData, loadAddr );
	    if( startAddr >= 0 ) {
	      this.cpu.setRegPC( startAddr );
	    }
	  } else {
	    this.curFDDrive = null;
	    this.fdc.reset( powerOn );
	    if( this.gide != null ) {
	      this.gide.reset();
	    }
	    this.cpu.resetCPU( powerOn );
	  }
	  this.cpu.run();
	}
	catch( Z80ExternalException ex ) {}
	catch( Exception ex ) {
	  SwingUtilities.invokeLater( new ErrorMsg( this.screenFrm, ex ) );
	}
      }
      powerOn = false;
    }
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      if( (timerNum >= 0) && (timerNum <= 2) ) {
	ctc.externalUpdate( timerNum + 1, 1 );
      }
    }
  }


	/* --- Z80IOSystem --- */

  @Override
  public int  readIOByte( int port )
  {
    int rv = 0xFF;
    if( ((port & 0xF0) == 0) && (this.gide != null) ) {
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
	  rv = this.ctc.read( port & 0x03 );
	  break;
      }
    }
    return rv;
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    if( ((port & 0xF0) == 0) && (this.gide != null) ) {
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
	  this.ctc.write( port & 0x03, value );
	  break;
      }
    }
  }


	/* --- Z80Memory --- */

  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;
    return (addr >= 0) && (addr < this.ram.length) ?
			(int) this.ram[ addr ] & 0xFF
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
    int rv = 0;
    if( (this.runLevel > 0) && (addr >= 0) && (addr < this.ram.length) ) {
      if( this.runLevel == 1 ) {
	if( addr >= 0xFC00 ) {
	  this.runLevel = 2;
	} else {
	  setMemByte( addr, 0 );
	  this.ram[ addr ] = (byte) rv;
	}
      }
      if( runLevel > 1 ) {
	rv = getMemByte( addr, m1 );
      }
    }
    return rv;
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( (addr >= 0) && (addr < this.ram.length) ) {
      this.ram[ addr ] = (byte) value;
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
    this.ctcTStateCounter += tStates;
    while( this.ctcTStateCounter >= 8 ) {
      ctc.externalUpdate( 0, 1 );
      this.ctcTStateCounter -= 8;
    }
    this.ctc.z80TStatesProcessed( cpu, tStates );
    this.fdc.z80TStatesProcessed( cpu, tStates );
  }


	/* --- private Methoden --- */

  private void loadIntoRAM( byte[] dataBytes, int addr )
  {
    if( (dataBytes != null) && (addr >= 0) ) {
      int pos = 0;
      while( (pos < dataBytes.length) && (addr < this.ram.length) ) {
	this.ram[ addr++ ] = dataBytes[ pos++ ];
      }
    }
  }
}

