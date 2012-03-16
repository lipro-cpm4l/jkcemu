/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine CPC-Disk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.*;


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
    return EmuUtil.getDskFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtension( srcFile, ".dsk" );
  }


  @Override
  public void save( File file ) throws IOException
  {
    checkFileExtension( file, ".dsk" );
    CPCDisk.export( this.disk, file );
  }
}

