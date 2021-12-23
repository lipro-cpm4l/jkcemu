/*
 * (c) 2019-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Zugriff auf physische Geraete unter einem unixoiden Betriebssystem
 */

package jkcemu.base.deviceio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.base.ProcessExecuter;


public class UnixDeviceIO
{
  private static final String[] driveNames = {
					"/dev/floppy",
					"/dev/cdrom",
					"/dev/dvd" };


  public static class UnixRandomAccessDevice
				extends DeviceIO.RandomAccessDevice
  {
    private RandomAccessFile  raf;
    private DeviceIO.DiskInfo diskInfo;

    protected UnixRandomAccessDevice(
				RandomAccessFile  raf,
				DeviceIO.DiskInfo diskInfo )
    {
      this.raf      = raf;
      this.diskInfo = diskInfo;
    }

    protected UnixRandomAccessDevice( RandomAccessFile raf )
    {
      this.raf      = raf;
      this.diskInfo = null;
    }

    @Override
    public void close() throws IOException
    {
      this.raf.close();
    }

    @Override
    public synchronized DeviceIO.DiskInfo getDiskInfo() throws IOException
    {
      if( this.diskInfo == null ) {
	this.diskInfo = new DeviceIO.DiskInfo( "", this.raf.length() );
      }
      return this.diskInfo;
    }

    @Override
    public int read( byte[] buf, int offs, int len ) throws IOException
    {
      return this.raf.read( buf, offs, len );
    }

    @Override
    public void seek( long pos ) throws IOException
    {
      this.raf.seek( pos );
    }

    @Override
    public void write( byte[] buf, int offs, int len ) throws IOException
    {
      this.raf.write( buf, offs, len );
    }
  };


  public static void addDrivesTo(
			List<DeviceIO.Drive> drives,
			DeviceIO.MediaType   requestedType )
  {
    List<String> possibleDriveNames = new ArrayList<>();
    switch( requestedType ) {
      case ANY_DISK_READ_ONLY:
      case FLOPPYDISK_READ_ONLY:
      case FLOPPYDISK:
	possibleDriveNames.add( "/dev/floppy" );
	break;
    }
    switch( requestedType ) {
      case ANY_DISK_READ_ONLY:
	DeviceIO.addAllTo(
			possibleDriveNames,
			"/dev/cdrom",
			"/dev/dvd" );
	break;
    }
    addDrivesTo( drives, requestedType, possibleDriveNames );
  }


