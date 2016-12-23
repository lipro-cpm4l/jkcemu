/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer Paintshop Pro Farbpalettendateien
 */

package jkcemu.image;

import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.base.EmuUtil;


public class JASCPaletteFile
{
  private static final String   MAGIC        = "JASC-PAL";
  private static final String[] fileSuffixes = { "pal" };

  private static FileNameExtensionFilter fileFilter = null;


  public static boolean accept( File file )
  {
    return EmuUtil.accept( file, fileSuffixes );
  }


  public static FileNameExtensionFilter getFileFilter()
  {
    if( fileFilter == null ) {
      fileFilter = ImgUtil.createFileFilter(
				"Paintshop Pro Farbpalettendatei",
				fileSuffixes );
    }
    return fileFilter;
  }


  public static String[] getFileSuffixes()
  {
    return fileSuffixes;
  }


  public static IndexColorModel read( File file ) throws IOException
  {
    IndexColorModel icm = null;
    BufferedReader  in  = null;
    try {
      in = new BufferedReader( new FileReader( file ) );
      String line = in.readLine();
      if( line == null ) {
	EmuUtil.throwUnsupportedFileFormat();
      }
      if( !line.equals( MAGIC ) ) {
	EmuUtil.throwUnsupportedFileFormat();
      }
      in.readLine();		// Zeile mit 0100 ueberlesen
      line = in.readLine();
      if( line == null ) {
	EmuUtil.throwUnsupportedFileFormat();
      }
      try {
	int colorCnt= Integer.parseInt( line.trim() );
	if( colorCnt < 1 ) {
	  EmuUtil.throwUnsupportedFileFormat();
	}
	byte[] reds   = new byte[ colorCnt ];
	byte[] greens = new byte[ colorCnt ];
	byte[] blues  = new byte[ colorCnt ];
	for( int i = 0; i < colorCnt; i++ ) {
	  line = in.readLine();
	  if( line == null ) {
	    EmuUtil.throwUnsupportedFileFormat();
	  }
	  String[] elems = line.split( "\\s" );
	  if( elems == null ) {
	    EmuUtil.throwUnsupportedFileFormat();
	  }
	  if( elems.length != 3 ) {
	    EmuUtil.throwUnsupportedFileFormat();
	  }

	  int r = Integer.parseInt( elems[ 0 ].trim() );
	  if( (r < 0) || (r > 0xFF) ) {
	    EmuUtil.throwUnsupportedFileFormat();
	  }
	  reds[ i ] = (byte) r;

	  int g = Integer.parseInt( elems[ 1 ].trim() );
	  if( (g < 0) || (g > 0xFF) ) {
	    EmuUtil.throwUnsupportedFileFormat();
	  }
	  greens[ i ] = (byte) g;

	  int b = Integer.parseInt( elems[ 2 ].trim() );
	  if( (b < 0) || (b > 0xFF) ) {
	    EmuUtil.throwUnsupportedFileFormat();
	  }
	  blues[ i ] = (byte) b;
	}
	icm = ImgUtil.createIndexColorModel(
					colorCnt,
					reds,
					greens,
					blues,
					null );
      }
      catch( NumberFormatException ex ) {
	EmuUtil.throwUnsupportedFileFormat();
      }
    }
    finally {
      EmuUtil.closeSilent( in );
    }
    if( icm == null ) {
      ImgUtil.throwNoColorTab();
    }
    return icm;
  }


  public static void write(
			File            file,
			IndexColorModel icm ) throws IOException
  {
    Writer out = null;
    try {
      int colorCnt = icm.getMapSize();
      out = new FileWriter( file );
      out.write( MAGIC );
      writeNewLine( out );
      out.write( "0100" );
      writeNewLine( out );
      out.write( String.valueOf( colorCnt ) );
      writeNewLine( out );
      for( int i = 0; i < colorCnt; i++ ) {
	int rgb = icm.getRGB( i );
	out.write(
		String.format(
			"%d %d %d",
			(rgb >> 16) & 0xFF,
			(rgb >> 8) & 0xFF,
			rgb & 0xFF ) );
	writeNewLine( out );
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
  }


	/* --- Konstruktor --- */

  private JASCPaletteFile()
  {
    // leer
  }


	/* --- private Methoden --- */

  private static void writeNewLine( Writer out ) throws IOException
  {
    out.write( "\r\n" );
  }
}
