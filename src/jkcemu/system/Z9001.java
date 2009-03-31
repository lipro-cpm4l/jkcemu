/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z9001 und Nachfolger (KC85/1, KC87)
 */

package jkcemu.system;

import java.awt.Color;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class Z9001 extends EmuSys implements
					ActionListener,
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  public static final int MEM_BASIC = 0xC000;
  public static final int MEM_COLOR = 0xE800;
  public static final int MEM_VIDEO = 0xEC00;
  public static final int MEM_OS    = 0xF000;

  private static final String[] biosCallNames = {
			"INIT",  "WBOOT", "CONST", "CONIN",
			"COOUT", "LIST",  "PUNCH", "READER",
			"GSTIK", "BOSER", "STIME", "GTIME",
			"SDMA",  "READ",  "WRITE", "LLIST",
			"GCURS", "SCURS", "BOSER", "GIOBY",
			"SIOBY", "GMEM",  "SMEM" };

  private static final int basicRGBValues[][] = {
				{ 0,   0,   0 },
				{ 255, 0,   0 },
				{ 0,   255, 0 },
				{ 255, 255, 0 },
				{ 0,   0,   255 },
				{ 255, 0,   255 },
				{ 0,   255, 255 },
				{ 255, 255, 255 } };

  private static int[][] kbMatrixNormal = {
		{ '0', '1', '2', '3', '4', '5', '6', '7' },
		{ '8', '9', ':', ';', ',', '=', '.', '?' },
		{ '@', 'A', 'B',  'C', 'D', 'E', 'F', 'G'  },
		{ 'H', 'I', 'J',  'K', 'L', 'M', 'N', 'O'  },
		{ 'P', 'Q', 'R',  'S', 'T', 'U', 'V', 'W'  },
		{ 'X', 'Y', 'Z',  0,   0,   0,   '^', 0    } };

  private static int[][] kbMatrixShift = {
		{ '_', '!', '\"', '#', '$', '%', '&', '\'' },
		{ '(', ')', '*',  '+', '<', '-', '>', '/'  },
		{ 0,   'a', 'b', 'c', 'd', 'e', 'f', 'g' },
		{ 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o' },
		{ 'p', 'q', 'r', 's', 't', 'u', 'v', 'w' },
		{ 'x', 'y', 'z', 0,   0,   0  , 0, 0   } };

  private static byte[] os12           = null;
  private static byte[] os13           = null;
  private static byte[] basic86        = null;
  private static byte[] z9001FontBytes = null;

  private byte[]            romBASIC;
  private byte[]            romOS;
  private byte[]            ramColor;
  private byte[]            ramVideo;
  private int               ramSize;
  private boolean           kc87;
  private boolean           mode20Rows;
  private boolean           colorMode;
  private boolean           colorSwap;
  private boolean           audioOutPhase;
  private boolean           audioInPhase;
  private int               audioInTStates;
  private int               lineNum;
  private int               lineTStates;
  private int               tStatesPerLine;
  private int               tStatesVisible;
  private int               borderColorIdx;
  private int[]             kbMatrix;
  private Z80PIO            pio90;
  private Z80PIO            pio88;
  private Z80CTC            ctc80;
  private javax.swing.Timer blinkTimer;
  private Color[]           colors;


  public Z9001( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( z9001FontBytes == null ) {
      z9001FontBytes = readResource( "/rom/z9001/z9001font.bin" );
    }
    String sysText = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysText.equals( "KC87" ) ) {
      if( basic86 == null ) {
        basic86 = readResource( "/rom/z9001/basic86.bin" );
      }
      if( os13 == null ) {
        os13 = readResource( "/rom/z9001/os13.bin" );
      }
      this.romBASIC = basic86;
      this.romOS    = os13;
      this.kc87     = true;
    } else {
      if( os12 == null ) {
        os12 = readResource( "/rom/z9001/os12.bin" );
      }
      this.romBASIC = null;
      this.romOS    = os12;
      this.kc87     = false;
    }
    this.ramVideo       = new byte[ 0x0400 ];
    this.ramColor       = new byte[ 0x0400 ];
    this.ramSize        = getRAMSize( props );
    this.lineNum        = 0;
    this.lineTStates    = 0;
    this.audioInTStates = 0;
    this.audioInPhase   = this.emuThread.readAudioPhase();
    this.audioOutPhase  = false;
    this.mode20Rows     = false;
    this.colorMode      = getColorMode( props );
    this.colorSwap      = false;
    this.borderColorIdx = 0;
    this.colors         = new Color[ basicRGBValues.length ];
    createColors( props );

    this.kbMatrix = new int[ 8 ];
    Arrays.fill( this.kbMatrix, 0 );

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio90 = new Z80PIO( cpu );
    this.pio88 = new Z80PIO( cpu );
    this.ctc80 = new Z80CTC( cpu );
    cpu.setInterruptSources( this.pio90, this.pio88, this.ctc80 );

    this.ctc80.addCTCListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    if( this.colorMode ) {
      this.blinkTimer = new javax.swing.Timer( 200, this );
      this.blinkTimer.start();
    } else {
      this.blinkTimer = null;
    }
    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged();
  }


  public static int getDefaultSpeedKHz()
  {
    return 2458;	// eigentlich 2,4576 MHz
  }


	/* --- ActionListener --- */

  public void actionPerformed( ActionEvent e )
  {
    if( (e.getSource() == this.blinkTimer)
	&& !this.emuThread.getZ80CPU().isPause() )
    {
      this.colorSwap = !this.colorSwap;
      this.screenFrm.setScreenDirty( true );
    }
  }


	/* --- Z80MaxSpeedListener --- */

  public void z80MaxSpeedChanged()
  {
    this.tStatesPerLine = this.emuThread.getZ80CPU().getMaxSpeedKHz()
								* 20 / 312;
    this.tStatesVisible = (int) Math.round( this.tStatesPerLine / 2 );
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc80 ) {
      switch( timerNum ) {
	case 0:
	  this.audioOutPhase = !this.audioOutPhase;
	  if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
	    if( (this.pio88.fetchOutValuePortA( false ) & 0x80) != 0 ) {
	      this.emuThread.writeAudioPhase( this.audioOutPhase );
	    }
	  } else {
	    this.emuThread.writeAudioPhase( this.audioOutPhase );
	  }
	  break;

	case 2:
	  // Verbindung von Ausgang 2 auf Eingang 3 emulieren
	  ctc.externalUpdate( 3, 1 );
	  break;
      }
    }
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc80.systemUpdate( tStates );
    this.audioInTStates += tStates;

    /*
     * Der Kassettenrecorderanschluss eingangsseitig wird emuliert,
     * indem zyklisch geschaut wird, ob sich die Eingangsphase geaendert hat.
     * Wenn ja, wird ein Impuls an der Strobe-Leitung der zugehoerigen PIO
     * emuliert.
     * Auf der einen Seite soll das Audiosystem nicht zu oft abgefragt
     * werden.
     * Auf der anderen Seite sollen aber die Zykluszeit nicht so gross werden,
     * dass die Genauigkeit der Zeitmessung kuenstlich verschlechert wird.
     * Aus diesem Grund werden genau soviele Taktzyklen abgezaehlt,
     * wie auch der Vorteile der CTC mindestens zaehlen muss.
     */
    if( this.audioInTStates > 15 ) {
      this.audioInTStates = 0;
      if( this.emuThread.readAudioPhase() != this.audioInPhase ) {
	this.audioInPhase = !this.audioInPhase;

	/*
	 * Bei jedem Phasenwechsel wird ein Impuls an ASTB erzeugt,
	 * was je nach Betriebsart der PIO eine Ein- oder Ausgabe bedeutet
	 * und, das ist das eigentliche Ziel, einen Interrupt ausloest.
	 */
	switch( this.pio88.getModePortA() ) {
	  case Z80PIO.MODE_BYTE_IN:
	    this.pio88.putInValuePortA( 0xFF, true );
	    break;

	  case Z80PIO.MODE_BYTE_INOUT:
	  case Z80PIO.MODE_BYTE_OUT:
	    this.pio88.fetchOutValuePortA( true );
	    break;
	}
      }
    }

    // Zugriffe auf den Bildwiederhol- und Farbspeicher verlangsamen
    if( (this.tStatesPerLine > 0) && (this.tStatesVisible > 0) ) {
      this.lineTStates += tStates;
      if( this.lineTStates >= this.tStatesPerLine ) {
	this.lineTStates %= this.tStatesPerLine;
	if( this.lineNum < 311 ) {
	  this.lineNum++;
	} else {
	  this.lineNum = 0;
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void applySettings( Properties props )
  {
    super.applySettings( props );
    createColors( props );
  }


  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    if( this.blinkTimer != null ) {
      this.blinkTimer.stop();
    }
    this.ctc80.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeMaxSpeedListener( this );
    cpu.removeTStatesListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getBorderColorIndex()
  {
    return this.borderColorIdx;
  }


  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    if( this.colorMode ) {
      if( (colorIdx >= 0) && (colorIdx < colors.length) ) {
	color = colors[ colorIdx ];
      }
    } else {
      if( colorIdx > 0 ) {
	color = Color.white;
      }
    }
    return color;
  }


  public int getColorCount()
  {
    return this.colorMode ? 8 : 2;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv        = 0;
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      fontBytes = z9001FontBytes;
    }
    if( fontBytes != null ) {
      int col  = x / 8;
      int row  = 0;
      int yChr = 0;
      int rMax = 0;
      if( this.mode20Rows ) {
	row  = y / 9;
	yChr = y % 9;
	rMax = 20;
      } else {
	row  = y / 8;
	yChr = y % 8;
	rMax = 24;
      }
      if( (yChr < 8) && (row < rMax) ) {
	int offs = (row * 40) + col;
	int idx  = (this.emuThread.getMemByte( MEM_VIDEO + offs ) * 8) + yChr;
	if( (idx >= 0) && (idx < fontBytes.length ) ) {
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  if( (fontBytes[ idx ] & m) != 0 ) {
	    rv = 1;
	  }
	}
	if( this.colorMode ) {
	  int colorInfo = this.emuThread.getMemByte( MEM_COLOR + offs );
	  if( ((colorInfo & 0x80) != 0) && this.colorSwap ) {
	    rv = (rv != 0 ? 0 : 1);
	  }
	  if( rv != 0 ) {
	    rv = (colorInfo >> 4) & 0x07;
	  } else {
	    rv = colorInfo & 0x07;
	  }
	}
      } else {
	rv = this.borderColorIdx;
      }
    }
    return rv;
  }


  public int getCharColCount()
  {
    return 40;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return this.mode20Rows ? 20 : 24;
  }


  public int getCharRowHeight()
  {
    return this.mode20Rows ? 9 : 8;
  }


  public int getCharWidth()
  {
    return 8;
  }


  public String getHelpPage()
  {
    return "/help/z9001.htm";
  }


  public int getMemByte( int addr )
  {
    return getMemByteInternal( addr, false );
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return MEM_OS;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * 40) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
        ch = b;
      }
    }
    return ch;
  }


  public int getScreenHeight()
  {
    return 192;
  }


  public int getScreenWidth()
  {
    return 320;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getTitle()
  {
    return this.kc87 ? "KC87" : "KC85/1 (Z9001)";
  }


  public boolean hasKCBasicInROM()
  {
    return this.romBASIC != null;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      switch( keyCode ) {
	case KeyEvent.VK_BACK_SPACE:
	  this.kbMatrix[ 0 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_LEFT:
	  if( shiftDown ) {
	    this.kbMatrix[ 3 ] = 0x20;
	  } else {
	    this.kbMatrix[ 0 ] = 0x40;
	  }
	  rv = true;
	  break;

	case KeyEvent.VK_RIGHT:
	  if( shiftDown ) {
	    this.kbMatrix[ 3 ] = 0x20;
	  } else {
	    this.kbMatrix[ 0 ] = 0x80;		// Shift
	    this.kbMatrix[ 1 ] = 0x40;
	  }
	  rv = true;
	  break;

	case KeyEvent.VK_DOWN:
	  this.kbMatrix[ 2 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_UP:
	  this.kbMatrix[ 3 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_ESCAPE:
	  if( shiftDown ) {
	    this.kbMatrix[ 0 ] = 0x80;		// Shift
	  }
	  this.kbMatrix[ 4 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_ENTER:
	  this.kbMatrix[ 5 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_SPACE:
	  this.kbMatrix[ 7 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_INSERT:
	  if( shiftDown ) {
	    this.kbMatrix[ 0 ] = 0x80;		// Shift
	  }
	  this.kbMatrix[ 5 ] = 0x20;
	  rv = true;
	  break;

	case KeyEvent.VK_DELETE:
	  this.kbMatrix[ 0 ] = 0x80;		// Shift
	  this.kbMatrix[ 5 ] = 0x20;
	  rv = true;
	  break;

	case KeyEvent.VK_PAUSE:
	  if( shiftDown ) {
	    this.kbMatrix[ 0 ] = 0x80;		// Shift
	  }
	  this.kbMatrix[ 4 ] = 0x20;
	  rv = true;
	  break;

	case KeyEvent.VK_F1:			// Z9001-Taste GRAPHIC
	  this.kbMatrix[ 3 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F2:			// Z9001-Taste COLOR
	  this.kbMatrix[ 1 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F3:			// Z9001-Taste LIST
	  this.kbMatrix[ 4 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F4:			// Z9001-Taste RUN
	  this.kbMatrix[ 5 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F5:			// Z9001-Taste STOP
	  this.kbMatrix[ 6 ] = 0x40;
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      putKeyboardMatrixValuesToPorts();
    }
    return rv;
  }


  public void keyReleased()
  {
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
    }
    putKeyboardMatrixValuesToPorts();
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      if( (ch >= 1) && (ch <='\u0020') ) {
	if( setCharInKBMatrix( ch + 0x40, kbMatrixShift ) ) {
	  this.kbMatrix[ 2 ] |= 0x80;	// Control
	  rv = true;
	}
      } else {
	if( setCharInKBMatrix( ch, kbMatrixNormal ) ) {
	  rv = true;
	} else {
	  if( setCharInKBMatrix( ch, kbMatrixShift ) ) {
	    this.kbMatrix[ 0 ] |= 0x80;	// Shift
	    rv = true;
	  }
	}
      }
    }
    if( rv ) {
      putKeyboardMatrixValuesToPorts();
    }
    return rv;
  }


  public void openBasicProgram()
  {
    int begAddr = 0x0401;
    if( !this.kc87 ) {
      begAddr = askKCBasicBegAddr();
    }
    if( begAddr >= 0 )
      SourceUtil.openKCBasicProgram( this.screenFrm, begAddr );
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    switch( port & 0xFB ) {	// Adressleitung A2 nicht benutzt
      case 0x88:
	rv = this.pio88.readPortA();
	break;

      case 0x89:
	rv = this.pio88.readPortB();
	break;

      case 0x8A:
	rv = this.pio88.readControlA();
	break;

      case 0x8B:
	rv = this.pio88.readControlB();
	break;

      case 0x90:
	rv = this.pio90.readPortA();
	break;

      case 0x91:
	rv = this.pio90.readPortB();
	break;

      case 0x92:
	rv = this.pio90.readControlA();
	break;

      case 0x93:
	rv = this.pio90.readControlB();
	break;

      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	rv = this.ctc80.read( port & 0x03 );
	break;
    }
    return rv;
  }


  public int readMemByte( int addr )
  {
    return getMemByteInternal( addr, true );
  }


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return reassSysCallTable(
			addr,
			0xF000,
			biosCallNames,
			buf,
			colMnemonic,
			colArgs,
			colRemark );
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System,
   * der Farbmodus oder die RAM-Groesse aendert
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv      = true;
    String  sysText = EmuUtil.getProperty( props, "jkcemu.system" );
    if( (sysText.length() == 0) || sysText.equals( "KC85/1" ) ) {
      if( !this.kc87 ) {
	rv = false;
      }
    } else if( sysText.equals( "KC87" ) ) {
      if( this.kc87 ) {
	rv = false;
      }
    }
    if( !rv ) {
      if( getColorMode( props ) != this.colorMode ) {
	rv = true;
      }
    }
    if( !rv ) {
      if( getRAMSize( props ) != this.ramSize ) {
	rv = true;
      }
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramColor );
      fillRandom( this.ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc80.reset( true );
      this.pio88.reset( true );
      this.pio90.reset( true );
    } else {
      this.ctc80.reset( false );
      this.pio88.reset( false );
      this.pio90.reset( false );
    }
    updScreenConfig( 0 );
  }


  public void saveBasicProgram()
  {
    int begAddr = 0x0401;
    if( !this.kc87 ) {
      begAddr = askKCBasicBegAddr();
    }
    if( begAddr >= 0 )
      SourceUtil.saveKCBasicProgram( this.screenFrm, begAddr );
  }


  public boolean setMemByte( int addr, int value )
  {
    return setMemByteInternal( addr, value, false );
  }


  public void updSysCells(
			int    begAddr,
			int    len,
			Object fileFmt,
			int    fileType )
  {
    SourceUtil.updKCBasicSysCells(
			this.emuThread,
			begAddr,
			len,
			fileFmt,
			fileType );
  }


  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFB ) {	// Adressleitung A2 nicht benutzt
      case 0x88:
	this.pio88.writePortA( value );
	int v = this.pio88.fetchOutValuePortA( false );
	updScreenConfig( v );
	if( this.emuThread.isLoudspeakerEmulationEnabled()
	    && ((v & 0x80) != 0) )
	{
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	}
	break;

      case 0x89:
	this.pio88.writePortB( value );
	break;

      case 0x8A:
	this.pio88.writeControlA( value );
	break;

      case 0x8B:
	this.pio88.writeControlB( value );
	break;

      case 0x90:
	this.pio90.writePortA( value );		// Tastatur Spalten
	this.pio90.putInValuePortB( getKeyboardRowValue(), 0xFF );
	break;

      case 0x91:
	this.pio90.writePortB( value );		// Tastatur Zeilen
	this.pio90.putInValuePortA( getKeyboardColValue(), 0xFF );
	break;

      case 0x92:
	this.pio90.writeControlA( value );
	break;

      case 0x93:
	this.pio90.writeControlB( value );
	break;

      case 0x80:
      case 0x81:
      case 0x82:
      case 0x83:
	this.ctc80.write( port & 0x03, value );
	break;
    }
  }


  public void writeMemByte( int addr, int value )
  {
    setMemByteInternal( addr, value, true );
  }


	/* --- private Methoden --- */

  private void adjustVideoRAMAccessTStates()
  {
    if( (this.lineNum < 192)
	&& (this.tStatesPerLine > 0)
	&& (this.tStatesVisible > 0)
	&& (this.lineTStates < this.tStatesVisible) )
    {
      this.emuThread.getZ80CPU().addWaitStates(
			this.tStatesVisible - this.lineTStates );
    }
  }


  private void createColors( Properties props )
  {
    double brightness = getBrightness( props );
    if( (brightness >= 0.0) && (brightness <= 1.0) ) {
      for( int i = 0; i < this.colors.length; i++ ) {
	this.colors[ i ] = new Color(
		(int) Math.round( basicRGBValues[ i ][ 0 ] * brightness ),
		(int) Math.round( basicRGBValues[ i ][ 1 ] * brightness ),
		(int) Math.round( basicRGBValues[ i ][ 2 ] * brightness ) );
      }
    }
  }


  private boolean getColorMode( Properties props )
  {
    return EmuUtil.getBooleanProperty( props, "jkcemu.z9001.color", true );
  }


  private int getKeyboardColValue()
  {
    int rv       = 0;
    int rowValue = ~this.pio90.fetchOutValuePortB( false );
    int mask     = 0x01;
    synchronized( this.kbMatrix ) {
      for( int i = 0; i < this.kbMatrix.length; i++ ) {
	if( (rowValue & this.kbMatrix[ i ]) != 0 ) {
	  rv |= mask;
	}
	mask <<= 1;
      }
    }
    return ~rv & 0xFF;
  }


  private int getKeyboardRowValue()
  {
    int colValue = ~this.pio90.fetchOutValuePortA( false );
    int rowValue = 0;
    int mask     = 0x01;
    synchronized( this.kbMatrix ) {
      for( int i = 0; i < this.kbMatrix.length; i++ ) {
	if( (colValue & mask) != 0 ) {
	  rowValue |= this.kbMatrix[ i ];
	}
	mask <<= 1;
      }
    }
    return ~rowValue & 0xFF;
  }


  private int getMemByteInternal( int addr, boolean emuWaitStates )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( this.romBASIC != null ) {
      if( (addr >= MEM_BASIC) && (addr < MEM_BASIC + this.romBASIC.length) ) {
	rv   = (int) this.romBASIC[ addr - MEM_BASIC ] & 0xFF;
	done = true;
      }
    }
    if( !done && (this.romOS != null) ) {
      if( (addr >= MEM_OS) && (addr < MEM_OS + this.romOS.length) ) {
	rv   = (int) this.romOS[ addr - MEM_OS ] & 0xFF;
	done = true;
      }
    }
    if( this.colorMode
	&& (addr >= MEM_COLOR) && (addr < MEM_COLOR + this.ramColor.length) )
    {
      rv   = (int) this.ramColor[ addr - MEM_COLOR ] & 0xFF;
      done = true;
      if( emuWaitStates ) {
	adjustVideoRAMAccessTStates();
      }
    }
    if( (addr >= MEM_VIDEO) && (addr < MEM_VIDEO + this.ramVideo.length) ) {
      rv   = (int) this.ramVideo[ addr - MEM_VIDEO ] & 0xFF;
      done = true;
      if( emuWaitStates ) {
	adjustVideoRAMAccessTStates();
      }
    }
    if( !done && (addr < this.ramSize) ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  private static int getRAMSize( Properties props )
  {
    int    ramSize = 0x4000;
    String ramText = EmuUtil.getProperty( props, "jkcemu.z9001.ram.kbyte" );
    if( ramText.equals( "32" ) ) {
      ramSize = 0x8000;
    }
    else if( ramText.equals( "48" ) ) {
      ramSize = 0xC000;
    }
    return ramSize;
  }


  private void putKeyboardMatrixValuesToPorts()
  {
    this.pio90.putInValuePortB( getKeyboardRowValue(), 0xFF );
    this.pio90.putInValuePortA( getKeyboardColValue(), 0xFF );
  }


  private boolean setCharInKBMatrix( int ch, int[][] matrixChars )
  {
    boolean rv   = false;
    int     mask = 1;
    for( int row = 0; row < matrixChars.length; row++ ) {
      for( int col = 0;
	   (col < matrixChars[ row ].length) && (col < this.kbMatrix.length);
	   col++ )
      {
	if( matrixChars[ row ][ col ] == ch ) {
	  this.kbMatrix[ col ] = mask;
	  rv = true;
	  break;
	}
      }
      mask <<= 1;
    }
    return rv;
  }


  private boolean setMemByteInternal(
				int     addr,
				int     value,
				boolean emuWaitStates )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( this.colorMode
	&& (addr >= MEM_COLOR) && (addr < MEM_COLOR + this.ramColor.length) )
    {
      this.ramColor[ addr - MEM_COLOR ] = (byte) value;
      this.screenFrm.setScreenDirty( true );
      rv = true;
      if( emuWaitStates ) {
	adjustVideoRAMAccessTStates();
      }
    }
    if( (addr >= MEM_VIDEO) && (addr < MEM_VIDEO + this.ramVideo.length) ) {
      this.ramVideo[ addr - MEM_VIDEO ] = (byte) value;
      this.screenFrm.setScreenDirty( true );
      rv = true;
      if( emuWaitStates ) {
	adjustVideoRAMAccessTStates();
      }
    }
    else if( addr < this.ramSize ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  private void updScreenConfig( int value )
  {
    boolean mode20Rows = ((value & 0x04) != 0);
    int     colorIdx   = (value >> 3) & 0x07;
    if( (mode20Rows != this.mode20Rows)
	|| (colorIdx != this.borderColorIdx) )
    {
      this.mode20Rows     = mode20Rows;
      this.borderColorIdx = colorIdx;
      this.screenFrm.setScreenDirty( true );
    }
  }
}

