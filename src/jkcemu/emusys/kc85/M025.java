/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des 8K-User-PROM-Moduls M025
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;
import jkcemu.base.*;


public class M025 extends KC85ROM8KModule
{
  private int    typeByte;
  private String fileName;


  public M025(
	    int       slot,
	    EmuThread emuThread,
	    int       typeByte,
	    Component owner,
	    String    fileName )
  {
    super( slot, emuThread, "M025", loadFile( owner, fileName ) );
    this.typeByte = typeByte;
    this.fileName = fileName;
  }


  @Override
  public String getFileName()
  {
    return this.fileName;
  }


  @Override
  public int getTypeByte()
  {
    return this.typeByte;
  }


  @Override
  public void reload( Component owner )
  {
    setROM( loadFile( owner, this.fileName ) );
  }


	/* --- private Methoden --- */

  private static byte[] loadFile( Component owner, String fileName )
  {
    return EmuUtil.readFile( owner, fileName, 0x2000, "M025 ROM-Datei" );
  }
}

