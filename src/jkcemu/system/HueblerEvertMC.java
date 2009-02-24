/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Huebler/Evert-MC
 */

package jkcemu.system;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import z80emu.*;


public class HueblerEvertMC extends EmuSys
{
  private static final String[] sysCallNames = {
			"BEGIN", "CI",    "RI",   "COE",
			"POE",   "LOE",   "CSTS", "CRI",
			"CPOE",  "MEMSI", "MAIN", "EXT" };

  private static byte[] fontBytes = null;
  private static byte[] monBytes  = null;

  private byte[] ramVideo;
  private byte[] ramStatic;
  private int    keyChar;
  private Z80CTC ctc;


  public HueblerEvertMC( EmuThread emuThread )
  {
    super( emuThread );
    fontBytes      = readResource( "/rom/huebler/hemcfont.bin" );
    monBytes       = readResource( "/rom/huebler/mon21.bin" );
    this.ramVideo  = new byte[ 0x0800 ];
    this.ramStatic = new byte[ 0x0400 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    cpu.setInterruptSources( this.ctc );
    cpu.addTStatesListener( this.ctc );

    reset( EmuThread.ResetLevel.POWER_ON );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    Z80CPU cpu = emuThread.getZ80CPU();
    cpu.removeTStatesListener( this.ctc );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public String extractScreenText()
  {
    return EmuUtil.extractText( this.ramVideo, 0, 24, 64, 24 );
  }


  public int getAppStartStackInitValue()
  {
    return 0xFEFE;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv     = BLACK;
    byte[] fBytes = this.emuThread.getExtFontBytes();
    if( fBytes == null ) {
      fBytes = fontBytes;
    }
    if( fBytes != null ) {
      int rPix = y % 10;
      int row  = y / 10;
      if( (rPix < 8) && (row >= 0) && (row < 24) ) {
	int col  = x / 6;
	int mIdx = (row * 64) + col;
	if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	  byte b    = this.ramVideo[ mIdx ];
	  int  fIdx = (((int) b & 0x7F) * 8) + rPix;
	  if( (fIdx >= 0) && (fIdx < fBytes.length ) ) {
	    int m = 0x80;
	    int n = x % 6;
	    if( n > 0 ) {
	      m >>= n;
	    }
	    if( (fBytes[ fIdx ] & m) != 0 ) {
	      if( (b & 0x80) == 0 ) {
		rv = WHITE;
	      }
	    } else {
	      if( (b & 0x80) != 0 ) {
		rv = WHITE;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  public int getMinOSAddress()
  {
    return 0xF000;
  }


  public int getMaxOSAddress()
  {
    return 0xFBFF;
  }


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( addr < 0xE800 ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    else if( (addr >= 0xE800) && (addr < 0xF000) ) {
      int idx = addr - 0xE800;
      if( idx < this.ramVideo.length ) {
	rv = (int) this.ramVideo[ idx ] & 0xFF;
      }
    }
    else if( (addr >= 0xF000) && (addr < 0xFC00) ) {
      int idx = addr - 0xF000;
      if( idx < monBytes.length ) {
	rv = (int) monBytes[ idx ] & 0xFF;
      }
    }
    else if( addr >= 0xFC00 ) {
      int idx = addr - 0xFC00;
      if( idx < this.ramStatic.length ) {
	rv = (int) this.ramStatic[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0xF000;
  }


  public int getScreenBaseHeight()
  {
    return 238;
  }


  public int getScreenBaseWidth()
  {
    return 384;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getSystemName()
  {
    return "H\u00FCbler/Evert-MC";
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
      this.keyChar = ch;
      rv = true;
    }
    return rv;
  }


  public void keyReleased()
  {
    this.keyChar = 0;
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.keyChar = ch;
      rv           = true;
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int rv = 0;
    switch( port & 0x3C ) {
      case 0x08:
	if( this.keyChar != 0 ) {
	  if( (port & 0x01) != 0 ) {
	    rv = 0xFF;
	  } else {
	    rv = this.keyChar;
	  }
	}
	break;

      case 0x14:
	rv = this.ctc.read( port & 0x03 );
	break;
    }
    return rv;
  }


  public int reassembleSysCall(
                        int           addr,
                        StringBuilder buf,
                        int           colMnemonic,
                        int           colArgs,
                        int           colRemark )
  {
    return reassSysCallTable(
			addr,
			0xF000,
			sysCallNames,
			buf,
			colMnemonic,
			colArgs,
			colRemark );
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "HueblerEvertMC" );
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramStatic );
      fillRandom( this.ramVideo );
    }
    this.keyChar = 0;
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( addr < 0xE800 ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    else if( (addr >= 0xE800) && (addr < 0xF000) ) {
      int idx = addr - 0xE800;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
    }
    else if( addr >= 0xFC00 ) {
      int idx = addr - 0xFC00;
      if( idx < this.ramStatic.length ) {
	this.ramStatic[ idx ] = (byte) value;
	rv = true;
      }
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0x3C) == 0x14 )
      this.ctc.write( port & 0x03, value );
  }
}

