/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen fuer Audio
 */

package jkcemu.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.swing.JComboBox;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.emusys.kc85.KCAudioCreator;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioCreator;
import jkcemu.file.FileInfo;
import jkcemu.file.FileUtil;


public class AudioUtil
{
  public static final String[] tapeFileExtensions = {
					".cdt", ".csw", ".tap", ".tzx" };

  public static final int FILE_READ_MAX         = 0x1000000;
  public static final int RECORDING_MINUTES_MAX = 120;


  private static final int[] frameRates = {
				96000, 48000, 44100, 45454, 32000,
				22050, 16000, 11025, 8000 };

  public static class MixerItem
  {
    private Mixer.Info mixerInfo;

    public MixerItem( Mixer.Info mixerInfo )
    {
      this.mixerInfo = mixerInfo;
    }

    public Mixer.Info getMixerInfo()
    {
      return this.mixerInfo;
    }

    @Override
    public String toString()
    {
      String s = this.mixerInfo.getName();
      if( s != null ) {
	if( s.isEmpty() ) {
	  s = null;
	}
      }
      return s != null ? s : this.mixerInfo.toString();
    }
  };


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


  public static void appendMixerItemsTo(
				JComboBox<Object> comboBox,
				boolean           forTargetLines )
  {
    comboBox.addItem( EmuUtil.TEXT_DEFAULT );
    for( Mixer.Info mixerInfo : AudioSystem.getMixerInfo() ) {
      try {
	Mixer       mixer         = AudioSystem.getMixer( mixerInfo );
	Line.Info[] lineInfoArray = (forTargetLines ?
					mixer.getTargetLineInfo()
					: mixer.getSourceLineInfo());
	if( lineInfoArray != null ) {
	  for( Line.Info lineInfo : lineInfoArray ) {
	    if( lineInfo instanceof DataLine.Info ) {
	      comboBox.addItem( new MixerItem( mixerInfo ) );
	      break;
	    }
	  }
	}
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  public static JComboBox<Object> createFrameRateComboBox()
  {
    JComboBox<Object> comboBox = GUIFactory.createComboBox();
    comboBox.setEditable( false );
    comboBox.addItem( EmuUtil.TEXT_DEFAULT );
    for( int i = 0; i < frameRates.length; i++ ) {
      comboBox.addItem( frameRates[ i ] );
    }
    return comboBox;
  }


  public static JComboBox<Object> createMixerComboBox(
					boolean forTargetLines )
  {
    JComboBox<Object> comboBox = GUIFactory.createComboBox();
    comboBox.setEditable( false );
    appendMixerItemsTo( comboBox, forTargetLines );
    return comboBox;
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


  public static Mixer.Info getSelectedMixerInfo( JComboBox<Object> comboBox )
  {
    Mixer.Info mixerInfo = null;
    Object     item       = comboBox.getSelectedItem();
    if( item != null ) {
      if( item instanceof MixerItem ) {
	mixerInfo = ((MixerItem) item).getMixerInfo();
      }
    }
    return mixerInfo;
  }


  public static int getSelectedFrameRate( JComboBox<Object> comboBox )
  {
    int    rv = 0;
    Object o  = comboBox.getSelectedItem();
    if( o != null ) {
      if( o instanceof Integer ) {
	rv = ((Integer) o).intValue();
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


  public static boolean isAudioFile( byte[] header )
  {
    boolean rv = false;
    try {
      if( AudioFile.getInfo( header ) != null ) {
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
	fileBytes = FileUtil.readFile( file, true, FILE_READ_MAX );
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
