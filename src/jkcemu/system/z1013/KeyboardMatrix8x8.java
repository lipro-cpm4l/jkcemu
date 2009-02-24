/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z1013-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten)
 * der Alpha-Tastatur (8x8-Matrix).
 */

package jkcemu.system.z1013;

import java.awt.event.KeyEvent;
import java.lang.*;


public class KeyboardMatrix8x8 extends KeyboardMatrix
{
  private static String matrixNormal =
	"13579-"
	+ "QETUO@"
	+ "ADGJL*"
	+ "YCBM.^"
	+ "24680["
	+ "WRZIP]"
	+ "SFHK+\\"
	+ "XVN,/_";

  private static String matrixShift =
	"!#%\')="
	+ "qetuo`"
	+ "adgjl:"
	+ "ycbm>~"
	+ "\"$&( {"
	+ "wrzip}"
	+ "sfhk;|"
	+ "xvn<?\u007F";


  private int[] rowMasks;


  public KeyboardMatrix8x8()
  {
    this.rowMasks = new int[ 8 ];
    reset();
  }


  public String getKeyboardType()
  {
    return "8x8";
  }


  public int getRowValues( int col, boolean pioB4State )
  {
    int colMask	= (1 << col);

    int r0 = 0;
    int r1 = 0;
    int r2 = 0;
    int r3 = 0;

    if( onlyShiftKeysReadable() ) {
      if( pioB4State ) {
	r1 = ((this.rowMasks[ 5 ] & colMask & 0x40) != 0 ? 2 : 0);  // Control
	r2 = ((this.rowMasks[ 6 ] & colMask & 0x80) != 0 ? 4 : 0);  // Shift
      }
    } else {
      if( pioB4State ) {
	r0 = ((this.rowMasks[ 4 ] & colMask) != 0 ? 1 : 0);
	r1 = ((this.rowMasks[ 5 ] & colMask) != 0 ? 2 : 0);
	r2 = ((this.rowMasks[ 6 ] & colMask) != 0 ? 4 : 0);
	r3 = ((this.rowMasks[ 7 ] & colMask) != 0 ? 8 : 0);
      } else {
	r0 = ((this.rowMasks[ 0 ] & colMask) != 0 ? 1 : 0);
	r1 = ((this.rowMasks[ 1 ] & colMask) != 0 ? 2 : 0);
	r2 = ((this.rowMasks[ 2 ] & colMask) != 0 ? 4 : 0);
	r3 = ((this.rowMasks[ 3 ] & colMask) != 0 ? 8 : 0);
      }
    }
    return r0 | r1 | r2 | r3;
  }


  public void reset()
  {
    super.reset();
    for( int i = 0; i < this.rowMasks.length; i++ )
      this.rowMasks[ i ] = 0;
  }


  public synchronized boolean setKeyCode( int keyCode )
  {
    boolean rv = true;
    reset();
    switch( keyCode ) {
      case KeyEvent.VK_ENTER:
	this.keyCharCode   = '\r';
	this.rowMasks[ 1 ] = 0x40;		// Taste "Enter"
	break;

      case KeyEvent.VK_LEFT:
	this.keyCharCode   = 8;
	this.rowMasks[ 2 ] = 0x40;		// Taste "Links"
	break;

      case KeyEvent.VK_RIGHT:
	this.keyCharCode   = '\t';
	this.rowMasks[ 3 ] = 0x40;		// Taste "Rechts"
	break;

      case KeyEvent.VK_SPACE:
	this.keyCharCode   = '\u0020';
	this.rowMasks[ 4 ] = 0x40;		// Taste "Space"
	break;

      case KeyEvent.VK_UP:
	this.keyCharCode   = 11;
	this.rowMasks[ 6 ] = 0x40;		// Taste "Hoch"
	break;

      case KeyEvent.VK_DOWN:
	this.keyCharCode   = 10;
	this.rowMasks[ 7 ] = 0x40;		// Taste "Runter"
	break;

      case KeyEvent.VK_BACK_SPACE:
	setKeyCharCode( 8 );
	break;

      case KeyEvent.VK_TAB:
	setKeyCharCode( '\t' );
	break;

      case KeyEvent.VK_DELETE:
	setKeyCharCode( 0x7F );
	break;

      default:
	rv = false;
    }
    updShiftKeysPressed();
    return rv;
  }


  protected boolean updRowMasks()
  {
    boolean rv          = false;
    boolean ctrl        = false;
    int     keyCharCode = this.keyCharCode;
    if( (keyCharCode > 0) && (keyCharCode < '\u0020') ) {
      keyCharCode += '@';
      ctrl        = true;
    }

    int pos = this.matrixNormal.indexOf( keyCharCode );
    if( pos >= 0 ) {
      int m = (1 << (pos % 6));
      this.rowMasks[ pos / 6 ] |= m;
      rv = true;
    } else {
      pos = this.matrixShift.indexOf( keyCharCode );
      if( pos >= 0 ) {
	int m = (1 << (pos % 6));
	this.rowMasks[ pos / 6 ] |= m;
	this.rowMasks[ 6 ] |= 0x80;		// Shift-Taste
	rv = true;
      }
    }
    if( rv ) {
      if( ctrl ) {
	this.rowMasks[ 5 ] |= 0x40;		// Control-Taste
      }
      updShiftKeysPressed();
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void updShiftKeysPressed()
  {
    setShiftKeysPressed(
	((this.rowMasks[ 5 ] & 0x40) != 0)
	|| ((this.rowMasks[ 6 ] & 0x80) != 0) );
  }
}

