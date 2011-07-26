/*
 * (c) 2008-2011 Jens Mueller
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
  public static final int PHASE0_VALUE = 35;
  public static final int PHASE1_VALUE = 220;

  protected EmuThread emuThread;
  protected boolean   enabled;
  protected int       maxPauseTStates;

  private boolean firstPhaseChange;


  protected AudioOut( Z80CPU z80cpu )
  {
    super( z80cpu );
    this.enabled          = false;
    this.firstPhaseChange = false;
    this.maxPauseTStates  = 0;
  }


  public boolean isSoundOutEnabled()
  {
    return false;
  }


  protected abstract void writeSamples( int nSamples, boolean phase );


  protected void writeSamples( int nSamples, byte value )
  {
    /*
     * Die Moeglichkeit, beliebige Audio-Werte auszugeben,
     * wird nicht allgemein unterstuetzt.
     * Deshalb ist die Methode hier leer.
     * Sie muss in der abgeleiteten Klasse ueberschrieben werden,
     * wo dieses Feature unterstuetzt werden soll.
     */
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


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und schreibt synchron zur verstrichenen CPU-Taktzyklenzahl
   * einen Byte-Wert in den Audiokanal.
   */
  public void writeValue( byte value )
  {
    if( this.enabled && (this.tStatesPerFrame > 0) ) {
      if( this.firstCall ) {
	this.firstCall   = false;
	this.lastTStates = this.z80cpu.getProcessedTStates();
	this.lastPhase   = false;

      } else {

	int tStates     = this.z80cpu.getProcessedTStates();
	int diffTStates = this.z80cpu.calcTStatesDiff(
					      this.lastTStates,
					      tStates );
	if( diffTStates > 0 ) {
	  currentTStates( tStates, diffTStates );
	  if( tStates > this.lastTStates ) {

	    // Anzahl der zu erzeugenden Samples
	    int nSamples  = diffTStates / this.tStatesPerFrame;
	    writeSamples( nSamples, value );

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
