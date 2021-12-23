/*
 * (c) 2009-2021 Jens Mueller
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
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.ScreenFrm;
import jkcemu.file.FileUtil;
import jkcemu.text.CharConverter;
import jkcemu.tools.hexedit.HexEditFrm;


public class PrintListFrm
			extends BaseFrm
			implements ListSelectionListener, PopupMenuOwner
{
  private static final String HELP_PAGE    = "/help/print.htm";
  private static final String PROP_CHARSET = "jkcemu.printer.charset";

  private static PrintListFrm instance = null;

  private ScreenFrm            screenFrm;
  private PrintMngr            printMngr;
  private JMenuItem            mnuFileFinish;
  private JMenuItem            mnuFilePrintOptions;
  private JMenuItem            mnuFilePrint;
  private JMenuItem            mnuFileOpenText;
  private JMenuItem            mnuFileOpenHex;
  private JMenuItem            mnuFileSaveAs;
  private JMenuItem            mnuFileDelete;
  private JMenuItem            mnuFileClose;
  private JMenuItem            mnuHelpContent;
  private JRadioButtonMenuItem mnuCharsetASCII;
  private JRadioButtonMenuItem mnuCharsetISO646DE;
  private JRadioButtonMenuItem mnuCharsetCP437;
  private JRadioButtonMenuItem mnuCharsetCP850;
  private JRadioButtonMenuItem mnuCharsetLATIN1;
  private JPopupMenu           popupMnu;
  private JMenuItem            popupFinish;
  private JMenuItem            popupPrint;
  private JMenuItem            popupOpenText;
  private JMenuItem            popupOpenHex;
  private JMenuItem            popupSaveAs;
  private JMenuItem            popupDelete;
  private JButton              btnPrint;
  private JButton              btnOpenText;
  private JButton              btnSaveAs;
  private JButton              btnDelete;
  private JTable               table;
  private JScrollPane          scrollPane;


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


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.mnuFileFinish) || (src == this.popupFinish) ) {
	rv = true;
	doFinish();
      }
      else if( src == this.mnuFilePrintOptions ) {
	rv = true;
	PrintOptionsDlg.showPrintOptionsDlg( this, true, false );
      }
      else if((src == this.mnuFilePrint)
	      || (src == this.popupPrint)
	      || (src == this.btnPrint) )
      {
	rv = true;
	doPrint();
      }
      else if( (src == this.mnuFileOpenText)
	       || (src == this.popupOpenText)
	       || (src == this.btnOpenText) )
      {
	rv = true;
	doOpenText();
      }
      else if( (src == this.mnuFileOpenHex)
	       || (src == this.popupOpenHex) )
      {
	rv = true;
	doOpenHex();
      }
      else if( (src == this.mnuFileSaveAs)
	       || (src == this.popupSaveAs)
	       || (src == this.btnSaveAs) )
      {
	rv = true;
	doSaveAs();
      }
      else if( (src == this.mnuFileDelete)
	       || (src == this.popupDelete)
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
        HelpFrm.openPage( HELP_PAGE );
      }
    }
    return rv;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      EmuUtil.setProperty(
		props,
		PROP_CHARSET,
		CharConverter.getEncodingName(
				getSelectedCharConverterEncoding() ) );
    }
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      this.popupMnu.show( c, e.getX(), e.getY() );
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
      data.setCharConverter( getSelectedCharConverter() );
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
	PrintDataScanner scanner = new PrintDataScanner(
					dataBytes,
					getSelectedCharConverter() );
	while( !scanner.endReached() ) {
	  String line = scanner.readLine();
	  while( line != null ) {
	    buf.append( line );
	    buf.append( '\n' );
	    line = scanner.readLine();
	  }
	  if( scanner.skipFormFeed() ) {
	    buf.append( '\f' );
	  }
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


  private void doSaveAs()
  {
    try {
      PrintData data = getSelectedPrintData();
      if( data != null ) {
	File file = FileUtil.showFileSaveDlg(
				this,
				"Druckauftrag speichern",
				Main.getLastDirFile( Main.FILE_GROUP_PRINT ),
				FileUtil.getTextFileFilter() );
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


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuFileFinish = createMenuItem( "Abschlie\u00DFen" );
    mnuFile.add( this.mnuFileFinish );
    mnuFile.addSeparator();

    this.mnuFilePrintOptions = createMenuItemOpenPrintOptions();
    mnuFile.add( this.mnuFilePrintOptions );

    this.mnuFilePrint = createMenuItemOpenPrint( true );
    mnuFile.add( this.mnuFilePrint );
    mnuFile.addSeparator();

    this.mnuFileOpenText = createMenuItem( "Im Texteditor \u00F6ffnen..." );
    mnuFile.add( this.mnuFileOpenText );

    this.mnuFileOpenHex = createMenuItem( "Im Hex-Editor \u00F6ffnen..." );
    mnuFile.add( this.mnuFileOpenHex );

    this.mnuFileSaveAs = createMenuItemSaveAs( true );
    mnuFile.add( this.mnuFileSaveAs );
    mnuFile.addSeparator();

    this.mnuFileDelete = createMenuItemWithDirectAccelerator(
						EmuUtil.TEXT_DELETE,
						KeyEvent.VK_DELETE );
    mnuFile.add( this.mnuFileDelete );
    mnuFile.addSeparator();

    this.mnuFileClose = createMenuItemClose();
    mnuFile.add( this.mnuFileClose );


    // Menu Zeichensatz
    JMenu mnuCharset = GUIFactory.createMenu( "Zeichensatz" );

    ButtonGroup grpCharset = new ButtonGroup();

    CharConverter.Encoding encoding = CharConverter.getEncodingByName(
					Main.getProperty( PROP_CHARSET ) );

    this.mnuCharsetASCII = GUIFactory.createRadioButtonMenuItem(
				CharConverter.getEncodingDisplayText(
					CharConverter.Encoding.ASCII ),
				encoding == CharConverter.Encoding.ASCII );
    grpCharset.add( this.mnuCharsetASCII );
    mnuCharset.add( this.mnuCharsetASCII );

    this.mnuCharsetISO646DE = GUIFactory.createRadioButtonMenuItem(
				CharConverter.getEncodingDisplayText(
					CharConverter.Encoding.ISO646DE ),
				encoding == CharConverter.Encoding.ISO646DE );
    grpCharset.add( this.mnuCharsetISO646DE );
    mnuCharset.add( this.mnuCharsetISO646DE );

    this.mnuCharsetCP437 = GUIFactory.createRadioButtonMenuItem(
				CharConverter.getEncodingDisplayText(
					CharConverter.Encoding.CP437 ),
				(encoding == CharConverter.Encoding.CP437)
					|| (encoding == null) );
    grpCharset.add( this.mnuCharsetCP437 );
    mnuCharset.add( this.mnuCharsetCP437 );

    this.mnuCharsetCP850 = GUIFactory.createRadioButtonMenuItem(
				CharConverter.getEncodingDisplayText(
					CharConverter.Encoding.CP850 ),
				encoding == CharConverter.Encoding.CP850 );
    grpCharset.add( this.mnuCharsetCP850 );
    mnuCharset.add( this.mnuCharsetCP850 );

    this.mnuCharsetLATIN1 = GUIFactory.createRadioButtonMenuItem(
				CharConverter.getEncodingDisplayText(
					CharConverter.Encoding.LATIN1 ),
				encoding == CharConverter.Encoding.LATIN1 );
    grpCharset.add( this.mnuCharsetLATIN1 );
    mnuCharset.add( this.mnuCharsetLATIN1 );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem(
				"Hilfe zu Druckauftr\u00E4gen..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuCharset, mnuHelp ) );


    // Popup-Menu
    this.popupMnu = GUIFactory.createPopupMenu();

    this.popupFinish = createMenuItem( this.mnuFileFinish.getText() );
    popupMnu.add( this.popupFinish );
    popupMnu.addSeparator();

    this.popupPrint = createMenuItemOpenPrint( false );
    popupMnu.add( this.popupPrint );

    this.popupOpenText = createMenuItem(
				this.mnuFileOpenText.getText() );
    popupMnu.add( this.popupOpenText );

    this.popupOpenHex = createMenuItem(
				this.mnuFileOpenHex.getText() );
    popupMnu.add( this.popupOpenHex );

    this.popupSaveAs = createMenuItem( this.mnuFileSaveAs.getText() );
    popupMnu.add( this.popupSaveAs );
    popupMnu.addSeparator();

    this.popupDelete = createMenuItem( EmuUtil.TEXT_DELETE );
    popupMnu.add( this.popupDelete );


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
    JToolBar toolBar = GUIFactory.createToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );

    this.btnPrint = GUIFactory.createRelImageResourceButton(
					this,
					"file/print.png",
					this.mnuFilePrint.getText() );
    this.btnPrint.addActionListener( this );
    toolBar.add( this.btnPrint );

    this.btnOpenText = GUIFactory.createRelImageResourceButton(
					this,
					"file/edit.png",
					this.mnuFileOpenText.getText() );
    this.btnOpenText.addActionListener( this );
    toolBar.add( this.btnOpenText );

    this.btnSaveAs = GUIFactory.createRelImageResourceButton(
					this,
					"file/save_as.png",
					this.mnuFileSaveAs.getText() );
    this.btnSaveAs.addActionListener( this );
    toolBar.add( this.btnSaveAs );
    toolBar.addSeparator();

    this.btnDelete = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					EmuUtil.TEXT_DELETE );
    this.btnDelete.addActionListener( this );
    toolBar.add( this.btnDelete );

    add( toolBar, gbc );


    // Tabelle
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridy++;

    this.table = GUIFactory.createTable( this.printMngr );
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

    this.scrollPane = GUIFactory.createScrollPane(
			this.table,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
    this.scrollPane.addMouseListener( this );

    add( this.scrollPane, gbc );


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setScreenCentered();
    }


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


  private CharConverter getSelectedCharConverter()
  {
    return CharConverter.getCharConverter(
				getSelectedCharConverterEncoding() );
  }


  private CharConverter.Encoding getSelectedCharConverterEncoding()
  {
    CharConverter.Encoding encoding = CharConverter.Encoding.ASCII;
    if( this.mnuCharsetISO646DE.isSelected() ) {
      encoding = CharConverter.Encoding.ISO646DE;
    } else if( this.mnuCharsetCP437.isSelected() ) {
      encoding = CharConverter.Encoding.CP437;
    } else if( this.mnuCharsetCP850.isSelected() ) {
      encoding = CharConverter.Encoding.CP850;
    } else if( this.mnuCharsetLATIN1.isSelected() ) {
      encoding = CharConverter.Encoding.LATIN1;
    }
    return encoding;
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
      if( (activeData != null) && (activeData == data) ) {
	status = true;
      }
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
    this.popupFinish.setEnabled( state );
  }


  private void updActionButtons()
  {
    int     nRows = this.table.getSelectedRowCount();
    boolean state = (nRows == 1);
    this.mnuFilePrint.setEnabled( state );
    this.mnuFileOpenText.setEnabled( state );
    this.mnuFileOpenHex.setEnabled( state );
    this.mnuFileSaveAs.setEnabled( state );
    this.mnuFileDelete.setEnabled( nRows > 0 );
    this.popupPrint.setEnabled( state );
    this.popupOpenText.setEnabled( state );
    this.popupOpenHex.setEnabled( state );
    this.popupSaveAs.setEnabled( state );
    this.popupDelete.setEnabled( nRows > 0 );
    this.btnPrint.setEnabled( state );
    this.btnOpenText.setEnabled( state );
    this.btnSaveAs.setEnabled( state );
    this.btnDelete.setEnabled( nRows > 0 );
    updFinishButtons();
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
