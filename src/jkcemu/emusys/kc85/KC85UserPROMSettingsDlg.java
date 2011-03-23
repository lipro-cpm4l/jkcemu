/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Einstellungen zu einem User-PROM-Modul
 */

package jkcemu.emusys.kc85;

import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class KC85UserPROMSettingsDlg
			extends BasicDlg
			implements DropTargetListener
{
  private Frame        owner;
  private String       approvedFileName;
  private String       approvedTypeByteText;
  private String       typeByteUserPROM;
  private String       typeByteROM;
  private JRadioButton btnTypeByteUserPROM;
  private JRadioButton btnTypeByteROM;
  private FileNameFld  fileNameFld;
  private JButton      btnSelect;
  private JButton      btnOK;
  private JButton      btnCancel;


  public KC85UserPROMSettingsDlg(
			Frame  owner,
			String typeByteUserPROM,
			String typeByteROM,
			String title )
  {
    super( owner, title );
    this.owner            = owner;
    this.typeByteUserPROM = typeByteUserPROM;
    this.typeByteROM      = typeByteROM;

    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Strukturbyte
    add( new JLabel( "Strukturbyte:" ), gbc );

    ButtonGroup grpTypeByte = new ButtonGroup();

    this.btnTypeByteUserPROM = new JRadioButton(
				typeByteUserPROM + ": User PROM/EPROM",
				true );
    grpTypeByte.add( this.btnTypeByteUserPROM );
    gbc.insets.left   = 50;
    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( this.btnTypeByteUserPROM, gbc );

    this.btnTypeByteROM = new JRadioButton(
				typeByteROM + ": ROM",
				false );
    grpTypeByte.add( this.btnTypeByteROM );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnTypeByteROM, gbc );

    // ROM-Datei
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridy++;
    add( new JLabel( "ROM-Datei:" ), gbc );

    this.fileNameFld = new FileNameFld();
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 0;
    gbc.gridwidth  = 1;
    gbc.gridy++;
    add( this.fileNameFld, gbc );

    this.btnSelect = EmuUtil.createImageButton(
					"/images/file/open.png",
					"ROM-Datei ausw\u00E4hlen" );
    this.btnSelect.addActionListener( this );
    this.btnSelect.addKeyListener( this );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.btnSelect, gbc );

    // Knoepfe
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 10;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.setEnabled( false );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    // Fenstergroesse
    pack();
    setParentCentered();

    // Drag&Drop ermoeglichen
    (new DropTarget( this.fileNameFld, this )).setActive( true );
  }


  public String getApprovedFileName()
  {
    return this.approvedFileName;
  }


  public String getApprovedTypeByteText()
  {
    return this.approvedTypeByteText;
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
    boolean           done    = false;
    DropTargetContext context = e.getDropTargetContext();
    if( context != null ) {
      if( context.getComponent() == this.fileNameFld ) {
	File file = EmuUtil.fileDrop( this, e );
	if( file != null ) {
	  this.fileNameFld.setFile( file );
	  this.btnOK.setEnabled( true );
	  done = true;
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
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
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


	/* --- private Methoden --- */

  private void doApprove()
  {
    File file = this.fileNameFld.getFile();
    if( file != null ) {
      if( this.btnTypeByteROM.isSelected() ) {
	this.approvedTypeByteText = this.typeByteROM;
      } else {
	this.approvedTypeByteText = this.typeByteUserPROM;
      }
      this.approvedFileName = file.getPath();
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
      file = Main.getLastPathFile( "rom" );
    }
    file = EmuUtil.showFileOpenDlg(
			this.owner,
			"ROM-Datei ausw\u00E4hlen",
			file,
			EmuUtil.getROMFileFilter() );
    if( file != null ) {
      this.fileNameFld.setFile( file );
      this.btnOK.setEnabled( true );
    }
  }
}

