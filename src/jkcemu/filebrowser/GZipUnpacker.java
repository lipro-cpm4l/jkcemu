/*
 * (c) 2008-2015 Jens Mueller
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
import jkcemu.Main;
import jkcemu.base.*;


public class GZipUnpacker extends Thread
{
  private BasicFrm owner;
  private File     srcFile;
  private File     outFile;


  public static void unpackFile(
			BasicFrm owner,
               		File     srcFile,
                	File     outFile )
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
      FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );

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
    catch( Exception ex ) {
      this.outFile.delete();
      msg  = ex.getMessage();
    }
    finally {
      EmuUtil.doClose( gzipIn );
      EmuUtil.doClose( in );
      EmuUtil.doClose( out );
    }
    FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
    if( msg != null ) {
      this.owner.fireShowErrorMsg( msg );
    }
  }


	/* --- private Konstruktoren --- */

  private GZipUnpacker(
		BasicFrm owner,
		File     srcFile,
		File     outFile )
  {
    super( Main.getThreadGroup(), "JKCEMU gzip unpacker" );
    this.owner   = owner;
    this.srcFile = srcFile;
    this.outFile = outFile;
  }
}
