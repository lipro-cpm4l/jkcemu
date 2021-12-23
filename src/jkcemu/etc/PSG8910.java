/*
 * (c) 2011-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des programmierbaren Sound-Generatorschaltkreises
 * AY-3-8910 sowie deren abgeleiteten Versionen
 *
 * Die einzelnen Schaltkreise der Schaltkreisfamilie unterscheiden
 * sich nur durch die Anzahl der zusaetzlichen IO-Ports.
 * Es wird der Grundtyp AY-3-8910 emuliert,
 * da er die maximale Anzahl an solchen Ports besitzt.
 * Moechte man einen abgeleiteten Schaltkreis emulieren,
 * so ignoriert man einfach die ueberzaehligen Ports.
 */

package jkcemu.etc;

import java.net.URL;
import jkcemu.Main;


public class PSG8910 extends Thread
{
  public interface Callback
  {
    public int  psgReadPort( PSG8910 psg, int port );
    public void psgWritePort( PSG8910 psg, int port, int value );
    public void psgWriteFrame( PSG8910 psg, int a, int b, int c );
  };

  public static final int PORT_A = 0;
  public static final int PORT_B = 1;

  private static final String TEXT_OUT_FIX        = "konstanter&nbsp;Pegel";
  private static final String TEXT_OUT_NOISE      = "Rauschen";
  private static final String TEXT_OUT_NOISE_TONE = "Rauschen+Ton";
  private static final String TEXT_OUT_TONE       = "Ton";

  /*
   * Tabelle mit den Werten fuer die 16 Pegelstufen
   * mit einem Umfang von 0 bis 200
   *
   * Die Abstufung betraegt laut Datenblatt 0.707 pro Stufe.
   * Die Werte in der Tabelle sind entsprechend der originalen Abstufung
   * gerundete Werte, wobei die unteren Werte so angepasst wurden,
   * dass bei Addition eines Kanalwertes mit dem halben Wert
   * eines anderen Kanals und anschliessender Normierung
   * (also (A + (B/2)) * 2 / 3)
   * immer noch 16 unterschiedliche Werte entstehen.
   */
  private static int[] volumeValues = {
				0, 2, 4, 6, 8, 11, 14, 18,
				23, 29, 37, 50, 71, 100, 141, 200 };

  private Callback         callback;
  private int              clockHz;
  private int              regNum;
  private volatile int     frameRate;
  private volatile int     amplitudeA;
  private volatile int     amplitudeB;
  private volatile int     amplitudeC;
  private volatile int     periodA;
  private volatile int     periodB;
  private volatile int     periodC;
  private volatile int     periodNoise;
  private volatile int     periodEnvelope;
  private volatile int     envelopeShape;
  private volatile int     modeBits;
  private volatile int     portA;
  private volatile int     portB;
  private int              channelOutA;
  private int              channelOutB;
  private int              channelOutC;
  private int              noiseCounter;
  private int              toneCounterA;
  private int              toneCounterB;
  private int              toneCounterC;
  private int              envPeriodCounter;
  private int              envShapeStep;
  private int              envShapeValue;
  private int              noiseShifter;
  private boolean          toneStateA;
  private boolean          toneStateB;
  private boolean          toneStateC;
  private boolean          noiseState;
  private volatile boolean envelopeReset;
  private boolean          envelopeDiv2;
  private boolean          envelopeEnd;
  private volatile boolean threadEnabled;
  private Object           waitMonitor;


  public PSG8910( int clockHz, Callback callback )
  {
    super( Main.getThreadGroup(), "JKCEMU PSG" );
    this.clockHz       = clockHz;
    this.callback      = callback;
    this.frameRate     = 0;
    this.waitMonitor   = new Object();
    this.threadEnabled = true;
    reset();
  }


