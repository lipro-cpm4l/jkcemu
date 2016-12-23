/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die interne Speicherung eines Bildes
*/

package jkcemu.image;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.lang.*;


public class ImgEntry
{
  public enum Mode {
		UNSPECIFIED, INDEXED_COLORS, GRAY, MONOCHROME,
		INVERTED, INVERTED_INDEXED_COLORS,
		INVERTED_GRAY, INVERTED_MONOCHROME,
		A5105,
		AC1_ACC, AC1_SCCH, AC1_2010,
		KC854HIRES,
		Z1013,
		Z9001 };


  private BufferedImage   image;
  private Mode            mode;
  private ImgFld.Rotation rotation;
  private String          title;
  private File            file;
  private byte[]          memBytes;


  public ImgEntry(
		BufferedImage   image,
		Mode            mode,
		ImgFld.Rotation rotation,
		String          title,
		File            file,
		byte[]          memBytes )
  {
    this.image    = image;
    this.mode     = mode;
    this.rotation = rotation;
    this.title    = title;
    this.file     = file;
    this.memBytes = memBytes;
  }


  public boolean equalsMode( Mode mode )
  {
    return this.mode.equals( mode );
  }


  public File getFile()
  {
    return this.file;
  }


  public int getHeight()
  {
    return this.image.getHeight();
  }


  public BufferedImage getImage()
  {
    return this.image;
  }


  public byte[] getMemBytes()
  {
    return this.memBytes;
  }


  public Mode getMode()
  {
    return this.mode;
  }


  public ImgFld.Rotation getRotation()
  {
    return this.rotation;
  }


  public String getTitle()
  {
    return this.title;
  }


  public int getWidth()
  {
    return this.image.getWidth();
  }


  public boolean isAGrayMode()
  {
    return this.mode.equals( Mode.GRAY )
		|| this.mode.equals( Mode.INVERTED_GRAY );
  }


  public boolean isAInversionMode()
  {
    return this.mode.equals( Mode.INVERTED )
		|| this.mode.equals( Mode.INVERTED_INDEXED_COLORS )
		|| this.mode.equals( Mode.INVERTED_GRAY )
		|| this.mode.equals( Mode.INVERTED_MONOCHROME );
  }


  public boolean isAMonochromeMode()
  {
    return this.mode.equals( Mode.MONOCHROME )
		|| this.mode.equals( Mode.INVERTED_MONOCHROME )
		|| this.mode.equals( Mode.AC1_ACC )
		|| this.mode.equals( Mode.AC1_SCCH )
		|| this.mode.equals( Mode.AC1_2010 )
		|| this.mode.equals( Mode.Z1013 )
		|| this.mode.equals( Mode.Z9001 );
  }


  public boolean isA5105Format()
  {
    return this.mode.equals( Mode.A5105 );
  }


  public boolean isAC1Format()
  {
    return this.mode.equals( Mode.AC1_ACC )
		|| this.mode.equals( Mode.AC1_SCCH )
		|| this.mode.equals( Mode.AC1_2010 );
  }


  public boolean isKC854HiresFormat()
  {
    return this.mode.equals( Mode.KC854HIRES );
  }


  public boolean isLLC2HiresFormat()
  {
    return this.mode.equals( Mode.MONOCHROME )
		|| this.mode.equals( Mode.INVERTED_MONOCHROME );
  }


  public boolean isZ1013Format()
  {
    return this.mode.equals( Mode.Z1013 );
  }


  public boolean isZ9001Format()
  {
    return this.mode.equals( Mode.Z9001 )
	   && (getWidth() == ImgUtil.Z9001_W)
	   && (getHeight() == ImgUtil.Z9001_H);
  }


  public static Mode probeMode( BufferedImage image )
  {
    Mode rv = Mode.UNSPECIFIED;
    if( image != null ) {
      IndexColorModel icm = ImgUtil.getIndexColorModel( image );
      if( icm != null ) {
	int n = icm.getMapSize();
	if( n > 0 ) {
	  boolean gray = true;
	  boolean mono = true;
	  for( int i = 0; i < n; i++ ) {
	    int rgb = icm.getRGB( i );
	    int c   = rgb & 0xFF;
	    if( (c != ((rgb >> 16) & 0xFF)) || (c != ((rgb >> 8) & 0xFF)) ) {
	      gray = false;
	      mono = false;
	      break;
	    }
	    if( (c != 0) && (c != 0xFF) ) {
	      mono = false;
	    }
	  }
	  if( mono ) {
	    rv = Mode.MONOCHROME;
	  } else if( gray ) {
	    rv = Mode.GRAY;
	  }
	}
      } else {
	int imgType = image.getType();
	if( (imgType == BufferedImage.TYPE_BYTE_GRAY)
	    || (imgType == BufferedImage.TYPE_USHORT_GRAY) )
	{
	  rv = Mode.GRAY;
	}
      }
    }
    return rv;
  }


  public void setFile( File file )
  {
    this.file = file;
  }


  public void setMemBytes( byte[] memBytes )
  {
    this.memBytes = memBytes;
  }


  public void setRotation( ImgFld.Rotation rotation )
  {
    this.rotation = rotation;
  }
}
