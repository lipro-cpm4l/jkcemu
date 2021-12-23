/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente zur Anzeige einer Unicode-Seite
 */

package jkcemu.base;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;


public class CharPageFld extends JComponent implements MouseListener
{
  public static final int COLS   = 32;
  public static final int ROWS   = 0x100 / COLS;
  public static final int MARGIN = 5;


  public interface Callback
  {
    public void charSelected( int ch );
  };


  public static class BodyFld
			extends JComponent
			implements MouseMotionListener, Scrollable
  {

    private CharPageFld charPageFld;
    private int         selectedChar;

    public BodyFld( CharPageFld charPageFld )
    {
      this.charPageFld  = charPageFld;
      this.selectedChar = -1;
      setBackground( SystemColor.text );
      setForeground( SystemColor.textText );
    }

    public int getCharAt( int x, int y )
    {
      int rv    = -1;
      int wChar = this.charPageFld.getCharWidth();
      int hChar = this.charPageFld.getCharHeight();
      if( (x >= 0) && (y >= 0) && (wChar > 0) && (hChar > 0) ) {
	int row = (y - MARGIN) / hChar;
	int col = (x - MARGIN) / wChar;
	int c   = col % 3;
	if( ((c == 0) || (c == 1)) && ((row % 2) == 0) ) {
	  col /= 3;
	  row /= 2;
	  if( (col >= 0) && (col < COLS)
	      && (row >= 0) && (row < ROWS) )
	  {
	    rv = this.charPageFld.getCodeBase() + (row * COLS) + col;
	  }
	}
      }
      return rv;
    }

    public int getSelectedChar()
    {
      return this.selectedChar;
    }

    public void setSelectedChar( int ch )
    {
      if( ch != this.selectedChar ) {
	this.selectedChar = ch;
	repaint();
      }
    }


	/* --- MouseMotionListsner --- */

    @Override
    public void mouseDragged( MouseEvent e )
    {
      setSelectedChar( -1 );
    }

    @Override
    public void mouseMoved( MouseEvent e )
    {
      setSelectedChar( getCharAt( e.getX(), e.getY() ) );
    }


	/* --- Scrollable --- */

    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
      return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(
				Rectangle visibleRect,
				int       orientation,
				int       direction )
    {
      int rv = 0;
      if( direction < 0 ) {
	rv = this.charPageFld.getCharHeight() * 2;
      } else if( direction > 0 ) {
	rv = this.charPageFld.getCharWidth() * 3;
      }
      return rv;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
      boolean   rv = false;
      Component p  = getParent();
      if( p != null ) {
	if( (p instanceof JViewport)
	    && (p.getHeight() > getPreferredSize().height) )
	{
	  // Viewport voll ausfuellen
	  rv = true;
	}
      }
      return rv;
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
      boolean   rv = false;
      Component p  = getParent();
      if( p != null ) {
	if( (p instanceof JViewport)
	    && (p.getWidth() > getPreferredSize().width) )
	{
	  // Viewport voll ausfuellen
	  rv = true;
	}
      }
      return rv;
    }

    @Override
    public int getScrollableUnitIncrement(
				Rectangle visibleRect,
				int       orientation,
				int       direction )
    {
      int rv = 0;
      if( direction < 0 ) {
	rv = this.charPageFld.getCharHeight();
      } else if( direction > 0 ) {
	rv = this.charPageFld.getCharWidth();
      }
      return rv;
    }


	/* --- ueberschriebene Methoden --- */

    @Override
    public Dimension getPreferredSize()
    {
      Dimension rv = null;
      if( isPreferredSizeSet() ) {
	rv = super.getPreferredSize();
      } else {
	rv = new Dimension(
			this.charPageFld.getPrefBodyWidth(),
			this.charPageFld.getPrefBodyHeight() );
      }
      return rv;
    }

    @Override
    public void paintComponent( Graphics g )
    {
      int w = getWidth();
      int h = getHeight();
      if( (w > 0) && (h > 0) ) {

	// Hintergrund loeschen
	g.setColor( getBackground() );
	g.setPaintMode();
	g.fillRect( 0, 0, w, h );

	// Inhalt erzeugen
	Font font  = getFont();
	int  wChar = this.charPageFld.getCharWidth();
	int  hChar = this.charPageFld.getCharHeight();
	if( (wChar > 0) && (hChar > 0) && (font != null) ) {
	  g.setFont( font );
	  for( int row = 0; row < ROWS; row++ ) {
	    for( int col = 0; col < COLS; col++ ) {
	      int x  = MARGIN + (col * wChar * 3);
	      int y  = MARGIN + hChar + (row * hChar * 2);
	      int ch = this.charPageFld.getCodeBase() + (row * COLS) + col;
	      if( Character.isDefined( ch ) && font.canDisplay( ch ) ) {
		if( ch == getSelectedChar() ) {
		  g.setColor( SystemColor.textHighlight );
		  g.fillRect( x, y - hChar + 3, 2 * wChar, hChar );
		  g.setColor( SystemColor.textHighlightText );
		} else {
		  g.setColor( SystemColor.textText );
		}
		g.drawString( Character.toString( (char) ch ), x, y );
	      }
	    }
	  }
	}
      }
    }
  };


