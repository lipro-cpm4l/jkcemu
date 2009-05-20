/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Debugger-Fenster
 */

package jkcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.*;


public class DebugFrm extends BasicFrm implements
						FocusListener,
						ListSelectionListener,
						Z80StatusListener,
						Runnable
{
  private ScreenFrm         screenFrm;
  private EmuThread         emuThread;
  private Z80CPU            z80cpu;
  private Z80Breakpoint[]   breakpoints;
  private File              lastTraceFile;
  private PrintWriter       traceWriter;
  private JMenuItem         mnuFileClose;
  private JMenuItem         mnuFileOpenTrace;
  private JMenuItem         mnuDebugRun;
  private JMenuItem         mnuDebugStop;
  private JMenuItem         mnuDebugStepOver;
  private JMenuItem         mnuDebugStepInto;
  private JMenuItem         mnuDebugStepUp;
  private JMenuItem         mnuDebugBreakAdd;
  private JMenuItem         mnuDebugBreakRemove;
  private JMenuItem         mnuDebugBreakRemoveAll;
  private JMenuItem         mnuDebugBreakEnable;
  private JMenuItem         mnuDebugBreakDisable;
  private JMenuItem         mnuDebugBreakEnableAll;
  private JMenuItem         mnuDebugBreakDisableAll;
  private JCheckBoxMenuItem mnuDebugTracer;
  private JMenuItem         mnuHelpContent;
  private JPopupMenu        mnuPopup;
  private JMenuItem         mnuPopupBreakAdd;
  private JMenuItem         mnuPopupBreakRemove;
  private JMenuItem         mnuPopupBreakRemoveAll;
  private JMenuItem         mnuPopupBreakEnable;
  private JMenuItem         mnuPopupBreakDisable;
  private JMenuItem         mnuPopupBreakEnableAll;
  private JMenuItem         mnuPopupBreakDisableAll;
  private JButton           btnRun;
  private JButton           btnStop;
  private JButton           btnStepOver;
  private JButton           btnStepInto;
  private JButton           btnStepUp;
  private AbstractButton    btnFlagSign;
  private AbstractButton    btnFlagZero;
  private AbstractButton    btnFlagHalf;
  private AbstractButton    btnFlagPV;
  private AbstractButton    btnFlagN;
  private AbstractButton    btnFlagCarry;
  private AbstractButton    btnIFF1;
  private AbstractButton    btnIFF2;
  private JTextField        fldRegAF;
  private JTextField        fldRegAsciiA;
  private JTextField        fldRegBC;
  private JTextField        fldRegAsciiB;
  private JTextField        fldRegAsciiC;
  private JTextField        fldRegDE;
  private JTextField        fldRegAsciiD;
  private JTextField        fldRegAsciiE;
  private JTextField        fldRegHL;
  private JTextField        fldRegAsciiH;
  private JTextField        fldRegAsciiL;
  private JTextField        fldRegIX;
  private JTextField        fldRegAsciiIXH;
  private JTextField        fldRegAsciiIXL;
  private JTextField        fldRegIY;
  private JTextField        fldRegAsciiIYH;
  private JTextField        fldRegAsciiIYL;
  private JTextField        fldRegPC;
  private JTextField        fldRegSP;
  private JTextField        fldRegAF2;
  private JTextField        fldRegBC2;
  private JTextField        fldRegDE2;
  private JTextField        fldRegHL2;
  private JTextField        fldMemBC;
  private JTextField        fldMemDE;
  private JTextField        fldMemHL;
  private JTextField        fldMemIX;
  private JTextField        fldMemIY;
  private JTextField        fldMemSP;
  private JTextArea         fldMemPC;
  private JLabel            labelStatus;
  private JList             listBreakpoint;
  private javax.swing.Timer timerForClear;


  public DebugFrm( ScreenFrm screenFrm )
  {
    this.screenFrm     = screenFrm;
    this.emuThread     = screenFrm.getEmuThread();
    this.z80cpu        = emuThread.getZ80CPU();
    this.breakpoints   = null;
    this.lastTraceFile = null;
    this.traceWriter   = null;

    setTitle( "JKCEMU Debugger" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Debuggen
    JMenu mnuDebug = new JMenu( "Debuggen" );
    mnuDebug.setMnemonic( KeyEvent.VK_B );

    this.mnuDebugRun = createJMenuItem(
				"Bis Haltepunkt ausf\u00FChren",
				KeyEvent.VK_F5, 0 );
    mnuDebug.add( this.mnuDebugRun );

    this.mnuDebugStop = createJMenuItem(
				"Programmausf\u00FChrung anhalten",
				KeyEvent.VK_F5, KeyEvent.SHIFT_MASK );
    mnuDebug.add( this.mnuDebugStop );

    this.mnuDebugStepOver = createJMenuItem(
				"\u00DCber Aufruf springen",
				KeyEvent.VK_F7, 0 );
    mnuDebug.add( this.mnuDebugStepOver );

    this.mnuDebugStepInto = createJMenuItem(
				"In Aufruf springen",
				KeyEvent.VK_F8, 0 );
    mnuDebug.add( this.mnuDebugStepInto );

    this.mnuDebugStepUp = createJMenuItem(
				"Aus Aufruf herausspringen",
				KeyEvent.VK_F8, KeyEvent.SHIFT_MASK );
    mnuDebug.add( this.mnuDebugStepUp );
    mnuDebug.addSeparator();

    this.mnuDebugBreakAdd = createJMenuItem(
				"Haltepunkt hinzuf\u00FCgen...",
				KeyEvent.VK_F9, 0 );
    mnuDebug.add( this.mnuDebugBreakAdd );

    this.mnuDebugBreakRemove = createJMenuItem(
				"Haltepunkt entfernen",
				KeyEvent.VK_F9, KeyEvent.SHIFT_MASK );
    mnuDebug.add( this.mnuDebugBreakRemove );

    this.mnuDebugBreakRemoveAll = createJMenuItem(
				"Alle Haltepunkte entfernen" );
    mnuDebug.add( this.mnuDebugBreakRemoveAll );
    mnuDebug.addSeparator();

    this.mnuDebugBreakEnable = createJMenuItem( "Haltepunkt aktivieren" );
    mnuDebug.add( this.mnuDebugBreakEnable );

    this.mnuDebugBreakDisable = createJMenuItem( "Haltepunkt deaktivieren" );
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

    this.mnuPopupBreakRemove = createJMenuItem( "Haltepunkt entfernen" );
    this.mnuPopup.add( this.mnuPopupBreakRemove );

    this.mnuPopupBreakRemoveAll = createJMenuItem(
				"Alle Haltepunkte entfernen" );
    this.mnuPopup.add( this.mnuPopupBreakRemoveAll );
    mnuPopup.addSeparator();

    this.mnuPopupBreakEnable = createJMenuItem( "Haltepunkt aktivieren" );
    mnuPopup.add( this.mnuPopupBreakEnable );

    this.mnuPopupBreakDisable = createJMenuItem( "Haltepunkt deaktivieren" );
    mnuPopup.add( this.mnuPopupBreakDisable );

    this.mnuPopupBreakEnableAll = createJMenuItem(
				"Alle Haltepunkte aktivieren" );
    mnuPopup.add( this.mnuPopupBreakEnableAll );

    this.mnuPopupBreakDisableAll = createJMenuItem(
				"Alle Haltepunkte deaktivieren" );
    mnuPopup.add( this.mnuPopupBreakDisableAll );


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


    // Toolbar
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    this.btnRun = createImageButton(
			"/images/debug/run.png",
			"Bis Haltepunkt ausf\u00FChren" );
    toolBar.add( this.btnRun );

    this.btnStop = createImageButton(
			"/images/debug/stop.png",
			"Programmausf\u00FChrung anhalten" );
    toolBar.add( this.btnStop );

    this.btnStepOver = createImageButton(
			"/images/debug/step_over.png",
			"\u00DCber Aufruf springen" );
    toolBar.add( this.btnStepOver );

    this.btnStepInto = createImageButton(
			"/images/debug/step_into.png",
			"In Aufruf springen" );
    toolBar.add( this.btnStepInto );

    this.btnStepUp = createImageButton(
			"/images/debug/step_up.png",
			"Aus Aufruf herausspringen" );
    toolBar.add( this.btnStepUp );


    // Bereich Flags
    JPanel panelFlag = new JPanel( new FlowLayout() );
    panelFlag.setBorder( BorderFactory.createTitledBorder( "Flags" ) );
    gbc.gridx++;
    add( panelFlag, gbc );

    this.btnFlagSign = createFlagField( "S" );
    panelFlag.add( this.btnFlagSign );

    this.btnFlagZero = createFlagField( "Z" );
    panelFlag.add( this.btnFlagZero );

    this.btnFlagHalf = createFlagField( "H" );
    panelFlag.add( this.btnFlagHalf );

    this.btnFlagPV = createFlagField( "PV" );
    panelFlag.add( this.btnFlagPV );

    this.btnFlagN = createFlagField( "N" );
    panelFlag.add( this.btnFlagN );

    this.btnFlagCarry = createFlagField( "C" );
    panelFlag.add( this.btnFlagCarry );

    this.btnIFF1 = createFlagField( "IFF1" );
    panelFlag.add( this.btnIFF1 );

    this.btnIFF2 = createFlagField( "IFF2" );
    panelFlag.add( this.btnIFF2 );


    // Bereich Register
    JPanel panelReg = new JPanel( new GridBagLayout() );
    panelReg.setBorder( BorderFactory.createTitledBorder( "Register" ) );
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( panelReg, gbc );

    GridBagConstraints gbcReg = new GridBagConstraints(
						1, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    // Ueberschriften
    panelReg.add( new JLabel( "HEX" ), gbcReg );
    gbcReg.gridwidth = 2;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "ASCII" ), gbcReg );
    gbcReg.gridwidth = 1;
    gbcReg.gridx++;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "Zeigt im Speicher auf" ), gbcReg );


    // Register AF
    gbcReg.gridx = 0;
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
    gbcReg.gridx++;
    gbcReg.gridx++;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "AF\':" ), gbcReg );

    this.fldRegAF2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegAF2, gbcReg );


    // Register BC
    gbcReg.gridx = 0;
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

    this.fldMemBC = new JTextField( 24 );
    this.fldMemBC.setEditable( false );
    gbcReg.fill = GridBagConstraints.HORIZONTAL;
    gbcReg.gridx++;
    panelReg.add( this.fldMemBC, gbcReg );


    // Register BC2
    gbcReg.fill = GridBagConstraints.NONE;
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

    this.fldMemDE = new JTextField( 24 );
    this.fldMemDE.setEditable( false );
    gbcReg.fill = GridBagConstraints.HORIZONTAL;
    gbcReg.gridx++;
    panelReg.add( this.fldMemDE, gbcReg );


    // Register DE2
    gbcReg.fill = GridBagConstraints.NONE;
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

    this.fldMemHL = new JTextField( 24 );
    this.fldMemHL.setEditable( false );
    gbcReg.fill = GridBagConstraints.HORIZONTAL;
    gbcReg.gridx++;
    panelReg.add( this.fldMemHL, gbcReg );


    // Register HL2
    gbcReg.fill = GridBagConstraints.NONE;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "HL\':" ), gbcReg );

    this.fldRegHL2 = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegHL2, gbcReg );


    // Register IX
    gbcReg.fill  = GridBagConstraints.NONE;
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
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx++;
    panelReg.add( this.fldMemIX, gbcReg );


    // Register IY
    gbcReg.fill  = GridBagConstraints.NONE;
    gbcReg.gridx = 0;
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
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx++;
    panelReg.add( this.fldMemIY, gbcReg );


    // Register SP
    gbcReg.fill  = GridBagConstraints.NONE;
    gbcReg.gridx = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "SP:" ), gbcReg );

    this.fldRegSP = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegSP, gbcReg );

    this.fldMemSP = new JTextField();
    this.fldMemSP.setEditable( false );
    gbcReg.fill      = GridBagConstraints.HORIZONTAL;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx++;
    gbcReg.gridx++;
    gbcReg.gridx++;
    panelReg.add( this.fldMemSP, gbcReg );


    // Register PC
    gbcReg.anchor    = GridBagConstraints.NORTHWEST;
    gbcReg.fill      = GridBagConstraints.NONE;
    gbcReg.gridwidth = 1;
    gbcReg.gridx     = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "PC:" ), gbcReg );

    this.fldRegPC = createHexField();
    gbcReg.gridx++;
    panelReg.add( this.fldRegPC, gbcReg );

    this.fldMemPC = new JTextArea( 5, 32 );
    this.fldMemPC.setEditable( false );
    Font font = this.fldMemPC.getFont();
    if( font != null ) {
      this.fldMemPC.setFont(
		new Font( "Monospaced", font.getStyle(), font.getSize() ) );
    } else {
      this.fldMemPC.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
    }
    gbcReg.fill      = GridBagConstraints.HORIZONTAL;
    gbcReg.anchor    = GridBagConstraints.WEST;
    gbcReg.gridwidth = GridBagConstraints.REMAINDER;
    gbcReg.gridx++;
    panelReg.add( new JScrollPane( this.fldMemPC ), gbcReg );


    // Haltepunkte
    JPanel panelBreak = new JPanel( new BorderLayout( 5, 5 ) );
    panelBreak.setBorder( BorderFactory.createTitledBorder( "Haltepunkte" ) );

    this.listBreakpoint  = new JList();
    this.listBreakpoint.addListSelectionListener( this );
    this.listBreakpoint.addMouseListener( this );
    this.listBreakpoint.setPrototypeCellValue( "123456789012" );
    this.listBreakpoint.setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION );

    JScrollPane spBreakpoints = new JScrollPane( this.listBreakpoint );
    spBreakpoints.addMouseListener( this );

    panelBreak.add( spBreakpoints, BorderLayout.CENTER );

    gbc.fill      = GridBagConstraints.VERTICAL;
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.gridwidth = 1;
    gbc.gridx += 3;
    add( panelBreak, gbc );


    // Statuszeile
    this.labelStatus = new JLabel( "Bereit" );
    gbc.anchor       = GridBagConstraints.WEST;
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth    = GridBagConstraints.REMAINDER;
    gbc.gridx        = 0;
    gbc.gridy++;
    add(  this.labelStatus, gbc );


    // sonstiges
    this.timerForClear = new javax.swing.Timer( 300, this );
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setScreenCentered();
    }
    setResizable( false );
    this.z80cpu.addStatusListener( this );
    updDebugger();
    updActionFields();
    updBreakpointList( -1 );
  }


	/* --- Methoden fuer FocusListener --- */

  public void focusGained( FocusEvent e )
  {
    // empty
  }


  public void focusLost( FocusEvent e )
  {
    Component c = e.getComponent();
    if( c != null )
      fieldValueChanged( c );
  }


	/* --- Methoden fuer ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.listBreakpoint )
      updActionFields();
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
  public void z80StatusChanged()
  {
    SwingUtilities.invokeLater( this );
  }


	/* --- Methoden fuer Runnable --- */

  /*
   * Diese Methode wird im Swing-Thread aufgerufen,
   * wenn sich der Zustand der CPU-Emulation aendert.
   * Es wird der Fensterinhalt aktualisiert.
   */
  public void run()
  {
    if( isShowing() ) {
      updDebugger();
      if( this.z80cpu.isPause() ) {
	setState( Frame.NORMAL );
	toFront();
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void setVisible( boolean state )
  {
    if( state ) {
      this.z80cpu.setDebugEnabled( state );  // Disable erfolgt ueber doClose()
      SwingUtilities.invokeLater( this );    // Anzeige aktualisieren
    }
    super.setVisible( state );
  }


  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {

      // Wenn Aktion nicht von einem Textfeld ausging,
      // dann Textfeld mit Focus validieren
      Component c = getFocusOwner();
      if( c != null ) {
	if( !(c instanceof JTextComponent) )
	  fieldValueChanged( c );
      }

      // Aktion auswerten
      Object src = e.getSource();
      if( src == this.mnuFileClose ) {
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
      else if( (src == this.mnuDebugStepUp)
	       || (src == this.btnStepUp) )
      {
	rv = true;
	doDebugStepUp();
      }
      else if( (src == this.mnuDebugBreakAdd)
	       || (src == this.mnuPopupBreakAdd) )
      {
	rv = true;
	doDebugBreakAdd();
      }
      else if( (src == this.mnuDebugBreakRemove)
	       || (src == this.mnuPopupBreakRemove) )
      {
	rv = true;
	doDebugBreakRemove();
      }
      else if( (src == this.mnuDebugBreakRemoveAll)
	       || (src == this.mnuPopupBreakRemoveAll) )
      {
	rv = true;
	doDebugBreakRemoveAll();
      }
      else if( (src == this.mnuDebugBreakEnable)
	       || (src == this.mnuPopupBreakEnable) )
      {
	rv = true;
	doDebugBreakSetEnabled( true );
      }
      else if( (src == this.mnuDebugBreakDisable)
	       || (src == this.mnuPopupBreakDisable) )
      {
	rv = true;
	doDebugBreakSetEnabled( false );
      }
      else if( (src == this.mnuDebugBreakEnableAll)
	       || (src == this.mnuPopupBreakEnableAll) )
      {
	rv = true;
	doDebugBreakSetAllEnabled( true );
      }
      else if( (src == this.mnuDebugBreakDisableAll)
	       || (src == this.mnuPopupBreakDisableAll) )
      {
	rv = true;
	doDebugBreakSetAllEnabled( false );
      }
      else if( src == this.mnuDebugTracer ) {
	rv = true;
	doDebugTracer();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	screenFrm.showHelp( "/help/tools/debugger.htm" );
      }
      else if( src == this.btnFlagSign ) {
	rv = true;
	if( this.btnFlagSign.isEnabled() ) {
	  this.z80cpu.setFlagSign( this.btnFlagSign.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagZero ) {
	rv = true;
	if( this.btnFlagZero.isEnabled() ) {
	  this.z80cpu.setFlagZero( this.btnFlagZero.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagHalf ) {
	rv = true;
	if( this.btnFlagHalf.isEnabled() ) {
	  this.z80cpu.setFlagHalf( this.btnFlagHalf.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagPV ) {
	rv = true;
	if( this.btnFlagPV.isEnabled() ) {
	  this.z80cpu.setFlagPV( this.btnFlagPV.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagN ) {
	rv = true;
	if( this.btnFlagN.isEnabled() ) {
	  this.z80cpu.setFlagN( this.btnFlagN.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnFlagCarry ) {
	rv = true;
	if( this.btnFlagCarry.isEnabled() ) {
	  this.z80cpu.setFlagCarry( this.btnFlagCarry.isSelected() );
	}
	updFieldsAF();
      }
      else if( src == this.btnIFF1 ) {
	rv = true;
	if( this.btnIFF1.isEnabled() ) {
	  this.z80cpu.setIFF1( this.btnIFF1.isSelected() );
	}
      }
      else if( src == this.btnIFF2 ) {
	rv = true;
	if( this.btnIFF2.isEnabled() ) {
	  this.z80cpu.setIFF2( this.btnIFF2.isSelected() );
	}
      }
      else if( src == this.timerForClear ) {
	rv = true;
	clear();
      }
      else if( (src == this.fldRegAF)
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
	       || (src == this.fldRegPC) )
      {
	rv = true;
	fieldValueChanged( src );
      }
    }
    return rv;
  }


  public boolean doClose()
  {
    closeTrace();
    this.z80cpu.setDebugEnabled( false );
    return super.doClose();
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


	/* --- Aktionen --- */

  private void doDebugRun()
  {
    this.z80cpu.fireAction( Z80CPU.Action.DEBUG_RUN );
  }


  private void doDebugStop()
  {
    this.labelStatus.setText( "Programmausf\u00FChrung wird angehalten..." );
    this.z80cpu.fireAction( Z80CPU.Action.DEBUG_STOP );
  }


  private void doDebugStepOver()
  {
    this.z80cpu.fireAction( Z80CPU.Action.DEBUG_STEP_OVER );
  }


  private void doDebugStepInto()
  {
    this.z80cpu.fireAction( Z80CPU.Action.DEBUG_STEP_INTO );
  }


  private void doDebugStepUp()
  {
    this.z80cpu.fireAction( Z80CPU.Action.DEBUG_STEP_UP );
  }


  private void doDebugBreakAdd()
  {
    ReplyHexDlg dlg = new ReplyHexDlg( this, "Haltepunktadresse:", 4, null );
    dlg.setVisible( true );
    Integer addr = dlg.getReply();
    if( addr != null ) {
      try {
	Z80Breakpoint bp = new Z80Breakpoint( addr.intValue() );
	if( this.breakpoints != null ) {
	  int idx = Arrays.binarySearch( this.breakpoints, bp );
	  if( idx < 0 ) {
	    Z80Breakpoint[] a = new Z80Breakpoint[
					this.breakpoints.length + 1 ];
	    idx     = -(idx + 1);
	    int dst = 0;
	    for( int i = 0; i < this.breakpoints.length; i++ ) {
	      if( i == idx ) {
		if( dst < a.length ) {
		  a[ dst++ ] = bp;
		}
	      }
	      if( dst < a.length ) {
		a[ dst++ ] = this.breakpoints[ i ];
	      }
	    }
	    if( (idx == this.breakpoints.length) && (dst < a.length) ) {
	      a[ dst++ ] = bp;
	    }
	    this.breakpoints = a;
	  }
	  updBreakpointList( idx );
	} else {
	  this.breakpoints      = new Z80Breakpoint[ 1 ];
	  this.breakpoints[ 0 ] = bp;
	  updBreakpointList( 0 );
	}
      }
      catch( ClassCastException ex ) {}
    }
  }


  private void doDebugBreakRemove()
  {
    if( this.breakpoints != null ) {
      int idx = this.listBreakpoint.getSelectedIndex();
      if( (idx >= 0) && (idx < this.breakpoints.length) ) {
	if( this.breakpoints.length > 1 ) {
	  Z80Breakpoint[] a = new Z80Breakpoint[ this.breakpoints.length - 1 ];
	  int dst = 0;
	  for( int i = 0;
	       (i < this.breakpoints.length) && (dst < a.length);
	       i++ )
	  {
	    if( i != idx ) {
	      a[ dst++ ] = this.breakpoints[ i ];
	    }
	  }
	  this.breakpoints = a;
	} else {
	  this.breakpoints = null;
	}
	updBreakpointList( -1 );
	updActionFields();
      }
    }
  }


  private void doDebugBreakRemoveAll()
  {
    if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie alle Haltepunkte entfernen?" ) )
    {
      this.breakpoints = null;
      updBreakpointList( -1 );
      updActionFields();
    }
  }


  private void doDebugBreakSetEnabled( boolean state )
  {
    if( this.breakpoints != null ) {
      int idx = this.listBreakpoint.getSelectedIndex();
      if( (idx >= 0) && (idx < this.breakpoints.length) ) {
	this.breakpoints[ idx ].setEnabled( state );
	updBreakpointList( idx );
	updActionFields();
      }
    }
  }


  private void doDebugBreakSetAllEnabled( boolean state )
  {
    if( this.breakpoints != null ) {
      for( int i = 0; i < this.breakpoints.length; i++ ) {
	this.breakpoints[ i ].setEnabled( state );
      }
      updBreakpointList( -1 );
      updActionFields();
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

	  this.z80cpu.setDebugTracer( this.traceWriter );
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


  private void setDebuggerDisabled()
  {
    clear();
    setDebuggerEditable( false );
    this.mnuDebugRun.setEnabled( false );
    this.mnuDebugStop.setEnabled( false );
    this.mnuDebugStepOver.setEnabled( false );
    this.mnuDebugStepInto.setEnabled( false );
    this.mnuDebugStepUp.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnStop.setEnabled( false );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnStepUp.setEnabled( false );
    this.labelStatus.setText( "Debugger nicht aktiv" );
  }


  private void setDebugRunning()
  {
    setDebuggerEditable( false );
    this.timerForClear.start();
    this.mnuDebugRun.setEnabled( false );
    this.mnuDebugStop.setEnabled( true );
    this.mnuDebugStepOver.setEnabled( false );
    this.mnuDebugStepInto.setEnabled( false );
    this.mnuDebugStepUp.setEnabled( false );
    this.btnRun.setEnabled( false );
    this.btnStop.setEnabled( true );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnStepUp.setEnabled( false );
    this.labelStatus.setText( "Programm wird gerade ausgef\u00FChrt..." );
  }


  private void setDebugStopped()
  {
    updFieldsFlag();
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

    this.mnuDebugRun.setEnabled( true );
    this.mnuDebugStop.setEnabled( false );
    this.mnuDebugStepOver.setEnabled( true );
    this.mnuDebugStepInto.setEnabled( true );
    this.mnuDebugStepUp.setEnabled( true );

    this.btnRun.setEnabled( true );
    this.btnStop.setEnabled( false );
    this.btnStepOver.setEnabled( true );
    this.btnStepInto.setEnabled( true );
    this.btnStepUp.setEnabled( true );

    setDebuggerEditable( true );
    this.labelStatus.setText( "Programmausf\u00FChrung angehalten" );
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
    this.fldMemBC.setText( "" );
    this.fldMemDE.setText( "" );
    this.fldMemHL.setText( "" );
    this.fldMemIX.setText( "" );
    this.fldMemIY.setText( "" );
    this.fldMemSP.setText( "" );
    this.fldMemPC.setText( "" );
  }


  private void closeTrace()
  {
    if( this.traceWriter != null ) {
      this.z80cpu.setDebugTracer( null );
      this.traceWriter.println( "---" );
      this.traceWriter.flush();
      this.traceWriter.close();

      boolean isErr = this.traceWriter.checkError();
      this.traceWriter = null;
      if( isErr ) {
	BasicDlg.showErrorDlg( this, "Die Befehlsaufzeichnungsdatei"
				+ " konnte nicht gespeichert werden." );
      }
    }
  }


  private void fieldValueChanged( Object fld )
  {
    if( fld instanceof JTextField ) {
      JTextField tf = (JTextField) fld;
      if( tf.isEditable() ) {

	// neuer Wert ermitteln
	int v = -1;
	String text = tf.getText();
	if( (text != null) && (text.length() > 0) ) {
	  try {
	    v = Integer.parseInt( text, 16 );
	  }
	  catch( NumberFormatException ex ) {
	    v = -1;
	  }
	}

	if( tf == this.fldRegAF ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegAF( v );
	  }
	  updFieldsFlag();
	  updFieldsAF();
	}
	else if( tf == this.fldRegAF2 ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegAF2( v );
	  }
	  updFieldsAF2();
	}
	else if( tf == this.fldRegBC ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegBC( v );
	  }
	  updFieldsBC();
	}
	else if( tf == this.fldRegBC2 ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegBC2( v );
	  }
	  updFieldsBC2();
	}
	else if( tf == this.fldRegDE ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegDE( v );
	  }
	  updFieldsDE();
	}
	else if( tf == this.fldRegDE2 ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegDE2( v );
	  }
	  updFieldsDE2();
	}
	else if( tf == this.fldRegHL ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegHL( v );
	  }
	  updFieldsHL();
	}
	else if( tf == this.fldRegHL2 ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegHL2( v );
	  }
	  updFieldsHL2();
	}
	else if( tf == this.fldRegIX ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegIX( v );
	  }
	  updFieldsIX();
	}
	else if( tf == this.fldRegIY ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegIY( v );
	  }
	  updFieldsIY();
	}
	else if( tf == this.fldRegSP ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegSP( v );
	  }
	  updFieldsSP();
	}
	else if( tf == this.fldRegPC ) {
	  if( v != -1 ) {
	    this.z80cpu.setRegPC( v );
	  }
	  updFieldsPC();
	}
      }
    }
  }


  private void updDebugger()
  {
    if( this.timerForClear.isRunning() )
      this.timerForClear.stop();

    if( this.z80cpu.isDebugEnabled() ) {
      if( this.z80cpu.isPause() ) {
	setDebugStopped();
      } else {
	setDebugRunning();
      }
    } else {
      setDebuggerDisabled();
    }
  }


  private void updFieldsFlag()
  {
    this.btnFlagSign.setSelected( this.z80cpu.getFlagSign() );
    this.btnFlagZero.setSelected( this.z80cpu.getFlagZero() );
    this.btnFlagHalf.setSelected( this.z80cpu.getFlagHalf() );
    this.btnFlagPV.setSelected( this.z80cpu.getFlagPV() );
    this.btnFlagN.setSelected( this.z80cpu.getFlagN() );
    this.btnFlagCarry.setSelected( this.z80cpu.getFlagCarry() );
    this.btnIFF1.setSelected( this.z80cpu.getIFF1() );
    this.btnIFF2.setSelected( this.z80cpu.getIFF2() );
  }


  private void updFieldsAF()
  {
    this.fldRegAF.setText( String.format( "%04X", this.z80cpu.getRegAF() ) );
    this.fldRegAsciiA.setText( getAscii( this.z80cpu.getRegA() ) );
  }


  private void updFieldsAF2()
  {
    this.fldRegAF2.setText( String.format( "%04X", this.z80cpu.getRegAF2() ) );
  }


  private void updFieldsBC()
  {
    int v =  this.z80cpu.getRegBC();
    this.fldRegBC.setText( String.format( "%04X", v ) );
    this.fldRegAsciiB.setText( getAscii( this.z80cpu.getRegB() ) );
    this.fldRegAsciiC.setText( getAscii( this.z80cpu.getRegC() ) );
    this.fldMemBC.setText( getMemInfo( v, 4 ) );
  }


  private void updFieldsBC2()
  {
    this.fldRegBC2.setText( String.format( "%04X", this.z80cpu.getRegBC2() ) );
  }


  private void updFieldsDE()
  {
    int v =  this.z80cpu.getRegDE();
    this.fldRegDE.setText( String.format( "%04X", v ) );
    this.fldRegAsciiD.setText( getAscii( this.z80cpu.getRegD() ) );
    this.fldRegAsciiE.setText( getAscii( this.z80cpu.getRegE() ) );
    this.fldMemDE.setText( getMemInfo( v, 4 ) );
  }


  private void updFieldsDE2()
  {
    this.fldRegDE2.setText( String.format( "%04X", this.z80cpu.getRegDE2() ) );
  }


  private void updFieldsHL()
  {
    int v =  this.z80cpu.getRegHL();
    this.fldRegHL.setText( String.format( "%04X", v ) );
    this.fldRegAsciiH.setText( getAscii( this.z80cpu.getRegH() ) );
    this.fldRegAsciiL.setText( getAscii( this.z80cpu.getRegL() ) );
    this.fldMemHL.setText( getMemInfo( v, 4 ) );
  }


  private void updFieldsHL2()
  {
    this.fldRegHL2.setText( String.format( "%04X", this.z80cpu.getRegHL2() ) );
  }


  private void updFieldsIX()
  {
    int v =  this.z80cpu.getRegIX();
    this.fldRegIX.setText( String.format( "%04X", v ) );
    this.fldRegAsciiIXH.setText( getAscii( this.z80cpu.getRegIXH() ) );
    this.fldRegAsciiIXL.setText( getAscii( this.z80cpu.getRegIXL() ) );
    this.fldMemIX.setText( getMemInfo( v, 6 ) );
  }


  private void updFieldsIY()
  {
    int v = this.z80cpu.getRegIY();
    this.fldRegIY.setText( String.format( "%04X", v ) );
    this.fldRegAsciiIYH.setText( getAscii( this.z80cpu.getRegIYH() ) );
    this.fldRegAsciiIYL.setText( getAscii( this.z80cpu.getRegIYL() ) );
    this.fldMemIY.setText( getMemInfo( v, 6 ) );
  }


  private void updFieldsSP()
  {
    int addr = this.z80cpu.getRegSP();
    this.fldRegSP.setText( String.format( "%04X", addr ) );

    // ersten Eintraege auf dem Stack anzeigen
    int           n   = 4;
    StringBuilder buf = new StringBuilder( n * 5 );

    for( int i = 0; i < n; i++ ) {
      buf.append( String.format(
			"%04X ",
			this.emuThread.getMemWord( addr ) ) );
      addr++;
      addr++;
    }
    this.fldMemSP.setText( buf.toString() );
  }


  private void updFieldsPC()
  {
    int addr = this.z80cpu.getRegPC();
    this.fldRegPC.setText( String.format( "%04X", addr ) );

    // naechsten Befehle anzeigen
    StringBuilder buf = new StringBuilder();

    for( int row = 0; row < this.fldMemPC.getRows(); row++ ) {
      if( row > 0 ) {
	buf.append( (char) '\n' );
      }

      int     b0    = this.emuThread.getMemByte( addr, true );
      boolean b1_m1 = ((b0 == 0xED) || (b0 == 0xDD) || (b0 == 0xFD));

      Z80ReassInstr instr = Z80Reassembler.reassInstruction(
				addr,
				b0,
				this.emuThread.getMemByte( addr + 1, b1_m1 ),
				this.emuThread.getMemByte( addr + 2, false ),
				this.emuThread.getMemByte( addr + 3, false ) );
      if( instr != null ) {

	// Befehlscode ausgeben
	int w = 12;
	int n = instr.getLength();
	for( int i = 0; i < n; i++ ) {
	  buf.append( String.format(
			"%02X ",
			this.emuThread.getMemByte(
					addr,
					(i == 0) || ((i == 1) && b1_m1) ) ) );
	  addr++;
	  w -= 3;
	}
	while( w > 0 ) {
	  buf.append( (char) '\u0020' );
	  --w;
	}

	// Assembler-Befehlsname ausgeben
	String s = instr.getName();
	if( s != null ) {
	  buf.append( s );

	  // Argument ausgeben
	  w = 8 - s.length();
	  s = instr.getArg1();
	  if( s != null ) {
	    while( w > 0 ) {
	      buf.append( (char) '\u0020' );
	      --w;
	    }
	    buf.append( s );

	    s = instr.getArg2();
	    if( s != null ) {
	      buf.append( (char) ',' );
	      buf.append( s );
	    }
	  }
	}

      } else {

	buf.append( String.format(
			"%02X",
			this.emuThread.getMemByte( addr, true ) ) );
	addr++;
      }
    }
    this.fldMemPC.setText( buf.toString() );
  }


  private String getAscii( int value )
  {
    String rv = "";
    if( (value > '\u0020') && (value < '\u007F') )
      rv += (char) value;
    return rv;
  }


  private String getMemInfo( int addr, int numBytes )
  {
    StringBuilder buf = new StringBuilder( (numBytes * 4) + 6 );

    // Hex-Darstellung
    for( int i = 0; i < numBytes; i++ ) {
      buf.append( String.format(
			"%02X ",
			this.emuThread.getMemByte( addr + i, false ) ) );
    }

    // ggf. ASCII-Darstellung ausgeben
    int ch = this.emuThread.getMemByte( addr, false );
    if( (ch >= '\u0020') && (ch <= '\u007E') ) {
      buf.append( " \"" );
      for( int k = 0; k < numBytes; k++ ) {
	ch = this.emuThread.getMemByte( addr + k, false );
	if( (ch < '\u0020') || (ch > '\u007E') )
	  break;
	buf.append( (char) ch );
      }
      buf.append( "...\"" );
    }
    return buf.toString();
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


  private void updActionFields()
  {
    boolean enabled  = false;
    boolean selected = false;
    if( this.listBreakpoint != null ) {
      int idx = this.listBreakpoint.getSelectedIndex();
      if( (idx >= 0) && (idx < this.breakpoints.length) ) {
	selected = true;
	enabled  = this.breakpoints[ idx ].isEnabled();
      }
    }
    this.mnuDebugBreakRemove.setEnabled( selected );
    this.mnuPopupBreakRemove.setEnabled( selected );

    this.mnuDebugBreakEnable.setEnabled( selected && !enabled );
    this.mnuPopupBreakEnable.setEnabled( selected && !enabled );

    this.mnuDebugBreakDisable.setEnabled( selected && enabled );
    this.mnuPopupBreakDisable.setEnabled( selected && enabled );
  }


  private void updBreakpointList( int idxToSelect )
  {
    int n = 0;
    if( this.breakpoints != null ) {
      n = this.breakpoints.length;
      this.listBreakpoint.setListData( this.breakpoints );
    } else {
      this.listBreakpoint.setListData( new Z80Breakpoint[ 0 ] );
    }
    if( (idxToSelect >= 0) && (idxToSelect < n) ) {
      this.listBreakpoint.setSelectedIndex( idxToSelect );
    }
    this.z80cpu.setBreakpoints( this.breakpoints );

    boolean state = (n > 0);
    this.mnuDebugBreakRemoveAll.setEnabled( state );
    this.mnuPopupBreakRemoveAll.setEnabled( state );
    this.mnuDebugBreakEnableAll.setEnabled( state );
    this.mnuPopupBreakEnableAll.setEnabled( state );
    this.mnuDebugBreakDisableAll.setEnabled( state );
    this.mnuPopupBreakDisableAll.setEnabled( state );
  }
}

