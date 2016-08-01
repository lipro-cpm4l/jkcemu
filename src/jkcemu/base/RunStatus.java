/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Status einer interaktiven Aktion
 */

package jkcemu.base;

import java.lang.*;


public class RunStatus
{
  private boolean canceled;
  private boolean failed;
  private boolean force;
  private boolean quiet;


  public RunStatus()
  {
    this.canceled = false;
    this.failed   = false;
    this.force    = false;
    this.quiet    = false;
  }


  public boolean getForce()
  {
    return this.force;
  }


  public boolean isQuiet()
  {
    return this.quiet;
  }


  public void setCanceled( boolean state )
  {
    this.canceled = state;
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


  public boolean wasCanceled()
  {
    return this.canceled;
  }


  public boolean wasFailed()
  {
    return this.failed;
  }
}

