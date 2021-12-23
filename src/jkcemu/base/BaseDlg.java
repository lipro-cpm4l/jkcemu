/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer modale Swing-Dialoge
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import jkcemu.Main;


public class BaseDlg extends JDialog implements
					ActionListener,
					KeyListener,
					MouseListener,
					WindowListener
{
  private static final String OPTION_YES    = "Ja";
  private static final String OPTION_NO     = "Nein";
  private static final String OPTION_CANCEL = "Abbrechen";

  private static Set<String>         suppressedMessages = new HashSet<>();
  private static Map<String,Boolean> suppressedValues   = new HashMap<>();


  public BaseDlg( Window owner, String title )
  {
    super( owner, title, Dialog.ModalityType.DOCUMENT_MODAL );
    init();
  }


  public BaseDlg( Window owner, Dialog.ModalityType modalityType )
  {
    super( owner, modalityType );
    init();
  }


  /*
   * Diese Methode erzeugt einen Knopf mit einem Bild.
   * Als ActionListner wird der Dialog eingetragen.
   */
  protected JButton createImageButton( String imgName, String text )
  {
    JButton btn = GUIFactory.createImageButton( this, imgName, text );
    btn.addActionListener( this );
    return btn;
  }


  public static JCheckBox createSuppressDlgCheckbox()
  {
    return createSuppressCheckbox( "Diesen Dialog nicht mehr anzeigen" );
  }


  /*
   * Die Methode fordert das Schliessen des Dialoges an und
   * liefert true zurueck, wenn der Dialog auch tatsaechlich
   * geschlossen wurde.
   */
  public boolean doClose()
  {
    setVisible( false );
    dispose();
    return true;
  }


  public boolean getPackOnUIUpdate()
  {
    return !isResizable();
  }


  /*
   * Die Methoden zentrieren den Dialog ueber dem Dialogeigentuemer
   */
  public void setParentCentered()
  {
    setParentCentered( this );
  }


  public static void setParentCentered( Dialog dlg )
  {
    int pX = 0;
    int pY = 0;
    int pW = 0;
    int pH = 0;

    Component parent = dlg.getParent();
    if( parent != null ) {
      pX = parent.getX();
      pY = parent.getY();
      pW = parent.getWidth();
      pH = parent.getHeight();
    } else {
      Toolkit tk = EmuUtil.getToolkit( dlg );
      if( tk != null ) {
	Dimension screenSize = tk.getScreenSize();
	if( screenSize != null ) {
	  pW = screenSize.width;
	  pH = screenSize.height;
	}
      }
    }
    if( (pW > 0) && (pH > 0) ) {
      int myW = dlg.getWidth();
      int myH = dlg.getHeight();
      if( (myW > 0) && (myH > 0) ) {
	int x = pX + ((pW - myW) / 2);
	int y = pY + ((pH - myH) / 2);
	dlg.setLocation( x > 0 ? x : 0, y > 0 ? y : 0 );
      }
    }
  }


  /*
   * Die Methode schaltet den Warte-Cursor ein bzw. aus.
   */
  public void setWaitCursor( boolean state )
  {
    Component c = getGlassPane();
    if( c != null ) {
      c.setVisible( state );
      c.setCursor( state ?
	Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) : null );
    }
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    doActionInternal( e );
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( showPopupMenuInternal( e ) )
      e.consume();
  }

  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mousePressed( MouseEvent e )
  {
    if( showPopupMenuInternal( e ) )
      e.consume();
  }

  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( showPopupMenuInternal( e ) )
      e.consume();
  }


	/* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
      if( doActionInternal( e ) ) {
	e.consume();
      }
    }
  }

  @Override
  public void keyReleased( KeyEvent e )
  {
    // leer
  }

  @Override
  public void keyTyped( KeyEvent e )
  {
    // leer
  }


	/* --- WindowListener --- */

  @Override
  public void windowActivated( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowClosed( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowClosing( WindowEvent e )
  {
    doClose();
  }

  @Override
  public void windowDeactivated( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowDeiconified( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowIconified( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowOpened( WindowEvent e )
  {
    // leer
  }


	 /* --- zu ueberschreibende Methoden --- */

  protected boolean doAction( EventObject e )
  {
    return false;
  }


  /*
   * Rueckgabewert:
   *   true:  Popup wurde angezeigt und Event soll konsumiert werden.
   *   false: Popup nicht angezeigt, Event nicht konsumieren
   */
  protected boolean showPopupMenu( MouseEvent e )
  {
    return false;
  }


	/* --- statische Methoden --- */

  public static JCheckBox createSuppressMsgCheckbox()
  {
    return createSuppressCheckbox( "Diese Meldung nicht mehr anzeigen" );
  }


  public static void fireShowSuppressableInfoDlg(
					final Component owner,
					final String    msg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    showSuppressableInfoDlg( owner, msg );
		  }
		} );
  }


  public static boolean showConfirmDlg( Component owner, String msg )
  {
    EmuUtil.frameToFront( owner );
    return (JOptionPane.showConfirmDialog(
		EmuUtil.getWindow( owner ),
		msg,
		"Best\u00E4tigung",
		JOptionPane.OK_CANCEL_OPTION ) == JOptionPane.OK_OPTION);
  }


  /*
   * Diese Methoden zeigen eine Fehlermeldung an.
   */
  public static void showErrorDlg( Component owner, String msg, Exception ex )
  {
    showErrorDlg( owner, EmuUtil.createErrorMsg( msg, ex ) );
  }


  public static void showErrorDlg( Component owner, Exception ex )
  {
    showErrorDlg( owner, null, ex );
  }


  public static void showErrorDlg( Component owner, String msg )
  {
    showErrorDlg( owner, msg, EmuUtil.TEXT_ERROR );
  }


  public static void showErrorDlg( Component owner, String msg, String title )
  {
    EmuUtil.frameToFront( owner );
    JOptionPane.showMessageDialog(
		EmuUtil.getWindow( owner ),
		msg != null ? msg : "Unbekannter Fehler",
		title,
		JOptionPane.ERROR_MESSAGE );
  }


  public static void showOpenFileErrorDlg(
				Component owner,
				File      file,
				Exception ex )
  {
    showErrorDlg(
	owner,
	"Die Datei \'" + file.getPath()
		+ "\'\nkann nicht ge\u00F6ffnet werden.\n\n"
		+ ex.getMessage() );
  }


  /*
   * Diese Methode zeigt einen Dialog mit waehlbaren Optionen an.
   *
   * Rueckgabewert:
   *	Index der Option oder -1, wenn Dialog
   *	ueber Fenstermanager geschlossen wurde.
   */
  public static int showOptionDlg(
				Component owner,
				String    msg,
				String    title,
				String... options )
  {
    int rv = -1;
    EmuUtil.frameToFront( owner );
    JOptionPane pane = new JOptionPane( msg, JOptionPane.QUESTION_MESSAGE );
    pane.setOptions( options );
    pane.createDialog(
		EmuUtil.getWindow( owner ),
		title ).setVisible( true );

    // ausgewaehlter Knopf ermitteln
    Object selOption = pane.getValue();
    if( selOption != null ) {
      for( int i = 0; i < options.length; i++ ) {
	if( selOption == options[ i ] ) {
	  rv = i;
	  break;
	}
      }
    }
    return rv;
  }


  /*
   * Diese Methoden zeigen einen Informationsdialog an.
   */
  public static void showInfoDlg( Component owner, String msg )
  {
    showInfoDlg( owner, msg, "Information" );
  }


  public static void showInfoDlg(
				Component owner,
				String    msg,
				String    title )
  {
    EmuUtil.frameToFront( owner );
    JOptionPane.showMessageDialog(
		EmuUtil.getWindow( owner ),
		msg,
		title,
		JOptionPane.INFORMATION_MESSAGE );
  }


  /*
   * Diese Methoden zeigen einen Dialog an,
   * der ein Haekchen enthalt, mit dem man ein zukuenftiges Anzeigen
   * unterdruecken kann.
   */
  public static boolean showSuppressableConfirmDlg(
						Component owner,
						String    msg )
  {
    boolean rv = true;
    if( msg != null ) {
      if( !suppressedMessages.contains( msg ) ) {
	EmuUtil.frameToFront( owner );
	JCheckBox cb = createSuppressDlgCheckbox();
	rv = (JOptionPane.showConfirmDialog(
		EmuUtil.getWindow( owner ),
		new Object[] { msg, cb },
		"Best\u00E4tigung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) == JOptionPane.OK_OPTION);
	if( rv && cb.isSelected() ) {
	  suppressedMessages.add( msg );
	}
      }
    }
    return rv;
  }


  public static void showSuppressableInfoDlg( Component owner, String msg )
  {
    if( msg != null ) {
      if( !suppressedMessages.contains( msg ) ) {
	EmuUtil.frameToFront( owner );
	JCheckBox cb = createSuppressMsgCheckbox();
	JOptionPane.showMessageDialog(
			EmuUtil.getWindow( owner ),
			new Object[] { msg, cb },
			"Hinweis",
			JOptionPane.INFORMATION_MESSAGE );
	if( cb.isSelected() ) {
	  suppressedMessages.add( msg );
	}
      }
    }
  }


  public static boolean showSuppressableYesNoDlg(
					Component owner,
					String    msg )
  {
    boolean rv = true;
    if( msg != null ) {
      if( !suppressedMessages.contains( msg ) ) {
	EmuUtil.frameToFront( owner );
	JCheckBox cb = createSuppressDlgCheckbox();
	rv           = showYesNoDlg(
				owner,
				msg,
				cb,
				EmuUtil.TEXT_CONFIRM,
				JOptionPane.QUESTION_MESSAGE );
	if( rv && cb.isSelected() ) {
	  suppressedMessages.add( msg );
	}
      }
    }
    return rv;
  }


  public static Boolean showSuppressableYesNoCancelDlg(
					Component owner,
					String    msg )
  {
    Boolean rv = null;
    if( msg != null ) {
      rv = suppressedValues.get( msg );
      if( rv == null ) {
	EmuUtil.frameToFront( owner );
	JCheckBox cb = createSuppressDlgCheckbox();
	rv           = showYesNoCancelDlg(
				owner,
				msg,
				cb,
				EmuUtil.TEXT_CONFIRM,
				JOptionPane.QUESTION_MESSAGE );
	if( (rv != null) && cb.isSelected() ) {
	  suppressedValues.put( msg, rv );
	}
      }
    }
    return rv;
  }


  /*
   * Diese Methoden zeigen einen Warnungsdialog
   */
  public static void showWarningDlg( Component owner, String msg )
  {
    showWarningDlg( owner, msg, "Warnung" );
  }


  public static void showWarningDlg(
				Component owner,
				String    msg,
				String    title )
  {
    EmuUtil.frameToFront( owner );
    JOptionPane.showMessageDialog(
		EmuUtil.getWindow( owner ),
		msg,
		title,
		JOptionPane.WARNING_MESSAGE );
  }


  /*
   * Diese Methoden zeigen einen Ja-Nein-Dialog an.
   *
   * Rueckgabewert:
   *	true:	"Ja" gedrueckt.
   *	false:	"Nein" gedrueckt oder Fenster geschlossen
   */
  public static boolean showYesNoDlg( Component owner, String msg )
  {
    return showYesNoDlg( owner, msg, EmuUtil.TEXT_CONFIRM );
  }


  public static boolean showYesNoDlg(
				Component owner,
				String    msg,
				JCheckBox checkBox )
  {
    EmuUtil.frameToFront( owner );
    return showYesNoDlg(
		owner,
		msg,
		checkBox,
		EmuUtil.TEXT_CONFIRM,
		JOptionPane.QUESTION_MESSAGE );
  }


  public static boolean showYesNoDlg(
				Component owner,
				String    msg,
				String    title )
  {
    EmuUtil.frameToFront( owner );
    return showYesNoDlg(
		owner,
		msg,
		null,
		title,
		JOptionPane.QUESTION_MESSAGE );
  }


  public static boolean showYesNoWarningDlg(
				Component owner,
				String    msg,
				String    title )
  {
    EmuUtil.frameToFront( owner );
    return showYesNoDlg(
		owner,
		msg,
		null,
		title,
		JOptionPane.WARNING_MESSAGE );
  }


	/* --- private Methoden --- */

  private static JCheckBox createSuppressCheckbox( String text )
  {
    JCheckBox cb = GUIFactory.createCheckBox( text );
    cb.setAlignmentX( JCheckBox.CENTER_ALIGNMENT );
    cb.setAlignmentY( JCheckBox.CENTER_ALIGNMENT );
    cb.setBorder( BorderFactory.createEmptyBorder( 20, 10, 20, 10 ) );
    return cb;
  }


  private void init()
  {
    setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
    Main.updIcon( this );
    addWindowListener( this );
  }


  private boolean doActionInternal( EventObject e )
  {
    boolean rv = false;
    setWaitCursor( true );
    try {
      rv = doAction( e );
    }
    catch( Exception ex ) {
      EmuUtil.checkAndShowError( this, null, ex );
    }
    setWaitCursor( false );
    return rv;
  }


  private boolean showPopupMenuInternal( MouseEvent e )
  {
    boolean rv = false;
    if( e.isPopupTrigger() ) {
      rv = showPopupMenu( e );
    }
    return rv;
  }


  private static boolean showYesNoDlg(
				Component owner,
				String    msg,
				JCheckBox checkBox,
				String    title,
				int       msgType )
  {
    String[] options = { OPTION_YES, OPTION_NO };

    EmuUtil.frameToFront( owner );
    JOptionPane pane = null;
    if( checkBox != null ) {
      pane = new JOptionPane( new Object[] { msg, checkBox }, msgType );
    } else {
      pane = new JOptionPane( msg, msgType );
    }
    pane.setOptions( options );
    Dialog dlg = pane.createDialog(
				EmuUtil.getWindow( owner ),
				title );
    Main.updIcon( dlg );
    dlg.setVisible( true );

    Object selOption = pane.getValue();
    return ((selOption != null) && (selOption == options[ 0 ]));
  }


  private static Boolean showYesNoCancelDlg(
				Component owner,
				String    msg,
				JCheckBox checkBox,
				String    title,
				int       msgType )
  {
    String[] options = { OPTION_YES, OPTION_NO, OPTION_CANCEL };

    EmuUtil.frameToFront( owner );
    JOptionPane pane = null;
    if( checkBox != null ) {
      pane = new JOptionPane( new Object[] { msg, checkBox }, msgType );
    } else {
      pane = new JOptionPane( msg, msgType );
    }
    pane.setOptions( options );
    Dialog dlg = pane.createDialog(
				EmuUtil.getWindow( owner ),
				title );
    Main.updIcon( dlg );
    dlg.setVisible( true );

    Boolean rv        = null;
    Object  selOption = pane.getValue();
    if( selOption != null ) {
      if( selOption.equals( OPTION_YES ) ) {
	rv = Boolean.TRUE;
      } else if( selOption.equals( OPTION_NO ) ) {
	rv = Boolean.FALSE;
      }
    }
    return rv;
  }
}
