/*
 * (c) 2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Aendern einer TAR-Datei
 *
 * Die Implementierung ist rudimentaer und gestattet nur,
 * die Zeitstempel der letzten Aenderung aller Eintraege in der Datei
 * zu aendern.
 */

package jkcemu.file;

import java.io.Closeable;
import java.io.File;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;
import jkcemu.base.EmuUtil;


public class TarFileUpdater implements AutoCloseable, Closeable
{
  private boolean          cancelled;
  private RandomAccessFile raf;


  public TarFileUpdater( File file ) throws IOException
  {
    this.cancelled = false;
    this.raf       = new RandomAccessFile( file, "rw" );
  }


  public void cancel()
  {
    this.cancelled = true;
  }


  public void updateTimeOfAllEntries( long millis ) throws IOException
  {
    byte[] timeBuf = new byte[ 12 ];
    Arrays.fill( timeBuf, (byte) 0 );
    String s = String.format( "%o", millis / 1000 );
    int    n = s.length();
    if( (n > 0) && (n < 12) ) {
      for( int i = 0; i < n; i++ ) {
	timeBuf[ i ] = (byte) s.charAt( i );
      }
    }

    byte[] headerBuf = new byte[ 512 ];
    byte[] paxExBuf  = new byte[ 512 ];
    try {
      Long bodySize    = null;
      long filePointer = 0;
      while( !this.cancelled ) {
	this.raf.readFully( headerBuf );

	int typeByte = (int) headerBuf[ 156 ] & 0xFF;
	if( (typeByte == 'g') || (typeByte == 'x') ) {

	  // PaxHeader-Erweiterung lesen
	  this.raf.readFully( paxExBuf );

	  // im PaxHeader Zeitstempel aktualisieren
	  this.raf.seek( filePointer );
	  updTarHeader( headerBuf, timeBuf );

	  if( typeByte == 'x' ) {
	    /*
	     * bei einem lokalen PaxHeader
	     * aus der Erweiterung die Laenge ermitteln
	     */
	    try {
	      int pos = 0; 
	      while( pos < paxExBuf.length ) {
		int endPos = pos;
		while( endPos < paxExBuf.length ) {
		  byte b = paxExBuf[ endPos ];
		  if( (b == (byte) 0) || (b == (byte) 0x0A) ) {
		    break;
		  }
		  endPos++;
		}
		if( endPos > pos ) {
		  try {
		    String text = new String(
					paxExBuf,
					pos,
					endPos - pos,
					"US-ASCII" );
		    int idx = text.indexOf( " size=" );
		    if( (idx >= 0) && ((idx + 6) < text.length()) ) { 
		      bodySize = Long.valueOf( text.substring( idx + 6 ) );
		      break;
		    }
		  }
		  catch( NumberFormatException ex ) {}
		}
		pos = endPos + 1;
	      }
	    }
	    catch( UnsupportedEncodingException ex ) {}

	    // anschliessend die Zeitstempel aktualisieren
	    this.raf.seek( filePointer + 512 );
	    updPaxHeaderEx( paxExBuf, millis );
	  }
	  filePointer += (2 * paxExBuf.length);

	} else if( (typeByte == 0x00) || (typeByte == 0xFF) ) {

	  // unbekannter Block -> ueberspringen
	  filePointer += 512;
	  bodySize = null;

	} else {

	  // TAR-Kopf
	  if( bodySize == null ) {
	    bodySize = EmuUtil.parseOctalNumber( headerBuf, 124, 136 );
	  }
	  this.raf.seek( filePointer );
	  updTarHeader( headerBuf, timeBuf );

	  // naechste Kopfposition berechnen
	  filePointer += 512;
	  if( bodySize != null ) {
	    long blocks = (bodySize.longValue() + 511) / 512;
	    filePointer += (blocks * 512);
	  }
	  this.raf.seek( filePointer );
	  bodySize = null;
	}
      }
    }
    catch( EOFException ex ) {}
  }


	/* --- AutoCloseable, Closeable --- */

  @Override
  public void close() throws IOException
  {
    this.raf.close();
  }


	/* --- private Methoden ---- */

  private void updTarHeader(
			byte[] headerBuf,
			byte[] timeBuf ) throws IOException
  {
    System.arraycopy( timeBuf, 0, headerBuf, 136, timeBuf.length );
    TarPacker.computeAndSetHeaderChecksum( headerBuf );
    this.raf.write( headerBuf );
  }


  private void updPaxHeaderEx(
			byte[] headerBuf,
			long   millis ) throws IOException
  {
    byte[] dstBuf = new byte[ headerBuf.length ];
    Arrays.fill( dstBuf, (byte) 0 );

    int dstPos = 0;
    int pos    = 0;
    while( pos < headerBuf.length ) {
      int endPos = pos;
      while( endPos < headerBuf.length ) {
	byte b = headerBuf[ endPos ];
	if( (b == (byte) 0) || (b == (byte) 0x0A)) {
	  break;
	}
	endPos++;
      }
      if( endPos <= pos ) {
	break;
      }
      try {
	String text = new String(
				headerBuf,
				pos,
				endPos - pos,
				"US-ASCII" );
	if( text.indexOf( " mtime=" ) >= 0 ) {
	  String v = String.format(
				Locale.US,
				" mtime=%f",
				(double) millis / 1000.0 );
	  text = String.format( "%d %s", v.length() + 4, v );
	}
	else if( (text.indexOf( " ctime=" ) >= 0)
		 || (text.indexOf( " atime=" ) >= 0) )
	{
	  /*
	   * Zeitpunkte der Erstellung und des letzten Zugriffs
	   * entfernen, damit keine logischen Inkonsistenzen entstehen
	   */
	  text = null;
	}
	if( text != null ) {
	  int len = text.length();
	  if( (dstPos + len + 1) < dstBuf.length ) {
	    for( int i = 0; i < len; i++ ) {
	      dstBuf[ dstPos++ ] = (byte) text.charAt( i );
	    }
	    dstBuf[ dstPos++ ] = (byte) 0x0A;
	  }
	}
      }
      catch( UnsupportedEncodingException ex ) {}
      pos = endPos;
    }
    this.raf.write( dstBuf );
  }
}
