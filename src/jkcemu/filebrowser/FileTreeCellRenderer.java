/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer Zellen in einer baumartigen Dateisystemansicht
 */

package jkcemu.filebrowser;

import java.awt.Component;
import java.io.File;
import java.lang.*;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;


public class FileTreeCellRenderer extends DefaultTreeCellRenderer
{
  FileSystemView fsv;


  public FileTreeCellRenderer()
  {
    this.fsv = FileSystemView.getFileSystemView();
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
    if( (this.fsv != null) && (c != null) && (value != null) ) {
      if( (c instanceof JLabel) && (value instanceof FileNode) ) {
	File file = ((FileNode) value).getFile();
	if( file != null ) {
	  Icon icon = this.fsv.getSystemIcon( file );
	  if( icon != null ) {
	    ((JLabel) c).setIcon( icon );
	  }
	}
      }
    }
    return c;
  }
}
