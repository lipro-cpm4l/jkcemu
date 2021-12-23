/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenfeld fuer Dateien
 */

package jkcemu.file;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.table.AbstractTableModel;


public class FileTableModel
			extends AbstractTableModel
			implements Comparator<FileEntry>
{
  public enum Column {
		NAME,
		INFO,
		SIZE,
		LAST_MODIFIED,
		FILE,
		USER_NUM,
		VALUE,
		READ_ONLY,
		SYSTEM_FILE,
		ARCHIVE };

  private static DateFormat dateFmt = null;

  private Column[]                  cols;
  private java.util.List<FileEntry> rows;
  private boolean                   sortCaseSensitive;
  private boolean                   sortDesc;
  private Column                    sortCol;


  public FileTableModel( Column... cols )
  {
    this.cols              = cols;
    this.rows              = new java.util.ArrayList<>();
    this.sortCol           = Column.NAME;
    this.sortCaseSensitive = false;
    this.sortDesc          = false;
  }


  public void addRow( FileEntry entry, boolean updView )
  {
    int row = this.rows.size();
    this.rows.add( entry );
    if( updView ) {
      fireTableRowsInserted( row, row );
    }
  }


  public void clear( boolean updView )
  {
    this.rows.clear();
    if( updView ) {
      fireTableDataChanged();
    }
  }


  public FileEntry getRow( int row )
  {
    return (row >= 0) && (row < this.rows.size()) ?
					this.rows.get( row )
					: null;
  }


  public void removeRow( int row, boolean updView )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.remove( row );
      if( updView ) {
	fireTableRowsDeleted( row, row );
      }
    }
  }


  public void setRow( int row, FileEntry entry )
  {
    if( (row >= 0) && (row < this.rows.size()) ) {
      this.rows.set( row, entry );
      fireTableRowsUpdated( row, row );
    }
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
      fireTableDataChanged();
    }
  }


  public void sortAscending( int col )
  {
    if( (col >= 0) && (col < this.cols.length) ) {
      this.sortCol  = this.cols[ col ];
      this.sortDesc = false;
      try {
	Collections.sort( this.rows, this );
      }
      catch( Exception ex ) {}
      fireTableDataChanged();
    }
  }


	/* --- Comparator --- */

  @Override
  public int compare( FileEntry f1, FileEntry f2 )
  {
    int rv = 0;
    if( (f1 != null) && (f2 != null) ) {
      switch( this.sortCol ) {
	case INFO:
	  rv = compareObject( f1.getInfo(), f2.getInfo() );
	  break;

	case SIZE:
	  rv = compareLong( f1.getSize(), f2.getSize() );
	  break;

	case LAST_MODIFIED:
	  rv = compareLong( f1.getLastModified(), f2.getLastModified() );
	  break;

	case FILE:
	  rv = compareFile( f1.getFile(), f2.getFile() );
	  break;

	case USER_NUM:
	  rv = compareObject( f1.getUserNum(), f2.getUserNum() );
	  break;

	case VALUE:
	  rv = compareObject( f1.getValue(), f2.getValue() );
	  break;
      }
      if( rv == 0 ) {
	rv = compareString( f1.getName(), f2.getName() );
      }
    }
    return this.sortDesc ? -rv : rv;
  }


	/* --- TableModel --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    Class<?> rv = Object.class;
    if( (col >= 0) && (col < this.cols.length) ) {
      switch( this.cols[ col ] ) {
	case NAME:
	case LAST_MODIFIED:
	  rv = String.class;
	  break;

	case FILE:
	  rv = File.class;
	  break;

	case SIZE:
	  rv = Long.class;
	  break;

	case USER_NUM:
	  rv = Integer.class;
	  break;

	case READ_ONLY:
	case SYSTEM_FILE:
	case ARCHIVE:
	  rv = Boolean.class;
	  break;
      }
    }
    return rv;
  }


  @Override
  public int getColumnCount()
  {
    return this.cols.length;
  }


  @Override
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

	case SIZE:
	  rv = "Gr\u00F6\u00DFe";
	  break;

	case LAST_MODIFIED:
	  rv = "Zuletzt ge\u00E4ndert";
	  break;

	case FILE:
	  rv = "Datei";
	  break;

	case USER_NUM:
	  rv = "User";
	  break;

	case VALUE:
	  rv = "Wert";
	  break;

	case READ_ONLY:
	  rv = "Schreibgesch\u00FCtzt";
	  break;

	case SYSTEM_FILE:
	  rv = "System-Datei";
	  break;

	case ARCHIVE:
	  rv = "Archiv";
	  break;
      }
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
    if( (row >= 0) && (row < this.rows.size())
	&& (col >= 0) && (col < this.cols.length) )
    {
      FileEntry entry = this.rows.get( row );
      if( entry != null ) {
	switch( this.cols[ col ] ) {
	  case NAME:
	    rv = entry.getName();
	    break;

	  case INFO:
	    rv = entry.getInfo();
	    break;

	  case SIZE:
	    rv = entry.getSize();
	    break;

	  case LAST_MODIFIED:
	    {
	      Long lastModified = entry.getLastModified();
	      if( lastModified != null ) {
		if( dateFmt == null ) {
		  dateFmt = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.MEDIUM );
		}
		rv = dateFmt.format( new java.util.Date( lastModified ) );
	      }
	    }
	    break;

	  case FILE:
	    rv = entry.getFile();
	    break;

	  case USER_NUM:
	    rv = entry.getUserNum();
	    break;

	  case VALUE:
	    rv = entry.getValue();
	    break;

	  case READ_ONLY:
	    rv = toBoolean( entry.isReadOnly() );
	    break;

	  case SYSTEM_FILE:
	    rv = toBoolean( entry.isSystemFile() );
	    break;

	  case ARCHIVE:
	    rv = toBoolean( entry.isArchive() );
	    break;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean isCellEditable( int row, int col )
  {
    boolean rv = false;
    if( (col >= 0) && (col < this.cols.length) ) {
      switch( this.cols[ col ] ) {
	case READ_ONLY:
	case SYSTEM_FILE:
	case ARCHIVE:
	  rv = true;
	  break;
      }
    }
    return rv;
  }


  @Override
  public void setValueAt( Object value, int row, int col )
  {
    if( (row >= 0) && (row < this.rows.size())
	&& (col >= 0) && (col < this.cols.length) )
    {
      FileEntry entry = this.rows.get( row );
      if( entry != null ) {
	boolean done = true;
	switch( this.cols[ col ] ) {
	  case READ_ONLY:
	    entry.setReadOnly( parseBoolean( value ) );
	    done = true;
	    break;

	  case SYSTEM_FILE:
	    entry.setSystemFile( parseBoolean( value ) );
	    done = true;
	    break;

	  case ARCHIVE:
	    entry.setArchive( parseBoolean( value ) );
	    done = true;
	    break;
	}
	if( done ) {
	  fireTableRowsUpdated( row, row );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private int compareObject( Object o1, Object o2 )
  {
    int rv = 0;
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
	  rv = compareString( o1, o2 );
	}
      }
    } else {
      rv = compareString( o1, o2 );
    }
    return rv;
  }


  private int compareFile( File f1, File f2 )
  {
    int rv = 0;
    if( (f1 != null) && (f2 != null) ) {
      rv = compareString( f1.getName(), f2.getName() );
    }
    else if( (f1 != null) && (f2 == null) ) {
      rv = 1;
    }
    else if( (f1 == null) && (f2 != null) ) {
      rv = -1;
    }
    return rv;
  }


  private int compareLong( Long v1, Long v2 )
  {
    int rv = 0;
    if( (v1 != null) && (v2 != null) ) {
      rv = v1.compareTo( v2 );
    }
    else if( (v1 != null) && (v2 == null) ) {
      rv = -1;
    }
    else if( (v1 == null) && (v2 != null) ) {
      rv = 1;
    }
    return rv;
  }


  private int compareString( Object o1, Object o2 )
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


  private static boolean parseBoolean( Object value )
  {
    boolean rv = false;
    if( value != null ) {
      if( value instanceof Boolean ) {
	rv = ((Boolean) value).booleanValue();
      } else {
	String s = value.toString();
	if( s != null ) {
	  rv = Boolean.parseBoolean( s );
	}
      }
    }
    return rv;
  }


  private static Boolean toBoolean( boolean state )
  {
    return state ? Boolean.TRUE : Boolean.FALSE;
  }
}
