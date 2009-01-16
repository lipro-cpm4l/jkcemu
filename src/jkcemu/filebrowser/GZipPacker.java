/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * GZip-Packer
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import java.util.zip.*;
import javax.swing.*;


public class GZipPacker extends Thread
{
  private FileBrowserFrm fileBrowserFrm;
  private File           srcFile;
  private File           outFile;


  public static void packFile(
		FileBrowserFrm fileBrowserFrm,
		File           srcFile,
		File           outFile )
  {
    (new GZipPacker( fileBrowserFrm, srcFile, outFile )).start();
  }


	/* --- Runnable --- */

  public void run()
  {
    long             srcLen = this.srcFile.length();
    long             millis = this.srcFile.lastModified();
    String           msg    = null;
    InputStream      in     = null;
    GZIPInputStream  gzipIn = null;
    GZIPOutputStream out    = null;
    try {
      in = new BufferedInputStream( new FileInputStream( this.srcFile ) );
      if( srcLen > 0 ) {
	ProgressMonitorInputStream pmIn = new ProgressMonitorInputStream(
			this.fileBrowserFrm,
			"Packen von " + this.srcFile.getName() + "...",
			in );
	ProgressMonitor pm = pmIn.getProgressMonitor();
        if( pm != null ) {
	  pm.setMinimum( 0 );
	  pm.setMaximum( (int) Math.min( srcLen, Integer.MAX_VALUE ) );
	  pm.setMillisToDecideToPopup( 500 );
	  pm.setMillisToPopup( 1000 );
	}
	in = pmIn;
      }
      out = new GZIPOutputStream(
			new BufferedOutputStream(
				new FileOutputStream( outFile ) ) );
      this.fileBrowserFrm.fireRefreshNodeFor( this.outFile.getParentFile() );

      int b = in.read();
      while( b != -1 ) {
	out.write( b );
	b = in.read();
      }
      out.finish();
      out.close();
      out = null;
      if( millis > 0 ) {
	this.outFile.setLastModified( millis );
      }
    }
    catch( InterruptedIOException ex ) {
      this.outFile.delete();
    }
    catch( IOException ex ) {
      this.outFile.delete();
      msg = ex.getMessage();
    }
    finally {
      close( in );
      close( out );
    }
    this.fileBrowserFrm.fireRefreshNodeFor( this.outFile.getParentFile() );
    if( msg != null ) {
      this.fileBrowserFrm.showErrorMsg( msg );
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private GZipPacker(
		FileBrowserFrm fileBrowserFrm,
		File           srcFile,
		File           outFile )
  {
    this.fileBrowserFrm = fileBrowserFrm;
    this.srcFile        = srcFile;
    this.outFile        = outFile;
  }


  private static void close( Closeable io )
  {
    if( io != null ) {
      try {
	io.close();
      }
      catch( IOException ex ) {}
    }
  }
}

