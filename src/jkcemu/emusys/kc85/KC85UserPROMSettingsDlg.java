/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Einstellungen zu einem User-PROM-Modul
 */

package jkcemu.emusys.kc85;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileNameFld;


public class KC85UserPROMSettingsDlg
			extends BaseDlg
			implements DropTargetListener
{
  private static String[][] moduleTable = {
		{ "M025", "F7: 8K User PROM/EPROM", "FB: 8K ROM" },
		{ "M028", "F8: 16K User PROM/EPROM", "FC: 16K ROM" },
		{ "M040", "01: Autostart", "F7: 8K User PROM/EPROM",
					"F8: 16K User PROM/EPROM" },
		{ "M045", "70" },
		{ "M046", "71" },
		{ "M047", "72" },
		{ "M048", "73" } };

  private Frame          owner;
  private String         approvedFileName;
  private String         approvedTypeByteText;
  private String         moduleName;
  private String[]       moduleTableRow;
  private JRadioButton[] typeByteBtns;
  private FileNameFld    fileNameFld;
  private JButton        btnSelect;
  private JButton        btnOK;
  private JButton        btnCancel;


  public KC85UserPROMSettingsDlg(
			Frame  owner,
			String moduleName,
			String typeByteText,
			String fileName )
  {
    super( owner, moduleName );
    this.owner                = owner;
    this.approvedFileName     = null;
    this.approvedTypeByteText = null;
    this.moduleName           = moduleName;
    this.moduleTableRow       = null;
    for( String[] moduleTableRow : this.moduleTable ) {
      if( moduleTableRow.length > 1 ) {
	if( moduleTableRow[ 0 ].equals( moduleName ) ) {
	  this.moduleTableRow = moduleTableRow;
	}
      }
    }


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
    this.typeByteBtns = null;
    if( this.moduleTableRow != null ) {
      if( this.moduleTableRow.length > 2 ) {
	add( new JLabel( "Strukturbyte:" ), gbc );

	ButtonGroup grpTypeByte = new ButtonGroup();
	boolean     selected    = false;

	gbc.insets.bottom = 0;
	this.typeByteBtns = new JRadioButton[
					this.moduleTableRow.length - 1 ];
	for( int i = 0; i < this.typeByteBtns.length; i++ ) {
	  String       text = this.moduleTableRow[ i + 1 ];
	  JRadioButton btn  = new JRadioButton( text );
	  grpTypeByte.add( btn );
	  if( typeByteText != null ) {
	    if( text.startsWith( typeByteText ) ) {
	      btn.setSelected( true );
	      selected = true;
	    }
	  }
	  gbc.insets.left = 50;
	  if( i > 0 ) {
	    gbc.insets.top = 0;
	  }
	  if( i == (this.typeByteBtns.length - 1) ) {
	    gbc.insets.bottom = 5;
	  }
	  gbc.gridy++;
	  add( btn, gbc );
	  this.typeByteBtns[ i ] = btn;
	}
	if( !selected ) {
	  this.typeByteBtns[ 0 ].setSelected( true );
	}
      }
    }

    // ROM-Datei
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridy++;
    add( new JLabel( "ROM-Datei:" ), gbc );

    this.fileNameFld = new FileNameFld();
    if( fileName != null ) {
      this.fileNameFld.setFileName( fileName );
    }
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 0;
    gbc.gridwidth  = 1;
    gbc.gridy++;
    add( this.fileNameFld, gbc );

    this.btnSelect = createImageButton(
				"/images/file/open.png",
				"ROM-Datei ausw\u00E4hlen" );
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
    this.btnOK.setEnabled( this.fileNameFld.getFile() != null );
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
      if( this.typeByteBtns != null ) {
	for( JRadioButton btn : this.typeByteBtns ) {
	  if( btn.isSelected() ) {
	    String text = btn.getText();
	    if( text != null ) {
	      int pos = text.indexOf( ':' );
	      if( pos >= 0 ) {
		text = text.substring( 0, pos );
	      }
	    }
	    this.approvedTypeByteText = text;
	    break;
	  }
	}
      } else {
	if( this.moduleTableRow != null ) {
	  if( this.moduleTableRow.length > 1 ) {
	    this.approvedTypeByteText = this.moduleTableRow[ 1 ];
	  }
	}
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
      file = Main.getLastDirFile( Main.FILE_GROUP_ROM );
    }
    file = EmuUtil.showFileOpenDlg(
			this.owner,
			"ROM-Datei ausw\u00E4hlen",
			file,
			EmuUtil.getROMFileFilter() );
    if( file != null ) {
      this.fileNameFld.setFile( file );
      this.btnOK.setEnabled( true );
      Main.setLastFile( file, Main.FILE_GROUP_ROM );
    }
  }
}
