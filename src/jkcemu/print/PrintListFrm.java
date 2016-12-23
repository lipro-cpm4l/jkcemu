/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Liste der Druckauftraege
 */

package jkcemu.print;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.util.Arrays;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;
import jkcemu.tools.hexedit.HexEditFrm;


public class PrintListFrm extends BaseFrm implements ListSelectionListener
{
  private static final String HELP_PAGE = "/help/print.htm";

  private static PrintListFrm instance = null;

  private ScreenFrm   screenFrm;
  private PrintMngr   printMngr;
  private JMenuItem   mnuFileFinish;
  private JMenuItem   mnuFilePrintOptions;
  private JMenuItem   mnuFilePrint;
  private JMenuItem   mnuFileOpenText;
  private JMenuItem   mnuFileOpenHex;
  private JMenuItem   mnuFileSave;
  private JMenuItem   mnuFileDelete;
  private JMenuItem   mnuFileClose;
  private JMenuItem   mnuHelpContent;
  private JPopupMenu  mnuPopup;
  private JMenuItem   mnuPopupFinish;
  private JMenuItem   mnuPopupPrint;
  private JMenuItem   mnuPopupOpenText;
  private JMenuItem   mnuPopupOpenHex;
  private JMenuItem   mnuPopupSave;
  private JMenuItem   mnuPopupDelete;
  private JButton     btnPrint;
  private JButton     btnOpenText;
  private JButton     btnSave;
  private JButton     btnDelete;
  private JTable      table;
  private JScrollPane scrollPane;


