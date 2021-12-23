/*
 * (c) 2019-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Zugriff auf physische Geraete unter MacOS X
 */

package jkcemu.base.deviceio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.base.DeviceIO;


public class MacDeviceIO extends UnixDeviceIO
{
  public static void addDrivesTo(
			List<DeviceIO.Drive> drives,
			DeviceIO.MediaType   requestedType )
  {
    /*
     * Zuerst werden die FileStores gelesen um zu erfahren,
     * welche gemounteten Geraete es gibt.
     * Ueblicherweise liefert auf einem Mac
     * die Methode FileStore.name() den Namen der Geraetedatei
     * und FileStore.toString() einen Infotext,
     * der mit dem Mount-Punkt beginnt und
     * mit dem in Klammern stehenden Namen der Geraetedatei endet.
     * Allerdings ist fuer die Klasse FileStore die Methode toString()
     * nicht als ueberschrieben dokumentiert,
     * so dass nicht wirklich sicher ist,
     * ob die Methode einen so aufgebauten Text liefert.
     *
     * Die Erkennung der optischen und Wechselspeichermedien
     * erfolgt anhand des Dateisystems.
     * Das ist zwar nicht wirklich sicher,
     * trifft aber mit einer gewissen Wahrscheinlichkeit zu.
     */
    Set<String>        driveNames     = new TreeSet<>();
    Map<String,String> driveName2Text = new HashMap<>();
    Map<String,String> driveName2Type = new HashMap<>();
    for( FileStore fs : FileSystems.getDefault().getFileStores() ) {
      String driveName = fs.name();
      String text      = fs.toString();
      if( (driveName != null) && (text != null) ) {
	driveNames.add( driveName );
	driveName2Text.put( driveName, text );
	String fsType = fs.type();
	if( fsType != null ) {
	  driveName2Type.put( driveName, fsType.toUpperCase() );
	}
      }
    }

    // Laufwerke suchen
    int diskNum = 1;		// Disk 0 ueberspringen
    for(;;) {
      String devName = String.format( "/dev/disk%d", diskNum );
      if( !(new File( devName )).exists() ) {
	break;
      }
      boolean mounted    = false;
      boolean optical    = false;
      boolean removable  = false;
      String  partPrefix = devName + "s";
      for( String driveName : driveNames ) {
	if( driveName.startsWith( partPrefix ) ) {
	  mounted = true;
	  String text = driveName2Text.get( driveName );
	  if( text != null ) {
	    if( text.startsWith( "/Volumes/" ) ) {
	      String fsType = driveName2Type.get( driveName );
	      if( fsType != null ) {
		if( fsType.startsWith( "CDFS" )
		    || fsType.startsWith( "UDF" )
		    || (fsType.indexOf( "9660" ) >= 0) )
		{
		  optical = true;
		}
		if( fsType.startsWith( "FAT" )
		    || fsType.startsWith( "EXFAT" ) 
		    || fsType.startsWith( "VFAT" ) 
		    || fsType.startsWith( "MSDOS" ) )
		{
		  removable = true;
		}
	      }
	    }
	  }
	}
      }
      if( !mounted || optical || removable ) {
	Boolean cdrom = null;
	if( optical ) {
	  cdrom = Boolean.TRUE;
	} else if( removable ) {
	  cdrom = Boolean.FALSE;
	}
	drives.add(
		new DeviceIO.Drive(
				devName,
				devName,
				cdrom,
				0L,
				DeviceIO.needsSpecialPrivileges(
						devName,
						requestedType ) ) );

	// Partitiionen suchen
	int partNum = 1;
	for(;;) {
	  String partName = String.format( "%s%d", partPrefix, partNum );
	  if( !(new File( partName )).exists() ) {
	    break;
	  }
	  drives.add(
		new DeviceIO.Drive(
				partName,
				partName,
				cdrom,
				0L,
				DeviceIO.needsSpecialPrivileges(
						partName,
						requestedType ) ) );
	  partNum++;
	}
      }
      diskNum++;
    }
  }


  public static DeviceIO.RandomAccessDevice openDeviceForRandomAccess(
				String  deviceName,
				boolean readOnly ) throws IOException
  {
    if( !readOnly ) {
      umountMacDevice( deviceName );
    }
    return new UnixRandomAccessDevice(
		new RandomAccessFile( deviceName, readOnly ? "r" : "rw" ) );
  }


  public static OutputStream openDeviceForSequentialWrite(
				String  deviceName ) throws IOException
  {
    umountMacDevice( deviceName );
    return new FileOutputStream( deviceName );
  }


	/* --- private Methoden --- */

  private static void umountMacDevice( String deviceName ) throws IOException
  {
    umountDevice( "diskutil", "unmount", deviceName );
  }


	/* --- Konstruktor --- */

  protected MacDeviceIO()
  {
    // leer
  }
}
