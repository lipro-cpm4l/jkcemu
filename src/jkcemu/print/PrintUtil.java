/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer das Drucken
 */

package jkcemu.print;

import java.awt.*;
import java.awt.print.*;
import java.lang.*;
import java.util.Locale;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import jkcemu.Main;
import jkcemu.base.BasicDlg;


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
      BasicDlg.showErrorDlg( owner, ex.getMessage() );
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
    g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );
    g.drawString(
	s,
	(int) (pf.getImageableX() + (pf.getImageableWidth() / 2))
			- (g.getFontMetrics().stringWidth( s ) / 2),
	(int) (pf.getImageableY() + pf.getImageableHeight()) );
  }
}

