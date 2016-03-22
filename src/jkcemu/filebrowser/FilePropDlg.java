/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige der Eigenschaften einer Datei- bzw. eines Verzeichnises
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.IOException;
import java.lang.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class FilePropDlg
		extends BasicDlg
		implements FileVisitor<Path>, Runnable
{
  private Path             path;
  private JButton          btnOK;
  private JLabel           sizeLabel;
  private long             dirSize;
  private volatile boolean threadEnabled;


  public FilePropDlg( Frame parent, Path path ) throws IOException
  {
    super( parent, "Eigenschaften" );
    this.path          = path;
    this.sizeLabel     = null;
    this.dirSize       = 0;
    this.threadEnabled = true;
    try {

      // Dateiattribute lesen
      UserPrincipal       owner     = null;
      GroupPrincipal      group     = null;
      String              posixPerm = null;
      BasicFileAttributes attrs     = null;
      try {
	PosixFileAttributeView view = Files.getFileAttributeView(
						path,
						PosixFileAttributeView.class,
						LinkOption.NOFOLLOW_LINKS );
	if( view != null ) {
	  PosixFileAttributes pAttrs = view.readAttributes();
	  if( pAttrs != null ) {
	    attrs     = pAttrs;
	    owner     = pAttrs.owner();
	    group     = pAttrs.group();
	    posixPerm = PosixFilePermissions.toString( pAttrs.permissions() );
	  }
	}
      }
      catch( Exception ex ) {}
      if( attrs == null ) {
	attrs = Files.readAttributes(
				path,
				BasicFileAttributes.class,
				LinkOption.NOFOLLOW_LINKS );
      }
      if( owner == null ) {
	try {
	  owner = Files.getOwner( path, LinkOption.NOFOLLOW_LINKS );
	}
	catch( Exception ex ) {}
      }

      // Ausgabeliste aufbereiten
      int                      sizeLineIdx = -1;
      java.util.List<String[]> lines       = new ArrayList<>();
      String                   text        = path.toString();
      Path                     namePath    = path.getFileName();
      if( namePath != null ) {
	String s = namePath.toString();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    text = s;
	  }
	}
      }
      if( text != null ) {
	if( !text.isEmpty() ) {
	  lines.add( new String[] { "Name:", text } );
	}
      }
      if( attrs.isRegularFile() ) {
	long size = attrs.size();
	if( size >= 0 ) {
	  lines.add( new String[] {
			"Gr\u00F6\u00DFe:",
			EmuUtil.formatSize( size, false, true ) } );
	}
      }
      else if( attrs.isDirectory() ) {
	sizeLineIdx = lines.size();
	lines.add( new String[] {
			"Gr\u00F6\u00DFe aller Dateien:",
			"wird berechnet..." } );
      }
      else if( attrs.isSymbolicLink() ) {
	try {
	  String s = null;
	  Path   p = Files.readSymbolicLink( path );
	  if( p != null ) {
	    s = p.toString();
	  }
	  lines.add( new String[] {
				"Symbolischer Link auf:",
				s != null ? s : "" } );
	}
	catch( Exception ex ) {}
      }
      try {
	FileTime t = Files.getLastModifiedTime( path );
	if( t != null ) {
	  lines.add( new String[] {
		"Zuletzt ge\u00E4ndert:",
		DateFormat.getDateTimeInstance(
			DateFormat.MEDIUM,
			DateFormat.MEDIUM ).format(
				new java.util.Date( t.toMillis() ) ) } );
	}
      }
      catch( Exception ex ) {}
      if( owner != null ) {
	String s = owner.getName();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    lines.add( new String[] { "Eigent\u00FCmer:", s } );
	  }
	}
      }
      if( group != null ) {
	String s = group.getName();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    lines.add( new String[] { "Gruppe:", s } );
	  }
	}
      }
      if( posixPerm != null ) {
	lines.add( new String[] { "Berechtigungen:", posixPerm } );
      }
      try {
	lines.add( new String[] {
		"Lesezugriff:",
		Files.isReadable( path ) ? "ja" : "nein" } );
      }
      catch( Exception ex ) {}
      try {
	lines.add( new String[] {
		"Schreibzugriff:",
		Files.isWritable( path ) ? "ja" : "nein" } );
      }
      catch( Exception ex ) {}
      try {
	lines.add( new String[] {
		"Versteckt:",
		Files.isHidden( path ) ? "ja" : "nein" } );
      }
      catch( Exception ex ) {}


      // Layout
      setLayout( new GridBagLayout() );

      GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 5, 0, 5 ),
					0, 0 );


      // Fensterinhalt
      int nLines = lines.size();
      for( int i = 0; i < nLines; i++ ) {
	String[] line = lines.get( i );
	if( line.length > 1 ) {
	  gbc.gridx = 0;
	  add( new JLabel( line[ 0 ] ), gbc );
	  gbc.gridx++;
	  JLabel label = new JLabel( line[ 1 ] );
	  add( label, gbc );
	  gbc.gridy++;
	  if( i == sizeLineIdx ) {
	    this.sizeLabel = label;
	  }
	}
      }

      // Knopf
      this.btnOK = new JButton( "OK" );
      this.btnOK.addActionListener( this );

      gbc.anchor        = GridBagConstraints.CENTER;
      gbc.insets.top    = 10;
      gbc.insets.bottom = 10;
      gbc.gridwidth     = GridBagConstraints.REMAINDER;
      gbc.gridx         = 0;
      gbc.gridy++;
      add( this.btnOK, gbc );


      // Fenstergroesse und -position
      pack();
      setParentCentered();
      setResizable( false );


      // Verzeichnisgroesse ermitteln
      if( this.threadEnabled && (this.sizeLabel != null) ) {
	(new Thread(
		Main.getThreadGroup(),
		this,
		"JKCEMU directory size calculator" )).start();
      }
    }
    catch( UnsupportedOperationException ex ) {
      throw new IOException(
		"Dateiattribute k\u00E4nnen nicht gelesen werden.\n" );
    }
  }


	/* --- FileVisistor --- */

  @Override
  public FileVisitResult postVisitDirectory(
				Path        dir,
				IOException ex ) throws IOException
  {
    if( ex != null ) {
      throw ex;
    }
    return this.threadEnabled ?
		FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    return this.threadEnabled ?
		FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    if( attrs.isRegularFile() ) {
      long size = attrs.size();
      if( size > 0 ) {
	this.dirSize += size;
      }
    }
    return this.threadEnabled ?
		FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
  }


  @Override
  public FileVisitResult visitFileFailed(
				Path        file,
				IOException ex ) throws IOException
  {
    if( (ex != null)
	&& (Files.isRegularFile( file, LinkOption.NOFOLLOW_LINKS )
		|| Files.isDirectory( file, LinkOption.NOFOLLOW_LINKS )) )
    {
      throw ex;
    }
    return this.threadEnabled ?
		FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    if( this.threadEnabled && (this.sizeLabel != null) ) {
      String sizeText = "unbekannt";
      try {
	this.dirSize = 0;
	Files.walkFileTree( this.path, this );
	if( this.threadEnabled ) {
	  sizeText = EmuUtil.formatSize( this.dirSize, false, true );
	}
      }
      catch( AccessDeniedException ex ) {
	sizeText = "wegen fehlender Berechtigung nicht ermittelbar";
      }
      catch( Exception ex ) {
	sizeText = "konnte aufgrund eines Fehlers nicht ermittelt werden";
      }
      final String text = sizeText;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    setSizeText( text );
		  }
		} );
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
        if( src == this.btnOK ) {
	  rv = true;
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
      this.threadEnabled = false;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void setSizeText( String text )
  {
    if( this.sizeLabel != null ) {
      this.sizeLabel.setText( text );
      pack();
      setParentCentered();
    }
  }
}
