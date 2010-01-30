/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber eine Datei
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;


public class FileInfo
{
  public static final String HEADERSAVE   = "Headersave-Datei";
  public static final String KCB          = "KCB-Datei (BASIC-Programm)";
  public static final String KCC          = "KCC/JTC-Datei";
  public static final String KCTAP_SYS    = "KC-TAP-Datei";
  public static final String KCTAP_BASIC  = "KC-TAP-BASIC-Programmdatei";
  public static final String KCTAP_HEADER = "\u00C3KC-TAPE by AF.\u0020";
  public static final String KCBASIC_HEAD =
				"KC-BASIC-Programmdatei mit Kopfdaten";
  public static final String KCBASIC_PURE = "KC-BASIC-Programmdatei";
  public static final String INTELHEX = "Intel-HEX-Datei";
  public static final String BIN = "BIN-Datei (Speicherabbild ohne Kopfdaten)";

  public static final int KCTAP_HLEN = KCTAP_HEADER.length();

  private byte[] header;
  private long   fileLen;
  private int    fileType;
  private String fileFmt;
  private String fileText;
  private String fileDesc;
  private String addrText;
  private String infoText;
  private int    nextTAPOffs;


  public static FileInfo analyzeFile( File file )
  {
    FileInfo rv = null;
    if( file != null ) {
      try {
	long fileLen = file.length();
	if( fileLen > 0 ) {
	  InputStream in = null;
	  try {
	    in = new FileInputStream( file );

	    byte[] header = new byte[ 40 ];
	    rv = analyzeFile( header, EmuUtil.read( in, header ), file );
	  }
	  finally {
	    if( in != null ) {
	      try {
		in.close();
	      }
	      catch( IOException ex ) {}
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
    return rv;
  }


  public static FileInfo analyzeFile( byte[] header, File file )
  {
    return analyzeFile( header, header.length, file );
  }


  public static FileInfo analyzeFile(
				byte[] header,
				int    headerLen,
				File   file )
  {
    FileInfo rv = null;
    if( header != null ) {
      if( headerLen > header.length ) {
	headerLen = header.length;
      }
      String fileFmt     = null;
      String fileText    = null;
      int    fileType    = -1;
      int    begAddr     = -1;
      int    endAddr     = -1;
      int    nextTAPOffs = -1;
      long   fileLen     = headerLen;
      if( file != null ) {
	fileLen = file.length();
      }
      if( (fileLen > 32) && (headerLen > 15) ) {
	int b12 = (int) header[ 12 ] & 0xFF;
	int b13 = (int) header[ 13 ] & 0xFF;
	int b14 = (int) header[ 14 ] & 0xFF;
	int b15 = (int) header[ 15 ] & 0xFF;
	if( (b13 == 0xD3) && (b14 == 0xD3) && (b15 == 0xD3) ) {
	  fileFmt = HEADERSAVE;
	  if( (b12 >= 0x20) && (b12 < 0x7F) ) {
	    fileType = b12;
	  }
	}
      }
      if( (fileFmt == null) && (fileLen > 144) && (headerLen > 33) ) {
	if( isTAPHeaderAt( header, headerLen, 0 ) ) {
	  int nextOffs = -1;
	  int b17      = header[ 17 ] & 0xFF;
	  int b18      = header[ 18 ] & 0xFF;
	  int b19      = header[ 19 ] & 0xFF;
	  int b33      = header[ 33 ] & 0xFF;
	  if( ((b17 < 0xD3) || (b17 > 0xD8))
	      && ((b18 < 0xD3) || (b18 > 0xD8))
	      && ((b19 < 0xD3) || (b19 > 0xD8))
	      && ((b33 >= 2) && (b33 <= 4)) )
	  {
	    fileFmt = KCTAP_SYS;
	    begAddr = getBegAddr( header, fileFmt );
	    endAddr = getEndAddr( header, fileFmt );
	    if( (begAddr >= 0) && (begAddr <= endAddr) ) {
	      int nBlks = (endAddr - begAddr + 127) / 128;
	      nextOffs  = 16 + (129 * (nBlks + 1));
	    }
	  } else {
	    if( ((b17 == 0xD3) && (b18 == 0xD3) && (b19 == 0xD3))
		|| ((b17 == 0xD6) && (b18 == 0xD6) && (b19 == 0xD6)) )
	    {
	      fileFmt = KCTAP_BASIC;
	      begAddr = getBegAddr( header, fileFmt );
	      endAddr = getEndAddr( header, fileFmt );
	      if( (begAddr >= 0) && (begAddr <= endAddr) ) {
		int nBlks = (endAddr - begAddr + 13 + 127) / 128;
		nextOffs  = 16 + (129 * nBlks);
	      }
	    }
	  }
	  if( nextOffs > 0 ) {
	    if( isTAPHeaderAt( header, headerLen, nextOffs ) ) {
	      nextTAPOffs = nextOffs;
	    }
	  }
	}
      }
      if( (fileFmt == null) && (fileLen > 20) && (headerLen > 2) ) {
	int b0 = header[ 0 ] & 0xFF;
	int b1 = header[ 1 ] & 0xFF;
	int b2 = header[ 2 ] & 0xFF;
	if( ((b0 == 0xD3) && (b1 == 0xD3) && (b2 == 0xD3))
	    || ((b0 == 0xD6) && (b1 == 0xD6) && (b2 == 0xD6)) )
	{
	  fileFmt = KCBASIC_HEAD;
	}
      }
      if( (fileFmt == null) && (fileLen > 10) && (headerLen > 10) ) {
	char c3 = (char) (header[ 3 ] & 0xFF);
	char c4 = (char) (header[ 4 ] & 0xFF);
	char c5 = (char) (header[ 5 ] & 0xFF);
	char c6 = (char) (header[ 6 ] & 0xFF);
	if( ((header[ 0 ] & 0xFF)== ':')
	    && EmuUtil.isHexChar( header[ 1 ] & 0xFF )
	    && EmuUtil.isHexChar( header[ 2 ] & 0xFF )
	    && EmuUtil.isHexChar( c3 )
	    && EmuUtil.isHexChar( c4 )
	    && EmuUtil.isHexChar( c5 )
	    && EmuUtil.isHexChar( c6 )
	    && ((header[ 7 ] & 0xFF) == '0')
	    && ((header[ 8 ] & 0xFF) == '0')
	    && EmuUtil.isHexChar( header[ 9 ] & 0xFF )
	    && EmuUtil.isHexChar( header[ 10 ] & 0xFF ) )
	{
	  fileFmt = INTELHEX;
	}
      }
      if( (fileFmt == null) && (file != null) ) {
	String fName = file.getName();
	if( fName != null ) {
	  String upperName = fName.toUpperCase();
	  if( upperName.endsWith( ".KCB" )
	      && (fileLen > 127) && (headerLen > 20) )
	  {
	    int b16 = header[ 16 ] & 0xFF;
	    if( (b16 >= 2) && (b16 <= 4)
		&& (EmuUtil.getWord( header, 17 ) <= 0x0401)
		&& (EmuUtil.getWord( header, 19 ) >= 0x0409) )
	    {
	      fileFmt = KCB;
	    }
	  }
	  if( (fileFmt == null)
	      && (upperName.endsWith( ".KCC" ) || upperName.endsWith( ".JTC" ))
	      && (fileLen > 127) && (headerLen > 16) )
	  {
	    int b16 = header[ 16 ] & 0xFF;
	    if( (b16 >= 2) && (b16 <= 4)
		&& (EmuUtil.getWord( header, 17 )
				<= EmuUtil.getWord( header, 19 )) )
	    {
	      fileFmt = KCC;
	    }
	  }
	  if( (fileFmt == null) && upperName.endsWith( ".SSS" )
	      && (fileLen >= 9) && (headerLen >= 9) )
	  {
	    int h = header[ 3 ] & 0xFF;
	    if( (h == 0x04) || (h == 0x2C) ) {
	      fileFmt = KCBASIC_PURE;
	    }
	  }
	  if( fileFmt == null ) {
	    fileFmt = BIN;
	    int pos = fName.lastIndexOf( '.' );
	    if( (pos >= 0) && (pos + 1 < fName.length()) ) {
	      fileText = fName.substring( pos + 1 ).toUpperCase() + "-Datei";
	    }
	  }
	}
      }
      if( fileFmt == null ) {
	fileFmt = BIN;
      }
      StringBuilder buf = new StringBuilder( 26 );
      if( begAddr < 0 ) {
	begAddr = getBegAddr( header, fileFmt );
      }
      if( begAddr >= 0 ) {
	buf.append( String.format( "%04X", begAddr ) );
	if( endAddr < 0 ) {
	  endAddr = getEndAddr( header, fileFmt );
	}
	if( endAddr >= begAddr ) {
	  buf.append( String.format( "-%04X", endAddr ) );

	  int startAddr = getStartAddr( header, fileFmt );
	  if( (startAddr >= begAddr) && (startAddr <= endAddr) ) {
	    buf.append( String.format( " Start=%04X", startAddr ) );
	  }
	}
      }
      if( fileType != -1 ) {
	if( buf.length() > 0 ) {
	  buf.append( (char) '\u0020' );
	}
	buf.append( "Typ=" );
	buf.append( (char) fileType );
      }
      rv = new FileInfo(
			header,
			fileLen,
			fileType,
			fileFmt,
			fileText,
			getFileDesc( header, fileFmt ),
			buf.length() > 0 ? buf.toString() : null,
			nextTAPOffs );
    }
    return rv;
  }


  public static LoadData createLoadData(
				byte[] fileBuf,
				Object fileFmt ) throws IOException
  {
    LoadData rv = null;
    if( fileFmt != null ) {
      if( fileFmt.equals( KCTAP_SYS ) || fileFmt.equals( KCTAP_BASIC ) ) {
	rv = createLoadDataFromKCTAP( fileBuf, fileFmt );
      } else if( fileFmt.equals( INTELHEX ) ) {
	rv = createLoadDataFromINTELHEX( fileBuf );
      } else {
	int begAddr = -1;
	int len     = -1;
	if( fileFmt.equals( HEADERSAVE ) ) {
	  if( fileBuf.length >= 32 ) {
	    begAddr = EmuUtil.getWord( fileBuf, 0 );
	    len     = ((EmuUtil.getWord( fileBuf, 2 ) - begAddr) & 0xFFFF) + 1;
	  }
	  rv = new LoadData( fileBuf, 32, len, begAddr, -1, fileFmt );
	  int fileType = fileBuf[ 12 ] & 0xFF;
	  if( fileType == 'C' ) {
	    rv.setStartAddr( EmuUtil.getWord( fileBuf, 4 ) );
	  }
	  rv.setFileType( fileType );
	} else if( fileFmt.equals( KCB ) ) {
	  int fileBegAddr = EmuUtil.getWord( fileBuf, 17 );
	  int fileEndAddr = EmuUtil.getWord( fileBuf, 19 );
	  if( (fileBegAddr > 0x0401) || (fileEndAddr < 0x0401 + 8) ) {
	    new IOException( "Laden als KCB-Datei nicht m\u00F6glich" );
	  }
	  begAddr      = 0x0401;
	  int nextAddr = -1;
	  int curAddr  = begAddr;
	  do {
	    nextAddr = EmuUtil.getWord(
			      fileBuf,
			      128 + curAddr - fileBegAddr );
	    if( ((nextAddr > 0) && (nextAddr <= curAddr))
		|| (nextAddr >= fileEndAddr) )
	    {
	      new IOException( "Laden als KCB-Datei nicht m\u00F6glich" );
	    }
	    len     = curAddr - begAddr + 2;
	    curAddr = nextAddr;
	  } while( curAddr > 0 );
	  rv = new LoadData(
			fileBuf,
			128 + begAddr - fileBegAddr,
			len,
			begAddr,
			-1,
			fileFmt );
	  rv.setInfoMsg(
		"Es wird nur das eigentliche KC-BASIC-Programm geladen.\n"
			+ "Eventuell in der Datei enthaltener Maschinencode"
			+ " oder\n"
			+ "sonstige Daten werden nicht geladen!\n"
			+ "Wenn Sie das w\u00FCnschen,"
			+ " m\u00FCssen Sie die Datei\n"
			+ "als KCC-Datei laden." );
	} else if( fileFmt.equals( KCC ) ) {
	  if( fileBuf.length >= 128 ) {
	    begAddr = EmuUtil.getWord( fileBuf, 17 );
	    len     = ((EmuUtil.getWord( fileBuf, 19 ) - begAddr) & 0xFFFF)
									+ 1;
	  }
	  rv = new LoadData( fileBuf, 128, len, begAddr, -1, fileFmt );
	  if( fileBuf[ 16 ] >= 3 ) {
	    rv.setStartAddr( EmuUtil.getWord( fileBuf, 21 ) );
	  }
	} else if( fileFmt.equals( KCBASIC_HEAD ) ) {
	  if( fileBuf.length > 12 ) {
	    len = EmuUtil.getWord( fileBuf, 11 );
	  }
	  if( len <= 0 ) {
	    new IOException( "Laden als KC-BASIC-Datei nicht m\u00F6glich" );
	  }
	  rv = new LoadData(
			fileBuf,
			13,
			len,
			(((int) fileBuf[ 14 ] & 0xFF) << 8) | 0x01,
			-1,
			fileFmt );
	} else if( fileFmt.equals( KCBASIC_PURE ) ) {
	  if( fileBuf.length > 1) {
	    len = EmuUtil.getWord( fileBuf, 0 );
	  }
	  if( len <= 0 ) {
	    new IOException( "Laden als KC-BASIC-Datei nicht m\u00F6glich" );
	  }
	  rv = new LoadData(
			fileBuf,
			2,
			len,
			(((int) fileBuf[ 3 ] & 0xFF) << 8) | 0x01,
			-1,
			fileFmt );
	}
      }
    }
    if( rv == null ) {
      rv = new LoadData( fileBuf, 0, fileBuf.length, -1, -1, fileFmt );
    }
    return rv;
  }


  public static LoadData createLoadData( File file ) throws IOException
  {
    LoadData rv      = null;
    byte[]   fileBuf = readFile( file );
    if( fileBuf != null ) {
      FileInfo fileInfo = FileInfo.analyzeFile( fileBuf, file );
      if( fileInfo != null ) {
	rv = FileInfo.createLoadData( fileBuf, fileInfo.getFileFormat() );
      } else {
	rv = new LoadData( fileBuf, 0, fileBuf.length, -1, -1, null );
      }
    }
    return rv;
  }


  public boolean equalsFileFormat( String format )
  {
    return (this.fileFmt != null) && (format != null) ?
					this.fileFmt.equals( format )
					: false;
  }


  public String getAddrText()
  {
    return this.addrText;
  }


  public static int getBegAddr( byte[] header, Object fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( HEADERSAVE ) && (header.length > 1) ) {
	rv = EmuUtil.getWord( header, 0 );
      }
      else if( fileFmt.equals( KCB ) && (header.length > 20) ) {
	if( (EmuUtil.getWord( header, 17 ) <= 0x0401)
	    && (EmuUtil.getWord( header, 19 ) >= 0x0409) )
	{
	  rv = 0x0401;
	}
      }
      else if( fileFmt.equals( KCC ) && (header.length > 18) ) {
	rv = EmuUtil.getWord( header, 17 );
      }
      else if( fileFmt.equals( KCTAP_SYS ) && (header.length > 35) ) {
	rv = EmuUtil.getWord( header, 34 );
      }
      else if( fileFmt.equals( KCTAP_BASIC ) && (header.length > 31) ) {
	rv = ((int) (header[ 31 ] & 0xFF) << 8) | 0x01;
      }
      else if( fileFmt.equals( KCBASIC_HEAD ) && (header.length > 14) ) {
	rv = ((int) (header[ 14 ] & 0xFF) << 8) | 0x01;
      }
      else if( fileFmt.equals( KCBASIC_PURE ) && (header.length > 3) ) {
	rv = ((int) (header[ 3 ] & 0xFF) << 8) | 0x01;
      }
      else if( fileFmt.equals( INTELHEX ) && (header.length > 6) ) {
	char c3 = (char) (header[ 3 ] & 0xFF);
	char c4 = (char) (header[ 4 ] & 0xFF);
	char c5 = (char) (header[ 5 ] & 0xFF);
	char c6 = (char) (header[ 6 ] & 0xFF);
	if( ((header[ 0 ] & 0xFF) == ':')
	    && EmuUtil.isHexChar( c3 )
	    && EmuUtil.isHexChar( c4 )
	    && EmuUtil.isHexChar( c5 )
	    && EmuUtil.isHexChar( c6 ) )
	{
	  rv = (getHexValue( c3 ) << 12)
			| (getHexValue( c4 ) << 8)
			| (getHexValue( c5 ) << 4)
			| getHexValue( c6 );
	}
      }
    }
    return rv;
  }


  public int getBegAddr()
  {
    return getBegAddr( this.header, this.fileFmt );
  }


  public static int getEndAddr( byte[] header, Object fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( HEADERSAVE ) && (header.length > 3) ) {
	rv = EmuUtil.getWord( header, 2 );
      }
      else if( fileFmt.equals( KCB ) && (header.length > 20) ) {
	int fileEndAddr = EmuUtil.getWord( header, 19 );
	if( (EmuUtil.getWord( header, 17 ) <= 0x0401)
	    && (fileEndAddr >= 0x0409) )
	{
	  rv = fileEndAddr;
	}
      }
      else if( fileFmt.equals( KCC ) && (header.length > 20) ) {
	rv = EmuUtil.getWord( header, 19 );
      }
      else if( fileFmt.equals( KCTAP_SYS ) && (header.length > 37) ) {
	rv = EmuUtil.getWord( header, 36 );
      }
      else if( fileFmt.equals( KCTAP_BASIC ) && (header.length > 31) ) {
	rv = EmuUtil.getWord( header, 28 ) + ((header[ 31 ] & 0xFF) << 8);
      }
      else if( fileFmt.equals( KCBASIC_HEAD ) && (header.length > 14) ) {
	rv = EmuUtil.getWord( header, 11 ) + ((header[ 14 ] & 0xFF) << 8);
      }
      else if( fileFmt.equals( KCBASIC_PURE ) && (header.length > 3) ) {
	rv = EmuUtil.getWord( header, 0 ) + ((header[ 3 ] & 0xFF) << 8);
      }
    }
    return rv;
  }


  public int getEndAddr()
  {
    return getEndAddr( this.header, this.fileFmt );
  }


  public static String getFileDesc( byte[] header, Object fileFmt )
  {
    String rv = null;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( HEADERSAVE ) && (header.length >= 32) ) {
	rv = getFileDesc( header, 16, 16 );
      }
      else if( fileFmt.equals( KCTAP_SYS ) && (header.length >= 28) ) {
	rv = getFileDesc( header, 17, 11 );
	if( rv != null ) {
	  if( rv.length() < 8 ) {
	    String ext = getFileDesc( header, 25, 3 );
	    if( ext != null )
	      rv = rv + "." + ext;
	  }
	}
      }
      else if( fileFmt.equals( KCTAP_BASIC ) && (header.length >= 28) ) {
	rv = getFileDesc( header, 20, 8 );
      }
      else if( (fileFmt.equals( KCB ) || fileFmt.equals( KCC ))
	       && (header.length >= 11) )
      {
	rv = getFileDesc( header, 0, 11 );
	if( rv != null ) {
	  if( rv.length() < 8 ) {
	    String ext = getFileDesc( header, 8, 3 );
	    if( ext != null )
	      rv = rv + "." + ext;
	  }
	}
      }
      else if( fileFmt.equals( KCBASIC_HEAD ) && (header.length >= 11) ) {
	rv = getFileDesc( header, 3, 8 );
      }
    }
    return rv;
  }


  public String getFileDesc()
  {
    return this.fileDesc;
  }


  public static int getFileType( byte[] header, Object fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( HEADERSAVE ) && (header.length > 12) ) {
	rv = header[ 12 ] & 0xFF;
      }
    }
    return rv;
  }


  public String getFileText()
  {
    return this.fileText;
  }


  public String getFileFormat()
  {
    return this.fileFmt;
  }


  public int getFileType()
  {
    return this.fileType;
  }


  public String getInfoText()
  {
    return this.infoText;
  }


  public int getNextTAPOffset()
  {
    return this.nextTAPOffs;
  }


  public static int getStartAddr( byte[] header, Object fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( HEADERSAVE ) && (header.length > 12) ) {
	if( (header[ 12 ] & 0xFF) == 'C' ) {
	  rv = EmuUtil.getWord( header, 4 );
	}
      }
      else if( fileFmt.equals( KCC ) && (header.length > 22) ) {
	if( (header[ 16 ] & 0xFF) >= 3 ) {
	  rv = EmuUtil.getWord( header, 21 );
	}
      }
      else if( fileFmt.equals( KCTAP_SYS ) && (header.length > 39) ) {
	if( (header[ 33 ] & 0xFF) >= 3 ) {
	  rv = EmuUtil.getWord( header, 38 );
	}
      }
    }
    return rv;
  }


  public int getStartAddr()
  {
    return getStartAddr( this.header, this.fileFmt );
  }


  public static boolean isKCBasicProgramFormat( Object fileFmt )
  {
    boolean rv = false;
    if( fileFmt != null ) {
      rv = fileFmt.equals( KCB )
		|| fileFmt.equals( KCTAP_BASIC )
		|| fileFmt.equals( KCBASIC_HEAD )
		|| fileFmt.equals( KCBASIC_PURE );
    }
    return rv;
  }


  public boolean isKCBasicProgramFormat()
  {
    return isKCBasicProgramFormat( this.fileFmt );
  }


  /*
   * Diese Methode liest eine Datei und liefert ihren Inhalt als Byte-Array.
   * Um bei einer sehr grossen Datei einen Speicherueberlauf zu verhindern,
   * werden nur soviele Bytes gelesen,
   * dass sich bis zu 64 KByte Nutzdaten extrahieren lassen.
   * Im unguenstigsten Fall enthaelt eine Intel-HEX-Datei ein Nutzbyte
   * pro Zeile, sodass fuer ein Nutzbyte theoretisch max. 13 Dateibytes
   * notwendig sein koennen (:01AAAABBCC\r\n).
   * Demzufolge brauchen nur bis zu 64k * 13 Bytes gelesen werden.
   */
  public static byte[] readFile( File file ) throws IOException
  {
    return EmuUtil.readFile( file, 0x10000 * 13 );
  }


	/* --- private Konstruktoren und Methoden --- */

  private FileInfo(
		byte[] header,
		long   fileLen,
		int    fileType,
		String fileFmt,
		String fileText,
		String fileDesc,
		String addrText,
		int    nextTAPOffs )
  {
    this.header      = header;
    this.fileLen     = fileLen;
    this.fileType    = fileType;
    this.fileFmt     = fileFmt;
    this.fileText    = (fileText != null ? fileText : fileFmt);
    this.fileDesc    = fileDesc;
    this.addrText    = addrText;
    this.nextTAPOffs = nextTAPOffs;

    boolean       colon = true;
    StringBuilder buf   = new StringBuilder( 64 );
    buf.append( this.fileText );
    if( this.addrText != null ) {
      if( colon ) {
	buf.append( ": " );
	colon = false;
      }
      buf.append( this.addrText );
    }
    if( this.fileDesc != null ) {
      if( colon ) {
	buf.append( ": " );
      } else {
	if( buf.length() > 0 ) {
	  buf.append( (char) '\u0020' );
	}
      }
      buf.append( this.fileDesc );
    }
    this.infoText = buf != null ? buf.toString() : null;
  }


  private static LoadData createLoadDataFromINTELHEX(
					byte[] fileBuf ) throws IOException
  {
    String infoMsg = null;

    ByteArrayOutputStream out = new ByteArrayOutputStream( 0x4000 );
    ByteArrayInputStream  in  = new ByteArrayInputStream( fileBuf );

    boolean loop     = true;
    int     firstAddr = -1;
    int     curAddr  = -1;
    int     ch       = in.read();
    while( loop && (ch != -1) ) {

      // Startmarkierung suchen
      while( (ch != -1) && (ch != ':') ) {
	ch = in.read();
      }
      if( ch != -1 ) {

	/*
	 * Segment verarbeiten
	 *
	 * Satzarten 0 (Daten) und 1 (Endekennung) werden verarbeitet.
	 * Satzarten 3 und 5 (Startadressen) werden ignoriert.
	 * Andere Satzarten fuehren zu einer Fehlermeldung.
	 */
	int cnt  = parseHex( in, 2 );
	int addr = parseHex( in, 4 );
	int type = parseHex( in, 2 );
	if( (type == 0) || (type == 1) ) {
	  if( cnt > 0 ) {
	    if( firstAddr < 0 ) {
	      firstAddr = addr;
	      curAddr   = addr;
	    }
	    if( addr == curAddr ) {
	      while( cnt > 0 ) {
		out.write( parseHex( in, 2 ) );
		--cnt;
		curAddr++;
	      }
	    } else {
	      infoMsg = "Die Datei enth\u00E4lt mehrere nicht"
				+ " zusammenh\u00E4ngende Datenbereiche.\n"
				+ "Es wurde nur der erste Bereich geladen.";
	      loop = false;
	    }
	  }
	  if( type == 1 ) {
	    loop = false;
	  }
	} else if( (type != 3) && (type != 5) ) {
	  infoMsg = "Die Datei enth\u00E4lt nicht unterst\u00FCtzte"
			+ " Datensatzarten.";
	  if( out.size() > 0 ) {
	    infoMsg = infoMsg
			+ "\nEs werden nur die Daten bis zur ersten nicht"
			+ " unterst\u00FCtzten Satzart geladen.";
	  } else {
	    throw new IOException( infoMsg );
	  }
	  loop = false;
	}
	ch = in.read();
      }
    }
    in.close();
    out.close();

    LoadData rv = null;
    if( firstAddr >= 0 ) {
      byte[] dataBytes = out.toByteArray();
      if( dataBytes != null ) {
	if( dataBytes.length > 0 ) {
	  rv = new LoadData(
			dataBytes,
			0,
			dataBytes.length,
			firstAddr,
			-1,
			FileInfo.INTELHEX );
	}
      }
    }
    if( rv == null ) {
      new IOException( "Laden als Intel-HEX-Datei nicht m\u00F6glich" );
    }
    rv.setInfoMsg( infoMsg );
    return rv;
  }


  private static LoadData createLoadDataFromKCTAP(
					byte[] fileBuf,
					Object fileFmt ) throws IOException
  {
    int     begAddr   = -1;
    int     startAddr = -1;
    int     len       = 0;
    int     pos       = 0;
    int     blkRemain = 0;
    boolean kcbasic   = false;
    if( fileFmt.equals( KCTAP_SYS ) && (fileBuf.length > 39) ) {
      begAddr = EmuUtil.getWord( fileBuf, 34 );
      len     = ((EmuUtil.getWord( fileBuf, 36 ) - begAddr) & 0xFFFF) + 1;
      if( fileBuf[ 33 ] >= 3 ) {
	startAddr = EmuUtil.getWord( fileBuf, 38 );
      }
      pos = 145;
    }
    else if( fileFmt.equals( KCTAP_BASIC ) && (fileBuf.length > 31) ) {
      begAddr   = ((fileBuf[ 31 ] & 0xFF) << 8) | 0x01;
      len       = EmuUtil.getWord( fileBuf, 28 );
      pos       = 30;
      blkRemain = 115;
      kcbasic   = true;
    }
    if( (begAddr < 0) || (len <= 0) ) {
      throw new IOException( "Laden als KC-TAP-Datei nicht m\u00F6glich" );
    }
    byte[] dstBuf = new byte[ len ];
    int    dstPos = 0;
    int    n      = len;
    while( (n > 0) && (pos < fileBuf.length) && (dstPos < dstBuf.length) ) {
      if( blkRemain == 0 ) {
	blkRemain = 128;
      } else {
	dstBuf[ dstPos++ ] = fileBuf[ pos ];
	--n;
	--blkRemain;
      }
      pos++;
    }
    LoadData rv = new LoadData(
			dstBuf,
			0,
			len - n,
			begAddr,
			startAddr,
			fileFmt );
    return rv;
  }


  private static String getFileDesc( byte[] header, int pos, int len )
  {
    StringBuilder buf = new StringBuilder( len );
    int           nSp = 0;
    while( (len > 0) && (pos < header.length) ) {
      int b = (int) header[ pos++ ] & 0xFF;
      if( b == 0 ) {
	break;
      }
      if( b == 0x20 ) {
	nSp++;
      }
      else if( (b > 0x20) && Character.isDefined( b ) ) {
	while( nSp > 0 ) {
	  buf.append( (char) '\u0020' );
	  --nSp;
	}
	buf.append( (char) b );
      }
      --len;
    }
    return buf.length() > 0 ? buf.toString() : null;
  }


  private static int getHexValue( char ch )
  {
    int rv = -1;
    if( (ch >= '0') && (ch <= '9') ) {
      rv = (ch - '0');
    }
    else if( (ch >= 'A') && (ch <= 'Z') ) {
      rv = (ch - 'A' + 10);
    }
    else if( (ch >= 'a') && (ch <= 'z') ) {
      rv = (ch - 'a' + 10);
    }
    return rv;
  }


  private static boolean isTAPHeaderAt(
				byte[] fileBytes,
				int    fileLen,
				int    offs )
  {
    boolean rv = false;
    if( (offs + KCTAP_HLEN) < Math.min( fileBytes.length, fileLen ) ) {
      rv = true;
      for( int i = 0; i < KCTAP_HLEN; i++ ) {
	if( ((int) fileBytes[ offs + i ] & 0xFF)
				!= (int) KCTAP_HEADER.charAt( i ) )
	{
	  rv = false;
	  break;
	}
      }
    }
    return rv;
  }


  private static int parseHex( InputStream in, int cnt ) throws IOException
  {
    int value = 0;
    while( cnt > 0 ) {
      int ch = in.read();
      if( (ch >= '0') && (ch <= '9') ) {
	value = (value << 4) | ((ch - '0') & 0x0F);
      } else if( (ch >= 'A') && (ch <= 'F') ) {
	value = (value << 4) | ((ch - 'A' + 10) & 0x0F);
      } else if( (ch >= 'a') && (ch <= 'f') ) {
	value = (value << 4) | ((ch - 'a' + 10) & 0x0F);
      } else {
	throw new IOException(
		"Datei entspricht nicht dem erwarteten HEX-Format." );
      }
      --cnt;
    }
    return value;
  }
}

