/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Native Methoden fuer Windows
 *
 * Abhaengig vom verwendeten Compiler bzw. MinGW-Build
 * ist eins der folgenden beiden Includes notwendig
 *   #include <ntddstor.h>
 *   #include <ddk/ntddstor.h>
 */

#include <stdlib.h>
#include <windows.h>
#include <winioctl.h>
#include <winnetwk.h>
#include <ntddstor.h>
#include "deviceio.h"

#ifdef _WIN64
# define TO_JLONG(a) ((jlong) a)
# define TO_HANDLE(a) ((HANDLE) a)
#else
# define TO_JLONG(a) ((jlong) (long) a)
# define TO_HANDLE(a) ((HANDLE) (long) a)
#endif


void copyBytesTo(
		const char *src,
		JNIEnv     *env,
		jbyteArray  dst )
{
  if( dst != NULL ) {
    int len = (int) (*env)->GetArrayLength( env, dst );
    if( len > 0 ) {
      jbyte *a = (*env)->GetByteArrayElements( env, dst, NULL );
      if( a != NULL ) {
	jbyte *p = a;
	while( (len > 0) && (*src != 0) ) {
	  *p++ = *src++;
	  --len;
	}
	if( len > 0 ) {
	  *p++ = '\0';
	}
	(*env)->ReleaseByteArrayElements( env, dst, a, 0 );
      }
    }
  }
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_deleteDriveLayout(
						JNIEnv     *env,
						jclass      class,
						jlong       handle )
{
  DWORD rv   = -1;
  DWORD nRet = 0;
  if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_DISK_DELETE_DRIVE_LAYOUT,
		NULL,
		0,
		NULL,
		0,
		&nRet,
		NULL ) != 0 )
  {
    rv = 0;
  } else {
    rv = GetLastError();
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getDiskGeometry(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jlongArray  result )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, result ) >= 4 ) {
    DWORD         nRet = 0;
    DISK_GEOMETRY diskGeometry;
    if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_DISK_GET_DRIVE_GEOMETRY,
		NULL,
		0,
		&diskGeometry,
		sizeof( diskGeometry ),
		&nRet,
		NULL ) != 0 )
    {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (jlong) diskGeometry.Cylinders.QuadPart;
	a[ 1 ] = (jlong) diskGeometry.TracksPerCylinder;
	a[ 2 ] = (jlong) diskGeometry.SectorsPerTrack;
	a[ 3 ] = (jlong) diskGeometry.BytesPerSector;
	(*env)->ReleaseLongArrayElements( env, result, a, 0 );
	rv = 0;
      }
    } else {
      rv = GetLastError();
    }
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getDiskGeometryEx(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jlongArray  result )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, result ) >= 5 ) {
    DWORD            nRet = 0;
    DISK_GEOMETRY_EX diskGeomEx;
    if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_DISK_GET_DRIVE_GEOMETRY_EX,
		NULL,
		0,
		&diskGeomEx,
		sizeof( diskGeomEx ),
		&nRet,
		NULL ) != 0 )
    {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (jlong) diskGeomEx.Geometry.Cylinders.QuadPart;
	a[ 1 ] = (jlong) diskGeomEx.Geometry.TracksPerCylinder;
	a[ 2 ] = (jlong) diskGeomEx.Geometry.SectorsPerTrack;
	a[ 3 ] = (jlong) diskGeomEx.Geometry.BytesPerSector;
	a[ 4 ] = (jlong) diskGeomEx.DiskSize.QuadPart;
	(*env)->ReleaseLongArrayElements( env, result, a, 0 );
	rv = 0;
      }
    } else {
      rv = GetLastError();
    }
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getDriveType(
						JNIEnv     *env,
						jclass      class,
						jstring     drvName )
{
  UINT        rv       = 0;
  const char *cDrvName = (*env)->GetStringUTFChars( env, drvName, NULL );
  if( cDrvName != NULL ) {
    rv = GetDriveTypeA( cDrvName );
    (*env)->ReleaseStringUTFChars( env, drvName, cDrvName );
  }
  return (jint) rv;
}


