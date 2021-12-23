/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung einer KC-TAP- oder Multi-KC-TAP-Datei
 * in eine Sound-Datei im KC-Format
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import jkcemu.audio.AudioFile;
import jkcemu.audio.PCMDataSource;
import jkcemu.base.UserInputException;
import jkcemu.emusys.kc85.KCAudioCreator;


public class KCAudioMultiFileTarget extends AbstractConvertTarget
{
  public static final String INFO_TEXT = "Sound-Datei im KC-Format,"
				+ " 1:1 aus TAP- oder Multi-TAP-Datei"
				+ " konvertiert";

  private byte[] tapFileBytes;


  public KCAudioMultiFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         tapFileBytes )
  {
    super( fileConvertFrm, INFO_TEXT );
    this.tapFileBytes = tapFileBytes;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canPlay()
  {
    return true;
  }


  @Override
  public PCMDataSource createPCMDataSource()
				throws IOException, UserInputException
  {
    return new KCAudioCreator(
			true,
			0,
			this.tapFileBytes,
			0,
			this.tapFileBytes.length ).newReader();
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioFile.getFileFilter();
  }


  @Override
  public File getSuggestedOutFile( File srcFile )
  {
    return replaceExtensionToAudioFile( srcFile );
  }


  @Override
  public String save( File file ) throws IOException, UserInputException
  {
    saveAudioFile( file, createPCMDataSource() );
    return null;
  }
}
