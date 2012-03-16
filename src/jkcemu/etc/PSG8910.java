/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des programmierbaren Sound-Generatorschaltkreises
 * AY-3-8910 sowie deren abgeleiteten Versionen
 *
 * Die einzelnen Schaltkreise der Schaltkreisfamilie unterscheiden
 * sich nur durch die Anzahl der zusaetlichen IO-Ports.
 * Es wird der Grundtyp AY-3-8910 emuliert,
 * da er die maximale Anzahl an solchen Ports besitzt.
 * Moechte man einen abgeleiteten Schaltkreis emulieren,
 * so ignoriert man einfach die ueberzaehligen Ports.
 */

package jkcemu.etc;

import java.lang.*;
import java.util.Random;


public class PSG8910 extends Thread
{
  public interface Callback
  {
    public int  psgReadPort( PSG8910 psg, int port );
    public void psgWritePort( PSG8910 psg, int port, int value );
    public void psgWriteSample( PSG8910 psg, int a, int b, int c );
  };

  public static final int PORT_A = 0;
  public static final int PORT_B = 1;

  private Callback         callback;
  private int              clockHz;
  private int              regNum;
  private volatile int     sampleRate;
  private volatile int     amplitudeA;
  private volatile int     amplitudeB;
  private volatile int     amplitudeC;
  private volatile int     periodA;
  private volatile int     periodB;
  private volatile int     periodC;
  private volatile int     periodNoise;
  private volatile int     periodEnvelope;
  private volatile int     shapeEnvelope;
  private volatile int     modeBits;
  private volatile int     portA;
  private volatile int     portB;
  private volatile int[]   volumeValues;
  private int              channelOutA;
  private int              channelOutB;
  private int              channelOutC;
  private int              noiseCounter;
  private int              toneCounterA;
  private int              toneCounterB;
  private int              toneCounterC;
  private int              shapeCounter;
  private int              shapeStep;
  private int              shapeValue;
  private boolean          toneStateA;
  private boolean          toneStateB;
  private boolean          toneStateC;
  private boolean          noiseState;
  private boolean          envelopeDiv2Counter;
  private volatile boolean threadEnabled;
  private Random           random;
  private Object           waitMonitor;


  public PSG8910( int clockHz, int maxOutValue, Callback callback )
  {
    super( "JKCEMU PSG" );
    this.clockHz       = clockHz;
    this.callback      = callback;
    this.sampleRate    = 0;
    this.volumeValues  = new int[ 16 ];
    this.random        = new Random();
    this.waitMonitor   = new Object();
    this.threadEnabled = true;

    /*
     * Berechnung der Lautstaerkewerte,
     * Laut Datenblatt YM2149 (kompatibel zu AY-3-8910)
     * betraegt die Abstufung 0.841 pro Stufe.
     */
    float normValue = 1.0F;
    for( int i = this.volumeValues.length - 1; i > 0; --i ) {
      this.volumeValues[ i ] = Math.round( normValue * (float) maxOutValue );
      normValue *= 0.841;
    }
    this.volumeValues[ 0 ] = 0;
    reset();
  }


  /*
   * Die Methode wird aufgerufen, eine moegliche Aenderung
   * der der drei Steuerleitungen mitzuteilen.
   * Wenn sich daraus eine Leseoperation ergibt,
   * wird der Wert entsprechende Wert zurueckgeliefert,
   * ansonsten -1,
   */
  public int access(
		boolean bdir,
		boolean bc1,
		boolean bc2,
		int     value )
  {
    int rv = -1;
    if( (!bdir && bc1 && !bc2)			// Register auswaehlen
	|| (bdir && !bc1 && !bc2)
	|| (bdir && bc1 && bc2) )
    {
      this.regNum = value & 0x0F;
    }
    else if( !bdir && bc1 && bc2 ) {		// Lesen
      rv = getRegister( this.regNum );
    }
    else if( bdir && !bc1 && bc2 ) {		// Schreiben
      setRegister( this.regNum, value );
    }
    return rv;
  }


  public void die()
  {
    this.threadEnabled = false;
    interrupt();
    wakeUp();
  }


