/*
 * (c) 2015-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen zur automatischen Tastatureingabe
 */

package jkcemu.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.AutoInputEntry;
import jkcemu.base.AutoInputWorker;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;


public class AutoInputSettingsFld
			extends AbstractSettingsFld
			implements
				ListSelectionListener,
				MouseListener
{
  private static final int DEFAULT_MILLIS_TO_WAIT = 200;

  private boolean             swapKeyCharCase;
  private int                 defaultFirstMillisToWait;
  private AutoInputCharSet    charSet;
  private AutoInputTableModel tableModel;
  private JTable              table;
  private JScrollPane         scrollPane;
  private ListSelectionModel  selectionModel;
  private JButton             btnAdd;
  private JButton             btnEdit;
  private JButton             btnRemove;
  private JButton             btnUp;
  private JButton             btnDown;


  public AutoInputSettingsFld(
		SettingsFrm      settingsFrm,
		String           propPrefix,
		AutoInputCharSet charSet,
		boolean          swapKeyCharCase,
		int              defaultFirstMillisToWait )
  {
    super( settingsFrm, propPrefix + AutoInputWorker.PROP_AUTOINPUT_PREFIX );
    this.charSet                  = charSet;
    this.swapKeyCharCase          = swapKeyCharCase;
    this.defaultFirstMillisToWait = defaultFirstMillisToWait;
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
	GUIFactory.createLabel( "Tastatureingaben, die nach"
			+ " dem Einschalten bzw. nach RESET" ),
	gbc );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add(
	GUIFactory.createLabel( "automatisch get\u00E4tigt werden sollen:" ),
	gbc );

    this.tableModel = new AutoInputTableModel( charSet );
    this.table      = GUIFactory.createTable( this.tableModel );
    this.table.setAutoCreateRowSorter( false );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setDragEnabled( false );
    this.table.setFillsViewportHeight( false );
    this.table.setPreferredScrollableViewportSize( new Dimension( 1, 1 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    EmuUtil.setTableColWidths( this.table, 100, 200, 300 );

    this.scrollPane = GUIFactory.createScrollPane( this.table );
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.gridy++;
    add( this.scrollPane, gbc );

    JPanel panelBtnRight = GUIFactory.createPanel(
					new GridLayout( 2, 1, 5, 5 ) );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtnRight, gbc );

    this.btnUp = GUIFactory.createRelImageResourceButton(
						this,
						"nav/up.png",
						"Auf" );
    panelBtnRight.add( this.btnUp );

    this.btnDown = GUIFactory.createRelImageResourceButton(
						this,
						"nav/down.png",
						"Ab" );
    panelBtnRight.add( this.btnDown );

    JPanel panelBtnBottom = GUIFactory.createPanel(
					new GridLayout( 1, 3, 5, 5 ) );
    gbc.gridx = 0;
    gbc.gridy++;
    add( panelBtnBottom, gbc );

    this.btnAdd = GUIFactory.createButtonAdd();
    panelBtnBottom.add( this.btnAdd );

    this.btnEdit = GUIFactory.createButtonEdit();
    panelBtnBottom.add( this.btnEdit );

    this.btnRemove = GUIFactory.createButtonRemove();
    panelBtnBottom.add( this.btnRemove );

    // Listener
    this.table.addMouseListener( this );
    this.btnUp.addActionListener( this );
    this.btnDown.addActionListener( this );
    this.btnAdd.addActionListener( this );
    this.btnEdit.addActionListener( this );
    this.btnRemove.addActionListener( this );
    this.selectionModel = this.table.getSelectionModel();
    if( this.selectionModel != null ) {
      this.selectionModel.addListSelectionListener( this );
      this.btnUp.setEnabled( false );
      this.btnDown.setEnabled( false );
      this.btnEdit.setEnabled( false );
      this.btnRemove.setEnabled( false );
    }
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
      AutoInputEntry entry = this.tableModel.getRow( i );
      if( entry != null ) {
	try {
	  String prefix = String.format( "%s%d.", this.propPrefix, i );
	  EmuUtil.setProperty(
			props,
			prefix + AutoInputEntry.PROP_WAIT_MILLIS,
			String.valueOf( entry.getMillisToWait() ) );
	  EmuUtil.setProperty(
			props,
			prefix + AutoInputEntry.PROP_INPUT_TEXT,
			URLEncoder.encode( entry.getInputText(), "UTF-8" ) );
	  EmuUtil.setProperty(
			props,
			prefix + AutoInputEntry.PROP_REMARK,
			entry.getRemark() );
	}
	catch( UnsupportedEncodingException ex ) {}
      }
    }
    props.setProperty(
		this.propPrefix + Main.PROP_COUNT,
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
  public void updFields( Properties props )
  {
    this.tableModel.clear();
    java.util.List<AutoInputEntry> entries = AutoInputEntry.readEntries(
							props,
							this.propPrefix );
    if( entries != null ) {
      this.tableModel.addRows( entries );
    }
  }


	/* --- Aktionen --- */

  private void doAdd()
  {
    int millisToWait = this.defaultFirstMillisToWait;
    if( (this.tableModel.getRowCount() > 0)
	&& (millisToWait > DEFAULT_MILLIS_TO_WAIT) )
    {
      millisToWait = DEFAULT_MILLIS_TO_WAIT;
    }
    AutoInputEntry entry = AutoInputEntryDlg.openNewEntryDlg(
						this.settingsFrm,
						this.charSet,
						this.swapKeyCharCase,
						millisToWait );
    if( entry != null ) {
      int nRows = this.tableModel.getRowCount();
      this.tableModel.addRow( entry );
      int viewRow = this.table.convertRowIndexToView( nRows );
      if( viewRow >= 0 ) {
	EmuUtil.fireSelectRow( this.table, viewRow );
      }
      fireDataChanged();
    }
  }


  private void doEdit()
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int modelRow = this.table.convertRowIndexToModel( rows[ 0 ] );
	if( modelRow >= 0 ) {
	  AutoInputEntry oldEntry = this.tableModel.getRow( modelRow );
	  if( oldEntry != null ) {
	    AutoInputEntry newEntry = AutoInputEntryDlg.openEditEntryDlg(
						this.settingsFrm,
						this.charSet,
						this.swapKeyCharCase,
						oldEntry );
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
	  AutoInputEntry rowData1 = this.tableModel.getRow( row1 );
	  AutoInputEntry rowData2 = this.tableModel.getRow( row2 );
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
}
