/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionen fuer den Basic-Compiler
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import jkcemu.programming.PrgOptions;
import jkcemu.text.EditText;
import jkcemu.system.*;


public class BasicOptions extends PrgOptions
{
  public static enum BreakPossibility {
				BREAK_NEVER,
				BREAK_INPUT,
				BREAK_ALWAYS };

  public static final int DEFAULT_ARRAY_SIZE     = 128;
  public static final int DEFAULT_STACK_SIZE     = 128;
  public static final int DEFAULT_END_OF_MEM     = 0x3FFF;

  private String           appName;
  private int              begAddr;
  private int              arraySize;
  private int              stackSize;
  private int              endOfMem;
  private boolean          allowLongVarNames;
  private boolean          checkStack;
  private boolean          checkArray;
  private boolean          showAsm;
  private boolean          strictAC1Basic;
  private boolean          strictZ1013Basic;
  private boolean          structuredForNext;
  private boolean          preferRelJumps;
  private boolean          printCalls;
  private BreakPossibility breakPossibility;


  public BasicOptions( EmuThread emuThread )
  {
    this.appName           = "MYAPP";
    this.begAddr           = getDefaultBegAddr( emuThread.getEmuSys() );
    this.arraySize         = DEFAULT_ARRAY_SIZE;
    this.stackSize         = DEFAULT_STACK_SIZE;
    this.endOfMem          = DEFAULT_END_OF_MEM;
    this.allowLongVarNames = false;
    this.checkStack        = true;
    this.checkArray        = true;
    this.showAsm           = false;
    this.strictAC1Basic    = false;
    this.strictZ1013Basic  = false;
    this.structuredForNext = false;
    this.preferRelJumps    = true;
    this.printCalls        = true;
    this.breakPossibility  = BreakPossibility.BREAK_INPUT;
    setSyntax( Syntax.ZILOG_ONLY );
    setAllowUndocInst( false );
    setLabelsCaseSensitive( false );
    setPrintLabels( false );
  }


  public BasicOptions( BasicOptions src )
  {
    super( src );
    this.appName           = src.appName;
    this.begAddr           = src.begAddr;
    this.arraySize         = src.arraySize;
    this.stackSize         = src.stackSize;
    this.endOfMem          = src.endOfMem;
    this.allowLongVarNames = src.allowLongVarNames;
    this.checkStack        = src.checkStack;
    this.checkArray        = src.checkArray;
    this.showAsm           = src.showAsm;
    this.strictAC1Basic    = src.strictAC1Basic;
    this.strictZ1013Basic  = src.strictZ1013Basic;
    this.structuredForNext = src.structuredForNext;
    this.preferRelJumps    = src.preferRelJumps;
    this.printCalls        = src.printCalls;
    this.breakPossibility  = src.breakPossibility;
  }


  public boolean getAllowLongVarNames()
  {
    return this.allowLongVarNames;
  }


  public String getAppName()
  {
    return this.appName;
  }


  public int getArraySize()
  {
    return this.arraySize;
  }


