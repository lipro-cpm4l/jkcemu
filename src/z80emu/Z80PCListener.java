/*
 * (c) 2009-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von Ereignissen,
 * wenn der PC einen bestimmten Wert erreicht hat
 */

package z80emu;


public interface Z80PCListener
{
  public void z80PCChanged( Z80CPU cpu, int addr );
}

