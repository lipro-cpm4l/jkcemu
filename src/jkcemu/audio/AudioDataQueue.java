/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speicheroptimierte Queue fuer Audio-Daten
 *
 * Auf der einen Seite wird die Phasenlage hineingeschrieben.
 * Auf der anderen Seite koennen die daraus gebildeten Audio-Daten
 * als InputStream gelesen werden.
 * Mit dem Lesen sollte erst begonnen werden,
 * wenn keine Daten mehr hineingeschrieben werden.
 *
 * In jedem Element des Attributs "phaseData" werden die Anzahl
 * mit gleicher Phasenlage gespeichert.
 * Dabei bedeutet ein positiver Wert eine positive Phase
 * und ein negativer Wert eine negative Phase.
 * Wenn ein Element ueberlaueft, wird das nachste Element
 * mit der gleichen Phasenlage beschrieben.
 */

package jkcemu.audio;

import java.io.InputStream;
import java.lang.*;
import jkcemu.base.EmuUtil;


public class AudioDataQueue extends InputStream
{
  private byte[]  phaseData;
  private int     size;
  private int     pos;
  private int     len;
  private int     remainSamples;
  private boolean lastPhase;
  private String  errorText;


  public AudioDataQueue( int initSize )
  {
    this.phaseData     = new byte[ initSize ];
    this.size          = 0;
    this.pos           = 0;
    this.len           = 0;
    this.remainSamples = 0;
    this.lastPhase     = false;
    this.errorText     = null;
  }


  public void appendPauseFrames( int nSamples )
  {
    this.remainSamples += nSamples;
  }


  public String getErrorText()
  {
    return this.errorText;
  }


  public int length()
  {
    if( this.len < this.remainSamples ) {
      this.len = this.remainSamples;
    }
    return this.len;
  }


  public void putPhase( boolean phase )
  {
    if( this.phaseData != null ) {
      if( this.size > 0 ) {
	this.lastPhase = (this.phaseData[ this.size - 1 ] > 0);
	if( phase == this.lastPhase ) {
	  putLastPhaseAgain();
	} else {
	  if( ensureSize() ) {
	    this.phaseData[ this.size ] = (byte) (phase ? 1 : -1);
	    this.size++;
	  }
	  this.lastPhase = !phase;
	}
      } else {
	this.phaseData[ 0 ] = (byte) (phase ? 1 : -1);
	this.size           = 1;
      }
      this.remainSamples++;
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean markSupported()
  {
    return false;
  }


  @Override
  public int read()
  {
    int rv = -1;
    while( this.pos < this.size ) {
      if( this.phaseData[ this.pos ] > 0 ) {
	this.phaseData[ this.pos ]--;
	rv = AudioOut.PHASE1_VALUE;
	break;
      }

      if( this.phaseData[ this.pos ] < 0 ) {
	this.phaseData[ this.pos ]++;
	rv = AudioOut.PHASE0_VALUE;
	break;
      }

      /*
       * Das Element ist 0 und wurde somit schon vollstaendig gelesen
       * -> naechstes Feld beginnen zu lesen
       */
      this.pos++;
    }
    if( (rv == -1) && (this.remainSamples > 0) ) {
      --this.remainSamples;
      rv = AudioOut.PHASE0_VALUE
		+ ((AudioOut.PHASE1_VALUE - AudioOut.PHASE0_VALUE) / 2);
    }
    return rv;
  }


	/* --- private Methoden --- */

  private boolean ensureSize()
  {
    boolean status = true;
    if( this.size >= this.phaseData.length ) {
      try {
	int stepSize = this.phaseData.length / 2;
	if( stepSize < 0x100 ) {
	  stepSize = 0x100;
	}
	else if( stepSize > 0x100000 ) {
	  stepSize = 0x100000;
	}
	byte[] buf = new byte[ this.phaseData.length + stepSize ];
	System.arraycopy( this.phaseData, 0, buf, 0, this.phaseData.length );
	this.phaseData = buf;
      }
      catch( OutOfMemoryError ex ) {
	status         = false;
	this.phaseData = null;
	this.errorText = "Kein Speicher mehr f\u00FCr die Aufzeichnung\n"
				+ "der Audio-Daten verf\u00FCgbar.";
	System.gc();
      }
    }
    return status;
  }


  private void putLastPhaseAgain()
  {
    final int  i = this.size - 1;
    final byte v = this.phaseData[ i ];
    if( v > 0 ) {
      if( v == Byte.MAX_VALUE ) {
	if( ensureSize() ) {
	  this.phaseData[ this.size ] = 1;
	  this.size++;
	}
      } else {
	this.phaseData[ i ]++;
      }
    }
    else if( v < 0 ) {
      if( v == Byte.MIN_VALUE ) {
	if( ensureSize() ) {
	  this.phaseData[ this.size ] = -1;
	  this.size++;
	}
      } else {
	--this.phaseData[ i ];
      }
    }
  }
}
