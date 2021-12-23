/*
 * (c) 2016-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer TZX-Dateien
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


public class TZXFile
{
  private final static String[] fileExts = { "cdt", "tzx" };

  private final static int   SAMPLE_RATE         = 44100;
  private final static float T_STATES_PER_SAMPLE =
				3500000F / (float) SAMPLE_RATE;

  private static javax.swing.filechooser.FileFilter fileFilter = null;


  public static String[] getFileExtensions()
  {
    return fileExts;
  }


  public static String getFileExtensionText()
  {
    return "*.cdt; *.tzx";
  }


  public static javax.swing.filechooser.FileFilter getFileFilter()
  {
    if( fileFilter == null ) {
      fileFilter = new FileNameExtensionFilter(
			"CDT/TZX-Dateien (" + getFileExtensionText() + ")",
			fileExts );
    }
    return fileFilter;
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
    if( !fName.endsWith( ".cdt" ) && !fName.endsWith( ".tzx" ) ) {
      throw new IOException( "Dateiformat nicht unterst\u00FCtzt!"
		+ "\n\nUnterst\u00FCtzte Dateiendungen sind"
		+ " *.cdt und *.tzx" );
    }
    if( (pcm.getSampleSizeInBits() > 1) || (pcm.getChannels() > 1) ) {
      throw new IOException( "In einer CDT/TZX-Datei k\u00F6nnen nur"
			+ " 1-Bit-Mono-Audiodaten gespeichert werden." );
    }
    long frameCount = pcm.getFrameCount();
    long nBytes     = (frameCount + 7) / 8;
    if( nBytes > 0x7FFFFF ) {
      throw new IOException( "Die Datei kann nicht gespeichert werden,\n"
		+ "da die L\u00E4nge der Audiodaten gr\u00F6\u00DFer ist,\n"
		+ "als das CDT/TZX-Dateiformat erm\u00F6glicht." );
    }
    byte[] frameBuf = new byte[ 1 ];
    if( pcm.read( frameBuf, 0, 1 ) != frameBuf.length ) {
      AudioUtil.throwNoAudioData();
    }
    OutputStream out = null;
    try {
      out = new BufferedOutputStream( new FileOutputStream( file ) );

      // TZX-Signatur mit Versionsnummer
      EmuUtil.writeASCII( out, FileInfo.TZX_MAGIC );
      out.write( 1 );
      out.write( 20 );

      // Text Description
      String text = "File created by JKCEMU";
      out.write( 0x30 );		// Block-ID
      out.write( text.length() );	// Textlaenge
      EmuUtil.writeASCII( out, text );

      // Direct Recording Block
      out.write( 0x15 );		// Block-ID

      /*
       * ZX-Spectrum-T-States pro Sample,
       * Fuer die Abtastfrequenz 22050 Hz wuerde der
       * gerundete Wert 159 betragen,
       * in der TZX-Spezifikation steht aber 158.
       * Aus diesem Grund wird hier fuer 22050 Hz der Wert 158
       * gesetzt und fuer alle anderen Abtastfrequenzen
       * der Wert berechnet und anschliessend gerundet.
       */
      int zxTStatesPerSample = 158;	// Wert fuer 22050 Hz
      int frameRate          = pcm.getFrameRate();
      if( frameRate != 22050 ) {
	zxTStatesPerSample = Math.round( 3500000F / (float) frameRate );
      }
      out.write( zxTStatesPerSample & 0xFF );
      out.write( (zxTStatesPerSample >> 8) & 0xFF );

      // anschliessende Pause in ms
      out.write( 0 );
      out.write( 0 );

      // benutzte Bits im letzten Byte
      int lastBits = (int) ((nBytes * 8) - frameCount);
      out.write( (lastBits > 0) && (lastBits < 8) ? lastBits : 8 );

      // Anzahl Datenbytes
      out.write( (int) (nBytes & 0xFF) );
      out.write( (int) ((nBytes >> 8) & 0xFF) );
      out.write( (int) (nBytes >> 16) );

      // Datenbytes
      while( nBytes > 0 ) {
	int b = 0;
	int m = 0x80;
	for( int i = 0; i < 8; i++ ) {
	  if( pcm.read( frameBuf, 0, 1 ) == frameBuf.length ) {
	    if( frameBuf[ 0 ] != 0 ) {
	      b |= m;
	    }
	  }
	  m >>= 1;
	}
	out.write( b );
	--nBytes;
      }

      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilently( out );
    }
  }


	/* --- Konstruktor --- */

  private TZXFile()
  {
    // Klasse kann nicht instanziiert werden
  }
}
