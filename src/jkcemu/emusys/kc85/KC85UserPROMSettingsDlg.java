/*
 * (c) 2011-2021 Jens Mueller
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
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;


public class KC85UserPROMSettingsDlg
			extends BaseDlg
			implements DropTargetListener
{
  private static String[][] moduleTable = {
		{ "M025", "01: Autostart", "F7: 8K User PROM/EPROM",
					"FB: 8K ROM" },
		{ "M028", "F8: 16K User PROM/EPROM", "FC: 16K ROM" },
		{ "M040", "01: Autostart", "F7: 8K User PROM/EPROM",
					"F8: 16K User PROM/EPROM" },
		{ "M041", "01: Autostart", "F1: 16K EEPROM",
					"F8: 16K User PROM/EPROM",
					"FC: 16K ROM" },
		{ "M045", "01: Autostart", "70: 4x8K PROM/EPROM" },
		{ "M046", "01: Autostart", "71: 8x8K PROM/EPROM" },
		{ "M047", "01: Autostart", "72: 16x8K PROM/EPROM" },
		{ "M048", "01: Autostart", "73: 16x16K PROM/EPROM" } };

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
    for( String[] moduleTableRow : moduleTable ) {
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
	add( GUIFactory.createLabel( "Strukturbyte:" ), gbc );

	ButtonGroup grpTypeByte = new ButtonGroup();
	boolean     selected    = false;

	gbc.insets.bottom = 0;
	this.typeByteBtns = new JRadioButton[
					this.moduleTableRow.length - 1 ];
	for( int i = 0; i < this.typeByteBtns.length; i++ ) {
	  String       text = this.moduleTableRow[ i + 1 ];
	  JRadioButton rb   = GUIFactory.createRadioButton( text );
	  grpTypeByte.add( rb );
	  if( typeByteText != null ) {
	    if( text.startsWith( typeByteText ) ) {
	      rb.setSelected( true );
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
	  add( rb, gbc );
	  this.typeByteBtns[ i ] = rb;
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
    add( GUIFactory.createLabel( "ROM-Datei:" ), gbc );

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

    this.btnSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_ROM_FILE );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridx++;
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
    this.btnOK.setEnabled( this.fileNameFld.getFile() != null );
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


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


	/* --- private Methoden --- */

  private void doApprove()
  {
    File file = this.fileNameFld.getFile();
    if( file != null ) {
      if( this.typeByteBtns != null ) {
	for( JRadioButton rb : this.typeByteBtns ) {
	  if( rb.isSelected() ) {
	    String text = rb.getText();
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
