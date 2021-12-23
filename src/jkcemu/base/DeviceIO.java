/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Zugriff auf physische Geraete
 */

package jkcemu.base;

import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jkcemu.Main;
import jkcemu.base.deviceio.LinuxDeviceIO;
import jkcemu.base.deviceio.MacDeviceIO;
import jkcemu.base.deviceio.UnixDeviceIO;
import jkcemu.base.deviceio.WinDeviceIO;


public class DeviceIO
{
  public static enum LibStatus {
			NOT_USED,
			LOADED,
			LOAD_ERROR,
			INSTALL_ERROR };

  public static enum MediaType {
			ANY_DISK,
			ANY_DISK_READ_ONLY,
			ANY_DISK_WRITE_ONLY,
			FLOPPYDISK,
			FLOPPYDISK_READ_ONLY };

  public static class DiskInfo
  {
    private String  text;
    private int     heads;
    private int     cyls;
    private int     sectorsPerTrack;
    private int     sectorSize;
    private long    diskSize;
    private boolean usb;

    public DiskInfo(
		String  text,
		int     cyls,
		int     heads,
		int     sectorsPerTrack,
		int     sectorSize )
    {
      this.text            = text;
      this.cyls            = cyls;
      this.heads           = heads;
      this.sectorsPerTrack = sectorsPerTrack;
      this.sectorSize      = sectorSize;
      this.usb             = false;
      this.diskSize        = ((long) heads
					* (long) cyls
					* (long) sectorsPerTrack
					* (long) sectorSize);
    }

    public DiskInfo( String text, long diskSize )
    {
      this.text            = text;
      this.usb             = false;
      this.heads           = 0;
      this.cyls            = 0;
      this.sectorsPerTrack = 0;
      this.sectorSize      = 0;
      this.diskSize        = diskSize;
    }

    public int getCylinders()
    {
      return this.cyls;
    }

    public long getDiskSize()
    {
      return this.diskSize;
    }

    public int getHeads()
    {
      return this.heads;
    }

    public int getSectorSize()
    {
      return this.sectorSize;
    }

    public int getSectorsPerTrack()
    {
      return this.sectorsPerTrack;
    }

    public boolean hasGeometry()
    {
      return ((this.cyls > 0)
		&& (this.heads > 0)
		&& (this.sectorsPerTrack > 0)
		&& (this.sectorSize > 0));
    }

    public boolean isUSB()
    {
      return this.usb;
    }

    public void setUSB( boolean state )
    {
      this.usb = state;
    }

    @Override
    public String toString()
    {
      return this.text;
    }
  };


  public static class Drive
  {
    private String  fileName;
    private String  text;
    private Boolean cdrom;
    private long    diskSize;
    private boolean specialPriv;

    public Drive(
		String  fileName,
		String  text,
		Boolean cdrom,
		long    diskSize,
		boolean specialPriv )
    {
      this.fileName    = fileName;
      this.cdrom       = cdrom;
      this.text        = text;
      this.diskSize    = diskSize;
      this.specialPriv = specialPriv;
    }

    public long getDiskSize()
    {
      return this.diskSize;
    }

    public String getFileName()
    {
      return this.fileName;
    }

    public Boolean getCDRom()
    {
      return this.cdrom;
    }

    public boolean needsSpecialPrivileges()
    {
      return this.specialPriv;
    }

    @Override
    public String toString()
    {
      return this.text;
    }
  };


  public static class LibInfo
  {
    private File      file;
    private LibStatus status;
    private int       recognizedVersion;
    private int       requiredVersion;
    private boolean   updRequested;

    public LibInfo(
		File      file,
		LibStatus status,
		int       recognizedVersion,
		int       requiredVersion,
		boolean   updRequested )
    {
      this.file              = file;
      this.status            = status;
      this.recognizedVersion = recognizedVersion;
      this.requiredVersion   = requiredVersion;
      this.updRequested      = updRequested;
    }

    public File getFile()
    {
      return this.file;
    }

