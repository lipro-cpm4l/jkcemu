/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines Diskettenlaufwerks
 */

package jkcemu.disk;

import java.lang.*;
import java.util.Properties;


public class FloppyDiskDrive
{
  private AbstractFloppyDisk disk;
  private boolean            skipOddCyls;
  private int                lastFormattedCyl;
  private int                head;
  private int                pcn;	// present cylinder number
  private int                ncn;	// new cylinder number


  protected FloppyDiskDrive()
  {
    this.disk = null;
    reset();
  }


  public boolean formatTrack(
			int        head,
			SectorID[] sectorIDs,
			byte[]     dataBuf )
  {
    boolean rv = false;
    this.head  = head;
    if( (sectorIDs != null) && (this.disk != null) ) {
      if( sectorIDs.length > 0 ) {
	if( this.pcn == 0 ) {
	  this.skipOddCyls = false;
	}
	else if( (this.pcn == 2)
		 && (this.lastFormattedCyl == 0)
		 && (this.disk.getCylinders() < 2) )
	{
	  this.skipOddCyls = true;
	} else if( (this.pcn % 2) != 0 ) {
	  this.skipOddCyls = false;
	}
	if( this.disk.formatTrack(
			getDiskCyl(),
			head,
			sectorIDs,
			dataBuf ) )
	{
	  this.lastFormattedCyl = this.pcn;
	  rv                    = true;
	}
      }
    }
    return rv;
  }


  public synchronized int getCylinder()
  {
    return this.pcn;
  }


  public synchronized int getHead()
  {
    return this.head;
  }


  public synchronized AbstractFloppyDisk getDisk()
  {
    return this.disk;
  }


  public boolean getSkipOddCylinders()
  {
    return this.skipOddCyls;
  }


  public synchronized boolean isReadOnly()
  {
    return this.disk != null ? this.disk.isReadOnly() : true;
  }


  public synchronized boolean isReady()
  {
    return this.disk != null;
  }


  public synchronized boolean isSeekMode()
  {
    return isReady() && (this.pcn != this.ncn);
  }


  public synchronized void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.disk != null) ) {
      disk.putSettingsTo( props, prefix );
      props.setProperty(
		prefix + "skip_odd_cylinders",
		Boolean.toString( this.skipOddCyls ) );
    }
  }


  public synchronized SectorData readSectorByID(
					int physHead,
					int startIdx,
					int cyl,
					int head,
					int sectorNum,
					int sizeCode )
  {
    SectorData sector = null;
    if( this.disk != null ) {
      int physCyl = getDiskCyl();
      sector      = this.disk.getSectorByID(
					physCyl,
					physHead,
					cyl,
					head,
					sectorNum,
					sizeCode );
      if( sector != null ) {
	if( sector.getIndexOnCylinder() < startIdx ) {
	  do {
	    sector = this.disk.getSectorByIndex(
					physCyl,
					physHead,
					startIdx );
	    if( sector == null ) {
	      break;		// keine weiteren Sektoren vorhanden
	    }
	    if( (sector.getCylinder() == cyl)
		&& (sector.getHead() == head)
		&& (sector.getSectorNum() == sectorNum)
		&& (sector.getSizeCode() == sizeCode) )
	    {
	      break;		// Sektor gefunden
	    }
	    startIdx++;
	    sector = null;
	  } while( sector == null );
	}
      }
    }
    return sector;
  }


  public synchronized SectorData readSectorByIndex(
					int physHead,
					int sectorIdx )
  {
    return this.disk != null ?
		this.disk.getSectorByIndex( getDiskCyl(), physHead, sectorIdx )
		: null;
  }


  public synchronized void removeDisk()
  {
    if( this.disk != null ) {
      this.disk.doClose();
      this.disk = null;
    }
  }


  public synchronized void reset()
  {
    this.skipOddCyls      = false;
    this.lastFormattedCyl = 0;
    this.head             = 0;
    this.pcn              = 0;
    this.ncn              = 0;
  }


  /*
   * Diese Methode fuehrt einen Kopfpositionierungsschritt aus
   *
   * Rueckgabewert:
   *  true:  anvisierter Zylinder erreicht
   *  false: anvisierter Zylinder noch nicht erreicht
   */
  public synchronized boolean seekStep()
  {
    if( this.pcn < this.ncn ) {
      this.pcn++;
    } else if( pcn > this.ncn ) {
      --this.pcn;
    }
    return this.pcn == this.ncn;
  }


  public synchronized void setDisk(
				AbstractFloppyDisk disk,
				boolean            skipOddCyls )
  {
    this.disk        = disk;
    this.skipOddCyls = skipOddCyls;
  }


  public synchronized void setSeekMode( int head, int cyl )
  {
    this.head = head;
    this.ncn  = cyl;
  }


  public synchronized boolean writeSector(
				int        physHead,
				SectorData sector,
				byte[]     dataBuf,
				int        dataLen,
				boolean    deleted )
  {
    boolean rv = false;
    if( this.disk != null ) {
      rv = this.disk.writeSector(
				getDiskCyl(),
				physHead,
				sector,
				dataBuf,
				dataLen,
				deleted );
    }
    return rv;
  }


	/* --- private Methoden --- */

  private int getDiskCyl()
  {
    int rv = this.pcn;
    if( this.disk != null ) {
      if( this.skipOddCyls ) {
	rv /= 2;
      }
    }
    return rv;
  }
}

