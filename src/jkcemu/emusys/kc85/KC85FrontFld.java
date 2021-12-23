/*
 * (c) 2017-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige der KC85-Front
 */

package jkcemu.emusys.kc85;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.EmuThread;
import jkcemu.emusys.KC85;
import jkcemu.image.ImageUtil;


public class KC85FrontFld extends AbstractScreenDevice
{
  public static final int RGB_LED_NONE   = 0x00000000;
  public static final int RGB_LED_DARK   = 0xFF303030;
  public static final int RGB_LED_RED    = 0xFFC00000;
  public static final int RGB_LED_GREEN  = 0xFF00C000;
  public static final int RGB_LED_YELLOW = 0xFFB0B000;

  private static final int DEVICE_W          = 500;
  private static final int DEVICE_H          = 100;
  private static final int DEVICE_TITLE_X    = 412;
  private static final int DEVICE_TITLE_Y    = 20;
  private static final int DEVICE_SUBTITLE_Y = 30;
  private static final int LABEL_M_H         = 7;
  private static final int LABEL_S_H         = 5;
  private static final int TITLE_H           = 12;
  private static final int LED_R             = 4;
  private static final int LED_W             = 2 * LED_R;
  private static final int MODULE_W          = 171;
  private static final int MODULE_H          = 31;
  private static final int MODULE_X1         = 20;
  private static final int MODULE_X2         = 206;
  private static final int MODULE_Y1         = 6;
  private static final int MODULE_Y2         = 46;
  private static final int RGB_BACKGROUND    = 0xFFC0C0C0;

  private static final String TEXT_BASIS_DEVICE = "BASIS DEVICE";
  private static final String TEXT_KEYBOARD     = "KEYBOARD";
  private static final String TEXT_MHZ          = "MHz";
  private static final String TEXT_POWER        = "POWER";
  private static final String TEXT_RESET        = "RESET";
  private static final String TEXT_SYSTEM       = "SYSTEM";
  private static final String TEXT_TAPE         = "TAPE";

  private static BufferedImage loadedImgD001Dark  = null;
  private static BufferedImage loadedImgD001Light = null;
  private static BufferedImage loadedImgD002      = null;
  private static BufferedImage loadedImgD004      = null;
  private static BufferedImage loadedImgModule    = null;

  private KC85                            kc85;
  private D004                            d004;
  private int                             nD002s;
  private int                             kcTypeNum;
  private int                             bgColorIdx;
  private int                             pioAValue;
  private boolean                         ram8Enabled;
  private Font                            fontLabelM;
  private Font                            fontLabelS;
  private Font                            fontTitle;
  private Image                           imgD001;
  private Image                           imgD002;
  private Image                           imgD004;
  private Image                           imgModule;
  private Color                           colorLEDGreen;
  private Color                           colorLEDDark;
  private Color[]                         colors;
  private SortedSet<Integer>              rgbs;
  private Map<Integer,AbstractKC85Module> slot2Module;


