/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse zur Emulation einer Diskette
 */

package jkcemu.disk;

import java.awt.EventQueue;
import java.awt.Frame;
import java.io.IOException;
import java.lang.*;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.BaseDlg;


public abstract class AbstractFloppyDisk
{
  public static final String PROP_CYLINDERS       = "cylinders";
  public static final String PROP_FILE            = "file";
  public static final String PROP_READONLY        = "readonly";
  public static final String PROP_RESOURCE        = "resource";
  public static final String PROP_SECTORS_PER_CYL = "sectors_per_cylinder";
  public static final String PROP_SECTORSIZE      = "sectorsize";
  public static final String PROP_SIDES           = "sides";

  private Frame           owner;
  private volatile int    sides;
  private volatile int    cyls;
  private volatile int    sectorsPerCyl;
  private volatile int    sectorSize;
  private volatile String fmtText;
  private String          mediaText;
  private String          warningText;
  private int[]           sectorIdxMap;


  protected AbstractFloppyDisk(
			Frame owner,
			int   sides,
			int   cyls,
			int   sectorsPerCyl,
			int   sectorSize,
			int   interleave )
  {
    this.owner         = owner;
    this.sides         = sides;
    this.cyls          = cyls;
    this.sectorsPerCyl = sectorsPerCyl;
    this.sectorSize    = sectorSize;
    this.fmtText       = null;
    this.mediaText     = null;
    this.warningText   = null;
    this.sectorIdxMap  = null;
    if( (interleave > 1)
	&& (interleave < sectorsPerCyl)
	&& (sectorsPerCyl > 2) )
    {
      this.sectorIdxMap = new int[ sectorsPerCyl ];
      Arrays.fill( this.sectorIdxMap, -1 );
      int srcIdx = 0;
      int dstIdx = 0;
      while( srcIdx < sectorsPerCyl ) {
	while( this.sectorIdxMap[ dstIdx ] >= 0 ) {
	  dstIdx = (dstIdx + 1) % sectorsPerCyl;
	}
	this.sectorIdxMap[ dstIdx ] = srcIdx++;
	dstIdx = (dstIdx + interleave) % sectorsPerCyl;
      }
    }
  }


  protected AbstractFloppyDisk(
			Frame owner,
			int   sides,
			int   cyls,
			int   sectorsPerCyl,
			int   sectorSize )
  {
    this( owner, sides, cyls, sectorsPerCyl, sectorSize, 0 );
  }


  public void closeSilent()
  {
    // leer
  }


