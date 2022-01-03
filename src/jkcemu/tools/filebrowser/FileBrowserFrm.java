/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Datei-Browser
 *
 * Aenderungen am Dateisystem werden durch Nutzung des WatchService
 * erkannt. Da aufgrund der nicht eindeutigen Implementierungsspezifika
 * des WatchService die Eindeutigkeit eines WatchKey unklar ist
 * (insbesondere auch bei symbolischen Links),
 * ist die WatchKey-Verwaltung so gestaltet,
 * dass ein WatchKey auch fuer mehrere Baumknoten in der Anzeige
 * zustaendig sein kann.
 */

package jkcemu.tools.filebrowser;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
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
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.ErrorMsg;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.ScreenFrm;
import jkcemu.file.AbstractFileWorker;
import jkcemu.file.FileActionMngr;
import jkcemu.file.FileCopier;
import jkcemu.file.FileEntry;
import jkcemu.file.FileMover;
import jkcemu.file.FileNode;
import jkcemu.file.FileNodeComparator;
import jkcemu.file.FileTableModel;
import jkcemu.file.FileTreeCellRenderer;
import jkcemu.file.FileUtil;
import jkcemu.file.TransferableFileList;
import jkcemu.text.TextUtil;
import jkcemu.tools.findfiles.FindFilesFrm;


