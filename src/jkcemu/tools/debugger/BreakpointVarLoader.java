/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Laden von Halte-/Log-Punkten und Variablen
 */

package jkcemu.tools.debugger;

import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import z80emu.Z80InterruptSource;


public class BreakpointVarLoader extends DefaultHandler
{
  private static final String PATH_BREAKPOINT = "/" + DebugFrm.ELEM_ROOT
				+ "/" + DebugFrm.ELEM_BREAKPOINTS
				+ "/" + AbstractBreakpoint.ELEM_BREAKPOINT;

  private static final String PATH_VARIABLE = "/" + DebugFrm.ELEM_ROOT
				+ "/" + DebugFrm.ELEM_VARIABLES
				+ "/" + VarData.ELEM_VARIABLE;

  private DebugFrm             debugFrm;
  private Z80InterruptSource[] intSources;
  private BreakpointListModel  pcModel;
  private BreakpointListModel  memModel;
  private BreakpointListModel  inpModel;
  private BreakpointListModel  outModel;
  private BreakpointListModel  intModel;
  private VarTableModel        varModel;
  private Stack<String>        stack;
  private boolean              loaded;


  public BreakpointVarLoader(
			DebugFrm             debugFrm,
			Z80InterruptSource[] intSources,
			BreakpointListModel  pcModel,
			BreakpointListModel  memModel,
			BreakpointListModel  inpModel,
			BreakpointListModel  outModel,
			BreakpointListModel  intModel,
			VarTableModel        varModel )
  {
    this.debugFrm   = debugFrm;
    this.intSources = intSources;
    this.pcModel    = pcModel;
    this.memModel   = memModel;
    this.inpModel   = inpModel;
    this.outModel   = outModel;
    this.intModel   = intModel;
    this.varModel   = varModel;
    this.stack      = new Stack<>();
    this.loaded     = false;
  }


  public boolean getLoaded()
  {
    return this.loaded;
  }


  public static boolean getBooleanValue( Attributes attrs, String attrName )
  {
    String s = attrs.getValue( attrName );
    return s != null ? Boolean.parseBoolean( s.trim() ) : false;
  }


  public static int getIntValue( String text )
  {
    Integer v = readInteger( text );
    return v != null ? v.intValue() : 0;
  }


  public static int getIntValue( Attributes attrs, String attrName )
  {
    return getIntValue( attrs.getValue( attrName ) );
  }


