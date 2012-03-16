/*
 * (c) 2008-2011 Jens Mueller
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


public class Z1013Keyboard
{
  private Z80PIO                  z80pio;
  private volatile KeyboardMatrix keyboardMatrix;
  private volatile boolean        fontAltEnabled;
  private volatile int            selectedCol;


  public Z1013Keyboard( Z80PIO z80pio )
  {
    this.z80pio         = z80pio;
    this.keyboardMatrix = null;
    this.fontAltEnabled = false;
    this.selectedCol    = 0;
    applySettings( Main.getProperties() );
    if( this.keyboardMatrix == null ) {
      this.keyboardMatrix = new KeyboardMatrix8x4();
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
	else if( monType.endsWith( "_K7669" ) ) {
	  kbMatrix = new KeyboardMatrixK7669();
	}
	else if( monType.endsWith( "_S6009" ) ) {
	  kbMatrix = new KeyboardMatrixS6009();
	}
      }
      if( kbMatrix == null ) {
	kbMatrix = new KeyboardMatrix8x4();
      }
      this.keyboardMatrix = kbMatrix;
    }
  }


  public KeyboardMatrix getKeyboardMatrix()
  {
    return this.keyboardMatrix;
  }


  public String getKeyboardType()
  {
    return this.keyboardMatrix.getKeyboardType();
  }


  public void putRowValuesToPIO()
  {
    this.z80pio.putInValuePortB(
	~this.keyboardMatrix.getRowValues(
		this.selectedCol,
		(this.z80pio.fetchOutValuePortB( false ) & 0x10) != 0 ),
	0x1F );
  }


  public void reset()
  {
    this.keyboardMatrix.reset();
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
      if( this.keyboardMatrix.setKeyCharCode( ch, hexMode ) ) {
	putRowValuesToPIO();
	rv = true;
      }
    }
    return rv;
  }


  public boolean setKeyCode( int keyCode )
  {
    boolean rv = this.keyboardMatrix.setKeyCode( keyCode );
    putRowValuesToPIO();
    return rv;
  }


  public void setKeyReleased()
  {
    this.keyboardMatrix.setKeyReleased();
    putRowValuesToPIO();
  }


  public synchronized void setSelectedCol( int col )
  {
    this.selectedCol = col;
    putRowValuesToPIO();
  }
}

