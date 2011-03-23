/*
 * (c) 2008-2009 Jens Mueller
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
import jkcemu.base.BasicDlg;


public class AudioUtil
{
  private static javax.swing.filechooser.FileFilter audioInFileFilter  = null;
  private static javax.swing.filechooser.FileFilter audioOutFileFilter = null;


  public static AudioFileFormat.Type getAudioFileType(
						Component owner,
						File      file )
  {
    Collection<AudioFileFormat.Type> types =
				new ArrayList<AudioFileFormat.Type>();
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AIFC ) ) {
      types.add( AudioFileFormat.Type.AIFC );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AIFF ) ) {
      types.add( AudioFileFormat.Type.AIFF );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AU ) ) {
      types.add( AudioFileFormat.Type.AU );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.SND ) ) {
      types.add( AudioFileFormat.Type.SND );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.WAVE ) ) {
      types.add( AudioFileFormat.Type.WAVE );
    }

    String fileName = file.getName();
    if( fileName != null ) {
      fileName = fileName.toUpperCase( Locale.ENGLISH );
      for( AudioFileFormat.Type fileType : types ) {
	String ext = fileType.getExtension();
	if( ext != null ) {
	  ext = ext.toUpperCase();
	  if( !ext.startsWith( "." ) ) {
	    ext = "." + ext;
	  }
	  if( fileName.endsWith( ext ) )
	    return fileType;
	}
      }
    }

    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "Das Dateiformat wird nicht unterst\u00FCtzt." );
    if( !types.isEmpty() ) {
      buf.append( "\nM\u00F6gliche Dateiendungen sind:\n" );
      String delim = null;
      for( AudioFileFormat.Type fileType : types ) {
	String ext = fileType.getExtension();
	if( ext != null ) {
	  if( delim != null ) {
	    buf.append( delim );
	  }
	  buf.append( ext );
	  delim = ", ";
	}
      }
    }
    BasicDlg.showErrorDlg( owner, buf.toString() );
    return null;
  }


  public static String getAudioFormatText( AudioFormat fmt )
  {
    StringBuilder buf = new StringBuilder( 64 );
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
    return buf.toString();
  }


  public static javax.swing.filechooser.FileFilter getAudioInFileFilter()
  {
    if( audioInFileFilter == null ) {
      audioInFileFilter = createAudioFileFilter(
				"Audiodateien",
				AudioFileFormat.Type.AIFC,
				AudioFileFormat.Type.AIFF,
				AudioFileFormat.Type.AU,
				AudioFileFormat.Type.SND,
				AudioFileFormat.Type.WAVE );
    }
    return audioInFileFilter;
  }


  public static javax.swing.filechooser.FileFilter getAudioOutFileFilter()
  {
    if( audioOutFileFilter == null ) {
      audioOutFileFilter = createAudioFileFilter(
				"Unterst\u00FCtzte Audiodateien",
				AudioSystem.getAudioFileTypes() );
    }
    return audioOutFileFilter;
  }


	/* --- private Methoden --- */

  private static javax.swing.filechooser.FileFilter createAudioFileFilter(
					String text,
					AudioFileFormat.Type... types )
  {
    javax.swing.filechooser.FileFilter rv = null;
    if( types != null ) {
      if( types.length > 0 ) {
	java.util.List<String> lst = new ArrayList<String>( types.length );
	for( int i = 0; i < types.length; i++ ) {
	  String s = types[ i ].getExtension();
	  if( s != null ) {
	    s = s.toLowerCase();
	    if( !lst.contains( s ) ) {
	      lst.add( s );
	    }
	  }
	}
	int n = lst.size();
	if( n > 0 ) {
	  String[] extensions = lst.toArray( new String[ n ] );
	  if( extensions != null ) {
	    if( extensions.length > 0 ) {
	      Arrays.sort( extensions );
	      rv = new javax.swing.filechooser.FileNameExtensionFilter(
								text,
								extensions );
	    }
	  }
	}
      }
    }
    return rv;
  }
}

