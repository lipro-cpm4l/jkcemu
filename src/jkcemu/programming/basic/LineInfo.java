/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen zu einer BASIC-Zeilennummer
 */

package jkcemu.programming.basic;

import java.lang.*;


public class LineInfo implements Comparable<LineInfo>
{
  private int  sourceLineNum;
  private long basicLineNum;
  private Long missingBasicLineNum;


  public LineInfo( int sourceLineNum, long basicLineNum )
  {
    this.sourceLineNum       = sourceLineNum;
    this.basicLineNum        = basicLineNum;
    this.missingBasicLineNum = null;
  }


  public long getBasicLineNum()
  {
    return this.basicLineNum;
  }


  public int getSourceLineNum()
  {
    return this.sourceLineNum;
  }


  public Long getMissingBasicLineNum()
  {
    return this.missingBasicLineNum;
  }


  public void setMissingBasicLineNum( Long lineNum )
  {
    this.missingBasicLineNum = lineNum;
  }


	/* --- Comparable --- */

  public int compareTo( LineInfo data )
  {
    int rv = this.sourceLineNum - data.sourceLineNum;
    if( rv == 0 ) {
      if( this.missingBasicLineNum == null ) {
	rv = -1;
      } else {
	if( data.missingBasicLineNum == null ) {
	  rv = 1;
	} else {
	  rv = this.missingBasicLineNum.compareTo( data.missingBasicLineNum );
	}
      }
    }
    return rv;
  }
}

