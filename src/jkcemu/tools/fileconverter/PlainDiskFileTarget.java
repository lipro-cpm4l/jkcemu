/*
 * (c) 2012-2016 Jens Mueller
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
import jkcemu.disk.PlainDisk;


public class PlainDiskFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public PlainDiskFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super(
	fileConvertFrm,
	"Einfache Diskettenabbilddatei (*.img; *.image; *.raw)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getPlainDiskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".img" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".img", ".image", ".raw" );
    return PlainDisk.export( this.disk, file );
  }
}
