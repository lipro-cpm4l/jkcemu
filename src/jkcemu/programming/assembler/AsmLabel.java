/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Assemblermarke
 */

package jkcemu.programming.assembler;

import java.lang.*;


public class AsmLabel implements Comparable<AsmLabel>
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


  public String getLabelName()
  {
    return this.labelName;
  }


  public int getLabelValue()
  {
    return this.labelValue;
  }


  public void setLabelValue( int value )
  {
    this.labelValue = value;
  }


  public String toHex16String()
  {
    if( this.hex16String == null ) {
      StringBuilder buf = new StringBuilder( 4 );
      appendHexDigit( buf, this.labelValue >> 12 );
      appendHexDigit( buf, this.labelValue >> 8 );
      appendHexDigit( buf, this.labelValue >> 4 );
      appendHexDigit( buf, this.labelValue );
      this.hex16String = buf.toString();
    }
    return this.hex16String;
  }


	/* --- Comparable --- */

  public int compareTo( AsmLabel label )
  {
    return label != null ? (this.labelValue - label.labelValue) : -1;
  }


	/* --- private Methoden --- */

  private static void appendHexDigit( StringBuilder buf, int value )
  {
    value &= 0x0F;
    if( value < 10 ) {
      buf.append( (char) (value + '0') );
    } else {
      buf.append( (char) (value - 10 + 'A') );
    }
  }
}

