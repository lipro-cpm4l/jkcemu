/*
 * (c) 2008 Jens Mueller
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


public class AudioDataQueue extends InputStream
{
  private byte[] phaseData;
  private int    size;
  private int    pos;
  private int    len;
  private String errorText;


  public AudioDataQueue( int initSize )
  {
    this.phaseData = new byte[ initSize ];
    this.size      = 0;
    this.pos       = 0;
    this.len       = 0;
    this.errorText = null;
  }


  public String getErrorText()
  {
    return this.errorText;
  }


  public int length()
  {
    return this.len;
  }


  public boolean markSupported()
  {
    return false;
  }


  public void putPhase( boolean phase )
  {
    if( this.phaseData != null ) {
      if( this.size > 0 ) {
	boolean lastPhase = (this.phaseData[ this.size - 1 ] > 0);
	if( phase == lastPhase ) {
	  putLastPhaseAgain();
	} else {
	  if( ensureSize() ) {
	    this.phaseData[ this.size ] = (byte) (phase ? 1 : -1);
	    this.size++;
	  }
	}
      } else {
	this.phaseData[ 0 ] = (byte) (phase ? 1 : -1);
	this.size           = 1;
      }
      this.len++;
    }
  }


  public int read()
  {
    while( this.pos < this.size ) {
      if( this.phaseData[ this.pos ] > 0 ) {
	this.phaseData[ this.pos ]--;
	return AudioOut.PHASE1_VALUE;
      }

      if( this.phaseData[ this.pos ] < 0 ) {
	this.phaseData[ this.pos ]++;
	return AudioOut.PHASE0_VALUE;
      }

      /*
       * Das Element ist 0 und wurde somit schon vollstaendig gelesen
       * -> naechstes Feld beginnen zu lesen
       */
      this.pos++;
    }
    return -1;
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

