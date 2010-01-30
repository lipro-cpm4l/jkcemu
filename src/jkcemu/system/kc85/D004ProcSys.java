/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des D004-Prozessorsystems
 */

package jkcemu.system.kc85;

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
  private FloppyDiskDrive   curFDDrive;
  private FloppyDiskDrive[] fdDrives;
  private int               ctcTStateCounter;
  private volatile boolean  powerOn;
  private volatile boolean  running;
  private byte[]            ram;
  private Object            lockObj;
  private FDC8272           fdc;
  private Z80CPU            cpu;
  private Z80CTC            ctc;


  public D004ProcSys( ScreenFrm screenFrm )
  {
    this.screenFrm        = screenFrm;
    this.ctcTStateCounter = 0;
    this.powerOn          = true;
    this.running          = false;
    this.ram              = new byte[ 0x10000 ];
    this.curFDDrive       = null;
    this.fdDrives         = new FloppyDiskDrive[ 4 ];
    Arrays.fill( this.fdDrives, null );

    this.lockObj = new Object();
    this.fdc     = new FDC8272( this, 4 );
    this.cpu     = new Z80CPU( this, this );
    this.ctc     = new Z80CTC( this.cpu );
    this.ctc.addCTCListener( this );
    this.cpu.setMaxSpeedKHz( 4000 );
    this.cpu.setInterruptSources( this.ctc );
    this.cpu.addMaxSpeedListener( this.fdc );
    this.cpu.addTStatesListener( this );
    this.fdc.setTStatesPerMilli( this.cpu.getMaxSpeedKHz() );
  }


  public void applySettings( Properties props )
  {
    this.cpu.setMaxSpeedKHz( EmuUtil.getIntProperty(
					props,
					"jkcemu.kc85.d004.maxspeed.khz",
					4000 ) );
  }


  public void die()
  {
    this.cpu.removeTStatesListener( this );
    this.cpu.removeMaxSpeedListener( this.fdc );
    this.ctc.removeCTCListener( this );
  }


  public void fireNMI()
  {
    this.cpu.fireNMI();
  }


  public void fireReset()
  {
    this.cpu.fireExit();
  }


  public void fireStop( boolean powerOn )
  {
    this.powerOn = powerOn;
    this.running = false;
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


  public void setDrive( int idx, FloppyDiskDrive drive )
  {
    if( (idx >= 0) && (idx < this.fdDrives.length) )
      this.fdDrives[ idx ] = drive;
  }


	/* --- FDC8272.DriveSelector --- */

  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    return this.curFDDrive;
  }


	/* --- Runnable --- */

  public void run()
  {
    /*
     * Sicherstellen, dass nicht zwei Threads gleichzeitig laufen;
     * Es koennte sein, dass durch Aenderung der Einstellungen
     * von Emulation mit D004 auf Emulation ohne D004 und
     * danach wieder auf Emulation mit D004 der erste D004-Thread
     * zwar das Abbruchsignal erhalten hat, aber noch nicht zu Ende ist,
     * weil z.B. eine IO-Operation des FDC haengt.
     */
    synchronized( this.lockObj ) {
      boolean initDRAM = true;
      this.running     = true;
      while( this.running ) {
	try {
	  if( initDRAM ) {
	    Arrays.fill( this.ram, 0, 0xFC00, (byte) 0 );
	    initDRAM = false;
	  }
	  this.curFDDrive = null;
	  this.fdc.reset( this.powerOn );
	  this.cpu.resetCPU( this.powerOn );
	  this.powerOn = false;
	  this.cpu.run();
	  if( this.powerOn ) {
	    EmuUtil.fillRandom( this.ram, 0xFC00 );
	  }
	}
	catch( Z80ExternalException ex ) {}
	catch( Exception ex ) {
	  SwingUtilities.invokeLater( new ErrorMsg( this.screenFrm, ex ) );
	}
      }
    }
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      if( (timerNum >= 0) && (timerNum <= 2) ) {
	ctc.externalUpdate( timerNum + 1, 1 );
      }
    }
  }


	/* --- Z80IOSystem --- */

  public int  readIOByte( int port )
  {
    int rv = 0xFF;
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
	  if( this.fdc.getIndexHoleState() ) {
	    rv &= ~0x10;
	  }
	  FloppyDiskDrive fdd = this.curFDDrive;
	  if( fdd != null ) {
	    if( fdd.isReady() ) {
	      rv |= 0x20;
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
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
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


	/* --- Z80Memory --- */

  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;
    return (addr >= 0) && (addr < this.ram.length) ?
				(int) this.ram[ addr ] & 0xFF
				: 0xFF;
  }


  public int getMemWord( int addr )
  {
    return (getMemByte( addr + 1, false ) << 8) | getMemByte( addr, false );
  }


  public int readMemByte( int addr, boolean m1 )
  {
    return getMemByte( addr, m1 );
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( (addr >= 0) && (addr < this.ram.length) ) {
      this.ram[ addr ] = (byte) value;
      rv = true;
    }
    return rv;
  }


  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctcTStateCounter += tStates;
    while( this.ctcTStateCounter >= 8 ) {
      ctc.externalUpdate( 0, 1 );
      this.ctcTStateCounter -= 8;
    }
    this.fdc.z80TStatesProcessed( cpu, tStates );
  }
}

