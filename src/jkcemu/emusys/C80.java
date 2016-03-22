/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des C80
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.emusys.etc.C80KeyboardFld;
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

  private C80KeyboardFld keyboardFld;
  private int[]          keyboardMatrix;
  private int[]          digitStatus;
  private int[]          digitValues;
  private byte[]         ram;
  private int            curDigitIdx;
  private int            pio1BValue;
  private int            a4TStates;
  private long           curDisplayTStates;
  private long           displayCheckTStates;
  private boolean        displayReset;
  private boolean        audioInPhase;
  private Z80PIO         pio1;
  private Z80PIO         pio2;


  public C80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, "jkcemu.c80." );
    if( mon == null ) {
      mon = readResource( "/rom/c80/c80mon.bin" );
    }
    this.ram = new byte[ 0x0400 ];

    this.a4TStates           = 0;
    this.curDisplayTStates   = 0;
    this.displayCheckTStates = 0;
    this.curDigitIdx         = -1;
    this.digitStatus         = new int[ 8 ];
    this.digitValues         = new int[ 8 ];
    this.keyboardMatrix      = new int[ 8 ];
    this.keyboardFld         = null;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio1  = new Z80PIO( "PIO 1" );
    this.pio2  = new Z80PIO( "PIO 2" );
    cpu.setInterruptSources( this.pio1, this.pio2 );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    z80MaxSpeedChanged( cpu );
  }


  public static int getDefaultSpeedKHz()
  {
    return 455;
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    synchronized( this.keyboardMatrix ) {
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
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.displayCheckTStates = cpu.getMaxSpeedKHz() * 50;
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    boolean phase = this.emuThread.readAudioPhase();
    if( phase != this.audioInPhase ) {
      this.audioInPhase = phase;
      this.pio1.putInValuePortA( this.audioInPhase ? 0x80 : 0, 0x80 );
    }
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

  @Override
  public boolean canApplySettings( Properties props )
  {
    return EmuUtil.getProperty( props, "jkcemu.system" ).equals( "C80" );
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new C80KeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x0FD7;
  }


  @Override
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


  @Override
  public int getColorCount()
  {
    return 3;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/c80.htm";
  }


  @Override
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


  @Override
  public int getScreenHeight()
  {
    return 85;
  }


  @Override
  public int getScreenWidth()
  {
    return (this.digitValues.length * 65) - 15;
  }


  @Override
  public String getTitle()
  {
    return "C-80";
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.keyboardMatrix ) {
      switch( keyCode ) {
	case KeyEvent.VK_ENTER:
	  this.keyboardMatrix[ 0 ] |= 0x02;	// +
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
	  this.keyboardMatrix[ 7 ] |= 0x01;	// FCT
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    if( keyChar > 0 ) {
      synchronized( this.keyboardMatrix ) {
	int ch = Character.toUpperCase( keyChar );
	int m  = 0x01;
	for( int i = 0; !rv && (i < keyMatrix.length); i++ ) {
	  for( int k = 0; !rv && (k < keyMatrix[ i ].length); k++ ) {
	    if( ch == keyMatrix[ i ][ k ] ) {
	      this.keyboardMatrix[ k ] |= m;
	      rv = true;
	    }
	  }
	  m <<= 1;
	}
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
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


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( (port & 0x40) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.pio1.readDataA();
	  break;

	case 1:
	  rv &= this.pio1.readDataB();
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
	  rv &= this.pio2.readDataA();
	  break;

	case 1:
	  rv &= this.pio2.readDataB();
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


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ram, props );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    this.pio1.putInValuePortA( 0xFF, false );
    this.displayReset = false;
    this.audioInPhase = this.emuThread.readAudioPhase();
  }


  @Override
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


  @Override
  public boolean supportsAudio()
  {
    return true;
  }


  @Override
  public boolean supportsKeyboardFld()
  {
    return true;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    boolean dirty = false;
    boolean ready = false;
    int     v     = 0;
    if( (port & 0x40) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.writeDataA( value );
	  v = this.pio1.fetchOutValuePortA( false );
	  if( (v & 0x20) == 0 ) {
	    this.displayReset = true;
	  }
	  this.emuThread.writeAudioPhase( (v & 0x40) != 0 );
	  break;

	case 1:
	  this.pio1.writeDataB( value );
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
	  synchronized( this.keyboardMatrix ) {
	    int m = 0x01;
	    for( int i = 0; i < this.keyboardMatrix.length; i++ ) {
	      if( (this.pio1BValue & m) == 0 ) {
		v &= ~this.keyboardMatrix[ i ];
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
	  this.pio2.writeDataA( value );
	  break;

	case 1:
	  this.pio2.writeDataB( value );
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


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}
