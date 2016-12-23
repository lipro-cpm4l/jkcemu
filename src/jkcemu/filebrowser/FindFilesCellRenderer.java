/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Renderer fuer die Ergbnisliste der Dateisuche
 */

package jkcemu.filebrowser;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.lang.*;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;


public class FindFilesCellRenderer extends DefaultListCellRenderer
{
  public static class CodeEntry
  {
    private String                    text;
    private FileActionMngr.FileObject file;

    public CodeEntry( String text, FileActionMngr.FileObject file )
    {
      this.text = text;
      this.file = file;
    }

    public FileActionMngr.FileObject getFileObject()
    {
      return this.file;
    }

    @Override
    public String toString()
    {
      return this.text;
    }
  };


  public static class EmphasizedEntry
  {
    private String text;

    public EmphasizedEntry( String text )
    {
      this.text = text;
    }

    @Override
    public String toString()
    {
      return this.text;
    }
  };


  private Font defaultFont;
  private Font codeFont;


  public FindFilesCellRenderer()
  {
    this.defaultFont = getFont();
    this.codeFont    = null;
    if( this.defaultFont != null ) {
      this.codeFont = new Font(
			Font.MONOSPACED,
			Font.PLAIN,
			this.defaultFont.getSize() );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Component getListCellRendererComponent(
					JList<?> list,
					Object   value,
					int      index,
					boolean  isSelected,
					boolean  cellHasFocus )
  {
    Component c = super.getListCellRendererComponent(
						list,
						value,
						index,
						isSelected,
						cellHasFocus );
    if( (c != null) && (this.defaultFont != null) ) {
      c.setFont( this.defaultFont );
      c.setForeground( isSelected ? Color.WHITE : Color.BLACK );
      if( value != null ) {
	if( value instanceof CodeEntry ) {
	  c.setForeground( isSelected ? Color.LIGHT_GRAY : Color.DARK_GRAY );
	  if( this.codeFont != null ) {
	    c.setFont( this.codeFont );
	  }
	} else if( value instanceof EmphasizedEntry ) {
	  c.setForeground( isSelected ? Color.YELLOW : Color.RED );
	}
      }
    }
    return c;
  }
}
