/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine CDT/TZX-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.audio.PCMDataSource;
import jkcemu.audio.TZXFile;
import jkcemu.base.*;


public class TZXFileTarget extends AbstractConvertTarget
{
  private BitSampleBuffer                      samples;
  private javax.swing.filechooser.FileFilter[] fileFilters;


  public TZXFileTarget(
		FileConvertFrm  fileConvertFrm,
		BitSampleBuffer samples )
  {
    super( fileConvertFrm,
	"CDT/TZX-Datei (" + TZXFile.getFileExtensionText() + ")" );
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
					{ TZXFile.getFileFilter() };
    }
    return this.fileFilters;
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return getSuggestedOutFile( srcFile, TZXFile.getFileExtensions(), "tzx" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    if( file != null ) {
      TZXFile.write( createPCMDataSource(), file );
    }
    return null;
  }
}
