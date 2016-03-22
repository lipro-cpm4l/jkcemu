/*
 * (c) 2009-2015 Jens Mueller
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


  public boolean equalsSectorID(
			int cyl,
			int head,
			int sectorNum,
			int sizeCode )
  {
    return (cyl == this.cyl)
		&& (head == this.head)
		&& (sectorNum == this.sectorNum)
		&& (sizeCode == this.sizeCode);
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


  protected void setSectorID( int cyl, int head, int sectorNum )
  {
    this.cyl       = cyl;
    this.head      = head;
    this.sectorNum = sectorNum;
  }


  protected void setSizeCode( int sizeCode )
  {
    this.sizeCode = sizeCode;
  }
}
