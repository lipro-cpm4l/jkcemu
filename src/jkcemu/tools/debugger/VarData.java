/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Variable
 */

package jkcemu.tools.debugger;

import jkcemu.programming.basic.VarDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import z80emu.Z80MemView;


public class VarData
{
  public static enum VarType {
			INT1,
			INT2_LE,
			INT2_BE,
			INT3_LE,
			INT3_BE,
			INT4_LE,
			INT4_BE,
			BC_DEC6,
			POINTER,
			BYTE_ARRAY };

  public static final String ELEM_VARIABLE = "variable";

  public static final String ATTR_ADDR     = "addr";
  public static final String ATTR_NAME     = "name";
  public static final String ATTR_SIZE     = "size";
  public static final String ATTR_TYPE     = "type";
  public static final String ATTR_IMPORTED = "imported";

  public static final String TYPE_INT1       = "INT1";
  public static final String TYPE_INT2_LE    = "INT2_LE";
  public static final String TYPE_INT2_BE    = "INT2_BE";
  public static final String TYPE_INT3_LE    = "INT3_LE";
  public static final String TYPE_INT3_BE    = "INT3_BE";
  public static final String TYPE_INT4_LE    = "INT4_LE";
  public static final String TYPE_INT4_BE    = "INT4_BE";
  public static final String TYPE_BC_DEC6    = "BC_DEC6";
  public static final String TYPE_POINTER    = "POINTER";
  public static final String TYPE_BYTE_ARRAY = "BYTE_ARRAY";

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


  public static VarType createDefaultType( String name, int size )
  {
    VarType type = VarType.BYTE_ARRAY;
    switch( size ) {
      case 1:
	type = VarType.INT1;
	break;
      case 2:
	type = VarType.INT2_LE;
	break;
      case 3:
	type = VarType.INT3_LE;
	break;
      case 4:
	type = VarType.INT4_LE;
	break;
    }
    if( name != null ) {
      if( name.startsWith( VarDecl.LABEL_PREFIX_STRING ) ) {
	type = VarType.BYTE_ARRAY;
      }
      else if( name.startsWith( VarDecl.LABEL_PREFIX_DEC6 )
	         && (size == 6) )
      {
	type = VarType.BC_DEC6;
      }
    }
    return type;
  }


  public static VarData createWithDefaultType(
					String  name,
					int     addr,
					int     size,
					boolean imported )
  {
    return new VarData(
		name,
		addr,
		createDefaultType( name, size ),
		size,
		imported );
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


  public boolean getImported()
  {
    return this.imported;
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
      case INT2_LE:
	this.size     = 2;
	this.typeText = "Int2 LE";
	break;
      case INT2_BE:
	this.size     = 2;
	this.typeText = "Int2 BE";
	break;
      case INT3_LE:
	this.size     = 3;
	this.typeText = "Int3 LE";
	break;
      case INT3_BE:
	this.size     = 3;
	this.typeText = "Int3 BE";
	break;
      case INT4_LE:
	this.size     = 4;
	this.typeText = "Int4 LE";
	break;
      case INT4_BE:
	this.size     = 4;
	this.typeText = "Int4 BE";
	break;
      case BC_DEC6:
	this.size     = 6;
	this.typeText = "Decimal (Basic Compiler)";
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
	  buf.append( '\u0020' );
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
      case INT2_LE:
	this.valueText = int2ToString( getMemValueLE( mem, 2 ) );
	break;
      case INT2_BE:
	this.valueText = int2ToString( getMemValueBE( mem, 2 ) );
	break;
      case INT3_LE:
	this.valueText = int3ToString( getMemValueLE( mem, 3 ) );
	break;
      case INT3_BE:
	this.valueText = int3ToString( getMemValueBE( mem, 3 ) );
	break;
      case INT4_LE:
	this.valueText = int4ToString( getMemValueLE( mem, 4 ) );
	break;
      case INT4_BE:
	this.valueText = int4ToString( getMemValueBE( mem, 4 ) );
	break;
      case BC_DEC6:
	this.valueText = dec6ToString( mem );
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


  public void writeTo( Document doc, Node parent )
  {
    Element elem = doc.createElement( ELEM_VARIABLE );
    String  type = TYPE_BYTE_ARRAY;
    switch( this.type ) {
      case INT1:
	type = TYPE_INT1;
	break;
      case INT2_LE:
	type = TYPE_INT2_LE;
	break;
      case INT2_BE:
	type = TYPE_INT2_BE;
	break;
      case INT3_LE:
	type = TYPE_INT3_LE;
	break;
      case INT3_BE:
	type = TYPE_INT3_BE;
	break;
      case INT4_LE:
	type = TYPE_INT4_LE;
	break;
      case INT4_BE:
	type = TYPE_INT4_BE;
	break;
      case BC_DEC6:
	type = TYPE_BC_DEC6;
	break;
      case POINTER:
	type = TYPE_POINTER;
	break;
    }
    elem.setAttribute( ATTR_TYPE, type );
    elem.setAttribute( ATTR_ADDR, String.format( "%04XH", this.addr ) );
    if( type.equals( TYPE_BYTE_ARRAY ) ) {
      elem.setAttribute( ATTR_SIZE, String.format( "%04XH", this.size ) );
    }
    if( this.name != null ) {
      if( !this.name.isEmpty() ) {
	elem.setAttribute( ATTR_NAME, this.name );
      }
    }
    elem.setAttribute( ATTR_IMPORTED, Boolean.toString( this.imported ) );
    parent.appendChild( elem );
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


  private String dec6ToString( Z80MemView mem )
  {
    boolean       err  = false;
    int           addr = this.addr;
    StringBuilder buf  = new StringBuilder( 13 );
    int           b    = mem.getMemByte( addr++, false );
    if( (b & 0x80) != 0 ) {
      buf.append( '-' );
    }
    int     digitsToPoint  = 10 - ((b >> 4) & 0x07);
    boolean prePointDigits = false;
    b &= 0x0F;
    if( b > 0 ) {
      if( b > 0x09 ) {
	err = true;
      } else {
	buf.append( (char) (b + '0') );
	prePointDigits = true;
      }
    }
    int     zeros = 0;
    boolean point = false;
    for( int i = 0; i < 5; i++ ) {
      b = mem.getMemByte( addr++, false );
      for( int k = 0; k < 2; k++ ) {
	int c = (b >> 4) & 0x0F;
	if( c > 9 ) {
	  err = true;
	  break;
	}
	if( digitsToPoint == 0 ) {
	  if( !prePointDigits ) {
	    buf.append( '0' );
	  }
	  buf.append( '.' );
	  point = true;
	}
	--digitsToPoint;
	if( point ) {
	  if( c == 0 ) {
	    zeros++;
	  } else {
	    while( zeros > 0 ) {
	      buf.append( '0' );
	      --zeros;
	    }
	    buf.append( (char) (c + '0') );
	  }
	} else {
	  if( prePointDigits || (c != 0) ) {
	    buf.append( (char) (c + '0') );
	    prePointDigits = true;
	  }
	}
	b <<= 4;
      }
    }
    return err ? "ung\u00FCltig" : buf.toString();
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
