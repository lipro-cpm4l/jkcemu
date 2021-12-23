/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag in der ROM-Liste eines buntzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.Component;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;


public class CustomSysROM
{
  private int     begAddr;
  private int     size;
  private String  fileName;
  private int     switchIOAddr;
  private int     switchIOMask;
  private int     switchIOValue;
  private boolean bootROM;
  private boolean enableOnReset;
  private boolean enabled;
  private String  addrText;
  private String  optionText;
  private byte[]  data;


  public CustomSysROM(
		int     begAddr,
		int     size,
		String  fileName,
		int     switchIOAddr,
		int     switchIOMask,
		int     switchIOValue,
		boolean enableOnReset,
		boolean bootROM )
  {
    this.begAddr       = begAddr;
    this.size          = size;
    this.fileName      = fileName;
    this.switchIOAddr  = switchIOAddr;
    this.switchIOMask  = switchIOMask & 0xFF;
    this.switchIOValue = switchIOValue & this.switchIOMask;
    this.enableOnReset = enableOnReset;
    this.bootROM       = bootROM;
    this.enabled       = true;
    this.data          = null;
    this.addrText      = String.format(
				"%04Xh-%04Xh",
				this.begAddr,
				this.begAddr + this.size - 1 );
    StringBuilder buf = new StringBuilder();
    if( this.switchIOAddr >= 0 ) {
      buf.append( this.switchIOMask != 0 ? 'S' : 'A' );
    }
    if( bootROM ) {
      if( buf.length() > 0 ) {
	buf.append( '\u0020' );
      }
      buf.append( 'B' );
    }
    this.optionText = buf.toString();

    reset();
  }


  public boolean declaresSameROM( CustomSysROM rom )
  {
    boolean rv = false;
    if( (this.begAddr == rom.begAddr)
	&& (this.size == rom.size)
	&& TextUtil.equals( this.fileName, rom.fileName )
	&& (this.bootROM == rom.bootROM) )
    {
      if( (this.switchIOAddr >= 0) || (rom.switchIOAddr >= 0) ) {
	if( (this.switchIOAddr == rom.switchIOAddr)
	    && (this.switchIOMask == rom.switchIOMask) )
	{
	  if( this.switchIOMask != 0 ) {
	    if( this.switchIOValue == rom.switchIOValue ) {
	      // beide ROMs ueber ein Bit an- und abschaltbar
	      rv = true;
	    }
	  } else {
	    // beide ROMs nur abschaltbar
	    rv = true;
	  }
	}
      } else {
	// beide ROMs nicht schaltbar
	rv = true;
      }
    }
    return rv;
  }


  public String getAddressText()
  {
    return this.addrText;
  }


  public int getBegAddr()
  {
    return this.begAddr;
  }


  public boolean getEnableOnReset()
  {
    return this.enableOnReset;
  }


  public String getFileName()
  {
    return this.fileName;
  }


  /*
   * Rueckgabewert:
   *  >= 0: Byte aus dem ROM-Bereich
   *  < 0:  Adresse ausserhalb des ROM-Bereichs
   */
  public synchronized int getMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + this.size)) )
    {
      rv = 0xFF;
      if( this.data != null ) {
	int idx = addr - begAddr;
	if( (idx >= 0) && (idx < this.data.length) ) {
	  rv = (int) this.data[ idx ] & 0xFF;
	}
      }
    }
    return rv;
  }


  public String getOptionText()
  {
    return this.optionText;
  }


  public int getSize()
  {
    return this.size;
  }


  public int getSwitchIOAddr()
  {
    return this.switchIOAddr;
  }


  public int getSwitchIOMask()
  {
    return this.switchIOMask;
  }


  public int getSwitchIOValue()
  {
    return this.switchIOValue;
  }


  public boolean isBootROM()
  {
    return this.bootROM;
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  public boolean isEnabledAfterReset()
  {
    return this.enableOnReset || (this.switchIOMask == 0);
  }


  public synchronized void load( Component owner )
  {
    this.data = FileUtil.readFile(
			owner,
			this.fileName,
			true,
			this.size,
			"ROM " + this.addrText );
  }


  public void reset()
  {
    if( this.switchIOAddr >= 0 ) {
      if( this.switchIOMask != 0 ) {
	// ROM an- und abschaltbar
	this.enabled = this.enableOnReset;
      } else {
	// ROM nur abschaltbar
	this.enabled = true;
      }
    } else {
      // ROM nicht schaltbar
      this.enabled = true;
    }
  }


  public void writeIOByte( int port, int value )
  {
    if( (this.switchIOAddr >= 0) && (port == this.switchIOAddr) ) {
      if( this.switchIOMask != 0 ) {
	// ROM an- und abschaltbar
	this.enabled = ((value & this.switchIOMask) == this.switchIOValue);
      } else {
	// ROM nur abschaltbar
	this.enabled = false;
      }
    }
  }
}
