/*
 * (c) 2019-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Herunterladen einer Datei
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.zip.GZIPInputStream;
import javax.swing.ProgressMonitorInputStream;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.etc.ReadableByteArrayOutputStream;


public class Downloader extends Thread
{
  private Component           owner;
  private URL                 url;
  private int                 maxFileLen;
  private boolean             unpackGZip;
  private String              fileName;
  private Downloader.Consumer consumer;


  public interface Consumer
  {
    public void consume( byte[] fileBytes, String fileName );
  };


  public static boolean checkAndStart(
				Component           owner,
				File                file,
				int                 maxFileLen,
				boolean             unpackGZip,
				DropTargetDropEvent e,
				Downloader.Consumer consumer )
  {
    boolean done   = false;
    boolean delete = false;
    if( e != null ) {
      int dndAction = e.getDropAction();
      if( ((dndAction == DnDConstants.ACTION_COPY_OR_MOVE)
		|| (dndAction == DnDConstants.ACTION_MOVE))
	  && ((file.lastModified() + 1000L) > System.currentTimeMillis()) )
      {
	/*
	 * Wenn die Dop-Action das Loeschen der Quelle erlaubt und
	 * die URL-Datei soeben angelegt wurde, dann diese wieder loeschen
	 */
	delete = true;
      }
    }
    URL url = null;
    try {
      url = FileUtil.readInternetShortcutURL( file );
    }
    catch( IOException ex ) {}
    if( url != null ) {
      String fileName = url.getFile();
      if( fileName != null ) {
	try {
	  fileName = URLDecoder.decode( fileName, "UTF-8" );
	}
	catch( UnsupportedEncodingException ex ) {}
	int idx  = fileName.lastIndexOf( '/' );
	if( idx >= 0 ) {
	  fileName = fileName.substring( idx + 1 );
	}
	if( fileName.isEmpty() ) {
	  fileName = null;
	}
      }
      (new Downloader(
		owner,
		url,
		maxFileLen,
		unpackGZip,
		fileName,
		consumer )).start();
      if( delete ) {
	file.delete();
      }
      done = true;
    }
    return done;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    byte[]        fileBytes = null;
    IOException   fileEx    = null;
    URLConnection urlCon    = null;
    InputStream   in        = null;
    try {
      urlCon = this.url.openConnection();
      urlCon.setReadTimeout( 20000 );
      long contentLen = urlCon.getContentLengthLong();
      if( contentLen > (long) Integer.MAX_VALUE ) {
	throwFileTooBig();
      }
      if( (this.maxFileLen > 0)
	  && (contentLen > 0)
	  && (contentLen > this.maxFileLen) )
      {
	throwFileTooBig();
      }
      String info = this.fileName;
      if( info == null ) {
	info = this.url.toExternalForm();
      }
      in = new BufferedInputStream(
			new ProgressMonitorInputStream(
				this.owner,
				"Download " + info,
				urlCon.getInputStream() ) );
      ReadableByteArrayOutputStream buf = null;
      if( contentLen > 0 ) {
	buf = new ReadableByteArrayOutputStream( (int) contentLen );
      } else {
	buf = new ReadableByteArrayOutputStream( 0x4000 );
      }
      int n = 0;
      int b = in.read();
      while( b >= 0 ) {
	if( (maxFileLen > 0) && (n >= maxFileLen) ) {
	  throwFileTooBig();
	}
	buf.write( b );
	n++;
	b = in.read();
      }
      in.close();
      in = null;
      if( urlCon instanceof HttpURLConnection ) {
	((HttpURLConnection) urlCon).disconnect();
      }
      if( buf.size() > 0 ) {
	if( this.unpackGZip && (this.fileName != null) ) {
	  if( this.fileName.toLowerCase().endsWith( ".gz" ) ) {
	    InputStream           inRaw   = null;
	    GZIPInputStream       inGZip  = null;
	    ByteArrayOutputStream bufGZip = null;
	    try {
	      inRaw   = buf.newInputStream();
	      inGZip  = new GZIPInputStream( inRaw );
	      bufGZip = new ByteArrayOutputStream( 0x8000 );
	      n       = 0;
	      b       = inGZip.read();
	      while( b >= 0 ) {
		if( (maxFileLen > 0) && (n >= maxFileLen) ) {
		  throwFileTooBig();
		}
		bufGZip.write( b );
		n++;
		b = inGZip.read();
	      }
	      inGZip.close();
	      inGZip = null;
	      if( bufGZip.size() > 0 ) {
		fileBytes = bufGZip.toByteArray();
	      }
	    }
	    catch( IOException ex ) {}
	    finally {
	      EmuUtil.closeSilently( inGZip );
	      EmuUtil.closeSilently( inRaw );
	      EmuUtil.closeSilently( bufGZip );
	    }
	  }
	}
	if( fileBytes == null ) {
	  fileBytes = buf.toByteArray();
	}
      }
    }
    catch( InterruptedIOException ex ) {}
    catch( IOException ex ) {
      fileEx = ex;
    }
    if( (fileBytes != null) || (fileEx != null) ) {
      final byte[]      fileBytes1 = fileBytes;
      final IOException fileEx1    = fileEx; 
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    downloadFinished( fileBytes1, fileEx1 );
			  }
			} );
    }
  }


	/* --- Konstruktor --- */

  private Downloader(
		Component           owner,
		URL                 url,
		int                 maxFileLen,
		boolean             unpackGZip,
		String              fileName,
		Downloader.Consumer consumer )
  {
    super( Main.getThreadGroup(), Main.APPNAME + " Downloader" );
    this.owner      = owner;
    this.url        = url;
    this.maxFileLen = maxFileLen;
    this.unpackGZip = unpackGZip;
    this.fileName   = fileName;
    this.consumer   = consumer;
  }


	/* --- private Methoden --- */

  private void downloadFinished( byte[] fileBytes, IOException fileEx )
  {
    if( fileEx != null ) {
      BaseDlg.showErrorDlg( this.owner, fileEx );
    }
    else if( fileBytes != null ) {
      this.consumer.consume( fileBytes, this.fileName );
    }
  }


  private static void throwFileTooBig() throws IOException
  {
    throw new IOException( "Datei zu gro\u00DF!" );
  }
}
