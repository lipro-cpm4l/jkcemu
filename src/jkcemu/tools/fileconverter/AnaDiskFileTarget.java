/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine AnaDisk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.AnaDisk;


public class AnaDiskFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public AnaDiskFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super( fileConvertFrm, "AnaDisk-Datei (*.dump)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getAnaDiskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".dump" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".dump" );
    return AnaDisk.export( this.disk, file );
  }
}
