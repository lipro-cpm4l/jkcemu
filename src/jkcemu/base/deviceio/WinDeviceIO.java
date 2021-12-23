/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Zugriff auf physische Geraete unter Windows
 */

package jkcemu.base.deviceio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.Main;
import jkcemu.base.DesktopHelper;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;


public class WinDeviceIO
{
  public static final String LIBNAME_WIN32 = "jkcemu_win32.dll";
  public static final String LIBNAME_WIN64 = "jkcemu_win64.dll";
  public static final String UPDNAME_WIN32 = "jkcemu_win32.upd";
  public static final String UPDNAME_WIN64 = "jkcemu_win64.upd";

  private static final String FILENAME_PHYS_DRIVE_PREFIX
					= "\\\\.\\PhysicalDrive";

  private static final int DRIVE_REMOVABLE = 2;
  private static final int DRIVE_FIXED     = 3;
  private static final int DRIVE_REMOTE    = 4;
  private static final int DRIVE_CDROM     = 5;

  private static final int PARTITION_STYLE_RAW    = 2;
  private static final int REQUIRED_LIB_VERSION   = 2;
  private static final int ERROR_INVALID_FUNCTION = 1;
  private static final int ERROR_SECTOR_NOT_FOUND = 27;

  private static int     libVersion      = -1;
  private static boolean libChecked      = false;
  private static boolean libUpdRequested = false;
  private static File    libFile         = null;

  private static DeviceIO.LibStatus libStatus = DeviceIO.LibStatus.NOT_USED;


  public static class WinInputStream extends InputStream
  {
    private long   handle;
    private long   maxLen;
    private byte[] singleByteBuf;

    protected WinInputStream( long handle )
    {
      this.handle        = handle;
      this.singleByteBuf = null;
    }

    @Override
    public void close() throws IOException
    {
      closeDeviceInternal( this.handle );
    }

    @Override
    public int read() throws IOException
    {
      if( this.singleByteBuf == null ) {
	this.singleByteBuf = new byte[ 1 ];
      }
      return read( this.singleByteBuf, 0, 1 ) == 1 ?
			((int) this.singleByteBuf[ 0 ] & 0xFF)
			: -1;
    }

    @Override
    public int read( byte[] buf ) throws IOException
    {
      return read( buf, 0, buf.length );
    }

    @Override
    public synchronized int read( byte[] buf, int offs, int len )
							throws IOException
    {
      return readInternal( this.handle, buf, offs, len );
    }

    @Override
    public long skip( long n ) throws IOException
    {
      throw new IOException(
		"WinDeviceIO.WinInputStream.skip(...) nicht implementiert" );
    }
  };


  protected static class WinJoystick extends DeviceIO.Joystick
  {
    private long   xMin;
    private long   xMax;
    private long   yMin;
    private long   yMax;
    private long   xLastRaw;
    private long   yLastRaw;
    private long[] resultBuf;

    protected WinJoystick(
			int  joyNum,
			long xMin,
			long xMax,
			long yMin,
			long yMax )
    {
      super( joyNum );
      this.xMin      = xMin;
      this.xMax      = xMax;
      this.yMin      = yMin;
      this.yMax      = yMax;
      this.xLastRaw  = 0;
      this.yLastRaw  = 0;
      this.resultBuf = new long[ 3 ];
    }

    @Override
    public boolean waitForEvent()
    {
      boolean rv = false;
      if( this.active && checkLibLoaded() ) {
	if( libVersion >= REQUIRED_LIB_VERSION ) {
	  try {
	    do {
	      if( getJoystickPos( this.joyNum, this.resultBuf ) == 0 ) {
		int  b = (int) this.resultBuf[ 0 ];
		long x = this.resultBuf[ 1 ];
		long y = this.resultBuf[ 2 ];
		if( (b == this.pressedBtns)
		    && (x == this.xLastRaw)
		    && (y == this.yLastRaw) )
		{
		  try {
		    Thread.sleep( 20 );
		  }
		  catch( InterruptedException ex ) {
		    this.active = false;
		  }
		} else {
		  this.pressedBtns = b;
		  this.xLastRaw    = x;
		  this.yLastRaw    = y;
		  this.xAxis = adjustToFloat( x, this.xMin, this.xMax );
		  this.yAxis = adjustToFloat( y, this.yMin, this.yMax );
		  rv         = true;
		}
	      } else {
		this.active = false;
	      }
	    } while( this.active && !rv );
	  }
	  catch( UnsatisfiedLinkError e ) {
	    libVersionFailed();
	    this.active = false;
	  }
	}
      }
      return rv;
    }
  };


  protected static class WinOutputStream extends OutputStream
  {
    private long   handle;
    private int    bootIdx;
    private byte[] bootBuf;
    private byte[] singleByteBuf;

    protected WinOutputStream( long handle, boolean physDrive )
    {
      this.handle        = handle;
      this.bootIdx       = 0;
      this.bootBuf       = (physDrive ? new byte[ 0x2000 ] : null);
      this.singleByteBuf = null;
    }

