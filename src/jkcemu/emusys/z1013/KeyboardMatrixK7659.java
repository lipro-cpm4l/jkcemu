/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten) der Tastatur K7659,
 * die nach Brosig/Eisenkolb an den Z1013 angeschlossen ist (12x8-Matrix).
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyEvent;
import java.lang.*;


public class KeyboardMatrixK7659 extends KeyboardMatrix12x8
{
  private static int myMatrixNormal[] = {
	'1',  '3', '5', '7', '9',  '~',  '>',  0, 0,    0,    0x06, 0x1D,
	'Q',  'E', 'T', 'U', 'O',  ']',  '\r', 0, 0x7F, 0,    0,    0,
	'A',  'D', 'G', 'J', 'L',  '[',  0x0B, 0, 0,    0,    0,    0,
	'Y',  'C', 'B', 'M', '.',  '<',  0x08, 0, 0,    0x15, 0,    0,
	'2',  '4', '6', '8', '0',  '+',  0x20, 0, 0x1B, 0x19, 0,    0,
	'W',  'R', 'Z', 'I', 'P',  '#',  0,    0, '{',  0x12, 0,    0,
	'S',  'F', 'H', 'K', '\\', '^',  0x0A, 0, '}',  0x10, 0,    0,
	'X',  'V', 'N', ',', '-',  '@',  0x09, 0, 0,    0x03, 0,    0 };


  private static int myMatrixShift[] = {
	'!',  '@', '%', '/', ')',  '?',  0,    0, 0,    0,    0,    0,
	'q',  'e', 't', 'u', 'o',  0,    0,    0, 0,    0,    0,    0,
	'a',  'd', 'g', 'j', 'l',  0,    0,    0, 0,    0,    0,    0,
	'y',  'c', 'b', 'm', ':',  0,    0,    0, 0,    0x0F, 0,    0,
	'\"', '$', '&', '(', '=',  '*',  0,    0, 0,    0x18, 0,    0x19,
	'w',  'r', 'z', 'i', 'p',  '\'', 0,    0, '`',  0x11, 0,    0,
	's',  'f', 'h', 'k', 0,    '|',  0,    0, 0,    0,    0,    0x18,
	'x',  'v', 'n', ';', '_',  0,    0,    0, 0,    0,    0,    0 };


  public KeyboardMatrixK7659()
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
    return "12x8_K7659";
  }


  @Override
  public synchronized boolean setKeyCode( int keyCode )
  {
    boolean rv = true;
    reset();
    switch( keyCode ) {
      case KeyEvent.VK_ENTER:
	this.keyboardMatrix[ 6 ] = 0x02;
	break;

      case KeyEvent.VK_LEFT:
	this.keyboardMatrix[ 6 ] = 0x08;
	break;

      case KeyEvent.VK_RIGHT:
	this.keyboardMatrix[ 6 ] = 0x80;
	break;

      case KeyEvent.VK_SPACE:
	this.keyboardMatrix[ 6 ] = 0x10;
	break;

      case KeyEvent.VK_UP:
	this.keyboardMatrix[ 6 ] = 0x04;
	break;

      case KeyEvent.VK_DOWN:
	this.keyboardMatrix[ 6 ] = 0x40;
	break;

      case KeyEvent.VK_BACK_SPACE:
	setKeyCharCode( 8, false );
	break;

      case KeyEvent.VK_TAB:
	setKeyCharCode( '\t', false );
	break;

      case KeyEvent.VK_ESCAPE:
	this.keyboardMatrix[ 8 ] = 0x10;
	break;

      case KeyEvent.VK_DELETE:
	this.keyboardMatrix[ 8 ] = 0x02;
	break;

      default:
	rv = false;
    }
    updShiftKeysPressed();
    return rv;
  }
}

