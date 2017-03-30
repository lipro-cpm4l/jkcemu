/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber eine Datei
 */

package jkcemu.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import jkcemu.Main;


public class FileInfo
{
  public static final String CSW_MAGIC   = "Compressed Square Wave\u001A";
  public static final String KCTAP_MAGIC = "\u00C3KC-TAPE by AF.\u0020";
  public static final String TZX_MAGIC   = "ZXTape!\u001A";

  private byte[]     header;
  private long       fileLen;
  private int        fileType;
  private FileFormat fileFmt;
  private String     fileText;
  private String     fileDesc;
  private String     addrText;
  private String     infoText;
  private int        nextTAPOffs;


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

	    byte[] header = new byte[ 256 ];
	    int    nRead  = EmuUtil.read( in, header );
	    if( nRead > 0 ) {
	      if( nRead < header.length ) {
		header = Arrays.copyOf( header, nRead );
	      }
	      rv = analyzeFile( header, file );
	    }
	  }
	  finally {
	    EmuUtil.closeSilent( in );
	  }
	}
      }
      catch( Exception ex ) {}
    }
    return rv;
  }


  public static FileInfo analyzeFile( byte[] header, File file )
  {
    FileInfo rv = null;
    if( header != null ) {
      String     upperFileName = null;
      FileFormat fileFmt       = null;
      String     fileText      = null;
      int        fileType      = -1;
      int        begAddr       = -1;
      int        endAddr       = -1;
      int        nextTAPOffs   = -1;
      long       fileLen       = 0;
      if( file != null ) {
	String fileName = file.getName();
	if( fileName != null ) {
	  upperFileName = fileName.toUpperCase();
	}
	fileLen = file.length();
      }
      if( (fileLen > 32) && (header.length > 15) ) {
	int b13 = (int) header[ 13 ] & 0xFF;
	int b14 = (int) header[ 14 ] & 0xFF;
	int b15 = (int) header[ 15 ] & 0xFF;
	if( (header[ 13 ] == (byte) 0xD3)
	    && (header[ 14 ] == (byte) 0xD3)
	    && (header[ 15 ] == (byte) 0xD3) )
	{
	  fileFmt = FileFormat.HEADERSAVE;
	  begAddr = getBegAddr( header, fileFmt );
	  endAddr = getEndAddr( header, fileFmt );
	  int b12 = (int) header[ 12 ] & 0xFF;
	  if( (b12 >= 0x20) && (b12 < 0x7F) ) {
	    fileType = b12;
	  }
	}
      }
      if( (fileFmt == null) && (fileLen > 144) && (header.length > 33) ) {
	if( isKCTapMagicAt( header, 0 ) ) {
	  int nextOffs = -1;
	  int b16      = (int) header[ 16 ] & 0xFF;
	  int b17      = (int) header[ 17 ] & 0xFF;
	  int b18      = (int) header[ 18 ] & 0xFF;
	  int b19      = (int) header[ 19 ] & 0xFF;
	  if( ((b17 < 0xD3) || (b17 > 0xD8))
	      && ((b18 < 0xD3) || (b18 > 0xD8))
	      && ((b19 < 0xD3) || (b19 > 0xD8)) )
	  {
	    if( b16 == 0 ) {
	      fileFmt = FileFormat.KCTAP_Z9001;
	    } else if( b16 == 1 ) {
	      fileFmt = FileFormat.KCTAP_KC85;
	    } else {
	      fileFmt = FileFormat.KCTAP_SYS;
	    }
	    begAddr = getBegAddr( header, fileFmt );
	    endAddr = getEndAddr( header, fileFmt );
	    if( (begAddr >= 0) && (begAddr <= endAddr) ) {
	      int nBlks = (endAddr - begAddr + 127) / 128;
	      nextOffs  = 16 + (129 * (nBlks + 1));
	    }
	  } else {
	    if( ((b17 == 0xD3) && (b18 == 0xD3) && (b19 == 0xD3))
		|| ((b17 == 0xD7) && (b18 == 0xD7) && (b19 == 0xD7)) )
	    {
	      fileFmt = FileFormat.KCTAP_BASIC_PRG;
	      begAddr = getBegAddr( header, fileFmt );
	      endAddr = getEndAddr( header, fileFmt );
	      if( (begAddr >= 0) && (begAddr <= endAddr) ) {
		int nBlks = (endAddr - begAddr + 13 + 127) / 128;
		nextOffs  = 16 + (129 * nBlks);
	      }
	    }
	    else if( ((b17 == 0xD4) && (b18 == 0xD4) && (b19 == 0xD4))
		     || ((b17 == 0xD8) && (b18 == 0xD8) && (b19 == 0xD8)) )
	    {
	      fileFmt = FileFormat.KCTAP_BASIC_DATA;
	    }
	    else if( ((b17 == 0xD5) && (b18 == 0xD5) && (b19 == 0xD5))
		     || ((b17 == 0xD9) && (b18 == 0xD9) && (b19 == 0xD9)) )
	    {
	      fileFmt = FileFormat.KCTAP_BASIC_ASC;
	    }
	  }
	  if( nextOffs > 0 ) {
	    if( isKCTapMagicAt( header, nextOffs ) ) {
	      nextTAPOffs = nextOffs;
	    }
	  }
	}
      }
      if( (fileFmt == null) && (fileLen > 20) && (header.length > 2) ) {
	AtomicBoolean cks     = new AtomicBoolean();
	boolean       blkNums = hasKCBlkNums( header, cks );
	int           pos     = blkNums ? 1 : 0;
	int           b0      = (int) header[ pos++ ] & 0xFF;
	int           b1      = (int) header[ pos++ ] & 0xFF;
	int           b2      = (int) header[ pos ] & 0xFF;
	if( ((b0 == 0xD3) && (b1 == 0xD3) && (b2 == 0xD3))
	    || ((b0 == 0xD7) && (b1 == 0xD7) && (b2 == 0xD7)) )
	{
	  if( blkNums ) {
	    if( cks.get() ) {
	      fileFmt = FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS;
	    } else {
	      fileFmt = FileFormat.KCBASIC_HEAD_PRG_BLKN;
	    }
	  } else {
	    fileFmt = FileFormat.KCBASIC_HEAD_PRG;
	  }
	}
	else if( ((b0 == 0xD4) && (b1 == 0xD4) && (b2 == 0xD4))
		 || ((b0 == 0xD8) && (b1 == 0xD8) && (b2 == 0xD8)) )
	{
	  if( blkNums ) {
	    if( cks.get() ) {
	      fileFmt = FileFormat.KCBASIC_HEAD_DATA_BLKN_CKS;
	    } else {
	      fileFmt = FileFormat.KCBASIC_HEAD_DATA_BLKN;
	    }
	  } else {
	    fileFmt = FileFormat.KCBASIC_HEAD_DATA;
	  }
	}
	else if( ((b0 == 0xD5) && (b1 == 0xD5) && (b2 == 0xD5))
		 || ((b0 == 0xD9) && (b1 == 0xD9) && (b2 == 0xD9)) )
	{
	  if( blkNums ) {
	    if( cks.get() ) {
	      fileFmt = FileFormat.KCBASIC_HEAD_ASC_BLKN_CKS;
	    } else {
	      fileFmt = FileFormat.KCBASIC_HEAD_ASC_BLKN;
	    }
	  } else {
	    fileFmt = FileFormat.KCBASIC_HEAD_ASC;
	  }
	}
      }
      if( (fileFmt == null)
	  && (fileLen > CSW_MAGIC.length())
	  && (header.length > CSW_MAGIC.length()) )
      {
	if( isCswMagicAt( header, 0 ) ) {
	  fileFmt = FileFormat.CSW;
	}
      }
      if( (fileFmt == null)
	  && (fileLen > TZX_MAGIC.length())
	  && (header.length > TZX_MAGIC.length()) )
      {
	if( isTzxMagicAt( header, 0 ) ) {
	  fileFmt = FileFormat.TZX;
	  if( upperFileName != null ) {
	    if( upperFileName.endsWith( ".CDT" ) ) {
	      fileFmt = FileFormat.CDT;
	    }
	  }
	}
      }
      if( (fileFmt == null) && (fileLen > 10) && (header.length > 10) ) {
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
	    && EmuUtil.isHexChar( header[ 7 ] & 0xFF )
	    && EmuUtil.isHexChar( header[ 8 ] & 0xFF )
	    && EmuUtil.isHexChar( header[ 9 ] & 0xFF )
	    && EmuUtil.isHexChar( header[ 10 ] & 0xFF ) )
	{
	  fileFmt = FileFormat.INTELHEX;
	}
      }
      if( (fileFmt == null)
	  && (file != null)
	  && (upperFileName != null) )
      {
	if( upperFileName.endsWith( ".KCB" )
	    && (fileLen > 127) && (header.length > 20) )
	{
	  AtomicBoolean cks     = new AtomicBoolean();
	  boolean       blkNums = hasKCBlkNums( header, cks );
	  int           begPos  = blkNums ? 1 : 0;
	  int b16 = (int) header[ begPos + 16 ] & 0xFF;
	  if( (b16 >= 2) && (b16 <= 4)
	      && (EmuUtil.getWord( header, begPos + 17 ) <= 0x0401)
	      && (EmuUtil.getWord( header, begPos + 19 ) >= 0x0409) )
	  {
	    if( blkNums ) {
	      if( cks.get() ) {
		fileFmt = FileFormat.KCB_BLKN_CKS;
	      } else {
		fileFmt = FileFormat.KCB_BLKN;
	      }
	    } else {
	      fileFmt = FileFormat.KCB;
	    }
	  }
	}
	if( (fileFmt == null)
	    && (upperFileName.endsWith( ".KCC" )
			|| upperFileName.endsWith( ".KCM" )
			|| upperFileName.endsWith( ".851" )
			|| upperFileName.endsWith( ".852" )
			|| upperFileName.endsWith( ".853" )
			|| upperFileName.endsWith( ".854" )
			|| upperFileName.endsWith( ".855" ))
	    && (fileLen > 127) && (header.length > 16) )
	{
	  AtomicBoolean cks = new AtomicBoolean();
	  if( hasKCBlkNums( header, cks ) ) {
	    if( cks.get() ) {
	      fileFmt = FileFormat.KCC_BLKN_CKS;
	    } else {
	      fileFmt = FileFormat.KCC_BLKN;
	    }
	  } else {
	    fileFmt = FileFormat.KCC;
	  }
	  begAddr = getBegAddr( header, fileFmt );
	  endAddr = getEndAddr( header, fileFmt );
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".SSS" )
	    && (fileLen >= 9) && (header.length >= 9) )
	{
	  fileFmt = FileFormat.KCBASIC_PRG;
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".ABC" )
	    && (fileLen >= 8) && (header.length >= 8) )
	{
	  if( ((header[ 1 ] & 0xFF) == 0x63)
	      && (EmuUtil.getWord( header, 0 ) >= 0x6307) )
	  {
	    // wahrscheinlich AC1-BASIC6-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x6300;
	    fileText = "AC1-BASIC6-Programmdatei";
	  }
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".BAC" )
	    && (fileLen >= 8) && (header.length >= 8) )
	{
	  if( ((header[ 1 ] & 0xFE) == 0x60)
	      && (EmuUtil.getWord( header, 0 ) >= 0x60FD) )
	  {
	    // wahrscheinlich BACOBAS-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x60F7;
	    fileText = "BACOBAS-Programmdatei";
	  }
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".BAS" )
	    && (fileLen >= 8) && (header.length >= 8) )
	{
	  if( ((header[ 0 ] & 0xFF) == 0xFF)
	      && ((header[ 2 ] & 0xFE) == 0x80)
	      && (EmuUtil.getWord( header, 1 ) >= 0x8007) )
	  {
	    fileFmt = FileFormat.RBASIC_PRG;
	    begAddr = 0x8001;
	  }
	  else if( ((header[ 1 ] & 0xFE) == 0x04)
		     && (EmuUtil.getWord( header, 0 ) >= 0x0407) )
	  {
	    // wahrscheinlich KC-BASIC-Programm (Interpreter im ROM)
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x0401;
	    fileText = "BASIC-Programmdatei f\u00FCr KC-ROM-BASIC";
	  }
	  else if( ((header[ 1 ] & 0xFE) == 0x10)
		     && (EmuUtil.getWord( header, 0 ) >= 0x1007) )
	  {
	    // wahrscheinlich KramerMC-BASIC-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x1001;
	    fileText = "BASIC-Programmdatei f\u00FCr Kramer-MC";
	  }
	  else if( ((header[ 1 ] & 0xFE) == 0x60)
		     && (EmuUtil.getWord( header, 0 ) >= 0x60FD) )
	  {
	    // wahrscheinlich AC1-/LLC2-BASIC-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x60F7;
	    fileText = "BASIC-Programmdatei f\u00FCr AC1/LLC2";
	  }
	  else if( ((header[ 1 ] & 0xFF) == 0x63)
		     && (EmuUtil.getWord( header, 0 ) >= 0x6307) )
	  {
	    // wahrscheinlich AC1-BASIC6-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x6300;
	    fileText = "AC1-BASIC6-Programmdatei";
	  }
	  else if( (((header[ 1 ] & 0xFF) == 0x6F)
			|| ((header[ 1 ] & 0xFF) == 0x70))
		     && (EmuUtil.getWord( header, 0 ) >= 0x6FBD) )
	  {
	    // wahrscheinlich AC1-12K-BASIC-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x6FB7;
	    fileText = "BASIC-Programmdatei f\u00FCr AC1 (12K BASIC)";
	  }
	  else if( ((header[ 1 ] & 0xFE) == 0x2C)
		     && (EmuUtil.getWord( header, 0 ) >= 0x2C07) )
	  {
	    // wahrscheinlich KC-BASIC-Programm (Interpreter im RAM)
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x2C01;
	    fileText = "BASIC-Programmdatei f\u00FCr KC-RAM-BASIC";
	  }
	  else if( (((header[ 1 ] & 0xFF) == 0x37)
			|| ((header[ 1 ] & 0xFF) == 0x38))
		     && (EmuUtil.getWord( header, 0 ) >= 0x3776) )
	  {
	    // wahrscheinlich HUEBLER-BASIC-Programm
	    fileFmt  = FileFormat.BASIC_PRG;
	    begAddr  = 0x3770;
	    fileText = "BASIC-Programmdatei f\u00FCr H\u00FCbler-Grafik-MC";
	  }
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".RMC" )
	    && (fileLen >= 8) && (header.length >= 7) )
	{
	  if( (header[ 0 ] & 0xFF) == 0xFE ) {
	    fileFmt = FileFormat.RMC;
	    begAddr = getBegAddr( header, fileFmt );
	    endAddr = getEndAddr( header, fileFmt );
	  }
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".TAP" )
	    && !isKCTapMagicAt( header, 0 ) )
	{
	  fileFmt = FileFormat.ZXTAP;
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".BIN" )
	    && (fileLen > 0) )
	{
	  fileFmt  = FileFormat.BIN;
	  fileText = "BIN-Datei";
	}
	if( (fileFmt == null) && upperFileName.endsWith( ".ROM" )
	    && (fileLen > 0) )
	{
	  fileFmt  = FileFormat.BIN;
	  fileText = "ROM-Datei";
	}
      }
      if( (fileFmt == null)
	  && (fileText == null)
	  && (upperFileName != null) )
      {
	int pos = upperFileName.lastIndexOf( '.' );
	if( (pos >= 0) && ((pos + 1) < upperFileName.length()) ) {
	  fileText = upperFileName.substring( pos + 1 ) + "-Datei";
	}
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


  public LoadData createLoadData( byte[] fileBuf ) throws IOException
  {
    return createLoadData( fileBuf, this.fileFmt );
  }


  /*
   * In einer KCC- oder KC-TAP-Datei wird geweohnlich die Endadresse + 1
   * eingetragen.  Es gibt aber auch Faelle,
   * in denen die tatsaechlie Endadresse eingetragen ist.
   * Damit kein Byte zu wenig geladen wird, wird so getan,
   * als ob die tatsaechliche Endadresse eingetragen ist.
   */
  public static LoadData createLoadData(
				byte[]     fileBuf,
				FileFormat fileFmt ) throws IOException
  {
    LoadData rv = null;
    if( fileFmt != null ) {
      if( fileFmt.equals( FileFormat.KCTAP_BASIC_DATA )
	  || fileFmt.equals( FileFormat.KCTAP_BASIC_ASC )
	  || fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC )
	  || fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC_BLKN )
	  || fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC_BLKN_CKS )
	  || fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA )
	  || fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA_BLKN )
	  || fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA_BLKN_CKS ) )
      {
	throw new IOException( "Laden von KC-BASIC-Datenfeldern"
			+ " und KC-BASIC-ASCII-Listings\n"
			+ "wird nicht unterst\u00FCtzt" );
      }
      if( fileFmt.equals( FileFormat.KCB_BLKN ) ) {
	fileBuf = removeKCBlockNums( fileBuf );
	fileFmt = FileFormat.KCB;
      } else if( fileFmt.equals( FileFormat.KCC_BLKN ) ) {
	fileBuf = removeKCBlockNums( fileBuf );
	fileFmt = FileFormat.KCC;
      } else if( fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN ) ) {
	fileBuf = removeKCBlockNums( fileBuf );
	fileFmt = FileFormat.KCBASIC_HEAD_PRG;
      } else if( fileFmt.equals( FileFormat.KCB_BLKN_CKS ) ) {
	fileBuf = removeKCBlockNumsAndChecksums( fileBuf );
	fileFmt = FileFormat.KCB;
      } else if( fileFmt.equals( FileFormat.KCC_BLKN_CKS ) ) {
	fileBuf = removeKCBlockNumsAndChecksums( fileBuf );
	fileFmt = FileFormat.KCC;
      } else if( fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS ) ) {
	fileBuf = removeKCBlockNumsAndChecksums( fileBuf );
	fileFmt = FileFormat.KCBASIC_HEAD_PRG;
      }
      if( fileFmt.equals( FileFormat.CDT )
	  || fileFmt.equals( FileFormat.TZX )
	  || fileFmt.equals( FileFormat.ZXTAP ) )
      {
	throw new IOException(
		"Die Datei ist eine Tape-Datei und kann nur\n"
			+ "\u00FCber die emulierte Kassettenschnittstelle\n"
			+ "(Audiofunktion) geladen werden." );
      }
      if( fileFmt.equals( FileFormat.KCTAP_SYS )
	  || fileFmt.equals( FileFormat.KCTAP_Z9001 )
	  || fileFmt.equals( FileFormat.KCTAP_KC85 )
	  || fileFmt.equals( FileFormat.KCTAP_BASIC_PRG ) )
      {
	rv = createLoadDataFromKCTAP( fileBuf, fileFmt );
      } else if( fileFmt.equals( FileFormat.INTELHEX ) ) {
	rv = createLoadDataFromINTELHEX( fileBuf );
      } else {
	int begAddr = -1;
	int len     = -1;
	if( fileFmt.equals( FileFormat.HEADERSAVE ) ) {
	  if( fileBuf.length >= 32 ) {
	    begAddr = EmuUtil.getWord( fileBuf, 0 );
	    len = ((EmuUtil.getWord( fileBuf, 2 ) - begAddr) & 0xFFFF) + 1;
	  }
	  rv = new LoadData( fileBuf, 32, len, begAddr, -1, fileFmt );
	  int fileType = fileBuf[ 12 ] & 0xFF;
	  if( fileType == 'C' ) {
	    rv.setStartAddr( EmuUtil.getWord( fileBuf, 4 ) );
	  }
	  rv.setFileType( fileType );
	} else if( fileFmt.equals( FileFormat.KCB ) ) {
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
	} else if( fileFmt.equals( FileFormat.KCC ) ) {
	  if( fileBuf.length >= 128 ) {
	    begAddr = EmuUtil.getWord( fileBuf, 17 );
	    len = ((EmuUtil.getWord( fileBuf, 19 ) - begAddr + 1) & 0xFFFF);
	  }
	  rv = new LoadData( fileBuf, 128, len, begAddr, -1, fileFmt );
	  if( fileBuf[ 16 ] >= 3 ) {
	    rv.setStartAddr( EmuUtil.getWord( fileBuf, 21 ) );
	  }
	} else if( fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG ) ) {
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
			getKCBasicBegAddr( fileBuf, 14 ),
			-1,
			fileFmt );
	} else if( fileFmt.equals( FileFormat.KCBASIC_PRG ) ) {
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
			getKCBasicBegAddr( fileBuf, 3 ),
			-1,
			fileFmt );
	} else if( fileFmt.equals( FileFormat.BASIC_PRG ) ) {
	  if( fileBuf.length > 1) {
	    begAddr = getBegAddr( fileBuf, fileFmt );
	  }
	  if( begAddr < 0 ) {
	    new IOException( "Laden als BASIC-Datei nicht m\u00F6glich" );
	  }
	  rv = new LoadData(
			fileBuf,
			0,
			fileBuf.length,
			begAddr,
			-1,
			fileFmt );
	} else if( fileFmt.equals( FileFormat.RBASIC_PRG ) ) {
	  rv = new LoadData(
			fileBuf,
			1,
			fileBuf.length,
			0x8001,
			-1,
			fileFmt );
	} else if( fileFmt.equals( FileFormat.RMC ) ) {
	  begAddr = EmuUtil.getWord( fileBuf, 1 );
	  rv = new LoadData(
		fileBuf,
		7,
		((EmuUtil.getWord( fileBuf, 3 ) - begAddr) & 0xFFFF) + 1,
		begAddr,
		(EmuUtil.getWord( fileBuf, 5 ) - begAddr) & 0xFFFF,
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


  public boolean equalsFileFormat( FileFormat fmt )
  {
    return (this.fileFmt != null) && (fmt != null) ?
					this.fileFmt.equals( fmt )
					: false;
  }


  public String getAddrText()
  {
    return this.addrText;
  }


  public static int getBegAddr( byte[] header, FileFormat fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileFormat.HEADERSAVE ) && (header.length > 1) ) {
	rv = EmuUtil.getWord( header, 0 );
      }
      else if( fileFmt.equals( FileFormat.KCB ) && (header.length > 20) ) {
	if( (EmuUtil.getWord( header, 17 ) <= 0x0401)
	    && (EmuUtil.getWord( header, 19 ) >= 0x0409) )
	{
	  rv = 0x0401;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCB_BLKN )
			|| fileFmt.equals( FileFormat.KCB_BLKN_CKS ))
	       && (header.length > 21) )
      {
	if( (EmuUtil.getWord( header, 18 ) <= 0x0401)
	    && (EmuUtil.getWord( header, 20 ) >= 0x0409) )
	{
	  rv = 0x0401;
	}
      }
      else if( fileFmt.equals( FileFormat.KCC ) && (header.length > 18) ) {
	rv = EmuUtil.getWord( header, 17 );
      }
      else if( (fileFmt.equals( FileFormat.KCC_BLKN )
			|| fileFmt.equals( FileFormat.KCC_BLKN_CKS ))
	       && (header.length > 19) )
      {
	rv = EmuUtil.getWord( header, 18 );
      }
      else if( (fileFmt.equals( FileFormat.KCTAP_SYS )
			|| fileFmt.equals( FileFormat.KCTAP_Z9001)
			|| fileFmt.equals( FileFormat.KCTAP_KC85 ))
	       && (header.length > 35) )
      {
	rv = EmuUtil.getWord( header, 34 );
      }
      else if( fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
	       && (header.length > 31) )
      {
	rv = getKCBasicBegAddr( header, 31 );
      }
      else if( fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
	       && (header.length > 14) )
      {
	rv = getKCBasicBegAddr( header, 14 );
      }
      else if( (fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS ))
	       && (header.length > 15) )
      {
	rv = getKCBasicBegAddr( header, 15 );
      }
      else if( fileFmt.equals( FileFormat.KCBASIC_PRG )
	       && (header.length > 3) )
      {
	rv = getKCBasicBegAddr( header, 3 );
      }
      else if( fileFmt.equals( FileFormat.BASIC_PRG ) ) {
	switch( header[ 1 ] & 0xFF ) {
	  case 0x10:
	  case 0x11:
	    rv = 0x1001;
	    break;
	  case 0x04:
	  case 0x05:
	    rv = 0x0401;
	    break;
	  case 0x60:
	  case 0x61:
	    rv = 0x60F7;
	    break;
	  case 0x63:
	    rv = 0x6300;
	    break;
	  case 0x6F:
	  case 0x70:
	    rv = 0x6FB7;
	    break;
	  case 0x2C:
	  case 0x2D:
	    rv = 0x2C01;
	    break;
	  case 0x36:
	  case 0x37:
	    rv = 0x3770;
	    break;
	}
      }
      else if( fileFmt.equals( FileFormat.RBASIC_PRG ) ) {
	rv = 0x8001;
      }
      else if( fileFmt.equals( FileFormat.RMC ) && (header.length > 2) ) {
	rv = EmuUtil.getWord( header, 1 );
      }
      else if( fileFmt.equals( FileFormat.INTELHEX ) && (header.length > 6) ) {
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


  public static int getEndAddr( byte[] header, FileFormat fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileFormat.HEADERSAVE ) && (header.length > 3) ) {
	rv = EmuUtil.getWord( header, 2 );
      }
      else if( fileFmt.equals( FileFormat.KCB ) && (header.length > 20) ) {
	int fileEndAddr = (EmuUtil.getWord( header, 19 ) - 1) & 0xFFFF;
	if( (EmuUtil.getWord( header, 17 ) <= 0x0401)
	    && (fileEndAddr >= 0x0409) )
	{
	  rv = fileEndAddr > 0 ? fileEndAddr : 0xFFFF;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCB_BLKN )
			|| fileFmt.equals( FileFormat.KCB_BLKN_CKS ))
	       && (header.length > 21) )
      {
	int fileEndAddr = (EmuUtil.getWord( header, 20 ) - 1) & 0xFFFF;
	if( (EmuUtil.getWord( header, 18 ) <= 0x0401)
	    && (fileEndAddr >= 0x0409) )
	{
	  rv = fileEndAddr > 0 ? fileEndAddr : 0xFFFF;
	}
      }
      else if( fileFmt.equals( FileFormat.KCC ) && (header.length > 20) ) {
	rv = (EmuUtil.getWord( header, 19 ) - 1) & 0xFFFF;
	if( (rv == 0) && (EmuUtil.getWord( header, 17 ) != 0) ) {
	  rv = 0xFFFF;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCC_BLKN )
			|| fileFmt.equals( FileFormat.KCC_BLKN_CKS ))
	       && (header.length > 21) )
      {
	rv = (EmuUtil.getWord( header, 20 ) - 1) & 0xFFFF;
	if( (rv == 0) && (EmuUtil.getWord( header, 18 ) != 0) ) {
	  rv = 0xFFFF;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCTAP_SYS )
			|| fileFmt.equals( FileFormat.KCTAP_Z9001 ))
	       && (header.length > 37) )
      {
	rv = EmuUtil.getWord( header, 36 ) & 0xFFFF;
	if( (rv == 0) && (EmuUtil.getWord( header, 34 ) != 0) ) {
	  rv = 0xFFFF;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCTAP_KC85 ))
	       && (header.length > 37) )
      {
	rv = (EmuUtil.getWord( header, 36 ) - 1) & 0xFFFF;
	if( (rv == 0) && (EmuUtil.getWord( header, 34 ) != 0) ) {
	  rv = 0xFFFF;
	}
      }
      else if( fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
	       && (header.length > 31) )
      {
	rv = EmuUtil.getWord( header, 28 ) + ((header[ 31 ] & 0xFF) << 8);
      }
      else if( fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
	       && (header.length > 14) )
      {
	rv = EmuUtil.getWord( header, 11 ) + ((header[ 14 ] & 0xFF) << 8);
      }
      else if( (fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS ))
	       && (header.length > 15) )
      {
	rv = EmuUtil.getWord( header, 12 ) + ((header[ 15 ] & 0xFF) << 8);
      }
      else if( fileFmt.equals( FileFormat.KCBASIC_PRG )
	       && (header.length > 3) )
      {
	rv = EmuUtil.getWord( header, 0 ) + ((header[ 3 ] & 0xFF) << 8);
      }
      else if( fileFmt.equals( FileFormat.RMC ) && (header.length > 4) ) {
	rv = EmuUtil.getWord( header, 3 );
      }
    }
    return rv;
  }


  public int getEndAddr()
  {
    return getEndAddr( this.header, this.fileFmt );
  }


  public String getFileDesc()
  {
    return this.fileDesc;
  }


  public static String getFileDesc( byte[] header, FileFormat fileFmt )
  {
    String rv = null;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileFormat.HEADERSAVE ) && (header.length >= 32) ) {
	rv = getFileDesc( header, 16, 16 );
      }
      else if( (fileFmt.equals( FileFormat.KCTAP_SYS )
			|| fileFmt.equals( FileFormat.KCTAP_Z9001 )
			|| fileFmt.equals( FileFormat.KCTAP_KC85 ))
	       && (header.length >= 28) )
      {
	rv = getFileDesc( header, 17, 11 );
	if( rv != null ) {
	  if( rv.length() < 8 ) {
	    String ext = getFileDesc( header, 25, 3 );
	    if( ext != null )
	      rv = rv + "." + ext;
	  }
	}
      }
      else if( (fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
			|| fileFmt.equals( FileFormat.KCTAP_BASIC_DATA )
			|| fileFmt.equals( FileFormat.KCTAP_BASIC_ASC ))
		&& (header.length >= 28) )
      {
	rv = getFileDesc( header, 20, 8 );
      }
      else if( (fileFmt.equals( FileFormat.KCB )
			|| fileFmt.equals( FileFormat.KCC ))
	       && (header.length >= 11) )
      {
	rv = getFileDesc( header, 0, 11 );
	if( rv != null ) {
	  if( rv.length() < 8 ) {
	    String ext = getFileDesc( header, 8, 3 );
	    if( ext != null ) {
	      rv = rv + "." + ext;
	    }
	  }
	}
      }
      else if( (fileFmt.equals( FileFormat.KCB_BLKN )
			|| fileFmt.equals( FileFormat.KCB_BLKN_CKS )
			|| fileFmt.equals( FileFormat.KCC_BLKN )
			|| fileFmt.equals( FileFormat.KCC_BLKN_CKS ))
	       && (header.length >= 12) )
      {
	rv = getFileDesc( header, 1, 11 );
	if( rv != null ) {
	  if( rv.length() < 8 ) {
	    String ext = getFileDesc( header, 9, 3 );
	    if( ext != null ) {
	      rv = rv + "." + ext;
	    }
	  }
	}
      }
      else if( (fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC )
			|| fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA )
			|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG ))
	       && (header.length >= 11) )
      {
	rv = getFileDesc( header, 3, 8 );
      }
      else if( (fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC_BLKN_CKS )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA_BLKN_CKS )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS ))
	       && (header.length >= 12) )
      {
	rv = getFileDesc( header, 4, 8 );
      }
    }
    return rv;
  }


  public FileFormat getFileFormat()
  {
    return this.fileFmt;
  }


  public String getFileText()
  {
    return this.fileText;
  }


  public int getFileType()
  {
    return this.fileType;
  }


  public static int getFileType( byte[] header, FileFormat fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileFormat.HEADERSAVE ) && (header.length > 12) ) {
	rv = (int) header[ 12 ] & 0xFF;
      }
    }
    return rv;
  }


  public String getInfoText()
  {
    return this.infoText;
  }


  public int getNextTAPOffset()
  {
    return this.nextTAPOffs;
  }


  public static int getStartAddr( byte[] header, FileFormat fileFmt )
  {
    int rv = -1;
    if( (header != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileFormat.HEADERSAVE ) && (header.length > 12) ) {
	if( (header[ 12 ] & 0xFF) == 'C' ) {
	  rv = EmuUtil.getWord( header, 4 );
	}
      }
      else if( fileFmt.equals( FileFormat.KCC ) && (header.length > 22) ) {
	rv = EmuUtil.getWord( header, 21 );
	if( rv == 0 ) {
	  rv = -1;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCC_BLKN )
			|| fileFmt.equals( FileFormat.KCC_BLKN_CKS ))
	       && (header.length > 23) )
      {
	rv = EmuUtil.getWord( header, 22 );
	if( rv == 0 ) {
	  rv = -1;
	}
      }
      else if( (fileFmt.equals( FileFormat.KCTAP_SYS )
			|| fileFmt.equals( FileFormat.KCTAP_Z9001 )
			|| fileFmt.equals( FileFormat.KCTAP_KC85 ))
	       && (header.length > 39) )
      {
	rv = EmuUtil.getWord( header, 38 );
	if( rv == 0 ) {
	  rv = -1;
	}
      }
      else if( fileFmt.equals( FileFormat.RMC ) ) {
	rv = EmuUtil.getWord( header, 5 );
      }
    }
    return rv;
  }


  public int getStartAddr()
  {
    return getStartAddr( this.header, this.fileFmt );
  }


  public static boolean isCswMagicAt( byte[] fileBytes, int offs )
  {
    return EmuUtil.isTextAt( CSW_MAGIC, fileBytes, offs );
  }


  public static boolean isKCBasicProgramFormat( FileFormat fileFmt )
  {
    boolean rv = false;
    if( fileFmt != null ) {
      rv = fileFmt.equals( FileFormat.KCB )
		|| fileFmt.equals( FileFormat.KCB_BLKN )
		|| fileFmt.equals( FileFormat.KCB_BLKN_CKS )
		|| fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS )
		|| fileFmt.equals( FileFormat.KCBASIC_PRG );
    }
    return rv;
  }


  public boolean isKCBasicProgramFormat()
  {
    return isKCBasicProgramFormat( this.fileFmt );
  }


  public static boolean isKCTapMagicAt( byte[] fileBytes, int offs )
  {
    return EmuUtil.isTextAt( KCTAP_MAGIC, fileBytes, offs );
  }


  public boolean isTapeFile()
  {
    boolean rv = false;
    if( this.fileFmt != null ) {
      if( this.fileFmt.equals( FileFormat.CDT )
	  || this.fileFmt.equals( FileFormat.CSW )
	  || this.fileFmt.equals( FileFormat.TZX )
	  || this.fileFmt.equals( FileFormat.ZXTAP ) )
      {
	rv = true;
      }
    }
    return rv;
  }


  public static boolean isTzxMagicAt( byte[] fileBytes, int offs )
  {
    return EmuUtil.isTextAt( TZX_MAGIC, fileBytes, offs );
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
    return EmuUtil.readFile( file, false, 0x10000 * 13 );
  }


	/* --- private Konstruktoren und Methoden --- */

  private FileInfo(
		byte[]     header,
		long       fileLen,
		int        fileType,
		FileFormat fileFmt,
		String     fileText,
		String     fileDesc,
		String     addrText,
		int        nextTAPOffs )
  {
    this.header      = header;
    this.fileLen     = fileLen;
    this.fileType    = fileType;
    this.fileFmt     = fileFmt;
    this.fileText    = fileText;
    this.fileDesc    = fileDesc;
    this.addrText    = addrText;
    this.nextTAPOffs = nextTAPOffs;
    this.infoText    = null;
    if( (this.fileText == null) && (fileFmt != null) ) {
      this.fileText = fileFmt.toString();
    }
    if( this.fileText != null ) {
      if( !this.fileText.isEmpty() ) {
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
	this.infoText = buf.toString();
      }
    }
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
	// Segment verarbeiten
	int cnt  = parseHex( in, 2 );
	int addr = parseHex( in, 4 );
	int type = parseHex( in, 2 );
	switch( type ) {
	  case 0:			// Data Record
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
	      }
	    }
	    break;

	  case 1:			// End of File Record
	    loop = false;
	    break;

	  case 2:			// Extended Segment Address Record
	    while( cnt > 0 ) {
	      if( parseHex( in, 2 ) != 0 ) {
		infoMsg = "Die Datei enth\u00E4lt einen Datensatz f\u00FCr"
			+ " eine segmentierte Adresse,\n"
			+ "der von JKCEMU nicht unterst\u00FCtzt wird.\n"
			+ "Es werden nur die Daten bis zu diesem Datensatz"
			+ " geladen.";
	      }
	      --cnt;
	    }
	    break;

	  case 3:			// Start Segment Address Record
	  case 5:			// Start Linear Address Record
	    // Datensatz ignorieren
	    break;

	  case 4:			// Extended Linear Address Record
	    while( cnt > 0 ) {
	      if( parseHex( in, 2 ) != 0 ) {
		infoMsg = "Die Datei enth\u00E4lt einen Datensatz f\u00FCr"
			+ " eine lineare 32-Bit-Adresse,\n"
			+ "die au\u00DFerhalb des von JKCEMU"
			+ " emulierten Adressraumes liegt.\n"
			+ "Es werden nur die Daten bis zu diesem Datensatz"
			+ " geladen.";
	      }
	      --cnt;
	    }
	    break;

	  default:
	    infoMsg = String.format(
			"Die Datei enth\u00E4lt einen Datensatzart"
				+ " des Typs %d,\n"
				+ "der von JKCEMU nicht unterst\u00FCtzt"
				+ " wird.\n"
				+ "Es werden nur die Daten bis zu diesem"
				+ " Datensatz geladen.",
			type );
	}
	if( infoMsg != null ) {
	  if( out.size() == 0 ) {
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
			FileFormat.INTELHEX );
	}
      }
    }
    if( rv == null ) {
      new IOException( "Laden als Intel-HEX-Datei nicht m\u00F6glich" );
    }
    rv.setInfoMsg( infoMsg );
    return rv;
  }


  /*
   * In einer KC-TAP-Datei wird geweohnlich die Endadresse + 1 eingetragen.
   * Es gibt aber auch Faelle,
   * in denen die tatsaechlie Endadresse eingetragen ist.
   * Damit kein Byte zu wenig geladen wird, wird so getan,
   * als ob die tatsaechliche Endadresse eingetragen ist.
   */
  private static LoadData createLoadDataFromKCTAP(
				byte[]     fileBuf,
				FileFormat fileFmt ) throws IOException
  {
    int     begAddr   = -1;
    int     startAddr = -1;
    int     len       = 0;
    int     pos       = 0;
    int     blkRemain = 0;
    boolean kcbasic   = false;
    if( (fileFmt.equals( FileFormat.KCTAP_SYS )
		|| fileFmt.equals( FileFormat.KCTAP_Z9001 )
		|| fileFmt.equals( FileFormat.KCTAP_KC85 ))
	&& (fileBuf.length > 39) )
    {
      begAddr   = EmuUtil.getWord( fileBuf, 34 );
      len       = (EmuUtil.getWord( fileBuf, 36 ) - begAddr + 1) & 0xFFFF;
      startAddr = EmuUtil.getWord( fileBuf, 38 );
      if( startAddr == 0 ) {
	startAddr = -1;
      }
      pos = 145;
    }
    else if( fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
	     && (fileBuf.length > 31) )
    {
      begAddr   = getKCBasicBegAddr( fileBuf, 31 );
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


  private static int getKCBasicBegAddr( byte[] header, int pos )
  {
    int rv = -1;
    if( header != null ) {
      if( (pos >= 0) && (pos < header.length) ) {
	rv = (((int) header[ pos ]) & 0xFF) < 0x2C ? 0x0401 : 0x2C01;
      }
    }
    return rv;
  }


  private static boolean hasKCBlkNums(
				byte[]        header,
				AtomicBoolean cksOut )
  {
    boolean rv     = false;
    boolean hasCks = false;
    if( header != null ) {
      if( header.length > 0 ) {
	int blkNum = (int) header[ 0 ] & 0xFF;
	if( (blkNum == 0) || (blkNum == 1) ) {
	  if( header.length > 129 ) {
	    int cks = 0;
	    for( int i = 1; i <= 128; i++ ) {
	      cks += ((int) header[ i ] & 0xFF);
	    }
	    if( (cks & 0xFF) == ((int) header[ 129 ] & 0xFF) ) {
	      hasCks = true;
	    }
	  }
	  rv = true;
	  int blkSize = (hasCks ? 130 : 129);
	  int pos     = blkSize;
	  while( pos < header.length ) {
	    int b = (int) header[ pos ] & 0xFF;
	    if( b == 0xFF ) {
	      if( (pos + blkSize) < header.length ) {
		rv = false;		// nicht der letzte Block
	      }
	      break;
	    }
	    if( b != (blkNum + 1) ) {
	      rv = false;		// nicht aufsteigend
	      break;
	    }
	    blkNum++;
	    pos += blkSize;
	  }
	}
      }
    }
    if( cksOut != null ) {
      cksOut.set( hasCks );
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


  private static byte[] removeKCBlockNums( byte[] fileBuf )
  {
    byte[] rv = fileBuf;
    if( fileBuf.length > 0 ) {
      ByteArrayOutputStream buf = new ByteArrayOutputStream( fileBuf.length );
      int pos = 0;
      while( pos < fileBuf.length ) {
	int b = (int) fileBuf[ pos ];
	if( (pos % 129) != 0 ) {
	  buf.write( b );
	}
	pos++;
      }
      rv = buf.toByteArray();
    }
    return rv;
  }


  private static byte[] removeKCBlockNumsAndChecksums( byte[] fileBuf )
  {
    byte[] rv = fileBuf;
    if( fileBuf.length > 0 ) {
      ByteArrayOutputStream buf = new ByteArrayOutputStream( fileBuf.length );
      int pos = 0;
      while( pos < fileBuf.length ) {
	int b = (int) fileBuf[ pos ];
	if( ((pos % 130) != 0) && ((pos % 130) != 129) ) {
	  buf.write( b );
	}
	pos++;
      }
      rv = buf.toByteArray();
    }
    return rv;
  }
}
