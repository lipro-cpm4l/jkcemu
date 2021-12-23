/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer ein Fenster mit einer Bildschirmanzeige
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.util.EventObject;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import jkcemu.Main;
import jkcemu.base.PopupMenuOwner;
import jkcemu.file.FileUtil;
import jkcemu.image.ImageFrm;
import jkcemu.image.ImageSaver;
import jkcemu.image.ImageUtil;
import jkcemu.joystick.JoystickFrm;
import jkcemu.text.TextUtil;


public abstract class AbstractScreenFrm
			extends BaseFrm
			implements FlavorListener, PopupMenuOwner
{
  public static final String PROP_PREFIX = "jkcemu.";

  public static final String PROP_SCREEN_MARGIN = "screen.margin";
  public static final String PROP_COPY_AND_PASTE_DIRECT
					= "copy_and_paste.direct";

  public static final boolean DEFAULT_COPY_AND_PASTE_DIRECT = true;


  protected static final String ACTION_COPY             = "edit.copy";
  protected static final String ACTION_PASTE            = "edit.paste";
  protected static final String ACTION_PASTE_CANCEL     = "edit.paste.cancel";
  protected static final String ACTION_PASTE_WITH       = "edit.paste.with";
  protected static final String ACTION_SCREENIMAGE_SHOW = "screen.image.show";
  protected static final String ACTION_SCREENIMAGE_COPY = "screen.image.copy";
  protected static final String ACTION_SCREENIMAGE_SAVE = "screen.image.save";
  protected static final String ACTION_SCREENTEXT_SHOW  = "screen.text.show";
  protected static final String ACTION_SCREENTEXT_COPY  = "screen.text.copy";
  protected static final String ACTION_SCREENTEXT_SAVE  = "screen.text.save";
  protected static final String ACTION_SCREENSHOT       = "screenshot";
  protected static final String ACTION_SCREENVIDEO      = "screen.video";

  protected EmuThread         emuThread;
  protected JButton           btnCopy;
  protected JButton           btnPaste;
  protected JMenuItem         mnuCopy;
  protected JMenuItem         mnuPaste;
  protected JMenuItem         mnuPasteWith;
  protected JMenuItem         mnuPasteCancel;
  protected JMenuItem         mnuScreenTextCopy;
  protected JMenuItem         mnuScreenTextSave;
  protected JMenuItem         mnuScreenTextShow;
  protected JMenuItem         popupCopy;
  protected JMenuItem         popupPaste;
  protected JPopupMenu        popupMnu;
  protected Clipboard         clipboard;
  protected boolean           copyEnabled;
  protected boolean           pasteEnabled;
  protected volatile boolean  screenDirty;
  protected ScreenFld         screenFld;
  protected int               screenRefreshMillis;
  protected javax.swing.Timer screenRefreshTimer;


  private static final int[] screenScaleKeyCodes = new int[] {
							KeyEvent.VK_1,
							KeyEvent.VK_2,
							KeyEvent.VK_3,
							KeyEvent.VK_4 };

  private JRadioButtonMenuItem[] mnuScaleItems;
  private int                    mnuShortcutKeyMask;
  private boolean                ignoreKeyChar;
  private boolean                joyActionByKey;


  protected AbstractScreenFrm()
  {
    this.emuThread           = null;
    this.btnCopy             = null;
    this.btnPaste            = null;
    this.mnuCopy             = null;
    this.mnuPaste            = null;
    this.mnuPasteWith        = null;
    this.mnuPasteCancel      = null;
    this.mnuScreenTextCopy   = null;
    this.mnuScreenTextSave   = null;
    this.mnuScreenTextShow   = null;
    this.mnuScaleItems       = null;
    this.popupMnu            = null;
    this.popupCopy           = null;
    this.popupPaste          = null;
    this.clipboard           = null;
    this.copyEnabled         = false;
    this.pasteEnabled        = false;
    this.ignoreKeyChar       = false;
    this.joyActionByKey      = false;
    this.screenDirty         = false;
    this.screenRefreshMillis = getDefaultScreenRefreshMillis();
    this.screenRefreshTimer  = new javax.swing.Timer(
					this.screenRefreshMillis,
					this );
    this.screenRefreshTimer.start();

    // Zwischenablage
    Toolkit tk = EmuUtil.getToolkit( this );
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
    }

    /*
     * Die Shortcut-Taste soll nicht die Control-Taste sein.
     * Aus diesem Grund wird geprueft,
     * ob die uebliche Shortcut-Taste die Control-Taste ist.
     * Wenn nein, wird diese verwendet (ist beim Mac so),
     * anderenfalls die ALT-Taste.
     */
    this.mnuShortcutKeyMask = 0;
    try {
      // Seit Java 10 gibt es die Methode Toolkit.getMenuShortcutKeyMaskEx()
      Object v = tk.getClass().getMethod( "getMenuShortcutKeyMaskEx" )
							.invoke( tk );
      if( v != null ) {
	if( v instanceof Number ) {
	  this.mnuShortcutKeyMask = ((Number) v).intValue();
	  if( this.mnuShortcutKeyMask == InputEvent.CTRL_DOWN_MASK ) {
	    this.mnuShortcutKeyMask = InputEvent.ALT_DOWN_MASK;
	  }
	}
      }
    }
    catch( Exception ex ) {}
    if( this.mnuShortcutKeyMask == 0 ) {
      /*
       * Vor Java 10 gibt es die Methode Toolkit.getMenuShortcutKeyMask(),
       * die noch eine alte Control-Maske liefert
       * (Event.CTRL_MASK bzw. InputEvent.CTRL_MASK).
       * Da aber die Felder der alten Codes mit Java 9 deprecated sind,
       * werden diese hier per Reflection ausgelesen und es wird auch
       * auf InputEvent.CTRL_DOWN_MASK geprueft.
       */
      int ctrlMask = InputEvent.CTRL_DOWN_MASK;
      try {
	ctrlMask |= Class.forName( "java.awt.Event" )
					.getDeclaredField( "CTRL_MASK" )
					.getInt( null );
      }
      catch( Exception ex ) {}
      try {
	ctrlMask |= InputEvent.class.getDeclaredField( "CTRL_MASK" )
					.getInt( null );
      }
      catch( Exception ex ) {}
      try {
	Object v = tk.getClass().getMethod( "getMenuShortcutKeyMask" )
							.invoke( tk );
	if( v != null ) {
	  if( v instanceof Number ) {
	    this.mnuShortcutKeyMask = ((Number) v).intValue();
	    if( (this.mnuShortcutKeyMask & ctrlMask) != 0 ) {
	      this.mnuShortcutKeyMask = InputEvent.ALT_DOWN_MASK;
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
    if( this.mnuShortcutKeyMask == 0 ) {
      this.mnuShortcutKeyMask = InputEvent.ALT_DOWN_MASK;
    }
  }


  protected JMenuItem createMenuItemWithNonControlAccelerator(
						String text,
						String actionCmd,
						int    keyCode )
  {
    JMenuItem item = createMenuItem( text, actionCmd );
    item.setAccelerator(
	KeyStroke.getKeyStroke( keyCode, this.mnuShortcutKeyMask ) );
    return item;
  }


  protected JMenuItem createMenuItemWithNonControlAccelerator(
						String  text,
						String  actionCmd,
						int     keyCode,
						boolean shiftDown )
  {
    JMenuItem item = createMenuItem( text, actionCmd );
    item.setAccelerator(
	KeyStroke.getKeyStroke(
		keyCode,
		this.mnuShortcutKeyMask
			| (shiftDown ? InputEvent.SHIFT_DOWN_MASK : 0) ) );
    return item;
  }


  protected void createPopupMenu( boolean copy, boolean paste )
  {
    this.popupMnu = GUIFactory.createPopupMenu();
    if( copy ) {
      this.popupCopy = createMenuItem(
				EmuUtil.TEXT_COPY,
				ACTION_COPY );
      this.popupCopy.setEnabled( false );
      this.popupMnu.add( this.popupCopy );
    }
    if( paste ) {
      this.popupPaste = createMenuItem(
				EmuUtil.TEXT_PASTE,
				ACTION_PASTE );
      this.popupPaste.setEnabled( false );
      this.popupMnu.add( this.popupPaste );
    }
    if( copy || paste ) {
      this.popupMnu.addSeparator();
    }
  }


  protected String checkConvertScreenText( String text )
  {
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( (screenDevice != null) && (text != null) ) {
      int len = text.length();
      if( len > 0 ) {

	// ggf. deutsche Umlaute konvertieren
	if( screenDevice.shouldAskConvertScreenChar() ) {
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
	    Boolean   iso646de  = null;
	    EmuThread emuThread = screenDevice.getEmuThread();
	    if( emuThread != null ) {
	      iso646de = emuThread.getISO646DE();
	    }
	    if( iso646de == null ) {
	      String[] options = {
				"ASCII",
				"Umlaute",
				EmuUtil.TEXT_CANCEL };
	      JOptionPane pane = new JOptionPane(
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
		  if( emuThread != null ) {
		    emuThread.setISO646DE( false );
		  }
		} else if( value.equals( options[ 1 ] ) ) {
		  state = true;
		  if( emuThread != null ) {
		    emuThread.setISO646DE( true );
		  }
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
		    buf.append( '\u00C4' );
		    break;
		  case '\\':
		    buf.append( '\u00D6' );
		    break;
		  case ']':
		    buf.append( '\u00DC' );
		    break;
		  case '{':
		    buf.append( '\u00E4' );
		    break;
		  case '|':
		    buf.append( '\u00F6' );
		    break;
		  case '}':
		    buf.append( '\u00FC' );
		    break;
		  case '~':
		    buf.append( '\u00DF' );
		    break;
		  default:
		    buf.append( ch );
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


  protected JMenu createEditMenu(
			boolean createCopyItem,
			boolean createPasteItems )
  {
    JMenu mnuEdit = null;
    if( createCopyItem || createPasteItems ) {
      mnuEdit = createMenuEdit();

      if( createCopyItem ) {
	this.mnuCopy = createMenuItem( EmuUtil.TEXT_COPY, ACTION_COPY );
	this.mnuCopy.setAccelerator(
		KeyStroke.getKeyStroke(
				KeyEvent.VK_C,
				this.mnuShortcutKeyMask ) );
	this.mnuCopy.setEnabled( false );
	mnuEdit.add( this.mnuCopy );
      }

      if( createPasteItems ) {
	this.mnuPaste = createMenuItem( EmuUtil.TEXT_PASTE, ACTION_PASTE );
	this.mnuPaste.setAccelerator(
		KeyStroke.getKeyStroke(
				KeyEvent.VK_V,
				this.mnuShortcutKeyMask ) );
	this.mnuPaste.setEnabled( false );
	mnuEdit.add( this.mnuPaste );

	this.mnuPasteWith = createMenuItem(
				"Einf\u00FCgen mit...",
				ACTION_PASTE_WITH );
	this.mnuPasteWith.setEnabled( false );
	mnuEdit.add( this.mnuPasteWith );
	mnuEdit.addSeparator();

	this.mnuPasteCancel = createMenuItem(
				"Einf\u00FCgen abbrechen",
				ACTION_PASTE_CANCEL );
	this.mnuPasteCancel.setEnabled( false );
	mnuEdit.add( this.mnuPasteCancel );
      }
    }
    return mnuEdit;
  }


  protected JMenu createScaleMenu()
  {
    JMenu       mnuScale = GUIFactory.createMenu( "Ansicht" );
    ButtonGroup grpScale = new ButtonGroup();

    this.mnuScaleItems = new JRadioButtonMenuItem[
					screenScaleKeyCodes.length ];
    for( int i = 0; i < this.mnuScaleItems.length; i++ ) {
      JRadioButtonMenuItem item = GUIFactory.createRadioButtonMenuItem(
		String.format( "%d%%", (i + 1) * 100 ) );
      item.setAccelerator(
		KeyStroke.getKeyStroke(
				screenScaleKeyCodes[ i ],
				this.mnuShortcutKeyMask ) );
      item.addActionListener( this );
      grpScale.add( item );
      mnuScale.add( item );
      this.mnuScaleItems[ i ] = item;
    }
    return mnuScale;
  }


  public void clearScreenSelection()
  {
    this.screenFld.clearSelection();
  }


  protected JMenu createScreenMenu( boolean createTextItems )
  {
    JMenu mnuScreen = GUIFactory.createMenu( "Grafische Ausgabe" );
    mnuScreen.add( createMenuItem(
				"im Bildbetrachter anzeigen...",
				ACTION_SCREENIMAGE_SHOW ) );
    mnuScreen.add( createMenuItem(
				"als Bild kopieren",
				ACTION_SCREENIMAGE_COPY ) );
    mnuScreen.add( createMenuItem(
				"als Bilddatei speichern...",
				ACTION_SCREENIMAGE_SAVE ) );
    if( createTextItems ) {
      mnuScreen.addSeparator();

      this.mnuScreenTextShow = createMenuItem(
				"im Texteditor anzeigen",
				ACTION_SCREENTEXT_SHOW );
      mnuScreen.add( this.mnuScreenTextShow );

      this.mnuScreenTextCopy = createMenuItem(
				"als Text kopieren",
				ACTION_SCREENTEXT_COPY );
      mnuScreen.add( this.mnuScreenTextCopy );

      this.mnuScreenTextSave = createMenuItem(
				"als Textdatei speichern...",
				ACTION_SCREENTEXT_SAVE );
      mnuScreen.add( this.mnuScreenTextSave );
    }
    return mnuScreen;
  }


  protected boolean doScreenImageSave()
  {
    return (ImageSaver.saveImageAs(
			this,
			this.screenFld.createBufferedImage(),
			ImageUtil.createScreenshotExifData(),
			null ) != null );
  }


  protected void doScreenScale( int screenScale )
  {
    this.screenFld.setScreenScale( screenScale );
    pack();
  }


  protected boolean doScreenTextSave()
  {
    boolean              rv           = false;
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( screenDevice != null ) {
      String screenText = checkConvertScreenText(
					screenDevice.getScreenText() );
      if( screenText != null ) {
	File file = FileUtil.showFileSaveDlg(
			this,
			"Textdatei speichern",
			Main.getLastDirFile( Main.FILE_GROUP_SCREEN ),
			FileUtil.getTextFileFilter() );
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
	      rv  = true;
	    }
	    catch( Exception ex ) {
	      BaseDlg.showErrorDlg(
		this,
		fileName + ":\nSpeichern der Datei fehlgeschlagen\n\n"
			+ ex.getMessage() );
	    }
	    finally {
	      EmuUtil.closeSilently( out );
	    }
	  }
	}
      }
    }
    return rv;
  }


  public void fireScreenSizeChanged()
  {
    if( getExtendedState() != Frame.MAXIMIZED_BOTH ) {
      final Window    window    = this;
      final ScreenFld screenFld = this.screenFld;
      if( (window != null) && (screenFld != null) ) {
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
  }


  public void fireUpdScreenTextActionsEnabled()
  {
    final AbstractScreenDevice screenDevice = getScreenDevice();
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    boolean state = false;
		    if( screenDevice != null ) {
		      state = screenDevice.canExtractScreenText();
		    }
		    if( !state ) {
		      clearScreenSelection();
		    }
		    setScreenTextActionsEnabled( state );
		  }
		} );
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


  public int getScreenRefreshMillis()
  {
    return this.screenRefreshMillis;
  }


  protected abstract AbstractScreenDevice getScreenDevice();


  protected void pasteText( String text )
  {
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( (screenDevice != null) && (text != null) ) {
      if( !text.isEmpty() ) {
	// ggf. Zeichen mappen
	if( text.indexOf( '\r' ) >= 0 ) {
	  text = text.replace( '\r', '\n' );
	}
	if( text.indexOf( '\u00A0' ) >= 0 ) {
	  text = text.replace( '\u00A0', '\u0020' );
	}
	screenDevice.startPastingText( text );
      }
    }
  }


  public void pastingTextStatusChanged( final boolean pasting )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    pastingTextStatusChangedInternal( pasting );
		  }
		} );
  }


  public void setScreenDirty( boolean state )
  {
    this.screenDirty = state;
  }


  protected void setScreenScale( int screenScale )
  {
    this.screenFld.setScreenScale( screenScale );
    if( this.mnuScaleItems != null ) {
      int idx = screenScale - 1;
      if( (idx >= 0) && (idx < this.mnuScaleItems.length) ) {
	this.mnuScaleItems[ idx ].setSelected( true );
      }
    }
  }


  public void setScreenTextActionsEnabled( boolean state )
  {
    if( this.mnuScreenTextShow != null ) {
      this.mnuScreenTextShow.setEnabled( state );
    }
    if( this.mnuScreenTextCopy != null ) {
      this.mnuScreenTextCopy.setEnabled( state );
    }
    if( this.mnuScreenTextSave != null ) {
      this.mnuScreenTextSave.setEnabled( state );
    }
  }


  public void setScreenTextSelected( boolean state )
  {
    if( !this.copyEnabled ) {
      state = false;
    }
    if( this.mnuCopy != null ) {
      this.mnuCopy.setEnabled( state );
    }
    if( this.popupCopy != null ) {
      this.popupCopy.setEnabled( state );
    }
    if( this.btnCopy != null ) {
      this.btnCopy.setEnabled( state );
    }
  }


  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean rv = false;
    if( this.popupMnu != null ) {
      Component c = e.getComponent();
      if( c != null ) {
	this.popupMnu.show( c, e.getX(), e.getY() );
	rv = true;
      }
    }
    return rv;
  }


  protected void updPasteBtns()
  {
    boolean state = false;
    if( this.pasteEnabled && (this.clipboard != null) ) {
      boolean pasting = false;
      if( this.mnuPasteCancel != null ) {
	pasting = this.mnuPasteCancel.isEnabled();
      }
      if( !pasting ) {
	try {
	  state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor );
	}
	catch( Exception ex ) {}
      }
    }
    if( this.mnuPaste != null ) {
      this.mnuPaste.setEnabled( state );
    }
    if( this.mnuPasteWith != null ) {
      this.mnuPasteWith.setEnabled( state );
    }
    if( this.popupPaste != null ) {
      this.popupPaste.setEnabled( state );
    }
    if( this.btnPaste != null ) {
      this.btnPaste.setEnabled( state );
    }
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    updPasteBtns();
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    boolean done = false;
    Object  src  = e.getSource();
    if( (src != null) && (src == this.screenRefreshTimer) ) {
      if( this.screenDirty ) {
	this.screenFld.repaint();
      }
      done = true;
    }
    if( !done ) {
      super.actionPerformed( e );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e instanceof ActionEvent ) {
      String actionCmd = ((ActionEvent) e).getActionCommand();
      if( actionCmd != null ) {
	if( actionCmd.equals( ACTION_COPY ) ) {
	  rv = true;
	  doCopy();
	}
	else if( actionCmd.equals( ACTION_PASTE ) ) {
	  rv = true;
	  doPaste( false );
	}
	else if( actionCmd.equals( ACTION_PASTE_WITH ) ) {
	  rv = true;
	  doPaste( true );
	}
	else if( actionCmd.equals( ACTION_PASTE_CANCEL ) ) {
	  rv = true;
	  doPasteCancel();
	}
	else if( actionCmd.equals( ACTION_SCREENIMAGE_SHOW ) ) {
	  rv = true;
	  doScreenImageShow();
	}
	else if( actionCmd.equals( ACTION_SCREENIMAGE_COPY ) ) {
	  rv = true;
	  doScreenImageCopy();
	}
	else if( actionCmd.equals( ACTION_SCREENIMAGE_SAVE ) ) {
	  rv = true;
	  doScreenImageSave();
	}
	else if( actionCmd.equals( ACTION_SCREENTEXT_SHOW ) ) {
	  rv = true;
	  doScreenTextShow();
	}
	else if( actionCmd.equals( ACTION_SCREENTEXT_SAVE ) ) {
	  rv = true;
	  doScreenTextSave();
	}
	else if( actionCmd.equals( ACTION_SCREENTEXT_COPY ) ) {
	  rv = true;
	  doScreenTextCopy();
	}
      }
    }
    if( !rv && (this.mnuScaleItems != null) ) {
      Object src = e.getSource();
      for( int i = 0; i < this.mnuScaleItems.length; i++ ) {
	if( src == this.mnuScaleItems[ i ] ) {
	  rv = true;
	  doScreenScale( i + 1 );
	  break;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doQuit()
  {
    return super.doClose();
  }


  @Override
  public boolean getPackOnUIUpdate()
  {
    return true;
  }


  /*
   * Die Taste F10 dient der Menuesteuerung des Emulators
   * (Oeffnen des Menues, von Java so vorgegeben)
   * und wird deshalb nicht an das emulierte System weitergegeben.
   */
  @Override
  public void keyPressed( KeyEvent e )
  {
    if( (this.emuThread != null) && (e.getKeyCode() != KeyEvent.VK_F10) ) {
      if( e.isAltDown() || e.isMetaDown() ) {
	int a = JoystickFrm.getJoystickActionByKeyCode( e.getKeyCode() );
	if( a != 0 ) {
	  this.joyActionByKey = true;
	  if( e.isShiftDown() ) {
	    this.emuThread.setJoystickAction( 1, a );
	  } else {
	    this.emuThread.setJoystickAction( 0, a );
	  }
	}
      } else {
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
	} else {
	  if( this.emuThread.keyPressed( e ) ) {
	    this.ignoreKeyChar = true;
	    e.consume();
	  }
	}
      }
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    if( this.emuThread != null ) {
      if( this.joyActionByKey ) {
	this.joyActionByKey = false;
	this.emuThread.setJoystickAction( 0, 0 );
	this.emuThread.setJoystickAction( 1, 0 );
      }
      if( !e.isAltDown() && !e.isMetaDown() ) {
	/*
	 * Das Loslassen von F10 und CONTROL nicht melden,
	 * da F10 von Java selbst verwendet wird und CONTROL
	 * im Tastaturfenster zum Deselektieren der Tasten fuehren wuerde.
	 */
	int keyCode = e.getKeyCode();
	if( (keyCode != KeyEvent.VK_F10)
	    && (keyCode != KeyEvent.VK_CONTROL) )
	{
	  this.emuThread.keyReleased();
	  e.consume();
	}
	this.ignoreKeyChar = false;
      }
    }
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    if( (this.emuThread != null) && !e.isAltDown() && !e.isMetaDown() ) {
      if( this.ignoreKeyChar ) {
	this.ignoreKeyChar = false;
      } else {
	this.emuThread.keyTyped( e.getKeyChar() );
      }
    }
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( (e.getSource() == this.screenFld) && (e.getClickCount() == 1) ) {
      int m = e.getModifiersEx();
      if( (m & InputEvent.BUTTON1_DOWN_MASK) != 0 ) {
	this.screenFld.clearSelection();
	this.screenFld.requestFocus();
	e.consume();
      }
      else if( (m & InputEvent.BUTTON2_DOWN_MASK) != 0 ) {
	if( Main.getBooleanProperty(
			PROP_PREFIX + PROP_COPY_AND_PASTE_DIRECT,
			DEFAULT_COPY_AND_PASTE_DIRECT ) )
	{
	  String text = checkConvertScreenText(
					this.screenFld.getSelectedText() );
	  if( text != null ) {
	    if( !text.isEmpty() ) {
	      setWaitCursor( true );
	      pasteText( text );
	      setWaitCursor( false );
	    }
	  }
	  e.consume();
	}
      }
    }
    if( !e.isConsumed() ) {
      super.mousePressed( e );
    }
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( this.mnuPasteCancel != null ) {
      this.mnuPasteCancel.setEnabled( false );
    }
    updPasteBtns();
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state != isVisible() ) {
      if( state && (this.clipboard != null) ) {
	this.clipboard.addFlavorListener( this );
	updPasteBtns();
      }
      super.setVisible( state );
      if( !state && (this.clipboard != null) ) {
	this.clipboard.removeFlavorListener( this );
      }
    }
  }


	/* --- private Methoden --- */

  private void doCopy()
  {
    EmuUtil.copyToClipboard(
		this,
		checkConvertScreenText( this.screenFld.getSelectedText() ) );
  }


  private void doPaste( boolean askConversion )
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


  private void doPasteCancel()
  {
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( screenDevice != null ) {
      screenDevice.cancelPastingText();
    }
  }


  private void doScreenImageCopy()
  {
    ImageUtil.copyToClipboard( this, this.screenFld.createBufferedImage() );
  }


  private void doScreenImageShow()
  {
    ImageFrm.open(
		this.screenFld.createBufferedImage(),
		ImageUtil.createScreenshotExifData(),
		"Schnappschuss" );
  }


  private void doScreenTextCopy()
  {
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( screenDevice != null ) {
      EmuUtil.copyToClipboard(
		this,
		checkConvertScreenText( screenDevice.getScreenText() ) );
    }
  }


  private void doScreenTextShow()
  {
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( screenDevice != null ) {
      String screenText = checkConvertScreenText(
					screenDevice.getScreenText() );
      if( screenText != null ) {
	ScreenFrm screenFrm = Main.getScreenFrm();
	if( screenFrm != null ) {
	  screenFrm.openText( screenText );
	}
      }
    }
  }


  private void pastingTextStatusChangedInternal( boolean pasting )
  {
    if( this.mnuPasteCancel != null ) {
      this.mnuPasteCancel.setEnabled( pasting );
    }
    updPasteBtns();
  }
}
