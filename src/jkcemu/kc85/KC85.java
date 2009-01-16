/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des KC85/2..4
 */

package jkcemu.kc85;

import java.awt.Color;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class KC85 extends EmuSys implements
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80TStatesListener
{
  private int[][] rgbValues = {

			// primaere Vordergrundfarben
			{ 0,   0,   0 },	// schwarz
			{ 0,   0,   255 },	// blau
			{ 255, 0,   0 },	// rot
			{ 255, 0,   255 },	// purpur
			{ 0,   255, 0 },	// gruen
			{ 0,   255, 255 },	// tuerkis
			{ 255, 255, 0 },	// gelb
			{ 255, 255, 255 },	// weiss

			// Vordergrundfarben mit 30 Grad Drehung im Farbkreis
			{ 0,   0,   0 },	// schwarz
			{ 75,  0,   180 },	// violett
			{ 180, 75,  0 },	// orange
			{ 180, 0,   138 },	// purpurrot
			{ 0,   180, 75 },	// gruenblau
			{ 0,   138, 180 },	// blaugruen
			{ 138, 255, 0 },	// gelbgruen
			{ 255, 255, 255 },	// weiss

			// Hintergrundfarben (30% dunkler)
			{ 0,   0,   0 },	// schwarz
			{ 0,   0,   180 },	// blau
			{ 180, 0,   0 },	// rot
			{ 180, 0,   180 },	// purpur
			{ 0,   180, 0 },	// gruen
			{ 0,   180, 180 },	// tuerkis
			{ 180, 180, 0 },	// gelb
			{ 180, 180, 180 } };	// weiss

  /*
   * Beim KC85/2..4 weicht der Zeichensatz vom ASCII-Standard etwas ab.
   * Deshalb werden hier nur die Tastencodes gemappt,
   * die auf dem emulierten Rechner zur Anzeige des inhaltlich
   * gleichen Zeichens fuehrt.
   *
   * Konkret bestehen folgende Unterschiede:
   *
   * HEX  ASCII  Mapping  KC
   * 5B   [               Vollzeichen
   * 5C   \        +-->   |
   * 5D   ]        |      Negationszeichen
   * 60   `        |      Copyright
   * 7B   {        |      Umlaut ae
   * 7C   |      --+      Umlaut oe
   * 7D   }               Umlaut ue
   * 7E   ~               Umlaut sz
   */
  private static int[] char2KeyNum = {
	 -1,  24,  41,  60,  -1,  -1,  -1,   6,		// 0x00
	 -1, 122, 118, 120,   9, 126,  -1,  25,
	  8, 121, 119,  76,  57,  -1,  -1,  -1,		// 0x10
	123,   7,  56,  77,   9,  -1,  -1,  40,
	 70, 117,   5,  21, 101,  37,  85,  53,		// 0x20   !"#$%&'
	 69,  59,  27, 104,  74,  10,  90, 106,		// 0x28  ()*+,-./
	 42, 116,   4,  20, 100,  36,  84,  52,		// 0x30  01234567
	 68,  58,  26, 105,  75,  11,  91, 107,		// 0x38  89:;<=>?
	 43,   2,  94, 110,  98,  16,  34,  82,		// 0x40  @ABCDEFG
	 50,  64,  66,  72,  88,  78,  62,  54,		// 0x48  HIJKLMNO
	 38, 112,  96,  18,  32,  48,  46,   0,		// 0x50  PQRSTUVW
	 30,  14,  80,  -1,  -1,  -1,  22, 102,		// 0x58  XYZ   ^_
	 -1,   3,  95, 111,  99,  17,  35,  83,		// 0x60   abcdefg
	 51,  65,  67,  73,  89,  79,  63,  55,		// 0x68  hijklmno
	 39, 113,  97,  19,  33,  49,  47,   1,		// 0x70  pqrstuvw
	 31,  15,  81,  -1, 103,  -1,  -1,  -1,		// 0x78  xyz |
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0x80
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0x90
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xA0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xB0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xC0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xD0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xE0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1, 124,  12,  28, 108,  44,  92, 125,		// 0xF0  F0...F7
	 13,  29, 109,  45,  93,  -1,  -1,  -1 };	// 0xF8  F8...FC


  private static byte[] basic_c000  = null;
  private static byte[] caos22_e000 = null;
  private static byte[] caos22_f000 = null;
  private static byte[] caos31_e000 = null;
  private static byte[] caos31_f000 = null;
  private static byte[] caos42_c000 = null;
  private static byte[] caos42_e000 = null;
  private static byte[] caos42_f000 = null;

  private volatile boolean blinkEnabled;
  private volatile boolean blinkState;
  private volatile boolean hiColorRes;
  private boolean          basicC000Enabled;
  private boolean          caosC000Enabled;
  private boolean          caosE000Enabled;
  private boolean          irmEnabled;
  private boolean          ram0Enabled;
  private boolean          ram0Writeable;
  private boolean          ram4Enabled;
  private boolean          ram4Writeable;
  private boolean          ram8Enabled;
  private boolean          ram81Enabled;
  private boolean          ram8Writeable;
  private boolean          ramColorEnabled;
  private boolean          screen1Enabled;
  private volatile boolean screen1Visible;
  private boolean          audioOutPhaseL;
  private boolean          audioOutPhaseR;
  private boolean          audioInPhase;
  private int              audioInTStates;
  private volatile int     keyNumPressed;
  private int              keyNumProcessing;
  private int              keyShiftBitCnt;
  private int              keyShiftValue;
  private int              keyTStates;
  private volatile int     tStatesPerLine;
  private int              lineTStateCounter;
  private int              ctcInCounter;
  private int              kcTypeNum;
  private byte[]           basicC000;
  private byte[]           caosC000;
  private byte[]           caosE000;
  private byte[]           caosF000;
  private byte[]           ram80;
  private byte[]           ram81;
  private byte[]           ramColor0;
  private byte[]           ramColor1;
  private byte[]           ramPixel0;
  private byte[]           ramPixel1;
  private Color[]          colors;
  private String           sysName;
  private Z80PIO           pio;
  private Z80CTC           ctc;


  public KC85( EmuThread emuThread, Properties props )
  {
    super( emuThread );
    this.sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( this.sysName.startsWith( "KC85/2" )
	|| this.sysName.startsWith( "HC900" ) )
    {
      if( caos22_e000 == null ) {
	caos22_e000 = readResource( "/rom/kc85/caos22_e000.bin" );
      }
      if( caos22_f000 == null ) {
	caos22_f000 = readResource( "/rom/kc85/caos22_f000.bin" );
      }
      this.kcTypeNum = 2;
      this.basicC000 = null;
      this.caosC000  = null;
      this.caosE000  = caos22_e000;
      this.caosF000  = caos22_f000;
      this.sysName   = "KC85/2 (HC900)";
    } else {
      if( basic_c000 == null ) {
	basic_c000 = readResource( "/rom/kc85/basic_c000.bin" );
      }
      this.basicC000 = basic_c000;
      if( this.sysName.startsWith( "KC85/3" ) ) {
	if( caos31_e000 == null ) {
	  caos31_e000 = readResource( "/rom/kc85/caos31_e000.bin" );
	}
	if( caos31_f000 == null ) {
	  caos31_f000 = readResource( "/rom/kc85/caos31_f000.bin" );
	}
	this.kcTypeNum = 3;
	this.caosC000  = null;
	this.caosE000  = caos31_e000;
	this.caosF000  = caos31_f000;
	this.sysName   = "KC85/3";
      } else {
	if( caos42_c000 == null ) {
	  caos42_c000 = readResource( "/rom/kc85/caos42_c000.bin" );
	}
	if( caos42_e000 == null ) {
	  caos42_e000 = readResource( "/rom/kc85/caos42_e000.bin" );
	}
	if( caos42_f000 == null ) {
	  caos42_f000 = readResource( "/rom/kc85/caos42_f000.bin" );
	}
	this.kcTypeNum = 4;
	this.caosC000  = caos42_c000;
	this.caosE000  = caos42_e000;
	this.caosF000  = caos42_f000;
	this.sysName   = "KC85/4";
      }
    }
    this.ram80     = new byte[ 0x4000 ];
    this.ram81     = new byte[ 0x4000 ];
    this.ramColor0 = new byte[ 0x4000 ];
    this.ramColor1 = new byte[ 0x4000 ];
    this.ramPixel0 = new byte[ 0x4000 ];
    this.ramPixel1 = new byte[ 0x4000 ];

    this.colors = new Color[ rgbValues.length ];
    for( int i = 0; i < rgbValues.length; i++ ) {
      this.colors[ i ] = new Color(
				rgbValues[ i ][ 0 ],
				rgbValues[ i ][ 1 ],
				rgbValues[ i ][ 2 ] );
    }

    this.tStatesPerLine = 0;

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc = new Z80CTC( cpu, null );
    this.pio = new Z80PIO( cpu, this.ctc );
    this.ctc.addCTCListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    reset( EmuThread.ResetLevel.POWER_ON );
    z80MaxSpeedChanged();
  }


	/* --- Z80CTCListener --- */

  /*
   * CTC-Ausgaenge:
   *   Kanal 0: Tonausgabe rechts
   *   Kanal 1: Tonausgabe links
   *   Kanal 2: Blinken
   */
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:
	  this.audioOutPhaseR = !this.audioOutPhaseR;
	  updAudioOut();
	  break;

	case 1:
	  this.audioOutPhaseL = !this.audioOutPhaseL;
	  updAudioOut();
	  break;

	case 2:
	  this.blinkState = !this.blinkState;
	  this.screenFrm.setScreenDirty( true );
	  break;
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  public void z80MaxSpeedChanged()
  {
    this.tStatesPerLine = this.emuThread.getZ80CPU().getMaxSpeedKHz()
					* 1000 / (624 * 50);
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.systemUpdate( tStates );

    /*
     * CTC-Eingaenge:
     *   Kanal 0 und 1: doppelte Zeilensynchronfrequenz
     *   Kanal 2 und 3: Bildsynchronimpulse
     *
     * Der CTC-Kanal mit der hoechsten Prioritaet bekommt zuletzt den Impuls,
     * damit er bei gleichzeitigem Erfuelltsein der Interruptbedingung
     * mit einem niedriger priorisierten Kanal bei diesem
     * die Interruptsausloesung nicht unterdrueckt.
     */
    int tStatesPerLine = this.tStatesPerLine;
    if( tStatesPerLine > 0 ) {
      this.lineTStateCounter += tStates;
      while( this.lineTStateCounter >= tStatesPerLine ) {
	this.ctcInCounter++;
	if( this.ctcInCounter >= 624 ) {
	  this.ctc.externalUpdate( 3, 1 );
	  this.ctc.externalUpdate( 2, 1 );
	  this.ctcInCounter = 0;
	}
	this.ctc.externalUpdate( 1, 1 );
	this.ctc.externalUpdate( 0, 1 );
	this.lineTStateCounter -= tStatesPerLine;

      }
    }

    /*
     * Der Kassettenrecorderanschluss wird eingangsseitig emuliert,
     * indem zyklisch geschaut wird, ob sich die Eingangsphase geaendert hat.
     * Wenn ja, wird ein Impuls an der Strobe-Leitung der zugehoerigen PIO
     * emuliert.
     * Auf der einen Seite soll das Audiosystem nicht zu oft abgefragt
     * werden.
     * Auf der anderen Seite sollen aber die Zykluszeit nicht so gross werden,
     * dass die Genauigkeit der Zeitmessung kuenstlich verschlechert wird.
     * Aus diesem Grund werden genau soviele Taktzyklen abgezaehlt,
     * wie auch der Vorteile der CTC mindestens zaehlen muss.
     */
    this.audioInTStates += tStates;
    if( this.audioInTStates > 15 ) {
      this.audioInTStates = 0;
      if( this.emuThread.readAudioPhase() != this.audioInPhase ) {
	this.audioInPhase = !this.audioInPhase;

	/*
	 * Bei jedem Phasenwechsel wird ein Impuls an ASTB erzeugt,
	 * was je nach Betriebsart der PIO eine Ein- oder Ausgabe bedeutet
	 * und, das ist das eigentliche Ziel, einen Interrupt ausloest.
	 */
	switch( this.pio.getModePortA() ) {
	  case Z80PIO.MODE_BYTE_IN:
	    this.pio.putInValuePortA( 0xFF, true );
	    break;

	  case Z80PIO.MODE_BYTE_INOUT:
	  case Z80PIO.MODE_BYTE_OUT:
	    this.pio.fetchOutValuePortA( true );
	    break;
	}
      }
    }

    /*
     * Beim Tastaturanschluss wird entsprechend der Pulsabstaende
     * des Bitmusters der Tastennummer jeweils ein Impuls
     * an der Strobe-Leitung der zugehoerigen PIO emuliert.
     * Die Zeitabstaende entsprechen denen des Schaltkreises U807.
     * Die angegebenen Takte beziehen sich auf die Standardtaktfrequenz.
     * Wird eine andere Taktfrequenz eingestellt,
     * laeuft alles mit der anderen Taktfrequenz,
     * die Zeitverhaeltnisse zueinander bleiben immer gleich.
     */
    if( this.keyShiftBitCnt <= 0 ) {
      if( this.keyTStates > 0 ) {
	this.keyTStates -= tStates;
      }
      if( this.keyTStates <= 0 ) {
	int keyNum = this.keyNumPressed;
	if( keyNum >= 0 ) {
	  this.keyNumProcessing = keyNum;
	  this.keyShiftValue    = keyNum;
	  this.keyShiftBitCnt   = 8;
	}
	this.keyTStates = 0;
      }
    }
    if( this.keyShiftBitCnt > 0 ) {
      boolean pulse = false;
      if( this.keyShiftBitCnt == 8 ) {
	pulse = true;				// Startimpuls
      }
      else if( this.keyShiftBitCnt < 8 ) {
	if( (this.keyShiftValue & 0x01) != 0 ) {
	  // 1-Bit: 7,14 ms -> 12496 Takte
	  if( this.keyTStates >= 12496 ) {
	    pulse = true;
	    this.keyShiftValue >>= 1;		// Bit verarbeitet
	  }
	} else {
	  // 0-Bit: 5,12 ms -> 8960 Takte
	  if( this.keyTStates >= 8960 ) {
	    pulse = true;
	    this.keyShiftValue >>= 1;		// Bit verarbeitet
	  }
	}
      }
      if( pulse ) {
	if( this.pio.getModePortA() == Z80PIO.MODE_BYTE_INOUT ) {
	  // BSTB wird fuer die Eingabe bei Port A verwendet
	  this.pio.putInValuePortA( 0xFF, true );
	} else {
	  switch( this.pio.getModePortB() ) {
	    case Z80PIO.MODE_BYTE_IN:
	      this.pio.putInValuePortB( 0xFF, true );
	      break;

	    case Z80PIO.MODE_BYTE_OUT:
	      this.pio.fetchOutValuePortB( true );
	      break;
	  }
	}
	if( this.keyShiftBitCnt == 1 ) {
	  // letztes Bit verarbeitet
	  int keyNum = this.keyNumPressed;
	  if( keyNum >= 0 ) {
	    if( keyNum == this.keyNumProcessing ) {
	      // Wortabstand 14,43 ms -> 25253 Takte
	      this.keyTStates = 25253;
	    } else {
	      // Doppelwortabstand 19,46 ms -> 34055 Takte
	      this.keyTStates = 34055;
	    }
	  } else {
	    this.keyTStates = 0;
	  }
	  this.keyShiftBitCnt = 0;
	} else {
	  this.keyTStates = 0;
	  //this.keyTStates += tStates;
	  --this.keyShiftBitCnt;
	}
      } else {
	this.keyTStates += tStates;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    this.ctc.removeCTCListener( this );
  }


  public String extractScreenText()
  {
    return extractMemText( 0xB200, 32, 40, 40 );
  }


  public int getBorderColorIndex()
  {
    return 0;	// schwarz
  }


  public Color getColor( int colorIdx )
  {

    Color color = null;
    if( this.colors != null ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) )
	color = this.colors[ colorIdx ];
    }
    return color != null ? color : super.getColor( colorIdx );
  }


  public int getColorCount()
  {
    return rgbValues.length;
  }


  public int getColorIndex( int x, int y )
  {
    int rv  = 0;
    int col = x / 8;
    if( this.kcTypeNum > 3 ) {
      boolean screen1  = this.screen1Visible;
      byte[]  ramPixel = screen1Visible ? this.ramPixel1 : this.ramPixel0;
      byte[]  ramColor = screen1Visible ? this.ramColor1 : this.ramColor0;
      int    idx       = (col * 256) + y;
      if( (idx >= 0) && (idx < ramPixel.length) ) {
	int p = ramPixel[ idx ];
	int c = ramColor[ idx ];
	int m = 0x80;
	int n = x % 8;
	if( n > 0 ) {
	  m >>= n;
	}
	if( this.hiColorRes ) {
	  if( (p & m) != 0 ) {
	    if( (c & m) != 0 ) {
	      rv = 7;			// weiss
	    } else {
	      rv = 2;			// rot
	    }
	  } else {
	    if( (c & m) != 0 ) {
	      rv = 5;			// tuerkis
	    } else {
	      rv = 0;			// schwarz
	    }
	  }
	} else {
	  rv = getColorIndex( c, (p & m) != 0 );
	}
      }
    } else {
      int pIdx = -1;
      int cIdx = -1;
      if( col < 32 ) {
	pIdx = ((y << 5) & 0x1E00)
		| ((y << 7) & 0x0180)
		| ((y << 3) & 0x0060)
		| (col & 0x001F);
	cIdx = 0x2800 | ((y << 3) & 0x07E0) | (col & 0x001F);
      } else {
	pIdx = 0x2000
		| ((y << 3) & 0x0600)
		| ((y << 7) & 0x0180)
		| ((y << 3) & 0x0060)
		| ((y >> 1) & 0x0018)
		| (col & 0x0007);
	cIdx = 0x3000
		| ((y << 1) & 0x0180)
		| ((y << 3) & 0x0060)
		| ((y >> 1) & 0x0018)
		| (col & 0x0007);
      }
      if( (pIdx >= 0) && (pIdx < this.ramPixel0.length)
	  && (cIdx >= 0) && (cIdx < this.ramPixel0.length) )
      {
	int p = this.ramPixel0[ pIdx ];
	int c = this.ramPixel0[ cIdx ];
	int m = 0x80;
	int n = x % 8;
	if( n > 0 ) {
	  m >>= n;
	}
	rv = getColorIndex( c, (p & m) != 0 );
      }
    }
    return rv;
  }


  public int getDefaultStartAddress()
  {
    return 0x0300;
  }


  public int getMinOSAddress()
  {
    return 0xE000;
  }


  public int getMaxOSAddress()
  {
    return 0xFFFF;
  }


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( (addr >= 0) && (addr < 0x4000) ) {
      if( this.ram0Enabled ) {
	rv = this.emuThread.getRAMByte( addr );
      }
    } else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( this.ram4Enabled ) {
	rv = this.emuThread.getRAMByte( addr );
      }
    } else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = addr - 0x8000;
      if( this.irmEnabled ) {
	byte[] a = null;
	if( addr < 0xA800 ) {
	  if( this.screen1Enabled ) {
	    a = this.ramColorEnabled ? this.ramColor1 : this.ramPixel1;
	  } else {
	    a = this.ramColorEnabled ? this.ramColor0 : this.ramPixel0;
	  }
	} else {
	  a = this.ramPixel0;
	}
	if( a != null ) {
	  if( idx < a.length ) {
	    rv = (int) a[ idx ] & 0xFF;
	  }
	}
      } else if( this.ram8Enabled ) {
	if( this.ram81Enabled ) {
	  if( idx < this.ram81.length ) {
	    rv = (int) this.ram81[ idx ] & 0xFF;
	  }
	} else {
	  if( idx < this.ram80.length ) {
	    rv = (int) this.ram80[ idx ] & 0xFF;
	  }
	}
      }
    } else if( (addr >= 0xC000) && (addr < 0xE000) ) {
      int idx = addr - 0xC000;
      if( this.caosC000Enabled && (this.caosC000 != null) ) {
	if( idx < this.caosC000.length ) {
	  rv = (int) this.caosC000[ idx ] & 0xFF;
	}
      } else if( this.basicC000Enabled && (this.basicC000 != null) ) {
	if( idx < this.basicC000.length ) {
	  rv = (int) basicC000[ idx ] & 0xFF;
	}
      }
    } else if( addr >= 0xE000 ) {
      if( this.caosE000Enabled ) {
	if( (addr < 0xF000) && (this.caosE000 != null) ) {
	  int idx = addr - 0xE000;
	  if( idx < caosE000.length ) {
	    rv = (int) caosE000[ idx ] & 0xFF;
	  }
	}
	else if( this.caosF000 != null ) {
	  int idx = addr - 0xF000;
	  if( idx < caosF000.length ) {
	    rv = (int) caosF000[ idx ] & 0xFF;
	  }
	}
      }
    }
    return rv;
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return resetLevel == EmuThread.ResetLevel.WARM_RESET ? 0xE000 : 0xF000;
  }


  public int getScreenBaseHeight()
  {
    return 256;
  }


  public int getScreenBaseWidth()
  {
    return 320;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getSystemName()
  {
    return this.sysName;
  }


  public boolean hasKCBasicInROM()
  {
    return (this.kcTypeNum > 2);
  }


  public boolean keyPressed( KeyEvent e )
  {
    boolean rv = false;
    if( this.emuThread.getZ80CPU().getIFF1()
	&& this.pio.canFireInterruptPortB() )
    {
      char ch = 0;
      switch( e.getKeyCode() ) {
	case KeyEvent.VK_LEFT:
	case KeyEvent.VK_BACK_SPACE:
	  ch = 7;
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

	case KeyEvent.VK_HOME:
	  ch = 0x10;
	  break;

	case KeyEvent.VK_END:
	  ch = 0x18;
	  break;

	case KeyEvent.VK_INSERT:
	  ch = 0x1A;
	  break;

	case KeyEvent.VK_ESCAPE:
	  ch = 0x1B;
	  break;

	case KeyEvent.VK_DELETE:
	  ch = 0x1F;
	  break;

	case KeyEvent.VK_SPACE:
	  ch = 0x20;
	  break;

	case KeyEvent.VK_F1:
	  ch = (char) (e.isShiftDown() ? 0xF7 : 0xF1);
	  break;

	case KeyEvent.VK_F2:
	  ch = (char) (e.isShiftDown() ? 0xF8 : 0xF2);
	  break;

	case KeyEvent.VK_F3:
	  ch = (char) (e.isShiftDown() ? 0xF9 : 0xF3);
	  break;

	case KeyEvent.VK_F4:
	  ch = (char) (e.isShiftDown() ? 0xFA : 0xF4);
	  break;

	case KeyEvent.VK_F5:
	  ch = (char) (e.isShiftDown() ? 0xFB : 0xF5);
	  break;

	case KeyEvent.VK_F6:
	  ch = (char) (e.isShiftDown() ? 0xFC : 0xFC);
	  break;
      }
      if( ch > 0 ) {
	keyTyped( ch );
	rv = true;
      }
    }
    return rv;
  }


  public void keyReleased( int keyCode )
  {
    this.keyNumPressed = -1;
  }


  public void keyTyped( char ch )
  {
    if( (ch > 0) && (ch < char2KeyNum.length) ) {
      int keyNum = char2KeyNum[ ch ];
      if( keyNum >= 0 )
	this.keyNumPressed = (keyNum & 0xFE) | (~keyNum & 0x01);
    }
  }


  public void openBasicProgram()
  {
    int begAddr = 0x0401;
    if( this.kcTypeNum == 2 ) {
      begAddr = askKCBasicBegAddr();
    }
    if( begAddr >= 0 )
      SourceUtil.openKCBasicProgram( this.screenFrm, begAddr );
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x88:
	rv = this.pio.readPortA();
	break;

      case 0x89:
	rv = this.pio.readPortB();
	break;

      case 0x8A:
	rv = this.pio.readControlA();
	break;

      case 0x8B:
	rv = this.pio.readControlB();
	break;

      case 0x8C:
      case 0x8D:
      case 0x8E:
      case 0x8F:
	rv = this.ctc.read( port & 0x03 );
	break;
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv      = true;
    String  sysText = EmuUtil.getProperty( props, "jkcemu.system" );
    if( (sysText.startsWith( "KC85/2" ) || sysText.startsWith( "HC900" ))
	&& (this.kcTypeNum == 2) )
    {
      rv = false;
    }
    else if( sysText.startsWith( "KC85/3" ) && (this.kcTypeNum == 3) ) {
      rv = false;
    }
    else if( sysText.startsWith( "KC85/4" ) && (this.kcTypeNum == 4) ) {
      rv = false;
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      Arrays.fill( this.ram80, (byte) 0 );
      Arrays.fill( this.ram81, (byte) 0 );
      Arrays.fill( this.ramColor0, (byte) 0 );
      Arrays.fill( this.ramColor1, (byte) 0 );
      Arrays.fill( this.ramPixel0, (byte) 0 );
      Arrays.fill( this.ramPixel1, (byte) 0 );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.basicC000Enabled = false;
      this.caosC000Enabled  = false;
      this.caosE000Enabled  = true;
      this.hiColorRes       = false;
      this.irmEnabled       = true;
      this.ram0Enabled      = true;
      this.ram0Writeable    = true;
      this.ram4Enabled      = (this.kcTypeNum > 3);
      this.ram4Writeable    = this.ram4Enabled;
      this.ram8Enabled      = false;
      this.ram81Enabled     = false;
      this.ram8Writeable    = false;
      this.ramColorEnabled  = false;
      this.screen1Enabled   = false;
      this.screen1Visible   = false;
      this.ctc.reset( true );
      this.pio.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
    }
    blinkEnabled           = false;
    blinkState             = false;
    this.audioOutPhaseL    = false;
    this.audioOutPhaseL    = false;
    this.audioInPhase      = this.emuThread.readAudioPhase();
    this.audioInTStates    = 0;
    this.ctcInCounter      = 0;
    this.lineTStateCounter = 0;
    this.keyNumPressed     = -1;
    this.keyNumProcessing  = -1;
    this.keyShiftBitCnt    = 0;
    this.keyShiftValue     = 0;
    this.keyTStates        = 0;
    updAudioOut();
  }


  public void saveBasicProgram()
  {
    int begAddr = 0x0401;
    if( this.kcTypeNum == 2 ) {
      begAddr = askKCBasicBegAddr();
    }
    if( begAddr >= 0 )
      SourceUtil.saveKCBasicProgram( this.screenFrm, begAddr );
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( (addr >= 0) && (addr < 0x4000) ) {
      if( this.ram0Enabled && this.ram0Writeable ) {
	this.emuThread.setRAMByte( addr, value );
	rv = true;
      }
    } else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( this.ram4Enabled && this.ram4Writeable ) {
	this.emuThread.setRAMByte( addr, value );
	rv = true;
      }
    } else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = addr - 0x8000;
      if( this.irmEnabled ) {
	byte[] a = null;
	if( addr < 0xA800 ) {
	  if( this.screen1Enabled ) {
	    a = this.ramColorEnabled ? this.ramColor1 : this.ramPixel1;
	  } else {
	    a = this.ramColorEnabled ? this.ramColor0 : this.ramPixel0;
	  }
	} else {
	  a = this.ramPixel0;
	}
	if( a != null ) {
	  if( idx < a.length ) {
	    a[ idx ] = (byte) value;
	    this.screenFrm.setScreenDirty( true );
	  }
	}
      } else if( this.ram8Enabled && this.ram8Writeable ) {
	if( this.ram81Enabled ) {
	  if( idx < this.ram81.length ) {
	    this.ram81[ idx ] = (byte) value;
	    rv = true;
	  }
	} else {
	  if( idx < this.ram80.length ) {
	    this.ram80[ idx ] = (byte) value;
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    int m = 0;
    switch( port & 0xFF ) {
      case 0x84:
      case 0x85:
	if( this.kcTypeNum > 3 ) {
	  this.screen1Visible  = ((value & 0x01) == 0);
	  this.ramColorEnabled = ((value & 0x02) != 0);
	  this.screen1Enabled  = ((value & 0x04) == 0);
	  this.hiColorRes      = ((value & 0x08) == 0);
	  this.ram81Enabled    = ((value & 0x10) != 0);
	}
	break;

      case 0x86:
      case 0x87:
	if( this.kcTypeNum > 3 ) {
	  this.ram4Enabled     = ((value & 0x01) != 0);
	  this.ram4Writeable   = ((value & 0x02) != 0);
	  this.caosC000Enabled = ((value & 0x80) != 0);
	}
	break;

      case 0x88:
	this.pio.writePortA( value );
	m = this.pio.fetchOutValuePortA( false );
	this.caosE000Enabled  = ((m & 0x01) != 0);
	this.ram0Enabled      = ((m & 0x02) != 0);
	this.irmEnabled       = ((m & 0x04) != 0);
	this.ram0Writeable    = ((m & 0x08) != 0);
	this.basicC000Enabled = ((m & 0x80) != 0);
	break;

      case 0x89:
	this.pio.writePortB( value );
	m = this.pio.fetchOutValuePortB( false );
	if( this.kcTypeNum > 3 ) {
	  if( (m & 0x01) == 0 ) {
	    this.audioOutPhaseL = false;
	    this.audioOutPhaseR = false;
	    updAudioOut();
	  }
	  this.ram8Enabled   = ((m & 0x20) != 0);
	  this.ram8Writeable = ((m & 0x40) != 0);
	}
	this.blinkEnabled = ((m & 0x80) != 0);
	break;

      case 0x8A:
	this.pio.writeControlA( value );
	break;

      case 0x8B:
	this.pio.writeControlB( value );
	break;

      case 0x8C:
      case 0x8D:
      case 0x8E:
      case 0x8F:
	this.ctc.write( port & 0x03, value );
	break;
    }
  }


	/* --- private Methoden --- */

  private int getColorIndex( int colorByte, boolean foreground )
  {
    if( this.blinkEnabled && this.blinkState && ((colorByte & 0x80) != 0) ) {
      foreground = false;
    }
    return foreground ? ((colorByte >> 3) & 0x0F) : ((colorByte & 0x07) + 16);
  }


  private void updAudioOut()
  {
    if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
      int value = 0;
      if( this.audioOutPhaseL == this.audioOutPhaseR ) {
	value = (this.audioOutPhaseL ? 127 : -127);
	int m = this.pio.fetchOutValuePortB( false );
	if( (m & 0x10) != 0 ) {
	  value = (value * 30) / 100;		// 1.0 / (2.35 + 1.0)
	}
	if( (m & 0x08) != 0 ) {
	  value = (value * 46) / 100;		// 2.0 / (2.35 + 2.0)
	}
	if( (m & 0x04) != 0 ) {
	  value = (value * 62) / 100;		// 3.9 / (2.35 + 3.9)
	}
	if( (m & 0x02) != 0 ) {
	  value = (value * 78) / 100;		// 8.2 / (2.35 + 8.2)
	}
	if( (this.kcTypeNum < 4) && ((m & 0x01) != 0) ) {
	  value = (value * 87) / 100;		// 16.0 / (2.35 + 16.0)
	}
      }
      this.emuThread.writeAudioValue( (byte) value );
    } else {
      this.emuThread.writeAudioPhase( this.audioOutPhaseL );
    }
  }
}