    @Override
    public synchronized void close() throws IOException
    {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  int errCode = 0;
	  if( this.bootBuf != null ) {
	    int[] lenOut = new int[ 1 ];
	    errCode      = seekDevice( this.handle, 0L );
	    if( errCode == 0 ) {
	      errCode = writeDevice(
				this.handle,
				this.bootBuf,
				0,
				this.bootBuf.length,
				lenOut );
	    }
	    this.bootBuf = null;
	  }
	  int errCode2 = closeDevice( handle );
	  if( errCode == 0 ) {
	    errCode = errCode2;
	  }
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	  throwLibCorrupt();
	}
      }
    }

    @Override
    public void flush() throws IOException
    {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  int errCode = flushDevice( this.handle );
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	  throwLibCorrupt();
	}
      }
    }

    @Override
    public synchronized void write( int b ) throws IOException
    {
      if( this.singleByteBuf == null ) {
	this.singleByteBuf      = new byte[ 1 ];
	this.singleByteBuf[ 0 ] = (byte) b;
      }
      write( this.singleByteBuf, 0, 1 );
    }

    @Override
    public void write( byte[] buf ) throws IOException
    {
      write( buf, 0, buf.length );
    }

    @Override
    public synchronized void write(
				byte[] buf,
				int    offs,
				int    len ) throws IOException
    {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  if( this.bootBuf != null ) {
	    // Die ersten Bytes erstmal nur merken.
	    if( (this.bootIdx >= 0)
		&& (this.bootIdx < this.bootBuf.length) )
	    {
	      while( (this.bootIdx < this.bootBuf.length)
		     && (len > 0) && (offs < buf.length) )
	      {
		this.bootBuf[ this.bootIdx++ ] = buf[ offs++ ];
		--len;
	      }
	      if( this.bootIdx == this.bootBuf.length ) {
		this.bootIdx = -1;	// kein weiteres merken
		int errCode  = seekDevice(
					this.handle,
					this.bootBuf.length );
		if( errCode != 0 ) {
		  throwErrMsg( errCode );
		}
	      }
	    }
	  }
	  if( len > 0 ) {
	    int[] lenOut = new int[ 1 ];
	    do {
	      int errCode = writeDevice(
				this.handle,
				buf,
				offs,
				len,
				lenOut );
	      if( errCode != 0 ) {
		throwErrMsg( errCode );
	      }
	      offs += lenOut[ 0 ];
	      len  -= lenOut[ 0 ];
	    } while( len > 0 );
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	  throwLibCorrupt();
	}
      }
    }
  };


  protected static class WinRandomAccessDevice
				extends DeviceIO.RandomAccessDevice
  {
    private long              handle;
    private DeviceIO.DiskInfo diskInfo;

    protected WinRandomAccessDevice(
			long              handle,
			DeviceIO.DiskInfo diskInfo )
    {
      this.handle   = handle;
      this.diskInfo = diskInfo;
    }

    @Override
    public void close() throws IOException
    {
      closeDeviceInternal( this.handle );
    }

    @Override
    public DeviceIO.DiskInfo getDiskInfo()
    {
      return this.diskInfo;
    }

    @Override
    public int read( byte[] buf, int offs, int len ) throws IOException
    {
      return readInternal( this.handle, buf, offs, len );
    }

    @Override
    public void seek( long pos ) throws IOException
    {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  int errCode = seekDevice( this.handle, pos );
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	  throwLibCorrupt();
	}
      }
    }

    @Override
    public void write( byte[] buf, int offs, int len ) throws IOException
    {
      if( len > 0 ) {
	if( libVersion >= REQUIRED_LIB_VERSION ) {
	  try {
	    int[] lenOut = new int[ 1 ];
	    do {
	      int errCode = writeDevice(
				this.handle,
				buf,
				offs,
				len,
				lenOut );
	      if( errCode != 0 ) {
		throwErrMsg( errCode );
	      }
	      offs += lenOut[ 0 ];
	      len  -= lenOut[ 0 ];
	    } while( len > 0 );
	  }
	  catch( UnsatisfiedLinkError e ) {
	    libVersionFailed();
	    throwLibCorrupt();
	  }
	}
      }
    }
  };


  public static void addDrivesTo(
			Collection<DeviceIO.Drive> drives,
			DeviceIO.MediaType         requestedType )
  {
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  Set<Integer> driveNums = new TreeSet<>();
	  int          driveMask = getLogicalDrives();
	  char         driveChar = 'A';
	  for( int i = 0; i < 26; i++ ) {
	    if( (driveMask & 0x1) != 0 ) {
	      boolean status    = false;
	      boolean cdrom     = false;
	      String  driveName = String.format( "%c:", driveChar );
	      int     driveType = getDriveType( driveName + "\\" );
	      switch( driveType ) {
		case DRIVE_REMOVABLE:		// Wechselmedium
		case DRIVE_FIXED:		// z.B. USB-Festplatte
		  switch( requestedType ) {
		    case ANY_DISK:
		    case ANY_DISK_READ_ONLY:
		    case ANY_DISK_WRITE_ONLY:
		    case FLOPPYDISK:
		    case FLOPPYDISK_READ_ONLY:
		      status = true;
		      break;
		  }
		  break;
		case DRIVE_CDROM:		// CD-ROM
		  switch( requestedType ) {
		    case ANY_DISK:
		    case ANY_DISK_READ_ONLY:
		      status = true;
		      cdrom  = true;
		      break;
		  }
		  break;
	      }
	      if( status ) {
		String fileName = "\\\\.\\" + driveName;
		long[] handle   = new long[ 1 ];
		if( openDevice(
			fileName,
			false,		// read access
			false,		// write access
			false,		// random access
			handle ) == 0 )
		{
		  try {
		    /*
		     * Von den Geraeten mit dem Type DRIVE_FIXED
		     * sollen die ignoriert werden, die wirklich fest sind.
		     */
		    if( driveType == DRIVE_FIXED ) {
		      boolean[] hotplug = new boolean[ 3 ];
		      if( getHotplugInfo( handle[ 0 ], hotplug ) == 0 ) {
			if( !hotplug[ 0 ]	// media removable
			    && !hotplug[ 1 ]	// media hotplug
			    && !hotplug[ 2 ] )	// device hotplug
			{
			  status = false;
			}
		      } else {
			status = false;
		      }
		    }
		    if( status ) {
		      // Text des Geraetes
		      StringBuilder     textBuf  = new StringBuilder();
		      long              diskSize = -1L;
		      DeviceIO.DiskInfo diskInfo = getDiskInfo( handle[ 0 ] );
		      if( diskInfo != null ) {
			textBuf.append( diskInfo.toString() );
			diskSize = diskInfo.getDiskSize();
			if( diskSize > 0 ) {
			  DeviceIO.appendSizeTextTo( textBuf, diskSize );
			}
		      }
		      // Name der Partition
		      String volumeName = null;
		      byte[] volumeBuf  = new byte[ 256 ];
		      if( getVolumeInfo(
				fileName + "\\",
				volumeBuf ) == 0 )
		      {
			volumeName = asciiToString( volumeBuf );
		      }
		      if( status && !cdrom ) {
			/*
			 * Partitionsinfos
			 *
			 * Bei einem Diskettenlaufwerk schlaegt das Lesen
			 * des Partitionsstils fehl.
			 * Aus diesem Grund wird in dem Fall das Laufwerk
			 * trotzdem in der Liste belassen.
			 */
			long[] partStyle = new long[ 3 ];
			if( getPartitionStyle(
					handle[ 0 ],
					partStyle ) == 0 )
			{
			  if( partStyle[ 0 ] != PARTITION_STYLE_RAW ) {
			    /*
			     * Der Datentraeger ist partitioniert.
			     * Zum Lesen oder Schreiben des gesamten
			     * Datentraegers muss deshalb das
			     * physische Geraet geoeffnet werden.
			     */
			    int driveNum = getPhysicalDriveNum( handle[ 0 ] );
			    if( !driveNums.contains( driveNum ) ) {
			      StringBuilder buf = new StringBuilder();
			      if( textBuf.length() > 0 ) {
				buf.append( textBuf );
			      } else {
				buf.append( driveName );
			      }
			      buf.append( " inkl. Bootsektor" );
			      drives.add(
				new DeviceIO.Drive(
					String.format(
						"%s%d",
						FILENAME_PHYS_DRIVE_PREFIX,
						driveNum ),
					buf.toString(),
					cdrom,
					diskSize,
					true ) );
			      driveNums.add( driveNum );
			    }
			    if( textBuf.length() > 0 ) {
			      long[] partInfo = new long[ 2 ];
			      if( getPartitionInfo(
						handle[ 0 ],
						partInfo ) == 0 )
			      {
				textBuf.append(
					String.format(
						", Partition %d",
						partInfo[ 0 ] ) );
			      } else {
				textBuf.append( ", Partition" );
			      }
			    }
			  }
			}
		      }
		      boolean enclose = (textBuf.length() > 0);
		      if( enclose ) {
			textBuf.append( " (" );
		      }
		      textBuf.append( driveName );
		      if( volumeName != null ) {
			if( !volumeName.isEmpty() ) {
			  textBuf.append( '\u0020' );
			  textBuf.append( volumeName );
			}
		      }
		      if( enclose ) {
			textBuf.append( ')' );
		      }
		      drives.add( new DeviceIO.Drive(
						fileName,
						textBuf.toString(),
						cdrom,
						diskSize,
						false ) );
		    }
		  }
		  finally {
		    closeDevice( handle[ 0 ] );
		  }
		}
	      }
	    }
	    driveMask >>= 1;
	    driveChar++;
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
      }
    }
  }


  public static DeviceIO.LibInfo getLibInfo()
  {
    return new DeviceIO.LibInfo(
				libFile,
				libStatus,
				libVersion,
				REQUIRED_LIB_VERSION,
				libUpdRequested );
  }


  public static String getShortPathName( String longName )
  {
    String rv = null;
    if( longName != null ) {
      int len = longName.length();
      if( (len > 0) && checkLibLoaded() ) {
	if( libVersion >= REQUIRED_LIB_VERSION ) {
	  try {
	    char[] buf = new char[ Math.max( (2 * len) + 1, 256 ) ];
	    for( int i = 0; i < len; i++ ) {
	      buf[ i ] = longName.charAt( i );
	    }
	    buf[ len ] = '\u0000';
	    int wrSize = getShortPathName( buf );
	    if( wrSize > 0 ) {
	      rv = String.valueOf( buf, 0, wrSize );
	    }
	  }
	  catch( UnsatisfiedLinkError e ) {
	    libVersionFailed();
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode ermittelt die eingehaengten,
   * aber nicht erreichbaren Netzwerklaufwerke.
   * Die zurueckgelieferten Laufwerksfade haben am Ende
   * das Pfadtrennzeichen.
   */
  public static Set<String> findUnreachableNetPaths()
  {
    Set<String> unreachableNetPaths = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  Set<String> netDrives = getNetDrives();
	  if( netDrives != null ) {
	    Map<String,Boolean> cache = new HashMap<>();
	    for( String driveName : netDrives ) {
	      boolean reachable = false;
	      byte[]  remoteBuf = new byte[ 256 ];
	      if( getNetConnection(
				driveName.substring( 0, 2 ),
				remoteBuf ) == 0 )
	      {
		String remoteShare = asciiToString( remoteBuf );
		if( remoteShare.startsWith( "\\\\" ) ) {
		  if( remoteShare.length() > 2 ) {
		    String hostName = null;
		    int    idx      = remoteShare.indexOf( '\\', 2 );
		    if( idx < 0 ) {
		      hostName = remoteShare.substring( 2 );
		    } else if( idx > 2 ) {
		      hostName = remoteShare.substring( 2, idx );
		    }
		    if( hostName != null ) {
		      reachable = DeviceIO.isReachable( hostName, cache );
		    }
		  }
		}
	      }
	      if( !reachable ) {
		if( unreachableNetPaths == null ) {
		  unreachableNetPaths = new TreeSet<>();
		}
		unreachableNetPaths.add( driveName );
	      }
	    }
	  }
	}
	catch( ArrayStoreException ex ) {}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
      }
    }
    return unreachableNetPaths;
  }


  /*
   * Die Methode ermittelt die Netzwerklaufwerke.
   * Die zurueckgelieferten Pfade haben am Ende das Pfadtrennzeichen.
   */
  public static Set<String> getNetDrives()
  {
    Set<String> netDrives = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  int  driveMask = getLogicalDrives();
	  char driveChar = 'A';
	  for( int i = 0; i < 26; i++ ) {
	    if( (driveMask & 0x1) != 0 ) {
	      String driveName = String.format( "%c:", driveChar );
	      if( getDriveType( driveName ) == DRIVE_REMOTE ) {
		driveName += "\\";
		if( netDrives == null ) {
		  netDrives = new TreeSet<>();
		}
		netDrives.add( driveName );
	      }
	    }
	    driveMask >>= 1;
	    driveChar++;
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
      }
    }
    return netDrives;
  }


  public static File[] listRoots()
  {
    File[] roots = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  List<File> driveList = new ArrayList<>();
	  int        driveMask = getLogicalDrives();
	  char       driveChar = 'A';
	  for( int i = 0; i < 26; i++ ) {
	    if( (driveMask & 0x1) != 0 ) {
	      driveList.add(
			new File( String.format( "%c:\\", driveChar ) ) );
	    }
	    driveMask >>= 1;
	    driveChar++;
	  }
	  roots = driveList.toArray( new File[ driveList.size() ] );
	}
	catch( ArrayStoreException ex ) {}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
      }
    }
    return roots != null ? roots : File.listRoots();
  }


  public static DeviceIO.RandomAccessDevice openDeviceForRandomAccess(
				String  deviceName,
				boolean readOnly ) throws IOException
  {
    DeviceIO.RandomAccessDevice rad = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	Long handle = null;
	try {
	  boolean physDrive = checkPhysDriveDismount( deviceName );
	  long[]  hOut      = new long[ 1 ];
	  int     errCode   = openDevice(
					deviceName,
					true,		// read access
					!readOnly,	// write access
					true,		// random access
					hOut );
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	  handle = Long.valueOf( hOut[ 0 ] );
	  lockAndDismountDevice( handle.longValue(), physDrive );
	  rad = new WinRandomAccessDevice(
				handle.longValue(),
				getDiskInfo( handle.longValue() ) );
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
	finally {
	  if( (handle != null) && (rad == null) ) {
	    closeDeviceSilently( handle.longValue() );
	  }
	}
      }
    }
    return rad;
  }


  public static InputStream openDeviceForSequentialRead(
				String deviceName ) throws IOException
  {
    InputStream in = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	Long handle = null;
	try {
	  boolean physDrive = checkPhysDriveDismount( deviceName );
	  long[]  hOut      = new long[ 1 ];
	  int     errCode   = openDevice(
					deviceName,
					true,		// read access
					false,		// write access
					false,		// random access
					hOut );
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	  handle = Long.valueOf( hOut[ 0 ] );
	  lockAndDismountDevice( handle.longValue(), physDrive );
	  in = new WinInputStream( handle.longValue() );
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
	finally {
	  if( (handle != null) && (in == null) ) {
	    closeDeviceSilently( handle.longValue() );
	  }
	}
      }
    }
    return in;
  }


  public static OutputStream openDeviceForSequentialWrite(
				String  deviceName ) throws IOException
  {
    OutputStream out = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	Long handle = null;
	try {
	  long[]  hOut      = new long[ 1 ];
	  boolean physDrive = checkPhysDriveDismount( deviceName );
	  int     errCode   = openDevice(
					deviceName,
					physDrive,	// read access
					true,		// write access
					physDrive,	// random access
					hOut );
	  if( errCode != 0 ) {
	    throwErrMsg( errCode );
	  }
	  handle = Long.valueOf( hOut[ 0 ] );
	  lockAndDismountDevice( handle.longValue(), physDrive );
	  if( physDrive ) {
	    deleteDriveLayout( handle.longValue() );
	  }
	  out = new WinOutputStream( handle.longValue(), physDrive );
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
	finally {
	  if( (handle != null) && (out == null) ) {
	    closeDeviceSilently( handle.longValue() );
	  }
	}
      }
    }
    return out;
  }


  public static DeviceIO.Joystick openJoystick( int joyNum )
  {
    DeviceIO.Joystick joystick = null;
    if( checkLibLoaded() ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  long[] resultBuf = new long[ 4 ];
	  /*
	   * getJoystickBounds(...) liefert auch dann noch gueltige Werte,
	   * nachdem der Joystick abgezogen wurde.
	   * Aus diesem Grund wird zuerst getJoystickPos(...) aufgerufen,
	   * um zu testen, ob der Joystick angeschlossen ist.
	   */
	  if( getJoystickPos( joyNum, resultBuf ) == 0 ) {
	    if( getJoystickBounds( joyNum, resultBuf ) == 0 ) {
	      joystick = new WinJoystick(
				joyNum,
				resultBuf[ 0 ],		// Xmin
				resultBuf[ 1 ],		// Xmax
				resultBuf[ 2 ],		// Ymin
				resultBuf[ 3 ] );	// Ymax
	    }
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
      }
    }
    return joystick;
  }


	/* --- private Methoden --- */

  private static float adjustToFloat( long v, long vMin, long vMax )
  {
    float rv = 0F;
    if( vMax > vMin ) {
      if( v < vMin ) {
	v = vMin;
      } else if( v > vMax ) {
	v = vMax;
      }
      rv = (((float) (v - vMin) / (float) (vMax - vMin)) * 2F) - 1F;
    }
    return rv;
  }


  private static String asciiToString( byte[] srcBytes )
  {
    StringBuilder buf = new StringBuilder();
    for( byte b : srcBytes ) {
      if( b == 0 ) {
	break;
      }
      buf.append( (char) (b > 0 ? b : '_') );
    }
    return buf.toString().trim();
  }


  private synchronized static boolean checkLibLoaded()
  {
    if( !libChecked ) {
      String libName = null;
      String updName = null;
      if( isArch32() ) {
	libName = LIBNAME_WIN32;
	updName = UPDNAME_WIN32;
      }
      else if( isArch64() ) {
	libName = LIBNAME_WIN64;
	updName = UPDNAME_WIN64;
      }
      if( libName != null ) {
	File updFile = null;

	/*
	 * Bibliothek suchen
	 */
	URL url = Main.class.getResource( "/lib/" + libName );
	if( url != null ) {
	  String protocol = url.getProtocol();
	  String fileName = url.getFile();
	  if( (protocol != null) && (fileName != null) ) {
	    if( protocol.equals( "file" ) ) {
	      if( fileName.length() > 3 ) {
		char ch1 = Character.toUpperCase( fileName.charAt( 1 ) );
		if( (fileName.charAt( 0 ) == '/')
		    && ((ch1 >= 'A') && (ch1 <= 'Z'))
		    && (fileName.charAt( 2 ) == ':') )
		{
		  fileName = fileName.substring( 1 );
		}
		if( (File.separatorChar == '\\')
		    && fileName.indexOf( '/' ) >= 0 )
		{
		  fileName.replace( '/', '\\' );
		}
		File file = new File( fileName );
		if( file.exists() ) {
		  // URL zeigt auf eine existierende lokale Datei
		  libFile = file;
		}
	      }
	    }
	  }
	}
	if( (url != null) && (libFile == null) ) {
	  File configDir = Main.getConfigDir();
	  if( configDir != null ) {
	    File dstFile = new File( configDir, libName );

	    /*
	     * Pruefen, ob die Bibliothek aktualisiert werden soll,
	     * Das ist der Fall, wenn eine Update-Datei existiert.
	     */
	    updFile = new File( configDir, updName );
	    if( updFile.exists() ) {
	      // Kopieren erzwingen
	      dstFile.delete();
	    }

	    /*
	     * Bibliothek bei Bedarf in das Dateisystem kopieren
	     */
	    if( dstFile.exists() ) {
	      libFile = dstFile;
	    } else {
	      try {
		InputStream  in = null;
		OutputStream out = null;
		try {
		  in    = url.openStream();
		  out   = new FileOutputStream( dstFile );
		  int b = in.read();
		  while( b >= 0 ) {
		    out.write( b );
		    b = in.read();
		  }
		  out.close();
		  out     = null;
		  libFile = dstFile;
		  if( updFile != null ) {
		    // Update abgeschlossen
		    updFile.delete();
		  }
		}
		finally {
		  EmuUtil.closeSilently( in );
		  EmuUtil.closeSilently( out );
		}
	      }
	      catch( Exception ex ) {
		showLibError(
			"Die Bibliothek \'" + dstFile.getPath()
				+ "\'\nkonnte nicht angelegt"
				+ " bzw. aktualisiert werden.",
			ex );
		libStatus = DeviceIO.LibStatus.INSTALL_ERROR;
	      }
	    }
	  }
	}

	/*
	 * Bibliothek laden
	 */
	if( (libFile != null)
	    && (libStatus == DeviceIO.LibStatus.NOT_USED) )
	{
	  try {
	    System.load( libFile.getPath() );
	    libStatus = DeviceIO.LibStatus.LOADED;
	    checkLibVersion( updFile, url != null );
	  }
	  catch( Exception ex ) {
	    libStatus = DeviceIO.LibStatus.LOAD_ERROR;
	    showLibErrorLoadFailed( libFile, updFile, ex );
	  }
	  catch( UnsatisfiedLinkError e ) {
	    libStatus = DeviceIO.LibStatus.LOAD_ERROR;
	    showLibErrorLoadFailed( libFile, updFile, e );
	  }
	}
      }
      libChecked = true;
    }
    return (libStatus == DeviceIO.LibStatus.LOADED);
  }


  private static void checkLibVersion( File updFile, boolean libCopyable )
  {
    try {
      libVersion = getLibVersion();
    }
    catch( UnsatisfiedLinkError e ) {
      libVersion = 1;
    }
    if( libVersion < REQUIRED_LIB_VERSION ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "Die Bibliothek \'" );
      buf.append( libFile.getPath() );
      buf.append( "\'\n"
		+ "ist veraltet und muss aktualisiert werden.\n"
		+ "Solange das nicht geschehen ist,"
		+ " stehen einige Funktionen nicht zur Verf\u00FCgung." );
      if( (updFile != null) && libCopyable ) {
	/*
	 * Update-Datei als Merker fuer eine
	 * notwendige Aktualisierung anlegen
	 */
	createEmptyFile( updFile );
	libUpdRequested = true;
	if( updFile.exists() ) {
	  buf.append( "\n\nDie Bibliothek wird beim n\u00E4chsten"
		+ " Start von JKCEMU aktualisiert.\n"
		+ "Beenden Sie bitte alle laufenden Instanzen"
		+ " des Emulators und starten Sie anschlie\u00DFend neu." );
	} else {
	  buf.append( "\n\nZum Aktualisieren beenden Sie bitte JKCEMU"
		+ " und l\u00F6schen danach\n"
		+ "die Bibliotheksdatei."
		+ " Anschlie\u00DFend k\u00F6nnen Sie den Emulator"
		+ " neu starten." );
	  if( DesktopHelper.isOpenSupported() ) {
	    File configDir = Main.getConfigDir();
	    if( configDir != null ) {
	      try {
		DesktopHelper.open( configDir );
		buf.append( "\n\n"
			+ "In dem gerade neu ge\u00F6ffneten Fenster"
			+ " k\u00F6nnen Sie die" );
		String fileName = libFile.getName();
		if( fileName != null ) {
		  buf.append( " Datei " );
		  buf.append( fileName );
		} else {
		  buf.append( " Bibliotheksdatei" );
		}
		buf.append( "\nl\u00F6schen, aber erst, nachdem alle"
			+ " JKCEMU-Instanzen beendet wurden." );
	      }
	      catch( IOException ex ) {}
	    }
	  }
	}
      }
      DeviceIO.showError( buf.toString() );
    }
  }


  /*
   * Die Methode prueft, ob der uebergebene Geraetename fuer ein
   * physisches Geraet mit einer Geraetenummer steht.
   * Wenn ja, werden die logischen Laufwerke gesucht,
   * die auf dem gleichen physischen Geraet liegen
   * und aus dem Dateisystem ausgehaengt.
   *
   * Rueckgabe:
   *   true:  Geraetename steht fuer ein physisches Geraet mit Geraetenummer,
   *          Alle zugehoerigen logischen Laufwerke wurden ausgehaengt.
   *   false: kein physisches Geraet mit Geraetenummer
   */
  private static boolean checkPhysDriveDismount( String deviceName )
							throws IOException
  {
    int     preLen    = FILENAME_PHYS_DRIVE_PREFIX.length();
    boolean physDrive = deviceName.startsWith( FILENAME_PHYS_DRIVE_PREFIX );
    if( physDrive && (deviceName.length() > preLen) ) {
      try {
	int  driveNum  = Integer.parseInt( deviceName.substring( preLen ) );
	int  driveMask = getLogicalDrives();
	char driveChar = 'A';
	for( int i = 0; i < 26; i++ ) {
	  if( (driveMask & 0x1) != 0 ) {
	    String  driveName = String.format( "%c:", driveChar );
	    int     driveType = getDriveType( driveName + "\\" );
	    if( (driveType == DRIVE_REMOVABLE)
		|| (driveType == DRIVE_FIXED) )
	    {
	      int    errCode = 0;
	      long[] handle  = new long[ 1 ];
	      if( openDevice(
			"\\\\.\\" + driveName,
			true,			// read access
			false,			// write access
			false,			// random access
			handle ) == 0 )
	      {
		try {
		  if( getPhysicalDriveNum( handle[ 0 ] ) == driveNum ) {
		    errCode = lockDevice( handle[ 0 ] );
		    if( errCode == 0 ) {
		      errCode = dismountDevice( handle[ 0 ] );
		    }
		  }
		}
		finally {
		  closeDevice( handle[ 0 ] );
		}
	      }
	      if( errCode != 0 ) {
		StringBuilder buf = new StringBuilder( 256 );
		buf.append( "Das auf dem Datentr\u00E4ger liegende"
				+ " Laufwerk " );
		buf.append( driveName );
		buf.append( "\nkonnte nicht aus dem Dateisystem"
				+ " ausgeh\u00E4ngt werden." );
		String errMsg = getErrorMsg( errCode );
		if( errMsg != null ) {
		  if( !errMsg.isEmpty() ) {
		    buf.append( "\n\n" );
		    buf.append( errMsg );
		  }
		}
		throw new IOException( buf.toString() );
	      }
	    }
	  }
	  driveMask >>= 1;
	  driveChar++;
	}
      }
      catch( NumberFormatException ex ) {}
    }
    return physDrive;
  }


  private static void closeDeviceInternal( long handle ) throws IOException
  {
    if( libVersion >= REQUIRED_LIB_VERSION ) {
      try {
	int errCode = closeDevice( handle );
	if( errCode != 0 ) {
	  throwErrMsg( errCode );
	}
      }
      catch( UnsatisfiedLinkError e ) {
	libVersionFailed();
	throwLibCorrupt();
      }
    }
  }


  private static void closeDeviceSilently( long handle )
  {
    if( libVersion >= REQUIRED_LIB_VERSION ) {
      try {
	closeDevice( handle );
      }
      catch( UnsatisfiedLinkError e ) {
	libVersionFailed();
      }
    }
  }


  private static void createEmptyFile( File file )
  {
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );
    }
    catch( IOException ex ) {}
    finally {
      EmuUtil.closeSilently( out );
    }
  }


  private static DeviceIO.DiskInfo getDiskInfo( long handle )
  {
    DeviceIO.DiskInfo diskInfo = null;
    if( libVersion >= REQUIRED_LIB_VERSION ) {
      try {
	long[] geometry = new long[ 5 ];
	Arrays.fill( geometry, 0L );
	int errCode = getDiskGeometryEx( handle, geometry );
	if( errCode != 0 ) {
	  /*
	   * Bei Diskettenlaufwerken schlaegt u.U. getDiskGeometryEx fehl,
	   * nicht jedoch getDiskGeometry.
	   */
	  errCode = getDiskGeometry( handle, geometry );
	}
	if( errCode == 0 ) {
	  boolean geomValid = true;
	  for( int i = 0; i < 4; i++ ) {
	    if( (geometry[ i ] < 1)
		|| (geometry[ i ] > Integer.MAX_VALUE) )
	    {
	      geomValid = false;
	      break;
	    }
	  }
	  if( geomValid ) {
	    byte[] usb    = new byte[ 1 ];
	    byte[] vendor = new byte[ 32 ];
	    byte[] model  = new byte[ 32 ];
	    Arrays.fill( vendor, (byte) 0 );
	    Arrays.fill( model, (byte) 0 );
	    if( getStorageDeviceInfo( handle, usb, vendor, model ) != 0 ) {
	      vendor[ 0 ] = (byte) 0;
	      model[ 0 ]  = (byte) 0;
	    }
	    diskInfo = new DeviceIO.DiskInfo(
				DeviceIO.createVendorModelText(
						asciiToString( vendor ),
						asciiToString( model ) ),
				(int) geometry[ 0 ],
				(int) geometry[ 1 ],
				(int) geometry[ 2 ],
				(int) geometry[ 3 ] );
	    /*
	     * Bei USB-Sticks unterscheidet sich u.U.
	     * die Groesse laut Geometriedaten
	     * von der gelesenen Groesse.
	     * Da die Geometriedaten nur bei Disketten von Nutzen sind,
	     * werden sie bei groesseren Datentraegern ignoriert
	     * und die gelesene Diskgroesse verwendet.
	     */
	    if( (diskInfo.getDiskSize() > (2880 * 1024))
		&& (geometry[ 4 ] > 0) )
	    {
	      diskInfo = new DeviceIO.DiskInfo(
					diskInfo.toString(),
					geometry[ 4 ] );
	    }
	    diskInfo.setUSB( usb[ 0 ] != 0 );
	  }
	}
      }
      catch( UnsatisfiedLinkError e ) {
	libVersionFailed();
      }
    }
    return diskInfo;
  }


  private static boolean isArch32()
  {
    boolean rv   = false;
    String  arch = System.getProperty( "os.arch" );
    if( arch != null ) {
      if( ((arch.indexOf( "86" ) >= 0) && (arch.indexOf( "64" ) < 0))
	  || (arch.indexOf( "32" ) >= 0) )
      {
	rv = true;
      }
    }
    return rv;
  }


  private static boolean isArch64()
  {
    boolean rv   = false;
    String  arch = System.getProperty( "os.arch" );
    if( arch != null ) {
      if( arch.indexOf( "64" ) >= 0 ) {
	rv = true;
      }
    }
    return rv;
  }


  /*
   * Diese Methode wird aufgerufen, wenn beim Aufruf
   * einer nativen Methode ein UnsatisfiedLinkError auftrat.
   * Es werden alle nativen Methoden der fehlgeschlagenen
   * Bibliotheksversion gesperrt und eine Aktualisierung
   * der Bibliothek vorbereitet.
   */
  private static void libVersionFailed()
  {
    libVersion     = -1;
    File configDir = Main.getConfigDir();
    if( configDir != null ) {
      if( isArch32() ) {
	createEmptyFile( new File( configDir, UPDNAME_WIN32 ) );
      } else if( isArch64() ) {
	createEmptyFile( new File( configDir, UPDNAME_WIN64 ) );
      }
      libUpdRequested = true;
    }
  }


  private static void lockAndDismountDevice(
				long    handle,
				boolean physDrive ) throws IOException
  {
    int errCode = lockDevice( handle );
    if( errCode == 0 ) {
      errCode = dismountDevice( handle );
    }
    if( errCode != 0 ) {
      closeDeviceSilently( handle );
      throwErrMsg( errCode );
    }
  }


  private static int readInternal(
			long   handle,
			byte[] buf,
			int    offs,
			int    len ) throws IOException
  {
    int rv = -1;
    if( len > 0 ) {
      if( libVersion >= REQUIRED_LIB_VERSION ) {
	try {
	  int[] lenOut  = new int[ 1 ];
	  int   errCode = readDevice( handle, buf, offs, len, lenOut );
	  if( errCode == 0 ) {
	    if( lenOut[ 0 ] > 0 ) {
	      rv = lenOut[ 0 ];		// Anzahl der gelesenen Bytes
	    } else {
	      rv = -1;			// Dateiende
	    }
	  }
	  else if( (errCode == ERROR_INVALID_FUNCTION)
		   || (errCode == ERROR_SECTOR_NOT_FOUND) )
	  {
	    /*
	     * Bei Leseoperationen hinter dem Ende der Geraetedatei
	     * kommt bei Windows entweder ein INVALID_FUNCTION-
	     * oder ein SECTOR_NOT_FOUND-Fehler.
	     * Bei den anderen ueblichen Fehlerursachen
	     * (z.B. Geraet nicht verfuegbar oder CRC-Fehler)
	     * kommt ein anderer Fehlercode.
	     * Aus diesem Grund werden hier INVALID_FUNCTION- und
	     * SECTOR_NOT_FOUND-Fehler als Dateiende gewertet,
	     * auch wenn im konkreten Fall der Fehler
	     * eine andere Ursache haben sollte.
	     */
	    rv = -1;
	  } else {
	    throwErrMsg( errCode );		// Fehler
	  }
	}
	catch( UnsatisfiedLinkError e ) {
	  libVersionFailed();
	}
      }
    }
    return rv;
  }


  private static void showLibError( String msg, Throwable t )
  {
    StringBuilder msgBuf = new StringBuilder( 1024 );
    msgBuf.append( msg );
    msgBuf.append( "\nDadurch ist aktuell der Zugriff auf physische"
	+ " Datentr\u00E4ger, Joysticks und andere\n"
	+ "am Emulatorrechner angeschlossene Ger\u00E4te nicht"
	+ " oder nur eingeschr\u00E4nkt m\u00F6glich.\n"
	+ "Ansonsten ist JKCEMU voll funktionsf\u00E4hig.\n\n"
	+ "M\u00F6glicherweise stimmt etwas mit den Dateien"
	+ " oder den Berechtigungen im\n"
	+ "Konfigurationsverzeichnis nicht"
	+ " (siehe JKCEMU-Einstellungen, Bereich Sonstiges, ganz unten)." );
    String exMsg = t.getMessage();
    if( exMsg != null ) {
      if( !exMsg.isEmpty() ) {
	msgBuf.append( "\n\nDetaillierte Fehlermeldung:\n" );
	msgBuf.append( exMsg );
      }
    }
    DeviceIO.showError( msgBuf.toString() );
  }


  private static void showLibErrorLoadFailed(
					File      libFile,
					File      updFile,
					Throwable t )
  {
    if( updFile != null ) {
      createEmptyFile( updFile );
      libUpdRequested = true;
    }
    StringBuilder buf = new StringBuilder( 1024 );
    buf.append( "Die Bibliothek \'" );
    buf.append( libFile.getPath() );
    buf.append( "\'\nkonnte nicht geladen werden" );
    if( libUpdRequested ) {
      buf.append( " und wird deshalb beim n\u00E4chsten Start"
				+ " von JKCEMU aktualisiert" );
    }
    buf.append( '.' );
    showLibError( buf.toString(), t );
  }


  private static void throwErrMsg( int errCode ) throws IOException
  {
    String errMsg = null;
    if( libVersion >= REQUIRED_LIB_VERSION ) {
      try {
	errMsg = getErrorMsg( errCode );
	if( errMsg != null ) {
	  if( errMsg.isEmpty() ) {
	    errMsg = null;
	  }
	}
      }
      catch( UnsatisfiedLinkError e ) {
	libVersionFailed();
	throwLibCorrupt();
      }
    }
    throw new IOException( errMsg != null ? errMsg : "Ein-/Ausgabefehler" );
  }


  private static void throwLibCorrupt() throws IOException
  {
    throw new IOException( "DeviceIO-Bibliothek korrupt" );
  }


	/* --- native Methoden --- */

  /*
   * Lesen der Geometrie eines Datentraegers
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Anzahl Zylinder
   *          result[ 1 ]: Anzahl Koepfe (Tracks pro Zylinder)
   *          result[ 2 ]: Sektoren pro Track
   *          result[ 3 ]: Bytes pro Sektor
   *     <>0: Fehlercode
   */
  private static native int getDiskGeometry( long handle, long[] result );

  /*
   * Lesen der Geometrie und der Groesse eines Datentraegers
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Anzahl Zylinder
   *          result[ 1 ]: Anzahl Koepfe (Tracks pro Zylinder)
   *          result[ 2 ]: Sektoren pro Track
   *          result[ 3 ]: Bytes pro Sektor
   *          result[ 4 ]: Groesse des Datentraegers
   *     <>0: Fehlercode
   */
  private static native int getDiskGeometryEx( long handle, long[] result );

  /*
   * Lesen des Laufwerktyps
   *   Parameter:
   *     rootPath: Pfad auf das Laufwerk, z.B. "C:"
   *   Rueckgabewert:
   *     0: Typ nicht ermittelbar
   *     1: Pfad ist kein Laufwerk
   *     2: Wechselmedium (z.B. SD-Card, USB-Stick)
   *     3: festes Laufwertk (auch USB-Festplatte)
   *     4: Netzwerklaufwerk
   *     5: CD-ROM
   *     6: RAM-Disk
   *
   * Achtung! Diese Funktion steht erst ab Bibliotheksversion 2
   * zur Verfuegung.
   */
  private static native int getDriveType( String deviceName );

  /*
   * Lesen des Fehlertextes zu einem Fehlercode
   *   Parameter: Fehlercode
   *   Rueckgabe: Fehlertext
   */
  private static native String getErrorMsg( int errCode );

  /*
   * Lesen der Hotplug-Faehigkeiten
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Media removable
   *          result[ 1 ]: Media Hotplug
   *          result[ 2 ]: DeviceHotplug
   *     <>0: Fehlercode
   */
  private static native int getHotplugInfo( long handle, boolean[] result );

  /*
   * Lesen der Wertebereiche eines Joysticks
   *   Parameter:
   *     joyNum: Nummer des Joysticks
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Xmin
   *          result[ 1 ]: Xmax
   *          result[ 2 ]: Ymin
   *          result[ 3 ]: Ymax
   *     <>0: Fehlercode
   *
   * Achtung! Die Methode kann auch dann noch gueltige Werte liefern,
   * nachdem der Joystick abgezogen wurde.
   */
  private static native int getJoystickBounds( int joyNum, long[] result );

  /*
   * Lesen des Zustands eines Joysticks
   *   Parameter:
   *     joyNum: Nummer des Joysticks
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Maske mit den gedrueckten Knoepfen
   *          result[ 1 ]: X-Position
   *          result[ 2 ]: Y-Position
   *     <>0: Fehlercode
   */
  private static native int getJoystickPos( int joyNum, long[] result );

  /*
   * Lesen der Bibliotheksversion
   *   Rueckgabe: Versionnummer der Bibliothek
   *
   * Achtung! Diese Funktion steht erst ab Bibliotheksversion 2
   * zur Verfuegung.
   */
  private static native int getLibVersion();

  /*
   * Lesen der vorhandenen logischen Laufwerke
   *   Rueckgabe: Bitmaske mit den Laufwerken, Bit 0: A
   *
   * Achtung! Diese Funktion steht erst ab Bibliotheksversion 2
   * zur Verfuegung.
   */
  private static native int getLogicalDrives();

  /*
   * Lesen von Informationen ueber ein Volume
   *   Parameter:
   *     rootPathName: Pfad zum Laufwerk mit abschliessendem Backslash
   *   Rueckgabe:
   *     0: Werte gelesen und in die Rueckgabe-Arrays geschrieben:
   *     <>0: Fehlercode
   */
  private static native int getNetConnection(
				String deviceName,
				byte[] remoteNameOut );

  /*
   * Lesen der Partitionsnummer und Partitionsgroesse
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Partitionsnummer
   *          result[ 1 ]: Pertitionsgroesse
   *     <>0: Fehlercode
   */
  private static native int getPartitionInfo( long handle, long[] result );

  /*
   * Lesen des Partitionsstils eines Datentraegers
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     0: Werte gelesen und nach "result" geschrieben:
   *          result[ 0 ]: Partitionsstil (0=MBR, 1=GPT, 2=RAW)
   *          result[ 1 ]: Pertitionsanzahl
   *     <>0: Fehlercode
   */
  private static native int getPartitionStyle( long handle, long[] result );

  /*
   * Ermittlung der physischen Geraetenummer
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     >=0: physische Geraetenummer
   *     <0:  Fehler oder Geraetenummer nicht eindeutig
   */
  private static native int getPhysicalDriveNum( long handle );

  /*
   * Lesen eines 8.3-Dateinamens zu einem langen Dateinamen
   *   Parameter:
   *     langer Datei- oder Verzeichnisname mit vollstaendigem Pfad
   *   Rueckgabe:
   *     8.3-Name mit vollstaendigem Pfad oder "null" im Fehlerfall
   *
   * Achtung! Diese Funktion steht erst ab Bibliotheksversion 2
   * zur Verfuegung.
   */
  private static native int getShortPathName( char[] buf );

  /*
   * Lesen der Vendor-ID und der Produkt-ID eines Datentraegers
   *   Parameter:
   *     handle: der von openDevice zurueckgelieferte Wert
   *   Rueckgabe:
   *     0: Werte gelesen und in die Rueckgabe-Arrays geschrieben:
   *         usbOut[ 0 ] <> 0: USB-Geraet
   *     <>0: Fehlercode
   */
  private static native int getStorageDeviceInfo(
				long   handle,
				byte[] usbOut,
				byte[] vendorIdOut,
				byte[] productIdOut );

  /*
   * Lesen von Informationen ueber ein Volume
   *   Parameter:
   *     rootPathName: Pfad zum Laufwerk mit abschliessendem Backslash
   *   Rueckgabe:
   *     0:   Rueckgabe-Array gefuellt
   *     <>0: Fehlercode
   */
  private static native int getVolumeInfo(
				String rootPathName,
				byte[] volumeNameOut );

  /*
   * Rueckgabewert der Methoden fuer die Arbeit mit Geraetedateien:
   *   Fehlercode
   */
  private static native int openDevice(
				String  deviceName,
				boolean readAccess,
				boolean writeAccess,
				boolean randomAccess,
				long[]  handleOut );

  private static native int readDevice(
				long   handle,
				byte[] buf,
				int    offs,
				int    len,
				int[]  lenOut );

  private static native int writeDevice(
				long   handle,
				byte[] buf,
				int    offs,
				int    len,
				int[]  lenOut );

  private static native int closeDevice( long handle );
  private static native int deleteDriveLayout( long handle );
  private static native int dismountDevice( long handle );
  private static native int flushDevice( long handle );
  private static native int lockDevice( long handle );
  private static native int seekDevice( long handle, long pos );


	/* --- Konstruktor --- */

  private WinDeviceIO()
  {
    // leer
  }
}
