/*
 * (c) 2008-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Zerlegte Assemblerzeile
 */

package jkcemu.programming.assembler;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgUtil;


public class AsmLine
{
  private boolean  commentAtStart;
  private String   comment;
  private String   label;
  private int      begOfInst;
  private String   instruction;
  private AsmArg[] args;
  private int      argPos;


  public static AsmLine scanLine(
				Z80Assembler asm,
				String       text,
				boolean      labelsCaseSensitive )
							throws PrgException
  {
    AsmLine rv = null;
    if( text != null ) {
      CharacterIterator iter = new StringCharacterIterator( text );
      char              ch   = iter.first();
      if( ch != CharacterIterator.DONE ) {
	boolean                commentAtStart = false;
	String                 comment        = null;
	String                 label          = null;
	int                    begOfInst      = -1;
	String                 instruction    = null;
	java.util.List<AsmArg> args           = null;

	if( ch == ';' ) {
	  commentAtStart = true;
	} else {

	  // Marke parsen
	  if( AsmLabel.isIdentifierStart( ch ) ) {
	    StringBuilder buf = new StringBuilder();
	    buf.append( ch );
	    ch = iter.next();
	    while( AsmLabel.isIdentifierPart( ch ) ) {
	      buf.append( ch );
	      ch = iter.next();
	    }
	    if( (ch != CharacterIterator.DONE)
		&& !PrgUtil.isWhitespace( ch )
		&& (ch != ':') && (ch != ';') )
	    {
	      throwInvalidCharInLabel( ch );
	    }
	    // Pruefen auf ein Schluesselwort fuer eine Bedingung
	    String s = buf.toString().toUpperCase();
	    if( s.equals( "IF" ) || s.equals( "IFT" )
		|| s.equals( "IFE" ) || s.equals( "IFF" )
		|| s.equals( "IF1" ) || s.equals( "IF2" )
		|| s.equals( "IFDEF" ) || s.equals( "IFNDEF" )
		|| s.equals( "ELSE" ) || s.equals( "ENDIF" ) )
	    {
	      ch = iter.first();
	    } else {
	      if( ch == ':' ) {
		ch = iter.next();
	      }
	      label             = buf.toString();
	      String upperLabel = label.toUpperCase();
	      if( !labelsCaseSensitive ) {
		label = upperLabel;
	      }
	      if( asm != null ) {
		boolean reserved = (asm.isReservedWord( upperLabel )
			    || AsmArg.isRegister( upperLabel )
			    || AsmArg.isFlagCondition( upperLabel )
			    || ExprParser.isReservedWord( upperLabel ));
		if( !reserved && asm.getOptions().getAllowUndocInst() ) {
		  reserved = AsmArg.isUndocRegister( upperLabel );
		}
		if( reserved ) {
		  asm.putWarning(
			"Marke \'" + label + "\': Reserviertes Wort" );
		}
	      }
	    }
	  } else {
	    if( (ch != CharacterIterator.DONE)
		&& (ch != '.') && !PrgUtil.isWhitespace( ch ) )
	    {
	      throwInvalidCharInLabel( ch );
	    }
	  }

	  // Befehl extrahieren
	  while( (ch != CharacterIterator.DONE)
		 && PrgUtil.isWhitespace( ch ) )
	  {
	    ch = iter.next();
	  }
	  if( (ch != CharacterIterator.DONE) && (ch != ';') ) {
	    begOfInst         = iter.getIndex();
	    StringBuilder buf = new StringBuilder();
	    while( (ch != CharacterIterator.DONE)
		   && !PrgUtil.isWhitespace( ch )
		   && (ch != ';') )
	    {
	      buf.append( Character.toUpperCase( ch ) );
	      ch = iter.next();
	    }
	    if( buf.length() > 0 ) {
	      instruction = buf.toString();
	    } else {
	      begOfInst = -1;
	    }
	  }

	  // Argumente extrahieren
	  String argText = nextArgText( iter );
	  while( argText != null ) {
	    if( args == null ) {
	      args = new ArrayList<>();
	    }
	    args.add( new AsmArg( argText ) );
	    argText = nextArgText( iter );
	  }
	}

	// Kommentar scannen und Objekt anlegen
	ch = iter.current();
	while( (ch != CharacterIterator.DONE)
	       && PrgUtil.isWhitespace( ch ) )
	{
	  ch = iter.next();
	}
	if( ch == ';' ) {
	  ch = iter.next();
	  while( (ch != CharacterIterator.DONE)
		 && PrgUtil.isWhitespace( ch ) )
	  {
	    ch = iter.next();
	  }
	  if( ch != CharacterIterator.DONE ) {
	    StringBuilder buf = new StringBuilder( 64 );
	    buf.append( ch );
	    ch = iter.next();
	    while( ch != CharacterIterator.DONE ) {
	      buf.append( ch );
	      ch = iter.next();
	    }
	    comment = buf.toString();
	  }
	}
	rv = new AsmLine(
			commentAtStart,
			comment,
			label,
			begOfInst,
			instruction,
			args != null ?
				args.toArray( new AsmArg[ args.size() ] )
				: null );
      }
    }
    return rv;
  }


