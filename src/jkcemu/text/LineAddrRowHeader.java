/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * RowHeader-Komponente zur Anzeige von Adressen
 */

package jkcemu.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;
import jkcemu.base.RolloverCloseBtn;
import jkcemu.text.TextUtil;


public class LineAddrRowHeader extends JComponent
{
  private static final int DEFAULT_CLOSE_W = 16;
  private static final int DEFAULT_CLOSE_H = 16;
  private static final int MARGIN          = 5;

  private JTextArea            textArea;
  private Map<Integer,Integer> lineAddrMap;
  private boolean              mouseBtnDown;
  private boolean              overClose;
  private int                  closeX;
  private int                  closeY;
  private int                  closeW;
  private int                  closeH;


  public LineAddrRowHeader(
		JTextArea            textArea,
		Map<Integer,Integer> lineAddrMap,
		Dimension            closeBtnSize )
  {
    this.textArea     = textArea;
    this.lineAddrMap  = lineAddrMap;
    this.mouseBtnDown = false;
    this.overClose    = false;
    this.closeX       = -1;
    this.closeY       = -1;
    this.closeW       = DEFAULT_CLOSE_W;
    this.closeH       = DEFAULT_CLOSE_H;
    if( closeBtnSize != null ) {
      if( (closeBtnSize.width > 0) && (closeBtnSize.height > 0) ) {
	this.closeW = closeBtnSize.width;
	this.closeH = closeBtnSize.height;
      }
    }
    setToolTipText( "Adressen im Arbeitsspeicher" );

    addMouseListener(
		new MouseAdapter()
		{
		  @Override
		  public void mouseClicked( MouseEvent e )
		  {
		    mouseClickedInternal( e );
		  }

		  @Override
		  public void mousePressed( MouseEvent e )
		  {
		    updMouseBtnDown( e );
		  }

		  @Override
		  public void mouseReleased( MouseEvent e )
		  {
		    updMouseBtnDown( e );
		  }
		} );

    addMouseMotionListener(
		new MouseMotionAdapter()
		{
		  @Override
		  public void mouseDragged( MouseEvent e )
		  {
		    mouseMovedInternal( e );
		  }

		  @Override
		  public void mouseMoved( MouseEvent e )
		  {
		    mouseMovedInternal( e );
		  }
		} );
  }


  public int getAddrByLine( int lineNum )
  {
    int rv = -1;
    if( this.lineAddrMap != null ) {
      Integer addr = this.lineAddrMap.get( lineNum );
      if( addr != null ) {
	rv = addr.intValue();
      }
    }
    return rv;
  }


