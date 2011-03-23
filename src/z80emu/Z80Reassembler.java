/*
 * (c) 2008-2011 Jens Mueller
 *
 * Z80-Emulator
 *
 * Reassembler
 */

package z80emu;

import java.lang.*;


public class Z80Reassembler
{
  public static String createWordText( Integer value )
  {
    return value != null ? createWordText( value.intValue() ) : null;
  }


  public static String createAddrText( Integer addr, boolean indirect )
  {
    String rv = createWordText( addr );
    if( (rv != null) && indirect ) {
      rv = String.format( "(%s)", rv );
    }
    return rv;
  }


  public static Z80ReassInstr reassInstruction(
					Z80MemView memory,
					int        addr )
  {
    Z80ReassInstr instr = null;

    int     b0    = memory.getMemByte( addr, true );
    boolean b1_m1 = ((b0 == 0xCB) || (b0 == 0xED)
				|| (b0 == 0xDD) || (b0 == 0xFD));
    int     b1    = memory.getMemByte( addr + 1, b1_m1 );
    int     b2    = memory.getMemByte( addr + 2, true );
    int     b3    = memory.getMemByte( addr + 3, true );
    if( (b0 >= 0) && (b0 <= 0x3F) ) {
      instr = reass00to3F( addr, b0, b1, b2 );
    }
    else if( b0 == 0x76 ) {
      instr = new Z80ReassInstr( 1, "HALT" );
    }
    else if( (b0 >= 0x40) && (b0 <= 0x7F) && (b0 != 0x76) ) {
      instr = new Z80ReassInstr(
			1,
			"LD",
			getRegName( b0 >> 3 ),
			getRegName( b0 ) );
    }
    else if( (b0 >= 0x80) && (b0 <= 0xBF) ) {
      String instrName = "";
      int    instCode  = (b0 >> 3) & 0x07;
      switch( instCode ) {
	case 0:
	  instrName = "ADD";
	  break;
	case 1:
	  instrName = "ADC";
	  break;
	case 2:
	  instrName = "SUB";
	  break;
	case 3:
	  instrName = "SBC";
	  break;
	case 4:
	  instrName = "AND";
	  break;
	case 5:
	  instrName = "XOR";
	  break;
	case 6:
	  instrName = "OR";
	  break;
	case 7:
	  instrName = "CP";
	  break;
      }
      if( (instCode == 0) || (instCode == 1) || (instCode == 3) ) {
	instr = new Z80ReassInstr(
			1,
			instrName,
			"A",
			getRegName( b0 ) );
      } else {
	instr = new Z80ReassInstr(
			1,
			instrName,
			getRegName( b0 ) );
      }
    } else {
      instr = reassC0toFF( addr, b0, b1, b2, b3 );
    }
    if( instr != null ) {
      instr.setBytes( b0, b1, b2, b3 );
    }
    return instr;
  }


	/* --- private Methoden --- */

