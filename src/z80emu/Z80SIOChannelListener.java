/*
 * (c) 2009-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von Ereignissen,
 * wenn sich der IO-Zustand eines SIO-Kanals geaendert hat.
 */

package z80emu;


public interface Z80SIOChannelListener
{
  public void z80SIOByteSent( Z80SIO sio, int channel, int value );
}

