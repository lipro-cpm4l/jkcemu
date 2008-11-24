/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen zum Behandeln von Quelltexten
 */

package jkcemu.base;

import java.io.IOException;
import java.lang.*;
import z80emu.Z80MemView;


public class SourceUtil
{
  /*
   * Diese Tabelle mappt die AC1-BASIC-Tokens in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] ac1Tokens = {
    "END",      "FOR",    "NEXT",   "DATA",		// 0x80
    "INPUT",    "DIM",    "READ",   "LET",
    "GOTO",     "RUN",    "IF",     "RESTORE",
    "GOSUB",    "RETURN", "REM",    "STOP",
    "OUT",      "ON",     "NULL",   "WAIT",		// 0x90
    "DEF",      "POKE",   "DOKE",   "AUTO",
    "LINES",    "CLS",    "WIDTH",  "BYE",
    "RENUMBER", "CALL",   "PRINT",  "CONT",
    "LIST",     "CLEAR",  "CLOAD",  "CSAVE",		// 0xA0
    "NEW",      "TAB(",   "TO",     "FN",
    "SPC(",     "THEN",   "NOT",    "STEP",
    "+",        "-",      "*",      "/",
    "^",        "AND",    "OR",     ">",		// 0xB0
    "=",        "<",      "SGN",    "INT",
    "ABS",      "USR",    "FRE",    "INP",
    "POS",      "SQR",    "RND",    "LN",
    "EXP",      "COS",    "SIN",    "TAN",		// 0xC0
    "ATN",      "PEEK",   "DEEK",   "POINT",
    "LEN",      "STR$",   "VAL",    "ASC",
    "CHR$",     "LEFT$",  "RIGHT$", "MID$",
    "SET",      "RESET",  "WINDOW", "SCREEN",		// 0xD0
    "EDIT",     "ASAVE",  "ALOAD",  "TRON",
    "TROFF" };



  /*
   * Diese Tabelle mappt die KC-BASIC-Tokens in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   * Die Tokens ab Code 226 (0xE2) gibt es im KC-BASIC-Interpreter
   * des Z1013 nicht, jedoch z.B. im KC85/4.
   * Da aber trotzdem ein BASIC-Programm, welches diese Anweisungen enthaelt,
   * in einen Z1013 geladen werden kann und dann dort auch richtig angezeigt
   * werden sollte, werden diese Tokens mit gemappt.
   *
   * Ab dem "KC-BASIC-Interpreter Plus" fuer den Z1013 sind folgende vier
   * Tokens hinzugekommen, von denen drei nicht mit den Tokens der
   * anderen KCs uebereinstimmen:
   *	226     HSAVE
   *	227     HLOAD
   *	228     PSET
   *	229     PRES
   *
   * Da jedoch die Befehle HSAVE und HLOAD in einem BASIC-Programm wenig
   * Sinn machen und die Anweisungen PSET und PRES trotzt Vorhandensein
   * einen Syntax-Fehler hervorrufen, werden diese Tokens entsprechend
   * der KCs gemappt, auf denen diese Anweisungen auch funktionieren.
   */
  private static final String[] kcTokens = {
    "END",       "FOR",      "NEXT",    "DATA",		// 0x80
    "INPUT",     "DIM",      "READ",    "LET",
    "GOTO",      "RUN",      "IF",      "RESTORE",
    "GOSUB",     "RETURN",   "REM",     "STOP",
    "OUT",       "ON",       "NULL",    "WAIT",		// 0x90
    "DEF",       "POKE",     "DOKE",    "AUTO",
    "LINES",     "CLS",      "WIDTH",   "BYE",
    "!",         "CALL",     "PRINT",   "CONT",
    "LIST",      "CLEAR",    "CLOAD",   "CSAVE",	// 0xA0
    "NEW",       "TAB(",     "TO",      "FN",
    "SPC(",      "THEN",     "NOT",     "STEP",
    "+",         "-",        "*",       "/",
    "^",         "AND",      "OR",      ">",		// 0xB0
    "=",         "<",        "SGN",     "INT",
    "ABS",       "USR",      "FRE",     "INP",
    "POS",       "SQR",      "RND",     "LN",
    "EXP",       "COS",      "SIN",     "TAN",		// 0xC0
    "ATN",       "PEEK",     "DEEK",    "PI",
    "LEN",       "STR$",     "VAL",     "ASC",
    "CHR$",      "LEFT$",    "RIGHT$",  "MID$",
    "LOAD",      "TRON",     "TROFF",   "EDIT",		// 0xD0
    "ELSE",      "INKEY$",   "JOYST",   "STRING$",
    "INSTR",     "RENUMBER", "DELETE",  "PAUSE",
    "BEEP",      "WINDOW",   "BORDER",  "INK",
    "PAPER",     "AT",       "COLOR",   "SOUND",	// 0xE0
    "PSET",      "PRESET",   "BLOAD",   "VPEEK",
    "VPOKE",     "LOCATE",   "KEYLIST", "KEY",
    "SWITCH",    "PTEST",    "CLOSE",   "OPEN",
    "RANDOMIZE", "VGET$",    "LINE",    "CIRCLE",	// 0xF0
    "CSRLIN" };


