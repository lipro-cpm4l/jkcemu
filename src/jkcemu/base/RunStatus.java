/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Status einer interaktiven Aktion
 */

package jkcemu.base;

import java.lang.*;


public class RunStatus
{
  private boolean cancelled;
  private boolean failed;
  private boolean force;
  private boolean quiet;


  public RunStatus()
  {
    this.cancelled = false;
    this.failed    = false;
    this.force     = false;
    this.quiet     = false;
  }


  public boolean getForce()
  {
    return this.force;
  }


  public boolean isQuiet()
  {
    return this.quiet;
  }


  public void setCancelled( boolean state )
  {
    this.cancelled = state;
  }


  public void setQuiet( boolean state )
  {
    this.quiet = state;
  }


  public void setFailed( boolean state )
  {
    this.failed = state;
  }


  public void setForce( boolean state )
  {
    this.force = state;
  }


  public boolean wasCancelled()
  {
    return this.cancelled;
  }


  public boolean wasFailed()
  {
    return this.failed;
  }
}

