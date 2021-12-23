/*
 * (c) 2014-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Suchen von Dateien in einem Verzeichnis
 */

package jkcemu.tools.findfiles;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
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
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.ComboBoxEnterActionMngr;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.ScreenFrm;
import jkcemu.base.UserInputException;
import jkcemu.file.AbstractFileWorker;
import jkcemu.file.DirSelectDlg;
import jkcemu.file.FileActionMngr;
import jkcemu.file.FileCheckResult;
import jkcemu.file.FileEntry;
import jkcemu.file.FileUtil;
import jkcemu.file.TransferableFileList;


public class FindFilesFrm
			extends BaseFrm
			implements
				AbstractFileWorker.PathListener,
				ComboBoxEnterActionMngr.EnterListener,
				DragGestureListener,
				DragSourceListener,
				DropTargetListener,
				FileVisitor<Path>,
				FocusListener,
				ListSelectionListener,
				PopupMenuOwner,
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


  public static final String TITLE = Main.APPNAME + " Dateisuche";

  private static final int    MAX_RESULT_ROWS = 2000;
  private static final int    MAX_ROW_LEN     = 1024;
  private static final String COPY_TEXT       = "Zeilen als Text kopieren";
  private static final String HELP_PAGE       = "/help/tools/findfiles.htm";

  private static final String DEFAULT_STATUS_TEXT        = "Bereit";
  private static final String TEXT_TODAY                 = "Heute";
  private static final String TEXT_YESTERDAY             = "Gestern";
  private static final String TEXT_DAY_BEFORE_YESTERDAY  = "Vorgestern";
  private static final String TEXT_STOP_SEARCH           = "Suche beenden";

  private static FindFilesFrm instance      = null;
  private static DateFormat   dateFmtShort  = null;
  private static DateFormat   dateFmtMedium = null;

  private ScreenFrm                screenFrm;
  private JPopupMenu               popupMnu;
  private JMenuItem                popupCopyText;
  private JMenuItem                mnuStart;
  private JMenuItem                mnuStop;
  private JMenuItem                mnuClose;
  private JMenuItem                mnuCopyText;
  private JMenuItem                mnuRemoveFromResult;
  private JMenuItem                mnuClearResult;
  private JMenuItem                mnuHelpContent;
  private JButton                  btnStartStop;
  private JButton                  btnDirSelect;
  private JLabel                   labelDir;
  private JComboBox<Object>        comboDir;
  private JComboBox<Object>        comboFileNameMask;
  private JComboBox<Object>        comboFileSizeFrom;
  private JComboBox<Object>        comboFileSizeTo;
  private JComboBox<Object>        comboLastModified;
  private JComboBox<Object>        comboLastModifiedTill;
  private JComboBox<Object>        comboContentPattern;
  private JTextField               fldCurDir;
  private JCheckBox                cbSubTrees;
  private JCheckBox                cbCaseSensitive;
  private JCheckBox                cbPrintMatchedRows;
  private JLabel                   labelCurDir;
  private JLabel                   labelStatus;
  private JList<Object>            list;
  private DefaultListModel<Object> listModel;
  private long                     millisStart;
  private volatile int             nFilesFound;
  private Path                     findDir;
  private boolean                  findCancelled;
  private boolean                  findIgnoreCase;
  private boolean                  findPrintMatchedRows;
  private boolean                  findSubTrees;
  private Collection<Pattern>      findFileNamePatterns;
  private Long                     findFileSizeFrom;
  private Long                     findFileSizeTo;
  private Long                     findLastModifiedFrom;
  private Long                     findLastModifiedTill;
  private String                   findContentPattern;
  private FileActionMngr           fileActionMngr;
  private volatile FileVisitResult fileVisitResult;
  private volatile Thread          thread;
  private javax.swing.Timer        timerDuration;


  public static FindFilesFrm open( ScreenFrm screenFrm )
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
    return instance;
  }


  public static FindFilesFrm open( ScreenFrm screenFrm, Path dirPath )
  {
    open( screenFrm );
    if( dirPath != null ) {
      instance.setDirectory( dirPath );
    }
    return instance;
  }


  public void setDirectory( Path dirPath )
  {
    if( dirPath != null ) {
      if( !Files.isDirectory( dirPath ) ) {
	dirPath = null;
      }
    }
    setComboItem( this.comboDir, dirPath );
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
    }
  }


	/* --- ComboBoxEnterActionMngr.EnterListener --- */

  @Override
  public void comboBoxEnterAction( JComboBox<?> combo )
  {
    if( combo == this.comboFileNameMask ) {
      this.comboFileSizeFrom.requestFocus();
    }
    else if( combo == this.comboFileSizeFrom ) {
      this.comboFileSizeTo.requestFocus();
    }
    else if( combo == this.comboFileSizeTo ) {
      this.comboLastModified.requestFocus();
    }
    else if( combo == this.comboLastModified ) {
      this.comboLastModifiedTill.requestFocus();
    }
    else if( combo == this.comboLastModifiedTill ) {
      this.comboContentPattern.requestFocus();
    }
    else if( combo == this.comboContentPattern ) {
      doStart();
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
	    e.startDrag( null, new TransferableFileList( files ) );
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
		  removeFromResult( (Collection<?>) o );
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
    if( (this.thread != null) || !FileUtil.isFileDrop( e ) )
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
    if( (this.thread == null) && FileUtil.isFileDrop( e ) ) {
      File file = FileUtil.fileDrop( this, e );
      if( file != null ) {
	if( file.isDirectory() ) {
	  try {
	    setDirectory( file.toPath() );
	  }
	  catch( InvalidPathException ex ) {}
	}
      }
      done = true;
    }
    if( !done ) {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- FileVisitor --- */

  @Override
  public FileVisitResult postVisitDirectory(
				final Path        dir,
				final IOException ex )
  {
    if( ex != null ) {
      fireAppendErrorToResult( dir, ex );
    }
    return this.fileVisitResult;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				final Path                dir,
				final BasicFileAttributes attrs )
  {
    final JTextField fldCurDir = this.fldCurDir;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    fldCurDir.setText( dir.toString() );
		  }
		} );
    if( matchesName( dir )
	&& (this.findFileSizeFrom == null)
	&& (this.findFileSizeTo == null)
	&& (this.findContentPattern == null) )
    {
      fireAppendPathToResult( dir, null );
    }
    return this.fileVisitResult;
  }


  @Override
  public FileVisitResult visitFile(
				final Path                file,
				final BasicFileAttributes attrs )
  {
    // Dateiname pruefen
    boolean matchesFile = matchesName( file );

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
      String findContentPattern = this.findContentPattern;
      if( findContentPattern != null ) {
	int patternLen = findContentPattern.length();
	if( patternLen > 0 ) {
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
	    char[] buf  = new char[ patternLen ];
	    if( reader.read( buf ) == patternLen ) {

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
		for( int i = 0; i < patternLen; i++ ) {
		  char ch = buf[ (begPos + i) % patternLen ];
		  if( this.findIgnoreCase ) {
		    ch = Character.toUpperCase( ch );
		  }
		  if( ch != findContentPattern.charAt( i ) ) {
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
		if( begPos >= patternLen ) {
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
	    fireAppendErrorToResult( file, ex );
	    matchesFile = false;
	  }
	  finally {
	    EmuUtil.closeSilently( reader );
	    EmuUtil.closeSilently( in );
	  }
	}
      }
    }
    if( matchesFile ) {
      fireAppendPathToResult( file, matchedRows );
    }
    return this.fileVisitResult;
  }


  @Override
  public FileVisitResult visitFileFailed(
				final Path        file,
				final IOException ex )
  {
    if( ex != null ) {
      fireAppendErrorToResult( file, ex );
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
	try {
	  if( c == this.comboFileNameMask ) {
	    parseFileNameMask();
	  }
	  else if( c == this.comboFileSizeFrom ) {
	    parseFileSize( this.comboFileSizeFrom );
	  }
	  else if( c == this.comboFileSizeTo ) {
	    parseFileSize( this.comboFileSizeTo );
	  }
	  else if( c == this.comboLastModified ) {
	    parseTimestamp( this.comboLastModified, null );
	  }
	  else if( c == this.comboLastModifiedTill ) {
	    parseTimestamp( this.comboLastModifiedTill, null );
	  }
	  else if( c == comboContentPattern ) {
	    // Auswahlliste vervollstaendigen
	    setComboItem(
			comboContentPattern,
			comboContentPattern.getSelectedItem() );
	  }
	}
	catch( UserInputException ex ) {}
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
    this.mnuClearResult.setEnabled( state );
    this.fileActionMngr.updActionButtonsEnabled(
		getFileObjectList( this.list.getSelectedIndices() ) );
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    this.millisStart = -1;
    this.nFilesFound = 0;
    try {
      Path findDir = this.findDir;
      if( (findDir != null) && (this.findFileNamePatterns != null) ) {
	this.millisStart     = System.currentTimeMillis();
	this.fileVisitResult = FileVisitResult.CONTINUE;
	Files.walkFileTree(
		findDir,
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
      if( src == this.btnDirSelect ) {
	rv = true;
	doSelectDir();
      }
      else if( src == this.btnStartStop ) {
	rv = true;
	if( this.thread == null ) {
	  doStart();
	} else {
	  doStop();
	}
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
      else if( src == this.mnuClearResult ) {
	rv = true;
	doClearResult();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
      }
      else if( src == this.timerDuration ) {
	rv = true;
	updStatusBar();
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
		case DONE:
		case FILES_CHANGED:
		  rv = true;
		  break;
		case FILE_RENAMED:
		  this.list.setModel( this.listModel );
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
    if( Main.isTopFrm( this ) ) {
      rv = EmuUtil.closeOtherFrames( this );
    }
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
      if( Main.isTopFrm( this ) ) {
	Main.exitSuccess();
      }
    }
    return rv;
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
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    this.fileActionMngr.updActionButtonsEnabled(
		getFileObjectList( this.list.getSelectedIndices() ) );
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state ) {
      ComboBoxEnterActionMngr.addListener( this.comboFileNameMask, this );
      ComboBoxEnterActionMngr.addListener( this.comboFileSizeFrom, this );
      ComboBoxEnterActionMngr.addListener( this.comboFileSizeTo, this );
      ComboBoxEnterActionMngr.addListener( this.comboLastModified, this );
      ComboBoxEnterActionMngr.addListener(
					this.comboLastModifiedTill,
					this );
      ComboBoxEnterActionMngr.addListener( this.comboContentPattern, this );
    }
    super.setVisible( state );
    if( !state ) {
      ComboBoxEnterActionMngr.removeListener( this.comboFileNameMask, this );
      ComboBoxEnterActionMngr.removeListener( this.comboFileSizeFrom, this );
      ComboBoxEnterActionMngr.removeListener( this.comboFileSizeTo, this );
      ComboBoxEnterActionMngr.removeListener(
					this.comboLastModified,
					this );
      ComboBoxEnterActionMngr.removeListener(
					this.comboLastModifiedTill,
					this );
      ComboBoxEnterActionMngr.removeListener(
					this.comboContentPattern,
					this );
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
    if( (e.getWindow() == this) && (this.comboFileNameMask != null) ) {
      this.comboFileNameMask.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private FindFilesFrm( ScreenFrm screenFrm )
  {
    this.screenFrm            = screenFrm;
    this.fileActionMngr       = new FileActionMngr( this, screenFrm, this );
    this.fileVisitResult      = FileVisitResult.TERMINATE;
    this.millisStart          = -1;
    this.nFilesFound          = 0;
    this.thread               = null;
    this.findDir              = null;
    this.findFileNamePatterns = null;
    this.findFileSizeFrom     = null;
    this.findFileSizeTo       = null;
    this.findLastModifiedFrom = null;
    this.findLastModifiedTill = null;
    this.findContentPattern   = null;
    this.findCancelled        = false;
    this.findIgnoreCase       = false;
    this.findPrintMatchedRows = false;
    this.findSubTrees         = false;
    setTitle( TITLE );


    // Popup-Menu
    this.popupMnu      = GUIFactory.createPopupMenu();
    this.popupCopyText = createMenuItem( COPY_TEXT );
    this.popupCopyText.setEnabled( false );
    this.popupMnu.add( this.popupCopyText );
    this.popupMnu.addSeparator();


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.mnuCopyText = createMenuItem( COPY_TEXT );
    this.mnuCopyText.setEnabled( false );
    mnuEdit.add( this.mnuCopyText );
    mnuEdit.addSeparator();

    this.fileActionMngr.addCopyFileMenuItemTo( this.popupMnu, mnuEdit );
    this.popupMnu.addSeparator();
    mnuEdit.addSeparator();
    this.fileActionMngr.addCopyFileNameMenuItemsTo(
						this.popupMnu,
						mnuEdit );
    this.popupMnu.addSeparator();
    mnuEdit.addSeparator();

    this.mnuRemoveFromResult = createMenuItemWithDirectAccelerator(
					"Aus Suchergebnis entfernen",
					KeyEvent.VK_DELETE );
    this.mnuRemoveFromResult.setEnabled( false );
    mnuEdit.add( this.mnuRemoveFromResult );

    this.mnuClearResult = createMenuItem( "Suchergebnis l\u00F6schen" );
    this.mnuClearResult.setEnabled( false );
    mnuEdit.add( this.mnuClearResult );


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuStart = createMenuItem( "Suche starten" );
    mnuFile.add( this.mnuStart );

    this.mnuStop = createMenuItem( TEXT_STOP_SEARCH );
    mnuFile.add( this.mnuStop );
    mnuFile.addSeparator();

    if( this.screenFrm != null ) {
      this.fileActionMngr.addLoadIntoEmuMenuItemsTo(
						this.popupMnu,
						mnuFile );
    }
    this.fileActionMngr.addFileMenuItemsTo( this.popupMnu, mnuFile );
    mnuFile.addSeparator();

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem( "Hilfe zur Dateisuche..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuEdit, mnuHelp ) );


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

    this.labelDir = GUIFactory.createLabel( FileUtil.LABEL_SEARCH_IN );
    add( this.labelDir, gbc );

    JPanel panelDir = GUIFactory.createPanel( new GridBagLayout() );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( panelDir, gbc );

    GridBagConstraints gbcDir = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    this.comboDir = GUIFactory.createComboBox();
    this.comboDir.setEditable( false );
    panelDir.add( this.comboDir, gbcDir );

    this.btnDirSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_DIR );
    gbcDir.insets  = new Insets( 0, 5, 0, 0 );
    gbcDir.fill    = GridBagConstraints.NONE;
    gbcDir.weightx = 0.0;
    gbcDir.gridx++;
    panelDir.add( this.btnDirSelect, gbcDir );

    this.cbSubTrees = GUIFactory.createCheckBox(
				"Unterverzeichnisse durchsuchen",
				true );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.gridx      = 1;
    gbc.gridy++;
    add( this.cbSubTrees, gbc );

    gbc.insets.top = 5;
    gbc.gridwidth  = 1;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Dateinamensmaske:" ), gbc );

    this.comboFileNameMask = GUIFactory.createComboBox();
    this.comboFileNameMask.setEditable( true );
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx   = 1.0;
    gbc.gridx++;
    add( this.comboFileNameMask, gbc );

    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Dateigr\u00F6\u00DFe (Bytes):" ), gbc );

    this.comboFileSizeFrom = GUIFactory.createComboBox();
    this.comboFileSizeFrom.setEditable( true );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    add( this.comboFileSizeFrom, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( GUIFactory.createLabel( "bis:" ), gbc );

    this.comboFileSizeTo = GUIFactory.createComboBox();
    this.comboFileSizeTo.setEditable( true );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    add( this.comboFileSizeTo, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Zuletzt ge\u00E4ndert am:" ), gbc );

    this.comboLastModified = GUIFactory.createComboBox();
    this.comboLastModified.setEditable( true );
    this.comboLastModified.addItem( "" );
    this.comboLastModified.addItem( TEXT_TODAY );
    this.comboLastModified.addItem( TEXT_YESTERDAY );
    this.comboLastModified.addItem( TEXT_DAY_BEFORE_YESTERDAY );

    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    add( this.comboLastModified, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( GUIFactory.createLabel( "bis:" ), gbc );

    this.comboLastModifiedTill = GUIFactory.createComboBox();
    this.comboLastModifiedTill.setEditable( true );
    this.comboLastModifiedTill.addItem( "" );
    this.comboLastModifiedTill.addItem( TEXT_TODAY );
    this.comboLastModifiedTill.addItem( TEXT_YESTERDAY );
    this.comboLastModifiedTill.addItem( TEXT_DAY_BEFORE_YESTERDAY );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    add( this.comboLastModifiedTill, gbc );

    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Enthaltener Text:" ), gbc );

    this.comboContentPattern = GUIFactory.createComboBox();
    this.comboContentPattern.setEditable( true );
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.comboContentPattern, gbc );

    this.cbCaseSensitive = GUIFactory.createCheckBox(
				"Gro\u00DF-/Kleinschreibung beachten" );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.gridy++;
    add( this.cbCaseSensitive, gbc );

    this.cbPrintMatchedRows = GUIFactory.createCheckBox(
				"Gefundene Textstellen ausgeben" );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.cbPrintMatchedRows, gbc );

    this.btnStartStop = GUIFactory.createButton( "Suche starten" );
    gbc.insets.top    = 10;
    gbc.gridy++;
    add( this.btnStartStop, gbc );

    this.labelCurDir = GUIFactory.createLabel( "Aktuell wird gesucht in:" );
    gbc.gridwidth    = 1;
    gbc.gridx        = 0;
    gbc.gridy++;
    add( this.labelCurDir, gbc );

    this.fldCurDir = GUIFactory.createTextField();
    this.fldCurDir.setEditable( false );
    this.fldCurDir.setEnabled( false );
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.fldCurDir, gbc );

    gbc.weightx = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Suchergebnis:" ), gbc );

    this.listModel = new DefaultListModel<>();
    this.list      = GUIFactory.createList( this.listModel );
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
    add( GUIFactory.createScrollPane( this.list ), gbc );

    this.labelStatus = GUIFactory.createLabel( DEFAULT_STATUS_TEXT );
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.weighty      = 0.0;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Font fuer JComboBoxes
    Font font = this.fldCurDir.getFont();
    if( font != null ) {
      this.comboDir.setFont( font );
      this.comboFileNameMask.setFont( font );
      this.comboFileSizeFrom.setFont( font );
      this.comboFileSizeTo.setFont( font );
      this.comboLastModified.setFont( font );
      this.comboLastModifiedTill.setFont( font );
      this.comboContentPattern.setFont( font );
    }


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      this.list.setVisibleRowCount( 8 );
      pack();
      setScreenCentered();
      this.list.setVisibleRowCount( 1 );
    }
    updFieldsEnabled();


    // Drag&Drop
    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(
					this.list,
					DnDConstants.ACTION_COPY,
					this );
    (new DropTarget( this.comboDir, this )).setActive( true );


    // Listener
    this.btnDirSelect.addActionListener( this );
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

    // Timer zur Aktualisierung der Suchdauer
    this.timerDuration = new javax.swing.Timer( 500, this );
  }


	/* --- Aktionen --- */

  private void doCopyText()
  {
    Toolkit tk = EmuUtil.getToolkit( this );
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
			buf.append( '\n' );
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


  private void doSelectDir()
  {
    File preselection = null;
    Path oldPath      = getSelectedDirPath();
    if( oldPath != null ) {
      try {
	preselection = oldPath.toFile();
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
      this.millisStart = -1;
      this.nFilesFound = 0;
      updStatusBar();
      try {
	this.findDir = getSelectedDirPath();
	if( this.findDir == null ) {
	  throw new UserInputException(
		"Sie m\u00FCssen im Feld \'"
			+ this.labelDir.getText()
			+ "\' ein Verzeichnis ausw\u00E4hlen\n"
			+ "welches durchsucht werden soll!" );
	}

	this.findFileNamePatterns = parseFileNameMask();
	if( this.findFileNamePatterns == null ) {
	  throw new UserInputException(
		"Sie m\u00FCssen eine Dateinamensmaske eingeben!" );
	}

	this.findFileSizeFrom = parseFileSize( this.comboFileSizeFrom );
	this.findFileSizeTo   = parseFileSize( this.comboFileSizeTo );

	AtomicInteger lastFieldFrom = new AtomicInteger();
	this.findLastModifiedFrom   = parseTimestamp(
					this.comboLastModified,
					lastFieldFrom );

	AtomicInteger lastFieldTill = new AtomicInteger();
	this.findLastModifiedTill   = parseTimestamp(
					this.comboLastModifiedTill,
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

	String contentPattern = null;
	Object o              = this.comboContentPattern.getSelectedItem();
	if( o != null ) {
	  contentPattern = o.toString();
	  if( contentPattern != null ) {
	    if( contentPattern.isEmpty() ) {
	      contentPattern = null;
	    }
	  }
	}

	// Auswahlliste vervollstaendigen
	setComboItem( comboContentPattern, o );

	// Suche starten
	this.listModel.clear();
	this.findCancelled        = false;
	this.findSubTrees         = this.cbSubTrees.isSelected();
	this.findIgnoreCase       = !this.cbCaseSensitive.isSelected();
	this.findPrintMatchedRows = this.cbPrintMatchedRows.isSelected();
	this.findContentPattern   = contentPattern;
	if( (contentPattern != null) && this.findIgnoreCase ) {
	  /*
	   * Bei der Umwandlung einer ganzen Zeichenkette in Grossbuchstaben
	   * wird aus einem sz ein SS.
	   * Da das hier aber stoerend ist,
	   * werden die Zeichen einzeln umgewandelt.
	   * Dadurch bleibt ein sz ein sz.
	   */
	  char[] a = contentPattern.toCharArray();
	  if( a != null ) {
	    for( int i = 0; i < a.length; i++ ) {
	      a[ i ] = Character.toUpperCase( a[ i ] );
	    }
	    this.findContentPattern = new String( a );
	  } else {
	    this.findContentPattern = contentPattern.toUpperCase();
	  }
	}
	synchronized( this ) {
	  this.thread = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU file search" );
	  this.thread.start();
	}
	this.timerDuration.start();
	updFieldsEnabled();
	this.btnStartStop.setText( TEXT_STOP_SEARCH );
	try {
	  Main.setLastFile( this.findDir.toFile(), Main.FILE_GROUP_FIND );
	}
	catch( UnsupportedOperationException ex ) {}
      }
      catch( UserInputException ex ) {
	BaseDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
      }
    }
  }


  private void doStop()
  {
    this.fileVisitResult = FileVisitResult.TERMINATE;
    this.findCancelled   = true;
    this.mnuStop.setEnabled( false );
    updStatusBar();
  }


  private void doRemoveFromResult()
  {
    synchronized( this.listModel ) {
      removeFromResult( this.list.getSelectedIndices() );
    }
  }


  private void doClearResult()
  {
    if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie das Suchergebnis l\u00F6schen?" ) )
    {
      this.mnuClearResult.setEnabled( false );
      synchronized( this.listModel ) {
	this.listModel.clear();
      }
    }
  }


	/* --- private Methoden --- */

  private void appendDurationTo( StringBuilder buf )
  {
    if( this.millisStart > 0 ) {
      long seconds = (System.currentTimeMillis() - this.millisStart) / 1000;
      long minutes = seconds / 60;
      long hours   = minutes / 60;
      if( hours > 0 ) {
	if( hours == 1 ) {
	  buf.append( String.format(
			"1:%02d:%02d Stunde",
			minutes % 60,
			seconds % 60 ) );
	} else {
	  buf.append( String.format(
			"%d:%02d:%02d Stunden",
			hours,
			minutes % 60,
			seconds % 60 ) );
	}
      } else if( minutes > 0 ) {
	if( minutes == 1 ) {
	  buf.append( String.format(
			"1:%02d Minute",
			seconds % 60 ) );
	} else {
	  buf.append( String.format(
			"%d:%02d Minuten",
			minutes,
			seconds % 60 ) );
	}
      } else {
	if( seconds == 1 ) {
	  buf.append( "1 Sekunde" );
	} else {
	  buf.append( String.format( "%d Sekunden", seconds ) );
	}
      }
    }
  }


  private void appendFoundFilesTo( StringBuilder buf )
  {
    if( this.nFilesFound == 1 ) {
      buf.append( "1 Datei/Verzeichnis" );
    } else {
      buf.append( String.format(
			"%d Dateien/Verzeichnisse",
			this.nFilesFound > 0 ? this.nFilesFound : 0 ) );
    }
    buf.append( " gefunden" );
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
	this.mnuClearResult.setEnabled( true );
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
	  this.popupMnu.show( c, e.getX(), e.getY() );
	  status = true;
	}
      }
    }
    return status;
  }


  private static long curDayWithOffset( int offset )
  {
    Calendar cal = Calendar.getInstance();
    cal.set( Calendar.AM_PM, Calendar.AM );
    cal.set( Calendar.MILLISECOND, 0 );
    cal.set( Calendar.SECOND, 0 );
    cal.set( Calendar.MINUTE, 0 );
    cal.set( Calendar.HOUR_OF_DAY, 0 );
    cal.add( Calendar.DAY_OF_MONTH, offset );
    return cal.getTimeInMillis();
  }


  private void fireAppendErrorToResult( Path path, IOException ex )
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
	    msg = "Fehler: " + pathText + ": " + msg;
	  } else {
	    msg = "Fehler: \"" + pathText + "\" kann nicht gelesen werden";
	  }
	  final String s = msg;
	  EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    appendToResult(
			new FindFilesCellRenderer.EmphasizedEntry( s ),
			null );
		  }
		} );
	}
      }
    }
  }


  private void fireAppendPathToResult(
			final Path                   file,
			final java.util.List<String> rows )
  {
    this.nFilesFound++;
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


  private Path getSelectedDirPath()
  {
    Path   rv = null;
    Object o  = this.comboDir.getSelectedItem();
    if( o != null ) {
      if( o instanceof Path ) {
	rv = (Path) o;
      } else {
	String s = o.toString();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    rv = Paths.get( s );
	  }
	}
      }
    }
    return rv;
  }


  private boolean matchesName( Path path )
  {
    boolean             rv       = false;
    Collection<Pattern> patterns = this.findFileNamePatterns;
    if( patterns != null ) {
      if( !patterns.isEmpty() ) {
	Path namePath = path.getFileName();
	if( namePath != null ) {
	  String fileName = namePath.toString();
	  if( fileName != null ) {
	    for( Pattern pattern : patterns ) {
	      if( pattern.matcher( fileName ).matches() ) {
		rv = true;
		break;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode parst die in Eingabefeld stehende Dateimaske
   * und liefert entsprechende Pattern-Objekte zurueck.
   * Wenn kein Pattern-Objekt generiert werden konnte,
   * wird null zurueckgeliefert.
   */
  private Collection<Pattern> parseFileNameMask()
  {
    Collection<Pattern> rv  = null;
    Object              o  = this.comboFileNameMask.getSelectedItem();
    if( o != null ) {
      String mask = o.toString();
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
		Pattern pattern = FileUtil.compileFileNameMask(
							buf.toString() );
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
	  setComboItem( this.comboFileNameMask, mask );
	}
      }
    }
    return rv;
  }


  private Long parseFileSize( JComboBox<Object> combo )
					throws UserInputException
  {
    Long   rv = null;
    Object o  = combo.getSelectedItem();
    if( o != null ) {
      String text = o.toString();
      if( text != null ) {
	text = text.trim();
	if( !text.isEmpty() ) {
	  try {
	    long v = Long.parseLong( text );
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
	  setComboItem( combo, rv != null ? rv.toString() : text );
	}
      }
    }
    return rv;
  }


  private Long parseTimestamp(
			JComboBox<Object> combo,
			AtomicInteger     rvLastField )
						throws UserInputException
  {
    Long   rv = null;
    Object o  = combo.getSelectedItem();
    if( o != null ) {
      String text = o.toString();
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
	    StringBuilder buf = new StringBuilder( 20 );

	    // Datum parsen
	    int lastField = Calendar.DAY_OF_MONTH;
	    if( dateText.equalsIgnoreCase( TEXT_DAY_BEFORE_YESTERDAY ) ) {
	      rv = curDayWithOffset( -2 );
	      buf.append( dateText );
	    }
	    else if( dateText.equalsIgnoreCase( TEXT_YESTERDAY ) ) {
	      rv = curDayWithOffset( -1 );
	      buf.append( dateText );
	    }
	    else if( dateText.equalsIgnoreCase( TEXT_TODAY ) ) {
	      rv = curDayWithOffset( 0 );
	      buf.append( dateText );
	    } else {
	      if( dateFmtShort == null ) {
		dateFmtShort = DateFormat.getDateInstance(
						DateFormat.SHORT );
	      }
	      if( dateFmtMedium == null ) {
		dateFmtMedium = DateFormat.getDateInstance(
						DateFormat.MEDIUM );
	      }
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
	    }

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
	    setComboItem( combo, buf.toString() );
	    if( rvLastField != null ) {
	      rvLastField.set( lastField );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void removeFromResult( Collection<?> files )
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
    this.timerDuration.stop();
    this.btnStartStop.setText( "Suche starten" );
    this.fldCurDir.setText( "" );
    if( (this.fileVisitResult == FileVisitResult.CONTINUE)
	&& this.listModel.isEmpty() )
    {
      this.listModel.addElement( "Keine Datei gefunden" );
    }
    updFieldsEnabled();
    updStatusBar();
  }


  private void setComboItem(
		JComboBox<Object> combo,
		Object            itemObj )
  {
    if( itemObj == null ) {
      itemObj = "";
    }
    String itemText = itemObj.toString();
    if( itemText == null ) {
      itemText = "";
    }
    boolean                      editable = combo.isEditable();
    DefaultComboBoxModel<Object> newModel = new DefaultComboBoxModel<>();
    if( combo.isEditable() ) {
      newModel.addElement( "" );
    }
    if( !itemText.isEmpty() ) {
      newModel.addElement( itemObj );
    }
    int n    = combo.getItemCount();
    int nMax = combo.getMaximumRowCount();
    if( (nMax > 0) && (nMax < n) ) {
      n = nMax;
    }
    for( int i = 0; i < n; i++ ) {
      Object o = combo.getItemAt( i );
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  if( !s.isEmpty() && !s.equals( itemText ) ) {
	    newModel.addElement( s );
	  }
	}
      }
    }
    combo.setModel( newModel );
    combo.setSelectedItem( itemObj );
    combo.setEditable( editable );
  }


  private void showError( Exception ex )
  {
    BaseDlg.showErrorDlg( this, ex );
  }


  private void updFieldsEnabled()
  {
    boolean stateInput   = (this.thread == null);
    boolean stateRunning = !stateInput;
    this.btnDirSelect.setEnabled( stateInput );
    this.comboFileNameMask.setEnabled( stateInput );
    this.comboFileSizeFrom.setEnabled( stateInput );
    this.comboFileSizeTo.setEnabled( stateInput );
    this.comboLastModified.setEnabled( stateInput );
    this.comboLastModifiedTill.setEnabled( stateInput );
    this.comboContentPattern.setEnabled( stateInput );
    this.cbCaseSensitive.setEnabled( stateInput );
    this.cbPrintMatchedRows.setEnabled( stateInput );
    this.cbSubTrees.setEnabled( stateInput );
    this.labelCurDir.setEnabled( stateRunning );
    this.fldCurDir.setEnabled( stateRunning );
    this.mnuStart.setEnabled( stateInput );
    this.mnuStop.setEnabled( stateRunning );
  }


  private void updStatusBar()
  {
    String text = DEFAULT_STATUS_TEXT;
    if( (this.thread != null)
	|| (this.millisStart > 0)
	|| (this.nFilesFound > 0) )
    {
      StringBuilder buf = new StringBuilder( 64 );
      if( this.thread != null ) {
	buf.append( "Suche l\u00E4uft" );
	if( this.millisStart > 0 ) {
	  buf.append( " seit " );
	  appendDurationTo( buf );
	}
	if( this.nFilesFound > 0 ) {
	  buf.append( ": " );
	  appendFoundFilesTo( buf );
	} else {
	  buf.append( "..." );
	}
      } else {
	buf.append( "Letzte Suche: " );
	if( this.millisStart > 0 ) {
	  if( this.findCancelled ) {
	    buf.append( "nach " );
	    appendDurationTo( buf );
	    buf.append( " abgebrochen" );

	  } else {
	    appendDurationTo( buf );
	  }
	  buf.append( ", " );
	}
	appendFoundFilesTo( buf );
      }
      text = buf.toString();
    }
    this.labelStatus.setText( text );
  }
}
