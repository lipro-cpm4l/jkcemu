/*
 * (c) 2019-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Ausfuehren eines Prozesses und Rueckgabe des Fehlertextes
 */

package jkcemu.base;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import jkcemu.Main;


public class ProcessExecuter
{
  private static class ByteCatcher extends Thread implements Runnable
  {
    private Reader       in;
    private StringBuffer buf;

    private ByteCatcher( StringBuffer buf, InputStream in )
    {
      super(
	Main.getThreadGroup(),
	"JKCEMU ProcessExecuter.ByteCatcher" );
      this.buf = buf;
      this.in  = new InputStreamReader( new BufferedInputStream( in ) );
    }

    @Override
    public void run()
    {
      try {
	int ch = this.in.read();
	while( ch >= 0 ) {
	  this.buf.append( (char) ch );
	  ch = this.in.read();
	}
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilently( this.in );
      }
    }
  };


  public static int execAndWait(
			StringBuffer errTextBuf,
			String...    cmdLine ) throws IOException
  {
    int rv = 0;
    if( cmdLine != null ) {
      if( cmdLine.length > 0 ) {
	String  cmd   = cmdLine[ 0 ];
	boolean found = (new File( cmd )).exists();
	if( !found && (cmd.indexOf( File.separatorChar ) < 0) ) {
	  String path = System.getenv( "PATH" );
	  if( path != null ) {
	    int len = path.length();
	    int idx = 0;
	    while( idx < len ) {
	      String s = null;
	      int    e = path.indexOf( File.pathSeparator, idx );
	      if( e >= idx ) {
		s   = path.substring( idx, e );
		idx = e + 1;
	      } else {
		s   = path.substring( idx );
		idx = len;
	      }
	      int l = s.length();
	      if( l > 0 ) {
		StringBuilder buf = new StringBuilder( 256 );
		buf.append( s );
		if( s.charAt( l - 1 ) != File.separatorChar ) {
		  buf.append( File.separatorChar );
		}
		buf.append( cmd );
		found = (new File( buf.toString() )).exists();
		if( found ) {
		  cmdLine[ 0 ] = buf.toString();
		  break;
		}
	      }
	    }
	  }
	}
	if( !found ) {
	  throw new IOException( cmd + " nicht gefunden" );
	}
	Process     p = Runtime.getRuntime().exec( cmdLine );
	ByteCatcher c = new ByteCatcher( errTextBuf, p.getErrorStream() );
	c.start();
	try {
	  rv = p.waitFor();
	  c.join( 500 );
	  if( !c.getState().equals( Thread.State.TERMINATED ) ) {
	    c.interrupt();
	  }
	}
	catch( InterruptedException ex ) {
	  rv = -1;
	}
      }
    }
    return rv;
  }
}
