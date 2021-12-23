/*
 * (c) 2017-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl einer Farbe
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.IndexColorModel;
import java.util.EventObject;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class ARGBSelectDlg
		extends BaseDlg
		implements ListSelectionListener
{
  private Integer                appliedARGB;
  private ColorPaletteTableModel tableModel;
  private ListSelectionModel     selModel;
  private JTable                 table;
  private JButton                btnSelect;
  private JButton                btnCancel;


  public static Integer showDlg(
				Window          owner,
				IndexColorModel icm,
				String          title )
  {
    ARGBSelectDlg dlg = new ARGBSelectDlg( owner, icm, title );
    dlg.setVisible( true );
    return dlg.appliedARGB;
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.selModel )
      this.btnSelect.setEnabled( this.table.getSelectedRow() >= 0 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnSelect ) {
      rv = true;
      doSelect();
    }
    else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      if( this.selModel != null ) {
	this.selModel.removeListSelectionListener( this );
      }
      this.btnSelect.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ARGBSelectDlg( Window owner, IndexColorModel icm, String title )
  {
    super( owner, title );
    this.appliedARGB = null;


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

    this.tableModel = new ColorPaletteTableModel( icm, false );
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
					new Dimension( 190, 280 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    add( GUIFactory.createScrollPane( this.table ), gbc );

    EmuUtil.setTableColWidths( this.table, 30, 110, 50 );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0;
    gbc.weighty     = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnSelect = GUIFactory.createButton( EmuUtil.TEXT_SELECT );
    panelBtn.add( this.btnSelect );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.selModel = this.table.getSelectionModel();
    if( this.selModel != null ) {
      this.selModel.addListSelectionListener( this );
      this.btnSelect.setEnabled( false );
    }
    this.btnSelect.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- Aktionen --- */

  private void doSelect()
  {
    int row = this.table.getSelectedRow();
    if( row >= 0 ) {
      row = this.table.convertRowIndexToModel( row );
      if( row >= 0 ) {
	this.appliedARGB = this.tableModel.getOrgARGB( row );
	if( this.appliedARGB != null ) {
	  doClose();
	}
      }
    }
  }
}
