/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Festplatte
 */

package jkcemu.disk;

import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;


public class HardDiskInfo implements Comparable<HardDiskInfo>
{
  private String producer;
  private String diskModel;
  private String fullInfo;
  private String fullDiskModel;
  private String typeSizeInfo;
  private int    cylinders;
  private int    heads;
  private int    sectorsPerTrack;


  public HardDiskInfo(
		String producer,
		String diskModel,
		int    cylinders,
		int    heads,
		int    sectorsPerTrack ) throws IllegalArgumentException
  {
    this.producer      = EmuUtil.emptyToNull( producer );
    this.diskModel     = EmuUtil.emptyToNull( diskModel );
    this.fullInfo      = null;
    this.fullDiskModel = null;
    this.typeSizeInfo  = null;
    setCylinders( cylinders );
    setHeads( heads );
    setSectorsPerTrack( sectorsPerTrack );
  }


  public int getCylinders()
  {
    return this.cylinders;
  }


  public String getDiskModel()
  {
    return this.diskModel;
  }


  public String getFullDiskModel()
  {
    if( this.fullDiskModel == null ) {
      if( this.producer != null ) {
	if( this.diskModel != null ) {
	  this.fullDiskModel = String.format(
					"%s %s",
					this.producer,
					this.diskModel );
	} else {
	  this.fullDiskModel = String.format(
					"%s %dx%dx%d",
					this.producer,
					this.cylinders,
					this.heads,
					this.sectorsPerTrack );
	}
      } else {
	if( this.diskModel != null ) {
	  this.fullDiskModel = this.diskModel;
	} else {
	  this.fullDiskModel = String.format(
					"%dx%dx%d",
					this.cylinders,
					this.heads,
					this.sectorsPerTrack );
	}
      }
    }
    return this.fullDiskModel;
  }


  public int getHeads()
  {
    return this.heads;
  }


  public String getProducer()
  {
    return this.producer;
  }


  public int getSectorsPerTrack()
  {
    return this.sectorsPerTrack;
  }


  public String getTypeSizeInfo()
  {
    if( this.typeSizeInfo == null ) {
      StringBuilder buf = new StringBuilder( 64 );
      if( this.diskModel != null ) {
	buf.append( this.diskModel );
      }
      appendSizeInfo( buf );
      this.typeSizeInfo = buf.toString();
    }
    return this.typeSizeInfo;
  }


  public void setCylinders( int cyls ) throws IllegalArgumentException
  {
    checkBounds( "Zylinder", cyls, 65536 );
    this.cylinders    = cyls;
    this.fullInfo     = null;
    this.typeSizeInfo = null;
  }


  public void setDiskModel( String diskModel )
  {
    this.diskModel     = diskModel;
    this.fullInfo      = null;
    this.fullDiskModel = null;
    this.typeSizeInfo  = null;
  }


  public void setHeads( int heads ) throws IllegalArgumentException
  {
    checkBounds( "K\u00F6pfe", heads, 16 );
    this.heads        = heads;
    this.fullInfo     = null;
    this.typeSizeInfo = null;
  }


  public void setProducer( String producer )
  {
    this.producer      = producer;
    this.fullInfo      = null;
    this.fullDiskModel = null;
  }


  public void setSectorsPerTrack( int sectorsPerTrack )
					throws IllegalArgumentException
  {
    checkBounds( "Sektoren pro Spur", sectorsPerTrack, 255 );
    this.sectorsPerTrack = sectorsPerTrack;
    this.fullInfo        = null;
    this.typeSizeInfo    = null;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( HardDiskInfo info )
  {
    int rv = EmuUtil.compare( this.producer, info.producer );
    if( rv == 0 ) {
      rv = EmuUtil.compare( this.diskModel, info.diskModel );
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    if( this.fullInfo == null ) {
      StringBuilder buf = new StringBuilder( 128 );
      if( this.producer != null ) {
	buf.append( this.producer );
      }
      if( this.diskModel != null ) {
	if( buf.length() > 0 ) {
	  buf.append( (char) '\u0020' );
	}
	buf.append( this.diskModel );
      }
      appendSizeInfo( buf );
      this.fullInfo = buf.toString();
    }
    return this.fullInfo;
  }


	/* --- private Methoden --- */

  private void appendSizeInfo( StringBuilder buf )
  {
    if( buf.length() > 0 ) {
      buf.append( " / " );
    }
    EmuUtil.appendSizeText(
		buf,
		(long) this.cylinders
			* (long) this.heads
			* (long) this.sectorsPerTrack
			* 512L,
		false,
		false );
    buf.append( " (" );
    buf.append( this.cylinders );
    buf.append( (char) 'x' );
    buf.append( this.heads );
    buf.append( (char) 'x' );
    buf.append( this.sectorsPerTrack );
    buf.append( (char) ')' );
  }


  private void checkBounds(
			String fldName,
			int    value,
			int    maxValue ) throws IllegalArgumentException
  {
    if( value < 1 ) {
      throw new IllegalArgumentException(
			fldName + ": Wert zu klein (mindestens 1)" );
    }
    if( value > maxValue ) {
      throw new IllegalArgumentException(
			String.format(
				"%s: Wert zu gro\u00DF (maximal %d)",
				fldName,
				maxValue ) );
    }
  }
}

