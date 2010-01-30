/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten eines Druckauftrags
 */

package jkcemu.print;

import java.awt.*;
import java.awt.print.*;
import java.io.*;
import java.lang.*;
import jkcemu.Main;


public class PrintData implements Printable
{
  private int                   entryNum;
  private ByteArrayOutputStream byteStream;
  private byte[]                byteArray;


  public PrintData( int entryNum )
  {
    this.entryNum   = entryNum;
    this.byteStream = null;
    this.byteArray  = null;
  }


  public synchronized byte[] getBytes()
  {
    if( this.byteArray == null ) {
      if( this.byteStream != null )
	this.byteArray = this.byteStream.toByteArray();
    }
    return this.byteArray;
  }


  public int getEntryNum()
  {
    return this.entryNum;
  }


  public synchronized void putByte( int b )
  {
    if( this.byteStream == null ) {
      this.byteStream = new ByteArrayOutputStream( 0x1000 );
    }
    this.byteStream.write( b );
    this.byteArray = null;
  }


  public void saveToFile( File file ) throws IOException
  {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream( file );

      byte[] data = getBytes();
      if( data != null ) {
	out.write( data );
      }
      out.close();
      out = null;
    }
    finally {
      if( out != null ) {
	try {
	  out.close();
	}
	catch( IOException ex ) {}
      }
    }
  }


  public int size()
  {
    return this.byteStream != null ? this.byteStream.size() : 0;
  }


	/* --- Printable --- */

  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    byte[] dataBytes = getBytes();
    if( dataBytes == null )
      return pageNum == 0 ? PAGE_EXISTS : NO_SUCH_PAGE;

    PrintDataScanner scanner = new PrintDataScanner( dataBytes );

    // Anzahl Zeilen pro Seite
    int fontSize     = Main.getPrintFontSize();
    int linesPerPage = (int) pf.getImageableHeight() / fontSize;
    if( Main.getPrintPageNum() ) {
      linesPerPage -= 2;
    }
    if( (linesPerPage < 1) || (pf.getImageableWidth() < 1.0) ) {
      throw new PrinterException(
		"Die Seite hat keinen bedruckbaren Bereich,\n"
			+ "da die R\u00E4nder zu gro\u00DF sind." );
    }

    // vorherige Seiten ueberspringen
    if( pageNum > 0 ) {
      int pagesToSkip = pageNum;
      while( (pagesToSkip > 0) && !scanner.endReached() ) {
	int nLines = linesPerPage;
	while( (nLines > 0) && scanner.skipLine() ) {
	  --nLines;
	}
	scanner.skipFormFeed();
	--pagesToSkip;
      }
      if( (pagesToSkip > 0) || scanner.endReached() )
	return NO_SUCH_PAGE;
    }

    // Seite drucken
    g.setColor( Color.black );
    g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );

    int x = (int) pf.getImageableX();
    int y = (int) pf.getImageableY() + fontSize;

    String line = scanner.readLine();
    while( (line != null) && (linesPerPage > 0) ) {
      g.drawString( PrintUtil.expandTabs( line, 8 ), x, y );
      y += fontSize;
      --linesPerPage;
      line = scanner.readLine();
    }

    // Fusszeile
    if( Main.getPrintPageNum() ) {
      fontSize -= 2;
      if( fontSize < 6 ) {
	fontSize = 6;
      }
      PrintUtil.printCenteredPageNum( g, pf, fontSize, pageNum + 1 );
    }

    return PAGE_EXISTS;
  }
}

