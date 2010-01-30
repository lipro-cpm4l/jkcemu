/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * GZip-Entpacker
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import java.util.zip.*;
import javax.swing.*;
import jkcemu.base.EmuUtil;


public class GZipUnpacker extends Thread
{
  private FileBrowserFrm fileBrowserFrm;
  private File           srcFile;
  private File           outFile;


  public static void unpackFile(
                FileBrowserFrm fileBrowserFrm,
                File           srcFile,
                File           outFile )
  {
    (new GZipUnpacker( fileBrowserFrm, srcFile, outFile )).start();
  }


	/* --- Runnable --- */

  public void run()
  {
    long            srcLen = this.srcFile.length();
    long            millis = this.srcFile.lastModified();
    String          msg    = null;
    InputStream     in     = null;
    GZIPInputStream gzipIn = null;
    OutputStream    out    = null;
    try {
      in = new BufferedInputStream( new FileInputStream( this.srcFile ) );
      if( srcLen > 0 ) {
	ProgressMonitorInputStream pmIn = new ProgressMonitorInputStream(
			this.fileBrowserFrm,
			"Entpacken von " + this.srcFile.getName() + "...",
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
      gzipIn = new GZIPInputStream( in );
      out    = new BufferedOutputStream(
			new FileOutputStream( this.outFile ) );
      this.fileBrowserFrm.fireDirectoryChanged( this.outFile.getParentFile() );

      int b = gzipIn.read();
      while( b != -1 ) {
	out.write( b );
	b = gzipIn.read();
      }
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
      msg  = ex.getMessage();
    }
    finally {
      EmuUtil.doClose( gzipIn );
      EmuUtil.doClose( in );
      EmuUtil.doClose( out );
    }
    this.fileBrowserFrm.fireDirectoryChanged( this.outFile.getParentFile() );
    if( msg != null ) {
      this.fileBrowserFrm.showErrorMsg( msg );
    }
  }


	/* --- private Konstruktoren --- */

  private GZipUnpacker(
		FileBrowserFrm fileBrowserFrm,
		File           srcFile,
		File           outFile )
  {
    this.fileBrowserFrm = fileBrowserFrm;
    this.srcFile        = srcFile;
    this.outFile        = outFile;
  }
}

