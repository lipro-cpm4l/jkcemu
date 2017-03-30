/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Format einer Diskette
 */

package jkcemu.disk;

import java.lang.*;


public class FloppyDiskFormat
{
  public static final FloppyDiskFormat FMT_400K
	= new FloppyDiskFormat(
		1, 80, 5, 1024, 1,
		0, 2, 2048, true, false,
		"400K (LLC2 CP/L)" );

  public static final FloppyDiskFormat FMT_624K
	= new FloppyDiskFormat(
		2, 80, 16, 256, 1,
		2, 2, 2048, true, false,
		"624K Standard (PC/M)" );

  public static final FloppyDiskFormat FMT_711K_I5_BASDOS
	= new FloppyDiskFormat(
		2, 80, 9, 512, 5,
		1, 1, 4096, false, false,
		"711K (KC compact BASDOS)" );

  public static final FloppyDiskFormat FMT_720K
	= new FloppyDiskFormat(
		2, 80, 9, 512, 1,
		0, 2, 2048, true, false,
		"720K Standard (A5105 RBASIC/SCPX)" );

  public static final FloppyDiskFormat FMT_780K
	= new FloppyDiskFormat(
		2, 80, 5, 1024, 1,
		2, 2, 2048, true, false,
		"780K Standard (CAOS, NANOS, Z1013 CP/M)" );

  public static final FloppyDiskFormat FMT_780K_I2
	= new FloppyDiskFormat(
		2, 80, 5, 1024, 2,
		2, 2, 2048, true, false,
		"780K mit Interleave 2:1 (A5105 RBASIC/SCPX)" );

  public static final FloppyDiskFormat FMT_780K_I3
	= new FloppyDiskFormat(
		2, 80, 5, 1024, 3,
		2, 2, 2048, true, false,
		"780K mit Interleave 3:1 (MicroDOS)" );

  public static final FloppyDiskFormat FMT_780K_I3_DATESTAMPER
	= new FloppyDiskFormat(
		2, 80, 5, 1024, 3,
		2, 2, 2048, true, true,
		"780K mit Interleave 3:1 und DateStamper (ZDOS/ML-DOS)" );

  public static final FloppyDiskFormat FMT_800K_I4
	= new FloppyDiskFormat(
		2, 80, 5, 1024, 4,
		0, 3, 2048, true, false,
		"800K mit Interleave 4:1 (Z9001 CP/A)" );

  private static final FloppyDiskFormat[] formats = {
			new FloppyDiskFormat( 2, 80,  9,  512 ),
			new FloppyDiskFormat( 2, 80,  5, 1024 ),
			new FloppyDiskFormat( 2, 80, 15,  512 ),
			new FloppyDiskFormat( 2, 80, 18,  512 ),
			new FloppyDiskFormat( 2, 80, 11, 1024 ),
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

  private int     sides;
  private int     cyls;
  private int     sectorsPerCyl;
  private int     sectorSize;
  private int     interleave;
  private int     diskSize;
  private int     sysTracks;
  private int     dirBlocks;
  private int     blockSize;
  private boolean blockNum16Bit;
  private boolean dateStamper;
  private String  infoText;


  public FloppyDiskFormat(
		int     sides,
		int     cyls,
		int     sectorsPerCyl,
		int     sectorSize,
		int     interleave,
		int     sysTracks,
                int     dirBlocks,
                int     blockSize,
                boolean blockNum16Bit,
		boolean dateStamper,
		String  infoText )
  {
    this.sides         = sides;
    this.cyls          = cyls;
    this.sectorsPerCyl = sectorsPerCyl;
    this.sectorSize    = sectorSize;
    this.interleave    = interleave;
    this.diskSize      = sides * cyls * sectorsPerCyl * sectorSize;
    this.sysTracks     = sysTracks;
    this.dirBlocks     = dirBlocks;
    this.blockSize     = blockSize;
    this.blockNum16Bit = blockNum16Bit;
    this.dateStamper   = dateStamper;
    if( infoText != null ) {
      this.infoText = infoText;
    } else {
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
  }


  public FloppyDiskFormat(
			int sides,
			int cyls,
			int sectorsPerCyl,
			int sectorSize )
  {
    this(
	sides,
	cyls,
	sectorsPerCyl,
	sectorSize,
	1,
	-1,
	-1,
	-1,
	true,
	false,
	null );
  }


  public int getBlockSize()
  {
    return this.blockSize;
  }


  public int getCylinders()
  {
    return this.cyls;
  }


  public int getDirBlocks()
  {
    return this.dirBlocks;
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


  public int getInterleave()
  {
    return this.interleave;
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
      maxDiskSize = m;
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


  public int getSysTracks()
  {
    return this.sysTracks;
  }


  public boolean isBlockNum16Bit()
  {
    return this.blockNum16Bit;
  }


  public boolean isDateStamperEnabled()
  {
    return this.dateStamper;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o instanceof FloppyDiskFormat ) {
	FloppyDiskFormat fmt = (FloppyDiskFormat) o;
	if( (fmt.sides == this.sides)
	    && (fmt.cyls == this.cyls)
	    && (fmt.sectorsPerCyl == this.sectorsPerCyl)
	    && (fmt.sectorSize == this.sectorSize)
	    && (fmt.diskSize == this.diskSize)
	    && (fmt.sysTracks == this.sysTracks)
	    && (fmt.dirBlocks == this.dirBlocks)
	    && (fmt.blockSize == this.blockSize)
	    && (fmt.blockNum16Bit == this.blockNum16Bit)
	    && (fmt.dateStamper == this.dateStamper) )
	{
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public String toString()
  {
    return this.infoText;
  }
}
