/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LLC1
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuMemView;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.SaveDlg;
import jkcemu.base.SourceUtil;
import jkcemu.emusys.llc1.LLC1KeyboardFld;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80InterruptSource;
import z80emu.Z80PIO;


public class LLC1 extends EmuSys
{
  public static final String SYSNAME     = "LLC1";
  public static final String PROP_PREFIX = "jkcemu.llc1.";

  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 500;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = true;

  private static final int DISPLAY_DISTANCE = 30;

  private static byte[] rom0000 = null;
  private static byte[] rom0800 = null;
  private static byte[] romFont = null;

  private byte[]          ramStatic;
  private byte[]          ramVideo;
  private LLC1KeyboardFld keyboardFld;
  private int[]           keyboardMatrix;
  private int[]           digitStatus;
  private int[]           digitValues;
  private int             digitIdx;
  private int             keyChar;
  private int             keyAlphaTStates;
  private int             keyAlphaTStateCounter;
  private int             keyStartTStates;
  private int             keyStartTStateCounter;
  private boolean         pio1B7Value;
  private long            curDisplayTStates;
  private long            displayCheckTStates;
  private Z80CTC          ctc;
  private Z80PIO          pio1;
  private Z80PIO          pio2;


  public LLC1( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    if( rom0000 == null ) {
      rom0000 = readResource( "/rom/llc1/llc1mon.bin" );
    }
    if( rom0800 == null ) {
      rom0800 = readResource( "/rom/llc1/tinybasic.bin" );
    }
    if( romFont == null ) {
      romFont = readResource( "/rom/llc1/llc1font.bin" );
    }
    this.ramStatic           = new byte[ 0x0800 ];
    this.ramVideo            = new byte[ 0x0400 ];
    this.keyboardFld         = null;
    this.keyboardMatrix      = new int[ 4 ];
    this.digitStatus         = new int[ 8 ];
    this.digitValues         = new int[ 8 ];
    this.digitIdx            = 0;
    this.displayCheckTStates = 0;
    this.curDisplayTStates   = 0;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( "CTC" );
    this.pio1  = new Z80PIO( "PIO 1" );	// nicht in der Interrupt-Kette!
    this.pio2  = new Z80PIO( "PIO 2" );
    cpu.setInterruptSources( this.ctc, this.pio2 );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    z80MaxSpeedChanged( cpu );
  }


