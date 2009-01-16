/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * ZIP-Packer
 */

package jkcemu.filebrowser;

import java.awt.Dialog;
import java.io.*;
import java.lang.*;
import java.util.Collection;
import java.util.zip.*;


public class ZipPacker extends AbstractPackDlg
{
  private Collection<File> srcFiles;
  private File             outFile;


  public static void packFiles(
		FileBrowserFrm   fileBrowserFrm,
		Collection<File> srcFiles,
		File             outFile )
  {
    Dialog dlg = new ZipPacker( fileBrowserFrm, srcFiles, outFile );
    dlg.setTitle( "ZIP-Datei packen" );
    dlg.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  protected void doProgress()
  {
    boolean         failed = false;
    InputStream     in     = null;
    ZipOutputStream out    = null;
    try {
      out = new ZipOutputStream(
			new BufferedOutputStream(
				new FileOutputStream( this.outFile ) ) );
      this.fileBrowserFrm.fireRefreshNodeFor( outFile.getParentFile() );
      for( File file : this.srcFiles ) {
	packFile( outFile, out, file, "" );
      }
      out.finish();
      out.close();
      out = null;
      appendToLog( "\nFertig\n" );
    }
    catch( InterruptedIOException ex ) {}
    catch( IOException ex ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "\nFehler beim Schreiben in Datei " );
      buf.append( this.outFile.getPath() );
      String errMsg = ex.getMessage();
      if( errMsg != null ) {
	if( errMsg.length() > 0 ) {
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
      close( out );
    }
    if( this.canceled || failed ) {
      outFile.delete();
    }
    this.fileBrowserFrm.fireRefreshNodeFor( outFile.getParentFile() );
  }


	/* --- private Konstruktoren und Methoden --- */

  private ZipPacker(
		FileBrowserFrm   fileBrowserFrm,
		Collection<File> srcFiles,
		File             outFile )
  {
    super( fileBrowserFrm );
    this.srcFiles = srcFiles;
    this.outFile  = outFile;
  }


  private void packFile(
		File            outFile,
		ZipOutputStream out,
		File            file,
		String          path ) throws IOException
  {
    if( this.canceled ) {
      throw new InterruptedIOException();
    }
    if( file != null ) {
      appendToLog( file.getPath() + "\n" );
      String entryName = file.getName();
      if( !file.equals( outFile) && (entryName != null) ) {
	if( entryName.length() > 0 ) {
	  long millis = file.lastModified();
	  if( file.isDirectory() ) {
	    String   subPath = path + entryName + "/";
	    ZipEntry entry   = new ZipEntry( subPath );
	    entry.setMethod( ZipEntry.STORED );
	    entry.setCrc( 0 );
	    entry.setCompressedSize( 0 );
	    entry.setSize( 0 );
	    if( millis > 0 ) {
	      entry.setTime( millis );
	    }
	    out.putNextEntry( entry );
	    out.closeEntry();
	    File[] subFiles = file.listFiles();
	    if( subFiles != null ) {
	      for( int i = 0; i < subFiles.length; i++ ) {
		packFile( outFile, out, subFiles[ i ], subPath );
	      }
	    } else {
	      appendToLog(
			"Fehler: Verzeichnis kann nicht gelesen werden.\n" );
	    }
	  }
	  else if( file.isFile() ) {
	    FileProgressInputStream in = null;
	    try {
	      in = openInputFile( file, 2 );

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
	      ZipEntry entry  = new ZipEntry( path + entryName );
	      entry.setCrc( crc32.getValue() );
	      entry.setMethod( ZipEntry.DEFLATED );
	      entry.setSize( fSize );
	      if( millis > 0 ) {
		entry.setTime( millis );
	      }
	      out.putNextEntry( entry );

	      // Datei in Eintrag schreiben
	      b = in.read();
	      while( !this.canceled && (b != -1) ) {
		out.write( b );
		b = in.read();
	      }
	      out.closeEntry();
	    }
	    catch( IOException ex ) {
	      String errMsg = ex.getMessage();
	      if( errMsg != null ) {
		appendToLog( "  Fehler:\n" + errMsg + "\n" );
	      } else {
		appendToLog( "  Fehler\n" );
	      }
	      incErrorCount();
	    }
	    finally {
	      close( in );
	    }
	  } else {
	    appendToLog( "  Ignoriert\n" );
	  }
	}
      } else {
	appendToLog( "  Nicht gefunden\n" );
      }
    }
  }
}

