/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Verzeichnisses
 */

package jkcemu.file;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import jkcemu.base.BaseDlg;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class DirSelectDlg
		extends BaseDlg
		implements TreeSelectionListener, TreeWillExpandListener
{
  private File               selectedFile;
  private FileNodeComparator comparator;
  private FileNode           rootNode;
  private DefaultTreeModel   treeModel;
  private JTree              tree;
  private JButton            btnApprove;
  private JButton            btnCancel;
  private JButton            btnNew;


  public static File selectDirectory( Window owner, File preselection )
  {
    DirSelectDlg dlg = new DirSelectDlg( owner, preselection );
    dlg.setVisible( true );
    return dlg.selectedFile;
  }


	/* --- TreeSelectionListener --- */

  @Override
  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      /*
       * Pruefen, ob der ausgewaehlte Eintrag ein Verzeichnis ist.
       * Das ist notwendig,
       * da ein symbolischer Link ausgewaehlt sein koennte,
       * der auf etwas anderes als ein Verzeichnis zeigt.
       */
      boolean state = false;
      File    file  = getSelectedFile();
      if( file != null ) {
	state = file.isDirectory();
      }
      this.btnApprove.setEnabled( state );
      this.btnNew.setEnabled( state );
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
	if( o instanceof FileNode ) {
	  refreshNode( (FileNode) o );
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
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( (src == this.tree) || (src == this.btnApprove) ) {
	  rv = true;
	  doApprove();
	}
	else if( src == this.btnNew ) {
	  rv = true;
	  doNew();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  this.selectedFile = null;
	  doClose();
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.tree.removeTreeSelectionListener( this );
      this.tree.removeTreeWillExpandListener( this );
      this.tree.removeKeyListener( this );
      this.btnApprove.removeActionListener( this );
      this.btnNew.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private DirSelectDlg( Window owner, final File preSelection )
  {
    super( owner, "Verzeichnisauswahl" );
    this.selectedFile = null;
    this.comparator   = FileNodeComparator.getIgnoreCaseInstance();

    DeviceIO.startFindUnreachableNetPaths(
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    updRoots();
				  }
				} );


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


    // Dateibaum
    DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
    selModel.setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

    this.rootNode  = new FileNode( null, null, true );
    this.treeModel = new DefaultTreeModel( this.rootNode );
    this.tree      = GUIFactory.createTree( this.treeModel );
    this.tree.setSelectionModel( selModel );
    this.tree.setEditable( false );
    this.tree.setPreferredSize( new Dimension( 400, 300 ) );
    this.tree.setRootVisible( false );
    this.tree.setScrollsOnExpand( true );
    this.tree.setShowsRootHandles( true );
    this.tree.setCellRenderer( new FileTreeCellRenderer() );
    add( GUIFactory.createScrollPane( this.tree ), gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel(
				new GridLayout( 1, 3, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApprove = GUIFactory.createButton( EmuUtil.TEXT_SELECT );
    this.btnApprove.setEnabled( false );
    panelBtn.add( this.btnApprove );

    this.btnNew = GUIFactory.createButton( "Neu..." );
    this.btnNew.setEnabled( false );
    panelBtn.add( this.btnNew );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setResizable( true );
    setParentCentered();
    this.tree.setPreferredSize( null );


    // Dateibaum aktualisieren
    this.rootNode.setChildrenLoaded( false );
    refreshNode( this.rootNode );
    this.treeModel.nodeStructureChanged( this.rootNode );


    // Listener
    this.tree.addTreeSelectionListener( this );
    this.tree.addTreeWillExpandListener( this );
    this.tree.addKeyListener( this );
    this.btnApprove.addActionListener( this );
    this.btnNew.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // vorausgewaehltes Verzeichnis einstellen
    if( preSelection != null ) {
      EventQueue.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    selectPath( preSelection );
			  }
			} );
    }
  }


	/* --- private Methoden --- */

  private void doApprove()
  {
    File file = getSelectedFile();
    if( file != null ) {
      if( file.isDirectory() ) {
	this.selectedFile = file;
	doClose();
      }
    }
  }


  private void doNew()
  {
    TreePath parentPath = this.tree.getSelectionPath();
    if( parentPath != null ) {
      Object o = parentPath.getLastPathComponent();
      if( (o != null) && (FileUtil.checkTreeNodeUsable( this.tree, o )) ) {
	if( o instanceof FileNode ) {
	  FileNode parent = (FileNode) o;
	  if( parent != null ) {
	    refreshNode( parent );
	    File parentFile = parent.getFile();
	    if( parentFile != null ) {
	      if( parentFile.isDirectory() ) {
		File newDir = FileUtil.createDir( this, parentFile );
		if( newDir != null ) {
		  FileNode node = new FileNode( parent, newDir, false );
		  parent.add( node );
		  this.treeModel.nodeStructureChanged( parent );
		  final JTree    tree    = this.tree;
		  final TreePath path = parentPath.pathByAddingChild( node );
		  EventQueue.invokeLater(
				new Runnable()
				{
				  public void run()
				  {
				    tree.setSelectionPath( path );
				  }
				} );
		}
	      }
	    }
	  }
	}
      }
    }
  }


  private void expandAndSelectPath( TreePath treePath )
  {
    if( treePath != null ) {
      if( treePath.getPathCount() > 1 ) {
	this.tree.expandPath( treePath );
	this.tree.makeVisible( treePath );
	this.tree.setSelectionPath( treePath );
	this.tree.scrollPathToVisible( treePath );
      }
    }
  }


  private File getSelectedFile()
  {
    File     rv = null;
    TreePath tp = this.tree.getSelectionPath();
    if( tp != null ) {
      Object o = tp.getLastPathComponent();
      if( (o != null) && (FileUtil.checkTreeNodeUsable( this.tree, o )) ) {
	if( o instanceof FileNode ) {
	  rv = ((FileNode) o).getFile();
	}
      }
    }
    return rv;
  }


  private void refreshNode( FileNode node )
  {
    if( (node.isFileSystemRoot()
		|| FileUtil.checkTreeNodeUsable( this.tree, node ))
	&& !node.hasChildrenLoaded() )
    {
      node.updNode();
      node.removeAllChildren();

      boolean done    = false;
      boolean fsRoot  = false;
      File[]  entries = null;
      File    file    = node.getFile();
      if( file != null ) {
	if( FileUtil.isUsable( file ) ) {
	  entries = file.listFiles();
	}
      } else {
	entries = DeviceIO.listRoots();
	fsRoot  = true;
      }
      if( entries != null ) {
	for( File tmpFile : entries ) {
	  boolean state = fsRoot;
	  if( !state ) {
	    if( FileUtil.isUsable( tmpFile ) ) {
	      if( tmpFile.isDirectory() && !tmpFile.isHidden() ) {
		state = true;
	      }
	    }
	  }
	  if( state ) {
	    node.add( new FileNode( node, tmpFile, fsRoot ) );
	  }
	}
      }
      node.sort( this.comparator );
      node.setChildrenLoaded( true );
      this.treeModel.nodeStructureChanged( node );
    }
  }


  private void selectPath( File file )
  {
    // Pfad in Abschnitte zerlegen
    java.util.List<File> fileItems = new ArrayList<>();
    while( file != null ) {
      fileItems.add( file );
      file = file.getParentFile();
    }

    // zu selektierenden Knoten suchen
    FileNode node     = this.rootNode;
    TreePath treePath = new TreePath( node );
    int      itemIdx  = fileItems.size() - 1;
    while( (node != null) && (itemIdx >= 0) ) {
      refreshNode( node );

      // zugehoeriges Kind suchen
      FileNode childNode = node.getChildByFile( fileItems.get( itemIdx ) );
      if( childNode == null ) {
	break;
      }
      treePath = treePath.pathByAddingChild( childNode );
      node = childNode;
      --itemIdx;
    }
    final TreePath tp = treePath;
    EventQueue.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    expandAndSelectPath( tp );
			  }
			} );
  }


  private void updRoots()
  {
    if( this.rootNode != null ) {
      int n = this.rootNode.getChildCount();
      for( int i = 0; i < n; i++ ) {
	TreeNode node = this.rootNode.getChildAt( i );
	if( node instanceof FileNode ) {
	  ((FileNode) node).updNode();
	  this.treeModel.nodeChanged( node );
	}
      }
    }
  }
}
