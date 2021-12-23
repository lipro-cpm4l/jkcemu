/*
 * (c) 2017-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Abfrage der Aktion,
 * was beim Kopieren/Verschieben passieren soll,
 * wenn die Zieldatei bereits existiert.
 */

package jkcemu.file;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public class ReplyOnExistingFileDlg extends BaseDlg
{
  public static enum FileAction { COPY, MOVE };

  public static enum UserAction {
				REPLACE,
				REPLACE_ALL,
				SKIP,
				SKIP_ALL,
				RENAME,
				CANCEL };


  public static class Reply
  {
    public UserAction action;
    public Path       renamedPath;

    public Reply( UserAction action, Path renamedPath )
    {
      this.action      = action;
      this.renamedPath = renamedPath;
    }
  };


  private Reply        reply;
  private Path         dirPath;
  private Path         dstFile;
  private JLabel       labelFileName;
  private JTextField   fldFileName;
  private JRadioButton rbReplace;
  private JRadioButton rbReplaceAll;
  private JRadioButton rbSkip;
  private JRadioButton rbSkipAll;
  private JRadioButton rbRename;
  private JButton      btnOK;
  private JButton      btnCancel;


  public static Reply callDlg(
			final Window     owner,
			final Path       srcFile,
			final Path       dstFile,
			final FileAction fileAction,
			final UserAction defaultAction )
  {
    final Reply reply = new Reply( UserAction.CANCEL, dstFile );
    if( EventQueue.isDispatchThread() ) {
      (new ReplyOnExistingFileDlg(
				owner,
				srcFile,
				dstFile,
				fileAction,
				defaultAction,
				reply )).setVisible( true );
    } else {
      /*
       * Dialog im Event-Dispatch-Thread anzeigen
       * und auf das Schliessen des Dialogs warten
       */
      synchronized( reply ) {
	try {
	  EventQueue.invokeLater(
		  new Runnable()
		  {
		    @Override
		    public void run()
		    {
		      callDlgAndWakeUp(
				owner,
				srcFile,
				dstFile,
				fileAction,
				defaultAction,
				reply );
		    }
		  } );
	  reply.wait();
	}
	catch( IllegalMonitorStateException ex ) {}
	catch( InterruptedException ex ) {}
      }
    }
    return reply;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.btnOK) || (src == this.fldFileName) ) {
      rv = true;
      approveSelection();
      if( this.reply.action != UserAction.CANCEL ) {
	doClose();
      }
    }
    else if( src == this.btnCancel ) {
      rv                = true;
      this.reply.action = UserAction.CANCEL;
      doClose();
    }
    else if( (src == this.rbReplace)
	    || (src == this.rbReplaceAll)
	    || (src == this.rbSkip)
	    || (src == this.rbSkipAll)
	    || (src == this.rbRename) )
    {
      rv = true;
      updFieldsEnabled();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.rbReplace.removeActionListener( this );
      this.rbReplaceAll.removeActionListener( this );
      this.rbSkip.removeActionListener( this );
      this.rbSkipAll.removeActionListener( this );
      this.rbRename.removeActionListener( this );
      this.fldFileName.removeActionListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && this.rbRename.isSelected() )
      this.fldFileName.requestFocus();
  }


	/* --- Konstruktor --- */

  private ReplyOnExistingFileDlg(
			Window     owner,
			Path       srcFile,
			Path       dstFile,
			FileAction fileAction,
			UserAction defaultAction,
			Reply      reply )
  {
    super( owner, getTitle( fileAction ) );
    this.reply   = reply;
    this.dirPath = dstFile.getParent();
    this.dstFile = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add(
	GUIFactory.createLabel( "Am Zielort existiert bereits die Datei:" ),
	gbc );

    gbc.insets.top = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( dstFile.toString() ), gbc );

    try {
      String msg = null;
      if( Files.size( srcFile ) == Files.size( dstFile ) ) {
	FileTime srcTime = Files.getLastModifiedTime(
					srcFile,
					LinkOption.NOFOLLOW_LINKS );
	FileTime dstTime = Files.getLastModifiedTime(
					srcFile,
					LinkOption.NOFOLLOW_LINKS );
	if( (srcTime != null) && (dstTime != null) ) {
	  if( srcTime.equals( dstTime ) ) {
	    msg = "Dateigr\u00F6\u00DFe und Zeitpunkt"
			+ " der letzten \u00C4nderung sind gleich.";
	  } else {
	    msg = "Dateigr\u00F6\u00DFe ist gleich,"
			+ " Zeitpunkt der letzten \u00C4nderung aber nicht.";
	  }
	} else {
	  msg = "Die Dateigr\u00F6\u00DFe stimmt \u00FCberein.";
	}
      } else {
	msg = "Die Datei unterscheidet sich von der zu kopierenden Datei!";
      }
      if( msg != null ) {
	gbc.insets.top = 10;
	gbc.gridy++;
	add( GUIFactory.createLabel( msg ), gbc );
      }
    }
    catch( IOException ex ) {}

    gbc.insets.top = 10;
    gbc.gridy++;
    add(
	GUIFactory.createLabel(
		"Was soll mit der Datei am Zielort passieren?" ),
	gbc );

    ButtonGroup grpAction = new ButtonGroup();

    this.rbReplace = GUIFactory.createRadioButton( "Datei ersetzen" );
    grpAction.add( this.rbReplace );
    gbc.insets.top  = 5;
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.rbReplace, gbc );

    this.rbReplaceAll = GUIFactory.createRadioButton(
		"Diese und alle weiteren Dateien ersetzen" );
    grpAction.add( this.rbReplaceAll );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.rbReplaceAll, gbc );

    this.rbSkip = GUIFactory.createRadioButton(
		"Datei behalten (Vorgang \u00FCberspringen)" );
    grpAction.add( this.rbSkip );
    gbc.gridy++;
    add( this.rbSkip, gbc );

    this.rbSkipAll = GUIFactory.createRadioButton(
		"Diese und alle weiteren betreffenden Dateien behalten"
			+ " (alle betreffenden Vorg\u00E4nge"
			+ " \u00FCberspringen)" );
    grpAction.add( this.rbSkipAll );
    gbc.gridy++;
    add( this.rbSkipAll, gbc );

    if( this.dirPath != null ) {
      StringBuilder buf = new StringBuilder( 64 );
      buf.append( "Datei behalten und " );
      if( fileAction != null ) {
	switch( fileAction ) {
	  case COPY:
	    buf.append( "zu kopierende" );
	    break;
	  case MOVE:
	    buf.append( "zu verschiebende" );
	    break;
	}
      } else {
	buf.append( "neue" );
      }
      buf.append( " Datei umbenennen" );
      this.rbRename = GUIFactory.createRadioButton( buf.toString() );
      grpAction.add( this.rbRename );
      gbc.gridy++;
      add( this.rbRename, gbc );

      this.labelFileName = GUIFactory.createLabel( "Dateiname:" );
      gbc.insets.left    = 50;
      gbc.gridy++;
      add( this.labelFileName, gbc );

      this.fldFileName = GUIFactory.createTextField();
      gbc.fill         = GridBagConstraints.HORIZONTAL;
      gbc.weightx      = 1.0;
      gbc.gridy++;
      add( this.fldFileName, gbc );
    } else {
      this.rbRename      = null;
      this.labelFileName = null;
      this.fldFileName   = null;
    }

    // Vorbelegung
    boolean presetDone = false;
    if( defaultAction != null ) {
      switch( defaultAction ) {
	case REPLACE:
	  this.rbReplace.setSelected( true );
	  presetDone = true;
	  break;
	case REPLACE_ALL:
	  this.rbReplaceAll.setSelected( true );
	  presetDone = true;
	  break;
	case SKIP:
	  this.rbSkip.setSelected( true );
	  presetDone = true;
	  break;
	case SKIP_ALL:
	  this.rbSkipAll.setSelected( true );
	  presetDone = true;
	  break;
      }
    }
    if( !presetDone ) {
      if( this.rbRename != null ) {
	this.rbRename.setSelected( true );
      } else {
	this.rbSkip.setSelected( true );
      }
    }

    // Bereich Knoepfe
    JPanel panelBtn = GUIFactory.createPanel(
				new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );

    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Vorschlag fuer neuen Dateiname
    if( this.dirPath != null ) {
      Path namePath = dstFile.getFileName();
      if( namePath != null ) {
	String fileName = namePath.toString();
	if( fileName != null ) {
	  String fileExt = "";
	  int    dotPos  = fileName.lastIndexOf( '.' );
	  if( (dotPos > 0) && (dotPos < (fileName.length() - 1)) ) {
	    fileExt  = fileName.substring( dotPos );
	    fileName = fileName.substring( 0, dotPos );
	  }
	  for( int i = 1; i < 100; i++ ) {
	    String newName = String.format(
					"%s (%d)%s",
					fileName,
					i,
					fileExt );
	    Path newFile = this.dirPath.resolve( newName );
	    if( !Files.exists( newFile, LinkOption.NOFOLLOW_LINKS ) ) {
	      this.fldFileName.setText( newName );
	      break;
	    }
	  }
	}
      }
    }


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );


    // Listener
    this.rbReplace.addActionListener( this );
    this.rbReplaceAll.addActionListener( this );
    this.rbSkip.addActionListener( this );
    this.rbSkipAll.addActionListener( this );
    this.rbRename.addActionListener( this );
    this.fldFileName.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void approveSelection()
  {
    if( this.rbReplace.isSelected() ) {
      this.reply.action = UserAction.REPLACE;
    } else if( this.rbReplaceAll.isSelected() ) {
      this.reply.action = UserAction.REPLACE_ALL;
    } else if( this.rbSkip.isSelected() ) {
      this.reply.action = UserAction.SKIP;
    } else if( this.rbSkipAll.isSelected() ) {
      this.reply.action = UserAction.SKIP_ALL;
    } else {
      if( (this.dirPath != null)
	 && (this.rbRename != null)
	 && (this.fldFileName != null) )
      {
	if( this.rbRename.isSelected() ) {
	  String fileName = this.fldFileName.getText();
	  if( fileName != null ) {
	    fileName = fileName.trim();
	    this.fldFileName.setText( fileName );
	  } else {
	    fileName = "";
	  }
	  if( fileName.isEmpty() ) {
	    showErrorDlg(
		this,
		"Neuer Dateinamen nicht eingegeben" );
	  } else {
	    Path newFile = this.dirPath.resolve( fileName );
	    if( Files.exists( newFile, LinkOption.NOFOLLOW_LINKS ) ) {
	      showErrorDlg(
		this,
		"Eine Datei mit dem neuen Namen existiert bereits." );
	    } else {
	      this.reply.renamedPath = newFile;
	      this.reply.action      = UserAction.RENAME;
	    }
	  }
	}
      }
    }
  }


  private static void callDlgAndWakeUp(
				Window     owner,
				Path       srcFile,
				Path       dstFile,
				FileAction fileAction,
				UserAction defaultAction,
				Reply      reply )
  {
    synchronized( reply ) {
      (new ReplyOnExistingFileDlg(
				owner,
				srcFile,
				dstFile,
				fileAction,
				defaultAction,
				reply )).setVisible( true );
      try {
	reply.notifyAll();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }


  private static String getTitle( FileAction fileAction )
  {
    String title = "Dateiverarbeitung";
    if( fileAction != null ) {
      switch( fileAction ) {
	case COPY:
	  title = "Datei kopieren";
	  break;
	case MOVE:
	  title = "Datei verschieben";
	  break;
      }
    }
    return title;
  }


  private void updFieldsEnabled()
  {
    boolean state = false;
    if( this.rbRename != null ) {
      state = this.rbRename.isSelected();
    }
    if( this.labelFileName != null ) {
      this.labelFileName.setEnabled( state );
    }
    if( this.fldFileName != null ) {
      this.fldFileName.setEnabled( state );
    }
  }
}
