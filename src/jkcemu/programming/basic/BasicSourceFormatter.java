/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Formatierer fuer BASIC-Quelltext
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.text.CharacterIterator;


public class BasicSourceFormatter
{
  private StringBuilder     buf;
  private CharacterIterator iter;
  private int               srcPos;
  private boolean           space;


  public BasicSourceFormatter( int initCapacity )
  {
    this.buf    = new StringBuilder( initCapacity );
    this.iter   = null;
    this.srcPos = 0;
    this.space  = false;
  }


  public void append( char ch )
  {
    formatLast();
    this.buf.append( ch );
  }


  public void append( String text )
  {
    formatLast();
    this.buf.append( text );
  }


  public void beginLine( CharacterIterator iter, long basicLineNum )
  {
    this.iter = iter;
    if( iter != null ) {
      this.srcPos = iter.getIndex();
    }
    if( basicLineNum >= 0 ) {
      this.buf.append( basicLineNum );
      this.space = true;
    } else {
      this.space = false;
    }
  }


  public void copyLast()
  {
    if( this.iter != null )
      processLastCharsTo( this.iter.getIndex(), false );
  }


  public void finishLine()
  {
    if( this.iter != null ) {
      processLastCharsTo( this.iter.getIndex(), true );
      this.iter = null;
    }
    this.buf.append( (char) '\n' );
  }


  public void formatLast()
  {
    if( this.iter != null )
      processLastCharsTo( this.iter.getIndex(), true );
  }


  public void replaceByKeyword(
			int     begPos,
			String  text,
			boolean space )
  {
    if( this.iter != null ) {
      processLastCharsTo( begPos, true );
      if( endsWithIdentifierChar() ) {
	this.buf.append( (char) '\u0020' );
      }
      this.buf.append( text );
      this.srcPos = this.iter.getIndex();
      this.space  = space;
    }
  }


  public void setSpace( boolean state )
  {
    this.space = state;
  }


	/* --- ueberschriebene Methoden --- */

  public String toString()
  {
    return this.buf.toString();
  }


	/* --- private Methoden --- */

  private boolean isIdentifierChar( char ch )
  {
    return ((ch >= '0') && (ch <= '9'))
		|| ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'))
		|| (ch == '_') || (ch == '$');
  }


  private boolean endsWithIdentifierChar()
  {
    boolean rv = false;
    int     n  = this.buf.length();
    if( n > 0 ) {
      rv = isIdentifierChar( this.buf.charAt( n - 1 ) );
    }
    return rv;
  }


  private void processLastCharsTo( int endPos, boolean format )
  {
    int pos = this.iter.getIndex();
    this.iter.setIndex( this.srcPos );
    char ch = this.iter.current();
    if( format ) {
      while( ((ch == '\t') || (ch == '\u0020'))
	     && (this.iter.getIndex() < endPos) )
      {
	ch = this.iter.next();
      }
    }
    if( isIdentifierChar( ch ) && endsWithIdentifierChar() ) {
      this.buf.append( (char) '\u0020' );
      this.space = false;
    }
    if( format ) {
      while( (ch != CharacterIterator.DONE)
	     && (this.iter.getIndex() < endPos) )
      {
	if( (ch != '\t') && (ch != '\u0020') ) {
	  if( this.space ) {
	    this.buf.append( (char) '\u0020' );
	    this.space = false;
	  }
	  this.buf.append( Character.toUpperCase( ch ) );
	}
	ch = this.iter.next();
      }
    } else {
      if( this.space
	  && (ch != CharacterIterator.DONE)
	  && (this.iter.getIndex() < endPos) )
      {
	this.buf.append( (char) '\u0020' );
	this.space = false;
      }
      while( (ch != CharacterIterator.DONE)
	     && (this.iter.getIndex() < endPos) )
      {
	this.buf.append( ch );
	ch = this.iter.next();
      }
    }
    this.srcPos = this.iter.getIndex();
    this.iter.setIndex( pos );
  }
}

