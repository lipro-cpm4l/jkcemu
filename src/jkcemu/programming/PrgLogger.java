/*
 * (c) 2012-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Logger fuer den Compiler/Assembler
 */

package jkcemu.programming;

import jkcemu.Main;


public class PrgLogger
{
  public static PrgLogger createLogger( final Appendable out )
  {
    return new PrgLogger()
		{
		  @Override
		  public void appendToErrLog( String text )
		  {
		    appendToOutLog( text );
		  }

		  @Override
		  public void appendToOutLog( String text )
		  {
		    if( text != null ) {
		      try {
			out.append( text );
		      }
		      catch( java.io.IOException ex ) {}
		    }
		  }
		};
  }


  public static PrgLogger createStandardLogger()
  {
    return new PrgLogger()
		{
		  @Override
		  public void appendToErrLog( String text )
		  {
		    Main.printErr( text );
		  }

		  @Override
		  public void appendToOutLog( String text )
		  {
		    Main.printOut( text );
		  }
		};
  }


  public void appendToErrLog( String text )
  {
    // wird ueberschrieben
  }


  public void appendToOutLog( String text )
  {
    // wird ueberschrieben
  }


	/* --- Konstruktor --- */

  private PrgLogger()
  {
    // leer
  }
}