  public static class ColHeaderFld extends JComponent
  {
    private CharPageFld charPageFld;

    public ColHeaderFld( CharPageFld charPageFld )
    {
      this.charPageFld = charPageFld;
      setBackground( SystemColor.scrollbar );
      setForeground( SystemColor.textText );
    }

	/* --- ueberschriebene Methoden --- */

    @Override
    public Dimension getPreferredSize()
    {
      Dimension rv = null;
      if( isPreferredSizeSet() ) {
	rv = super.getPreferredSize();
      } else {
	rv = new Dimension(
			this.charPageFld.getPrefBodyWidth(),
			(2 * MARGIN) + this.charPageFld.getCharHeight() );
      }
      return rv;
    }

    @Override
    public void paintComponent( Graphics g )
    {
      int w = getWidth();
      int h = getHeight();
      if( (w > 0) && (h > 0) ) {
	
	// Hintergrund loeschen
	g.setColor( getBackground() );
	g.setPaintMode();
	g.fillRect( 0, 0, w, h );

	// Inhalt erzeugen
	Font font  = getFont();
	int  wChar = this.charPageFld.getCharWidth();
	int  hChar = this.charPageFld.getCharHeight();
	if( (wChar > 0) && (hChar > 0) && (font != null) ) {
	  g.setFont( font );
	  g.setColor( getForeground() );
	  int y = MARGIN + hChar;
	  for( int i = 0; i < COLS; i++ ) {
	    g.drawString(
			String.format( "%02X", i ),
			MARGIN + (i * 3 * wChar),
			y );
	  }
	}
      }
    }
  };


  public static class RowHeaderFld extends JComponent
  {
    private CharPageFld charPageFld;

    public RowHeaderFld( CharPageFld charPageFld )
    {
      this.charPageFld = charPageFld;
      setBackground( SystemColor.scrollbar );
      setForeground( SystemColor.textText );
    }

	/* --- ueberschriebene Methoden --- */

    @Override
    public Dimension getPreferredSize()
    {
      Dimension rv = null;
      if( isPreferredSizeSet() ) {
	rv = super.getPreferredSize();
      } else {
	int  w    = 2 * MARGIN;
	Font font = getFont();
	if( font != null ) {
	  FontMetrics fm = getFontMetrics( font );
	  if( fm != null ) {
	    w += fm.stringWidth( "0000" );
	  }
	}
	rv = new Dimension( w, this.charPageFld.getPrefBodyHeight() );
      }
      return rv;
    }

    @Override
    public void paintComponent( Graphics g )
    {
      int w = getWidth();
      int h = getHeight();
      if( (w > 0) && (h > 0) ) {
	
	// Hintergrund loeschen
	g.setColor( getBackground() );
	g.setPaintMode();
	g.fillRect( 0, 0, w, h );

	// Inhalt erzeugen
	Font font  = getFont();
	int  hChar = this.charPageFld.getCharHeight();
	if( (hChar > 0) && (font != null) ) {
	  g.setFont( font );
	  g.setColor( getForeground() );
	  int a = this.charPageFld.getCodeBase();
	  int y = MARGIN + hChar;
	  for( int i = 0; i < ROWS; i++ ) {
	    g.drawString( String.format( "%04X", a ), MARGIN, y );
	    a += 0x0020;
	    y += (2 * hChar);
	  }
	}
      }
    }
  };


