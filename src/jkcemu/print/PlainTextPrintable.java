/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Drucken einer Textes
 */

package jkcemu.print;

import java.awt.*;
import java.awt.print.*;
import java.lang.*;
import jkcemu.Main;


public class PlainTextPrintable implements Printable
{
  private String text;
  private int    tabSize;
  private String fileName;


  public PlainTextPrintable( String text, int tabSize, String fileName )
  {
    this.text     = text;
    this.tabSize  = tabSize;
    this.fileName = fileName;
  }


	/* --- Printable --- */

  @Override
  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    int rv = NO_SUCH_PAGE;
    if( (this.text != null) && (this.tabSize > 0) ) {

      // Anzahl Zeilen pro Seite
      int fontSize     = Main.getPrintFontSize();
      int linesPerPage = (int) pf.getImageableHeight() / fontSize;
      if( Main.getPrintFileName() || Main.getPrintPageNum() ) {
	linesPerPage -= 2;
      }
      if( (linesPerPage < 1) || (pf.getImageableWidth() < 1.0) ) {
	throw new PrinterException(
		"Die Seite hat keinen bedruckbaren Bereich,\n"
			+ "da die R\u00E4nder zu gro\u00DF sind." );
      }

      // vorherige Seiten ueberspringen
      int linesToSkip = pageNum * linesPerPage;
      int len         = this.text.length();
      int pos         = 0;
      while( (linesToSkip > 0) && (pos < len) ) {
	int i = this.text.indexOf( '\n', pos );
	if( i < pos ) {
	  pos = len;
	  break;
	}
	pos = i + 1;
	--linesToSkip;
      }
      if( (linesToSkip == 0) && (pos < len) ) {

	// Seite drucken
	g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );
	g.setColor( Color.black );

	int x = (int) pf.getImageableX();
	int y = (int) pf.getImageableY() + fontSize;
	while( (linesPerPage > 0) && (pos < len) ) {
	  int i = this.text.indexOf( '\n', pos );
	  if( i >= pos ) {
	    g.drawString(
		PrintUtil.expandTabs(
				this.text.substring( pos, i ),
				this.tabSize ),
		x,
		y );
	    pos = i + 1;
	  } else {
	    g.drawString(
		PrintUtil.expandTabs(
				this.text.substring( pos ),
				this.tabSize ),
		x,
		y );
	    pos = len;
	  }
	  y += fontSize;
	  --linesPerPage;
	}

	// Fusszeile
	fontSize -= 2;
	if( fontSize < 6 ) {
	  fontSize = 6;
	}
	y = (int) (pf.getImageableY() + pf.getImageableHeight());
	if( Main.getPrintFileName() && (this.fileName != null) ) {
	  g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );
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
	  if( Main.getPrintPageNum() )
	    PrintUtil.printCenteredPageNum( g, pf, fontSize, pageNum + 1 );
	}
	rv = PAGE_EXISTS;
      }
    }
    return rv;
  }
}

