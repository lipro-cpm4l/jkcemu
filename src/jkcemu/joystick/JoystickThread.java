/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Thread zur Abfrage eines Joysticks
 */

package jkcemu.joystick;

import jkcemu.Main;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;


public class JoystickThread extends Thread
{
  public static final int LEFT_MASK    = 0x01;
  public static final int RIGHT_MASK   = 0x02;
  public static final int UP_MASK      = 0x04;
  public static final int DOWN_MASK    = 0x08;
  public static final int BUTTON1_MASK = 0x10;
  public static final int BUTTON2_MASK = 0x20;
  public static final int BUTTONS_MASK = (BUTTON1_MASK | BUTTON2_MASK);

  private EmuThread        emuThread;
  private int              joyNum;
  private boolean          interactive;
  private volatile boolean running;


  public JoystickThread(
		EmuThread emuThread,
		int       joyNum,
		boolean   interactive )
  {
    super(
	Main.getThreadGroup(),
	String.format( "JKCEMU joystick %d listener", joyNum ) );
    this.emuThread   = emuThread;
    this.joyNum      = joyNum;
    this.interactive = interactive;
    this.running     = true;
  }


  public void fireStop()
  {
    this.running = false;
    interrupt();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    DeviceIO.Joystick js = null;
    try {
      if( this.interactive ) {
	/*
	 * Beim manuellen Verbinden auch alternativen Joystick zulassen,
	 * sodass bei nur einem angeschlossenen Joystick auch
	 * der zweite emulierte Spielhebel bedient werden kann.
	 */
	int physJoyNum = this.joyNum + 1;
	while( physJoyNum > 0 ) {
	  --physJoyNum;
	  js = DeviceIO.openJoystick( physJoyNum );
	}
      } else {
	js = DeviceIO.openJoystick( this.joyNum );
      }
      if( js != null ) {
	while( this.running ) {
	  if( Main.isEmuWindowActive() && js.waitForEvent() ) {
	    int   m = 0;
	    float x = js.getXAxis();
	    float y = js.getYAxis();
	    int   b = js.getPressedButtons();
	    if( x < -0.5F ) {
	      m |= LEFT_MASK;
	    } else if( x > 0.5F ) {
	      m |= RIGHT_MASK;
	    }
	    if( y < -0.5F ) {
	      m |= UP_MASK;
	    } else if( y > 0.5F ) {
	      m |= DOWN_MASK;
	    }
	    if( (b & 0x5555) != 0 ) {	// Bit 0, 2, 4, ...
	      m |= BUTTON1_MASK;
	    }
	    if( (b & 0xAAAA) != 0 ) {	// Bit 1, 3, 5, ...
	      m |= BUTTON2_MASK;
	    }
	    this.emuThread.setJoystickAction( this.joyNum, m );
	  } else {
	    this.emuThread.setJoystickAction( this.joyNum, 0 );
	    try {
	      Thread.sleep( 50 );
	    }
	    catch( InterruptedException ex ) {}
	  }
	}
      } else {
	if( this.interactive ) {
	  this.emuThread.fireShowJoystickError(
		"Joystick nicht angeschlossen oder nicht unterst\u00FCtzt" );
	}
      }
    }
    finally {
      this.emuThread.setJoystickAction( this.joyNum, 0 );
      EmuUtil.closeSilently( js );
    }
    this.emuThread.joystickThreadTerminated( this );
  }
}
