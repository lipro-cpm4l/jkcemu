/*
 * (c) 2017-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des K1520-Sound-Moduls
 */

package jkcemu.etc;

import jkcemu.audio.AudioOut;
import jkcemu.base.EmuSys;
import jkcemu.etc.PSG8910;
import jkcemu.etc.PSGSoundDevice;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class K1520Sound implements
				PSG8910.Callback,
				Z80InterruptSource,
				Z80MaxSpeedListener,
				Z80TStatesListener

{
  private static final int PSG_CLOCK_KHZ = 1790;

  private EmuSys         emuSys;
  private Z80CTC         ctc;
  private PSG8910        psg;
  private PSGSoundDevice soundDevice;
  private int            psgReg;
  private int            ioBaseAddr;
  private int            counterClk4;
  private volatile int   maxSpeedKHz;


  public K1520Sound( EmuSys emuSys, int ioBaseAddr )
  {
    this.emuSys     = emuSys;
    this.ioBaseAddr = ioBaseAddr;
    this.ctc        = new Z80CTC(
			String.format(
				"CTC (E/A-Adressen %02X-%02X)",
				ioBaseAddr + 4,
				ioBaseAddr + 7 ) );
    this.ctc.setTimerConnection( 0, 1 );
    this.ctc.setTimerConnection( 1, 2 );
    this.ctc.setTimerConnection( 2, 3 );
    this.counterClk4 = 0;
    this.maxSpeedKHz = 0;
    this.psgReg      = 0;

    this.psg         = new PSG8910( PSG_CLOCK_KHZ * 1000, this );
    this.soundDevice = new PSGSoundDevice(
				"K1520-Sound-Karte",
				true,
				psg );
    this.psg.start();
  }


  public void die()
  {
    this.soundDevice.fireStop();
  }


  public PSGSoundDevice getSoundDevice()
  {
    return this.soundDevice;
  }


  public int read( int port, int tStates )
  {
    int rv = -1;
    switch( (port - this.ioBaseAddr) & 0x07 ) {
      case 0x00:
	rv = this.psg.getRegister( this.psgReg );
	break;
      case 0x04:
      case 0x05:
      case 0x06:
      case 0x07:
	rv = this.ctc.read( port & 0x03, tStates );
	break;
    }
    return rv;
  }


  public void reset( boolean powerOn )
  {
    this.counterClk4 = 0;
    this.ctc.reset( powerOn );
    this.soundDevice.reset();
  }


  public boolean write( int port, int value, int tStates )
  {
    boolean rv = false;
    switch( (port - this.ioBaseAddr) & 0x07 ) {
      case 0x00:
	this.psgReg = value & 0x0F;
	rv          = true;
	break;
      case 0x01:
	this.psg.setRegister( this.psgReg, value & 0xFF );
	rv = true;
	break;
      case 0x04:
      case 0x05:
      case 0x06:
      case 0x07:
	this.ctc.write( port & 0x03, value, tStates );
	rv = true;
	break;
    }
    return rv;
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
    this.soundDevice.writeFrames(
				1,
				(a + b + c) / 3,
				(c + (b / 2)) * 2 / 3,
				(a + (b / 2)) * 2 / 3);
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    buf.append(
	String.format(
		"<h2>PSG (E/A-Adressen %02Xh/%02Xh)</h2>\n",
		this.ioBaseAddr,
		this.ioBaseAddr + 1 ) );
    this.psg.appendStatusHTMLTo( buf );
    buf.append(
	String.format(
		"<br/><br/>\n"
			+ "<h2>CTC (E/A-Adressen %02Xh-%02Xh)</h2>\n",
		this.ioBaseAddr + 4,
		this.ioBaseAddr + 7 ) );
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

    // CLK/4-Takt fuer CTC-Timer 0 erzeugen
    int maxSpeedKHz = this.maxSpeedKHz;
    if( maxSpeedKHz > 0 ) {
      int pulses = 0;
      synchronized( this ) {
	this.counterClk4 += (PSG_CLOCK_KHZ * tStates / 4);
	pulses = this.counterClk4 / maxSpeedKHz;
	this.counterClk4 -= (pulses * maxSpeedKHz);
      }
      if( pulses > 0 ) {
	this.ctc.externalUpdate( 0, pulses );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.soundDevice.toString();
  }
}