  public static BasicOptions getBasicOptions(
					EmuThread  emuThread,
					Properties props )
  {
    BasicOptions options = null;
    if( props != null ) {
      String  appName = props.getProperty( "jkcemu.programmimg.basic.name" );

      Integer begAddr = getInteger(
			props,
			"jkcemu.programmimg.basic.address.begin" );

      Integer endOfMem = getInteger(
			props,
			"jkcemu.programmimg.basic.address.top" );

      Integer arraySize = getInteger(
			props,
			"jkcemu.programmimg.basic.array.size" );

      Integer stackSize = getInteger(
			props,
			"jkcemu.programmimg.basic.stack.size" );

      Boolean allowLongVarNames = getBoolean(
			props,
			"jkcemu.programmimg.basic.allow_long_variable_names" );

      Boolean checkArray = getBoolean(
			props,
			"jkcemu.programmimg.basic.array.check" );

      Boolean checkStack = getBoolean(
			props,
			"jkcemu.programmimg.basic.stack.check" );

      Boolean showAsm = getBoolean(
			props,
			"jkcemu.programmimg.basic.show_assembler_source" );

      Boolean strictAC1Basic = getBoolean(
			props,
			"jkcemu.programmimg.basic.strict_ac1_minibasic" );

      Boolean strictZ1013Basic = getBoolean(
			props,
			"jkcemu.programmimg.basic.strict_z1013_tinybasic" );

      Boolean structuredForNext = getBoolean(
			props,
			"jkcemu.programmimg.basic.structured_for_next" );

      Boolean preferRelJumps = getBoolean(
			props,
			"jkcemu.programmimg.basic.prefer_relative_jumps" );

      Boolean printCalls = getBoolean(
			props,
			"jkcemu.programmimg.basic.print_calls" );

      String breakPossibilityText = props.getProperty(
			"jkcemu.programmimg.basic.breakable" );

      if( (appName != null)
	  || (begAddr != null)
	  || (endOfMem != null)
	  || (arraySize != null)
	  || (stackSize != null)
	  || (allowLongVarNames != null)
	  || (checkArray != null)
	  || (checkStack != null)
	  || (strictAC1Basic != null)
	  || (strictZ1013Basic != null)
	  || (structuredForNext != null)
	  || (preferRelJumps != null)
	  || (printCalls != null)
	  || (breakPossibilityText != null) )
      {
	options = new BasicOptions( emuThread );

	if( appName != null ) {
	  options.appName = appName;
	}
	if( begAddr != null ) {
	  options.begAddr = begAddr.intValue();
	}
	if( arraySize != null ) {
	  options.arraySize = arraySize.intValue();
	}
	if( stackSize != null ) {
	  options.stackSize = stackSize.intValue();
	}
	if( allowLongVarNames != null ) {
	  options.allowLongVarNames = allowLongVarNames.booleanValue();
	}
	if( checkArray != null ) {
	  options.checkArray = checkArray.booleanValue();
	}
	if( checkStack != null ) {
	  options.checkStack = checkStack.booleanValue();
	}
	if( endOfMem != null ) {
	  options.endOfMem = endOfMem.intValue();
	}
	if( showAsm != null ) {
	  options.showAsm = showAsm.booleanValue();
	}
	if( strictAC1Basic != null ) {
	  options.strictAC1Basic = strictAC1Basic.booleanValue();
	}
	if( strictZ1013Basic != null ) {
	  options.strictZ1013Basic = strictZ1013Basic.booleanValue();
	}
	if( structuredForNext != null ) {
	  options.structuredForNext = structuredForNext.booleanValue();
	}
	if( preferRelJumps != null ) {
	  options.preferRelJumps = preferRelJumps.booleanValue();
	}
	if( printCalls != null ) {
	  options.printCalls = printCalls.booleanValue();
	}
	if( breakPossibilityText != null ) {
	  if( breakPossibilityText.equals( "never" ) ) {
	    options.breakPossibility = BreakPossibility.BREAK_NEVER;
	  } else if( breakPossibilityText.equals( "input" ) ) {
	    options.breakPossibility = BreakPossibility.BREAK_INPUT;
	  } else {
	    options.breakPossibility = BreakPossibility.BREAK_ALWAYS;
	  }
	}
      }
    }
    return options;
  }


  public int getBegAddr()
  {
    return this.begAddr;
  }


  public BreakPossibility getBreakPossibility()
  {
    return this.breakPossibility;
  }


  public boolean getCheckArray()
  {
    return this.checkArray;
  }


  public boolean getCheckStack()
  {
    return this.checkStack;
  }


  public static int getDefaultBegAddr( EmuSys emuSys )
  {
    int rv = 0x0300;		// HC900, Z9001
    if( emuSys != null ) {
      if( emuSys instanceof AC1 ) {
	rv = 0x2000;
      }
      else if( emuSys instanceof KramerMC ) {
	rv = 0x1000;
      }
      else if( emuSys instanceof Z1013 ) {
	rv = 0x0100;
      }
    }
    return rv;
  }


  public int getEndOfMemory()
  {
    return this.endOfMem;
  }


  public boolean getPreferRelativeJumps()
  {
    return this.preferRelJumps;
  }


  public boolean getPrintCalls()
  {
    return this.printCalls;
  }


  public boolean getShowAsm()
  {
    return this.showAsm;
  }


  public int getStackSize()
  {
    return this.stackSize;
  }


  public boolean getStrictAC1MiniBASIC()
  {
    return this.strictAC1Basic;
  }


  public boolean getStrictZ1013TinyBASIC()
  {
    return this.strictZ1013Basic;
  }