  private static Z80ReassInstr reass00to3F(
					int addr,
					int b0,
					int b1,
					int b2 )
  {
    switch( b0 ) {
      case 0x00:
	return new Z80ReassInstr( 1, "NOP" );
      case 0x01:
	return new Z80ReassInstr(
			3,
			"LD", "BC", createWordText( b1, b2 ) );
      case 0x02:
	return new Z80ReassInstr( 1, "LD", "(BC)", "A" );
      case 0x03:
	return new Z80ReassInstr( 1, "INC", "BC" );
      case 0x04:
	return new Z80ReassInstr( 1, "INC", "B" );
      case 0x05:
	return new Z80ReassInstr( 1, "DEC", "B" );
      case 0x06:
	return new Z80ReassInstr( 2, "LD", "B", getByteText( b1 ) );
      case 0x07:
	return new Z80ReassInstr( 1, "RLCA" );
      case 0x08:
	return new Z80ReassInstr( 1, "EX", "AF", "AF\'" );
      case 0x09:
	return new Z80ReassInstr( 1, "ADD", "HL", "BC" );
      case 0x0A:
	return new Z80ReassInstr( 1, "LD", "A", "(BC)" );
      case 0x0B:
	return new Z80ReassInstr( 1, "DEC", "BC" );
      case 0x0C:
	return new Z80ReassInstr( 1, "INC", "C" );
      case 0x0D:
	return new Z80ReassInstr( 1, "DEC", "C" );
      case 0x0E:
	return new Z80ReassInstr( 2, "LD", "C", getByteText( b1 ) );
      case 0x0F:
	return new Z80ReassInstr( 1, "RRCA" );
      case 0x10:
	return new Z80ReassInstr(
			2,
			"DJNZ", getRelAddr( addr + 2, b1 ) );
      case 0x11:
	return new Z80ReassInstr( 3, "LD", "DE", createWordText( b1, b2 ) );
      case 0x12:
	return new Z80ReassInstr( 1, "LD", "(DE)", "A" );
      case 0x13:
	return new Z80ReassInstr( 1, "INC", "DE" );
      case 0x14:
	return new Z80ReassInstr( 1, "INC", "D" );
      case 0x15:
	return new Z80ReassInstr( 1, "DEC", "D" );
      case 0x16:
	return new Z80ReassInstr( 2, "LD", "D", getByteText( b1 ) );
      case 0x17:
	return new Z80ReassInstr( 1, "RLA" );
      case 0x18:
	return new Z80ReassInstr(
			2,
			"JR", getRelAddr( addr + 2, b1 ) );
      case 0x19:
	return new Z80ReassInstr( 1, "ADD", "HL", "DE" );
      case 0x1A:
	return new Z80ReassInstr( 1, "LD", "A", "(DE)" );
      case 0x1B:
	return new Z80ReassInstr( 1, "DEC", "DE" );
      case 0x1C:
	return new Z80ReassInstr( 1, "INC", "E" );
      case 0x1D:
	return new Z80ReassInstr( 1, "DEC", "E" );
      case 0x1E:
	return new Z80ReassInstr( 2, "LD", "E", getByteText( b1 ) );
      case 0x1F:
	return new Z80ReassInstr( 1, "RRA" );
      case 0x20:
	return new Z80ReassInstr(
			2,
			"JR", "NZ", getRelAddr( addr + 2, b1 ) );
      case 0x21:
	return new Z80ReassInstr(
			3,
			"LD", "HL", createWordText( b1, b2 ) );
      case 0x22:
	return new Z80ReassInstr(
			3,
			"LD", getWord( b1, b2 ), true, "HL" );
      case 0x23:
	return new Z80ReassInstr( 1, "INC", "HL" );
      case 0x24:
	return new Z80ReassInstr( 1, "INC", "H" );
      case 0x25:
	return new Z80ReassInstr( 1, "DEC", "H" );
      case 0x26:
	return new Z80ReassInstr( 2, "LD", "H", getByteText( b1 ) );
      case 0x27:
	return new Z80ReassInstr( 1, "DAA" );
      case 0x28:
	return new Z80ReassInstr(
			2,
			"JR", "Z", getRelAddr( addr + 2, b1 ) );
      case 0x29:
	return new Z80ReassInstr( 1, "ADD", "HL", "HL" );
      case 0x2A:
	return new Z80ReassInstr(
			3,
			"LD", "HL", getWord( b1, b2 ), true );
      case 0x2B:
	return new Z80ReassInstr( 1, "DEC", "HL" );
      case 0x2C:
	return new Z80ReassInstr( 1, "INC", "L" );
      case 0x2D:
	return new Z80ReassInstr( 1, "DEC", "L" );
      case 0x2E:
	return new Z80ReassInstr( 2, "LD", "L", getByteText( b1 ) );
      case 0x2F:
	return new Z80ReassInstr( 1, "CPL" );
      case 0x30:
	return new Z80ReassInstr(
			2,
			"JR", "NC", getRelAddr( addr + 2, b1 ) );
      case 0x31:
	return new Z80ReassInstr(
			3,
			"LD", "SP", getWord( b1, b2 ) );
      case 0x32:
	return new Z80ReassInstr(
			3,
			"LD", getWord( b1, b2 ), true, "A" );
      case 0x33:
	return new Z80ReassInstr( 1, "INC", "SP" );
      case 0x34:
	return new Z80ReassInstr( 1, "INC", "(HL)" );
      case 0x35:
	return new Z80ReassInstr( 1, "DEC", "(HL)" );
      case 0x36:
	return new Z80ReassInstr( 2, "LD", "(HL)", getByteText( b1 ) );
      case 0x37:
	return new Z80ReassInstr( 1, "SCF" );
      case 0x38:
	return new Z80ReassInstr(
			2,
			"JR", "C", getRelAddr( addr + 2, b1 ) );
      case 0x39:
	return new Z80ReassInstr( 1, "ADD", "HL", "SP" );
      case 0x3A:
	return new Z80ReassInstr(
			3,
			"LD", "A", getWord( b1, b2 ), true );
      case 0x3B:
	return new Z80ReassInstr( 1, "DEC", "SP" );
      case 0x3C:
	return new Z80ReassInstr( 1, "INC", "A" );
      case 0x3D:
	return new Z80ReassInstr( 1, "DEC", "A" );
      case 0x3E:
	return new Z80ReassInstr( 2, "LD", "A", getByteText( b1 ) );
      case 0x3F:
	return new Z80ReassInstr( 1, "CCF" );
    }
    return null;
  }


