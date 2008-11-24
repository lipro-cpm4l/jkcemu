/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateieingabestrom, der einen Fortschrittsbalken bedient
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import javax.swing.*;


public class FileProgressInputStream extends InputStream
{
  private RandomAccessFile in;
  private long             inRead;
  private long             inLen;
  private long             inEnd;
  private long             progressUpdPeriod;
  private long             progressUpdCounter;
  private JProgressBar     progressBar;


  public FileProgressInputStream(
			File         file,
			JProgressBar progressBar,
			int          nReads ) throws IOException
  {
    this.in = new RandomAccessFile( file, "r" );
    try {
      this.inLen = in.length();
    }
    catch( IOException ex ) {
      try {
	this.in.close();
      }
      catch( IOException ex1 ) {}
      throw ex;
    }
    this.inRead             = 0;
    this.inEnd              = this.inLen * nReads;
    this.progressUpdPeriod  = this.inEnd / 100;
    this.progressUpdCounter = 0;
    this.progressBar        = progressBar;
    fireProgressValues( 0, 100, 0 );
  }


  public long length() throws IOException
  {
    return this.inLen;
  }


  public void seek( long pos ) throws IOException
  {
    this.in.seek( pos );
  }


	/* --- ueberschriebene Methoden --- */

  public void close() throws IOException
  {
    this.in.close();
  }


  public int read() throws IOException
  {
    this.inRead++;
    if( this.progressUpdCounter < this.progressUpdPeriod ) {
      this.progressUpdCounter++;
    } else {
      this.progressUpdCounter = 0;
      if( (this.progressUpdPeriod > 0) && (this.inEnd > 0) ) {
	fireProgressValues(
		-1,
		-1,
		(int) Math.round(
			(double) this.inRead / (double) this.inEnd * 100.0 ) );
      }
    }
    return this.in.read();
  }


  public long skip( long nCnt ) throws IOException
  {
    long rv  = nCnt;
    long pos = this.in.getFilePointer();
    if( pos + nCnt > this.inLen ) {
      rv = this.inLen - pos;
    }
    this.in.seek( pos + rv );
    return rv;
  }


	/* --- private Methoden --- */

  private void fireProgressValues(
			final int minValue,
			final int maxValue,
			final int value )
  {
    final JProgressBar progressBar = this.progressBar;
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    if( minValue >= 0 ) {
		      progressBar.setMinimum( minValue );
		    }
		    if( maxValue >= 0 ) {
		      progressBar.setMaximum( maxValue );
		    }
		    if( value >= 0 ) {
		      progressBar.setValue( value );
		    }
		  }
		} );
  }
}

