/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.JOptionPane;
import jkcemu.base.ScreenFrm;
import z80emu.*;


public abstract class EmuSys
{
  protected static final int BLACK = 0;
  protected static final int WHITE = 1;

  protected EmuThread emuThread;
  protected ScreenFrm screenFrm;

  private static Random random = null;


  public EmuSys( EmuThread emuThread )
  {
    this.emuThread = emuThread;
    this.screenFrm = emuThread.getScreenFrm();
  }


  protected void appendSpacesToCol(
			StringBuilder buf,
			int           begOfLine,
			int           col )
  {
    int n = begOfLine + col - buf.length();
    while( n > 0 ) {
      buf.append( (char) '\u0020' );
      --n;
    }
  }


  public void applySettings( Properties props )
  {
    // leer
  }


  protected int askKCBasicBegAddr()
  {
    int         addr    = -1;
    String[]    options = { "RAM-BASIC", "ROM-BASIC", "Abbrechen" };
    JOptionPane pane    = new JOptionPane(
	"W\u00E4hlen Sie bitte aus,"
		+ " ob das KC-BASIC-Programm im Adressbereich f\u00FCr das\n"
		+ "RAM-BASIC (ab 2C01h) oder f\u00FCr das"
		+ " ROM-BASIC (ab 0401h) gesucht werden soll.",
	JOptionPane.QUESTION_MESSAGE );
    pane.setOptions( options );
    pane.createDialog(
		this.screenFrm,
		"Adresse des KC-BASIC-Programms" ).setVisible( true );
    Object value = pane.getValue();
    if( value != null ) {
      if( value.equals( options[ 0 ] ) ) {
	addr = 0x2C01;
      }
      else if( value.equals( options[ 1 ] ) ) {
	addr = 0x0401;
      }
    }
    return addr;
  }


  public void die()
  {
    // leer
  }


  public String extractScreenText()
  {
    return null;
  }


  protected String extractMemText(
				int addr,
				int nRows,
				int nCols,
				int colDist )
  {
    StringBuilder buf = new StringBuilder( nRows * (nCols + 1) );
    for( int i = 0; i < nRows; i++ ) {
      int rowAddr = addr + (i * colDist);
      int nSpaces = 0;
      for( int k = 0; k < nCols; k++ ) {
	int b = this.emuThread.getMemByte( rowAddr + k );
	if( (b == 0) || b == 0x20 ) {
	  nSpaces++;
	}
	else if( (b > 0x20) && (b < 0x7F) ) {
	  while( nSpaces > 0 ) {
	    buf.append( (char) '\u0020' );
	    --nSpaces;
	  }
	  buf.append( (char) b );
	}
      }
      buf.append( (char) '\n' );
    }
    return buf.toString();
  }


  protected void fillRandom( byte[] a )
  {
    if( a != null ) {
      if( random == null ) {
	random = new Random();
	random.setSeed( System.currentTimeMillis() );
      }
      for( int i = 0; i < a.length; i++ ) {
	a[ i ] = (byte) (random.nextInt() & 0xFF);
      }
    }
  }


  /*
   * Die Methode liefert den Wert, auf den der Stackpointer vor einem
   * durch JKCEMU initiierten Programmstart gesetzt wird.
   * Bei einem negativen Wert wird der Stackpointer nicht gesetzt.
   */
  public int getAppStartStackInitValue()
  {
    return -1;
  }


  public int getBorderColorIndex()
  {
    return BLACK;
  }


  public Color getColor( int colorIdx )
  {
    return colorIdx == WHITE ? Color.white : Color.black;
  }


  public int getColorCount()
  {
    return 2;		// schwarz/weiss
  }


  public int getColorIndex( int x, int y )
  {
    return WHITE;
  }


  public Integer getLoadAddr()
  {
    return null;
  }


  public abstract int getMemByte( int addr );


  public int getMemWord( int addr )
  {
    return (getMemByte( addr + 1 ) << 8) | getMemByte( addr );
  }


