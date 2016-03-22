/*
 * (c) 2008-2013 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer Haltepunkte
 */

package z80emu;


public interface Z80Breakpoint
{
  /*
   * Die Methode wird vor Ausfuehrung eines jeden Befehls aufgerufen.
   * Liefert Sie true zurueck, haelt die Programmausfuehrung an
   * (Haltepunkt erreicht).
   *
   * Eine Interruptquelle wird nur im Fall eines gerade angenommenen
   * Interrupts uebergeben.
   * Der Program Counter ist zu diesem Zeitpunkt bereits gekellert
   * und auf den Anfang der Interrupt-Serviceroutine gesetzt.
   */
  public boolean matches( Z80CPU cpu, Z80InterruptSource iSource );
}
