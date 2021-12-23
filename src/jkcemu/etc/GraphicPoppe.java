/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Grafikkarte von Heiko Poppe
 */

package jkcemu.etc;

import java.awt.Color;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.AbstractScreenFrm;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileUtil;


public class GraphicPoppe extends AbstractScreenDevice
{
  private static byte[] ascii8x6 = null;

  private EmuSys    emuSys;
  private boolean   defaultMode1;
  private boolean   altFontSelected;
  private boolean   colorRamSelected;
  private boolean   fillInterspace;
  private boolean   fixedScreenSize;
  private boolean   mode64x32;
  private int       mode64x32XOffs;
  private int       mode64x32YOffs;
  private int       mode1XOffs;
  private int       mode1YOffs;
  private int       mode1ScreenW;
  private int       mode1ScreenH;
  private int       mode1RamOffs;
  private byte[]    fontBytes8x6;
  private byte[]    fontBytes8x8;
  private byte[]    ramText;
  private byte[]    ramColor;
  private Color[]   colors;


  public GraphicPoppe(
		EmuSys     emuSys,
		boolean    defaultMode1,
		Properties props )
  {
    super( props );
    this.emuSys          = emuSys;
    this.defaultMode1    = defaultMode1;
    this.mode64x32       = !defaultMode1;
    this.fixedScreenSize = false;
    if( ascii8x6 == null ) {
      ascii8x6 = EmuUtil.readResource(
				this.emuSys.getScreenFrm(),
				"/rom/etc/ascii8x6.bin" );
    }
    this.fontBytes8x6    = ascii8x6;
    this.fontBytes8x8    = ascii8x6;
    this.mode64x32XOffs  = 0;
    this.mode64x32YOffs  = 0;
    this.mode1ScreenW    = getMode1Cols() * 8;
    this.mode1ScreenH    = getMode1Rows() * getMode1RowHeight();
    this.mode1XOffs      = 0;
    this.mode1YOffs      = 0;
    this.mode1RamOffs    = 0;
    this.ramText         = new byte[ 0x800 ];
    this.ramColor        = new byte[ 0x800 ];
    this.colors          = new Color[ 16 ];
    createColors( props );
    reset( true, props );
  }


