/*
 * (c) 2013-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speichern von Bildern
 */

package jkcemu.image;

import java.awt.Component;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Transparency;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuMemView;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileFormat;
import jkcemu.file.FileSaver;
import jkcemu.file.FileUtil;


public class ImageSaver
{
  // Quelltext: /etc/ac1_acc_showimg.asm
  private static final byte[] ac1AccAppMemBytes = {
	(byte) 0x31, (byte) 0x00, (byte) 0x20, (byte) 0x21,
	(byte) 0x20, (byte) 0x20, (byte) 0x11, (byte) 0x00,
	(byte) 0x10, (byte) 0x01, (byte) 0x00, (byte) 0x08,
	(byte) 0xED, (byte) 0xB0, (byte) 0xCD, (byte) 0xFA,
	(byte) 0x07, (byte) 0xE6, (byte) 0x7F, (byte) 0x28,
	(byte) 0xF9, (byte) 0xCD, (byte) 0x08, (byte) 0x00,
	(byte) 0x3E, (byte) 0x0C, (byte) 0xCD, (byte) 0x10,
	(byte) 0x00, (byte) 0xC3, (byte) 0xFD, (byte) 0x07 };

  // Quelltext: /etc/ac1_scch_showimg.asm
  private static final byte[] ac1ScchAppMemBytes = {
	(byte) 0x31, (byte) 0x00, (byte) 0x20, (byte) 0x21,
	(byte) 0x1D, (byte) 0x20, (byte) 0x11, (byte) 0x00,
	(byte) 0x10, (byte) 0x01, (byte) 0x00, (byte) 0x08,
	(byte) 0xED, (byte) 0xB0, (byte) 0xCD, (byte) 0xFA,
	(byte) 0x07, (byte) 0xE6, (byte) 0x7F, (byte) 0x28,
	(byte) 0xF9, (byte) 0x3E, (byte) 0x0C, (byte) 0xCD,
	(byte) 0x10, (byte) 0x00, (byte) 0xC3, (byte) 0xFD,
	(byte) 0x07 };

  // Quelltext: /etc/kc852_showimg_bw.asm
  private static final byte[] kc852BWAppMemBytes = {
	(byte) 0x7F, (byte) 0x7F, (byte) 0x53, (byte) 0x48,
	(byte) 0x4F, (byte) 0x57, (byte) 0x49, (byte) 0x4D,
	(byte) 0x47, (byte) 0x01, (byte) 0x2A, (byte) 0x9C,
	(byte) 0xB7, (byte) 0xE5, (byte) 0x2A, (byte) 0x9E,
	(byte) 0xB7, (byte) 0xE5, (byte) 0x21, (byte) 0x00,
	(byte) 0x00, (byte) 0x22, (byte) 0x9C, (byte) 0xB7,
	(byte) 0x22, (byte) 0xA0, (byte) 0xB7, (byte) 0x21,
	(byte) 0x28, (byte) 0x20, (byte) 0x22, (byte) 0x9E,
	(byte) 0xB7, (byte) 0xDB, (byte) 0x88, (byte) 0xF5,
	(byte) 0xF6, (byte) 0x04, (byte) 0xD3, (byte) 0x88,
	(byte) 0x11, (byte) 0x77, (byte) 0x03, (byte) 0x21,
	(byte) 0x00, (byte) 0x80, (byte) 0xCD, (byte) 0x59,
	(byte) 0x03, (byte) 0x21, (byte) 0x00, (byte) 0xA8,
	(byte) 0x3E, (byte) 0x07, (byte) 0x0E, (byte) 0x0A,
	(byte) 0x06, (byte) 0x00, (byte) 0x77, (byte) 0x23,
	(byte) 0x10, (byte) 0xFC, (byte) 0x0D, (byte) 0x20,
	(byte) 0xF7, (byte) 0xCD, (byte) 0x03, (byte) 0xF0,
	(byte) 0x0E, (byte) 0x30, (byte) 0xFA, (byte) 0xF1,
	(byte) 0xD3, (byte) 0x88, (byte) 0x3E, (byte) 0x0C,
	(byte) 0xCD, (byte) 0x03, (byte) 0xF0, (byte) 0x00,
	(byte) 0xE1, (byte) 0x22, (byte) 0x9E, (byte) 0xB7,
	(byte) 0xE1, (byte) 0x22, (byte) 0x9C, (byte) 0xB7,
	(byte) 0xC9, (byte) 0x1A, (byte) 0x13, (byte) 0xFE,
	(byte) 0x80, (byte) 0xC8, (byte) 0x38, (byte) 0x0D,
	(byte) 0x47, (byte) 0x3E, (byte) 0x01, (byte) 0x90,
	(byte) 0x47, (byte) 0x1A, (byte) 0x13, (byte) 0x77,
	(byte) 0x23, (byte) 0x10, (byte) 0xFC, (byte) 0x18,
	(byte) 0xEC, (byte) 0x3C, (byte) 0x47, (byte) 0x1A,
	(byte) 0x13, (byte) 0x77, (byte) 0x23, (byte) 0x10,
	(byte) 0xFA, (byte) 0x18, (byte) 0xE2 };

