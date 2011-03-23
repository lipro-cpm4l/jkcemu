/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Native Methoden fuer Windows
 */

#include <stdlib.h>
#include <windows.h>
#include "deviceio.h"

#ifdef _WIN64
# define TO_JLONG(a) ((jlong) a)
# define TO_HANDLE(a) ((HANDLE) a)
#else
# define TO_JLONG(a) ((jlong) (long) a)
# define TO_HANDLE(a) ((HANDLE) (long) a)
#endif


JNIEXPORT jstring JNICALL Java_jkcemu_base_DeviceIO_getErrorMsg(
						JNIEnv *env,
						jclass  class,
						jint    errCode )
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


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_getJoystickBounds(
						JNIEnv     *env,
						jclass      class,
						jint        joyNum,
						jlongArray  result )
{
  jint    rv = -1;
  JOYCAPS joyCaps;
  if( (*env)->GetArrayLength( env, result ) >= 4 ) {
    if( joyGetDevCaps(
		joyNum,
		&joyCaps,
		sizeof( joyCaps ) ) == JOYERR_NOERROR )
    {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (unsigned long) joyCaps.wXmin;
	a[ 1 ] = (unsigned long) joyCaps.wXmax;
	a[ 2 ] = (unsigned long) joyCaps.wYmin;
	a[ 3 ] = (unsigned long) joyCaps.wYmax;
	(*env)->ReleaseLongArrayElements( env, result, (jlong *) a, 0 );
	rv = 0;
      } else {
	rv = GetLastError();
      }
    }
  }
  return rv;
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_getJoystickPos(
						JNIEnv     *env,
						jclass      class,
						jint        joyNum,
						jlongArray  result )
{
  jint    rv = -1;
  JOYINFO joyInfo;
  if( (*env)->GetArrayLength( env, result ) >= 3 ) {
    if( joyGetPos( joyNum, &joyInfo ) == JOYERR_NOERROR ) {
      jlong *a = (*env)->GetLongArrayElements( env, result, NULL );
      if( a != NULL ) {
	a[ 0 ] = (unsigned long) joyInfo.wButtons;
	a[ 1 ] = (unsigned long) joyInfo.wXpos;
	a[ 2 ] = (unsigned long) joyInfo.wYpos;
	(*env)->ReleaseLongArrayElements( env, result, (jlong *) a, 0 );
	rv = 0;
      } else {
	rv = GetLastError();
      }
    }
  }
  return rv;
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_openDevice(
						JNIEnv     *env,
						jclass      class,
						jstring     devName,
						jboolean    readOnly,
						jboolean    randomAccess,
						jlongArray  handleOut )
{
  jint rv = -1;
  if( (*env)->GetArrayLength( env, handleOut ) >= 1 ) {
    const char *cDevName = (*env)->GetStringUTFChars( env, devName, NULL );
    if( cDevName != NULL ) {
      HANDLE handle     = (HANDLE) 0;
      DWORD  accessMode = 0;
      DWORD  shareMode  = 0;
      DWORD  flags      = 0;
      if( readOnly != 0 ) {
	accessMode = GENERIC_READ;
	shareMode  = FILE_SHARE_READ;
	if( randomAccess == 0 ) {
	  flags = FILE_FLAG_SEQUENTIAL_SCAN;
	}
      } else {
	accessMode = GENERIC_WRITE;
	shareMode  = FILE_SHARE_WRITE;
	if( randomAccess != 0 ) {
	  accessMode |= GENERIC_READ;
	  shareMode  |= FILE_SHARE_READ;
	  flags      = FILE_FLAG_RANDOM_ACCESS;
	}
      }
      handle = CreateFile(
			cDevName,
			accessMode,
			shareMode,
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
  return rv;
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_closeDevice(
				JNIEnv  *env,
				jclass   class,
				jlong    handle )
{
  return CloseHandle( TO_HANDLE( handle ) ) == TRUE ? 0 : GetLastError();
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_flushDevice(
				JNIEnv  *env,
				jclass   class,
				jlong    handle )
{
  return FlushFileBuffers(
		TO_HANDLE( handle ) ) == TRUE ? 0 : GetLastError();
}


JNIEXPORT jint JNICALL Java_jkcemu_base_DeviceIO_seekDevice(
						JNIEnv  *env,
						jclass   class,
						jlong    handle,
						jlong    pos )
{
  return SetFilePointerEx(
		TO_HANDLE( handle ),
		(LARGE_INTEGER) pos,
		NULL,
		FILE_BEGIN ) == TRUE ? 0 : GetLastError();
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
  jint rv = -1;
  if( (*env)->GetArrayLength( env, lenOut ) >= 1 ) {
    int size = (*env)->GetArrayLength( env, buf );
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
		NULL ) == TRUE )
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
  return rv;
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
  jint rv = -1;
  if( (*env)->GetArrayLength( env, lenOut ) >= 1 ) {
    int size = (*env)->GetArrayLength( env, buf );
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
		NULL ) == TRUE )
	{
	  jint nTmp = nWritten;
	  (*env)->SetIntArrayRegion( env, lenOut, 0, 1, &nTmp );
	  rv = 0;
	} else {
	  rv = GetLastError();
	}
	(*env)->ReleaseByteArrayElements( env, buf, (jbyte *) tmpBuf, 0 );
      }
    }
  }
  return rv;
}
