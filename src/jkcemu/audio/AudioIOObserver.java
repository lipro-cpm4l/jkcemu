/*
 * (c) 2016-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle zur Ueberwachung eines Audio-IO-Prozesses
 */

package jkcemu.audio;


public interface AudioIOObserver
{
  public void fireFinished( AudioIO audioIO, String errorText );
  public void fireFormatChanged( AudioIO audioIO, String formatText );
  public void fireMonitorFailed( AudioIn audioIn, String errorText );
  public void fireProgressUpdate( AudioInFile audioInFile );
  public void fireRecordingStatusChanged( AudioOut audioOut );
  public void setVolumeLimits( int minLimit, int maxLimit );
  public void updVolume( int value );
}
