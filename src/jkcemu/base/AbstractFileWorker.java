/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Dateibaumoperationen
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;


public abstract class AbstractFileWorker
			implements FileVisitor<Path>, Runnable
{
  public interface PathListener
  {
    public void pathsPasted( Set<Path> files );
    public void pathsRemoved( Set<Path> files );
  };


  protected volatile Path    curParent;
  protected volatile Path    curPath;
  protected volatile boolean canceled;

  private Window                         owner;
  private PathListener                   pathListener;
  private Collection<AbstractFileWorker> register;
  private JDialog                        progressDlg;
  private JLabel                         progressLabel;
  private javax.swing.Timer              progressTimer;
  private java.util.List<Path>           paths;
  private volatile boolean               skipAll;
  private boolean                        failed;
  private Thread                         thread;
  private Object                         syncMonitor;
  private Set<Path>                      pastedPaths;
  private Set<Path>                      removedPaths;


  /*
   * Der Paramater files ist absichtlich nicht Generics-maessig
   * typisiert, um auch die von
   * Transferable.getTransferData( DataFlavor.javaFileListFlavor )
   * zurueckgelieferte Collection hier problemlos uebergeben zu koennen.
   */
  protected AbstractFileWorker(
			Window                         owner,
			Collection                     files,
			PathListener                   pathListener,
			Collection<AbstractFileWorker> register )
  {
    this.owner         = owner;
    this.pathListener  = pathListener;
    this.register      = register;
    this.paths         = null;
    this.curParent     = null;
    this.curPath       = null;
    this.progressDlg   = null;
    this.progressLabel = null;
    this.progressTimer = null;
    this.canceled      = false;
    this.failed        = false;
    this.skipAll       = false;
    this.progressDlg   = null;
    this.thread        = null;
    this.syncMonitor   = new Object();
    this.pastedPaths   = EmuUtil.createPathSet();
    this.removedPaths  = EmuUtil.createPathSet();

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
    this.canceled = true;
    setProgressText( "Vorgang abgebrochen, bitte warten..." );
    Thread thread = this.thread;
    if( thread != null ) {
      thread.interrupt();
    }
  }


  protected void checkSrcPathsAgainstDstPath(
				Path       dstPath,
				Collection files,
				String     errMsg ) throws IOException
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
    this.failed = true;
    if( !this.skipAll ) {
      StringBuilder buf = new StringBuilder( 512 );
      buf.append( getFileFailedMsg( path.toString() ) );
      if( ex != null ) {
	String msg = ex.getMessage();
	if( msg != null ) {
	  msg = msg.trim();
	  if( !msg.isEmpty() ) {
	    msg = null;
	  }
	}
	if( msg != null ) {
	  buf.append( "\n\n" );
	  buf.append( msg );
	}
      }
      switch( showJOptionPane(
			buf.toString(),
			JOptionPane.ERROR_MESSAGE,
			"Fehler",
			new String[] {
				"\u00DCberspringen",
				"Alle \u00FCberspringen",
				"Abbrechen" } ) )
      {
	case 0:
	  // leer
	  break;
	case 1:
	  this.skipAll = true;
	  break;
	default:
	  this.canceled = true;
      }
    }
  }


  /*
   * Die Methode wird im Worker-Thread aufgerufen
   * und zeigt einen JOptionPane-Dialog an.
   *
   * Rueckgabewert:
   *   Index der ausgewaehlten Option bzw. -1, wenn der Dialog
   *   am Fenster geschlossen wurde
   */
  protected int showJOptionPane(
			final String   msg,
			final int      msgType,
			final String   title,
			final String[] options )
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
	this.canceled = true;
      }
    }
    int    rv    = -1;
    Object value = pane.getValue();
    if( value != null ) {
      for( int i = 0; i < options.length; i++ ) {
	if( value.equals( options[ i ] ) ) {
	  rv = i;
	  break;
	}
      }
    }
    return rv;
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
      this.progressLabel = new JLabel( "In Arbeit..." );
      this.progressLabel.setBorder(
			BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      this.progressLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
      this.progressLabel.setAlignmentY( Component.CENTER_ALIGNMENT );

      JButton cancelBtn = new JButton( "Abbrechen" );
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

      Container contentPane = this.progressDlg.getContentPane();
      contentPane.setLayout(
		new BoxLayout( contentPane, BoxLayout.Y_AXIS ) );
      contentPane.add( this.progressLabel );
      contentPane.add( cancelBtn );

      this.progressDlg.pack();
      BasicDlg.setParentCentered( this.progressDlg );
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
    return this.canceled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    return this.canceled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    return this.canceled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult visitFileFailed( Path file, IOException ex )
  {
    return this.canceled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      if( this.paths != null ) {
	for( Path path : this.paths ) {
	  if( this.canceled ) {
	    break;
	  }
	  try {
	    this.curParent = path.getParent();
	    Files.walkFileTree( path, this );
	  }
	  catch( IOException ex ) {
	    handleError( path, ex );
	  }
	}
	this.curParent = null;
	this.curPath   = null;
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
	label.setText( text );
	dlg.pack();
      }
    }
  }


  private void updProgressDlg()
  {
    if( this.canceled ) {
      if( this.progressTimer != null ) {
	this.progressTimer.stop();
      }
    } else {
      Path path = this.curPath;
      if( path != null ) {
	setProgressText( path.toString() );
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
    if( this.failed && !this.canceled ) {
      BasicDlg.showErrorDlg( owner, getUncompletedWorkMsg() );
    }
  }
}

