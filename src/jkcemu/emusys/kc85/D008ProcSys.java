/*
 * (c) 2018-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des D008-Prozessorsystems
 */

package jkcemu.emusys.kc85;

import java.util.Properties;
import jkcemu.base.EmuUtil;
import jkcemu.base.ScreenFrm;
import jkcemu.emusys.KC85;
import z80emu.Z80CPU;


public class D008ProcSys extends D004ProcSys
{
  private volatile boolean maxSpeedSwitchable;
  private boolean          maxSpeed4MHz;
  private volatile int     dmaTStatesCounter;
  private int              dmaTStatesInit;
  private volatile boolean ramFloppyEnabled;
  private int              ramFloppyBank;
  private byte[]           ramFloppyBytes;


  public D008ProcSys(
		D004       d004,
		ScreenFrm  screenFrm,
		Properties props,
		String     propPrefix )
  {
    super( d004, screenFrm, props, propPrefix );
    this.maxSpeedSwitchable = true;
    this.maxSpeed4MHz       = false;
    this.dmaTStatesCounter  = 0;
    this.dmaTStatesInit     = 0;
    this.ramFloppyEnabled   = false;
    this.ramFloppyBank      = 0;
    this.ramFloppyBytes     = new byte[ 0x80 * 0x4000 ];
  }


  public boolean isLEDofDMAon()
  {
    return (this.dmaTStatesCounter > 0);
  }


  public boolean isLEDofRamFloppyOn()
  {
    return this.ramFloppyEnabled;
  }


  public boolean isMaxSpeed4MHz()
  {
    return this.maxSpeed4MHz;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean alwaysEmulatesGIDE()
  {
    return true;
  }


  @Override
  public void applySettings( Properties props )
  {
    int maxSpeedKHz = EmuUtil.getIntProperty(
		props,
		this.propPrefix + KC85.PROP_DISKSTATION_MAXSPEED_KHZ,
		0 );
    if( maxSpeedKHz > 0 ) {
      this.maxSpeedSwitchable = false;
      setMaxSpeedKHz( maxSpeedKHz );
    } else {
      this.maxSpeedSwitchable = true;
      setMaxSpeedKHz( this.maxSpeed4MHz ? 4000 : 8000 );
    }
  }


  @Override
  public void clearRAM( Properties props )
  {
    EmuUtil.initSRAM( this.ramBytes, props );
    EmuUtil.initSRAM( this.ramFloppyBytes, props );
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( this.ramFloppyEnabled && (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = (this.ramFloppyBank * 0x4000) + (addr - 0x8000);
      if( idx < this.ramFloppyBytes.length ) {
	rv = this.ramFloppyBytes[ idx ] & 0xFF;
      } else {
	rv = super.getMemByte( addr, m1 );
      }
    } else {
      rv = super.getMemByte( addr, m1 );
    }
    return rv;
  }


  @Override
  protected void maxSpeedChanged()
  {
    super.maxSpeedChanged();

    // LED-Nachleuchtzeit 100 ms
    this.dmaTStatesInit = this.cpu.getMaxSpeedKHz() * 100;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x11:
	/*
	 * DMA wird nicht emuliert,
	 * aber wenn die Leitung zur Aktivierung des nicht vorhandenen
	 * DMA-Schaltkreises aktiv geschaltet wird, geht die LED an.
	 */
	setLEDofDMAon();
	break;
      case 0x12:
	rv = this.maxSpeed4MHz ? 0x01 : 0x00;
	break;
      default:
	rv = super.readIOByte( port, tStates );
    }
    return rv;
  }


  @Override
  protected void reset()
  {
    super.reset();
    this.ramFloppyBank    = 0;
    this.ramFloppyEnabled = false;
    this.maxSpeed4MHz     = false;
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( this.ramFloppyEnabled && (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = (this.ramFloppyBank * 0x4000) + (addr - 0x8000);
      if( idx < this.ramFloppyBytes.length ) {
	this.ramFloppyBytes[ idx ] = (byte) value;
	rv = true;
      } else {
	rv = super.setMemByte( addr, value );
      }
    } else {
      rv = super.setMemByte( addr, value );
    }
    return rv;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    switch( port & 0xFF ) {
      case 0x10:
	{
	  boolean oldState = this.ramFloppyEnabled;
	  this.ramFloppyEnabled = ((value & 0x01) != 0);
	  this.ramFloppyBank    = (value >> 1) & 0x7F;
	  if( this.ramFloppyEnabled != oldState ) {
	    this.kc85.setFrontFldDirty();
	  }
	}
	break;
      case 0x11:
	/*
	 * DMA wird nicht emuliert,
	 * aber wenn die Leitung zur Aktivierung des nicht vorhandenen
	 * DMA-Schaltkreises aktiv geschaltet wird, geht die LED an.
	 */
	setLEDofDMAon();
	break;
      case 0x12:
	{
	  boolean oldState  = this.maxSpeed4MHz;
	  this.maxSpeed4MHz = ((value & 0x01) != 0);
	  if( this.maxSpeedSwitchable ) {
	    setMaxSpeedKHz( this.maxSpeed4MHz ? 4000 : 8000 );
	  }
	  if( this.maxSpeed4MHz != oldState ) {
	    this.kc85.setFrontFldDirty();
	  }
	}
	break;
      default:
	super.writeIOByte( port, value, tStates );
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    if( this.dmaTStatesCounter > 0 ) {
      this.dmaTStatesCounter -= tStates;
      if( this.dmaTStatesCounter <= 0 ) {
	this.kc85.setFrontFldDirty();
      }
    }
  }


	/* --- private Methoden --- */

  private void setLEDofDMAon()
  {
    int tStates            = this.dmaTStatesCounter;
    this.dmaTStatesCounter = this.dmaTStatesInit;
    if( (tStates <= 0) && (this.dmaTStatesCounter > 0) ) {
      this.kc85.setFrontFldDirty();
    }
  }
}
