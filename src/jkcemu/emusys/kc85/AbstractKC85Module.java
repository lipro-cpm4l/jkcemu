/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer ein KC85-Modul
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;
import jkcemu.text.TextUtil;


public abstract class AbstractKC85Module
{
  protected int     slot;
  protected boolean enabled;

  private String typeByteText;


  protected AbstractKC85Module( int slot )
  {
    this.slot         = slot;
    this.enabled      = false;
    this.typeByteText = null;
  }


  public void clearRAM()
  {
    // leer
  }


  public void die()
  {
    // leer;
  }


  public boolean equalsModule(
			String moduleName,
			String typeByteText,
			String fileName )
  {
    boolean rv = TextUtil.equals( getModuleName(), moduleName );
    if( rv ) {
      if( typeByteText != null ) {
	if( !typeByteText.isEmpty() ) {
	  if( !TextUtil.equalsIgnoreCase(
				getTypeByteText(),
				typeByteText ) )
	  {
	    rv = false;
	  }
	}
      }
    }
    if( rv ) {
      String myFileName = getFileName();
      if( myFileName != null ) {
	if( !myFileName.isEmpty() ) {
	  rv = TextUtil.equals( myFileName, fileName );
	}
      }
    }
    return rv;
  }


  public String getFileName()
  {
    return null;
  }


  public int getSlot()
  {
    return this.slot;
  }


  public abstract String getModuleName();
  public abstract int    getTypeByte();


  public String getTypeByteText()
  {
    if( this.typeByteText == null ) {
      this.typeByteText = String.format( "%02X", getTypeByte() );
    }
    return this.typeByteText;
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  /*
   * Rueckgabewert:
   *  -1: Modul bedient diesen Lesevorgang nicht.
   */
  public int readIOByte( int port )
  {
    return -1;
  }


  /*
   * Rueckgabewert:
   *  -1: Modul bedient diesen Lesevorgang nicht.
   */
  public int readMemByte( int addr )
  {
    return -1;
  }


  public void reload( Component owner )
  {
    // leer
  }


  public void setStatus( int value )
  {
    this.enabled = ((value & 0x01) != 0);
  }


  public boolean supportsPrinter()
  {
    return false;
  }


  /*
   * Rueckgabewert:
   *  false: Modul bedient diesen Schreibvorgang nicht.
   */
  public boolean writeIOByte( int port, int value )
  {
    return false;
  }


  /*
   * Rueckgabewert:
   *  0: Modul bedient diesen Schreibvorgang nicht.
   *  1: Modul bedient prinzipiell diesen Schreibvorgang,
   *     es ist jedoch der Schreibschutz aktiv.
   *  2: Modul hat den Schreibvorgang ausgefuehrt.
   */
  public int writeMemByte( int addr, int value )
  {
    return 0;
  }
}

