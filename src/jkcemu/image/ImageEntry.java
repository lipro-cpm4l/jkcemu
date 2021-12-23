/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die interne Speicherung eines Bildes
 */

package jkcemu.image;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImageEntry
{
  public enum Action {
		INITIAL_LOADED,
		CHANGED,
		HORIZONTAL_FLIPPED,
		INVERTED,
		SHARPENED,
		SOFTENED };

  public enum Mode {
		UNSPECIFIED, INDEXED_COLORS, GRAY, MONOCHROME,
		A5105,
		AC1_ACC, AC1_SCCH, AC1_2010,
		KC854_HIRES,
		Z1013,
		Z9001 };


  private BufferedImage     image;
  private ExifData          exifData;
  private Action            action;
  private Mode              mode;
  private ImageFld.Rotation rotation;
  private String            title;
  private File              file;
  private byte[]            memBytes;


  public ImageEntry(
		BufferedImage     image,
		ExifData          exifData,
		Action            action,
		Mode              mode,
		ImageFld.Rotation rotation,
		String            title,
		File              file,
		byte[]            memBytes )
  {
    this.image    = image;
    this.exifData = exifData;
    this.action   = action;
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


  public Action getAction()
  {
    return this.action;
  }


  public ExifData getExifData()
  {
    return this.exifData;
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


  public ImageFld.Rotation getRotation()
  {
    return this.rotation;
  }


  public ExifData getSharedExifDataCopyForChangedImage()
  {
    return this.exifData != null ?
		this.exifData.getSharedCopyForChangedImage()
		: null;
  }


  public ExifData getSharedExifDataCopyForResizedImage()
  {
    return this.exifData != null ?
		this.exifData.getSharedCopyForResizedImage()
		: null;
  }


  public String getTitle()
  {
    return this.title;
  }


  public int getWidth()
  {
    return this.image.getWidth();
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


  public boolean isGray()
  {
    return this.mode.equals( Mode.GRAY );
  }


  public boolean isKC854HiresFormat()
  {
    return this.mode.equals( Mode.KC854_HIRES );
  }


  public boolean isKC85MonochromeFormat()
  {
    return this.mode.equals( Mode.MONOCHROME )
		&& (getWidth() == ImageUtil.KC85_W)
		&& (getHeight() == ImageUtil.KC85_H);
  }


  public boolean isLLC2HiresFormat()
  {
    return this.mode.equals( Mode.MONOCHROME )
		&& (getWidth() == ImageUtil.LLC2_W)
		&& (getHeight() == ImageUtil.LLC2_H);
  }


  public boolean isMonochrome()
  {
    return this.mode.equals( Mode.MONOCHROME )
		|| this.mode.equals( Mode.AC1_ACC )
		|| this.mode.equals( Mode.AC1_SCCH )
		|| this.mode.equals( Mode.AC1_2010 )
		|| this.mode.equals( Mode.Z1013 )
		|| this.mode.equals( Mode.Z9001 );
  }


  public boolean isZ1013Format()
  {
    return this.mode.equals( Mode.Z1013 );
  }


  public boolean isZ9001Format()
  {
    return this.mode.equals( Mode.Z9001 );
  }


  public void setFile( File file )
  {
    this.file = file;
  }


  public void setMemBytes( byte[] memBytes )
  {
    this.memBytes = memBytes;
  }


  public void setRotation( ImageFld.Rotation rotation )
  {
    this.rotation = rotation;
  }
}
