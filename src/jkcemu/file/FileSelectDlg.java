/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateiauswahldialog
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.PopupMenuOwner;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;


public class FileSelectDlg
			extends BaseDlg
			implements
				AbstractFileWorker.PathListener,
				DocumentListener,
				DropTargetListener,
				FocusListener,
				ListSelectionListener,
				PopupMenuOwner
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

  private Mode                          mode;
  private boolean                       startSelected;
  private boolean                       loadWithOptionsSelected;
  private boolean                       loadable;
  private boolean                       startable;
  private java.util.List<FileFilter>    fileFilters;
  private File                          curDir;
  private File                          selectedFile;
  private java.util.List<File>          selectedFiles;
  private FileSelectRenderer            dirCellRenderer;
  private FileSelectRenderer            fileCellRenderer;
  private JList<File>                   list;
  private JScrollPane                   scrollPane;
  private JTextField                    fldFileName;
  private JComboBox<String>             comboFileType;
  private JComboBox<DirItem>            comboDir;
  private JButton                       btnCreateDir;
  private JButton                       btnGoUp;
  private JButton                       btnApprove;
  private JButton                       btnStart;
  private JButton                       btnLoadWithOptions;
  private JButton                       btnCancel;
  private JLabel                        labelStatus;
  private JPopupMenu                    popupMnu;
  private JMenuItem                     popupCreateDir;
  private JMenuItem                     popupDelete;
  private JMenuItem                     popupGoUp;
  private JMenuItem                     popupRefresh;
  private JMenuItem                     popupRename;
  private DefaultComboBoxModel<DirItem> modelDir;
  private Document                      docFileName;
  private java.util.List<DirItem>       baseDirItems;
  private Cursor                        defaultCursor;
  private Cursor                        waitCursor;
  private String                        approveBtnText;


  public FileSelectDlg(
		Window        owner,
		Mode          mode,
		boolean       startEnabled,
		boolean       loadWithOptionsEnabled,
		String        title,
		File          preSelection,
		FileFilter... fileFilters )
  {
    super( owner, title );
    this.mode                    = mode;
    this.startSelected           = false;
    this.loadWithOptionsSelected = false;
    this.loadable                = false;
    this.startable               = false;
    this.fileFilters             = null;
    this.curDir                  = null;
    this.selectedFile            = null;
    this.selectedFiles           = null;
    this.baseDirItems            = new ArrayList<>();
    this.dirCellRenderer         = new FileSelectRenderer();
    this.fileCellRenderer        = new FileSelectRenderer();

    DeviceIO.startFindUnreachableNetPaths(
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    repaintView();
				  }
				} );

    File   defaultDir  = null;
    String homeDirText = System.getProperty( "user.home" );
    if( homeDirText != null ) {
      if( !homeDirText.isEmpty() ) {
	defaultDir = new File( homeDirText );
      }
    }

    File[] roots = FileUtil.listRoots();
    if( roots != null ) {
      for( int i = 0; i < roots.length; i++ ) {
	File file = roots[ i ];
	boolean state = file.isDirectory() && !file.isHidden();
	if( !state && (defaultDir != null) ) {
	  if( defaultDir.getPath().startsWith( file.getPath() ) ) {
	    state = true;
	  }
	}
	if( state ) {
	  this.baseDirItems.add( new DirItem( file, 0 ) );
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
    add( GUIFactory.createLabel( FileUtil.LABEL_SEARCH_IN ), gbc );
    this.modelDir = new DefaultComboBoxModel<>();
    this.comboDir = GUIFactory.createComboBox( this.modelDir );
    this.comboDir.setEditable( false );
    this.comboDir.setRenderer( this.dirCellRenderer );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboDir, gbc );

    JToolBar toolBar = GUIFactory.createToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( toolBar, gbc );

    this.btnGoUp = GUIFactory.createRelImageResourceButton(
					this,
					"file/folder_up.png",
					"Eine Ebene h\u00F6her" );
    toolBar.add( this.btnGoUp, gbc );

    this.btnCreateDir = GUIFactory.createRelImageResourceButton(
					this,
					"file/createdir.png",
					"Neues Verzeichnis erstellen" );
    toolBar.add( this.btnCreateDir, gbc );


    // Liste
    this.list = GUIFactory.createList();
    this.list.setDragEnabled( false );
    this.list.setLayoutOrientation( JList.VERTICAL_WRAP );
    this.list.setSelectionMode(
	this.mode.equals( Mode.MULTIPLE_LOAD ) ?
		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
		: ListSelectionModel.SINGLE_SELECTION );
    this.list.setVisibleRowCount( 0 );
    this.list.setCellRenderer( this.fileCellRenderer );
    this.scrollPane = GUIFactory.createScrollPane( this.list );
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
    add( GUIFactory.createLabel( "Dateiname:" ), gbc );

    this.fldFileName = GUIFactory.createTextField();
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.weightx      = 1.0;
    gbc.insets.left  = 5;
    gbc.gridwidth    = 2;
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
    add( GUIFactory.createLabel( "Dateityp:" ), gbc );

    this.comboFileType = GUIFactory.createComboBox();
    this.comboFileType.setEditable( false );
    this.comboFileType.addItem( "Alle Dateien" );

    if( fileFilters != null ) {
      if( fileFilters.length > 0 ) {
	this.fileFilters = new ArrayList<>( fileFilters.length );
	for( int i = 0; i < fileFilters.length; i++ ) {
	  if( fileFilters[ i ] != null ) {
	    this.comboFileType.addItem( fileFilters[ i ].getDescription() );
	    this.fileFilters.add( fileFilters[ i ] );
	  }
	}
      }
    }
    try {
      this.comboFileType.setSelectedIndex(
			this.comboFileType.getItemCount() == 2 ? 1 : 0 );
    }
    catch( IllegalArgumentException ex ) {}
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
    add( GUIFactory.createSeparator(), gbc );

    this.labelStatus = GUIFactory.createLabel( defaultStatusText );
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

    JPanel panelBtn = GUIFactory.createPanel(
				new GridLayout( numBtns, 1, 5, 5 ) );
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

    this.approveBtnText = EmuUtil.TEXT_OPEN;
    if( isForSave() ) {
      this.approveBtnText = EmuUtil.TEXT_SAVE;
    } else {
      if( loadWithOptionsEnabled ) {
	this.approveBtnText = EmuUtil.TEXT_LOAD;
      }
    }
    this.btnApprove = GUIFactory.createButton( this.approveBtnText );
    panelBtn.add( this.btnApprove );

    this.btnStart = null;
    if( startEnabled ) {
      this.btnStart = GUIFactory.createButton( "Starten" );
      panelBtn.add( this.btnStart );
    }

    this.btnLoadWithOptions = null;
    if( loadWithOptionsEnabled ) {
      this.btnLoadWithOptions = GUIFactory.createButton( "Laden mit..." );
      panelBtn.add( this.btnLoadWithOptions );
    }

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Popup-Menu
    this.popupMnu = GUIFactory.createPopupMenu();

    this.popupGoUp = GUIFactory.createMenuItem( "Eine Ebene h\u00F6her" );
    this.popupMnu.add( this.popupGoUp );

    this.popupCreateDir = GUIFactory.createMenuItem(
					"Verzeichnis erstellen..." );
    this.popupMnu.add( this.popupCreateDir );
    this.popupMnu.addSeparator();

    this.popupRename = GUIFactory.createMenuItem( "Umbenennen..." );
    this.popupRename.setEnabled( false );
    this.popupMnu.add( this.popupRename );

    this.popupDelete = GUIFactory.createMenuItem( EmuUtil.TEXT_DELETE );
    this.popupDelete.setEnabled( false );
    this.popupMnu.add( this.popupDelete );
    this.popupMnu.addSeparator();

    this.popupRefresh = GUIFactory.createMenuItem( "Aktualisieren" );
    this.popupMnu.add( this.popupRefresh );


    // Cursors
    this.defaultCursor = this.list.getCursor();
    if( this.defaultCursor == null ) {
      this.defaultCursor = Cursor.getDefaultCursor();
    }
    this.waitCursor = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );


    // Vorbelegung
    boolean done = false;
    if( FileUtil.isUsable( preSelection ) ) {
      if( isForSave() ) {
	if( preSelection.exists() && preSelection.isDirectory() ) {
	  setCurDir( preSelection, null );
	  done = true;
	} else {
	  File parentDir = FileUtil.getParent( preSelection );
	  if( parentDir == null ) {
	    parentDir = preSelection.getParentFile();
	  }
	  if( parentDir != null ) {
	    if( parentDir.exists() ) {
	      setCurDir( parentDir, null );
	      this.fldFileName.setText( preSelection.getName() );
	      java.util.List<File> preSelections
			= Collections.singletonList( preSelection );
	      if( selectFiles( preSelections ) == 0 ) {
		filesSelected( preSelections );
	      }
	      done = true;
	    }
	  }
	}
      } else {
	while( preSelection != null ) {
	  if( preSelection.exists() && preSelection.isDirectory() ) {
	    setCurDir( preSelection, null );
	    done = true;
	    break;
	  }
	  preSelection = preSelection.getParentFile();
	}
      }
    }
    if( !done ) {
      setCurDir( defaultDir, null );
    }
    this.docFileName = this.fldFileName.getDocument();
    if( this.docFileName != null ) {
      if( this.btnStart != null ) {
	this.btnStart.setEnabled( false );
      }
      if( this.btnLoadWithOptions != null ) {
	this.btnLoadWithOptions.setEnabled( false );
      }
    }
    updApproveBtn( false );


    // Listener
    this.comboDir.addActionListener( this );
    this.btnGoUp.addActionListener( this );
    this.btnCreateDir.addActionListener( this );
    this.list.addKeyListener( this );
    this.list.addMouseListener( this );
    this.list.addListSelectionListener( this );
    this.fldFileName.addActionListener( this );
    this.fldFileName.addFocusListener( this );
    this.comboFileType.addActionListener( this );
    this.btnApprove.addActionListener( this );
    if( this.btnStart != null ) {
      this.btnStart.addActionListener( this );
    }
    if( this.btnLoadWithOptions != null ) {
      this.btnLoadWithOptions.addActionListener( this );
    }
    this.btnCancel.addActionListener( this );
    this.popupGoUp.addActionListener( this );
    this.popupCreateDir.addActionListener( this );
    this.popupRename.addActionListener( this );
    this.popupDelete.addActionListener( this );
    this.popupRefresh.addActionListener( this );
    if( this.docFileName != null ) {
      this.docFileName.addDocumentListener( this );
    }


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
    if( !FileUtil.isFileDrop( e ) )
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
    File file = FileUtil.fileDrop( this, e );
    if( FileUtil.isUsable( file ) ) {
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
      final JList<File> list = this.list;
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
	if( FileUtil.isUsable( file ) ) {
	  if( file.isDirectory() ) {
	    dirSelected = true;
	    break;
	  }
	  String s = TextUtil.emptyToNull( file.getName() );
	  if( fileName != null ) {
	    fileName = "";
	  } else {
	    fileName = s;
	  }
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
      this.popupDelete.setEnabled( !files.isEmpty() );
      this.popupRename.setEnabled( files.size() == 1 );
      updApproveBtn( dirSelected );

      String statusText = filesSelected( files );
      this.labelStatus.setText(
		statusText != null ? statusText : defaultStatusText );
    }
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
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
	else if( (src == this.btnGoUp) || (src == this.popupGoUp) ) {
	  rv = true;
	  doGoUp();
	}
	else if( (src == this.btnCreateDir) || (src == this.popupCreateDir) ) {
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
	else if( src == this.popupDelete ) {
	  rv = true;
	  doDelete();
	}
	else if( src == this.popupRefresh ) {
	  rv = true;
	  doRefresh();
	}
	else if( src == this.popupRename ) {
	  rv = true;
	  doRename();
	}
      }
    }
    return rv;
  }


  @Override
  public boolean getPackOnUIUpdate()
  {
    return false;
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
  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean   rv = false;
    Component c  = e.getComponent();
    if( c != null ) {
      this.popupMnu.show( c, e.getX(), e.getY() );
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
	    file = new File( this.curDir, fileName );
	  }
	  files = Collections.singletonList( file );
	}
      }
    }
    approveFiles( files, startSelected, loadWithOptionsSelected );
  }


  private void doCreateDir()
  {
    if( FileUtil.isUsable( this.curDir ) ) {
      File dirFile = FileUtil.createDir( this, this.curDir );
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
	    if( FileUtil.isUsable( f ) ) {
	      file = f;
	      paths.add( f.toPath() );
	    }
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
    if( this.curDir != null ) {
      String text = this.fldFileName.getText();
      if( text != null ) {
	text = text.trim();
	if( !text.isEmpty() ) {
	  if( text.equals( "." ) ) {
	    setCurDir( this.curDir, null );
	  } else if( text.equals( ".." ) ) {
	    File parent = FileUtil.getParent( this.curDir );
	    if( parent != null ) {
	      setCurDir( parent, this.curDir );
	    } else {
	      setCurDir( this.curDir, null );
	    }
	  } else {
	    // Name eine Unterverzeichnis, evtl. mit virtuellem Name?
	    File            subDir = null;
	    ListModel<File> model  = this.list.getModel();
	    if( model != null ) {
	      int n = model.getSize();
	      for( int i = 0; i < n; i++ ) {
		File   f = model.getElementAt( i );
		String s = f.getName();
		if( (s != null) && f.isDirectory() ) {
		  if( File.separatorChar == '/' ) {
		    if( text.equals( s )
			|| text.equals( s + File.separator ) )
		    {
		      subDir = f;
		      break;
		    }
		  } else {
		    if( text.equalsIgnoreCase( s )
			|| text.equalsIgnoreCase( s + File.separator ) )
		    {
		      subDir = f;
		      break;
		    }
		  }
		}
	      }
	    }
	    if( subDir != null ) {
	      setCurDir( subDir, null );
	    } else {
	      // sonstige Datei oder Verzeichnis
	      File   file  = new File( text );
	      String fName = file.getName();
	      if( fName != null ) {
		if( (fName.indexOf( '?' ) < 0)
		    && (fName.indexOf( '*' ) < 0) )
		{
		  if( !file.isAbsolute() ) {
		    file = new File( this.curDir, fName );
		  }
		  approveFiles(
			Collections.singletonList( file ),
			false,
			true );
		}
	      }
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
      upDir = FileUtil.getParent( this.curDir );
    }
    setCurDir( upDir, this.curDir );
  }


  private void doListAction( int idx )
  {
    if( idx >= 0 ) {
      ListModel<?> model = this.list.getModel();
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
      File file = files.get( 0 );
      if( FileUtil.isUsable( file ) ) {
	file = FileUtil.renameFile( this, file );
	if( file != null ) {
	  updList( Collections.singletonList( file ) );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private DirItem addDirItem( java.util.List<DirItem> dstList, File file )
  {
    DirItem rv = null;
    if( FileUtil.isUsable( file ) ) {
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
	  file = FileUtil.getParent( file );
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
	if( FileUtil.isUsable( file ) ) {
	  if( file.isDirectory() ) {
	    setCurDir( file, null );
	    done = true;
	    break;
	  }
	}
      }
      if( !done && !files.isEmpty() ) {
	File file = files.get( 0 );
	if( FileUtil.isUsable( file ) ) {

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
		// Dateiendung ggf. automatisch anhaengen
		file = FileUtil.completeFileExtension(
					file,
					getSelectedFileFilter() );
	      }
	    }

	    // bei Vorhandensein der Datei warnen
	    if( !FileUtil.confirmFileOverwrite( this, file ) ) {
	      file  = null;
	      files = null;
	    }
	  }
	  if( file != null ) {
	    this.selectedFile  = file;
	    this.selectedFiles = files;
	    this.startSelected = startSelected;
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
	      buf.append( '\\' );
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
    String  statusText = null;
    File    file       = null;
    boolean singleFile = false;
    if( files != null ) {
      if( files.size() == 1 ) {
	file = files.get( 0 );
	if( FileUtil.isUsable( file ) ) {
	  singleFile = file.isFile();
	  if( singleFile && file.canRead() ) {
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
		this.mode.equals( Mode.LOAD ) && singleFile );
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
    if( (list != null) && FileUtil.isUsable( file ) ) {
      int i = 0;
      for( DirItem item : list ) {
	if( FileUtil.equals( item.getDirectory(), file ) ) {
	  rv = i;
	  break;
	}
	i++;
      }
    }
    return rv;
  }


  private FileFilter getSelectedFileFilter()
  {
    FileFilter fileFilter = null;
    if( this.fileFilters != null ) {
      int idx = this.comboFileType.getSelectedIndex() - 1;
      if( (idx >= 0) && (idx < this.fileFilters.size()) ) {
	fileFilter = this.fileFilters.get( idx );
      }
    }
    return fileFilter;
  }


  private boolean isForSave()
  {
    return this.mode.equals( Mode.SAVE );
  }


  private void readAndShowDir(
			final File                 dirFile,
			final java.util.List<File> filesToSelect )
  {
    File[] files = null;
    try {
      files = FileUtil.listFiles( dirFile );
    }
    catch( Exception ex ) {}
    finally {
      fireSetListData( dirFile, files, filesToSelect  );
    }
  }


  private void repaintView()
  {
    if( this.dirCellRenderer != null ) {
      this.dirCellRenderer.clearCache();
    }
    if( this.fileCellRenderer != null ) {
      this.fileCellRenderer.clearCache();
    }
    if( this.comboDir != null ) {
      this.comboDir.repaint();
    }
    if( this.list != null ) {
      this.list.repaint();
    }
  }


  private static File replaceName( File file, String fName )
  {
    if( file != null ) {
      File parent = FileUtil.getParent( file );
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
	    if( FileUtil.isUsable( file ) ) {
	      String path = file.getPath();
	      if( path != null ) {
		if( pathsToSelect == null ) {
		  pathsToSelect = new TreeSet<>();
		}
		pathsToSelect.add( path );
	      }
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
      this.dirCellRenderer.clearCache();
      this.comboDir.setSelectedItem( defaultDirItem );
      this.comboDir.setToolTipText(
			dirFile != null ? dirFile.getPath() : null );
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
	FileUtil.sortFilesByName( files );
	FileFilter fileFilter = getSelectedFileFilter();
	Pattern    pattern    = null;
	String     mask       = this.fldFileName.getText();
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
	  if( FileUtil.isUsable( file ) ) {
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
	this.btnApprove.setText( EmuUtil.TEXT_OPEN );
	stateApprove = true;
      } else {
	this.btnApprove.setText( this.approveBtnText );
	boolean stateInput = false;
	if( this.docFileName != null ) {
	  String fileName = this.fldFileName.getText();
	  if( fileName != null ) {
	    fileName = fileName.trim();
	    if( !fileName.isEmpty() ) {
	      stateInput = true;
	    }
	  }
	} else {
	  stateInput = true;
	}
	if( this.btnLoadWithOptions != null ) {
	  stateApprove         = this.loadable;
	  stateStart           = this.startable;
	  stateLoadWithOptions = this.loadable || stateInput;
	} else {
	  stateApprove = stateInput;
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
    this.fileCellRenderer.clearCache();
    if( this.curDir != null ) {
      this.btnCreateDir.setEnabled( true );
      this.popupCreateDir.setEnabled( true );
      boolean state = (FileUtil.getParent( this.curDir ) != null);
      this.btnGoUp.setEnabled( state );
      this.popupGoUp.setEnabled( state );
    } else {
      this.btnCreateDir.setEnabled( false );
      this.popupCreateDir.setEnabled( false );
      this.btnGoUp.setEnabled( false );
      this.popupGoUp.setEnabled( false );
    }
    final File dirFile = this.curDir;
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
		    readAndShowDir( dirFile, filesToSelect );
		  }
		};
      t.start();
    }
  }
}
