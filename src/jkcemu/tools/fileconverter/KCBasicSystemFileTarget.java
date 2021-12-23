/*
 * (c) 2012-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine KCB-Datei
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


public class KCBasicSystemFileTarget extends AbstractConvertTarget
{
  // Speicherbereich 0300h-03D6h entsprechend KC85/4
  private static final byte[] mem0300 = {
	(byte) 0xC3, (byte) 0x89, (byte) 0xC0, (byte) 0xC3,	// 0300h
	(byte) 0x67, (byte) 0xC9, (byte) 0x00, (byte) 0x00,
	(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xD6,
	(byte) 0x00, (byte) 0x6F, (byte) 0x7C, (byte) 0xDE,
	(byte) 0x00, (byte) 0x67, (byte) 0x78, (byte) 0xDE,	// 0310h
	(byte) 0x00, (byte) 0x47, (byte) 0x3E, (byte) 0x00,
	(byte) 0xC9, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x35, (byte) 0x4A, (byte) 0xCA, (byte) 0x99,
	(byte) 0x39, (byte) 0x1C, (byte) 0x76, (byte) 0x98,	// 0320h
	(byte) 0x22, (byte) 0x95, (byte) 0xB3, (byte) 0x98,
	(byte) 0x0A, (byte) 0xDD, (byte) 0x47, (byte) 0x98,
	(byte) 0x53, (byte) 0xD1, (byte) 0x99, (byte) 0x99,
	(byte) 0x0A, (byte) 0x1A, (byte) 0x9F, (byte) 0x98,	// 0330h
	(byte) 0x65, (byte) 0xBC, (byte) 0xCD, (byte) 0x98,
	(byte) 0xD6, (byte) 0x77, (byte) 0x3E, (byte) 0x98,
	(byte) 0x52, (byte) 0xC7, (byte) 0x4F, (byte) 0x80,
	(byte) 0x0B, (byte) 0xFF, (byte) 0x1B, (byte) 0x00,	// 0340h
	(byte) 0x0A, (byte) 0x00, (byte) 0x0A, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0xC3, (byte) 0xAE,
	(byte) 0xC5, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,	// 0350h
	(byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xBE,
	(byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
	(byte) 0xC9, (byte) 0x00, (byte) 0x00, (byte) 0x01,
	(byte) 0x04, (byte) 0x00, (byte) 0x20, (byte) 0x00,	// 0360h
	(byte) 0x20, (byte) 0x8F, (byte) 0xC0, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,	// 0370h
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,	// 0380h
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,	// 0390h
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,	// 03A0h
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
	(byte) 0xFF, (byte) 0xBF, (byte) 0xB4, (byte) 0x03,	// 03B0h
	(byte) 0x03, (byte) 0x00, (byte) 0x1D, (byte) 0xC3,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x03, (byte) 0x00, (byte) 0x1D, (byte) 0xC3,	// 03C0h
	(byte) 0xFF, (byte) 0xBF, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,	// 03D0h
	(byte) 0x00, (byte) 0x00, (byte) 0x00 };

  // Speicherbereich 03D8h-03D6h entsprechend KC85/4
  private static final byte[] mem03DD = {
	(byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0xB4, (byte) 0x03, (byte) 0x00, (byte) 0x30,
	(byte) 0xF4, (byte) 0x20, (byte) 0x34, (byte) 0x37,
	(byte) 0x38, (byte) 0x35, (byte) 0x34, (byte) 0x00,
	(byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xAF,
	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };


  private byte[] dataBytes;
  private int    offs;
  private int    len;


  public KCBasicSystemFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len )
  {
    super( fileConvertFrm, "KC-Systemdatei mit KC-BASIC-Programm (*.kcb)" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return FileUtil.getKCBasicSystemFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return 11;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return FileUtil.replaceExtension( srcFile, ".kcb" );
  }


  @Override
  public String save( File file ) throws IOException, UserInputException
  {
    checkFileExtension( file, ".kcb" );
    int          endAddr   = 0x0401 + this.len;
    String       fileDesc  = this.fileConvertFrm.getFileDesc( true );
    OutputStream out       = null;
    try {
      out = new FileOutputStream( file );
      FileSaver.writeKCHeader(
			out,
			0x0300,
			endAddr - 1,
			null,
			false,
			fileDesc,
			null );
      out.write( mem0300 );
      for( int i = 0; i < 3; i++ ) {
	out.write( endAddr & 0xFF );
	out.write( (endAddr >> 8) & 0xFF );
      }
      out.write( mem03DD );
      int n = Math.min( this.dataBytes.length - this.offs, this.len );
      out.write( this.dataBytes, this.offs, n );
      n = (mem0300.length + 6 + mem03DD.length + n) % 0x80;
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
}
