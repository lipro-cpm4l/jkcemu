/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer Modulliste
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.ArrayList;


public class KC85ModuleTableModel extends javax.swing.table.AbstractTableModel
{
  private static final String[] colNames = {
					"Schacht",
					"Modul",
					"Beschreibung" };

  private java.util.List<String[]> rows;


  public KC85ModuleTableModel()
  {
    this.rows = new ArrayList<>();
  }


  public void addRow( String... rowData )
  {
    int row = this.rows.size();
    this.rows.add( rowData );
    fireTableRowsInserted( row, row );
  }


  public void clear()
  {
    if( !this.rows.isEmpty() ) {
      this.rows.clear();
      fireTableDataChanged();
    }
  }


  public String[] getRow( int row )
  {
    return (row >= 0) && (row < this.rows.size()) ?
					this.rows.get( row )
					: null;
  }


  public void removeRow( int row )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.remove( row );
      fireTableRowsDeleted( row, row );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    return (col >= 0) && (col < 3) ? String.class : Object.class;
  }


  @Override
  public int getColumnCount()
  {
    return 3;
  }


  @Override
  public String getColumnName( int col )
  {
    return (col >= 0) && (col < colNames.length) ?  colNames[ col ] : "";
  }


  @Override
  public int getRowCount()
  {
    return this.rows.size();
  }


  @Override
  public Object getValueAt( int row, int col )
  {
    Object rv = null;
    if( (row >= 0) && (row < this.rows.size()) ) {
      String[] rowData = this.rows.get( row );
      if( rowData != null ) {
	if( (col >= 0) && (col < rowData.length) ) {
	  rv = rowData[ col ];
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


  @Override
  public void setValueAt( Object value, int row, int col )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      String[] rowData = this.rows.get( row );
      if( rowData != null ) {
	if( (col >= 0) && (col < rowData.length) ) {
	  if( value != null ) {
	    rowData[ col ] = value.toString();
	  } else {
	    rowData[ col ] = null;
	  }
	  fireTableRowsUpdated( row, row );
	}
      }
    }
  }
}