  public void appendStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<tr><td>Bildschirmformat:</td><td>" );
    buf.append( this.mode64x32 ?
			"64x32"
			: String.format(
				"%dx%d",
				getMode1Cols(),
				getMode1Rows() ) );
    buf.append( "</td></tr>\n" );
    if( this.mode64x32 ) {
      buf.append( "<tr><td>Zeichensatz:</td><td>" );
      buf.append( this.altFontSelected ? "2" : "1 (Standard)" );
      buf.append( "</td></tr>\n" );
    } else {
      if( getMode1RowHeight() > 8 ) {
	buf.append( "<tr><td>Zeilenzwischenraum:</td><td>" );
	buf.append(
		this.fillInterspace ? "Blockgrafik" : "Hintergrundfarbe" );
	buf.append( "</td></tr>\n" );
      }
    }
    int videoAddr = getVideoBegAddr();
    buf.append(
	String.format(
		"<tr><td>%04Xh-%04Xh:</td><td>",
		videoAddr,
		videoAddr + 0x7FF ) );
    if( this.colorRamSelected ) {
      buf.append( "Farb" );
    } else {
      buf.append( "Text" );
    }
    buf.append( "-RAM</td></tr>\n" );
  }


  public byte[] getCurFontBytes()
  {
    return this.mode64x32 ? this.fontBytes8x6 : this.fontBytes8x8;
  }


  public int getMode1Cols()
  {
    return 80;
  }


  public int getMode1RowHeight()
  {
    return 10;
  }


  public int getMode1Rows()
  {
    return 25;
  }


  public int getMode1VideoAddrOffs()
  {
    return 0;
  }


  public int getVideoBegAddr()
  {
    return 0xF800;
  }


  public void load8x6FontByProperty( Properties props, String propName )
  {
    set8x6FontBytes(
		FileUtil.readFile(
			this.emuSys.getScreenFrm(),
			props.getProperty( propName ),
			true,
			0x2000,
			"Zeichensatzdatei f\u00FCr Modus 64x32" ) );
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0x0F) == 0x02 ) {
      rv = 0xF0;
      if( this.colorRamSelected ) {
	rv |= 0x01;
      }
      if( !this.mode64x32 ) {
	rv |= 0x02;
      }
      if( this.fillInterspace ) {
	rv |= 0x04;
      }
      if( this.altFontSelected ) {
	rv |= 0x08;
      }
    }
    return rv;
  }


  public int readMemByte( int addr )
  {
    int rv  = 0xFF;
    int idx = addr - getVideoBegAddr();
    if( this.colorRamSelected ) {
      if( (idx >= 0) && (idx < this.ramColor.length) ) {
	rv = (int) this.ramColor[ idx ] & 0xFF;
      }
    } else {
      if( (idx >= 0) && (idx < this.ramText.length) ) {
	rv = (int) this.ramText[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  public void reset( boolean powerOn, Properties props )
  {
    this.altFontSelected  = false;
    this.colorRamSelected = false;
    this.fillInterspace   = false;
    setMode64x32( !this.defaultMode1 );
    if( powerOn ) {
      EmuUtil.initSRAM( this.ramText, props );
      EmuUtil.initSRAM( this.ramColor, props );
      setScreenDirty( true );
    }
  }


  public void setFixedScreenSize( boolean state )
  {
    if( state != this.fixedScreenSize ) {
      this.fixedScreenSize = state;
      if( state ) {
	if( this.mode1ScreenW > 384 ) {
	  this.mode64x32XOffs = (this.mode1ScreenW - 384) / 2;
	  this.mode1XOffs     = 0;
	} else {
	  this.mode64x32XOffs = 0;
	  this.mode1XOffs     = (384 - this.mode1ScreenW) / 2;
	}
	if( this.mode1ScreenH > 256 ) {
	  this.mode64x32YOffs = (this.mode1ScreenH - 256) / 2;
	  this.mode1YOffs     = 0;
	} else {
	  this.mode64x32YOffs = 0;
	  this.mode1YOffs     = (256 - this.mode1ScreenH) / 2;
	}
      } else {
	this.mode64x32XOffs = 0;
	this.mode64x32YOffs = 0;
	this.mode1XOffs     = 0;
	this.mode1YOffs     = 0;
      }
      fireScreenSizeChanged();
    }
  }


  public void set8x6FontBytes( byte[] fontBytes )
  {
    this.fontBytes8x6 = fontBytes;
    if( this.fontBytes8x6 == null ) {
      this.fontBytes8x6 = ascii8x6;
    }
    setScreenDirty( true );
  }


  public void set8x8FontBytes( byte[] fontBytes )
  {
    this.fontBytes8x8 = fontBytes;
    if( this.fontBytes8x8 == null ) {
      this.fontBytes8x8 = ascii8x6;
    }
    setScreenDirty( true );
  }


  public void writeIOByte( int port, int value )
  {
    switch( port & 0x0F ) {
      case 0x01:
	this.altFontSelected = ((value & 0x08) != 0);
	this.fillInterspace  = ((value & 0x04) != 0);
	setMode64x32( (value & 0x02) == 0 );
	setScreenDirty( true );
	break;
      case 0x02:
	this.colorRamSelected = ((value & 0x01) != 0);
	break;
    }
  }


  public boolean writeMemByte( int addr, int value )
  {
    boolean rv  = false;
    int     idx = addr - getVideoBegAddr();
    if( this.colorRamSelected ) {
      if( (idx >= 0) && (idx < this.ramColor.length) ) {
	this.ramColor[ idx ] = (byte) value;
	setScreenDirty( true );
	rv = true;
      }
    } else {
      if( (idx >= 0) && (idx < this.ramText.length) ) {
	this.ramText[ idx ] = (byte) value;
	setScreenDirty( true );
	rv = true;
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    createColors( props );
  }


  @Override
  public void cancelPastingText()
  {
    this.emuSys.cancelPastingText();
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.BLACK;
    if( (colorIdx >= 0) && (colorIdx < this.colors.length) ) {
      color = this.colors[ colorIdx ];
    } else {
      color = this.colorWhite;
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return this.colors.length;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int    rv            = BLACK;
    int    fontOffs      = 0;
    int    pixPerCol     = 8;
    int    pixPerRow     = 0;
    int    colsPerRow    = 0;
    int    charsOnScreen = 0;
    int    addrOffs      = 0;
    byte[] fontBytes     = this.fontBytes8x8;
    if( this.mode64x32 ) {
      if( this.fixedScreenSize || isFullScreenMode() ) {
	x -= this.mode64x32XOffs;
	if( x > 384 ) {
	  x = -1;
	}
	y -= this.mode64x32YOffs;
	if( y > 256 ) {
	  y = -1;
	}
      }
      fontBytes     = this.fontBytes8x6;
      pixPerCol     = 6;
      pixPerRow     = 8;
      colsPerRow    = 64;
      charsOnScreen = 2048;
      if( this.altFontSelected && (fontBytes != null) ) {
	if( fontBytes.length > 0x0800 ) {
	  fontOffs = 0x0800;
	}
      }
    } else {
      if( this.fixedScreenSize || isFullScreenMode() ) {
	x -= this.mode1XOffs;
	if( x > this.mode1ScreenW ) {
	  x = -1;
	}
	y -= this.mode1YOffs;
	if( y > this.mode1ScreenH ) {
	  y = -1;
	}
      }
      pixPerCol     = 8;
      pixPerRow     = getMode1RowHeight();
      colsPerRow    = getMode1Cols();
      charsOnScreen = colsPerRow * getMode1Rows();
      addrOffs      = getMode1VideoAddrOffs();
    }
    if( (x >= 0) && (y >= 0) && (fontBytes != null) ) {
      int rPix = y % pixPerRow;
      int row  = y / pixPerRow;
      int col  = x / pixPerCol;
      if( (rPix >= 8) && this.fillInterspace ) {
	rPix -= 8;
	fontOffs = 0x0800;
      }
      int mIdx = (row * colsPerRow) + col;
      if( (mIdx >= 0) && (mIdx < charsOnScreen) ) {
	mIdx += addrOffs;
	if( mIdx < this.ramColor.length ) {
	  rv = (int) this.ramColor[ mIdx ] & 0xFF;
	}
	if( rPix < 8 ) {
	  int ch = 0;
	  if( mIdx < this.ramText.length ) {
	    ch = (int) this.ramText[ mIdx ] & 0xFF;
	  }
	  int fIdx = fontOffs + (ch * 8) + rPix;
	  if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
	    int m = 0x80;
	    int n = x % pixPerCol;
	    if( n > 0 ) {
	      m >>= n;
	    }
	    if( (fontBytes[ fIdx ] & m) == 0 ) {
	      rv >>= 4;
	    }
	  }
	} else {
	  rv >>= 4;
	}
	rv &= 0x0F;
      }
    }
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return this.mode64x32 ?
		new CharRaster(
			64,
			32,
			8,
			6,
			8,
			this.mode64x32XOffs,
			this.mode64x32YOffs )
		: new CharRaster(
			getMode1Cols(),
			getMode1Rows(),
			getMode1RowHeight(),
			8,
			8,
			this.mode1XOffs,
			this.mode1YOffs );
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.emuSys.getEmuThread();
  }


  @Override
  public int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch    = -1;
    int nCols = 0;
    int nRows = 0;
    if( this.mode64x32 ) {
      nCols = 64;
      nRows = 32;
    } else {
      nCols = 80;
      nRows = 24;
    }
    if( (chX >= 0) && (chX < nCols) && (chY >= 0) && (chY < nRows) ) {
      int idx = (chY * nCols) + chX;
      if( idx < this.ramText.length ) {
	int b = (int) this.ramText[ idx ] & 0xFF;
	if( b >= 0x20 ) {
	  ch = b;
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return this.fixedScreenSize ?
		Math.max( 256, this.mode1ScreenH )
		: (this.mode64x32 ? 256 : this.mode1ScreenH);
  }


  @Override
  public int getScreenWidth()
  {
    return this.fixedScreenSize ?
		Math.max( 384, this.mode1ScreenW )
		: (this.mode64x32 ? 384 : this.mode1ScreenW);
  }


  @Override
  public String getTitle()
  {
    return String.format(
		"%s: Farbgrafikkarte %dx%d/64x32",
		Main.APPNAME,
		getMode1Cols(),
		getMode1Rows() );
  }


  @Override
  public void informPastingTextStatusChanged( boolean pasting )
  {
    this.emuSys.informPastingTextStatusChanged( pasting );
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
    return true;
  }


	/* --- private Methoden --- */

  private void createColors( Properties props )
  {
    float f = getBrightness( props );
    if( (f >= 0F) && (f <= 1F) ) {
      for( int i = 0; i < this.colors.length; i++ ) {
	int v = Math.round( ((i & 0x08) != 0 ? 0xFF : 0xBF) * f );
	this.colors[ i ] = new Color(
				(i & 0x01) != 0 ? v : 0,
				(i & 0x02) != 0 ? v : 0,
				(i & 0x04) != 0 ? v : 0 );
      } 
    }
  }


  private void setMode64x32( boolean state )
  {
    if( state != this.mode64x32 ) {
      this.mode64x32              = state;
      AbstractScreenFrm screenFrm = getScreenFrm();
      if( screenFrm != null ) {
	screenFrm.fireScreenSizeChanged();
      }
    }
  }
}
