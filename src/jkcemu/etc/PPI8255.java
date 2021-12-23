/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Programmable Peripheral Interface 8255
 */

package jkcemu.etc;


public class PPI8255
{
  public interface Callback
  {
    public int  ppiReadPort( PPI8255 ppi, int port );
    public void ppiWritePort(
			PPI8255 ppi,
			int     port,
			int     value,
			boolean strobe );
  };


  public static final int PORT_A = 0;
  public static final int PORT_B = 1;
  public static final int PORT_C = 2;

  private static final int ACK_A_MASK = 0x40;
  private static final int ACK_B_MASK = 0x04;

  private static final int IBF_A_MASK = 0x20;
  private static final int IBF_B_MASK = 0x02;

  private static final int INTR_A_MASK = 0x08;
  private static final int INTR_B_MASK = 0x01;

  private static final int OBF_A_MASK = 0x80;
  private static final int OBF_B_MASK = 0x02;

  private static final int STB_A_MASK = 0x10;
  private static final int STB_B_MASK = 0x04;

  private static final int INTE_A_IN_MASK = 0x10;
  private static final int INTE_B_IN_MASK = 0x04;

  private static final int INTE_A_OUT_MASK = 0x40;
  private static final int INTE_B_OUT_MASK = 0x04;

  private Callback callback;
  private int      modeA;
  private int      valueA;
  private int      valueB;
  private int      valueC;
  private boolean  modeB1;
  private boolean  dirAin;
  private boolean  dirBin;
  private boolean  dirCHin;
  private boolean  dirCLin;


  public PPI8255( Callback callback )
  {
    this.callback = callback;
    reset();
  }


  public int fetchOutValueA( boolean ack )
  {
    if( ack ) {
      this.valueC &= ~ACK_A_MASK;
      setBitsC( OBF_A_MASK, true );
    }
    return this.valueA;
  }


  public int fetchOutValueB( boolean ack )
  {
    if( ack ) {
      this.valueC &= ~ACK_B_MASK;
      setBitsC( OBF_B_MASK, true );
    }
    return this.valueB;
  }


  public int fetchOutValueC()
  {
    return this.valueC;
  }


  public int read( int port, int defaultValue )
  {
    int rv = defaultValue;
    switch( port & 0x03 ) {
      case 0:						// Port A
	if( (this.modeA == 0) && this.dirAin ) {
	  if( this.callback != null ) {
	    rv = this.callback.ppiReadPort( this, PORT_A );
	  }
	} else if( ((this.modeA == 1) && this.dirAin) || (this.modeA == 2) ) {
	  rv = this.valueA;
	  this.valueC &= ~INTR_A_MASK;
	}
	break;

      case 1:						// Port B
	if( this.dirBin ) {
	  if( this.modeB1 ) {
	    rv = this.valueB;
	    this.valueC &= ~INTR_B_MASK;
	  } else {
	    if( this.callback != null ) {
	      rv = this.callback.ppiReadPort( this, PORT_B );
	    }
	  }
	}
	break;

      case 2:						// Port C
	rv = this.valueC;
	if( (this.modeA == 0) && !this.modeB1 ) {
	  int v = rv;
	  if( this.callback != null ) {
	    v = this.callback.ppiReadPort( this, PORT_C );
	  }
	  if( this.dirCLin ) {
	    rv = (rv & 0xF0) | (v & 0x0F);
	  }
	  if( this.dirCHin ) {
	    rv = (v & 0xF0) | (rv & 0x0F);
	  }
	}
	break;
    }
    return rv & 0xFF;
  }


  public synchronized void reset()
  {
    this.modeA   = 0;
    this.valueA  = 0xFF;
    this.valueB  = 0xFF;
    this.valueC  = 0xFF;
    this.modeB1  = false;
    this.dirAin  = true;
    this.dirBin  = true;
    this.dirCHin = true;
    this.dirCLin = true;
  }


  public void setStrobedInValueA( int value )
  {
    if( (this.dirAin && (this.modeA == 1)) || (this.modeA == 2) ) {
      this.valueA = value & 0xFF;
      /*
       * Nach dem Strobe-Signal geht STB_A wieder auf 1.
       * IBF_A=1
       * INTR_A=1 wenn INTE_A_IN=1
       */
      int mask = STB_A_MASK | IBF_A_MASK;
      if( (this.valueC & INTE_A_IN_MASK) != 0 ) {
	mask |= INTR_A_MASK;
      }
      setBitsC( mask, true );
    }
  }


