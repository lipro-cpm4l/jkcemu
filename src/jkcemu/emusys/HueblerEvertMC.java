/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Huebler/Evert-MC
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import jkcemu.emusys.huebler.AbstractHueblerMC;
import z80emu.*;


public class HueblerEvertMC extends AbstractHueblerMC
{
  private static final String[] sysCallNames = {
			"BEGIN", "CI",    "RI",   "COE",
			"POE",   "LOE",   "CSTS", "CRI",
			"CPOE",  "MEMSI", "MAIN", "EXT" };

  private static byte[] hemcFontBytes = null;
  private static byte[] monBytes      = null;

  private byte[] fontBytes;
  private byte[] ramVideo;
  private byte[] ramStatic;
  private Z80PIO pio2;


  public HueblerEvertMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.fontBytes = readFontByProperty(
				props,
				"jkcemu.hemc.font.file",
				0x0800 );
    if( this.fontBytes == null ) {
      if( hemcFontBytes == null ) {
	hemcFontBytes = readResource( "/rom/huebler/hemcfont.bin" );
      }
      this.fontBytes = hemcFontBytes;
    }
    if( monBytes == null ) {
      monBytes = readResource( "/rom/huebler/mon21.bin" );
    }
    this.ramVideo  = new byte[ 0x0800 ];
    this.ramStatic = new byte[ 0x0400 ];
    this.pio2      = new Z80PIO( "PIO (IO-Adressen 10-13)" );
    createIOSystem();
    this.emuThread.getZ80CPU().setInterruptSources(
					this.ctc,
					this.pio,
					this.pio2 );
    checkAddPCListener( props );
    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canApplySettings( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( "HueblerEvertMC" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0xFEFE;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( this.fontBytes != null ) {
      int rPix = y % 10;
      int row  = y / 10;
      if( (rPix < 8) && (row >= 0) && (row < 24) ) {
	int col  = x / 6;
	int mIdx = (row * 64) + col;
	if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	  byte b    = this.ramVideo[ mIdx ];
	  int  fIdx = (((int) b & 0x7F) * 8) + rPix;
	  if( (fIdx >= 0) && (fIdx < this.fontBytes.length ) ) {
	    int m = 0x80;
	    int n = x % 6;
	    if( n > 0 ) {
	      m >>= n;
	    }
	    if( (this.fontBytes[ fIdx ] & m) != 0 ) {
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


  @Override
  public int getCharColCount()
  {
    return 64;
  }


  @Override
  public int getCharHeight()
  {
    return 8;
  }


  @Override
  public int getCharRowCount()
  {
    return 24;
  }


  @Override
  public int getCharRowHeight()
  {
    return 10;
  }


  @Override
  public int getCharWidth()
  {
    return 6;
  }


  @Override
  protected boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/hemc.htm";
  }


  @Override
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


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0xF000;
  }


  @Override
  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * 64) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      if( this.fontBytes == hemcFontBytes ) {
	// integrierter Zeichensatz
	int b = (int) this.ramVideo[ idx ] & 0x7F;	// B7=1: invertiert
	switch( b ) {
	  case 3:			// Gleichheitszeichen
	    ch = '=';
	    break;

	  case 5:			// kleines Alpha
	    ch = '\u03B1';
	    break;

	  case 6:			// kleines Beta
	    ch = '\u03B2';
	    break;

	  case 7:			// kleines Delta
	    ch = '\u03B4';
	    break;

	  case 9:
	    ch = '\u03B7';
	    break;

	  case 0x0A:
	    ch = '\u03B8';
	    break;

	  case 0x0B:
	    ch = '\u03BB';
	    break;

	  case 0x0C:		// kleines Mikro
	    ch = '\u03BC';
	    break;

	  case 0x0E:
	    ch = '\u03C3';
	    break;

	  case 0x0F:		// Summenzeichen
	    ch = '\u03A3';
	    break;

	  case 0x11:
	    ch = '\u03C6';
	    break;

	  case 0x12:		// Omikron
	    ch = '\u03A9';
	    break;

	  case 0x15:		// Ae-Umlaut
	    ch = '\u00C4';
	    break;

	  case 0x16:		// ee-Umlaut
	    ch = '\u00E4';
	    break;

	  case 0x17:		// Oe-Umlaut
	    ch = '\u00D6';
	    break;

	  case 0x18:		// oe-Umlaut
	    ch = '\u00F6';
	    break;

	  case 0x19:		// Ue-Umlaut
	    ch = '\u00DC';
	    break;

	  case 0x1A:		// ue-Umlaut
	    ch = '\u00FC';
	    break;

	  case 0x1D:		// hochgestellte 2
	    ch = '\u00B2';
	    break;

	  case 0x1E:		// Pfund-Zeichen
	    ch = '\u00A3';
	    break;

	  case 0x1F:		// Y mit Querstrich
	    ch = '\u00A5';
	    break;

	  default:
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      ch = b;
	    }
	    break;
	}
      } else {
	int b = (int) this.ramVideo[ idx ] & 0xFF;
	if( (b >= 0x20) && (b < 0x7F) ) {
	  ch = b;
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return 238;
  }


  @Override
  public int getScreenWidth()
  {
    return 384;
  }


  @Override
  public String getTitle()
  {
    return "H\u00FCbler/Evert-MC";
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
      keyTyped( (char) ch );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( ch > 0 ) {
      switch( ch ) {
	case '\u00DF':		// Sz
	  this.keyChar = 6;
	  break;

	case '\u00C4':		// Ae-Umlaut
	  this.keyChar = 0x15;
	  break;

	case '\u00E4':		// ae-Umlaut
	  this.keyChar = 0x16;
	  break;

	case '\u00D6':		// Oe-Umlaut
	  this.keyChar = 0x17;
	  break;

	case '\u00F6':		// oe-Umlaut
	  ch = '\u00F6';
	  this.keyChar = 0x18;
	  break;

	case '\u00DC':		// Ue-Umlaut
	  this.keyChar = 0x19;
	  break;

	case '\u00FC':		// ue-Umlaut
	  this.keyChar = 0x1A;
	  break;

	case '\u00B2':		// hochgestellte 2
	  this.keyChar = 0x1D;
	  break;

	case '\u00A3':		// Pfund-Zeichen
	  this.keyChar = 0x1E;
	  break;

	case '\u00A5':		// Y mit Querstrich
	  this.keyChar = 0x1F;
	  break;

	default:
	  this.keyChar = ch;
      }
      rv = true;
    }
    return rv;
  }


  @Override
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


  @Override
  public int reassembleSysCall(
                        int           addr,
                        StringBuilder buf,
			boolean       sourceOnly,
                        int           colMnemonic,
                        int           colArgs,
                        int           colRemark )
  {
    return reassSysCallTable(
			addr,
			0xF000,
			sysCallNames,
			buf,
			sourceOnly,
			colMnemonic,
			colArgs,
			colRemark );
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ramStatic, props );
      fillRandom( this.ramVideo );
    }
    this.keyChar = 0;
  }


  @Override
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


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != hemcFontBytes;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  @Override
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

