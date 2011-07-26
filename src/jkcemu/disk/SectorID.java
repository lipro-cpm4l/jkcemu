/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Sektor-ID
 */

package jkcemu.disk;

import java.lang.*;


public class SectorID
{
  private int cyl;
  private int head;
  private int sectorNum;
  private int sizeCode;


  public SectorID(
		int cyl,
		int head,
		int sectorNum,
		int sizeCode )
  {
    this.cyl       = cyl;
    this.head      = head;
    this.sectorNum = sectorNum;
    this.sizeCode  = sizeCode;
  }


  public int getCylinder()
  {
    return this.cyl;
  }


  public int getHead()
  {
    return this.head;
  }

  public int getSectorNum()
  {
    return this.sectorNum;
  }

  public int getSizeCode()
  {
    return this.sizeCode;
  }


  protected void setSizeCode( int sizeCode )
  {
    this.sizeCode = sizeCode;
  }
}

