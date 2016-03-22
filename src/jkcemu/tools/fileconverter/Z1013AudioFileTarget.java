/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im Z1013-Format
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.AudioInputStream;
import javax.swing.JComboBox;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.emusys.z1013.Z1013AudioDataStream;


public class Z1013AudioFileTarget extends AbstractConvertTarget
{
  private byte[]  fileBytes;
  private byte[]  dataBytes;
  private int     offs;
  private int     len;
  private boolean headersave;


  public Z1013AudioFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len,
		boolean        headersave )
  {
    super( fileConvertFrm, createInfoText( headersave ) );
    this.fileBytes  = null;
    this.dataBytes  = dataBytes;
    this.offs       = offs;
    this.len        = len;
    this.headersave = headersave;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canPlay()
  {
    return true;
  }


  @Override
  public AudioInputStream getAudioInputStream() throws UserInputException
  {
    if( this.headersave && (this.fileBytes == null) ) {
      int len = Math.min( this.len, this.dataBytes.length - this.offs );
      if( len > 0 ) {
	int begAddr   = this.fileConvertFrm.getBegAddr( true );
	int endAddr   = begAddr + len - 1;
	int startAddr = this.fileConvertFrm.getBegAddr( false );
	if( startAddr < 0 ) {
	  startAddr = 0;
	}
	int fileType = this.fileConvertFrm.getFileTypeChar( true );
	if( fileType < 0 ) {
	  fileType = 0x20;
	}
	String s = this.fileConvertFrm.getFileDesc( true );
	byte[] m = new byte[ 32 + len ];
	m[ 0 ]   = (byte) begAddr;
	m[ 1 ]   = (byte) (begAddr >> 8);
	m[ 2 ]   = (byte) endAddr;
	m[ 3 ]   = (byte) (endAddr >> 8);
	m[ 4 ]   = (byte) startAddr;
	m[ 5 ]   = (byte) (startAddr >> 8);
	m[ 6 ]   = (byte) 'J';
	m[ 7 ]   = (byte) 'K';
	m[ 8 ]   = (byte) 'C';
	m[ 9 ]   = (byte) 'E';
	m[ 10 ]  = (byte) 'M';
	m[ 11 ]  = (byte) 'U';
	m[ 12 ]  = (byte) fileType;
	m[ 13 ]  = (byte) 0xD3;
	m[ 14 ]  = (byte) 0xD3;
	m[ 15 ]  = (byte) 0xD3;
	int p = 16;
	if( s != null ) {
	  int l = s.length();
	  int i = 0;
	  while( (p < 32) && (i < l) ) {
	    m[ p++ ] = (byte) s.charAt( i++ );
	  }
	}
	while( p < 32 ) {
	  m[ p++ ] = (byte) 0x20;
	}
	System.arraycopy( this.dataBytes, this.offs, m, p, len );
	this.fileBytes = m;
      }
    }
    Z1013AudioDataStream ads = null;
    if( this.headersave && (this.fileBytes != null) ) {
      ads = new Z1013AudioDataStream(
				true,
				this.fileBytes,
				0,
				this.fileBytes.length );

    } else {
      ads = new Z1013AudioDataStream(
				false,
				this.dataBytes,
				this.offs,
				this.len );
    }
    return new AudioInputStream(
			ads,
			ads.getAudioFormat(),
			ads.getFrameLength() );
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioUtil.getAudioOutFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return this.headersave ? 16 : 0;
  }


  @Override
  public int getMaxFileTypeLength()
  {
    return this.headersave ? 1 : 0;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtensionToAudioFile( srcFile );
  }


  @Override
  public String save( File file ) throws IOException, UserInputException
  {
    saveAudioFile( file, getAudioInputStream() );
    return null;
  }


  @Override
  public void setFileTypesTo( JComboBox<String> combo )
  {
    if( this.headersave ) {
      HeadersaveFileTarget.setFileTypesTo( combo, this.fileConvertFrm );
    } else {
      super.setFileTypesTo( combo );
    }
  }


  @Override
  public boolean usesBegAddr()
  {
    return this.headersave;
  }


  @Override
  public boolean usesStartAddr( int fileType )
  {
    return (fileType == 'C') || (fileType == 'M');
  }


	/* --- private Methoden --- */

  private static String createInfoText( boolean headersave )
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "Sound-Datei im Z1013-" );
    if( headersave ) {
      buf.append( "Headersave-" );
    }
    buf.append( "Format" );
    AudioUtil.appendAudioFileExtensionText(
			buf,
			3,
			AudioUtil.getAudioOutFileExtensions( null, null ) );
    return buf.toString();
  }
}
