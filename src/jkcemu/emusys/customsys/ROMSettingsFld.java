/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die ROM-Einstellungen
 * des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.emusys.CustomSys;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class ROMSettingsFld
			extends AbstractSettingsFld
			implements
				ListSelectionListener,
				MouseListener
{
  private JButton            btnAdd;
  private JButton            btnEdit;
  private JButton            btnRemove;
  private JButton            btnUp;
  private JButton            btnDown;
  private JTable             table;
  private ROMTableModel      tableModel;
  private ListSelectionModel selModel;


  public ROMSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tableModel = new ROMTableModel();

    this.table = GUIFactory.createTable( this.tableModel );
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

    add( GUIFactory.createScrollPane( this.table ), gbc );

    EmuUtil.setTableColWidths( this.table, 100, 70, 300 );

    JPanel panelRomBtnRight = GUIFactory.createPanel(
					new GridLayout( 2, 1, 5, 5 ) );
    gbc.fill             = GridBagConstraints.NONE;
    gbc.weightx          = 0.0;
    gbc.weighty          = 0.0;
    gbc.gridx++;
    add( panelRomBtnRight, gbc );

    this.btnUp = GUIFactory.createRelImageResourceButton(
						this,
						"nav/up.png",
						"Auf" );
    this.btnUp.addActionListener( this );
    panelRomBtnRight.add( this.btnUp );

    this.btnDown = GUIFactory.createRelImageResourceButton(
						this,
						"nav/down.png",
						"Ab" );
    this.btnDown.addActionListener( this );
    panelRomBtnRight.add( this.btnDown );

    JPanel panelRomBtnBottom = GUIFactory.createPanel(
					new GridLayout( 1, 3, 5, 5 ) );
    gbc.gridx = 0;
    gbc.gridy++;
    add( panelRomBtnBottom, gbc );

    this.btnAdd = GUIFactory.createButtonAdd();
    this.btnAdd.addActionListener( this );
    panelRomBtnBottom.add( this.btnAdd );

    this.btnEdit = GUIFactory.createButtonEdit();
    this.btnEdit.addActionListener( this );
    panelRomBtnBottom.add( this.btnEdit );

    this.btnRemove = GUIFactory.createButtonRemove();
    this.btnRemove.addActionListener( this );
    panelRomBtnBottom.add( this.btnRemove );

    this.selModel = this.table.getSelectionModel();
    if( this.selModel != null ) {
      this.selModel.addListSelectionListener( this );
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
    if( e.getSource() == this.selModel ) {
      int     nRows     = this.table.getRowCount();
      int     nSelRows  = this.table.getSelectedRowCount();
      int     selRowNum = this.table.getSelectedRow();
      boolean stateOne  = (nSelRows == 1) && (selRowNum >= 0);
      boolean stateEdit = false;
      this.btnUp.setEnabled( (nSelRows == 1) && (selRowNum > 0) );
      this.btnDown.setEnabled( stateOne && (selRowNum < (nRows - 1)) );
      this.btnRemove.setEnabled( nSelRows > 0 );
      if( stateOne ) {
	int row = this.table.convertRowIndexToModel( selRowNum );
	if( row >= 0 ) {
	  CustomSysROM rom = this.tableModel.getRow( row );
	  if( rom != null ) {
	    stateEdit = true;
	  }
	}
      }
      this.btnEdit.setEnabled( stateEdit );
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
		boolean    selected ) throws
					UserCancelException,
					UserInputException
  {
    boolean overlap   = false;
    boolean at0000    = false;
    int     nBootROMs = 0;
    int     nROMs     = this.tableModel.getRowCount();
    if( selected ) {
      for( int i = 0; i < nROMs; i++ ) {
	CustomSysROM rom = this.tableModel.getRow( i );
	if( rom != null ) {
	  if( rom.getBegAddr() == 0x0000 ) {
	    at0000 = true;
	  }
	  if( rom.isBootROM() && rom.isEnabledAfterReset()) {
	    nBootROMs++;
	  }
	  for( int k = i + 1; k < nROMs; k++ ) {
	    int          addr1 = rom.getBegAddr();
	    int          size1 = rom.getSize();
	    CustomSysROM rom2  = this.tableModel.getRow( k );
	    if( rom2 != null ) {
	      int addr2 = rom2.getBegAddr();
	      int size2 = rom2.getSize();
	      if( ((addr1 <= addr2) && ((addr1 + size1) > addr2))
		  || ((addr2 <= addr1) && ((addr2 + size2) > addr1)) )
	      {
		overlap = true;
		break;
	      }
	    }
	  }
	}
      }
      if( overlap ) {
	if( !BaseDlg.showConfirmDlg(
		this.settingsFrm,
		"ROM-Bereiche \u00FCberlappen sich.\n"
			+ "Im Fall einer \u00DCberlappung ist der"
			+ " ROM-Bereich relevant,\n"
			+ "der in der Liste weiter oben steht." ) )
	{
	  throw new UserCancelException();
	}
      }
      if( !at0000 && (nBootROMs == 0) ) {
	if( !BaseDlg.showConfirmDlg(
		this.settingsFrm,
		"An der Adesse 0000h befindet sich kein ROM\n"
			+ "und es ist auch kein Boot-ROM markiert.\n"
			+ "Nach RESET beginnt die Programmausf\u00FChrung"
			+ " somit im RAM!" ) )
	{
	  throw new UserCancelException();
	}
      }
    }
    int romIdx = 0;
    for( int i = 0; i < nROMs; i++ ) {
      CustomSysROM rom = this.tableModel.getRow( i );
      if( rom != null ) {
	String fileName = rom.getFileName();
	String prefix   = String.format(
				"%s%s%d.",
				this.propPrefix,
				CustomSys.PROP_ROM_PREFIX,
				romIdx++ );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_BEGADDR,
			rom.getBegAddr() );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SIZE,
			rom.getSize() );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_FILE,
			fileName != null ? fileName : "" );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SWITCH_IOADDR,
			rom.getSwitchIOAddr() );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SWITCH_IOMASK,
			rom.getSwitchIOMask() );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SWITCH_IOVALUE,
			rom.getSwitchIOValue() );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_ENABLE_ON_RESET,
			rom.getEnableOnReset() );
	EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_BOOT,
			rom.isBootROM() );
      }
    }
    EmuUtil.setProperty(
		props,
		this.propPrefix
			+ CustomSys.PROP_ROM_PREFIX
			+ CustomSys.PROP_COUNT,
		romIdx );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    this.settingsFrm.setWaitCursor( true );

    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnAdd ) {
	rv = true;
	doAdd();
      }
      else if( src == this.btnEdit ) {
	rv = true;
	doEdit();
      }
      else if( src == this.btnRemove ) {
	rv = true;
	doRemove();
      }
      else if( src == this.btnUp ) {
	rv = true;
	doMove( -1 );
      }
      else if( src == this.btnDown ) {
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
    this.tableModel.setRows( CustomSys.getDeclaredROMs( props ) );
  }


	/* --- Aktionen --- */

  private void doAdd()
  {
    boolean hasBootROM = false;
    int     nRoms      = this.tableModel.getRowCount();
    for( int i = 0; i < nRoms; i++ ) {
      CustomSysROM rom = this.tableModel.getRow( i );
      if( rom != null ) {
	if( rom.isBootROM() && rom.isEnabledAfterReset()) {
	  hasBootROM = true;
	  break;
	}
      }
    }
    CustomSysROM rom = ROMSettingsDlg.showNewROMDlg(
						this.settingsFrm,
						!hasBootROM );
    if( rom != null ) {
      this.tableModel.addRow( rom );
      fireDataChanged();
    }
  }


  private void doEdit()
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int          row = this.table.convertRowIndexToModel( rows[ 0 ] );
	CustomSysROM rom = this.tableModel.getRow( row );
	if( rom != null ) {
	  rom = ROMSettingsDlg.showDlg( this.settingsFrm, rom );
	  if( rom != null ) {
	    this.tableModel.setRow( row, rom );
	    fireDataChanged();
	  }
	}
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
	  CustomSysROM rom1 = this.tableModel.getRow( row1 );
	  CustomSysROM rom2 = this.tableModel.getRow( row2 );
	  if( (rom1 != null) && (rom2 != null) ) {
	    this.tableModel.setRow( row1, rom2 );
	    this.tableModel.setRow( row2, rom1 );
	    EmuUtil.fireSelectRow( this.table, row2 );
	    fireDataChanged();
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
}
