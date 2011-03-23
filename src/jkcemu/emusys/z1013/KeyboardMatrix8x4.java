/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten)
 * der Z1013-Folientastatur (8x4-Matrix).
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyEvent;
import java.lang.*;


public class KeyboardMatrix8x4 extends KeyboardMatrix
{
  private int rowMask0;
  private int rowMask1;
  private int rowMask2;
  private int rowMask3;


  public KeyboardMatrix8x4()
  {
    reset();
  }


  /*
   * Die Methode besagt, ob eine Cursor-, Leer- oder Enter-Taste
   * gedrueckt ist
   */
  public boolean isAnyControlKeyPressed()
  {
    return (this.rowMask3 & 0xF0) != 0;
  }


  public void setRowMasks( int rm0, int rm1, int rm2, int rm3 )
  {
    this.rowMask0    = rm0;
    this.rowMask1    = rm1;
    this.rowMask2    = rm2;
    this.rowMask3    = rm3;
    this.keyCharCode = '\0';

    if( (this.rowMask3 & 0x10) != 0 ) {
      this.keyCharCode = '\u0008';
    }
    else if( (this.rowMask3 & 0x20) != 0 ) {
      this.keyCharCode = '\u0020';
    }
    else if( (this.rowMask3 & 0x40) != 0 ) {
      this.keyCharCode = '\t';
    }
    else if( (this.rowMask3 & 0x80) != 0 ) {
      this.keyCharCode = '\r';
    } else {
      int shiftNum = 0;
      for( int i = 0; i < 4; i++ ) {
	if( (this.rowMask3 & (1 << i)) != 0 ) {
	  shiftNum = i + 1;
	  break;
	}
      }
      Character ch = computeKeyChar( this.rowMask0, shiftNum,
				'@', 'X', 'x', '\u0060', '\u0010' );
      if( ch == null ) {
	ch = computeKeyChar( this.rowMask1, shiftNum,
				'H', '0', '\u0020', 'h', '\u0000' );
      }
      if( ch == null ) {
	ch = computeKeyChar( this.rowMask2, shiftNum,
				'P', '8', '(', 'p', '\u0008' );
      }
      if( ch != null ) {
	this.keyCharCode = ch.charValue();
      }
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
    int rv	= 0;
    int colMask	= (1 << col);

    if( onlyShiftKeysReadable() ) {
      rv = ((this.rowMask3 & colMask & 0x0F) != 0 ? 8 : 0);
    } else {
      int r0 = ((this.rowMask0 & colMask) != 0 ? 1 : 0);
      int r1 = ((this.rowMask1 & colMask) != 0 ? 2 : 0);
      int r2 = ((this.rowMask2 & colMask) != 0 ? 4 : 0);
      int r3 = ((this.rowMask3 & colMask) != 0 ? 8 : 0);
      rv = r0 | r1 | r2 | r3;
    }
    return rv;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.rowMask0 = 0;
    this.rowMask1 = 0;
    this.rowMask2 = 0;
    this.rowMask3 = 0;
  }


  @Override
  public synchronized boolean setKeyCode( int keyCode )
  {
    boolean rv = true;
    reset();
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
	this.keyCharCode = 8;
	this.rowMask3    = 0x10;	// Taste "Links"
	break;

      case KeyEvent.VK_RIGHT:
	this.keyCharCode = '\t';
	this.rowMask3    = 0x40;	// Taste "Rechts"
	break;

      case KeyEvent.VK_ENTER:
	this.keyCharCode = '\r';
	this.rowMask3    = 0x80;	// Taste "Enter"
	break;

      case KeyEvent.VK_SPACE:
	this.keyCharCode = '\u0020';
	this.rowMask3    = 0x20;	// Taste "Space"
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
	this.rowMask3 = 0x01;		// Taste "S1"
	break;

      case KeyEvent.VK_F2:
	this.rowMask3 = 0x02;		// Taste "S2"
	break;

      case KeyEvent.VK_F3:
	this.rowMask3 = 0x04;		// Taste "S3"
	break;

      case KeyEvent.VK_F4:
	this.rowMask3 = 0x08;		// Taste "S4"
	break;
      default:
	rv = false;
    }
    updShiftKeysPressed();
    return rv;
  }


