/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer Tabellenzellen
 */

package jkcemu.filebrowser;

import java.awt.Component;
import java.lang.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;


public class FileTableCellRenderer extends DefaultTableCellRenderer
{
  private DateFormat dateFmt;
  private NumberFormat numFmt;


  public FileTableCellRenderer()
  {
    this.dateFmt = DateFormat.getDateTimeInstance(
					DateFormat.MEDIUM,
					DateFormat.MEDIUM );
    this.numFmt = NumberFormat.getNumberInstance();
    this.numFmt.setGroupingUsed( true );
  }


	/* --- TableCellRenderer --- */

  @Override
  public Component getTableCellRendererComponent(
					JTable  table,
					Object  value,
					boolean selected,
					boolean focus,
					int     row,
					int     col )
  {
    int hAlign = SwingConstants.LEFT;
    if( value != null ) {
      if( value instanceof java.util.Date ) {
	value = this.dateFmt.format( (java.util.Date) value );
      }
      else if( value instanceof Number ) {
	hAlign = SwingConstants.RIGHT;
	value  = this.numFmt.format( (Number) value );
      }
    }
    Component c = super.getTableCellRendererComponent(
						table,
						value,
						selected,
						focus,
						row,
						col );
    if( c != null ) {
      if( c instanceof JLabel )
	((JLabel) c).setHorizontalAlignment( hAlign );
    }
    return c;
  }
}
