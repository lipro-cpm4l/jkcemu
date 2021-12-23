/*
 * (c) 2017-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Rekursives Setzen des letzten Aenderungszeitpunktes
 */

package jkcemu.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jkcemu.base.EmuUtil;


public class LastModifiedSetter implements FileVisitor<Path>
{
  private static final String SCHEME_JKCEMU_TAR = "jkcemu:tar";

  private volatile static Map<String,String> suffix2Scheme = null;

  private Path                    path;
  private FileTime                fileTime;
  private Set<String>             archiveFileSuffixs;
  private boolean                 recursive;
  private volatile boolean        cancelled;
  private Map<Path,String>        vfsFile2Scheme;
  private int                     numFailed;
  private int                     numTouched;
  private volatile TarFileUpdater tarFileUpdater;


  public LastModifiedSetter(
			Path        path,
			FileTime    fileTime,
			boolean     recursive,
			Set<String> archiveFileSuffixs )
 {
    this.path               = path;
    this.fileTime           = fileTime;
    this.recursive          = recursive;
    this.archiveFileSuffixs = archiveFileSuffixs;
    this.vfsFile2Scheme     = new HashMap<>();
    this.cancelled          = false;
    this.numFailed          = 0;
    this.numTouched         = 0;
    if( this.archiveFileSuffixs != null ) {
      if( !this.archiveFileSuffixs.isEmpty() ) {
	if( getVfsSuffix2SchemeMap().isEmpty() ) {
	  this.archiveFileSuffixs = null;
	}
      }
    }
  }


  public void cancel()
  {
    TarFileUpdater updater = this.tarFileUpdater;
    if( updater != null ) {
      updater.cancel();
    }
    this.cancelled = true;
  }


  public void exec() throws IOException
  {
    if( this.recursive
	&& Files.isDirectory( this.path, LinkOption.NOFOLLOW_LINKS ) )
    {
      Files.walkFileTree( this.path, this );
    } else {
      if( !checkAddVfsFile( this.path ) ) {
	setLastModified( this.path );
      }
    }
    for( Path path : this.vfsFile2Scheme.keySet() ) {
      if( this.cancelled ) {
	break;
      }
      String scheme = this.vfsFile2Scheme.get( path );
      if( scheme != null ) {
	if( scheme.equals( SCHEME_JKCEMU_TAR ) ) {
	  updTarFile( path );
	} else {
	  FileTime parentTime = null;
	  Path     parentPath = path.getParent();
	  if( parentPath != null ) {
	    try {
	      parentTime = Files.getLastModifiedTime(
					parentPath,
					LinkOption.NOFOLLOW_LINKS );
	    }
	    catch( IOException ex ) {}
	  }
	  FileSystem vfs = null;
	  try {
	    Map<String,String> options = new HashMap<>();
	    options.put( "create", "false" );
	    vfs = FileSystems.newFileSystem(
			URI.create( scheme + ":" + path.toUri().toString() ),
			options );
	    (new LastModifiedSetter(
				vfs.getPath( "/" ),
				this.fileTime,
				true,
				null )).exec();
	  }
	  catch( Exception ex ) {}
	  finally {
	    EmuUtil.closeSilently( vfs );
	    if( (parentPath != null) && (parentTime != null) ) {
	      try {
		Files.setLastModifiedTime( parentPath, parentTime );
	      }
	      catch( IOException ex ) {}
	    }
	  }
	}
      }
      setLastModified( path );
    }
  }


  public int getNumTouched()
  {
    return this.numTouched;
  }


  public int getNumFailed()
  {
    return this.numFailed;
  }


  public static Map<String,String> getVfsSuffix2SchemeMap()
  {
    if( suffix2Scheme == null ) {
      Map<String,String> map = new HashMap<>();
      for( FileSystemProvider p : FileSystemProvider.installedProviders() ) {
	String s = p.getScheme();
	if( s != null ) {
	  s = s.toLowerCase();
	  if( !s.equals( "file" ) ) {
	    map.put( s, s );
	  }
	}
      }
      if( map.containsKey( "jar" ) && !map.containsKey( "zip" ) ) {
	map.put( "zip", "jar" );
      }
      if( !map.containsKey( "tar" ) ) {
	map.put( "tar", SCHEME_JKCEMU_TAR );
      }
      suffix2Scheme = map;
    }
    return suffix2Scheme;
  }


	/* --- FileVisitor --- */

  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    FileVisitResult rv = FileVisitResult.TERMINATE;
    if( !this.cancelled ) {
      setLastModified( dir );
      rv = FileVisitResult.CONTINUE;
    }
    return rv;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    return this.cancelled ?
		FileVisitResult.TERMINATE
		: FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.TERMINATE;
    if( !this.cancelled ) {
      if( !checkAddVfsFile( file ) ) {
	setLastModified( file );
      }
      rv = FileVisitResult.CONTINUE;
    }
    return rv;
  }


  @Override
  public FileVisitResult visitFileFailed( Path file, IOException ex )
  {
    this.numFailed++;
    return this.cancelled ?
		FileVisitResult.TERMINATE
		: FileVisitResult.CONTINUE;
  }


	/* --- private Methoden --- */

  private boolean checkAddVfsFile( Path path )
  {
    boolean rv = false;
    if( this.archiveFileSuffixs != null ) {
      if( !this.archiveFileSuffixs.isEmpty() ) {
	String suffix = FileUtil.getLowerFileSuffix( path );
	if( suffix != null ) {
	  if( this.archiveFileSuffixs.contains( suffix ) ) {
	    String scheme = getVfsSuffix2SchemeMap().get( suffix );
	    if( scheme != null ) {
	      this.vfsFile2Scheme.put( path, scheme );
	      rv = true;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void setLastModified( Path path )
  {
    if( (this.fileTime != null) && (path != null) ) {
      try {
	Files.setLastModifiedTime( path, this.fileTime );
	this.numTouched++;
      }
      catch( NoSuchFileException ex ) {
	/*
	 * Wenn eine Datei nicht mehr existiert,
	 * soll dafuer kein Fehler gezaehlt werden.
	 */
      }
      catch( Exception ex ) {
	this.numFailed++;
      }
    }
  }


  private void updTarFile( Path path )
  {
    try {
      this.tarFileUpdater = new TarFileUpdater( path.toFile() );
      this.tarFileUpdater.updateTimeOfAllEntries( this.fileTime.toMillis() );
    }
    catch( IOException ex ) {}
    finally {
      EmuUtil.closeSilently( this.tarFileUpdater );
      this.tarFileUpdater = null;
    }
  }
}
