/*
 * (c) 2010-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des A5105-Video-Interface-Schaltkreises
 */

package jkcemu.emusys.a5105;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import jkcemu.base.EmuThread;
import jkcemu.base.ScreenFrm;
import jkcemu.etc.GDC82720;


public class VIS implements GDC82720.GDCListener, GDC82720.VRAM
{
  private static final int COLOR_COUNT = 16;

  /*
   * Der A5105 hat ueblicherweise eine Bildschirmaufloesung von 200 Zeilen.
   * Es ist aber moeglich, bis zu 250 Zeilen zu programmieren.
   * Damit sich die Fenstergroesse durch das Umprogrammieren
   * nicht staendig aendert, werden hier fest 250 Pixel gesetzt.
   */
  private static final int DEFAULT_SCREEN_HEIGHT = 250;
  private static final int DEFAULT_SCREEN_WIDTH  = 320;


  private GDC82720        gdc;
  private ScreenFrm       screenFrm;
  private BufferedImage   screenImage;
  private int             screenWidth;
  private IndexColorModel colorModel;
  private Color[]         colors;
  private int[]           colorRGBs;
  private boolean         colorMode;
  private boolean         fixedScreenSize;
  private boolean         fontGenVisAccess;
  private boolean         fontGenEnabled;
  private boolean         lightColors;
  private boolean         w640;
  private int             mode;
  private int             colorReg0;
  private int             colorReg1;
  private int             colorReg2;
  private int             colorReg3;
  private int             colorReg4;
  private int             fontBaseAddr;
  private int             fontRowAddr;
  private byte[]          fontBytes;
  private short[]         vram;


  public VIS( ScreenFrm screenFrm, GDC82720 gdc )
  {
    this.screenFrm   = screenFrm;
    this.gdc         = gdc;
    this.screenWidth = DEFAULT_SCREEN_WIDTH;
    this.fontBytes   = new byte[ 0x0800 ];
    this.vram        = new short[ 0x10000 ];
    this.colorModel  = null;
    this.colors      = new Color[ COLOR_COUNT ];
    this.colorRGBs   = new int[ COLOR_COUNT ];
    Arrays.fill( this.colors, Color.BLACK );
    Arrays.fill( this.colorRGBs, 0 );
    reset( true );
  }


  public boolean canExtractScreenText()
  {
    return this.fontGenEnabled && this.gdc.canExtractScreenText();
  }


  public static IndexColorModel createColorModel( float brightness )
  {
    byte[] r  = new byte[ COLOR_COUNT ];
    byte[] g  = new byte[ COLOR_COUNT ];
    byte[] b  = new byte[ COLOR_COUNT ];
    int    v3 = Math.round( 255 * brightness );
    int    v2 = Math.round( 180 * brightness );
    int    v1 = Math.round( 80 * brightness );
    for( int i = 0; i < COLOR_COUNT; i++ ) {
      if( (i & 0x08) != 0 ) {
	r[ i ] = (byte) ((i & 0x04) != 0 ? v3 : v1);
	g[ i ] = (byte) ((i & 0x02) != 0 ? v3 : v1);
	b[ i ] = (byte) ((i & 0x01) != 0 ? v3 : v1);
      } else {
	r[ i ] = (byte) ((i & 0x04) != 0 ? v2 : 0);
	g[ i ] = (byte) ((i & 0x02) != 0 ? v2 : 0);
	b[ i ] = (byte) ((i & 0x01) != 0 ? v2 : 0);
      }
    }
    return new IndexColorModel( 4, COLOR_COUNT, r, g, b );
  }


  public void createColors( float brightness )
  {
    synchronized( this.colors ) {
      this.screenImage = null;
      this.colorModel  = createColorModel( brightness );
      this.colorModel.getRGBs( this.colorRGBs );
      for( int i = 0; i < COLOR_COUNT; i++ ) {
	this.colors[ i ] = new Color( this.colorRGBs[ i ] );
      }
    }
  }


  public int getBorderColorIndex()
  {
    return this.colorReg0;
  }


  public int getCharColCount()
  {
    return getScreenWidth() / getCharWidth();
  }


  public int getCharRowCount()
  {
    return this.fontGenEnabled ? this.gdc.getCharRowCount() : 0;
  }


  public int getCharTopLine()
  {
    int yMargin = (DEFAULT_SCREEN_HEIGHT - this.gdc.getDisplayLines()) / 2;
    if( yMargin < 0 ) {
      yMargin = 0;
    }
    int rv = yMargin + this.gdc.getCharTopLine();
    if( this.fixedScreenSize || this.screenFrm.isFullScreenMode() ) {
      rv *= 2;
    }
    return rv;
  }