  private static Z80ReassInstr reassC0toFF(
					int addr,
					int b0,
					int b1,
					int b2,
					int b3 )
  {
    switch( b0 ) {
      case 0xC0:
	return new Z80ReassInstr( 1, "RET", "NZ" );
      case 0xC1:
	return new Z80ReassInstr( 1, "POP", "BC" );
      case 0xC2:
	return new Z80ReassInstr(
			3,
			"JP", "NZ", getWord( b1, b2 ) );
      case 0xC3:
	return new Z80ReassInstr( 3, "JP", getWord( b1, b2 ) );
      case 0xC4:
	return new Z80ReassInstr(
			3,
			"CALL", "NZ", getWord( b1, b2 ) );
      case 0xC5:
	return new Z80ReassInstr( 1, "PUSH", "BC" );
      case 0xC6:
	return new Z80ReassInstr( 2, "ADD", "A", getByteText( b1 ) );
      case 0xC7:
	return new Z80ReassInstr( 1, "RST", "00H" );
      case 0xC8:
	return new Z80ReassInstr( 1, "RET", "Z" );
      case 0xC9:
	return new Z80ReassInstr( 1, "RET" );
      case 0xCA:
	return new Z80ReassInstr(
			3,
			"JP", "Z", getWord( b1, b2 ) );
      case 0xCB:
	return reassCB( b1 );
      case 0xCC:
	return new Z80ReassInstr(
			3,
			"CALL", "Z", getWord( b1, b2 ) );
      case 0xCD:
	return new Z80ReassInstr(
			3,
			"CALL", getWord( b1, b2 ) );
      case 0xCE:
	return new Z80ReassInstr( 2, "ADC", "A", getByteText( b1 ) );
      case 0xCF:
	return new Z80ReassInstr( 1, "RST", "08H" );
      case 0xD0:
	return new Z80ReassInstr( 1, "RET", "NC" );
      case 0xD1:
	return new Z80ReassInstr( 1, "POP", "DE" );
      case 0xD2:
	return new Z80ReassInstr(
			3,
			"JP", "NC", getWord( b1, b2 ) );
      case 0xD3:
	return new Z80ReassInstr(
			2, "OUT", getIndirectByteText( b1 ), "A" );
      case 0xD4:
	return new Z80ReassInstr(
			3,
			"CALL", "NC", getWord( b1, b2 ) );
      case 0xD5:
	return new Z80ReassInstr( 1, "PUSH", "DE" );
      case 0xD6:
	return new Z80ReassInstr( 2, "SUB", getByteText( b1 ) );
      case 0xD7:
	return new Z80ReassInstr( 1, "RST", "10H" );
      case 0xD8:
	return new Z80ReassInstr( 1, "RET", "C" );
      case 0xD9:
	return new Z80ReassInstr( 1, "EXX" );
      case 0xDA:
	return new Z80ReassInstr(
			3,
			"JP", "C", getWord( b1, b2 ) );
      case 0xDB:
	return new Z80ReassInstr(
			2, "IN", "A", getIndirectByteText( b1 ) );
      case 0xDC:
	return new Z80ReassInstr(
			3,
			"CALL", "C", getWord( b1, b2 ) );
      case 0xDD:
	return ((b1 == 0xDD) || (b1 == 0xED) || (b1 == 0xFD)) ?
	    new Z80ReassInstr( 1, "*NOP" ) :
		reassIXY( "IX", addr, b1, b2, b3 );
      case 0xDE:
	return new Z80ReassInstr( 2, "SBC", getByteText( b1 ) );
      case 0xDF:
	return new Z80ReassInstr( 1, "RST", "18H" );
      case 0xE0:
	return new Z80ReassInstr( 1, "RET", "PO" );
      case 0xE1:
	return new Z80ReassInstr( 1, "POP", "HL" );
      case 0xE2:
	return new Z80ReassInstr(
			3,
			"JP", "PO", getWord( b1, b2 ) );
      case 0xE3:
	return new Z80ReassInstr( 1, "EX", "(SP)", "HL" );
      case 0xE4:
	return new Z80ReassInstr(
			3,
			"CALL", "PO", getWord( b1, b2 ) );
      case 0xE5:
	return new Z80ReassInstr( 1, "PUSH", "HL" );
      case 0xE6:
	return new Z80ReassInstr( 2, "AND", getByteText( b1 ) );
      case 0xE7:
	return new Z80ReassInstr( 1, "RST", "20H" );
      case 0xE8:
	return new Z80ReassInstr( 1, "RET", "PE" );
      case 0xE9:
	return new Z80ReassInstr( 1, "JP", "(HL)" );
      case 0xEA:
	return new Z80ReassInstr(
			3,
			"JP", "PE", getWord( b1, b2 ) );
      case 0xEB:
	return new Z80ReassInstr( 1, "EX", "DE", "HL" );
      case 0xEC:
	return new Z80ReassInstr(
			3,
			"CALL", "PE", getWord( b1, b2 ) );
      case 0xED:
	return reassED( b1, b2, b3 );
      case 0xEE:
	return new Z80ReassInstr( 2, "XOR", getByteText( b1 ) );
      case 0xEF:
	return new Z80ReassInstr( 1, "RST", "28H" );
      case 0xF0:
	return new Z80ReassInstr( 1, "RET", "P" );
      case 0xF1:
	return new Z80ReassInstr( 1, "POP", "AF" );
      case 0xF2:
	return new Z80ReassInstr(
			3,
			"JP", "P", getWord( b1, b2 ) );
      case 0xF3:
	return new Z80ReassInstr( 1, "DI" );
      case 0xF4:
	return new Z80ReassInstr(
			3,
			"CALL", "P", getWord( b1, b2 ) );
      case 0xF5:
	return new Z80ReassInstr( 1, "PUSH", "AF" );
      case 0xF6:
	return new Z80ReassInstr( 2, "OR", getByteText( b1 ) );
      case 0xF7:
	return new Z80ReassInstr( 1, "RST", "30H" );
      case 0xF8:
	return new Z80ReassInstr( 1, "RET", "M" );
      case 0xF9:
	return new Z80ReassInstr( 1, "LD", "SP", "HL" );
      case 0xFA:
	return new Z80ReassInstr(
			3,
			"JP", "M", getWord( b1, b2 ) );
      case 0xFB:
	return new Z80ReassInstr( 1, "EI" );
      case 0xFC:
	return new Z80ReassInstr(
			3,
			"CALL", "M", getWord( b1, b2 ) );
      case 0xFD:
	return ((b1 == 0xDD) || (b1 == 0xED) || (b1 == 0xFD)) ?
	    new Z80ReassInstr( 1, "*NOP" ) :
			reassIXY( "IY", addr, b1, b2, b3 );
      case 0xFE:
	return new Z80ReassInstr( 2, "CP", getByteText( b1 ) );
      case 0xFF:
	return new Z80ReassInstr( 1, "RST", "38H" );
    }
    return null;
  }


