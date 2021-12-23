/*
 * (c) 2008-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateieingabestrom, der einen Fortschrittsbalken bedient
 */

package jkcemu.file;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Checksum;
import javax.swing.JProgressBar;
import jkcemu.base.EmuUtil;


public class FileProgressInputStream extends InputStream
{
  private static final int DEFAULT_BUFFER_SIZE = 0x4000;


  private BufferedInputStream in;
  private long                fileSize;
  private long                readPos;
  private long                readEnd;
  private int                 lastProgressValue;
  private int                 progressMinValue;
  private int                 progressMaxValue;
  private long                progressUpdPeriod;
  private long                progressUpdCounter;
  private float               progressRange;
  private JProgressBar        progressBar;


  public FileProgressInputStream(
			File         file,
			JProgressBar progressBar,
			Checksum     cks ) throws IOException
  {
    this.progressBar      = progressBar;
    this.progressMinValue = progressBar.getMinimum();
    this.progressMaxValue = progressBar.getMaximum();
    fireProgressValue( this.progressMinValue );

    this.fileSize = file.length();
    if( this.fileSize < 0 ) {
      this.fileSize = 0;
    }
    this.readPos = 0;
    this.readEnd = this.fileSize;

    int bufSize = DEFAULT_BUFFER_SIZE;
    if( this.fileSize < bufSize ) {
      bufSize = (int) (this.fileSize > 0 ? this.fileSize : 1);
    }
    this.in = new BufferedInputStream(
				new FileInputStream( file ),
				bufSize );
    if( cks != null ) {
      this.readEnd *= 2;
      initProgressBar();
      try {
	int b = read();
	while( b >= 0 ) {
	  cks.update( b );
	  b = read();
	}
      }
      finally {
	EmuUtil.closeSilently( this.in );
      }
      this.in = new BufferedInputStream(
				new FileInputStream( file ),
				bufSize );
    } else {
      initProgressBar();
    }
  }


  public long getFileSize()
  {
    return this.fileSize;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void close() throws IOException
  {
    if( this.readPos == this.readEnd ) {
      fireProgressValue( this.progressMaxValue );
    }
    this.in.close();
  }


  @Override
  public int read() throws IOException
  {
    int b = this.in.read();
    if( b >= 0 ) {
      progressUpd( 1 );
    }
    return b;
  }


  @Override
  public int read( byte[] buf, int offs, int len ) throws IOException
  {
    int rv = this.in.read( buf, offs, len );
    if( rv > 0 ) {
      progressUpd( rv );
    }
    return rv;
  }


  @Override
  public long skip( long n ) throws IOException
  {
    long rv = this.in.skip( n );
    if( rv > 0 ) {
      progressUpd( rv );
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void fireProgressValue( final int value )
  {
    final JProgressBar progressBar = this.progressBar;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    progressBar.setValue( value );
		  }
		} );
  }


  private void initProgressBar()
  {
    int range = this.progressMaxValue - this.progressMinValue;
    if( range > 0 ) {
      this.progressUpdPeriod = this.readEnd / (long) range;
    } else {
      this.progressUpdPeriod = 0;
    }
    this.progressRange      = (float) range;
    this.progressUpdCounter = 0;
  }


  private void progressUpd( long diffBytes )
  {
    this.readPos            += diffBytes;
    this.progressUpdCounter += diffBytes;
    if( (this.progressUpdCounter >= this.progressUpdPeriod)
	&& (this.readEnd > 0) )
    {
      int value = this.progressMinValue
			+ Math.round( (float) this.readPos
						/ (float) this.readEnd
						* this.progressRange );
      if( value > this.progressMaxValue ) {
	value = this.progressMaxValue;
      }
      if( value != this.lastProgressValue ) {
	this.lastProgressValue = value;
	fireProgressValue( value );
      }
      this.progressUpdCounter = 0;
    }
  }
}