  public int getRegister( int regNum )
  {
    int rv = 0;
    switch( regNum ) {
      case 0:
	rv = this.periodA & 0xFF;
	break;
      case 1:
	rv = (this.periodA >> 8) & 0x0F;
	break;
      case 2:
	rv = this.periodB & 0xFF;
	break;
      case 3:
	rv = (this.periodB >> 8) & 0x0F;
	break;
      case 4:
	rv = this.periodC & 0xFF;
	break;
      case 5:
	rv = (this.periodC >> 8) & 0x0F;
	break;
      case 6:
	rv = this.periodNoise;
	break;
      case 7:
	rv = this.modeBits;
	break;
      case 8:
	rv = this.amplitudeA;
	break;
      case 9:
	rv = this.amplitudeB;
	break;
      case 10:
	rv = this.amplitudeC;
	break;
      case 11:
	rv = this.periodEnvelope & 0xFF;
	break;
      case 12:
	rv = (this.periodEnvelope >> 8) & 0xFF;
	break;
      case 13:
	rv = this.shapeEnvelope;
	break;
      case 14:
	rv = this.portA;
	if( ((this.modeBits & 0x40) == 0)
	    && (this.callback != null) )
	{
	  rv = this.callback.psgReadPort( this, PORT_A );
	}
	break;
      case 15:
	rv = this.portB;
	if( ((this.modeBits & 0x80) == 0)
	    && (this.callback != null) )
	{
	  rv = this.callback.psgReadPort( this, PORT_B );
	}
	break;
    }
    return rv;
  }


  public void reset()
  {
    this.portA          = 0xFF;
    this.portB          = 0xFF;
    this.modeBits       = 0xFF;
    this.amplitudeA     = 0;
    this.amplitudeB     = 0;
    this.amplitudeC     = 0;
    this.periodA        = 0;
    this.periodB        = 0;
    this.periodC        = 0;
    this.periodNoise    = 0;
    this.periodEnvelope = 0;
    this.shapeEnvelope  = 0;
    this.regNum         = 0;
    this.channelOutA    = 0;
    this.channelOutB    = 0;
    this.channelOutC    = 0;
    this.noiseCounter   = 0;
    this.shapeCounter   = 0;
    this.shapeStep      = 0;
    this.shapeValue     = 0;
    this.toneCounterA   = 0;
    this.toneCounterB   = 0;
    this.toneCounterC   = 0;
    this.toneStateA     = false;
    this.toneStateB     = false;
    this.toneStateC     = false;
    this.noiseState     = false;
  }


  public void setRegister( int regNum, int value )
  {
    switch( regNum ) {
      case 0:
	this.periodA = (this.periodA & 0x0F00) | (value & 0x00FF);
	break;
      case 1:
	this.periodA = ((value << 8) & 0x0F00) | (this.periodA & 0x00FF);
	break;
      case 2:
	this.periodB = (this.periodB & 0x0F00) | (value & 0x00FF);
	break;
      case 3:
	this.periodB = ((value << 8) & 0x0F00) | (this.periodB & 0x00FF);
	break;
      case 4:
	this.periodC = (this.periodC & 0x0F00) | (value & 0x00FF);
	break;
      case 5:
	this.periodC = ((value << 8) & 0x0F00) | (this.periodC & 0x00FF);
	break;
      case 6:
	this.periodNoise = value & 0x1F;
	break;
      case 7:
	this.modeBits = value & 0xFF;
	break;
      case 8:
	this.amplitudeA = value & 0x1F;
	break;
      case 9:
	this.amplitudeB = value & 0x1F;
	break;
      case 10:
	this.amplitudeC = value & 0x1F;
	break;
      case 11:
	synchronized( this ) {
	  this.periodEnvelope = (this.periodEnvelope & 0xFF00)
					| (value & 0x00FF);
	  resetShape();
	}
	break;
      case 12:
	synchronized( this ) {
	  this.periodEnvelope = ((value << 8) & 0xFF00)
					| (this.periodEnvelope & 0x00FF);
	  resetShape();
	}
	break;
      case 13:
	synchronized( this ) {
	  this.shapeEnvelope = value & 0x0F;
	  resetShape();
	}
	break;
      case 14:
	if( (this.modeBits & 0x40) != 0 ) {
	  this.portA = value;
	  if( this.callback != null ) {
	    this.callback.psgWritePort( this, PORT_A, value );
	  }
	}
	break;
      case 15:
	if( (this.modeBits & 0x80) != 0 ) {
	  this.portB = value;
	  if( this.callback != null ) {
	    this.callback.psgWritePort( this, PORT_B, value );
	  }
	}
	break;
    }
  }


