/*
 * (c) 2012-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Ausdruck fuer eine BASIC-Zeile (Zeilennummer oder Marke)
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.text.CharacterIterator;
import jkcemu.programming.*;


public class BasicLineExpr extends BasicSourcePos
{
  private String  exprText;
  private boolean label;


  public static BasicLineExpr checkBasicLineExpr(
				CharacterIterator iter,
				PrgSource         source,
				long              basicLineNum )
  {
    BasicLineExpr rv = null;
    char          ch = iter.current();
    while( (ch != CharacterIterator.DONE) && PrgUtil.isWhitespace( ch ) ) {
      ch = iter.next();
    }
    if( ((ch >= '0') && (ch <= '9'))
	|| ((ch >= 'A') && (ch <= 'Z'))
	|| ((ch >= 'a') && (ch <= 'z')) )
    {
      boolean       label = ((ch < '0') || (ch > '9'));
      StringBuilder buf   = new StringBuilder();
      buf.append( ch );
      ch = iter.next();
      while( ((ch >= '0') && (ch <= '9'))
	     || label && (((ch >= 'A') && (ch <= 'Z'))
			    || ((ch >= 'a') && (ch <= 'z'))
			    || (ch == '_')) )
      {
	buf.append( ch );
	ch = iter.next();
      }
      rv = new BasicLineExpr( source, basicLineNum, buf.toString(), label );
    }
    return rv;
  }


  public String getExprText()
  {
    return this.exprText;
  }


  public boolean isLabel()
  {
    return this.label;
  }


	/* --- Konstruktor --- */

  private BasicLineExpr(
		PrgSource source,
		long      basicLineNum,
		String    exprText,
		boolean   label )
  {
    super( source, basicLineNum );
    this.exprText = exprText;
    this.label    = label;
  }
}

