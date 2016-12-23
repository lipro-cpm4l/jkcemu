/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erzeugen einer Diskettenabbilddatei
 */

package jkcemu.disk;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.util.Arrays;
import java.util.Calendar;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileTimesView;
import jkcemu.base.FileTimesViewFactory;


public class DiskImgCreator
{
  private FileTimesViewFactory ftvFactory;
  private boolean              blockNum16Bit;
  private int                  extentBlocks;
  private int                  blockSize;
  private int                  dirBlocks;
  private int                  begDirArea;
  private int                  begFileArea;
  private int                  dstDirPos;
  private int                  dstFilePos;
  private int                  blockNum;
  private int                  begTimeFile;
  private int                  dstTimeFile;
  private int                  endTimeFile;
  private Calendar             calendar;
  private byte[]               diskBuf;


  public DiskImgCreator(
		FileTimesViewFactory fileTimesViewFactory,
		int                  sides,
		int                  cyls,
		int                  sysTracks,
		int                  sectPerCyl,
		int                  sectorSize,
		boolean              blockNum16Bit,
		int                  blockSize,
		int                  dirBlocks,
		boolean              dateStamper ) throws IOException
  {
    this.ftvFactory    = fileTimesViewFactory;
    this.blockNum16Bit = blockNum16Bit;
    this.blockSize     = blockSize;
    this.dirBlocks     = dirBlocks;
    this.diskBuf       = new byte[ sides * cyls * sectPerCyl * sectorSize ];
    this.begDirArea    = sysTracks * sides * sectPerCyl * sectorSize;
    this.begFileArea   = this.begDirArea + (this.dirBlocks * this.blockSize);
    this.dstDirPos     = this.begDirArea;
    this.dstFilePos    = this.begFileArea;
    this.extentBlocks  = (this.blockNum16Bit ? 8 : 16);
    this.blockNum      = this.dirBlocks;
    this.begTimeFile   = -1;
    this.dstTimeFile   = -1;
    this.endTimeFile   = -1;
    this.calendar      = null;
    if( this.begDirArea > 0 ) {
      Arrays.fill(
		this.diskBuf,
		0,
		Math.min( this.begDirArea, this.diskBuf.length ),
		(byte) 0x00 );
    }
    if( this.begDirArea < this.diskBuf.length ) {
      Arrays.fill(
		this.diskBuf,
		this.begDirArea,
		this.diskBuf.length,
		(byte) 0xE5 );
    }
    if( dateStamper ) {
      byte[] timeBytes = new byte[ this.dirBlocks * this.blockSize / 2 ];
      Arrays.fill( timeBytes, (byte) 0x00 );
      String text    = "!!!TIME\u0092";
      int    textLen = text.length();
      int    srcPos  = 0;
      int    dstPos  = 0x0F;
      while( dstPos < timeBytes.length ) {
	if( srcPos >= textLen ) {
	  srcPos = 0;
	}
	timeBytes[ dstPos ] = (byte) text.charAt( srcPos++ );
	dstPos += 0x10;
      }
      try( ByteArrayInputStream in = new ByteArrayInputStream( timeBytes ) ) {
	this.begTimeFile = this.dstFilePos;
	addFile(
		0,
		DateStamper.FILENAME,
		in,
		false,
		false,
		false,
		null );
	this.dstTimeFile = this.begTimeFile + 0x10;
	this.endTimeFile = this.dstFilePos;
      }
      this.calendar = Calendar.getInstance();
    }
  }


  /*
   * Rueckgabewert:
   *   Anzahl der Directory-Eintraege
   */
  public int addFile(
		int     userNum,
		String  entryName,
		File    file,
		boolean readOnly,
		boolean sysFile,
		boolean archived ) throws IOException
  {
    int rv = 0;
    if( (entryName != null) && (file != null) ) {
      if( (this.begTimeFile >= 0)
	  && entryName.equals( DateStamper.FILENAME ) )
      {
	throw new IOException( "Bei einem Diskettenformat mit"
		+ " DateStamper-Unterst\u00FCtzung\n"
		+ "werden die Zeitstempel in der Datei "
		+ DateStamper.FILENAME + " gespeichert.\n"
		+ "Diese Datei wird automatisch angelegt und"
		+ " kann deshalb\n"
		+ "nicht vom Anwender hinzugef\u00FCgt werden." );
      }
      InputStream in = null;
      try {
	in = new BufferedInputStream( new FileInputStream( file ) );
	rv = addFile(
		userNum,
		entryName,
		in,
		readOnly,
		sysFile,
		archived,
		this.ftvFactory.getFileTimesView( file ) );
      }
      finally {
	EmuUtil.closeSilent( in );
      }
    }
    return rv;
  }