  // Quelltext: /etc/kc854_showimg_hires.asm
  private static final byte[] kc854HiresAppMemBytes = {
	(byte) 0x7F, (byte) 0x7F, (byte) 0x53, (byte) 0x48,
	(byte) 0x4F, (byte) 0x57, (byte) 0x49, (byte) 0x4D,
	(byte) 0x47, (byte) 0x01, (byte) 0xDB, (byte) 0x89,
	(byte) 0xF5, (byte) 0xE6, (byte) 0x7F, (byte) 0xD3,
	(byte) 0x89, (byte) 0xDB, (byte) 0x88, (byte) 0xF5,
	(byte) 0xF6, (byte) 0x04, (byte) 0xD3, (byte) 0x88,
	(byte) 0xDD, (byte) 0x7E, (byte) 0x01, (byte) 0xF5,
	(byte) 0xE6, (byte) 0xF1, (byte) 0xEE, (byte) 0x01,
	(byte) 0x28, (byte) 0x02, (byte) 0xF6, (byte) 0x04,
	(byte) 0xD3, (byte) 0x84, (byte) 0xDD, (byte) 0x77,
	(byte) 0x01, (byte) 0xF5, (byte) 0x11, (byte) 0x7A,
	(byte) 0x03, (byte) 0x21, (byte) 0x00, (byte) 0x80,
	(byte) 0xCD, (byte) 0x5C, (byte) 0x03, (byte) 0xF1,
	(byte) 0xF6, (byte) 0x02, (byte) 0xD3, (byte) 0x84,
	(byte) 0xDD, (byte) 0x77, (byte) 0x01, (byte) 0x21,
	(byte) 0x00, (byte) 0x80, (byte) 0xCD, (byte) 0x5C,
	(byte) 0x03, (byte) 0xCD, (byte) 0x03, (byte) 0xF0,
	(byte) 0x0E, (byte) 0x30, (byte) 0xFA, (byte) 0xF1,
	(byte) 0xD3, (byte) 0x84, (byte) 0xDD, (byte) 0x77,
	(byte) 0x01, (byte) 0xF1, (byte) 0xD3, (byte) 0x88,
	(byte) 0xF1, (byte) 0xE6, (byte) 0x80, (byte) 0x47,
	(byte) 0xDB, (byte) 0x89, (byte) 0xE6, (byte) 0x7F,
	(byte) 0xB0, (byte) 0xD3, (byte) 0x89, (byte) 0xC9,
	(byte) 0x1A, (byte) 0x13, (byte) 0xFE, (byte) 0x80,
	(byte) 0xC8, (byte) 0x38, (byte) 0x0D, (byte) 0x47,
	(byte) 0x3E, (byte) 0x01, (byte) 0x90, (byte) 0x47,
	(byte) 0x1A, (byte) 0x13, (byte) 0x77, (byte) 0x23,
	(byte) 0x10, (byte) 0xFC, (byte) 0x18, (byte) 0xEC,
	(byte) 0x3C, (byte) 0x47, (byte) 0x1A, (byte) 0x13,
	(byte) 0x77, (byte) 0x23, (byte) 0x10, (byte) 0xFA,
	(byte) 0x18, (byte) 0xE2 };

  // Quelltext: /etc/kc854_showimg_bw.asm
  private static final byte[] kc854LowresBWAppMemBytes = {
	(byte) 0x7F, (byte) 0x7F, (byte) 0x53, (byte) 0x48,
	(byte) 0x4F, (byte) 0x57, (byte) 0x49, (byte) 0x4D,
	(byte) 0x47, (byte) 0x01, (byte) 0xDB, (byte) 0x88,
	(byte) 0xF5, (byte) 0xF6, (byte) 0x04, (byte) 0xD3,
	(byte) 0x88, (byte) 0xDD, (byte) 0x7E, (byte) 0x01,
	(byte) 0xF5, (byte) 0xE6, (byte) 0xF1, (byte) 0xF6,
	(byte) 0x08, (byte) 0xEE, (byte) 0x01, (byte) 0x28,
	(byte) 0x02, (byte) 0xF6, (byte) 0x04, (byte) 0xD3,
	(byte) 0x84, (byte) 0xDD, (byte) 0x77, (byte) 0x01,
	(byte) 0xF5, (byte) 0x11, (byte) 0x74, (byte) 0x03,
	(byte) 0x21, (byte) 0x00, (byte) 0x80, (byte) 0xCD,
	(byte) 0x56, (byte) 0x03, (byte) 0xF1, (byte) 0xF6,
	(byte) 0x02, (byte) 0xD3, (byte) 0x84, (byte) 0xDD,
	(byte) 0x77, (byte) 0x01, (byte) 0x21, (byte) 0x00,
	(byte) 0x80, (byte) 0x3E, (byte) 0x07, (byte) 0x0E,
	(byte) 0x28, (byte) 0x06, (byte) 0x00, (byte) 0x77,
	(byte) 0x23, (byte) 0x10, (byte) 0xFC, (byte) 0x0D,
	(byte) 0x20, (byte) 0xF7, (byte) 0xCD, (byte) 0x03,
	(byte) 0xF0, (byte) 0x0E, (byte) 0x30, (byte) 0xFA,
	(byte) 0xF1, (byte) 0xD3, (byte) 0x84, (byte) 0xDD,
	(byte) 0x77, (byte) 0x01, (byte) 0xF1, (byte) 0xD3,
	(byte) 0x88, (byte) 0xC9, (byte) 0x1A, (byte) 0x13,
	(byte) 0xFE, (byte) 0x80, (byte) 0xC8, (byte) 0x38,
	(byte) 0x0D, (byte) 0x47, (byte) 0x3E, (byte) 0x01,
	(byte) 0x90, (byte) 0x47, (byte) 0x1A, (byte) 0x13,
	(byte) 0x77, (byte) 0x23, (byte) 0x10, (byte) 0xFC,
	(byte) 0x18, (byte) 0xEC, (byte) 0x3C, (byte) 0x47,
	(byte) 0x1A, (byte) 0x13, (byte) 0x77, (byte) 0x23,
	(byte) 0x10, (byte) 0xFA, (byte) 0x18, (byte) 0xE2 };

