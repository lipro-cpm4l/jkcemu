/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine SELECT-Anweisung
 */

package jkcemu.programming.basic;

import java.util.HashSet;
import java.util.Set;
import jkcemu.programming.PrgSource;


public class SelectEntry extends BasicSourcePos
{
  private String      caseLabel;
  private String      endLabel;
  private boolean     elseDone;
  private Set<Object> values;


  public SelectEntry(
		PrgSource source,
		long      basicLineNum,
		String    caseLabel,
		String    endLabel )
  {
    super( source, basicLineNum );
    this.caseLabel = caseLabel;
    this.endLabel  = endLabel;
    this.elseDone  = false;
    this.values    = new HashSet<>();
  }


  public boolean checkUniqueCaseValue(
				BasicCompiler compiler,
				Object        value )
  {
    boolean rv = false;
    if( this.values.contains( value ) ) {
      compiler.putWarning( "CASE-Wert \'"
				+ value.toString()
				+ "\' bereits vorhanden" );
    } else {
      this.values.add( value );
      rv = true;
    }
    return rv;
  }


  public String getCaseLabel()
  {
    return this.caseLabel;
  }


  public String getEndLabel()
  {
    return this.endLabel;
  }


  public boolean isElseDone()
  {
    return this.elseDone;
  }


  public void setCaseLabel( String label )
  {
    this.caseLabel = label;
  }


  public void setElseDone()
  {
    this.elseDone = true;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "SELECT-Anweisung";
  }
}
