/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenfeld fuer automatisch zu ladende Dateien
 */

package jkcemu.base;

import java.lang.*;
import java.text.*;
import java.util.*;
import javax.swing.table.*;


public class AutoLoadTableModel extends AbstractTableModel
{
  private static NumberFormat waitTimeFmt = null;


  private static String[] colNames = {
				"Wartezeit",
				"Ladeadresse",
				"Dateiname" };


  private java.util.List<AutoLoadEntry> rows;


  public AutoLoadTableModel()
  {
    if( waitTimeFmt == null ) {
      waitTimeFmt = NumberFormat.getNumberInstance();
      if( waitTimeFmt instanceof DecimalFormat ) {
	((DecimalFormat) waitTimeFmt).applyPattern( "##0.0" );
      }
    }
    this.rows = new java.util.ArrayList<>();
  }


  public void addRow( AutoLoadEntry entry )
  {
    int row = this.rows.size();
    this.rows.add( entry );
    fireTableRowsInserted( row, row );
  }


  public void addRows( Collection<AutoLoadEntry> entries )
  {
    if( entries != null ) {
      if( !entries.isEmpty() ) {
	int row = this.rows.size();
	this.rows.addAll( entries );
	fireTableRowsInserted( row, this.rows.size() - 1 );
      }
    }
  }


  public void clear()
  {
    this.rows.clear();
    fireTableDataChanged();
  }


  public AutoLoadEntry getRow( int row )
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


  public void setRow( int row, AutoLoadEntry entry )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.set( row, entry );
      fireTableRowsUpdated( row, row );
    }
  }


  public static String toHex4( Integer value )
  {
    String rv = null;
    if( value != null ) {
      if( (value.intValue() >= 0) && (value.intValue() <= 0xFFFF) ) {
	rv = String.format( "%04X", value & 0xFFFF );
      }
    }
    return rv;
  }


	/* --- TableModel --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    return (col >= 0) && (col < colNames.length) ?
					String.class : Object.class;
  }


  @Override
  public int getColumnCount()
  {
    return this.colNames.length;
  }


  @Override
  public String getColumnName( int col )
  {
    return (col >= 0) && (col < this.colNames.length) ? colNames[ col ] : "";
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
    if( (row >= 0) && (row < this.rows.size())
	&& (col >= 0) && (col < this.colNames.length) )
    {
      AutoLoadEntry entry = this.rows.get( row );
      if( entry != null ) {
	switch( col ) {
	  case 0:
	    rv = waitTimeFmt.format(
			(double) entry.getMillisToWait() / 1000.0 ) + " s";
	    break;

	  case 1:
	    rv = toHex4( entry.getLoadAddr() );
	    break;

	  case 2:
	    rv = entry.getFileName();
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
