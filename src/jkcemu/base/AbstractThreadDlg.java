/*
 * (c) 2009-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstrakter Dialog fuer Aktionen,
 * die in einem separaten Thread laufen
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EventObject;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.Checksum;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import jkcemu.Main;
import jkcemu.base.PopupMenuOwner;
import jkcemu.file.FileProgressInputStream;
import jkcemu.text.LogTextActionMngr;


public abstract class AbstractThreadDlg
				extends BaseDlg
				implements PopupMenuOwner, Runnable
{
  protected boolean cancelled;
  protected int     errorCount;

  private boolean           autoClose;
  private boolean           notified;
  private Set<Path>         renamedPaths;
  private Thread            thread;
  private LogTextActionMngr actionMngr;
  private JTextArea         fldLog;
  private JProgressBar      progressBar;
  private JButton           btnClose;


  protected AbstractThreadDlg(
			Window  owner,
			String  threadName,
			boolean withProgressBar )
  {
    super( owner, Dialog.ModalityType.MODELESS );
    this.autoClose    = true;
    this.cancelled    = false;
    this.notified     = false;
    this.errorCount   = 0;
    this.renamedPaths = new TreeSet<>();
    this.thread       = new Thread(
				Main.getThreadGroup(),
				this,
				threadName );


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

    this.fldLog = GUIFactory.createTextArea( 10, 32 );
    this.fldLog.setEditable( false );
    add( GUIFactory.createScrollPane( this.fldLog ), gbc );

    if( withProgressBar ) {
      this.progressBar = GUIFactory.createProgressBar(
					JProgressBar.HORIZONTAL );
      this.progressBar.setBorderPainted( true );
      this.progressBar.setStringPainted( false );
      gbc.fill    = GridBagConstraints.HORIZONTAL;
      gbc.weighty = 0.0;
      gbc.gridy++;
      add( this.progressBar, gbc );
    } else {
      this.progressBar = null;
    }

    this.btnClose = GUIFactory.createButtonCancel();
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.weighty   = 0.0;
    gbc.gridy++;
    add( this.btnClose, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    this.fldLog.setColumns( 0 );
    this.fldLog.setRows( 0 );


    // Aktionen im Popup-Menu
    this.actionMngr = new LogTextActionMngr( this.fldLog, true );


    // Starten des Threads veranlassen
    final Thread thread = this.thread;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    thread.start();
		  }
		} );
  }


  protected void appendErrorToLog( Object errObj )
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "  Fehler" );
    if( errObj != null ) {
      String errMsg = null;
      if( errObj instanceof Exception ) {
	errMsg = ((Exception) errObj).getMessage();
      } else {
	errMsg = errObj.toString();
      }
      if( errMsg != null ) {
	if( !errMsg.isEmpty() ) {
	  buf.append( ": " );
	  buf.append( errMsg );
	}
      }
    }
    buf.append( '\n' );
    appendToLog( buf.toString() );
    this.autoClose = false;
  }


  protected void appendIgnoredToLog()
  {
    appendToLog( " Ignoriert\n" );
    this.autoClose = false;
  }


  protected void appendToLog( final String msg )
  {
    if( !this.cancelled ) {
      final JTextArea fld = this.fldLog;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    fld.append( msg );
		  }
		} );
		    
    }
  }


  protected void disableAutoClose()
  {
    this.autoClose = false;
  }


  protected abstract void doProgress();


  protected void incErrorCount()
  {
    this.errorCount++;
  }


  /*
   * Die Methode oeffnet eine Datei zum Lesen.
   * Optional kann ein Checksum-Objekt uebergeben werden.
   * In dem Fall wird die Datei erst einmal komplett gelesen
   * und an dem uebergebenen Object die Pruefsumme berechnet.
   * Anschliessend kehrt die Methode mit einem geoeffneten
   * und am Anfang der Datei stehenden InputStream-Objekt zurueck.
   * Der Fortschrittsbalken steht dann schon bei 50%.
   */
  protected FileProgressInputStream openInputFile(
					File     file,
					Checksum cks ) throws IOException
  {
    return new FileProgressInputStream( file, this.progressBar, cks );
  }


  /*
   * Die Methode erzeugt ein Dateiobjekt.
   * Dabei werden ungueltige Zeichen in Unterstriche gewandelt und
   * innerhalb des Vorgangs die Eindeutigkeit des Namens sichergestellt.
   */
  public File prepareOutFile( File outDir, String orgFileName )
  {
    boolean renamed   = false;
    char[]  nameChars = orgFileName.toCharArray();
    for( int i = 0; i < nameChars.length; i++ ) {
      char ch = nameChars[ i ];
      if( (ch < '\u0020')
	  || (ch == '\\') || (ch == '/') || (ch == ':') || (ch == '\'')
	  || ((ch > '\u007E') && (ch < '\u00A0'))
	  || (ch > '\u00FF') )
      {
	nameChars[ i ] = '_';
	renamed        = true;
      }
    }
    String usedName = String.valueOf( nameChars );
    File   file     = new File( outDir, usedName );

    /*
     * Durch das Umbenennen koennte der Dateiname innerhalb
     * des Vorgangs doppelt vorkommen.
     * Aus diesem Grund wird in diesem Fall,
     * und nur in solchen Faellen (um z.B. das bewusste uebereinander
     * Entpacken von Archiven zu ermoeglichen)
     * die Eindeutigkeit des Namens sichergstellt.
     */
    if( file.exists() && (renamed || equalsToRenamedFile( file )) ) {
      String baseName  = usedName;
      String extension = "";
      int idx = usedName.lastIndexOf( '.' );
      if( idx >= 0 ) {
	baseName  = usedName.substring( 0, idx );
	extension = usedName.substring( idx );
      }
      File tmpFile = file;
      int  counter = 1;
      do {
	usedName = String.format(
				"%s_(%d)%s",
				baseName,
				counter++,
				extension );
	tmpFile = new File( outDir, usedName );
      } while( tmpFile.exists() );
      file    = tmpFile;
      renamed = true;
    }
    if( renamed ) {
      StringBuilder buf = new StringBuilder( 256 );
      int           len = orgFileName.length();
      buf.append( '\'' );
      for( int i = 0; i < len; i++ ) {
	char ch = orgFileName.charAt( i );
	if( (ch < '\u0020')
	    || (ch == '\\') || (ch == '\'')
	    || ((ch > '\u007E') && (ch < '\u00A0'))
	    || (ch > '\u00FF') )
	{
	  buf.append( String.format( "\\u%04X", (int) ch ) );
	} else {
	  buf.append( ch );
	}
      }
      buf.append( "\' -> \'" );
      buf.append( usedName );
      buf.append( "\'\n" );
      appendToLog( buf.toString() );
      try {
	this.renamedPaths.add( file.toPath().normalize().toAbsolutePath() );
      }
      catch( InvalidPathException ex ) {}
      this.autoClose = false;
    }
    return file;
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.actionMngr.getPopupMenu();
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    doProgress();
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    progressFinished();
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.fldLog.addMouseListener( this );
      this.btnClose.addActionListener( this );
    }
  }


  @Override
  public boolean doAction( EventObject e )
  {
    boolean matched = false;
    if( e.getSource() == this.btnClose ) {
      matched        = true;
      this.cancelled = true;
      if( !this.thread.isAlive() ) {
	doClose();
      }
    }
    return matched;
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    return this.actionMngr.showPopupMenu( e );
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.fldLog.removeMouseListener( this );
      this.btnClose.removeActionListener( this );
    }
  }


	/* --- private Methoden --- */

  private boolean equalsToRenamedFile( File file )
  {
    boolean rv = false;
    try {
      rv = this.renamedPaths.contains(
			file.toPath().normalize().toAbsolutePath() );
    }
    catch( InvalidPathException ex ) {}
    return rv;
  }


  private void progressFinished()
  {
    this.btnClose.setText( EmuUtil.TEXT_CLOSE );
    if( !this.cancelled && (this.errorCount > 0) ) {
      if( this.progressBar != null ) {
	this.progressBar.setMinimum( 0 );
	this.progressBar.setMaximum( 1 );
	this.progressBar.setValue( 0 );
      }
      this.fldLog.append( "\n" );
      this.fldLog.append( Integer.toString( this.errorCount ) );
      this.fldLog.append( " Fehler\n" );
      try {
	Document doc = this.fldLog.getDocument();
	if( doc != null ) {
	  this.fldLog.setCaretPosition( doc.getLength() );
	}
      }
      catch( IllegalArgumentException ex ) {}
    } else {
      if( this.autoClose ) {
	doClose();
      }
    }
  }
}