    public int getRecognizedVersion()
    {
      return this.recognizedVersion;
    }

    public int getRequiredVersion()
    {
      return this.requiredVersion;
    }

    public LibStatus getStatus()
    {
      return this.status;
    }

    public boolean isUpdateRequested()
    {
      return this.updRequested;
    }
  };


  public static class Joystick implements Closeable
  {
    protected int     joyNum;
    protected float   xAxis;
    protected float   yAxis;
    protected int     pressedBtns;
    protected boolean active;

    protected Joystick( int joyNum )
    {
      this.joyNum      = joyNum;
      this.xAxis       = 0F;
      this.yAxis       = 0F;
      this.pressedBtns = 0;
      this.active      = true;
    }

    @Override
    public void close() throws IOException
    {
      this.active = false;
    }

    public float getXAxis()
    {
      return this.xAxis;
    }

    public float getYAxis()
    {
      return this.yAxis;
    }

    public int getPressedButtons()
    {
      return this.pressedBtns;
    }

    public boolean waitForEvent()
    {
      return false;
    }
  };


  public static abstract class RandomAccessDevice implements Closeable
  {
    protected RandomAccessDevice()
    {
      // leer
    }

    @Override
    public void close() throws IOException
    {
      // leer
    }

    public DiskInfo getDiskInfo() throws IOException
    {
      return null;
    }

    public abstract int read(
			byte[] buf,
			int    offs,
			int    len ) throws IOException;

    public abstract void seek( long pos ) throws IOException;

    public abstract void write(
			byte[] buf,
			int    offs,
			int    len ) throws IOException;
  };


  private static volatile NumberFormat numFmt                = null;
  private static volatile Set<String>  unreachableNetPaths   = null;
  private static volatile Thread       unreachableFindThread = null;


  public static void addAllTo( Collection<String> c, String... items )
  {
    for( String item : items )
      c.add( item );
  }


  public static void appendSizeTextTo( StringBuilder buf, long size )
  {
    if( size > 0 ) {
      if( buf.length() > 0 ) {
	buf.append( '\u0020' );
      }
      long kByte = size / 1024;
      if( kByte < 10000 ) {
	buf.append( kByte );
	buf.append( " KByte" );
      } else {
	long mByte = kByte / 1024;
	if( mByte < 1000 ) {
	  buf.append( mByte );
	  buf.append( " MByte" );
	} else {
	  synchronized( DeviceIO.class ) {
	    if( numFmt == null ) {
	      numFmt = NumberFormat.getNumberInstance();
	      if( numFmt instanceof DecimalFormat ) {
		((DecimalFormat) numFmt).applyPattern( "#####0.#" );
	      }
	    }
	  }
	  buf.append( numFmt.format( (double) size / 1073741824.0 ) );
	  buf.append( " GByte" );
	}
      }
    }
  }


  public static String createVendorModelText( String vendor, String model )
  {
    String rv = "";
    if( vendor != null ) {
      if( model != null ) {
	rv = String.format( "%s %s", vendor, model ).trim();
      } else {
	rv = vendor;
      }
    } else {
      if( model != null ) {
	rv = model;
      }
    }
    return rv;
  }


  public static List<Drive> getDrives( MediaType mediaType )
  {
    List<Drive> drives = new ArrayList<>();
    switch( Main.getOS() ) {
      case LINUX:
	LinuxDeviceIO.addDrivesTo( drives, mediaType );
	break;
      case MAC:
	MacDeviceIO.addDrivesTo( drives, mediaType );
	break;
      case UNIX:
	UnixDeviceIO.addDrivesTo( drives, mediaType );
	break;
      case WINDOWS:
	WinDeviceIO.addDrivesTo( drives, mediaType );
	break;
    }
    return drives;
  }


  public static LibInfo getLibInfo()
  {
    return Main.getOS() == Main.OS.WINDOWS ?
			WinDeviceIO.getLibInfo()
			: null;
  }


