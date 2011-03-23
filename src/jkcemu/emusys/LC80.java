/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LC80
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class LC80 extends EmuSys implements
					Z80HaltStateListener,
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80TStatesListener
{
  private static byte[] lc80_u505  = null;
  private static byte[] lc80_2716  = null;
  private static byte[] lc80_2     = null;
  private static byte[] lc80e_0000 = null;
  private static byte[] lc80e_c000 = null;

  private byte[]           rom0000;
  private byte[]           romC000;
  private byte[]           ram;
  private int[]            kbMatrixValues;
  private int[]            digitStatus;
  private int[]            digitValues;
  private int              curDigitValue;
  private volatile int     pio1BValue;
  private long             curDisplayTStates;
  private long             displayCheckTStates;
  private boolean          audioOutLED;
  private volatile boolean audioOutPhase;
  private volatile boolean audioOutState;
  private boolean          chessComputer;
  private boolean          chessMode;
  private boolean          haltState;
  private Z80CTC           ctc;
  private Z80PIO           pio1;
  private Z80PIO           pio2;
  private String           sysName;


  public LC80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.chessComputer = false;
    this.sysName       = EmuUtil.getProperty( props, "jkcemu.system" );
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
      this.rom0000       = lc80e_0000;
      this.romC000       = lc80e_c000;
      this.ram           = new byte[ 0x1000 ];
      this.chessComputer = true;
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
    this.curDigitValue       = 0xFF;
    this.digitValues         = new int[ 6 ];
    this.digitStatus         = new int[ 6 ];
    this.kbMatrixValues      = new int[ 6 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio1 = new Z80PIO( "PIO 1" );
    this.pio2 = new Z80PIO( "PIO 2" );
    this.ctc  = new Z80CTC( "CTC" );
    cpu.setInterruptSources( this.ctc, this.pio2, this.pio1 );
    cpu.addMaxSpeedListener( this );
    cpu.addHaltStateListener( this );
    cpu.addTStatesListener( this );
    if( this.chessComputer ) {
      cpu.addPCListener( this, 0x0000, 0xC800 );
    }

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged( cpu );
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		"jkcemu.system" ).startsWith( "LC80e" ) ? 1800 : 900;
  }


	/* --- Z80HaltStateListener --- */

  @Override
  public void z80HaltStateChanged( Z80CPU cpu, boolean haltState )
  {
    if( haltState != this.haltState ) {
      this.haltState = haltState;
      this.screenFrm.setScreenDirty( true );
    }
  }


	/* --- Z80PCListener --- */

  @Override
  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    switch( pc ) {
      case 0x0000:
	this.chessMode = false;
	break;

      case 0xC800:
	this.chessMode = true;
	break;
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
    this.ctc.z80TStatesProcessed( cpu, tStates );
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

  @Override
  public boolean canApplySettings( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( this.sysName );
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeHaltStateListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.chessComputer ) {
      cpu.removePCListener( this );
    }
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x23EA;
  }


  @Override
  public Chessman getChessman( int row, int col )
  {
    Chessman rv = null;
    if( this.chessMode
	&& (row >= 0) && (row < 8) && (col >= 0) && (col < 8) )
    {
      switch( getMemByte( 0x2715 + (row * 10) + col, false ) & 0x8F ) {
	case 1:
	  rv = Chessman.WHITE_PAWN;
	  break;

	case 2:
	  rv = Chessman.WHITE_KNIGHT;
	  break;

	case 3:
	  rv = Chessman.WHITE_BISHOP;
	  break;

	case 4:
	  rv = Chessman.WHITE_ROOK;
	  break;

	case 5:
	  rv = Chessman.WHITE_QUEEN;
	  break;

	case 6:
	  rv = Chessman.WHITE_KING;
	  break;

	case 0x81:
	  rv = Chessman.BLACK_PAWN;
	  break;

	case 0x82:
	  rv = Chessman.BLACK_KNIGHT;
	  break;

	case 0x83:
	  rv = Chessman.BLACK_BISHOP;
	  break;

	case 0x84:
	  rv = Chessman.BLACK_ROOK;
	  break;

	case 0x85:
	  rv = Chessman.BLACK_QUEEN;
	  break;

	case 0x86:
	  rv = Chessman.BLACK_KING;
	  break;
      }
    }
    return rv;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
	color = this.colorGreenDark;
	break;

      case 2:
	color = this.colorGreenLight;
	break;

      case 3:
	color = this.colorRedDark;
	break;

      case 4:
	color = this.colorRedLight;
	break;
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return 5;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/lc80.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
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


  @Override
  public int getScreenHeight()
  {
    return 85;
  }


  @Override
  public int getScreenWidth()
  {
    return 39 + (65 * this.digitValues.length);
  }


  @Override
  public String getTitle()
  {
    String rv = "LC-80";
    if( this.rom0000 == lc80_2 ) {
      rv = "LC-80.2";
    }
    else if( this.rom0000 == lc80e_0000 ) {
      rv = "LC-80e";
    }
    return rv;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.kbMatrixValues ) {
      switch( keyCode ) {
	case KeyEvent.VK_F1:
	  this.kbMatrixValues[ 5 ] = 0x80;	// ADR / NEW GAME
	  rv = true;
	  break;

	case KeyEvent.VK_F2:
	  this.kbMatrixValues[ 5 ] = 0x40;	// DAT / SW / SELF PLAY
	  rv = true;
	  break;

	case KeyEvent.VK_F3:
	  this.kbMatrixValues[ 0 ] = 0x20;	// LD / RAN
	  rv = true;
	  break;

	case KeyEvent.VK_F4:
	  this.kbMatrixValues[ 0 ] = 0x40;	// ST / Contr.
	  rv = true;
	  break;

	case KeyEvent.VK_ENTER:
	  this.kbMatrixValues[ 0 ] = 0x80;	// EX
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


  @Override
  public void keyReleased()
  {
    synchronized( this.kbMatrixValues ) {
      Arrays.fill( this.kbMatrixValues, 0 );
    }
    putKBMatrixRowValueToPort();
  }


  @Override
  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    synchronized( this.kbMatrixValues ) {
      switch( Character.toUpperCase( keyChar ) ) {
	case '0':
	  this.kbMatrixValues[ 1 ] = 0x80;
	  rv = true;
	  break;

	case '1':
	  this.kbMatrixValues[ 1 ] = 0x40;
	  rv = true;
	  break;

	case '2':
	  this.kbMatrixValues[ 1 ] = 0x20;
	  rv = true;
	  break;

	case '3':
	  this.kbMatrixValues[ 1 ] = 0x10;
	  rv = true;
	  break;

	case '4':
	  this.kbMatrixValues[ 2 ] = 0x80;
	  rv = true;
	  break;

	case '5':
	  this.kbMatrixValues[ 2 ] = 0x40;
	  rv = true;
	  break;

	case '6':
	  this.kbMatrixValues[ 5 ] = 0x20;
	  rv = true;
	  break;

	case '7':
	  this.kbMatrixValues[ 2 ] = 0x10;
	  rv = true;
	  break;

	case '8':
	  this.kbMatrixValues[ 3 ] = 0x80;
	  rv = true;
	  break;

	case '9':
	  this.kbMatrixValues[ 3 ] = 0x40;
	  rv = true;
	  break;

	case '-':
	  this.kbMatrixValues[ 5 ] = 0x10;
	  rv = true;
	  break;

	case '+':
	  this.kbMatrixValues[ 2 ] = 0x20;
	  rv = true;
	  break;

	case 'A':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 1 ] = 0x40;		// auch 1
	  } else {
	    this.kbMatrixValues[ 4 ] = 0x20;
	  }
	  rv = true;
	  break;

	case 'B':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 1 ] = 0x20;		// auch 2
	  } else {
	    this.kbMatrixValues[ 3 ] = 0x10;
	  }
	  rv = true;
	  break;

	case 'C':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 1 ] = 0x10;		// auch 3
	  } else {
	    this.kbMatrixValues[ 4 ] = 0x80;
	  }
	  rv = true;
	  break;

	case 'D':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 2 ] = 0x80;		// auch 4
	  } else {
	    this.kbMatrixValues[ 4 ] = 0x40;
	  }
	  rv = true;
	  break;

	case 'E':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 2 ] = 0x40;		// auch 5
	  } else {
	    this.kbMatrixValues[ 3 ] = 0x20;
	  }
	  rv = true;
	  break;

	case 'F':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 5 ] = 0x20;		// auch 6
	  } else {
	    this.kbMatrixValues[ 4 ] = 0x10;
	  }
	  rv = true;
	  break;

	case 'G':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 2 ] = 0x10;		// auch 7
	    rv = true;
	  }
	  break;

	case 'H':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 3 ] = 0x80;		// auch 8
	    rv = true;
	  }
	  break;

	case 'K':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 4 ] = 0x10;		// Koenig
	    rv = true;
	  }
	  break;

	case 'L':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 4 ] = 0x80;		// Laeufer
	  } else {
	    this.kbMatrixValues[ 0 ] = 0x20;		// LD
	  }
	  rv = true;
	  break;

	case 'M':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 3 ] = 0x20;		// Dame
	    rv = true;
	  }
	  break;

	case 'N':
	  this.emuThread.getZ80CPU().fireNMI();	// NMI
	  rv = true;
	  break;

	case 'O':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 2 ] = 0x20;		// BOARD
	    rv = true;
	  }
	  break;

	case 'R':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 0 ] = 0x20;		// RAN
	    rv = true;
	  }
	  break;

	case 'S':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 3 ] = 0x10;		// Springer
	  } else {
	    this.kbMatrixValues[ 0 ] = 0x40;		// ST
	  }
	  rv = true;
	  break;

	case 'T':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 4 ] = 0x40;		// Turm
	    rv = true;
	  }
	  break;

	case 'U':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 4 ] = 0x20;		// Bauer
	    rv = true;
	  }
	  break;

	case 'W':
	  if( this.chessComputer && this.chessMode ) {
	    this.kbMatrixValues[ 5 ] = 0x10;		// COLOR (WHITE)
	    rv = true;
	  }
	  break;

	case 'X':
	  this.kbMatrixValues[ 0 ] = 0x80;		// EX
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      putKBMatrixRowValueToPort();
    }
    return rv;
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    // LED fuer Tonausgabe, L-aktiv
    if( this.audioOutLED
	|| (!this.audioOutPhase && !this.audioOutState) )
    {
      g.setColor( this.colorGreenLight );
    } else {
      g.setColor( this.colorGreenDark );
    }
    g.fillArc(
	x,
	y,
	20 * screenScale,
	20 * screenScale,
	0,
	360 );

    // LED fuer HALT-Zustand
    g.setColor( this.haltState ? this.colorRedLight : this.colorRedDark );
    g.fillArc(
	x,
	y + (30 * screenScale),
	20 * screenScale,
	20 * screenScale,
	0,
	360 );

    // 7-Segment-Anzeige
    x += (50 * screenScale);
    synchronized( this.digitValues ) {
      for( int i = 0; i < this.digitValues.length; i++ ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitValues[ i ],
		this.colorGreenDark,
		this.colorGreenLight,
		screenScale );
	x += (65 * screenScale);
      }
    }
    return true;
  }


  @Override
  public int readIOByte( int port )
  {
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


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ram, props );
    }
    synchronized( this.kbMatrixValues ) {
      Arrays.fill( this.kbMatrixValues, 0 );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    this.chessMode = false;
  }


  @Override
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
	if( this.chessComputer && (addr >= 0x2715) && (addr < 0x2763) ) {
          this.screenFrm.setChessboardDirty( true );
        }
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
  public boolean supportsChessboard()
  {
    return this.chessComputer;
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.writePortA( value );
	  this.curDigitValue = toDigitValue(
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

  private void putKBMatrixRowValueToPort()
  {
    int v = 0;
    synchronized( this.kbMatrixValues ) {
      int m = 0x04;
      for( int i = 0; i < this.kbMatrixValues.length; i++ ) {
	if( (this.pio1BValue & m) == 0 ) {
	  v |= this.kbMatrixValues[ i ];
	}
	m <<= 1;
      }
    }
    this.pio2.putInValuePortB( ~v, 0xF0 );
  }


  private boolean putKeyToMatrix( int[][] kbMatrix, char ch )
  {
    boolean rv = false;
    int     m  = 0x10;
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
    return rv;
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
    boolean dirty = false;
    synchronized( this.digitValues ) {
      int m = 0x80;
      for( int i = 0; i < this.digitValues.length; i++ ) {
	if( (this.pio1BValue & m) == 0 ) {
	  this.digitStatus[ i ] = 2;
	  if( this.digitValues[ i ] != this.curDigitValue ) {
	    this.digitValues[ i ] = this.curDigitValue;
	    dirty = true;
	  }
	}
	m >>= 1;
      }
    }
    if( dirty )
      this.screenFrm.setScreenDirty( true );
  }
}

