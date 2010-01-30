/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Entpacker
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.*;


public class TarUnpacker extends AbstractThreadDlg
{
  private File    srcFile;
  private File    outDir;
  private boolean compression;


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

  protected void doProgress()
  {
    boolean dirExists = this.outDir.exists();
    this.outDir.mkdirs();
    InputStream in = null;
    try {
      in = openInputFile( this.srcFile, 1 );
      if( this.compression ) {
	in = new GZIPInputStream( in );
      }
      TarEntry entry = TarEntry.readEntryHeader( in );
      while( !this.canceled && (entry != null) ) {
	String entryName = entry.getName();
	if( entryName != null ) {
	  String errMsg = entry.getErrorMsg();
	  if( errMsg != null ) {
	    appendToLog( "Fehler bei Eintrag " + entryName + ":\n" + errMsg );
	  }
	  File outFile = null;
	  int  len     = entryName.length();
	  int  pos     = 0;
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
		  outFile = new File( outFile, elem );
		} else {
		  outFile = new File( this.outDir, elem );
		}
	      }
	    }
	  }
	  if( outFile != null ) {
	    appendToLog( outFile.getPath() + "\n" );
	    long millis = entry.getTime();
	    if( entry.isDirectory() || entryName.endsWith( "/" ) ) {
	      outFile.mkdirs();
	      if( !outFile.exists() ) {
		throw new IOException(
				"Verzeichnis kann nicht angelegt werden" );
	      }
	    } else {
	      File parent = outFile.getParentFile();
	      if( parent != null ) {
		parent.mkdirs();
	      }
	      boolean      failed = false;
	      OutputStream out    = null;
	      try {
		out = new BufferedOutputStream(
				new FileOutputStream( outFile ) );

		long size = entry.getSize();
		if( size > 0 ) {
		  pos   = 1;
		  int b = in.read();
		  while( !this.canceled && (pos < size) && (b != -1) ) {
		    out.write( b );
		    b = in.read();
		    pos++;
		  }
		  if( (pos % 512) != 0 ) {
		    in.skip( ((pos + 512) & ~0x1FF) - pos );
		  }
		}
		out.close();
		out = null;
	      }
	      catch( IOException ex ) {
		appendErrorToLog( ex );
		incErrorCount();
		failed = true;
	      }
	      finally {
		EmuUtil.doClose( out );
	      }
	      if( failed ) {
		outFile.delete();
	      } else {
		if( millis > 0 ) {
		  outFile.setLastModified( millis );
		}
		int mode = entry.getMode();
		outFile.setExecutable( (mode & 0x001) != 0, false );
		outFile.setExecutable( (mode & 0x040) != 0, true );
		outFile.setReadable( (mode & 0x004) != 0, false );
		outFile.setReadable( (mode & 0x100) != 0, true );
		outFile.setWritable( (mode & 0x002) != 0, false );
		outFile.setWritable( (mode & 0x080) != 0, true );
	      }
	    }
	  }
	}
	entry = TarEntry.readEntryHeader( in );
      }
    }
    catch( IOException ex ) {
      StringBuilder buf = new StringBuilder( 128 );
      buf.append( "\nFehler beim Lesen der Datei " );
      buf.append( this.srcFile.getPath() );
      String errMsg = ex.getMessage();
      if( errMsg != null ) {
	if( errMsg.length() > 0 ) {
	  buf.append( ": " );
	  buf.append( errMsg );
	}
      }
      buf.append( (char) '\n' );
      appendToLog( buf.toString() );
      incErrorCount();
    }
    finally {
      EmuUtil.doClose( in );
    }
    fireDirectoryChanged(
		dirExists ? this.outDir : this.outDir.getParentFile() );
  }


	/* --- private Konstruktoren --- */

  private TarUnpacker(
		Window  owner,
		File    srcFile,
		File    outDir,
		boolean compression )
  {
    super( owner, true );
    this.srcFile     = srcFile;
    this.outDir      = outDir;
    this.compression = compression;
  }
}

