/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer den USB-Anschluss
 */

package jkcemu.etc;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.DirSelectDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileNameFld;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;


public class USBInterfaceFrm
			extends BaseFrm
			implements DropTargetListener
{
  private static final String ACTION_CLOSE = "close";
  private static final String ACTION_HELP  = "help";
  private static final String HELP_PAGE    = "/help/usb.htm";

  private static USBInterfaceFrm instance = null;

  private ScreenFrm   screenFrm;
  private FileNameFld dirFld;
  private JButton     btnDirSelect;
  private JButton     btnDirRemove;
  private JCheckBox   btnReadOnly;
  private JCheckBox   btnForceLowerCase;
  private JCheckBox   btnForceCurTimestamp;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new USBInterfaceFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
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
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      setDirFile( file );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    updFields();
    return super.applySettings( props, resizable );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnDirSelect ) {
	rv = true;
	doDirSelect();
      }
      else if( src == this.btnDirRemove ) {
	rv = true;
	setDirFile( null );
      }
      else if( src == this.btnReadOnly ) {
	rv = true;
	setReadOnly( this.btnReadOnly.isSelected() );
      }
      else if( src == this.btnForceLowerCase ) {
	rv = true;
	setForceLowerCase( this.btnForceLowerCase.isSelected() );
      }
      else if( src == this.btnForceCurTimestamp ) {
	rv = true;
	setForceCurrentTimestamp( this.btnForceCurTimestamp.isSelected() );
      }
      if( !rv && (e instanceof ActionEvent) ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.equals( ACTION_CLOSE ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( cmd.equals( ACTION_HELP ) ) {
	    rv = true;
	    HelpFrm.open( HELP_PAGE );
          }
	}
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private USBInterfaceFrm( ScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
    setTitle( "JKCEMU USB-Anschluss" );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuFile.add( createJMenuItem( "Schlie\u00DFen", ACTION_CLOSE ) );
    mnuBar.add( mnuFile );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuHelp.add( createJMenuItem( "Hilfe...", ACTION_HELP ) );
    mnuBar.add( mnuHelp );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add(
	new JLabel( "Verzeichnis des emulierten USB-Speichersticks:" ),
	gbc );

    this.dirFld   = new FileNameFld();
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = 1;
    gbc.gridy++;
    add( this.dirFld, gbc );

    this.btnDirSelect = createImageButton(
				"/images/file/open.png",
				"Verzeichnis ausw\u00E4hlen" );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.btnDirSelect, gbc );

    this.btnDirRemove = createImageButton(
				"/images/file/delete.png",
				"Verzeichnis entfernen" );
    gbc.gridx++;
    add( this.btnDirRemove, gbc );

    this.btnReadOnly = new JCheckBox(
				"Schreibschutz (Nur-Lese-Modus)",
				true );
    this.btnReadOnly.addActionListener( this );
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.btnReadOnly, gbc );

    this.btnForceLowerCase = new JCheckBox( "Dateinamen klein schreiben" );
    this.btnForceLowerCase.setEnabled( false );
    this.btnForceLowerCase.addActionListener( this );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnForceLowerCase, gbc );

    this.btnForceCurTimestamp = new JCheckBox(
			"Immer aktueller Zeitstempel bei Schreibzugriffen" );
    this.btnForceCurTimestamp.setEnabled( false );
    this.btnForceCurTimestamp.addActionListener( this );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btnForceCurTimestamp, gbc );


    // Fenstergroesse
    setLocationByPlatform( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
    }
    setResizable( true );


    // Sonstiges
    (new DropTarget( this.dirFld, this )).setActive( true );
    updFields();
  }


	/* --- private Methoden --- */

  private void doDirSelect()
  {
    File lastDir = this.dirFld.getFile();
    if( lastDir == null ) {
      lastDir = Main.getLastDirFile( Main.FILE_GROUP_USB );
    }
    if( lastDir != null ) {
      if( !lastDir.isDirectory() ) {
	lastDir = lastDir.getParentFile();
      }
    }
    File dirFile = DirSelectDlg.selectDirectory( this, lastDir );
    if( dirFile != null ) {
      setDirFile( dirFile );
    }
  }


  private void setDirFile( File dirFile )
  {
    EmuSys emuSys = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      if( dirFile != null ) {
	if( dirFile.isDirectory() ) {
	  this.dirFld.setFile( dirFile );
	  emuSys.setUSBMemStickDirectory( dirFile );
	  this.btnDirRemove.setEnabled( true );
	  Main.setProperty( VDIP.PROP_USB_DIR, dirFile.getPath() );
	  Main.setLastFile( dirFile, Main.FILE_GROUP_USB );
	  setReadOnly( this.btnReadOnly.isSelected() );
	  setForceLowerCase( this.btnForceLowerCase.isSelected() );
	}
      } else {
	this.dirFld.setFile( null );
	emuSys.setUSBMemStickDirectory( null );
	this.btnDirRemove.setEnabled( false );
	Main.setProperty( VDIP.PROP_USB_DIR, "" );
	Main.setLastFile( dirFile, Main.FILE_GROUP_USB );
      }
    }
  }


  private void setForceCurrentTimestamp( boolean state )
  {
    EmuSys emuSys = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      emuSys.setUSBMemStickForceCurrentTimestamp( state );
      Main.setProperty(
		"jkcemu.usb.memstick.force_current_timestamp",
		Boolean.toString( state ) );
    }
  }


  private void setForceLowerCase( boolean state )
  {
    EmuSys emuSys = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      emuSys.setUSBMemStickForceLowerCaseFileNames( state );
      Main.setProperty(
		"jkcemu.usb.memstick.force_lowercase_filenames",
		Boolean.toString( state ) );
    }
  }


  private void setReadOnly( boolean state )
  {
    EmuSys emuSys = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      emuSys.setUSBMemStickReadOnly( state );
      Main.setProperty(
		"jkcemu.usb.memstick.readonly",
		Boolean.toString( state ) );
    }
    this.btnForceCurTimestamp.setEnabled( !state );
    this.btnForceLowerCase.setEnabled( !state );
  }


  private void updFields()
  {
    File   dirFile = null;
    EmuSys emuSys  = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      boolean readOnly = emuSys.getUSBMemStickReadOnly();
      this.btnReadOnly.setSelected( readOnly );
      this.btnForceCurTimestamp.setSelected(
		emuSys.getUSBMemStickForceCurrentTimestamp() );
      this.btnForceCurTimestamp.setEnabled( !readOnly );
      this.btnForceLowerCase.setSelected(
		emuSys.getUSBMemStickForceLowerCaseFileNames() );
      this.btnForceLowerCase.setEnabled( !readOnly );
      dirFile = emuSys.getUSBMemStickDirectory();
    }
    this.dirFld.setFile( dirFile );
    this.btnDirRemove.setEnabled( dirFile != null );
  }
}
