/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface fuer eine Byte-orientierte Datenquelle
 */

package jkcemu.base;


public interface ByteDataSource
{
  public int getAddrOffset();
  public int getDataByte( int addr );
  public int getDataLength();
}