  public static String getShortPathName( String longName )
  {
    return Main.getOS() == Main.OS.WINDOWS ?
			WinDeviceIO.getShortPathName( longName )
			: null;
  }


  /*
   * Die Methode liefert die Pfade (Laufwerke bzw. Mount Points) zurueck,
   * die auf nicht erreichbare Netzwerk-Shares zeigen
   * und die durch einen Aufruf der Methode
   * startFindUnreachableNetPaths(...) ermittelt wurden.
   * Die zurueckgelieferten Pfade haben am Ende das Pfadtrennzeichen.
   */
  public static Set<String> getUnreachableNetPaths()
  {
    return unreachableNetPaths;
  }


  public static boolean hasUnreachableNetPaths()
  {
    return (unreachableNetPaths != null);
  }


  public static boolean isReachable(
				String              hostName,
				Map<String,Boolean> cache )
  {
    boolean reachable = false;
    if( hostName != null ) {
      Boolean lastReachable = cache.get( hostName );
      if( lastReachable != null ) {
	reachable = lastReachable.booleanValue();
      } else {
	try {
	  reachable = InetAddress.getByName( hostName ).isReachable( 5000 );
	}
	catch( IOException ex ) {}
	cache.put( hostName, reachable );
      }
    }
    return reachable;
  }


  public static File[] listRoots()
  {
    File[] roots = null;
    if( Main.getOS() == Main.OS.WINDOWS ) {
      roots = WinDeviceIO.listRoots();
    }
    return roots != null ? roots : File.listRoots();
  }


  public static boolean needsSpecialPrivileges(
					String             fileName,
					DeviceIO.MediaType requestedType )
  {
    boolean rv    = false;
    boolean read  = false;
    boolean write = false;
    switch( requestedType ) {
      case ANY_DISK:
      case FLOPPYDISK:
	read  = true;
	write = true;
	break;
      case ANY_DISK_READ_ONLY:
      case FLOPPYDISK_READ_ONLY:
	read  = true;
	break;
      case ANY_DISK_WRITE_ONLY:
	write = true;
	break;
    }
    if( read || write ) {
      File file = new File( fileName );
      if( read && !file.canRead() ) {
	rv = true;
      }
      if( write && !file.canWrite() ) {
	rv = true;
      }
    }
    return rv;
  }


  public static RandomAccessDevice openDeviceForRandomAccess(
				String  deviceName,
				boolean readOnly ) throws IOException
  {
    RandomAccessDevice rad = null;
    switch( Main.getOS() ) {
      case LINUX:
	rad = LinuxDeviceIO.openDeviceForRandomAccess( deviceName, readOnly );
	break;
      case MAC:
	rad = MacDeviceIO.openDeviceForRandomAccess( deviceName, readOnly );
	break;
      case UNIX:
	rad = UnixDeviceIO.openDeviceForRandomAccess( deviceName, readOnly );
	break;
      case WINDOWS:
	rad = WinDeviceIO.openDeviceForRandomAccess( deviceName, readOnly );
	break;
    }
    return rad;
  }


  public static InputStream openDeviceForSequentialRead(
				String deviceName ) throws IOException
  {
    InputStream in = null;
    switch( Main.getOS() ) {
      case LINUX:
	in = LinuxDeviceIO.openDeviceForSequentialRead( deviceName );
	break;
      case MAC:
	in = MacDeviceIO.openDeviceForSequentialRead( deviceName );
	break;
      case UNIX:
	in = UnixDeviceIO.openDeviceForSequentialRead( deviceName );
	break;
      case WINDOWS:
	in = WinDeviceIO.openDeviceForSequentialRead( deviceName );
	break;
    }
    return in;
  }


  public static OutputStream openDeviceForSequentialWrite(
				String  deviceName ) throws IOException
  {
    OutputStream out = null;
    switch( Main.getOS() ) {
      case LINUX:
	out = LinuxDeviceIO.openDeviceForSequentialWrite( deviceName );
	break;
      case MAC:
	out = MacDeviceIO.openDeviceForSequentialWrite( deviceName );
	break;
      case UNIX:
	out = UnixDeviceIO.openDeviceForSequentialWrite( deviceName );
	break;
      case WINDOWS:
	out = WinDeviceIO.openDeviceForSequentialWrite( deviceName );
	break;
    }
    return out;
  }


