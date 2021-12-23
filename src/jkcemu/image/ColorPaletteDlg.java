/*
 * (c) 2017-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anzeigen und Aendern der Farbpalette
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileUtil;
import jkcemu.base.PopupMenusOwner;
import jkcemu.base.GUIFactory;


public class ColorPaletteDlg
			extends BaseDlg
			implements
				ListSelectionListener,
				PopupMenusOwner,
				TableModelListener
{
  private static boolean blinkOldNew     = false;
  private static File    lastPaletteFile = null;

  private ImageFld               imageFld;
  private IndexColorModel        orgICM;
  private IndexColorModel        importedICM;
  private BufferedImage          orgImg;
  private BufferedImage          appliedImg;
  private Point                  tablePopupPoint;
  private ColorPaletteTableModel tableModel;
  private ListSelectionModel     selModel;
  private JTable                 table;
  private JCheckBox              cbBlinkOldNew;
  private JButton                btnApply;
  private JButton                btnColor;
  private JButton                btnCancel;
  private JButton                btnExport;
  private JButton                btnImport;
  private JButton                btnReset;
  private JMenuItem              mnuImportA5105;
  private JMenuItem              mnuImportCPC;
  private JMenuItem              mnuImportKC854Hires;
  private JMenuItem              mnuImportFile;
  private JMenuItem              mnuColorAll;
  private JMenuItem              mnuColorA5105;
  private JMenuItem              mnuColorCPC;
  private JMenuItem              mnuColorKC854Hires;
  private JMenuItem              mnuColorFile;
  private JMenuItem              mnuContextCut;
  private JMenuItem              mnuContextCopy;
  private JMenuItem              mnuContextPaste;
  private JMenuItem              mnuContextColorAll;
  private JMenuItem              mnuContextColorA5105;
  private JMenuItem              mnuContextColorCPC;
  private JMenuItem              mnuContextColorKC854Hires;
  private JMenuItem              mnuContextColorFile;
  private JPopupMenu             popupColor;
  private JPopupMenu             popupImport;
  private JPopupMenu             popupTable;
  private boolean                blinkStateOld;
  private javax.swing.Timer      blinkTimer;


  public static BufferedImage showDlg( ImageFrm imageFrm )
  {
    ImageFld      imageFld = imageFrm.getImageFld();
    BufferedImage orgImg   = imageFld.getImage();
    BufferedImage retImg   = null;
    if( orgImg != null ) {
      IndexColorModel icm = ImageUtil.getIndexColorModel( orgImg );
      if( icm != null ) {
	ColorPaletteDlg dlg = new ColorPaletteDlg(
						imageFrm,
						imageFld,
						orgImg,
						icm );
	dlg.setVisible( true );
	retImg = dlg.appliedImg;
      } else {
	showErrorDlg(
		imageFrm,
		"Das Bild hat keine indexierten Farben,\n"
			+ "die als Farbpalette angezeigt werden"
			+ " k\u00F6nnten." );
      }
    }
    return retImg;
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.selModel )
      this.btnColor.setEnabled( this.table.getSelectedRowCount() == 1 );
  }


	/* --- PopupMenusOwner --- */

  @Override
  public JPopupMenu[] getPopupMenus()
  {
    return new JPopupMenu[] {
			this.popupColor,
			this.popupImport,
			this.popupTable };
  }


	/* --- TableModelListener --- */

  @Override
  public void tableChanged( TableModelEvent e )
  {
    if( e.getSource() == this.tableModel ) {
      boolean state = this.tableModel.hasChangedARGBs();
      this.btnApply.setEnabled( state );
      this.btnReset.setEnabled( state );
      updPreview();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object src = e.getSource();
      if( src == this.blinkTimer ) {
	rv = true;
	doBlinkTimer();
      }
      else if( src == this.btnApply ) {
	rv = true;
	doApply();
      }
      else if( src == this.cbBlinkOldNew ) {
	rv = true;
	doBlinkOldNew();
      }
      else if( src == this.btnColor ) {
	rv = true;
	this.popupColor.show( this.btnColor, 0, this.btnColor.getHeight() );
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
      else if( src == this.btnExport ) {
	rv = true;
	doExport();
      }
      else if( src == this.btnImport ) {
	rv = true;
	this.popupImport.show(
			this.btnImport,
			0,
			this.btnImport.getHeight() );
      }
      else if( src == this.btnReset ) {
	rv = true;
	this.tableModel.clearChangedARGBs();
      }
      else if( (src == this.mnuColorAll)
	       || (src == this.mnuContextColorAll) )
      {
	doColorAll();
      }
      else if( (src == this.mnuColorA5105)
	       || (src == this.mnuContextColorA5105) )
      {
	doColorSelect(
		ImageUtil.getColorModelA5105(),
		"A5105-Farbpalette" );
      }
      else if( (src == this.mnuColorCPC)
	       || (src == this.mnuContextColorCPC) )
      {
	doColorSelect(
		ImageUtil.getColorModelCPC(),
		"CPC/KC-compact-Farbpalette" );
      }
      else if( (src == this.mnuColorKC854Hires)
	       || (src == this.mnuContextColorKC854Hires) )
      {
	doColorSelect(
		ImageUtil.getColorModelKC854Hires(),
		"KC85/4-HIRES-Farbpalette" );
      }
      else if( src == this.mnuImportA5105 ) {
	rv = true;
	importColorPalette( ImageUtil.getColorModelA5105(), null );
      }
      else if( src == this.mnuImportCPC ) {
	rv = true;
	importColorPalette( ImageUtil.getColorModelCPC(), null );
      }
      else if( src == this.mnuImportKC854Hires ) {
	rv = true;
	importColorPalette( ImageUtil.getColorModelKC854Hires(), null );
      }
      else if( src == this.mnuImportFile ) {
	rv = true;
	doImportFile();
      }
      else if( src == this.mnuContextCut ) {
	rv = true;
	doContextCut();
      }
      else if( src == this.mnuContextCopy ) {
	rv = true;
	doContextCopy();
      }
      else if( src == this.mnuContextPaste ) {
	rv = true;
	doContextPaste();
      }
    }
    catch( IOException ex ) {
      showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      blinkOldNew = this.cbBlinkOldNew.isSelected();
      this.cbBlinkOldNew.removeActionListener( this );
      this.btnApply.removeActionListener( this );
      this.btnColor.removeActionListener( this );
      this.btnExport.removeActionListener( this );
      this.btnImport.removeActionListener( this );
      this.btnReset.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
      this.mnuColorAll.removeActionListener( this );
      this.mnuColorA5105.removeActionListener( this );
      this.mnuColorCPC.removeActionListener( this );
      this.mnuColorKC854Hires.removeActionListener( this );
      this.mnuColorFile.removeActionListener( this );
      this.mnuImportA5105.removeActionListener( this );
      this.mnuImportCPC.removeActionListener( this );
      this.mnuImportKC854Hires.removeActionListener( this );
      this.mnuImportFile.removeActionListener( this );
      this.mnuContextCut.removeActionListener( this );
      this.mnuContextCopy.removeActionListener( this );
      this.mnuContextPaste.removeActionListener( this );
      this.mnuContextColorAll.removeActionListener( this );
      this.mnuContextColorA5105.removeActionListener( this );
      this.mnuContextColorCPC.removeActionListener( this );
      this.mnuContextColorKC854Hires.removeActionListener( this );
      this.mnuContextColorFile.removeActionListener( this );
      this.table.removeMouseListener( this );
      this.tableModel.removeTableModelListener( this );
      if( this.selModel != null ) {
	this.selModel.removeListSelectionListener( this );
      }
      this.blinkTimer.stop();
    }
    return rv;
  }


  @Override
  public boolean showPopupMenu( MouseEvent e )
  {
    boolean rv = false;
    if( e.getComponent() == this.table ) {
      this.tablePopupPoint = e.getPoint();
      if( this.tablePopupPoint != null ) {
	int row = this.table.rowAtPoint( this.tablePopupPoint );
	if( row >= 0 ) {
	  EmuUtil.fireSelectRow( this.table, row );
	}
      }
      boolean stateEdit = false;
      boolean stateCopy = false;
      if( this.tablePopupPoint != null ) {
	EmuUtil.Cell cell = EmuUtil.getModelCellAt(
						this.table,
						this.tablePopupPoint );
	if( cell != null ) {
	  String text = this.tableModel.getTextAt( cell.row, cell.col );
	  if( text != null ) {
	    stateCopy = !text.isEmpty();
	  }
	  stateEdit = this.tableModel.isCellEditable( cell.row, cell.col );
	}
      }
      boolean stateCut   = stateCopy && stateEdit;
      boolean statePaste = false;
      if( stateEdit ) {
	String text = EmuUtil.getClipboardText( this );
	if( text != null ) {
	  statePaste = !text.isEmpty();
	}
      }
      this.mnuContextCut.setEnabled( stateCut );
      this.mnuContextCopy.setEnabled( stateCopy );
      this.mnuContextPaste.setEnabled( statePaste );
      this.popupTable.show( e.getComponent(), e.getX(), e.getY() );
      rv = true;
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ColorPaletteDlg(
		ImageFrm        imageFrm,
		ImageFld        imageFld,
		BufferedImage   orgImg,
		IndexColorModel icm )
  {
    super( imageFrm, "Farbpalette" );
    this.imageFld        = imageFld;
    this.orgImg          = orgImg;
    this.orgICM          = icm;
    this.importedICM     = null;
    this.appliedImg      = null;
    this.tablePopupPoint = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tableModel = new ColorPaletteTableModel( icm, true );
    this.table      = GUIFactory.createTable( this.tableModel );
    this.table.setAutoCreateRowSorter( false );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setDefaultRenderer(
				Color.class,
				new ColorPaletteTableCellRenderer() );
    this.table.setDragEnabled( false );
    this.table.setFillsViewportHeight( false );
    this.table.setPreferredScrollableViewportSize(
					new Dimension( 420, 280 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    add( GUIFactory.createScrollPane( this.table ), gbc );

    EmuUtil.setTableColWidths( this.table, 30, 110, 50, 150, 80 );


    // beide Farbpaletten anzeigen
    this.cbBlinkOldNew = GUIFactory.createCheckBox(
		"Urspr\u00FCngliche und ge\u00E4nderte Farbpalette"
			+ " abwechselnd anzeigen",
		blinkOldNew );
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.weighty   = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( this.cbBlinkOldNew, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 6, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.gridwidth   = 1;
    gbc.gridy       = 0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButtonApply();
    this.btnApply.setEnabled( false );
    panelBtn.add( this.btnApply );

    this.btnColor = GUIFactory.createButton( "Farbe..." );
    panelBtn.add( this.btnColor );

    this.btnExport = GUIFactory.createButton( "Exportieren..." );
    panelBtn.add( this.btnExport );

    this.btnImport = GUIFactory.createButton( "Importieren..." );
    panelBtn.add( this.btnImport );

    this.btnReset = GUIFactory.createButtonReset();
    this.btnReset.setEnabled( false );
    panelBtn.add( this.btnReset );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Popup-Menu fuer Color-Button
    this.popupColor = GUIFactory.createPopupMenu();

    this.mnuColorAll = GUIFactory.createMenuItem( "aus allen Farben..." );
    this.popupColor.add( this.mnuColorAll );

    this.mnuColorA5105 = GUIFactory.createMenuItem(
					"aus A5105-Farbpalette..." );
    this.popupColor.add( this.mnuColorA5105 );

    this.mnuColorCPC = GUIFactory.createMenuItem(
				"aus CPC-/KC-compact-Farbpalette..." );
    this.popupColor.add( this.mnuColorCPC );

    this.mnuColorKC854Hires = GUIFactory.createMenuItem(
				"aus KC85/4-HIRES-Farbpalette..." );
    this.popupColor.add( this.mnuColorKC854Hires );

    this.mnuColorFile = GUIFactory.createMenuItem(
				"aus importierter Farbpalette..." );
    this.mnuColorFile.setEnabled( false );
    this.popupColor.add( this.mnuColorFile );


    // Popup-Menu fuer Import-Button
    this.popupImport = GUIFactory.createPopupMenu();

    this.mnuImportA5105 = GUIFactory.createMenuItem(
					"A5105-Farbpalette (16 Farben)" );
    this.popupImport.add( this.mnuImportA5105 );

    this.mnuImportCPC = GUIFactory.createMenuItem(
				"CPC-/KC-compact-Farbpalette (27 Farben)" );
    this.popupImport.add( this.mnuImportCPC );

    this.mnuImportKC854Hires = GUIFactory.createMenuItem(
				"KC85/4-HIRES-Farbpalette (4 Farben)" );
    this.popupImport.add( this.mnuImportKC854Hires );

    this.mnuImportFile = GUIFactory.createMenuItem(
					"Farbpalette aus Datei..." );
    this.popupImport.add( this.mnuImportFile );


    // Popup-Menu fuer Tabelle
    this.popupTable = GUIFactory.createPopupMenu();

    this.mnuContextCut = GUIFactory.createMenuItem( EmuUtil.TEXT_CUT );
    this.popupTable.add( this.mnuContextCut );

    this.mnuContextCopy = GUIFactory.createMenuItem( EmuUtil.TEXT_COPY );
    this.popupTable.add( this.mnuContextCopy );

    this.mnuContextPaste = GUIFactory.createMenuItem( EmuUtil.TEXT_PASTE );
    this.popupTable.add( this.mnuContextPaste );
    this.popupTable.addSeparator();

    this.mnuContextColorAll = GUIFactory.createMenuItem(
					"Farbe aus allen Farben..." );
    this.popupTable.add( this.mnuContextColorAll );

    this.mnuContextColorA5105 = GUIFactory.createMenuItem(
				"Farbe aus A5105-Farbpalette..." );
    this.popupTable.add( this.mnuContextColorA5105 );

    this.mnuContextColorCPC = GUIFactory.createMenuItem(
				"Farbe aus CPC-/KC-compact-Farbpalette..." );
    this.popupTable.add( this.mnuContextColorCPC );

    this.mnuContextColorKC854Hires = GUIFactory.createMenuItem(
				"Farbe aus KC85/4-HIRES-Farbpalette..." );
    this.popupTable.add( this.mnuContextColorKC854Hires );

    this.mnuContextColorFile = GUIFactory.createMenuItem(
				"Farbe aus importierter Farbpalette..." );
    this.mnuContextColorFile.setEnabled( false );
    this.popupTable.add( this.mnuContextColorFile );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Bild anzeigen
    updPreview();
    imageFrm.fireFitImage();


    // Sonstiges
    this.cbBlinkOldNew.addActionListener( this );
    this.btnApply.addActionListener( this );
    this.btnColor.addActionListener( this );
    this.btnExport.addActionListener( this );
    this.btnImport.addActionListener( this );
    this.btnReset.addActionListener( this );
    this.btnCancel.addActionListener( this );
    this.mnuColorAll.addActionListener( this );
    this.mnuColorA5105.addActionListener( this );
    this.mnuColorCPC.addActionListener( this );
    this.mnuColorKC854Hires.addActionListener( this );
    this.mnuColorFile.addActionListener( this );
    this.mnuImportA5105.addActionListener( this );
    this.mnuImportCPC.addActionListener( this );
    this.mnuImportKC854Hires.addActionListener( this );
    this.mnuImportFile.addActionListener( this );
    this.mnuContextCut.addActionListener( this );
    this.mnuContextCopy.addActionListener( this );
    this.mnuContextPaste.addActionListener( this );
    this.mnuContextColorAll.addActionListener( this );
    this.mnuContextColorA5105.addActionListener( this );
    this.mnuContextColorCPC.addActionListener( this );
    this.mnuContextColorKC854Hires.addActionListener( this );
    this.mnuContextColorFile.addActionListener( this );
    this.table.addMouseListener( this );
    this.tableModel.addTableModelListener( this );

    this.selModel = this.table.getSelectionModel();
    if( this.selModel != null ) {
      this.selModel.addListSelectionListener( this );
      this.btnColor.setEnabled( false );
    }
    this.blinkStateOld = false;
    this.blinkTimer    = new javax.swing.Timer( 500, this );
    if( blinkOldNew ) {
      this.blinkTimer.start();
    }
  }


	/* --- Aktionen --- */

  private void doApply()
  {
    this.appliedImg = createImage();
    doClose();
  }


  private void doBlinkOldNew()
  {
    if( this.cbBlinkOldNew.isSelected() ) {
      this.blinkStateOld = true;
      doBlinkTimer();
      this.blinkTimer.start();
    } else {
      this.blinkTimer.stop();
      this.blinkStateOld = false;
      updPreview();
    }
  }


  private void doBlinkTimer()
  {
    this.imageFld.setImage(
		this.blinkStateOld ? this.orgImg : createImage() );
    this.imageFld.repaint();
    this.blinkStateOld = !this.blinkStateOld;
  }


  private void doColorAll()
  {
    int row = getSelectedModelRow();
    if( row >= 0 ) {
      Integer argb = this.tableModel.getChangedARGB( row );
      if( argb == null ) {
	argb = this.tableModel.getOrgARGB( row );
      }
      Color color = JColorChooser.showDialog(
				this,
				"Auswahl Farbe",
				argb != null ?
					new Color( argb.intValue() )
					: null );
      if( color != null ) {
	this.tableModel.setChangedARGB( row, color.getRGB() );
      }
    }
  }


  private void doColorSelect( IndexColorModel icm, String title )
  {
    if( icm != null ) {
      int row = getSelectedModelRow();
      if( row >= 0 ) {
	Integer argb = ARGBSelectDlg.showDlg( this, icm, title );
	if( argb != null ) {
	  this.tableModel.setChangedARGB( row, argb );
	}
      }
    }
  }


  private void doContextCopy()
  {
    if( this.tablePopupPoint != null ) {
      EmuUtil.Cell cell = EmuUtil.getModelCellAt(
					this.table,
					this.tablePopupPoint );
      if( cell != null ) {
	String text = this.tableModel.getTextAt( cell.row, cell.col );
	if( text != null ) {
	  if( !text.isEmpty() ) {
	    EmuUtil.copyToClipboard( this, text );
	  }
	}
      }
    }
  }


  private void doContextCut()
  {
    if( this.tablePopupPoint != null ) {
      EmuUtil.Cell cell = EmuUtil.getModelCellAt(
					this.table,
					this.tablePopupPoint );
      if( cell != null ) {
	if( this.tableModel.isCellEditable( cell.row, cell.col ) ) {
	  String text = this.tableModel.getTextAt( cell.row, cell.col );
	  if( text != null ) {
	    if( !text.isEmpty() ) {
	      EmuUtil.copyToClipboard( this, text );
	      this.tableModel.setValueAt( null, cell.row, cell.col );
	    }
	  }
	}
      }
    }
  }


  private void doContextPaste()
  {
    if( this.tablePopupPoint != null ) {
      EmuUtil.Cell cell = EmuUtil.getModelCellAt(
					this.table,
					this.tablePopupPoint );
      String text = EmuUtil.getClipboardText( this );
      if( (cell != null) && (text != null) ) {
	if( !text.isEmpty() ) {
	  this.tableModel.setValueAt( text, cell.row, cell.col );
	}
      }
    }
  }


  private void doExport() throws IOException
  {
    IndexColorModel icm = this.orgICM;
    if( this.tableModel.hasChangedARGBs() ) {
      switch( showOptionDlg(
		this,
		"M\u00F6chten Sie die urspr\u00FCngliche oder die"
			+ " ge\u00E4nderte Farbpalette exportieren?",
		"Farbpalette exportieren",
		"Urspr\u00FCngliche",
		"Ge\u00E4nderte",
		EmuUtil.TEXT_CANCEL ) )
      {
	case 0:
	  icm = this.orgICM;
	  break;
	case 1:
	  icm = this.tableModel.createIndexColorModel();
	  break;
	default:
	  icm = null;
      }
    }
    if( icm != null ) {
      File file = FileUtil.showFileSaveDlg(
			this,
			"Farbpalette exportieren",
			Main.getLastDirFile( Main.FILE_GROUP_IMAGE ),
			IFFFile.getPaletteFileFilter(),
			JASCPaletteFile.getFileFilter() );
      if( file != null ) {
	if( JASCPaletteFile.accept( file ) ) {
	  JASCPaletteFile.write( file, icm );
	} else if( IFFFile.accept( file ) ) {
	  IFFFile.writePalette( file, icm );
	} else {
	  throw new IOException(
			ImageUtil.createFileSuffixNotSupportedMsg(
					JASCPaletteFile.getFileSuffixes(),
					IFFFile.getFileSuffixes() ) );
	}
	Main.setLastFile( file, Main.FILE_GROUP_IMAGE );
      }
    }
  }


  private void doImportFile() throws IOException
  {
    Boolean override = checkConfirmOverride();
    if( override != null ) {
      File file = ImageUtil.chooseColorPaletteFile( this, lastPaletteFile );
      if( file != null ) {
	IndexColorModel icm = ImageUtil.readColorPaletteFile( file );
	if( icm == null ) {
	  ImageUtil.throwNoColorTabInFile();
	}
	importColorPalette( icm, override );
	lastPaletteFile  = file;
	this.importedICM = icm;
	this.mnuColorFile.setEnabled( true );
	this.mnuContextColorFile.setEnabled( true );
      }
    }
  }


	/* --- private Methoden --- */

  private Boolean checkConfirmOverride()
  {
    Boolean rv = null;
    if( this.tableModel.hasChangedARGBs() ) {
      switch( showOptionDlg(
		this,
		"Sollen die bereits ge\u00E4nderten Farbwerte"
			+ " \u00FCberschrieben werden?",
		"Best\u00E4tigung",
		"Ja",
		"Nein",
		EmuUtil.TEXT_CANCEL) )
      {
	case 0:
	  rv = Boolean.TRUE;
	  break;
	case 1:
	  rv = Boolean.FALSE;
	  break;
      }
    } else {
      rv = Boolean.TRUE;
    }
    return rv;
  }


  private BufferedImage createImage()
  {
    return new BufferedImage(
			this.tableModel.createIndexColorModel(),
			this.orgImg.getRaster(),
			false,
			new Hashtable<>() );
  }


  private int getSelectedModelRow()
  {
    int   rv   = -1;
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int row = this.table.convertRowIndexToModel( rows[ 0 ] );
	if( (row >= 0) && (row < this.tableModel.getRowCount()) ) {
	  rv = row;
	}
      }
    }
    return rv;
  }


  private void importColorPalette( IndexColorModel icm, Boolean override )
  {
    if( override == null ) {
      override = checkConfirmOverride();
    }
    if( override != null ) {
      int n = this.tableModel.getRowCount();
      for( int i = 0; i < n; i++ ) {
	if( override.booleanValue()
	    || (this.tableModel.getChangedARGB( i ) == null) )
	{
	  Integer orgARGB = this.tableModel.getOrgARGB( i );
	  if( orgARGB != null ) {
	    int idx = ImageUtil.getNearestIndex( icm, orgARGB.intValue() );
	    if( idx >= 0 ) {
	      this.tableModel.setChangedARGB(
			i,
			(orgARGB.intValue() & 0xFF000000)
				| (icm.getRGB( idx ) & 0x00FFFFFF) );
	    }
	  }
	}
      }
    }
  }


  private void updPreview()
  {
    this.imageFld.setImage( createImage() );
    this.imageFld.repaint();
  }
}
