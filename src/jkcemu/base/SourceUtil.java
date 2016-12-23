/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen zum Behandeln von Quelltexten
 */

package jkcemu.base;

import java.awt.Component;
import java.lang.*;


public class SourceUtil
{
  public static String getEDAS4Text(
				EmuMemView memory,
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


  public static int getBasicEndAddr( EmuMemView memory, int begAddr )
  {
    int endAddr      = -1;
    int curLineAddr  = begAddr;
    int nextLineAddr = EmuUtil.getBasicMemWord( memory, curLineAddr );
    while( (nextLineAddr > curLineAddr + 5)
	   && (memory.getBasicMemByte( nextLineAddr - 1 ) == 0) )
    {
      curLineAddr  = nextLineAddr;
      nextLineAddr = EmuUtil.getBasicMemWord( memory, curLineAddr );
    }
    if( curLineAddr > begAddr ) {
      endAddr = curLineAddr + 1;
    }
    return endAddr;
  }


  public static String getBasicProgram(
				EmuMemView memory,
				int        addr,
				String[]   tokens )
  {
    return getBasicProgram( memory, addr, tokens, null );
  }


  public static String getBasicProgram(
				EmuMemView memory,
				int        addr,
				String[]   tokens,
				String[]   tokensFF )
  {
    StringBuilder buf = new StringBuilder( 0x4000 );

    int nextLineAddr = EmuUtil.getBasicMemWord( memory, addr );
    while( (nextLineAddr > addr + 5)
	   && (memory.getBasicMemByte( nextLineAddr - 1 ) == 0) )
    {
      // Zeilennummer
      addr += 2;
      buf.append( EmuUtil.getBasicMemWord( memory, addr ) );
      addr += 2;

      // Anzahl Leerzeichen vor der Anweisung ermitteln
      boolean sep = true;
      int     n   = 0;
      while( addr < nextLineAddr ) {
	int ch = memory.getBasicMemByte( addr );
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
	int b = memory.getBasicMemByte( addr++ );
	if( b == 0 ) {
	  break;
	}
	if( b == '\"' ) {
	  if( sep ) {
	    buf.append( (char) '\u0020' );
	  }
	  buf.append( (char) b );
	  while( addr < nextLineAddr ) {
	    b = memory.getBasicMemByte( addr++ );
	    if( b == 0 ) {
	      break;
	    }
	    buf.append( (char) b );
	    if( b == '\"' ) {
	      break;
	    }
	  }
	} else {
	  String[] tmpTokens = tokens;
	  if( b == 0xFF ) {
	    b         = memory.getBasicMemByte( addr++ );
	    tmpTokens = tokensFF;
	  }
	  if( b >= 0x80 ) {
	    int pos = b - 0x80;
	    if( (pos >= 0) && (pos < tmpTokens.length) ) {
	      String s = tmpTokens[ pos ];
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
		b = 0;
	      }
	    }
	  }
	  if( b > 0 ) {
	    if( sep
		&& (isIdentifierChar( b ) || (b == '\'') || (b == '\"')) )
	    {
	      buf.append( (char) '\u0020' );
	    }
	    buf.append( (char) b );
	    sep = false;
	  }
	}
      }
      buf.append( (char) '\n' );

      // naechste Zeile
      addr         = nextLineAddr;
      nextLineAddr = EmuUtil.getBasicMemWord( memory, addr );
    }
    return buf.length() > 0 ? buf.toString() : null;
  }


