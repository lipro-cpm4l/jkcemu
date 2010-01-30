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


public class HueblerEvertMC extends AbstractHueblerMC
{
  private static final String[] sysCallNames = {
			"BEGIN", "CI",    "RI",   "COE",
			"POE",   "LOE",   "CSTS", "CRI",
			"CPOE",  "MEMSI", "MAIN", "EXT" };

  private static byte[] fontBytes = null;
  private static byte[] monBytes  = null;

  private byte[] ramVideo;
  private byte[] ramStatic;
  private Z80PIO pio2;


  public HueblerEvertMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    fontBytes      = readResource( "/rom/huebler/hemcfont.bin" );
    monBytes       = readResource( "/rom/huebler/mon21.bin" );
    this.ramVideo  = new byte[ 0x0800 ];
    this.ramStatic = new byte[ 0x0400 ];
    this.pio2      = new Z80PIO( emuThread.getZ80CPU() );
    createIOSystem();
    checkAddPCListener( props );
    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- ueberschriebene Methoden --- */

  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
  }


  public boolean canExtractScreenText()
  {
    return true;
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
    return 24;
  }


  public int getCharRowHeight()
  {
    return 10;
  }


  public int getCharWidth()
  {
    return 6;
  }


  public String getHelpPage()
  {
    return "/help/hemc.htm";
  }


  public int getMemByte( int addr, boolean m1 )
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


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * 64) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
    }
    return ch;
  }


  public int getScreenHeight()
  {
    return 238;
  }


  public int getScreenWidth()
  {
    return 384;
  }


  public String getTitle()
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


  public int readIOByte( int port )
  {
    port &= 0x3F;

    int rv = 0;
    switch( port ) {
      case 0x10:
        rv = this.pio2.readPortA();
        break;

      case 0x11:
        rv = this.pio2.readControlA();
        break;

      case 0x12:
        rv = this.pio2.readPortB();
        break;

      case 0x13:
        rv = this.pio2.readControlB();
        break;

      default:
	rv = super.readIOByte( port );
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


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ramStatic, props );
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


  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  public void writeIOByte( int port, int value )
  {
    port &= 0x3F;
    switch( port ) {
      case 0x10:
	this.pio2.writePortA( value );
	break;

      case 0x11:
	this.pio2.writeControlA( value );
	break;

      case 0x12:
	this.pio2.writePortB( value );
	break;

      case 0x13:
	this.pio2.writeControlB( value );
	break;

      default:
	super.writeIOByte( port, value );
    }
  }


	/* --- private Methoden --- */

  private void checkAddPCListener( Properties props )
  {
    checkAddPCListener( props, "jkcemu.hemc.catch_print_calls" );
  }
}

