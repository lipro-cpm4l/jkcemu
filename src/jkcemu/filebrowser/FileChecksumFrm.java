/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Berechnung von Pruefsummen und Hashwerten von Dateien
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.security.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;


public class FileChecksumFrm extends BasicFrm
				implements
					ListSelectionListener,
					Runnable
{
  private static final String BTN_TEXT_CALCULATE = "Berechnen";

  private ScreenFrm        screenFrm;
  private JMenuItem        mnuClose;
  private JMenuItem        mnuCopyUpper;
  private JMenuItem        mnuCopyLower;
  private JMenuItem        mnuCompare;
  private JMenuItem        mnuHelpContent;
  private JPopupMenu       mnuPopup;
  private JMenuItem        mnuPopupCopyUpper;
  private JMenuItem        mnuPopupCopyLower;
  private JMenuItem        mnuPopupCompare;
  private JLabel           labelAlgorithm;
  private JComboBox        comboAlgorithm;
  private JButton          btnAction;
  private JTable           table;
  private FileTableModel   tableModel;
  private Thread           thread;
  private String           algorithm;
  private CksCalculator    cks;
  private volatile boolean cancelled;
  private volatile boolean filesChanged;


  public FileChecksumFrm( ScreenFrm screenFrm )
  {
    this.screenFrm    = screenFrm;
    this.thread       = null;
    this.algorithm    = null;
    this.cks          = null;
    this.cancelled    = false;
    this.filesChanged = false;

    setTitle( "JKCEMU Pr\u00FCfsumme-/Hash-Wert berechnen" );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );

    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );

    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuCopyUpper = createJMenuItem(
				"Wert in Gro\u00DFschreibweise kopieren" );
    mnuEdit.add( this.mnuCopyUpper );

    this.mnuCopyLower = createJMenuItem(
				"Wert in Kleinschreibweise kopieren" );
    mnuEdit.add( this.mnuCopyLower );
    mnuEdit.addSeparator();

    this.mnuCompare = createJMenuItem( "Wert mit Zwischenablage vergleichen" );
    mnuEdit.add( this.mnuCompare );

    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupCopyUpper = createJMenuItem(
				"Wert in Gro\u00DFschreibweise kopieren" );
    this.mnuPopup.add( this.mnuPopupCopyUpper );

    this.mnuPopupCopyLower = createJMenuItem(
				"Wert in Kleinschreibweise kopieren" );
    this.mnuPopup.add( this.mnuPopupCopyLower );
    this.mnuPopup.addSeparator();

    this.mnuPopupCompare = createJMenuItem(
				"Wert mit Zwischenablage vergleichen" );
    this.mnuPopup.add( this.mnuPopupCompare );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.labelAlgorithm = new JLabel( "Algorithmus:" );
    this.labelAlgorithm.setEnabled( false );
    add( this.labelAlgorithm, gbc );

    this.comboAlgorithm = new JComboBox(
				CksCalculator.getAvailableAlgorithms() );
    this.comboAlgorithm.setEditable( false );
    this.comboAlgorithm.setEnabled( false );
    gbc.gridx++;
    add( this.comboAlgorithm, gbc );

    this.btnAction = new JButton( BTN_TEXT_CALCULATE );
    this.btnAction.setEnabled( false );
    this.btnAction.addActionListener( this );
    gbc.gridx++;
    add( this.btnAction, gbc );

    FileTableModel.Column[] cols = {
				FileTableModel.Column.NAME,
				FileTableModel.Column.VALUE };
    this.tableModel = new FileTableModel( cols );

    this.table = new JTable( this.tableModel );
    this.table.addMouseListener( this );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setRowSelectionAllowed( true );
    this.table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    gbc.anchor    = GridBagConstraints.NORTHWEST;
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JScrollPane( this.table ), gbc );

    ListSelectionModel selModel = this.table.getSelectionModel();
    if( selModel != null ) {
      selModel.addListSelectionListener( this );
      this.mnuCopyUpper.setEnabled( false );
      this.mnuCopyLower.setEnabled( false );
      this.mnuCompare.setEnabled( false );
      this.mnuPopupCopyUpper.setEnabled( false );
      this.mnuPopupCopyLower.setEnabled( false );
      this.mnuPopupCompare.setEnabled( false );
    }
    EmuUtil.setTableColWidths( this.table, 200, 150 );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      this.table.setPreferredScrollableViewportSize(
					new Dimension( 350, 200 ) );
      pack();
      setScreenCentered();
      this.table.setPreferredScrollableViewportSize( new Dimension( 1, 1 ) );
    }
    setResizable( true );
  }


  public void setFiles( Collection<File> files )
  {
    this.cancelled = true;
    synchronized( this.tableModel ) {
      this.filesChanged = true;
      this.tableModel.clear( false );
      if( files != null ) {
	for( File file : files ) {
	  if( file.isFile() ) {
	    FileEntry entry = new FileEntry();
	    entry.setName( file.getName() );
	    entry.setFile( file );
	    this.tableModel.addRow( entry, false );
	  }
	}
      }
      this.tableModel.fireTableDataChanged();
    }
    updFields();
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    updEditBtns();
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    String        algorithm = null;
    CksCalculator cks       = null;
    int           nRows     = 0;
    synchronized( this.tableModel ) {
      algorithm = this.algorithm;
      cks       = this.cks;
      nRows     = this.tableModel.getRowCount();
      if( nRows > 0 ) {
	for( int i = 0; i < nRows; i++ ) {
	  FileEntry entry = this.tableModel.getRow( i );
	  if( entry != null ) {
	    entry.setValue( null );
	  }
	}
	fireTableRowsUpdated( 0, nRows - 1 );
      }
    }
    if( (cks != null) && (nRows > 0) ) {
      for( int i = 0; !this.cancelled && (i < nRows); i++ ) {
	FileEntry entry = null;
	synchronized( this.tableModel ) {
	  if( i < this.tableModel.getRowCount() ) {
	    entry = this.tableModel.getRow( i );
	  }
	}
	if( entry != null ) {
	  InputStream in = null;
	  cks.reset();
	  try {
	    in = new BufferedInputStream(
				new FileInputStream( entry.getFile() ) );
	    entry.setMarked( true );
	    entry.setValue( "Wird berechnet..." );
	    fireTableRowsUpdated( i, i );
	    if( cks != null ) {
	      int b = in.read();
	      while( !this.cancelled && (b != -1) ) {
		cks.update( b );
		b = in.read();
	      }
	      if( !this.cancelled ) {
		entry.setValue( cks.getValue() );
	      }
	    }
	    if( this.cancelled ) {
	      entry.setValue( null );
	    }
	    entry.setMarked( false );
	  }
	  catch( IOException ex ) {
	    String msg = ex.getMessage();
	    if( msg != null ) {
	      entry.setValue( "Fehler: " + msg );
	    } else {
	      entry.setValue( "Fehler" );
	    }
	  }
	  finally {
	    if( in != null ) {
	      try {
		in.close();
	      }
	      catch( IOException ex ) {}
	    }
	  }
	  if( !this.filesChanged ) {
	    fireTableRowsUpdated( i, i );
	  }
	} else {
	  this.cancelled = true;
	}
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    updEditBtns();
		  }
		} );
      }
    }
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    calculationFinished();
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.btnAction ) {
	rv = true;
	doCalculate();
      }
      else if( (src == this.mnuCopyUpper)
	       || (src == this.mnuPopupCopyUpper) )
      {
	rv = true;
	doCopyUpper();
      }
      else if( (src == this.mnuCopyLower)
	       || (src == this.mnuPopupCopyLower) )
      {
	rv = true;
	doCopyLower();
      }
      else if( (src == this.mnuCompare)
	       || (src == this.mnuPopupCompare) )
      {
	rv = true;
	doCompare();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/tools/filechecksum.htm" );
      }

    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


  @Override
  protected boolean showPopup( MouseEvent e )
  {
    boolean rv = false;
    if( e != null ) {
      Component c = e.getComponent();
      if( c != null ) {
	this.mnuPopup.show( c, e.getX(), e.getY() );
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.cancelled = true;
  }


	/* --- private Methoden --- */

  private void calculationFinished()
  {
    this.thread = null;
    updFields();
  }


  private void copyToClipboard( String text )
  {
    try {
      if( text != null ) {
	Toolkit tk = getToolkit();
	if( tk != null ) {
	  Clipboard clipboard = tk.getSystemClipboard();
	  if( clipboard != null ) {
	    StringSelection contents = new StringSelection( text );
	    clipboard.setContents( contents, contents );
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doCopyUpper()
  {
    String value = getSelectedValue();
    if( value != null )
      copyToClipboard( value.toUpperCase() );
  }


  private void doCopyLower()
  {
    String value = getSelectedValue();
    if( value != null )
      copyToClipboard( value.toLowerCase() );
  }


  private void doCompare()
  {
    String value = getSelectedValue();
    if( value != null ) {
      if( value.length() > 0 ) {
	String text = null;
	try {
	  Toolkit tk = getToolkit();
	  if( tk != null ) {
	    Clipboard clipboard = tk.getSystemClipboard();
	    if( clipboard != null ) {
	      if( clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor ) )
	      {
		Object o = clipboard.getData( DataFlavor.stringFlavor );
		if( o != null ) {
		  text = o.toString();
		}
	      }
	    }
	  }
	}
	catch( IllegalStateException ex1 ) {}
	catch( IOException ex2 ) {}
	catch( UnsupportedFlavorException ex3 ) {}
	if( text != null ) {
	  try {
	    text = text.replaceAll( "[ \t\r\n]", "" );
	  }
	  catch( PatternSyntaxException ex ) {}
	  if( text.length() < 1 ) {
	    text = null;
	  }
	}
	if( text != null ) {
	  if( value.equalsIgnoreCase( text ) ) {
	    JOptionPane.showMessageDialog(
			this,
			"Der ausgew\u00E4hlte Wert stimmt mit dem\n"
				+ "in der Zwischenablage stehenden Text"
				+ " \u00FCberein.",
			"\u00DCbereinstimmung",
			JOptionPane.INFORMATION_MESSAGE );
	  } else {
	    JOptionPane.showMessageDialog(
			this,
			"Der ausgew\u00E4hlte Wert stimmt mit dem\n"
				+ "in der Zwischenablage stehenden Text\n"
				+ "nicht \u00FCberein.",
			"Abweichung",
			JOptionPane.WARNING_MESSAGE );
	  }
	} else {
	  BasicDlg.showErrorDlg(
		this,
		"Die Zwischenablage enth\u00E4lt keinen Text.\n"
			+ "Kopieren Sie bitte den zu pr\u00FCfenden Wert\n"
			+ "in die Zwischenablage und\n"
			+ "rufen die Funktion noch einmal auf." );
	}
      }
    }
  }


  private void doCalculate()
  {
    synchronized( this.tableModel ) {
      if( this.thread != null ) {
	this.cancelled = true;
      } else {
	Object o = this.comboAlgorithm.getSelectedItem();
	if( o != null ) {
	  String algorithm = o.toString();
	  if( algorithm != null ) {
	    this.cks = null;
	    try {
	      this.cks          = new CksCalculator( algorithm );
	      this.algorithm    = algorithm;
	      this.cancelled    = false;
	      this.filesChanged = false;
	      this.thread       = new Thread(
					this,
					"JKCEMU checksum calculator" );
	      this.thread.start();
	      updFields();
	    }
	    catch( NoSuchAlgorithmException ex ) {
	      BasicDlg.showErrorDlg(
			this,
			"Der Algorithmus wird nicht unterst&uuml;tzt." );
	    }
	  }
	}
      }
    }
  }


  private void fireTableRowsUpdated( final int fromRow, final int toRow )
  {
    final FileTableModel tableModel = this.tableModel;
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    if( toRow < tableModel.getRowCount() )
		      tableModel.fireTableRowsUpdated( fromRow, toRow );
		  }
		} );
  }


  private String getSelectedValue()
  {
    String rv  = null;
    int    row = this.table.getSelectedRow();
    if( row >= 0 ) {
      FileEntry entry = this.tableModel.getRow( row );
      if( entry != null ) {
	if( !entry.isMarked() ) {
	  Object value = entry.getValue();
	  if( value != null ) {
	    rv = value.toString();
	  }
	}
      }
    }
    return rv;
  }


  private void updEditBtns()
  {
    boolean state = false;
    int     row   = this.table.getSelectedRow();
    if( row >= 0 ) {
      FileEntry entry = this.tableModel.getRow( row );
      if( entry != null ) {
	state = !entry.isMarked() && (entry.getValue() != null);
      }
    }
    this.mnuCopyUpper.setEnabled( state );
    this.mnuCopyLower.setEnabled( state );
    this.mnuCompare.setEnabled( state );
    this.mnuPopupCopyUpper.setEnabled( state );
    this.mnuPopupCopyLower.setEnabled( state );
    this.mnuPopupCompare.setEnabled( state );
  }


  private void updFields()
  {
    if( this.thread != null ) {
      this.labelAlgorithm.setEnabled( false );
      this.comboAlgorithm.setEnabled( false );
      this.btnAction.setText( "Abbrechen" );
    } else {
      boolean state = (this.tableModel.getRowCount() > 0);
      this.labelAlgorithm.setEnabled( state );
      this.comboAlgorithm.setEnabled( state );
      this.btnAction.setText( BTN_TEXT_CALCULATE );
      this.btnAction.setEnabled( state );
    }
  }
}
