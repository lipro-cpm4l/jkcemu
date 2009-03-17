/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LC80
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class LC80 extends EmuSys implements
					Z80HaltStateListener,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static final int[][] kbMatrix = {
				{ 0,   '3', '7', 'B', 'F', '-' },
				{ 'L', '2', '+', 'E', 'A', '6' },
				{ 'S', '1', '5', '9', 'D', 0   },
				{ 'X', '0', '4', '8', 'C', 0   } };

  private static byte[] lc80_u505  = null;
  private static byte[] lc80_2716  = null;
  private static byte[] lc80_2     = null;
  private static byte[] lc80e_0000 = null;
  private static byte[] lc80e_c000 = null;

  private byte[]           rom0000;
  private byte[]           romC000;
  private byte[]           ram;
  private int[]            kbMatrixValues;
  private int[]            segStatus;
  private int[]            segValues;
  private volatile int     curSegValue;
  private volatile int     pio1BValue;
  private long             curDisplayTStates;
  private long             displayCheckTStates;
  private boolean          audioOutLED;
  private volatile boolean audioOutPhase;
  private volatile boolean audioOutState;
  private boolean          haltState;
  private Z80CTC           ctc;
  private Z80PIO           pio1;
  private Z80PIO           pio2;
  private String           sysName;
  private Color            redLight;
  private Color            redDark;
  private Color            greenLight;
  private Color            greenDark;


  public LC80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    createColors( props );
    this.sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( this.sysName.equals( "LC80_U505" ) ) {
      if( lc80_u505 == null ) {
	lc80_u505 = readResource( "/rom/lc80/lc80_u505.bin" );
      }
      this.rom0000 = lc80_u505;
      this.romC000 = null;
      this.ram     = new byte[ 0x0400 ];
    } else if( this.sysName.equals( "LC80.2" ) ) {
      if( lc80_2 == null ) {
	lc80_2 = readResource( "/rom/lc80/lc80_2.bin" );
      }
      this.rom0000 = lc80_2;
      this.romC000 = null;
      this.ram     = new byte[ 0x1000 ];
    } else if( this.sysName.equals( "LC80e" ) ) {
      if( lc80e_0000 == null ) {
	lc80e_0000 = readResource( "/rom/lc80/lc80e_0000.bin" );
      }
      if( lc80e_c000 == null ) {
	lc80e_c000 = readResource( "/rom/lc80/lc80e_c000.bin" );
      }
      this.rom0000 = lc80e_0000;
      this.romC000 = lc80e_c000;
      this.ram     = new byte[ 0x1000 ];
    } else {
      if( lc80_2716 == null ) {
	lc80_2716 = readResource( "/rom/lc80/lc80_2716.bin" );
      }
      this.rom0000 = lc80_2716;
      this.romC000 = null;
      this.ram     = new byte[ 0x1000 ];
    }

    this.audioOutLED         = false;
    this.audioOutPhase       = false;
    this.audioOutState       = false;
    this.haltState           = false;
    this.curDisplayTStates   = 0;
    this.displayCheckTStates = 0;
    this.pio1BValue          = 0xFF;
    this.curSegValue         = 0xFF;
    this.segValues           = new int[ 6 ];
    this.segStatus           = new int[ 6 ];
    this.kbMatrixValues      = new int[ 6 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio1 = new Z80PIO( cpu );
    this.pio2 = new Z80PIO( cpu );
    this.ctc  = new Z80CTC( cpu );
    cpu.setInterruptSources( this.ctc, this.pio2, this.pio1 );
    cpu.addMaxSpeedListener( this );
    cpu.addHaltStateListener( this );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged();
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		"jkcemu.system" ).startsWith( "LC80e" ) ? 1800 : 900;
  }


	/* --- Z80HaltStateListener --- */

  public void z80HaltStateChanged( Z80CPU cpu, boolean haltState )
  {
    if( haltState != this.haltState ) {
      this.haltState = haltState;
      this.screenFrm.setScreenDirty( true );
    }
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
    this.ctc.systemUpdate( tStates );
    if( this.displayCheckTStates > 0 ) {
      this.curDisplayTStates += tStates;
      if( this.curDisplayTStates > this.displayCheckTStates ) {
	boolean dirty = false;
	for( int i = 0; i < this.segValues.length; i++ ) {
	  if( this.segStatus[ i ] > 0 ) {
	    --this.segStatus[ i ];
	  } else {
	    if( this.segValues[ i ] != 0 ) {
	      this.segValues[ i ] = 0;
	      dirty = true;
	    }
	  }
	}
	if( this.audioOutState ) {
	  this.audioOutLED = !this.audioOutLED;
	  dirty            = true;
	} else {
	  if( this.audioOutLED ) {
	    this.audioOutLED = false;
	    dirty            = true;
	  }
	}
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
	this.audioOutState     = false;
	this.curDisplayTStates = 0;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void applySettings( Properties props )
  {
    super.applySettings( props );
    createColors( props );
  }


  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeHaltStateListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x23EA;
  }


  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
	color = this.greenDark;
	break;

      case 2:
	color = this.greenLight;
	break;

      case 3:
	color = this.redDark;
	break;

      case 4:
	color = this.redLight;
	break;
    }
    return color;
  }


  public int getColorCount()
  {
    return 5;
  }


  public int getMemByte( int addr )
  {
    if( this.romC000 != null ) {
      addr &= 0xFFFF;
    } else {
      addr &= 0x3FFF;		// unvollstaendige Adressdekodierung
    }

    int rv = 0xFF;
    if( addr < 0x2000 ) {
      if( this.rom0000 != null ) {
	if( addr < this.rom0000.length ) {
	  rv = (int) this.rom0000[ addr ] & 0xFF;
	}
      }
    }
    else if( (addr >= 0x2000) && (addr < 0xC000) ) {
      int idx = addr - 0x2000;
      if( idx < this.ram.length ) {
	rv = (int) this.ram[ idx ] & 0xFF;
      }
    }
    else if( addr >= 0xC000 ) {
      if( this.romC000 != null ) {
	int idx = addr - 0xC000;
	if( idx < this.romC000.length ) {
	  rv = (int) this.romC000[ idx ] & 0xFF;
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
    return 429;
  }


  public String getTitle()
  {
    String rv = "LC80";
    if( this.rom0000 == lc80_2 ) {
      rv = "LC80.2";
    }
    else if( this.rom0000 == lc80e_0000 ) {
      rv = "LC80e";
    }
    return rv;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.kbMatrixValues ) {
      switch( keyCode ) {
	case KeyEvent.VK_F1:
	  this.kbMatrixValues[ 5 ] = 0x80;		// ADR
	  rv = true;
	  break;

	case KeyEvent.VK_F2:
	  this.kbMatrixValues[ 5 ] = 0x40;		// DAT
	  rv = true;
	  break;

	case KeyEvent.VK_F3:
	  this.kbMatrixValues[ 0 ] = 0x20;		// LD
	  rv = true;
	  break;

	case KeyEvent.VK_F4:
	  this.kbMatrixValues[ 0 ] = 0x40;		// ST
	  rv = true;
	  break;

	case KeyEvent.VK_F5:
	  this.kbMatrixValues[ 0 ] = 0x80;		// EX
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      putKBMatrixRowValueToPort();
    } else {
      if( keyCode == KeyEvent.VK_ESCAPE ) {
	this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
	rv = true;
      }
    }
    return rv;
  }


  public void keyReleased()
  {
    synchronized( this.kbMatrixValues ) {
      Arrays.fill( this.kbMatrixValues, 0 );
    }
    putKBMatrixRowValueToPort();
  }


  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    int     ch = Character.toUpperCase( keyChar );
    if( ch == 'N' ) {
      this.emuThread.getZ80CPU().fireNMI();
      rv = true;
    } else {
      synchronized( this.kbMatrixValues ) {
	int m  = 0x10;
	for( int i = 0; i < kbMatrix.length; i++ ) {
	  int[] rowKeys = kbMatrix[ i ];
	  for( int k = 0; k < rowKeys.length; k++ ) {
	    if( rowKeys[ k ] == ch ) {
	      this.kbMatrixValues[ k ] |= m;
	      rv = true;
	      break;
	    }
	  }
	  m <<= 1;
	}
      }
      if( rv ) {
	putKBMatrixRowValueToPort();
      }
    }
    return rv;
  }


  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    // LED fuer Tonausgabe, L-aktiv
    if( this.audioOutLED
	|| (!this.audioOutPhase && !this.audioOutState) )
    {
      g.setColor( this.greenLight );
    } else {
      g.setColor( this.greenDark );
    }
    g.fillArc(
	x,
	y,
	20 * screenScale,
	20 * screenScale,
	0,
	360 );

    // LED fuer HALT-Zustand
    g.setColor( this.haltState ? this.redLight : this.redDark );
    g.fillArc(
	x,
	y + (30 * screenScale),
	20 * screenScale,
	20 * screenScale,
	0,
	360 );

    // 7-Segment-Anzeige
    x += (50 * screenScale);
    for( int i = 0; i < this.segValues.length; i++ ) {
      paint7SegDigit(
		g,
		x,
		y,
		this.segValues[ i ],
		this.greenDark,
		this.greenLight,
		screenScale );
      x += (65 * screenScale);
    }
    return true;
  }


  public int readIOByte( int port )
  {
    port &= 0x1F;	// unvollstaendige Adressdekodierung

    int rv = 0xFF;
    if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.pio1.readPortA();
	  break;

	case 1:
	  this.pio1.putInValuePortB(
			this.emuThread.readAudioPhase() ? 1 : 0,
			false );
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
    if( (port & 0x04) == 0 ) {
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
    if( (port & 0x10) == 0 ) {
      rv &= this.ctc.read( port & 0x03 );
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich,
   * wenn sich das emulierte System und der ROM-Inhalt aendern
   */
  public boolean requiresReset( Properties props )
  {
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    return !sysName.startsWith( "LC80" ) || !sysName.equals( this.sysName );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ram );
    }
    Arrays.fill( this.kbMatrixValues, 0 );
    Arrays.fill( this.segValues, 0 );
  }


  public boolean setMemByte( int addr, int value )
  {
    if( this.romC000 != null ) {
      addr &= 0xFFFF;
    } else {
      addr &= 0x3FFF;		// unvollstaendige Adressdekodierung
    }

    boolean rv = false;
    if( (addr >= 0x2000) && (addr < 0xC000) ) {
      int idx = addr - 0x2000;
      if( idx < this.ram.length ) {
	this.ram[ idx ] = (byte) value;
	rv = true;
      }
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    port &= 0x1F;	// unvollstaendige Adressdekodierung

    if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.writePortA( value );
	  this.curSegValue = toDigitValue(
				this.pio1.fetchOutValuePortA( false ) );
	  putKBMatrixRowValueToPort();
	  updDisplay();
	  break;

	case 1:
	  this.pio1.writePortB( value );
	  this.pio1BValue    = this.pio1.fetchOutValuePortB( false );
	  boolean audioPhase = ((this.pio1BValue & 0x02) != 0);
	  this.emuThread.writeAudioPhase( audioPhase );
	  if( audioPhase != this.audioOutPhase ) {
	    this.audioOutPhase = audioPhase;
	    this.audioOutState = true;
	    this.screenFrm.setScreenDirty( true );
	  }
	  putKBMatrixRowValueToPort();
	  updDisplay();
	  break;

	case 2:
	  this.pio1.writeControlA( value );
	  break;

	case 3:
	  this.pio1.writeControlB( value );
	  break;
      }
    }
    if( (port & 0x04) == 0 ) {
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
    if( (port & 0x10) == 0 ) {
      this.ctc.write( port & 0x03, value );
    }
  }


	/* --- private Methoden --- */

  private void createColors( Properties props )
  {
    int value       = getMaxRGBValue( props );
    this.redLight   = new Color( value, 0, 0 );
    this.redDark    = new Color( value / 5, 0, 0 );
    this.greenLight = new Color( 0, value, 0 );
    this.greenDark  = new Color( 0, value / 8, 0 );
  }


  private void putKBMatrixRowValueToPort()
  {
    int v = 0;
    int m = 0x04;
    for( int i = 0; i < this.kbMatrixValues.length; i++ ) {
      if( (this.pio1BValue & m) == 0 ) {
	v |= this.kbMatrixValues[ i ];
      }
      m <<= 1;
    }
    this.pio2.putInValuePortB( ~v, 0xF0 );
  }


  /*
   * Eingang: L-Aktiv
   *   Bit: 0 -> B
   *   Bit: 1 -> F
   *   Bit: 2 -> A
   *   Bit: 3 -> G
   *   Bit: 4 -> P
   *   Bit: 5 -> C
   *   Bit: 6 -> E
   *   Bit: 7 -> D
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
    int rv = 0;
    if( (value & 0x01) == 0 ) {
      rv |= 0x02;
    }
    if( (value & 0x02) == 0 ) {
      rv |= 0x20;
    }
    if( (value & 0x04) == 0 ) {
      rv |= 0x01;
    }
    if( (value & 0x08) == 0 ) {
      rv |= 0x40;
    }
    if( (value & 0x10) == 0 ) {
      rv |= 0x80;
    }
    if( (value & 0x20) == 0 ) {
      rv |= 0x04;
    }
    if( (value & 0x40) == 0 ) {
      rv |= 0x10;
    }
    if( (value & 0x80) == 0 ) {
      rv |= 0x08;
    }
    return rv;
  }


  private void updDisplay()
  {
    boolean b = false;
    int     m = 0x80;
    for( int i = 0; i < this.segValues.length; i++ ) {
      if( (this.pio1BValue & m) == 0 ) {
	this.segStatus[ i ] = 2;
	if( this.segValues[ i ] != this.curSegValue ) {
	  this.segValues[ i ] = this.curSegValue;
	  b = true;
	}
      }
      m >>= 1;
    }
    if( b )
      this.screenFrm.setScreenDirty( true );
  }
}