  public KC85FrontFld(
		KC85                 kc85,
		D004                 d004,
		AbstractKC85Module[] modules,
		Properties           props )
  {
    super( props );
    this.kc85          = kc85;
    this.d004          = d004;
    this.nD002s        = 0;
    this.kcTypeNum     = kc85.getKCTypeNum();
    this.pioAValue     = 0;
    this.ram8Enabled   = false;
    this.fontTitle     = new Font( Font.SANS_SERIF, Font.BOLD, TITLE_H );
    this.fontLabelM    = new Font( Font.SANS_SERIF, Font.BOLD, LABEL_M_H );
    this.fontLabelS    = new Font( Font.SANS_SERIF, Font.BOLD, LABEL_S_H );
    this.imgD001       = null;
    this.imgD002       = null;
    this.imgD004       = null;
    this.colorLEDDark  = new Color( RGB_LED_DARK );
    this.colorLEDGreen = new Color( RGB_LED_GREEN );
    this.colors        = null;
    this.bgColorIdx    = 0;
    this.rgbs          = new TreeSet<>();
    this.rgbs.add( 0xFF000000 );		// schwarz
    this.rgbs.add( 0xFFFFFFFF );		// weiss
    this.rgbs.add( RGB_BACKGROUND );		// Hintergrundfarbe

    this.slot2Module = new HashMap<>();
    if( modules != null ) {
      int maxSlot = 0;
      for( AbstractKC85Module module : modules ) {
	int slot = module.getSlot();
	if( (slot > maxSlot) && (slot < 0xF0) ) {
	  maxSlot = slot;
	}
	this.slot2Module.put( slot, module );
      }
      this.nD002s = maxSlot / 16;

      if( loadedImgModule == null ) {
	loadedImgModule = loadImage( "/images/emusys/kc85/module.png" );
      }
      imgModule = prepareImage( loadedImgModule );
    }

    if( this.kcTypeNum >= 4 ) {
      if( loadedImgD001Light == null ) {
	loadedImgD001Light = loadImage(
				"/images/emusys/kc85/d001_light.png" );
      }
      imgD001 = prepareImage( loadedImgD001Light );
    } else {
      if( loadedImgD001Dark == null ) {
	loadedImgD001Dark = loadImage(
				"/images/emusys/kc85/d001_dark.png" );
      }
      imgD001 = prepareImage( loadedImgD001Dark );
    }
    if( this.nD002s > 0 ) {
      if( loadedImgD002 == null ) {
	loadedImgD002 = loadImage( "/images/emusys/kc85/d002.png" );
      }
      imgD002 = prepareImage( loadedImgD002 );
    }
    if( this.d004 != null ) {
      if( loadedImgD004 == null ) {
	loadedImgD004 = loadImage( "/images/emusys/kc85/d004.png" );
      }
      imgD004 = prepareImage( loadedImgD004 );
    }
  }


  public void setPioAValue( int value )
  {
    value &= (this.kcTypeNum >= 4 ? 0x20 : 0x27);
    if( value != this.pioAValue ) {
      this.pioAValue = value;
      setScreenDirty( true );
    }
  }


  public void setRAM8Enabled( boolean state )
  {
    if( this.ram8Enabled != state ) {
      this.ram8Enabled = state;
      setScreenDirty( true );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized int getBorderColorIndex()
  {
    ensureColorsCreated();
    return this.bgColorIdx;
  }


  @Override
  public synchronized Color getColor( int colorIdx )
  {
    Color color = null;
    if( this.colors != null ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) ) {
	color = this.colors[ colorIdx ];
      }
    }
    return color != null ? color : Color.BLACK;
  }


  @Override
  public synchronized int getColorCount()
  {
    ensureColorsCreated();
    return this.colors.length;
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.kc85.getEmuThread();
  }


  @Override
  public int getScreenHeight()
  {
    int n = this.nD002s + 1;
    if( this.d004 != null ) {
      n++;
    }
    return n * DEVICE_H;
  }


  @Override
  public int getScreenWidth()
  {
    return DEVICE_W;
  }


  @Override
  public String getTitle()
  {
    return String.format(
		"%s: %s System",
		Main.APPNAME,
		this.kc85.getTitle() );
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    g.translate( x, y );
    y = 0;

    ImageObserver observer = getScreenFrm();

    // Skalierung
    if( screenScale > 1 ) {
      if( g instanceof Graphics2D ) {
	((Graphics2D) g).scale( (double) screenScale, (double) screenScale );
      }
    }

    // D004
    D004 d004 = this.d004;
    if( d004 != null ) {
      drawD004( g, d004, observer );
      y += DEVICE_H;
    }

    // D002s
    int slot = this.nD002s * 16;
    for( int i = this.nD002s - 1; i >= 0; --i ) {
      drawD002( g, y, slot, observer );
      slot -= 16;
      y += DEVICE_H;
    }

    // Grundgeraet
    if( this.kcTypeNum >= 4 ) {
      drawD001Light( g, y, observer );
    } else {
      drawD001Dark( g, y, observer );
    }

    return true;
  }


	/* --- private Methoden --- */

