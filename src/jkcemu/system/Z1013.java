/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z1013
 */

package jkcemu.system;

import java.lang.*;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.system.z1013.*;
import z80emu.*;


public class Z1013 extends EmuSys implements Z80AddressListener
{
  public static final int MEM_HEAD   = 0x00E0;
  public static final int MEM_SCREEN = 0xEC00;
  public static final int MEM_OS     = 0xF000;

  private static final String[] sysCallNames = {
			"OUTCH", "INCH",  "PRST7", "INHEX",
			"INKEY", "INLIN", "OUTHX", "OUTHL",
			"CSAVE", "CLOAD", "MEM",   "WIND",
			"OTHLS", "OUTDP", "OUTSP", "TRANS",
			"INSTR", "KILL",  "HEXUM", "ALPHA" };

  private static byte[] mon202         = null;
  private static byte[] monA2          = null;
  private static byte[] monRB_K7659    = null;
  private static byte[] monRB_S6009    = null;
  private static byte[] monJM_1992     = null;
  private static byte[] z1013FontBytes = null;
  private static byte[] altFontBytes   = null;

  private Z80PIO   pio;
  private Keyboard keyboard;
  private int      ramEndAddr;
  private byte[]   ramVideo;
  private byte[]   romOS;
  private boolean  romDisabled;
  private boolean  altFontEnabled;
  private boolean  mode4MHz;
  private boolean  mode64x16;


  public Z1013( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( z1013FontBytes == null ) {
      z1013FontBytes = readResource( "/rom/z1013/z1013font.bin" );
    }
    if( altFontBytes == null ) {
      altFontBytes = readResource( "/rom/z1013/altfont.bin" );
    }
    String monText = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
    if( monText.equals( "A.2" ) ) {
      if( monA2 == null ) {
	monA2 = readResource( "/rom/z1013/mon_a2.bin" );
      }
      this.romOS = monA2;
    } else if( monText.equals( "RB_K7659" ) ) {
      if( monRB_K7659 == null ) {
	monRB_K7659 = readResource( "/rom/z1013/mon_rb_k7659.bin" );
      }
      this.romOS = monRB_K7659;
    } else if( monText.equals( "RB_S6009" ) ) {
      if( monRB_S6009 == null ) {
	monRB_S6009 = readResource( "/rom/z1013/mon_rb_s6009.bin" );
      }
      this.romOS = monRB_S6009;
    } else if( monText.equals( "JM_1992" ) ) {
      if( monJM_1992 == null ) {
	monJM_1992 = readResource( "/rom/z1013/mon_jm_1992.bin" );
      }
      this.romOS = monJM_1992;
    } else {
      if( mon202 == null ) {
	mon202 = readResource( "/rom/z1013/mon_202.bin" );
      }
      this.romOS = mon202;
    }
    this.romDisabled    = false;
    this.altFontEnabled = false;
    this.mode4MHz       = false;
    this.mode64x16      = false;
    this.ramEndAddr     = getRAMEndAddr( props );
    this.ramVideo       = new byte[ 0x0400 ];

    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.pio );
    cpu.addAddressListener( this );

