/*
 * (c) 2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstraktion einer BASIC-Quelltextstelle
 */

package jkcemu.programming.basic;

import java.lang.*;
import jkcemu.programming.PrgSource;


public class BasicSourcePos
{
  private PrgSource source;
  private long      basicLineNum;


  public BasicSourcePos( PrgSource source, long basicLineNum )
  {
    this.source       = source;
    this.basicLineNum = basicLineNum;
  }


  public boolean appendMsgPrefixTo( String msgType, StringBuilder buf )
  {
    boolean appended   = false;
    String  srcName    = null;
    int     srcLineNum = 0;
    if( this.source != null ) {
      srcLineNum = this.source.getLineNum();
      srcName    = this.source.getName();
    }
    if( (srcLineNum > 0) || (this.basicLineNum >= 0) ) {
      if( srcName != null ) {
	if( !srcName.isEmpty() ) {
	   buf.append( srcName );
	   buf.append( ": " );
	}
      }
      if( msgType != null ) {
        buf.append( msgType );
        buf.append( " in " );
      }
      if( srcLineNum > 0 ) {
	buf.append( "Zeile " );
	buf.append( srcLineNum );
	if( this.basicLineNum >= 0 ) {
	  buf.append( " (BASIC-Zeilennummer " );
	  buf.append( this.basicLineNum );
	  buf.append( (char) ')' );
	}
      } else if( this.basicLineNum >= 0 ) {
        buf.append( " BASIC-Zeile " );
        buf.append( this.basicLineNum );
      }
      appended = true;
    }
    return appended;
  }


  public long getBasicLineNum()
  {
    return this.basicLineNum;
  }


  public PrgSource getSource()
  {
    return this.source;
  }
}

