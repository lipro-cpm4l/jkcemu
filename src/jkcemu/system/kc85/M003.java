/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des V24-Moduls M003 mit einem an Kanal A angeschlossenen Drucker
 */

package jkcemu.system.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;
import jkcemu.print.PrintMngr;
import z80emu.*;


public class M003 extends AbstractKC85Module implements
						Z80InterruptSource,
						Z80SIOChannelListener
{
  private PrintMngr printMngr;
  private Z80CPU    cpu;
  private Z80CTC    ctc;
  private Z80SIO    sio;


  public M003( int slot, EmuThread emuThread )
  {
    super( slot );
    this.printMngr = emuThread.getPrintMngr();
    this.cpu       = emuThread.getZ80CPU();
    this.ctc       = new Z80CTC( cpu );
    this.sio       = new Z80SIO();
    this.sio.addChannelListener( this, 0 );
    this.cpu.addTStatesListener( this.ctc );
  }


	/* --- Z80InterruptSource --- */

  public synchronized int interruptAccepted()
  {
    int rv = 0;
    if( this.sio.isInterruptRequested() ) {
      rv = this.sio.interruptAccepted();;
    }
    else if( this.ctc.isInterruptRequested() ) {
      rv = this.ctc.interruptAccepted();;
    }
    return rv;
  }


  public synchronized void interruptFinished()
  {
    if( this.sio.isInterruptPending() ) {
      this.sio.interruptFinished();
    }
    else if( this.ctc.isInterruptPending() ) {
      this.ctc.interruptFinished();
    }
  }


  public boolean isInterruptPending()
  {
    return this.sio.isInterruptPending() || this.ctc.isInterruptPending();
  }


  public boolean isInterruptRequested()
  {
    boolean rv = this.sio.isInterruptRequested();
    if( !rv && !this.sio.isInterruptPending() ) {
      rv = this.ctc.isInterruptRequested();
    }
    return rv;
  }


  public void reset()
  {
    this.sio.reset();
    this.ctc.reset();
  }


	/* --- Z80SIOChannelListener --- */

  public void z80SIOChannelByteAvailable( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio) && (channel == 0) )
      this.printMngr.putByte( value );
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    this.cpu.removeTStatesListener( this.ctc );
    this.sio.removeChannelListener( this, 0 );
  }


  public String getModuleName()
  {
    return "M003";
  }


  public int getTypeByte()
  {
    return 0xEE;
  }


  public int readIOByte( int port )
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
	  rv = this.ctc.read( port & 0x03 );
	  break;
      }
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    reset();
  }


  public boolean writeIOByte( int port, int value )
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
	  this.ctc.write( port & 0x03, value );
	  rv = true;
	  break;
      }
    }
    return rv;
  }
}

