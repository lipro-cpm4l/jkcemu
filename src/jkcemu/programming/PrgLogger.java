/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Logger fuer den Compiler/Assembler
 */

package jkcemu.programming;

import java.io.*;
import java.lang.*;
import jkcemu.base.EmuUtil;


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
		    EmuUtil.printErr( text );
		  }

		  @Override
		  public void appendToOutLog( String text )
		  {
		    EmuUtil.printOut( text );
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
