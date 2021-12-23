/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine CSW-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.audio.CSWFile;
import jkcemu.audio.PCMDataSource;


public class CSWFileTarget extends AbstractConvertTarget
{
  private BitSampleBuffer                      samples;
  private javax.swing.filechooser.FileFilter[] fileFilters;


  public CSWFileTarget(
		FileConvertFrm  fileConvertFrm,
		BitSampleBuffer samples )
  {
    super( fileConvertFrm,
	"CSW-Datei (" + CSWFile.getFileExtensionText() + ")" );
    this.samples     = samples;
    this.fileFilters = null;
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
    return this.samples.newReader();
  }


  @Override
  public synchronized javax.swing.filechooser.FileFilter[] getFileFilters()
  {
    if( this.fileFilters == null ) {
      this.fileFilters = new javax.swing.filechooser.FileFilter[]
					{ CSWFile.getFileFilter() };
    }
    return this.fileFilters;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return getSuggestedOutFile( srcFile, CSWFile.getFileExtensions(), "csw" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    if( file != null ) {
      CSWFile.write( createPCMDataSource(), file );
    }
    return null;
  }
}
