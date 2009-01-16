/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen fuer Audio
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;


public class AudioUtil
{
  public static String getAudioFormatText( AudioFormat fmt )
  {
    StringBuilder buf = new StringBuilder( 64 );
    if( fmt != null ) {
      buf.append( (int) fmt.getSampleRate() );
      buf.append( " Hz, " );

      buf.append( fmt.getSampleSizeInBits() );
      buf.append( " Bit, " );

      int channels = fmt.getChannels();
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
    return buf.toString();
  }
}

