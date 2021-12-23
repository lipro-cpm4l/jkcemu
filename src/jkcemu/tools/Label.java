/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface einer Marke
 */

package jkcemu.tools;


public interface Label extends Comparable<Label>
{
  public String  getLabelName();
  public int     getVarSize();
  public int     intValue();
  public boolean isAddress();
}
