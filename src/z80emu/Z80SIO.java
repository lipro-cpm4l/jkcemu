/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z80-Emulator
 *
 * rudimentaere Emulation der Z80 SIO
 */

package z80emu;

import java.lang.*;
import java.util.*;


public class Z80SIO implements Z80InterruptSource
{
  private Channel channelA;
  private Channel channelB;


  public Z80SIO()
  {
    this.channelA = new Channel( 0 );
    this.channelB = new Channel( 1 );
  }


  public void addChannelListener(
			Z80SIOChannelListener listener,
			int                   channel )
  {
    switch( channel ) {
      case 0:
	addListener( this.channelA, listener );
	break;

      case 1:
	addListener( this.channelA, listener );
	break;
    }
  }


  public void removeChannelListener(
			Z80SIOChannelListener listener,
			int                   channel )
  {
    switch( channel ) {
      case 0:
	removeListener( this.channelA, listener );
	break;

      case 1:
	removeListener( this.channelA, listener );
	break;
    }
  }


  public int readControlA()
  {
    return readControl( this.channelA );
  }


  public int readControlB()
  {
    return readControl( this.channelA );
  }


  public int readDataA()
  {
    return 0;
  }


  public int readDataB()
  {
    return 0;
  }


  public void writeControlA( int value )
  {
    writeControl( this.channelA, value );
  }


  public void writeControlB( int value )
  {
    writeControl( this.channelB, value );
  }


  public void writeDataA( int value )
  {
    fireByteAvailable( this.channelA, value );
  }


  public void writeDataB( int value )
  {
    fireByteAvailable( this.channelB, value );
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  public synchronized int interruptAccepted()
  {
    return 0;
  }


  public synchronized void interruptFinished()
  {
    // leer
  }


  public boolean isInterruptPending()
  {
    return false;
  }


  public boolean isInterruptRequested()
  {
    return false;
  }


  public void reset()
  {
    this.channelA.reset();
    this.channelB.reset();
  }


	/* --- private Methoden --- */

  private class Channel
  {
    public int                               channelNum;
    public int                               regNum;
    public Collection<Z80SIOChannelListener> listeners;


    public Channel( int channelNum )
    {
      this.channelNum = channelNum;
      this.regNum     = 0;
      this.listeners  = null;
      reset();
    }

    public void reset()
    {
      // leer
    }
  }


  private static synchronized void addListener(
				Channel               channel,
				Z80SIOChannelListener listener )
  {
    if( channel.listeners == null ) {
      channel.listeners = new ArrayList<Z80SIOChannelListener>();
    }
    channel.listeners.add( listener );
  }


  private static synchronized void removeListener(
				Channel               channel,
				Z80SIOChannelListener listener )
  {
    if( channel.listeners != null )
      channel.listeners.remove( listener );
  }


  private synchronized void fireByteAvailable( Channel channel, int value )
  {
    if( channel.listeners != null ) {
      for( Z80SIOChannelListener listener : channel.listeners ) {
	listener.z80SIOChannelByteAvailable( this, channel.channelNum, value );
      }
    }
  }


  private static int readControl( Channel channel )
  {
    // Bei Leseregister 0: Sendebereitschaft und Sendepuffer leer zurueckgeben
    return channel.regNum == 0 ? 0x24 : 0;
  }


  private static void writeControl( Channel channel, int value )
  {
    if( channel.regNum == 0 ) {
      channel.regNum = value & 0x07;
    } else {
      channel.regNum = 0;
    }
  }
}

