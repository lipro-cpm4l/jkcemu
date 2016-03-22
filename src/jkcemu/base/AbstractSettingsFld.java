/*
 * (c) 2010-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer eine Komponente fuer Einstellungen
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;


public abstract class AbstractSettingsFld
				extends JPanel
				implements
					ActionListener,
					DropTargetListener,
					KeyListener
{
  protected SettingsFrm settingsFrm;
  protected String      propPrefix;


  protected AbstractSettingsFld(
			SettingsFrm settingsFrm,
			String      propPrefix )
  {
    this.settingsFrm = settingsFrm;
    this.propPrefix  = propPrefix;
  }


  public void applyInput(
			Properties props,
			boolean    selected ) throws
						UserCancelException,
						UserInputException
  {
    // leer
  }


  protected void checkFileReadable( File file ) throws UserInputException
  {
    if( file != null ) {
      if( file.isDirectory() || !file.canRead() ) {
	throw new UserInputException(
			file.getPath() + ": Datei nicht lesbar" );
      }
    }
  }


  protected JButton createImageButton( String imgName, String text )
  {
    JButton btn = EmuUtil.createImageButton( this.settingsFrm, imgName, text );
    btn.addActionListener( this );
    btn.addKeyListener( this );
    return btn;
  }


  protected boolean doAction( EventObject e )
  {
    return false;
  }


  protected void enableFileDrop( Component c )
  {
    (new DropTarget( c, this )).setActive( true );
  }


  protected boolean fileDropped( Component c, File file )
  {
    return false;
  }


  protected void fireDataChanged()
  {
    this.settingsFrm.fireDataChanged();
  }


  public void lookAndFeelChanged()
  {
    // leer
  }


  protected File selectFile(
			String                                title,
			String                                category,
			File                                  oldFile,
			javax.swing.filechooser.FileFilter... fileFilters )
  {
    File   rv     = null;
    Window window = EmuUtil.getWindow( this );
    if( window != null ) {
      File file = EmuUtil.showFileOpenDlg(
			window,
			title,
			oldFile != null ?
				oldFile
				: Main.getLastDirFile( category ),
			fileFilters );
      if( file != null ) {
	String msg = null;
	if( file.exists() ) {
	  if( file.canRead() ) {
	    rv = file;
	    Main.setLastFile( file, category );
	  } else {
	    msg = "Datei nicht lesbar";
	  }
	} else {
	  msg = "Datei nicht gefunden";
	}
	if( msg != null ) {
	  BasicDlg.showErrorDlg( this, msg );
	}
      }
    }
    return rv;
  }


  public void updFields( Properties props )
  {
    // leer
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    doActionInternal( e );
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !isEnabled() || !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    boolean done = false;
    if( isEnabled() ) {
      DropTargetContext context = e.getDropTargetContext();
      if( context != null ) {
	File file = EmuUtil.fileDrop( this, e );
	if( file != null ) {
	  done = fileDropped( context.getComponent(), file );
	}
      }
    }
    if( done ) {
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !isEnabled() || !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e != null ) {
      if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
	if( doActionInternal( e ) ) {
	  e.consume();
	}
      }
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    // leer
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    // leer
  }


	/* --- private Methoden --- */

  private boolean doActionInternal( EventObject e )
  {
    boolean rv = false;
    this.settingsFrm.setWaitCursor( true );
    try {
      rv = doAction( e );
    }
    catch( Exception ex ) {
      EmuUtil.exitSysError( this, null, ex );
    }
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }
}

