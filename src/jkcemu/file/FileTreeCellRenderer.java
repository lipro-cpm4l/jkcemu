/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer Zellen in einer baumartigen Dateisystemansicht
 */

package jkcemu.file;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;


public class FileTreeCellRenderer extends DefaultTreeCellRenderer
{
  public FileTreeCellRenderer()
  {
    // leer
  }


	/* --- TreeCellRenderer --- */

  @Override
  public Component getTreeCellRendererComponent(
					JTree   tree,
					Object  value,
					boolean selected,
					boolean expanded,
					boolean leaf,
					int     row,
					boolean focus )
  {
    Component c = super.getTreeCellRendererComponent(
						tree,
						value,
						selected,
						expanded,
						leaf,
						row,
						focus );
    if( (c != null) && (value != null) ) {
      if( (c instanceof JLabel) && (value instanceof FileNode) ) {
	Icon icon = ((FileNode) value).getNodeIcon();
	if( icon != null ) {
	  ((JLabel) c).setIcon( icon );
	}
      }
    }
    return c;
  }
}
