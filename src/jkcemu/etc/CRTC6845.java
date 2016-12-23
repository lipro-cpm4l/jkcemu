/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des CRTC Controllers 6845 (Typ 0)
 */

package jkcemu.etc;

import java.lang.*;
import z80emu.Z80CPU;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80TStatesListener;


public class CRTC6845 implements Z80MaxSpeedListener, Z80TStatesListener
{
  public interface SyncListener
  {
    public void crtcHSyncBegin( int totalLine, int charLine, int lineBegAddr );
    public void crtcHSyncEnd();
    public void crtcVSyncBegin();
    public void crtcVSyncEnd();
  };


  private enum CursorMode {
			BLINK_FAST,
			BLINK_SLOW,
			BLINK_NONE,
			INVISIBLE };

  private SyncListener syncListener;
  private Z80CPU       cpu;
  private int          khz;
  private int          regNum;
  private int          startAddr;
  private int          crsAddr;
  private int          crsRasterStart;
  private int          crsRasterEnd;
  private int          crsBlinkMode;
  private CursorMode   crsMode;
  private int          hCharTotal;
  private int          hCharVisible;
  private int          hCharSyncPos;
  private int          hCharSyncWidth;
  private int          hSyncWidthCounter;
  private int          vCharTotal;
  private int          vCharVisible;
  private int          vCharSyncPos;
  private int          vCharLines;
  private int          vCharCounter;
  private int          vLineCounter;
  private int          totalLineCounter;
  private int          lineTStatesCounter;
  private int          tStatesHSyncPos;
  private int          tStatesHSyncWidth;
  private int          tStatesLineWidth;
  private int          curAddr;
  private boolean      vSync;


  public CRTC6845( int khz, SyncListener syncListener )
  {
    this.khz          = (khz > 1 ? khz : 1);
    this.syncListener = syncListener;
    this.cpu          = null;
    reset();
  }


  public boolean isVSync()
  {
    return this.vSync;
  }


  public int read()
  {
    int rv = 0;
    switch( this.regNum ) {
      case 12:
	rv = (this.startAddr >> 8) & 0x3F;
	break;

      case 13:
	rv = this.startAddr & 0xFF;
	break;

      case 14:
	rv = (this.crsAddr >> 8) & 0x3F;
	break;

      case 15:
	rv = this.crsAddr & 0xFF;
	break;
    }
    return rv;
  }


  private void reset()
  {
    this.regNum             = 0;
    this.startAddr          = 0;
    this.crsAddr            = 0;
    this.crsRasterStart     = 0;
    this.crsRasterEnd       = 0;
    this.crsBlinkMode       = 0;
    this.crsMode            = CursorMode.INVISIBLE;
    this.hCharTotal         = 0;
    this.hCharVisible       = 0;
    this.hCharSyncPos       = 0;
    this.hCharSyncWidth     = 0;
    this.hSyncWidthCounter  = 0;
    this.vCharTotal         = 0;
    this.vCharVisible       = 0;
    this.vCharSyncPos       = 0;
    this.vCharLines         = 0;
    this.vCharCounter       = 0;
    this.vLineCounter       = 0;
    this.totalLineCounter   = 0;
    this.lineTStatesCounter = 0;
    this.tStatesHSyncPos    = 0;
    this.tStatesHSyncWidth  = 0;
    this.tStatesLineWidth   = 0;
    this.curAddr            = 0;
  }


  public int getHCharVisible()
  {
    return this.hCharVisible;
  }


  public int getStartAddr()
  {
    return this.startAddr;
  }


  public void setRegNum( int regNum )
  {
    this.regNum = regNum & 0x1F;
  }


  public void write( int value )
  {
    switch( this.regNum ) {
      case 0:
	this.hCharTotal = value & 0xFF;
	calcTStates();
	break;

      case 1:
	this.hCharVisible = value & 0xFF;
	break;

      case 2:
	this.hCharSyncPos = value & 0xFF;
	calcTStates();
	break;

      case 3:
	this.hCharSyncWidth = value & 0x0F;
	calcTStates();
	break;

      case 4:
	this.vCharTotal = value & 0x7F;
	break;

      case 6:
	this.vCharVisible = value & 0x7F;
	break;

      case 7:
	this.vCharSyncPos = value & 0x7F;
	break;

      case 9:
	this.vCharLines = value & 0x1F;
	break;

      case 10:
	this.crsRasterStart = value & 0x1F;
	switch( value & 0x60 ) {
	  case 0x60:
	    this.crsMode = CursorMode.BLINK_FAST;
	    break;
	  case 0x40:
	    this.crsMode = CursorMode.BLINK_SLOW;
	    break;
	  case 0x20:
	    this.crsMode = CursorMode.INVISIBLE;
	    break;
	  case 0x00:
	    this.crsMode = CursorMode.BLINK_NONE;
	    break;
	}
	break;

      case 11:
	this.crsRasterEnd = value & 0x1F;
	break;

      case 12:
	this.startAddr = ((value << 8) & 0xFF00) | (this.startAddr & 0x00FF);
	break;

      case 13:
	this.startAddr = (this.startAddr & 0x3F00) | (value & 0x00FF);
	break;

      case 14:
	this.crsAddr = ((value << 8) & 0x3F00) | (this.crsAddr & 0x00FF);
	break;

      case 15:
	this.crsAddr = (this.crsAddr & 0x3F00) | (value & 0x00FF);
	break;
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.cpu = cpu;
    calcTStates();
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( (this.lineTStatesCounter < this.tStatesHSyncPos)
	&& ((this.lineTStatesCounter + tStates) >= this.tStatesHSyncPos) )
    {
      this.hSyncWidthCounter = this.tStatesHSyncWidth
					+ this.tStatesHSyncPos
					- this.lineTStatesCounter
					- tStates;
      this.syncListener.crtcHSyncBegin(
				this.totalLineCounter,
				this.vLineCounter,
				this.curAddr );
    } else {
      if( this.hSyncWidthCounter > 0 ) {
	this.hSyncWidthCounter -= tStates;
	if( this.hSyncWidthCounter <= 0 ) {
	  this.syncListener.crtcHSyncEnd();
	}
      }
    }
    this.lineTStatesCounter += tStates;
    if( this.lineTStatesCounter >= this.tStatesLineWidth ) {
      this.lineTStatesCounter -= this.tStatesLineWidth;
      this.totalLineCounter++;
      if( this.vLineCounter < this.vCharLines ) {
	this.vLineCounter++;
      } else {
	this.vLineCounter = 0;
	if( this.vCharCounter < this.vCharTotal ) {
	  this.vCharCounter++;
	  this.curAddr += this.hCharVisible;
	} else {
	  this.vCharCounter = 0;
	  this.curAddr      = this.startAddr;
	}
	if( this.vCharCounter == this.vCharSyncPos ) {
	  this.vSync = true;
	  this.syncListener.crtcVSyncBegin();
	}
	if( this.vCharCounter == 0 ) {
	  this.vSync = false;
	  this.syncListener.crtcVSyncEnd();
	  this.totalLineCounter = 0;
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void calcTStates()
  {
    int cpuKHz              = cpu.getMaxSpeedKHz();
    this.tStatesHSyncPos    = this.hCharSyncPos * cpuKHz / this.khz;
    this.tStatesHSyncWidth  = this.hCharSyncWidth * cpuKHz / this.khz;
    this.tStatesLineWidth   = this.hCharTotal * cpuKHz / this.khz;
    this.lineTStatesCounter = 0;
  }
}