  public int getCharWidth()
  {
    return 8 * getScreenWidth() / this.screenWidth;
  }


  public Color getColor( int idx )
  {
    return (idx >= 0) && (idx < this.colors.length) ?
					this.colors[ idx ]
					: Color.BLACK;
  }


  public int getColorCount()
  {
    return this.colors.length;
  }


  public int getScreenHeight()
  {
    return (this.fixedScreenSize || this.screenFrm.isFullScreenMode()) ?
		(2 * DEFAULT_SCREEN_HEIGHT) : DEFAULT_SCREEN_HEIGHT;
  }


  public int getScreenWidth()
  {
    int rv = DEFAULT_SCREEN_WIDTH;
    if( this.fixedScreenSize || this.screenFrm.isFullScreenMode() ) {
      rv = 2 * DEFAULT_SCREEN_WIDTH;
    } else {
      if( this.screenWidth > DEFAULT_SCREEN_WIDTH ) {
	rv = this.screenWidth;
      }
    }
    return rv;
  }


  public boolean isFixedScreenSize()
  {
    return this.fixedScreenSize;
  }


  public void paintScreen( Graphics g, int xOffs, int yOffs, int screenScale )
  {
    BufferedImage img = getScreenImage();
    if( img != null ) {
      int width   = img.getWidth();
      int height  = img.getHeight();
      int border  = this.colorRGBs[ colorReg0 ];
      int cLine   = 0;
      int yMargin = (DEFAULT_SCREEN_HEIGHT - this.gdc.getDisplayLines()) / 2;
      if( yMargin > 0 ) {
	if( this.fixedScreenSize || this.screenFrm.isFullScreenMode() ) {
	  yMargin *= 2;
	}
	yOffs += (yMargin * screenScale);
      }
      for( int y = 0; y < height; y++ ) {
	int x   = 0;
	int xCh = 0;
	int a   = this.gdc.getDisplayValue( xCh++, y );
	if( a < 0 ) {
	  for( int i = 0; i < width; i++ ) {
	    img.setRGB( i, y, border );
	  }
	} else {
	  if( (this.mode == 0) && ((a & GDC82720.DISPL_IMAGE_MASK) == 0) ) {
	    // Textmode
	    if( (a & GDC82720.DISPL_NEW_CROW_MASK) != 0 ) {
	      cLine = 0;
	    } else {
	      cLine++;
	    }
	    while( a >= 0 ) {
	      int b = 0;
	      int v = getVRAMWord( a & GDC82720.DISPL_ADDR_MASK );
	      if( this.fontGenEnabled ) {
		if( cLine < 8 ) {
		  int idx = ((v << 3) & 0x7F8) | cLine;
		  if( idx < this.fontBytes.length ) {
		    b = (int) this.fontBytes[ idx ] & 0xFF;
		  }
		}
	      } else {
		b = v & 0xFF;
	      }
	      if( (a & GDC82720.DISPL_CURSOR_MASK) != 0 ) {
		b = ~b & 0xFF;
	      }
	      int bgIdx = (v >> 13) & 0x07;
	      if( this.lightColors ) {
		bgIdx |= 0x08;
	      }
	      int bg = this.colorRGBs[ bgIdx ];
	      int fg = bg;
	      if( ((v & 0x1000) == 0)
		  || (a & GDC82720.DISPL_BLINK_MASK) != 0 )
	      {
		fg = this.colorRGBs[ (v >> 8) & 0x0F ];
	      }
	      int m = 0x01;
	      for( int i = 0; i < 8; i++ ) {
		img.setRGB( x++, y, (b & m) != 0 ? fg : bg );
		m <<= 1;
	      }
	      if( x >= width ) {
		break;
	      }
	      a = this.gdc.getDisplayValue( xCh++, y );
	    }
	  } else {
	    // Grafikmodi
	    if( this.mode == 0 ) {
	      while( a >= 0 ) {
		int v = getVRAMWord( a & GDC82720.DISPL_ADDR_MASK );
		int m = 0x0001;
		int c = 0;
		for( int i = 0; i < COLOR_COUNT; i++ ) {
		  img.setRGB(
			x++,
			y,
			this.colorRGBs[ (v & m) != 0 ?
						this.colorReg2
						: this.colorReg1 ] );
		  m <<= 1;
		}
		if( x >= width ) {
		  break;
		}
		a = this.gdc.getDisplayValue( xCh++, y );
	      }
	    } else if( this.mode == 1 ) {
	      while( a >= 0 ) {
		int v  = getVRAMWord( a & GDC82720.DISPL_ADDR_MASK );
		int m0 = 0x0001;
		int m1 = 0x0100;
		int c  = 0;
		for( int i = 0; i < 8; i++ ) {
		  int r = 0;
		  if( (v & m0) != 0 ) {
		    r |= 0x01;
		  }
		  if( (v & m1) != 0 ) {
		    r |= 0x02;
		  }
		  switch( r ) {
		    case 0:
		      c = this.colorReg1;
		      break;
		    case 1:
		      c = this.colorReg2;
		      break;
		    case 2:
		      c = this.colorReg3;
		      break;
		    case 3:
		      c = this.colorReg4;
		      break;
		  }
		  img.setRGB( x++, y, this.colorRGBs[ c ] );
		  m0 <<= 1;
		  m1 <<= 1;
		}
		if( x >= width ) {
		  break;
		}
		a = this.gdc.getDisplayValue( xCh++, y );
	      }
	    } else if( this.mode == 2 ) {
	      while( a >= 0 ) {
		int v  = getVRAMWord( a & GDC82720.DISPL_ADDR_MASK );
		int mb = 0x0001;
		int mg = 0x0010;
		int mr = 0x0100;
		int mi = 0x1000;
		for( int i = 0; i < 4; i++ ) {
		  int c = 0;
		  if( (v & mb) != 0 ) {
		    c |= 0x01;
		  }
		  if( (v & mg) != 0 ) {
		    c |= 0x02;
		  }
		  if( (v & mr) != 0 ) {
		    c |= 0x04;
		  }
		  if( (v & mi) != 0 ) {
		    c |= 0x08;
		  }
		  img.setRGB( x++, y, this.colorRGBs[ c ] );
		  mb <<= 1;
		  mg <<= 1;
		  mr <<= 1;
		  mi <<= 1;
		}
		if( x >= width ) {
		  break;
		}
		a = this.gdc.getDisplayValue( xCh++, y );
	      }
	    }
	  }
	}
	while( x < width ) {
	  img.setRGB( x++, y, border );
	}
      }
      if( this.fixedScreenSize || this.screenFrm.isFullScreenMode() ) {
	g.drawImage(
		img,
		xOffs,
		yOffs,
		2 * DEFAULT_SCREEN_WIDTH * screenScale,
		2 * img.getHeight() * screenScale,
		this.screenFrm );
      } else {
	if( screenScale > 1 ) {
	  g.drawImage(
		img,
		xOffs,
		yOffs,
		img.getWidth() * screenScale,
		img.getHeight() * screenScale,
		this.screenFrm );
	} else {
	  g.drawImage( img, xOffs, yOffs, this.screenFrm );
	}
      }
    }
  }


