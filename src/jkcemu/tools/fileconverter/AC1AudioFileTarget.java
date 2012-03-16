/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im AC1-Format
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.AudioInputStream;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.emusys.ac1_llc2.AC1AudioDataStream;


public class AC1AudioFileTarget extends AbstractConvertTarget
{
  private byte[]  dataBytes;
  private int     offs;
  private int     len;
  private boolean basic;


  public AC1AudioFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len,
		boolean        basic )
  {
    super( fileConvertFrm, createInfoText( basic ) );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
    this.basic     = basic;
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
    AC1AudioDataStream ads = new AC1AudioDataStream(
			this.basic,
			this.dataBytes,
			this.offs,
			this.len,
			this.fileConvertFrm.getFileDesc( true ),
			this.fileConvertFrm.getBegAddr( true ),
			this.fileConvertFrm.getStartAddr( false ) );
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
  public int getMaxFileDescLen()
  {
    return this.basic ? 6 : 16;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtensionToAudioFile( srcFile );
  }


  @Override
  public void save( File file ) throws IOException, UserInputException
  {
    saveAudioFile( file, getAudioInputStream() );
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


	/* --- private Methoden --- */

  private static String createInfoText( boolean basic )
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "Sound-Datei im AC1-" );
    if( basic ) {
      buf.append( "BASIC-" );
    }
    buf.append( "Format" );
    AudioUtil.appendAudioFileExtensionText(
			buf,
			3,
			AudioUtil.getAudioOutFileExtensions( null, null ) );
    return buf.toString();
  }
}
