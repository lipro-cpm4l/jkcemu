/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des VCS80
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class VCS80 extends EmuSys implements
					Z80AddressListener,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static final int[][] keyMatrix = {
		{ '7', '6', '5', '4', '3', '2', '1', '0' },
		{ 'F', 'E', 'D', 'C', 'B', 'A', '9', '8' },
		{ 'P', 'S', 'T', 'G', 'R', 'M', '-', '+' } };

  private static byte[] mon = null;

  private byte[]  ram;
  private int[]   keyMatrixValues;
  private int[]   digitValues;
  private int     colValue;
  private boolean curDispCycleState;
  private long    curDispTStates;
  private long    dispHCycleTStates;
  private Z80PIO  pio;


  public VCS80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( mon == null ) {
      mon = readResource( "/rom/vcs80/vcs80mon.bin" );
    }
    this.ram = new byte[ 0x0400 ];

    this.curDispCycleState = false;
    this.curDispTStates    = 0;
    this.dispHCycleTStates = 0;
    this.colValue          = 0;
    this.digitValues       = new int[ 9 ];
    this.keyMatrixValues   = new int[ 8 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.pio );
    cpu.addAddressListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged();
  }


  public static int getDefaultSpeedKHz()
  {
    return 2500;
  }


	/* --- Z80AddressListener --- */

  public void z80AddressChanged( int addr )
  {
    // Verbindung A0 - PIO B7 emulieren
    this.pio.putInValuePortB( (addr << 7) & 0x80, 0x80 );
  }


	/* --- Z80MaxSpeedListener --- */

  public void z80MaxSpeedChanged()
  {
    /*
     * Der Takt fuer die Multiplexansteruerung der Anzeige
     * soll 500 bis 1000 Hz betragen.
     * Fuer die Laenge einer Halbschwingung wird hier CPU-Taktfrequenz
     * durch 2 geteilt,
     * was einen Takt fuer die Anzeige von 625 KHz ergibt.
     */
    this.dispHCycleTStates = this.emuThread.getZ80CPU().getMaxSpeedKHz() / 2;
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
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
	synchronized( this.keyMatrixValues ) {
	  int idx = ~this.colValue & 0x07;
	  if( idx < this.keyMatrixValues.length ) {
	    v &= ~this.keyMatrixValues[ idx ];
	  }
	}
	if( this.curDispCycleState ) {
	  v |= 0x80;
	}
	this.pio.putInValuePortA( v, 0xFF );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.removeAddressListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x07E0;
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
    return "/help/vcs80.htm";
  }


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
    return "VCS80";
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    if( keyCode == KeyEvent.VK_ESCAPE ) {
      this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
      rv = true;
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
	int m  = 0x10;
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


  public int readIOByte( int port )
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
	rv = this.pio.readPortB();
	break;

      case 7:
	rv = this.pio.readPortA();
	break;
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert.
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty( props, "jkcemu.system" ).equals( "VCS80" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ram, props );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitValues, 0 );
    }
    synchronized( this.keyMatrixValues ) {
      Arrays.fill( this.keyMatrixValues, 0 );
    }
  }


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


  public void writeIOByte( int port, int value )
  {
    switch( port & 0x07 ) {	// A3 bis A7 ignorieren
      case 4:
	this.pio.writeControlB( value );
	break;

      case 5:
	this.pio.writeControlA( value );
	break;

      case 6:
	this.pio.writePortB( value );
	break;

      case 7:
	this.pio.writePortA( value );
	break;
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
}

