/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer ein Fenster mit einer Bildschirmanzeige
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
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
import java.lang.*;
import java.util.EventObject;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import jkcemu.Main;
import jkcemu.image.ImageFrm;
import jkcemu.image.ImgSaver;
import jkcemu.image.ImgSelection;
import jkcemu.text.TextUtil;


public abstract class AbstractScreenFrm
			extends BaseFrm
			implements FlavorListener
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
  protected static final String ACTION_SCALE_1          = "scale.1";
  protected static final String ACTION_SCALE_2          = "scale.2";
  protected static final String ACTION_SCALE_3          = "scale.3";
  protected static final String ACTION_SCALE_4          = "scale.4";
  protected static final String ACTION_SCREENIMAGE_SHOW = "screen.image.show";
  protected static final String ACTION_SCREENIMAGE_COPY = "screen.image.copy";
  protected static final String ACTION_SCREENIMAGE_SAVE = "screen.image.save";
  protected static final String ACTION_SCREENTEXT_SHOW  = "screen.text.show";
  protected static final String ACTION_SCREENTEXT_COPY  = "screen.text.copy";
  protected static final String ACTION_SCREENTEXT_SAVE  = "screen.text.save";
  protected static final String ACTION_SCREENSHOT       = "screenshot";
  protected static final String ACTION_SCREENVIDEO      = "screen.video";

  protected JButton              btnCopy;
  protected JButton              btnPaste;
  protected JMenuItem            mnuCopy;
  protected JMenuItem            mnuPaste;
  protected JMenuItem            mnuPasteWith;
  protected JMenuItem            mnuPasteCancel;
  protected JMenuItem            mnuScreenTextCopy;
  protected JMenuItem            mnuScreenTextSave;
  protected JMenuItem            mnuScreenTextShow;
  protected JMenuItem            mnuPopupCopy;
  protected JMenuItem            mnuPopupPaste;
  protected JRadioButtonMenuItem mnuScale1;
  protected JRadioButtonMenuItem mnuScale2;
  protected JRadioButtonMenuItem mnuScale3;
  protected JRadioButtonMenuItem mnuScale4;
  protected JPopupMenu           mnuPopup;
  protected Clipboard            clipboard;
  protected boolean              copyEnabled;
  protected boolean              pasteEnabled;
  protected volatile boolean     screenDirty;
  protected ScreenFld            screenFld;
  protected int                  screenRefreshMillis;
  protected javax.swing.Timer    screenRefreshTimer;

  private static Set<String> closeMsgShownSet = new TreeSet<>();

  private String closeMsg;


  protected AbstractScreenFrm()
  {
    this.btnCopy             = null;
    this.btnPaste            = null;
    this.mnuCopy             = null;
    this.mnuPaste            = null;
    this.mnuPasteWith        = null;
    this.mnuPasteCancel      = null;
    this.mnuScreenTextCopy   = null;
    this.mnuScreenTextSave   = null;
    this.mnuScreenTextShow   = null;
    this.mnuPopupCopy        = null;
    this.mnuPopupPaste       = null;
    this.mnuPopup            = null;
    this.mnuScale1           = null;
    this.mnuScale2           = null;
    this.mnuScale3           = null;
    this.mnuScale4           = null;
    this.closeMsg            = null;
    this.clipboard           = null;
    this.copyEnabled         = false;
    this.pasteEnabled        = false;
    this.screenDirty         = false;
    this.screenRefreshMillis = getDefaultScreenRefreshMillis();
    this.screenRefreshTimer  = new javax.swing.Timer(
					this.screenRefreshMillis,
					this );
    this.screenRefreshTimer.start();

    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
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


  protected JMenu createEditMenu(
			boolean createCopyItem,
			boolean createPasteItems )
  {
    JMenu mnuEdit = null;
    if( createCopyItem || createPasteItems ) {
      mnuEdit = new JMenu( "Bearbeiten" );
      mnuEdit.setMnemonic( KeyEvent.VK_B );

      if( createCopyItem ) {
	this.mnuCopy = createJMenuItem(
			"Kopieren",
			ACTION_COPY,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_C,
					InputEvent.ALT_MASK ) );
	this.mnuCopy.setEnabled( false );
	mnuEdit.add( this.mnuCopy );
      }

      if( createPasteItems ) {
	this.mnuPaste = createJMenuItem(
			"Einf\u00FCgen",
			ACTION_PASTE,
			KeyStroke.getKeyStroke(
					KeyEvent.VK_V,
					InputEvent.ALT_MASK ) );
	this.mnuPaste.setEnabled( false );
	mnuEdit.add( this.mnuPaste );

	this.mnuPasteWith = createJMenuItem(
				"Einf\u00FCgen mit...",
				ACTION_PASTE_WITH );
	this.mnuPasteWith.setEnabled( false );
	mnuEdit.add( this.mnuPasteWith );
	mnuEdit.addSeparator();

	this.mnuPasteCancel = createJMenuItem(
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
    JMenu       mnuScale = new JMenu( "Ansicht" );
    ButtonGroup grpScale = new ButtonGroup();

    this.mnuScale1 = createJRadioButtonMenuItem(
				grpScale,
				"100 %",
				ACTION_SCALE_1,
				true,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_1,
					InputEvent.ALT_MASK ) );
    mnuScale.add( this.mnuScale1 );

    this.mnuScale2 = createJRadioButtonMenuItem(
				grpScale,
				"200 %",
				ACTION_SCALE_2,
				false,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_2,
					InputEvent.ALT_MASK ) );
    mnuScale.add( this.mnuScale2 );

    this.mnuScale3 = createJRadioButtonMenuItem(
				grpScale,
				"300 %",
				ACTION_SCALE_3,
				false,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_3,
					InputEvent.ALT_MASK ) );
    mnuScale.add( this.mnuScale3 );

    this.mnuScale4 = createJRadioButtonMenuItem(
				grpScale,
				"400 %",
				ACTION_SCALE_4,
				false,
				KeyStroke.getKeyStroke(
					KeyEvent.VK_4,
					InputEvent.ALT_MASK ) );
    mnuScale.add( this.mnuScale4 );
    return mnuScale;
  }


  protected JMenu createScreenMenu( boolean createTextItems )
  {
    JMenu mnuScreen = new JMenu( "Bildschirmausgabe" );
    mnuScreen.add( createJMenuItem(
				"als Bildschirmfoto anzeigen...",
				ACTION_SCREENIMAGE_SHOW ) );
    mnuScreen.add( createJMenuItem(
				"als Bild kopieren",
				ACTION_SCREENIMAGE_COPY ) );
    mnuScreen.add( createJMenuItem(
				"als Bilddatei speichern...",
				ACTION_SCREENIMAGE_SAVE ) );
    if( createTextItems ) {
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
    }
    return mnuScreen;
  }


  protected boolean doScreenImageSave()
  {
    return (ImgSaver.saveImageAs(
			this,
			this.screenFld.createBufferedImage(),
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
	      rv  = true;
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
    return rv;
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


  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


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
	if( this.mnuPasteCancel != null ) {
	  this.mnuPasteCancel.setEnabled( true );
	}
	updPasteBtns();
	screenDevice.startPastingText( text );
      }
    }
  }


  protected void pastingTextFinished()
  {
    if( this.mnuPasteCancel != null ) {
      this.mnuPasteCancel.setEnabled( false );
    }
    updPasteBtns();
  }


  public void setCloseMsg( String closeMsg )
  {
    this.closeMsg = closeMsg;
  }


  public void setScreenDirty( boolean state )
  {
    this.screenDirty = state;
  }


  protected void setScreenScale( int screenScale )
  {
    this.screenFld.setScreenScale( screenScale );
    switch( screenScale ) {
      case 1:
	if( this.mnuScale1 != null ) {
	  this.mnuScale1.setSelected( true );
	}
	break;

      case 2:
	if( this.mnuScale2 != null ) {
	  this.mnuScale2.setSelected( true );
	}
	break;

      case 3:
	if( this.mnuScale3 != null ) {
	  this.mnuScale3.setSelected( true );
	}
	break;

      case 4:
	if( this.mnuScale4 != null ) {
	  this.mnuScale4.setSelected( true );
	}
	break;
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
    if( this.mnuPopupCopy != null ) {
      this.mnuPopupCopy.setEnabled( state );
    }
    if( this.btnCopy != null ) {
      this.btnCopy.setEnabled( state );
    }
  }


  protected boolean showPopup( MouseEvent e )
  {
    boolean rv = false;
    if( this.mnuPopup != null ) {
      Component c = e.getComponent();
      if( c != null ) {
	this.mnuPopup.show( c, e.getX(), e.getY() );
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
    if( this.mnuPopupPaste != null ) {
      this.mnuPopupPaste.setEnabled( state );
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
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv       = false;
    boolean canClose = true;
    if( (this.closeMsg != null) && !isVisible() ) {
      AbstractScreenDevice screenDevice = getScreenDevice();
      if( screenDevice != null ) {
	String className = screenDevice.getClass().getName();
	if( !closeMsgShownSet.contains( className ) ) {
	  JCheckBox cb = new JCheckBox( EmuUtil.TEXT_DONT_SHOW_MSG_AGAIN );
	  canClose     = (JOptionPane.showConfirmDialog(
				this,
				new Object[] { this.closeMsg, cb },
				"Hinweis",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE )
						== JOptionPane.OK_OPTION);
	  if( cb.isSelected() ) {
	    closeMsgShownSet.add( className );
	  }
	}
      }
    }
    if( canClose ) {
      rv = super.doClose();
    }
    return rv;
  }


  @Override
  public boolean doQuit()
  {
    return super.doClose();
  }


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
    if( !e.isConsumed() ) {
      super.mousePressed( e );
    }
  }


  @Override
  public void resetFired()
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
	this.clipboard.addFlavorListener( this );
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


  protected void doPasteCancel()
  {
    AbstractScreenDevice screenDevice = getScreenDevice();
    if( screenDevice != null ) {
      screenDevice.cancelPastingText();
    }
  }


  private void doScreenImageCopy()
  {
    try {
      if( this.clipboard != null ) {
	ImgSelection ims = new ImgSelection(
				this.screenFld.createBufferedImage() );
	this.clipboard.setContents( ims, ims );
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doScreenImageShow()
  {
    ImageFrm.open( this.screenFld.createBufferedImage(), "Schnappschuss" );
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
}
