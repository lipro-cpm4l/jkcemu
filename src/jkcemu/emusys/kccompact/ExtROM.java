/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Tabellenmodell fuer ROM-Liste im KC compact
 */

package jkcemu.emusys.kccompact;


public class ExtROM implements Comparable<ExtROM>
{
  private int    romNum;
  private String fileName;

  public ExtROM( int romNum, String fileName )
  {
    this.romNum   = romNum;
    this.fileName = fileName;
  }

  public int getRomNum()
  {
    return this.romNum;
  }

  public String getFileName()
  {
    return this.fileName;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( ExtROM extROM )
  {
    return this.romNum - extROM.romNum;
  }
}