  public static Integer readInteger( String text )
  {
    Integer rv = 0;
    if( text != null ) {
      text    = text.trim().toUpperCase();
      int len = text.length();
      if( len > 0 ) {
	try {
	  if( text.endsWith( "H" ) ) {
	    rv = Integer.valueOf( text.substring( 0, len - 1 ), 16 );
	  } else {
	    rv = Integer.valueOf( text );
	  }
	}
	catch( NumberFormatException ex ) {}
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void characters(
			char[] cAry,
			int    offs,
			int    len ) throws SAXException
  {
    // leer
  }


  @Override
  public void endElement(
                        String uri,
                        String localName,
                        String qualifiedName )
  {
    if( qualifiedName != null ) {
      if( !qualifiedName.isEmpty() && !this.stack.isEmpty() ) {
	this.stack.pop();
      }
    }
  }


  @Override
  public void startElement(
                        String     uri,
                        String     localName,
                        String     qualifiedName,
                        Attributes attrs )
  {
    if( qualifiedName != null ) {
      if( !qualifiedName.isEmpty() ) {
	String s = "/" + qualifiedName;
	if( this.stack.isEmpty() ) {
	  this.stack.add( s );
	} else {
	  this.stack.add( this.stack.peek() + s );
	}
	if( attrs != null ) {
	  if( checkPath( PATH_BREAKPOINT ) ) {
	    String bpType = attrs.getValue( AbstractBreakpoint.ATTR_TYPE );
	    if( bpType != null ) {
	      switch( bpType ) {
		case PCBreakpoint.BP_TYPE:
		  createPCBreakpoint( attrs );
		  break;
		case MemoryBreakpoint.BP_TYPE:
		  createMemoryBreakpoint( attrs );
		  break;
		case InputBreakpoint.BP_TYPE:
		  createInputBreakpoint( attrs );
		  break;
		case OutputBreakpoint.BP_TYPE:
		  createOutputBreakpoint( attrs );
		  break;
		case InterruptBreakpoint.BP_TYPE:
		  createInterruptBreakpoint( attrs );
		  break;
	      }
	    }
	  } else if( checkPath( PATH_VARIABLE ) ) {
	    try {
	      Integer addr = readInteger(
				attrs.getValue( VarData.ATTR_ADDR ) );
	      String typeText = attrs.getValue( VarData.ATTR_TYPE );
	      if( (addr != null) && (typeText != null) ) {
		VarData.VarType varType = VarData.VarType.BYTE_ARRAY;
		int             varSize = 0;
		switch( typeText ) {
		  case VarData.TYPE_INT1:
		    varType = VarData.VarType.INT1;
		    varSize = 1;
		    break;
		  case VarData.TYPE_INT2_LE:
		    varType = VarData.VarType.INT2_LE;
		    varSize = 2;
		    break;
		  case VarData.TYPE_INT2_BE:
		    varType = VarData.VarType.INT2_BE;
		    varSize = 2;
		    break;
		  case VarData.TYPE_INT3_LE:
		    varType = VarData.VarType.INT3_LE;
		    varSize = 3;
		    break;
		  case VarData.TYPE_INT3_BE:
		    varType = VarData.VarType.INT3_BE;
		    varSize = 3;
		    break;
		  case VarData.TYPE_INT4_LE:
		    varType = VarData.VarType.INT4_LE;
		    varSize = 4;
		    break;
		  case VarData.TYPE_INT4_BE:
		    varType = VarData.VarType.INT4_BE;
		    varSize = 4;
		    break;
		  case VarData.TYPE_BC_DEC6:
		    varType = VarData.VarType.BC_DEC6;
		    varSize = 6;
		    break;
		  case VarData.TYPE_POINTER:
		    varType = VarData.VarType.POINTER;
		    varSize = 2;
		    break;
		}
		if( varSize < 1 ) {
		  varSize = getIntValue( attrs, VarData.ATTR_SIZE );
		}
		if( varSize > 0 ) {
		  this.varModel.addRow(
			new VarData(
				attrs.getValue( VarData.ATTR_NAME ),
				addr.intValue(),
				varType,
				varSize,
				getBooleanValue(
					attrs,
					VarData.ATTR_IMPORTED ) ) );
		}
	      }
	    }
	    catch( NumberFormatException ex ) {}
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private boolean checkPath( String path )
  {
    return this.stack.isEmpty() ? false : this.stack.peek().equals( path );
  }


  private void createInputBreakpoint( Attributes attrs )
  {
    InputBreakpoint bp = InputBreakpoint.createByAttrs(
						this.debugFrm,
						attrs );
    if( bp != null ) {
      updLogStopEnabled( bp, attrs );
      this.inpModel.put( bp );
      this.loaded = true;
    }
  }


  private void createInterruptBreakpoint( Attributes attrs )
  {
    String srcText = attrs.getValue( InterruptBreakpoint.ATTR_SOURCE );
    if( (srcText != null) && (this.intSources != null) ) {
      for( Z80InterruptSource iSource : this.intSources ) {
	String s = iSource.toString();
	if( s != null ) {
	  if( s.equals( srcText ) ) {
	    InterruptBreakpoint bp = new InterruptBreakpoint(
							this.debugFrm,
							iSource );
	    updLogStopEnabled( bp, attrs );
	    this.intModel.put( bp );
	    this.loaded = true;
	    break;
	  }
	}
      }
    }
  }


  private void createMemoryBreakpoint( Attributes attrs )
  {
    MemoryBreakpoint bp = MemoryBreakpoint.createByAttrs(
						this.debugFrm,
						attrs );
    if( bp != null ) {
      updLogStopEnabled( bp, attrs );
      this.memModel.put( bp );
      this.loaded = true;
    }
  }


  private void createOutputBreakpoint( Attributes attrs )
  {
    OutputBreakpoint bp = OutputBreakpoint.createByAttrs(
						this.debugFrm,
						attrs );
    if( bp != null ) {
      updLogStopEnabled( bp, attrs );
      this.outModel.put( bp );
      this.loaded = true;
    }
  }


  private void createPCBreakpoint( Attributes attrs )
  {
    PCBreakpoint bp = PCBreakpoint.createByAttrs( this.debugFrm, attrs );
    if( bp != null ) {
      updLogStopEnabled( bp, attrs );
      this.pcModel.put( bp );
      this.loaded = true;
    }
  }


  private static void updLogStopEnabled(
				AbstractBreakpoint bp,
				Attributes         attrs )
  {
    bp.setLogEnabled(
	getBooleanValue( attrs, AbstractBreakpoint.ATTR_LOG_ENABLED ) );
    bp.setStopEnabled(
	getBooleanValue( attrs, AbstractBreakpoint.ATTR_STOP_ENABLED ) );
  }
}
