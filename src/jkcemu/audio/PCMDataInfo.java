/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle fuer Informarionen ueber PCM-Audioformat
 */

package jkcemu.audio;


public interface PCMDataInfo
{
  public int     getChannels();
  public int     getFrameRate();
  public long    getFrameCount();
  public int     getSampleSizeInBits();
  public boolean isBigEndian();
  public boolean isSigned();
};
