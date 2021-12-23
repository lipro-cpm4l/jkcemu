/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Floppy Disk Station D004
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.emusys.KC85;
import jkcemu.file.FileUtil;
import z80emu.Z80CPU;
import z80emu.Z80Memory;


public class D004 extends AbstractKC85Module
{
  protected KC85        kc85;
  protected String      propPrefix;
  protected D004ProcSys procSys;
  protected String      romProp;
  protected byte[]      romBytes;
  protected int         romAddr;

  private static final String TEXT_D004_ROM_FILE = "D004-ROM-Datei";

  private static byte[] romD004_20    = null;
  private static byte[] romD004_331_2 = null;
  private static byte[] romD004_331_4 = null;

  private volatile boolean connected;
  private boolean          cpuEnableValue;
  private boolean          cpuStopValue;
  private boolean          cpuResetValue;
  private boolean          cpuNMIValue;
  private boolean          pendingStartUp;
  private Thread           thread;


  public D004(
	KC85       kc85,
	Properties props,
	String     propPrefix )
  {
    super( 0xFC );
    this.kc85       = kc85;
    this.propPrefix = propPrefix;
    this.romProp    = EmuUtil.getProperty(
				props,
				propPrefix + KC85.PROP_DISKSTATION_ROM );
    this.romBytes = loadROM( kc85 );
    this.thread   = null;
    this.procSys  = createProcSys( props, propPrefix );
    reset( false );
  }


  public void applySettings( Properties props )
  {
    this.procSys.applySettings( props );
  }


  public boolean canApplySettings( Properties props )
  {
    return this.procSys.canApplySettings( props )
		&& this.romProp.equals(
			EmuUtil.getProperty(
				props,
				propPrefix + KC85.PROP_DISKSTATION_ROM ) )
		&& EmuUtil.getProperty(
			props,
			this.propPrefix + KC85.PROP_DISKSTATION ).equals(
				getDiskStationPropValue() );
  }


  protected D004ProcSys createProcSys(
				Properties props,
				String     propPrefix )
  {
    return new D004ProcSys(
			this,
			this.kc85.getScreenFrm(),
			props,
			propPrefix );
  }


  public synchronized void fireStop()
  {
    if( this.thread != null ) {
      this.procSys.fireStop();
      this.thread = null;
    }
  }


  protected String getDiskStationPropValue()
  {
    return KC85.VALUE_D004;
  }


  public KC85 getKC85()
  {
    return this.kc85;
  }


  public Z80CPU getZ80CPU()
  {
    return this.procSys.getZ80CPU();
  }


  public Z80Memory getZ80Memory()
  {
    return this.procSys;
  }


  public int getSupportedFloppyDiskDriveCount()
  {
    return this.procSys.getSupportedFloppyDiskDriveCount();
  }


  public boolean isConnected()
  {
    return this.connected;
  }


  public boolean isLEDofGIDEon()
  {
    return this.procSys.isLEDofGIDEon();
  }


  public boolean isRunning()
  {
    return this.thread != null;
  }


