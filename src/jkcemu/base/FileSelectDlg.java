/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateiauswahldialog
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.Document;
import javax.swing.table.*;


public class FileSelectDlg
			extends BasicDlg
			implements
				DocumentListener,
				FocusListener,
				ListSelectionListener
{
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

  private boolean                              forSave;
  private boolean                              startSelected;
  private boolean                              loadWithOptionsSelected;
  private boolean                              loadable;
  private boolean                              startable;
  private javax.swing.filechooser.FileFilter[] fileFilters;
  private File                                 curDir;
  private File                                 selectedFile;
  private FileSystemView                       fsv;
  private JList                                list;
  private JTextField                           fldFileName;
  private JComboBox                            comboFileType;
  private JComboBox                            comboDir;
  private JButton                              btnCreateDir;
  private JButton                              btnDirUp;
  private JButton                              btnApprove;
  private JButton                              btnStart;
  private JButton                              btnLoadWithOptions;
  private JButton                              btnCancel;
  private JLabel                               labelStatus;
  private DefaultComboBoxModel                 modelDir;
  private Document                             docFileName;
  private java.util.List<DirItem>              baseDirItems;
  private String                               approveBtnText;


  public FileSelectDlg(
		Window                                owner,
		boolean                               forSave,
		boolean                               startEnabled,
		boolean                               loadWithOptionsEnabled,
		String                                title,
		File                                  preSelection,
		javax.swing.filechooser.FileFilter... fileFilters )
  {
    super( owner, title );
    this.forSave                 = forSave;
    this.startSelected           = false;
    this.loadWithOptionsSelected = false;
    this.loadable                = false;
    this.startable               = false;
    this.fileFilters             = fileFilters;
    this.curDir                  = null;
    this.selectedFile            = null;
    this.fsv                     = FileSystemView.getFileSystemView();
    this.baseDirItems            = new ArrayList<DirItem>();

    File defaultDir = null;
    if( this.fsv != null ) {
      File[] files0 = this.fsv.getRoots();
      if( files0 != null ) {
	for( int i0 = 0; i0 < files0.length; i0++ ) {
	  File f0 = files0[ i0 ];
	  if( f0.isDirectory() && !f0.isHidden() ) {
	    this.baseDirItems.add( new DirItem( f0, 0 ) );
	    File[] files1 = this.fsv.getFiles( f0, false );
	    if( files1 != null ) {
	      for( int i1 = 0; i1 < files1.length; i1++ ) {
		File f1 = files1[ i1 ];
		if( f1.isDirectory()
		    && (!this.fsv.isFileSystem( f1 )
			|| this.fsv.isFileSystemRoot( f1 )) )
		{
		  this.baseDirItems.add( new DirItem( f1, 1 ) );
		  File[] files2 = this.fsv.getFiles( f1, false );
		  if( files2 != null ) {
		    for( int i2 = 0; i2 < files2.length; i2++ ) {
		      File f2 = files2[ i2 ];
		      if( f2.isDirectory() && this.fsv.isFileSystemRoot( f2 ) )
			this.baseDirItems.add( new DirItem( f2, 2 ) );
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
      defaultDir = this.fsv.getDefaultDirectory();
      addDirItem( this.baseDirItems, defaultDir );

      File homeDir = this.fsv.getHomeDirectory();
      addDirItem( this.baseDirItems, homeDir );
      if( defaultDir == null ) {
	defaultDir = homeDir;
      }
    } else {
      File[] roots = File.listRoots();
      if( roots != null ) {
	for( int i = 0; i < roots.length; i++ ) {
	  File file = roots[ i ];
	  if( file.isDirectory() && !file.isHidden() )
	    this.baseDirItems.add( new DirItem( file, 0 ) );
	}
      }
    }
    if( defaultDir == null ) {
      String homeDirText = System.getProperty( "user.home" );
      if( homeDirText != null ) {
	if( homeDirText.length() > 0 ) {
	  defaultDir = new File( homeDirText );
	  addDirItem( this.baseDirItems, defaultDir );
	}
      }
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
    this.modelDir   = new DefaultComboBoxModel();
    this.comboDir   = new JComboBox( this.modelDir );
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

    this.btnDirUp = createImageButton(
			"/images/file/up.png",
			"Eine Ebene h\u00F6her" );
    toolBar.add( this.btnDirUp, gbc );

    this.btnCreateDir = createImageButton(
			"/images/file/createdir.png",
			"Neuen Ordner erstellen" );
    toolBar.add( this.btnCreateDir, gbc );


    // Liste
    this.list = new JList();
    this.list.setDragEnabled( false );
    this.list.setLayoutOrientation( JList.VERTICAL_WRAP );
    this.list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    this.list.setVisibleRowCount( 0 );
    this.list.setCellRenderer( new FileSelectRenderer( this.fsv ) );
    this.list.addKeyListener( this );
    this.list.addMouseListener( this );
    this.list.addListSelectionListener( this );
    gbc.fill        = GridBagConstraints.BOTH;
    gbc.weightx     = 1.0;
    gbc.weighty     = 1.0;
    gbc.insets.top  = 5;
    gbc.insets.left = 10;
    gbc.gridwidth   = 3;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( new JScrollPane( this.list ), gbc );


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

    this.comboFileType = new JComboBox();
    this.comboFileType.setEditable( false );
    this.comboFileType.addItem( "Alle Dateien" );
    if( fileFilters != null ) {
      for( int i = 0; i < fileFilters.length; i++ ) {
	if( fileFilters[ i ] != null )
	  this.comboFileType.addItem( fileFilters[ i ].getDescription() );
      }
    }
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
    if( forSave ) {
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


    // Vorbelegung
    if( preSelection != null ) {
      if( preSelection.isDirectory() ) {
	setCurDir( preSelection );
      } else {
	File parentDir = null;
	if( this.fsv != null ) {
	  parentDir = this.fsv.getParentDirectory( preSelection );
	}
	if( parentDir == null ) {
	  parentDir = preSelection.getParentFile();
	}
	setCurDir( parentDir );
	this.fldFileName.setText( preSelection.getName() );
	if( !selectFile( preSelection ) ) {
	  fileSelected( preSelection );
	}
      }
    } else {
      setCurDir( defaultDir );
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


    // Fenstergroesse und -position
    setSize( 600, 400 );
    setParentCentered();
    setResizable( true );
  }


  public File getSelectedFile()
  {
    return this.selectedFile;
  }


  public boolean isLoadWithOptionsSelected()
  {
    return this.loadWithOptionsSelected;
  }


  public boolean isStartSelected()
  {
    return this.startSelected;
  }


	/* --- DocumentListener --- */

  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- FocusListener --- */

  public void focusGained( FocusEvent e )
  {
    if( !e.isTemporary() && (e.getComponent() == this.fldFileName) ) {
      final JList list = this.list;
      SwingUtilities.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    list.clearSelection();
			  }
			} );
    }
  }


  public void focusLost( FocusEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.list ) {
      this.loadable  = false;
      this.startable = false;

      String  statusText  = null;
      boolean dirSelected = false;
      File    file        = getSelectedListFile();
      if( file != null ) {
	if( file.isDirectory() ) {
	  dirSelected = true;
	} else {
	  String fileName = file.getName();
	  if( fileName != null ) {
	    if( fileName.length() > 0 ) {
	      if( this.docFileName != null ) {
		this.docFileName.removeDocumentListener( this );
		this.fldFileName.setText( fileName );
		this.docFileName.addDocumentListener( this );
	      } else {
		this.fldFileName.setText( fileName );
	      }
	    }
	  }
	  statusText = fileSelected( file );
	}
      }
      updApproveBtn( dirSelected );
      this.labelStatus.setText(
		statusText != null ? statusText : defaultStatusText );
    }
  }


	/* --- ueberschriebene Methoden --- */

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
	  updList();
	}
	else if( src == this.list ) {
	  rv = true;
	  doListAction( this.list.getSelectedIndex() );
	}
	else if( src == this.btnDirUp ) {
	  rv = true;
	  doDirUp();
	}
	else if( src == this.btnCreateDir ) {
	  rv = true;
	  doCreateDir();
	}
	else if( src == this.btnApprove ) {
	  rv = true;
	  doApproveFile( false, false );
	}
	else if( src == this.btnStart ) {
	  rv = true;
	  doApproveFile( true, false );
	}
	else if( (src == this.btnLoadWithOptions)
		 || (src == this.fldFileName) )
	{
	  rv = true;
	  doApproveFile( false, true );
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.list)
	&& (e.getClickCount() > 1)
	&& (e.getButton() == MouseEvent.BUTTON1) )
    {
      Point pt = e.getPoint();
      if( pt != null ) {
	int idx = this.list.locationToIndex( pt );
	if( idx >= 0 )
	  doListAction( idx );
      }
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.list.requestFocus();
  }


	/* --- Aktionen --- */

  private void doApproveFile(
			boolean startSelected,
			boolean loadWithOptionsSelected )
  {
    File file = getSelectedListFile();
    if( file == null ) {
      if( this.curDir != null ) {
	String fileName = this.fldFileName.getText();
	if( fileName != null ) {
	  fileName = fileName.trim();
	  if( fileName.length() > 0 ) {
	    file = new File( fileName );
	    if( !file.isAbsolute() ) {
	      if( this.fsv != null ) {
		file = this.fsv.getChild( this.curDir, fileName );
	      } else {
		file = new File( this.curDir, fileName );
	      }
	    }
	  }
	}
      }
    }
    approveFile( file, startSelected, loadWithOptionsSelected );
  }


  private void doCreateDir()
  {
    if( this.curDir != null ) {
      File dirFile = EmuUtil.createDir( this, this.curDir );
      if( dirFile != null )
	setCurDir( dirFile );
    }
  }


  private void doDirUp()
  {
    if( this.curDir != null ) {
      if( this.fsv != null ) {
	setCurDir( this.fsv.getParentDirectory( this.curDir ) );
      } else {
	setCurDir( this.curDir.getParentFile() );
      }
    } else {
      setCurDir( null );
    }
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
	      File file = (File) o;
	      if( file != null )
		approveFile( file, false, true );
	    }
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private DirItem addDirItem( java.util.List<DirItem> dstList, File file )
  {
    DirItem rv = null;
    if( file != null ) {
      if( file.isDirectory() ) {

	/*
	 * Liste mit allen uebergeordneten Verzeichnissen erstellen,
	 * das erste Element repraesentiert die oberste Ebene.
	 */
	java.util.List<File> srcList = new ArrayList<File>();
	while( file != null ) {
	  if( srcList.contains( file ) ) {	// Ring verhindern
	    file = null;
	  } else {
	    srcList.add( 0, file );
	    if( this.fsv != null ) {
	      file = this.fsv.getParentDirectory( file );
	    } else {
	      file = file.getParentFile();
	    }
	  }
	}

	// in die uebergebene Liste einfuegen oder anhaengen
	boolean done   = false;
	int     dstLen = dstList.size();
	int     srcLen = srcList.size();
	for( int i = srcLen - 1; !done && (i >= 0); --i ) {
	  File f = srcList.get( i );
	  for( int k = 0; !done && (k < dstLen); k++ ) {
	    DirItem dstItem = dstList.get( k );
	    if( dstItem.getDirectory().equals( f ) ) {
	      rv        = dstItem;
	      int level = dstItem.getLevel() + 1;
	      int pos   = k + 1;
	      for( int l = i + 1; l < srcLen; l++ ) {
		rv = new DirItem( srcList.get( l ), level++ );
		if( pos < dstLen ) {
		  dstList.add( pos, rv );
		} else {
		  dstList.add( rv );
		}
		pos++;
	      }
	      done = true;
	    }
	  }
	}
	if( !done ) {
	  int level = 0;
	  for( File f : srcList ) {
	    rv = new DirItem( f, level++ );
	    dstList.add( rv );
	  }
	}
      }
    }
    return rv;
  }


  private void approveFile(
			File    file,
			boolean startSelected,
			boolean loadWithOptionsSelected )
  {
    if( file != null ) {
      if( file.isDirectory() ) {
	setCurDir( file );
      } else {
	if( this.forSave && file.exists() ) {
	  if( !BasicDlg.showYesNoDlg(
			this,
			"Die Datei \'"
				+ file.getName()
				+ "\' existiert bereits.\n"
				+ "M\u00F6chten Sie die Datei"
				+ " \u00FCberschreiben?" ) )
	  {
	    file = null;
	  }
	}
	if( file != null ) {
	  this.selectedFile            = file;
	  this.startSelected           = startSelected;
	  this.loadWithOptionsSelected = loadWithOptionsSelected;
	  doClose();
	}
      }
    }
  }


  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docFileName )
      updApproveBtn( false );
  }


  private String fileSelected( File file )
  {
    String statusText = null;
    if( file != null ) {
      if( file.isFile() && file.canRead() ) {
	FileInfo fileInfo = FileInfo.analyzeFile( file );
	if( fileInfo != null ) {
	  if( !fileInfo.isKCBasicProgramFormat()
	      && (fileInfo.getBegAddr() >= 0) )
	  {
	    this.loadable = true;
	    if( fileInfo.getStartAddr() >= 0 )
	      this.startable = true;
	  }
	  statusText = fileInfo.getInfoText();
	}
      }
    }
    if( this.btnApprove != null ) {
      this.btnApprove.setEnabled( (!this.forSave && this.loadable)
					|| (this.btnLoadWithOptions == null) );
    }
    if( this.btnStart != null ) {
      this.btnStart.setEnabled( !this.forSave && this.startable );
    }
    if( this.btnLoadWithOptions != null ) {
      this.btnLoadWithOptions.setEnabled( !this.forSave && (file != null) );
    }
    if( statusText != null ) {
      if( statusText.length() < 1 )
	statusText = null;
    }
    return statusText;
  }


  private void fireSetListData( final File dirFile, final File[] files )
  {
    SwingUtilities.invokeLater(
			new Runnable()
			{
			  public void run()
			  {
			    setListData( dirFile, files );
			  }
			} );
  }


  private File getSelectedListFile()
  {
    File   file  = null;
    Object value = this.list.getSelectedValue();
    if( value != null ) {
      if( value instanceof File )
	file = (File) value;
    }
    return file;
  }


  private boolean selectFile( File file )
  {
    boolean rv = false;
    if( file != null ) {
      ListModel model = this.list.getModel();
      if( model != null ) {
	int n = model.getSize();
	for( int i = 0; i < n; i++ ) {
	  Object o = model.getElementAt( i );
	  if( o != null ) {
	    if( o.equals( file ) ) {
	      this.list.setSelectedIndex( i );
	      rv = true;
	      break;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void setCurDir( File dirFile )
  {
    this.comboDir.removeActionListener( this );
    DirItem defaultDirItem        = null;
    java.util.List<DirItem> items = new ArrayList<DirItem>();
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
    updList();
    this.comboDir.addActionListener( this );
  }


  private void setListData( File dirFile, File[] files )
  {
    if( (this.curDir != null) && (dirFile != null) && (files != null) ) {
      if( this.curDir.equals( dirFile ) ) {
	javax.swing.filechooser.FileFilter fileFilter = null;
	try {
	  Arrays.sort( files );
	}
	catch( ClassCastException ex ) {}
	if( this.fileFilters != null ) {
	  int idx = this.comboFileType.getSelectedIndex() - 1;
	  if( (idx >= 0) && (idx < this.fileFilters.length) ) {
	    fileFilter = this.fileFilters[ idx ];
	  }
	}
	java.util.List<File> entries = new ArrayList<File>(
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
	    if( fileFilter != null ) {
	      if( fileFilter.accept( file ) ) {
		entries.add( file );
	      }
	    } else {
	      entries.add( file );
	    }
	  }
	}
	this.list.setListData( entries.toArray() );
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
	boolean hasFileName = false;
	if( this.docFileName != null ) {
	  String fileName = this.fldFileName.getText();
	  if( fileName != null ) {
	    fileName = fileName.trim();
	    if( fileName.length() > 0 ) {
	      hasFileName = true;
	    }
	  }
	} else {
	  hasFileName = true;
	}
	if( this.btnLoadWithOptions != null ) {
	  stateApprove         = this.loadable;
	  stateStart           = this.startable;
	  stateLoadWithOptions = hasFileName;
	} else {
	  stateApprove = hasFileName;
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


  private void updList()
  {
    this.curDir    = null;
    Object dirItem = this.comboDir.getSelectedItem();
    if( dirItem != null ) {
      if( dirItem instanceof DirItem )
	this.curDir = ((DirItem) dirItem).getDirectory();
    }
    this.list.setListData( new Object[ 0 ] );
    if( this.curDir != null ) {
      this.btnCreateDir.setEnabled( true );
      if( this.fsv != null ) {
	this.btnDirUp.setEnabled(
		this.fsv.getParentDirectory( this.curDir ) != null );
      } else {
	this.btnDirUp.setEnabled( this.curDir.getParentFile() != null );
      }
    } else {
      this.btnCreateDir.setEnabled( false );
      this.btnDirUp.setEnabled( false );
    }
    final FileSystemView fsv     = this.fsv;
    final File           dirFile = this.curDir;
    if( curDir != null ) {
      this.labelStatus.setText( "Lese Verzeichnis..." );
      Thread t = new Thread()
			{
			  public void run()
			  {
			    File[] files = null;
			    try {
			      if( fsv != null ) {
				files = fsv.getFiles( dirFile, false );
			      } else {
				files = dirFile.listFiles();
			      }
			    }
			    catch( Exception ex ) {}
			    finally {
			      fireSetListData( dirFile, files );
			    }
			  }
			};
      t.start();
    }
  }
}

