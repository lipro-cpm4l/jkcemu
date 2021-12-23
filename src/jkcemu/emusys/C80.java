/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des C80
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.etc.C80KeyboardFld;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80PIO;
import z80emu.Z80PIOPortListener;


public class C80 extends EmuSys implements
					Z80MaxSpeedListener,
					Z80PIOPortListener
{
  public static final String SYSNAME     = "C80";
  public static final String SYSTEXT     = "C-80";
  public static final String PROP_PREFIX = "jkcemu.c80.";

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
  private Z80PIO         pio1;
  private Z80PIO         pio2;


  public C80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
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

    this.pio1.addPIOPortListener( this, Z80PIO.PortInfo.A );
    this.pio1.addPIOPortListener( this, Z80PIO.PortInfo.B );

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


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio1)
	&& ((status == Z80PIO.Status.OUTPUT_AVAILABLE)
	    || (status == Z80PIO.Status.OUTPUT_CHANGED)) )
    {
      if( port == Z80PIO.PortInfo.A ) {
	int v = this.pio1.fetchOutValuePortA( 0xFF );
	if( (v & 0x20) == 0 ) {
	  this.displayReset = true;
	}
	this.tapeOutPhase = ((v & 0x40) != 0);
      }
      else if( port == Z80PIO.PortInfo.B ) {
	int     v       = 0xFF;
	boolean dirty   = false;
	boolean ready   = this.pio1.isReadyPortB();
	this.pio1BValue = this.pio1.fetchOutValuePortB( 0xFF, ready );
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
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canApplySettings( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
  }


  @Override
  public C80KeyboardFld createKeyboardFld()
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

    this.pio1.removePIOPortListener( this, Z80PIO.PortInfo.A );
    this.pio1.removePIOPortListener( this, Z80PIO.PortInfo.B );

    super.die();
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x0FD7;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.BLACK;
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
    return SYSTEXT;
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
	  this.emuThread.fireReset( false );
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
      }
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );
    if( powerOn ) {
      initSRAM( this.ram, props );
    }
    this.pio1.reset( powerOn );
    this.pio2.reset( powerOn );
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    this.pio1.putInValuePortA( 0xFF, false );
    this.displayReset = false;
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
  public boolean supportsKeyboardFld()
  {
    return true;
  }


  @Override
  public boolean supportsTapeIn()
  {
    return true;
  }


  @Override
  public boolean supportsTapeOut()
  {
    return true;
  }


  @Override
  public void tapeInPhaseChanged()
  {
    this.pio1.putInValuePortA( this.tapeInPhase ? 0x80 : 0, 0x80 );
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( (port & 0x40) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.writeDataA( value );
	  break;

	case 1:
	  this.pio1.writeDataB( value );
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
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
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


	/* --- private Methoden --- */

  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}
