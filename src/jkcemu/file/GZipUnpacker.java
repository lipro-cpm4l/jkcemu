/*
 * (c) 2008-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * GZip-Entpacker
 */

package jkcemu.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;


public class GZipUnpacker extends Thread
{
  private BaseFrm owner;
  private File    srcFile;
  private File    outFile;


  public static void unpackFile(
			BaseFrm owner,
			File    srcFile,
			File    outFile )
  {
    (new GZipUnpacker( owner, srcFile, outFile )).start();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
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
			this.owner,
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

      int b = gzipIn.read();
      while( b != -1 ) {
	out.write( b );
	b = gzipIn.read();
      }
      out.close();
      out = null;
      if( millis != -1 ) {
	this.outFile.setLastModified( millis );
      }
    }
    catch( InterruptedIOException ex ) {
      this.outFile.delete();
    }
    catch( Exception ex ) {
      this.outFile.delete();
      msg  = ex.getMessage();
    }
    finally {
      EmuUtil.closeSilently( gzipIn );
      EmuUtil.closeSilently( in );
      EmuUtil.closeSilently( out );
    }
    if( msg != null ) {
      EmuUtil.fireShowErrorDlg( this.owner, msg, null );
    }
  }


	/* --- private Konstruktoren --- */

  private GZipUnpacker(
		BaseFrm owner,
		File    srcFile,
		File    outFile )
  {
    super( Main.getThreadGroup(), "JKCEMU gzip unpacker" );
    this.owner   = owner;
    this.srcFile = srcFile;
    this.outFile = outFile;
  }
}
