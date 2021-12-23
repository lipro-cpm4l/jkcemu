/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Argument eines Assemblerbefehls
 */

package jkcemu.programming.assembler;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgUtil;


public class AsmArg
{
  private int    argLen;
  private String argText;
  private String upperText;
  private String indirectText;


  public AsmArg( String argText )
  {
    // weisse Leerzeichen am Anfang und Ende entfernen
    int len = argText.length();
    int pos = 0;
    while( pos < len ) {
      if( !PrgUtil.isWhitespace( argText.charAt( pos ) ) ) {
	break;
      }
      pos++;
    }
    if( pos > 0 ) {
      argText = argText.substring( pos );
    }
    len = argText.length();
    pos = len - 1;
    while( pos >= 0 ) {
      if( !PrgUtil.isWhitespace( argText.charAt( pos ) ) ) {
	break;
      }
      --pos;
    }
    if( pos < 0 ) {
      argText = "";
    } else if( pos < (len - 1) ) {
      argText = argText.substring( 0, pos + 1 );
    }
    this.argText      = argText;
    this.argLen       = this.argText.length();
    this.upperText    = this.argText.toUpperCase();
    this.indirectText = null;
    if( this.argLen > 1 ) {
      if( (this.argText.charAt( 0 ) == '(')
	  && (this.argText.charAt( this.argLen - 1 ) == ')') )
      {
	this.indirectText = this.upperText.substring( 1, this.argLen - 1 );
      }
    }
  }


  public String createFormattedText()
  {
    CharacterIterator iter = new StringCharacterIterator( this.argText );
    StringBuilder     buf  = new StringBuilder(
					Math.max( iter.getEndIndex(), 16 ) );

    char ch = iter.first();
    while( ch != CharacterIterator.DONE ) {
      if( ch == '\'' ) {
	buf.append( ch );
	ch = iter.next();
	while( (ch != CharacterIterator.DONE) && (ch != '\'') ) {
	  buf.append( ch );
	  ch = iter.next();
	}
	if( ch == '\'' ) {
	  buf.append( ch );
	  ch = iter.next();
	}
      } else {
	buf.append( Character.toUpperCase( ch ) );
	ch = iter.next();
      }
    }
    return buf.toString();
  }


  public boolean equalsUpper( String s )
  {
    return this.upperText.equals( s );
  }


  public String getIndirectText() throws PrgException
  {
    if( this.indirectText == null ) {
      throw new PrgException( "Indirekte Adressierung erwartet" );
    }
    return this.indirectText;
  }


  /*
   * Die Methode extrahiert das "d" aus "(IX+d)" und "(IY+d)".
   */
  public String getIndirectIXYDist() throws PrgException
  {
    String text = getIndirectText();
    String rv   = "";
    if( !this.indirectText.startsWith( "IX" )
	&& !this.indirectText.startsWith( "IY" ) )
    {
      throw new PrgException(
		"Indirekte Adressierung mit Indexregister erwartet" );
    }
    if( this.indirectText.length() > 2 ) {
      rv = this.indirectText.substring( 2 );
      if( !rv.startsWith( "+" ) && !rv.startsWith( "-" ) ) {
	throw new PrgException(
		"Ung\u00FCltige Distanzangabe bei"
			+ " indirekter Adressierung mit Indexregister" );
      }
    }
    return rv;
  }


  public int getReg8Code() throws PrgException
  {
    int rv = -1;
    if( equalsUpper( "A" ) ) {
      rv = 7;
    }
    else if( equalsUpper( "B" ) ) {
      rv = 0;
    }
    else if( equalsUpper( "C" ) ) {
      rv = 1;
    }
    else if( equalsUpper( "D" ) ) {
      rv = 2;
    }
    else if( equalsUpper( "E" ) ) {
      rv = 3;
    }
    else if( equalsUpper( "H" ) ) {
      rv = 4;
    }
    else if( equalsUpper( "L" ) ) {
      rv = 5;
    }
    else if( equalsUpper( "M" ) ) {
      rv = 6;
    }
    else if( isIndirectHL() ) {
      rv = 6;
    }
    if( (rv < 0) || (rv > 7) ) {
      throw new PrgException( "Register oder (HL) erwartet" );
    }
    return rv;
  }


