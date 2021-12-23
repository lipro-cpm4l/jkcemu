/*
 * (c) 2012-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine CPC-Disk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.CPCDisk;
import jkcemu.file.FileUtil;


public class CPCDiskFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public CPCDiskFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super( fileConvertFrm, "CPC-Disk-Datei (*.dsk)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return FileUtil.getDskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return FileUtil.replaceExtension( srcFile, ".dsk" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".dsk" );
    return CPCDisk.export( this.disk, file );
  }
}
