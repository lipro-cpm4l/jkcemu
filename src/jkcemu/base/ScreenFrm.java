/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster mit der Bildschirmanzeige
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.SystemTray;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import jkcemu.Main;
import jkcemu.audio.AudioFrm;
import jkcemu.audio.AudioIO;
import jkcemu.audio.AudioRecorderFrm;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.DiskImgCreateFrm;
import jkcemu.disk.DiskImgProcessDlg;
import jkcemu.disk.DiskImgViewFrm;
import jkcemu.disk.DiskUtil;
import jkcemu.disk.DriveSelectDlg;
import jkcemu.disk.FloppyDiskStationFrm;
import jkcemu.etc.ChessboardFrm;
import jkcemu.etc.Plotter;
import jkcemu.etc.PlotterFrm;
import jkcemu.file.Downloader;
import jkcemu.file.FileSelectDlg;
import jkcemu.file.FileUtil;
import jkcemu.file.LoadDlg;
import jkcemu.file.SaveDlg;
import jkcemu.joystick.JoystickFrm;
import jkcemu.image.ImageCaptureFrm;
import jkcemu.image.ImageFrm;
import jkcemu.image.VideoCaptureFrm;
import jkcemu.print.PrintListFrm;
import jkcemu.settings.SettingsFrm;
import jkcemu.text.TextEditFrm;
import jkcemu.text.TextUtil;
import jkcemu.tools.ReassFrm;
import jkcemu.tools.calculator.CalculatorFrm;
import jkcemu.tools.debugger.DebugFrm;
import jkcemu.tools.filebrowser.FileBrowserFrm;
import jkcemu.tools.fileconverter.FileConvertFrm;
import jkcemu.tools.findfiles.FindFilesFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexedit.HexEditFrm;
import jkcemu.tools.hexedit.MemEditFrm;
import jkcemu.usb.USBInterfaceFrm;
import z80emu.Z80Breakpoint;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80Memory;
import z80emu.Z80StatusListener;


