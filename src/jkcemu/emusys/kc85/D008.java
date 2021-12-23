/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Floppy Disk Station D008
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.util.Properties;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.KC85;
import jkcemu.file.FileUtil;


public class D008 extends D004
{
  private static final String TEXT_D008_ROM_FILE = "D008-ROM-Datei";

  private static byte[] romD008 = null;

  private int romBank;


  public D008(
	KC85       kc85,
	Properties props,
	String     propPrefix )
  {
    super( kc85, props, propPrefix );
    this.romBank = 0;
  }


  public int getROMBank()
  {
    return this.romBank;
  }


  public boolean isLEDofDMAon()
  {
    return (this.procSys instanceof D008ProcSys ?
		((D008ProcSys) this.procSys).isLEDofDMAon()
		: false);
  }


  public boolean isLEDofRamFloppyOn()
  {
    return (this.procSys instanceof D008ProcSys ?
		((D008ProcSys) this.procSys).isLEDofRamFloppyOn()
		: false);
  }


  public boolean isMaxSpeed4MHz()
  {
    return (this.procSys instanceof D008ProcSys ?
		((D008ProcSys) this.procSys).isMaxSpeed4MHz()
		: true);
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendEtcInfoHTMLTo( StringBuilder buf )
  {
    super.appendEtcInfoHTMLTo( buf );
    buf.append( ", ROM-Bank " );
    buf.append( this.romBank );
  }


  @Override
  protected D008ProcSys createProcSys(
				Properties props,
				String     propPrefix )
  {
    return new D008ProcSys(
			this,
			this.kc85.getScreenFrm(),
			props,
			propPrefix );
  }


  @Override
  protected String getDiskStationPropValue()
  {
    return KC85.VALUE_D008;
  }


  @Override
  public String getModuleName()
  {
    return "D008";
  }


  @Override
  protected byte[] loadROM( KC85 kc85 )
  {
    byte[] romBytes = null;
    if( (this.romProp.length() > KC85.VALUE_PREFIX_FILE.length())
	&& this.romProp.startsWith( KC85.VALUE_PREFIX_FILE ) )
    {
      romBytes = FileUtil.readFile(
		kc85.getScreenFrm(),
		this.romProp.substring( KC85.VALUE_PREFIX_FILE.length() ),
		true,
		0x8000,
		TEXT_D008_ROM_FILE );
    }
    if( romBytes == null ) {
      if( romD008 == null ) {
	romD008 = EmuUtil.readResource(
				this.kc85.getScreenFrm(),
				"/rom/kc85/d008.bin" );
      }
      romBytes = romD008;
    }
    return romBytes;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled && (this.romBytes != null) ) {
      int idx = (this.romBank * 0x2000) + addr - this.romAddr;
      if( (idx >= 0) && (idx < this.romBytes.length) ) {
	rv = (int) this.romBytes[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void reload( Component owner )
  {
    if( (this.romProp.length() > KC85.VALUE_PREFIX_FILE.length())
	&& this.romProp.startsWith( KC85.VALUE_PREFIX_FILE ) )
    {
      byte[] romBytes = FileUtil.readFile(
		owner,
		this.romProp.substring( KC85.VALUE_PREFIX_FILE.length() ),
		true,
		0x8000,
		TEXT_D008_ROM_FILE );
      if( romBytes != null ) {
	this.romBytes = romBytes;
      }
    }
  }


  @Override
  public void reset( boolean powerOn )
  {
    super.reset( powerOn );
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.romBank = (value >> 6) & 0x03;
  }


  @Override
  public boolean supportsHDDisks()
  {
    return true;
  }
}