  public static Joystick openJoystick( int joyNum )
  {
    Joystick joystick = null;
    switch( Main.getOS() ) {
      case LINUX:
	joystick = LinuxDeviceIO.openJoystick( joyNum );
	break;
      case WINDOWS:
	joystick = WinDeviceIO.openJoystick( joyNum );
	break;
    }
    return joystick;
  }


  public static DiskInfo readDiskInfo( String deviceName ) throws IOException
  {
    DiskInfo           info = null;
    RandomAccessDevice rad  = null;
    try {
      rad  = openDeviceForRandomAccess( deviceName, true );
      info = rad.getDiskInfo();
    }
    finally {
      EmuUtil.closeSilently( rad );
    }
    return info;
  }


  public static void showError( String msg )
  {
    Frame owner = Main.getTopFrm();
    if( owner == null ) {
      owner = new Frame();
    }
    ErrorMsg.showLater( owner, msg, null );
  }


  /*
   * Die Methode ermittelt die Netzwerkpfade,
   * markiert diese erst einmal als nicht erreichbar,
   * und startet dann einen Thread,
   * der die Erreichbarkeit der Netzwerkpfade prueft.
   * Der uebergebene "observer", sofern not null,
   * wird nach der Suche im AWT-Thread aufgerufen.
   *
   * Ruechgabe:
   *    Netzwerkpfade, auch erreichbare
   */
  public synchronized static Set<String> startFindUnreachableNetPaths(
						final Runnable observer )
  {
    final Set<String> oldPaths = unreachableNetPaths;
    unreachableNetPaths        = getNetPaths();
    if( (unreachableNetPaths != null) && (unreachableFindThread == null) ) {
      unreachableFindThread = new Thread(
				Main.getThreadGroup(),
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    findUnreachableNetPaths( observer );
				  }
				},
				"JKCEMU unreachable net path finder" );
      unreachableFindThread.setDaemon( true );
      unreachableFindThread.start();
    }
    return unreachableNetPaths;
  }


	/* --- private Methoden --- */

  private static void findUnreachableNetPaths( final Runnable observer )
  {
    try {
      switch( Main.getOS() ) {
	case LINUX:
	  unreachableNetPaths = LinuxDeviceIO.findUnreachableNetPaths();
	  break;
	case MAC:
	  unreachableNetPaths = MacDeviceIO.findUnreachableNetPaths();
	  break;
	case UNIX:
	  unreachableNetPaths = UnixDeviceIO.findUnreachableNetPaths();
	  break;
	case WINDOWS:
	  unreachableNetPaths = WinDeviceIO.findUnreachableNetPaths();
	  break;
      }
    }
    finally {
      if( unreachableFindThread == Thread.currentThread() ) {
	unreachableFindThread = null;
      }
    }
    if( observer != null ) {
      EventQueue.invokeLater( observer );
    }
  }


  /*
   * Die Methode liefert die eingehaengten Netzwerkpfade
   * (Laufwerke bzw. Mount Points) zurueck.
   * Die zurueckgelieferten Pfade haben am Ende das Pfadtrennzeichen.
   */
  private static Set<String> getNetPaths()
  {
    Set<String> paths = null;
    switch( Main.getOS() ) {
      case LINUX:
	paths = LinuxDeviceIO.getNetPaths();
	break;
      case MAC:
	paths = MacDeviceIO.getNetPaths();
	break;
      case UNIX:
	paths = UnixDeviceIO.getNetPaths();
	break;
      case WINDOWS:
	paths = WinDeviceIO.getNetDrives();
	break;
    }
    return paths;
  }


	/* --- Konstruktor --- */

  private DeviceIO()
  {
    // leer
  }
}
