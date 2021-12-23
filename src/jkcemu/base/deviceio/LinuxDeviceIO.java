/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Zugriff auf physische Geraete unter Linux
 */

package jkcemu.base.deviceio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;


public class LinuxDeviceIO extends UnixDeviceIO
{
  private static final String MOUNT_TAB = "/proc/mounts";


  public static class LinuxJoystick extends DeviceIO.Joystick
  {
    private InputStream in;
    private byte[]      evtBuf;

    protected LinuxJoystick( int joyNum, InputStream in )
    {
      super( joyNum );
      this.in     = in;
      this.evtBuf = new byte[ 8 ];
    }

    @Override
    public void close() throws IOException
    {
      EmuUtil.closeSilently( this.in );
      this.in = null;
    }

    @Override
    public boolean waitForEvent()
    {
      boolean rv = false;
      if( this.active ) {
	try {
	  do {
	    if( EmuUtil.read(
			this.in,
			this.evtBuf ) == this.evtBuf.length )
	    {
	      short evtValue = (short) (((this.evtBuf[ 5 ] << 8) & 0xFF00)
					| (this.evtBuf[ 4 ] & 0xFF));
	      int   evtType    = evtBuf[ 6 ] & 0xFF;
	      int   evtSubType = evtBuf[ 7 ] & 0xFF;
	      if( (evtType & 0x3) == 1 ) {
		this.pressedBtns = (int) evtValue & 0xFFFF;
		rv               = true;
	      }
	      else if( (evtType & 0x3) == 2 ) {
		if( evtSubType == 0 ) {
		  this.xAxis = (float) evtValue / (float) Short.MAX_VALUE;
		  rv         = true;
		}
		else if( evtSubType == 1 ) {
		  this.yAxis = (float) evtValue / (float) Short.MAX_VALUE;
		  rv         = true;
		}
	      }
	    }
	  } while( this.active && !rv );
	}
	catch( IOException ex ) {
	  this.active = false;
	}
      }
      return rv;
    }
  };


