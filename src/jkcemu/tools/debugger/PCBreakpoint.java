/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Halte-/Log-Punkt auf eine Programmadresse
 */

package jkcemu.tools.debugger;

import java.util.Arrays;
import org.xml.sax.Attributes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;


public class PCBreakpoint extends ImportableBreakpoint
{
  public static final String BP_TYPE = "pc";

  private static final String ATTR_ADDR          = "addr";
  private static final String ATTR_REG_NAME      = "reg_name";
  private static final String ATTR_REG_MASK      = "reg_mask";
  private static final String ATTR_REG_CONDITION = "reg_condition";
  private static final String ATTR_REG_VALUE     = "reg_value";
  private static final String ATTR_FLAG_MASK     = "flag_mask";
  private static final String ATTR_FLAG_VALUE    = "flag_value";


  private static final String[] sortedRegNames = {
			"A", "B", "BC", "C", "D", "DE", "E", "H", "HL",
			"IX", "IXH", "IXL", "IY", "IYH", "IYL", "L", "SP" };

  private static final String[] textFlagsCleared = {
			"P", "NZ", null, null, null, "PO", null, "NC" };

  private static final String[] textFlagsSet = {
			"M", "Z", null, null, null, "PE", null, "C" };

  private int    addr;
  private String regName;
  private int    regMask;
  private String regCond;
  private int    regValue;
  private int    flagMask;
  private int    flagValue;


  public PCBreakpoint(
		DebugFrm debugFrm,
		String   name,
		int      addr ) throws InvalidParamException
  {
    this( debugFrm, name, addr, null, 0, null, 0, 0, 0 );
  }


  public PCBreakpoint(
		DebugFrm debugFrm,
		String   name,
		int      addr,
		String   regName,
		int      regMask,
		String   regCond,
		int      regValue,
		int      flagMask,
		int      flagValue ) throws InvalidParamException
  {
    super( debugFrm, name );
    this.regName   = checkRegName( regName );
    this.regMask   = regMask & 0xFFFF;
    this.regCond   = checkCondition( regCond );
    this.regValue  = regValue & 0xFFFF;
    this.flagMask  = flagMask & 0xFF;
    this.flagValue = flagValue & 0xFF;
    setAddress( addr );
  }


  public static PCBreakpoint createByAttrs(
					DebugFrm   debugFrm,
					Attributes attrs )
  {
    PCBreakpoint bp = null;
    if( attrs != null ) {
      Integer addr = BreakpointVarLoader.readInteger(
					attrs.getValue( ATTR_ADDR ) );
      if( addr != null ) {
	try {
	  int    regMask  = 0xFFFF;
	  int    regValue = 0;
	  String regName  = null;
	  String regCond  = null;
	  try {
	    regName = checkRegName( attrs.getValue( ATTR_REG_NAME ) );
	    regCond = checkCondition( attrs.getValue( ATTR_REG_CONDITION ) );
	    if( (regName != null) && (regCond != null) ) {
	      regMask  = getHex4Value( attrs, ATTR_REG_MASK );
	      regValue = getHex4Value( attrs, ATTR_REG_VALUE );
	    }
	  }
	  catch( InvalidParamException ex ) {}
	  bp = new PCBreakpoint(
			debugFrm,
			checkName( attrs.getValue( ATTR_NAME ) ),
			addr.intValue(),
			regName,
			regMask,
			regCond,
			regValue,
			getHex2Value( attrs, ATTR_FLAG_MASK ),
			getHex2Value( attrs, ATTR_FLAG_VALUE ) );
	}
	catch( NumberFormatException ex ) {}
	catch( InvalidParamException ex ) {}
      }
    }
    return bp;
  }


  public int getFlagMask()
  {
    return this.flagMask;
  }


  public int getFlagValue()
  {
    return this.flagValue;
  }


  public String getRegCondition()
  {
    return this.regCond;
  }


  public int getRegMask()
  {
    return this.regMask;
  }


  public String getRegName()
  {
    return this.regName;
  }


  public int getRegValue()
  {
    return this.regValue;
  }


