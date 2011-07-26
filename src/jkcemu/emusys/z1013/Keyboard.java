/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Z1013-Tastatur
 */

package jkcemu.emusys.z1013;

import java.lang.*;
import java.util.Properties;
import jkcemu.Main;
import z80emu.Z80PIO;


public class Keyboard
{
  private Z80PIO                  z80pio;
  private volatile KeyboardMatrix curKeyboardMatrix;
  private volatile boolean        fontAltEnabled;
  private volatile int            selectedCol;


  public Keyboard( Z80PIO z80pio )
  {
    this.z80pio            = z80pio;
    this.curKeyboardMatrix = null;
    this.fontAltEnabled    = false;
    this.selectedCol       = 0;
    applySettings( Main.getProperties() );
    if( this.curKeyboardMatrix == null ) {
      this.curKeyboardMatrix = new KeyboardMatrix8x4();
    }
  }


  public void applySettings( Properties props )
  {
    if( props != null ) {
      KeyboardMatrix kbMatrix = null;
      String         monType  = props.getProperty( "jkcemu.z1013.monitor" );
      if( monType != null ) {
	if( monType.equals( "A.2" ) ) {
	  kbMatrix = new KeyboardMatrix8x8();
	}
	else if( monType.endsWith( "_K7659" ) ) {
	  kbMatrix = new KeyboardMatrixK7659();
	}
	else if( monType.endsWith( "_S6009" ) ) {
	  kbMatrix = new KeyboardMatrixS6009();
	}
      }
      if( kbMatrix == null ) {
	kbMatrix = new KeyboardMatrix8x4();
      }
      this.curKeyboardMatrix = kbMatrix;
    }
  }


  public KeyboardMatrix getCurKeyboardMatrix()
  {
    return this.curKeyboardMatrix;
  }


  public int getKeyCharCode()
  {
    return this.curKeyboardMatrix.getKeyCharCode();
  }


  public String getKeyboardType()
  {
    return this.curKeyboardMatrix.getKeyboardType();
  }


  public void putRowValuesToPIO()
  {
    int mask = this.curKeyboardMatrix.getRowValues(
		this.selectedCol,
		(this.z80pio.fetchOutValuePortB( false ) & 0x10) != 0 );
    this.z80pio.putInValuePortB( ~mask, 0x1F );		// L-aktiv
  }


  public void reset()
  {
    this.curKeyboardMatrix.reset();
  }


  public boolean setKeyChar( char keyChar, boolean hexMode )
  {
    boolean rv = false;
    int     ch = 0;
    if( keyChar == '\n' ) {
      ch = '\r';
    } else {
      ch = keyChar;
    }
    if( (ch > 0) && (ch <= 0xFF) ) {
      if( this.curKeyboardMatrix.setKeyCharCode( ch, hexMode ) ) {
	putRowValuesToPIO();
	rv = true;
      }
    }
    return rv;
  }


  public boolean setKeyCode( int keyCode )
  {
    boolean rv = this.curKeyboardMatrix.setKeyCode( keyCode );
    putRowValuesToPIO();
    return rv;
  }


  public void setKeyReleased()
  {
    this.curKeyboardMatrix.setKeyReleased();
    putRowValuesToPIO();
  }


  public synchronized void setSelectedCol( int col )
  {
    this.selectedCol = col;
    putRowValuesToPIO();
  }
}