  // Quelltext: /etc/llc2_showimg_hires.asm
  private static final byte[] llc2HiresAppMemBytes = {
	(byte) 0x31, (byte) 0x00, (byte) 0x20, (byte) 0x11,
	(byte) 0x39, (byte) 0x20, (byte) 0x21, (byte) 0x00,
	(byte) 0x80, (byte) 0x1A, (byte) 0x13, (byte) 0xFE,
	(byte) 0x80, (byte) 0x28, (byte) 0x19, (byte) 0x38,
	(byte) 0x0D, (byte) 0x47, (byte) 0x3E, (byte) 0x01,
	(byte) 0x90, (byte) 0x47, (byte) 0x1A, (byte) 0x13,
	(byte) 0x77, (byte) 0x23, (byte) 0x10, (byte) 0xFC,
	(byte) 0x18, (byte) 0xEB, (byte) 0x3C, (byte) 0x47,
	(byte) 0x1A, (byte) 0x13, (byte) 0x77, (byte) 0x23,
	(byte) 0x10, (byte) 0xFA, (byte) 0x18, (byte) 0xE1,
	(byte) 0x3E, (byte) 0x50, (byte) 0xD3, (byte) 0xEE,
	(byte) 0xCD, (byte) 0xFA, (byte) 0x07, (byte) 0xE6,
	(byte) 0x7F, (byte) 0x28, (byte) 0xF9, (byte) 0xAF,
	(byte) 0xD3, (byte) 0xEE, (byte) 0xC3, (byte) 0xFD,
	(byte) 0x07 };

  // Quelltext: /etc/z1013_showimg.asm
  private static final byte[] z1013AppMemBytes = {
	(byte) 0x21, (byte) 0x16, (byte) 0x01, (byte) 0x11,
	(byte) 0x00, (byte) 0xEC, (byte) 0x01, (byte) 0x00,
	(byte) 0x04, (byte) 0xED, (byte) 0xB0, (byte) 0xE7,
	(byte) 0x01, (byte) 0x00, (byte) 0x3E, (byte) 0x0C,
	(byte) 0xE7, (byte) 0x00, (byte) 0x00, (byte) 0xC3,
	(byte) 0x38, (byte) 0x00 };

  // Quelltext: /etc/z9001_showimg.asm
  private static final byte[] z9001AppMemBytes = {
	(byte) 0xC3, (byte) 0x0D, (byte) 0x03, (byte) 0x53,
	(byte) 0x48, (byte) 0x4F, (byte) 0x57, (byte) 0x49,
	(byte) 0x4D, (byte) 0x47, (byte) 0x20, (byte) 0x00,
	(byte) 0x00, (byte) 0x0E, (byte) 0x1D, (byte) 0xCD,
	(byte) 0x05, (byte) 0x00, (byte) 0x21, (byte) 0x00,
	(byte) 0xE8, (byte) 0x36, (byte) 0x70, (byte) 0x11,
	(byte) 0x01, (byte) 0xE8, (byte) 0x01, (byte) 0xBF,
	(byte) 0x04, (byte) 0xED, (byte) 0xB0, (byte) 0x21,
	(byte) 0x3E, (byte) 0x03, (byte) 0x11, (byte) 0x00,
	(byte) 0xEC, (byte) 0x01, (byte) 0xC0, (byte) 0x03,
	(byte) 0xED, (byte) 0xB0, (byte) 0x0E, (byte) 0x01,
	(byte) 0xCD, (byte) 0x05, (byte) 0x00, (byte) 0x0E,
	(byte) 0x1E, (byte) 0xCD, (byte) 0x05, (byte) 0x00,
	(byte) 0x1E, (byte) 0x0C, (byte) 0x0E, (byte) 0x02,
	(byte) 0xCD, (byte) 0x05, (byte) 0x00, (byte) 0xC3,
	(byte) 0x00, (byte) 0x00 };

