/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Grafikkarte des Computerclubs Jena
 */

package jkcemu.etc;

import java.awt.event.KeyListener;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.AbstractScreenFrm;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.etc.GDC82720;
import jkcemu.file.FileUtil;


public class GraphicCCJena
		extends AbstractScreenDevice
		implements GDC82720.GDCListener, GDC82720.VRAM
{
  private static final int CHAR_WIDTH            = 8;
  private static final int DEFAULT_SCREEN_HEIGHT = 256;
  private static final int DEFAULT_SCREEN_WIDTH  = 640;

  private static byte[] gccjFont = null;

  private EmuSys   emuSys;
  private GDC82720 gdc;
  private byte[]   fontBytes;
  private byte[]   vram;


  public GraphicCCJena( EmuSys emuSys, Properties props )
  {
    super( props );
    this.emuSys = emuSys;
    this.gdc    = new GDC82720();
    this.vram   = new byte[ 0x0800 ];
    this.gdc.setVRAM( this );
    if( gccjFont == null ) {
      gccjFont = EmuUtil.readResource(
				this.emuSys.getScreenFrm(),
				"/rom/etc/gccjfont.bin" );
    }
  }


  public void loadFontByProperty( Properties props, String propName )
  {
    byte[] fontBytes = FileUtil.readFile(
			this.emuSys.getScreenFrm(),
			props.getProperty( propName ),
			true,
			0x2000,
			"Zeichensatzdatei f\u00FCr Grafikkarte CC Jena" );
    if( fontBytes == null ) {
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


  public void reset( boolean powerOn, Properties props )
  {
    if( powerOn ) {
      EmuUtil.initSRAM( this.vram, props );
      setScreenDirty( true );
    }
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

  @Override
  public void screenConfigChanged( GDC82720 gdc )
  {
    AbstractScreenFrm screenFrm = getScreenFrm();
    if( screenFrm != null ) {
      screenFrm.clearScreenSelection();
      screenFrm.fireUpdScreenTextActionsEnabled();
    }
  }


  @Override
  public void screenDirty( GDC82720 gdc )
  {
    setScreenDirty( true );
  }


	/* --- GDC82720.VRAM --- */

  @Override
  public int getVRAMWord( int addr )
  {
    return ((int) this.vram[ addr & 0x07FF ] & 0xFF) | 0xFF00;
  }


  @Override
  public void setVRAMWord( int addr, int value )
  {
    this.vram[ addr & 0x07FF ]= (byte) value;
    screenDirty( this.gdc );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void cancelPastingText()
  {
    this.emuSys.cancelPastingText();
  }


  @Override
  public boolean canExtractScreenText()
  {
    return this.gdc.canExtractScreenText();
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
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
	    rv  = WHITE;
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
	  if( ((b & m) != 0) ) {
	    rv = WHITE;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return this.gdc.canExtractScreenText() ?
		new CharRaster(
			this.gdc.getCharColCount(),
			this.gdc.getCharRowCount(),
			this.gdc.getCharRowHeight(),
			CHAR_WIDTH,
			this.gdc.getCharRowHeight(),
			0,
			getYMargin() + this.gdc.getCharTopLine() )
		: null;
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.emuSys.getEmuThread();
  }


  @Override
  public KeyListener getKeyListener()
  {
    return this.emuSys.getScreenFrm();
  }


  @Override
  public int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    return this.gdc.getScreenChar( chX, chY ) & 0x7F;
  }


  @Override
  public int getScreenHeight()
  {
    int h = this.gdc.getDisplayLines();
    return h > DEFAULT_SCREEN_HEIGHT ? h : DEFAULT_SCREEN_HEIGHT;
  }


  @Override
  public int getScreenWidth()
  {
    int w = this.gdc.getCharColCount() * CHAR_WIDTH;
    return w > DEFAULT_SCREEN_WIDTH ? w : DEFAULT_SCREEN_WIDTH;
  }


  @Override
  public String getTitle()
  {
    return Main.APPNAME + ": Grafikkarte CC Jena";
  }


  @Override
  public void informPastingTextStatusChanged( boolean pasting )
  {
    this.emuSys.informPastingTextStatusChanged( pasting );
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != gccjFont;
  }


  @Override
  public void startPastingText( String text )
  {
    this.emuSys.startPastingText( text );
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return this.emuSys.supportsPasteFromClipboard();
  }


	/* --- private Methoden --- */

  private int getYMargin()
  {
    int h = this.gdc.getDisplayLines();
    return DEFAULT_SCREEN_HEIGHT > h ? ((DEFAULT_SCREEN_HEIGHT - h) / 2) : 0;
  }
}
