/*
 * (c) 2011-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine KCC-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import jkcemu.base.EmuUtil;
import jkcemu.base.UserInputException;
import jkcemu.file.FileSaver;
import jkcemu.file.FileUtil;


public class KCSystemFileTarget extends AbstractConvertTarget
{
  private byte[] dataBytes;
  private int    offs;
  private int    len;


  public KCSystemFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len )
  {
    super( fileConvertFrm, "KC-Systemdatei (*.kcc)" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return FileUtil.getKCSystemFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return 11;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return FileUtil.replaceExtension( srcFile, ".kcc" );
  }


  @Override
  public String save( File file ) throws IOException, UserInputException
  {
    checkFileExtension( file, ".kcc" );
    int          begAddr   = this.fileConvertFrm.getBegAddr( true );
    int          startAddr = this.fileConvertFrm.getStartAddr( false );
    String       fileDesc  = this.fileConvertFrm.getFileDesc( true );
    OutputStream out       = null;
    try {
      out = new FileOutputStream( file );
      FileSaver.writeKCHeader(
		out,
		begAddr,
		(begAddr + this.len - 1) & 0xFFFF,
		startAddr >= 0 ? startAddr : null,
		false,
		fileDesc,
		null );
      int n = Math.min( this.dataBytes.length - this.offs, this.len );
      out.write( this.dataBytes, this.offs, n );
      n = n % 0x80;
      if( n > 0 ) {
	for( int i = n; i < 0x80; i++ ) {
	  out.write( 0 );
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilently( out );
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
