/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Halte-/Log-Punkt, der importiert werden
 * und somit auch eine Marke haben kann
 */

package jkcemu.tools.debugger;

import jkcemu.programming.assembler.AsmLabel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public abstract class ImportableBreakpoint extends AbstractBreakpoint
{
  public static final String ATTR_NAME     = "name";
  public static final String ATTR_IMPORTED = "imported";

  private String  name;
  private boolean imported;


  protected ImportableBreakpoint(
			DebugFrm debugFrm,
			String   name ) throws InvalidParamException
  {
    super( debugFrm );
    this.name     = checkName( name );
    this.imported = false;
  }


  protected static String checkName( String name )
					throws InvalidParamException
  {
    if( name != null ) {
      int len = name.length();
      if( len > 0 ) {
	for( int i = 0; i < len; i++ ) {
	  char ch = name.charAt( i );
	  if( ((i == 0) && !AsmLabel.isIdentifierStart( ch ))
	      || !AsmLabel.isIdentifierPart( ch ) )
	  {
	    throw new InvalidParamException(
				name + ": Ung\u00FCltiger Name" );
	  }
	}
      } else {
	name = null;
      }
    }
    return name;
  }


  public abstract int getAddress();


  /*
   * Die Methode liefert entweder den Namen oder null,
   * jedoch niemals einen leeren String zurueck.
   */
  public String getName()
  {
    return this.name;
  }


  public boolean getImported()
  {
    return this.imported;
  }


  public void setImported( boolean state )
  {
    this.imported = state;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void appendAttributesTo( Element elem )
  {
    if( this.name != null ) {
      if( !this.name.isEmpty() ) {
	elem.setAttribute( ATTR_NAME, name );
      }
    }
    elem.setAttribute( ATTR_IMPORTED, Boolean.toString( this.imported ) );
    super.appendAttributesTo( elem );
  }
}
