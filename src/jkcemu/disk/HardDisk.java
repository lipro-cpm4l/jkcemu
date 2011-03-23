/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Festplatte
 */

package jkcemu.disk;

import java.io.File;
import jkcemu.base.*;


public class HardDisk
{
  private String diskModel;
  private File   file;
  private int    cylinders;
  private int    heads;
  private int    sectorsPerTrack;


  public HardDisk(
		String diskModel,
		int    cylinders,
		int    heads,
		int    sectorsPerTrack,
		String fileName )
  {
    this.diskModel       = diskModel;
    this.file            = new File( fileName );
    this.cylinders       = cylinders;
    this.heads           = heads;
    this.sectorsPerTrack = sectorsPerTrack;
  }


  public int getCylinders()
  {
    return this.cylinders;
  }


  public String getDiskModel()
  {
    return this.diskModel;
  }


  public File getFile()
  {
    return this.file;
  }


  public int getHeads()
  {
    return this.heads;
  }


  public int getSectorsPerTrack()
  {
    return this.sectorsPerTrack;
  }


  public boolean isSameDisk( HardDisk disk )
  {
    boolean rv = false;
    if( disk != null ) {
      if( disk.file.equals( this.file )
	  && (disk.cylinders == this.cylinders)
	  && (disk.heads == this.heads)
	  && (disk.sectorsPerTrack == this.sectorsPerTrack) )
      {
	rv = true;
      }
    }
    return rv;
  }
}

