/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speichern einer Datei
 */

package jkcemu.base;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.*;
import java.util.Arrays;
import jkcemu.Main;
import jkcemu.emusys.Z1013;
import z80emu.Z80Memory;


public class FileSaver
{
  public static void saveFile(
			File       file,
			FileFormat fmt,
			EmuMemView memory,
			int        begAddr,
			int        endAddr,
			boolean    basic,
			int        headBegAddr,
			Integer    headStartAddr,
			String     headFileDesc,
			String     headFileType,
			Z80Memory  memForHeader ) throws IOException
  {
    if( file != null ) {
      BufferedOutputStream out = null;
      try {
	int    headEndAddr   = headBegAddr + endAddr - begAddr;
	byte[] hsHeaderBytes = null;

	out = new BufferedOutputStream( new FileOutputStream( file ) );
	if( fmt.equals( FileFormat.HEADERSAVE ) ) {
	  int typeChar = 0x20;
	  if( headFileType != null ) {
	    if( !headFileType.isEmpty() ) {
	      char ch = headFileType.charAt( 0 );
	      if( (ch >= '\u0020') && (ch < '\u007F') ) {
		typeChar = ch;
	      }
	    }
	  }
	  hsHeaderBytes      = new byte[ 32 ];
	  hsHeaderBytes[ 0 ] = (byte) (headBegAddr & 0xFF);
	  hsHeaderBytes[ 1 ] = (byte) ((headBegAddr >> 8) & 0xFF);
	  hsHeaderBytes[ 2 ] = (byte) (headEndAddr & 0xFF);
	  hsHeaderBytes[ 3 ] = (byte) ((headEndAddr >> 8) & 0xFF);
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
	  hsHeaderBytes[ 12 ] = (byte) typeChar;
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
	  if( (typeChar == 'B')
	      && ((begAddr == 0x0401) || (begAddr == 0x2C01)) )
	  {
	    begAddr -= 0x0041;
	    int endOfMem = EmuUtil.getBasicMemWord(
					memory, begAddr + 0x04 );
	    int begOfVars = EmuUtil.getBasicMemWord(
					memory, begAddr + 0x17 );
	    int begOfFields = EmuUtil.getBasicMemWord(
					memory, begAddr + 0x19 );
	    int topAddr = EmuUtil.getBasicMemWord( memory, begAddr + 0x1B );
	    if( (endOfMem < begAddr) || (endOfMem >= 0xFF00)
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
	    while( addr <= endAddr ) {
	      out.write( memory.getBasicMemByte( addr ) );
	      addr++;
	    }
	  } else {
	    while( addr <= endAddr ) {
	      out.write( getMemByte( memory, addr++, basic ) );
	    }
	  }
	  int n = (addr - begAddr) % 0x20;
	  if( n > 0 ) {
	    for( int i = n; i < 0x20; i++ ) {
	      out.write( 0 );
	    }
	  }
	  if( fmt.equals( FileFormat.HEADERSAVE )
	      && (memForHeader != null) )
	  {
	    for( int i = 0; i < hsHeaderBytes.length; i++ ) {
	      memForHeader.setMemByte(
				Z1013.MEM_HEAD + i,
				hsHeaderBytes[ i ] );
	    }
	  }
	}
	else if( fmt.equals( FileFormat.KCC ) ) {
	  writeKCHeader(
			out,
			headBegAddr,
			headEndAddr,
			headStartAddr,
			false,
			headFileDesc,
			null );
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( getMemByte( memory, addr++, basic ) );
	  }
	  int n = (addr - begAddr) % 0x80;
	  if( n > 0 ) {
	    for( int i = n; i < 0x80; i++ ) {
	      out.write( 0 );
	    }
	  }
	}
	else if( fmt.equals( FileFormat.KCTAP_KC85 )
		 || fmt.equals( FileFormat.KCTAP_Z9001 )
		 || fmt.equals( FileFormat.KCTAP_BASIC_PRG ) )
	{
	  String s = FileInfo.KCTAP_MAGIC;
	  int    n = s.length();
	  for( int i = 0; i < n; i++ ) {
	    out.write( s.charAt( i ) );
	  }
	  boolean z9001  = fmt.equals( FileFormat.KCTAP_Z9001 );
	  int     blkNum = (z9001 ? 0 : 1);
	  if( fmt.equals( FileFormat.KCTAP_BASIC_PRG ) ) {
	    blkNum = 1;
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

	    int addr = begAddr;
	    int len  = endAddr - addr + 1;
	    out.write( len & 0xFF );
	    out.write( (len >> 8) & 0xFF );
	    n -= 2;

	    while( addr <= endAddr ) {
	      if( n == 0 ) {
		if( (addr + 128) <= endAddr ) {
		  out.write( blkNum++ );
		} else {
		  out.write( 0xFF );
		}
		n = 128;
	      }
	      out.write( memory.getBasicMemByte( addr ) );
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
			z9001,
			headFileDesc,
			headFileType );
	    int addr = begAddr;
	    n        = 0;
	    while( addr <= endAddr ) {
	      if( n == 0 ) {
		out.write( (addr + 128) > endAddr ? 0xFF : blkNum++ );
		n = 128;
	      }
	      out.write( getMemByte( memory, addr++, basic ) );
	      --n;
	    }
	    while( n > 0 ) {
	      out.write( 0 );
	      --n;
	    }
	  }
	} else if( fmt.equals( FileFormat.KCBASIC_PRG ) ) {
	  int addr = begAddr;
	  int len  = endAddr - addr + 1;
	  int n    = 2;
	  out.write( len & 0xFF );
	  out.write( (len >> 8) & 0xFF );
	  while( addr <= endAddr ) {
	    out.write( memory.getBasicMemByte( addr ) );
	    n++;
	    addr++;
	  }
	  out.write( 3 );
	  n = (n + 1) % 0x80;
	  if( n > 0 ) {
	    for( int i = n; i < 0x80; i++ ) {
	      out.write( 0 );
	    }
	  }
	}
	else if( fmt.equals( FileFormat.RBASIC_PRG ) ) {
	  out.write( 0xFF );
	  int n    = 1;
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( memory.getBasicMemByte( addr ) );
	    n++;
	    addr++;
	  }
	  out.write( 0x1A );
	  n = (n + 1) % 0x80;
	  if( n > 0 ) {
	    for( int i = n; i < 0x80; i++ ) {
	      out.write( 0 );
	    }
	  }
	}
	else if( fmt.equals( FileFormat.RMC ) ) {
	  out.write( 0xFE );
	  out.write( begAddr & 0xFF );
	  out.write( begAddr >> 8 );
	  out.write( endAddr & 0xFF );
	  out.write( endAddr >> 8 );
	  if( headStartAddr != null ) {
	    out.write( headStartAddr.intValue() & 0xFF );
	    out.write( headStartAddr.intValue() >> 8 );
	  } else {
	    out.write( 0 );
	    out.write( 0 );
	  }
	  int n    = 7;
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( getMemByte( memory, addr++, basic ) );
	    n++;
	  }
	  n %= 0x80;
	  if( n > 0 ) {
	    for( int i = n; i < 0x80; i++ ) {
	      out.write( 0 );
	    }
	  }
	}
	else if( fmt.equals( FileFormat.INTELHEX ) ) {
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    int cnt = writeHexSegment(
				out,
				memory,
				addr,
				endAddr,
				basic,
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

	  // BASIC-Programm- und BIN-Datei
	  int addr = begAddr;
	  while( addr <= endAddr ) {
	    out.write( getMemByte( memory, addr++, basic ) );
	  }
	}
	out.close();
	out = null;
      }
      finally {
	EmuUtil.closeSilent( out );
      }
    }
  }


  public static void writeKCHeader(
				OutputStream out,
				int          begAddr,
				int          endAddr,
				Integer      startAddr,
				boolean      z9001,
				String       fileDesc,
				String       fileType ) throws IOException
  {
    if( z9001 ) {
      EmuUtil.writeFixLengthASCII( out, fileDesc, 8, 0 );
      EmuUtil.writeFixLengthASCII( out, fileType, 3, 0 );
    } else {
      EmuUtil.writeFixLengthASCII( out, fileDesc, 11, 0x20 );
    }
    for( int i = 0; i < 5; i++ ) {
      out.write( 0 );
    }
    out.write( startAddr != null ? 3 : 2 );
    out.write( begAddr );
    out.write( begAddr >> 8 );
    if( !z9001 ) {
      endAddr++;
    }
    out.write( endAddr );
    out.write( endAddr >> 8 );
    if( startAddr != null ) {
      out.write( startAddr.intValue() );
      out.write( startAddr.intValue() >> 8 );
    } else {
      out.write( 0 );
      out.write( 0 );
    }
    for( int i = 0; i < 105; i++ ) {
      out.write( 0 );
    }
  }


	/* --- private Methoden --- */

  private static int getMemByte( EmuMemView memory, int addr, boolean basic )
  {
    int b = 0;
    if( basic ) {
      b = memory.getBasicMemByte( addr );
    } else {
      b = memory.getMemByte( addr, false );
    }
    return b;
  }


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
				EmuMemView   memory,
				int          addr,
				int          endAddr,
				boolean      basic,
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
	int b = getMemByte( memory, addr++, basic );
	writeHexByte( out, b );
	cks += b;
      }
      writeHexByte( out, 0 - cks );
      out.write( 0x0D );
      out.write( 0x0A );
    }
    return cnt;
  }
}
