/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer CSW-Dateien
 */

package jkcemu.audio;

import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import jkcemu.Main;
import jkcemu.base.*;


public class CSWFile
{
  private static class RLEDecoder extends InputStream
  {
    private InputStream in;
    private boolean     phase;
    private long        remainPhasePulses;

    private RLEDecoder( InputStream in, boolean initialPhase )
    {
      this.in                = in;
      this.phase             = !initialPhase;
      this.remainPhasePulses = 0;
    }

    @Override
    public int available()
    {
      return (int) (this.remainPhasePulses > Integer.MAX_VALUE ?
				Integer.MAX_VALUE
				: (int) this.remainPhasePulses);
    }

    @Override
    public void close() throws IOException
    {
      this.in.close();
    }

    @Override
    public boolean markSupported()
    {
      return false;
    }

    @Override
    public synchronized int read() throws IOException
    {
      if( this.remainPhasePulses <= 0 ) {
	this.phase = !this.phase;
	while( this.remainPhasePulses <= 0 ) {
	  int b = this.in.read();
	  if( b < 0 ) {
	    break;
	  }
	  if( b > 0 ) {
	    this.remainPhasePulses = b;
	    break;
	  }
	  byte[] m = new byte[ 4 ];
	  if( this.in.read( m ) != m.length ) {
	    break;
	  }
	  this.remainPhasePulses = EmuUtil.getInt4( m, 0 );
	}
      }
      int rv = -1;
      if( this.remainPhasePulses > 0 ) {
	--this.remainPhasePulses;
	rv = (this.phase ? AudioOut.SIGNED_VALUE_1 : AudioOut.SIGNED_VALUE_0);
      }
      return rv;
    }

    @Override
    public void reset() throws IOException
    {
      throw new IOException(
		"CSWFile.RLEDecoder: reset() nicht unters\u00FCtzt" );
    }

    @Override
    public synchronized long skip( long n ) throws IOException
    {
      long rv = 0;
      while( n > 0 ) {
	if( this.remainPhasePulses > 0 ) {
	  if( n > this.remainPhasePulses ) {
	    n  -= this.remainPhasePulses;
	    rv += this.remainPhasePulses;
	    this.remainPhasePulses = 0;
	  } else {
	    rv += n;
	    this.remainPhasePulses -= n;
	  }
	} else {
	  if( read() < 0 ) {
	    break;
	  }
	  --n;
	  rv++;
	}
      }
      return rv;
    }
  };


  private static class CSWFormat
  {
    private long    sampleRate;
    private long    pulses;
    private boolean initialPhase;
    private int     headerLen;

    private CSWFormat(
		long    sampleRate,
		long    pulses,
		boolean initialPhase,
		int     headerLen )
    {
      this.sampleRate   = sampleRate;
      this.pulses       = pulses;
      this.initialPhase = initialPhase;
      this.headerLen    = headerLen;
    }
  };


  private static class MyByteArrayOutputStream extends ByteArrayOutputStream
  {
    private MyByteArrayOutputStream()
    {
      super( 0x10000 );
    }

    private byte[] getBuf()
    {
      return this.buf;
    }
  };


  public static AudioFileFormat getAudioFileFormat( InputStream in )
			throws IOException, UnsupportedAudioFileException
  {
    AudioFileFormat rv  = null;
    CSWFormat       fmt = parseHeader( in );
    return new AudioFileFormat(
			new AudioFileFormat.Type( "CSW", "csw" ),
			createAudioFormat( fmt ),
			AudioSystem.NOT_SPECIFIED );
  }


  public static AudioInputStream getAudioInputStream(
					byte[] fileBytes,
					int    pos,
					int    len )
			throws IOException, UnsupportedAudioFileException
  {
    AudioInputStream rv      = null;
    CSWFormat        fmt     = parseHeader( fileBytes, pos, len );
    int              dataPos = pos + fmt.headerLen;
    int              dataLen = len - fmt.headerLen;
    long             pulses  = fmt.pulses;
    if( pulses <= 0 ) {
      /*
       * Wenn die Anzahl der Pulse nicht im Kopf steht,
       * muss sie ermittelt werden.
       */
      int tmpPos = dataPos;
      int tmpLen = dataLen;
      pulses = 0;
      while( (tmpPos < fileBytes.length) && (tmpLen > 0) ) {
	int b = (int) fileBytes[ tmpPos++ ] & 0xFF;
	--tmpLen;
	if( b > 0 ) {
	  pulses += b;
	} else {
	  if( ((tmpPos + 3) >= fileBytes.length) && (tmpLen > 3) ) {
	    break;
	  }
	  pulses += EmuUtil.getInt4( fileBytes, tmpPos );
	  tmpPos += 4;
	  tmpLen -= 4;
	}
      }
    }
    return new AudioInputStream(
			new RLEDecoder(
				new ByteArrayInputStream(
						fileBytes,
						dataPos,
						dataLen ),
				fmt.initialPhase ),
			createAudioFormat( fmt ),
			pulses );
  }