  public void setStrobedInValueB( int value )
  {
    if( this.dirBin && this.modeB1 ) {
      this.valueB = value & 0xFF;
      /*
       * Nach dem Strobe-Signal geht STB_B wieder auf 1.
       * IBF_B=1
       * INTR_B=1 wenn INTE_B_IN=1
       */
      int mask = STB_B_MASK | IBF_B_MASK;
      if( (this.valueC & INTE_B_IN_MASK) != 0 ) {
	mask |= INTR_B_MASK;
      }
      setBitsC( mask, true );
    }
  }


  public void write( int port, int value )
  {
    value &= 0xFF;
    switch( port & 0x03 ) {
      case 0:						// Port A
	if( !this.dirAin ) {
	  if( this.modeA == 0 ) {
	    if( value != this.valueA ) {
	      this.valueA = value;
	      if( this.callback != null ) {
		this.callback.ppiWritePort( this, PORT_A, value, false );
	      }
	    }
	  }
	  else if( (this.modeA == 1) || (this.modeA == 2) ) {
	    this.valueA = value;
	    if( this.callback != null ) {
	      this.callback.ppiWritePort( this, PORT_A, value, true );
	    }
	    this.valueC &= ~INTR_A_MASK;
	    setBitsC( OBF_A_MASK, false );
	  }
	}
	break;

      case 1:						// Port B
	if( !this.dirBin ) {
	  if( this.modeB1 ) {
	    this.valueB = value;
	    if( this.callback != null ) {
	      this.callback.ppiWritePort( this, PORT_B, value, true );
	    }
	    this.valueC &= ~INTR_B_MASK;
	    setBitsC( OBF_B_MASK, false );
	  } else {
	    if( value != this.valueB ) {
	      this.valueB = value;
	      if( this.callback != null ) {
		this.callback.ppiWritePort( this, PORT_B, value, false );
	      }
	    }
	  }
	}
	break;

      case 2:						// Port C
	if( (!this.dirCLin || !this.dirCHin)
	    && (this.modeA != 1) && !this.modeB1 )
	{
	  if( this.dirCLin ) {
	    value = (value & 0xF0) | (this.valueC & 0x0F);
	  }
	  if( this.dirCHin ) {
	    value = (this.valueC & 0xF0) | (value & 0x0F);
	  }
	  if( this.modeA > 1 ) {
	    value = (this.valueC & 0xF8) | (value & 0x07);
	  }
	  if( value != this.valueC ) {
	    this.valueC = value;
	    if( this.callback != null ) {
	      this.callback.ppiWritePort( this, PORT_C, value, false );
	    }
	  }
	}
	break;

      case 3:						// Steuerbyte
	if( (value & 0x80) != 0 ) {
	  this.modeA   = (value >> 5) & 0x03;
	  this.dirAin  = ((value & 0x10) != 0);
	  this.modeB1  = ((value & 0x04) != 0);
	  this.dirBin  = ((value & 0x02) != 0);
	  this.dirCHin = ((value & 0x08) != 0);
	  this.dirCLin = ((value & 0x01) != 0);
	} else {
	  if( (this.modeA != 1) && !this.modeB1 ) {
	    int b = (value >> 1) & 0x07;
	    if( ((this.modeA == 0) || (b <= 2))
		&& ((!this.dirCLin && (b < 4))
		    || (!this.dirCHin && (b >= 4))) )
	    {
	      int m = 1 << b;
	      setBitsC( 1 << b, (value & 0x01) != 0 );
	    }
	  }
	}
	break;
    }
  }


	/* --- private Methoden --- */

  private void setBitsC( int mask, boolean state )
  {
    if( state ) {
      if( (this.valueC & mask) != mask ) {
	this.valueC |= mask;
	if( this.callback != null ) {
	  this.callback.ppiWritePort( this, PORT_C, this.valueC, false );
	}
      }
    } else {
      if( (this.valueC & mask) != 0 ) {
	this.valueC &= ~mask;
	if( this.callback != null ) {
	  this.callback.ppiWritePort( this, PORT_C, this.valueC, false );
	}
      }
    }
  }
}

