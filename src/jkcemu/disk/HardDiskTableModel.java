/*
 * (c) 2010-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer Festplatten
 */

package jkcemu.disk;

import java.awt.Window;
import java.lang.*;
import java.util.*;
import javax.swing.table.AbstractTableModel;
import jkcemu.base.*;


public class HardDiskTableModel extends AbstractTableModel
{
  private static final String[] colNames = {
					"Hersteller",
					"Modell",
					"Zylinder",
					"K\u00F6pfe",
					"Sektoren pro Spur",
					"Gr\u00F6\u00DFe" };

  private HardDiskListDlg              owner;
  private java.util.List<HardDiskInfo> rows;


  public HardDiskTableModel( HardDiskListDlg owner )
  {
    this.owner = owner;
    this.rows  = new ArrayList<>();
  }


  public void addRow( HardDiskInfo info )
  {
    this.rows.add( info );
  }


  public HardDiskInfo getRow( int row )
  {
    return (row >= 0) && (row < this.rows.size()) ?
					this.rows.get( row ) : null;
  }


  public int indexOf( HardDiskInfo info )
  {
    return this.rows.indexOf( info );
  }


  public void removeRow( int row )
  {
    if( (row >= 0) && (row < this.rows.size()) )
      this.rows.remove( row );
  }


  public void sort()
  {
    try {
      Collections.sort( this.rows );
    }
    catch( ClassCastException ex ) {}
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
    Object       rv   = null;
    HardDiskInfo data = getRow( row );
    if( data != null ) {
      switch( col ) {
	case 0:
	  rv = data.getProducer();
	  break;
	case 1:
	  rv = data.getDiskModel();
	  break;
	case 2:
	  rv = data.getCylinders();
	  break;
	case 3:
	  rv = data.getHeads();
	  break;
	case 4:
	  rv = data.getSectorsPerTrack();
	  break;
	case 5:
	  rv = EmuUtil.formatSize(
				(long) data.getCylinders()
					* (long) data.getHeads()
					* (long) data.getSectorsPerTrack()
					* 512L,
				true,
				false );
	  break;
      }
    }
    return rv;
  }


  @Override
  public boolean isCellEditable( int row, int col )
  {
    return (col >= 2) && (col <= 4);
  }


  @Override
  public void setValueAt( Object value, int row, int col )
  {
    if( value != null ) {
      HardDiskInfo data = getRow( row );
      if( data != null ) {
	if( (col >= 2) && (col <= 4) ) {
	  String s = value.toString();
	  if( s != null ) {
	    s = s.trim();
	    if( !s.isEmpty() ) {
	      String msg = null;
	      int    v   = 0;
	      try {
		v = Integer.parseInt( s );
	      }
	      catch( NumberFormatException ex ) {
		msg = s + ": Ung\u00FCltige Eingabe (Zahl erwartet)";
	      }
	      if( msg == null ) {
		try {
		  switch( col ) {
		    case 2:
		      data.setCylinders( v );
		      this.owner.setDataChanged();
		      break;
		    case 3:
		      data.setHeads( v );
		      this.owner.setDataChanged();
		      break;
		    case 4:
		      data.setSectorsPerTrack( v );
		      this.owner.setDataChanged();
		      break;
		  }
		  fireTableRowsUpdated( row, row );
		}
		catch( IllegalArgumentException ex ) {
		  msg = ex.getMessage();
		}
	      }
	      if( msg != null ) {
		BasicDlg.showErrorDlg( this.owner, msg );
	      }
	    }
	  }
	}
      }
    }
  }
}

