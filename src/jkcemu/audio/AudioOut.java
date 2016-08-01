/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang)
 *
 * Die Ausgabe erfolgt als Rechteckkurve
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;
import jkcemu.base.*;
import z80emu.*;


public abstract class AudioOut extends AudioIO
{
  public static final int MAX_OUT_VALUE  = 255;
  public static final int MAX_USED_VALUE = MAX_OUT_VALUE * 9 / 10;

  protected static final int SIGNED_VALUE_1 = MAX_USED_VALUE / 2;
  protected static final int SIGNED_VALUE_0 = -SIGNED_VALUE_1;


  protected EmuThread emuThread;
  protected boolean   enabled;

  private boolean firstPhaseChange;
  private long    lastTStates;


  protected AudioOut( AudioFrm audioFrm, Z80CPU z80cpu )
  {
    super( audioFrm, z80cpu );
    this.enabled          = false;
    this.firstPhaseChange = false;
  }


  public void fireReopenLine()
  {
    this.audioFrm.fireReopenLine();
  }


  public boolean isSoundOutEnabled()
  {
    return false;
  }


  protected void writeSamples( int nSamples, boolean phase )
  {
    int value = (phase ? MAX_USED_VALUE : 0);
    writeSamples( nSamples, value, value, value );
  }


  /*
   * Wo ein direkter Zugriff auf den Audiokanal notwendig ist,
   * muss die Methode ueberschrieben werden.
   * Der Wertebereich ist unabhaengig vom konkreten AudioFormat
   * 0...MAX_OUT_VALUE.
   */
  public void writeSamples(
			int nSamples,
			int monoValue,
			int leftValue,
			int rightValue )
  {
    // leer
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und besagt, dass auf die entsprechenden Ausgabeleitung
   * ein Wert geschrieben wurde.
   */
  public void writePhase( boolean phase )
  {
    if( this.enabled && (this.tStatesPerFrame > 0) ) {
      if( this.firstCall ) {
	this.firstCall        = false;
	this.firstPhaseChange = true;
	this.lastPhase        = phase;

      } else {

	if( phase != this.lastPhase ) {
	  this.lastPhase = phase;
	  if( this.firstPhaseChange ) {
	    this.firstPhaseChange = false;
	    this.lastTStates      = this.z80cpu.getProcessedTStates();
	  } else {
	    long tStates     = this.z80cpu.getProcessedTStates();
	    long diffTStates = this.z80cpu.calcTStatesDiff(
						  this.lastTStates,
						  tStates );
	    if( diffTStates > 0 ) {

	      // Anzahl der zu erzeugenden Samples
	      int nSamples = (int) (diffTStates / this.tStatesPerFrame);
	      if( currentDiffTStates( diffTStates ) ) {
		writeSamples( nSamples, phase );
	      }

	      /*
	       * Anzahl der verstrichenen Taktzyklen auf den Wert
	       * des letzten ausgegebenen Samples korrigieren
	       */
	      this.lastTStates += (nSamples * this.tStatesPerFrame);
	    }
	  }
	}
      }
    }
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und schreibt synchron zur verstrichenen CPU-Taktzyklenzahl
   * einen Byte-Wert in den Audiokanal.
   * Wertebereich: 0...MAX_OUT_VALUE
   */
  public void writeValue( int monoValue, int leftValue, int rightValue )
  {
    if( this.enabled && (this.tStatesPerFrame > 0) ) {
      if( this.firstCall ) {
	this.firstCall   = false;
	this.lastTStates = this.z80cpu.getProcessedTStates();
	this.lastPhase   = false;

      } else {

	long tStates     = this.z80cpu.getProcessedTStates();
	long diffTStates = this.z80cpu.calcTStatesDiff(
					      this.lastTStates,
					      tStates );
	if( diffTStates > 0 ) {

	  // Anzahl der zu erzeugenden Samples
	  int nSamples  = (int) (diffTStates / this.tStatesPerFrame);
	  if( currentDiffTStates( diffTStates ) ) {
	    writeSamples( nSamples, monoValue, leftValue, rightValue );
	  }

	  /*
	   * Anzahl der verstrichenen Taktzyklen auf den Wert
	   * des letzten ausgegebenen Samples korrigieren
	   */
	  this.lastTStates += (nSamples * this.tStatesPerFrame);
	}
      }
    }
  }
}
