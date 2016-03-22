/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateiauswahldialog
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.lang.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.Document;
import javax.swing.table.*;
import jkcemu.Main;


public class FileSelectDlg
			extends BasicDlg
			implements
				AbstractFileWorker.PathListener,
				DocumentListener,
				DropTargetListener,
				FocusListener,
				ListSelectionListener
{
  public enum Mode { SAVE, LOAD, MULTIPLE_LOAD };

  public static class DirItem
  {
    private File dirFile;
    private int  level;

    public DirItem( File dirFile, int level )
    {
      this.dirFile = dirFile;
      this.level   = level;
    }

    public File getDirectory()
    {
      return this.dirFile;
    }

    public int getLevel()
    {
      return this.level;
    }
  };


  private static final String defaultStatusText = "Bereit";

  private Mode                                 mode;
  private boolean                              startSelected;
  private boolean                              loadWithOptionsSelected;
  private boolean                              loadable;
  private boolean                              startable;
  private javax.swing.filechooser.FileFilter[] fileFilters;
  private File                                 curDir;
  private File                                 selectedFile;
  private java.util.List<File>                 selectedFiles;
  private FileSystemView                       fsv;
  private JList<File>                          list;
  private JScrollPane                          scrollPane;
  private JTextField                           fldFileName;
  private JComboBox<String>                    comboFileType;
  private JComboBox<DirItem>                   comboDir;
  private JButton                              btnCreateDir;
  private JButton                              btnGoUp;
  private JButton                              btnApprove;
  private JButton                              btnStart;
  private JButton                              btnLoadWithOptions;
  private JButton                              btnCancel;
  private JLabel                               labelStatus;
  private JPopupMenu                           mnuPopup;
  private JMenuItem                            mnuCreateDir;
  private JMenuItem                            mnuDelete;
  private JMenuItem                            mnuGoUp;
  private JMenuItem                            mnuRefresh;
  private JMenuItem                            mnuRename;
  private DefaultComboBoxModel<DirItem>        modelDir;
  private Document                             docFileName;
  private java.util.List<DirItem>              baseDirItems;
  private Cursor                               defaultCursor;
  private Cursor                               waitCursor;
  private String                               approveBtnText;


  public FileSelectDlg(
		Window                                owner,
		Mode                                  mode,
		boolean                               startEnabled,
		boolean                               loadWithOptionsEnabled,
		String                                title,
		File                                  preSelection,
		javax.swing.filechooser.FileFilter... fileFilters )
  {
    super( owner, title );
    this.mode                    = mode;
    this.startSelected           = false;
    this.loadWithOptionsSelected = false;
    this.loadable                = false;
    this.startable               = false;
    this.fileFilters             = fileFilters;
    this.curDir                  = null;
    this.selectedFile            = null;
    this.selectedFiles           = null;
    this.fsv                     = FileSystemView.getFileSystemView();
    this.baseDirItems            = new ArrayList<>();

    File   defaultDir = null;
    File[] roots      = null;
    if( this.fsv != null ) {
      roots = this.fsv.getRoots();
      defaultDir = this.fsv.getDefaultDirectory();
      if( defaultDir == null ) {
	File homeDir = this.fsv.getHomeDirectory();
	if( homeDir != null ) {
	  defaultDir = homeDir;
	}
      }
    }
    if( roots == null ) {
      roots = File.listRoots();
    }
    if( roots != null ) {
      EmuUtil.sortFilesByName( roots );
      for( int i = 0; i < roots.length; i++ ) {
	File file = roots[ i ];
	if( file.isDirectory() && !file.isHidden() ) {
	  this.baseDirItems.add( new DirItem( file, 0 ) );
	}
      }
    }
    if( (this.fsv != null) && (roots != null) ) {
      if( roots.length == 1 ) {
	File rootFile = roots[ 0 ];
	if( rootFile.isDirectory() ) {
	  boolean unixLikeOS   = Main.isUnixLikeOS();
	  File    computerNode = null;
	  File[]  subFiles     = this.fsv.getFiles( roots[ 0 ], true );
	  if( subFiles != null ) {
	    EmuUtil.sortFilesByName( subFiles );
	    for( int i = subFiles.length - 1; i >= 0; --i ) {
	      File file = subFiles[ i ];
	      if( this.fsv.isComputerNode( file ) ) {
		addDirItem( this.baseDirItems, file );
		computerNode = file;
		if( unixLikeOS ) {
		  break;
		}
	      } else {
		if( !unixLikeOS ) {
		  addDirItem( this.baseDirItems, file );
		}
	      }
	    }
	  }
	  if( !unixLikeOS && (computerNode != null) ) {
	    if( computerNode.isDirectory() ) {
	      subFiles = this.fsv.getFiles( computerNode, true );
	      if( subFiles != null ) {
		EmuUtil.sortFilesByName( subFiles );
		for( int i = subFiles.length - 1; i >= 0; --i ) {
		  addDirItem( this.baseDirItems, subFiles[ i ] );
		}
	      }
	    }
	  }
	}
      }
    }
    if( defaultDir == null ) {
      String homeDirText = System.getProperty( "user.home" );
      if( homeDirText != null ) {
	if( !homeDirText.isEmpty() ) {
	  defaultDir = new File( homeDirText );
	}
      }
    }
    if( defaultDir != null ) {
      addDirItem( this.baseDirItems, defaultDir );
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 10, 10, 5, 5 ),
					0, 0 );


    // Verzeichnisauswahl
    add( new JLabel( "Suchen in:" ), gbc );
    this.modelDir = new DefaultComboBoxModel<>();
    this.comboDir = new JComboBox<>( this.modelDir );
    this.comboDir.setEditable( false );
    this.comboDir.setRenderer( new FileSelectRenderer( this.fsv ) );
    this.comboDir.addActionListener( this );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboDir, gbc );

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( toolBar, gbc );

    this.btnGoUp = createImageButton(
			"/images/file/folder_up.png",
			"Eine Ebene h\u00F6her" );
    toolBar.add( this.btnGoUp, gbc );

    this.btnCreateDir = createImageButton(
			"/images/file/createdir.png",
			"Neues Verzeichnis erstellen" );
    toolBar.add( this.btnCreateDir, gbc );


    // Liste
    this.list = new JList<>();
    this.list.setDragEnabled( false );
    this.list.setLayoutOrientation( JList.VERTICAL_WRAP );
    this.list.setSelectionMode(
	this.mode.equals( Mode.MULTIPLE_LOAD ) ?
		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
		:ListSelectionModel.SINGLE_SELECTION );
    this.list.setVisibleRowCount( 0 );
    this.list.setCellRenderer( new FileSelectRenderer( this.fsv ) );
    this.list.addKeyListener( this );
    this.list.addMouseListener( this );
    this.list.addListSelectionListener( this );
    this.scrollPane = new JScrollPane( this.list );
    gbc.fill        = GridBagConstraints.BOTH;
    gbc.weightx     = 1.0;
    gbc.weighty     = 1.0;
    gbc.insets.top  = 5;
    gbc.insets.left = 10;
    gbc.gridwidth   = 3;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.scrollPane, gbc );


    // Dateiname
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.weighty   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridy++;
    add( new JLabel( "Dateiname:" ), gbc );

    this.fldFileName = new JTextField();
    this.fldFileName.addActionListener( this );
    this.fldFileName.addFocusListener( this );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 5;
    gbc.gridwidth   = 2;
    gbc.gridx++;
    add( this.fldFileName, gbc );


    // Dateityp
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.top  = 0;
    gbc.insets.left = 10;
    gbc.gridwidth   = 1;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( new JLabel( "Dateityp:" ), gbc );

    this.comboFileType = new JComboBox<>();
    this.comboFileType.setEditable( false );
    this.comboFileType.addItem( "Alle Dateien" );

    int idx = 0;
    if( fileFilters != null ) {
      if( fileFilters.length == 1 ) {
	if( fileFilters[ 0 ] != null ) {
	  this.comboFileType.addItem( fileFilters[ 0 ].getDescription() );
	  idx = 1;
	}
      } else {
	for( int i = 0; i < fileFilters.length; i++ ) {
	  if( fileFilters[ i ] != null ) {
	    this.comboFileType.addItem( fileFilters[ i ].getDescription() );
	  }
	}
      }
    }
    try {
      this.comboFileType.setSelectedIndex( idx );
    }
    catch( IllegalArgumentException ex ) {}
    this.comboFileType.addActionListener( this );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 5;
    gbc.gridwidth   = 2;
    gbc.gridx++;
    add( this.comboFileType, gbc );


    // Statuszeile
    gbc.insets.left  = 0;
    gbc.insets.right = 0;
    gbc.insets.top   = 5;
    gbc.gridwidth    = GridBagConstraints.REMAINDER;
    gbc.gridx        = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.labelStatus = new JLabel( defaultStatusText );
    gbc.insets.left  = 10;
    gbc.insets.right = 10;
    gbc.insets.top   = 0;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Knoepfe
    int numBtns = 2;
    if( startEnabled ) {
      numBtns++;
    }
    if( loadWithOptionsEnabled ) {
      numBtns++;
    }

    JPanel panelBtn = new JPanel( new GridLayout( numBtns, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridwidth   = 1;
    gbc.gridheight  = GridBagConstraints.REMAINDER;
    gbc.gridy       = 0;
    gbc.gridx       = 4;
    add( panelBtn, gbc );

    this.approveBtnText = "\u00D6ffnen";
    if( isForSave() ) {
      this.approveBtnText = "Speichern";
    } else {
      if( loadWithOptionsEnabled ) {
	this.approveBtnText = "Laden";
      }
    }
    this.btnApprove = new JButton( this.approveBtnText );
    this.btnApprove.addActionListener( this );
    this.btnApprove.addKeyListener( this );
    panelBtn.add( this.btnApprove );

    this.btnStart = null;
    if( startEnabled ) {
      this.btnStart = new JButton( "Starten" );
      this.btnStart.addActionListener( this );
      this.btnStart.addKeyListener( this );
      panelBtn.add( this.btnStart );
    }

    this.btnLoadWithOptions = null;
    if( loadWithOptionsEnabled ) {
      this.btnLoadWithOptions = new JButton( "Laden mit..." );
      this.btnLoadWithOptions.addActionListener( this );
      this.btnLoadWithOptions.addKeyListener( this );
      panelBtn.add( this.btnLoadWithOptions );
    }

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuGoUp = new JMenuItem( "Eine Ebene h\u00F6her" );
    this.mnuGoUp.addActionListener( this );
    this.mnuPopup.add( this.mnuGoUp );

    this.mnuCreateDir = new JMenuItem( "Verzeichnis erstellen..." );
    this.mnuCreateDir.addActionListener( this );
    this.mnuPopup.add( this.mnuCreateDir );
    this.mnuPopup.addSeparator();

    this.mnuRename = new JMenuItem( "Umbenennen..." );
    this.mnuRename.setEnabled( false );
    this.mnuRename.addActionListener( this );
    this.mnuPopup.add( this.mnuRename );

    this.mnuDelete = new JMenuItem( "L\u00F6schen" );
    this.mnuDelete.setEnabled( false );
    this.mnuDelete.addActionListener( this );
    this.mnuPopup.add( this.mnuDelete );
    this.mnuPopup.addSeparator();

    this.mnuRefresh = new JMenuItem( "Aktualisieren" );
    this.mnuRefresh.addActionListener( this );
    this.mnuPopup.add( this.mnuRefresh );


    // Cursors
    this.defaultCursor = this.list.getCursor();
    if( this.defaultCursor == null ) {
      this.defaultCursor = Cursor.getDefaultCursor();
    }
    this.waitCursor = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );


    // Vorbelegung
    if( preSelection != null ) {
      if( preSelection.isDirectory() ) {
	setCurDir( preSelection, null );
      } else {
	File parentDir = null;
	if( this.fsv != null ) {
	  parentDir = this.fsv.getParentDirectory( preSelection );
	}
	if( parentDir == null ) {
	  parentDir = preSelection.getParentFile();
	}
	setCurDir( parentDir != null ? parentDir : defaultDir, null );
	this.fldFileName.setText( preSelection.getName() );
	java.util.List<File> preSelections
		= Collections.singletonList( preSelection );
	if( selectFiles( preSelections ) == 0 ) {
	  filesSelected( preSelections );
	}
      }
    } else {
      setCurDir( defaultDir, null );
    }
    this.docFileName = this.fldFileName.getDocument();
    if( this.docFileName != null ) {
      this.docFileName.addDocumentListener( this );
      if( this.btnStart != null ) {
	this.btnStart.setEnabled( false );
      }
      if( this.btnLoadWithOptions != null ) {
	this.btnLoadWithOptions.setEnabled( false );
      }
    }
    updApproveBtn( false );


    // Drop aktivieren
    (new DropTarget( this.fldFileName, this )).setActive( true );


    // Fenstergroesse und -position
    setSize( 600, 400 );
    setParentCentered();
    setResizable( true );
  }


  public File getSelectedFile()
  {
    return this.selectedFile;
  }


  public java.util.List<File> getSelectedFiles()
  {
    return this.selectedFiles;
  }


  public boolean isLoadWithOptionsSelected()
  {
    return this.loadWithOptionsSelected;
  }


  public boolean isStartSelected()
  {
    return this.startSelected;
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
    updList( null );
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
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
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      this.fldFileName.setText( file.getPath() );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    if( !e.isTemporary() && (e.getComponent() == this.fldFileName) ) {
      final JList list = this.list;
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    list.clearSelection();
			  }
			} );
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
    if( e.getSource() == this.list ) {
      this.loadable  = false;
      this.startable = false;

      boolean              dirSelected = false;
      String               fileName    = null;
      java.util.List<File> files       = this.list.getSelectedValuesList();
      for( File file : files ) {
	if( file.isDirectory() ) {
	  dirSelected = true;
	  break;
	}
	String s = file.getName();
	if( s != null ) {
	  if( s.isEmpty() ) {
	    s = null;
	  }
	}
	if( fileName != null ) {
	  fileName = "";
	} else {
	  fileName = s;
	}
      }
      if( !dirSelected && (fileName != null) ) {
	if( this.docFileName != null ) {
	  this.docFileName.removeDocumentListener( this );
	  this.fldFileName.setText( fileName );
	  this.docFileName.addDocumentListener( this );
	} else {
	  this.fldFileName.setText( fileName );
	}
      }
      this.mnuDelete.setEnabled( !files.isEmpty() );
      this.mnuRename.setEnabled( files.size() == 1 );
      updApproveBtn( dirSelected );

      String statusText = filesSelected( files );
      this.labelStatus.setText(
		statusText != null ? statusText : defaultStatusText );
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
	if( (src == this.comboDir)
	    || (src == this.comboFileType) )
	{
	  rv = true;
	  if( !isForSave() ) {
	    this.fldFileName.setText( "" );
	  }
	  updList( null );
	}
	else if( src == this.list ) {
	  rv = true;
	  doListAction( this.list.getSelectedIndex() );
	}
	else if( (src == this.btnGoUp) || (src == this.mnuGoUp) ) {
	  rv = true;
	  doGoUp();
	}
	else if( (src == this.btnCreateDir) || (src == this.mnuCreateDir) ) {
	  rv = true;
	  doCreateDir();
	}
	else if( src == this.btnApprove ) {
	  rv = true;
	  doApprove( false, false );
	}
	else if( src == this.btnStart ) {
	  rv = true;
	  doApprove( true, false );
	}
	else if( src == this.btnLoadWithOptions ) {
	  rv = true;
	  doApprove( false, true );
	}
	else if( src == this.fldFileName ) {
	  rv = true;
	  doFileNameAction();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
	else if( src == this.mnuDelete ) {
	  rv = true;
	  doDelete();
	}
	else if( src == this.mnuRefresh ) {
	  rv = true;
	  doRefresh();
	}
	else if( src == this.mnuRename ) {
	  rv = true;
	  doRename();
	}
      }
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e != null ) {
      if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
	doApprove( false, false );
	e.consume();
      }
    }
  }


  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.list)
	&& (e.getClickCount() > 1)
	&& (e.getButton() == MouseEvent.BUTTON1) )
    {
      Point pt = e.getPoint();
      if( pt != null ) {
	int idx = this.list.locationToIndex( pt );
	if( idx >= 0 ) {
	  doListAction( idx );
	}
      }
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


  @Override
  protected boolean showPopup( MouseEvent e )
  {
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      this.mnuPopup.show( c, e.getX(), e.getY() );
      rv = true;
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.list.requestFocus();
  }


	/* --- Aktionen --- */

  private void doApprove(
			boolean startSelected,
			boolean loadWithOptionsSelected )
  {
    java.util.List<File> files = this.list.getSelectedValuesList();
    if( (files.isEmpty()) && (this.curDir != null) ) {
      String fileName = this.fldFileName.getText();
      if( fileName != null ) {
	fileName = fileName.trim();
	if( !fileName.isEmpty() ) {
	  File file = new File( fileName );
	  if( !file.isAbsolute() ) {
	    if( this.fsv != null ) {
	      file = this.fsv.getChild( this.curDir, fileName );
	    } else {
	      file = new File( this.curDir, fileName );
	    }
	  }
	  files = Collections.singletonList( file );
	}
      }
    }
    approveFiles( files, startSelected, loadWithOptionsSelected );
  }


  private void doCreateDir()
  {
    if( this.curDir != null ) {
      File dirFile = EmuUtil.createDir( this, this.curDir );
      if( dirFile != null ) {
	setCurDir( dirFile, null );
      }
    }
  }


  private void doDelete()
  {
    int idx = this.list.getSelectedIndex();
    if( idx >= 0 ) {
      java.util.List<File> files  = this.list.getSelectedValuesList();
      int                  nFiles = files.size();
      if( nFiles > 0 ) {
	File file = null;
	try {
	  java.util.List<Path> paths = new ArrayList<>( nFiles );
	  for( File f : files ) {
	    file = f;
	    paths.add( f.toPath() );
	  }
	  FileRemover.startRemove( this, paths, this, null );
	}
	catch( InvalidPathException ex ) {
	  String msg = null;
	  if( file != null ) {
	    msg = file.getPath();
	  }
	  if( msg != null ) {
	    if( msg.isEmpty() ) {
	      msg = null;
	    }
	  }
	  if( msg != null ) {
	    msg += "\nkonnte nicht gel\u00F6scht werden.";
	  } else {
	    msg = "L\u00F6schen fehlgeschlagen";
	  }
	  showErrorDlg( this, msg );
	}
      }
    }
  }


  private void doFileNameAction()
  {
    boolean done = false;
    if( this.curDir != null ) {
      String text = this.fldFileName.getText();
      if( text != null ) {
	text = text.trim();
	if( !text.isEmpty() ) {
	  File   file  = new File( text );
	  String fName = file.getName();
	  if( fName != null ) {
	    if( (fName.indexOf( '?' ) < 0) && (fName.indexOf( '*' ) < 0) ) {
	      if( !file.isAbsolute() ) {
		if( this.fsv != null ) {
		  file = this.fsv.getChild( this.curDir, fName );
		} else {
		  file = new File( this.curDir, fName );
		}
	      }
	      approveFiles( Collections.singletonList( file ), false, true );
	      done = true;
	    }
	  }
	}
      }
    }
    updList( null );
  }


  private void doGoUp()
  {
    File upDir = null;
    if( this.curDir != null ) {
      if( this.fsv != null ) {
	upDir = this.fsv.getParentDirectory( this.curDir );
      } else {
	upDir = this.curDir.getParentFile();
      }
    }
    setCurDir( upDir, this.curDir );
  }


  private void doListAction( int idx )
  {
    if( idx >= 0 ) {
      ListModel model = this.list.getModel();
      if( model != null ) {
	if( idx < model.getSize() ) {
	  Object o = model.getElementAt( idx );
	  if( o != null ) {
	    if( o instanceof File ) {
	      approveFiles(
			Collections.singletonList( (File) o ),
			false,
			true );
	    }
	  }
	}
      }
    }
  }


  private void doRefresh()
  {
    updList( this.list.getSelectedValuesList() );
  }


  private void doRename()
  {
    java.util.List<File> files = this.list.getSelectedValuesList();
    if( files.size() == 1 ) {
      File file = EmuUtil.renameFile( this, files.get( 0 ) );
      if( file != null ) {
	updList( Collections.singletonList( file ) );
      }
    }
  }


	/* --- private Methoden --- */

  private DirItem addDirItem( java.util.List<DirItem> dstList, File file )
  {
    DirItem rv = null;
    if( file != null ) {
      if( file.isDirectory() ) {
	java.util.List<File> srcList = null;
	int                  dstPos  = -1;
	while( file != null ) {
	  dstPos = getIndexOf( dstList, file );
	  if( dstPos >= 0 ) {
	    break;
	  }
	  if( srcList == null ) {
	    srcList = new ArrayList<>();
	  }
	  srcList.add( file );
	  if( this.fsv != null ) {
	    file = this.fsv.getParentDirectory( file );
	  } else {
	    file = file.getParentFile();
	  }
	}
	int level = 0;
	if( (dstPos >= 0) && (dstPos < dstList.size()) ) {
	  rv    = dstList.get( dstPos );
	  level = rv.getLevel() + 1;
	  dstPos++;
	}
	if( srcList != null ) {
	  if( dstPos >= dstList.size() ) {
	    dstPos = -1;	// nicht einfuegen, sondern anhaengen
	  }
	  int srcLen = srcList.size();
	  for( int i = srcLen - 1; i >= 0; --i ) {
	    rv = new DirItem( srcList.get( i ), level++ );
	    if( dstPos >= 0 ) {
	      dstList.add( dstPos++, rv );
	    } else {
	      dstList.add( rv );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void approveFiles(
			java.util.List<File> files,
			boolean              startSelected,
			boolean              loadWithOptionsSelected )
  {
    if( files != null ) {
      boolean done = false;
      for( File file : files ) {
	if( file.isDirectory() ) {
	  setCurDir( file, null );
	  done = true;
	  break;
	}
      }
      if( !done && !files.isEmpty() ) {
	File file = files.get( 0 );

	// spezielle Behandlung beim Speichern
	if( isForSave() && (files.size() == 1) ) {

	  // Dateiname aufbereiten
	  String fName = file.getName();
	  if( fName != null ) {
	    if( fName.startsWith( "\"" ) && fName.endsWith( "\"" ) ) {
	      /*
	       * Dateiname in doppelten Anfuehrungsstriche
	       * unveraendert uebernehmen
	       */
	      if( fName.length() > 2 ) {
		fName = fName.substring( 1, fName.length() - 1 );
	      } else {
		fName = "";
	      }
	      file = replaceName( file, fName );
	    } else {
	      /*
	       * Dateiendung automatisch anhaengen,
	       * wenn laut Dateifilter nur eine Endung angegeben ist
	       */
	      if( this.fileFilters != null ) {
		if( this.fileFilters.length == 1 ) {
		  javax.swing.filechooser.FileFilter ff = fileFilters[ 0 ];
		  if( ff != null ) {
		    if( ff instanceof FileNameExtensionFilter ) {
		      String[] extensions = ((FileNameExtensionFilter) ff)
							.getExtensions();
		      if( extensions != null ) {
			if( extensions.length == 1 ) {
			  String ext = extensions[ 0 ];
			  if( ext != null ) {
			    if( !fName.toUpperCase().endsWith(
						ext.toUpperCase() ) )
			    {
			      file = replaceName(
						file,
						String.format(
							"%s.%s",
							fName,
							ext ) );
			    }
			  }
			}
		      }
		    }
		  }
		}
	      }
	    }
	  }

	  // bei Vorhandensein der Datei warnen
	  if( file.exists() ) {
	    if( !BasicDlg.showYesNoWarningDlg(
			this,
			"Die Datei \'" + file.getName()
				+ "\' existiert bereits.\n"
				+ "M\u00F6chten Sie die Datei"
				+ " \u00FCberschreiben?",
			"Best\u00E4tigung" ) )
	    {
	      file  = null;
	      files = null;
	    }
	  }
	}
	if( file != null ) {
	  this.selectedFile            = file;
	  this.selectedFiles           = files;
	  this.startSelected           = startSelected;
	  if( this.mode.equals( Mode.LOAD ) ) {
	    this.loadWithOptionsSelected = loadWithOptionsSelected;
	  } else {
	    this.loadWithOptionsSelected = false;
	  }
	  doClose();
	}
      }
    }
  }


  public static Pattern compileFileNameMask( String text )
  {
    Pattern pattern = null;
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	final String  specialChars = "\\.[]()^$";
	boolean       processed    = false;
	StringBuilder buf = new StringBuilder( len + 10 );
	for( int i = 0; i < len; i++ ) {
	  processed = false;
	  char ch   = text.charAt( i );
	  if( ch == '*' ) {
	    buf.append( ".*" );
	    processed = true;
	  }
	  if( !processed && (ch == '?') ) {
	    buf.append( "." );
	    processed = true;
	  }
	  if( !processed ) {
	    if( specialChars.indexOf( ch ) >= 0 ) {
	      buf.append( (char) '\\' );
	      buf.append( ch );
	      processed = true;
	    }
	  }
	  if( !processed ) {
	    buf.append( ch );
	  }
	}
	try {
	  int flags = Pattern.DOTALL;
	  if( !Main.isUnixLikeOS() ) {
	    flags |= Pattern.CASE_INSENSITIVE;
	  }
	  pattern = Pattern.compile( buf.toString(), flags );
	}
	catch( PatternSyntaxException ex ) {}
      }
    }
    return pattern;
  }


  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docFileName )
      updApproveBtn( false );
  }


  private String filesSelected( java.util.List<File> files )
  {
    String statusText = null;
    File  file        = null;
    if( files != null ) {
      if( files.size() == 1 ) {
	file = files.get( 0 );
	if( file.isFile() && file.canRead() ) {
	  FileInfo fileInfo = FileInfo.analyzeFile( file );
	  if( fileInfo != null ) {
	    if( !fileInfo.isKCBasicProgramFormat()
		&& (fileInfo.getBegAddr() >= 0) )
	    {
	      this.loadable = true;
	      if( fileInfo.getStartAddr() >= 0 ) {
		this.startable = true;
	      }
	    }
	    statusText = fileInfo.getInfoText();
	  }
	}
      }
    }
    if( this.btnApprove != null ) {
      this.btnApprove.setEnabled(
		(this.mode.equals( Mode.LOAD ) && this.loadable)
		|| (this.btnLoadWithOptions == null) );
    }
    if( this.btnStart != null ) {
      this.btnStart.setEnabled(
		this.mode.equals( Mode.LOAD ) && this.startable );
    }
    if( this.btnLoadWithOptions != null ) {
      this.btnLoadWithOptions.setEnabled(
		this.mode.equals( Mode.LOAD ) && (file != null) );
    }
    if( statusText != null ) {
      if( statusText.isEmpty() ) {
	statusText = null;
      }
    }
    return statusText;
  }


  private void fireSetListData(
			final File                 dirFile,
			final File[]               files,
			final java.util.List<File> filesToSelect )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    setListData( dirFile, files, filesToSelect );
		  }
		} );
  }


  private static int getIndexOf( java.util.List<DirItem> list, File file )
  {
    int rv = -1;
    if( (list != null) && (file != null) ) {
      int i = 0;
      for( DirItem item : list ) {
	if( EmuUtil.equals( item.getDirectory(), file ) ) {
	  rv = i;
	  break;
	}
	i++;
      }
    }
    return rv;
  }


  private boolean isForSave()
  {
    return this.mode.equals( Mode.SAVE );
  }


  private static File replaceName( File file, String fName )
  {
    if( file != null ) {
      File parent = file.getParentFile();
      if( parent != null ) {
	file = new File( parent, fName );
      } else {
	file = new File( fName );
      }
    }
    return file;
  }


  private int selectFiles( Collection<File> filesToSelect )
  {
    int rv = 0;
    this.list.clearSelection();
    if( filesToSelect != null ) {
      if( !filesToSelect.isEmpty() ) {
	ListModel<File> model = this.list.getModel();
	if( model != null ) {
	  Set<String> pathsToSelect = new TreeSet<>();
	  for( File file : filesToSelect ) {
	    String path = file.getPath();
	    if( path != null ) {
	      if( pathsToSelect == null ) {
		pathsToSelect = new TreeSet<>();
	      }
	      pathsToSelect.add( path );
	    }
	  }
	  int n = model.getSize();
	  for( int i = 0; i < n; i++ ) {
	    String path = model.getElementAt( i ).getPath();
	    if( path != null ) {
	      if( pathsToSelect.contains( path ) ) {
		this.list.addSelectionInterval( i, i );
		rv++;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void setCurDir( File dirFile, File fileToSelect )
  {
    if( !isForSave() ) {
      this.fldFileName.setText( "" );
    }
    this.comboDir.removeActionListener( this );
    DirItem defaultDirItem        = null;
    java.util.List<DirItem> items = new ArrayList<>();
    items.addAll( this.baseDirItems );
    if( dirFile != null ) {
      defaultDirItem = addDirItem( items, dirFile );
    }
    this.modelDir.removeAllElements();
    for( DirItem item : items ) {
      this.modelDir.addElement( item );
    }
    if( defaultDirItem != null ) {
      this.comboDir.setSelectedItem( defaultDirItem );
    }
    updList( fileToSelect != null ?
			Collections.singletonList( fileToSelect )
			: null );
    this.comboDir.addActionListener( this );
  }


  private void setListData(
			File                       dirFile,
			File[]                     files,
			final java.util.List<File> filesToSelect )
  {
    if( (this.curDir != null) && (dirFile != null) && (files != null) ) {
      if( this.curDir.equals( dirFile ) ) {
	javax.swing.filechooser.FileFilter fileFilter = null;
	EmuUtil.sortFilesByName( files );
	if( this.fileFilters != null ) {
	  int idx = this.comboFileType.getSelectedIndex() - 1;
	  if( (idx >= 0) && (idx < this.fileFilters.length) ) {
	    fileFilter = this.fileFilters[ idx ];
	  }
	}
	Pattern pattern = null;
	String  mask    = this.fldFileName.getText();
	if( mask != null ) {
	  if( (mask.indexOf( '*' ) >= 0) || (mask.indexOf( '?' ) >= 0) ) {
	    pattern = compileFileNameMask( mask );
	    if( (pattern != null) && (fileFilter != null) ) {
	      try {
		this.comboFileType.removeActionListener( this );
		this.comboFileType.setSelectedIndex( 0 );
		this.comboFileType.addActionListener( this );
		fileFilter = null;
	      }
	      catch( IllegalArgumentException ex ) {}
	    }
	  }
	}
	java.util.List<File> entries = new ArrayList<>(
					files.length > 1 ? files.length : 1 );

	for( int i = 0; i < files.length; i++ ) {
	  File file = files[ i ];
	  if( file.isDirectory() && !file.isHidden() ) {
	    String name = file.getName();
	    if( name != null ) {
	      if( !name.equals( "." ) && !name.equals( ".." ) ) {
		entries.add( file );
	      }
	    }
	  }
	}
	for( int i = 0; i < files.length; i++ ) {
	  File file = files[ i ];
	  if( !file.isDirectory() && !file.isHidden() ) {
	    if( pattern != null ) {
	      String fName = file.getName();
	      if( fName != null ) {
		Matcher m = pattern.matcher( fName );
		if( m != null ) {
		  if( m.matches() ) {
		    entries.add( file );
		  }
		}
	      }
	    } else {
	      if( fileFilter != null ) {
		if( fileFilter.accept( file ) ) {
		  entries.add( file );
		}
	      } else {
		entries.add( file );
	      }
	    }
	  }
	}
	this.list.setListData(
			entries.toArray( new File[ entries.size() ] ) );
	if( filesToSelect != null ) {
	  if( !filesToSelect.isEmpty() ) {
	    EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    selectFiles( filesToSelect );
			  }
			} );
	  }
	}
      }
    }
    if( this.defaultCursor != null ) {
      this.list.setCursor( this.defaultCursor );
      this.scrollPane.setCursor( this.defaultCursor );
      JViewport vp = this.scrollPane.getViewport();
      if( vp != null ) {
	vp.setCursor( this.defaultCursor );
      }
    }
    this.labelStatus.setText( defaultStatusText );
  }


  private void updApproveBtn( boolean dirSelected )
  {
    if( this.btnApprove != null ) {
      boolean stateApprove         = false;
      boolean stateStart           = false;
      boolean stateLoadWithOptions = false;
      if( dirSelected ) {
	this.btnApprove.setText( "\u00D6ffnen" );
	stateApprove = true;
      } else {
	this.btnApprove.setText( this.approveBtnText );
	boolean stateSelection = (this.list.getSelectedIndex() > 0);
	if( !stateSelection ) {
	  if( this.docFileName != null ) {
	    String fileName = this.fldFileName.getText();
	    if( fileName != null ) {
	      fileName = fileName.trim();
	      if( !fileName.isEmpty() ) {
		stateSelection = true;
	      }
	    }
	  } else {
	    stateSelection = true;
	  }
	}
	if( this.btnLoadWithOptions != null ) {
	  stateApprove         = this.loadable;
	  stateStart           = this.startable;
	  stateLoadWithOptions = stateSelection;
	} else {
	  stateApprove = stateSelection;
	}
      }
      this.btnApprove.setEnabled( stateApprove );
      if( this.btnStart != null ) {
	this.btnStart.setEnabled( stateStart );
      }
      if( this.btnLoadWithOptions != null ) {
	this.btnLoadWithOptions.setEnabled( stateLoadWithOptions );
      }
    }
  }


  private void updList( final java.util.List<File> filesToSelect )
  {
    this.curDir    = null;
    Object dirItem = this.comboDir.getSelectedItem();
    if( dirItem != null ) {
      if( dirItem instanceof DirItem ) {
	this.curDir = ((DirItem) dirItem).getDirectory();
      }
    }
    this.list.setListData( new File[ 0 ] );
    if( this.curDir != null ) {
      this.btnCreateDir.setEnabled( true );
      this.mnuCreateDir.setEnabled( true );
      boolean state = false;
      if( this.fsv != null ) {
	state = (this.fsv.getParentDirectory( this.curDir ) != null);
      } else {
	state = (this.curDir.getParentFile() != null);
      }
      this.btnGoUp.setEnabled( state );
      this.mnuGoUp.setEnabled( state );
    } else {
      this.btnCreateDir.setEnabled( false );
      this.mnuCreateDir.setEnabled( false );
      this.btnGoUp.setEnabled( false );
      this.mnuGoUp.setEnabled( false );
    }
    final FileSystemView fsv     = this.fsv;
    final File           dirFile = this.curDir;
    if( curDir != null ) {
      if( (this.defaultCursor != null) && (this.waitCursor != null) ) {
	this.list.setCursor( this.waitCursor );
	this.scrollPane.setCursor( this.waitCursor );
	JViewport vp = this.scrollPane.getViewport();
	if( vp != null ) {
	  vp.setCursor( this.waitCursor );
	}
      }
      this.labelStatus.setText( "Lese Verzeichnis..." );
      Thread t = new Thread(
			Main.getThreadGroup(),
			"JKCEMU directory reader of file select dialog" )
		{
		  @Override
		  public void run()
		  {
		    File[] files = null;
		    try {
		      if( fsv != null ) {
			files = fsv.getFiles( dirFile, true );
		      } else {
			files = dirFile.listFiles();
		      }
		    }
		    catch( Exception ex ) {}
		    finally {
		      fireSetListData( dirFile, files, filesToSelect  );
		    }
		  }
		};
      t.start();
    }
  }
}