  protected static void addDrivesTo(
				List<DeviceIO.Drive> drives,
				DeviceIO.MediaType   requestedType,
				Collection<String>   possibleDriveNames )
  {
    /*
     * Zuerst werden die FileStores gelesen um zu erfahren,
     * welche gemounteten Geraete es gibt.
     * Ueblicherweise liefert auf einem Linux/Unix-System
     * die Methode FileStore.name() den Namen der Geraetedatei
     * und FileStore.toString() einen Infotext,
     * der mit dem Mount-Punkt beginnt und
     * mit dem in Klammern stehenden Namen der Geraetedatei endet.
     * Allerdings ist fuer die Klasse FileStore die Methode toString()
     * nicht als ueberschrieben dokumentiert,
     * so dass nicht wirklich sicher ist,
     * ob die Methode einen so aufgebauten Text liefert.
     * Deshalb wird dieser Text hier nur dann verwendet,
     * wenn er mit einem Slash beginnt.
     *
     * Die Erkennung der optischen und Wechselspeichermedien
     * erfolgt anhand des Dateisystems.
     * Das ist zwar nicht wirklich sicher,
     * trifft aber mit einer gewissen Wahrscheinlichkeit zu.
     *
     * Laufwerke, die mehrmals vorkommen und somit mehrere
     * Mount-Punkte haben, werden ignoriert.
     */
    Map<String,String> driveName2Text      = new HashMap<>();
    Set<String>        ignoredDriveNames   = new TreeSet<>();
    Set<String>        mountedDriveNames   = new TreeSet<>();
    Set<String>        cdromDriveNames     = new TreeSet<>();
    Set<String>        removableDriveNames = new TreeSet<>();
    for( FileStore fs : FileSystems.getDefault().getFileStores() ) {
      String driveName = fs.name();
      if( driveName != null ) {
	if( mountedDriveNames.contains( driveName ) ) {
	  driveName2Text.remove( driveName );
	  ignoredDriveNames.add( driveName );
	} else {
	  mountedDriveNames.add( driveName );
	  String text = fs.toString();
	  if( text != null ) {
	    if( text.startsWith( "/" ) ) {
	      driveName2Text.put( driveName, text );
	    }
	  }
	  String fsType = fs.type();
	  if( fsType != null ) {
	    fsType = fsType.toUpperCase();
	    if( fsType.startsWith( "CDFS" )
		|| fsType.startsWith( "UDF" )
		|| (fsType.indexOf( "9660" ) >= 0) )
	    {
	      cdromDriveNames.add( driveName );
	    }
	    if( fsType.startsWith( "FAT" )
		|| fsType.startsWith( "EXFAT" )
		|| fsType.startsWith( "VFAT" )
		|| fsType.startsWith( "MSDOS" ) )
	    {
	      removableDriveNames.add( driveName );
	    }
	  }
	}
      }
    }

    // uebliche Geraetedateien auf Vorhandensein testen
    for( String driveName : possibleDriveNames ) {
      File f = new File( driveName );
      if( !ignoredDriveNames.contains( driveName )
	  && f.exists()
	  && ((requestedType != DeviceIO.MediaType.ANY_DISK_WRITE_ONLY)
							|| f.canWrite()) )
      {
	Boolean cdrom = null;
	if( driveName.indexOf( "floppy" ) >= 0 ) {
	  cdrom = Boolean.FALSE;
	} else if( (driveName.indexOf( "cdrom" ) >= 0)
		   || (driveName.indexOf( "dvd" ) >= 0) )
	{
	  cdrom = Boolean.TRUE;
	}
	if( mountedDriveNames.contains( driveName ) ) {
	  if( cdrom == null ) {
	    if( removableDriveNames.contains( driveName ) ) {
	      cdrom = Boolean.FALSE;
	    } else if( cdromDriveNames.contains( driveName ) ) {
	      cdrom = Boolean.TRUE;
	    }
	  }
	}
	if( cdrom != null ) {
	  if( cdrom.booleanValue() ) {
	    if( (requestedType != DeviceIO.MediaType.ANY_DISK)
		&& (requestedType != DeviceIO.MediaType.ANY_DISK_READ_ONLY) )
	    {
	      // Laufwerk aussortieren
	      driveName = null;
	    }
	  }
	}
	if( driveName != null ) {
	  String text = driveName2Text.get( driveName );
	  if( text == null ) {
	    text = driveName;
	  }
	  drives.add(
		new DeviceIO.Drive(
				driveName,
				text,
				cdrom,
				0L,
				DeviceIO.needsSpecialPrivileges(
						driveName,
						requestedType ) ) );
	  mountedDriveNames.remove( driveName );
	}
      }
    }

    // passende Laufwerke aus den restlichen FileStores hinzufuegen
    for( String driveName : mountedDriveNames ) {
      /*
       * systeminterne virtuelle Laufwerke (ohne beginnenden Slash),
       * Netzwerklaufwerke und die erste Festplatte ausblenden
       */
      if( driveName.startsWith( "/" )
	  && !driveName.startsWith( "//" ) )		// Netzwerk
      {
	String text = driveName2Text.get( driveName );
	if( text == null ) {
	  text = driveName;
	}
	if( cdromDriveNames.contains( driveName ) ) {
	  switch( requestedType ) {
	    case ANY_DISK:
	    case ANY_DISK_READ_ONLY:
	      drives.add( new DeviceIO.Drive(
				driveName,
				text,
				Boolean.TRUE,
				0L,
				DeviceIO.needsSpecialPrivileges(
						driveName,
						requestedType ) ) );
	  }
	} else if( removableDriveNames.contains( driveName ) ) {
	  drives.add(
		new DeviceIO.Drive(
				driveName,
				text,
				Boolean.FALSE,
				0L,
				DeviceIO.needsSpecialPrivileges(
						driveName,
						requestedType ) ) );
	}
      }
    }
  }


  /*
   * Die Methode ermittelt die eingehaengten,
   * aber nicht erreichbaren Netzwerkpfade.
   */
  public static Set<String> findUnreachableNetPaths( )
  {
    return findUnreachableNetPaths( "/etc/fstab" );
  }


  protected static Set<String> findUnreachableNetPaths( String fstab )
  {
    Set<String>        paths     = null;
    Map<String,String> path2Host = getNetPathToHostNameMap( fstab );
    if( path2Host != null ) {
      Map<String,Boolean> cache = new HashMap<>();
      for( String path : path2Host.keySet() ) {
	boolean reachable = false;
	String  hostName  = path2Host.get( path );
	if( hostName != null ) {
	  reachable = DeviceIO.isReachable( hostName, cache );
	}
	if( !reachable ) {
	  if( paths == null ) {
	    paths = new TreeSet<>();
	  }
	  paths.remove( path );
	}
      }
    }
    return paths;
  }


  public static Set<String> getNetPaths()
  {
    return getNetPaths( "/etc/fstab" );
  }


  protected static Set<String> getNetPaths( String fstab )
  {
    Map<String,String> path2Host = getNetPathToHostNameMap( fstab );
    return path2Host != null ? path2Host.keySet() : null;
  }


