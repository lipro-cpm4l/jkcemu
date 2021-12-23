/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Z1013-Variante der Grafikkarte von Heiko Poppe
 */

package jkcemu.emusys.z1013;

import java.awt.event.KeyListener;
import java.util.Properties;
import jkcemu.emusys.Z1013;
import jkcemu.etc.GraphicPoppe;


public class Z1013GraphicPoppe extends GraphicPoppe
{
  private Z1013 z1013;


  public Z1013GraphicPoppe( Z1013 z1013, Properties props )
  {
    super( z1013, true, props );
    this.z1013 = z1013;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public KeyListener getKeyListener()
  {
    return this.z1013.getScreenFrm();
  }


  @Override
  public int getMode1Cols()
  {
    return 32;
  }


  @Override
  public int getMode1RowHeight()
  {
    return 8;
  }


  @Override
  public int getMode1Rows()
  {
    return 32;
  }


  @Override
  public int getMode1VideoAddrOffs()
  {
    return 0x400;
  }


  @Override
  public int getVideoBegAddr()
  {
    return 0xE800;
  }
}