  public JTextArea getJTextArea()
  {
    return this.textArea;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Dimension getPreferredSize()
  {
    Dimension rv = null;
    if( isPreferredSizeSet() ) {
      rv = super.getPreferredSize();
    } else {
      int  h    = 0;
      int  w    = 0;
      Font font = this.textArea.getFont();
      if( (font != null) && (this.lineAddrMap != null) ) {
	FontMetrics fm = getFontMetrics( font );
	if( fm != null ) {
	  w = fm.stringWidth( "8888" );
	  if( w > 0 ) {
	    w += (2 * MARGIN);
	  }
	}
      }
      Dimension size = this.textArea.getPreferredSize();
      if( size != null ) {
	h = size.height;
      }
      rv = new Dimension(
			Math.max( w, (2 * MARGIN) + DEFAULT_CLOSE_W ),
			Math.max( h, (2 * MARGIN) + DEFAULT_CLOSE_H ) );
    }
    return rv;
  }


  @Override
  public String getToolTipText( MouseEvent e )
  {
    return isOverClose( e ) ?
		"Adressspalte ausblenden"
		: super.getToolTipText( e );
  }


  @Override
  public void paintComponent( Graphics g )
  {
    super.paintComponent( g );

    int w = getWidth();

    // sichtbarer Bereich ermitteln
    int yVisible = 0;
    int hVisible = getHeight();
    if( (w > 0) && (hVisible > 0) ) {

      // sichtbarer Bereich ermitteln
      Component parent = getParent();
      if( parent != null ) {
	if( parent instanceof JViewport ) {
	  Rectangle r = ((JViewport) parent).getViewRect();
	  if( r != null ) {
	    yVisible += r.y;
	    hVisible = r.height;
	  }
	}
      }

      // Hintergrund loeschen
      g.setColor( getBackground() );
      g.setPaintMode();
      g.fillRect( 0, yVisible, w, yVisible + hVisible );
      g.setColor( Color.GRAY );
      g.drawLine( w - 1, yVisible, w - 1, yVisible + hVisible );

      // Adressliste
      try {
	g.setColor( SystemColor.textText );
	Font font = this.textArea.getFont();
	if( (font != null) && (this.lineAddrMap != null) ) {
	  g.setFont( font );

	  // Bereich der sichtbaren Zeilen ermitteln
	  Insets insets  = this.textArea.getInsets();
	  int    x       = (insets != null ? insets.left : 0);
	  int    begLine = 0;
	  int    endLine = 0;

	  int pos = TextUtil.viewToModel(
				this.textArea,
				new Point( x, yVisible ) );
	  if( pos >= 0 ) {
	    begLine = this.textArea.getLineOfOffset( pos );
	  }
	  pos = TextUtil.viewToModel(
				this.textArea,
				new Point( x, yVisible + hVisible) );
	  if( pos >= 0 ) {
	    endLine = this.textArea.getLineOfOffset( pos ) + 1;
	  }

	  // sichtbare Zeilen zeichnen
	  int nLines = this.textArea.getLineCount();
	  if( endLine >= nLines ) {
	    endLine = nLines - 1;
	  }
	  for( int line = begLine; line < endLine; line++ ) {
	    Integer addr = this.lineAddrMap.get( line + 1 );
	    if( addr != null ) {
	      Rectangle2D r = TextUtil.modelToView(
				this.textArea,
				this.textArea.getLineStartOffset( line ) );
	      if( r != null ) {
		int y = (int) Math.round( r.getY() + r.getHeight() - 5.0 );
		if( (y >= yVisible)
		  && ((y - font.getSize()) < (yVisible + hVisible)) )
		{
		  g.drawString(
			String.format( "%04X", addr ), MARGIN, y );
		}
	      }
	    }
	  }
	}
      }
      catch( BadLocationException ex ) {}

      // Close-Button
      if( this.closeX < 0 ) {
	this.closeX = w - MARGIN - this.closeW;
      }
      this.closeY = yVisible + MARGIN;
      Color bg = getBackground();
      if( bg == null ) {
	bg = Color.WHITE;
      }
      g.setColor( new Color(
			bg.getRed(),
			bg.getGreen(),
			bg.getBlue(),
			bg.getAlpha() / 2 ) );
      g.fillRect(
		this.closeX - MARGIN,
		this.closeY - MARGIN,
		(2 * MARGIN) + this.closeW,
		(2 * MARGIN) + this.closeH );
      g.setColor( bg );
      g.fillRect(
		this.closeX - 1,
		this.closeY - 1,
		this.closeW + 2,
		this.closeH + 2 );
      if( this.overClose ) {
	RolloverCloseBtn.paintBorder(
				g,
				this.closeX,
				this.closeY,
				this.closeW,
				this.closeH );
      }
      RolloverCloseBtn.paintComponent(
				g,
				this.closeX,
				this.closeY,
				this.closeW,
				this.closeH,
				true,
				this.overClose && this.mouseBtnDown );
    }
  }


  @Override
  public void validate()
  {
    this.closeX = -1;
    super.validate();
  }


	/* --- private Methoden --- */

  private boolean isOverClose( MouseEvent e )
  {
    boolean rv = false;
    if( (this.closeX >= 0) && (this.closeY >= 0) ) {
      int x = e.getX();
      int y = e.getY();
      if( (x >= this.closeX) && (x < (this.closeX + this.closeW))
	  && (y >= this.closeY) && (y < (this.closeY + this.closeH)) )
      {
	rv = true;
      }
    }
    return rv;
  }


  private void mouseClickedInternal( MouseEvent e )
  {
    updMouseBtnDown( e );
    if( isOverClose( e ) ) {
      JScrollPane sp = null;
      Component   c  = this;
      while( (sp == null) && (c != null) ) {
	if( c instanceof JScrollPane ) {
	  sp = (JScrollPane) c;
	  break;
	}
	c = c.getParent();
      }
      if( sp != null ) {
	sp.setRowHeader( null );
      }
      e.consume();
    }
  }


  private void mouseMovedInternal( MouseEvent e )
  {
    boolean newState = isOverClose( e );
    if( newState != this.overClose ) {
      this.overClose = newState;
      repaint();
    }
    updMouseBtnDown( e );
  }


  private void updMouseBtnDown( MouseEvent e )
  {
    boolean newState = ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK)
						!= 0);
    if( newState != this.mouseBtnDown ) {
      this.mouseBtnDown = newState;
      repaint();
    }
  }
}
