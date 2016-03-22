/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Iterator ueber die Argumente einer Kommandozeile
 */

package jkcemu.programming;

import java.io.*;
import java.lang.*;


public class CmdLineArgIterator implements Closeable
{
  private Reader   reader;
  private String[] ary;
  private int      idx;


  public static CmdLineArgIterator createFromStringArray(
						String[] ary,
						int      idx )
  {
    return new CmdLineArgIterator( null, ary, idx );
  }


  public static CmdLineArgIterator createFromReader( Reader reader )
  {
    return new CmdLineArgIterator( reader, null, 0 );
  }


  public synchronized String next() throws IOException
  {
    String rv = null;
    if( this.ary != null ) {
      if( this.idx < this.ary.length ) {
	rv = this.ary[ this.idx++ ];	
	if( rv == null ) {
	  rv = "";
	}
      }
    }
    else if( this.reader != null ) {
      int ch = this.reader.read();
      while( (ch >= 0) && isWhitespace( ch ) ) {
	ch = this.reader.read();
      }
      if( ch >= 0 ) {
	StringBuilder buf = new StringBuilder( 64 );
	if( ch == '\"' ) {
	  ch = this.reader.read();
	  while( (ch >= 0) && (ch != '\"') && !isWhitespace( ch ) ) {
	    buf.append( (char) ch );
	    ch = this.reader.read();
	  }
	  if( ch != '\"' ) {
	    throw new IOException(
		"In \'\"\' eingeschlossenes Argument nicht abgeschlossen" );
	  }
	} else {
	  buf.append( (char) ch );
	  ch = this.reader.read();
	  while( (ch >= 0) && !isWhitespace( ch ) ) {
	    buf.append( (char) ch );
	    ch = this.reader.read();
	  }
	}
	rv = buf.toString();
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized void close() throws IOException
  {
    try {
      if( this.reader != null ) {
	this.reader.close();
      }
    }
    finally {
      this.reader = null;
      this.ary    = null;
    }
  }


	/* --- private Methoden --- */

  private static boolean isWhitespace( int ch )
  {
    return (ch < 0x20) || Character.isWhitespace( ch );
  }


  private CmdLineArgIterator( Reader reader, String[] ary, int idx )
  {
    this.reader = reader;
    this.ary    = ary;
    this.idx    = idx;
  }
}
