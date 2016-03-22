/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellennmodell fuer Variablen
 */

package jkcemu.tools.debugger;

import java.lang.*;
import java.util.*;
import javax.swing.table.AbstractTableModel;
import z80emu.Z80MemView;


public class VarTableModel extends AbstractTableModel
{
  private static final String[] colNames = {
					"Bezeichnung",
					"Adresse",
					"Typ",
					"Bytes (Hex)",
					"Wert" };

  private java.util.List<VarData> rows;
  private boolean                 valuesEnabled;


  public VarTableModel()
  {
    this.rows          = new ArrayList<>();
    this.valuesEnabled = true;
  }


  public int addRow( VarData varData )
  {
    int row = -1;
    if( varData != null ) {
      row = this.rows.size();
      this.rows.add( varData );
      fireTableRowsInserted( row, row );
    }
    return row;
  }


  public void clear()
  {
    this.rows.clear();
    fireTableDataChanged();
  }


  public VarData getRow( int row )
  {
    return (row >= 0) && (row < this.rows.size()) ?
					this.rows.get( row )
					: null;
  }


  public boolean getValuesEnabled()
  {
    return this.valuesEnabled;
  }


  public int indexOf( VarData varData )
  {
    return varData != null ? this.rows.indexOf( varData ) : -1;
  }


  public boolean isEmpty()
  {
    return this.rows.isEmpty();
  }


  public void remove( int row )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.remove( row );
      fireTableRowsDeleted( row, row );
    }
  }


  public void setRow( int row, VarData varData )
  {
    if( (row >= 0) && (row < this.rows.size()) && (varData != null) ) {
      this.rows.set( row, varData );
      fireTableRowsUpdated( row, row );
    }
  }


  public void setValuesEnabled( boolean state )
  {
    if( state != this.valuesEnabled ) {
      this.valuesEnabled = state;
      fireTableDataChanged();
    }
  }


  public void update( Z80MemView memory )
  {
    for( VarData data : this.rows ) {
      data.update( memory );
    }
    fireTableDataChanged();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    return (col >= 0) && (col < colNames.length) ?
					String.class
					: Object.class;
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
    return this.rows.size();
  }


  @Override
  public Object getValueAt( int row, int col )
  {
    Object rv = null;
    if( (row >= 0) && (row < this.rows.size()) ) {
      VarData varData = this.rows.get( row );
      if( varData != null ) {
	switch( col ) {
	  case 0:
	    rv = varData.getName();
	    break;

	  case 1:
	    rv = varData.getAddrText();
	    break;

	  case 2:
	    rv = varData.getTypeText();
	    break;

	  case 3:
	    if( this.valuesEnabled ) {
	      rv = varData.getByteText();
	    }
	    break;

	  case 4:
	    if( this.valuesEnabled ) {
	      rv = varData.getValueText();
	    }
	    break;
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
}
