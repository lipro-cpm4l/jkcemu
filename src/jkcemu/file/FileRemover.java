/*
 * (c) 2014-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Loeschen von Dateien und Dateibaeumen
 */

package jkcemu.file;

import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JCheckBox;
import jkcemu.base.BaseDlg;
import jkcemu.base.DesktopHelper;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


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
	  buf.append( '\u0020' );
	}
	buf.append( "l\u00F6schen?" );

	boolean status      = false;
	boolean moveToTrash = false;
	if( DesktopHelper.isMoveToTrashSupported() ) {
	  JCheckBox cb = GUIFactory.createCheckBox(
					"In den Papierkorb werfen",
					true );
	  status      = BaseDlg.showYesNoDlg( owner, buf.toString(), cb );
	  moveToTrash = cb.isSelected();
	} else {
	  status = BaseDlg.showYesNoDlg( owner, buf.toString() );
	}
	if( status ) {
	  if( moveToTrash ) {
	    try {
	      Set<Path> removedPaths = new HashSet<>();
	      try {
		for( Path path : paths ) {
		  DesktopHelper.moveToTrash( path.toFile() );
		  removedPaths.add( path );
		}
	      }
	      finally {
		if( !removedPaths.isEmpty() ) {
		  pathListener.pathsRemoved( removedPaths );
		}
	      }
	    }
	    catch( IOException ex ) {
	      BaseDlg.showErrorDlg( owner, ex );
	    }
	  } else {
	    (new FileRemover(
			owner,
			paths,
			moveToTrash,
			pathListener,
			register )).startWork();
	  }
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFailedMsg( String fileName )
  {
    return "\'" + fileName + "\' kann nicht gel\u00F6scht werden.";
  }


  @Override
  public String getProgressDlgTitle()
  {
    return EmuUtil.TEXT_DELETE;
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
    return delete( dir );
  }


  @Override
  public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
  {
    return delete( file );
  }


	/* --- Konstruktor --- */

  private FileRemover(
		Window                         owner,
		java.util.List<Path>           paths,
		boolean                        moveToTrash,
		PathListener                   pathListener,
		Collection<AbstractFileWorker> register )
  {
    super( owner, paths, pathListener, register );
  }


	/* --- private Methoden --- */

  private FileVisitResult delete( Path path )
  {
    boolean done = false;
    while( !done && !this.cancelled ) {
      done         = true;
      this.curPath = path;
      try {
	Files.delete( path );
	pathRemoved( path );
      }
      catch( NoSuchFileException ex1 ) {}
      catch( IOException ex1 ) {
	done = !handleError( path, ex1, true );
      }
    }
    return this.cancelled ?
		FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
  }
}