  public abstract int getMinOSAddress();
  public abstract int getMaxOSAddress();


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0;
  }


  public abstract int    getScreenBaseHeight();
  public abstract int    getScreenBaseWidth();
  public abstract String getSystemName();


  public Image getScreenImage()
  {
    return null;
  }


  public boolean getSwapKeyCharCase()
  {
    return false;
  }


  public boolean hasKCBasicInROM()
  {
    return false;
  }


  /*
   * Diese Methode besagt, ob der Zeichensatz Zeichen
   * nach der deutschen Variante von ISO646 enthaelt.
   */
  public boolean isISO646DE()
  {
    return false;
  }


  public void openBasicProgram()
  {
    showFunctionNotSupported();
  }


  public void openTinyBasicProgram()
  {
    showFunctionNotSupported();
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    return false;
  }


  public void keyReleased()
  {
    // leer
  }


  public boolean keyTyped( char keyChar )
  {
    return false;
  }


  public boolean pasteChar( char ch )
  {
    boolean rv = false;
    switch( ch ) {
      case '\n':
      case '\r':
	rv = keyPressed( KeyEvent.VK_ENTER, false );
	break;

      case '\u0020':
	rv = keyPressed( KeyEvent.VK_SPACE, false );
	break;

      default:
	rv = keyTyped( ch );
    }
    if( rv ) {
      try {
	Thread.sleep( 150 );
      }
      catch( InterruptedException ex ) {}
      keyReleased();
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    return 0xFF;
  }


  public int readMemByte( int addr )
  {
    return getMemByte( addr );
  }


  protected byte[] readResource( String resource )
  {
    ByteArrayOutputStream buf  = new ByteArrayOutputStream( 0x0800 );
    boolean               done = false;
    InputStream           in   = null;
    Exception             ex   = null;
    try {
      in = getClass().getResourceAsStream( resource );
      if( in != null ) {
	int b = in.read();
	while( b != -1 ) {
	  buf.write( b );
	  b = in.read();
	}
	done = true;
      }
    }
    catch( IOException ioEx ) {
      ex = ioEx;
    }
    finally {
      EmuUtil.doClose( in );
    }
    if( !done ) {
      EmuUtil.showSysError(
		this.emuThread.getScreenFrm(),
		"Resource " + resource + " kann nicht geladen werden",
		ex );
    }
    return buf.toByteArray();
  }


  /*
   * Diese Methode reassembliert Bytes als Zeichenkette
   * bis einschliesslich das Byte, bei dem Bit 7 gesetzt ist.
   *
   * Rueckgabewert: Anzahl der reassemlierten Bytes
   */
  protected int reassStringBit7(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs )
  {
    int     rv   = 0;
    boolean loop = true;
    while( loop ) {
      int     a = addr;
      int     n = 0;
      long    r = 0;
      boolean c = false;
      while( (n < 5) && (a < 0x10000) ) {
	int b = getMemByte( a );
	if( n == 0 ) {
	  c = ((b >= 0x20) && (b < 0x7F));
	} else {
	  if( ((b >= 0x20) && (b < 0x7F)) != c ) {
	    break;
	  }
	}
	r = (r << 8) | b;
	n++;
	a++;
	if( (b & 0x80) != 0 ) {
	  loop = false;
	  break;
	}
      }
      if( n > 0 ) {
	int begOfLine = buf.length();
	buf.append( String.format( "%04X ", addr ) );
	addr = a;

	long m1 = 0;
	for( int i = 0; i < n; i++ ) {
	  m1 = (m1 << 8) | (r & 0xFF);
	  r >>= 8;
	}
	long m2 = m1;

	for( int i = 0; i < n; i++ ) {
	  buf.append( String.format( " %02X", (int) m1 & 0xFF ) );
	  m1 >>= 8;
	}
	appendSpacesToCol( buf, begOfLine, colMnemonic );
	buf.append( "DB" );
	appendSpacesToCol( buf, begOfLine, colArgs );

	boolean first = true;
	boolean quote = false;
	for( int i = 0; i < n; i++ ) {
	  int b = (int) m2 & 0xFF;
	  if( (b & 0x80) != 0 ) {
	    if( quote ) {
	      buf.append( (char) '\'' );
	      quote = false;
	    }
	    if( first ) {
	      first = false;
	    } else {
	      buf.append( (char) ',' );
	    }
	    if( (b >= 0xA0) && (b < 0xFF) ) {
	      buf.append( "80H+\'" );
	      buf.append( (char) (b & 0x7F) );
	      buf.append( (char) '\'' );
	    } else {
	      if( b >= 0xA0 ) {
		buf.append( (char) '0' );
	      }
	      buf.append( String.format( "%02XH", b ) );
	    }
	  } else {
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      if( !quote ) {
		if( first ) {
		  first = false;
		} else {
		  buf.append( (char) ',' );
		}
		buf.append( (char) '\'' );
		quote = true;
	      }
	      buf.append( (char) b );
	    } else {
	      if( quote ) {
		buf.append( (char) '\'' );
		quote = false;
	      }
	      if( first ) {
		first = false;
	      } else {
		buf.append( (char) ',' );
	      }
	      buf.append( String.format( "%02XH", b ) );
	    }
	  }
	  m2 >>= 8;
	}
	if( quote ) {
	  buf.append( (char) '\'' );
	}
	buf.append( (char) '\n' );
	rv += n;
      }
    }
    return rv;
  }


  /*
   * Diese Methode reassembliert einen Aufruf in einer Sprungtabelle.
   *
   * Rueckgabewert: Anzahl der reassemlierten Bytes
   */
  protected int reassSysCallTable(
			int           addr,
			int           sysCallTableAddr,
			String[]      sysCallNames,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv = 0;
    String s = null;
    int    b = getMemByte( addr );
    switch( b ) {
      case 0xC3:
	s = "JP";
	break;
      case 0xCD:
	s = "CALL";
	break;
    }
    if( s != null ) {
      int w = getMemWord( addr + 1 );
      if( w >= sysCallTableAddr ) {
	int m = w - sysCallTableAddr;
	int idx = m / 3;
	if( ((idx * 3) == m) && (idx < sysCallNames.length) ) {
	  int bol = buf.length();
	  buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w & 0xFF,
				w >> 8 ) );
	  appendSpacesToCol( buf, bol, colMnemonic );
	  buf.append( s );
	  appendSpacesToCol( buf, bol, colArgs );
	  if( w >= 0xA000 ) {
	    buf.append( (char) '0' );
	  }
	  buf.append( String.format( "%04XH", w ) );
	  appendSpacesToCol( buf, bol, colRemark );
	  buf.append( (char) ';' );
	  buf.append( sysCallNames[ idx ] );
	  buf.append( (char) '\n' );
	  rv = 3;
	}
      }
    }
    return rv;
  }


  /*
   * Diese Methode wird vom Reassembler aufgerufen,
   * bevor der Befehl an der uebergebenen Adresse reassembliert wird.
   * Damit kann das emulierte System Einfluss auf die Reassemblierung
   * nehmen, z.B. bei Systemaufrufen.
   * Insbesondere wenn hinter Systemaufrufen Datenbytes stehen,
   * die nicht als Befehle uebersetzt werden sollen,
   * muss die Methode ueberschrieben werden.
   *
   * An an Puffer muessen immer vollstaendige Zeilen angehaengt werden.
   * Die Formatierung wird durch die restlichen drei Argumente angegeben.
   *
   * Rueckgabewert: Anzahl der reassemlierten Bytes
   */
  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return 0;		// kein Byte reassembliert
  }


  public boolean requiresReset( Properties props )
  {
    return true;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    // leer
  }


  public void saveBasicProgram()
  {
    showFunctionNotSupported();
  }


  public void saveTinyBasicProgram()
  {
    showFunctionNotSupported();
  }


  public abstract boolean setMemByte( int addr, int value );


  protected void showNoBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  public boolean supportsRAMFloppyA()
  {
    return false;
  }


  public boolean supportsRAMFloppyB()
  {
    return false;
  }


  public void updSysCells(
		int    begAddr,
		int    len,
		Object fileFmt,
		int    fileType )
  {
    // leer
  }


  public void writeIOByte( int port, int value )
  {
    // leer
  }


  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );
  }


	/* --- private Methoden --- */

  private void showFunctionNotSupported()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Diese Funktion steht f\u00Fcr das gerade emulierte System\n"
		+ "nicht zur Verf\u00FCgung." );
  }
}

