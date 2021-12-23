/*
 * (c) 2017-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Sound-Moduls M066
 */

package jkcemu.emusys.kc85;

import jkcemu.audio.AudioOut;
import jkcemu.emusys.KC85;
import jkcemu.etc.PSG8910;
import jkcemu.etc.PSGSoundDevice;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class M066 extends AbstractKC85Module
				implements
					PSG8910.Callback,
					Z80InterruptSource,
					Z80MaxSpeedListener,
					Z80TStatesListener

{
  private KC85           kc85;
  private int            counter500KHz;
  private int            levelCounter;
  private volatile int   levelValue;
  private volatile int   maxSpeedKHz;
  private int            psgReg;
  private PSG8910        psg;
  private PSGSoundDevice soundDevice;
  private Z80CTC         ctc;


  public M066( int slot, KC85 kc85 )
  {
    super( slot, false );
    this.kc85 = kc85;
    this.ctc  = new Z80CTC( "CTC (M066)" );
    this.ctc.setTimerConnection( 0, 1 );
    this.ctc.setTimerConnection( 1, 2 );
    this.ctc.setTimerConnection( 2, 3 );
    this.counter500KHz = 0;
    this.levelCounter  = 0;
    this.levelValue    = 0;
    this.maxSpeedKHz   = 0;
    this.psgReg        = 0;
    this.psg           = new PSG8910( 2000000, this );
    this.soundDevice   = null;
  }


  public synchronized PSGSoundDevice getSoundDevice()
  {
    if( this.soundDevice == null ) {
      this.soundDevice = new PSGSoundDevice(
				String.format(
					"M066 im Schacht %02X",
					this.slot ),
				true,
				this.psg );
      this.psg.start();
    }
    return this.soundDevice;
  }


	/* --- PSG8910.Callback --- */

  @Override
  public int psgReadPort( PSG8910 psg, int port )
  {
    return 0;
  }


  @Override
  public void psgWritePort( PSG8910 psg, int port, int value )
  {
    // leer
  }


  @Override
  public void psgWriteFrame( PSG8910 psg, int a, int b, int c )
  {
    if( this.soundDevice != null ) {
      int monoValue = (a + b + c) / 3;
      if( monoValue >= this.levelValue ) {
	setLevelValue( monoValue );
      } else {
	if( this.levelCounter > 0 ) {
	  --this.levelCounter;
	} else {
	  /*
	   * 256 Schritte pro Sekunde zum Abklingen,
	   * entspricht knapp eine Selunde Abklingzeit;
	   */
	  this.levelCounter = (this.psg.getFrameRate() % 256);
	  if( this.levelValue > 0 ) {
	    setLevelValue( this.levelValue - 1 );
	  }
	}
      }
      this.soundDevice.writeFrames(
			1,
			monoValue,
			(c + (b / 2)) * 2 / 3,
			(a + (b / 2)) * 2 / 3 );
    }
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<h2>PSG (E/A-Adressen 38h/39h)</h2>\n" );
    this.psg.appendStatusHTMLTo( buf );
    buf.append( "<br/><br/>\n"
		+ "<h2>CTC (E/A-Adressen 3Ch-3Fh)</h2>\n" );
    this.ctc.appendInterruptStatusHTMLTo( buf );
  }


  @Override
  public synchronized int interruptAccept()
  {
    return this.ctc.interruptAccept();
  }


  @Override
  public synchronized boolean interruptFinish( int addr )
  {
    return this.ctc.interruptFinish( addr );
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.ctc.isInterruptAccepted();
  }


  @Override
  public boolean isInterruptRequested()
  {
    return this.ctc.isInterruptRequested();
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.maxSpeedKHz = cpu.getMaxSpeedKHz();
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );

    // 500 KHz Takt fuer CTC-Timer 0 erzeugen
    int maxSpeedKHz = this.maxSpeedKHz;
    if( maxSpeedKHz > 0 ) {
      int pulses = 0;
      synchronized( this ) {
	this.counter500KHz += (500 * tStates);
	pulses = this.counter500KHz / maxSpeedKHz;
	this.counter500KHz -= (pulses * maxSpeedKHz);
      }
      if( pulses > 0 ) {
	this.ctc.externalUpdate( 0, pulses );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void die()
  {
    if( this.soundDevice != null ) {
      this.psg.fireStop();
      this.soundDevice.fireStop();
    }
  }


  @Override
  public int getLEDrgb()
  {
    int v = Math.min( this.levelValue, 0xFF );
    int b = Math.min( 4 * v, 0xFF );
    return 0xFF000000
		| Math.max(
			KC85FrontFld.RGB_LED_DARK & 0xFF0000,
			(v << 16) & 0xFF0000 )
		| Math.max(
			KC85FrontFld.RGB_LED_DARK & 0x00FF00,
			(v << 8) & 0x00FF00 )
		| Math.max(
			KC85FrontFld.RGB_LED_DARK & 0x0000FF,
			b & 0x0000FF );
  }


  @Override
  public String getModuleName()
  {
    return "M066";
  }


  @Override
  public int getTypeByte()
  {
    return 0xDC;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = -1;
    switch( port & 0xFF ) {
      case 0x38:
	rv = this.psg.getRegister( this.psgReg );
	break;
      case 0x39:
	rv = 0xFF;
	break;
      case 0x3A:
      case 0x3B:
	rv = getTypeByte();
	break;
      case 0x3C:
      case 0x3D:
      case 0x3E:
      case 0x3F:
	rv = this.ctc.read( port & 0x03, tStates );
	break;
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn )
  {
    this.counter500KHz = 0;
    this.levelCounter  = 0;
    setLevelValue( 0 );
    this.ctc.reset( powerOn );
    if( this.soundDevice != null ) {
      this.psg.reset();
      this.soundDevice.reset();
    }
  }


  @Override
  public boolean writeIOByte( int port, int value, int tStates )
  {
    boolean rv = false;
    switch( port & 0xFF ) {
      case 0x38:
	this.psgReg = value & 0x0F;
	rv          = true;
	break;
      case 0x39:
	this.psg.setRegister( this.psgReg, value & 0xFF );
	rv = true;
	break;
      case 0x3C:
      case 0x3D:
      case 0x3E:
      case 0x3F:
	this.ctc.write( port & 0x03, value, tStates );
	rv = true;
	break;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void setLevelValue( int value )
  {
    if( value != this.levelValue ) {
      this.levelValue = value;
      this.kc85.setFrontFldDirty();
    }
  }
}
