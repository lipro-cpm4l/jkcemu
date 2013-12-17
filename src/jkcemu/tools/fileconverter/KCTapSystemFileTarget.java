/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine TAP-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import jkcemu.base.*;


public class KCTapSystemFileTarget extends AbstractConvertTarget
{
  private byte[]  dataBytes;
  private int     offs;
  private int     len;
  private boolean z9001;


  public KCTapSystemFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len,
		boolean        z9001 )
  {
    super(
	fileConvertFrm,
	z9001 ?
	    "KC-TAP-Systemdatei f\u00FCr KC85/1, KC87 und Z9001 (*.tap)"
	    : "KC-TAP-Systemdatei f\u00FCr HC900 und KC85/2..5 (*.tap)" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
    this.z9001     = z9001;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getTapFileFilter();
  }


  @Override
  public int getMaxFileDescLen()
  {
    return 11;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".tap" );
  }


  @Override
  public String save( File file ) throws IOException, UserInputException
  {
    checkFileExtension( file, ".tap" );
    int          blkNum    = (this.z9001 ? 0 : 1);
    int          begAddr   = this.fileConvertFrm.getBegAddr( true );
    int          startAddr = this.fileConvertFrm.getStartAddr( false );
    String       fileDesc  = this.fileConvertFrm.getFileDesc( true );
    OutputStream out       = null;
    try {
      out = new FileOutputStream( file );

      // KC-TAP-Header
      int n = FileInfo.KCTAP_HEADER.length();
      for( int i = 0; i < n; i++ ) {
	out.write( FileInfo.KCTAP_HEADER.charAt( i ) );
      }

      // Kopfblock
      out.write( blkNum++ );
      FileSaver.writeKCHeader(
		out,
		begAddr,
		this.z9001 ?
			begAddr
			: ((begAddr + this.len - 1) & 0xFFFF),
		startAddr >= 0 ? new Integer( startAddr ) : null,
		fileDesc );

      // Datenbloecke
      int offs    = this.offs;
      int nRemain = Math.min( this.len, this.dataBytes.length - this.offs );
      while( nRemain > 0 ) {
	if( nRemain > 128 ) {
	  out.write( blkNum++ );
	} else {
	  out.write( 0xFF );
	}
	out.write( this.dataBytes, offs, Math.min( nRemain, 128 ) );
	offs    += 128;
	nRemain -= 128;
	while( nRemain < 0 ) {
	  out.write( 0 );
	  nRemain++;
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.doClose( out );
    }
    return null;
  }


  @Override
  public boolean usesBegAddr()
  {
    return true;
  }


  @Override
  public boolean usesStartAddr( int fileType )
  {
    return true;
  }
}
