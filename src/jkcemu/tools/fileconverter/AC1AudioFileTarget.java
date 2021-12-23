/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im AC1-Format
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import jkcemu.audio.AudioFile;
import jkcemu.audio.PCMDataSource;
import jkcemu.base.UserInputException;
import jkcemu.emusys.ac1_llc2.AC1AudioCreator;


public class AC1AudioFileTarget extends AbstractConvertTarget
{
  private byte[]  buf;
  private int     offs;
  private int     len;
  private boolean basic;


  public AC1AudioFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         buf,
		int            offs,
		int            len,
		boolean        basic )
  {
    super( fileConvertFrm, createInfoText( basic ) );
    this.buf   = buf;
    this.offs  = offs;
    this.len   = len;
    this.basic = basic;
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
    return new AC1AudioCreator(
		this.basic,
		this.buf,
		this.offs,
		this.len,
		this.fileConvertFrm.getFileDesc( true ),
		this.fileConvertFrm.getBegAddr( true ),
		this.fileConvertFrm.getStartAddr( false ) ).newReader();
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioFile.getFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return this.basic ? 6 : 16;
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
  public boolean usesBegAddr()
  {
    return true;
  }


  @Override
  public boolean usesStartAddr( int fileType )
  {
    return true;
  }


	/* --- private Methoden --- */

  private static String createInfoText( boolean basic )
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "Sound-Datei im AC1-" );
    if( basic ) {
      buf.append( "BASIC-" );
    }
    buf.append( "Format (" );
    buf.append( AudioFile.getFileExtensionText() );
    buf.append( ')' );
    return buf.toString();
  }
}
