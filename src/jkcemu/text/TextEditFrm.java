/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Editor fuer Dokumente
 */

package jkcemu.text;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import jkcemu.Main;
import jkcemu.programming.AbstractOptionsDlg;
import jkcemu.programming.PrgOptions;
import jkcemu.programming.PrgSource;
import jkcemu.programming.PrgThread;
import jkcemu.programming.assembler.AsmThread;
import jkcemu.programming.assembler.AsmOptionsDlg;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.BasicCompilerThread;
import jkcemu.programming.basic.BasicOptions;
import jkcemu.programming.basic.BasicOptionsDlg;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.HelpFrm;
import jkcemu.base.ReplyIntDlg;
import jkcemu.base.ScreenFrm;
import jkcemu.base.UserCancelException;
import jkcemu.print.PlainTextPrintable;
import jkcemu.print.PrintOptionsDlg;
import jkcemu.print.PrintUtil;
import jkcemu.tools.debugger.DebugFrm;


public class TextEditFrm extends BaseFrm implements
					ChangeListener,
					DropTargetListener,
					FlavorListener
{
  public static final String PROP_SHOW_LINE_ADDRS
				= "jkcemu.texteditor.show_line_addrs";
  public static final String PROP_TABSIZE
				= "jkcemu.texteditor.tabsize";
  public static final String PROP_SHIFT_WIDTH
				= "jkcemu.texteditor.shift.width";
  public static final String PROP_SHIFT_USE_TABS
				= "jkcemu.texteditor.shift.use_tabs";

  private static final String DEFAULT_STATUS_TEXT = "Bereit";
  private static final String HELP_PAGE = "/help/tools/texteditor.htm";

  private static TextEditFrm                          instance        = null;
  private static javax.swing.filechooser.FileFilter[] textFileFilters = null;

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
  private JMenuItem                mnuEditRemoveWordstarFmt;
  private JMenuItem                mnuEditFind;
  private JMenuItem                mnuEditFindPrev;
  private JMenuItem                mnuEditFindNext;
  private JMenuItem                mnuEditReplace;
  private JMenuItem                mnuEditBracket;
  private JMenuItem                mnuEditGoto;
  private JMenuItem                mnuEditSelectAll;
  private JMenuItem                mnuPrgAssemble;
  private JMenuItem                mnuPrgAssembleRun;
  private JMenuItem                mnuPrgAssembleOpt;
  private JCheckBoxMenuItem        mnuPrgLineAddrs;
  private JMenuItem                mnuRemoveRowHeader;
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
  private JMenuItem                mnuPopupFindPrev;
  private JMenuItem                mnuPopupFindNext;
  private JMenuItem                mnuPopupReplace;
  private JMenuItem                mnuPopupBreak;
  private JMenuItem                mnuPopupSelectAll;
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
  private int                      lastNewTextNum;
  private int                      popupLineAddr;
  private int                      shiftWidth;
  private boolean                  shiftUseTabs;


  public static TextEditFrm open( EmuThread emuThread )
  {
    if( instance == null ) {
      instance = new TextEditFrm( emuThread );
    }
    EmuUtil.showFrame( instance );
    return instance;
  }


  public static TextEditFrm open( EmuThread emuThread, File file )
  {
    TextEditFrm frm = open( emuThread );
    if( file != null ) {
      frm.openFile( file );
    }
    return frm;
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
    if( prgThread != null ) {
      prgThread.cancel();
    }
  }


  public int getNewTextNum()
  {
    return ++this.lastNewTextNum;
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
      EmuUtil.closeSilent( in );
    }
    if( props != null ) {
      String s = props.getProperty( EditText.PROP_PROPERTIES_TYPE );
      if( s != null ) {
	if( !s.equals( EditText.VALUE_PROPERTIES_TYPE_PROJECT ) ) {
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
				EditText.PROP_PRG_SOURCE_FILE_NAME );
      if( srcFileName != null ) {
	File srcFile = new File( srcFileName );
	if( !srcFile.isAbsolute() ) {
	  File parent = file.getParentFile();
	  if( parent != null ) {
	    srcFile = new File( parent, srcFileName );
	  }
	}
	EditText editText = openTextFile( srcFile );
	if( editText != null ) {
	  editText.setProject( file, props );
	  Main.setLastFile( file, Main.FILE_GROUP_PROJECT );
	}
      }
    }
  }


  public EditText openText( String text )
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      if( editText.isUsed() ) {
	editText = null;
      }
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


  public void threadTerminated( final Thread thread )
  {
    if( (thread != null) && (thread == this.prgThread) ) {
      this.prgThread = null;
      final JMenuItem         mnuPrgCancel   = this.mnuPrgCancel;
      final JCheckBoxMenuItem mnuPrgLineAddr = this.mnuPrgLineAddrs;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    mnuPrgCancel.setEnabled( false );
		    updFileButtons();
		    if( (thread instanceof AsmThread)
			&& mnuPrgLineAddr.isSelected() )
		    {
		      setLineAddrs( ((AsmThread) thread).getPrgSources() );
		    }
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
    if( file != null ) {
      openFile( file );
    }
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
  public void setVisible( boolean state )
  {
    super.setVisible( state );
    if( state && !this.editTexts.isEmpty() ) {
      JTextArea textArea = this.editTexts.get( 0 ).getJTextArea();
      if( textArea != null ) {
	textArea.setColumns( 0 );
	textArea.setRows( 0 );
      }
    }
  }


  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean            rv  = super.applySettings( props, resizable );
    AbstractOptionsDlg dlg = this.prgOptionsDlg;
    if( dlg != null ) {
      dlg.settingsChanged();
    }
    this.mnuPrgLineAddrs.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				PROP_SHOW_LINE_ADDRS,
				true ) );
    updShowLineAddrs();
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
      else if( src == this.mnuEditRemoveWordstarFmt ) {
	rv = true;
	doEditRemoveWordstarFmt();
      }
      else if( (src == this.mnuEditFind)
	       || (src == this.mnuPopupFind)
	       || (src == this.btnFind) )
      {
	rv = true;
	doEditFind();
      }
      else if( (src == this.mnuEditFindPrev)
	       || (src == this.mnuPopupFindPrev) )
      {
	rv = true;
	doEditFindNext( true );
      }
      else if( (src == this.mnuEditFindNext)
	       || (src == this.mnuPopupFindNext) )
      {
	rv = true;
	doEditFindNext( false );
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
      else if( (src == this.mnuEditSelectAll)
	       || (src == this.mnuPopupSelectAll) )
      {
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
      else if( src == this.mnuPrgLineAddrs ) {
	rv = true;
	updShowLineAddrs();
      }
      else if( src == this.mnuRemoveRowHeader ) {
	rv = true;
	doRemoveRowHeader();
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
	HelpFrm.open( HELP_PAGE );
      }
      else if( src == this.mnuPopupBreak ) {
	rv = true;
	doCreateBreakpoint();
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
      if( !Main.checkQuit( this ) ) {
	// Voreinstellung fuer erneutes Oeffnen
	this.lastNewTextNum = 0;
	doFileNew();
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
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      props.setProperty(
		PROP_SHOW_LINE_ADDRS,
		String.valueOf( this.mnuPrgLineAddrs.isSelected() ) );
    }
  }


  @Override
  public void resetFired()
  {
    removeRowHeaders();
  }


  @Override
  protected boolean showPopup( MouseEvent e )
  {
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      this.popupLineAddr = -1;
      if( this.mnuPopupBreak != null ) {
	EditText editText = getSelectedEditText();
	if( editText != null ) {
	  PrgOptions options  = editText.getPrgOptions();
	  if( options != null ) {
	    if( options.getCodeToEmu() ) {
	      this.popupLineAddr = getLineAddr( e );
	    }
	  }
	}
	this.mnuPopupBreak.setEnabled( this.popupLineAddr >= 0 );
      }
      this.mnuPopup.show( c, e.getX(), e.getY() );
      rv = true;
    }
    return rv;
  }


	/* --- Aktionen im Menu Datei --- */

  private EditText doFileNew()
  {
    JTextArea   textArea   = createJTextArea();
    JScrollPane scrollPane = new JScrollPane( textArea );
    EditText    editText   = new EditText( this, scrollPane, textArea );
    this.editTexts.add( editText );
    this.tabbedPane.addTab( editText.getName(), scrollPane );
    setSelectedTabComponent( scrollPane );
    updFileButtons();
    updStatusBar();
    return editText;
  }


  private void doFileOpen()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Textdatei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_TEXT ),
			getTextFileFilters() );
    if( file != null ) {
      openFile( file );
    }
  }


  private void doFileOpenCharset()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Textdatei \u00F6ffnen mit Zeichensatz",
			Main.getLastDirFile( Main.FILE_GROUP_TEXT ),
			getTextFileFilters() );
    if( file != null ) {
      if( checkFileAlreadyOpen( file ) == null ) {
	SelectEncodingDlg dlg = new SelectEncodingDlg( this );
	dlg.setVisible( true );
	forceOpenFile(
		file,
		dlg.getCharConverter(),
		dlg.getEncodingName(),
		dlg.getEncodingDisplayText(),
		dlg.getIgnoreEofByte() );
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
        BaseDlg.showErrorDlg(
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
    if( editText != null ) {
      (new TextPropDlg( this, editText )).setVisible( true );
    }
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
	    prgThread.cancel();
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
	editText.die();
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updTitle( getSelectedEditText() );
		    updUndoButtons();
		    updCaretButtons();
		    updPasteButtons();
		    updFileButtons();
		    updStatusBar();
		  }
		} );
      }
    } else {
      rv = super.doClose();
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
    if( textArea != null ) {
      textArea.cut();
    }
  }


  private void doEditCopy()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      textArea.copy();
    }
  }


  private void doEditPaste()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      textArea.paste();
    }
  }


  private void doEditUpper()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      String s = textArea.getSelectedText();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  textArea.replaceSelection( s.toUpperCase() );
	}
      }
    }
  }


  private void doEditLower()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      String s = textArea.getSelectedText();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  textArea.replaceSelection( s.toLowerCase() );
	}
      }
    }
  }


  private void doEditTabSize()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      (new ReplyTabSizeDlg( this, textArea )).setVisible( true );
    }
  }


  private void doEditTabToSpaces()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      String oldText = editText.getText();
      if( oldText != null ) {
	if( oldText.indexOf( '\t' ) < 0 ) {
	  BaseDlg.showInfoDlg(
		this,
		"Der Text enth\u00E4lt keine Tabulatoren." );
	} else {
	  if( BaseDlg.showYesNoDlg(
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
	    if( newCaretPos >= 0 ) {
	      editText.setCaretPosition( newCaretPos );
	    }
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
      if( BaseDlg.showYesNoDlg(
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
      if( BaseDlg.showYesNoDlg(
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


  private void doEditRemoveWordstarFmt()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      String msg = "M\u00F6chten Sie die WordStar-Formatierungen entfernen\n"
		+ "und so aus der Datei eine reine Textdatei machen?";
      if( editText.getCharsLostOnOpen() ) {
	msg = "Beim \u00D6ffnen der Datei konnten nicht alle Bytes"
		+ " in Zeichen gemappt werden.\n"
		+ "Dadurch k\u00F6nnen Zeichen fehlen oder der Text"
		+ " anderweitig zerst\u00F6rt sein.\n"
		+ "Aus diesem Grund sollten Sie die Datei zuerst"
		+ " mit dem Zeichensatz\n"
		+ "ISO-8859-1 (Latin 1) \u00F6ffnen (Men\u00FCpunkt"
		+ " \'Datei \u00F6ffnen mit Zeichensatz...\')\n"
		+ "und dann erst die WordStar-Formatierungen entfernen.\n\n"
		+ msg;
      }
      if( BaseDlg.showYesNoDlg( this, msg ) ) {
	String oldText = editText.getText();
	if( oldText != null ) {
	  int           oldCrs = editText.getCaretPosition();
	  int           newCrs = -1;
	  int           len    = oldText.length();
	  int           pos    = 0;
	  boolean       bol    = true;	// Zeilenanfang
	  boolean       ign    = false;	// Zeile ignorieren
	  StringBuilder buf    = new StringBuilder( len );
	  while( pos < len ) {
	    if( (newCrs < 0) && (oldCrs == pos) ) {
	      newCrs = buf.length();
	    }
	    char ch = oldText.charAt( pos++ );
	    if( bol && (ch == '.') ) {
	      ign = true;
	    }
	    ch &= 0x7F;
	    if( (ch == '\n') || (ch == '\t') || (ch >= 0x20) ) {
	      if( !ign ) {
		buf.append( ch );
	      }
	      if( ch <= 0x20 ) {
		ign = false;
	      }
	      if( ch == '\n' ) {
		bol = true;
	      } else {
		bol = false;
	      }
	    }
	  }
	  editText.replaceText( buf.toString() );
	  editText.setDataChanged();
	  editText.setCaretPosition( newCrs );
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
	if( textFind != null ) {
	  if( textFind.isEmpty() ) {
	    textFind = null;
	  }
	}
	if( textFind == null ) {
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
	    this.textFind       = dlg.getFindText();
	    this.textReplace    = dlg.getReplaceText();
	    this.findIgnoreCase = dlg.getIgnoreCase();

	    findText(
		editText,
                textArea,
                Math.max(
                        textArea.getCaretPosition(),
                        textArea.getSelectionEnd() ),
		false,
                true );
	    break;

	  case REPLACE_ALL:
	    this.textFind       = dlg.getFindText();
	    this.textReplace    = dlg.getReplaceText();
	    this.findIgnoreCase = dlg.getIgnoreCase();

	    boolean found = findText( editText, textArea, 0, false, false );
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
                        false,
			false );
	    }

	    if( n == 0 ) {
	      TextUtil.showTextNotFound( this );
	    } else {
	      BaseDlg.showInfoDlg(
                this,
                String.valueOf( n ) + " Textersetzungen durchgef\u00FChrt.",
                "Text ersetzen" );
	    }
	    break;
	}
      }
    }
  }


  private void doEditFindNext( boolean backward )
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      JTextArea textArea = editText.getJTextArea();
      if( textArea != null ) {
	if( this.textFind == null ) {
	  doEditFind();
	} else {
	  if( backward ) {
	    findText(
		editText,
                textArea,
                Math.min(
                        textArea.getCaretPosition(),
                        textArea.getSelectionStart() ) - 1,
		true,
                true );
	  } else {
	    findText(
		editText,
                textArea,
                Math.max(
                        textArea.getCaretPosition(),
                        textArea.getSelectionEnd() ),
		false,
                true );
	  }
	}
      }
    }
  }


  private void doEditReplace()
  {
    JTextArea textArea = getSelectedJTextArea();
    if( textArea != null ) {
      if( this.textReplace != null ) {
        replaceText( textArea );
      }
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
		  dstPos = pos;
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
        lineNum = textArea.getLineOfOffset( textArea.getCaretPosition() ) + 1;
      }
      catch( BadLocationException ex ) {
        lineNum = null;
      }

      // Dialog aufrufen
      ReplyIntDlg dlg = new ReplyIntDlg(
                                this,
                                "Zeilenummer:",
                                lineNum,
                                1,
                                null );
      dlg.setTitle( "Gehe zu Zeile" );
      dlg.setVisible( true );
      lineNum = dlg.getReply();

      // zur Zeile springen
      if( lineNum != null ) {
	gotoLine( textArea, lineNum.intValue() );
      }
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

  private void doPrgAssemble( boolean forceOptionsDlg, boolean forceRun )
  {
    if( this.prgThread == null ) {
      EditText editText = getSelectedEditText();
      if( editText != null ) {
	PrgOptions options = editText.getPrgOptions();
	if( options != null ) {
	  if( options instanceof BasicOptions ) {
	    forceOptionsDlg = true;
	  }
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
	  options.setForceRun( forceRun );
	  editText.setPrgOptions( options );
	  PrgThread thread = new AsmThread(
				this.emuThread,
				editText,
				options,
				openLog( editText, editText.getName() ) );
	  this.prgThread = thread;
	  this.mnuPrgCancel.setEnabled( true );
	  updFileButtons();
	  thread.start();
	}
      }
    }
  }


  private void doPrgCompile( boolean forceOptionsDlg, boolean forceRun )
  {
    if( this.prgThread == null ) {
      EditText editText = getSelectedEditText();
      if( editText != null ) {
	AbstractTarget target       = null;
	BasicOptions   basicOptions = null;
	PrgOptions     options      = editText.getPrgOptions();
	if( options != null ) {
	  if( options instanceof BasicOptions ) {
	    basicOptions = (BasicOptions) options;
	    target       = basicOptions.getTarget();
	  }
	}
	if( !forceOptionsDlg && (this.emuThread != null) ) {
	  /*
	   * Optionen-Dialog auf jeden Fall anzeigen,
	   * wenn sich das aktuelle System geaendert hat
	   */
	  forceOptionsDlg = true;
	  if( basicOptions != null ) {
	    EmuSys oldSys = basicOptions.getEmuSys();
	    EmuSys curSys = this.emuThread.getEmuSys();
	    if( (oldSys != null) && (curSys != null) ) {
	      if( oldSys == curSys ) {
		forceOptionsDlg = false;
	      }
	    }
	  }
	}
	if( forceOptionsDlg || (target == null) || (basicOptions == null) ) {
	  basicOptions       = null;
	  this.prgOptionsDlg = new BasicOptionsDlg(
						this,
						this.emuThread,
						options );
	  this.prgOptionsDlg.setVisible( true );
	  options = this.prgOptionsDlg.getAppliedOptions();
	  if( options != null ) {
	    if( options instanceof BasicOptions ) {
	      basicOptions = (BasicOptions) options;
	    }
	  }
	  this.prgOptionsDlg = null;
	}
	if( basicOptions != null ) {
	  if( this.emuThread != null ) {
	    basicOptions.setEmuSys( this.emuThread.getEmuSys() );
	  }
	  basicOptions.setForceRun( forceRun );
	  editText.setPrgOptions( basicOptions );
	  PrgThread thread = new BasicCompilerThread(
				this.emuThread,
				editText,
				basicOptions,
				openLog( editText, editText.getName() ) );
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
			Main.getLastDirFile( Main.FILE_GROUP_TEXT ),
			EmuUtil.getProjectFileFilter() );
    if( file != null ) {
      try {
	Properties props = loadProject( file );
	if( props != null ) {
	  openProject( file, props );
	} else {
	  BaseDlg.showErrorDlg(
		this,
		"Die ausgew\u00E4hlte Datei ist keine JKCEMU-Projektdatei." );
	}
      }
      catch( IOException ex ) {
	BaseDlg.showOpenFileErrorDlg( this, file, ex );
      }
    }
  }


  private void doPrjSave( boolean askFileName )
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      editText.saveProject( this, askFileName );
    }
  }


  private void doRemoveRowHeader()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      editText.removeRowHeader();
      updRemoveRowHeaderButtons();
    }
  }


	/* --- sonstige Aktionen --- */

  private void doCreateBreakpoint()
  {
    if( this.popupLineAddr >= 0 ) {
      EditText  editText  = getSelectedEditText();
      ScreenFrm screenFrm = Main.getScreenFrm();
      if( (editText != null) && (screenFrm != null) ) {
	boolean    secondSys = false;
	DebugFrm   debugFrm  = null;
	PrgOptions options   = editText.getPrgOptions();
	if( options != null ) {
	  secondSys = options.getCodeToSecondSystem();
	}
	if( secondSys ) {
	  debugFrm = screenFrm.openSecondDebugger();
	} else {
	  debugFrm = screenFrm.openPrimaryDebugger();
	}
	if( debugFrm != null ) {
	  debugFrm.doDebugBreakPCAdd( this.popupLineAddr );
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private TextEditFrm( EmuThread emuThread )
  {
    this.emuThread      = emuThread;
    this.editTexts      = new ArrayList<>();
    this.hasTextFind    = false;
    this.findIgnoreCase = true;
    this.textFind       = null;
    this.textReplace    = null;
    this.clipboard      = null;
    this.logFrm         = null;
    this.prgThread      = null;
    this.prgOptionsDlg  = null;
    this.lastNewTextNum = 0;
    this.popupLineAddr  = -1;
    this.shiftWidth     = Main.getIntProperty( PROP_SHIFT_WIDTH, 2 );
    this.shiftUseTabs   = Main.getBooleanProperty(
					PROP_SHIFT_USE_TABS,
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

    this.mnuEditRemoveWordstarFmt = createJMenuItem(
			"WordStar-Formatierungen entfernen" );
    mnuEdit.add( this.mnuEditRemoveWordstarFmt );
    mnuEdit.addSeparator();

    this.mnuEditFind = createJMenuItem(
		"Suchen und Ersetzen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_F, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditFind );

    this.mnuEditFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    mnuEdit.add( this.mnuEditFindNext );

    this.mnuEditFindPrev = createJMenuItem(
		"R\u00FCckw\u00E4rts suchen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F3, Event.SHIFT_MASK ) );
    mnuEdit.add( this.mnuEditFindPrev );

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

    this.mnuPrgLineAddrs = new JCheckBoxMenuItem(
		"Adressspalte nach Assemblieren anzeigen",
		Main.getBooleanProperty(
				PROP_SHOW_LINE_ADDRS,
				true ) );
    this.mnuPrgLineAddrs.addActionListener( this );
    mnuPrg.add( this.mnuPrgLineAddrs );

    this.mnuRemoveRowHeader = createJMenuItem( "Adressspalte ausblenden" );
    mnuPrg.add( this.mnuRemoveRowHeader );
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

    this.mnuPopupFindPrev = createJMenuItem( "R\u00FCckw\u00E4rts suchen" );
    this.mnuPopup.add( this.mnuPopupFindPrev );

    this.mnuPopupReplace = createJMenuItem( "Ersetzen" );
    this.mnuPopup.add( this.mnuPopupReplace );
    this.mnuPopup.addSeparator();

    if( this.emuThread != null ) {
      this.mnuPopupBreak = createJMenuItem( "Halte-/Log-Punkt anlegen" );
      this.mnuPopup.add( this.mnuPopupBreak );
      this.mnuPopup.addSeparator();
    } else {
      this.mnuPopupBreak = null;
    }

    this.mnuPopupSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    this.mnuPopup.add( this.mnuPopupSelectAll );


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
				"/images/file/close.png",
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


    // leeren Text erzeugen
    doFileNew();


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      boolean done = false;
      if( !this.editTexts.isEmpty() ) {
	JTextArea textArea = this.editTexts.get( 0 ).getJTextArea();
	if( textArea != null ) {
	  textArea.setColumns( 80 );
	  textArea.setRows( 25 );
	  setLocationByPlatform( true );
	  pack();
	  done = true;
	}
      }
      if( !done ) {
	setBoundsToDefaults();
      }
    }
    setResizable( true );


    // sonstiges
    updCaretButtons();
    updUndoButtons();
    updTitle();
    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
      if( this.clipboard != null ) {
	this.clipboard.addFlavorListener( this );
      }
    }
    updPasteButtons();
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


  private EditText checkFileAlreadyOpen( File file )
  {
    EditText rv = null;
    for( EditText editText : this.editTexts ) {
      if( editText.isSameFile( file ) ) {
        setSelectedEditText( editText );
        BaseDlg.showInfoDlg(
		this,
		"Diese Datei ist bereits ge\u00F6ffnet.",
		"Hinweis" );
        rv = editText;
      }
    }
    return rv;
  }


  private JTextArea createJTextArea()
  {
    JTextArea textArea = new JTextArea();
    textArea.setMargin( new Insets( 5, 5, 5, 5 ) );

    String tabSizeText = Main.getProperty( PROP_TABSIZE );
    if( tabSizeText != null ) {
      if( !tabSizeText.isEmpty() ) {
	try {
	  int tabSize = Integer.parseInt( tabSizeText );
	  if( (tabSize > 0) && (tabSize < 100) ) {
	    textArea.setTabSize( tabSize );
	  }
	}
	catch( NumberFormatException ex ) {}
      }
    }

    Font font = textArea.getFont();
    if( font != null ) {
      textArea.setFont(
	new Font( Font.MONOSPACED, font.getStyle(), font.getSize() ) );
    } else {
      textArea.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 12 ) );
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
	if( level <= 0 ) {
	  return pos;
	}
      }
      pos += step;
    }
    return null;
  }


  private boolean findText(
			EditText  editText,
                        JTextArea textArea,
                        int       startPos,
			boolean   backward,
                        boolean   interactive )
  {
    boolean rv       = false;
    String  textFind = this.textFind;
    String  textBase = textArea.getText();
    if( textBase == null ) {
      textBase = "";
    }
    int len = textBase.length();

    if( (textFind != null) && !textFind.isEmpty() ) {
      this.hasTextFind = true;
      setFindNextBtnsEnabled( this.hasTextFind );

      if( this.findIgnoreCase ) {
        textFind = textFind.toUpperCase();
        textBase = textBase.toUpperCase();
      }
      int posFind = -1;
      if( backward ) {
	if( startPos >= (len - 1) ) {
	  startPos = len - 1;
	}
	if( startPos > 0 ) {
	  posFind = textBase.lastIndexOf( textFind, startPos );
	}
      } else {
	if( startPos < 0 ) {
	  startPos = 0;
	}
	if( startPos < len ) {
	  posFind = textBase.indexOf( textFind, startPos );
	}
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
      if( backward ) {
	if( startPos < (len - 1) ) {
	  rv = findText( editText, textArea, len - 1, true, interactive );
	} else {
	  TextUtil.showTextNotFound( this );
	}
      } else {
	if( startPos > 0 ) {
	  rv = findText( editText, textArea, 0, false, interactive );
	} else {
	  TextUtil.showTextNotFound( this );
	}
      }
    }
    return rv;
  }


  private int getLineAddr( MouseEvent e )
  {
    int addr = -1;
    try {
      Component c    = e.getComponent();
      Point     pt   = e.getPoint();
      if( (c != null) && (pt != null) ) {
	JScrollPane scrollPane = null;
	Component   parent     = c.getParent();
	while( parent != null ) {
	  if( parent instanceof JScrollPane ) {
	    scrollPane = (JScrollPane) parent;
	    break;
	  }
	  parent = parent.getParent();
	}
	if( scrollPane != null ) {
	  JViewport vp = scrollPane.getRowHeader();
	  if( vp != null ) {
	    Component v = vp.getView();
	    if( v != null ) {
	      if( v instanceof LineAddrRowHeader ) {
		JTextArea textArea = ((LineAddrRowHeader) v).getJTextArea();
		if( (textArea != null) && (textArea == c) ) {
		  int offs = textArea.viewToModel( pt );
		  if( offs >= 0 ) {
		    addr = ((LineAddrRowHeader) v).getAddrByLine(
					textArea.getLineOfOffset( offs ) );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    catch( BadLocationException ex ) {}
    return addr;
  }


  private EditText getSelectedEditText()
  {
    Component c = this.tabbedPane.getSelectedComponent();
    if( c != null ) {
      for( EditText editText : this.editTexts ) {
        if( editText.getTabComponent() == c ) {
          return editText;
	}
      }
    }
    return null;
  }


  private JTextArea getSelectedJTextArea()
  {
    EditText editText = getSelectedEditText();
    return editText != null ? editText.getJTextArea() : null;
  }


  private EditText forceOpenFile(
			File          file,
			CharConverter charConverter,
                	String        encodingName,
                	String        encodingDisplayText,
			boolean       ignoreEofByte )
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      if( editText.isUsed() ) {
	editText = null;
      }
    }
    try {
      if( editText != null ) {
	editText.loadFile(
			file,
			charConverter,
			encodingName,
			encodingDisplayText,
			ignoreEofByte );

	// neuer Dateiname auf dem Reiter
	Component tabComponent = editText.getTabComponent();
	if( tabComponent != null ) {
	  int i = this.tabbedPane.indexOfComponent( tabComponent );
	  if( i >= 0 ) {
	    this.tabbedPane.setTitleAt( i, editText.getName() );
	  }
	}
      }
      if( editText == null ) {
        editText = new EditText(
				this,
				file,
				charConverter,
				encodingName,
				encodingDisplayText,
				ignoreEofByte );
        JTextArea   textArea   = createJTextArea();
        JScrollPane scrollPane = new JScrollPane( textArea );
        editText.setComponents( scrollPane, textArea );
        this.editTexts.add( editText );
        tabbedPane.addTab( editText.getName(), scrollPane );
      }
      Main.setLastFile( file, Main.FILE_GROUP_TEXT );

      // Text sichtbar machen und Oberflaechenelemente aktualisieren
      setSelectedTabComponent( editText.getTabComponent() );
      updFileButtons();
      updStatusBar();
    }
    catch( IOException ex ) {
      editText = null;
      BaseDlg.showOpenFileErrorDlg( this, file, ex );
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
      editText = forceOpenFile( file, null, null, null, false );
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
    }
    EmuUtil.showFrame( this.logFrm );
    return this.logFrm;
  }


  private void removeRowHeaders()
  {
    for( EditText editText : this.editTexts ) {
      editText.removeRowHeader();
    }
    updRemoveRowHeaderButtons();
  }


  private boolean replaceText( JTextArea textArea )
  {
    int selBeg = textArea.getSelectionStart();
    int selEnd = textArea.getSelectionEnd();

    if( (selBeg < 0) || (selBeg >= selEnd) ) {
      return false;
    }

    textArea.replaceSelection(
			this.textReplace != null ? this.textReplace : "" );
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
    this.mnuEditRemoveWordstarFmt.setEnabled( state );
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
    this.mnuPopupSelectAll.setEnabled( state );
    this.btnPrint.setEnabled( state );
    this.btnClose.setEnabled( state );
    this.btnFind.setEnabled( state );
  }


  private void setFindNextBtnsEnabled( boolean state )
  {
    this.mnuEditFindPrev.setEnabled( state );
    this.mnuEditFindNext.setEnabled( state );
    this.mnuPopupFindPrev.setEnabled( state );
    this.mnuPopupFindNext.setEnabled( state );
  }


  private void setLineAddrs( Collection<PrgSource> prgSources )
  {
    for( EditText editText : this.editTexts ) {
      Component tabComponent = editText.getTabComponent();
      JTextArea textArea     = editText.getJTextArea();
      if( (tabComponent != null) && (textArea != null) ) {
	if( tabComponent instanceof JScrollPane ) {
	  Component rowHeader = null;
	  if( prgSources != null ) {
	    for( PrgSource prgSource : prgSources ) {
	      if( editText.isSameText( prgSource ) ) {
		Map<Integer,Integer> lineAddrMap = prgSource.getLineAddrMap();
		if( lineAddrMap != null ) {
		  rowHeader = new LineAddrRowHeader( textArea, lineAddrMap );
		}
	      }
	    }
	  }
	  ((JScrollPane) tabComponent).setRowHeaderView( rowHeader );
	}
      }
    }
    updRemoveRowHeaderButtons();
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
    BaseDlg.showInfoDlg( this, String.valueOf( cnt ) + " Ersetzungen" );
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
    updRemoveRowHeaderButtons();
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


  private void updRemoveRowHeaderButtons()
  {
    EditText editText = getSelectedEditText();
    if( editText != null ) {
      this.mnuRemoveRowHeader.setEnabled( editText.hasRowHeader() );
    } else {
      this.mnuRemoveRowHeader.setEnabled( false );
    }
  }


  private void updShowLineAddrs()
  {
    if( !this.mnuPrgLineAddrs.isSelected() )
      removeRowHeaders();
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
