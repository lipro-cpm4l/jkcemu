/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Berechnung von CRC16-CCITT mit Polynom x16 + x12 + x5 + 1 
 */

package jkcemu.base;

import java.lang.*;
import java.util.zip.Checksum;


public class CRC16CCITT implements Checksum
{
  private static final int POLYNOM = 0x1021;

  private int crc;


  public CRC16CCITT()
  {
    reset();
  }


  @Override
  public long getValue()
  {
    return this.crc & 0xFFFFL;
  }


  @Override
  public void reset()
  {
    this.crc = 0xFFFF;
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
    b <<= 8;
    for( int i = 0; i < 8; i++ ) {
      if( ((b ^ this.crc) & 0x8000) != 0 ) {
	this.crc <<= 1;
	this.crc ^= POLYNOM;
      } else {
	this.crc <<= 1;
      }
      b <<= 1;
    }
  }
}
