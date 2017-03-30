/*
 * (c) 2015-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine TeleDisk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.TeleDisk;


public class TeleDiskFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public TeleDiskFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super( fileConvertFrm, "TeleDisk-Datei (*.td0)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getMaxRemarkLength()
  {
    return 0x7E;
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getTeleDiskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".td0" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".td0" );
    return TeleDisk.export(
			this.disk,
			file,
			this.fileConvertFrm.getRemark() );
  }
}
