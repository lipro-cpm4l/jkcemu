/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konvertierung in eine Sound-Datei im KC-Format
 */

package jkcemu.tools.fileconverter;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.util.Arrays;
import javax.swing.JComboBox;
import jkcemu.audio.AudioFile;
import jkcemu.audio.PCMDataSource;
import jkcemu.base.UserInputException;
import jkcemu.emusys.kc85.KCAudioCreator;


public class KCAudioFileTarget extends AbstractConvertTarget
{
  public static enum Target { Z9001, KC85, KCBASIC_PRG };

  private byte[] dataBytes;
  private int    offs;
  private int    len;
  private Target target;


  public KCAudioFileTarget(
		FileConvertFrm fileConvertFrm,
		byte[]         dataBytes,
		int            offs,
		int            len,
		Target         target )
  {
    super( fileConvertFrm, createInfoText( target ) );
    this.dataBytes = dataBytes;
    this.offs      = offs;
    this.len       = len;
    this.target    = target;
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
    PCMDataSource pcm       = null;
    int           blkNum    = 0;
    byte[]        fileBytes = null;
    if( this.target != null ) {
      int len = Math.min( this.len, this.dataBytes.length - this.offs );
      if( len > 0 ) {
	if( this.target.equals( Target.Z9001 ) ) {
	  int    begAddr = this.fileConvertFrm.getBegAddr( true );
	  int    endAddr = begAddr + len - 1;
	  int    startAddr = this.fileConvertFrm.getStartAddr( false );
	  String fileDesc  = this.fileConvertFrm.getFileDesc( true );
	  String fileType  = this.fileConvertFrm.getFileType();

	  byte[] m = new byte[ 128 + len ];
	  Arrays.fill( m, (byte) 0 );
	  int p = 0;
	  if( fileDesc != null ) {
	    int l = fileDesc.length();
	    while( (p < 8) && (p < l) ) {
	      m[ p ] = (byte) fileDesc.charAt( p);
	      p++;
	    }
	  }
	  p = 8;
	  if( fileType != null ) {
	    int l = fileType.length();
	    for( int i = 0; (i < l) && (p < 11); i++ ) {
	      m[ p++ ] = (byte) fileDesc.charAt( i);
	    }
	  }
	  p += 5;
	  m[ p++ ] = (byte) (startAddr >= 0 ? 3 : 2);
	  m[ p++ ] = (byte) begAddr;
	  m[ p++ ] = (byte) (begAddr >> 8);
	  m[ p++ ] = (byte) endAddr;
	  m[ p++ ] = (byte) (endAddr >> 8);
	  if( startAddr >= 0 ) {
	    m[ p++ ] = (byte) startAddr;
	    m[ p++ ] = (byte) (startAddr >> 8);
	  }
	  System.arraycopy( this.dataBytes, this.offs, m, 128, len );
	  fileBytes = m;
	  blkNum    = 0;
	} else if( this.target.equals( Target.KC85 ) ) {
	  int    begAddr = this.fileConvertFrm.getBegAddr( true );
	  int    endAddr = begAddr + len;
	  int    startAddr = this.fileConvertFrm.getStartAddr( false );
	  String fileDesc  = this.fileConvertFrm.getFileDesc( true );

	  byte[] m = new byte[ 128 + len ];
	  Arrays.fill( m, (byte) 0 );
	  int p = 0;
	  if( fileDesc != null ) {
	    int l = fileDesc.length();
	    while( (p < 11) && (p < l) ) {
	      m[ p ] = (byte) fileDesc.charAt( p);
	      p++;
	    }
	  }
	  while( p < 11 ) {
	    m[ p++ ] = (byte) 0x20;
	  }
	  p += 5;
	  m[ p++ ] = (byte) (startAddr >= 0 ? 3 : 2);
	  m[ p++ ] = (byte) begAddr;
	  m[ p++ ] = (byte) (begAddr >> 8);
	  m[ p++ ] = (byte) endAddr;
	  m[ p++ ] = (byte) (endAddr >> 8);
	  if( startAddr >= 0 ) {
	    m[ p++ ] = (byte) startAddr;
	    m[ p++ ] = (byte) (startAddr >> 8);
	  }
	  System.arraycopy( this.dataBytes, this.offs, m, 128, len );
	  fileBytes = m;
	  blkNum    = 1;
	} else if( this.target.equals( Target.KCBASIC_PRG ) ) {
	  String s = this.fileConvertFrm.getFileDesc( true );
	  byte[] m = new byte[ 14 + len ];
	  int    p = 0;
	  m[ p++ ] = (byte) 0xD3;
	  m[ p++ ] = (byte) 0xD3;
	  m[ p++ ] = (byte) 0xD3;
	  if( s != null ) {
	    int l = s.length();
	    int i = 0;
	    while( (p < 11) && (i < l) ) {
	      m[ p++ ] = (byte) s.charAt( i++ );
	    }
	  }
	  while( p < 11 ) {
	    m[ p++ ] = (byte) 0x20;
	  }
	  m[ p++ ] = (byte) len;
	  m[ p++ ] = (byte) (len >> 8);
	  System.arraycopy( this.dataBytes, this.offs, m, p, len );
	  m[ 13 + len ] = (byte) 0x03;
	  fileBytes     = m;
	  blkNum        = 1;
	}
      }
    }
    if( fileBytes != null ) {
      pcm = new KCAudioCreator(
			false,
			blkNum,
			fileBytes,
			0,
			fileBytes.length ).newReader();
    } else {
      pcm = new KCAudioCreator(
			false,
			1,
			this.dataBytes,
			this.offs,
			this.len ).newReader();
    }
    return pcm;
  }


  @Override
  public javax.swing.filechooser.FileFilter getFileFilter()
  {
    return AudioFile.getFileFilter();
  }


  @Override
  public int getMaxFileDescLength()
  {
    return this.target == Target.KC85 ? 11 : 8;
  }


  @Override
  public int getMaxFileTypeLength()
  {
    return this.target == Target.Z9001 ? 3 : 0;
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
    if( this.target == Target.Z9001 ) {
      combo.removeAllItems();
      combo.addItem( "" );
      combo.addItem( "COM" );
      try {
	if( this.fileConvertFrm.getStartAddr( false ) >= 0 ) {
	  combo.setSelectedItem( "COM" );
	}
      }
      catch( UserInputException ex ) {}
      combo.setEnabled( true );
    } else {
      super.setFileTypesTo( combo );
    }
  }


  @Override
  public boolean usesBegAddr()
  {
    return (this.target == Target.Z9001) || (this.target == Target.KC85);
  }


  @Override
  public boolean usesStartAddr( int fileType )
  {
    return (this.target == Target.Z9001) || (this.target == Target.KC85);
  }


	/* --- private Methoden --- */

  private static String createInfoText( Target target )
  {
    String text = "Sound-Datei im KC-Format";
    if( target != null ) {
      switch( target ) {
	case Z9001:
	  text = "Sound-Datei im KC-Systemformat"
			+ " f\u00FCr KC85/1, KC87 und Z9001";
	  break;
	case KC85:
	  text = "Sound-Datei im KC-Systemformat"
			+ " f\u00FCr HC900 und KC85/2..5";
	  break;
	case KCBASIC_PRG:
	  text = "Sound-Datei im KC-BASIC-Format";
	  break;
      }
    }
    return text + " (" + AudioFile.getFileExtensionText() + ")";
  }
}