  /*
   * Die Methode wird aufgerufen, um eine moegliche Aenderung
   * der drei Steuerleitungen mitzuteilen.
   * Wenn sich daraus eine Leseoperation ergibt,
   * wird der entsprechende Wert zurueckgeliefert, anderenfalls -1.
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


  public void appendStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<table border=\"1\">\n"
	+ "<tr><th>Register</th><th>Wert</th></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "0/1 - Periodendauer Ton Kanal A:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%03Xh", this.periodA & 0x0FFF ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "2/3 - Periodendauer Ton Kanal B:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%03Xh", this.periodB & 0x0FFF ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "4/5 - Periodendauer Ton Kanal C:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%03Xh", this.periodC & 0x0FFF ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "6 - Periodendauer Rauschen:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%02Xh", this.periodNoise & 0x1F ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "7 - Mixer:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">" );
    buf.append( String.format( "%02Xh:", this.modeBits & 0xFF ) );
    buf.append( "<br/>Kanal A: " );
    switch( this.modeBits & 0x09 ) {
      case 0x00:			// Rauschen + Tongenerator
	buf.append( TEXT_OUT_NOISE_TONE );
	break;
      case 0x01:			// Rauschen
	buf.append( TEXT_OUT_NOISE );
	break;
      case 0x08:			// Tongenerator
	buf.append( TEXT_OUT_TONE );
	break;
      default:				// konstanter Pegel
	buf.append( TEXT_OUT_FIX );
    }
    buf.append( "<br/>Kanal B: " );
    switch( this.modeBits & 0x12 ) {
      case 0x00:			// Rauschen + Tongenerator
	buf.append( TEXT_OUT_NOISE_TONE );
	break;
      case 0x02:			// Rauschen
	buf.append( TEXT_OUT_NOISE );
	break;
      case 0x10:			// Tongenerator
	buf.append( TEXT_OUT_TONE );
	break;
      default:				// konstanter Pegel
	buf.append( TEXT_OUT_FIX );
    }
    buf.append( "<br/>Kanal C: " );
    switch( this.modeBits & 0x24 ) {
      case 0x00:			// Rauschen + Tongenerator
	buf.append( TEXT_OUT_NOISE_TONE );
	break;
      case 0x04:			// Rauschen
	buf.append( TEXT_OUT_NOISE );
	break;
      case 0x20:			// Tongenerator
	buf.append( TEXT_OUT_TONE );
	break;
      default:				// konstanter Pegel
	buf.append( TEXT_OUT_FIX );
    }
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "10 - Amplitude Kanal A:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%02Xh", this.amplitudeA & 0x1F ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "11 - Amplitude Kanal B:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%02Xh", this.amplitudeB & 0x1F ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "12 - Amplitude Kanal C:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%02Xh", this.amplitudeC & 0x1F ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "13/14 - Periodendauer H&uuml;llkurve:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\">" );
    buf.append( String.format( "%04Xh", this.periodEnvelope & 0xFFFF ) );
    buf.append( "</td></tr>\n"
	+ "<tr>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">"
	+ "15 - H&uuml;llkurvenform:"
	+ "</td>"
	+ "<td align=\"left\" valign=\"top\" nowrap=\"nowrap\">" );
    buf.append( String.format( "%1Xh", this.envelopeShape & 0x0F ) );
    String envelopeFile = null;
    switch( this.envelopeShape & 0x0F ) {
      case 0x00:
      case 0x01:
      case 0x02:
      case 0x03:
      case 0x09:
	envelopeFile = "envelope_00XX.png";
	break;
      case 0x04:
      case 0x05:
      case 0x06:
      case 0x07:
      case 0x0F:
	envelopeFile = "envelope_01XX.png";
	break;
      case 0x08:
	envelopeFile = "envelope_1000.png";
	break;
      case 0x0A:
	envelopeFile = "envelope_1010.png";
	break;
      case 0x0B:
	envelopeFile = "envelope_1011.png";
	break;
      case 0x0C:
	envelopeFile = "envelope_1100.png";
	break;
      case 0x0D:
	envelopeFile = "envelope_1101.png";
	break;
      case 0x0E:
	envelopeFile = "envelope_1110.png";
	break;
    }
    if( envelopeFile != null ) {
      URL url = getClass().getResource( "/images/psg/" + envelopeFile );
      if( url != null ) {
	buf.append( ": <img src=\"" );
	buf.append( url );
	buf.append( "\" />" );
      }
    }
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
  }


  public void fireStop()
  {
    this.threadEnabled = false;
    interrupt();
    wakeUp();
  }


  public int getFrameRate()
  {
    return this.frameRate;
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
	rv = this.envelopeShape;
	break;
      case 14:
	rv = this.portA;
	if( (this.modeBits & 0x40) == 0 ) {
	  rv = this.callback.psgReadPort( this, PORT_A );
	}
	break;
      case 15:
	rv = this.portB;
	if( (this.modeBits & 0x80) == 0 ) {
	  rv = this.callback.psgReadPort( this, PORT_B );
	}
	break;
    }
    return rv;
  }


  public void reset()
  {
    this.portA            = 0xFF;
    this.portB            = 0xFF;
    this.modeBits         = 0xFF;
    this.noiseShifter     = 0;
    this.amplitudeA       = 0;
    this.amplitudeB       = 0;
    this.amplitudeC       = 0;
    this.periodA          = 0;
    this.periodB          = 0;
    this.periodC          = 0;
    this.periodNoise      = 0;
    this.periodEnvelope   = 0;
    this.envelopeShape    = 0;
    this.regNum           = 0;
    this.channelOutA      = 0;
    this.channelOutB      = 0;
    this.channelOutC      = 0;
    this.noiseCounter     = 0;
    this.envPeriodCounter = 0;
    this.envShapeStep     = 0;
    this.envShapeValue    = 0;
    this.toneCounterA     = 0;
    this.toneCounterB     = 0;
    this.toneCounterC     = 0;
    this.toneStateA       = false;
    this.toneStateB       = false;
    this.toneStateC       = false;
    this.noiseState       = false;
    this.envelopeReset    = false;
    this.envelopeDiv2     = false;
    this.envelopeEnd      = false;
  }


  public void setFrameRate( int frameRate )
  {
    synchronized( this.waitMonitor ) {
      int oldFrameRate = this.frameRate;
      this.frameRate   = frameRate;
      if( (frameRate > 0) && (oldFrameRate <= 0) ) {
	wakeUp();
      }
    }
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
	}
	break;
      case 12:
	synchronized( this ) {
	  this.periodEnvelope = ((value << 8) & 0xFF00)
					| (this.periodEnvelope & 0x00FF);
	}
	break;
      case 13:
	synchronized( this ) {
	  this.envelopeShape = value & 0x0F;
	  this.envelopeEnd   = true;
	  this.envelopeReset = true;
	}
	break;
      case 14:
	if( (this.modeBits & 0x40) != 0 ) {
	  this.portA = value;
	  this.callback.psgWritePort( this, PORT_A, value );
	}
	break;
      case 15:
	if( (this.modeBits & 0x80) != 0 ) {
	  this.portB = value;
	  this.callback.psgWritePort( this, PORT_B, value );
	}
	break;
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    int remainClocks  = 0;
    int lastFrameRate = 0;
    int div8Counter   = 0;
    while( this.threadEnabled ) {

      // naechstes Frame erzeugen
      int frameRate = this.frameRate;
      if( frameRate > 0 ) {
	if( frameRate != lastFrameRate ) {
	  remainClocks = 0;
	}
	int clocksPerFrame = (this.clockHz + remainClocks) / frameRate;
	if( clocksPerFrame > 0 ) {
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
	  int n = div8Counter + clocksPerFrame;
	  while( n >= 8 ) {
	    n -= 8;
	    internalClockPhaseChange();
	  }
	  div8Counter = n;
	}
	this.callback.psgWriteFrame(
				this,
				this.channelOutA,
				this.channelOutB,
				this.channelOutC );
	remainClocks = this.clockHz - (clocksPerFrame * frameRate);
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
    /*
     * Noch ein Frame schreiben, um sicherzustellen,
     * dass der Audiokanal geschlossen werden kann.
     */
    this.callback.psgWriteFrame(
				this,
				this.channelOutA,
				this.channelOutB,
				this.channelOutC );
  }


	/* --- private Methoden --- */

  private int getOutValue( int amplitudeReg, boolean channelState )
  {
    int rv = 0;
    if( channelState ) {
      if( amplitudeReg > 0x0F ) {
	rv = volumeValues[ this.envShapeValue ];
      } else {
	rv = volumeValues[ amplitudeReg ];
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
	this.toneStateC = !this.toneStateC;
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
      /*
       * Das Rauschen wird ueber ein rueckgekoppeltes 17-stelliges
       * Schieberegister erzeugt:
       * Rueckkopplung: Negation(Bit13 XOR Bit16) -> Bit0
       * Ausgang:       Bit16
       */
      boolean bit13   = ((this.noiseShifter & 0x00002000) != 0);  // Bit13
      this.noiseState = ((this.noiseShifter & 0x00010000) != 0);  // Bit16
      this.noiseShifter <<= 1;
      if( bit13 == this.noiseState ) {
	this.noiseShifter |= 0x01;
      }
    }

    // Mixer
    boolean stateA = true;
    boolean stateB = true;
    boolean stateC = true;
    switch( this.modeBits & 0x09 ) {
      case 0x00:			// Rauschen + Tongenerator
	stateA = this.noiseState || this.toneStateA;
	break;
      case 0x01:			// Rauschen
	stateA = this.noiseState;
	break;
      case 0x08:			// Tongenerator
	stateA = this.toneStateA;
	break;
    }
    switch( this.modeBits & 0x12 ) {
      case 0x00:			// Rauschen + Tongenerator
	stateB = this.noiseState || this.toneStateB;
	break;
      case 0x02:			// Rauschen
	stateB = this.noiseState;
	break;
      case 0x10:			// Tongenerator
	stateB = this.toneStateB;
	break;
    }
    switch( this.modeBits & 0x24 ) {
      case 0x00:			// Rauschen + Tongenerator
	stateC = this.noiseState || this.toneStateC;
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
    this.envelopeDiv2 = !this.envelopeDiv2;
    if( this.envelopeDiv2 ) {
      if( this.envPeriodCounter > 0 ) {
	--this.envPeriodCounter;
      }
      if( this.envelopeReset || (this.envPeriodCounter == 0) ) {
	int envelopeShape = 0;
	synchronized( this ) {
	  envelopeShape         = this.envelopeShape;
	  this.envPeriodCounter = this.periodEnvelope;
	}
	if( this.envelopeReset
	    && (this.envelopeEnd || (this.envShapeStep == 0)) )
	{
	  this.envelopeReset = false;
	  this.envelopeEnd   = false;
	  this.envShapeStep  = 0;
	  if( (envelopeShape <= 3)
	      || ((envelopeShape >= 8) && (envelopeShape <= 11)) )
	  {
	    this.envShapeValue = 15;
	  } else {
	    this.envShapeValue = 0;
	  }
	}
	switch( envelopeShape ) {
	  case 4:						// /____
	  case 5:
	  case 6:
	  case 7:
	  case 15:
	    if( this.envShapeStep < 16 ) {
	      this.envShapeValue = (this.envShapeStep & 0x0F);
	      this.envShapeStep++;
	    } else {
	      this.envShapeValue = 0;
	      this.envelopeEnd   = true;
	    }
	    break;
	  case 8:						// \\\\
	    this.envShapeValue = (15 - this.envShapeStep) & 0x0F;
	    this.envShapeStep  = (this.envShapeStep + 1) & 0x0F;
	    break;
	  case 10:						// \/\/
	    if( this.envShapeStep < 16 ) {
	      this.envShapeValue = (15 - this.envShapeStep) & 0x0F;
	    } else {
	      this.envShapeValue = (this.envShapeStep - 16) & 0x0F;
	    }
	    this.envShapeStep  = (this.envShapeStep + 1) & 0x1F;
	    break;
	  case 11:						// \~~~
	    if( this.envShapeStep < 16 ) {
	      this.envShapeValue = (15 - this.envShapeStep) & 0x0F;
	      this.envShapeStep++;
	    } else {
	      this.envShapeValue = 15;
	      this.envelopeEnd   = true;
	    }
	    break;
	  case 12:						// ////
	    this.envShapeValue = this.envShapeStep & 0x0F;
	    this.envShapeStep  = (this.envShapeStep + 1) & 0x0F;
	    break;
	  case 13:						// /~~~
	    if( this.envShapeStep < 16 ) {
	      this.envShapeValue = this.envShapeStep & 0x0F;
	      this.envShapeStep++;
	    } else {
	      this.envShapeValue = 15;
	      this.envelopeEnd   = true;
	    }
	    break;
	  case 14:						// /\/\
	    if( this.envShapeStep < 16 ) {
	      this.envShapeValue = this.envShapeStep & 0x0F;
	    } else {
	      this.envShapeValue = (31 - this.envShapeStep) & 0x0F;
	    }
	    this.envShapeStep = (this.envShapeStep + 1) & 0x1F;
	    break;
	  default:						// \___
	    if( this.envShapeStep < 16 ) {
	      this.envShapeValue = (15 - this.envShapeStep) & 0x0F;
	      this.envShapeStep++;
	    } else {
	      this.envShapeValue = 0;
	      this.envelopeEnd   = true;
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
