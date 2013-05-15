/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Debugger-Fenster
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.text.TextUtil;
import jkcemu.tools.*;
import z80emu.*;


public class DebugFrm extends BasicFrm implements
						ComponentListener,
						FocusListener,
						ListSelectionListener,
						Z80StatusListener
{
  private static final int BP_PC_IDX        = 0;
  private static final int BP_MEMORY_IDX    = 1;
  private static final int BP_INPUT_IDX     = 2;
  private static final int BP_OUTPUT_IDX    = 3;
  private static final int BP_INTERRUPT_IDX = 4;
  private static final int BP_GROUP_CNT     = 5;

  private static final String TEXT_STOP
			= "Programmausf\u00FChrung anhalten";
  private static final String TEXT_RUN
			= "Programmausf\u00FChrung bis Haltepunkt fortsetzen";
  private static final String TEXT_STEP_OVER
			= "Einzelschritt \u00FCber Aufruf hinweg";
  private static final String TEXT_STEP_INTO
			= "Einzelschritt in Aufruf hinein";
  private static final String TEXT_STEP_TO_RET
			= "Bis RETURN ausf\u00FChren";

  private Z80CPU                cpu;
  private Z80Memory             memory;
  private AbstractBreakpointDlg bpDlg;
  private BreakpointListModel[] bpModels;
  private Z80InterruptSource[]  interruptSources;
  private Z80InterruptSource    lastSelectedIntSrc;
  private File                  lastBreakpointFile;
  private File                  lastTraceFile;
  private PrintWriter           traceWriter;
  private int                   popupBreakGroupIdx;
  private JMenuItem             mnuFileClose;
  private JMenuItem             mnuFileBreakImportClp;
  private JMenuItem             mnuFileBreakImportFile;
  private JMenuItem             mnuFileBreakRemoveImported;
  private JMenuItem             mnuFileOpenTrace;
  private JMenuItem             mnuDebugRun;
  private JMenuItem             mnuDebugStop;
  private JMenuItem             mnuDebugStepOver;
  private JMenuItem             mnuDebugStepInto;
  private JMenuItem             mnuDebugStepToRET;
  private JMenuItem             mnuDebugBreakInterruptAdd;
  private JMenuItem             mnuDebugBreakInputAdd;
  private JMenuItem             mnuDebugBreakOutputAdd;
  private JMenuItem             mnuDebugBreakMemoryAdd;
  private JMenuItem             mnuDebugBreakPCAdd;
  private JMenuItem             mnuDebugBreakRemove;
  private JMenuItem             mnuDebugBreakRemoveAll;
  private JMenuItem             mnuDebugBreakEnable;
  private JMenuItem             mnuDebugBreakDisable;
  private JMenuItem             mnuDebugBreakEnableAll;
  private JMenuItem             mnuDebugBreakDisableAll;
  private JCheckBoxMenuItem     mnuDebugTracer;
  private JMenuItem             mnuHelpContent;
  private JPopupMenu            mnuPopup;
  private JMenuItem             mnuPopupBreakAdd;
  private JMenuItem             mnuPopupBreakRemove;
  private JMenuItem             mnuPopupBreakRemoveAll;
  private JMenuItem             mnuPopupBreakEnable;
  private JMenuItem             mnuPopupBreakDisable;
  private JMenuItem             mnuPopupBreakEnableAll;
  private JMenuItem             mnuPopupBreakDisableAll;
  private JTabbedPane           tabbedPane;
  private JButton               btnRun;
  private JButton               btnStop;
  private JButton               btnStepOver;
  private JButton               btnStepInto;
  private JButton               btnStepToRET;
  private AbstractButton        btnFlagSign;
  private AbstractButton        btnFlagZero;
  private AbstractButton        btnFlagHalf;
  private AbstractButton        btnFlagPV;
  private AbstractButton        btnFlagN;
  private AbstractButton        btnFlagCarry;
  private AbstractButton        btnIFF1;
  private AbstractButton        btnIFF2;
  private JSpinner              spinnerIntMode;
  private JTextField            fldRegAF;
  private JTextField            fldRegAsciiA;
  private JTextField            fldRegBC;
  private JTextField            fldRegAsciiB;
  private JTextField            fldRegAsciiC;
  private JTextField            fldRegDE;
  private JTextField            fldRegAsciiD;
  private JTextField            fldRegAsciiE;
  private JTextField            fldRegHL;
  private JTextField            fldRegAsciiH;
  private JTextField            fldRegAsciiL;
  private JTextField            fldRegIX;
  private JTextField            fldRegAsciiIXH;
  private JTextField            fldRegAsciiIXL;
  private JTextField            fldRegIY;
  private JTextField            fldRegAsciiIYH;
  private JTextField            fldRegAsciiIYL;
  private JTextField            fldRegPC;
  private JTextField            fldRegSP;
  private JTextField            fldRegAF2;
  private JTextField            fldRegBC2;
  private JTextField            fldRegDE2;
  private JTextField            fldRegHL2;
  private JTextField            fldRegI;
  private JTextField            fldMemBC;
  private JTextField            fldMemDE;
  private JTextField            fldMemHL;
  private JTextField            fldMemIX;
  private JTextField            fldMemIY;
  private JTextField            fldMemSP;
  private JTextArea             fldMemPC;
  private JScrollPane           spMemPC;
  private JTextField            fldTStates;
  private JButton               btnResetTStates;
  private JLabel                labelIntMode;
  private JLabel                labelRegI;
  private JLabel                labelStatus;
  private JList[]               bpLists;
  private JScrollPane[]         bpScrollPanes;
  private JList                 listIntSrc;
  private JEditorPane           fldIntSrc;
  private javax.swing.Timer     timerForClear;


  public DebugFrm( Z80CPU cpu, Z80Memory memory )
  {
    this.cpu                = cpu;
    this.memory             = memory;
    this.lastSelectedIntSrc = null;
    this.lastBreakpointFile = null;
    this.lastTraceFile      = null;
    this.traceWriter        = null;
    this.bpDlg              = null;
    this.bpModels           = new BreakpointListModel[ BP_GROUP_CNT ];
    this.bpLists            = new JList[ BP_GROUP_CNT ];
    this.bpScrollPanes      = new JScrollPane[ BP_GROUP_CNT ];
    this.popupBreakGroupIdx = -1;
    this.interruptSources   = cpu.getInterruptSources();
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

    this.mnuFileBreakImportFile = createJMenuItem(
			"Haltepunkte aus Datei importieren..." );
    mnuFile.add( this.mnuFileBreakImportFile );

    this.mnuFileBreakImportClp = createJMenuItem(
			"Haltepunkte aus Zwischenablage importieren" );
    mnuFile.add( this.mnuFileBreakImportClp );
    mnuFile.addSeparator();

    this.mnuFileBreakRemoveImported = createJMenuItem(
			"Importierte Haltepunkte entfernen" );
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
			"Haltepunkt auf Programmadresse hinzuf\u00FCgen...",
			KeyEvent.VK_A, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakPCAdd );

    this.mnuDebugBreakMemoryAdd = createJMenuItem(
			"Haltepunkt auf Speicherbereich hinzuf\u00FCgen...",
			KeyEvent.VK_M, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakMemoryAdd );

    this.mnuDebugBreakInputAdd = createJMenuItem(
			"Haltepunkt auf Eingabetor hinzuf\u00FCgen...",
			KeyEvent.VK_I, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakInputAdd );

    this.mnuDebugBreakOutputAdd = createJMenuItem(
			"Haltepunkt auf Ausgabetor hinzuf\u00FCgen...",
			KeyEvent.VK_O, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakOutputAdd );

    this.mnuDebugBreakInterruptAdd = createJMenuItem(
			"Haltepunkt auf Interrupt-Quelle hinzuf\u00FCgen...",
			KeyEvent.VK_Q, InputEvent.CTRL_MASK );
    mnuDebug.add( this.mnuDebugBreakInterruptAdd );

    this.mnuDebugBreakRemove = createJMenuItem(
			"Haltepunkte entfernen",
			KeyEvent.VK_DELETE, 0 );
    mnuDebug.add( this.mnuDebugBreakRemove );

    this.mnuDebugBreakRemoveAll = createJMenuItem(
			"Alle Haltepunkte entfernen" );
    mnuDebug.add( this.mnuDebugBreakRemoveAll );
    mnuDebug.addSeparator();

    this.mnuDebugBreakEnable = createJMenuItem( "Haltepunkte aktivieren" );
    mnuDebug.add( this.mnuDebugBreakEnable );

    this.mnuDebugBreakDisable = createJMenuItem( "Haltepunkte deaktivieren" );
    mnuDebug.add( this.mnuDebugBreakDisable );

    this.mnuDebugBreakEnableAll = createJMenuItem(
				"Alle Haltepunkte aktivieren" );
    mnuDebug.add( this.mnuDebugBreakEnableAll );

    this.mnuDebugBreakDisableAll = createJMenuItem(
				"Alle Haltepunkte deaktivieren" );
    mnuDebug.add( this.mnuDebugBreakDisableAll );
    mnuDebug.addSeparator();

    this.mnuDebugTracer = new JCheckBoxMenuItem( "Befehle aufzeichnen" );
    this.mnuDebugTracer.setSelected( false );
    this.mnuDebugTracer.addActionListener( this );
    mnuDebug.add( this.mnuDebugTracer );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuDebug );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupBreakAdd = createJMenuItem( "Haltepunkt hinzuf\u00FCgen..." );
    this.mnuPopup.add( this.mnuPopupBreakAdd );

    this.mnuPopupBreakRemove = createJMenuItem( "Haltepunkte entfernen" );
    this.mnuPopup.add( this.mnuPopupBreakRemove );

    this.mnuPopupBreakRemoveAll = createJMenuItem(
				"Alle Haltepunkte entfernen" );
    this.mnuPopup.add( this.mnuPopupBreakRemoveAll );
    mnuPopup.addSeparator();

    this.mnuPopupBreakEnable = createJMenuItem( "Haltepunkte aktivieren" );
    mnuPopup.add( this.mnuPopupBreakEnable );

    this.mnuPopupBreakDisable = createJMenuItem( "Haltepunkte deaktivieren" );
    mnuPopup.add( this.mnuPopupBreakDisable );

    this.mnuPopupBreakEnableAll = createJMenuItem(
				"Alle Haltepunkte aktivieren" );
    mnuPopup.add( this.mnuPopupBreakEnableAll );

    this.mnuPopupBreakDisableAll = createJMenuItem(
				"Alle Haltepunkte deaktivieren" );
    mnuPopup.add( this.mnuPopupBreakDisableAll );


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
    this.fldMemPC = new JTextArea( 5, 38 );
    this.fldMemPC.setEditable( false );
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
      font = new Font( "Monospaced", font.getStyle(), font.getSize() );
    } else {
      font = new Font( "Monospaced", Font.PLAIN, 12 );
    }
    this.fldMemPC.setFont( font );

    // Haltepunkte
    JPanel panelBreak = new JPanel( new GridBagLayout() );
    panelBreak.setBorder(
		BorderFactory.createTitledBorder( "Haltepunkte auf..." ) );
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
		BorderFactory.createTitledBorder( "P-Adresse" ) );
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

    // Tab Interrupt-Quellen
    JPanel panelIntSrc = new JPanel( new BorderLayout( 5, 5 ) );
    this.tabbedPane.addTab( "Interrupt-Quellen", panelIntSrc );

    this.listIntSrc = new JList();
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


    // Statuszeile
    JPanel panelStatus = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    add( panelStatus, BorderLayout.SOUTH );

    this.labelStatus = new JLabel( "Bereit" );
    panelStatus.add( this.labelStatus );


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
    addComponentListener( this );
  }


  public Z80CPU getZ80CPU()
  {
    return this.cpu;
  }


  public Z80Memory getZ80Memory()
  {
    return this.memory;
  }


  public void setLabels( jkcemu.tools.Label[] labels )
  {
    importLabels( labels, true );
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
    } else {
      JList list = null;
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
    updIntSrcFields();
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
      if( src == this.mnuFileBreakImportFile ) {
	rv = true;
	doBreakpointImportFile();
      }
      else if( src == this.mnuFileBreakImportClp ) {
	rv = true;
	doBreakpointImportClp();
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
	doDebugBreakPCAdd();
      }
      else if( src == this.mnuDebugBreakRemove ) {
	rv = true;
	doDebugBreakRemove();
      }
      else if( src == this.mnuDebugBreakRemoveAll ) {
	rv = true;
	doDebugBreakRemoveAll();
      }
      else if( src == this.mnuDebugBreakEnable ) {
	rv = true;
	doDebugBreakSetEnabled( true );
      }
      else if( src == this.mnuDebugBreakDisable ) {
	rv = true;
	doDebugBreakSetEnabled( false );
      }
      else if( src == this.mnuDebugBreakEnableAll ) {
	rv = true;
	doDebugBreakSetAllEnabled( true );
      }
      else if( src == this.mnuDebugBreakDisableAll ) {
	rv = true;
	doDebugBreakSetAllEnabled( false );
      }
      else if( src == this.mnuDebugTracer ) {
	rv = true;
	doDebugTracer();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.open( "/help/tools/debugger.htm" );
      }
      else if( src == this.mnuPopupBreakAdd ) {
	rv = true;
	doDebugBreakAdd( this.popupBreakGroupIdx );
      }
      else if( src == this.mnuPopupBreakRemove ) {
	rv = true;
	doDebugBreakRemove( this.popupBreakGroupIdx );
      }
      else if( src == this.mnuPopupBreakRemoveAll ) {
	rv = true;
	doDebugBreakRemoveAll( this.popupBreakGroupIdx );
      }
      else if( src == this.mnuPopupBreakEnable ) {
	rv = true;
	doDebugBreakSetEnabled( this.popupBreakGroupIdx, true );
      }
      else if( src == this.mnuPopupBreakDisable ) {
	rv = true;
	doDebugBreakSetEnabled( this.popupBreakGroupIdx, false );
      }
      else if( src == this.mnuPopupBreakEnableAll ) {
	rv = true;
	doDebugBreakSetAllEnabled( this.popupBreakGroupIdx, true );
      }
      else if( src == this.mnuPopupBreakDisableAll ) {
	rv = true;
	doDebugBreakSetAllEnabled( this.popupBreakGroupIdx, false );
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
      else if( src == this.timerForClear ) {
	rv = true;
	clear();
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
      else if( src instanceof JList ) {
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
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
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
	  BreakpointListModel model       = this.bpModels[ idx ];
	  boolean             hasEnabled  = false;
	  boolean             hasDisabled = false;
	  if( model != null ) {
	    for( AbstractBreakpoint bp : model ) {
	      if( bp.isEnabled() ) {
		hasEnabled = true;
	      } else {
		hasDisabled = true;
	      }
	      if( hasEnabled && hasDisabled ) {
		break;
	      }
	    }
	    this.mnuPopupBreakRemoveAll.setEnabled( !model.isEmpty() );
	  } else {
	    this.mnuPopupBreakRemoveAll.setEnabled( false );
	  }
	  this.mnuPopupBreakEnableAll.setEnabled( hasDisabled );
	  this.mnuPopupBreakDisableAll.setEnabled( hasEnabled );

	  int nEnabled  = 0;
	  int nDisabled = 0;
	  if( model != null ) {
	    int[] rows  = this.bpLists[ idx ].getSelectedIndices();
	    if( rows != null ) {
	      for( int i = 0; i < rows.length; i++ ) {
		int row = rows[ i ];
		if( (row >= 0) && (row < model.size()) ) {
		  if( model.get( row ).isEnabled() ) {
		    nEnabled++;
		  } else {
		    nDisabled++;
		  }
		}
		if( (nEnabled > 1) && (nDisabled > 1) ) {
		  break;
		}
	      }
	    }
	  }
	  if( (nEnabled + nDisabled) ==  1) {
	    this.mnuPopupBreakRemove.setText( "Haltepunkt entfernen" );
	    this.mnuPopupBreakRemove.setEnabled( true );
	  } else {
	    this.mnuPopupBreakRemove.setText( "Haltepunkte entfernen" );
	    this.mnuPopupBreakRemove.setEnabled( (nEnabled + nDisabled) > 0 );
	  }
	  if( nDisabled == 1) {
	    this.mnuPopupBreakEnable.setText( "Haltepunkt aktivieren" );
	    this.mnuPopupBreakEnable.setEnabled( true );
	  } else {
	    this.mnuPopupBreakEnable.setText( "Haltepunkte aktivieren" );
	    this.mnuPopupBreakEnable.setEnabled( nDisabled > 0 );
	  }
	  if( nEnabled == 1) {
	    this.mnuPopupBreakDisable.setText( "Haltepunkt deaktivieren" );
	    this.mnuPopupBreakDisable.setEnabled( true );
	  } else {
	    this.mnuPopupBreakDisable.setText( "Haltepunkte deaktivieren" );
	    this.mnuPopupBreakDisable.setEnabled( nEnabled > 0 );
	  }
	  this.popupBreakGroupIdx = idx;
	  this.mnuPopup.show( c, e.getX(), e.getY() );
	  rv = true;
	}
      }
    }
    return rv;
  }


	/* --- Aktionen --- */

  private void doBreakpointImportClp()
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
			+ "Liste mit Haltepunkten interpretiert werden." );
    }
  }


  private void doBreakpointImportFile()
  {
    File file = this.lastBreakpointFile;
    if( file == null ) {
      file = Main.getLastPathFile( "breakpoint" );
    }
    file = EmuUtil.showFileOpenDlg(
			this,
			"Haltepunkte importieren",
			file,
			EmuUtil.getTextFileFilter() );
    if( file != null ) {
      try {
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
	  this.lastBreakpointFile = file;
	  Main.setLastFile( file, "breakpoint" );
	} else {
	  BasicDlg.showErrorDlg(
		this,
		"Der Inhalt der Datei konnte nicht als Liste\n"
			+ "mit Haltepunkten interpretiert werden." );
	}
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( this, ex );
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
    this.cpu.fireAction( Z80CPU.Action.DEBUG_RUN );
  }


  private void doDebugStop()
  {
    this.labelStatus.setText( "Programmausf\u00FChrung wird angehalten..." );
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STOP );
  }


  private void doDebugStepOver()
  {
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_OVER );
  }


  private void doDebugStepInto()
  {
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_INTO );
  }


  private void doDebugStepToRET()
  {
    this.cpu.fireAction( Z80CPU.Action.DEBUG_STEP_TO_RET );
  }


  private void doDebugBreakAdd( int bpGroupIdx )
  {
    switch( bpGroupIdx ) {
      case BP_PC_IDX:
	doDebugBreakPCAdd();
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
      BasicDlg.showErrorDlg(
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


  private void doDebugBreakPCAdd()
  {
    if( this.bpDlg == null ) {
      this.bpDlg = new PCBreakpointDlg( this, null );
      this.bpDlg.setVisible( true );
      AbstractBreakpoint bp = this.bpDlg.getApprovedBreakpoint();
      if( bp != null ) {
	addBreakpoint( BP_PC_IDX, bp );
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
	    Arrays.sort( rows );
	    for( int i = rows.length - 1; i >= 0; --i ) {
	      int row = rows[ i ];
	      if( (row >= 0) && (row < model.size()) ) {
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
    if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Haltepunkte entfernen?" ) )
    {
      for( int i = 0; i < this.bpModels.length; i++ ) {
	this.bpModels[ i ].clear();
	updBreakpointList( i, null );
      }
      updBreakpointActionsEnabled();
    }
  }


  private void doDebugBreakRemoveAll( int bpGroupIdx )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Haltepunkte"
			+ " dieser Gruppe entfernen?" ) )
      {
	this.bpModels[ bpGroupIdx ].clear();
	updBreakpointList( bpGroupIdx, null );
	updBreakpointActionsEnabled();
      }
    }
  }


  private void doDebugBreakSetEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakSetEnabled( i, state );
  }


  private void doDebugBreakSetEnabled( int bpGroupIdx, boolean state )
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
		bp.setEnabled( state );
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


  private void doDebugBreakSetAllEnabled( boolean state )
  {
    for( int i = 0; i < BP_GROUP_CNT; i++ )
      doDebugBreakSetAllEnabled( i, state );
  }


  private void doDebugBreakSetAllEnabled( int bpGroupIdx, boolean state )
  {
    if( (bpGroupIdx >= 0) && (bpGroupIdx < BP_GROUP_CNT) ) {
      if( this.bpModels[ bpGroupIdx ] != null ) {
	for( int i = 0; i < this.bpModels[ bpGroupIdx ].size(); i++ ) {
	  this.bpModels[ bpGroupIdx ].get( i ).setEnabled( state );
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

	action = BasicDlg.showOptionDlg(
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
			Main.getLastPathFile( "debug" ),
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
	  Main.setLastFile( file, "debug" );
	}
	catch( IOException ex ) {
	  this.traceWriter = null;
	  BasicDlg.showErrorDlg(
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
	BasicDlg.showErrorDlg(
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
    BreakpointListModel model      = new BreakpointListModel();
    this.bpModels[ bpGroupIdx ] = model;

    JList list = new JList( model );
    list.setVisibleRowCount( 4 );
    list.setPrototypeCellValue( "12345678901234" );
    list.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    list.setCellRenderer(
	new DefaultListCellRenderer()
	{
	  @Override
	  public Component getListCellRendererComponent(
					JList   list,
					Object  value,
					int     idx,
					boolean selected,
					boolean hasFocus )
	  {
	    String s = null;
	    if( value != null ) {
	      s = value.toString();
	    }
	    setToolTipText( s );
	    return super.getListCellRendererComponent(
						list,
						value,
						idx,
						selected,
						hasFocus );
	  }
	} );
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
    JList               list  = this.bpLists[ bpGroupIdx ];
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
		  this.bpDlg = new PCBreakpointDlg( this, oldBP );
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


  private boolean hasSpaceInMemPC( int row )
  {
    boolean   rv       = false;
    Dimension prefSize = this.fldMemPC.getPreferredSize();
    if( prefSize != null ) {
      if( prefSize.height > 0 ) {
	int hMax = this.spMemPC.getHeight() - 4;
	int hRow = prefSize.height / (row + 1);
	if( (prefSize.height + hRow) < hMax ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  private boolean importLabels(
			jkcemu.tools.Label[] labels,
			boolean              removeObsolete )
  {
    boolean                      rv       = false;
    Map<String,LabelBreakpoint>  labelMap = null;
    Map<Integer,LabelBreakpoint> addrMap  = null;
    BreakpointListModel          model    = this.bpModels[ BP_PC_IDX ];
    int len = model.size();
    if( len > 0 ) {
      for( int i = len - 1 ; i >= 0; --i ) {
	AbstractBreakpoint bp = model.get( i );
	if( bp != null ) {
	  if( bp instanceof LabelBreakpoint ) {
	    boolean done = false;
	    String  labelText = ((LabelBreakpoint) bp).getLabelText();
	    if( labelText != null ) {
	      if( !labelText.isEmpty() ) {
		if( labelMap == null ) {
		  labelMap = new HashMap<String,LabelBreakpoint>( len );
		}
		labelMap.put( labelText, (LabelBreakpoint) bp );
		done = true;
	      }
	    }
	    if( !done ) {
	      if( addrMap == null ) {
		addrMap = new HashMap<Integer,LabelBreakpoint>();
	      }
	      addrMap.put(
			((LabelBreakpoint) bp).getAddress(),
			(LabelBreakpoint) bp );
	    }
	    model.remove( i );
	  }
	}
      }
    }
    if( labels != null ) {
      for( jkcemu.tools.Label label : labels ) {
	boolean enabled = false;
	boolean done    = false;
	if( labelMap != null ) {
	  String labelName = label.getLabelName();
	  if( labelName != null ) {
	    if( !labelName.isEmpty() ) {
	      LabelBreakpoint oldBP = labelMap.get( labelName );
	      if( oldBP != null ) {
		enabled = oldBP.isEnabled();
		labelMap.remove( labelName );
		done = true;
	      }
	    }
	  }
	}
	if( !done && (addrMap != null) ) {
	  Integer         addr  = new Integer( label.getLabelValue() );
	  LabelBreakpoint oldBP = addrMap.get( addr );
	  if( oldBP != null ) {
	    enabled = oldBP.isEnabled();
	    addrMap.remove( addr );
	  }
	}
	LabelBreakpoint bp = new LabelBreakpoint(
					label.getLabelName(),
					label.getLabelValue() );
	bp.setEnabled( enabled );
	model.add( bp );
	rv = true;
      }
    }
    if( rv ) {
      if( !removeObsolete ) {
	int nOld = 0;
	if( labelMap != null ) {
	  nOld = labelMap.size();
	}
	if( addrMap != null ) {
	  nOld += addrMap.size();
	}
	if( nOld > 0 ) {
	  this.bpLists[ BP_PC_IDX ].setModel( model );
	  removeObsolete = BasicDlg.showYesNoDlg(
		this,
		"Es wurden nicht alle bereits vorher importierten"
			+ " Haltepunkte ersetzt.\n"
			+ "Sollen die jetzt nicht mehr mit"
			+ " dabei gewesenen Haltepunkte\n"
			+ "entfernt werden?" );
	}
      }
      if( !removeObsolete ) {
	if( labelMap != null ) {
	  Collection<LabelBreakpoint> c = labelMap.values();
	  if( c != null ) {
	    model.addAll( c );
	  }
	}
	if( addrMap != null ) {
	  Collection<LabelBreakpoint> c = addrMap.values();
	  if( c != null ) {
	    model.addAll( c );
	  }
	}
      }
      try {
	Collections.sort( model );
      }
      catch( ClassCastException ex ) {}
      this.bpLists[ BP_PC_IDX ].setModel( model );
      updBreakpointList( BP_PC_IDX, null );
      if( this.cpu.isPause() ) {
	updFieldsPC();
      }
    }
    return rv;
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
    this.mnuDebugStop.setEnabled( false );
    this.mnuDebugStepOver.setEnabled( false );
    this.mnuDebugStepInto.setEnabled( false );
    this.mnuDebugStepToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnStop.setEnabled( false );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnStepToRET.setEnabled( false );
    this.btnResetTStates.setEnabled( false );
    this.labelStatus.setText( statusText );
  }


  private void setDebugRunning()
  {
    setDebuggerEditable( false );
    this.timerForClear.start();
    this.mnuDebugRun.setEnabled( false );
    this.mnuDebugStop.setEnabled( true );
    this.mnuDebugStepOver.setEnabled( false );
    this.mnuDebugStepInto.setEnabled( false );
    this.mnuDebugStepToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnStop.setEnabled( true );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnStepToRET.setEnabled( false );
    this.btnResetTStates.setEnabled( false );
    this.labelStatus.setText( "Programm wird gerade ausgef\u00FChrt..." );
  }


  private void setDebugStopped( Z80InterruptSource iSource )
  {
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
    this.mnuDebugStop.setEnabled( false );
    this.mnuDebugStepOver.setEnabled( true );
    this.mnuDebugStepInto.setEnabled( true );
    this.mnuDebugStepToRET.setEnabled( true );

    this.btnRun.setEnabled( true );
    this.btnStop.setEnabled( false );
    this.btnStepOver.setEnabled( true );
    this.btnStepInto.setEnabled( true );
    this.btnStepToRET.setEnabled( true );
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


  private void updBreakpointActionsEnabled()
  {
    boolean hasEnabled   = false;
    boolean hasDisabled  = false;
    int     nSelEnabled  = 0;
    int     nSelDisabled = 0;
    for( int i = 0; i < BP_GROUP_CNT; i++ ) {
      if( this.bpModels[ i ] != null ) {
	BreakpointListModel model = this.bpModels[ i ];
	for( AbstractBreakpoint bp : model ) {
	  if( bp.isEnabled() ) {
	    hasEnabled = true;
	  } else {
	    hasDisabled = true;
	  }
	  if( hasEnabled && hasDisabled ) {
	    break;
	  }
	}
	int[] rows = this.bpLists[ i ].getSelectedIndices();
	if( rows != null ) {
	  for( int k = 0; k < rows.length; k++ ) {
	    int row = rows[ k ];
	    if( (row >= 0) && (row < model.size()) ) {
	      if( model.get( row ).isEnabled() ) {
		nSelEnabled++;
	      } else {
		nSelDisabled++;
	      }
	    }
	    if( (nSelEnabled > 1) && (nSelDisabled > 1) ) {
	      break;
	    }
	  }
	}
      }
      if( hasEnabled && hasDisabled
	  && (nSelEnabled > 1) && (nSelDisabled > 1) )
      {
	break;
      }
    }
    if( (nSelEnabled + nSelDisabled) == 1 ) {
      this.mnuDebugBreakRemove.setText( "Haltepunkt entfernen" );
      this.mnuDebugBreakRemove.setEnabled( true );
    } else {
      this.mnuDebugBreakRemove.setText( "Haltepunkte entfernen" );
      this.mnuDebugBreakRemove.setEnabled( (nSelEnabled + nSelDisabled) > 0 );
    }
    if( nSelDisabled == 1 ) {
      this.mnuDebugBreakEnable.setText( "Haltepunkt aktivieren" );
      this.mnuDebugBreakEnable.setEnabled( true );
    } else {
      this.mnuDebugBreakEnable.setText( "Haltepunkte aktivieren" );
      this.mnuDebugBreakEnable.setEnabled( nSelDisabled > 0 );
    }
    if( nSelEnabled == 1 ) {
      this.mnuDebugBreakDisable.setText( "Haltepunkt deaktivieren" );
      this.mnuDebugBreakDisable.setEnabled( true );
    } else {
      this.mnuDebugBreakDisable.setText( "Haltepunkte deaktivieren" );
      this.mnuDebugBreakDisable.setEnabled( nSelEnabled > 0 );
    }
    this.mnuDebugBreakEnableAll.setEnabled( hasDisabled );
    this.mnuDebugBreakDisableAll.setEnabled( hasEnabled );
    this.mnuDebugBreakRemoveAll.setEnabled( hasEnabled || hasDisabled );
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
		if( bp.isEnabled() ) {
		  if( bp instanceof Z80Breakpoint ) {
		    if( list == null ) {
		      list = new ArrayList<Z80Breakpoint>();
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
      if( this.cpu.isPause() ) {
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
	  setDebugStopped( iSource );
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


  private void updFieldIntMode()
  {
    try {
      this.spinnerIntMode.setValue( this.cpu.getInterruptMode() );
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

    int addr = this.cpu.getRegPC();
    this.fldRegPC.setText( String.format( "%04X", addr ) );

    // Befehle anzeigen
    this.fldMemPC.setText( "" );
    int row = 0;
    while( hasSpaceInMemPC( row ) ) {
      if( row > 0 ) {
	this.fldMemPC.append( "\n" );
      }
      row++;

      int    linePos = 0;
      String label   = null;
      if( (doc != null) && (bpModel != null) ) {
	linePos = doc.getLength();
	for( AbstractBreakpoint breakpoint : bpModel ) {
	  if( breakpoint instanceof LabelBreakpoint ) {
	    if( ((LabelBreakpoint) breakpoint).getAddress() == addr ) {
	      String s = ((LabelBreakpoint) breakpoint).getLabelText();
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
	      this.lastSelectedIntSrc.appendStatusHTMLTo( buf );
	      buf.append( "</html>\n" );
	      this.fldIntSrc.setContentType( "text/html" );
	      this.fldIntSrc.setText( buf.toString() );
	      done = true;
	    }
	  }
	}
      }
      if( !done ) {
	this.fldIntSrc.setContentType( "text/plain" );
	this.fldIntSrc.setText( "Bitte Interrupt-Quelle ausw\u00E4hlen!" );
      }
    }
    try {
      this.fldIntSrc.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }
}