  private static Z80ReassInstr reassCB( int b1 )
  {
    if( (b1 >= 0) && (b1 <= 0x3F) ) {
      return new Z80ReassInstr(
			2, getShiftName( b1 ), getRegName( b1 ) );
    }

    String instrName = "";
    switch( (b1 >> 6) & 0x03 ) {
      case 1:
	instrName = "BIT";
	break;
      case 2:
	instrName = "RES";
	break;
      case 3:
	instrName = "SET";
	break;
    }
    return new Z80ReassInstr(
			2,
			instrName,
			String.valueOf( (b1 >> 3) & 0x07 ),
			getRegName( b1 ) );
  }


  private static Z80ReassInstr reassED( int b1, int b2, int b3 )
  {
    switch( b1 ) {
      case 0x40:
	return new Z80ReassInstr( 2, "IN", "B", "(C)" );
      case 0x41:
	return new Z80ReassInstr( 2, "OUT", "(C)", "B" );
      case 0x42:
	return new Z80ReassInstr( 2, "SBC", "HL", "BC" );
      case 0x43:
	return new Z80ReassInstr(
			4,
			"LD", getWord( b2, b3 ), true, "BC" );
      case 0x44:
	return new Z80ReassInstr( 2, "NEG" );
      case 0x45:
	return new Z80ReassInstr( 2, "RETN" );
      case 0x46:
	return new Z80ReassInstr( 2, "IM", "0" );
      case 0x47:
	return new Z80ReassInstr( 2, "LD", "I", "A" );
      case 0x48:
	return new Z80ReassInstr( 2, "IN", "C", "(C)" );
      case 0x49:
	return new Z80ReassInstr( 2, "OUT", "(C)", "C" );
      case 0x4A:
	return new Z80ReassInstr( 2, "ADC", "HL", "BC" );
      case 0x4B:
	return new Z80ReassInstr(
			4,
			"LD", "BC", getWord( b2, b3 ) );
      case 0x4C:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x4D:
	return new Z80ReassInstr( 2, "RETI" );
      case 0x4E:
	return new Z80ReassInstr( 2, "*IM", "0" );
      case 0x4F:
	return new Z80ReassInstr( 2, "LD", "R", "A" );
      case 0x50:
	return new Z80ReassInstr( 2, "IN", "D", "(C)" );
      case 0x51:
	return new Z80ReassInstr( 2, "OUT", "(C)", "D" );
      case 0x52:
	return new Z80ReassInstr( 2, "SBC", "HL", "DE" );
      case 0x53:
	return new Z80ReassInstr(
			4,
			"LD", getWord( b2, b3 ), true, "DE" );
      case 0x54:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x55:
	return new Z80ReassInstr( 2, "*RETN" );
      case 0x56:
	return new Z80ReassInstr( 2, "IM", "1" );
      case 0x57:
	return new Z80ReassInstr( 2, "LD", "A", "I" );
      case 0x58:
	return new Z80ReassInstr( 2, "IN", "E", "(C)" );
      case 0x59:
	return new Z80ReassInstr( 2, "OUT", "(C)", "E" );
      case 0x5A:
	return new Z80ReassInstr( 2, "ADC", "HL", "DE" );
      case 0x5B:
	return new Z80ReassInstr(
			4,
			"LD", "DE", getWord( b2, b3 ), true );
      case 0x5C:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x5D:
	return new Z80ReassInstr( 2, "*RETN" );
      case 0x5E:
	return new Z80ReassInstr( 2, "IM", "2" );
      case 0x5F:
	return new Z80ReassInstr( 2, "LD", "A", "R" );
      case 0x60:
	return new Z80ReassInstr( 2, "IN", "H", "(C)" );
      case 0x61:
	return new Z80ReassInstr( 2, "OUT", "(C)", "H" );
      case 0x62:
	return new Z80ReassInstr( 2, "SBC", "HL", "HL" );
      case 0x63:
	return new Z80ReassInstr(
			4,
			"*LD", getWord( b2, b3 ), true, "HL" );
      case 0x64:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x65:
	return new Z80ReassInstr( 2, "*RETN" );
      case 0x66:
	return new Z80ReassInstr( 2, "*IM", "0" );
      case 0x67:
	return new Z80ReassInstr( 2, "RRD" );
      case 0x68:
	return new Z80ReassInstr( 2, "IN", "L", "(C)" );
      case 0x69:
	return new Z80ReassInstr( 2, "OUT", "(C)", "L" );
      case 0x6A:
	return new Z80ReassInstr( 2, "ADC", "HL", "HL" );
      case 0x6B:
	return new Z80ReassInstr(
			4,
			"*LD", "HL", getWord( b2, b3 ) );
      case 0x6C:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x6D:
	return new Z80ReassInstr( 2, "*RETN" );
      case 0x6E:
	return new Z80ReassInstr( 2, "*IM", "0" );
      case 0x6F:
	return new Z80ReassInstr( 2, "RLD" );
      case 0x70:
	return new Z80ReassInstr( 2, "*IN", "F", "(C)" );
      case 0x71:
	return new Z80ReassInstr( 2, "*OUT", "(C)", "0" );
      case 0x72:
	return new Z80ReassInstr( 2, "SBC", "HL", "SP" );
      case 0x73:
	return new Z80ReassInstr(
			4,
			"LD", getWord( b2, b3 ), true, "SP" );
      case 0x74:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x75:
	return new Z80ReassInstr( 2, "*RETN" );
      case 0x76:
	return new Z80ReassInstr( 2, "*IM", "1" );
      case 0x78:
	return new Z80ReassInstr( 2, "IN", "A", "(C)" );
      case 0x79:
	return new Z80ReassInstr( 2, "OUT", "(C)", "A" );
      case 0x7A:
	return new Z80ReassInstr( 2, "ADC", "HL", "SP" );
      case 0x7B:
	return new Z80ReassInstr(
			4,
			"LD", "SP", getWord( b2, b3 ), true );
      case 0x7C:
	return new Z80ReassInstr( 2, "*NEG" );
      case 0x7D:
	return new Z80ReassInstr( 2, "*RETN" );
      case 0x7E:
	return new Z80ReassInstr( 2, "*IM", "2" );
      case 0xA0:
	return new Z80ReassInstr( 2, "LDI" );
      case 0xA1:
	return new Z80ReassInstr( 2, "CPI" );
      case 0xA2:
	return new Z80ReassInstr( 2, "INI" );
      case 0xA3:
	return new Z80ReassInstr( 2, "OUTI" );
      case 0xA8:
	return new Z80ReassInstr( 2, "LDD" );
      case 0xA9:
	return new Z80ReassInstr( 2, "CPD" );
      case 0xAA:
	return new Z80ReassInstr( 2, "IND" );
      case 0xAB:
	return new Z80ReassInstr( 2, "OUTD" );
      case 0xB0:
	return new Z80ReassInstr( 2, "LDIR" );
      case 0xB1:
	return new Z80ReassInstr( 2, "CPIR" );
      case 0xB2:
	return new Z80ReassInstr( 2, "INIR" );
      case 0xB3:
	return new Z80ReassInstr( 2, "OTIR" );
      case 0xB8:
	return new Z80ReassInstr( 2, "LDDR" );
      case 0xB9:
	return new Z80ReassInstr( 2, "CPDR" );
      case 0xBA:
	return new Z80ReassInstr( 2, "INDR" );
      case 0xBB:
	return new Z80ReassInstr( 2, "OTDR" );
    }
    return new Z80ReassInstr( 2, "*NOP" );
  }


