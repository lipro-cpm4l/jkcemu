/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des PC/M (Mugler/Mathes-PC)
 */

package jkcemu.system;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import javax.swing.JOptionPane;
import jkcemu.base.*;
import z80emu.*;


public class PCM extends EmuSys implements Z80CTCListener
{
  private static byte[] bdos         = null;
  private static byte[] rom_1rf      = null;
  private static byte[] pcmFontBytes = null;

  private byte[]  ramVideo;
  private Z80CTC  ctc;
  private Z80PIO  pio;
  private boolean audioOutPhase;
  private boolean keyboardUsed;
  private boolean romEnabled;
  private boolean upperBank0Enabled;
  private int     ramBank;
  private int     nmiCounter;


  public PCM( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );

    rom_1rf            = readResource( "/rom/pcm/pcm_1rf.bin" );
    pcmFontBytes       = readResource( "/rom/pcm/pcmfont.bin" );
    this.ramVideo      = new byte[ 0x0800 ];
    this.audioOutPhase = false;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.ctc, this.pio );

    this.ctc.addCTCListener( this );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( (ctc == this.ctc) && (timerNum == 2) ) {
      if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
	this.audioOutPhase = !this.audioOutPhase;
	this.emuThread.writeAudioPhase( this.audioOutPhase );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0xC800;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv        = BLACK;
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      fontBytes = pcmFontBytes;
    }
    if( fontBytes != null ) {
      int rPix = y % 16;
      int row  = y / 16;
      int col  = x / 8;
      int offs = 0x0400;
      if( rPix >= 8 ) {
	offs = 0;		// Zwischenzeile
	rPix -= 8;
      }
      int mIdx = offs + (row * 64) + col;
      if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	int fIdx = (((int) this.ramVideo[ mIdx ] & 0xFF) * 8) + rPix;
	if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  if( (fontBytes[ fIdx ] & m) != 0 ) {
	    rv = WHITE;
	  }
	}
      }
    }
    return rv;
  }


  public int getCharColCount()
  {
    return 64;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return 32;
  }


  public int getCharRowHeight()
  {
    return 8;
  }


  public int getCharWidth()
  {
    return 8;
  }


  public String getHelpPage()
  {
    return "/help/pcm.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr < 0x2000) && this.romEnabled ) {
      if( rom_1rf != null ) {
	if( addr < rom_1rf.length ) {
	  rv = (int) rom_1rf[ addr ] & 0xFF;
	}
      }
    } else if( addr >= 0xF800 ) {
      int idx = addr - 0xF800;
      if( idx < this.ramVideo.length ) {
	rv = (int) this.ramVideo[ idx ] & 0xFF;
      }
    } else {
      if( (this.ramBank == 0)
	  || ((addr >= 0xC00) && this.upperBank0Enabled) )
      {
	rv = this.emuThread.getRAMByte( addr );
      }
      else if( this.ramBank == 1 ) {
	rv = this.emuThread.getRAMFloppy1().getByte( addr );
      }
      else if( this.ramBank == 2 ) {
	rv = this.emuThread.getRAMFloppy1().getByte( addr + 0xF800 );
      }
    }
    return rv;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = -1;
    if( (chY & 0x01) == 0 ) {
      chY /= 2;
      idx = 0x0400 + (chY * 64) + chX;
    } else {
      chY /= 2;
      idx = (chY * 64) + chX;
    }
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ];
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
    }
    return ch;
  }


  public int getScreenHeight()
  {
    return 256;
  }


  public int getScreenWidth()
  {
    return 512;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getTitle()
  {
    return "PC/M (Mugler/Mathes-PC)";
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    return ((addr < 0x2000) && this.romEnabled)
		|| ((addr >= 0x2000) && (addr < 0xC000) && (this.ramBank == 0))
		|| ((addr >= 0xC000) && (addr < 0xF800)
			&& ((this.ramBank == 0) || this.upperBank0Enabled))
		|| (addr >= 0xF800);
  }


  public boolean isISO646DE()
  {
    return false;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 8;
	break;

      case KeyEvent.VK_RIGHT:
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
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  public void keyReleased()
  {
    this.pio.putInValuePortA( 0, 0xFF );
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  public boolean pasteChar( char ch )
  {
    boolean rv = false;
    if( ch == '\n' ) {
      ch = '\r';
    }
    if( (ch > 0) && (ch < 0x7F) ) {
      if( Character.isUpperCase( ch ) ) {
	ch = Character.toLowerCase( ch );
      } else if( Character.isLowerCase( ch ) ) {
	ch = Character.toUpperCase( ch );
      }
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      try {
	Thread.sleep( 100 );
      }
      catch( InterruptedException ex ) {}
      this.pio.putInValuePortA( 0, 0xFF );
      rv = true;
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	rv = this.ctc.read( port & 0x03 );
	break;

      case 0x84:
	if( !this.keyboardUsed ) {
	  this.pio.putInValuePortA( 0, 0xFF );
	  this.keyboardUsed = true;
	}
	rv = this.pio.readPortA();
	break;

      case 0x85:
	this.pio.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x80 : 0, 0x80 );
	rv = this.pio.readPortB();
	break;

      case 0x86:
	rv = this.pio.readControlA();
	break;

      case 0x87:
	rv = this.pio.readControlB();
	break;
    }
    return rv;
  }


  public int readMemByte( int addr, boolean m1 )
  {
    if( m1 && (this.nmiCounter > 0) ) {
      --this.nmiCounter;
      if( this.nmiCounter == 0 ) {
	this.emuThread.getZ80CPU().fireNMI();
      }
    }
    return getMemByte( addr, m1 );
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System
   * oder das Monitorprogramm aendert
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "PCM" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramVideo );
    }
    if( EmuUtil.getBooleanProperty(
			props,
			"jkcemu.pcm.auto_load_bdos",
			true ) )
    {
      if( bdos == null ) {
	bdos = readResource( "/rom/pcm/bdos.bin" );
      }
      if( bdos != null ) {
	int addr = 0xD000;
	for( int i = 0; (addr < 0x10000) && (i < bdos.length); i++ ) {
	  this.emuThread.setRAMByte( addr++, bdos[ i ] );
	}
      }
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
    }
    this.keyboardUsed      = false;
    this.romEnabled        = true;
    this.upperBank0Enabled = true;
    this.ramBank           = 0;
    this.nmiCounter        = 0;
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    if( addr >= 0xF800 ) {
      int idx = addr - 0xF800;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
    } else {
      if( (addr >= 0x2000) || !this.romEnabled ) {
	if( (this.ramBank == 0)
	    || ((addr >= 0xC00) && this.upperBank0Enabled) )
	{
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
	else if( this.ramBank == 1 ) {
	  this.emuThread.getRAMFloppy1().setByte( addr, value );
	  rv = true;
	}
	else if( this.ramBank == 2 ) {
	  this.emuThread.getRAMFloppy1().setByte( addr + 0xF800, value );
	  rv = true;
	}
      }
    }
    return rv;
  }


  public boolean supportsRAMFloppy1()
  {
    return true;
  }


  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFF ) {
      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	this.ctc.write( port & 0x03, value );
	break;

      case 0x84:
	this.pio.writePortA( value );
	break;

      case 0x85:
	this.pio.writePortB( value );
	if( !this.emuThread.isLoudspeakerEmulationEnabled() ) {
	  this.audioOutPhase = ((this.pio.fetchOutValuePortB( false ) & 0x40)
									!= 0);
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	}
	break;

      case 0x86:
	this.pio.writeControlA( value );
	break;

      case 0x87:
	this.pio.writeControlB( value );
	break;

      case 0x94:
      case 0x95:
      case 0x96:
      case 0x97:
	this.ramBank           = value & 0x03;
	this.upperBank0Enabled = ((value & 0x40) != 0);
	this.romEnabled        = ((value & 0x80) == 0);
	break;

      case 0x98:
      case 0x99:
      case 0x9A:
      case 0x9B:
	this.nmiCounter = 3;
	break;
    }
  }
}

