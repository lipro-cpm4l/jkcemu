/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface fuer das Empfangen von Reset-Ereignissen
 */

package jkcemu.base;

import java.lang.*;


public interface ResetListener
{
  public void resetFired();
}