  public static String getBasicProgram( EmuMemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x154E,
				memory.getMemWord( 0x141B ) );
  }


  public static int getDefaultSpeedKHz()
  {
    return 2000;
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    boolean pressed = false;
    synchronized( this.keyboardMatrix ) {
      int n = Math.min( kbMatrix.length, this.keyboardMatrix.length );
      int i = 0;
      while( i < n ) {
	if( (~this.keyboardMatrix[ i ] & kbMatrix[ i ]) != 0 ) {
	  pressed = true;
	}
	this.keyboardMatrix[ i ] = kbMatrix[ i ];
	i++;
      }
      while( i < this.keyboardMatrix.length ) {
	this.keyboardMatrix[ i ] = 0;
	i++;
      }
    }
    if( pressed ) {
      this.ctc.externalUpdate( 3, 1 );
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
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new LLC1KeyboardFld( this );
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
    return 0x1C00;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
        color = this.colorWhite;
        break;

      case 2:
        color = this.colorRedDark;
        break;

      case 3:
        color = this.colorRedLight;
        break;
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return 4;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( 64, 16, 14, 8, 8, 0 );
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 50;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 150;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 50;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/llc1.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr < 0x0800) && (rom0000 != null) ) {
      if( addr < rom0000.length ) {
	rv = (int) rom0000[ addr ] & 0xFF;
      }
    }
    else if( (addr >= 0x0800) && (addr < 0x1400) && (rom0800 != null) ) {
      int idx = addr - 0x0800;
      if( idx < rom0800.length ) {
	rv = (int) rom0800[ idx ] & 0xFF;
      }
    }
    else if( (addr >= 0x1400) && (addr < 0x1C00) ) {
      int idx = addr - 0x1400;
      if( idx < this.ramStatic.length ) {
	rv = (int) this.ramStatic[ idx ] & 0xFF;
      }
    }
    else if( (addr >= 0x1C00) && (addr < 0x2000) ) {
      int idx = addr - 0x1C00;
      if( idx < this.ramVideo.length ) {
	rv = (int) this.ramVideo[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * 64) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      switch( b ) {
	case 0x00:
	case 0x01:
	case 0x02:
	case 0x03:
	case 0x04:
	case 0x0F:
	case 0x20:
	case 0x21:
	case 0x22:
	case 0x23:
	case 0x24:
	case 0x40:
	case 0x5F:
	case 0x60:
	  ch = '\u0020';
	  break;
	case 0x0E:
	  ch = '\u00B7';
	  break;
	case 0x10:
	  ch = '/';
	  break;
	case 0x11:
	  ch = ';';
	  break;
	case 0x12:
	  ch = '\"';
	  break;
	case 0x13:
	  ch = '=';
	  break;
	case 0x14:
	  ch = '%';
	  break;
	case 0x15:
	  ch = '&';
	  break;
	case 0x16:
	  ch = '(';
	  break;
	case 0x17:
	  ch = ')';
	  break;
	case 0x18:
	  ch = '_';
	  break;
	case 0x19:
	  /*
	   * Dieser Code stellt beim LLC1 optische das Paragraph-Zeichen dar.
	   * Er wird aber im BASIC fuer das Variablen-Array verwendet,
	   * was normalerweise durch den Klammeraffen dargestellt wird.
	   * Aus diesem Grund wird es hier auch auf den Klammeraffen gemappt.
	   */
	  ch = '@';
	  break;
	case 0x1A:
	  ch = ':';
	  break;
	case 0x1B:
	  ch = '#';
	  break;
	case 0x1C:
	  ch = '*';
	  break;
	case 0x1D:
	  ch = '\'';
	  break;
	case 0x1E:
	  ch = '!';
	  break;
	case 0x1F:
	  ch = '?';
	  break;
	case 0x3A:
	  ch = '\u00AC';
	  break;
	case 0x3B:
	  ch = '$';
	  break;
	case 0x3C:
	  ch = '+';
	  break;
	case 0x3D:
	  ch = '-';
	  break;
	case 0x3E:
	  ch = '.';
	  break;
	case 0x3F:
	  ch = ',';
	  break;
	case 0x5B:
	  ch = ']';
	  break;
	case 0x5C:
	  ch = '[';
	  break;
	case 0x5E:
	  ch = '\u0060';
	  break;
	case 0x7B:
	  ch = '>';
	  break;
	case 0x7C:
	  ch = '<';
	  break;
	case 0x7D:
	  ch = '|';
	  break;
	case 0x7E:
	  ch = '^';
	  break;
	default:
	  if( ((b >= '0') && (b <= '9'))
	      || ((b >= 'A') && (b <= 'Z'))
	      || ((b >= 'a') && (b <= 'z')) )
	  {
	    ch = b;
	  }
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    // (15 Zeilen * 14) + (1 Zeile * 8) + DISPLAY_DISTANCE + 80
    return 298 + DISPLAY_DISTANCE;
  }


  @Override
  public int getScreenWidth()
  {
    return 512;
  }


  @Override
  public String getTitle()
  {
    return SYSNAME;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_DOWN:
	ch = 0x04;
	break;

      case KeyEvent.VK_UP:
	ch = 0x24;
	break;

      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 0x08;
	break;

      case KeyEvent.VK_RIGHT:
	ch = 0x09;
	break;

      case KeyEvent.VK_ENTER:
	synchronized( this.keyboardMatrix ) {
	  this.keyboardMatrix[ 2 ] = 0x82;		// ST
	  if( this.keyAlphaTStateCounter <= 0 ) {
	    this.ctc.externalUpdate( 3, 1 );
	    updKeyboardFld();
	  }
	}
	ch = 0x0D;
	rv = true;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x40;
	break;
    }
    if( ch > 0 ) {
      synchronized( this.keyboardMatrix ) {
	this.keyChar               = ch;
	this.keyStartTStateCounter = this.keyStartTStates;
      }
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
      this.keyChar               = 0;
      this.keyStartTStateCounter = 0;
    }
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      synchronized( this.keyboardMatrix ) {
	switch( ch ) {
	  case '0':
	  case '1':
	  case '2':
	  case '3':
	    this.keyboardMatrix[ ch - '0' ] = 0x01;
	    rv = true;
	    break;

	  case '4':
	  case '5':
	  case '6':
	  case '7':
	    this.keyboardMatrix[ ch - '4' ] = 0x02;
	    rv = true;
	    break;

	  case '8':
	  case '9':
	    this.keyboardMatrix[ ch - '8' ] = 0x04;
	    rv = true;
	    break;

	  case 'A':
	  case 'a':
	    this.keyboardMatrix[ 2 ] = 0x04;
	    rv = true;
	    break;

	  case 'B':
	  case 'b':
	    this.keyboardMatrix[ 3 ] = 0x04;
	    rv = true;
	    break;

	  case 'C':
	  case 'c':
	    this.keyboardMatrix[ 0 ] = 0x81;
	    rv = true;
	    break;

	  case 'D':
	  case 'd':
	    this.keyboardMatrix[ 1 ] = 0x81;
	    rv = true;
	    break;

	  case 'E':
	  case 'e':
	    this.keyboardMatrix[ 2 ] = 0x81;
	    rv = true;
	    break;

	  case 'F':
	  case 'f':
	    this.keyboardMatrix[ 3 ] = 0x81;
	    rv = true;
	    break;

	  case 'R':
	    this.keyboardMatrix[ 0 ] = 0x82;		// REG
	    rv = true;
	    break;

	  case 'M':
	    this.keyboardMatrix[ 1 ] = 0x82;		// EIN
	    rv = true;
	    break;

	  case 'X':
	    this.keyboardMatrix[ 2 ] = 0x82;		// ST
	    rv = true;
	    break;

	  case 'S':
	    this.keyboardMatrix[ 0 ] = 0x84;		// ES
	    rv = true;
	    break;

	  case 'G':
	  case 'J':
	    this.keyboardMatrix[ 1 ] = 0x84;		// DL
	    rv = true;
	    break;

	  case 'H':
	    this.keyboardMatrix[ 2 ] = 0x84;		// HP
	    rv = true;
	    break;

	  default:
	    if( ((ch < 'A') || (ch > 'Z'))
		&& ((ch < 'a') || (ch > 'z')) )
	    {
	      switch( ch ) {
		case '\u00B7':
		  ch = 0xE0;
		  break;

		case '/':
		  ch = 0x10;
		  break;

		case ';':
		  ch = 0x11;
		  break;

		case '\"':
		  ch = 0x12;
		  break;

		case '=':
		  ch = 0x13;
		  break;

		case '%':
		  ch = 0x14;
		  break;

		case '&':
		  ch = 0x15;
		  break;

		case '(':
		  ch = 0x16;
		  break;

		case ')':
		  ch = 0x17;
		  break;

		case '_':
		  ch = 0x18;
		  break;

		case '@':
		case '\u00A7':
		  ch = 0x19;
		  break;

		case ':':
		  ch = 0x1A;
		  break;

		case '#':
		  ch = 0x1B;
		  break;

		case '*':
		  ch = 0x1C;
		  break;

		case '\'':
		  ch = 0x1D;
		  break;

		case '!':
		  ch = 0x1E;
		  break;

		case '?':
		  ch = 0x1F;
		  break;

		case '\u00AC':
		  ch = 0x3A;
		  break;

		case '$':
		  ch = 0x3B;
		  break;

		case '+':
		  ch = 0x3C;
		  break;

		case '-':
		  ch = 0x3D;
		  break;

		case '.':
		  ch = 0x3E;
		  break;

		case ',':
		  ch = 0x3F;
		  break;

		case '\u0020':
		  ch = 0x40;
		  break;

		case ']':
		  ch = 0x5B;
		  break;

		case '[':
		  ch = 0x5C;
		  break;

		case '\\':
		  ch = 0x5E;
		  break;

		case '>':
		  ch = 0x7B;
		  break;

		case '<':
		  ch = 0x7C;
		  break;

		case '|':
		  ch = 0x7D;
		  break;

		case '^':
		  ch = 0x7E;
		  break;

		default:
		  ch = 0;
	      }
	    }
	}
	if( rv && (this.keyAlphaTStateCounter <= 0) ) {
	  this.ctc.externalUpdate( 3, 1 );
	  updKeyboardFld();
	}
	if( ch > 0 ) {
	  this.keyChar               = ch;
	  this.keyStartTStateCounter = this.keyStartTStates;
	}
      }
      rv = true;
    }
    return rv;
  }


  @Override
  public void openBasicProgram()
  {
    String text = getBasicProgram( this.emuThread );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    // Bildschirmausgabe
    if( romFont != null ) {
      g.setColor( this.colorWhite );
      for( int pRow = 0; pRow < 218; pRow++ ) {
	int pCh = pRow % 14;
	if( (pCh >= 0) && (pCh < 8) ) {
	  boolean lastState = false;
	  int     xBeg      = 0;
	  int     cRow      = pRow / 14;
	  for( int cCol = 0; cCol < 64; cCol++ ) {
	    int mIdx = (cRow * 64) + cCol;
	    if( (mIdx >= 0) && (mIdx < this.ramVideo.length) ) {
	      int b  = 0;
	      int ch = (int) this.ramVideo[ mIdx ] & 0xFF;
	      if( (pCh == 7) && ((ch & 0x80) != 0) ) {
		b = 0xFF;	// Cursor als Strich in 7. Pixelzeile
	      } else {
		int fIdx = (pCh * 128) + (ch & 0x7F);
		if( (fIdx >= 0) && (fIdx < romFont.length) ) {
		  b = (int) romFont[ fIdx ] & 0xFF;
		}
	      }
	      int m = 0x80;
	      for( int i = 0; i < 8; i++ ) {
		boolean curState = ((b & m) != 0);
		if( curState != lastState ) {
		  int xEnd = (cCol * 8) + i;
		  if( lastState && (xEnd > xBeg) ) {
		    if( screenScale > 0 ) {
		      g.fillRect(
				x + (xBeg * screenScale),
				y + (pRow * screenScale),
				(xEnd - xBeg) * screenScale,
				screenScale );
		    } else {
		      g.fillRect( x + xBeg, y + pRow, xEnd - xBeg, 1 );
		    }
		  }
		  xBeg      = xEnd;
		  lastState = curState;
		}
		m >>= 1;
	      }
	    }
	  }
	  if( lastState ) {
	    if( screenScale > 0 ) {
	      g.drawRect(
			x + (xBeg * screenScale),
			y + (pRow * screenScale),
			(512 - xBeg) * screenScale,
			screenScale );
	    } else {
	      g.drawRect( x + xBeg, y + pRow, 512 - xBeg, 1 );
	    }
	  }
	}
      }
    }

    // 7-Segment-Anzeige
    x += (3 * screenScale);		// zentrieren
    y += ((218 + DISPLAY_DISTANCE) * screenScale);
    synchronized( this.digitValues ) {
      for( int i = 0; i < this.digitValues.length; i++ ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitStatus[ i ] > 0 ? this.digitValues[ i ] : 0,
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
    if( (port & 0x04) == 0 ) {
      rv &= this.ctc.read( port & 0x03, tStates );
    }
    else if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  synchronized( this.keyboardMatrix ) {
	    this.pio1.putInValuePortA( getHexKeyMatrixValue(), 0x8F );
	  }
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
    else if( (port & 0x10) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.pio2.readDataA();
	  break;

	case 1:
	  synchronized( this.keyboardMatrix ) {
	    this.pio2.putInValuePortB(
			this.keyStartTStateCounter > 0 ? 0xFF : this.keyChar,
			false );
	  }
	  rv &= this.pio2.readDataB();
	  this.keyAlphaTStateCounter = this.keyAlphaTStates;
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
    super.reset( resetLevel, props );
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ramStatic, props );
      fillRandom( this.ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio1.reset( true );
      this.pio2.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio1.reset( false );
      this.pio2.reset( false );
    }
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    this.keyChar               = 0;
    this.keyAlphaTStateCounter = 0;
    this.keyStartTStateCounter = 0;
    this.pio1B7Value           = false;
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = this.emuThread.getMemWord( 0x141B );
    if( (endAddr > 0x154E)
	&& (this.emuThread.getMemByte( endAddr - 1, false ) == 0x0D) )
    {
      (new SaveDlg(
		this.screenFrm,
		0x1400,
		endAddr,
		"LLC1-BASIC-Programm speichern",
		SaveDlg.BasicType.TINYBASIC,
		null )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    if( (addr < 0x0800) && (rom0000 != null) ) {
      if( addr < rom0000.length ) {
	rv = true;
      }
    }
    else if( (addr >= 0x0800) && (addr < 0x1400) && (rom0800 != null) ) {
      int idx = addr - 0x0800;
      if( idx < rom0800.length ) {
	rv = true;
      }
    }
    else if( (addr >= 0x1400) && (addr < 0x1C00) ) {
      int idx = addr - 0x1400;
      if( idx < this.ramStatic.length ) {
	this.ramStatic[ idx ] = (byte) value;
	rv = true;
      }
    }
    else if( (addr >= 0x1C00) && (addr < 0x2000) ) {
      int idx = addr - 0x1C00;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsKeyboardFld()
  {
    return true;
  }


  @Override
  public boolean supportsOpenBasic()
  {
    return true;
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsSaveBasic()
  {
    return true;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( (port & 0x04) == 0 ) {
      this.ctc.write( port & 0x03, value, tStates );
    }
    else if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pio1.writeDataA( value );
	  break;

	case 1:
	  boolean oldReady = this.pio1.isReadyPortB();
	  this.pio1.writeDataB( value );
	  if( !oldReady && this.pio1.isReadyPortB() ) {
	    this.digitIdx = (this.digitIdx + 1) & 0x07;
	  }
	  int     bValue  = this.pio1.fetchOutValuePortB( true );
	  boolean b7Value = ((bValue & 0x80) != 0);
	  if( b7Value != this.pio1B7Value ) {
	    if( b7Value ) {
	      this.digitIdx = 0;
	    }
	    this.pio1B7Value = b7Value;
	  }
	  bValue &= 0x7F;
	  if( bValue != 0 ) {
	    synchronized( this.digitValues ) {
	      if( bValue != this.digitValues[ this.digitIdx ] ) {
		this.digitValues[ this.digitIdx ] = bValue;
		this.screenFrm.setScreenDirty( true );
	      }
	      this.digitStatus[ this.digitIdx ] = 2;
	    }
	  }
	  break;

	case 2:
	  this.pio1.writeControlA( value );
	  break;

	case 3:
	  this.pio1.writeControlB( value );
	  break;
      }
    }
    else if( (port & 0x10) == 0 ) {
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
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );

    int maxSpeedKHz          = cpu.getMaxSpeedKHz();
    this.displayCheckTStates = maxSpeedKHz * 50;
    synchronized( this.keyboardMatrix ) {
      this.keyAlphaTStates = maxSpeedKHz * 1000;	// 1 Sekunde
      this.keyStartTStates = maxSpeedKHz * 10;		// 1/100 Sekunde
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
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
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
	this.curDisplayTStates = 0;
      }
    }
    if( (this.keyAlphaTStateCounter > 0)
	|| (this.keyStartTStateCounter > 0) )
    {
      synchronized( this.keyboardMatrix ) {
	if( this.keyAlphaTStateCounter > 0 ) {
	  this.keyAlphaTStateCounter -= tStates;
	}
	if( this.keyStartTStateCounter > 0 ) {
	  this.keyStartTStateCounter -= tStates;
	}
      }
    }
  }


	/* --- private Methoden --- */

  private int getHexKeyMatrixValue()
  {
    int rv = 0;
    if( this.keyAlphaTStateCounter <= 0 ) {
      int a = (~this.pio1.fetchOutValuePortA( false ) >> 4) & 0x07;
      int m = 0x01;
      for( int i = 0; i < this.keyboardMatrix.length; i++ ) {
	if( (this.keyboardMatrix[ i ] & a) != 0 ) {
	  rv |= (m | (this.keyboardMatrix[ i ] & 0x80));
	}
	m <<= 1;
      }
    }
    return rv;
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}

