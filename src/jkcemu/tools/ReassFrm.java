/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer Reassembler
 */

package jkcemu.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.HexDocument;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.ReplyTextDlg;
import jkcemu.base.ScreenFrm;
import jkcemu.file.FileUtil;
import jkcemu.print.PlainTextPrintable;
import jkcemu.print.PrintOptionsDlg;
import jkcemu.print.PrintUtil;
import jkcemu.programming.assembler.AsmLabel;
import jkcemu.text.TextFinder;
import jkcemu.text.TextUtil;
import jkcemu.tools.debugger.DebugFrm;
import z80emu.Z80Memory;
import z80emu.Z80Reassembler;
import z80emu.Z80ReassInstr;


public class ReassFrm
		extends BaseFrm
		implements CaretListener, PopupMenuOwner
{
  private static final String HELP_PAGE = "/help/tools/reassembler.htm";

  private static final int COL_MNEMONIC = 23;
  private static final int COL_ARGS     = COL_MNEMONIC + 8;
  private static final int COL_REMARK   = COL_ARGS + 18;

  private static final int COL_SRC_MNEMONIC = 8;
  private static final int COL_SRC_ARGS     = COL_SRC_MNEMONIC + 8;
  private static final int COL_SRC_REMARK   = COL_SRC_ARGS + 18;

  private ScreenFrm                  screenFrm;
  private Z80Memory                  memory;
  private boolean                    notified;
  private int                        begAddr;
  private int                        endAddr;
  private int                        clickAddr;
  private String                     clickLabel;
  private File                       lastFile;
  private File                       lastLabelFile;
  private String                     labelPrefix;
  private Map<Integer,Set<String>>   addr2Labels;
  private TextFinder                 textFinder;
  private JTextArea                  textArea;
  private JTextComponent             selectionFld;
  private JMenuItem                  mnuReass;
  private JMenuItem                  mnuImportLabelsFile;
  private JMenuItem                  mnuImportLabelsClp;
  private JMenuItem                  mnuRemoveLabels;
  private JMenuItem                  mnuPrintOptions;
  private JMenuItem                  mnuPrint;
  private JMenuItem                  mnuSaveAs;
  private JMenuItem                  mnuSourceOpen;
  private JMenuItem                  mnuSourceExport;
  private JMenuItem                  mnuClose;
  private JMenuItem                  mnuCopy;
  private JMenuItem                  mnuFind;
  private JMenuItem                  mnuFindNext;
  private JMenuItem                  mnuFindPrev;
  private JMenuItem                  mnuSelectAll;
  private JMenuItem                  mnuHelpContent;
  private JTextField                 fldBegAddr;
  private JTextField                 fldEndAddr;
  private HexDocument                docBegAddr;
  private HexDocument                docEndAddr;
  private JPopupMenu                 popupMnu;
  private JMenuItem                  popupCopy;
  private JMenuItem                  popupFind;
  private JMenuItem                  popupFindPrev;
  private JMenuItem                  popupFindNext;
  private JMenuItem                  popupBreak;
  private JMenuItem                  popupSelectAll;


  public ReassFrm( ScreenFrm screenFrm, Z80Memory memory )
  {
    this.screenFrm      = screenFrm;
    this.memory         = memory;
    this.notified       = false;
    this.begAddr        = -1;
    this.endAddr        = -1;
    this.clickAddr      = -1;
    this.clickLabel     = null;
    this.labelPrefix    = "M_";
    this.lastFile       = null;
    this.lastLabelFile  = null;
    this.addr2Labels    = null;
    this.textFinder     = null;
    this.selectionFld   = null;
    this.textArea       = GUIFactory.createCodeArea();
    setTitle( "JKCEMU Reassembler" );


    // Popup-Menu
    this.popupMnu = GUIFactory.createPopupMenu();

    this.popupCopy = createMenuItemCopy( false );
    this.popupCopy.setEnabled( false );
    this.popupMnu.add( this.popupCopy );
    this.popupMnu.addSeparator();

    this.popupFind = createMenuItemOpenFind( false );
    this.popupFind.setEnabled( false );
    this.popupMnu.add( this.popupFind );

    this.popupFindNext = createMenuItemFindNext( false );
    this.popupFindNext.setEnabled( false );
    this.popupMnu.add( this.popupFindNext );

    this.popupFindPrev = createMenuItemFindPrev( false );
    this.popupFindPrev.setEnabled( false );
    this.popupMnu.add( this.popupFindPrev );
    this.popupMnu.addSeparator();

    this.popupBreak = createMenuItem(
			"Im Debugger Halte-/Log-Punkt hinzuf\u00FCgen..." );
    this.popupBreak.setEnabled( false );
    this.popupMnu.add( this.popupBreak );
    this.popupMnu.addSeparator();

    this.popupSelectAll = createMenuItemSelectAll( false );
    this.popupSelectAll.setEnabled( false );
    this.popupMnu.add( this.popupSelectAll );


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuReass = createMenuItemWithStandardAccelerator(
						"Reassemblieren",
						KeyEvent.VK_R );
    mnuFile.add( this.mnuReass );
    mnuFile.addSeparator();

    this.mnuImportLabelsFile = createMenuItem(
			"Marken aus Datei importieren..." );
    mnuFile.add( this.mnuImportLabelsFile );

    this.mnuImportLabelsClp = createMenuItem(
			"Marken aus Zwischenablage importieren" );
    mnuFile.add( this.mnuImportLabelsClp );

    this.mnuRemoveLabels = createMenuItem(
			"Importierte Marken entfernen" );
    this.mnuRemoveLabels.setEnabled( false );
    mnuFile.add( this.mnuRemoveLabels );
    mnuFile.addSeparator();

    this.mnuSourceOpen = createMenuItem(
			"Als Quelltext im Texteditor \u00F6ffnen..." );
    this.mnuSourceOpen.setEnabled( false );
    mnuFile.add( this.mnuSourceOpen );

    this.mnuSourceExport = createMenuItem( "Als Quelltext exportieren..." );
    this.mnuSourceExport.setEnabled( false );
    mnuFile.add( this.mnuSourceExport );
    mnuFile.addSeparator();

    this.mnuPrintOptions = createMenuItemOpenPrintOptions();
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = createMenuItemOpenPrint( true );
    this.mnuPrint.setEnabled( false );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuSaveAs = createMenuItemSaveAs( true );
    this.mnuSaveAs.setEnabled( false );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.mnuCopy = createMenuItemCopy( true );
    this.mnuCopy.setEnabled( false );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuFind = createMenuItemOpenFind( true );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createMenuItemFindNext( true );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );

    this.mnuFindPrev = createMenuItemFindPrev( true );
    this.mnuFindPrev.setEnabled( false );
    mnuEdit.add( this.mnuFindPrev );
    mnuEdit.addSeparator();

    this.mnuSelectAll = createMenuItemSelectAll( true );
    this.mnuSelectAll.setEnabled( false );
    mnuEdit.add( this.mnuSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = GUIFactory.createMenuItem(
				"Hilfe zum Reassembler..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuEdit, mnuHelp ) );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Kopfbereich
    JPanel panelHead = GUIFactory.createPanel( new GridBagLayout() );
    add( panelHead, BorderLayout.NORTH );

    GridBagConstraints gbcHead = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panelHead.add( GUIFactory.createLabel( "Anfangsadresse:" ), gbcHead );

    this.docBegAddr = new HexDocument( 4, "Anfangsadresse" );
    this.fldBegAddr = GUIFactory.createTextField( this.docBegAddr, 4 );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldBegAddr, gbcHead );

    gbcHead.fill    = GridBagConstraints.NONE;
    gbcHead.weightx = 0.0;
    gbcHead.gridx++;
    panelHead.add( GUIFactory.createLabel( "Endadresse:" ), gbcHead );

    this.docEndAddr = new HexDocument( 4, "Endadresse" );
    this.fldEndAddr = GUIFactory.createTextField( this.docEndAddr, 4 );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldEndAddr, gbcHead );


    // Ergebnisbereich
    this.textArea.setColumns( 58 );
    this.textArea.setRows( 20 );
    this.textArea.setEditable( false );
    this.textArea.setMargin( new Insets( 5, 5, 5, 5 ) );
    add( GUIFactory.createScrollPane( this.textArea ), BorderLayout.CENTER );


    // sonstiges
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
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
    importLabels( labels, true, true );
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
	boolean state = TextUtil.isTextSelected( this.selectionFld );
	this.mnuCopy.setEnabled( state );
	this.popupCopy.setEnabled( state );
      }
    }
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.mnuHelpContent.addActionListener( this );
      this.fldBegAddr.addActionListener( this );
      this.fldBegAddr.addCaretListener( this );
      this.fldEndAddr.addActionListener( this );
      this.fldEndAddr.addCaretListener( this );
      this.textArea.addCaretListener( this );
      this.textArea.addMouseListener( this );
    }
  }


  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object src = e.getSource();
      if( src == this.fldBegAddr ) {
	this.fldEndAddr.requestFocus();
	rv = true;
      }
      else if( (src == this.fldEndAddr) || (src == this.mnuReass) ) {
	doReass();
	rv = true;
      }
      else if( src == this.mnuImportLabelsClp ) {
	rv = true;
	doImportLabelsClp();
      }
      else if( src == this.mnuImportLabelsFile ) {
	doImportLabelsFile();
	rv = true;
      }
      else if( src == this.mnuRemoveLabels ) {
	doRemoveLabels();
	rv = true;
      }
      else if( src == this.mnuSourceOpen ) {
	String text = createSourceText();
	if( text != null ) {
	  this.screenFrm.openText( text );
	}
	rv = true;
      }
      else if( src == this.mnuSourceExport ) {
	doSourceExport();
	rv = true;
      }
      else if( src == this.mnuPrintOptions ) {
	PrintOptionsDlg.showPrintOptionsDlg( this, true, true );
	rv = true;
      }
      else if( src == this.mnuPrint ) {
	doPrint();
	rv = true;
      }
      else if( src == this.mnuSaveAs ) {
	doSaveAs();
	rv = true;
      }
      else if( src == this.mnuClose ) {
	doClose();
	rv = true;
      }
      else if( (src == this.mnuCopy) || (src == this.popupCopy) ) {
	if( this.selectionFld != null ) {
	  this.selectionFld.copy();
	}
	rv = true;
      }
      else if( (src == this.mnuFind) || (src == this.popupFind) ) {
	doFind();
	rv = true;
      }
      else if( (src == this.mnuFindPrev) || (src == this.popupFindPrev) ) {
	if( this.textFinder != null ) {
	  this.textFinder.findPrev( this.textArea );
	}
	rv = true;
      }
      else if( (src == this.mnuFindNext) || (src == this.popupFindNext) ) {
	if( this.textFinder != null ) {
	  this.textFinder.findNext( this.textArea );
	}
	rv = true;
      }
      else if( (src == this.mnuSelectAll) || (src == this.popupSelectAll) ) {
	this.textArea.requestFocus();
	this.textArea.selectAll();
	rv = true;
      }
      else if( src == this.mnuHelpContent ) {
	HelpFrm.openPage( HELP_PAGE );
	rv = true;
      }
      else if( src == this.popupCopy ) {
	this.textArea.copy();
	rv = true;
      }
      else if( src == this.popupBreak ) {
	doCreateBreakpoint();
	rv = true;
      }
      else if( src == this.popupSelectAll ) {
	this.textArea.selectAll();
	rv = true;
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
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
      this.mnuSourceOpen.setEnabled( false );
      this.mnuSourceExport.setEnabled( false );
      this.mnuFind.setEnabled( false );
      this.mnuFindNext.setEnabled( false );
      this.mnuFindPrev.setEnabled( false );
      this.mnuSelectAll.setEnabled( false );
      this.popupFind.setEnabled( false );
      this.popupFindNext.setEnabled( false );
      this.popupFindPrev.setEnabled( false );
      this.popupSelectAll.setEnabled( false );
      setText( "" );
    }
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.mnuHelpContent.removeActionListener( this );
      this.fldBegAddr.removeActionListener( this );
      this.fldBegAddr.removeCaretListener( this );
      this.fldEndAddr.removeActionListener( this );
      this.fldEndAddr.removeCaretListener( this );
      this.textArea.removeCaretListener( this );
      this.textArea.removeMouseListener( this );
    }
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( newEmuSys != null ) {
      final int millis = newEmuSys.getDefaultPromptAfterResetMillisMax();
      if( millis > 0 ) {
	javax.swing.Timer timer = new javax.swing.Timer(
			millis,
			new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
			  {
			    reassemble();
			  }
			} );
	timer.setRepeats( false );
	timer.start();
      } else {
	reassemble();
      }
    }
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      if( c == this.textArea ) {
	StringBuilder labelBuf = new StringBuilder();
	this.clickAddr         = ToolUtil.getReassAddr(
						this.textArea,
						e.getPoint(),
						labelBuf );
	if( (this.clickAddr >= 0) && (labelBuf.length() > 0) ) {
	  this.clickLabel = labelBuf.toString();
	} else {
	  this.clickLabel = null;
	}
	this.popupCopy.setEnabled(
		this.textArea.getSelectionStart()
			!= this.textArea.getSelectionEnd() );
	this.popupBreak.setEnabled( this.clickAddr >= 0 );
	this.popupMnu.show( c, e.getX(), e.getY() );
	rv = true;
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
	  debugFrm.openBreakpointAdd( this.clickLabel, this.clickAddr, 0 );
	}
      }
    }
  }


  private void doFind()
  {
    this.textFinder = TextFinder.openFindDlg(
					this.textArea,
					this.textFinder );
    if( textFinder != null ) {
      this.mnuFindPrev.setEnabled( true );
      this.mnuFindNext.setEnabled( true );
      this.popupFindPrev.setEnabled( true );
      this.popupFindNext.setEnabled( true );
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
	state  = importLabels( ToolUtil.readLabels( reader ), false, false );
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilently( reader );
      }
    }
    if( !state ) {
      BaseDlg.showErrorDlg(
		this,
		"Der Inhalt der Zwischenablage konnte nicht als\n"
			+ "Liste mit Marken interpretiert werden." );
    }
  }


  private void doImportLabelsFile() throws IOException
  {
    File file = this.lastLabelFile;
    if( file == null ) {
      file = Main.getLastDirFile( Main.FILE_GROUP_LABEL );
    }
    file = FileUtil.showFileOpenDlg(
			this,
			"Haltepunkte importieren",
			FileUtil.getDirectory( file ),
			FileUtil.getTextFileFilter() );
    if( file != null ) {
      boolean state  = false;
      Reader  reader = null;
      try {
	reader = new FileReader( file );
	state  = importLabels( ToolUtil.readLabels( reader ), false, true );
      }
      finally {
	EmuUtil.closeSilently( reader );
      }
      if( state ) {
	this.lastLabelFile = file;
	Main.setLastFile( file, Main.FILE_GROUP_LABEL );
      } else {
	BaseDlg.showErrorDlg(
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
		this.textArea.getFont(),
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
      BaseDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }


  private void doRemoveLabels()
  {
    if( this.addr2Labels != null ) {
      if( BaseDlg.showYesNoDlg(
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
    File file = FileUtil.showFileSaveDlg(
			this,
			"Textdatei speichern",
			this.lastFile != null ?
				this.lastFile
				: Main.getLastDirFile( Main.FILE_GROUP_TEXT ),
			FileUtil.getTextFileFilter() );
    if( file != null ) {
      Writer out = null;
      try {
	out = new BufferedWriter( new FileWriter( file ) );
	this.textArea.write( out );
	out.close();
	out           = null;
	this.lastFile = file;
	Main.setLastFile( file, Main.FILE_GROUP_TEXT );
      }
      finally {
	EmuUtil.closeSilently( out );
      }
    }
  }


  private void doSourceExport() throws IOException
  {
    String text = createSourceText();
    if( text != null ) {
      File file = FileUtil.showFileSaveDlg(
		this,
		"Als Quelltext speichern",
		this.lastFile != null ?
			this.lastFile
			: Main.getLastDirFile( Main.FILE_GROUP_LABEL ),
		FileUtil.getTextFileFilter() );
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
	  Main.setLastFile( file, Main.FILE_GROUP_TEXT );
	}
	finally {
	  EmuUtil.closeSilently( out );
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
      buf.append( '\u0020' );
  }


  private String createAddrLabel( int addr )
  {
    return String.format( "%s%04X", this.labelPrefix, addr );
  }


  private String createSourceText()
  {
    String rv = null;
    if( (this.begAddr >= 0) && (this.endAddr >= this.begAddr) ) {
      String labelPrefix = ReplyTextDlg.showDlg(
					this,
					"Prefix f\u00FCr Marken:",
					"Eingabe",
					this.labelPrefix );
      if( labelPrefix != null ) {
	boolean prefixOK  = false;
	int     prefixLen = labelPrefix.length();
	if( prefixLen > 0 ) {
	  prefixOK = AsmLabel.isIdentifierStart( labelPrefix.charAt( 0 ) );
	  for( int i = 1; i < prefixLen; i++ ) {
	    prefixOK &= AsmLabel.isIdentifierPart( labelPrefix.charAt( i ) );
	  }
	}
	if( prefixOK ) {
	  this.labelPrefix     = labelPrefix;
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
	    buf.append( '0' );
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
		buf.append( '\u0020' );
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
		      buf.append( ',' );
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
	      buf.append( '\n' );
	    }
	  }
	  rv = buf.toString();
	} else {
	  BaseDlg.showErrorDlg(
		this, 
		"Der Prefix entspricht nicht den Namenskonventionen"
			+ " f\u00FCr Assembler-Marken." );
	}
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
			boolean              addrLabelsOnly,
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
	if( label.isAddress() ) {
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
      }
      if( map != null ) {
	if( !removeObsolete && (oldLabel2Addr != null) ) {
	  if( !oldLabel2Addr.isEmpty() ) {
	    removeObsolete = BaseDlg.showYesNoDlg(
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
	  Set<String> labelNames = this.addr2Labels.get( addr );
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
	    buf.append( '\u0020' );
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
		  buf.append( ',' );
		  buf.append( s );
		}
	      }
	    }
	  } else {
	    buf.append( String.format(
				"  %02X",
				this.memory.getMemByte( addr++, true ) ) );
	  }
	  buf.append( '\n' );
	}
      }
      setText( buf.toString() );
      this.textArea.requestFocus();
      if( buf.length() > 0 ) {
	this.mnuSourceOpen.setEnabled( true );
	this.mnuSourceExport.setEnabled( true );
	this.mnuPrint.setEnabled( true );
	this.mnuSaveAs.setEnabled( true );
	this.mnuFind.setEnabled( true );
	this.mnuSelectAll.setEnabled( true );
	this.popupFind.setEnabled( true );
	this.popupSelectAll.setEnabled( true );
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