public class FileBrowserFrm
			extends BaseFrm
			implements
				AbstractFileWorker.PathListener,
				DragGestureListener,
				DragSourceListener,
				DropTargetListener,
				FlavorListener,
				FocusListener,
				ListSelectionListener,
				PopupMenuOwner,
				TreeExpansionListener,
				TreeSelectionListener,
				TreeWillExpandListener
{
  public static final String TITLE = Main.APPNAME + " Datei-Browser";

  private static FileBrowserFrm instance = null;

  private static final String HELP_PAGE = "/help/tools/filebrowser.htm";

  private static final String ITEM_FILE_CUT
				= "Dateien/Verzeichnisse ausschneiden";
  private static final String ITEM_FILE_PASTE
				= "Dateien/Verzeichnisse einf\u00FCgen";
  private static final String ITEM_CREATE_DIR = "Verzeichnis erstellen...";
  private static final String ITEM_FIND       = "Im Verzeichnis suchen...";
  private static final String ITEM_REFRESH    = "Aktualisieren";

  private static final String PROP_SHOW_HIDDEN_FILES
				= "jkcemu.filebrowser.show_hidden_files";
  private static final String PROP_SORT_CASE_SENSITIVE
				= "jkcemu.filebrowser.sort_case_sensitive";
  private static final String PROP_PREVIEW_MAX_FILE_SIZE
				= "jkcemu.filebrowser.preview.max_file_size";
  private static final String PROP_SPLIT_POSITION
				= "jkcemu.filebrowser.split.position";

  private static final String VALUE_PREVIEW_NONE              = "no_preview";
  private static final String VALUE_PREVIEW_MAX_FILESIZE_100K = "100K";
  private static final String VALUE_PREVIEW_MAX_FILESIZE_1M   = "1M";
  private static final String VALUE_PREVIEW_MAX_FILESIZE_10M  = "10M";
  private static final String VALUE_PREVIEW_MAX_FILESIZE_100M = "100M";

  private static final int UNREACHABLE_SEARCH_PERIOD_MILLIS = 30000;

  private ScreenFrm                   screenFrm;
  private FileActionMngr              fileActionMngr;
  private Map<TreePath,WatchKey>      treePath2WatchKey;
  private Map<WatchKey,Set<TreePath>> watchKey2TreePaths;
  private WatchService                watchService;
  private javax.swing.Timer           watchTimer;
  private JMenuItem                   mnuFileCreateDir;
  private JMenuItem                   mnuFileFind;
  private JMenuItem                   mnuFileRefresh;
  private JMenuItem                   mnuFileClose;
  private JMenuItem                   mnuEditCut;
  private JMenuItem                   mnuEditPaste;
  private JMenu                       mnuSettings;
  private JCheckBoxMenuItem           mnuHiddenFiles;
  private JCheckBoxMenuItem           mnuSortCaseSensitive;
  private JRadioButtonMenuItem        mnuNoPreview;
  private JRadioButtonMenuItem        mnuPreviewMaxFileSize100K;
  private JRadioButtonMenuItem        mnuPreviewMaxFileSize1M;
  private JRadioButtonMenuItem        mnuPreviewMaxFileSize10M;
  private JRadioButtonMenuItem        mnuPreviewMaxFileSize100M;
  private JRadioButtonMenuItem        mnuPreviewNoFileSizeLimit;
  private JMenuItem                   mnuHelpContent;
  private JPopupMenu                  popupMnu;
  private JMenuItem                   popupCut;
  private JMenuItem                   popupPaste;
  private JMenuItem                   popupCreateDir;
  private JMenuItem                   popupFind;
  private JMenuItem                   popupRefresh;
  private JSplitPane                  splitPane;
  private JTable                      table;
  private JTree                       tree;
  private DefaultTreeModel            treeModel;
  private int[]                       treeDragSelectionRows;
  private ExtendedFileNode            rootNode;
  private FilePreviewFld              filePreviewFld;
  private Component                   lastActiveFld;
  private Clipboard                   clipboard;
  private java.util.List<File>        cutFiles;
  private Set<String>                 unreachableNetPaths;
  private javax.swing.Timer           unreachableRefreshTimer;
  private boolean                     ignoreFlavorEvent;
  private boolean                     pasteState;


  public static void clearPreview()
  {
    if( instance != null )
      instance.setPreviewedFileNode( null );
  }


  public static FileBrowserFrm open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new FileBrowserFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
    return instance;
  }


  public static void fireFileChanged( final Object file )
  {
    if( file != null ) {
      try {
	Path path = null;
	if( file instanceof Path ) {
	  path = (Path) file;
	} else if( file instanceof File ) {
	  path = ((File) file).toPath();
	}
	if( path != null ) {
	  if( !Files.exists( path, LinkOption.NOFOLLOW_LINKS )
	      || !Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) )
	  {
	    path = path.getParent();
	  }
	}
	if( path != null ) {
	  Frame[] frms = Frame.getFrames();
	  if( frms != null ) {
	    for( final Frame f : frms ) {
	      if( f instanceof FileBrowserFrm ) {
		((FileBrowserFrm) f).fireRefreshNodeFor( path );
	      }
	    }
	  }
	}
      }
      catch( InvalidPathException ex ) {}
    }
  }


  public static void fireFilesChanged( Collection<?> files )
  {
    if( files != null ) {
      for( Object f : files ) {
	fireFileChanged( f );
      }
    }
  }


	/* --- AbstractFileWorker.PathListener --- */

  @Override
  public void pathsPasted( Set<Path> paths )
  { 
    fireRefreshParentNodesFor( paths );
  }


  @Override
  public void pathsRemoved( Set<Path> paths )
  { 
    fireRefreshParentNodesFor( paths );
  }


	/* --- DragGestureListener --- */

  @Override
  public void dragGestureRecognized( DragGestureEvent e )
  {
    Collection<FileActionMngr.FileObject> fObjs = getSelectedFileObjects();
    if( fObjs != null ) {
      int n = fObjs.size();
      if( n > 0 ) {
	Collection<File> files = new ArrayList<>( n );
	for( FileActionMngr.FileObject fObj : fObjs ) {
	  File file = fObj.getFile();
	  if( file != null ) {
	    files.add( file );
	  }
	}
	if( !files.isEmpty() ) {
	  this.treeDragSelectionRows = this.tree.getSelectionRows();
	  try {
	    e.startDrag( null, new TransferableFileList( files ), this );
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
    this.treeDragSelectionRows = null;
    if( (e.getDropAction() & DnDConstants.ACTION_MOVE) != 0 ) {
      DragSourceContext context = e.getDragSourceContext();
      if( context != null ) {
	try {
	  Transferable t = context.getTransferable();
	  if( t != null ) {
	    if( t.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
	      Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	      if( o != null ) {
		if( o instanceof Collection ) {
		  fireRefreshParentNodesFor( (Collection) o );
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
    /*
     * Da die JTree-Komponente selbst auch ein DropTarget ist,
     * wird die Methode aufgerufen,
     * wenn man etwas aus der Komponente herauszieht.
     */
    if( this.treeDragSelectionRows != null )
      this.tree.setSelectionRows( this.treeDragSelectionRows );
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
    dragInternal( e, true );
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    dragInternal( e, false );
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    Path     dstPath  = null;
    TreeNode treeNode = null;
    int      action   = e.getDropAction();
    if( ((action == DnDConstants.ACTION_COPY)
		|| (action == DnDConstants.ACTION_MOVE))
	&& ((action & e.getSourceActions()) != 0)
	&& e.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) )
    {
      treeNode = getSelectedExtendedFileNode();
      while( treeNode != null ) {
	if( treeNode instanceof FileNode ) {
	  Path path = ((FileNode) treeNode).getPath();
	  if( path != null ) {
	    if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) ) {
	      dstPath = path;
	      break;
	    }
	  }
	}
	treeNode = treeNode.getParent();
      }
    }
    if( (dstPath != null) && (treeNode != null) ) {
      boolean success = false;
      try {

	/*
	 * Es wird hier bewusst ACTION_COPY_OR_MOVE akzeptiert
	 * und nicht nur ACTION_COPY bzw. ACTION_MOVE,
	 * da bei einer Verschiebeaktion die Drag-Quelle
	 * nicht auf die Idee kommen darf, die Quelldateien zu loeschen,
	 * da die eigentlichen Datei-Operationen asynchron ausgefuehrt
	 * werden und somit in dem Zeitpunkt noch nicht abgeschlossen sind.
	 */
	e.acceptDrop( DnDConstants.ACTION_COPY_OR_MOVE );

	// Datei-Operationen anstossen
	Transferable t = e.getTransferable();
	if( t != null ) {
	  Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof Collection ) {
	      Collection<?> files = (Collection<?>) o;

	      // Pruefung und Sicherheitsabfrage
	      boolean status    = true;
	      File    srcFile   = null;
	      int     nSrcFiles = 0;
	      int     nSrcDirs  = 0;
	      boolean hasURLs   = false;
	      for( Object f : files ) {
		if( f instanceof File ) {
		  srcFile = (File) f;
		  if( srcFile.isDirectory() ) {
		    nSrcDirs++;
		  } else {
		    nSrcFiles++;
		    String fName = srcFile.getName();
		    if( fName != null ) {
		      if( fName.toLowerCase().endsWith( ".url" ) ) {
			hasURLs = true;
		      }
		    }
		  }
		}
	      }
	      if( (nSrcDirs > 0) || (nSrcFiles > 0) ) {
		if( (action == DnDConstants.ACTION_MOVE)
		    && ((nSrcDirs + nSrcFiles) == 1)
		    && (srcFile != null) )
		{
		  File srcDir = (nSrcDirs > 0 ?
					srcFile : srcFile.getParentFile());
		  if( srcDir != null ) {
		    try {
		      if( FileUtil.equals( srcDir, dstPath.toFile() ) ) {
			/*
			 * Fehlermeldung nur anzeigen, wenn der Mauszeiger
			 * nicht mehr ueber der Quelldatei steht.
			 */
			boolean msgState = true;
			Point   point    = e.getLocation();
			if( point != null ) {
			  TreePath tp = this.tree.getPathForLocation(
								point.x,
								point.y );
			  if( tp != null ) {
			    Object o1 = tp.getLastPathComponent();
			    if( o1 != null ) {
			      if( o1 instanceof FileNode ) {
				File tmpFile = ((FileNode) o1).getFile();
				if( tmpFile != null ) {
				  if( FileUtil.equals( srcFile, tmpFile ) ) {
				    msgState = false;
				  }
				}
			      }
			    }
			  }
			}
			if( msgState ) {
			  BaseDlg.showErrorDlg(
			  	this,
			  	"Verschieben im selben Verzeichnis"
					  + " ist nicht m\u00F6glich." );
			}
			status = false;
		      }
		    }
		    catch( UnsupportedOperationException ex ) {}
		  }
		}
		if( status ) {
		  StringBuilder buf = new StringBuilder( 512 );
		  buf.append( "M\u00F6chten Sie" );
		  if( (srcFile != null)
		      && (nSrcDirs == 1)
		      && (nSrcFiles == 0) )
		  {
		    buf.append( " das Verzeichnis " );
		    buf.append( srcFile.getPath() );
		  } else if( (srcFile != null)
			     && (nSrcDirs == 0)
			     && (nSrcFiles == 1) )
		  {
		    buf.append( " die Datei " );
		    buf.append( srcFile.getPath() );
		  } else {
		    if( nSrcDirs == 1 ) {
		      buf.append( " ein Verzeichnis" );
		    } else if( nSrcDirs > 1 ) {
		      buf.append(
			String.format( " %d Verzeichnisse", nSrcDirs ) );
		    }
		    if( nSrcFiles > 0 ) {
		      if( nSrcDirs > 0 ) {
			buf.append( " und" );
		      }
		      if( nSrcFiles == 1 ) {
			buf.append( " eine Datei" );
		      } else {
			buf.append(
				String.format( " %d Dateien", nSrcFiles ) );
		      }
		    }
		  }
		  buf.append( "\nnach " );
		  buf.append( dstPath );
		  if( (nSrcDirs > 0) || (nSrcFiles > 0) ) {
		    if( action == DnDConstants.ACTION_MOVE ) {
		      buf.append( " verschieben" );
		    } else {
		      buf.append( " kopieren" );
		    }
		    if( hasURLs ) {
		      buf.append( "\nbzw. die darin enthaltene" );
		      if( nSrcDirs > 1 ) {
			buf.append( "n URLs" );
		      } else {
			buf.append( " URL" );
		      }
		      buf.append( " herunterladen" );
		    }
		  }
		  buf.append( '?' );
		  status = BaseDlg.showYesNoDlg( this, buf.toString() );
		}
		if( status ) {
		  if( action == DnDConstants.ACTION_MOVE ) {
		    (new FileMover(
			this,
			files,
			true,
			dstPath,
			this,
			this.fileActionMngr.getFileWorkers() )).startWork();
		  } else {
		    (new FileCopier(
			this,
			files,
			true,
			dstPath,
			this,
			this.fileActionMngr.getFileWorkers() )).startWork();
		  }
		  if( treeNode instanceof FileNode ) {
		    final FileNode fileNode = (FileNode) treeNode;
		    fireSelectNode( fileNode );
		  }

		  /*
		   * DropTargetDropEvent.dropComplete(...)
		   * muss in dieser Methode aufgerufen werden,
		   * damit die Erfolgsmeldung auch an die Drag-Quelle
		   * uebermittelt wird.
		   * Aus diesem Grund bleibt hier nichts anderes uebrig,
		   * als die Drop-Operation erfolgreich abzuschliessen,
		   * obwohl der eigentliche Kopier- bzw. Verschiebevorgang
		   * noch gar nicht zu Ende ist.
		   */
		  success = true;
		}
	      }
	    }
	  }
	}
      }
      catch( UnsupportedFlavorException ex ) {}
      catch( IOException ex ) {
	ErrorMsg.showLater( this, ex );
      }
      finally {
	e.dropComplete( success );
      }
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    if( e.getSource() == this.clipboard ) {
      if( this.ignoreFlavorEvent ) {
	this.ignoreFlavorEvent = false;
      } else {
	this.cutFiles.clear();
	updPasteState();
      }
    }
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    if( !e.isTemporary() ) {
      this.lastActiveFld = e.getComponent();
      updActionButtons();
    }
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    updActionButtons();
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }


	/* --- TreeExpansionListener --- */

  @Override
  public void treeCollapsed( TreeExpansionEvent e )
  {
    checkWatcherRegistration( e.getPath() );
  }


  @Override
  public void treeExpanded( TreeExpansionEvent e )
  {
    checkWatcherRegistration( e.getPath() );
  }


	/* --- TreeSelectionListener --- */

  @Override
  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      TreePath[] paths = this.tree.getSelectionPaths();
      if( paths != null ) {
	for( TreePath tp : paths ) {
	  Object o = tp.getLastPathComponent();
	  if( o != null ) {
	    if( o instanceof ExtendedFileNode ) {
	      refreshNode( (ExtendedFileNode) o );
	    }
	  }
	  checkWatcherRegistration( tp );
	}
      }
      updActionButtons();
      updPreviewFld();
    }
  }


	/* --- TreeWillExpandListener --- */

  @Override
  public void treeWillCollapse( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    // leer
  }


  @Override
  public void treeWillExpand( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    FileUtil.checkTreeWillExpand( e );

    TreePath treePath = e.getPath();
    if( treePath != null ) {
      setWaitCursor( true );
      Object o = treePath.getLastPathComponent();
      if( o != null ) {
	if( o instanceof ExtendedFileNode ) {
	  ExtendedFileNode fileNode = (ExtendedFileNode) o;
	  refreshNode( fileNode );
	  int n = fileNode.getChildCount();
	  for( int i = 0; i < n; i++ ) {
	    Object child = fileNode.getChildAt( i );
	    if( child != null ) {
	      if( child instanceof ExtendedFileNode ) {
		refreshNode( (ExtendedFileNode) child );
	      }
	    }
	  }
	}
      }
      setWaitCursor( false );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.unreachableRefreshTimer ) {
	  rv = true;
	  startFindUnreachableNetPaths();
	}
	else if( src == this.watchTimer ) {
	  rv = true;
	  checkWatcherEvents();
	}
	else if( (src == this.table) || (src == this.tree) ) {
	  rv = true;
	  doFileAction( src );
	}
	else if( (src == this.mnuFileRefresh)
		 || (src == this.popupRefresh) )
	{
	  rv = true;
	  doFileRefresh();
	}
	else if( (src == this.mnuFileCreateDir)
		 || (src == this.popupCreateDir) )
	{
	  rv = true;
	  doFileCreateDir();
	}
	else if( (src == this.mnuFileFind)
		 || (src == this.popupFind) )
	{
	  rv = true;
	  doFileFind();
	}
	else if( src == this.mnuFileClose ) {
	  rv = true;
	  doClose();
	}
	else if( (src == this.mnuEditCut)
		 || (src == this.popupCut) )
	{
	  rv = true;
	  doEditCut();
	}
	else if( (src == this.mnuEditPaste)
		 || (src == this.popupPaste) )
	{
	  rv = true;
	  doEditPaste();
	}
	else if( src == this.mnuHiddenFiles ) {
	  rv = true;
	  doSettingsHiddenFiles();
	}
	else if( src == this.mnuSortCaseSensitive ) {
	  rv = true;
	  doSettingsSortCaseSensitive();
	}
	else if( src == this.mnuHelpContent ) {
	  rv = true;
	  HelpFrm.openPage( HELP_PAGE );
	}
	if( !rv && (e instanceof ActionEvent) ) {
	  String actionCmd = ((ActionEvent) e).getActionCommand();
	  if( actionCmd != null ) {
	    if( actionCmd.equals( FileActionMngr.ACTION_COPY )
		|| actionCmd.equals( FileActionMngr.ACTION_COPY_PATH )
		|| actionCmd.equals( FileActionMngr.ACTION_COPY_URL ) )
	    {
	      this.cutFiles.clear();
	      this.ignoreFlavorEvent = false;
	      updPasteState();
	    }
	    java.util.List<FileActionMngr.FileObject> fileObjs
					      = getSelectedFileObjects();
	    if( fileObjs != null ) {
	      if( !fileObjs.isEmpty() ) {
		switch( this.fileActionMngr.actionPerformed(
							actionCmd,
							fileObjs ) )
		{
		  case DONE:
		    rv = true;
		    break;
		  case FILES_CHANGED:
		  case FILE_RENAMED:
		    for( FileActionMngr.FileObject o : fileObjs ) {
		      if( o instanceof TreeNode ) {
			this.treeModel.nodeChanged( (TreeNode) o );
		      }
		    }
		    fireUpdPreviewFld();
		    rv = true;
		    break;
		}
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
      if( Main.isTopFrm( this ) ) {
	rv = EmuUtil.closeOtherFrames( this );
	if( rv ) {
	  rv = super.doClose();
	}
	if( rv ) {
	  Main.exitSuccess();
	}
      } else {
	rv = super.doClose();
      }
    }
    return rv;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );

      EmuUtil.setProperty(
		props,
		PROP_SHOW_HIDDEN_FILES,
		this.mnuHiddenFiles.isSelected() );
      EmuUtil.setProperty(
		props,
		PROP_SORT_CASE_SENSITIVE,
		this.mnuSortCaseSensitive.isSelected() );

      String previewMaxFileSize = "unlimited";
      if( this.mnuNoPreview.isSelected() ) {
	previewMaxFileSize = "no_preview";
      } else if( this.mnuPreviewMaxFileSize100K.isSelected() ) {
	previewMaxFileSize = "100K";
      } else if( this.mnuPreviewMaxFileSize1M.isSelected() ) {
	previewMaxFileSize = "1M";
      } else if( this.mnuPreviewMaxFileSize10M.isSelected() ) {
	previewMaxFileSize = "10M";
      } else if( this.mnuPreviewMaxFileSize100M.isSelected() ) {
	previewMaxFileSize = "100M";
      }
      EmuUtil.setProperty(
		props,
		PROP_PREVIEW_MAX_FILE_SIZE,
		previewMaxFileSize );

      EmuUtil.setProperty(
		props,
		PROP_SPLIT_POSITION,
		this.splitPane.getDividerLocation() );
    }
  }


  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( checkPopup( e ) ) {
      e.consume();
    } else {
      boolean done = false;
      if( (e.getClickCount() > 1) && (e.getButton() == MouseEvent.BUTTON1) ) {
	Component c = e.getComponent();
	if( c != null ) {
	  if( (c == this.tree) || (c == this.table) ) {
	    try {
	      doFileAction( c );
	      done = true;
	    }
	    catch( IOException ex ) {
	      BaseDlg.showErrorDlg( this, ex );
	    }
	    e.consume();
	  }
	}
      }
      if( !done ) {
	super.mouseClicked( e );
      }
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
    updActionButtons();
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state != isVisible() ) {
      if( !state ) {
	this.unreachableRefreshTimer.stop();
	deactivateWatchService();
      }
      super.setVisible( state );
      if( state ) {
	this.unreachableRefreshTimer.start();
	activateWatchService();
      }
    }
  }


	/* --- Aktionen --- */

  private void doEditCut() throws IOException
  {
    this.cutFiles.clear();

    java.util.List<FileActionMngr.FileObject> fObjs
					= getSelectedFileObjects();
    if( fObjs != null ) {
      int n = fObjs.size();
      if( n > 0 ) {
	for( FileActionMngr.FileObject fObj : fObjs ) {
	  File file = fObj.getFile();
	  if( file != null ) {
	    this.cutFiles.add( file );
	  }
	}
      }
    }

    // Zwischenablage leeren
    try {
      if( this.clipboard != null ) {
	this.ignoreFlavorEvent = true;
	StringSelection ss = new StringSelection( "" );
	this.clipboard.setContents( ss, ss );
      }
    }
    catch( IllegalStateException ex ) {}

    // Einfuegen ermoeglichen
    updPasteState();
  }


  private void doEditPaste()
  {
    Path             dstPath  = null;
    ExtendedFileNode fileNode = getSelectedExtendedFileNode();
    if( fileNode != null ) {
      Path path = fileNode.getPath();
      if( path != null ) {
	if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS) ) {
	  dstPath = path;
	}
      }
    }
    if( dstPath != null ) {
      try {
	if( this.cutFiles.isEmpty() ) {
	  if( this.clipboard != null ) {
	    Object o = this.clipboard.getData(
				DataFlavor.javaFileListFlavor );
	    if( o != null ) {
	      Collection<?> files = null;
	      if( o instanceof File ) {
		files = Collections.singletonList( (File) o );
	      } else if( o instanceof Collection ) {
		files = (Collection) o;
	      }
	      if( !files.isEmpty() ) {
		(new FileCopier(
			this,
			files,
			false,
			dstPath,
			this,
			this.fileActionMngr.getFileWorkers() )).startWork();
	      }
	    }
	  }
	} else {
	  (new FileMover(
			this,
			this.cutFiles,
			false,
			dstPath,
			this,
			this.fileActionMngr.getFileWorkers() )).startWork();
	}
      }
      catch( IllegalStateException ex ) {}
      catch( UnsupportedFlavorException ex ) {}
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    } else {
      BaseDlg.showErrorDlg(
		this,
		"Kein Verzeichnis ausgew\u00E4hlt,"
			+ " in das eingef\u00FCgt werden soll" );
    }
  }


  private void doFileAction( Object src ) throws IOException
  {
    ExtendedFileNode fileNode = getSelectedExtendedFileNode();
    if( fileNode != null ) {
      if( (src == this.table) && !fileNode.isLeaf() ) {
	TreeNode[] path = this.treeModel.getPathToRoot( fileNode );
	if( path != null ) {
	  setWaitCursor( true );
	  refreshNode( fileNode );
	  fireSelectNode( fileNode );
	  setWaitCursor( false );
	}
      } else {
	this.fileActionMngr.doFileAction( fileNode );
      }
    }
  }


  private void doFileCreateDir()
  {
    ExtendedFileNode fileNode = getSelectedExtendedFileNode();
    if( fileNode != null ) {
      TreeNode parent = fileNode;
      File     file   = fileNode.getFile();
      if( file != null ) {
	if( !file.isDirectory() ) {
	  file   = file.getParentFile();
	  parent = fileNode.getParent();
	}
      }
      if( (parent != null) && (file != null) ) {
	File dirFile = FileUtil.createDir( this, file );
	if( dirFile != null ) {
	  try {
	    this.rootNode.refreshNodeFor(
				file.toPath(),
				this.treeModel,
				this.mnuHiddenFiles.isSelected(),
				getFileNodeComparator() );
	  }
	  catch( InvalidPathException ex ) {
	    if( parent instanceof ExtendedFileNode ) {
	      refreshNode( (ExtendedFileNode) parent );
	    }
	  }
	  updActionButtons();
	  fireSelectChildNode( parent, dirFile );
	}
      }
    }
  }

  private void doFileRefresh()
  {
    ExtendedFileNode fileNode = getSelectedExtendedFileNode();
    if( fileNode != null ) {
      refreshNode( fileNode );
    } else {
      refreshNode( this.rootNode );
    }
    updActionButtons();
  }


  private void doFileFind()
  {
    Path             path     = null;
    ExtendedFileNode fileNode = getSelectedExtendedFileNode();
    if( fileNode != null ) {
      path = fileNode.getPath();
    }
    FindFilesFrm.open( this.screenFrm, path );
  }


  private void doSettingsHiddenFiles()
  {
    EmuUtil.setProperty(
		Main.getProperties(),
		PROP_SHOW_HIDDEN_FILES,
		this.mnuHiddenFiles.isSelected() );
    refreshNode( this.rootNode );
  }


  private void doSettingsSortCaseSensitive()
  {
    EmuUtil.setProperty(
		Main.getProperties(),
		PROP_SORT_CASE_SENSITIVE,
		this.mnuSortCaseSensitive.isSelected() );
    refreshNode( this.rootNode );
  }


	/* --- Konstruktor --- */

  private FileBrowserFrm( ScreenFrm screenFrm )
  {
    this.screenFrm             = screenFrm;
    this.fileActionMngr        = new FileActionMngr( this, screenFrm, this );
    this.ignoreFlavorEvent     = false;
    this.pasteState            = false;
    this.treePath2WatchKey     = new HashMap<>();
    this.watchKey2TreePaths    = new HashMap<>();
    this.watchService          = null;
    this.watchTimer            = null;
    this.lastActiveFld         = null;
    this.treeDragSelectionRows = null;
    this.clipboard             = null;
    this.cutFiles              = new ArrayList<>();
    this.unreachableNetPaths   = startFindUnreachableNetPaths();
    setTitle( TITLE );


    // Popup-Menu
    this.popupMnu = GUIFactory.createPopupMenu();


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.fileActionMngr.addCopyFileNameMenuItemsTo( this.popupMnu, mnuEdit );
    mnuEdit.addSeparator();
    this.popupMnu.addSeparator();

    this.mnuEditCut = createMenuItem( ITEM_FILE_CUT );
    mnuEdit.add( this.mnuEditCut );

    this.popupCut = createMenuItem( ITEM_FILE_CUT );
    this.popupMnu.add( this.popupCut );

    this.fileActionMngr.addCopyFileMenuItemTo( this.popupMnu, mnuEdit );

    this.mnuEditPaste = createMenuItem( ITEM_FILE_PASTE );
    mnuEdit.add( this.mnuEditPaste );

    this.popupPaste = createMenuItem( ITEM_FILE_PASTE );
    this.popupMnu.add( this.popupPaste );
    this.popupMnu.addSeparator();


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    if( this.screenFrm != null ) {
      this.fileActionMngr.addLoadIntoEmuMenuItemsTo(
						this.popupMnu,
						mnuFile );
    }
    this.fileActionMngr.addFileMenuItemsTo( this.popupMnu, mnuFile );
    this.popupMnu.addSeparator();
    mnuFile.addSeparator();

    this.mnuFileCreateDir = createMenuItem( ITEM_CREATE_DIR );
    mnuFile.add( this.mnuFileCreateDir );

    this.popupCreateDir = createMenuItem( ITEM_CREATE_DIR );
    this.popupMnu.add( this.popupCreateDir );

    this.mnuFileFind = createMenuItem( ITEM_FIND );
    mnuFile.add( this.mnuFileFind );
    mnuFile.addSeparator();

    this.popupFind = createMenuItem( ITEM_FIND );
    this.popupMnu.add( this.popupFind );
    this.popupMnu.addSeparator();

    this.mnuFileRefresh = createMenuItem( ITEM_REFRESH );
    mnuFile.add( this.mnuFileRefresh );
    mnuFile.addSeparator();

    this.popupRefresh = createMenuItem( ITEM_REFRESH );
    this.popupMnu.add( this.popupRefresh );

    this.mnuFileClose = createMenuItemClose();
    mnuFile.add( this.mnuFileClose );


    // Menu Einstellungen
    this.mnuSettings = GUIFactory.createMenu( EmuUtil.TEXT_SETTINGS );
    this.mnuSettings.setMnemonic( KeyEvent.VK_E );

    this.mnuHiddenFiles = GUIFactory.createCheckBoxMenuItem(
		"Versteckte Dateien anzeigen",
		Main.getBooleanProperty( PROP_SHOW_HIDDEN_FILES, false ) );
    this.mnuHiddenFiles.addActionListener( this );
    this.mnuSettings.add( this.mnuHiddenFiles );

    this.mnuSortCaseSensitive = GUIFactory.createCheckBoxMenuItem(
		"Gro\u00DF-/Kleinschreibung bei Sortierung beachten",
		Main.getBooleanProperty( PROP_SORT_CASE_SENSITIVE, false ) );
    this.mnuSortCaseSensitive.addActionListener( this );
    this.mnuSettings.add( this.mnuSortCaseSensitive );
    this.mnuSettings.addSeparator();

    JMenu mnuSettingsPreview = GUIFactory.createMenu(
			"Max. Dateigr\u00F6\u00DFe f\u00FCr Vorschau" );
    this.mnuSettings.add( mnuSettingsPreview );

    ButtonGroup grpPreviewMaxFileSize = new ButtonGroup();

    this.mnuNoPreview = GUIFactory.createRadioButtonMenuItem(
							"Keine Vorschau" );
    this.mnuNoPreview.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuNoPreview );
    mnuSettingsPreview.add( this.mnuNoPreview );

    this.mnuPreviewMaxFileSize100K = GUIFactory.createRadioButtonMenuItem(
							"100 KByte" );
    this.mnuPreviewMaxFileSize100K.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize100K );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize100K );

    this.mnuPreviewMaxFileSize1M = GUIFactory.createRadioButtonMenuItem(
							"1 MByte" );
    this.mnuPreviewMaxFileSize1M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize1M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize1M );

    this.mnuPreviewMaxFileSize10M = GUIFactory.createRadioButtonMenuItem(
							"10 MByte" );
    this.mnuPreviewMaxFileSize10M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize10M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize10M );

    this.mnuPreviewMaxFileSize100M = GUIFactory.createRadioButtonMenuItem(
							"100 MByte" );
    this.mnuPreviewMaxFileSize100M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize100M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize100M );

    this.mnuPreviewNoFileSizeLimit = GUIFactory.createRadioButtonMenuItem(
						"Unbegrenzt" );
    this.mnuPreviewNoFileSizeLimit.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewNoFileSizeLimit );
    mnuSettingsPreview.add( this.mnuPreviewNoFileSizeLimit );

    String maxFileSize = Main.getProperty( PROP_PREVIEW_MAX_FILE_SIZE );
    if( maxFileSize != null ) {
      switch( maxFileSize ) {
	case VALUE_PREVIEW_NONE:
	  this.mnuNoPreview.setSelected( true );
	  break;
	case VALUE_PREVIEW_MAX_FILESIZE_100K:
	  this.mnuPreviewMaxFileSize100K.setSelected( true );
	  break;
	case VALUE_PREVIEW_MAX_FILESIZE_1M:
	  this.mnuPreviewMaxFileSize1M.setSelected( true );
	  break;
	case VALUE_PREVIEW_MAX_FILESIZE_10M:
	  this.mnuPreviewMaxFileSize10M.setSelected( true );
	  break;
	case VALUE_PREVIEW_MAX_FILESIZE_100M:
	  this.mnuPreviewMaxFileSize100M.setSelected( true );
	  break;
	default:
	  this.mnuPreviewMaxFileSize1M.setSelected( true );
      }
    } else {
      this.mnuPreviewMaxFileSize10M.setSelected( true );
    }


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem( "Hilfe zum Datei-Browser..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar(
					mnuFile,
					mnuEdit,
					this.mnuSettings,
					mnuHelp ) );


    // Fensterinhalt
    setLayout( new GridBagLayout() );
    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = GUIFactory.createToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    if( this.screenFrm != null ) {
      toolBar.add( this.fileActionMngr.createLoadIntoEmuButton( this ) );
      toolBar.add( this.fileActionMngr.createStartInEmuButton( this ) );
    }
    toolBar.add( this.fileActionMngr.createEditTextButton( this ) );
    toolBar.add( this.fileActionMngr.createViewImageButton( this ) );
    toolBar.add( this.fileActionMngr.createPlayButton( this ) );


    // Dateibaum
    DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
    selModel.setSelectionMode(
		TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

    this.rootNode = new ExtendedFileNode( null, null, true );
    this.rootNode.refresh(
			null,
			this.mnuHiddenFiles.isSelected(),
			getFileNodeComparator() );

    this.treeModel = new DefaultTreeModel( this.rootNode );
    this.tree      = GUIFactory.createTree( this.treeModel );
    this.tree.setSelectionModel( selModel );
    this.tree.setEditable( false );
    this.tree.setExpandsSelectedPaths( false );
    this.tree.setRootVisible( false );
    this.tree.setScrollsOnExpand( true );
    this.tree.setShowsRootHandles( true );
    this.tree.setCellRenderer( new FileTreeCellRenderer() );
    this.tree.addFocusListener( this );
    this.tree.addKeyListener( this );
    this.tree.addMouseListener( this );
    this.tree.addTreeExpansionListener( this );
    this.tree.addTreeSelectionListener( this );
    this.tree.addTreeWillExpandListener( this );


    // Dateivorschau
    this.filePreviewFld = new FilePreviewFld( this );
    this.filePreviewFld.setBorder( BorderFactory.createEtchedBorder() );

    this.table = this.filePreviewFld.getJTable();
    if( this.table != null ) {
      this.table.addFocusListener( this );
      this.table.addKeyListener( this );
      this.table.addMouseListener( this );
      ListSelectionModel lsm = this.table.getSelectionModel();
      if( lsm != null ) {
	lsm.addListSelectionListener( this );
      }
    }


    // Anzeigebereich
    this.splitPane = GUIFactory.createSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				false,
				GUIFactory.createScrollPane( this.tree ),
				this.filePreviewFld );
    this.splitPane.setContinuousLayout( false );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( this.splitPane, gbc );

    int splitPos = Main.getIntProperty( PROP_SPLIT_POSITION, -1 );
    if( splitPos >= 0 ) {
      this.splitPane.setDividerLocation( splitPos );
    } else {
      Component c = this.splitPane.getLeftComponent();
      if( c != null ) {
	int       xDiv = c.getWidth() / 2;
	Dimension size = this.tree.getPreferredSize();
	if( size != null ) {
	  int xTmp = (size.width * 3) + 50;
	  if( xTmp > xDiv ) {
	    xDiv = xTmp;
	  }
	}
	this.splitPane.setDividerLocation( xDiv );
      }
    }


    // Zwischenablage
    this.clipboard = null;
    Toolkit tk = EmuUtil.getToolkit( this );
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
      if( this.clipboard != null ) {
	this.clipboard.addFlavorListener( this );
      }
    }


    // Dateibaum und Tabelle als Drag-Quelle
    this.tree.setDragEnabled( false );
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
		this.tree,
		DnDConstants.ACTION_COPY_OR_MOVE,
		this );
    if( this.table != null ) {
      this.table.setDragEnabled( false );
      DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
		this.table,
		DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE,
		this );
    }


    // Dateibaum und Tabelle als Drop-Ziel
    (new DropTarget( this.tree, this )).setActive( true );
    if( this.table != null ) {
      (new DropTarget( this.table, this )).setActive( true );
    }


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      setBoundsToDefaults();
    }


    // sonstiges
    updPasteState();
    updActionButtons();
    this.unreachableRefreshTimer = new javax.swing.Timer(
					UNREACHABLE_SEARCH_PERIOD_MILLIS,
					this );
  }


	/* --- private Methoden --- */

  private void activateWatchService()
  {
    try {
      this.watchService = FileSystems.getDefault().newWatchService();
      if( this.watchTimer != null ) {
	this.watchTimer.restart();
      } else {
	this.watchTimer = new javax.swing.Timer( 500, this );
	this.watchTimer.start();
      }
    }
    catch( IOException ex ) {}
    catch( UnsupportedOperationException ex ) {}
  }


  private boolean checkDropAction( DropTargetEvent e )
  {
    int dropAction    = 0;
    int sourceActions = 0;
    if( e instanceof DropTargetDragEvent ) {
      dropAction    = ((DropTargetDragEvent) e).getDropAction();
      sourceActions = ((DropTargetDragEvent) e).getSourceActions();
    } else if( e instanceof DropTargetDropEvent ) {
      dropAction    = ((DropTargetDropEvent) e).getDropAction();
      sourceActions = ((DropTargetDropEvent) e).getSourceActions();
    }
    return ((dropAction & sourceActions) != 0)
	   && ((dropAction == DnDConstants.ACTION_COPY)
		|| (dropAction == DnDConstants.ACTION_MOVE));
  }


  private boolean checkPopup( MouseEvent e )
  {
    boolean status = false;
    if( e != null ) {
      if( e.isPopupTrigger() ) {
	Component c = e.getComponent();
	int       x = e.getX();
	int       y = e.getY();
	if( c != null ) {

	   /*
	    * Wenn die Zeile unter dem Click-Punkt selektiert ist,
	    * wird das Popupmenu angezeigt,
	    * anderenfalls wird erst einmal selektiert.
	    */
	  boolean selected = false;
	  if( c == this.table ) {
	    int row = this.table.rowAtPoint( new Point( x, y ) );
	    if( row >= 0 ) {
	      if( this.table.isRowSelected( row ) ) {
		selected = true;
	      } else {
		this.table.setRowSelectionInterval( row, row );
	      }
	      this.lastActiveFld = this.table;
	      this.table.requestFocus();
	    }
	  }
	  else if( c == this.tree ) {
	    int row = this.tree.getRowForLocation( x, y );
	    if( row >= 0 ) {
	      if( this.tree.isRowSelected( row ) ) {
		selected = true;
	      } else {
		this.tree.setSelectionRow( row );
	      }
	      this.lastActiveFld = this.tree;
	      this.tree.requestFocus();
	    }
	  }
	  if( selected ) {
	    updActionButtons();
	    this.popupMnu.show( c, x, y );
	  }
	  status = true;
	}
      }
    }
    return status;
  }


  private void checkWatcherEvents()
  {
    if( this.watchService != null  ) {
      try {
	WatchKey wk = this.watchService.poll();
	if( wk != null ) {
	  wk.pollEvents();		// Events entfernen
	  boolean       used      = false;
	  Set<TreePath> treePaths = this.watchKey2TreePaths.get( wk );
	  if( treePaths != null ) {
	    for( TreePath tp : treePaths ) {
	      Object o = tp.getLastPathComponent();
	      if( o != null ) {
		if( o instanceof ExtendedFileNode ) {
		  boolean selected = this.tree.isPathSelected( tp );
		  if( selected || this.tree.isExpanded( tp ) ) {
		    refreshNode( (ExtendedFileNode) o );
		  }
		  if( selected ) {
		    fireUpdPreviewFld();
		  }
		  used = true;
		}
	      }
	    }
	  }
	  wk.reset();
	  if( !used ) {
	    wk.cancel();
	    this.watchKey2TreePaths.remove( wk );
	  }
	}
      }
      catch( ClosedWatchServiceException e ) {
	deactivateWatchService();
      }
    }
  }


  private void checkWatcherRegistration( TreePath tp )
  {
    if( (tp != null) && (this.watchService != null)  ) {
      Object node = tp.getLastPathComponent();
      if( FileUtil.checkTreeNodeUsable( this.tree, node ) ) {
	if( this.tree.isPathSelected( tp ) || this.tree.isExpanded( tp ) ) {
	  // Verzeichnis muss ueberwacht werden
	  WatchKey wk = this.treePath2WatchKey.get( tp );
	  if( (wk == null) && (node != null) ) {
	    if( node instanceof FileNode ) {
	      Path path = ((FileNode) node).getPath();
	      if( path != null ) {
		try {
		  wk = path.register(
				this.watchService,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE );
		  this.treePath2WatchKey.put( tp, wk );
		}
		catch( Exception ex ) {}
	      }
	    }
	  }
	  if( wk != null ) {
	    Set<TreePath> treePaths = this.watchKey2TreePaths.get( wk );
	    if( treePaths == null ) {
	      treePaths = new HashSet<>();
	      this.watchKey2TreePaths.put( wk, treePaths );
	    }
	    treePaths.add( tp );
	  }
	} else {
	  /*
	   * Verzeichnis braucht nicht mehr ueberwacht zu werden
	   *
	   * Beim Zuklappen eines Baumes werden keine Events
	   * fuer evtl. aufgeklappte Kindbaeume erzeugt.
	   * Damit auch die Registrierungen fuer diese Kindbaeume
	   * aufgehoben werden,
	   * werden hier einfach alle Registrierungen ueberprueft.
	   */
	  Collection<WatchKey> obsoleteWKs = new ArrayList<>();
	  for( WatchKey wk : this.watchKey2TreePaths.keySet() ) {
	    boolean       used      = false;
	    Set<TreePath> treePaths = this.watchKey2TreePaths.get( wk );
	    if( treePaths != null ) {
	      int n = treePaths.size();
	      if( n > 0 ) {
		try {
		  for( TreePath tp1
			: treePaths.toArray( new TreePath[ n ] ) )
		  {
		    if( this.tree.isPathSelected( tp1 )
				  || this.tree.isExpanded( tp1 ) )
		    {
		      used = true;
		    } else {
		      treePaths.remove( tp1 );
		      this.treePath2WatchKey.remove( tp1 );
		    }
		  }
		}
		catch( ArrayStoreException ex ) {}
	      }
	    }
	    if( !used ) {
	      obsoleteWKs.add( wk );
	    }
	  }
	  for( WatchKey wk : obsoleteWKs ) {
	    wk.cancel();
	    this.watchKey2TreePaths.remove( wk );
	  }
	}
      }
    }
  }


  private void collapseNode( TreeNode node )
  {
    if( node != null ) {
      try {
	TreeNode[] path = this.treeModel.getPathToRoot( node );
	if( path != null ) {
	  this.tree.collapsePath( new TreePath( path ) );
	}
      }
      catch( Exception ex ) {}
    }
  }


  private void deactivateWatchService()
  {
    if( this.watchTimer != null ) {
      this.watchTimer.stop();
    }
    for( WatchKey wk : this.watchKey2TreePaths.keySet() ) {
      wk.cancel();
    }
    this.treePath2WatchKey.clear();
    this.watchKey2TreePaths.clear();
    if( this.watchService != null ) {
      EmuUtil.closeSilently( this.watchService);
      this.watchService = null;
    }
  }


  private void dragInternal( DropTargetDragEvent e, boolean dragEnter )
  {
    boolean status = false;
    if( checkDropAction( e ) ) {
      DropTargetContext context = e.getDropTargetContext();
      if( context != null ) {
	Component c = context.getComponent();
	if( c == this.tree ) {
	  if( dragEnter ) {
	    this.tree.clearSelection();
	  }
	  Point point = e.getLocation();
	  if( point != null ) {
	    TreePath tp = this.tree.getPathForLocation( point.x, point.y );
	    if( tp != null ) {
	      TreePath dirPath = null;
	      while( tp != null ) {
		Object o = tp.getLastPathComponent();
		if( o != null ) {
		  if( o instanceof FileNode ) {
		    Path path = ((FileNode) o).getPath();
		    if( path != null ) {
		      if( Files.isDirectory(
					path,
					LinkOption.NOFOLLOW_LINKS ) )
		      {
			dirPath = tp;
			break;
		      }
		    }
		  }
		}
		tp = tp.getParentPath();
	      }
	      if( dirPath != null ) {
		fireSelectPath( dirPath );
		status = true;
	      }
	    }
	  }
	} else if( c == this.table ) {
	  if( this.tree.getSelectionCount() == 1 ) {
	    Object o = this.tree.getLastSelectedPathComponent();
	    if( o != null ) {
	      if( o instanceof FileNode ) {
		Path path = ((FileNode) o).getPath();
		if( path != null ) {
		  if( Files.isDirectory(
				path,
				LinkOption.NOFOLLOW_LINKS ) )
		  {
		    status = true;
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    if( status ) {
      e.acceptDrag( DnDConstants.ACTION_COPY_OR_MOVE );
    } else {
      e.rejectDrag();
    }
  }


  private void fireRefreshNodeFor( final Path path )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    refreshNodeFor( path );
		  }
		} );
  }


  private void fireRefreshParentNodesFor( Collection<?> files )
  {
    if( files != null ) {
      Set<Path> parents = FileUtil.createPathSet();
      for( Object o : files ) {
	if( o != null ) {
	  Path parent = null;
	  if( o instanceof File ) {
	    File parentFile = ((File) o).getParentFile();
	    if( parentFile != null ) {
	      try {
		parent = parentFile.toPath();
	      }
	      catch( InvalidPathException ex ) {}
	    }
	  } else if( o instanceof Path ) {
	    parent = ((Path) o).getParent();
	  }
	  if( parent != null ) {
	    parents.add( parent );
	  }
	}
      }
      for( Path parent : parents ) {
	 fireRefreshNodeFor( parent );
      }
    }
  }


  private void fireSelectChildNode( TreeNode parent, File file )
  {
    if( (parent != null) && (file != null) ) {
      TreeNode node = null;
      int      n    = parent.getChildCount();
      for( int i = 0; i < n; i++ ) {
	TreeNode child = parent.getChildAt( i );
	if( child != null ) {
	  if( child instanceof FileNode ) {
	    File f = ((FileNode) child).getFile();
	    if( f != null ) {
	      if( f.equals( file ) ) {
		node = child;
		break;
	      }
	    }
	  }
	}
      }
      if( node != null ) {
	Object[] parentPath = this.treeModel.getPathToRoot( parent );
	if( parentPath != null ) {
	  Object[] path = new Object[ parentPath.length + 1 ];
	  System.arraycopy( parentPath, 0, path, 0, parentPath.length );
	  path[ path.length - 1 ] = node;
	  fireSelectPath( new TreePath( path ) );
	}
      }
    }
  }


  private void fireSelectNode( TreeNode node )
  {
    if( node != null ) {
      TreeNode[] path = this.treeModel.getPathToRoot( node );
      if( path != null ) {
	fireSelectPath( new TreePath( path ) );
      }
    }
  }


  private void fireSelectPath( final TreePath path )
  {
    if( path != null ) {
      final JTree tree = this.tree;
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    tree.setSelectionPath( path );
			  }
			} );
    }
  }


  private void fireUpdPreviewFld()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updPreviewFld();
		  }
		} );
  }


  private FileNodeComparator getFileNodeComparator()
  {
    return this.mnuSortCaseSensitive.isSelected() ?
			FileNodeComparator.getCaseSensitiveInstance()
			: FileNodeComparator.getIgnoreCaseInstance();
  }


  private long getPreviewMaxFileSize()
  {
    long rv = 0;
    if( this.mnuNoPreview.isSelected() ) {
      rv = -1;
    } else if( this.mnuPreviewMaxFileSize100K.isSelected() ) {
      rv = 100 * 1024;
    } else if( this.mnuPreviewMaxFileSize1M.isSelected() ) {
      rv = 1024 * 1024;
    } else if( this.mnuPreviewMaxFileSize10M.isSelected() ) {
      rv = 10 * 1024 * 1024;
    } else if( this.mnuPreviewMaxFileSize100M.isSelected() ) {
      rv = 100 * 1024 * 1024;
    }
    return rv;
  }


  private ExtendedFileNode getSelectedExtendedFileNode()
  {
    ExtendedFileNode rv = null;
    if( this.lastActiveFld != null ) {
      if( this.lastActiveFld == this.table ) {
	TableModel tm = this.table.getModel();
	if( tm != null ) {
	  if( tm instanceof FileTableModel ) {
	    if( this.table.getSelectedRowCount() == 1 ) {
	      int viewRow = this.table.getSelectedRow();
	      if( viewRow >= 0 ) {
		int modelRow = this.table.convertRowIndexToModel( viewRow );
		if( modelRow >= 0 ) {
		  FileEntry e = ((FileTableModel) tm).getRow( modelRow );
		  if( e != null ) {
		    if( e instanceof ExtendedFileEntry ) {
		      rv = ((ExtendedFileEntry) e).getExtendedFileNode();
		      if( !FileUtil.checkTreeNodeUsable( this.tree, rv ) ) {
			rv = null;
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
      } else if( this.lastActiveFld == this.tree ) {
	if( this.tree.getSelectionCount() == 1 ) {
	  Object o = this.tree.getLastSelectedPathComponent();
	  if( (o != null)
	      && FileUtil.checkTreeNodeUsable( this.tree, o ) )
	  {
	    if( o instanceof ExtendedFileNode ) {
	      rv = (ExtendedFileNode) o;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private java.util.List<FileActionMngr.FileObject> getSelectedFileObjects()
  {
    java.util.List<FileActionMngr.FileObject> rv = null;
    if( this.lastActiveFld != null ) {
      if( this.lastActiveFld == this.table ) {
	rv = getSelectedTableFileObjects();
      } else {
	rv = getSelectedTreeFileObjects();
      }
    }
    return rv;
  }


  /*
   * Die Methode ermittelt die ausgewaehlten Knoten im Baum.
   * Wenn Vater und Nachkommen ausgewaehlt sind,
   * wird nur der Vater ermittelt.
   */
  private java.util.List<FileActionMngr.FileObject>
					getSelectedTreeFileObjects()
  {
    java.util.List<FileActionMngr.FileObject> rv = null;

    TreePath[] paths = this.tree.getSelectionPaths();
    if( paths != null ) {
      if( paths.length > 0 ) {
	Collection<TreePath> treePaths = new ArrayList<>( paths.length );
	for( TreePath path : paths ) {
	  Iterator<TreePath> iter = treePaths.iterator();
	  if( (path != null) && (iter != null) ) {
	    try {
	      while( iter.hasNext() ) {
		TreePath tmpPath = iter.next();
		if( tmpPath.equals( path )
		    || tmpPath.isDescendant( path ) )
		{
		  path = null;
		  break;
		} else if( path.isDescendant( tmpPath ) ) {
		  iter.remove();
		}
	      }
	    }
	    catch( NoSuchElementException ex1 ) {}
	    catch( UnsupportedOperationException ex2 ) {}
	    if( path != null ) {
	      treePaths.add( path );
	    }
	  }
	}
	int n = treePaths.size();
	if( n > 0 ) {
	  rv = new ArrayList<>( n );
	  for( TreePath path : treePaths ) {
	    Object o = path.getLastPathComponent();
	    if( (o != null)
		&& FileUtil.checkTreeNodeUsable( this.tree, o ) )
	    {
	      if( o instanceof FileActionMngr.FileObject ) {
		rv.add( (FileActionMngr.FileObject) o );
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private java.util.List<FileActionMngr.FileObject>
					getSelectedTableFileObjects()
  {
    java.util.List<FileActionMngr.FileObject> rv = null;

    TableModel tm = this.table.getModel();
    if( tm != null ) {
      if( tm instanceof FileTableModel ) {
	int[] rows = this.table.getSelectedRows();
	if( rows != null ) {
	  if( rows.length > 0 ) {
	    rv = new ArrayList<>( rows.length );
	    for( int i = 0; i < rows.length; i++ ) {
	      int modelRow = this.table.convertRowIndexToModel( rows[ i ] );
	      if( modelRow >= 0 ) {
		FileEntry e = ((FileTableModel) tm).getRow( modelRow );
		if( e != null ) {
		  if( e instanceof ExtendedFileEntry ) {
		    ExtendedFileNode fileNode =
			    ((ExtendedFileEntry) e).getExtendedFileNode();
		    if( (fileNode != null)
			&& FileUtil.checkTreeNodeUsable(
						this.tree,
						fileNode ) )
		    {
		      rv.add( fileNode );
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void refreshNode( ExtendedFileNode node )
  {
    if( node != null ) {
      if( node.isFileSystemRoot()
		|| FileUtil.checkTreeNodeUsable( this.tree, node ) )
      {
	node.refresh(
		this.treeModel,
		this.mnuHiddenFiles.isSelected(),
		getFileNodeComparator() );
      }
    }
  }


  private void refreshNodeFor( Path path )
  {
    if( path != null ) {
      Collection<ExtendedFileNode> nodes = this.rootNode.refreshNodeFor(
					path,
					this.treeModel,
					this.mnuHiddenFiles.isSelected(),
					getFileNodeComparator() );

      if( nodes != null ) {
	if( this.tree.getSelectionCount() == 1 ) {
	  Object o = this.tree.getLastSelectedPathComponent();
	  if( o != null ) {
	    for( ExtendedFileNode node : nodes ) {
	      if( o.equals( node ) ) {
		setPreviewedFileNode( node );
		break;
	      }
	    }
	  }
	}
      }
    }
  }


  private void setPreviewedFileNode( ExtendedFileNode fileNode )
  {
    if( !FileUtil.checkTreeNodeUsable( this.tree, fileNode ) ) {
      fileNode = null;
    }
    this.filePreviewFld.setFileNode(
			fileNode,
			getPreviewMaxFileSize(),
			this.mnuSortCaseSensitive.isSelected() );
  }


  private Set<String> startFindUnreachableNetPaths()
  {
    return DeviceIO.startFindUnreachableNetPaths(
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    updUnreachableNetPaths();
				  }
				} );
  }


  private void updActionButtons()
  {
    updActionButtons( false );
  }


  private void updActionButtons( boolean pasteOnly )
  {
    boolean stateOneDir  = false;
    boolean stateEntries = false;
    java.util.List<FileActionMngr.FileObject> fileObjs
					= getSelectedFileObjects();
    if( fileObjs != null ) {
      int n = fileObjs.size();
      if( n > 0 ) {
	stateEntries = true;
      }
      if( n == 1 ) {
	Path path = fileObjs.get( 0 ).getPath();
	if( path != null ) {
	  stateOneDir = Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS );
	}
      }
    }

    this.mnuEditPaste.setEnabled( stateOneDir && this.pasteState );
    this.popupPaste.setEnabled( stateOneDir && this.pasteState );

    if( !pasteOnly ) {
      this.mnuFileCreateDir.setEnabled( stateOneDir );
      this.popupCreateDir.setEnabled( stateOneDir );

      this.mnuFileFind.setEnabled( stateOneDir );
      this.popupFind.setEnabled( stateOneDir );

      this.mnuEditCut.setEnabled( stateEntries );
      this.popupCut.setEnabled( stateEntries );

      this.fileActionMngr.updActionButtonsEnabled( fileObjs );
    }
  }


  private void updPasteState()
  {
    boolean state = !this.cutFiles.isEmpty();
    if( !state && (this.clipboard != null)) {
      try {
	state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.javaFileListFlavor );
      }
      catch( Exception ex ) {}
    }
    this.pasteState = state;
    updActionButtons( true );
  }


  private void updPreviewFld()
  {
    ExtendedFileNode fileNode = null;
    if( this.tree.getSelectionCount() == 1 ) {
      Object o = this.tree.getLastSelectedPathComponent();
      if( o != null ) {
	if( o instanceof ExtendedFileNode ) {
	  fileNode = (ExtendedFileNode) o;
	}
      }
    }
    setPreviewedFileNode( fileNode );
  }


  private void updUnreachableNetPaths()
  {
    /*
     * Aenderungen an den nicht erreichbaren Netzwerkpfaden ermitteln
     * und fuer diese die Anzeige aktualisieren
     */
    Set<String> newPaths = DeviceIO.getUnreachableNetPaths();
    if( (this.rootNode != null)
	&& ((newPaths != null) || (this.unreachableNetPaths != null)) )
    {
      Set<String> diffPaths = null;
      if( (newPaths != null) && (this.unreachableNetPaths != null) ) {
	diffPaths = new TreeSet<>();
	for( String p : newPaths ) {
	  if( !this.unreachableNetPaths.contains( p ) ) {
	    diffPaths.add( p );
	  }
	}
	for( String p : this.unreachableNetPaths ) {
	  if( !newPaths.contains( p ) ) {
	    diffPaths.add( p );
	  }
	}
      } else if( (newPaths != null) && (this.unreachableNetPaths == null) ) {
	diffPaths = newPaths;
      } else if( (newPaths == null) && (this.unreachableNetPaths != null) ) {
	diffPaths = this.unreachableNetPaths;
      }
      if( diffPaths != null ) {

	/*
	 * Dateisystemwurzeln aktualisieren,
	 * da fuer diese moeglicherweise nicht alle Icons ermittelt wurden
	 */
	int n = this.rootNode.getChildCount();
	for( int i = 0; i < n; i++ ) {
	  TreeNode node = this.rootNode.getChildAt( i );
	  if( node instanceof FileNode ) {
	    ((FileNode) node).updNode();
	    this.treeModel.nodeChanged( node );
	    File f = ((FileNode) node).getFile();
	    if( f != null ) {
	      diffPaths.remove( f.getPath() );
	    }
	  }
	}

	// verbleibende Differenzpfade aktualisieren
	for( String path : diffPaths ) {
	  File file = new File( path );

	  // Pfad in Abschnitte zerlegen
	  java.util.List<File> fileItems = new ArrayList<>();
	  while( file != null ) {
	    fileItems.add( file );
	    file = file.getParentFile();
	  }

	  // zu aktualisierenden Knoten suchen
	  FileNode node    = this.rootNode;
	  int      itemIdx = fileItems.size() - 1;
	  while( (node != null) && (itemIdx >= 0) ) {
	    FileNode childNode = node.getChildByFile(
					fileItems.get( itemIdx ) );
	    if( childNode == null ) {
	      break;
	    }
	    node = childNode;
	    --itemIdx;
	  }
	  if( node != this.rootNode ) {
	    boolean usable = node.updNode();
	    this.treeModel.nodeChanged( node );
	    if( !usable ) {
	      collapseNode( node );
	    }
	  }
	}
      }
    }

    // neue nicht erreichbare Pfade kopieren und merken
    if( newPaths != null ) {
      this.unreachableNetPaths = new TreeSet<>( newPaths );
    } else {
      this.unreachableNetPaths = null;
    }
  }
}
