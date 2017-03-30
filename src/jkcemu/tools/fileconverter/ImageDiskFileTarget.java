/*
 * (c) 2012-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine ImageDisk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.ImageDisk;


public class ImageDiskFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public ImageDiskFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super( fileConvertFrm, "ImageDisk-Datei (*.imd)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getMaxRemarkLength()
  {
    return -1;		// unbegrenzt
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getImageDiskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".imd" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".imd" );
    return ImageDisk.export(
			this.disk,
			file,
			this.fileConvertFrm.getRemark() );
  }
}
