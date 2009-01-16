/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * ZIP-Entpacker
 */

package jkcemu.filebrowser;

import java.awt.Dialog;
import java.io.*;
import java.lang.*;
import java.util.zip.*;


public class ZipUnpacker extends AbstractPackDlg
{
  private File srcFile;
  private File outDir;


  public static void unpackFile(
		FileBrowserFrm fileBrowserFrm,
		File           srcFile,
		File           outDir )
  {
    Dialog dlg = new ZipUnpacker( fileBrowserFrm, srcFile, outDir );
    dlg.setTitle( "ZIP-Datei entpacken" );
    dlg.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  protected void doProgress()
  {
    boolean dirExists = this.outDir.exists();
    this.outDir.mkdirs();
    InputStream    in    = null;
    ZipInputStream inZip = null;
    try {
      in    = openInputFile( this.srcFile, 1 );
      inZip = new ZipInputStream( new BufferedInputStream( in ) );
      ZipEntry entry = inZip.getNextEntry();
      while( !this.canceled && (entry != null) ) {
	String entryName = entry.getName();
	if( entryName != null ) {
	  File outFile = null;
	  int  len     = entryName.length();
	  int  pos     = 0;
	  while( pos < len ) {
	    String elem     = null;
	    int    delim1 = entryName.indexOf( '/', pos );
	    int    delim2 = entryName.indexOf( '\\', pos );
	    if( (delim1 < 0) || ((delim2 >= 0) && (delim2 < delim1)) ) {
	      delim1 = delim2;
	    }
	    if( delim1 >= pos ) {
	      elem = entryName.substring( pos, delim1 );
	      pos  = delim1 + 1;
	    } else {
	      elem = entryName.substring( pos );
	      pos  = len;
	    }
	    if( elem != null ) {
	      if( elem.length() > 0 ) {
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
	    if( entry.isDirectory()
		|| entryName.endsWith( "/" )
		|| entryName.endsWith( "\\" ) )
	    {
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
		long  chkSum = entry.getCrc();
		CRC32 crc32  = null;
		if( chkSum >= 0 ) {
		  crc32 = new CRC32();
		}
		out = new BufferedOutputStream(
				new FileOutputStream( outFile ) );

		int b = inZip.read();
		while( !this.canceled && (b != -1) ) {
		  if( crc32 != null ) {
		    crc32.update( b );
		  }
		  out.write( b );
		  b = inZip.read();
		}
		out.close();
		out = null;

		if( crc32 != null ) {
		  if( crc32.getValue() != chkSum ) {
		    appendToLog(
			"  Fehler: CRC32-Pr\u00FCfsumme differiert\n" );
		    incErrorCount();
		  }
		}
	      }
	      catch( IOException ex ) {
		StringBuilder buf = new StringBuilder( 128 );
		buf.append( "  Fehler" );
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
		failed = true;
	      }
	      finally {
		close( out );
	      }
	      if( failed ) {
		outFile.delete();
	      } else {
		if( millis > 0 )
		  outFile.setLastModified( millis );
	      }
	    }
	  }
	}
	inZip.closeEntry();
	entry = inZip.getNextEntry();
      }
    }
    catch( IOException ex ) {
      StringBuilder buf = new StringBuilder( 128 );
      buf.append( "\nFehler beim Lesen der Datei " );
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
      if( inZip != null ) {
	close( inZip );
      } else {
	close( in );
      }
    }
    this.fileBrowserFrm.fireRefreshNodeFor(
		dirExists ? this.outDir : this.outDir.getParentFile() );
  }


	/* --- private Konstruktoren und Methoden --- */

  private ZipUnpacker(
		FileBrowserFrm fileBrowserFrm,
		File           srcFile,
		File           outDir )
  {
    super( fileBrowserFrm );
    this.srcFile = srcFile;
    this.outDir  = outDir;
  }
}

