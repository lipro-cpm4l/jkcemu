/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines buntzerdefinierten Computers
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.GIDE;
import jkcemu.emusys.customsys.CustomSysROM;
import jkcemu.etc.VDIP;
import jkcemu.net.KCNet;
import jkcemu.text.CharConverter;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80PIO;
import z80emu.Z80SIO;
import z80emu.Z80SIOChannelListener;


public class CustomSys
		extends EmuSys
		implements
			FDC8272.DriveSelector,
			Z80CTCListener,
			Z80SIOChannelListener
{
  public static final String SYSNAME     = "CUSTOMSYS";
  public static final String SYSTEXT     = "Benutzerdefinierter Computer";
  public static final String PROP_PREFIX = "jkcemu.customsys.";
  public static final String PROP_TITLE              = "title";
  public static final String PROP_BOOT               = "boot";
  public static final String PROP_BEGADDR            = "addr.begin";
  public static final String PROP_SIZE               = "size";
  public static final String PROP_SWITCH_IOADDR      = "switch.ioaddr";
  public static final String PROP_SWITCH_IOMASK      = "switch.iomask";
  public static final String PROP_SWITCH_IOVALUE     = "switch.iovalue";
  public static final String PROP_ENABLE_ON_RESET    = "enable_on_reset";
  public static final String PROP_SCREEN_ENABLED     = "screen.enabled";
  public static final String PROP_SCREEN_BEGADDR     = "screen.addr.begin";
  public static final String PROP_SCREEN_COLS        = "screen.cols";
  public static final String PROP_SCREEN_ROWS        = "screen.rows";
  public static final String PROP_SPEED_KHZ          = "speed.khz";
  public static final String PROP_KEYBOARD_HW        = "keyboard.hardware";
  public static final String PROP_KEYBOARD_IOADDR    = "keyboard.io_addr";
  public static final String PROP_SWAP_KEY_CHAR_CASE = "swap_key_char_case";
  public static final String PROP_PIO_ENABLED        = "pio.enabled";
  public static final String PROP_SIO_ENABLED        = "sio.enabled";
  public static final String PROP_CTC_ENABLED        = "ctc.enabled";
  public static final String PROP_PIO_IOBASEADDR     = "pio.io_base_addr";
  public static final String PROP_SIO_IOBASEADDR     = "sio.io_base_addr";
  public static final String PROP_SIO_A_CLOCK        = "sio.a.clock";
  public static final String PROP_SIO_A_OUT          = "sio.a.out";
  public static final String PROP_SIO_B_CLOCK        = "sio.b.clock";
  public static final String PROP_SIO_B_OUT          = "sio.b.out";
  public static final String PROP_CTC_IOBASEADDR     = "ctc.io_base_addr";
  public static final String PROP_KCNET_IOBASEADDR   = "kcnet.io_base_addr";
  public static final String PROP_VDIP_IOBASEADDR    = "vdip.io_base_addr";
  public static final String PROP_FDC_DATA_IOADDR    = "fdc.data.ioaddr";
  public static final String PROP_FDC_STATUS_IOADDR  = "fdc.status.ioaddr";
  public static final String PROP_FDC_TC_IOADDR      = "fdc.tc.ioaddr";
  public static final String PROP_FDC_TC_IOMASK      = "fdc.tc.iomask";
  public static final String PROP_FDC_TC_IOVALUE     = "fdc.tc.iovalue";
  public static final String PROP_UNUSED_PORT_VALUE  = "unused_port.value";

  public static final int DEFAULT_KEYBOARD_IOADDR  = 0x00;
  public static final int DEFAULT_SCREEN_BEGADDR   = 0xF800;

  public static final String VALUE_PRINTER             = "printer";
  public static final String VALUE_KEYBOARD_PORT_RAW   = "port.raw";
  public static final String VALUE_KEYBOARD_PIO_A_HS   = "pio.a.handshake";
  public static final String VALUE_KEYBOARD_PIO_A_BIT7 = "pio.a.bit7";
  public static final String VALUE_KEYBOARD_PIO_B_HS   = "pio.b.handshake";
  public static final String VALUE_KEYBOARD_PIO_B_BIT7 = "pio.b.bit7";
  public static final String VALUE_KEYBOARD_SIO_A      = "sio.a";
  public static final String VALUE_KEYBOARD_SIO_B      = "sio.b";

  public static final String TEXT_NO_SCREEN
			= "Keine Bildschirmausgabe verf\u00FCgbar";

  public static final int DEFAULT_GIDE_IOBASEADDR = 0x80;

  private static final String DEFAULT_TITLE = "Benutzerdefinierter Computer";

  private static final int DEFAULT_SPEED_KHZ         = 2458;
  private static final int DEFAULT_PORT_VALUE        = 0xFF;
  private static final int DEFAULT_PIO_IOBASEADDR    = 0x00;
  private static final int DEFAULT_SIO_IOBASEADDR    = 0x04;
  private static final int DEFAULT_CTC_IOBASEADDR    = 0x08;
  private static final int DEFAULT_FDC_DATA_IOADDR   = 0x95;
  private static final int DEFAULT_FDC_STATUS_IOADDR = 0x94;
  private static final int DEFAULT_FDC_TC_IOADDR     = 0x92;
  private static final int DEFAULT_KCNET_IOBASEADDR  = 0xC0;
  private static final int DEFAULT_VDIP_IOBASEADDR   = 0xFC;

  private enum KeyboardHW {
			NONE,
			PORT_RAW,
			PIO_A_HS,
			PIO_A_BIT7,
			PIO_B_HS,
			PIO_B_BIT7,
			SIO_A,
			SIO_B };

  private enum SioOut { NONE, PRINTER };

  private static byte[]        romFont = null;
  private static CharConverter cp437
			= new CharConverter( CharConverter.Encoding.CP437 );

  private String         title;
  private CustomSysROM[] roms;
  private KeyboardHW     keyboardHW;
  private int            keyboardIOAddr;
  private int            keyChar;
  private int            screenCols;
  private int            screenRows;
  private int            screenBegAddr;
  private int            screenEndAddr;
  private int            unusedPortValue;
  private int            ctcIOBaseAddr;
  private int            pioIOBaseAddr;
  private int            sioIOBaseAddr;
  private int            sioAClock;
  private int            sioBClock;
  private int            fdcDataIOAddr;
  private int            fdcStatusIOAddr;
  private int            fdcTCIOAddr;
  private int            fdcTCIOMask;
  private int            fdcTCIOValue;
  private int            lastTCIOValue;
  private int            gideIOBaseAddr;
  private int            kcNetIOBaseAddr;
  private int            vdipIOBaseAddr;
  private byte[]         fontBytes;
  private Z80CTC         ctc;
  private Z80PIO         pio;
  private Z80SIO         sio;
  private SioOut         sioAout;
  private SioOut         sioBout;
  private KCNet          kcNet;
  private VDIP           vdip;
  private GIDE           gide;
  private FDC8272        fdc;
  private FloppyDiskDrive[] floppyDiskDrives;
  private boolean        keyboardUsed;
  private boolean        swapKeyCharCase;


  public CustomSys( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    if( emulatesScreen( props ) ) {
      this.screenCols    = getScreenCols( props );
      this.screenRows    = getScreenRows( props );
      this.screenBegAddr = getScreenBegAddr( props );
      this.screenEndAddr = this.screenBegAddr
				+ (this.screenCols * this.screenRows)
				- 1;
    } else {
      this.screenCols    = 0;
      this.screenRows    = 0;
      this.screenBegAddr = -1;
      this.screenEndAddr = -1;
    }
    this.title           = getTitle( props );
    this.fontBytes       = null;
    this.roms            = getDeclaredROMs( props );
    this.keyboardHW      = getKeyboardHW( props );
    this.keyboardIOAddr  = getKeyboardIOAddr( props );
    this.keyboardUsed    = false;
    this.keyChar         = 0;
    this.unusedPortValue = getUnusedPortValue( props );

    // Interrupt-System
    java.util.List<Z80InterruptSource> iSources = new ArrayList<>();

    // CTC
    this.ctc           = null;
    this.ctcIOBaseAddr = -1;
    if( emulatesCTC( props ) ) {
      this.ctcIOBaseAddr = getCtcIOBaseAddr( props );
      this.ctc           = new Z80CTC(
				String.format(
					"CTC (E/A-Adressen %02X-%02X)",
					this.ctcIOBaseAddr,
					this.ctcIOBaseAddr + 3 ) );
      iSources.add( this.ctc );
    }

    // PIO
    this.pio           = null;
    this.pioIOBaseAddr = -1;
    if( emulatesPIO( props ) ) {
      this.pioIOBaseAddr = getPioIOBaseAddr( props );
      this.pio           = new Z80PIO(
				String.format(
					"PIO (E/A-Adressen %02X-%02X)",
					this.pioIOBaseAddr,
					this.pioIOBaseAddr + 3 ) );
      iSources.add( this.pio );
    }

    // SIO
    this.sio           = null;
    this.sioIOBaseAddr = -1;
    this.sioAClock     = EmuUtil.getIntProperty(
				props,
				this.propPrefix + PROP_SIO_A_CLOCK,
				0 );
    this.sioBClock     = EmuUtil.getIntProperty(
				props,
				this.propPrefix + PROP_SIO_B_CLOCK,
				0 );
    if( emulatesSIO( props ) ) {
      this.sioIOBaseAddr = getSioIOBaseAddr( props );
      this.sio           = new Z80SIO(
				String.format(
					"SIO (E/A-Adressen %02X-%02X)",
					this.sioIOBaseAddr,
					this.sioIOBaseAddr + 3 ) );
      iSources.add( this.sio );
      if( this.ctc != null ) {
	this.ctc.addCTCListener( this );
      }
      this.sioAout = getSioOut( props, PROP_SIO_A_OUT );
      if( this.sioAout != SioOut.NONE ) {
	this.sio.addChannelListener( this, 0 );
      }
      this.sioBout = getSioOut( props, PROP_SIO_B_OUT );
      if( this.sioAout != SioOut.NONE ) {
	this.sio.addChannelListener( this, 1 );
      }
    } else {
      this.sioAout = SioOut.NONE;
      this.sioBout = SioOut.NONE;
    }

    // KCNet
    this.kcNet           = null;
    this.kcNetIOBaseAddr = -1;
    if( emulatesKCNet( props ) ) {
      this.kcNetIOBaseAddr = getKCNetIOBaseAddr( props );
      this.kcNet           = new KCNet(
				String.format(
					"KCNet (E/A-Adressen %02X-%02X)",
					this.kcNetIOBaseAddr,
					this.kcNetIOBaseAddr + 3 ) );
      iSources.add( this.kcNet );
    }

    // GIDE
    this.gide = GIDE.getGIDE( this.screenFrm, props, this.propPrefix );
    this.gideIOBaseAddr = getGideIOBaseAddr( props );

    // FDC
    this.floppyDiskDrives = null;
    this.fdc              = null;
    if( emulatesFDC( props ) ) {
      this.floppyDiskDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.floppyDiskDrives, null );
      this.fdc = new FDC8272( this, 4 );
    }
    this.fdcDataIOAddr   = getFdcDataIOAddr( props );
    this.fdcStatusIOAddr = getFdcStatusIOAddr( props );
    this.fdcTCIOAddr     = getFdcTCIOAddr( props );

    // VDIP
    this.vdip           = null;
    this.vdipIOBaseAddr = -1;
    if( emulatesSIO( props ) ) {
      this.vdipIOBaseAddr = getVdipIOBaseAddr( props );
      this.vdip           = new VDIP(
				this.emuThread.getFileTimesViewFactory(),
				String.format(
					"USB-PIO (E/A-Adressen %02X-%02X)",
					this.vdipIOBaseAddr,
					this.vdipIOBaseAddr + 3 ) );
      iSources.add( this.vdip );
      this.vdip.applySettings( props );
    }

    // CPU und Interrupt-System
    Z80CPU cpu = emuThread.getZ80CPU();
    if( !iSources.isEmpty() ) {
      try {
	cpu.setInterruptSources(
		iSources.toArray(
			new Z80InterruptSource[ iSources.size() ] ) );
      }
      catch( ArrayStoreException ex ) {}
    }
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    // Sonstiges
    updSwapKeyCharCase( props );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
  }


  public static boolean emulatesCTC( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_CTC_ENABLED,
			false );
  }


  public static boolean emulatesFDC( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_FDC_ENABLED,
			false );
  }


  public static boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_KCNET_ENABLED,
			false );
  }


  public static boolean emulatesPIO( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_PIO_ENABLED,
			false );
  }


  public static boolean emulatesScreen( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_SCREEN_ENABLED,
			false );
  }


  public static boolean emulatesSIO( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_SIO_ENABLED,
			false );
  }


  public static boolean emulatesVDIP( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_VDIP_ENABLED,
			false );
  }


  public static CustomSysROM[] getDeclaredROMs( Properties props )
  {
    java.util.List<CustomSysROM> roms = new ArrayList<>();
    int n = EmuUtil.getIntProperty(
		props,
		PROP_PREFIX + PROP_ROM_PREFIX + PROP_COUNT,
		0 );
    for( int i = 0; i < n; i++ ) {
      String prefix   = String.format(
				"%s%s%d.",
				PROP_PREFIX,
				PROP_ROM_PREFIX,
				i );
      int begAddr = EmuUtil.getIntProperty(
				props,
				prefix + PROP_BEGADDR,
				-1 );
      int size = EmuUtil.getIntProperty(
				props,
				prefix + PROP_SIZE,
				-1 );
      if( (begAddr >= 0) && (size > 0) ) {
	roms.add(
		new CustomSysROM(
			begAddr,
			size,
			EmuUtil.getProperty( props, prefix + PROP_FILE ),
			EmuUtil.getIntProperty(
					props,
					prefix + PROP_SWITCH_IOADDR,
					-1 ),
			EmuUtil.getIntProperty(
					props,
					prefix + PROP_SWITCH_IOMASK,
					0 ),
			EmuUtil.getIntProperty(
					props,
					prefix + PROP_SWITCH_IOVALUE,
					0 ),
			EmuUtil.getBooleanProperty(
					props,
					prefix + PROP_ENABLE_ON_RESET,
					false ),
			EmuUtil.getBooleanProperty(
					props,
					prefix + PROP_BOOT,
					false ) ) );
      }
    }
    return roms.toArray( new CustomSysROM[ roms.size() ] );
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_SPEED_KHZ,
			DEFAULT_SPEED_KHZ );
  }


  public static int getCtcIOBaseAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_CTC_IOBASEADDR,
			DEFAULT_CTC_IOBASEADDR ) & 0xFF;
  }


  public static int getFdcDataIOAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_FDC_DATA_IOADDR,
			DEFAULT_FDC_DATA_IOADDR ) & 0xFF;
  }


  public static int getFdcStatusIOAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_FDC_STATUS_IOADDR,
			DEFAULT_FDC_STATUS_IOADDR ) & 0xFF;
  }


  public static int getFdcTCIOAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_FDC_TC_IOADDR,
			DEFAULT_FDC_TC_IOADDR ) & 0xFF;
  }


  public static int getKeyboardIOAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_KEYBOARD_IOADDR,
			DEFAULT_KEYBOARD_IOADDR ) & 0xFF;
  }


  public static int getKCNetIOBaseAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_KCNET_IOBASEADDR,
			DEFAULT_KCNET_IOBASEADDR ) & 0xFF;
  }


  public static int getPioIOBaseAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_PIO_IOBASEADDR,
			DEFAULT_PIO_IOBASEADDR ) & 0xFF;
  }


  public static int getScreenBegAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_SCREEN_BEGADDR,
			DEFAULT_SCREEN_BEGADDR );
  }


  public static int getScreenCols( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_SCREEN_COLS,
			0 );
  }


  public static int getScreenRows( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_SCREEN_ROWS,
			0 );
  }


  public static int getSioIOBaseAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_SIO_IOBASEADDR,
			DEFAULT_SIO_IOBASEADDR ) & 0xFF;
  }


  public static String getTitle( Properties props )
  {
    String s = EmuUtil.getProperty( props, PROP_PREFIX + PROP_TITLE );
    if( s != null ) {
      s = s.trim();
    } else {
      s = "";
    }
    return s.isEmpty() ? DEFAULT_TITLE : s;
  }


  public static int getUnusedPortValue( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_UNUSED_PORT_VALUE,
			DEFAULT_PORT_VALUE );
  }


  public static int getVdipIOBaseAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + PROP_VDIP_IOBASEADDR,
			DEFAULT_VDIP_IOBASEADDR ) & 0xFF;
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    FloppyDiskDrive rv = null;
    if( this.floppyDiskDrives != null ) {
      if( (driveNum >= 0) && (driveNum < this.floppyDiskDrives.length) ) {
	rv = this.floppyDiskDrives[ driveNum ];
      }
    }
    return rv;
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      if( timerNum == this.sioAClock ) {
	this.sio.clockPulseSenderA();
	this.sio.clockPulseReceiverA();
      }
      if( timerNum == this.sioBClock ) {
	this.sio.clockPulseSenderB();
	this.sio.clockPulseReceiverB();
      }
    }
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOByteSent( Z80SIO sio, int channel, int value )
  {
    if( sio == this.sio ) {
      switch( channel ) {
	case 0:
	  if( this.sioAout == SioOut.PRINTER ) {
	    this.emuThread.getPrintMngr().putByte( value );
	  }
	  this.sio.setClearToSendA( false );
	  this.sio.setClearToSendA( true );
	  break;
	case 1:
	  if( this.sioAout == SioOut.PRINTER ) {
	    this.emuThread.getPrintMngr().putByte( value );
	  }
	  this.sio.setClearToSendB( false );
	  this.sio.setClearToSendB( true );
	  break;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>Benutzerdefinierter Computer</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>Bezeichnung:</td><td>" );
    EmuUtil.appendHTML( buf, this.title );
    buf.append( "</td></tr>\n" );
    for( CustomSysROM rom : this.roms ) {
      buf.append( "<tr><td nowrap=\"nowrap\">ROM&nbsp;" );
      EmuUtil.appendHTML( buf, rom.getAddressText() );
      String fileName = rom.getFileName();
      if( fileName != null ) {
	if( !fileName.isEmpty() ) {
	  fileName = (new File( fileName )).getName();
	  if( fileName != null ) {
	    if( !fileName.isEmpty() ) {
	      buf.append( ", Datei:&nbsp;" );
	      EmuUtil.appendHTML( buf, fileName );
	    }
	  }
	}
      }
      buf.append( "</td><td>" );
      EmuUtil.appendOnOffText( buf, rom.isEnabled() );
      buf.append( "</td></tr>\n" );
    }
    buf.append( "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    this.title = getTitle( props );
    loadFont( props );
    updSwapKeyCharCase( props );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      CustomSysROM[] roms = getDeclaredROMs( props );
      if( roms.length == this.roms.length ) {
	for( int i = 0; i < roms.length; i++ ) {
	  if( !roms[ i ].declaresSameROM( this.roms[ i ] ) ) {
	    rv = false;
	    break;
	  }
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      boolean screenState = emulatesScreen( props );
      if( (this.screenBegAddr >= 0)
	  && (this.screenCols > 0)
	  && (this.screenRows > 0) )
      {
	if( !screenState
	    || (this.screenBegAddr != getScreenBegAddr( props ))
	    || (this.screenCols != getScreenCols( props ))
	    || (this.screenRows != getScreenRows( props )) )
	{
	  rv = false;
	}
      } else {
	if( screenState ) {
	  rv = false;
	}
      }
    }
    if( rv ) {
      if( getKeyboardHW( props ) == this.keyboardHW ) {
	if( (this.keyboardHW == KeyboardHW.PORT_RAW)
	    && (getKeyboardIOAddr( props ) != this.keyboardIOAddr) )
	{
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      boolean hasCtc = (this.ctc != null);
      if( emulatesCTC( props ) == hasCtc ) {
	if( hasCtc && getCtcIOBaseAddr( props ) != this.ctcIOBaseAddr ) {
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      boolean hasPio = (this.pio != null);
      if( emulatesPIO( props ) == hasPio ) {
	if( hasPio && getPioIOBaseAddr( props ) != this.pioIOBaseAddr ) {
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      boolean hasSio = (this.sio != null);
      if( emulatesSIO( props ) == hasSio ) {
	if( hasSio
	    && ((getSioIOBaseAddr( props ) != this.sioIOBaseAddr)
		|| (getSioOut( props, PROP_SIO_A_OUT ) != this.sioAout)
		|| (getSioOut( props, PROP_SIO_B_OUT ) != this.sioBout)) )
	{
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      boolean hasFdc = (this.fdc != null);
      if( emulatesFdc( props ) == hasFdc ) {
	if( hasFdc
	    && ((getFdcDataIOAddr( props ) != this.fdcDataIOAddr)
		|| (getFdcStatusIOAddr( props ) != this.fdcStatusIOAddr)
		|| (getFdcTCIOAddr( props ) != this.fdcTCIOAddr)) )
	{
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, this.propPrefix );
    }
    if( rv && (this.gide != null) ) {
      if( getGideIOBaseAddr( props ) != this.gideIOBaseAddr ) {
	rv = false;
      }
    }
    if( rv ) {
      boolean hasKCNet = (this.kcNet != null);
      if( emulatesKCNet( props ) == hasKCNet ) {
	if( hasKCNet
	    && (getKCNetIOBaseAddr( props ) != this.kcNetIOBaseAddr) )
	{
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    if( rv ) {
      boolean hasVdip = (this.vdip != null);
      if( emulatesVDIP( props ) == hasVdip ) {
	if( hasVdip && getVdipIOBaseAddr( props ) != this.vdipIOBaseAddr ) {
	  rv = false;
	}
      } else {
	rv = false;
      }
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return (this.screenBegAddr >= 0)
		&& (this.screenCols > 0)
		&& (this.screenRows > 0);
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.ctc != null ) {
      this.ctc.removeCTCListener( this );
    }
    if( this.sio != null ) {
      if( this.sioAout != SioOut.NONE ) {
	this.sio.removeChannelListener( this, 0 );
      }
      if( this.sioAout != SioOut.NONE ) {
	this.sio.removeChannelListener( this, 1 );
      }
    }
    if( this.fdc != null ) {
      this.fdc.die();
    }
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.kcNet != null ) {
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( (this.screenBegAddr >= 0)
	&& (this.screenCols > 0)
	&& (this.screenRows > 0) )
    {
      byte[] fontBytes = this.fontBytes;
      if( fontBytes != null ) {
	int col  = x / 8;
	int row  = y / 8;
	int rPix = y % 8;
	int addr = this.screenBegAddr + (row * this.screenCols) + col;
	if( (addr >= 0) && (addr < 0x10000) ) {
	  int idx = (this.emuThread.getRAMByte( addr ) & 0xFF) * 8 + rPix;
	  if( (idx >= 0) && (idx < fontBytes.length) ) {
	    int m = 0x80;
	    int n = x % 8;
	    if( n > 0 ) {
	      m >>= n;
	    }
	    if( (fontBytes[ idx ] & m) != 0 ) {
	      rv = WHITE;
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  protected boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    CharRaster rv = null;
    if( (this.screenBegAddr >= 0)
	&& (this.screenCols > 0)
	&& (this.screenRows > 0) )
    {
      rv = new CharRaster( this.screenCols, this.screenRows, 8, 8, 8, 0 );
    }
    return rv;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/customsys.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = -1;
    boolean done = false;
    for( CustomSysROM rom : roms ) {
      if( rom.isEnabled() ) {
	rv = rom.getMemByte( addr );
	break;
      }
    }
    if( rv < 0 ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return (rv & 0xFF);
  }


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    int rv = 0;
    for( CustomSysROM rom : roms ) {
      if( rom.isBootROM() ) {
	rv = rom.getBegAddr();
	break;
      }
    }
    return rv;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch = 0x20;
    if( (this.screenBegAddr >= 0)
	&& (this.screenCols > 0)
	&& (this.screenRows > 0) )
    {
      int addr = this.screenBegAddr + (chY * this.screenCols) + chX;
      if( (addr >= 0) && (addr < 0x10000) ) {
	int b = this.emuThread.getRAMByte( addr );
	if( b >= 0x20 ) {
	  ch = cp437.toUnicode( b );
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    int rv = 100;
    if( (this.screenBegAddr >= 0)
	&& (this.screenCols > 0)
	&& (this.screenRows > 0) )
    {
      rv = this.screenRows * 8;
    }
    return rv;
  }


  @Override
  public int getScreenWidth()
  {
    int rv = 300;
    if( (this.screenBegAddr >= 0)
	&& (this.screenCols > 0)
	&& (this.screenRows > 0) )
    {
      rv = this.screenCols * 8;
    }
    return rv;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.floppyDiskDrives != null ? this.floppyDiskDrives.length : 0;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return this.swapKeyCharCase;
  }


  @Override
  public String getTitle()
  {
    return this.title;
  }


  @Override
  protected VDIP getVDIP()
  {
    return this.vdip;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
	ch = 8;
	break;
      case KeyEvent.VK_RIGHT:
	ch = 9;
	break;
      case KeyEvent.VK_DOWN:
	ch = 0x0A;
	break;
      case KeyEvent.VK_UP:
	ch = 0x0B;
	break;
      case KeyEvent.VK_ENTER:
	ch = 0x0D;
	break;
      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;
    }
    if( ch > 0 ) {
      rv = putKeyChar( ch );
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    putKeyChar( 0 );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    return putKeyChar( ch );
  }


  @Override
  public boolean paintScreen(
			Graphics g,
			int      xOffs,
			int      yOffs,
			int      screenScale )
  {
    boolean rv = false;
    if( (this.screenBegAddr < 0)
	|| (this.screenCols <= 0)
	|| (this.screenRows <= 0) )
    {
      int x          = xOffs;
      int y          = yOffs;
      int fontHeight = 12 * screenScale;
      g.setFont( new Font( Font.SANS_SERIF, Font.PLAIN, fontHeight ) );
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	x = xOffs + (getScreenWidth() - fm.stringWidth( TEXT_NO_SCREEN )) / 2;
	if( x < 0 ) {
	  x = 0;
	}
      }
      y = yOffs + ((getScreenHeight() + fontHeight) / 2);
      if( y < fontHeight ) {
	y = fontHeight;
      }
      g.setColor( Color.GRAY );
      g.drawString( TEXT_NO_SCREEN, x, y );
      rv = true;
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    port &= 0xFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( (this.ctc != null)
	&& (port >= this.ctcIOBaseAddr)
	&& (port < (this.ctcIOBaseAddr + 4)) )
    {
      rv &= this.ctc.read( port - this.ctcIOBaseAddr, tStates );
      done = true;
    }
    if( (this.pio != null)
	&& (port >= this.pioIOBaseAddr)
	&& (port < (this.pioIOBaseAddr + 4)) )
    {
      switch( port - this.pioIOBaseAddr ) {
	case 0:
	  if( ((this.keyboardHW == KeyboardHW.PIO_A_HS)
			|| (this.keyboardHW == KeyboardHW.PIO_A_BIT7))
	      && !this.keyboardUsed )
	  {
	    this.pio.putInValuePortA( 0, 0xFF );
	    this.keyboardUsed = true;
	  }
	  rv &= this.pio.readDataA();
	  done = true;
	  break;
	case 1:
	  if( ((this.keyboardHW == KeyboardHW.PIO_B_HS)
			|| (this.keyboardHW == KeyboardHW.PIO_B_BIT7))
	      && !this.keyboardUsed )
	  {
	    this.pio.putInValuePortB( 0, 0xFF );
	    this.keyboardUsed = true;
	  }
	  rv &= this.pio.readDataB();
	  done = true;
	  break;
	case 2:
	  rv &= this.pio.readControlA();
	  done = true;
	  break;
	case 3:
	  rv &= this.pio.readControlB();
	  done = true;
	  break;
      }
    }
    if( (this.sio != null)
	&& (port >= this.sioIOBaseAddr)
	&& (port < (this.sioIOBaseAddr + 4)) )
    {
      switch( port - this.sioIOBaseAddr ) {
	case 0:
	  rv &= this.sio.readDataA();
	  done = true;
	  break;
	case 1:
	  rv &= this.sio.readDataB();
	  done = true;
	  break;
	case 2:
	  rv &= this.sio.readControlA();
	  done = true;
	  break;
	case 3:
	  rv &= this.sio.readControlB();
	  done = true;
	  break;
      }
    }
    if( this.fdc != null ) {
      if( port == this.fdcDataIOAddr ) {
	rv &= this.fdc.readData();
	done = true;
      } else if( port == this.fdcStatusIOAddr ) {
	rv &= this.fdc.readMainStatusReg();
	done = true;
      }
    }
    if( this.gide != null ) {
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv   = value;
	done = true;
      }
    }
    if( (this.kcNet != null)
	&& (port >= this.kcNetIOBaseAddr)
	&& (port < (this.kcNetIOBaseAddr + 4)) )
    {
      rv &= this.kcNet.read( port - this.kcNetIOBaseAddr );
    }
    if( (this.vdip != null)
	&& (port >= this.vdipIOBaseAddr)
	&& (port < (this.vdipIOBaseAddr + 4)) )
    {
      rv &= this.vdip.read( port - this.vdipIOBaseAddr );
      done = true;
    }
    if( (this.keyboardHW == KeyboardHW.PORT_RAW)
	&& (port == this.keyboardIOAddr) )
    {
      rv &= this.keyChar;
      done = true;
    }
    return done ? rv : this.unusedPortValue;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    this.keyChar       = 0;
    this.keyboardUsed  = false;
    this.lastTCIOValue = this.fdcTCIOValue;


    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
    }
    boolean coldReset = (resetLevel == EmuThread.ResetLevel.POWER_ON)
		|| (resetLevel == EmuThread.ResetLevel.COLD_RESET);
    if( this.ctc != null ) {
      this.ctc.reset( coldReset );
    }
    if( this.pio != null ) {
      this.pio.reset( coldReset );
    }
    if( this.sio != null ) {
      this.sio.reset( coldReset );
    }
    if( this.fdc != null ) {
      this.fdc.reset( coldReset );
    }
    if( this.floppyDiskDrives != null ) {
      for( int i = 0; i < this.floppyDiskDrives.length; i++ ) {
	FloppyDiskDrive drive = this.floppyDiskDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    if( this.gide != null ) {
      this.gide.reset();
    }
    if( this.kcNet != null ) {
      this.kcNet.reset( coldReset );
    }
    if( this.vdip != null ) {
      this.vdip.reset( coldReset );
    }
    for( CustomSysROM rom : roms ) {
      rom.reset();
    }
  }


  @Override
  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.floppyDiskDrives != null ) {
      if( (idx >= 0) && (idx < this.floppyDiskDrives.length) ) {
	this.floppyDiskDrives[ idx ] = drive;
      }
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    this.emuThread.setRAMByte( addr & 0xFFFF, value );
    if( (addr >= this.screenBegAddr) && (addr <= this.screenEndAddr) ) {
      this.screenFrm.setScreenDirty( true );
    }
    return true;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return (this.screenBegAddr >= 0)
		&& (this.screenCols > 0)
		&& (this.screenRows > 0);
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return (this.keyboardHW != KeyboardHW.NONE);
  }


  @Override
  public boolean supportsPrinter()
  {
    return ((this.sioAout == SioOut.PRINTER)
	    || (this.sioBout == SioOut.PRINTER));
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    port &= 0xFF;
    for( CustomSysROM rom : roms ) {
      rom.writeIOByte( port, value );
    }
    if( (this.ctc != null)
	&& (port >= this.ctcIOBaseAddr)
	&& (port < (this.ctcIOBaseAddr + 4)) )
    {
      this.ctc.write( port - this.ctcIOBaseAddr, value, tStates );
    }
    if( (this.pio != null)
	&& (port >= this.pioIOBaseAddr)
	&& (port < (this.pioIOBaseAddr + 4)) )
    {
      switch( port - this.pioIOBaseAddr ) {
	case 0:
	  this.pio.writeDataA( value );
	  break;
	case 1:
	  this.pio.writeDataB( value );
	  break;
	case 2:
	  this.pio.writeControlA( value );
	  break;
	case 3:
	  this.pio.writeControlB( value );
	  break;
      }
    }
    if( (this.sio != null)
	&& (port >= this.sioIOBaseAddr)
	&& (port < (this.sioIOBaseAddr + 4)) )
    {
      switch( port - this.sioIOBaseAddr ) {
	case 0:
	  this.sio.writeDataA( value );
	  break;
	case 1:
	  this.sio.writeDataB( value );
	  break;
	case 2:
	  this.sio.writeControlA( value );
	  break;
	case 3:
	  this.sio.writeControlA( value );
	  break;
      }
    }
    if( this.fdc != null ) {
      if( port == this.fdcDataIOAddr ) {
	this.fdc.write( value );
      } else if( port == this.fdcTCIOAddr ) {
	if( this.fdcTCIOMask != 0 ) {
	  int tcValue = (this.lastTCIOValue & this.fdcTCIOMask);
	  if( (tcValue != this.lastTCIOValue)
	      && (tcValue == this.fdcTCIOValue) )
	  {
	    this.fdc.fireTC();
	  }
	  this.lastTCIOValue = tcValue;
	} else {
	  this.fdc.fireTC();
	}
      }
    }
    if( (this.gide != null)
	&& (port >= this.gideIOBaseAddr)
	&& (port < (this.gideIOBaseAddr + 16)) )
    {
      this.gide.write( port - this.gideIOBaseAddr, value );
    }
    if( (this.kcNet != null)
	&& (port >= this.kcNetIOBaseAddr)
	&& (port < (this.kcNetIOBaseAddr + 4)) )
    {
      this.kcNet.write( port - this.kcNetIOBaseAddr, value );
    }
    if( (this.vdip != null)
	&& (port >= this.vdipIOBaseAddr)
	&& (port < (this.vdipIOBaseAddr + 4)) )
    {
      this.vdip.write( port - this.vdipIOBaseAddr, value );
    }
  }


  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );
    if( this.fdc != null ) {
      this.fdc.z80MaxSpeedChanged( cpu );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
  }


	/* --- private Methoden --- */

  private boolean emulatesFdc( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_FDC_ENABLED,
			false );
  }


  private static int getGideIOBaseAddr( Properties props )
  {
    return EmuUtil.getIntProperty(
			props,
			PROP_PREFIX + GIDE.PROP_IOBASEADDR,
			DEFAULT_GIDE_IOBASEADDR ) & 0xFF;
  }


  private static KeyboardHW getKeyboardHW( Properties props )
  {
    KeyboardHW rv = KeyboardHW.NONE;
    switch( EmuUtil.getProperty( props, PROP_PREFIX + PROP_KEYBOARD_HW ) ) {
      case VALUE_KEYBOARD_PORT_RAW:
	rv = KeyboardHW.PORT_RAW;
	break;
      case VALUE_KEYBOARD_PIO_A_HS:
	rv = KeyboardHW.PIO_A_HS;
	break;
      case VALUE_KEYBOARD_PIO_A_BIT7:
	rv = KeyboardHW.PIO_A_BIT7;
	break;
      case VALUE_KEYBOARD_PIO_B_HS:
	rv = KeyboardHW.PIO_B_HS;
	break;
      case VALUE_KEYBOARD_PIO_B_BIT7:
	rv = KeyboardHW.PIO_B_BIT7;
	break;
      case VALUE_KEYBOARD_SIO_A:
	rv = KeyboardHW.SIO_A;
	break;
      case VALUE_KEYBOARD_SIO_B:
	rv = KeyboardHW.SIO_B;
	break;
    }
    return rv;
  }


  private static SioOut getSioOut( Properties props, String propName )
  {
    SioOut rv = SioOut.NONE;
    switch( EmuUtil.getProperty( props, PROP_PREFIX + propName ) ) {
      case VALUE_PRINTER:
	rv = SioOut.PRINTER;
	break;
    }
    return rv;
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				this.propPrefix + PROP_FONT_FILE,
				0x0800 );
    if( this.fontBytes == null ) {
      if( romFont == null ) {
	romFont = readResource( "/rom/customsys/cp437.bin" );
      }
      this.fontBytes = romFont;
    }
  }


  private void loadROMs( Properties props )
  {
    for( CustomSysROM rom : this.roms ) {
      rom.load( this.emuThread.getScreenFrm() );
    }
    loadFont( props );
  }


  private boolean putKeyChar( int ch )
  {
    boolean rv = false;
    if( ch > 0 ) {
      ch = cp437.toCharsetByte( (char) ch );
    }
    switch( this.keyboardHW ) {
      case PORT_RAW:
	if( (ch >= 0) && (ch <= 0xFF) ) {
	  this.keyChar = ch;
	  rv           = true;
	}
	break;
      case PIO_A_HS:
	if( (this.pio != null) && (ch > 0) && (ch <= 0xFF) ) {
	  this.pio.putInValuePortA( ch, true );
	  rv = true;
	}
	break;
      case PIO_A_BIT7:
	if( this.pio != null ) {
	  if( ch == 0 ) {
	    this.pio.putInValuePortA( 0, 0xFF );
	    rv = true;
	  }
	  else if( (ch > 0) && (ch <= 0x7F) ) {
	    this.pio.putInValuePortA( ch | 0x80, 0xFF );
	    rv = true;
	  }
	}
	break;
      case PIO_B_HS:
	if( (this.pio != null) && (ch > 0) && (ch <= 0xFF) ) {
	  this.pio.putInValuePortB( ch, true );
	  rv = true;
	}
	break;
      case PIO_B_BIT7:
	if( this.pio != null ) {
	  if( ch == 0 ) {
	    this.pio.putInValuePortB( 0, 0xFF );
	    rv = true;
	  }
	  else if( (ch > 0) && (ch <= 0x7F) ) {
	    this.pio.putInValuePortB( ch | 0x80, 0xFF );
	    rv = true;
	  }
	}
	break;
      case SIO_A:
	if( (this.sio != null) && (ch > 0) && (ch <= 0xFF) ) {
	  this.sio.putToReceiverA( ch );
	  rv = true;
	}
	break;
      case SIO_B:
	if( (this.sio != null) && (ch > 0) && (ch <= 0xFF) ) {
	  this.sio.putToReceiverB( ch );
	  rv = true;
	}
	break;
    }
    return rv;
  }


  private void updSwapKeyCharCase( Properties props )
  {
    this.swapKeyCharCase = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_SWAP_KEY_CHAR_CASE,
				DEFAULT_SWAP_KEY_CHAR_CASE );
  }
}
