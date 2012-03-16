/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten)
 * der Z1013-Folientastatur (8x4-Matrix).
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;


public class KeyboardMatrix8x4 extends KeyboardMatrix
{
  private int[] keyboardMatrix;


  public KeyboardMatrix8x4()
  {
    this.keyboardMatrix = new int[ 8 ];
    reset();
  }


  /*
   * Die Methode besagt, ob eine Cursor-, Leer- oder Enter-Taste
   * gedrueckt ist
   */
  public boolean isAnyControlKeyPressed()
  {
    boolean rv = false;
    for( int i = 0; i < 4; i++ ) {
      if( (this.keyboardMatrix[ i ] & 0x08) != 0 ) {
	rv = true;
	break;
      }
    }
    return rv;
  }


  public synchronized void updKeyboardMatrix( int[] kbMatrix )
  {
    int n = Math.min( kbMatrix.length, this.keyboardMatrix.length );
    int i = 0;
    while( i < n ) {
      this.keyboardMatrix[ i ] = kbMatrix[ i ];
      i++;
    }
    while( i < this.keyboardMatrix.length ) {
      this.keyboardMatrix[ i ] = 0;
      i++;
    }
    updShiftKeysPressed();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getKeyboardType()
  {
    return "8x4";
  }


  /*
   * Bei der 8x4-Matrix wird wird der Wert von PIO B4 nicht ausgewertet.
   */
  @Override
  public int getRowValues( int col, boolean pioB4State )
  {
    int rv = 0;
    if( col >= 0 ) {
      if( onlyShiftKeysReadable() ) {
	if( col < 4 ) {
	  rv = this.keyboardMatrix[ col ] & 0x08;
	}
      } else {
	if( col < this.keyboardMatrix.length ) {
	  rv = this.keyboardMatrix[ col ];
	}
      }
    }
    return rv;
  }


  @Override
  public void reset()
  {
    super.reset();
    Arrays.fill( this.keyboardMatrix, 0 );
  }


  @Override
  public synchronized boolean setKeyCode( int keyCode )
  {
    boolean rv = true;
    reset();
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
	this.keyboardMatrix[ 4 ] = 0x08;
	break;

      case KeyEvent.VK_RIGHT:
	this.keyboardMatrix[ 6 ] = 0x08;
	break;

      case KeyEvent.VK_ENTER:
	this.keyboardMatrix[ 7 ] = 0x08;
	break;

      case KeyEvent.VK_SPACE:
	this.keyboardMatrix[ 5 ] = 0x08;
	break;

      case KeyEvent.VK_BACK_SPACE:
	setKeyCharCode( 8, false );
	break;

      case KeyEvent.VK_TAB:
	setKeyCharCode( '\t', false );
	break;

      case KeyEvent.VK_DOWN:
	setKeyCharCode( 10, false );
	break;

      case KeyEvent.VK_UP:
	setKeyCharCode( 11, false );
	break;

      case KeyEvent.VK_DELETE:
	setKeyCharCode( 0x7F, false );
	break;

      case KeyEvent.VK_F1:
	this.keyboardMatrix[ 0 ] = 0x08;
	break;

      case KeyEvent.VK_F2:
	this.keyboardMatrix[ 1 ] = 0x08;
	break;

      case KeyEvent.VK_F3:
	this.keyboardMatrix[ 2 ] = 0x08;
	break;

      case KeyEvent.VK_F4:
	this.keyboardMatrix[ 3 ] = 0x08;
	break;

      default:
	rv = false;
    }
    updShiftKeysPressed();
    return rv;
  }


  @Override
  public void updKeyboardFld( AbstractKeyboardFld keyboardFld )
  {
    if( keyboardFld != null )
      keyboardFld.updKeySelection( this.keyboardMatrix );
  }


  /*
   * Diese Methode bildet den in "keyCharCode" enthaltenen Tastencode
   * auf der Tastaturmatrix ab.
   * Die Codes 08h, 09h, 0Dh und 20h werden nicht in die Sondertasten,
   * sondern in Tastenkombinationen mit S4 umgesetzt,
   * da sie ja auch auf der Emulator-Tastatur mit Control erzeugt wurden.
   * Enter, Leer- und Cursor-Tasten werden separat behandelt.
   */
  @Override
  protected synchronized boolean updKeyboardMatrix(
						int     keyCharCode,
						boolean hexMode )
  {
    boolean rv = false;
    if( keyCharCode > 0 ) {
      int col = keyCharCode & 0x07;
      switch( keyCharCode & 0xF8 ) {
	case '@':
	  this.keyboardMatrix[ col ] |= 0x01;
	  rv = true;
	  break;
	case 'H':
	  this.keyboardMatrix[ col ] |= 0x02;
	  if( hexMode ) {
	    this.keyboardMatrix[ 0 ] |= 0x08;		// S1
	  }
	  rv = true;
	  break;
	case 'P':
	  this.keyboardMatrix[ col ] |= 0x04;
	  if( hexMode ) {
	    this.keyboardMatrix[ 0 ] |= 0x08;		// S1
	  }
	  rv = true;
	  break;
	case 'X':
	  this.keyboardMatrix[ col ] |= 0x01;
	  this.keyboardMatrix[ 0 ]   |= 0x08;		// S1
	  rv = true;
	  break;
	case '0':
	  this.keyboardMatrix[ col ] |= 0x02;
	  if( !hexMode ) {
	    this.keyboardMatrix[ 0 ] |= 0x08;		// S1
	  }
	  rv = true;
	  break;
	case '8':
	  this.keyboardMatrix[ col ] |= 0x04;
	  if( !hexMode ) {
	    this.keyboardMatrix[ 0 ] |= 0x08;		// S1
	  }
	  rv = true;
	  break;
	case 'x':
	  this.keyboardMatrix[ col ] |= 0x01;
	  this.keyboardMatrix[ 1 ]   |= 0x08;		// S2
	  rv = true;
	  break;
	case '\u0020':
	  this.keyboardMatrix[ col ] |= 0x02;
	  this.keyboardMatrix[ 1 ]   |= 0x08;		// S2
	  rv = true;
	  break;
	case '(':
	  this.keyboardMatrix[ col ] |= 0x04;
	  this.keyboardMatrix[ 1 ]   |= 0x08;		// S2
	  rv = true;
	  break;
	case '\u0060':
	  this.keyboardMatrix[ col ] |= 0x01;
	  this.keyboardMatrix[ 2 ]   |= 0x08;		// S3
	  rv = true;
	  break;
	case 'h':
	  this.keyboardMatrix[ col ] |= 0x02;
	  this.keyboardMatrix[ 2 ]   |= 0x08;		// S3
	  rv = true;
	  break;
	case 'p':
	  this.keyboardMatrix[ col ] |= 0x04;
	  this.keyboardMatrix[ 2 ]   |= 0x08;		// S3
	  rv = true;
	  break;
	case 0x10:
	  this.keyboardMatrix[ col ] |= 0x01;
	  this.keyboardMatrix[ 3 ]   |= 0x08;		// S4
	  rv = true;
	  break;
	case 0:
	  this.keyboardMatrix[ col ] |= 0x02;
	  this.keyboardMatrix[ 3 ]   |= 0x08;		// S4
	  rv = true;
	  break;
	case 8:
	  this.keyboardMatrix[ col ] |= 0x04;
	  this.keyboardMatrix[ 3 ]   |= 0x08;		// S4
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      updShiftKeysPressed();
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void updShiftKeysPressed()
  {
    setShiftKeysPressed( isAnyControlKeyPressed() );
  }
}

