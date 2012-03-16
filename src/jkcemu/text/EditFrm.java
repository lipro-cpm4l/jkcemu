/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Editor fuer Dokumente
 */

package jkcemu.text;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.text.CharacterIterator;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import jkcemu.Main;
import jkcemu.programming.*;
import jkcemu.programming.assembler.*;
import jkcemu.programming.basic.*;
import jkcemu.base.*;
import jkcemu.print.*;


public class EditFrm extends BasicFrm implements
					ChangeListener,
					DropTargetListener,
					FlavorListener
{
  private static final String DEFAULT_STATUS_TEXT = "Bereit";
  private static javax.swing.filechooser.FileFilter[] textFileFilters = null;

  private ScreenFrm                screenFrm;
  private EmuThread                emuThread;
  private JMenuItem                mnuFileNew;
  private JMenuItem                mnuFileOpen;
  private JMenuItem                mnuFileOpenCharset;
  private JMenuItem                mnuFileSave;
  private JMenuItem                mnuFileSaveAs;
  private JMenuItem                mnuFilePrintOptions;
  private JMenuItem                mnuFilePrint;
  private JMenuItem                mnuFileProperties;
  private JMenuItem                mnuFileTabClose;
  private JMenuItem                mnuFileClose;
  private JMenuItem                mnuEditUndo;
  private JMenuItem                mnuEditCut;
  private JMenuItem                mnuEditCopy;
  private JMenuItem                mnuEditPaste;
  private JMenuItem                mnuEditUpper;
  private JMenuItem                mnuEditLower;
  private JMenuItem                mnuEditTabSize;
  private JMenuItem                mnuEditTabToSpaces;
  private JMenuItem                mnuEditShiftIn;
  private JMenuItem                mnuEditShiftOut;
  private JMenuItem                mnuEditShiftWidth;
  private JMenuItem                mnuEditUmlautGerToUni;
  private JMenuItem                mnuEditUmlautDosToUni;
  private JMenuItem                mnuEditFind;
  private JMenuItem                mnuEditFindNext;
  private JMenuItem                mnuEditReplace;
  private JMenuItem                mnuEditBracket;
  private JMenuItem                mnuEditGoto;
  private JMenuItem                mnuEditSelectAll;
  private JMenuItem                mnuPrgAssemble;
  private JMenuItem                mnuPrgAssembleRun;
  private JMenuItem                mnuPrgAssembleOpt;
  private JMenuItem                mnuPrgCompile;
  private JMenuItem                mnuPrgCompileRun;
  private JMenuItem                mnuPrgCompileOpt;
  private JMenuItem                mnuPrgCancel;
  private JMenuItem                mnuPrjOpen;
  private JMenuItem                mnuPrjSave;
  private JMenuItem                mnuPrjSaveAs;
  private JMenuItem                mnuHelpContent;
  private JPopupMenu               mnuPopup;
  private JMenuItem                mnuPopupCut;
  private JMenuItem                mnuPopupCopy;
  private JMenuItem                mnuPopupPaste;
  private JMenuItem                mnuPopupFind;
  private JMenuItem                mnuPopupFindNext;
  private JMenuItem                mnuPopupReplace;
  private JButton                  btnNew;
  private JButton                  btnOpen;
  private JButton                  btnSave;
  private JButton                  btnPrint;
  private JButton                  btnClose;
  private JButton                  btnUndo;
  private JButton                  btnCut;
  private JButton                  btnCopy;
  private JButton                  btnPaste;
  private JButton                  btnFind;
  private JTabbedPane              tabbedPane;
  private JLabel                   labelStatus;
  private java.util.List<EditText> editTexts;
  private boolean                  hasTextFind;
  private boolean                  findIgnoreCase;
  private String                   textFind;
  private String                   textReplace;
  private Clipboard                clipboard;
  private LogFrm                   logFrm;
  private PrgThread                prgThread;
  private AbstractOptionsDlg       prgOptionsDlg;
  private EditText                 unusedEditText;
  private int                      lastNewTextNum;
  private int                      shiftWidth;
  private boolean                  shiftUseTabs;


  public EditFrm( ScreenFrm screenFrm )
  {
    this.screenFrm       = screenFrm;
    this.emuThread       = screenFrm.getEmuThread();
    this.editTexts       = new ArrayList<EditText>();
    this.hasTextFind     = false;
    this.findIgnoreCase  = true;
    this.textFind        = null;
    this.textReplace     = null;
    this.clipboard       = null;
    this.logFrm          = null;
    this.prgThread       = null;
    this.prgOptionsDlg   = null;
    this.unusedEditText  = null;
    this.lastNewTextNum  = 0;
    this.shiftWidth      = Main.getIntProperty(
					"jkcemu.texteditor.shift.width",
					2 );
    this.shiftUseTabs   = Main.getBooleanProperty(
					"jkcemu.texteditor.shift.use_tabs",
					true );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuFileNew = createJMenuItem(
		"Neuer Text",
		KeyStroke.getKeyStroke( KeyEvent.VK_N, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileNew );

    this.mnuFileOpen = createJMenuItem(
		"\u00D6ffnen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileOpen );

    this.mnuFileOpenCharset = createJMenuItem(
				"\u00D6ffnen mit Zeichensatz..." );
    mnuFile.add( this.mnuFileOpenCharset );
    mnuFile.addSeparator();

    this.mnuFileSave = createJMenuItem(
		"Speichern",
		KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileSave );

    this.mnuFileSaveAs = createJMenuItem(
		"Speichern unter...",
		KeyStroke.getKeyStroke(
			KeyEvent.VK_S, Event.CTRL_MASK | Event.SHIFT_MASK) );
    mnuFile.add( this.mnuFileSaveAs );
    mnuFile.addSeparator();

    this.mnuFilePrintOptions = createJMenuItem( "Druckoptionen..." );
    mnuFile.add( this.mnuFilePrintOptions );

    this.mnuFilePrint = createJMenuItem(
		"Drucken...",
		KeyStroke.getKeyStroke( KeyEvent.VK_P, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFilePrint );
    mnuFile.addSeparator();

    this.mnuFileProperties = createJMenuItem( "Eigenschaften..." );
    mnuFile.add( this.mnuFileProperties );
    mnuFile.addSeparator();

    this.mnuFileTabClose = createJMenuItem(
		"Unterfenster schlie\u00DFen",
		KeyStroke.getKeyStroke( KeyEvent.VK_W, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileTabClose );


    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuEditUndo = createJMenuItem(
		"R\u00FCckg\u00E4ngig",
		KeyStroke.getKeyStroke( KeyEvent.VK_Z, Event.CTRL_MASK ) );
    this.mnuEditUndo.setEnabled( false );
    mnuEdit.add( this.mnuEditUndo );
    mnuEdit.addSeparator();

    this.mnuEditCut = createJMenuItem(
		"Ausschneiden",
		KeyStroke.getKeyStroke( KeyEvent.VK_X, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditCut );

    this.mnuEditCopy = createJMenuItem(
		"Kopieren",
		KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditCopy );

    this.mnuEditPaste = createJMenuItem(
		"Einf\u00FCgen",
		KeyStroke.getKeyStroke( KeyEvent.VK_V, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditPaste );
    mnuEdit.addSeparator();

    this.mnuEditUpper = createJMenuItem(
		"In Gro\u00DFbuchstaben wandeln",
		KeyStroke.getKeyStroke( KeyEvent.VK_U, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditUpper );

    this.mnuEditLower = createJMenuItem(
		"In Kleinbuchstaben wandeln",
		KeyStroke.getKeyStroke( KeyEvent.VK_L, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditLower );
    mnuEdit.addSeparator();

    this.mnuEditTabSize = createJMenuItem(
		"Tabulatorbreite...",
		KeyStroke.getKeyStroke( KeyEvent.VK_T, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditTabSize );

    this.mnuEditTabToSpaces = createJMenuItem(
		"Tabulatoren durch Leerzeichen ersetzen" );
    mnuEdit.add( this.mnuEditTabToSpaces );
    mnuEdit.addSeparator();

    this.mnuEditShiftIn = createJMenuItem(
		"Einr\u00FCcken",
		KeyStroke.getKeyStroke( KeyEvent.VK_I, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditShiftIn );

    this.mnuEditShiftOut = createJMenuItem(
		"Herausr\u00FCcken",
		KeyStroke.getKeyStroke(
				KeyEvent.VK_I,
				Event.CTRL_MASK | Event.SHIFT_MASK ) );
    mnuEdit.add( this.mnuEditShiftOut );

    this.mnuEditShiftWidth = createJMenuItem( "Einr\u00FCcktiefe..." );
    mnuEdit.add( this.mnuEditShiftWidth );
    mnuEdit.addSeparator();

    JMenu mnuEditUmlaut = new JMenu( "Deutsche Umlaute konvertieren" );

    this.mnuEditUmlautGerToUni = createJMenuItem(
			"\"[\\]{|}~\" in Umlaute konvertieren" );
    mnuEditUmlaut.add( this.mnuEditUmlautGerToUni );

    this.mnuEditUmlautDosToUni = createJMenuItem(
			"Umlaute im DOS-Zeichensatz konvertieren" );
    mnuEditUmlaut.add( this.mnuEditUmlautDosToUni );

    mnuEdit.add( mnuEditUmlaut );
    mnuEdit.addSeparator();

    this.mnuEditFind = createJMenuItem(
		"Suchen und Ersetzen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_F, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditFind );

    this.mnuEditFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    mnuEdit.add( this.mnuEditFindNext );

    this.mnuEditReplace = createJMenuItem(
		"Ersetzen",
		KeyStroke.getKeyStroke( KeyEvent.VK_R, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditReplace );
    mnuEdit.addSeparator();

    this.mnuEditBracket = createJMenuItem(
		"Klammer pr\u00FCfen",
		KeyStroke.getKeyStroke( KeyEvent.VK_K, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditBracket );

    this.mnuEditGoto = createJMenuItem(
		"Gehe zu Zeile...",
		KeyStroke.getKeyStroke( KeyEvent.VK_G, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditGoto );

    this.mnuEditSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    mnuEdit.add( this.mnuEditSelectAll );


    // Menu Programmierung
    JMenu mnuPrg = new JMenu( "Programmierung" );
    mnuPrg.setMnemonic( KeyEvent.VK_P );
    mnuBar.add( mnuPrg );

    this.mnuPrgCompile = createJMenuItem(
		"BASIC-Programm compilieren",
		KeyStroke.getKeyStroke( KeyEvent.VK_F9, 0 ) );
    mnuPrg.add( this.mnuPrgCompile );

    this.mnuPrgCompileRun = createJMenuItem(
		"BASIC-Programm compilieren und starten",
		KeyStroke.getKeyStroke( KeyEvent.VK_F9, Event.SHIFT_MASK ) );
    mnuPrg.add( this.mnuPrgCompileRun );

    this.mnuPrgCompileOpt = createJMenuItem(
		"BASIC-Programm compilieren mit..." );
    mnuPrg.add( this.mnuPrgCompileOpt );
    mnuPrg.addSeparator();

    this.mnuPrgAssemble = createJMenuItem(
		"Assemblieren",
		KeyStroke.getKeyStroke( KeyEvent.VK_F8, 0 ) );
    mnuPrg.add( this.mnuPrgAssemble );

    this.mnuPrgAssembleRun = createJMenuItem(
		"Assemblieren und Programm starten",
		KeyStroke.getKeyStroke( KeyEvent.VK_F8, Event.SHIFT_MASK ) );
    mnuPrg.add( this.mnuPrgAssembleRun );

    this.mnuPrgAssembleOpt = createJMenuItem( "Assemblieren mit..." );
    mnuPrg.add( this.mnuPrgAssembleOpt );
    mnuPrg.addSeparator();

    this.mnuPrgCancel = createJMenuItem(
		"Assembler/Compiler abbrechen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F7, 0 ) );
    this.mnuPrgCancel.setEnabled( false );
    mnuPrg.add( this.mnuPrgCancel );
    mnuPrg.addSeparator();

    this.mnuPrjOpen = createJMenuItem( "Projekt \u00F6ffnen..." );
    mnuPrg.add( this.mnuPrjOpen );

    this.mnuPrjSave = createJMenuItem( "Projekt speichern" );
    mnuPrg.add( this.mnuPrjSave );

    this.mnuPrjSaveAs = createJMenuItem( "Projekt speichern unter..." );
    mnuPrg.add( this.mnuPrjSaveAs );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupCut = createJMenuItem( "Ausschneiden" );
    this.mnuPopup.add( this.mnuPopupCut );

    this.mnuPopupCopy = createJMenuItem( "Kopieren" );
    this.mnuPopup.add( this.mnuPopupCopy );

    this.mnuPopupPaste = createJMenuItem( "Einf\u00FCgen" );
    this.mnuPopup.add( this.mnuPopupPaste );
    this.mnuPopup.addSeparator();

    this.mnuPopupFind = createJMenuItem( "Suchen..." );
    this.mnuPopup.add( this.mnuPopupFind );

    this.mnuPopupFindNext = createJMenuItem( "Weitersuchen" );
    this.mnuPopup.add( this.mnuPopupFindNext );

    this.mnuPopupReplace = createJMenuItem( "Ersetzen" );
    this.mnuPopup.add( this.mnuPopupReplace );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 0, 0, 0 ),
						0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );

    this.btnNew = createImageButton( "/images/file/new.png", "Neu" );
    toolBar.add( this.btnNew );

    this.btnOpen = createImageButton( "/images/file/open.png", "\u00D6ffnen" );
    toolBar.add( this.btnOpen );

    this.btnSave = createImageButton( "/images/file/save.png", "Speichern" );
    toolBar.add( this.btnSave );

    this.btnPrint = createImageButton( "/images/file/print.png", "Drucken" );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnUndo = createImageButton(
				"/images/edit/undo.png",
				"R\u00FCckg\u00E4ngig" );
    toolBar.add( this.btnUndo );
    toolBar.addSeparator();

    this.btnCut = createImageButton( "/images/edit/cut.png", "Ausschneiden" );
    toolBar.add( this.btnCut );

    this.btnCopy = createImageButton( "/images/edit/copy.png", "Kopieren" );
    toolBar.add( this.btnCopy );

    this.btnPaste = createImageButton(
				"/images/edit/paste.png",
				"Einf\u00FCgen" );
    toolBar.add( this.btnPaste );
    toolBar.addSeparator();

    this.btnFind = createImageButton(
				"/images/edit/find.png",
				"Suchen und Ersetzen..." );
    toolBar.add( this.btnFind );

    add( toolBar, gbc );


    // Schliessenknopf fuer einzelne Tabs
    this.btnClose = createImageButton(
				"/images/file/closetab.png",
				"Unterfenster schlie\u00DFen" );
    this.btnClose.setBorder( BorderFactory.createEmptyBorder() );
    gbc.anchor       = GridBagConstraints.EAST;
    gbc.insets.right = 5;
    gbc.weightx      = 0.0;
    gbc.gridx++;
    add( this.btnClose, gbc );


    // Textbereich
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    this.tabbedPane.addChangeListener( this );

    JTextArea   firstTextArea   = createJTextArea();
    JScrollPane firstScrollPane = new JScrollPane( firstTextArea );
    this.unusedEditText = new EditText(
				this,
				this.emuThread,
				firstScrollPane,
				firstTextArea,
				false );
    this.editTexts.add( this.unusedEditText );
    this.tabbedPane.addTab( this.unusedEditText.getName(), firstScrollPane );

    gbc.anchor       = GridBagConstraints.CENTER;
    gbc.fill         = GridBagConstraints.BOTH;
    gbc.insets.top   = 0;
    gbc.insets.right = 0;
    gbc.weightx      = 1.0;
    gbc.weighty      = 1.0;
    gbc.gridwidth    = 2;
    gbc.gridx        = 0;
    gbc.gridy++;
    add( tabbedPane, gbc );

    (new DropTarget( this.tabbedPane, this )).setActive( true );


    // Statuszeile
    this.labelStatus  = new JLabel( DEFAULT_STATUS_TEXT );
    gbc.anchor        = GridBagConstraints.WEST;
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.insets.top    = 5;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 5;
    gbc.weighty       = 0.0;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      setBoundsToDefaults();
    }
    setResizable( true );


    // sonstiges
    updFileButtons();
    updCaretButtons();
    updUndoButtons();
    updTitle();
    updStatusBar();
    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
      if( this.clipboard != null ) {
	this.clipboard.addFlavorListener( this );
      }
    }
    updPasteButtons();
  }


  public void caretPositionChanged()
  {
    updCaretButtons();
    updStatusBar();
  }


  public boolean contains( EditText editText )
  {
    return editText != null ? this.editTexts.contains( editText ) : false;
  }


  public void dataChangedStateChanged( EditText editText )
  {
    EditText selectedEditText = getSelectedEditText();
    if( (selectedEditText != null) && (selectedEditText == editText) ) {
      setSaveBtnsEnabled( !selectedEditText.isSaved() );
      setSavePrjBtnsEnabled( selectedEditText.hasProjectChanged() );
    }
  }


  public void doPrgCancel()
  {
    PrgThread prgThread = this.prgThread;
    if( prgThread != null )
      prgThread.fireStop();
  }


  public int getNewTextNum()
  {
    return ++(this.lastNewTextNum);
  }


  public boolean getShiftUseTabs()
  {
    return this.shiftUseTabs;
  }


  public int getShiftWidth()
  {
    return this.shiftWidth;
  }


  public static javax.swing.filechooser.FileFilter[] getTextFileFilters()
  {
    if( textFileFilters == null ) {
      textFileFilters      = new javax.swing.filechooser.FileFilter[ 2 ];
      textFileFilters[ 0 ] = new FileNameExtensionFilter(
				"Quelltextdateien (*.asm; *.bas)",
				"asm", "bas" );
      textFileFilters[ 1 ] = EmuUtil.getTextFileFilter();
    }
    return textFileFilters;
  }


  /*
   * Die Methode springt zu einer Zeile.
   * Die uebergebene Zeilennummer beginnt mit 1.
   */
  public void gotoLine( final JTextArea textArea, int lineNum )
  {
    String text = textArea.getText();
    int    pos  = 0;
    try {
      pos = textArea.getLineStartOffset( lineNum - 1 );
    }
    catch( BadLocationException ex ) {
      if( text != null ) {
	pos = text.length();
      } else {
	pos = 0;
      }
    }
    textArea.requestFocus();

    final int lineBegPos = pos;
    try {
      textArea.setCaretPosition( lineBegPos );
    }
    catch( IllegalArgumentException ex ) {}

    // Zeile kurzzeitig markieren
    if( text != null ) {
      try {
        int eol = text.indexOf( '\n', lineBegPos );
	if( eol > lineBegPos ) {
	  textArea.moveCaretPosition( eol );
	  javax.swing.Timer timer = new javax.swing.Timer(
			500,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    try {
			      textArea.setCaretPosition( lineBegPos );
			      textArea.moveCaretPosition( lineBegPos );
			    }
			    catch( IllegalArgumentException ex ) {}
			  }
			} );
	  timer.setRepeats( false );
	  timer.start();
	}
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  /*
   * Die Methode laedt eine Projektdatei.
   * Wenn die Datei als solche nicht geladen werden kann,
   * wird eine IOException geworfen.
   * Ist die Datei dagegen keine Projektdatei,
   * wird null zurueckgeliefert.
   */
  public static Properties loadProject( File file ) throws IOException
  {
    Properties  props = null;
    InputStream in    = null;
    try {
      in    = new FileInputStream( file );
      props = new Properties();
      props.loadFromXML( in );
    }
    catch( InvalidPropertiesFormatException ex1 ) {
      props = null;
    }
    finally {
      EmuUtil.doClose( in );
    }
    if( props != null ) {
      String s = props.getProperty( "jkcemu.properties.type" );
      if( s != null ) {
	if( !s.equals( "project" ) ) {
	  props = null;
	}
      } else {
	props = null;
      }
    }
    return props;
  }


  public void openFile( File file )
  {
    Properties props = null;
    try {
      props = loadProject( file );
    }
    catch( IOException ex ) {}

    if( props != null ) {
      setState( Frame.NORMAL );
      toFront();
      String[] options = { "Projekt", "Inhalt", "Abbrechen" };
      int      selOpt  = JOptionPane.showOptionDialog(
				this,
				"Die ausgew\u00E4hlte Datei ist eine"
					+ " JKCEMU-Projektdatei.\n"
					+ "M\u00F6chten Sie das Projekt oder"
					+ " den Inhalt\n"
					+ "der Projektdatei \u00F6ffnen?",
				"Projektdatei ausgew\u00E4hlt",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				"Projekt" );
      if( selOpt == 0 ) {
	openProject( file, props );
      }
      else if( selOpt == 1 ) {
	openTextFile( file );
      }
    } else {
      openTextFile( file );
    }
  }


  public void openProject( File file, Properties props )
  {
    if( props != null ) {
      String srcFileName = props.getProperty(
				"jkcemu.programming.source.file.name" );
      if( srcFileName != null ) {
	EditText editText = openTextFile( new File( srcFileName ) );
	if( editText != null )
	  editText.setProject( file, props );
      }
    }
  }


  public EditText openText( String text )
  {
    EditText editText = null;

    /*
     * Pruefen, ob das Default-Textfeld unbenutzt ist
     * und somit verwendet werden kann
     */
    if( this.unusedEditText != null ) {
      if( !this.unusedEditText.isUsed() ) {
	editText = this.unusedEditText;
      }
      this.unusedEditText = null;
    }
    if( editText == null ) {
      editText = doFileNew();
    }
    editText.setText( text );
    return editText;
  }


  public void setSelectedTabComponent( Component tabComponent )
  {
    if( tabComponent != null ) {
      tabbedPane.setSelectedComponent( tabComponent );
      updTitle();
      updUndoButtons();
      updCaretButtons();
      updPasteButtons();
      updStatusBar();
    }
  }


  public void setShiftUseTabs( boolean state )
  {
    this.shiftUseTabs = state;
  }


  public void setShiftWidth( int shiftWidth )
  {
    this.shiftWidth = shiftWidth;
  }


  public void threadTerminated( Thread thread )
  {
    if( (thread != null) && (thread == this.prgThread) ) {
      this.prgThread = null;
      final JMenuItem mnuPrgCancel = this.mnuPrgCancel;
      EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    mnuPrgCancel.setEnabled( false );
		    updFileButtons();
		  }
		} );
    }
  }


  public void updCaretButtons()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      boolean hasSel   = textArea.getSelectionEnd()
                                  > textArea.getSelectionStart();
      boolean editable = textArea.isEditable();

      setCutBtnsEnabled( hasSel && editable );
      setCopyBtnsEnabled( hasSel );
      setReplaceBtnsEnabled( hasSel && editable );
    } else {
      setCutBtnsEnabled( false );
      setCopyBtnsEnabled( false );
      setReplaceBtnsEnabled( false );
    }
  }


  public void updUndoButtons()
  {
    EditText editText = getSelectedEditText();
    setUndoBtnsEnabled( editText != null ? editText.canUndo() : false );
  }


  public void updTitle()
  {
    updTitle( getSelectedEditText() );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e != null ) {
      if( e.getSource() == tabbedPane ) {
        updTitle();
        updFileButtons();
        updUndoButtons();
        updCaretButtons();
        updPasteButtons();
        updStatusBar();
      }
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // empty
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = EmuUtil.fileDrop( this, e );
    if( file != null )
      openFile( file );
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    if( (this.clipboard != null) && (e.getSource() == this.clipboard) )
      updPasteButtons();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = super.applySettings( props, resizable );
    AbstractOptionsDlg dlg = this.prgOptionsDlg;
    if( dlg != null ) {
      dlg.settingsChanged();
    }
    return rv;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.mnuFileNew) || (src == this.btnNew) ) {
	rv = true;
	doFileNew();
      }
      else if( (src == this.mnuFileOpen) || (src == this.btnOpen) ) {
	rv = true;
	doFileOpen();
      }
      else if( src == this.mnuFileOpenCharset ) {
	rv = true;
	doFileOpenCharset();
      }
      else if( (src == this.mnuFileSave) || (src == this.btnSave) ) {
	rv = true;
	doFileSave( false );
      }
      else if( src == this.mnuFileSaveAs ) {
	rv = true;
	doFileSave( true );
      }
      else if( src == this.mnuFilePrintOptions ) {
	rv = true;
	doFilePrintOptions();
      }
      else if( (src == this.mnuFilePrint) || (src == this.btnPrint) ) {
	rv = true;
	doFilePrint();
      }
      else if( src == this.mnuFileProperties ) {
	rv = true;
	doFileProperties();
      }
      else if( (src == this.mnuFileTabClose) || (src == this.btnClose) ) {
	rv = true;
	doFileClose();
      }
      else if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( (src == this.mnuEditUndo) || (src == this.btnUndo) ) {
	rv = true;
	doEditUndo();
      }
      else if( (src == this.mnuEditCut)
	       || (src == this.mnuPopupCut)
	       || (src == this.btnCut) )
      {
	rv = true;
	doEditCut();
      }
      else if( (src == this.mnuEditCopy)
	       || (src == this.mnuPopupCopy)
	       || (src == this.btnCopy) )
      {
	rv = true;
	doEditCopy();
      }
      else if( (src == this.mnuEditPaste)
	       || (src == this.mnuPopupPaste)
	       || (src == this.btnPaste) )
      {
	rv = true;
	doEditPaste();
      }
      else if( src == this.mnuEditUpper ) {
	rv = true;
        doEditUpper();
      }
      else if( src == this.mnuEditLower ) {
	rv = true;
        doEditLower();
      }
      else if( src == this.mnuEditTabSize ) {
	rv = true;
	doEditTabSize();
      }
      else if( src == this.mnuEditTabToSpaces ) {
	rv = true;
	doEditTabToSpaces();
      }
      else if( src == this.mnuEditShiftIn ) {
	rv = true;
	doEditShift( false );
      }
      else if( src == this.mnuEditShiftOut ) {
	rv = true;
	doEditShift( true );
      }
      else if( src == this.mnuEditShiftWidth ) {
	rv = true;
	(new ReplyShiftWidthDlg( this )).setVisible( true );
      }
      else if( src == this.mnuEditUmlautGerToUni ) {
	rv = true;
	doEditUmlautGerToUni();
      }
      else if( src == this.mnuEditUmlautDosToUni ) {
	rv = true;
	doEditUmlautDosToUni();
      }
      else if( (src == this.mnuEditFind)
	       || (src == this.mnuPopupFind)
	       || (src == this.btnFind) )
      {
	rv = true;
	doEditFind();
      }
      else if( (src == this.mnuEditFindNext)
	       || (src == this.mnuPopupFindNext) )
      {
	rv = true;
	doEditFindNext();
      }
      else if( (src == this.mnuEditReplace)
	       || (src == this.mnuPopupReplace) )
      {
	rv = true;
	doEditReplace();
      }
      else if( src == this.mnuEditBracket ) {
	rv = true;
	doEditBracket();
      }
      else if( src == this.mnuEditGoto ) {
	rv = true;
	doEditGoto();
      }
      else if( src == this.mnuEditSelectAll ) {
	rv = true;
	doEditSelectAll();
      }
      else if( src == this.mnuPrgAssemble ) {
	rv = true;
	doPrgAssemble( false, false );
      }
      else if( src == this.mnuPrgAssembleRun ) {
	rv = true;
	doPrgAssemble( false, true );
      }
      else if( src == this.mnuPrgAssembleOpt ) {
	rv = true;
	doPrgAssemble( true, false );
      }
      else if( src == this.mnuPrgCompile ) {
	rv = true;
	doPrgCompile( false, false );
      }
      else if( src == this.mnuPrgCompileRun ) {
	rv = true;
	doPrgCompile( false, true );
      }
      else if( src == this.mnuPrgCompileOpt ) {
	rv = true;
	doPrgCompile( true, false );
      }
      else if( src == this.mnuPrgCancel ) {
	rv = true;
	doPrgCancel();
      }
      else if( src == this.mnuPrjSave ) {
	rv = true;
	doPrjSave( false );
      }
      else if( src == this.mnuPrjSaveAs ) {
	rv = true;
	doPrjSave( true );
      }
      else if( src == this.mnuPrjOpen ) {
	rv = true;
	doPrjOpen();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/tools/texteditor.htm" );
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = true;
    if( this.logFrm != null ) {
      this.logFrm.doClose();
    }
    while( rv && !this.editTexts.isEmpty() ) {
      EditText editText = this.editTexts.get( 0 );
      setSelectedEditText( editText );
      rv = doFileClose();
    }
    if( rv ) {
      rv = super.doClose();
    }
    if( rv ) {
      this.screenFrm.childFrameClosed( this );
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
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      this.mnuPopup.show( c, e.getX(), e.getY() );
      rv = true;
    }
    return rv;
  }


  @Override
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.screenFrm.childFrameClosed( this );
  }


	/* --- Aktionen im Menu Datei --- */

  private EditText doFileNew()
  {
    JTextArea   textArea   = createJTextArea();
    JScrollPane scrollPane = new JScrollPane( textArea );
    EditText    editText   = new EditText(
					this,
					this.emuThread,
					scrollPane,
					textArea,
					true );
    this.editTexts.add( editText );
    this.tabbedPane.addTab( editText.getName(), scrollPane );
    setSelectedTabComponent( scrollPane );
    this.unusedEditText = null;
    updFileButtons();
    updStatusBar();
    return editText;
  }


  private void doFileOpen()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Textdatei \u00F6ffnen",
			Main.getLastPathFile( "text" ),
			getTextFileFilters() );
    if( file != null )
      openFile( file );
  }


  private void doFileOpenCharset()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Textdatei \u00F6ffnen mit Zeichensatz",
			Main.getLastPathFile( "text" ),
			getTextFileFilters() );
    if( file != null ) {
      if( checkFileAlreadyOpen( file ) == null ) {
	SelectEncodingDlg dlg = new SelectEncodingDlg( this );
	dlg.setVisible( true );
	forceOpenFile(
		file,
		dlg.getCharConverter(),
		dlg.getEncodingName(),
		dlg.getEncodingDisplayText() );
      }
    }
  }


  private boolean doFileSave( boolean askFileName )
  {
    boolean  wasSaved = false;
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      askFileName |= editText.getAskFileNameOnSave();
      try {
        if( SaveTextDlg.saveFile( editText, this.editTexts, askFileName ) ) {
          Component tabComponent = editText.getTabComponent();
          if( tabComponent != null ) {
            int i = tabbedPane.indexOfComponent( tabComponent );
            if( i >= 0 ) {
              tabbedPane.setTitleAt( i, editText.getName() );
	    }
          }
          wasSaved = true;
	  editText.setAskFileNameOnSave( false );
          this.labelStatus.setText( "Datei gespeichert" );
        }
      }
      catch( IOException ex ) {
        BasicDlg.showErrorDlg(
                this,
                "Die Datei kann nicht gespeichert werden.\n\n"
                        + ex.getMessage() );
      }
    }
    return wasSaved;
  }


  private void doFilePrintOptions()
  {
    if( PrintOptionsDlg.showPrintOptionsDlg( this, true, true ) )
      this.labelStatus.setText( "Druckoptionen ge\u00E4ndert" );
  }


  private void doFilePrint()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      File file = editText.getFile();
      if( PrintUtil.doPrint(
		this,
		new PlainTextPrintable(
			editText.getText(),
			editText.getTabSize(),
			file != null ? file.getName() : null ),
		editText.getName() ) )
      {
        this.labelStatus.setText( "Datei gedruckt" );
      }
    }
  }


  private void doFileProperties()
  {
    EditText editText = getSelectedEditText();
    if( editText != null )
      (new TextPropDlg( this, editText )).setVisible( true );
  }


  private boolean doFileClose()
  {
    boolean  rv       = false;
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      boolean textChanged = editText.hasDataChanged();
      boolean prjChanged  = false;
      if( editText.getProjectFile() != null ) {
	prjChanged = editText.hasProjectChanged();
      }
      if( textChanged || prjChanged ) {
	setState( Frame.NORMAL );
	toFront();
	String[]    options = { "Speichern", "Verwerfen", "Abbrechen" };
	JOptionPane pane    = new JOptionPane(
		String.format(
			"%s wurde ge\u00E4ndert und nicht gespeichert.\n"
				+ "M\u00F6chten Sie jetzt speichern?",
			prjChanged ? "Das Projekt" : "Der Text" ),
		JOptionPane.WARNING_MESSAGE );
	pane.setWantsInput( false );
	pane.setOptions( options );
	pane.setInitialValue( options[ 0 ] );
	setState( Frame.NORMAL );
	toFront();
	pane.createDialog( this, "Daten ge\u00E4ndert" ).setVisible( true );
	Object value = pane.getValue();
	if( value != null ) {
	  if( value.equals( options[ 0 ] ) ) {
	    rv = true;
	    if( textChanged ) {
	      if( !doFileSave( false ) ) {
		rv = false;
	      }
	    }
	    if( rv && prjChanged ) {
	      if( !editText.saveProject( this, false ) ) {
		rv = false;
	      }
	    }
	  }
	  else if( value.equals( options[ 1 ] ) ) {
	    rv = true;
	  }
	}
      } else {
	rv = true;
      }
      if( rv ) {
	PrgThread prgThread = this.prgThread;
	if( prgThread != null ) {
	  if( editText == prgThread.getEditText() ) {
	    prgThread.fireStop();
	  }
	}
	Component tabComponent = editText.getTabComponent();
	if( tabComponent != null ) {
	  tabbedPane.remove( tabComponent );
	}
	JTextArea textArea = editText.getJTextArea();
	if( textArea != null ) {
	  textArea.removeMouseListener( this );
	}
	this.editTexts.remove( editText );
	if( (this.unusedEditText != null)
	    && (this.unusedEditText == editText) )
	{
	  this.unusedEditText = null;
	}
	editText.die();
	updTitle( null );
	updUndoButtons();
	updCaretButtons();
	updPasteButtons();
	updFileButtons();
	updStatusBar();
      }
    } else {
      rv = super.doClose();
      if( rv ) {
        this.screenFrm.childFrameClosed( this );
      }
    }
    return rv;
  }


	/* --- Aktionen im Menu Bearbeiten --- */

  private void doEditUndo()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      editText.undo();
      updUndoButtons();
    }
  }


  private void doEditCut()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null )
      textArea.cut();
  }


  private void doEditCopy()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null )
      textArea.copy();
  }


  private void doEditPaste()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null )
      textArea.paste();
  }


  private void doEditUpper()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      String s = textArea.getSelectedText();
      if( s != null ) {
	if( s.length() > 0 )
	  textArea.replaceSelection( s.toUpperCase() );
      }
    }
  }


  private void doEditLower()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      String s = textArea.getSelectedText();
      if( s != null ) {
	if( s.length() > 0 )
	  textArea.replaceSelection( s.toLowerCase() );
      }
    }
  }


  private void doEditTabSize()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null )
      (new ReplyTabSizeDlg( this, textArea )).setVisible( true );
  }


  private void doEditTabToSpaces()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      String oldText = editText.getText();
      if( oldText != null ) {
	if( oldText.indexOf( '\t' ) < 0 ) {
	  BasicDlg.showInfoDlg(
		this,
		"Der Text enth\u00E4lt keine Tabulatoren." );
	} else {
	  if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die Tabulatoren durch Leerzeichen"
			+ " ersetzen?" ) )
	  {
	    int tabSize     = editText.getTabSize();
	    int oldCaretPos = editText.getCaretPosition();
	    int newCaretPos = -1;
	    int textLen     = oldText.length();

	    StringBuilder buf = new StringBuilder( 2 * textLen );
	    int           cnt = 0;
	    int           col = 0;
	    int           n;

	    for( int i = 0; i < textLen; i++ ) {
	      if( (oldCaretPos >= 0) && (oldCaretPos == i) ) {
		newCaretPos = buf.length();
	      }

	      char ch = oldText.charAt( i );
	      switch( ch ) {
		case '\n':
		  buf.append( ch );
		  col = 0;
		  break;

		case '\t':
		  n = tabSize - (col % tabSize);
		  for( int k = 0; k < n; k++ ) {
		    buf.append( (char) '\u0020' );
		    col++;
		  }
		  cnt++;
		  break;

		default:
		  buf.append( ch );
		  col++;
	      }
	    }
	    if( cnt > 0 ) {
	      editText.replaceText( buf.toString() );
	      editText.setDataChanged();
	    }
	    showConvResult( cnt );
	    if( newCaretPos >= 0 )
	      editText.setCaretPosition( newCaretPos );
	  }
	}
      }
    }
  }


  private void doEditShift( boolean shiftOut )
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      String text   = textArea.getText();
      int    begPos = textArea.getSelectionStart();
      int    endPos = textArea.getSelectionEnd();
      if( endPos < begPos ) {
	int m  = endPos;
	endPos = begPos;
	begPos = m;
      }
      if( text != null ) {
	int len = text.length();
	if( (begPos >= 0) && (begPos < len) ) {
	  boolean hasSelection = (endPos > begPos);
	  if( endPos < begPos ) {
	    endPos = begPos;
	  } else if( endPos > len ) {
	    endPos = len;
	  }
	  int tabSize = textArea.getTabSize();
	  if( tabSize < 1 ) {
	    tabSize = 8;
	  }
	  // Anfang des markierten Bereichs auf den Zeilenanfang setzen
	  while( begPos > 0 ) {
	    if( text.charAt( begPos - 1 ) == '\n' ) {
	      break;
	    }
	    --begPos;
	  }
	  /*
	   * Ende des markierten Bereichs hinter das Zeilenende setzen;
	   * Wenn das Ende der Markierung am Zeilenanfang steht
	   * und Text markiert wurde,
	   * muss der Markierungsanfang vor der aktuellen Zeile sein.
	   * In dem Fall bedeutet das Markierungsende das Ende
	   * der vorherigen Zeile.
	   */
	  boolean done = false;
	  if( hasSelection && (endPos > 0) ) {
	    if( text.charAt( endPos - 1 ) == '\n' ) {
	      done = true;
	    }
	  }
	  if( !done ) {
	    while( endPos < len ) {
	      if( text.charAt( endPos++ ) == '\n' ) {
		break;
	      }
	    }
	  }
	  // markierten Text in Zeilen zerlegen und verarbeiten
	  StringBuilder buf = new StringBuilder( 2 * len );
	  int           pos = begPos;
	  while( pos < endPos ) {
	    int eol = text.indexOf( '\n', pos );
	    if( (eol >= pos) && ((eol + 1) < len) ) {
	      appendShiftedLine(
			buf,
			text.substring( pos, Math.min( eol + 1, endPos ) ),
			tabSize,
			shiftOut );
	      pos = eol + 1;
	    } else {
	      if( endPos < (len - 1) ) {
		appendShiftedLine(
			buf,
			text.substring( pos, endPos ),
			tabSize,
			shiftOut );
	      } else {
		appendShiftedLine(
			buf,
			text.substring( pos ),
			tabSize,
			shiftOut );
	      }
	      break;
	    }
	  }
	  /*
	   * Block ersetzen, aber nur, wenn er sich unterescheidet,
	   * damit keine sinnlosen UndoableEdits erzeugt werden
	   */
	  String newStr = buf.toString();
	  String oldStr = ((endPos + 1) < len ?
				text.substring( begPos, endPos )
				: text.substring( begPos ));
	  if( !newStr.equals( oldStr ) ) {
	    try {
	      textArea.replaceRange( buf.toString(), begPos, endPos );
	      if( hasSelection ) {
		textArea.setCaretPosition( endPos );
		textArea.setSelectionStart( begPos );
		textArea.setSelectionEnd( begPos + buf.length() );
	      } else {
		textArea.setCaretPosition( begPos );
	      }
	    }
	    catch( IllegalArgumentException ex ) {}
	  }
	}
      }
    }
  }


  private void doEditUmlautGerToUni()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die Zeichen \"[\\]{|}~\"\n"
			+ "in deutsche Umlaute konvertieren?" ) )
      {
	String oldText = editText.getText();
	if( oldText != null ) {
	  int           crs = editText.getCaretPosition();
	  int           len = oldText.length();
	  StringBuilder buf = new StringBuilder( len );
	  int           pos = 0;
	  int           cnt = 0;

	  while( pos < len ) {
	    char ch = oldText.charAt( pos++ );
	    switch( ch ) {
	      case '[':
		buf.append( (char) '\u00C4' );
		cnt++;
		break;
	      case '\\':
		buf.append( (char) '\u00D6' );
		cnt++;
		break;
	      case ']':
		buf.append( (char) '\u00DC' );
		cnt++;
		break;
	      case '{':
		buf.append( (char) '\u00E4' );
		cnt++;
		break;
	      case '|':
		buf.append( (char) '\u00F6' );
		cnt++;
		break;
	      case '}':
		buf.append( (char) '\u00FC' );
		cnt++;
		break;
	      case '~':
		buf.append( (char) '\u00DF' );
		cnt++;
		break;
	      default:
		buf.append( (char) ch );
	    }
	  }
	  if( cnt > 0 ) {
	    editText.replaceText( buf.toString() );
	    editText.setDataChanged();
	  }
	  showConvResult( cnt );
	  editText.setCaretPosition( crs );
	}
      }
    }
  }


  private void doEditUmlautDosToUni()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die im DOS-Zeichensatz kodierten\n"
			+ "deutschen Umlaute konvertieren und so"
			+ " sichtbar machen?" ) )
      {
	String oldText = editText.getText();
	if( oldText != null ) {
	  int           crs = editText.getCaretPosition();
	  int           len = oldText.length();
	  StringBuilder buf = new StringBuilder( len );
	  int           pos = 0;
	  int           cnt = 0;

	  while( pos < len ) {
	    char ch = oldText.charAt( pos++ );
	    switch( ch ) {
	      case 0x8E:
		buf.append( (char) '\u00C4' );
		cnt++;
		break;
	      case 0x99:
		buf.append( (char) '\u00D6' );
		cnt++;
		break;
	      case 0x9A:
		buf.append( (char) '\u00DC' );
		cnt++;
		break;
	      case 0x84:
		buf.append( (char) '\u00E4' );
		cnt++;
		break;
	      case 0x94:
		buf.append( (char) '\u00F6' );
		cnt++;
		break;
	      case 0x81:
		buf.append( (char) '\u00FC' );
		cnt++;
		break;
	      case 0xE1:
		buf.append( (char) '\u00DF' );
		cnt++;
		break;
	      default:
		buf.append( (char) ch );
	    }
	  }
	  if( cnt > 0 ) {
	    editText.replaceText( buf.toString() );
	    editText.setDataChanged();
	  }
	  showConvResult( cnt );
	  editText.setCaretPosition( crs );
	}
      }
    }
  }


  private void doEditFind()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      JTextArea textArea = editText.getJTextArea();
      if( textArea != null ) {
	String textFind = textArea.getSelectedText();
	if( (textFind == null) || (textFind.length() < 1) ) {
	  textFind = this.textFind;
	}
	FindTextDlg dlg = new FindTextDlg(
                                this,
                                textFind,
                                this.textReplace,
                                this.findIgnoreCase );
	dlg.setVisible( true );

	switch( dlg.getAction() ) {
	  case FIND_NEXT:
	    this.textFind       = dlg.getTextFind();
	    this.textReplace    = dlg.getTextReplace();
	    this.findIgnoreCase = dlg.getIgnoreCase();

	    findText(
		editText,
                textArea,
                Math.max(
                        textArea.getCaretPosition(),
                        textArea.getSelectionEnd() ),
                true );
	    break;

	  case REPLACE_ALL:
	    this.textFind       = dlg.getTextFind();
	    this.textReplace    = dlg.getTextReplace();
	    this.findIgnoreCase = dlg.getIgnoreCase();

	    boolean found = findText( editText, textArea, 0, false );
	    int     n     = 0;

	    while( found ) {
	      if( replaceText( textArea ) ) {
		n++;
	      }
	      found = findText(
			editText,
                        textArea,
                        Math.max(
                                textArea.getCaretPosition(),
                                textArea.getSelectionEnd() ),
                        false );
	    }

	    if( n == 0 ) {
	      showTextNotFound();
	    } else {
	      BasicDlg.showInfoDlg(
                this,
                String.valueOf( n ) + " Textersetzungen durchgef\u00FChrt.",
                "Text ersetzen" );
	    }
	    break;
	}
      }
    }
  }


  private void doEditFindNext()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      JTextArea textArea = editText.getJTextArea();
      if( textArea != null ) {
	if( this.textFind == null ) {
	  doEditFind();
	} else {
	  findText(
		editText,
                textArea,
                Math.max(
                        textArea.getCaretPosition(),
                        textArea.getSelectionEnd() ),
                true );
	}
      }
    }
  }


  private void doEditReplace()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      if( this.textReplace != null )
        replaceText( textArea );
    }
  }


  private void doEditBracket()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      String text = textArea.getText();
      if( text != null ) {
	Integer dstPos = null;
	int     len    = text.length();
	int     pos    = textArea.getCaretPosition();
	if( (pos >= 0) && (pos < len) ) {
	  char ch = text.charAt( pos );
	  switch ( ch ) {
	    case '(':
	      dstPos = findBracket( text, pos, ch, ')', 1 );
	      break;
	    case ')':
	      dstPos = findBracket( text, pos, ch, '(', -1 );
	      break;
	    case '[':
	      dstPos = findBracket( text, pos, ch, ']', 1 );
	      break;
	    case ']':
	      dstPos = findBracket( text, pos, ch, '[', -1 );
	      break;
	    case '{':
	      dstPos = findBracket( text, pos, ch, '}', 1 );
	      break;
	    case '}':
	      dstPos = findBracket( text, pos, ch, '{', -1 );
	      break;
	    default:
	      while( pos < len ) {
		ch = text.charAt( pos );
		if( (ch == ')') || (ch == ']') || (ch == '}') ) {
		  dstPos = new Integer( pos );
		  break;
		}
		pos++;
	      }
	      break;
	  }
	  if( dstPos != null ) {
	    textArea.setCaretPosition( dstPos.intValue() );
	    textArea.requestFocus();
	  }
	}
      }
    }
  }


  private void doEditGoto()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {

      // Zeilennummer ermitteln
      Integer lineNum = null;
      try {
        lineNum = new Integer( textArea.getLineOfOffset(
                                textArea.getCaretPosition() ) + 1 );
      }
      catch( BadLocationException ex ) {
        lineNum = null;
      }

      // Dialog aufrufen
      ReplyIntDlg dlg = new ReplyIntDlg(
                                this,
                                "Zeilenummer:",
                                lineNum,
                                new Integer( 1 ),
                                null );
      dlg.setTitle( "Gehe zu Zeile" );
      dlg.setVisible( true );
      lineNum = dlg.getReply();

      // zur Zeile springen
      if( lineNum != null )
	gotoLine( textArea, lineNum.intValue() );
    }
  }


  private void doEditSelectAll()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      textArea.requestFocus();
      textArea.selectAll();
      updCaretButtons();
    }
  }


	/* --- Aktionen im Menu Programmierung --- */

  private void doPrgAssemble(
			boolean forceOptionsDlg,
			boolean forceRunProgram )
  {
    if( this.prgThread == null ) {
      EditText editText = getSelectedEditText();
      if( editText != null ) {
	PrgOptions options = editText.getPrgOptions();
	if( options != null ) {
	  if( options instanceof BasicOptions )
	    forceOptionsDlg = true;
	}
	if( forceOptionsDlg || (options == null) ) {
	  this.prgOptionsDlg = new AsmOptionsDlg(
					this,
					this.emuThread,
					options );
	  this.prgOptionsDlg.setVisible( true );
	  options = this.prgOptionsDlg.getAppliedOptions();
	  this.prgOptionsDlg = null;
	}
	if( options != null ) {
	  editText.setPrgOptions( options );
	  if( forceRunProgram && !options.getCodeToEmu() ) {
	    options = new PrgOptions( options );
	    options.setCodeToEmu( true );
	  }
	  PrgThread thread = new Z80Assembler(
				this.emuThread,
				editText,
				editText.getText(),
				openLog( editText, editText.getName() ),
				options,
				forceRunProgram );
	  this.prgThread = thread;
	  this.mnuPrgCancel.setEnabled( true );
	  updFileButtons();
	  thread.start();
	}
      }
    }
  }


  private void doPrgCompile(
			boolean forceOptionsDlg,
			boolean forceRunProgram )
  {
    if( this.prgThread == null ) {
      EditText editText = getSelectedEditText();
      if( editText != null ) {
	BasicCompiler.Platform platform
		= BasicCompiler.getPlatform( this.emuThread.getEmuSys() );

	BasicOptions basicOptions = null;
	PrgOptions   options      = editText.getPrgOptions();
	if( options != null ) {
	  if( options instanceof BasicOptions )
	    basicOptions = (BasicOptions) options;
	}
	if( basicOptions != null ) {
	  BasicCompiler.Platform lastPlatform = basicOptions.getPlatform();
	  if( lastPlatform != null ) {
	    if( !lastPlatform.equals( platform ) ) {
	      forceOptionsDlg = true;
	    }
	  }
	}
	if( forceOptionsDlg || (basicOptions == null) ) {
	  basicOptions       = null;
	  this.prgOptionsDlg = new BasicOptionsDlg(
						this,
						this.emuThread,
						options );
	  this.prgOptionsDlg.setVisible( true );
	  options = this.prgOptionsDlg.getAppliedOptions();
	  if( options != null ) {
	    if( options instanceof BasicOptions )
	      basicOptions = (BasicOptions) options;
	  }
	  this.prgOptionsDlg = null;
	}
	if( basicOptions != null ) {
	  basicOptions.setPlatform( platform );
	  editText.setPrgOptions( basicOptions );
	  if( forceRunProgram && !basicOptions.getCodeToEmu() ) {
	    basicOptions = new BasicOptions( basicOptions );
	    basicOptions.setCodeToEmu( true );
	  }
	  PrgThread thread = new BasicCompiler(
				this.emuThread,
				editText,
				editText.getText(),
				openLog( editText, editText.getName() ),
				basicOptions,
				forceRunProgram );
	  this.prgThread = thread;
	  this.mnuPrgCancel.setEnabled( true );
	  updFileButtons();
	  thread.start();
	}
      }
    }
  }


  private void doPrjOpen()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Projekt \u00F6ffnen",
			Main.getLastPathFile( "text" ),
			EmuUtil.getProjectFileFilter() );
    if( file != null ) {
      try {
	Properties props = loadProject( file );
	if( props != null ) {
	  openProject( file, props );
	} else {
	  BasicDlg.showErrorDlg(
		this,
		"Die ausgew\u00E4hlte Datei ist keine JKCEMU-Projektdatei." );
	}
      }
      catch( IOException ex ) {
	BasicDlg.showOpenFileErrorDlg( this, file, ex );
      }
    }
  }


  private void doPrjSave( boolean askFileName )
  {
    EditText editText = getSelectedEditText();
    if( editText != null )
      editText.saveProject( this, askFileName );
  }


	/* --- private Methoden --- */

  private void appendShiftedLine(
			StringBuilder buf,
			String        line,
			int           tabSize,
			boolean       shiftOut )
  {
    // Anzahl der sichtbaren Leerstellen ermitteln
    int len     = line.length();
    int pos     = 0;
    int nSpaces = 0;
    while( pos < len ) {
      char ch = line.charAt( pos );
      if( ch == '\u0020' ) {
	nSpaces++;
	pos++;
      } else if( ch == '\t' ) {
	nSpaces += (tabSize - (nSpaces % tabSize));
	pos++;
      } else {
	break;
      }
    }
    if( shiftOut ) {
      nSpaces -= this.shiftWidth;
      if( nSpaces < 0 ) {
	nSpaces = 0;
      }
    } else {
      nSpaces += this.shiftWidth;
    }
    if( pos < len ) {
      // Leerzeichen/Tabs einfuegen, aber nur bei gefuellten Zeilen
      if( line.charAt( pos ) != '\n' ) {
	if( this.shiftUseTabs ) {
	  while( nSpaces >= tabSize ) {
	    buf.append( (char) '\t' );
	    nSpaces -= tabSize;
	  }
	}
	for( int i = 0; i < nSpaces; i++ ) {
	  buf.append( (char) '\u0020' );
	}
      }
      // Rest der Zeile anhaengen
      if( pos > 0 ) {
	buf.append( line.substring( pos ) );
      } else {
	buf.append( line );
      }
    }
  }


  private JTextArea createJTextArea()
  {
    JTextArea textArea = new JTextArea();
    textArea.setMargin( new Insets( 5, 5, 5, 5 ) );

    String tabSizeText = Main.getProperty( "jkcemu.texteditor.tabsize" );
    if( tabSizeText != null ) {
      if( tabSizeText.length() > 0 ) {
	try {
	  int tabSize = Integer.parseInt( tabSizeText );
	  if( (tabSize > 0) && (tabSize < 100) )
	    textArea.setTabSize( tabSize );
	}
	catch( NumberFormatException ex ) {}
      }
    }

    Font font = textArea.getFont();
    if( font != null ) {
      textArea.setFont(
                new Font( "Monospaced", font.getStyle(), font.getSize() ) );
    } else {
      textArea.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
    }
    textArea.addMouseListener( this );
    return textArea;
  }


  private Integer findBracket(
			String text,
			int    pos,
			char   bracket1,
			char   bracket2,
			int    step )
  {
    int level = 0;
    int len   = text.length();
    while( (pos >= 0) && (pos < len) ) {
      char ch = text.charAt( pos );
      if( ch == bracket1 ) {
	level++;
      }
      else if( ch == bracket2 ) {
	--level;
	if( level <= 0 )
	  return new Integer( pos );
      }
      pos += step;
    }
    return null;
  }


  private boolean findText(
			EditText  editText,
                        JTextArea textArea,
                        int       startPos,
                        boolean   interactive )
  {
    boolean rv       = false;
    String  textFind = this.textFind;

    if( (textFind != null) && (textFind.length() > 0) ) {
      this.hasTextFind = true;
      setFindNextBtnsEnabled( this.hasTextFind );

      String textBase = textArea.getText();
      if( textBase == null ) {
        textBase = "";
      }
      if( this.findIgnoreCase ) {
        textFind = textFind.toUpperCase();
        textBase = textBase.toUpperCase();
      }
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
        textArea.requestFocus();
        if( interactive ) {
          textArea.setCaretPosition( posFind );
	}
        textArea.select( posFind, posFind + textFind.length() );
	rv = true;
      }
    }
    if( !rv && interactive ) {
      if( startPos > 0 ) {
	rv = findText( editText, textArea, 0, interactive );
      } else {
	showTextNotFound();
      }
    }
    return rv;
  }


  private EditText getSelectedEditText()
  {
    Component c = this.tabbedPane.getSelectedComponent();
    if( c != null ) {
      for( EditText editText : this.editTexts ) {
        if( editText.getTabComponent() == c )
          return editText;
      }
    }
    return null;
  }


  private JTextArea getSelectedJTextArea()
  {
    EditText editText = getSelectedEditText();
    return editText != null ? editText.getJTextArea() : null;
  }


  private EditText checkFileAlreadyOpen( File file )
  {
    EditText rv = null;
    for( EditText editText : this.editTexts ) {
      if( editText.isSameFile( file ) ) {
        setSelectedEditText( editText );
        BasicDlg.showInfoDlg(
		this,
		"Diese Datei ist bereits ge\u00F6ffnet.",
		"Hinweis" );
        rv = editText;
      }
    }
    return rv;
  }


  private EditText forceOpenFile(
			File          file,
			CharConverter charConverter,
                	String        encodingName,
                	String        encodingDisplayText )
  {
    EditText editText = null;
    try {

      /*
       * Pruefen, ob das Default-Textfeld unbenutzt ist
       * und somit verwendet werden kann
       */
      if( this.unusedEditText != null ) {
        if( !this.unusedEditText.isUsed() ) {
          this.unusedEditText.loadFile(
				file,
				charConverter,
				encodingName,
				encodingDisplayText );
          editText = this.unusedEditText;

          // neuer Dateiname auf der Lasche
          Component tabComponent = editText.getTabComponent();
          if( tabComponent != null ) {
            int i = this.tabbedPane.indexOfComponent( tabComponent );
	    if( i >= 0 )
              this.tabbedPane.setTitleAt( i, editText.getName() );
          }
        }
        this.unusedEditText = null;
      }
      if( editText == null ) {
        editText = new EditText(
				this,
				this.emuThread,
				file,
				charConverter,
				encodingName,
				encodingDisplayText );
        JTextArea   textArea   = createJTextArea();
        JScrollPane scrollPane = new JScrollPane( textArea );
        editText.setComponents( scrollPane, textArea );
        this.editTexts.add( editText );
        tabbedPane.addTab( editText.getName(), scrollPane );
      }
      Main.setLastFile( file, "text" );

      // Text sichtbar machen und Oberflaechenelemente aktualisieren
      setSelectedTabComponent( editText.getTabComponent() );
      updFileButtons();
      updStatusBar();
    }
    catch( IOException ex ) {
      editText = null;
      BasicDlg.showOpenFileErrorDlg( this, file, ex );
    }
    catch( UserCancelException ex ) {
      editText = null;
    }
    return editText;
  }


  private EditText openTextFile( File file )
  {
    EditText editText = checkFileAlreadyOpen( file );
    if( editText == null ) {
      editText = forceOpenFile( file, null, null, null );
    }
    return editText;
  }


  private Appendable openLog( EditText editText, String subTitle )
  {
    String title = "Meldungen";
    if( subTitle != null ) {
      title = title + ": " + subTitle;
    }
    if( this.logFrm != null ) {
      this.logFrm.reset( editText, title );
      this.logFrm.setVisible( true );
      this.logFrm.setState( Frame.NORMAL );
      this.logFrm.toFront();
    } else {
      this.logFrm          = new LogFrm( editText, title );
      Rectangle bounds     = getBounds();
      Dimension screenSize = this.logFrm.getToolkit().getScreenSize();
      if( (bounds != null) && (screenSize != null) ) {
	if( (bounds.x + 100 < screenSize.width)
	    || (bounds.y + 100 < screenSize.height) )
	{
	  this.logFrm.setLocation( bounds.x + 50, bounds.y + 50 );
	} else {
	  this.logFrm.setLocation( 0, 0 );
	}
      }
      this.logFrm.setVisible( true );
    }
    return this.logFrm;
  }


  private boolean replaceText( JTextArea textArea )
  {
    int selBeg = textArea.getSelectionStart();
    int selEnd = textArea.getSelectionEnd();

    if( (selBeg < 0) || (selBeg >= selEnd) )
      return false;

    textArea.replaceSelection( this.textReplace != null ?
                                                this.textReplace : "" );
    return true;
  }


  private void setCopyBtnsEnabled( boolean state )
  {
    this.mnuEditCopy.setEnabled( state );
    this.mnuPopupCopy.setEnabled( state );
    this.btnCopy.setEnabled( state );
  }


  private void setCutBtnsEnabled( boolean state )
  {
    this.mnuEditCut.setEnabled( state );
    this.mnuPopupCut.setEnabled( state );
    this.btnCut.setEnabled( state );
  }


  private void setFileBtnsEnabled( boolean state )
  {
    boolean prgState = (state && (this.prgThread == null));
    this.mnuFileSaveAs.setEnabled( state );
    this.mnuFilePrint.setEnabled( state );
    this.mnuFileProperties.setEnabled( state );
    this.mnuFileTabClose.setEnabled( state );
    this.mnuEditTabSize.setEnabled( state );
    this.mnuEditTabToSpaces.setEnabled( state );
    this.mnuEditUmlautGerToUni.setEnabled( state );
    this.mnuEditUmlautDosToUni.setEnabled( state );
    this.mnuEditFind.setEnabled( state );
    this.mnuEditBracket.setEnabled( state );
    this.mnuEditGoto.setEnabled( state );
    this.mnuEditSelectAll.setEnabled( state );
    this.mnuPrgAssemble.setEnabled( prgState );
    this.mnuPrgAssembleRun.setEnabled( prgState );
    this.mnuPrgAssembleOpt.setEnabled( prgState );
    this.mnuPrgCompile.setEnabled( prgState );
    this.mnuPrgCompileRun.setEnabled( prgState );
    this.mnuPrgCompileOpt.setEnabled( prgState );
    this.mnuPrjSaveAs.setEnabled( state );
    this.mnuPopupFind.setEnabled( state );
    this.btnPrint.setEnabled( state );
    this.btnClose.setEnabled( state );
    this.btnFind.setEnabled( state );
  }


  private void setFindNextBtnsEnabled( boolean state )
  {
    this.mnuEditFindNext.setEnabled( state );
    this.mnuPopupFindNext.setEnabled( state );
  }


  private void setPasteBtnsEnabled( boolean state )
  {
    this.mnuEditPaste.setEnabled( state );
    this.mnuPopupPaste.setEnabled( state );
    this.btnPaste.setEnabled( state );
  }


  private void setReplaceBtnsEnabled( boolean state )
  {
    this.mnuEditUpper.setEnabled( state );
    this.mnuEditLower.setEnabled( state );
    this.mnuEditReplace.setEnabled( state );
    this.mnuPopupReplace.setEnabled( state );
  }


  private void setSaveBtnsEnabled( boolean state )
  {
    this.mnuFileSave.setEnabled( state );
    this.btnSave.setEnabled( state );
  }


  private void setSavePrjBtnsEnabled( boolean state )
  {
    this.mnuPrjSave.setEnabled( state );
  }


  private void setSelectedEditText( EditText editText )
  {
    if( editText != null ) {
      Component c = editText.getTabComponent();
      if( c != null ) {
        tabbedPane.setSelectedComponent( c );
        updTitle( editText );
        updUndoButtons();
        updCaretButtons();
        updPasteButtons();
      }
    }
  }


  private void setUndoBtnsEnabled( boolean state )
  {
    this.mnuEditUndo.setEnabled( state );
    this.btnUndo.setEnabled( state );
  }


  private void showConvResult( int cnt )
  {
    BasicDlg.showInfoDlg( this, String.valueOf( cnt ) + " Ersetzungen" );
  }


  private void showTextNotFound()
  {
    Toolkit tk = getToolkit();
    if( tk != null )
      tk.beep();

    BasicDlg.showInfoDlg(
                this,
                "Text nicht gefunden!",
                "Text suchen" );
  }


  private void updFileButtons()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      setFileBtnsEnabled( true );
      setSaveBtnsEnabled( !editText.isSaved() );
      setSavePrjBtnsEnabled( editText.hasProjectChanged() );
      setFindNextBtnsEnabled( this.hasTextFind );
    } else {
      setFileBtnsEnabled( false );
      setSaveBtnsEnabled( false );
      setSavePrjBtnsEnabled( false );
      setFindNextBtnsEnabled( false );
    }
  }


  private void updPasteButtons()
  {
    boolean   state    = false;
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      if( textArea.isEditable() && (this.clipboard != null) ) {
	try {
          state = this.clipboard.isDataFlavorAvailable(
                                        DataFlavor.stringFlavor );
	}
	catch( Exception ex ) {}
      }
    }
    setPasteBtnsEnabled( state );
  }


  private void updStatusBar()
  {
    if( this.labelStatus != null ) {
      String    statusText = DEFAULT_STATUS_TEXT;
      JTextArea textArea   = getSelectedJTextArea();
      if( textArea != null ) {
        try {
          int pos = textArea.getCaretPosition();
          int row = textArea.getLineOfOffset( pos );
	  int bol = textArea.getLineStartOffset( row );
          int col = pos - bol;
	  if( col > 0 ) {
	    Document doc     = textArea.getDocument();
	    int      tabSize = textArea.getTabSize();
	    if( (doc != null) && (tabSize > 0) ) {
	      Segment seg = new Segment();
	      seg.setPartialReturn( false );
	      doc.getText( bol, col, seg );
	      col = 0;
	      int ch = seg.first();
	      while( ch != CharacterIterator.DONE ) {
		if( ch == '\t' ) {
		  col = ((col / tabSize) + 1) * tabSize;
		} else {
		  col++;
		}
		ch = seg.next();
	      }
	    }
	  }
          statusText = String.format( "Z:%d S:%d", row + 1, col + 1 );
        }
        catch( BadLocationException ex ) {}
      }
      this.labelStatus.setText( statusText );
    }
  }


  private void updTitle( EditText editText )
  {
    String title = "JKCEMU Editor";
    if( editText != null ) {
      title += ": ";
      File file = editText.getFile();
      if( file != null ) {
	title += file.getPath();
      } else {
	title += editText.getName();
      }
    }
    setTitle( title );
  }
}

