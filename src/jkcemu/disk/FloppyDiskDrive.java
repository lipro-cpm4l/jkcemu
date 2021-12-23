/*
 * (c) 2009-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines Diskettenlaufwerks
 */

package jkcemu.disk;

import java.util.Properties;


public class FloppyDiskDrive
{
  public static final String PROP_SKIP_ODD_CYLS = "skip_odd_cylinders";

  private FloppyDiskStationFrm        owner;
  private volatile AbstractFloppyDisk disk;
  private boolean                     skipOddCyls;
  private int                         lastFormattedCyl;
  private int                         head;
  private int                         pcn;	// present cylinder number
  private int                         ncn;	// new cylinder number


  public FloppyDiskDrive( FloppyDiskStationFrm owner )
  {
    this.owner = owner;
    this.disk  = null;
    reset();
  }


  public boolean formatTrack(
			int        head,
			SectorID[] sectorIDs,
			byte[]     dataBuf )
  {
    boolean            rv   = false;
    AbstractFloppyDisk disk = this.disk;
    fireDriveAccess();
    if( (disk != null) && (sectorIDs != null) ) {
      if( sectorIDs.length > 0 ) {
	if( this.pcn == 0 ) {
	  this.skipOddCyls = false;
	}
	else if( (this.pcn == 2)
		 && (this.lastFormattedCyl == 0)
		 && (disk.getCylinders() < 2) )
	{
	  this.skipOddCyls = true;
	} else if( (this.pcn % 2) != 0 ) {
	  this.skipOddCyls = false;
	}
	if( disk.formatTrack(
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
    this.head = head;
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
    AbstractFloppyDisk disk = this.disk;
    return disk != null ? disk.isReadOnly() : true;
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
    AbstractFloppyDisk disk = this.disk;
    if( (props != null) && (disk != null) ) {
      disk.putSettingsTo( props, prefix );
      props.setProperty(
		prefix + PROP_SKIP_ODD_CYLS,
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
    SectorData         sector = null;
    AbstractFloppyDisk disk   = this.disk;
    fireDriveAccess();
    if( disk != null ) {
      int physCyl = getDiskCyl();
      sector      = disk.getSectorByID(
				physCyl,
				physHead,
				cyl,
				head,
				sectorNum,
				sizeCode );
      if( sector != null ) {
	if( sector.getIndexOnCylinder() < startIdx ) {
	  do {
	    sector = disk.getSectorByIndex(
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
    this.head = head;
    return sector;
  }


  public synchronized SectorData readSectorByIndex(
					int physHead,
					int sectorIdx )
  {
    fireDriveAccess();
    AbstractFloppyDisk disk = this.disk;
    return disk != null ?
		disk.getSectorByIndex( getDiskCyl(), physHead, sectorIdx )
		: null;
  }


  public synchronized void removeDisk()
  {
    AbstractFloppyDisk disk = this.disk;
    if( disk != null ) {
      disk.closeSilently();
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
    fireDriveAccess();
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
    fireDriveAccess();
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
    boolean            rv   = false;
    AbstractFloppyDisk disk = this.disk;
    fireDriveAccess();
    if( disk != null ) {
      rv = disk.writeSector(
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

  private void fireDriveAccess()
  {
    this.owner.fireDriveAccess( this );
  }


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