  public static AudioInputStream getAudioInputStream( InputStream in )
			throws IOException, UnsupportedAudioFileException
  {
    AudioInputStream rv     = null;
    CSWFormat        fmt    = parseHeader( in );
    long             pulses = fmt.pulses;
    if( pulses <= 0 ) {
      /*
       * Wenn die Anzahl der Pulse nicht im Kopf steht,
       * werden die Daten in einen Puffer gelesen
       * und dabei die Anzahl der Pulse ermittelt.
       */
      pulses                      = 0;
      MyByteArrayOutputStream buf = new MyByteArrayOutputStream();
      int                     b   = in.read();
      while( b >= 0 ) {
	buf.write( b );
	if( b > 0 ) {
	  pulses += b;
	} else {
	  byte[] m = new byte[ 4 ];
	  if( in.read( m ) != m.length ) {
	    break;
	  }
	  buf.write( m );
	  pulses += EmuUtil.getInt4( m, 0 );
	}
	b = in.read();
      }
      in = new ByteArrayInputStream( buf.getBuf(), 0, buf.size() );
    }
    return new AudioInputStream(
			new RLEDecoder( in, fmt.initialPhase ),
			createAudioFormat( fmt ),
			pulses );
  }


  public static void write(
			int         sampleRate,
			InputStream in,		// 1 Byte pro Sample
			long        len,
			File        file ) throws IOException
  {
    boolean      lastPhase = (in.read() > 0);
    OutputStream out       = null;
    try {
      out = new BufferedOutputStream( new FileOutputStream( file ) );

      // CSW-Signatur mit Versionsnummer
      EmuUtil.writeASCII( out, FileInfo.CSW_MAGIC );
      out.write( 2 );
      out.write( 0 );

      // 4 Bytes Abtastrate
      writeInt4( out, sampleRate );

      // 4 Bytes Gesamtanzahl Pulse
      writeInt4( out, len );

      // Kompression
      out.write( 1 );				// RLE

      // Flags
      out.write( lastPhase ? 0x01 : 0 );	// B0: Initial-Phase

      // Header Extension
      out.write( 0 );				// keine Erweiterung

      // Encoding Application
      EmuUtil.writeFixLengthASCII( out, Main.APPNAME, 16, 0 );

      // CSW-Daten
      --len;
      int n = 1;
      while( len > 0 ) {
	boolean phase = (in.read() > 0);
	if( phase == lastPhase ) {
	  n++;
	} else {
	  writeSampleCount( out, n );
	  n = 1;
	  lastPhase = phase;
	}
	--len;
      }
      writeSampleCount( out, n );
    }
    finally {
      if( out != null ) {
	out.close();
      }
    }
  }


	/* --- private Methoden --- */

  private static AudioFormat createAudioFormat( CSWFormat fmt )
  {
    /*
     * CSW hat zwar nur 1 Bit Aufloesung pro Sample,
     * aber es werden 8-Bit-Werte gelesen.
     */
    return new AudioFormat( (float) fmt.sampleRate, 8, 1, true, false );
  }


  private static CSWFormat parseHeader(
				byte[] buf,
				int    pos,
				int    len )
			throws IOException, UnsupportedAudioFileException
  {
    CSWFormat rv = null;
    if( !FileInfo.isCswMagicAt( buf, pos, len ) ) {
      throwMissingCswHeader();
    }
    long sampleRate  = 0;
    long pulses      = 0;
    int  compression = 0;
    int  flags       = 0;
    int  headerLen   = 0;
    if( ((int) buf[ pos + 0x17 ] & 0xFF) < 2 ) {
      sampleRate  = EmuUtil.getWord( buf, pos + 0x19 );
      compression = (int) buf[ pos + 0x1B ] & 0xFF;
      flags       = (int) buf[ pos + 0x1C ] & 0xFF;
      headerLen   = 0x20;
    } else {
      sampleRate  = EmuUtil.getInt4( buf, pos + 0x19 );
      pulses      = EmuUtil.getInt4( buf, pos + 0x1D );
      compression = (int) buf[ pos + 0x21 ] & 0xFF;
      flags       = (int) buf[ pos + 0x22 ] & 0xFF;
      headerLen   = 0x34 + ((int) buf[ pos + 0x23 ] & 0xFF);
    }
    if( sampleRate < 1 ) {
      throw new UnsupportedAudioFileException( "Ung\u00FCltige Abtastrate" );
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
      throw new UnsupportedAudioFileException( strBuf.toString() );
    }
    return new CSWFormat(
			sampleRate,
			pulses,
			(flags & 0x01) != 0,
			headerLen );
  }


  private static CSWFormat parseHeader( InputStream in )
			throws IOException, UnsupportedAudioFileException
  {
    // Puffergroesse fuer einen CSW2-Kopf
    byte[] header = new byte[ 0x34 ];

    // Erstmal nur soviele Bytes lesen wie ein CSW1-Kopf gross ist.
    if( in.read( header, 0, 0x20 ) != 0x20 ) {
      throwMissingCswHeader();
    }

    /*
     * Wenn es ein CSW2-Kopf ist, dann den Rest lesen.
     * Die Pruefung auf die CSW-Kennung erfolgt spaeter.
     */
    if( (header[ 0x17 ] & 0xFF) >= 2 ) {
      if( in.read( header, 0x20, 0x14 ) != 0x14 ) {
	throwMissingCswHeader();
      }
      in.skip( (int) header[ 0x23 ] & 0xFF );
    }

    // Kopf auswerten
    return parseHeader( header, 0, header.length );
  }


  private static void throwMissingCswHeader()
				throws UnsupportedAudioFileException
  {
    throw new UnsupportedAudioFileException(
			"Dateiformat nicht unterst\u00FCtzt" );
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
    if( (n & 0xFF) == n ) {
      out.write( n );
    } else {
      out.write( 0 );
      writeInt4( out, n );
    }
  }


	/* --- Konstruktor --- */

  private CSWFile()
  {
    // Klasse kann nicht instanziiert werden
  }
}
