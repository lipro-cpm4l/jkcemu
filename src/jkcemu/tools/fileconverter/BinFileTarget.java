/*
 * (c) 2011-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine BIN-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileUtil;


public class BinFileTarget extends AbstractConvertTarget
{
  private byte[] dataBytes;
  private int    offs;
  private int    len;
  private int    begAddr;
  private int    endAddr;
  private int    startAddr;


  public BinFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len,
		int            begAddr,
		int            endAddr,
		int            startAddr )
  {
    super( fileConvertFrm, "Einfache Speicherabbilddatei (*.bin)" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
    this.begAddr   = begAddr;
    this.endAddr   = endAddr;
    this.startAddr = startAddr;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return FileUtil.getBinaryFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    File outFile = null;
    if( srcFile != null ) {
      String fName = srcFile.getName();
      if( fName != null ) {
	int pos = fName.lastIndexOf( '.' );
	if( pos >= 0 ) {
	  fName = fName.substring( 0, pos );
	}
	if( !fName.isEmpty() ) {
	  String addrExt   = "";
	  if( this.begAddr >= 0 ) {
	    addrExt = String.format( "_%04X", this.begAddr );
	    if( fName.toUpperCase().indexOf( addrExt ) < 0 ) {
	      if( this.endAddr >= 0 ) {
		if( this.startAddr >= 0 ) {
		  addrExt = String.format(
				"_%04X_%04X_%04X",
				this.begAddr,
				this.endAddr,
				this.startAddr );
		} else {
		  addrExt = String.format(
				"_%04X_%04X",
				this.begAddr,
				this.endAddr );
		}
	      }
	      if( !fName.toUpperCase().endsWith( addrExt ) ) {
		fName += addrExt;
	      }
	    }
	  }
	  if( fName.endsWith( "." ) ) {
	    fName += "bin";
	  } else {
	    fName += ".bin";
	  }
	  File dirFile = srcFile.getParentFile();
	  if( dirFile != null ) {
	    outFile = new File( dirFile, fName );
	  } else {
	    outFile = new File( fName );
	  }
	}
      }
    }
    return outFile;
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".bin", ".rom" );
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );
      out.write(
		this.dataBytes,
		this.offs,
		Math.min( this.dataBytes.length - this.offs, this.len ) );
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilently( out );
    }
    return null;
  }
}
