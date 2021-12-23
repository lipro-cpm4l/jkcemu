/*
 * (c) 2009-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Drucken eines Textes
 */

package jkcemu.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import jkcemu.Main;


public class PlainTextPrintable implements Printable
{
  private String text;
  private int    tabSize;
  private int    contentFontSize;
  private Font   contentFont;
  private Font   footerFont;
  private String fileName;


  public PlainTextPrintable(
			String text,
			int    tabSize,
			Font   font,
			String fileName )
  {
    this.text            = text;
    this.tabSize         = tabSize;
    this.fileName        = fileName;
    this.footerFont      = null;
    this.contentFont     = null;
    this.contentFontSize = Main.getPrintFontSize();
    if( this.contentFontSize > 0 ) {
      if( font != null ) {
	if( font.getSize() == this.contentFontSize ) {
	  this.contentFont = font;
	} else {
	  this.contentFont = font.deriveFont( (float) this.contentFontSize );
	}
      } else {
	this.contentFont = new Font(
				Font.MONOSPACED,
				Font.PLAIN,
				this.contentFontSize );
      }
      int footerSize = this.contentFontSize - 2;
      if( footerSize < 6 ) {
	footerSize = 6;
      }
      this.footerFont = this.contentFont.deriveFont( (float) footerSize );
    }
  }


	/* --- Printable --- */

  @Override
  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    int rv = NO_SUCH_PAGE;
    if( (this.text != null)
	&& (this.tabSize > 0)
	&& (this.contentFontSize > 0)
	&& (this.contentFont != null) )
    {
      // Anzahl Zeilen pro Seite
      int linesPerPage = (int) pf.getImageableHeight() / this.contentFontSize;
      if( Main.getPrintFileName() || Main.getPrintPageNum() ) {
	linesPerPage -= 2;
      }
      if( (linesPerPage < 1) || (pf.getImageableWidth() < 1.0) ) {
	throw new PrinterException(
		"Die Seite hat keinen bedruckbaren Bereich,\n"
			+ "da die R\u00E4nder zu gro\u00DF sind." );
      }

      // vorherige Seiten ueberspringen
      int pagesToSkip = pageNum;
      int len         = this.text.length();
      int pos         = 0;
      int linesOnPage = 0;
      while( (pagesToSkip > 0) && (pos < len) ) {
	char ch = this.text.charAt( pos++ );
	if( ch == '\f' ) {
	  --pagesToSkip;
	  linesOnPage = 0;
	} else if( ch == '\n' ) {
	  linesOnPage++;
	  if( linesOnPage > linesPerPage ) {
	    linesOnPage = 0;
	    --pagesToSkip;
	    linesOnPage = 0;
	  }
	}
      }
      if( (pagesToSkip == 0) && (pos < len) ) {

	// Seite drucken
	g.setFont( this.contentFont );
	g.setColor( Color.BLACK );

	int x = (int) pf.getImageableX();
	int y = (int) pf.getImageableY() + this.contentFontSize;
	while( (linesPerPage > 0) && (pos < len) ) {
	  boolean newPage = false;
	  int     eop     = this.text.indexOf( '\f', pos );
	  int     idx     = this.text.indexOf( '\n', pos );
	  if( (idx < 0) || ((eop >= 0) && (eop < idx)) ) {
	    idx     = eop;
	    newPage = true;
	  }
	  if( idx >= pos ) {
	    g.drawString(
		PrintUtil.expandTabs(
				this.text.substring( pos, idx ),
				this.tabSize ),
		x,
		y );
	    pos = idx + 1;
	  } else {
	    g.drawString(
		PrintUtil.expandTabs(
				this.text.substring( pos ),
				this.tabSize ),
		x,
		y );
	    pos = len;
	  }
	  if( newPage ) {
	    break;
	  }
	  y += this.contentFontSize;
	  --linesPerPage;
	}

	// Fusszeile
	if( this.footerFont != null ) {
	  y = (int) (pf.getImageableY() + pf.getImageableHeight());
	  if( Main.getPrintFileName() && (this.fileName != null) ) {
	    g.setFont( this.footerFont );
	    g.drawString( this.fileName, x, y );
	    if( Main.getPrintPageNum() ) {
	      String s = String.valueOf( pageNum + 1 );
	      g.drawString(
		s,
		(int) (pf.getImageableX() + pf.getImageableWidth()
				- g.getFontMetrics().stringWidth( s )),
		y );
	    }
	  } else {
	    if( Main.getPrintPageNum() ) {
	      PrintUtil.printCenteredPageNum(
					g,
					pf,
					this.footerFont,
					pageNum + 1 );
	    }
	  }
	}
	rv = PAGE_EXISTS;
      }
    }
    return rv;
  }
}
