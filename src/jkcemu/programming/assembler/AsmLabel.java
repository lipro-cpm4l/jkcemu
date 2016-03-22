/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Assemblermarke
 */

package jkcemu.programming.assembler;

import java.lang.*;


public class AsmLabel implements jkcemu.tools.Label
{
  private String labelName;
  private Object labelValue;
  private String hex16String;
  private int    varSize;


  public AsmLabel( String labelName, int labelValue )
  {
    this.labelName   = labelName;
    this.labelValue  = labelValue;
    this.hex16String = null;
    this.varSize     = -1;
  }


  public Object getLabelValue()
  {
    return this.labelValue;
  }


  public static boolean isIdentifierPart( char ch )
  {
    return (ch == '_') || (ch == '@') || (ch == '?')
		|| ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'))
		|| ((ch >= '0') && (ch <= '9'));
  }


  public static boolean isIdentifierStart( char ch )
  {
    return (ch == '_') || (ch == '@')
		|| ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'));
  }


  public void setLabelValue( Object value )
  {
    this.labelValue = value;
  }


  public void setVarSize( int varSize )
  {
    this.varSize = varSize;
  }


	/* --- jkcemu.tools.Label --- */

  @Override
  public String getLabelName()
  {
    return this.labelName;
  }


  @Override
  public int getVarSize()
  {
    return this.varSize;
  }


  @Override
  public int intValue()
  {
    int rv = 0;
    if( this.labelValue != null ) {
      if( this.labelValue instanceof Number ) {
	rv = ((Number) this.labelValue).intValue();
      }
    }
    return rv;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( jkcemu.tools.Label label )
  {
    return label != null ? (intValue() - label.intValue()) : -1;
  }
}