  public void fillSysTracks( File file ) throws IOException
  {
    if( file != null ) {
      InputStream in = null;
      try {
	in    = new BufferedInputStream( new FileInputStream( file ) );
	int p = 0;
	int b = in.read();
	while( b >= 0 ) {
	  if( p >= this.begDirArea ) {
	    throw new IOException(
			"Datei f\u00FCr Systemspuren ist zu gro\u00DF!" );
	  }
	  this.diskBuf[ p++ ] = (byte) b;
	  b = in.read();
	}
      }
      finally {
	EmuUtil.closeSilent( in );
      }
    }
  }


  public byte[] getPlainDiskByteBuffer()
  {
    // DateStamper Pruefsummen berechnen
    if( (this.begTimeFile >= 0) && (this.begTimeFile < this.endTimeFile) ) {
      int pos = this.begTimeFile;
      while( pos < this.endTimeFile ) {
	int cks = 0;
	for( int i = 0; (i < 0x7F) && (pos < this.endTimeFile); i++ ) {
	  cks += ((int) this.diskBuf[ pos++ ] & 0xFF);
	}
	if( pos < this.endTimeFile ) {
	  this.diskBuf[ pos++ ] = (byte) cks;
	}
      }
    }
    return this.diskBuf;
  }


	/* --- private Methoden --- */

  private int addDirEntry(
			int           userNum,
			String        entryName,
			boolean       readOnly,
			boolean       sysFile,
			boolean       archived,
			FileTimesView fileTimesView,
			int           extentNum ) throws IOException
  {
    if( this.dstDirPos >= this.begFileArea ) {
      throw new IOException( "Directory voll" );
    }

    int entryBegPos                  = this.dstDirPos;
    this.diskBuf[ this.dstDirPos++ ] = (byte) (userNum & 0x0F);

    int[] sizes = { 8, 3 };
    int   len   = entryName.length();
    int   pos   = 0;
    for( int i = 0; i < sizes.length; i++ ) {
      int n = sizes[ i ];
      while( (pos < len) && (n > 0) ) {
	char ch = entryName.charAt( pos++ );
	if( ch == '.' ) {
	  break;
	}
	this.diskBuf[ this.dstDirPos++ ] = (byte) ch;
	--n;
      }
      while( (n > 0) ) {
	this.diskBuf[ this.dstDirPos++ ] = (byte) '\u0020';
	--n;
      }
      if( pos < len ) {
	if( entryName.charAt( pos ) == '.' ) {
	  pos++;
	}
      }
    }
    this.diskBuf[ this.dstDirPos++ ] = (byte) (extentNum & 0x1F);
    this.diskBuf[ this.dstDirPos++ ] = (byte) 0;
    this.diskBuf[ this.dstDirPos++ ] = (byte) ((extentNum >> 5) & 0x3F);

    if( readOnly ) {
      this.diskBuf[ entryBegPos + 9 ] |= 0x80;
    }
    if( sysFile ) {
      this.diskBuf[ entryBegPos + 10 ] |= 0x80;
    }
    if( archived ) {
      this.diskBuf[ entryBegPos + 11 ] |= 0x80;
    }
    for( int i = 0; i < 17; i++ ) {
      this.diskBuf[ this.dstDirPos++ ] = (byte) 0;
    }
    if( (this.dstTimeFile >= 0) && (this.dstTimeFile < this.endTimeFile) ) {
      if( fileTimesView != null ) {
	writeTimeEntry( fileTimesView.getCreationMillis() );
	writeTimeEntry( fileTimesView.getLastAccessMillis() );
	writeTimeEntry( fileTimesView.getLastModifiedMillis() );
	this.dstTimeFile++;
      } else {
	this.dstTimeFile += 0x10;
      }
    }
    return entryBegPos;
  }


