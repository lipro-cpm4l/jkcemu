/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Setzen des Aenderungszeitpunktes
 */

package jkcemu.filebrowser;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EventObject;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Document;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class LastModifiedDlg extends BaseDlg
			implements FileVisitor<Path>, Runnable
{
  private java.util.List<Path> paths;
  private Thread               thread;
  private FileTime             time;
  private int                  numChanged;
  private int                  numUnchanged;
  private boolean              cancelled;
  private boolean              recursive;
  private boolean              archiveFiles;
  private DateFormat           dateFmtStd;
  private DateFormat           dateFmtShort;
  private JRadioButton         btnCurrentTime;
  private JRadioButton         btnTimeInput;
  private JCheckBox            btnRecursive;
  private JCheckBox            btnWithinArchiveFiles;
  private JTextField           fldTime;
  private JTextArea            fldLog;
  private JButton              btnOK;
  private JButton              btnCancel;


	/* --- FileVisitor --- */

  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    FileVisitResult rv = FileVisitResult.TERMINATE;
    if( !this.cancelled && (this.time != null) ) {
      if( this.recursive ) {
	setLastModified( dir );
      }
      rv = FileVisitResult.CONTINUE;
    }
    return rv;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.TERMINATE;
    if( !this.cancelled && (this.time != null) ) {
      if( this.recursive ) {
	rv = FileVisitResult.CONTINUE;
      } else {
	setLastModified( dir );
	rv = FileVisitResult.SKIP_SUBTREE;
      }
    }
    return rv;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.TERMINATE;
    if( !this.cancelled && (this.time != null) ) {
      setLastModified( file );
      rv = FileVisitResult.CONTINUE;
    }
    return rv;
  }


  @Override
  public FileVisitResult visitFileFailed( Path file, IOException ex )
  {
    this.numUnchanged++;
    return this.cancelled ?
		FileVisitResult.TERMINATE
		: FileVisitResult.CONTINUE;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    String msg = null;
    try {
      for( Path path : paths ) {
	Files.walkFileTree( path, this );
      }
    }
    catch( Exception ex ) {
      msg = ex.getMessage();
      this.cancelled = true;
    }
    finally {
      final String msg1 = msg;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    threadTerminated( msg1 );
		  }
		} );
    }
  }


  /*
   * Rueckgabewert:
   *   true:  Zeitstempel wurde geaendert
   *   false: Zeitstempel nicht geaendert
   */
  public static boolean open(
			Window               owner,
			java.util.List<Path> paths )
  {
    boolean rv = false;
    if( paths != null ) {
      if( !paths.isEmpty() ) {
        LastModifiedDlg dlg = new LastModifiedDlg( owner, paths );
        dlg.setVisible( true );
        if( dlg.numChanged > 0 ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( (src == this.btnCurrentTime) || (src == this.btnTimeInput) ) {
	  rv = true;
	  this.fldTime.setEditable( this.btnTimeInput.isSelected() );
	}
	else if( src == this.btnOK ) {
	  rv = true;
	  doOK();
	}
        else if( src == this.btnCancel ) {
	  rv = true;
	  doCancel();
	}
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private LastModifiedDlg(
		Window               owner,
		java.util.List<Path> paths )
  {
    super( owner, "\u00C4nderungszeitpunkt" );
    this.paths        = paths;
    this.thread       = null;
    this.time         = null;
    this.numChanged   = 0;
    this.numUnchanged = 0;
    this.cancelled    = false;
    this.recursive    = false;
    this.archiveFiles = false;
    this.dateFmtStd   = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.MEDIUM );
    this.dateFmtShort = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.SHORT );
    this.dateFmtStd.setLenient( false );
    this.dateFmtShort.setLenient( false );


    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					2, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );


    // Fensterinhalt
    add( new JLabel( "\u00C4nderungszeitpunkt setzen auf:" ), gbc );

    ButtonGroup grpTime = new ButtonGroup();

    this.btnCurrentTime = new JRadioButton( "aktuellen Zeitpunkt", true );
    grpTime.add( this.btnCurrentTime );
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.btnCurrentTime, gbc );

    this.btnTimeInput = new JRadioButton( "Datum/Uhrzeit:", false );
    grpTime.add( this.btnTimeInput );
    gbc.insets.top = 0;
    gbc.gridwidth  = 1;
    gbc.gridy++;
    add( this.btnTimeInput, gbc );

    this.fldTime = new JTextField();
    this.fldTime.setEditable( false );
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridx++;
    add( this.fldTime, gbc );

    this.btnRecursive = new JCheckBox(
			"In Verzeichnisse hinein wechseln",
			false );
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridwidth   = 2;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.btnRecursive, gbc );

    this.btnWithinArchiveFiles = new JCheckBox(
			"In JAR- und ZIP-Dateien hinein wechseln",
			false );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnWithinArchiveFiles, gbc );

    boolean hasDirs     = false;
    boolean hasArchives = false;
    for( Path path : this.paths ) {
      if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) ) {
	hasDirs = true;
      }
      if( Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS ) ) {
	Path namePath = path.getFileName();
	if( namePath != null ) {
	  String fName = namePath.toString();
	  if( fName != null ) {
	    fName = fName.toLowerCase();
	    if( fName.endsWith( ".jar" ) || fName.endsWith( ".zip" ) ) {
	      hasArchives = true;
	    }
	  }
	}
      }
      if( hasDirs && hasArchives ) {
	break;
      }
    }
    this.btnRecursive.setEnabled( hasDirs );
    this.btnWithinArchiveFiles.setEnabled( hasDirs || hasArchives );


    // Ausgabe
    this.fldLog = new JTextArea( 4, 0 );
    this.fldLog.setEditable( false );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.BOTH;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.weightx       = 1.0;
    gbc.weighty       = 1.0;
    gbc.gridwidth     = 2;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( new JScrollPane( this.fldLog ), gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = new JButton( "OK" );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Vorbelegung
    java.util.Date date = null;
    if( !this.paths.isEmpty() ) {
      try {
	FileTime t = Files.getLastModifiedTime( this.paths.get( 0 ) );
	if( t != null ) {
	  date = new java.util.Date( t.toMillis() );
	}
      }
      catch( IOException ex ) {}
    }
    if( date == null ) {
      date = new java.util.Date();
    }
    this.fldTime.setText( this.dateFmtStd.format( date ) );


    // Listener
    this.btnCurrentTime.addActionListener( this );
    this.btnTimeInput.addActionListener( this );
    this.fldTime.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
    this.fldLog.setRows( 0 );
  }


	/* --- Aktionen --- */

  private void doCancel()
  {
    if( this.thread != null ) {
      this.cancelled = true;
      this.btnOK.setEnabled( true );
    } else {
      doClose();
    }
  }


  private void doOK()
  {
    if( this.thread != null ) {
      if( this.cancelled
	  || this.thread.getState().equals( Thread.State.TERMINATED ) )
      {
	doClose();
      }
    } else {
      try {
	if( this.btnCurrentTime.isSelected() ) {
	  this.time = FileTime.fromMillis( System.currentTimeMillis() );
	}
	else if( this.btnTimeInput.isSelected() ) {
	  String text = this.fldTime.getText();
	  if( text != null ) {
	    java.util.Date date = null;
	    try {
	      date = this.dateFmtStd.parse( text );
	    }
	    catch( ParseException ex ) {}
	    if( date == null ) {
	      date = this.dateFmtShort.parse( text );
	    }
	    this.time = FileTime.fromMillis(
				date != null ?
					date.getTime()
					: System.currentTimeMillis() );

	  }
	}
	if( this.time != null ) {
	  this.btnOK.setEnabled( false );
	  this.numChanged   = 0;
	  this.numUnchanged = 0;
	  this.cancelled    = false;
	  this.recursive    = this.btnRecursive.isSelected();
	  this.archiveFiles = this.btnWithinArchiveFiles.isSelected();
	  this.thread       = new Thread( this, "JKCEMU set last modified" );
	  this.thread.start();
	} else {
	  showErrorDlg( this, "Sie m\u00FCssen eine Zeit festlegen." );
	}
      }
      catch( ParseException ex ) {
	showErrorDlg( this, "Datum/Uhrzeit: ung\u00FCltige Eingabe" );
      }
    }
  }


	/* --- private Methoden --- */

  private void appendToLog( String msg )
  {
    if( msg != null ) {
      if( !msg.isEmpty() ) {
	Document doc = this.fldLog.getDocument();
	if( doc != null ) {
	  if( doc.getLength() > 0 ) {
	    this.fldLog.append( "\n\n" );
	  }
	}
	this.fldLog.append( msg );
      }
    }
  }


  private void fireAppendToLog( final String msg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    appendToLog( msg );
		  }
		} );
  }


  private void setLastModified( Path path )
  {
    if( this.time != null ) {
      if( this.archiveFiles
	  && Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS ) )
      {
	Path namePath = path.getFileName();
	if( namePath != null ) {
	  String fName = namePath.toString();
	  if( fName != null ) {
	    fName = fName.toLowerCase();
	    if( fName.endsWith( ".jar" ) || fName.endsWith( ".zip" ) ) {
	      setLastModifiedInZIP( path );
	    }
	  }
	}
      }
      try {
	Files.setLastModifiedTime( path, this.time );
	this.numChanged++;
      }
      catch( Exception ex ) {
	this.numUnchanged++;
      }
    }
  }


  /*
   * Die Methode setzt in allen Eintraegen einer ZIP-Datei
   * den Zeitstempel der letzten Aenderung.
   * Dazu wird eine neue ZIP-Datei angelegt und alle ZIP-Eintraege
   * mit dem geaenderten Zeitstempel in die neue Datei hineinkopiert.
   * Danach wird die neue Datei auf die alte verschoben.
   */
  private void setLastModifiedInZIP( Path file )
  {
    if( (file != null) && (this.time != null) ) {
      InputStream     in     = null;
      ZipInputStream  zipIn  = null;
      ZipOutputStream zipOut = null;
      try {

	// Dateiattribute lesen
	Map<String,Object> attrs = Files.readAttributes(
						file,
						"*",
						LinkOption.NOFOLLOW_LINKS );

	Path tmpFile = null;
	if( !Files.isWritable( file ) ) {
	  throw new IOException( "Datei ist schreibgesch\u00FCtzt." );
	}
	in    = Files.newInputStream( file, StandardOpenOption.READ );
	zipIn = new ZipInputStream( in );

	// ZIP-Datei konnte geoeffnet werden -> Ausgabedatei anlegen
	Path dir = file.getParent();
	if( dir != null ) {
	  tmpFile = Files.createTempFile( dir, "jkcemu_", ".zip" );
	} else {
	  tmpFile = Files.createTempFile( "jkcemu_", ".zip" );
	}
	zipOut = new ZipOutputStream(
			Files.newOutputStream(
					tmpFile,
					StandardOpenOption.CREATE ) );

	// Datei kopieren und dabei die Zeitstempel anpassen
	ZipEntry entry = zipIn.getNextEntry();
	while( entry != null ) {
	  entry.setTime( this.time.toMillis() );
	  zipOut.putNextEntry( entry );
	  int b = zipIn.read();
	  while( b >= 0 ) {
	    zipOut.write( b );
	    b = zipIn.read();
	  }
	  zipOut.closeEntry();
	  zipIn.closeEntry();
	  entry = zipIn.getNextEntry();
	}
	zipOut.finish();
	zipOut.close();
	zipOut = null;

	zipIn.close();
	zipIn = null;

	// temporere Datei auf die alte verschieben
	Files.move( tmpFile, file, StandardCopyOption.REPLACE_EXISTING );

	// Dateiattribute setzen
	if( attrs != null ) {
	  boolean failed = false;
	  for( String s : attrs.keySet() ) {
	    Object v = attrs.get( s );
	    if( v != null ) {
	      try {
		Files.setAttribute( file, s, v, LinkOption.NOFOLLOW_LINKS );
	      }
	      catch( Exception ex ) {
		failed = true;
	      }
	    }
	  }
	  if( failed ) {
	    fireAppendToLog(
		file.toString() + ":\n"
			+ "Die urspr\u00FCnglichen Dateiattribute"
			+ " konnten nicht wieder hergestellt werden." );
	  }
	}
      }
      catch( Exception ex ) {
	StringBuilder buf = new StringBuilder( 256 );
	buf.append( file.toString() );
	buf.append( ":\nDie Zeitstempel der in der Archivdatei enthaltenen\n"
		+ "Dateien konnten nicht ge\u00E4ndert werden." );
	String exMsg = ex.getMessage();
	if( exMsg != null ) {
	  if( !exMsg.isEmpty() ) {
	    buf.append( "\n\n" );
	    buf.append( exMsg );
	  }
	}
	fireAppendToLog( buf.toString() );
      }
      finally {
	EmuUtil.closeSilent( in );
	EmuUtil.closeSilent( zipOut );
      }
    }
  }


  private void threadTerminated( String msg )
  {
    StringBuilder buf = new StringBuilder( 256 );
    if( this.cancelled ) {
      buf.append( "Vorgang abgebrochen\n" );
    }
    buf.append( this.numChanged );
    if( this.numChanged == 1 ) {
      buf.append( " Datei/Verzeichnis" );
    } else {
      buf.append( " Dateien/Verzeichnissen" );
    }
    buf.append( " ge\u00E4ndert" );
    if( this.numUnchanged > 0 ) {
      buf.append( "\n" );
      buf.append( this.numUnchanged );
      if( this.numUnchanged == 1 ) {
	buf.append( " Datei/Verzeichnis konnte" );
      } else {
	buf.append( " Dateien/Verzeichnisse konnten" );
      }
      buf.append( " nicht ge\u00E4ndert werden." );
    }
    if( !this.cancelled ) {
      buf.append( "\nFertig" );
    }
    appendToLog( buf.toString() );
    this.btnOK.setEnabled( true );
    this.btnCancel.setEnabled( false );
  }
}
