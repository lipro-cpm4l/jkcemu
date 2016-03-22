/*
 * (c) 2014-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Loeschen von Dateien und Dateibaeumen
 */

package jkcemu.base;


import java.awt.Window;
import java.io.IOException;
import java.lang.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;


public class FileRemover extends AbstractFileWorker
{
  public static void startRemove(
			Window                         owner,
			java.util.List<Path>           paths,
			PathListener                   pathListener,
			Collection<AbstractFileWorker> register )
  {
    if( paths != null ) {
      int n = paths.size();
      if( n > 0 ) {
	StringBuilder buf = new StringBuilder( 128 );
	buf.append( "M\u00F6chten Sie " );
	if( n == 1 ) {
	  Path p = paths.get( 0 );
	  if( Files.isDirectory( p ) ) {
	    buf.append( "das Verzeichnis" );
	  } else if( Files.isSymbolicLink( p ) ) {
	    buf.append( "den symbolischen Link" );
	  } else {
	    buf.append( "die Datei" );
	  }
	  buf.append( "\n\'" );
	  buf.append( p );
	  buf.append( "\'\n" );
	} else {
	  int nDirs  = 0;
	  int nFiles = 0;
	  int nLinks = 0;
	  for( Path p : paths ) {
	    if( Files.isDirectory( p ) ) {
	      nDirs++;
	    } else if( Files.isSymbolicLink( p ) ) {
	      nLinks++;
	    } else {
	      nFiles++;
	    }
	  }
	  if( nDirs == 1 ) {
	    buf.append( "das Verzeichnis" );
	  } else if( nDirs > 1 ) {
	    buf.append( nDirs );
	    buf.append( " Verzeichnisse" );
	  }
	  if( nFiles > 0 ) {
	    if( nDirs > 0 ) {
	      if( nLinks > 0 ) {
		buf.append( ", " );
	      } else {
		buf.append( " und " );
	      }
	    }
	    if( nFiles == 1 ) {
	      buf.append( "die Datei" );
	    } else if( nFiles > 1 ) {
	      buf.append( nFiles );
	      buf.append( " Dateien" );
	    }
	  }
	  if( nLinks > 0 ) {
	    if( (nDirs > 0) && (nFiles > 0) ) {
	      buf.append( " und " );
	    }
	    if( nLinks == 1 ) {
	      buf.append( "den symbolischen Link" );
	    } else if( nLinks > 1 ) {
	      buf.append( nLinks );
	      buf.append( " symbolische Links" );
	    }
	  }
	  buf.append( (char) '\u0020' );
	}
	buf.append( "l\u00F6schen?" );
	if( BasicDlg.showYesNoDlg( owner, buf.toString() ) ) {
	  (new FileRemover(
			owner,
			paths,
			pathListener,
			register )).startWork();
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFailedMsg( String fileName )
  {
    return fileName + "\nkann nicht gel\u00F6scht werden.";
  }


  @Override
  public String getProgressDlgTitle()
  {
    return "L\u00F6schen";
  }


  @Override
  public String getUncompletedWorkMsg()
  {
    return "Es konnten nicht alle Dateien, Verzeichnisse bzw.\n"
			+ "symbolische Links gel\u00F6scht werden.";
  }


  @Override
  public FileVisitResult postVisitDirectory( Path dir, IOException ex )
  {
    if( !this.cancelled ) {
      this.curPath = dir;
      try {
	Files.delete( dir );
	pathRemoved( dir );
      }
      catch( NoSuchFileException ex1 ) {}
      catch( IOException ex1 ) {
	handleError( dir, ex1 );
      }
    }
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    if( !this.cancelled ) {
      this.curPath = file;
      try {
	Files.delete( file );
	pathRemoved( file );
      }
      catch( NoSuchFileException ex1 ) {}
      catch( IOException ex1 ) {
	handleError( file, ex1 );
      }
    }
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }


	/* --- Konstruktor --- */

  private FileRemover(
		Window                         owner,
		java.util.List<Path>           paths,
		PathListener                   pathListener,
		Collection<AbstractFileWorker> register )
  {
    super( owner, paths, pathListener, register );
  }
}
