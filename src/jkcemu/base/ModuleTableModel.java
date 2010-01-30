/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer Modulliste
 */

package jkcemu.base;

import java.lang.*;
import java.util.*;


public class ModuleTableModel extends javax.swing.table.AbstractTableModel
{
  private static final String[] colNames = {
					"Schacht",
					"Modul",
					"Beschreibung" };

  private java.util.List<String[]> rows;


  public ModuleTableModel()
  {
    this.rows = new ArrayList<String[]>();
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


  public void removeRow( int row )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.remove( row );
      fireTableRowsDeleted( row, row );
    }
  }


	/* --- ueberschriebene Methoden --- */

  public Class<?> getColumnClass( int col )
  {
    return (col >= 0) && (col < 3) ? String.class : Object.class;
  }


  public int getColumnCount()
  {
    return 3;
  }


  public String getColumnName( int col )
  {
    return (col >= 0) && (col < colNames.length) ?  colNames[ col ] : "";
  }


  public int getRowCount()
  {
    return this.rows.size();
  }


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


  public boolean isCellEditable( int row, int col )
  {
    return false;
  }


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

