/*
 * (c) 2014-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Dateibaumoperationen
 */

package jkcemu.file;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public abstract class AbstractFileWorker
			implements FileVisitor<Path>, Runnable
{
  public interface PathListener
  {
    public void pathsPasted( Set<Path> files );
    public void pathsRemoved( Set<Path> files );
  };


  protected final String OPTION_REPEAT   = "Erneut versuchen";
  protected final String OPTION_SKIP     = "\u00DCberspringen";
  protected final String OPTION_SKIP_ALL = "Alle \u00FCberspringen";

  protected Window           owner;
  protected volatile Path    curBaseParent;
  protected volatile Path    curPath;
  protected volatile String  curURLText;
  protected volatile boolean cancelled;

  private PathListener                   pathListener;
  private Collection<AbstractFileWorker> register;
  private JDialog                        progressDlg;
  private JLabel                         progressLabel;
  private Dimension                      progressLabelSize;
  private javax.swing.Timer              progressTimer;
  private java.util.List<Path>           paths;
  private volatile URLConnection         curURLCon;
  private volatile InputStream           curURLStream;
  private volatile boolean               skipAll;
  private boolean                        failed;
  private Thread                         thread;
  private Object                         syncMonitor;
  private Set<Path>                      pastedPaths;
  private Set<Path>                      removedPaths;


  protected AbstractFileWorker(
			Window                         owner,
			Collection<?>                  files,
			PathListener                   pathListener,
			Collection<AbstractFileWorker> register )
  {
    this.owner             = owner;
    this.pathListener      = pathListener;
    this.register          = register;
    this.paths             = null;
    this.curBaseParent     = null;
    this.curPath           = null;
    this.curURLCon         = null;
    this.curURLStream      = null;
    this.curURLText        = null;
    this.progressDlg       = null;
    this.progressLabel     = null;
    this.progressLabelSize = null;
    this.progressTimer     = null;
    this.cancelled         = false;
    this.failed            = false;
    this.skipAll           = false;
    this.progressDlg       = null;
    this.thread            = null;
    this.syncMonitor       = new Object();
    this.pastedPaths       = FileUtil.createPathSet();
    this.removedPaths      = FileUtil.createPathSet();

    if( files != null ) {
      int n = files.size();
      if( n > 0 ) {
	this.paths = new ArrayList<>( n );
	for( Object o : files ) {
	  if( o != null ) {
	    if( o instanceof Path ) {
	      this.paths.add( (Path) o );
	    } else if( o instanceof File ) {
	      try {
		Path path = ((File) o).toPath();
		if( path != null ) {
		  this.paths.add( path );
		}
	      }
	      catch( InvalidPathException ex ) {
		this.failed = true;
	      }
	    }
	  }
	}
      }
    }
  }


  public void cancelWork()
  {
    this.cancelled = true;
    setProgressText( "Vorgang abgebrochen, bitte warten..." );
    EmuUtil.closeSilently( this.curURLStream );
    URLConnection con = this.curURLCon;
    if( con != null ) {
      if( con instanceof HttpURLConnection ) {
	((HttpURLConnection) con).disconnect();
      }
    }
    Thread thread = this.thread;
    if( thread != null ) {
      thread.interrupt();
    }
  }


  protected void checkSrcPathsAgainstDstPath(
				Path          dstPath,
				Collection<?> files,
				String        errMsg ) throws IOException
  {
    for( Object f : files ) {
      if( f != null ) {
	try {
	  Path path = null;
	  if( f instanceof Path ) {
	    path = (Path) f;
	  } else if( f instanceof File ) {
	    path = ((File) f).toPath();
	  }
	  if( path != null ) {
	    if( dstPath.startsWith( path ) ) {
	      throw new IOException( errMsg );
	    }
	  }
	}
	catch( InvalidPathException ex ) {}
      }
    }
  }


  public static boolean checkWindowClosing(
			Component                            owner,
			final Collection<AbstractFileWorker> workers )
  {
    boolean rv = true;
    if( workers != null ) {
      if( !workers.isEmpty() ) {
	JOptionPane pane = new JOptionPane(
		"Das Fenster wird erst geschlossen, wenn die laufenden\n"
			+ "Datei-Operationen fertig sind"
			+ " oder abgebrochen wurden.\n",
		JOptionPane.WARNING_MESSAGE,
		JOptionPane.OK_OPTION );
	pane.setOptions( new String[] { "Fenster nicht schlie\u00DFen" } );
	final JDialog dlg = pane.createDialog( owner, "Hinweis" );

	final javax.swing.Timer timer = new javax.swing.Timer( 100, null );
	timer.addActionListener(
		new ActionListener()
		{
		  @Override
		  public void actionPerformed( ActionEvent e )
		  {
		    if( workers.isEmpty() ) {
		      timer.stop();
		      dlg.setVisible( false );
		      dlg.dispose();
		    }
		  }
		} );
	timer.start();

	dlg.setVisible( true );
	rv = workers.isEmpty();
      }
    }
    return rv;
  }


  protected void download( URL url, Path dstFile ) throws IOException
  {
    try {
      this.curURLCon = url.openConnection();
      this.curURLCon.setReadTimeout( 20000 );
      try {
	this.curURLStream = this.curURLCon.getInputStream();
	Files.copy(
		this.curURLStream,
		dstFile,
		StandardCopyOption.REPLACE_EXISTING );
	long millis = this.curURLCon.getLastModified();
	if( millis > 0 ) {
	  try {
	    Files.setLastModifiedTime(
				dstFile,
				FileTime.fromMillis( millis ) );
	  }
	  catch( IOException ex ) {}
	}
	this.curURLStream.close();
	this.curURLStream = null;
      }
      finally {
	EmuUtil.closeSilently( this.curURLStream );
	this.curURLStream = null;
	if( this.curURLCon instanceof HttpURLConnection ) {
	  ((HttpURLConnection) this.curURLCon).disconnect();
	}
	this.curURLCon = null;
      }
    }
    catch( IOException ex ) {
      try {
	Files.deleteIfExists( dstFile );
      }
      catch( IOException ex2 ) {}
      throw ex;
    }
  }


  protected void pathPasted( Path path )
  {
    this.pastedPaths.add( path );
  }


  protected void pathRemoved( Path path )
  {
    this.removedPaths.add( path );
  }


  public abstract String getFileFailedMsg( String fileName );
  public abstract String getProgressDlgTitle();
  public abstract String getUncompletedWorkMsg();


  protected void handleError( final Path path, final IOException ex )
  {
    handleError( path, ex, false );
  }


  /*
   * Anzeige einer Fehlermeldung mit der Auswahlmoeglichkeit,
   * wie weiter fortgefahren werden soll.
   *
   * Rueckgabewert:
   *   true:  Aktion erneut versuchen
   *   false: Aktion nicht erneut versuchen
   */
  protected boolean handleError(
			final Path        path,
			final IOException ex,
			final boolean     enableRepeat )
  {
    boolean rv  = false;
    if( this.skipAll ) {
      this.failed = true;
    } else {
      String        pName = path.toString();
      StringBuilder buf   = new StringBuilder( 512 );
      buf.append( getFileFailedMsg( pName ) );
      if( ex != null ) {
	String msg = ex.getMessage();
	if( msg != null ) {
	  if( msg.startsWith( pName ) ) {
	    msg = msg.substring( pName.length() );
	  }
	  if( msg.startsWith( ":" ) ) {
	    msg = msg.substring( 1 );
	  }
	  msg = msg.trim();
	  if( msg.isEmpty() ) {
	    msg = null;
	  }
	}
	if( msg != null ) {
	  buf.append( '\n' );
	  buf.append( msg );
	}
      }
      switch( showJOptionPane(
			buf.toString(),
			JOptionPane.ERROR_MESSAGE,
			EmuUtil.TEXT_ERROR,
			enableRepeat ?
				new String[] {
					OPTION_REPEAT,
					OPTION_SKIP,
					OPTION_SKIP_ALL,
					EmuUtil.TEXT_CANCEL }
				: new String[] {
					OPTION_SKIP,
					OPTION_SKIP_ALL,
					EmuUtil.TEXT_CANCEL } ) )
      {
	case OPTION_REPEAT:
	  rv = true;
	  break;
	case OPTION_SKIP:
	  this.failed = true;
	  break;
	case OPTION_SKIP_ALL:
	  this.failed  = true;
	  this.skipAll = true;
	  break;
	default:
	  this.failed    = true;
	  this.cancelled = true;
      }
    }
    return rv;
  }


  /*
   * Die Methode wird im Worker-Thread aufgerufen
   * und zeigt einen JOptionPane-Dialog an.
   *
   * Rueckgabewert:
   *   Ausgewaehlte Option oder null,
   *   wenn der Dialog am Fenster geschlossen wurde
   */
  protected String showJOptionPane(
			final String   msg,
			final int      msgType,
			final String   title,
			final Object[] options )
  {
    final JOptionPane pane = new JOptionPane( msg, msgType );
    pane.setOptions( options );
    synchronized( this.syncMonitor ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    pane.createDialog( owner, title ).setVisible( true );
		    synchronized( syncMonitor ) {
		      try {
			syncMonitor.notifyAll();
		      }
		      catch( IllegalMonitorStateException ex ) {}
		    }
		  }
		} );
      try {
	this.syncMonitor.wait();
      }
      catch( IllegalMonitorStateException ex1 ) {}
      catch( InterruptedException ex2 ) {
	this.cancelled = true;
      }
    }
    Object option = pane.getValue();
    return option != null ? option.toString() : null;
  }


  public void startWork()
  {
    if( (this.thread == null)
	&& (this.progressDlg == null)
	&& (this.paths != null) )
    {
      if( this.register != null ) {
	this.register.add( this );
      }
      this.progressLabel = GUIFactory.createLabel( "In Arbeit..." );
      this.progressLabel.setBorder(
		BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      this.progressLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
      this.progressLabel.setAlignmentY( Component.CENTER_ALIGNMENT );

      JButton cancelBtn = GUIFactory.createButton( EmuUtil.TEXT_CANCEL );
      cancelBtn.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      cancelBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
      cancelBtn.setAlignmentY( Component.CENTER_ALIGNMENT );
      cancelBtn.addActionListener(
			new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
			  {
			    cancelWork();
			  }
			} );

      this.progressDlg = new JDialog(
				this.owner,
				getProgressDlgTitle(),
				Dialog.ModalityType.MODELESS );

      this.progressDlg.setDefaultCloseOperation(
				WindowConstants.DO_NOTHING_ON_CLOSE );

      JPanel panel = GUIFactory.createPanel();
      panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
      panel.add( this.progressLabel );
      panel.add( cancelBtn );

      Container contentPane = this.progressDlg.getContentPane();
      contentPane.setLayout( new BorderLayout( 5, 5 ) );
      contentPane.add( panel, BorderLayout.CENTER );

      this.progressDlg.pack();
      BaseDlg.setParentCentered( this.progressDlg );
      this.progressDlg.setVisible( true );

      this.progressTimer = new javax.swing.Timer(
			100,
			new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
			  {
			    updProgressDlg();
			  }
			} );
      this.progressTimer.start();

      this.thread = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU file worker" );
      this.thread.start();
    }
  }


	/* --- FileVisistor --- */

  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult visitFileFailed( Path file, IOException ex )
  {
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      if( this.paths != null ) {
	for( Path path : this.paths ) {
	  if( this.cancelled ) {
	    break;
	  }
	  try {
	    this.curBaseParent = path.getParent();
	    Files.walkFileTree( path, this );
	  }
	  catch( IOException ex ) {
	    handleError( path, ex );
	  }
	}
	this.curBaseParent = null;
	this.curPath       = null;
	this.curURLText    = null;
      }
    }
    finally {
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    workFinished();
			  }
			} );
    }
  }


	/* --- private Methoden --- */

  private void setProgressText( String text )
  {
    if( text != null ) {
      JDialog dlg   = this.progressDlg;
      JLabel  label = this.progressLabel;
      if( (dlg != null) && (label != null) ) {
	boolean   enablePack  = true;
	label.setText( text );

	/*
	 * Die Groesse des Dialogs soll nur dann angepasst werden,
	 * wenn der neue Text in der alten Fenstergroesse
	 * nicht vollstaendig angezeigt werden kann.
	 */
	Dimension newPrefSize = label.getPreferredSize();
	if( (newPrefSize != null) && (this.progressLabelSize != null) ) {
	  if( (newPrefSize.width <= this.progressLabelSize.width)
	      && (newPrefSize.height <= this.progressLabelSize.height) )
	  {
	    enablePack = false;
	  }
	}
	if( enablePack ) {
	  dlg.pack();
	  this.progressLabelSize = label.getSize();
	}
      }
    }
  }


  private void updProgressDlg()
  {
    if( this.cancelled ) {
      if( this.progressTimer != null ) {
	this.progressTimer.stop();
      }
    } else {
      String urlText = this.curURLText;
      if( urlText != null ) {
	setProgressText( urlText );
	JDialog dlg = this.progressDlg;
	if( dlg != null ) {
	  dlg.setTitle( "Laden" );
	}
      } else {
	Path path = this.curPath;
	if( path != null ) {
	  setProgressText( path.toString() );
	}
      }
    }
  }


  private void workFinished()
  {
    if( this.register != null ) {
      this.register.remove( this );
    }
    if( this.pathListener != null ) {
      if( !this.pastedPaths.isEmpty() ) {
	this.pathListener.pathsPasted( this.pastedPaths );
      }
      if( !this.removedPaths.isEmpty() ) {
	this.pathListener.pathsRemoved( this.removedPaths );
      }
    }
    if( this.progressDlg != null ) {
      this.progressDlg.setVisible( false );
      this.progressDlg.dispose();
      this.progressDlg = null;
    }
    if( this.failed && !this.cancelled ) {
      BaseDlg.showErrorDlg( owner, getUncompletedWorkMsg() );
    }
  }
}
