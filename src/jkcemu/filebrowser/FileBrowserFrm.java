/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Datei-Browser
 */

package jkcemu.filebrowser;

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
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import jkcemu.Main;
import jkcemu.base.AbstractFileWorker;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileCopier;
import jkcemu.base.FileEntry;
import jkcemu.base.FileMover;
import jkcemu.base.FileTableModel;
import jkcemu.base.FileTreeNode;
import jkcemu.base.FileTreeNodeComparator;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;


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
				TreeSelectionListener,
				TreeWillExpandListener
{
  private static FileBrowserFrm instance = null;

  private static final String HELP_PAGE = "/help/tools/filebrowser.htm";
  private static final String PROP_SHOW_HIDDEN_FILES
				= "jkcemu.filebrowser.show_hidden_files";
  private static final String PROP_SORT_CASE_SENSITIVE
				= "jkcemu.filebrowser.sort_case_sensitive";
  private static final String PROP_PREVIEW_MAX_FILE_SIZE
				= "jkcemu.filebrowser.preview.max_file_size";
  private static final String PROP_SPLIT_POSITION
				= "jkcemu.filebrowser.split.position";

  private ScreenFrm            screenFrm;
  private FileActionMngr       fileActionMngr;
  private FileSystemView       fsv;
  private JMenuItem            mnuFileCreateDir;
  private JMenuItem            mnuFileFind;
  private JMenuItem            mnuFileRefresh;
  private JMenuItem            mnuFileClose;
  private JMenuItem            mnuEditCut;
  private JMenuItem            mnuEditPaste;
  private JCheckBoxMenuItem    mnuPhysFileSys;
  private JCheckBoxMenuItem    mnuHiddenFiles;
  private JCheckBoxMenuItem    mnuSortCaseSensitive;
  private JRadioButtonMenuItem mnuNoPreview;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize100K;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize1M;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize10M;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize100M;
  private JRadioButtonMenuItem mnuPreviewNoFileSizeLimit;
  private JMenuItem            mnuHelpContent;
  private JPopupMenu           mnuPopup;
  private JMenuItem            mnuPopupCut;
  private JMenuItem            mnuPopupPaste;
  private JMenuItem            mnuPopupCreateDir;
  private JMenuItem            mnuPopupFind;
  private JSplitPane           splitPane;
  private JTable               table;
  private JTree                tree;
  private DefaultTreeModel     treeModel;
  private int[]                treeDragSelectionRows;
  private FileNode             rootNode;
  private FilePreviewFld       filePreviewFld;
  private Component            lastActiveFld;
  private Clipboard            clipboard;
  private java.util.List<File> cutFiles;
  private boolean              ignoreFlavorEvent;
  private boolean              pasteState;


  public static void open( ScreenFrm screenFrm )
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


  public static void fireFilesChanged( Collection files )
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
	    e.startDrag( null, new FileListSelection( files ), this );
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
    int     action    = e.getDropAction();
    if( ((action == DnDConstants.ACTION_COPY)
		|| (action == DnDConstants.ACTION_MOVE))
	&& ((action & e.getSourceActions()) != 0)
	&& e.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) )
    {
      treeNode = getSelectedFileNode();
      while( treeNode != null ) {
	if( treeNode instanceof FileTreeNode ) {
	  Path path = ((FileTreeNode) treeNode).getPath();
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
	 * da die eigentliche Datei-Operationen asynchron ausgefuehrt werden
	 * und somit in dem Zeitpunkt noch nicht abgeschlossen sind.
	 */
	e.acceptDrop( DnDConstants.ACTION_COPY_OR_MOVE );

	// Datei-Operationen anstossen
	Transferable t = e.getTransferable();
	if( t != null ) {
	  Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof Collection ) {
	      Collection files = (Collection) o;

	      // Pruefung und Sicherheitsabfrage
	      boolean status    = true;
	      File    srcFile   = null;
	      int     nSrcFiles = 0;
	      int     nSrcDirs  = 0;
	      for( Object f : files ) {
		if( f instanceof File ) {
		  srcFile = (File) f;
		  if( srcFile.isDirectory() ) {
		    nSrcDirs++;
		  } else {
		    nSrcFiles++;
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
		      if( EmuUtil.equals( srcDir, dstPath.toFile() ) ) {
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
				  if( EmuUtil.equals( srcFile, tmpFile ) ) {
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
		  if( action == DnDConstants.ACTION_MOVE ) {
		    buf.append( " verschieben?" );
		  } else {
		    buf.append( " kopieren?" );
		  }
		  status = BaseDlg.showYesNoDlg( this, buf.toString() );
		}
		if( status ) {
		  if( action == DnDConstants.ACTION_MOVE ) {
		    (new FileMover(
			this,
			files,
			dstPath,
			this,
			this.fileActionMngr.getFileWorkers() )).startWork();
		  } else {
		    (new FileCopier(
			this,
			files,
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
	BaseDlg.showErrorDlg( this, ex );
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


	/* --- TreeSelectionListener --- */

  @Override
  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      TreePath[] paths = this.tree.getSelectionPaths();
      if( paths != null ) {
	for( TreePath p : paths ) {
	  Object o = p.getLastPathComponent();
	  if( o != null ) {
	    if( o instanceof FileNode ) {
	      refreshNode( (FileNode) o );
	    }
	  }
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
    TreePath treePath = e.getPath();
    if( treePath != null ) {
      setWaitCursor( true );
      Object o = treePath.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileNode ) {
	  FileNode fileNode = (FileNode) o;
	  refreshNode( fileNode );
	  int n = fileNode.getChildCount();
	  for( int i = 0; i < n; i++ ) {
	    Object child = fileNode.getChildAt( i );
	    if( child != null ) {
	      if( child instanceof FileNode ) {
		refreshNode( (FileNode) child );
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
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = super.applySettings( props, resizable );
    if( !isVisible() ) {
      String previewMaxFileSize = null;
      if( props != null ) {
	previewMaxFileSize = props.getProperty(
				PROP_PREVIEW_MAX_FILE_SIZE );
      }
      if( previewMaxFileSize != null ) {
	if( previewMaxFileSize.equals( "no_preview" ) ) {
	  this.mnuNoPreview.setSelected( true );
	} else if( previewMaxFileSize.equals( "100K" ) ) {
	  this.mnuPreviewMaxFileSize100K.setSelected( true );
	} else if( previewMaxFileSize.equals( "1M" ) ) {
	  this.mnuPreviewMaxFileSize1M.setSelected( true );
	} else if( previewMaxFileSize.equals( "10M" ) ) {
	  this.mnuPreviewMaxFileSize10M.setSelected( true );
	} else if( previewMaxFileSize.equals( "100M" ) ) {
	  this.mnuPreviewMaxFileSize100M.setSelected( true );
	} else {
	  this.mnuPreviewNoFileSizeLimit.setSelected( true );
	}
      } else {
	this.mnuPreviewNoFileSizeLimit.setSelected( true );
      }
      int splitPos = EmuUtil.getIntProperty(
				props,
				PROP_SPLIT_POSITION,
				-1 );
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
    }
    boolean changed = false;
    boolean state   = EmuUtil.getBooleanProperty(
				props,
				PROP_SHOW_HIDDEN_FILES,
				false );
    if( state != this.mnuHiddenFiles.isSelected() ) {
      this.mnuHiddenFiles.setSelected( state );
      changed = true;
    }
    state = EmuUtil.getBooleanProperty(
				props,
				PROP_SORT_CASE_SENSITIVE,
				File.separatorChar == '/' );
    if( state != this.mnuSortCaseSensitive.isSelected() ) {
      this.mnuSortCaseSensitive.setSelected( state );
      changed = true;
    }
    if( this.mnuPhysFileSys != null ) {
      state = EmuUtil.getBooleanProperty(
				props,
				EmuUtil.PROP_SHOW_PHYS_FILESYS,
				false );
      if( state != this.mnuPhysFileSys.isSelected() ) {
	this.mnuPhysFileSys.setSelected( state );
	this.rootNode.setFileSystemView( state ? this.fsv : null );
	changed = true;
      }
    }
    if( changed ) {
      refreshNode( this.rootNode );
    }
    return rv;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      if( e != null ) {
	Object src = e.getSource();
	if( src != null ) {
	  if( (src == this.table) || (src == this.tree) ) {
	    rv = true;
	    doFileAction( src );
	  }
	  else if( src == this.mnuFileRefresh ) {
	    rv = true;
	    doFileRefresh();
	  }
	  else if( (src == this.mnuFileCreateDir)
		   || (src == this.mnuPopupCreateDir) )
	  {
	    rv = true;
	    doFileCreateDir();
	  }
	  else if( (src == this.mnuFileFind)
		   || (src == this.mnuPopupFind) )
	  {
	    rv = true;
	    doFileFind();
	  }
	  else if( src == this.mnuFileClose ) {
	    rv = true;
	    doClose();
	  }
	  else if( (src == this.mnuEditCut)
		   || (src == this.mnuPopupCut) )
	  {
	    rv = true;
	    doEditCut();
	  }
	  else if( (src == this.mnuEditPaste)
		   || (src == this.mnuPopupPaste) )
	  {
	    rv = true;
	    doEditPaste();
	  }
	  else if( src == this.mnuHiddenFiles ) {
	    rv = true;
	    doSettingsHiddenFiles();
	  }
	  else if( src == this.mnuPhysFileSys ) {
	    rv = true;
	    doSettingsPhysFileSys();
	  }
	  else if( src == this.mnuSortCaseSensitive ) {
	    rv = true;
	    doSettingsSortCaseSensitive();
	  }
	  else if( src == this.mnuHelpContent ) {
	    rv = true;
	    HelpFrm.open( HELP_PAGE );
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
      Main.checkQuit( this );
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      props.setProperty(
		PROP_SHOW_HIDDEN_FILES,
		String.valueOf( this.mnuHiddenFiles.isSelected() ) );
      props.setProperty(
		PROP_SORT_CASE_SENSITIVE,
		String.valueOf( this.mnuSortCaseSensitive.isSelected() ) );

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
      props.setProperty(
		PROP_PREVIEW_MAX_FILE_SIZE,
		previewMaxFileSize );

      props.setProperty(
		PROP_SPLIT_POSITION,
		String.valueOf( this.splitPane.getDividerLocation() ) );
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
    Path     dstPath  = null;
    FileNode fileNode = getSelectedFileNode();
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
	    Object o = this.clipboard.getData( DataFlavor.javaFileListFlavor );
	    if( o != null ) {
	      Collection files = null;
	      if( o instanceof File ) {
		files = Collections.singletonList( (File) o );
	      } else if( o instanceof Collection ) {
		files = (Collection) o;
	      }
	      if( !files.isEmpty() ) {
		(new FileCopier(
			this,
			files,
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
    FileNode fileNode = getSelectedFileNode();
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
    FileNode fileNode = getSelectedFileNode();
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
	File dirFile = EmuUtil.createDir( this, file );
	if( dirFile != null ) {
	  boolean selected = false;
	  try {
	    this.rootNode.refreshNodeFor(
				file.toPath(),
				this.treeModel,
				this.mnuHiddenFiles.isSelected(),
				getFileTreeNodeComparator() );
	  }
	  catch( InvalidPathException ex ) {
	    if( parent instanceof FileNode ) {
	      refreshNode( (FileNode) parent );
	    }
	  }
	  fireSelectNode( parent, dirFile );
	  updActionButtons();
	}
      }
    }
  }

  private void doFileRefresh()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      refreshNode( fileNode );
    } else {
      refreshNode( this.rootNode );
    }
    updActionButtons();
  }


  private void doFileFind()
  {
    Path     path     = null;
    FileNode fileNode = getSelectedFileNode();
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


  private void doSettingsPhysFileSys()
  {
    if( this.mnuPhysFileSys != null ) {
      boolean state = this.mnuPhysFileSys.isSelected();
      EmuUtil.setProperty(
		Main.getProperties(),
		EmuUtil.PROP_SHOW_PHYS_FILESYS,
		state );
      this.rootNode.setFileSystemView( state ? null : this.fsv );
      refreshNode( this.rootNode );
    }
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
    this.fsv                   = EmuUtil.getDifferentLogicalFileSystemView();
    this.ignoreFlavorEvent     = false;
    this.pasteState            = false;
    this.lastActiveFld         = null;
    this.treeDragSelectionRows = null;
    this.clipboard             = null;
    this.cutFiles              = new ArrayList<>();
    setTitle( "JKCEMU Datei-Browser" );
    Main.updIcon( this );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.fileActionMngr.addCopyFileNameMenuItemsTo( this.mnuPopup, mnuEdit );
    mnuEdit.addSeparator();
    this.mnuPopup.addSeparator();

    this.mnuEditCut = createJMenuItem(
				"Dateien/Verzeichnisse ausschneiden" );
    mnuEdit.add( this.mnuEditCut );

    this.mnuPopupCut = createJMenuItem(
				"Dateien/Verzeichnisse ausschneiden" );
    this.mnuPopup.add( this.mnuPopupCut );

    this.fileActionMngr.addCopyFileMenuItemTo( this.mnuPopup, mnuEdit );

    this.mnuEditPaste = createJMenuItem(
				"Dateien/Verzeichnisse einf\u00FCgen" );
    mnuEdit.add( this.mnuEditPaste );

    this.mnuPopupPaste = createJMenuItem(
				"Dateien/Verzeichnisse einf\u00FCgen" );
    this.mnuPopup.add( this.mnuPopupPaste );
    this.mnuPopup.addSeparator();


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    if( this.screenFrm != null ) {
      this.fileActionMngr.addLoadIntoEmuMenuItemsTo( this.mnuPopup, mnuFile );
      mnuFile.addSeparator();
    }

    this.fileActionMngr.addFileMenuItemsTo( this.mnuPopup, mnuFile );
    this.mnuPopup.addSeparator();
    mnuFile.addSeparator();

    this.mnuFileCreateDir = createJMenuItem( "Verzeichnis erstellen..." );
    mnuFile.add( this.mnuFileCreateDir );

    this.mnuFileFind = createJMenuItem( "Im Verzeichnis suchen..." );
    mnuFile.add( this.mnuFileFind );

    this.mnuFileRefresh = createJMenuItem( "Aktualisieren" );
    mnuFile.add( this.mnuFileRefresh );
    mnuFile.addSeparator();

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );

    this.mnuPopupCreateDir = createJMenuItem( "Verzeichnis erstellen..." );
    this.mnuPopup.add( this.mnuPopupCreateDir );

    this.mnuPopupFind = createJMenuItem( "Im Verzeichnis suchen..." );
    this.mnuPopup.add( this.mnuPopupFind );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuSettings.setMnemonic( KeyEvent.VK_E );

    this.mnuPhysFileSys = null;
    if( this.fsv != null ) {
      this.mnuPhysFileSys = new JCheckBoxMenuItem(
		"Physische Dateisystemstruktur anzeigen",
		Main.getBooleanProperty(
				EmuUtil.PROP_SHOW_PHYS_FILESYS,
				false ) );
      this.mnuPhysFileSys.addActionListener( this );
      mnuSettings.add( this.mnuPhysFileSys );
    }

    this.mnuHiddenFiles = new JCheckBoxMenuItem(
		"Versteckte Dateien anzeigen",
		Main.getBooleanProperty( PROP_SHOW_HIDDEN_FILES, false ) );
    this.mnuHiddenFiles.addActionListener( this );
    mnuSettings.add( this.mnuHiddenFiles );

    this.mnuSortCaseSensitive = new JCheckBoxMenuItem(
		"Gro\u00DF-/Kleinschreibung bei Sortierung beachten",
		Main.getBooleanProperty( PROP_SORT_CASE_SENSITIVE, false ) );
    this.mnuSortCaseSensitive.addActionListener( this );
    mnuSettings.add( this.mnuSortCaseSensitive );
    mnuSettings.addSeparator();

    JMenu mnuSettingsPreview = new JMenu(
			"Max. Dateigr\u00F6\u00DFe f\u00FCr Vorschau" );
    mnuSettings.add( mnuSettingsPreview );

    ButtonGroup grpPreviewMaxFileSize = new ButtonGroup();

    this.mnuNoPreview = new JRadioButtonMenuItem( "Keine Vorschau" );
    this.mnuNoPreview.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuNoPreview );
    mnuSettingsPreview.add( this.mnuNoPreview );

    this.mnuPreviewMaxFileSize100K = new JRadioButtonMenuItem( "100 KByte" );
    this.mnuPreviewMaxFileSize100K.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize100K );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize100K );

    this.mnuPreviewMaxFileSize1M = new JRadioButtonMenuItem( "1 MByte" );
    this.mnuPreviewMaxFileSize1M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize1M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize1M );

    this.mnuPreviewMaxFileSize10M = new JRadioButtonMenuItem( "10 MByte" );
    this.mnuPreviewMaxFileSize10M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize10M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize10M );

    this.mnuPreviewMaxFileSize100M = new JRadioButtonMenuItem( "100 MByte" );
    this.mnuPreviewMaxFileSize100M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize100M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize100M );

    this.mnuPreviewNoFileSizeLimit = new JRadioButtonMenuItem( "Unbegrenzt" );
    this.mnuPreviewNoFileSizeLimit.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewNoFileSizeLimit );
    mnuSettingsPreview.add( this.mnuPreviewNoFileSizeLimit );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuSettings );
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
						new Insets( 0, 0, 0, 0 ),
						0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    if( this.screenFrm != null ) {
      toolBar.add( this.fileActionMngr.createLoadIntoEmuButton() );
      toolBar.add( this.fileActionMngr.createStartInEmuButton() );
    }
    toolBar.add( this.fileActionMngr.createEditTextButton() );
    toolBar.add( this.fileActionMngr.createViewImageButton() );
    toolBar.add( this.fileActionMngr.createPlayButton() );


    // Dateibaum
    DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
    selModel.setSelectionMode(
		TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

    this.rootNode = new FileNode( null, null, null, true, null );
    if( this.mnuPhysFileSys != null ) {
      this.rootNode.setFileSystemView(
		this.mnuPhysFileSys.isSelected() ? null : this.fsv );
    }
    this.rootNode.refresh(
			null,
			this.mnuHiddenFiles.isSelected(),
			getFileTreeNodeComparator() );

    this.treeModel = new DefaultTreeModel( this.rootNode );
    this.tree      = new JTree( this.treeModel );
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
    this.splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane( this.tree ),
				this.filePreviewFld );
    this.splitPane.setContinuousLayout( false );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( this.splitPane, gbc );


    // Zwischenablage
    this.clipboard = null;
    Toolkit tk = getToolkit();
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
    if( !applySettings( Main.getProperties(), true ) ) {
      setBoundsToDefaults();
    }
    setResizable( true );


    // sonstiges
    updPasteState();
    updActionButtons();
  }


	/* --- private Methoden --- */

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
	    this.mnuPopup.show( c, x, y );
	  }
	  status = true;
	}
      }
    }
    return status;
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
		  if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) ) {
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


  private void fireRefreshParentNodesFor( Collection files )
  {
    if( files != null ) {
      Set<Path> parents = EmuUtil.createPathSet();
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


  private FileTreeNodeComparator getFileTreeNodeComparator()
  {
    return this.mnuSortCaseSensitive.isSelected() ?
			FileTreeNodeComparator.getCaseSensitiveInstance()
			: FileTreeNodeComparator.getIgnoreCaseInstance();
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


  private FileNode getSelectedFileNode()
  {
    FileNode rv = null;
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
		  FileEntry entry = ((FileTableModel) tm).getRow( modelRow );
		  if( entry != null ) {
		    if( entry instanceof ExtendedFileEntry ) {
			rv = ((ExtendedFileEntry) entry).getFileNode();
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
	  if( o != null ) {
	    if( o instanceof FileNode ) {
	      rv = (FileNode) o;
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
		if( tmpPath.equals( path ) || tmpPath.isDescendant( path ) ) {
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
	    if( o != null ) {
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
		FileEntry entry = ((FileTableModel) tm).getRow( modelRow );
		if( entry != null ) {
		  if( entry instanceof ExtendedFileEntry ) {
		    FileNode fileNode =
			    ((ExtendedFileEntry) entry).getFileNode();
		    if( fileNode != null ) {
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


  private void refreshNode( FileNode node )
  {
    if( node != null ) {
      node.refresh(
		this.treeModel,
		this.mnuHiddenFiles.isSelected(),
		getFileTreeNodeComparator() );
    }
  }


  private void refreshNodeFor( Path path )
  {
    if( path != null ) {
      Collection<FileNode> nodes = this.rootNode.refreshNodeFor(
				path,
				this.treeModel,
				this.mnuHiddenFiles.isSelected(),
				getFileTreeNodeComparator() );

      if( nodes != null ) {
	if( this.tree.getSelectionCount() == 1 ) {
	  Object o = this.tree.getLastSelectedPathComponent();
	  if( o != null ) {
	    for( FileNode node : nodes ) {
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


  private void fireSelectNode( TreeNode parent, File file )
  {
    if( (parent != null) && (file != null) ) {
      Object      childNode = null;
      Enumeration children  = parent.children();
      if( children != null ) {
	try {
	  while( children.hasMoreElements() ) {
	    Object o = children.nextElement();
	    if( o != null ) {
	      if( o instanceof FileNode ) {
		File f = ((FileNode) o).getFile();
		if( f != null ) {
		  if( f.equals( file ) ) {
		    childNode = o;
		    break;
		  }
		}
	      }
	    }
	  }
	}
	catch( NoSuchElementException ex ) {}
      }
      if( childNode != null ) {
	Object[] parentPath = this.treeModel.getPathToRoot( parent );
	if( parentPath != null ) {
	  Object[] path = new Object[ parentPath.length + 1 ];
	  System.arraycopy( parentPath, 0, path, 0, parentPath.length );
	  path[ path.length - 1 ] = childNode;
	  fireSelectPath( new TreePath( path ) );
	}
      }
    }
  }


  private void setPreviewedFileNode( FileNode fileNode )
  {
    this.filePreviewFld.setFileNode(
		fileNode,
		getPreviewMaxFileSize(),
		this.mnuSortCaseSensitive.isSelected() );
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
    this.mnuPopupPaste.setEnabled( stateOneDir && this.pasteState );

    if( !pasteOnly ) {
      this.mnuFileCreateDir.setEnabled( stateOneDir );
      this.mnuPopupCreateDir.setEnabled( stateOneDir );

      this.mnuFileFind.setEnabled( stateOneDir );
      this.mnuPopupFind.setEnabled( stateOneDir );

      this.mnuEditCut.setEnabled( stateEntries );
      this.mnuPopupCut.setEnabled( stateEntries );

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
    FileNode fileNode = null;
    if( this.tree.getSelectionCount() == 1 ) {
      Object o = this.tree.getLastSelectedPathComponent();
      if( o != null ) {
	if( o instanceof FileNode ) {
	  fileNode = (FileNode) o;
	}
      }
    }
    setPreviewedFileNode( fileNode );
  }
}
