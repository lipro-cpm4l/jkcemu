/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige von DateiInformationen
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.lang.*;
import java.text.*;
import java.util.*;


public class FileInfoFld extends Component
{
  public enum Item {
		NAME,
		TYPE,
		FORMAT,
		DURATION,
		AUTHOR,
		TITLE,
		DATE,
		COMMENT,
		SIZE,
		LAST_MODIFIED };

  private static final int LABEL_VALUE_DISTANCE = 10;


  private int          minRows;
  private Integer      rowHeight;
  private String[]     labels;
  private String[]     values;
  private String[]     addonRows;
  private DateFormat   dateFmt;
  private NumberFormat decFmt;
  private NumberFormat intFmt;


  FileInfoFld( int minRows )
  {
    this.minRows    = minRows;
    this.rowHeight  = null;
    this.labels     = null;
    this.values     = null;
    this.addonRows  = null;
    this.dateFmt    = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.MEDIUM );
    NumberFormat numFmt = NumberFormat.getNumberInstance();
    if( numFmt instanceof DecimalFormat ) {
      this.decFmt = (DecimalFormat) numFmt;
    } else {
      this.decFmt = new DecimalFormat();
    }
    this.decFmt.setMaximumFractionDigits( 1 );
    this.intFmt = NumberFormat.getIntegerInstance();
    this.intFmt.setGroupingUsed( true );
  }


  public void setMinRows( int minRows )
  {
    this.minRows = minRows;
  }


  public void setValues(
		Map<Item,Object> items,
		String[]         addonRows )
  {
    Collection<String> labels = new ArrayList<String>();
    Collection<String> values = new ArrayList<String>();

    Object item = items.get( Item.NAME );
    if( item != null ) {
      String s = item.toString().trim();
      if( s.length() > 0 ) {
	labels.add( "Name:" );
	values.add( item.toString() );
	item = items.get( Item.TYPE );
	if( item != null ) {
	  labels.add( "Typ:" );
	  values.add( item.toString() );
	}
	item = items.get( Item.FORMAT );
	if( item != null ) {
	  labels.add( "Format:" );
	  values.add( item.toString() );
	}
	item = items.get( Item.DURATION );
	if( item != null ) {
	  if( item instanceof Number ) {
	    double dSeconds = ((Number) item).doubleValue();
	    if( dSeconds > 0.0 ) {
	      labels.add( "L\u00E4nge:" );
	      int seconds  = (int) Math.round( dSeconds );
	      if( seconds >= 60 ) {
		int hours = seconds / 3600;
		if( hours > 0 ) {
		  values.add( String.format(
				"%02d:%02d:%02d Stunden",
				hours,
				(seconds / 60) % 60,
				seconds % 60 ) );
		} else {
		  values.add( String.format(
				"%02d:%02d Minuten",
				seconds / 60,
				seconds % 60 ) );
		}
	      } else {
		values.add( this.decFmt.format( dSeconds ) + " Sekunden" );
	      }
	    }
	  }
	}
	item = items.get( Item.AUTHOR );
	if( item != null ) {
	  labels.add( "Author:" );
	  values.add( item.toString() );
	}
	item = items.get( Item.TITLE );
	if( item != null ) {
	  labels.add( "Titel:" );
	  values.add( item.toString() );
	}
	String dateText = getDateTimeText( items, Item.DATE );
	if( dateText != null ) {
	  labels.add( "Datum:" );
	  values.add( dateText );
	}
	item = items.get( Item.COMMENT );
	if( item != null ) {
	  labels.add( "Kommentar:" );
	  values.add( item.toString() );
	}
	item = items.get( Item.SIZE );
	if( item != null ) {
	  if( item instanceof Number ) {
	    long fSize = ((Number) item).longValue();
	    StringBuilder buf = new StringBuilder( 64 );
	    final long    kb  = 1024L;
	    final long    mb  = kb * 1024L;
	    final long    gb  = mb * 1024L;
	    if( fSize >= gb ) {
	      buf.append( this.decFmt.format( (double) fSize / (double) gb ) );
	      buf.append( " GByte" );
	    }
	    else if( fSize >= mb ) {
	      buf.append( this.decFmt.format( (double) fSize / (double) mb ) );
	      buf.append( " MByte" );
	    }
	    else if( fSize >= kb ) {
	      buf.append( this.decFmt.format( (double) fSize / (double) kb ) );
	      buf.append( " kByte" );
	    }
	    boolean enclose = (buf.length() > 0);
	    if( enclose ) {
	      buf.append( " (" );
	    }
	    buf.append( this.intFmt.format( fSize ) );
	    buf.append( " Bytes" );
	    if( enclose ) {
	      buf.append( (char) ')' );
	    }
	    labels.add( "Gr\u00F6\u00DFe:" );
	    values.add( buf.toString() );
	  }
	}
	String lastModifiedText = getDateTimeText( items, Item.LAST_MODIFIED );
	if( lastModifiedText != null ) {
	  labels.add( "Ge\u00E4ndert:" );
	  values.add( lastModifiedText );
	}
      }
    }
    this.labels    = labels.toArray( new String[ labels.size() ] );
    this.values    = values.toArray( new String[ values.size() ] );
    this.addonRows = addonRows;
    invalidate();
    repaint();
  }


	/* --- ueberschriebene Methoden --- */

  public Font getFont()
  {
    Font font = super.getFont();
    if( font == null ) {
      font = new Font( "SansSerif", Font.PLAIN, 12 );
      setFont( font );
    }
    return font;
  }


  public Dimension getPreferredSize()
  {
    int w     = 1;
    int nRows = Math.max(
			this.labels != null ? this.labels.length : 0,
			this.values != null ? this.values.length : 0 );
    if( nRows > 0 ) {
      nRows++;
    }
    if( this.addonRows != null ) {
      if( addonRows.length > 0 )
	nRows += (this.addonRows.length + 1);
    }
    if( nRows > 0) {
      w = getVerticalListWidth( this.labels )
			      + LABEL_VALUE_DISTANCE
			      + getVerticalListWidth( this.values );
    }
    if( (this.minRows > 0) && (nRows < this.minRows) ) {
      nRows = this.minRows;
    }
    return new Dimension( w, nRows * getRowHeight() );
  }


  public void paint( Graphics g )
  {
    g.setFont( getFont() );
    int y  = drawVerticalList( g, this.labels, 0, 0 );
    int x  = getVerticalListWidth( this.labels ) + LABEL_VALUE_DISTANCE;
    int y2 = drawVerticalList( g, this.values, x, 0 );
    if( y2 > y ) {
      y = y2;
    }
    drawVerticalList( g, this.addonRows, 0, y + getRowHeight() );
  }


	/* --- private Methoden --- */

  private int drawVerticalList( Graphics g, String[] list, int x, int y )
  {
    if( list != null ) {
      for( int i = 0; i < list.length; i++ ) {
	y += getRowHeight();
	String s = list[ i ];
	if( s != null ) {
	  g.drawString( s, x, y );
	}
      }
    }
    return y;
  }


  private String getDateTimeText(
			Map<Item,Object> items,
			Item             keyObj )
  {
    String rv   = null;
    Object item = items.get( keyObj );
    if( item != null ) {
      if( item instanceof Number ) {
	rv = this.dateFmt.format(
			new java.util.Date( ((Number) item).longValue() ) );
      } else if( item instanceof java.util.Date ) {
	rv = this.dateFmt.format( (java.util.Date) item );
      } else {
	rv = item.toString();
      }
    }
    return rv;
  }


  private int getRowHeight()
  {
    if( this.rowHeight == null ) {
      this.rowHeight = new Integer( getFont().getSize() + 1 );
    }
    return this.rowHeight.intValue();
  }


  private int getVerticalListWidth( String[] list )
  {
    int wMax = 0;
    if( list != null ) {
      FontMetrics fm = getFontMetrics( getFont() );
      if( fm != null ) {
	for( int i = 0; i < list.length; i++ ) {
	  String s = list[ i ];
	  if( s != null ) {
	    int w = fm.stringWidth( s );
	    if( w > wMax ) {
	      wMax = w;
	    }
	  }
	}
      }
    }
    return wMax;
  }


  public static boolean hasText( String text )
  {
    return text != null ? (text.length() > 0) : false;
  }
}

