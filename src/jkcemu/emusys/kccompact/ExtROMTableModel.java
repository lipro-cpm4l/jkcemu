/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer Liste der ROM-Erweiterungen
 */

package jkcemu.emusys.kccompact;

import java.util.Collections;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;


public class ExtROMTableModel extends AbstractTableModel
{
  private java.util.List<ExtROM> rows;


  public ExtROMTableModel()
  {
    this.rows = new ArrayList<>();
  }


  public void clear()
  {
    this.rows.clear();
    fireTableDataChanged();
  }


  public void addRow( ExtROM extROM )
  {
    int row = this.rows.size();
    this.rows.add( extROM );
    Collections.sort( this.rows );
    fireTableDataChanged();
  }


  public ExtROM getRow( int row )
  {
    return (row >= 0) && (row < this.rows.size()) ?
					this.rows.get( row )
					: null;
  }


  public int getRowNumByRomNum( int romNum )
  {
    int rv = -1;
    int n  = this.rows.size();
    for( int i = 0; i < n; i++ ) {
      if( this.rows.get( i ).getRomNum() == romNum ) {
	rv = i;
	break;
      }
    }
    return rv;
  }


  public void removeRow( int row )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.remove( row );
      fireTableRowsDeleted( row, row );
    }
  }


  public synchronized void setRow( int row, ExtROM extROM )
  {
    this.rows.set( row, extROM );
    Collections.sort( this.rows );
    fireTableDataChanged();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    return (col == 0) || (col == 1) ? String.class : Object.class;
  }


  @Override
  public int getColumnCount()
  {
    return 2;
  }


  @Override
  public String getColumnName( int col )
  {
    String rv = "";
    switch( col ) {
      case 0:
	rv = "ROM-Nr.";
	break;
      case 1:
	rv = "ROM-Datei";
	break;
    }
    return rv;
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
      ExtROM extROM = this.rows.get( row );
      if( extROM != null ) {
	switch( col ) {
	  case 0:
	    int romNum = extROM.getRomNum();
	    if( romNum == 7 ) {
	      rv = "7 (FDC-ROM)";
	    } else {
	      rv = String.valueOf( romNum );
	    }
	    break;
	  case 1:
	    rv = extROM.getFileName();
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
