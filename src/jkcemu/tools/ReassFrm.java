/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer Reassembler
 */

package jkcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.print.*;
import jkcemu.tools.debugger.DebugFrm;
import z80emu.*;


public class ReassFrm extends BasicFrm implements CaretListener
{
  private static final int COL_MNEMONIC = 23;
  private static final int COL_ARGS     = COL_MNEMONIC + 8;
  private static final int COL_REMARK   = COL_ARGS + 18;

  private static final int COL_SRC_MNEMONIC = 8;
  private static final int COL_SRC_ARGS     = COL_SRC_MNEMONIC + 8;
  private static final int COL_SRC_REMARK   = COL_SRC_ARGS + 18;

  private Z80Memory                  memory;
  private int                        begAddr;
  private int                        endAddr;
  private int                        clickAddr;
  private File                       lastFile;
  private File                       lastLabelFile;
  private Map<Integer,Set<String>>   addr2Labels;
  private String                     textFind;
  private JTextArea                  textArea;
  private JTextComponent             selectionFld;
  private JMenuItem                  mnuReass;
  private JMenuItem                  mnuImportLabelsFile;
  private JMenuItem                  mnuImportLabelsClp;
  private JMenuItem                  mnuRemoveLabels;
  private JMenuItem                  mnuPrintOptions;
  private JMenuItem                  mnuPrint;
  private JMenuItem                  mnuSaveAs;
  private JMenuItem                  mnuSourceCopy;
  private JMenuItem                  mnuSourceExport;
  private JMenuItem                  mnuClose;
  private JMenuItem                  mnuCopy;
  private JMenuItem                  mnuFind;
  private JMenuItem                  mnuFindNext;
  private JMenuItem                  mnuSelectAll;
  private JMenuItem                  mnuHelpContent;
  private JTextField                 fldBegAddr;
  private JTextField                 fldEndAddr;
  private HexDocument                docBegAddr;
  private HexDocument                docEndAddr;
  private JPopupMenu                 popup;
  private JMenuItem                  popupCopy;
  private JMenuItem                  popupBreak;
  private JMenuItem                  popupSelectAll;


