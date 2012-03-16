/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstrakte Beschreibung eines Zustandes (gedrueckte Tasten)
 * der Z1013-Tastatur.
 */

package jkcemu.emusys.z1013;

import java.lang.*;
import jkcemu.Main;
import jkcemu.base.*;


public abstract class KeyboardMatrix
{
  protected int  shiftPreHoldMillis;
  private   long readShiftKeysOnlyTill;


  protected KeyboardMatrix()
  {
    this.shiftPreHoldMillis    = 50;
    this.readShiftKeysOnlyTill = -1L;
  }


  public abstract String getKeyboardType();


  /*
   * Die Methode liest den Zeilenwert (4 Bits) der Tastaturmatrix
   * fuer die uebergebene Spalte.
   * Der Wert von PIO B Port 4 wird ebenfalls uebergeben,
   * da der er fuer die 8x8-Tastaturmatrix benoetigt wird.
   */
  public abstract int getRowValues( int col, boolean pioB4 );


  protected boolean onlyShiftKeysReadable()
  {
    boolean state = false;
    if( this.readShiftKeysOnlyTill >= 0L ) {
      if( System.currentTimeMillis() < this.readShiftKeysOnlyTill ) {
	state = true;
      } else {
	this.readShiftKeysOnlyTill = -1L;
      }
    }
    return state;
  }


  public void reset()
  {
    this.readShiftKeysOnlyTill = -1L;
  }


  protected void setShiftKeysPressed( boolean state )
  {
    if( state ) {
      this.readShiftKeysOnlyTill = System.currentTimeMillis()
					+ (long) this.shiftPreHoldMillis;
    } else {
      this.readShiftKeysOnlyTill = -1L;
    }
  }


  public boolean setKeyCharCode( int keyCharCode, boolean hexMode )
  {
    boolean rv = false;
    reset();
    if( (keyCharCode > 0) && (keyCharCode <= 0xFF) ) {
      rv = updKeyboardMatrix( keyCharCode, hexMode );
    } else {
      setShiftKeysPressed( false );
    }
    return rv;
  }


  public abstract boolean setKeyCode( int keyCode );


  public void setKeyReleased()
  {
    reset();
  }


  public void updKeyboardFld( AbstractKeyboardFld keyboardFld )
  {
    // leer
  }


  protected abstract boolean updKeyboardMatrix(
					int     keyCharCode,
					boolean hexMode );
}