  private CharPageFld.Callback callback;
  private boolean              notified;
  private int                  codeBase;
  private int                  fontScale;
  private int                  wChar;
  private int                  hChar;
  private BodyFld              bodyFld;
  private ColHeaderFld         colHeaderFld;
  private RowHeaderFld         rowHeaderFld;
  private JScrollPane          scrollPane;


  public CharPageFld( CharPageFld.Callback callback )
  {
    this.callback     = callback;
    this.notified     = false;
    this.codeBase     = 0;
    this.fontScale    = 1;
    this.hChar        = 0;
    this.wChar        = 0;
    this.bodyFld      = new BodyFld( this );
    this.colHeaderFld = new ColHeaderFld( this );
    this.rowHeaderFld = new RowHeaderFld( this );
    setFont( FontMngr.getDefaultFont( FontMngr.FontUsage.CODE ) );

    setLayout( new BorderLayout() );
    this.scrollPane = GUIFactory.createScrollPane( this.bodyFld );
    this.scrollPane.setColumnHeaderView( this.colHeaderFld );
    this.scrollPane.setRowHeaderView( this.rowHeaderFld );
    add( this.scrollPane, BorderLayout.CENTER );

    setFocusable( true );
  }


  public int getCharHeight()
  {
    return this.hChar;
  }


  public int getCharWidth()
  {
    if( this.wChar == 0 ) {
      calcWChar();
    }
    return this.wChar;
  }


  public int getCodeBase()
  {
    return this.codeBase;
  }


  public int getPrefBodyHeight()
  {
    return (ROWS * getCharHeight())
		+ ((ROWS - 1) * getCharHeight())
		+ (2 * MARGIN);
  }


  public int getPrefBodyWidth()
  {
    return (COLS * 2 * getCharWidth())
		+ ((COLS - 1) * getCharWidth())
		+ (2 * MARGIN);
  }


  public int getSelectedChar()
  {
    return this.bodyFld.getSelectedChar();
  }


  public void setCodeBase( int codeBase )
  {
    this.codeBase = codeBase;
    this.wChar    = 0;		// Layout neu berechnen
    this.revalidate();
    this.repaint();
  }


  public void setFontScale( int scale )
  {
    if( scale < 1 ) {
      scale = 1;
    }
    if( scale != this.fontScale ) {
      this.fontScale = scale;
      setFontInternal( getFont() );
    }
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mouseExited( MouseEvent e )
  {
    if( e.getComponent() == this.bodyFld )
      this.bodyFld.setSelectedChar( -1 );
  }

  @Override
  public void mousePressed( MouseEvent e )
  {
    if( (e.getComponent() == this.bodyFld)
	&& (e.getButton() == MouseEvent.BUTTON1) )
    {
      requestFocus();
      this.callback.charSelected(
		this.bodyFld.getCharAt( e.getX(), e.getY() ) );
    }
  }

  @Override
  public void mouseReleased( MouseEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.bodyFld.addMouseListener( this );
      this.bodyFld.addMouseMotionListener( this.bodyFld );
    }
  }


  @Override
  public void setFont( Font font )
  {
    if( font != null ) {
      super.setFont( font );
      setFontInternal( font );
    }
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.bodyFld.removeMouseListener( this );
      this.bodyFld.removeMouseMotionListener( this.bodyFld );
    }
  }


	/* --- private Methoden --- */

  private void calcWChar()
  {
    int  wChar = 15;
    Font font  = this.bodyFld.getFont();
    if( font != null ) {
      FontMetrics fm = this.bodyFld.getFontMetrics( font );
      if( fm != null ) {
	wChar = fm.charWidth( '0' );
      }
    }
    this.wChar = wChar;
  }


  private void setFontInternal( Font font )
  {
    if( font != null ) {
      if( this.fontScale > 1 ) {
	font = font.deriveFont( (float) (font.getSize() * this.fontScale) );
      }
      this.bodyFld.setFont( font );
      this.colHeaderFld.setFont( font );
      this.rowHeaderFld.setFont( font );
      this.wChar = 0;		// Layout neu berechnen
      this.hChar = font.getSize();
      this.revalidate();
      this.repaint();
    }
  }
}