  // Quelltext: /etc/kc85_unpack_image.asm
  private static final byte[] kcUnpackBytes = {
	(byte) 0x21, (byte) 0x1E, (byte) 0x40, (byte) 0x11,
	(byte) 0x00, (byte) 0x80, (byte) 0x3E, (byte) 0xA8,
	(byte) 0xBA, (byte) 0xC8, (byte) 0x7E, (byte) 0x23,
	(byte) 0xFE, (byte) 0x1B, (byte) 0x28, (byte) 0x04,
	(byte) 0x12, (byte) 0x13, (byte) 0x18, (byte) 0xF2,
	(byte) 0x7E, (byte) 0x23, (byte) 0x46, (byte) 0x23,
	(byte) 0x12, (byte) 0x13, (byte) 0x10, (byte) 0xFC,
	(byte) 0x18, (byte) 0xE8 };


  public static void saveAppAC1ACC(
			byte[] videoMemBytes,
			File   file ) throws IOException
  {
    saveApp( file, false, 0x2000, 0x2000, ac1AccAppMemBytes, videoMemBytes );
  }


  public static void saveAppAC1SCCH(
			byte[] videoMemBytes,
			File   file ) throws IOException
  {
    saveApp( file, false, 0x2000, 0x2000, ac1ScchAppMemBytes, videoMemBytes );
  }


  public static void saveAppKC852BW(
			BufferedImage image,
			File          file ) throws IOException
  {
    byte[] pixelIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    ImageUtil.createKC852MonochromeMemBytes( image, pixelIRM );

    byte[] packedPixels = ImageUtil.toPackBits( pixelIRM, true );
    if( packedPixels != null ) {
      if( packedPixels.length > 0 ) {
	saveApp(
		file,
		false,
		0x0300,
		0x030A,
		kc852BWAppMemBytes,
		packedPixels );
      }
    }
  }


  public static void saveAppKC854Hires(
			BufferedImage image,
			File          file ) throws IOException
  {
    byte[] pixelIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    byte[] colorIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    ImageUtil.createKC854HiresMemBytes( image, pixelIRM, colorIRM );

    byte[] packedPixels = ImageUtil.toPackBits( pixelIRM, true );
    byte[] packedColors = ImageUtil.toPackBits( colorIRM, true );
    if( (packedPixels != null) && (packedColors != null) ) {
      if( (packedPixels.length > 0) && (packedColors.length > 0) ) {
	saveApp(
		file,
		false,
		0x0300,
		0x030A,
		kc854HiresAppMemBytes,
		packedPixels,
		packedColors );
      }
    }
  }


  public static void saveAppKC854LowresBW(
			BufferedImage image,
			File          file ) throws IOException
  {
    byte[] pixelIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    ImageUtil.createKC854MonochromeMemBytes( image, pixelIRM );

    byte[] packedPixels = ImageUtil.toPackBits( pixelIRM, true );
    if( packedPixels != null ) {
      if( packedPixels.length > 0 ) {
	saveApp(
		file,
		false,
		0x0300,
		0x030A,
		kc854LowresBWAppMemBytes,
		packedPixels );
      }
    }
  }


  public static void saveAppLLC2Hires(
			byte[] videoMemBytes,
			File   file ) throws IOException
  {
    byte[] packedBytes = ImageUtil.toPackBits( videoMemBytes, false );
    if( packedBytes != null ) {
      if( packedBytes.length > 0 ) {
	saveApp(
		file,
		false,
		0x2000,
		0x2000,
		llc2HiresAppMemBytes,
		packedBytes );
      }
    }
  }


  public static void saveAppZ1013(
			byte[] videoMemBytes,
			File   file ) throws IOException
  {
    saveApp( file, true, 0x0100, 0x0100, z1013AppMemBytes, videoMemBytes );
  }


  public static void saveAppZ9001(
			byte[] videoMemBytes,
			File   file ) throws IOException
  {
    saveApp( file, true, 0x0300, 0x0300, z9001AppMemBytes, videoMemBytes );
  }


