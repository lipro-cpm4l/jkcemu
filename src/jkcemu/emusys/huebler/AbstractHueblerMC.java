/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse zur Emulation eines Huebler-Computers
 */

package jkcemu.emusys.huebler;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.text.*;
import java.util.Properties;
import jkcemu.base.*;
import jkcemu.text.TextUtil;
import z80emu.*;


public abstract class AbstractHueblerMC
				extends EmuSys
				implements Z80PCListener
{
  protected boolean pcListenerAdded;
  protected int     keyChar;
  protected Z80CTC  ctc;
  protected Z80PIO  pio;

  private int              pasteStateNum;
  private volatile boolean pasteIgnoreMsgActive;


  public AbstractHueblerMC(
			EmuThread  emuThread,
			Properties props,
			String     propPrefix )
  {
    super( emuThread, props, propPrefix );
    this.pcListenerAdded      = false;
    this.pasteIgnoreMsgActive = false;
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
	if( !this.pasteIgnoreMsgActive ) {
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
	      if( (ch == '\r')
		  || ((ch >= '\u0000') && (ch < '\u007F')) )
	      {
		this.keyChar = ch;
		this.pasteStateNum = 8;
	      } else {
		final char ch1            = ch;
		this.pasteIgnoreMsgActive = true;
		EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    showPasteIgnoreMsg( ch1 );
			  }
			} );
	      }
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

      case 0x0D:
	rv = this.pio.readControlA();
	break;

      case 0x0E:
	rv = this.pio.readDataB();
	break;

      case 0x0F:
	rv = this.pio.readControlB();
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
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    this.keyChar       = 0;
    this.pasteStateNum = 0;
  }


  @Override
  public synchronized void startPastingText( String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	cancelPastingText();
	this.pasteIter = new StringCharacterIterator( text );
	done = true;
      }
    }
    if( !done ) {
      this.screenFrm.firePastingTextFinished();
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


	/* --- private Methoden --- */

  private void showPasteIgnoreMsg( char ch )
  {
    if( BasicDlg.showOptionDlg(
		this.emuThread.getScreenFrm(),
		String.format(
			"Das Zeichen mit dem Code %02Xh kann nicht"
				+ " eingef\u00FCgt werden.",
			(int) ch ),
		"Einf\u00FCgen",
		"Weiter",
		"Abbrechen" ) != 0 )
    {
      cancelPastingText();
    }
    this.pasteIgnoreMsgActive = false;
  }
}
