/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Poly-Computers 880
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.emusys.poly880.Poly880KeyboardFld;
import jkcemu.text.TextUtil;
import z80emu.*;


public class Poly880 extends EmuSys implements
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private static final int[] key4 = { 'G', -1,  'X', '-', 'R', -1,  'S', 'M' };
  private static final int[] key5 = { '0', '2', '3', '1', '8', '9', 'B', 'A' };
  private static final int[] key7 = { '4', '6', '7', '5', 'C', 'D', 'F', 'E' };

  private static byte[] mon0000 = null;
  private static byte[] mon1000 = null;

  private byte[]             ram;
  private byte[]             rom0Bytes;
  private byte[]             rom1Bytes;
  private byte[]             rom2Bytes;
  private byte[]             rom3Bytes;
  private String             rom0File;
  private String             rom1File;
  private String             rom2File;
  private String             rom3File;
  private int[]              keyboardMatrix;
  private int[]              digitStatus;
  private int[]              digitValues;
  private int                colMask;
  private long               curDisplayTStates;
  private long               displayCheckTStates;
  private boolean            audioInPhase;
  private boolean            nmiEnabled;
  private boolean            nmiTrigger;
  private boolean            ram8000;
  private boolean            extRomsNegated;
  private Z80CTC             ctc;
  private Z80PIO             pio1;
  private Z80PIO             pio2;
  private Poly880KeyboardFld keyboardFld;


  public Poly880( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, "jkcemu.poly880." );
    this.rom0Bytes           = null;
    this.rom1Bytes           = null;
    this.rom2Bytes           = null;
    this.rom3Bytes           = null;
    this.rom0File            = null;
    this.rom1File            = null;
    this.rom2File            = null;
    this.rom3File            = null;
    this.ram                 = new byte[ 0x0400 ];
    this.ram8000             = emulatesRAM8000( props );
    this.extRomsNegated      = emulatesNegatedROMs( props );
    this.curDisplayTStates   = 0;
    this.displayCheckTStates = 0;
    this.digitStatus         = new int[ 8 ];
    this.digitValues         = new int[ 8 ];
    this.keyboardMatrix      = new int[ 8 ];
    this.keyboardFld         = null;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio1  = new Z80PIO( "PIO (80-83)" );
    this.pio2  = new Z80PIO( "PIO (84-87)" );
    this.ctc   = new Z80CTC( "CTC (88-8B)" );
    cpu.setInterruptSources( this.pio1, this.pio2, this.ctc );

    this.ctc.setTimerConnection( 2, 3 );
    this.ctc.addCTCListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    z80MaxSpeedChanged( cpu );

    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
  }


  public void fireMonKey()
  {
    if( this.nmiEnabled )
      this.emuThread.getZ80CPU().fireNMI();
  }


  public static int getDefaultSpeedKHz()
  {
    return 922;		// eigentlich 921,6
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    synchronized( this.keyboardMatrix ) {
      int n = Math.min( kbMatrix.length, this.keyboardMatrix.length );
      int i = 0;
      while( i < n ) {
	this.keyboardMatrix[ i ] = kbMatrix[ i ];
	i++;
      }
      while( i < this.keyboardMatrix.length ) {
	this.keyboardMatrix[ i ] = 0;
	i++;
      }
    }
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    // NMI-Ausloesung durch CTC
    if( (ctc == this.ctc) && (timerNum == 0) && this.nmiEnabled ) {
      this.nmiEnabled = false;
      this.nmiTrigger = true;	// nach dem naechsten Befehl NMI ausloesen
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.displayCheckTStates = cpu.getMaxSpeedKHz() * 50;
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    boolean phase = this.emuThread.readAudioPhase();
    if( phase != this.audioInPhase ) {
      this.audioInPhase = phase;
      this.pio1.putInValuePortB( this.audioInPhase ? 0x02 : 0, 0x02 );
    }
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.displayCheckTStates > 0 ) {
      this.curDisplayTStates += tStates;
      if( this.curDisplayTStates > this.displayCheckTStates ) {
	boolean dirty = false;
	synchronized( this.digitValues ) {
	  for( int i = 0; i < this.digitValues.length; i++ ) {
	    if( this.digitStatus[ i ] > 0 ) {
	      --this.digitStatus[ i ];
	    } else {
	      if( this.digitValues[ i ] != 0 ) {
		this.digitValues[ i ] = 0;
		dirty = true;
	      }
	    }
	  }
	}
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
	this.curDisplayTStates = 0;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
				props,
				"jkcemu.system" ).equals( "Poly880" );
    if( rv ) {
      rv = TextUtil.equals(
		this.rom0File,
		EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_0000.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.rom1File,
		EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_1000.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.rom2File,
		EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_2000.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.rom3File,
		EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_3000.file" ) );
    }
    if( rv
	&& ((this.rom0File != null) || (this.rom1File != null)
	    || (this.rom2File != null) || (this.rom3File != null)) )
    {
      if( emulatesNegatedROMs( props ) != this.extRomsNegated ) {
	rv = false;
      }
    }
    if( rv && (emulatesRAM8000( props ) != this.ram8000) ) {
      rv = false;
    }
    return rv;
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new Poly880KeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x43A2;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
	color = this.colorGreenDark;
	break;

      case 2:
	color = this.colorGreenLight;
	break;
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return 3;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/poly880.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( addr < 0x8000 ) {
      addr &= 0xF3FF;		// A10 und A11 ignorieren
      if( (addr < 0x1000) && (this.rom0Bytes != null) ) {
	if( addr < this.rom0Bytes.length ) {
	  rv = (int) this.rom0Bytes[ addr ] & 0xFF;
	}
      }
      else if( (addr >= 0x1000) && (addr < 0x2000)
	       && (this.rom1Bytes != null) )
      {
	int idx = addr - 0x1000;
	if( idx < this.rom1Bytes.length ) {
	  rv = (int) this.rom1Bytes[ idx ] & 0xFF;
	}
      }
      else if( (addr >= 0x2000) && (addr < 0x3000)
	       && (this.rom2Bytes != null) )
      {
	int idx = addr - 0x2000;
	if( idx < this.rom2Bytes.length ) {
	  rv = (int) this.rom2Bytes[ idx ] & 0xFF;
	}
      }
      else if( (addr >= 0x3000) && (addr < 0x4000)
	       && (this.rom3Bytes != null) )
      {
	int idx = addr - 0x3000;
	if( idx < this.rom3Bytes.length ) {
	  rv = (int) this.rom3Bytes[ idx ] & 0xFF;
	}
      }
      else if( addr >= 0x4000 ) {
	int idx = addr - 0x4000;
	if( idx < this.ram.length ) {
	  rv = (int) this.ram[ idx ] & 0xFF;
	}
      }
    } else {
      if( this.ram8000 ) {
	rv = this.emuThread.getRAMByte( addr );
      }
    }
    return rv;
  }


  @Override
  public int getScreenHeight()
  {
    return 85;
  }


  @Override
  public int getScreenWidth()
  {
    return (this.digitValues.length * 65) - 15;
  }


  @Override
  public String getTitle()
  {
    return "Poly-Computer 880";
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.keyboardMatrix ) {
      switch( keyCode ) {
	case KeyEvent.VK_BACK_SPACE:
	  this.keyboardMatrix[ 3 ] |= 0x10;	// BACK
	  rv = true;
	  break;

	case KeyEvent.VK_ENTER:
	  this.keyboardMatrix[ 2 ] |= 0x10;	// EXEC
	  rv = true;
	  break;

	case KeyEvent.VK_ESCAPE:
	  this.emuThread.fireReset( EmuThread.ResetLevel.WARM_RESET );
	  rv = true;
	  break;

	case KeyEvent.VK_F1:
	  this.keyboardMatrix[ 5 ] |= 0x10;	// FCT
	  rv = true;
	  break;

	case KeyEvent.VK_F2:			// MON
	  fireMonKey();
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    if( keyChar > 0 ) {
      int ch = Character.toUpperCase( keyChar );
      rv = setKeyMatrixValue( key4, ch, 0x10 );
      if( !rv ) {
	rv = setKeyMatrixValue( key5, ch, 0x20 );
      }
      if( !rv ) {
	rv = setKeyMatrixValue( key7, ch, 0x80 );
      }
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    synchronized( this.digitValues ) {
      for( int i = 0; i < this.digitValues.length; i++ ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitValues[ i ],
		this.colorGreenDark,
		this.colorGreenLight,
		screenScale );
	x += (65 * screenScale);
      }
    }
    return true;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;

    switch( port & 0x8F ) {	// A4 bis A6 ignorieren
      case 0x80:
	rv = this.pio1.readDataA();
	break;

      case 0x81:
	rv = this.pio1.readControlA();
	break;

      case 0x82:
	{
	  int v = 0;
	  synchronized( this.keyboardMatrix ) {
	    int m = 0x01;
	    for( int i = 0; i < this.keyboardMatrix.length; i++ ) {
	      if( (this.colMask & m) != 0 ) {
		v |= this.keyboardMatrix[ i ];
	      }
	      m <<= 1;
	    }
	  }
	  this.pio1.putInValuePortB( v & 0xB0, 0xFD );
	  rv = this.pio1.readDataB();
	}
	break;

      case 0x83:
	rv = this.pio1.readControlB();
	break;

      case 0x84:
	rv = this.pio2.readDataA();
	break;

      case 0x85:
	rv = this.pio2.readControlA();
	break;

      case 0x86:
	rv = this.pio2.readDataB();
	break;

      case 0x87:
	rv = this.pio2.readControlB();
	break;

      case 0x88:
      case 0x89:
      case 0x8A:
      case 0x8B:
      rv = this.ctc.read( port & 0x03, tStates );
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    int b = getMemByte( addr, m1 );
    if( m1 && this.nmiTrigger ) {
      this.nmiTrigger = false;
      this.emuThread.getZ80CPU().fireNMI();
    }
    return b;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      initSRAM( this.ram, props );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    synchronized( this.keyboardMatrix ) {
      Arrays.fill( this.keyboardMatrix, 0 );
    }
    this.colMask      = 0;
    this.audioInPhase = this.emuThread.readAudioPhase();
    this.nmiEnabled   = true;
    this.nmiTrigger   = false;
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    boolean rv = false;

    addr &= 0xFFFF;
    if( addr < 0x8000 ) {
      addr &= 0xF3FF;		// A10 und A11 ignorieren
      if( addr >= 0x4000 ) {
	int idx = addr - 0x4000;
	if( idx < this.ram.length ) {
	  this.ram[ idx ] = (byte) value;
	  rv = true;
	}
      }
    } else {
      if( this.ram8000 ) {
	this.emuThread.setRAMByte( addr, value );
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public boolean supportsAudio()
  {
    return true;
  }


  @Override
  public boolean supportsKeyboardFld()
  {
    return true;
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    int v = 0;
    switch( port & 0x8F ) {	// A4 bis A6 ignorieren
      case 0x80:
	this.pio1.writeDataA( value );
	break;

      case 0x81:
	this.pio1.writeControlA( value );
	break;

      case 0x82:
	this.pio1.writeDataB( value );
	v = this.pio1.fetchOutValuePortB( false );
	this.emuThread.writeAudioPhase(
		(v & (this.emuThread.isSoundOutEnabled() ?
						0x01 : 0x04 )) != 0 );
	this.nmiEnabled = ((v & 0x40) == 0);
	break;

      case 0x83:
	this.pio1.writeControlB( value );
	break;

      case 0x84:
	this.pio2.writeDataA( value );
	break;

      case 0x85:
	this.pio2.writeControlA( value );
	break;

      case 0x86:
	this.pio2.writeDataB( value );
	break;

      case 0x87:
	this.pio2.writeControlB( value );
	break;

      case 0x88:
      case 0x89:
      case 0x8A:
      case 0x8B:
      this.ctc.write( port & 0x03, value, tStates );
    }
    if( (port & 0xB0) == 0xB0 ) {
      this.colMask  = value;
      boolean dirty = false;
      v = toDigitValue( this.pio1.fetchOutValuePortA( false ) );
      synchronized( this.digitValues ) {
	int m = 0x80;
	for( int i = 0; i < this.digitValues.length; i++ ) {
	  if( (value & m) != 0 ) {
	    this.digitStatus[ i ] = 2;
	    if( this.digitValues[ i ] != v ) {
	      this.digitValues[ i ] = v;
	      dirty = true;
	    }
	  }
	  m >>= 1;
	}
      }
      if( dirty ) {
	this.screenFrm.setScreenDirty( true );
      }
    }
  }


	/* --- private Methoden --- */

  private boolean emulatesNegatedROMs( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom.negated",
				false );
  }


  private boolean emulatesRAM8000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_8000.enabled",
				false );
  }


  private void loadROMs( Properties props )
  {
    this.rom0File  = EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_0000.file" );
    this.rom0Bytes = readPoly880ROMFile( this.rom0File, "ROM 0" );
    if( this.rom0Bytes == null ) {
      if( mon0000 == null ) {
	mon0000 = readResource( "/rom/poly880/poly880_0000.bin" );
      }
      this.rom0Bytes = mon0000;
    }

    this.rom1File = EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_1000.file" );
    this.rom1Bytes = readPoly880ROMFile( this.rom1File, "ROM 1" );
    if( this.rom1Bytes == null ) {
      if( mon1000 == null ) {
	mon1000 = readResource( "/rom/poly880/poly880_1000.bin" );
      }
      this.rom1Bytes = mon1000;
    }

    this.rom2File  = EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_2000.file" );
    this.rom2Bytes = readPoly880ROMFile( this.rom2File, "ROM 2" );

    this.rom3File  = EmuUtil.getProperty(
				props,
				this.propPrefix + "rom_3000.file" );
    this.rom3Bytes = readPoly880ROMFile( this.rom3File, "ROM 3" );
  }


  private byte[] readPoly880ROMFile( String fileName, String objName )
  {
    byte[] rom = readROMFile( fileName, 0x0400, objName );
    if( (rom != null) && this.extRomsNegated ) {
      for( int i = 0; i < rom.length; i++ ) {
	rom[ i ] = (byte) ~rom[ i ];
      }
    }
    return rom;
  }


  private boolean setKeyMatrixValue( int[] rowKeys, int ch, int value )
  {
    boolean rv = false;
    synchronized( this.keyboardMatrix ) {
      int n  = Math.min( rowKeys.length, this.keyboardMatrix.length );
      for( int i = 0; i < n; i++ ) {
	if( ch == rowKeys[ i ] ) {
	  this.keyboardMatrix[ i ] |= value;
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


  /*
   * Eingang: H-Aktiv
   *   Bit: 0 -> E
   *   Bit: 1 -> D
   *   Bit: 2 -> C
   *   Bit: 3 -> P
   *   Bit: 4 -> G
   *   Bit: 5 -> A
   *   Bit: 6 -> F
   *   Bit: 7 -> B
   *
   * Ausgang: H-Aktiv
   *   Bit: 0 -> A
   *   Bit: 1 -> B
   *   Bit: 2 -> C
   *   Bit: 3 -> D
   *   Bit: 4 -> E
   *   Bit: 5 -> F
   *   Bit: 6 -> G
   *   Bit: 7 -> P
   */
  private int toDigitValue( int value )
  {
    int rv = value & 0x04;	// C stimmt ueberein
    if( (value & 0x01) != 0 ) {
      rv |= 0x10;
    }
    if( (value & 0x02) != 0 ) {
      rv |= 0x08;
    }
    if( (value & 0x08) != 0 ) {
      rv |= 0x80;
    }
    if( (value & 0x10) != 0 ) {
      rv |= 0x40;
    }
    if( (value & 0x20) != 0 ) {
      rv |= 0x01;
    }
    if( (value & 0x40) != 0 ) {
      rv |= 0x20;
    }
    if( (value & 0x80) != 0 ) {
      rv |= 0x02;
    }
    return rv;
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyboardMatrix );
  }
}

