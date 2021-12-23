/*
 * (c) 2013-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige eines Log-Textes
 */

package jkcemu.tools.fileconverter;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.Writer;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.PopupMenuOwner;
import jkcemu.file.FileUtil;
import jkcemu.text.LogTextActionMngr;
import jkcemu.text.TextFinder;
import jkcemu.text.TextUtil;


public class LogDlg extends BaseDlg implements PopupMenuOwner
{
  private File              preSelection;
  private LogTextActionMngr actionMngr;
  private TextFinder        textFinder;
  private JTextArea         textArea;
  private JButton           btnSave;
  private JButton           btnMenu;
  private JButton           btnClose;


  public static void showDlg(
			Window owner,
			String logText,
			String title,
			File   preSelection )
  {
    (new LogDlg( owner, logText, title, preSelection )).setVisible( true );
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.actionMngr.getPopupMenu();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnSave ) {
      rv = true;
      this.actionMngr.doSaveAs();
    }
    else if( src == this.btnMenu ) {
      rv = true;
      this.actionMngr.showPopupMenu(
				this.btnMenu,
				0,
				this.btnMenu.getHeight() );
    }
    else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnSave.removeActionListener( this );
      this.btnMenu.removeActionListener( this );
      this.btnClose.removeActionListener( this );
      this.textArea.removeMouseListener( this );
    }
    return rv;
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    return this.actionMngr.showPopupMenu( e );
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getComponent() == this )
      this.btnClose.requestFocus();
  }


	/* --- Konstruktor --- */

  private LogDlg(
		Window owner,
		String logText,
		String title,
		File   preSelection )
  {
    super( owner, title );
    this.preSelection = preSelection;
    this.textFinder   = null;

    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Textbereich
    this.textArea = GUIFactory.createTextArea( 16, 40 );
    this.textArea.setEditable( false );
    if( logText != null ) {
      this.textArea.setText( logText );
    }
    add( GUIFactory.createScrollPane( this.textArea ), gbc );

    // Schaltflaechen
    JPanel panelBtns = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.gridy++;
    add( panelBtns, gbc );

    this.btnSave = GUIFactory.createButton( EmuUtil.TEXT_OPEN_SAVE );
    panelBtns.add( this.btnSave );

    this.btnMenu = GUIFactory.createButton( "Men\u00FC" );
    panelBtns.add( this.btnMenu );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtns.add( this.btnClose );

    // Popup-Menu
    this.actionMngr = new LogTextActionMngr( this.textArea, true );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
    this.textArea.setColumns( 0 );
    this.textArea.setRows( 0 );


    // Listener
    this.btnSave.addActionListener( this );
    this.btnMenu.addActionListener( this );
    this.btnClose.addActionListener( this );
    this.textArea.addMouseListener( this );
  }
}
