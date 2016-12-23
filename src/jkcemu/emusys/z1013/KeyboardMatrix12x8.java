/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten) einer Z1013-Tastatur,
 * die nach Brosig/Eisenkolb angeschlossen ist (12x8-Matrix).
 */

package jkcemu.emusys.z1013;

import java.lang.*;
import java.util.Arrays;


public abstract class KeyboardMatrix12x8 extends KeyboardMatrix
{
  protected int matrixNormal[];
  protected int matrixShift[];
  protected int ctrlCol;
  protected int ctrlValue;
  protected int shiftCol;
  protected int shiftValue;

  protected int[] keyboardMatrix;

  protected boolean ctrlPressed;
  protected boolean shiftPressed;


  protected KeyboardMatrix12x8()
  {
    // werden von Subklasse ueberschrieben
    this.matrixNormal = null;
    this.matrixShift  = null;
    this.ctrlCol      = -1;
    this.ctrlValue    = 0;
    this.shiftCol     = -1;
    this.shiftValue   = 0;

    // eigentliche Initialisierungen
    this.keyboardMatrix = new int[ 12 ];
    reset();
  }


  protected void updShiftKeysPressed()
  {
    setShiftKeysPressed( this.ctrlPressed || this.shiftPressed );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getRowValues( int col, boolean pioB4State )
  {
    int rv = 0;
    int m  = 0;
    if( col >= 0 ) {
      if( ((col == 15) || (col == this.ctrlCol))
	  && this.ctrlPressed
	  && (this.ctrlCol >= 0)
	  && (this.ctrlCol < this.keyboardMatrix.length) )
      {
	m |= this.ctrlValue;
      }
      if( ((col == 15) || (col == this.shiftCol))
	  && this.shiftPressed
	  && (this.shiftCol >= 0)
	  && (this.shiftCol < this.keyboardMatrix.length) )
      {
	m |= this.shiftValue;
      }
      if( !onlyShiftKeysReadable() ) {
	if( col == 15 ) {
	  for( int i = 0; i < this.keyboardMatrix.length; i++ ) {
	    m |= this.keyboardMatrix[ i ];
	  }
	} else {
	  if( col < this.keyboardMatrix.length ) {
	    m |= this.keyboardMatrix[ col ];
	  }
        }
      }
      if( (m & 0x55) != 0 ) {
	rv |= 0x01;
      }
      if( (m & 0x66) != 0 ) {
	rv |= 0x02;
      }
      if( (m & 0x78) != 0 ) {
	rv |= 0x04;
      }
      if( (m & 0x80) != 0 ) {
	rv |= 0x08;
      }
    }
    return rv;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.ctrlPressed  = false;
    this.shiftPressed = false;
    Arrays.fill( this.keyboardMatrix, 0 );
  }


  @Override
  protected synchronized boolean updKeyboardMatrix(
                                                int     keyCharCode,
                                                boolean hexMode )
  {
    boolean rv = updKeyboardMatrixInternal( keyCharCode );
    if( !rv ) {
      if( keyCharCode < '\u0020' ) {
	if( updKeyboardMatrixInternal( keyCharCode + 'A' - 1 ) ) {
	  this.ctrlPressed = true;
	  rv               = true;
	}
      }
    }
    if( rv ) {
      updShiftKeysPressed();
    }
    return rv;
  }


	/* --- private Methoden --- */

  private boolean updKeyboardMatrixInternal( int keyCharCode )
  {
    boolean rv = false;
    if( keyCharCode > 0 ) {
      int pos = indexOf( this.matrixNormal, keyCharCode );
      if( pos >= 0 ) {
	rv    = true;
	int m = (1 << (pos / 12));
	this.keyboardMatrix[ pos % 12 ] |= m;
      } else {
	pos = indexOf( this.matrixShift, keyCharCode );
	if( pos >= 0 ) {
	  rv    = true;
	  int m = (1 << (pos / 12));
	  this.keyboardMatrix[ pos % 12 ] |= m;
	  this.shiftPressed = true;
	}
      }
    }
    return rv;
  }


  private int indexOf( int[] a, int value )
  {
    int rv = -1;
    for( int i = 0; i < a.length; i++ ) {
      if( a[ i ] == value ) {
	rv = i;
	break;
      }
    }
    return rv;
  }
}
