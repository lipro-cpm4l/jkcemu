/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Berechnung einer Pruefsumme bzw. eines Hashwertes
 */

package jkcemu.base;

import java.lang.*;
import java.security.*;
import java.util.zip.*;


public class CksCalculator
{
  private static final String CKS_ADLER32    = "Adler-32";
  private static final String CKS_CRC16CCITT = "CRC-CCITT (CRC-16 HDLC)";
  private static final String CKS_CRC32      = "CRC-32";

  private static String[] algorithms = {
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
    if( algorithm.equals( CKS_ADLER32 ) ) {
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
