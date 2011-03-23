/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speichern einer Datei
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;
import java.util.Arrays;
import jkcemu.Main;
import jkcemu.emusys.Z1013;
import z80emu.*;


public class FileSaver
{
  public static final String HEADERSAVE = "HEADERSAVE";
  public static final String KCC        = "KCC";
  public static final String KCTAP_0    = "KCTAP_0";
  public static final String KCTAP_1    = "KCTAP_1";
  public static final String KCBASIC    = "KCBASIC";
  public static final String RBASIC     = "RBASIC";
  public static final String INTELHEX   = "INTELHEX";
  public static final String BIN        = "BIN";


  public static void checkFileDesc(
			String  format,
			boolean kcbasic,
			String  fileDesc ) throws UserInputException
  {
    if( (format != null) && (fileDesc != null) ) {
      int m = getMaxFileDescLength( format, false );
      if( m > 0 ) {
	if( fileDesc.length() > m ) {
	  throw new UserInputException(
			"Die Bezeichnung der Datei ist zu lang (max. "
				+ String.valueOf( m ) + " Zeichen)." );
	}
      }
    }
  }


  public static String getFormatText( String format )
  {
    String rv = null;
    if( format != null ) {
      if( format.equals( HEADERSAVE ) ) {
	rv = "Headersave-Datei";
      }
      else if( format.equals( KCC ) ) {
	rv = "KCC-Datei";
      }
      else if( format.equals( KCTAP_0 ) ) {
	rv = "KC-TAP-Datei mit Block 0 (KC85/1, KC87, Z9001)";
      }
      else if( format.equals( KCTAP_1 ) ) {
	rv = "KC-TAP-Datei mit Block 1 (HC900, KC85/2-5, KC-BASIC)";
      }
      else if( format.equals( KCBASIC ) ) {
	rv = "KC-BASIC-Programmdatei";
      }
      else if( format.equals( RBASIC ) ) {
	rv = "RBASIC-Programmdatei";
      }
      else if( format.equals( INTELHEX ) ) {
	rv = "Intel-HEX-Datei";
      }
    }
    return rv != null ? rv : "Bin\u00E4rdatei ohne Kopfdaten";
  }


  public static int getMaxFileDescLength()
  {
    return 16;
  }


  public static int getMaxFileDescLength( String format, boolean kcbasic )
  {
    int rv = 0;
    if( format != null ) {
      if( format.equals( HEADERSAVE ) ) {
	rv = 16;
      } else if( format.equals( KCC ) && !kcbasic ) {
	rv = 11;
      } else if( format.equals( KCTAP_0 ) || format.equals( KCTAP_1 ) ) {
	rv = (kcbasic ? 8 : 11);
      }
    }
    return rv;
  }


