/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * ListModel fuer Halte-/Log-Punkte, welches java.util.List implementiert
 */

package jkcemu.tools.debugger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.AbstractListModel;


public class BreakpointListModel
			extends AbstractListModel<AbstractBreakpoint>
			implements Iterable<AbstractBreakpoint>
{
  private java.util.List<AbstractBreakpoint> items;


  public BreakpointListModel()
  {
    this.items = new ArrayList<>();
  }


  public void clear()
  {
    int n = this.items.size();
    if( n > 0 ) {
      this.items.clear();
      fireIntervalRemoved( this, 0, n - 1 );
    }
  }


  public void fireItemChanged( int idx )
  {
    int n = this.items.size();
    if( (idx >= 0) && (idx < n) ) {
      fireContentsChanged( this, 0, n - 1 );
    }
  }


  public ImportableBreakpoint getByName( String name )
  {
    ImportableBreakpoint bp = null;
    if( name != null ) {
      for( AbstractBreakpoint item : this.items ) {
	if( item instanceof ImportableBreakpoint ) {
	  String s = ((ImportableBreakpoint) item).getName();
	  if( s != null ) {
	    if( s.equals( name ) ) {
	      bp = (ImportableBreakpoint) item;
	      break;
	    }
	  }
	}
      }
    }
    return bp;
  }


  public boolean isEmpty()
  {
    return this.items.isEmpty();
  }


  public int put( AbstractBreakpoint bp )
  {
    int idx = Collections.binarySearch( this.items, bp );
    if( idx >= 0 ) {
      this.items.set( idx, bp);
      fireContentsChanged( this, idx, idx );
    } else {
      idx   = -(idx + 1);
      int n = this.items.size();
      if( idx < n ) {
	this.items.add( idx, bp );
      } else {
	idx = n;
	this.items.add( bp );
      }
      fireIntervalAdded( this, idx, idx );
    }
    return idx;
  }


  public void remove( int idx )
  {
    this.items.remove( idx );
    fireIntervalRemoved( this, idx, idx );
  }


  public void removeAll( Collection<? extends AbstractBreakpoint> c )
  {
    if( c != null ) {
      for( int i = this.items.size() - 1; i >= 0; --i ) {
	if( c.contains( this.items.get( i ) ) ) {
	  remove( i );
	}
      }
    }
  }


  public void sort()
  {
    int n = this.items.size();
    if( n > 0 ) {
      try {
	Collections.sort( this.items );
      }
      catch( ClassCastException ex ) {}
      fireContentsChanged( this, 0, n - 1 );
    }
  }


	/* --- Iterable --- */

  @Override
  public Iterator<AbstractBreakpoint> iterator()
  {
    return this.items.iterator();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public AbstractBreakpoint getElementAt( int idx )
  {
    return this.items.get( idx );
  }


  @Override
  public int getSize()
  {
    return this.items.size();
  }
}
