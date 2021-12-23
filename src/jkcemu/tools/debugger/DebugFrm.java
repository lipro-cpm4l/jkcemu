/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Debugger-Fenster
 */

package jkcemu.tools.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.PopupMenusOwner;
import jkcemu.base.ReplyIntDlg;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;
import jkcemu.tools.ToolUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import z80emu.Z80Breakpoint;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80Memory;
import z80emu.Z80ReassInstr;
import z80emu.Z80Reassembler;
import z80emu.Z80StatusListener;


public class DebugFrm extends BaseFrm implements
						ChangeListener,
						ComponentListener,
						FocusListener,
						ListSelectionListener,
						PopupMenusOwner,
						Z80StatusListener
{
  /*
   * Da generic Arrays in Java nicht moeglich sind,
   * wird fuer JList<AbstractBreakpoint> eine eigene Klasse angelegt,
   * mit der dann auch Arrays ohne Compiler-Warnung moeglich sind.
   */
  public class BreakpointList extends JList<AbstractBreakpoint>
  {
    public BreakpointList( BreakpointListModel model )
    {
      super( model );
    }
  }

  private static final String HELP_PAGE = "/help/tools/debugger.htm";

  private static final int BP_PC_IDX        = 0;
  private static final int BP_MEMORY_IDX    = 1;
  private static final int BP_INPUT_IDX     = 2;
  private static final int BP_OUTPUT_IDX    = 3;
  private static final int BP_INTERRUPT_IDX = 4;
  private static final int BP_GROUP_CNT     = 5;

  private static final int TAB_IDX_CPU = 0;
  private static final int TAB_IDX_LOG = 1;
  private static final int TAB_IDX_VAR = 2;

  private static final int DEFAULT_FLD_MEM_PC_ROWS = 5;

  public static final String ELEM_ROOT        = "jkcemu";
  public static final String ELEM_BREAKPOINTS = "breakpoints";
  public static final String ELEM_VARIABLES   = "variables";

  private static final String PROP_MAX_LOG_COUNT = "max_log_count";

  private static final String TEXT_STOP
			= "Programmausf\u00FChrung anhalten";
  private static final String TEXT_RUN
			= "Programm bis Haltepunkt ausf\u00FChren";
  private static final String TEXT_WALK
			= "Programm bis Haltepunkt langsam ausf\u00FChren";
  private static final String TEXT_STEP_OVER
			= "Einzelschritt \u00FCber Aufruf hinweg";
  private static final String TEXT_STEP_INTO
			= "Einzelschritt in Aufruf hinein";
  private static final String TEXT_STEP_TO_RET
			= "Bis RET ausf\u00FChren";

  private static final String TEXT_BP_EDIT
			= "Halte-/Log-Punkt bearbeiten...";
  private static final String TEXT_BP_REMOVE
			= "Ausgew\u00E4hlte Halte-/Log-Punkte entfernen";
  private static final String TEXT_BP_REMOVE_ALL
			= "Alle Halte-/Log-Punkte entfernen";

  private static final String TEXT_BP_ENABLE_STOP
	= "In ausgew\u00E4hlten Halte-/Log-Punkten Anhalten aktivieren";
  private static final String TEXT_BP_DISABLE_STOP
	= "In ausgew\u00E4hlten Halte-/Log-Punkten Anhalten deaktivieren";

  private static final String TEXT_BP_ENABLE_STOP_ALL
		= "In allen Halte-/Log-Punkten Anhalten aktivieren";
  private static final String TEXT_BP_DISABLE_STOP_ALL
		= "In allen Halte-/Log-Punkten Anhalten deaktivieren";

  private static final String TEXT_BP_ENABLE_LOG
	= "In ausgew\u00E4hlten Halte-/Log-Punkten Loggen aktivieren";
  private static final String TEXT_BP_DISABLE_LOG
	= "In ausgew\u00E4hlten Halte-/Log-Punkten Loggen deaktivieren";

  private static final String TEXT_BP_ENABLE_LOG_ALL
		= "In allen Halte-/Log-Punkten Loggen aktivieren";
  private static final String TEXT_BP_DISABLE_LOG_ALL
		= "In allen Halte-/Log-Punkten Loggen deaktivieren";

  private static final String TEXT_VAR_ADD
			= "Variable hinzuf\u00FCgen...";
  private static final String TEXT_VAR_EDIT
			= "Variable bearbeiten...";
  private static final String TEXT_VAR_BP_ADD
			= "Halte-/Log-Punkt auf Variable hinzuf\u00FCgen...";
  private static final String TEXT_VAR_REMOVE
			= "Ausgew\u00E4hlte Variablen entfernen";
  private static final String TEXT_VAR_REMOVE_ALL
			= "Alle Variablen entfernen";

  private static final String TEXT_LOG_COPY
			= "Ausgew\u00E4hlte Log-Meldungen kopieren";
  private static final String TEXT_LOG_SELECT_ALL
			= "Alle Log-Meldungen ausw\u00E4hlen";
  private static final String TEXT_LOG_REMOVE
			= "Ausgew\u00E4hlte Log-Meldungen entfernen";
  private static final String TEXT_LOG_REMOVE_ALL
			= "Alle Log-Meldungen entfernen";

  private static final int DEFAULT_MAX_LOG_CNT = 500;

  private EmuThread                 emuThread;
  private Z80CPU                    cpu;
  private Z80Memory                 memory;
  private LabelImportOptions        labelImportOptions;
  private int                       maxLogCnt;
  private int                       memPCClickAddr;
  private int                       walkMillis;
  private javax.swing.Timer         walkTimer;
  private AbstractBreakpointDlg     bpDlg;
  private BreakpointListModel[]     bpModels;
  private DefaultListModel<String>  listModelLog;
  private VarTableModel             tableModelVar;
  private Z80InterruptSource[]      interruptSources;
  private Z80InterruptSource        lastSelectedIntSrc;
  private File                      lastBreakpointFile;
  private File                      lastTraceFile;
  private PrintWriter               traceWriter;
  private int                       popupBpGroupIdx;
  private JMenuItem                 mnuFileClose;
  private JMenuItem                 mnuFileBpsImport;
  private JMenuItem                 mnuFileBpsReImport;
  private JMenuItem                 mnuFileBpsRemoveImported;
  private JMenuItem                 mnuFileBpsVarsLoad;
  private JMenuItem                 mnuFileBpsVarsSave;
  private JMenuItem                 mnuExecRun;
  private JMenuItem                 mnuExecWalk30;
  private JMenuItem                 mnuExecWalk100;
  private JMenuItem                 mnuExecWalk300;
  private JMenuItem                 mnuExecWalk500;
  private JMenuItem                 mnuExecStop;
  private JMenuItem                 mnuExecStepOver;
  private JMenuItem                 mnuExecStepInto;
  private JMenuItem                 mnuExecStepToRET;
  private JCheckBoxMenuItem         mnuExecTracer;
  private JMenuItem                 mnuBpInterruptAdd;
  private JMenuItem                 mnuBpInputAdd;
  private JMenuItem                 mnuBpOutputAdd;
  private JMenuItem                 mnuBpMemoryAdd;
  private JMenuItem                 mnuBpPCAdd;
  private JMenuItem                 mnuBpEdit;
  private JMenuItem                 mnuBpEnableStop;
  private JMenuItem                 mnuBpDisableStop;
  private JMenuItem                 mnuBpEnableStopAll;
  private JMenuItem                 mnuBpDisableStopAll;
  private JMenuItem                 mnuBpEnableLog;
  private JMenuItem                 mnuBpDisableLog;
  private JMenuItem                 mnuBpEnableLogAll;
  private JMenuItem                 mnuBpDisableLogAll;
  private JMenuItem                 mnuBpRemove;
  private JMenuItem                 mnuBpRemoveAll;
  private JMenuItem                 mnuVarAdd;
  private JMenuItem                 mnuVarEdit;
  private JMenuItem                 mnuVarBpAdd;
  private JMenuItem                 mnuVarRemove;
  private JMenuItem                 mnuVarRemoveAll;
  private JMenuItem                 mnuLogCopy;
  private JMenuItem                 mnuLogSelectAll;
  private JMenuItem                 mnuLogRemove;
  private JMenuItem                 mnuLogRemoveAll;
  private JMenuItem                 mnuLogMaxLogCnt;
  private JMenuItem                 mnuHelpContent;
  private JPopupMenu                popupBp;
  private JMenuItem                 popupBpAdd;
  private JMenuItem                 popupBpEdit;
  private JMenuItem                 popupBpEnableStop;
  private JMenuItem                 popupBpDisableStop;
  private JMenuItem                 popupBpEnableStopAll;
  private JMenuItem                 popupBpDisableStopAll;
  private JMenuItem                 popupBpEnableLog;
  private JMenuItem                 popupBpDisableLog;
  private JMenuItem                 popupBpEnableLogAll;
  private JMenuItem                 popupBpDisableLogAll;
  private JMenuItem                 popupBpRemove;
  private JMenuItem                 popupBpRemoveAll;
  private JPopupMenu                popupLog;
  private JMenuItem                 popupLogCopy;
  private JMenuItem                 popupLogSelectAll;
  private JMenuItem                 popupLogRemove;
  private JMenuItem                 popupLogRemoveAll;
  private JPopupMenu                popupVar;
  private JMenuItem                 popupVarAdd;
  private JMenuItem                 popupVarEdit;
  private JMenuItem                 popupVarBpAdd;
  private JMenuItem                 popupVarRemove;
  private JMenuItem                 popupVarRemoveAll;
  private JPopupMenu                popupWalk;
  private JMenuItem                 popupWalk30;
  private JMenuItem                 popupWalk100;
  private JMenuItem                 popupWalk300;
  private JMenuItem                 popupWalk500;
  private JPopupMenu                popupMemPC;
  private JMenuItem                 popupMemPCCopy;
  private JMenuItem                 popupMemPCBreak;
  private JTabbedPane               tabbedPane;
  private JButton                   btnRun;
  private JButton                   btnWalk;
  private JButton                   btnStop;
  private JButton                   btnStepOver;
  private JButton                   btnStepInto;
  private JButton                   btnStepToRET;
  private AbstractButton            btnFlagSign;
  private AbstractButton            btnFlagZero;
  private AbstractButton            btnFlagHalf;
  private AbstractButton            btnFlagPV;
  private AbstractButton            btnFlagN;
  private AbstractButton            btnFlagCarry;
  private AbstractButton            btnIFF1;
  private AbstractButton            btnIFF2;
  private JSpinner                  spinnerIntMode;
  private JTextField                fldRegAF;
  private JTextField                fldRegAsciiA;
  private JTextField                fldRegBC;
  private JTextField                fldRegAsciiB;
  private JTextField                fldRegAsciiC;
  private JTextField                fldRegDE;
  private JTextField                fldRegAsciiD;
  private JTextField                fldRegAsciiE;
  private JTextField                fldRegHL;
  private JTextField                fldRegAsciiH;
  private JTextField                fldRegAsciiL;
  private JTextField                fldRegIX;
  private JTextField                fldRegAsciiIXH;
  private JTextField                fldRegAsciiIXL;
  private JTextField                fldRegIY;
  private JTextField                fldRegAsciiIYH;
  private JTextField                fldRegAsciiIYL;
  private JTextField                fldRegPC;
  private JTextField                fldRegSP;
  private JTextField                fldRegAF2;
  private JTextField                fldRegBC2;
  private JTextField                fldRegDE2;
  private JTextField                fldRegHL2;
  private JTextField                fldRegI;
  private JTextField                fldMemBC;
  private JTextField                fldMemDE;
  private JTextField                fldMemHL;
  private JTextField                fldMemIX;
  private JTextField                fldMemIY;
  private JTextField                fldMemSP;
  private JTextArea                 fldMemPC;
  private JScrollPane               spMemPC;
  private JTextField                fldTStates;
  private JButton                   btnResetTStates;
  private JLabel                    labelIntMode;
  private JLabel                    labelRegI;
  private JLabel                    labelStatus;
  private JPanel                    panelCPU;
  private BreakpointList[]          bpLists;
  private JScrollPane[]             bpScrollPanes;
  private JSplitPane                splitBpDown;
  private JList<Z80InterruptSource> listIntSrc;
  private JEditorPane               fldIntSrc;
  private JList<String>             listLog;
  private JTable                    tableVar;
  private ListSelectionModel        selectionModelVar;
  private JEditorPane               fldEtc;
  private javax.swing.Timer         timerForClear;


  public DebugFrm(
		EmuThread emuThread,
		Z80CPU    cpu,
		Z80Memory memory )
  {
    this.emuThread          = emuThread;
    this.cpu                = cpu;
    this.memory             = memory;
    this.labelImportOptions = null;
    this.maxLogCnt          = DEFAULT_MAX_LOG_CNT;
    this.memPCClickAddr     = -1;
    this.walkMillis         = 0;
    this.walkTimer          = new javax.swing.Timer( 300, this );
    this.walkTimer.setRepeats( false );
    this.lastSelectedIntSrc = null;
    this.lastBreakpointFile = null;
    this.lastTraceFile      = null;
    this.traceWriter        = null;
    this.bpDlg              = null;
    this.bpModels           = new BreakpointListModel[ BP_GROUP_CNT ];
    this.bpLists            = new BreakpointList[ BP_GROUP_CNT ];
    this.bpScrollPanes      = new JScrollPane[ BP_GROUP_CNT ];
    this.popupBpGroupIdx    = -1;
    this.interruptSources   = cpu.getInterruptSources();
    if( this.interruptSources != null ) {
      if( this.interruptSources.length == 0 ) {
	this.interruptSources = null;
      }
    }
    setTitle( "JKCEMU Debugger" );


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuFileBpsVarsLoad = createMenuItem(
		"Halte-/Log-Punkte und Variablen laden..." );
    mnuFile.add( this.mnuFileBpsVarsLoad );

    this.mnuFileBpsVarsSave = createMenuItem(
		"Halte-/Log-Punkte und Variablen speichern..." );
    mnuFile.add( this.mnuFileBpsVarsSave );
    mnuFile.addSeparator();

    this.mnuFileBpsImport = createMenuItem(
		"Halte-/Log-Punkte importieren..." );
    mnuFile.add( this.mnuFileBpsImport );

    this.mnuFileBpsReImport = createMenuItemWithDirectAccelerator(
			"Halte-/Log-Punkte erneut importieren",
			KeyEvent.VK_F3 );
    this.mnuFileBpsReImport.setEnabled( false );
    mnuFile.add( this.mnuFileBpsReImport );

    this.mnuFileBpsRemoveImported = createMenuItem(
				"Importierte Halte-/Log-Punkte entfernen" );
    this.mnuFileBpsRemoveImported.setEnabled( false );
    mnuFile.add( this.mnuFileBpsRemoveImported );
    mnuFile.addSeparator();

    this.mnuFileClose = createMenuItemClose();
    mnuFile.add( this.mnuFileClose );


    // Menu Programmausfuehrung
    JMenu mnuExec = GUIFactory.createMenu( "Programmausf\u00FChrung" );
    mnuExec.setMnemonic( KeyEvent.VK_A );

    this.mnuExecStop = createMenuItemWithDirectAccelerator(
						TEXT_STOP,
						KeyEvent.VK_F4 );
    mnuExec.add( this.mnuExecStop );

    this.mnuExecRun = createMenuItemWithDirectAccelerator(
						TEXT_RUN,
						KeyEvent.VK_F5 );
    mnuExec.add( this.mnuExecRun );

    JMenu mnuExecWalk = GUIFactory.createMenu( TEXT_WALK );
    mnuExec.add( mnuExecWalk );

    this.mnuExecWalk500 = createMenuItem( "sehr langsam" );
    mnuExecWalk.add( this.mnuExecWalk500 );

    this.mnuExecWalk300 = createMenuItem( "langsam" );
    mnuExecWalk.add( this.mnuExecWalk300 );

    this.mnuExecWalk100 = createMenuItem( "etwas schneller" );
    mnuExecWalk.add( this.mnuExecWalk100 );

    this.mnuExecWalk30 = createMenuItem( "schneller" );
    mnuExecWalk.add( this.mnuExecWalk30 );

    this.mnuExecStepOver = createMenuItemWithDirectAccelerator(
						TEXT_STEP_OVER,
						KeyEvent.VK_F6 );
    mnuExec.add( this.mnuExecStepOver );

    this.mnuExecStepInto = createMenuItemWithDirectAccelerator(
						TEXT_STEP_INTO,
						KeyEvent.VK_F7 );
    mnuExec.add( this.mnuExecStepInto );

    this.mnuExecStepToRET = createMenuItemWithDirectAccelerator(
						TEXT_STEP_TO_RET,
						KeyEvent.VK_F8 );
    mnuExec.add( this.mnuExecStepToRET );
    mnuExec.addSeparator();

    this.mnuExecTracer = GUIFactory.createCheckBoxMenuItem(
						"Befehle aufzeichnen",
						false );
    this.mnuExecTracer.setSelected( false );
    this.mnuExecTracer.addActionListener( this );
    mnuExec.add( this.mnuExecTracer );


    // Menu Halte-/Log-Punkte
    JMenu mnuBp = GUIFactory.createMenu( "Halte-/Log-Punkte" );
    mnuBp.setMnemonic( KeyEvent.VK_P );

    this.mnuBpPCAdd = createMenuItemWithStandardAccelerator(
		"Halte-/Log-Punkt auf Programmadresse hinzuf\u00FCgen...",
		KeyEvent.VK_A );
    mnuBp.add( this.mnuBpPCAdd );

    this.mnuBpMemoryAdd = createMenuItemWithStandardAccelerator(
		"Halte-/Log-Punkt auf Speicherbereich hinzuf\u00FCgen...",
		KeyEvent.VK_M );
    mnuBp.add( this.mnuBpMemoryAdd );

    this.mnuBpInputAdd = createMenuItemWithStandardAccelerator(
		"Halte-/Log-Punkt auf Eingabetor hinzuf\u00FCgen...",
		KeyEvent.VK_I );
    mnuBp.add( this.mnuBpInputAdd );

    this.mnuBpOutputAdd = createMenuItemWithStandardAccelerator(
		"Halte-/Log-Punkt auf Ausgabetor hinzuf\u00FCgen...",
		KeyEvent.VK_O );
    mnuBp.add( this.mnuBpOutputAdd );

    this.mnuBpInterruptAdd = createMenuItemWithStandardAccelerator(
		"Halte-/Log-Punkt auf Interrupt-Quelle hinzuf\u00FCgen...",
		KeyEvent.VK_Q );
    mnuBp.add( this.mnuBpInterruptAdd );

    this.mnuBpEdit = createMenuItemWithStandardAccelerator(
						TEXT_BP_EDIT,
						KeyEvent.VK_E );
    mnuBp.add( this.mnuBpEdit );
    mnuBp.addSeparator();

    this.mnuBpRemove = createMenuItem( TEXT_BP_REMOVE );
    mnuBp.add( this.mnuBpRemove );

    this.mnuBpRemoveAll = createMenuItem( TEXT_BP_REMOVE_ALL );
    mnuBp.add( this.mnuBpRemoveAll );
    mnuBp.addSeparator();

    this.mnuBpEnableStop = createMenuItem( TEXT_BP_ENABLE_STOP );
    mnuBp.add( this.mnuBpEnableStop );

    this.mnuBpDisableStop = createMenuItem( TEXT_BP_DISABLE_STOP );
    mnuBp.add( this.mnuBpDisableStop );
    mnuBp.addSeparator();

    this.mnuBpEnableStopAll = createMenuItem( TEXT_BP_ENABLE_STOP_ALL );
    mnuBp.add( this.mnuBpEnableStopAll );

    this.mnuBpDisableStopAll = createMenuItem( TEXT_BP_DISABLE_STOP_ALL );
    mnuBp.add( this.mnuBpDisableStopAll );
    mnuBp.addSeparator();

    this.mnuBpEnableLog = createMenuItem( TEXT_BP_ENABLE_LOG );
    mnuBp.add( this.mnuBpEnableLog );

    this.mnuBpDisableLog = createMenuItem( TEXT_BP_DISABLE_LOG );
    mnuBp.add( this.mnuBpDisableLog );
    mnuBp.addSeparator();

    this.mnuBpEnableLogAll = createMenuItem( TEXT_BP_ENABLE_LOG_ALL );
    mnuBp.add( this.mnuBpEnableLogAll );

    this.mnuBpDisableLogAll = createMenuItem( TEXT_BP_DISABLE_LOG_ALL );
    mnuBp.add( this.mnuBpDisableLogAll );
    mnuBp.addSeparator();


    // Menu Log-Meldungen
    JMenu mnuLog = GUIFactory.createMenu( "Log-Meldungen" );
    mnuLog.setMnemonic( KeyEvent.VK_L );

    this.mnuLogCopy = createMenuItem( TEXT_LOG_COPY );
    mnuLog.add( this.mnuLogCopy );

    this.mnuLogSelectAll = createMenuItem( TEXT_LOG_SELECT_ALL );
    mnuLog.add( this.mnuLogSelectAll );
    mnuLog.addSeparator();

    this.mnuLogRemove = createMenuItem( TEXT_LOG_REMOVE );
    mnuLog.add( this.mnuLogRemove );

    this.mnuLogRemoveAll = createMenuItem( TEXT_LOG_REMOVE_ALL );
    mnuLog.add( this.mnuLogRemoveAll );
    mnuLog.addSeparator();

    this.mnuLogMaxLogCnt = createMenuItem( "Max. Anzahl Log-Meldungen..." );
    mnuLog.add( this.mnuLogMaxLogCnt );


    // Menu Variablen
    JMenu mnuVar = GUIFactory.createMenu( "Variablen" );
    mnuVar.setMnemonic( KeyEvent.VK_V );

    this.mnuVarAdd = createMenuItem( TEXT_VAR_ADD );
    mnuVar.add( this.mnuVarAdd );

    this.mnuVarEdit = createMenuItem( TEXT_VAR_EDIT );
    mnuVar.add( this.mnuVarEdit );
    mnuVar.addSeparator();

    this.mnuVarBpAdd = createMenuItem( TEXT_VAR_BP_ADD );
    mnuVar.add( this.mnuVarBpAdd );
    mnuVar.addSeparator();

    this.mnuVarRemove = createMenuItem( TEXT_VAR_REMOVE );
    mnuVar.add( this.mnuVarRemove );

    this.mnuVarRemoveAll = createMenuItem( TEXT_VAR_REMOVE_ALL );
    mnuVar.add( this.mnuVarRemoveAll );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem( "Hilfe zum Debugger..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar(
					mnuFile,
					mnuExec,
					mnuBp,
					mnuLog,
					mnuVar,
					mnuHelp ) );


    // Popup-Menu fuer Halte-/Log-Punkt
    this.popupBp = GUIFactory.createPopupMenu();

    this.popupBpAdd = createMenuItem(
				"Halte-/Log-Punkt hinzuf\u00FCgen..." );
    this.popupBp.add( this.popupBpAdd );

    this.popupBpEdit = createMenuItem( TEXT_BP_EDIT );
    this.popupBp.add( this.popupBpEdit );
    this.popupBp.addSeparator();

    this.popupBpEnableStop = createMenuItem( TEXT_BP_ENABLE_STOP );
    this.popupBp.add( this.popupBpEnableStop );

    this.popupBpDisableStop = createMenuItem( TEXT_BP_DISABLE_STOP );
    this.popupBp.add( this.popupBpDisableStop );
    this.popupBp.addSeparator();

    this.popupBpEnableLog = createMenuItem( TEXT_BP_ENABLE_LOG );
    this.popupBp.add( this.popupBpEnableLog );

    this.popupBpDisableLog = createMenuItem( TEXT_BP_DISABLE_LOG );
    this.popupBp.add( this.popupBpDisableLog );
    this.popupBp.addSeparator();

    this.popupBpEnableStopAll = createMenuItem( TEXT_BP_ENABLE_STOP_ALL );
    this.popupBp.add( this.popupBpEnableStopAll );

    this.popupBpDisableStopAll = createMenuItem( TEXT_BP_DISABLE_STOP_ALL );
    this.popupBp.add( this.popupBpDisableStopAll );
    this.popupBp.addSeparator();

    this.popupBpEnableLogAll = createMenuItem( TEXT_BP_ENABLE_LOG_ALL );
    this.popupBp.add( this.popupBpEnableLogAll );

    this.popupBpDisableLogAll = createMenuItem( TEXT_BP_DISABLE_LOG_ALL );
    this.popupBp.add( this.popupBpDisableLogAll );
    this.popupBp.addSeparator();

    this.popupBpRemove = createMenuItem( TEXT_BP_REMOVE );
    this.popupBp.add( this.popupBpRemove );

    this.popupBpRemoveAll = createMenuItem( TEXT_BP_REMOVE_ALL );
    this.popupBp.add( this.popupBpRemoveAll );


    // Popup-Menu fuer Log-Meldungen
    this.popupLog = GUIFactory.createPopupMenu();

    this.popupLogCopy = createMenuItem( TEXT_LOG_COPY );
    this.popupLog.add( this.popupLogCopy );

    this.popupLogSelectAll = createMenuItem( TEXT_LOG_SELECT_ALL );
    this.popupLog.add( this.popupLogSelectAll );
    this.popupLog.addSeparator();

    this.popupLogRemove = createMenuItem( TEXT_LOG_REMOVE );
    this.popupLog.add( this.popupLogRemove );

    this.popupLogRemoveAll = createMenuItem( TEXT_LOG_REMOVE_ALL );
    this.popupLog.add( this.popupLogRemoveAll );


    // Popup-Menu fuer Variablen
    this.popupVar = GUIFactory.createPopupMenu();

    this.popupVarAdd = createMenuItem( TEXT_VAR_ADD );
    this.popupVar.add( this.popupVarAdd );

    this.popupVarEdit = createMenuItem( TEXT_VAR_EDIT );
    this.popupVar.add( this.popupVarEdit );
    this.popupVar.addSeparator();

    this.popupVarBpAdd = createMenuItem( TEXT_VAR_BP_ADD );
    this.popupVar.add( this.popupVarBpAdd );
    this.popupVar.addSeparator();

    this.popupVarRemove = createMenuItem( TEXT_VAR_REMOVE );
    this.popupVar.add( this.popupVarRemove );

    this.popupVarRemoveAll = createMenuItem( TEXT_VAR_REMOVE_ALL );
    this.popupVar.add( this.popupVarRemoveAll );


    // Popup-Menu fuer langsames ausfuehren
    this.popupWalk = GUIFactory.createPopupMenu();

    this.popupWalk500 = createMenuItem(
				"Programm sehr langsam ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk500 );

    this.popupWalk300 = createMenuItem( "Programm langsam ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk300 );

    this.popupWalk100 = createMenuItem(
				"Programm etwas schneller ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk100 );

    this.popupWalk30 = createMenuItem(
				"Programm schneller ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk30 );


    // Popup-Menu in der Reassembler-Anzeige des PC-Bereichs
    this.popupMemPC = GUIFactory.createPopupMenu();

    this.popupMemPCCopy = createMenuItem( EmuUtil.TEXT_COPY );
    this.popupMemPC.add( this.popupMemPCCopy );
    this.popupMemPC.addSeparator();

    this.popupMemPCBreak = createMenuItem(
				"Halte-/Log-Punkt hinzuf\u00FCgen..." );
    this.popupMemPC.add( this.popupMemPCBreak );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Werkzeugleiste
    JToolBar toolBar = GUIFactory.createToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, BorderLayout.NORTH );

    this.btnStop = GUIFactory.createRelImageResourceButton(
					this,
					"debug/stop.png",
					TEXT_STOP );
    this.btnStop.addActionListener( this );
    toolBar.add( this.btnStop );

    this.btnRun = GUIFactory.createRelImageResourceButton(
					this,
					"debug/run.png",
					TEXT_RUN );
    this.btnRun.addActionListener( this );
    toolBar.add( this.btnRun );

    this.btnWalk = GUIFactory.createRelImageResourceButton(
					this,
					"debug/walk.png",
					TEXT_WALK );
    this.btnWalk.addActionListener( this );
    toolBar.add( this.btnWalk );

    this.btnStepOver = GUIFactory.createRelImageResourceButton(
					this,
					"debug/step_over.png",
					TEXT_STEP_OVER );
    this.btnStepOver.addActionListener( this );
    toolBar.add( this.btnStepOver );

    this.btnStepInto = GUIFactory.createRelImageResourceButton(
					this,
					"debug/step_into.png",
					TEXT_STEP_INTO );
    this.btnStepInto.addActionListener( this );
    toolBar.add( this.btnStepInto );

    this.btnStepToRET = GUIFactory.createRelImageResourceButton(
					this,
					"debug/step_up.png",
					TEXT_STEP_TO_RET );
    this.btnStepToRET.addActionListener( this );
    toolBar.add( this.btnStepToRET );


    // Hauptbereich
    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab CPU
    this.panelCPU = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "CPU", this.panelCPU );

    GridBagConstraints gbcCPU = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.VERTICAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Bereich Flags
    JPanel panelFlag = GUIFactory.createPanel( new GridBagLayout() );
    panelFlag.setBorder( GUIFactory.createTitledBorder( "Flags" ) );
    this.panelCPU.add( panelFlag, gbcCPU );

    GridBagConstraints gbcFlag = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 2, 2, 2, 2 ),
						0, 0 );

    this.btnFlagSign = createFlagField( "S" );
    panelFlag.add( this.btnFlagSign, gbcFlag );

    this.btnFlagZero = createFlagField( "Z" );
    gbcFlag.gridx++;
    panelFlag.add( this.btnFlagZero, gbcFlag );

    this.btnFlagHalf = createFlagField( "H" );
    gbcFlag.gridx++;
    panelFlag.add( this.btnFlagHalf, gbcFlag );

    this.btnFlagPV = createFlagField( "PV" );
    gbcFlag.gridx++;
    panelFlag.add( this.btnFlagPV, gbcFlag );

    this.btnFlagN = createFlagField( "N" );
    gbcFlag.gridx++;
    panelFlag.add( this.btnFlagN, gbcFlag );

    this.btnFlagCarry = createFlagField( "C" );
    gbcFlag.gridx++;
    panelFlag.add( this.btnFlagCarry, gbcFlag );


    // Bereich Interrupt
    JPanel panelInt = GUIFactory.createPanel( new GridBagLayout() );
    panelInt.setBorder( GUIFactory.createTitledBorder( "Interrupt" ) );
    gbcCPU.gridx++;
    this.panelCPU.add( panelInt, gbcCPU );

    GridBagConstraints gbcInt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 2, 2, 2, 2 ),
						0, 0 );
    this.labelIntMode = GUIFactory.createLabel( "IM:" );
    panelInt.add( this.labelIntMode, gbcInt );

    this.spinnerIntMode = GUIFactory.createSpinner(
				new SpinnerNumberModel( 0, 0, 2, 1 ) );
    gbcInt.insets.left  = 0;
    gbcInt.gridx++;
    panelInt.add( this.spinnerIntMode, gbcInt );

    this.labelRegI     = GUIFactory.createLabel( "IR:" );
    gbcInt.insets.left = 5;
    gbcInt.gridx++;
    panelInt.add( this.labelRegI, gbcInt );

    this.fldRegI = GUIFactory.createTextField( 3 );
    this.fldRegI.addActionListener( this );
    this.fldRegI.addFocusListener( this );
    this.fldRegI.setEditable( false );
    gbcInt.insets.left = 0;
    gbcInt.gridx++;
    panelInt.add( this.fldRegI, gbcInt );

    this.btnIFF1       = createFlagField( "IFF1" );
    gbcInt.insets.left = 2;
    gbcInt.gridx++;
    panelInt.add( this.btnIFF1, gbcInt );

    this.btnIFF2 = createFlagField( "IFF2" );
    gbcInt.gridx++;
    panelInt.add( this.btnIFF2, gbcInt );


    // Bereich Register
    JPanel panelReg = GUIFactory.createPanel( new GridBagLayout() );
    panelReg.setBorder( GUIFactory.createTitledBorder( "Register" ) );
    gbcCPU.fill      = GridBagConstraints.BOTH;
    gbcCPU.weighty   = 1.0;
    gbcCPU.gridwidth = 2;
    gbcCPU.gridx     = 0;
    gbcCPU.gridy++;
    this.panelCPU.add( panelReg, gbcCPU );

    GridBagConstraints gbcReg = new GridBagConstraints(
						1, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 2, 2, 2, 2 ),
						0, 0 );

    // Ueberschriften
    panelReg.add( GUIFactory.createLabel( "Hex" ), gbcReg );
    gbcReg.gridwidth = 2;
    gbcReg.gridx++;
    panelReg.add( GUIFactory.createLabel( "ASCII" ), gbcReg );
    gbcReg.gridwidth = 1;
    gbcReg.gridx += 2;
    panelReg.add(
		GUIFactory.createLabel( "Zeigt im Speicher auf" ),
		gbcReg );
    gbcReg.gridx += 2;
    panelReg.add( GUIFactory.createLabel( "Hex" ), gbcReg );


    // Register AF
    gbcReg.anchor = GridBagConstraints.WEST;
    gbcReg.gridx  = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "AF:" ), gbcReg );

    this.fldRegAF = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegAF, gbcReg );

    this.fldRegAsciiA = GUIFactory.createTextField( 2 );
    this.fldRegAsciiA.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiA, gbcReg );


    // Register AF2
    gbcReg.gridx += 3;
    panelReg.add( GUIFactory.createLabel( "AF\':" ), gbcReg );

    this.fldRegAF2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegAF2, gbcReg );


    // Register BC
    gbcReg.gridx  = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "BC:" ), gbcReg );

    this.fldRegBC = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegBC, gbcReg );

    this.fldRegAsciiB = GUIFactory.createTextField( 2 );
    this.fldRegAsciiB.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiB, gbcReg );

    this.fldRegAsciiC = GUIFactory.createTextField( 2 );
    this.fldRegAsciiC.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiC, gbcReg );

    this.fldMemBC = GUIFactory.createTextField();
    this.fldMemBC.setEditable( false );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 1.0;
    gbcReg.gridx++;
    panelReg.add( this.fldMemBC, gbcReg );


    // Register BC2
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( GUIFactory.createLabel( "BC\':" ), gbcReg );

    this.fldRegBC2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegBC2, gbcReg );


    // Register DE
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "DE:" ), gbcReg );

    this.fldRegDE = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegDE, gbcReg );

    this.fldRegAsciiD = GUIFactory.createTextField( 2 );
    this.fldRegAsciiD.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiD, gbcReg );

    this.fldRegAsciiE = GUIFactory.createTextField( 2 );
    this.fldRegAsciiE.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiE, gbcReg );

    this.fldMemDE = GUIFactory.createTextField();
    this.fldMemDE.setEditable( false );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 1.0;
    gbcReg.gridx++;
    panelReg.add( this.fldMemDE, gbcReg );


    // Register DE2
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( GUIFactory.createLabel( "DE\':" ), gbcReg );

    this.fldRegDE2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegDE2, gbcReg );


    // Register HL
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "HL:" ), gbcReg );

    this.fldRegHL = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegHL, gbcReg );

    this.fldRegAsciiH = GUIFactory.createTextField( 2 );
    this.fldRegAsciiH.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiH, gbcReg );

    this.fldRegAsciiL = GUIFactory.createTextField( 2 );
    this.fldRegAsciiL.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiL, gbcReg );

    this.fldMemHL = GUIFactory.createTextField();
    this.fldMemHL.setEditable( false );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 1.0;
    gbcReg.gridx++;
    panelReg.add( this.fldMemHL, gbcReg );


    // Register HL2
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( GUIFactory.createLabel( "HL\':" ), gbcReg );

    this.fldRegHL2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegHL2, gbcReg );


    // Register IX
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "IX:" ), gbcReg );

    this.fldRegIX = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegIX, gbcReg );

    this.fldRegAsciiIXH = GUIFactory.createTextField( 2 );
    this.fldRegAsciiIXH.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIXH, gbcReg );

    this.fldRegAsciiIXL = GUIFactory.createTextField( 2 );
    this.fldRegAsciiIXL.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIXL, gbcReg );

    this.fldMemIX = GUIFactory.createTextField();
    this.fldMemIX.setEditable( false );
    gbcReg.fill      = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx   = 1.0;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx++;
    panelReg.add( this.fldMemIX, gbcReg );


    // Register IY
    gbcReg.fill      = GridBagConstraints.NONE;
    gbcReg.weightx   = 0.0;
    gbcReg.gridwidth = 1;
    gbcReg.gridx     = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "IY:" ), gbcReg );

    this.fldRegIY = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegIY, gbcReg );

    this.fldRegAsciiIYH = GUIFactory.createTextField( 2 );
    this.fldRegAsciiIYH.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIYH, gbcReg );

    this.fldRegAsciiIYL = GUIFactory.createTextField( 2 );
    this.fldRegAsciiIYL.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIYL, gbcReg );

    this.fldMemIY = GUIFactory.createTextField();
    this.fldMemIY.setEditable( false );
    gbcReg.fill      = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx   = 1.0;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx++;
    panelReg.add( this.fldMemIY, gbcReg );


    // Register SP
    gbcReg.fill      = GridBagConstraints.NONE;
    gbcReg.weightx   = 0.0;
    gbcReg.gridwidth = 1;
    gbcReg.gridx     = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "SP:" ), gbcReg );

    this.fldRegSP = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegSP, gbcReg );

    this.fldMemSP = GUIFactory.createTextField();
    this.fldMemSP.setEditable( false );
    gbcReg.fill      = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx   = 1.0;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx += 3;
    panelReg.add( this.fldMemSP, gbcReg );


    // Register PC
    gbcReg.fill      = GridBagConstraints.NONE;
    gbcReg.weightx   = 0.0;
    gbcReg.gridwidth = 1;
    gbcReg.gridx     = 0;
    gbcReg.gridy++;
    panelReg.add( GUIFactory.createLabel( "PC:" ), gbcReg );

    this.fldRegPC = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegPC, gbcReg );


    // T-States
    JPanel panelTStates = GUIFactory.createPanel( new GridBagLayout() );
    gbcReg.anchor    = GridBagConstraints.WEST;
    gbcReg.fill      = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx   = 1.0;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx     = 4;
    panelReg.add( panelTStates, gbcReg );

    GridBagConstraints gbcTStates = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    panelTStates.add( GUIFactory.createLabel( "Taktzyklen:" ), gbcTStates );

    this.fldTStates = GUIFactory.createTextField();
    this.fldTStates.setEditable( false );
    gbcTStates.fill        = GridBagConstraints.HORIZONTAL;
    gbcTStates.weightx     = 1.0;
    gbcTStates.insets.left = 5;
    gbcTStates.gridx++;
    panelTStates.add( this.fldTStates, gbcTStates );

    this.btnResetTStates = GUIFactory.createButtonReset();
    this.btnResetTStates.addActionListener( this );
    gbcTStates.fill      = GridBagConstraints.NONE;
    gbcTStates.weightx   = 0.0;
    gbcTStates.gridx++;
    panelTStates.add( this.btnResetTStates, gbcTStates );


    // Programmcode-Anzeige
    this.fldMemPC = GUIFactory.createCodeArea( DEFAULT_FLD_MEM_PC_ROWS, 38 );
    this.fldMemPC.setEditable( false );
    this.fldMemPC.setPreferredSize( new Dimension( 1, 1 ) );
    this.fldMemPC.addMouseListener( this );
    this.spMemPC     = GUIFactory.createScrollPane( this.fldMemPC );
    gbcReg.anchor    = GridBagConstraints.WEST;
    gbcReg.fill      = GridBagConstraints.BOTH;
    gbcReg.weightx   = 1.0;
    gbcReg.weighty   = 1.0;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx     = 1;
    gbcReg.gridy++;
    panelReg.add( this.spMemPC, gbcReg );


    // Halte-/Log-Punkte
    JComponent cBreakPC = createBreakpointFields( BP_PC_IDX );
    cBreakPC.setBorder( GUIFactory.createTitledBorder( "Programmadresse" ) );

    JComponent cBreakMem = createBreakpointFields( BP_MEMORY_IDX );
    cBreakMem.setBorder( GUIFactory.createTitledBorder( "Speicher" ) );

    JComponent cBreakIn = createBreakpointFields( BP_INPUT_IDX );
    cBreakIn.setBorder( GUIFactory.createTitledBorder( "Eingabetor" ) );

    JComponent cBreakOut = createBreakpointFields( BP_OUTPUT_IDX );
    cBreakOut.setBorder( GUIFactory.createTitledBorder( "Ausgabetor" ) );

    JComponent cBreakInt = createBreakpointFields( BP_INTERRUPT_IDX );
    cBreakInt.setBorder(
		GUIFactory.createTitledBorder( "Interrupt-Quelle" ) );

    JSplitPane splitBpIO = GUIFactory.createSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					true,
					cBreakIn,
					cBreakOut );
    splitBpIO.setResizeWeight( 0.5 );

    JSplitPane splitBpRight = GUIFactory.createSplitPane(
					JSplitPane.HORIZONTAL_SPLIT,
					true,
					cBreakMem,
					splitBpIO );
    splitBpRight.setResizeWeight( 0.5 );

    JSplitPane splitBpLeft = GUIFactory.createSplitPane(
					JSplitPane.HORIZONTAL_SPLIT,
					true,
					cBreakPC,
					splitBpRight );
    splitBpLeft.setResizeWeight( 0.33 );

    this.splitBpDown = GUIFactory.createSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					true,
					splitBpLeft,
					cBreakInt );
    this.splitBpDown.setResizeWeight( 0.8 );
    this.splitBpDown.setBorder(
	GUIFactory.createTitledBorder( "Halte-/Log-Punkte auf..." ) );
    gbcCPU.fill       = GridBagConstraints.BOTH;
    gbcCPU.weightx    = 1.0;
    gbcCPU.gridheight = 2;
    gbcCPU.gridy      = 0;
    gbcCPU.gridx      = 2;
    this.panelCPU.add( this.splitBpDown, gbcCPU );


    // Tab Log-Meldungen
    this.listModelLog = new DefaultListModel<>();
    this.listLog      = GUIFactory.createList( this.listModelLog );
    this.listLog.setDragEnabled( false );
    this.listLog.setLayoutOrientation( JList.VERTICAL );
    this.listLog.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.listLog.addListSelectionListener( this );
    this.listLog.addKeyListener( this );
    this.listLog.addMouseListener( this );

    JScrollPane spLog = GUIFactory.createScrollPane( this.listLog );
    spLog.addMouseListener( this );

    this.tabbedPane.addTab( "Log-Meldungen", spLog );


    // Tab Variablen
    this.tableModelVar = new VarTableModel();
    this.tableVar      = GUIFactory.createTable( this.tableModelVar );
    this.tableVar.setAutoCreateRowSorter( true );
    this.tableVar.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.tableVar.setColumnSelectionAllowed( false );
    this.tableVar.setRowSelectionAllowed( true );
    this.tableVar.setDragEnabled( false );
    this.tableVar.setFillsViewportHeight( true );
    this.tableVar.setPreferredScrollableViewportSize( new Dimension( 1, 1 ) );
    this.tableVar.setShowGrid( false );
    this.tableVar.setSelectionMode(
		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.selectionModelVar = this.tableVar.getSelectionModel();
    if( this.selectionModelVar != null ) {
      this.selectionModelVar.addListSelectionListener( this );
    }
    EmuUtil.setTableColWidths( this.tableVar, 120, 90, 90, 200, 200 );
    this.tableVar.addKeyListener( this );
    this.tableVar.addMouseListener( this );
    this.tabbedPane.addTab(
		"Variablen",
		GUIFactory.createScrollPane( this.tableVar ) );


    // Tab Interrupt-Quellen
    JPanel panelIntSrc = GUIFactory.createPanel( new BorderLayout( 5, 5 ) );
    this.tabbedPane.addTab( "Interrupt-Quellen", panelIntSrc );

    this.listIntSrc = GUIFactory.createList();
    this.listIntSrc.setDragEnabled( false );
    this.listIntSrc.setLayoutOrientation( JList.VERTICAL );
    this.listIntSrc.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.listIntSrc.setVisibleRowCount( 4 );
    if( this.interruptSources != null ) {
      this.listIntSrc.setListData( this.interruptSources );
    }
    this.listIntSrc.addListSelectionListener( this );
    panelIntSrc.add(
		GUIFactory.createScrollPane( this.listIntSrc ),
		BorderLayout.NORTH );

    this.fldIntSrc = GUIFactory.createEditorPane();
    this.fldIntSrc.setEditable( false );
    panelIntSrc.add(
		GUIFactory.createScrollPane( this.fldIntSrc ),
		BorderLayout.CENTER );


    // Tab Sonstiges
    this.fldEtc = new JEditorPane()
		{
		  @Override
		  public Dimension getPreferredScrollableViewportSize()
		  {
		    return new Dimension( 1, 1 );
		  }
		};
    GUIFactory.initFont( this.fldEtc );
    this.fldEtc.setEditable( false );
    this.tabbedPane.addTab(
		"Sonstiges",
		GUIFactory.createScrollPane( this.fldEtc ) );


    // Statuszeile
    JPanel panelStatus = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    add( panelStatus, BorderLayout.SOUTH );

    this.labelStatus = GUIFactory.createLabel( "Bereit" );
    panelStatus.add( this.labelStatus );


    // Listener
    this.tabbedPane.addChangeListener( this );


    // sonstiges
    this.timerForClear = new javax.swing.Timer( 300, this );
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setScreenCentered();
    }
    this.fldMemPC.setRows( 0 );
    this.fldMemPC.setColumns( 0 );
    this.cpu.addStatusListener( this );
    updDebuggerInternal( null, null );
    updBreakpointsInCPU();
    addComponentListener( this );
    fireUpdBreakpointActionsEnabled();
    fireUpdLogActionsEnabled();
    fireUpdVarActionsEnabled();
  }


  public void appendLogEntry( Z80InterruptSource iSource )
  {
    if( iSource != null ) {
      fireAppendLogEntry( "--- Interrupt: " + iSource.toString() + " ---" );
    }
    StringWriter stringWriter = new StringWriter( 128 );
    PrintWriter  printWriter  = new PrintWriter( stringWriter );
    printWriter.write( "T:" );
    printWriter.write( String.valueOf( this.cpu.getProcessedTStates() ) );
    printWriter.write( "   " );
    this.cpu.writeDebugStatusEntry( printWriter );
    printWriter.close();
    fireAppendLogEntry( stringWriter.toString() );
  }


  public void openBreakpointAdd( String name, int addr, int size )
  {
    if( size > 0 ) {
      if( this.bpDlg == null ) {
	int begAddr = -1;
	int endAddr = -1;
	if( (addr >= 0) && (size > 0) ) {
	  begAddr = addr;
	  endAddr = addr + size - 1;
	}
	this.bpDlg = new MemoryBreakpointDlg(
					this,
					null,
					name,
					begAddr,
					endAddr );
	this.bpDlg.setVisible( true );
	AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
	if( bp != null ) {
	  addBreakpoint( BP_MEMORY_IDX, bp );
	}
	this.bpDlg = null;
      }
    } else {
      doBpPCAdd( name, addr );
    }
  }


  public void openVarAdd( String name, int addr, int size )
  {
    VarData varData = VarDataDlg.showNewVarDlg( this, name, addr, size );
    if( varData != null ) {
      final int row = this.tableModelVar.addRow( varData );
      if( row >= 0 ) {
	String varName = varData.getName();
	if( this.tableModelVar.getValuesEnabled() ) {
	  varData.update( this.memory );
	  this.tableModelVar.fireTableRowsUpdated( row, row );
	}
	int viewRow = this.tableVar.convertRowIndexToView( row );
	if( viewRow >= 0 ) {
	  EmuUtil.fireSelectRow( this.tableVar, viewRow );
	}
	fireUpdVarActionsEnabled();
      }
    }
  }


  public ImportableBreakpoint getMemoryBreakpointByName( String name )
  {
    return this.bpModels[ BP_MEMORY_IDX ].getByName( name );
  }


  public ImportableBreakpoint getPCBreakpointByName( String name )
  {
    return this.bpModels[ BP_PC_IDX ].getByName( name );
  }


  public VarData getVarByName( String name )
  {
    VarData rv    = null;
    int     nVars = this.tableModelVar.getRowCount();
    for( int i = 0; i < nVars; i++ ) {
      VarData tmpVar = this.tableModelVar.getRow( i );
      if( tmpVar != null ) {
	String s = tmpVar.getName();
	if( s != null ) {
	  if( s.equalsIgnoreCase( name ) ) {
	    rv = tmpVar;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public Z80CPU getZ80CPU()
  {
    return this.cpu;
  }


  public Z80Memory getZ80Memory()
  {
    return this.memory;
  }


  public boolean importLabels(
			Component          owner,
			LabelImportOptions options )
  {
    boolean state      = false;
    Reader  reader     = null;
    String  sourceText = null;
    try {
      switch( options.getLabelSource() ) {
	case CLIPBOARD:
	  String text = EmuUtil.getClipboardText( this );
	  if( text != null ) {
	    reader     = new StringReader( text );
	    sourceText = " der Zwischenablage";
	  }
	  break;
	case FILE:
	  File file = options.getFile();
	  if( file == null ) {
	    throw new IOException( "Datei nicht angegeben" );
	  }
	  reader     = new FileReader( file );
	  sourceText = " der Datei";
	  break;
      }
      if( reader != null ) {
	try {
	  state = importLabels(
			ToolUtil.readLabels( reader ),
			options.getCaseSensitive(),
			options.getUpdateBreakpointsOnly(),
			options.getRemoveObsoleteLabels() );
	}
	finally {
	  EmuUtil.closeSilently( reader );
	}
      }
      if( state ) {
	this.labelImportOptions = options;
	this.mnuFileBpsReImport.setEnabled( true );
	this.mnuFileBpsRemoveImported.setEnabled( true );
      } else {
	if( reader != null ) {
	  BaseDlg.showErrorDlg(
		this,
		"Der Inhalt" + sourceText + " konnte nicht als Liste\n"
		  + "mit Halte-/Log-Punkten interpretiert werden." );
	}
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return state;
  }


  public void selectVar( VarData var )
  {
    if( var != null ) {
      int modelRow = this.tableModelVar.indexOf( var );
      if( modelRow >= 0 ) {
	int viewRow = this.tableVar.convertRowIndexToView( modelRow );
	if( viewRow >= 0 ) {
	  final JTable table = this.tableVar;
	  EmuUtil.fireSelectRow( table, viewRow );
	}
      }
    }
  }


  public void setLabels(
		jkcemu.tools.Label[] labels,
		boolean              caseSensitive,
		boolean              updBreakpointsOnly )
  {
    importLabels( labels, caseSensitive, updBreakpointsOnly, true );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.tabbedPane ) {
      updBreakpointActionsEnabled();
      updLogActionsEnabled();
      updVarActionsEnabled();
    }
  }


	/* --- ComponentListener --- */

  @Override
  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    if( (e.getComponent() == this) && this.cpu.isPause() ) {
      updFieldsPC();
    }
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    // leer
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    // leer
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    Component c = e.getComponent();
    if( c != null )
      fieldValueChanged( c );
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    Object src = e.getSource();
    if( src == this.listIntSrc ) {
      updIntSrcInfoField();
    } else if( src == this.listLog ) {
      updLogActionsEnabled();
    } else if( src == this.selectionModelVar ) {
      updVarActionsEnabled();
    } else {
      BreakpointList list = null;
      for( int i = 0; i < BP_GROUP_CNT; i++ ) {
	if( (src == this.bpLists[ i ])
	    || (src == this.bpScrollPanes[ i ]) )
	{
	  list = this.bpLists[ i ];
	  break;
	}
      }
      if( list != null ) {
	if( list.getSelectedIndex() >= 0 ) {
	  // andere Listen deselektiren
	  for( int i = 0; i < this.bpLists.length; i++ ) {
	    if( list != this.bpLists[ i ] ) {
	      this.bpLists[ i ].clearSelection();
	    }
	  }
	}
	fireUpdBreakpointActionsEnabled();
      }
    }
  }


	/* --- PopupMenusOwner --- */

  @Override
  public JPopupMenu[] getPopupMenus()
  {
    return new JPopupMenu[] {
			this.popupBp,
			this.popupLog,
			this.popupVar,
			this.popupWalk,
			this.popupMemPC };
  }


	/* --- Z80StatusListener --- */

  /*
   * Diese Methode wird aufgerufen, wenn sich der Zustand der CPU-Emulation
   * aendert, z.B. wenn die Ausfuehrung des Programmcodes angehalten wurde
   * und die Anzeige somit aktualisiert werden soll.
   * Da die Methode von einem anderen Thread aufgerufen wird,
   * wird ueber invokeLater(...) und dem Runable-Interface
   * die Aktualisierung der Anzeige in den Swing-Thread verlagert.
   */
  @Override
  public void z80StatusChanged(
			Z80Breakpoint      breakpoint,
			Z80InterruptSource iSource )
  {
    fireUpdDebugger( breakpoint, iSource );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props )
  {
    if( props != null ) {
      setMaxLogCnt( props.getProperty(
			getSettingsPrefix() + PROP_MAX_LOG_COUNT ) );
    }
    return super.applySettings( props );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {

      /*
       * Wenn Aktion nicht von einem Textfeld ausging,
       * dann Textfeld mit Focus validieren
       */
      Component c = getFocusOwner();
      if( c != null ) {
	if( !(c instanceof JTextComponent) ) {
	  fieldValueChanged( c );
	}
      }

      // Aktion auswerten
      Object src = e.getSource();
      if( src == this.mnuFileBpsVarsLoad ) {
	rv = true;
	doFileBpsVarsLoad();
      }
      else if( src == this.mnuFileBpsVarsSave ) {
	rv = true;
	doFileBpsVarsSave();
      }
      else if( src == this.mnuFileBpsImport ) {
	rv = true;
	doFileBpsImport( true );
      }
      else if( src == this.mnuFileBpsReImport ) {
	rv = true;
	doFileBpsImport( false );
      }
      else if( src == this.mnuFileBpsRemoveImported ) {
	rv = true;
	doFileBpsRemoveImported();
      }
      else if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( (src == this.mnuExecRun)
	       || (src == this.btnRun) )
      {
	rv = true;
	doExecRun();
      }
      else if( src == this.btnWalk )
      {
	rv = true;
	this.popupWalk.show( this.btnWalk, 0, this.btnWalk.getHeight() );
      }
      else if( (src == this.mnuExecWalk30)
	       || (src == this.popupWalk30) )
      {
	rv = true;
	doExecWalk( 30 );
      }
      else if( (src == this.mnuExecWalk100)
	       || (src == this.popupWalk100) )
      {
	rv = true;
	doExecWalk( 100 );
      }
      else if( (src == this.mnuExecWalk300)
	       || (src == this.popupWalk300) )
      {
	rv = true;
	doExecWalk( 300 );
      }
      else if( (src == this.mnuExecWalk500)
	       || (src == this.popupWalk500) )
      {
	rv = true;
	doExecWalk( 500 );
      }
      else if( (src == this.mnuExecStop)
	       || (src == this.btnStop) )
      {
	rv = true;
	doExecStop();
      }
      else if( (src == this.mnuExecStepOver)
	       || (src == this.btnStepOver) )
      {
	rv = true;
	doExecStepOver();
      }
      else if( (src == this.mnuExecStepInto)
	       || (src == this.btnStepInto) )
      {
	rv = true;
	doExecStepInto();
      }
      else if( (src == this.mnuExecStepToRET)
	       || (src == this.btnStepToRET) )
      {
	rv = true;
	doExecStepToRET();
      }
      else if( src == this.mnuExecTracer ) {
	rv = true;
	doExecTracer();
      }
      else if( src == this.mnuBpInputAdd ) {
	rv = true;
	doBpInputAdd();
      }
      else if( src == this.mnuBpInterruptAdd ) {
	rv = true;
	doBpInterruptAdd();
      }
      else if( src == this.mnuBpMemoryAdd ) {
	rv = true;
	doBpMemoryAdd( null, -1, -1 );
      }
      else if( src == this.mnuBpOutputAdd ) {
	rv = true;
	doBpOutputAdd();
      }
      else if( src == this.mnuBpPCAdd ) {
	rv = true;
	doBpPCAdd( null, -1 );
      }
      else if( src == this.mnuBpEdit ) {
	rv = true;
	doBpEdit( -1 );
      }
      else if( src == this.mnuBpRemove ) {
	rv = true;
	doBpRemove();
      }
      else if( src == this.mnuBpRemoveAll ) {
	rv = true;
	doBpRemoveAll();
      }
      else if( src == this.mnuBpEnableStop ) {
	rv = true;
	doBpSetStopEnabled( true );
      }
      else if( src == this.mnuBpDisableStop ) {
	rv = true;
	doBpSetStopEnabled( false );
      }
      else if( src == this.mnuBpEnableStopAll ) {
	rv = true;
	doBpSetAllStopEnabled( true );
      }
      else if( src == this.mnuBpDisableStopAll ) {
	rv = true;
	doBpSetAllStopEnabled( false );
      }
      else if( src == this.mnuBpEnableLog ) {
	rv = true;
	doBpSetLogEnabled( true );
      }
      else if( src == this.mnuBpDisableLog ) {
	rv = true;
	doBpSetLogEnabled( false );
      }
      else if( src == this.mnuBpEnableLogAll ) {
	rv = true;
	doBpSetAllLogEnabled( true );
      }
      else if( src == this.mnuBpDisableLogAll ) {
	rv = true;
	doBpSetAllLogEnabled( false );
      }
      else if( (src == this.mnuVarAdd)
	       || (src == this.popupVarAdd) )
      {
	rv = true;
	openVarAdd( null, -1, -1 );
      }
      else if( (src == this.mnuVarEdit)
	       || (src == this.popupVarEdit)
	       || (src == this.tableVar) )
      {
	rv = true;
	doVarEdit();
      }
      else if( (src == this.mnuVarBpAdd)
	       || (src == this.popupVarBpAdd) )
      {
	rv = true;
	doVarBpAdd();
      }
      else if( (src == this.mnuVarRemove)
	       || (src == this.popupVarRemove) )
      {
	rv = true;
	doVarRemove();
      }
      else if( (src == this.mnuVarRemoveAll)
	       || (src == this.popupVarRemoveAll) )
      {
	rv = true;
	doVarRemoveAll();
      }
      else if( (src == this.mnuLogCopy)
	       || (src == this.popupLogCopy) )
      {
	rv = true;
	doLogCopy();
      }
      else if( (src == this.mnuLogSelectAll)
	       || (src == this.popupLogSelectAll) ) {
	rv = true;
	doLogSelectAll();
      }
      else if( (src == this.mnuLogRemove)
	       || (src == this.popupLogRemove) )
      {
	rv = true;
	doLogRemove();
      }
      else if( (src == this.mnuLogRemoveAll)
	       || (src == this.popupLogRemoveAll) )
      {
	rv = true;
	doLogRemoveAll();
      }
      else if( src == this.mnuLogMaxLogCnt ) {
	rv = true;
	doLogMaxLogCnt();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
      }
      else if( src == this.popupBpAdd ) {
	rv = true;
	doBpAdd( this.popupBpGroupIdx );
      }
      else if( src == this.popupBpEdit ) {
	rv = true;
	doBpEdit( this.popupBpGroupIdx );
      }
      else if( src == this.popupBpRemove ) {
	rv = true;
	doBpRemove( this.popupBpGroupIdx );
      }
      else if( src == this.popupBpRemoveAll ) {
	rv = true;
	doBpRemoveAll( this.popupBpGroupIdx );
      }
      else if( src == this.popupBpEnableStop ) {
	rv = true;
	doBpSetStopEnabled( this.popupBpGroupIdx, true );
      }
      else if( src == this.popupBpDisableStop ) {
	rv = true;
	doBpSetStopEnabled( this.popupBpGroupIdx, false );
      }
      else if( src == this.popupBpEnableStopAll ) {
	rv = true;
	doBpSetAllStopEnabled( this.popupBpGroupIdx, true );
      }
      else if( src == this.popupBpDisableStopAll ) {
	rv = true;
	doBpSetAllStopEnabled( this.popupBpGroupIdx, false );
      }
      else if( src == this.popupBpEnableLog ) {
	rv = true;
	doBpSetLogEnabled( this.popupBpGroupIdx, true );
      }
      else if( src == this.popupBpDisableLog ) {
	rv = true;
	doBpSetLogEnabled( this.popupBpGroupIdx, false );
      }
      else if( src == this.popupBpEnableLogAll ) {
	rv = true;
	doBpSetAllLogEnabled( this.popupBpGroupIdx, true );
      }
      else if( src == this.popupBpDisableLogAll ) {
	rv = true;
	doBpSetAllLogEnabled( this.popupBpGroupIdx, false );
      }
      else if( src == this.btnFlagSign ) {
	rv = true;
	if( this.btnFlagSign.isEnabled() ) {
	  this.cpu.setFlagSign( this.btnFlagSign.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagZero ) {
	rv = true;
	if( this.btnFlagZero.isEnabled() ) {
	  this.cpu.setFlagZero( this.btnFlagZero.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagHalf ) {
	rv = true;
	if( this.btnFlagHalf.isEnabled() ) {
	  this.cpu.setFlagHalf( this.btnFlagHalf.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagPV ) {
	rv = true;
	if( this.btnFlagPV.isEnabled() ) {
	  this.cpu.setFlagPV( this.btnFlagPV.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagN ) {
	rv = true;
	if( this.btnFlagN.isEnabled() ) {
	  this.cpu.setFlagN( this.btnFlagN.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagCarry ) {
	rv = true;
	if( this.btnFlagCarry.isEnabled() ) {
	  this.cpu.setFlagCarry( this.btnFlagCarry.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnIFF1 ) {
	rv = true;
	if( this.btnIFF1.isEnabled() ) {
	  this.cpu.setIFF1( this.btnIFF1.isSelected() );
	}
      }
      else if( src == this.btnIFF2 ) {
	rv = true;
	if( this.btnIFF2.isEnabled() ) {
	  this.cpu.setIFF2( this.btnIFF2.isSelected() );
	}
      }
      else if( src == this.btnResetTStates ) {
	rv = true;
	this.cpu.resetProcessTStates();
	updFieldsTStates();
      }
      else if( src == this.popupMemPCCopy ) {
	rv = true;
	this.fldMemPC.copy();
      }
      else if( src == this.popupMemPCBreak ) {
	rv = true;
	if( this.memPCClickAddr >= 0 ) {
	  doBpPCAdd( null, this.memPCClickAddr );
	}
      }
      else if( src == this.timerForClear ) {
	rv = true;
	clear();
      }
      else if( src == this.walkTimer ) {
	rv = true;
	doWalkTimer();
      }
      else if( (src == this.spinnerIntMode)
	       || (src == this.fldRegAF)
	       || (src == this.fldRegAF2)
	       || (src == this.fldRegBC)
	       || (src == this.fldRegBC2)
	       || (src == this.fldRegDE)
	       || (src == this.fldRegDE2)
	       || (src == this.fldRegHL)
	       || (src == this.fldRegHL2)
	       || (src == this.fldRegIX)
	       || (src == this.fldRegIY)
	       || (src == this.fldRegSP)
	       || (src == this.fldRegPC)
	       || (src == this.fldRegI) )
      {
	rv = true;
	fieldValueChanged( src );
      }
      else if( src instanceof BreakpointList ) {
	if( src == this.bpLists[ BP_PC_IDX ] ) {
	  rv = true;
	  editBreakpoint( BP_PC_IDX );
	} else if( src == this.bpLists[ BP_MEMORY_IDX ] ) {
	  rv = true;
	  editBreakpoint( BP_MEMORY_IDX );
	} else if( src == this.bpLists[ BP_INPUT_IDX ] ) {
	  rv = true;
	  editBreakpoint( BP_INPUT_IDX );
	} else if( src == this.bpLists[ BP_OUTPUT_IDX ] ) {
	  rv = true;
	  editBreakpoint( BP_OUTPUT_IDX );
	} else if( src == this.bpLists[ BP_INTERRUPT_IDX ] ) {
	  rv = true;
	  editBreakpoint( BP_INTERRUPT_IDX );
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    closeTrace();
    this.cpu.setDebugEnabled( false );
    return super.doClose();
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    boolean done = false;
    if( e.getKeyCode() == KeyEvent.VK_DELETE ) {
      switch( this.tabbedPane.getSelectedIndex() ) {
	case TAB_IDX_CPU:
	  doBpRemove();
	  done = true;
	  break;
	case TAB_IDX_LOG:
	  doLogRemove();
	  done = true;
	  break;
	case TAB_IDX_VAR:
	  doVarRemove();
	  done = true;
	  break;
      }
    }
    if( done ) {
      e.consume();
    } else {
      super.keyPressed( e );
    }
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    super.putSettingsTo( props );
    if( props != null ) {
      EmuUtil.setProperty(
		props,
		getSettingsPrefix() + PROP_MAX_LOG_COUNT,
		this.maxLogCnt );
    }
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    updIntSrcFields();
    updEtcField();
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state ) {
      this.cpu.setDebugEnabled( state );  // Disable erfolgt ueber doClose()
      fireUpdDebugger( null, null );
    }
    super.setVisible( state );
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean rv = false;
    if( e != null ) {
      Component c = e.getComponent();
      if( c != null ) {
	if( c == this.fldMemPC ) {
	  this.memPCClickAddr = ToolUtil.getReassAddr(
						this.fldMemPC,
						e.getPoint(),
						null );
	  this.popupMemPCCopy.setEnabled(
		this.fldMemPC.getSelectionStart()
			!= this.fldMemPC.getSelectionEnd() );
	  this.popupMemPCBreak.setEnabled( this.memPCClickAddr >= 0 );
	  this.popupMemPC.show( c, e.getX(), e.getY() );
	  rv = true;
	} else {
	  int tabIdx = this.tabbedPane.getSelectedIndex();
	  if( tabIdx == TAB_IDX_CPU ) {
	    int idx = -1;
	    for( int i = 0; i < BP_GROUP_CNT; i++ ) {
	      if( (c == this.bpLists[ i ])
		  || (c == this.bpScrollPanes[ i ]) )
	      {
		idx = i;
		break;
	      }
	    }
	    if( (idx >= 0) && (idx < BP_GROUP_CNT) ) {
	      BreakpointListModel model           = this.bpModels[ idx ];
	      boolean             anyStopEnabled  = false;
	      boolean             anyStopDisabled = false;
	      boolean             anyLogEnabled   = false;
	      boolean             anyLogDisabled  = false;
	      if( model != null ) {
		for( AbstractBreakpoint bp : model ) {
		  if( bp.isStopEnabled() ) {
		    anyStopEnabled = true;
		  } else {
		    anyStopDisabled = true;
		  }
		  if( bp.isLogEnabled() ) {
		    anyLogEnabled = true;
		  } else {
		    anyLogDisabled = true;
		  }
		  if( anyStopEnabled && anyStopDisabled
		      && anyLogEnabled && anyLogDisabled )
		  {
		    break;
		  }
		}
		this.popupBpRemoveAll.setEnabled( !model.isEmpty() );
	      } else {
		this.popupBpRemoveAll.setEnabled( false );
	      }
	      this.popupBpEnableStopAll.setEnabled( anyStopDisabled );
	      this.popupBpDisableStopAll.setEnabled( anyStopEnabled );
	      this.popupBpEnableLogAll.setEnabled( anyLogDisabled );
	      this.popupBpDisableLogAll.setEnabled( anyLogEnabled );

	      int     nSelected       = 0;
	      boolean selStopEnabled  = false;
	      boolean selStopDisabled = false;
	      boolean selLogEnabled   = false;
	      boolean selLogDisabled  = false;
	      if( model != null ) {
		int[] rows  = this.bpLists[ idx ].getSelectedIndices();
		if( rows != null ) {
		  for( int i = 0; i < rows.length; i++ ) {
		    int row = rows[ i ];
		    if( (row >= 0) && (row < model.getSize()) ) {
		      nSelected++;
		      if( model.getElementAt( row ).isStopEnabled() ) {
			selStopEnabled = true;
		      } else {
			selStopDisabled = true;
		      }
		      if( model.getElementAt( row ).isLogEnabled() ) {
			selLogEnabled = true;
		      } else {
			selLogDisabled = true;
		      }
		    }
		    if( (nSelected > 1)
			&& selStopEnabled && selStopDisabled
			&& selLogEnabled && selLogDisabled )
		    {
		      break;
		    }
		  }
		}
	      }
	      this.popupBpEdit.setEnabled( nSelected == 1 );
	      this.popupBpEnableStop.setEnabled( selStopDisabled );
	      this.popupBpDisableStop.setEnabled( selStopEnabled );
	      this.popupBpEnableLog.setEnabled( selLogDisabled );
	      this.popupBpDisableLog.setEnabled( selLogEnabled );
	      this.popupBpRemove.setEnabled( nSelected > 0 );
	      this.popupBpGroupIdx = idx;
	      this.popupBp.show( c, e.getX(), e.getY() );
	      rv = true;
	    }
	  } else if( tabIdx == TAB_IDX_VAR ) {
	    boolean hasEntries = !this.tableModelVar.isEmpty();
	    int     nSelected  = this.tableVar.getSelectedRowCount();
	    this.popupVarEdit.setEnabled( nSelected == 1 );
	    this.popupVarBpAdd.setEnabled( nSelected == 1 );
	    this.popupVarRemove.setEnabled( nSelected > 0 );
	    this.popupVarRemoveAll.setEnabled( hasEntries );
	    this.popupVar.show( c, e.getX(), e.getY() );
	    rv = true;
	  } else if( tabIdx == TAB_IDX_LOG ) {
	    boolean hasEntries = !this.listModelLog.isEmpty();
	    boolean selected   = (this.listLog.getSelectedIndex() >= 0);
	    this.popupLogCopy.setEnabled( selected );
	    this.popupLogSelectAll.setEnabled( hasEntries );
	    this.popupLogRemove.setEnabled( selected );
	    this.popupLogRemoveAll.setEnabled( hasEntries );
	    this.popupLog.show( c, e.getX(), e.getY() );
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      int h = this.splitBpDown.getHeight();
      if( h > 0 ) {
	this.splitBpDown.setDividerLocation( h * 4 / 5 );
      }
    }
  }


	/* --- Aktionen --- */

  private void doBpPCAdd( String name, int addr )
  {
    if( this.bpDlg == null ) {
      PCBreakpoint prototypeBP = null;
      this.bpDlg = new PCBreakpointDlg( this, prototypeBP, name, addr );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_PC_IDX, bp );
      }
      this.bpDlg = null;
    }
  }


  private void doBpAdd( int bpGroupIdx )
  {
    switch( bpGroupIdx ) {
      case BP_PC_IDX:
	doBpPCAdd( null, -1 );
	break;
      case BP_MEMORY_IDX:
	doBpMemoryAdd( null, -1, -1 );
	break;
      case BP_INPUT_IDX:
	doBpInputAdd();
	break;
      case BP_OUTPUT_IDX:
	doBpOutputAdd();
	break;
      case BP_INTERRUPT_IDX:
	doBpInterruptAdd();
	break;
    }
  }


  private void doBpEdit( int bpGroupIdx )
  {
    if( bpGroupIdx < 0 ) {
      if( this.tabbedPane.getSelectedIndex() == TAB_IDX_CPU ) {
	for( int i = 0; i < BP_GROUP_CNT; i++ ) {
	  if( this.bpModels[ i ] != null ) {
	    int[] rows = this.bpLists[ i ].getSelectedIndices();
	    if( rows != null ) {
	      if( rows.length > 0 ) {
		if( bpGroupIdx < 0 ) {
		  bpGroupIdx = i;
		} else {
		  // Breakpoints in mehrere Gruppen ausgewaehlt -> Abbruch
		  bpGroupIdx = -1;
		  break;
		}
	      }
	    }
	  }
	}
      }
    }
    if( bpGroupIdx >= 0 ) {
      editBreakpoint( bpGroupIdx );
    }
  }


  private void doBpInputAdd()
  {
    if( this.bpDlg == null ) {
      this.bpDlg = new InputBreakpointDlg( this, null );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_INPUT_IDX, bp );
      }
      this.bpDlg = null;
    }
  }


  private void doBpInterruptAdd()
  {
    if( this.interruptSources != null ) {
      if( this.bpDlg == null ) {
	this.bpDlg = new InterruptBreakpointDlg(
					this,
					null,
					this.interruptSources );
	this.bpDlg.setVisible( true );
	AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
	if( bp != null ) {
	  addBreakpoint( BP_INTERRUPT_IDX, bp );
	}
	this.bpDlg = null;
      }
    } else {
      BaseDlg.showErrorDlg(
		this,
		"Das emulierte System hat keine Interrupt-Quellen." );
    }
  }


  private void doBpMemoryAdd( String name, int begAddr, int endAddr )
  {
    if( this.bpDlg == null ) {
      this.tabbedPane.setSelectedComponent( this.panelCPU );
      this.bpDlg = new MemoryBreakpointDlg(
					this,
					null,
					name,
					begAddr,
					endAddr );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_MEMORY_IDX, bp );
      }
      this.bpDlg = null;
    }
  }


  private void doBpOutputAdd()
  {
    if( this.bpDlg == null ) {
      this.bpDlg = new OutputBreakpointDlg( this, null );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_OUTPUT_IDX, bp );
      }
      this.bpDlg = null;
    }
  }


  private void doBpRemove()
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doBpRemove( i );
  }


  private void doBpRemove( int bpGroupIdx )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      BreakpointListModel model = this.bpModels[ bpGroupIdx ];
      if( model != null ) {
	int[] rows = this.bpLists[ bpGroupIdx ].getSelectedIndices();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    for( int i = rows.length - 1; i >= 0; --i ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < model.getSize()) ) {
		model.remove( row );
	      }
	    }
	    updBreakpointsInCPU();
	    fireUpdBreakpointActionsEnabled();
	  }
	}
      }
    }
  }


  private void doBpRemoveAll()
  {
    if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Halte-/Log-Punkte entfernen?" ) )
    {
      removeAllBreakpoints();
    }
  }


  private void doBpRemoveAll( int bpGroupIdx )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Halte-/Log-Punkte"
			+ " dieser Gruppe entfernen?" ) )
      {
	this.bpModels[ bpGroupIdx ].clear();
	updBreakpointsInCPU();
	fireUpdBreakpointActionsEnabled();
      }
    }
  }


  private void doBpSetStopEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doBpSetStopEnabled( i, state );
  }


  private void doBpSetStopEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      BreakpointListModel model = this.bpModels[ bpGroupIdx ];
      if( model != null ) {
	int[] rows = this.bpLists[ bpGroupIdx ].getSelectedIndices();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    for( int i = 0; i < rows.length; i++ ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < model.getSize()) ) {
		AbstractBreakpoint bp = model.getElementAt( row );
		bp.setStopEnabled( state );
		model.fireItemChanged( row );
	      }
	    }
	    updBreakpointsInCPU();
	    EmuUtil.fireSelectRows( this.bpLists[ bpGroupIdx ], rows );
	    fireUpdBreakpointActionsEnabled();
	  }
	}
      }
    }
  }


  private void doBpSetAllStopEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doBpSetAllStopEnabled( i, state );
  }


  private void doBpSetAllStopEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      BreakpointListModel model = this.bpModels[ bpGroupIdx ];
      if( model != null ) {
	int n = model.getSize();
	for( int i = 0; i < n; i++ ) {
	  model.getElementAt( i ).setStopEnabled( state );
	  model.fireItemChanged( i );
	}
	updBreakpointsInCPU();
	fireUpdBreakpointActionsEnabled();
      }
    }
  }


  private void doBpSetLogEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doBpSetLogEnabled( i, state );
  }


  private void doBpSetLogEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      BreakpointListModel model = this.bpModels[ bpGroupIdx ];
      if( model != null ) {
	int[] rows = this.bpLists[ bpGroupIdx ].getSelectedIndices();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    for( int i = 0; i < rows.length; i++ ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < model.getSize()) ) {
		AbstractBreakpoint bp = model.getElementAt( row );
		bp.setLogEnabled( state );
		model.fireItemChanged( row );
	      }
	    }
	    updBreakpointsInCPU();
	    EmuUtil.fireSelectRows( this.bpLists[ bpGroupIdx ], rows );
	    fireUpdBreakpointActionsEnabled();
	  }
	}
      }
    }
  }


  private void doBpSetAllLogEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doBpSetAllLogEnabled( i, state );
  }


  private void doBpSetAllLogEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      BreakpointListModel model = this.bpModels[ bpGroupIdx ];
      if( model != null ) {
	int n = model.getSize();
	for( int i = 0; i < n; i++ ) {
	  model.getElementAt( i ).setLogEnabled( state );
	  model.fireItemChanged( i );
	}
	updBreakpointsInCPU();
	fireUpdBreakpointActionsEnabled();
      }
    }
  }


  private void doFileBpsVarsLoad()
  {
    File file = FileUtil.showFileOpenDlg(
			this,
			"Halte-/Log-Punkte und Variablen laden",
			this.lastBreakpointFile != null ?
				this.lastBreakpointFile
				: Main.getLastDirFile(
					Main.FILE_GROUP_DEBUG_BREAK ),
			FileUtil.getXMLFileFilter() );
    if( file != null ) {
      Boolean state = Boolean.FALSE;
      if( hasBreakpoints() || (this.tableModelVar.getRowCount() > 0) ) {
	state = BaseDlg.showSuppressableYesNoCancelDlg(
			this,
			"Sollen vor dem Laden die bereits vorhandenen"
				+ " Halte-/Log-Punkte\n"
				+ "und Variablen entfernt werden?" );
      }
      if( state != null ) {
	if( state.booleanValue() ) {
	  removeAllBreakpoints();
	  removeAllVars();
	}
	InputStream in = null;
	try {
	  in = new BufferedInputStream( new FileInputStream( file ) );

	  BreakpointVarLoader loader = new BreakpointVarLoader(
					this,
					this.interruptSources,
					this.bpModels[ BP_PC_IDX ],
					this.bpModels[ BP_MEMORY_IDX ],
					this.bpModels[ BP_INPUT_IDX ],
					this.bpModels[ BP_OUTPUT_IDX ],
					this.bpModels[ BP_INTERRUPT_IDX ],
					this.tableModelVar );
	  SAXParserFactory.newInstance().newSAXParser().parse( in, loader );
	  if( !loader.getLoaded() ) {
	    BaseDlg.showErrorDlg(
			this,
			"Datei enth\u00E4lt weder Halte-/Log-Punke"
				+ " noch Variablen." );
	  }
	}
	catch( IOException ex1 ) {
	  BaseDlg.showErrorDlg( this, ex1 );
	}
	catch( SAXException ex2 ) {
	  BaseDlg.showErrorDlg(
			this,
			"Datei kann nicht verarbeitet werden.",
			ex2 );
	}
	catch( ParserConfigurationException ex ) {
	  showErrorNoXMLSupport( ex );
	}
      }
    }
  }


  private void doFileBpsVarsSave()
  {
    boolean stateBPs  = hasBreakpoints();
    boolean stateVars = (this.tableModelVar.getRowCount() > 0);
    if( stateBPs || stateVars ) {
      File file = FileUtil.showFileSaveDlg(
			this,
			"Halte-/Log-Punkte und Variablen speichern",
			this.lastBreakpointFile != null ?
				this.lastBreakpointFile
				: Main.getLastDirFile(
					Main.FILE_GROUP_DEBUG_BREAK ),
			FileUtil.getXMLFileFilter() );
      if( file != null ) {
	OutputStream out = null;
	try {
	  try {

	    // DOM-Struktur erzeugen
	    Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();

	    Element rootElem = doc.createElement( ELEM_ROOT );
	    doc.appendChild( rootElem );

	    if( stateBPs ) {
	      Element bpListElem = doc.createElement( ELEM_BREAKPOINTS );
	      rootElem.appendChild( bpListElem );
	      for( BreakpointListModel model : this.bpModels ) {
		int n = model.getSize();
		for( int i = 0; i < n; i++ ) {
		  model.getElementAt( i ).writeTo( doc, bpListElem );
		}
	      }
	    }
	    if( stateVars ) {
	      Element varListElem = doc.createElement( ELEM_VARIABLES );
	      rootElem.appendChild( varListElem );
	      int n = this.tableModelVar.getRowCount();
	      for( int i = 0; i < n; i++ ) {
		this.tableModelVar.getRow( i ).writeTo( doc, varListElem );
	      }
	    }

	    // DOM-Struktur in XML schreiben
	    out = new BufferedOutputStream( new FileOutputStream( file ) );

	    Transformer t = TransformerFactory.newInstance().newTransformer();
	    t.setOutputProperty( "indent", "yes" );
	    t.transform( new DOMSource( doc ), new StreamResult( out ) );

	    out.close();
	    out = null;
	    this.lastBreakpointFile = file;
	    Main.setLastFile( file, Main.FILE_GROUP_DEBUG_BREAK );
	  }
	  finally {
	    EmuUtil.closeSilently( out );
	  }
	}
	catch( IOException ex ) {
	  BaseDlg.showErrorDlg( this, ex );
	}
	catch( ParserConfigurationException | TransformerException ex ) {
	  showErrorNoXMLSupport( ex );

	}
      }
    } else {
      BaseDlg.showErrorDlg(
		this,
		"Es sind keine Halte-/Log-Punkte und Variablen vorhanden,\n"
			+ "die gespeichert werden k\u00F6nnten." );
    }
  }


  private void doFileBpsImport( boolean interactive )
  {
    LabelImportOptions options = null;
    if( interactive ) {
      LabelImportDlg.showDlg( this, this.labelImportOptions );
    } else {
      if( this.labelImportOptions != null ) {
	importLabels( this, this.labelImportOptions );
      }
    }
  }


  private void doFileBpsRemoveImported()
  {
    if( BaseDlg.showSuppressableYesNoDlg(
		this,
		"M\u00F6chten Sie alle importierten Halte-/Log-Punkte"
			+ " entfernen?" ) )
    {
      boolean changed = false;
      for( int bpGroupIdx : new int[] { BP_PC_IDX, BP_MEMORY_IDX } ) {
	BreakpointListModel model = this.bpModels[ bpGroupIdx ];
	if( model != null ) {
	  int size = model.getSize();
	  if( size > 0 ) {
	    for( int i = size - 1; i >= 0; --i ) {
	      AbstractBreakpoint bp = model.getElementAt( i );
	      if( bp instanceof ImportableBreakpoint ) {
		if( ((ImportableBreakpoint) bp).getImported() ) {
		  model.remove( i );
		  changed = true;
		}
	      }
	    }
	  }
	}
      }
      if( changed ) {
	updBreakpointsInCPU();
	fireUpdBreakpointActionsEnabled();
      }
    }
  }


  private void doExecRun()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_RUN );
  }


  private void doExecWalk( int millis )
  {
    this.walkMillis = millis;
    this.labelStatus.setText( "Programm wird langsam ausgef\u00FChrt..." );
    this.cpu.fireAction( Z80CPU.Action.DEBUG_WALK );
  }


  private void doExecStop()
  {
    if( this.walkMillis > 0 ) {
      this.walkMillis = 0;
      fireUpdDebugger( null, null );
    }
    this.labelStatus.setText( "Programmausf\u00FChrung wird angehalten..." );
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STOP );
  }


  private void doExecStepOver()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_OVER );
  }


  private void doExecStepInto()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_INTO );
  }


  private void doExecStepToRET()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_TO_RET );
  }


  private void doExecTracer()
  {
    if( this.mnuExecTracer.isSelected() ) {

      /*
       * Menueintrag ersteinmal ausschalten, falls ein Fehler
       * auftritt oder die Aktion abgebrochen wird.
       */
      this.mnuExecTracer.setSelected( false );

      // voreingestellte Aktion: neue Datei anlegen
      int     action = 1;
      boolean append = false;
      File    file   = null;

      // an alte Datei anhaengen oder neue Datei anlegen?
      if( this.lastTraceFile != null ) {
	String[] options = {
			"Anh\u00E4ngen",
			"Neue Datei",
			EmuUtil.TEXT_CANCEL };
	action = BaseDlg.showOptionDlg(
		this,
		"Soll die Befehlsaufzeichnung an die alte Datei\n"
			+ this.lastTraceFile.getPath()
			+ "\nangeh\u00E4ngt werden oder m\u00F6chten Sie\n"
			+ "eine neue Datei anlegen?",
		"Entscheidung",
		options );
	if( action == 0 ) {
	  file   = this.lastTraceFile;
	  append = true;
	}
      }

      // Trace-Datei auswaehlen
      if( ((action == 0) && (file == null)) || (action == 1) ) {
	file = FileUtil.showFileSaveDlg(
			this,
			"Befehlsaufzeichnung speichern",
			Main.getLastDirFile( Main.FILE_GROUP_DEBUG_TRACE ),
			FileUtil.getTextFileFilter() );
	append = false;
      }

      // Datei zum schreiben oeffnen
      if( file != null ) {
	try {
	  this.traceWriter = new PrintWriter(
		new BufferedWriter(
			new FileWriter( file.getPath(), append ) ),
		true );

	  this.cpu.setDebugTracer( this.traceWriter );
	  this.mnuExecTracer.setSelected( true );
	  this.lastTraceFile = file;
	  Main.setLastFile( file, Main.FILE_GROUP_DEBUG_TRACE );
	}
	catch( IOException ex ) {
	  this.traceWriter = null;
	  BaseDlg.showErrorDlg(
		this,
		"Die Befehlsaufzeichnungsdatei kann nicht\n"
			+ "zum Schreiben ge\u00F6ffnet werden.\n\n"
			+ ex.getMessage() );
	}
      }
    } else {
      closeTrace();
    }
  }


  private void doLogCopy()
  {
    if( this.tabbedPane.getSelectedIndex() == TAB_IDX_LOG ) {
      int[] rows = this.listLog.getSelectedIndices();
      if( rows != null ) {
	if( rows.length > 0 ) {
	  StringBuilder buf = new StringBuilder( rows.length * 128 );
	  for( int row : rows ) {
	    String text = this.listModelLog.getElementAt( row );
	    if( text != null ) {
	      if( buf.length() > 0 ) {
		buf.append( '\n' );
	      }
	      buf.append( text );
	    }
	  }
	  EmuUtil.copyToClipboard( this, buf.toString() );
	}
      }
    }
  }


  private void doLogMaxLogCnt()
  {
    ReplyIntDlg dlg = new ReplyIntDlg(
				this,
				"Max. Anzahl Log-Meldungen:",
				this.maxLogCnt,
				1,
				10000 );
    dlg.setVisible( true );
    setMaxLogCnt( dlg.getReply() );
  }


  private void doLogRemove()
  {
    int[] rows = this.listLog.getSelectedIndices();
    if( rows != null ) {
      if( rows.length > 0 ) {
	for( int i = rows.length - 1; i >= 0; --i ) {
	  this.listModelLog.remove( rows[ i ] );
	}
	fireUpdLogActionsEnabled();
      }
    }
  }


  private void doLogRemoveAll()
  {
    if( !this.listModelLog.isEmpty() ) {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Log-Meldungen entfernen?" ) )
      {
	this.listModelLog.clear();
	fireUpdLogActionsEnabled();
      }
    }
  }


  private void doLogSelectAll()
  {
    int n = this.listModelLog.getSize();
    if( n > 0 ) {
      this.listLog.setSelectionInterval( 0, n -1 );
    }
  }


  private void doVarEdit()
  {
    int[] rows = this.tableVar.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int modelRow = this.tableVar.convertRowIndexToModel( rows[ 0 ] );
	if( modelRow >= 0 ) {
	  VarData oldVar = this.tableModelVar.getRow( modelRow );
	  if( oldVar != null ) {
	    final VarData newVar = VarDataDlg.showEditVarDlg( this, oldVar );
	    if( newVar != null ) {
	      this.tableModelVar.setRow( modelRow, newVar );
	      String varName = newVar.getName();
	      if( this.tableModelVar.getValuesEnabled() ) {
		newVar.update( this.memory );
		this.tableModelVar.fireTableRowsUpdated( modelRow, modelRow );
		EventQueue.invokeLater(
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    selectVar( newVar );
				  }
				} );
		fireUpdVarActionsEnabled();
	      }
	    }
	  }
	}
      }
    }
  }


  private void doVarBpAdd()
  {
    int[] rows = this.tableVar.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int modelRow = this.tableVar.convertRowIndexToModel( rows[ 0 ] );
	if( modelRow >= 0 ) {
	  VarData varData = this.tableModelVar.getRow( modelRow );
	  if( varData != null ) {
	    doBpMemoryAdd(
			varData.getName(),
			varData.getAddress(),
			varData.getAddress() + varData.getSize() - 1 );
	  }
	}
      }
    }
  }


  private void doVarRemove()
  {
    int[] rows = this.tableVar.getSelectedRows();
    if( rows != null ) {
      if( rows.length > 0 ) {
	for( int i = 0; i < rows.length; i++ ) {
	  if( rows[ i ] >= 0 ) {
	    rows[ i ] = this.tableVar.convertRowIndexToModel( rows[ i ] );
	  }
	}
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  this.tableModelVar.remove( rows[ i ] );
	  fireUpdVarActionsEnabled();
	}
      }
    }
  }


  private void doVarRemoveAll()
  {
    if( !this.tableModelVar.isEmpty() ) {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Variablen entfernen?" ) )
      {
	removeAllVars();
      }
    }
  }


  private void doWalkTimer()
  {
    this.walkTimer.stop();
    if( this.walkMillis > 0 ) {
      this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_INTO );
    }
  }


	/* --- private Methoden --- */

  private void addBreakpoint( int bpGroupIdx, AbstractBreakpoint bp )
  {
    try {
      if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
	BreakpointListModel model = this.bpModels[ bpGroupIdx ];
	if( model != null ) {
	  model.put( bp );
	  updBreakpointsInCPU();
	  EmuUtil.fireSelectRow( this.bpLists[ bpGroupIdx ], bp );
	}
	fireUpdBreakpointActionsEnabled();
      }
    }
    catch( ClassCastException ex ) {}
  }


  private void appendLogEntryInternal( String text )
  {
    int nToRemove = this.listModelLog.getSize() - this.maxLogCnt + 1;
    if( nToRemove > 0 ) {
      try {
	this.listModelLog.removeRange( 0, nToRemove - 1 );
      }
      catch( ArrayIndexOutOfBoundsException ex ) {}
    }
    this.listModelLog.addElement( text );
    fireUpdLogActionsEnabled();
  }


  private void clear()
  {
    if( this.timerForClear.isRunning() ) {
      this.timerForClear.stop();
    }
    this.btnFlagSign.setSelected( false );
    this.btnFlagZero.setSelected( false );
    this.btnFlagHalf.setSelected( false );
    this.btnFlagPV.setSelected( false );
    this.btnFlagN.setSelected( false );
    this.btnFlagCarry.setSelected( false );
    this.btnIFF1.setSelected( false );
    this.btnIFF2.setSelected( false );
    this.fldRegAF.setText( "" );
    this.fldRegAsciiA.setText( "" );
    this.fldRegBC.setText( "" );
    this.fldRegAsciiB.setText( "" );
    this.fldRegAsciiC.setText( "" );
    this.fldRegDE.setText( "" );
    this.fldRegAsciiD.setText( "" );
    this.fldRegAsciiE.setText( "" );
    this.fldRegHL.setText( "" );
    this.fldRegAsciiH.setText( "" );
    this.fldRegAsciiL.setText( "" );
    this.fldRegIX.setText( "" );
    this.fldRegAsciiIXH.setText( "" );
    this.fldRegAsciiIXL.setText( "" );
    this.fldRegIY.setText( "" );
    this.fldRegAsciiIYH.setText( "" );
    this.fldRegAsciiIYL.setText( "" );
    this.fldRegPC.setText( "" );
    this.fldRegSP.setText( "" );
    this.fldRegAF2.setText( "" );
    this.fldRegBC2.setText( "" );
    this.fldRegDE2.setText( "" );
    this.fldRegHL2.setText( "" );
    this.fldRegI.setText( "" );
    this.fldMemBC.setText( "" );
    this.fldMemDE.setText( "" );
    this.fldMemHL.setText( "" );
    this.fldMemIX.setText( "" );
    this.fldMemIY.setText( "" );
    this.fldMemSP.setText( "" );
    this.fldMemPC.setText( "" );
    this.fldTStates.setText( "" );
    this.listIntSrc.clearSelection();
    this.listIntSrc.setEnabled( false );
    this.fldIntSrc.setContentType( "text/plain" );
    this.fldIntSrc.setText( "" );
    this.fldIntSrc.setEnabled( false );
    this.fldEtc.setContentType( "text/plain" );
    this.fldEtc.setText( "" );
    this.fldEtc.setEnabled( false );
  }


  private void closeTrace()
  {
    if( this.traceWriter != null ) {
      this.cpu.setDebugTracer( null );
      this.traceWriter.println( "---" );
      this.traceWriter.flush();
      this.traceWriter.close();

      boolean isErr = this.traceWriter.checkError();
      this.traceWriter = null;
      if( isErr ) {
	BaseDlg.showErrorDlg(
		this,
		"Die Befehlsaufzeichnungsdatei konnte nicht"
			+ " gespeichert werden." );
      }
    }
  }


  private JComponent createBreakpointFields( int bpGroupIdx )
  {
    BreakpointListModel model   = new BreakpointListModel();
    this.bpModels[ bpGroupIdx ] = model;

    BreakpointList list = new BreakpointList( model );
    list.setVisibleRowCount( 4 );
    try {
      list.setPrototypeCellValue(
		new PCBreakpoint(
			this,
			null,
			0xFFFF,
			"HL",
			0xFFFF,
			"=",
			0xFFFF,
			0x40,
			0 ) );
    }
    catch( InvalidParamException ex ) {}
    list.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    list.setCellRenderer( new BreakpointCellRenderer( this ) );
    this.bpLists[ bpGroupIdx ] = list;

    JScrollPane sp = GUIFactory.createScrollPane( list );
    sp.addMouseListener( this );
    this.bpScrollPanes[ bpGroupIdx ] = sp;

    list.addListSelectionListener( this );
    list.addKeyListener( this );
    list.addMouseListener( this );

    return sp;
  }


  private AbstractButton createFlagField( String text )
  {
    AbstractButton btn = GUIFactory.createCheckBox( text );
    btn.setEnabled( false );
    btn.addActionListener( this );
    return btn;
  }


  private JTextField createHexField()
  {
    JTextField fld = GUIFactory.createTextField( 5 );
    fld.addActionListener( this );
    fld.addFocusListener( this );
    fld.setEditable( false );
    return fld;
  }


  private void editBreakpoint( int bpGroupIdx )
  {
    BreakpointList      list  = this.bpLists[ bpGroupIdx ];
    BreakpointListModel model = this.bpModels[ bpGroupIdx ];
    if( (list != null) && (model != null) ) {
      int[] rows = list.getSelectedIndices();
      if( rows != null ) {
	if( rows.length == 1 ) {
	  int row = rows[ 0 ];
	  if( (row >= 0) && (row < model.getSize()) ) {
	    AbstractBreakpoint oldBP = model.getElementAt( row );
	    if( (oldBP != null) && (this.bpDlg == null) ) {
	      switch( bpGroupIdx ) {
		case BP_PC_IDX:
		  this.bpDlg = new PCBreakpointDlg(
						this,
						oldBP,
						null,
						-1 );
		  break;
		case BP_MEMORY_IDX:
		  this.bpDlg = new MemoryBreakpointDlg(
						this,
						oldBP,
						null,
						-1,
						-1 );
		  break;
		case BP_INPUT_IDX:
		  this.bpDlg = new InputBreakpointDlg( this, oldBP );
		  break;
		case BP_OUTPUT_IDX:
		  this.bpDlg = new OutputBreakpointDlg( this, oldBP );
		  break;
		case BP_INTERRUPT_IDX:
		  this.bpDlg = new InterruptBreakpointDlg(
					this,
					oldBP,
					this.interruptSources );
		  break;
	      }
	      if( this.bpDlg != null ) {
		this.bpDlg.setVisible( true );
		AbstractBreakpoint newBP = this.bpDlg.getApprovedBreakpoint();
		if( newBP != null ) {
		  model.remove( row );
		  addBreakpoint( bpGroupIdx, newBP );
		}
		this.bpDlg = null;
	      }
	    }
	  }
	}
      }
    }
  }


  private void fieldValueChanged( Object fld )
  {
    if( fld == this.spinnerIntMode ) {
      Object value = this.spinnerIntMode.getValue();
      if( value != null ) {
	if( value instanceof Number ) {
	  int intMode = ((Number) value).intValue();
	  if( (intMode >= 0) && (intMode <= 2) ) {
	    this.cpu.setInterruptMode( intMode );
	  }
	}
      }
      updFieldIntMode();
    } else if( fld instanceof JTextField ) {
      JTextField tf = (JTextField) fld;
      if( tf.isEditable() ) {

	// neuer Wert ermitteln
	int v = -1;
	String text = tf.getText();
	if( text != null ) {
	  if( !text.isEmpty() ) {
	    try {
	      v = Integer.parseInt( text, 16 );
	    }
	    catch( NumberFormatException ex ) {
	      v = -1;
	    }
	  }
	}

	if( tf == this.fldRegAF ) {
	  if( v != -1 ) {
	    this.cpu.setRegAF( v );
	  }
	  updFieldsFlag();
	  updFieldsAF();
	}
	else if( tf == this.fldRegAF2 ) {
	  if( v != -1 ) {
	    this.cpu.setRegAF2( v );
	  }
	  updFieldsAF2();
	}
	else if( tf == this.fldRegBC ) {
	  if( v != -1 ) {
	    this.cpu.setRegBC( v );
	  }
	  updFieldsBC();
	}
	else if( tf == this.fldRegBC2 ) {
	  if( v != -1 ) {
	    this.cpu.setRegBC2( v );
	  }
	  updFieldsBC2();
	}
	else if( tf == this.fldRegDE ) {
	  if( v != -1 ) {
	    this.cpu.setRegDE( v );
	  }
	  updFieldsDE();
	}
	else if( tf == this.fldRegDE2 ) {
	  if( v != -1 ) {
	    this.cpu.setRegDE2( v );
	  }
	  updFieldsDE2();
	}
	else if( tf == this.fldRegHL ) {
	  if( v != -1 ) {
	    this.cpu.setRegHL( v );
	  }
	  updFieldsHL();
	}
	else if( tf == this.fldRegHL2 ) {
	  if( v != -1 ) {
	    this.cpu.setRegHL2( v );
	  }
	  updFieldsHL2();
	}
	else if( tf == this.fldRegIX ) {
	  if( v != -1 ) {
	    this.cpu.setRegIX( v );
	  }
	  updFieldsIX();
	}
	else if( tf == this.fldRegIY ) {
	  if( v != -1 ) {
	    this.cpu.setRegIY( v );
	  }
	  updFieldsIY();
	}
	else if( tf == this.fldRegSP ) {
	  if( v != -1 ) {
	    this.cpu.setRegSP( v );
	  }
	  updFieldsSP();
	}
	else if( tf == this.fldRegPC ) {
	  if( v != -1 ) {
	    this.cpu.setRegPC( v );
	  }
	  updFieldsPC();
	}
	else if( tf == this.fldRegI ) {
	  if( v != -1 ) {
	    this.cpu.setRegI( v );
	  }
	  updFieldI();
	}
      }
    }
  }


  private void fireAppendLogEntry( final String text )
  {
    if( text != null ) {
      if( !text.isEmpty() ) {
	EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    appendLogEntryInternal( text );
			  }
			} );
      }
    }
  }


  private void fireUpdBreakpointActionsEnabled()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updBreakpointActionsEnabled();
		  }
		} );
  }


  private void fireUpdDebugger(
			final Z80Breakpoint      breakpoint,
			final Z80InterruptSource iSource )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updDebugger( breakpoint, iSource );
		  }
		} );
  }


  private void fireUpdLogActionsEnabled()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updLogActionsEnabled();
		  }
		} );
  }


  private void fireUpdVarActionsEnabled()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updVarActionsEnabled();
		  }
		} );
  }


  private String getAscii( int value )
  {
    String rv = "";
    if( (value > '\u0020') && (value < '\u007F') ) {
      rv += (char) value;
    }
    return rv;
  }


  private String getMemInfo( int addr, int numBytes )
  {
    StringBuilder buf = new StringBuilder( (numBytes * 4) + 6 );

    // Hex-Darstellung
    for( int i = 0; i < numBytes; i++ ) {
      buf.append( String.format(
			"%02X ",
			this.memory.getMemByte( addr + i, false ) ) );
    }

    // ggf. ASCII-Darstellung ausgeben
    int ch = this.memory.getMemByte( addr, false );
    if( (ch >= '\u0020') && (ch <= '\u007E') ) {
      buf.append( " \"" );
      for( int k = 0; k < numBytes; k++ ) {
	ch = this.memory.getMemByte( addr + k, false );
	if( (ch < '\u0020') || (ch > '\u007E') )
	  break;
	buf.append( (char) ch );
      }
      buf.append( "...\"" );
    }
    return buf.toString();
  }


  private boolean hasBreakpoints()
  {
    boolean rv = false;
    for( BreakpointListModel model : this.bpModels ) {
      if( !model.isEmpty() ) {
	rv = true;
	break;
      }
    }
    return rv;
  }


  private boolean importLabels(
			jkcemu.tools.Label[] labels,
			boolean              caseSensitive,
			boolean              updExistingOnly,
			boolean              removeObsolete )
  {
    boolean rv = false;
    if( labels != null ) {
      boolean stateMem = false;
      boolean statePC  = false;

      // alte importierte Haltepunkte und Variablen merken
      Set<ImportableBreakpoint> oldBPs  = new HashSet<>();
      Set<VarData>              oldVars = new HashSet<>();
      if( updExistingOnly ) {
	for( int groupIdx : new int[] { BP_PC_IDX, BP_MEMORY_IDX } ) {
	  for( AbstractBreakpoint bp : this.bpModels[ groupIdx ] ) {
	    if( bp instanceof ImportableBreakpoint ) {
	      if( ((ImportableBreakpoint) bp).getImported() ) {
		oldBPs.add( (ImportableBreakpoint) bp );
	      }
	    }
	  }
	}
	int nVars = this.tableModelVar.getRowCount();
	for( int i = 0; i < nVars; i++ ) {
	  VarData tmpVar = this.tableModelVar.getRow( i );
	  if( tmpVar != null ) {
	    if( tmpVar.getImported() ) {
	      oldVars.add( tmpVar );
	    }
	  }
	}
      }

      // eigentliches Importieren
      for( jkcemu.tools.Label label : labels ) {
	if( label.isAddress() ) {
	  try {
	    int    labelAddr = label.intValue();
	    String labelName = TextUtil.emptyToNull( label.getLabelName() );
	    if( (labelName != null) && !caseSensitive ) {
	      labelName = labelName.toUpperCase();
	    }
	    int varSize = label.getVarSize();
	    if( varSize > 0 ) {

	      // Speicherzelle
	      int     endAddr = (varSize > 1 ? labelAddr + varSize - 1 : -1);
	      boolean bpFound = false;
	      for( AbstractBreakpoint bp : this.bpModels[ BP_MEMORY_IDX ] ) {
		if( bp instanceof MemoryBreakpoint ) {
		  MemoryBreakpoint memBP = (MemoryBreakpoint) bp;
		  if( ((labelName != null)
			&& TextUtil.equals( labelName, memBP.getName() ))
		      || ((memBP.getName() == null)
			&& (memBP.getBegAddress() == labelAddr)) )
		  {
		    memBP.setAddresses( labelAddr, endAddr );
		    memBP.setImported( true );
		    oldBPs.remove( memBP );
		    bpFound = true;
		  }
		}
	      }
	      if( !bpFound && !updExistingOnly ) {
		MemoryBreakpoint memBP = new MemoryBreakpoint(
							this,
							labelName,
							labelAddr,
							endAddr,
							false,
							true,
							0xFF,
							null,
							0 );
		memBP.setImported( true );
		this.bpModels[ BP_MEMORY_IDX ].put( memBP );
	      }
	      boolean varFound = false;
	      int     nVars    = this.tableModelVar.getRowCount();
	      for( int i = 0; i < nVars; i++ ) {
		VarData tmpVar = this.tableModelVar.getRow( i );
		if( tmpVar != null ) {
		  if( ((labelName != null)
			&& TextUtil.equals( labelName, tmpVar.getName() ))
		      || ((tmpVar.getName() == null)
			&& (tmpVar.getAddress() == labelAddr)) )
		  {
		    VarData.VarType varType = tmpVar.getType();
		    if( (varSize != tmpVar.getSize())
			&& (tmpVar.getType() != VarData.VarType.BYTE_ARRAY) )
		    {
		      varType = VarData.createDefaultType(
						labelName,
						varSize );
		    }
		    tmpVar.setValues(
				labelName,
				labelAddr,
				varType,
				varSize,
				true );
		    oldVars.remove( tmpVar );
		    varFound = true;
		  }
		}
	      }
	      if( !varFound && !updExistingOnly ) {
		this.tableModelVar.addRow(
			VarData.createWithDefaultType(
						labelName,
						labelAddr,
						varSize,
						true ) );
	      }
	      stateMem = true;

	    } else {

	      // Programmadresse
	      boolean bpFound = false;
	      for( AbstractBreakpoint bp : this.bpModels[ BP_PC_IDX ] ) {
		if( bp instanceof PCBreakpoint ) {
		  PCBreakpoint pcBP = (PCBreakpoint) bp;
		  if( ((labelName != null)
			&& TextUtil.equals( labelName, pcBP.getName() ))
		      || ((pcBP.getName() == null)
			&& (pcBP.getAddress() == labelAddr)) )
		  {
		    pcBP.setAddress( labelAddr );
		    pcBP.setImported( true );
		    oldBPs.remove( pcBP );
		    bpFound = true;
		  }
		}
	      }
	      if( !bpFound && !updExistingOnly ) {
		PCBreakpoint pcBP = new PCBreakpoint(
						this,
						labelName,
						labelAddr );
		pcBP.setImported( true );
		this.bpModels[ BP_PC_IDX ].put( pcBP );
	      }
	      statePC = true;
	    }
	  }
	  catch( InvalidParamException ex ) {}
	}
      }

      // Anzeigen aktualisieren
      if( stateMem ) {
	BreakpointListModel model = this.bpModels[ BP_MEMORY_IDX ];
	model.removeAll( oldBPs );
	model.sort();
	this.tableModelVar.removeAll( oldVars );
	this.tableModelVar.fireTableDataChanged();
	rv = true;
      }
      if( statePC ) {
	BreakpointListModel model = this.bpModels[ BP_PC_IDX ];
	model.removeAll( oldBPs );
	model.sort();
	rv = true;
      }
      if( rv && this.cpu.isPause() ) {
	updFieldsPC();
      }
    }

    return rv;
  }


  private void removeAllBreakpoints()
  {
    for( int i = 0; i < this.bpModels.length; i++ ) {
      this.bpModels[ i ].clear();
    }
    updBreakpointsInCPU();
    fireUpdBreakpointActionsEnabled();
  }


  private void removeAllVars()
  {
    this.tableModelVar.clear();
    fireUpdVarActionsEnabled();
  }


  private void setDebuggerEditable( boolean state )
  {
    this.btnFlagSign.setEnabled( state );
    this.btnFlagZero.setEnabled( state );
    this.btnFlagHalf.setEnabled( state );
    this.btnFlagPV.setEnabled( state );
    this.btnFlagN.setEnabled( state );
    this.btnFlagCarry.setEnabled( state );
    this.btnIFF1.setEnabled( state );
    this.btnIFF2.setEnabled( state );
    this.labelIntMode.setEnabled( state );
    this.spinnerIntMode.setEnabled( state );
    this.labelRegI.setEnabled( state );
    this.fldRegI.setEditable( state );
    this.fldRegAF.setEditable( state );
    this.fldRegBC.setEditable( state );
    this.fldRegDE.setEditable( state );
    this.fldRegHL.setEditable( state );
    this.fldRegIX.setEditable( state );
    this.fldRegIY.setEditable( state );
    this.fldRegPC.setEditable( state );
    this.fldRegSP.setEditable( state );
    this.fldRegAF2.setEditable( state );
    this.fldRegBC2.setEditable( state );
    this.fldRegDE2.setEditable( state );
    this.fldRegHL2.setEditable( state );
  }


  private void setDebuggerDisabled( String statusText )
  {
    clear();
    setDebuggerEditable( false );
    this.mnuExecRun.setEnabled( false );
    this.mnuExecWalk30.setEnabled( false );
    this.mnuExecWalk100.setEnabled( false );
    this.mnuExecWalk300.setEnabled( false );
    this.mnuExecWalk500.setEnabled( false );
    this.mnuExecStop.setEnabled( false );
    this.mnuExecStepOver.setEnabled( false );
    this.mnuExecStepInto.setEnabled( false );
    this.mnuExecStepToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnWalk.setEnabled( false );
    this.popupWalk30.setEnabled( false );
    this.popupWalk100.setEnabled( false );
    this.popupWalk300.setEnabled( false );
    this.popupWalk500.setEnabled( false );
    this.btnStop.setEnabled( false );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnStepToRET.setEnabled( false );
    this.btnResetTStates.setEnabled( false );
    this.tableModelVar.setValuesEnabled( false );
    this.labelStatus.setText( statusText );
  }


  private void setDebugRunning()
  {
    setDebuggerEditable( false );
    this.timerForClear.start();
    this.mnuExecRun.setEnabled( false );
    this.mnuExecWalk30.setEnabled( true );
    this.mnuExecWalk100.setEnabled( true );
    this.mnuExecWalk300.setEnabled( true );
    this.mnuExecWalk500.setEnabled( true );
    this.mnuExecStop.setEnabled( true );
    this.mnuExecStepOver.setEnabled( false );
    this.mnuExecStepInto.setEnabled( false );
    this.mnuExecStepToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnWalk.setEnabled( true );
    this.popupWalk30.setEnabled( true );
    this.popupWalk100.setEnabled( true );
    this.popupWalk300.setEnabled( true );
    this.popupWalk500.setEnabled( true );
    this.btnStop.setEnabled( true );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnStepToRET.setEnabled( false );
    this.btnResetTStates.setEnabled( false );
    this.tableModelVar.setValuesEnabled( false );
    this.labelStatus.setText( "Programm wird gerade ausgef\u00FChrt..." );
  }


  private void setDebugStopped(
			Z80Breakpoint      breakpoint,
			Z80InterruptSource iSource )
  {
    if( (breakpoint != null) || (iSource != null) ) {
      this.walkMillis = 0;
    }
    boolean walking = (this.walkMillis > 0);
    boolean stopped = !walking;

    updFieldsFlag();
    updFieldsInterrupt();
    updFieldsAF();
    updFieldsAF2();
    updFieldsBC();
    updFieldsBC2();
    updFieldsDE();
    updFieldsDE2();
    updFieldsHL();
    updFieldsHL2();
    updFieldsIX();
    updFieldsIY();
    updFieldsSP();
    updFieldsPC();
    updFieldsTStates();

    this.mnuExecRun.setEnabled( true );
    this.mnuExecWalk30.setEnabled( true );
    this.mnuExecWalk100.setEnabled( true );
    this.mnuExecWalk300.setEnabled( true );
    this.mnuExecWalk500.setEnabled( true );
    this.mnuExecStop.setEnabled( walking );
    this.mnuExecStepOver.setEnabled( stopped );
    this.mnuExecStepInto.setEnabled( stopped );
    this.mnuExecStepToRET.setEnabled( stopped );

    this.btnRun.setEnabled( true );
    this.btnWalk.setEnabled( true );
    this.popupWalk30.setEnabled( true );
    this.popupWalk100.setEnabled( true );
    this.popupWalk300.setEnabled( true );
    this.popupWalk500.setEnabled( true );
    this.btnStop.setEnabled( walking );
    this.btnStepOver.setEnabled( stopped );
    this.btnStepInto.setEnabled( stopped );
    this.btnStepToRET.setEnabled( stopped );
    this.btnResetTStates.setEnabled( true );

    this.listIntSrc.setEnabled( true );
    this.fldIntSrc.setEnabled( true );
    updIntSrcFields();
    ListModel<Z80InterruptSource> lm = this.listIntSrc.getModel();
    if( lm != null ) {
      if( lm.getSize() == 1 ) {
	this.listIntSrc.setSelectedIndex( 0 );
      }
    }

    this.tableModelVar.update( this.memory );
    this.tableModelVar.setValuesEnabled( true );

    this.fldEtc.setEnabled( true );
    updEtcField();

    if( this.walkMillis > 0 ) {
      this.walkTimer.setInitialDelay( this.walkMillis );
      this.walkTimer.restart();
    } else {
      setDebuggerEditable( true );
      String text = "Programmausf\u00FChrung angehalten";
      if( iSource != null ) {
	StringBuilder buf = new StringBuilder( 128 );
	buf.append( text );
	buf.append( ", Interrupt von " );
	buf.append( iSource.toString() );
	buf.append( " angenommen" );
	text = buf.toString();
      }
      this.labelStatus.setText( text );
    }
  }


  private boolean setMaxLogCnt( Object value )
  {
    boolean rv = false;
    if( value != null ) {
      try {
	int maxLogCnt = -1;
	if( value instanceof Number ) {
	  maxLogCnt = ((Number) value).intValue();
	} else {
	  String s = value.toString();
	  if( s != null ) {
	    maxLogCnt = Integer.parseInt( s );
	  }
	}
	if( maxLogCnt > 0 ) {
	  this.maxLogCnt = maxLogCnt;
	  rv             = true;
	  int nToRemove  = this.listModelLog.getSize() - maxLogCnt;
	  if( nToRemove > 0 ) {
	    try {
	      this.listModelLog.removeRange( 0, nToRemove - 1 );
	    }
	    catch( ArrayIndexOutOfBoundsException ex ) {}
	  }
	}
      }
      catch( NumberFormatException ex ) {}
    }
    return rv;
  }


  private void showErrorNoXMLSupport( Exception ex )
  {
    BaseDlg.showErrorDlg(
		this,
		"Die Java-Laufzeitumgebung unterst\u00FCtzt"
			+ " keine XML-Verarbeitung.",
		ex );
  }


  private void updBreakpointActionsEnabled()
  {
    boolean hasEntries      = false;
    boolean hasImported     = false;
    boolean anyStopEnabled  = false;
    boolean anyStopDisabled = false;
    boolean anyLogEnabled   = false;
    boolean anyLogDisabled  = false;
    boolean selStopEnabled  = false;
    boolean selStopDisabled = false;
    boolean selLogEnabled   = false;
    boolean selLogDisabled  = false;
    int     nSelected       = 0;
    if( this.tabbedPane.getSelectedIndex() == TAB_IDX_CPU ) {
      for( int i = 0; i < BP_GROUP_CNT; i++ ) {
	if( this.bpModels[ i ] != null ) {
	  BreakpointListModel model = this.bpModels[ i ];
	  for( AbstractBreakpoint bp : model ) {
	    hasEntries = true;
	    if( bp.isStopEnabled() ) {
	      anyStopEnabled = true;
	    } else {
	      anyStopDisabled = true;
	    }
	    if( bp.isLogEnabled() ) {
	      anyLogEnabled = true;
	    } else {
	      anyLogDisabled = true;
	    }
	    if( anyStopEnabled && anyStopDisabled
		&& anyLogEnabled && anyLogDisabled )
	    {
	      break;
	    }
	  }
	  int[] rows = this.bpLists[ i ].getSelectedIndices();
	  if( rows != null ) {
	    for( int k = 0; k < rows.length; k++ ) {
	      int row = rows[ k ];
	      if( (row >= 0) && (row < model.getSize()) ) {
		nSelected++;
		if( model.getElementAt( row ).isStopEnabled() ) {
		  selStopEnabled = true;
		} else {
		  selStopDisabled = true;
		}
		if( model.getElementAt( row ).isLogEnabled() ) {
		  selLogEnabled = true;
		} else {
		  selLogDisabled = true;
		}
	      }
	      if( (nSelected > 1)
		  && selStopEnabled && selStopDisabled
		  && selLogEnabled && selLogDisabled )
	      {
		break;
	      }
	    }
	  }
	}
	if( (nSelected > 1)
	    && selStopEnabled && selStopDisabled
	    && selLogEnabled && selLogDisabled
	    && anyStopEnabled && anyStopDisabled
	    && anyLogEnabled && anyLogDisabled )
	{
	  break;
	}
      }
      hasImported = (!this.bpModels[ BP_MEMORY_IDX ].isEmpty()
			|| !this.bpModels[ BP_PC_IDX ].isEmpty());
    }
    this.mnuFileBpsRemoveImported.setEnabled( hasImported );
    this.mnuBpEnableStop.setEnabled( selStopDisabled );
    this.mnuBpDisableStop.setEnabled( selStopEnabled );
    this.mnuBpEnableStopAll.setEnabled( anyStopDisabled );
    this.mnuBpDisableStopAll.setEnabled( anyStopEnabled );
    this.mnuBpEnableLog.setEnabled( selLogDisabled );
    this.mnuBpDisableLog.setEnabled( selLogEnabled );
    this.mnuBpEnableLogAll.setEnabled( anyLogDisabled );
    this.mnuBpDisableLogAll.setEnabled( anyLogEnabled );
    this.mnuBpEdit.setEnabled( nSelected == 1 );
    this.mnuBpRemove.setEnabled( nSelected > 0 );
    this.mnuBpRemoveAll.setEnabled( hasEntries );
  }


  private void updBreakpointsInCPU()
  {
    java.util.List<Z80Breakpoint> list = null;
    for( int i = 0; i < this.bpModels.length; i++ ) {
      BreakpointListModel model = this.bpModels[ i ];
      if( model != null ) {
	int n = model.getSize();
	if( n > 0 ) {
	  for( AbstractBreakpoint bp : model ) {
	    if( bp.isLogEnabled() || bp.isStopEnabled() ) {
	      if( list == null ) {
		list = new ArrayList<>();
	      }
	      list.add( bp );
	    }
	  }
	}
      }
    }
    this.cpu.setBreakpoints( list != null ?
		list.toArray( new Z80Breakpoint[ list.size() ] )
		: null );
  }


  private void updDebugger(
			Z80Breakpoint      breakpoint,
			Z80InterruptSource iSource )
  {
    if( isShowing() ) {
      updDebuggerInternal( breakpoint, iSource );
      if( this.cpu.isPause() && (this.walkMillis == 0) ) {
	setState( Frame.NORMAL );
	toFront();
      }
    }
  }


  private void updDebuggerInternal(
			Z80Breakpoint      breakpoint,
			Z80InterruptSource iSource )
  {
    if( this.timerForClear.isRunning() ) {
      this.timerForClear.stop();
    }
    if( this.cpu.isActive() ) {
      if( this.cpu.isDebugEnabled() ) {
	if( this.cpu.isPause() ) {
	  setDebugStopped( breakpoint, iSource );
	  if( breakpoint != null ) {
	    for( int i = 0; i < this.bpModels.length; i++ ) {
	      BreakpointListModel model = this.bpModels[ i ];
	      if( model != null ) {
		for( AbstractBreakpoint bp : model ) {
		  if( bp == breakpoint ) {
		    this.bpLists[ i ].requestFocus();
		    EmuUtil.fireSelectRow( this.bpLists[ i ], bp );
		    break;
		  }
		}
	      }
	    }
	  }
	} else {
	  setDebugRunning();
	}
      } else {
	setDebugRunning();
      }
    } else {
      setDebuggerDisabled( "Prozessorsystem nicht aktiv" );
    }
  }


  private void updEtcField()
  {
    StringBuilder buf = new StringBuilder( 2048 );
    buf.append( "<html>\n" );
    int len = buf.length();
    if( this.emuThread != null ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
	emuSys.appendStatusHTMLTo( buf, this.cpu );
      }
    }
    if( buf.length() == len ) {
      buf.append( "Keine sonstigen Daten verf&uuml;gbar" );
    }
    buf.append( "</html>\n" );
    this.fldEtc.setContentType( "text/html" );
    this.fldEtc.setText( buf.toString() );
    try {
      this.fldEtc.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private void updFieldsInterrupt()
  {
    this.btnIFF1.setSelected( this.cpu.getIFF1() );
    this.btnIFF2.setSelected( this.cpu.getIFF2() );
    updFieldIntMode();
    updFieldI();
  }


  private void updFieldsFlag()
  {
    this.btnFlagSign.setSelected( this.cpu.getFlagSign() );
    this.btnFlagZero.setSelected( this.cpu.getFlagZero() );
    this.btnFlagHalf.setSelected( this.cpu.getFlagHalf() );
    this.btnFlagPV.setSelected( this.cpu.getFlagPV() );
    this.btnFlagN.setSelected( this.cpu.getFlagN() );
    this.btnFlagCarry.setSelected( this.cpu.getFlagCarry() );
  }


  private void updFieldsAF()
  {
    this.fldRegAF.setText( String.format( "%04X", this.cpu.getRegAF() ) );
    this.fldRegAsciiA.setText( getAscii( this.cpu.getRegA() ) );
  }


  private void updFieldsAF2()
  {
    this.fldRegAF2.setText( String.format( "%04X", this.cpu.getRegAF2() ) );
  }


  private void updFieldsBC()
  {
    int v =  this.cpu.getRegBC();
    this.fldRegBC.setText( String.format( "%04X", v ) );
    this.fldRegAsciiB.setText( getAscii( this.cpu.getRegB() ) );
    this.fldRegAsciiC.setText( getAscii( this.cpu.getRegC() ) );
    this.fldMemBC.setText( getMemInfo( v, 6 ) );
  }


  private void updFieldsBC2()
  {
    this.fldRegBC2.setText( String.format( "%04X", this.cpu.getRegBC2() ) );
  }


  private void updFieldsDE()
  {
    int v =  this.cpu.getRegDE();
    this.fldRegDE.setText( String.format( "%04X", v ) );
    this.fldRegAsciiD.setText( getAscii( this.cpu.getRegD() ) );
    this.fldRegAsciiE.setText( getAscii( this.cpu.getRegE() ) );
    this.fldMemDE.setText( getMemInfo( v, 6 ) );
  }


  private void updFieldsDE2()
  {
    this.fldRegDE2.setText( String.format( "%04X", this.cpu.getRegDE2() ) );
  }


  private void updFieldsHL()
  {
    int v =  this.cpu.getRegHL();
    this.fldRegHL.setText( String.format( "%04X", v ) );
    this.fldRegAsciiH.setText( getAscii( this.cpu.getRegH() ) );
    this.fldRegAsciiL.setText( getAscii( this.cpu.getRegL() ) );
    this.fldMemHL.setText( getMemInfo( v, 6 ) );
  }


  private void updFieldsHL2()
  {
    this.fldRegHL2.setText( String.format( "%04X", this.cpu.getRegHL2() ) );
  }


  private void updFieldsIX()
  {
    int v =  this.cpu.getRegIX();
    this.fldRegIX.setText( String.format( "%04X", v ) );
    this.fldRegAsciiIXH.setText( getAscii( this.cpu.getRegIXH() ) );
    this.fldRegAsciiIXL.setText( getAscii( this.cpu.getRegIXL() ) );
    this.fldMemIX.setText( getMemInfo( v, 8 ) );
  }


  private void updFieldsIY()
  {
    int v = this.cpu.getRegIY();
    this.fldRegIY.setText( String.format( "%04X", v ) );
    this.fldRegAsciiIYH.setText( getAscii( this.cpu.getRegIYH() ) );
    this.fldRegAsciiIYL.setText( getAscii( this.cpu.getRegIYL() ) );
    this.fldMemIY.setText( getMemInfo( v, 8 ) );
  }


  private void updFieldI()
  {
    this.fldRegI.setText( String.format( "%02X", this.cpu.getRegI() ) );
  }


  private void updFieldsSP()
  {
    int addr = this.cpu.getRegSP();
    this.fldRegSP.setText( String.format( "%04X", addr ) );

    // ersten Eintraege auf dem Stack anzeigen
    int           n   = 6;
    StringBuilder buf = new StringBuilder( n * 5 );

    for( int i = 0; i < n; i++ ) {
      buf.append( String.format(
			"%04X ",
			this.memory.getMemWord( addr ) ) );
      addr++;
      addr++;
    }
    this.fldMemSP.setText( buf.toString() );
  }


  private void updFieldsPC()
  {
    javax.swing.text.Document doc     = this.fldMemPC.getDocument();
    BreakpointListModel       bpModel = this.bpModels[ BP_PC_IDX ];
    StringBuilder             tmpBuf  = new StringBuilder();

    int addr = this.cpu.getRegPC();
    this.fldRegPC.setText( String.format( "%04X", addr ) );

    // Befehle anzeigen
    int    nRows  = 0;
    int    hAvail = this.fldMemPC.getHeight();
    Border border = this.fldMemPC.getBorder();
    if( border != null ) {
      if( border != null ) {
	Insets insets = border.getBorderInsets( this.fldMemPC );
	if( insets != null ) {
	  hAvail -= insets.top;
	  hAvail -= insets.bottom;
	}
      }
    }
    Insets margin = this.fldMemPC.getMargin();
    if( margin != null ) {
      hAvail -= margin.top;
      hAvail -= margin.bottom;
    }
    if( hAvail > 0 ) {
      nRows = DEFAULT_FLD_MEM_PC_ROWS;
      Font font = this.fldMemPC.getFont();
      if( font != null ) {
	int hFont = font.getSize();
	if( hFont > 0 ) {
	  nRows = hAvail / hFont;
	}
      }
    }
    this.fldMemPC.setText( "" );
    for( int row = 0; row < nRows; row++ ) {
      if( row > 0 ) {
	this.fldMemPC.append( "\n" );
      }
      int    linePos = 0;
      String label   = null;
      if( (doc != null) && (bpModel != null) ) {
	linePos = doc.getLength();
	for( AbstractBreakpoint breakpoint : bpModel ) {
	  if( breakpoint instanceof PCBreakpoint ) {
	    if( ((PCBreakpoint) breakpoint).getAddress() == addr ) {
	      String s = ((PCBreakpoint) breakpoint).getName();
	      if( s != null ) {
		if( label != null ) {
		  label = label + ", " + s;
		} else {
		  label = s;
		}
	      }
	    }
	  }
	}
      }

      int len = 0;
      if( this.emuThread != null ) {
	EmuSys emuSys = this.emuThread.getEmuSys();
	if( emuSys != null ) {
	  if( emuSys != null ) {
	    tmpBuf.setLength( 0 );
	    len = emuSys.reassembleSysCall(
					this.memory,
					addr,
					tmpBuf,
					false,
					18,
					26,
					40 );
	  }
	}
      }
      int txtLen = tmpBuf.length();
      if( (len > 0) && (txtLen > 0) ) {
	if( tmpBuf.charAt( txtLen - 1 ) == '\n' ) {
	  tmpBuf.setLength( txtLen - 1 );
	}
	this.fldMemPC.append( tmpBuf.toString() );
	addr += len;
      } else {
	Z80ReassInstr instr = Z80Reassembler.reassInstruction(
						      this.memory,
						      addr );
	if( instr != null ) {

	  // Adresse ausgeben
	  this.fldMemPC.append( String.format( "%04X  ", addr ) );

	  // Befehlscode ausgeben
	  int w = 12;
	  int n = instr.getLength();
	  for( int i = 0; i < n; i++ ) {
	    this.fldMemPC.append(
		  String.format( "%02X ", instr.getByte( i ) ) );
	    addr++;
	    w -= 3;
	  }
	  while( w > 0 ) {
	    this.fldMemPC.append( "\u0020" );
	    --w;
	  }

	  // Assembler-Befehlsname ausgeben
	  String s = instr.getName();
	  if( s != null ) {
	    this.fldMemPC.append( s );

	    // Argument ausgeben
	    w = 8 - s.length();
	    s = instr.getArg1();
	    if( s != null ) {
	      while( w > 0 ) {
		this.fldMemPC.append( "\u0020" );
		--w;
	      }
	      this.fldMemPC.append( s );

	      s = instr.getArg2();
	      if( s != null ) {
		this.fldMemPC.append( "," );
		this.fldMemPC.append( s );
	      }
	    }
	  }

	} else {

	  this.fldMemPC.append( String.format(
				"%02X",
				this.memory.getMemByte( addr, true ) ) );
	  addr++;
	}

	if( (label != null) && (doc != null) ) {
	  int lineLen = doc.getLength() - linePos;
	  do {
	    this.fldMemPC.append( "\u0020" );
	    lineLen++;
	  } while( lineLen < 40 );
	  this.fldMemPC.append( ";" );
	  if( label.length() > 20 ) {
	    this.fldMemPC.append( label.substring( 0, 17 ) );
	    this.fldMemPC.append( "..." );
	  } else {
	    this.fldMemPC.append( label );
	  }
	}
      }
    }
    try {
      this.fldMemPC.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private void updFieldsTStates()
  {
    this.fldTStates.setText(
		Long.toString( this.cpu.getProcessedTStates() ) );
  }


  private void updIntBreakpoints( Z80InterruptSource[] iSources )
  {
    BreakpointListModel model = this.bpModels[ BP_INTERRUPT_IDX ];
    if( model != null ) {
      if( iSources != null ) {
	int n = model.getSize();
	for( int i = n - 1; i >= 0; --i ) {
	  boolean            done = false;
	  AbstractBreakpoint bp   = model.getElementAt( i );
	  if( bp instanceof InterruptBreakpoint ) {
	    InterruptBreakpoint iBP     = (InterruptBreakpoint) bp;
	    Z80InterruptSource  iSource = iBP.getInterruptSource();
	    if( iSource != null ) {
	      String s = iSource.toString();
	      for( int k = 0; k < iSources.length; k++ ) {
		if( TextUtil.equals( s, iSources[ k ].toString() ) ) {
		  /*
		   * Breakpoint-Objekt nicht neu anlegen,
		   * sondern darin nur die Interrupt-Quelle ersetzen,
		   * damit es automatisch gleich im Z80CPU-Objekt passt.
		   */
		  iBP.setInterruptSource( iSources[ k ] );
		  done = true;
		  break;
		}
	      }
	    }
	  }
	  if( !done ) {
	    model.remove( i );
	  }
	}
      }
    }
    updBreakpointsInCPU();
    fireUpdBreakpointActionsEnabled();
  }


  private void updIntSrcFields()
  {
    Z80InterruptSource[] iSources = this.cpu.getInterruptSources();
    if( iSources != null ) {
      if( iSources.length == 0 ) {
	iSources = null;
      }
    }
    if( (this.interruptSources == null)
	|| (iSources != this.interruptSources) )
    {
      AbstractBreakpointDlg dlg = this.bpDlg;
      if( dlg != null ) {
	if( dlg instanceof InterruptBreakpointDlg ) {
	  ((InterruptBreakpointDlg) dlg).setInterruptSources( iSources );
	}
      }
      updIntBreakpoints( iSources );
      if( iSources != null ) {
	this.listIntSrc.setListData( iSources );
      } else {
	this.listIntSrc.setListData( new Z80InterruptSource[ 0 ] );
      }
      this.interruptSources = iSources;
    }
    if( (this.interruptSources != null)
	&& (this.lastSelectedIntSrc != null) )
    {
      for( int i = 0; i < this.interruptSources.length; i++ ) {
	if( this.interruptSources[ i ] == this.lastSelectedIntSrc ) {
	  this.listIntSrc.setSelectedValue( this.lastSelectedIntSrc, true );
	}
      }
    }
    updIntSrcInfoField();
  }


  private void updIntSrcInfoField()
  {
    boolean done = false;
    if( this.listIntSrc.isEnabled() ) {
      int[] rows = this.listIntSrc.getSelectedIndices();
      if( rows != null ) {
	if( rows.length == 1 ) {
	  Object obj = this.listIntSrc.getSelectedValue();
	  if( obj != null ) {
	    if( obj instanceof Z80InterruptSource ) {
	      this.lastSelectedIntSrc = (Z80InterruptSource) obj;
	      StringBuilder buf = new StringBuilder( 2048 );
	      buf.append( "<html>\n<h1>" );
	      EmuUtil.appendHTML( buf, this.lastSelectedIntSrc.toString() );
	      buf.append( "</h1>\n" );
	      this.lastSelectedIntSrc.appendInterruptStatusHTMLTo( buf );
	      buf.append( "</html>\n" );
	      this.fldIntSrc.setContentType( "text/html" );
	      this.fldIntSrc.setText( buf.toString() );
	      done = true;
	    }
	  }
	}
      }
      if( !done ) {
	ListModel<Z80InterruptSource> lm = this.listIntSrc.getModel();
	if( lm != null ) {
	  if( lm.getSize() > 0 ) {
	    this.fldIntSrc.setContentType( "text/plain" );
	    this.fldIntSrc.setText(
			"Bitte Interrupt-Quelle ausw\u00E4hlen!" );
	  }
	}
      }
    }
    try {
      this.fldIntSrc.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private void updFieldIntMode()
  {
    try {
      this.spinnerIntMode.setValue( this.cpu.getInterruptMode() );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private void updLogActionsEnabled()
  {
    boolean isTab      = (this.tabbedPane.getSelectedIndex() == TAB_IDX_LOG);
    boolean hasEntries = !this.listModelLog.isEmpty();
    boolean selected   = (this.listLog.getSelectedIndex() >= 0);
    this.mnuLogCopy.setEnabled( isTab && selected );
    this.mnuLogSelectAll.setEnabled( isTab && hasEntries );
    this.mnuLogRemove.setEnabled( isTab && selected );
    this.mnuLogRemoveAll.setEnabled( isTab && hasEntries );
  }


  private void updVarActionsEnabled()
  {
    if( this.selectionModelVar != null ) {
      boolean isTab = (this.tabbedPane.getSelectedIndex() == TAB_IDX_VAR);
      boolean hasEntries = !this.tableModelVar.isEmpty();
      boolean selected   = (this.tableVar.getSelectedRow() >= 0);
      this.mnuVarEdit.setEnabled( isTab && selected );
      this.mnuVarBpAdd.setEnabled( isTab && selected );
      this.mnuVarRemove.setEnabled( isTab && selected );
      this.mnuVarRemoveAll.setEnabled( isTab && hasEntries );
    }
  }
}
