/*
 * (c) 2016-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer CSW-Dateien
 */

package jkcemu.audio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileInfo;
import jkcemu.file.FileUtil;


public class CSWFile
{
  private static class CSWFormat
  {
    private long    sampleRate;
    private boolean initialPhase;
    private int     headerLen;

    private CSWFormat(
		long    sampleRate,
		boolean initialPhase,
		int     headerLen )
    {
      this.sampleRate   = sampleRate;
      this.initialPhase = initialPhase;
      this.headerLen    = headerLen;
    }
  };


  private static final String[] fileExts = { "csw", "csw1" };

  private static javax.swing.filechooser.FileFilter fileFilter = null;


  public static String[] getFileExtensions()
  {
    return fileExts;
  }


  public static String getFileExtensionText()
  {
    return "*.csw; *.csw1";
  }


  public static javax.swing.filechooser.FileFilter getFileFilter()
  {
    if( fileFilter == null ) {
      fileFilter = new FileNameExtensionFilter(
			"CSW-Dateien (" + getFileExtensionText() + ")",
			fileExts );
    }
    return fileFilter;
  }


  public static BitSampleBuffer getBitSampleBuffer(
					byte[] fileBytes,
					int    pos ) throws IOException
  {
    CSWFormat       fmt = parseHeader( fileBytes, pos );
    BitSampleBuffer buf = new BitSampleBuffer(
					(int) fmt.sampleRate,
					0x8000 );
    int     dataPos = fmt.headerLen;
    int     dataLen = fileBytes.length - fmt.headerLen;
    boolean phase   = fmt.initialPhase;
    while( (dataPos < fileBytes.length) && (dataLen > 0) ) {
      int b = (int) fileBytes[ dataPos++ ] & 0xFF;
      --dataLen;
      if( b > 0 ) {
	buf.addSamples( b, phase );
	phase = !phase;
      } else {
	if( ((dataPos + 3) >= fileBytes.length) && (dataLen > 3) ) {
	  break;
	}
	long n = EmuUtil.getInt4LE( fileBytes, dataPos );
	if( n > Integer.MAX_VALUE ) {
	  EmuUtil.throwMysteriousData();
	}
	buf.addSamples( (int) n, phase );
	phase = !phase;
	dataPos += 4;
	dataLen -= 4;
      }
    }
    return buf;
  }


  public static PCMDataSource getPCMDataSource(
					byte[] fileBytes,
					int    pos ) throws IOException
  {
    return getBitSampleBuffer( fileBytes, pos ).newReader();
  }


