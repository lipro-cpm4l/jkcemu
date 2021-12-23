/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang)
 */

package jkcemu.audio;

import javax.sound.sampled.Mixer;
import z80emu.Z80CPU;


public abstract class AudioIn extends AudioIO
{
  protected Z80CPU z80cpu;
  protected int    speedHz;
  protected int    minValue;
  protected int    maxValue;

  private boolean firstCall;
  private boolean lastPhase;
  private long    lastTStates;
  private long    begTStates;
  private long    maxTStates;
  private long    totalFrameCnt;
  private int     adjustPeriodCnt;
  private int     adjustPeriodLen;
  private int     sampleBitMask;
  private int     sampleSignMask;
  private int     selectedChannel;


  protected AudioIn( AudioIOObserver observer, Z80CPU z80cpu )
  {
    super( observer );
    this.z80cpu          = z80cpu;
    this.firstCall       = true;
    this.lastPhase       = false;
    this.lastTStates     = 0;
    this.begTStates      = 0;
    this.maxTStates      = 0;
    this.totalFrameCnt   = 0;
    this.minValue        = 0;
    this.maxValue        = 0;
    this.adjustPeriodCnt = 0;
    this.adjustPeriodLen = 0;
    this.sampleBitMask   = 0;
    this.sampleSignMask  = 0;
    this.selectedChannel = 0;
    this.speedHz         = 0;
  }


  protected void checkCloseAndFinished()
  {
    // leer
  }


  protected void checkOpen()
  {
    // leer
  }


  public boolean isPause()
  {
    return false;
  }


  public String getSpecialFormatText()
  {
    return null;
  }


  public boolean isMonitorActive()
  {
    return false;
  }


  protected abstract byte[] readFrame();


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und liest die Phase des Toneingangs.
   */
  public boolean readPhase()
  {
    try {
      checkOpen();
      if( this.firstCall ) {
	this.firstCall   = false;
	this.maxTStates  = 0x7FFFFFFF00000000L / (this.frameRate + 1);
	this.begTStates  = this.z80cpu.getProcessedTStates();
	this.lastTStates = this.begTStates;
	this.lastPhase   = false;
      } else {
	long curTStates  = this.z80cpu.getProcessedTStates();
	long allTStates  = curTStates - this.begTStates;
	if( (allTStates < 0) || (allTStates > this.maxTStates) ) {
	  fireStop();
	} else if( allTStates > 0 ) {
	  int nFrames = (int) ((allTStates
				* this.frameRate
				/ this.speedHz) - this.totalFrameCnt);
	  if( nFrames > 0 ) {
	    long diffTStates = curTStates - this.lastTStates;
	    if( diffTStates > 0 ) {
	      if( currentDiffTStates( diffTStates ) ) {

		/*
		 * bis zum naechsten auszuwertenden Frame lesen
		 *
		 * Die Auswahl des richtigen Samples aus dem Frame
		 * erfolgt in der Methode readFrameAndGetSample()
		 */
		int v1 = 0;
		int v2 = 0;
		int i = nFrames;
		do {
		  v1 = readFrameAndGetSample();
		  if( v1 < 0 ) {
		    break;
		  }
		  v1 &= this.sampleBitMask;
		  v2 = v1;

		  // Wenn gelesener Wert negativ, dann Zahl korrigieren
		  if( this.dataSigned
		      && ((v2 & this.sampleSignMask) != 0) )
		  {
		    v2 |= ~this.sampleBitMask;
		  }

		  // Minimum-/Maximum-Werte anpassen
		  if( this.adjustPeriodCnt > 0 ) {
		    --this.adjustPeriodCnt;
		  } else {
		    this.adjustPeriodCnt = this.adjustPeriodLen;
		    if( this.minValue < this.maxValue ) {
		      this.minValue++;
		    }
		    if( this.maxValue > this.minValue ) {
		      --this.maxValue;
		    }
		  }
		  if( v2 < this.minValue ) {
		    this.minValue = v2;
		  }
		  else if( v2 > this.maxValue ) {
		    this.maxValue = v2;
		  }

		  // Pegelanzeige
		  this.observer.updVolume( v2 );
		} while( --i > 0 );

		if( v1 >= 0 ) {
		  int range      = this.maxValue - this.minValue;
		  this.lastPhase = (v2 > (this.minValue + (range / 2)));
		}
	      }
	      this.totalFrameCnt += nFrames;
	      this.lastTStates = curTStates;
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {
      /*
       * z.B. InterruptedException bei Programmbeendigung oder
       * eine andere Exception bei Abziehen eines aktiven USB-Audiogeraetes
       */
      fireStop();
    }
    finally {
      checkCloseAndFinished();
    }
    return this.lastPhase;
  }


  public void setMonitorEnabled( boolean state, Mixer.Info mixerInfo )
  {
    if( state ) {
      this.observer.fireMonitorFailed(
		this,
		"Das Mith\u00F6ren ist in diesem Fall nicht m\u00F6glich." );
    }
  }


  public void setSelectedChannel( int channel )
  {
    this.selectedChannel = channel;
  }


  public boolean supportsMonitor()
  {
    return false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void setFormat(
			String  fmtTextPrefix,
			int     frameRate,
			int     sampleSizeInBits,
			int     channels,
			boolean dataSigned,
			boolean bigEndian )
  {
    super.setFormat(
		fmtTextPrefix,
		frameRate,
		sampleSizeInBits,
		channels,
		dataSigned,
		bigEndian );

    // Vorzeichenbit
    if( sampleSizeInBits < 8 ) {
      sampleSizeInBits = 8;
    }
    this.sampleBitMask  = ((1 << sampleSizeInBits) - 1);
    this.sampleSignMask = (1 << (sampleSizeInBits - 1));

    /*
     * Min-/Max-Regelung initialisieren
     *
     * Nach einer Periodenlaenge werden die Minimum- und Maximum-Werte
     * zueinander um einen Schritt angenaehert,
     * um so einen dynamischen Mittelwert errechnen zu koennen.
     */
    this.adjustPeriodLen = this.frameRate / 256;
    if( this.adjustPeriodLen < 1 ) {
      this.adjustPeriodLen = 1;
    }
    this.adjustPeriodCnt = this.adjustPeriodLen;
  }


	/* --- private Methoden --- */

  /*
   * Die Methode liest ein Frame und gibt das Samples
   * des ausgewaehlten Kanals zurueck.
   * Auch bei vorzeichenbehaftenen Audiodaten ist der Rueckgabewert
   * nicht negativ, da nur die betreffenden unteren Bits gefuellt sind.
   * Bei einem Rueckgabewert kleiner Null konnte kein Sample gelesen werden.
   */
  private int readFrameAndGetSample()
  {
    int value = -1;
    if( !isPause() ) {
      byte[] frameData = readFrame();
      if( frameData != null ) {
	int offset = this.selectedChannel * this.bytesPerSample;
	if( offset + this.bytesPerSample <= frameData.length ) {
	  value = 0;
	  if( this.bigEndian ) {
	    for( int i = 0; i < this.bytesPerSample; i++ ) {
	      value = (value << 8) | ((int) frameData[ offset + i ] & 0xFF);
	    }
	  } else {
	    for( int i = this.bytesPerSample - 1; i >= 0; --i ) {
	      value = (value << 8) | ((int) frameData[ offset + i ] & 0xFF);
	    }
	  }
	}
      }
    }
    return value;
  }
}
