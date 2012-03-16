/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine AnaDisk-Datei
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import jkcemu.base.EmuUtil;
import jkcemu.disk.*;


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
  public void save( File file ) throws IOException
  {
    checkFileExtension( file, ".dump" );
    AnaDisk.export( this.disk, file );
  }
}

