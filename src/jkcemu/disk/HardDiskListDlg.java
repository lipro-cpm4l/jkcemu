/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Verzeichnisses
 */

package jkcemu.disk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.text.TextUtil;


public class HardDiskListDlg extends BaseDlg implements ListSelectionListener
{
  public static final String HARDDISKS_FILE = "harddisks.csv";

  private boolean            dataChangedInfoShown;
  private HardDiskInfo       selectedRow;
  private HardDiskTableModel tableModel;
  private ListSelectionModel selectionModel;
  private JTable             table;
  private JScrollPane        scrollPane;
  private JButton            btnSelect;
  private JButton            btnAdd;
  private JButton            btnRemove;
  private JButton            btnSave;
  private JButton            btnCancel;


  public static HardDiskInfo showHardDiskListDlg( Window owner )
  {
    HardDiskListDlg dlg = new HardDiskListDlg( owner );
    dlg.setVisible( true );
    return dlg.selectedRow;
  }


  public void setDataChanged()
  {
    this.btnSelect.setEnabled( false );
    this.btnSave.setEnabled( true );
    if( !this.dataChangedInfoShown ) {
      this.dataChangedInfoShown = true;
      BaseDlg.showInfoDlg(
		this,
		"Sie m\u00FCssen das Festplattenverzeichnis"
			+ " erst speichern,\n"
			+ "bevor Sie wieder eine Festplatte"
			+ " ausw\u00E4hlen k\u00F6nnen." );
    }
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    updActionButtons();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnSelect ) {
	rv = true;
	doSelect();
      }
      else if( src == this.btnAdd ) {
	rv = true;
	doAdd();
      }
      else if( src == this.btnRemove ) {
	rv = true;
	doRemove();
      }
      else if( src == this.btnSave ) {
	rv = true;
	doSave();
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( this.btnSave.isEnabled() ) {
      switch( JOptionPane.showConfirmDialog(
		this,
		"Das Festplattenverzeichnis wurde ge\u00E4ndert,"
			+ " aber nicht gespeichert.\n"
			+ "M\u00F6chten Sie es speichern,"
			+ " bevor das Fenster geschlossen wird?",
		"Daten ge\u00E4ndert",
		JOptionPane.YES_NO_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) )
      {
	case JOptionPane.YES_OPTION:
	  rv = doSave();
	  break;
	case JOptionPane.NO_OPTION:
	  rv = true;
	  break;
      }
    } else {
      rv = true;
    }
    if( rv ) {
      rv = super.doClose();
      if( rv ) {
	this.table.removeMouseListener( this );
	this.btnSelect.removeActionListener( this );
	this.btnAdd.removeActionListener( this );
	this.btnRemove.removeActionListener( this );
	this.btnSave.removeActionListener( this );
	this.btnCancel.removeActionListener( this );
	if( this.selectionModel != null ) {
	  this.selectionModel.removeListSelectionListener( this );
	}
      }
    }
    return rv;
  }


  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getSource() == this.table)
	&& (e.getButton() == MouseEvent.BUTTON1)
	&& (e.getClickCount() > 1) )
    {
      doSelect();
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    BufferedReader reader = null;
    try {
      try {
	File configDir = Main.getConfigDir();
	if( configDir != null ) {
	  File file = new File( configDir, HARDDISKS_FILE );
	  if( file.exists() ) {
	    reader = new BufferedReader( new FileReader( file ) );
	  }
	}
	if( reader == null ) {
	  InputStream in = getClass().getResourceAsStream(
					  "/disks/" + HARDDISKS_FILE );
	  if( in != null ) {
	    reader = new BufferedReader( new InputStreamReader( in ) );
	  }
	}
	if( reader != null ) {
	  String line = reader.readLine();
	  while( line != null ) {
	    try {
	      String[] a = line.split( "\t", 6 );
	      if( a != null ) {
		if( a.length >= 5 ) {
		  int c = Integer.parseInt( a[ 2 ] );
		  int h = Integer.parseInt( a[ 3 ] );
		  int n = Integer.parseInt( a[ 4 ] );
		  if( !a[ 1 ].isEmpty() && (c > 0) && (h > 0) && (n > 0) ) {
		    this.tableModel.addRow(
				new HardDiskInfo( a[ 0 ], a[ 1 ], c, h, n ) );
		  }
		}
	      }
	    }
	    catch( PatternSyntaxException ex ) {}
	    catch( NumberFormatException ex ) {}
	    line = reader.readLine();
	  }
	  this.tableModel.sort();
	  this.tableModel.fireTableDataChanged();
	}
      }
      finally {
	EmuUtil.closeSilently( reader );
      }
    }
    catch( IOException ex ) {
      String msg   = "Lesen der Festplattenstammdaten fehlgeschlagen";
      String exMsg = ex.getMessage();
      if( exMsg != null ) {
	if( !exMsg.isEmpty() ) {
	  msg = msg + ":\n" + exMsg;
	}
      }
      showErrorDlg( this, msg );
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private HardDiskListDlg( Window owner )
  {
    super( owner, "JKCEMU Festplattenverzeichnis" );
    this.dataChangedInfoShown = false;
    this.selectedRow          = null;


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


    // Tabelle
    this.tableModel = new HardDiskTableModel( this );
    this.table      = GUIFactory.createTable( this.tableModel );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setPreferredScrollableViewportSize( new Dimension( 560, 300 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setRowSorter( null );
    this.table.setShowGrid( false );
    this.table.setShowHorizontalLines( false );
    this.table.setShowVerticalLines( false );
    this.table.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    EmuUtil.setTableColWidths( this.table, 120, 120, 70, 70, 70, 100 );

    // ab der 3. Spalte rechtsbuendig
    TableColumnModel tcm = this.table.getColumnModel();
    if( tcm != null ) {
      DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
      renderer.setHorizontalAlignment( SwingConstants.RIGHT );

      int nCols = tcm.getColumnCount();
      for( int i = 2; i < nCols; i++ ) {
	TableColumn tc = tcm.getColumn( i );
	if( tc != null ) {
	  tc.setCellRenderer( renderer );
	}
      }
    }

    this.scrollPane = GUIFactory.createScrollPane(
			this.table,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
    add( this.scrollPane, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 5, 5, 5 ) );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.weighty     = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnSelect = GUIFactory.createButton( "Ausw\u00E4hlen" );
    panelBtn.add( this.btnSelect );

    this.btnAdd = GUIFactory.createButtonAdd();
    panelBtn.add( this.btnAdd );

    this.btnRemove = GUIFactory.createButtonRemove();
    panelBtn.add( this.btnRemove );

    this.btnSave = GUIFactory.createButtonSave();
    this.btnSave.setEnabled( false );
    panelBtn.add( this.btnSave );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );


    // Listener
    this.table.addMouseListener( this );
    this.btnSelect.addActionListener( this );
    this.btnAdd.addActionListener( this );
    this.btnRemove.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // sonstiges
    this.selectionModel = this.table.getSelectionModel();
    if( this.selectionModel != null ) {
      this.selectionModel.addListSelectionListener( this );
      updActionButtons();
    }
    updBgColor();
  }


  private void doAdd()
  {
    EmuUtil.stopCellEditing( this.table );

    Set<String> producers = new TreeSet<>();

    int nRows = this.tableModel.getRowCount();
    for( int i = 0; i < nRows; i++ ) {
      HardDiskInfo info = this.tableModel.getRow( i );
      if( info != null ) {
	String producer = TextUtil.emptyToNull( info.getProducer() );
	if( producer != null ) {
	  producers.add( producer );
	}
      }
    }
    HardDiskInfo data = HardDiskDetailsDlg.showHardDiskDetailsDlg(
						this,
						"Neue Festplatte",
						producers );
    if( data != null ) {
      int    modelRow = -1;
      String msg      = null;
      for( int i = 0; i < nRows; i++ ) {
	HardDiskInfo info = this.tableModel.getRow( i );
	if( info != null ) {
	  if( TextUtil.equals( data.getProducer(), info.getProducer() )
	      && TextUtil.equals( data.getDiskModel(), info.getDiskModel() ) )
	  {
	    if( (data.getCylinders() != info.getCylinders())
		|| (data.getHeads() != info.getHeads())
		|| (data.getSectorsPerTrack() != info.getSectorsPerTrack()) )
	    {
	      msg = "Die Festplatte wurde bereits erfasst,\n"
			+ "allerdings mit anderen Geometriedaten.\n"
			+ "Bei Bedarf k\u00F6nnen Sie die Geometriedaten\n"
			+ "direkt in der Tabelle \u00E4ndern.";
	    }
	    modelRow = i;
	  }
	}
      }
      if( modelRow < 0 ) {
	this.tableModel.addRow( data );
	this.tableModel.sort();
	modelRow = this.tableModel.indexOf( data );
	if( modelRow >= 0 ) {
	  this.tableModel.fireTableRowsInserted( modelRow, modelRow );
	} else {
	  this.tableModel.fireTableDataChanged();
	}
	setDataChanged();
      }
      if( modelRow >= 0 ) {
	EmuUtil.fireSelectRow(
		this.table,
		this.table.convertRowIndexToView( modelRow ) );
      }
      if( msg != null ) {
	BaseDlg.showErrorDlg( this, msg );
      }
    }
  }


  private void doRemove()
  {
    int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      for( int i = 0; i < rowNums.length; i++ ) {
	rowNums[ i ] = this.table.convertRowIndexToModel( rowNums[ i ] );
      }
      Arrays.sort( rowNums );
      for( int i = rowNums.length - 1; i >= 0; --i ) {
	int row = rowNums[ i ];
	this.tableModel.removeRow( row );
	this.tableModel.fireTableRowsDeleted( row, row );
	setDataChanged();
      }
    }
  }


  private boolean doSave()
  {
    EmuUtil.stopCellEditing( this.table );

    boolean rv        = false;
    File    configDir = Main.getConfigDir();
    if( configDir != null ) {
      try {
	Writer writer = null;
	try {
	  writer = new FileWriter( new File( configDir, HARDDISKS_FILE ) );

	  int n = this.tableModel.getRowCount();
	  for( int i = 0; i < n; i++ ) {
	    HardDiskInfo info = this.tableModel.getRow( i );
	    if( info != null ) {
	      String producer  = info.getProducer();
	      String diskModel = info.getDiskModel();
	      writer.write( String.format(
				"%s\t%s\t%d\t%d\t%d\n",
				producer != null ? producer : "",
				diskModel != null ? diskModel : "",
				info.getCylinders(),
				info.getHeads(),
				info.getSectorsPerTrack() ) );
	    }
	  }
	  writer.close();
	  writer = null;
	  rv     = true;
	  this.btnSave.setEnabled( false );
	  updActionButtons();
	}
	finally {
	  EmuUtil.closeSilently( writer );
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    } else {
      BaseDlg.showErrorDlg(
		this,
		"Speichern nicht m\u00F6glich, da das\n"
			+ "JKCEMU-Konfigurationsverzeichnis"
			+ " nicht bekannt ist." );
    }
    return rv;
  }


  private void doSelect()
  {
    EmuUtil.stopCellEditing( this.table );
    if( this.table.getSelectedRowCount() == 1 ) {
      int row = this.table.getSelectedRow();
      if( row >= 0 ) {
	row = this.table.convertRowIndexToModel( row );
	if( row >= 0 ) {
	  HardDiskInfo info = this.tableModel.getRow( row );
	  if( info != null ) {
	    this.selectedRow = info;
	    if( !doClose() ) {
	      this.selectedRow = null;		// Schliessen widerrufen
	    }
	  }
	}
      }
    }
  }


  private void updActionButtons()
  {
    int nRows = this.table.getSelectedRowCount();
    this.btnSelect.setEnabled( (nRows == 1) && !this.btnSave.isEnabled() );
    this.btnRemove.setEnabled( nRows > 0 );
  }


  private void updBgColor()
  {
    Color     color = this.table.getBackground();
    JViewport vp    = this.scrollPane.getViewport();
    if( (color != null) && (vp != null) ) {
      vp.setBackground( color );
    }
  }
}
