/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Repraesentation eines Tongenerators auf Basis eines PSG
 */

package jkcemu.etc;

import jkcemu.audio.AbstractSoundDevice;
import jkcemu.audio.AudioOut;


public class PSGSoundDevice extends AbstractSoundDevice
{
  private PSG8910 psg;


  public PSGSoundDevice( String text, boolean stereo, PSG8910 psg )
  {
    super( text, stereo );
    this.psg = psg;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean isSingleBitMode()
  {
    return false;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.psg.reset();
  }


  @Override
  public synchronized void setAudioOut( AudioOut audioOut )
  {
    super.setAudioOut( audioOut );

    /*
     * Der PSG benoetigt eine Framerate, um anlaufen zu koennen.
     * Das ist notwendig, da der Audiokanal erst bei der Ausgabe
     * von Daten geoeffnet.
     */
    if( audioOut != null ) {
      int frameRate = audioOut.getFrameRate();
      if( frameRate <= 0 ) {
	frameRate = AudioOut.getDefaultFrameRate();
      }
      this.psg.setFrameRate( frameRate );
    }
  }


  @Override
  public void writeFrames(
			int nFrames,
			int valueM,
			int valueL,
			int valueR )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      audioOut.writeFrames( nFrames, valueM, valueL, valueR );
    } else {
      this.psg.setFrameRate( 0 );
    }
  }
}
