/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Grafikkarte des Computerclubs Jena
 */

package jkcemu.emusys.z1013;

import java.io.File;
import java.lang.*;
import jkcemu.base.*;
import jkcemu.etc.GDC82720;


public class GraphicCCJ implements GDC82720.GDCListener, GDC82720.VRAM
{
  private static final int CHAR_WIDTH            = 8;
  private static final int DEFAULT_SCREEN_HEIGHT = 256;
  private static final int DEFAULT_SCREEN_WIDTH  = 640;

  private static byte[] gccjFont = null;

  private ScreenFrm screenFrm;
  private GDC82720  gdc;
  private byte[]    fontBytes;
  private byte[]    vram;


  public GraphicCCJ( ScreenFrm screenFrm, String fontFileName )
  {
    this.screenFrm = screenFrm;
    this.gdc       = new GDC82720();
    this.vram      = new byte[ 0x0800 ];
    this.gdc.setVRAM( this );
    loadFont( fontFileName );
  }


  public boolean canExtractScreenText()
  {
    return this.gdc.canExtractScreenText();
  }


  public int getCharColCount()
  {
    return this.gdc.getCharColCount();
  }


  public int getCharRowCount()
  {
    return this.gdc.getCharRowCount();
  }


  public int getCharRowHeight()
  {
    return this.gdc.getCharRowHeight();
  }


  public int getCharTopLine()
  {
    return getYMargin() + this.gdc.getCharTopLine();
  }


  public int getCharWidth()
  {
    return CHAR_WIDTH;
  }


  public boolean getPixel( int x, int y )
  {
    boolean rv = false;
    if( this.fontBytes != null ) {
      y -= getYMargin();
      int h = this.gdc.getCharRowHeight();
      int v = this.gdc.getDisplayValue( x / CHAR_WIDTH, y );
      if( (h > 0) && (v >= 0) ) {
	int ch  = (int) this.vram[ v & 0x7FF ] & 0xFF;
	int idx = ch | (((y % h) << 8) & 0x0F00);
	if( (v & GDC82720.DISPL_CURSOR_MASK) != 0 ) {
	  if( this.fontBytes.length > 0x1000 ) {
	    idx |= 0x1000;
	  } else {
	    rv  = true;
	    idx = -1;
	  }
	}
	if( (idx >= 0) && (idx < this.fontBytes.length) ) {
	  int b = (int) this.fontBytes[ idx ];
	  int m = 0x80;
	  int n = x % CHAR_WIDTH;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  rv = ((b & m) != 0);
	}
      }
    }
    return rv;
  }


  public int getScreenChar( int chX, int chY )
  {
    return this.gdc.getScreenChar( chX, chY ) & 0x7F;
  }


  public int getScreenHeight()
  {
    int h = this.gdc.getDisplayLines();
    return h > DEFAULT_SCREEN_HEIGHT ? h : DEFAULT_SCREEN_HEIGHT;
  }


  public int getScreenWidth()
  {
    int w = this.gdc.getCharColCount() * CHAR_WIDTH;
    return w > DEFAULT_SCREEN_WIDTH ? w : DEFAULT_SCREEN_WIDTH;
  }


  public void loadFont( String fontFileName )
  {
    byte[] fontBytes = null;
    if( fontFileName != null ) {
      if( !fontFileName.isEmpty() ) {
	fontBytes = EmuUtil.readFile(
				this.screenFrm,
				fontFileName,
				0x2000,
				"Zeichensatzdatei" );
      }
    }
    if( fontBytes == null ) {
      if( gccjFont == null ) {
	gccjFont = EmuUtil.readResource(
				this.screenFrm,
				"/rom/z1013/gccjfont.bin" );
      }
      fontBytes = gccjFont;
    }
    this.fontBytes = fontBytes;
  }


  public int readData()
  {
    return this.gdc.readData();
  }


  public int readStatus()
  {
    return this.gdc.readStatus();
  }


  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != gccjFont;
  }


  public void writeArg( int value )
  {
    this.gdc.writeArg( value );
  }


  public void writeCmd( int value )
  {
    this.gdc.writeCmd( value );
  }


	/* --- GDC82720.GDCListener --- */

  public void screenConfigChanged( GDC82720 gdc )
  {
    this.screenFrm.clearScreenSelection();
    this.screenFrm.updScreenTextActionsEnabled();
  }


  public void screenDirty( GDC82720 gdc )
  {
    this.screenFrm.setScreenDirty( true );
  }


	/* --- GDC82720.VRAM --- */

  public int getVRAMWord( int addr )
  {
    return ((int) this.vram[ addr & 0x07FF ] & 0xFF) | 0xFF00;
  }


  public void setVRAMWord( int addr, int value )
  {
    this.vram[ addr & 0x07FF ]= (byte) value;
    this.screenFrm.setScreenDirty( true );
  }


	/* --- private Methoden --- */

  private int getYMargin()
  {
    int h = this.gdc.getDisplayLines();
    return DEFAULT_SCREEN_HEIGHT > h ? ((DEFAULT_SCREEN_HEIGHT - h) / 2) : 0;
  }
}
