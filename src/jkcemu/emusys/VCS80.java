/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des VCS80
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.etc.VCS80KeyboardFld;
import z80emu.Z80AddressListener;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80PIO;


public class VCS80 extends EmuSys implements Z80AddressListener
{
  public static final String SYSNAME     = "VCS80";
  public static final String PROP_PREFIX = "jkcemu.vcs80.";

  private static final int[][] kbMatrix = {
		{ '7', '6', '5', '4', '3', '2', '1', '0' },
		{ 'F', 'E', 'D', 'C', 'B', 'A', '9', '8' },
		{ 'P', 'S', 'T', 'G', 'R', 'M', '-', '+' } };

  private static byte[] mon = null;

  private VCS80KeyboardFld keyboardFld;
  private int[]            keyboardMatrix;
  private int[]            digitValues;
  private byte[]           ram;
  private int              colValue;
  private boolean          curDispCycleState;
  private long             curDispTStates;
  private long             dispHCycleTStates;
  private Z80PIO           pio;


  public VCS80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    if( mon == null ) {
      mon = readResource( "/rom/vcs80/vcs80mon.bin" );
    }
    this.ram = new byte[ 0x0400 ];

    this.curDispCycleState = false;
    this.curDispTStates    = 0;
    this.dispHCycleTStates = 0;
    this.colValue          = 0;
    this.keyboardFld       = null;
    this.keyboardMatrix    = new int[ 8 ];
    this.digitValues       = new int[ 9 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio   = new Z80PIO( "PIO" );
    cpu.setInterruptSources( this.pio );
    cpu.addAddressListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    z80MaxSpeedChanged( cpu );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
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


	/* --- Z80AddressListener --- */

  @Override
  public void z80AddressChanged( int addr )
  {
    // Verbindung A0 - PIO B7 emulieren
    this.pio.putInValuePortB( (addr << 7) & 0x80, 0x80 );
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
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new VCS80KeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.removeAddressListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x07E0;
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
    return "/help/vcs80.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    int rv = 0xFF;

    addr &= 0xFFFF;
    if( (addr < 0x0400) && (mon != null) ) {
      addr &= 0x01FF;				// bei ROM A9 ignorieren
      if( addr < mon.length ) {
	rv = (int) mon[ addr ] & 0xFF;
      }
    } else {
      int idx = addr - 0x0400;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	rv = (int) this.ram[ idx ] & 0xFF;
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
    return SYSNAME;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    if( keyCode == KeyEvent.VK_ESCAPE ) {
      this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
      rv = true;
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
	int m  = 0x10;
	for( int i = 0; !rv && (i < kbMatrix.length); i++ ) {
	  for( int k = 0; !rv && (k < kbMatrix[ i ].length); k++ ) {
	    if( ch == kbMatrix[ i ][ k ] ) {
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
      for( int i = this.digitValues.length - 1; i >= 0; --i ) {
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

    switch( port & 0x07 ) {	// A3 bis A7 ignorieren
      case 4:
	rv = this.pio.readControlB();
	break;

      case 5:
	rv = this.pio.readControlA();
	break;

      case 6:
	rv = this.pio.readDataB();
	break;

      case 7:
	rv = this.pio.readDataA();
	break;
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ram, props );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitValues, 0 );
    }
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    boolean rv = false;

    addr &= 0xFFFF;
    if( addr >= 0x0400 ) {
      int idx = addr - 0x0400;
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
  public void writeIOByte( int port, int value, int tStates )
  {
    switch( port & 0x07 ) {	// A3 bis A7 ignorieren
      case 4:
	this.pio.writeControlB( value );
	break;

      case 5:
	this.pio.writeControlA( value );
	break;

      case 6:
	this.pio.writeDataB( value );
	break;

      case 7:
	this.pio.writeDataA( value );
	break;
    }
  }


  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );

    /*
     * Der Takt fuer die Multiplexansteruerung der Anzeige
     * soll 500 bis 1000 Hz betragen.
     * Fuer die Laenge einer Halbschwingung wird hier CPU-Taktfrequenz
     * durch 2 geteilt,
     * was einen Takt fuer die Anzeige von 625 KHz ergibt.
     */
    this.dispHCycleTStates = cpu.getMaxSpeedKHz() / 2;
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );

    long hCycleTStates = this.dispHCycleTStates;
    if( hCycleTStates > 0 ) {
      this.curDispTStates += tStates;
      if( this.curDispTStates > hCycleTStates ) {
	this.curDispTStates    = 0;
	this.curDispCycleState = !this.curDispCycleState;
	if( this.curDispCycleState ) {

	  // alte Spalte anzeigen
	  boolean dirty = false;
	  int     value = toDigitValue(
				this.pio.fetchOutValuePortB( false ) & 0x7F );
	  synchronized( this.digitValues ) {
	    int idx = this.colValue;
	    if( idx >= 4 ) {
	      idx++;
	    }
	    if( idx < this.digitValues.length ) {
	      if( value != this.digitValues[ idx ] ) {
		this.digitValues[ idx ] = value;
		dirty = true;
	      }
	    }
	  }
	  if ( dirty ) {
	    this.screenFrm.setScreenDirty( true );
	  }

	  // Spaltenzaehler inkrementieren
	  this.colValue = (this.colValue + 1) & 0x07;
	}

	// Port-A-Eingaenge aktualisieren
	int v = 0x70 | this.colValue;
	synchronized( this.keyboardMatrix ) {
	  int idx = ~this.colValue & 0x07;
	  if( idx < this.keyboardMatrix.length ) {
	    v &= ~this.keyboardMatrix[ idx ];
	  }
	}
	if( this.curDispCycleState ) {
	  v |= 0x80;
	}
	this.pio.putInValuePortA( v, 0xFF );
      }
    }
  }


	/* --- private Methoden --- */

  /*
   * Eingang: H-Aktiv
   *   Bit: 0 -> A
   *   Bit: 1 -> B
   *   Bit: 2 -> C
   *   Bit: 3 -> D
   *   Bit: 4 -> E
   *   Bit: 5 -> G
   *   Bit: 6 -> F
   *   Bit: 7 -> nicht verwendet
   *
   * Ausgang: H-Aktiv
   *   Bit: 0 -> A
   *   Bit: 1 -> B
   *   Bit: 2 -> C
   *   Bit: 3 -> D
   *   Bit: 4 -> E
   *   Bit: 5 -> F
   *   Bit: 6 -> G
   *   Bit: 7 -> P
   */
  private int toDigitValue( int value )
  {
    int rv = value & 0x1F;
    if( (value & 0x20) != 0 ) {
      rv |= 0x40;
    }
    if( (value & 0x40) != 0 ) {
      rv |= 0x20;
    }
    return rv;
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}