  public void appendFormattedTo( StringBuilder buf )
  {
    int     tabsToComment = 3;
    boolean hasLabel      = false;
    if( this.label != null ) {
      if( !this.label.isEmpty() ) {
	buf.append( this.label );
	buf.append( ':' );
	hasLabel = true;
      }
    }
    if( instruction != null ) {
      if( !instruction.isEmpty() ) {
	buf.append( '\t' );
	buf.append( instruction );
	--tabsToComment;
      }
    }
    if( this.args != null ) {
      if( this.args.length > 0 ) {
	buf.append( '\t' );
	for( int i = 0; i < this.args.length; i++ ) {
	  if( i > 0 ) {
	    buf.append( ',' );
	  }
	  buf.append( this.args[ i ].createFormattedText() );
	}
	--tabsToComment;
      }
    }
    if( this.comment != null ) {
      if( !this.comment.isEmpty() ) {
	if( hasLabel || (tabsToComment < 3) || !this.commentAtStart ) {
	  while( tabsToComment > 0 ) {
	    buf.append( '\t' );
	    --tabsToComment;
	  }
	}
	buf.append( ';' );
	buf.append( this.comment );
      }
    }
  }


  public void checkEOL() throws PrgException
  {
    if( this.args != null ) {
      if( this.argPos < this.args.length ) {
	StringBuilder buf  = new StringBuilder( 32 );
	String        text = this.args[ this.argPos ].toString();
	if( text != null ) {
	  if( !text.isEmpty() ) {
	    buf.append( text );
	    buf.append( ": " );
	  }
	}
	buf.append( "Unerwartetes Argument" );
	throw new PrgException( buf.toString() );
      }
    }
  }


  public int getBegOfInstruction()
  {
    return this.begOfInst;
  }


  public String getInstruction()
  {
    return this.instruction;
  }


  public String getLabel()
  {
    return this.label;
  }


  public boolean hasMoreArgs()
  {
    boolean rv = false;
    if( this.args != null ) {
      if( this.argPos < this.args.length ) {
	rv = true;
      }
    }
    return rv;
  }


  public AsmArg nextArg() throws PrgException
  {
    AsmArg arg = null;
    if( this.args != null ) {
      if( this.argPos < this.args.length ) {
	arg = this.args[ this.argPos++ ];
      }
    }
    if( arg == null ) {
      throw new PrgException( "Argument erwartet" );
    }
    return arg;
  }


	/* --- private Konstruktoren und Methoden --- */

  private AsmLine(
		boolean commentAtStart,
		String  comment,
		String  label,
		int     begOfInst,
		String  instruction,
		AsmArg[] args )
  {
    this.commentAtStart = commentAtStart;
    this.comment        = comment;
    this.label          = label;
    this.begOfInst      = begOfInst;
    this.instruction    = instruction;
    this.args           = args;
    this.argPos         = 0;
  }


  private static String nextArgText( CharacterIterator iter )
						throws PrgException
  {
    char ch  = iter.current();
    while( (ch != CharacterIterator.DONE)
	   && PrgUtil.isWhitespace( ch ) )
    {
      ch = iter.next();
    }
    StringBuilder buf = null;
    if( (ch == '\'') || (ch == '\"') ) {
      char chEnd = ch;
      buf        = new StringBuilder();
      buf.append( ch );
      ch = iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != chEnd) ) {
	buf.append( ch );
	ch = iter.next();
      }
      if( ch == chEnd ) {
	buf.append( ch );
	ch = iter.next();
      }
    }
    if( (ch != CharacterIterator.DONE) && (ch != ',') && (ch != ';') ) {
      if( buf == null ) {
	buf = new StringBuilder();
      }
      while( (ch != CharacterIterator.DONE) && (ch != ',') && (ch != ';') ) {
	buf.append( ch );
	ch = iter.next();
      }
    }
    if( ch == ',' ) {
      if( buf == null ) {
	throw new PrgException( "Komma ohne vorheriges Argument" );
      }
      iter.next();
    }
    String rv = null;
    if( buf != null ) {
      rv = buf.toString();
      if( rv.isEmpty() ) {
	rv = null;
      }
    }
    return rv;
  }


  private static void throwInvalidCharInLabel( char ch ) throws PrgException
  {
    StringBuilder buf = new StringBuilder( 40 );
    if( ch >= '\u0020' ) {
      buf.append( '\'' );
      buf.append( ch );
      buf.append( "\': " );
    }
    buf.append( "Zeichen in Marke nicht erlaubt" );
    throw new PrgException( buf.toString() );
  }
}
