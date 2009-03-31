/*
 * (c) 2008-2009 Jens Mueller
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
import jkcemu.filebrowser.*;
import jkcemu.image.*;
import jkcemu.text.EditFrm;
import jkcemu.tools.*;
import jkcemu.tools.calculator.CalculatorFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexeditor.HexEditFrm;
import z80emu.*;


public class ScreenFrm extends BasicFrm implements
						DropTargetListener,
						FlavorListener
{
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
  private JButton              btnCopy;
  private JButton              btnPaste;
  private JLabel               labelStatus;
  private boolean              ignoreKeyChar;
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


  public ScreenFrm()
  {
    setTitle( "JKCEMU" );


    // Initialisierungen
    this.pasteTextMngr       = null;
    this.pasteRemainText     = null;
    this.ignoreKeyChar       = false;
    this.screenDirty         = false;
    this.screenRefreshMillis = 50;
    this.screenRefreshTimer  = new javax.swing.Timer(
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

    this.subFrms   = new Hashtable<Class,BasicFrm>();
    this.emuThread = new EmuThread( this );
    this.speedFmt  = NumberFormat.getNumberInstance();
    if( this.speedFmt instanceof DecimalFormat ) {
      ((DecimalFormat) this.speedFmt).applyPattern( "###,###,##0.0#" );
    }


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    mnuFile.add( createJMenuItem( "Laden...", "file.load" ) );
    mnuFile.add( createJMenuItem( "Speichern...", "file.save" ) );
    mnuFile.addSeparator();

    // Untermenu RAM-Floppy A
    JMenu mnuRAMFloppyA = new JMenu( "RAM-Floppy A" );
    mnuFile.add( mnuRAMFloppyA );
    mnuRAMFloppyA.add( createJMenuItem(
			"Laden...",
			"file.ramfloppy_a.load" ) );
    mnuRAMFloppyA.add( createJMenuItem(
			"Speichern...",
			"file.ramfloppy_a.save" ) );

    // Untermenu RAM-Floppy B
    JMenu mnuRAMFloppyB = new JMenu( "RAM-Floppy B" );
    mnuFile.add( mnuRAMFloppyB );
    mnuRAMFloppyB.add( createJMenuItem(
			"Laden...",
			"file.ramfloppy_b.load" ) );
    mnuRAMFloppyB.add( createJMenuItem(
			"Speichern...",
			"file.ramfloppy_b.save" ) );
    mnuFile.addSeparator();

    // Untermenu BASIC
    JMenu mnuBasic = new JMenu( "BASIC-Programm" );
    mnuFile.add( mnuBasic );
    mnuBasic.add( createJMenuItem(
			"Im Texteditor \u00F6ffnen...",
			"file.basic.open" ) );
    mnuBasic.add( createJMenuItem(
			"Speichern...",
			"file.basic.save" ) );

    // Untermenu Mini-/Tiny-BASIC
    JMenu mnuTinyBasic = new JMenu( "Mini-/Tiny-BASIC-Programm" );
    mnuFile.add( mnuTinyBasic );
    mnuFile.addSeparator();
    mnuTinyBasic.add( createJMenuItem(
			"Im Texteditor \u00F6ffnen...",
			"file.tinybasic.open" ) );
    mnuTinyBasic.add( createJMenuItem(
			"Speichern...",
			"file.tinybasic.save" ) );

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

    this.mnuEditCopy = createJMenuItem( "Kopieren", "edit.copy" );
    this.mnuEditCopy.setEnabled( false );
    mnuEdit.add( this.mnuEditCopy );

    this.mnuEditPaste = createJMenuItem( "Einf\u00FCgen", "edit.paste" );
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
					true );
    mnuExtraScale.add( this.btnScreenScale1 );

    this.btnScreenScale2 = createJRadioButtonMenuItem(
					grpScale,
					"200 %",
					"extra.scale.2",
					false );
    mnuExtraScale.add( this.btnScreenScale2 );

    this.btnScreenScale3 = createJRadioButtonMenuItem(
					grpScale,
					"300 %",
					"extra.scale.3",
					false );
    mnuExtraScale.add( this.btnScreenScale3 );

    this.btnScreenScale4 = createJRadioButtonMenuItem(
					grpScale,
					"400 %",
					"extra.scale.4",
					false );
    mnuExtraScale.add( this.btnScreenScale4 );

    mnuExtra.add( mnuExtraScale );
    mnuExtra.addSeparator();

    mnuExtra.add( createJMenuItem( "Audio/Kassette...", "extra.audio" ) );

    JMenu mnuExtraTools = new JMenu( "Werkzeuge" );
    mnuExtraTools.add( createJMenuItem( "Debugger...", "extra.debugger" ) );
    mnuExtraTools.add( createJMenuItem(
				"Reassembler...", "extra.reassembler" ) );
    mnuExtraTools.add( createJMenuItem(
				"Speicheransicht...", "extra.memviewer" ) );
    mnuExtraTools.add( createJMenuItem( "Rechner...", "extra.calculator" ) );
    mnuExtraTools.add( createJMenuItem(
				"Hex-Dateivergleich...",
				"extra.hex.diff" ) );
    mnuExtraTools.add( createJMenuItem(
				"Hex-Editor...",
				"extra.hex.editor" ) );
    mnuExtraTools.add( createJMenuItem(
				"Bildbetrachter...", "extra.imageviewer" ) );
    mnuExtra.add( mnuExtraTools );
    mnuExtra.addSeparator();

    mnuExtra.add( createJMenuItem( "Einstellungen...", "extra.settings" ) );
    mnuExtra.add( createJMenuItem( "Profil anwenden...", "extra.profile" ) );
    mnuExtra.addSeparator();

    this.mnuExtraPause = createJMenuItem(
				"Pause",
				"extra.pause",
				KeyStroke.getKeyStroke( KeyEvent.VK_F7, 0 ) );
    mnuExtra.add( this.mnuExtraPause );

    mnuExtra.add( createJMenuItem(
			"NMI ausl\u00F6sen",
			"extra.nmi",
			KeyStroke.getKeyStroke( KeyEvent.VK_F8, 0 ) ) );
    mnuExtra.add( createJMenuItem(
			"Zur\u00FCcksetzen (RESET)",
			"extra.reset",
			KeyStroke.getKeyStroke( KeyEvent.VK_F9, 0 ) ) );
    mnuExtra.add( createJMenuItem(
			"Einschalten (Power On)",
			"extra.power_on" ) );


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

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    panelToolBar.add( toolBar );

    toolBar.add( createImageButton(
				"/images/file/open.png",
				"Laden",
				"file.load" ) );

    toolBar.add( createImageButton(
				"/images/file/save.png",
				"Speichern",
				"file.save" ) );
    toolBar.addSeparator();

    this.btnCopy = createImageButton(
				"/images/edit/copy.png",
				"Kopieren",
				"edit.copy" );
    this.btnCopy.setEnabled( false );
    toolBar.add( this.btnCopy );

    this.btnPaste = createImageButton(
				"/images/edit/paste.png",
				"Einf\u00FCgen",
				"edit.paste" );
    this.btnPaste.setEnabled( false );
    toolBar.add( this.btnPaste );
    toolBar.addSeparator();

    toolBar.add( createImageButton(
				"/images/file/browse.png",
				"Datei-Browser",
				"file.browser" ) );
    toolBar.add( createImageButton(
				"/images/file/audio.png",
				"Audio",
				"extra.audio" ) );

    toolBar.add( createImageButton(
				"/images/file/reset.png",
				"Zur\u00FCcksetzen (RESET)",
				"extra.reset" ) );


    // Z1013-Bildschirmausgabe
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


    // Fensterposition
    Dimension size = getToolkit().getScreenSize();
    if( size != null ) {
      int x = (size.width  - 256) / 2;
      int y = (size.height - 256) / 2;
      setLocation( x >= 0 ? x : 0, y >= 0 ? y : 0 );
    }


    // Drop-Ziel
    (new DropTarget( this.screenFld, this )).setActive( true );


    // sonstiges
    updActionComponents();
    updPasteBtnsEnabled();
    this.statusRefreshTimer = new javax.swing.Timer( 1000, this );
    this.statusRefreshTimer.start();

    Z80CPU z80cpu = this.emuThread.getZ80CPU();
    if( z80cpu != null ) {
      z80cpu.addStatusListener(
			new Z80StatusListener()
			{
			  public void z80StatusChanged()
			  {
			    fireUpdActionComponents();
			  }
			} );
    }
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


  public int getScreenRefreshMillis()
  {
    return this.screenRefreshMillis;
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


  public void openText( String text )
  {
    doFileEditor().openText( text );
  }


  public void openTextFile( File file )
  {
    doFileEditor().openFile( file );
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
    // empty
  }


  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  public void drop( DropTargetDropEvent e )
  {
    if( EmuUtil.isFileDrop( e ) ) {
      e.acceptDrop( DnDConstants.ACTION_COPY );    // Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
	try {
	  Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof Collection ) {
	      Iterator iter = ((Collection) o).iterator();
	      if( iter != null ) {
		if( iter.hasNext() ) {
		  o = iter.next();
		  if( o != null ) {
		    File file = null;
		    if( o instanceof File ) {
		      file = (File) o;
		    }
		    else if( o instanceof String ) {
		      file = new File( o.toString() );
		    }
		    if( file != null ) {
		      if( iter.hasNext() ) {
			BasicDlg.showErrorDlg(
				this,
				"Sie k\u00F6nnen nur eine Datei"
					+ " in den Emulator ziehen." );
		      } else {
 			LoadDlg.loadFile(
					this,		// owner
					this,		// ScreenFrm
					file,
					true,		// interactive
					true,		// startEnabled
					true );		// startsSelected
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
	catch( Exception ex ) {}
      }
      e.dropComplete( true );
    } else {
      e.rejectDrop();
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


	/* --- ueberschriebene Methoden fuer KeyListener --- */

  public boolean applySettings( Properties props, boolean resizable )
  {
    EmuSys emuSys = emuThread.getNextEmuSys();
    if( emuSys == null ) {
      emuSys = emuThread.getEmuSys();
    }
    setTitle( "JKCEMU: " + emuSys.getTitle() );
    this.mnuHelpSys.setEnabled( emuSys.getHelpPage() != null );

    // Bildschirmaktualisierung
    this.screenRefreshTimer.stop();
    this.screenRefreshMillis = EmuUtil.parseIntProperty(
				props,
				"jkcemu.screen.refresh.ms",
				0,
				SettingsFrm.DEFAULT_SCREEN_REFRESH_MS );
    if( this.screenRefreshMillis < 10 ) {
      this.screenRefreshMillis = SettingsFrm.DEFAULT_SCREEN_REFRESH_MS;
    }
    this.screenRefreshTimer.setDelay( this.screenRefreshMillis );
    this.screenRefreshTimer.start();

    // Bildschirmgroesse,
    EmuSys oldEmuSys      = this.screenFld.getEmuSys();
    int    oldMargin      = this.screenFld.getMargin();
    int    oldScreenScale = this.screenFld.getScreenScale();
    int    screenScale    = oldScreenScale;

    // Skalierung nur anpassen, wenn das Fenster noch nicht angezeigt wird
    if( !isVisible() ) {
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
    this.screenFld.setEmuSys( emuSys );
    this.screenFld.setScreenScale( screenScale );
    setScreenDirty( true );

    boolean state = emuSys.canExtractScreenText();
    this.mnuScreenTextShow.setEnabled( state );
    this.mnuScreenTextCopy.setEnabled( state );
    this.mnuScreenTextSave.setEnabled( state );


    // Fenstergroesse
    boolean rv     = false;
    int     margin = EmuUtil.parseIntProperty(
					props,
					"jkcemu.screen.margin",
					0,
					ScreenFld.DEFAULT_MARGIN );
    this.screenFld.setMargin( margin );
    if( (screenScale != oldScreenScale)
	|| (emuSys.getScreenWidth() != oldEmuSys.getScreenWidth())
	|| (emuSys.getScreenHeight() != oldEmuSys.getScreenHeight())
	|| (margin != oldMargin) )
    {
      pack();
      rv = true;
    } else {
      rv = super.applySettings( props, resizable );
    }
    return rv;
  }


  public void keyPressed( KeyEvent e )
  {
    int keyCode = e.getKeyCode();
    if( (keyCode == KeyEvent.VK_BACK_SPACE)
	|| (keyCode == KeyEvent.VK_CONTROL)
	|| (keyCode == KeyEvent.VK_DELETE)
	|| (keyCode == KeyEvent.VK_DOWN)
	|| (keyCode == KeyEvent.VK_END)
	|| (keyCode == KeyEvent.VK_ENTER)
	|| (keyCode == KeyEvent.VK_ESCAPE)
	|| (keyCode == KeyEvent.VK_F1)
	|| (keyCode == KeyEvent.VK_F2)
	|| (keyCode == KeyEvent.VK_F3)
	|| (keyCode == KeyEvent.VK_F4)
	|| (keyCode == KeyEvent.VK_F5)
	|| (keyCode == KeyEvent.VK_F6)
	|| (keyCode == KeyEvent.VK_HOME)
	|| (keyCode == KeyEvent.VK_INSERT)
	|| (keyCode == KeyEvent.VK_LEFT)
	|| (keyCode == KeyEvent.VK_PAUSE)
	|| (keyCode == KeyEvent.VK_RIGHT)
	|| (keyCode == KeyEvent.VK_SHIFT)
	|| (keyCode == KeyEvent.VK_SPACE)
	|| (keyCode == KeyEvent.VK_TAB)
	|| (keyCode == KeyEvent.VK_UP) )
    {
      if( this.emuThread.keyPressed( e ) ) {
	this.ignoreKeyChar = true;
	e.consume();
      }
    }
  }


  public void keyReleased( KeyEvent e )
  {
    int keyCode = e.getKeyCode();
    if( (keyCode != KeyEvent.VK_F7)
	&& (keyCode != KeyEvent.VK_F8)
	&& (keyCode != KeyEvent.VK_F9)
	&& (keyCode != KeyEvent.VK_F10) )
    {
      this.emuThread.keyReleased();
      e.consume();
    }
    this.ignoreKeyChar = false;
  }


  public void keyTyped( KeyEvent e )
  {
    if( this.ignoreKeyChar ) {
      this.ignoreKeyChar = false;
    } else {
      this.emuThread.keyTyped( e.getKeyChar() );
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
	  else if( actionCmd.equals( "file.ramfloppy_a.load" ) ) {
	    rv = true;
	    doFileRAMFloppyLoad(
			this.emuThread.getRAMFloppyA(),
			'A',
			this.emuThread.getEmuSys().supportsRAMFloppyA() );
	  }
	  else if( actionCmd.equals( "file.ramfloppy_a.save" ) ) {
	    rv = true;
	    doFileRAMFloppySave(
			this.emuThread.getRAMFloppyA(),
			'A',
			this.emuThread.getEmuSys().supportsRAMFloppyA() );
	  }
	  else if( actionCmd.equals( "file.ramfloppy_b.load" ) ) {
	    rv = true;
	    doFileRAMFloppyLoad(
			this.emuThread.getRAMFloppyB(),
			'B',
			this.emuThread.getEmuSys().supportsRAMFloppyB() );
	  }
	  else if( actionCmd.equals( "file.ramfloppy_b.save" ) ) {
	    rv = true;
	    doFileRAMFloppySave(
			this.emuThread.getRAMFloppyB(),
			'B',
			this.emuThread.getEmuSys().supportsRAMFloppyB() );
	  }
	  else if( actionCmd.equals( "file.basic.open" ) ) {
	    rv = true;
	    this.emuThread.getEmuSys().openBasicProgram();
	  }
	  else if( actionCmd.equals( "file.basic.save" ) ) {
	    rv = true;
	    this.emuThread.getEmuSys().saveBasicProgram();
	  }
	  else if( actionCmd.equals( "file.tinybasic.open" ) ) {
	    rv = true;
	    this.emuThread.getEmuSys().openTinyBasicProgram();
	  }
	  else if( actionCmd.equals( "file.tinybasic.save" ) ) {
	    rv = true;
	    this.emuThread.getEmuSys().saveTinyBasicProgram();
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
	  else if( actionCmd.equals( "extra.imageviewer" ) ) {
	    rv = true;
	    doExtraImageViewer();
	  }
	  else if( actionCmd.equals( "extra.debugger" ) ) {
	    rv = true;
	    doExtraDebugger();
	  }
	  else if( actionCmd.equals( "extra.reassembler" ) ) {
	    rv = true;
	    doExtraReassembler();
	  }
	  else if( actionCmd.equals( "extra.memviewer" ) ) {
	    rv = true;
	    doExtraMemViewer();
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
	    showHelp( null );
	  }
	  else if( actionCmd.equals( "help.system" ) ) {
	    rv = true;
	    doHelpSystem();
	  }
	  else if( actionCmd.equals( "help.about" ) ) {
	    rv = true;
	    doHelpAbout();
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
    String msg = null;
    if( this.emuThread.getRAMFloppyA().hasDataChanged()
	&& this.emuThread.getRAMFloppyB().hasDataChanged() )
    {
      msg = "Die Daten in beiden RAM-Floppies";
    } else {
      if( this.emuThread.getRAMFloppyA().hasDataChanged() ) {
	msg = "Die Daten in RAM-Floppy A";
      }
      else if( this.emuThread.getRAMFloppyB().hasDataChanged() ) {
	msg = "Die Daten in RAM-Floppy B";
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

    // Programmbeendigung nicht durch Exception verhindern lassen
    try {

      // untergeordnete Fenster schliessen
      Collection<BasicFrm> c = this.subFrms.values();
      if( c != null ) {
	Iterator<BasicFrm> iter = c.iterator();
	if( iter != null ) {
	  try {
	    while( iter.hasNext() ) {
	      BasicFrm frm = iter.next();
	      if( frm instanceof AudioFrm ) {
		((AudioFrm) frm).doQuit();
	      }
	      else if( !frm.doClose() ) {
		return false;
	      }
	    }
	  }
	  catch( NoSuchElementException ex ) {}
	}
      }

      // Emulator-Thread beenden
      this.emuThread.stopEmulator();

      // max. eine halbe Sekunde auf Thread-Beendigung warten
      try {
	this.emuThread.join( 500 );
      }
      catch( InterruptedException ex ) {}

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
    String screenText = checkConvertFromISO646DE(
				this.emuThread.getEmuSys().getScreenText() );
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


  private void doFileScreenTextSave()
  {
    String screenText = checkConvertFromISO646DE(
				this.emuThread.getEmuSys().getScreenText() );
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


  private void doFileScreenTextShow()
  {
    String screenText = checkConvertFromISO646DE(
				this.emuThread.getEmuSys().getScreenText() );
    if( screenText != null )
      doFileEditor().openText( screenText );
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


	/* --- Aktionen im Menu Datei --- */

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


  private void doExtraAudio()
  {
    if( reopenSubFrm( AudioFrm.class ) == null ) {
      AudioFrm f = new AudioFrm( this );
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
    if( reopenSubFrm( DebugFrm.class ) == null ) {
      DebugFrm f = new DebugFrm( this );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  private void doExtraMemViewer()
  {
    if( reopenSubFrm( MemViewFrm.class ) == null ) {
      MemViewFrm f = new MemViewFrm( this.emuThread );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
    }
  }


  private void doExtraReassembler()
  {
    if( reopenSubFrm( ReassFrm.class ) == null ) {
      ReassFrm f = new ReassFrm( this.emuThread );
      f.setVisible( true );
      this.subFrms.put( f.getClass(), f );
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


  private HexEditFrm doExtraHexEditor()
  {
    HexEditFrm f = (HexEditFrm) reopenSubFrm( HexEditFrm.class );
    if( f == null ) {
      f = new HexEditFrm( this );
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

  private void doHelpAbout()
  {
    BasicDlg.showInfoDlg(
      this,
      Main.getVersion()
	+ "\n...ein in Java geschriebener Kleincomputer-Emulator"
	+ "\n\n(c) 2008-2009 Jens M\u00FCller"
	+ "\n\nLizenz: GNU General Public License Version 3"
	+ "\n\nJegliche Gew\u00E4hrleistung und Haftung ist ausgeschlossen!"
	+ "\nDie Anwendung dieser Software erfolgt ausschlie\u00DFlich"
	+ "\nauf eigenes Risiko."
	+ "\n\nDer Emulator enth\u00E4lt Systemsoftware, Schriftarten,"
	+ " Grafiksymbole"
	+ "\nund Beschreibungen der emulierten Computer."
	+ "\nDie Urheberschaften daran liegen bei:"
	+ "\n- VEB Me\u00DFelektronik Dresden (Z9001, KC85/1, KC87)"
	+ "\n- VEB Mikroelektronik Erfurt (LC80)"
	+ "\n- VEB Mikroelektronik M\u00FChlhausen"
	+ " (HC900, KC85/2, KC85/3, KC85/4)"
	+ "\n- VEB Polytechnik Karl-Marx-Stadt (Poly-Computer 880)"
	+ "\n- VEB Robotron-Elektronik Riesa (Z1013)"
	+ "\n- Bernd H\u00FCbler (H\u00FCbler/Evert-MC, H\u00FCbler-Grafik-MC)"
	+ "\n- Dr. Rainer Brosig (erweitertes Z1013-Monitorprogramm)"
	+ "\n- Eckart Buschendorf (LC-80.2-Monitorprogramm)"
	+ "\n- Eckhard Ludwig (SCCH-Software f\u00FCr AC1 und LLC2)"
	+ "\n- Eckhard Schiller (BCS3 und VCS-80)"
	+ "\n- Frank Heyder (Monitorprogramm 3.1 f\u00FCr AC1)"
	+ "\n- Frank Pr\u00FCfer (S/P-BASIC V3.3 f\u00FCr BCS3)"
	+ "\n- Joachim Czepa (C-80)"
	+ "\n- Klaus-Peter Evert (H\u00FCbler/Evert-MC)"
	+ "\n- Manfred Kramer (Kramer-MC)"
	+ "\n- Torsten Musiol (Maschinenkode-Editor f\u00FCr BCS3)"
	+ "\n\nWeitere Informationen finden Sie in der Hilfe sowie"
	+ "\nim Internet unter http://www.jens-mueller.org/jkcemu"
	+ "\n",
      "\u00DCber JKCEMU..." );
  }


  private void doHelpSystem()
  {
    String page = this.screenFld.getEmuSys().getHelpPage();
    if( page != null )
      showHelp( page );
  }


	/* --- private Methoden --- */

  private String checkConvertFromISO646DE( String text )
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {

	// ggf. deutsche Umlaute konvertieren
	boolean extFont = (this.emuThread.getExtFont() != null);
	if( extFont || this.emuThread.getEmuSys().isISO646DE() ) {
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


  private void fireUpdActionComponents()
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


  private void refreshStatus()
  {
    String msg    = "Bereit";
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
	  double curSpeedMHz = z80cpu.getCurrentSpeedKHz() / 1000.0;
	  if( curSpeedMHz > 0.09 ) {
	    msg = "Emulierte Taktfrequenz: "
		    + this.speedFmt.format( curSpeedMHz ) + " MHz";
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
      frm.setVisible( true );
      frm.setState( Frame.NORMAL );
      frm.toFront();
      if( frmClass.isInstance( frm ) )
	rv = frm;
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

