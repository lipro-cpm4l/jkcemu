/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z1013
 */

package jkcemu.z1013;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.*;


public class Z1013 extends EmuSys implements Z80AddressListener
{
  public static final int MEM_HEAD   = 0x00E0;
  public static final int MEM_SCREEN = 0xEC00;
  public static final int MEM_OS     = 0xF000;

  private static byte[] mon202         = null;
  private static byte[] monA2          = null;
  private static byte[] monRB_K7659    = null;
  private static byte[] monRB_S6009    = null;
  private static byte[] monJM_1992     = null;
  private static byte[] z1013FontBytes = null;
  private static byte[] altFontBytes   = null;
  private static byte[] ramVideo       = new byte[ 0x0400 ];

  private Z80PIO   pio;
  private Keyboard keyboard;
  private byte[]   romOS;
  private boolean  romDisabled;
  private boolean  altFontEnabled;
  private boolean  mode4MHz;
  private boolean  mode64x16;
  private boolean  ram16k;


  public Z1013( EmuThread emuThread, Properties props )
  {
    super( emuThread );
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
    this.ram16k         = EmuUtil.getProperty(
				props,
				"jkcemu.z1013.ram.kbyte" ).equals( "16" );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.pio   = new Z80PIO( cpu, null );
    this.keyboard = new Keyboard( this.pio );
    this.keyboard.applySettings( props );
    cpu.addAddressListener( this );

    reset( EmuThread.ResetLevel.POWER_ON );
  }


  public Keyboard getKeyboard()
  {
    return this.keyboard;
  }


	/* --- Z80AddressListener --- */

  public void z80AddressChanged( int addr )
  {
    // Verbindung A0 - PIO B5 emulieren
    this.pio.putInValuePortB( (addr << 5) & 0x20, 0x20 );
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    this.emuThread.getZ80CPU().removeAddressListener( this );
  }


  public String extractScreenText()
  {
    return extractMemText( 0xEC00, 32, 32, 32 );
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
      int row = y / 8;
      int col = x / 8;
      int idx = (this.emuThread.getMemByte(
			0xEC00 + (row * (this.mode64x16 ? 64 : 32)) + col )
							* 8) + (y % 8);
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


  public int getDefaultStartAddress()
  {
    return 0x0100;
  }


  public int getMinOSAddress()
  {
    return MEM_OS;
  }


  public int getMaxOSAddress()
  {
    int rv = MEM_OS;
    if( this.romOS != null ) {
      rv = MEM_OS + this.romOS.length - 1;
    }
    return rv;
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
    if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + ramVideo.length) ) {
      rv   = (int) ramVideo[ addr - MEM_SCREEN ] & 0xFF;
      done = true;
    }
    if( !done && (!this.ram16k || (addr < 0x4000)) ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return MEM_OS;
  }


  public int getScreenBaseHeight()
  {
    return this.mode64x16 ? 128 : 256;
  }


  public int getScreenBaseWidth()
  {
    return this.mode64x16 ? 512 : 256;
  }


  public String getSystemName()
  {
    return "Z1013";
  }


  public static String getTinyBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x1152,
				memory.getMemWord( 0x101F ) + 1 );
  }


  public boolean keyPressed( KeyEvent e )
  {
    return this.keyboard.setKeyCode( e.getKeyCode() );
  }


  public void keyReleased( int keyCode )
  {
    this.keyboard.setKeyReleased();
  }


  public void keyTyped( char ch )
  {
    this.keyboard.setKeyChar( ch );
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
	  value = (this.emuThread.readAudioPhase() ? 0x40 : 0)
					| (this.pio.readPortB() & 0xBF);
	  break;

	case 3:
	  value = this.pio.readControlB();
	  break;
      }
    }
    z80AddressChanged( port );
    return value;
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
    if( !rv ) {
      rv = EmuUtil.getProperty(
			props,
			"jkcemu.z1013.ram.kbyte" ).equals( "16" )
		&& !this.ram16k;
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    this.romDisabled    = false;
    this.altFontEnabled = false;
    this.mode64x16      = false;
    if( this.mode4MHz ) {
      this.emuThread.updCPUSpeed( Main.getProperties(), getSystemName() );
      this.mode4MHz = false;
    }
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.pio.reset( true );
    } else {
      this.pio.reset( false );
    }
    this.keyboard.reset();
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
      if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + ramVideo.length) ) {
	ramVideo[ addr - MEM_SCREEN ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
      else if( !this.ram16k || (addr < 0x4000) ) {
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


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xFF) == 4 ) {
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
      this.screenFrm.setScreenDirty( true );
      this.screenFrm.fireScreenSizeChanged();
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

  private void showNoTinyBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein Z1013-TinyBASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

