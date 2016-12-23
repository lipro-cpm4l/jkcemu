/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer das Drucken
 */

package jkcemu.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.*;
import java.util.Locale;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import jkcemu.Main;
import jkcemu.base.BaseDlg;


public class PrintUtil
{
  public static boolean doPrint(
				Frame     owner,
				Printable printable,
				String    jobName )
  {
    boolean status = false;
    try {

      // Optionen setzen
      PrintRequestAttributeSet attrs = Main.getPrintRequestAttributeSet();
      if( attrs == null ) {
	attrs = new HashPrintRequestAttributeSet();
	attrs.add( MediaSizeName.ISO_A4 );
	attrs.add( OrientationRequested.PORTRAIT );
      }
      if( jobName == null ) {
	jobName = "JKCEMU";
      }
      attrs.add( new Copies( 1 ) );
      attrs.add( new JobName( jobName, Locale.getDefault() ) );

      PrinterJob pj = PrinterJob.getPrinterJob();
      pj.setCopies( 1 );
      pj.setJobName( jobName );
      if( pj.printDialog( attrs ) ) {
        pj.setPrintable( printable );
        pj.print( attrs );
	Main.setPrintRequestAttributeSet( attrs );
	status = true;
      }
    }
    catch( PrinterException ex ) {
      BaseDlg.showErrorDlg( owner, ex.getMessage() );
    }
    return status;
  }


  /*
   * Die Methode wandelt in dem uebergebenen Text die Tabulatoren
   * in entsprechend viele Leerzeichen um und gibt den Text zurueck.
   */
  public static String expandTabs( String text, int tabSize )
  {
    String rv = "";
    if( text != null ) {
      if( text.indexOf( '\t' ) >= 0 ) {
	StringBuilder buf = new StringBuilder();
	int           len = text.length();
	for( int i = 0; i < len; i++ ) {
	  char ch = text.charAt( i );
	  if( ch == '\t' ) {
	    int n = tabSize - (buf.length() % tabSize);
	    for( int k = 0; k < n; k++ ) {
	      buf.append( (char) '\u0020' );
	    }
	  } else {
	    buf.append( ch );
	  }
	}
	rv = buf.toString();
      } else {
	rv = text;
      }
    }
    return rv;
  }


  /*
   * Die Methode druckt am unteren Rand zentriert eine Seitenzahl
   */
  public static void printCenteredPageNum(
				Graphics   g,
				PageFormat pf,
				int        fontSize,
				int        pageNum )
  {
    String s = "- " + String.valueOf( pageNum ) + " -";
    int    w = g.getFontMetrics().stringWidth( s );
    g.setColor( Color.black );
    g.setFont( new Font( Font.MONOSPACED, Font.PLAIN, fontSize ) );
    g.drawString(
	s,
	(int) (pf.getImageableX() + (pf.getImageableWidth() / 2))
			- (g.getFontMetrics().stringWidth( s ) / 2),
	(int) (pf.getImageableY() + pf.getImageableHeight()) );
  }
}
