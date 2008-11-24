/*
 * (c) 2008 Jens Mueller
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
    JButton btn = EmuUtil.createImageButton( imgName, text );
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

  public void actionPerformed( ActionEvent e )
  {
    doActionInternal( e );
  }


	/* --- MouseListener --- */

  public void mouseClicked( MouseEvent e )
  {
    if( showPopupInternal( e ) )
      e.consume();
  }

  public void mouseEntered( MouseEvent e )
  {
    // empty
  }

  public void mouseExited( MouseEvent e )
  {
    // empty
  }

  public void mousePressed( MouseEvent e )
  {
    if( showPopupInternal( e ) )
      e.consume();
  }

  public void mouseReleased( MouseEvent e )
  {
    if( showPopupInternal( e ) )
      e.consume();
  }


	/* --- KeyListener --- */

  public void keyPressed( KeyEvent e )
  {
    if( e != null ) {
      if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
	if( doActionInternal( e ) )
	  e.consume();
      }
    }
  }

  public void keyReleased( KeyEvent e )
  {
    // empty
  }

  public void keyTyped( KeyEvent e )
  {
    // empty
  }


	/* --- WindowListener --- */

  public void windowActivated( WindowEvent e )
  {
    // empty
  }

  public void windowClosed( WindowEvent e )
  {
    // empty
  }

  public void windowClosing( WindowEvent e )
  {
    doClose();
  }

  public void windowDeactivated( WindowEvent e )
  {
    // empty
  }

  public void windowDeiconified( WindowEvent e )
  {
    // empty
  }

  public void windowIconified( WindowEvent e )
  {
    // empty
  }

  public void windowOpened( WindowEvent e )
  {
    // empty
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
  public static void showErrorDlg( Component owner, Exception ex )
  {
    String msg = ex.getMessage();
    if( msg == null ) {
      msg = ex.getClass().getName();
    }
    showErrorDlg( owner, msg );
  }


  public static void showErrorDlg( Component owner, String msg )
  {
    showErrorDlg( owner, msg, "Fehler" );
  }


  public static void showErrorDlg(
				Component owner,
				String    msg,
				String    title )
  {
    JOptionPane.showMessageDialog(
		owner,
		msg,
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
    JOptionPane.showMessageDialog(
		owner,
		msg,
		"Warnung",
		JOptionPane.WARNING_MESSAGE );
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
				String[]  options )
  {
    JOptionPane pane = new JOptionPane( msg, JOptionPane.QUESTION_MESSAGE );
    pane.setOptions( options );
    pane.createDialog( owner, title ).setVisible( true );

    // ausgewaehlter Knopf ermitteln
    Object selOption = pane.getValue();
    if( selOption != null ) {
      for( int i = 0; i < options.length; i++ ) {
	if( selOption == options[ i ] )
	  return i;
      }
    }
    return -1;
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
    String optionYes = "Ja";
    String optionNo  = "Nein";

    String[] options = new String[ 2 ];
    options[ 0 ] = optionYes;
    options[ 1 ] = optionNo;

    JOptionPane pane = new JOptionPane( msg, JOptionPane.QUESTION_MESSAGE );
    pane.setOptions( options );
    pane.createDialog( owner, title ).setVisible( true );

    // ausgewaehlter Knopf ermitteln
    Object selOption = pane.getValue();
    return ((selOption != null) && (selOption == optionYes)) ? true : false;
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
      EmuUtil.showSysError( this, null, ex );
    }
    setWaitCursor( false );
    return rv;
  }


  private boolean showPopupInternal( MouseEvent e )
  {
    if( e != null ) {
      if( e.isPopupTrigger() )
        return showPopup( e );
    }
    return false;
  }
}