  public static File saveImageAs(
			Frame         owner,
			BufferedImage image,
			ExifData      exifData,
			File          presetFile )
  {
    boolean         monochrome = false;
    IndexColorModel icm        = ImageUtil.getIndexColorModel( image );
    if( icm != null ) {
      if( icm.getMapSize() == 2 ) {
	monochrome = true;
      }
    }
    if( presetFile == null ) {
      presetFile = Main.getLastDirFile( Main.FILE_GROUP_IMAGE );
    }
    File file = FileUtil.showFileSaveDlg(
				owner,
				"Bilddatei speichern",
				presetFile,
				createFileFilters( monochrome ) );
    if( file != null ) {
      if( exifData != null ) {
	exifData = exifData.copyForNewEncoding(
					image.getWidth(),
					image.getHeight() );
      }
      boolean open = false;
      try {
	boolean done = false;
	if( IFFFile.accept( file ) ) {
	  IFFFile.writeImage( file, image, exifData );
	  done = true;
	} else {

	  /*
	   * Speichern ueber ImageIO
	   *
	   * Augrund der Anpassung der Metadaten wird anhand
	   * der Dateiendung ein passender ImageWriter ermittelt.
	   */
	  String fName = file.getName();
	  if( fName != null ) {
	    int idx = fName.lastIndexOf( '.' );
	    if( (idx >= 0) && ((idx + 1) < fName.length()) ) {
	      String suffix = fName.substring( idx + 1 ).toLowerCase();
	      Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix(
								suffix );
	      if( iter.hasNext() ) {

		// Dateiformat wird unterstuetzt
		if( suffix.equalsIgnoreCase( "jpeg" )
		    || suffix.equalsIgnoreCase( "jpg" ) )
		{
		  OutputStream out = null;
		  try {
		    open = true;
		    out  = new JPEGExifOutputStream(
				new BufferedOutputStream(
					new FileOutputStream( file ) ),
				exifData );
		    done = ImageIO.write( image, "JPEG", out );
		    out.close();
		    out  = null;
		  }
		  finally {
		    EmuUtil.closeSilently( out );
		  }
		}
		else if( suffix.equalsIgnoreCase( "bmp" )
			 && (image.getTransparency()
					!= Transparency.OPAQUE) )
		{
		  throw new IOException(
			"Das Bild enth\u00E4lt transparente Pixel,\n"
				+ "die nicht im BMP-Format gespeichert"
				+ " werden k\u00F6nnen." );
		}
		else if( suffix.equalsIgnoreCase( "wbmp" ) && !monochrome ) {
		  throw new IOException(
			"Das WBMP-Format kann nur monochrome Bilder"
				+ " (max. 2 Farben) speichern." );
		} else {
		  ImageWriter imgWriter   = iter.next();
		  IIOMetadata imgMetadata = null;
		  if( suffix.equalsIgnoreCase( "gif" ) ) {
		    imgMetadata = prepareImgMetadataGIF(
						image,
						imgWriter,
						exifData );
		  } else if( suffix.equalsIgnoreCase( "png" ) ) {
		    imgMetadata = prepareImgMetadataPNG(
						image,
						imgWriter,
						exifData );
		  }
		  ImageOutputStream out = null;
		  try {
		    open = true;
		    out  = ImageIO.createImageOutputStream( file );
		    try {
		      imgWriter.setOutput( out );
		      imgWriter.write(
				null,
				new IIOImage( image, null, imgMetadata ),
				null );
		    }
		    finally {
		      imgWriter.dispose();
		    }
		    out.close();
		    out  = null;
		    done = true;
		  }
		  catch( Exception ex ) {
		    if( ex instanceof IOException ) {
		      throw ex;
		    }
		  }
		  finally {
		    EmuUtil.closeSilently( out );
		  }
		}
	      }
	    }
	  }
	}
	if( done ) {
	  Main.setLastFile( file, Main.FILE_GROUP_IMAGE );
	} else {
	  if( open ) {	
	    throw new IOException(
			"Das Bild l\u00E4sst sich mit seinen Eigenschaften"
				+ " nicht\n"
				+ "in dem durch die Dateiendung"
				+ " angegebenen Format speichern." );
	  } else {
	    throw new IOException(
			ImageUtil.createFileSuffixNotSupportedMsg(
				ImageIO.getWriterFileSuffixes(),
				IFFFile.getFileSuffixes() ) );
	  }
	}
      }
      catch( IOException ex ) {
	if( open && (file != null) ) {
	  file.delete();
	}
	BaseDlg.showErrorDlg( owner, ex.getMessage() );
	file = null;
      }
    }
    return file;
  }


  /*
   * Die Methode speichert ein Bild im A5105-Format.
   * Das BufferedImage muss dazu ein IndexColorModel haben,
   * welches den 16 Farben des A5105 entspricht.
   */
  public static void saveImageA5105(
			Component     owner,
			BufferedImage image,
			File          file ) throws IOException
  {
    IndexColorModel icm = ImageUtil.getIndexColorModel( image );
    if( icm != null ) {
      int          w   = image.getWidth();
      int          h   = image.getHeight();
      OutputStream out = null;
      try {
	out = new FileOutputStream( file );

	Map<Integer,Integer> rgb2Idx = new HashMap<>();
	out.write( 0xFD );		// Kennung Videospeicher
	out.write( 0x00 );		// Anfangsadresse Grafikseite 0
	out.write( 0x40 );
	int wordCnt = ImageUtil.A5105_H * 80;
	int endAddr = 0x4000 + wordCnt - 1;
	out.write( endAddr & 0xFF );	// Endadresse
	out.write( (endAddr >> 8) & 0xFF );
	out.write( 0x00 );		// Startadresse
	out.write( 0x40 );
	for( int y = 0; y < ImageUtil.A5105_H; y++ ) {
	  int x = 0;
	  for( int i = 0; i < 80; i++ ) {	// 80 Datenworte
	    int b0 = 0;
	    int b1 = 0;
	    for( int p = 0; p < 4; p++ ) {	// je 4 Pixel
	      b0 >>= 1;
	      b1 >>= 1;
	      int rgb = 0;
	      if( (x < w) && (y < h) ) {
		rgb = image.getRGB( x++, y );
	      }
	      Integer idx = rgb2Idx.get( rgb );
	      if( idx == null ) {
		idx = ImageUtil.getNearestIndex( icm, rgb );
		rgb2Idx.put( rgb, idx );
	      }
	      int v = idx.intValue();
	      if( (v & 0x01) != 0 ) {
		b0 |= 0x08;
	      }
	      if( (v & 0x02) != 0 ) {
		b0 |= 0x80;
	      }
	      if( (v & 0x04) != 0 ) {
		b1 |= 0x08;
	      }
	      if( (v & 0x08) != 0 ) {
		b1 |= 0x80;
	      }
	    }
	    out.write( b0 );
	    out.write( b1 );
	  }
	}
	out.write( 0x1A );
	int len  = 8 + (2 * wordCnt);
	while( (len & 0x007F) != 0 ) {
	  out.write( 0 );
	  len++;
	}
	out.close();
	out = null;
      }
      finally {
	EmuUtil.closeSilently( out );
      }
    }
  }


