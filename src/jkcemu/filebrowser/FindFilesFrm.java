/*
 * (c) 2014-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Suchen von Dateien in einem Verzeichnis
 */

package jkcemu.filebrowser;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.lang.*;
import java.nio.file.Path;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.AbstractFileWorker;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.DirSelectDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileEntry;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;
import jkcemu.base.UserInputException;


public class FindFilesFrm
			extends BaseFrm
			implements
				AbstractFileWorker.PathListener,
				DragGestureListener,
				DragSourceListener,
				DropTargetListener,
				FileVisitor<Path>,
				FocusListener,
				ListSelectionListener,
				Runnable
{
  public static class FileEntry implements FileActionMngr.FileObject
  {
    private Path            path;
    private boolean         wasChecked;
    private FileCheckResult checkResult;

    public FileEntry( Path path )
    {
      this.path        = path;
      this.wasChecked  = false;
      this.checkResult = null;
    }

    @Override
    public File getFile()
    {
      File file = null;
      try {
	file = this.path.toFile();
      }
      catch( UnsupportedOperationException ex ) {}
      return file;
    }

    @Override
    public Path getPath()
    {
      return this.path;
    }

    @Override
    public FileCheckResult getCheckResult()
    {
      if( !this.wasChecked ) {
	this.wasChecked = true;
	try {
	  this.checkResult = FileCheckResult.checkFile( this.path.toFile() );
	}
	catch( UnsupportedOperationException ex ) {}
      }
      return this.checkResult;
    }

    @Override
    public void setPath( Path path )
    {
      this.path = path;
    }

    @Override
    public String toString()
    {
      return this.path.toString();
    }
  };


  private static final int    MAX_RESULT_ROWS = 2000;
  private static final int    MAX_ROW_LEN     = 1024;
  private static final String COPY_TEXT       = "Zeilen als Text kopieren";
  private static final String HELP_PAGE       = "/help/tools/findfiles.htm";

  private static FindFilesFrm instance      = null;
  private static DateFormat   dateFmtShort  = null;
  private static DateFormat   dateFmtMedium = null;

  private ScreenFrm                screenFrm;
  private JPopupMenu               popup;
  private JMenuItem                popupCopyText;
  private JMenuItem                mnuStart;
  private JMenuItem                mnuStop;
  private JMenuItem                mnuClose;
  private JMenuItem                mnuCopyText;
  private JMenuItem                mnuRemoveFromResult;
  private JMenuItem                mnuHelpContent;
  private JButton                  btnStartStop;
  private JButton                  btnRootDirSelect;
  private JTextField               fldRootDir;
  private JTextField               fldFileNameMask;
  private JTextField               fldFileSizeFrom;
  private JTextField               fldFileSizeTo;
  private JTextField               fldLastModified;
  private JTextField               fldLastModifiedTill;
  private JTextField               fldText;
  private JTextField               fldCurDir;
  private JCheckBox                btnSubTrees;
  private JCheckBox                btnCaseSensitive;
  private JCheckBox                btnPrintMatchedRows;
  private JLabel                   labelCurDir;
  private JList<Object>            list;
  private DefaultListModel<Object> listModel;
  private Path                     dirPath;
  private Collection<Pattern>      findFileNamePatterns;
  private Long                     findFileSizeFrom;
  private Long                     findFileSizeTo;
  private Long                     findLastModifiedFrom;
  private Long                     findLastModifiedTill;
  private String                   findText;
  private boolean                  findIgnoreCase;
  private boolean                  findPrintMatchedRows;
  private boolean                  findSubTrees;
  private FileActionMngr           fileActionMngr;
  private volatile FileVisitResult fileVisitResult;
  private volatile Thread          thread;


  public static void open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new FindFilesFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public static void open( ScreenFrm screenFrm, Path dirPath )
  {
    open( screenFrm );
    if( dirPath != null ) {
      instance.setDirectory( dirPath );
    }
  }


  public void setDirectory( Path dirPath )
  {
    if( dirPath != null ) {
      if( !Files.isDirectory( dirPath ) ) {
	dirPath = null;
      }
    }
    this.dirPath = dirPath;
    this.fldRootDir.setText( dirPath != null ? dirPath.toString() : "" );
  }


	/* --- AbstractFileWorker.PathListener --- */

  @Override
  public void pathsPasted( Set<Path> paths )
  {
    // leer
  }


  @Override
  public void pathsRemoved( Set<Path> paths )
  {
    if( paths != null ) {
      removeFromResult( paths );
      Set<Path> parents = EmuUtil.createPathSet();
      for( Path path : paths ) {
	Path parent = path.getParent();
	if( parent != null ) {
	  FileBrowserFrm.fireFileChanged( parent );
	}
      }
    }
  }


	/* --- DragGestureListener --- */

  @Override
  public void dragGestureRecognized( DragGestureEvent e )
  {
    java.util.List<FileActionMngr.FileObject> items =
		getFileObjectList( this.list.getSelectedIndices() );
    if( items != null ) {
      int n = items.size();
      if( n > 0 ) {
	Collection<File> files = new ArrayList<>( n );
	for( Object o : items ) {
	  if( o instanceof FileActionMngr.FileObject ) {
	    File file = ((FileActionMngr.FileObject) o).getFile();
	    if( file != null ) {
	      files.add( file );
	    }
	  }
	}
	if( !files.isEmpty() ) {
	  try {
	    e.startDrag( null, new FileListSelection( files ) );
	  }
	  catch( Exception ex ) {
	    BaseDlg.showErrorDlg( this, ex );
	  }
	}
      }
    }
  }


	/* --- DragSourceListener --- */

  @Override
  public void dragDropEnd( DragSourceDropEvent e )
  {
    if( e.getDropAction() == DnDConstants.ACTION_MOVE ) {
      DragSourceContext context = e.getDragSourceContext();
      if( context != null ) {
        try {
          Transferable t = context.getTransferable();
          if( t != null ) {
            if( t.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
              Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
              if( o != null ) {
                if( o instanceof Collection ) {
		  removeFromResult( (Collection) o );
                }
              }
            }
          }
        }
        catch( IOException ex ) {}
        catch( UnsupportedFlavorException ex ) {}
      }
    }
  }


  @Override
  public void dragEnter( DragSourceDragEvent e )
  {
    // leer
  }


  @Override
  public void dragExit( DragSourceEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DragSourceDragEvent e )
  {
    // leer
  }


  @Override
  public void dropActionChanged( DragSourceDragEvent e )
  {
    // leer
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( (this.thread != null) || !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    boolean done = false;
    if( (this.thread == null) && EmuUtil.isFileDrop( e ) ) {
      File file = EmuUtil.fileDrop( this, e );
      if( file != null ) {
	if( file.isDirectory() ) {
	  try {
	    setDirectory( file.toPath() );
	  }
	  catch( InvalidPathException ex ) {}
	  done = true;
	}
      }
    }
    if( done ) {
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( (this.thread != null) || !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FileVisitor --- */

  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    this.fldCurDir.setText( dir.toString() );
    if( ex != null ) {
      appendErrorToResult( dir, ex );
    }
    return this.fileVisitResult;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    return this.fileVisitResult;
  }


  @Override
  public FileVisitResult visitFile(
				final Path          file,
				BasicFileAttributes attrs )
  {
    boolean matchesFile = true;

    // Dateiname pruefen
    Collection<Pattern> fileNamePatterns = this.findFileNamePatterns;
    if( fileNamePatterns != null ) {
      if( !fileNamePatterns.isEmpty() ) {
	matchesFile   = false;
	Path namePath = file.getFileName();
	if( namePath != null ) {
	  String fileName = namePath.toString();
	  if( fileName != null ) {
	    for( Pattern fileNamePattern : fileNamePatterns ) {
	      if( fileNamePattern.matcher( fileName ).matches() ) {
		matchesFile = true;
		break;
	      }
	    }
	  }
	}
      }
    }

    // Dateigroesse pruefen
    if( matchesFile && (attrs != null)
	&& ((this.findFileSizeFrom != null)
			|| (this.findFileSizeTo != null)) )
    {
      long fileSize = attrs.size();
      if( fileSize >= 0 ) {
	Long sizeFrom = this.findFileSizeFrom;
	if( sizeFrom != null ) {
	  if( fileSize < sizeFrom ) {
	    matchesFile = false;
	  }
	}
	Long sizeTo = this.findFileSizeTo;
	if( sizeTo != null ) {
	  if( fileSize > sizeTo ) {
	    matchesFile = false;
	  }
	}
      }
    }

    // Zeitpunkt der letzten Aktualisierung pruefen
    if( matchesFile && (attrs != null)
	&& ((this.findLastModifiedFrom != null)
			|| (this.findLastModifiedTill != null)) )
    {
      FileTime lastModified = attrs.lastModifiedTime();
      if( lastModified != null ) {
	long fileMillis       = lastModified.toMillis();
	Long lastModifiedFrom = this.findLastModifiedFrom;
	if( lastModifiedFrom != null ) {
	  if( fileMillis < lastModifiedFrom.longValue() ) {
	    matchesFile = false;
	  }
	}
	Long lastModifiedTill = this.findLastModifiedTill;
	if( lastModifiedTill != null ) {
	  if( fileMillis >= lastModifiedTill.longValue() ) {
	    matchesFile = false;
	  }
	}
      }
    }

    // enthaltenen Text pruefen
    java.util.List<String> matchedRows = new ArrayList<>();
    if( matchesFile ) {
      String findText = this.findText;
      if( findText != null ) {
	int textLen = findText.length();
	if( textLen > 0 ) {
	  PushbackInputStream in     = null;
	  Reader              reader = null;
	  try {
	    /*
	     * Wird die Datei mit Files.newBufferedReader(...) geoeffnet,
	     * werden beim Lesen UnmappableCharacterException geworfen,
	     * wenn der Zeichensatz nicht passt.
	     * Aus diesem Grund erfolgt hier die Umwandlung
	     * des Byte- in einen Char-Stream mittels InputStreamReader.
	     *
	     * Eine evtl. vorhandene Byte-Order-Markierung wird ausgewertet.
	     */
	    in = new PushbackInputStream( Files.newInputStream( file ), 3 );

	    String enc   = null;
	    byte[] bom   = new byte[ 3 ];
	    int    nRead = in.read( bom );
	    if( nRead == bom.length ) {
	      if( (bom[ 0 ] == (byte) 0xEF)
		  && (bom[ 1 ] == (byte) 0xBB)
		  && (bom[ 2 ] == (byte) 0xBF) )
	      {
		enc = "UTF-8";
	      }
	      else if( (bom[ 0 ] == (byte) 0xFE)
		  && (bom[ 1 ] == (byte) 0xFF) )
	      {
		enc = "UTF-16BE";
		in.unread( bom[ 2 ] );
	      }
	      else if( (bom[ 0 ] == (byte) 0xFF)
		  && (bom[ 1 ] == (byte) 0xFE) )
	      {
		enc = "UTF-16LE";
		in.unread( bom[ 2 ] );
	      }
	    } else {
	      if( nRead > 0 ) {
		in.unread( bom, 0, Math.max( nRead, bom.length ) );
	      }
	    }
	    if( enc != null ) {
	      reader = new InputStreamReader( in, enc );
	    } else {
	      reader = new InputStreamReader( in );
	    }
	    matchesFile = false;
	    char[] buf  = new char[ textLen ];
	    if( reader.read( buf ) == textLen ) {

	      // Puffer zur Ausgabe der betreffenden Zeile anlegen
	      StringBuilder rowBuf = null;
	      if( this.findPrintMatchedRows ) {
		rowBuf = new StringBuilder( 256 );
		for( int i = 0; i < buf.length; i++ ) {
		  char ch = buf[ i ];
		  if( ch <= 0x0D ) {
		    rowBuf.setLength( 0 );
		  } else {
		    int rowLen = rowBuf.length();
		    if( rowLen < MAX_ROW_LEN ) {
		      rowBuf.append( ch );
		    } else if( rowLen == MAX_ROW_LEN ) {
		      rowBuf.append( "..." );
		    }
		  }
		}
	      }

	      // Dateiinhalt pruefen
	      boolean  matchesRow = false;
	      boolean  notEOF     = true;
	      int      begPos     = 0;
	      while( notEOF ) {
		boolean matchesBuf = true;
		for( int i = 0; i < textLen; i++ ) {
		  char ch = buf[ (begPos + i) % textLen ];
		  if( this.findIgnoreCase ) {
		    ch = Character.toUpperCase( ch );
		  }
		  if( ch != findText.charAt( i ) ) {
		    matchesBuf = false;
		    break;
		  }
		}
		if( matchesBuf ) {
		  matchesFile = true;
		  matchesRow  = true;
		  if( rowBuf == null ) {
		    /*
		     * Wenn die betroffenen Zeilen ausgegeben werden sollen,
		     * muss die Datei auch volstaendig durchgegangen werden.
		     * Deshalb hier nur abbrechen bei rowBuf == null
		     */
		    break;
		  }
		}
		int ch = reader.read();
		if( ch < 0 ) {
		  notEOF = false;
		  break;
		}
		buf[ begPos++ ] = (char) ch;
		if( begPos >= textLen ) {
		  begPos = 0;
		}
		if( rowBuf != null ) {
		  if( ch <= 0x0D ) {
		    if( matchesRow && (rowBuf.length() > 0) ) {
		      matchedRows.add( rowBuf.toString() );
		    }
		    rowBuf.setLength( 0 );
		    matchesRow = false;
		  } else {
		    int rowLen = rowBuf.length();
		    if( rowLen < MAX_ROW_LEN ) {
		      rowBuf.append( (char) ch );
		    } else if( rowLen == MAX_ROW_LEN ) {
		      rowBuf.append( "..." );
		    }
		  }
		}
	      }
	      if( rowBuf != null ) {
		if( matchesRow && (rowBuf.length() > 0) ) {
		  matchedRows.add( rowBuf.toString() );
		}
	      }
	    }
	  }
	  catch( IOException ex ) {
	    appendErrorToResult( file, ex );
	    matchesFile = false;
	  }
	  finally {
	    EmuUtil.closeSilent( reader );
	    EmuUtil.closeSilent( in );
	  }
	}
      }
    }
    if( matchesFile ) {
      final java.util.List<String> rows = matchedRows;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    appendToResult( new FileEntry( file ), rows );
		  }
		} );
    }
    return this.fileVisitResult;
  }


  @Override
  public FileVisitResult visitFileFailed( Path file, IOException ex )
  {
    if( ex != null ) {
      appendErrorToResult( file, ex );
    }
    return this.fileVisitResult;
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    // leer
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    if( !e.isTemporary() ) {
      Component c = e.getComponent();
      if( c != null ) {
	if( (c instanceof JTextField)
	    && ((c == this.fldLastModified)
		|| (c == this.fldLastModifiedTill)) )
	{
	  try {
	    parseTimestamp( (JTextField) c, null );
	  }
	  catch( UserInputException ex ) {}
	}
      }
    }
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    boolean state = (this.list.getSelectedIndex() >= 0);
    this.popupCopyText.setEnabled( state );
    this.mnuCopyText.setEnabled( state );
    this.mnuRemoveFromResult.setEnabled( state );
    this.fileActionMngr.updActionButtonsEnabled(
		getFileObjectList( this.list.getSelectedIndices() ) );
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      Path dirPath = this.dirPath;
      if( dirPath != null ) {
	this.fileVisitResult = FileVisitResult.CONTINUE;
	Files.walkFileTree(
		dirPath,
		EnumSet.noneOf( FileVisitOption.class ),
		this.findSubTrees ? Integer.MAX_VALUE : 1,
		this );
      }
    }
    catch( final Exception ex ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    showError( ex );
		  }
		} );
    }
    finally {
      synchronized( this ) {
	this.thread = null;
      }
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    searchFinished();
		  }
		} );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object src = e.getSource();
      if( src == this.btnRootDirSelect ) {
	rv = true;
	doSelectRootDir();
      }
      else if( src == this.btnStartStop ) {
	rv = true;
	if( this.thread == null ) {
	  doStart();
	} else {
	  doStop();
	}
      }
      else if( src == this.fldFileNameMask ) {
	rv = true;
	this.fldFileSizeFrom.requestFocus();
      }
      else if( src == this.fldFileSizeFrom ) {
	rv = true;
	this.fldFileSizeTo.requestFocus();
      }
      else if( src == this.fldFileSizeTo ) {
	rv = true;
	this.fldLastModified.requestFocus();
      }
      else if( src == this.fldLastModified ) {
	rv = true;
	this.fldLastModifiedTill.requestFocus();
      }
      else if( src == this.fldLastModifiedTill ) {
	rv = true;
	this.fldText.requestFocus();
      }
      else if( (src == this.fldText)
	       || (src == this.mnuStart) )
      {
	rv = true;
	doStart();
      }
      else if( src == this.mnuStop ) {
	rv = true;
	doStop();
      }
      else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( (src == this.mnuCopyText) || (src == this.popupCopyText) ) {
	rv = true;
	doCopyText();
      }
      else if( src == this.mnuRemoveFromResult ) {
	rv = true;
	doRemoveFromResult();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.open( HELP_PAGE );
      }
      else if( src == this.list ) {
	rv         = true;
	int[] rows = this.list.getSelectedIndices();
	if( rows.length == 1 ) {
	  int row = rows[ 0 ];
	  if( (row >= 0) && (row < this.listModel.getSize()) ) {
	    Object o = this.listModel.getElementAt( row );
	    if( o != null ) {
	      FileActionMngr.FileObject fObj = null;
	      if( o instanceof FindFilesCellRenderer.CodeEntry ) {
		fObj = ((FindFilesCellRenderer.CodeEntry) o).getFileObject();
	      } else if( o instanceof FileActionMngr.FileObject ) {
		fObj = (FileActionMngr.FileObject) o;
	      }
	      if( fObj != null ) {
		rv = this.fileActionMngr.doFileAction( fObj );
	      }
	    }
	  }
	}
      }
      if( !rv && (e instanceof ActionEvent) ) {
	String actionCmd = ((ActionEvent) e).getActionCommand();
	if( actionCmd != null ) {
	  int[] rows = this.list.getSelectedIndices();
	  if( rows.length > 0 ) {
	    java.util.List<FileActionMngr.FileObject> files
					= getFileObjectList( rows );
	    if( !files.isEmpty() ) {
	      switch( this.fileActionMngr.actionPerformed(
							actionCmd,
							files ) )
	      {
		case FILES_CHANGED:
		  fireFilesChanged( files );
		  rv = true;
		  break;
		case DONE:
		  rv = true;
		  break;
		case FILE_RENAMED:
		  this.list.setModel( this.listModel );
		  fireFilesChanged( files );
		  rv = true;
		  break;
	      }
	    }
	  }
	}
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = AbstractFileWorker.checkWindowClosing(
				this,
				this.fileActionMngr.getFileWorkers() );
    if( rv ) {
      rv = super.doClose();
    }
    if( rv ) {
      this.fileVisitResult = FileVisitResult.TERMINATE;
      Thread thread        = this.thread;
      if( thread != null ) {
	thread.interrupt();
      }
      instance = null;
      Main.checkQuit( this );
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    if( this.popup != null )
      SwingUtilities.updateComponentTreeUI( this.popup );
  }


  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( checkPopup( e ) ) {
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( checkPopup( e ) ) {
      e.consume();
    } else {
      super.mousePressed( e );
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( checkPopup( e ) ) {
      e.consume();
    } else {
      super.mouseReleased( e );
    }
  }


  @Override
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      this.fileVisitResult = FileVisitResult.TERMINATE;
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.fldFileNameMask != null) ) {
      this.fldFileNameMask.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private FindFilesFrm( ScreenFrm screenFrm )
  {
    setTitle( "JKCEMU Dateisuche" );
    Main.updIcon( this );
    this.screenFrm            = screenFrm;
    this.fileActionMngr       = new FileActionMngr( this, screenFrm, this );
    this.fileVisitResult      = FileVisitResult.TERMINATE;
    this.thread               = null;
    this.dirPath              = null;
    this.findFileNamePatterns = null;
    this.findFileSizeFrom     = null;
    this.findFileSizeTo       = null;
    this.findLastModifiedFrom = null;
    this.findLastModifiedTill = null;
    this.findText             = null;
    this.findIgnoreCase       = false;
    this.findPrintMatchedRows = false;
    this.findSubTrees         = false;


    // Popup-Menu
    this.popup = new JPopupMenu();

    this.popupCopyText = createJMenuItem( COPY_TEXT );
    this.popupCopyText.setEnabled( false );
    this.popup.add( this.popupCopyText );
    this.popup.addSeparator();


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuCopyText = createJMenuItem( COPY_TEXT );
    this.mnuCopyText.setEnabled( false );
    mnuEdit.add( this.mnuCopyText );
    mnuEdit.addSeparator();

    this.fileActionMngr.addCopyFileNameMenuItemsTo( this.popup, mnuEdit );
    this.fileActionMngr.addCopyFileMenuItemTo( this.popup, mnuEdit );
    this.popup.addSeparator();
    mnuEdit.addSeparator();

    this.mnuRemoveFromResult = createJMenuItem(
		"Aus Suchergebnis entfernen",
		KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    this.mnuRemoveFromResult.setEnabled( false );
    mnuEdit.add( this.mnuRemoveFromResult );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuStart = createJMenuItem( "Suche starten" );
    mnuFile.add( this.mnuStart );

    this.mnuStop = createJMenuItem( "Suche beenden" );
    mnuFile.add( this.mnuStop );
    mnuFile.addSeparator();

    if( this.screenFrm != null ) {
      this.fileActionMngr.addLoadIntoEmuMenuItemsTo( this.popup, mnuFile );
      this.popup.addSeparator();
      mnuFile.addSeparator();
    }
    this.fileActionMngr.addFileMenuItemsTo( this.popup, mnuFile );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


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

    add( new JLabel( "Suchen in:" ), gbc );

    JPanel panelRootDir = new JPanel( new GridBagLayout() );
    gbc.fill            = GridBagConstraints.HORIZONTAL;
    gbc.weightx         = 1.0;
    gbc.gridwidth       = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( panelRootDir, gbc );

    GridBagConstraints gbcRootDir = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    this.fldRootDir = new JTextField();
    this.fldRootDir.setEnabled( false );
    panelRootDir.add( this.fldRootDir, gbcRootDir );

    this.btnRootDirSelect = createImageButton(
					"/images/file/open.png",
					"Verzeichnis ausw\u00E4hlen" );
    gbcRootDir.insets  = new Insets( 0, 5, 0, 0 );
    gbcRootDir.fill    = GridBagConstraints.NONE;
    gbcRootDir.weightx = 0.0;
    gbcRootDir.gridx++;
    panelRootDir.add( this.btnRootDirSelect, gbcRootDir );

    this.btnSubTrees = new JCheckBox(
				"Unterverzeichnisse durchsuchen",
				true );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.gridx      = 1;
    gbc.gridy++;
    add( this.btnSubTrees, gbc );

    gbc.insets.top = 5;
    gbc.gridwidth  = 1;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new JLabel( "Dateinamensmaske:" ), gbc );

    this.fldFileNameMask = new JTextField();
    this.fldFileNameMask.setEnabled( false );
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx   = 1.0;
    gbc.gridx++;
    add( this.fldFileNameMask, gbc );

    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JLabel( "Dateigr\u00F6\u00DFe (Bytes):" ), gbc );

    this.fldFileSizeFrom = new JTextField();
    gbc.fill             = GridBagConstraints.HORIZONTAL;
    gbc.weightx          = 0.5;
    gbc.gridx++;
    add( this.fldFileSizeFrom, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( new JLabel( "bis:" ), gbc );

    this.fldFileSizeTo = new JTextField();
    gbc.fill           = GridBagConstraints.HORIZONTAL;
    gbc.weightx        = 0.5;
    gbc.gridx++;
    add( this.fldFileSizeTo, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( new JLabel( "Zuletzt ge\u00E4ndert am:" ), gbc );

    this.fldLastModified = new JTextField();
    gbc.fill             = GridBagConstraints.HORIZONTAL;
    gbc.weightx          = 0.5;
    gbc.gridx++;
    add( this.fldLastModified, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( new JLabel( "bis:" ), gbc );

    this.fldLastModifiedTill = new JTextField();
    gbc.fill                 = GridBagConstraints.HORIZONTAL;
    gbc.weightx              = 0.5;
    gbc.gridx++;
    add( this.fldLastModifiedTill, gbc );

    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JLabel( "Enthaltener Text:" ), gbc );

    this.fldText  = new JTextField();
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.fldText, gbc );

    this.btnCaseSensitive = new JCheckBox(
				"Gro\u00DF-/Kleinschreibung beachten",
				false );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.gridy++;
    add( this.btnCaseSensitive, gbc );

    this.btnPrintMatchedRows = new JCheckBox(
				"Gefundene Textstellen ausgeben",
				false );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnPrintMatchedRows, gbc );

    this.btnStartStop = new JButton( "Suche starten" );
    gbc.insets.top    = 10;
    gbc.gridy++;
    add( this.btnStartStop, gbc );

    this.labelCurDir = new JLabel( "Aktuell wird gesucht in:" );
    gbc.gridwidth    = 1;
    gbc.gridx        = 0;
    gbc.gridy++;
    add( this.labelCurDir, gbc );

    this.fldCurDir = new JTextField();
    this.fldCurDir.setEnabled( false );
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.fldCurDir, gbc );

    gbc.weightx = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( new JLabel( "Suchergebnis:" ), gbc );

    this.listModel = new DefaultListModel<>();
    this.list      = new JList<>( this.listModel );
    this.list.setCellRenderer( new FindFilesCellRenderer() );
    this.list.setSelectionMode(
		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.fill          = GridBagConstraints.BOTH;
    gbc.weightx       = 1.0;
    gbc.weighty       = 1.0;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( new JScrollPane( this.list ), gbc );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      this.list.setVisibleRowCount( 8 );
      pack();
      setScreenCentered();
      this.list.setVisibleRowCount( 1 );
    }
    setResizable( true );
    updFieldsEnabled();


    // Drag&Drop
    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(
					this.list,
					DnDConstants.ACTION_COPY,
					this );
    (new DropTarget( this.fldRootDir, this )).setActive( true );


    // Listener
    this.fldFileNameMask.addActionListener( this );
    this.fldFileSizeFrom.addActionListener( this );
    this.fldFileSizeTo.addActionListener( this );
    this.fldLastModified.addActionListener( this );
    this.fldLastModified.addFocusListener( this );
    this.fldLastModifiedTill.addActionListener( this );
    this.fldLastModifiedTill.addFocusListener( this );
    this.fldText.addActionListener( this );
    this.btnStartStop.addActionListener( this );
    this.list.addKeyListener( this );
    this.list.addListSelectionListener( this );
    this.list.addMouseListener( this );

    // Status der Schaltflaechen initialisieren
    this.fileActionMngr.updActionButtonsEnabled( null );

    // letztes Suchverzeichnis einstellen
    File lastDir = Main.getLastDirFile( Main.FILE_GROUP_FIND );
    if( lastDir != null ) {
      try {
	setDirectory( lastDir.toPath() );
      }
      catch( InvalidPathException ex ) {}
    }
  }


	/* --- Aktionen --- */

  private void doCopyText()
  {
    Toolkit tk = getToolkit();
    if( tk != null ) {
      try {
	Clipboard clipboard = tk.getSystemClipboard();
	if( clipboard != null ) {
	  StringBuilder buf = new StringBuilder( 0x400 );
	  synchronized( this.listModel ) {
	    int   nRows = this.listModel.getSize();
	    int[] rows  = this.list.getSelectedIndices();
	    for( int row : rows ) {
	      if( (row >= 0) && (row < nRows) ) {
		Object o = this.listModel.getElementAt( row );
		if( o != null ) {
		  String s = o.toString();
		  if( s != null ) {
		    if( !s.isEmpty() ) {
		      if( buf.length() > 0 ) {
			buf.append( (char) '\n' );
		      }
		      buf.append( s );
		    }
		  }
		}
	      }
	    }
	  }
	  if( buf.length() > 0 ) {
	    StringSelection ss = new StringSelection( buf.toString() );
	    clipboard.setContents( ss, ss );
	  }
	}
      }
      catch( IllegalStateException ex ) {}
    }
  }


  private void doSelectRootDir()
  {
    File preselection = null;
    if( this.dirPath != null ) {
      try {
	preselection = this.dirPath.toFile();
      }
      catch( UnsupportedOperationException ex ) {}
    }
    File dirFile = DirSelectDlg.selectDirectory( this, preselection );
    if( dirFile != null ) {
      try {
	setDirectory( dirFile.toPath() );
      }
      catch( InvalidPathException ex ) {}
    }
  }


  private void doStart()
  {
    if( this.thread == null ) {
      try {
	if( this.dirPath == null ) {
	  throw new UserInputException(
		"Sie m\u00FCssen im Feld \'Suchen in:\'"
			+ " ein Verzeichnis ausw\u00E4hlen\n"
			+ "welches durchsucht werden soll!" );
	}
	this.findFileNamePatterns = parseFileNameMask();
	this.findFileSizeFrom     = parseFileSize( this.fldFileSizeFrom );
	this.findFileSizeTo       = parseFileSize( this.fldFileSizeTo );

	AtomicInteger lastFieldFrom = new AtomicInteger();
	this.findLastModifiedFrom   = parseTimestamp(
						this.fldLastModified,
						lastFieldFrom );

	AtomicInteger lastFieldTill = new AtomicInteger();
	this.findLastModifiedTill   = parseTimestamp(
					this.fldLastModifiedTill,
					lastFieldTill );

	if( this.findLastModifiedTill != null ) {
	  Calendar cal = Calendar.getInstance();
	  cal.clear();
	  cal.setTimeInMillis( this.findLastModifiedTill );
	  cal.add( lastFieldTill.get(), 1 );
	  this.findLastModifiedTill = cal.getTimeInMillis();
	}
	if( (this.findLastModifiedFrom != null)
	    && (this.findLastModifiedTill == null) )
	{
	  Calendar cal = Calendar.getInstance();
	  cal.clear();
	  cal.setTimeInMillis( this.findLastModifiedFrom );
	  cal.add( lastFieldFrom.get(), 1 );
	  this.findLastModifiedTill = cal.getTimeInMillis();
	}

	String text = this.fldText.getText();
	if( text != null ) {
	  if( text.isEmpty() ) {
	    text = null;
	  }
	}

	if( this.findFileNamePatterns == null ) {
	  throw new IOException( "Sie m\u00FCssen Suchkriterien eingeben,\n"
			+ "mindestens die Dateinamensmaske!" );
	}

	this.listModel.clear();
	this.findSubTrees         = this.btnSubTrees.isSelected();
	this.findIgnoreCase       = !this.btnCaseSensitive.isSelected();
	this.findPrintMatchedRows = this.btnPrintMatchedRows.isSelected();
	this.findText             = text;
	if( (text != null) && this.findIgnoreCase ) {
	  /*
	   * Bei der Umwandlung einer ganzen Zeichenkette in Grossbuchstaben
	   * wird aus einem sz ein SS.
	   * Da das hier aber stoerend ist,
	   * werden die Zeichen einzeln umgewandelt.
	   * Dadurch bleibt ein sz ein sz.
	   */
	  char[] a = text.toCharArray();
	  if( a != null ) {
	    for( int i = 0; i < a.length; i++ ) {
	      a[ i ] = Character.toUpperCase( a[ i ] );
	    }
	    this.findText = new String( a );
	  } else {
	    this.findText = text.toUpperCase();
	  }
	}
	synchronized( this ) {
	  this.thread = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU file search" );
	  this.thread.start();
	}
	updFieldsEnabled();
	this.btnStartStop.setText( "Suche beenden" );
	try {
	  Main.setLastFile( this.dirPath.toFile(), Main.FILE_GROUP_FIND );
	}
	catch( UnsupportedOperationException ex ) {}
      }
      catch( UserInputException ex ) {
	BaseDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
      }
      catch( IOException | PatternSyntaxException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doStop()
  {
    this.fileVisitResult = FileVisitResult.TERMINATE;
    this.mnuStop.setEnabled( false );
  }


  private void doRemoveFromResult()
  {
    synchronized( this.listModel ) {
      removeFromResult( this.list.getSelectedIndices() );
    }
  }


	/* --- private Methoden --- */

  private void appendErrorToResult( Path path, IOException ex )
  {
    if( path != null ) {
      String pathText = path.toString();
      if( pathText != null ) {
	pathText = pathText.trim();
	if( !pathText.isEmpty() ) {
	  String msg = null;
	  if( ex != null ) {
	    msg = ex.getMessage();
	    if( msg != null ) {
	      msg = msg.trim();
	      if( msg.isEmpty() || msg.equalsIgnoreCase( pathText ) ) {
		msg = null;
	      }
	    }
	  }
	  if( msg != null ) {
	    appendToResult(
			new FindFilesCellRenderer.EmphasizedEntry(
				"Fehler: " + pathText + ": " + msg ),
			null );
	  } else {
	    appendToResult(
			new FindFilesCellRenderer.EmphasizedEntry(
				"Fehler: \"" + pathText
					+ "\" kann nicht gelesen werden" ),
			null );
	  }
	}
      }
    }
  }


  private void appendToResult( Object o, java.util.List<String> rows )
  {
    if( this.fileVisitResult == FileVisitResult.CONTINUE ) {
      if( o != null ) {
	synchronized( this.listModel ) {
	  this.listModel.addElement( o );
	  if( rows != null ) {
	    for( String row : rows ) {
	      this.listModel.addElement(
		new FindFilesCellRenderer.CodeEntry(
			"\u0020\u0020\u0020\u0020" + row,
			o instanceof FileEntry ? (FileEntry) o : null ) );
	    }
	  }
	  if( this.listModel.getSize() >= MAX_RESULT_ROWS ) {
	    this.listModel.addElement( "" );
	    this.listModel.addElement(
		new FindFilesCellRenderer.EmphasizedEntry(
			 "Suche abgebrochen aufgrund zu vieler"
				+ " Eintr\u00E4ge im Suchergebnis" ) );
	    doStop();
	  }
	}
      }
    }
  }


  private boolean checkPopup( MouseEvent e )
  {
    boolean status = false;
    if( e != null ) {
      if( e.isPopupTrigger() ) {
	Component c = e.getComponent();
	if( c != null ) {
	  this.popup.show( c, e.getX(), e.getY() );
	  status = true;
	}
      }
    }
    return status;
  }


  private static void fireFilesChanged(
		    java.util.List<FileActionMngr.FileObject> files )
  {
    Set<Path> paths = EmuUtil.createPathSet();
    for( FileActionMngr.FileObject f : files ) {
      Path p = f.getPath();
      if( p != null ) {
	paths.add( p );
      }
    }
    if( !paths.isEmpty() ) {
      FileBrowserFrm.fireFilesChanged( paths );
    }
  }


  /*
   * Die Methode liefert aus den in der Ergebnisliste selektierten Zeilen
   * eine Liste mit FileObjects.
   * Dabei wird ueber ein Set sichergestellt,
   * dass kein FileObject doppelt in der Ergbenisliste enthalten.
   * Wenn die Ergebnisliste auch Code-Eintraege enthaelt,
   * ist es naemlich moeglich, das gleiche FileObject mehrmals auszuwaehlen.
   */
  private java.util.List<FileActionMngr.FileObject> getFileObjectList(
								int[] rows )
  {
    int nRows = this.listModel.getSize();
    int size  = (nRows > 1 ? nRows : 1);
    java.util.List<FileActionMngr.FileObject> rv   = new ArrayList<>( size );
    Set<FileActionMngr.FileObject>            objs = new HashSet<>();
    for( int i = 0; i < rows.length; i++ ) {
      int row = rows[ i ];
      if( (row >= 0) && (row < nRows) ) {
	Object o = this.listModel.getElementAt( row );
	if( o != null ) {
	  FileActionMngr.FileObject f = null;
	  if( o instanceof FileActionMngr.FileObject ) {
	    f = (FileActionMngr.FileObject) o;
	  } else if( o instanceof FindFilesCellRenderer.CodeEntry ) {
	    f = ((FindFilesCellRenderer.CodeEntry) o).getFileObject();
	  }
	  if( f != null ) {
	    if( objs.add( f ) ) {
	      rv.add( f );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private static Long parseFileSize( JTextField fld )
					throws UserInputException
  {
    Long   rv = null;
    String s  = fld.getText();
    if( s != null ) {
      s = s.trim();
      if( !s.isEmpty() ) {
	try {
	  long v = Long.parseLong( s );
	  if( v < 0 ) {
	    throw new UserInputException(
			"Dateigr\u00F6\u00DFe kann nicht kleiner Null sein" );
	  }
	  rv = v;
	}
	catch( NumberFormatException ex ) {
	  throw new UserInputException(
			"Ung\u00FCltige Dateigr\u00F6\u00DFe" );
	}
      }
    }
    return rv;
  }


  private static Long parseTimestamp(
			JTextField    textFld,
			AtomicInteger rvLastField ) throws UserInputException
  {
    Long   rv   = null;
    String text = textFld.getText();
    if( text != null ) {
      text = text.trim();
      if( !text.isEmpty() ) {

	// Eingabe zerlegen
	String dateText = null;
	String timeText = null;
	try {
	  String[] items = text.split( "\\s" );
	  if( items != null ) {
	    for( String item : items ) {
	      if( item != null ) {
		if( !item.isEmpty() ) {
		  if( dateText == null ) {
		    dateText = item;
		  } else if( timeText == null ) {
		    timeText = item;
		  } else {
		    throw new UserInputException(
			"Ung\u00FCltige Eingabe f\u00FCr Datem/Uhrzeit" );
		  }
		}
	      }
	    }
	  }
	}
	catch( PatternSyntaxException ex ) {}
	if( dateText != null ) {

	  // Vorbereitungen zum Parsen und neu Formatieren
	  if( dateFmtShort == null ) {
	    dateFmtShort = DateFormat.getDateInstance( DateFormat.SHORT );
	  }
	  if( dateFmtMedium == null ) {
	    dateFmtMedium = DateFormat.getDateInstance( DateFormat.MEDIUM );
	  }
	  StringBuilder buf       = new StringBuilder( 20 );
	  int           lastField = Calendar.DAY_OF_MONTH;

	  // Datum parsen
	  java.util.Date date = null;
	  try {
	    date = dateFmtShort.parse( dateText );
	  }
	  catch( ParseException ex ) {}
	  if( date == null ) {
	    try {
	      date = dateFmtMedium.parse( dateText );
	    }
	    catch( ParseException ex ) {}
	  }
	  if( date == null ) {
	    throw new UserInputException( "Ung\u00FCltiges Datum" );
	  }
	  buf.append( dateFmtMedium.format( date ) );
	  rv = date.getTime();

	  // Uhrzeit parsen
	  if( timeText != null ) {
	    int hour   = -1;
	    int minute = -1;
	    int second = -1;
	    try {
	      int len = timeText.length();
	      int h1  = -1;
	      int h2  = -1;
	      int m1  = -1;
	      int m2  = -1;
	      int s1  = -1;
	      int s2  = -1;
	      if( len == 4 ) {
		h1 = 0;
		if( timeText.charAt( 1 ) == ':' ) {
		  h2 = 1;			// h:mm
		} else {
		  h2 = 2;			// hhmm
		}
		m1 = 2;
		m2 = 4;
	      } else if( len == 5 ) {
		if( timeText.charAt( 2 ) == ':' ) {
		  h1 = 0;			// hh:mm
		  h2 = 2;
		  m1 = 3;
		  m2 = 5;
		}
	      } else if( len == 6 ) {
		h1 = 0;				// hhmmss
		h2 = 2;
		m1 = 2;
		m2 = 4;
		s1 = 4;
		s2 = 6;
	      } else if( len == 7 ) {
		if( (timeText.charAt( 1 ) == ':')
		    && (timeText.charAt( 4 ) == ':') )
		{
		  h1 = 0;			// h:mm:ss
		  h2 = 1;
		  m1 = 2;
		  m2 = 4;
		  s1 = 5;
		  s2 = 7;
		}
	      } else if( len == 8 ) {
		if( (timeText.charAt( 2 ) == ':')
		    && (timeText.charAt( 5 ) == ':') )
		{
		  h1 = 0;			// hh:mm:ss
		  h2 = 2;
		  m1 = 3;
		  m2 = 5;
		  s1 = 6;
		  s2 = 8;
		}
	      }
	      if( (h1 >= 0) && (h2 > h1)
		  && (m1 >= 0) && (m2 > m1) )
	      {
		hour      = Integer.parseInt( timeText.substring( h1, h2 ) );
		minute    = Integer.parseInt( timeText.substring( m1, m2 ) );
		lastField = Calendar.MINUTE;
		buf.append( String.format( " %02d:%02d", hour, minute ) );
		if( (s1 >= 0) && (s2 > s1) ) {
		  second    = Integer.parseInt(
					timeText.substring( s1, s2 ) );
		  lastField = Calendar.SECOND;
		  buf.append( String.format( ":%02d", second ) );
		}
	      }
	    }
	    catch( NumberFormatException ex ) {
	      hour = -1;
	    }
	    if( (hour >= 0) && (hour < 24)
		&& (minute >= 0) && (minute < 60) )
	    {
	      rv = rv.longValue() + (((hour * 60) + minute) * 60000L);
	      if( (second >= 0) && (second < 60) ) {
		rv = rv.longValue() + (second * 1000L);
	      }
	    } else {
	      throw new UserInputException( "Ung\u00FCltige Uhrzeit" );
	    }
	  }
	  textFld.setText( buf.toString() );
	  if( rvLastField != null ) {
	    rvLastField.set( lastField );
	  }
	}
      }
    }
    return rv;
  }


  private void removeFromResult( Collection files )
  {
    synchronized( this.listModel ) {
      for( Object f : files ) {
	String s = f.toString();
	int    n = this.listModel.size();
	for( int i = (n - 1 ); i >= 0; --i ) {
	  if( this.listModel.get( i ).toString().equals( s ) ) {
	    this.listModel.remove( i );
	  }
	}
      }
    }
  }


  private void removeFromResult( int[] rows )
  {
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = (rows.length - 1); i >= 0; --i ) {
	  int rowNum = rows[ i ];
	  if( (rowNum >= 0) && (rowNum < this.listModel.size()) ) {
	    this.listModel.removeElementAt( rowNum );
	  }
	}
      }
    }
  }


  public void searchFinished()
  {
    this.btnStartStop.setText( "Suche starten" );
    this.fldCurDir.setText( "" );
    if( (this.fileVisitResult == FileVisitResult.CONTINUE)
	&& this.listModel.isEmpty() )
    {
      this.listModel.addElement( "Keine Datei gefunden" );
    }
    updFieldsEnabled();
  }


  private void showError( Exception ex )
  {
    BaseDlg.showErrorDlg( this, ex );
  }


  /*
   * Die Methode parst die in Eingabefeld stehende Dateimaske
   * und liefert entsprechende Pattern-Objekte zurueck.
   * Wenn kein Pattern-Objekt generiert werden konnte,
   * wird null zurueckgeliefert.
   */
  private Collection<Pattern> parseFileNameMask()
  {
    Collection<Pattern> rv   = null;
    String              mask = this.fldFileNameMask.getText();
    if( mask != null ) {
      int len = mask.length();
      if( len > 0 ) {
	StringBuilder     buf  = new StringBuilder( len );
	CharacterIterator iter = new StringCharacterIterator( mask );
	char ch = iter.first();
	while( ch != CharacterIterator.DONE ) {

	  // Trennzeichen uebergehen
	  while( (ch == '\u0020') || (ch == ',') || (ch == ';') ) {
	    ch = iter.next();
	  }

	  // zu einer Maske gehoerende Zeichen verarbeiten
	  while( (ch != CharacterIterator.DONE)
		 && (ch != '\u0020') && (ch != ',') && (ch != ';') )
	  {
	    if( ch == '\"' ) {
	      /*
	       * in doppelte Hochkommas eingeschlossene Zeichen
	       * unveraendert uebernehmen
	       */
	      ch = iter.next();
	      while( (ch != CharacterIterator.DONE) && (ch != '\"') ) {
		buf.append( ch );
		ch = iter.next();
	      }
	      if( ch == '\"' ) {
		ch = iter.next();
	      }
	    } else {
	      buf.append( ch );
	      ch = iter.next();
	    }
	  }
	  if( buf.length() > 0 ) {
	    try {
	      Pattern pattern = EmuUtil.compileFileNameMask( buf.toString() );
	      if( pattern != null ) {
		if( rv == null ) {
		  rv = new ArrayList<>();
		}
		rv.add( pattern );
	      }
	    }
	    catch( PatternSyntaxException ex ) {}
	    finally {
	      buf.setLength( 0 );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void updFieldsEnabled()
  {
    boolean stateInput   = (this.thread == null);
    boolean stateRunning = !stateInput;
    this.btnRootDirSelect.setEnabled( stateInput );
    this.fldFileNameMask.setEnabled( stateInput );
    this.fldFileSizeFrom.setEnabled( stateInput );
    this.fldFileSizeTo.setEnabled( stateInput );
    this.fldLastModified.setEnabled( stateInput );
    this.fldLastModifiedTill.setEnabled( stateInput );
    this.fldText.setEnabled( stateInput );
    this.btnCaseSensitive.setEnabled( stateInput );
    this.btnPrintMatchedRows.setEnabled( stateInput );
    this.btnSubTrees.setEnabled( stateInput );
    this.labelCurDir.setEnabled( stateRunning );
    this.fldCurDir.setEnabled( stateRunning );
    this.mnuStart.setEnabled( stateInput );
    this.mnuStop.setEnabled( stateRunning );
  }
}
