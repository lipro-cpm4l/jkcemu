/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Detailanzeige bzw. Vorschau einer Datei
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.image.*;


public class FilePreviewFld extends JPanel
				implements
					DragGestureListener,
					MouseListener,
					Runnable
{
  private Frame          owner;
  private long           maxFileSize;
  private Object         lockMonitor;
  private boolean        fieldsAdded;
  private boolean        sortCaseSensitive;
  private boolean        fileChanged;
  private FileNode       fileNode;
  private Thread         thread;
  private FileInfoFld    headerFld;
  private JPanel         detailsFld;
  private CardLayout     cardLayout;
  private ImageCard      imageCard;
  private JTextArea      textArea;
  private FileTableModel fileTableModel;
  private JTableHeader   fileTableHeader;
  private JTable         fileTable;


  public FilePreviewFld( Frame owner )
  {
    this.owner             = owner;
    this.maxFileSize       = 0;
    this.lockMonitor       = "lock monitor";
    this.fieldsAdded       = false;
    this.sortCaseSensitive = false;
    this.fileChanged       = false;
    this.fileNode          = null;
    this.thread            = null;

    // Layout
    setLayout( new BorderLayout( 5, 5 ) );

    this.headerFld = new FileInfoFld( 15 );
    add( this.headerFld, BorderLayout.NORTH );

    this.cardLayout = new CardLayout();
    this.detailsFld = new JPanel( this.cardLayout );
    add( this.detailsFld, BorderLayout.CENTER );

    this.detailsFld.add( new JPanel(), "empty" );

    this.imageCard = new ImageCard();
    this.detailsFld.add( this.imageCard, "image" );

    this.textArea = new JTextArea();
    this.textArea.setBorder( BorderFactory.createLineBorder(
					getForeground(),
					1 ) );
    this.textArea.setEditable( false );
    this.textArea.setFont( new Font( "Monospaced", Font.PLAIN, 9 ) );
    this.textArea.setPreferredSize( new Dimension( 1, 1 ) );
    this.detailsFld.add( this.textArea, "text" );

    this.fileTableModel = new FileTableModel(
				FileTableModel.Column.NAME,
				FileTableModel.Column.INFO,
				FileTableModel.Column.LAST_MODIFIED );

    this.fileTable = new JTable( this.fileTableModel );
    this.fileTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.fileTable.setColumnSelectionAllowed( false );
    this.fileTable.setRowSelectionAllowed( true );
    this.fileTable.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.detailsFld.add(
		new JScrollPane(
			this.fileTable,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ),
		"file.table" );

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

    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(
			this.fileTable,
			DnDConstants.ACTION_COPY,
			this );
  }


  public JTable getJTable()
  {
    return this.fileTable;
  }


  public void setFileNode(
			FileNode fileNode,
			long     maxFileSize,
			boolean  sortCaseSensitive )
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


	/* --- DragGestureListener --- */

  @Override
  public void dragGestureRecognized( DragGestureEvent e )
  {
    int[] rowNums = this.fileTable.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length > 0 ) {
	Collection<File> files = new ArrayList<File>();
	for( int i = 0; i < rowNums.length; i++ ) {
	  FileEntry entry = this.fileTableModel.getRow( rowNums[ i ] );
	  if( entry != null ) {
	    File file = entry.getFile();
            if( file != null )
              files.add( file );
          }
        }
	if( !files.isEmpty() ) {
	  try {
	    e.startDrag( null, new FileListSelection( files ) );
	  }
	  catch( InvalidDnDOperationException ex ) {}
	}
      }
    }
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
	boolean  sortCaseSensitive = false;
	boolean  fileChanged       = false;
	FileNode fileNode          = null;
	long     maxFileSize       = 0;
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
	  String cardName = null;

	  // Datei auswerten
	  Map<FileInfoFld.Item,Object> infoItems
				= new HashMap<FileInfoFld.Item,Object>();
	  String[] addonLines	= null;

	  if( fileNode != null ) {
	    File file = fileNode.getFile();
	    if( file != null ) {
	      long lastModified = file.lastModified();
	      if( lastModified > 0 ) {
		infoItems.put(
			FileInfoFld.Item.LAST_MODIFIED,
			new Long( lastModified ) );
	      }
	      String fName = file.getName();
	      if( fName != null ) {
		if( !fName.isEmpty() ) {
		  infoItems.put( FileInfoFld.Item.NAME, fName );
		}
	      }
	      if( !fileNode.isLeaf() ) {
		addDirectoryInfo(
			infoItems,
			fileNode.children(),
			sortCaseSensitive );
		cardName = "file.table";
	      }
	      else if( file.isFile() ) {
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
		infoItems.put(
			FileInfoFld.Item.SIZE,
			new Long( file.length() ) );

		if( fileNode.isAudioFile() ) {
		  addAudioInfo( infoItems, file );
		} else if( fileNode.isImageFile() ) {
		  if( addImageInfo( infoItems, file, maxFileSize ) ) {
		    cardName = "image";
		  }
		} else if( fileNode.isPlainDiskFile() ) {
		  if( addPlainDiskInfo(
				infoItems,
				fileNode.isCompressedFile() ?
					"Komprimierte einfache Abbilddatei"
					: "Einfache Abbilddatei",
				file,
				maxFileSize,
				sortCaseSensitive ) )
		  {
		    cardName = "file.table";
		  }
		} else if( fileNode.isNonPlainDiskFile() ) {
		  if( checkFileSize( file.length(), maxFileSize ) ) {
		    try {
		      AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this.owner,
								file );
		      if( disk != null ) {
			addDiskInfo(
				infoItems,
				fileNode.isCompressedFile(),
				disk,
				sortCaseSensitive );
			cardName = "file.table";
		      }
		    }
		    catch( IOException ex ) {}
		  }
		} else if( fileNode.isTextFile() ) {
		  if( addTextInfo( infoItems, file, maxFileSize ) ) {
		    cardName = "text";
		  }
		} else if( fileNode.isArchiveFile() ) {
		  if( addArchiveInfo(
				infoItems,
				file,
				maxFileSize,
				sortCaseSensitive ) )
		  {
		    cardName = "file.table";
		  }
		} else {
		  FileInfo fileInfo = fileNode.getFileInfo();
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
      Thread thread = new Thread( this, "JKCEMU File Browser Details Viewer" );
      this.thread   = thread;
      thread.setDaemon( true );
      thread.start();
    }
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    this.thread = null;
  }


	/* --- private Methoden --- */

  private void addAudioInfo(
			Map<FileInfoFld.Item,Object> infoItems,
			File                         file )
  {
    boolean gzip = EmuUtil.isGZipFile( file );
    
    // AudioFileFormat ermitteln (nur bei keinen gz-Dateien)
    BufferedInputStream bIn  = null;
    AudioInputStream    aIn  = null;
    AudioFileFormat     fFmt = null;
    try {
      if( gzip ) {
	bIn  = EmuUtil.openBufferedOptionalGZipFile( file );
	fFmt = AudioSystem.getAudioFileFormat( bIn );
      } else {
	fFmt = AudioSystem.getAudioFileFormat( file );
      }
    }
    catch( UnsupportedAudioFileException ex1 ) {}
    catch( IOException ex2 ) {}
    finally {
      EmuUtil.doClose( aIn );
      EmuUtil.doClose( bIn );
      aIn = null;
      bIn = null;
    }

    // AudioFormat ermitteln
    try {
      if( gzip ) {
	bIn = EmuUtil.openBufferedOptionalGZipFile( file );
	aIn = AudioSystem.getAudioInputStream( bIn );
      } else {
	aIn = AudioSystem.getAudioInputStream( file );
      }
      if( aIn != null ) {
	AudioFormat aFmt = aIn.getFormat();
	if( aFmt != null ) {
	  infoItems.put(
		FileInfoFld.Item.TYPE,
		gzip ? "Komprimierte Sound-Datei" : "Sound-Datei" );
	  String fmtText = AudioUtil.getAudioFormatText( aFmt );
	  if( fmtText != null ) {
	    if( !fmtText.isEmpty() ) {
	      infoItems.put( FileInfoFld.Item.FORMAT, fmtText );
	    }
	  }
	  if( fFmt != null ) {
	    Object author = fFmt.getProperty( "author" );
	    if( author != null ) {
	      infoItems.put( FileInfoFld.Item.AUTHOR, author );
	    }
	    Object title = fFmt.getProperty( "title" );
	    if( title != null ) {
	      infoItems.put( FileInfoFld.Item.TITLE, title );
	    }
	    Object date = fFmt.getProperty( "date" );
	    if( date != null ) {
	      infoItems.put( FileInfoFld.Item.DATE, date );
	    }
	    Object comment = fFmt.getProperty( "comment" );
	    if( comment != null ) {
	      infoItems.put( FileInfoFld.Item.COMMENT, comment );
	    }
	    infoItems.put(
			FileInfoFld.Item.DURATION,
			new Double( (double) fFmt.getFrameLength()
						/ aFmt.getFrameRate() ) );
	  }
	}
      }
    }
    catch( UnsupportedAudioFileException ex1 ) {}
    catch( IOException ex2 ) {}
    finally {
      EmuUtil.doClose( aIn );
      EmuUtil.doClose( bIn );
      aIn = null;
      bIn = null;
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
	  EmuUtil.doClose( in );
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
	      if( (size % 512) != 0 ) {
		size = (size + 512) & ~0x1FF;
	      }
	      in.skip( size );
	    }
	    entry = TarEntry.readEntryHeader( in );
	  }
	}
	catch( IOException ex ) {}
	finally {
	  EmuUtil.doClose( in );
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
			Enumeration                  children,
			boolean                      sortCaseSensitive )
  {
    boolean rv = false;
    infoItems.put( FileInfoFld.Item.TYPE, "Verzeichnis" );
    this.fileTableModel.clear( false );
    if( children != null ) {
      try {
	while( children.hasMoreElements() ) {
	  Object o = children.nextElement();
	  if( o != null ) {
	    if( o instanceof FileNode )
	      this.fileTableModel.addRow(
			new ExtendedFileEntry( (FileNode) o ),
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
	    Toolkit tk = getToolkit();
	    if( tk != null ) {
	      image = tk.createImage( file.getPath() );
	    }
	  }
	  catch( Exception ex ) {}
	}
	if( image != null ) {
	  ImgUtil.ensureImageLoaded( this, image );
	} else {
	  image = ImgLoader.load( file );
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
	  Object date = image.getProperty( "date", this );
	  if( date != null ) {
	    if( !date.equals( Image.UndefinedProperty ) ) {
	      infoItems.put( FileInfoFld.Item.DATE, date );
	    }
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
      Toolkit tk = getToolkit();
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
		buf.append( (char) '\n' );
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
      EmuUtil.doClose( in );
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
		cardName != null ? cardName : "empty" );
    invalidate();
  }
}

