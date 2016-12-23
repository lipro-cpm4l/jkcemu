/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Scanner zum Extrahieren der Zeilen und Seitenumbrueche
 * aus den Druckdaten
 */

package jkcemu.print;

import java.lang.*;


public class PrintDataScanner
{
  private byte[] dataBytes;
  private int    pos;


  public PrintDataScanner( byte[] dataBytes )
  {
    this.dataBytes = dataBytes;
    this.pos       = 0;
  }


  public boolean endReached()
  {
    return this.pos >= this.dataBytes.length;
  }


  public String readLine()
  {
    StringBuilder buf = null;
    while( this.pos < this.dataBytes.length ) {
      int b = ((int) this.dataBytes[ this.pos ]) & 0xFF;

      // Seitenumbruch
      if( b == 0x0C ) {
	break;
      }

      // Zeile vorhanden
      this.pos++;
      if( buf == null ) {
	buf = new StringBuilder();
      }

      // Zeilenende?
      if( (b == 0x0A) || (b == 0x0D) || (b == 0x1E) ) {
	if( b == 0x0D ) {
	  if( this.pos < this.dataBytes.length ) {
	    if( this.dataBytes[ this.pos ] == 0x0A ) {
	      this.pos++;
	    }
	  }
	}
	break;
      }

      if( (b != 0) && (b != 3) ) {
	buf.append( (char) b );
      }
    }
    return buf != null ? buf.toString() : null;
  }


  public boolean skipFormFeed()
  {
    if( this.pos < this.dataBytes.length ) {
      if( this.dataBytes[ this.pos ] == 0x0C ) {
	this.pos++;
	return true;
      }
    }
    return false;
  }


  public boolean skipLine()
  {
    boolean rv = false;
    while( this.pos < this.dataBytes.length ) {
      int b = ((int) this.dataBytes[ this.pos ]) & 0xFF;
      if( b == 0x0C ) {
	break;
      }

      // Zeile vorhanden
      this.pos++;
      rv = true;

      // Zeilenende?
      if( (b == 0x0A) || (b == 0x0D) || (b == 0x1E) ) {
	if( b == 0x0D ) {
	  if( this.pos < this.dataBytes.length ) {
	    if( this.dataBytes[ this.pos ] == 0x0A ) {
	      this.pos++;
	    }
	  }
	}
	break;
      }
    }
    return rv;
  }
}
