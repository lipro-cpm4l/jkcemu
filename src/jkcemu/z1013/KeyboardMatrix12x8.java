/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten) einer Tastatur,
 * die nach Brosig/Eisenkolb angeschlossen ist (12x8-Matrix).
 */

package jkcemu.z1013;

import java.lang.*;


public abstract class KeyboardMatrix12x8 extends KeyboardMatrix
{
  protected int matrixNormal[];
  protected int matrixShift[];
  protected int ctrlCol;
  protected int ctrlValue;
  protected int shiftCol;
  protected int shiftValue;

  protected int[] rowMasks;

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
    this.rowMasks = new int[ 8 ];
    reset();
  }


  /*
   * Bei der 12x8-Matrix wird der Wert von PIO B4 nicht ausgewertet.
   */
  public int getRowValues( int col, boolean pioB4State )
  {
    int rv = 0;
    if( (col >= 0) && (col <= 15) ) {

      if( !onlyShiftKeysReadable() ) {
	int colMask = (col == 15 ? 0x0FFF : (1 << col));

	rv = 0;
	for( int i = 0; i < this.rowMasks.length; i++ ) {
	  if( (this.rowMasks[ i ] & colMask) != 0 )
	    rv |= i + 1;
	}
      }

      if( col < 15 ) {
	if( this.ctrlPressed && (col == this.ctrlCol) )
	  rv |= this.ctrlValue;

	if( this.shiftPressed && (col == this.shiftCol) )
	  rv |= this.shiftValue;
      }
    }
    return rv;
  }


  public void reset()
  {
    super.reset();
    this.ctrlPressed  = false;
    this.shiftPressed = false;
    for( int i = 0; i < this.rowMasks.length; i++ )
      this.rowMasks[ i ] = 0;
  }


  protected void updRowMasks()
  {
    if( !updRowMasks( this.keyCharCode ) ) {
      if( keyCharCode < '\u0020' ) {
	if( updRowMasks( this.keyCharCode + 'A' - 1 ) )
	  this.ctrlPressed = true;
      }
    }
    updShiftKeysPressed();
  }


  protected void updShiftKeysPressed()
  {
    setShiftKeysPressed( this.ctrlPressed || this.shiftPressed );
  }


	/* --- private Methoden --- */

  private boolean updRowMasks( int keyCharCode )
  {
    boolean rv = false;
    if( keyCharCode > 0 ) {
      int pos = indexOf( this.matrixNormal, keyCharCode );
      if( pos >= 0 ) {
	rv    = true;
	int m = (1 << (pos % 12));
	this.rowMasks[ pos / 12 ] |= m;
      } else {
	pos = indexOf( this.matrixShift, keyCharCode );
	if( pos >= 0 ) {
	  rv    = true;
	  int m = (1 << (pos % 12));
	  this.rowMasks[ pos / 12 ] |= m;
	  this.shiftPressed = true;
	}
      }
    }
    return rv;
  }


  private int indexOf( int[] a, int value )
  {
    for( int i = 0; i < a.length; i++ ) {
      if( a[ i ] == value )
	return i;
    }
    return -1;
  }
}

