/*
 * (c) 2008-2017 Jens Mueller
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.HelpFrm;
import jkcemu.base.ReplyIntDlg;
import jkcemu.text.TextUtil;
import jkcemu.tools.ToolsUtil;
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

  private static final String TEXT_BP_REMOVE
			= "Markierte Halte-/Log-Punkte entfernen";
  private static final String TEXT_BP_REMOVE_ALL
			= "Alle Halte-/Log-Punkte entfernen";

  private static final String TEXT_BP_ENABLE_STOP
		= "In markierten Halte-/Log-Punkten Anhalten aktivieren";
  private static final String TEXT_BP_DISABLE_STOP
		= "In markierten Halte-/Log-Punkten Anhalten deaktivieren";

  private static final String TEXT_BP_ENABLE_STOP_ALL
		= "In allen Halte-/Log-Punkten Anhalten aktivieren";
  private static final String TEXT_BP_DISABLE_STOP_ALL
		= "In allen Halte-/Log-Punkten Anhalten deaktivieren";

  private static final String TEXT_BP_ENABLE_LOG
		= "In markierten Halte-/Log-Punkten Loggen aktivieren";
  private static final String TEXT_BP_DISABLE_LOG
		= "In markierten Halte-/Log-Punkten Loggen deaktivieren";

  private static final String TEXT_BP_ENABLE_LOG_ALL
		= "In allen Halte-/Log-Punkten Loggen aktivieren";
  private static final String TEXT_BP_DISABLE_LOG_ALL
		= "In allen Halte-/Log-Punkten Loggen deaktivieren";

  private static final String TEXT_VAR_ADD
			= "Variable hinzuf\u00FCgen...";
  private static final String TEXT_VAR_EDIT
			= "Variable bearbeiten...";
  private static final String TEXT_VAR_REMOVE
			= "Selektierte Variablen entfernen";
  private static final String TEXT_VAR_REMOVE_ALL
			= "Alle Variablen entfernen";

  private static final String TEXT_LOG_COPY
			= "Selektierte Log-Meldungen kopieren";
  private static final String TEXT_LOG_SELECT_ALL
			= "Alle Log-Meldungen ausw\u00E4hlen";
  private static final String TEXT_LOG_REMOVE
			= "Selektierte Log-Meldungen entfernen";
  private static final String TEXT_LOG_REMOVE_ALL
			= "Alle Log-Meldungen entfernen";

  private static final int DEFAULT_MAX_LOG_CNT = 500;

  private EmuThread                 emuThread;
  private Z80CPU                    cpu;
  private Z80Memory                 memory;
  private LabelImportOptions        labelImportOptions;
  private Set<Integer>              removedImportedBreakAddrs;
  private Set<String>               removedImportedBreakNames;
  private Set<String>               removedImportedVarNames;
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
  private int                       popupBreakGroupIdx;
  private JMenuItem                 mnuFileClose;
  private JMenuItem                 mnuFileBreakImport;
  private JMenuItem                 mnuFileBreakImportAgain;
  private JMenuItem                 mnuFileBreakRemoveImported;
  private JMenuItem                 mnuFileOpenTrace;
  private JMenuItem                 mnuDebugRun;
  private JMenuItem                 mnuDebugWalk100;
  private JMenuItem                 mnuDebugWalk300;
  private JMenuItem                 mnuDebugWalk500;
  private JMenuItem                 mnuDebugStop;
  private JMenuItem                 mnuDebugStepOver;
  private JMenuItem                 mnuDebugStepInto;
  private JMenuItem                 mnuDebugStepToRET;
  private JMenuItem                 mnuDebugBreakInterruptAdd;
  private JMenuItem                 mnuDebugBreakInputAdd;
  private JMenuItem                 mnuDebugBreakOutputAdd;
  private JMenuItem                 mnuDebugBreakMemoryAdd;
  private JMenuItem                 mnuDebugBreakPCAdd;
  private JMenuItem                 mnuDebugBreakEdit;
  private JMenuItem                 mnuDebugBreakEnableStop;
  private JMenuItem                 mnuDebugBreakDisableStop;
  private JMenuItem                 mnuDebugBreakEnableStopAll;
  private JMenuItem                 mnuDebugBreakDisableStopAll;
  private JMenuItem                 mnuDebugBreakEnableLog;
  private JMenuItem                 mnuDebugBreakDisableLog;
  private JMenuItem                 mnuDebugBreakEnableLogAll;
  private JMenuItem                 mnuDebugBreakDisableLogAll;
  private JMenuItem                 mnuDebugBreakRemove;
  private JMenuItem                 mnuDebugBreakRemoveAll;
  private JCheckBoxMenuItem         mnuDebugTracer;
  private JMenuItem                 mnuVarAdd;
  private JMenuItem                 mnuVarEdit;
  private JMenuItem                 mnuVarRemove;
  private JMenuItem                 mnuVarRemoveAll;
  private JMenuItem                 mnuLogCopy;
  private JMenuItem                 mnuLogSelectAll;
  private JMenuItem                 mnuLogRemove;
  private JMenuItem                 mnuLogRemoveAll;
  private JMenuItem                 mnuLogMaxLogCnt;
  private JMenuItem                 mnuHelpContent;
  private JPopupMenu                popupBreak;
  private JMenuItem                 popupBreakAdd;
  private JMenuItem                 popupBreakRemove;
  private JMenuItem                 popupBreakRemoveAll;
  private JMenuItem                 popupBreakEnableStop;
  private JMenuItem                 popupBreakDisableStop;
  private JMenuItem                 popupBreakEnableStopAll;
  private JMenuItem                 popupBreakDisableStopAll;
  private JMenuItem                 popupBreakEnableLog;
  private JMenuItem                 popupBreakDisableLog;
  private JMenuItem                 popupBreakEnableLogAll;
  private JMenuItem                 popupBreakDisableLogAll;
  private JPopupMenu                popupLog;
  private JMenuItem                 popupLogCopy;
  private JMenuItem                 popupLogSelectAll;
  private JMenuItem                 popupLogRemove;
  private JMenuItem                 popupLogRemoveAll;
  private JPopupMenu                popupVar;
  private JMenuItem                 popupVarAdd;
  private JMenuItem                 popupVarEdit;
  private JMenuItem                 popupVarRemove;
  private JMenuItem                 popupVarRemoveAll;
  private JPopupMenu                popupWalk;
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
  private BreakpointList[]          bpLists;
  private JScrollPane[]             bpScrollPanes;
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
    this.emuThread                 = emuThread;
    this.cpu                       = cpu;
    this.memory                    = memory;
    this.labelImportOptions        = null;
    this.removedImportedBreakAddrs = new TreeSet<>();
    this.removedImportedBreakNames = new TreeSet<>();
    this.removedImportedVarNames   = new TreeSet<>();
    this.maxLogCnt                 = DEFAULT_MAX_LOG_CNT;
    this.memPCClickAddr            = -1;
    this.walkMillis                = 0;
    this.walkTimer                 = new javax.swing.Timer( 300, this );
    this.walkTimer.setRepeats( false );
    this.lastSelectedIntSrc        = null;
    this.lastBreakpointFile        = null;
    this.lastTraceFile             = null;
    this.traceWriter               = null;
    this.bpDlg                     = null;
    this.bpModels                  = new BreakpointListModel[ BP_GROUP_CNT ];
    this.bpLists                   = new BreakpointList[ BP_GROUP_CNT ];
    this.bpScrollPanes             = new JScrollPane[ BP_GROUP_CNT ];
    this.popupBreakGroupIdx        = -1;
    this.interruptSources          = cpu.getInterruptSources();
    if( this.interruptSources != null ) {
      if( this.interruptSources.length == 0 ) {
	this.interruptSources = null;
      }
    }
    setTitle( "JKCEMU Debugger" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileBreakImport = createJMenuItem(
		"Halte-/Log-Punkte importieren..." );
    mnuFile.add( this.mnuFileBreakImport );

    this.mnuFileBreakImportAgain = createJMenuItem(
			"Halte-/Log-Punkte erneut importieren",
			KeyEvent.VK_F3,
			0 );
    this.mnuFileBreakImportAgain.setEnabled( false );
    mnuFile.add( this.mnuFileBreakImportAgain );
    mnuFile.addSeparator();

    this.mnuFileBreakRemoveImported = createJMenuItem(
				"Importierte Halte-/Log-Punkte entfernen" );
    this.mnuFileBreakRemoveImported.setEnabled( false );
    mnuFile.add( this.mnuFileBreakRemoveImported );
    mnuFile.addSeparator();

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Debuggen
    JMenu mnuDebug = new JMenu( "Debuggen" );
    mnuDebug.setMnemonic( KeyEvent.VK_B );

    this.mnuDebugStop = createJMenuItem( TEXT_STOP, KeyEvent.VK_F4, 0 );
    mnuDebug.add( this.mnuDebugStop );

    this.mnuDebugRun = createJMenuItem( TEXT_RUN, KeyEvent.VK_F5, 0 );
    mnuDebug.add( this.mnuDebugRun );

    JMenu mnuDebugWalk = new JMenu( TEXT_WALK );
    mnuDebug.add( mnuDebugWalk );

    this.mnuDebugWalk500 = createJMenuItem( "sehr langsam" );
    mnuDebugWalk.add( this.mnuDebugWalk500 );

    this.mnuDebugWalk300 = createJMenuItem( "langsam" );
    mnuDebugWalk.add( this.mnuDebugWalk300 );

    this.mnuDebugWalk100 = createJMenuItem( "etwas schneller" );
    mnuDebugWalk.add( this.mnuDebugWalk100 );

    this.mnuDebugStepOver = createJMenuItem(
					TEXT_STEP_OVER,
					KeyEvent.VK_F6,
					0 );
    mnuDebug.add( this.mnuDebugStepOver );

    this.mnuDebugStepInto = createJMenuItem(
					TEXT_STEP_INTO,
					KeyEvent.VK_F7,
					0 );
    mnuDebug.add( this.mnuDebugStepInto );

    this.mnuDebugStepToRET = createJMenuItem(
					TEXT_STEP_TO_RET,
					KeyEvent.VK_F8,
					0 );
    mnuDebug.add( this.mnuDebugStepToRET );
    mnuDebug.addSeparator();

    this.mnuDebugBreakPCAdd = createJMenuItem(
		"Halte-/Log-Punkt auf Programmadresse hinzuf\u00FCgen...",
		KeyEvent.VK_A, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakPCAdd );

    this.mnuDebugBreakMemoryAdd = createJMenuItem(
		"Halte-/Log-Punkt auf Speicherbereich hinzuf\u00FCgen...",
		KeyEvent.VK_M, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakMemoryAdd );

    this.mnuDebugBreakInputAdd = createJMenuItem(
		"Halte-/Log-Punkt auf Eingabetor hinzuf\u00FCgen...",
		KeyEvent.VK_I, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakInputAdd );

    this.mnuDebugBreakOutputAdd = createJMenuItem(
		"Halte-/Log-Punkt auf Ausgabetor hinzuf\u00FCgen...",
		KeyEvent.VK_O, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakOutputAdd );

    this.mnuDebugBreakInterruptAdd = createJMenuItem(
		"Halte-/Log-Punkt auf Interrupt-Quelle hinzuf\u00FCgen...",
		KeyEvent.VK_Q, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakInterruptAdd );

    this.mnuDebugBreakEdit = createJMenuItem(
		"Halte-/Log-Punkt bearbeiten",
		KeyEvent.VK_E, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakEdit );
    mnuDebug.addSeparator();

    this.mnuDebugBreakRemove = createJMenuItem( TEXT_BP_REMOVE );
    mnuDebug.add( this.mnuDebugBreakRemove );

    this.mnuDebugBreakRemoveAll = createJMenuItem( TEXT_BP_REMOVE_ALL );
    mnuDebug.add( this.mnuDebugBreakRemoveAll );
    mnuDebug.addSeparator();

    this.mnuDebugBreakEnableStop = createJMenuItem( TEXT_BP_ENABLE_STOP );
    mnuDebug.add( this.mnuDebugBreakEnableStop );

    this.mnuDebugBreakDisableStop = createJMenuItem( TEXT_BP_DISABLE_STOP );
    mnuDebug.add( this.mnuDebugBreakDisableStop );
    mnuDebug.addSeparator();

    this.mnuDebugBreakEnableStopAll = createJMenuItem(
						TEXT_BP_ENABLE_STOP_ALL );
    mnuDebug.add( this.mnuDebugBreakEnableStopAll );

    this.mnuDebugBreakDisableStopAll = createJMenuItem(
						TEXT_BP_DISABLE_STOP_ALL );
    mnuDebug.add( this.mnuDebugBreakDisableStopAll );
    mnuDebug.addSeparator();

    this.mnuDebugBreakEnableLog = createJMenuItem( TEXT_BP_ENABLE_LOG );
    mnuDebug.add( this.mnuDebugBreakEnableLog );

    this.mnuDebugBreakDisableLog = createJMenuItem( TEXT_BP_DISABLE_LOG );
    mnuDebug.add( this.mnuDebugBreakDisableLog );
    mnuDebug.addSeparator();

    this.mnuDebugBreakEnableLogAll = createJMenuItem(
						TEXT_BP_ENABLE_LOG_ALL );
    mnuDebug.add( this.mnuDebugBreakEnableLogAll );

    this.mnuDebugBreakDisableLogAll = createJMenuItem(
						TEXT_BP_DISABLE_LOG_ALL );
    mnuDebug.add( this.mnuDebugBreakDisableLogAll );
    mnuDebug.addSeparator();

    this.mnuDebugTracer = new JCheckBoxMenuItem( "Befehle aufzeichnen" );
    this.mnuDebugTracer.setSelected( false );
    this.mnuDebugTracer.addActionListener( this );
    mnuDebug.add( this.mnuDebugTracer );


    // Menu Log-Meldungen
    JMenu mnuLog = new JMenu( "Log-Meldungen" );
    mnuLog.setMnemonic( KeyEvent.VK_L );

    this.mnuLogCopy = createJMenuItem( TEXT_LOG_COPY );
    mnuLog.add( this.mnuLogCopy );

    this.mnuLogSelectAll = createJMenuItem( TEXT_LOG_SELECT_ALL );
    mnuLog.add( this.mnuLogSelectAll );
    mnuLog.addSeparator();

    this.mnuLogRemove = createJMenuItem( TEXT_LOG_REMOVE );
    mnuLog.add( this.mnuLogRemove );

    this.mnuLogRemoveAll = createJMenuItem( TEXT_LOG_REMOVE_ALL );
    mnuLog.add( this.mnuLogRemoveAll );
    mnuLog.addSeparator();

    this.mnuLogMaxLogCnt = createJMenuItem( "Max. Anzahl Log-Meldungen..." );
    mnuLog.add( this.mnuLogMaxLogCnt );


    // Menu Variablen
    JMenu mnuVar = new JMenu( "Variablen" );
    mnuVar.setMnemonic( KeyEvent.VK_V );

    this.mnuVarAdd = createJMenuItem( TEXT_VAR_ADD );
    mnuVar.add( this.mnuVarAdd );

    this.mnuVarEdit = createJMenuItem( TEXT_VAR_EDIT );
    mnuVar.add( this.mnuVarEdit );
    mnuVar.addSeparator();

    this.mnuVarRemove = createJMenuItem( TEXT_VAR_REMOVE );
    mnuVar.add( this.mnuVarRemove );

    this.mnuVarRemoveAll = createJMenuItem( TEXT_VAR_REMOVE_ALL );
    mnuVar.add( this.mnuVarRemoveAll );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuDebug );
    mnuBar.add( mnuLog );
    mnuBar.add( mnuVar );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Popup-Menu fuer Halte-/Log-Punkt
    this.popupBreak = new JPopupMenu();

    this.popupBreakAdd = createJMenuItem(
				"Halte-/Log-Punkt hinzuf\u00FCgen..." );
    this.popupBreak.add( this.popupBreakAdd );

    this.popupBreakRemove = createJMenuItem( TEXT_BP_REMOVE );
    this.popupBreak.add( this.popupBreakRemove );

    this.popupBreakRemoveAll = createJMenuItem( TEXT_BP_REMOVE_ALL );
    this.popupBreak.add( this.popupBreakRemoveAll );
    this.popupBreak.addSeparator();

    this.popupBreakEnableStop = createJMenuItem( TEXT_BP_ENABLE_STOP );
    this.popupBreak.add( this.popupBreakEnableStop );

    this.popupBreakDisableStop = createJMenuItem( TEXT_BP_DISABLE_STOP );
    this.popupBreak.add( this.popupBreakDisableStop );
    this.popupBreak.addSeparator();

    this.popupBreakEnableStopAll = createJMenuItem( TEXT_BP_ENABLE_STOP_ALL );
    this.popupBreak.add( this.popupBreakEnableStopAll );

    this.popupBreakDisableStopAll = createJMenuItem(
						TEXT_BP_DISABLE_STOP_ALL );
    this.popupBreak.add( this.popupBreakDisableStopAll );
    this.popupBreak.addSeparator();

    this.popupBreakEnableLog = createJMenuItem( TEXT_BP_ENABLE_LOG );
    this.popupBreak.add( this.popupBreakEnableLog );

    this.popupBreakDisableLog = createJMenuItem( TEXT_BP_DISABLE_LOG );
    this.popupBreak.add( this.popupBreakDisableLog );
    this.popupBreak.addSeparator();

    this.popupBreakEnableLogAll = createJMenuItem( TEXT_BP_ENABLE_LOG_ALL );
    this.popupBreak.add( this.popupBreakEnableLogAll );

    this.popupBreakDisableLogAll = createJMenuItem(
						TEXT_BP_DISABLE_LOG_ALL );
    this.popupBreak.add( this.popupBreakDisableLogAll );


    // Popup-Menu fuer Log-Meldungen
    this.popupLog = new JPopupMenu();

    this.popupLogCopy = createJMenuItem( TEXT_LOG_COPY );
    this.popupLog.add( this.popupLogCopy );

    this.popupLogSelectAll = createJMenuItem( TEXT_LOG_SELECT_ALL );
    this.popupLog.add( this.popupLogSelectAll );
    this.popupLog.addSeparator();

    this.popupLogRemove = createJMenuItem( TEXT_LOG_REMOVE );
    this.popupLog.add( this.popupLogRemove );

    this.popupLogRemoveAll = createJMenuItem( TEXT_LOG_REMOVE_ALL );
    this.popupLog.add( this.popupLogRemoveAll );


    // Popup-Menu fuer Variablen
    this.popupVar = new JPopupMenu();

    this.popupVarAdd = createJMenuItem( TEXT_VAR_ADD );
    this.popupVar.add( this.popupVarAdd );

    this.popupVarEdit = createJMenuItem( TEXT_VAR_EDIT );
    this.popupVar.add( this.popupVarEdit );
    this.popupVar.addSeparator();

    this.popupVarRemove = createJMenuItem( TEXT_VAR_REMOVE );
    this.popupVar.add( this.popupVarRemove );

    this.popupVarRemoveAll = createJMenuItem( TEXT_VAR_REMOVE_ALL );
    this.popupVar.add( this.popupVarRemoveAll );


    // Popup-Menu fuer langsames ausfuehren
    this.popupWalk = new JPopupMenu();

    this.popupWalk500 = createJMenuItem(
				"Programm sehr langsam ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk500 );

    this.popupWalk300 = createJMenuItem( "Programm langsam ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk300 );

    this.popupWalk100 = createJMenuItem(
				"Programm etwas schneller ausf\u00FChren" );
    this.popupWalk.add( this.popupWalk100 );


    // Popup-Menu in der Reassembler-Anzeige des PC-Bereichs
    this.popupMemPC = new JPopupMenu();

    this.popupMemPCCopy = createJMenuItem( "Kopieren" );
    this.popupMemPC.add( this.popupMemPCCopy );
    this.popupMemPC.addSeparator();

    this.popupMemPCBreak = createJMenuItem( "Halte-/Log-Punkt anlegen" );
    this.popupMemPC.add( this.popupMemPCBreak );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, BorderLayout.NORTH );

    this.btnStop = createImageButton( "/images/debug/stop.png", TEXT_STOP );
    toolBar.add( this.btnStop );

    this.btnRun = createImageButton( "/images/debug/run.png", TEXT_RUN );
    toolBar.add( this.btnRun );

    this.btnWalk = createImageButton( "/images/debug/walk.png", TEXT_WALK );
    toolBar.add( this.btnWalk );

    this.btnStepOver = createImageButton(
				"/images/debug/step_over.png",
				TEXT_STEP_OVER );
    toolBar.add( this.btnStepOver );

    this.btnStepInto = createImageButton(
				"/images/debug/step_into.png",
				TEXT_STEP_INTO );
    toolBar.add( this.btnStepInto );

    this.btnStepToRET = createImageButton(
				"/images/debug/step_up.png",
				TEXT_STEP_TO_RET );
    toolBar.add( this.btnStepToRET );


    // Hauptbereich
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab CPU
    JPanel panelCPU = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "CPU", panelCPU );

    GridBagConstraints gbcCPU = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.VERTICAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Bereich Flags
    JPanel panelFlag = new JPanel( new GridBagLayout() );
    panelFlag.setBorder( BorderFactory.createTitledBorder( "Flags" ) );
    panelCPU.add( panelFlag, gbcCPU );

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

    this.btnFlagZero    = createFlagField( "Z" );
    gbcFlag.insets.left = 5;
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
    JPanel panelInt = new JPanel( new GridBagLayout() );
    panelInt.setBorder( BorderFactory.createTitledBorder( "Interrupt" ) );
    gbcCPU.gridx++;
    panelCPU.add( panelInt, gbcCPU );

    GridBagConstraints gbcInt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 2, 2, 2, 2 ),
						0, 0 );
    this.labelIntMode = new JLabel( "IM:" );
    panelInt.add( this.labelIntMode, gbcInt );

    this.spinnerIntMode = new JSpinner( new SpinnerNumberModel( 0, 0, 2, 1 ) );
    gbcInt.insets.left  = 0;
    gbcInt.gridx++;
    panelInt.add( this.spinnerIntMode, gbcInt );

    this.labelRegI     = new JLabel( "IR:" );
    gbcInt.insets.left = 5;
    gbcInt.gridx++;
    panelInt.add( this.labelRegI, gbcInt );

    this.fldRegI = new JTextField( 3 );
    this.fldRegI.addActionListener( this );
    this.fldRegI.addFocusListener( this );
    this.fldRegI.setEditable( false );
    gbcInt.insets.left = 0;
    gbcInt.gridx++;
    panelInt.add( this.fldRegI, gbcInt );

    this.btnIFF1       = createFlagField( "IFF1" );
    gbcInt.insets.left = 5;
    gbcInt.gridx++;
    panelInt.add( this.btnIFF1, gbcInt );

    this.btnIFF2 = createFlagField( "IFF2" );
    gbcInt.gridx++;
    panelInt.add( this.btnIFF2, gbcInt );


    // Bereich Register
    JPanel panelReg = new JPanel( new GridBagLayout() );
    panelReg.setBorder( BorderFactory.createTitledBorder( "Register" ) );
    gbcCPU.fill      = GridBagConstraints.BOTH;
    gbcCPU.weighty   = 1.0;
    gbcCPU.gridwidth = 2;
    gbcCPU.gridx     = 0;
    gbcCPU.gridy++;
    panelCPU.add( panelReg, gbcCPU );

    GridBagConstraints gbcReg = new GridBagConstraints(
						1, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 2, 2, 2, 2 ),
						0, 0 );

    // Ueberschriften
    panelReg.add( new JLabel( "Hex" ), gbcReg );
    gbcReg.gridwidth = 2;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "ASCII" ), gbcReg );
    gbcReg.gridwidth = 1;
    gbcReg.gridx += 2;
    panelReg.add( new JLabel( "Zeigt im Speicher auf" ), gbcReg );
    gbcReg.gridx += 2;
    panelReg.add( new JLabel( "Hex" ), gbcReg );


    // Register AF
    gbcReg.anchor = GridBagConstraints.WEST;
    gbcReg.gridx  = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "AF:" ), gbcReg );

    this.fldRegAF = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegAF, gbcReg );

    this.fldRegAsciiA = new JTextField( 2 );
    this.fldRegAsciiA.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiA, gbcReg );


    // Register AF2
    gbcReg.gridx += 3;
    panelReg.add( new JLabel( "AF\':" ), gbcReg );

    this.fldRegAF2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegAF2, gbcReg );


    // Register BC
    gbcReg.gridx  = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "BC:" ), gbcReg );

    this.fldRegBC = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegBC, gbcReg );

    this.fldRegAsciiB = new JTextField( 2 );
    this.fldRegAsciiB.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiB, gbcReg );

    this.fldRegAsciiC = new JTextField( 2 );
    this.fldRegAsciiC.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiC, gbcReg );

    this.fldMemBC = new JTextField();
    this.fldMemBC.setEditable( false );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 1.0;
    gbcReg.gridx++;
    panelReg.add( this.fldMemBC, gbcReg );


    // Register BC2
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "BC\':" ), gbcReg );

    this.fldRegBC2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegBC2, gbcReg );


    // Register DE
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "DE:" ), gbcReg );

    this.fldRegDE = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegDE, gbcReg );

    this.fldRegAsciiD = new JTextField( 2 );
    this.fldRegAsciiD.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiD, gbcReg );

    this.fldRegAsciiE = new JTextField( 2 );
    this.fldRegAsciiE.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiE, gbcReg );

    this.fldMemDE = new JTextField();
    this.fldMemDE.setEditable( false );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 1.0;
    gbcReg.gridx++;
    panelReg.add( this.fldMemDE, gbcReg );


    // Register DE2
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "DE\':" ), gbcReg );

    this.fldRegDE2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegDE2, gbcReg );


    // Register HL
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "HL:" ), gbcReg );

    this.fldRegHL = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegHL, gbcReg );

    this.fldRegAsciiH = new JTextField( 2 );
    this.fldRegAsciiH.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiH, gbcReg );

    this.fldRegAsciiL = new JTextField( 2 );
    this.fldRegAsciiL.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiL, gbcReg );

    this.fldMemHL = new JTextField();
    this.fldMemHL.setEditable( false );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 1.0;
    gbcReg.gridx++;
    panelReg.add( this.fldMemHL, gbcReg );


    // Register HL2
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "HL\':" ), gbcReg );

    this.fldRegHL2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegHL2, gbcReg );


    // Register IX
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "IX:" ), gbcReg );

    this.fldRegIX = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegIX, gbcReg );

    this.fldRegAsciiIXH = new JTextField( 2 );
    this.fldRegAsciiIXH.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIXH, gbcReg );

    this.fldRegAsciiIXL = new JTextField( 2 );
    this.fldRegAsciiIXL.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIXL, gbcReg );

    this.fldMemIX = new JTextField();
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
    panelReg.add( new JLabel( "IY:" ), gbcReg );

    this.fldRegIY = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegIY, gbcReg );

    this.fldRegAsciiIYH = new JTextField( 2 );
    this.fldRegAsciiIYH.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIYH, gbcReg );

    this.fldRegAsciiIYL = new JTextField( 2 );
    this.fldRegAsciiIYL.setEditable( false );
    gbcReg.gridx++;
    panelReg.add( this.fldRegAsciiIYL, gbcReg );

    this.fldMemIY = new JTextField();
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
    panelReg.add( new JLabel( "SP:" ), gbcReg );

    this.fldRegSP = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegSP, gbcReg );

    this.fldMemSP = new JTextField();
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
    panelReg.add( new JLabel( "PC:" ), gbcReg );

    this.fldRegPC = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegPC, gbcReg );


    // T-States
    JPanel panelTStates = new JPanel( new GridBagLayout() );
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

    panelTStates.add( new JLabel( "Taktzyklen:" ), gbcTStates );

    this.fldTStates = new JTextField();
    this.fldTStates.setEditable( false );
    gbcTStates.fill        = GridBagConstraints.HORIZONTAL;
    gbcTStates.weightx     = 1.0;
    gbcTStates.insets.left = 5;
    gbcTStates.gridx++;
    panelTStates.add( this.fldTStates, gbcTStates );

    this.btnResetTStates = new JButton( "Zur\u00FCcksetzen" );
    this.btnResetTStates.addActionListener( this );
    gbcTStates.fill    = GridBagConstraints.NONE;
    gbcTStates.weightx = 0.0;
    gbcTStates.gridx++;
    panelTStates.add( this.btnResetTStates, gbcTStates );


    // Programmcode-Anzeige
    this.fldMemPC = new JTextArea( DEFAULT_FLD_MEM_PC_ROWS, 38 );
    this.fldMemPC.setEditable( false );
    this.fldMemPC.setPreferredSize( new Dimension( 1, 1 ) );
    this.fldMemPC.addMouseListener( this );
    this.spMemPC     = new JScrollPane( this.fldMemPC );
    gbcReg.anchor    = GridBagConstraints.WEST;
    gbcReg.fill      = GridBagConstraints.BOTH;
    gbcReg.weightx   = 1.0;
    gbcReg.weighty   = 1.0;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx     = 1;
    gbcReg.gridy++;
    panelReg.add( this.spMemPC, gbcReg );

    Font font = this.fldMemPC.getFont();
    if( font != null ) {
      font = new Font( Font.MONOSPACED, font.getStyle(), font.getSize() );
    } else {
      font = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    }
    this.fldMemPC.setFont( font );

    // Halte-/Log-Punkte
    JPanel panelBreak = new JPanel( new GridBagLayout() );
    panelBreak.setBorder(
	BorderFactory.createTitledBorder( "Halte-/Log-Punkte auf..." ) );
    gbcCPU.fill       = GridBagConstraints.BOTH;
    gbcCPU.weightx    = 1.0;
    gbcCPU.gridheight = 2;
    gbcCPU.gridy      = 0;
    gbcCPU.gridx      = 2;
    panelCPU.add( panelBreak, gbcCPU );

    GridBagConstraints gbcBreak = new GridBagConstraints(
						0, 0,
						1, 2,
						0.33, 1.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.BOTH,
						new Insets( 2, 2, 2, 2 ),
						0, 0 );

    JPanel panelBreakPC = new JPanel( new BorderLayout( 0, 0 ) );
    panelBreakPC.setBorder(
		BorderFactory.createTitledBorder( "Programmadresse" ) );
    panelBreak.add( panelBreakPC, gbcBreak );
    createBreakpointFields( panelBreakPC, BP_PC_IDX );

    JPanel panelBreakMem = new JPanel( new BorderLayout( 0, 0 ) );
    panelBreakMem.setBorder(
		BorderFactory.createTitledBorder( "Speicher" ) );
    gbcBreak.gridx++;
    panelBreak.add( panelBreakMem, gbcBreak );
    createBreakpointFields( panelBreakMem, BP_MEMORY_IDX );

    JPanel panelBreakIO = new JPanel( new BorderLayout( 0, 0 ) );
    panelBreakIO.setBorder(
		BorderFactory.createTitledBorder( "Eingabetor" ) );
    gbcBreak.weighty    = 0.5;
    gbcBreak.gridheight = 1;
    gbcBreak.gridx++;
    panelBreak.add( panelBreakIO, gbcBreak );
    createBreakpointFields( panelBreakIO, BP_INPUT_IDX );

    JPanel panelBreakReg = new JPanel( new BorderLayout( 0, 0 ) );
    panelBreakReg.setBorder(
		BorderFactory.createTitledBorder( "Ausgabetor" ) );
    gbcBreak.gridy++;
    panelBreak.add( panelBreakReg, gbcBreak );
    createBreakpointFields( panelBreakReg, BP_OUTPUT_IDX );

    JPanel panelBreakInt = new JPanel( new BorderLayout( 0, 0 ) );
    panelBreakInt.setBorder(
		BorderFactory.createTitledBorder( "Interrupt-Quelle" ) );
    gbcBreak.fill      = GridBagConstraints.BOTH;
    gbcBreak.weightx   = 1.0;
    gbcBreak.weighty   = 0.0;
    gbcBreak.gridwidth = GridBagConstraints.REMAINDER;
    gbcBreak.gridx = 0;
    gbcBreak.gridy++;
    panelBreak.add( panelBreakInt, gbcBreak );
    createBreakpointFields( panelBreakInt, BP_INTERRUPT_IDX );


    // Tab Log-Meldungen
    this.listModelLog = new DefaultListModel<>();
    this.listLog      = new JList<>( this.listModelLog );
    this.listLog.setDragEnabled( false );
    this.listLog.setLayoutOrientation( JList.VERTICAL );
    this.listLog.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.listLog.addListSelectionListener( this );
    this.listLog.addKeyListener( this );
    this.listLog.addMouseListener( this );

    JScrollPane spLog = new JScrollPane( this.listLog );
    spLog.addMouseListener( this );

    this.tabbedPane.addTab( "Log-Meldungen", spLog );


    // Tab Variablen
    this.tableModelVar = new VarTableModel();
    this.tableVar      = new JTable( this.tableModelVar );
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
      updVarActionsEnabled();
    }
    EmuUtil.setTableColWidths( this.tableVar, 120, 90, 90, 200, 200 );
    this.tableVar.addKeyListener( this );
    this.tableVar.addMouseListener( this );
    this.tabbedPane.addTab( "Variablen", new JScrollPane( this.tableVar ) );


    // Tab Interrupt-Quellen
    JPanel panelIntSrc = new JPanel( new BorderLayout( 5, 5 ) );
    this.tabbedPane.addTab( "Interrupt-Quellen", panelIntSrc );

    this.listIntSrc = new JList<>();
    this.listIntSrc.setDragEnabled( false );
    this.listIntSrc.setLayoutOrientation( JList.VERTICAL );
    this.listIntSrc.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.listIntSrc.setVisibleRowCount( 4 );
    if( this.interruptSources != null ) {
      this.listIntSrc.setListData( this.interruptSources );
    }
    this.listIntSrc.addListSelectionListener( this );
    panelIntSrc.add( new JScrollPane( this.listIntSrc ), BorderLayout.NORTH );

    this.fldIntSrc = new JEditorPane();
    this.fldIntSrc.setEditable( false );
    panelIntSrc.add( new JScrollPane( this.fldIntSrc ), BorderLayout.CENTER );


    // Tab Sonstiges
    this.fldEtc = new JEditorPane()
		{
		  @Override
		  public Dimension getPreferredScrollableViewportSize()
		  {
		    return new Dimension( 1, 1 );
		  }
		};
    this.fldEtc.setEditable( false );
    this.tabbedPane.addTab( "Sonstiges", new JScrollPane( this.fldEtc ) );


    // Statuszeile
    JPanel panelStatus = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    add( panelStatus, BorderLayout.SOUTH );

    this.labelStatus = new JLabel( "Bereit" );
    panelStatus.add( this.labelStatus );


    // Listener
    this.tabbedPane.addChangeListener( this );


    // sonstiges
    this.timerForClear = new javax.swing.Timer( 300, this );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );
    this.fldMemPC.setRows( 0 );
    this.fldMemPC.setColumns( 0 );
    this.cpu.addStatusListener( this );
    updDebuggerInternal( null, null );
    for( int i = 0; i < BP_GROUP_CNT; i++ ) {
      updBreakpointList( i, null );
    }
    updBreakpointActionsEnabled();
    updLogActionsEnabled();
    addComponentListener( this );
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


  public void doDebugBreakPCAdd( int presetAddr )
  {
    if( this.bpDlg == null ) {
      this.bpDlg = new PCBreakpointDlg( this, null, presetAddr );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_PC_IDX, bp );
      }
      this.bpDlg = null;
    }
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
			ToolsUtil.readLabels( reader ),
			options.getSuppressRecreateRemovedLabels(),
			options.getRemoveObsoleteLabels(),
			options.getCaseSensitive() );
	}
	finally {
	  EmuUtil.closeSilent( reader );
	}
      }
      if( state ) {
	this.labelImportOptions = options;
	this.mnuFileBreakImportAgain.setEnabled( true );
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
		boolean              suppressRecreate,
		boolean              caseSensitive )
  {
    importLabels( labels, suppressRecreate, true, caseSensitive );
  }


	/* --- Methoden fuer ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.tabbedPane ) {
      updBreakpointActionsEnabled();
      updLogActionsEnabled();
      updVarActionsEnabled();
    }
  }


	/* --- Methoden fuer ComponentListener --- */

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


	/* --- Methoden fuer FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    // empty
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    Component c = e.getComponent();
    if( c != null )
      fieldValueChanged( c );
  }


	/* --- Methoden fuer ListSelectionListener --- */

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
    }
  }


	/* --- Methoden fuer Z80StatusListener --- */

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
  public boolean applySettings( Properties props, boolean resizable )
  {
    if( props != null ) {
      setMaxLogCnt( props.getProperty(
			getSettingsPrefix() + "max_log_count" ) );
    }
    updIntSrcFields();
    updEtcField();
    return super.applySettings( props, resizable );
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
      if( src == this.mnuFileBreakImport ) {
	rv = true;
	doBreakpointImport( true );
      }
      else if( src == this.mnuFileBreakImport ) {
	rv = true;
	doBreakpointImport( false );
      }
      else if( src == this.mnuFileBreakRemoveImported ) {
	rv = true;
	doBreakpointRemoveImported();
      }
      else if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( (src == this.mnuDebugRun)
	       || (src == this.btnRun) )
      {
	rv = true;
	doDebugRun();
      }
      else if( src == this.btnWalk )
      {
	rv = true;
	this.popupWalk.show( this.btnWalk, 0, this.btnWalk.getHeight() );
      }
      else if( (src == this.mnuDebugWalk100)
	       || (src == this.popupWalk100) )
      {
	rv = true;
	doDebugWalk( 100 );
      }
      else if( (src == this.mnuDebugWalk300)
	       || (src == this.popupWalk300) )
      {
	rv = true;
	doDebugWalk( 300 );
      }
      else if( (src == this.mnuDebugWalk500)
	       || (src == this.popupWalk500) )
      {
	rv = true;
	doDebugWalk( 500 );
      }
      else if( (src == this.mnuDebugStop)
	       || (src == this.btnStop) )
      {
	rv = true;
	doDebugStop();
      }
      else if( (src == this.mnuDebugStepOver)
	       || (src == this.btnStepOver) )
      {
	rv = true;
	doDebugStepOver();
      }
      else if( (src == this.mnuDebugStepInto)
	       || (src == this.btnStepInto) )
      {
	rv = true;
	doDebugStepInto();
      }
      else if( (src == this.mnuDebugStepToRET)
	       || (src == this.btnStepToRET) )
      {
	rv = true;
	doDebugStepToRET();
      }
      else if( src == this.mnuDebugBreakInputAdd ) {
	rv = true;
	doDebugBreakInputAdd();
      }
      else if( src == this.mnuDebugBreakInterruptAdd ) {
	rv = true;
	doDebugBreakInterruptAdd();
      }
      else if( src == this.mnuDebugBreakMemoryAdd ) {
	rv = true;
	doDebugBreakMemoryAdd();
      }
      else if( src == this.mnuDebugBreakOutputAdd ) {
	rv = true;
	doDebugBreakOutputAdd();
      }
      else if( src == this.mnuDebugBreakPCAdd ) {
	rv = true;
	doDebugBreakPCAdd( -1 );
      }
      else if( src == this.mnuDebugBreakEdit ) {
	rv = true;
	doDebugBreakEdit();
      }
      else if( src == this.mnuDebugBreakRemove ) {
	rv = true;
	doDebugBreakRemove();
      }
      else if( src == this.mnuDebugBreakRemoveAll ) {
	rv = true;
	doDebugBreakRemoveAll();
      }
      else if( src == this.mnuDebugBreakEnableStop ) {
	rv = true;
	doDebugBreakSetStopEnabled( true );
      }
      else if( src == this.mnuDebugBreakDisableStop ) {
	rv = true;
	doDebugBreakSetStopEnabled( false );
      }
      else if( src == this.mnuDebugBreakEnableStopAll ) {
	rv = true;
	doDebugBreakSetAllStopEnabled( true );
      }
      else if( src == this.mnuDebugBreakDisableStopAll ) {
	rv = true;
	doDebugBreakSetAllStopEnabled( false );
      }
      else if( src == this.mnuDebugBreakEnableLog ) {
	rv = true;
	doDebugBreakSetLogEnabled( true );
      }
      else if( src == this.mnuDebugBreakDisableLog ) {
	rv = true;
	doDebugBreakSetLogEnabled( false );
      }
      else if( src == this.mnuDebugBreakEnableLogAll ) {
	rv = true;
	doDebugBreakSetAllLogEnabled( true );
      }
      else if( src == this.mnuDebugBreakDisableLogAll ) {
	rv = true;
	doDebugBreakSetAllLogEnabled( false );
      }
      else if( src == this.mnuDebugTracer ) {
	rv = true;
	doDebugTracer();
      }
      else if( (src == this.mnuVarAdd)
	       || (src == this.popupVarAdd) )
      {
	rv = true;
	doVarAdd();
      }
      else if( (src == this.mnuVarEdit)
	       || (src == this.popupVarEdit)
	       || (src == this.tableVar) )
      {
	rv = true;
	doVarEdit();
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
	HelpFrm.open( HELP_PAGE );
      }
      else if( src == this.popupBreakAdd ) {
	rv = true;
	doDebugBreakAdd( this.popupBreakGroupIdx );
      }
      else if( src == this.popupBreakRemove ) {
	rv = true;
	doDebugBreakRemove( this.popupBreakGroupIdx );
      }
      else if( src == this.popupBreakRemoveAll ) {
	rv = true;
	doDebugBreakRemoveAll( this.popupBreakGroupIdx );
      }
      else if( src == this.popupBreakEnableStop ) {
	rv = true;
	doDebugBreakSetStopEnabled( this.popupBreakGroupIdx, true );
      }
      else if( src == this.popupBreakDisableStop ) {
	rv = true;
	doDebugBreakSetStopEnabled( this.popupBreakGroupIdx, false );
      }
      else if( src == this.popupBreakEnableStopAll ) {
	rv = true;
	doDebugBreakSetAllStopEnabled( this.popupBreakGroupIdx, true );
      }
      else if( src == this.popupBreakDisableStopAll ) {
	rv = true;
	doDebugBreakSetAllStopEnabled( this.popupBreakGroupIdx, false );
      }
      else if( src == this.popupBreakEnableLog ) {
	rv = true;
	doDebugBreakSetLogEnabled( this.popupBreakGroupIdx, true );
      }
      else if( src == this.popupBreakDisableLog ) {
	rv = true;
	doDebugBreakSetLogEnabled( this.popupBreakGroupIdx, false );
      }
      else if( src == this.popupBreakEnableLogAll ) {
	rv = true;
	doDebugBreakSetAllLogEnabled( this.popupBreakGroupIdx, true );
      }
      else if( src == this.popupBreakDisableLogAll ) {
	rv = true;
	doDebugBreakSetAllLogEnabled( this.popupBreakGroupIdx, false );
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
	this.cpu.resetSpeed();
	updFieldsTStates();
      }
      else if( src == this.popupMemPCCopy ) {
	rv = true;
	this.fldMemPC.copy();
      }
      else if( src == this.popupMemPCBreak ) {
	rv = true;
	if( this.memPCClickAddr >= 0 ) {
	  doDebugBreakPCAdd( this.memPCClickAddr );
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
	  doDebugBreakRemove();
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
  public void lookAndFeelChanged()
  {
    if( this.popupBreak != null )
      SwingUtilities.updateComponentTreeUI( this.popupBreak );
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    super.putSettingsTo( props );
    if( props != null ) {
      props.setProperty(
			getSettingsPrefix() + "max_log_count",
			String.valueOf( this.maxLogCnt ) );
    }
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
  protected boolean showPopup( MouseEvent e )
  {
    boolean rv = false;
    if( e != null ) {
      Component c = e.getComponent();
      if( c != null ) {
	if( c == this.fldMemPC ) {
	  this.memPCClickAddr = ToolsUtil.getReassAddr(
						this.fldMemPC,
						e.getPoint() );
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
		this.popupBreakRemoveAll.setEnabled( !model.isEmpty() );
	      } else {
		this.popupBreakRemoveAll.setEnabled( false );
	      }
	      this.popupBreakEnableStopAll.setEnabled( anyStopDisabled );
	      this.popupBreakDisableStopAll.setEnabled( anyStopEnabled );
	      this.popupBreakEnableLogAll.setEnabled( anyLogDisabled );
	      this.popupBreakDisableLogAll.setEnabled( anyLogEnabled );

	      boolean selected        = false;
	      boolean selStopEnabled  = false;
	      boolean selStopDisabled = false;
	      boolean selLogEnabled   = false;
	      boolean selLogDisabled  = false;
	      if( model != null ) {
		int[] rows  = this.bpLists[ idx ].getSelectedIndices();
		if( rows != null ) {
		  for( int i = 0; i < rows.length; i++ ) {
		    int row = rows[ i ];
		    if( (row >= 0) && (row < model.size()) ) {
		      selected = true;
		      if( model.get( row ).isStopEnabled() ) {
			selStopEnabled = true;
		      } else {
			selStopDisabled = true;
		      }
		      if( model.get( row ).isLogEnabled() ) {
			selLogEnabled = true;
		      } else {
			selLogDisabled = true;
		      }
		    }
		    if( selStopEnabled && selStopDisabled
			&& selLogEnabled && selLogDisabled )
		    {
		      break;
		    }
		  }
		}
	      }
	      this.popupBreakRemove.setEnabled( selected );
	      this.popupBreakEnableStop.setEnabled( selStopDisabled );
	      this.popupBreakDisableStop.setEnabled( selStopEnabled );
	      this.popupBreakEnableLog.setEnabled( selLogDisabled );
	      this.popupBreakDisableLog.setEnabled( selLogEnabled );
	      this.popupBreakGroupIdx = idx;
	      this.popupBreak.show( c, e.getX(), e.getY() );
	      rv = true;
	    }
	  } else if( tabIdx == TAB_IDX_VAR ) {
	    boolean hasEntries = !this.tableModelVar.isEmpty();
	    int     nSelected  = this.tableVar.getSelectedRowCount();
	    this.popupVarEdit.setEnabled( nSelected == 1 );
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


	/* --- Aktionen --- */

  private void doBreakpointImport( boolean interactive )
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


  private void doBreakpointRemoveImported()
  {
    BreakpointListModel model = this.bpModels[ BP_PC_IDX ];
    if( model != null ) {
      int size = model.size();
      if( size > 0 ) {
	boolean changed = false;
	for( int i = size - 1; i >= 0; --i ) {
	  if( model.get( i ) instanceof LabelBreakpoint ) {
	    model.remove( i );
	    changed = true;
	  }
	}
	if( changed ) {
	  updBreakpointList( BP_PC_IDX, null );
	}
      }
    }
  }


  private void doDebugRun()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_RUN );
  }


  private void doDebugWalk( int millis )
  {
    this.walkMillis = millis;
    this.labelStatus.setText( "Programm wird langsam ausgef\u00FChrt..." );
    this.cpu.fireAction( Z80CPU.Action.DEBUG_WALK );
  }


  private void doDebugStop()
  {
    if( this.walkMillis > 0 ) {
      this.walkMillis = 0;
      fireUpdDebugger( null, null );
    }
    this.labelStatus.setText( "Programmausf\u00FChrung wird angehalten..." );
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STOP );
  }


  private void doDebugStepOver()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_OVER );
  }


  private void doDebugStepInto()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_INTO );
  }


  private void doDebugStepToRET()
  {
    this.walkMillis = 0;
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_TO_RET );
  }


  private void doDebugBreakAdd( int bpGroupIdx )
  {
    switch( bpGroupIdx ) {
      case BP_PC_IDX:
	doDebugBreakPCAdd( -1 );
	break;
      case BP_MEMORY_IDX:
	doDebugBreakMemoryAdd();
	break;
      case BP_INPUT_IDX:
	doDebugBreakInputAdd();
	break;
      case BP_OUTPUT_IDX:
	doDebugBreakOutputAdd();
	break;
      case BP_INTERRUPT_IDX:
	doDebugBreakInterruptAdd();
	break;
    }
  }


  private void doDebugBreakEdit()
  {
    int grpIdx = -1;
    if( this.tabbedPane.getSelectedIndex() == TAB_IDX_CPU ) {
      for( int i = 0; i < BP_GROUP_CNT; i++ ) {
	if( this.bpModels[ i ] != null ) {
	  int[] rows = this.bpLists[ i ].getSelectedIndices();
	  if( rows != null ) {
	    if( rows.length > 0 ) {
	      if( grpIdx < 0 ) {
		grpIdx = i;
	      } else {
		// Breakpoints in mehrere Gruppen ausgewaehlt -> Abbruch
		grpIdx = -1;
		break;
	      }
	    }
	  }
	}
      }
    }
    if( grpIdx >= 0 ) {
      editBreakpoint( grpIdx );
    }
  }


  private void doDebugBreakInputAdd()
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


  private void doDebugBreakInterruptAdd()
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


  private void doDebugBreakMemoryAdd()
  {
    if( this.bpDlg == null ) {
      this.bpDlg = new MemoryBreakpointDlg( this, null );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_MEMORY_IDX, bp );
      }
      this.bpDlg = null;
    }
  }


  private void doDebugBreakOutputAdd()
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


  private void doDebugBreakRemove()
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakRemove( i );
  }


  private void doDebugBreakRemove( int bpGroupIdx )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      BreakpointListModel model = this.bpModels[ bpGroupIdx ];
      if( model != null ) {
	int[] rows = this.bpLists[ bpGroupIdx ].getSelectedIndices();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    for( int i = rows.length - 1; i >= 0; --i ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < model.size()) ) {
		registerRemove( model.getElementAt( row ) );
		model.remove( row );
	      }
	    }
	    updBreakpointList( bpGroupIdx, null );
	    updBreakpointActionsEnabled();
	  }
	}
      }
    }
  }


  private void doDebugBreakRemoveAll()
  {
    if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Halte-/Log-Punkte entfernen?" ) )
    {
      for( int i = 0; i < this.bpModels.length; i++ ) {
	removeAllBreakpointsFromModel( this.bpModels[ i ] );
	updBreakpointList( i, null );
      }
      updBreakpointActionsEnabled();
    }
  }


  private void doDebugBreakRemoveAll( int bpGroupIdx )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Halte-/Log-Punkte"
			+ " dieser Gruppe entfernen?" ) )
      {
	removeAllBreakpointsFromModel( this.bpModels[ bpGroupIdx ] );
	updBreakpointList( bpGroupIdx, null );
	updBreakpointActionsEnabled();
      }
    }
  }


  private void doDebugBreakSetStopEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakSetStopEnabled( i, state );
  }


  private void doDebugBreakSetStopEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( this.bpModels[ bpGroupIdx ] != null ) {
	int[] rows = this.bpLists[ bpGroupIdx ].getSelectedIndices();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    for( int i = 0; i < rows.length; i++ ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < this.bpModels[ bpGroupIdx ].size()) ) {
		AbstractBreakpoint bp = this.bpModels[ bpGroupIdx].get( row );
		bp.setStopEnabled( state );
	      }
	    }
	    updBreakpointList( bpGroupIdx, null );
	    updBreakpointActionsEnabled();
	    EmuUtil.fireSelectRows( this.bpLists[ bpGroupIdx ], rows );
	  }
	}
      }
    }
  }


  private void doDebugBreakSetAllStopEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakSetAllStopEnabled( i, state );
  }


  private void doDebugBreakSetAllStopEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( this.bpModels[ bpGroupIdx ] != null ) {
	for( int i = 0; i < this.bpModels[ bpGroupIdx ].size(); i++ ) {
	  this.bpModels[ bpGroupIdx ].get( i ).setStopEnabled( state );
	}
	updBreakpointList( bpGroupIdx, null );
	updBreakpointActionsEnabled();
      }
    }
  }


  private void doDebugBreakSetLogEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakSetLogEnabled( i, state );
  }


  private void doDebugBreakSetLogEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( this.bpModels[ bpGroupIdx ] != null ) {
	int[] rows = this.bpLists[ bpGroupIdx ].getSelectedIndices();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    for( int i = 0; i < rows.length; i++ ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < this.bpModels[ bpGroupIdx ].size()) ) {
		AbstractBreakpoint bp = this.bpModels[ bpGroupIdx].get( row );
		bp.setLogEnabled( state );
	      }
	    }
	    updBreakpointList( bpGroupIdx, null );
	    updBreakpointActionsEnabled();
	    EmuUtil.fireSelectRows( this.bpLists[ bpGroupIdx ], rows );
	  }
	}
      }
    }
  }


  private void doDebugBreakSetAllLogEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakSetAllLogEnabled( i, state );
  }


  private void doDebugBreakSetAllLogEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( this.bpModels[ bpGroupIdx ] != null ) {
	for( int i = 0; i < this.bpModels[ bpGroupIdx ].size(); i++ ) {
	  this.bpModels[ bpGroupIdx ].get( i ).setLogEnabled( state );
	}
	updBreakpointList( bpGroupIdx, null );
	updBreakpointActionsEnabled();
      }
    }
  }


  private void doDebugTracer()
  {
    if( this.mnuDebugTracer.isSelected() ) {
      // Menueintrag ersteinmal ausschalten, falls ein Fehler
      // auftritt oder die Aktion abgebrochen wird.
      this.mnuDebugTracer.setSelected( false );

      // voreingestellte Aktion: neue Datei anlegen
      int     action = 1;
      boolean append = false;
      File    file   = null;

      // an alte Datei anhaengen oder neue Datei anlegen?
      if( this.lastTraceFile != null ) {
	String[] options = {"Anh\u00E4ngen", "Neue Datei", "Abbrechen"};

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
	file = EmuUtil.showFileSaveDlg(
			this,
			"Befehlsaufzeichnung speichern",
			Main.getLastDirFile( Main.FILE_GROUP_DEBUG ),
			EmuUtil.getTextFileFilter() );
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
	  this.mnuDebugTracer.setSelected( true );
	  this.lastTraceFile = file;
	  Main.setLastFile( file, Main.FILE_GROUP_DEBUG );
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
		buf.append( (char) '\n' );
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
	updLogActionsEnabled();
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
	updLogActionsEnabled();
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


  private void doVarAdd()
  {
    VarData varData = VarDataDlg.showNewVarDlg( this );
    if( varData != null ) {
      final int row = this.tableModelVar.addRow( varData );
      if( row >= 0 ) {
	String varName = varData.getName();
	if( varName != null ) {
	  this.removedImportedVarNames.remove( varName );
	}
	if( this.tableModelVar.getValuesEnabled() ) {
	  varData.update( this.memory );
	  this.tableModelVar.fireTableRowsUpdated( row, row );
	}
	int viewRow = this.tableVar.convertRowIndexToView( row );
	if( viewRow >= 0 ) {
	  EmuUtil.fireSelectRow( this.tableVar, viewRow );
	}
      }
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
	      if( varName != null ) {
		this.removedImportedVarNames.remove( varName );
	      }
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
	      }
	    }
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
	  int row = rows[ i ];
	  registerRemove( this.tableModelVar.getRow( row ) );
	  this.tableModelVar.remove( row );
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
	int nVars = this.tableModelVar.getRowCount();
	for( int i = 0; i < nVars; i++ ) {
	  registerRemove( this.tableModelVar.getRow( i ) );
	}
	this.tableModelVar.clear();
	updVarActionsEnabled();
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

  private void addBreakpoint( int bpGroupIdx, AbstractBreakpoint bpToAdd )
  {
    try {
      if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
	BreakpointListModel model = this.bpModels[ bpGroupIdx ];
	if( model != null ) {
	  int idx = Collections.binarySearch( model, bpToAdd );
	  if( idx < 0 ) {
	    idx = -(idx + 1);
	    if( idx < model.size() ) {
	      model.add( idx, bpToAdd );
	    } else {
	      model.add( bpToAdd );
	    }
	  }
	  updBreakpointList( bpGroupIdx, bpToAdd );
	}
	updBreakpointActionsEnabled();
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
    updLogActionsEnabled();
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


  private void createBreakpointFields(
				Container container,
				int       bpGroupIdx )
  {
    BreakpointListModel model   = new BreakpointListModel();
    this.bpModels[ bpGroupIdx ] = model;

    BreakpointList list = new BreakpointList( model );
    list.setVisibleRowCount( 4 );
    list.setPrototypeCellValue(
		new PCBreakpoint(
			this,
			0xFFFF,
			"HL",
			0xFFFF,
			"=",
			0xFFFF,
			false ) );
    list.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    list.setCellRenderer( new BreakpointCellRenderer( this ) );
    this.bpLists[ bpGroupIdx ] = list;

    JScrollPane sp = new JScrollPane( list );
    sp.addMouseListener( this );
    this.bpScrollPanes[ bpGroupIdx ] = sp;
    container.add( sp );

    list.addListSelectionListener( this );
    list.addKeyListener( this );
    list.addMouseListener( this );
  }


  private AbstractButton createFlagField( String text )
  {
    AbstractButton btn = new JCheckBox( text );
    btn.setEnabled( false );
    btn.addActionListener( this );
    return btn;
  }


  private JTextField createHexField()
  {
    JTextField fld = new JTextField( 5 );
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
	  if( (row >= 0) && (row < model.size()) ) {
	    AbstractBreakpoint oldBP = model.get( row );
	    if( (oldBP != null) && (this.bpDlg == null) ) {
	      switch( bpGroupIdx ) {
		case BP_PC_IDX:
		  this.bpDlg = new PCBreakpointDlg( this, oldBP, -1 );
		  break;
		case BP_MEMORY_IDX:
		  this.bpDlg = new MemoryBreakpointDlg( this, oldBP );
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


  private boolean importLabels(
			jkcemu.tools.Label[] labels,
			boolean              suppressRecreate,
			boolean              removeObsolete,
			boolean              caseSensitive )
  {
    boolean rv = false;

    // alte importierte Haltepunkte merken
    Map<String,LabelBreakpoint>  labelNameMap = null;
    Map<Integer,LabelBreakpoint> labelAddrMap = null;
    BreakpointListModel          model        = this.bpModels[ BP_PC_IDX ];
    int nBPs = model.size();
    if( nBPs > 0 ) {
      for( int i = nBPs - 1 ; i >= 0; --i ) {
	AbstractBreakpoint bp = model.get( i );
	if( bp != null ) {
	  if( bp instanceof LabelBreakpoint ) {
	    boolean done      = false;
	    String  labelName = ((LabelBreakpoint) bp).getLabelName();
	    if( labelName != null ) {
	      if( !labelName.isEmpty() ) {
		if( !caseSensitive ) {
		  labelName = labelName.toUpperCase();
		}
		if( labelNameMap == null ) {
		  labelNameMap = new HashMap<>( nBPs );
		}
		labelNameMap.put( labelName, (LabelBreakpoint) bp );
		done = true;
	      }
	    }
	    if( !done ) {
	      if( labelAddrMap == null ) {
		labelAddrMap = new HashMap<>( nBPs );
	      }
	      labelAddrMap.put(
			((LabelBreakpoint) bp).getAddress(),
			(LabelBreakpoint) bp );
	    }
	    model.remove( i );
	  }
	}
      }
    }

    // alte importierte Variablen merken
    Map<String,Integer> varRowMap = null;
    int nVars = this.tableModelVar.getRowCount();
    if( nVars > 0 ) {
      varRowMap = new HashMap<>( nVars );
      for( int i = 0; i < nVars; i++ ) {
	VarData varData = this.tableModelVar.getRow( i );
	if( varData != null ) {
	  if( varData.wasImported() ) {
	    String varName = varData.getName();
	    if( varName != null ) {
	      if( !caseSensitive ) {
		varName = varName.toUpperCase();
	      }
	      varRowMap.put( varName, i );
	    }
	  }
	}
      }
    }

    // Marken durch neue ersetzen
    if( labels != null ) {
      for( jkcemu.tools.Label label : labels ) {
	String labelName = label.getLabelName();
	int    varSize   = label.getVarSize();
	if( varSize > 0 ) {
	  if( labelName != null ) {
	    if( !caseSensitive ) {
	      labelName = labelName.toUpperCase();
	    }
	    if( !suppressRecreate
		|| !this.removedImportedVarNames.contains( labelName ) )
	    {
	      Integer row = null;
	      if( varRowMap != null ) {
		row = varRowMap.get( labelName );
	      }
	      if( row != null ) {
		VarData varData = this.tableModelVar.getRow( row.intValue() );
		if( varData != null ) {
		  this.tableModelVar.setRow(
		      row.intValue(),
		      VarData.createWithDefaultType(
					labelName,
					label.intValue(),
					varSize,
					true ) );
		} else {
		  row = null;
		}
		varRowMap.remove( labelName );
	      }
	      if( row == null ) {
		this.tableModelVar.addRow(
			VarData.createWithDefaultType(
					labelName,
					label.intValue(),
					varSize,
					true ) );
	      }
	      this.removedImportedVarNames.remove( labelName );
	      rv = true;
	    }
	  }
	} else {
	  if( labelName != null ) {
	    if( !caseSensitive ) {
	      labelName = labelName.toUpperCase();
	    }
	    if( !suppressRecreate
		|| !this.removedImportedBreakNames.contains( labelName ) )
	    {
	      LabelBreakpoint oldBP = null;
	      if( labelNameMap != null ) {
		oldBP = labelNameMap.remove( labelName );
	      }
	      model.add( new LabelBreakpoint(
					this,
					labelName,
					label.intValue(),
					oldBP,
					true ) );
	      this.removedImportedBreakNames.remove( labelName );
	      rv = true;
	    }
	  } else {
	    Integer addr = label.intValue();
	    if( !suppressRecreate
		|| !this.removedImportedBreakAddrs.contains( addr ) )
	    {
	      LabelBreakpoint oldBP = null;
	      if( labelAddrMap != null ) {
		oldBP = labelAddrMap.remove( addr );
	      }
	      model.add( new LabelBreakpoint(
					this,
					null,
					label.intValue(),
					oldBP,
					true ) );
	      this.removedImportedBreakAddrs.remove( addr );
	      rv = true;
	    }
	  }
	}
      }
    }
    if( rv ) {
      if( removeObsolete ) {
	if( varRowMap != null ) {
	  Collection<Integer> c = varRowMap.values();
	  if( c != null ) {
	    int n = c.size();
	    if( n > 0 ) {
	      try {
		Integer[] a = c.toArray( new Integer[ n ] );
		if( a != null ) {
		  Arrays.sort( a );
		  for( int i = a.length - 1; i >= 0; --i ) {
		    this.tableModelVar.remove( a[ i ] );
		  }
		}
	      }
	      catch( ArrayStoreException ex ) {}
	      catch( ClassCastException ex ) {}
	    }
	  }
	}
      } else {
	if( labelNameMap != null ) {
	  Collection<LabelBreakpoint> c = labelNameMap.values();
	  if( c != null ) {
	    model.addAll( c );
	  }
	}
	if( labelAddrMap != null ) {
	  Collection<LabelBreakpoint> c = labelAddrMap.values();
	  if( c != null ) {
	    model.addAll( c );
	  }
	}
      }
      try {
	Collections.sort( model );
      }
      catch( ClassCastException ex ) {}
      this.bpLists[ BP_PC_IDX ].setListData( new AbstractBreakpoint[ 0 ] );
      this.bpLists[ BP_PC_IDX ].setModel( model );
      updBreakpointList( BP_PC_IDX, null );
      this.tableModelVar.update( this.memory );
      if( this.cpu.isPause() ) {
	updFieldsPC();
      }
    }
    return rv;
  }


  private void registerRemove( AbstractBreakpoint bp )
  {
    if( bp != null ) {
      if( bp instanceof PCBreakpoint ) {
	PCBreakpoint pcbp = (PCBreakpoint) bp;
	if( pcbp.wasImported() ) {
	  String labelName = null;
	  if( pcbp instanceof LabelBreakpoint ) {
	    labelName = ((LabelBreakpoint) pcbp).getLabelName();
	    if( labelName != null ) {
	      if( labelName.isEmpty() ) {
		labelName = null;
	      }
	    }
	  }
	  if( labelName != null ) {
	    this.removedImportedBreakNames.add( labelName );
	  } else {
	    this.removedImportedBreakAddrs.add( pcbp.getAddress() );
	  }
	}
      }
    }
  }


  private void registerRemove( VarData varData )
  {
    if( varData != null ) {
      String varName = varData.getName();
      if( varName != null ) {
	if( !varName.isEmpty() ) {
	  this.removedImportedVarNames.add( varName );
	}
      }
    }
  }


  private void removeAllBreakpointsFromModel( BreakpointListModel model )
  {
    if( model != null ) {
      int n = model.getSize();
      for( int k = 0; k < n; k++ ) {
	registerRemove( model.getElementAt( k ) );
      }
      model.clear();
    }
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
    this.mnuDebugRun.setEnabled( false );
    this.mnuDebugWalk100.setEnabled( false );
    this.mnuDebugWalk300.setEnabled( false );
    this.mnuDebugWalk500.setEnabled( false );
    this.mnuDebugStop.setEnabled( false );
    this.mnuDebugStepOver.setEnabled( false );
    this.mnuDebugStepInto.setEnabled( false );
    this.mnuDebugStepToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnWalk.setEnabled( false );
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
    this.mnuDebugRun.setEnabled( false );
    this.mnuDebugWalk100.setEnabled( true );
    this.mnuDebugWalk300.setEnabled( true );
    this.mnuDebugWalk500.setEnabled( true );
    this.mnuDebugStop.setEnabled( true );
    this.mnuDebugStepOver.setEnabled( false );
    this.mnuDebugStepInto.setEnabled( false );
    this.mnuDebugStepToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnWalk.setEnabled( true );
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

    this.mnuDebugRun.setEnabled( true );
    this.mnuDebugWalk100.setEnabled( true );
    this.mnuDebugWalk300.setEnabled( true );
    this.mnuDebugWalk500.setEnabled( true );
    this.mnuDebugStop.setEnabled( walking );
    this.mnuDebugStepOver.setEnabled( stopped );
    this.mnuDebugStepInto.setEnabled( stopped );
    this.mnuDebugStepToRET.setEnabled( stopped );

    this.btnRun.setEnabled( true );
    this.btnWalk.setEnabled( true );
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
    ListModel lm = this.listIntSrc.getModel();
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


  private void updBreakpointActionsEnabled()
  {
    boolean hasEntries      = false;
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
	      if( (row >= 0) && (row < model.size()) ) {
		nSelected++;
		if( model.get( row ).isStopEnabled() ) {
		  selStopEnabled = true;
		} else {
		  selStopDisabled = true;
		}
		if( model.get( row ).isLogEnabled() ) {
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
    }
    this.mnuDebugBreakEnableStop.setEnabled( selStopDisabled );
    this.mnuDebugBreakDisableStop.setEnabled( selStopEnabled );
    this.mnuDebugBreakEnableStopAll.setEnabled( anyStopDisabled );
    this.mnuDebugBreakDisableStopAll.setEnabled( anyStopEnabled );
    this.mnuDebugBreakEnableLog.setEnabled( selLogDisabled );
    this.mnuDebugBreakDisableLog.setEnabled( selLogEnabled );
    this.mnuDebugBreakEnableLogAll.setEnabled( anyLogDisabled );
    this.mnuDebugBreakDisableLogAll.setEnabled( anyLogEnabled );
    this.mnuDebugBreakEdit.setEnabled( nSelected == 1 );
    this.mnuDebugBreakRemove.setEnabled( nSelected > 0 );
    this.mnuDebugBreakRemoveAll.setEnabled( hasEntries );
  }


  private void updBreakpointList(
			int                bpGroupIdx,
			AbstractBreakpoint bpToSelect )
  {
    boolean hasImported = false;
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( (this.bpModels[ bpGroupIdx ] != null)
	  && (this.bpLists[ bpGroupIdx ] != null) )
      {
	this.bpLists[ bpGroupIdx ].setModel( this.bpModels[ bpGroupIdx ] );
	this.bpLists[ bpGroupIdx ].repaint();
	if( bpToSelect != null ) {
	  EmuUtil.fireSelectRow( this.bpLists[ bpGroupIdx ], bpToSelect );
	}
	java.util.List<Z80Breakpoint> list = null;
	for( int i = 0; i < this.bpModels.length; i++ ) {
	  BreakpointListModel model = this.bpModels[ i ];
	  if( model != null ) {
	    int n = model.size();
	    if( n > 0 ) {
	      for( AbstractBreakpoint bp : model ) {
		if( bp.isLogEnabled() || bp.isStopEnabled() ) {
		  if( bp instanceof Z80Breakpoint ) {
		    if( list == null ) {
		      list = new ArrayList<>();
		    }
		    list.add( (Z80Breakpoint) bp );
		  }
		}
		if( bp instanceof LabelBreakpoint ) {
		  hasImported = true;
		}
	      }
	    }
	  }
	}
	this.cpu.setBreakpoints( list != null ?
		list.toArray( new Z80Breakpoint[ list.size() ] )
		: null );
      }
    }
    if( bpGroupIdx == BP_PC_IDX ) {
      this.mnuFileBreakRemoveImported.setEnabled( hasImported );
    }
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
	      if( this.bpModels[ i ] != null ) {
		for( AbstractBreakpoint bp : this.bpModels[ i ] ) {
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
	  if( breakpoint instanceof LabelBreakpoint ) {
	    if( ((LabelBreakpoint) breakpoint).getAddress() == addr ) {
	      String s = ((LabelBreakpoint) breakpoint).getLabelName();
	      if( s != null ) {
		if( s.isEmpty() ) {
		  s = null;
		}
	      }
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
	int n = model.size();
	for( int i = n - 1; i >= 0; --i ) {
	  boolean            done = false;
	  AbstractBreakpoint bp   = model.get( i );
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
    updBreakpointList( BP_INTERRUPT_IDX, null );
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
	ListModel lm = this.listIntSrc.getModel();
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
    boolean isTab      = (this.tabbedPane.getSelectedIndex() == TAB_IDX_VAR);
    boolean hasEntries = !this.tableModelVar.isEmpty();
    boolean selected   = (this.tableVar.getSelectedRow() >= 0);
    this.mnuVarEdit.setEnabled( isTab && selected );
    this.mnuVarRemove.setEnabled( isTab && selected );
    this.mnuVarRemoveAll.setEnabled( isTab && hasEntries );
  }
}
