/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster mit der Bildschirmanzeige
 */

package jkcemu.base;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.*;
import java.net.URL;
import java.text.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.audio.AudioFrm;
import jkcemu.disk.*;
import jkcemu.filebrowser.*;
import jkcemu.image.*;
import jkcemu.print.PrintListFrm;
import jkcemu.text.EditFrm;
import jkcemu.tools.*;
import jkcemu.tools.calculator.CalculatorFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexedit.*;
import z80emu.*;


public class ScreenFrm extends BasicFrm implements
						DropTargetListener,
						FlavorListener,
						Z80StatusListener
{
  private static final int DEFAULT_SCREEN_REFRESH_MILLIS = 30;

  private JMenuItem            mnuScreenTextShow;
  private JMenuItem            mnuScreenTextCopy;
  private JMenuItem            mnuScreenTextSave;
  private JMenuItem            mnuEditCopy;
  private JMenuItem            mnuEditPaste;
  private JMenuItem            mnuEditPasteStop;
  private JMenuItem            mnuEditPasteContinue;
  private JMenuItem            mnuExtraPause;
  private JMenuItem            mnuHelpSys;
  private JRadioButtonMenuItem btnScreenScale1;
  private JRadioButtonMenuItem btnScreenScale2;
  private JRadioButtonMenuItem btnScreenScale3;
  private JRadioButtonMenuItem btnScreenScale4;
  private JPopupMenu           mnuPopup;
  private JMenuItem            mnuPopupCopy;
  private JMenuItem            mnuPopupPaste;
  private JToolBar             toolBar;
  private JButton              btnLoad;
  private JButton              btnSave;
  private JButton              btnCopy;
  private JButton              btnPaste;
  private JButton              btnAudio;
  private JButton              btnChessboard;
  private JButton              btnFloppyDiskStation;
  private JButton              btnFileBrowser;
  private JButton              btnReset;
  private JLabel               labelStatus;
  private boolean              ignoreKeyChar;
  private volatile boolean     chessboardDirty;
  private volatile boolean     screenDirty;
  private ScreenFld            screenFld;
  private int                  screenRefreshMillis;
  private javax.swing.Timer    screenRefreshTimer;
  private javax.swing.Timer    statusRefreshTimer;
  private NumberFormat         speedFmt;
  private Map<Class,BasicFrm>  subFrms;
  private EmuThread            emuThread;
  private Clipboard            clipboard;
  private PasteTextMngr        pasteTextMngr;
  private String               pasteRemainText;
  private ChessboardFrm        chessboardFrm;
  private FloppyDiskStationFrm floppyDiskStationFrm;
  private DebugFrm             secondDebugFrm;
  private MemEditFrm           secondMemEditFrm;
  private ReassFrm             secondReassFrm;


  public ScreenFrm()
  {
    setTitle( "JKCEMU" );


    // Initialisierungen
    this.emuThread            = null;
    this.pasteTextMngr        = null;
    this.pasteRemainText      = null;
    this.chessboardFrm        = null;
    this.floppyDiskStationFrm = null;
    this.secondDebugFrm       = null;
    this.secondMemEditFrm     = null;
    this.secondReassFrm       = null;
    this.ignoreKeyChar        = false;
    this.chessboardDirty      = false;
    this.screenDirty          = false;
    this.screenRefreshMillis  = DEFAULT_SCREEN_REFRESH_MILLIS;
    this.screenRefreshTimer   = new javax.swing.Timer(
					this.screenRefreshMillis,
					this );

    this.clipboard = null;
    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
    }
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
    }

    this.subFrms  = new Hashtable<Class,BasicFrm>();
    this.speedFmt = NumberFormat.getNumberInstance();
    if( this.speedFmt instanceof DecimalFormat ) {
      ((DecimalFormat) this.speedFmt).applyPattern( "###,###,##0.0#" );
    }


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    mnuFile.add( createJMenuItem(
			"Laden...",
			"file.load",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_L,
					InputEvent.ALT_MASK ) ) );
    mnuFile.add( createJMenuItem(
			"Speichern...",
			"file.save",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_S,
					InputEvent.ALT_MASK ) ) );
    mnuFile.addSeparator();
    mnuFile.add( createJMenuItem(
			"BASIC-Programm im Texteditor \u00F6ffnen...",
			"file.basic.open" ) );
    mnuFile.add( createJMenuItem(
			"BASIC-Programm speichern...",
			"file.basic.save" ) );
    mnuFile.addSeparator();


    // Untermenu RAM-Floppy 1
    JMenu mnuRAMFloppy1 = new JMenu( "RAM-Floppy 1" );
    mnuFile.add( mnuRAMFloppy1 );
    mnuRAMFloppy1.add( createJMenuItem(
			"Laden...",
			"file.ramfloppy_1.load" ) );
    mnuRAMFloppy1.add( createJMenuItem(
			"Speichern...",
			"file.ramfloppy_1.save" ) );

    // Untermenu RAM-Floppy 2
    JMenu mnuRAMFloppy2 = new JMenu( "RAM-Floppy 2" );
    mnuFile.add( mnuRAMFloppy2 );
    mnuRAMFloppy2.add( createJMenuItem(
			"Laden...",
			"file.ramfloppy_2.load" ) );
    mnuRAMFloppy2.add( createJMenuItem(
			"Speichern...",
			"file.ramfloppy_2.save" ) );

    mnuFile.add( createJMenuItem(
			"Diskettenstation...",
			"file.floppydisk.station" ) );
    mnuFile.addSeparator();

    // Untermenu Schnappschuss der Bildschirmausgabe
    JMenu mnuScreen = new JMenu( "Schnappschuss der Bildschirmausgabe" );
    mnuFile.add( mnuScreen );
    mnuScreen.add( createJMenuItem(
			"im Bildbetrachter anzeigen...",
			"file.screen.image.show" ) );
    mnuScreen.add( createJMenuItem(
			"als Bild kopieren",
			"file.screen.image.copy" ) );
    mnuScreen.add( createJMenuItem(
			"als Bilddatei speichern...",
			"file.screen.image.save" ) );
    mnuScreen.addSeparator();

    this.mnuScreenTextShow = createJMenuItem(
				"im Texteditor anzeigen",
				"file.screen.text.show" );
    mnuScreen.add( this.mnuScreenTextShow );

    this.mnuScreenTextCopy = createJMenuItem(
				"als Text kopieren",
				"file.screen.text.copy" );
    mnuScreen.add( this.mnuScreenTextCopy );

    this.mnuScreenTextSave = createJMenuItem(
				"als Textdatei speichern...",
				"file.screen.text.save" );
    mnuScreen.add( this.mnuScreenTextSave );
    mnuFile.addSeparator();

    mnuFile.add( createJMenuItem( "Datei-Browser...", "file.browser" ) );
    mnuFile.add( createJMenuItem(
			"Texteditor/Programmierung...",
			"file.editor" ) );
    mnuFile.addSeparator();
    mnuFile.add( createJMenuItem( "Beenden", "file.quit" ) );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuEditCopy = createJMenuItem(
			"Kopieren",
			"edit.copy",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_C,
					InputEvent.ALT_MASK ) );
    this.mnuEditCopy.setEnabled( false );
    mnuEdit.add( this.mnuEditCopy );

    this.mnuEditPaste = createJMenuItem(
			"Einf\u00FCgen",
			"edit.paste",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_V,
					InputEvent.ALT_MASK ) );
    this.mnuEditPaste.setEnabled( false );
    mnuEdit.add( this.mnuEditPaste );
    mnuEdit.addSeparator();

    this.mnuEditPasteStop = createJMenuItem(
				"Einf\u00FCgen anhalten",
				"edit.paste.stop" );
    this.mnuEditPasteStop.setEnabled( false );
    mnuEdit.add( this.mnuEditPasteStop );

    this.mnuEditPasteContinue = createJMenuItem(
				"Einf\u00FCgen fortsetzen",
				"edit.paste.continue" );
    this.mnuEditPasteContinue.setEnabled( false );
    mnuEdit.add( this.mnuEditPasteContinue );


    // Menu Extra
    JMenu mnuExtra = new JMenu( "Extra" );
    mnuExtra.setMnemonic( KeyEvent.VK_E );

    JMenu mnuExtraScale = new JMenu( "Ansicht" );

    ButtonGroup grpScale = new ButtonGroup();

    this.btnScreenScale1 = createJRadioButtonMenuItem(
			grpScale,
			"100 %",
			"extra.scale.1",
			true,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_1,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale1 );

    this.btnScreenScale2 = createJRadioButtonMenuItem(
			grpScale,
			"200 %",
			"extra.scale.2",
			false,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_2,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale2 );

    this.btnScreenScale3 = createJRadioButtonMenuItem(
			grpScale,
			"300 %",
			"extra.scale.3",
			false,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_3,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale3 );

    this.btnScreenScale4 = createJRadioButtonMenuItem(
			grpScale,
			"400 %",
			"extra.scale.4",
			false,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_4,
					InputEvent.ALT_MASK ) );
    mnuExtraScale.add( this.btnScreenScale4 );

    mnuExtra.add( mnuExtraScale );
    mnuExtra.addSeparator();

    mnuExtra.add( createJMenuItem( "Audio/Kassette...", "extra.audio" ) );
    mnuExtra.add( createJMenuItem( "Druckauftr\u00E4ge...", "extra.print" ) );
    mnuExtra.add( createJMenuItem( "Schachbrett...", "extra.chessboard" ) );

    JMenu mnuExtraTools = new JMenu( "Werkzeuge" );
    mnuExtraTools.add( createJMenuItem( "Debugger...", "extra.debugger" ) );
    mnuExtraTools.add( createJMenuItem(
				"Reassembler...", "extra.reassembler" ) );
    mnuExtraTools.add( createJMenuItem(
				"Speichereditor...", "extra.memeditor" ) );
    mnuExtraTools.add( createJMenuItem( "Rechner...", "extra.calculator" ) );
    mnuExtraTools.add( createJMenuItem(
				"Hex-Dateivergleicher...",
				"extra.hex.diff" ) );
    mnuExtraTools.add( createJMenuItem(
				"Hex-Editor...",
				"extra.hex.editor" ) );
    mnuExtraTools.add( createJMenuItem(
				"Bildbetrachter...", "extra.imageviewer" ) );
    mnuExtraTools.addSeparator();
    mnuExtraTools.add( createJMenuItem(
			"CP/M-Diskettenabbilddatei manuell erstellen...",
			"extra.diskimage.create_manually" ) );
    mnuExtraTools.add( createJMenuItem(
			"CP/M-Diskettenabbilddatei entpacken...",
			"extra.diskimage.unpack" ) );
    mnuExtraTools.add( createJMenuItem(
			"CP/M-Diskette entpacken...",
			"extra.disk.unpack" ) );
    mnuExtraTools.addSeparator();
    mnuExtraTools.add( createJMenuItem(
				"Abbilddatei von Diskette erstellen...",
				"extra.diskimage.create_from_disk" ) );
    if( File.separatorChar == '/' ) {
      mnuExtraTools.add( createJMenuItem(
				"Abbilddatei auf Diskette schreiben...",
				"extra.diskimage.write_to_disk" ) );
    }
    mnuExtra.add( mnuExtraTools );
    mnuExtra.addSeparator();

    mnuExtra.add( createJMenuItem( "Einstellungen...", "extra.settings" ) );
    mnuExtra.add( createJMenuItem( "Profil anwenden...", "extra.profile" ) );
    mnuExtra.addSeparator();

    this.mnuExtraPause = createJMenuItem(
			"Pause",
			"extra.pause",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_P,
				InputEvent.ALT_MASK ) );
    mnuExtra.add( this.mnuExtraPause );

    mnuExtra.add( createJMenuItem(
			"NMI ausl\u00F6sen",
			"extra.nmi",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_N,
				InputEvent.ALT_MASK ) ) );
    mnuExtra.add( createJMenuItem(
			"Zur\u00FCcksetzen (RESET)",
			"extra.reset",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_R,
				InputEvent.ALT_MASK ) ) );
    mnuExtra.add( createJMenuItem(
			"Einschalten (Power On)",
			"extra.power_on",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_I,
				InputEvent.ALT_MASK ) ) );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuHelp.add( createJMenuItem( "Hilfe...", "help.content" ) );

    this.mnuHelpSys = createJMenuItem(
				"Hilfe zum emulierten System...",
				"help.system" );
    this.mnuHelpSys.setEnabled( false );
    mnuHelp.add( this.mnuHelpSys );
    mnuHelp.addSeparator();
    mnuHelp.add( createJMenuItem( "\u00DCber JKCEMU...", "help.about" ) );
    mnuHelp.add( createJMenuItem( "Lizenzbestimmungen...", "help.license" ) );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuExtra );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupCopy = createJMenuItem( "Kopieren", "edit.copy" );
    this.mnuPopupCopy.setEnabled( false );
    this.mnuPopup.add( this.mnuPopupCopy );

    this.mnuPopupPaste = createJMenuItem( "Einf\u00FCgen", "edit.paste" );
    this.mnuPopupPaste.setEnabled( false );
    this.mnuPopup.add( this.mnuPopupPaste );
    this.mnuPopup.addSeparator();

    this.mnuPopup.add( createJMenuItem( "Laden...", "file.load" ) );
    this.mnuPopup.add( createJMenuItem( "Speichern...", "file.save" ) );
    this.mnuPopup.addSeparator();

    this.mnuPopup.add( createJMenuItem(
				"Zur\u00FCcksetzen (RESET)",
				"extra.reset" ) );
    this.mnuPopup.add( createJMenuItem(
				"Einschalten (Power On)",
				"extra.power_on" ) );


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
				"file.load" );

    this.btnSave = createImageButton(
				"/images/file/save.png",
				"Speichern",
				"file.save" );

    this.btnCopy = createImageButton(
				"/images/edit/copy.png",
				"Kopieren",
				"edit.copy" );
    this.btnCopy.setEnabled( false );

    this.btnPaste = createImageButton(
				"/images/edit/paste.png",
				"Einf\u00FCgen",
				"edit.paste" );
    this.btnPaste.setEnabled( false );

    this.btnChessboard = createImageButton(
				"/images/chess/chessboard.png",
				"Schachbrett",
				"extra.chessboard" );

    this.btnAudio = createImageButton(
				"/images/file/audio.png",
				"Audio",
				"extra.audio" );

    this.btnFloppyDiskStation = createImageButton(
				"/images/disk/floppydiskstation.png",
				"Diskettenstation",
				"file.floppydisk.station" );

    this.btnFileBrowser = createImageButton(
				"/images/file/browse.png",
				"Datei-Browser",
				"file.browser" );

    this.btnReset = createImageButton(
				"/images/file/reset.png",
				"Zur\u00FCcksetzen (RESET)",
				"extra.reset" );


    // Bildschirmausgabe
    this.screenFld = new ScreenFld( this );
    this.screenFld.setFocusable( true );
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
    updActionComponents();
    updPasteBtnsEnabled();
    this.statusRefreshTimer = new javax.swing.Timer( 1000, this );
    this.statusRefreshTimer.start();
  }


  public void addToHexDiff( Collection<File> files )
  {
    doExtraHexDiff().addFiles( files );
  }


  /*
   * Die Methode wird aufgerufen,
   * wenn sich ein untergeordnetes Fenster schliesst.
   */
  public void childFrameClosed( Frame frm )
  {
    if( frm != null ) {
      if( this.subFrms.containsValue( frm ) )
	this.subFrms.remove( frm.getClass() );
    }
  }


  public void clearScreenSelection()
  {
    if( this.screenFld != null )
      this.screenFld.clearSelection();
  }


  public BufferedImage createSnapshot()
  {
    return this.screenFld.createBufferedImage();
  }


  public void fireDirectoryChanged( File dirFile )
  {
    Frame frm = this.subFrms.get( FileBrowserFrm.class );
    if( frm != null ) {
      if( frm instanceof FileBrowserFrm ) {
	((FileBrowserFrm) frm).fireDirectoryChanged( dirFile );
      }
    }
  }


  public void firePasteFinished( final String remainText )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    pasteFinished( remainText );
		  }
		} );
  }


  public void fireScreenSizeChanged()
  {
    final Window    window    = this;
    final ScreenFld screenFld = this.screenFld;
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    screenFld.updPreferredSize();
		    window.pack();
		  }
		} );
  }


  public void fireShowErrorDlg( final String msg )
  {
    final Component owner = this;
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    BasicDlg.showErrorDlg(
			owner,
			msg != null ? msg : "Unbekannter Fehler" );
		  }
		} );
  }


  public EmuThread getEmuThread()
  {
    return this.emuThread;
  }


  public FloppyDiskStationFrm getFloppyDiskStationFrm()
  {
    if( this.floppyDiskStationFrm == null ) {
      this.floppyDiskStationFrm = new FloppyDiskStationFrm( this );
    }
    return this.floppyDiskStationFrm;
  }


  public int getScreenRefreshMillis()
  {
    return this.screenRefreshMillis;
  }


  public void openHexEditor( byte[] data )
  {
    doExtraHexEditor().newFile( data );
  }


  public void openHexEditor( File file )
  {
    doExtraHexEditor().openFile( file );
  }


  public void openFileChecksumFrm( Collection<File> files )
  {
    FileChecksumFrm f = (FileChecksumFrm) reopenSubFrm(
						FileChecksumFrm.class );
    if( f != null ) {
      f.setFiles( files );
    } else {
      f = new FileChecksumFrm( this );
      f.setFiles( files );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  public void openProject( File file, Properties props )
  {
    doFileEditor().openProject( file, props );
  }


  public void openTAPViaAudio(
			File   file,
			byte[] fileBytes,
			int    offs )
  {
    doExtraAudio().openTAP( file, fileBytes, offs );
  }


  public void openText( String text )
  {
    doFileEditor().openText( text );
  }


  public void openTextFile( File file )
  {
    doFileEditor().openFile( file );
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
    rebuildToolbar( getEmuSys() );
  }


  public void setScreenDirty( boolean state )
  {
    this.screenDirty = state;
  }


  public void setScreenTextSelected( boolean state )
  {
    this.mnuEditCopy.setEnabled( state );
    this.mnuPopupCopy.setEnabled( state );
    this.btnCopy.setEnabled( state );
  }


  public void showHelp( final String page )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    showHelpInternal( page );
		  }
		} );
  }


  public void showImageFile( File file )
  {
    doExtraImageViewer().showImageFile( file );
  }


  public void showImage( BufferedImage image, String title )
  {
    doExtraImageViewer().showImage( image, title );
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
    this.emuThread.start();
  }


  public void stopPastingText()
  {
    PasteTextMngr pasteTextMngr = this.pasteTextMngr;
    if( pasteTextMngr != null )
      pasteTextMngr.fireStop();
  }


	/* --- DropTargetListener --- */

  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


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


  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FlavorListener --- */

  public void flavorsChanged( FlavorEvent e )
  {
    updPasteBtnsEnabled();
  }


	/* --- Z80StatusListener --- */

  public void z80StatusChanged()
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    updActionComponents();
		  }
		} );
  }


	/* --- ueberschriebene Methoden fuer KeyListener --- */

  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean   visible             = isVisible();
    boolean   canExtractText      = false;
    boolean   supportsChessboard  = false;
    boolean   supportsFloppyDisks = false;
    Z80CPU    secondCPU           = null;
    Z80Memory secondMem           = null;
    int       oldMargin           = this.screenFld.getMargin();
    int       oldScreenScale      = this.screenFld.getScreenScale();
    EmuSys    oldEmuSys           = this.screenFld.getEmuSys();
    EmuSys    emuSys              = emuThread.getNextEmuSys();
    if( emuSys == null ) {
      emuSys = emuThread.getEmuSys();
    }
    if( emuSys != null ) {
      setTitle( "JKCEMU: " + emuSys.getTitle() );
      this.mnuHelpSys.setEnabled( emuSys.getHelpPage() != null );
      canExtractText      = emuSys.canExtractScreenText();
      supportsChessboard  = emuSys.supportsChessboard();
      supportsFloppyDisks = (emuSys.getSupportedFloppyDiskDriveCount() > 0);
      secondCPU           = emuSys.getSecondaryZ80CPU();
      secondMem           = emuSys.getSecondaryZ80Memory();
    }
    rebuildToolbar( emuSys );

    // Bildschirmaktualisierung
    if( this.screenRefreshTimer.isRunning() ) {
      this.screenRefreshTimer.stop();
    }
    if( emuSys != null ) {
      this.screenRefreshMillis = EmuUtil.parseIntProperty(
				props,
				"jkcemu.screen.refresh.ms",
				0,
				DEFAULT_SCREEN_REFRESH_MILLIS );
      if( this.screenRefreshMillis < 10 ) {
	this.screenRefreshMillis = DEFAULT_SCREEN_REFRESH_MILLIS;
      }
      this.screenRefreshTimer.setDelay( this.screenRefreshMillis );
      this.screenRefreshTimer.start();
    }

    // Skalierung nur anpassen, wenn das Fenster noch nicht angezeigt wird
    int screenScale = oldScreenScale;
    if( !visible ) {
      screenScale = EmuUtil.parseIntProperty(
					props,
					"jkcemu.screen.scale",
					1,
					1 );
      this.screenFld.setScreenScale( screenScale );
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

    // Bildschirmausgabe
    this.mnuScreenTextShow.setEnabled( canExtractText );
    this.mnuScreenTextCopy.setEnabled( canExtractText );
    this.mnuScreenTextSave.setEnabled( canExtractText );
    this.screenFld.setEmuSys( emuSys );
    setScreenDirty( true );

    // Fenstergroesse
    boolean rv     = false;
    boolean done   = false;
    int     margin = EmuUtil.parseIntProperty(
					props,
					"jkcemu.screen.margin",
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
    if( !done && !rv ) {
      pack();
      if( !visible ) {
	setScreenCentered();
      }
      rv = true;
    }

    // ggf. nicht relevante Fenster schliessen
    if( !supportsChessboard && (this.chessboardFrm != null) ) {
      if( this.chessboardFrm.isVisible() ) {
	this.chessboardFrm.setVisible( false );
      }
    }
    if( !supportsFloppyDisks && (this.floppyDiskStationFrm != null) ) {
      if( this.floppyDiskStationFrm.isVisible() ) {
	this.floppyDiskStationFrm.setVisible( false );
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
    return rv;
  }


  /*
   * Die Taste F10 dient der Menuesteuerung des Emulators
   * (Oeffnen des Menues, von Java so vorgegeben)
   * und wird deshalb nicht an das emulierte System weitergegeben.
   */
  public void keyPressed( KeyEvent e )
  {
    if( !e.isAltDown() ) {
      int keyCode = e.getKeyCode();
      if( keyCode != KeyEvent.VK_F10 ) {
	if( this.emuThread.keyPressed( e ) ) {
	  this.ignoreKeyChar = true;
	  e.consume();
	}
      }
    }
  }


  public void keyReleased( KeyEvent e )
  {
    if( !e.isAltDown() ) {
      int keyCode = e.getKeyCode();
      if( keyCode != KeyEvent.VK_F10 ) {
	this.emuThread.keyReleased();
	e.consume();
      }
      this.ignoreKeyChar = false;
    }
  }


  public void keyTyped( KeyEvent e )
  {
    if( !e.isAltDown() ) {
      if( this.ignoreKeyChar ) {
	this.ignoreKeyChar = false;
      } else {
	this.emuThread.keyTyped( e.getKeyChar() );
      }
    }
  }


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
	if( Main.getBooleanProperty( "jkcemu.copy_and_paste.direct", true ) ) {
	  String text = checkConvertFromISO646DE(
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


  public void windowDeactivated( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.emuThread.keyReleased();
  }


	/* --- ueberschriebene Methoden fuer WindowListener --- */

  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.screenFld != null) )
      this.screenFld.requestFocus();
  }


	/* --- ueberschriebene Methoden --- */

  public void actionPerformed( ActionEvent e )
  {
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.screenRefreshTimer ) {
	  if( this.chessboardDirty && (this.chessboardFrm != null) ) {
	    this.chessboardFrm.repaintChessboard();
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


  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      if( e instanceof ActionEvent ) {
	String actionCmd = ((ActionEvent) e).getActionCommand();
	if( actionCmd != null ) {
	  if( actionCmd.equals( "file.quit" ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( actionCmd.equals( "file.load" ) ) {
	    rv = true;
	    doFileLoad( true );
	  }
	  else if( actionCmd.equals( "file.save" ) ) {
	    rv = true;
	    doFileSave();
	  }
	  else if( actionCmd.equals( "file.ramfloppy_1.load" ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      doFileRAMFloppyLoad(
			this.emuThread.getRAMFloppy1(),
			'1',
			emuSys.getSupportedRAMFloppyCount() >= 1 );
	    }
	  }
	  else if( actionCmd.equals( "file.ramfloppy_1.save" ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      doFileRAMFloppySave(
			this.emuThread.getRAMFloppy1(),
			'1',
			emuSys.getSupportedRAMFloppyCount() >= 1 );
	    }
	  }
	  else if( actionCmd.equals( "file.ramfloppy_2.load" ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      doFileRAMFloppyLoad(
			this.emuThread.getRAMFloppy2(),
			'2',
			emuSys.getSupportedRAMFloppyCount() >= 2 );
	    }
	  }
	  else if( actionCmd.equals( "file.ramfloppy_2.save" ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      doFileRAMFloppySave(
			this.emuThread.getRAMFloppy2(),
			'2',
			emuSys.getSupportedRAMFloppyCount() >= 2 );
	    }
	  }
	  else if( actionCmd.equals( "file.floppydisk.station" ) ) {
	    rv = true;
	    doFloppyDiskStation();
          }
	  else if( actionCmd.equals( "file.basic.open" ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      emuSys.openBasicProgram();
	    }
	  }
	  else if( actionCmd.equals( "file.basic.save" ) ) {
	    rv            = true;
	    EmuSys emuSys = getEmuSys();
	    if( emuSys != null ) {
	      emuSys.saveBasicProgram();
	    }
	  }
	  else if( actionCmd.equals( "file.screen.image.show" ) ) {
	    rv = true;
	    doFileScreenImageShow();
	  }
	  else if( actionCmd.equals( "file.screen.image.copy" ) ) {
	    rv = true;
	    doFileScreenImageCopy();
	  }
	  else if( actionCmd.equals( "file.screen.image.save" ) ) {
	    rv = true;
	    doFileScreenImageSave();
	  }
	  else if( actionCmd.equals( "file.screen.text.show" ) ) {
	    rv = true;
	    doFileScreenTextShow();
	  }
	  else if( actionCmd.equals( "file.screen.text.save" ) ) {
	    rv = true;
	    doFileScreenTextSave();
	  }
	  else if( actionCmd.equals( "file.screen.text.copy" ) ) {
	    rv = true;
	    doFileScreenTextCopy();
	  }
	  else if( actionCmd.equals( "file.browser" ) ) {
	    rv = true;
	    doFileBrowser();
	  }
	  else if( actionCmd.equals( "file.editor" ) ) {
	    rv = true;
	    doFileEditor();
	  }
	  else if( actionCmd.equals( "edit.copy" ) ) {
	    rv = true;
	    doEditCopy();
	  }
	  else if( actionCmd.equals( "edit.paste" ) ) {
	    rv = true;
	    doEditPaste();
	  }
	  else if( actionCmd.equals( "edit.paste.stop" ) ) {
	    rv = true;
	    stopPastingText();
	  }
	  else if( actionCmd.equals( "edit.paste.continue" ) ) {
	    rv = true;
	    pasteText( this.pasteRemainText );
	  }
	  else if( actionCmd.equals( "extra.scale.1" ) ) {
	    rv = true;
	    doScreenScale( 1 );
	  }
	  else if( actionCmd.equals( "extra.scale.2" ) ) {
	    rv = true;
	    doScreenScale( 2 );
	  }
	  else if( actionCmd.equals( "extra.scale.3" ) ) {
	    rv = true;
	    doScreenScale( 3 );
	  }
	  else if( actionCmd.equals( "extra.scale.4" ) ) {
	    rv = true;
	    doScreenScale( 4 );
	  }
	  else if( actionCmd.equals( "extra.audio" ) ) {
	    rv = true;
	    doExtraAudio();
	  }
	  else if( actionCmd.equals( "extra.print" ) ) {
	    rv = true;
	    doExtraPrintList();
	  }
	  else if( actionCmd.equals( "extra.chessboard" ) ) {
	    rv = true;
	    doExtraChessboard();
	  }
	  else if( actionCmd.equals( "extra.imageviewer" ) ) {
	    rv = true;
	    doExtraImageViewer();
	  }
	  else if( actionCmd.equals( "extra.debugger" ) ) {
	    rv = true;
	    doExtraDebugger();
	  }
	  else if( actionCmd.equals( "extra.memeditor" ) ) {
	    rv = true;
	    doExtraMemEditor();
	  }
	  else if( actionCmd.equals( "extra.reassembler" ) ) {
	    rv = true;
	    doExtraReassembler();
	  }
	  else if( actionCmd.equals( "extra.calculator" ) ) {
	    rv = true;
	    doExtraCalculator();
	  }
	  else if( actionCmd.equals( "extra.hex.diff" ) ) {
	    rv = true;
	    doExtraHexDiff();
	  }
	  else if( actionCmd.equals( "extra.hex.editor" ) ) {
	    rv = true;
	    doExtraHexEditor();
	  }
	  else if( actionCmd.equals( "extra.diskimage.create_manually" ) ) {
	    rv = true;
	    doExtraDiskImgCreate();
	  }
	  else if( actionCmd.equals( "extra.diskimage.unpack" ) ) {
	    rv = true;
	    doExtraDiskImgUnpack();
	  }
	  else if( actionCmd.equals( "extra.disk.unpack" ) ) {
	    rv = true;
	    doExtraDiskUnpack();
	  }
	  else if( actionCmd.equals( "extra.diskimage.create_from_disk" ) ) {
	    rv = true;
	    DiskImgProcessDlg.createDiskImageFromDrive( this );
	  }
	  else if( actionCmd.equals( "extra.diskimage.write_to_disk" ) ) {
	    rv = true;
	    DiskImgProcessDlg.writeDiskImageToDrive( this );
	  }
	  else if( actionCmd.equals( "extra.settings" ) ) {
	    rv = true;
	    doExtraSettings();
	  }
	  else if( actionCmd.equals( "extra.profile" ) ) {
	    rv = true;
	    doExtraProfile();
	  }
	  else if( actionCmd.equals( "extra.pause" ) ) {
	    rv = true;
	    doExtraPause();
	  }
	  else if( actionCmd.equals( "extra.nmi" ) ) {
	    rv = true;
	    doExtraNMI();
	  }
	  else if( actionCmd.equals( "extra.reset" ) ) {
	    rv = true;
	    doExtraReset();
	  }
	  else if( actionCmd.equals( "extra.power_on" ) ) {
	    rv = true;
	    doExtraPowerOn();
	  }
	  else if( actionCmd.equals( "help.content" ) ) {
	    rv = true;
	    showHelp( "/help/home.htm" );
	  }
	  else if( actionCmd.equals( "help.system" ) ) {
	    rv = true;
	    doHelpSystem();
	  }
	  else if( actionCmd.equals( "help.about" ) ) {
	    rv = true;
	    (new AboutDlg( this )).setVisible( true );
	  }
	  else if( actionCmd.equals( "help.license" ) ) {
	    rv = true;
	    showHelp( "/help/license.htm" );
	  }
	}
      }
    }
    return rv;
  }


  public boolean doClose()
  {
    if( EmuUtil.parseBoolean(
		Main.getProperty( "jkcemu.confirm.quit" ),
		true ) )
    {
      if( !BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie den Emulator jetzt beenden?",
		"Best\u00E4tigung" ) )
      {
	return false;
      }
    }

    // Pruefen, ob RAM-Floppies gespeichert wurden
    if( this.emuThread != null ) {
      String msg = null;
      if( this.emuThread.getRAMFloppy1().hasDataChanged()
	  && this.emuThread.getRAMFloppy2().hasDataChanged() )
      {
	msg = "Die Daten in beiden RAM-Floppies";
      } else {
	if( this.emuThread.getRAMFloppy1().hasDataChanged() ) {
	  msg = "Die Daten in RAM-Floppy 1";
	}
	else if( this.emuThread.getRAMFloppy2().hasDataChanged() ) {
	  msg = "Die Daten in RAM-Floppy 2";
	}
      }
      if( msg != null ) {
	if( JOptionPane.showConfirmDialog(
		this,
		msg + " wurden ge\u00E4ndert und nicht gespeichert.\n"
			+ "M\u00F6chten Sie trotzdem beenden?",
		"Daten ge\u00E4ndert",
		JOptionPane.YES_NO_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.YES_OPTION )
	{
	  return false;
	}
      }
    }

    // Programmbeendigung nicht durch Exception verhindern lassen
    try {

      // untergeordnete Fenster schliessen
      if( this.floppyDiskStationFrm != null ) {
	if( !this.floppyDiskStationFrm.doClose() ) {
	  return false;
	}
      }
      if( this.chessboardFrm != null ) {
	if( !this.chessboardFrm.doClose() ) {
	  return false;
	}
      }
      if( this.secondDebugFrm != null ) {
	if( !this.secondDebugFrm.doClose() ) {
	  return false;
	}
      }
      if( this.secondMemEditFrm != null ) {
	if( !this.secondMemEditFrm.doClose() ) {
	  return false;
	}
      }
      if( this.secondReassFrm != null ) {
	if( !this.secondReassFrm.doClose() ) {
	  return false;
	}
      }
      Collection<BasicFrm> c = this.subFrms.values();
      if( c != null ) {
	int n = c.size();
	if( n > 0 ) {
	  BasicFrm[] frms = c.toArray( new BasicFrm[ n ] );
	  if( frms != null ) {
	    for( int i = 0; i < frms.length; i++ ) {
	      BasicFrm frm = frms[ i ];
	      if( frm instanceof AudioFrm ) {
		((AudioFrm) frm).doQuit();
	      }
	      else if( !frm.doClose() ) {
		return false;
	      }
	    }
	  }
	}
      }

      // Emulator-Thread beenden
      if( this.emuThread != null ) {
	this.emuThread.stopEmulator();

	// max. eine halbe Sekunde auf Thread-Beendigung warten
	try {
	  this.emuThread.join( 500 );
	}
	catch( InterruptedException ex ) {}
      }
      super.doClose();
    }
    catch( Exception ex ) {}
    System.exit( 0 );
    return true;
  }


  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      props.setProperty(
		"jkcemu.screen.scale",
		String.valueOf( this.screenFld.getScreenScale() ) );
    }
  }


  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


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
    File    file            = null;
    boolean startSelected   = false;
    boolean loadWithOptions = true;
    if( EmuUtil.isNativeFileDialogSelected() ) {
      file = EmuUtil.showNativeFileDlg(
					this,
					false,
					"Datei laden",
					Main.getLastPathFile( "software" ) );
    } else {
      FileSelectDlg dlg = new FileSelectDlg(
			this,
			false,	// forSave
			startEnabled,
			true,	// loadWithOptionsEnabled
			"Datei laden",
			Main.getLastPathFile( "software" ),
			EmuUtil.getKCSystemFileFilter(),
			EmuUtil.getKCBasicFileFilter(),
			EmuUtil.getTapFileFilter(),
			EmuUtil.getHeadersaveFileFilter(),
			EmuUtil.getHexFileFilter(),
			EmuUtil.getBinaryFileFilter() );
      dlg.setVisible( true );
      file            = dlg.getSelectedFile();
      loadWithOptions = dlg.isLoadWithOptionsSelected();
      startSelected   = dlg.isStartSelected();
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
    SaveDlg dlg = new SaveDlg(
			this,
			-1,		// Anfangsadresse
			-1,		// Endadresse
			-1,		// Dateityp
			false,		// KC-BASIC
			null );
    dlg.setVisible( true );
  }


  private void doFileRAMFloppyLoad(
			RAMFloppy ramFloppy,
			char      floppyCh,
			boolean   supported )
  {
    if( confirmRAMFloppyOperation( floppyCh, supported ) ) {
      boolean status = true;
      if( ramFloppy.hasDataChanged() ) {
	if( JOptionPane.showConfirmDialog(
		this,
		"Die Daten in der RAM-Floppy wurden ge\u00E4ndert"
			+ " und nicht gespeichert.\n"
			+ "M\u00F6chten Sie trotzdem laden?",
		"Daten ge\u00E4ndert",
		JOptionPane.YES_NO_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.YES_OPTION )
	{
	  status = false;
	}
      }
      if( status ) {
	File file = EmuUtil.showFileOpenDlg(
			this,
			String.format( "RAM-Floppy %c laden", floppyCh ),
			Main.getLastPathFile( "ramfloppy" ),
			EmuUtil.getBinaryFileFilter() );
	if( file != null ) {
	  try {
	    ramFloppy.load( file );
	    Main.setLastFile( file, "ramfloppy" );
	    showStatusText( "RAM-Floppy geladen" );
	  }
	  catch( IOException ex ) {
	    BasicDlg.showErrorDlg(
		this,
		"Die RAM-Floppy kann nicht geladen werden.\n\n"
						+ ex.getMessage() );
	  }
	}
      }
    }
  }


  private void doFileRAMFloppySave(
			RAMFloppy ramFloppy,
			char      floppyCh,
			boolean   supported )
  {
    if( confirmRAMFloppyOperation( floppyCh, supported ) ) {
      File file = EmuUtil.showFileSaveDlg(
			this,
			String.format( "RAM-Floppy %c speichern", floppyCh ),
			ramFloppy.getFile() != null ?
				ramFloppy.getFile()
				: Main.getLastPathFile( "ramfloppy" ) );
      if( file != null ) {
	try {
	  ramFloppy.save( file );
	  Main.setLastFile( file, "ramfloppy" );
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg(
		this,
		"RAM-Floppy kann nicht gespeichert werden.\n\n"
						+ ex.getMessage() );
	}
      }
    }
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
    if( ImgUtil.saveImage(
			this,
			this.screenFld.createBufferedImage() ) != null )
    {
      showStatusText( "Bilddatei gespeichert" );
    }
  }


  private void doFileScreenImageShow()
  {
    showImage( this.screenFld.createBufferedImage(), "Schnappschuss" );
  }


  private void doFileScreenTextCopy()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      String screenText = checkConvertFromISO646DE( emuSys.getScreenText() );
      if( screenText != null ) {
	try {
	  Toolkit tk = getToolkit();
	  if( tk != null ) {
	    Clipboard clp = tk.getSystemClipboard();
	    if( clp != null ) {
	      StringSelection ss = new StringSelection( screenText );
	      clp.setContents( ss, ss );
	    }
	  }
	}
	catch( IllegalStateException ex ) {}
      }
    }
  }


  private void doFileScreenTextSave()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      String screenText = checkConvertFromISO646DE( emuSys.getScreenText() );
      if( screenText != null ) {
	File file = EmuUtil.showFileSaveDlg(
				this,
				"Textdatei speichern",
				Main.getLastPathFile( "screen" ),
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
	      BasicDlg.showErrorDlg(
		this,
		fileName + ":\nSpeichern der Datei fehlgeschlagen\n\n"
			+ ex.getMessage() );
	    }
	    finally {
	      EmuUtil.doClose( out );
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
      String screenText = checkConvertFromISO646DE( emuSys.getScreenText() );
      if( screenText != null ) {
	doFileEditor().openText( screenText );
      }
    }
  }


  private void doFileBrowser()
  {
    if( reopenSubFrm( FileBrowserFrm.class ) == null ) {
      FileBrowserFrm f = new FileBrowserFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  private EditFrm doFileEditor()
  {
    EditFrm f = (EditFrm) reopenSubFrm( EditFrm.class );
    if( f == null ) {
      f = new EditFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
    return f;
  }


  private void doFloppyDiskStation()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      int n = emuSys.getSupportedFloppyDiskDriveCount();
      if( n > 0 ) {
	FloppyDiskStationFrm frm = getFloppyDiskStationFrm();
	frm.setDriveCount( n );
	if( !this.floppyDiskStationFrm.isVisible() ) {
	  this.floppyDiskStationFrm.setVisible( true );
	}
	if( this.floppyDiskStationFrm.getState() != Frame.NORMAL ) {
	  this.floppyDiskStationFrm.setState( Frame.NORMAL );
	}
	this.floppyDiskStationFrm.toFront();
      } else {
	BasicDlg.showInfoDlg(
		this,
		"Das gerade emulierte System unterst\u00FCtzt"
			+ " keine Floppy Disks." );
      }
    }
  }


	/* --- Aktionen im Menu Bearbeiten --- */

  private void doEditCopy()
  {
    if( this.clipboard != null ) {
      String text = checkConvertFromISO646DE(
				this.screenFld.getSelectedText() );
      if( text != null ) {
	if( text.length() > 0 ) {
	  try {
	    StringSelection ss = new StringSelection( text );
	    this.clipboard.setContents( ss, ss );
	  }
	  catch( Exception ex ) {}
	}
      }
    }
  }


  private void doEditPaste()
  {
    if( this.clipboard != null ) {
      try {
	if( this.clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor ) )
	{
	  Object data = this.clipboard.getData( DataFlavor.stringFlavor );
	  if( data != null )
	    pasteText( data.toString() );
	}
      }
      catch( Exception ex ) {}
    }
  }


	/* --- Aktionen im Menu Extra --- */

  private void doScreenScale( int screenScale )
  {
    this.screenFld.setScreenScale( screenScale );
    pack();
  }


  private AudioFrm doExtraAudio()
  {
    AudioFrm f = (AudioFrm) reopenSubFrm( AudioFrm.class );
    if( f == null ) {
      f = new AudioFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
    return f;
  }


  private void doExtraChessboard()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsChessboard() ) {
	if( this.chessboardFrm != null ) {
	  if( !this.chessboardFrm.isVisible() ) {
	    this.chessboardFrm.setVisible( true );
	  }
	  if( this.chessboardFrm.getState() != Frame.NORMAL ) {
	    this.chessboardFrm.setState( Frame.NORMAL );
	  }
	  this.chessboardFrm.toFront();
	} else {
	  this.chessboardFrm = new ChessboardFrm( this );
	  this.chessboardFrm.setVisible( true );
	}
      } else {
        BasicDlg.showInfoDlg(
		this,
		"Das Schachbrett kann nur angezeigt werden,\n"
			+ "wenn ein Schachcomputer oder Lerncomputer\n"
			+ "mit integriertem Schachprogramm emuliert wird.\n"
			+ "Das trifft jedoch f\u00FCr das gerade"
			+ " emulierte System nicht zu." );
      }
    }
  }


  private void doExtraPrintList()
  {
    if( reopenSubFrm( PrintListFrm.class ) == null ) {
      PrintListFrm f = new PrintListFrm(
				this,
				this.emuThread.getPrintMngr() );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  private void doExtraCalculator()
  {
    if( reopenSubFrm( CalculatorFrm.class ) == null ) {
      CalculatorFrm f = new CalculatorFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
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
	secondCPU  = emuSys.getSecondaryZ80CPU();
	secondMem  = emuSys.getSecondaryZ80Memory();
	secondName = emuSys.getSecondarySystemName();
      }
      if( (secondCPU != null) && (secondMem != null) ) {
	sysNum = askAccessToSysNum( secondName );
      }
      if( sysNum == 0 ) {
	DebugFrm frm = (DebugFrm) reopenSubFrm( DebugFrm.class );
	if( frm == null ) {
	  frm = new DebugFrm(
			this,
			this.emuThread.getZ80CPU(),
			this.emuThread );
	  frm.setVisible( true );
	  this.subFrms.put( frm.getClass(), frm );
	}
      }
      else if( sysNum == 1 ) {
	if( this.secondDebugFrm != null ) {
	  this.secondDebugFrm.setVisible( true );
	  this.secondDebugFrm.setState( Frame.NORMAL );
	  this.secondDebugFrm.toFront();
	} else {
	  this.secondDebugFrm = new DebugFrm(
					this,
					secondCPU,
					secondMem );
	  if( secondName != null ) {
	    this.secondDebugFrm.setTitle(
			"JKCEMU Debugger: " + secondName );
	  } else {
	    this.secondDebugFrm.setTitle(
			"JKCEMU Debugger: Sekund\u00E4rsystem" );
	  }
	  this.secondDebugFrm.setVisible( true );
	}
      }
    }
  }


  private void doExtraDiskImgCreate()
  {
    DiskImgCreateFrm f = (DiskImgCreateFrm) reopenSubFrm(
						DiskImgCreateFrm.class );
    if( f == null ) {
      f = new DiskImgCreateFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  private void doExtraDiskImgUnpack()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"CP/M-Diskettenabbilddatei entpacken",
			Main.getLastPathFile( "disk" ),
			EmuUtil.getAnadiskFileFilter(),
			EmuUtil.getTelediskFileFilter(),
			EmuUtil.getPlainDiskFileFilter() );
    if( file != null ) {
      try {
	boolean done     = false;
	String  fileName = file.getName();
	if( fileName != null ) {
	  fileName = fileName.toLowerCase();
	  if( EmuUtil.endsWith( fileName, DiskUtil.anadiskFileExt ) ) {
	    AbstractFloppyDisk disk = AnadiskFloppyDisk.readFile(
								this,
								file );
	    if( disk != null ) {
	      DiskUtil.unpackDisk( this, file, disk, "Anadisk-Datei" );
	    }
	    done = true;
	  }
	  else if( EmuUtil.endsWith( fileName, DiskUtil.telediskFileExt ) ) {
	    AbstractFloppyDisk disk = TelediskFloppyDisk.readFile(
								this,
								file );
	    if( disk != null ) {
	      DiskUtil.unpackDisk( this, file, disk, "Teledisk-Datei" );
	    }
	    done = true;
	  }
	  else if( EmuUtil.endsWith( fileName, DiskUtil.plainDiskFileExt )
		   || EmuUtil.endsWith(
				fileName,
				DiskUtil.gzPlainDiskFileExt ) )
	  {
	    DiskUtil.unpackPlainDiskFile( this, file );
	    done = true;
	  }
	  else if( EmuUtil.endsWith(
			fileName,
			DiskUtil.gzAnadiskFileExt ) )
	  {
	    AbstractFloppyDisk disk = AnadiskFloppyDisk.readFile(
								this,
								file );
	    if( disk != null ) {
	      DiskUtil.unpackDisk(
			this,
			file,
			disk,
			"Komprimierte Anadisk-Datei" );
	    }
	    done = true;
	  }
	  else if( EmuUtil.endsWith( fileName, DiskUtil.gzTelediskFileExt ) ) {
	    AbstractFloppyDisk disk = TelediskFloppyDisk.readFile(
								this,
								file );
	    if( disk != null ) {
	      DiskUtil.unpackDisk(
			this,
			file,
			disk,
			"Komprimierte Teledisk-Datei" );
	    }
	    done = true;
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
	BasicDlg.showErrorDlg( this, ex );
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
	BasicDlg.showErrorDlg( this, ex );
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
	secondMem  = emuSys.getSecondaryZ80Memory();
	secondName = emuSys.getSecondarySystemName();
      }
      if( secondMem != null ) {
	sysNum = askAccessToSysNum( secondName );
      }
      if( sysNum == 0 ) {
	MemEditFrm frm = (MemEditFrm) reopenSubFrm( MemEditFrm.class );
	if( frm == null ) {
	  frm = new MemEditFrm( this, this.emuThread );
	  frm.setVisible( true );
	  this.subFrms.put( frm.getClass(), frm );
	}
      }
      else if( sysNum == 1 ) {
	if( this.secondMemEditFrm != null ) {
	  this.secondMemEditFrm.setVisible( true );
	  this.secondMemEditFrm.setState( Frame.NORMAL );
	  this.secondMemEditFrm.toFront();
	} else {
	  this.secondMemEditFrm = new MemEditFrm( this, secondMem );
	  if( secondName != null ) {
	    this.secondMemEditFrm.setTitle(
			"JKCEMU Speichereditor: " + secondName );
	  } else {
	    this.secondMemEditFrm.setTitle(
			"JKCEMU Speichereditor: Sekund\u00E4rsystem" );
	  }
	  this.secondMemEditFrm.setVisible( true );
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
	secondMem  = emuSys.getSecondaryZ80Memory();
	secondName = emuSys.getSecondarySystemName();
      }
      if( secondMem != null ) {
	sysNum = askAccessToSysNum( secondName );
      }
      if( sysNum == 0 ) {
	ReassFrm frm = (ReassFrm) reopenSubFrm( ReassFrm.class );
	if( frm == null ) {
	  frm = new ReassFrm(
			this,
			this.emuThread.getEmuSys(),
			this.emuThread );
	  frm.setVisible( true );
	  this.subFrms.put( frm.getClass(), frm );
	}
      }
      else if( sysNum == 1 ) {
	if( this.secondReassFrm != null ) {
	  this.secondReassFrm.setVisible( true );
	  this.secondReassFrm.setState( Frame.NORMAL );
	  this.secondReassFrm.toFront();
	} else {
	  this.secondReassFrm = new ReassFrm( this, null, secondMem );
	  if( secondName != null ) {
	    this.secondReassFrm.setTitle(
			"JKCEMU Reassembler: " + secondName );
	  } else {
	    this.secondReassFrm.setTitle(
			"JKCEMU Reassembler: Sekund\u00E4rsystem" );
	  }
	  this.secondReassFrm.setVisible( true );
	}
      }
    }
  }


  private HexDiffFrm doExtraHexDiff()
  {
    HexDiffFrm f = (HexDiffFrm) reopenSubFrm( HexDiffFrm.class );
    if( f == null ) {
      f = new HexDiffFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
    return f;
  }


  private FileEditFrm doExtraHexEditor()
  {
    FileEditFrm f = (FileEditFrm) reopenSubFrm( FileEditFrm.class );
    if( f == null ) {
      f = new FileEditFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
    return f;
  }


  private ImageFrm doExtraImageViewer()
  {
    ImageFrm f = (ImageFrm) reopenSubFrm( ImageFrm.class );
    if( f == null ) {
      f = new ImageFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
    return f;
  }


  private void doExtraSettings()
  {
    if( reopenSubFrm( SettingsFrm.class ) == null ) {
      SettingsFrm f = new SettingsFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
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
	/*
         * Die eingebundenen Dateien (ROM-Images, Zeichensatzdatei)
	 * sollen nur einmal geladen werden,
	 * und nicht doppelt in EmuThread und SettingsFrm.
	 * Aus diesem Grund werden diese hier gesondert behandelt.
	 */
	ExtFile  extFont = EmuUtil.readExtFont( this, props );
	ExtROM[] extROMs = EmuUtil.readExtROMs( this, props );
	this.emuThread.applySettings( props, extFont, extROMs, false );
	Main.applyProfileToFrames( file, props, true, null );
	Frame frm = this.subFrms.get( SettingsFrm.class );
	if( frm != null ) {
	  if( frm instanceof SettingsFrm ) {
	    ((SettingsFrm) frm).setExtFont( extFont );
	    ((SettingsFrm) frm).setExtROMs( extROMs );
	  }
	}
	getFloppyDiskStationFrm().openDisks( props );
	fireReset( EmuThread.ResetLevel.COLD_RESET );
      }
    }
  }


  private void doExtraPause()
  {
    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      if( z80cpu.isActive() )
	z80cpu.firePause( !z80cpu.isPause() );
    }
  }


  private void doExtraNMI()
  {
    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      if( EmuUtil.parseBoolean(
		Main.getProperty( "jkcemu.confirm.nmi" ),
		true ) )
      {
	if( BasicDlg.showYesNoDlg(
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
		Main.getProperty( "jkcemu.confirm.reset" ),
		true ) )
    {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie den Emulator neu starten?",
		"Best\u00E4tigung" ) )
      {
	this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
      }
    } else {
      this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
    }
    /*
     * Die Timer werden neu gestartet fuer den Fall,
     * dass sich ein Timer aufgehaengt haben sollte,
     * und man moechte ihn mit RESET reaktivieren.
     */
    this.screenRefreshTimer.restart();
    this.statusRefreshTimer.restart();
  }


  private void doExtraPowerOn()
  {
    if( EmuUtil.parseBoolean(
		Main.getProperty( "jkcemu.confirm.power_on" ),
		true ) )
    {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie das Aus- und wieder Einschalten"
			+ " emulieren?\n"
			+ "Dabei gehen alle im Arbeitsspeicher befindlichen\n"
			+ "Programme und Daten verloren.",
		"Best\u00E4tigung" ) )
      {
	this.emuThread.fireReset( EmuThread.ResetLevel.POWER_ON );
      }
    } else {
      this.emuThread.fireReset( EmuThread.ResetLevel.POWER_ON );
    }
  }


	/* --- Aktionen im Menu Hilfe --- */

  private void doHelpSystem()
  {
    EmuSys emuSys = getEmuSys();
    if( emuSys != null ) {
      String page = emuSys.getHelpPage();
      if( page != null ) {
	showHelp( page );
      }
    }
  }


	/* --- private Methoden --- */

  private int askAccessToSysNum( String sysName )
  {
    int rv = -1;

    String[] options = {
		"Hauptsystem",
		sysName != null ? sysName : "Sekund\u00E4rystem",
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


  private String checkConvertFromISO646DE( String text )
  {
    EmuSys emuSys = getEmuSys();
    if( (emuSys != null) && (text != null) ) {
      int len = text.length();
      if( len > 0 ) {

	// ggf. deutsche Umlaute konvertieren
	boolean extFont = (this.emuThread.getExtFont() != null);
	if( extFont || emuSys.isISO646DE() ) {
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
	    if( extFont ) {
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
		} else if( value.equals( options[ 2 ] ) ) {
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


  private boolean confirmRAMFloppyOperation(
					char    floppyCh,
					boolean supported )
  {
    if( !supported ) {
      String[]    options = { "Weiter", "Abbrechen" };
      JOptionPane pane    = new JOptionPane(
		String.format(
			"Die RAM-Floppy %c wird von dem gerade emulierten"
				+ " System nicht unterst\u00FCtzt.\n"
				+ "Sie k\u00F6nnen zwar die RAM-Floppy"
				+ " laden und speichern,\n"
				+ "jedoch nicht auf sie zugreifen.",
			floppyCh ),
		JOptionPane.WARNING_MESSAGE );
      pane.setOptions( options );
      pane.setWantsInput( false );
      pane.setInitialSelectionValue( options[ 0 ] );
      pane.createDialog(
		this,
		"RAM-Floppy nicht unterst\u00FCtzt" ).setVisible( true );
      Object value = pane.getValue();
      if( value != null ) {
	supported = value.equals( options[ 0 ] );
      }
    }
    return supported;
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


  private void fireReset( final EmuThread.ResetLevel resetLevel )
  {
    final EmuThread emuThread = this.emuThread;
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    emuThread.fireReset( resetLevel );
		  }
		} );
  }


  private EmuSys getEmuSys()
  {
    return this.emuThread != null ? this.emuThread.getEmuSys() : null;
  }


  private void pasteFinished( String remainText )
  {
    this.pasteTextMngr   = null;
    this.pasteRemainText = remainText;
    this.mnuEditPasteStop.setEnabled( false );
    if( remainText != null ) {
      if( remainText.length() > 0 )
        this.mnuEditPasteContinue.setEnabled( true );
    }
  }


  private void pasteText( String text )
  {
    if( (this.pasteTextMngr == null) && (text != null) ) {
      if( text.length() > 0 ) {
	this.pasteTextMngr = new PasteTextMngr( this, text );
	this.mnuEditPasteStop.setEnabled( true );
	this.mnuEditPasteContinue.setEnabled( false );
	this.pasteTextMngr.start();
      }
    }
  }


  private void rebuildToolbar( EmuSys emuSys )
  {
    boolean supportsCopy        = false;
    boolean supportsPaste       = false;
    boolean supportsChessboard  = false;
    boolean supportsFloppyDisks = false;
    boolean supportsAudio       = false;
    if( emuSys != null ) {
      supportsCopy        = emuSys.supportsCopyToClipboard();
      supportsPaste       = emuSys.supportsPasteFromClipboard();
      supportsChessboard  = emuSys.supportsChessboard();
      supportsAudio       = emuSys.supportsAudio();
      supportsFloppyDisks = (emuSys.getSupportedFloppyDiskDriveCount() > 0);
    }
    this.toolBar.removeAll();
    this.toolBar.add( this.btnLoad );
    this.toolBar.add( this.btnSave );
    if( supportsCopy || supportsPaste ) {
      this.toolBar.addSeparator();
      if( supportsCopy ) {
	this.toolBar.add( this.btnCopy );
      }
      if( supportsPaste ) {
	this.toolBar.add( this.btnPaste );
      }
    }
    this.toolBar.addSeparator();
    if( supportsChessboard ) {
      this.toolBar.add( this.btnChessboard );
    }
    if( supportsAudio ) {
      this.toolBar.add( this.btnAudio );
    }
    if( supportsFloppyDisks ) {
      this.toolBar.add( this.btnFloppyDiskStation );
    }
    this.toolBar.add( this.btnFileBrowser );
    this.toolBar.add( this.btnReset );
    SwingUtilities.updateComponentTreeUI( this.toolBar );
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
	  } else {
	    String mhzText = createMHzText( z80cpu );
	    if( mhzText != null ) {
	      msg = String.format(
			"Emulierte Taktfrequenz: %s MHz",
			mhzText );

	      EmuSys emuSys = this.emuThread.getEmuSys();
	      if( emuSys != null ) {
		String secondName = emuSys.getSecondarySystemName();
		Z80CPU secondCPU  = emuSys.getSecondaryZ80CPU();
		if( (secondName != null) && (secondCPU != null) ) {
		  if( emuSys.isSecondarySystemRunning()
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


  private Frame reopenSubFrm( Class frmClass )
  {
    Frame rv  = null;
    Frame frm = this.subFrms.get( frmClass );
    if( frm != null ) {
      if( frmClass.isInstance( frm ) ) {
	if( !frm.isVisible() ) {
	  frm.setVisible( true );
	}
	if( frm.getState() != Frame.NORMAL ) {
	  frm.setState( Frame.NORMAL );
	}
	frm.toFront();
	rv = frm;
      }
    }
    return rv;
  }


  private void showHelpInternal( String page )
  {
    HelpFrm f = (HelpFrm) reopenSubFrm( HelpFrm.class );
    if( f != null ) {
      f.setPage( page );
    } else {
      f = new HelpFrm( this );
      f.setPage( page );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  private void updActionComponents()
  {
    String pauseText = "Pause";
    if( this.emuThread != null ) {
      Z80CPU z80cpu = this.emuThread.getZ80CPU();
      if( z80cpu != null ) {
	if( z80cpu.isActive() ) {
	  if( z80cpu.isPause() ) {
	    pauseText = "Fortsetzen";
	  }
	  this.mnuExtraPause.setEnabled( true );
	} else {
	  this.mnuExtraPause.setEnabled( false );
	}
      }
    }
    this.mnuExtraPause.setText( pauseText );
  }


  private void updPasteBtnsEnabled()
  {
    boolean state = false;
    if( this.clipboard != null ) {
      try {
	state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor );
      }
      catch( Exception ex ) {}
    }
    this.mnuEditPaste.setEnabled( state );
    this.mnuPopupPaste.setEnabled( state );
    this.btnPaste.setEnabled( state );
  }
}

