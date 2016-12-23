/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Information ueber eine Diskette
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;


public class FloppyDiskInfo implements Comparable<FloppyDiskInfo>
{
  private String  resource;
  private String  infoText;
  private int     sysTracks;
  private int     blockSize;
  private boolean blockNum16Bit;


  public FloppyDiskInfo(
		String  resource,
		String  infoText,
		int     sysTracks,
		int     blockSize,
		boolean blockNum16Bit )
  {
    this.resource      = resource;
    this.infoText      = infoText;
    this.sysTracks     = sysTracks;
    this.blockSize     = blockSize;
    this.blockNum16Bit = blockNum16Bit;
  }


  public boolean getBlockNum16Bit()
  {
    return this.blockNum16Bit;
  }


  public int getBlockSize()
  {
    return this.blockSize;
  }


  public String getResource()
  {
    return this.resource;
  }


  public int getSysTracks()
  {
    return this.sysTracks;
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
	disk = AnaDisk.readResourceStream( owner, gz, this.resource );
      }
    }
    finally {
      EmuUtil.closeSilent( gz );
      EmuUtil.closeSilent( in );
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

