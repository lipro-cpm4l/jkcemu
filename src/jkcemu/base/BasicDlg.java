/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer modale Swing-Dialoge
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;


public class BasicDlg extends JDialog implements
					ActionListener,
					MouseListener,
					KeyListener,
					WindowListener
{
  public BasicDlg( Window owner, String title )
  {
    super( owner, title, Dialog.ModalityType.DOCUMENT_MODAL );
    init();
  }


  public BasicDlg( Window owner, Dialog.ModalityType modalityType )
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
    JButton btn = EmuUtil.createImageButton( this, imgName, text );
    btn.addActionListener( this );
    return btn;
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


  /*
   * Die Methoden zentrieren den Dialog ueber dem Dialogeigentuemer
   */
  public void setParentCentered()
  {
    setParentCentered( this );
  }


  public static void setParentCentered( Dialog dlg )
  {
    Component parent = dlg.getParent();
    if( parent != null ) {
      Dimension pSize  = parent.getSize();
      Point     pLoc   = parent.getLocation();
      Dimension mySize = dlg.getSize();

      int x = pLoc.x + (pSize.width / 2) - (mySize.width / 2);
      int y = pLoc.y + (pSize.height / 2) - (mySize.height / 2);

      dlg.setBounds(
		x >= 0 ? x : 0,
		y >= 0 ? y : 0,
		mySize.width,
		mySize.height );
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
    if( showPopupInternal( e ) )
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
    if( showPopupInternal( e ) )
      e.consume();
  }

  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( showPopupInternal( e ) )
      e.consume();
  }


	/* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e != null ) {
      if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
	if( doActionInternal( e ) ) {
	  e.consume();
	}
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


  protected boolean showPopup( MouseEvent e )
  {
    return false;
  }


	/* --- statische Methoden --- */

  /*
   * Diese Methoden zeigen eine Fehlermeldung an.
   */
  public static void showErrorDlg( Component owner, String msg, Exception ex )
  {
    String exMsg = null;
    if( ex != null ) {
      exMsg = ex.getMessage();
      if( exMsg != null ) {
	if( exMsg.trim().isEmpty() ) {
	  exMsg = null;
	}
      }
    }
    if( (msg != null) && (ex != null) ) {
      String exName = ex.getClass().getName();
      if( exMsg != null ) {
	if( exName.startsWith( "jkcemu." )
	    || exName.startsWith( "z80emu." ) )
	{
	  msg = msg + "\n\n" + exMsg;
	} else {
	  msg = msg + "\n\n" + "Fehlermeldung des Betriebssystems:\n" + exMsg;
	}
      } else {
	msg = msg + "\n\n" + exName;
      }
    }
    if( msg == null ) {
      msg = exMsg;
    }
    showErrorDlg( owner, msg != null ? msg : "Unbekannter Fehler" );
  }


  public static void showErrorDlg( Component owner, Exception ex )
  {
    showErrorDlg( owner, null, ex );
  }


  public static void showErrorDlg( Component owner, String msg )
  {
    showErrorDlg( owner, msg, "Fehler" );
  }


  public static void showErrorDlg( Component owner, String msg, String title )
  {
    EmuUtil.frameToFront( owner );
    JOptionPane.showMessageDialog(
		owner,
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
    pane.createDialog( owner, title ).setVisible( true );

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
		owner,
		msg,
		title,
		JOptionPane.INFORMATION_MESSAGE );
  }


  /*
   * Diese Methode zeigt einen Warnungssdialog
   */
  public static void showWarningDlg( Component owner, String msg )
  {
    EmuUtil.frameToFront( owner );
    JOptionPane.showMessageDialog(
		owner,
		msg,
		"Warnung",
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
    return showYesNoDlg( owner, msg, "Best\u00E4tigung" );
  }


  public static boolean showYesNoDlg(
				Component owner,
				String    msg,
				String    title )
  {
    EmuUtil.frameToFront( owner );
    return showYesNoDlg( owner, msg, title, JOptionPane.QUESTION_MESSAGE );
  }


  public static boolean showYesNoWarningDlg(
				Component owner,
				String    msg,
				String    title )
  {
    EmuUtil.frameToFront( owner );
    return showYesNoDlg( owner, msg, title, JOptionPane.WARNING_MESSAGE );
  }


	/* --- private Methoden --- */

  private void init()
  {
    setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
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
      EmuUtil.exitSysError( this, null, ex );
    }
    setWaitCursor( false );
    return rv;
  }


  private boolean showPopupInternal( MouseEvent e )
  {
    boolean rv = false;
    if( e != null ) {
      if( e.isPopupTrigger() ) {
        rv = showPopup( e );
      }
    }
    return rv;
  }


  private static boolean showYesNoDlg(
				Component owner,
				String    msg,
				String    title,
				int       msgType )
  {
    final String optionYes = "Ja";
    final String optionNo  = "Nein";

    String[] options = { optionYes, optionNo };

    EmuUtil.frameToFront( owner );
    JOptionPane pane = new JOptionPane( msg, msgType );
    pane.setOptions( options );
    pane.createDialog( owner, title ).setVisible( true );

    Object selOption = pane.getValue();
    return ((selOption != null) && (selOption == options[ 0 ])) ?
								true : false;
  }
}

