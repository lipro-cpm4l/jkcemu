/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im AC1/LLC2-TurboSave-Format
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import javax.swing.JComboBox;
import jkcemu.audio.AudioFile;
import jkcemu.audio.PCMDataSource;
import jkcemu.base.UserInputException;
import jkcemu.emusys.ac1_llc2.SCCHAudioCreator;


public class SCCHAudioFileTarget extends AbstractConvertTarget
{
  private static String[] fileTypeItems = {
				"P - Programm",
				"B - BASIC-Programm",
				"F - BASIC-Feld",
				"D - Daten" };

  private byte[] dataBytes;
  private int    offs;
  private int    len;


  public SCCHAudioFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len )
  {
    super( fileConvertFrm, createInfoText() );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
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
    int begAddr  = this.fileConvertFrm.getBegAddr( true );
    int endAddr  = begAddr + len - 1;
    return new SCCHAudioCreator(
			this.dataBytes,
			this.offs,
			this.len,
			this.fileConvertFrm.getFileDesc( true ),
			(char) this.fileConvertFrm.getFileTypeChar( true ),
			begAddr,
			endAddr ).newReader();
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioFile.getFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return 16;
  }


  @Override
  public int getMaxFileTypeLength()
  {
    return 1;
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


  @Override
  public void setFileTypesTo( JComboBox<String> combo )
  {
    combo.removeAllItems();
    for( int i = 0; i < fileTypeItems.length; i++ ) {
      combo.addItem( fileTypeItems[ i ] );
    }
    combo.setEnabled( true );
    combo.setEditable( true );
    combo.setSelectedItem(
	fileTypeItems[ this.fileConvertFrm.getOrgStartAddr() >= 0 ? 0 : 3 ] );
  }


  @Override
  public boolean usesBegAddr()
  {
    return true;
  }


	/* --- private Methoden --- */

  private static String createInfoText()
  {
    return "Sound-Datei im AC1/LLC2-TurboSave-Format ("
		+ AudioFile.getFileExtensionText() + ")";
  }
}
