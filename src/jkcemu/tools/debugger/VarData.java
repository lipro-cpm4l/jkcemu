/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Variable
 */

package jkcemu.tools.debugger;

import java.lang.*;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import z80emu.Z80MemView;


public class VarData
{
  public static enum VarType {
			INT1,
			INT2LE,
			INT2BE,
			INT3LE,
			INT3BE,
			INT4LE,
			INT4BE,
			INT8LE,
			INT8BE,
			FLOAT4LE,
			FLOAT4BE,
			FLOAT8LE,
			FLOAT8BE,
			BYTE_ARRAY,
			POINTER };

  private String  name;
  private int     addr;
  private VarType type;
  private int     size;
  private String  addrText;
  private String  typeText;
  private String  byteText;
  private String  valueText;
  private boolean imported;


  public VarData(
		String  name,
		int     addr,
		VarType type,
		int     size,
		boolean imported )
  {
    setValues( name, addr, type, size, imported );
  }


  public static VarData createWithDefaultType(
				String  name,
				int     addr,
				int     size,
				boolean imported )
  {
    VarType type = VarType.BYTE_ARRAY;
    switch( size ) {
      case 1:
	type = VarType.INT1;
	break;
      case 2:
	type = VarType.INT2LE;
	break;
      case 3:
	type = VarType.INT3LE;
	break;
      case 4:
	type = VarType.INT4LE;
	break;
      case 8:
	type = VarType.INT8LE;
	break;
    }
    return new VarData( name, addr, type, size, imported );
  }


  public int getAddress()
  {
    return this.addr;
  }


  public String getAddrText()
  {
    return this.addrText;
  }


  public String getByteText()
  {
    return this.byteText;
  }


  public String getName()
  {
    return this.name;
  }


  public int getSize()
  {
    return this.size;
  }


  public VarType getType()
  {
    return this.type;
  }


  public String getTypeText()
  {
    return this.typeText;
  }


  public String getValueText()
  {
    return this.valueText;
  }


  public void setValues(
		String  name,
		int     addr,
		VarType type,
		int     size,
		boolean imported )
  {
    this.name      = name;
    this.addr      = addr & 0xFFFF;
    this.type      = type;
    this.size      = size;
    this.imported  = imported;
    this.addrText  = null;
    this.typeText  = null;
    this.byteText  = null;
    this.valueText = null;
    if( this.type == null ) {
      this.type = VarType.BYTE_ARRAY;
    }
    switch( this.type ) {
      case INT1:
	this.size     = 1;
	this.typeText = "Byte / Int1";
	break;
      case INT2LE:
	this.size     = 2;
	this.typeText = "Int2 LE";
	break;
      case INT2BE:
	this.size     = 2;
	this.typeText = "Int2 BE";
	break;
      case INT3LE:
	this.size     = 3;
	this.typeText = "Int3 LE";
	break;
      case INT3BE:
	this.size     = 3;
	this.typeText = "Int3 BE";
	break;
      case INT4LE:
	this.size     = 4;
	this.typeText = "Int4 LE";
	break;
      case INT4BE:
	this.size     = 4;
	this.typeText = "Int4 BE";
	break;
      case INT8LE:
	this.size     = 8;
	this.typeText = "Int8 LE";
	break;
      case INT8BE:
	this.size     = 8;
	this.typeText = "Int8 BE";
	break;
      case FLOAT4LE:
	this.size     = 4;
	this.typeText = "Float4 LE";
	break;
      case FLOAT4BE:
	this.size     = 4;
	this.typeText = "Float4 BE";
	break;
      case FLOAT8LE:
	this.size     = 8;
	this.typeText = "Float8 LE";
	break;
      case FLOAT8BE:
	this.size     = 8;
	this.typeText = "Float8 BE";
	break;
      case BYTE_ARRAY:
	this.typeText = "Byte-Array / Text";
	break;
      case POINTER:
	this.size     = 2;
	this.typeText = "Zeiger";
	break;
    }
    if( size == 1 ) {
      this.addrText = String.format( "%04X", this.addr );
    } else if( this.size > 1 ) {
      this.addrText = String.format(
				"%04X-%04X",
				this.addr,
				this.addr + this.size - 1 );
    }
  }


