/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bilder
 */

package jkcemu.image;

import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.*;


public class ImgUtil
{
  public static FileNameExtensionFilter createFileFilter( String[] suffixes )
  {
    FileNameExtensionFilter rv = null;
    if( suffixes != null ) {
      if( suffixes.length > 0 ) {
	rv = new FileNameExtensionFilter(
				"Unterst\u00FCtzte Bilddateien",
				suffixes );
      }
    }
    return rv;
  }


  public static File saveImage(
			Frame         owner,
			BufferedImage image )
  {
    File rv   = null;
    File file = EmuUtil.showFileSaveDlg(
			owner,
			"Bilddatei speichern",
			Main.getLastPathFile( "image" ),
			createFileFilter( ImageIO.getWriterFileSuffixes() ) );
    if( file != null ) {
      String fileName = file.getPath();
      if( fileName != null ) {
	try {
	  boolean formatOK      = false;
	  String  lowerFileName = fileName.toLowerCase();

	  // Grafikformat ermitteln
	  String   usedFmt = null;
	  String[] formats = ImageIO.getWriterFormatNames();
	  if( formats != null ) {
	    for( int i = 0; (usedFmt == null) && (i < formats.length); i++ ) {
	      String s = formats[ i ];
	      if( s != null ) {
		if( lowerFileName.endsWith( "." + s.toLowerCase() ) )
		  usedFmt = s;
	      }
	    }
	  }

	  /*
	   * Eigentliche wuerde sich die Funktion
	   * ImageIO.write( RenderedImage, String, File )
	   * gut eignen, doch wenn die Datei nicht angelegt werden kann,
	   * wird in der Funktion ein StackTrace geschrieben und
	   * anschliessend eine IllegalArgumentException
	   * statt einer IOException geworfen.
	   */
	  if( usedFmt != null ) {
	    OutputStream out = null;
	    try {
	      out      = new FileOutputStream( file );
	      formatOK = ImageIO.write( image, usedFmt, out );
	      out.close();
	      out = null;
	      Main.setLastFile( file, "screen" );
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

	  // erfolgreich gespeichert oder Format war falsch
	  if( formatOK ) {
	    Main.setLastFile( file, "image" );
	    rv = file;
	  } else {
	    java.util.List<String> fmtList = new ArrayList<String>();

	    // Grafikformate hinzufuegen
	    if( formats != null ) {
	      for( int i = 0; i < formats.length; i++ ) {
		String s = formats[ i ];
		if( s != null ) {
		  s = s.toLowerCase();
		  if( !fmtList.contains( s ) )
		    fmtList.add( s );
		}
	      }
	    }

	    // Fehlermeldung anzeigen
	    try {
	      Collections.sort( fmtList );
	    }
	    catch( ClassCastException ex ) {}
	    if( fmtList.isEmpty() ) {
	      BasicDlg.showErrorDlg(
			owner,
			"Die Funktion wird auf dieser Plattform\n"
				+ "nicht unterst\u00FCtzt." );
	    } else {
	      StringBuilder buf = new StringBuilder( 128 );

	      buf.append( "Das durch die Dateiendung angegebene Format\n"
			+ "wird nicht unterst\u00FCtzt.\n"
			+ "Folgende Dateiendungen sind m\u00F6glich:\n" );

	      int col = 0;
	      for( String s : fmtList ) {
		if( col > 0 ) {
		  buf.append( ", " );
		} else {
		  buf.append( (char) '\n' );
		}
		buf.append( s );
		col++;
		if( col >= 10 )
		  col = 0;
	      }
	      BasicDlg.showErrorDlg( owner, buf.toString() );
	    }
	  }
	}
	catch( Exception ex ) {
	  BasicDlg.showErrorDlg(
		owner,
		fileName + ":\nSpeichern der Datei fehlgeschlagen\n\n"
			+ ex.getMessage() );
	}
      }
    }
    return rv;
  }
}

