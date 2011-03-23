/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse zur Anzeige einer Fehlermeldung aus einem Nicht-Swing-Thread
 */

package jkcemu.base;

import java.awt.Component;
import java.lang.*;


public class ErrorMsg implements Runnable
{
  private Component parent;
  private Exception ex;


  public ErrorMsg( Component parent, Exception ex )
  {
    this.parent = parent;
    this.ex     = ex;
  }


	/* --- Methoden fuer Runnable --- */

  @Override
  public void run()
  {
    EmuUtil.exitSysError( this.parent, null, this.ex );
  }
}

