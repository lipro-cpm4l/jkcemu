/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * RowHeader-Komponente zur Anzeige von Adressen
 */

package jkcemu.text;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import jkcemu.Main;


public class LineAddrRowHeader extends JComponent
{
  private static final int MARGIN = 5;

  private JTextArea            textArea;
  private Map<Integer,Integer> lineAddrMap;
  private Image                closeImg;
  private int                  closeX;
  private int                  closeY;
  private int                  closeW;
  private int                  closeH;


  public LineAddrRowHeader(
		JTextArea            textArea,
		Map<Integer,Integer> lineAddrMap )
  {
    this.textArea    = textArea;
    this.lineAddrMap = lineAddrMap;
    this.closeImg    = Main.getImage( this, "/images/file/close.png" );
    this.closeX      = 0;
    this.closeY      = 0;
    this.closeW      = 0;
    this.closeH      = 0;
    setToolTipText( "Adressen im Arbeitsspeicher" );
    addMouseListener(
	new MouseAdapter()
	{
	  @Override
	  public void mouseClicked( MouseEvent e )
	  {
	    mouseClickedInternal( e );
	  }
	} );
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
      rv = new Dimension( w, h );
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
      g.setColor( Color.gray );
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

	  int pos = this.textArea.viewToModel( new Point( x, yVisible ) );
	  if( pos >= 0 ) {
	    begLine = this.textArea.getLineOfOffset( pos );
	  }
	  pos = this.textArea.viewToModel(
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
	      Rectangle r = this.textArea.modelToView(
				this.textArea.getLineStartOffset( line ) );
	      if( r != null ) {
		int y = r.y + r.height - 5;
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
      if( this.closeImg != null ) {
	int closeW = this.closeImg.getWidth( this );
	int closeH = this.closeImg.getHeight( this );
	if( (closeW > 0) && (closeH > 0) && (closeW < (w - (2 * MARGIN))) ) {
	  this.closeX = w - MARGIN - closeW - 1;
	  this.closeY = yVisible + 1;
	  this.closeW = closeW;
	  this.closeH = closeH;
	  g.setColor( getBackground() );
	  g.fillRect(
		this.closeX - 1,
		this.closeY - 1,
		this.closeW + 2,
		this.closeH + 2 );
	  g.drawImage( this.closeImg, this.closeX, this.closeY, this );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private boolean isOverClose( MouseEvent e )
  {
    boolean rv = false;
    if( (this.closeW > 0) && (this.closeH > 0) ) {
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
}
