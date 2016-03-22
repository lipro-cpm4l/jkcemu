/*
 * (c) 2008-2015 Jens Mueller
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
import jkcemu.Main;
import jkcemu.base.*;


public class GZipPacker extends Thread
{
  private BasicFrm owner;
  private File     srcFile;
  private File     outFile;


  public static void packFile(
		BasicFrm owner,
		File     srcFile,
		File     outFile )
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
    GZIPInputStream  gzipIn = null;
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
      EmuUtil.doClose( in );
      EmuUtil.doClose( out );
    }
    FileBrowserFrm.fireFileChanged( this.outFile.getParentFile() );
    if( msg != null ) {
      this.owner.fireShowErrorMsg( msg );
    }
  }


	/* --- private Konstruktoren --- */

  private GZipPacker(
		BasicFrm owner,
		File     srcFile,
		File     outFile )
  {
    super( Main.getThreadGroup(), "JKCEMU gzip packer" );
    this.owner   = owner;
    this.srcFile = srcFile;
    this.outFile = outFile;
  }
}