  // Die Methode speichert ein Bild im KC85/2-Format ohne Farben.
  public static void saveImageKC852Monochrome(
			BufferedImage image,
			File          file ) throws IOException
  {
    byte[] pixelIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    ImageUtil.createKC852MonochromeMemBytes( image, pixelIRM );
    saveKC85File( file, 1, 3, pixelIRM );
  }


  /*
   * Die Methode speichert ein Bild im KC85/4-HIRES-Format.
   * Dabei werden zwei Dateien erzeugt, naemlich *.hip und *.hif.
   */
  public static void saveImageKC854Hires(
			BufferedImage image,
			File          file ) throws IOException
  {
    File   dirFile = file.getParentFile();
    String fName   = file.getName();
    if( fName == null ) {
      throwInvalidKC854HiresFileSuffix();
    }
    String baseName = "";
    int    pos      = fName.lastIndexOf( '.' );
    if( pos >= 0 ) {
      baseName = fName.substring( 0, pos + 1 );
    } else {
     baseName = fName + ".";
    }

    byte[] pixelIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    byte[] colorIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    ImageUtil.createKC854HiresMemBytes( image, pixelIRM, colorIRM );

    saveKC85File(
		dirFile != null ?
			new File( dirFile, baseName + "hip" )
			: new File( baseName + "hip" ),
		-1,
		-1,
		pixelIRM );

    saveKC85File(
		dirFile != null ?
			new File( dirFile, baseName + "hif" )
			: new File( baseName + "hif" ),
		-1,
		-1,
		colorIRM );
  }


  // Die Methode speichert ein Bild im KC85/4-LOWRES-Format ohne Farben.
  public static void saveImageKC854Monochrome(
			BufferedImage image,
			File          file ) throws IOException
  {
    byte[] pixelIRM = new byte[ ImageUtil.KC85_COLS * ImageUtil.KC85_H ];
    ImageUtil.createKC854MonochromeMemBytes( image, pixelIRM );
    saveKC85File( file, 5, 7, pixelIRM );
  }


	/* --- private Methoden --- */

  private static void appendPNGTextEntryTo(
				IIOMetadataNode parentNode,
				String          keyword,
				String          value )
  {
    if( value != null ) {
      IIOMetadataNode childNode = new IIOMetadataNode( "tEXtEntry" );
      childNode.setAttribute( "keyword", keyword );
      childNode.setAttribute( "value", value );
      parentNode.appendChild( childNode );
    }
  }


  private static EmuMemView createEmuMemView(
				final int       begAddr,
				final byte[]... data )
  {
    return new EmuMemView()
		{
		  @Override
		  public int getBasicMemByte( int addr )
		  {
		    return getMemByte( addr, false );
		  }

		  @Override
		  public int getMemByte( int addr, boolean m1 )
		  {
		    int rv  = 0;
		    int idx = addr - begAddr;
		    for( byte[] a : data ) {
		      if( a != null ) {
			if( idx < 0 ) {
			  break;
			}
			if( idx < a.length ) {
			  rv = (int) a[ idx ] & 0xFF;
			  break;
			}
			idx -= a.length;
		      }
		    }
		    return rv;
		  }

		  @Override
		  public int getMemWord( int addr )
		  {
		    return ((getMemByte( addr + 1, false ) << 8) & 0xFF00)
				| (getMemByte( addr, false ) & 0x00FF);
		  }
		};
  }