  protected void fireShowError( final String msg, final Exception ex )
  {
    if( this.owner instanceof FloppyDiskStationFrm ) {
      ((FloppyDiskStationFrm) this.owner).fireShowDiskError( this, msg, ex );
    } else {
      final Frame owner = this.owner;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    BaseDlg.showErrorDlg( owner, msg, ex );
		  }
		} );
    }
  }


  protected void fireShowReadError(
				int       cyl,
				int       head,
				int       sectorNum,
				Exception ex )
  {
    fireShowError(
	String.format(
		"Sektor [C=%d,H=%d,R=%d] kann nicht gelesen werden",
		cyl,
		head,
		sectorNum ),
	ex );
  }


  protected void fireShowWriteError(
				int       cyl,
				int       head,
				int       sectorNum,
				Exception ex )
  {
    fireShowError(
	String.format(
		"Sektor [C=%d,H=%d,R=%d] kann nicht geschrieben werden",
		cyl,
		head,
		sectorNum ),
	ex );
  }


  /*
   * Diese Methode dient zum Formatieren einer Spur
   * und liefert im Erfolgsfall true zurueck.
   */
  public boolean formatTrack(
			int        physCyl,
			int        physHead,
			SectorID[] sectorIDs,
			byte[]     dataBuf )
  {
    boolean rv = false;
    if( !isReadOnly() && (sectorIDs != null) ) {
      if( sectorIDs.length > 0 ) {
	rv = true;
	for( int i = 0; i < sectorIDs.length; i++ ) {
	  boolean    state  = false;
	  SectorID sectorID = sectorIDs[ i ];
	  if( sectorID != null ) {
	    SectorData sector = getSectorByID(
					physCyl,
					physHead,
					sectorID.getCylinder(),
					sectorID.getHead(),
					sectorID.getSectorNum(),
					sectorID.getSizeCode() );
	    if( sector != null ) {
	      state = writeSector(
				physCyl,
				physHead,
				sector,
				dataBuf,
				dataBuf.length,
				false );
	    }
	  }
	  if( !state ) {
	    rv = false;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public int getCylinders()
  {
    return this.cyls;
  }


  public java.util.Date getDiskDate()
  {
    return null;
  }


  public int getDiskSize()
  {
    return this.sides * this.cyls * this.sectorsPerCyl * this.sectorSize;
  }


  public abstract String getFileFormatText();


  public String getFormatText()
  {
    if( this.fmtText == null ) {
      StringBuilder buf = new StringBuilder( 128 );
      if( (this.sides > 0)
	  && (this.cyls > 0)
	  && (this.sectorsPerCyl > 0)
	  && (this.sectorSize > 0) )
      {
	int kBytes  = this.sides * this.cyls * this.sectorsPerCyl
						* this.sectorSize / 1024;
	int sysTracks = getSysTracks();
	if( (sysTracks > 0) && (sysTracks < this.cyls) ) {
	  buf.append( this.sides * (this.cyls - sysTracks)
			* this.sectorsPerCyl * this.sectorSize / 1024 );
	  buf.append( (char) '/' );
	  buf.append( kBytes );
	  buf.append( " KByte, " );
	} else {
	  buf.append( kBytes );
	  buf.append( " KByte brutto, " );
	}
	buf.append( this.cyls );
	buf.append( " Spuren a " );
	buf.append( this.sectorsPerCyl );
	buf.append( " * " );
	buf.append( this.sectorSize );
	buf.append( " Bytes" );
	switch( sides ) {
	  case 1:
	    buf.append( ", einseitig" );
	    break;
	  case 2:
	    buf.append( ", doppelseitig" );
	    break;
	}
      } else {
	buf.append( "unformatiert" );
      }
      if( isReadOnly() ) {
	buf.append( ", schreibgesch\u00FCtzt" );
      }
      this.fmtText = buf.toString();
    }
    return this.fmtText;
  }


  public String getMediaText()
  {
    return this.mediaText;
  }


  public String getRemark()
  {
    return null;
  }


  public int getSectorOffset() throws IOException
  {
    int sectorOffs = -1;
    int cyls       = getCylinders();
    int sides      = getSides();
    for( int cyl = 0; cyl < cyls; cyl++ ) {
      for( int head = 0; head < sides; head++ ) {
	int minSectorNum = -1;
	int cylSectors   = getSectorsOfCylinder( cyl, head );
	for( int i = 0; i < cylSectors; i++ ) {
	  SectorData sector = getSectorByIndex( cyl, head, i );
	  if( sector != null ) {
	    int sectorNum = sector.getSectorNum();
	    if( (minSectorNum < 0) || (sectorNum < minSectorNum) ) {
	      minSectorNum = sectorNum;
	      if( minSectorNum <= 1 ) {
		break;
	      }
	    }
	  }
	}
	int trackSectorOffs = 1 - minSectorNum;
	if( (minSectorNum < 1)
	    || ((sectorOffs >= 0) && (sectorOffs != trackSectorOffs)) )
	{
	  throw new IOException(
		"Diskette hat unregelm\u00E4\u00DFige Sektornummern" );
	}
	sectorOffs = trackSectorOffs;
      }
    }
    return sectorOffs;
  }


  public int getSectorSize()
  {
    return this.sectorSize;
  }


  public int getSectorsPerCylinder()
  {
    return this.sectorsPerCyl;
  }


  /*
   * Diese Methode liefert einen Sektor anhand
   * seiner physischen Position auf der Spur.
   */
  public abstract SectorData getSectorByIndex(
					int physCyl,
					int physHead,
					int sectorIdx );


  /*
   * Diese Methode liefert einen Sektor anhand seiner Sektor-ID.
   *
   * Anstelle von sizeCode kann auch -1 uebergeben werden,
   * d.h. sizeCode wird dann nicht verglichen.
   *
   * Die Standard-Implementierung sucht den Sektor zuerst auf seiner
   * wahrscheinlichsten Position.
   * Ist er dort nicht zu finden,
   * werden alle Sektoren der Spur in die Suche einbezogen.
   */
  public SectorData getSectorByID(
				int physCyl,
				int physHead,
				int cyl,
				int head,
				int sectorNum,
				int sizeCode )
  {
    int        idx = sectorNum - 1;
    SectorData rv  = getSectorByIndex( physCyl, physHead, idx );
    if( rv != null ) {
      if( (rv.getCylinder() != cyl)
	  || (rv.getHead() != head)
	  || (rv.getSectorNum() != sectorNum)
	  || ((sizeCode >= 0) && (rv.getSizeCode() != sizeCode)) )
      {
	rv = null;
      }
    }
    if( rv == null ) {
      int n = getSectorsOfCylinder( physCyl, physHead );
      for( int i = 0; i < n; i++ ) {
	if( i != idx ) {
	  SectorData sector = getSectorByIndex( physCyl, physHead, i );
	  if( sector != null ) {
	    if( (sector.getCylinder() == cyl)
		&& (sector.getHead() == head)
		&& (sector.getSectorNum() == sectorNum)
		&& ((sizeCode < 0) || (sector.getSizeCode() == sizeCode)) )
	    {
	      rv = sector;
	      break;
	    }
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode liefert die Anzahl der Sektoren
   * auf einer gegebenen Spur und einer gegebenen Seite.
   * Die Standard-Implementierung liefert nur dann den richtigen Wert,
   * wenn auf allen Spuren immer die gleiche Anzahl von Sektoren zu finden ist.
   * Abgeleitete Klassen, die ein von dieser Einschraenkung abweichendes
   * Format repraesentieren, muessen die Methode ueberschreiben.
   */
  public int getSectorsOfCylinder( int physCyl, int physHead )
  {
    int rv = 0;
    int sides = getSides();
    if( (physHead < sides) && (sides >= 1) ) {
      if( (physCyl >= 0) && (physCyl < getCylinders()) ) {
	rv = getSectorsPerCylinder();
      }
    }
    return rv;
  }


  public int getSides()
  {
    return sides;
  }


  protected int getSysTracks()
  {
    return 0;
  }


  public String getWarningText()
  {
    return this.warningText;
  }


  public static boolean isDiskFileHeader( byte[] header )
  {
    return CopyQMDisk.isCopyQMFileHeader( header )
		|| CPCDisk.isCPCDiskFileHeader( header )
		|| ImageDisk.isImageDiskFileHeader( header )
		|| TeleDisk.isTeleDiskFileHeader( header );
  }


  public boolean isReadOnly()
  {
    return true;
  }


  public void putSettingsTo( Properties props, String prefix )
  {
    if( props != null ) {
      props.setProperty(
		prefix + PROP_READONLY,
		Boolean.toString( isReadOnly() ) );
      props.setProperty(
		prefix + PROP_SIDES,
		Integer.toString( getSides() ) );
      props.setProperty(
		prefix + PROP_CYLINDERS,
		Integer.toString( getCylinders() ) );
      props.setProperty(
		prefix + PROP_SECTORS_PER_CYL,
		Integer.toString( getSectorsPerCylinder() ) );
      props.setProperty(
		prefix + PROP_SECTORSIZE,
		Integer.toString( getSectorSize() ) );
    }
  }


  protected int sectorIndexToInterleave( int sectorIdx )
  {
    if( this.sectorIdxMap != null ) {
      if( (sectorIdx >= 0) && (sectorIdx < this.sectorIdxMap.length) ) {
	sectorIdx = this.sectorIdxMap[ sectorIdx ];
      }
    }
    return sectorIdx;
  }


  public void setMediaText( String text )
  {
    this.mediaText = text;
  }


  public void setOwner( Frame owner )
  {
    this.owner = owner;
  }


  public void setCylinders( int cyls )
  {
    if( cyls != this.cyls ) {
      this.cyls = cyls;
      fireDiskFmtChanged();
    }
  }


  public void setSectorSize( int sectorSize )
  {
    if( sectorSize != this.sectorSize ) {
      this.sectorSize = sectorSize;
      fireDiskFmtChanged();
    }
  }


  public void setSectorsPerCylinder( int sectorsPerCyl )
  {
    if( sectorsPerCyl != this.sectorsPerCyl ) {
      this.sectorsPerCyl = sectorsPerCyl;
      fireDiskFmtChanged();
    }
  }


  public void setSides( int sides )
  {
    if( sides != this.sides ) {
      this.sides = sides;
      fireDiskFmtChanged();
    }
  }


  public void setWarningText( String text )
  {
    this.warningText = text;
  }


  public boolean supportsDeletedSectors()
  {
    return false;
  }


  protected static void throwSectorSpaceTooSmall(
				int cyl,
				int head,
				int sectorNum ) throws IOException
  {
    throw new IOException(
	String.format(
		"Seite %d, Spur %d, Sektor %d: Datenfeld zu klein,\n"
			+ "Der Sektor kann nicht geschrieben werden,\n"
			+ "weil das Datenfeld des Sektors kleiner ist,\n"
			+ "als es laut Diskettenformat sein m\u00FCsste.",
		head - 1,
		cyl,
		sectorNum ) );
  }


  protected static void throwUnexpectedEOF() throws IOException
  {
    throw new IOException( "Unerwartetes Dateiende" );
  }


  /*
   * Diese Methode dient zum Schreiben eines Sektors
   * und liefert im Erfolgsfall true zurueck.
   * Die Sektordaten stehen in dem Byte-Array.
   * Konnte der Sektor geschrieben werden,
   * muessen die Daten auch in das Sektorobjekt kopiert werden.
   */
  public boolean writeSector(
			int        physCyl,
			int        physHead,
			SectorData sector,
			byte[]     dataBuf,
			int        dataLen,
			boolean    deleted )
  {
    return false;
  }


	/* --- private Methoden --- */

  private static String createSectorKey(
				int physCyl,
				int physHead,
				int sectorIdx )
  {
    return String.format(
			"%d:%d:%d",
			physCyl,
			physHead,
			sectorIdx );
  }


  private void fireDiskFmtChanged()
  {
    this.fmtText = null;
    if( this.owner instanceof FloppyDiskStationFrm ) {
      ((FloppyDiskStationFrm) owner).fireDiskFormatChanged( this );
    }
  }
}
