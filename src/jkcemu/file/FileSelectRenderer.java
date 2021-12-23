/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer Zellen im Dateiauswahldialog
 */

package jkcemu.file;

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.metal.MetalIconFactory;


public class FileSelectRenderer extends DefaultListCellRenderer
{
  private static Icon fileIcon   = new MetalIconFactory.FileIcon16();
  private static Icon folderIcon = new MetalIconFactory.FolderIcon16();

  private Map<File,Icon> iconCache;


  public FileSelectRenderer()
  {
    this.iconCache = new HashMap<>();
  }


  public void clearCache()
  {
    this.iconCache.clear();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Component getListCellRendererComponent(
					JList<?> list,
					Object   value,
					int      index,
					boolean  isSelected,
					boolean  hasFocus )
  {
    File   file  = null;
    Icon   icon  = null;
    String text  = null;
    int    level = -1;
    if( value != null ) {
      FileSelectDlg.DirItem dirItem = null;
      if( value instanceof FileSelectDlg.DirItem ) {
        dirItem = (FileSelectDlg.DirItem) value;
	file    = dirItem.getDirectory();
	if( index >= 0 ) {
	  level = ((FileSelectDlg.DirItem) value).getLevel();
	} else {
	  level = 0;
	}
      }
      else if( value instanceof File ) {
	file = (File) value;
      }
      if( file != null ) {
	if( this.iconCache.containsKey( file ) ) {
	  icon = this.iconCache.get( file );
	} else {
	  icon = FileUtil.getSystemIcon( file );
	  if( icon == null ) {
	    if( (level >= 0) || file.isDirectory() ) {
	      icon = folderIcon;
	    } else {
	      icon = fileIcon;
	    }
	  }
	  this.iconCache.put( file, icon );
	}
	text = FileUtil.getSystemDisplayName( file );
	if( (dirItem == null) && (icon == null) && file.isDirectory() ) {
	  text += File.separatorChar;
	}
	value = text;
      }
    }
    Component rv = super.getListCellRendererComponent(
						list,
						value,
						index,
						isSelected,
						hasFocus );
    if( (rv != null) && (file != null) ) {
      if( rv instanceof JLabel ) {
	((JLabel) rv).setIcon( icon );
	((JLabel) rv).setText( text );
	if( level >= 0 ) {
	  ((JLabel) rv).setBorder(
		BorderFactory.createEmptyBorder(
					2, 2 + (10 * level), 2, 2 ) );
	}
      }
    }
    return rv;
  }
}