  public static void open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new PrintListFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
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
      if( (src == this.mnuFileFinish) || (src == this.mnuPopupFinish) ) {
	rv = true;
	doFinish();
      }
      else if( src == this.mnuFilePrintOptions ) {
	rv = true;
	PrintOptionsDlg.showPrintOptionsDlg( this, true, false );
      }
      else if((src == this.mnuFilePrint)
	      || (src == this.mnuPopupPrint)
	      || (src == this.btnPrint) )
      {
	rv = true;
	doPrint();
      }
      else if( (src == this.mnuFileOpenText)
	       || (src == this.mnuPopupOpenText)
	       || (src == this.btnOpenText) )
      {
	rv = true;
	doOpenText();
      }
      else if( (src == this.mnuFileOpenHex)
	       || (src == this.mnuPopupOpenHex) )
      {
	rv = true;
	doOpenHex();
      }
      else if( (src == this.mnuFileSave)
	       || (src == this.mnuPopupSave)
	       || (src == this.btnSave) )
      {
	rv = true;
	doSave();
      }
      else if( (src == this.mnuFileDelete)
	       || (src == this.mnuPopupDelete)
	       || (src == this.btnDelete) )
      {
	rv = true;
	doDelete();
      }
      else if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuHelpContent ) {
        rv = true;
        HelpFrm.open( HELP_PAGE );
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null ) {
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
    }
    updBgColor();
  }


  @Override
  protected boolean showPopup( MouseEvent e )
  {
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      this.mnuPopup.show( c, e.getX(), e.getY() );
      rv = true;
    }
    return rv;
  }


	/* --- Aktionen --- */

  private void doFinish()
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int row = this.table.convertRowIndexToModel( rows[ 0 ] );
	if( row >= 0 ) {
	  int modelRow = this.table.convertRowIndexToModel( row );
	  if( modelRow >= 0 ) {
	    PrintData data = this.printMngr.getPrintData( modelRow );
	    if( data != null ) {
	      if( isActivePrintData( data ) ) {
		if( BaseDlg.showYesNoDlg(
			this,
			"M\u00F6chten Sie den Druckauftrag"
						+ " abschlie\u00DFen?" ) )
		{
		  this.printMngr.deactivatePrintData( data );
		  fireUpdActionButtons();
		}
	      } else {
		BaseDlg.showInfoDlg(
			this,
			"Der Druckauftrag ist bereits abgeschlossen." );
	      }
	    }
	  }
	}
      }
    }
  }


  private void doPrint()
  {
    PrintData data = getSelectedPrintData();
    if( data != null ) {
      PrintUtil.doPrint(
		this,
		data,
		String.format(
			"JKCEMU Druckauftrag %d",
			data.getEntryNum() ) );
    }
  }


  private void doOpenText()
  {
    PrintData data = getSelectedPrintData();
    if( data != null ) {
      StringBuilder buf = new StringBuilder( data.size() );

      byte[] dataBytes = data.getBytes();
      if( dataBytes != null ) {
	PrintDataScanner scanner = new PrintDataScanner( dataBytes );
	while( !scanner.endReached() ) {
	  String line = scanner.readLine();
	  while( line != null ) {
	    buf.append( line );
	    buf.append( (char) '\n' );
	    line = scanner.readLine();
	  }
	  if( !scanner.skipFormFeed() )
	    break;
	}
      }
      this.screenFrm.openText( buf.toString() );
    }
  }


  private void doOpenHex()
  {
    PrintData data = getSelectedPrintData();
    if( data != null ) {
      byte[] dataBytes = data.getBytes();
      if( dataBytes != null ) {
	HexEditFrm.open( dataBytes );
      }
    }
  }


  private void doSave()
  {
    try {
      PrintData data = getSelectedPrintData();
      if( data != null ) {
	File file = EmuUtil.showFileSaveDlg(
				this,
				"Druckauftrag speichern",
				Main.getLastDirFile( Main.FILE_GROUP_PRINT ),
				EmuUtil.getTextFileFilter() );
	if( file != null ) {
	  data.saveToFile( file );
	  this.printMngr.fireTableDataChanged();
	  Main.setLastFile( file, Main.FILE_GROUP_PRINT );
	}
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg(
		this,
		"Der Druckauftrag kann nicht gespeichert werden."
						+ ex.getMessage() );
    }
  }


  private void doDelete()
  {
    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	if( !BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie den Druckauftrag l\u00F6schen?" ) )
	{
	  rows = null;
	}
      } else if( rows.length > 1 ) {
	if( !BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die ausgew\u00E4hlten"
			+ " Druckauftr\u00E4ge l\u00F6schen?" ) )
	{
	  rows = null;
	}
      } else {
	rows = null;
      }
    }
    if( rows != null ) {
      for( int i = 0; i < rows.length; i++ ) {
	rows[ i ] = this.table.convertRowIndexToModel( rows[ i ] );
      }
      Arrays.sort( rows );
      for( int i = rows.length - 1; i >= 0; --i ) {
	int row = rows[ i ];
	if( row >= 0 ) {
	  this.printMngr.removeRow( row );
	}
      }
      fireUpdActionButtons();
    }
  }


	/* --- Konstruktor --- */

  private PrintListFrm( ScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
    this.printMngr = screenFrm.getEmuThread().getPrintMngr();

    setTitle( "JKCEMU Druckauftr\u00E4ge" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileFinish = createJMenuItem( "Abschlie\u00DFen" );
    mnuFile.add( this.mnuFileFinish );
    mnuFile.addSeparator();

    this.mnuFilePrintOptions = createJMenuItem( "Druckoptionen..." );
    mnuFile.add( this.mnuFilePrintOptions );

    this.mnuFilePrint = createJMenuItem(
		"Drucken...",
		KeyStroke.getKeyStroke( KeyEvent.VK_P, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFilePrint );
    mnuFile.addSeparator();

    this.mnuFileOpenText = createJMenuItem( "Im Texteditor \u00F6ffnen..." );
    mnuFile.add( this.mnuFileOpenText );

    this.mnuFileOpenHex = createJMenuItem( "Im Hex-Editor \u00F6ffnen..." );
    mnuFile.add( this.mnuFileOpenHex );

    this.mnuFileSave = createJMenuItem( "Speichern unter..." );
    mnuFile.add( this.mnuFileSave );
    mnuFile.addSeparator();

    this.mnuFileDelete = createJMenuItem(
		"L\u00F6schen",
		KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    mnuFile.add( this.mnuFileDelete );
    mnuFile.addSeparator();

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupFinish = createJMenuItem( "Abschlie\u00DFen" );
    mnuPopup.add( this.mnuPopupFinish );
    mnuPopup.addSeparator();

    this.mnuPopupPrint = createJMenuItem( "Drucken..." );
    mnuPopup.add( this.mnuPopupPrint );

    this.mnuPopupOpenText = createJMenuItem( "Im Texteditor \u00F6ffnen..." );
    mnuPopup.add( this.mnuPopupOpenText );

    this.mnuPopupOpenHex = createJMenuItem( "Im Hex-Editor \u00F6ffnen..." );
    mnuPopup.add( this.mnuPopupOpenHex );

    this.mnuPopupSave = createJMenuItem( "Speichern unter..." );
    mnuPopup.add( this.mnuPopupSave );
    mnuPopup.addSeparator();

    this.mnuPopupDelete = createJMenuItem( "L\u00F6schen" );
    mnuPopup.add( this.mnuPopupDelete );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 0, 0, 0 ),
						0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );

    this.btnPrint = createImageButton(
			"/images/file/print.png",
			"Drucken..." );
    toolBar.add( this.btnPrint );

    this.btnOpenText = createImageButton(
			"/images/file/edit.png",
			"Im Texteditor \u00F6ffnen" );
    toolBar.add( this.btnOpenText );

    this.btnSave = createImageButton(
			"/images/file/save_as.png",
			"Speichern unter" );
    toolBar.add( this.btnSave );
    toolBar.addSeparator();

    this.btnDelete = createImageButton(
			"/images/file/delete.png",
			"L\u00F6schen" );
    toolBar.add( this.btnDelete );

    add( toolBar, gbc );


    // Tabelle
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridy++;

    this.table = new JTable( this.printMngr );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setPreferredScrollableViewportSize( new Dimension( 340, 200 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setShowGrid( false );
    this.table.setShowHorizontalLines( false );
    this.table.setShowVerticalLines( false );
    this.table.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.table.addMouseListener( this );

    EmuUtil.setTableColWidths( this.table, 70, 70, 200 );

    ListSelectionModel selectionModel = this.table.getSelectionModel();
    if( selectionModel != null ) {
      selectionModel.addListSelectionListener( this );
      updActionButtons();
    }

    this.scrollPane = new JScrollPane(
			this.table,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
    this.scrollPane.addMouseListener( this );

    add( this.scrollPane, gbc );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );


    // sonstiges
    updActionButtons();
    updBgColor();
  }


	/* --- private Methoden --- */

  private void fireUpdActionButtons()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updActionButtons();
		  }
		} );
  }


  private PrintData getSelectedPrintData()
  {
    PrintData data = null;

    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int row = this.table.convertRowIndexToModel( rows[ 0 ] );
	if( row >= 0 ) {
	  data = this.printMngr.getPrintData( row );
	  if( data != null ) {
	    if( isActivePrintData( data ) ) {
	      if( BaseDlg.showYesNoDlg(
			this,
			"Der Druckauftrag ist noch nicht abgeschlossen.\n"
			  + "M\u00F6chten Sie ihn jetzt abschlie\u00DFen?" ) )
	      {
		this.printMngr.deactivatePrintData( data );
		updFinishButtons();
	      }
	    }
	  }
	}
      }
    }
    return data;
  }


  private boolean isActivePrintData( PrintData data )
  {
    boolean status = false;
    if( data != null ) {
      PrintData activeData = this.printMngr.getActivePrintData();
      if( (activeData != null) && (activeData == data) )
	status = true;
    }
    return status;
  }


  private void updFinishButtons()
  {
    boolean state = false;

    int[] rows = this.table.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int row = this.table.convertRowIndexToModel( rows[ 0 ] );
	if( row >= 0 ) {
	  if( isActivePrintData( this.printMngr.getPrintData( row ) ) ) {
	    state = true;
	  }
	}
      }
    }
    this.mnuFileFinish.setEnabled( state );
    this.mnuPopupFinish.setEnabled( state );
  }


  private void updActionButtons()
  {
    int     nRows = this.table.getSelectedRowCount();
    boolean state = (nRows == 1);
    this.mnuFilePrint.setEnabled( state );
    this.mnuFileOpenText.setEnabled( state );
    this.mnuFileOpenHex.setEnabled( state );
    this.mnuFileSave.setEnabled( state );
    this.mnuFileDelete.setEnabled( nRows > 0 );
    this.mnuPopupPrint.setEnabled( state );
    this.mnuPopupOpenText.setEnabled( state );
    this.mnuPopupOpenHex.setEnabled( state );
    this.mnuPopupSave.setEnabled( state );
    this.mnuPopupDelete.setEnabled( nRows > 0 );
    this.btnPrint.setEnabled( state );
    this.btnOpenText.setEnabled( state );
    this.btnSave.setEnabled( state );
    this.btnDelete.setEnabled( nRows > 0 );
    updFinishButtons();
  }


  private void updBgColor()
  {
    Color     color = this.table.getBackground();
    JViewport vp    = this.scrollPane.getViewport();
    if( (color != null) && (vp != null) )
      vp.setBackground( color );
  }
}
