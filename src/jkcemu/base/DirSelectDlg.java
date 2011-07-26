/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Verzeichnisses
 */

package jkcemu.base;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;


public class DirSelectDlg
		extends BasicDlg
		implements TreeSelectionListener, TreeWillExpandListener
{
  private File         selectedFile;
  private JButton      btnApprove;
  private JButton      btnCancel;
  private JTree        tree;
  private FileTreeNode rootNode;


  public static File selectDirectory( Window owner )
  {
    DirSelectDlg dlg = new DirSelectDlg( owner );
    dlg.setVisible( true );
    return dlg.selectedFile;
  }


	/* --- TreeSelectionListener --- */

  @Override
  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      this.btnApprove.setEnabled( getSelectedFile() != null );
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
	if( o instanceof FileTreeNode ) {
	  refreshNode( (FileTreeNode) o );
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
	else if( src == this.btnCancel ) {
	  rv = true;
	  this.selectedFile = null;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Konstruktoren und Mothoden --- */

  private DirSelectDlg( Window owner )
  {
    super( owner, "Verzeichnisauswahl" );


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

    this.rootNode = new FileTreeNode( null, null, true );
    refreshNode( this.rootNode );

    this.tree = new JTree( this.rootNode );
    this.tree.setSelectionModel( selModel );
    this.tree.setEditable( false );
    this.tree.setRootVisible( false );
    this.tree.setScrollsOnExpand( true );
    this.tree.setShowsRootHandles( true );
    add( new JScrollPane( this.tree ), gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApprove = new JButton( "Ausw\u00E4hlen" );
    this.btnApprove.setEnabled( false );
    panelBtn.add( this.btnApprove );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Listener
    this.tree.addTreeSelectionListener( this );
    this.tree.addTreeWillExpandListener( this );
    this.tree.addKeyListener( this );

    this.btnApprove.addActionListener( this );
    this.btnApprove.addKeyListener( this );

    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );


    // Fenstergroesse
    setSize( 300, 300 );
    setResizable( true );
    setParentCentered();
  }


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


  private File getSelectedFile()
  {
    File     rv = null;
    TreePath tp = this.tree.getSelectionPath();
    if( tp != null ) {
      Object o = tp.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileTreeNode ) {
	  rv = ((FileTreeNode) o).getFile();
	}
      }
    }
    return rv;
  }


  private void refreshNode( FileTreeNode node )
  {
    if( !node.hasChildrenLoaded() ) {
      try {
	boolean fsRoot  = false;
	File[]  entries = null;
	File    file    = node.getFile();
	if( file != null ) {
	  entries = file.listFiles();
	} else {
	  entries = File.listRoots();
	  fsRoot  = true;
	}
	if( entries != null ) {
	  Arrays.sort( entries );
	  for( int i = 0; i < entries.length; i++ ) {
	    File f = entries[ i ];
	    if( f.isDirectory() && (fsRoot || !f.isHidden()) ) {
	      node.add( new FileTreeNode( node, f, fsRoot ) );
	    }
	  }
	}
      }
      catch( Exception ex ) {}
      node.setChildrenLoaded( true );
    }
  }
}

