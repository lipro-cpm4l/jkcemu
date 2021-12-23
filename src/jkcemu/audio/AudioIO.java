/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang und Ausgang)
 */

package jkcemu.audio;

import java.io.IOException;
import javax.sound.sampled.DataLine;
import jkcemu.Main;
import jkcemu.base.ScreenFrm;


public abstract class AudioIO
{
  public static final String ERROR_LINE_UNAVAILABLE =
	"Der Audiokanal ist nicht verf\u00FCgbar.\n"
		+ "M\u00F6glicherweise wird er bereits durch eine andere"
		+ " Applikation verwendet.";

  public static final String ERROR_NO_LINE =
	"Der Audiokanal konnte nicht ge\u00F6ffnet werden.";

  public static final String ERROR_RECORDING_OUT_OF_MEMORY =
	"Kein Speicher mehr f\u00FCr die Aufzeichnung\n"
		+ "der Audiodaten verf\u00FCgbar.";

  protected AudioIOObserver  observer;
  protected int              frameRate;
  protected int              sampleSizeInBits;
  protected int              channels;
  protected boolean          bigEndian;
  protected boolean          dataSigned;
  protected volatile boolean stopRequested;
  protected int              bytesPerSample;

  private static volatile DataLine cpuSyncLine = null;

  private volatile boolean   finishedFired;
  private String             errorText;


  protected AudioIO( AudioIOObserver observer )
  {
    this.observer         = observer;
    this.frameRate        = 0;
    this.sampleSizeInBits = 0;
    this.channels         = 0;
    this.bigEndian        = false;
    this.dataSigned       = false;
    this.stopRequested    = false;
    this.finishedFired    = false;
    this.bytesPerSample   = 0;
    this.errorText        = null;
  }


  public static void checkOpenExclCPUSynchronLine() throws IOException
  {
    if( cpuSyncLine != null ) {
      throw new IOException(
	"Es ist bereits ein Audiokanal ge\u00F6ffnet,"
		+ " der synchron zum emulierten Mikroprozessor"
		+ " bedient wird.\n"
		+ "Sie m\u00FCssen zuerst dieses Audiokanal schlie\u00DFen,"
		+ " bevor Sie einen anderen \u00F6ffnen k\u00F6nnen." );
    }
  }


  protected void closeDataLine( DataLine line )
  {
    if( line != null ) {
      try {
	line.stop();
	line.flush();
	line.close();
      }
      catch( Exception ex ) {}
      finally {
	if( line == cpuSyncLine ) {
	  cpuSyncLine = null;
	}
      }
    }
  }


  public void closeLine()
  {
    // leer
  }


  /*
   * Mit dieser Methode erfaehrt die Klasse die Anzahl
   * der seit dem letzten Aufruf vergangenen Taktzyklen.
   *
   * Rueckgabewert:
   *   true:  Audiodaten verwenden
   *   false: Audiodaten verwerfen
   */
  protected boolean currentDiffTStates( long diffTStates )
  {
    return true;
  }


  protected synchronized void finished()
  {
    if( !this.finishedFired ) {
      this.observer.fireFinished( this, this.errorText );
      this.finishedFired = true;
    }
  }


  public void fireStop()
  {
    this.stopRequested = true;
  }


  public int getChannels()
  {
    return this.channels;
  }


  public int getFrameRate()
  {
    return this.frameRate;
  }


  public int getSampleSizeInBits()
  {
    return this.sampleSizeInBits;
  }


  public static boolean isCPUSynchronLineOpen()
  {
    return cpuSyncLine != null;
  }


  protected static boolean isEmuThread()
  {
    ScreenFrm screenFrm = Main.getScreenFrm();
    return screenFrm != null ?
		(screenFrm.getEmuThread() == Thread.currentThread())
		: false;
  }


  protected void setErrorText( String errorText )
  {
    this.errorText = errorText;
  }


  protected void registerCPUSynchronLine( DataLine line )
						throws IOException
  {
    checkOpenExclCPUSynchronLine();
    cpuSyncLine = line;
  }


  protected void setFormat(
			String  fmtTextPrefix,
			int     frameRate,
			int     sampleSizeInBits,
			int     channels,
			boolean dataSigned,
			boolean bigEndian )
  {
    this.frameRate        = frameRate;
    this.sampleSizeInBits = sampleSizeInBits;
    this.bytesPerSample   = (sampleSizeInBits + 7) / 8;
    this.channels         = channels;
    this.dataSigned       = dataSigned;
    this.bigEndian        = bigEndian;

    // Formattext erzeugen
    StringBuilder buf = new StringBuilder( 128 );
    if( fmtTextPrefix != null ) {
      buf.append( fmtTextPrefix );
    }
    buf.append( frameRate );
    buf.append( " Hz, " );
    buf.append( sampleSizeInBits );
    buf.append( " Bit" );
    switch( this.channels ) {
      case 1:
	buf.append( " Mono" );
	break;
      case 2:
	buf.append( " Stereo" );
	break;
      default:
	buf.append( ", " );
	buf.append( this.channels );
	buf.append( " Kan\u00E4le" );
	break;
    }
    this.observer.fireFormatChanged( this, buf.toString() );

    // Wertebereich der Pegelanzeige festlegen
    if( this.bytesPerSample == 1 ) {
      if( this.dataSigned ) {
	this.observer.setVolumeLimits( -128, 127 );
      } else {
	this.observer.setVolumeLimits( 0, 255 );
      }
    } else {
      if( this.dataSigned ) {
	this.observer.setVolumeLimits( -32768, 32767 );
      } else {
	this.observer.setVolumeLimits( 0, 65535 );
      }
    }
  }
}