  public void update( Z80MemView mem )
  {
    if( this.size > 0 ) {
      StringBuilder buf  = new StringBuilder( (this.size * 3) + 10 );
      int           size = this.size;
      int           addr = this.addr;
      if( this.type == VarType.POINTER ) {
	size = 16;
	addr = (int) getMemValueLE( mem, 2 );
	buf.append( String.format( "%04X: ", addr ) );
      }
      for( int i = 0; i < size; i++ ) {
	if( i > 0 ) {
	  buf.append( (char) '\u0020' );
	}
	buf.append( String.format( "%02X", mem.getMemByte( addr, false ) ) );
	addr = (addr + 1) & 0xFFFF;
      }
      this.byteText = buf.toString();
    }
    switch( this.type ) {
      case INT1:
	{
	  int v = mem.getMemByte( this.addr, false ) & 0xFF;
	  if( (v & 0x80) != 0 ) {
	    this.valueText = String.format( "%d / %d", (int) (byte) v, v );
	  } else {
	    if( (v > 0x20) && (v < 0x7F) ) {
	      this.valueText = String.format( "%d, ASCII=%c", v, (char) v );
	    } else {
	      this.valueText = Integer.toString( v );
	    }
	  }
	}
	break;
      case INT2LE:
	this.valueText = int2ToString( getMemValueLE( mem, 2 ) );
	break;
      case INT2BE:
	this.valueText = int2ToString( getMemValueBE( mem, 2 ) );
	break;
      case INT3LE:
	this.valueText = int3ToString( getMemValueLE( mem, 3 ) );
	break;
      case INT3BE:
	this.valueText = int3ToString( getMemValueBE( mem, 3 ) );
	break;
      case INT4LE:
	this.valueText = int4ToString( getMemValueLE( mem, 4 ) );
	break;
      case INT4BE:
	this.valueText = int4ToString( getMemValueBE( mem, 4 ) );
	break;
      case INT8LE:
	this.valueText = Long.toString( getMemValueLE( mem, 8 ) );
	break;
      case INT8BE:
	this.valueText = Long.toString( getMemValueBE( mem, 8 ) );
	break;
      case FLOAT4LE:
	this.valueText = Float.toString(
		Float.intBitsToFloat( (int) getMemValueLE( mem, 4 ) ) );
	break;
      case FLOAT4BE:
	this.valueText = Float.toString(
		Float.intBitsToFloat( (int) getMemValueBE( mem, 4 ) ) );
	break;
      case FLOAT8LE:
	this.valueText = Double.toString(
		Double.longBitsToDouble( (int) getMemValueLE( mem, 8 ) ) );
	break;
      case FLOAT8BE:
	this.valueText = Double.toString(
		Double.longBitsToDouble( (int) getMemValueBE( mem, 8 ) ) );
	break;
      case BYTE_ARRAY:
	if( this.size > 0 ) {
	  this.valueText = createByteArrayValueText(
						mem,
						this.addr,
						this.size,
						"" );
	}
	break;
      case POINTER:
	{
	  int addr       = (int) getMemValueLE( mem, 2 );
	  this.valueText = createByteArrayValueText(
				mem,
				addr,
				16,
				String.format( "%04X: ", addr ) );
	}
	break;
    }
  }


  public boolean wasImported()
  {
    return this.imported;
  }


	/* --- private Methoden --- */

  private String createByteArrayValueText(
					Z80MemView mem,
					int        addr,
					int        size,
					String     prefix )
  {
    StringBuilder buf = new StringBuilder( size + prefix.length() );
    buf.append( prefix );
    for( int i = 0; i < size; i++ ) {
      int b = mem.getMemByte( addr, false );
      if( (b < 0x20) || (b >= 0x7F) ) {
	b = '.';
      }
      buf.append( (char) b );
      addr = (addr + 1) & 0xFFFF;
    }
    return buf.toString();
  }


  private long getMemValueLE( Z80MemView mem, int len )
  {
    long rv = 0;
    int  a  = this.addr + len;
    for( int i = 0; i < len; i++ ) {
      a  = (a - 1) & 0xFFFF;
      rv = (rv << 8) | (mem.getMemByte( a, false ) & 0xFFL);
    }
    return rv;
  }


  private long getMemValueBE( Z80MemView mem, int len )
  {
    long rv = 0;
    int  a  = this.addr;
    for( int i = 0; i < len; i++ ) {
      rv = (rv << 8) | (mem.getMemByte( a, false ) & 0xFFL);
      a  = (a + 1) & 0xFFFF;
    }
    return rv;
  }


  private static String int2ToString( long v )
  {
    String rv = null;
    v &= 0xFFFF;
    if( (v & 0x8000) != 0 ) {
      int v2 = (int) (v | 0xFFFF0000);
      rv     = String.format( "%d / %d", v2, v );
    } else {
      rv = Long.toString( v );
    }
    return rv;
  }


  private static String int3ToString( long v )
  {
    String rv = null;
    v &= 0xFFFFFF;
    if( (v & 0x800000) != 0 ) {
      int v2 = (int) (v | 0xFF000000);
      rv     = String.format( "%d / %d", v2, v );
    } else {
      rv = Long.toString( v );
    }
    return rv;
  }


  private static String int4ToString( long v )
  {
    String rv = null;
    v &= 0xFFFFFFFF;
    if( (v & 0x80000000) != 0 ) {
      int v2 = (int) v;
      rv     = String.format( "%d / %d", v2, v );
    } else {
      rv = Long.toString( v );
    }
    return rv;
  }


}
