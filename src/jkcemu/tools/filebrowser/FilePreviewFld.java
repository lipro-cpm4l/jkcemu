/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Detailanzeige bzw. Vorschau einer Datei
 */

package jkcemu.tools.filebrowser;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import jkcemu.Main;
import jkcemu.audio.AudioFile;
import jkcemu.audio.AudioUtil;
import jkcemu.audio.PCMDataInfo;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.DiskUtil;
import jkcemu.file.FileCheckResult;
import jkcemu.file.FileEntry;
import jkcemu.file.FileInfo;
import jkcemu.file.FileTableModel;
import jkcemu.file.FileUtil;
import jkcemu.file.TarEntry;
import jkcemu.image.ImageEntry;
import jkcemu.image.ImageLoader;
import jkcemu.image.ImageUtil;


public class FilePreviewFld extends JPanel
				implements
					MouseListener,
					Runnable
{
  private static final String CARD_NAME_FILE_TABLE = "file.table";
  private static final String CARD_NAME_IMAGE      = "image";
  private static final String CARD_NAME_TEXT       = "text";
  private static final String CARD_NAME_EMPTY      = "empty";

  private Frame            owner;
  private long             maxFileSize;
  private Object           lockMonitor;
  private boolean          sortCaseSensitive;
  private boolean          fileChanged;
  private ExtendedFileNode fileNode;
  private Thread           thread;
  private FileInfoFld      headerFld;
  private JPanel           detailsFld;
  private CardLayout       cardLayout;
  private ImageCard        imageCard;
  private JTextArea        textArea;
  private FileTableModel   fileTableModel;
  private JTableHeader     fileTableHeader;
  private JTable           fileTable;


  public FilePreviewFld( Frame owner )
  {
    this.owner             = owner;
    this.maxFileSize       = 0;
    this.lockMonitor       = "lock monitor";
    this.sortCaseSensitive = false;
    this.fileChanged       = false;
    this.fileNode          = null;
    this.thread            = null;

    // Layout
    setLayout( new BorderLayout( 5, 5 ) );

    this.headerFld = new FileInfoFld( 15 );
    add( this.headerFld, BorderLayout.NORTH );

    this.cardLayout = new CardLayout();
    this.detailsFld = GUIFactory.createPanel( this.cardLayout );
    add( this.detailsFld, BorderLayout.CENTER );

    this.detailsFld.add( GUIFactory.createPanel(), CARD_NAME_EMPTY );

    this.imageCard = new ImageCard();
    this.detailsFld.add( this.imageCard, CARD_NAME_IMAGE );

    this.textArea = GUIFactory.createTextArea();
    this.textArea.setBorder( BorderFactory.createLineBorder(
					getForeground(),
					1 ) );
    this.textArea.setEditable( false );
    this.textArea.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 9 ) );
    this.textArea.setPreferredSize( new Dimension( 1, 1 ) );
    this.detailsFld.add( this.textArea, CARD_NAME_TEXT );

    this.fileTableModel = new FileTableModel(
				FileTableModel.Column.NAME,
				FileTableModel.Column.INFO,
				FileTableModel.Column.LAST_MODIFIED );

    this.fileTable = GUIFactory.createTable( this.fileTableModel );
    this.fileTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.fileTable.setColumnSelectionAllowed( false );
    this.fileTable.setRowSelectionAllowed( true );
    this.fileTable.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.detailsFld.add(
		GUIFactory.createScrollPane(
			this.fileTable,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ),
		CARD_NAME_FILE_TABLE );

    TableCellRenderer renderer = new FileTableCellRenderer();
    this.fileTable.setDefaultRenderer( Number.class, renderer );
    this.fileTable.setDefaultRenderer( Object.class, renderer );
    this.fileTable.setDefaultRenderer( String.class, renderer );
    this.fileTable.setDefaultRenderer( java.util.Date.class, renderer );

    EmuUtil.setTableColWidths( this.fileTable, 150, 100, 140 );

    this.fileTableHeader = this.fileTable.getTableHeader();
    if( this.fileTableHeader != null ) {
      this.fileTableHeader.addMouseListener( this );
    }
  }


  public JTable getJTable()
  {
    return this.fileTable;
  }


  public void setFileNode(
			ExtendedFileNode fileNode,
			long             maxFileSize,
			boolean          sortCaseSensitive )
  {
    synchronized( this.lockMonitor ) {
      this.fileNode    = fileNode;
      this.maxFileSize = maxFileSize;
      this.fileChanged = true;
      try {
	this.lockMonitor.notifyAll();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
    this.fileTableModel.setSortCaseSensitive( sortCaseSensitive );
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getSource() == this.fileTableHeader) && (e.getClickCount() > 0) ) {
      int col = this.fileTableHeader.columnAtPoint(
				new Point( e.getX(), e.getY() ) );
      if( col >= 0 ) {
	this.fileTableModel.sort(
		this.fileTable.convertColumnIndexToModel( col ) );
	this.fileTableModel.fireTableDataChanged();
	e.consume();
      }
    }
  }


  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    // leer
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    while( this.thread != null ) {
      try {

	// Datei holen
	boolean          sortCaseSensitive = false;
	boolean          fileChanged       = false;
	ExtendedFileNode fileNode          = null;
	long             maxFileSize       = 0;
	while( !fileChanged && (this.thread != null) ) {
	  synchronized( this.lockMonitor ) {
	    sortCaseSensitive = this.sortCaseSensitive;
	    fileNode          = this.fileNode;
	    fileChanged       = this.fileChanged;
	    maxFileSize       = this.maxFileSize;
	    if( fileChanged ) {
	      this.fileNode    = null;
	      this.fileChanged = false;
	    } else {
	      try {
		this.lockMonitor.wait();
	      }
	      catch( IllegalMonitorStateException ex ) {}
	      catch( InterruptedException ex ) {}
	    }
	  }
	}
	if( fileChanged && (this.thread != null) ) {
	  this.imageCard.setImage( null );
	  String cardName = null;

	  // Datei auswerten
	  Map<FileInfoFld.Item,Object> infoItems  = new HashMap<>();
	  String[]                     addonLines = null;

	  if( fileNode != null ) {
	    File file = fileNode.getFile();
	    if( file != null ) {
	      long lastModified = file.lastModified();
	      if( lastModified > 0 ) {
		infoItems.put( FileInfoFld.Item.LAST_MODIFIED, lastModified );
	      }
	      String fName = file.getName();
	      if( fName != null ) {
		if( !fName.isEmpty() ) {
		  infoItems.put( FileInfoFld.Item.NAME, fName );
		}
	      }
	      if( fileNode.getAllowsChildren() ) {
		addDirectoryInfo(
			infoItems,
			fileNode.children(),
			sortCaseSensitive );
		cardName = CARD_NAME_FILE_TABLE;
	      }
	      Path path = fileNode.getPath();
	      if( path != null ) {
		if( Files.isSymbolicLink( path ) ) {
		  Path destPath = Files.readSymbolicLink( path );
		  if( destPath != null ) {
		    infoItems.put( FileInfoFld.Item.LINKED_TO, destPath );
		  }
		}
	      }
	      if( file.isFile() ) {
		String fExt = null;
		if( fName != null ) {
		  int pos = fName.lastIndexOf( '.' );
		  if( (pos >= 0) && (pos < fName.length() - 1) ) {
		    fExt = fName.substring( pos + 1 ).toUpperCase();
		  }
		}
		if( fExt != null ) {
		  infoItems.put( FileInfoFld.Item.TYPE, fExt + "-Datei" );
		} else {
		  infoItems.put( FileInfoFld.Item.TYPE, "Datei" );
		}
		infoItems.put( FileInfoFld.Item.SIZE, file.length() );

		FileCheckResult checkResult = fileNode.getCheckResult();
		if( checkResult.isAudioFile()
		    || (checkResult.isTapeFile()
			&& !checkResult.isKC85TapFile()
			&& !checkResult.isKCBasicHeadFile()
			&& !checkResult.isKCBasicFile()
			&& !checkResult.isKCSysFile()
			&& !checkResult.isZ9001TapFile()) )
		{
		  addAudioInfo( infoItems, file );
		} else if( checkResult.isImageFile() ) {
		  if( addImageInfo( infoItems, file, maxFileSize ) ) {
		    cardName = CARD_NAME_IMAGE;
		  }
		} else if( checkResult.isPlainDiskFile() ) {
		  if( addPlainDiskInfo(
				infoItems,
				checkResult.isCompressedFile() ?
					"Komprimierte einfache Abbilddatei"
					: "Einfache Abbilddatei",
				file,
				maxFileSize,
				sortCaseSensitive ) )
		  {
		    cardName = CARD_NAME_FILE_TABLE;
		  }
		} else if( checkResult.isNonPlainDiskFile() ) {
		  if( checkFileSize( file.length(), maxFileSize ) ) {
		    try {
		      AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this.owner,
								file,
								true );
		      if( disk != null ) {
			addDiskInfo(
				infoItems,
				checkResult.isCompressedFile(),
				disk,
				sortCaseSensitive );
			cardName = CARD_NAME_FILE_TABLE;
		      }
		    }
		    catch( IOException ex ) {}
		  }
		} else if( checkResult.isTextFile() ) {
		  if( addTextInfo( infoItems, file, maxFileSize ) ) {
		    cardName = CARD_NAME_TEXT;
		  }
		} else if( checkResult.isArchiveFile() ) {
		  if( addArchiveInfo(
				infoItems,
				file,
				maxFileSize,
				sortCaseSensitive ) )
		  {
		    cardName = CARD_NAME_FILE_TABLE;
		  }
		} else {
		  FileInfo fileInfo = checkResult.getFileInfo();
		  if( fileInfo != null ) {
		    infoItems.put(
			  FileInfoFld.Item.TYPE,
			  fileInfo.getFileText() );
		    String addrText = fileInfo.getAddrText();
		    String fileDesc = fileInfo.getFileDesc();
		    if( (addrText != null) && (fileDesc != null) ) {
		      addonLines      = new String[ 2 ];
		      addonLines[ 0 ] = addrText;
		      addonLines[ 1 ] = fileDesc;
		    } else {
		      if( addrText != null ) {
			addonLines      = new String[ 1 ];
			addonLines[ 0 ] = addrText;
		      }
		      else if( fileDesc != null ) {
			addonLines      = new String[ 1 ];
			addonLines[ 0 ] = fileDesc;
		      }
		    }
		  }
		}
	      }
	    }
	  }

	  final Map<FileInfoFld.Item,Object> theInfoItems  = infoItems;
	  final String[]                     theAddonLines = addonLines;
	  final String                       theCardName   = cardName;
	  EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    setContents( theInfoItems, theAddonLines, theCardName );
		  }
		} );
	}
      }
      catch( Exception ex ) {}
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( this.thread == null ) {
      Thread thread = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU File Browser Details Viewer" );
      this.thread = thread;
      thread.setDaemon( true );
      thread.start();
    }
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.thread != null ) {
      synchronized( this.lockMonitor ) {
	try {
	  this.lockMonitor.notifyAll();
	}
	catch( IllegalMonitorStateException ex ) {}
      }
    }
    this.thread = null;
  }


	/* --- private Methoden --- */

  private void addAudioInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			File                         file )
  {
    boolean     gzip = FileUtil.isGZipFile( file );
    PCMDataInfo info = null;
    try {
      info = AudioFile.getInfo( file );
    }
    catch( IOException ex ) {}
    try {
      if( info == null ) {
	info = AudioUtil.openAudioOrTapeFile( file );
      }
      if( info != null ) {
	infoItems.put(
		FileInfoFld.Item.TYPE,
		gzip ? "Komprimierte Sound-Datei" : "Sound-/Tape-Datei" );
	String fmtText = AudioUtil.getAudioFormatText( info );
	if( fmtText != null ) {
	  if( !fmtText.isEmpty() ) {
	    infoItems.put( FileInfoFld.Item.FORMAT, fmtText );
	  }
	}
	long frameCount = info.getFrameCount();
	if( frameCount > 0 ) {
	  infoItems.put(
		FileInfoFld.Item.DURATION,
		(double) frameCount / (double) info.getFrameRate() );
	}
      }
    }
    catch( IOException ex ) {}
    finally {
      if( info != null ) {
	if( info instanceof Closeable ) {
	  EmuUtil.closeSilently( (Closeable) info );
	}
      }
    }
  }


  private boolean addArchiveInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			File                         file,
			long                         maxFileSize,
			boolean                      sortCaseSensitive )
  {
    boolean rv    = false;
    String  fName = file.getName();
    if( (fName != null) && checkFileSize( file.length(), maxFileSize ) ) {
      fName = fName.toLowerCase();
      infoItems.put( FileInfoFld.Item.TYPE, "Archivdatei" );
      this.fileTableModel.clear( false );
      if( fName.endsWith( ".jar" ) || fName.endsWith( ".zip" ) ) {
	ZipFile in = null;
	try {
	  in = new ZipFile( file );
	  Enumeration<? extends ZipEntry> entries = in.entries();
	  if( entries != null ) {
	    while( entries.hasMoreElements() ) {
	      this.fileTableModel.addRow(
			new ExtendedFileEntry( entries.nextElement() ),
			false );
	    }
	  }
	}
	catch( NoSuchElementException ex ) {}
	catch( IOException ex ) {}
	finally {
	  EmuUtil.closeSilently( in );
	}
	rv = true;
      }
      else if( fName.endsWith( ".tar" )
	       || fName.endsWith( ".tar.gz" )
	       || fName.endsWith( ".tgz" ) )
      {
	InputStream in = null;
	try {
	  in = new FileInputStream( file );
	  if( fName.endsWith( "z" ) ) {
	    in = new GZIPInputStream( in );
	  }
	  TarEntry entry = TarEntry.readEntryHeader( in );
	  while( entry != null ) {
	    this.fileTableModel.addRow(
			new ExtendedFileEntry( entry ),
			false );
	    long size = entry.getSize();
	    if( size > 0 ) {
	      long blocks = (size + 511) / 512;
	      in.skip( blocks * 512 );
	    }
	    entry = TarEntry.readEntryHeader( in );
	  }
	}
	catch( IOException ex ) {}
	finally {
	  EmuUtil.closeSilently( in );
	}
	rv = true;
      }
      this.fileTableModel.setSortCaseSensitive( sortCaseSensitive );
      this.fileTableModel.fireTableDataChanged();
    }
    return rv;
  }


  private void addDirectoryInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			Enumeration<?>               children,
			boolean                      sortCaseSensitive )
  {
    infoItems.put( FileInfoFld.Item.TYPE, "Verzeichnis" );
    this.fileTableModel.clear( false );
    if( children != null ) {
      try {
	while( children.hasMoreElements() ) {
	  Object o = children.nextElement();
	  if( o != null ) {
	    if( o instanceof ExtendedFileNode )
	      this.fileTableModel.addRow(
			new ExtendedFileEntry( (ExtendedFileNode) o ),
			false );
	  }
	}
      }
      catch( NoSuchElementException ex ) {}
    }
    this.fileTableModel.setSortCaseSensitive( sortCaseSensitive );
    this.fileTableModel.fireTableDataChanged();
  }


  private boolean addDiskInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			boolean                      compressed,
			AbstractFloppyDisk           disk,
			boolean                      sortCaseSensitive )
  {
    boolean               rv      = false;
    Collection<FileEntry> entries = DiskUtil.readDirectory( disk );
    if( entries != null ) {
      String fmtText = disk.getFileFormatText();
      if( fmtText == null ) {
	fmtText = "Diskettenabbilddatei";
      }
      if( compressed ) {
	fmtText = "Komprimierte " + fmtText;
      }
      this.fileTableModel.clear( false );
      for( FileEntry entry : entries ) {
	this.fileTableModel.addRow( entry, false );
      }
      this.fileTableModel.setSortCaseSensitive( sortCaseSensitive );
      this.fileTableModel.fireTableDataChanged();
      rv = true;
    }
    return rv;
  }


  private boolean addImageInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			File                         file,
			long                         maxFileSize )
  {
    boolean rv = false;
    if( checkFileSize( file.length(), maxFileSize ) ) {
      try {
	Image image = null;
	/*
	 * GIF-Bilder mit der klassischen API laden,
	 * damit auch animierte GIFs abgespielt werden koennen.
	 */
	if( file.getPath().toLowerCase().endsWith( ".gif" ) ) {
	  try {
	    Toolkit tk = EmuUtil.getToolkit( this );
	    if( tk != null ) {
	      image = tk.createImage( file.getPath() );
	    }
	  }
	  catch( Exception ex ) {}
	}
	if( image != null ) {
	  ImageUtil.ensureImageLoaded( this, image );
	} else {
	  ImageEntry entry = ImageLoader.load( file );
	  if( entry != null ) {
	    image = entry.getImage();
	  }
	}
	if( image != null ) {
	  infoItems.put( FileInfoFld.Item.TYPE, "Bilddatei" );
	  int w = image.getWidth( this );
	  int h = image.getHeight( this );
	  if( (w > 0) && (h > 0) ) {
	    infoItems.put(
			FileInfoFld.Item.FORMAT,
			String.format( "%dx%d Pixel", w, h ) );
	  }
	  Object comment = image.getProperty( "comment", this );
	  if( comment != null ) {
	    if( !comment.equals( Image.UndefinedProperty ) ) {
	      String s = comment.toString();
	      if( s != null ) {
		s = s.trim();
		if( !s.isEmpty() ) {
		  infoItems.put( FileInfoFld.Item.COMMENT, s );
		}
	      }
	    }
	  }
	  this.imageCard.setImage( image );
	  rv = true;
	}
      }
      catch( Exception ex ) {}
      catch( OutOfMemoryError ex ) {}
    }
    return rv;
  }


  private boolean addPlainDiskInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			String                       typeText,
			File                         file,
			long                         maxFileSize,
			boolean                      sortCaseSensitive )
  {
    boolean rv    = false;
    String  fName = file.getName();
    if( (fName != null) && checkFileSize( file.length(), maxFileSize ) ) {
      Collection<FileEntry> entries = DiskUtil.readDirFromPlainDisk( file );
      if( entries != null ) {
	infoItems.put( FileInfoFld.Item.TYPE, typeText );
	this.fileTableModel.clear( false );
	for( FileEntry entry : entries ) {
	  this.fileTableModel.addRow( entry, false );
	}
	this.fileTableModel.setSortCaseSensitive( sortCaseSensitive );
	this.fileTableModel.fireTableDataChanged();
	rv = true;
      }
    }
    return rv;
  }


  private boolean addTextInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			File                         file,
			long                         maxFileSize )
  {
    boolean        rv = false;
    BufferedReader in = null;
    try {
      Toolkit tk = EmuUtil.getToolkit( this );
      if( tk != null ) {
	Dimension screenSize = tk.getScreenSize();
	Font      font       = this.textArea.getFont();
	if( (screenSize != null) && (font != null) ) {
	  int rowHeight = font.getSize();
	  if( (screenSize.height > 0) && (rowHeight > 0) ) {
	    int linesToRead = screenSize.height / rowHeight;
	    if( linesToRead > 0 ) {
	      in = new BufferedReader( new FileReader( file ) );

	      StringBuilder buf    = new StringBuilder( linesToRead * 256 );
	      int           nChars = 0;
	      String        line   = in.readLine();
	      while( (line != null)
		     && (linesToRead > 0)
		     && checkFileSize( nChars, maxFileSize ) )
	      {
		buf.append( line );
		buf.append( '\n' );
		nChars += line.length();
		nChars++;
		--linesToRead;
		line = in.readLine();
	      }
	      this.textArea.setText( buf.toString() );
	      this.textArea.setCaretPosition( 0 );
	      infoItems.put( FileInfoFld.Item.TYPE, "Textdatei" );
	      rv = true;
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}
    finally {
      EmuUtil.closeSilently( in );
    }
    return rv;
  }


  private static boolean checkFileSize( long size, long maxSize )
  {
    return (maxSize == 0) || ((maxSize > 0) && (size <= maxSize));
  }


  private void setContents(
		Map<FileInfoFld.Item,Object> infoItems,
		String[]                     addonLines,
		String                       cardName )
  {
    if( cardName != null ) {
      this.headerFld.setMinRows( 0 );
    }
    this.headerFld.setValues( infoItems, addonLines );
    this.cardLayout.show(
		this.detailsFld,
		cardName != null ? cardName : CARD_NAME_EMPTY );
    invalidate();
  }
}
