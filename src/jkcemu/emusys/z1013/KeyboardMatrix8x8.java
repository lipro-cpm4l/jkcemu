/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beschreibung eines Zustandes (gedrueckte Tasten)
 * der Z1013-Alphatastatur (8x8-Matrix).
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;


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


  private int[] keyboardMatrix;


  public KeyboardMatrix8x8()
  {
    this.keyboardMatrix = new int[ 8 ];
    reset();
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
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getKeyboardType()
  {
    return "8x8";
  }


  @Override
  public int getRowValues( int col, boolean pioB4State )
  {
    int rv = 0;
    if( col >= 0 ) {
      if( onlyShiftKeysReadable() ) {
        if( col == 6 ) {
          rv = this.keyboardMatrix[ col ] & 0x20;	// Control
        } else if( col == 7 ) {
          rv = this.keyboardMatrix[ col ] & 0x40;	// Shift
        }
      } else {
        if( col < this.keyboardMatrix.length ) {
          rv = this.keyboardMatrix[ col ];
	  if( pioB4State ) {
	    rv >>= 4;
	  }
        }
      }
    }
    return rv & 0x0F;
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
      case KeyEvent.VK_F1:				// Graph E/A
	this.keyboardMatrix[ 6 ] = 0x01;
	break;

      case KeyEvent.VK_ENTER:
	this.keyboardMatrix[ 6 ] = 0x02;
	break;

      case KeyEvent.VK_LEFT:
	this.keyboardMatrix[ 6 ] = 0x04;
	break;

      case KeyEvent.VK_RIGHT:
	this.keyboardMatrix[ 6 ] = 0x08;
	break;

      case KeyEvent.VK_SPACE:
	this.keyboardMatrix[ 6 ] = 0x10;
	break;

      case KeyEvent.VK_UP:
	this.keyboardMatrix[ 6 ] = 0x40;
	break;

      case KeyEvent.VK_DOWN:
	this.keyboardMatrix[ 6 ] = 0x80;
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


  @Override
  public void updKeyboardFld( AbstractKeyboardFld keyboardFld )
  {
    if( keyboardFld != null )
      keyboardFld.updKeySelection( this.keyboardMatrix );
  }


  @Override
  protected boolean updKeyboardMatrix(
				int     keyCharCode,
				boolean hexMode )
  {
    boolean rv   = false;
    boolean ctrl = false;
    if( (keyCharCode > 0) && (keyCharCode < '\u0020') ) {
      keyCharCode += '@';
      ctrl        = true;
    }

    int pos = this.matrixNormal.indexOf( keyCharCode );
    if( pos >= 0 ) {
      int m = (1 << (pos / 6));
      this.keyboardMatrix[ pos % 6 ] |= m;
      rv = true;
    } else {
      pos = this.matrixShift.indexOf( keyCharCode );
      if( pos >= 0 ) {
	int m = (1 << (pos / 6));
	this.keyboardMatrix[ pos % 6 ] |= m;
	this.keyboardMatrix[ 7 ] |= 0x40;		// Shift
	rv = true;
      }
    }
    if( rv ) {
      if( ctrl ) {
	this.keyboardMatrix[ 6 ] |= 0x20;		// Control
      }
      updShiftKeysPressed();
    }
    if( keyCharCode == 0xF1 ) {				// Graph E/A
      this.keyboardMatrix[ 6 ] = 0x01;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void updShiftKeysPressed()
  {
    setShiftKeysPressed(
	((this.keyboardMatrix[ 6 ] & 0x20) != 0)
	|| ((this.keyboardMatrix[ 7 ] & 0x40) != 0) );
  }
}
