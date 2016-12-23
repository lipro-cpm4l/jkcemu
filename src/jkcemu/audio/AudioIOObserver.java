/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle zur Ueberwachung eines Audio-IO-Prozesses
 */

package jkcemu.audio;

import java.lang.*;


public interface AudioIOObserver
{
  public void fireFinished( AudioIO audioIO, String errText );
  public void fireProgressUpdate( AudioInFile audioInFile );
  public void fireRecordingStatusChanged( AudioOut audioOut );
  public void setVolumeLimits( int minLimit, int maxLimit );
  public void updVolume( int value );
}
