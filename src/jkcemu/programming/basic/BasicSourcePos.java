/*
 * (c) 2014-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer BASIC-Quelltextstelle
 *
 * Beim Anlegen eines BasicSourcePos-Objektes wird
 * die aktuelle Quelltextstelle im BasicSourcePos-Objekte fixiert.
 */

package jkcemu.programming.basic;

import jkcemu.programming.PrgSource;


public class BasicSourcePos
{
  private String srcName;
  private int    srcLineNum;
  private long   basicLineNum;


  public BasicSourcePos( PrgSource source, long basicLineNum )
  {
    if( source != null ) {
      this.srcName    = source.getName();
      this.srcLineNum = source.getLineNum();
    } else {
      this.srcName    = null;
      this.srcLineNum = -1;
    }
    this.basicLineNum = basicLineNum;
  }


  public boolean appendMsgPrefixTo( String msgType, StringBuilder buf )
  {
    boolean appended   = false;
    if( (this.srcLineNum > 0) || (this.basicLineNum >= 0) ) {
      if( this.srcName != null ) {
	if( !this.srcName.isEmpty() ) {
	   buf.append( this.srcName );
	   buf.append( ": " );
	}
      }
      if( msgType != null ) {
        buf.append( msgType );
        buf.append( " in " );
      }
      if( this.srcLineNum > 0 ) {
	buf.append( "Zeile " );
	buf.append( this.srcLineNum );
	if( this.basicLineNum >= 0 ) {
	  buf.append( " (BASIC-Zeilennummer " );
	  buf.append( this.basicLineNum );
	  buf.append( ')' );
	}
      } else if( this.basicLineNum >= 0 ) {
        buf.append( " BASIC-Zeile " );
        buf.append( this.basicLineNum );
      }
      appended = true;
    }
    return appended;
  }
}
