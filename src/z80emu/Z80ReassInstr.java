/*
 * (c) 2008-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Daten einer reassemblierten Instruction
 */

package z80emu;


public class Z80ReassInstr extends Exception
{
  private int     len;
  private String  name;
  private String  arg1;
  private Integer addr1;
  private boolean indirect1;
  private String  arg2;
  private Integer addr2;
  private boolean indirect2;
  private int[]   instrBytes;


  public Z80ReassInstr( int len, String name )
  {
    this.len        = len;
    this.name       = name;
    this.arg1       = null;
    this.addr1      = null;
    this.indirect1  = false;
    this.arg2       = null;
    this.addr2      = null;
    this.indirect2  = false;
    this.instrBytes = null;
  }


  public Z80ReassInstr( int len, String name, String arg1 )
  {
    this( len, name );
    this.arg1 = arg1;
  }


  public Z80ReassInstr( int len, String  name, Integer addr1 )
  {
    this( len, name );
    this.arg1  = Z80Reassembler.createWordText( addr1 );
    this.addr1 = addr1;
  }


  public Z80ReassInstr(
		int    len,
		String name,
		String arg1,
		String arg2 )
  {
    this( len, name );
    this.arg1 = arg1;
    this.arg2 = arg2;
  }


  public Z80ReassInstr(
		int     len,
		String  name,
		Integer addr1,
		String  arg2 )
  {
    this( len, name );
    this.arg1  = Z80Reassembler.createWordText( addr1 );
    this.addr1 = addr1;
    this.arg2  = arg2;
  }


  public Z80ReassInstr(
		int     len,
		String  name,
		Integer addr1,
		boolean indirect1,
		String  arg2 )
  {
    this( len, name );
    this.arg1      = Z80Reassembler.createAddrText( addr1, indirect1 );
    this.addr1     = addr1;
    this.indirect1 = indirect1;
    this.arg2      = arg2;
  }


  public Z80ReassInstr(
		int     len,
		String  name,
		String  arg1,
		Integer addr2 )
  {
    this( len, name );
    this.arg1     = arg1;
    this.arg2     = Z80Reassembler.createWordText( addr2 );
    this.addr2 = addr2;
  }


  public Z80ReassInstr(
		int     len,
		String  name,
		String  arg1,
		Integer addr2,
		boolean indirect2 )
  {
    this( len, name );
    this.arg1      = arg1;
    this.arg2      = Z80Reassembler.createAddrText( addr2, indirect2 );
    this.addr2     = addr2;
    this.indirect2 = indirect2;
  }


  public int getByte( int idx )
  {
    int rv = -1;
    if( this.instrBytes != null ) {
      if( (idx >= 0) && (idx < this.instrBytes.length) ) {
	rv = this.instrBytes[ idx ];
      }
    }
    return rv;
  }


  public int getLength()
  {
    return this.len;
  }


  public String getName()
  {
    return this.name;
  }


  public String getArg1()
  {
    return this.arg1;
  }


  public Integer getAddress1()
  {
    return this.addr1;
  }


  public String getArg2()
  {
    return this.arg2;
  }


  public Integer getAddress2()
  {
    return this.addr2;
  }


  public boolean isIndirect1()
  {
    return this.indirect1;
  }


  public boolean isIndirect2()
  {
    return this.indirect2;
  }


  public void setBytes( int... instrBytes )
  {
    if( this.instrBytes == null ) {
      this.instrBytes = new int[ this.len ];
    }
    int i = 0;
    if( instrBytes != null ) {
      while( (i < instrBytes.length) && (i < this.instrBytes.length) ) {
	this.instrBytes[ i ] = instrBytes[ i ];
	i++;
      }
    }
    while( i < this.instrBytes.length ) {
      this.instrBytes[ i++ ] = -1;
    }
  }
}
