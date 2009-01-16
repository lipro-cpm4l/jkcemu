/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.Color;
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
    StringBuilder buf = new StringBuilder( nRows + (nCols + 1) );
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


  public abstract int getDefaultStartAddress();


  public int getMemByte( int addr )
  {
    return 0xFF;
  }


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


  public boolean getSwapKeyCharCase()
  {
    return false;
  }


  public boolean hasKCBasicInROM()
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


  public boolean keyPressed( KeyEvent e )
  {
    return false;
  }


  public void keyReleased( int keyCode )
  {
    // leer
  }


  public void keyTyped( char keyChar )
  {
    // leer
  }


  public int readIOByte( int port )
  {
    return 0xFF;
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


  public boolean setMemByte( int addr, int value )
  {
    return false;
  }


  public boolean supportsRAMFloppyA()
  {
    return false;
  }


  public boolean supportsRAMFloppyB()
  {
    return false;
  }


  public void writeIOByte( int port, int value )
  {
    // leer
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

