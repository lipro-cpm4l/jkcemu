/*
 * (c) 2013-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anordnung der Zeichen auf dem Bildschirm
 */

package jkcemu.base;

import java.util.Map;


public class CharRaster
{
  private int                 colCount;
  private int                 rowCount;
  private int                 rowHeight;
  private int                 charWidth;
  private int                 charHeight;
  private int                 xOffs;
  private int                 yOffs;
  private Map<Long,Character> charMap;


  public CharRaster(
		int colCount,
		int rowCount,
		int rowHeight,
		int charWidth,
		int charHeight,
		int xOffs,
		int yOffs )
  {
    this.colCount   = colCount;
    this.rowCount   = rowCount;
    this.rowHeight  = rowHeight;
    this.charWidth  = charWidth;
    this.charHeight = charHeight;
    this.xOffs      = xOffs;
    this.yOffs      = yOffs;
    this.charMap    = null;
  }


  public CharRaster(
		int colCount,
		int rowCount,
		int rowHeight,
		int charWidth,
		int charHeight )
  {
    this( colCount, rowCount, rowHeight, charWidth, charHeight, 0, 0 );
  }


  public int getCharHeight()
  {
    return this.charHeight;
  }


  public Map<Long,Character> getCharMap()
  {
    return this.charMap;
  }


  public int getCharWidth()
  {
    return this.charWidth;
  }


  public int getColCount()
  {
    return this.colCount;
  }


  public int getRowCount()
  {
    return this.rowCount;
  }


  public int getRowHeight()
  {
    return this.rowHeight;
  }


  public int getXOffset()
  {
    return this.xOffs;
  }


  public int getYOffset()
  {
    return this.yOffs;
  }


  public void setCharMap( Map<Long,Character> charMap )
  {
    this.charMap = charMap;
  }
}