  private static Z80ReassInstr reassIXY(
					String ixy,
					int    addr,
					int    b1,
					int    b2,
					int    b3 )
  {
    Z80ReassInstr instr = null;

    if( (b1 >= 0) && (b1 <= 0x3F) ) {
      instr = reassIXY_00to3F( ixy, addr, b1, b2, b3 );
    }
    else if( ((b1 >= 0x40) && (b1 <= 0x6F))
	     || ((b1 >= 0x78) && (b1 <= 0x7F)) )
    {
      if( ((b1 & 0x0F) == 0x06) || ((b1 & 0x0F) == 0x0E) ) {
	instr = new Z80ReassInstr(
			3,
			"LD",
			getRegName( b1 >> 3 ),
			getIXYMem( ixy, b2 ) );
      } else {
	instr = new Z80ReassInstr(
			2,
			"*LD",
			getIXYRegName( ixy, b1 >> 3, b2 ),
			getIXYRegName( ixy, b1, b2 ) );
      }
    }
    else if( (b1 >= 0x70) && (b1 <= 0x77) ) {
      if( b1 == 0x76 ) {
	instr = new Z80ReassInstr( 2, "*HALT" );
      } else {
	instr = new Z80ReassInstr(
			3,
			"LD",
			getIXYMem( ixy, b2 ),
			getRegName( b1 ) );
      }
    }
    else if( (b1 >= 0x80) && (b1 <= 0xBF) ) {
      String instrName = "";
      int    instCode  = (b1 >> 3) & 0x07;
      switch( instCode ) {
	case 0:
	  instrName = "ADD";
	  break;
	case 1:
	  instrName = "ADC";
	  break;
	case 2:
	  instrName = "SUB";
	  break;
	case 3:
	  instrName = "SBC";
	  break;
	case 4:
	  instrName = "AND";
	  break;
	case 5:
	  instrName = "XOR";
	  break;
	case 6:
	  instrName = "OR";
	  break;
	case 7:
	  instrName = "CP";
	  break;
      }
      if( ((b1 & 0x0F) == 0x06) || ((b1 & 0x0F) == 0x0E) ) {
	if( (instCode == 0) || (instCode == 1) ) {
	  instr = new Z80ReassInstr(
			3,
			instrName,
			"A",
			getIXYMem( ixy, b2 ) );
	} else {
	  instr = new Z80ReassInstr(
			3,
			instrName,
			getIXYMem( ixy, b2 ) );
	}
      } else {
	if( (instCode == 0) || (instCode == 1) ) {
	  instr = new Z80ReassInstr(
			2,
			"*" + instrName,
			"A",
			getIXYRegName( ixy, b1, b2 ) );
	} else {
	  instr = new Z80ReassInstr(
			2,
			"*" + instrName,
			getIXYRegName( ixy, b1, b2 ) );
	}
      }
    } else {
      instr = reassIXY_C0toFF( ixy, addr, b1, b2, b3 );
    }

    return instr;
  }


