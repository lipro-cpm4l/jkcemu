/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer ein Bildschirmgeraet
 */

package jkcemu.base;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.*;
import java.util.Properties;


public abstract class AbstractScreenDevice
{
  protected static final int BLACK = 0;
  protected static final int WHITE = 1;

  protected Color colorWhite;
  protected Color colorRedLight;
  protected Color colorRedDark;
  protected Color colorGreenLight;
  protected Color colorGreenDark;


  // Basiskoordinaten eines horizontalen Segments einer 7-Segment-Anzeige
  private static final int[] base7SegHXPoints = { 0, 3, 31, 34, 31, 3, 0 };
  private static final int[] base7SegHYPoints = { 3, 0, 0, 3, 6, 6, 3 };

  // Basiskoordinaten eines vertikalen Segments einer 7-Segment-Anzeige
  private static final int[] base7SegVXPoints = { 3, 0, 4, 7, 10, 6, 3 };
  private static final int[] base7SegVYPoints = { 5, 2, -27, -30, -27, 2, 5 };

  private static int[] tmp7SegXPoints = new int[ base7SegHXPoints.length ];
  private static int[] tmp7SegYPoints = new int[ base7SegHYPoints.length ];


  private volatile AbstractScreenFrm screenFrm;


  protected AbstractScreenDevice( Properties props )
  {
    createColors( props );
  }


  public void applySettings( Properties props )
  {
    createColors( props );
  }


  public void cancelPastingText()
  {
    // leer
  }


  public boolean canExtractScreenText()
  {
    return false;
  }


  public int getBorderColorIndex()
  {
    return BLACK;
  }


  public int getBorderColorIndexByLine( int line )
  {
    return getBorderColorIndex();
  }


  /*
   * Die Helligkeit wird logarithmisch gewertet,
   * damit man auch im unteren und mittleren Einstellbereich
   * noch etwas sieht.
   */
  protected static float getBrightness( Properties props )
  {
    int value = EmuUtil.getIntProperty(
				props,
				ScreenFld.PROP_BRIGHTNESS,
				ScreenFld.DEFAULT_BRIGHTNESS );
    float rv = 1F;
    if( (value > 0) && (value < 100) ) {
      rv = 1F - (float) Math.abs(
			  Math.log10( (double) (value + 10) / 110.0 ) );
    } else {
      rv = (float) value / 100F;
    }
    if( rv < 0F ) {
      rv = 0F;
    } else if( rv > 1F ) {
      rv = 1F;
    }
    return rv;
  }


  public Color getColor( int colorIdx )
  {
    return colorIdx == WHITE ? this.colorWhite : Color.black;
  }


  public int getColorCount()
  {
    return 2;		// schwarz/weiss
  }


  public int getColorIndex( int x, int y )
  {
    return BLACK;
  }


  public CharRaster getCurScreenCharRaster()
  {
    return null;
  }


  public abstract EmuThread getEmuThread();


