/*
 * (c) 2008-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige von DateiInformationen
 */

package jkcemu.tools.filebrowser;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import jkcemu.base.EmuUtil;


public class FileInfoFld extends Component
{
  public enum Item {
		NAME,
		LINKED_TO,
		TYPE,
		FORMAT,
		DURATION,
		COMMENT,
		SIZE,
		LAST_MODIFIED };

  private static final int LABEL_VALUE_DISTANCE = 10;


  private int        minRows;
  private Integer    rowHeight;
  private String[]   labels;
  private String[]   values;
  private String[]   addonRows;
  private DateFormat dateFmt;


  FileInfoFld( int minRows )
  {
    this.minRows   = minRows;
    this.rowHeight = null;
    this.labels    = null;
    this.values    = null;
    this.addonRows = null;
    this.dateFmt   = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.MEDIUM );
  }


  public void setMinRows( int minRows )
  {
    this.minRows = minRows;
  }


  public void setValues(
		Map<Item,Object> items,
		String[]         addonRows )
  {
    Collection<String> labels = new ArrayList<>();
    Collection<String> values = new ArrayList<>();

    Object item = items.get( Item.NAME );
    if( item != null ) {
      String s = item.toString().trim();
      if( !s.isEmpty() ) {
	labels.add( "Name:" );
	values.add( item.toString() );
	item = items.get( Item.LINKED_TO );
	if( item != null ) {
	  labels.add( "Symbolischer Link auf:" );
	  values.add( item.toString() );
	}
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
		values.add( EmuUtil.getDecimalFormatMax1().format(
						dSeconds ) + " Sekunden" );
	      }
	    }
	  }
	}
	item = items.get( Item.COMMENT );
	if( item != null ) {
	  labels.add( "Kommentar:" );
	  values.add( item.toString() );
	}
	item = items.get( Item.SIZE );
	if( item != null ) {
	  if( item instanceof Number ) {
	    labels.add( "Gr\u00F6\u00DFe:" );
	    values.add( EmuUtil.formatSize(
				((Number) item).longValue(),
				false,
				true ) );
	  }
	}
	String lastModifiedText = getDateTimeText(
					items,
					Item.LAST_MODIFIED );
	if( lastModifiedText != null ) {
	  labels.add( "Zuletzt ge\u00E4ndert:" );
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

  @Override
  public Font getFont()
  {
    Font font = super.getFont();
    if( font == null ) {
      font = new Font( Font.SANS_SERIF, Font.PLAIN, 12 );
      setFont( font );
    }
    return font;
  }


  @Override
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


  @Override
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
      this.rowHeight = getFont().getSize() + 1;
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
