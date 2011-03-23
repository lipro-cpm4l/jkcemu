/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Assemblermarke
 */

package jkcemu.programming.assembler;

import java.lang.*;


public class AsmLabel implements /*Comparable<AsmLabel>,*/ jkcemu.tools.Label
{
  private String labelName;
  private String hex16String;
  private int    labelValue;


  public AsmLabel( String labelName, int labelValue )
  {
    this.labelName   = labelName;
    this.labelValue  = labelValue;
    this.hex16String = null;
  }


  public void setLabelValue( int value )
  {
    this.labelValue = value;
  }


  public String toHex16String()
  {
    if( this.hex16String == null ) {
      this.hex16String = String.format( "%04X", this.labelValue );
    }
    return this.hex16String;
  }


	/* --- jkcemu.tools.Label --- */

  @Override
  public String getLabelName()
  {
    return this.labelName;
  }


  @Override
  public int getLabelValue()
  {
    return this.labelValue;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( jkcemu.tools.Label label )
  {
    return label != null ? (this.labelValue - label.getLabelValue()) : -1;
  }
}
