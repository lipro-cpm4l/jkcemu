/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer die ROMs des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.lang.*;
import java.util.ArrayList;


public class CustomSysROMTableModel
			extends javax.swing.table.AbstractTableModel
{
  private static final String[] colNames = {
					"Adressbereich",
					"Optionen",
					"Datei" };

  private java.util.List<CustomSysROM> rows;


  public CustomSysROMTableModel()
  {
    this.rows = new ArrayList<>();
  }


  public void addRow( CustomSysROM rom )
  {
    int row = this.rows.size();
    this.rows.add( rom );
    fireTableRowsInserted( row, row );
  }


  public void clear()
  {
    if( !this.rows.isEmpty() ) {
      this.rows.clear();
      fireTableDataChanged();
    }
  }


  public CustomSysROM getRow( int row )
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


  public void setRow( int row, CustomSysROM rom )
  {
    if( (rom != null) && (row >= 0) && (row < this.rows.size()) ) {
      this.rows.set( row, rom );
      fireTableRowsUpdated( row, row );
    }
  }


  public void setRows( CustomSysROM[] roms )
  {
    this.rows.clear();
    if( roms != null ) {
      for( CustomSysROM rom : roms ) {
	this.rows.add( rom );
      }
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
      CustomSysROM rom = this.rows.get( row );
      if( rom != null ) {
	switch( col ) {
	  case 0:
	    rv = rom.getAddressText();
	    break;
	  case 1:
	    rv = rom.getOptionText();
	    break;
	  case 2:
	    rv = rom.getFileName();
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
