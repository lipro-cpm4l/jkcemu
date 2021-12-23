/*
 * (c) 2011-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import jkcemu.audio.AudioFile;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.audio.PCMDataSource;
import jkcemu.base.EmuUtil;


public class AudioFileTarget extends AbstractConvertTarget
{
  private File                                 file;
  private javax.swing.filechooser.FileFilter[] fileFilters;
  private BitSampleBuffer                      samples;


  public AudioFileTarget(
		FileConvertFrm fileConvertFrm,
		File           file )
  {
    super(
	fileConvertFrm,
	"Sound-Datei (" + AudioFile.getFileExtensionText() + ")" );
    this.file        = file;
    this.fileFilters = null;
    this.samples     = null;
  }


  public AudioFileTarget(
		FileConvertFrm  fileConvertFrm,
		BitSampleBuffer samples )
  {
    super(
	fileConvertFrm,
	"Sound-Datei (" + AudioFile.getFileExtensionText() + ")" );
    this.file        = null;
    this.fileFilters = null;
    this.samples     = samples;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canPlay()
  {
    return true;
  }


  @Override
  public PCMDataSource createPCMDataSource() throws IOException
  {
    PCMDataSource pcm = null;
    try {
      if( this.samples != null ) {
	pcm = this.samples.newReader();
      } else if( this.file != null ) {
	pcm = AudioFile.open( this.file );
      }
    }
    catch( IOException ex ) {
      EmuUtil.closeSilently( pcm );
      throw ex;
    }
    return pcm;
  }


  @Override
  public synchronized javax.swing.filechooser.FileFilter[] getFileFilters()
  {
    if( this.fileFilters == null ) {
      this.fileFilters = new javax.swing.filechooser.FileFilter[]
					{ AudioFile.getFileFilter() };
    }
    return this.fileFilters;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return getSuggestedOutFile(
			srcFile,
			AudioFile.getFileExtensions(),
			"wav" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    if( file != null ) {
      AudioFile.write( createPCMDataSource(), file );
    }
    return null;
  }
}