  public ReassFrm( Z80Memory memory )
  {
    this.memory         = memory;
    this.begAddr        = -1;
    this.endAddr        = -1;
    this.clickAddr      = -1;
    this.lastFile       = null;
    this.lastLabelFile  = null;
    this.addr2Labels    = null;
    this.textFind       = null;
    this.selectionFld   = null;
    this.textArea       = new JTextArea();
    this.textArea.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 12 ) );
    setTitle( "JKCEMU Reassembler" );
    Main.updIcon( this );


    // Popup-Menu
    this.popup = new JPopupMenu();

    this.popupCopy = createJMenuItem( "Kopieren" );
    this.popup.add( this.popupCopy );
    this.popup.addSeparator();

    this.popupBreak = createJMenuItem( "Halte-/Log-Punkt anlegen" );
    this.popup.add( this.popupBreak );
    this.popup.addSeparator();

    this.popupSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    this.popup.add( this.popupSelectAll );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuReass = createJMenuItem(
			"Reassemblieren",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_R,
					InputEvent.CTRL_MASK ) );
    mnuFile.add( this.mnuReass );
    mnuFile.addSeparator();

    this.mnuImportLabelsFile = createJMenuItem(
			"Marken aus Datei importieren..." );
    mnuFile.add( this.mnuImportLabelsFile );

    this.mnuImportLabelsClp = createJMenuItem(
			"Marken aus Zwischenablage importieren" );
    mnuFile.add( this.mnuImportLabelsClp );

    this.mnuRemoveLabels = createJMenuItem(
			"Importierte Marken entfernen" );
    this.mnuRemoveLabels.setEnabled( false );
    mnuFile.add( this.mnuRemoveLabels );
    mnuFile.addSeparator();

    this.mnuSourceCopy = createJMenuItem( "Als Quelltext kopieren" );
    this.mnuSourceCopy.setEnabled( false );
    mnuFile.add( this.mnuSourceCopy );

    this.mnuSourceExport = createJMenuItem( "Als Quelltext exportieren..." );
    this.mnuSourceExport.setEnabled( false );
    mnuFile.add( this.mnuSourceExport );
    mnuFile.addSeparator();

    this.mnuPrintOptions = createJMenuItem( "Druckoptionen..." );
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = createJMenuItem(
			"Drucken...",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_P,
				InputEvent.CTRL_MASK ) );
    this.mnuPrint.setEnabled( false );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuSaveAs = createJMenuItem(
		"Speichern unter...",
		KeyStroke.getKeyStroke(
			KeyEvent.VK_S,
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuSaveAs.setEnabled( false );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuCopy = createJMenuItem(
			"Kopieren",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_C,
				InputEvent.CTRL_MASK ) );
    this.mnuCopy.setEnabled( false );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuFind = createJMenuItem(
			"Suchen...",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_F,
				InputEvent.CTRL_MASK ) );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );
    mnuEdit.addSeparator();

    this.mnuSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    this.mnuSelectAll.setEnabled( false );
    mnuEdit.add( this.mnuSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Kopfbereich
    JPanel panelHead = new JPanel( new GridBagLayout() );
    add( panelHead, BorderLayout.NORTH );

    GridBagConstraints gbcHead = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panelHead.add( new JLabel( "Anfangsadresse:" ), gbcHead );

    this.docBegAddr = new HexDocument( 4, "Anfangsadresse" );
    this.fldBegAddr = new JTextField( this.docBegAddr, "", 4 );
    this.fldBegAddr.addActionListener( this );
    this.fldBegAddr.addCaretListener( this );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldBegAddr, gbcHead );

    gbcHead.fill    = GridBagConstraints.NONE;
    gbcHead.weightx = 0.0;
    gbcHead.gridx++;
    panelHead.add( new JLabel( "Endadresse:" ), gbcHead );

    this.docEndAddr = new HexDocument( 4, "Endadresse" );
    this.fldEndAddr = new JTextField( this.docEndAddr, "", 4 );
    this.fldEndAddr.addActionListener( this );
    this.fldEndAddr.addCaretListener( this );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldEndAddr, gbcHead );


    // Ergebnisbereich
    this.textArea.setColumns( 58 );
    this.textArea.setRows( 20 );
    this.textArea.setEditable( false );
    this.textArea.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.textArea.addCaretListener( this );
    this.textArea.addMouseListener( this );
    add( new JScrollPane( this.textArea ), BorderLayout.CENTER );


    // sonstiges
    setResizable( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setLocationByPlatform( true );
      this.fldBegAddr.setColumns( 0 );
      this.fldEndAddr.setColumns( 0 );
      this.textArea.setColumns( 0 );
      this.textArea.setRows( 0 );
    }
  }


  public Z80Memory getZ80Memory()
  {
    return this.memory;
  }


  public void setLabels(
		jkcemu.tools.Label[] labels,
		int                  begAddr,
		int                  endAddr )
  {
    importLabels( labels, true );
    if( (begAddr >= 0) && (begAddr <= endAddr) && (endAddr < 0x10000) ) {
      this.docBegAddr.setValue( begAddr, 4 );
      this.docEndAddr.setValue( endAddr, 4 );
      this.begAddr = begAddr;
      this.endAddr = endAddr;
      reassemble();
    }
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JTextComponent ) {
	this.selectionFld = (JTextComponent) src;
	this.mnuCopy.setEnabled(
		this.selectionFld.getSelectionStart()
			!= this.selectionFld.getSelectionEnd() );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object  src = e.getSource();
      if( src != null ) {
	if( src == this.fldBegAddr ) {
	  rv = true;
	  this.fldEndAddr.requestFocus();
	}
	else if( (src == this.fldEndAddr) || (src == this.mnuReass) ) {
	  rv = true;
	  doReass();
	}
	else if( src == this.mnuImportLabelsClp ) {
	  rv = true;
	  doImportLabelsClp();
	}
	else if( src == this.mnuImportLabelsFile ) {
	  rv = true;
	  doImportLabelsFile();
	}
	else if( src == this.mnuRemoveLabels ) {
	  rv = true;
	  doRemoveLabels();
	}
	else if( src == this.mnuSourceCopy ) {
	  rv = true;
	  EmuUtil.copyToClipboard( this, createSourceText() );
	}
	else if( src == this.mnuSourceExport ) {
	  rv = true;
	  doSourceExport();
	}
	else if( src == this.mnuPrintOptions ) {
	  rv = true;
	  PrintOptionsDlg.showPrintOptionsDlg( this, true, true );
	}
	else if( src == this.mnuPrint ) {
	  rv = true;
	  doPrint();
	}
	else if( src == this.mnuSaveAs ) {
	  rv = true;
	  doSaveAs();
	}
	else if( src == this.mnuClose ) {
	  rv = true;
	  doClose();
	}
	else if( src == this.mnuCopy ) {
	  rv = true;
	  if( this.selectionFld != null ) {
	    this.selectionFld.copy();
	  }
	}
	else if( src == this.mnuFind ) {
	  rv = true;
	  doFind();
	}
	else if( src == this.mnuFindNext ) {
	  rv = true;
	  doFindNext();
	}
	else if( src == this.mnuSelectAll ) {
	  rv = true;
	  this.textArea.requestFocus();
	  this.textArea.selectAll();
	}
	else if( src == this.mnuHelpContent ) {
	  rv = true;
	  HelpFrm.open( "/help/tools/reassembler.htm" );
	}
	else if( src == this.popupCopy ) {
	  rv = true;
	  this.textArea.copy();
	}
	else if( src == this.popupBreak ) {
	  rv = true;
	  doCreateBreakpoint();
	}
	else if( src == this.popupSelectAll ) {
	  rv = true;
	  this.textArea.selectAll();
	}
      }
    }
    catch( IOException ex ) {
      BasicDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.fldBegAddr.setText( "" );
      this.fldEndAddr.setText( "" );
      this.mnuPrint.setEnabled( false );
      this.mnuSaveAs.setEnabled( false );
      this.mnuSourceCopy.setEnabled( false );
      this.mnuSourceExport.setEnabled( false );
      this.mnuFind.setEnabled( false );
      this.mnuSelectAll.setEnabled( false );
      setText( "" );
    }
    return rv;
  }


  @Override
  protected boolean showPopup( MouseEvent e )
  {
    boolean rv = false;
    if( e != null ) {
      Component c = e.getComponent();
      if( c != null ) {
	if( c == this.textArea ) {
	  this.clickAddr = ToolsUtil.getReassAddr(
						this.textArea,
						e.getPoint() );
	  this.popupCopy.setEnabled(
		this.textArea.getSelectionStart()
			!= this.textArea.getSelectionEnd() );
	  this.popupBreak.setEnabled( this.clickAddr >= 0 );
	  this.popup.show( c, e.getX(), e.getY() );
	  rv = true;
	}
      }
    }
    return rv;
  }


	/* --- Aktionen --- */

  private void doCreateBreakpoint()
  {
    if( this.clickAddr >= 0 ) {
      ScreenFrm screenFrm = Main.getScreenFrm();
      if( screenFrm != null ) {
	DebugFrm  debugFrm  = null;
	EmuThread emuThread = screenFrm.getEmuThread();
	if( emuThread != null ) {
	  if( this.memory == emuThread ) {
	    debugFrm = screenFrm.openPrimaryDebugger();
	  } else {
	    debugFrm = screenFrm.openSecondDebugger();
	  }
	}
	if( debugFrm != null ) {
	  debugFrm.doDebugBreakPCAdd( this.clickAddr );
	}
      }
    }
  }


  private void doFind()
  {
    String selectedText = this.textArea.getSelectedText();
    if( selectedText != null ) {
      if( selectedText.length() == 0 ) {
	selectedText = null;
      }
    }

    String[]    options = { "Suchen", "Abbrechen" };
    JOptionPane pane    = new JOptionPane(
				"Suchen nach:",
				JOptionPane.PLAIN_MESSAGE );
    pane.setOptions( options );
    pane.setWantsInput( true );
    if( selectedText != null ) {
      pane.setInitialSelectionValue( selectedText );
    } else {
      if( this.textFind != null ) {
	pane.setInitialSelectionValue( this.textFind );
      }
    }
    pane.setInitialValue( options[ 0 ] );
    pane.createDialog( this, "Suchen" ).setVisible( true );
    Object value = pane.getValue();
    if( value != null ) {
      if( value.equals( options[ 0 ] ) ) {
	value = pane.getInputValue();
	if( value != null ) {
	  String text = value.toString();
	  if( text != null ) {
	    if( text.length() > 0 ) {
	      this.textFind = text;
	      findText( Math.max(
				this.textArea.getCaretPosition(),
				this.textArea.getSelectionEnd() ) );
	    }
	  }
	}
      }
    }
  }


  private void doFindNext()
  {
    if( this.textFind == null ) {
      doFind();
    } else {
      findText( Math.max(
			this.textArea.getCaretPosition(),
			this.textArea.getSelectionEnd() ) );
    }
  }


  private void doImportLabelsClp()
  {
    boolean state = false;
    String  text  = EmuUtil.getClipboardText( this );
    if( text != null ) {
      Reader  reader = null;
      try {
	reader = new StringReader( text );
	state  = importLabels( ToolsUtil.readLabels( reader ), false );
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.doClose( reader );
      }
    }
    if( !state ) {
      BasicDlg.showErrorDlg(
		this,
		"Der Inhalt der Zwischenablage konnte nicht als\n"
			+ "Liste mit Marken interpretiert werden." );
    }
  }


  private void doImportLabelsFile() throws IOException
  {
    File file = this.lastLabelFile;
    if( file == null ) {
      file = Main.getLastDirFile( "label" );
    }
    file = EmuUtil.showFileOpenDlg(
			this,
			"Haltepunkte importieren",
			file,
			EmuUtil.getTextFileFilter() );
    if( file != null ) {
      boolean state  = false;
      Reader  reader = null;
      try {
	reader = new FileReader( file );
	state  = importLabels( ToolsUtil.readLabels( reader ), true );
      }
      finally {
	EmuUtil.doClose( reader );
      }
      if( state ) {
	this.lastLabelFile = file;
	Main.setLastFile( file, "label" );
      } else {
	BasicDlg.showErrorDlg(
		this,
		"Der Inhalt der Datei konnte nicht als Liste\n"
			+ "mit Marken interpretiert werden." );
      }
    }
  }


  private void doPrint()
  {
    PrintUtil.doPrint(
	this,
	new PlainTextPrintable(
		this.textArea.getText(),
		this.textArea.getTabSize(),
		this.lastFile != null ? this.lastFile.getName() : null ),
	"JKCEMU - Reassembler" );
  }


  private void doReass()
  {
    try {
      int    begAddr = this.docBegAddr.intValue();
      int    endAddr = begAddr;
      String text    = this.fldEndAddr.getText();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  endAddr = this.docEndAddr.intValue();
	}
      }
      if( (begAddr >= 0) && (endAddr >= begAddr) ) {
	this.begAddr = begAddr;
	this.endAddr = endAddr;
	reassemble();
      }
    }
    catch( NumberFormatException ex ) {
      BasicDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }


  private void doRemoveLabels()
  {
    if( this.addr2Labels != null ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die importierten Marken entfernen?" ) )
      {
	this.addr2Labels = null;
	this.mnuRemoveLabels.setEnabled( false );
	reassemble();
      }
    }
  }


  private void doSaveAs() throws IOException
  {
    File file = EmuUtil.showFileSaveDlg(
				this,
				"Textdatei speichern",
				this.lastFile != null ?
					this.lastFile
					: Main.getLastDirFile( "text" ),
				EmuUtil.getTextFileFilter() );
    if( file != null ) {
      Writer out = null;
      try {
	out = new BufferedWriter( new FileWriter( file ) );
	this.textArea.write( out );
	out.close();
	out           = null;
	this.lastFile = file;
	Main.setLastFile( file, "text" );
      }
      finally {
	EmuUtil.doClose( out );
      }
    }
  }


  private void doSourceExport() throws IOException
  {
    String text = createSourceText();
    if( text != null ) {
      File file = EmuUtil.showFileSaveDlg(
				this,
				"Als Quelltext speichern",
				this.lastFile != null ?
					this.lastFile
					: Main.getLastDirFile( "text" ),
				EmuUtil.getTextFileFilter() );
      if( file != null ) {
	BufferedWriter out = null;
	try {
	  out = new BufferedWriter( new FileWriter( file ) );

	  int len = text.length();
	  for( int i = 0; i < len; i++ ) {
	    char ch = text.charAt( i );
	    if( ch == '\n' ) {
	      out.newLine();
	    } else {
	      out.write( ch );
	    }
	  }
	  out.close();
	  out = null;
	  this.lastFile = file;
	  Main.setLastFile( file, "text" );
	}
	finally {
	  EmuUtil.doClose( out );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static void addLabel(
			Map<Integer,Set<String>> map,
			Integer                  addr,
			String                   labelName )
  {
    if( (addr != null) && (labelName != null) ) {
      if( !labelName.isEmpty() ) {
	Set<String> labelNames = map.get( addr );
	if( labelNames == null ) {
	  labelNames = new TreeSet<>();
	  map.put( addr, labelNames );
	}
	labelNames.add( labelName );
      }
    }
  }


  private static void appendSpaces( StringBuilder buf, int endPos )
  {
    for( int i = buf.length(); i < endPos; i++ )
      buf.append( (char) '\u0020' );
  }


  private static String createAddrLabel( int addr )
  {
    return String.format( "m%04X", addr );
  }


  private String createSourceText()
  {
    String rv = null;
    if( (this.begAddr >= 0) && (this.endAddr >= this.begAddr) ) {
      StringBuilder buf    = new StringBuilder( 0x4000 );
      EmuSys        emuSys = getEmuSys();

      // Pass 1: Adressen ermitteln
      Set<Integer> instrAddrs = new TreeSet<>();
      Set<Integer> destAddrs  = new TreeSet<>();
      int          addr       = this.begAddr;
      while( addr <= this.endAddr ) {
	instrAddrs.add( addr );

	int len = 0;
	if( emuSys != null ) {
	  len = emuSys.reassembleSysCall(
					this.memory,
					addr,
					buf,
					true,
					COL_SRC_MNEMONIC,
					COL_SRC_ARGS,
					COL_SRC_REMARK );
	}
	if( len > 0 ) {
	  addr += len;
	} else {
	  Z80ReassInstr instr = Z80Reassembler.reassInstruction(
						this.memory,
						addr );
	  if( instr != null ) {
	    Integer tmpAddr = instr.getAddress1();
	    if( tmpAddr != null ) {
	      destAddrs.add( tmpAddr );
	    }
	    tmpAddr = instr.getAddress2();
	    if( tmpAddr != null ) {
	      destAddrs.add( tmpAddr );
	    }
	    addr += instr.getLength();
	  } else {
	    addr++;
	  }
	}
      }

      /*
       * Alle Zieladressen entfernen,
       * die nicht den Anfang eines Befehls darstellen.
       */
      try {
	int n = destAddrs.size();
	if( n > 0 ) {
	  Integer[] ary = destAddrs.toArray( new Integer[ n ] );
	  if( ary != null ) {
	    for( Integer a : ary ) {
	      if( !instrAddrs.contains( a ) ) {
		destAddrs.remove( a );
	      }
	    }
	  }
	}
      }
      catch( Exception ex ) {}

      // Pass 2: eigentliche Texterzeugung
      addr = this.begAddr;
      buf.setLength( 0 );
      buf.append( "\tORG     " );
      if( addr >= 0xA000 ) {
	buf.append( (char) '0' );
      }
      buf.append( String.format( "%04XH\n", addr ) );

      int nExtLabels = 0;
      while( addr <= this.endAddr ) {
	boolean addrLabelEnabled = true;
	if( this.addr2Labels != null ) {
	  Set<String> labels = this.addr2Labels.get( addr );
	  if( labels != null ) {
	    int n = 0;
	    for( String label : labels ) {
	      if( label != null ) {
		if( !label.isEmpty() ) {
		  buf.append( label );
		  buf.append( ":\n" );
		  nExtLabels++;
		}
	      }
	    }
	  }
	}
	if( (nExtLabels != 1) && destAddrs.contains( addr ) ) {
	  buf.append( createAddrLabel( addr ) );
	  buf.append( ":\n" );
	}
	int len = 0;
	if( emuSys != null ) {
	  len = emuSys.reassembleSysCall(
					this.memory,
					addr,
					buf,
					true,
					COL_SRC_MNEMONIC,
					COL_SRC_ARGS,
					COL_SRC_REMARK );
	}
	if( len > 0 ) {
	  addr += len;
	} else {
	  int bol = buf.length();
	  for( int i = 0; i < COL_SRC_MNEMONIC; i++ ) {
	    buf.append( (char) '\u0020' );
	  }
	  Z80ReassInstr instr = Z80Reassembler.reassInstruction(
						this.memory,
						addr );
	  if( instr != null ) {
	    String s = instr.getName();
	    if( s != null ) {
	      if( s.startsWith( "*" ) ) {
		s = s.substring( 1 );
	      }
	      appendSpaces( buf, bol + COL_SRC_MNEMONIC );
	      buf.append( s );

	      Integer addr1 = instr.getAddress1();
	      s = getArgText(
			instr.getArg1(),
			addr1,
			instr.isIndirect1(),
			destAddrs );
	      if( s != null ) {
		appendSpaces( buf, bol + COL_SRC_ARGS );
		buf.append( s );

		Integer addr2 = instr.getAddress2();
		s = getArgText(
			instr.getArg2(),
			addr2,
			instr.isIndirect2(),
			destAddrs );
		if( s != null ) {
		  buf.append( (char) ',' );
		  buf.append( s );
		}
	      }
	    }
	    addr += instr.getLength();
	  } else {
	    buf.append( String.format(
				"  %02X",
				this.memory.getMemByte( addr++, true ) ) );
	  }
	  buf.append( (char) '\n' );
	}
      }
      rv = buf.toString();
    }
    return rv;
  }


  private boolean findText( int startPos )
  {
    boolean rv       = false;
    String  textFind = this.textFind;

    if( (textFind != null) && (textFind.length() > 0) ) {
      this.mnuFindNext.setEnabled( true );

      String textBase = this.textArea.getText();
      if( textBase == null ) {
	textBase = "";
      }
      textFind = textFind.toUpperCase();
      textBase = textBase.toUpperCase();
      if( startPos < 0 ) {
	startPos = 0;
      }
      int len     = textBase.length();
      int posFind = -1;
      if( startPos < len ) {
	posFind = textBase.indexOf( textFind, startPos );
      }
      if( (posFind >= 0) && (posFind < len) ) {
	toFront();
	this.textArea.requestFocus();
	this.textArea.setCaretPosition( posFind );
	this.textArea.select( posFind, posFind + textFind.length() );
	rv = true;
      }
    }
    if( !rv ) {
      if( startPos > 0 ) {
	rv = findText( 0 );
      } else {
	BasicDlg.showInfoDlg( this, "Text nicht gefunden!", "Text suchen" );
      }
    }
    return rv;
  }


  private String getArgText(
			String       arg,
			Integer      addr,
			boolean      indirect,
			Set<Integer> destAddrs )
  {
    String rv    = arg;
    String label = null;
    if( (this.addr2Labels != null) && (addr != null) ) {
      Set<String> labels = this.addr2Labels.get( addr );
      if( labels != null ) {
	for( String tmpLabel : labels ) {
	  if( tmpLabel != null ) {
	    if( !tmpLabel.isEmpty() ) {
	      if( label != null ) {
		label = null;
		break;
	      }
	      label = tmpLabel;
	    }
	  }
	}
      }
    }
    if( (label == null) && (addr != null) && (destAddrs != null) ) {
      if( destAddrs.contains( addr ) ) {
	label = createAddrLabel( addr );
      }
    }
    if( label != null ) {
      if( indirect ) {
	rv = String.format( "(%s)", label );
      } else {
	rv = label;
      }
    }
    return rv;
  }


  /*
   * EmuSys nur zurueckliefern,
   * wenn auf das Grundsystem zugegriffen wird
   */
  private EmuSys getEmuSys()
  {
    return this.memory instanceof EmuThread ?
			((EmuThread) this.memory).getEmuSys()
			: null;
  }


  private boolean importLabels(
			jkcemu.tools.Label[] labels,
			boolean              removeObsolete )
  {
    boolean             rv            = false;
    Map<String,Integer> oldLabel2Addr = null;
    if( !removeObsolete && (this.addr2Labels != null) ) {
      oldLabel2Addr      = new HashMap<>();
      Set<Integer> addrs = this.addr2Labels.keySet();
      if( addrs != null ) {
	for( Integer addr : addrs ) {
	  Collection<String> labelNames = this.addr2Labels.get( addr );
	  if( labelNames != null ) {
	    for( String labelName : labelNames ) {
	      oldLabel2Addr.put( labelName, addr );
	    }
	  }
	}
      }
    }
    if( labels != null ) {
      Map<Integer,Set<String>> map = null;
      for( jkcemu.tools.Label label : labels ) {
	String labelName = label.getLabelName();
	if( labelName != null ) {
	  if( !labelName.isEmpty() ) {
	    if( map == null ) {
	      map = new HashMap<>();
	    }
	    addLabel( map, label.intValue(), labelName );
	    if( oldLabel2Addr != null ) {
	      oldLabel2Addr.remove( labelName );
	    }
	  }
	}
      }
      if( map != null ) {
	if( !removeObsolete && (oldLabel2Addr != null) ) {
	  if( !oldLabel2Addr.isEmpty() ) {
	    removeObsolete = BasicDlg.showYesNoDlg(
		this,
		"Sollen die bereits vorher importierten und im"
			+ " jetzigen Import\n"
			+ "nicht mehr vorhandenen Marken"
			+ " entfernt werden?" );
	    if( !removeObsolete ) {
	      Set<Map.Entry<String,Integer>> c = oldLabel2Addr.entrySet();
	      if( c != null ) {
		for( Map.Entry<String,Integer> label : c ) {
		  addLabel( map, label.getValue(), label.getKey() );
		}
	      }
	    }
	  }
	}
	this.addr2Labels = map;
	rv               = true;
        this.mnuRemoveLabels.setEnabled( true );
	reassemble();
      }
    }
    return rv;
  }


  private void reassemble()
  {
    if( (this.begAddr >= 0) && (this.endAddr >= this.begAddr) ) {
      StringBuilder buf    = new StringBuilder( 0x4000 );
      EmuSys        emuSys = getEmuSys();
      int           addr   = this.begAddr;
      while( addr <= endAddr ) {
	if( this.addr2Labels != null ) {
	  Integer     addrObj    = new Integer( addr );
	  Set<String> labelNames = this.addr2Labels.get( addrObj );
	  if( labelNames != null ) {
	    if( !labelNames.isEmpty() ) {
	      for( String labelName : labelNames ) {
		buf.append( labelName );
		buf.append( ":\n" );
	      }
	    }
	  }
	}
	int len = 0;
	if( emuSys != null ) {
	  len = emuSys.reassembleSysCall(
					this.memory,
					addr,
					buf,
					false,
					COL_MNEMONIC,
					COL_ARGS,
					COL_REMARK );
	}
	if( len > 0 ) {
	  addr += len;
	} else {
	  int bol = buf.length();
	  buf.append( String.format( "%04X", addr ) );

	  Z80ReassInstr instr = Z80Reassembler.reassInstruction(
						this.memory,
						addr );
	  if( instr != null ) {
	    buf.append( (char) '\u0020' );
	    len = instr.getLength();
	    for( int i = 0; i < len; i++ ) {
	      buf.append( String.format( " %02X", instr.getByte( i ) ) );
	      addr++;
	    }

	    String s = instr.getName();
	    if( s != null ) {
	      appendSpaces( buf, bol + COL_MNEMONIC );
	      buf.append( s );

	      Integer addr1 = instr.getAddress1();
	      s = getArgText(
			instr.getArg1(),
			addr1,
			instr.isIndirect1(),
			null );
	      if( s != null ) {
		appendSpaces( buf, bol + COL_ARGS );
		buf.append( s );

		Integer addr2 = instr.getAddress2();
		s = getArgText(
			instr.getArg2(),
			addr2,
			instr.isIndirect2(),
			null );
		if( s != null ) {
		  buf.append( (char) ',' );
		  buf.append( s );
		}
	      }
	    }
	  } else {
	    buf.append( String.format(
				"  %02X",
				this.memory.getMemByte( addr++, true ) ) );
	  }
	  buf.append( (char) '\n' );
	}
      }
      setText( buf.toString() );
      this.textArea.requestFocus();
      if( buf.length() > 0 ) {
	this.mnuSourceCopy.setEnabled( true );
	this.mnuSourceExport.setEnabled( true );
	this.mnuPrint.setEnabled( true );
	this.mnuSaveAs.setEnabled( true );
	this.mnuFind.setEnabled( true );
	this.mnuSelectAll.setEnabled( true );
      }
    }
  }


  private void setText( String text )
  {
    try {
      this.textArea.setText( text );
      this.textArea.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }
}

