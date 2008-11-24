/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des AC1
 */

package jkcemu.ac1;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import z80emu.*;


public class AC1 extends EmuSys
{
  public static final int MEM_BASIC  = 0x0800;
  public static final int MEM_SCREEN = 0x1000;

  private static byte[] mon31     = null;
  private static byte[] minibasic = null;
  private static byte[] fontBytes = null;
  private static byte[] ramVideo  = null;

  private Z80CTC  ctc;
  private Z80PIO  pio;
  private boolean keyboardUsed;


  public AC1( EmuThread emuThread )
  {
    super( emuThread );
    if( mon31 == null ) {
      mon31 = readResource( "/rom/ac1/mon_31.bin" );
    }
    if( minibasic == null ) {
      minibasic = readResource( "/rom/ac1/minibasic.bin" );
    }
    if( fontBytes == null ) {
      fontBytes = readResource( "/rom/ac1/ac1font.bin" );
    }
    if( ramVideo == null ) {
      ramVideo = new byte[ 0x0800 ];
      fillRandom( ramVideo );
    }
    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu, null );
    this.pio   = new Z80PIO( cpu, this.ctc );
    cpu.addTStatesListener( this.ctc );
    this.keyboardUsed = false;
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    this.emuThread.getZ80CPU().removeTStatesListener( this.ctc );
  }


  public String extractScreenText()
  {
    String rv = null;
    if( ramVideo != null ) {
      StringBuilder buf = new StringBuilder( 65 * 32 );
      for( int i = 0; i < 32; i++ ) {
	int rowIdx  = 0x07FF - (i * 64);
	int nSpaces = 0;
	for( int k = 0; k < 64; k++ ) {
	  int a = rowIdx - k;
	  int b = 20;
	  if( (a >= 0) && (a < ramVideo.length) ) {
	    b = (int) ramVideo[ a ] & 0xFF;
	  }
	  if( b == 0x20 ) {
	    nSpaces++;
	  }
	  else if( (b > 0x20) && (b < 0x7F) ) {
	    while( nSpaces > 0 ) {
	      buf.append( (char) '\u0020' );
	      --nSpaces;
	    }
	    buf.append( (char) b );
	  }
	}
	buf.append( (char) '\n' );
      }
      rv = buf.toString();
    }
    return rv;
  }


  public int getAppStartStackInitValue()
  {
    return 0x2000;
  }


  public int getColorIndex( int x, int y )
  {
    int rv = 0;
    if( fontBytes != null ) {
      int row = y / 8;
      int col = x / 8;
      int idx = (this.emuThread.getMemByte(
			MEM_SCREEN + fontBytes.length - 1 - (row * 64) - col )
				* 8) + (y % 8);
      if( (idx >= 0) && (idx < fontBytes.length ) ) {
	int m = 1;
	int n = x % 8;
	if( n > 0 ) {
	  m <<= n;
	}
	if( (fontBytes[ idx ] & m) != 0 ) {
	  rv = 1;
	}
      }
    }
    return rv;
  }


  public int getDefaultStartAddress()
  {
    return 0x2000;
  }


  public int getMinOSAddress()
  {
    return 0;
  }


  public int getMaxOSAddress()
  {
    return mon31 != null ? (mon31.length - 1) : 0;
  }


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( mon31 != null ) {
      if( addr < mon31.length ) {
	rv   = (int) mon31[ addr ] & 0xFF;
	done = true;
      }
    }
    if( !done && (minibasic != null) ) {
      if( (addr >= MEM_BASIC) && (addr < MEM_BASIC + minibasic.length) ) {
	rv   = (int) minibasic[ addr - MEM_BASIC ] & 0xFF;
	done = true;
      }
    }
    if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + ramVideo.length) ) {
      rv   = (int) ramVideo[ addr - MEM_SCREEN ] & 0xFF;
      done = true;
    }
    if( !done ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getResetStartAddress()
  {
    return 0;
  }


  public int getScreenBaseHeight()
  {
    return 256;
  }


  public int getScreenBaseWidth()
  {
    return 512;
  }


  public String getSystemName()
  {
    return "AC1";
  }


  public boolean keyEvent( KeyEvent e )
  {
    boolean rv = false;
    int     ch = 0;
    switch( e.getID() ) {
      case KeyEvent.KEY_PRESSED:
	switch( e.getKeyCode() ) {
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
	    ch = 0x5F;
	    break;
	}
	if( ch > 0 ) {
	  this.pio.putInValuePortA( ch | 0x80, 0xFF );
	  rv = true;
        }
        break;

      case KeyEvent.KEY_RELEASED:
	this.pio.putInValuePortA( 0, 0xFF );
	rv = true;
        break;

      case KeyEvent.KEY_TYPED:
	ch = e.getKeyChar();
	if( (ch > 0) && (ch < 0x7F) ) {
	  if( (ch >= 'A') && (ch <= 'Z') ) {
	    ch = Character.toLowerCase( (char) ch );
	  }
	  else if( (ch >= 'a') && (ch <= 'z') ) {
	    ch = Character.toUpperCase( (char) ch );
	  }
	  this.pio.putInValuePortA( ch | 0x80, 0xFF );
	  rv = true;
	}
        break;
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0:
      case 1:
      case 2:
      case 3:
	rv = this.ctc.read( port & 0x03 );
	break;

      case 4:
	if( !this.keyboardUsed ) {
	  this.pio.putInValuePortA( 0, 0xFF );
	  this.keyboardUsed = true;
	}
	rv = this.pio.readPortA();
	break;

      case 5:
	rv = (this.emuThread.readAudioPhase() ? 0x80 : 0)
				| (this.pio.readPortB() & 0x7F);
	break;

      case 6:
	rv = this.pio.readControlA();
	break;

      case 7:
	rv = this.pio.readControlB();
	break;
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "AC1" );
  }


  public void reset( boolean powerOn )
  {
    this.ctc.reset( powerOn );
    this.pio.reset( powerOn );
    this.keyboardUsed = false;
    if( powerOn )
      fillRandom( ramVideo );
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( this.mon31 != null ) {
      if( addr < mon31.length )
	done = true;
    }
    if( !done ) {
      if( this.minibasic != null ) {
	if( addr < minibasic.length )
	  done = true;
      }
    }
    if( !done ) {
      if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + ramVideo.length) ) {
        ramVideo[ addr - MEM_SCREEN ] = (byte) value;
        this.screenFrm.setScreenDirty( true );
        rv = true;
      } else {
        this.emuThread.setRAMByte( addr, value );
        rv = true;
      }
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFF ) {
      case 0:
      case 1:
      case 2:
      case 3:
	this.ctc.write( port & 0x03, value );
	break;

      case 4:
	this.pio.writePortA( value );
	break;

      case 5:
	this.pio.writePortB( value );
	this.emuThread.updAudioOutPhase(
		(this.pio.fetchOutValuePortB( false ) & 0x40) != 0 );
	break;

      case 6:
	this.pio.writeControlA( value );
	break;

      case 7:
	this.pio.writeControlB( value );
	break;
    }
  }
}

