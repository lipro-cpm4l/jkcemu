/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des BCS3
 */

package jkcemu.bcs3;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class BCS3 extends EmuSys implements
					Z80CTCListener,
					Z80TStatesListener
{
  /*
   * Die beiden Tabellen mappen die BASIC-SE2.4- und BASIC-SE3.1-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer jede Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] se24Tokens = {
	null,     null,   null,    null,		// 0xB0
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,		// 0xC0
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     "THEN", "AND",   null,		// 0xD0
	null,     null,   "OR",    "PEEK(",
	null,     "IN(",  "RND(",  null,
	null,     "BYTE", "END",   null,
	"REM",    null,   "GOSUB", null,		// 0xE0
	"CLEAR",  null,   "IF",    null,
	"INPUT",  null,   "PRINT", null,
	"RETURN", null,   "GOTO",  null,
	"RUN",    null,   "LIST",  null,		// 0xF0
	"LET",    null,   "LOAD",  null,
	"SAVE",   null,   "POKE",  null,
	"OUT",    null,   "NEW",   null };

  /*
   * Diese Tabelle mappt die BCS3-BASIC-SE3.1-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] se31Tokens = {
	"LEN(",    null, "INT(",   null,		// 0xB0
	"USR(",    null, null,     null,
	"INKEY$",  null, "STEP",   null,
	"TO",      null, "CHR$",   null,
	"THEN",    null, "AND",    null,		// 0xC0
	"OR",      null, "PEEK(",  null,
	"IN(",     null, "RND",    null,
	null,      null, "END",    null,
	"REM",     null, "GOSUB",  null,		// 0xD0
	"CLS",     null, "IF",     null,
	"INPUT",   null, "PRINT",  null,
	"RESTORE", null, "RETURN", null,
	"GOTO",    null, "RUN",    null,		// 0xE0
	"LIST",    null, "LET",    null,
	"LOAD",    null, "SAVE",   null,
	"POKE",    null, "OUT",    null,
	"FOR",     null, "NEXT",   null,		// 0xF0
	"DIM",     null, "PLOT",   null,
	"UNPLOT",  null, "NEW",    null,
	"READ",    null, "DATA",   null };

  private static byte[] se24Bytes  = null;
  private static byte[] se31_29Bytes  = null;
  private static byte[] se31_40Bytes  = null;
  private static byte[] bcs3FontBytes = null;

  private Z80CTC           ctc;
  private byte[]           rom;
  private byte[]           ram;
  private int              ramEndAddr;
  private int              screenOffset;
  private int              screenCharCols;
  private int              screenCharRows;
  private int              screenRowOffset;
  private int              screenActiveTStates;
  private volatile boolean screenEnabled;
  private int[]            kbMatrix;
  private boolean          se31;
  private boolean          audioOutPhase;


  public BCS3( EmuThread emuThread, Properties props )
  {
    super( emuThread );
    if( bcs3FontBytes == null ) {
      bcs3FontBytes = readResource( "/rom/bcs3/bcs3font.bin" );
    }
    this.se31 = isSE31( props );
    if( this.se31 ) {
      this.screenOffset   = 0x0080;
      this.screenCharRows = 3;		// Anfangswert
      if( is40CharsPerLineMode( props ) ) {
	this.screenCharCols  = 40;
	this.screenRowOffset = 41;
	if( se31_40Bytes == null ) {
	  se31_40Bytes = readResource( "/rom/bcs3/se31_40.bin" );
	}
	this.rom = se31_40Bytes;
      } else {
	this.screenCharCols  = 29;
	this.screenRowOffset = 30;
	if( se31_29Bytes == null ) {
	  se31_29Bytes = readResource( "/rom/bcs3/se31_29.bin" );
	}
	this.rom = se31_29Bytes;
      }
    } else {
      this.screenOffset    = 0x0050;
      this.screenCharCols  = 27;
      this.screenCharRows  = 12;
      this.screenRowOffset = 28;
      if( se24Bytes == null ) {
	se24Bytes = readResource( "/rom/bcs3/se24.bin" );
      }
      this.rom = se24Bytes;
    }
    this.screenActiveTStates = 0;
    this.screenEnabled       = false;
    this.ram                 = new byte[ 0x0400 ];
    this.ramEndAddr          = getRAMEndAddr( props );
    this.kbMatrix            = new int[ 10 ];
    this.audioOutPhase       = false;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu, null );
    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON );
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return is40CharsPerLineMode( props ) ? 3500 : 2500;
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:
	  this.audioOutPhase = !this.audioOutPhase;
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	  ctc.externalUpdate( 1, 1 );
	  break;

	case 1:
	  this.audioOutPhase = false;
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	  ctc.externalUpdate( 2, 1 );
	  break;

	case 2:
	  // Bildschirmausgabe einschalten, wenn Bildsynchronimpulse kommen
	  this.screenActiveTStates = 100000;
	  if( !this.screenEnabled ) {
	    this.screenEnabled = true;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.systemUpdate( tStates );

    // Bildschirmausgabe ausschalten, wenn keine Bildsynchronimpulse kommen
    if( this.screenActiveTStates > 0 ) {
      this.screenActiveTStates -= tStates;
      if( this.screenActiveTStates <= 0 ) {
	if( this.screenEnabled ) {
	  this.screenEnabled = false;
	  this.screenFrm.setScreenDirty( true );
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    this.emuThread.getZ80CPU().removeTStatesListener( this );
  }


  public String extractScreenText()
  {
    return EmuUtil.extractText(
			this.ram,
			this.screenOffset,
			this.screenCharRows,
			this.screenCharCols,
			this.screenRowOffset );
  }


  public int getAppStartStackInitValue()
  {
    return this.se31 ? 0x3C80 : 0x3C50;
  }


  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( this.screenEnabled ) {
      byte[] fontBytes = this.emuThread.getExtFontBytes();
      if( fontBytes == null ) {
	fontBytes = bcs3FontBytes;
      }
      if( fontBytes != null ) {
	int fIdx = -1;
	if( this.se31 ) {
	  int row = y / 8;
	  int col = x / 8;
	  if( (row < this.screenCharRows) && (col < this.screenCharCols) ) {
	    int mIdx = this.screenOffset
				+ (row * this.screenRowOffset)
				+ col;
	    if( (mIdx >= 0) && (mIdx < this.ram.length) ) {
	      int ch = (int) (this.ram[ mIdx ] & 0xFF);
	      fIdx = (ch * 8) + (y % 8);
	    }
	  }
	} else {
	  int rPix = y % 16;
	  if( (rPix >= 2) && (rPix <= 9) ) {
	    int row = y / (this.se31 ? 8 : 16);
	    int col = x / 8;
	    if( (row < this.screenCharRows) && (col < this.screenCharCols) ) {
	      int mIdx = this.screenOffset
				+ (row * this.screenRowOffset)
				+ col;
	      if( (mIdx >= 0) && (mIdx < this.ram.length) ) {
		int ch = (int) (this.ram[ mIdx ] & 0xFF);
		fIdx = (ch * 8) + rPix - 2;
	      }
	    }
	  }
	}
	if( (fIdx >= 0) && (fIdx < fontBytes.length) ) {
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


  public int getDefaultStartAddress()
  {
    return 0x4000;
  }


  public int getMinOSAddress()
  {
    return 0;
  }


  public int getMaxOSAddress()
  {
    return this.rom != null ? this.rom.length - 1 : 0;
  }


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( addr < 0x4000 ) {
      addr &= 0xDFFF;		// A13=0
      if( (addr >= 0x1000) && (addr < 0x1400)) {
	// Abfrage Tastatur und Kassettenrecordereingang
	rv    = 0x00;
	int a = ~addr;
	int m = 0x01;
	for( int i = 0; i < this.kbMatrix.length; i++ ) {
	  if( (a & m) != 0 ) {
	    rv |= this.kbMatrix[ i ];
	  }
	  m <<= 1;
	}
	if( this.emuThread.readAudioPhase() ) {
	  rv |= 0x80;
	} else {
	  rv &= 0x7F;
	}
      }
      else if( (addr >= 0x1800) && (addr < 0x1C00)) {
	/*
	 * Zugriff auf den Bildwiederholspeicher
	 * Beim BCS3 wird damit das Zeichen auf dem Bildschirm ausgegeben.
	 * Ist Bit 7 nicht gesetzt, liest die CPU NOP-Befehle (0x00).
	 */
	rv      = 0;
	int idx = addr - 0x1800;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  int ch = (int) (this.ram[ idx ] & 0xFF);
	  if( (ch & 0x80) != 0 ) {
	    rv = ch;
	  }
	}
      }
      else if( (addr >= 0x1C00) && (addr < 0x2000)) {
	int idx = addr - 0x1C00;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  rv = (int) (this.ram[ idx ] & 0xFF);
	}
      }
      else if( this.rom != null ) {
	if( addr < this.rom.length ) {
	  rv = (int) this.rom[ addr ] & 0xFF;
	}
      }
    }
    else if( addr <= this.ramEndAddr ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getScreenBaseHeight()
  {
    return this.se31 ? 232 : 192;
  }


  public int getScreenBaseWidth()
  {
    return this.screenCharCols * 8;
  }


  public boolean getSwapKeyCharCase()
  {
    return false;
  }


  public String getSystemName()
  {
    return "BCS3";
  }


  public boolean keyPressed( KeyEvent e )
  {
    boolean rv = false;
    switch( e.getKeyCode() ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	this.kbMatrix[ 7 ] = 0x01;
	rv                 = true;
	break;

      case KeyEvent.VK_ENTER:
	this.kbMatrix[ 8 ] = 0x01;
	rv                 = true;
	break;

      case KeyEvent.VK_SPACE:
	this.kbMatrix[ 6 ] = 0x01;
	rv                 = true;
    }
    return rv;
  }


  public void keyReleased( int keyCode )
  {
    Arrays.fill( this.kbMatrix, 0 );
  }


  public void keyTyped( char ch )
  {
    int     idx       = -1;
    int     value     = 0;
    boolean shiftMode = false;
    if( (ch >= 0x20) && (ch <= 0x29) ) {
      idx       = ch - 0x20;
      value     = 0x08;
      shiftMode = true;
    } else if( (ch >= 0x29) && (ch <= 0x2F) ) {
      idx       = ch - 0x26;
      value     = 0x04;
      shiftMode = true;
    } else if( (ch >= '0') && (ch <= '9') ) {
      idx   = ch - '0';
      value = 0x08;
    } else if( (ch >= 0x3A) && (ch <= 0x40) ) {
      idx       = ch - 0x3A;
      value     = 0x02;
      shiftMode = true;
    } else {
      if( (ch >= 'a') && (ch <= 'z') ) {
	ch = Character.toUpperCase( ch );
      }
      if( (ch >= 'A') && (ch <= 'J') ) {
	idx   = ch - 'A';
	value = 0x04;
      }
      else if( (ch >= 'K') && (ch <= 'T') ) {
	idx   = ch - 'K';
	value = 0x02;
      }
      else if( (ch >= 'U') && (ch <= 'Z') ) {
	idx   = ch - 'U';
	value = 0x01;
      }
    }
    if( (idx >= 0) && (idx < this.kbMatrix.length) ) {
      if( shiftMode && (idx == 9) ) {
	this.kbMatrix[ idx ] = value | 0x01;
      } else {
	if( shiftMode ) {
	  this.kbMatrix[ 9 ] = 0x01;
	}
	this.kbMatrix[ idx ] = value;
      }
    }
  }


  public void openBasicProgram()
  {
    String text = null;
    int    addr = 0x3DA1;
    if( this.se31 ) {
      addr = getMemWord( 0x3C00 );
    }
    int lineNum = getMemWord( addr );
    if( (lineNum >= 0) && (lineNum < 9999) ) {
      String[]      tokens = this.se31 ? se31Tokens : se24Tokens;
      StringBuilder buf    = new StringBuilder( 0x2000 );
      while( (lineNum >= 0) && (lineNum <= 9999) ) {
	buf.append( lineNum );
	addr += 2;
	boolean sp = true;
	int     ch = getMemByte( addr++ );
	while( (ch != 0) && (ch != 0x1E) ) {
	  if( sp ) {
	    buf.append( (char) '\u0020' );
	    sp = false;
	  }
	  if( ch >= 0xB0 ) {
	    int idx = ch - 0xB0;
	    if( (idx >= 0) && (idx < tokens.length) ) {
	      String s = tokens[ idx ];
	      if( s != null ) {
		buf.append( s );
		ch = 0;
	      }
	    }
	  }
	  if( ch != 0 ) {
	    buf.append( (char) ch );
	  }
	  ch = getMemByte( addr++ );
	}
	buf.append( (char) '\n' );
	if( ch == 0 ) {
	  break;
	}
	if( lineNum == 9999 ) {
	  break;
	}
	lineNum = getMemWord( addr );
      }
      if( buf.length() > 0 ) {
	text = buf.toString();
      }
    }
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0x04) == 0 ) {		// A2=0
      rv = this.ctc.read( port & 0x03 );
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System
   * oder das Monitorprogramm aendert
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv = !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "BCS3" );
    if( !rv ) {
      if( this.se31 == isSE31( props ) ) {
	boolean mode40 = is40CharsPerLineMode( props );
	if( (mode40 && (this.screenCharCols != 40))
	    || (!mode40 && (this.screenCharCols == 40)) )
	{
	  rv = true;
	}
      } else {
	rv = true;
      }
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      if( this.se31 ) {
	this.screenCharRows = 3;
      }
      fillRandom( this.ram );
      this.ctc.reset( true );
    } else {
      this.ctc.reset( false );
    }
    Arrays.fill( this.kbMatrix, 0 );
  }


  public void saveBasicProgram()
  {
    int begAddr = 0x3DA1;
    if( this.se31 ) {
      begAddr = getMemWord( 0x3C00 );
    }
    int endAddr = begAddr;
    int lineNum = getMemWord( begAddr );
    if( (lineNum >= 0) && (lineNum < 9999) ) {
      int addr = begAddr;
      while( (lineNum >= 0) && (lineNum <= 9999) ) {
	addr += 2;
	int ch = getMemByte( addr++ );
	while( (ch != 0) && (ch != 0x1E) ) {
	  ch = getMemByte( addr++ );
	}
	if( ch == 0x1E ) {
	  endAddr = addr;
	} else {
	  break;
	}
	lineNum = getMemWord( addr );
      }
    }
    if( endAddr > begAddr + 1 ) {
      (new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr - 1,
		'B',
		false,          // kein KC-BASIC
		"BASIC-SE-Programm speichern" )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    if( addr < 0x4000 ) {
      addr &= 0xDFFF;		// A13=0
      if( (addr >= 0x1C00) && (addr < 0x2000)) {
	int idx = addr - 0x1C00;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  this.ram[ idx ] = (byte) value;
	  if( this.se31 && (addr == 0x1C06) ) {
	    int nRows = (int) (this.ram[ 6 ] & 0xFF);
	    if( nRows != this.screenCharRows ) {
	      this.screenCharRows = nRows;
	      this.screenFrm.setScreenDirty( true );
	    }
	  }
	  if( (idx >= this.screenOffset)
	      && (idx < this.screenOffset
				+ (this.screenCharRows * this.screenCharCols)) )
	  {
	    this.screenFrm.setScreenDirty( true );
	  }
	}
	rv = true;
      }
    }
    else if( addr <= this.ramEndAddr ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0x04) == 0 )		// A2=0 
      this.ctc.write( port & 0x03, value );
  }


	/* --- private Methoden --- */

  private static int getRAMEndAddr( Properties props )
  {
    int    endAddr = 0x3FFF;
    String ramText = EmuUtil.getProperty( props, "jkcemu.bcs3.ram.kbyte" );
    if( ramText.equals( "17" ) ) {
      endAddr = 0x7FFF;
    }
    else if( ramText.equals( "33" ) ) {
      endAddr = 0xBFFF;
    }
    return endAddr;
  }


  private static boolean is40CharsPerLineMode( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.bcs3.chars_per_line" ).equals( "40" );
  }


  private static boolean isSE31( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.bcs3.os.version" ).equals( "SE3.1" );
  }


  private void showNoBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein BASIC-SE-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

