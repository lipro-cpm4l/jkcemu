/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Floppy Disk Station D004
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import jkcemu.disk.FloppyDiskDrive;
import z80emu.*;


public class D004 extends AbstractKC85Module
{
  private byte[]      rom;
  private int         romAddr;
  private boolean     connected;
  private boolean     cpuEnableValue;
  private boolean     cpuStopValue;
  private boolean     cpuResetValue;
  private boolean     cpuNMIValue;
  private D004ProcSys procSys;
  private Thread      thread;


  public D004(
		ScreenFrm  screenFrm,
		Properties props,
		String     propPrefix,
		byte[]     rom )
  {
    super( 0xFC );
    this.rom            = rom;
    this.romAddr        = 0xC000;
    this.connected      = false;
    this.cpuEnableValue = false;
    this.cpuStopValue   = false;
    this.cpuResetValue  = false;
    this.cpuNMIValue    = false;
    this.procSys        = new D004ProcSys( screenFrm, props, propPrefix );
    this.thread         = null;
  }


  public void applySettings( Properties props )
  {
    this.procSys.applySettings( props );
  }


  public boolean canApplySettings( Properties props )
  {
    return this.procSys.canApplySettings( props );
  }


  public Z80CPU getZ80CPU()
  {
    return this.procSys.getZ80CPU();
  }


  public Z80Memory getZ80Memory()
  {
    return this.procSys;
  }


  public synchronized void fireStop()
  {
    if( this.thread != null ) {
      this.procSys.fireStop();
      this.thread = null;
    }
  }


  public int getSupportedFloppyDiskDriveCount()
  {
    return this.procSys.getSupportedFloppyDiskDriveCount();
  }


  public boolean isRunning()
  {
    return this.thread != null;
  }


  public void loadIntoRAM( byte[] loadData, int loadAddr, int startAddr )
  {
    this.procSys.loadIntoRAM( loadData, loadAddr, startAddr );
  }


  public void setDrive( int idx, FloppyDiskDrive drive )
  {
    this.procSys.setDrive( idx, drive );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void die()
  {
    this.procSys.die();
  }


  @Override
  public String getModuleName()
  {
    return "D004";
  }


  @Override
  public int getTypeByte()
  {
    return 0xA7;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled && (this.rom != null) ) {
      int idx = addr - this.romAddr;
      if( (idx >= 0) && (idx < this.rom.length) ) {
	rv = (int) this.rom[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public int readIOByte( int port )
  {
    int rv = -1;
    if( this.connected ) {
      int portL = port & 0xFF;
      if( (portL >= 0xF0) && (portL <= 0xF3) ) {
	rv = this.procSys.getMemByte(
			0xFC00 | (portL << 8) | ((port >> 8) & 0xFF),
			false );
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel )
  {
    fireStop();
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      this.procSys.clearRAM();
    }
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.connected = ((value & 0x04) != 0);
    this.romAddr   = ((value & 0x20) != 0) ? 0xE000 : 0xC000;
  }


  @Override
  public boolean writeIOByte( int port, int value )
  {
    boolean rv    = false;
    int     portL = (port & 0xFF);
    if( (portL >= 0xF0) && (portL <= 0xF4) ) {
      if( this.connected ) {
	if( (portL >= 0xF0) && (portL <= 0xF3) ) {
	  rv = this.procSys.setMemByte(
			0xFC00 | (portL << 8) | ((port >> 8) & 0xFF),
			value );
	}
	else if( portL == 0xF4 ) {
	  boolean state = ((value & 0x01) != 0);
	  if( state != this.cpuEnableValue ) {
	    this.cpuEnableValue = state;
	    if( state ) {
	      enableCPU();
	    }
	  }
	  state = ((value & 0x02) != 0);
	  if( state != this.cpuStopValue ) {
	    this.cpuStopValue = state;
	    if( state ) {
	      fireStop();
	    }
	  }
	  state = ((value & 0x04) != 0);
	  if( state != this.cpuResetValue ) {
	    this.cpuResetValue = state;
	    if( state && (this.thread != null) ) {
	      this.procSys.fireReset();
	    }
	  }
	  state = ((value & 0x08) != 0);
	  if( state != this.cpuNMIValue ) {
	    this.cpuNMIValue = state;
	    if( state && (this.thread != null) ) {
	      this.procSys.fireNMI();
	    }
	  }
	}
      }
      rv = true;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private synchronized void enableCPU()
  {
    if( this.thread == null ) {
      Thread t    = new Thread( this.procSys, "D004" );
      this.thread = t;
      t.start();
    }
  }
}

