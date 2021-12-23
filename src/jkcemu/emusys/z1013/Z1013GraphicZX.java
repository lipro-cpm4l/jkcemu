/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Z1013-Vollgrafikerweiterung nach practic 2/88
 * Die Aufloesung und die Pixelzuordnungen sind kompatibel zum ZX Spectrum.
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyListener;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.AbstractScreenFrm;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.Z1013;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class Z1013GraphicZX
		extends AbstractScreenDevice
		implements
			Z80InterruptSource,
			Z80MaxSpeedListener,
			Z80TStatesListener
{
  private static final int SCREEN_HEIGHT     = 192;
  private static final int SCREEN_WIDTH      = 256;
  private static final int LINES_PER_SCREEN  = 312;
  private static final int FIRST_SCREEN_LINE = 64;

  private Z1013        z1013;
  private byte[]       pixels;
  private byte[]       ram;
  private boolean      interruptRequested;
  private int          curScreenLine;
  private int          lineTStateCounter;
  private volatile int tStatesPerLine;


  public Z1013GraphicZX( Z1013 z1013, Properties props )
  {
    super( props );
    this.z1013              = z1013;
    this.pixels             = new byte[ SCREEN_WIDTH * SCREEN_HEIGHT ];
    this.ram                = new byte[ this.pixels.length ];
    this.interruptRequested = false;
    this.curScreenLine      = 0;
    this.lineTStateCounter  = 0;
    this.tStatesPerLine     = 0;
  }


  public int read( int addr )
  {
    int rv  = -1;
    int idx = addr - 0x4000;
    if( (idx >= 0) && (idx < this.ram.length) ) {
      rv = (int) this.ram[ idx ] & 0xFF;
    }
    return rv;
  }


  public void reset( boolean powerOn, Properties props )
  {
    if( powerOn ) {
      EmuUtil.initSRAM( this.ram, props );
      setScreenDirty( true );
    }
    this.interruptRequested = false;
  }


  public boolean write( int addr, int value )
  {
    boolean rv  = false;
    int     idx = addr - 0x4000;
    if( (idx >= 0) && (idx < this.ram.length) ) {
      this.ram[ idx ] = (byte) value;
      setScreenDirty( true );
      rv = true;
    }
    return rv;
  }


	/* --- Z80InterruptSource --- */

  /*
   * Die Hardware der Grafikkarte ist an den ZX Spectrum angelehnt
   * und reagiert nicht auf RETI-Befehle,
   * weshalb diese speziellen Interrupt-Return-Befehle
   * auch nicht vewendet werden.
   * Damit wird aber auch "interruptFinish(...)" nie aufgerufen.
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
  public boolean interruptFinish( int addr )
  {
    return false;
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


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    /*
     * Paramter wie beim ZX Spectrum 48K
     * (224 T-States bei 3.5 MHz Taktfrequenz)
     */
    this.tStatesPerLine = (int) Math.round( cpu.getMaxSpeedKHz() / 15.625 );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.lineTStateCounter += tStates;
    if( this.lineTStateCounter >= this.tStatesPerLine ) {
      this.lineTStateCounter -= this.tStatesPerLine;
      updScreenLine();
      this.curScreenLine++;
      if( this.curScreenLine >= LINES_PER_SCREEN ) {
	this.curScreenLine      = 0;
	this.interruptRequested = true;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void cancelPastingText()
  {
    this.z1013.cancelPastingText();
  }


  @Override
  public int getBorderColorIndex()
  {
    return WHITE;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv  = WHITE;
    int col = x / 8;
    int idx = (y << 5) | col;
    if( (idx >= 0) && (idx < this.pixels.length) ) {
      int b = (int) this.pixels[ idx ] & 0xFF;
      int m = 0x01;
      int p = x % 8;
      if( p > 0 ) {
	m <<= p;
      }
      if( (b & m) != 0 ) {
	rv = BLACK;
      }
    }
    return rv;
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.z1013.getEmuThread();
  }


  @Override
  public KeyListener getKeyListener()
  {
    return this.z1013.getScreenFrm();
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
  public String getTitle()
  {
    return Main.APPNAME + ": Z1013-Vollgrafik nach practic 2/88";
  }


  @Override
  public String toString()
  {
    return "ZX-Spectrum-kompatible S/W-Grafikkarte nach practic 2/88";
  }


	/* --- private Methoden --- */

  private void updScreenLine()
  {
    int screenLine = this.curScreenLine;
    int pixelLine  = screenLine - FIRST_SCREEN_LINE;
    if( (pixelLine >= 0) && (pixelLine < SCREEN_HEIGHT) ) {
      int pixelIdx = pixelLine << 5;
      int ramIdx   = ((pixelLine << 5) & 0x1800)
			| ((pixelLine << 2) & 0x00E0)
			| ((pixelLine << 8) & 0x0700);
      for( int i = 0; i < 32; i++ ) {
	byte b = this.ram[ ramIdx++ ];
	if( b != this.pixels[ pixelIdx ] ) {
	  this.pixels[ pixelIdx ] = b;
	  setScreenDirty( true );
	}
	pixelIdx++;
      }
    }
  }
}
