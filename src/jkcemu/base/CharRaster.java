/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anordnung der Zeichen auf dem Bildschirm
 */

package jkcemu.base;

import java.lang.*;
import java.util.Map;


public class CharRaster
{
  private int                 colCount;
  private int                 rowCount;
  private int                 rowHeight;
  private int                 charHeight;
  private int                 charWidth;
  private int                 topLine;
  private Map<Long,Character> charMap;


  public CharRaster(
		int colCount,
		int rowCount,
		int rowHeight,
		int charHeight,
		int charWidth,
		int topLine )
  {
    this.colCount   = colCount;
    this.rowCount   = rowCount;
    this.rowHeight  = rowHeight;
    this.charHeight = charHeight;
    this.charWidth  = charWidth;
    this.topLine    = topLine;
    this.charMap    = null;
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


  public int getTopLine()
  {
    return this.topLine;
  }


  public void setCharMap( Map<Long,Character> charMap )
  {
    this.charMap = charMap;
  }
}
