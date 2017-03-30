/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des V24-Moduls M003 mit einem an Kanal A angeschlossenen Drucker
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;
import jkcemu.print.PrintMngr;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80SIO;
import z80emu.Z80SIOChannelListener;
import z80emu.Z80TStatesListener;


public class M003 extends AbstractKC85Module implements
						Z80CTCListener,
						Z80InterruptSource,
						Z80SIOChannelListener,
						Z80TStatesListener
{
  private String    title;
  private int       remainTStates;
  private PrintMngr printMngr;
  private Z80CPU    cpu;
  private Z80CTC    ctc;
  private Z80SIO    sio;


  public M003( int slot, EmuThread emuThread )
  {
    super( slot );
    this.title         = String.format( "M003 im Schacht %02X", slot );
    this.remainTStates = 0;
    this.printMngr     = emuThread.getPrintMngr();
    this.cpu           = emuThread.getZ80CPU();
    this.ctc           = new Z80CTC( "CTC (M003)" );
    this.sio           = new Z80SIO( "SIO (M003)" );
    this.sio.addChannelListener( this, 0 );
    this.ctc.addCTCListener( this );
    this.cpu.addTStatesListener( this );
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      if( timerNum == 0 ) {
	this.sio.clockPulseSenderA();
	this.sio.clockPulseReceiverA();
      } else if( timerNum == 1 ) {
	this.sio.clockPulseSenderB();
	this.sio.clockPulseReceiverB();
      }
    }
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<h2>CTC (E/A-Adressen 0C-0F)</h2>\n" );
    this.ctc.appendInterruptStatusHTMLTo( buf );
    buf.append( "<br/><br/>\n"
		+ "<h2>SIO (E/A-Adressen 08-0B)</h2>\n" );
    this.sio.appendInterruptStatusHTMLTo( buf );
  }


  @Override
  public synchronized int interruptAccept()
  {
    int rv = 0;
    if( this.sio.isInterruptRequested() ) {
      rv = this.sio.interruptAccept();
    }
    else if( this.ctc.isInterruptRequested() ) {
      rv = this.ctc.interruptAccept();
    }
    return rv;
  }


  @Override
  public synchronized void interruptFinish()
  {
    if( this.sio.isInterruptAccepted() ) {
      this.sio.interruptFinish();
    }
    else if( this.ctc.isInterruptAccepted() ) {
      this.ctc.interruptFinish();
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.sio.isInterruptAccepted() || this.ctc.isInterruptAccepted();
  }


  @Override
  public boolean isInterruptRequested()
  {
    boolean rv = this.sio.isInterruptRequested();
    if( !rv && !this.sio.isInterruptAccepted() ) {
      rv = this.ctc.isInterruptRequested();
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn )
  {
    this.ctc.reset( powerOn );
    this.sio.reset( powerOn );
    this.sio.setClearToSendA( true );
    this.sio.setClearToSendB( true );
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOByteSent( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio) && (channel == 0) ) {
      this.printMngr.putByte( value );
      this.sio.setClearToSendA( false );
      this.sio.setClearToSendA( true );
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );

    // Systemtakt durch 2 teilen und den ersten beiden CTC-Kanaelen zufuehren
    this.remainTStates += tStates;
    int pulses = this.remainTStates / 2;
    if( pulses > 0 ) {
      this.ctc.externalUpdate( 0, pulses );
      this.ctc.externalUpdate( 1, pulses );
      this.remainTStates -= (pulses * 2);
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void die()
  {
    this.cpu.removeTStatesListener( this );
    this.ctc.removeCTCListener( this );
    this.sio.removeChannelListener( this, 0 );
  }


  @Override
  public String getModuleName()
  {
    return "M003";
  }


  @Override
  public int getTypeByte()
  {
    return 0xEE;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = -1;
    if( this.enabled ) {
      switch( port & 0xFF ) {
	case 0x08:
	  rv = this.sio.readDataA();
	  break;

	case 0x09:
	  rv = this.sio.readDataB();
	  break;

	case 0x0A:
	  rv = this.sio.readControlA();
	  break;

	case 0x0B:
	  rv = this.sio.readControlB();
	  break;

	case 0x0C:
	case 0x0D:
	case 0x0E:
	case 0x0F:
	  rv = this.ctc.read( port & 0x03, tStates );
	  break;
      }
    }
    return rv;
  }


  @Override
  public boolean supportsPrinter()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return this.title;
  }


  @Override
  public boolean writeIOByte( int port, int value, int tStates )
  {
    boolean rv = false;
    if( this.enabled ) {
      switch( port & 0xFF ) {
	case 0x08:
	  this.sio.writeDataA( value );
	  rv = true;
	  break;

	case 0x09:
	  this.sio.writeDataB( value );
	  rv = true;
	  break;

	case 0x0A:
	  this.sio.writeControlA( value );
	  rv = true;
	  break;

	case 0x0B:
	  this.sio.writeControlB( value );
	  rv = true;
	  break;

	case 0x0C:
	case 0x0D:
	case 0x0E:
	case 0x0F:
	  this.ctc.write( port & 0x03, value, tStates );
	  rv = true;
	  break;
      }
    }
    return rv;
  }
}
