/*
 * (c) 2010-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Berechnung einer Pruefsumme bzw. eines Hashwertes
 */

package jkcemu.etc;

import java.lang.*;
import java.security.*;
import java.util.zip.*;


public class CksCalculator
{
  public static class Add8 implements Checksum
  {
    private int cks;

    public Add8()
    {
      reset();
    }

    @Override
    public long getValue()
    {
      return this.cks & 0xFFFFL;
    }

    @Override
    public void reset()
    {
      this.cks = 0;
    }

    @Override
    public void update( byte[] a, int offs, int len )
    {
      while( len > 0 ) {
	update( a[ offs++ ] );
	--len;
      }
    }

    @Override
    public void update( int b )
    {
      this.cks = (this.cks + (b & 0xFF)) & 0xFFFF;
    }
  };


  public static class Add16BE implements Checksum
  {
    private int cks;
    private int buf;

    public Add16BE()
    {
      reset();
    }

    @Override
    public long getValue()
    {
      int cks = this.cks;
      if( this.buf >= 0 ) {
	cks += ((this.buf << 8) & 0xFF00);
      }
      return cks & 0xFFFFL;
    }

    @Override
    public void reset()
    {
      this.cks = 0;
      this.buf = -1;
    }

    @Override
    public void update( byte[] a, int offs, int len )
    {
      while( len > 0 ) {
	update( a[ offs++ ] );
	--len;
      }
    }

    @Override
    public void update( int b )
    {
      b &= 0xFF;
      if( this.buf >= 0 ) {
	this.cks += ((this.buf << 8) & 0xFF00) | b;
	this.cks &= 0xFFFF;
	this.buf = -1;
      } else {
	this.buf = b;
      }
    }
  };


  public static class Add16LE implements Checksum
  {
    private int cks;
    private int buf;

    public Add16LE()
    {
      reset();
    }

    @Override
    public long getValue()
    {
      int cks = this.cks;
      if( this.buf >= 0 ) {
	cks += this.buf;
      }
      return cks & 0xFFFFL;
    }

    @Override
    public void reset()
    {
      this.cks = 0;
      this.buf = -1;
    }

    @Override
    public void update( byte[] a, int offs, int len )
    {
      while( len > 0 ) {
	update( a[ offs++ ] );
	--len;
      }
    }

    @Override
    public void update( int b )
    {
      if( this.buf >= 0 ) {
	this.cks += ((b << 8) & 0xFF00) | this.buf;
	this.cks &= 0xFFFF;
	this.buf = -1;
      } else {
	this.buf = b & 0xFF;
      }
    }
  };


  private static final String CKS_ADD8    = "Summe der Bytes";
  private static final String CKS_ADD16LE =
			"Summe der 16-Bit-Worte (Little Endian)";
  private static final String CKS_ADD16BE =
			"Summe der 16-Bit-Worte (Big Endian)";
  private static final String CKS_ADLER32    = "Adler-32";
  private static final String CKS_CRC16CCITT = "CRC-CCITT (CRC-16 HDLC)";
  private static final String CKS_CRC32      = "CRC-32";

  private static String[] algorithms = {
				CKS_ADD8,
				CKS_ADD16LE,
				CKS_ADD16BE,
				CKS_CRC16CCITT,
				CKS_CRC32,
				CKS_ADLER32,
				"MD2",
				"MD5",
				"SHA-1",
				"SHA-256",
				"SHA-384",
				"SHA-512" };

  private String        algorithm;
  private String        value;
  private Checksum      checksum;
  private MessageDigest digest;


  public CksCalculator( String algorithm ) throws NoSuchAlgorithmException
  {
    this.algorithm = algorithm;
    this.value     = null;
    this.checksum  = null;
    this.digest    = null;
    if( algorithm.equals( CKS_ADD8 ) ) {
      this.checksum = new Add8();
    } else if( algorithm.equals( CKS_ADD16LE ) ) {
      this.checksum = new Add16LE();
    } else if( algorithm.equals( CKS_ADD16BE ) ) {
      this.checksum = new Add16BE();
    } else if( algorithm.equals( CKS_ADLER32 ) ) {
      this.checksum = new Adler32();
    } else if( algorithm.equals( CKS_CRC16CCITT ) ) {
      this.checksum = new CRC16CCITT();
    } else if( algorithm.equals( CKS_CRC32 ) ) {
      this.checksum = new CRC32();
    } else {
      this.digest = MessageDigest.getInstance( algorithm );
    }
  }


  public String getAlgorithm()
  {
    return this.algorithm;
  }


  public static String[] getAvailableAlgorithms()
  {
    return algorithms;
  }


  public String getValue()
  {
    if( this.value == null ) {
      if( this.checksum != null ) {
	if( this.checksum instanceof CRC16CCITT ) {
	  this.value = String.format( "%04X", this.checksum.getValue() );
	} else {
	  this.value = String.format( "%08X", this.checksum.getValue() );
	}
      } else if( this.digest != null ) {
	byte[] result = this.digest.digest();
	if( result != null ) {
	  StringBuilder buf = new StringBuilder( 2 * result.length );
	  for( int i = 0; i < result.length; i++ ) {
	    buf.append( String.format( "%02X", ((int) result[ i ] & 0xFF) ) );
	  }
	  this.value = buf.toString();
	}
      }
    }
    return this.value;
  }


  public void reset()
  {
    this.value = null;
    if( this.checksum != null ) {
      this.checksum.reset();
    } else if( this.digest != null ) {
      this.digest.reset();
    }
  }


  public void update( int b )
  {
    if( this.checksum != null ) {
      this.checksum.update( b );
    } else if( this.digest != null ) {
      this.digest.update( (byte) b );
    }
  }
}
