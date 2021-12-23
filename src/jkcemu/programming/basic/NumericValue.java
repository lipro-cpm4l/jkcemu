/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abbildung einer BASIC-Zahl
 */

package jkcemu.programming.basic;

import java.text.CharacterIterator;
import jkcemu.programming.PrgException;


public class NumericValue
{
  private BasicCompiler.DataType dataType;
  private long                   d6Bits;
  private long                   lValue;


  public static NumericValue checkLiteral(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			BasicCompiler.DataType prefDataType,
			boolean                enableWarnings )
							throws PrgException
						
  {
    NumericValue rv = null;
    char         ch = BasicUtil.skipSpaces( iter );
    if( (ch >= '0') && (ch <= '9') ) {
      NumericValue value = readNumber(
				compiler,
				iter,
				prefDataType,
				enableWarnings );
      if( value == null ) {
	BasicUtil.throwNumberExpected();
      }
      rv = value;
    }
    else if( ch == '&' ) {
      ch = iter.next();
      if( (ch == 'B') || (ch == 'b') ) {
	ch = iter.next();
	if( (ch == '0') || (ch == '1') ) {
	  long value = 0L;
	  while( (ch == '0') || (ch == '1') ) {
	    value <<= 1;
	    if( ch == '1' ) {
	      value |= 1;
	    }
	    if( value > 0xFFFFFFFFL ) {
	      BasicUtil.throwNumberTooBig();
	    }
	    ch = iter.next();
	  }
	  if( (ch == 'L') || (ch == 'l') ) {
	    iter.next();
	    prefDataType = BasicCompiler.DataType.INT4;
	  }
	  rv = fromUnsignedValue( value, prefDataType );
	} else {
	  throw new PrgException( "0 oder 1 erwartet" );
	}
      } else if( (ch == 'H') || (ch == 'h') ) {
	iter.next();
	Number value = BasicUtil.readHex( iter );
	if( value == null ) {
	  BasicUtil.throwHexDigitExpected();
	}
	ch = iter.current();
	if( (ch == 'L') || (ch == 'l') ) {
	  iter.next();
	  prefDataType = BasicCompiler.DataType.INT4;
	}
	rv = fromUnsignedValue( value.longValue(), prefDataType );
      } else {
	throw new PrgException( "B oder H erwartet" );
      }
    }
    else if( ch == '\'' ) {
      ch = iter.next();
      iter.next();
      if( enableWarnings
	  && compiler.getBasicOptions().getWarnNonAsciiChars()
	  && ((ch < '\u0020') || (ch > '\u007F')) )
      {
	compiler.putWarningNonAsciiChar( ch );
      }
      BasicUtil.parseToken( iter, '\'' );
      BasicUtil.check8BitChar( ch );
      rv = from( (long) ch, prefDataType );
    }
    return rv;
  }


  public long dec6Bits()
  {
    return this.d6Bits;
  }


  public static NumericValue from( int value ) throws PrgException
  {
    return from( (long) value, BasicCompiler.DataType.INT2 );
  }


  public static NumericValue from(
				long                   value,
				BasicCompiler.DataType prefDataType )
					throws PrgException
  {
    long d6Bits = toD6Bits( value );
    BasicCompiler.DataType dataType = null;
    if( prefDataType != null ) {
      if( prefDataType.equals( BasicCompiler.DataType.INT2 )
	  && isInt2( value ) )
      {
	dataType = BasicCompiler.DataType.INT2;
      }
      else if( prefDataType.equals( BasicCompiler.DataType.INT4 )
	       && isInt4( value ) )
      {
	dataType = BasicCompiler.DataType.INT4;
      }
      else if( prefDataType.equals( BasicCompiler.DataType.DEC6 )
	       && (d6Bits != -1) )
      {
	dataType = BasicCompiler.DataType.DEC6;
      }
    }
    if( dataType == null ) {
      if( isInt2( value ) ) {
	dataType = BasicCompiler.DataType.INT2;
      } else if( isInt4( value ) ) {
	dataType = BasicCompiler.DataType.INT4;
      } else if( d6Bits == -1 ) {
	throw new PrgException( "Betrag zu gro\u00DF" );
      }
    }
    return new NumericValue( dataType, d6Bits, value );
  }


