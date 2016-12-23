/*
 * (c) 2008-2016 Jens Mueller
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
import java.awt.Window;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import jkcemu.Main;
import jkcemu.audio.AudioFrm;
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
import jkcemu.etc.USBInterfaceFrm;
import jkcemu.joystick.JoystickFrm;
import jkcemu.filebrowser.FileBrowserFrm;
import jkcemu.filebrowser.FindFilesFrm;
import jkcemu.image.ImageCaptureFrm;
import jkcemu.image.ImageFrm;
import jkcemu.image.ImgSaver;
import jkcemu.image.ImgSelection;
import jkcemu.image.VideoCaptureFrm;
import jkcemu.print.PrintListFrm;
import jkcemu.text.TextEditFrm;
import jkcemu.text.TextUtil;
import jkcemu.tools.ReassFrm;
import jkcemu.tools.calculator.CalculatorFrm;
import jkcemu.tools.debugger.DebugFrm;
import jkcemu.tools.fileconverter.FileConvertFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexedit.HexEditFrm;
import jkcemu.tools.hexedit.MemEditFrm;
import z80emu.Z80Breakpoint;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80Memory;
import z80emu.Z80StatusListener;


public class ScreenFrm extends BaseFrm implements
						DropTargetListener,
						FlavorListener,
						Z80StatusListener
{
  public static final String PROP_PREFIX            = "jkcemu.";
  public static final String PROP_CONFIRM_NMI       = "confirm.nmi";
  public static final String PROP_CONFIRM_RESET     = "confirm.reset";
  public static final String PROP_CONFIRM_QUIT      = "confirm.quit";
  public static final String PROP_CONFIRM_POWER_ON  = "confirm.power_on";
  public static final String PROP_SCREEN_MARGIN     = "screen.margin";
  public static final String PROP_SCREEN_REFRESH_MS = "screen.refresh.ms";

  public static final String PROP_COPY_AND_PASTE_DIRECT
					= "copy_and_paste.direct";

  public static final boolean DEFAULT_CONFIRM_NMI           = true;
  public static final boolean DEFAULT_CONFIRM_RESET         = false;
  public static final boolean DEFAULT_CONFIRM_POWER_ON      = false;
  public static final boolean DEFAULT_CONFIRM_QUIT          = false;
  public static final boolean DEFAULT_COPY_AND_PASTE_DIRECT = true;

  private static final String HELP_PAGE_HOME    = "/help/home.htm";
  private static final String HELP_PAGE_LICENSE = "/help/license.htm";

  private static final String TEXT_FULLSCREEN_ON
					= "Vollbildmodus einschalten";
  private static final String TEXT_FULLSCREEN_OFF
					= "Vollbildmodus ausschalten";

  private static final String TEXT_MAX_SPEED = "Maximale Geschwindigkeit";
  private static final String TEXT_STD_SPEED = "Standard-Geschwindigkeit";

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
  private static final String ACTION_HELP_CONTENT      = "help.content";
  private static final String ACTION_HELP_LICENSE      = "help.license";
  private static final String ACTION_HELP_SYSTEM       = "help.system";
  private static final String ACTION_HEXDIFF           = "hexdiff";
  private static final String ACTION_HEXEDITOR         = "hexeditor";
  private static final String ACTION_IMAGEVIEWER       = "imageviewer";
  private static final String ACTION_JOYSTICK          = "joystick";
  private static final String ACTION_KEYBOARD          = "keyboard";
  private static final String ACTION_COPY              = "edit.copy";
  private static final String ACTION_PASTE             = "edit.paste";
  private static final String ACTION_PASTE_CANCEL      = "edit.paste.cancel";
  private static final String ACTION_PASTE_WITH        = "edit.paste.with";
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
  private static final String ACTION_SCALE_1           = "scale.1";
  private static final String ACTION_SCALE_2           = "scale.2";
  private static final String ACTION_SCALE_3           = "scale.3";
  private static final String ACTION_SCALE_4           = "scale.4";
  private static final String ACTION_SCREENIMAGE_SHOW  = "screen.image.show";
  private static final String ACTION_SCREENIMAGE_COPY  = "screen.image.copy";
  private static final String ACTION_SCREENIMAGE_SAVE  = "screen.image.save";
  private static final String ACTION_SCREENTEXT_SHOW   = "screen.text.show";
  private static final String ACTION_SCREENTEXT_COPY   = "screen.text.copy";
  private static final String ACTION_SCREENTEXT_SAVE   = "screen.text.save";
  private static final String ACTION_SCREENSHOT        = "screenshot";
  private static final String ACTION_SCREENVIDEO       = "screen.video";
  private static final String ACTION_SETTINGS          = "extra.settings";
  private static final String ACTION_SPEED             = "speed";
  private static final String ACTION_TEXTEDITOR        = "texteditor";
  private static final String ACTION_USB               = "usb";


  private JMenuBar             mnuBar;
  private JMenuItem            mnuScreenTextShow;
  private JMenuItem            mnuScreenTextCopy;
  private JMenuItem            mnuScreenTextSave;
  private JMenuItem            mnuEditCopy;
  private JMenuItem            mnuEditPaste;
  private JMenuItem            mnuEditPasteWith;
  private JMenuItem            mnuEditPasteCancel;
  private JMenuItem            mnuBasicOpen;
  private JMenuItem            mnuBasicSave;
  private JMenuItem            mnuAudio;
  private JMenuItem            mnuChessboard;
  private JMenuItem            mnuFloppyDisks;
  private JMenuItem            mnuFullScreen;
  private JMenuItem            mnuJoystick;
  private JMenuItem            mnuKeyboard;
  private JMenuItem            mnuPause;
  private JMenuItem            mnuPlotter;
  private JMenuItem            mnuPrintJobs;
  private JMenuItem            mnuRAMFloppies;
  private JMenuItem            mnuSpeed;
  private JMenuItem            mnuUSB;
  private JMenuItem            mnuHelpSys;
  private JRadioButtonMenuItem btnScreenScale1;
  private JRadioButtonMenuItem btnScreenScale2;
  private JRadioButtonMenuItem btnScreenScale3;
  private JRadioButtonMenuItem btnScreenScale4;
  private JPopupMenu           mnuPopup;
  private JMenuItem            mnuPopupCopy;
  private JMenuItem            mnuPopupPaste;
  private JMenuItem            mnuPopupAudio;
  private JMenuItem            mnuPopupFloppyDisk;
  private JMenuItem            mnuPopupUSB;
  private JMenuItem            mnuPopupKeyboard;
  private JMenuItem            mnuPopupJoystick;
  private JMenuItem            mnuPopupSpeed;
  private JMenuItem            mnuPopupPause;
  private JMenuItem            mnuPopupFullScreen;
  private JToolBar             toolBar;
  private JButton              btnLoad;
  private JButton              btnSave;
  private JButton              btnCopy;
  private JButton              btnPaste;
  private JButton              btnAudio;
  private JButton              btnChessboard;
  private JButton              btnFloppyDisks;
  private JButton              btnKeyboard;
  private JButton              btnSettings;
  private JButton              btnReset;
  private JLabel               labelStatus;
  private Point                windowLocation;
  private Dimension            windowSize;
  private boolean              fullScreenMode;
  private boolean              fullScreenInfoDone;
  private boolean              ignoreKeyChar;
  private boolean              copyEnabled;
  private boolean              pasteEnabled;
  private volatile boolean     chessboardDirty;
  private volatile boolean     screenDirty;
  private ScreenFld            screenFld;
  private int                  screenScale;
  private int                  screenRefreshMillis;
  private javax.swing.Timer    screenRefreshTimer;
  private javax.swing.Timer    statusRefreshTimer;
  private NumberFormat         speedFmt;
  private EmuThread            emuThread;
  private Clipboard            clipboard;
  private KeyboardFrm          keyboardFrm;
  private MsgFrm               msgFrm;
  private DebugFrm             primDebugFrm;
  private MemEditFrm           primMemEditFrm;
  private ReassFrm             primReassFrm;
  private DebugFrm             secondDebugFrm;
  private MemEditFrm           secondMemEditFrm;
  private ReassFrm             secondReassFrm;


  public ScreenFrm()
  {
    setTitle( "JKCEMU" );


    // Initialisierungen
    this.emuThread           = null;
    this.keyboardFrm         = null;
    this.msgFrm              = null;
    this.primDebugFrm        = null;
    this.primMemEditFrm      = null;
    this.primReassFrm        = null;
    this.secondDebugFrm      = null;
    this.secondMemEditFrm    = null;
    this.secondReassFrm      = null;
    this.windowLocation      = null;
    this.windowSize          = null;
    this.fullScreenMode      = false;
    this.fullScreenInfoDone  = false;
    this.ignoreKeyChar       = false;
    this.copyEnabled         = false;
    this.pasteEnabled        = false;
    this.chessboardDirty     = false;
    this.screenDirty         = false;
    this.screenScale         = 1;
    this.screenRefreshMillis = getDefaultScreenRefreshMillis();
    this.screenRefreshTimer  = new javax.swing.Timer(
					this.screenRefreshMillis,
					this );

    this.clipboard = null;
    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
    }

    this.speedFmt = NumberFormat.getNumberInstance();
    if( this.speedFmt instanceof DecimalFormat ) {
      ((DecimalFormat) this.speedFmt).applyPattern( "###,###,##0.0#" );
    }


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    mnuFile.add( createJMenuItem(
			"Laden...",
			ACTION_FILE_LOAD,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_L,
					InputEvent.ALT_MASK ) ) );
    mnuFile.add( createJMenuItem(
			"Speichern...",
			ACTION_FILE_SAVE,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_S,
					InputEvent.ALT_MASK ) ) );
    mnuFile.addSeparator();
    this.mnuBasicOpen = createJMenuItem(
	"BASIC-Programm im Texteditor \u00F6ffnen...",
	ACTION_BASIC_OPEN,
	KeyStroke.getKeyStroke(
			KeyEvent.VK_T,
			InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ) );
    mnuFile.add( this.mnuBasicOpen );

    this.mnuBasicSave = createJMenuItem(
	"BASIC-Programm speichern...",
	ACTION_BASIC_SAVE,
	KeyStroke.getKeyStroke(
			KeyEvent.VK_S,
			InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ) );
    mnuFile.add( this.mnuBasicSave );
    mnuFile.addSeparator();

    this.mnuRAMFloppies = createJMenuItem(
				"RAM-Floppies...",
				ACTION_RAMFLOPPIES );
    mnuFile.add( this.mnuRAMFloppies );

    this.mnuFloppyDisks = createJMenuItem(
				"Diskettenstation...",
				ACTION_FLOPPYDISKS );
    mnuFile.add( this.mnuFloppyDisks );
    mnuFile.addSeparator();

    // Untermenu Bildschirmausgabe
    JMenu mnuScreen = new JMenu( "Bildschirmausgabe" );
    mnuFile.add( mnuScreen );
    mnuScreen.add( createJMenuItem(
				"als Bildschirmfoto anzeigen...",
				ACTION_SCREENIMAGE_SHOW ) );
    mnuScreen.add( createJMenuItem(
				"als Bild kopieren",
				ACTION_SCREENIMAGE_COPY ) );
    mnuScreen.add( createJMenuItem(
				"als Bilddatei speichern...",
				ACTION_SCREENIMAGE_SAVE ) );
    mnuScreen.addSeparator();

    this.mnuScreenTextShow = createJMenuItem(
				"im Texteditor anzeigen",
				ACTION_SCREENTEXT_SHOW );
    mnuScreen.add( this.mnuScreenTextShow );

    this.mnuScreenTextCopy = createJMenuItem(
				"als Text kopieren",
				ACTION_SCREENTEXT_COPY );
    mnuScreen.add( this.mnuScreenTextCopy );

    this.mnuScreenTextSave = createJMenuItem(
				"als Textdatei speichern...",
				ACTION_SCREENTEXT_SAVE );
    mnuScreen.add( this.mnuScreenTextSave );
    mnuFile.addSeparator();

    mnuFile.add( createJMenuItem(
			"Texteditor/Programmierung...",
			ACTION_TEXTEDITOR,
			KeyStroke.getKeyStroke(
				KeyEvent.VK_T,
				InputEvent.ALT_MASK ) ) );
    mnuFile.add( createJMenuItem( "Datei-Browser...", ACTION_FILEBROWSER ) );
    mnuFile.add( createJMenuItem(
			"Dateien suchen...",
			ACTION_FIND_FILES,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_F,
					InputEvent.ALT_MASK ) ) );
    mnuFile.addSeparator();
    mnuFile.add( createJMenuItem( "Beenden", ACTION_QUIT ) );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuEditCopy = createJMenuItem(
			"Kopieren",
			ACTION_COPY,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_C,
					InputEvent.ALT_MASK ) );
    this.mnuEditCopy.setEnabled( false );
    mnuEdit.add( this.mnuEditCopy );

    this.mnuEditPaste = createJMenuItem(
			"Einf\u00FCgen",
			ACTION_PASTE,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_V,
					InputEvent.ALT_MASK ) );
    this.mnuEditPaste.setEnabled( false );
    mnuEdit.add( this.mnuEditPaste );

    this.mnuEditPasteWith = createJMenuItem(
				"Einf\u00FCgen mit...",
				ACTION_PASTE_WITH );
    this.mnuEditPasteWith.setEnabled( false );
    mnuEdit.add( this.mnuEditPasteWith );
    mnuEdit.addSeparator();

    this.mnuEditPasteCancel = createJMenuItem(
				"Einf\u00FCgen abbrechen",
				ACTION_PASTE_CANCEL );
    this.mnuEditPasteCancel.setEnabled( false );
    mnuEdit.add( this.mnuEditPasteCancel );


    // Menu Extra
    JMenu mnuExtra = new JMenu( "Extra" );
    mnuExtra.setMnemonic( KeyEvent.VK_E );

    JMenu mnuExtraScale = new JMenu( "Ansicht" );

    ButtonGroup grpScale = new ButtonGroup();

    this.btnScreenScale1 = createJRadioButtonMenuItem(
				grpScale,
				"100 %",
				ACTION_SCALE_1,
				true,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_1,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale1 );

    this.btnScreenScale2 = createJRadioButtonMenuItem(
				grpScale,
				"200 %",
				ACTION_SCALE_2,
				false,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_2,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale2 );

    this.btnScreenScale3 = createJRadioButtonMenuItem(
				grpScale,
				"300 %",
				ACTION_SCALE_3,
				false,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_3,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale3 );

    this.btnScreenScale4 = createJRadioButtonMenuItem(
				grpScale,
				"400 %",
				ACTION_SCALE_4,
				false,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_4,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale4 );

    mnuExtra.add( mnuExtraScale );
    mnuExtra.addSeparator();

    this.mnuAudio = createJMenuItem(
				"Audio/Kassette...",
				ACTION_AUDIO,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_A,
					InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuAudio );

    this.mnuKeyboard = createJMenuItem(
				"Tastatur...",
				ACTION_KEYBOARD,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_K,
					InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuKeyboard );

    this.mnuJoystick = createJMenuItem(
				"Joysticks...",
				ACTION_JOYSTICK,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_J,
					InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuJoystick );

    this.mnuPrintJobs = createJMenuItem(
				"Druckauftr\u00E4ge...",
				ACTION_PRINTER );
    mnuExtra.add( this.mnuPrintJobs );

    this.mnuPlotter = createJMenuItem(
				"Plotter...",
				ACTION_PLOTTER );
    mnuExtra.add( this.mnuPlotter );

    this.mnuUSB = createJMenuItem(
				"USB-Anschluss...",
				ACTION_USB,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_U,
					InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuUSB );

    this.mnuChessboard = createJMenuItem(
				"Schachbrett...",
				ACTION_CHESSBOARD );
    mnuExtra.add( this.mnuChessboard );
    mnuExtra.addSeparator();

    mnuExtra.add( createJMenuItem(
				"Bildschirmfoto...",
				ACTION_SCREENSHOT ) );
    mnuExtra.add( createJMenuItem(
				"Bildschirmvideo...",
				ACTION_SCREENVIDEO ) );

    JMenu mnuExtraTools = new JMenu( "Werkzeuge" );
    mnuExtraTools.add( createJMenuItem(
		"Debugger...",
		ACTION_DEBUGGER,
		KeyStroke.getKeyStroke(
			KeyEvent.VK_D,
			InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ) ) );
    mnuExtraTools.add( createJMenuItem(
		"Reassembler...",
		ACTION_REASSEMBLER,
		KeyStroke.getKeyStroke(
			KeyEvent.VK_R,
			InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ) ) );
    mnuExtraTools.add( createJMenuItem(
		"Speichereditor...",
		ACTION_MEMEDITOR,
		KeyStroke.getKeyStroke(
			KeyEvent.VK_M,
			InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ) ) );
    mnuExtraTools.add( createJMenuItem(
		"Hex-Editor...",
		ACTION_HEXEDITOR,
		KeyStroke.getKeyStroke(
			KeyEvent.VK_H,
			InputEvent.ALT_MASK | InputEvent.SHIFT_MASK ) ) );
    mnuExtraTools.add( createJMenuItem(
				"Hex-Dateivergleicher...",
				ACTION_HEXDIFF ) );
    mnuExtraTools.add( createJMenuItem(
				"Dateikonverter...",
				ACTION_FILECONVERTER ) );
    mnuExtraTools.add( createJMenuItem(
				"Bildbetrachter/-bearbeitung...",
				ACTION_IMAGEVIEWER ) );
    mnuExtraTools.add( createJMenuItem(
				"Audiorecorder...",
				ACTION_AUDIORECORDER ) );
    mnuExtraTools.add( createJMenuItem( "Rechner...", ACTION_CALCULATOR ) );
    mnuExtraTools.add( createJMenuItem(
			"Diskettenabbilddatei-Inspektor...",
			ACTION_DISKVIEWER ) );
    mnuExtraTools.addSeparator();
    mnuExtraTools.add( createJMenuItem(
			"CP/M-Diskettenabbilddatei manuell erstellen...",
			ACTION_DISKIMAGE_BUILD ) );
    mnuExtraTools.add( createJMenuItem(
			"CP/M-Diskettenabbilddatei entpacken...",
			ACTION_DISKIMAGE_UNPACK ) );
    mnuExtraTools.add( createJMenuItem(
			"CP/M-Diskette entpacken...",
			ACTION_DISK_UNPACK ) );
    mnuExtraTools.addSeparator();
    mnuExtraTools.add( createJMenuItem(
			"Abbilddatei von Datentr\u00E4ger erstellen...",
			ACTION_DISKIMAGE_CAPTURE ) );
    mnuExtraTools.add( createJMenuItem(
			"Abbilddatei auf Datentr\u00E4ger schreiben...",
			ACTION_DISKIMAGE_WRITE ) );
    mnuExtra.add( mnuExtraTools );
    mnuExtra.addSeparator();

    mnuExtra.add( createJMenuItem( "Einstellungen...", ACTION_SETTINGS ) );
    mnuExtra.add( createJMenuItem( "Profil anwenden...", ACTION_PROFILE ) );
    mnuExtra.addSeparator();

    this.mnuFullScreen = createJMenuItem(
				TEXT_FULLSCREEN_ON,
				ACTION_FULLSCREEN );
    mnuExtra.add( this.mnuFullScreen );

    this.mnuSpeed = createJMenuItem(
				TEXT_MAX_SPEED,
				ACTION_SPEED,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_G,
					InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuSpeed );

    this.mnuPause = createJMenuItem(
				"Pause",
				ACTION_PAUSE,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_P,
					InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuPause );

    mnuExtra.add( createJMenuItem(
				"NMI ausl\u00F6sen",
				ACTION_NMI,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_N,
					InputEvent.ALT_MASK ) ) );
    mnuExtra.add( createJMenuItem(
				"Zur\u00FCcksetzen (RESET)",
				ACTION_RESET,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_R,
					InputEvent.ALT_MASK ) ) );
    mnuExtra.add( createJMenuItem(
				"Einschalten (Power On)",
				ACTION_POWER_ON,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_I,
					InputEvent.ALT_MASK ) ) );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuHelp.add( createJMenuItem( "Hilfe...", ACTION_HELP_CONTENT ) );

    this.mnuHelpSys = createJMenuItem(
				"Hilfe zum emulierten System...",
				ACTION_HELP_SYSTEM );
    this.mnuHelpSys.setEnabled( false );
    mnuHelp.add( this.mnuHelpSys );
    mnuHelp.addSeparator();
    mnuHelp.add( createJMenuItem(
			"\u00DCber JKCEMU...",
			ACTION_HELP_ABOUT ) );
    mnuHelp.add( createJMenuItem(
			"Lizenzbestimmungen...",
			ACTION_HELP_LICENSE ) );


    // Menu zusammenbauen
    this.mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuExtra );
    mnuBar.add( mnuHelp );
    setJMenuBar( this.mnuBar );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupCopy = createJMenuItem( "Kopieren", ACTION_COPY );
    this.mnuPopupCopy.setEnabled( false );
    this.mnuPopup.add( this.mnuPopupCopy );

    this.mnuPopupPaste = createJMenuItem( "Einf\u00FCgen", ACTION_PASTE );
    this.mnuPopupPaste.setEnabled( false );
    this.mnuPopup.add( this.mnuPopupPaste );
    this.mnuPopup.addSeparator();

    this.mnuPopup.add( createJMenuItem( "Laden...", ACTION_FILE_LOAD ) );
    this.mnuPopup.add( createJMenuItem( "Speichern...", ACTION_FILE_SAVE ) );
    this.mnuPopup.addSeparator();

    this.mnuPopupAudio = createJMenuItem(
				"Audio/Kassette...",
				ACTION_AUDIO );
    this.mnuPopup.add( this.mnuPopupAudio );

    this.mnuPopupFloppyDisk = createJMenuItem(
				"Diskettenstation...",
				ACTION_FLOPPYDISKS );
    this.mnuPopup.add( this.mnuPopupFloppyDisk );

    this.mnuPopupUSB = createJMenuItem(
				"USB-Anschluss...",
				ACTION_USB );
    this.mnuPopup.add( this.mnuPopupUSB );

    this.mnuPopupKeyboard = createJMenuItem(
				"Tastatur...",
				ACTION_KEYBOARD );
    this.mnuPopup.add( this.mnuPopupKeyboard );

    this.mnuPopupJoystick = createJMenuItem(
				"Joysticks...",
				ACTION_JOYSTICK );
    this.mnuPopup.add( this.mnuPopupJoystick );
    this.mnuPopup.addSeparator();

    mnuPopup.add( createJMenuItem(
				"Einstellungen...",
				ACTION_SETTINGS ) );
    this.mnuPopup.addSeparator();

    this.mnuPopupFullScreen = createJMenuItem(
				TEXT_FULLSCREEN_ON,
				ACTION_FULLSCREEN );
    this.mnuPopup.add( this.mnuPopupFullScreen );

    this.mnuPopupSpeed = createJMenuItem(
				TEXT_MAX_SPEED,
				ACTION_SPEED );
    this.mnuPopup.add( this.mnuPopupSpeed );

    this.mnuPopupPause = createJMenuItem(
				"Pause",
				ACTION_PAUSE );
    this.mnuPopup.add( this.mnuPopupPause );
    this.mnuPopup.addSeparator();

    this.mnuPopup.add( createJMenuItem(
				"Zur\u00FCcksetzen (RESET)",
				ACTION_RESET ) );
    this.mnuPopup.add( createJMenuItem(
				"Einschalten (Power On)",
				ACTION_POWER_ON ) );
    this.mnuPopup.addSeparator();
    this.mnuPopup.add( createJMenuItem( "Beenden", ACTION_QUIT ) );


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
    JPanel panelToolBar = new JPanel(
                new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( panelToolBar, gbc );

    this.toolBar = new JToolBar();
    this.toolBar.setFloatable( false );
    this.toolBar.setBorderPainted( false );
    this.toolBar.setOrientation( JToolBar.HORIZONTAL );
    this.toolBar.setRollover( true );
    panelToolBar.add( this.toolBar );

    this.btnLoad = createImageButton(
				"/images/file/open.png",
				"Laden",
				ACTION_FILE_LOAD );

    this.btnSave = createImageButton(
				"/images/file/save.png",
				"Speichern",
				ACTION_FILE_SAVE );

    this.btnCopy = createImageButton(
				"/images/edit/copy.png",
				"Kopieren",
				ACTION_COPY );
    this.btnCopy.setEnabled( false );

    this.btnPaste = createImageButton(
				"/images/edit/paste.png",
				"Einf\u00FCgen",
				ACTION_PASTE );
    this.btnPaste.setEnabled( false );

    this.btnAudio = createImageButton(
				"/images/audio/audio.png",
				"Audio",
				ACTION_AUDIO );

    this.btnChessboard = createImageButton(
				"/images/chess/chessboard.png",
				"Schachbrett",
				ACTION_CHESSBOARD );

    this.btnFloppyDisks = createImageButton(
				"/images/disk/floppydiskstation.png",
				"Diskettenstation",
				ACTION_FLOPPYDISKS );

    this.btnKeyboard = createImageButton(
				"/images/keyboard/keyboard.png",
				"Tastatur",
				ACTION_KEYBOARD );

    this.btnSettings = createImageButton(
				"/images/file/settings.png",
				"Einstellungen",
				ACTION_SETTINGS );

    this.btnReset = createImageButton(
				"/images/file/reset.png",
				"Zur\u00FCcksetzen (RESET)",
				ACTION_RESET );


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
    this.labelStatus  = new JLabel( "Bereit" );
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


    // sonstiges
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
    }
    updPasteBtns();
    updPauseBtn();
    this.statusRefreshTimer = new javax.swing.Timer( 1000, this );
    this.statusRefreshTimer.start();
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
      }
    }
  }


  public void clearScreenSelection()
  {
    if( this.screenFld != null ) {
      this.screenFld.clearSelection();
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


  public void firePastingTextFinished()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    pastingTextFinished();
		  }
		} );
  }


  public void fireReset( final EmuThread.ResetLevel resetLevel )
  {
    if( this.emuThread != null ) {
      this.emuThread.informWillReset();
    }
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    execReset( resetLevel );
		  }
		} );
  }


  public void fireScreenSizeChanged()
  {
    this.windowSize = null;
    if( !this.fullScreenMode && (getExtendedState() != Frame.MAXIMIZED_BOTH) ) {
      final Window    window    = this;
      final ScreenFld screenFld = this.screenFld;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    screenFld.updPreferredSize();
		    window.pack();
		  }
		} );
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


  public void fireUpdScreenTextActionsEnabled()
  {
    final EmuSys emuSys = getEmuSys();
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    boolean state = false;
		    if( emuSys != null ) {
		      state = emuSys.canExtractScreenText();
		    }
		    if( !state ) {
		      clearScreenSelection();
		    }
		    setScreenTextActionsEnabled( state );
		  }
		} );
  }


  public EmuThread getEmuThread()
  {
    return this.emuThread;
  }


  public static int getDefaultScreenRefreshMillis()
  {
    int rv     = 100;
    int nProcs = Runtime.getRuntime().availableProcessors();
    if( nProcs > 1 ) {
      rv = 50;
      if( nProcs >= 4 ) {
	rv = 20;
      }
    }
    return rv;
  }


  public EmuSys getEmuSys()
  {
    return this.emuThread != null ? this.emuThread.getEmuSys() : null;
  }


  public int getScreenRefreshMillis()
  {
    return this.screenRefreshMillis;
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
	this.primReassFrm = new ReassFrm( this.emuThread );
	EmuUtil.showFrame( this.primReassFrm );
      }
    }
    return this.primReassFrm;
  }


  public DebugFrm openSecondDebugger()
  {
    DebugFrm debugFrm = null;
    if( this.emuThread != null ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
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
    }
    return debugFrm;
  }


  public ReassFrm openSecondReassembler()
  {
    ReassFrm reassFrm = null;
    EmuSys   emuSys   = getEmuSys();
    if( emuSys != null ) {
      reassFrm = openSecondReassembler(
			emuSys.getSecondZ80Memory(),
			emuSys.getSecondSystemName() );
    }
    return reassFrm;
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


  public void setEmuThread( EmuThread emuThread )
  {
    if( this.emuThread != emuThread ) {
      if( this.emuThread != null ) {
	Z80CPU z80cpu = this.emuThread.getZ80CPU();
	if( z80cpu != null ) {
	  z80cpu.removeStatusListener( this );
	}
      }
      this.emuThread = emuThread;
      if( this.emuThread != null ) {
	Z80CPU z80cpu = this.emuThread.getZ80CPU();
	if( z80cpu != null ) {
	  z80cpu.addStatusListener( this );
	}
      }
    }
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      this.copyEnabled  = emuSys.supportsCopyToClipboard();
      this.pasteEnabled = emuSys.supportsPasteFromClipboard();
    } else {
      this.copyEnabled  = false;
      this.pasteEnabled = false;
    }
    updActionBtns( getEmuSys() );
  }


  public boolean setMaxSpeed( Component owner, boolean state )
  {
    boolean rv = false;
    if( this.emuThread != null ) {
      Z80CPU z80cpu = this.emuThread.getZ80CPU();
      if( z80cpu != null ) {
	if( state && this.emuThread.isAudioLineOpen() ) {
	  BaseDlg.showInfoDlg(
		owner,
		"Solange ein Audiokanal ge\u00F6ffnet ist,\n"
			+ "kann nicht auf maximale Geschwindigkeit"
			+ " geschaltet werden\n"
			+ "da der Audiokanal bremst." );
	} else {
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
	    this.mnuPopupSpeed.setText( TEXT_STD_SPEED );
	  } else {
	    this.mnuSpeed.setText( TEXT_MAX_SPEED );
	    this.mnuPopupSpeed.setText( TEXT_MAX_SPEED );
	  }
	  rv = true;
	}
      }
    }
    return rv;
  }


  public void setScreenDirty( boolean state )
  {
    this.screenDirty = state;
  }


  public void setScreenTextSelected( boolean state )
  {
    if( !this.copyEnabled ) {
      state = false;
    }
    this.mnuEditCopy.setEnabled( state );
    this.mnuPopupCopy.setEnabled( state );
    this.btnCopy.setEnabled( state );
  }


  public void showStatusText( String text )
  {
    this.statusRefreshTimer.stop();
    this.labelStatus.setText( text );
    this.statusRefreshTimer.setInitialDelay( 5000 );	// 5 sec. anzeigen
    this.statusRefreshTimer.restart();
  }


  public void startEmulationThread()
  {
    if( this.emuThread != null )
      this.emuThread.start();
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
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      LoadDlg.loadFile(
		this,		// owner
		this,		// ScreenFrm
		file,
		true,		// interactive
		true,		// startEnabled
		true );		// startsSelected
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
    updPasteBtns();
  }


	/* --- Z80StatusListener --- */

  @Override
  public void z80StatusChanged(
			Z80Breakpoint      breakpoint,
			Z80InterruptSource iSource )
  {
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
    if( e != null ) {
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
  }


	/* --- ueberschriebene Methoden fuer KeyListener --- */

  /*
   * Die Taste F10 dient der Menuesteuerung des Emulators
   * (Oeffnen des Menues, von Java so vorgegeben)
   * und wird deshalb nicht an das emulierte System weitergegeben.
   */
  @Override
  public void keyPressed( KeyEvent e )
  {
    if( (this.emuThread != null) && !e.isAltDown()
	&& (e.getKeyCode() != KeyEvent.VK_F10) )
    {
      /*
       * CTRL-M liefert auf verschiedenen Betriebssystemen
       * unterschiedliche ASCII-Codes (10 bzw. 13).
       * Aus diesem Grund wird hier CTRL-M fest auf 13 gemappt,
       * so wie es auch die von JKCEMU emulierten Computer im Original tun.
       */
      if( (e.getKeyCode() == KeyEvent.VK_M)
	  && !e.isAltDown()
	  && !e.isAltGraphDown()
	  && e.isControlDown()
	  && !e.isMetaDown()
	  && !e.isShiftDown() )
      {
	this.emuThread.keyTyped( '\r' );
	this.ignoreKeyChar = true;
	e.consume();
      } else

      if( this.emuThread.keyPressed( e ) ) {
	this.ignoreKeyChar = true;
	e.consume();
      }
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    if( (this.emuThread != null) && !e.isAltDown() ) {
      /*
       * Das Loslassen von F10 und CONTROL nicht melden,
       * da F10 von Java selbst verwendet wird und CONTROL
       * im Tastaturfenster zum Deselktieren der Tasten fuehren wuerde.
       */
      int keyCode = e.getKeyCode();
      if( (keyCode != KeyEvent.VK_F10) && (keyCode != KeyEvent.VK_CONTROL) ) {
	this.emuThread.keyReleased();
	e.consume();
      }
      this.ignoreKeyChar = false;
    }
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    if( (this.emuThread != null) && !e.isAltDown() ) {
      if( this.ignoreKeyChar ) {
	this.ignoreKeyChar = false;
      } else {
	this.emuThread.keyTyped( e.getKeyChar() );
      }
    }
  }


	/* --- ueberschriebene Methoden fuer MouseListener --- */

  @Override
  public void mousePressed( MouseEvent e )
  {
    if( (e.getSource() == this.screenFld) && (e.getClickCount() == 1) ) {
      int m = e.getModifiers();
      if( (m & InputEvent.BUTTON1_MASK) != 0 ) {
	this.screenFld.clearSelection();
	this.screenFld.requestFocus();
	e.consume();
      }
      else if( (m & InputEvent.BUTTON2_MASK) != 0 ) {
	if( Main.getBooleanProperty(
			PROP_PREFIX + PROP_COPY_AND_PASTE_DIRECT,
			DEFAULT_COPY_AND_PASTE_DIRECT ) )
	{
	  String text = checkConvertScreenText(
					this.screenFld.getSelectedText() );
	  if( text != null ) {
	    if( text.length() > 0 ) {
	      setWaitCursor( true );
	      pasteText( text );
	      setWaitCursor( false );
	    }
	  }
	  e.consume();
	}
      }
    }
    if( !e.isConsumed() )
      super.mousePressed( e );
  }


	/* --- ueberschriebene Methoden fuer WindowListener --- */

  @Override
  public void windowActivated( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      Main.setWindowActivated( Main.WINDOW_MASK_SCREEN );
    }
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
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
    if( e.getWindow() == this ) {
      if( this.screenFld != null ) {
	this.screenFld.requestFocus();
      }
      if( Main.isFirstExecution() ) {
	SettingsFrm.open( this );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean   visible             = isVisible();
    boolean   canExtractText      = false;
    boolean   supportsChessboard  = false;
    boolean   supportsFloppyDisks = false;
    boolean   supportsRAMFloppies = false;
    boolean   supportsUSB         = false;
    Z80CPU    secondCPU           = null;
    Z80Memory secondMem           = null;
    int       oldMargin           = this.screenFld.getMargin();
    int       oldScreenScale      = this.screenFld.getScreenScale();
    EmuSys    oldEmuSys           = this.screenFld.getEmuSys();
    EmuSys    emuSys              = getEmuSys();
    if( emuSys != null ) {
      setTitle( "JKCEMU: " + emuSys.getTitle() );
      this.mnuHelpSys.setEnabled( emuSys.getHelpPage() != null );
      this.copyEnabled    = emuSys.supportsCopyToClipboard();
      this.pasteEnabled   = emuSys.supportsPasteFromClipboard();
      canExtractText      = emuSys.canExtractScreenText();
      supportsChessboard  = emuSys.supportsChessboard();
      supportsFloppyDisks = (emuSys.getSupportedFloppyDiskDriveCount() > 0);
      supportsRAMFloppies = emuSys.supportsRAMFloppies();
      supportsUSB         = emuSys.supportsUSB();
      secondCPU           = emuSys.getSecondZ80CPU();
      secondMem           = emuSys.getSecondZ80Memory();
    } else {
      this.copyEnabled  = false;
      this.pasteEnabled = false;
    }
    updActionBtns( emuSys );
    if( !this.copyEnabled ) {
      this.mnuEditCopy.setEnabled( false );
      this.mnuPopupCopy.setEnabled( false );
    }
    updPasteBtns();

    // Bildschirmaktualisierung
    if( this.screenRefreshTimer.isRunning() ) {
      this.screenRefreshTimer.stop();
    }
    if( emuSys != null ) {
      if( !emuSys.isAutoScreenRefresh() ) {
	this.screenRefreshMillis = EmuUtil.parseIntProperty(
				props,
				PROP_PREFIX + PROP_SCREEN_REFRESH_MS,
				0,
				getDefaultScreenRefreshMillis() );
	if( this.screenRefreshMillis < 10 ) {
	  this.screenRefreshMillis = getDefaultScreenRefreshMillis();
	}
	this.screenRefreshTimer.setDelay( this.screenRefreshMillis );
	this.screenRefreshTimer.start();
      }
    }

    // Skalierung nur anpassen, wenn das Fenster noch nicht angezeigt wird
    int screenScale = oldScreenScale;
    if( !visible ) {
      this.screenScale = EmuUtil.parseIntProperty(
					props,
					"jkcemu.screen.scale",
					1,
					1 );
      setScreenScale( this.screenScale );
    }

    // Bildschirmausgabe
    this.screenFld.setEmuSys( emuSys );
    setScreenTextActionsEnabled( canExtractText );
    setScreenDirty( true );

    /*
     * Fenstergroesse
     *
     * Den Vollbildmodus nur einstellen,
     * wenn das Fenster noch nicht angezeigt wird
     */
    boolean rv     = false;
    boolean done   = false;
    int     margin = EmuUtil.parseIntProperty(
				props,
				PROP_PREFIX + PROP_SCREEN_MARGIN,
				0,
				ScreenFld.DEFAULT_MARGIN );
    this.screenFld.setMargin( margin );
    if( visible && (emuSys != null) && (oldEmuSys != null) ) {
      if( emuSys.getClass().equals( oldEmuSys.getClass() )
	  && (screenScale == oldScreenScale)
	  && (emuSys.getScreenWidth() == oldEmuSys.getScreenWidth())
	  && (emuSys.getScreenHeight() == oldEmuSys.getScreenHeight())
	  && (margin == oldMargin) )
      {
	done = true;
      }
    }
    if( !done ) {
      rv = super.applySettings( props, resizable );
    }
    if( visible ) {
      if( this.fullScreenMode ) {
	updFullScreenScale();
      }
    } else {
      setFullScreenMode( EmuUtil.parseBooleanProperty(
					props,
					"jkcemu.screen.fullscreen",
					false ) );
    }
    if( !done && !rv ) {
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

    // ggf. nicht relevante Fenster schliessen
    if( !supportsChessboard ) {
      ChessboardFrm.close();
    }
    if( !supportsFloppyDisks ) {
      FloppyDiskStationFrm.close();
    }
    if( this.keyboardFrm != null ) {
      if( emuSys != null ) {
	if( !this.keyboardFrm.accepts( emuSys ) ) {
	  this.keyboardFrm.doClose();
	}
      } else {
	this.keyboardFrm.doClose();
      }
    }
    Plotter plotter = emuSys.getPlotter();
    if( plotter != null ) {
      PlotterFrm.lazySetPlotter( plotter );
    } else {
      PlotterFrm.close();
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
    if( !supportsRAMFloppies ) {
      RAMFloppyFrm.close();
    }
    if( !supportsUSB ) {
      USBInterfaceFrm.close();
    }
    return rv;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
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
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      emuSys.openBasicProgram();
	    }
	  }
	  else if( actionCmd.equals( ACTION_BASIC_SAVE ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      emuSys.saveBasicProgram();
	    }
	  }
	  else if( actionCmd.equals( ACTION_SCREENIMAGE_SHOW ) ) {
	    rv = true;
	    doFileScreenImageShow();
	  }
	  else if( actionCmd.equals( ACTION_SCREENIMAGE_COPY ) ) {
	    rv = true;
	    doFileScreenImageCopy();
	  }
	  else if( actionCmd.equals( ACTION_SCREENIMAGE_SAVE ) ) {
	    rv = true;
	    doFileScreenImageSave();
	  }
	  else if( actionCmd.equals( ACTION_SCREENTEXT_SHOW ) ) {
	    rv = true;
	    doFileScreenTextShow();
	  }
	  else if( actionCmd.equals( ACTION_SCREENTEXT_SAVE ) ) {
	    rv = true;
	    doFileScreenTextSave();
	  }
	  else if( actionCmd.equals( ACTION_SCREENTEXT_COPY ) ) {
	    rv = true;
	    doFileScreenTextCopy();
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
	  else if( actionCmd.equals( ACTION_COPY ) ) {
	    rv = true;
	    doEditCopy();
	  }
	  else if( actionCmd.equals( ACTION_PASTE ) ) {
	    rv = true;
	    doEditPaste( false );
	  }
	  else if( actionCmd.equals( ACTION_PASTE_WITH ) ) {
	    rv = true;
	    doEditPaste( true );
	  }
	  else if( actionCmd.equals( ACTION_PASTE_CANCEL ) ) {
	    rv = true;
	    doEditPasteCancel();
	  }
	  else if( actionCmd.equals( ACTION_SCALE_1 ) ) {
	    rv = true;
	    doScreenScale( 1 );
	  }
	  else if( actionCmd.equals( ACTION_SCALE_2 ) ) {
	    rv = true;
	    doScreenScale( 2 );
	  }
	  else if( actionCmd.equals( ACTION_SCALE_3 ) ) {
	    rv = true;
	    doScreenScale( 3 );
	  }
	  else if( actionCmd.equals( ACTION_SCALE_4 ) ) {
	    rv = true;
	    doScreenScale( 4 );
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
	    JoystickFrm.open( this.emuThread );
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
	    if( e.getSource() == this.mnuPopupFullScreen ) {
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
	  else if( actionCmd.equals( ACTION_HELP_CONTENT ) ) {
	    rv = true;
	    HelpFrm.open( HELP_PAGE_HOME );
	  }
	  else if( actionCmd.equals( ACTION_HELP_SYSTEM ) ) {
	    rv = true;
	    doHelpSystem();
	  }
	  else if( actionCmd.equals( ACTION_HELP_ABOUT ) ) {
	    rv = true;
	    (new AboutDlg( this )).setVisible( true );
	  }
	  else if( actionCmd.equals( ACTION_HELP_LICENSE ) ) {
	    rv = true;
	    HelpFrm.open( HELP_PAGE_LICENSE );
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = true;
    if( EmuUtil.parseBoolean(
		Main.getProperty( PROP_PREFIX + PROP_CONFIRM_QUIT ),
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
    // Programmbeendigung nicht durch Exception verhindern lassen
    try {

      // untergeordnete Fenster schliessen
      Frame[] frms = Frame.getFrames();
      if( frms != null ) {
	for( Frame f : frms ) {
	  if( f != this ) {
	    if( f instanceof BaseFrm ) {
	      if( !((BaseFrm) f).doQuit() ) {
		return false;
	      }
	    } else {
	      f.setVisible( false );
	      f.dispose();
	    }
	  }
	}
      }

      // Threads beenden
      if( this.emuThread != null ) {
	this.emuThread.stopEmulator();
	Main.getThreadGroup().interrupt();

	// max. eine halbe Sekunde auf Thread-Beendigung warten
	try {
	  this.emuThread.join( 500 );
	}
	catch( InterruptedException ex ) {}
      }
      if( !super.doClose() ) {
	return false;
      }
    }
    catch( Exception ex ) {}
    Main.exitSuccess();
    return true;
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
			prefix + BaseFrm.PROP_WINDOW_X,
			String.valueOf( this.windowLocation.x ) );
	props.setProperty(
			prefix + BaseFrm.PROP_WINDOW_Y,
			String.valueOf( this.windowLocation.y ) );
	props.setProperty(
			prefix + BaseFrm.PROP_WINDOW_WIDTH,
			String.valueOf( this.windowSize.width ) );
	props.setProperty(
			prefix + BaseFrm.PROP_WINDOW_HEIGHT,
			String.valueOf( this.windowSize.height ) );
      }
      props.setProperty(
		"jkcemu.screen.fullscreen",
		String.valueOf( this.fullScreenMode ) );
      props.setProperty(
		"jkcemu.screen.scale",
		String.valueOf( this.screenScale ) );
    }
  }


  @Override
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


  @Override
  public void resetFired()
  {
    this.mnuEditPasteCancel.setEnabled( false );
    updPasteBtns();
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


	/* --- Aktionen im Menu Datei --- */

  private void doFileLoad( boolean startEnabled )
  {
    boolean startSelected   = false;
    boolean loadWithOptions = true;
    File    file            = null;
    File    preSelection    = EmuUtil.getDirectory(
			Main.getLastDirFile( Main.FILE_GROUP_SOFTWARE ) );
    if( EmuUtil.isJKCEMUFileDialogSelected() ) {
      FileSelectDlg dlg = new FileSelectDlg(
			this,
			FileSelectDlg.Mode.LOAD,
			startEnabled,
			true,		// loadWithOptionsEnabled
			"Datei in Arbeitsspeicher laden",
			preSelection,
			EmuUtil.getBinaryFileFilter(),
			EmuUtil.getAC1Basic6FileFilter(),
			EmuUtil.getBasicFileFilter(),
			EmuUtil.getKCSystemFileFilter(),
			EmuUtil.getKCBasicFileFilter(),
			EmuUtil.getTapeFileFilter(),
			EmuUtil.getHeadersaveFileFilter(),
			EmuUtil.getHexFileFilter() );
      dlg.setVisible( true );
      file            = dlg.getSelectedFile();
      loadWithOptions = dlg.isLoadWithOptionsSelected();
      startSelected   = dlg.isStartSelected();
    } else {
      file = EmuUtil.showFileOpenDlg(
			this,
			"Datei in Arbeitsspeicher laden",
			preSelection,
			EmuUtil.getBinaryFileFilter(),
			EmuUtil.getAC1Basic6FileFilter(),
			EmuUtil.getBasicFileFilter(),
			EmuUtil.getKCSystemFileFilter(),
			EmuUtil.getKCBasicFileFilter(),
			EmuUtil.getTapeFileFilter(),
			EmuUtil.getHeadersaveFileFilter(),
			EmuUtil.getHexFileFilter() );
    }
    if( file != null ) {
      LoadDlg.loadFile(
		this,			// owner
		this,
		file,
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


  private void doFileScreenImageCopy()
  {
    try {
      Toolkit tk = getToolkit();
      if( tk != null ) {
	Clipboard clp = tk.getSystemClipboard();
	if( clp != null ) {
	  ImgSelection ims = new ImgSelection(
				this.screenFld.createBufferedImage() );
	  clp.setContents( ims, ims );
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doFileScreenImageSave()
  {
    if( ImgSaver.saveImageAs(
			this,
			this.screenFld.createBufferedImage(),
			null ) != null )
    {
      showStatusText( "Bilddatei gespeichert" );
    }
  }


  private void doFileScreenImageShow()
  {
    ImageFrm.open( this.screenFld.createBufferedImage(), "Schnappschuss" );
  }


  private void doFileScreenTextCopy()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      EmuUtil.copyToClipboard(
			this,
			checkConvertScreenText( emuSys.getScreenText() ) );
    }
  }


  private void doFileScreenTextSave()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      String screenText = checkConvertScreenText( emuSys.getScreenText() );
      if( screenText != null ) {
	File file = EmuUtil.showFileSaveDlg(
			this,
			"Textdatei speichern",
			Main.getLastDirFile( Main.FILE_GROUP_SCREEN ),
			EmuUtil.getTextFileFilter() );
	if( file != null ) {
	  String fileName = file.getPath();
	  if( fileName != null ) {
	    BufferedWriter out = null;
	    try {
	      out = new BufferedWriter( new FileWriter( file ) );

	      int len = screenText.length();
	      for( int i = 0; i < len; i++ ) {
		char ch = screenText.charAt( i );
		if( ch == '\n' ) {
		  out.newLine();
		} else {
		  out.write( ch );
		}
	      }

	      out.close();
	      out = null;
	      showStatusText( "Textdatei gespeichert" );
	    }
	    catch( Exception ex ) {
	      BaseDlg.showErrorDlg(
		this,
		fileName + ":\nSpeichern der Datei fehlgeschlagen\n\n"
			+ ex.getMessage() );
	    }
	    finally {
	      EmuUtil.closeSilent( out );
	    }
	  }
	}
      }
    }
  }


  private void doFileScreenTextShow()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      String screenText = checkConvertScreenText( emuSys.getScreenText() );
      if( screenText != null ) {
	openText( screenText );
      }
    }
  }


  private void doFloppyDisk()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      int n = emuSys.getSupportedFloppyDiskDriveCount();
      if( n > 0 ) {
	FloppyDiskStationFrm frm
			= FloppyDiskStationFrm.getSharedInstance( this );
	frm.setDriveCount( n );
	EmuUtil.showFrame( frm );
      } else {
	BaseDlg.showInfoDlg(
		this,
		"Das gerade emulierte System unterst\u00FCtzt"
			+ " keine Floppy Disks." );
      }
    }
  }


  private void doRAMFloppies()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsRAMFloppy1() || emuSys.supportsRAMFloppy2() ) {
	RAMFloppyFrm.open( this.emuThread );
      }
    }
  }


	/* --- Aktionen im Menu Bearbeiten --- */

  private void doEditCopy()
  {
    EmuUtil.copyToClipboard(
		this,
		checkConvertScreenText( this.screenFld.getSelectedText() ) );
  }


  private void doEditPaste( boolean askConversion )
  {
    if( this.clipboard != null ) {
      try {
	if( this.clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor ) )
	{
	  Object data = this.clipboard.getData( DataFlavor.stringFlavor );
	  if( data != null ) {
	    String text = data.toString();
	    if( text != null ) {
	      if( !text.isEmpty() ) {
		if( askConversion ) {
		  switch( OptionDlg.showOptionDlg(
			this,
			"Mit welcher Gro\u00DF-/Keinschreibung"
				+ " soll der Text eingef\u00FCgt werden?",
			"Gro\u00DF-/Keinschreibung",
			0,
			"Gro\u00DF-/Keinschreibung beibehalten",
			"Alles in Gro\u00DFbuchstaben",
			"Alles in Kleinbuchstaben",
			"Gro\u00DF-/Keinschreibung umkehren" ) )
		  {
		    case 0:
		      // nichts aendern
		      break;
		    case 1:
		      /*
		       * Es wird hier absichtlich nicht String.toUpperCase()
		       * verwendet, da dort naemlich z.B. aus einem SZ
		       * ('\u00DF') Doppel-S wird.
		       * Das ist dann inkonsitent zur Methode
		       * EmuUtil.toReverseCase(...).
		       */
		      text = TextUtil.toUpperCase( text );
		      break;
		    case 2:
		      text = TextUtil.toLowerCase( text );
		      break;
		    case 3:
		      text = TextUtil.toReverseCase( text );
		      break;
		    default:
		      text = null;
		  }
		}
		if( text != null ) {
		  pasteText( text );
		}
	      }
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
  }


  private void doEditPasteCancel()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      emuSys.cancelPastingText();
    }
  }


	/* --- Aktionen im Menu Extra --- */

  private void doScreenScale( int screenScale )
  {
    this.screenScale = screenScale;
    this.screenFld.setScreenScale( screenScale );
    if( !this.fullScreenMode ) {
      pack();
    }
  }


  private void doExtraChessboard()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
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
  }


  private void doExtraKeyboard()
  {
    if( this.keyboardFrm != null ) {
      EmuUtil.showFrame( this.keyboardFrm );
    } else {
      EmuSys emuSys = getEmuSys();
      if( emuSys != null ) {
	try {
	  if( emuSys.supportsKeyboardFld() ) {
	    try {
	      AbstractKeyboardFld kbFld = emuSys.createKeyboardFld();
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
  }


  private void doExtraPlotter()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      Plotter plotter = emuSys.getPlotter();
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
  }


  private void doExtraDebugger()
  {
    if( this.emuThread != null ) {
      int       sysNum     = 0;
      Z80CPU    secondCPU  = null;
      Z80Memory secondMem  = null;
      String    secondName = null;
      EmuSys    emuSys     = emuThread.getEmuSys();
      if( emuSys != null ) {
	secondCPU  = emuSys.getSecondZ80CPU();
	secondMem  = emuSys.getSecondZ80Memory();
	secondName = emuSys.getSecondSystemName();
      }
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
  }


  private void doExtraDiskImgUnpack()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"CP/M-Diskettenabbilddatei entpacken",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			EmuUtil.getPlainDiskFileFilter(),
			EmuUtil.getAnaDiskFileFilter(),
			EmuUtil.getCopyQMFileFilter(),
			EmuUtil.getDskFileFilter(),
			EmuUtil.getImageDiskFileFilter(),
			EmuUtil.getTeleDiskFileFilter() );
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
	    done = true;
	  } else {
	    AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this,
								file,
								true );
	    if( disk != null ) {
	      if( DiskUtil.checkAndConfirmWarning( this, disk ) ) {
		DiskUtil.unpackDisk( this, file, disk, true );
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
    String driveFileName = DriveSelectDlg.selectDriveFileName( this );
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
    if( this.emuThread != null ) {
      int       sysNum     = 0;
      Z80Memory secondMem  = null;
      String    secondName = null;
      EmuSys    emuSys     = emuThread.getEmuSys();
      if( emuSys != null ) {
	secondMem  = emuSys.getSecondZ80Memory();
	secondName = emuSys.getSecondSystemName();
      }
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
  }


  private void doExtraReassembler()
  {
    if( this.emuThread != null ) {
      int       sysNum     = 0;
      Z80Memory secondMem  = null;
      String    secondName = null;
      EmuSys    emuSys     = emuThread.getEmuSys();
      if( emuSys != null ) {
	secondMem  = emuSys.getSecondZ80Memory();
	secondName = emuSys.getSecondSystemName();
      }
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
    File file = dlg.getSelectedProfile();
    if( file != null ) {
      Properties props = Main.loadProperties( file );
      if( props != null ) {
	this.emuThread.applySettings( props );
	Main.applyProfileToFrames( file, props, true, null );
	FloppyDiskStationFrm.getSharedInstance( this ).openDisks( props );
	fireReset( EmuThread.ResetLevel.COLD_RESET );
      }
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
    EmuSys emuSys = emuThread.getEmuSys();
    if( emuSys != null ) {
      z80cpu = emuSys.getSecondZ80CPU();
      if( z80cpu != null ) {
	if( z80cpu.isActive() ) {
	  z80cpu.firePause( !z80cpu.isPause() );
	}
      }
    }
  }


  private void doExtraNMI()
  {
    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      if( EmuUtil.parseBoolean(
		Main.getProperty( PROP_PREFIX + PROP_CONFIRM_NMI ),
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
    if( EmuUtil.parseBoolean(
		Main.getProperty( PROP_PREFIX + PROP_CONFIRM_RESET ),
		DEFAULT_CONFIRM_RESET ) )
    {
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie den Emulator neu starten?",
		"Best\u00E4tigung" ) )
      {
	fireReset( EmuThread.ResetLevel.WARM_RESET );
      }
    } else {
      fireReset( EmuThread.ResetLevel.WARM_RESET );
    }
  }


  private void doExtraPowerOn()
  {
    if( EmuUtil.parseBoolean(
		Main.getProperty( PROP_PREFIX + PROP_CONFIRM_POWER_ON ),
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
	fireReset( EmuThread.ResetLevel.POWER_ON );
      }
    } else {
      fireReset( EmuThread.ResetLevel.POWER_ON );
    }
  }


  private void doExtraUSB()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsUSB() ) {
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
  }


	/* --- Aktionen im Menu Hilfe --- */

  private void doHelpSystem()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      String page = emuSys.getHelpPage();
      if( page != null ) {
	HelpFrm.open( page );
      }
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
		"Abbrechen" };

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


  private String checkConvertScreenText( String text )
  {
    EmuSys emuSys = getEmuSys();
    if( (this.emuThread != null) && (emuSys != null) && (text != null) ) {
      int len = text.length();
      if( len > 0 ) {

	// ggf. deutsche Umlaute konvertieren
	if( emuSys.shouldAskConvertScreenChar() ) {
	  boolean state = false;
	  for( int i = 0; i < len; i++ ) {
	    char ch = text.charAt( i );
	    if( (ch == 0x5B) || (ch == 0x5C) || (ch == 0x5D)
		|| (ch == 0x7B) || (ch == 0x7C) || (ch == 0x7D)
		|| (ch == 0x7E) )
	    {
	      state = true;
	      break;
	    }
	  }
	  if( state ) {
	    Boolean iso646de = this.emuThread.getISO646DE();
	    if( iso646de == null ) {
	      String[]    options = { "ASCII", "Umlaute", "Abbrechen" };
	      JOptionPane pane    = new JOptionPane(
		"Der Text enth\u00E4lt Zeichencodes, die nach ASCII"
			+ " die Zeichen [ \\ ] { | } ~\n"
			+ "und nach ISO646-DE deutsche Umlaute darstellen.\n"
			+ "Da sie eine externe Zeichensatzdatei"
			+ " eingebunden haben,\n"
			+ "kann JKCEMU nicht wissen, ob ASCII-Zeichen\n"
			+ "oder deutsche Umlaute angezeigt werden.\n"
			+ "Wie sind diese Zeichencodes zu interpretieren?",
		JOptionPane.QUESTION_MESSAGE );
	      pane.setOptions( options );
	      pane.setWantsInput( false );
	      pane.createDialog( this, "Zeichensatz" ).setVisible( true );
	      Object value = pane.getValue();
	      if( value != null ) {
		if( value.equals( options[ 0 ] ) ) {
		  state = false;
		  this.emuThread.setISO646DE( false );
		} else if( value.equals( options[ 1 ] ) ) {
		  state = true;
		  this.emuThread.setISO646DE( true );
		} else {
		  text = null;
		}
	      } else {
		text = null;
	      }
	    }
	    if( state && (text != null) ) {
	      StringBuilder buf = new StringBuilder( len );
	      for( int k = 0; k < len; k++ ) {
		char ch = text.charAt( k );
		switch( ch ) {
		  case '[':
		    buf.append( (char) '\u00C4' );
		    break;
		  case '\\':
		    buf.append( (char) '\u00D6' );
		    break;
		  case ']':
		    buf.append( (char) '\u00DC' );
		    break;
		  case '{':
		    buf.append( (char) '\u00E4' );
		    break;
		  case '|':
		    buf.append( (char) '\u00F6' );
		    break;
		  case '}':
		    buf.append( (char) '\u00FC' );
		    break;
		  case '~':
		    buf.append( (char) '\u00DF' );
		    break;
		  default:
		    buf.append( (char) ch );
		}
	      }
	      text = buf.toString();
	    }
	  }
	}
      }
    }
    return text;
  }


  /*
   * Das Laden von Bildern muss in diesem Fenster unabhaengig
   * von der Main-Klasse geschehen,
   * da die Methode im Konstruktor aufgerufen wird
   * und dieses Frame das erste und oberste Fenster der Applikation ist.
   * Zu diesem Zeitpunkt ist die Referenz in der Main-Klasse
   * auf dieses Fenster noch nicht gesetzt.
   * Die Implementierung fuer das Laden von Bildern in der Superklasse
   * verwendet jedoch die Main-Klasse,
   * und diese wiederum benoetigt das erste Applikationsfenster.
   */
  private JButton createImageButton(
				String imgName,
				String text,
				String actionCmd )
  {
    JButton btn = null;
    Toolkit tk  = getToolkit();
    if( tk != null ) {
      URL url = getClass().getResource( imgName );
      if( url != null ) {
	Image img = tk.createImage( url );
	if( img != null ) {
	  btn = new JButton( new ImageIcon( img ) );
	  btn.setToolTipText( text );
	  Main.putImage( imgName, img );
	}
      }
    }
    if( btn == null ) {
      btn = new JButton( text );
    }
    btn.setFocusable( false );
    if( actionCmd != null ) {
      btn.setActionCommand( actionCmd );
    }
    btn.addActionListener( this );
    return btn;
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


  private void execReset( EmuThread.ResetLevel resetLevel )
  {
    this.emuThread.fireReset( resetLevel );
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
	this.secondReassFrm = new ReassFrm( secondMem );
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


  private void pastingTextFinished()
  {
    this.mnuEditPasteCancel.setEnabled( false );
    updPasteBtns();
  }


  private void pasteText( String text )
  {
    EmuSys emuSys = getEmuSys();
    if( (emuSys != null) && (text != null) ) {
      if( !text.isEmpty() ) {
	// ggf. Zeichen mappen
	if( text.indexOf( '\r' ) >= 0 ) {
	  text = text.replace( '\r', '\n' );
	}
	if( text.indexOf( '\u00A0' ) >= 0 ) {
	  text = text.replace( '\u00A0', '\u0020' );
	}
	this.mnuEditPasteCancel.setEnabled( true );
	updPasteBtns();
	emuSys.startPastingText( text );
      }
    }
  }


  private void refreshStatus()
  {
    String msg = "Bereit";
    if( this.emuThread != null ) {
      Z80CPU z80cpu = this.emuThread.getZ80CPU();
      if( z80cpu != null ) {
	if( z80cpu.isActive() ) {
	  if( z80cpu.isPause() ) {
	    if( z80cpu.isDebugEnabled() ) {
	      msg = "Debug-Haltepunkt erreicht";
	    } else {
	      msg = "Pause";
	    }
	    EmuSys  emuSys = getEmuSys();
	    if( emuSys != null ) {
	      emuSys.updDebugScreen();
	    }
	    this.screenDirty = true;
	    this.screenFld.repaint();
	  } else {
	    String mhzText = createMHzText( z80cpu );
	    if( mhzText != null ) {
	      msg = String.format(
			"Emulierte Taktfrequenz: %s MHz",
			mhzText );

	      EmuSys emuSys = this.emuThread.getEmuSys();
	      if( emuSys != null ) {
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
    }
    this.labelStatus.setText( msg );
  }


  private void setFullScreenMode( boolean state )
  {
    if( state != this.fullScreenMode ) {
      clearScreenSelection();
      setVisible( false );
      dispose();
      this.fullScreenMode = state;
      if( this.fullScreenMode ) {
	this.windowLocation = getLocation();
	this.windowSize     = getSize();
	this.mnuBar.setVisible( false );
	this.toolBar.setVisible( false );
	this.labelStatus.setVisible( false );
	if( !updFullScreenScale() ) {
	  setExtendedState( Frame.MAXIMIZED_BOTH );
	}
	try {
	  setUndecorated( true );
	}
	catch( IllegalComponentStateException ex ) {}
	this.mnuFullScreen.setText( TEXT_FULLSCREEN_OFF );
	this.mnuPopupFullScreen.setText( TEXT_FULLSCREEN_OFF );
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
	this.mnuBar.setVisible( true );
	this.toolBar.setVisible( true );
	this.labelStatus.setVisible( true );
	setScreenScale( this.screenScale );
	try {
	  setUndecorated( false );
	}
	catch( IllegalComponentStateException ex ) {}
	setExtendedState( Frame.NORMAL );
	if( this.windowLocation != null ) {
	  setLocation( this.windowLocation );
	}
	if( this.windowSize != null ) {
	  setSize( this.windowSize );
	} else {
	  screenFld.updPreferredSize();
	  pack();
	}
	this.mnuFullScreen.setText( TEXT_FULLSCREEN_ON );
	this.mnuPopupFullScreen.setText( TEXT_FULLSCREEN_ON );
      }
      setVisible( true );
    }
  }


  private void setScreenScale( int screenScale )
  {
    this.screenFld.setScreenScale( this.screenScale );
    switch( screenScale ) {
      case 1:
	this.btnScreenScale1.setSelected( true );
	break;

      case 2:
	this.btnScreenScale2.setSelected( true );
	break;

      case 3:
	this.btnScreenScale3.setSelected( true );
	break;

      case 4:
	this.btnScreenScale4.setSelected( true );
	break;
    }
  }


  private void setScreenTextActionsEnabled( boolean state )
  {
    this.mnuScreenTextShow.setEnabled( state );
    this.mnuScreenTextCopy.setEnabled( state );
    this.mnuScreenTextSave.setEnabled( state );
  }


  private void updActionBtns( EmuSys emuSys )
  {
    boolean supportsOpenBasic   = false;
    boolean supportsSaveBasic   = false;
    boolean supportsAudio       = false;
    boolean supportsChessboard  = false;
    boolean supportsFloppyDisks = false;
    boolean supportsJoystick    = false;
    boolean supportsKeyboardFld = false;
    boolean supportsPlotter     = false;
    boolean supportsPrinter     = false;
    boolean supportsRAMFloppies = false;
    boolean supportsUSB         = false;
    if( emuSys != null ) {
      supportsOpenBasic   = emuSys.supportsOpenBasic();
      supportsSaveBasic   = emuSys.supportsSaveBasic();
      supportsAudio       = emuSys.supportsAudio();
      supportsChessboard  = emuSys.supportsChessboard();
      supportsFloppyDisks = (emuSys.getSupportedFloppyDiskDriveCount() > 0);
      supportsJoystick    = (emuSys.getSupportedJoystickCount() > 0);
      supportsKeyboardFld = emuSys.supportsKeyboardFld();
      supportsPlotter     = (emuSys.getPlotter() != null);
      supportsPrinter     = emuSys.supportsPrinter();
      supportsUSB         = emuSys.supportsUSB();
      supportsRAMFloppies = emuSys.supportsRAMFloppies();
    }

    // Menueeintrage
    this.mnuBasicOpen.setEnabled( supportsOpenBasic );
    this.mnuBasicSave.setEnabled( supportsSaveBasic );
    this.mnuFloppyDisks.setEnabled( supportsFloppyDisks );
    this.mnuPopupFloppyDisk.setEnabled( supportsFloppyDisks );
    this.mnuAudio.setEnabled( supportsAudio );
    this.mnuPopupAudio.setEnabled( supportsAudio );
    this.mnuChessboard.setEnabled( supportsChessboard );
    this.mnuJoystick.setEnabled( supportsJoystick );
    this.mnuPopupJoystick.setEnabled( supportsJoystick );
    this.mnuKeyboard.setEnabled( supportsKeyboardFld );
    this.mnuPopupKeyboard.setEnabled( supportsKeyboardFld );
    this.mnuPlotter.setEnabled( supportsPlotter );
    this.mnuPrintJobs.setEnabled( supportsPrinter );
    this.mnuRAMFloppies.setEnabled( supportsRAMFloppies );
    this.mnuUSB.setEnabled( supportsUSB );
    this.mnuPopupUSB.setEnabled( supportsUSB );

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
	    if( emuSys != null ) {
	      int wSys = emuSys.getScreenWidth();
	      int hSys = emuSys.getScreenHeight();
	      if( (wSys > 0) && (hSys > 0) ) {
		int scale = Math.min( (w - 50) / wSys, (h - 50) / hSys );
		this.screenFld.setScreenScale( scale > 1 ? scale : 1 );
		rv = true;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void updPasteBtns()
  {
    boolean state = false;
    if( this.pasteEnabled
	&& (this.clipboard != null)
	&& !this.mnuEditPasteCancel.isEnabled() )
    {
      try {
	state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor );
      }
      catch( Exception ex ) {}
    }
    this.mnuEditPaste.setEnabled( state );
    this.mnuEditPasteWith.setEnabled( state );
    this.mnuPopupPaste.setEnabled( state );
    this.btnPaste.setEnabled( state );
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
	  this.mnuPopupPause.setEnabled( true );
	} else {
	  this.mnuPause.setEnabled( false );
	  this.mnuPopupPause.setEnabled( false );
	}
      }
    }
    this.mnuPause.setText( pauseText );
    this.mnuPopupPause.setText( pauseText );
  }
}