  public static String getAssemblerText(
				Z80MemView memory,
				int        addr ) throws IOException
  {
    String rv      = null;
    int    endAddr = addr + memory.getMemWord( addr ) - 1;
    if( endAddr >= 6 ) {
      if( (memory.getMemByte( endAddr  ) == 0xFF)
	  && (memory.getMemByte( endAddr - 1 ) == 0) )
      {
	StringBuilder buf = new StringBuilder( 0x4000 );
	addr += 7;
	while( addr < endAddr ) {
	  int b = memory.getMemByte( addr++ );
	  if( b == 0xFF ) {
	    break;
	  }
	  if( b == 0 ) {
	    buf.append( (char) '\n' );
	    addr += 2;
	  } else if( b < 0x20 ) {
	    for( int i = 0; i < b; i++ ) {
	      buf.append( (char) '\u0020' );
	    }
	  } else {
	    buf.append( (char) b );
	  }
	}
	rv = buf.toString();
      }
    }
    return rv;
  }


  public static String getAC1BasicText( Z80MemView memory ) throws IOException
  {
    String rv = getBasicText( memory, 0x60F7, ac1Tokens );
    if( rv == null ) {
      throwNoAC1Basic();
    }
    return rv;
  }


  public static String getKCBasicText(
				Z80MemView memory,
				int        addr ) throws IOException
  {
    String rv = getBasicText( memory, addr, kcTokens );
    if( rv == null ) {
      throwNoKCBasic();
    }
    return rv;
  }


  public static String getTinyBasicText(
				String     sysName,
				Z80MemView memory ) throws IOException
  {
    int addr    = -1;
    int endAddr = -1;
    if( sysName.startsWith( "AC1" ) ) {
      addr    = 0x1950;
      endAddr = memory.getMemWord( 0x18E9 ) + 1;
    } else if( sysName.startsWith( "Z1013" ) ) {
      addr    = 0x1152;
      endAddr = memory.getMemWord( 0x101F ) + 1;
    }
    if( addr < 0 ) {
      throwTinyBasicNotSupported();
    }
    if( endAddr <= addr ) {
      throwNoTinyBasic();
    }
    int           len = endAddr - addr;
    StringBuilder buf = new StringBuilder( len * 3 / 2 );

    // Tiny-BASIC-Programm extrahieren
    while( addr < endAddr - 1 ) {
      buf.append( memory.getMemWord( addr ) );
      addr += 2;

      // Anzahl Leerzeichen vor der Anweisung ermitteln
      int n = 0;
      while( addr < endAddr ) {
	int ch = memory.getMemByte( addr );
	if( ch == '\u0020' ) {
	  n++;
	  addr++;
	} else {
	  if( ch != '\r' ) {
	    for( int i = 0; i <= n; i++ )
	      buf.append( (char) '\u0020' );
	  }
	  break;
	}
      }

      // Zeile ausgeben
      while( addr < endAddr ) {
	int ch = memory.getMemByte( addr++ );
	if( ch == '\r' ) {
	  break;
	}
	if( ch != 0 ) {
	  buf.append( (char) ch );
	}
      }
      buf.append( (char) '\n' );
    }
    return buf.toString();
  }


  public static void saveAC1BasicProgram( ScreenFrm screenFrm )
							throws IOException
  {
    int endAddr = getBasicEndAddr( screenFrm, 0x60F7 );
    if( endAddr >= 0x60F7 ) {
      SaveDlg dlg = new SaveDlg(
				screenFrm,
				0x60F7,
				endAddr,
				'B',
				false,		// KC-BASIC
				"AC1-BASIC-Programm speichern" );
      dlg.setVisible( true );
    } else {
      throwNoAC1Basic();
    }
  }


  public static void saveKCBasicProgram(
				ScreenFrm  screenFrm,
				int        begAddr ) throws IOException
  {
    int endAddr = getBasicEndAddr( screenFrm, begAddr );
    if( endAddr >= begAddr ) {
      SaveDlg dlg = new SaveDlg(
				screenFrm,
				begAddr - 0x41,
				endAddr,
				'B',
				true,		// KC-BASIC
				"KC-BASIC-Programm speichern" );
      dlg.setVisible( true );
    } else {
      throwNoKCBasic();
    }
  }


