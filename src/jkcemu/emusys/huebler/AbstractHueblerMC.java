/*
 * (c) 2009-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse zur Emulation eines Huebler-Computers
 */

package jkcemu.emusys.huebler;

import java.awt.event.KeyEvent;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Properties;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80InterruptSource;
import z80emu.Z80PCListener;
import z80emu.Z80PIO;


public abstract class AbstractHueblerMC
				extends EmuSys
				implements Z80PCListener
{
  protected boolean pcListenerAdded;
  protected int     keyChar;
  protected Z80CTC  ctc;
  protected Z80PIO  pio;

  private int pasteStateNum;


  public AbstractHueblerMC(
			EmuThread  emuThread,
			Properties props,
			String     propPrefix )
  {
    super( emuThread, props, propPrefix );
    this.pcListenerAdded = false;
  }


  protected synchronized void checkAddPCListener(
					Properties props,
					String     propName )
  {
    boolean state = EmuUtil.getBooleanProperty( props, propName, true );
    if( state != this.pcListenerAdded ) {
      Z80CPU cpu = this.emuThread.getZ80CPU();
      if( state ) {
	cpu.addPCListener( this, 0xF00F );
      } else {
	cpu.removePCListener( this );
      }
      this.pcListenerAdded = state;
    }
  }


  protected void createIOSystem()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( "CTC (E/A-Adressen 14h-17h)" );
    this.pio   = new Z80PIO( "PIO (E/A-Adressen 0Ch-0Fh)" );
    cpu.setInterruptSources( this.ctc, this.pio );
    cpu.addTStatesListener( this.ctc );
  }


	/* --- Z80PCListener --- */

  @Override
  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    if( pc == 0xF00F ) {
      this.emuThread.getPrintMngr().putByte( cpu.getRegC() );
      cpu.setRegPC( cpu.doPop() );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void die()
  {
    Z80CPU cpu = emuThread.getZ80CPU();
    if( this.ctc != null ) {
      cpu.removeTStatesListener( this.ctc );
    }
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pcListenerAdded ) {
      cpu.removePCListener( this );
    }
    super.die();
  }


  @Override
  public long getDelayMillisAfterPasteChar()
  {
    return 50;
  }


  @Override
  public long getDelayMillisAfterPasteEnter()
  {
    return 300;
  }


  @Override
  public long getHoldMillisPasteChar()
  {
    return 50;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 8;
	break;

      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_TAB:
	ch = 9;
	break;

      case KeyEvent.VK_DOWN:
	ch = 0x0A;
	break;

      case KeyEvent.VK_UP:
	ch = 0x0B;
	break;

      case KeyEvent.VK_ENTER:
	ch = 0x0D;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;

      case KeyEvent.VK_DELETE:
	ch = 0x7F;
	break;
    }
    if( ch > 0 ) {
      this.keyChar = ch;
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    this.keyChar = 0;
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.keyChar = ch;
      rv           = true;
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    port &= 0xFF;

    int rv = 0;
    if( (port >= 0x08) && (port <= 0x0B) ) {
      // Einfuegen aus der Zwischenablage
      if( this.pasteStateNum > 0 ) {
	if( this.pasteStateNum == 4 ) {
	  this.keyChar = 0;
	}
	--this.pasteStateNum;
      } else {
	CharacterIterator iter = this.pasteIter;
	if( iter != null ) {
	  char ch = iter.current();
	  iter.next();
	  if( ch == CharacterIterator.DONE ) {
	    cancelPastingText();
	  } else {
	    ch = TextUtil.toISO646DE( ch );
	    if( ch == '\n' ) {
	      ch = '\r';
	    }
	    if( (ch == '\r') || ((ch >= '\u0000') && (ch < '\u007F')) ) {
	      this.keyChar = ch;
	      this.pasteStateNum = 8;
	    } else {
	      cancelPastingText();
	      fireShowCharNotPasted( iter );
	    }
	  }
	}
      }
    }
    switch( port ) {
      case 0x08:
      case 0x0A:
	rv = this.keyChar;
	break;

      case 0x09:
      case 0x0B:
	if( this.keyChar != 0 ) {
	  rv = 0xFF;
	}
	break;

      case 0x0C:
	rv = this.pio.readDataA();
	break;

      case 0x0E:
	rv = this.pio.readDataB();
	break;

      case 0x14:
      case 0x15:
      case 0x16:
      case 0x17:
	rv = this.ctc.read( port & 0x03, tStates );
	break;
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );
    this.keyChar       = 0;
    this.pasteStateNum = 0;
    if( this.ctc != null ) {
      this.ctc.reset( powerOn );
    }
    if( this.pio != null ) {
      this.pio.reset( powerOn );
    }
  }


  @Override
  public synchronized void startPastingText( String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	cancelPastingText();
	informPastingTextStatusChanged( true );
	this.pasteIter = new StringCharacterIterator( text );
	done           = true;
      }
    }
    if( !done ) {
      informPastingTextStatusChanged( false );
    }
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPrinter()
  {
    return this.pcListenerAdded;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    switch( port & 0xFF ) {
      case 0x0C:
	this.pio.writeDataA( value );
	break;

      case 0x0D:
	this.pio.writeControlA( value );
	break;

      case 0x0E:
	this.pio.writeDataB( value );
	break;

      case 0x0F:
	this.pio.writeControlB( value );
	break;

      case 0x14:
      case 0x15:
      case 0x16:
      case 0x17:
	this.ctc.write( port & 0x03, value, tStates );
	break;
    }
  }
}
