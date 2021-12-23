/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenzellen-Renderer fuer Farbpalette
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public class ColorPaletteTableCellRenderer extends DefaultTableCellRenderer
{
  private Color defaultBg;


  public ColorPaletteTableCellRenderer()
  {
    this.defaultBg = getBackground();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Component getTableCellRendererComponent(
					JTable  table,
					Object  value,
					boolean isSelected,
					boolean hasFocus,
					int     row,
					int     col )
  {
    Color color = this.defaultBg;
    if( value != null ) {
      if( value instanceof Color ) {
	color = (Color) value;
	value = null;
      }
    }
    Component component = super.getTableCellRendererComponent(
							table,
							value,
							isSelected,
							hasFocus,
							row,
							col );
    if( component != null ) {
      component.setBackground( color );
    }
    return component;
  }
}