  public static String getTinyBasicProgram(
				EmuMemView memory,
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
	buf.append( EmuUtil.getBasicMemWord( memory, addr ) );
	addr += 2;

	// Anzahl Leerzeichen vor der Anweisung ermitteln
	int n = 0;
	while( addr < endAddr ) {
	  int ch = memory.getBasicMemByte( addr );
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
	  int ch = memory.getBasicMemByte( addr++ );
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
				int       begAddr,
				String[]  tokens )
  {
    EmuMemView memory = screenFrm.getEmuThread();
    if( begAddr == 0x0401 ) {
      int tmpAddr = EmuUtil.getBasicMemWord( memory, 0x035F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    } else if( begAddr == 0x2C01 ) {
      int tmpAddr = EmuUtil.getBasicMemWord( memory, 0x2B5F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    }
    String text = getBasicProgram( memory, begAddr, tokens );
    if( text != null ) {
      Component owner = screenFrm.openText( text );
      if( (owner != null) && (begAddr != 0x0401) && (begAddr != 0x2C01) ) {
	BaseDlg.showInfoDlg(
		owner,
		"Das BASIC-Programm befindet sich au\u00DFerhalb\n"
			+ "des standardm\u00E4\u00DFigen Adressbereichs." );
      }
    } else {
      showNoBasic( screenFrm );
    }
  }


  public static void saveKCBasicProgram(
				ScreenFrm screenFrm,
				int       begAddr )
  {
    EmuMemView memory = screenFrm.getEmuThread();
    if( begAddr == 0x0401 ) {
      int tmpAddr = EmuUtil.getBasicMemWord( memory, 0x035F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    } else if( begAddr == 0x2C01 ) {
      int tmpAddr = EmuUtil.getBasicMemWord( memory, 0x2B5F );
      if( tmpAddr > 0 ) {
	begAddr = tmpAddr;
      }
    }
    int endAddr = getBasicEndAddr( memory, begAddr );
    if( endAddr >= begAddr ) {
      if( (begAddr == 0x0401) || (begAddr == 0x2C01) ) {
	(new SaveDlg(
		screenFrm,
		begAddr,
		endAddr,
		"KC-BASIC-Programm speichern",
		SaveDlg.BasicType.KCBASIC,
		EmuUtil.getKCBasicFileFilter() )).setVisible( true );
      } else {
	BaseDlg.showErrorDlg(
		screenFrm,
		"Es ist zwar ein BASIC-Programm vorhanden, jedoch befindet\n"
			+ "es sich au\u00DFerhalb des"
			+ " standardm\u00E4\u00DFigen Adressbereichs.\n"
			+ "Es kann deshalb nicht auf diese Art und Weise"
			+ " gespeichert werden." );
      }
    } else {
      showNoBasic( screenFrm );
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
      int basicBegAddr = -1;
      if( ((fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
			    || fileFmt.equals( FileFormat.KCBASIC_PRG )
			    || fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
			    || fileFmt.equals( FileFormat.BASIC_PRG ))
			&& (begAddr == 0x0401)
			&& (len > 7))
	  || (fileFmt.equals( FileFormat.HEADERSAVE )
                        && (fileType == 'B')
                        && (begAddr <= 0x0401)
                        && ((begAddr + len) > 0x0407)) )
      {
	basicBegAddr = 0x0401;
      }
      else if( ((fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
			    || fileFmt.equals( FileFormat.KCBASIC_PRG )
			    || fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
			    || fileFmt.equals( FileFormat.BASIC_PRG ))
			&& (begAddr == 0x2C01)
			&& (len > 7))
	       || (fileFmt.equals( FileFormat.HEADERSAVE )
			&& (fileType == 'B')
			&& (begAddr <= 0x2C01)
			&& ((begAddr + len) > 0x2C07)) )
      {
	basicBegAddr = 0x2C01;
      }
      if( basicBegAddr >= 0 ) {
	int topAddr = getBasicEndAddr( emuThread, basicBegAddr );
	if( topAddr > basicBegAddr ) {
	  emuThread.setBasicMemWord( begAddr - 42, topAddr );
	  emuThread.setBasicMemWord( begAddr - 40, topAddr );
	  emuThread.setBasicMemWord( begAddr - 38, topAddr );
	}
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


  private static void showNoBasic( Component owner )
  {
    BaseDlg.showErrorDlg(
	owner,
	"Es ist kein BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

