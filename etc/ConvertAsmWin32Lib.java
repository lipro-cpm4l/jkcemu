/*
 * (c) 2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Beim Compilieren der JKCEMU C-Bibliothek fuer Win32
 * haengt der gcc an die Marken der oeffentlichen Funktionen
 * ein @ gefolgt von einer Nummer an.
 * Diese Anhaenge muessen vor dem Assemblieren entfernt werden.
 * Anderenfalls kommt es beim Aufruf der Funktionen aus Java
 * zu einem java.lang.UnsatisfiedLinkError.
 *
 * Dieses Programm konvertiert in der erzeugten Assemblerdatei
 * die Marken der oeffentlichen Funktionen,
 * d.h., es schneidet die @-Anhaenge ab.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;


public class ConvertAsmWin32Lib
{
  public static void main( String[] args )
  {
    for( String arg : args ) {
      try {
	convert( arg );
      }
      catch( IOException ex ) {
	System.out.println( ex.getMessage() );
      }
    }
  }


  private static void convert( String fileName ) throws IOException
  {
    String lineSep = System.getProperty( "line.separator" );
    if( lineSep == null ) {
      lineSep = "\r\n";
    }

    // Datei lesen und dabei die zu konvertierenden Marken ermitteln
    File               file   = new File( fileName );
    int                len    = (int) file.length();
    StringBuilder      buf    = new StringBuilder( len > 0 ? len : 0x400 );
    Map<String,String> labels = new HashMap<>();
    BufferedReader     reader = null;
    try {
      reader      = new BufferedReader( new FileReader( file ) );
      String line = reader.readLine();
      while( line != null ) {
	if( line.startsWith( "_Java_" ) ) {
	  int idx1 = line.indexOf( '@' );
	  int idx2 = line.indexOf( ':' );
	  if( (idx1 > 0) && (idx2 > idx1) ) {
	    labels.put(
		line.substring( 1, idx2 ),	// alte Marke ohne Unterstrich
		line.substring( 1, idx1 ) );	// neue Marke ohne Unterstrich
	  }
	}
	buf.append( line );
	buf.append( lineSep );
	line = reader.readLine();
      }
    }
    finally {
      if( reader != null ) {
	reader.close();
      }
    }

    // Marken konvertieren
    for( String oldLabel : labels.keySet() ) {
      String newLabel = labels.get( oldLabel );
      if( newLabel != null ) {
	for(;;) {
	  int idx = buf.indexOf( oldLabel );
	  if( idx < 0 ) {
	    break;
	  }
	  buf = buf.replace( idx, idx + oldLabel.length(), newLabel );
	}
      }
    }

    // Datei schreiben
    File   outFile = new File( fileName + ".out" );
    Writer writer  = null;
    try {
      writer = new FileWriter( outFile );
      writer.write( buf.toString() );
      writer.close();
      writer = null;
      if( !file.delete() ) {
	throw new IOException( "Loeschen der Quelldatei fehlgeschlagen" );
      }
      outFile.renameTo( file );
    }
    finally {
      if( writer != null ) {
	writer.close();
      }
    }
  }
}
