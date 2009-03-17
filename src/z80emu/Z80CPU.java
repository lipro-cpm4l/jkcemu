/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z80-Emulator
 *
 * Emulation der Z80 CPU
 *
 * Die Register werden in int-Variablen gespeichert.
 * Die nicht genutzten oberen Bits muessen immer 0 sein.
 */

package z80emu;

import java.lang.*;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Z80CPU
{
  public static enum Action {
			RUN,
			PAUSE,
			DEBUG_RUN,
			DEBUG_STOP,
			DEBUG_STEP_OVER,
			DEBUG_STEP_INTO,
			DEBUG_STEP_UP };

  // Nach wieviel Taktzyklen die Zaehlung bei Null beginnen soll
  private static final int tStatesWrap = 1000000000;


  // Masken fuer die einzelnen Bits
  private static final int BIT0 = 0x01;
  private static final int BIT1 = 0x02;
  private static final int BIT2 = 0x04;
  private static final int BIT3 = 0x08;
  private static final int BIT4 = 0x10;
  private static final int BIT5 = 0x20;
  private static final int BIT6 = 0x40;
  private static final int BIT7 = 0x80;


  // private Attribute
  private volatile Z80AddressListener      addrListener;
  private volatile Z80TStatesListener      tStatesListener;
  private Z80Memory                        memory;
  private Z80IOSystem                      ioSys;
  private Thread                           thread;
  private volatile Z80Breakpoint[]         debugBreakpoints;
  private volatile PrintWriter             debugTracer;
  private volatile Z80InterruptSource[]    interruptSources;
  private Collection<Z80HaltStateListener> haltStateListeners;
  private Collection<Z80MaxSpeedListener>  maxSpeedListeners;
  private Collection<Z80StatusListener>    statusListeners;
  private boolean[]                        parity;
  private volatile int[]                   codeBuf;
  private volatile int                     codeBufPos;
  private volatile int                     maxSpeedKHz;
  private volatile long                    speedUnlimitedTill;
  private volatile long                    speedMillisBeg;
  private volatile long                    speedMillisEnd;
  private volatile int                     speedTStates;
  private volatile int                     instTStates;
  private volatile int                     debugCallLevel;
  private volatile Action                  action;
  private volatile int                     interruptMode;
  private int                              interruptReg;
  private boolean                          haltState;
  private Integer                          haltPC;
  private int                              instBegPC;
  private int                              regPC;
  private int                              regSP;
  private int                              regA;
  private int                              regB;
  private int                              regC;
  private int                              regD;
  private int                              regE;
  private int                              regH;
  private int                              regL;
  private int                              regA2;
  private int                              regF2;
  private int                              regB2;
  private int                              regC2;
  private int                              regD2;
  private int                              regE2;
  private int                              regH2;
  private int                              regL2;
  private int                              regIX;
  private int                              regIY;
  private int                              regR_bits0to6;
  private boolean                          regR_bit7;
  private boolean                          iff1;
  private boolean                          iff2;
  private volatile boolean                 nmiFired;
  private boolean                          flagSign;
  private boolean                          flagZero;
  private boolean                          flag5;
  private boolean                          flagHalf;
  private boolean                          flag3;
  private boolean                          flagPV;
  private boolean                          flagN;
  private boolean                          flagCarry;
  private boolean                          lastInstWasEIorDI;
  private volatile boolean                 active;
  private volatile boolean                 pause;
  private volatile boolean                 debugEnabled;
  private AtomicInteger                    waitStates;
  private String                           waitMonitor;


  public Z80CPU( Z80Memory memory, Z80IOSystem ioSys )
  {
    this.memory             = memory;
    this.ioSys              = ioSys;
    this.thread             = null;
    this.addrListener       = null;
    this.tStatesListener    = null;
    this.interruptSources   = null;
    this.haltStateListeners = new ArrayList<Z80HaltStateListener>();
    this.maxSpeedListeners  = new ArrayList<Z80MaxSpeedListener>();
    this.statusListeners    = new ArrayList<Z80StatusListener>();
    this.debugBreakpoints   = null;
    this.debugTracer        = null;
    this.haltPC             = null;
    this.maxSpeedKHz        = -1;
    this.active             = false;
    this.haltState          = false;
    this.debugEnabled       = false;
    this.waitStates         = new AtomicInteger( 0 );
    this.waitMonitor        = "wait monitor";

    // Paritaeten fuer den Byte-Bereich berechnen
    this.parity = new boolean[ 0x100 ];
    for( int i = 0; i < this.parity.length; i++ ) {
      boolean v = true;
      int     m = i;
      for( int k = 0; k < 8; k++ ) {
	if( (m & BIT0) != 0 ) {
	  v = !v;
	}
	m >>= 1;
      }
      this.parity[ i ] = v;
    }
    resetCPU( true );
  }


  public void addWaitStates( int tStates )
  {
    this.waitStates.addAndGet( tStates );
  }


  /*
   * Die CPU-Emulation zaehlt die orginalen Taktzyklen mit.
   * Damit der Zaehler nicht ueberlaeuft,
   * wird dieser an einer bestimmten Stelle zurueckgesetzt.
   * Diese Methode berechnet die Differenz zwischen zwei dieser
   * Taktzyklenwerte, die mit der Methode "getProcessedTStates()"
   * ermittelt werden.
   * Dabei wird auch dann die richtige Differenz zurueckgegeben,
   * wenn zwischen den beiden Aufrufen von "getProcessedTStates()"
   * der Zaehler einmal zurueckgesetzt wurde.
   */
  public static int calcTStatesDiff( int tStates1, int tStates2 )
  {
    while( tStates2 < tStates1 ) {
      tStates2 += tStatesWrap;
    }
    return tStates2 - tStates1;
  }


  /*
   * Es kann nur ein AdressListener gesetzt werden.
   */
  public void addAddressListener( Z80AddressListener addrListener )
  {
    if( this.addrListener != null ) {
      throw new IllegalStateException( "Zu viele Z80AddressListeners" );
    }
    this.addrListener = addrListener;
  }


  public void removeAddressListener( Z80AddressListener addrListener )
  {
    if( (this.addrListener != null)
	&& (this.addrListener == addrListener) )
    {
      this.addrListener = null;
    }
  }


  /*
   * Der erste Argument bzw. der erste Eintrag in der Argumentliste
   * hat die hochste Interrupt-Prioritaet.
   */
  public void setInterruptSources( Z80InterruptSource... iSources )
  {
    this.interruptSources = iSources;
  }


  public void addHaltStateListener( Z80HaltStateListener listener )
  {
    synchronized( this.haltStateListeners ) {
      this.haltStateListeners.add( listener );
    }
  }


  public void removeHaltStateListener( Z80HaltStateListener listener )
  {
    synchronized( this.haltStateListeners ) {
      this.maxSpeedListeners.remove( listener );
    }
  }


  public void addMaxSpeedListener( Z80MaxSpeedListener listener )
  {
    synchronized( this.maxSpeedListeners ) {
      this.maxSpeedListeners.add( listener );
    }
  }


  public void removeMaxSpeedListener( Z80MaxSpeedListener listener )
  {
    synchronized( this.maxSpeedListeners ) {
      this.maxSpeedListeners.remove( listener );
    }
  }


  /*
   * Es kann nur ein TStatesListener gesetzt werden.
   */
  public void addTStatesListener( Z80TStatesListener listener )
  {
    if( this.tStatesListener != null ) {
      throw new IllegalStateException( "Zu viele Z80TStatesListeners" );
    }
    this.tStatesListener = listener;
  }


  public void removeTStatesListener( Z80TStatesListener listener )
  {
    if( (this.tStatesListener != null)
	&& (this.tStatesListener == listener) )
    {
      this.tStatesListener = null;
    }
  }


  public boolean isDebugEnabled()
  {
    return this.debugEnabled;
  }


  public boolean isActive()
  {
    return this.active;
  }



  public boolean isPause()
  {
    return this.pause;
  }


  public void setDebugEnabled( boolean state )
  {
    this.debugEnabled = state;
    if( this.pause ) {
      fireAction( this.debugEnabled ? Action.DEBUG_STOP : Action.PAUSE );
      updStatusListeners();
    } else {
      fireAction( this.debugEnabled ? Action.DEBUG_RUN : Action.RUN );
    }
  }


  public void setSpeedCalculationEnabled( boolean state )
  {
    if( state ) {

      /*
       * wenn zwischenzeitlich die Geschwindigkeit nicht
       * zurueckgesetzt wurde, dann korrigieren
       */
      if( this.speedMillisEnd > 0L ) {
	this.speedMillisBeg += (System.currentTimeMillis()
						- this.speedMillisEnd);
      }
      this.speedMillisEnd = -1L;
    } else {
      this.speedMillisEnd = System.currentTimeMillis();
    }
  }


  /*
   * Die beiden Methoden implementieren den Mechanismus des Wartens.
   * Als Monitor-Objekt wird ein (beliebiges) Objekt verwendet,
   * auf das im Swing-Thread nicht zugegriffen werden muss.
   * Die waitFor-Methode haelt den atuellen Thread an und
   * darf nur im Emulations-Threads aufgerufen werden.
   */
  public void waitFor()
  {
    synchronized( this.waitMonitor ) {
      setSpeedCalculationEnabled( false );
      try {
	this.waitMonitor.wait();
      }
      catch( InterruptedException ex ) {}
      setSpeedCalculationEnabled( true );
    }
  }


  public void wakeUp()
  {
    synchronized( this.waitMonitor ) {
      try {
	this.waitMonitor.notifyAll();
      }
      catch( IllegalMonitorStateException ex ) {}
    }
  }


  public void resetCPU( boolean powerOn )
  {
    Z80InterruptSource[] iSources = this.interruptSources;
    if( iSources != null ) {
      for( int i = 0; i < iSources.length; i++ )
	iSources[ i ].reset();
    }
    this.codeBuf           = null;
    this.codeBufPos        = 0;
    this.nmiFired          = false;
    this.iff1              = false;
    this.iff2              = false;
    this.interruptMode     = 0;
    this.interruptReg      = 0;
    this.instTStates       = 0;
    this.regR_bits0to6     = 0;
    this.regR_bit7         = false;
    this.lastInstWasEIorDI = false;
    this.debugCallLevel    = 0;
    this.pause             = false;
    this.action            = this.debugEnabled ? Action.DEBUG_RUN : Action.RUN;
    this.haltPC            = null;
    this.instBegPC         = 0;
    this.regPC             = 0;
    setHaltState( false );
    if( powerOn ) {
      setRegF( 0xFF );
      this.regA  = 0xFF;
      this.regB  = 0xFF;
      this.regC  = 0xFF;
      this.regD  = 0xFF;
      this.regE  = 0xFF;
      this.regH  = 0xFF;
      this.regL  = 0xFF;
      this.regA2 = 0xFF;
      this.regF2 = 0xFF;
      this.regB2 = 0xFF;
      this.regC2 = 0xFF;
      this.regD2 = 0xFF;
      this.regE2 = 0xFF;
      this.regH2 = 0xFF;
      this.regL2 = 0xFF;
      this.regIX = 0xFFFF;
      this.regIY = 0xFFFF;
      this.regSP = 0xFFFF;
    }
    resetSpeed();
  }


  public void resetSpeed()
  {
    this.speedUnlimitedTill = 0L;
    this.speedTStates       = 0;
    this.speedMillisBeg     = System.currentTimeMillis();
    this.speedMillisEnd     = -1L;
  }


  public int getProcessedTStates()
  {
    return this.speedTStates;
  }


  public double getCurrentSpeedKHz()
  {
    /*
     * Werte moeglichst schnell auslesen, da die Methode
     * in einem anderen Thread aufgerufen werden kann
     */
    int  tStates   = this.speedTStates;
    long millisBeg = this.speedMillisBeg;
    long millisEnd = this.speedMillisEnd;
    if( millisEnd <= 0 ) {
      millisEnd = System.currentTimeMillis();
    }
    long usedTimeMillis	= millisEnd - millisBeg;

    return ((usedTimeMillis > 0L) && (tStates > 0)) ?
	((double) tStates / (double) usedTimeMillis) : -1.0;
  }


  public int getMaxSpeedKHz()
  {
    return this.maxSpeedKHz;
  }


  /*
   * Geschwindigkeit des Emulators begrenzen
   *
   * 0: Geschwindigkeit nicht begrenzen
   */
  public void setMaxSpeedKHz( int maxSpeedKhz )
  {
    int oldMaxSpeed  = this.maxSpeedKHz;
    this.maxSpeedKHz = maxSpeedKhz;
    resetSpeed();
    if( this.maxSpeedKHz != oldMaxSpeed ) {
      synchronized( this.maxSpeedListeners ) {
	for( Z80MaxSpeedListener listener : this.maxSpeedListeners )
	  listener.z80MaxSpeedChanged();
      }
    }
  }


  /*
   * Diese Methode schaltet die Geschwindigkeitsbremse
   * fuer die Dauer der uebergebenen Anzahl an Z80-Taktzyklen aus.
   */
  public void setSpeedUnlimitedFor( int unlimitedSpeedTStates )
  {
    this.speedUnlimitedTill = this.speedTStates + (long) unlimitedSpeedTStates;
  }


	/* --- Empfang externer Signale aus einen anderen Thread --- */

  public void fireExit()
  {
    this.active = false;
    wakeUp();
    updStatusListeners();
  }


  /*
   * Ausloesen eines nicht maskierbaren Interrupts
   */
  public void fireNMI()
  {
    this.nmiFired = true;
  }


	/* --- Zustandssteuerung --- */

  public void addStatusListener( Z80StatusListener listener )
  {
    synchronized( this.statusListeners ) {
      if( !this.statusListeners.contains( listener ) )
	this.statusListeners.add( listener );
    }
  }


  public void removeStatusListener( Z80StatusListener listener )
  {
    synchronized( this.statusListeners ) {
      this.statusListeners.remove( listener );
    }
  }


  public void fireAction( Action action )
  {
    this.action         = action;
    this.debugCallLevel = 0;
    this.debugEnabled   = ((this.action == Action.DEBUG_RUN)
				|| (this.action == Action.DEBUG_STOP)
				|| (this.action == Action.DEBUG_STEP_OVER)
				|| (this.action == Action.DEBUG_STEP_INTO)
				|| (this.action == Action.DEBUG_STEP_UP));

    if( (this.action != Action.PAUSE) && (this.action != Action.DEBUG_STOP) ) {
      wakeUp();
      updStatusListeners();
    }
  }


  public void firePause( boolean state )
  {
    if( this.debugEnabled ) {
      fireAction( state ? Action.DEBUG_STOP : Action.DEBUG_RUN );
    } else {
      fireAction( state ? Action.PAUSE : Action.RUN );
    }
  }


  public void setBreakpoints( Z80Breakpoint[] breakpoints )
  {
    this.debugBreakpoints = breakpoints;
  }


  public void setDebugTracer( PrintWriter debugTracer )
  {
    this.debugTracer = debugTracer;
  }


	/* --- Operationen mit dem Hauptspeicher --- */

  public int doPop()
  {
    int rv     = readMemByte( this.regSP );
    this.regSP = (this.regSP + 1) & 0xFFFF;
    rv |= (readMemByte( this.regSP ) << 8);
    this.regSP = (this.regSP + 1) & 0xFFFF;
    --this.debugCallLevel;
    return rv;
  }


  public void doPush( int value )
  {
    this.regSP = (this.regSP - 1) & 0xFFFF;
    writeMemByte( this.regSP, value >> 8 );
    this.regSP = (this.regSP - 1) & 0xFFFF;
    writeMemByte( this.regSP, value );
    this.debugCallLevel++;
  }


  public String getMemString( int addr, int len )
  {
    StringBuilder buf = new StringBuilder( len );
    for( int i = 1; i < len; i++ ) {
      int b = this.memory.getMemByte( i ) & 0xFF;
      if( b > 0 )
	buf.append( (char) b );
    }
    return buf.toString();
  }


	/* --- Zugriff auf Flags --- */

  public boolean getFlagSign()
  {
    return this.flagSign;
  }


  public boolean getFlagZero()
  {
    return this.flagZero;
  }


  public boolean getFlagHalf()
  {
    return this.flagHalf;
  }


  public boolean getFlagPV()
  {
    return this.flagPV;
  }


  public boolean getFlagN()
  {
    return this.flagN;
  }


  public boolean getFlagCarry()
  {
    return this.flagCarry;
  }


  public boolean getIFF1()
  {
    return this.iff1;
  }


  public boolean getIFF2()
  {
    return this.iff2;
  }


  public void setFlagSign( boolean state )
  {
    this.flagSign = state;
  }


  public void setFlagZero( boolean state )
  {
    this.flagZero = state;
  }


  public void setFlagHalf( boolean state )
  {
    this.flagHalf = state;
  }


  public void setFlagPV( boolean state )
  {
    this.flagPV = state;
  }


  public void setFlagN( boolean state )
  {
    this.flagN = state;
  }


  public void setFlagCarry( boolean state )
  {
    this.flagCarry = state;
  }


  public void setIFF1( boolean state )
  {
    this.iff1 = state;
  }


  public void setIFF2( boolean state )
  {
    this.iff2 = state;
  }


	/* --- Zugriff auf 8-Bit-Register --- */

  public int getRegA()
  {
    return this.regA;
  }


  public int getRegB()
  {
    return this.regB;
  }


  public int getRegC()
  {
    return this.regC;
  }


  public int getRegD()
  {
    return this.regD;
  }


  public int getRegE()
  {
    return this.regE;
  }


  public int getRegH()
  {
    return this.regH;
  }


  public int getRegL()
  {
    return this.regL;
  }


  public int getRegF()
  {
    return (this.flagSign ? BIT7 : 0)
		| (this.flagZero  ? BIT6 : 0)
		| (this.flag5     ? BIT5 : 0)
		| (this.flagHalf  ? BIT4 : 0)
		| (this.flag3     ? BIT3 : 0)
		| (this.flagPV    ? BIT2 : 0)
		| (this.flagN     ? BIT1 : 0)
		| (this.flagCarry ? BIT0 : 0);
  }


  public int getRegI()
  {
    return this.interruptReg;
  }


  public int getRegIXH()
  {
    return (this.regIX >> 8) & 0xFF;
  }


  public int getRegIXL()
  {
    return this.regIX & 0xFF;
  }


  public int getRegIYH()
  {
    return (this.regIY >> 8) & 0xFF;
  }


  public int getRegIYL()
  {
    return this.regIY & 0xFF;
  }


  public void setRegA( int value )
  {
    this.regA = value & 0xFF;
  }


  public void setRegF( int value )
  {
    this.flagSign  = (value & BIT7) != 0;
    this.flagZero  = (value & BIT6) != 0;
    this.flag5     = (value & BIT5) != 0;
    this.flagHalf  = (value & BIT4) != 0;
    this.flag3     = (value & BIT3) != 0;
    this.flagPV    = (value & BIT2) != 0;
    this.flagN     = (value & BIT1) != 0;
    this.flagCarry = (value & BIT0) != 0;
  }


  public void setRegI( int value )
  {
    this.interruptReg = value & 0xFF;
  }


  public void setRegIXH( int value )
  {
    this.regIX = ((value << 8) & 0xFF00) | (this.regIX & 0xFF);
  }


  public void setRegIXL( int value )
  {
    this.regIX = (this.regIX & 0xFF00) | (value & 0xFF);
  }


  public void setRegIYH( int value )
  {
    this.regIY = ((value << 8) & 0xFF00) | (this.regIY & 0xFF);
  }


  public void setRegIYL( int value )
  {
    this.regIY = (this.regIY & 0xFF00) | (value & 0xFF);
  }


	/* --- Zugriff auf 16-Bit-Register --- */

  public int getRegAF()
  {
    return ((this.regA << 8) | getRegF()) & 0xFFFF;
  }


  public int getRegBC()
  {
    return ((this.regB << 8) | this.regC) & 0xFFFF;
  }


  public int getRegDE()
  {
    return ((this.regD << 8) | this.regE) & 0xFFFF;
  }


  public int getRegHL()
  {
    return ((this.regH << 8) | this.regL) & 0xFFFF;
  }


  public int getRegIX()
  {
    return this.regIX;
  }


  public int getRegIY()
  {
    return this.regIY;
  }


  public int getRegSP()
  {
    return this.regSP;
  }


  public int getRegPC()
  {
    return this.regPC;
  }


  public int getRegAF2()
  {
    return ((this.regA2 << 8) | this.regF2) & 0xFFFF;
  }


  public int getRegBC2()
  {
    return ((this.regB2 << 8) | this.regC2) & 0xFFFF;
  }


  public int getRegDE2()
  {
    return ((this.regD2 << 8) | this.regE2) & 0xFFFF;
  }


  public int getRegHL2()
  {
    return ((this.regH2 << 8) | this.regL2) & 0xFFFF;
  }


  public void setRegAF( int value )
  {
    this.regA = (value >> 8) & 0xFF;
    setRegF( value & 0xFF );
  }


  public void setRegAF2( int value )
  {
    this.regA2 = (value >> 8) & 0xFF;
    this.regF2 = value & 0xFF;
  }


  public void setRegBC( int value )
  {
    this.regB = (value >> 8) & 0xFF;
    this.regC = value & 0xFF;
  }


  public void setRegBC2( int value )
  {
    this.regB2 = (value >> 8) & 0xFF;
    this.regC2 = value & 0xFF;
  }


  public void setRegDE( int value )
  {
    this.regD = (value >> 8) & 0xFF;
    this.regE = value & 0xFF;
  }


  public void setRegDE2( int value )
  {
    this.regD2 = (value >> 8) & 0xFF;
    this.regE2 = value & 0xFF;
  }


  public void setRegHL( int value )
  {
    this.regH = (value >> 8) & 0xFF;
    this.regL = value & 0xFF;
  }


  public void setRegHL2( int value )
  {
    this.regH2 = (value >> 8) & 0xFF;
    this.regL2 = value & 0xFF;
  }


  public void setRegSP( int value )
  {
    this.regSP = value & 0xFFFF;
  }


  public void setRegPC( int value )
  {
    this.regPC = value & 0xFFFF;
  }


  public void setRegIX( int value )
  {
    this.regIX = value & 0xFFFF;
  }


  public void setRegIY( int value )
  {
    this.regIY = value & 0xFFFF;
  }


	/* --- Emulation ausfuehren --- */

  public void run() throws Z80ExternalException
  {
    int speedBrakeTStates = 0;
    int opCode            = 0;

    resetSpeed();

    this.active = true;
    this.thread = Thread.currentThread();
    updStatusListeners();

    try {
      while( this.active ) {

	/*
	 * Geschwindigkeitsverwaltung
	 */
	if( this.speedTStates >= tStatesWrap ) {

	  // Zaehler fuer die Taktzyklen darf nicht ueberlaufen
	  this.speedUnlimitedTill = 0L;
	  this.speedMillisBeg     = System.currentTimeMillis();
	  this.speedMillisEnd     = -1L;
	  this.speedTStates       -= tStatesWrap;

	} else {

	  /*
	   * Geschwindigkeit begrenzen
	   * Die Geschwindigkeitsbremse nicht nach jedem Befehl aktivieren,
	   * da sonst zuviel Rechenzeit fuer die Bremse selbst
	   * benoetigt wird.
	   */
	  if( this.speedUnlimitedTill < this.speedTStates ) {
	    if( speedBrakeTStates < 500 ) {
	      speedBrakeTStates++;
	    } else {
	      speedBrakeTStates = 0;

	      if( this.maxSpeedKHz > 0 ) {
		long timeToUse = (long) ((float) this.speedTStates
						/ (float) this.maxSpeedKHz );
		long usedTime  = (this.speedMillisEnd > 0L) ?
			  (this.speedMillisEnd - this.speedMillisBeg) :
			  (System.currentTimeMillis() - this.speedMillisBeg);

		if( timeToUse > usedTime ) {
		  try {
		    Thread.sleep( Math.min( timeToUse - usedTime, 50 ) );
		  }
		  catch( InterruptedException ex ) {}
		}
	      }
	    }
	  }
	}
	this.instTStates = 0;


	/*
	 * Interrupt-Verwaltung
	 *
	 * Unmittelbar nach einem EI- und DI-Befehl darf kein
	 * maskierbarer Interrupt auftreten.
	 */
	boolean iAccepted = false;
	if( this.nmiFired ) {
	  this.nmiFired = false;
	  this.iff2     = this.iff1;
	  this.iff1     = false;
	  incRegR();
	  doPush( this.regPC );
	  this.haltPC = null;
	  this.regPC  = 0x0066;
	  this.instTStates += 11;
	  iAccepted = true;
	} else {
	  if( this.lastInstWasEIorDI ) {
	    this.lastInstWasEIorDI = false;
	  } else {
	    if( this.iff1 ) {
	      Z80InterruptSource[] iSources = this.interruptSources;
	      if( iSources != null ) {
		for( int i = 0; i < iSources.length; i++ ) {
		  Z80InterruptSource iSource = iSources[ i ];
		  if( iSource.isInterruptPending() ) {
		    break;
		  }
		  if( iSource.isInterruptRequested() ) {
		    this.haltPC = null;
		    this.iff1   = false;
		    this.iff2   = false;
		    int iVector = iSource.interruptAccepted();
		    incRegR();

		    switch( this.interruptMode ) {
		      case 0:
			this.instBegPC = this.regPC;
			execInst( -1, iVector );
			this.instTStates += 2;	// insgesamt 13 bei RST-Befehl
			break;

		      case 1:
			doPush( this.regPC );
			this.regPC = 0x0038;
			this.instTStates += 13;
			break;

		      case 2:
			doPush( this.regPC );
			this.regPC = readMemWord(
				(this.interruptReg << 8) | iVector );
			this.instTStates += 19;
			break;
		    }
		    iAccepted = false;
		  }
		}
	      }
	    }
	  }
	}
	if( !iAccepted && (this.haltPC != null) ) {
	  this.regPC  = this.haltPC.intValue();
	  this.haltPC = null;
	} else {
	  setHaltState( false );
	}


	/*
	 * Debugger- und Pausesteuerung
	 */
	if( (this.action == Action.PAUSE) || this.debugEnabled ) {
	  if( this.debugEnabled && (this.debugTracer != null) )
	    doDebugTrace();

	  /*
	   * Pruefen, ob der Debugger anhalten soll
	   * Wenn ja, dann die dafuer benoetigte Zeit abziehen
	   */
	  if( (this.action == Action.PAUSE)
	      || (this.action == Action.DEBUG_STOP)
	      || (this.action == Action.DEBUG_STEP_INTO)
	      || ((this.action == Action.DEBUG_STEP_OVER)
				&& (this.debugCallLevel <= 0))
	      || ((this.action == Action.DEBUG_STEP_UP)
				&& (this.debugCallLevel <= 0)
				&& debugHasInstRETXReached())
	      || debugHasBreakpointReached() )
	  {
	    this.pause = true;
	    updStatusListeners();
	    waitFor();
	    this.pause = false;
	    updStatusListeners();

	    if( !this.active )
	      break;
	  }
	}

	// BefehlsOpCode lesen und PC weitersetzen
	this.instBegPC = this.regPC;
	opCode         = readMemByte( this.regPC );
	this.regPC     = (this.regPC + 1) & 0xFFFF;
	execInst( -1, opCode );
	this.instTStates  += this.waitStates.getAndSet( 0 );
	this.speedTStates += this.instTStates;

	// verbrauchte Anzahl Taktzyklen melden
	Z80TStatesListener tStatesListener = this.tStatesListener;
	if( tStatesListener != null ) {
	  while( this.instTStates >= 4 ) {
	    tStatesListener.z80TStatesProcessed( this, 4 );
	    this.instTStates -= 4;
	  }
	  if( this.instTStates > 0 ) {
	    tStatesListener.z80TStatesProcessed( this, this.instTStates );
	  }
	}
      }
    }
    finally {
      this.active = false;
      updStatusListeners();
    }
  }


  /*
   * Diese Methode fuehrt einen CPU-Befehl aus.
   */
  public void execInstruction( int... opCode )
  {
    this.instBegPC = this.regPC;
    try {
      this.codeBuf    = opCode;
      this.codeBufPos = 0;
      while( this.codeBufPos < this.codeBuf.length )
	execInst( -1, this.codeBuf[ this.codeBufPos++ ] );
    }
    finally {
      this.codeBuf    = null;
      this.codeBufPos = -1;
    }
  }


	/* --- private Methoden --- */

  public void execInst( int preCode, int opCode )
  {
    incRegR();
    if( opCode < 0x80 ) {
      if( opCode < 0x40 ) {
	if( opCode < 0x20 ) {
	  if( opCode < 0x10 ) {
	    exec00to0F( preCode, opCode );
	  } else {
	    exec10to1F( preCode, opCode );
	  }
	} else {
	  if( opCode < 0x30 ) {
	    exec20to2F( preCode, opCode );
	  } else {
	    exec30to3F( preCode, opCode );
	  }
	}
      } else {
	exec40to7F( preCode, opCode );
      }
    } else {
      if( opCode < 0xC0 ) {
	exec80toBF( preCode, opCode );
      } else {
	if( opCode < 0xE0 ) {
	  if( opCode < 0xD0 ) {
	    execC0toCF( preCode, opCode );
	  } else {
	    execD0toDF( opCode );
	  }
	} else {
	  if( opCode < 0xF0 ) {
	    execE0toEF( preCode, opCode );
	  } else {
	    execF0toFF( preCode, opCode );
	  }
	}
      }
    }
  }


	/* --- private Methode zur Emulation der Codes 00-FF --- */

  /*
   * Bei Befehlen mit den Vorbytes 0xDD und 0xFD (IX- und IY-Register)
   * werden die Taktzyklen um 4 weniger hochgezaehlt,
   * da bereits bei den Vorbytes selbst die 4 Taktzyklen gezaehlt werden.
   */
  private void exec00to0F( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0x00:				// NOP
	this.instTStates += 4;
	break;
      case 0x01:				// LD BC,nn
	setRegBC( nextWord() );
	this.instTStates += 10;
	break;
      case 0x02:				// LD (BC),A
	writeMemByte( getRegBC(), this.regA );
	this.instTStates += 7;
	break;
      case 0x03:				// INC BC
	setRegBC( getRegBC() + 1 );
	this.instTStates += 6;
	break;
      case 0x04:				// INC B
	this.regB = doInstINC8( this.regB );
	this.instTStates += 4;
	break;
      case 0x05:				// DEC B
	this.regB = doInstDEC8( this.regB );
	this.instTStates += 4;
	break;
      case 0x06:				// LD B,n
	this.regB = nextByte();
	this.instTStates += 7;
	break;
      case 0x07:				// RLCA
	if( (this.regA & BIT7) != 0 ) {
	  this.regA = ((this.regA << 1) | BIT0) & 0xFF;
	  this.flagCarry = true;
	} else {
	  this.regA = (this.regA << 1) & 0xFF;
	  this.flagCarry = false;
	}
	this.flagHalf = false;
	this.flagN    = false;
	this.flag5    = ((this.regA & BIT5) != 0);
	this.flag3    = ((this.regA & BIT3) != 0);
	this.instTStates += 4;
	break;
      case 0x08:				// EX AF,AF'
	{
	  int b      = this.regA;
	  this.regA  = this.regA2;
	  this.regA2 = b;
	  b          = getRegF();
	  setRegF( this.regF2 );
	  this.regF2 = b;
	  this.instTStates += 4;
	}
	break;
      case 0x09:
	if( preCode == 0xDD ) {				// ADD IX,BC
	  this.regIX = doInstADD16( this.regIX, getRegBC() );
	} else if( preCode == 0xFD ) {			// ADD IY,BC
	  this.regIY = doInstADD16( this.regIY, getRegBC() );
	} else {					// ADD HL,BC
	  setRegHL( doInstADD16( getRegHL(), getRegBC() ) );
	}
	this.instTStates += 11;
	break;
      case 0x0A:				// LD A,(BC)
	this.regA = readMemByte( getRegBC() );
	this.instTStates += 11;
	break;
      case 0x0B:				// DEC BC
	setRegBC( getRegBC() - 1 );
	this.instTStates += 6;
	break;
      case 0x0C:				// INC C
	this.regC = doInstINC8( this.regC );
	this.instTStates += 4;
	break;
      case 0x0D:				// DEC C
	this.regC = doInstDEC8( this.regC );
	this.instTStates += 4;
	break;
      case 0x0E:				// LD C,n
	this.regC = nextByte();
	this.instTStates += 7;
	break;
      case 0x0F:				// RRCA
	if( (this.regA & BIT0) != 0 ) {
	  this.regA = (this.regA >> 1) | BIT7;
	  this.flagCarry = true;
	} else {
	  this.regA >>= 1;
	  this.flagCarry = false;
	}
	this.flagHalf = false;
	this.flagN    = false;
	this.flag5    = ((this.regA & BIT5) != 0);
	this.flag3    = ((this.regA & BIT3) != 0);
	this.instTStates += 4;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void exec10to1F( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0x10:				// DJNZ n
	{
	  int d = nextByte();
	  this.regB = (this.regB - 1) & 0xFF;
	  if( this.regB != 0 ) {
	    doJmpRel( d );
	    this.instTStates += 13;
	  } else {
	    this.instTStates += 8;
	  }
	}
	break;
      case 0x11:				// LD DE,nn
	setRegDE( nextWord() );
	this.instTStates += 10;
	break;
      case 0x12:				// LD (DE),A
	writeMemByte( getRegDE(), this.regA );
	this.instTStates += 7;
	break;
      case 0x13:				// INC DE
	setRegDE( getRegDE() + 1 );
	this.instTStates += 6;
	break;
      case 0x14:				// INC D
	this.regD = doInstINC8( this.regD );
	this.instTStates += 4;
	break;
      case 0x15:				// DEC D
	this.regD = doInstDEC8( this.regD );
	this.instTStates += 4;
	break;
      case 0x16:				// LD D,n
	this.regD = nextByte();
	this.instTStates += 7;
	break;
      case 0x17:				// RLA
	this.regA <<= 1;
	if( this.flagCarry ) {
	  this.regA |= BIT0;
	}
	this.flagCarry = ((this.regA & 0x100) != 0);
	this.regA      &= 0xFF;
	this.flagHalf  = false;
	this.flagN     = false;
	this.flag5     = ((this.regA & BIT5) != 0);
	this.flag3     = ((this.regA & BIT3) != 0);
	this.instTStates += 4;
	break;
      case 0x18:				// JR n
	doJmpRel( nextByte() );
	this.instTStates += 12;
	break;
      case 0x19:
	if( preCode == 0xDD ) {			// ADD IX,DE
	  this.regIX = doInstADD16( this.regIX, getRegDE() );
	} else if( preCode == 0xFD ) {		// ADD IY,DE
	  this.regIY = doInstADD16( this.regIY, getRegDE() );
	} else {				// ADD HL,DE
	  setRegHL( doInstADD16( getRegHL(), getRegDE() ) );
	}
	this.instTStates += 11;
	break;
      case 0x1A:				// LD A,(DE)
	this.regA = readMemByte( getRegDE() );
	this.instTStates += 7;
	break;
      case 0x1B:				// DEC DE
	setRegDE( getRegDE() - 1 );
	this.instTStates += 6;
	break;
      case 0x1C:				// INC E
	this.regE = doInstINC8( this.regE );
	this.instTStates += 4;
	break;
      case 0x1D:				// DEC E
	this.regE = doInstDEC8( this.regE );
	this.instTStates += 4;
	break;
      case 0x1E:				// LD E,n
	this.regE = nextByte();
	this.instTStates += 7;
	break;
      case 0x1F:				// RRA
	{
	  int b          = this.flagCarry ? BIT7 : 0;
	  this.flagCarry = ((this.regA & BIT0) != 0);
	  this.regA      = (this.regA >> 1) | b;
	  this.flagHalf  = false;
	  this.flagN     = false;
	  this.flag5     = ((this.regA & BIT5) != 0);
	  this.flag3     = ((this.regA & BIT3) != 0);
	  this.instTStates += 4;
	}
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void exec20to2F( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0x20:				// JR NZ,n
	{
	  int d = nextByte();
	  if( !this.flagZero ) {
	    this.instTStates += 12;
	    doJmpRel( d );
	  } else {
	    this.instTStates += 7;
	  }
	}
	break;
      case 0x21:
	if( preCode == 0xDD ) {			// LD IX,nn
	  this.regIX = nextWord();
	} else if( preCode == 0xFD ) {		// LD IY,nn
	  this.regIY = nextWord();
	} else {				// LD HL,nn
	  setRegHL( nextWord() );
	}
	this.instTStates += 10;
	break;
      case 0x22:
	if( preCode == 0xDD ) {			// LD (nn),IX
	  writeMemWord( nextWord(), this.regIX );
	} else if( preCode == 0xFD ) {		// LD (nn),IY
	  writeMemWord( nextWord(), this.regIY );
	} else {				// LD (nn),HL
	  writeMemWord( nextWord(), getRegHL() );
	}
	this.instTStates += 16;
	break;
      case 0x23:
	if( preCode == 0xDD ) {			// INC IX
	  this.regIX = (this.regIX + 1) & 0xFFFF;
	} else if( preCode == 0xFD ) {		// INC IY
	  this.regIY = (this.regIY + 1) & 0xFFFF;
	} else {				// INC HL
	  setRegHL( getRegHL() + 1 );
	}
	this.instTStates += 6;
	break;
      case 0x24:
	if( preCode == 0xDD ) {			// *INC IXH
	  setRegIXH( doInstINC8( getRegIXH() ) );
	} else if( preCode == 0xFD ) {		// *INC IYH
	  setRegIYH( doInstINC8( getRegIYH() ) );
	} else {				// INC H
	  this.regH = doInstINC8( this.regH );
	}
	this.instTStates += 4;
	break;
      case 0x25:
	if( preCode == 0xDD ) {			// *DEC IXH
	  setRegIXH( doInstDEC8( getRegIXH() ) );
	} else if( preCode == 0xFD ) {		// *DEC IYH
	  setRegIYH( doInstDEC8( getRegIYH() ) );
	} else {				// DEC H
	  this.regH = doInstDEC8( this.regH );
	}
	this.instTStates += 4;
	break;
      case 0x26:
	if( preCode == 0xDD ) {			// *LD IXH,n
	  setRegIXH( nextByte() );
	} else if( preCode == 0xFD ) {		// *LD IYH,n
	  setRegIYH( nextByte() );
	} else {				// LD H,n
	  this.regH = nextByte();
	}
	this.instTStates += 4;
	break;
      case 0x27:				// DAA
	doInstDAA();
	this.instTStates += 4;
	break;
      case 0x28:				// JR Z,n
	{
	  int n = nextByte();
	  if( this.flagZero ) {
	    this.instTStates += 12;
	    doJmpRel( n );
	  } else {
	    this.instTStates += 7;
	  }
	}
	break;
      case 0x29:
	if( preCode == 0xDD ) {			// ADD IX,IX
	  this.regIX = doInstADD16( this.regIX, this.regIX );
	} else if( preCode == 0xFD ) {		// ADD IY,IY
	  this.regIY = doInstADD16( this.regIY, this.regIY );
	} else {				// ADD HL,HL
	  setRegHL( doInstADD16( getRegHL(), getRegHL() ) );
	}
	this.instTStates += 11;
	break;
      case 0x2A:
	if( preCode == 0xDD ) {			// LD IX,(nn)
	  this.regIX = readMemWord( nextWord() );
	} else if( preCode == 0xFD ) {		// LD IY,(nn)
	  this.regIY = readMemWord( nextWord() );
	} else {				// LD HL,(nn)
	  setRegHL( readMemWord( nextWord() ) );
	}
	this.instTStates += 16;
	break;
      case 0x2B:
	if( preCode == 0xDD ) {			// DEC IX
	  this.regIX = (this.regIX - 1) & 0xFFFF;
	} else if( preCode == 0xFD ) {		// DEC IY
	  this.regIY = (this.regIY - 1) & 0xFFFF;
	} else {				// DEC HL
	  setRegHL( getRegHL() - 1 );
	}
	this.instTStates += 6;
	break;
      case 0x2C:
	if( preCode == 0xDD ) {			// *INC IXL
	  setRegIXL( doInstINC8( getRegIXL() ) );
	} else if( preCode == 0xFD ) {		// *INC IYL
	  setRegIYL( doInstINC8( getRegIYL() ) );
	} else {				// INC L
	  this.regL = doInstINC8( this.regL );
	}
	this.instTStates += 4;
	break;
      case 0x2D:
	if( preCode == 0xDD ) {			// *DEC IXL
	  setRegIXL( doInstDEC8( getRegIXL() ) );
	} else if( preCode == 0xFD ) {		// *DEC IYL
	  setRegIYL( doInstDEC8( getRegIYL() ) );
	} else {				// DEC L
	  this.regL = doInstDEC8( this.regL );
	}
	this.instTStates += 4;
	break;
      case 0x2E:
	if( preCode == 0xDD ) {			// *LD IXL,n
	  setRegIXL( nextByte() );
	} else if( preCode == 0xFD ) {		// *LD IYL,n
	  setRegIYL( nextByte() );
	} else {				// LD L,n
	  this.regL = nextByte();
	}
	this.instTStates += 4;
	break;
      case 0x2F:				// CPL
	this.regA     = (~this.regA) & 0xFF;
	this.flag5    = ((this.regA & BIT5) != 0);
	this.flagHalf = true;
	this.flag3    = ((this.regA & BIT3) != 0);
	this.flagN    = true;
	this.instTStates += 4;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void exec30to3F( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0x30:				// JR NC,n
	{
	  int d = nextByte();
	  if( !this.flagCarry ) {
	    this.instTStates += 12;
	    doJmpRel( d );
	  } else {
	    this.instTStates += 7;
	  }
	}
	break;
      case 0x31:				// LD SP,nn
	this.regSP = nextWord();
	this.instTStates += 10;
	break;
      case 0x32:				// LD (nn),A
	writeMemByte( nextWord(), this.regA );
	this.instTStates += 13;
	break;
      case 0x33:				// INC SP
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.instTStates += 6;
	break;
      case 0x34:
	if( preCode == 0xDD ) {			// INC (IX+d)
	  int m = computeRelAddr( this.regIX, nextByte() );
	  writeMemByte( m, doInstINC8( readMemByte( m ) ) );
	  this.instTStates += 19;
	}
	else if( preCode == 0xFD ) {		// INC (IY+d)
	  int m = computeRelAddr( this.regIY, nextByte() );
	  writeMemByte( m, doInstINC8( readMemByte( m ) ) );
	  this.instTStates += 19;
	} else {				// INC (HL)
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstINC8( readMemByte( regHL ) ) );
	  this.instTStates += 11;
	}
	break;
      case 0x35:
	if( preCode == 0xDD ) {			// DEC (IX+d)
	  int m = computeRelAddr( this.regIX, nextByte() );
	  writeMemByte( m, doInstDEC8( readMemByte( m ) ) );
	  this.instTStates += 19;
	}
	else if( preCode == 0xFD ) {		// DEC (IY+d)
	  int m = computeRelAddr( this.regIY, nextByte() );
	  writeMemByte( m, doInstDEC8( readMemByte( m ) ) );
	  this.instTStates += 19;
	} else {				// DEC (HL)
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstDEC8( readMemByte( regHL ) ) );
	  this.instTStates += 11;
	}
	break;
      case 0x36:
	if( preCode == 0xDD ) {			// LD (IX+d),n
	  int m = computeRelAddr( this.regIX, nextByte() );
	  writeMemByte( m, nextByte() );
	  this.instTStates += 15;
	}
	else if( preCode == 0xFD ) {		// LD (IY+d),n
	  int m = computeRelAddr( this.regIY, nextByte() );
	  writeMemByte( m, nextByte() );
	  this.instTStates += 15;
	} else {				// LD (HL),n
	  writeMemByte( getRegHL(), nextByte() );
	  this.instTStates += 10;
	}
	break;
      case 0x37:				// SCF
	this.flagCarry = true;
	this.flagHalf  = false;
	this.flagN     = false;
	this.flag5     = ((this.regA & BIT5) != 0);
	this.flag3     = ((this.regA & BIT3) != 0);
	this.instTStates += 4;
	break;
      case 0x38:				// JR C,n
	{
	  int d = nextByte();
	  if( this.flagCarry ) {
	    doJmpRel( d );
	    this.instTStates += 12;
	  } else {
	    this.instTStates += 7;
	  }
	}
	break;
      case 0x39:
	if( preCode == 0xDD ) {			// ADD IX,SP
	  this.regIX = doInstADD16( this.regIX, this.regSP );
	} else if( preCode == 0xFD ) {		// ADD IY,SP
	  this.regIY = doInstADD16( this.regIY, this.regSP );
	} else {				// ADD HL,SP
	  setRegHL( doInstADD16( getRegHL(), this.regSP ) );
	}
	this.instTStates += 11;
	break;
      case 0x3A:				// LD A,(nn)
	this.regA = readMemByte( nextWord() );
	this.instTStates += 13;
	break;
      case 0x3B:				// DEC SP
	this.regSP = (this.regSP - 1) & 0xFFFF;
	this.instTStates += 6;
	break;
      case 0x3C:				// INC A
	this.regA = doInstINC8( this.regA );
	this.instTStates += 4;
	break;
      case 0x3D:				// DEC A
	this.regA = doInstDEC8( this.regA );
	this.instTStates += 4;
	break;
      case 0x3E:				// LD A,n
	this.regA = nextByte();
	this.instTStates += 7;
	break;
      case 0x3F:				// CCF
	this.flagHalf  = this.flagCarry;
	this.flagCarry = !this.flagCarry;
	this.flagN     = false;
	this.flag5     = ((this.regA & BIT5) != 0);
	this.flag3     = ((this.regA & BIT3) != 0);
	this.instTStates += 4;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void exec40to7F( int preCode, int opCode )
  {
    if( opCode == 0x76 ) {			// HALT

      // NOP-Befehl mit Ruecksetzen des PCs
      this.haltPC = new Integer( this.instBegPC );
      this.instTStates += 4;
      setHaltState( true );

    } else {					// 8-Bit-Ladebefehle

      if( ((preCode == 0xDD) || (preCode == 0xFD))
	  && (opCode >= 0x70) && (opCode <= 0x77) )
      {
	// LD (IXY+d),...
	writeMemByte(
		computeRelAddr(
			preCode == 0xDD ? this.regIX : this.regIY,
			nextByte() ),
		getSrcValue( -1, opCode ) );
	this.instTStates += 15;			// insgesamt 19
      }
      else if( ((preCode == 0xDD) || (preCode == 0xFD))
	       && ((opCode & 0x07) == 6) )
      {
	// LD ...,(IXY+d)
	int value = readMemByte( computeRelAddr(
			preCode == 0xDD ? this.regIX : this.regIY,
			nextByte() ) );
	switch( opCode & 0x38 ) {		// Bits 3-5: Ziel
	  case 0x00:
	    this.regB = value;
	    break;
	  case 0x08:
	    this.regC = value;
	    break;
	  case 0x10:
	    this.regD = value;
	    break;
	  case 0x18:
	    this.regE = value;
	    break;
	  case 0x20:
	    this.regH = value;
	    break;
	  case 0x28:
	    this.regL = value;
	    break;
	  case 0x38:
	    this.regA = value;
	    break;
	  default:
	    throwIllegalState( opCode );
	}
	this.instTStates += 15;			// insgesamt 19

      } else {

	int value = getSrcValue( preCode, opCode );
	switch( opCode & 0x38 ) {		// Bits 3-5: Ziel
	  case 0x00:
	    this.regB = value;
	    break;
	  case 0x08:
	    this.regC = value;
	    break;
	  case 0x10:
	    this.regD = value;
	    break;
	  case 0x18:
	    this.regE = value;
	    break;
	  case 0x20:
	    if( preCode == 0xDD ) {
	      this.regIX = (value << 8) | (this.regIX & 0xFF);
	    } else if( preCode == 0xFD ) {
	      this.regIY = (value << 8) | (this.regIY & 0xFF);
	    } else {
	      this.regH = value;
	    }
	    break;
	  case 0x28:
	    if( preCode == 0xDD ) {
	      this.regIX = (this.regIX & 0xFF00) | value;
	    } else if( preCode == 0xFD ) {
	      this.regIY = (this.regIY & 0xFF00) | value;
	    } else {
	      this.regL = value;
	    }
	    break;
	  case 0x30:
	    writeMemByte( getRegHL(), value );
	    this.instTStates += 3;	// bei "LD (HL),..." insgesamt 7 Takte
	    break;
	  case 0x38:
	    this.regA = value;
	    break;
	  default:
	    throwIllegalState( opCode );
	}
	this.instTStates += 4;
	if( (opCode & 0x07) == 6 )
	  this.instTStates += 3;	 // bei "LD ...,(HL)" insgesamt 7 Takte
      }
    }
  }


  private void exec80toBF( int preCode, int opCode )
  {
    int value = getSrcValue( preCode, opCode );
    switch( opCode & 0x38 ) {		// Bits 3-5: Operation
      case 0x00:
	doInstADD8( value, 0 );
	break;
      case 0x08:
	doInstADD8( value, this.flagCarry ? 1 : 0 );
	break;
      case 0x10:
	doInstSUB8( value, 0 );
	break;
      case 0x18:
	doInstSUB8( value, this.flagCarry ? 1 : 0 );
	break;
      case 0x20:
	doInstAND( value );
	break;
      case 0x28:
	doInstXOR( value );
	break;
      case 0x30:
	doInstOR( value );
	break;
      case 0x38:
	doInstCP( value );
	break;
      default:
	throwIllegalState( opCode );
    }
    if( (opCode & 0x07) == 6 ) {
      if( (preCode == 0xDD) || (preCode == 0xFD) ) {
	this.instTStates += 15;			// insgesamt 19: ....(IXY+d)
      } else {
	this.instTStates += 7;			// (HL)
      }
    } else {
      this.instTStates += 4;
    }
  }


  private void execC0toCF( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0xC0:				// RET NZ
	if( !this.flagZero ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xC1:				// POP BC
	this.regC  = readMemByte( this.regSP );
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.regB  = readMemByte( this.regSP );
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.instTStates += 10;
	break;
      case 0xC2:				// JP NZ,nn
	{
	  int nn = nextWord();
	  if( !this.flagZero ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xC3:				// JP nn
	this.regPC = nextWord();
	this.instTStates += 10;
	break;
      case 0xC4:				// CALL NZ,nn
	{
	  int nn = nextWord();
	  if( !this.flagZero ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xC5:				// PUSH BC
	this.regSP = (this.regSP - 1) & 0xFFFF;
	writeMemByte( this.regSP, this.regB );
	this.regSP = (this.regSP - 1) & 0xFFFF;
	writeMemByte( this.regSP, this.regC );
	this.instTStates += 11;
	break;
      case 0xC6:				// ADD n
	doInstADD8( nextByte(), 0 );
	this.instTStates += 7;
	break;
      case 0xC7:				// RST 00
	doPush( this.regPC );
	this.regPC = 0x0000;
	this.instTStates += 11;
	break;
      case 0xC8:				// RET Z
	if( this.flagZero ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xC9:				// RET
	this.regPC = doPop();
	this.instTStates += 10;
	break;
      case 0xCA:				// JP Z,nn
	{
	  int nn = nextWord();
	  if( this.flagZero ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xCB:				// Befehle mit Vorbyte CB
	incRegR();
	if( preCode == 0xDD ) {
	  this.regIX = execIXY_CB( this.regIX );
	} else if( preCode == 0xFD ) {
	  this.regIY = execIXY_CB( this.regIY );
	} else {
	  execCB();
	}
	break;
      case 0xCC:				// CALL Z,nn
	{
	  int nn = nextWord();
	  if( this.flagZero ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xCD:				// CALL nn
	{
	  int nn = nextWord();
	  doPush( this.regPC );
	  this.regPC = nn;
	  this.instTStates += 17;
	}
	break;
      case 0xCE:				// ADC n
	doInstADD8( nextByte(), this.flagCarry ? 1 : 0 );
	this.instTStates += 7;
	break;
      case 0xCF:				// RST 08
	doPush( this.regPC );
	this.regPC = 0x0008;
	this.instTStates += 11;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execD0toDF( int opCode )
  {
    switch( opCode ) {
      case 0xD0:				// RET NC
	if( !this.flagCarry ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xD1:				// POP DE
	this.regE  = readMemByte( this.regSP );
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.regD  = readMemByte( this.regSP );
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.instTStates += 10;
	break;
      case 0xD2:				// JP NC,nn
	{
	  int nn = nextWord();
	  if( !this.flagCarry ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xD3:				// OUT (n),A
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regA << 8) | nextByte(), this.regA );
	}
	this.instTStates += 11;
	break;
      case 0xD4:				// CALL NC,nn
	{
	  int nn = nextWord();
	  if( !this.flagCarry ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xD5:				// PUSH DE
	this.regSP = (this.regSP - 1) & 0xFFFF;
	writeMemByte( this.regSP, this.regD );
	this.regSP = (this.regSP - 1) & 0xFFFF;
	writeMemByte( this.regSP, this.regE );
	this.instTStates += 11;
	break;
      case 0xD6:				// SUB n
	doInstSUB8( nextByte(), 0 );
	this.instTStates += 7;
	break;
      case 0xD7:				// RST 10
	doPush( this.regPC );
	this.regPC = 0x0010;
	this.instTStates += 11;
	break;
      case 0xD8:				// RET C
	if( this.flagCarry ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xD9:				// EXX
	{
	  int b      = this.regB;
	  this.regB  = this.regB2;
	  this.regB2 = b;
	  b          = this.regC;
	  this.regC  = this.regC2;
	  this.regC2 = b;
	  b          = this.regD;
	  this.regD  = this.regD2;
	  this.regD2 = b;
	  b          = this.regE;
	  this.regE  = this.regE2;
	  this.regE2 = b;
	  b          = this.regH;
	  this.regH  = this.regH2;
	  this.regH2 = b;
	  b          = this.regL;
	  this.regL  = this.regL2;
	  this.regL2 = b;
	  this.instTStates += 4;
	}
	break;
      case 0xDA:				// JP C,nn
	{
	  int nn = nextWord();
	  if( this.flagCarry )
	    this.regPC = nn;
	  this.instTStates += 10;
	}
	break;
      case 0xDB:				// IN A,(n)
	{
	  int p = nextByte();
	  if( this.ioSys != null ) {
	    this.regA = this.ioSys.readIOByte( (this.regA << 8) | p ) & 0xFF;
	  } else {
	    this.regA = 0;
	  }
	  this.instTStates += 11;
	}
	break;
      case 0xDC:				// CALL C,nn
	{
	  int nn = nextWord();
	  if( this.flagCarry ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xDD:				// IX-Befehle
	this.instTStates += 4;
	execInst( opCode, nextByte() );
	break;
      case 0xDE:				// SBC n
	doInstSUB8( nextByte(), this.flagCarry ? 1 : 0 );
	this.instTStates += 7;
	break;
      case 0xDF:				// RST 18
	doPush( this.regPC );
	this.regPC = 0x0018;
	this.instTStates += 11;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execE0toEF( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0xE0:				// RET PO
	if( !this.flagPV ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xE1:
	if( preCode == 0xDD ) {			// POP IX
	  this.regIX = doPop();
	} else if( preCode == 0xFD ) {		// POP IY
	  this.regIY = doPop();
	} else {				// POP HL
	  setRegHL( doPop() );
	}
	this.instTStates += 10;
	break;
      case 0xE2:				// JP PO,nn
	{
	  int nn = nextWord();
	  if( !this.flagPV ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xE3:
	{
	  int m = readMemWord( this.regSP );
	  if( preCode == 0xDD ) {		// EX (SP),IX
	    writeMemWord( this.regSP, this.regIX );
	    this.regIX = m;
	  }
	  else if( preCode == 0xFD ) {		// EX (SP),IY
	    writeMemWord( this.regSP, this.regIY );
	    this.regIY = m;
	  } else {				// EX (SP),HL
	    writeMemWord( this.regSP, getRegHL() );
	    setRegHL( m );
	  }
	  this.instTStates += 19;
	}
	break;
      case 0xE4:				// CALL PO,nn
	{
	  int nn = nextWord();
	  if( !this.flagPV ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xE5:
	if( preCode == 0xDD ) {			// PUSH IX
	  doPush( this.regIX );
	} else if( preCode == 0xFD ) {		// PUSH IY
	  doPush( this.regIY );
	} else {				// PUSH HL
	  doPush( getRegHL() );
	}
	this.instTStates += 11;
	break;
      case 0xE6:				// AND n
	doInstAND( nextByte() );
	this.instTStates += 7;
	break;
      case 0xE7:				// RST 20
	doPush( this.regPC );
	this.regPC = 0x0020;
	this.instTStates += 11;
	break;
      case 0xE8:				// RET PE
	if( this.flagPV ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xE9:
	if( preCode == 0xDD ) {			// JP (IX)
	  this.regPC = this.regIX;
	} else if( preCode == 0xFD ) {		// JP (IY)
	  this.regPC = this.regIY;
	} else {				// JP (HL)
	  this.regPC = getRegHL();
	}
	this.instTStates += 4;
	break;
      case 0xEA:				// JP PE,nn
	{
	  int nn = nextWord();
	  if( this.flagPV ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xEB:				// EX DE,HL
	{
	  int m = getRegDE();
	  setRegDE( getRegHL() );
	  setRegHL( m );
	  this.instTStates += 4;
	}
	break;
      case 0xEC:				// CALL PE,nn
	{
	  int nn = nextWord();
	  if( this.flagPV ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xED:				// Befehle mit Vorbyte ED
	incRegR();
	execED();
	break;
      case 0xEE:				// XOR n
	doInstXOR( nextByte() );
	this.instTStates += 7;
	break;
      case 0xEF:				// RST 28
	doPush( this.regPC );
	this.regPC = 0x0028;
	this.instTStates += 11;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execF0toFF( int preCode, int opCode )
  {
    switch( opCode ) {
      case 0xF0:				// RET P
	if( !this.flagSign ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xF1:				// POP AF
	setRegF( readMemByte( this.regSP ) );
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.regA  = readMemByte( this.regSP );
	this.regSP = (this.regSP + 1) & 0xFFFF;
	this.instTStates += 10;
	break;
      case 0xF2:				// JP P,nn
	{
	  int nn = nextWord();
	  if( !this.flagSign ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xF3:				// DI
	this.iff1 = false;
	this.iff2 = false;
	this.lastInstWasEIorDI = true;
	this.instTStates += 4;
	break;
      case 0xF4:				// CALL P,nn
	{
	  int nn = nextWord();
	  if( !this.flagSign ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xF5:				// PUSH AF
	this.regSP = (this.regSP - 1) & 0xFFFF;
	writeMemByte( this.regSP, this.regA );
	this.regSP = (this.regSP - 1) & 0xFFFF;
	writeMemByte( this.regSP, getRegF() );
	this.instTStates += 11;
	break;
      case 0xF6:				// OR n
	doInstOR( nextByte() );
	this.instTStates += 7;
	break;
      case 0xF7:				// RST 30
	doPush( this.regPC );
	this.regPC = 0x0030;
	this.instTStates += 11;
	break;
      case 0xF8:				// RET M
	if( this.flagSign ) {
	  this.regPC = doPop();
	  this.instTStates += 11;
	} else {
	  this.instTStates += 5;
	}
	break;
      case 0xF9:
	if( preCode == 0xDD ) {
	  this.regSP = this.regIX;
	} else if( preCode == 0xFD ) {
	  this.regSP = this.regIY;
	} else {				// LD SP,HL
	  this.regSP = getRegHL();
	}
	this.instTStates += 6;
	break;
      case 0xFA:				// JP M,nn
	{
	  int nn = nextWord();
	  if( this.flagSign ) {
	    this.regPC = nn;
	  }
	  this.instTStates += 10;
	}
	break;
      case 0xFB:				// EI
	this.iff1 = true;
	this.iff2 = true;
	this.lastInstWasEIorDI = true;
	this.instTStates += 4;
	break;
      case 0xFC:				// CALL M,nn
	{
	  int nn = nextWord();
	  if( this.flagSign ) {
	    doPush( this.regPC );
	    this.regPC = nn;
	    this.instTStates += 17;
	  } else {
	    this.instTStates += 10;
	  }
	}
	break;
      case 0xFD:				// IY-Befehle
	this.instTStates += 4;
	execInst( opCode, nextByte() );
	break;
      case 0xFE:				// CP n
	doInstCP( nextByte() );
	this.instTStates += 7;
	break;
      case 0xFF:				// RST 38
	doPush( this.regPC );
	this.regPC = 0x0038;
	this.instTStates += 11;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


	/* --- Emulation der Befehle mit Vorbyte CB --- */

  private void execCB()
  {
    int opCode = nextByte();
    if( opCode < 0x80 ) {
      if( opCode < 0x40 ) {
	if( opCode < 0x20 ) {
	  if( opCode < 0x10 ) {
	    execCB_00to0F( opCode );
	  } else {
	    execCB_10to1F( opCode );
	  }
	} else {
	  if( opCode < 0x30 ) {
	    execCB_20to2F( opCode );
	  } else {
	    execCB_30to3F( opCode );
	  }
	}
      } else {

	// 0x40-0x7F: BIT n,r
	doInstBIT( opCode, getSrcValue( -1, opCode ) );
	this.instTStates += ((opCode & 0x07) == 6 ? 12 : 8);
      }

    } else {

      if( opCode < 0xC0 ) {

	// 0x80-0xBF: RES r,n
	int mask = ~getBitMask( opCode );
	switch( opCode & 0x07 ) {
	  case 0:
	    this.regB &= mask;
	    this.instTStates += 8;
	    break;
	  case 1:
	    this.regC &= mask;
	    this.instTStates += 8;
	    break;
	  case 2:
	    this.regD &= mask;
	    this.instTStates += 8;
	    break;
	  case 3:
	    this.regE &= mask;
	    this.instTStates += 8;
	    break;
	  case 4:
	    this.regH &= mask;
	    this.instTStates += 8;
	    break;
	  case 5:
	    this.regL &= mask;
	    this.instTStates += 8;
	    break;
	  case 6:
	    int regHL = getRegHL();
	    writeMemByte( regHL, readMemByte( regHL ) & mask );
	    this.instTStates += 15;
	    break;
	  case 7:
	    this.regA &= mask;
	    this.instTStates += 8;
	    break;
	}

      } else {

	// 0xC0-0xFF: SET r,n
	int mask = getBitMask( opCode );
	switch( opCode & 0x07 ) {
	  case 0:
	    this.regB |= mask;
	    this.instTStates += 8;
	    break;
	  case 1:
	    this.regC |= mask;
	    this.instTStates += 8;
	    break;
	  case 2:
	    this.regD |= mask;
	    this.instTStates += 8;
	    break;
	  case 3:
	    this.regE |= mask;
	    this.instTStates += 8;
	    break;
	  case 4:
	    this.regH |= mask;
	    this.instTStates += 8;
	    break;
	  case 5:
	    this.regL |= mask;
	    this.instTStates += 8;
	    break;
	  case 6:
	    int regHL = getRegHL();
	    writeMemByte( regHL, readMemByte( regHL ) | mask );
	    this.instTStates += 15;
	    break;
	  case 7:
	    this.regA |= mask;
	    this.instTStates += 8;
	    break;
	}
      }
    }
  }


  private void execCB_00to0F( int opCode )
  {
    switch( opCode ) {
      case 0x00:				// RLC B
	this.regB = doInstRLC( this.regB );
	this.instTStates += 8;
	break;
      case 0x01:				// RLC C
	this.regC = doInstRLC( this.regC );
	this.instTStates += 8;
	break;
      case 0x02:				// RLC D
	this.regD = doInstRLC( this.regD );
	this.instTStates += 8;
	break;
      case 0x03:				// RLC E
	this.regE = doInstRLC( this.regE );
	this.instTStates += 8;
	break;
      case 0x04:				// RLC H
	this.regH = doInstRLC( this.regH );
	this.instTStates += 8;
	break;
      case 0x05:				// RLC L
	this.regL = doInstRLC( this.regL );
	this.instTStates += 8;
	break;
      case 0x06:				// RLC (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstRLC( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x07:				// RLC A
	this.regA = doInstRLC( this.regA );
	this.instTStates += 8;
	break;
      case 0x08:				// RRC B
	this.regB = doInstRRC( this.regB );
	this.instTStates += 8;
	break;
      case 0x09:				// RRC C
	this.regC = doInstRRC( this.regC );
	this.instTStates += 8;
	break;
      case 0x0A:				// RRC D
	this.regD = doInstRRC( this.regD );
	this.instTStates += 8;
	break;
      case 0x0B:				// RRC E
	this.regE = doInstRRC( this.regE );
	this.instTStates += 8;
	break;
      case 0x0C:				// RRC H
	this.regH = doInstRRC( this.regH );
	this.instTStates += 8;
	break;
      case 0x0D:				// RRC L
	this.regL = doInstRRC( this.regL );
	this.instTStates += 8;
	break;
      case 0x0E:				// RRC (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstRRC( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x0F:				// RRC A
	this.regA = doInstRRC( this.regA );
	this.instTStates += 8;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execCB_10to1F( int opCode )
  {
    switch( opCode ) {
      case 0x10:				// RL B
	this.regB = doInstRL( this.regB );
	this.instTStates += 8;
	break;
      case 0x11:				// RL C
	this.regC = doInstRL( this.regC );
	this.instTStates += 8;
	break;
      case 0x12:				// RL D
	this.regD = doInstRL( this.regD );
	this.instTStates += 8;
	break;
      case 0x13:				// RL E
	this.regE = doInstRL( this.regE );
	this.instTStates += 8;
	break;
      case 0x14:				// RL H
	this.regH = doInstRL( this.regH );
	this.instTStates += 8;
	break;
      case 0x15:				// RL L
	this.regL = doInstRL( this.regL );
	this.instTStates += 8;
	break;
      case 0x16:				// RL (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstRL( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x17:				// RL A
	this.regA = doInstRL( this.regA );
	this.instTStates += 8;
	break;
      case 0x18:				// RR B
	this.regB = doInstRR( this.regB );
	this.instTStates += 8;
	break;
      case 0x19:				// RR D
	this.regC = doInstRR( this.regC );
	this.instTStates += 8;
	break;
      case 0x1A:				// RR D
	this.regD = doInstRR( this.regD );
	this.instTStates += 8;
	break;
      case 0x1B:				// RR E
	this.regE = doInstRR( this.regE );
	this.instTStates += 8;
	break;
      case 0x1C:				// RR H
	this.regH = doInstRR( this.regH );
	this.instTStates += 8;
	break;
      case 0x1D:				// RR L
	this.regL = doInstRR( this.regL );
	this.instTStates += 8;
	break;
      case 0x1E:				// RR (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstRR( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x1F:				// RR A
	this.regA = doInstRR( this.regA );
	this.instTStates += 8;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execCB_20to2F( int opCode )
  {
    switch( opCode ) {
      case 0x20:				// SLA B
	this.regB = doInstSLA( this.regB );
	this.instTStates += 8;
	break;
      case 0x21:				// SLA C
	this.regC = doInstSLA( this.regC );
	this.instTStates += 8;
	break;
      case 0x22:				// SLA D
	this.regD = doInstSLA( this.regD );
	this.instTStates += 8;
	break;
      case 0x23:				// SLA E
	this.regE = doInstSLA( this.regE );
	this.instTStates += 8;
	break;
      case 0x24:				// SLA H
	this.regH = doInstSLA( this.regH );
	this.instTStates += 8;
	break;
      case 0x25:				// SLA L
	this.regL = doInstSLA( this.regL );
	this.instTStates += 8;
	break;
      case 0x26:				// SLA (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstSLA( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x27:				// SLA A
	this.regA = doInstSLA( this.regA );
	this.instTStates += 8;
	break;
      case 0x28:				// SRA B
	this.regB = doInstSRA( this.regB );
	this.instTStates += 8;
	break;
      case 0x29:				// SRA C
	this.regC = doInstSRA( this.regC );
	this.instTStates += 8;
	break;
      case 0x2A:				// SRA D
	this.regD = doInstSRA( this.regD );
	this.instTStates += 8;
	break;
      case 0x2B:				// SRA E
	this.regE = doInstSRA( this.regE );
	this.instTStates += 8;
	break;
      case 0x2C:				// SRA H
	this.regH = doInstSRA( this.regH );
	this.instTStates += 8;
	break;
      case 0x2D:				// SRA L
	this.regL = doInstSRA( this.regL );
	this.instTStates += 8;
	break;
      case 0x2E:				// SRA (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstSRA( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x2F:				// SRA A
	this.regA = doInstSRA( this.regA );
	this.instTStates += 8;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execCB_30to3F( int opCode )
  {
    switch( opCode ) {
      case 0x30:				// *SLL B
	this.regB = doInstSLL( this.regB );
	this.instTStates += 8;
	break;
      case 0x31:				// *SLL C
	this.regC = doInstSLL( this.regC );
	this.instTStates += 8;
	break;
      case 0x32:				// *SLL D
	this.regD = doInstSLL( this.regD );
	this.instTStates += 8;
	break;
      case 0x33:				// *SLL E
	this.regE = doInstSLL( this.regE );
	this.instTStates += 8;
	break;
      case 0x34:				// *SLL H
	this.regH = doInstSLL( this.regH );
	this.instTStates += 8;
	break;
      case 0x35:				// *SLL L
	this.regL = doInstSLL( this.regL );
	this.instTStates += 8;
	break;
      case 0x36:				// *SLL (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstSLL( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x37:				// *SLL A
	this.regA = doInstSLL( this.regA );
	this.instTStates += 8;
	break;
      case 0x38:				// SRL B
	this.regB = doInstSRL( this.regB );
	this.instTStates += 8;
	break;
      case 0x39:				// SRL C
	this.regC = doInstSRL( this.regC );
	this.instTStates += 8;
	break;
      case 0x3A:				// SRL D
	this.regD = doInstSRL( this.regD );
	this.instTStates += 8;
	break;
      case 0x3B:				// SRL E
	this.regE = doInstSRL( this.regE );
	this.instTStates += 8;
	break;
      case 0x3C:				// SRL H
	this.regH = doInstSRL( this.regH );
	this.instTStates += 8;
	break;
      case 0x3D:				// SRL L
	this.regL = doInstSRL( this.regL );
	this.instTStates += 8;
	break;
      case 0x3E:				// SRL (HL)
	{
	  int regHL = getRegHL();
	  writeMemByte( regHL, doInstSRL( readMemByte( regHL ) ) );
	  this.instTStates += 15;
	}
	break;
      case 0x3F:				// SRL A
	this.regA = doInstSRL( this.regA );
	this.instTStates += 8;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


	/* --- Emulation der Befehle mit Vorbyte ED --- */

  private void execED()
  {
    int opCode = nextByte();
    if( opCode < 0x80 ) {
      if( opCode < 0x40 ) {
	this.instTStates += 8;			// *NOP
      } else {
	if( opCode < 0x60 ) {
	  if( opCode < 0x50 ) {
	    execED_40to4F( opCode );
	  } else {
	    execED_50to5F( opCode );
	  }
	} else {
	  if( opCode < 0x70 ) {
	    execED_60to6F( opCode );
	  } else {
	    execED_70to7F( opCode );
	  }
	}
      }
    } else {
      execED_80toFF( opCode );
    }
  }


  private void execED_40to4F( int opCode )
  {
    switch( opCode ) {
      case 0x40:				// IN B,(C)
	this.regB = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x41:				// OUT (C),B
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regB );
	}
	this.instTStates += 12;
	break;
      case 0x42:				// SBC HL,BC
	doInstSBC16( getRegBC() );
	this.instTStates += 15;
	break;
      case 0x43:				// LD (nn),BC
	writeMemWord( nextWord(), getRegBC() );
	this.instTStates += 20;
	break;
      case 0x44:				// NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x45:				// RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x46:				// IM 0
	this.interruptMode = 0;
	this.instTStates += 8;
	break;
      case 0x47:				// LD I,A
	this.interruptReg = this.regA;
	this.instTStates += 9;
	break;
      case 0x48:				// IN C,(C)
	this.regC = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x49:				// OUT (C),C
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regC );
	}
	this.instTStates += 12;
	break;
      case 0x4A:				// ADC HL,BC
	doInstADC16( getRegBC() );
	this.instTStates += 15;
	break;
      case 0x4B:				// LD BC,(nn)
	setRegBC( readMemWord( nextWord() ) );
	this.instTStates += 20;
	break;
      case 0x4C:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x4D:				// RETI
	doInstRETN();
	Z80InterruptSource[] iSources = this.interruptSources;
	if( iSources != null ) {
	  for( int i = 0; i < iSources.length; i++ ) {
	    if( iSources[ i ].isInterruptPending() ) {
	      iSources[ i ].interruptFinished();
	      break;
	    }
	  }
	}
	this.instTStates += 14;
	break;
      case 0x4E:				// *IM ?
	this.interruptMode = 0;
	this.instTStates += 8;
	break;
      case 0x4F:				// LD R,A
	this.regR_bits0to6 = this.regA & 0x7F;
	this.regR_bit7     = ((this.regA & BIT7) != 0);
	this.instTStates += 9;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execED_50to5F( int opCode )
  {
    switch( opCode ) {
      case 0x50:				// IN D,(C)
	this.regD = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x51:				// OUT (C),D
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regD );
	}
	this.instTStates += 12;
	break;
      case 0x52:				// SBC HL,DE
	doInstSBC16( getRegDE() );
	this.instTStates += 15;
	break;
      case 0x53:				// LD (nn),DE
	writeMemWord( nextWord(), getRegDE() );
	this.instTStates += 20;
	break;
      case 0x54:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x55:				// *RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x56:				// IM 1
	this.interruptMode = 1;
	this.instTStates += 8;
	break;
      case 0x57:				// LD A,I
	this.regA     = this.interruptReg;
	this.flagSign = ((this.regA & BIT7) != 0);
	this.flagZero = (this.regA == 0);
	this.flagPV   = this.iff2;
	this.flagHalf = false;
	this.flagN    = false;
	this.flag5    = ((this.regA & BIT5) != 0);
	this.flag3    = ((this.regA & BIT3) != 0);
	this.instTStates += 9;
	break;
      case 0x58:				// IN E,(C)
	this.regE = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x59:				// OUT (C),E
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regE );
	}
	this.instTStates += 12;
	break;
      case 0x5A:				// ADC HL,DE
	doInstADC16( getRegDE() );
	this.instTStates += 15;
	break;
      case 0x5B:				// LD DE,(nn)
	setRegDE( readMemWord( nextWord() ) );
	this.instTStates += 20;
	break;
      case 0x5C:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x5D:				// *RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x5E:				// IM 2
	this.interruptMode = 2;
	this.instTStates += 8;
	break;
      case 0x5F:				// LD A,R
	this.regA = this.regR_bits0to6;
	if( this.regR_bit7 ) {
	  this.regA |= BIT7;
	}
	this.flagSign = ((this.regA & BIT7) != 0);
	this.flagZero = (this.regA == 0);
	this.flagPV   = this.iff2;
	this.flagHalf = false;
	this.flagN    = false;
	this.flag5    = ((this.regA & BIT5) != 0);
	this.flag3    = ((this.regA & BIT3) != 0);
	this.instTStates += 9;
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execED_60to6F( int opCode )
  {
    switch( opCode ) {
      case 0x60:				// IN H,(C)
	this.regH = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x61:				// OUT (C),H
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regH );
	}
	this.instTStates += 12;
	break;
      case 0x62:				// SBC HL,HL
	doInstSBC16( getRegHL() );
	this.instTStates += 15;
	break;
      case 0x63:				// *LD (nn),HL
	writeMemWord( nextWord(), getRegHL() );
	this.instTStates += 20;
	break;
      case 0x64:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x65:				// *RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x66:				// *IM 0
	this.interruptMode = 0;
	this.instTStates += 8;
	break;
      case 0x67:				// RRD
	{
	  int a = getRegHL();
	  int m = readMemByte( a );
	  int r = this.regA;
	  this.regA = (this.regA & 0xF0) | (m & 0x0F);
	  writeMemByte( a, ((m >> 4) & 0x0F) | ((r << 4) & 0xF0) );
	  this.flagSign = ((this.regA & BIT7) != 0);
	  this.flagZero = (this.regA == 0);
	  this.flagHalf = false;
	  this.flagN    = false;
	  this.flag5    = ((this.regA & BIT5) != 0);
	  this.flag3    = ((this.regA & BIT3) != 0);
	  updParityFlag( this.regA );
	  this.instTStates += 18;
	}
	break;
      case 0x68:				// IN L,(C)
	this.regL = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x69:				// OUT (C),L
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regL );
	}
	this.instTStates += 12;
	break;
      case 0x6A:				// ADC HL,HL
	doInstADC16( getRegHL() );
	this.instTStates += 15;
	break;
      case 0x6B:				// *LD HL,(nn)
	setRegHL( readMemWord( nextWord() ) );
	this.instTStates += 20;
	break;
      case 0x6C:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x6D:				// *RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x6E:				// *IM ?
	this.interruptMode = 0;
	this.instTStates += 8;
	break;
      case 0x6F:				// RLD
	{
	  int a = getRegHL();
	  int m = readMemByte( a );
	  int r = this.regA;
	  this.regA = (this.regA & 0xF0) | ((m >> 4) & 0x0F);
	  writeMemByte( a, ((m << 4) & 0xF0) | (r & 0x0F) );
	  this.flagSign = ((this.regA & BIT7) != 0);
	  this.flagZero = (this.regA == 0);
	  this.flagHalf = false;
	  this.flagN    = false;
	  this.flag5    = ((this.regA & BIT5) != 0);
	  this.flag3    = ((this.regA & BIT3) != 0);
	  updParityFlag( this.regA );
	  this.instTStates += 18;
	}
	break;
      default:
	throwIllegalState( opCode );
    }
  }


  private void execED_70to7F( int opCode )
  {
    switch( opCode ) {
      case 0x70:				// *IN F,(C)
	doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x71:				// *OUT (C),?
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, 0 );
	}
	this.instTStates += 12;
	break;
      case 0x72:				// SBC HL,SP
	doInstSBC16( this.regSP );
	this.instTStates += 15;
	break;
      case 0x73:				// LD (nn),SP
	writeMemWord( nextWord(), this.regSP );
	this.instTStates += 20;
	break;
      case 0x74:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x75:				// *RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x76:				// *IM 1
	this.interruptMode = 1;
	this.instTStates += 8;
	break;
      case 0x78:				// IN A,(C)
	this.regA = doInstIN( this.regC );
	this.instTStates += 12;
	break;
      case 0x79:				// OUT (C),A
	if( this.ioSys != null ) {
	  this.ioSys.writeIOByte( (this.regB << 8) | this.regC, this.regA );
	}
	this.instTStates += 12;
	break;
      case 0x7A:				// ADC HL,SP
	doInstADC16( this.regSP );
	this.instTStates += 15;
	break;
      case 0x7B:				// LD SP,(nn)
	this.regSP = readMemWord( nextWord() );
	this.instTStates += 20;
	break;
      case 0x7C:				// *NEG
	doInstNEG();
	this.instTStates += 8;
	break;
      case 0x7D:				// *RETN
	doInstRETN();
	this.instTStates += 14;
	break;
      case 0x7E:				// *IM 2
	this.interruptMode = 2;
	this.instTStates += 8;
	break;
      default:					// *NOP
	this.instTStates += 8;
    }
  }


  private void execED_80toFF( int opCode )
  {
    switch( opCode ) {
      case 0xA0:				// LDI
	doInstBlockLD( 1 );
	this.instTStates += 16;
	break;
      case 0xA1:				// CPI
	doInstBlockCP( 1 );
	this.instTStates += 16;
	break;
      case 0xA2:				// INI
	doInstBlockIN( 1 );
	this.instTStates += 16;
	break;
      case 0xA3:				// OUTI
	doInstBlockOUT( 1 );
	this.instTStates += 16;
	break;
      case 0xA8:				// LDD
	doInstBlockLD( -1 );
	this.instTStates += 16;
	break;
      case 0xA9:				// CPD
	doInstBlockCP( -1 );
	this.instTStates += 16;
	break;
      case 0xAA:				// IND
	doInstBlockIN( -1 );
	this.instTStates += 16;
	break;
      case 0xAB:				// OUTD
	doInstBlockOUT( -1 );
	this.instTStates += 16;
	break;
      case 0xB0:				// LDIR
	doInstBlockLD( 1 );
	if( !this.flagPV ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xB1:				// CPIR
	doInstBlockCP( 1 );
	if( this.flagZero || !this.flagPV ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xB2:				// INIR
	doInstBlockIN( 1 );
	if( this.flagZero ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xB3:				// OTIR
	doInstBlockOUT( 1 );
	if( this.flagZero ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xB8:				// LDDR
	doInstBlockLD( -1 );
	if( !this.flagPV ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xB9:				// CPDR
	doInstBlockCP( -1 );
	if( this.flagZero || !this.flagPV ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xBA:				// INDR
	doInstBlockIN( -1 );
	if( this.flagZero ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      case 0xBB:				// OTDR
	doInstBlockOUT( -1 );
	if( this.flagZero ) {
	  this.instTStates += 16;
	} else {
	  incRegR();
	  setPCRel( -2 );
	  this.instTStates += 21;
	}
	break;
      default:					// *NOP
	this.instTStates += 8;
    }
  }


	/* --- Emulation der Befehle mit Vorbyte DD oder FD --- */

  private int execIXY_CB( int regIXY )
  {
    int addr   = computeRelAddr( regIXY, nextByte() );
    int value  = readMemByte( addr );
    int opCode = nextByte();
    if( opCode < 0x80 ) {
      if( opCode < 0x40 ) {
        switch( opCode & 0x38 ) {
          case 0x00:
            value = doInstRLC( value );
            break;
          case 0x08:
            value = doInstRRC( value );
            break;
          case 0x10:
            value = doInstRL( value );
            break;
          case 0x18:
            value = doInstRR( value );
            break;
          case 0x20:
            value = doInstSLA( value );
            break;
          case 0x28:
            value = doInstSRA( value );
            break;
          case 0x30:
            value = doInstSLL( value );
            break;
          case 0x38:
            value = doInstSRL( value );
            break;
          default:
            throwIllegalState( opCode );
        }
        writeMemByte( addr, value );
	execIXY_setUndoc( opCode, value );
	this.instTStates += 19;

      } else {

	// 0x40-0x7F: BIT n,(IXY+d)
	doInstBIT( opCode, value );
	this.instTStates += 16;
      }

    } else {

      if( opCode < 0xC0 ) {
        // IXY CB 80-BF: RES (IXY+d)
	value &= ~getBitMask( opCode );
      } else {
        // IXY CB 80-BF: SET (IXY+d)
	value |= getBitMask( opCode );
      }
      writeMemByte( addr, value );
      execIXY_setUndoc( opCode, value );
      this.instTStates += 19;
    }
    return regIXY;
  }


  /*
   * Bei vielen undokumentierten Rotations- und Schiebebefehlen
   * mit den Indexregistern wird das Ergebnis zusaetzlich
   * in ein Register geschrieben.
   * Das erledigt diese Methode.
   */
  private void execIXY_setUndoc( int opCode, int value )
  {
    switch( opCode & 0x07 ) {
      case 0:
	this.regB = value;
	break;
      case 1:
	this.regC = value;
	break;
      case 2:
	this.regD = value;
	break;
      case 3:
	this.regE = value;
	break;
      case 4:
	this.regH = value;
	break;
      case 5:
	this.regL = value;
	break;
      case 7:
	this.regA = value;
	break;
    }
  }


	/* --- Hilfsmethoden fuer einzelne Befehle --- */

  private void doInstADD8( int op2, int op3 )
  {
    int result = this.regA + op2 + op3;
    int m      = this.regA ^ op2 ^ result;

    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = ((result & 0xFF) == 0);
    this.flag5     = ((result & BIT5) != 0);
    this.flagHalf  = ((m & 0x10) != 0);
    this.flag3     = ((result & BIT3) != 0);
    this.flagPV    = ((((m >> 1) ^ m) & 0x80) != 0);
    this.flagN     = false;
    this.flagCarry = ((m & 0x100) != 0);
    this.regA      = result & 0xFF;
  }


  private void doInstSUB8( int op2, int op3 )
  {
    int result = this.regA - op2 - op3;
    int m      = this.regA ^ op2 ^ result;

    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = ((result & 0xFF) == 0);
    this.flag5     = ((result & BIT5) != 0);
    this.flagHalf  = ((m & 0x10) != 0);
    this.flag3     = ((result & BIT3) != 0);
    this.flagPV    = ((((m >> 1) ^ m) & 0x80) != 0);
    this.flagN     = true;
    this.flagCarry = ((m & 0x100) != 0);
    this.regA      = result & 0xFF;
  }


  private void doInstCP( int op2 )
  {
    int result = this.regA - op2;
    int m      = this.regA ^ op2 ^ result;

    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = ((result & 0xFF) == 0);
    this.flag5     = ((op2 & BIT5) != 0);
    this.flagHalf  = ((m & 0x10) != 0);
    this.flag3     = ((op2 & BIT3) != 0);
    this.flagPV    = ((((m >> 1) ^ m) & 0x80) != 0);
    this.flagN     = true;
    this.flagCarry = ((m & 0x100) != 0);
  }


  private int doInstINC8( int value )
  {
    // Half-Carry-Flag ermitteln
    int result    = (value & 0x0F) + 1;
    this.flagHalf = ((result & 0xFFFFFFF0) != 0);

    // eigentliche Berechnung
    result        = ((int) ((byte) value)) + 1;
    this.flagSign = ((result & BIT7) != 0);
    this.flagZero = (result == 0);
    this.flagPV   = (result != (int) ((byte) result));
    this.flagN    = false;
    this.flag5    = ((result & BIT5) != 0);
    this.flag3    = ((result & BIT3) != 0);
    return result & 0xFF;
  }


  private int doInstDEC8( int value )
  {
    // Half-Carry-Flag ermitteln
    int result    = (value & 0x0F) - 1;
    this.flagHalf = ((result & 0xFFFFFFF0) != 0);

    // eigentliche Berechnung
    result        = ((int) ((byte) value)) - 1;
    this.flagSign = ((result & BIT7) != 0);
    this.flagZero = (result == 0);
    this.flagPV   = (result != (int) ((byte) result));
    this.flagN    = true;
    this.flag5    = ((result & BIT5) != 0);
    this.flag3    = ((result & BIT3) != 0);
    return result & 0xFF;
  }


  private void doInstAND( int op2 )
  {
    this.regA      = (this.regA & op2) & 0xFF;
    this.flagSign  = ((this.regA & BIT7) != 0);
    this.flagZero  = (this.regA == 0);
    this.flagHalf  = true;
    this.flagN     = false;
    this.flagCarry = false;
    this.flag5     = ((this.regA & BIT5) != 0);
    this.flag3     = ((this.regA & BIT3) != 0);
    updParityFlag( this.regA );
  }


  private void doInstOR( int op2 )
  {
    this.regA      = (this.regA | op2) & 0xFF;
    this.flagSign  = ((this.regA & BIT7) != 0);
    this.flagZero  = (this.regA == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flagCarry = false;
    this.flag5     = ((this.regA & BIT5) != 0);
    this.flag3     = ((this.regA & BIT3) != 0);
    updParityFlag( this.regA );
  }


  private void doInstXOR( int op2 )
  {
    this.regA      = (this.regA ^ op2) & 0xFF;
    this.flagSign  = ((this.regA & BIT7) != 0);
    this.flagZero  = (this.regA == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flagCarry = false;
    this.flag5     = ((this.regA & BIT5) != 0);
    this.flag3     = ((this.regA & BIT3) != 0);
    updParityFlag( this.regA );
  }


  private void doInstDAA()
  {
    int a = this.regA;
    int h = this.regA & 0x0F;

    if( this.flagN ) {
      if( this.flagHalf || (h > 0x09) ) {
	if( h > 0x05 ) {
	  this.flagHalf = false;
	}
	a = (a - 0x06) & 0xFF;
      }
      if( this.flagCarry || (this.regA > 0x99) ) {
	a -= 0x160;
      }
    } else {
      if( this.flagHalf || (h > 0x09) ) {
	this.flagHalf = (h > 0x09);
	a += 0x06;
      }
      if( this.flagCarry || ((a & 0x1F0) > 0x90) ) {
	a += 0x60;
      }
    }
    if( (a & 0x100) != 0 ) {
      this.flagCarry = true;
    }
    this.regA     = a & 0xFF;
    this.flagSign = ((this.regA & BIT7) != 0);
    this.flagZero = (this.regA == 0);
    this.flag5    = ((this.regA & BIT5) != 0);
    this.flag3    = ((this.regA & BIT3) != 0);
    updParityFlag( this.regA );
  }


  private void doInstNEG()
  {
    int result     = (-this.regA) & 0xFF;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flag5     = ((result & BIT5) != 0);
    this.flagHalf  = ((this.regA & 0x0F) != 0);
    this.flag3     = ((result & BIT3) != 0);
    this.flagPV    = (this.regA == 0x80);
    this.flagN     = true;
    this.flagCarry = (this.regA != 0);
    this.regA      = result;
  }


  private void doInstBIT( int opCode, int value )
  {
    int mask      = getBitMask( opCode );
    this.flagZero = ((value & mask) == 0);
    this.flagSign = (mask == BIT7) && !this.flagZero;
    this.flagHalf = true;
    this.flagPV   = this.flagZero;
    this.flagN    = false;

    if( (opCode & 0x07) == 0x06 ) {
      // bei Test einer Speicherzelle
      this.flag5 = false;
      this.flag3 = false;
    } else {
      // bei Test eines Registers
      this.flag5 = ((value & BIT5) != 0);
      this.flag3 = ((value & BIT3) != 0);
    }
  }


  private int doInstRL( int value )
  {
    int b          = this.flagCarry ? BIT0 : 0;
    this.flagCarry = ((value & BIT7) != 0);
    int result     = ((value << 1) | b) & 0xFF;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flag5     = ((result & BIT5) != 0);
    this.flag3     = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstRLC( int value )
  {
    this.flagCarry = ((value & BIT7) != 0);
    int result     = (value << 1) & 0xFF;
    if( this.flagCarry ) {
      result |= BIT0;
    }
    this.flagSign = ((result & BIT7) != 0);
    this.flagZero = (result == 0);
    this.flagHalf = false;
    this.flagN    = false;
    this.flag5    = ((result & BIT5) != 0);
    this.flag3    = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstRR( int value )
  {
    int b          = this.flagCarry ? BIT7 : 0;
    this.flagCarry = ((value & BIT0) != 0);
    int result     = (value >> 1) | b;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flag5     = ((result & BIT5) != 0);
    this.flag3     = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstRRC( int value )
  {
    this.flagCarry = ((value & BIT0) != 0);
    int result     = value >> 1;
    if( this.flagCarry ) {
      result |= BIT7;
    }
    this.flagSign = ((result & BIT7) != 0);
    this.flagZero = (result == 0);
    this.flagHalf = false;
    this.flagN    = false;
    this.flag5    = ((result & BIT5) != 0);
    this.flag3    = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstSLA( int value )
  {
    this.flagCarry = ((value & BIT7) != 0);
    int result     = (value << 1) & 0xFF;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flag5     = ((result & BIT5) != 0);
    this.flag3     = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstSLL( int value )
  {
    this.flagCarry = ((value & BIT7) != 0);
    int result     = ((value << 1) | BIT0) & 0xFF;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flag5     = ((result & BIT5) != 0);
    this.flag3     = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstSRA( int value )
  {
    this.flagCarry = ((value & BIT0) != 0);
    int b          = value & BIT7;
    int result     = (value >> 1) | b;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flag5     = ((result & BIT5) != 0);
    this.flag3     = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstSRL( int value )
  {
    this.flagCarry = ((value & BIT0) != 0);
    int result     = value >> 1;
    this.flagSign  = ((result & BIT7) != 0);
    this.flagZero  = (result == 0);
    this.flagHalf  = false;
    this.flagN     = false;
    this.flag5     = ((result & BIT5) != 0);
    this.flag3     = ((result & BIT3) != 0);
    updParityFlag( result );
    return result;
  }


  private int doInstADD16( int op1, int op2 )
  {
    // Carry-Flag ermitteln
    int result     = (op1 & 0xFFFF) + (op2 & 0xFFFF);
    this.flagCarry = ((result & 0xFFFF0000) != 0);

    // Half-Carry-Flag ermitteln
    result        = (op1 & 0x0FFF) + (op2 & 0x0FFF);
    this.flagHalf = ((result & 0xFFFFF000) != 0);

    // eigentliche Berechnung
    result     = (int) ((short) op1) + (int) ((short) op2);
    this.flagN = false;
    this.flag5 = ((result & 0x2000) != 0);
    this.flag3 = ((result & 0x0800) != 0);
    return result & 0xFFFF;
  }


  private void doInstADC16( int op2 )
  {
    int op3   = this.flagCarry ? 1 : 0;
    int regHL = getRegHL();

    // Carry-Flag ermitteln
    int result     = (regHL & 0xFFFF) + (op2 & 0xFFFF) + op3;
    this.flagCarry = ((result & 0xFFFF0000) != 0);

    // Half-Carry-Flag ermitteln
    result        = (regHL & 0x0FFF) + (op2 & 0x0FFF) + op3;
    this.flagHalf = ((result & 0xFFFFF000) != 0);

    // eigentliche Berechnung
    result        = (int) ((short) regHL) + (int) ((short) op2) + op3;
    this.flagSign = ((result & 0x8000) != 0);
    this.flagZero = (result == 0);
    this.flagPV   = (result != (int) ((short) result));
    this.flagN    = false;
    this.flag5    = ((result & 0x2000) != 0);
    this.flag3    = ((result & 0x0800) != 0);
    setRegHL( result );
  }


  private void doInstSBC16( int op2 )
  {
    int op3   = this.flagCarry ? 1 : 0;
    int regHL = getRegHL();

    // Carry-Flag ermitteln
    int result     = (regHL & 0xFFFF) - (op2 & 0xFFFF) - op3;
    this.flagCarry = ((result & 0xFFFF0000) != 0);

    // Half-Carry-Flag ermitteln
    result        = (regHL & 0x0FFF) - (op2 & 0x0FFF) - op3;
    this.flagHalf = ((result & 0xFFFFF000) != 0);

    // eigentliche Berechnung
    result        = (int) ((short) regHL) - (int) ((short) op2) - op3;
    this.flagSign = ((result & 0x8000) != 0);
    this.flagZero = (result == 0);
    this.flagPV   = (result != (int) ((short) result));
    this.flagN    = true;
    this.flag5    = ((result & 0x2000) != 0);
    this.flag3    = ((result & 0x0800) != 0);
    setRegHL( result );
  }


  /*
   * Diese Methode fuehrt einen Zyklus der Befehle
   * LDI, LDD, LDIR und LDDR aus.
   */
  private void doInstBlockLD( int addValue )
  {
    int regBC = getRegBC();
    int regDE = getRegDE();
    int regHL = getRegHL();

    int b = readMemByte( regHL );
    writeMemByte( regDE, b );

    setRegDE( regDE + addValue );
    setRegHL( regHL + addValue );
    --regBC;
    setRegBC( regBC );
    this.flagPV   = (regBC != 0);
    this.flagHalf = false;
    this.flagN    = false;

    // undokumentierte Flagbeeinflussung
    b += this.regA;
    this.flag5 = ((b & BIT1) != 0);
    this.flag3 = ((b & BIT3) != 0);
  }


  /*
   * Diese Methode fuehrt einen Zyklus der Befehle
   * CPI, CPD, CPIR und CPDR aus.
   */
  private void doInstBlockCP( int addValue )
  {
    int regBC = getRegBC();
    int regHL = getRegHL();
    int m     = readMemByte( regHL );

    // Half-Carry-Flag ermitteln
    int result    = (this.regA & 0x0F) - (m & 0x0F);
    this.flagHalf = ((result & 0xFFFFFFF0) != 0);

    // eigentlicher Vergleich
    result        = (int) ((byte) this.regA) - (int) ((byte) m);
    this.flagSign = ((result & BIT7) != 0);
    this.flagZero = (result == 0);
    this.flagN    = true;

    // sonstiges
    setRegHL( regHL + addValue );
    --regBC;
    setRegBC( regBC );
    this.flagPV = (regBC != 0);

    // undokumentierte Flagbeeinflussung
    if( this.flagHalf ) {
      --result;
    }
    this.flag5 = ((result & BIT1) != 0);
    this.flag3 = ((result & BIT3) != 0);
  }


  /*
   * Diese Methode fuehrt einen Zyklus der Befehle
   * INI, IND, INIR und INDR aus.
   */
  private void doInstBlockIN( int addValue )
  {
    int regHL = getRegHL();
    writeMemByte(
	regHL,
	this.ioSys != null ?
		this.ioSys.readIOByte( (this.regB << 8) | this.regC )
		: 0 );
    setRegHL( regHL + 1 );
    this.regB     = (this.regB - 1) & 0xFF;
    this.flagSign = ((this.regB & BIT7) != 0);
    this.flagZero = (this.regB == 0);
    this.flagN    = true;
    this.flag5    = ((this.regB & BIT5) != 0);
    this.flag3    = ((this.regB & BIT3) != 0);
  }


  /*
   * Diese Methode fuehrt einen Zyklus der Befehle
   * OUTI, OUTD, OTIR und OTDR aus.
   */
  private void doInstBlockOUT( int addValue )
  {
    int regHL = getRegHL();
    if( this.ioSys != null ) {
      this.ioSys.writeIOByte(
		(this.regB << 8) | this.regC,
		readMemByte( regHL ) );
    }
    setRegHL( regHL + 1 );
    this.regB     = (this.regB - 1) & 0xFF;
    this.flagSign = ((this.regB & BIT7) != 0);
    this.flagZero = (this.regB == 0);
    this.flagN    = true;
    this.flag5    = ((this.regB & BIT5) != 0);
    this.flag3    = ((this.regB & BIT3) != 0);
  }


  private int doInstIN( int port )
  {
    int value = (this.ioSys != null ?
			this.ioSys.readIOByte( (this.regB << 8) | this.regC )
			: 0) & 0xFF;
    this.flagSign = ((value & BIT7) != 0);
    this.flagZero = (value == 0);
    this.flagHalf = false;
    this.flagN    = false;
    this.flag5    = ((value & BIT5) != 0);
    this.flag3    = ((value & BIT3) != 0);
    updParityFlag( value );
    return value;
  }


  private void doInstRETN()
  {
    this.regPC = doPop();
    this.iff1  = this.iff2;
  }


	/* --- allgemeine Hilfsmethoden --- */

  private void setPCRel( int d )
  {
    if( this.codeBuf != null ) {
      this.codeBufPos += d;
    } else {
      this.regPC = ((this.regPC + d) & 0xFFFF);
    }
  }


  private int computeRelAddr( int baseAddr, int d )
  {
    return (baseAddr + (int) ((byte) d)) & 0xFFFF;
  }


  private void doJmpRel( int d )
  {
    this.regPC = computeRelAddr( this.regPC, d );
  }


  private int getBitMask( int opCode )
  {
    return 1 << ((opCode >> 3) & 0x07);
  }


  private int getSrcValue( int preCode, int opCode )
  {
    switch( opCode & 0x07 ) {
      case 7:
	return this.regA;
      case 0:
	return this.regB;
      case 1:
	return this.regC;
      case 2:
	return this.regD;
      case 3:
	return this.regE;
      case 4:
	if( preCode == 0xDD ) {
	  return (this.regIX >> 8) & 0xFF;
	}
	if( preCode == 0xFD ) {
	  return (this.regIY >> 8) & 0xFF;
	}
	return this.regH;
      case 5:
	if( preCode == 0xDD ) {
	  return this.regIX & 0xFF;
	}
	if( preCode == 0xFD ) {
	  return this.regIY & 0xFF;
	}
	return this.regL;
      case 6:
	{
	  int addr = 0;
	  if( preCode == 0xDD ) {
	    addr = computeRelAddr( this.regIX, nextByte() );
	  } else if( preCode == 0xFD ) {
	    addr = computeRelAddr( this.regIY, nextByte() );
	  } else {
	    addr = getRegHL();
	  }
	  return readMemByte( addr );
	}
    }
    throwIllegalState( opCode );
    return -1;
  }


  private void incRegR()
  {
    this.regR_bits0to6 = (this.regR_bits0to6 + 1) & 0x7F;
  }


  private int nextByte()
  {
    if( this.codeBuf != null ) {
      return this.codeBuf[ this.codeBufPos++ ];
    }
    int rv     = readMemByte( this.regPC );
    this.regPC = (this.regPC + 1) & 0xFFFF;
    return rv;
  }


  private int nextWord()
  {
    int lowByte  = nextByte();
    int highByte = nextByte();
    return (highByte << 8) | lowByte;
  }


  private void updParityFlag( int value )
  {
    this.flagPV = this.parity[ value & 0xFF ];
  }


  private boolean debugHasBreakpointReached()
  {
    boolean         rv          = false;
    Z80Breakpoint[] breakpoints = this.debugBreakpoints;
    if( breakpoints != null ) {
      for( int i = 0; i < breakpoints.length; i++ ) {
	Z80Breakpoint bp = breakpoints[ i ];
	if( bp != null ) {
	  if( bp.isEnabled() && (bp.getAddress() == this.regPC) ) {
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  private boolean debugHasInstRETXReached()
  {
    int opCode = this.memory.getMemByte( this.regPC );
    if( this.codeBuf != null ) {
      if( (this.codeBufPos >= 0) && (this.codeBufPos < this.codeBuf.length) )
	opCode = this.codeBuf[ this.codeBufPos ];
    }
    if( (opCode == 0xC9)				// RET
	|| ((opCode == 0xC0) && !this.flagZero)		// RET NZ
	|| ((opCode == 0xC8) && this.flagZero)		// RET Z
	|| ((opCode == 0xD0) && !this.flagCarry)	// RET NC
	|| ((opCode == 0xD8) && this.flagCarry)		// RET C
	|| ((opCode == 0xE0) && !this.flagPV)		// RET PO
	|| ((opCode == 0xE8) && this.flagPV)		// RET PE
	|| ((opCode == 0xF0) && !this.flagSign)		// RET P
	|| ((opCode == 0xF8) && this.flagSign) )	// RET M
    {
      return true;
    }

    if( opCode == 0xED ) {
      opCode = this.memory.getMemByte( this.regPC + 1 );
      if( this.codeBuf != null ) {
	if( (this.codeBufPos >= 0)
	    && (this.codeBufPos < this.codeBuf.length - 1) )
	{
	  opCode = this.codeBuf[ this.codeBufPos + 1 ];
	}
      }
      if( (opCode == 0x4D)		// RETI
	  || (opCode == 0x45)		// RETN
	  || (opCode == 0x55)		// RETN
	  || (opCode == 0x5D)		// RETN
	  || (opCode == 0x65)		// RETN
	  || (opCode == 0x6D)		// RETN
	  || (opCode == 0x75)		// RETN
	  || (opCode == 0x7D) )		// RETN
      {
	return true;
      }
    }
    return false;
  }


  private void doDebugTrace()
  {
    PrintWriter debugTracer = this.debugTracer;
    if( debugTracer != null ) {

      // Register ausgeben
      debugTracer.print( "AF:" );
      debugTracer.printf( "%04X", getRegAF() );
      debugTracer.print( " [" );
      debugTracer.print( this.flagSign	? "S" : "." );
      debugTracer.print( this.flagZero	? "Z" : "." );
      debugTracer.print( this.flag5	? "1" : "." );
      debugTracer.print( this.flagHalf	? "H" : "." );
      debugTracer.print( this.flag3	? "1" : "." );
      debugTracer.print( this.flagPV	? "P" : "." );
      debugTracer.print( this.flagN	? "N" : "." );
      debugTracer.print( this.flagCarry	? "C" : "." );
      debugTracer.print( "] BC:" );
      debugTracer.printf( "%04X", getRegBC() );
      debugTracer.print( " DE:" );
      debugTracer.printf( "%04X", getRegDE() );
      debugTracer.print( " HL:" );
      debugTracer.printf( "%04X", getRegHL() );
      debugTracer.print( " IX:" );
      debugTracer.printf( "%04X", this.regIX );
      debugTracer.print( " IY:" );
      debugTracer.printf( "%04X", this.regIY );
      debugTracer.print( " SP:" );
      debugTracer.printf( "%04X", this.regSP );
      debugTracer.print( "  " );

      // Adresse ausgeben
      int addr = this.regPC;
      debugTracer.printf( "%04X", addr );
      debugTracer.print( "  " );

      // Befehl reassemblieren
      Z80ReassInstr instr = Z80Reassembler.reassInstruction(
					addr,
					this.memory.getMemByte( addr ),
					this.memory.getMemByte( addr + 1 ),
					this.memory.getMemByte( addr + 2 ),
					this.memory.getMemByte( addr + 3 ) );
      if( instr != null ) {
	// Befehlscode ausgeben
	int w = 12;
	int n = instr.getLength();
	for( int i = 0; i < n; i++ ) {
	  debugTracer.printf( "%02X", this.memory.getMemByte( addr ) );

	  debugTracer.print( (char) '\u0020' );
	  addr++;
	  w -= 3;
	}
	while( w > 0 ) {
	  debugTracer.print( (char) '\u0020' );
	  --w;
	}

	// Assembler-Befehlsname ausgeben
	String s = instr.getName();
	if( s != null ) {
	  debugTracer.print( s );

	  // Argument ausgeben
	  w = 8 - s.length();
	  s = instr.getArg1();
	  if( s != null ) {
	    while( w > 0 ) {
	      debugTracer.print( (char) '\u0020' );
	      --w;
	    }
	    debugTracer.print( s );

	    s = instr.getArg2();
	    if( s != null ) {
	      debugTracer.print( (char) ',' );
	      debugTracer.print( s );
	    }
	  }
	}

      } else {

	debugTracer.printf( "%02X", this.memory.getMemByte( addr ) );
      }
      debugTracer.println();
    }
  }


  private void fireAddressChanged( int addr )
  {
    // wegen Thread-Sicherheit in lokale Variable laden
    Z80AddressListener addrListener = this.addrListener;
    if( addrListener != null )
      addrListener.z80AddressChanged( addr );
  }


  private int readMemByte( int addr )
  {
    int value = this.memory.readMemByte( addr );
    fireAddressChanged( addr );
    return value;
  }


  private int readMemWord( int addr )
  {
    int value = (this.memory.readMemByte( addr + 1 ) << 8)
					| this.memory.readMemByte( addr );
    fireAddressChanged( addr );
    return value;
  }


  private void writeMemByte( int addr, int value )
  {
    this.memory.writeMemByte( addr, value );
    fireAddressChanged( addr );
  }


  private void writeMemWord( int addr, int value )
  {
    this.memory.writeMemByte( addr, value & 0xFF );
    this.memory.writeMemByte( addr + 1, value >> 8 );
    fireAddressChanged( addr );
  }


  private void setHaltState( boolean state )
  {
    if( state != this.haltState ) {
      this.haltState = state;
      synchronized( this.haltStateListeners ) {
	for( Z80HaltStateListener listener : this.haltStateListeners ) {
	  listener.z80HaltStateChanged( this, state );
	}
      }
    }
  }


  private void throwIllegalState( int opCode )
  {
    throw new IllegalStateException(
	String.format( "%02X: Operationscode nicht erwartet", opCode ) );
  }


  private void updStatusListeners()
  {
    synchronized( this.statusListeners ) {
      for( Z80StatusListener listener : this.statusListeners )
	listener.z80StatusChanged();
    }
  }
}

