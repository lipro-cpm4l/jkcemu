/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang)
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;
import z80emu.Z80CPU;


public abstract class AudioIn extends AudioIO
{
  protected int minValue;
  protected int maxValue;

  private int     selectedChannel;
  private int     sampleSignMask;
  private int     sampleBitMask;
  private int     sampleSizeInBytes;
  private int     channelCount;
  private int     adjustPeriodLen;
  private int     adjustPeriodCnt;
  private boolean bigEndian;
  private boolean dataSigned;


  protected AudioIn( AudioFrm audioFrm, Z80CPU z80cpu )
  {
    super( audioFrm, z80cpu );
    this.minValue          = 0;
    this.maxValue          = 0;
    this.selectedChannel   = 0;
    this.sampleBitMask     = 0;
    this.sampleSizeInBytes = 0;
    this.channelCount      = 0;
    this.bigEndian         = false;
    this.dataSigned        = false;
  }


  public boolean isPause()
  {
    return false;
  }


  public boolean isTapeFile()
  {
    return false;
  }


  protected abstract byte[] readFrame();


  protected void setAudioFormat( AudioFormat fmt )
  {
    this.audioFmt = fmt;
    if( fmt != null ) {
      int sampleSizeInBits = fmt.getSampleSizeInBits();

      this.sampleBitMask = 0;
      for( int i = 0; i < sampleSizeInBits; i++ ) {
	this.sampleBitMask = (this.sampleBitMask << 1) | 1;
      }
      this.sampleSizeInBytes = (sampleSizeInBits + 7) / 8;
      this.channelCount      = fmt.getChannels();
      this.bigEndian         = fmt.isBigEndian();
      this.dataSigned        = fmt.getEncoding().equals(
					AudioFormat.Encoding.PCM_SIGNED );

      // Wertebereich der Pegelanzeige,
      if( this.sampleSizeInBytes == 1 ) {
	if( this.dataSigned ) {
	  this.audioFrm.setVolumeLimits( -128, 127 );
	} else {
	  this.audioFrm.setVolumeLimits( 0, 255 );
	}
      } else {
	if( this.dataSigned ) {
	  this.audioFrm.setVolumeLimits( -32768, 32767 );
	} else {
	  this.audioFrm.setVolumeLimits( 0, 65535 );
	}
      }

      // Vorzeichenbit
      this.sampleSignMask = 0;
      if( this.dataSigned ) {
	this.sampleSignMask = 1;
	for( int i = 1; i < sampleSizeInBits; i++ ) {
	  this.sampleSignMask <<= 1;
	}
      }

      /*
       * Min-/Max-Regelung initialisieren
       *
       * Nach einer Periodenlaenge werden die Minimum- und Maximum-Werte
       * zueinander um einen Schritt angenaehert,
       * um so einen dynamischen Mittelwert errechnen zu koennen.
       */
      this.adjustPeriodLen = (int) fmt.getSampleRate() / 256;
      if( this.adjustPeriodLen < 1 ) {
	this.adjustPeriodLen = 1;
      }
      this.adjustPeriodCnt = this.adjustPeriodLen;
    }
  }


  public void setSelectedChannel( int channel )
  {
    this.selectedChannel = channel;
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
	      int v = 0;
	      int i = nSamples;
	      do {
		v = readSample();
		if( v >= 0 ) {

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
		  if( this.dataSigned && ((v & this.sampleSignMask) != 0) ) {
		    v |= ~this.sampleBitMask;
		  }

		  // Minimum-/Maximum-Werte aktualisieren
		  if( v < this.minValue ) {
		    this.minValue = v;
		  }
		  else if( v > this.maxValue ) {
		    this.maxValue = v;
		  }

		  // Pegelanzeige
		  this.audioFrm.updVolume( v );
		}
	      } while( --i > 0 );

	      if( v != -1 ) {
		int d = this.maxValue - this.minValue;
		if( this.lastPhase ) {
		  if( v < this.minValue + (d / 3) ) {
		    this.lastPhase = false;
		  }
		} else {
		  if( v > this.maxValue - (d / 3) ) {
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
	int offset = this.selectedChannel * this.sampleSizeInBytes;
	if( offset + this.sampleSizeInBytes <= frameData.length ) {
	  value = 0;
	  if( this.bigEndian ) {
	    for( int i = 0; i < this.sampleSizeInBytes; i++ ) {
	      value = (value << 8) | ((int) frameData[ offset + i ] & 0xFF);
	    }
	  } else {
	    for( int i = this.sampleSizeInBytes - 1; i >= 0; --i ) {
	      value = (value << 8) | ((int) frameData[ offset + i ] & 0xFF);
	    }
	  }
	  value &= this.sampleBitMask;
	}
      }
    }
    return value;
  }
}
