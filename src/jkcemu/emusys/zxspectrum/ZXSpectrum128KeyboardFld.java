/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Tastatur des ZX Spectrum+ 128K
 */

package jkcemu.emusys.zxspectrum;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.ZXSpectrum;


public class ZXSpectrum128KeyboardFld extends AbstractKeyboardFld<ZXSpectrum>
{
  private static final String KEY_TEXT_CAPS_SHIFT   = "\nCAPS SHIFT";
  private static final String KEY_TEXT_SYMBOL_SHIFT = "SYMBOL\nSHIFT";

  private static final int KEY_H            = 60;
  private static final int KEY_W            = 60;
  private static final int KEY_EDIT_W       = KEY_W * 5 / 4;
  private static final int KEY_LARGE_W      = KEY_W * 3 / 2;
  private static final int KEY_CAPS_SHIFT_W = KEY_W * 9 / 4;
  private static final int KEY_SPACE_W      = KEY_W * 9 / 2;
  private static final int KEY_ENTER_W      = 105;
  private static final int KEY_ENTER_H      = 120;
  private static final int FONT_HUGE_H      = 18;
  private static final int FONT_LARGE_H     = 14;
  private static final int FONT_STD_H       = 10;
  private static final int KEY_X1           = KEY_W / 2;
  private static final int KEY_Y1           = KEY_H;
  private static final int CAPS_SHIFT_COL   = 0;
  private static final int CAPS_SHIFT_VALUE = 0x01;
  private static final int SYM_SHIFT_COL    = 7;
  private static final int SYM_SHIFT_VALUE  = 0x02;

  private Color        colorBg;
  private Color        colorSelected;
  private Color        colorShadow;
  private Color        colorLight;
  private Color        colorLogo;
  private Font         fontHuge;
  private Font         fontLarge;
  private Font         fontStd;
  private Polygon      polygonEnter;
  private Image        imgG1;
  private Image        imgG2;
  private Image        imgG3;
  private Image        imgG4;
  private Image        imgG5;
  private Image        imgG6;
  private Image        imgG7;
  private Image        imgG8;
  private Image        imgKeyEnter;
  private Image        imgLeft;
  private Image        imgRight;
  private Image        imgUp;
  private Image        imgDown;
  private Image        imgKeyCapsShift;
  private Image        imgKeyEdit;
  private Image        imgKeyLarge;
  private Image        imgKeySpace;
  private Image        imgKeyStd;
  private Image        imgLogo128k;
  private Set<KeyData> capsShiftLinks;
  private Set<KeyData> symShiftLinks;
  private KeyData      keyCapsShift1;
  private KeyData      keyCapsShift2;
  private KeyData      keySymShift1;
  private KeyData      keySymShift2;
  private KeyData      keyEnter;
  private int[]        kbMatrix;
  private int          curIdx;
  private int          curX;
  private int          curY;