  public int readFontByte()
  {
    int rv = 0;
    if( !this.fontGenVisAccess ) {
      int idx = this.fontBaseAddr | this.fontRowAddr;
      if( (idx >= 0) && (idx < this.fontBytes.length) ) {
	rv = (int) this.fontBytes[ idx ] & 0xFF;
      }
      this.fontRowAddr = (this.fontRowAddr + 1) & 0x07;
    }
    return rv;
  }


  public void reset( boolean powerOn )
  {
    this.fontGenVisAccess = false;
    this.fontGenEnabled   = false;
    this.lightColors      = false;
    this.mode             = -1;
    this.colorReg0        = 0;
    this.colorReg0        = 0;
    this.colorReg0        = 0;
    this.colorReg0        = 0;
    this.fontBaseAddr     = 0;
    this.fontRowAddr      = 0;
    if( powerOn ) {
      Arrays.fill( this.fontBytes, (byte) 0 );
      Arrays.fill( this.vram, (short) 0 );
    }
    synchronized( this.colors ) {
      this.screenImage = null;
      this.w640        = false;
    }
    this.screenFrm.setScreenDirty( true );
  }


  public void setFixedScreenSize( boolean state )
  {
    this.fixedScreenSize = state;
  }


  public void writeFontAddr( int value )
  {
    if( !this.fontGenVisAccess ) {
      this.fontBaseAddr = (value << 3) & 0x07F8;
      this.fontRowAddr  = 0;
    }
  }


