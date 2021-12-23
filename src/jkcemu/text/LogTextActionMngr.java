/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verwaltung der Aktionen fuer einen Log-Text
 */

package jkcemu.text;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.PopupMenuOwner;
import jkcemu.file.FileUtil;


public class LogTextActionMngr implements ActionListener, PopupMenuOwner
{
  private TextFinder textFinder;
  private JTextArea  textArea;
  private JMenuItem  popupCopy;
  private JMenuItem  popupFind;
  private JMenuItem  popupFindPrev;
  private JMenuItem  popupFindNext;
  private JMenuItem  popupSaveAs;
  private JMenuItem  popupSelectAll;
  private JPopupMenu popupMnu;


  public LogTextActionMngr( JTextArea textArea, boolean withAccelerator )
  {
    this.textArea   = textArea;
    this.textFinder = null;
    this.popupMnu   = GUIFactory.createPopupMenu();

    this.popupCopy = GUIFactory.createMenuItem( EmuUtil.TEXT_COPY );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator(
				this.popupCopy,
				KeyEvent.VK_C,
				false );
    }
    this.popupCopy.addActionListener( this );
    this.popupMnu.add( this.popupCopy );
    this.popupMnu.addSeparator();

    this.popupFind = GUIFactory.createMenuItem( EmuUtil.TEXT_OPEN_FIND );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator(
				this.popupFind,
				KeyEvent.VK_F,
				false );
    }
    this.popupFind.addActionListener( this );
    this.popupMnu.add( this.popupFind );

    this.popupFindNext = GUIFactory.createMenuItem( EmuUtil.TEXT_FIND_NEXT );
    if( withAccelerator ) {
      EmuUtil.setDirectAccelerator(
				this.popupFindNext,
				KeyEvent.VK_F3,
				false );
    }
    this.popupFindNext.addActionListener( this );
    this.popupMnu.add( this.popupFindNext );

    this.popupFindPrev = GUIFactory.createMenuItem( EmuUtil.TEXT_FIND_PREV );
    if( withAccelerator ) {
      EmuUtil.setDirectAccelerator(
				this.popupFindPrev,
				KeyEvent.VK_F3,
				true );
    }
    this.popupFindPrev.addActionListener( this );
    this.popupMnu.add( this.popupFindPrev );
    this.popupMnu.addSeparator();

    this.popupSaveAs = GUIFactory.createMenuItem( "Speichern unter..." );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator(
				this.popupSaveAs,
				KeyEvent.VK_S,
				true );
    }
    this.popupSaveAs.addActionListener( this );
    this.popupMnu.add( this.popupSaveAs );
    this.popupMnu.addSeparator();

    this.popupSelectAll = GUIFactory.createMenuItem(
					EmuUtil.TEXT_SELECT_ALL );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator(
				this.popupSelectAll,
				KeyEvent.VK_A,
				false );
    }
    this.popupSelectAll.addActionListener( this );
    this.popupMnu.add( this.popupSelectAll );
  }


  public void doCopy()
  {
    String text = this.textArea.getSelectedText();
    if( text != null ) {
      if( text.isEmpty() ) {
	text = null;
      }
    }
    if( text == null ) {
      text = this.textArea.getText();
    }
    if( text != null ) {
      if( !text.isEmpty() ) {
	EmuUtil.copyToClipboard( this.textArea, text );
      }
    }
  }


  public void doSaveAs()
  {
    try {
      File file = FileUtil.showFileSaveDlg(
			EmuUtil.getWindow( this.textArea ),
			"Text speichern",
			Main.getLastDirFile( Main.FILE_GROUP_LOG ),
			FileUtil.getTextFileFilter() );
      if( file != null ) {
	Writer out = null;
	try {
	  out = new FileWriter( file );
	  this.textArea.write( out );
	  out.close();
	  out = null;
	  Main.setLastFile( file, Main.FILE_GROUP_LOG );
	}
	finally {
	  EmuUtil.closeSilently( out );
	}
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this.textArea, ex );
    }
  }


  public boolean showPopupMenu( MouseEvent e )
  {
    return e != null ?
	showPopupMenu( e.getComponent(), e.getX(), e.getY() )
	: false;
  }


  public boolean showPopupMenu( Component c, int x, int y )
  {
    boolean rv = false;
    if( c != null ) {
      boolean  hasText = false;
      Document doc     = this.textArea.getDocument();
      if( doc != null ) {
	if( doc.getLength() > 0 ) {
	  hasText = true;
	}
      }
      this.popupCopy.setEnabled( TextUtil.isTextSelected( this.textArea ) );
      this.popupFind.setEnabled( hasText );
      this.popupFindNext.setEnabled( hasText && (this.textFinder != null) );
      this.popupFindPrev.setEnabled( hasText && (this.textFinder != null) );
      this.popupSaveAs.setEnabled( hasText );
      this.popupSelectAll.setEnabled( hasText );
      this.popupMnu.show( c, x, y );
      rv = true;
    }
    return rv;
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.popupCopy ) {
      doCopy();
    } else if( src == this.popupFind ) {
      this.textFinder = TextFinder.openFindDlg(
				      this.textArea,
				      this.textFinder );
    } else if( src == this.popupFindPrev ) {
      if( this.textFinder != null ) {
	this.textFinder.findPrev( this.textArea );
      }
    } else if( src == this.popupFindNext ) {
      if( this.textFinder != null ) {
	this.textFinder.findNext( this.textArea );
      }
    } else if( src == this.popupSaveAs ) {
      doSaveAs();
    } else if( src == this.popupSelectAll ) {
      this.textArea.selectAll();
    }
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }
}