  private static javax.swing.filechooser.FileFilter[] createFileFilters(
							boolean monochrome )
  {
    javax.swing.filechooser.FileFilter[] rv = null;
    try {
      String[] suffixes = ImageIO.getWriterFileSuffixes();
      if( suffixes != null ) {
	java.util.List<javax.swing.filechooser.FileFilter> filters
							= new ArrayList<>();
	/*
	 * JPG/JPEG und TIF/TIFF zusammenfassen
	 * Dazu die Suffixes sortieren, wodurch beim gleichen Typ
	 * die laengeren hinter den kuerzeren stehen.
	 */
	SortedSet<String> suffixSet = new TreeSet<>();
	for( int i = 0; i < suffixes.length; i++ ) {
	  String s = suffixes[ i ];
	  if( s != null ) {
	    if( !s.isEmpty() ) {
	      suffixSet.add( s.toUpperCase() );
	    }
	  }
	}
	if( !monochrome ) {
	  suffixSet.remove( "WBMP" );
	}
	while( !suffixSet.isEmpty() ) {
	  String s = suffixSet.first();
	  if( s.equals( "JPEG" ) ) {
	    if( suffixSet.remove( "JPG" ) ) {
	      filters.add(
		new javax.swing.filechooser.FileNameExtensionFilter(
					"JPEG-Dateien (*.jpg; *.jpeg)",
					"jpeg", "jpg" ) );
	    }
	  } else if( s.equals( "TIF" ) ) {
	    if( suffixSet.remove( "TIFF" ) ) {
	      filters.add(
		new javax.swing.filechooser.FileNameExtensionFilter(
					"TIFF-Dateien (*.tif; *.tiff)",
					"tiff", "tif" ) );
	    }
	  } else {
	    filters.add(
		new javax.swing.filechooser.FileNameExtensionFilter(
					String.format(
						"%s-Dateien (*.%s)",
						s,
						s.toLowerCase() ),
					s ) );
	  }
	  suffixSet.remove( s );
	}
	filters.add( IFFFile.getImageFileFilter() );
	rv = filters.toArray(
		new javax.swing.filechooser.FileFilter[ filters.size() ] );
      }
    }
    catch( ArrayStoreException ex ) {}
    return rv;
  }


  private static void saveApp(
			File      file,
			boolean   tapForZ9001,
			int       begAddr,
			int       startAddr,
			byte[]... memBytes ) throws IOException
  {
    FileFormat fmt      = FileFormat.BIN;
    String     appName  = "SHOWIMG COM";
    String     fileName = file.getName();
    if( fileName != null ) {
      fileName = fileName.toLowerCase();
      if( fileName.endsWith( ".hex" ) ) {
	fmt = FileFormat.INTELHEX;
      } else if( fileName.endsWith( ".kcc" ) ) {
	fmt = FileFormat.KCC;
      } else if( fileName.endsWith( ".rmc" ) ) {
	fmt = FileFormat.RMC;
      } else if( fileName.endsWith( ".tap" ) ) {
	if( tapForZ9001 ) {
	  fmt = FileFormat.KCTAP_Z9001;
	} else {
	  fmt = FileFormat.KCTAP_KC85;
	}
      } else if( fileName.endsWith( ".z80" ) ) {
	fmt     = FileFormat.HEADERSAVE;
	appName = "ShowImage";
      }
    }
    int len = 0;
    for( byte[] a : memBytes ) {
      if( a != null ) {
	len += a.length;
      }
    }
    FileSaver.saveFile(
		file,
		fmt,
		createEmuMemView( begAddr, memBytes ),
		begAddr,
		begAddr + len - 1,
		false,
		begAddr,
		startAddr,
		appName,
		"C" );
  }


