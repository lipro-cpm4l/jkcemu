/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer Zellen im Dateiauswahldialog
 */

package jkcemu.base;

import java.awt.Component;
import java.io.File;
import java.lang.*;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;


public class FileSelectRenderer extends DefaultListCellRenderer
{
  private FileSystemView fsv;


  public FileSelectRenderer( FileSystemView fsv )
  {
    this.fsv = fsv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Component getListCellRendererComponent(
					JList   list,
					Object  value,
					int     index,
					boolean isSelected,
					boolean hasFocus )
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
	if( this.fsv != null ) {
	  icon = this.fsv.getSystemIcon( file );
	  text = this.fsv.getSystemDisplayName( file );
	} else {
	  text = file.getName();
	}
	if( text != null ) {
	  if( text.isEmpty() ) {
	    text = null;
	  }
	}
	if( text == null ) {
	  text = file.getPath();
	}
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
		BorderFactory.createEmptyBorder( 2, 2 + (10 * level), 2, 2 ) );
	}
      }
    }
    return rv;
  }
}