  public ZXSpectrum128KeyboardFld( ZXSpectrum spectrum )
  {
    super( spectrum, 58, false );
    this.colorBg         = new Color( 0xFF404040 );
    this.colorSelected   = new Color( 0xC0303030 );
    this.colorShadow     = new Color( 0xFF000000 );
    this.colorLight      = new Color( 0xFF808080 );
    this.colorLogo       = Color.RED;
    this.imgG1           = getImage( "g1.png" );
    this.imgG2           = getImage( "g2.png" );
    this.imgG3           = getImage( "g3.png" );
    this.imgG4           = getImage( "g4.png" );
    this.imgG5           = getImage( "g5.png" );
    this.imgG6           = getImage( "g6.png" );
    this.imgG7           = getImage( "g7.png" );
    this.imgG8           = getImage( "g8.png" );
    this.imgLeft         = getImage( "left.png" );
    this.imgRight        = getImage( "right.png" );
    this.imgUp           = getImage( "up.png" );
    this.imgDown         = getImage( "down.png" );
    this.imgKeyEnter     = getImage( "key_enter.png" );
    this.imgKeyEdit      = getImage( "key75.png" );
    this.imgKeyLarge     = getImage( "key90.png" );
    this.imgKeyCapsShift = getImage( "key135.png" );
    this.imgKeySpace     = getImage( "key270.png" );
    this.imgKeyStd       = getImage( "key60.png" );
    this.imgLogo128k     = getImage( "logo128k.png" );
    this.capsShiftLinks  = new HashSet<>();
    this.symShiftLinks   = new HashSet<>();
    this.fontHuge  = new Font( Font.SANS_SERIF, Font.BOLD, FONT_HUGE_H );
    this.fontLarge = new Font( Font.SANS_SERIF, Font.PLAIN, FONT_LARGE_H );
    this.fontStd   = new Font( Font.SANS_SERIF, Font.PLAIN, FONT_STD_H );
    this.kbMatrix  = new int[ 8 ];
    this.curIdx    = 0;
    this.curX      = KEY_X1;
    this.curY      = KEY_Y1;

    this.capsShiftLinks.add( addKey( null, "TRUE\nVIDEO", null, 3, 0x04 ) );
    this.capsShiftLinks.add( addKey( null, "INV\nVIDEO",  null, 3, 0x08 ) );
    addKey( "1",  this.imgG1, "!",  "BLUE\nDEF FN",   3, 0x01 );
    addKey( "2",  this.imgG2, "@",  "RED\nFN",        3, 0x02 );
    addKey( "3",  this.imgG3, "#",  "MGNTA\nLINE",    3, 0x04 );
    addKey( "4",  this.imgG4, "$",  "GREEN\nOPEN #",  3, 0x08 );
    addKey( "5",  this.imgG5, "%",  "CYAN\nCLOSE #",  3, 0x10 );
    addKey( "6",  this.imgG6, "&",  "YELLOW\nMOVE",   4, 0x10 );
    addKey( "7",  this.imgG7, "\'", "WHITE\nERASE",   4, 0x08 );
    addKey( "8",  this.imgG8, "(",  "\nPOINT",        4, 0x04 );
    addKey( "9",              ")",  "\nCAT",          4, 0x02 );
    addKey( "0",              "_",  "BLACK\nFORMAT",  4, 0x01 );
    this.capsShiftLinks.add(
		addKey( KEY_LARGE_W, null, "\nBREAK", null, 7, 0x01 ) );
    this.curX = KEY_X1;
    this.curY += KEY_H;
    this.capsShiftLinks.add(
		addKey( KEY_LARGE_W, null, "\nDELETE", null, 4, 0x01 ) );
    this.capsShiftLinks.add(
		addKey( null, "\nGRAPH",  null, 4, 0x02 ) );
    addKey( "Q", "PLOT\n<=", "SIN\nASIN",   2, 0x01 );
    addKey( "W", "DRAW\n<>", "COS\nACS",    2, 0x02 );
    addKey( "E", "REM\n>=",  "TAN\nATN",    2, 0x04 );
    addKey( "R", "RUN\n<",   "INT\nVERIFY",    2, 0x08 );
    addKey( "T", "RAND\n>",   "RND\nMERGE",   2, 0x10 );
    addKey( "Y", "RETURN\nAND", "STR$\n[", 5, 0x10 );
    addKey( "U", "IF\nOR",      "CHR$\n]",     5, 0x08 );
    addKey( "I", "INPUT\nAT",  "CODE\nIN",  5, 0x04 );
    addKey( "O", "POKE",  "PEEK\nOUT", 5, 0x02 );
    addKey( "P", "PRINT", "TAB\n(c)", 5, 0x01 );
    this.curX = KEY_X1;
    this.curY += KEY_H;
    KeyData extendedModeKey = addKey(
				KEY_LARGE_W,
				null,
				"EXTENDED\nMODE",
				null,
				0,
				0x00 );
    this.capsShiftLinks.add( extendedModeKey );
    this.symShiftLinks.add( extendedModeKey );
    this.capsShiftLinks.add(
		addKey( KEY_EDIT_W, null, "\nEDIT", null, 3, 0x01 ) );
    addKey( "A", "NEW\nSTOP", "READ\n~",   1, 0x01 );
    addKey( "S", "SAVE\nNOT",  "RESTR\n|",  1, 0x02 );
    addKey( "D", "DIM\nSTEP", "DATA\n\\",   1, 0x04 );
    addKey( "F", "FOR\nTO",   "SGN\n{",   1, 0x08 );
    addKey( "G", "GOTO\nTHEN", "ABS\n}",  1, 0x10 );
    addKey( "H", "GOSUB\n\u2191",    "SQR\nCIRCLE", 6, 0x10 );
    addKey( "J", "LOAD\n-",    "VAL\nVAL$",  6, 0x08 );
    addKey( "K", "LIST\n+",    "LEN\nSCRN$",  6, 0x04 );
    addKey( "L", "LET\n=",    "USR\nATTR",   6, 0x02 );
    this.keyEnter = new KeyData(
				this.curX,
				this.curY - KEY_H,
				KEY_ENTER_W,
				KEY_ENTER_H,
				null,
				"ENTER",
				null,
				null,
				null,
				6,
				0x01,
				false,
				null );
    this.keys[ this.curIdx++ ] = this.keyEnter;
    this.curX = KEY_X1;
    this.curY += KEY_H;
    this.keyCapsShift1 = new KeyData(
				this.curX,
				this.curY,
				KEY_CAPS_SHIFT_W,
				KEY_H,
				null,
				KEY_TEXT_CAPS_SHIFT,
				null,
				null,
				null,
				CAPS_SHIFT_COL,
				CAPS_SHIFT_VALUE,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.keyCapsShift1;
    this.curX += KEY_CAPS_SHIFT_W;
    this.capsShiftLinks.add(
		addKey( null, "CAPS\nLOCK",  null, 3, 0x02 ) );
    addKey( "Z", "COPY\n:",      "LN\nBEEP",   0, 0x02 );
    addKey( "X", "CLEAR\n\u00A3", "EXP\nINK",  0, 0x04 );
    addKey( "C", "CONT\n?",      "LPRINT\nPAPER",   0, 0x08 );
    addKey( "V", "CLS\n/",      "LLIST\nFLASH",    0, 0x10 );
    addKey( "B", "BORDER\n*",      "BIN\nBRIGHT", 7, 0x10 );
    addKey( "N", "NEXT",      "INKEY$\nOVER",   7, 0x08 );
    addKey( "M", "PAUSE",      "PI\nINVERS",  7, 0x04 );
    this.symShiftLinks.add( addKey( ".", null, null, 7, 0x04 ) );
    this.keyCapsShift2 = new KeyData(
				this.curX,
				this.curY,
				KEY_CAPS_SHIFT_W,
				KEY_H,
				null,
				KEY_TEXT_CAPS_SHIFT,
				null,
				null,
				null,
				CAPS_SHIFT_COL,
				CAPS_SHIFT_VALUE,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.keyCapsShift2;
    this.curX = KEY_X1;
    this.curY += KEY_H;
    this.keySymShift1 = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				null,
				KEY_TEXT_SYMBOL_SHIFT,
				null,
				null,
				null,
				SYM_SHIFT_COL,
				SYM_SHIFT_VALUE,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.keySymShift1;
    this.curX += KEY_W;
    this.symShiftLinks.add( addKey( ";", null, null, 5, 0x02 ) );
    this.symShiftLinks.add( addKey( "\"", null, null, 5, 0x01 ) );
    this.capsShiftLinks.add(
		addKey( null,  this.imgLeft, null,  null, 3, 0x10 ) );
    this.capsShiftLinks.add(
		addKey( null,  this.imgRight, null, null, 4, 0x04 ) );
    addKey( KEY_SPACE_W, null, null, null,  7, 0x01 );
    this.capsShiftLinks.add(
		addKey( null, this.imgUp, null, null, 4, 0x08 ) );
    this.capsShiftLinks.add(
		addKey( null, this.imgDown, null, null, 4, 0x10 ) );
    this.symShiftLinks.add( addKey( ",", null, null, 7, 0x08 ) );
    this.keySymShift2 = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				null,
				KEY_TEXT_SYMBOL_SHIFT,
				null,
				null,
				null,
				SYM_SHIFT_COL,
				SYM_SHIFT_VALUE,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.keySymShift2;
    this.polygonEnter = new Polygon(
			new int[] {
				this.keyEnter.x + this.keyEnter.w - KEY_W,
				this.keyEnter.x + this.keyEnter.w - 1,
				this.keyEnter.x + this.keyEnter.w - 1,
				this.keyEnter.x,
				this.keyEnter.x,
				this.keyEnter.x + this.keyEnter.w - KEY_W,
				this.keyEnter.x + this.keyEnter.w - KEY_W },
			new int[] {
				this.keyEnter.y,
				this.keyEnter.y,
				this.keyEnter.y + this.keyEnter.h - 1,
				this.keyEnter.y + this.keyEnter.h - 1,
				this.keyEnter.y + KEY_H,
				this.keyEnter.y + KEY_H,
				this.keyEnter.y },
			7 );

    int rMargin = Math.max( KEY_X1, this.imgLogo128k.getWidth( this ) + 20 );
    setPreferredSize(
		new Dimension(
			this.keySymShift2.x
					+ this.keySymShift2.w
					+ rMargin,
			this.keySymShift2.y
					+ this.keySymShift2.h
					+ (KEY_Y1 / 2) ) );
    setShiftKeys(
		this.keyCapsShift1,
		this.keyCapsShift2,
		this.keySymShift1,
		this.keySymShift2 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof ZXSpectrum ?
				((ZXSpectrum) emuSys).isMode128K()
				: false;
  }


  @Override
  public String getKeyboardName()
  {
    return "ZX Spectrum+ 128K Tastatur";
  }


  @Override
  protected void keySelectionChanged()
  {
    Arrays.fill( this.kbMatrix, 0 );
    synchronized( this.selectedKeys ) {
      for( KeyData key : this.selectedKeys ) {
	if( this.capsShiftLinks.contains( key ) ) {
	  this.kbMatrix[ CAPS_SHIFT_COL ] |= CAPS_SHIFT_VALUE;
	}
	if( this.symShiftLinks.contains( key ) ) {
	  this.kbMatrix[ SYM_SHIFT_COL ] |= SYM_SHIFT_VALUE;
	}
	if( (key.col >= 0) && (key.col < this.kbMatrix.length) ) {
	  this.kbMatrix[ key.col ] |= key.value;
	}
      }
    }
    this.emuSys.updKeyboardMatrix( this.kbMatrix );
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this ) {
      super.mousePressed( e );
      synchronized( this.selectedKeys ) {
	if( hits( this.keyCapsShift1, e )
	    || hits( this.keyCapsShift2, e )
	    || hits( this.keySymShift1, e )
	    || hits( this.keySymShift2, e ) )
	{
	  if( ((isKeySelected( this.keyCapsShift1 )
				&& !this.keyCapsShift1.locked)
			||(isKeySelected( this.keyCapsShift2 )
				&& !this.keyCapsShift2.locked))
	      && ((isKeySelected( this.keySymShift1 )
				&& !this.keySymShift1.locked )
			||(isKeySelected( this.keySymShift2 )
				&& !this.keySymShift2.locked)) )
	  {
	    this.selectedKeys.remove( this.keyCapsShift1 );
	    this.selectedKeys.remove( this.keyCapsShift2 );
	    this.selectedKeys.remove( this.keySymShift1 );
	    this.selectedKeys.remove( this.keySymShift2 );
	  }
	}
      }
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    // Hintergrund
    int w = getWidth();
    g.setColor( this.colorBg );
    g.fillRect( 0, 0, w, getHeight() );
    g.setColor( this.colorLogo );
    setFont( new Font( Font.SANS_SERIF, Font.PLAIN, 16 ) );
    g.drawString( "ZX Spectrum+", KEY_X1 + 1, 20 );

    // Linien links
    int y = KEY_Y1;
    for( int i = 0; i < 5; i++ ) {
      g.setColor( this.colorShadow );
      g.drawLine( 0, y, KEY_X1, y );
      g.setColor( this.colorLight );
      g.drawLine( 0, y + 1, KEY_X1, y + 1 );
      y += KEY_H;
    }

    // Tasten
    for( int i = 0; i < this.keys.length; i++ ) {
      KeyData key      = this.keys[ i ];
      Image   img      = null;
      boolean selected = isKeySelected( key );
      if( key == this.keyEnter ) {
	if( selected ) {
	  g.setColor( this.colorSelected );
	  g.fillPolygon( this.polygonEnter );
	} else {
	  g.setColor( Color.BLACK );
	  g.drawPolygon( this.polygonEnter );
	}
	if( this.imgKeyEnter != null ) {
	  g.drawImage( this.imgKeyEnter, key.x, key.y, this );
	}
	g.setColor( Color.WHITE );
	g.setFont( this.fontStd );
	drawStringCenter(
			g,
			key.text2,
			key.x,
			key.y + (key.h / 4 * 3) + FONT_STD_H - 4,
			key.w );
      } else {
	if( selected ) {
	  g.setColor( this.colorSelected );
	  g.fillRect( key.x, key.y, key.w - 1, key.h - 1 );
	} else {
	  g.setColor( Color.BLACK );
	  g.drawRect( key.x, key.y, key.w - 1, key.h - 1 );
	}
	switch( key.w ) {
	  case KEY_W:
	    img = this.imgKeyStd;
	    break;
	  case KEY_EDIT_W:
	    img = this.imgKeyEdit;
	    break;
	  case KEY_LARGE_W:
	    img = this.imgKeyLarge;
	    break;
	  case KEY_CAPS_SHIFT_W:
	    img = this.imgKeyCapsShift;
	    break;
	  case KEY_SPACE_W:
	    img = this.imgKeySpace;
	    break;
	}
	if( img != null ) {
	  g.drawImage( img, key.x, key.y, this );
	}
	if( key.image != null ) {
	  g.drawImage( key.image, key.x, key.y, this );
	}
	g.setColor( Color.WHITE );
	if( key.text1 != null ) {
	  g.setFont( this.fontHuge );
	  if( (key.text2 == null) && (key.text3 == null) ) {
	    drawStringCenter(
			g,
			key.text1,
			key.x,
			key.y + (key.h / 2) + FONT_HUGE_H - 8,
			key.w );
	  } else {
	    drawStringCenter(
			g,
			key.text1,
			key.x,
			key.y + key.h - 7,
			key.w );
	  }
	}
	if( key.text2 != null ) {
	  int yText = key.y + (KEY_H / 2) - 2;
	  if( key.text2.length() == 1 ) {
	    g.setFont( this.fontLarge );
	    g.drawString(
			key.text2,
			key.x + (KEY_W / 2) + 5,
			yText + 5 );
	  } else {
	    String line1 = null;
	    String line2 = null;
	    int    pos   = key.text2.indexOf( '\n' );
	    if( pos >= 0 ) {
	      line1 = key.text2.substring( 0, pos );
	      line2 = key.text2.substring( pos + 1 );
	    } else {
	      line1 = key.text2;
	    }
	    g.setFont( this.fontStd );
	    if( line1 != null ) {
	      drawStringCenter( g, line1, key.x, yText, key.w );
	    }
	    if( line2 != null ) {
	      if( !line2.isEmpty() ) {
		g.setFont( this.fontStd );
		yText += (FONT_STD_H - 2);
		char ch = line2.charAt( 0 );
		if( (ch == '~') || Character.isLetter( ch ) ) {
		  drawStringCenter( g, line2, key.x, yText, key.w );
		} else {
		  int xText = key.x + key.w - 22;
		  if( (ch == '|') || (ch == '\\')
		      || (ch == '[') || (ch == ']')
		      || (ch == '{') || (ch == '}') )
		  {
		    xText = key.x + 15;
		  }
		  g.drawString( line2, xText, yText );
		}
	      }
	    }
	  }
	}
	if( key.text3 != null ) {
	  g.setFont( this.fontStd );
	  drawMultiLineString(
			g,
			key.x,
			key.y + FONT_STD_H - 1,
			key.w,
			key.text3,
			FONT_STD_H - 2 );
	}
      }
    }

    // rechte Seite
    int x = this.keySymShift2.x + this.keySymShift2.w;
    y     = KEY_Y1;
    for( int i = 0; i < 5; i++ ) {
      g.setColor( this.colorShadow );
      g.drawLine( x, y, w, y );
      g.setColor( this.colorLight );
      g.drawLine( x, y + 1, w, y + 1 );
      y += KEY_H;
    }
    g.setColor( this.colorShadow );
    g.drawLine( 0, y, w, y );
    g.setColor( this.colorLight );
    g.drawLine( 0, y + 1, w, y + 1 );
    if( this.imgLogo128k != null ) {
      g.drawImage(
		this.imgLogo128k,
		x + 10,
		y - 5 - this.imgLogo128k.getHeight( this ),
		this );
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof ZXSpectrum ) {
      this.emuSys = (ZXSpectrum) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != ZXSpectrum" );
    }
  }


  @Override
  public void updKeySelection( int[] kbMatrix )
  {
    boolean dirty = false;
    synchronized( this.selectedKeys ) {
      dirty = !this.selectedKeys.isEmpty();
      this.selectedKeys.clear();
      if( kbMatrix != null ) {
	boolean done           = false;
	boolean capsShiftMode  = false;
	boolean symShiftMode   = false;
	boolean pressCapsShift = false;
	boolean pressSymShift  = false;
	if( CAPS_SHIFT_COL < kbMatrix.length ) {
	  if( (kbMatrix[ CAPS_SHIFT_COL ] & CAPS_SHIFT_VALUE) != 0 ) {
	    capsShiftMode = true;
	  }
	}
	if( SYM_SHIFT_COL < kbMatrix.length ) {
	  if( (kbMatrix[ SYM_SHIFT_COL ] & SYM_SHIFT_VALUE) != 0 ) {
	    symShiftMode = true;
	  }
	}

	// zuerst die Tasten suchen, die exakt passen
	for( int col = 0; col < kbMatrix.length; col++ ) {
	  if( kbMatrix[ col ] != 0 ) {
	    for( KeyData key : this.keys ) {
	      if( (key.col == col)
		  && ((key.value & kbMatrix[ col ]) != 0)
		  && (key != this.keySymShift1)
		  && (key != this.keySymShift2)
		  && (key != this.keyCapsShift1)
		  && (key != this.keyCapsShift2)
		  && (capsShiftMode == this.capsShiftLinks.contains( key ))
		  && (symShiftMode  == this.symShiftLinks.contains( key )) )
	      {
		done       = true;
		dirty      = true;
		key.locked = false;
		this.selectedKeys.add( key );
	      }
	    }
	  }
	}
	if( !done ) {

	  // im zweiten Durchlauf Tastenkombinationen suchen
	  for( int col = 0; col < kbMatrix.length; col++ ) {
	    if( kbMatrix[ col ] != 0 ) {
	      for( KeyData key : this.keys ) {
		if( (key.col == col)
		    && ((key.value & kbMatrix[ col ]) != 0)
		    && (key != this.keySymShift1)
		    && (key != this.keySymShift2)
		    && (key != this.keyCapsShift1)
		    && (key != this.keyCapsShift2)
		    && !this.capsShiftLinks.contains( key )
		    && !this.symShiftLinks.contains( key ) )
		{
		  if( capsShiftMode ) {
		    pressCapsShift = true;
		  }
		  if( symShiftMode ) {
		    pressSymShift = true;
		  }
		  dirty      = true;
		  key.locked = false;
		  this.selectedKeys.add( key );
		}
	      }
	    }
	  }
	}
	if( pressCapsShift ) {
	  this.keyCapsShift1.locked = false;
	  this.keyCapsShift2.locked = false;
	  this.selectedKeys.add( this.keyCapsShift1 );
	  this.selectedKeys.add( this.keyCapsShift2 );
	  dirty = true;
	}
	if( pressSymShift ) {
	  this.keySymShift1.locked = false;
	  this.keySymShift2.locked = false;
	  this.selectedKeys.add( this.keySymShift1 );
	  this.selectedKeys.add( this.keySymShift2 );
	  dirty = true;
	}
      }
    }
    if( dirty ) {
      repaint();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected Image getImage( String filename )
  {
    return super.getImage( "/images/keyboard/zxspectrum/128k/" + filename );
  }


	/* --- private Methoden --- */

  private KeyData addKey(
			String  text1,
			String  text2,
			String  text3,
			int     col,
			int     value )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				text1,
				text2,
				text3,
				null,
				null,
				col,
				value,
				false,
				null );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_W;
    return keyData;
  }


  private KeyData addKey(
			int     w,
			String  text1,
			String  text2,
			String  text3,
			int     col,
			int     value )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				w,
				KEY_H,
				text1,
				text2,
				text3,
				null,
				null,
				col,
				value,
				false,
				null );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += w;
    return keyData;
  }


  private KeyData addKey(
			String  text1,
			Image   image,
			String  text2,
			String  text3,
			int     col,
			int     value )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				text1,
				text2,
				text3,
				null,
				image,
				col,
				value,
				false,
				null );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_W;
    return keyData;
  }


  private void drawMultiLineString(
				Graphics g,
				int      x,
				int      y,
				int      w,
				String   text,
				int      hRow )
  {
    if( text != null ) {
      int len = text.length();
      int pos = 0;
      while( pos < len ) {
	int eol = text.indexOf( '\n', pos );
	if( eol < pos ) {
	  drawStringCenter( g, text.substring( pos ), x, y, w );
	  break;
	}
	drawStringCenter( g, text.substring( pos, eol ), x, y, w );
	pos = eol + 1;
	y += hRow;
      }
    }
  }
}
