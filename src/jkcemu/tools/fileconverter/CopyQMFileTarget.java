/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine CopyQM-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.CopyQMDisk;


public class CopyQMFileTarget extends AbstractConvertTarget
{
  private AbstractFloppyDisk disk;


  public CopyQMFileTarget(
			FileConvertFrm     fileConvertFrm,
			AbstractFloppyDisk disk )
  {
    super( fileConvertFrm, "CopyQM-Datei (*.cqm)" );
    this.disk = disk;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getMaxRemarkLength()
  {
    return 0x7FFE;
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return EmuUtil.getCopyQMFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".cqm" );
  }


  @Override
  public String save( File file ) throws IOException
  {
    checkFileExtension( file, ".cqm" );
    CopyQMDisk.export( this.disk, file, this.fileConvertFrm.getRemark() );
    return null;
  }
}
