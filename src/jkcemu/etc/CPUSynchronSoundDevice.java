/*
 * (c) 2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Repraesentation eines Tongenerators,
 * der Toene synchron zur CPU erzeugt
 */

package jkcemu.etc;

import jkcemu.audio.AbstractSoundDevice;
import jkcemu.audio.AudioOut;
import z80emu.Z80CPU;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class CPUSynchronSoundDevice
			extends AbstractSoundDevice
			implements
				Z80MaxSpeedListener,
				Z80TStatesListener
{
  private boolean singleBit;
  private int     curValueM;
  private int     curValueL;
  private int     curValueR;
  private int     tStatesCounter;
  private int     tStatesPeriod;
  private int     frameRate;
  private int     maxSpeedKHz;


  public CPUSynchronSoundDevice(
			String  text,
			boolean stereo,
			boolean singleBit )
  {
    super( text, stereo );
    this.singleBit = singleBit;
    reset();
  }


  public CPUSynchronSoundDevice( String text )
  {
    this( text, false, true );
    reset();
  }


  public void setCurPhase( boolean phase )
  {
    int value      = (phase ? AudioOut.MAX_USED_UNSIGNED_VALUE : 0);
    this.curValueM = value;
    this.curValueL = value;
    this.curValueR = value;
  }


  public void setCurValues( int valueM, int valueL, int valueR )
  {
    this.curValueM = valueM;
    this.curValueL = valueL;
    this.curValueR = valueR;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean isSingleBitMode()
  {
    return this.singleBit;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.curValueM      = 0;
    this.curValueL      = 0;
    this.curValueR      = 0;
    this.tStatesCounter = 0;
    this.tStatesPeriod  = 0;
    this.frameRate      = 0;
    this.maxSpeedKHz    = 0;
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    updTStatesPeriod();
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      if( this.tStatesPeriod > 0 ) {
	if( this.tStatesCounter > 0 ) {
	  this.tStatesCounter -= tStates;
	} else {
	  this.tStatesCounter = this.tStatesPeriod;
	  audioOut.writeValues(
			this.curValueM,
			this.curValueL,
			this.curValueR );
	}
      } else {
	audioOut.writeValues(
			this.curValueM,
			this.curValueL,
			this.curValueR );
	this.frameRate = audioOut.getFrameRate();
	updTStatesPeriod();
      }
    }
  }


	/* --- private Methoden --- */

  private void updTStatesPeriod()
  {
    int frameRate = this.frameRate;
    if( frameRate > 0 ) {
      // Ausgabe mit etwa der doppelten Abtastfrequenz
      this.tStatesPeriod = Math.min( this.maxSpeedKHz * 1000 / frameRate, 1 );
    } else {
      this.tStatesPeriod = 0;
    }
  }
}
