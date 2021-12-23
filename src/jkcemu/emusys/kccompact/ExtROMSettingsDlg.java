/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Einstellungen zu einer ROM-Erweiterung
 */

package jkcemu.emusys.kccompact;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;


public class ExtROMSettingsDlg
			extends BaseDlg
			implements DropTargetListener
{
  private Window            owner;
  private ExtROM            approvedExtROM;
  private JComboBox<String> comboRomNum;
  private FileNameFld       fileNameFld;
  private JButton           btnSelect;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static ExtROM showDlg( Window owner, ExtROM extROM )
  {
    ExtROMSettingsDlg dlg = new ExtROMSettingsDlg( owner, extROM );
    dlg.setVisible( true );
    return dlg.approvedExtROM;
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !FileUtil.isFileDrop( e ) )
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
    boolean           done    = false;
    DropTargetContext context = e.getDropTargetContext();
    if( context != null ) {
      if( context.getComponent() == this.fileNameFld ) {
	File file = FileUtil.fileDrop( this, e );
	if( file != null ) {
	  this.fileNameFld.setFile( file );
	  this.btnOK.setEnabled( true );
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
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnSelect ) {
	  rv = true;
	  doFileSelect();
	}
	else if( src == this.btnOK ) {
	  rv = true;
	  doApprove();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnSelect.removeActionListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ExtROMSettingsDlg( Window owner, ExtROM extROM )
  {
    super( owner, "ROM-Erweiterung" );
    this.owner          = owner;
    this.approvedExtROM = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "ROM-Nummer:" ), gbc );

    this.comboRomNum = GUIFactory.createComboBox();
    this.comboRomNum.setEditable( false );
    for( int i = 1; i < 16; i++ ) {
      if( i == 7 ) {
	this.comboRomNum.addItem( "7 (FDC-ROM)" );
      } else {
	this.comboRomNum.addItem( String.valueOf( i ) );
      }
    }
    gbc.gridx++;
    add( this.comboRomNum, gbc );

    gbc.insets.top = 10;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "ROM-Datei:" ), gbc );

    this.fileNameFld = new FileNameFld();
    this.fileNameFld.setColumns( 40 );
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 0;
    gbc.gridwidth  = 3;
    gbc.gridy++;
    add( this.fileNameFld, gbc );

    this.btnSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_ROM_FILE );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridx += 3;
    add( this.btnSelect, gbc );


    // Knoepfe
    JPanel panelBtn   = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 10;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    this.btnOK.setEnabled( false );
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Vorbelegungen
    if( extROM != null ) {
      int romNum = extROM.getRomNum();
      if( (romNum >= 1) && (romNum < 16) ) {
	try {
	  this.comboRomNum.setSelectedIndex( romNum - 1 );
	}
	catch( IllegalArgumentException ex ) {}
      }
      String fileName = extROM.getFileName();
      if( fileName != null ) {
	this.fileNameFld.setFileName( fileName );
	this.btnOK.setEnabled( true );
      }
    }


    // Fenstergroesse
    pack();
    setParentCentered();


    // Listener
    this.btnSelect.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // Drag&Drop ermoeglichen
    (new DropTarget( this.fileNameFld, this )).setActive( true );
  }


	/* --- private Methoden --- */

  private void doApprove()
  {
    File file = this.fileNameFld.getFile();
    if( file != null ) {
      this.approvedExtROM = new ExtROM(
			this.comboRomNum.getSelectedIndex() + 1,
			file.getPath() );
      doClose();
    } else {
      showErrorDlg(
		this,
		"Sie m\u00Fcssen eine ROM-Datei ausw\u00E4hlen" );
    }
  }


  private void doFileSelect()
  {
    File file = this.fileNameFld.getFile();
    if( file == null ) {
      file = Main.getLastDirFile( Main.FILE_GROUP_ROM );
    }
    file = FileUtil.showFileOpenDlg(
			this.owner,
			EmuUtil.TEXT_SELECT_ROM_FILE,
			file,
			FileUtil.getROMFileFilter() );
    if( file != null ) {
      this.fileNameFld.setFile( file );
      this.btnOK.setEnabled( true );
      Main.setLastFile( file, Main.FILE_GROUP_ROM );
    }
  }
}
