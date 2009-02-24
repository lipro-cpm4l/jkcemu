/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstrakte Beschreibung eines Zustandes (gedrueckte Tasten)
 * der Tastatur.
 */

package jkcemu.system.z1013;

import java.lang.*;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public abstract class KeyboardMatrix
{
  protected int  keyCharCode;
  protected int  shiftPreHoldMillis;
  private   long readShiftKeysOnlyTill;


  protected KeyboardMatrix()
  {
    this.keyCharCode        = 0;
    this.shiftPreHoldMillis = EmuUtil.parseInt(
		Main.getProperty( "j1013.keyboard.shift.prehold.ms" ),
		0,
		50 );

    this.readShiftKeysOnlyTill = -1L;
  }


  public int getKeyCharCode()
  {
    return this.keyCharCode;
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
      if( System.currentTimeMillis() < this.readShiftKeysOnlyTill )
	state = true;
      else
	this.readShiftKeysOnlyTill = -1L;
    }
    return state;
  }


  public void reset()
  {
    this.keyCharCode           = 0;
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


  public boolean setKeyCharCode( int keyCharCode )
  {
    boolean rv = false;

    reset();
    this.keyCharCode = keyCharCode;

    // Tastencode in Matrix abbilden
    if( (this.keyCharCode > 0) && (this.keyCharCode <= 0xFF) ) {
      rv = updRowMasks();
    } else {
      this.keyCharCode = 0;
      setShiftKeysPressed( false );
    }
    return rv;
  }


  public abstract boolean setKeyCode( int keyCode );


  public void setKeyReleased()
  {
    reset();
  }


  protected abstract boolean updRowMasks();
}

