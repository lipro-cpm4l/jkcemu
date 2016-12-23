/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Packer
 */

package jkcemu.filebrowser;

import java.awt.Dialog;
import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import jkcemu.base.AbstractThreadDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileProgressInputStream;


public class TarPacker extends AbstractThreadDlg
			implements FileVisitor<Path>
{
  private Collection<Path> srcPaths;
  private Path             curRootPath;
  private File             outFile;
  private OutputStream     out;
  private boolean          compression;
  private boolean          ownerEnabled;
  private boolean          posixEnabled;


  public static void packFiles(
			Window           owner,
			Collection<Path> srcPaths,
			File             outFile,
			boolean          compression )
  {
    Dialog dlg = new TarPacker( owner, srcPaths, outFile, compression );
    if( compression ) {
      dlg.setTitle( "TGZ-Datei packen" );
    } else {
      dlg.setTitle( "TAR-Datei packen" );
    }
    dlg.setVisible( true );
  }


	/* --- FileVisitor --- */

  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    return this.cancelled ? FileVisitResult.TERMINATE
				: FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.SKIP_SUBTREE;
    if( (this.out == null) || this.cancelled ) {
      rv = FileVisitResult.TERMINATE;
    } else {
      if( (dir != null) && (attrs != null) ) {
	if( attrs.isDirectory() ) {
	  String entryDir = getEntryDir( dir );
	  if( !entryDir.isEmpty() ) {
	    appendToLog( dir.toString() + "\n" );
	    try {
	      writeTarHeader(
			dir,
			entryDir,
			'5',			// Typ: Directory
			null,			// verlinkter Name
			0,			// Dateilaenge
			attrs,
			true );		// executable
	      rv = FileVisitResult.CONTINUE;
	    }
	    catch( IOException ex ) {
	      appendErrorToLog( ex );
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.SKIP_SIBLINGS;
    if( (this.out == null) || this.cancelled ) {
      rv = FileVisitResult.TERMINATE;
    } else {
      if( (file != null) && (attrs != null) ) {
	Path namePath = file.getFileName();
	if( namePath != null ) {
	  String fileName = namePath.toString();
	  if( fileName != null ) {
	    if( !fileName.isEmpty() ) {
	      appendToLog( file.toString() + "\n" );
	      String entryDir = getEntryDir( file.getParent() );
	      if( attrs.isRegularFile() ) {
		FileProgressInputStream in = null;
		try {
		  in       = openInputFile( file.toFile(), 2 );
		  long len = in.length();
		  writeTarHeader(
			file,
			entryDir + fileName,
			'0',			// Typ: Datei
			null,			// verlinkter Name
			len,
			attrs,
			false );
		  if( len > 0 ) {
		    long pos = 0;
		    int  b   = in.read();
		    while( !this.cancelled && (pos < len) && (b != -1) ) {
		      this.out.write( b );
		      pos++;
		      b = in.read();
		    }
		    long nBlocks = len / 512;
		    long fullLen = nBlocks * 512;
		    if( fullLen < len ) {
		      fullLen += 512;
		    }
		    while( !this.cancelled && (pos < fullLen) ) {
		      this.out.write( 0 );
		      pos++;
		    }
		  }
		}
		catch( IOException ex ) {
		  appendErrorToLog( ex );
		  incErrorCount();
		}
		catch( UnsupportedOperationException ex ) {
		  appendIgnoredToLog();
		}
		finally {
		  EmuUtil.closeSilent( in );
		}
	      } else if( attrs.isSymbolicLink() ) {
		try {
		  Path destPath = Files.readSymbolicLink( file );
		  if( destPath != null ) {
		    writeTarHeader(
			file,
			entryDir + fileName,
			'2',			// Typ: Link
			destPath.toString(),	// verlinkter Name
			0,			// Dateilaenge
			attrs,
			true );			// executable
		  }
		}
		catch( Exception ex ) {
		  StringBuilder buf = new StringBuilder( 256 );
		  buf.append( " Lesen des symbolischen Links"
					+ " nicht m\u00F6glich\n" );
		  String msg = ex.getMessage();
		  if( msg != null ) {
		    msg = msg.trim();
		    if( msg.isEmpty() ) {
		      buf.append( ":\n" );
		      buf.append( msg );
		    }
		  }
		  buf.append( (char) '\n' );
		  appendToLog( buf.toString() );
		  disableAutoClose();
		}
	      } else {
		appendIgnoredToLog();
	      }
	    }
	  }
	}
	rv = FileVisitResult.CONTINUE;
      }
    }
    return rv;
  }


  @Override
  public FileVisitResult visitFileFailed( Path file, IOException ex )
  {
    if( file != null ) {
      appendToLog( file.toString() );
      appendErrorToLog( ex );
      incErrorCount();
    }
    return this.cancelled ? FileVisitResult.TERMINATE
				: FileVisitResult.CONTINUE;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void doProgress()
  {
    boolean     failed  = false;
    InputStream in      = null;
    try {
      this.out = new BufferedOutputStream(
				new FileOutputStream( this.outFile ) );
      if( this.compression ) {
	this.out = new GZIPOutputStream( this.out );
      }
      FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
      for( Path path : this.srcPaths ) {
        this.curRootPath = path.getParent();
        Files.walkFileTree( path, this );
      }
      for( int i = 0; i < 1024; i++ ) {
	this.out.write( 0 );
      }
      if( this.out instanceof GZIPOutputStream ) {
	((GZIPOutputStream) this.out).finish();
      }
      this.out.close();
      this.out = null;
      appendToLog( "\nFertig\n" );
    }
    catch( InterruptedIOException ex ) {}
    catch( IOException ex ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "\nFehler beim Schreiben in Datei " );
      buf.append( this.outFile.getPath() );
      String errMsg = ex.getMessage();
      if( errMsg != null ) {
	if( !errMsg.isEmpty() ) {
	  buf.append( ":\n" );
	  buf.append( errMsg );
	}
      }
      buf.append( (char) '\n' );
      appendToLog( buf.toString() );
      incErrorCount();
      failed = true;
    }
    finally {
      EmuUtil.closeSilent( this.out );
    }
    if( this.cancelled || failed ) {
      this.outFile.delete();
    }
    FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
  }


	/* --- private Konstruktoren und Methoden --- */

  private TarPacker(
		Window           owner,
		Collection<Path> srcPaths,
		File             outFile,
		boolean          compression )
  {
    super( owner, "JKCEMU tar packer", true );
    this.srcPaths     = srcPaths;
    this.outFile      = outFile;
    this.out          = null;
    this.compression  = compression;
    this.ownerEnabled = true;
    this.posixEnabled = true;
  }


  private String getEntryDir( Path dir )
  {
    String rv = "";
    if( this.curRootPath != null ) {
      Path relPath = null;
      if( dir != null ) {
	try {
	  relPath = this.curRootPath.relativize( dir );
	}
	catch( IllegalArgumentException ex ) {}
      }
      if( relPath == null ) {
	relPath = this.curRootPath;
      }
      if( relPath.getNameCount() > 0 ) {
	StringBuilder buf = new StringBuilder( 256 );
	for( Path p : relPath ) {
	  String s = p.toString();
	  if( s != null ) {
	    if( !s.isEmpty() ) {
	      buf.append( s );
	      buf.append( "/" );
	    }
	  }
	}
	rv = buf.toString();
      }
    }
    return rv;
  }


  private void writeASCII( byte[] buf, int pos, String text )
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	buf[ pos++ ] = (byte) (text.charAt( i ) & 0xFF );
      }
    }
  }


  private void writeTarHeader(
			Path                path,
			String              entryName,
			char                entryType,
			String              linkedName,
			long                fileSize,
			BasicFileAttributes attrs,
			boolean             defaultExec ) throws IOException
  {
    byte[] headerBuf = new byte[ 512 ];
    Arrays.fill( headerBuf, (byte) 0 );

    /*
     * Benutzer, Gruppe und Posix-Dateiattribute ermitteln,
     * sofern moeglich
     */
    UserPrincipal            owner       = null;
    GroupPrincipal           group       = null;
    Set<PosixFilePermission> permissions = null;
    PosixFileAttributes      posixAttrs  = null;
    if( attrs instanceof PosixFileAttributes ) {
      posixAttrs = (PosixFileAttributes) attrs;
    } else {
      if( this.posixEnabled ) {
	try {
	  posixAttrs = (PosixFileAttributes) Files.readAttributes(
						path,
						PosixFileAttributes.class,
						LinkOption.NOFOLLOW_LINKS );
	}
	catch( UnsupportedOperationException ex ) {
	  this.posixEnabled = false;
	}
	catch( Exception ex ) {}
      }
    }
    if( posixAttrs != null ) {
      owner       = posixAttrs.owner();
      group       = posixAttrs.group();
      permissions = posixAttrs.permissions();
    }
    if( owner == null ) {
      try {
	owner = Files.getOwner( path, LinkOption.NOFOLLOW_LINKS );
      }
      catch( UnsupportedOperationException ex ) {
	this.ownerEnabled = false;
      }
      catch( Exception ex ) {}
    }

    // 0-99: Name
    int len = entryName.length();
    if( len > 99 ) {
      throw new IOException( "Name des Eintrags zu lang" );
    }
    byte[] nameBytes = null;
    try {
      nameBytes = entryName.getBytes( "ISO-8859-1" );
    }
    catch( Exception ex ) {}
    if( nameBytes == null ) {
      nameBytes = entryName.getBytes();
    }
    if( nameBytes.length != len ) {
      throw new IOException( "Name enth\u00E4lt nicht erlaubte Zeichen" );
    }
    System.arraycopy( nameBytes, 0, headerBuf, 0, nameBytes.length );

    // 100-107: Mode
    String permissionsText = "0000644";
    if( permissions != null ) {
      int bits = 0;
      if( permissions.contains( PosixFilePermission.OWNER_READ ) ) {
	bits |= 0x400;
      }
      if( permissions.contains( PosixFilePermission.OWNER_WRITE ) ) {
	bits |= 0x200;
      }
      if( permissions.contains( PosixFilePermission.OWNER_EXECUTE ) ) {
	bits |= 0x100;
      }
      if( permissions.contains( PosixFilePermission.GROUP_READ ) ) {
	bits |= 0x040;
      }
      if( permissions.contains( PosixFilePermission.GROUP_WRITE ) ) {
	bits |= 0x020;
      }
      if( permissions.contains( PosixFilePermission.GROUP_EXECUTE ) ) {
	bits |= 0x010;
      }
      if( permissions.contains( PosixFilePermission.OTHERS_READ ) ) {
	bits |= 0x004;
      }
      if( permissions.contains( PosixFilePermission.OTHERS_WRITE ) ) {
	bits |= 0x002;
      }
      if( permissions.contains( PosixFilePermission.OTHERS_EXECUTE ) ) {
	bits |= 0x001;
      }
      permissionsText = String.format( "%07X", bits );
    } else {
      if( attrs.isDirectory() || defaultExec ) {
	permissionsText = "0000755";
      }
    }
    writeASCII( headerBuf, 100, permissionsText );

    // 108-115: Benutzer-ID
    writeASCII( headerBuf, 108, "0000000" );

    // 116-123: Gruppe-ID
    writeASCII( headerBuf, 116, "0000000" );

    // 124-135: Laenge
    writeASCII( headerBuf, 124, String.format( "%011o", fileSize ) );

    // 136-147: Aenderungszeitpunkt
    long     fileMillis = System.currentTimeMillis();
    FileTime fileTime   = attrs.lastModifiedTime();
    if( fileTime != null ) {
      fileMillis = fileTime.toMillis();
    }
    writeASCII(
	headerBuf,
	136,
	String.format( "%011o", fileMillis / 1000L ) );

    // 148-155: Pruefsumme des Kopfblocks
    int pos = 148;
    while( pos < 156 ) {
      headerBuf[ pos++ ] = (byte) 0x20;		// wird spaeter ersetzt
    }

    // 156: Typ
    headerBuf[ 156 ] = (byte) entryType;

    // 157-256: Verlinkter Name
    if( linkedName != null ) {
      len = linkedName.length();
      if( len > 99 ) {
	throw new IOException( linkedName + ": Verlinkter Name zu lang" );
      }
      nameBytes = null;
      try {
	nameBytes = linkedName.getBytes( "ISO-8859-1" );
      }
      catch( Exception ex ) {}
      if( nameBytes == null ) {
	nameBytes = entryName.getBytes();
      }
      if( nameBytes.length != len ) {
	throw new IOException( linkedName
		+ ": Verlinkter Name enth\u00E4lt nicht erlaubte Zeichen" );
      }
      System.arraycopy( nameBytes, 0, headerBuf, 157, nameBytes.length );
    }

    // 257-262: Magic
    writeASCII( headerBuf, 257, "ustar\u0020" );

    // 263-264: Version
    headerBuf[ 263 ] = '\u0020';

    // 265-296: Eigentuemername
    String ownerName = "root";
    if( owner != null ) {
      String s = owner.getName();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  int delimPos = s.lastIndexOf( '\\' );
	  if( (delimPos >= 0) && ((delimPos + 1) < s.length()) ) {
	    ownerName = s.substring( delimPos + 1 );
	  } else {
	    ownerName = s;
	  }
	}
      }
    }
    writeASCII( headerBuf, 265, ownerName );

    // 297-328: Gruppenname
    String groupName = null;
    if( group != null ) {
      String s = group.getName();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  groupName = s;
	}
      }
    }
    if( groupName == null ) {
      groupName = (ownerName.equals( "root" ) ? "root" : "users");
    }
    writeASCII( headerBuf, 297, groupName );

    // 329-512: restliche Felder hier unbenutzt

    // Pruefsumme berechnen und eintragen
    int chkSum = 0;
    for( int i = 0; i < headerBuf.length; i++ ) {
      chkSum += ((int) headerBuf[ i ]) & 0xFF;
    }
    writeASCII(
	headerBuf,
	148,
	String.format( "%06o", chkSum ) );
    headerBuf[ 154 ] = (byte) 0;

    // Kopfblock schreiben
    this.out.write( headerBuf );
  }
}
