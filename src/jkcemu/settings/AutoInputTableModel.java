/*
 * (c) 2015-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenfeld fuer automatische Tastatureingabe
 */

package jkcemu.settings;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import javax.swing.table.AbstractTableModel;
import jkcemu.base.AutoInputEntry;
import jkcemu.base.AutoInputCharSet;


public class AutoInputTableModel extends AbstractTableModel
{
  private static NumberFormat waitTimeFmt = null;


  private static String[] colNames = {
				"Wartezeit",
				"Eingabetext",
				"Bermerkung" };


  private AutoInputCharSet               charSet;
  private java.util.List<AutoInputEntry> rows;


  public AutoInputTableModel( AutoInputCharSet charSet )
  {
    this.charSet = charSet;
    if( waitTimeFmt == null ) {
      waitTimeFmt = NumberFormat.getNumberInstance();
      if( waitTimeFmt instanceof DecimalFormat ) {
	((DecimalFormat) waitTimeFmt).applyPattern( "##0.0" );
      }
    }
    this.rows = new java.util.ArrayList<>();
  }


  public void addRow( AutoInputEntry entry )
  {
    int row = this.rows.size();
    this.rows.add( entry );
    fireTableRowsInserted( row, row );
  }


  public void addRows( Collection<AutoInputEntry> entries )
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


  public AutoInputEntry getRow( int row )
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


  public void setRow( int row, AutoInputEntry entry )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.set( row, entry );
      fireTableRowsUpdated( row, row );
    }
  }


	/* --- TableModel --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    Class<?> rv = Object.class;
    switch( col ) {
      case 0:
      case 1:
      case 2:
	rv = String.class;
	break;
    }
    return rv;
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
    if( (row >= 0) && (row < this.rows.size())
	&& (col >= 0) && (col < colNames.length) )
    {
      AutoInputEntry entry = this.rows.get( row );
      if( entry != null ) {
	switch( col ) {
	  case 0:
	    rv = waitTimeFmt.format(
			(double) entry.getMillisToWait() / 1000.0 ) + " s";
	    break;

	  case 1:
	    rv = this.charSet.toViewText( entry.getInputText() );
	    break;

	  case 2:
	    rv = entry.getRemark();
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
