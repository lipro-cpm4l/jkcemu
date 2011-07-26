/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Information ueber eine Diskette
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;


public class FloppyDiskInfo implements Comparable<FloppyDiskInfo>
{
  private String resource;
  private String infoText;


  public FloppyDiskInfo( String resource, String infoText )
  {
    this.resource = resource;
    this.infoText = infoText;
  }


  public String getResource()
  {
    return this.resource;
  }


  public AbstractFloppyDisk openDisk( Frame owner ) throws IOException
  {
    AbstractFloppyDisk disk = null;
    InputStream        in   = null;
    GZIPInputStream    gz   = null;
    try {
      in = getClass().getResourceAsStream( this.resource );
      if( in == null ) {
	throw new IOException( "Resource " + this.resource
				+ " kann nicht ge\u00F6ffnet werden" );
      }
      if( this.resource.endsWith( ".dump.gz" ) ) {
	gz   = new GZIPInputStream( in );
	disk = AnadiskFloppyDisk.readResourceStream(
						owner,
						gz,
						this.resource );
      }
    }
    finally {
      EmuUtil.doClose( gz );
      EmuUtil.doClose( in );
    }
    if( disk == null ) {
      throw new IOException( "Resource " + this.resource
				+ " kann nicht gelesen werden" );
    }
    return disk;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( FloppyDiskInfo info )
  {
    String s1 = this.infoText;
    String s2 = info.infoText;
    if( s1 == null ) {
      s1 = "";
    }
    return s1.compareTo( s2 != null ? s2 : "" );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.infoText;
  }
}

