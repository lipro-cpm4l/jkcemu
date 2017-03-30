/*
 * (c) 2014-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kopieren von Dateien und Dateibaeumen
 */

package jkcemu.base;

import java.awt.Window;
import java.io.IOException;
import java.lang.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import javax.swing.JOptionPane;


public class FileCopier extends AbstractFileWorker
{
  private Path             dstPath;
  private volatile Boolean copyAllDirs;
  private volatile Boolean replaceAllFiles;


  /*
   * Der Paramater srcFiles ist absichtlich nicht Generics-maessig
   * typisiert, um auch die von
   * Transferable.getTransferData( DataFlavor.javaFileListFlavor )
   * zurueckgelieferte Collection hier problemlos uebergeben zu koennen.
   */
  public FileCopier(
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
			+ " kopiert werden." );
    this.dstPath         = dstPath;
    this.copyAllDirs     = null;
    this.replaceAllFiles = null;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFailedMsg( String fileName )
  {
    return fileName + "\nkann nicht kopiert werden.";
  }


  @Override
  public String getProgressDlgTitle()
  {
    return "Kopieren";
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
	if( Files.isSameFile( dstDir, dir ) ) {
	  rv = FileVisitResult.SKIP_SUBTREE;
	} else {
	  boolean exists    = false;
	  boolean forceCopy = false;
	  if( this.copyAllDirs != null ) {
	    forceCopy = this.copyAllDirs.booleanValue();
	  }
	  if( !forceCopy ) {
	    exists = Files.exists( dstDir, LinkOption.NOFOLLOW_LINKS );
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
				"Kopieren",
				"Alle kopieren",
				"\u00DCberspringen",
				"Alle \u00FCberspringen",
				"Abbrechen" } ) )
	    {
	      case 0:
		forceCopy = true;
		break;
	      case 1:
		forceCopy        = true;
		this.copyAllDirs = Boolean.TRUE;
		break;
	      case 2:
		forceCopy = false;
		break;
	      case 3:
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
	Path dstFile = resolveDst( file );
	if( !Files.isSameFile( dstFile, file ) ) {
	  boolean exists       = false;
	  boolean forceReplace = false;
	  if( this.replaceAllFiles != null ) {
	    forceReplace = this.replaceAllFiles.booleanValue();
	  }
	  if( !forceReplace ) {
	    exists = Files.exists( dstFile, LinkOption.NOFOLLOW_LINKS );
	  }
	  if( (this.replaceAllFiles == null) && exists ) {
	    switch( showJOptionPane(
			"Die Datei \'" + dstFile.toString()
				+ "\' existiert bereits.\n"
				+ "M\u00F6chten Sie die Datei ersetzen?",
			JOptionPane.WARNING_MESSAGE,
			"Datei bereits vorhanden",
			new String[] {
				"Ersetzen",
				"Alle ersetzen",
				"Nicht ersetzen",
				"Alle nicht ersetzen",
				"Abbrechen" } ) )
	    {
	      case 0:
		forceReplace = true;
		break;
	      case 1:
		forceReplace         = true;
		this.replaceAllFiles = Boolean.TRUE;
		break;
	      case 2:
		forceReplace = false;
		break;
	      case 3:
		forceReplace         = false;
		this.replaceAllFiles = Boolean.FALSE;
		break;
	      default:
		this.cancelled = true;
	    }
	  }
	  if( !this.cancelled && (!exists || forceReplace) ) {
	    Files.copy(
		file,
		dstFile,
		StandardCopyOption.REPLACE_EXISTING,
		StandardCopyOption.COPY_ATTRIBUTES,
		LinkOption.NOFOLLOW_LINKS );
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
    Path parent = this.curParent;
    if( parent != null ) {
      path = parent.relativize( path );
    }
    return this.dstPath.resolve( path );
  }
}
