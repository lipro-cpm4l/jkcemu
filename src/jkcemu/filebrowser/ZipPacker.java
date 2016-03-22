/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * ZIP-Packer
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Collection;
import java.util.zip.*;
import jkcemu.base.*;


public class ZipPacker extends AbstractThreadDlg
			implements FileVisitor<Path>
{
  private Collection<Path> srcPaths;
  private String           curEntryDir;
  private Path             curRootPath;
  private Path             outPath;
  private File             outFile;
  private ZipOutputStream  out;


  public static void packFiles(
		Window           owner,
		Collection<Path> srcPaths,
		File             outFile )
  {
    try {
      Dialog dlg = new ZipPacker( owner, srcPaths, outFile );
      dlg.setTitle( "ZIP-Datei packen" );
      dlg.setVisible( true );
    }
    catch( InvalidPathException ex ) {}
  }


	/* --- FileVisitor --- */

  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    return this.canceled ? FileVisitResult.TERMINATE
				: FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.SKIP_SUBTREE;
    if( (this.out == null) || this.canceled ) {
      rv = FileVisitResult.TERMINATE;
    } else {
      if( (dir != null) && (attrs != null) ) {
	if( attrs.isDirectory() ) {
	  this.curEntryDir = getEntryDir( dir );
	  if( this.curEntryDir != null ) {
	    if( !this.curEntryDir.isEmpty() ) {
	      appendToLog( dir.toString() + "\n" );
	      try {
		ZipEntry entry = new ZipEntry( this.curEntryDir );
		entry.setMethod( ZipEntry.STORED );
		entry.setCrc( 0 );
		entry.setCompressedSize( 0 );
		entry.setSize( 0 );
		FileTime lastModified = attrs.lastModifiedTime();
		if( lastModified != null ) {
		  entry.setTime( lastModified.toMillis() );
		}
		this.out.putNextEntry( entry );
		this.out.closeEntry();
		rv = FileVisitResult.CONTINUE;
	      }
	      catch( IOException ex ) {
		appendErrorToLog( ex );
	      }
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
    if( (this.out == null) || this.canceled ) {
      rv = FileVisitResult.TERMINATE;
    } else {
      if( (file != null) && (attrs != null) ) {
	if( this.curEntryDir == null ) {
	  this.curEntryDir = getEntryDir( file.getParent() );
	}
	if( this.curEntryDir != null ) {
	  int nameCnt = file.getNameCount();
	  if( nameCnt > 0 ) {
	    appendToLog( file.toString() + "\n" );
	    if( attrs.isRegularFile() ) {
	      FileProgressInputStream in = null;
	      try {
		in = openInputFile( file.toFile(), 2 );

		// CRC32-Pruefsumme und Dateigroesse ermitteln
		CRC32 crc32 = new CRC32();
		long  fSize = 0;
		int   b     = in.read();
		while( !this.canceled && (b != -1) ) {
		  fSize++;
		  crc32.update( b );
		  b = in.read();
		}
		in.seek( 0 );

		// ZIP-Eintrag anlegen
		ZipEntry entry = new ZipEntry( this.curEntryDir
				+ file.getName( nameCnt - 1 ).toString() );
		entry.setCrc( crc32.getValue() );
		entry.setMethod( ZipEntry.DEFLATED );
		entry.setSize( fSize );
		FileTime lastModified = attrs.lastModifiedTime();
		if( lastModified != null ) {
		  entry.setTime( lastModified.toMillis() );
		}
		this.out.putNextEntry( entry );

		// Datei in Eintrag schreiben
		b = in.read();
		while( !this.canceled && (b != -1) ) {
		  this.out.write( b );
		  b = in.read();
		}
		this.out.closeEntry();
	      }
	      catch( IOException ex ) {
		appendErrorToLog( ex );
		incErrorCount();
	      }
	      catch( UnsupportedOperationException ex ) {
		appendIgnoredToLog();
	      }
	      finally {
		EmuUtil.doClose( in );
	      }
	    } else if( attrs.isSymbolicLink() ) {
	      appendToLog( " Symbolischer Link ignoriert\n" );
	      disableAutoClose();
	    } else {
	      appendIgnoredToLog();
	    }
	    rv = FileVisitResult.CONTINUE;
	  }
	}
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
    return this.canceled ? FileVisitResult.TERMINATE
				: FileVisitResult.CONTINUE;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void doProgress()
  {
    boolean     failed = false;
    InputStream in     = null;
    try {
      this.out = new ZipOutputStream(
			new BufferedOutputStream(
				new FileOutputStream( this.outFile ) ) );
      FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
      for( Path path : this.srcPaths ) {
	this.curRootPath = path.getParent();
	Files.walkFileTree( path, this );
      }
      this.out.finish();
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
      EmuUtil.doClose( this.out );
    }
    if( this.canceled || failed ) {
      this.outFile.delete();
    }
    FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
  }


	/* --- privater Konstruktor --- */

  private ZipPacker(
		Window           owner,
		Collection<Path> srcPaths,
		File             outFile ) throws InvalidPathException
  {
    super( owner, "JKCEMU zip packer", true );
    this.srcPaths = srcPaths;
    this.outPath  = outFile.toPath();
    this.outFile  = outFile;
    this.out      = null;
  }


  private String getEntryDir( Path dir )
  {
    String rv = null;
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
}

