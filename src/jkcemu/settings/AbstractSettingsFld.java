/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer eine Komponente fuer Einstellungen
 */

package jkcemu.settings;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.file.FileUtil;


public abstract class AbstractSettingsFld
				extends JPanel
				implements
					ActionListener,
					DropTargetListener,
					KeyListener
{
  protected SettingsFrm settingsFrm;
  protected String      propPrefix;


  protected AbstractSettingsFld( SettingsFrm settingsFrm )
  {
    this( settingsFrm, "" );
  }


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


  protected void confirmConflictSettings( String msg )
					throws UserCancelException
  {
    if( !BaseDlg.showYesNoWarningDlg(
		this, 
		msg + "\nM\u00F6chten Sie trotzdem die Einstellungen"
			+ " \u00FCbernehmen?",
		"Konflikt" ) )
    {
      throw new UserCancelException();
    }
  }


  protected boolean doAction( EventObject e )
  {
    return false;
  }


  protected void enableFileDrop( Component c )
  {
    (new DropTarget( c, this )).setActive( true );
  }


  protected void fileDropped( Component c, File file )
  {
    // leer
  }


  protected void fireDataChanged()
  {
    this.settingsFrm.fireDataChanged( true );
  }


  public SettingsFrm getSettingsFrm()
  {
    return this.settingsFrm;
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
      File file = FileUtil.showFileOpenDlg(
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
	  BaseDlg.showErrorDlg( this, msg );
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
    if( !isEnabled() || !FileUtil.isFileDrop( e ) )
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
      final DropTargetContext context = e.getDropTargetContext();
      if( context != null ) {
	final File file = FileUtil.fileDrop( this, e );
	if( file != null ) {
	  EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    fileDropped( context.getComponent(), file );
			  }
			} );
	}
	done = true;
      }
    }
    if( !done ) {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !isEnabled() || !FileUtil.isFileDrop( e ) )
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
      EmuUtil.checkAndShowError( this, null, ex );
    }
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }
}
