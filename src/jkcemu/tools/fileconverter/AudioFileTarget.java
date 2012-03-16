/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import java.util.Arrays;
import javax.sound.sampled.*;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;


public class AudioFileTarget extends AbstractConvertTarget
{
  private File                                 file;
  private String[]                             extensions;
  private String                               extensionText;
  private javax.swing.filechooser.FileFilter[] fileFilters;


  public AudioFileTarget(
		FileConvertFrm fileConvertFrm,
		File           file,
		String[]       extensions,
		String         extensionText )
  {
    super( fileConvertFrm, "Sound-Datei " + extensionText );
    this.file          = file;
    this.extensions    = extensions;
    this.extensionText = extensionText;
    this.fileFilters   = null;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canPlay()
  {
    return true;
  }


  @Override
  public AudioInputStream getAudioInputStream() throws IOException
  {
    AudioInputStream ais = null;
    try {
      ais = AudioSystem.getAudioInputStream( this.file );
    }
    catch( UnsupportedAudioFileException ex ) {
      throw new IOException( ex.getMessage() );
    }
    return ais;
  }


  @Override
  public javax.swing.filechooser.FileFilter[] getFileFilters()
  {
    if( this.fileFilters == null ) {
      if( this.extensions != null ) {
	if( this.extensions.length > 0 ) {
	  String text = "Sound-Dateien ";
	  if( this.extensionText != null ) {
	    text += this.extensionText;
	  }
	  this.fileFilters = new javax.swing.filechooser.FileFilter[] {
		new javax.swing.filechooser.FileNameExtensionFilter(
							text,
							this.extensions ) };
	}
      }
    }
    return this.fileFilters;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    File outFile = null;
    if( (srcFile != null) && (this.extensions != null) ) {
      if( this.extensions.length > 0 ) {
	outFile = replaceExtension(
		srcFile,
		this.extensions[ this.extensions.length - 1 ] );
      }
    }
    return outFile;
  }


  @Override
  public void save( File file ) throws IOException
  {
    if( file != null ) {
      AudioFileFormat.Type aft = AudioUtil.getAudioFileType(
						this.fileConvertFrm,
						file );
      if( aft != null ) {
	AudioInputStream ais = getAudioInputStream();
	if( ais != null ) {
	  AudioSystem.write( ais, aft, file );
	}
      }
    }
  }
}

