/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verschieben von Dateien und Dateibaeumen
 */

package jkcemu.base;


import java.awt.Window;
import java.io.IOException;
import java.lang.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;


public class FileMover extends AbstractFileWorker
{
  private Path    dstPath;
  private boolean dirFailed;


  /*
   * Der Paramater srcFiles ist absichtlich nicht Generics-maessig
   * typisiert, um auch die von
   * Transferable.getTransferData( DataFlavor.javaFileListFlavor )
   * zurueckgelieferte Collection hier problemlos uebergeben zu koennen.
   */
  public FileMover(
		Window                         owner,
		Collection                     srcFiles,
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
    this.dstPath   = dstPath;
    this.dirFailed = false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFailedMsg( String fileName )
  {
    return fileName + "\nkann nicht verschoben werden.";
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
  protected void handleError( Path path, IOException ex )
  {
    this.dirFailed = true;
    super.handleError( path, ex );
  }


  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    if( !this.cancelled && !this.dirFailed ) {
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
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult preVisitDirectory(
				Path                dir,
				BasicFileAttributes attrs )
  {
    FileVisitResult rv = FileVisitResult.CONTINUE;
    if( !this.cancelled ) {
      this.curPath = dir;
      Path dstDir = resolveDst( dir );
      try {
	this.dirFailed = false;
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
	Path dstFile = resolveDst( file );
	Files.move(
		file,
		resolveDst( file ),
		StandardCopyOption.REPLACE_EXISTING );
	pathRemoved( file );
	pathPasted( dstFile );
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
    Path parent = this.curParent;
    if( parent != null ) {
      path = parent.relativize( path );
    }
    return this.dstPath.resolve( path );
  }
}
