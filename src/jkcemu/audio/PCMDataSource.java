/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle fuer PCM-Audiodaten
 */

package jkcemu.audio;

import java.io.Closeable;
import java.io.IOException;


public interface PCMDataSource extends AutoCloseable, Closeable, PCMDataInfo
{
  public void    close() throws IOException;
  public long    getFramePos();
  public int     read( byte[] buf, int offs, int len ) throws IOException;
  public void    setFramePos( long framePos ) throws IOException;
  public boolean supportsSetFramePos();
};
