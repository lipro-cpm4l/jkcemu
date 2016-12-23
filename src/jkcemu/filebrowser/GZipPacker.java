/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * GZip-Packer
 */

package jkcemu.filebrowser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.*;
import java.util.zip.GZIPOutputStream;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;


public class GZipPacker extends Thread
{
  private BaseFrm owner;
  private File    srcFile;
  private File    outFile;


  public static void packFile(
			BaseFrm owner,
			File    srcFile,
			File    outFile )
  {
    (new GZipPacker( owner, srcFile, outFile )).start();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    long             srcLen = this.srcFile.length();
    long             millis = this.srcFile.lastModified();
    String           msg    = null;
    InputStream      in     = null;
    GZIPOutputStream out    = null;
    try {
      in = new BufferedInputStream( new FileInputStream( this.srcFile ) );
      if( srcLen > 0 ) {
	ProgressMonitorInputStream pmIn = new ProgressMonitorInputStream(
			this.owner,
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
      FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );

      int b = in.read();
      while( b != -1 ) {
	out.write( b );
	b = in.read();
      }
      out.finish();
      out.close();
      out = null;
      if( millis != -1 ) {
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
      EmuUtil.closeSilent( in );
      EmuUtil.closeSilent( out );
    }
    FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
    if( msg != null ) {
      this.owner.fireShowErrorMsg( msg );
    }
  }


	/* --- private Konstruktoren --- */

  private GZipPacker(
		BaseFrm owner,
		File    srcFile,
		File    outFile )
  {
    super( Main.getThreadGroup(), "JKCEMU gzip packer" );
    this.owner   = owner;
    this.srcFile = srcFile;
    this.outFile = outFile;
  }
}