  public void setAddress( int addr )
  {
    this.addr = addr & 0xFFFF;

    boolean       hasCond = false;
    StringBuilder buf     = new StringBuilder();
    String        name    = getName();
    if( name != null ) {
      buf.append( name );
      buf.append( ':' );
    }
    buf.append( String.format( "%04X", this.addr ) );
    if( (this.regName != null) && (this.regCond != null) ) {
      if( !this.regName.isEmpty() && !this.regCond.isEmpty() ) {
	buf.append( ':' );
	if( regName.length() == 2 ) {
	  if( (this.regMask & 0xFFFF) != 0xFFFF ) {
	    buf.append( String.format(
				"(%s&%04X)",
				this.regName,
				this.regMask ) );
	  } else {
	    buf.append( regName );
	  }
	  buf.append( this.regCond );
	  buf.append( String.format( "%04X", this.regValue ) );
	} else {
	  if( (this.regMask & 0xFF) != 0xFF ) {
	    buf.append( String.format(
				"(%s&%02X)",
				this.regName,
				this.regMask ) );
	  } else {
	    buf.append( regName );
	  }
	  buf.append( this.regCond );
	  buf.append( String.format( "%02X", this.regValue ) );
	}
	hasCond = true;
      }
    }
    if( this.flagMask != 0 ) {
      buf.append( hasCond ? ',' : ':' );
      boolean isFirst = true;
      int     mask    = 0x80;
      for( int i = 0;
	  (i < textFlagsCleared.length) && (i < textFlagsSet.length);
	  i++ )
      {
	if( (this.flagMask & mask) != 0 ) {
	  String s = null;
	  if( (this.flagValue & mask) != 0 ) {
	    s = textFlagsSet[ i ];
	  } else {
	    s = textFlagsCleared[ i ];
	  }
	  if( s != null ) {
	    if( isFirst ) {
	      isFirst = false;
	    } else {
	      buf.append( ',' );
	    }
	    buf.append( s );
	  }
	}
	mask >>= 1;
      }
    }
    setText( buf.toString() );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getAddress()
  {
    return this.addr;
  }


  @Override
  protected boolean matchesImpl( Z80CPU cpu, Z80InterruptSource iSource )
  {
    boolean rv = (cpu.getRegPC() == this.addr);
    if( rv && (this.regName != null) && (this.regCond != null) ) {
      int r8  = -1;
      int r16 = -1;
      if( this.regName.equals( "A" ) ) {
	r8 = cpu.getRegA();
      } else if( this.regName.equals( "B" ) ) {
	r8 = cpu.getRegB();
      } else if( this.regName.equals( "C" ) ) {
	r8 = cpu.getRegC();
      } else if( this.regName.equals( "D" ) ) {
	r8 = cpu.getRegD();
      } else if( this.regName.equals( "E" ) ) {
	r8 = cpu.getRegE();
      } else if( this.regName.equals( "H" ) ) {
	r8 = cpu.getRegH();
      } else if( this.regName.equals( "L" ) ) {
	r8 = cpu.getRegL();
      } else if( this.regName.equals( "BC" ) ) {
	r16 = cpu.getRegBC();
      } else if( this.regName.equals( "DE" ) ) {
	r16 = cpu.getRegDE();
      } else if( this.regName.equals( "HL" ) ) {
	r16 = cpu.getRegHL();
      } else if( this.regName.equals( "IX" ) ) {
	r16 = cpu.getRegIX();
      } else if( this.regName.equals( "IXH" ) ) {
	r8 = (cpu.getRegIX() >> 8);
      } else if( this.regName.equals( "IXL" ) ) {
	r8 = cpu.getRegIX() & 0xFF;
      } else if( this.regName.equals( "IY" ) ) {
	r16 = cpu.getRegIY();
      } else if( this.regName.equals( "IYH" ) ) {
	r8 = (cpu.getRegIY() >> 8);
      } else if( this.regName.equals( "IYL" ) ) {
	r8 = cpu.getRegIY() & 0xFF;
      } else if( this.regName.equals( "SP" ) ) {
	r16 = cpu.getRegSP();
      }
      if( (r8 >= 0) || (r16 >= 0) ) {
	int v1 = 0;
	int v2 = 0;
	if( r8 >= 0 ) {
	  v1 = (r8 & this.regMask & 0xFF);
	  v2 = (this.regValue & 0xFF);
	} else {
	  v1 = (r16 & this.regMask & 0xFFFF);
	  v2 = (this.regValue & 0xFFFF);
	}
	rv = checkValues( v1, this.regCond, v2 );
      }
    }
    if( rv && (this.flagMask != 0) ) {
      rv = ((cpu.getRegF() & this.flagMask) == this.flagValue);
    }
    return rv;
  }


  @Override
  public void writeTo( Document doc, Node parent )
  {
    Element elem = createBreakpointElement( doc, BP_TYPE );
    elem.setAttribute( ATTR_ADDR, toHex4( this.addr ) );
    appendAttributesTo( elem );
    if( (this.regName != null) && (this.regCond != null) ) {
      if( !this.regName.isEmpty() && !this.regCond.isEmpty() ) {
	elem.setAttribute( ATTR_REG_NAME, this.regName );
	elem.setAttribute( ATTR_REG_MASK, toHex4( this.regMask ) );
	elem.setAttribute( ATTR_REG_CONDITION, this.regCond );
	elem.setAttribute( ATTR_REG_VALUE, toHex4( this.regValue ) );
      }
    }
    elem.setAttribute( ATTR_FLAG_MASK, toHex2( this.flagMask ) );
    elem.setAttribute( ATTR_FLAG_VALUE, toHex2( this.flagValue ) );
    parent.appendChild( elem );
  }


	/* --- private Methoden --- */

  private static String checkRegName( String regName )
					throws InvalidParamException
  {
    if( regName != null ) {
      if( regName.isEmpty() ) {
	regName = null;
      } else {
	if( Arrays.binarySearch( sortedRegNames, regName ) < 0 ) {
	  throw new InvalidParamException(
				regName + ": Ung\u00FCltiges Register" );
	}
      }
    }
    return regName;
  }
}