  /*
   * Rueckgabewert:
   *   Anzahl der Directory-Eintraege
   */
  private int addFile(
		int           userNum,
		String        entryName,
		InputStream   in,
		boolean       readOnly,
		boolean       sysFile,
		boolean       archived,
		FileTimesView fileTimesView ) throws IOException
  {
    int extentNum = 0;
    int b         = in.read();
    if( b >= 0 ) {
      while( b >= 0 ) {
	if( extentNum >= 0x800 ) {
	  throwFileTooBig( entryName );
	}
	int dirPos = addDirEntry(
				userNum,
				entryName,
				readOnly,
				sysFile,
				archived,
				fileTimesView,
				extentNum++ );
	int blkPos = dirPos + 0x10;

	// Extent schreiben
	int begExtentPos       = this.dstFilePos;
	int endExtentPos       = begExtentPos;
	int remainExtentBlocks = this.extentBlocks;
	while( (remainExtentBlocks > 0) && (b >= 0) ) {
	  int remainBlockBytes = this.blockSize;
	  while( (remainBlockBytes > 0) && (b >= 0) ) {
	    if( this.dstFilePos >= this.diskBuf.length ) {
	      throw new IOException(
		"Das ausgew\u00E4hlten Diskettenformat bietet nicht"
				+ " gen\u00FCgend Platz." );
	    }
	    this.diskBuf[ this.dstFilePos++ ] = (byte) b;
	    --remainBlockBytes;
	    b = in.read();
	  }
	  endExtentPos       = this.dstFilePos;
	  int endOf128BlkPos = (this.dstFilePos + 127) & ~0x7F;
	  if( endOf128BlkPos > this.diskBuf.length ) {
	    endOf128BlkPos = this.diskBuf.length;
	  }
	  while( (remainBlockBytes > 0)
		 && (this.dstFilePos < endOf128BlkPos) )
	  {
	    this.diskBuf[ this.dstFilePos++ ] = (byte) 0x1A;
	    --remainBlockBytes;
	  }
	  while( (remainBlockBytes > 0)
		 && (this.dstFilePos < this.diskBuf.length) )
	  {
	    this.diskBuf[ this.dstFilePos++ ] = (byte) 0x00;
	    --remainBlockBytes;
	  }
	  if( this.blockNum16Bit ) {
	    this.diskBuf[ blkPos++ ] = (byte) (this.blockNum & 0xFF);
	    this.diskBuf[ blkPos++ ] = (byte) ((this.blockNum >> 8) & 0xFF);
	    this.blockNum++;
	    if( this.blockNum > 0xFFFF ) {
	      throwFileTooBig( entryName );
	    }
	  } else {
	    this.diskBuf[ blkPos++ ] = (byte) this.blockNum++;
	    if( this.blockNum > 0xFF ) {
	      throwFileTooBig( entryName );
	    }
	  }
	  --remainExtentBlocks;
	}
	this.diskBuf[ dirPos + 15 ] =
		(byte) ((endExtentPos - begExtentPos + 127) / 128);
      }
    } else {
      // leere Datei
      addDirEntry(
		userNum,
		entryName,
		readOnly,
		sysFile,
		archived,
		fileTimesView,
		extentNum++ );
    }
    return extentNum;
  }


  private static void throwFileTooBig( String entryName ) throws IOException
  {
    throw new IOException( entryName + ": Datei zu gro\u00DF!" );
  }


  private static byte toBcdByte( int value )
  {
    return (byte) (((((value / 10) % 10) << 4) & 0xF0)
				| ((value % 10) & 0x0F));
  }


  private void writeTimeEntry( Long millis )
  {
    if( (this.dstTimeFile + 4) < this.endTimeFile ) {
      boolean done = false;
      if( (this.calendar != null) && (millis != null) ) {
	this.calendar.clear();
	this.calendar.setTimeInMillis( millis.longValue() );
	int year = this.calendar.get( Calendar.YEAR );
	if( (year >= 1978) && (year < 2078) ) {
	  this.diskBuf[ this.dstTimeFile++ ] = toBcdByte( year );
	  this.diskBuf[ this.dstTimeFile++ ] =
		toBcdByte( this.calendar.get( Calendar.MONTH ) + 1 );
	  this.diskBuf[ this.dstTimeFile++ ] =
		toBcdByte( this.calendar.get( Calendar.DAY_OF_MONTH ) );
	  this.diskBuf[ this.dstTimeFile++ ] =
		toBcdByte( this.calendar.get( Calendar.HOUR_OF_DAY ) );
	  this.diskBuf[ this.dstTimeFile++ ] =
		toBcdByte( this.calendar.get( Calendar.MINUTE ) );
	  done = true;
	}
      }
      if( !done ) {
	this.dstTimeFile += 5;
      }
    }
  }
}
