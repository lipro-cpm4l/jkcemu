/*
 * (c) 2010-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verwaltung einer Tab-Sequenz durch explizite Angabe
 * der einzelnen Komponenten
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import javax.swing.SwingUtilities;


public class ListFocusTraversalPolicy extends FocusTraversalPolicy
{
  private Component[] components;


  public ListFocusTraversalPolicy( Component... components )
  {
    this.components = components;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Component getComponentAfter( Container container, Component c )
  {
    return getComponentByOffs( c, 1 );
  }


  @Override
  public Component getComponentBefore( Container container, Component c )
  {
    return getComponentByOffs( c, -1 );
  }


  @Override
  public Component getDefaultComponent( Container container )
  {
    return getFirstComponent( container );
  }


  @Override
  public Component getFirstComponent( Container container )
  {
    Component rv = null;
    if( this.components != null ) {
      if( this.components.length > 0 ) {
	rv = this.components[ 0 ];
      }
    }
    return rv;
  }


  @Override
  public Component getLastComponent( Container container )
  {
    Component rv = null;
    if( this.components != null ) {
      if( this.components.length > 0 ) {
	rv = this.components[ this.components.length - 1 ];
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private Component getComponentByOffs( Component c, int offs )
  {
    Component rv = null;
    if( (this.components != null) && (c != null) ) {
      if( this.components.length > 0 ) {
	int idx = -1;
	for( int i = 0; i < this.components.length; i++ ) {
	  if( (c == this.components[ i ])
	      || SwingUtilities.isDescendingFrom( c, this.components[ i ] ) )
	  {
	    rv = this.components[ (i + offs) % this.components.length ];
	    break;
	  }
	}
      }
    }
    return rv;
  }
}
