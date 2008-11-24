/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenfeld fuer Dateien
 */

package jkcemu.filebrowser;

import java.lang.*;
import java.util.*;
import javax.swing.table.*;


public class FileTableModel
			extends AbstractTableModel
			implements Comparator<FileEntry>
{
  public enum Column { NAME, INFO, LAST_MODIFIED, VALUE }; 

  private Column[]                  cols;
  private java.util.List<FileEntry> rows;
  private boolean                   sortCaseSensitive;
  private boolean                   sortDesc;
  private Column                    sortCol;


  public FileTableModel( Column... cols )
  {
    this.cols              = cols;
    this.rows              = new java.util.ArrayList<FileEntry>();
    this.sortCol           = Column.NAME;
    this.sortCaseSensitive = false;
    this.sortDesc          = false;
  }


  public void addRow( FileEntry row )
  {
    this.rows.add( row );
  }


  public void clear()
  {
    this.rows.clear();
  }


  public FileEntry getRow( int row )
  {
    return (row >= 0) && (row < this.rows.size()) ?
					this.rows.get( row )
					: null;
  }


  public void setSortCaseSensitive( boolean state )
  {
    this.sortCaseSensitive = state;
  }


  public void sort( int col )
  {
    if( (col >= 0) && (col < this.cols.length) ) {
      if( this.cols[ col ] == this.sortCol ) {
	this.sortDesc = !this.sortDesc;
      } else {
	this.sortCol  = this.cols[ col ];
	this.sortDesc = false;
      }
      try {
	Collections.sort( this.rows, this );
      }
      catch( Exception ex ) {}
    }
  }


	/* --- Comparator --- */

  public int compare( FileEntry f1, FileEntry f2 )
  {
    int rv = -1;
    if( (f1 != null) && (f2 != null) ) {
      switch( this.sortCol ) {
	case NAME:
	  rv = compareStrings( f1.getName(), f2.getName() );
	  break;

	case INFO:
	  Object o1 = f1.getInfo();
	  Object o2 = f2.getInfo();
	  if( (o1 != null) && (o2 != null) ) {
	    if( (o1 instanceof Number) && (o2 instanceof Number) ) {
	      long v1 = ((Number) o1).longValue();
	      long v2 = ((Number) o2).longValue();
	      if( v1 < v2 ) {
		rv = -1;
	      } else if( v1 > v2 ) {
		rv = 1;
	      } else {
		rv = 0;
	      }
	    } else {
	      if( o1 instanceof Number ) {
		rv = 1;
	      } else if( o2 instanceof Number ) {
		rv = -1;
	      } else {
		rv = compareStrings( o1, o2 );
	      }
	    }
	  } else {
	    if( (o1 == null) && (o2 == null) ) {
	      rv = compareStrings( f1.getName(), f2.getName() );
	    } else {
	      rv = (o1 == null ? -1 : 1);
	    }
	  }
	  break;

	case LAST_MODIFIED:
	  java.util.Date d1 = f1.getTime();
	  java.util.Date d2 = f2.getTime();
	  if( (d1 != null) && (d2 != null) ) {
	    rv = d1.compareTo( d2 );
	  } else {
	    rv = (d1 == null ? -1 : 1);
	  }
	  break;
      }
    }
    return this.sortDesc ? -rv : rv;
  }


	/* --- TableModel --- */

  public Class<?> getColumnClass( int col )
  {
    Class<?> rv = Object.class;
    if( (col >= 0) && (col < this.cols.length) ) {
      switch( this.cols[ col ] ) {
	case NAME:
	  rv = String.class;
	  break;

	case INFO:
	  rv = Object.class;
	  break;

	case LAST_MODIFIED:
	  rv = java.util.Date.class;
	  break;
      }
    }
    return rv;
  }


  public int getColumnCount()
  {
    return this.cols.length;
  }


  public String getColumnName( int col )
  {
    String rv = "";
    if( (col >= 0) && (col < this.cols.length) ) {
      switch( this.cols[ col ] ) {
	case NAME:
	  rv = "Name";
	  break;

	case INFO:
	  rv = "Typ/Gr\u00F6\u00DFe";
	  break;

	case LAST_MODIFIED:
	  rv = "Ge\u00E4ndert";
	  break;

	case VALUE:
	  rv = "Wert";
	  break;
      }
    }
    return rv;
  }


  public int getRowCount()
  {
    return this.rows.size();
  }


  public Object getValueAt( int row, int col )
  {
    Object rv = null;
    if( (row >= 0) && (row < this.rows.size())
	&& (col >= 0) && (col < this.cols.length) )
    {
      FileEntry entry = this.rows.get( row );
      switch( this.cols[ col ] ) {
	case NAME:
	  rv = entry.getName();
	  break;

	case INFO:
	  rv = entry.getInfo();
	  break;

	case LAST_MODIFIED:
	  rv = entry.getTime();
	  break;

	case VALUE:
	  rv = entry.getValue();
	  break;
      }
    }
    return rv;
  }


  public boolean isCellEditable( int row, int col )
  {
    return false;
  }


	/* --- private Methoden --- */

  private int compareStrings( Object o1, Object o2 )
  {
    String s1 = null;
    String s2 = null;
    if( o1 != null ) {
      s1 = o1.toString();
    }
    if( o2 != null ) {
      s2 = o2.toString();
    }
    if( s1 == null ) {
      s1 = "";
    }
    if( s2 == null ) {
      s2 = "";
    }
    return this.sortCaseSensitive ?
			s1.compareTo( s2 )
			: s1.compareToIgnoreCase( s2 );
  }
}

