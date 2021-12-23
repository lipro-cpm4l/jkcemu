/*
 * (c) 2015-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verschieben von Dateien und Dateibaeumen
 */

package jkcemu.file;


import java.awt.Window;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;


public class FileMover extends AbstractFileWorker
{
  private Path                              dstPath;
  private Path                              curDir;
  private boolean                           followURLs;
  private ReplyOnExistingFileDlg.UserAction userAction;
  private Set<Path>                         failedDirs;


  /*
   * Der Paramater srcFiles ist absichtlich nicht Generics-maessig
   * typisiert, um auch die von
   * Transferable.getTransferData( DataFlavor.javaFileListFlavor )
   * zurueckgelieferte Collection hier problemlos uebergeben zu koennen.
   */
  public FileMover(
		Window                         owner,
		Collection<?>                  srcFiles,
		boolean                        followURLs,
		Path                           dstPath,
		PathListener                   pathListener,
		Collection<AbstractFileWorker> register ) throws IOException
  {
    super( owner, srcFiles, pathListener, register );
    checkSrcPathsAgainstDstPath(
		dstPath,
		srcFiles,
		"Ein Verzeichnis kann nicht in sich selbst hinein"
			+ " verschoben werden." );
    this.followURLs = followURLs;
    this.dstPath    = dstPath;
    this.curDir     = null;
    this.userAction = null;
    this.failedDirs = new TreeSet<>();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFailedMsg( String fileName )
  {
    String urlText = this.curURLText;
    return urlText != null ?
	("\'" + urlText + "\' kann nicht geladen werden.")
	: ("\'" + fileName + "\' kann nicht verschoben werden.");
  }


  @Override
  public String getProgressDlgTitle()
  {
    return "Verschieben";
  }


  @Override
  public String getUncompletedWorkMsg()
  {
    return "Es konnten nicht alle Dateien, Verzeichnisse bzw.\n"
			+ "symbolische Links verschoben werden.";
  }


  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    if( !this.cancelled && !this.failedDirs.contains( dir ) ) {
      try {
	Files.delete( dir );
      }
      catch( NoSuchFileException ex1 ) {}
      catch( IOException ex1 ) {
	String msg = dir.toString()
			+ ":\nDas Quellverzeichnis konnte nicht"
			+ " gel\u00F6scht werden.";
	String exMsg = ex1.getMessage();
	if( exMsg != null ) {
	  if( !exMsg.isEmpty() ) {
	    msg = msg + "\n\n" + exMsg;
	  }
	}
	handleError( dir, new IOException( msg ) );
      }
    }
    this.curDir = null;
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.CONTINUE;
    this.curDir        = dir;
    if( !this.cancelled ) {
      this.curPath = dir;
      Path dstDir  = resolveDst( dir );
      try {
	Files.copy(
		dir,
		dstDir,
		StandardCopyOption.COPY_ATTRIBUTES,
		LinkOption.NOFOLLOW_LINKS );
	pathPasted( dstDir );
      }
      catch( IOException ex ) {
	if( !(ex instanceof FileAlreadyExistsException)
	    || !Files.isDirectory( dstDir ) )
	{
	  handleError( dir, ex );
	  if( !this.cancelled ) {
	    rv = FileVisitResult.SKIP_SUBTREE;
	  }
	}
      }
    }
    return this.cancelled ? FileVisitResult.TERMINATE : rv;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    if( !this.cancelled ) {
      this.curPath = file;
      try {
	Path dstFile = null;
	URL  url     = null;
	if( this.followURLs ) {
	  try {
	    url = FileUtil.readInternetShortcutURL( file.toFile() );
	  }
	  catch( UnsupportedOperationException ex ) {}
	}
	if( url != null ) {
	  this.curURLText = url.toExternalForm();
	  String fileName = url.getFile();
	  if( fileName != null ) {
	    try {
	      fileName = URLDecoder.decode( fileName, "UTF-8" );
	    }
	    catch( UnsupportedEncodingException ex ) {}
	    int idx  = fileName.lastIndexOf( '/' );
	    if( idx >= 0 ) {
	      fileName = fileName.substring( idx + 1 );
	    }
	    if( !fileName.isEmpty() ) {
	      dstFile = this.dstPath.resolve( fileName );
	    }
	  }
	  if( dstFile == null ) {
	    throw new IOException( "URL \'" + this.curURLText
				+ "\'\nenth\u00E4lt keinen Dateinamen" );
	  }
	} else {
	  dstFile = resolveDst( file );
	}
	boolean forceReplace = false;
	boolean exists       = Files.exists(
					dstFile,
					LinkOption.NOFOLLOW_LINKS );
	if( exists ) {
	  if( !Files.isSameFile( dstFile, file ) ) {
	    Path renamedFile = null;
	    ReplyOnExistingFileDlg.UserAction action = this.userAction;
	    if( action != null ) {
	      switch( action ) {
		case REPLACE_ALL:
		case SKIP_ALL:
		  // leer: Aktion behalten -> Dialog nicht anzeigen
		  break;
		default:
		  // Aktion entfernen -> Dialog anzeigen
		  action = null;
	      }
	    }
	    if( action == null ) {
	      ReplyOnExistingFileDlg.Reply reply
			= ReplyOnExistingFileDlg.callDlg(
				this.owner,
				file,
				dstFile,
				ReplyOnExistingFileDlg.FileAction.MOVE,
				this.userAction );
	      this.userAction = reply.action;
	      action           = reply.action;
	      renamedFile      = reply.renamedPath;
	    }
	    switch( action ) {
	      case REPLACE:
	      case REPLACE_ALL:
		if( url != null ) {
		  download( url, dstFile );
		} else {
		  Files.move(
			file,
			dstFile,
			StandardCopyOption.REPLACE_EXISTING );
		}
		pathPasted( dstFile );
		break;
	      case SKIP:
	      case SKIP_ALL:
		// Quellverzeichnis nicht loeschen
		if( this.curDir != null ) {
		  this.failedDirs.add( this.curDir );
		}
		break;
	      case RENAME:
		if( renamedFile != null ) {
		  if( url != null ) {
		    download( url, renamedFile );
		  } else {
		    Files.move(
			file,
			renamedFile,
			StandardCopyOption.REPLACE_EXISTING );
		  }
		  pathPasted( dstFile );
		} else {
		  this.cancelled = true;
		}
		break;
	      default:
		this.cancelled = true;
	    }
	  }
	} else {
	  if( !this.cancelled ) {
	    if( url != null ) {
	      download( url, dstFile );
	    } else {
	      Files.move(
			file,
			dstFile,
			StandardCopyOption.REPLACE_EXISTING,
			LinkOption.NOFOLLOW_LINKS );
	    }
	    pathPasted( dstFile );
	  }
	}
      }
      catch( IOException ex ) {
	handleError( file, ex );
      }
    }
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


	/* --- private Methoden --- */

  private Path resolveDst( Path path )
  {
    Path parent = this.curBaseParent;
    if( parent != null ) {
      path = parent.relativize( path );
    }
    return this.dstPath.resolve( path );
  }
}
