/*
 * (c) 2008-2009 Jens Mueller
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


public class AC1 extends EmuSys implements Z80CTCListener
{
  /*
   * Diese Tabelle mappt die AC1-BASIC-Tokens in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] ac1Tokens = {
	"END",      "FOR",    "NEXT",   "DATA",             // 0x80
	"INPUT",    "DIM",    "READ",   "LET",
	"GOTO",     "RUN",    "IF",     "RESTORE",
	"GOSUB",    "RETURN", "REM",    "STOP",
	"OUT",      "ON",     "NULL",   "WAIT",             // 0x90
	"DEF",      "POKE",   "DOKE",   "AUTO",
	"LINES",    "CLS",    "WIDTH",  "BYE",
	"RENUMBER", "CALL",   "PRINT",  "CONT",
	"LIST",     "CLEAR",  "CLOAD",  "CSAVE",            // 0xA0
	"NEW",      "TAB(",   "TO",     "FN",
	"SPC(",     "THEN",   "NOT",    "STEP",
	"+",        "-",      "*",      "/",
	"^",        "AND",    "OR",     ">",                // 0xB0
	"=",        "<",      "SGN",    "INT",
	"ABS",      "USR",    "FRE",    "INP",
	"POS",      "SQR",    "RND",    "LN",
	"EXP",      "COS",    "SIN",    "TAN",              // 0xC0
	"ATN",      "PEEK",   "DEEK",   "POINT",
	"LEN",      "STR$",   "VAL",    "ASC",
	"CHR$",     "LEFT$",  "RIGHT$", "MID$",
	"SET",      "RESET",  "WINDOW", "SCREEN",           // 0xD0
	"EDIT",     "ASAVE",  "ALOAD",  "TRON",
	"TROFF" };

  public static final int MEM_BASIC  = 0x0800;
  public static final int MEM_SCREEN = 0x1000;
  public static final int MEM_SRAM   = 0x1800;

  private static byte[] mon31         = null;
  private static byte[] scchMon1088   = null;
  private static byte[] minibasic     = null;
  private static byte[] ac1FontBytes  = null;
  private static byte[] scchFontBytes = null;

  private byte[]  ramStatic;
  private byte[]  ramVideo;
  private Z80CTC  ctc;
  private Z80PIO  pio;
  private boolean fullDRAM;
  private boolean scch1088;
  private boolean keyboardUsed;


  public AC1( EmuThread emuThread, Properties props )
  {
    super( emuThread );
    this.ramStatic = new byte[ 0x0800 ];
    this.ramVideo  = new byte[ 0x0800 ];
    this.fullDRAM  = false;
    this.scch1088  = isSCCH1088( props );
    if( this.scch1088 ) {
      if( scchMon1088 == null ) {
	scchMon1088 = readResource( "/rom/ac1/scchmon_1088.bin" );
      }
      if( scchFontBytes == null ) {
	scchFontBytes = readResource( "/rom/ac1/scchfont.bin" );
      }
    } else {
      if( mon31 == null ) {
	mon31 = readResource( "/rom/ac1/mon_31.bin" );
      }
      if( minibasic == null ) {
	minibasic = readResource( "/rom/ac1/minibasic.bin" );
      }
      if( ac1FontBytes == null ) {
	ac1FontBytes = readResource( "/rom/ac1/ac1font.bin" );
      }
    }

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu, null );
    this.pio   = new Z80PIO( cpu, this.ctc );
    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this.ctc );
    this.keyboardUsed = false;

    reset( EmuThread.ResetLevel.POWER_ON );
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    // Verbindung von Ausgang 0 auf Eingang 1 emulieren
    if( (ctc == this.ctc) && (timerNum == 0) )
      ctc.externalUpdate( 1, 1 );
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    this.emuThread.getZ80CPU().removeTStatesListener( this.ctc );
  }


  public String extractScreenText()
  {
    StringBuilder buf = new StringBuilder( 65 * 32 );
    for( int i = 0; i < 32; i++ ) {
      int rowIdx  = 0x07FF - (i * 64);
      int nSpaces = 0;
      for( int k = 0; k < 64; k++ ) {
	int a = rowIdx - k;
	int b = 20;
	if( (a >= 0) && (a < this.ramVideo.length) ) {
	  b = (int) this.ramVideo[ a ] & 0xFF;
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
    return buf.toString();
  }


  public int getAppStartStackInitValue()
  {
    return 0x2000;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv        = BLACK;
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      fontBytes = (this.scch1088 ? scchFontBytes : ac1FontBytes);
    }
    if( fontBytes != null ) {
      int row  = y / 8;
      int col  = x / 6;
      int vIdx = this.ramVideo.length - 1 - (row * 64) - col;
      if( (vIdx >= 0) && (vIdx < this.ramVideo.length) ) {
	int fIdx = (((int) this.ramVideo[ vIdx ] & 0xFF) * 8) + (y % 8);
	if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
	  int m = 1;
	  int n = x % 6;
	  if( n > 0 ) {
	    m <<= n;
	  }
	  if( (fontBytes[ fIdx ] & m) != 0 ) {
	    rv = WHITE;
	  }
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
    int rv = 0;
    if( this.scch1088 ) {
      if( this.scchMon1088 != null ) {
	rv = this.scchMon1088.length - 1;
      }
    } else {
      if( mon31 != null ) {
	rv = mon31.length - 1;
      }
    }
    return rv;
  }


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( !this.fullDRAM ) {
      if( this.scch1088 ) {
	if( scchMon1088 != null ) {
	  if( addr < scchMon1088.length ) {
	    rv   = (int) scchMon1088[ addr ] & 0xFF;
	    done = true;
	  }
	}
      } else {
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
      }
      if( (addr >= MEM_SCREEN)
	  && (addr < MEM_SCREEN + this.ramVideo.length) )
      {
	rv   = (int) this.ramVideo[ addr - MEM_SCREEN ] & 0xFF;
	done = true;
      }
      else if( (addr >= MEM_SRAM)
	       && (addr < MEM_SRAM + this.ramStatic.length) )
      {
	rv   = (int) this.ramStatic[ addr - MEM_SRAM ] & 0xFF;
	done = true;
      }
    }
    if( !done ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getScreenBaseHeight()
  {
    return 256;
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
    return "AC1";
  }


  public static String getTinyBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x1950,
				memory.getMemWord( 0x18E9 ) + 1 );
  }


  public boolean keyPressed( KeyEvent e )
  {
    boolean rv = false;
    int     ch = 0;
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
	ch = 0x7F;
	break;
    }
    if( ch > 0 ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  public void keyReleased( int keyCode )
  {
    this.pio.putInValuePortA( 0, 0xFF );
  }


  public void keyTyped( char ch )
  {
    if( (ch > 0) && (ch < 0x7F) )
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
  }


  public void openBasicProgram()
  {
    String text = SourceUtil.getKCStyleBasicProgram(
					this.emuThread,
					0x60F7,
					ac1Tokens );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoAC1Basic();
    }
  }


  public void openTinyBasicProgram()
  {
    if( this.scch1088 ) {
      super.openTinyBasicProgram();
    } else {
      String text = getTinyBasicProgram( this.emuThread );
      if( text != null ) {
	this.screenFrm.openText( text );
      } else {
	showNoMiniBasic();
      }
    }
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0xF8) == 0xE0 ) {
      rv = this.emuThread.getRAMFloppyA().readByte( port & 0x07 );
    } else {
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
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System
   * oder das Monitorprogramm aendert
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv =  !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "AC1" );
    if( !rv ) {
      if( isSCCH1088( props ) != this.scch1088 )
	rv = true;
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramStatic );
      fillRandom( this.ramVideo );
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
    this.keyboardUsed = false;
  }


  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCStyleBasicEndAddr( this.emuThread, 0x60F7 );
    if( endAddr >= 0x60F7 ) {
      (new SaveDlg(
		this.screenFrm,
		0x60F7,
		endAddr,
		'B',
		false,          // kein KC-BASIC
		"AC1-BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoAC1Basic();
    }
  }


  public void saveTinyBasicProgram()
  {
    if( this.scch1088 ) {
      super.saveTinyBasicProgram();
    } else {
      int endAddr = this.emuThread.getMemWord( 0x18E9 ) + 1;
      if( endAddr > 0x1950 ) {
	(new SaveDlg(
		this.screenFrm,
		0x18C0,
		endAddr,
		'b',
		false,          // kein KC-BASIC
                "AC1-MiniBASIC-Programm speichern" )).setVisible( true );
      } else {
	showNoMiniBasic();
      }
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( !this.fullDRAM ) {
      if( this.scch1088 ) {
	if( scchMon1088 != null ) {
	  if( addr < scchMon1088.length )
	    done = true;
	}
      } else {
	if( this.mon31 != null ) {
	  if( addr < mon31.length )
	    done = true;
	}
	if( !done ) {
	  if( this.minibasic != null ) {
	    if( (addr >= MEM_BASIC) && (addr < MEM_BASIC + minibasic.length) )
	      done = true;
	  }
	}
      }
      if( !done ) {
	if( (addr >= MEM_SCREEN)
	    && (addr < MEM_SCREEN + this.ramVideo.length) )
	{
	  this.ramVideo[ addr - MEM_SCREEN ] = (byte) value;
	  this.screenFrm.setScreenDirty( true );
	  rv = true;
	}
	else if( (addr >= MEM_SRAM)
		 && (addr < MEM_SRAM + this.ramStatic.length) )
	{
	  this.ramStatic[ addr - MEM_SRAM ] = (byte) value;
	  rv = true;
	}
      }
    }
    if( !done ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  public boolean supportsRAMFloppyA()
  {
    return true;
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xF8) == 0xE0 ) {
      this.emuThread.getRAMFloppyA().writeByte( port & 0x07, value );
    } else {
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
	  this.emuThread.writeAudioPhase(
		(this.pio.fetchOutValuePortB( false )
			& (this.emuThread.isLoudspeakerEmulationEnabled() ?
							0x01 : 0x40 )) != 0 );
	  break;

	case 6:
	  this.pio.writeControlA( value );
	  break;

	case 7:
	  this.pio.writeControlB( value );
	  break;

	case 0x1C:
	case 0x1D:
	case 0x1E:
	case 0x1F:
	  this.fullDRAM = ((value & 0x01) != 0);
	  break;
      }
    }
  }


	/* --- private Methoden --- */

  private static boolean isSCCH1088( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.ac1.monitor" ).equals( "SCCH10/88" );
  }


  private void showNoAC1Basic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein AC1-BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  private void showNoMiniBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein AC1-MiniBASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

