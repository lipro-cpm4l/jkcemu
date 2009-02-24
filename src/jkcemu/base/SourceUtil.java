/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen zum Behandeln von Quelltexten
 */

package jkcemu.base;

import java.awt.Component;
import java.lang.*;
import z80emu.Z80MemView;


public class SourceUtil
{
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
				int        addr )
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
	if( buf.length() > 0 ) {
	  rv = buf.toString();
	}
      }
    }
    return rv;
  }


  public static int getKCStyleBasicEndAddr( Z80MemView memory, int begAddr )
  {
    int endAddr      = -1;
    int curLineAddr  = begAddr;
    int nextLineAddr = memory.getMemWord( curLineAddr );
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


  public static String getKCBasicProgram( Z80MemView memory, int addr )
  {
    return getKCStyleBasicProgram( memory, addr, kcTokens );
  }


  public static String getKCStyleBasicProgram(
				Z80MemView memory,
				int        addr,
				String[]   tokenTab )
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
	    for( int i = 0; i <= n; i++ ) {
	      buf.append( (char) '\u0020' );
	    }
	  }
	  break;
	}
      }

      // Programmzeile extrahieren
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr++ );
	if( ch == 0 ) {
	  break;
	}
	if( ch == '\"' ) {
	  buf.append( (char) ch );
	  while( addr < nextLineAddr ) {
	    ch = memory.getMemByte( addr++ );
	    if( ch == 0 ) {
	      break;
	    }
	    buf.append( (char) ch );
	    if( ch == '\"' ) {
	      break;
	    }
	  }
	} else {
	  if( ch >= 0x80 ) {
	    int pos = ch - 0x80;
	    if( (pos >= 0) && (pos < tokenTab.length) ) {
	      buf.append( tokenTab[ pos ] );
	    }
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
    return buf.length() > 0 ? buf.toString() : null;
  }


  public static String getTinyBasicProgram(
				Z80MemView memory,
				int        begAddr,
				int        endAddr )
  {
    String rv = null;
    if( endAddr > begAddr ) {
      int           len = endAddr - begAddr;
      StringBuilder buf = new StringBuilder( len * 3 / 2 );

      // Tiny-BASIC-Programm extrahieren
      int addr = begAddr;
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
      if( buf.length() > 0 ) {
	rv = buf.toString();
      }
    }
    return rv;
  }


  public static void openKCBasicProgram(
				ScreenFrm screenFrm,
				int       begAddr )
  {
    String text = getKCBasicProgram( screenFrm.getEmuThread(), begAddr );
    if( text != null ) {
      screenFrm.openText( text );
    } else {
      showNoKCBasic( screenFrm );
    }
  }


  public static void saveKCBasicProgram(
				ScreenFrm  screenFrm,
				int        begAddr )
  {
    int endAddr = getKCStyleBasicEndAddr( screenFrm.getEmuThread(), begAddr );
    if( endAddr >= begAddr ) {
      (new SaveDlg(
		screenFrm,
		begAddr - 0x41,
		endAddr,
		'B',
		true,		// KC-BASIC
		"KC-BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoKCBasic( screenFrm );
    }
  }


  public static void updKCBasicSysCells(
			EmuThread emuThread,
			int       begAddr,
			int       len,
			Object    fileFmt,
			int       fileType )
  {
    if( fileFmt != null ) {
      boolean state = false;
      if( ((begAddr == 0x2BC0) || (begAddr == 0x2C00) || (begAddr == 0x2C01))
	  && fileFmt.equals( FileInfo.HEADERSAVE )
	  && (fileType == 'B') )
      {
	state = true;
      }
      else if( ((begAddr == 0x0401) || (begAddr == 0x2C01))
	       && (fileFmt.equals( FileInfo.KCB )
			|| fileFmt.equals( FileInfo.KCBASIC_HEAD )
			|| fileFmt.equals( FileInfo.KCBASIC_PURE )
			|| fileFmt.equals( FileInfo.KCTAP_BASIC )) )
      {
	state = true;
      }
      if( state ) {
	int topAddr = begAddr + len;
	emuThread.setMemWord( begAddr - 42, topAddr );
	emuThread.setMemWord( begAddr - 40, topAddr );
	emuThread.setMemWord( begAddr - 38, topAddr );
      }
    }
  }


	/* --- private Methoden --- */

  private static void showNoKCBasic( Component owner )
  {
    BasicDlg.showErrorDlg(
	owner,
	"Es ist kein KC-BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

