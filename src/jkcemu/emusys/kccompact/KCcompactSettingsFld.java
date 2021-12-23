/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KC-compact-Einstellungen
 */

package jkcemu.emusys.kccompact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.emusys.KCcompact;
import jkcemu.file.FileUtil;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.SettingsFrm;


public class KCcompactSettingsFld
			extends AbstractSettingsFld
			implements
				DropTargetListener,
				ListSelectionListener,
				MouseListener
{
  private JTabbedPane          tabbedPane;
  private JButton              btnExtRomAdd;
  private JButton              btnExtRomEdit;
  private JButton              btnExtRomRemove;
  private JCheckBox            cbFDC;
  private JCheckBox            cbExtRAM512K;
  private JCheckBox            cbFixedScreenSize;
  private JPanel               tabExtROM;
  private JPanel               tabEtc;
  private JTable               tableExtROM;
  private ExtROMTableModel     tableModelExtROM;
  private ROMFileSettingsFld   fldAltOS;
  private ROMFileSettingsFld   fldAltBasic;
  private AutoInputSettingsFld tabAutoInput;


  public KCcompactSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    setLayout( new BorderLayout() );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab ROM
    this.tabExtROM = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM-Erweiterungen", this.tabExtROM );

    GridBagConstraints gbcROM = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tableModelExtROM = new ExtROMTableModel();
    this.tableExtROM      = GUIFactory.createTable( this.tableModelExtROM );
    this.tableExtROM.setAutoCreateRowSorter( false );
    this.tableExtROM.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.tableExtROM.setColumnSelectionAllowed( false );
    this.tableExtROM.setDragEnabled( false );
    this.tableExtROM.setFillsViewportHeight( false );
    this.tableExtROM.setPreferredScrollableViewportSize(
						new Dimension( 1, 1 ) );
    this.tableExtROM.setRowSelectionAllowed( true );
    this.tableExtROM.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

    JScrollPane spExtROM = GUIFactory.createScrollPane( this.tableExtROM );
    this.tabExtROM.add( spExtROM, gbcROM );

    EmuUtil.setTableColWidths( this.tableExtROM, 60, 400 );
    this.tableExtROM.addMouseListener( this );

    JPanel panelRomBtn = GUIFactory.createPanel(
					new GridLayout( 1, 3, 5, 5 ) );
    gbcROM.fill        = GridBagConstraints.NONE;
    gbcROM.weightx     = 0.0;
    gbcROM.weighty     = 0.0;
    gbcROM.gridy++;
    this.tabExtROM.add( panelRomBtn, gbcROM );

    this.btnExtRomAdd = GUIFactory.createButtonAdd();
    this.btnExtRomAdd.addActionListener( this );
    this.btnExtRomAdd.addKeyListener( this );
    panelRomBtn.add( this.btnExtRomAdd );

    this.btnExtRomEdit = GUIFactory.createButtonEdit();
    this.btnExtRomEdit.addActionListener( this );
    this.btnExtRomEdit.addKeyListener( this );
    panelRomBtn.add( this.btnExtRomEdit );

    this.btnExtRomRemove = GUIFactory.createButtonRemove();
    this.btnExtRomRemove.addActionListener( this );
    this.btnExtRomRemove.addKeyListener( this );
    panelRomBtn.add( this.btnExtRomRemove );

    ListSelectionModel lsm = this.tableExtROM.getSelectionModel();
    if( lsm != null ) {
      lsm.addListSelectionListener( this );
      this.btnExtRomEdit.setEnabled( false );
      this.btnExtRomRemove.setEnabled( false );
    }
    (new DropTarget( spExtROM, this )).setActive( true );


    // Tab Sonstiges
    this.tabEtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbFDC = GUIFactory.createCheckBox( "Floppy-Disk-Station" );
    this.cbFDC.addActionListener( this );
    this.tabEtc.add( this.cbFDC, gbcEtc );

    this.cbExtRAM512K = GUIFactory.createCheckBox(
		"512 KByte RAM-Erweiterung"
			+ " (2 x DK\'tronics 256K Memory Expansion)" );
    this.cbExtRAM512K.addActionListener( this );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbExtRAM512K, gbcEtc );

    this.cbFixedScreenSize = GUIFactory.createCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in allen Bildschirmmodi" );
    this.cbFixedScreenSize.addActionListener( this );
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbFixedScreenSize, gbcEtc );

    gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx       = 1.0;
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( GUIFactory.createSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + KCcompact.PROP_OS_PREFIX,
				"Alternativer Betriebssystem-ROM:" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltBasic = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + KCcompact.PROP_BASIC_PREFIX,
				"Alternativer BASIC-ROM:" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltBasic, gbcEtc );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		KCcompact.getAutoInputCharSet(),
		KCcompact.DEFAULT_SWAP_KEY_CHAR_CASE,
		KCcompact.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );
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
    final File file = FileUtil.fileDrop( this, e );
    if( file != null ) {
      // nicht auf Benutzerinteraktion warten
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    doExtRomAdd( file.getPath() );
			  }
			} );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    boolean state = (this.tableExtROM.getSelectedRowCount() > 0);
    this.btnExtRomEdit.setEnabled( state );
    this.btnExtRomRemove.setEnabled( state );
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.tableExtROM)
	&& (e.getButton() == MouseEvent.BUTTON1)
	&& (e.getClickCount() > 1) )
    {
      e.consume();
      doExtRomEdit();
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
    Component tab = null;
    try {

      // Tab ROM-Erweiterungen
      tab = this.tabExtROM;
      EmuUtil.removePropertiesByPrefix(
		props, 
		this.propPrefix + KCcompact.PROP_EXT_ROM_PREFIX );
      int nExtROMs = this.tableModelExtROM.getRowCount();
      for( int i = 0; i < nExtROMs; i++ ) {
	ExtROM extROM = this.tableModelExtROM.getRow( i );
	EmuUtil.setProperty(
		props,
		KCcompact.getExtRomFilePropName( extROM.getRomNum() ),
		extROM.getFileName() );
      }

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + KCcompact.PROP_FDC_ENABLED,
		this.cbFDC.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + KCcompact.PROP_EXT_RAM_512K,
		this.cbExtRAM512K.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + KCcompact.PROP_FIXED_SCREEN_SIZE,
		this.cbFixedScreenSize.isSelected() );
      this.fldAltOS.applyInput( props, selected );
      this.fldAltBasic.applyInput( props, selected );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );
    }
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnExtRomAdd ) {
	rv = true;
	doExtRomAdd( null );
      } else if( src == this.btnExtRomEdit ) {
	rv = true;
	doExtRomEdit();
      } else if( src == this.btnExtRomRemove ) {
	rv = true;
	doExtRomRemove();
      } else {
	rv = this.tabAutoInput.doAction( e );
      }
      if( !rv && (src instanceof AbstractButton) ) {
	fireDataChanged();
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.tableModelExtROM.clear();
    for( int romNum = 1; romNum < 16; romNum++ ) {
      String fileName = EmuUtil.getProperty(
				props,
				KCcompact.getExtRomFilePropName( romNum ) );
      if( !fileName.isEmpty() ) {
	this.tableModelExtROM.addRow( new ExtROM( romNum, fileName ) );
      }
    }

    this.cbFDC.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KCcompact.PROP_FDC_ENABLED,
			false ) );
    this.cbExtRAM512K.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KCcompact.PROP_EXT_RAM_512K,
			false ) );
    this.cbFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KCcompact.PROP_FIXED_SCREEN_SIZE,
			false ) );

    this.fldAltOS.updFields( props );
    this.fldAltBasic.updFields( props );
    this.tabAutoInput.updFields( props );
  }


	/* --- private Methoden --- */

  private void doExtRomAdd( String fileName )
  {
    int romNum = 1;
    int nRoms  = this.tableModelExtROM.getRowCount();
    if( nRoms > 0 ) {
      romNum = Math.min(
		this.tableModelExtROM.getRow( nRoms - 1 ).getRomNum() + 1,
		15 );
    }
    setExtROM(
	ExtROMSettingsDlg.showDlg(
			this.settingsFrm,
			new ExtROM( romNum, fileName ) ) );
  }


  private void doExtRomEdit()
  {
    int[] rows = this.tableExtROM.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	setExtROM( ExtROMSettingsDlg.showDlg(
			this.settingsFrm,
			this.tableModelExtROM.getRow(
				this.tableExtROM.convertRowIndexToModel(
							rows[ 0 ] ) ) ) );
      }
    }
  }


  private void doExtRomRemove()
  {
    int[] rows = this.tableExtROM.getSelectedRows();
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  this.tableModelExtROM.removeRow(
		this.tableExtROM.convertRowIndexToModel( rows[ 0 ] ) );
	}
	fireDataChanged();
      }
    }
  }


  private void setExtROM( ExtROM extROM )
  {
    if( extROM != null ) {
      boolean done  = false;
      int     nRows = this.tableModelExtROM.getRowCount();
      for( int i = 0; i < nRows; i++ ) {
	ExtROM tmpROM = this.tableModelExtROM.getRow( i );
	if( tmpROM != null ) {
	  if( tmpROM.getRomNum() == extROM.getRomNum() ) {
	    this.tableModelExtROM.setRow( i, extROM );
	    fireDataChanged();
	    done = true;
	    break;
	  }
	}
      }
      if( !done ) {
	this.tableModelExtROM.addRow( extROM );
	fireDataChanged();
      }
      int rowNum = this.tableModelExtROM.getRowNumByRomNum(
						extROM.getRomNum() );
      if( rowNum >= 0 ) {
	rowNum = this.tableExtROM.convertRowIndexToView( rowNum );
	if( rowNum >= 0 ) {
	  EmuUtil.fireSelectRow( this.tableExtROM, rowNum );
	}
      }
    }
  }
}
