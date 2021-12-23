/*
 * (c) 2009-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer KC85-Modulliste
 */

package jkcemu.emusys.kc85;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import jkcemu.text.TextUtil;


public class KC85ModuleTableModel extends AbstractTableModel
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
    updateDescriptions();
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


  public boolean isDiskStationSlot( int row )
  {
    boolean  rv      = false;
    String[] rowData = getRow( row );
    if( rowData != null ) {
      if( rowData.length > 1 ) {
	rv = isDiskStationSlot( rowData[ 0 ] );
      }
    }
    return rv;
  }


  public static boolean isDiskStationSlot( String slotText )
  {
    boolean rv = false;
    if( slotText != null ) {
      slotText = slotText.trim();
      if( (slotText.length() > 1) && slotText.startsWith( "F" ) ) {
	rv = true;
      }
    }
    return rv;
  }


  public void recalcSlots()
  {
    boolean diskStationSwitch = false;
    int     begRow     = -1;
    int     endRow     = -1;
    int     slot       = 8;
    int     nRows      = this.rows.size();
    for( int i = 0; i < nRows; i++ ) {
      String[] rowData = this.rows.get( i );
      if( rowData != null ) {
	if( rowData.length > 0 ) {
	  String oldSlot = rowData[ 0 ];
	  if( !diskStationSwitch
	      && (i >= (nRows - 2))
	      && isDiskStationSlot( oldSlot ) )
	  {
	    slot              = 0xF0;
	    diskStationSwitch = true;
	  }
	  rowData[ 0 ] = String.format( "%02X", slot );
	  if( !TextUtil.equals( rowData[ 0 ], oldSlot ) ) {
	    if( (begRow < 0) || (i < begRow) ) {
	      begRow = i;
	    }
	    if( (endRow < 0) || (i > endRow) ) {
	      endRow = i;
	    }
	  }
	}
      }
      slot += 4;
    }
    if( (begRow >= 0) && (endRow >= begRow) ) {
      fireTableRowsUpdated( begRow, endRow );
    }
  }


  public void removeRow( int row )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.remove( row );
      fireTableRowsDeleted( row, row );
      updateDescriptions();
    }
  }


  public void updateDescriptions()
  {
    boolean firstM052 = true;
    int     nRows     = getRowCount();
    for( int i = 0; i < nRows; i++ ) {
      Object o = getValueAt( i, 1 );
      if( o != null ) {
	String moduleName = o.toString();
	if( moduleName != null ) {
	  if( moduleName.equals( "M052" ) ) {
	    if( firstM052 ) {
	      firstM052 = false;
	      setValueAt( M052.DESCRIPTION, i, 2 );
	    } else {
	      setValueAt( M052.USB_ONLY_DESCRIPTION, i, 2 );
	    }
	  }
	}
      }
    }
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


  /*
   * Obwohl isCellEditable(...) immer false zurueckliefert,
   * muss die Methode setValueAt(...) implementiert werden,
   * da sie ausserhalb von JTable direkt aufgerufen wird.
   */
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
