/*
 * (c) 2011-2017 Jens Mueller
 *
 * Z1013-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten) der Tastatur K7669,
 * die nach Brosig/Eisenkolb angeschlossen ist (12x8-Matrix).
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyEvent;


public class KeyboardMatrixK7669 extends KeyboardMatrix12x8
{
  private static int myMatrixNormal[] = {
	'1',  '3', '5', 0,    '7', '9', '-',  0, '^',  '_', 0, 0,
	'Q',  'E', 'T', 0,    'U', 'O', 0x08, 0, '\r', 0,   0, 0,
	'A',  'D', 'G', 0,    'J', 'L', 0x0A, 0, '[',  0,   0, 0,
	'Y',  'C', 'B', 0,    'M', '.', 0x03, 0, ']',  0,   0, 0,
	'2',  '4', '6', 0,    '8', '0', 0x0B, 0, 0x20, 0,   0, 0,
	'W',  'R', 'Z', 0,    'I', 'P', 0x09, 0, 0,    0,   0, 0,
	'S',  'F', 'H', 0,    'K', ':', '#',  0, 0x12, 0,   0, 0,
	'X',  'V', 'N', 0,    ',', ';', '\\', 0, 0x10, 0,   0, 0 };


  private static int myMatrixShift[] = {
	'!',  '@', '%', 0x0C, '/', ')', '+',  0, '~',  0,    0, 0,
	'q',  'e', 't', 0x09, 'u', 'o', 0,    0, 0,    0,    0, 0,
	'a',  'd', 'g', 0x1C, 'j', 'l', 0x8A, 0, '{',  0,    0, 0,
	'y',  'c', 'b', 0x11, 'm', '>', 0x8A, 0, '}',  0,    0, 0,
	'\"', '$', '&', 0x02, '(', '=', 0x8B, 0, 0,    0x7F, 0, 0,
	'w',  'r', 'z', 0x1A, 'i', 'p', 0,    0, 0x14, 0,    0, 0,
	's',  'f', 'h', 0x0F, 'k', '*', '\'', 0, 0x01, 0,    0, 0,
	'x',  'v', 'n', 0x1F, '<', '?', '|',  0, 0x05, 0,    0, 0, };


  public KeyboardMatrixK7669()
  {
    this.matrixNormal = myMatrixNormal;
    this.matrixShift  = myMatrixShift;
    this.ctrlCol      = 7;
    this.ctrlValue    = 0x80;
    this.shiftCol     = 7;
    this.shiftValue   = 0x40;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getKeyboardType()
  {
    return "12x8_K7669";
  }


  @Override
  public synchronized boolean setKeyCode( int keyCode )
  {
    boolean rv = true;
    reset();
    switch( keyCode ) {
      case KeyEvent.VK_ENTER:
	this.keyboardMatrix[ 8 ] = 0x02;
	break;

      case KeyEvent.VK_LEFT:
	this.keyboardMatrix[ 6 ] = 0x02;
	break;

      case KeyEvent.VK_RIGHT:
	this.keyboardMatrix[ 6 ] = 0x20;
	break;

      case KeyEvent.VK_SPACE:
	this.keyboardMatrix[ 8 ] = 0x10;
	break;

      case KeyEvent.VK_UP:
	this.keyboardMatrix[ 6 ] = 0x10;
	break;

      case KeyEvent.VK_DOWN:
	this.keyboardMatrix[ 6 ] = 0x04;
	break;

      case KeyEvent.VK_BACK_SPACE:
	setKeyCharCode( 8, false );
	break;

      case KeyEvent.VK_TAB:
	setKeyCharCode( '\t', false );
	break;

      case KeyEvent.VK_DELETE:
	setKeyCharCode( 0x7F, false );
	break;

      default:
	rv = false;
    }
    updShiftKeysPressed();
    return rv;
  }
}
