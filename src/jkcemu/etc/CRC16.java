/*
 * (c) 2010-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Berechnung von CRC16 unter Angabe des Polynoms und des Startwerts
 */

package jkcemu.etc;

import java.util.zip.Checksum;


public class CRC16 implements Checksum
{
  private int polynom;
  private int initValue;
  private int crcValue;


  public CRC16( int polynom, int initValue )
  {
    this.polynom   = polynom;
    this.initValue = initValue;
    reset();
  }


  /*
   * CRC16-CCITT-Instanz mit Polynom x16 + x12 + x5 + 1 (0x1021)
   * und Startwert -1 erzeugen
   */
  public static CRC16 createCRC16CCITT()
  {
    return new CRC16( 0x1021, 0xFFFF );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public long getValue()
  {
    return this.crcValue & 0xFFFFL;
  }


  @Override
  public void reset()
  {
    this.crcValue = this.initValue;
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
      if( ((b ^ this.crcValue) & 0x8000) != 0 ) {
	this.crcValue <<= 1;
	this.crcValue ^= this.polynom;
      } else {
	this.crcValue <<= 1;
      }
      b <<= 1;
    }
  }
}