  public static NumericValue fromDec6Bits( long dec6Bits )
  {
    long lValue = 0;
    if( (dec6Bits & 0x700000000000L) == 0 ) {
      long d = dec6Bits;
      for( int i = 0; i < 11; i++ ) {
	lValue = (lValue * 10) | (d & 0x0F);
	d >>= 4;
      }
      if( (dec6Bits & 0x800000000000L) != 0 ) {
	lValue = -lValue;
      }
    }
    return new NumericValue(
			BasicCompiler.DataType.DEC6,
			dec6Bits,
			lValue );
  }


  public BasicCompiler.DataType getDataType()
  {
    return this.dataType;
  }


  public int intValue()
  {
    return (int) this.lValue;
  }


  public long longValue()
  {
    return this.lValue;
  }


  public NumericValue negate()
  {
    return new NumericValue(
			this.dataType,
			this.d6Bits ^ 0x800000000000L,
			-this.lValue );
  }


  public static NumericValue readNumber(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			BasicCompiler.DataType prefDataType )
							throws PrgException
  {
    return readNumber( compiler, iter, prefDataType, true );
  }


  public void writeCode_LD_Reg_DirectValue( BasicCompiler compiler )
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    switch( this.dataType ) {
      case INT2:
	asmOut.append_LD_HL_nn( (int) this.lValue );
	break;
      case INT4:
	asmOut.append_LD_DEHL_nnnn( this.lValue );
	break;
      case DEC6:
	asmOut.append( "\tCALL\tD6_LD_ACCU_NNNNNN\n"
			+ "\tDB\t" );
	long d6Bits = this.d6Bits;
	for( int i = 0; i < 6; i++ ) {
	  if( i > 0 ) {
	    asmOut.append( ',' );
	  }
	  asmOut.appendHex2( (int) (d6Bits >> 40) );
	  d6Bits <<= 8;
	}
	asmOut.newLine();
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_NNNNNN );
	break;
    }
  }


	/* --- private Methoden --- */

  private static NumericValue fromUnsignedValue(
				long                   value,
				BasicCompiler.DataType prefDataType )
					throws PrgException
  {
    long d6Bits = toD6Bits( value );
    BasicCompiler.DataType dataType = null;
    if( prefDataType != null ) {
      if( prefDataType.equals( BasicCompiler.DataType.INT2 )
	  && ((value & ~0xFFFF) == 0) )
      {
	dataType = BasicCompiler.DataType.INT2;
      }
      else if( prefDataType.equals( BasicCompiler.DataType.INT4 )
	  && ((value & ~0xFFFFFFFF) == 0) )
      {
	dataType = BasicCompiler.DataType.INT4;
      }
      else if( prefDataType.equals( BasicCompiler.DataType.DEC6 )
	       && (d6Bits != -1) )
      {
	dataType = BasicCompiler.DataType.DEC6;
      }
    }
    if( dataType == null ) {
      if( ((value & ~0xFFFF) == 0) ) {
	dataType = BasicCompiler.DataType.INT2;
      } else if( ((value & ~0xFFFFFFFF) == 0) ) {
	dataType = BasicCompiler.DataType.INT4;
      } else if( d6Bits == -1 ) {
	throw new PrgException( "Betrag zu gro\u00DF" );
      }
    }
    return new NumericValue( dataType, d6Bits, value );
  }


  private static NumericValue readNumber(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			BasicCompiler.DataType prefDataType,
			boolean                enableWarnings )
							throws PrgException
  {
    boolean point    = false;
    boolean valid    = false;
    int     nIgnored = 0;
    int     begPos   = iter.getIndex();
    int     prec     = 0;
    int     scale    = 0;
    long    d6Bits   = 0;
    long    lValue   = 0;
    char    ch       = BasicUtil.skipSpaces( iter );
    while( ch == '0' ) {
      valid = true;
      ch    = iter.next();
    }
    if( (ch >= '0') && (ch <= '9') ) {
      prec++;
      valid  = true;
      lValue = ch - '0';
      d6Bits = lValue;
      ch     = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	prec++;
	if( prec > 11 ) {
	  BasicUtil.throwNumberTooBig();
	}
	int cValue = ch - '0';
	lValue     = (lValue * 10) + cValue;
	d6Bits = (d6Bits << 4) | cValue;
	ch     = iter.next();
      }
    }
    if( ch == '.' ) {
      if( point ) {
	valid = false;
      } else {
	point = true;
	ch    = iter.next();
	if( (ch >= '0') && (ch <= '9') ) {
	  valid = true;
	  while( (ch >= '0') && (ch <= '9') ) {
	    if( (prec < 11) && (scale < 7) ) {
	      d6Bits = (d6Bits << 4) | (ch - '0');
	      prec++;
	      scale++;
	    } else {
	      nIgnored++;
	    }
	    ch = iter.next();
	  }
	  while( (scale > 0) && ((d6Bits & 0x0F) == 0) ) {
	    d6Bits >>= 4;
	    --scale;
	  }
	  d6Bits |= ((long) scale << 44);
	} else {
	  valid = false;
	}
      }
    }
    NumericValue rv = null;
    if( valid ) {
      if( (ch == 'L') || (ch == 'l') ) {
	ch = iter.next();
	if( point || ((d6Bits & 0x700000000000L) != 0) ) {
	  throw new PrgException( "Literal mit Dezimalpunkt und"
			+ " abschlie\u00DFendem \'L\' nicht m\u00F6glich" );
	}
	rv = new NumericValue( BasicCompiler.DataType.INT4, d6Bits, lValue );
      } else if( (ch == 'D') || (ch == 'd') ) {
	ch = iter.next();
	rv = new NumericValue( BasicCompiler.DataType.DEC6, d6Bits, lValue );
      } else {
	BasicCompiler.DataType dataType = BasicCompiler.DataType.DEC6;
	if( !point && ((d6Bits & 0x700000000000L) == 0) ) {
	  if( prefDataType != null ) {
	    if( prefDataType.equals( BasicCompiler.DataType.INT2 )
		&& isInt2( lValue ) )
	    {
	      dataType = BasicCompiler.DataType.INT2;
	    }
	    else if( prefDataType.equals( BasicCompiler.DataType.INT4 )
		     && isInt4( lValue ) )
	    {
	      dataType = BasicCompiler.DataType.INT4;
	    }
	  } else {
	    if( isInt2( lValue ) ) {
	      dataType = BasicCompiler.DataType.INT2;
	    } else if( isInt4( lValue ) ) {
	      dataType = BasicCompiler.DataType.INT4;
	    }
	  }
	}
	rv = new NumericValue( dataType, d6Bits, lValue );
      }
    }
    if( compiler.getBasicOptions().getWarnTooManyDigits()
	&& (rv != null) && (nIgnored > 0) )
    {
      compiler.putWarningLastDigitsIgnored( nIgnored );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private NumericValue(
		BasicCompiler.DataType dataType,
		long                   d6Bits,
		long                   lValue )
  {
    this.dataType = dataType;
    this.d6Bits   = d6Bits;
    this.lValue   = lValue;
  }

  private NumericValue( boolean point, long d6Bits, long lValue )
  {
    if( point || ((d6Bits != -1L) && ((d6Bits & 0x700000000000L) != 0)) ) {
      this.dataType = BasicCompiler.DataType.DEC6;
    } else {
      if( isInt2( lValue ) ) {
	this.dataType = BasicCompiler.DataType.INT2;
      } else {
	this.dataType = BasicCompiler.DataType.INT4;
      }
    }
    this.d6Bits = d6Bits;
    this.lValue = lValue;
  }


  private static boolean isInt2( long lValue )
  {
    return (lValue >= -32768) && (lValue <= 32767);
  }


  private static boolean isInt4( long lValue )
  {
    return (lValue >= -2147483648) && (lValue <= 2147483647);
  }


  private static long toD6Bits( long value )
  {
    long d6Bits = -1L;
    if( (value >= -99999999999L) && (value <= 99999999999L) ) {
      boolean neg = false;
      long    v   = value;
      if( v < 0 ) {
	neg = true;
	v -= value;
      }
      d6Bits = 0;
      for( int i = 0; i < 11; i++ ) {
	d6Bits = (((v % 10) << 40) & 0x0F0000000000L) | (d6Bits >> 4);
	v /= 10;
      }
      if( neg ) {
	d6Bits |= 0x800000000000L;
      }
    }
    return d6Bits;
  }
}
