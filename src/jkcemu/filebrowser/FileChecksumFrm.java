/*
 * (c) 2008 Jens Mueller
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
  private static final String BUTTON_TEXT_CALCULATE  = "Berechnen";
  private static final String CHECKSUM_ADLER32_CODE  = "Adler-32";
  private static final String CHECKSUM_CRC32_CODE    = "CRC-32";

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
  private Checksum         checksum;
  private MessageDigest    digest;
  private volatile boolean cancelled;
  private volatile boolean filesChanged;


  public FileChecksumFrm( ScreenFrm screenFrm )
  {
    this.screenFrm    = screenFrm;
    this.thread       = null;
    this.algorithm    = null;
    this.checksum     = null;
    this.digest       = null;
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

    this.comboAlgorithm = new JComboBox();
    this.comboAlgorithm.addItem( CHECKSUM_ADLER32_CODE );
    this.comboAlgorithm.addItem( CHECKSUM_CRC32_CODE );
    this.comboAlgorithm.addItem( "MD2" );
    this.comboAlgorithm.addItem( "MD5" );
    this.comboAlgorithm.addItem( "SHA-1" );
    this.comboAlgorithm.addItem( "SHA-256" );
    this.comboAlgorithm.addItem( "SHA-384" );
    this.comboAlgorithm.addItem( "SHA-512" );
    this.comboAlgorithm.setEditable( false );
    this.comboAlgorithm.setEnabled( false );
    gbc.gridx++;
    add( this.comboAlgorithm, gbc );

    this.btnAction = new JButton( BUTTON_TEXT_CALCULATE );
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
      this.tableModel.clear();
      if( files != null ) {
	for( File file : files ) {
	  if( file.isFile() )
	    this.tableModel.addRow( new FileEntry( file ) );
	}
      }
    }
    this.tableModel.fireTableDataChanged();
    updFields();
  }


	/* --- ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    updEditBtns();
  }


	/* --- Runnable --- */

  public void run()
  {
    String        algorithm = null;
    Checksum      checksum  = null;
    MessageDigest digest    = null;
    int           nRows     = 0;
    synchronized( this.tableModel ) {
      algorithm = this.algorithm;
      checksum  = this.checksum;
      digest    = this.digest;
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
    if( ((checksum != null) || (digest != null)) && (nRows > 0) ) {
      for( int i = 0; !this.cancelled && (i < nRows); i++ ) {
	FileEntry entry = null;
	synchronized( this.tableModel ) {
	  if( i < this.tableModel.getRowCount() )
	    entry = this.tableModel.getRow( i );
	}
	if( entry != null ) {
	  InputStream in = null;
	  try {
	    in = new BufferedInputStream(
				new FileInputStream( entry.getFile() ) );
	    entry.setMarked( true );
	    entry.setValue( "Wird berechnet..." );
	    fireTableRowsUpdated( i, i );
	    if( checksum != null ) {
	      int b = in.read();
	      while( !this.cancelled && (b != -1) ) {
		checksum.update( b );
		b = in.read();
	      }
	      if( !this.cancelled ) {
		entry.setValue( String.format( "%08X", checksum.getValue() ) );
	      }
	    }
	    else if( digest != null ) {
	      int b = in.read();
	      while( !this.cancelled && (b != -1) ) {
		digest.update( (byte) b );
		b = in.read();
	      }
	      byte[] result = digest.digest();
	      if( result != null ) {
		StringBuilder buf = new StringBuilder( 2 * result.length );
		for( int k = 0; k < result.length; k++ ) {
		  buf.append(
			String.format( "%02X", ((int) result[ k ] & 0xFF) ) );
		}
		if( !this.cancelled ) {
		  entry.setValue( buf.toString() );
		}
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
	SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    updEditBtns();
		  }
		} );
      }
    }
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    calculationFinished();
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

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


  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


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
		if( o != null )
		  text = o.toString();
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
	    this.checksum = null;
	    this.digest   = null;
	    try {
	      if( algorithm.equals( CHECKSUM_ADLER32_CODE ) ) {
		checksum = new Adler32();
	      } else if( algorithm.equals( CHECKSUM_CRC32_CODE ) ) {
		checksum = new CRC32();
	      } else {
		digest = MessageDigest.getInstance( algorithm );
	      }
	      this.algorithm    = o.toString();
	      this.cancelled    = false;
	      this.filesChanged = false;
	      this.thread       = new Thread( this );
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
    SwingUtilities.invokeLater(
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
	  if( value != null )
	    rv = value.toString();
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
      if( entry != null )
	state = !entry.isMarked() && (entry.getValue() != null);
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
      this.btnAction.setText( BUTTON_TEXT_CALCULATE );
      this.btnAction.setEnabled( state );
    }
  }
}

