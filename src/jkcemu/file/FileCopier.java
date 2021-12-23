/*
 * (c) 2014-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kopieren von Dateien und Dateibaeumen
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import javax.swing.JOptionPane;
import jkcemu.base.EmuUtil;


public class FileCopier extends AbstractFileWorker
{
  private static final String OPTION_COPY_ALL = "Alle kopieren";

  private Path                              dstPath;
  private boolean                           followURLs;
  private ReplyOnExistingFileDlg.UserAction userAction;
  private volatile Boolean                  copyAllDirs;
  private volatile Boolean                  replaceAllFiles;


  /*
   * Der Paramater srcFiles ist absichtlich nicht Generics-maessig
   * typisiert, um auch die von
   * Transferable.getTransferData( DataFlavor.javaFileListFlavor )
   * zurueckgelieferte Collection hier problemlos uebergeben zu koennen.
   */
  public FileCopier(
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
			+ " kopiert werden." );
    this.followURLs      = followURLs;
    this.dstPath         = dstPath;
    this.userAction      = null;
    this.copyAllDirs     = null;
    this.replaceAllFiles = null;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFailedMsg( String fileName )
  {
    String urlText = this.curURLText;
    return urlText != null ?
	("\'" + urlText + "\' kann nicht geladen werden.")
	: ("\'" + fileName + "\' kann nicht kopiert werden.");
  }


  @Override
  public String getProgressDlgTitle()
  {
    return EmuUtil.TEXT_COPY;
  }


  @Override
  public String getUncompletedWorkMsg()
  {
    return "Es konnten nicht alle Dateien, Verzeichnisse bzw.\n"
			+ "symbolische Links kopiert werden.";
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.CONTINUE;
    if( !this.cancelled ) {
      this.curPath = dir;
      Path dstDir  = resolveDst( dir );
      try {
	boolean exists = Files.exists( dstDir, LinkOption.NOFOLLOW_LINKS );
	if( exists ) {
	  if( Files.isSameFile( dstDir, dir ) ) {
	    rv = FileVisitResult.SKIP_SUBTREE;
	  }
	}
	if( rv == FileVisitResult.CONTINUE ) {
	  boolean forceCopy = false;
	  if( this.copyAllDirs != null ) {
	    forceCopy = this.copyAllDirs.booleanValue();
	  }
	  if( (this.copyAllDirs == null) && exists ) {
	    switch( showJOptionPane(
			"Das Verzeichnis \'" + dstDir.toString()
				+ "\' existiert bereits.\n"
				+ "M\u00F6chten Sie trotzdem in das"
				+ " Verzeichnis hinein kopieren?",
			JOptionPane.WARNING_MESSAGE,
			"Zielverzeichnis bereits vorhanden",
			new String[] {
				EmuUtil.TEXT_COPY,
				OPTION_COPY_ALL,
				OPTION_SKIP,
				OPTION_SKIP_ALL,
				EmuUtil.TEXT_CANCEL } ) )
	    {
	      case EmuUtil.TEXT_COPY:
		forceCopy = true;
		break;
	      case OPTION_COPY_ALL:
		forceCopy        = true;
		this.copyAllDirs = Boolean.TRUE;
		break;
	      case OPTION_SKIP:
		forceCopy = false;
		break;
	      case OPTION_SKIP_ALL:
		forceCopy        = false;
		this.copyAllDirs = Boolean.FALSE;
		break;
	      default:
		this.cancelled = true;
	    }
	  }
	  if( !this.cancelled && (!exists || forceCopy) ) {
	    Files.copy(
		dir,
		dstDir,
		StandardCopyOption.COPY_ATTRIBUTES,
		LinkOption.NOFOLLOW_LINKS );
	    pathPasted( dstDir );
	  } else {
	    rv = FileVisitResult.SKIP_SUBTREE;
	  }
	}
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
	  if( Files.isSameFile( dstFile, file ) ) {
	    dstFile = createPathCopy( dstFile );
	    if( url != null ) {
	      download( url, dstFile );
	    } else {
	      Files.copy(
			file,
			dstFile,
			StandardCopyOption.COPY_ATTRIBUTES,
			LinkOption.NOFOLLOW_LINKS );
	    }
	    pathPasted( dstFile );
	  } else {
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
				ReplyOnExistingFileDlg.FileAction.COPY,
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
		  Files.copy(
			file,
			dstFile,
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING,
			LinkOption.NOFOLLOW_LINKS );
		}
		pathPasted( dstFile );
		break;
	      case SKIP:
	      case SKIP_ALL:
		// nichts tun
		break;
	      case RENAME:
		if( renamedFile != null ) {
		  if( url != null ) {
		    download( url, renamedFile );
		  } else {
		    Files.copy(
			file,
			renamedFile,
			StandardCopyOption.COPY_ATTRIBUTES,
			StandardCopyOption.REPLACE_EXISTING,
			LinkOption.NOFOLLOW_LINKS );
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
	      Files.copy(
			file,
			dstFile,
			StandardCopyOption.COPY_ATTRIBUTES,
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

  private static Path createPathCopy( Path path ) throws IOException
  {
    Path   parentPath = path.getParent();
    Path   namePath   = path.getFileName();
    String namePart   = null;
    if( namePath != null ) {
      namePart = namePath.toString();
    }
    if( (parentPath == null) || (namePart == null) ) {
      throw new IOException( "Name der Quelldatei kann nicht"
			+ " in Pfad und Dateiname zerlegt werden." );
    }
    Path   rv      = null;
    String extPart = "";
    int    dotPos  = namePart.lastIndexOf( '.' );
    if( (dotPos > 0) && (dotPos < (namePart.length() - 1)) ) {
      extPart  = namePart.substring( dotPos );
      namePart = namePart.substring( 0, dotPos );
    }
    String newName = namePart + " - Kopie" + extPart;
    Path   newPath = parentPath.resolve( newName );
    if( Files.exists( newPath, LinkOption.NOFOLLOW_LINKS ) ) {
      for( int i = 1; i < 3; i++ ) {
	newName = String.format(
				"%s - Kopie (%d)%s",
				namePart,
				i,
				extPart );
	newPath = parentPath.resolve( newName );
	if( !Files.exists( newPath, LinkOption.NOFOLLOW_LINKS ) ) {
	  rv = newPath;
	  break;
	}
      }
    } else {
      rv = newPath;
    }
    if( rv == null ) {
      throw new IOException( newPath.toString()
				+ ": Zieldatei existiert bereits" );
    }
    return rv;
  }


  private Path resolveDst( Path path )
  {
    Path parent = this.curBaseParent;
    if( parent != null ) {
      path = parent.relativize( path );
    }
    return this.dstPath.resolve( path );
  }
}