public class ScreenFrm
		extends AbstractScreenFrm
		implements
			Downloader.Consumer,
			DropTargetListener,
			Z80StatusListener
{
  public static final String PROP_CONFIRM_NMI       = "confirm.nmi";
  public static final String PROP_CONFIRM_RESET     = "confirm.reset";
  public static final String PROP_CONFIRM_QUIT      = "confirm.quit";
  public static final String PROP_CONFIRM_POWER_ON  = "confirm.power_on";
  public static final String PROP_SCREEN_REFRESH_MS = "screen.refresh.ms";

  public static final boolean DEFAULT_CONFIRM_NMI      = true;
  public static final boolean DEFAULT_CONFIRM_RESET    = false;
  public static final boolean DEFAULT_CONFIRM_POWER_ON = false;
  public static final boolean DEFAULT_CONFIRM_QUIT     = false;

  private static final String TEXT_FULLSCREEN_ON
					= "Vollbildmodus einschalten";
  private static final String TEXT_FULLSCREEN_OFF
					= "Vollbildmodus ausschalten";

  private static final String TEXT_OPEN_SETTINGS = "Einstellungen...";

  private static final String TEXT_MAX_SPEED    = "Maximale Geschwindigkeit";
  private static final String TEXT_STD_SPEED    = "Standard-Geschwindigkeit";
  private static final String PROP_SCREEN_SCALE = "jkcemu.screen.scale";
  private static final String PROP_FULLSCREEN   = "jkcemu.screen.fullscreen";
  private static final String PROP_STATUSBAR_ENABLED
					= "jkcemu.statusbar.enabled";
  private static final String PROP_STATUSMSG_IN_SYSTRAY
					= "jkcemu.statusmsg_in_systray";
  private static final String PROP_TOOLBAR_ENABLED
					= "jkcemu.toolbar.enabled";

  private static final String ACTION_AUDIO             = "audio";
  private static final String ACTION_AUDIORECORDER     = "audiorecorder";
  private static final String ACTION_BASIC_OPEN        = "basic.open";
  private static final String ACTION_BASIC_SAVE        = "basic.save";
  private static final String ACTION_CALCULATOR        = "calculator";
  private static final String ACTION_CHESSBOARD        = "chessboard";
  private static final String ACTION_DEBUGGER          = "debugger";
  private static final String ACTION_DISK_UNPACK       = "disk.unpack";
  private static final String ACTION_DISKIMAGE_BUILD   = "diskimage.build";
  private static final String ACTION_DISKIMAGE_CAPTURE = "diskimage.capture";
  private static final String ACTION_DISKIMAGE_UNPACK  = "diskimage.unpack";
  private static final String ACTION_DISKIMAGE_WRITE   = "diskimage.write";
  private static final String ACTION_DISKVIEWER        = "diskviewer";
  private static final String ACTION_FILEBROWSER       = "filebrowser";
  private static final String ACTION_FILECONVERTER     = "fileconverter";
  private static final String ACTION_FILE_LOAD         = "file.load";
  private static final String ACTION_FILE_SAVE         = "file.save";
  private static final String ACTION_FIND_FILES        = "find_files";
  private static final String ACTION_FLOPPYDISKS       = "floppydisks";
  private static final String ACTION_FULLSCREEN        = "fullscreen";
  private static final String ACTION_HELP_ABOUT        = "help.about";
  private static final String ACTION_HELP_EMUSYS       = "help.emusys";
  private static final String ACTION_HELP_FIND         = "help.find";
  private static final String ACTION_HELP_HOME         = "help.home";
  private static final String ACTION_HELP_INDEX        = "help.index";
  private static final String ACTION_HELP_LICENSE      = "help.license";
  private static final String ACTION_HEXDIFF           = "hexdiff";
  private static final String ACTION_HEXEDITOR         = "hexeditor";
  private static final String ACTION_IMAGEVIEWER       = "imageviewer";
  private static final String ACTION_JOYSTICK          = "joystick";
  private static final String ACTION_KEYBOARD          = "keyboard";
  private static final String ACTION_PLOTTER           = "plotter";
  private static final String ACTION_PRINTER           = "printer";
  private static final String ACTION_PROFILE           = "profile";
  private static final String ACTION_QUIT              = "file.quit";
  private static final String ACTION_MEMEDITOR         = "memeditor";
  private static final String ACTION_NMI               = "nmi";
  private static final String ACTION_PAUSE             = "pause";
  private static final String ACTION_POWER_ON          = "power_on";
  private static final String ACTION_RAMFLOPPIES       = "ramfloppies";
  private static final String ACTION_REASSEMBLER       = "reassembler";
  private static final String ACTION_RESET             = "reset";
  private static final String ACTION_SECOND_SCREEN     = "second_screen";
  private static final String ACTION_SETTINGS          = "extra.settings";
  private static final String ACTION_SPEED             = "speed";
  private static final String ACTION_TEXTEDITOR        = "texteditor";
  private static final String ACTION_USB               = "usb";


  private JMenuBar           mnuBar;
  private JMenu              mnuExtra;
  private JMenu              mnuView;
  private JMenuItem          mnuBasicOpen;
  private JMenuItem          mnuBasicSave;
  private JMenuItem          mnuAudio;
  private JMenuItem          mnuChessboard;
  private JMenuItem          mnuFloppyDisks;
  private JMenuItem          mnuFullScreen;
  private JMenuItem          mnuJoystick;
  private JMenuItem          mnuKeyboard;
  private JMenuItem          mnuPause;
  private JMenuItem          mnuPlotter;
  private JMenuItem          mnuPrintJobs;
  private JMenuItem          mnuRAMFloppies;
  private JMenuItem          mnuSecondScreen;
  private JMenuItem          mnuSpeed;
  private JMenuItem          mnuUSB;
  private JMenuItem          mnuHelpEmuSys;
  private JMenuItem          popupAudio;
  private JMenuItem          popupFloppyDisk;
  private JMenuItem          popupUSB;
  private JMenuItem          popupKeyboard;
  private JMenuItem          popupJoystick;
  private JMenuItem          popupSpeed;
  private JMenuItem          popupPause;
  private JMenuItem          popupFullScreen;
  private JToolBar           toolBar;
  private JCheckBoxMenuItem  mnuStatusBar;
  private JCheckBoxMenuItem  mnuStatusMsgInSysTray;
  private JCheckBoxMenuItem  mnuToolBar;
  private JButton            btnLoad;
  private JButton            btnSave;
  private JButton            btnAudio;
  private JButton            btnChessboard;
  private JButton            btnFloppyDisks;
  private JButton            btnKeyboard;
  private JButton            btnSettings;
  private JButton            btnReset;
  private JLabel             labelStatus;
  private JPanel             panelToolBar;
  private Point              windowLocation;
  private Dimension          windowSize;
  private Dimension          emuSysScreenSize;
  private boolean            fullScreenMode;
  private boolean            fullScreenInfoDone;
  private volatile boolean   chessboardDirty;
  private int                screenScale;
  private javax.swing.Timer  statusRefreshTimer;
  private NumberFormat       speedFmt;
  private SystemTray         sysTray;
  private TrayIcon           trayIcon;
  private EmuSys             oldEmuSys;
  private KeyboardFrm        keyboardFrm;
  private MsgFrm             msgFrm;
  private DebugFrm           primDebugFrm;
  private MemEditFrm         primMemEditFrm;
  private ReassFrm           primReassFrm;
  private DebugFrm           secondDebugFrm;
  private MemEditFrm         secondMemEditFrm;
  private ReassFrm           secondReassFrm;
  private SecondaryScreenFrm secondScreenFrm;


  public ScreenFrm( Properties props )
  {
    setTitle( "JKCEMU" );


    // Initialisierungen
    this.keyboardFrm        = null;
    this.msgFrm             = null;
    this.primDebugFrm       = null;
    this.primMemEditFrm     = null;
    this.primReassFrm       = null;
    this.secondDebugFrm     = null;
    this.secondMemEditFrm   = null;
    this.secondReassFrm     = null;
    this.secondScreenFrm    = null;
    this.sysTray            = null;
    this.trayIcon           = null;
    this.windowLocation     = null;
    this.windowSize         = null;
    this.emuSysScreenSize   = null;
    this.fullScreenMode     = false;
    this.fullScreenInfoDone = false;
    this.chessboardDirty    = false;
    this.oldEmuSys          = null;
    this.emuThread          = new EmuThread( this, props );
    this.speedFmt           = NumberFormat.getNumberInstance();
    if( this.speedFmt instanceof DecimalFormat ) {
      ((DecimalFormat) this.speedFmt).applyPattern( "###,###,##0.0#" );
    }
    this.screenScale = EmuUtil.getIntProperty(
				props,
				PROP_SCREEN_SCALE,
				1 );
    if( this.screenScale < 1 ) {
      this.screenScale = 1;
    }


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    mnuFile.add( createMenuItemWithNonControlAccelerator(
				EmuUtil.TEXT_OPEN_LOAD,
				ACTION_FILE_LOAD,
				KeyEvent.VK_L ) );
    mnuFile.add( createMenuItemWithNonControlAccelerator(
				EmuUtil.TEXT_OPEN_SAVE,
				ACTION_FILE_SAVE,
				KeyEvent.VK_S ) );
    mnuFile.addSeparator();
    this.mnuBasicOpen = createMenuItemWithNonControlAccelerator(
		"BASIC-Programm im Texteditor \u00F6ffnen...",
		ACTION_BASIC_OPEN,
		KeyEvent.VK_T,
		true );
    mnuFile.add( this.mnuBasicOpen );

    this.mnuBasicSave = createMenuItemWithNonControlAccelerator(
				"BASIC-Programm speichern...",
				ACTION_BASIC_SAVE,
				KeyEvent.VK_S,
				true );
    mnuFile.add( this.mnuBasicSave );
    mnuFile.addSeparator();

    this.mnuRAMFloppies = createMenuItem(
				"RAM-Floppies...",
				ACTION_RAMFLOPPIES );
    mnuFile.add( this.mnuRAMFloppies );

    this.mnuFloppyDisks = createMenuItem(
				"Diskettenstation...",
				ACTION_FLOPPYDISKS );
    mnuFile.add( this.mnuFloppyDisks );
    mnuFile.addSeparator();

    // Untermenu Bildschirmausgabe
    JMenu mnuScreen = createScreenMenu( true );
    if( mnuScreen != null ) {
      mnuFile.add( mnuScreen );
      mnuFile.addSeparator();
    }

    mnuFile.add( createMenuItemWithNonControlAccelerator(
				"Texteditor/Programmierung...",
				ACTION_TEXTEDITOR,
				KeyEvent.VK_T ) );
    mnuFile.add( createMenuItemWithNonControlAccelerator(
				"Datei-Browser...",
				ACTION_FILEBROWSER,
				KeyEvent.VK_B,
				true ) );
    mnuFile.add( createMenuItemWithNonControlAccelerator(
				"Dateien suchen...",
				ACTION_FIND_FILES,
				KeyEvent.VK_F ) );
    mnuFile.addSeparator();
    mnuFile.add( createMenuItem( "Beenden", ACTION_QUIT ) );


    // Menu Bearbeiten
    JMenu mnuEdit = createEditMenu( true, true );


    // Menu Ansicht
    this.mnuView = createScaleMenu();
    this.mnuView.setMnemonic( KeyEvent.VK_A );
    this.mnuView.addSeparator();

    this.mnuToolBar = GUIFactory.createCheckBoxMenuItem(
						"Werkzeugleiste",
						true );
    this.mnuToolBar.addActionListener( this );
    this.mnuView.add( this.mnuToolBar );

    this.mnuStatusBar = GUIFactory.createCheckBoxMenuItem(
						"Statuszeile",
						true );
    this.mnuStatusBar.addActionListener( this );
    this.mnuView.add( this.mnuStatusBar );

    if( SystemTray.isSupported() ) {
      this.mnuStatusMsgInSysTray = GUIFactory.createCheckBoxMenuItem(
			"Statusmeldungen in der Taskleise anzeigen",
			true );
      this.mnuStatusMsgInSysTray.addActionListener( this );
      this.mnuView.add( this.mnuStatusMsgInSysTray );
    } else {
      this.mnuStatusMsgInSysTray = null;
    }


    // Menu Extra
    this.mnuExtra = GUIFactory.createMenu( "Extra" );
    this.mnuExtra.setMnemonic( KeyEvent.VK_E );

    this.mnuAudio = createMenuItemWithNonControlAccelerator(
				"Audio/Kassette...",
				ACTION_AUDIO,
				KeyEvent.VK_A,
				true );
    this.mnuExtra.add( this.mnuAudio );

    this.mnuKeyboard = createMenuItemWithNonControlAccelerator(
				"Tastatur...",
				ACTION_KEYBOARD,
				KeyEvent.VK_K );
    this.mnuExtra.add( this.mnuKeyboard );

    this.mnuJoystick = createMenuItemWithNonControlAccelerator(
				"Joysticks...",
				ACTION_JOYSTICK,
				KeyEvent.VK_J );
    this.mnuExtra.add( this.mnuJoystick );

    this.mnuPrintJobs = createMenuItem(
				"Druckauftr\u00E4ge...",
				ACTION_PRINTER );
    this.mnuExtra.add( this.mnuPrintJobs );

    this.mnuPlotter = createMenuItem(
				"Plotter...",
				ACTION_PLOTTER );
    this.mnuExtra.add( this.mnuPlotter );

    this.mnuUSB = createMenuItemWithNonControlAccelerator(
				"USB-Anschluss...",
				ACTION_USB,
				KeyEvent.VK_U );
    this.mnuExtra.add( this.mnuUSB );

    this.mnuChessboard = createMenuItem(
				"Schachbrett...",
				ACTION_CHESSBOARD );
    this.mnuExtra.add( this.mnuChessboard );

    this.mnuSecondScreen = createMenuItem(
				"Zweite Anzeigeeinheit...",
				ACTION_SECOND_SCREEN );
    this.mnuExtra.add( this.mnuSecondScreen );
    this.mnuExtra.addSeparator();

    this.mnuExtra.add( createMenuItem(
				"Bildschirmfoto...",
				ACTION_SCREENSHOT ) );
    this.mnuExtra.add( createMenuItem(
				"Bildschirmvideo...",
				ACTION_SCREENVIDEO ) );

    JMenu mnuExtraTools = GUIFactory.createMenu( "Werkzeuge" );
    mnuExtraTools.add( createMenuItemWithNonControlAccelerator(
				"Debugger...",
				ACTION_DEBUGGER,
				KeyEvent.VK_D,
				true ) );
    mnuExtraTools.add( createMenuItemWithNonControlAccelerator(
				"Reassembler...",
				ACTION_REASSEMBLER,
				KeyEvent.VK_R,
				true ) );
    mnuExtraTools.add( createMenuItemWithNonControlAccelerator(
				"Speichereditor...",
				ACTION_MEMEDITOR,
				KeyEvent.VK_M,
				true ) );
    mnuExtraTools.add( createMenuItemWithNonControlAccelerator(
				"Hex-Editor...",
				ACTION_HEXEDITOR,
				KeyEvent.VK_H,
				true ) );
    mnuExtraTools.add( createMenuItem(
				"Hex-Dateivergleicher...",
				ACTION_HEXDIFF ) );
    mnuExtraTools.add( createMenuItem(
				"Dateikonverter...",
				ACTION_FILECONVERTER ) );
    mnuExtraTools.add( createMenuItem(
				"Bildbetrachter/Bildbearbeitung...",
				ACTION_IMAGEVIEWER ) );
    mnuExtraTools.add( createMenuItem(
				"Audiorecorder...",
				ACTION_AUDIORECORDER ) );
    mnuExtraTools.add( createMenuItem( "Rechner...", ACTION_CALCULATOR ) );
    mnuExtraTools.add( createMenuItem(
			"Diskettenabbilddatei-Inspektor...",
			ACTION_DISKVIEWER ) );
    mnuExtraTools.addSeparator();
    mnuExtraTools.add( createMenuItem(
			"CP/M-Diskettenabbilddatei manuell erstellen...",
			ACTION_DISKIMAGE_BUILD ) );
    mnuExtraTools.add( createMenuItem(
			"CP/M-Diskettenabbilddatei entpacken...",
			ACTION_DISKIMAGE_UNPACK ) );
    mnuExtraTools.add( createMenuItem(
			"CP/M-Diskette entpacken...",
			ACTION_DISK_UNPACK ) );
    mnuExtraTools.addSeparator();
    mnuExtraTools.add( createMenuItem(
			"Abbilddatei von Datentr\u00E4ger erstellen...",
			ACTION_DISKIMAGE_CAPTURE ) );
    mnuExtraTools.add( createMenuItem(
			"Abbilddatei auf Datentr\u00E4ger schreiben...",
			ACTION_DISKIMAGE_WRITE ) );
    this.mnuExtra.add( mnuExtraTools );
    this.mnuExtra.addSeparator();

    this.mnuExtra.add( createMenuItem(
				TEXT_OPEN_SETTINGS,
				ACTION_SETTINGS ) );
    this.mnuExtra.add( createMenuItem(
				"Profil anwenden...",
				ACTION_PROFILE ) );
    this.mnuExtra.addSeparator();

    this.mnuFullScreen = createMenuItem(
				TEXT_FULLSCREEN_ON,
				ACTION_FULLSCREEN );
    this.mnuExtra.add( this.mnuFullScreen );

    this.mnuSpeed = createMenuItemWithNonControlAccelerator(
				TEXT_MAX_SPEED,
				ACTION_SPEED,
				KeyEvent.VK_G );
    this.mnuExtra.add( this.mnuSpeed );

    this.mnuPause = createMenuItemWithNonControlAccelerator(
				"Pause",
				ACTION_PAUSE,
				KeyEvent.VK_P );
    this.mnuExtra.add( this.mnuPause );

    this.mnuExtra.add( createMenuItemWithNonControlAccelerator(
				"NMI ausl\u00F6sen",
				ACTION_NMI,
				KeyEvent.VK_N ) );
    this.mnuExtra.add( createMenuItemWithNonControlAccelerator(
				"Zur\u00FCcksetzen (RESET)",
				ACTION_RESET,
				KeyEvent.VK_R ) );
    this.mnuExtra.add( createMenuItemWithNonControlAccelerator(
				"Einschalten (Power On)",
				ACTION_POWER_ON,
				KeyEvent.VK_I ) );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();
    mnuHelp.add( createMenuItem( "\u00DCbersicht...", ACTION_HELP_HOME ) );
    mnuHelp.add( createMenuItem( "Index...", ACTION_HELP_INDEX ) );
    mnuHelp.add(
	createMenuItem( "Hilfe durchsuchen...", ACTION_HELP_FIND ) );

    this.mnuHelpEmuSys = createMenuItem(
				"Hilfe zum emulierten System...",
				ACTION_HELP_EMUSYS );
    this.mnuHelpEmuSys.setEnabled( false );
    mnuHelp.add( this.mnuHelpEmuSys );

    mnuHelp.addSeparator();
    mnuHelp.add( createMenuItem(
				"\u00DCber JKCEMU...",
				ACTION_HELP_ABOUT ) );
    mnuHelp.add( createMenuItem(
				"Lizenzbestimmungen...",
				ACTION_HELP_LICENSE ) );


    // Menu zusammenbauen
    this.mnuBar = GUIFactory.createMenuBar(
					mnuFile,
					mnuEdit,
					this.mnuView,
					this.mnuExtra,
					mnuHelp );
    setJMenuBar( this.mnuBar );


    // Popup-Menu
    createPopupMenu( true, true );
    this.popupMnu.add( createMenuItem(
				EmuUtil.TEXT_OPEN_LOAD,
				ACTION_FILE_LOAD ) );
    this.popupMnu.add( createMenuItem(
				EmuUtil.TEXT_OPEN_SAVE,
				ACTION_FILE_SAVE ) );
    this.popupMnu.addSeparator();

    this.popupAudio = createMenuItem(
				"Audio/Kassette...",
				ACTION_AUDIO );
    this.popupMnu.add( this.popupAudio );

    this.popupFloppyDisk = createMenuItem(
				"Diskettenstation...",
				ACTION_FLOPPYDISKS );
    this.popupMnu.add( this.popupFloppyDisk );

    this.popupUSB = createMenuItem(
				"USB-Anschluss...",
				ACTION_USB );
    this.popupMnu.add( this.popupUSB );

    this.popupKeyboard = createMenuItem(
				"Tastatur...",
				ACTION_KEYBOARD );
    this.popupMnu.add( this.popupKeyboard );

    this.popupJoystick = createMenuItem(
				"Joysticks...",
				ACTION_JOYSTICK );
    this.popupMnu.add( this.popupJoystick );
    this.popupMnu.addSeparator();

    popupMnu.add( createMenuItem(
				TEXT_OPEN_SETTINGS,
				ACTION_SETTINGS ) );
    this.popupMnu.addSeparator();

    this.popupFullScreen = createMenuItem(
				TEXT_FULLSCREEN_ON,
				ACTION_FULLSCREEN );
    this.popupMnu.add( this.popupFullScreen );

    this.popupSpeed = createMenuItem(
				TEXT_MAX_SPEED,
				ACTION_SPEED );
    this.popupMnu.add( this.popupSpeed );

    this.popupPause = createMenuItem(
				"Pause",
				ACTION_PAUSE );
    this.popupMnu.add( this.popupPause );
    this.popupMnu.addSeparator();

    this.popupMnu.add( createMenuItem(
				"Zur\u00FCcksetzen (RESET)",
				ACTION_RESET ) );
    this.popupMnu.add( createMenuItem(
				"Einschalten (Power On)",
				ACTION_POWER_ON ) );
    this.popupMnu.addSeparator();
    this.popupMnu.add( createMenuItem( "Beenden", ACTION_QUIT ) );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );


    // Werkzeugleiste
    this.panelToolBar = GUIFactory.createPanel(
                new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( this.panelToolBar, gbc );

    this.toolBar = GUIFactory.createToolBar();
    this.toolBar.setFloatable( false );
    this.toolBar.setBorderPainted( false );
    this.toolBar.setOrientation( JToolBar.HORIZONTAL );
    this.toolBar.setRollover( true );
    this.panelToolBar.add( this.toolBar );

    this.btnLoad = GUIFactory.createRelImageResourceButton(
						this,
						"file/open.png",
						EmuUtil.TEXT_LOAD );
    this.btnLoad.setActionCommand( ACTION_FILE_LOAD );

    this.btnSave = GUIFactory.createRelImageResourceButton(
						this,
						"file/save.png",
						EmuUtil.TEXT_SAVE );
    this.btnSave.setActionCommand( ACTION_FILE_SAVE );

    this.btnCopy = GUIFactory.createRelImageResourceButton(
						this,
						"edit/copy.png",
						EmuUtil.TEXT_COPY );
    this.btnCopy.setActionCommand( ACTION_COPY );
    this.btnCopy.setEnabled( false );

    this.btnPaste = GUIFactory.createRelImageResourceButton(
						this,
						"edit/paste.png",
						EmuUtil.TEXT_PASTE );
    this.btnPaste.setActionCommand( ACTION_PASTE );
    this.btnPaste.setEnabled( false );

    this.btnAudio = GUIFactory.createRelImageResourceButton(
						this,
						"audio/audio.png",
						"Audio" );
    this.btnAudio.setActionCommand( ACTION_AUDIO );

    this.btnChessboard = GUIFactory.createRelImageResourceButton(
						this,
						"etc/chessboard.png",
						"Schachbrett" );
    this.btnChessboard.setActionCommand( ACTION_CHESSBOARD );

    this.btnFloppyDisks = GUIFactory.createRelImageResourceButton(
						this,
						"disk/floppydiskstation.png",
						"Diskettenstation" );
    this.btnFloppyDisks.setActionCommand( ACTION_FLOPPYDISKS );

    this.btnKeyboard = GUIFactory.createRelImageResourceButton(
						this,
						"etc/keyboard.png",
						"Tastatur" );
    this.btnKeyboard.setActionCommand( ACTION_KEYBOARD );

    this.btnSettings = GUIFactory.createRelImageResourceButton(
						this,
						"edit/settings.png",
						TEXT_OPEN_SETTINGS );
    this.btnSettings.setActionCommand( ACTION_SETTINGS );

    this.btnReset = GUIFactory.createRelImageResourceButton(
					this,
					"file/reset.png",
					"Zur\u00FCcksetzen (RESET)" );
    this.btnReset.setActionCommand( ACTION_RESET );


    // Bildschirmausgabe
    this.screenFld = new ScreenFld( this );
    this.screenFld.setFocusable( true );
    this.screenFld.setFocusTraversalKeysEnabled( false );
    this.screenFld.addKeyListener( this );
    this.screenFld.addMouseListener( this );

    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( this.screenFld, gbc );


    // Statuszeile
    this.labelStatus  = GUIFactory.createLabel( "Bereit" );
    gbc.anchor        = GridBagConstraints.WEST;
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.insets.left   = 5;
    gbc.insets.top    = 5;
    gbc.insets.bottom = 5;
    gbc.weighty       = 0.0;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Drop-Ziel
    (new DropTarget( this.screenFld, this )).setActive( true );


    // Sonstiges
    setScreenScale( this.screenScale );
    updPasteBtns();
    updPauseBtn();
    updToolBar( false );
    updStatusBar( false );
    emuSysChangedInternal( getEmuSys(), props );
    this.emuThread.getZ80CPU().addStatusListener( this );
    this.statusRefreshTimer = new javax.swing.Timer( 1000, this );
    this.statusRefreshTimer.start();
    if( EmuUtil.getBooleanProperty( props, PROP_FULLSCREEN, false ) ) {
      this.fullScreenInfoDone = true;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    setFullScreenMode( true );
		  }
		} );
    }


    // Listener
    this.btnLoad.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnCopy.addActionListener( this );
    this.btnPaste.addActionListener( this );
    this.btnAudio.addActionListener( this );
    this.btnChessboard.addActionListener( this );
    this.btnFloppyDisks.addActionListener( this );
    this.btnKeyboard.addActionListener( this );
    this.btnSettings.addActionListener( this );
    this.btnReset.addActionListener( this );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn sich ein untergeordnetes Fenster schliesst.
   */
  public void childFrameClosed( Frame frm )
  {
    if( frm != null ) {
      if( frm == this.keyboardFrm ) {
	this.keyboardFrm = null;
      } else if( frm == this.secondScreenFrm ) {
	this.secondScreenFrm = null;
      }
    }
  }


  public BufferedImage createSnapshot()
  {
    return this.screenFld.createBufferedImage();
  }


  public void fireAppendMsg( final String msg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    appendMsg( msg );
		  }
		} );
  }


  public void fireOpenSecondScreen()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    doExtraSecondScreen();
		  }
		} );
  }


  public void fireReset( final boolean powerOn )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    execReset( powerOn );
		  }
		} );
  }


  public void fireScreenSizeChanged()
  {
    this.windowSize       = null;
    this.emuSysScreenSize = null;
    if( !this.fullScreenMode ) {
      super.fireScreenSizeChanged();
    }
  }


  public void fireShowStatusText( final String text )
  {
    if( text != null ) {
      if( !text.isEmpty() ) {
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    showStatusText( text );
		  }
		} );
      }
    }
  }


  public EmuThread getEmuThread()
  {
    return this.emuThread;
  }


  public EmuSys getEmuSys()
  {
    return this.emuThread.getEmuSys();
  }


  public boolean isFullScreenMode()
  {
    return this.fullScreenMode;
  }


  public DebugFrm openPrimaryDebugger()
  {
    if( this.primDebugFrm != null ) {
      EmuUtil.showFrame( this.primDebugFrm );
    } else {
      if( this.emuThread != null ) {
	this.primDebugFrm = new DebugFrm(
				this.emuThread,
				this.emuThread.getZ80CPU(),
				this.emuThread );
	EmuUtil.showFrame( this.primDebugFrm );
      }
    }
    return this.primDebugFrm;
  }


  public ReassFrm openPrimaryReassembler()
  {
    if( this.primReassFrm != null ) {
      EmuUtil.showFrame( this.primReassFrm );
    } else {
      if( this.emuThread != null ) {
	this.primReassFrm = new ReassFrm( this, this.emuThread );
	EmuUtil.showFrame( this.primReassFrm );
      }
    }
    return this.primReassFrm;
  }


  public DebugFrm openSecondDebugger()
  {
    DebugFrm debugFrm = null;
    if( this.emuThread != null ) {
      EmuSys    emuSys    = this.emuThread.getEmuSys();
      Z80CPU    secondCPU = emuSys.getSecondZ80CPU();
      Z80Memory secondMem = emuSys.getSecondZ80Memory();
      if( (secondCPU != null) && (secondMem != null) ) {
	if( this.secondDebugFrm != null ) {
	  EmuUtil.showFrame( this.secondDebugFrm );
	} else {
	  this.secondDebugFrm = new DebugFrm(
					this.emuThread, 
					secondCPU,
					secondMem );
	  String secondSysName = emuSys.getSecondSystemName();
	  if( secondSysName != null ) {
	    this.secondDebugFrm.setTitle(
		      "JKCEMU Debugger: " + secondSysName );
	  } else {
	    this.secondDebugFrm.setTitle(
		      "JKCEMU Debugger: Sekund\u00E4rsystem" );
	  }
	  EmuUtil.showFrame( this.secondDebugFrm );
	}
	debugFrm = this.secondDebugFrm;
      }
    }
    return debugFrm;
  }


  public ReassFrm openSecondReassembler()
  {
    EmuSys emuSys = getEmuSys();
    return openSecondReassembler(
			emuSys.getSecondZ80Memory(),
			emuSys.getSecondSystemName() );
  }


  public void openAudioInFile(
			File   file,
			byte[] fileBytes,
			int    offs )
  {
    AudioFrm.open( this ).openFile( file, fileBytes, offs );
  }


  public void openAudioInFile( File file )
  {
    AudioFrm.open( this ).openFile( file, null, 0 );
  }



  public TextEditFrm openText( String text )
  {
    TextEditFrm frm = TextEditFrm.open( this.emuThread );
    if( frm != null ) {
      frm.openText( text );
    }
    return frm;
  }


  public void setChessboardDirty( boolean state )
  {
    this.chessboardDirty = state;
  }


  public boolean setMaxSpeed( Component owner, boolean state )
  {
    boolean rv = false;
    if( this.emuThread != null ) {
      if( state && AudioIO.isCPUSynchronLineOpen() ) {
	BaseDlg.showInfoDlg(
		owner,
		"Es ist ein Audiokanal ge\u00F6ffnet,"
			+ " der den emulierten Mikroprozessor bremst.\n"
			 + "Solange dieser Audiokanal ge\u00F6ffnet ist,"
			+ " kann nicht auf maximale Geschwindigkeit"
			+ " geschaltet werden." );
      } else {
	Z80CPU z80cpu = this.emuThread.getZ80CPU();
	if( z80cpu != null ) {
	  z80cpu.setBrakeEnabled( !state );
	  EmuSys emuSys = this.emuThread.getEmuSys();
	  if( emuSys != null ) {
	    z80cpu = emuSys.getSecondZ80CPU();
	    if( z80cpu != null ) {
	      z80cpu.setBrakeEnabled( !state );
	    }
	  }
	  if( state ) {
	    this.mnuSpeed.setText( TEXT_STD_SPEED );
	    this.popupSpeed.setText( TEXT_STD_SPEED );
	  } else {
	    this.mnuSpeed.setText( TEXT_MAX_SPEED );
	    this.popupSpeed.setText( TEXT_MAX_SPEED );
	  }
	  rv = true;
	}
      }
    }
    return rv;
  }


  public void showStatusText( String text )
  {
    if( this.labelStatus.isVisible() ) {
      this.statusRefreshTimer.stop();
      this.labelStatus.setText( text );
      this.statusRefreshTimer.setInitialDelay( 5000 );	// 5 sec. anzeigen
      this.statusRefreshTimer.restart();
    } else {
      if( this.trayIcon != null ) {
	this.trayIcon.displayMessage(
			Main.APPNAME + " Mitteilung",
			text,
			TrayIcon.MessageType.NONE );
      }
    }
  }


  public void startEmulationThread()
  {
    if( this.emuThread != null )
      this.emuThread.start();
  }


	/* --- Downloader.Consumer --- */

  @Override
  public void consume( byte[] fileBytes, String fileName )
  {
    LoadDlg.loadFile(
		this,		// owner
		this,		// ScreenFrm
		null,		// file
		fileName,
		fileBytes,
		true,		// interactive
		true,		// startEnabled
		true );		// startsSelected
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !FileUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    final File file = FileUtil.fileDrop( this, e );
    if( file != null ) {
      if( !Downloader.checkAndStart(
			this,
			file,
			0x30000,	// bei HEX-Datei notwendig
			true,		// GZip-Dateien entpacken
			e,
			this ) )
      {
	final ScreenFrm screenFrm = this;
	EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    LoadDlg.loadFile(
					screenFrm,
					screenFrm,
					file,
					null,		// fileName
					null,		// fileBytes
					true,		// interactive
					true,		// startEnabled
					true );		// startsSelected
			  }
			} );
      }
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !FileUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- Z80StatusListener --- */

  @Override
  public void z80StatusChanged(
			Z80Breakpoint      breakpoint,
			Z80InterruptSource iSource )
  {
    if( this.emuThread != null ) {
      Z80CPU z80cpu = this.emuThread.getZ80CPU();
      if( z80cpu != null ) {
	if( !z80cpu.isActive() || z80cpu.isPause() ) {
	  this.emuThread.closeAudioLines();
	}
      }
    }
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updPauseBtn();
		  }
		} );
  }


	/* --- ueberschriebene Methoden fuer ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.screenRefreshTimer ) {
	if( this.chessboardDirty ) {
	  ChessboardFrm.repaintChessboard();
	}
	if( this.screenDirty ) {
	  this.screenFld.repaint();
	}
      }
      else if( src == this.statusRefreshTimer ) {
	refreshStatus();
      } else {
	super.actionPerformed( e );
      }
    }
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( (newEmuSys != null) && (newEmuSys != this.oldEmuSys) )
      emuSysChangedInternal( newEmuSys, newProps );
  }


	/* --- ueberschriebene Methoden fuer MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (this.trayIcon != null) && (this.trayIcon == e.getSource())
	&& (e.getButton() == MouseEvent.BUTTON1) )
    {
      toFront();
      e.consume();
    }
  }


	/* --- ueberschriebene Methoden fuer WindowListener --- */

  @Override
  public void windowActivated( WindowEvent e )
  {
    super.windowActivated( e );
    if( e.getWindow() == this ) {
      Main.setWindowActivated( Main.WINDOW_MASK_SCREEN );
    }
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
    super.windowDeactivated( e );
    if( e.getWindow() == this ) {
      Main.setWindowDeactivated( Main.WINDOW_MASK_SCREEN );
      if( this.emuThread != null ) {
	this.emuThread.keyReleased();
      }
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    super.windowOpened( e );
    if( e.getWindow() == this ) {
      this.screenFld.requestFocus();
      if( Main.isFirstExecution() ) {
	SettingsFrm.open( this );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props )
  {
    // Bildschirmaktualisierung
    updScreenRefreshSettings( props );

    // Werkzeugleiste
    this.mnuToolBar.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				PROP_TOOLBAR_ENABLED,
				true ) );
    updToolBar( false );

    // Statuszeile
    this.mnuStatusBar.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				PROP_STATUSBAR_ENABLED,
				true ) );
    if( this.mnuStatusMsgInSysTray != null ) {
      this.mnuStatusMsgInSysTray.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				PROP_STATUSMSG_IN_SYSTRAY,
				true ) );
    }
    updStatusBar( false );

    // Fenstergroesse und -position
    return (super.applySettings( props )
				|| updScreenSize( props ));
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e instanceof ActionEvent ) {
      String actionCmd = ((ActionEvent) e).getActionCommand();
      if( actionCmd != null ) {
	if( actionCmd.equals( ACTION_QUIT ) ) {
	  rv = true;
	  doClose();
	}
	else if( actionCmd.equals( ACTION_FILE_LOAD ) ) {
	  rv = true;
	  doFileLoad( true );
	}
	else if( actionCmd.equals( ACTION_FILE_SAVE ) ) {
	  rv = true;
	  doFileSave();
	}
	else if( actionCmd.equals( ACTION_RAMFLOPPIES ) ) {
	  rv = true;
	  doRAMFloppies();
	}
	else if( actionCmd.equals( ACTION_FLOPPYDISKS ) ) {
	  rv = true;
	  doFloppyDisk();
	}
	else if( actionCmd.equals( ACTION_BASIC_OPEN ) ) {
	  rv = true;
	  getEmuSys().openBasicProgram();
	}
	else if( actionCmd.equals( ACTION_BASIC_SAVE ) ) {
	  rv = true;
	  getEmuSys().saveBasicProgram();
	}
	else if( actionCmd.equals( ACTION_SCREENIMAGE_SAVE ) ) {
	  rv = true;
	  doFileScreenImageSave();
	}
	else if( actionCmd.equals( ACTION_SCREENTEXT_SAVE ) ) {
	  rv = true;
	  doFileScreenTextSave();
	}
	else if( actionCmd.equals( ACTION_FIND_FILES ) ) {
	  rv = true;
	  FindFilesFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_FILEBROWSER ) ) {
	  rv = true;
	  FileBrowserFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_TEXTEDITOR ) ) {
	  rv = true;
	  TextEditFrm.open( this.emuThread );
	}
	else if( actionCmd.equals( ACTION_AUDIO ) ) {
	  rv = true;
	  AudioFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_CHESSBOARD ) ) {
	  rv = true;
	  doExtraChessboard();
	}
	else if( actionCmd.equals( ACTION_JOYSTICK ) ) {
	  rv = true;
	  doExtraJoystick();
	}
	else if( actionCmd.equals( ACTION_KEYBOARD ) ) {
	  rv = true;
	  doExtraKeyboard();
	}
	else if( actionCmd.equals( ACTION_PLOTTER ) ) {
	  rv = true;
	  doExtraPlotter();
	}
	else if( actionCmd.equals( ACTION_PRINTER ) ) {
	  rv = true;
	  PrintListFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_USB ) ) {
	  rv = true;
	  doExtraUSB();
	}
	else if( actionCmd.equals( ACTION_SCREENSHOT ) ) {
	  rv = true;
	  ImageCaptureFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_SCREENVIDEO ) ) {
	  rv = true;
	  VideoCaptureFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_SECOND_SCREEN ) ) {
	  rv = true;
	  doExtraSecondScreen();
	}
	else if( actionCmd.equals( ACTION_IMAGEVIEWER ) ) {
	  rv = true;
	  ImageFrm.open();
	}
	else if( actionCmd.equals( ACTION_AUDIORECORDER ) ) {
	  rv = true;
	  AudioRecorderFrm.open();
	}
	else if( actionCmd.equals( ACTION_DEBUGGER ) ) {
	  rv = true;
	  doExtraDebugger();
	}
	else if( actionCmd.equals( ACTION_MEMEDITOR ) ) {
	  rv = true;
	  doExtraMemEditor();
	}
	else if( actionCmd.equals( ACTION_REASSEMBLER ) ) {
	  rv = true;
	  doExtraReassembler();
	}
	else if( actionCmd.equals( ACTION_CALCULATOR ) ) {
	  rv = true;
	  CalculatorFrm.open();
	}
	else if( actionCmd.equals( ACTION_HEXDIFF ) ) {
	  rv = true;
	  HexDiffFrm.open();
	}
	else if( actionCmd.equals( ACTION_HEXEDITOR ) ) {
	  rv = true;
	  HexEditFrm.open();
	}
	else if( actionCmd.equals( ACTION_FILECONVERTER ) ) {
	  rv = true;
	  FileConvertFrm.open();
	}
	else if( actionCmd.equals( ACTION_DISKIMAGE_BUILD ) ) {
	  rv = true;
	  DiskImgCreateFrm.open();
	}
	else if( actionCmd.equals( ACTION_DISKIMAGE_UNPACK ) ) {
	  rv = true;
	  doExtraDiskImgUnpack();
	}
	else if( actionCmd.equals( ACTION_DISK_UNPACK ) ) {
	  rv = true;
	  doExtraDiskUnpack();
	}
	else if( actionCmd.equals( ACTION_DISKIMAGE_CAPTURE ) ) {
	  rv = true;
	  DiskImgProcessDlg.createDiskImageFromDrive( this );
	}
	else if( actionCmd.equals( ACTION_DISKIMAGE_WRITE ) ) {
	  rv = true;
	  DiskImgProcessDlg.writeDiskImageToDrive( this );
	}
	else if( actionCmd.equals( ACTION_DISKVIEWER ) ) {
	  rv = true;
	  DiskImgViewFrm.open();
	}
	else if( actionCmd.equals( ACTION_SETTINGS ) ) {
	  rv = true;
	  SettingsFrm.open( this );
	}
	else if( actionCmd.equals( ACTION_PROFILE ) ) {
	  rv = true;
	  doExtraProfile();
	}
	else if( actionCmd.equals( ACTION_FULLSCREEN ) ) {
	  rv = true;
	  if( e.getSource() == this.popupFullScreen ) {
	    this.fullScreenInfoDone = true;
	  }
	  setFullScreenMode( !this.fullScreenMode );
	}
	else if( actionCmd.equals( ACTION_SPEED ) ) {
	  rv = true;
	  doExtraSpeed();
	}
	else if( actionCmd.equals( ACTION_PAUSE ) ) {
	  rv = true;
	  doExtraPause();
	}
	else if( actionCmd.equals( ACTION_NMI ) ) {
	  rv = true;
	  doExtraNMI();
	}
	else if( actionCmd.equals( ACTION_RESET ) ) {
	  rv = true;
	  doExtraReset();
	}
	else if( actionCmd.equals( ACTION_POWER_ON ) ) {
	  rv = true;
	  doExtraPowerOn();
	}
	else if( actionCmd.equals( ACTION_HELP_HOME ) ) {
	  rv = true;
	  HelpFrm.openPage( HelpFrm.PAGE_HOME );
	}
	else if( actionCmd.equals( ACTION_HELP_INDEX ) ) {
	  rv = true;
	  HelpFrm.openPage( HelpFrm.PAGE_INDEX );
	}
	else if( actionCmd.equals( ACTION_HELP_FIND) ) {
	  rv = true;
	  HelpFrm.openFindInHelp();
	}
	else if( actionCmd.equals( ACTION_HELP_EMUSYS ) ) {
	  rv = true;
	  doHelpEmuSys();
	}
	else if( actionCmd.equals( ACTION_HELP_ABOUT ) ) {
	  rv = true;
	  AboutDlg.fireOpen( this );
	}
	else if( actionCmd.equals( ACTION_HELP_LICENSE ) ) {
	  rv = true;
	  HelpFrm.openPage( HelpFrm.PAGE_LICENSE );
	}
      }
    }
    if( !rv ) {
      Object src = e.getSource();
      if( src == this.mnuToolBar ) {
	rv = true;
	doViewToolBar();
      } else if( src == this.mnuStatusBar ) {
	rv = true;
	doViewStatusBar();
      } else if( src == this.mnuStatusMsgInSysTray ) {
	rv = true;
	doViewStatusMsgInSysTray();
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    this.screenFld.requestFocus();
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = true;
    if( Main.getBooleanProperty(
		PROP_PREFIX + PROP_CONFIRM_QUIT,
		DEFAULT_CONFIRM_QUIT ) )
    {
      if( !BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie den Emulator jetzt beenden?",
		"Best\u00E4tigung" ) )
      {
	rv = false;
      }
    }
    if( rv ) {
      rv = doQuit();
    }
    return rv;
  }


  @Override
  public boolean doQuit()
  {
    // untergeordnete Fenster schliessen
    boolean rv = EmuUtil.closeOtherFrames( this );
    if( rv ) {

      // Programmbeendigung nicht durch Exception verhindern lassen
      try {

	// Threads beenden
	if( this.emuThread != null ) {
	  this.emuThread.stopEmulator();
	  Main.getThreadGroup().interrupt();

	  // max. eine Sekunde auf Thread-Beendigung warten
	  try {
	    this.emuThread.join( 1000 );
	  }
	  catch( InterruptedException ex ) {}
	}
	rv = super.doClose();
      }
      catch( Exception ex ) {}
    }
    if( rv ) {
      Main.exitSuccess();
    }
    return rv;
  }


  @Override
  protected void doScreenScale( int screenScale )
  {
    this.screenScale = screenScale;
    this.screenFld.setScreenScale( screenScale );
    if( !this.fullScreenMode ) {
      pack();
    }
  }


  @Override
  protected AbstractScreenDevice getScreenDevice()
  {
    return getEmuSys();
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      if( this.fullScreenMode
	  && (this.windowLocation != null)
	  && (this.windowSize != null) )
      {
	String prefix = getSettingsPrefix();
	props.setProperty(
			prefix + PROP_WINDOW_X,
			String.valueOf( this.windowLocation.x ) );
	props.setProperty(
			prefix + PROP_WINDOW_Y,
			String.valueOf( this.windowLocation.y ) );
	props.setProperty(
			prefix + PROP_WINDOW_WIDTH,
			String.valueOf( this.windowSize.width ) );
	props.setProperty(
			prefix + PROP_WINDOW_HEIGHT,
			String.valueOf( this.windowSize.height ) );
      }
      props.setProperty(
		PROP_FULLSCREEN,
		String.valueOf( this.fullScreenMode ) );
      props.setProperty(
		PROP_SCREEN_SCALE,
		String.valueOf( this.screenScale ) );
      props.setProperty(
		PROP_STATUSBAR_ENABLED,
		String.valueOf( this.mnuStatusBar.isSelected() ) );
      if( this.mnuStatusMsgInSysTray != null ) {
	props.setProperty(
		PROP_STATUSMSG_IN_SYSTRAY,
		String.valueOf( this.mnuStatusMsgInSysTray.isSelected() ) );
      }
      props.setProperty(
		PROP_TOOLBAR_ENABLED,
		String.valueOf( this.mnuToolBar.isSelected() ) );
    }
  }


	/* --- Aktionen im Menu Datei --- */

  private void doFileLoad( boolean startEnabled )
  {
    boolean startSelected   = false;
    boolean loadWithOptions = true;
    File    file            = null;

    File preSelection = Main.getLastDirFile( Main.FILE_GROUP_SOFTWARE );
    if( FileUtil.isJKCEMUFileDialogSelected() ) {
      FileSelectDlg dlg = new FileSelectDlg(
			this,
			FileSelectDlg.Mode.LOAD,
			startEnabled,
			true,		// loadWithOptionsEnabled
			"Datei in Arbeitsspeicher laden",
			preSelection,
			FileUtil.getBinaryFileFilter(),
			FileUtil.getAC1Basic6FileFilter(),
			FileUtil.getBasicOrRBasicFileFilter(),
			FileUtil.getKCSystemFileFilter(),
			FileUtil.getKCBasicFileFilter(),
			FileUtil.getHeadersaveFileFilter(),
			FileUtil.getCommandFileFilter(),
			FileUtil.getTapeFileFilter(),
			FileUtil.getHexFileFilter() );
      dlg.setVisible( true );
      file            = dlg.getSelectedFile();
      loadWithOptions = dlg.isLoadWithOptionsSelected();
      startSelected   = dlg.isStartSelected();
    } else {
      file = FileUtil.showFileOpenDlg(
			this,
			"Datei in Arbeitsspeicher laden",
			preSelection,
			FileUtil.getBinaryFileFilter(),
			FileUtil.getAC1Basic6FileFilter(),
			FileUtil.getBasicOrRBasicFileFilter(),
			FileUtil.getKCSystemFileFilter(),
			FileUtil.getKCBasicFileFilter(),
			FileUtil.getHeadersaveFileFilter(),
			FileUtil.getCommandFileFilter(),
			FileUtil.getTapeFileFilter(),
			FileUtil.getHexFileFilter() );
    }
    if( file != null ) {
      LoadDlg.loadFile(
		this,			// owner
		this,
		file,
		null,			// fileName
		null,			// fileBytes
		loadWithOptions,	// interactive
		startEnabled,
		startSelected );
    }
  }


  private void doFileSave()
  {
    (new SaveDlg(
		this,
		-1,		// Anfangsadresse
		-1,		// Endadresse
		"Programm/Adressbereich speichern",
		SaveDlg.BasicType.NO_BASIC,
		null )).setVisible( true );
  }


  private void doFileScreenImageSave()
  {
    if( doScreenImageSave() ) {
      showStatusText( "Bilddatei gespeichert" );
    }
  }


  private void doFileScreenTextSave()
  {
    if( doScreenTextSave() ) {
      showStatusText( "Textdatei gespeichert" );
    }
  }


  private void doFloppyDisk()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys.getSupportedFloppyDiskDriveCount() > 0 ) {
      FloppyDiskStationFrm.open( this );
    } else {
      BaseDlg.showInfoDlg(
		this,
		"Das gerade emulierte System unterst\u00FCtzt"
			+ " keine Disketten." );
    }
  }


  private void doRAMFloppies()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys.supportsRAMFloppy1() || emuSys.supportsRAMFloppy2() ) {
      RAMFloppyFrm.open( this.emuThread );
    }
  }


	/* --- Aktionen im Menu Ansicht --- */

  private void doViewToolBar()
  {
    Main.setProperty(
		PROP_TOOLBAR_ENABLED,
		String.valueOf( this.mnuToolBar.isSelected() ) );
    updToolBar( true );
  }


  private void doViewStatusBar()
  {
    Main.setProperty(
		PROP_STATUSBAR_ENABLED,
		String.valueOf( this.mnuStatusBar.isSelected() ) );
    updStatusBar( true );
  }


  private void doViewStatusMsgInSysTray()
  {
    if( this.mnuStatusMsgInSysTray != null ) {
      Main.setProperty(
		PROP_STATUSMSG_IN_SYSTRAY,
		String.valueOf( this.mnuStatusMsgInSysTray.isSelected() ) );
    }
    checkUpdTrayIcon();
  }


	/* --- Aktionen im Menu Extra --- */

  private void doExtraChessboard()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys.supportsChessboard() ) {
      ChessboardFrm.open( this.emuThread );
    } else {
      BaseDlg.showInfoDlg(
		this,
		"Das Schachbrett kann nur angezeigt werden,\n"
			+ "wenn ein Schachcomputer oder Lerncomputer\n"
			+ "mit integriertem Schachprogramm emuliert wird.\n"
			+ "Das trifft jedoch f\u00FCr das gerade"
			+ " emulierte System nicht zu." );
    }
  }


  private void doExtraJoystick()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys.getSupportedJoystickCount() > 0 ) {
      JoystickFrm.open( this.emuThread );
    } else {
      BaseDlg.showInfoDlg(
		this,
		"Das emulierte System unterst\u00FCtzt keine Spielhebel." );
    }
  }


  private void doExtraKeyboard()
  {
    if( this.keyboardFrm != null ) {
      EmuUtil.showFrame( this.keyboardFrm );
    } else {
      EmuSys emuSys = getEmuSys();
      try {
	if( emuSys.supportsKeyboardFld() ) {
	  try {
	    AbstractKeyboardFld<? extends EmuSys> kbFld
					= emuSys.createKeyboardFld();
	    if( kbFld != null ) {
	      this.keyboardFrm = new KeyboardFrm( this, emuSys, kbFld );
	    }
	  }
	  catch( UnsupportedOperationException ex ) {}
	}
	if( this.keyboardFrm != null ) {
	  EmuUtil.showFrame( this.keyboardFrm );
	} else {
	  BaseDlg.showErrorDlg(
		this,
		"F\u00FCr das emulierte System steht keine Tastaturansicht"
			+ " zur Verf\u00FCgung." );
	}
      }
      catch( UserCancelException ex ) {}
    }
  }


  private void doExtraPlotter()
  {
    Plotter plotter = getEmuSys().getPlotter();
    if( plotter != null ) {
      PlotterFrm.open( plotter );
    } else {
      BaseDlg.showInfoDlg(
		this,
		"Das Plotter-Fenster kann nur angezeigt werden,\n"
			+ "wenn auch ein Plotter emuliert wird.\n"
			+ "Das ist aber bei der gerade eingestellten"
			+ " Konfiguration nicht der Fall." );
    }
  }


  private void doExtraDebugger()
  {
    EmuSys    emuSys     = getEmuSys();
    int       sysNum     = 0;
    Z80CPU    secondCPU  = emuSys.getSecondZ80CPU();
    Z80Memory secondMem  = emuSys.getSecondZ80Memory();
    String    secondName = emuSys.getSecondSystemName();
    if( (secondCPU != null) && (secondMem != null) ) {
      sysNum = askAccessToSysNum( secondName );
    }
    if( sysNum == 0 ) {
      openPrimaryDebugger();
    }
    else if( sysNum == 1 ) {
      openSecondDebugger();
    }
  }


  private void doExtraDiskImgUnpack()
  {
    File file = FileUtil.showFileOpenDlg(
			this,
			"CP/M-Diskettenabbilddatei entpacken",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			FileUtil.getPlainDiskFileFilter(),
			FileUtil.getAnaDiskFileFilter(),
			FileUtil.getCopyQMFileFilter(),
			FileUtil.getDskFileFilter(),
			FileUtil.getImageDiskFileFilter(),
			FileUtil.getTeleDiskFileFilter() );
    if( file != null ) {
      try {
	boolean done     = false;
	String  fileName = file.getName();
	if( fileName != null ) {
	  fileName = fileName.toLowerCase();
	  if( TextUtil.endsWith( fileName, DiskUtil.plainDiskFileExt )
	      || TextUtil.endsWith(
				fileName,
				DiskUtil.gzPlainDiskFileExt ) )
	  {
	    DiskUtil.unpackPlainDiskFile( this, file );
	    Main.setLastFile( file, Main.FILE_GROUP_DISK );
	    done = true;
	  } else {
	    AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this,
								file,
								true );
	    if( disk != null ) {
	      if( DiskUtil.checkAndConfirmWarning( this, disk ) ) {
		DiskUtil.unpackDisk( this, file, disk, true );
		Main.setLastFile( file, Main.FILE_GROUP_DISK );
	      }
	      done = true;
	    }
	  }
	}
	if( !done ) {
	  if( JOptionPane.showConfirmDialog(
			this,
			"Aus der Dateiendung l\u00E4sst sich der Typ\n"
				+ "der Abbilddatei nicht ermitteln.\n"
				+ "Die Datei wird deshalb als einfach"
				+ " Abbilddatei ge\u00F6ffnet.",
			"Dateityp",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE )
					== JOptionPane.OK_OPTION )
	  {
	    DiskUtil.unpackPlainDiskFile( this, file );
	  }
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doExtraDiskUnpack()
  {
    String driveFileName = DriveSelectDlg.selectDriveFileName(
				this,
				DeviceIO.MediaType.FLOPPYDISK_READ_ONLY );
    if( driveFileName != null ) {
      try {
	DiskUtil.unpackPlainDisk( this, driveFileName );
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doExtraMemEditor()
  {
    EmuSys    emuSys     = getEmuSys();
    int       sysNum     = 0;
    Z80Memory secondMem  = emuSys.getSecondZ80Memory();
    String    secondName = emuSys.getSecondSystemName();
    if( secondMem != null ) {
      sysNum = askAccessToSysNum( secondName );
    }
    if( sysNum == 0 ) {
      if( this.primMemEditFrm != null ) {
	EmuUtil.showFrame( this.primMemEditFrm );
      } else {
	this.primMemEditFrm = new MemEditFrm( this.emuThread );
	EmuUtil.showFrame( this.primMemEditFrm );
      }
    }
    else if( sysNum == 1 ) {
      if( this.secondMemEditFrm != null ) {
	EmuUtil.showFrame( this.secondMemEditFrm );
      } else {
	this.secondMemEditFrm = new MemEditFrm( secondMem );
	if( secondName != null ) {
	  this.secondMemEditFrm.setTitle(
			"JKCEMU Speichereditor: " + secondName );
	} else {
	  this.secondMemEditFrm.setTitle(
			"JKCEMU Speichereditor: Sekund\u00E4rsystem" );
	}
	EmuUtil.showFrame( this.secondMemEditFrm );
      }
    }
  }


  private void doExtraReassembler()
  {
    EmuSys    emuSys     = emuThread.getEmuSys();
    int       sysNum     = 0;
    Z80Memory secondMem  = emuSys.getSecondZ80Memory();
    String    secondName = emuSys.getSecondSystemName();
    if( secondMem != null ) {
      sysNum = askAccessToSysNum( secondName );
    }
    if( sysNum == 0 ) {
      openPrimaryReassembler();
    }
    else if( sysNum == 1 ) {
      openSecondReassembler();
    }
  }


  private void doExtraSecondScreen()
  {
    if( this.secondScreenFrm != null ) {
      EmuUtil.showFrame( this.secondScreenFrm );
    } else {
      AbstractScreenDevice scrDevice = getEmuSys().getSecondScreenDevice();
      if( scrDevice != null ) {
	this.secondScreenFrm = new SecondaryScreenFrm( this, scrDevice );
	EmuUtil.showFrame( this.secondScreenFrm );
      }
    }
  }


  private void doExtraProfile()
  {
    ProfileDlg dlg = new ProfileDlg(
				this,
				"Profil anwenden",
				"Anwenden",
				Main.getProfileFile(),
				false );
    dlg.setVisible( true );
    Properties props = dlg.getSelectedProfileProps();
    if( props != null ) {
      this.emuThread.applySettings( props );  // loest auch RESET aus
      Main.applyProfileToFrames(
			dlg.getSelectedProfileFile(),
			props,
			true,
			null );
      FloppyDiskStationFrm.checkOpenDisks( this, props );
    }
  }


  private void doExtraSpeed()
  {
    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      setMaxSpeed( this, z80cpu.isBrakeEnabled() );
    }
  }


  private void doExtraPause()
  {
    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      if( z80cpu.isActive() ) {
	z80cpu.firePause( !z80cpu.isPause() );
      }
    }
    z80cpu = getEmuSys().getSecondZ80CPU();
    if( z80cpu != null ) {
      if( z80cpu.isActive() ) {
	z80cpu.firePause( !z80cpu.isPause() );
      }
    }
  }


  private void doExtraNMI()
  {
    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      if( Main.getBooleanProperty(
		PROP_PREFIX + PROP_CONFIRM_NMI,
		DEFAULT_CONFIRM_NMI ) )
      {
	if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie einen nicht maskierbaren\n"
			+ "Interrupt (NMI) ausl\u00F6sen?\n\n"
			+ "Achtung! Wenn auf Adresse 0066h keine\n"
			+ "Interrupt-Routine installiert ist,\n"
			+ "kann das Ausl\u00F6sen eines NMI zum Absturz\n"
			+ "des im Emulator laufenden Programms\n"
			+ "und damit zu Datenverlust f\u00FChren.",
		"Best\u00E4tigung" ) )
	{
	  z80cpu.fireNMI();
	}
      } else {
	z80cpu.fireNMI();
      }
    }
  }


  private void doExtraReset()
  {
    if( Main.getBooleanProperty(
		PROP_PREFIX + PROP_CONFIRM_RESET,
		DEFAULT_CONFIRM_RESET ) )
    {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie den Emulator neu starten?",
		"Best\u00E4tigung" ) )
      {
	fireReset( false );
      }
    } else {
      fireReset( false );
    }
  }


  private void doExtraPowerOn()
  {
    if( Main.getBooleanProperty(
		PROP_PREFIX + PROP_CONFIRM_POWER_ON,
		DEFAULT_CONFIRM_POWER_ON ) )
    {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie das Aus- und wieder Einschalten"
			+ " emulieren?\n"
			+ "Dabei gehen alle im Arbeitsspeicher befindlichen\n"
			+ "Programme und Daten verloren.",
		"Best\u00E4tigung" ) )
      {
	fireReset( true );
      }
    } else {
      fireReset( true );
    }
  }


  private void doExtraUSB()
  {
    if( getEmuSys().supportsUSB() ) {
      USBInterfaceFrm.open( this );
    } else {
      BaseDlg.showInfoDlg(
		this,
		"Das Fenster f\u00FCr den USB-Anschluss kann nur"
			+ " angezeigt werden,\n"
			+ "wenn auch ein USB-Anschluss emuliert wird.\n"
			+ "Das ist aber bei der gerade eingestellten"
			+ " Konfiguration nicht der Fall." );
    }
  }


	/* --- Aktionen im Menu Hilfe --- */

  private void doHelpEmuSys()
  {
    String page = getEmuSys().getHelpPage();
    if( page != null ) {
      HelpFrm.openPage( page );
    }
  }


	/* --- private Methoden --- */

  private void appendMsg( String msg )
  {
    if( this.msgFrm == null ) {
      this.msgFrm = new MsgFrm( this );
    }
    EmuUtil.showFrame( this.msgFrm );
    this.msgFrm.appendMsg( msg );
  }


  private int askAccessToSysNum( String sysName )
  {
    int rv = -1;

    String[] options = {
		"Grundger\u00E4t",
		sysName != null ? sysName : "Zweitsystem",
		EmuUtil.TEXT_CANCEL };

    JOptionPane pane = new JOptionPane(
		"Auf welches Prozessorsystem m\u00F6chten Sie zugreifen?",
		JOptionPane.QUESTION_MESSAGE );
    pane.setOptions( options );
    pane.setValue( options[ 0 ] );
    pane.setWantsInput( false );
    pane.createDialog( this, "Auswahl Prozessorsystem" ).setVisible( true );
    Object value = pane.getValue();
    if( value != null ) {
      if( value.equals( options[ 0 ] ) ) {
	rv = 0;
      }
      if( value.equals( options[ 1 ] ) ) {
	rv = 1;
      }
    }
    return rv;
  }


  private void checkUpdTrayIcon()
  {
    boolean state = false;
    if( !this.labelStatus.isVisible() && (mnuStatusMsgInSysTray != null) ) {
      state = this.mnuStatusMsgInSysTray.isSelected();
    }
    if( state ) {
      if( SystemTray.isSupported() ) {
	if( this.trayIcon == null ) {
	  try {
	    java.util.List<Image> iconImages = new ArrayList<>();
	    for( String resource : Main.getIconImageResources() ) {
	      Image image = Main.getLoadedImage( this, resource );
	      if( image != null ) {
		iconImages.add( image );
	      }
	    }
	    if( !iconImages.isEmpty() ) {
	      this.sysTray            = SystemTray.getSystemTray();
	      Image      usedImage    = null;
	      boolean    iconAutoSize = false;
	      Dimension  trayIconSize = this.sysTray.getTrayIconSize();
	      if( trayIconSize != null ) {
		// Icon mit der am besten passenden Groesse heraussuchen
		int lastW = 0;
		int lastH = 0;
		for( Image image : iconImages ) {
		  int w = image.getWidth( this );
		  int h = image.getHeight( this );
		  if( (w > 0) && (h > 0) ) {
		    if( (usedImage == null)
			|| ((w > lastW) && (w <= trayIconSize.width)
			    && (h > lastH) && (h < trayIconSize.height)) )
		    {
		      usedImage = image;
		      lastW     = w;
		      lastH     = h;
		    }
		  }
		}
	      } else {
		usedImage = iconImages.get( 0 );
		iconAutoSize = true;
	      }
	      if( usedImage != null ) {
		this.trayIcon = new TrayIcon( usedImage, Main.APPNAME );
		this.trayIcon.addMouseListener( this );
		this.sysTray.add( this.trayIcon );
	      }
	    }
	  }
	  catch( Exception ex ) {}

	  /*
	   * Wenn das TrayIcon nicht angelegt werden konnte,
	   * soll es auch den zugehoerigen Menupunkt nicht mehr geben
	   */
	  if( (this.trayIcon == null)
	      && (this.mnuStatusMsgInSysTray != null) )
	  {
	    this.mnuStatusMsgInSysTray.removeActionListener( this );
	    this.mnuView.remove( this.mnuStatusMsgInSysTray );
	    this.mnuStatusMsgInSysTray = null;
	  }
	}
      }
    } else {
      if( this.trayIcon != null ) {
	this.trayIcon.removeMouseListener( this );
	if( this.sysTray != null ) {
	  this.sysTray.remove( this.trayIcon );
	  this.sysTray = null;
	}
	this.trayIcon = null;
      }
    }
  }


  private String createMHzText( Z80CPU cpu )
  {
    String rv = null;
    if( cpu != null ) {
      double curSpeedMHz = cpu.getCurrentSpeedKHz() / 1000.0;
      if( curSpeedMHz > 0.09 ) {
	rv = this.speedFmt.format( curSpeedMHz );
      }
    }
    return rv;
  }


  private void emuSysChangedInternal( EmuSys emuSys, Properties props )
  {
    Z80CPU    secondCPU = emuSys.getSecondZ80CPU();
    Z80Memory secondMem = emuSys.getSecondZ80Memory();

    setTitle( "JKCEMU: " + emuSys.getTitle() );
    this.mnuHelpEmuSys.setEnabled( emuSys.getHelpPage() != null );
    this.copyEnabled  = emuSys.supportsCopyToClipboard();
    this.pasteEnabled = emuSys.supportsPasteFromClipboard();
    updActionBtns( emuSys );
    if( !this.copyEnabled ) {
      this.mnuCopy.setEnabled( false );
      this.popupCopy.setEnabled( false );
    }
    updPasteBtns();

    // Bildschirmaktualisierung
    updScreenRefreshSettings( props );

    // Fenstergroesse
    this.screenFld.setScreenDevice( emuSys );
    updScreenSize( props );

    // Bildschirmausgabe
    setScreenTextActionsEnabled( emuSys.canExtractScreenText() );
    setScreenDirty( true );

    // ggf. nicht relevante Fenster schliessen
    if( this.keyboardFrm != null ) {
      if( emuSys != null ) {
	if( !this.keyboardFrm.accepts( emuSys ) ) {
	  this.keyboardFrm.doClose();
	}
      } else {
	this.keyboardFrm.doClose();
      }
    }
    if( this.secondDebugFrm != null ) {
      if( (secondCPU == null) || (secondMem == null)
	  || (this.secondDebugFrm.getZ80CPU() != secondCPU)
	  || (this.secondDebugFrm.getZ80Memory() != secondMem) )
      {
	this.secondDebugFrm.doClose();
	this.secondDebugFrm = null;
      }
    }
    if( this.secondMemEditFrm != null ) {
      if( (secondMem == null)
	  || (this.secondMemEditFrm.getZ80Memory() != secondMem) )
      {
	this.secondMemEditFrm.doClose();
	this.secondMemEditFrm = null;
      }
    }
    if( this.secondReassFrm != null ) {
      if( (secondMem == null)
	  || (this.secondReassFrm.getZ80Memory() != secondMem) )
      {
	this.secondReassFrm.doClose();
	this.secondReassFrm = null;
      }
    }
    if( this.secondScreenFrm != null ) {
      if( emuSys != null ) {
	if( !this.secondScreenFrm.accepts(
				emuSys.getSecondScreenDevice() ) )
	{
	  this.secondScreenFrm.doClose();
	}
      } else {
	this.secondScreenFrm.doClose();
      }
    }
    if( !emuSys.supportsRAMFloppies() ) {
      RAMFloppyFrm.close();
    }
    if( !emuSys.supportsUSB() ) {
      USBInterfaceFrm.close();
    }
    this.oldEmuSys = emuSys;
  }


  private void execReset( boolean powerOn )
  {
    this.emuThread.fireReset( powerOn );
    /*
     * Die Timer werden neu gestartet fuer den Fall,
     * dass sich ein Timer aufgehaengt haben sollte,
     * und man moechte ihn mit RESET reaktivieren.
     */
    this.screenRefreshTimer.restart();
    this.statusRefreshTimer.restart();
  }


  private ReassFrm openSecondReassembler(
				Z80Memory secondMem,
				String    secondName )
  {
    ReassFrm reassFrm = null;
    if( (secondMem != null) && (secondName != null) ) {
      if( this.secondReassFrm != null ) {
	EmuUtil.showFrame( this.secondReassFrm );
      } else {
	this.secondReassFrm = new ReassFrm( this, secondMem );
	if( secondName != null ) {
	  this.secondReassFrm.setTitle(
			"JKCEMU Reassembler: " + secondName );
	} else {
	  this.secondReassFrm.setTitle(
			"JKCEMU Reassembler: Sekund\u00E4rsystem" );
	}
	EmuUtil.showFrame( this.secondReassFrm );
      }
      reassFrm = this.secondReassFrm;
    }
    return reassFrm;
  }


  private void refreshStatus()
  {
    String msg = "Bereit";
    if( this.emuThread != null ) {
      Z80CPU z80cpu = this.emuThread.getZ80CPU();
      if( z80cpu != null ) {
	if( z80cpu.isActive() ) {
	  if( z80cpu.isPause() ) {
	    getEmuSys().updDebugScreen();
	    this.screenDirty = true;
	    this.screenFld.repaint();
	    msg = "Pause";
	  } else {
	    String mhzText = createMHzText( z80cpu );
	    if( mhzText != null ) {
	      msg = String.format(
			"Emulierte Taktfrequenz: %s MHz",
			mhzText );

	      EmuSys emuSys = this.emuThread.getEmuSys();
	      String secondName = emuSys.getSecondSystemName();
	      Z80CPU secondCPU  = emuSys.getSecondZ80CPU();
	      if( (secondName != null) && (secondCPU != null) ) {
		if( emuSys.isSecondSystemRunning()
		    && !secondCPU.isPause() )
		{
		  mhzText = createMHzText( secondCPU );
		  if( mhzText != null ) {
		    msg = String.format(
					"%s, %s: %s MHz",
					msg,
					secondName,
					mhzText );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    this.labelStatus.setText( msg );
  }


  private void setFullScreenMode( boolean state )
  {
    if( state != this.fullScreenMode ) {
      clearScreenSelection();
      setVisible( false );
      dispose();
      if( state ) {
	this.windowLocation   = getLocation();
	this.windowSize       = getSize();
	this.emuSysScreenSize = getEmuSys().getScreenSize();
	this.fullScreenMode   = state;
	this.mnuBar.setVisible( false );
	this.panelToolBar.setVisible( false );
	this.labelStatus.setVisible( false );
	if( !updFullScreenScale() ) {
	  setExtendedState( Frame.MAXIMIZED_BOTH );
	}
	try {
	  setUndecorated( true );
	}
	catch( IllegalComponentStateException ex ) {}
	this.mnuFullScreen.setText( TEXT_FULLSCREEN_OFF );
	this.popupFullScreen.setText( TEXT_FULLSCREEN_OFF );
	if( !this.fullScreenInfoDone ) {
	  this.fullScreenInfoDone = true;
	  final Component owner = this;
	  EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    BaseDlg.showInfoDlg(
			owner,
			"Den Vollbildmodus k\u00F6nnen Sie"
				+ " im Kontextmen\u00FC"
				+ " wieder ausschalten." );
		  }
		} );
	}
	this.screenFld.requestFocus();
      } else {
	this.fullScreenMode = state;
	this.mnuBar.setVisible( true );
	try {
	  setUndecorated( false );
	}
	catch( IllegalComponentStateException ex ) {}
	setExtendedState( Frame.NORMAL );
	setScreenScale( this.screenScale );
	updToolBar( false );
	updStatusBar( false );
	if( this.windowLocation != null ) {
	  setLocation( this.windowLocation );
	}
	/*
	 * Wenn waehrend des Vollbildzeit sich die Graikaufloesung
	 * des emulierten Systems geaendert hat,
	 * soll nicht die alte Fenstergroesse wieder hergestellt werden,
	 * sondern neu bestimmt werden.
	 */
	boolean done = false;
	if( this.windowSize != null ) {
	  Dimension oldSize = this.emuSysScreenSize;
	  Dimension curSize = getEmuSys().getScreenSize();
	  if( (oldSize != null) && (curSize != null) ) {
	    if( oldSize.equals( curSize ) ) {
	      setSize( this.windowSize );
	      done = true;
	    }
	  }
	}
	if( !done ) {
	  screenFld.updPreferredSize();
	  pack();
	}
	this.mnuFullScreen.setText( TEXT_FULLSCREEN_ON );
	this.popupFullScreen.setText( TEXT_FULLSCREEN_ON );
      }
      setVisible( true );
    }
  }


  private void updActionBtns( EmuSys emuSys )
  {
    boolean supportsOpenBasic    = false;
    boolean supportsSaveBasic    = false;
    boolean supportsAudio        = false;
    boolean supportsChessboard   = false;
    boolean supportsFloppyDisks  = false;
    boolean supportsJoystick     = false;
    boolean supportsKeyboardFld  = false;
    boolean supportsPlotter      = false;
    boolean supportsPrinter      = false;
    boolean supportsRAMFloppies  = false;
    boolean supportsSecondScreen = false;
    boolean supportsUSB          = false;
    if( emuSys != null ) {
      supportsOpenBasic    = emuSys.supportsOpenBasic();
      supportsSaveBasic    = emuSys.supportsSaveBasic();
      supportsAudio        = emuSys.supportsAudio();
      supportsChessboard   = emuSys.supportsChessboard();
      supportsFloppyDisks  = (emuSys.getSupportedFloppyDiskDriveCount() > 0);
      supportsJoystick     = (emuSys.getSupportedJoystickCount() > 0);
      supportsKeyboardFld  = emuSys.supportsKeyboardFld();
      supportsPlotter      = (emuSys.getPlotter() != null);
      supportsPrinter      = emuSys.supportsPrinter();
      supportsUSB          = emuSys.supportsUSB();
      supportsSecondScreen = (emuSys.getSecondScreenDevice() != null);
      supportsRAMFloppies  = emuSys.supportsRAMFloppies();
    }

    // Menueeintrage
    this.mnuBasicOpen.setEnabled( supportsOpenBasic );
    this.mnuBasicSave.setEnabled( supportsSaveBasic );
    this.mnuFloppyDisks.setEnabled( supportsFloppyDisks );
    this.popupFloppyDisk.setEnabled( supportsFloppyDisks );
    this.mnuAudio.setEnabled( supportsAudio );
    this.popupAudio.setEnabled( supportsAudio );
    this.mnuChessboard.setEnabled( supportsChessboard );
    this.mnuJoystick.setEnabled( supportsJoystick );
    this.popupJoystick.setEnabled( supportsJoystick );
    this.mnuKeyboard.setEnabled( supportsKeyboardFld );
    this.popupKeyboard.setEnabled( supportsKeyboardFld );
    this.mnuPlotter.setEnabled( supportsPlotter );
    this.mnuPrintJobs.setEnabled( supportsPrinter );
    this.mnuSecondScreen.setEnabled( supportsSecondScreen );
    this.mnuRAMFloppies.setEnabled( supportsRAMFloppies );
    this.mnuUSB.setEnabled( supportsUSB );
    this.popupUSB.setEnabled( supportsUSB );

    // Werkzeugleiste anpassen
    this.toolBar.removeAll();
    this.toolBar.add( this.btnLoad );
    this.toolBar.add( this.btnSave );
    if( this.copyEnabled || this.pasteEnabled ) {
      this.toolBar.addSeparator();
      if( this.copyEnabled ) {
	this.toolBar.add( this.btnCopy );
      }
      if( this.pasteEnabled ) {
	this.toolBar.add( this.btnPaste );
      }
    }
    this.toolBar.addSeparator();
    if( supportsChessboard ) {
      this.toolBar.add( this.btnChessboard );
    }
    if( supportsFloppyDisks ) {
      this.toolBar.add( this.btnFloppyDisks );
    }
    if( supportsKeyboardFld ) {
      this.toolBar.add( this.btnKeyboard );
    }
    if( supportsAudio ) {
      this.toolBar.add( this.btnAudio );
    }
    this.toolBar.add( this.btnSettings );
    this.toolBar.addSeparator();
    this.toolBar.add( this.btnReset );
    SwingUtilities.updateComponentTreeUI( this.toolBar );
  }


  private boolean updFullScreenScale()
  {
    boolean               rv = false;
    GraphicsConfiguration gc = getGraphicsConfiguration();
    if( gc != null ) {
      GraphicsDevice gd = gc.getDevice();
      if( gd != null ) {
	DisplayMode dm = gd.getDisplayMode();
	if( dm != null ) {
	  int w = dm.getWidth();
	  int h = dm.getHeight();
	  if( (w > 0) && (h > 0) ) {
	    setBounds( 0, 0, w, h );
	    EmuSys emuSys = getEmuSys();
	    int    wSys   = emuSys.getScreenWidth();
	    int    hSys   = emuSys.getScreenHeight();
	    if( (wSys > 0) && (hSys > 0) ) {
	      int scale = Math.min( (w - 50) / wSys, (h - 50) / hSys );
	      this.screenFld.setScreenScale( scale > 1 ? scale : 1 );
	      rv = true;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void updPauseBtn()
  {
    String pauseText = "Pause";
    if( this.emuThread != null ) {
      Z80CPU z80cpu = this.emuThread.getZ80CPU();
      if( z80cpu != null ) {
	if( z80cpu.isActive() ) {
	  if( z80cpu.isPause() ) {
	    pauseText = "Fortsetzen";
	  }
	  this.mnuPause.setEnabled( true );
	  this.popupPause.setEnabled( true );
	} else {
	  this.mnuPause.setEnabled( false );
	  this.popupPause.setEnabled( false );
	}
      }
    }
    this.mnuPause.setText( pauseText );
    this.popupPause.setText( pauseText );
  }


  private void updScreenRefreshSettings( Properties props )
  {
    if( this.screenRefreshTimer.isRunning() ) {
      this.screenRefreshTimer.stop();
    }
    if( !getEmuSys().isAutoScreenRefresh() ) {
      this.screenRefreshMillis = EmuUtil.getIntProperty(
				props,
				PROP_PREFIX + PROP_SCREEN_REFRESH_MS,
				getDefaultScreenRefreshMillis() );
      if( this.screenRefreshMillis < 10 ) {
	this.screenRefreshMillis = getDefaultScreenRefreshMillis();
      }
      this.screenRefreshTimer.setDelay( this.screenRefreshMillis );
      this.screenRefreshTimer.start();
    }
  }


  private boolean updScreenSize( Properties props )
  {
    boolean rv      = false;
    boolean done    = false;
    boolean visible = isVisible();
    int     margin  = EmuUtil.getIntProperty(
				props,
				PROP_PREFIX + PROP_SCREEN_MARGIN,
				ScreenFld.DEFAULT_MARGIN );
    if( margin < 0 ) {
      margin = 0;
    }

    EmuSys oldEmuSys = this.oldEmuSys;
    EmuSys newEmuSys = getEmuSys();
    if( visible && (oldEmuSys != null) ) {
      if( this.screenFld.isPreferredSizeSet()
	  && (this.screenScale == this.screenFld.getScreenScale())
	  && (newEmuSys.getScreenWidth() == oldEmuSys.getScreenWidth())
	  && (newEmuSys.getScreenHeight() == oldEmuSys.getScreenHeight())
	  && (margin == this.screenFld.getMargin()) )
      {
	done = true;
      }
    }
    if( visible && this.fullScreenMode ) {
      updFullScreenScale();
    }
    if( !done ) {
      this.screenFld.setMargin( margin );
      if( this.fullScreenMode ) {
	this.windowSize = null;
      } else {
	pack();
      }
      if( !visible ) {
	setScreenCentered();
      }
      rv = true;
    }
    return rv;
  }


  private void updStatusBar( boolean updWindowSize )
  {
    boolean state = (this.mnuStatusBar.isSelected() && !this.fullScreenMode);
    if( this.mnuStatusMsgInSysTray != null ) {
      this.mnuStatusMsgInSysTray.setEnabled( !state );
    }
    if( state != this.labelStatus.isVisible() ) {
      this.labelStatus.setVisible( state );
      if( state ) {
	this.statusRefreshTimer.start();
      } else {
	this.statusRefreshTimer.stop();
      }
      if( updWindowSize ) {
	fireScreenSizeChanged();
      }
      checkUpdTrayIcon();
    }
  }


  private void updToolBar( boolean updWindowSize )
  {
    boolean state = (this.mnuToolBar.isSelected() && !this.fullScreenMode);
    if( state != this.panelToolBar.isVisible() ) {
      this.panelToolBar.setVisible( state );
      if( updWindowSize ) {
	fireScreenSizeChanged();
      }
    }
  }
}
