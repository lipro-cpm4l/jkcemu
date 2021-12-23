/*
 * (c) 2009-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Huebler/Evert-MC
 */

package jkcemu.emusys;

import java.util.Properties;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.huebler.AbstractHueblerMC;
import z80emu.Z80MemView;
import z80emu.Z80PIO;


public class HueblerEvertMC extends AbstractHueblerMC
{
  public static final String SYSNAME     = "HueblerEvertMC";
  public static final String SYSTEXT     = "H\u00FCbler/Evert-MC";
  public static final String PROP_PREFIX = "jkcemu.hemc.";

  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 4000;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = true;

  private static final String[] sysCallNames = {
			"BEGIN", "CI",    "RI",   "COE",
			"POE",   "LOE",   "CSTS", "CRI",
			"CPOE",  "MEMSI", "MAIN", "EXT" };

  private static final int[] charToUnicode = {
			'\u25C0', '\u2016',       -1,      '=',
			'\u00F1', '\u03B1', '\u03B2', '\u03B4',
			'\u2302', '\u03B7', '\u03B8', '\u03BB',
			'\u03BC',       -1, '\u03C3', '\u03A3',
			      -1, '\u03C6', '\u03A9', '\u00C5',
			'\u00E5', '\u00C4', '\u00E4', '\u00D6',
			'\u00F6', '\u00DC', '\u00FC', '\u2192',
			'\u221A', '\u00B2', '\u00A3', '\u00A5' };

  private static byte[] hemcFont = null;
  private static byte[] mon21    = null;

  private byte[] fontBytes;
  private byte[] ramVideo;
  private byte[] ramStatic;
  private byte[] osBytes;
  private String osFile;
  private Z80PIO pio2;


  public HueblerEvertMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.osBytes   = null;
    this.osFile    = null;
    this.ramVideo  = new byte[ 0x0800 ];
    this.ramStatic = new byte[ 0x0400 ];
    this.pio2      = new Z80PIO( "PIO (E/A-Adressen 10h-13h)" );
    createIOSystem();
    this.emuThread.getZ80CPU().setInterruptSources(
					this.ctc,
					this.pio,
					this.pio2 );
    checkAddPCListener( props );
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
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
    loadFont( props );
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
  protected boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( 64, 24, 10, 6, 8 );
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
      if( this.osBytes != null ) {
	int idx = addr - 0xF000;
	if( idx < this.osBytes.length ) {
	  rv = (int) this.osBytes[ idx ] & 0xFF;
	}
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
  public int getResetStartAddress( boolean powerOn )
  {
    return 0xF000;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * 64) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      if( this.fontBytes == hemcFont ) {
	// integrierter Zeichensatz
	int b = (int) this.ramVideo[ idx ] & 0x7F;	// B7=1: invertiert
	if( (b >= 0) && (b < charToUnicode.length) ) {
	  ch = charToUnicode[ b ];
	} else if( (b >= 0x20) && (b < 0x7F) ) {
	  ch = b;
	} else if( b == 0x7F ) {
	  ch = '\u2592';
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
    return SYSTEXT;
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
  public void loadROMs( Properties props )
  {
    this.osFile = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_OS_PREFIX + PROP_FILE );
    this.osBytes = readROMFile( this.osFile, 0x0C00, "Monitorprogramm" );
    if( this.osBytes == null ) {
      if( mon21 == null ) {
	mon21 = readResource( "/rom/huebler/mon21.bin" );
      }
      this.osBytes = mon21;
    }
    loadFont( props );
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    port &= 0x3F;

    int rv = 0;
    switch( port ) {
      case 0x10:
        rv = this.pio2.readDataA();
        break;

      case 0x12:
        rv = this.pio2.readDataB();
        break;

      default:
	rv = super.readIOByte( port, tStates );
    }
    return rv;
  }


  @Override
  public int reassembleSysCall(
			Z80MemView    memory,
                        int           addr,
                        StringBuilder buf,
			boolean       sourceOnly,
                        int           colMnemonic,
                        int           colArgs,
                        int           colRemark )
  {
    return reassSysCallTable(
			memory,
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
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );
    if( powerOn ) {
      initDRAM();
      initSRAM( this.ramStatic, props );
      fillRandom( this.ramVideo );
    }
    this.pio2.reset( powerOn );
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
    return this.fontBytes != hemcFont;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    port &= 0x3F;
    switch( port ) {
      case 0x10:
	this.pio2.writeDataA( value );
	break;

      case 0x11:
	this.pio2.writeControlA( value );
	break;

      case 0x12:
	this.pio2.writeDataB( value );
	break;

      case 0x13:
	this.pio2.writeControlB( value );
	break;

      default:
	super.writeIOByte( port, value, tStates );
    }
  }


	/* --- private Methoden --- */

  private void checkAddPCListener( Properties props )
  {
    checkAddPCListener( props, this.propPrefix + PROP_CATCH_PRINT_CALLS );
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
			props,
			this.propPrefix + PROP_FONT_PREFIX + PROP_FILE,
			0x0800 );
    if( this.fontBytes == null ) {
      if( hemcFont == null ) {
	hemcFont = readResource( "/rom/huebler/hemcfont.bin" );
      }
      this.fontBytes = hemcFont;
    }
  }
}