    this.keyboard = new Keyboard( this.pio );
    this.keyboard.applySettings( props );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "Z1013.01" ) ?
								1000 : 2000;
  }


  public Keyboard getKeyboard()
  {
    return this.keyboard;
  }


  public static String getTinyBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x1152,
				memory.getMemWord( 0x101F ) + 1 );
  }


	/* --- Z80AddressListener --- */

  public void z80AddressChanged( int addr )
  {
    // Verbindung A0 - PIO B5 emulieren
    this.pio.putInValuePortB( (addr << 5) & 0x20, 0x20 );
  }


	/* --- ueberschriebene Methoden --- */

  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeAddressListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x00B0;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv        = BLACK;
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      if( this.altFontEnabled ) {
	fontBytes = altFontBytes;
      } else {
	fontBytes = z1013FontBytes;
      }
    }
    if( fontBytes != null ) {
      int col = x / 8;
      int idx = -1;
      if( this.mode64x16 ) {
	int rPix = y % 16;
	if( rPix < 8 ) {
	  int row = y / 16;
	  idx = (this.emuThread.getMemByte( 0xEC00 + (row * 64) + col ) * 8)
								+ rPix;
	}
      } else {
	int row = y / 8;
	idx = (this.emuThread.getMemByte( 0xEC00 + (row * 32) + col ) * 8)
								+ (y % 8);
      }
      if( (idx >= 0) && (idx < fontBytes.length ) ) {
	int m = 0x80;
	int n = x % 8;
	if( n > 0 ) {
	  m >>= n;
	}
	if( (fontBytes[ idx ] & m) != 0 ) {
	  rv = WHITE;
	}
      }
    }
    return rv;
  }


  public int getCharColCount()
  {
    return this.mode64x16 ? 64 : 32;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return this.mode64x16 ? 16 : 32;
  }


  public int getCharRowHeight()
  {
    return this.mode64x16 ? 16 : 8;
  }


  public int getCharWidth()
  {
    return 8;
  }


  public String getHelpPage()
  {
    return "/help/z1013.htm";
  }


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( !this.romDisabled && (this.romOS != null) ) {
      if( (addr >= MEM_OS) && (addr < MEM_OS + this.romOS.length) ) {
	rv   = (int) this.romOS[ addr - MEM_OS ] & 0xFF;
	done = true;
      }
    }
    if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + this.ramVideo.length) ) {
      rv   = (int) this.ramVideo[ addr - MEM_SCREEN ] & 0xFF;
      done = true;
    }
    if( !done && (addr <= this.ramEndAddr) ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return MEM_OS;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * (this.mode64x16 ? 64 : 32)) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
      else if( (ch >= 0xA0) && (ch < 0xFF) && this.altFontEnabled ) {
	ch = b & 0x7F; 		// 0xA0 bis 0xFE: invertierte Zeichen
      }
    }
    return ch;
  }


  public int getScreenHeight()
  {
    return this.mode64x16 ? 248 : 256;
  }


  public int getScreenWidth()
  {
    return this.mode64x16 ? 512 : 256;
  }


  public String getTitle()
  {
    return "Z1013";
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    return !this.romDisabled || (addr < MEM_OS);
  }


  public boolean isISO646DE()
  {
    return this.altFontEnabled;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    return this.keyboard.setKeyCode( keyCode );
  }


  public void keyReleased()
  {
    this.keyboard.setKeyReleased();
  }


  public boolean keyTyped( char ch )
  {
    return this.keyboard.setKeyChar( ch );
  }


  public void openBasicProgram()
  {
    int begAddr = askKCBasicBegAddr();
    if( begAddr >= 0 )
      SourceUtil.openKCBasicProgram( this.screenFrm, begAddr );
  }


  public void openTinyBasicProgram()
  {
    String text = getTinyBasicProgram( this.emuThread );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoTinyBasic();
    }
  }


  public int readIOByte( int port )
  {
    int value = 0x0F;			// wird von unbelegten Ports gelesen

    if( (port & 0xFF) == 4 ) {
      value = 0;
      if( this.romDisabled ) {
	value |= 0x10;
      }
      if( this.altFontEnabled ) {
	value |= 0x20;
      }
      if( this.mode4MHz ) {
	value |= 0x40;
      }
      if( this.mode64x16 ) {
	value |= 0x80;
      }
    }
    else if( (port & 0xF8) == 0x98 ) {
      value = this.emuThread.getRAMFloppyA().readByte( port & 0x07 );
    }
    else if( (port & 0xF8) == 0x58 ) {
      value = this.emuThread.getRAMFloppyB().readByte( port & 0x07 );
    }
    if( (port & 0x1C) == 0 ) {		// IOSEL0 -> PIO, ab A5 ignorieren
      switch( port & 0x03 ) {
	case 0:
	  value = this.pio.readPortA();
	  break;

	case 1:
	  value = this.pio.readControlA();
	  break;

	case 2:
	  this.pio.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x40 : 0, 0x40 );
	  value = this.pio.readPortB();
	  break;

	case 3:
	  value = this.pio.readControlB();
	  break;
      }
    }
    z80AddressChanged( port );
    return value;
  }


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv  = 0;
    int bol = buf.length();
    int b   = this.emuThread.getMemByte( addr );
    if( b == 0xE7 ) {
      buf.append( String.format( "%04X  E7", addr ) );
      appendSpacesToCol( buf, bol, colMnemonic );
      buf.append( "RST" );
      appendSpacesToCol( buf, bol, colArgs );
      buf.append( "20H\n" );
      rv = 1;
    } else if( b == 0xCD ) {
      if( getMemWord( addr + 1 ) == 0x0020 ) {
	buf.append( String.format( "%04X  CD 00 20", addr ) );
	appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "CALL" );
	appendSpacesToCol( buf, bol, colArgs );
	buf.append( "0020H\n" );
	rv = 3;
      }
    }
    if( rv > 0 ) {
      addr += rv;

      bol = buf.length();
      b   = this.emuThread.getMemByte( addr );
      buf.append( String.format( "%04X  %02X", addr, b ) );
      appendSpacesToCol( buf, bol, colMnemonic );
      buf.append( "DB" );
      appendSpacesToCol( buf, bol, colArgs );
      if( b >= 0xA0 ) {
	buf.append( (char) '0' );
      }
      buf.append( String.format( "%02XH", b ) );
      if( (b >= 0) && (b < sysCallNames.length) ) {
	appendSpacesToCol( buf, bol, colRemark );
	buf.append( (char) ';' );
	buf.append( sysCallNames[ b ] );
      }
      buf.append( (char) '\n' );
      rv++;
      if( b == 2 ) {
	rv += reassStringBit7(
			this.emuThread,
			addr + 1,
			buf,
			colMnemonic,
			colArgs );
      }
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System oder
   * das Monitorprogramm aendert oder wenn der RAM
   * von 64 auf 16 KByte reduziert wird.
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv = !EmuUtil.getProperty(
				props,
				"jkcemu.system" ).startsWith( "Z1013" );
    if( !rv ) {
      String monText = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
      if( monText.equals( "A.2" ) ) {
	rv = (this.romOS != monA2);
      } else if( monText.equals( "RB_K7659" ) ) {
	rv = (this.romOS != monRB_K7659);
      } else if( monText.equals( "RB_S6009" ) ) {
	rv = (this.romOS != monRB_S6009);
      } else if( monText.equals( "JM_1992" ) ) {
	rv = (this.romOS != monJM_1992);
      } else {
	rv = (this.romOS != mon202);
      }
    }
    if( !rv && (getRAMEndAddr( props ) != this.ramEndAddr) ) {
      rv = true;
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    boolean old64x16 = this.mode64x16;

    this.romDisabled    = false;
    this.altFontEnabled = false;
    this.mode64x16      = false;
    if( this.mode4MHz ) {
      this.emuThread.updCPUSpeed( Main.getProperties() );
      this.mode4MHz = false;
    }
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.pio.reset( true );
    } else {
      this.pio.reset( false );
    }
    this.keyboard.reset();

    if( old64x16 )
      this.screenFrm.fireScreenSizeChanged();
  }


  public void saveBasicProgram()
  {
    int begAddr = askKCBasicBegAddr();
    if( begAddr >= 0 )
      SourceUtil.saveKCBasicProgram( this.screenFrm, begAddr );
  }


  public void saveTinyBasicProgram()
  {
    int endAddr = this.emuThread.getMemWord( 0x101F ) + 1;
    if( endAddr > 0x1152 ) {
      (new SaveDlg(
		this.screenFrm,
		0x1000,
		endAddr,
		'b',
		false,          // kein KC-BASIC
		"Z1013-TinyBASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoTinyBasic();
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( !this.romDisabled && (this.romOS != null) ) {
      if( (addr >= MEM_OS) && (addr < MEM_OS + this.romOS.length) )
	done = true;
    }
    if( !done ) {
      if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + this.ramVideo.length) ) {
	this.ramVideo[ addr - MEM_SCREEN ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
      else if( !done && (addr <= this.ramEndAddr) ) {
	this.emuThread.setRAMByte( addr, value );
	rv = true;
      }
    }
    return rv;
  }


  public boolean supportsRAMFloppyA()
  {
    return true;
  }


  public boolean supportsRAMFloppyB()
  {
    return true;
  }


  public void updSysCells(
			int    begAddr,
			int    len,
			Object fileFmt,
			int    fileType )
  {
    SourceUtil.updKCBasicSysCells(
			this.emuThread,
			begAddr,
			len,
			fileFmt,
			fileType );
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xFF) == 4 ) {
      boolean oldAltFontEnabled = this.mode64x16;
      boolean oldMode64x16      = this.mode64x16;

      this.romDisabled    = ((value & 0x10) != 0);
      this.altFontEnabled = ((value & 0x20) != 0);
      this.mode64x16      = ((value & 0x80) != 0);

      Z80CPU cpu = this.emuThread.getZ80CPU();
      if( (value & 0x40) != 0 ) {
	if( !this.mode4MHz && (cpu.getMaxSpeedKHz() == 2000) ) {
	  cpu.setMaxSpeedKHz( 4000 );
	  this.mode4MHz = true;
	}
      } else {
	if( this.mode4MHz && (cpu.getMaxSpeedKHz() == 4000) ) {
	  cpu.setMaxSpeedKHz( 2000 );
	}
	this.mode4MHz = false;
      }

      if( (this.altFontEnabled != oldAltFontEnabled)
	  || (this.mode64x16 != oldMode64x16) )
      {
	this.screenFrm.setScreenDirty( true );
      }
      if( this.mode64x16 != oldMode64x16 ) {
	this.screenFrm.fireScreenSizeChanged();
      }
    }
    else if( (port & 0xF8) == 0x98 ) {
      this.emuThread.getRAMFloppyA().writeByte( port & 0x07, value );
    }
    else if( (port & 0xF8) == 0x58 ) {
      this.emuThread.getRAMFloppyB().writeByte( port & 0x07, value );
    } else {
      switch( port & 0x1C ) {		// Adressleitungen ab A5 ignorieren
	case 0:				// IOSEL0 -> PIO
	  switch( port & 0x03 ) {
	    case 0:
	      this.pio.writePortA( value );
	      break;

	    case 1:
	      this.pio.writeControlA( value );
	      break;

	    case 2:
	      this.pio.writePortB( value );
	      this.keyboard.putRowValuesToPIO();
	      this.emuThread.writeAudioPhase(
			(this.pio.fetchOutValuePortB( false ) & 0x80) != 0 );
	      break;

	    case 3:
	      this.pio.writeControlB( value );
	      break;
	  }
	  break;

	case 8:				// IOSEL2 -> Tastaturspalten
	  this.keyboard.setSelectedCol( value );
	  break;
      }
    }
    z80AddressChanged( port );
  }


	/* --- private Methoden --- */

  private static int getRAMEndAddr( Properties props )
  {
    int    rv      = 0xFFFF;
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "Z1013.01" )
	|| sysName.startsWith( "Z1013.16" ) )
    {
      rv = 0x3FFF;
    }
    else if( sysName.startsWith( "Z1013.12" ) ) {
      rv = 0x03FF;
    }
    return rv;
  }


  private void showNoTinyBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein Z1013-TinyBASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

