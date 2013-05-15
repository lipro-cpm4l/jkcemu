/*
 * (c) 2008-2013 Jens Mueller
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
  public static String getEDAS4Text(
				Z80MemView memory,
				int        addr )
  {
    String rv      = null;
    int    endAddr = addr + memory.getMemWord( addr ) - 1;
    if( endAddr >= 6 ) {
      if( (memory.getMemByte( endAddr, false  ) == 0xFF)
	  && (memory.getMemByte( endAddr - 1, false ) == 0) )
      {
	StringBuilder buf = new StringBuilder( 0x4000 );
	addr += 7;
	while( addr < endAddr ) {
	  int b = memory.getMemByte( addr++, false );
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


  public static int getKCBasicStyleEndAddr( Z80MemView memory, int begAddr )
  {
    int endAddr      = -1;
    int curLineAddr  = begAddr;
    int nextLineAddr = memory.getMemWord( curLineAddr );
    while( (nextLineAddr > curLineAddr + 5)
	   && (memory.getMemByte( nextLineAddr - 1, false ) == 0) )
    {
      curLineAddr  = nextLineAddr;
      nextLineAddr = memory.getMemWord( curLineAddr );
    }
    if( curLineAddr > begAddr ) {
      endAddr = curLineAddr + 2;
    }
    return endAddr;
  }


  public static String getKCBasicStyleProgram(
				Z80MemView memory,
				int        addr,
				String[]   tokens )
  {
    StringBuilder buf = new StringBuilder( 0x4000 );

    int nextLineAddr = memory.getMemWord( addr );
    while( (nextLineAddr > addr + 5)
	   && (memory.getMemByte( nextLineAddr - 1, false ) == 0) )
    {
      // Zeilennummer
      addr += 2;
      buf.append( memory.getMemWord( addr ) );
      addr += 2;

      // Anzahl Leerzeichen vor der Anweisung ermitteln
      boolean sep = true;
      int     n   = 0;
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr, false );
	if( ch == '\u0020' ) {
	  n++;
	  addr++;
	} else {
	  if( (ch != 0) && (n > 0) ) {
	    for( int i = 0; i <= n; i++ ) {
	      buf.append( (char) '\u0020' );
	    }
	    sep = false;
	  }
	  break;
	}
      }

      // Programmzeile extrahieren
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr++, false );
	if( ch == 0 ) {
	  break;
	}
	if( ch == '\"' ) {
	  if( sep ) {
	    buf.append( (char) '\u0020' );
	  }
	  buf.append( (char) ch );
	  while( addr < nextLineAddr ) {
	    ch = memory.getMemByte( addr++, false );
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
	    if( (pos >= 0) && (pos < tokens.length) ) {
	      String s = tokens[ pos ];
	      if( s != null ) {
		int len = s.length();
		if( len > 0 ) {
		  if( isIdentifierChar( buf.charAt( buf.length() - 1 ) )
		      && isIdentifierChar( s.charAt( 0 ) ) )
		  {
		    buf.append( (char) '\u0020' );
		  }
		  buf.append( s );
		  if( isIdentifierChar( s.charAt( len - 1 ) ) ) {
		    sep = true;
		  } else {
		    sep = false;
		  }
		}
		ch = 0;
	      }
	    }
	  }
	  if( ch > 0 ) {
	    if( sep
		&& (isIdentifierChar( ch ) || (ch == '\'') || (ch == '\"')) )
	    {
	      buf.append( (char) '\u0020' );
	    }
	    buf.append( (char) ch );
	    sep = false;
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
	  int ch = memory.getMemByte( addr, false );
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
	  int ch = memory.getMemByte( addr++, false );
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


  public static void openKCBasicStyleProgram(
				ScreenFrm screenFrm,
				int       begAddr,
				String[]  tokens )
  {
    if( begAddr == 0x0401 ) {
      int tmpAddr = screenFrm.getEmuThread().getMemWord( 0x035F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    } else if( begAddr == 0x2C01 ) {
      int tmpAddr = screenFrm.getEmuThread().getMemWord( 0x2B5F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    }
    String text = getKCBasicStyleProgram(
				screenFrm.getEmuThread(),
				begAddr,
				tokens );
    if( text != null ) {
      Component owner = screenFrm.openText( text );
      if( (owner != null) && (begAddr != 0x0401) && (begAddr != 0x2C01) ) {
	BasicDlg.showInfoDlg(
		owner,
		"Das BASIC-Programm befindet sich au\u00DFerhalb\n"
			+ "des standardm\u00E4\u00DFigen Adressbereichs." );
      }
    } else {
      showNoKCBasic( screenFrm );
    }
  }


  public static void saveKCBasicStyleProgram(
				ScreenFrm screenFrm,
				int       begAddr )
  {
    if( begAddr == 0x0401 ) {
      int tmpAddr = screenFrm.getEmuThread().getMemWord( 0x035F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    } else if( begAddr == 0x2C01 ) {
      int tmpAddr = screenFrm.getEmuThread().getMemWord( 0x2B5F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    }
    int endAddr = getKCBasicStyleEndAddr( screenFrm.getEmuThread(), begAddr );
    if( endAddr >= begAddr ) {
      if( (begAddr == 0x0401) || (begAddr == 0x2C01) ) {
	(new SaveDlg(
		screenFrm,
		begAddr - 0x41,
		endAddr,
		'B',
		true,		// KC-BASIC
		false,		// kein RBASIC
		"KC-BASIC-Programm speichern" )).setVisible( true );
      } else {
	BasicDlg.showErrorDlg(
		screenFrm,
		"Es ist zwar ein BASIC-Programm vorhanden, jedoch befindet\n"
			+ "es sich au\u00DFerhalb des"
			+ " standardm\u00E4\u00DFigen Adressbereichs.\n"
			+ "Es kann deshalb nicht auf diese Art und Weise"
			+ " gespeichert werden." );
      }
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
			|| fileFmt.equals( FileInfo.KCBASIC_HEAD_PRG )
			|| fileFmt.equals( FileInfo.KCBASIC_PRG )
			|| fileFmt.equals( FileInfo.KCTAP_BASIC_PRG )) )
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

  private static boolean isIdentifierChar( int ch )
  {
    return ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'))
		|| ((ch >= '0') && (ch <= '9'));
  }


  private static void showNoKCBasic( Component owner )
  {
    BasicDlg.showErrorDlg(
	owner,
	"Es ist kein KC-BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