  /*
   * Diese Methode bildet den in "this.keyCharCode" enthaltenen Tastencode
   * auf der Tastaturmatrix ab.
   * Die Codes 08h, 09h, 0Dh und 20h werden nicht in die Sondertasten,
   * sondern in Tastenkombinationen mit S4 umgesetzt,
   * da sie ja auch auf der Emulator-Tastatur mit Control erzeugt wurden.
   * Enter, Leer- und Cursor-Tasten werden separat behandelt.
   */
  @Override
  protected synchronized boolean updRowMasks( boolean hexMode )
  {
    boolean rv = false;
    if( this.keyCharCode > 0 ) {

      // einzelne 8er-Gruppen pruefen
      int rowMask = (1 << (this.keyCharCode & 0x07));

      rv = true;
      switch( this.keyCharCode & 0xF8 ) {

	// standardmaessig ohne Shift
	case '@':
	  this.rowMask0 = rowMask;
	  break;
	case 'H':
	  this.rowMask1 = rowMask;
	  if( hexMode ) {
	    this.rowMask3 = 1;
	  }
	  break;
	case 'P':
	  this.rowMask2 = rowMask;
	  if( hexMode ) {
	    this.rowMask3 = 1;
	  }
	  break;

	// standardmaessig mit Shift 1
	case 'X':
	  this.rowMask0 = rowMask;
	  this.rowMask3 = 1;
	  break;
	case '0':
	  this.rowMask1 = rowMask;
	  if( !hexMode ) {
	    this.rowMask3 = 1;
	  }
	  break;
	case '8':
	  this.rowMask2 = rowMask;
	  if( !hexMode ) {
	    this.rowMask3 = 1;
	  }
	  break;

	// Shift 2
	case 'x':
	  this.rowMask0 = rowMask;
	  this.rowMask3 = 2;
	  break;
	case '\u0020':
	  this.rowMask1 = rowMask;
	  this.rowMask3 = 2;
	  break;
	case '(':
	  this.rowMask2 = rowMask;
	  this.rowMask3 = 2;
	  break;

	// Shift 3
	case '\u0060':
	  this.rowMask0 = rowMask;
	  this.rowMask3 = 4;
	  break;
	case 'h':
	  this.rowMask1 = rowMask;
	  this.rowMask3 = 4;
	  break;
	case 'p':
	  this.rowMask2 = rowMask;
	  this.rowMask3 = 4;
	  break;

	// Shift 4
	case '\u0010':
	  this.rowMask0 = rowMask;
	  this.rowMask3 = 8;
	  break;
	case '\u0000':
	  this.rowMask1 = rowMask;
	  this.rowMask3 = 8;
	  break;
	case '\u0008':
	  this.rowMask2 = rowMask;
	  this.rowMask3 = 8;
	  break;

	// Code auf Tastaturmatrix nicht abbildbar
	default:
	  rv = false;
      }
    }
    if( rv ) {
      updShiftKeysPressed();
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static Character computeKeyChar(
				int  rowMask,
				int  shiftNum,
				int  ch0,
				int  ch1,
				int  ch2,
				int  ch3,
				int  ch4 )
  {
    int colNum = 0;
    for( int i = 0; i < 8; i++ ) {
      if( (rowMask & 0x01) != 0 ) {
	break;
      }
      rowMask >>= 1;
      colNum++;
    }
    if( colNum < 8 ) {
      switch( shiftNum ) {
	case 1:
	  return new Character( (char) (ch1 + colNum) );

	case 2:
	  return new Character( (char) (ch2 + colNum) );

	case 3:
	  return new Character( (char) (ch3 + colNum) );

	case 4:
	  return new Character( (char) (ch4 + colNum) );

	default:
	  return new Character( (char) (ch0 + colNum) );
      }
    }
    return null;
  }


  private void updShiftKeysPressed()
  {
    setShiftKeysPressed( (this.rowMask3 & 0x0F) != 0 );
  }
}