  public void setSampleRate( int sampleRate )
  {
    this.sampleRate = sampleRate;
    if( this.sampleRate > 0 ) {
      wakeUp();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    int remainClocks   = 0;
    int lastSampleRate = 0;
    int div8Counter    = 0;
    while( this.threadEnabled ) {

      // naechstes Sample erzeugen
      int sampleRate = this.sampleRate;
      if( sampleRate > 0 ) {
	if( sampleRate != lastSampleRate ) {
	  remainClocks = 0;
	}
	int clocksPerSample = (this.clockHz + remainClocks) / sampleRate;
	if( clocksPerSample > 0 ) {
	  /*
	   * 1:16-Teiler
	   *
	   * Aus 16 Eingangsschwingungen entsteht eine volle Schwingung
	   * der interner Taktfrequenz, die bei Periode=1 zu einer
	   * vollen Ausgangsschwingung (2 Phasenwechsel) fuehrt.
	   * In der Emulation wird aber mit "internen Phasenwechsel"
	   * (Halbschwingung) gearbeitet.
	   * Aus diesem Grund darf der Vorteiler nur durch 8 teilen,
	   * um auf die gleiche Frequenz zu kommen.
	   */
	  int n = div8Counter + clocksPerSample;
	  while( n >= 8 ) {
	    n -= 8;
	    internalClockPhaseChange();
	  }
	  div8Counter = n;
	}
	this.callback.psgWriteSample(
				this,
				this.channelOutA,
				this.channelOutB,
				this.channelOutC );
	remainClocks = this.clockHz - (clocksPerSample * sampleRate);
      } else {
	synchronized( this.waitMonitor ) {
	  try {
	    this.waitMonitor.wait();
	  }
	  catch( IllegalMonitorStateException ex ) {}
	  catch( InterruptedException ex ) {}
	}
      }
    }
  }


	/* --- private Methoden --- */

  private int getOutValue( int amplitudeReg, boolean channelState )
  {
    int rv = 0;
    if( channelState ) {
      if( amplitudeReg > 0x0F ) {
	rv = this.volumeValues[ this.shapeValue ];
      } else {
	rv = this.volumeValues[ amplitudeReg ];
      }
    }
    return rv;
  }


  private synchronized void internalClockPhaseChange()
  {
    /*
     * Tongeneratoren aktualisieren,
     * Bei Periode=0 ist der Kanal immer an.
     * In dem Fall kann durch Programmierung der Lautstaerke
     * ein Ton erzeugt werden.
     */
    if( this.toneCounterA > 0 ) {
      --this.toneCounterA;
    } else {
      this.toneCounterA = this.periodA;
      if( this.toneCounterA > 0 ) {
	--this.toneCounterA;
	this.toneStateA = !this.toneStateA;
      } else {
	this.toneStateA = true;
      }
    }
    if( this.toneCounterB > 0 ) {
      --this.toneCounterB;
    } else {
      this.toneCounterB = this.periodB;
      if( this.toneCounterB > 0 ) {
	--this.toneCounterB;
	this.toneStateB = !this.toneStateB;
      } else {
	this.toneStateB = true;
      }
    }
    if( this.toneCounterC > 0 ) {
      --this.toneCounterC;
    } else {
      this.toneCounterC = this.periodC;
      if( this.toneCounterC > 0 ) {
	--this.toneCounterC;
	this.toneStateC = !this.toneStateB;
      } else {
	this.toneStateC = true;
      }
    }

    /*
     * Rauschgenerator aktualisieren,
     * Periode=0 erzeugt wie bei Periode=1 ein helles Rauschen.
     */
    if( this.noiseCounter > 0 ) {
      --this.noiseCounter;
    } else {
      this.noiseCounter = this.periodNoise;
      if( this.noiseCounter > 0 ) {
	--this.noiseCounter;
      }
      if( this.random.nextBoolean() ) {
	this.noiseState = !this.noiseState;
      }
    }

    // Mixer
    boolean stateA = false;
    boolean stateB = false;
    boolean stateC = false;
    switch( this.modeBits & 0x09 ) {
      case 0x00:			// Tongenerator + Rauschen
	stateA = this.toneStateA || this.noiseState;
	break;
      case 0x01:			// Rauschen
	stateA = this.noiseState;
	break;
      case 0x08:			// Tongenerator
	stateA = this.toneStateA;
	break;
    }
    switch( this.modeBits & 0x12 ) {
      case 0x00:			// Tongenerator + Rauschen
	stateB = this.toneStateB || this.noiseState;
	break;
      case 0x02:			// Rauschen
	stateB = this.noiseState;
	break;
      case 0x10:			// Tongenerator
	stateB = this.toneStateB;
	break;
    }
    switch( this.modeBits & 0x24 ) {
      case 0x00:			// Tongenerator + Rauschen
	stateC = this.toneStateC || this.noiseState;
	break;
      case 0x04:			// Rauschen
	stateC = this.noiseState;
	break;
      case 0x20:			// Tongenerator
	stateC = this.toneStateC;
	break;
    }

    /*
     * Huellkurve aktualisieren
     *
     * Da die Methode zweimal pro interner Schwingung aufgerufen wird,
     * muss noch durch zwei geteilt werden.
     * Periode=0 ist wie Periode=1 sehr kurz
     */
    this.envelopeDiv2Counter = !this.envelopeDiv2Counter;
    if( this.envelopeDiv2Counter ) {
      if( this.shapeCounter > 0 ) {
	--this.shapeCounter;
      }
      if( this.shapeCounter == 0 ) {
	this.shapeCounter = this.periodEnvelope;
	switch( this.shapeEnvelope ) {
	  case 4:						// /____
	  case 5:
	  case 6:
	  case 7:
	  case 15:
	    if( this.shapeStep < 15 ) {
	      this.shapeStep++;
	      this.shapeValue = this.shapeStep & 0x0F;
	    } else {
	      this.shapeValue = 0;
	    }
	    break;
	  case 8:						// \\\\
	    if( this.shapeStep < 15 ) {
	      this.shapeStep++;
	      this.shapeValue = (15 - this.shapeStep) & 0x0F;
	    } else {
	      this.shapeStep  = 0;
	      this.shapeValue = 15;
	    }
	    break;
	  case 10:						// \/\/
	    if( this.shapeStep < 31 ) {
	      this.shapeStep++;
	    } else {
	      this.shapeStep = 0;
	    }
	    if( this.shapeStep < 16 ) {
	      this.shapeValue = (15 - this.shapeStep) & 0x0F;
	    } else {
	      this.shapeValue = (this.shapeStep - 16) & 0x0F;
	    }
	    break;
	  case 11:						// \~~~
	    if( this.shapeStep < 15 ) {
	      this.shapeStep++;
	      if( this.shapeValue > 0 ) {
		--this.shapeValue;
	      }
	    } else {
	      this.shapeValue = 15;
	    }
	    break;
	  case 12:						// ////
	    if( this.shapeStep < 15 ) {
	      this.shapeStep++;
	      this.shapeValue = this.shapeStep & 0x0F;
	    } else {
	      this.shapeStep  = 0;
	      this.shapeValue = 0;
	    }
	    break;
	  case 13:						// /~~~
	    if( this.shapeStep < 15 ) {
	      this.shapeStep++;
	      this.shapeValue = this.shapeStep & 0x0F;
	    }
	    break;
	  case 14:						// /\/\
	    if( this.shapeStep < 31 ) {
	      this.shapeStep++;
	    } else {
	      this.shapeStep = 0;
	    }
	    if( this.shapeStep < 16 ) {
	      this.shapeValue = this.shapeStep & 0x0F;
	    } else {
	      this.shapeValue = (31 - this.shapeStep) & 0x0F;
	    }
	    break;
	  default:						// \___
	    if( this.shapeStep < 16 ) {
	      this.shapeStep++;
	    }
	    if( this.shapeValue > 0 ) {
	      --this.shapeValue;
	    }
	    break;
	}
      }
    }

    // Ausgangspegel berechnen
    this.channelOutA = getOutValue( this.amplitudeA, stateA );
    this.channelOutB = getOutValue( this.amplitudeB, stateB );
    this.channelOutC = getOutValue( this.amplitudeC, stateC );
  }


  private void resetShape()
  {
    this.shapeStep = 0;
    if( (this.shapeEnvelope <= 3)
	|| ((this.shapeEnvelope >= 8) && (this.shapeEnvelope <= 11)) )
    {
      this.shapeValue = 15;
    } else {
      this.shapeValue = 0;
    }
  }


  private void wakeUp()
  {
    synchronized( this.waitMonitor ) {
      try {
	this.waitMonitor.notify();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }
}