  /*
   * Die Methode liefert entsprechend der eingestellten Helligkeit
   * den max. Wert fuer die jeweiligen Primaerfarben.
   */
  public static int getMaxRGBValue( Properties props )
  {
    int   value      = 255 * ScreenFld.DEFAULT_BRIGHTNESS / 100;
    float brightness = getBrightness( props );
    if( (brightness >= 0F) && (brightness <= 1F) ) {
      value = Math.round( 255 * brightness );
    }
    return value;
  }


  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    return -1;
  }


  public AbstractScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public abstract int getScreenHeight();
  public abstract int getScreenWidth();


  public String getScreenText()
  {
    CharRaster chRaster = getCurScreenCharRaster();
    return chRaster != null ?
		getScreenText(
			chRaster,
			0,
			0,
			chRaster.getColCount() - 1,
			chRaster.getRowCount() - 1 )
		: null;
  }


  public String getScreenText(
			CharRaster chRaster,
			int        chX1,
			int        chY1,
			int        chX2,
			int        chY2 )
  {
    String rv = null;
    if( (chX1 >= 0) && (chY1 >= 0) ) {
      int nCols = chRaster.getColCount();
      int nRows = chRaster.getRowCount();
      if( (nCols > 0) && (nRows > 0) ) {
	if( chY2 >= nRows ) {
	  chY2 = nRows - 1;
	}
	StringBuilder buf     = new StringBuilder( nRows * (nCols + 1) );
	int           nSpaces = 0;
	while( (chY1 < chY2)
	       || ((chY1 == chY2) && (chX1 <= chX2)) )
	{
	  int b = getScreenChar( chRaster, chX1, chY1 );
	  if( (b == 0) || b == 0x20 ) {
	    if( chY1 < chY2 ) {
	      nSpaces++;
	    } else {
	      buf.append( (char) '\u0020' );
	    }
	  } else {
	    while( nSpaces > 0 ) {
	      buf.append( (char) '\u0020' );
	      --nSpaces;
	    }
	    buf.append( (char) (b > 0 ? b : '_') );
	  }
	  chX1++;
	  if( chX1 >= nCols ) {
	    buf.append( (char) '\n' );
	    nSpaces = 0;
	    chX1    = 0;
	    chY1++;
	  }
	}
	if( buf.length() > 0 ) {
	 rv = buf.toString();
	}
      }
    }
    return rv;
  }


  public abstract String getTitle();


  /*
   * Malen einer Stelle einer 7-Segment-Anzeige
   * in der Basisgroesse 50x85 (BxH)
   *
   * Kodierung der Segmente
   *   A: Bit0
   *   B: Bit1
   *   C: Bit2
   *   D: Bit3
   *   E: Bit4
   *   F: Bit6
   *   G: Bit7
   *   P: Bit8
   */
  protected static void paint7SegDigit(
				Graphics g,
				int      x,
				int      y,
				int      v,
				Color    d,
				Color    l,
				int      f )
  {
    paint7SegH( g, x + (14 * f), y + (0 * f), f, (v & 0x01) != 0 ? l : d );
    paint7SegV( g, x + (44 * f), y + (35 * f), f, (v & 0x02) != 0 ? l : d );
    paint7SegV( g, x + (40 * f), y + (75 * f), f, (v & 0x04) != 0 ? l : d );
    paint7SegH( g, x + (6 * f), y + (80 * f), f, (v & 0x08) != 0 ? l : d );
    paint7SegV( g, x + (0 * f), y + (75 * f), f, (v & 0x10) != 0 ? l : d );
    paint7SegV( g, x + (4 * f), y + (35 * f), f, (v & 0x20) != 0 ? l : d );
    paint7SegH( g, x + (10 * f), y + (40 * f), f, (v & 0x40) != 0 ? l : d );
    g.setColor( (v & 0x80) != 0 ? l : d );
    g.fillArc(
	x + (47 * f),
	y + (80 * f),
	5 * f,
	5 * f,
	0,
	360 );
  }


  /*
   * Durch Ueberschreiben dieser Methode hat das emulierte System
   * die Moeglichkeit,
   * selbst die Bildschirmausgabe grafisch darzustellen.
   * Wenn nicht (Rueckgabewert false) werden die Methoden getColorCount()
   * und getColorIndex( x, y ) aufgerufen.
   */
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    return false;
  }


  public void setScreenDirty( boolean state )
  {
    AbstractScreenFrm screenFrm = this.screenFrm;
    if( screenFrm != null ) {
      screenFrm.setScreenDirty( state );
    }
  }


  public void setScreenFrm( AbstractScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
  }


  /*
   * Diese Methode besagt, ob die mit getScreenChar() und getScreenText()
   * gelieferten Zeichen moeglicherweise noch konvertiert werden muessen.
   * Das ist dann der Fall, wenn die Zeichensatzdatei
   * bzw. bei einem Vollgrafiksystem das Betriebssystem
   * von extern geladen wird und der Zeichensatz somit nicht bekannt ist.
   */
  public boolean shouldAskConvertScreenChar()
  {
    return false;
  }


  public void startPastingText( String text )
  {
    // leer
  }


  public boolean supportsBorderColorByLine()
  {
    return false;
  }


  public boolean supportsCopyToClipboard()
  {
    return false;
  }


  public boolean supportsPasteFromClipboard()
  {
    return false;
  }


	/* --- private Methoden --- */

  private void createColors( Properties props )
  {
    int value            = getMaxRGBValue( props );
    this.colorWhite      = new Color( value, value, value );
    this.colorRedLight   = new Color( value, 0, 0 );
    this.colorRedDark    = new Color( value / 5, 0, 0 );
    this.colorGreenLight = new Color( 0, value, 0 );
    this.colorGreenDark  = new Color( 0, value / 8, 0 );
  }


  /*
   * Zeichnen eines horizontalen Segments einer 7-Segment-Anzeige
   * Laenge: 35, Hoehe: 7
   *
   * Parameter:
   *   x, y: linke Spitze
   */
  private static void paint7SegH(
			Graphics g,
			int      x,
			int      y,
			int      f,
			Color    color )
  {
    paint7Seg( g, x, y, f, base7SegHXPoints, base7SegHYPoints, color );
  }


  /*
   * Zeichnen eines vertikalen Segments einer 7-Segment-Anzeige
   * Hoehe:35 , Breite: 7 + 4 durch Neigung
   *
   * Parameter:
   *   x, y: untere Spitze
   */
  private static void paint7SegV(
			Graphics g,
			int      x,
			int      y,
			int      f,
			Color    color )
  {
    paint7Seg( g, x, y, f, base7SegVXPoints, base7SegVYPoints, color );
  }


  private static void paint7Seg(
			Graphics g,
			int      x,
			int      y,
			int      f,
			int[]    baseXPoints,
			int[]    baseYPoints,
			Color    color )
  {
    if( color != null ) {
      for( int i = 0; i < tmp7SegXPoints.length; i++ ) {
	tmp7SegXPoints[ i ] = x + (baseXPoints[ i ] * f);
	tmp7SegYPoints[ i ] = y + (baseYPoints[ i ] * f);
      }
      g.setColor( color );
      g.fillPolygon( tmp7SegXPoints, tmp7SegYPoints, tmp7SegXPoints.length );
    }
  }
}
