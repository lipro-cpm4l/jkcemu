/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer Farbpalette
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.util.Arrays;


public class ColorPaletteTableModel
			extends javax.swing.table.AbstractTableModel
{
  private static final String[] colNames = {
					"Nr.",
					"ARGB-Wert (hex)",
					"Farbe",
					"Neuer ARGB-Wert (hex)",
					"Neue Farbe" };

  private IndexColorModel icm;
  private Integer[]       changedARGBs;


  public ColorPaletteTableModel( IndexColorModel icm, boolean editable )
  {
    this.icm      = icm;
    if( editable ) {
      this.changedARGBs = new Integer[ icm.getMapSize() ];
      Arrays.fill( this.changedARGBs, null );
    } else {
      this.changedARGBs = null;
    }
  }


  public void clearChangedARGBs()
  {
    if( this.changedARGBs != null ) {
      Arrays.fill( this.changedARGBs, null );
      fireTableDataChanged();
    }
  }


  public IndexColorModel createIndexColorModel()
  {
    boolean t = false;
    int     n = this.icm.getMapSize();
    byte[]  a = new byte[ n ];
    byte[]  r = new byte[ n ];
    byte[]  g = new byte[ n ];
    byte[]  b = new byte[ n ];
    for( int i = 0; i < n; i++ ) {
      int argb = this.icm.getRGB( i );
      if( this.changedARGBs != null ) {
	if( i < this.changedARGBs.length ) {
	  if( this.changedARGBs[ i ] != null ) {
	    argb = this.changedARGBs[ i ].intValue();
	  }
	}
      }
      int alpha = (argb >> 24) & 0xFF;
      if( alpha < 0xFF ) {
	t = true;
      }
      a[ i ] = (byte) alpha;
      r[ i ] = (byte) (argb >> 16);
      g[ i ] = (byte) (argb >> 8);
      b[ i ] = (byte) argb;
    }
    return ImageUtil.createIndexColorModel( n, r, g, b, t ? a : null );
  }


  public Integer getChangedARGB( int row )
  {
    Integer rv = null;
    if( this.changedARGBs != null ) {
      if( (row >= 0) && (row < this.changedARGBs.length) ) {
	rv = this.changedARGBs[ row ];
      }
    }
    return rv;
  }


  public Integer getOrgARGB( int row )
  {
    return (row >= 0) && (row < this.icm.getMapSize()) ?
			Integer.valueOf( this.icm.getRGB( row ) )
			: null;
  }


  public String getTextAt( int row, int col )
  {
    String rv = null;
    if( (col == 0) || (col == 1) || (col == 3) ) {
      Object o = getValueAt( row, col );
      if( o != null ) {
	rv = o.toString();
      }
    }
    return rv;
  }


  public boolean hasChangedARGBs()
  {
    boolean rv = false;
    if( this.changedARGBs != null ) {
      for( Integer argb : this.changedARGBs ) {
	if( argb != null ) {
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


  public void setChangedARGB( int row, Integer argb )
  {
    if( this.changedARGBs != null ) {
      if( (row >= 0) && (row < this.changedARGBs.length) ) {
	this.changedARGBs[ row ] = argb;
	fireTableRowsUpdated( row, row );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Class<?> getColumnClass( int col )
  {
    Class<?> rv = Object.class;
    switch( col ) {
      case 0:
	rv = Integer.class;
	break;
      case 1:
      case 3:
	rv = String.class;
	break;
      case 2:
      case 4:
	rv = Color.class;
	break;
    }
    return rv;
  }


  @Override
  public int getColumnCount()
  {
    return this.changedARGBs != null ?
			colNames.length
			: (colNames.length - 2);
  }


  @Override
  public String getColumnName( int col )
  {
    return (col >= 0) && (col < colNames.length) ?  colNames[ col ] : "";
  }


  @Override
  public int getRowCount()
  {
    return this.icm.getMapSize();
  }


  @Override
  public Object getValueAt( int row, int col )
  {
    Object rv = null;
    if( (row >= 0) && (row < this.icm.getMapSize()) ) {
      switch( col ) {
	case 0:
	  rv = Integer.valueOf( row + 1 );
	  break;
	case 1:
	  rv = String.format( "%08X", getARGB( row ) );
	  break;
	case 2:
	  rv = new Color( getARGB( row ) );
	  break;
	case 3:
	  if( this.changedARGBs != null ) {
	    if( row < this.changedARGBs.length ) {
	      if( this.changedARGBs[ row ] != null ) {
		rv = String.format( "%08X", this.changedARGBs[ row ] );
	      }
	    }
	  }
	  break;
	case 4:
	  if( this.changedARGBs != null ) {
	    if( row < this.changedARGBs.length ) {
	      if( this.changedARGBs[ row ] != null ) {
		rv = new Color( this.changedARGBs[ row ].intValue() );
	      }
	    }
	  }
	  break;
      }
    }
    return rv;
  }


  @Override
  public boolean isCellEditable( int row, int col )
  {
    return (col == 3);
  }


  @Override
  public void setValueAt( Object value, int row, int col )
  {
    if( (this.changedARGBs != null) && (col == 3) ) {
      if( (row >= 0) && (row < this.changedARGBs.length) ) {
	boolean err  = false;
	Integer argb = null;
	if( value != null ) {
	  String  text = value.toString();
	  if( text != null ) {
	    text    = text.trim();
	    int len = text.length();
	    if( len > 0 ) {
	      try {
		long v = Long.parseLong( text, 16 );
		if( ((v & 0xFF000000L) == 0) && (len < 7) ) {
		  v |= 0xFF000000L;
		}
		argb = Integer.valueOf( (int) v );
	      }
	      catch( NumberFormatException ex ) {
		err = true;
	      }
	    }
	  }
	}
	if( !err ) {
	  this.changedARGBs[ row ] = argb;
	  fireTableRowsUpdated( row, row );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private int getARGB( int row )
  {
    return row < this.icm.getMapSize() ? this.icm.getRGB( row ) : 0;
  }
}
