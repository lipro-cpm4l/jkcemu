/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Schach-Computers SC2
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class SC2 extends EmuSys implements
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static byte[] rom0000 = null;
  private static byte[] rom2000 = null;

  private byte[]  ram;
  private int[]   keyMatrixValues;
  private int[]   digitStatus;
  private int[]   digitValues;
  private int     beepStatus;
  private int     ledChessStatus;
  private int     ledMateStatus;
  private boolean ledChessValue;
  private boolean ledMateValue;
  private boolean audioOutPhase;
  private long    curBeepCheckTStates;
  private long    curBeepFreqTStates;
  private long    curDisplayTStates;
  private long    displayCheckTStates;
  private long    beepCheckTStates;
  private long    beepFreqTStates;
  private Z80PIO  pio;


  public SC2( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( rom0000 == null ) {
      rom0000 = readResource( "/rom/sc2/sc2_0000.bin" );
    }
    if( rom2000 == null ) {
      rom2000 = readResource( "/rom/sc2/sc2_2000.bin" );
    }
    this.ram             = new byte[ 0x0400 ];
    this.keyMatrixValues = new int[ 4 ];
    this.digitStatus     = new int[ 4 ];
    this.digitValues     = new int[ 4 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio   = new Z80PIO( "PIO" );
    cpu.setInterruptSources( this.pio );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged( cpu );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2458;		// eigentlich 2,4576 MHz
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    int maxSpeedKHz          = cpu.getMaxSpeedKHz();
    this.beepCheckTStates    = maxSpeedKHz * 20;
    this.beepFreqTStates     = maxSpeedKHz / 3;		// Tonhoehe des Beeps
    this.displayCheckTStates = maxSpeedKHz * 50;
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.displayCheckTStates > 0 ) {
      this.curDisplayTStates += tStates;
      if( this.curDisplayTStates > this.displayCheckTStates ) {
	boolean dirty = false;
	synchronized( this.digitValues ) {
	  for( int i = 0; i < this.digitValues.length; i++ ) {
	    int status = this.digitStatus[ i ];
	    if( status < 4 ) {
	      if( status > 0 ) {
		--this.digitStatus[ i ];
	      } else {
		if( this.digitValues[ i ] != 0 ) {
		  this.digitValues[ i ] = 0;
		  dirty = true;
		}
	      }
	    }
	  }
	  if( this.ledChessStatus < 4 ) {
	    if( this.ledChessStatus > 0 ) {
	      --this.ledChessStatus;
	    } else {
	      if( this.ledChessValue ) {
		this.ledChessValue = false;
		dirty = true;
	      }
	    }
	  }
	  if( this.ledMateStatus < 4 ) {
	    if( this.ledMateStatus > 0 ) {
	      --this.ledMateStatus;
	    } else {
	      if( this.ledMateValue ) {
		this.ledMateValue = false;
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
    if( (this.beepStatus > 0)
	&& (this.beepCheckTStates > 0)
	&& (this.beepFreqTStates > 0) )
    {
      this.curBeepCheckTStates += tStates;
      if( this.curBeepCheckTStates > this.beepCheckTStates ) {
	this.curBeepCheckTStates = 0;
	--this.beepStatus;
      }
      if( this.beepStatus > 0 ) {
	this.curBeepFreqTStates += tStates;
	if( this.curBeepFreqTStates > this.beepFreqTStates ) {
	  this.curBeepFreqTStates = 0;
	  this.audioOutPhase = !this.audioOutPhase;
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canApplySettings( Properties props )
  {
    return EmuUtil.getProperty( props, "jkcemu.system" ).equals( "SC2" );
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
  public Chessman getChessman( int row, int col )
  {
    Chessman rv = null;
    if( (row >= 0) && (row < 8) && (col >= 0) && (col < 8) ) {
      switch( getMemByte( 0x1000 + (row * 16) + col, false ) ) {
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

	case 0xFF:
	  rv = Chessman.BLACK_PAWN;
	  break;

	case 0xFE:
	  rv = Chessman.BLACK_KNIGHT;
	  break;

	case 0xFD:
	  rv = Chessman.BLACK_BISHOP;
	  break;

	case 0xFC:
	  rv = Chessman.BLACK_ROOK;
	  break;

	case 0xFB:
	  rv = Chessman.BLACK_QUEEN;
	  break;

	case 0xFA:
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
    return "/help/sc2.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    int rv = 0xFF;

    addr &= 0x3FFF;		// A14 und A15 ignorieren
    if( (addr < 0x1000) && (rom0000 != null) ) {
      if( addr < rom0000.length ) {
	rv = (int) rom0000[ addr ] & 0xFF;
      }
    }
    else if( (addr >= 0x1000) && (addr < 0x2000) ) {
      int idx = addr - 0x1000;
      if( idx < this.ram.length ) {
	rv = (int) this.ram[ idx ] & 0xFF;
      }
    }
    else if( (addr >= 0x2000) && (addr < 0x3C00) && (rom2000 != null) ) {
      int idx = addr - 0x2000;
      if( idx < rom2000.length ) {
	rv = (int) rom2000[ idx ] & 0xFF;
      }
    }
    else if( addr >= 0x3C00 ) {
      this.beepStatus = 3;
    }
    return rv;
  }


  @Override
  public int getScreenHeight()
  {
    return 110;
  }


  @Override
  public int getScreenWidth()
  {
    return 70 + (this.digitValues.length * 50);
  }


  @Override
  public String getTitle()
  {
    return "SC2";
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    if( keyCode == KeyEvent.VK_BACK_SPACE ) {
      synchronized( this.keyMatrixValues ) {
	this.keyMatrixValues[ 0 ] |= 0x40;
        rv = true;
      }
    }
    else if( keyCode == KeyEvent.VK_ENTER ) {
      synchronized( this.keyMatrixValues ) {
	this.keyMatrixValues[ 0 ] |= 0x80;
        rv = true;
      }
    }
    else if( keyCode == KeyEvent.VK_ESCAPE ) {
      this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.keyMatrixValues ) {
      Arrays.fill( this.keyMatrixValues, 0 );
    }
  }


  @Override
  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    synchronized( this.keyMatrixValues ) {
      switch( Character.toUpperCase( keyChar ) ) {
	case '1':
	case 'A':
	  this.keyMatrixValues[ 1 ] |= 0x10;
	  rv = true;
	  break;

	case '2':
	case 'B':
	  this.keyMatrixValues[ 1 ] |= 0x20;
	  rv = true;
	  break;

	case '3':
	case 'C':
	  this.keyMatrixValues[ 1 ] |= 0x40;
	  rv = true;
	  break;

	case '4':
	case 'D':
	  this.keyMatrixValues[ 1 ] |= 0x80;
	  rv = true;
	  break;

	case '5':
	case 'E':
	  this.keyMatrixValues[ 2 ] |= 0x10;
	  rv = true;
	  break;

	case '6':
	case 'F':
	  this.keyMatrixValues[ 2 ] |= 0x20;
	  rv = true;
	  break;

	case '7':
	case 'G':
	  this.keyMatrixValues[ 2 ] |= 0x40;
	  rv = true;
	  break;

	case '8':
	case 'H':
	  this.keyMatrixValues[ 2 ] |= 0x80;
	  rv = true;
	  break;

	case 'K':
	case '+':
	  this.keyMatrixValues[ 3 ] |= 0x10;
	  rv = true;
	  break;

	case 'L':
	  this.keyMatrixValues[ 0 ] |= 0x40;
	  rv = true;
	  break;

	case 'P':
	  this.keyMatrixValues[ 3 ] |= 0x80;
	  rv = true;
	  break;

	case 'Q':
	  this.keyMatrixValues[ 0 ] |= 0x80;
	  rv = true;
	  break;

	case 'S':
	case 'W':
	  this.keyMatrixValues[ 3 ] |= 0x20;
	  rv = true;
	  break;

	case 'T':
	  this.keyMatrixValues[ 0 ] |= 0x20;
	  rv = true;
	  break;
      }
    }
    return rv;
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    synchronized( this.digitValues ) {

      // LED Schach
      g.setFont( new Font( "SansSerif", Font.BOLD, 18 * screenScale ) );
      g.setColor( this.ledChessValue ?
				this.colorGreenLight
				: this.colorGreenDark );
      g.drawString( "Schach", x, y + (110 * screenScale) );

      // LED Matt
      int          xMate = 0;
      final String s     = "Matt";
      FontMetrics  fm    = g.getFontMetrics();
      if( fm != null ) {
	xMate = x + (getScreenWidth() * screenScale) - fm.stringWidth( s );
      } else {
	xMate = x + (getScreenWidth() / 2 * screenScale);
      }
      g.setColor( this.ledMateValue ?
				this.colorGreenLight
				: this.colorGreenDark );
      g.drawString( s, xMate, y + (110 * screenScale) );

      // 7-Segment-Anzeige
      for( int i = 0; i < this.digitValues.length; i++ ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitValues[ i ],
		this.colorGreenDark,
		this.colorGreenLight,
		screenScale );
	x += ((i == 1 ? 90 : 65) * screenScale);
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
          rv = this.pio.readPortA();
          break;

        case 1:
	  {
	    int v = this.pio.fetchOutValuePortB( false ) & 0x0F;
	    synchronized( this.keyMatrixValues ) {
	      int m = 0x01;
	      for( int i = 0; i < this.keyMatrixValues.length; i++ ) {
		if( (v & m) != 0 ) {
		  v |= (this.keyMatrixValues[ i ] & 0xF0);
		}
		m <<= 1;
	      }
	    }
	    this.pio.putInValuePortB( v, 0xF0 );
	  }
          rv = this.pio.readPortB();
          break;

        case 2:
          rv = this.pio.readControlA();
          break;

        case 3:
          rv = this.pio.readControlB();
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
    this.audioOutPhase       = false;
    this.ledChessValue       = false;
    this.ledMateValue        = false;
    this.ledChessStatus      = 0;
    this.ledMateStatus       = 0;
    this.curDisplayTStates   = 0;
    this.curBeepFreqTStates  = 0;
    this.curBeepCheckTStates = 0;
    this.beepStatus          = 0;
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    boolean rv = false;

    addr &= 0x3FFF;		// A14 und A15 ignorieren
    if( (addr >= 0x1000) && (addr < 0x3C00) ) {
      int idx = addr - 0x1000;
      if( idx < this.ram.length ) {
	this.ram[ idx ] = (byte) value;
	if( (idx < 0x78) && ((idx % 16) < 8) ) {
	  this.screenFrm.setChessboardDirty( true );
	}
	rv = true;
      }
    }
    else if( addr >= 0x3C00 ) {
      this.beepStatus = 3;
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
    return true;
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
          this.pio.writePortA( value );
	  updDisplay();
          break;

        case 1:
          this.pio.writePortB( value );
	  updDisplay();
          break;

        case 2:
          this.pio.writeControlA( value );
          break;

        case 3:
          this.pio.writeControlB( value );
          break;
      }
    }
  }


	/* --- private Methoden --- */

  /*
   * Eingang: H-Aktiv
   *   Bit: 0 -> G
   *   Bit: 1 -> F
   *   Bit: 2 -> E
   *   Bit: 3 -> D
   *   Bit: 4 -> C
   *   Bit: 5 -> B
   *   Bit: 6 -> A
   *   Bit: 7 -> LED, in 7-Segment-Anzeige nicht verwendet
   *
   * Ausgang: H-Aktiv
   *   Bit: 0 -> A
   *   Bit: 1 -> B
   *   Bit: 2 -> C
   *   Bit: 3 -> D
   *   Bit: 4 -> E
   *   Bit: 5 -> F
   *   Bit: 6 -> G
   */
  private int toDigitValue( int value )
  {
    int rv = value & 0x08;	// D stimmt ueberein
    if( (value & 0x01) != 0 ) {
      rv |= 0x40;
    }
    if( (value & 0x02) != 0 ) {
      rv |= 0x20;
    }
    if( (value & 0x04) != 0 ) {
      rv |= 0x10;
    }
    if( (value & 0x10) != 0 ) {
      rv |= 0x04;
    }
    if( (value & 0x20) != 0 ) {
      rv |= 0x02;
    }
    if( (value & 0x40) != 0 ) {
      rv |= 0x01;
    }
    return rv;
  }


  private void updDisplay()
  {
    int     portAValue = this.pio.fetchOutValuePortA( false );
    int     digitValue = toDigitValue( portAValue & 0x7F );
    int     colValue   = this.pio.fetchOutValuePortB( false );
    boolean ledValue   = ((portAValue & 0x80) != 0);
    boolean dirty      = false;
    synchronized( this.digitValues ) {
      int m = 0x01;
      for( int i = 0; i < this.digitValues.length; i++ ) {
	if( (colValue & m) == 0 ) {
	  if( digitValue != 0 ) {
	    if( digitValue != this.digitValues[ i ] ) {
	      this.digitValues[ i ] = digitValue;
	      dirty                 = true;
	    }
	    this.digitStatus[ i ] = 4;
	  } else {
	    if( this.digitStatus[ i ] > 3 ) {
	      this.digitStatus[ i ] = 3;
	    }
	  }
	}
	m <<= 1;
      }
      if( ledValue && ((colValue & 0x01) == 0) ) {
	if( ledValue != this.ledChessValue ) {
	  this.ledChessValue = ledValue;
	  dirty              = true;
	}
	this.ledChessStatus = 4;
      } else {
	if( this.ledChessStatus > 3 ) {
	  this.ledChessStatus = 3;
	}
      }
      if( ledValue && ((colValue & 0x02) == 0) ) {
	if( ledValue != this.ledMateValue ) {
	  this.ledMateValue = ledValue;
	  dirty             = true;
	}
	this.ledMateStatus = 4;
      } else {
	if( this.ledMateStatus > 3 ) {
	  this.ledMateStatus = 3;
	}
      }
    }
    if( dirty ) {
      this.screenFrm.setScreenDirty( true );
    }
  }
}

