/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im AC1/LLC2-TurboSave-Format
 */

package jkcemu.tools.fileconverter;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.AudioInputStream;
import javax.swing.JComboBox;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.emusys.ac1_llc2.SCCHAudioDataStream;


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
    this.dataBytes   = dataBytes;
    this.offs        = offs;
    this.len         = len;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canPlay()
  {
    return true;
  }


  @Override
  public AudioInputStream getAudioInputStream() throws
						IOException,
						UserInputException
  {
    AudioInputStream ais = null;
    int len = Math.min( this.len, this.dataBytes.length - this.offs );
    if( len > 0 ) {
      int begAddr = this.fileConvertFrm.getBegAddr( true );
      int endAddr = begAddr + len - 1;
      SCCHAudioDataStream ads =  new SCCHAudioDataStream(
			this.dataBytes,
			this.offs,
			this.len,
			this.fileConvertFrm.getFileDesc( true ),
			(char) this.fileConvertFrm.getFileTypeChar( true ),
			begAddr,
			endAddr );
      ais = new AudioInputStream(
			ads,
			ads.getAudioFormat(),
			ads.getFrameLength() );
    }
    return ais;
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioUtil.getAudioOutFileFilter();
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
    saveAudioFile( file, getAudioInputStream() );
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
    String   rv  = "Sound-Datei im AC1/LLC2-TurboSave-Format";
    String[] ext = AudioUtil.getAudioOutFileExtensions( null, null );
    if( ext != null ) {
      if( ext.length > 0 ) {
	StringBuilder buf = new StringBuilder( 128 );
	buf.append( rv );
	AudioUtil.appendAudioFileExtensionText( buf, 3, ext );
	rv = buf.toString();
      }
    }
    return rv;
  }
}
