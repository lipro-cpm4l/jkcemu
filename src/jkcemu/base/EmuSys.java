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
import jkcemu.base.ScreenFrm;
import z80emu.*;


public abstract class EmuSys
{
  protected EmuThread emuThread;
  protected ScreenFrm screenFrm;

  private static Random random = null;


  public EmuSys( EmuThread emuThread )
  {
    this.emuThread = emuThread;
    this.screenFrm = emuThread.getScreenFrm();
  }


  public void die()
  {
    // leer
  }


  public String extractScreenText()
  {
    return null;
  }


  protected String extractScreenText(
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
	if( b == 0x20 ) {
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
    return 0;		// schwarz
  }


  public Color getColor( int colorIdx )
  {
    return colorIdx == 1 ? Color.white : Color.black;
  }


  public int getColorCount()
  {
    return 2;		// schwarz/weiss
  }


  public int getColorIndex( int x, int y )
  {
    return 0;
  }


  public abstract int getDefaultStartAddress();


  public int getMemByte( int addr )
  {
    return 0xFF;
  }


  public abstract int getMinOSAddress();
  public abstract int getMaxOSAddress();


  public int getResetStartAddress()
  {
    return 0;
  }


  public abstract int    getScreenBaseHeight();
  public abstract int    getScreenBaseWidth();
  public abstract String getSystemName();


  public boolean hasKCBasicInROM()
  {
    return false;
  }


  public boolean keyEvent( KeyEvent e )
  {
    return false;
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


  public void reset( boolean powerOn )
  {
    // leer
  }


  public boolean setMemByte( int addr, int value )
  {
    return false;
  }


  public void writeIOByte( int port, int value )
  {
    // leer
  }
}

