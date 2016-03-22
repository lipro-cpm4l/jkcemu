/*
 * (c) 2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im ZX Spectrum Format
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.AudioInputStream;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioDataStream;


public class ZXSpectrumAudioFileTarget extends AbstractConvertTarget
{
  private byte[] fileBytes;


  public ZXSpectrumAudioFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         fileBytes )
  {
    super( fileConvertFrm, createInfoText() );
    this.fileBytes = fileBytes;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canPlay()
  {
    return true;
  }


  @Override
  public AudioInputStream getAudioInputStream() throws IOException
  {
    AudioInputStream rv = null;
    if( this.fileBytes != null ) {
      ZXSpectrumAudioDataStream ads = new ZXSpectrumAudioDataStream(
					this.fileBytes,
					0,
					this.fileBytes.length );
      rv = new AudioInputStream(
			ads,
			ads.getAudioFormat(),
			ads.getFrameLength() );

    }
    return rv;
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioUtil.getAudioOutFileFilter();
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


	/* --- private Methoden --- */

  private static String createInfoText()
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "Sound-Datei im ZX Spectrum Format" );
    AudioUtil.appendAudioFileExtensionText(
			buf,
			3,
			AudioUtil.getAudioOutFileExtensions( null, null ) );
    return buf.toString();
  }
}