  private static void saveKC85File(
				File   file,
				int    typeByteUncompressed,
				int    typeByteCompressed,
				byte[] irm ) throws IOException
  {
    OutputStream out = null;
    try {

      // Daten komprimieren
      ByteArrayOutputStream buf = new ByteArrayOutputStream( 0x2800 );
      buf.write( kcUnpackBytes );

      int pos = 0;
      while( pos < 0x2800 ) {
	int  nEqual = 1;
	byte b0     = (byte) 0;
	if( irm != null ) {
	  if( pos < irm.length ) {
	    b0 = irm[ pos ];
	  }
	}
	int p2 = pos + 1;
	while( (nEqual < 256) && (p2 < 0x2800) ) {
	  byte b1 = (byte) 0;
	  if( irm != null ) {
	    if( p2 < irm.length ) {
	      b1 = irm[ p2 ];
	    }
	  }
	  if( b1 != b0 ) {
	    break;
	  }
	  nEqual++;
	  p2++;
	}
	if( (nEqual > 3) || (b0 == (byte) 0x1B) ) {
	  buf.write( 0x1B );
	  buf.write( b0 );
	  buf.write( nEqual );
	  pos += nEqual;
	} else {
	  buf.write( b0 );
	  pos++;
	}
      }
      buf.close();
      if( buf.size() >= 0x2800 ) {
	buf = null;
      }

      // Datei schreiben
      out = new FileOutputStream( file );
      for( int i = 0; i < 16; i++ ) {
	out.write( 0 );
      }
      if( (typeByteUncompressed >= 0) || (typeByteCompressed >= 0) ) {
	out.write( 2 );				// Anzahl Argumente
	out.write( 0xFF );			// Anfangsadresse
	out.write( 0x3F );
      } else {
	out.write( buf != null ? 3 : 2 );	// Anzahl Argumente
	out.write( 0 );				// Anfangsadresse
	out.write( 0x40 );
      }
      if( buf != null ) {
	int endAddr = 0x4000 + buf.size();
	out.write( endAddr );			// Endadresse
	out.write( endAddr >> 8 );
	out.write( 0 );				// Startadresse
	out.write( 0x40 );
	for( int i = 0; i < 105; i++ ) {
	  out.write( 0 );
	}
	if( typeByteCompressed >= 0 ) {
	  out.write( typeByteCompressed );
	}
	buf.writeTo( out );
	int len = buf.size();
	while( (len & 0x007F) != 0 ) {
	  out.write( 0 );
	  len++;
	}
      } else {
	out.write( 0 );			// Endadresse
	out.write( 0x68 );
	for( int i = 0; i < 107; i++ ) {
	  out.write( 0 );
	}
	if( typeByteUncompressed >= 0 ) {
	  out.write( typeByteUncompressed );
	}
	int len = 0;
	if( irm != null ) {
	  len = Math.min( irm.length, 0x2800 );
	  if( len > 0 ) {
	    out.write( irm, 0, len );
	  }
	}
	while( len < 0x2800 ) {
	  out.write( 0 );
	  len++;
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilently( out );
    }
  }


  private static IIOMetadata prepareImgMetadataGIF(
					BufferedImage image,
					ImageWriter   imgWriter,
					ExifData      exifData )
  {
    IIOMetadata imgMetadata = null;
    if( exifData != null ) {
      String comment = exifData.getComment();
      if( comment == null ) {
	comment = exifData.getImageDesc();
      }
      if( comment != null ) {
	try {
	  imgMetadata = imgWriter.getDefaultImageMetadata(
		ImageTypeSpecifier.createFromRenderedImage( image ),
		null );
	  if( imgMetadata != null ) {
	    IIOMetadataNode imgMetaRoot =
			(IIOMetadataNode) imgMetadata.getAsTree(
				imgMetadata.getNativeMetadataFormatName() );

	    IIOMetadataNode commentNode = new IIOMetadataNode(
						"CommentExtension" );
	    commentNode.setAttribute( "value", comment );

	    IIOMetadataNode commentsNode = new IIOMetadataNode(
						"CommentExtensions" );
	    commentsNode.appendChild( commentNode );

	    imgMetaRoot.appendChild( commentsNode );

	    imgMetadata.setFromTree(
			imgMetadata.getNativeMetadataFormatName(),
			imgMetaRoot );
	  }
	}
	catch( Exception ex ) {}
      }
    }
    return imgMetadata;
  }


  private static IIOMetadata prepareImgMetadataPNG(
					BufferedImage image,
					ImageWriter   imgWriter,
					ExifData      exifData )
  {
    IIOMetadata imgMetadata = null;
    if( exifData != null ) {
      String         docName   = exifData.getDocumentName();
      String         imgDesc   = exifData.getImageDesc();
      String         author    = exifData.getAuthor();
      String         copyright = exifData.getCopyright();
      String         comment   = exifData.getComment();
      String         software  = exifData.getSoftware();
      java.util.Date date      = exifData.getDate();
      String         timeZone  = exifData.getTimeZone();
      if( (docName != null)
	  || (imgDesc != null)
	  || (author != null)
	  || (copyright != null)
	  || (comment != null)
	  || (software != null)
	  || (date != null) )
      {
	try {
	  imgMetadata = imgWriter.getDefaultImageMetadata(
		ImageTypeSpecifier.createFromRenderedImage( image ),
		null );
	  if( imgMetadata != null ) {
	    IIOMetadataNode imgMetaRoot =
			(IIOMetadataNode) imgMetadata.getAsTree(
				imgMetadata.getNativeMetadataFormatName() );

	    IIOMetadataNode tEXtChunk = new IIOMetadataNode( "tEXt" );
	    imgMetaRoot.appendChild( tEXtChunk );

	    appendPNGTextEntryTo( tEXtChunk, "Title", docName );
	    appendPNGTextEntryTo( tEXtChunk, "Description", imgDesc );
	    appendPNGTextEntryTo( tEXtChunk, "Author", author );
	    appendPNGTextEntryTo( tEXtChunk, "Copyright", copyright );
	    appendPNGTextEntryTo( tEXtChunk, "Comment", comment );
	    appendPNGTextEntryTo( tEXtChunk, "Software", software );
	    if( date != null ) {
	      String dateText = null;
	      if( timeZone != null ) {
		if( timeZone.isEmpty() ) {
		  timeZone = null;
		}
	      }
	      if( timeZone != null ) {
		dateText = String.format(
				"%s %s",
				(new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss",
					Locale.US )).format( date ),
				timeZone );
	      } else {
		dateText = (new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss Z",
					Locale.US )).format( date );
	      }
	      appendPNGTextEntryTo(
				tEXtChunk,
				"Creation Time",
				dateText );
	    }

	    imgMetadata.setFromTree(
			imgMetadata.getNativeMetadataFormatName(),
			imgMetaRoot );
	  }
	}
	catch( Exception ex ) {}
      }
    }
    return imgMetadata;
  }


  private static void throwInvalidKC854HiresFileSuffix()
						throws IOException
  {
    throw new IOException(
	"Der Dateiname muss die Endung *.hif oder *.hip haben." );
  }


	/* --- Konstruktor --- */

  private ImageSaver()
  {
    // nicht instanziierbar
  }
}
