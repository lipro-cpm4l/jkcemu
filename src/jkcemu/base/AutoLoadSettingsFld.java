/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen zum automatischen Laden
 * von Dateien in den Arbeitsspeicher
 */

package jkcemu.base;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;


public class AutoLoadSettingsFld
			extends AbstractSettingsFld
			implements
				ListSelectionListener,
				MouseListener
{
  private int                defaultFirstMillisToWait;
  private boolean            checkAddrs;
  private AutoLoadTableModel tableModel;
  private JTable             table;
  private JScrollPane        scrollPane;
  private ListSelectionModel selectionModel;
  private JButton            btnAdd;
  private JButton            btnEdit;
  private JButton            btnRemove;
  private JButton            btnUp;
  private JButton            btnDown;


  public AutoLoadSettingsFld(
		SettingsFrm settingsFrm,
		String      propPrefix,
		int         defaultFirstMillisToWait,
		boolean     checkAddrs )
  {
    super( settingsFrm, propPrefix + "autoload.");
    this.defaultFirstMillisToWait = defaultFirstMillisToWait;
    this.checkAddrs               = checkAddrs;
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add(
	new JLabel( "Dateien, die nach dem Einschalten bzw. nach RESET" ),
	gbc );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add(
	new JLabel( "automatisch in den Arbeitsspeicher"
			+ " geladen werden sollen:" ),
	gbc );

    this.tableModel = new AutoLoadTableModel();
    this.table      = new JTable( this.tableModel );
    this.table.setAutoCreateRowSorter( false );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setDragEnabled( false );
    this.table.setFillsViewportHeight( false );
    this.table.setPreferredScrollableViewportSize( new Dimension( 1, 1 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.table.addMouseListener( this );
    EmuUtil.setTableColWidths( this.table, 100, 100, 400 );

    this.scrollPane = new JScrollPane( this.table );
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.gridy++;
    add( this.scrollPane, gbc );

    JPanel panelBtnRight = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtnRight, gbc );

    this.btnUp = createImageButton( "/images/nav/up.png", "Auf" );
    panelBtnRight.add( this.btnUp );

    this.btnDown = createImageButton( "/images/nav/down.png", "Ab" );
    panelBtnRight.add( this.btnDown );

    JPanel panelBtnBottom = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbc.gridx = 0;
    gbc.gridy++;
    add( panelBtnBottom, gbc );

    this.btnAdd = new JButton( "Hinzuf\u00FCgen" );
    this.btnAdd.addActionListener( this );
    this.btnAdd.addKeyListener( this );
    panelBtnBottom.add( this.btnAdd );

    this.btnEdit = new JButton( "Bearbeiten" );
    this.btnEdit.addActionListener( this );
    this.btnEdit.addKeyListener( this );
    panelBtnBottom.add( this.btnEdit );

    this.btnRemove = new JButton( "Entfernen" );
    this.btnRemove.addActionListener( this );
    this.btnRemove.addKeyListener( this );
    panelBtnBottom.add( this.btnRemove );

    this.selectionModel = this.table.getSelectionModel();
    if( this.selectionModel != null ) {
      this.selectionModel.addListSelectionListener( this );
      this.btnUp.setEnabled( false );
      this.btnDown.setEnabled( false );
      this.btnEdit.setEnabled( false );
      this.btnRemove.setEnabled( false );
    }

    // Drag&Drop ermoeglichen
    (new DropTarget( this.table, this )).setActive( true );
    (new DropTarget( this.scrollPane, this )).setActive( true );
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.selectionModel ) {
      int     nRows     = this.table.getRowCount();
      int     nSelRows  = this.table.getSelectedRowCount();
      int     selRowNum = this.table.getSelectedRow();
      boolean stateOne  = (nSelRows == 1) && (selRowNum >= 0);
      this.btnUp.setEnabled( (nSelRows == 1) && (selRowNum > 0) );
      this.btnDown.setEnabled( stateOne && (selRowNum < (nRows - 1)) );
      this.btnEdit.setEnabled( stateOne );
      this.btnRemove.setEnabled( nSelRows > 0 );
    }
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getButton() == MouseEvent.BUTTON1)
	&& (e.getClickCount() > 1)
	&& (e.getComponent() == this.table) )
    {
      doEdit();
      e.consume();
    }
  }


  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    int nRows = this.tableModel.getRowCount();
    for( int i = 0; i < nRows; i++ ) {
      AutoLoadEntry entry = this.tableModel.getRow( i );
      if( entry != null ) {
	String prefix = this.propPrefix + String.valueOf( i );
	EmuUtil.setProperty(
			props,
			prefix + ".wait.millis",
			String.valueOf( entry.getMillisToWait() ) );
	EmuUtil.setProperty(
			props,
			prefix + ".file",
			entry.getFileName() );
	EmuUtil.setProperty(
			props,
			prefix + ".address.load",
			AutoLoadTableModel.toHex4( entry.getLoadAddr() ) );
      }
    }
    props.setProperty(
		this.propPrefix + "count",
		Integer.toString( nRows ) );

  }


  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv = false;
    this.settingsFrm.setWaitCursor( true );

    Object src = e.getSource();
    if( src != null ) {
      if( src == this.btnAdd ) {
	rv = true;
	doAdd();
      } else if( src == this.btnEdit ) {
	rv = true;
	doEdit();
      } else if( src == this.btnRemove ) {
	rv = true;
	doRemove();
      } else if( src == this.btnUp ) {
	rv = true;
	doMove( -1 );
      } else if( src == this.btnDown ) {
	rv = true;
	doMove( 1 );
      }
    }

    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  protected boolean fileDropped( Component c, File file )
  {
    boolean rv = false;
    if( (c == this.table) || (c == this.scrollPane) ) {
      addFile( file );
      rv = true;
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.tableModel.clear();
    java.util.List<AutoLoadEntry> entries = AutoLoadEntry.readEntries(
							props,
							this.propPrefix );
    if( entries != null ) {
      this.tableModel.addRows( entries );
    }
  }


	/* --- Aktionen --- */

  private void doAdd()
  {
    File file = EmuUtil.showFileOpenDlg(
				this.settingsFrm,
				"Datei ausw\u00E4hlen",
				Main.getLastDirFile( "software" ),
				EmuUtil.getBinaryFileFilter(),
				EmuUtil.getBasicFileFilter(),
				EmuUtil.getKCSystemFileFilter(),
				EmuUtil.getKCBasicFileFilter(),
				EmuUtil.getTapeFileFilter(),
				EmuUtil.getHeadersaveFileFilter(),
				EmuUtil.getHexFileFilter() );
    if( file != null ) {
      addFile( file );
    }
  }


  private void doEdit()
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int modelRow = this.table.convertRowIndexToModel( rows[ 0 ] );
	if( modelRow >= 0 ) {
	  AutoLoadEntry oldEntry = this.tableModel.getRow( modelRow );
	  if( oldEntry != null ) {
	    AutoLoadEntry newEntry = AutoLoadEntryDlg.openEditEntryDlg(
							this.settingsFrm,
							oldEntry,
							this.checkAddrs );
	    if( newEntry != null ) {
	      this.tableModel.setRow( modelRow, newEntry );
	      fireDataChanged();
	    }
	  }
	}
      }
    }
  }


  private void doRemove()
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  int row = this.table.convertRowIndexToModel( rows[ i ] );
	  if( row >= 0 ) {
	    this.tableModel.removeRow( row );
	  }
	}
	fireDataChanged();
      }
    }
  }


  private void doMove( int diffRows )
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int nRows = this.tableModel.getRowCount();
	int row1  = rows[ 0 ];
	int row2  = row1 + diffRows;
	if( (row1 >= 0) && (row1 < nRows)
	    && (row2 >= 0) && (row2 < nRows) )
	{
	  AutoLoadEntry rowData1 = this.tableModel.getRow( row1 );
	  AutoLoadEntry rowData2 = this.tableModel.getRow( row2 );
	  if( (rowData1 != null) && (rowData2 != null) ) {
	    this.tableModel.setRow( row1, rowData2 );
	    this.tableModel.setRow( row2, rowData1 );
	    EmuUtil.fireSelectRow( this.table, row2 );
	    fireDataChanged();
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private boolean addFile( File file )
  {
    int nRows = this.tableModel.getRowCount();
    AutoLoadEntry entry = AutoLoadEntryDlg.openNewEntryDlg(
				this.settingsFrm,
				file,
				nRows > 0 ? 0 : defaultFirstMillisToWait,
				this.checkAddrs );
    if( entry != null ) {
      this.tableModel.addRow( entry );
      int viewRow = this.table.convertRowIndexToView( nRows );
      if( viewRow >= 0 ) {
	EmuUtil.fireSelectRow( this.table, viewRow );
      }
      Main.setLastFile( file, "software" );
      fireDataChanged();
    }
    return entry != null;
  }
}