  public boolean getStructuredForNext()
  {
    return this.structuredForNext;
  }


  public boolean isBreakAlwaysPossible()
  {
    return this.breakPossibility == BreakPossibility.BREAK_ALWAYS;
  }


  public boolean isBreakOnInputPossible()
  {
    return (this.breakPossibility == BreakPossibility.BREAK_ALWAYS)
	   || (this.breakPossibility == BreakPossibility.BREAK_INPUT);
  }


  public void putOptionsTo( Properties props )
  {
    super.putOptionsTo( props );
    if( props != null ) {
      props.setProperty(
		"jkcemu.programmimg.basic.name",
		this.appName != null ? this.appName : "" );

      props.setProperty(
		"jkcemu.programmimg.basic.address.begin",
                Integer.toString( this.begAddr ) );

      props.setProperty(
		"jkcemu.programmimg.basic.address.top",
                Integer.toString( this.endOfMem ) );

      props.setProperty(
		"jkcemu.programmimg.basic.array.size",
                Integer.toString( this.arraySize ) );

      props.setProperty(
		"jkcemu.programmimg.basic.array.check",
                Boolean.toString( this.checkArray ) );

      props.setProperty(
		"jkcemu.programmimg.basic.stack.size",
                Integer.toString( this.stackSize ) );

      props.setProperty(
		"jkcemu.programmimg.basic.stack.check",
                Boolean.toString( this.checkStack ) );

      props.setProperty(
		"jkcemu.programmimg.basic.allow_long_variable_names",
                Boolean.toString( this.allowLongVarNames ) );

      props.setProperty(
		"jkcemu.programmimg.basic.show_assembler_source",
                Boolean.toString( this.showAsm ) );

      props.setProperty(
		"jkcemu.programmimg.basic.strict_ac1_minibasic",
                Boolean.toString( this.strictAC1Basic ) );

      props.setProperty(
		"jkcemu.programmimg.basic.strict_z1013_tinybasic",
                Boolean.toString( this.strictZ1013Basic ) );

      props.setProperty(
		"jkcemu.programmimg.basic.structured_for_next",
                Boolean.toString( this.structuredForNext ) );

      props.setProperty(
		"jkcemu.programmimg.basic.prefer_relative_jumps",
                Boolean.toString( this.preferRelJumps ) );

      props.setProperty(
		"jkcemu.programmimg.basic.print_calls",
                Boolean.toString( this.printCalls ) );

      if( this.breakPossibility != null ) {
	switch( this.breakPossibility ) {
	  case BREAK_NEVER:
	    props.setProperty( "jkcemu.programmimg.basic.breakable", "never" );
	    break;

	  case BREAK_INPUT:
	    props.setProperty( "jkcemu.programmimg.basic.breakable", "input" );
	    break;

	  default:
	    props.setProperty(
			"jkcemu.programmimg.basic.breakable",
			"always" );
	}
      }
    }
  }


  public void setAllowLongVarNames( boolean state )
  {
    this.allowLongVarNames = state;
  }


  public void setAppName( String appName )
  {
    this.appName = appName;
  }


  public void setArraySize( int value )
  {
    this.arraySize = value;
  }


  public void setBegAddr( int value )
  {
    this.begAddr = value;
  }


  public void setBreakPossibility( BreakPossibility value )
  {
    this.breakPossibility = value;
  }


  public void setCheckArray( boolean state )
  {
    this.checkArray = state;
  }


  public void setCheckStack( boolean state )
  {
    this.checkStack = state;
  }


  public void setEndOfMemory( int value )
  {
    this.endOfMem = value;
  }


  public void setPreferRelativeJumps( boolean state )
  {
    this.preferRelJumps = state;
  }


  public void setPrintCalls( boolean state )
  {
    this.printCalls = state;
  }


  public void setShowAsm( boolean state )
  {
    this.showAsm = state;
  }


  public void setStackSize( int value )
  {
    this.stackSize = value;
  }


  public void setStrictAC1MiniBASIC( boolean state )
  {
    this.strictAC1Basic = state;
  }


  public void setStrictZ1013TinyBASIC( boolean state )
  {
    this.strictZ1013Basic = state;
  }


  public void setStructuredForNext( boolean state )
  {
    this.structuredForNext = state;
  }
}

