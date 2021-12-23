/*
 * (c) 2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Repraesentation eines Tongenerators
 */

package jkcemu.audio;


public abstract class AbstractSoundDevice
{
  protected volatile AudioOut audioOut;

  private String  text;
  private boolean stereo;


  protected AbstractSoundDevice( String text, boolean stereo )
  {
    this.text     = text;
    this.stereo   = stereo;
    this.audioOut = null;
  }


  public void fireStop()
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      audioOut.fireStop();
    }
  }


  public AudioOut getAudioOut()
  {
    return this.audioOut;
  }


  public abstract boolean isSingleBitMode();


  public void reset()
  {
    // leer
  }


  public void setAudioOut( AudioOut audioOut )
  {
    this.audioOut = audioOut;
  }


  public boolean supportsStereo()
  {
    return this.stereo;
  }


  public void writeFrames(
			int nFrames,
			int valueM,
			int valueL,
			int valueR )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      audioOut.writeFrames( nFrames, valueM, valueL, valueR );
    }
 }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.text;
  }
}
