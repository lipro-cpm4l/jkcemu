/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer ein Konvertierungsziel
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import javax.swing.JComboBox;
import jkcemu.audio.AudioFile;
import jkcemu.audio.PCMDataSource;
import jkcemu.base.EmuUtil;
import jkcemu.base.UserInputException;


public abstract class AbstractConvertTarget
				implements Comparable<AbstractConvertTarget>
{
  private static String suggestedAudioFileExt = null;

  protected FileConvertFrm fileConvertFrm;
  private   String         infoText;


  public AbstractConvertTarget(
			FileConvertFrm fileConvertFrm,
			String         infoText )
  {
    this.fileConvertFrm = fileConvertFrm;
    setInfoText( infoText );
  }


  public boolean canPlay()
  {
    return false;
  }


  protected void checkFileExtension(
				File      file,
				String... ext ) throws IOException
  {
    boolean matched = false;
    if( (file != null) && (ext != null) ) {
      String path = file.getPath().toLowerCase();
      for( String e : ext ) {
	if( path.endsWith( e ) ) {
	  matched = true;
	  break;
	}
      }
    }
    if( !matched ) {
      if( ext != null ) {
	if( ext.length == 1 ) {
	  throw new IOException(
		"Die Ausgabedatei muss die Endung \'"
					+ ext[ 0 ] + "\' haben." );
	}
	if( ext.length > 1 ) {
	  StringBuilder buf = new StringBuilder( 256 );
	  buf.append( "Die Ausgabedatei muss eine"
				+ " der folgenden Endungen haben:" );
	  for( String e : ext ) {
	    buf.append( "\n    " );
	    buf.append( e );
	  }
	  throw new IOException( buf.toString() );
	}
      }
      throw new IOException(
		  "Die Ausgabedatei hat die falsche Endung." );
    }
  }


  public PCMDataSource createPCMDataSource()
			throws IOException, UserInputException
  {
    return null;
  }


  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return null;
  }


  public javax.swing.filechooser.FileFilter[] getFileFilters()
  {
    javax.swing.filechooser.FileFilter fileFilter = getFileFilter();
    return fileFilter != null ?
		new javax.swing.filechooser.FileFilter[] { fileFilter }
		: null;
  }


  /*
   * Maximale Laenge des Dateibezeichnung:
   *  < 0: unbegrenzt
   *    0: Dateibezeichnung nicht unterstuetzt
   *  > 0: max. Laenge der Dateibezeichnung
   */
  public int getMaxFileDescLength()
  {
    return 0;		// Dateiname nicht unterstuetzt
  }


  /*
   * Maximale Laenge des Dateityps:
   *  < 0: unbegrenzt
   *    0: Dateityp nicht unterstuetzt
   *  > 0: max. Dateityplaenge
   */
  public int getMaxFileTypeLength()
  {
    return 0;		// Dateityp nicht unterstuetzt
  }


  /*
   * Maximale Laenge des Kommentars:
   *  < 0: unbegrenzt
   *    0: Kommentar nicht unterstuetzt
   *  > 0: max. kommentarlaenge
   */
  public int getMaxRemarkLength()
  {
    return 0;		// Kommentar nicht unterstuetzt
  }


  public abstract File getSuggestedOutFile( File srcFile );


  protected File getSuggestedOutFile(
				File     srcFile,
				String[] fileExtensions,
				String   prefExtension )
  {
    File outFile = null;
    if( (srcFile != null) && (fileExtensions != null) ) {
      if( fileExtensions.length > 0 ) {
	String ext = fileExtensions[ 0 ];
	if( prefExtension != null ) {
	  for( String tmpExt : fileExtensions ) {
	    if( tmpExt.equalsIgnoreCase( prefExtension ) ) {
	      ext = prefExtension;
	      break;
	    }
	  }
	}
	outFile = replaceExtension( srcFile, ext );
      }
    }
    return outFile;
  }


  /*
   * Die Methode speichert die konvertierten Daten.
   * Moeglicherweise konnte die Konvertierung nicht 1:1 durchgefuehrt werden.
   * Derartige Informationen werden als Log-Text zurueckgeliefert.
   * Dieser Text kann auch null sein.
   */
  public abstract String save( File file )
				throws IOException, UserInputException;


  public void setInfoText( String text )
  {
    this.infoText = (text != null ? text : "");
  }


  protected File replaceExtension( File srcFile, String ext )
  {
    File outFile = null;
    if( (srcFile != null) && (ext != null) ) {
      String fName = srcFile.getName();
      if( fName != null ) {
	int pos = fName.lastIndexOf( '.' );
	if( pos >= 0 ) {
	  if( ext.startsWith( "." ) ) {
	    fName = fName.substring( 0, pos ) + ext;
	  } else {
	    fName = fName.substring( 0, pos + 1 ) + ext;
	  }
	  File dirFile = srcFile.getParentFile();
	  if( dirFile != null ) {
	    outFile = new File( dirFile, fName );
	  } else {
	    outFile = new File( fName );
	  }
	}
      }
    }
    return outFile;
  }


  protected File replaceExtensionToAudioFile( File srcFile )
  {
    return replaceExtension( srcFile, getSuggestedAudioFileExtension() );
  }


  protected void saveAudioFile(
			File          file,
			PCMDataSource pcm ) throws IOException
  {
    try {
      if( (file != null) && (pcm != null) ) {
	AudioFile.write( pcm, file );
      }
    }
    finally {
      EmuUtil.closeSilent( pcm );
    }
  }


  public void setFileTypesTo( JComboBox<String> combo )
  {
    combo.removeAllItems();
    combo.setEnabled( false );
  }


  public boolean usesBegAddr()
  {
    return false;
  }


  public boolean usesStartAddr( int fileType )
  {
    return false;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( AbstractConvertTarget target )
  {
    return this.infoText.compareTo( target.infoText );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.infoText;
  }


	/* --- private Methoden --- */

  private static String getSuggestedAudioFileExtension()
  {
    if( suggestedAudioFileExt == null ) {
      String[] supportedExts = AudioFile.getSupportedFileExtensions();
      if( supportedExts != null ) {
	if( supportedExts.length > 0 ) {
	  String[] orderedExts = {
				"wav",
				"aif",
				"aifc",
				"aiff",
				"au" };
	  for( String e1 : orderedExts ) {
	    for( String e2 : supportedExts ) {
	      if( e1.equalsIgnoreCase( e2 ) ) {
		suggestedAudioFileExt = e1;
		break;
	      }
	    }
	    if( suggestedAudioFileExt != null ) {
	      break;
	    }
	  }
	  if( suggestedAudioFileExt == null ) {
	    suggestedAudioFileExt = supportedExts[ 0 ];
	  }
	}
      }
    }
    return suggestedAudioFileExt;
  }
}