  public void writeFontByte( int value )
  {
    if( !this.fontGenVisAccess ) {
      int idx = this.fontBaseAddr | this.fontRowAddr;
      if( (idx >= 0) && (idx < this.fontBytes.length) ) {
	this.fontBytes[ idx ] = (byte) value;
      }
      this.fontRowAddr = (this.fontRowAddr + 1) & 0x07;
      this.screenFrm.setScreenDirty( true );
    }
  }


  public void writeMode( int value )
  {
    boolean configChanged = false;
    switch( value & 0x70 ) {
      case 0x00:				// Betriebsartenregister 0
	int oldMode    = this.mode;
	this.colorMode = ((value & 0x03) != 0x03);
	switch( value & 0x07 ) {
	  case 0:
	  case 3:
	  case 4:
	    this.mode = 0;
	    updScreenWidth();
	    break;
	  case 1:
	  case 5:
	    this.mode = 1;
	    updScreenWidth();
	    break;
	  case 2:
	  case 6:
	    this.mode = 2;
	    updScreenWidth();
	    break;
	}
	if( this.mode != oldMode ) {
	  configChanged = true;
	}
	break;

      case 0x10:				// Betriebsartenregister 1
	boolean oldW640           = this.w640;
	boolean oldFontGenEnabled = this.fontGenEnabled;
	synchronized( this.colors ) {
	  this.w640             = ((value & 0x02) == 0);
	  this.fontGenVisAccess = ((value & 0x01) == 0);
	  this.fontGenEnabled   = ((value & 0x04) == 0);
	  this.lightColors      = ((value & 0x08) != 0);
	  updScreenWidth();
	}
	if( (this.w640 != oldW640)
	    || (this.fontGenEnabled != oldFontGenEnabled) )
	{
	  configChanged = true;
	}
	break;

      case 0x20:
	this.colorReg0 = value & 0x0F;
	break;

      case 0x30:
	this.colorReg1 = value & 0x0F;
	break;

      case 0x40:
	this.colorReg2 = value & 0x0F;
	break;

      case 0x50:
	this.colorReg3 = value & 0x0F;
	break;

      case 0x60:
	this.colorReg4 = value & 0x0F;
	break;
    }
    if( configChanged ) {
      this.screenFrm.clearScreenSelection();
      this.screenFrm.fireUpdScreenTextActionsEnabled();
    }
    this.screenFrm.setScreenDirty( true );
  }


	/* --- GDC82720.GDCListener --- */

  @Override
  public void screenConfigChanged( GDC82720 gdc )
  {
    this.screenFrm.clearScreenSelection();
    this.screenFrm.fireUpdScreenTextActionsEnabled();
  }


  @Override
  public void screenDirty( GDC82720 gdc )
  {
    this.screenFrm.setScreenDirty( true );
  }


	/* --- GDC82720.VRAM --- */

  @Override
  public int getVRAMWord( int addr )
  {
    return (int) this.vram[ addr & 0xFFFF ] & 0xFFFF;
  }


  @Override
  public void setVRAMWord( int addr, int value )
  {
    this.vram[ addr & 0xFFFF ]= (short) value;
    this.screenFrm.setScreenDirty( true );
  }


	/* --- private Methoden --- */

  private BufferedImage getScreenImage()
  {
    BufferedImage img = this.screenImage;
    synchronized( this.colors ) {
      int height = this.gdc.getDisplayLines();
      int width  = this.screenWidth;
      if( img != null ) {
	if( (img.getHeight() != height) || (img.getWidth() != width) ) {
	  img = null;
	}
      }
      if( (img == null) && (height > 0) ) {
	if( width > 0 ) {
	  IndexColorModel cm = this.colorModel;
	  if( cm != null ) {
	    img = new BufferedImage(
				width,
				height,
				BufferedImage.TYPE_BYTE_BINARY,
				cm );
	  } else {
	    img = new BufferedImage(
				width,
				height,
				BufferedImage.TYPE_3BYTE_BGR );
	  }
	  this.screenImage = img;
	}
      }
    }
    return img;
  }


  private void updScreenWidth()
  {
    int oldWidth = getScreenWidth();
    int newWidth = (this.w640 ? 640 : 320);
    if( this.mode == 2 ) {
      // Im Mode 2 laeuft VIS mit halben Bildpunkttakt
      newWidth /= 2;
    }
    this.screenWidth = newWidth;
    if( (newWidth != oldWidth) && !this.fixedScreenSize ) {
      this.screenFrm.fireScreenSizeChanged();
    }
  }
}
