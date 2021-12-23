/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Entpacker
 */

package jkcemu.file;

import java.awt.Dialog;
import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import jkcemu.base.AbstractThreadDlg;
import jkcemu.base.EmuUtil;


public class TarUnpacker extends AbstractThreadDlg
{
  private File    srcFile;
  private File    outDir;
  private boolean compression;
  private boolean posixEnabled;


  public static void unpackFile(
			Window  owner,
			File    srcFile,
			File    outDir,
			boolean compression )
  {
    Dialog dlg = new TarUnpacker( owner, srcFile, outDir, compression );
    if( compression ) {
      dlg.setTitle( "TGZ-Datei entpacken" );
    } else {
      dlg.setTitle( "TAR-Datei entpacken" );
    }
    dlg.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void doProgress()
  {
    boolean dirExists = this.outDir.exists();
    this.outDir.mkdirs();
    InputStream in = null;
    try {
      in = openInputFile( this.srcFile, null );
      if( this.compression ) {
	in = new GZIPInputStream( in );
      }
      TarEntry entry = TarEntry.readEntryHeader( in );
      while( !this.cancelled && (entry != null) ) {
	String entryName = entry.getName();
	if( entryName != null ) {
	  appendToLog( entryName + "\n" );
	  String errMsg = entry.getErrorMsg();
	  if( errMsg != null ) {
	    appendErrorToLog( errMsg );
	  }
	  File   outFile = null;
	  String msg     = "  ignoriert";
	  try {
	    int len = entryName.length();
	    int pos = 0;
	    while( pos < len ) {
	      String elem  = null;
	      int    delim = entryName.indexOf( '/', pos );
	      if( delim >= pos ) {
		elem = entryName.substring( pos, delim );
		pos  = delim + 1;
	      } else {
		elem = entryName.substring( pos );
		pos  = len;
	      }
	      if( elem != null ) {
		if( !elem.isEmpty() ) {
		  if( outFile != null ) {
		    outFile = prepareOutFile( outFile, elem );
		  } else {
		    outFile = prepareOutFile( this.outDir, elem );
		  }
		}
	      }
	    }
	    if( outFile != null ) {
	      if( entry.isDirectory() ) {
		outFile.mkdirs();
		if( !outFile.exists() ) {
		  throw new IOException(
				"Verzeichnis kann nicht angelegt werden" );
		}
		setAttributes( outFile, null, entry );
		msg = null;
	      } else if( entry.isSymbolicLink() ) {
		String linkTarget = entry.getLinkTarget();
		if( linkTarget != null ) {
		  File parent = outFile.getParentFile();
		  if( parent != null ) {
		    parent.mkdirs();
		  }
		  boolean done = false;
		  Path    path = outFile.toPath();
		  if( this.posixEnabled ) {
		    try {
		      Files.createSymbolicLink(
				path,
				Paths.get( linkTarget ),
				PosixFilePermissions.asFileAttribute(
					entry.getPosixFilePermissions() ) );
		      done = true;
		    }
		    catch( UnsupportedOperationException ex ) {
		      this.posixEnabled = false;
		    }
		    catch( Exception ex ) {}
		  }
		  if( !done ) {
		    Files.createSymbolicLink( path, Paths.get( linkTarget ) );
		  }
		  Long millis = entry.getTimeMillis();
		  if( millis != null ) {
		    try {
		      Files.setLastModifiedTime(
				path,
				FileTime.fromMillis( millis.longValue() ) );
		    }
		    catch( Exception ex ) {}
		  }
		  msg = null;
		}
	      } else if( entry.isRegularFile() ) {
		File parent = outFile.getParentFile();
		if( parent != null ) {
		  parent.mkdirs();
		}
		OutputStream out = null;
		try {
		  out = new BufferedOutputStream(
				new FileOutputStream( outFile ) );

		  long size   = entry.getSize();
		  long remain = size;
		  while( !this.cancelled && (remain > 0) ) {
		    int b = in.read();
		    if( b < 0 ) {
		      break;
		    }
		    out.write( b );
		    --remain;
		  }
		  if( remain == 0 ) {
		    int blkRemain = (int) ((0L - size) & 0x1FFL);
		    if( blkRemain > 0 ) {
		      in.skip( blkRemain );
		    }
		  }
		  out.close();
		  out = null;
		}
		finally {
		  EmuUtil.closeSilently( out );
		}
		setAttributes( outFile, null, entry );
		msg = null;
	      }
	    }
	  }
	  catch( Exception ex ) {
	    msg          = EmuUtil.TEXT_ERROR;
	    String exMsg = ex.getMessage();
	    if( exMsg != null ) {
	      exMsg = exMsg.trim();
	      if( !exMsg.isEmpty() ) {
		msg = exMsg;
	      }
	    }
	    incErrorCount();
	  }
	  if( msg != null ) {
	    msg += "\n";
	    appendToLog( msg );
	    disableAutoClose();
	    if( outFile != null ) {
	      outFile.delete();
	    }
	  }
	}
	entry = TarEntry.readEntryHeader( in );
      }
    }
    catch( Exception ex ) {
      StringBuilder buf = new StringBuilder( 128 );
      buf.append( "\nFehler beim Lesen der Datei " );
      buf.append( this.srcFile.getPath() );
      String errMsg = ex.getMessage();
      if( errMsg != null ) {
	if( !errMsg.isEmpty() ) {
	  buf.append( ": " );
	  buf.append( errMsg );
	}
      }
      buf.append( '\n' );
      appendErrorToLog( buf.toString() );
      incErrorCount();
    }
    finally {
      EmuUtil.closeSilently( in );
    }
  }


	/* --- private Konstruktoren --- */

  private TarUnpacker(
		Window  owner,
		File    srcFile,
		File    outDir,
		boolean compression )
  {
    super( owner, "JKCEMU tar unpacker", true );
    this.srcFile      = srcFile;
    this.outDir       = outDir;
    this.compression  = compression;
    this.posixEnabled = true;
  }


  private void setAttributes( File file, Path path, TarEntry entry )
  {
    boolean done = false;
    if( path == null ) {
      try {
	path = file.toPath();
      }
      catch( InvalidPathException ex ) {}
    }

    // Zeitpunkt der letzen Aenderung setzen
    Long millis = entry.getTimeMillis();
    if( millis != null ) {
      try {
	if( path != null ) {
	  Files.setLastModifiedTime(
			path,
			FileTime.fromMillis( millis.longValue() ) );
	} else {
	  file.setLastModified( millis.longValue() );
	}
      }
      catch( Exception ex ) {}
    }

    // Dateiberechtigungen setzen
    Set<PosixFilePermission> perms = entry.getPosixFilePermissions();
    if( !this.posixEnabled ) {
      try {
	Files.setPosixFilePermissions( file.toPath(), perms );
	done = true;
      }
      catch( IOException ex ) {}
      catch( Exception ex ) {
	this.posixEnabled = false;
      }
    }
    if( !done ) {
      try {
	file.setExecutable(
		perms.contains( PosixFilePermission.OTHERS_EXECUTE )
			|| perms.contains( PosixFilePermission.GROUP_EXECUTE ),
		false );
	file.setExecutable(
		perms.contains( PosixFilePermission.OWNER_EXECUTE ),
		true );
	file.setWritable(
		perms.contains( PosixFilePermission.OTHERS_WRITE )
			|| perms.contains( PosixFilePermission.GROUP_WRITE ),
		false );
	file.setWritable(
		perms.contains( PosixFilePermission.OWNER_WRITE ),
		true );
	file.setReadable(
		perms.contains( PosixFilePermission.OTHERS_READ )
			|| perms.contains( PosixFilePermission.GROUP_READ ),
		false );
	file.setReadable(
		perms.contains( PosixFilePermission.OWNER_READ ),
		true );
      }
      catch( Exception ex ) {}
    }
  }
}
