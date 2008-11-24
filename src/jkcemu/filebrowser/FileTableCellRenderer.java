/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer Tabellenzellen
 */

package jkcemu.filebrowser;

import java.awt.Component;
import java.lang.*;
import java.text.*;
import javax.swing.*;


public class FileTableCellRenderer
			extends javax.swing.table.DefaultTableCellRenderer
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