JNIEXPORT
jstring JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getErrorMsg(
						JNIEnv     *env,
						jclass      class,
						jint        errCode )
{
  jstring rv = NULL;
  LPVOID  msg;

  FormatMessage(
	FORMAT_MESSAGE_ALLOCATE_BUFFER
		| FORMAT_MESSAGE_FROM_SYSTEM
		| FORMAT_MESSAGE_IGNORE_INSERTS,
	NULL,
	(DWORD) errCode,
	MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
	(LPTSTR) &msg,
	0,
	NULL );

  if( msg != NULL ) {
    rv = (*env)->NewStringUTF( env, (const char *) msg );
    LocalFree( msg );
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getHotplugInfo(
						JNIEnv        *env,
						jclass         class,
						jlong          handle,
						jbooleanArray  result )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, result ) >= 3 ) {
    DWORD                nRet = 0;
    STORAGE_HOTPLUG_INFO hotplugInfo;
    hotplugInfo.Size = sizeof( hotplugInfo );
    if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_STORAGE_GET_HOTPLUG_INFO,
		NULL, 0,
		&hotplugInfo,
		sizeof( hotplugInfo ),
		&nRet,
		NULL ) != 0 )
    {
      jboolean *a = (*env)->GetBooleanArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (jboolean) hotplugInfo.MediaRemovable;
	a[ 1 ] = (jboolean) hotplugInfo.MediaHotplug;
	a[ 2 ] = (jboolean) hotplugInfo.DeviceHotplug;
	(*env)->ReleaseBooleanArrayElements( env, result, a, 0 );
	rv = 0;
      }
    } else {
      rv = GetLastError();
    }
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getJoystickBounds(
						JNIEnv     *env,
						jclass      class,
						jint        joyNum,
						jlongArray  result )
{
  DWORD   rv = -1;
  JOYCAPS joyCaps;
  if( (*env)->GetArrayLength( env, result ) >= 4 ) {
    if( joyGetDevCaps(
		joyNum,
		&joyCaps,
		sizeof( joyCaps ) ) == JOYERR_NOERROR )
    {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (jlong) joyCaps.wXmin;
	a[ 1 ] = (jlong) joyCaps.wXmax;
	a[ 2 ] = (jlong) joyCaps.wYmin;
	a[ 3 ] = (jlong) joyCaps.wYmax;
	(*env)->ReleaseLongArrayElements( env, result, a, 0 );
	rv = 0;
      }
    }
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getJoystickPos(
						JNIEnv     *env,
						jclass      class,
						jint        joyNum,
						jlongArray  result )
{
  DWORD   rv = -1;
  JOYINFO joyInfo;
  if( (*env)->GetArrayLength( env, result ) >= 3 ) {
    if( joyGetPos( joyNum, &joyInfo ) == JOYERR_NOERROR ) {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (jlong) joyInfo.wButtons;
	a[ 1 ] = (jlong) joyInfo.wXpos;
	a[ 2 ] = (jlong) joyInfo.wYpos;
	(*env)->ReleaseLongArrayElements( env, result, a, 0 );
	rv = 0;
      }
    }
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getLibVersion(
						JNIEnv     *env,
						jclass      class )
{
  return (jint) 2;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getLogicalDrives(
						JNIEnv     *env,
						jclass      class )
{
  return (jint) GetLogicalDrives();
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getNetConnection(
					JNIEnv     *env,
					jclass      class,
					jstring     devName,
					jbyteArray  remoteNameOut )
{
  DWORD rv = -1;

  const char *cDevName = (*env)->GetStringUTFChars( env, devName, NULL );
  if( cDevName != NULL ) {
    char  remoteNameBuf[ PATH_MAX ];
    DWORD remoteNameBufSize = sizeof( remoteNameBuf );

    rv = WNetGetConnectionA(
			cDevName,
			remoteNameBuf,
			&remoteNameBufSize );
    if( rv == NO_ERROR ) {
      if( remoteNameBuf != NULL ) {
	copyBytesTo( remoteNameBuf, env, remoteNameOut );
      }
    }
    (*env)->ReleaseStringUTFChars( env, devName, cDevName );
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getPartitionStyle(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jlongArray  result )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, result ) >= 2 ) {
    DWORD nRet = 0;
    BYTE  retBuf[ sizeof( DRIVE_LAYOUT_INFORMATION_EX ) + 4096 ];
    if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_DISK_GET_DRIVE_LAYOUT_EX,
		NULL, 0,
		&retBuf,
		sizeof( retBuf ),
		&nRet,
		NULL ) != 0 )
    {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	DRIVE_LAYOUT_INFORMATION_EX *p
			= (DRIVE_LAYOUT_INFORMATION_EX *) &retBuf;
	DWORD partCnt = p->PartitionCount;
	if( p->PartitionStyle == PARTITION_STYLE_MBR ) {
	  DWORD                     nUsed  = 0;
	  PARTITION_INFORMATION_EX *pEntry = &p->PartitionEntry[ 0 ];
	  while( partCnt > 0 ) {
	    if( pEntry->Mbr.PartitionType != PARTITION_ENTRY_UNUSED ) {
	      nUsed++;
	    }
	    pEntry++;
	    --partCnt;
	  }
	  partCnt = nUsed;
	}
	a[ 0 ] = p->PartitionStyle;
	a[ 1 ] = partCnt;
	(*env)->ReleaseLongArrayElements( env, result, a, 0 );
	rv = 0;
      }
    } else {
      rv = GetLastError();
    }
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getPartitionInfo(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jlongArray  result )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, result ) >= 2 ) {
    DWORD                    nRet = 0;
    PARTITION_INFORMATION_EX partInfo;
    if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_DISK_GET_PARTITION_INFO_EX,
		NULL, 0,
		&partInfo,
		sizeof( partInfo ),
		&nRet,
		NULL ) != 0 )
    {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (jlong) partInfo.PartitionNumber;
	a[ 1 ] = (jlong) partInfo.PartitionLength.QuadPart;
	(*env)->ReleaseLongArrayElements( env, result, a, 0 );
	rv = 0;
      }
    } else {
      rv = GetLastError();
    }
 }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getPhysicalDriveNum(
						JNIEnv     *env,
						jclass      class,
						jlong       handle )
{
  DWORD               rv   = -1;
  DWORD               nRet = 0;
  VOLUME_DISK_EXTENTS diskExtends;
  if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_VOLUME_GET_VOLUME_DISK_EXTENTS,
		NULL, 0,
		&diskExtends,
		sizeof( diskExtends ),
		&nRet,
		NULL ) != 0 )
  {
    if( diskExtends.NumberOfDiskExtents == 1 ) {
      rv = diskExtends.Extents[ 0 ].DiskNumber;
    }
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getShortPathName(
						JNIEnv     *env,
						jclass      class,
						jcharArray  buf )
{
  DWORD rv      = -1;
  DWORD bufSize = (DWORD) (*env)->GetArrayLength( env, buf );
  if( bufSize >= 2 ) {
    wchar_t *tmpBuf = (*env)->GetCharArrayElements( env, buf, NULL );
    rv = GetShortPathNameW( tmpBuf, tmpBuf, bufSize - 1 );
    (*env)->ReleaseCharArrayElements( env, buf, tmpBuf, 0 );
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getStorageDeviceInfo(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jbyteArray  usbOut,
						jbyteArray  vendorIdOut,
						jbyteArray  productIdOut )
{
  DWORD rv   = 0;
  DWORD nRet = 0;
  BYTE  retBuf[ sizeof( STORAGE_DEVICE_DESCRIPTOR ) + 4096 ];

  STORAGE_PROPERTY_QUERY query;
  query.PropertyId              = StorageDeviceProperty;
  query.QueryType               = PropertyStandardQuery;
  query.AdditionalParameters[0] = 0;

  if( DeviceIoControl(
		TO_HANDLE( handle ),
		IOCTL_STORAGE_QUERY_PROPERTY,
		&query, sizeof( query ),
		&retBuf,
		sizeof( retBuf ),
		&nRet,
		NULL ) != 0 )
  {
    STORAGE_DEVICE_DESCRIPTOR *p = (STORAGE_DEVICE_DESCRIPTOR *) &retBuf;

    if( (*env)->GetArrayLength( env, usbOut ) >= 1 ) {
      char *usbBuf = (*env)->GetByteArrayElements( env, usbOut, NULL );
      if( usbBuf != NULL ) {
	usbBuf[ 0 ] = (jbyte) (p->BusType == BusTypeUsb ? 1 : 0);
	(*env)->ReleaseByteArrayElements( env, usbOut, usbBuf, 0 );
      }
    }

    /*
     * Die einzelnen Textabschnitte sind nicht immer mit Null terminiert.
     * Aus diesem Grund wird dies temporaer getan.
     */
    char *pVendor    = (char *) p + p->VendorIdOffset;
    char *pProduct   = (char *) p + p->ProductIdOffset;
    char *pRevision  = (char *) p + p->ProductRevisionOffset;
    char *pSerialNum = (char *) p + p->SerialNumberOffset;

    *pRevision  = '\0';
    *pSerialNum = '\0';

    char cTmp = *pProduct;
    *pProduct = '\0';
    copyBytesTo( pVendor, env, vendorIdOut );

    *pProduct = cTmp;
    *pVendor = '\0';
    copyBytesTo( pProduct, env, productIdOut );
  } else {
    rv = GetLastError();
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_getVolumeInfo(
					JNIEnv     *env,
					jclass      class,
					jstring     rootPathName,
					jbyteArray  volumeNameOut )
{
  DWORD rv = -1;

  const char *cRootPathName = (*env)->GetStringUTFChars(
							env,
							rootPathName,
							NULL );
  if( cRootPathName != NULL ) {
    char  volumeNameBuf[ PATH_MAX ];
    char  fileSysNameBuf[ PATH_MAX ];
    DWORD maxComponentLen = 0;
    DWORD fileSysFlags    = 0;

    if( GetVolumeInformationA(
			cRootPathName,
			volumeNameBuf,
			sizeof( volumeNameBuf ),
			NULL,
			&maxComponentLen,
			&fileSysFlags,
			fileSysNameBuf,
			sizeof( fileSysNameBuf ) ) != 0 )
    {
      if( volumeNameBuf != NULL ) {
	copyBytesTo( volumeNameBuf, env, volumeNameOut );
      }
      rv = 0;
    } else {
      rv = GetLastError();
    }
    (*env)->ReleaseStringUTFChars( env, rootPathName, cRootPathName );
  }
  return rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_openDevice(
						JNIEnv     *env,
						jclass      class,
						jstring     devName,
						jboolean    readAccess,
						jboolean    writeAccess,
						jboolean    randomAccess,
						jlongArray  handleOut )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, handleOut ) >= 1 ) {
    const char *cDevName = (*env)->GetStringUTFChars( env, devName, NULL );
    if( cDevName != NULL ) {
      HANDLE handle     = (HANDLE) 0;
      DWORD  accessMode = 0;
      DWORD  flags      = 0;
      if( readAccess != 0 ) {
	accessMode = GENERIC_READ;
      }
      if( writeAccess != 0 ) {
	accessMode |= GENERIC_WRITE;
      }
      if( ((readAccess != 0) || (writeAccess != 0))
	  && (randomAccess == 0) )
      {
	flags = FILE_FLAG_SEQUENTIAL_SCAN;
      }
      handle = CreateFileA(
			cDevName,
			accessMode,
			FILE_SHARE_READ | FILE_SHARE_WRITE,
			NULL,		/* security attributes */
			OPEN_EXISTING,
			flags,
			NULL );
      if( handle == INVALID_HANDLE_VALUE ) {
	rv = GetLastError();
	if( rv == 0 ) {
	  rv = -1;
	}
      } else {
	jlong tmpHandle = TO_JLONG( handle );
	(*env)->SetLongArrayRegion( env, handleOut, 0, 1, &tmpHandle );
	rv = 0;
      }
      (*env)->ReleaseStringUTFChars( env, devName, cDevName );
    }
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_readDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jbyteArray  buf,
						jint        offs,
						jint        len,
						jintArray   lenOut )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, lenOut ) >= 1 ) {
    jint size = (jint) (*env)->GetArrayLength( env, buf );
    if( size < (offs + len) ) {
      len = size - offs;
    }
    if( (offs >= 0) && (len > 0) ) {
      char *tmpBuf = (*env)->GetByteArrayElements( env, buf, NULL );
      if( tmpBuf != NULL ) {
	DWORD nRead = 0;
	if( ReadFile(
		TO_HANDLE( handle ),
		&tmpBuf[ offs ],
		len,
		&nRead,
		NULL ) != 0 )
	{
	  jint nTmp = nRead;
	  (*env)->SetIntArrayRegion( env, lenOut, 0, 1, &nTmp );
	  rv = 0;
	} else {
	  rv = GetLastError();
	}
	/* Zurueckkopieren und freigeben */
	(*env)->ReleaseByteArrayElements( env, buf, (jbyte *) tmpBuf, 0 );
      }
    }
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_writeDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jbyteArray  buf,
						jint        offs,
						jint        len,
						jintArray   lenOut )
{
  DWORD rv = -1;
  if( (*env)->GetArrayLength( env, lenOut ) >= 1 ) {
    jint size = (jint) (*env)->GetArrayLength( env, buf );
    if( size < (offs + len) ) {
      len = size - offs;
    }
    if( (offs >= 0) && (len > 0) ) {
      const char *tmpBuf = (*env)->GetByteArrayElements( env, buf, NULL );
      if( tmpBuf != NULL ) {
	DWORD nWritten = 0;
	if( WriteFile(
		TO_HANDLE( handle ),
		&tmpBuf[ offs ],
		len,
		&nWritten,
		NULL ) != 0 )
	{
	  jint nTmp = (jint) nWritten;
	  (*env)->SetIntArrayRegion( env, lenOut, 0, 1, &nTmp );
	  rv = 0;
	} else {
	  rv = GetLastError();
	}
	(*env)->ReleaseByteArrayElements( env, buf, (jbyte *) tmpBuf, 0 );
      }
    }
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_closeDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle )
{
  DWORD nRet = 0;

  DeviceIoControl(
		TO_HANDLE( handle ),
		FSCTL_UNLOCK_VOLUME,
		NULL,
		0,
		NULL,
		0,
		&nRet,
		NULL );
  return (jint) (CloseHandle( TO_HANDLE( handle ) ) != 0 ?
						0
						: GetLastError());
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_flushDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle )
{
  return (jint) FlushFileBuffers(
			TO_HANDLE( handle ) ) != 0 ? 0 : GetLastError();
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_lockDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle )
{
  DWORD rv   = 0;
  DWORD nRet = 0;

  if( DeviceIoControl(
		TO_HANDLE( handle ),
		FSCTL_LOCK_VOLUME,
		NULL,
		0,
		NULL,
		0,
		&nRet,
		NULL ) == 0 )
  {
    rv = GetLastError();
  }
  return (jint) rv;
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_seekDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jlong       pos )
{
  return (jint) SetFilePointerEx(
			TO_HANDLE( handle ),
			(LARGE_INTEGER) pos,
			NULL,
			FILE_BEGIN ) != 0 ? 0 : GetLastError();
}


JNIEXPORT
jint JNICALL Java_jkcemu_base_deviceio_WinDeviceIO_dismountDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle )
{
  DWORD rv   = 0;
  DWORD nRet = 0;

  if( DeviceIoControl(
		TO_HANDLE( handle ),
		FSCTL_DISMOUNT_VOLUME,
		NULL,
		0,
		NULL,
		0,
		&nRet,
		NULL ) == 0 )
  {
    rv = GetLastError();
    if( rv == ERROR_NOT_READY ) {	/* bereits dismounted */
      rv = 0;
    }
  }
  return (jint) rv;
}


/*
 * alte Funktionen bis JKCEMU 0.9.7
 *
 * Die alten Funktionen sind aus Gruenden der Kompatibilitaet enthalten,
 * so dass die DLL auch mit alten JKCEMU-Versionen verwendet werden kann.
 * Damit sollen Probleme mit der DLL verhindert werden,
 * die bei abwechselnder Verwendung von alten und neuen JKCEMU-Versionen
 * entstehen koennten.
 */

JNIEXPORT jstring JNICALL Java_jkcemu_base_DeviceIO_getErrorMsg(
						JNIEnv *env,
						jclass  class,
						jint    errCode )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_getErrorMsg(
						env,
						class,
						errCode );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_getJoystickBounds(
						JNIEnv     *env,
						jclass      class,
						jint        joyNum,
						jlongArray  result )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_getJoystickBounds(
						env,
						class,
						joyNum,
						result );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_getJoystickPos(
						JNIEnv     *env,
						jclass      class,
						jint        joyNum,
						jlongArray  result )
{
 return Java_jkcemu_base_deviceio_WinDeviceIO_getJoystickPos(
						env,
						class,
						joyNum,
						result );
}



JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_openDevice(
						JNIEnv     *env,
						jclass      class,
						jstring     devName,
						jboolean    readOnly,
						jboolean    randomAccess,
						jlongArray  handleOut )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_openDevice(
						env,
						class,
						devName,
						(jboolean) TRUE,
						(jboolean) !readOnly,
						randomAccess,
						handleOut );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_closeDevice(
						JNIEnv  *env,
						jclass   class,
						jlong    handle )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_closeDevice(
						env,
						class,
						handle );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_flushDevice(
						JNIEnv  *env,
						jclass   class,
						jlong    handle )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_flushDevice(
						env,
						class,
						handle );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_seekDevice(
						JNIEnv  *env,
						jclass   class,
						jlong    handle,
						jlong    pos )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_seekDevice(
						env,
						class,
						handle,
						pos );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_readDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jbyteArray  buf,
						jint        offs,
						jint        len,
						jintArray   lenOut )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_readDevice(
						env,
						class,
						handle,
						buf,
						offs,
						len,
						lenOut );
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_writeDevice(
						JNIEnv     *env,
						jclass      class,
						jlong       handle,
						jbyteArray  buf,
						jint        offs,
						jint        len,
						jintArray   lenOut )
{
  return Java_jkcemu_base_deviceio_WinDeviceIO_writeDevice(
						env,
						class,
						handle,
						buf,
						offs,
						len,
						lenOut );
}