  private void drawD001Light( Graphics g, int y, ImageObserver observer )
  {
    g.setColor( Color.BLACK );
    drawDevice(
	g,
	y,
	imgD001,
	observer,
	this.kc85.getTitle(),
	TEXT_BASIS_DEVICE );

    // Anschluesse
    g.fillArc( 66, y + 53, 18, 18, 0, 360 );
    g.fillArc( 127, y + 53, 18, 18, 0, 360 );

    // Beschriftungen
    g.setFont( this.fontLabelM );
    drawDeviceLightLabel( g, 80, y + 69, 9, TEXT_TAPE );
    drawDeviceLightLabel( g, 141, y + 69, 9, TEXT_KEYBOARD );
    drawDeviceLightLabel( g, 231, y + 66, 12, TEXT_SYSTEM );
    drawDeviceLightLabel( g, 288, y + 66, 12, "RAM8" );
    drawDeviceLightLabel( g, 352, y + 67, 11, TEXT_RESET );
    drawDeviceLightLabel( g, 435, y + 67, 11, TEXT_POWER );

    // LEDs
    drawLED(
	g,
	225,
	y + 59,
	(this.pioAValue & 0x20) != 0 ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    drawLED(
	g,
	282,
	y + 59,
	this.ram8Enabled ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    drawLED( g, 390, y + 59, RGB_LED_RED, true );

    // Module
    drawModule( g, y, 0, 0, 0x0C, observer );
    drawModule( g, y, 1, 0, 0x08, observer );
  }


  private void drawD001Dark( Graphics g, int y, ImageObserver observer )
  {
    g.setColor( Color.WHITE );
    drawDevice(
	g,
	y,
	imgD001,
	observer,
	this.kc85.getTitle(),
	TEXT_BASIS_DEVICE );

    // Anschluesse
    g.setColor( Color.BLACK );
    g.fillArc( 66, y + 57, 18, 18, 0, 360 );
    g.fillArc( 150, y + 62, 8, 8, 0, 360 );

    // Beschriftungen
    g.setFont( this.fontLabelM );
    g.drawString( TEXT_TAPE, 65, y + 49 );
    g.drawString( TEXT_KEYBOARD, 134, y + 49 );
    g.drawString( "MEMORY SELECTION", 225, y + 49 );
    g.drawString( TEXT_RESET, 329, y + 49 );
    g.drawString( TEXT_POWER, 411, y + 49 );
    g.setFont( this.fontLabelS );
    g.drawString( "ROM", 223, y + 77 );
    g.drawString( "RAM", 256, y + 77 );
    g.drawString( "IRM", 289, y + 77 );
    g.drawString( "ON/OFF", 414, y + 77 );

    // LEDs
    drawLED(
	g,
	102,
	y + 63,
	(this.pioAValue & 0x20) != 0 ? RGB_LED_YELLOW : RGB_LED_DARK,
	true );
    drawLED(
	g,
	225,
	y + 63,
	(this.pioAValue & 0x01) != 0 ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    drawLED(
	g,
	258,
	y + 63,
	(this.pioAValue & 0x02) != 0 ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    drawLED(
	g,
	291,
	y + 63,
	(this.pioAValue & 0x04) != 0 ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    drawLED( g, 390, y + 63, RGB_LED_RED, true );

    // Module
    drawModule( g, y, 0, 0, 0x0C, observer );
    drawModule( g, y, 1, 0, 0x08, observer );
  }


  private void drawD002( Graphics g, int y, int slot, ImageObserver observer )
  {
    g.setColor( Color.BLACK );
    drawDevice( g, y, imgD002, observer, "KC85", "BUSDRIVER" );
    g.setFont( this.fontLabelM );
    drawDeviceLightLabel( g, 435, y + 67, 11, TEXT_POWER );
    drawLED( g, 390, y + 59, RGB_LED_RED, true );
    drawModule( g, y, 0, 0, slot + 0x0C, observer );
    drawModule( g, y, 1, 0, slot + 0x08, observer );
    drawModule( g, y, 0, 1, slot + 0x04, observer );
    drawModule( g, y, 1, 1, slot, observer );
  }


  private void drawD004( Graphics g, D004 d004, ImageObserver observer )
  {
    g.setColor( Color.BLACK );
    drawDevice( g, 0, imgD004, observer, "KC85", "FLOPPY DISK BASIS" );
    if( this.d004 instanceof D008 ) {
      g.setFont( this.fontLabelM );
      g.drawString( TEXT_MHZ, 5, 31 );

      int x7Seg = 26;
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	int w = fm.stringWidth( TEXT_MHZ );
	if( w > 0 ) {
	  x7Seg = 7 + w;
	}
      }

      Graphics g7Seg = g.create( x7Seg, 4, 36, 27 );
      g7Seg.fillRect( 0, 0, 45, 27 );
      if( g7Seg instanceof Graphics2D ) {
	((Graphics2D) g7Seg).scale( 0.27, 0.27 );

	// Taktfrequenz
	int v7Seg = 0x7F;
	if( ((D008) d004).isMaxSpeed4MHz() ) {
	  v7Seg = 0x66;
	}
	if( d004.isEnabled() ) {
	  v7Seg |= 0x80;
	}
	paint7SegDigit(
		g7Seg,
		8,
		8,
		v7Seg,
		this.colorLEDDark,
		this.colorLEDGreen,
		1 );

	// ROM-Bank
	v7Seg = 0;
	switch( ((D008) d004).getROMBank() ) {
	  case 0:
	    v7Seg = 0x3F;
	    break;
	  case 1:
	    v7Seg = 0x06;
	    break;
	  case 2:
	    v7Seg = 0x5B;
	    break;
	  case 3:
	    v7Seg = 0x4F;
	    break;
	}
	paint7SegDigit(
		g7Seg,
		70,
		8,
		v7Seg,
		this.colorLEDDark,
		this.colorLEDGreen,
		1 );
      }
      g7Seg.dispose();

      g.setColor( Color.BLACK );
      g.setFont( this.fontLabelM );
      g.drawString( "ROM", x7Seg + 38, 31 );

      drawDeviceLightLabel( g, 100, 19, 12, "GIDE" );
      drawDeviceLightLabel( g, 165, 19, 12, "RAMDISK" );
      drawLED(
	g,
	94,
	12,
	((D008) d004).isLEDofGIDEon() ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
      drawLED(
	g,
	159,
	12,
	((D008) d004).isLEDofRamFloppyOn() ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    }

    g.setColor( Color.BLACK );
    g.setFont( this.fontLabelM );
    drawDeviceLightLabel( g, 230, 19, 12, "CONNECTION" );
    drawDeviceLightLabel( g, 295, 19, 12, TEXT_SYSTEM );
    drawLED(
	g,
	224,
	12,
	this.d004.isConnected() ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    drawLED(
	g,
	289,
	12,
	this.d004.isRunning() ? RGB_LED_GREEN : RGB_LED_DARK,
	true );

    if( this.d004 instanceof D008 ) {
      g.setColor( Color.BLACK );
      drawDeviceLightLabel( g, 360, 19, 12, "DMA" );
      drawLED(
	g,
	354,
	12,
	((D008) d004).isLEDofDMAon() ? RGB_LED_GREEN : RGB_LED_DARK,
	true );
    }

    g.setColor( Color.BLACK );
    drawDeviceLightLabel( g, 435, 67, 11, TEXT_POWER );
    drawLED( g, 390, 59, RGB_LED_RED, true );
    drawModule( g, 0, 0, 1, 0xF4, observer );
    drawModule( g, 0, 1, 1, 0xF0, observer );
  }


  private void drawDevice(
		Graphics      g,
		int           y,
		Image         imgBg,
		ImageObserver observer,
		String        deviceTitle,
		String        deviceSubtitle )
  {
    if( (imgBg != null) && (observer != null) ) {
      g.drawImage( imgBg, 0, y, observer );
    }
    g.setFont( this.fontTitle );
    g.drawString( deviceTitle, DEVICE_TITLE_X, y + DEVICE_TITLE_Y );
    g.setFont( this.fontLabelM );
    g.drawString( deviceSubtitle, DEVICE_TITLE_X, y + DEVICE_SUBTITLE_Y );
  }


  private void drawDeviceLightLabel(
			Graphics g,
			int      x1,
			int      y1,
			int      d,
			String   text )
  {
    int x2 = x1 + d;
    int y2 = y1 + d;
    g.drawString( text, x2 + 1, y2 - 2 );
    FontMetrics fm = g.getFontMetrics();
    if( fm != null ) {
      int w = fm.stringWidth( text );
      if( w > 0 ) {
	g.drawLine( x1, y1, x2, y2 );
	g.drawLine( x2, y2, x2 + w, y2 );
      }
    }
  }


  private void drawLED(
			Graphics g,
			int      x,
			int      y,
			int      v,
			boolean  margin )
  {
    if( margin ) {
      g.setColor( Color.BLACK );
      g.fillArc( x - 2, y - 2, LED_W + 2, LED_W + 2, 0, 360 );
    }
    if( (v & 0xFF000000) != 0 ) {
      setColor(
	g,
	(v >> 16) & 0xFF,
	(v >> 8) & 0xFF,
	v & 0xFF );
      g.fillArc( x, y, LED_W - 2, LED_W - 2, 0, 360 );
    }
  }


  private void drawModule(
			Graphics      g,
			int           yDevice,
			int           xPos,
			int           yPos,
			int           slot,
			ImageObserver observer )
  {
    int x = (xPos == 0 ? MODULE_X1 : MODULE_X2);
    int y = yDevice + (yPos == 0 ? MODULE_Y1 : MODULE_Y2);

    AbstractKC85Module module = this.slot2Module.get( slot );
    if( module != null ) {
      if( (imgModule != null) && (observer != null) ) {
	g.drawImage( imgModule, x, y, observer );
      }
      String s = module.getExternalModuleName();
      if( s != null ) {
	g.setColor( Color.WHITE );
	g.setFont( this.fontTitle );
	g.drawString(
		s,
		x + (MODULE_W * 6 / 10),
		y + ((MODULE_H - TITLE_H) / 2) + TITLE_H - 1 );
	int xLED = x + LED_W + LED_R;
	for( int i = 0; i < 4; i++ ) {
	  module = this.slot2Module.get( slot + i );
	  if( module != null ) {
	    drawLED(
		g,
		xLED,
		y + (MODULE_H / 2) - (LED_R / 2),
		module.getLEDrgb(),
		false );
	  }
	  xLED += (LED_W + LED_R);
	}
      }
    }
  }


  private void ensureColorsCreated()
  {
    if( this.colors == null ) {
      int n       = this.rgbs.size();
      this.colors = new Color[ n ];
      int idx     = 0;
      for( Integer rgb : this.rgbs ) {
	if( rgb == RGB_BACKGROUND ) {
	  this.bgColorIdx = idx;
	}
	this.colors[ idx++ ] = new Color( rgb );
      }
    }
  }


  private BufferedImage loadImage( String resource )
  {
    BufferedImage img = null;
    try {
      URL url = getClass().getResource( resource );
      if( url != null ) {
	img = ImageIO.read( url );
      }
    }
    catch( Exception ex ) {}
    return img;
  }


  private synchronized BufferedImage prepareImage( BufferedImage img )
  {
    if( img != null ) {
      IndexColorModel icm = ImageUtil.getIndexColorModel( img );
      if( icm != null ) {
	int n = icm.getMapSize();
	for( int i = 0; i < n; i++ ) {
	  this.rgbs.add( icm.getRGB( i ) );
	}
      } else {
	int w = img.getWidth();
	int h = img.getHeight();
	for( int y = 0; y < h; y++ ) {
	  for( int x = 0; x < w; x++ ) {
	    this.rgbs.add( img.getRGB( x, y ) );
	  }
	}
      }
      this.colors = null;
    }
    return img;
  }


  private synchronized void setColor( 
				Graphics g,
				int      red,
				int      green,
				int      blue )
  {
    Color color = new Color( red, green, blue );
    if( this.rgbs.size() < 256 ) {
      this.rgbs.add( color.getRGB() );
      this.colors = null;
    }
    g.setColor( color );
  }
}
