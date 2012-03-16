/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine KCC-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import jkcemu.base.*;


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
    return EmuUtil.getKCSystemFileFilter();
  }


  @Override
  public int getMaxFileDescLen()
  {
    return 11;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".kcc" );
  }


  @Override
  public void save( File file ) throws IOException, UserInputException
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
			begAddr + this.len,
			startAddr >= 0 ? new Integer( startAddr ) : null,
			fileDesc );
      out.write(
		this.dataBytes,
		this.offs,
		Math.min( this.dataBytes.length - this.offs, this.len ) );
      out.close();
      out = null;
    }
    finally {
      EmuUtil.doClose( out );
    }
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
