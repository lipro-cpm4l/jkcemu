/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine SSS-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import jkcemu.base.*;


public class KCBasicFileTarget extends AbstractConvertTarget
{
  private byte[] dataBytes;
  private int    offs;
  private int    len;


  public KCBasicFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len )
  {
    super( fileConvertFrm, "KC-BASIC-Programmdatei (*.sss)" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getKCBasicFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".sss" );
  }


  @Override
  public void save( File file ) throws IOException
  {
    checkFileExtension( file, ".sss" );
    OutputStream out = null;
    int          len = Math.min( this.dataBytes.length - this.offs, this.len );
    try {
      out = new FileOutputStream( file );
      out.write( len & 0xFF );
      out.write( len >> 8 );
      out.write( this.dataBytes, this.offs, len );
      out.write( 0x03 );
      int n = (len + 3) % 0x80;
      if( n > 0 ) {
	for( int i = n; i < 0x80; i++ ) {
	  out.write( 0 );
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.doClose( out );
    }
  }
}
