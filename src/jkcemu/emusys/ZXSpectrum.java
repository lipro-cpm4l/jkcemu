/*
 * (c) 2013-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des ZX Spectrum
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;
import jkcemu.audio.AudioIn;
import jkcemu.audio.AudioOut;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.zxspectrum.ZXSpectrum128KeyboardFld;
import jkcemu.emusys.zxspectrum.ZXSpectrum48KeyboardFld;
import jkcemu.etc.PSG8910;
import jkcemu.joystick.JoystickThread;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;


public class ZXSpectrum extends EmuSys implements
					PSG8910.Callback,
					Z80InterruptSource
{
  public static final String SYSNAME     = "ZXSpectrum";
  public static final String SYSTEXT     = "ZX Spectrum";
  public static final String PROP_PREFIX = "jkcemu.zxspectrum.";

  public static final String VALUE_128K = "128k";

  private static final int SCREEN_WIDTH           = 256;
  private static final int SCREEN_HEIGHT          = 192;
  private static final int DEFAULT_48K_KHZ        = 3500;
  private static final int DEFAULT_128K_KHZ       = 3547;
  private static final int MIN_TAPE_IN_1TO0_DELAY = 180;
  private static final int MAX_TAPE_IN_1TO0_DELAY = 2000;

  private static final int[][] kbMatrixNormal = {
			{   -1, 'z', 'x', 'c', 'v' },		// A8
			{  'a', 's', 'd', 'f', 'g' },		// A9
			{  'q', 'w', 'e', 'r', 't' },		// A10
			{  '1', '2', '3', '4', '5' },		// A11
			{  '0', '9', '8', '7', '6' },		// A12
			{  'p', 'o', 'i', 'u', 'y' },		// A13
			{ 0x0D, 'l', 'k', 'j', 'h' },		// A14
			{ 0x20,  -1, 'm', 'n', 'b' } };		// A15

  private static final int[][] kbMatrixSymbolShift = {
			{   -1, ':', '\u00A3',  '?', '/' },	// A8
			{  '~', '|',     '\\',  '{', '}' },	// A9
			{   -1,  -1,       -1,  '<', '>' },	// A10
			{  '!', '@',      '#',  '$', '%' },	// A11
			{  '_', ')',      '(', '\'', '&' },	// A12
			{ '\"', ';',       -1,  ']', '[' },	// A13
			{ 0x0D, '=',      '+',  '-', '^' },	// A14
			{ 0x20,  -1,      '.',  ',', '*' } };	// A15

  private static final int[][] kbMatrixControl = {
			{ -1,
			  KeyEvent.VK_Z,
			  KeyEvent.VK_X,
			  KeyEvent.VK_C,
			  KeyEvent.VK_V },			// A8
			{ KeyEvent.VK_A,
			  KeyEvent.VK_S,
			  KeyEvent.VK_D,
			  KeyEvent.VK_F,
			  KeyEvent.VK_G },			// A9
			{ KeyEvent.VK_Q,
			  KeyEvent.VK_W,
			  KeyEvent.VK_E,
			  KeyEvent.VK_R,
			  KeyEvent.VK_T },			// A10
			{ KeyEvent.VK_1,
			  KeyEvent.VK_2,
			  KeyEvent.VK_3,
			  KeyEvent.VK_4,
			  KeyEvent.VK_5 },			// A11
			{ KeyEvent.VK_0,
			  KeyEvent.VK_9,
			  KeyEvent.VK_8,
			  KeyEvent.VK_7,
			  KeyEvent.VK_6 },			// A12
			{ KeyEvent.VK_P,
			  KeyEvent.VK_O,
			  KeyEvent.VK_I,
			  KeyEvent.VK_U,
			  KeyEvent.VK_Y },			// A13
			{ KeyEvent.VK_ENTER,
			  KeyEvent.VK_L,
			  KeyEvent.VK_K,
			  KeyEvent.VK_J,
			  KeyEvent.VK_H },			// A14
			{ KeyEvent.VK_SPACE,
			  -1,
			  KeyEvent.VK_M,
			  KeyEvent.VK_N,
			  KeyEvent.VK_B } };			// A15

  private static byte[]              os48k            = null;
  private static byte[]              os128k           = null;
  private static Map<Long,Character> pixelCRC32ToChar = null;

  private Color[]             colors;
  private String              osFile;
  private volatile float      fTStatesPerLine;
  private volatile int        tStatesPerLine;
  private boolean             mode128k;
  private boolean             cfgRegProtected;
  private boolean             interruptRequested;
  private boolean             earPhase;
  private boolean             blinkState;
  private int                 blinkLineCounter;
  private int                 tapeOutBits;
  private int                 tapeInLDelayTStateCounter;
  private int                 joyActionMask;
  private int                 linesPerScreen;
  private int                 firstScreenLine;
  private int                 lastScreenLine;
  private int                 curScreenLine;
  private int                 screenTStateCounter;
  private int                 lineTStateCounter;
  private int                 borderColorNum;
  private int                 romOffs;
  private int                 ramC000Offs;
  private volatile int        screenOffs;
  private int                 psgRegNum;
  private int[]               kbMatrix;
  private byte[]              osBytes;
  private byte[]              borderColorNums;
  private byte[]              screenColorNums;
  private byte[]              ram;
  private PSG8910             psg;
  private AbstractKeyboardFld keyboardFld;


  public ZXSpectrum( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.colors          = new Color[ 16 ];
    this.kbMatrix        = new int[ 8 ];
    this.osFile          = null;
    this.osBytes         = null;
    this.keyboardFld     = null;
    this.cfgRegProtected = false;
    this.psgRegNum       = 0;
    this.romOffs         = 0;
    this.ramC000Offs     = 0;
    this.ram             = null;
    this.psg             = null;

    // emulierte Hardware konfigurieren
    this.mode128k = emulates128K( props );
    if( this.mode128k ) {
      this.screenOffs      = 5 * 0x4000;
      this.linesPerScreen  = 311;
      this.firstScreenLine = 63;
      this.ram             = this.emuThread.getExtendedRAM( 0x20000 );
      this.psg             = new PSG8910(
					DEFAULT_128K_KHZ * 500,
					AudioOut.MAX_UNSIGNED_USED_VALUE,
					this );
    } else {
      this.screenOffs      = 0x4000;
      this.linesPerScreen  = 312;
      this.firstScreenLine = 64;
    }
    this.lastScreenLine  = this.firstScreenLine + SCREEN_HEIGHT;
    this.borderColorNums = new byte[ this.linesPerScreen ];
    this.screenColorNums = new byte[ SCREEN_WIDTH * SCREEN_HEIGHT ];

    Z80CPU cpu = emuThread.getZ80CPU();
    cpu.setInterruptSources( this );
    cpu.addTStatesListener( this );
    cpu.addMaxSpeedListener( this );

    applySettings( props );
    z80MaxSpeedChanged( cpu );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    if( this.psg != null ) {
      this.psg.start();
    }
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return emulates128K( props ) ? DEFAULT_128K_KHZ : DEFAULT_48K_KHZ;
  }


  public boolean isMode128K()
  {
    return this.mode128k;
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    synchronized( this.kbMatrix ) {
      int n = Math.min( kbMatrix.length, this.kbMatrix.length );
      int i = 0;
      while( i < n ) {
	this.kbMatrix[ i ] = kbMatrix[ i ];
	i++;
      }
      while( i < this.kbMatrix.length ) {
	this.kbMatrix[ i ] = 0;
	i++;
      }
    }
  }


	/* --- PSG8910.Callback --- */

  @Override
  public int psgReadPort( PSG8910 psg, int port )
  {
    return 0xFF;
  }


  @Override
  public void psgWritePort( PSG8910 psg, int port, int value )
  {
    // leer
  }


  @Override
  public void psgWriteFrame( PSG8910 psg, int a, int b, int c )
  {
    int value = (a + b + c) / 6;
    this.emuThread.writeSoundOutFrames( 1, value, value, value );
  }


	/* --- Z80InterruptSource --- */

  /*
   * Die Hardware des ZX Spectrum reagiert nicht auf RETI-Befehle,
   * weshalb diese speziellen Interrupt-Return-Befehle
   * auch nicht vewendet werden.
   * Damit wird aber auch "interruptFinished()" nie aufgerufen.
   * Aus diesem Grund muss der Interrupt-Zustand mit der
   * Interrupt-Annahme bereits zurueckgesetzt werden, was bedeutet,
   * dass "isInterruptAccepted()" nie true zurueckliefern kann.
   */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<table border=\"1\">\n"
        + "<tr><td>Interrupt angemeldet:</td><td>" );
    buf.append( this.interruptRequested ? "ja" : "nein" );
    buf.append( "</td></tr>\n"
        + "</table>\n" );
  }


  @Override
  public int interruptAccept()
  {
    this.interruptRequested = false;
    return 0xFF;
  }


  @Override
  public void interruptFinish()
  {
    // leer
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return false;
  }


  @Override
  public boolean isInterruptRequested()
  {
    return this.interruptRequested;
  }


  @Override
  public void reset( boolean powerOn )
  {
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
    }
    Arrays.fill( this.borderColorNums, (byte) 0 );
    Arrays.fill( this.screenColorNums, (byte) 0 );
    this.earPhase                  = true;
    this.cfgRegProtected           = false;
    this.interruptRequested        = false;
    this.blinkState                = false;
    this.blinkLineCounter          = 0;
    this.tapeOutBits               = 0;
    this.tapeInLDelayTStateCounter = 0;
    this.joyActionMask             = 0;
    this.screenTStateCounter       = 0;
    this.lineTStateCounter         = 0;
    this.curScreenLine             = 0;
    this.borderColorNum            = 0;
    this.ramC000Offs               = 0;
    this.romOffs                   = 0;
    this.screenOffs                = (this.mode128k ? 5 : 1) * 0x4000;
    this.psgRegNum                 = 0;
    if( this.psg != null ) {
      this.psg.reset();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>ZX Spectrum Status</h1>\n"
	+ "<table border=\"1\">\n"
        + "<tr><td>Aktuelle Pixelzeile:</td><td>" );
    buf.append( this.curScreenLine );
    buf.append( "</td></tr>\n"
        + "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    createColors( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv && (emulates128K( props ) != this.mode128k) ) {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_PREFIX + PROP_FILE ) ) )
    {
      rv = false;
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    if( this.mode128k ) {
      this.keyboardFld = new ZXSpectrum128KeyboardFld( this );
    } else {
      this.keyboardFld = new ZXSpectrum48KeyboardFld( this );
    }
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.psg != null ) {
      this.psg.die();
    }
  }


  @Override
  public int getBorderColorIndex()
  {
    return this.borderColorNum;
  }


  @Override
  public int getBorderColorIndexByLine( int line )
  {
    line += this.firstScreenLine;
    if( (line < 0) || (line >= this.borderColorNums.length) ) {
      line = Math.abs( line ) % this.borderColorNums.length;
    }
    return this.borderColorNums[ line ];
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = null;
    if( this.colors != null ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) ) {
	color = this.colors[ colorIdx ];
      }
    }
    return color != null ? color : super.getColor( colorIdx );
  }


  @Override
  public int getColorCount()
  {
    return this.colors.length;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = 0;
    if( (x >= 0) && (x < this.SCREEN_WIDTH)
	&& (y >= 0) && (y < SCREEN_HEIGHT) )
    {
      rv = (int) this.screenColorNums[ (y * SCREEN_WIDTH) + x ] & 0x0F;
    }
    return rv;
  }


  @Override
  public boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( 32, 24, 8, 8, 8, 0 );
  }


  @Override
  public String getHelpPage()
  {
    return "/help/zxspectrum.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( addr < 0x4000 ) {
      if( this.osBytes != null ) {
	int idx = addr + this.romOffs;
	if( idx < this.osBytes.length ) {
	  rv = (int) this.osBytes[ idx ] & 0xFF;
	}
      }
    } else {
      if( this.ram != null ) {
	int idx = addr;
	if( (addr >= 0x4000) && (addr < 0x8000) ) {
	  idx += (4 * 0x4000);			// - 0x4000 + (5 * 0x4000)
	} else if( addr >= 0xC000 ) {
	  idx = addr - 0xC000 + this.ramC000Offs;
	}
	if( idx < this.ram.length ) {
	  rv = (int) (this.ram[ idx ] & 0xFF);
	}
      } else {
	rv = this.emuThread.getRAMByte( addr );
      }
    }
    return rv;
  }


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0x0000;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int rv = -1;
    if( (chX >= 0) && (chX < 32) && (chY >= 0) && (chY < 24) ) {
      Map<Long,Character> crc32ToChar = getPixelCRC32ToCharMap();
      if( crc32ToChar != null ) {
	CRC32 crc1 = new CRC32();
	CRC32 crc2 = new CRC32();
	int   addr = 0x4000
			| ((chY << 8) & 0x1800)
			| ((chY << 5) & 0x00E0)
			| (chX & 0x001F);
	for( int i = 0; i < 8; i++ ) {
	  int b = getMemByte( addr, false );
	  crc1.update( b );
	  crc2.update( ~b & 0xFF );
	  addr += 0x0100;
	}
	Character ch = crc32ToChar.get( crc1.getValue() );
	if( ch == null ) {
	  // Zeichen invers?
	  ch = crc32ToChar.get( crc2.getValue() );
	}
	if( ch != null ) {
	  rv = ch.charValue();
	}
      }
    }
    return rv;
  }


  @Override
  public int getScreenHeight()
  {
    return SCREEN_HEIGHT;
  }


  @Override
  public int getScreenWidth()
  {
    return SCREEN_WIDTH;
  }


  @Override
  public int getSupportedJoystickCount()
  {
    return 1;
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
    synchronized( this.kbMatrix ) {
      if( ctrlDown ) {
	if( shiftDown ) {
	  this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	  this.kbMatrix[ 7 ] |= 0x02;		// SYMBOL SHIFT
	  rv = true;
	} else {
	  if( setValueInKBMatrix( kbMatrixControl, keyCode ) ) {
	    this.kbMatrix[ 7 ] |= 0x02;		// SYMBOL SHIFT
	    rv = true;
	  }
	}
      } else {
	switch( keyCode ) {
	  case KeyEvent.VK_F1:			// EDIT
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 3 ] |= 0x01;		// 1
	    rv = true;
	    break;
	  case KeyEvent.VK_F2:			// CAPS LOCK
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 3 ] |= 0x02;		// 2
	    rv = true;
	    break;
	  case KeyEvent.VK_F3:			// TRUE VIDEO
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 3 ] |= 0x04;		// 3
	    rv = true;
	    break;
	  case KeyEvent.VK_F4:			// INV VIDEO
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 3 ] |= 0x08;		// 3
	    rv = true;
	    break;
	  case KeyEvent.VK_F5:
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    rv = true;
	    break;
	  case KeyEvent.VK_F6:
	    this.kbMatrix[ 7 ] |= 0x02;		// SYMBOL SHIFT
	    rv = true;
	    break;
	  case KeyEvent.VK_F9:			// GRAPHICS
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 4 ] |= 0x02;		// 9
	    rv = true;
	    break;
	  case KeyEvent.VK_BACK_SPACE:
	  case KeyEvent.VK_DELETE:
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 4 ] |= 0x01;		// 0
	    rv = true;
	    break;
	  case KeyEvent.VK_LEFT:
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 3 ] |= 0x10;		// 5
	    rv = true;
	    break;
	  case KeyEvent.VK_DOWN:
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 4 ] |= 0x10;		// 6
	    rv = true;
	    break;
	  case KeyEvent.VK_UP:
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 4 ] |= 0x08;		// 7
	    rv = true;
	    break;
	  case KeyEvent.VK_RIGHT:
	    this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	    this.kbMatrix[ 4 ] |= 0x04;		// 8
	    rv = true;
	    break;
	  case KeyEvent.VK_ENTER:
	    if( shiftDown ) {
	      this.kbMatrix[ 0 ] |= 0x01;	// CAPS SHIFT
	    }
	    this.kbMatrix[ 6 ] |= 0x01;
	    rv = true;
	    break;
	  case KeyEvent.VK_SPACE:
	    if( shiftDown ) {
	      this.kbMatrix[ 0 ] |= 0x01;	// CAPS SHIFT -> BREAK
	    }
	    this.kbMatrix[ 7 ] |= 0x01;
	    rv = true;
	    break;
	}
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
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
    }
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      rv = setValueInKBMatrix( kbMatrixNormal, ch );
      if( !rv && ch >= 'A' && ch <= 'Z' ) {
	rv = setValueInKBMatrix(
			kbMatrixNormal,
			Character.toLowerCase( ch ) );
	if( rv ) {
	  this.kbMatrix[ 0 ] |= 0x01;		// CAPS SHIFT
	}
      }
      if( !rv ) {
	rv = setValueInKBMatrix( kbMatrixSymbolShift, ch );
	if( rv ) {
	  this.kbMatrix[ 7 ] |= 0x02;		// SYMBOL SHIFT
	}
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    if( ch == '\u00A3' ) {			// Pfund-Zeichen
      ch = '\u0060';
    } else if( ch == '\u00A9' ) {		// Copyright-Zeichen
      ch = '\u007F';
    }
    return super.pasteChar( ch );
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( (port & 0x01) == 0 ) {
      /*
       * Die ersten 4 Takte des Eingabebefehls sind bereits abgearbeitet
       * und muessen deshalb mit beruecksichtigt werden.
       * Es kommt auch dann zu Wait-States, wenn der Wert
       * auf dem Adressbus nicht innerhalb von 4000h-7FFFh liegt.
       * Allerdings ist die Emulation der Wait-States in dem Fall
       * nicht ganz exakt.
       */
      checkInsertWaitStates( (port & 0x3FFF) | 0x4000, 4 );
      synchronized( this.kbMatrix ) {
	int invPort  = ~port;
	int addrMask = 0x0100;		// A8
	for( int i = 0; i < this.kbMatrix.length; i++ ) {
	  if( (invPort & addrMask) != 0 ) {
	    rv &= ~this.kbMatrix[ i ];
	  }
	  addrMask <<= 1;
	}
      }
      boolean running = false;
      AudioIn audioIn = this.emuThread.getTapeIn();
      if( audioIn != null ) {
	if( !audioIn.isPause() ) {
	  running = true;
	  if( !audioIn.readPhase() ) {
	    rv &= ~0x40;
	  }
	}
      }
      if( !running ) {
	/*
	 * Wenn am Mic-Eingang kein Pegel anliegt,
	 * wird das Bit vom EAR-Ausgangspegel beeinflusst:
         *   1 -> 0: Verzoegerung, abhangig von der Dauer
	 *           des vorherigen H-Pegels
	 *   0 -> 1: keine Verzoegerung
         */
	if( !this.earPhase && (this.tapeInLDelayTStateCounter <= 0) ) {
	  rv &= ~0x40;
	}
      }
    } else {
      if( (port & 0xFF) == 0x1F ) {
	// Kemston Interface I
	rv = 0;
	if( (this.joyActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	  rv |= 0x01;
	}
	if( (this.joyActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	  rv |= 0x02;
	}
	if( (this.joyActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	  rv |= 0x04;
	}
	if( (this.joyActionMask & JoystickThread.UP_MASK) != 0 ) {
	  rv |= 0x08;
	}
	if( (this.joyActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
	  rv |= 0x10;
	}
      }
      if( this.mode128k ) {
	if( (port & 0x8002) == 0x0000 ) {
	  // 7FFDh (A15=0, A1=0)
	  writeCfgReg( 0xFF );
	} else if( (port & 0xC002) == 0xC000 ) {
	  // FFFDh (A15=1, A14=1, A1=0)
	  if( this.psg != null ) {
	    rv = this.psg.getRegister( this.psgRegNum );
	  }
	}
      }
      checkInsertWaitStates( port, 4 );
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    /*
     * Wenn m1 gesetzt ist, wird davon ausgegangen,
     * dass das erste Byte eines Befehls gelesen wird.
     * In dem Fall sind keine zusaetzlichen Takte zu beruecksichtigen.
     * Ist m1 nicht gesetzt, wird davon ausgegangen,
     * dass bereits ein 1-Byte-Befehlscode verarbeitet wurde
     * und somit 4 Takte zu beruecksichtigen sind.
     * Mit diesen Annahmen wird zwar nicht bei allen Befehlen
     * die exakte Anzahl der Wait States emuliert,
     * aber bei einem grossen Teil.
     */
    checkInsertWaitStates( addr, m1 ? 0 : 4 );
    return getMemByte( addr, true );
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
        && isReloadExtROMsOnPowerOnEnabled( props ) )
    {
      loadROMs( props );
    }
  }


  @Override
  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( joyNum == 0 )
      this.joyActionMask = actionMask;
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( addr >= 0x4000 ) {
      if( this.ram != null ) {
	int idx = addr;
	if( (addr >= 0x4000) && (addr < 0x8000) ) {
	  idx += (4 * 0x4000);			// - 0x4000 + (5 * 0x4000)
	} else if( addr >= 0xC000 ) {
	  idx = idx - 0xC000 + this.ramC000Offs;
	}
	if( idx < this.ram.length ) {
	  this.ram[ idx ] = (byte) value;
	}
      } else {
	this.emuThread.setRAMByte( addr, value );
      }
      rv = true;
    }
    return rv;
  }


  @Override
  public void soundOutFrameRateChanged( int frameRate )
  {
    if( this.psg != null ) {
      this.psg.setFrameRate( frameRate );
    } else {
      super.soundOutFrameRateChanged( frameRate );
    }
  }


  @Override
  public boolean supportsBorderColorByLine()
  {
    return true;
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
  public boolean supportsPasteFromClipboard()
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
  public boolean supportsSoundOut8Bit()
  {
    return (this.psg != null);
  }


  @Override
  public boolean supportsSoundOutMono()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "ULA (Bildschirmsteuerung)";
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( (port & 0x01) == 0 ) {
      /*
       * Die ersten 4 Takte des Ausgabebefehls sind bereits abgearbeitet
       * und muessen deshalb mit beruecksichtigt werden.
       * Es kommt auch dann zu Wait-States, wenn der Wert
       * auf dem Adressbus nicht innerhalb von 4000h-7FFFh liegt.
       * Allerdings ist die Emulation der Wait-States in dem Fall
       * nicht ganz exakt.
       */
      checkInsertWaitStates( (port & 0x3FFF) | 0x4000, 4 );
      this.borderColorNum  = value & 0x07;
      int      tapeOutBits = (value & 0x18);
      boolean  earPhase    = ((value & 0x10) != 0);
      if( this.psg == null ) {
	this.soundOutPhase = earPhase;
      }
      if( tapeOutBits != this.tapeOutBits ) {
	this.tapeOutPhase = !this.tapeOutPhase;
      }
      this.tapeOutBits = tapeOutBits;
      /*
       * Emulation der Rueckkopplung vom EAR-Ausgang zum Eingang
       */
      if( earPhase != this.earPhase ) {
	if( earPhase ) {
	  this.tapeInLDelayTStateCounter = 0;
	} else {
	  if( this.tapeInLDelayTStateCounter < MIN_TAPE_IN_1TO0_DELAY ) {
	    this.tapeInLDelayTStateCounter = MIN_TAPE_IN_1TO0_DELAY;
	  }
	}
	this.earPhase = earPhase;
      }
    } else {
      if( this.mode128k ) {
	if( (port & 0x8002) == 0x0000 ) {
	  // 7FFDh (A15=0, A1=0)
	  writeCfgReg( value );
	} else if( (port & 0xC002) == 0x8000 ) {
	  // BFFDh (A15=0, A14=1, A1=0)
	  if( this.psg != null ) {
	    this.psg.setRegister( this.psgRegNum, value );
	  }
	} else if( (port & 0xC002) == 0xC000 ) {
	  // FFFDh (A15=1, A14=1, A1=0)
	  this.psgRegNum = value & 0x0F;
	}
      }
      checkInsertWaitStates( port, 4 );
    }
  }


  @Override
  public void writeMemByte( int addr, int value )
  {
    /*
     * Es wird davon ausgegamgen, dass bereits ein 1-Byte-Befehlscode
     * verarbeitet wurde und somit 4 Takte zu beruecksichtigen sind.
     * Mit dieser Annahme wird zwar nicht bei allen Schreibbefehlen
     * die exakte Anzahl der Wait States emuliert,
     * aber doch bei einem bedeutenden Teil.
     */
    checkInsertWaitStates( addr, 4 );
    setMemByte( addr, value );
  }


  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );
    if( mode128k ) {
      // 228 T-States bei normaler Taktfrequenz
      this.tStatesPerLine = (int) Math.round( cpu.getMaxSpeedKHz() / 15.557 );
    } else {
      // 224 T-States bei normaler Taktfrequenz
      this.tStatesPerLine = (int) Math.round( cpu.getMaxSpeedKHz() / 15.625 );
    }
    this.fTStatesPerLine = (float) this.tStatesPerLine;
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );

    this.lineTStateCounter += tStates;
    if( this.lineTStateCounter >= this.tStatesPerLine ) {
      this.lineTStateCounter -= this.tStatesPerLine;
      updScreenLine();
      this.curScreenLine++;
      if( this.curScreenLine >= this.linesPerScreen ) {
	this.curScreenLine      = 0;
	this.interruptRequested = true;
	this.blinkLineCounter++;
	if( this.blinkLineCounter >= 16 ) {
	  this.blinkLineCounter = 0;
	  this.blinkState = !this.blinkState;
	}
      }
    }
    if( this.earPhase ) {
      if( this.tapeInLDelayTStateCounter < MAX_TAPE_IN_1TO0_DELAY ) {
	this.tapeInLDelayTStateCounter += (tStates / 2);
      }
    } else {
      if( this.tapeInLDelayTStateCounter > 0 ) {
	this.tapeInLDelayTStateCounter -= tStates;
      }
    }
  }


	/* --- private Methoden --- */

  private void checkInsertWaitStates( int addr, int diffTStates )
  {
    addr &= 0xFFFF;
    if( (((addr >= 0x4000) && (addr < 0x8000))
	 || (this.mode128k && (addr >= 0xC000)
	     && ((this.ramC000Offs & 0x4000) != 0)))
	&& (this.curScreenLine >= this.firstScreenLine)
	&& (this.curScreenLine <= this.lastScreenLine) )
    {
      int t = Math.round(
		(float) (this.lineTStateCounter + diffTStates)
				/ this.fTStatesPerLine * 224F );
      t -= 63;		// ein TState vor sichtbaren Bereich
      if( (t >= 0) && (t < 128) ) {
	int waitStates = 6 - (t % 8);
	if( waitStates > 0 ) {
	  this.emuThread.getZ80CPU().addWaitStates( waitStates );
	}
      }
    }
  }


  private void createColors( Properties props )
  {
    float f = getBrightness( props );
    if( (f >= 0F) && (f <= 1F) ) {
      for( int i = 0; i < this.colors.length; i++ ) {
	int v = Math.round( f * (float) ((i & 0x08) != 0 ? 255 : 192) );
	int r = ((i & 0x02) != 0 ? v : 0);
	int g = ((i & 0x04) != 0 ? v : 0);
	int b = ((i & 0x01) != 0 ? v : 0);
	this.colors[ i ] = new Color( r, g, b );
      }
    }
  }


  private static boolean emulates128K( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			PROP_PREFIX + PROP_MODEL ).equals( VALUE_128K );
  }


  private Map<Long,Character> getPixelCRC32ToCharMap()
  {
    if( pixelCRC32ToChar == null ) {
      byte[] os48kBytes = getOS48kBytes();
      if( os48kBytes != null ) {
	if( os48kBytes.length >= 0x4000 ) {
	  Map<Long,Character> map  = new HashMap<>();
	  CRC32               crc  = new CRC32();
	  int                 addr = 0x3D00;
	  for( int c = 0x20; c <= 0x7F; c++ ) {
	    crc.reset();
	    crc.update( os48kBytes, addr, 8 );
	    char ch = (char) c;
	    if( c == 0x60 ) {
	      ch = '\u00A3';			// Pfund-Zeichen
	    } else if( c == 0x7F ) {
	      ch = '\u00A9';			// Copyright-Zeichen
	    }
	    map.put( crc.getValue(), Character.valueOf( ch ) );
	    addr += 8;
	  }
	  pixelCRC32ToChar = map;
	}
      }
    }
    return pixelCRC32ToChar;
  }


  private byte[] getOS48kBytes()
  {
    if( os48k == null ) {
      os48k = readResource( "/rom/zxspectrum/os48k.bin" );
    }
    return os48k;
  }


  private byte[] getOS128kBytes()
  {
    if( os128k == null ) {
      os128k = readResource( "/rom/zxspectrum/os128k.bin" );
    }
    return os128k;
  }


  private void loadROMs( Properties props )
  {
    this.osFile  = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_PREFIX + PROP_FILE );
    this.osBytes = readROMFile(
			this.osFile,
			this.mode128k ? 0x8000 : 0x4000,
			"ROM" );
    if( osBytes == null ) {
      this.osBytes = (this.mode128k ? getOS128kBytes() : getOS48kBytes());
    }
  }


  private boolean setValueInKBMatrix( int[][] kbMatrix, int value )
  {
    boolean rv = false;
    int     n  = Math.min( kbMatrix.length, this.kbMatrix.length );
    for( int i = 0; i < n; i++ ) {
      int   m   = 0x01;
      int[] col = kbMatrix[ i ];
      for( int k = 0; k < col.length; k++ ) {
	if( col[ k ] == value ) {
	  this.kbMatrix[ i ] |= m;
	  rv = true;
	}
	m <<= 1;
      }
    }
    return rv;
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.kbMatrix );
  }


  private void updScreenLine()
  {
    int screenLine = this.curScreenLine;
    int pixelLine  = screenLine - this.firstScreenLine;
    if( (pixelLine >= 0) && (pixelLine < SCREEN_HEIGHT) ) {
      int dstPos    = pixelLine * SCREEN_WIDTH;
      int charRow   = pixelLine / 8;
      int attrAddr  = this.screenOffs + 0x1800 + (charRow * 32);
      int pixelAddr = this.screenOffs
			| ((pixelLine << 5) & 0x1800)
			| ((pixelLine << 2) & 0x00E0)
			| ((pixelLine << 8) & 0x0700);
      for( int i = 0; i < 32; i++ ) {
	int     attr   = 0;
	int     pixels = 0;
	if( this.ram != null ) {
	  if( attrAddr < this.ram.length ) {
	    attr = (int) this.ram[ attrAddr++ ] & 0xFF;
	  }
	  if( pixelAddr < this.ram.length ) {
	    pixels = (int) this.ram[ pixelAddr++ ] & 0xFF;
	  }
	} else {
	  attr   = getMemByte( attrAddr++, false );
	  pixels = getMemByte( pixelAddr++, false );
	}
	boolean blink  = ((attr & 0x80) != 0);
	boolean bright = ((attr & 0x40) != 0);
	for( int k = 0; k < 8; k++ ) {
	  boolean fg = (blink && this.blinkState);
	  if( (pixels & 0x80) != 0 ) {
	    fg = !fg;
	  }
	  int colorNum = 0;
	  if( fg ) {
	    colorNum = attr & 0x07;
	    if( bright ) {
	      colorNum |= 0x08;
	    }
	  } else {
	    colorNum = (attr >> 3) & 0x07;
	  }
	  this.screenColorNums[ dstPos++ ] = (byte) colorNum;
	  this.screenFrm.setScreenDirty( true );
	  pixels <<= 1;
	}
      }
    }
    if( (screenLine >= 0) && (screenLine < this.borderColorNums.length) ) {
      this.borderColorNums[ screenLine ] = (byte) this.borderColorNum;
    }
  }


  private void writeCfgReg( int value )
  {
    if( this.mode128k && !this.cfgRegProtected ) {
      this.ramC000Offs     = 0x4000 * (value & 0x07);
      this.screenOffs      = ((value & 0x08) != 0 ? 7 : 5) * 0x4000;
      this.romOffs         = ((value & 0x10) != 0 ? 0x4000 : 0x0000);
      this.cfgRegProtected = ((value & 0x20) != 0);
    }
  }
}