  public static void write(
			PCMDataSource pcm,
			File          file ) throws IOException
  {
    String fName = file.getName();
    if( fName == null ) {
      fName = "";
    }
    fName        = fName.toLowerCase();
    boolean csw1 = fName.endsWith( ".csw1" );
    if( !csw1 && !fName.endsWith( ".csw" ) ) {
      throw new IOException( "Dateiformat nicht unterst\u00FCtzt!"
		+ "\n\nUnterst\u00FCtzte Dateiendungen sind"
		+ " *.csw und *.csw1" );
    }
    int frameRate = pcm.getFrameRate();
    if( csw1 && (frameRate > 0xFFFF) ) {
      throw new IOException( "Die Abtastrate ist f\u00FCr eine"
		+ " CSW1-Datei zu gro\u00DF.\n"
		+ "Speichern Sie bitte die Datei mit der Endung *.csw,\n"
		+ "damit sie im CSW2-Format erzeugt wird." );
    }
    if( (pcm.getSampleSizeInBits() > 1) || (pcm.getChannels() > 1) ) {
      throw new IOException( "In einer CSW-Datei k\u00F6nnen nur"
			+ " 1-Bit-Mono-Audiodaten gespeichert werden." );
    }
    byte[] frameBuf = new byte[ 1 ];
    if( pcm.read( frameBuf, 0, 1 ) != frameBuf.length ) {
      AudioUtil.throwNoAudioData();
    }
    boolean      lastPhase = (frameBuf[ 0 ] != 0);
    OutputStream out       = null;
    try {
      out = new BufferedOutputStream( new FileOutputStream( file ) );

      // CSW-Signatur
      EmuUtil.writeASCII( out, FileInfo.CSW_MAGIC );

      // Kopf unterscheidet sich zwischen Version 1 und 2
      if( csw1 ) {

	// Versionsnummer 1.01
	out.write( 1 );
	out.write( 1 );

	// 2 Bytes Abtastrate
	out.write( frameRate & 0xFF );
	out.write( (frameRate >> 8) & 0xFF );

	// Kompression
	out.write( 1 );				// RLE

	// Flags
	out.write( lastPhase ? 0x01 : 0 );	// Bit 0: Initial-Phase

	// 3 reserverte Bytes
	for( int i = 0; i < 3; i++ ) {
	  out.write( 0 );
	}

      } else {

	// Versionsnummer 2.00
	out.write( 2 );
	out.write( 0 );

	// 4 Bytes Abtastrate
	writeInt4( out, frameRate );

	// 4 Bytes Gesamtanzahl Pulse
	writeInt4( out, pcm.getFrameCount() );

	// Kompression
	out.write( 1 );				// RLE

	// Flags
	out.write( lastPhase ? 0x01 : 0 );	// Bit 0: Initial-Phase

	// Header Extension
	out.write( 0 );				// keine Erweiterung

	// Encoding Application
	EmuUtil.writeFixLengthASCII( out, Main.APPNAME, 16, 0 );
      }

      // CSW-Daten
      int n = 0;
      while( pcm.read( frameBuf, 0, 1 ) == frameBuf.length ) {
	boolean phase = (frameBuf[ 0 ] != 0);
	if( phase == lastPhase ) {
	  n++;
	} else {
	  writeSampleCount( out, n );
	  n = 1;
	  lastPhase = phase;
	}
      }
      writeSampleCount( out, n );

      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilently( out );
    }
  }


	/* --- private Methoden --- */

  private static CSWFormat parseHeader(
				byte[] buf,
				int    pos ) throws IOException
  {
    if( !FileInfo.isCswMagicAt( buf, pos ) ) {
      FileUtil.throwUnsupportedFileFormat();
    }
    long sampleRate  = 0;
    int  compression = 0;
    int  flags       = 0;
    int  headerLen   = 0;
    if( ((int) buf[ pos + 0x17 ] & 0xFF) < 2 ) {
      sampleRate  = EmuUtil.getWord( buf, pos + 0x19 );
      compression = (int) buf[ pos + 0x1B ] & 0xFF;
      flags       = (int) buf[ pos + 0x1C ] & 0xFF;
      headerLen   = 0x20;
    } else {
      sampleRate  = EmuUtil.getInt4LE( buf, pos + 0x19 );
      compression = (int) buf[ pos + 0x21 ] & 0xFF;
      flags       = (int) buf[ pos + 0x22 ] & 0xFF;
      headerLen   = 0x34 + ((int) buf[ pos + 0x23 ] & 0xFF);
    }
    if( (sampleRate < 1) || (sampleRate > 192000) ) {
      throw new IOException( "Ung\u00FCltige Abtastrate" );
    }
    if( compression != 1 ) {
      StringBuilder strBuf = new StringBuilder( 256 );
      strBuf.append(  "CSW-Kompressionsmethode " );
      if( compression == 2 ) {
	strBuf.append( "Z-RLE" );
      } else {
	strBuf.append( compression );
      }
      strBuf.append( " nicht unterst\u00FCtzt" );
      throw new IOException( strBuf.toString() );
    }
    return new CSWFormat(
			sampleRate,
			(flags & 0x01) != 0,
			headerLen );
  }


  private static void writeInt4(
				OutputStream out,
				long         v ) throws IOException
  {
    out.write( (int) (v & 0xFF) );
    out.write( (int) ((v >> 8) & 0xFF) );
    out.write( (int) ((v >> 16) & 0xFF) );
    out.write( (int) (v >> 24) );
  }


  private static void writeSampleCount(
				OutputStream out,
				int          n ) throws IOException
  {
    if( n > 0 ) {
      if( (n & 0xFF) == n ) {
	out.write( n );
      } else {
	out.write( 0 );
	writeInt4( out, n );
      }
    }
  }


	/* --- Konstruktor --- */

  private CSWFile()
  {
    // Klasse kann nicht instanziiert werden
  }
}
