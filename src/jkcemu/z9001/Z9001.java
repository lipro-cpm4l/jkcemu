/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z9001 und Nachfolger (KC85/1, KC87)
 */

package jkcemu.z9001;

import java.awt.Color;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class Z9001 extends EmuSys implements
					ActionListener,
					Z80CTCListener,
					Z80TStatesListener
{
  public static final int MEM_BASIC = 0xC000;
  public static final int MEM_COLOR = 0xE800;
  public static final int MEM_VIDEO = 0xEC00;
  public static final int MEM_OS    = 0xF000;

  private static Color[] colors = {
				Color.black,
				Color.red,
				Color.green,
				Color.yellow,
				Color.blue,
				new Color( 255, 0, 255 ),
				new Color( 0, 255, 255 ),
				Color.white };

  private static int[][] kbMatrixNormal = {
		{ '0', '1', '2', '3', '4', '5', '6', '7' },
		{ '8', '9', ':', ';', ',', '=', '.', '?' },
		{ '@', 'a', 'b', 'c', 'd', 'e', 'f', 'g' },
		{ 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o' },
		{ 'p', 'q', 'r', 's', 't', 'u', 'v', 'w' },
		{ 'x', 'y', 'z', 0,   0,   0  , '^', 0   } };

  private static int[][] kbMatrixShift = {
		{ '_', '!', '\"', '#', '$', '%', '&', '\'' },
		{ '(', ')', '*',  '+', '<', '-', '>', '/'  },
		{ 0,   'A', 'B',  'C', 'D', 'E', 'F', 'G'  },
		{ 'H', 'I', 'J',  'K', 'L', 'M', 'N', 'O'  },
		{ 'P', 'Q', 'R',  'S', 'T', 'U', 'V', 'W'  },
		{ 'X', 'Y', 'Z',  0,   0,   0,   0,   0    } };

  private static byte[] os12      = null;
  private static byte[] os13      = null;
  private static byte[] basic86   = null;
  private static byte[] fontBytes = null;
  private static byte[] ramColor  = null;
  private static byte[] ramVideo  = null;

  private byte[]            romBASIC;
  private byte[]            romOS;
  private int               ramSize;
  private boolean           kc87;
  private boolean           mode20Rows;
  private boolean           colorMode;
  private boolean           colorSwap;
  private boolean           audioOutPhase;
  private boolean           audioInPhase;
  private int               audioInTStates;
  private int               borderColorIdx;
  private int[]             kbMatrix;
  private Z80PIO            pio90;
  private Z80PIO            pio88;
  private Z80CTC            ctc80;
  private javax.swing.Timer blinkTimer;


  public Z9001( EmuThread emuThread, Properties props )
  {
    super( emuThread );
    if( fontBytes == null ) {
      fontBytes = readResource( "/rom/z9001/z9001font.bin" );
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
    this.ramSize = getRAMSize( props );
    if( ramColor == null ) {
      ramColor = new byte[ 0x0400 ];
      fillRandom( ramColor );
    }
    if( ramVideo == null ) {
      ramVideo = new byte[ 0x0400 ];
      fillRandom( ramVideo );
    }
    this.audioInTStates = 0;
    this.audioInPhase   = this.emuThread.readAudioPhase();
    this.audioOutPhase  = false;
    this.mode20Rows     = false;
    this.colorMode      = getColorMode( props );
    this.colorSwap      = false;
    this.borderColorIdx = 0;

    this.kbMatrix = new int[ 8 ];
    Arrays.fill( this.kbMatrix, 0 );

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio90 = new Z80PIO( cpu, null );
    this.pio88 = new Z80PIO( cpu, this.pio90 );
    this.ctc80 = new Z80CTC( cpu, this.pio88 );
    this.ctc80.addCTCListener( this );
    cpu.addTStatesListener( this );

    if( this.colorMode ) {
      this.blinkTimer = new javax.swing.Timer( 200, this );
      this.blinkTimer.start();
    } else {
      this.blinkTimer = null;
    }
    updScreenConfig( 0 );
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


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc80 ) {
      switch( timerNum ) {
	case 0:
	  this.audioOutPhase = !this.audioOutPhase;
	  this.emuThread.updAudioOutPhase( this.audioOutPhase );
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
     * Auf der einen Seite soll das Audiosystem soll nicht zu oft abgefragt
     * werden.
     * Auf der anderen Seite sollen aber die Zykluszeit nicht so gross werden,
     * Dass die Genauigkeit der Zeitmessung kuenstlich verschlechert wird.
     * Aus diesem Grund werden genau soviele Taktzyklen abgezaehlt,
     * die wir auch der Vorteile der CTC mindestens zaehlen muss.
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
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    if( this.blinkTimer != null ) {
      this.blinkTimer.stop();
    }
    this.ctc80.removeCTCListener( this );
    this.emuThread.getZ80CPU().removeTStatesListener( this );
  }


  public String extractScreenText()
  {
    return extractScreenText( MEM_VIDEO, 24, 40, 40 );
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
    int rv = 0;
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


  public int getDefaultStartAddress()
  {
    return 0x0300;
  }


  public int getMinOSAddress()
  {
    return MEM_OS;
  }


  public int getMaxOSAddress()
  {
    int rv = MEM_OS;
    if( this.romOS != null ) {
      rv = MEM_OS + this.romOS.length - 1;
    }
    return rv;
  }


  public int getMemByte( int addr )
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
	&& (addr >= MEM_COLOR) && (addr < MEM_COLOR + ramColor.length) )
    {
      rv   = (int) ramColor[ addr - MEM_COLOR ] & 0xFF;
      done = true;
    }
    if( (addr >= MEM_VIDEO) && (addr < MEM_VIDEO + ramVideo.length) ) {
      rv   = (int) ramVideo[ addr - MEM_VIDEO ] & 0xFF;
      done = true;
    }
    if( !done && (addr < this.ramSize) ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getResetStartAddress()
  {
    return MEM_OS;
  }


  public int getScreenBaseHeight()
  {
    return 192;
  }


  public int getScreenBaseWidth()
  {
    return 320;
  }


  public String getSystemName()
  {
    return this.kc87 ? "KC87" : "KC85/1 (Z9001)";
  }


  public boolean hasKCBasicInROM()
  {
    return this.romBASIC != null;
  }


  public boolean keyEvent( KeyEvent e )
  {
    boolean rv = false;
    switch( e.getID() ) {
      case KeyEvent.KEY_PRESSED:
	switch( e.getKeyCode() ) {
	  case KeyEvent.VK_BACK_SPACE:
	    this.kbMatrix[ 0 ] = 0x40;
	    rv = true;
	    break;

	  case KeyEvent.VK_LEFT:
	    if( e.isShiftDown() ) {
	      this.kbMatrix[ 3 ] = 0x20;
	    } else {
	      this.kbMatrix[ 0 ] = 0x40;
	    }
	    rv = true;
	    break;

	  case KeyEvent.VK_RIGHT:
	    if( e.isShiftDown() ) {
	      this.kbMatrix[ 3 ] = 0x20;
	    } else {
	      this.kbMatrix[ 0 ] = 0x80;	// Shift
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
	    if( e.isShiftDown() ) {
	      this.kbMatrix[ 0 ] = 0x80;	// Shift
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
	    if( e.isShiftDown() ) {
	      this.kbMatrix[ 0 ] = 0x80;	// Shift
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
	    if( e.isShiftDown() ) {
	      this.kbMatrix[ 0 ] = 0x80;	// Shift
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

	  case KeyEvent.VK_CONTROL:
	    this.kbMatrix[ 2 ] = 0x80;
	    rv = true;
	    break;

	  case KeyEvent.VK_SHIFT:
	    this.kbMatrix[ 0 ] = 0x80;
	    rv = true;
	    break;
	}
        break;

      case KeyEvent.KEY_RELEASED:
	Arrays.fill( this.kbMatrix, 0 );
	putKeyboardMatrixValuesToPorts();
        rv = true;
        break;

      case KeyEvent.KEY_TYPED:
	char ch = e.getKeyChar();
	if( ch > 0 ) {
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
	}
	break;
    }
    if( rv ) {
      putKeyboardMatrixValuesToPorts();
    }
    return rv;
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


  public void reset( boolean powerOn )
  {
    this.ctc80.reset( powerOn );
    this.pio88.reset( powerOn );
    this.pio90.reset( powerOn );
    updScreenConfig( 0 );
    if( powerOn ) {
      fillRandom( ramColor );
      fillRandom( ramVideo );
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( this.colorMode
	&& (addr >= MEM_COLOR) && (addr < MEM_COLOR + ramColor.length) )
    {
      ramColor[ addr - MEM_COLOR ] = (byte) value;
      this.screenFrm.setScreenDirty( true );
      rv = true;
    }
    if( (addr >= MEM_VIDEO) && (addr < MEM_VIDEO + ramVideo.length) ) {
      ramVideo[ addr - MEM_VIDEO ] = (byte) value;
      this.screenFrm.setScreenDirty( true );
      rv = true;
    }
    else if( addr < this.ramSize ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFB ) {	// Adressleitung A2 nicht benutzt
      case 0x88:
	this.pio88.writePortA( value );
	updScreenConfig( this.pio88.fetchOutValuePortA( false ) );
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


	/* --- private Methoden --- */

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


  private boolean getColorMode( Properties props )
  {
    return EmuUtil.getBooleanProperty( props, "jkcemu.z9001.color", true );
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

