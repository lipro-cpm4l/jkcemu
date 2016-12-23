/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer Audiodateien
 */

package jkcemu.audio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.*;
import java.util.Arrays;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class AudioFile
{
  private static final int SIGNED_VALUE_1 = AudioOut.UNSIGNED_VALUE_1 / 2;
  private static final int SIGNED_VALUE_0 = -SIGNED_VALUE_1;


  public static class Info implements PCMDataInfo
  {
    private int     frameRate;
    private int     sampleSizeInBits;
    private int     channels;
    private boolean dataSigned;
    private boolean bigEndian;
    private long    frameCount;

    public Info(
		int     frameRate,
		int     sampleSizeInBits,
		int     channels,
		boolean dataSigned,
		boolean bigEndian,
		long    frameCount )
    {
      this.frameRate        = frameRate;
      this.sampleSizeInBits = sampleSizeInBits;
      this.channels         = channels;
      this.dataSigned       = dataSigned;
      this.bigEndian        = bigEndian;
      this.frameCount       = frameCount;
    }

	/* --- PCMDataInfo --- */

    @Override
    public int getChannels()
    {
      return this.channels;
    }

    @Override
    public int getFrameRate()
    {
      return this.frameRate;
    }

    @Override
    public long getFrameCount()
    {
      return this.frameCount;
    }

    @Override
    public int getSampleSizeInBits()
    {
      return this.sampleSizeInBits;
    }

    @Override
    public boolean isBigEndian()
    {
      return this.bigEndian;
    }

    @Override
    public boolean isSigned()
    {
      return this.dataSigned;
    }
  };


  private static final String[] fileExts = {
			"aif", "aiff", "aifc", "au", "wav",
			"aif.gz", "aiff.gz", "aifc.gz", "au.gz", "wav.gz" };

  private static javax.swing.filechooser.FileFilter fileFilter = null;


  public static String[] getFileExtensions()
  {
    return fileExts;
  }


  public static String getFileExtensionText()
  {
    return "*.aif; *.aifc; *.aiff; *.au; *.wav; *.aif.gz; ...";
  }


  public static javax.swing.filechooser.FileFilter getFileFilter()
  {
    if( fileFilter == null ) {
      fileFilter = new FileNameExtensionFilter(
			"Sound-Dateien (" + getFileExtensionText() + ")",
			fileExts );
    }
    return fileFilter;
  }


  public static PCMDataInfo getInfo( File file ) throws IOException
  {
    PCMDataInfo rv  = null;
    InputStream in  = null;
    try {
      byte[] header    = new byte[ 0x1000 ];
      int    headerLen = 0;

      in        = EmuUtil.openBufferedOptionalGZipFile( file );
      headerLen = EmuUtil.read( in, header );
      if( headerLen < header.length ) {
	header = Arrays.copyOf( header, headerLen );
      }
      rv = processFile( header, null, null, null );
      if( rv == null ) {
	EmuUtil.throwUnsupportedFileFormat();
      }
    }
    finally {
      EmuUtil.closeSilent( in );
    }
    return rv;
  }


  public static String[] getSupportedFileExtensions()
  {
    return fileExts;
  }


  public static PCMDataSource open( File file ) throws IOException
  {
    return open( file, null );
  }


  public static PCMDataSource open(
				File   file,
				byte[] fileBytes ) throws IOException
  {
    PCMDataInfo      rv  = null;
    IOException      ex  = null;
    InputStream      in  = null;
    RandomAccessFile raf = null;
    try {
      byte[] header    = null;
      int    headerLen = 0;
      if( fileBytes != null ) {
	header    = fileBytes;
	headerLen = fileBytes.length;
      } else {
	long    fileLen = file.length();
	boolean gz      = EmuUtil.isGZipFile( file );

	/*
	 * Kleine GZip-komprimierte und etwas groessere
	 * unkomprimierte Dateien werden vollstaendig eingelesen.
	 * Sehr grosse unkomprimierte Dateien werden
	 * mit RandomAccessFile geoeffnet,
	 * um die Abspielposition aendern zu koennen.
	 * Bei grossen GZip-komprimierten Dateien geht das dann nicht.
	 */
	if( !gz && (fileLen > AudioUtil.FILE_FULL_READ_SIZE_MAX) ) {
	  raf       = new RandomAccessFile( file, "r" );
	  header    = new byte[ 0x1000 ];
	  headerLen = in.read( header );
	  raf.seek( 0 );
	} else {
	  if( !gz
	      || (gz && (fileLen <= AudioUtil.GZIP_FILE_FULL_READ_SIZE_MAX)) )
	  {
	    fileBytes = EmuUtil.readFile( file, true );
	    header    = fileBytes;
	    headerLen = fileBytes.length;
	  } else {
	    in     = EmuUtil.openBufferedOptionalGZipFile( file );
	    header = new byte[ 0x1000 ];
	    in.mark( header.length );
	    headerLen = EmuUtil.read( in, header );
	    in.reset();
	  }
	}
	if( headerLen < header.length ) {
	  // duerfte eigentlich nie vorkommen
	  header = Arrays.copyOf( header, headerLen );
	}
      }
      rv = processFile( header, fileBytes, raf, in );
      if( rv != null ) {
	if( !(rv instanceof PCMDataSource) ) {
	  rv = null;
	}
      }
    }
    catch( IOException ex1 ) {
      ex = ex1;
    }
    finally {
      if( rv == null  ) {
	EmuUtil.closeSilent( in );
	EmuUtil.closeSilent( raf );
	if( ex != null ) {
	  throw ex;
	} else {
	  EmuUtil.throwUnsupportedFileFormat();
	}
      }
    }
    return (PCMDataSource) rv;
  }


  public static void write(
			PCMDataSource source,
			File          file ) throws IOException
  {
    String fName = file.getName();
    if( fName == null ) {
      throwUnsupportedFileSuffix();
    }
    fName = fName.toLowerCase();
    if( fName.endsWith( ".aif" ) || fName.endsWith( ".aiff" )
	|| fName.endsWith( ".aif.gz" ) || fName.endsWith( ".aiff.gz" ) )
    {
      writeAIF( source, file, false );
    } else if( fName.endsWith( ".aifc" ) || fName.endsWith( ".aifc.gz" ) ) {
      writeAIF( source, file, true );
    } else if( fName.endsWith( ".au" ) || fName.endsWith( ".au.gz" ) ) {
      writeAU( source, file );
    } else if( fName.endsWith( ".wav" ) || fName.endsWith( ".wav.gz" ) ) {
      writeWAV( source, file );
    } else {
      throwUnsupportedFileSuffix();
    }
  }


	/* --- private Methoden --- */

  private static long calcDataLen( PCMDataSource source ) throws IOException
  {
    long bytesPerSample = (source.getSampleSizeInBits() + 7) / 8;
    long rv = bytesPerSample * source.getChannels() * source.getFrameCount();
    if( rv < 1 ) {
      AudioUtil.throwNoAudioData();
    }
    return rv;
  }


  private static void checkFormat(
				long frameRate,
				long sampleSizeInBits,
				long channels ) throws IOException
  {
    if( (frameRate < 1000) || (frameRate > 192000)
	|| (sampleSizeInBits < 1) || (sampleSizeInBits > 24)
	|| (channels < 1) || (channels > 9) )
    {
      EmuUtil.throwMysteriousData();
    }
  }


  private static PCMDataInfo createPCMDataInfo(
				int              frameRate,
				int              sampleSizeInBits,
				int              channels,
				boolean          dataSigned,
				boolean          bigEndian,
				byte[]           fileBytes,
				RandomAccessFile raf,
				InputStream      in,
				long             dataOffs,
				long             dataLen ) throws IOException
  {
    PCMDataInfo rv = null;
    if( fileBytes != null ) {
      rv = new PCMDataBuffer(
			frameRate,
			sampleSizeInBits,
			channels,
			dataSigned,
			bigEndian,
			fileBytes,
			dataOffs,
			dataLen );
    }
    else if( raf != null ) {
      rv = new PCMDataFile(
			frameRate,
			sampleSizeInBits,
			channels,
			dataSigned,
			bigEndian,
			raf,
			dataOffs,
			dataLen );
    }
    else if( in != null ) {
      if( dataOffs > 0 ) {
	in.skip( dataOffs );
      }
      rv = new PCMDataStream(
			frameRate,
			sampleSizeInBits,
			channels,
			dataSigned,
			bigEndian,
			in,
			dataLen );
    } else {
      int bytesPerSample = (sampleSizeInBits + 7) / 8;
      rv = new AudioFile.Info(
			frameRate,
			sampleSizeInBits,
			channels,
			dataSigned,
			bigEndian,
			dataLen / bytesPerSample / channels );
    }
    return rv;
  }


  /*
   * Die Methode liesst ein 10-Byte-Fliesskommazahl nach IEEE 754
   * in Big Endian und gibt sie gerundet
   * (die niederwaertigen 6 Bytes werden nicht ausgewertet)
   * als float zurueck.
   *
   * Das hier verwendete Verfahren ist nicht allgemeingueltig!
   * Es funktioniert nur bei Zahlen in der Groessenordnung
   * der bei JKCEMU auftretenden Abtastraten korrekt.
   */
  private static float getFloat10BE( byte[] buf, int pos )
  {
    float rv = 0F;
    if( buf != null ) {
      if( (pos >= 0) && ((pos + 9) < buf.length) ) {
	int t = (int) EmuUtil.getInt4BE( buf, pos );
	if( (t & 0x00008000) != 0 ) {
	  rv = Float.intBitsToFloat(
			((t & 0xF0000000)
				| ((t << 7) & 0x0F800000)
				| ((t << 8) & 0x007FFF00)) );
	}
      }
    }
    return rv;
  }


  private static PCMDataInfo processAIF(
				boolean          aifc,
				byte[]           header,
				byte[]           fileBytes,
				RandomAccessFile raf,
				InputStream      in ) throws IOException
  {
    PCMDataInfo rv               = null;
    boolean     bigEndian        = true;
    long        remainBytes      = EmuUtil.getInt4BE( header, 4 ) - 4;
    long        dataOffs         = 0;
    long        dataLen          = 0;
    long        frameCount       = 0;
    int         frameRate        = 0;
    int         sampleSizeInBits = 0;
    int         channels         = 0;
    int         pos              = 12;
    while( (remainBytes > 8) && ((pos + 8) < header.length) ) {
      long blkLen = EmuUtil.getInt4BE(header, pos + 4 ) + 8;
      if( EmuUtil.isTextAt( "COMM", header, pos )
	  && ((aifc && (blkLen >= 30)) || (!aifc && (blkLen >= 26))) )
      {
	channels         = EmuUtil.getInt2BE( header, pos + 8 );
	frameCount       = EmuUtil.getInt4BE( header, pos + 10 );
	sampleSizeInBits = EmuUtil.getInt2BE( header, pos + 14 );
	frameRate        = Math.round( getFloat10BE( header, pos + 16 ) );
	if( aifc ) {
	  if( EmuUtil.isTextAt( "NONE", header, pos + 26 ) ) {
	    bigEndian = true;
	  } else if( EmuUtil.isTextAt( "sowt", header, pos + 26 ) ) {
	    bigEndian = false;
	  } else {
	    throwUnsupportedEncoding();
	  }
	}
      } else if( EmuUtil.isTextAt( "SSND", header, pos ) ) {
	long offs = EmuUtil.getInt4BE(header, pos + 8 );
	if( EmuUtil.getInt4BE(header, pos + 12 ) != 0 ) {
	  // in Bloecken formatierte Audiodaten nicht unterstuetzt
	  throwUnsupportedEncoding();
	}
	dataOffs = pos + 16 + offs;
	if( frameCount > 0 ) {
	  int bytesPerSample = (sampleSizeInBits + 7) / 8;
	  dataLen            = frameCount * bytesPerSample * channels;
	} else {
	  dataLen = blkLen - 16 - offs;
	}
      }
      // Wenn Format- und Datenblock gelesen wurden, Verarbeitung beenden
      if( (dataOffs > 0) && (dataLen > 0) && (frameRate > 0) ) {
	rv = createPCMDataInfo(
			frameRate,
			sampleSizeInBits,
			channels,
			true,			// signed
			bigEndian,
			fileBytes,
			raf,
			in,
			dataOffs,
			dataLen );
	break;
      }
      // Bloecke beginnen immer auf einer geraden Byte-Zahl
      if( (blkLen % 2) != 0 ) {
	blkLen++;
      }
      pos         += (int) blkLen;
      remainBytes -= blkLen;
    }
    return rv;
  }


  private static PCMDataInfo processAU(
				byte[]           header,
				byte[]           fileBytes,
				RandomAccessFile raf,
				InputStream      in ) throws IOException
  {
    long dataOffs  = EmuUtil.getInt4BE(header, 4 );
    long dataLen   = EmuUtil.getInt4BE(header, 8 );
    long coding    = EmuUtil.getInt4BE(header, 12 );
    long frameRate = EmuUtil.getInt4BE( header, 16 );
    long channels  = EmuUtil.getInt4BE( header, 20 );
    if( (coding < 2) || (coding > 5) ) {
      throwUnsupportedEncoding();
    }
    int sampleSizeInBits = ((int) coding - 1) * 8;
    checkFormat( frameRate, sampleSizeInBits, channels );
    return createPCMDataInfo(
			(int) frameRate,
			sampleSizeInBits,
			(int) channels,
			true,				// signed
			true,				// bigEndian
			fileBytes,
			raf,
			in,
			dataOffs,
			dataLen );
  }


  private static PCMDataInfo processFile(
				byte[]           header,
				byte[]           fileBytes,
				RandomAccessFile raf,
				InputStream      in ) throws IOException
  {
    PCMDataInfo rv = null;
    if( EmuUtil.isTextAt( "FORM", header, 0 ) ) {
      if( EmuUtil.isTextAt( "AIFF", header, 8 ) ) {
	rv = processAIF( false, header, fileBytes, raf, in );
      } else if( EmuUtil.isTextAt( "AIFC", header, 8 ) ) {
	rv = processAIF( true, header, fileBytes, raf, in );
      }
    }
    else if( EmuUtil.isTextAt( ".snd", header, 0 ) ) {
      rv = processAU( header, fileBytes, raf, in );
    }
    if( EmuUtil.isTextAt( "RIFF", header, 0 )
	&& EmuUtil.isTextAt( "WAVE", header, 8 ) )
    {
      rv = processWAV( header, fileBytes, raf, in );
    }
    return rv;
  }


  private static PCMDataInfo processWAV(
				byte[]           header,
				byte[]           fileBytes,
				RandomAccessFile raf,
				InputStream      in ) throws IOException
  {
    PCMDataInfo rv               = null;
    long        remainBytes      = EmuUtil.getInt4LE( header, 4 ) - 4;
    long        dataOffs         = 0;
    long        dataLen          = 0;
    long        frameRate        = 0;
    long        sampleSizeInBits = 0;
    int         channels         = 0;
    int         pos              = 12;
    while( (remainBytes > 8) && ((pos + 8) < header.length) ) {
      long blkLen = EmuUtil.getInt4LE( header, pos + 4 ) + 8;
      if( EmuUtil.isTextAt( "fmt\u0020", header, pos ) && (blkLen >= 24) ) {
	if( EmuUtil.getWord( header, pos + 8 ) != 1 ) {	// PCM?
	  throwUnsupportedEncoding();
	}
	channels         = EmuUtil.getWord( header, pos + 10 );
	frameRate        = EmuUtil.getInt4LE( header, pos + 12 );
	sampleSizeInBits = EmuUtil.getWord( header, pos + 22 );
	checkFormat( frameRate, sampleSizeInBits, channels );
      }
      else if( EmuUtil.isTextAt( "data", header, pos ) && (blkLen >= 8) ) {
	dataOffs = pos + 8;
	dataLen  = blkLen - 8;
      }
      // Wenn Format- und Datenblock gelesen wurden, Verarbeitung beenden
      if( (dataOffs > 0) && (dataLen > 0) && (frameRate > 0) ) {
	rv = createPCMDataInfo(
			(int) frameRate,
			(int) sampleSizeInBits,
			channels,
			sampleSizeInBits > 8,		// signed
			false,				// bigEndian
			fileBytes,
			raf,
			in,
			dataOffs,
			dataLen );
	break;
      }
      pos         += (int) blkLen;
      remainBytes -= blkLen;
    }
    return rv;
  }


  private static void throwUnsupportedEncoding() throws IOException
  {
    throw new IOException(
		"Die in der Datei verwendete Kodierung der Audiodaten"
			+ " wird nicht unterst\u00FCtzt." );
  }


  private static void throwUnsupportedFileSuffix() throws IOException
  {
    throw new IOException( "Dateiformat nicht unterst\u00FCtzt!"
		+ "\n\nUnterst\u00FCtzte Dateiendungen sind:"
		+ "\n  *.aif; *.aifc; *.aiff; *.au; *.wav"
		+ "\n\nSowie die gleichen mit GZip komprimiert:"
		+ "\n  *.aif.gz; *.aifc.gz; *.aiff.gz; *.au.gz; *.wav.gz" );
  }


  private static void writeAIF(
			PCMDataSource source,
			File          file,
			boolean       aifc ) throws IOException
  {
    long         len = calcDataLen( source );
    OutputStream out = null;
    try {

      /*
       * Audiodaten vorzeichenbehaftet in Big-Endian speichern,
       * Ausnahme: AIFF-C/sowt-Dateien (Little Endian),
       *           was hier im Fall von *.aifc verwendet wird.
       */
      PCMConverterInputStream pcm = new PCMConverterInputStream(
							source,
							true,
							!aifc );

      // Datei schreiben
      out = new BufferedOutputStream(
			EmuUtil.createOptionalGZipOutputStream( file ) );

      // FORM Chunk
      EmuUtil.writeASCII( out, "FORM" );
				// Chunk-Laenge: restliche Dateilaenge
      EmuUtil.writeInt4BE( out, aifc ? (len + 74) : (len + 42) );
      EmuUtil.writeASCII( out, aifc ? "AIFC" : "AIFF" );

      // FVER Chunk (nur AIFF-C)
      if( aifc ) {
	EmuUtil.writeASCII( out, "FVER" );
	EmuUtil.writeInt4BE( out, 4 );
	// von Apple vorgegebner Wert fuer AIFF-C Version 1
	EmuUtil.writeInt4BE( out, 0xA2805140 );
      }

      // COMM Chunk
      EmuUtil.writeASCII( out, "COMM" );
      EmuUtil.writeInt4BE( out, aifc ? 37 : 18 );	// ohne Fuellbyte
      EmuUtil.writeInt2BE( out, source.getChannels() );
      EmuUtil.writeInt4BE( out, source.getFrameCount() );
      EmuUtil.writeInt2BE( out, pcm.getSampleSizeInBits() );
      writeFloat10BE( out, source.getFrameRate() );
      if( aifc ) {
	EmuUtil.writeASCII( out, "sowt" );	// Little Endian
	out.write( 14 );
	EmuUtil.writeASCII( out, "not compressed" );
	out.write( 0 );			// Fuellbyte fuer gerade Byteanzahl
      }

      // SSND Chunk
      EmuUtil.writeASCII( out, "SSND" );
      EmuUtil.writeInt4BE( out, len + 8 );	// Chunk-Laenge
      EmuUtil.writeInt4BE( out, 0 );		// Offset
      EmuUtil.writeInt4BE( out, 0 );		// Blocks size
      while( len > 0 ) {
	out.write( pcm.read() );
	--len;
      }

      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
  }


  private static void writeAU(
			PCMDataSource source,
			File          file ) throws IOException
  {
    long         len = calcDataLen( source );
    OutputStream out = null;
    try {

      // Audiodaten vorzeichenbehaftet in Big-Endian speichern
      PCMConverterInputStream pcm = new PCMConverterInputStream(
							source,
							true,
							true );
      int sampleSizeInBits = pcm.getSampleSizeInBits();

      // Datei oeffnen und Kopf schreiben
      out = new BufferedOutputStream(
			EmuUtil.createOptionalGZipOutputStream( file ) );
      EmuUtil.writeASCII( out, ".snd" );
      EmuUtil.writeInt4BE( out, 24 );		// Kopfgroesse
      EmuUtil.writeInt4BE( out, len );		// Groesse Datenbereich

      // 2: 8-Bit-PCM, 3: 16-Bit-PCM, 4: 24-Bit-PCM usw.
      EmuUtil.writeInt4BE( out, ((sampleSizeInBits + 7) / 8) + 1 );
      EmuUtil.writeInt4BE( out, source.getFrameRate() );
      EmuUtil.writeInt4BE( out, source.getChannels() );

      // Audiodaten schreiben
      while( len > 0 ) {
	out.write( pcm.read() );
	--len;
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
  }


  /*
   * Die Methode schreibt eine kleine positive Ganzzahl
   * als 10-Byte-Fliesskommazahl nach IEEE 754 in Big Endian heraus.
   *
   * Das hier verwendete Verfahren ist nicht allgemeingueltig!
   * Es funktioniert nur bei Zahlen in der Groessenordnung
   * der bei JKCEMU auftretenden Abtastraten korrekt.
   */
  private static void writeFloat10BE(
				OutputStream out,
				long         value ) throws IOException
  {
    int f = Float.floatToIntBits( (float) value );
    int t = (f & 0xF0000000)
		| ((f >> 7) & 0x001F0000)
		| 0x00008000
		| ((f >> 8) & 0x00007FFF);
    EmuUtil.writeInt4BE( out, t );
    for( int i = 0; i < 6; i++ ) {
      out.write( 0 );
    }
  }


  private static void writeWAV(
			PCMDataSource source,
			File          file ) throws IOException
  {
    long         len = calcDataLen( source );
    OutputStream out = null;
    try {

      /*
       * 1-Byte-Audiodaten vorzeichenlos in Little-Endian speichern
       * Mehr-Byte-Audiodaten vorzeichenbehaftet in Little-Endian speichern
       */
      int sampleSizeInBits        = source.getSampleSizeInBits();
      PCMConverterInputStream pcm = new PCMConverterInputStream(
						source,
						sampleSizeInBits > 8,
						false );
      sampleSizeInBits     = pcm.getSampleSizeInBits();
      int channels         = source.getChannels();
      int frameRate        = source.getFrameRate();
      int frameSizeInBytes = ((sampleSizeInBits + 7) / 8) * channels;

      // Datei oeffnen und Kopf schreiben
      out = new BufferedOutputStream(
			EmuUtil.createOptionalGZipOutputStream( file ) );
      EmuUtil.writeASCII( out, "RIFF" );
      EmuUtil.writeInt4LE( out, len + 36 );	// restliche Dateilaenge
      EmuUtil.writeASCII( out, "WAVEfmt\u0020" );
      EmuUtil.writeInt4LE( out, 16 );		// Restlaenge fmt-Block
      EmuUtil.writeInt2LE( out, 1 );		// PCM
      EmuUtil.writeInt2LE( out, channels );
      EmuUtil.writeInt4LE( out, frameRate );
      EmuUtil.writeInt4LE( out, frameRate * frameSizeInBytes );
      EmuUtil.writeInt2LE( out, frameSizeInBytes );
      EmuUtil.writeInt2LE( out, sampleSizeInBits );
      EmuUtil.writeASCII( out, "data" );
      EmuUtil.writeInt4LE( out, len );		// Restlaenge Datenblock

      // Audiodaten schreiben
      while( len > 0 ) {
	out.write( pcm.read() );
	--len;
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
  }


	/* --- Konstruktor --- */

  private AudioFile()
  {
    // Klasse kann nicht instanziiert werden
  }
}
