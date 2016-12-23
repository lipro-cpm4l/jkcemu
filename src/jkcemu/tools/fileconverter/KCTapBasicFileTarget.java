/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine TAP-Datei (KC-BASIC-Format)
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileInfo;
import jkcemu.base.UserInputException;


public class KCTapBasicFileTarget extends AbstractConvertTarget
{
  private byte[] dataBytes;
  private int    offs;
  private int    len;


  public KCTapBasicFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len )
  {
    super( fileConvertFrm, "KC-TAP-BASIC-Datei" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getKCTapFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return 8;
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
    String       fileDesc = this.fileConvertFrm.getFileDesc( true );
    OutputStream out      = null;
    try {
      out = new FileOutputStream( file );

      // KC-TAP-Kopf
      int n = FileInfo.KCTAP_MAGIC.length();
      for( int i = 0; i < n; i++ ) {
	out.write( FileInfo.KCTAP_MAGIC.charAt( i ) );
      }

      // Datenbloecke
      boolean isFirst = true;
      boolean eofDone = false;
      int     pos     = this.offs;
      int     blkNum  = 1;
      int     nRemain = Math.min(
				this.len,
				this.dataBytes.length - this.offs );
      while( nRemain > 0 ) {
	if( nRemain > 127 ) {		// noch anzuhaengende 0x03 beachten!
	  out.write( blkNum++ );
	} else {
	  out.write( 0xFF );
	}
	int blkRemain = 128;
	if( isFirst ) {
	  out.write( 0xD3 );
	  out.write( 0xD3 );
	  out.write( 0xD3 );
	  n = 8;
	  if( fileDesc != null ) {
	    int l = fileDesc.length();
	    int p = 0;
	    while( (n > 0) && (p < l) ) {
	      out.write( fileDesc.charAt( p++ ) & 0xFF );
	      --n;
	    }
	  }
	  while( n > 0 ) {
	    out.write( 0x20 );
	    --n;
	  }
	  out.write( this.len );
	  out.write( this.len >> 8 );
	  blkRemain -= 13;
	  isFirst = false;
	}
	n = Math.min( nRemain, blkRemain );
	out.write( this.dataBytes, pos, n );
	pos       += n;
	nRemain   -= n;
	blkRemain -= n;
	if( (blkRemain > 0) && (nRemain == 0) ) {
	  out.write( 0x03 );
	  eofDone = true;
	  --blkRemain;
	}
	while( blkRemain > 0 ) {
	  out.write( 0 );
	  --blkRemain;
	}
      }
      // ggf. separater Block mit EOF-Zeichen
      if( !eofDone ) {
	out.write( 0xFF );	// Blocknummer
	out.write( 0x03 );	// EOF
	for( int i = 0; i < 127; i++ ) {
	  out.write( 0 );
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
    return null;
  }
}
