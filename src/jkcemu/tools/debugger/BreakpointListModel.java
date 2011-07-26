/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * ListModel fuer Haltepunkte, welches java.util.List implementiert
 */

package jkcemu.tools.debugger;

import java.lang.*;
import java.util.ArrayList;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;


public class BreakpointListModel
			extends ArrayList<AbstractBreakpoint>
			implements ListModel
{
  private java.util.List<ListDataListener> listeners;


  public BreakpointListModel()
  {
    this.listeners = null;
  }


	/* --- ListModel --- */

  @Override
  public void addListDataListener( ListDataListener l )
  {
    if( this.listeners == null ) {
      this.listeners = new ArrayList<ListDataListener>();
    }
    this.listeners.add( l );
  }


  @Override
  public Object getElementAt( int idx )
  {
    return get( idx );
  }


  @Override
  public int getSize()
  {
    return size();
  }


  @Override
  public void removeListDataListener( ListDataListener l )
  {
    if( this.listeners != null )
      this.listeners.remove( l );
  }
}