  private static Z80ReassInstr reassIXY_00to3F(
					String ixy,
					int    addr,
					int    b1,
					int    b2,
					int    b3 )
  {
    switch( b1 ) {
      case 0x00:
	return new Z80ReassInstr( 2, "*NOP" );
      case 0x01:
	return new Z80ReassInstr(
			4,
			"*LD", "BC", createWordText( b2, b3 ) );
      case 0x02:
	return new Z80ReassInstr( 2, "*LD", "(BC)", "A" );
      case 0x03:
	return new Z80ReassInstr( 2, "*INC", "BC" );
      case 0x04:
	return new Z80ReassInstr( 2, "*INC", "B" );
      case 0x05:
	return new Z80ReassInstr( 2, "*DEC", "B" );
      case 0x06:
	return new Z80ReassInstr( 3, "*LD", "B", getByteText( b1 ) );
      case 0x07:
	return new Z80ReassInstr( 2, "*RLCA" );
      case 0x08:
	return new Z80ReassInstr( 2, "*EX", "AF", "AF\'" );
      case 0x09:
	return new Z80ReassInstr( 2, "ADD", ixy, "BC" );
      case 0x0A:
	return new Z80ReassInstr( 2, "*LD", "A", "(BC)" );
      case 0x0B:
	return new Z80ReassInstr( 2, "*DEC", "BC" );
      case 0x0C:
	return new Z80ReassInstr( 2, "*INC", "C" );
      case 0x0D:
	return new Z80ReassInstr( 2, "*DEC", "C" );
      case 0x0E:
	return new Z80ReassInstr( 3, "*LD", "C", getByteText( b2 ) );
      case 0x0F:
	return new Z80ReassInstr( 2, "*RRCA" );
      case 0x10:
	return new Z80ReassInstr(
			3,
			"*DJNZ", getRelAddr( addr + 3, b2 ) );
      case 0x11:
	return new Z80ReassInstr(
			4,
			"*LD", "DE", createWordText( b2, b3 ) );
      case 0x12:
	return new Z80ReassInstr( 2, "*LD", "(DE)", "A" );
      case 0x13:
	return new Z80ReassInstr( 2, "*INC", "DE" );
      case 0x14:
	return new Z80ReassInstr( 2, "*INC", "D" );
      case 0x15:
	return new Z80ReassInstr( 2, "*DEC", "D" );
      case 0x16:
	return new Z80ReassInstr( 3, "*LD", "D", getByteText( b2 ) );
      case 0x17:
	return new Z80ReassInstr( 2, "*RLA" );
      case 0x18:
	return new Z80ReassInstr(
			3,
			"*JR", getRelAddr( addr + 3, b2 ) );
      case 0x19:
	return new Z80ReassInstr( 2, "ADD", ixy, "DE" );
      case 0x1A:
	return new Z80ReassInstr( 2, "*LD", "A", "(DE)" );
      case 0x1B:
	return new Z80ReassInstr( 2, "*DEC", "DE" );
      case 0x1C:
	return new Z80ReassInstr( 2, "*INC", "E" );
      case 0x1D:
	return new Z80ReassInstr( 2, "*DEC", "E" );
      case 0x1E:
	return new Z80ReassInstr( 3, "*LD", "E", getByteText( b2 ) );
      case 0x1F:
	return new Z80ReassInstr( 2, "*RRA" );
      case 0x20:
	return new Z80ReassInstr(
			3,
			"*JR", "NZ", getRelAddr( addr + 3, b2 ) );
      case 0x21:
	return new Z80ReassInstr(
			4,
			"LD", ixy, createWordText( b2, b3 ) );
      case 0x22:
	return new Z80ReassInstr(
			4,
			"LD", getWord( b2, b3 ), true, ixy );
      case 0x23:
	return new Z80ReassInstr( 2, "INC", ixy );
      case 0x24:
	return new Z80ReassInstr( 2, "*INC", ixy + "H" );
      case 0x25:
	return new Z80ReassInstr( 2, "*DEC", ixy + "H" );
      case 0x26:
	return new Z80ReassInstr( 3, "*LD", ixy + "H", getByteText( b2 ) );
      case 0x27:
	return new Z80ReassInstr( 2, "*DAA" );
      case 0x28:
	return new Z80ReassInstr(
			3,
			"*JR", "Z", getRelAddr( addr + 3, b2 ) );
      case 0x29:
	return new Z80ReassInstr( 2, "ADD", ixy, ixy );
      case 0x2A:
	return new Z80ReassInstr(
			4,
			"LD", ixy, getWord( b2, b3 ), true );
      case 0x2B:
	return new Z80ReassInstr( 2, "DEC", ixy );
      case 0x2C:
	return new Z80ReassInstr( 2, "*INC", ixy + "L" );
      case 0x2D:
	return new Z80ReassInstr( 2, "*DEC", ixy + "L" );
      case 0x2E:
	return new Z80ReassInstr( 3, "*LD", ixy + "L", getByteText( b2 ) );
      case 0x2F:
	return new Z80ReassInstr( 2, "*CPL" );
      case 0x30:
	return new Z80ReassInstr(
			3,
			"*JR", "NC", getRelAddr( addr + 3, b2 ) );
      case 0x31:
	return new Z80ReassInstr(
			4,
			"*LD", "SP", getWord( b2, b3 ) );
      case 0x32:
	return new Z80ReassInstr(
			4,
			"*LD", getWord( b2, b3 ), true, "A" );
      case 0x33:
	return new Z80ReassInstr( 2, "*INC", "SP" );
      case 0x34:
	return new Z80ReassInstr( 3, "INC", getIXYMem( ixy, b2 ) );
      case 0x35:
	return new Z80ReassInstr( 3, "DEC", getIXYMem( ixy, b2 ) );
      case 0x36:
	return new Z80ReassInstr(
			4, "LD", getIXYMem( ixy, b2 ), getByteText( b3 ) );
      case 0x37:
	return new Z80ReassInstr( 2, "*SCF" );
      case 0x38:
	return new Z80ReassInstr(
			3,
			"*JR", "C", getRelAddr( addr + 3, b2 ) );
      case 0x39:
	return new Z80ReassInstr( 2, "ADD", ixy, "SP" );
      case 0x3A:
	return new Z80ReassInstr(
			4,
			"*LD", "A", getWord( b2, b3 ), true );
      case 0x3B:
	return new Z80ReassInstr( 2, "*DEC", "SP" );
      case 0x3C:
	return new Z80ReassInstr( 2, "*INC", "A" );
      case 0x3D:
	return new Z80ReassInstr( 2, "*DEC", "A" );
      case 0x3E:
	return new Z80ReassInstr( 3, "*LD", "A", getByteText( b2 ) );
      case 0x3F:
	return new Z80ReassInstr( 2, "*CCF" );
    }
    return null;
  }


