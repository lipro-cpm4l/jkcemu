/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang)
 */

package jkcemu.audio;

import java.lang.*;
import z80emu.Z80CPU;


public abstract class AudioIn extends AudioIO
{
  protected int minValue;
  protected int maxValue;

  private int adjustPeriodCnt;
  private int adjustPeriodLen;
  private int sampleNegMask;
  private int sampleSignMask;
  private int selectedChannel;


  protected AudioIn( AudioIOObserver observer, Z80CPU z80cpu )
  {
    super( observer, z80cpu );
    this.minValue        = 0;
    this.maxValue        = 0;
    this.adjustPeriodCnt = 0;
    this.adjustPeriodLen = 0;
    this.sampleNegMask   = 0;
    this.sampleSignMask  = 0;
    this.selectedChannel = 0;
  }


  public boolean isPause()
  {
    return false;
  }


  public String getSpecialFormatText()
  {
    return null;
  }


  public boolean isProgressUpdateEnabled()
  {
    return false;
  }


  protected abstract byte[] readFrame();


  @Override
  protected void setFormat(
			int     frameRate,
			int     sampleSizeInBits,
			int     channels,
			boolean dataSigned,
			boolean bigEndian )
  {
    super.setFormat(
		frameRate,
		sampleSizeInBits,
		channels,
		dataSigned,
		bigEndian );

    // Vorzeichenbit
    this.sampleSignMask = 0;
    if( this.dataSigned ) {
      switch( this.bytesPerSample ) {
	case 1:
	  this.sampleSignMask = 0x00000080;
	  this.sampleNegMask  = 0xFFFFFF00;
	  break;
	case 2:
	  this.sampleSignMask = 0x00008000;
	  this.sampleNegMask  = 0xFFFF0000;
	  break;
	case 3:
	  this.sampleSignMask = 0x00800000;
	  this.sampleNegMask  = 0xFF000000;
	  break;
	case 4:
	  this.sampleSignMask = 0x80000000;
	  break;
      }
    }

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


  public void setSelectedChannel( int channel )
  {
    this.selectedChannel = channel;
  }


  public void stopAudio()
  {
    // leer
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und liest die Phase des Toneingangs.
   */
  public boolean readPhase()
  {
    if( this.tStatesPerFrame > 0 ) {
      if( this.firstCall ) {
	this.firstCall   = false;
	this.lastTStates = this.z80cpu.getProcessedTStates();

      } else {

	long tStates     = z80cpu.getProcessedTStates();
	long diffTStates = z80cpu.calcTStatesDiff(
					this.lastTStates,
					tStates );

	if( diffTStates > 0 ) {
	  if( currentDiffTStates( diffTStates ) ) {

	    // bis zum naechsten auszuwertenden Samples lesen
	    int nSamples = (int) (diffTStates / this.tStatesPerFrame);
	    if( nSamples > 0 ) {
	      int v1 = 0;
	      int v2 = 0;
	      int i = nSamples;
	      do {
		v1 = readSample();
		if( v1 < 0 ) {
		  break;
		}
		v2 = v1;

		// dynamische Mittelwertbestimmung
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

		// Wenn gelesender Wert negativ ist, Zahl korrigieren
		if( this.dataSigned && ((v2 & this.sampleSignMask) != 0) ) {
		  v2 |= ~this.sampleNegMask;
		}

		// Minimum-/Maximum-Werte aktualisieren
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
		int d = this.maxValue - this.minValue;
		if( this.lastPhase ) {
		  if( v2 < this.minValue + (d / 3) ) {
		    this.lastPhase = false;
		  }
		} else {
		  if( v2 > this.maxValue - (d / 3) ) {
		    this.lastPhase = true;
		  }
		}
	      }

	      /*
	       * Anzahl der verstrichenen Taktzyklen auf den Wert
	       * des letzten gelesenen Samples korrigieren
	       */
	      this.lastTStates += nSamples * this.tStatesPerFrame;
	    }
	  } else {
	    this.lastTStates = this.z80cpu.getProcessedTStates();
	  }
	}
      }
    }
    return this.lastPhase;
  }


	/* --- private Methoden --- */

  /*
   * Die Methode liest ein Sample.
   * Im Rueckgabewert sind nur die betreffenden Bits gesetzt,
   * die hoeherwertigen Bits sind 0.
   * Dadurch ist der Rueckgabewert bei einem gueltigen Sample niemals negativ,
   * auch wenn der eigentlich gelesene Wert negativ ist.
   *
   * Rueckgabewert = -1: kein Sample gelesen
   */
  private int readSample()
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
