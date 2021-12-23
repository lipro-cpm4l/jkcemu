/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * CellRenderer fuer einen Eintrag in einer Breakpoint-Liste
 */

package jkcemu.tools.debugger;

import java.awt.Component;
import java.awt.Image;
import java.awt.Window;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import jkcemu.Main;


public class BreakpointCellRenderer extends DefaultListCellRenderer
{
  private Window window;


  public BreakpointCellRenderer( Window window )
  {
    this.window = window;
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
    if( (value != null) && (c != null) ) {
      if( (value instanceof AbstractBreakpoint) && (c instanceof JLabel) ) {
	boolean stopEnabled = ((AbstractBreakpoint) value).isStopEnabled();
	boolean logEnabled  = ((AbstractBreakpoint) value).isLogEnabled();
	Image   image       = null;
	String  imgResource = null;
	if( stopEnabled && logEnabled ) {
	  imgResource = "/images/debug/bp_stop_log.png";
	} else {
	  if( stopEnabled ) {
	    imgResource = "/images/debug/bp_stop.png";
	  } else if( logEnabled ) {
	    imgResource = "/images/debug/bp_log.png";
	  } else {
	    imgResource = "/images/debug/bp_disabled.png";
	  }
	}
	if( imgResource != null ) {
	  image = Main.getLoadedImage( this.window, imgResource );
	}
	((JLabel) c).setHorizontalTextPosition( SwingConstants.TRAILING );
	((JLabel) c).setVerticalTextPosition( SwingConstants.CENTER );
	((JLabel) c).setIcon( image != null ? new ImageIcon( image ) : null );

	// den vollstaendigen Text als ToolTip anzeigen
	String s = null;
	if( value != null ) {
	  s = value.toString();
	}
	((JLabel) c).setToolTipText( s );
      }
    }
    return c;
  }
}