  public static void saveFile(
			File       file,
			String     format,
			Z80MemView memory,
			int        begAddr,
			int        endAddr,
			boolean    kcbasic,
			boolean    rbasic,
			int        headBegAddr,
			Integer    headStartAddr,
			int        headFileType,
			String     headFileDesc,
			Z80Memory  memForHeader ) throws IOException
  {
    if( file != null ) {
      boolean isHS    = false;
      boolean isKCC   = false;
      boolean isTAP_0 = false;
      boolean isTAP_1 = false;
      boolean isSSS   = false;
      boolean isRBAS  = false;
      boolean isHEX   = false;
      boolean isBIN   = false;
      if( format != null ) {
	if( format.equals( HEADERSAVE ) ) {
	  isHS = true;
	} else if( format.equals( KCC ) ) {
	  isKCC = true;
	} else if( format.equals( KCTAP_0 ) ) {
	  isTAP_0 = true;
	} else if( format.equals( KCTAP_1 ) ) {
	  isTAP_1 = true;
	} else if( format.equals( KCBASIC ) ) {
	  isSSS = true;
	} else if( format.equals( RBASIC ) ) {
	  isRBAS = true;
	} else if( format.equals( INTELHEX ) ) {
	  isHEX = true;
	} else {
	  isBIN = true;
	}
      } else {
	isBIN = true;
      }
      BufferedOutputStream out = null;
      try {
	int    headEndAddr   = headBegAddr + endAddr - begAddr;
	byte[] hsHeaderBytes = null;

	out = new BufferedOutputStream( new FileOutputStream( file ) );
	if( isHS ) {
	  hsHeaderBytes       = new byte[ 32 ];
	  hsHeaderBytes[ 0 ]  = (byte) (headBegAddr & 0xFF);
	  hsHeaderBytes[ 1 ]  = (byte) ((headBegAddr >> 8) & 0xFF);
	  hsHeaderBytes[ 2 ]  = (byte) (headEndAddr & 0xFF);
	  hsHeaderBytes[ 3 ]  = (byte) ((headEndAddr >> 8) & 0xFF);
	  if( headStartAddr != null ) {
	    hsHeaderBytes[ 4 ] = (byte) (headStartAddr.intValue() & 0xFF);
	    hsHeaderBytes[ 5 ] = (byte) ((headStartAddr.intValue() >> 8)
								& 0xFF);
	  } else {
	    hsHeaderBytes[ 4 ] = (byte) 0;
	    hsHeaderBytes[ 5 ] = (byte) 0;
	  }
	  hsHeaderBytes[ 6 ]  = (byte) 'J';
	  hsHeaderBytes[ 7 ]  = (byte) 'K';
	  hsHeaderBytes[ 8 ]  = (byte) 'C';
	  hsHeaderBytes[ 9 ]  = (byte) 'E';
	  hsHeaderBytes[ 10 ] = (byte) 'M';
	  hsHeaderBytes[ 11 ] = (byte) 'U';
	  hsHeaderBytes[ 12 ] = (byte) headFileType;
	  hsHeaderBytes[ 13 ] = (byte) 0xD3;
	  hsHeaderBytes[ 14 ] = (byte) 0xD3;
	  hsHeaderBytes[ 15 ] = (byte) 0xD3;
	  int pos = 16;
	  if( headFileDesc != null ) {
	    int len = headFileDesc.length();
	    for( int i = 0; (i < len) && (pos < hsHeaderBytes.length); i++ ) {
	      hsHeaderBytes[ pos++ ] =
				(byte) (headFileDesc.charAt( i ) & 0xFF);
	    }
	  }
	  while( pos < hsHeaderBytes.length ) {
	    hsHeaderBytes[ pos++ ] = (byte) '\u0020';
	  }
	  out.write( hsHeaderBytes );

	  /*
	   * Wenn ein KC-BASIC-Programm in eine Headersave-Datei
	   * gespeichert wird, enthaelt die Datei auch einige Systemzellen
	   * des BASIC-Interpreters.
	   * Damit keine ungueltige Datei erzeugt wird,
	   * muss zuerst geprueft werden,
	   * ob diese Systemzellen initialisiert sind.
	   * Wenn nicht, werden Standardwerte verwendet,
	   * die denen eines BASIC-Interpreters
	   * nach einem Kaltstart entsprechen.
	   */
	  int addr = begAddr;
	  if( kcbasic ) {
	    int endOfMem    = memory.getMemWord( begAddr + 0x04 );
	    int begOfVars   = memory.getMemWord( begAddr + 0x17 );
	    int begOfFields = memory.getMemWord( begAddr + 0x19 );
	    int topAddr     = memory.getMemWord( begAddr + 0x1B );
	    if( (endOfMem < begAddr) || (endOfMem >= Z1013.MEM_SCREEN)
		|| (begOfVars < begAddr) || (begOfVars > endOfMem)
		|| (begOfFields < begAddr) || (begOfFields > endOfMem)
		|| (topAddr < begAddr) || (topAddr > endOfMem) )
	    {
	      // Systemzellen sind nicht initialisiert
	      int hBegAddr = (begAddr >> 8) & 0xFF;
	      int hTopAddr = ((endAddr + 1) >> 8) & 0xFF;
	      int lTopAddr = (endAddr + 1) & 0xFF;

	      byte[] sysBytes = new byte[ 0x41 ];
	      Arrays.fill( sysBytes, (byte) 0 );
	      sysBytes[ 0 ] = (byte) 0x03;
	      if( addr < 0x2B00 ) {
		sysBytes[ 2 ] = (byte) 0x1D;
		sysBytes[ 3 ] = (byte) 0xC3;
		sysBytes[ 4 ] = (byte) 0xFF;
		sysBytes[ 5 ] = (byte) 0xBF;
	      } else {
		sysBytes[ 2 ] = (byte) 0x1D;
		sysBytes[ 3 ] = (byte) 0x06;
		sysBytes[ 4 ] = (byte) 0xFF;
		sysBytes[ 5 ] = (byte) 0xE7;
	      }
	      sysBytes[ 0x10 ] = (byte) (hBegAddr + 1);
	      sysBytes[ 0x17 ] = (byte) lTopAddr;
	      sysBytes[ 0x18 ] = (byte) hTopAddr;
	      sysBytes[ 0x19 ] = (byte) lTopAddr;
	      sysBytes[ 0x1A ] = (byte) hTopAddr;
	      sysBytes[ 0x1B ] = (byte) lTopAddr;
	      sysBytes[ 0x1C ] = (byte) hTopAddr;
	      sysBytes[ 0x1E ] = (byte) (hBegAddr + 1);
	      sysBytes[ 0x25 ] = (byte) 0xB4;
	      sysBytes[ 0x26 ] = (byte) hBegAddr;
	      sysBytes[ 0x28 ] = (byte) 0x30;
	      sysBytes[ 0x29 ] = (byte) 0xF4;
	      sysBytes[ 0x31 ] = (byte) 0x30;
	      sysBytes[ 0x3C ] = (byte) 0xAF;
	      out.write( sysBytes );
	      addr += sysBytes.length;
	    }
	  }
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr, false ) );
	    addr++;
	  }
	  if( isHS && (memForHeader != null) ) {
	    for( int i = 0; i < hsHeaderBytes.length; i++ ) {
	      memForHeader.setMemByte(
				Z1013.MEM_HEAD + i,
				hsHeaderBytes[ i ] );
	    }
	  }

	} else if( isKCC ) {
	  writeKCHeader(
			out,
			headBegAddr,
			headEndAddr,
			headStartAddr,
			headFileDesc );
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr, false ) );
	    addr++;
	  }

	} else if( isTAP_0 || isTAP_1 ) {
	  String s = "\u00C3KC-TAPE by AF.\u0020";
	  int    n = s.length();
	  for( int i = 0; i < n; i++ ) {
	    out.write( s.charAt( i ) );
	  }
	  int blkNum = isTAP_0 ? 0 : 1;
	  if( kcbasic ) {
	    out.write( blkNum++ );
	    out.write( 0xD3 );
	    out.write( 0xD3 );
	    out.write( 0xD3 );

	    n = 125;		// 3x 0xD3 bereits abgezogen
	    if( headFileDesc != null ) {
	      int len = headFileDesc.length();
	      int pos = 0;
	      while( (n > 117) && (pos < len) ) {
		char ch = headFileDesc.charAt( pos++ );
		if( (ch >= '\u0020') && (ch <= 0xFF) ) {
		  out.write( ch );
		  --n;
		}
	      }
	    }
	    while( n > 117 ) {
	      out.write( 0x20 );
	      --n;
	    }

	    int addr = begAddr + 65;
	    int len  = endAddr - addr + 1;
	    out.write( len & 0xFF );
	    out.write( (len >> 8) & 0xFF );
	    n -= 2;

	    while( addr <= endAddr ) {
	      if( n == 0 ) {
		out.write( blkNum++ );
		n = 128;
	      }
	      out.write( memory.getMemByte( addr, false ) );
	      addr++;
	      --n;
	    }
	    if( n == 0 ) {
	      out.write( blkNum++ );
	      n = 128;
	    }
	    out.write( 3 );
	    --n;

	    while( n > 0 ) {
	      out.write( 0 );
	      --n;
	    }
	  } else {
	    out.write( blkNum++ );
	    writeKCHeader(
			out,
			headBegAddr,
			headEndAddr,
			headStartAddr,
			headFileDesc );
	    int addr = begAddr;
	    n        = 0;
	    while( addr <= endAddr ) {
	      if( n == 0 ) {
		out.write( (addr + 128) > endAddr ? 0xFF : blkNum++ );
		n = 128;
	      }
	      out.write( memory.getMemByte( addr++, false ) );
	      --n;
	    }
	    while( n > 0 ) {
	      out.write( 0 );
	      --n;
	    }
	  }

	} else if( isSSS ) {
	  int addr = begAddr + 65;
	  int len  = endAddr - addr + 1;
	  out.write( len & 0xFF );
	  out.write( (len >> 8) & 0xFF );
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr, false ) );
	    addr++;
	  }
	  out.write( 3 );

	} else if( isRBAS ) {
	  out.write( 0xFF );
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr, false ) );
	    addr++;
	  }
	  out.write( 0x1A );

	} else if( isHEX ) {
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    int cnt = writeHexSegment(
				out,
				memory,
				addr,
				endAddr,
				headBegAddr );
	    addr += cnt;
	    headBegAddr += cnt;
	  }
	  out.write( ':' );
	  writeHexByte( out, 0 );
	  writeHexByte( out, 0 );
	  writeHexByte( out, 0 );
	  writeHexByte( out, 1 );
	  writeHexByte( out, 0xFF );
	  out.write( 0x0D );
	  out.write( 0x0A );

	} else {

	  // BIN-Datei
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr, false ) );
	    addr++;
	  }
	}
	out.close();
	out   = null;
      }
      finally {
	EmuUtil.doClose( out );
      }
    }
  }


	/* --- private Methoden --- */

  private static void writeHexByte(
				OutputStream out,
				int          value ) throws IOException
  {
    out.write( EmuUtil.getHexChar( value >> 4 ) );
    out.write( EmuUtil.getHexChar( value ) );
  }


  /*
   * Die Methode schreibt ein Datensegment im Intel-Hex-Format.
   *
   * Rueckabewert: Anzahl der geschriebenen Bytes
   */
  private static int writeHexSegment(
				OutputStream out,
				Z80MemView   memory,
				int          addr,
				int          endAddr,
				int          headBegAddr ) throws IOException
  {
    int cnt = 0;
    if( (addr >= 0) && (addr <= endAddr) ) {
      cnt = endAddr - addr + 1;
      if( cnt > 32 ) {
	cnt = 32;
      }
      out.write( ':' );
      writeHexByte( out, cnt );

      int hHeadBegAddr = headBegAddr >> 8;
      writeHexByte( out, hHeadBegAddr );
      writeHexByte( out, headBegAddr );
      writeHexByte( out, 0 );

      int cks = (cnt & 0xFF) + (hHeadBegAddr & 0xFF) + (headBegAddr & 0xFF);
      for( int i = 0; i < cnt; i++ ) {
	int b = memory.getMemByte( addr++, false );
	writeHexByte( out, b );
	cks += b;
      }
      writeHexByte( out, 0 - cks );
      out.write( 0x0D );
      out.write( 0x0A );
    }
    return cnt;
  }


  private static void writeKCHeader(
				OutputStream out,
				int          begAddr,
				int          endAddr,
				Integer      startAddr,
				String       fileDesc ) throws IOException
  {
    int n   = 11;
    int src = 0;
    if( fileDesc != null ) {
      int len = fileDesc.length();
      while( (src < len) && (n > 0) ) {
        char ch = fileDesc.charAt( src++ );
        if( (ch >= '\u0020') && (ch <= 0xFF) ) {
          out.write( ch );
          --n;
        }
      }
    }
    while( n > 0 ) {
      out.write( '\u0020' );
      --n;
    }
    for( int i = 0; i < 5; i++ ) {
      out.write( 0 );
    }
    out.write( startAddr != null ? 3 : 2 );
    out.write( begAddr & 0xFF );
    out.write( begAddr >> 8 );
    endAddr++;
    out.write( endAddr & 0xFF );
    out.write( endAddr >> 8 );
    if( startAddr != null ) {
      out.write( startAddr.intValue() & 0xFF );
      out.write( startAddr.intValue() >> 8 );
    } else {
      out.write( 0 );
      out.write( 0 );
    }
    for( int i = 0; i < 105; i++ ) {
      out.write( 0 );
    }
  }
}