  public static void addDrivesTo(
			List<DeviceIO.Drive> drives,
			DeviceIO.MediaType   requestedType )
  {
    Set<String>    mounts = new TreeSet<>();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader( new FileReader( MOUNT_TAB ) );
      String line = reader.readLine();
      while( line != null ) {
	if( line.startsWith( "/" ) ) {
	  int idx = line.indexOf( '\u0020' );
	  if( idx >= 0 ) {
	    mounts.add( line.substring( 0, idx ) );
	  } else {
	    mounts.add( line );
	  }
	}
	line = reader.readLine();
      }
    }
    catch( IOException ex ) {}
    finally {
      EmuUtil.closeSilently( reader );
    }
    boolean  state = false;
    String   dir   = "/sys/block";
    String[] items = (new File( dir )).list();
    if( items != null ) {
      for( String item : items ) {
	String  devFileName = "/dev/" + item;
	String  infoPath    = dir + "/" + item;
	Boolean cdrom       = null;
	boolean readOnly    = false;
	boolean removable   = false;
	Integer capability  = readHexSilently( infoPath + "/capability" );
	if( capability != null ) {
	  removable = ((capability.intValue() & 0x01) != 0);
	  if( (capability.intValue() & 0x08) != 0 ) {
	    cdrom    = Boolean.TRUE;
	    readOnly = true;
	  } else {
	    cdrom = Boolean.FALSE;
	  }
	} else {
	  removable = readAsciiSilently(
				infoPath + "/removable" ).equals( "1" );
	}
	if( !readOnly ) {
	  readOnly = readAsciiSilently( infoPath + "/ro" ).equals( "1" );
	}
	if( (removable || !mounts.contains( devFileName ))
	    && ((requestedType != DeviceIO.MediaType.ANY_DISK_WRITE_ONLY)
							|| !readOnly) )
	{
	  int    oldDriveCnt = drives.size();
	  String devicePath  = infoPath + "/device";
	  String vendor      = readAsciiSilently( devicePath + "/vendor" );
	  String model       = readAsciiSilently( devicePath + "/model" );

	  StringBuilder buf = new StringBuilder( 256 );
	  buf.append( vendor );
	  if( (buf.length() > 0) && !model.isEmpty() ) {
	    buf.append( '\u0020' );
	  }
	  buf.append( model );
	  long size = readSize( infoPath );
	  DeviceIO.appendSizeTextTo( buf, size );
	  int baseInfoLen = buf.length();
	  appendDeviceTo( buf, devFileName );
	  drives.add(
		new DeviceIO.Drive(
				devFileName,
				buf.toString(),
				cdrom,
				size,
				DeviceIO.needsSpecialPrivileges(
						devFileName,
						requestedType ) ) );

	  // Partitionen lesen und anhaengen
	  int partitionNum = 1;
	  for(;;) {
	    String partition = String.format(
					"%s%1d",
					item,
					partitionNum );

	    String partitionPath = infoPath + "/" + partition;
	    File   partitionDir  = new File( partitionPath );
	    if( !partitionDir.exists() || !partitionDir.isDirectory() ) {
	      break;
	    }
	    devFileName = "/dev/" + partition;

	    /*
	     * Wenn eine Partition im Dateisystem eingehaengt ist,
	     * dann dass gesamte Laufwerk ignorieren
	     */
	    if( mounts.contains( devFileName ) ) {
	      for( int i = drives.size() - 1; i >= oldDriveCnt; --i ) {
		drives.remove( i );
	      }
	      break;
	    }

	    // Partition anhaengen
	    buf.setLength( baseInfoLen );
	    if( buf.length() > 0 ) {
	      buf.append( ", " );
	    }
	    buf.append( "Partition " );
	    buf.append( partitionNum );
	    appendDeviceTo( buf, devFileName );
	    drives.add(
		new DeviceIO.Drive(
				devFileName,
				buf.toString(),
				cdrom,
				readSize( infoPath ),
				DeviceIO.needsSpecialPrivileges(
						devFileName,
						requestedType ) ) );
	    partitionNum++;
	  }
	}
	state = true;
      }
    }
    if( !state ) {
      List<String> possibleDriveNames = new ArrayList<>();
      possibleDriveNames.add( "/dev/floppy" );
      switch( requestedType ) {
	case ANY_DISK:
	case ANY_DISK_READ_ONLY:
	  DeviceIO.addAllTo(
			possibleDriveNames,
			"/dev/cdrom",
			"/dev/dvd",
			"/dev/sr0",
			"/dev/sr1" );
	  break;
      }
      DeviceIO.addAllTo(
			possibleDriveNames,
			"/dev/sdb",
			"/dev/sdb1",
			"/dev/sdb2",
			"/dev/sdb3",
			"/dev/sdb4",
			"/dev/sdc",
			"/dev/sdc1",
			"/dev/sdc2",
			"/dev/sdc3",
			"/dev/sdc4" );
      UnixDeviceIO.addDrivesTo( drives, requestedType, possibleDriveNames );
    }
  }


  /*
   * Die Methode ermittelt die eingehaengten,
   * aber nicht erreichbaren Netzwerkpfade.
   */
  public static Set<String> findUnreachableNetPaths()
  {
    return findUnreachableNetPaths( MOUNT_TAB );
  }


  public static Set<String> getNetPaths()
  {
    return getNetPaths( MOUNT_TAB );
  }


  public static DeviceIO.Joystick openJoystick( int joyNum )
  {
    DeviceIO.Joystick joystick = null;
    try {
      File devFile = new File( String.format(
					"/dev/input/js%d",
					joyNum ) );
      if( devFile.exists() ) {
	joystick = new LinuxJoystick(
				joyNum, 
				new FileInputStream( devFile ) );
      }
    }
    catch( IOException ex ) {}
    return joystick;
  }


  public static DeviceIO.RandomAccessDevice openDeviceForRandomAccess(
				String  deviceName,
				boolean readOnly ) throws IOException
  {
    DeviceIO.DiskInfo diskInfo = null;
    if( !readOnly ) {
      umountUnixDevice( deviceName );
    }
    if( deviceName.startsWith( "/dev/" ) ) {
      String device = deviceName.substring( 5 );
      if( !device.isEmpty() ) {
	String infoPath = "/sys/block/" + device;
	String vendor   = readAsciiSilently( infoPath + "/device/vendor" );
	String model    = readAsciiSilently( infoPath + "/device/model" );
	long   diskSize = readSize( infoPath );

	// Pruefen, ob es sich um ein Diskettenformat handelt
	long sectorSize = readLongSilently(
				infoPath + "/queue/hw_sector_size" );
	if( sectorSize == 0 ) {
	  sectorSize = readLongSilently(
				infoPath + "/queue/physical_block_size" );
	}
	if( sectorSize == 0 ) {
	  sectorSize = 512;
	}
	if( (sectorSize == 512) || (sectorSize == 1024) ) {
	  if( (diskSize == (720 * 1024))
	      || (diskSize == (1440 * 1024))
	      || (diskSize == (2880 * 1024)) )
	  {
	    long sectorsPerTrack = diskSize / 2 / 80 / sectorSize;
	    if( (sectorsPerTrack * sectorSize * 2 * 80) == diskSize ) {
	      diskInfo = new DeviceIO.DiskInfo(
				DeviceIO.createVendorModelText(
							vendor,
							model ),
				80,
				2,
				(int) sectorsPerTrack,
				(int) sectorSize );
	    }
	  }
	}
	if( (diskInfo == null) && (diskSize > 0) ) {
	  diskInfo = new DeviceIO.DiskInfo(
				DeviceIO.createVendorModelText(
							vendor,
							model ),
				diskSize );
	}
      }
    }
    return new UnixRandomAccessDevice(
		new RandomAccessFile( deviceName, readOnly ? "r" : "rw" ),
		diskInfo );
  }


	/* --- private Methoden --- */

  private static void appendDeviceTo( StringBuilder buf, String devFileName )
  {
    if( buf.length() > 0 ) {
      buf.append( " (" );
      buf.append( devFileName );
      buf.append( ')' );
    } else {
      buf.append( devFileName );
    }
  }


  private static String readAsciiSilently( String fileName )
  {
    StringBuilder buf = new StringBuilder( 32 );
    InputStream   in  = null;
    try {
      in    = new FileInputStream( fileName );
      int b = in.read();
      while( (b >= 0x20) && (b < 0x7F) ) {
	buf.append( (char) b );
	b = in.read();
      }
    }
    catch( IOException ex ) {}
    finally {
      EmuUtil.closeSilently( in );
    }
    return buf.toString().trim();
  }


  private static Integer readHexSilently( String fileName )
  {
    Integer rv = null;
    String  s  = readAsciiSilently( fileName );
    if( s != null ) {
      if( !s.isEmpty() ) {
	try {
	  rv = Integer.valueOf( s, 16 );
	}
	catch( NumberFormatException ex ) {}
      }
    }
    return rv;
  }


  private static long readLongSilently( String fileName )
  {
    long rv = 0;
    try {
      rv = Long.parseLong( readAsciiSilently( fileName ) );
    }
    catch( NumberFormatException ex ) {}
    return rv;
  }


  private static long readSize( String infoPath )
  {
    return readLongSilently( infoPath + "/size" ) * 512L;
  }


	/* --- Konstruktor --- */

  protected LinuxDeviceIO()
  {
    // leer
  }
}
