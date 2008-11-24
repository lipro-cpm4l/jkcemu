/*
 * (c) 2008 Jens Mueller
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
  public static final int PHASE0_VALUE = (-100 & 0xFF);
  public static final int PHASE1_VALUE = 100;

  protected EmuThread emuThread;
  protected boolean   enabled;
  protected int       maxPauseTStates;


  protected AudioOut( Z80CPU z80cpu )
  {
    super( z80cpu );
    this.enabled         = false;
    this.maxPauseTStates = 0;
  }


  protected abstract void writeSamples( int nSamples, boolean phase );


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und besagt, dass in das entsprechende Tor der PIO
   * ein Wert geschrieben wurde.
   */
  public void updPhase( boolean phase )
  {
    if( this.enabled && (this.tStatesPerFrame > 0) ) {
      if( this.firstCall ) {
	this.firstCall   = false;
	this.lastTStates = this.z80cpu.getProcessedTStates();
	this.lastPhase   = phase;

      } else {

	if( phase != this.lastPhase ) {
	  this.lastPhase  = phase;
	  int tStates     = this.z80cpu.getProcessedTStates();
	  int diffTStates = this.z80cpu.calcTStatesDiff(
						this.lastTStates,
						tStates );
	  if( diffTStates > 0 ) {
	    currentTStates( tStates, diffTStates );
	    if( tStates > this.lastTStates ) {

	      // Anzahl der zu erzeugenden Samples
	      int nSamples  = diffTStates / this.tStatesPerFrame;
	      writeSamples( nSamples, phase );

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
}
