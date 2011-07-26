/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Packer
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import jkcemu.base.*;


public class TarPacker extends AbstractThreadDlg
{
  private Collection<File> srcFiles;
  private File             outFile;
  private boolean          compression;


  public static void packFiles(
		Window           owner,
		Collection<File> srcFiles,
		File             outFile,
		boolean          compression )
  {
    Dialog dlg = new TarPacker( owner, srcFiles, outFile, compression );
    if( compression ) {
      dlg.setTitle( "TGZ-Datei packen" );
    } else {
      dlg.setTitle( "TAR-Datei packen" );
    }
    dlg.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void doProgress()
  {
    boolean          failed  = false;
    InputStream      in      = null;
    OutputStream     out     = null;
    GZIPOutputStream gzipOut = null;
    try {
      out = new BufferedOutputStream( new FileOutputStream( this.outFile ) );
      if( this.compression ) {
	gzipOut = new GZIPOutputStream( out );
	out     = gzipOut;
      }
      fireDirectoryChanged( this.outFile.getParentFile() );
      for( File file : this.srcFiles ) {
	packFile( this.outFile, out, file, "" );
      }
      for( int i = 0; i < 1024; i++ ) {
	out.write( 0 );
      }
      if( gzipOut != null ) {
	gzipOut.finish();
      }
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
      failed = true;
      incErrorCount();
    }
    finally {
      EmuUtil.doClose( out );
    }
    if( this.canceled || failed ) {
      this.outFile.delete();
    }
    fireDirectoryChanged( this.outFile.getParentFile() );
  }


	/* --- private Konstruktoren und Methoden --- */

  private TarPacker(
		Window           owner,
		Collection<File> srcFiles,
		File             outFile,
		boolean          compression )
  {
    super( owner, "JKCEMU tar packer", true );
    this.srcFiles    = srcFiles;
    this.outFile     = outFile;
    this.compression = compression;
  }


  private void packFile(
		File         outFile,
		OutputStream out,
		File         file,
		String       path ) throws IOException
  {
    if( this.canceled ) {
      throw new InterruptedIOException();
    }
    if( file != null ) {
      appendToLog( file.getPath() + "\n" );
      String entryName = file.getName();
      if( !file.equals( outFile) && (entryName != null) ) {
	if( !entryName.isEmpty() ) {
	  long millis = file.lastModified();
	  if( file.isDirectory() ) {
	    String subPath = path + entryName + "/";
	    writeTarHeader( out, subPath, true, false, 0, millis );
	    File[] subFiles = file.listFiles();
	    if( subFiles != null ) {
	      for( int i = 0; i < subFiles.length; i++ ) {
		packFile( outFile, out, subFiles[ i ], subPath );
	      }
	    } else {
	      appendErrorToLog(
			"Fehler: Verzeichnis kann nicht gelesen werden.\n" );
	    }
	  }
	  else if( file.isFile() ) {
	    boolean                 exec = file.canExecute();
	    FileProgressInputStream in   = null;
	    try {
	      in       = openInputFile( file, 1 );
	      long len = in.length();
	      writeTarHeader(
			out,
			path + entryName,
			false,
			exec,
			len,
			millis );
	      if( len > 0 ) {
		long pos = 0;
		int  b   = in.read();
		while( !this.canceled && (pos < len) && (b != -1) ) {
		  out.write( b );
		  pos++;
		  b = in.read();
		}
		long nBlocks = len / 512;
		long fullLen = nBlocks * 512;
		if( fullLen < len ) {
		  fullLen += 512;
		}
		while( !this.canceled && (pos < fullLen) ) {
		  out.write( 0 );
		  pos++;
		}
	      }
	    }
	    catch( IOException ex ) {
	      appendErrorToLog( ex );
	      incErrorCount();
	    }
	    finally {
	      EmuUtil.doClose( in );
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


  private void writeASCII( byte[] buf, int pos, String text )
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ )
	buf[ pos++ ] = (byte) (text.charAt( i ) & 0xFF );
    }
  }


  private void writeTarHeader(
			OutputStream out,
			String       entryName,
			boolean      isDir,
			boolean      exec,
			long         fileSize,
			long         fileTime ) throws IOException
  {
    byte[] headerBuf = new byte[ 512 ];
    Arrays.fill( headerBuf, (byte) 0 );

    // 0-100: Name
    int pos = 0;
    int len = entryName.length();
    if( len > 99 ) {
      throw new IOException( "Name des Eintrags zu lang" );
    }
    while( pos < len ) {
      char ch = entryName.charAt( pos );
      if( ch > 0xFF ) {
	throw new IOException( "Zeichen \'" + ch
			+ "\' im Namen eines Eintrags nicht erlaubt" );
      }
      headerBuf[ pos++ ] = (byte) ch;
    }

    // 100-107: Mode
    writeASCII( headerBuf, 100, (isDir || exec) ? "0000755" : "0000644" );

    // 108-115: Benutzer-ID
    writeASCII( headerBuf, 108, "0000000" );

    // 116-123: Gruppe-ID
    writeASCII( headerBuf, 116, "0000000" );

    // 124-135: Laenge
    writeASCII(
	headerBuf,
	124,
	String.format( "%011o", new Long( fileSize ) ) );

    // 136-147: Aenderungszeit
    if( fileTime < 0 ) {
      fileTime = System.currentTimeMillis();
    }
    if( fileTime >= 0 ) {
      writeASCII(
		headerBuf,
		136,
		String.format( "%011o", new Long( fileTime / 1000 ) ) );
    } else {
      writeASCII( headerBuf, 136, "00000000000" );
    }

    // 148-155: Pruefsumme des Kopfblocks
    pos = 148;
    while( pos < 156 ) {
      headerBuf[ pos++ ] = (byte) 0x20;		// wird spaeter ersetzt
    }

    // 156: Typ
    if( isDir ) {
      headerBuf[ 156 ] = '5';
    } else {
      headerBuf[ 156 ] = '0';
    }

    // 157-256: Linkname (hier unbenutzt)

    // 257-262: Magic
    writeASCII( headerBuf, 257, "ustar\u0020" );

    // 263-264: Version
    headerBuf[ 263 ] = '\u0020';

    // 265-296: Benutzername
    writeASCII( headerBuf, 265, "root" );

    // 297-328: Gruppenname
    writeASCII( headerBuf, 297, "root" );

    // 329-512: restliche Felder hier unbenutzt

    // Pruefsumme berechnen und eintragen
    int chkSum = 0;
    for( int i = 0; i < headerBuf.length; i++ ) {
      chkSum += ((int) headerBuf[ i ]) & 0xFF;
    }
    writeASCII(
	headerBuf,
	148,
	String.format( "%06o", new Integer( chkSum ) ) );
    headerBuf[ 154 ] = (byte) 0;

    // Kopfblock schreiben
    out.write( headerBuf );
  }
}

