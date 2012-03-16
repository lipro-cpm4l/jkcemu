/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen fuer Audio
 */

package jkcemu.audio;

import java.awt.Component;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.base.BasicDlg;


public class AudioUtil
{
  private static javax.swing.filechooser.FileFilter audioInFileFilter  = null;
  private static javax.swing.filechooser.FileFilter audioOutFileFilter = null;


  public static void appendAudioFileExtensionText(
					StringBuilder buf,
					int           maxItems,
					String...     extensions )
  {
    if( extensions != null ) {
      /*
       * Sortieren und von hinten beginnen, damit in einer
       * evt. lmit "..." abgekuerzten Liste moeglichst
       * "*.wav" und "*.snd" sichtbar bleiben.
       */
      try {
	Arrays.sort( extensions );
      }
      catch( ClassCastException ex ) {}
      int     nAdded  = 0;
      boolean isFirst = true;
      for( int i = extensions.length - 1; i >= 0; --i ) {
	if( isFirst ) {
	  if( buf.length() > 0 ) {
	    buf.append( (char) '\u0020' );
	  }
	  buf.append( (char) '(' );
	  isFirst = false;
	} else {
	  buf.append( "; " );
	}
	String s = extensions[ i ];
	if( (maxItems > 0) && (nAdded >= maxItems) ) {
	  buf.append( " ..." );
	  break;
	}
	buf.append( (char) '*' );
	if( !s.startsWith( "." ) ) {
	  buf.append( (char) '.' );
	}
	buf.append( s );
	nAdded++;
      }
      if( !isFirst ) {
	buf.append( (char) ')' );
      }
    }
  }


  public static void appendAudioFormatText(
				StringBuilder buf,
				AudioFormat   fmt )
  {
    if( fmt != null ) {
      buf.append( (int) fmt.getSampleRate() );
      buf.append( " Hz, " );

      buf.append( fmt.getSampleSizeInBits() );
      buf.append( " Bit, " );

      int channels = fmt.getChannels();
      switch( channels ) {
	case 1:
	  buf.append( "Mono" );
	  break;
	case 2:
	  buf.append( "Stereo" );
	  break;
	default:
	  buf.append( channels );
	  buf.append( " Kan\u00E4le" );
	  break;
      }
    }
  }


  public static AudioFileFormat.Type getAudioFileType(
						Component owner,
						File      file )
  {
    AudioFileFormat.Type[] fTypes = AudioSystem.getAudioFileTypes();
    String                 fName  = file.getName();
    if( (fTypes != null) && (fName != null) ) {
      fName = fName.toUpperCase();
      for( AudioFileFormat.Type fType : fTypes ) {
	String ext = fType.getExtension();
	if( ext != null ) {
	  ext = ext.toUpperCase();
	  if( !ext.startsWith( "." ) ) {
	    ext = "." + ext;
	  }
	  if( fName.endsWith( ext ) )
	    return fType;
	}
      }
    }

    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "Das Dateiformat wird nicht unterst\u00FCtzt." );
    if( fTypes != null ) {
      if( fTypes.length > 0 ) {
	buf.append( "\nM\u00F6gliche Dateiendungen sind:\n" );
	String delim = null;
	for( AudioFileFormat.Type fType : fTypes ) {
	  String ext = fType.getExtension();
	  if( ext != null ) {
	    if( delim != null ) {
	      buf.append( delim );
	    }
	    buf.append( ext );
	    delim = ", ";
	  }
	}
      }
    }
    BasicDlg.showErrorDlg( owner, buf.toString() );
    return null;
  }


  public static String getAudioFormatText( AudioFormat fmt )
  {
    StringBuilder buf = new StringBuilder( 64 );
    appendAudioFormatText( buf, fmt );
    return buf.toString();
  }


  public static javax.swing.filechooser.FileFilter getAudioInFileFilter()
  {
    if( audioInFileFilter == null ) {
      String[] ext = getAudioOutFileExtensions( null, null );
      if( ext != null ) {
	if( ext.length == 0 ) {
	  ext = null;
	}
      }
      if( ext == null ) {
	ext = new String[] {
			AudioFileFormat.Type.WAVE.getExtension(),
			AudioFileFormat.Type.AU.getExtension(),
			AudioFileFormat.Type.AIFF.getExtension(),
			AudioFileFormat.Type.AIFC.getExtension(),
			AudioFileFormat.Type.SND.getExtension() };
      }
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "Sound-Dateien" );
      appendAudioFileExtensionText( buf, 3, ext );
      audioInFileFilter = new FileNameExtensionFilter( buf.toString(), ext );
    }
    return audioInFileFilter;
  }


  public static String[] getAudioOutFileExtensions(
					AudioInputStream ais,
					String           extToExclude )
  {
    String[]               rv = null;
    AudioFileFormat.Type[] t  = null;
    if( ais != null ) {
      t = AudioSystem.getAudioFileTypes( ais );
    } else {
      t = AudioSystem.getAudioFileTypes();
    }
    if( t != null ) {
      if( t.length > 0 ) {
	java.util.List<String> list = new ArrayList<String>( t.length );
	for( AudioFileFormat.Type tmpT : t ) {
	  String s = tmpT.getExtension();
	  if( s != null ) {
	    if( !s.isEmpty() ) {
	      s = s.toLowerCase();
	      if( extToExclude != null ) {
		if( s.equalsIgnoreCase( extToExclude ) ) {
		  s = null;
		}
	      }
	      if( s != null ) {
		list.add( s );
	      }
	    }
	  }
	}
	int n = list.size();
	if( n > 0 ) {
	  try {
	    rv = list.toArray( new String[ n ] );
	  }
	  catch( ArrayStoreException ex ) {}
	}
      }
    }
    return rv;
  }


  public static javax.swing.filechooser.FileFilter getAudioOutFileFilter()
  {
    if( audioOutFileFilter == null ) {
      String[]      ext = getAudioOutFileExtensions( null, null );
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "Sound-Dateien" );
      appendAudioFileExtensionText( buf, 3, ext );
      audioOutFileFilter = new FileNameExtensionFilter( buf.toString(), ext );
    }
    return audioOutFileFilter;
  }
}