  private static Z80ReassInstr reassIXY_C0toFF(
					String ixy,
					int    addr,
					int    b1,
					int    b2,
					int    b3 )
  {
    switch( b1 ) {
      case 0xC0:
	return new Z80ReassInstr( 2, "*RET", "NZ" );
      case 0xC1:
	return new Z80ReassInstr( 2, "*POP", "BC" );
      case 0xC2:
	return new Z80ReassInstr(
			4,
			"*JP", "NZ", getWord( b2, b3 ) );
      case 0xC3:
	return new Z80ReassInstr(
			4,
			"*JP", getWord( b2, b3 ) );
      case 0xC4:
	return new Z80ReassInstr(
			4,
			"*CALL", "NZ", getWord( b2, b3 ) );
      case 0xC5:
	return new Z80ReassInstr( 2, "*PUSH", "BC" );
      case 0xC6:
	return new Z80ReassInstr( 3, "*ADD", "A", getByteText( b2 ) );
      case 0xC7:
	return new Z80ReassInstr( 2, "*RST", "00H" );
      case 0xC8:
	return new Z80ReassInstr( 2, "*RET", "Z" );
      case 0xC9:
	return new Z80ReassInstr( 2, "*RET" );
      case 0xCA:
	return new Z80ReassInstr(
			4,
			"*JP", "Z", getWord( b2, b3 ) );
      case 0xCB:
	return reassIXY_CB( ixy, b2, b3 );
      case 0xCC:
	return new Z80ReassInstr(
			4,
			"*CALL", "Z", getWord( b2, b3 ) );
      case 0xCD:
	return new Z80ReassInstr(
			4,
			"*CALL", getWord( b2, b3 ) );
      case 0xCE:
	return new Z80ReassInstr( 3, "*ADC", "A", getByteText( b2 ) );
      case 0xCF:
	return new Z80ReassInstr( 2, "*RST", "08H" );
      case 0xD0:
	return new Z80ReassInstr( 2, "*RET", "NC" );
      case 0xD1:
	return new Z80ReassInstr( 2, "*POP", "DE" );
      case 0xD2:
	return new Z80ReassInstr(
			4,
			"*JP", "NC", getWord( b2, b3 ) );
      case 0xD3:
	return new Z80ReassInstr(
			3, "*OUT", "(" + getByteText( b2 ) + ")", "A" );
      case 0xD4:
	return new Z80ReassInstr(
			4,
			"*CALL", "NC", getWord( b2, b3 ) );
      case 0xD5:
	return new Z80ReassInstr( 2, "*PUSH", "DE" );
      case 0xD6:
	return new Z80ReassInstr( 3, "*SUB", getByteText( b2 ) );
      case 0xD7:
	return new Z80ReassInstr( 2, "*RST", "10H" );
      case 0xD8:
	return new Z80ReassInstr( 2, "*RET", "C" );
      case 0xD9:
	return new Z80ReassInstr( 2, "*EXX" );
      case 0xDA:
	return new Z80ReassInstr(
			4,
			"*JP", "C", getWord( b2, b3 ) );
      case 0xDB:
	return new Z80ReassInstr(
			3, "*IN", "A", "(" + getByteText( b2 ) + ")" );
      case 0xDC:
	return new Z80ReassInstr(
			4,
			"*CALL", "C", getWord( b2, b3 ) );
      case 0xDD:
	return new Z80ReassInstr( 2, "*?" );
      case 0xDE:
	return new Z80ReassInstr( 3, "*SBC", getByteText( b2 ) );
      case 0xDF:
	return new Z80ReassInstr( 2, "*RST", "18H" );
      case 0xE0:
	return new Z80ReassInstr( 2, "*RET", "PO" );
      case 0xE1:
	return new Z80ReassInstr( 2, "POP", ixy );
      case 0xE2:
	return new Z80ReassInstr(
			4,
			"*JP", "PO", getWord( b2, b3 ) );
      case 0xE3:
	return new Z80ReassInstr( 2, "EX", "(SP)", ixy );
      case 0xE4:
	return new Z80ReassInstr(
			4,
			"*CALL", "PO", getWord( b2, b3 ) );
      case 0xE5:
	return new Z80ReassInstr( 2, "PUSH", ixy );
      case 0xE6:
	return new Z80ReassInstr( 3, "*AND", getByteText( b2 ) );
      case 0xE7:
	return new Z80ReassInstr( 2, "*RST", "20H" );
      case 0xE8:
	return new Z80ReassInstr( 2, "*RET", "PE" );
      case 0xE9:
	return new Z80ReassInstr( 2, "JP", "(" + ixy + ")" );
      case 0xEA:
	return new Z80ReassInstr(
			4,
			"*JP", "PE", getWord( b2, b3 ) );
      case 0xEB:
	return new Z80ReassInstr( 2, "*EX", "DE", "HL" );
      case 0xEC:
	return new Z80ReassInstr(
			4,
			"*CALL", "PE", getWord( b2, b3 ) );
      case 0xED:
	return new Z80ReassInstr( 2, "*?" );
      case 0xEE:
	return new Z80ReassInstr( 3, "*XOR", getByteText( b2 ) );
      case 0xEF:
	return new Z80ReassInstr( 2, "*RST", "28H" );
      case 0xF0:
	return new Z80ReassInstr( 2, "*RET", "P" );
      case 0xF1:
	return new Z80ReassInstr( 2, "*POP", "AF" );
      case 0xF2:
	return new Z80ReassInstr(
			4,
			"*JP", "P", getWord( b2, b3 ) );
      case 0xF3:
	return new Z80ReassInstr( 2, "*DI" );
      case 0xF4:
	return new Z80ReassInstr(
			4,
			"*CALL", "P", getWord( b2, b3 ) );
      case 0xF5:
	return new Z80ReassInstr( 2, "*PUSH", "AF" );
      case 0xF6:
	return new Z80ReassInstr( 3, "*OR", getByteText( b2 ) );
      case 0xF7:
	return new Z80ReassInstr( 2, "*RST", "30H" );
      case 0xF8:
	return new Z80ReassInstr( 2, "*RET", "M" );
      case 0xF9:
	return new Z80ReassInstr( 2, "LD", "SP", ixy );
      case 0xFA:
	return new Z80ReassInstr(
			4,
			"*JP", "M", getWord( b2, b3 ) );
      case 0xFB:
	return new Z80ReassInstr( 2, "*EI" );
      case 0xFC:
	return new Z80ReassInstr(
			4,
			"*CALL", "M", getWord( b2, b3 ) );
      case 0xFD:
	return new Z80ReassInstr( 2, "*?" );
      case 0xFE:
	return new Z80ReassInstr( 3, "*CP", getByteText( b2 ) );
      case 0xFF:
	return new Z80ReassInstr( 2, "RST", "38H" );
    }
    return null;
  }


