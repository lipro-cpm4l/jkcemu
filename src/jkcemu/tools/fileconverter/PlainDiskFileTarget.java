/*
 * (c) 2012-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine ImageDisk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.PlainDisk;
import jkcemu.file.FileUtil;


public class PlainDiskFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public PlainDiskFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super(
	fileConvertFrm,
	"Einfache Diskettenabbilddatei (*.dd; *.img; *.image; *.raw)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return FileUtil.getPlainDiskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return FileUtil.replaceExtension( srcFile, ".img" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".dd", ".img", ".image", ".raw" );
    return PlainDisk.export( this.disk, file );
  }
}