  public static void saveTinyBasicProgram( ScreenFrm screenFrm )
							throws IOException
  {
    EmuThread emuThread = screenFrm.getEmuThread();
    String    sysName   = emuThread.getEmuSys().getSystemName();
    if( sysName.startsWith( "AC1" ) ) {
      int endAddr = emuThread.getMemWord( 0x18E9 ) + 1;
      if( endAddr <= 0x1950 ) {
	throwNoTinyBasic();
      }
      new SaveDlg(
		screenFrm,
		0x18C0,
		endAddr,
		'b',
		false,		// KC-BASIC
		"AC1-Mini-BASIC-Programm speichern" ).setVisible( true );
    } else if( sysName.startsWith( "Z1013" ) ) {
      int endAddr = emuThread.getMemWord( 0x101F ) + 1;
      if( endAddr <= 0x1152 ) {
	throwNoTinyBasic();
      }
      new SaveDlg(
		screenFrm,
		0x1000,
		endAddr,
		'b',
		false,		// KC-BASIC
		"Z1013-Tiny-BASIC-Programm speichern" ).setVisible( true );
    } else {
      throwTinyBasicNotSupported();
    }
  }


	/* --- private Methoden --- */

  private static int getBasicEndAddr(
				ScreenFrm screenFrm,
				int       begAddr ) throws IOException
  {
    Z80MemView memory       = screenFrm.getEmuThread();
    int        endAddr      = -1;
    int        curLineAddr  = begAddr;
    int        nextLineAddr = memory.getMemWord( curLineAddr );
    while( (nextLineAddr > curLineAddr + 5)
	   && (memory.getMemByte( nextLineAddr - 1 ) == 0) )
    {
      curLineAddr  = nextLineAddr;
      nextLineAddr = memory.getMemWord( curLineAddr );
    }
    if( curLineAddr > begAddr ) {
      endAddr = curLineAddr + 1;
    }
    return endAddr;
  }


  private static String getBasicText(
				Z80MemView memory,
				int        addr,
				String[]   tokenTab ) throws IOException
  {
    StringBuilder buf = new StringBuilder( 0x4000 );

    int nextLineAddr = memory.getMemWord( addr );
    while( (nextLineAddr > addr + 5)
	   && (memory.getMemByte( nextLineAddr - 1 ) == 0) )
    {
      // Zeilennummer
      addr += 2;
      buf.append( memory.getMemWord( addr ) );
      addr += 2;

      // Anzahl Leerzeichen vor der Anweisung ermitteln
      int n = 0;
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr );
	if( ch == '\u0020' ) {
	  n++;
	  addr++;
	} else {
	  if( ch != 0 ) {
	    for( int i = 0; i <= n; i++ )
	      buf.append( (char) '\u0020' );
	  }
	  break;
	}
      }

      // Programmzeile extrahieren
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr++ );
	if( ch == 0 )
	  break;

	if( ch == '\"' ) {
	  buf.append( (char) ch );
	  while( addr < nextLineAddr ) {
	    ch = memory.getMemByte( addr++ );
	    if( ch == 0 )
	      break;

	    buf.append( (char) ch );
	    if( ch == '\"' )
	      break;
	  }
	} else {
	  if( ch >= 0x80 ) {
	    int pos = ch - 0x80;
	    if( (pos >= 0) && (pos < tokenTab.length) )
	      buf.append( tokenTab[ pos ] );
	  } else {
	    buf.append( (char) ch );
	  }
	}
      }
      buf.append( (char) '\n' );

      // naechste Zeile
      addr         = nextLineAddr;
      nextLineAddr = memory.getMemWord( addr );
    }

    String rv = null;
    if( buf.length() > 0 ) {
      rv = buf.toString();
    } else {
      throwNoKCBasic();
    }
    return rv;
  }


  private static void throwNoAC1Basic() throws IOException
  {
    throw new IOException(
		"Es ist kein AC1-BASIC-Programm im entsprechenden\n"
			+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  private static void throwNoKCBasic() throws IOException
  {
    throw new IOException(
		"Es ist kein KC-BASIC-Programm im entsprechenden\n"
			+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  private static void throwNoTinyBasic() throws IOException
  {
      throw new IOException(
		"Es ist kein Mini-/Tiny-BASIC-Programm im entsprechenden\n"
			+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  private static void throwTinyBasicNotSupported() throws IOException
  {
      throw new IOException(
		"Mini-/Tiny-BASIC wird f\u00FCr das gerade emulierte System"
			+ " nicht unterst\u00FCtzt." );
  }
}

