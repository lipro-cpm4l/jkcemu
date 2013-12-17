/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine ImageDisk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.*;


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
    ImageDisk.export( this.disk, file, null );
    return null;
  }
}

