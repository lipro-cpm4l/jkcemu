/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verwaltung der Druckauftraege
 */

package jkcemu.print;

import java.lang.*;
import java.util.ArrayList;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;


public class PrintMngr extends AbstractTableModel
{
  private static final String[] colNames = {
					"Nummer",
					"Gr\u00F6\u00DFe",
					"Status" };

  private int                       nextEntryNum;
  private java.util.List<PrintData> entries;
  private PrintData                 activeEntry;


  public PrintMngr()
  {
    this.nextEntryNum = 1;
    this.entries      = new ArrayList<PrintData>();
    this.activeEntry  = null;
  }


  public void deactivatePrintData( PrintData data )
  {
    if( data != null ) {
      synchronized( this.entries ) {
	if( (this.activeEntry != null) && (this.activeEntry == data) ) {
	  this.activeEntry = null;
	  fireDataChanged( true );
	}
      }
    }
  }


  public PrintData getActivePrintData()
  {
    return this.activeEntry;
  }


  public PrintData getPrintData( int row )
  {
    PrintData data = null;
    synchronized( this.entries ) {
      if( (row >= 0) && (row < this.entries.size()) ) {
	data = this.entries.get( row );
      }
    }
    return data;
  }


  public void putByte( int b )
  {
    boolean lastRowOnly = true;
    synchronized( this.entries ) {
      if( this.activeEntry == null ) {
	this.activeEntry = new PrintData( this.nextEntryNum++ );
	this.entries.add( this.activeEntry );
	lastRowOnly = false;
      }
      this.activeEntry.putByte( b );
    }
    fireDataChanged( lastRowOnly );
  }


  public void removeRow( int row )
  {
    synchronized( this.entries ) {
      if( this.entries.remove( row ) == this.activeEntry ) {
	this.activeEntry = null;
      }
      fireTableRowsDeleted( row, row );
    }
  }


  public void reset()
  {
    synchronized( this.entries ) {
      boolean lastRowOnly = true;

      // aktuelle Druckdaten schliessen
      if( this.activeEntry != null ) {

	// Druckdaten werden entfernt, wenn sie leer sind.
	if( this.activeEntry.size() < 1 ) {
	  this.entries.remove( this.activeEntry );
	  lastRowOnly = false;
	}
      }
      this.activeEntry = null;
      fireDataChanged( lastRowOnly );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    return (col == 0) || (col == 1) ? Integer.class : Object.class;
  }


  @Override
  public int getColumnCount()
  {
    return colNames.length;
  }


  @Override
  public String getColumnName( int col )
  {
    return (col >= 0) && (col < colNames.length) ? colNames[ col ] : "";
  }


  @Override
  public int getRowCount()
  {
    int n = 0;
    synchronized( this.entries ) {
      n = this.entries.size();
    }
    return n;
  }


  @Override
  public Object getValueAt( int row, int col )
  {
    Object    rv   = null;
    PrintData data = getPrintData( row );
    if( data != null ) {
      switch( col ) {
	case 0:
	  rv = new Integer( data.getEntryNum() );
	  break;

	case 1:
	  rv = new Integer( data.size() );
	  break;

	case 2:
	  if( (this.activeEntry != null) && (data == this.activeEntry) ) {
	    rv = "aktiv";
	  } else {
	    rv = "abgeschlossen";
	  }
      }
    }
    return rv;
  }


  @Override
  public boolean isCellEditable( int row, int col )
  {
    return false;
  }


	/* --- private Methoden --- */

  private void fireDataChanged( boolean lastRowOnly )
  {
    if( lastRowOnly ) {
      final int row = this.entries.size() - 1;
      if( row >= 0 ) {
	fireTableRowsUpdated( row, row );
      }
    } else {
      fireTableDataChanged();
    }
  }
}

