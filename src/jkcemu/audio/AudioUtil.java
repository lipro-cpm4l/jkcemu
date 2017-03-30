/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen fuer Audio
 */

package jkcemu.audio;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileInfo;
import jkcemu.emusys.kc85.KCAudioCreator;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioCreator;


public class AudioUtil
{
  public static final String[] tapeFileExtensions = {
					".852", ".853", ".854",
					".cdt", ".csw", ".tap", ".tzx" };

  public static final String ERROR_TEXT_LINE_UNAVAILABLE =
	"Der Audiokanal kann nicht ge\u00F6ffnet werden,\n"
		+ "da er bereits durch eine andere Anwendung benutzt wird.";

  public static final String ERROR_RECORDING_OUT_OF_MEMORY =
	"Kein Speicher mehr f\u00FCr die Aufzeichnung\n"
		+ "der Audiodaten verf\u00FCgbar.";

  public static final int FILE_READ_MAX         = 0x1000000;
  public static final int RECORDING_MINUTES_MAX = 120;


  public static void appendAudioFormatText(
				StringBuilder buf,
				PCMDataInfo   info )
  {
    if( info != null ) {
      buf.append( info.getFrameRate() );
      buf.append( " Hz, " );

      buf.append( info.getSampleSizeInBits() );
      buf.append( " Bit, " );

      int channels = info.getChannels();
      switch( channels ) {
	case 1:
	  buf.append( "Mono" );
	  break;
	case 2:
	  buf.append( "Stereo" );
	  break;
	default:
	  buf.append( channels );
	  buf.append( " Kan\u00E4le" );
	  break;
      }
    }
  }


  public static String getAudioFormatText( PCMDataInfo info )
  {
    StringBuilder buf = new StringBuilder( 64 );
    appendAudioFormatText( buf, info );
    return buf.toString();
  }


  public static String getDurationText( int frameRate, long frameCount )
  {
    String rv = null;
    if( (frameRate > 0) && (frameCount > 0) ) {
      long sec = (frameCount / frameRate);
      if( sec < 60 ) {
	rv = String.format( "%d Sekunden", sec );
      } else if( sec < 3600 ) {
	rv = String.format( "%d:%02d Minuten", sec / 60, sec % 60 );
      } else {
	rv = String.format(
		"%d:%02d:%02d Stunden",
		sec / 3600,
		(sec % 3600) / 60,
		sec % 60 );
      }
    }
    return rv;
  }


  public static boolean isAudioFile( File file )
  {
    boolean rv = false;
    try {
      if( AudioFile.getInfo( file ) != null ) {
	rv = true;
      }
    }
    catch( Exception ex ) {}
    return rv;
  }


  public static PCMDataSource openAudioOrTapeFile( File file )
							throws IOException
  {
    PCMDataSource pcm       = null;
    byte[]        fileBytes = null;
    boolean       isTAP     = false;
    if( file.isFile() ) {
      String fName = file.getName();
      if( fName != null ) {
	fName     = fName.toLowerCase();
	isTAP     = fName.endsWith( ".tap" ) || fName.endsWith( ".tap.gz" );
	fileBytes = EmuUtil.readFile( file, true, FILE_READ_MAX );
      }
    }
    if( fileBytes != null ) {
      if( FileInfo.isCswMagicAt( fileBytes, 0 ) ) {
	pcm = CSWFile.getPCMDataSource( fileBytes, 0 );
      } else if( FileInfo.isKCTapMagicAt( fileBytes, 0 ) ) {
	pcm = new KCAudioCreator(
				true,
				0,
				fileBytes,
				0,
				fileBytes.length ).newReader();
      } else {
	if( isTAP || FileInfo.isTzxMagicAt( fileBytes, 0 ) ) {
	  pcm = new ZXSpectrumAudioCreator(
				fileBytes,
				0,
				fileBytes.length ).newReader();
	}
      }
    }
    if( pcm == null ) {
      pcm = AudioFile.open( file, fileBytes );
    }
    return pcm;
  }


  public static void throwNoAudioData() throws IOException
  {
    throw new IOException( "Keine Audiodaten vorhanden" );
  }
}
