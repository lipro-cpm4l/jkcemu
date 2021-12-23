/*
 * (c) 2011-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Intel-HEX-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import jkcemu.base.EmuUtil;
import jkcemu.base.UserInputException;
import jkcemu.file.FileUtil;


public class IntelHexFileTarget extends AbstractConvertTarget
{
  private byte[] dataBytes;
  private int    offs;
  private int    len;


  public IntelHexFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len )
  {
    super( fileConvertFrm, "Intel-HEX-Datei (*.hex)" );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return FileUtil.getHexFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return FileUtil.replaceExtension( srcFile, ".hex" );
  }


  @Override
  public String save( File file ) throws IOException, UserInputException
  {
    checkFileExtension( file, ".hex" );
    int    addr = this.fileConvertFrm.getBegAddr( true );
    Writer out  = null;
    try {
      out = new FileWriter( file );

      int pos    = this.offs;
      int eof    = Math.min( this.offs + this.len, this.dataBytes.length );
      int segLen = Math.min( eof - pos, 0x20 );
      while( segLen > 0 ) {
	int cks = segLen & 0xFF;
	cks += (addr & 0xFF);
	cks += ((addr >> 8) & 0xFF);
	out.write( String.format( ":%02X%04X00", segLen, addr ) );
	for( int i = 0; i < segLen; i++ ) {
	  int b = (int) this.dataBytes[ pos++ ] & 0xFF;
	  out.write( String.format( "%02X", b ) );
	  cks += b;
	}
	out.write( String.format( "%02X", -cks & 0xFF ) );
	out.write( 0x0D );
	out.write( 0x0A );
	addr += segLen;
	segLen = Math.min( eof - pos, 0x20 );
      }
      out.write( ":00000001FF" );
      out.write( 0x0D );
      out.write( 0x0A );
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
}