  private static Z80ReassInstr reassIXY_CB(
					String ixy,
					int    b2,
					int    b3 )
  {
    if( (b3 >= 0x00) && (b3 <= 0x3F) ) {
      if( ((b3 & 0x0F) == 0x06) || ((b3 & 0x0F) == 0x0E) ) {
	return new Z80ReassInstr(
			4, getShiftName( b3 ), getIXYMem( ixy, b2 ) );
      } else {
	return new Z80ReassInstr(
			4,
			"*LD",
			getRegName( b3 ),
			getShiftName( b3 ) + getIXYMem( ixy, b2 ) );
      }
    }

    String instrName = "BIT";
    if( (b3 >= 0x40) && (b3 <= 0x7F) ) {
      if( ((b3 & 0x0F) != 0x06) && ((b3 & 0x0F) != 0x0E) )
	instrName = "*BIT";

      return new Z80ReassInstr(
			4,
			instrName,
			String.valueOf( (b3 >> 3) & 0x07 ),
			getIXYMem( ixy, b2 ) );
    }

    instrName = (b3 < 0xC0) ? "RES" : "SET";
    if( ((b3 & 0x0F) == 0x06) || ((b3 & 0x0F) == 0x0E) ) {
      return new Z80ReassInstr(
			4,
			instrName,
			String.valueOf( (b3 >> 3) & 0x07 ),
			getIXYMem( ixy, b2 ) );
    }
    return new Z80ReassInstr(
			4,
			"*LD",
			getRegName( b3 ),
			String.format(
				"%s %d,%s",
				instrName,
				(b3 >> 3) & 0x07,
				getIXYMem( ixy, b2 ) ) );
  }


  private static String createWordText( int value )
  {
    value &= 0xFFFF;
    return String.format(
			"%s%04XH",
			(value >= 0xA000 ? "0" : ""),
			value );
  }


  private static String createWordText( int l, int h )
  {
    return String.format( "%s%02X%02XH", h >= 0xA0 ? "0" : "", h, l );
  }


  private static String getRegName( int code )
  {
    switch( code & 0x07 ) {
      case 0:
	return "B";
      case 1:
	return "C";
      case 2:
	return "D";
      case 3:
	return "E";
      case 4:
	return "H";
      case 5:
	return "L";
      case 6:
	return "(HL)";
      case 7:
	return "A";
    }
    return "?";
  }


  private static String getIXYRegName(
				String ixy,
				int    code,
				int    d )
  {
    switch( code & 0x07 ) {
      case 0:
	return "B";
      case 1:
	return "C";
      case 2:
	return "D";
      case 3:
	return "E";
      case 4:
	return ixy + "H";
      case 5:
	return ixy + "L";
      case 6:
	return getIXYMem( ixy, d );
      case 7:
	return "A";
    }
    return "?";
  }


  private static String getShiftName( int code )
  {
    switch( code & 0x38 ) {
      case 0x00:
	return "RLC";
      case 0x08:
	return "RRC";
      case 0x10:
	return "RL";
      case 0x18:
	return "RR";
      case 0x20:
	return "SLA";
      case 0x28:
	return "SRA";
      case 0x30:
	return "*SLL";
      case 0x38:
	return "SRL";
    }
    return "?";
  }


  private static String getByteText( int v )
  {
    v &= 0xFF;
    return String.format( "%s%02XH", (v >= 0xA0 ? "0" : ""), v );
  }


  private static String getIndirectByteText( int v )
  {
    v &= 0xFF;
    return String.format( "(%s%02XH)", (v >= 0xA0 ? "0" : ""), v );
  }


  private static String getIXYMem( String ixy, int d )
  {
    return "(" + ixy + "+" + getByteText( d ) + ")";
  }


  private static int getRelAddr( int nextInstAddr, int d )
  {
    return nextInstAddr + (int) ((byte) d);
  }


  private static int getWord( int l, int h )
  {
    return ((h << 8) & 0xFF00) | (l & 0x00FF);
  }
}
