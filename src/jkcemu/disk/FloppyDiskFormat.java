/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Format einer Diskette
 */

package jkcemu.disk;

import java.lang.*;


public class FloppyDiskFormat
{
  private static final FloppyDiskFormat[] formats = {
			new FloppyDiskFormat( 2, 80,  9,  512 ),
			new FloppyDiskFormat( 2, 80,  5, 1024 ),
			new FloppyDiskFormat( 2, 80, 15,  512 ),
			new FloppyDiskFormat( 2, 80, 18,  512 ),
			new FloppyDiskFormat( 2, 80, 36,  512 ),
			new FloppyDiskFormat( 2, 80, 16,  256 ),
			new FloppyDiskFormat( 1, 80,  5, 1024 ),
			new FloppyDiskFormat( 1, 80, 16,  256 ),
			new FloppyDiskFormat( 1, 40, 16,  256 ),
			new FloppyDiskFormat( 2, 40, 16,  256 ),
			new FloppyDiskFormat( 1, 40,  8,  512 ),
			new FloppyDiskFormat( 1, 40,  9,  512 ),
			new FloppyDiskFormat( 2, 40,  8,  512 ),
			new FloppyDiskFormat( 2, 40,  9,  512 ),
			new FloppyDiskFormat( 1, 40,  5, 1024 ),
			new FloppyDiskFormat( 2, 40,  5, 1024 ) };

  private static Integer maxDiskSize = null;

  private int    sides;
  private int    cyls;
  private int    sectorsPerCyl;
  private int    sectorSize;
  private int    diskSize;
  private String infoText;


  public FloppyDiskFormat(
		int sides,
		int cyls,
		int sectorsPerCyl,
		int sectorSize )
  {
    this.sides         = sides;
    this.cyls          = cyls;
    this.sectorsPerCyl = sectorsPerCyl;
    this.sectorSize    = sectorSize;
    this.diskSize      = sides * cyls * sectorsPerCyl * sectorSize;

    StringBuilder buf = new StringBuilder( 128 );
    buf.append( diskSize / 1024 );
    buf.append( " KByte, " );
    buf.append( cyls );
    buf.append( " Spuren a " );
    buf.append( sectorsPerCyl );
    buf.append( " * " );
    buf.append( sectorSize );
    buf.append( " Bytes" );
    switch( sides ) {
      case 1:
	buf.append( ", einseitig" );
	break;
      case 2:
	buf.append( ", doppelseitig" );
	break;
    }
    this.infoText = buf.toString();
  }


  public int getCylinders()
  {
    return this.cyls;
  }


  public int getDiskSize()
  {
    return this.diskSize;
  }


  public static FloppyDiskFormat getFormat(
					int sides,
					int cyls,
					int sectorsPerCyl,
					int sectorSize )
  {
    FloppyDiskFormat rv = null;
    for( int i= 0; i < formats.length; i++ ) {
      FloppyDiskFormat fmt = formats[ i ];
      if( (fmt.getSides() == sides)
	  && (fmt.getCylinders() == cyls)
	  && (fmt.getSectorsPerCylinder() == sectorsPerCyl)
	  && (fmt.getSectorSize() == sectorSize) )
      {
	rv = fmt;
	break;
      }
    }
    return rv;
  }


  public static FloppyDiskFormat getFormatByDiskSize( long diskSize )
  {
    FloppyDiskFormat rv = null;
    if( diskSize > 0 ) {
      for( int i= 0; i < formats.length; i++ ) {
	if( formats[ i ].getDiskSize() == diskSize ) {
	  if( rv != null ) {
	    rv = null;
	    break;		// Format nicht eindeutig
	  }
	  rv = formats[ i ];
	}
      }
    }
    return rv;
  }


  public static FloppyDiskFormat[] getFormats()
  {
    return formats;
  }


  public static int getMaxDiskSize()
  {
    if( maxDiskSize == null ) {
      int m = 0;
      for( int i = 0; i < formats.length; i++ ) {
	int diskSize = formats[ i ].getDiskSize();
	if( diskSize > m ) {
	  m = diskSize;
	}
      }
      maxDiskSize = new Integer( m );
    }
    return maxDiskSize.intValue();
  }


  public int getSectorsPerCylinder()
  {
    return this.sectorsPerCyl;
  }


  public int getSectorSize()
  {
    return this.sectorSize;
  }


  public int getSides()
  {
    return this.sides;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.infoText;
  }
}

