/*
 * (c) 2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber den Kontext beim Parsen
 */

package jkcemu.programming.basic;


public class ParseContext
{
  private boolean mAccuDirty;


  public ParseContext()
  {
    this.mAccuDirty = false;
  }


  public boolean isMAccuDirty()
  {
    return this.mAccuDirty;
  }


  public void resetMAccuDirty()
  {
    this.mAccuDirty = false;
  }


  public void setMAccuDirty()
  {
    this.mAccuDirty = true;
  }
}