  public boolean isIndirectAddr()
  {
    boolean rv = false;
    if( this.indirectText != null ) {
      if( !isRegister( this.indirectText ) ) {
	rv = true;
	if( this.indirectText.startsWith( "IX" )
	    || this.indirectText.startsWith( "IY" ) )
	{
	  if( this.indirectText.length() > 2 ) {
	    char ch = this.indirectText.charAt( 2 );
	    if( (ch == '+') || (ch =='-') )
	      rv = false;
	  }
	}
      }
    }
    return rv;
  }

  public boolean isIndirectBC()
  {
    return this.indirectText != null ?
			this.indirectText.equals( "BC" )
			: false;
  }


  public boolean isIndirectDE()
  {
    return this.indirectText != null ?
			this.indirectText.equals( "DE" )
			: false;
  }


  public boolean isIndirectHL()
  {
    return this.indirectText != null ?
			this.indirectText.equals( "HL" )
			: false;
  }


  public boolean isIndirectIX()
  {
    return this.indirectText != null ?
			this.indirectText.equals( "IX" )
			: false;
  }


  public boolean isIndirectIY()
  {
    return this.indirectText != null ?
			this.indirectText.equals( "IY" )
			: false;
  }


  public boolean isIndirectIXDist()
  {
    return isIndirectDist( "IX" );
  }


  public boolean isIndirectIYDist()
  {
    return isIndirectDist( "IY" );
  }


  public boolean isIndirectSP()
  {
    return this.indirectText != null ?
			this.indirectText.equals( "SP" )
			: false;
  }


  public boolean isRegAtoL()
  {
    return equalsUpper( "A" ) || isRegBtoL();
  }


  public boolean isRegBtoL()
  {
    return equalsUpper( "B" )
		|| equalsUpper( "C" )
		|| equalsUpper( "D" )
		|| equalsUpper( "E" )
		|| equalsUpper( "H" )
		|| equalsUpper( "L" );
  }


  public static boolean isRegister( String text )
  {
    return text.equals( "A" )
		|| text.equals( "B" )
		|| text.equals( "C" )
		|| text.equals( "D" )
		|| text.equals( "E" )
		|| text.equals( "H" )
		|| text.equals( "L" )
		|| text.equals( "I" )
		|| text.equals( "R" )
		|| text.equals( "AF" )
		|| text.equals( "BC" )
		|| text.equals( "DE" )
		|| text.equals( "HL" )
		|| text.equals( "IX" )
		|| text.equals( "IY" )
		|| text.equals( "AF\'" )
		|| text.equals( "BC\'" )
		|| text.equals( "DE\'" )
		|| text.equals( "HL\'" );
  }


  public static boolean isFlagCondition( String text )
  {
    return text.equals( "NZ" )
		|| text.equals( "Z" )
		|| text.equals( "NC" )
		|| text.equals( "C" )
		|| text.equals( "PO" )
		|| text.equals( "PE" )
		|| text.equals( "P" )
		|| text.equals( "M" );
  }


  public static boolean isUndocRegister( String text )
  {
    return text.equals( "IXH" )
		|| text.equals( "IXL" )
		|| text.equals( "IYH" )
		|| text.equals( "IYL" )
		|| text.equals( "HX" )
		|| text.equals( "LX" )
		|| text.equals( "HY" )
		|| text.equals( "LY" );
  }


  public String toUpperString()
  {
    return this.upperText;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.argText;
  }


	/* --- private Methoden --- */

  private boolean isIndirectDist( String reg )
  {
    boolean rv = false;
    if( this.indirectText != null ) {
      if( this.indirectText.startsWith( reg ) ) {
	rv      = true;
	int len = reg.length();
	if( this.indirectText.length() > len ) {
	  char ch = this.indirectText.charAt( len );
	  if( (ch != '+') && (ch != '-') )
	    rv = false;
	}
      }
    }
    return rv;
  }
}
