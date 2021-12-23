/*
 * (c) 2008-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * ZIP-Packer
 */

package jkcemu.file;

import java.awt.Dialog;
import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jkcemu.base.AbstractThreadDlg;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileProgressInputStream;


public class ZipPacker extends AbstractThreadDlg
			implements FileVisitor<Path>
{
  private Collection<Path> srcPaths;
  private Path             curRootPath;
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
	      ZipEntry entry = new ZipEntry( entryDir );
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

		  // Datei oeffnen und dabei die Pruefsumme berechnen
		  CRC32 crc32 = new CRC32();
		  in          = openInputFile( file.toFile(), crc32 );

		  // ZIP-Eintrag anlegen
		  ZipEntry entry = new ZipEntry( entryDir + fileName );
		  entry.setCrc( crc32.getValue() );
		  entry.setMethod( ZipEntry.DEFLATED );
		  entry.setSize( in.getFileSize() );
		  FileTime lastModified = attrs.lastModifiedTime();
		  if( lastModified != null ) {
		    entry.setTime( lastModified.toMillis() );
		  }
		  this.out.putNextEntry( entry );

		  // Datei in Eintrag schreiben
		  int b = in.read();
		  while( !this.cancelled && (b != -1) ) {
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
		  EmuUtil.closeSilently( in );
		}
	      } else if( attrs.isSymbolicLink() ) {
		appendToLog( " Symbolischer Link ignoriert\n" );
		disableAutoClose();
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
    boolean     failed = false;
    InputStream in     = null;
    try {
      this.out = new ZipOutputStream(
			new BufferedOutputStream(
				new FileOutputStream( this.outFile ) ) );
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
      buf.append( '\n' );
      appendToLog( buf.toString() );
      incErrorCount();
      failed = true;
    }
    finally {
      EmuUtil.closeSilently( this.out );
    }
    if( this.cancelled || failed ) {
      this.outFile.delete();
    }
  }


	/* --- privater Konstruktor --- */

  private ZipPacker(
		Window           owner,
		Collection<Path> srcPaths,
		File             outFile ) throws InvalidPathException
  {
    super( owner, "JKCEMU zip packer", true );
    this.srcPaths = srcPaths;
    this.outFile  = outFile;
    this.out      = null;
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
}
