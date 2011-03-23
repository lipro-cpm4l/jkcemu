/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface einer Marke
 */

package jkcemu.tools;

import java.lang.*;


public interface Label extends Comparable<Label>
{
  public String getLabelName();
  public int    getLabelValue();
}

