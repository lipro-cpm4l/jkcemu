/*
 * (c) 2012-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Struktureintrag eines aufrufbaren Unterprogramms
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.*;
import jkcemu.programming.PrgSource;


public abstract class CallableEntry extends BasicSourcePos
{
  private String                     name;
  private String                     label;
  private BasicSourcePos             firstCallSourcePos;
  private java.util.List<String>     args;
  private java.util.List<String>     vars;
  private Map<String,BasicSourcePos> var2SourcePos;
  private Set<String>                usedVars;
  private Map<String,Integer>        name2iyOffs;
  private boolean                    implemented;
  private boolean                    stackFrame;


  protected CallableEntry(
		PrgSource source,
		long      basicLineNum,
		String    name,
		String    label )
  {
    super( source, basicLineNum );
    this.name               = name;
    this.label              = label;
    this.firstCallSourcePos = null;
    this.args               = new ArrayList<>();
    this.vars               = new ArrayList<>();
    this.var2SourcePos      = new HashMap<>();
    this.usedVars           = new TreeSet<>();
    this.name2iyOffs        = new HashMap<>();
    this.implemented        = false;
    this.stackFrame         = false;
  }


  public void addVar(
		PrgSource source,
		long      basicLineNum,
		String    varName )
  {
    this.name2iyOffs.put(
		varName,
		new Integer( getVarIYOffs( this.vars.size() ) ) );
    this.var2SourcePos.put(
		varName,
		new BasicSourcePos( source, basicLineNum ) );
    this.vars.add( varName );
  }


  public boolean equalsArgType( int idx, String argName )
  {
    return (idx >= 0) && (idx < this.args.size()) ?
	(this.args.get( idx ).endsWith( "$" ) == argName.endsWith( "$" ))
	: false;
  }


  public int getArgCount()
  {
    return this.args.size();
  }


  public int getArgIYOffs( int idx, int nArgs )
  {
    return 4 + ((nArgs - idx - 1) * 2);
  }


  public BasicCompiler.DataType getArgType( int idx )
  {
    return this.args.get( idx ).endsWith( "$" ) ?
					BasicCompiler.DataType.STRING
					: BasicCompiler.DataType.INTEGER;
  }


  public BasicSourcePos getFirstCallSourcePos()
  {
    return this.firstCallSourcePos;
  }


  public Integer getIYOffs( String varName )
  {
    return this.name2iyOffs.get( varName );
  }


  public String getLabel()
  {
    return this.label;
  }


  public String getName()
  {
    return this.name;
  }


  public int getTotalArgSize()
  {
    return this.args.size() * 2;
  }


  public int getTotalVarSize()
  {
    return this.vars.size() * 2;
  }


  public int getVarCount()
  {
    return this.vars.size();
  }


  public int getVarIYOffs( int idx )
  {
    return -(idx * 2) - 2;
  }


  public BasicSourcePos getVarSourcePos( String varName )
  {
    return varName != null ? this.var2SourcePos.get( varName ) : null;
  }


  public String getVarName( int idx )
  {
    return (idx >= 0) && (idx < this.vars.size()) ?
					this.vars.get( idx )
					: null;
  }


  public BasicCompiler.DataType getVarType( int idx )
  {
    return this.vars.get( idx ).endsWith( "$" ) ?
					BasicCompiler.DataType.STRING
					: BasicCompiler.DataType.INTEGER;
  }


  public boolean hasStackFrame()
  {
    return this.stackFrame;
  }


  public boolean isCalled()
  {
    return (this.firstCallSourcePos != null);
  }


  public boolean isImplemented()
  {
    return this.implemented;
  }


  public boolean isVarUsed( String varName )
  {
    return varName != null ? this.usedVars.contains( varName ) : false;
  }


  public void putCallPos(
			PrgSource source,
			long      basicLineNum )
  {
    if( this.firstCallSourcePos == null )
      this.firstCallSourcePos = new BasicSourcePos( source, basicLineNum );
  }


  public void setArg( int idx, String argName )
  {
    if( (idx >= 0) && (idx < this.args.size()) ) {
      this.name2iyOffs.remove( this.args.get( idx ) );
      this.name2iyOffs.put(
		argName,
		new Integer( getArgIYOffs( idx, this.args.size() ) ) );
      this.args.set( idx, argName );
    }
  }


  public void setArgs( java.util.List<String> args )
  {
    this.args.clear();
    if( args != null ) {
      int nArgs = args.size();
      if( nArgs > 0 ) {
	for( String arg : args ) {
	  this.name2iyOffs.put(
		arg,
		new Integer( getArgIYOffs( this.args.size(), nArgs ) ) );
	  this.args.add( arg );
	}
      }
    }
  }


  public void setImplemented()
  {
    this.implemented = true;
  }


  public void setStackFrameCreated()
  {
    this.stackFrame = true;
  }


  public void setVarUsed( String varName )
  {
    if( varName != null )
      this.usedVars.add( varName );
  }
}