  public void loadIntoRAM(
			byte[] dataBytes,
			int    dataOffs,
			int    begAddr )
  {
    this.procSys.loadIntoRAM( dataBytes, dataOffs, begAddr );
  }


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
		0x2000,
		TEXT_D004_ROM_FILE );
    }
    if( romBytes == null ) {
      if( this.romProp.equals( KC85.VALUE_ROM_20 ) ) {
	romBytes = getROMBytes20();
      } else if( this.romProp.equals( KC85.VALUE_ROM_33 ) ) {
	if( kc85.getKCTypeNum() >= 4 ) {
	  romBytes = getROMBytes331_4();
	} else {
	  romBytes = getROMBytes331_2();
	}
      } else {
	if( kc85.getKCTypeNum() >= 4 ) {
	  romBytes = getROMBytes331_4();
	} else {
	  romBytes = getROMBytes20();
	}
      }
    }
    return romBytes;
  }


  public void setDrive( int idx, FloppyDiskDrive drive )
  {
    this.procSys.setDrive( idx, drive );
  }


  public boolean supportsHDDisks()
  {
    return false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendEtcInfoHTMLTo( StringBuilder buf )
  {
    buf.append( "Transfer-RAM " );
    buf.append( this.connected ? "verbunden" : "getrennt" );
  }


  @Override
  public int getBegAddr()
  {
    return this.romAddr;
  }


  @Override
  public void die()
  {
    this.procSys.die();
  }


  @Override
  public String getModuleName()
  {
    return "D004";
  }


  @Override
  public int getTypeByte()
  {
    return 0xA7;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled && (this.romBytes != null) ) {
      int idx = addr - this.romAddr;
      if( (idx >= 0) && (idx < this.romBytes.length) ) {
	rv = (int) this.romBytes[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = -1;
    if( this.connected ) {
      int portL = port & 0xFF;
      if( (portL >= 0xF0) && (portL <= 0xF3) ) {
	rv = this.procSys.getMemByte(
			0xFC00 | (portL << 8) | ((port >> 8) & 0xFF),
			false );
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
		0x2000,
		TEXT_D004_ROM_FILE );
      if( romBytes != null ) {
	this.romBytes = romBytes;
      }
    }
  }


  @Override
  public void reset( boolean powerOn )
  {
    if( powerOn ) {
      fireStop();
      this.procSys.clearRAM( Main.getProperties() );
    }
    this.connected      = false;
    this.cpuEnableValue = false;
    this.cpuStopValue   = false;
    this.cpuResetValue  = false;
    this.cpuNMIValue    = false;
    this.romAddr        = 0xC000;
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.connected = ((value & 0x04) != 0);
    this.romAddr   = ((value & 0x20) != 0) ? 0xE000 : 0xC000;
  }


  @Override
  public boolean writeIOByte( int port, int value, int tStates )
  {
    boolean rv    = false;
    int     portL = (port & 0xFF);
    if( (portL >= 0xF0) && (portL <= 0xF4) ) {
      if( this.connected ) {
	if( (portL >= 0xF0) && (portL <= 0xF3) ) {
	  rv = this.procSys.setMemByte(
			0xFC00 | (portL << 8) | ((port >> 8) & 0xFF),
			value );
	}
	else if( portL == 0xF4 ) {
	  boolean state = ((value & 0x01) != 0);
	  if( state != this.cpuEnableValue ) {
	    this.cpuEnableValue = state;
	    if( state ) {
	      enableCPU();
	    }
	  }
	  state = ((value & 0x02) != 0);
	  if( state != this.cpuStopValue ) {
	    this.cpuStopValue = state;
	    if( state ) {
	      fireStop();
	    }
	  }
	  state = ((value & 0x04) != 0);
	  if( state != this.cpuResetValue ) {
	    this.cpuResetValue = state;
	    if( state && (this.thread != null) ) {
	      this.procSys.fireReset();
	    }
	  }
	  state = ((value & 0x08) != 0);
	  if( state != this.cpuNMIValue ) {
	    this.cpuNMIValue = state;
	    if( state && (this.thread != null) ) {
	      this.procSys.fireNMI();
	    }
	  }
	}
      }
      rv = true;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private synchronized void enableCPU()
  {
    if( this.thread == null ) {
      Thread t = new Thread( Main.getThreadGroup(), this.procSys, "D004" );
      this.thread = t;
      t.start();
    }
  }


  private byte[] getROMBytes20()
  {
    if( romD004_20 == null ) {
      romD004_20 = EmuUtil.readResource(
				this.kc85.getScreenFrm(),
				"/rom/kc85/d004_20.bin" );
    }
    return romD004_20;
  }


  private byte[] getROMBytes331_2()
  {
    if( romD004_331_2 == null ) {
      romD004_331_2 = EmuUtil.readResource(
				this.kc85.getScreenFrm(),
				"/rom/kc85/d004_331_2.bin" );
    }
    return romD004_331_2;
  }


  private byte[] getROMBytes331_4()
  {
    if( romD004_331_4 == null ) {
      romD004_331_4 = EmuUtil.readResource(
				this.kc85.getScreenFrm(),
				"/rom/kc85/d004_331_4.bin" );
    }
    return romD004_331_4;
  }
}
