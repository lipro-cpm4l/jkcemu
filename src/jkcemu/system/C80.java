/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des C80
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class C80 extends EmuSys implements
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static final int[][] keyMatrix = {
		{ 'R', 'G', 'D', 'A', '7', '4', '1', 0, },
		{ '+', '-', 'E', 'B', '8', '5', '2', '0' },
		{ 0,   0,   'F', 'C', '9', '6', '3', 'M' } };

  private static byte[] mon = null;

  private byte[]  ram;
  private int[]   keyMatrixValues;
  private int[]   digitStatus;
  private int[]   digitValues;
  private int     curDigitIdx;
  private int     pio1BValue;
  private int     a4TStates;
  private long    curDisplayTStates;
  private long    displayCheckTStates;
  private boolean displayReset;
  private boolean audioOutPhase;
  private Z80PIO  pio1;
  private Z80PIO  pio2;


  public C80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( mon == null ) {
      mon = readResource( "/rom/c80/c80mon.bin" );
    }
    this.ram = new byte[ 0x0400 ];

    this.audioOutPhase       = false;
    this.a4TStates           = 0;
    this.curDisplayTStates   = 0;
    this.displayCheckTStates = 0;
    this.curDigitIdx         = -1;
    this.digitStatus         = new int[ 8 ];
    this.digitValues         = new int[ 8 ];
    this.keyMatrixValues     = new int[ 8 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio1  = new Z80PIO( cpu );
    this.pio2  = new Z80PIO( cpu );
    cpu.setInterruptSources( this.pio1, this.pio2 );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged();
  }


  public static int getDefaultSpeedKHz()
  {
    return 455;
  }


	/* --- Z80MaxSpeedListener --- */

  public void z80MaxSpeedChanged()
  {
    this.displayCheckTStates = this.emuThread.getZ80CPU().getMaxSpeedKHz()
								* 50;
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.a4TStates > 0 ) {
      this.a4TStates -= tStates;
      if( this.a4TStates <= 0 ) {
	this.pio1.putInValuePortA( 0x10, 0x10 );	// A4=1
      }
    }
    if( this.displayCheckTStates > 0 ) {
      this.curDisplayTStates += tStates;
      if( this.curDisplayTStates > this.displayCheckTStates ) {
	boolean dirty = false;
	synchronized( this.digitValues ) {
	  for( int i = 0; i < this.digitValues.length; i++ ) {
	    if( this.digitStatus[ i ] > 0 ) {
	      --this.digitStatus[ i ];
	    } else {
	      if( this.digitValues[ i ] != 0 ) {
		this.digitValues[ i ] = 0;
		dirty = true;
	      }
	    }
	  }
	}
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
	this.curDisplayTStates = 0;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x0FD7;
  }


  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
	color = this.colorRedDark;
	break;

      case 2:
	color = this.colorRedLight;
	break;
    }
    return color;
  }


  public int getColorCount()
  {
    return 3;
  }


  public String getHelpPage()
  {
    return "/help/c80.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    int rv = 0xFF;

    addr &= 0xFFFF;
    if( (addr < 0x0800) && (mon != null) ) {
      if( addr < mon.length ) {
	rv = (int) mon[ addr ] & 0xFF;
      }
    } else {
      addr &= 0xFBFF;		// A10 ignorieren
      if( addr >= 0x0800 ) {
	int idx = addr - 0x0800;
	if( idx < this.ram.length ) {
	  rv = (int) this.ram[ idx ] & 0xFF;
	}
      }
    }
    return rv;
  }


  public int getScreenHeight()
  {
    return 85;
  }


  public int getScreenWidth()
  {
    return (this.digitValues.length * 65) - 15;
  }


  public String getTitle()
  {
    return "C-80";
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.keyMatrixValues ) {
      switch( keyCode ) {
	case KeyEvent.VK_ENTER:
	  this.keyMatrixValues[ 0 ] |= 0x02;	// +
	  rv = true;
	  break;

	case KeyEvent.VK_ESCAPE:
	  this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
	  rv = true;
	  break;

	case KeyEvent.VK_N:
	  this.emuThread.getZ80CPU().fireNMI();
	  rv = true;
	  break;

	case KeyEvent.VK_F1:
	  this.keyMatrixValues[ 7 ] |= 0x01;	// FCT
	  rv = true;
	  break;
      }
    }
    return rv;
  }


  public void keyReleased()
  {
    synchronized( this.keyMatrixValues ) {
      Arrays.fill( this.keyMatrixValues, 0 );
    }
  }


  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    if( keyChar > 0 ) {
      synchronized( this.keyMatrixValues ) {
	int ch = Character.toUpperCase( keyChar );
	int m  = 0x01;
	for( int i = 0; !rv && (i < keyMatrix.length); i++ ) {
	  for( int k = 0; !rv && (k < keyMatrix[ i ].length); k++ ) {
	    if( ch == keyMatrix[ i ][ k ] ) {
	      this.keyMatrixValues[ k ] |= m;
	      rv = true;
	    }
	  }
	  m <<= 1;
	}
      }
    }
    return rv;
  }


  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    synchronized( this.digitValues ) {
      for( int i = 0; i < this.digitValues.length; i++ ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitValues[ i ],
		this.colorRedDark,
		this.colorRedLight,
		screenScale );
	x += (65 * screenScale);
      }
    }
    return true;
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0x40) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.putInValuePortA(
		this.emuThread.readAudioPhase() ? 0x80 : 0, 0x80 );
	  rv &= this.pio1.readPortA();
	  break;

	case 1:
	  rv &= this.pio1.readPortB();
	  break;

	case 2:
	  rv &= this.pio1.readControlA();
	  break;

	case 3:
	  rv &= this.pio1.readControlB();
	  break;
      }
    }
    if( (port & 0x80) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.pio2.readPortA();
	  break;

	case 1:
	  rv &= this.pio2.readPortB();
	  break;

	case 2:
	  rv &= this.pio2.readControlA();
	  break;

	case 3:
	  rv &= this.pio2.readControlB();
	  break;
      }
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert.
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty( props, "jkcemu.system" ).equals( "C80" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ram, props );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    synchronized( this.keyMatrixValues ) {
      Arrays.fill( this.keyMatrixValues, 0 );
    }
    this.pio1.putInValuePortA( 0xFF, false );
    this.displayReset = false;
  }


  public boolean setMemByte( int addr, int value )
  {
    boolean rv = false;

    addr &= 0xFBFF;		// A10 ignorieren
    if( addr >= 0x0800 ) {
      int idx = addr - 0x0800;
      if( idx < this.ram.length ) {
	this.ram[ idx ] = (byte) value;
	rv = true;
      }
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    boolean dirty = false;
    boolean ready = false;
    int     v     = 0;
    if( (port & 0x40) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.writePortA( value );
	  v = this.pio1.fetchOutValuePortA( false );
	  if( (v & 0x20) == 0 ) {
	    this.displayReset = true;
	  }
	  this.emuThread.writeAudioPhase( (v & 0x40) != 0 );
	  break;

	case 1:
	  this.pio1.writePortB( value );
	  ready           = this.pio1.isReadyPortB();
	  this.pio1BValue = this.pio1.fetchOutValuePortB( ready );
	  v               = 0xFF;
	  if( ready ) {

	    // Anzeige aktualisieren
	    synchronized( this.digitValues ) {
	      if( displayReset ) {
		this.curDigitIdx = 0;
		displayReset     = false;
	      } else {
		if( (this.curDigitIdx >= 0)
		    && (this.curDigitIdx < (this.digitValues.length - 1)) )
		{
		  this.curDigitIdx++;
		} else {
		  this.curDigitIdx = -1;
		}
	      }
	      if( (this.curDigitIdx >= 0)
		  && (this.curDigitIdx < this.digitValues.length) )
	      {
		this.digitStatus[ this.curDigitIdx ] = 2;
		if( this.pio1BValue != this.digitValues[ this.curDigitIdx ] ) {
		  this.digitValues[ this.curDigitIdx ] = this.pio1BValue;
		  dirty = true;
		}
	      }
	    }

	    // fuer 1 ms A4=0
	    this.a4TStates = this.emuThread.getZ80CPU().getMaxSpeedKHz();
	    v &= 0xEF;
	  }

	  // Tastatur
	  synchronized( this.keyMatrixValues ) {
	    int m = 0x01;
	    for( int i = 0; i < this.keyMatrixValues.length; i++ ) {
	      if( (this.pio1BValue & m) == 0 ) {
		v &= ~this.keyMatrixValues[ i ];
	      }
	      m <<= 1;
	    }
	  }
	  this.pio1.putInValuePortA( v, 0x17 );
	  break;

	case 2:
	  this.pio1.writeControlA( value );
	  break;

	case 3:
	  this.pio1.writeControlB( value );
	  break;
      }
    }
    if( (port & 0x80) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio2.writePortA( value );
	  break;

	case 1:
	  this.pio2.writePortB( value );
	  break;

	case 2:
	  this.pio2.writeControlA( value );
	  break;

	case 3:
	  this.pio2.writeControlB( value );
	  break;
      }
    }
    if( dirty ) {
      this.screenFrm.setScreenDirty( true );
    }
  }
}