  public static DeviceIO.RandomAccessDevice openDeviceForRandomAccess(
				String  deviceName,
				boolean readOnly ) throws IOException
  {
    if( !readOnly ) {
      umountUnixDevice( deviceName );
    }
    return new UnixRandomAccessDevice(
		new RandomAccessFile( deviceName, readOnly ? "r" : "rw" ) );
  }


  public static InputStream openDeviceForSequentialRead(
				String deviceName ) throws IOException
  {
    return new FileInputStream( deviceName );
  }


  public static OutputStream openDeviceForSequentialWrite(
				String  deviceName ) throws IOException
  {
    umountUnixDevice( deviceName );
    return new FileOutputStream( deviceName );
  }


  protected static void umountDevice( String... cmdLine ) throws IOException
  {
    if( cmdLine != null ) {
      if( cmdLine.length >= 2 ) {
	String deviceName = cmdLine[ 0 ];
	String mountPoint = null;
	int    mountCount = 0;
	for( FileStore fs : FileSystems.getDefault().getFileStores() ) {
	  String driveName = fs.name();
	  if( driveName != null ) {
	    if( driveName.equals( deviceName ) ) {
	      if( mountCount > 0 ) {
		StringBuilder buf = new StringBuilder( 256 );
		buf.append( deviceName );
		buf.append( " ist mehrfach im Dateisystem eingeh\u00E4ngt."
			+ "\nBitte f\u00FChren Sie \'" );
		for( int i = 0; i < cmdLine.length; i++ ) {
		  if( i > 0 ) {
		    buf.append( '\u0020' );
		  }
		  buf.append( cmdLine[ i ] );
		}
		buf.append( "\' manuell aus!" );
		throw new IOException( buf.toString() );
	      }
	      mountCount++;
	      String s = fs.toString();
	      if( s != null ) {
		if( s.startsWith( "/" ) ) {
		  int idx = s.indexOf( " (" );
		  if( idx >= 0 ) {
		    mountPoint = s.substring( 0, idx );
		  }
		}
	      }
	    }
	  }
	}
	if( mountCount > 0 ) {
	  StringBuffer buf = new StringBuffer( 0x100 );
	  buf.append( deviceName );
	  buf.append( " konnte nicht aus dem Dateisystem"
			+ " ausgeh\u00E4ngt werden" );
	  int baseErrTextLen = buf.length();
	  buf.append( ":\n\n" );
	  int prefixErrTextLen = buf.length();

	  // umount-Kommando ausfuehren
	  int rv = 0;
	  try {
	    rv = ProcessExecuter.execAndWait( buf, cmdLine );
	  }
	  catch( IOException ex ) {
	    buf.setLength( prefixErrTextLen );
	    buf.append( ex.getMessage() );
	    throw new IOException( buf.toString() );
	  }
	  if( rv != 0 ) {
	    if( buf.length() == prefixErrTextLen ) {
	      buf.setLength( baseErrTextLen );
	    }
	    throw new IOException( buf.toString() );
	  }
	}
      }
    }
  }


  protected static void umountUnixDevice( String deviceName )
						throws IOException
  {
    umountDevice( "umount", deviceName );
  }


	/* --- private Methoden --- */

  private static Map<String,String> getNetPathToHostNameMap( String fstab )
  {
    Map<String,String> path2Host = null;
    File               file      = new File( fstab );
    if( file.canRead() ) {
      BufferedReader in = null;
      try {
	in = new BufferedReader(
			new InputStreamReader(
				new FileInputStream( file ) ) );
	String line = in.readLine();
	while( line != null ) {
	  if( line.startsWith( "//" ) ) {
	    try {
	      String[] items = line.split( "\u0020", 3 );
	      if( items != null ) {
		if( items.length >= 2 ) {
		  String shareName = items[ 0 ];
		  String path      = items[ 1 ];
		  if( shareName.startsWith( "//" ) && (shareName.length() > 2)
		      && path.startsWith( "/" ) && (path.length() > 1) )
		  {
		    if( !path.endsWith( File.separator ) ) {
		      path += File.separator;
		    }
		    String hostName = null;
		    int    pos      = shareName.indexOf( '/', 2 );
		    if( pos < 0 ) {
		      hostName = shareName.substring( 2 );
		    } else if( pos > 2 ) {
		      hostName = shareName.substring( 2, pos );
		    }
		    if( hostName != null ) {
		      if( path2Host == null ) {
			path2Host = new HashMap<>();
		      }
		      path2Host.put( path, hostName );
		    }
		  }
		}
	      }
	    }
	    catch( PatternSyntaxException ex ) {}
	  }
	  line = in.readLine();
	}
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilently( in );
      }
    }
    return path2Host;
  }


	/* --- Konstruktor --- */

  protected UnixDeviceIO()
  {
    // leer
  }
}
