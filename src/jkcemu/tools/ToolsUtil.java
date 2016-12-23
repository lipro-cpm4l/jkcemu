/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer die Werkzeuge
 */

package jkcemu.tools;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.*;
import java.io.Reader;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import jkcemu.base.EmuUtil;


public class ToolsUtil
{
  public static int getReassAddr( JTextArea textArea, Point point )
  {
    int rv = -1;
    if( point != null ) {
      try {
	int pos = textArea.viewToModel( point );
	if( pos >= 0 ) {
	  int line = textArea.getLineOfOffset( pos );
	  if( line >= 0 ) {
	    pos = textArea.getLineStartOffset( line );
	    if( pos >= 0 ) {
	      String text = textArea.getText();
	      if( text != null ) {
		if( (pos + 4) < text.length() ) {
		  if( text.charAt( pos + 4 ) == '\u0020' ) {
		    int value = EmuUtil.getHex4( text, pos );
		    if( value >= 0 ) {
		      rv = value;
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
      catch( BadLocationException ex ) {}
    }
    return rv;
  }


  public static jkcemu.tools.Label[] readLabels( Reader reader )
							throws IOException
  {
    Set<jkcemu.tools.Label> labels = null;
    if( reader != null ) {
      BufferedReader bufReader = new BufferedReader( reader ); 
      try {
	String line = bufReader.readLine();
	while( line != null ) {
	  String[] items = line.split( "\\s" );
	  if( items != null ) {
	    String labelName  = null;
	    int    labelValue = -1;
	    for( int i = 0; i < items.length; i++ ) {
	      String s = items[ i ];
	      if( s != null ) {
		s = s.trim();
		if( !s.isEmpty() ) {
		  boolean done = false;
		  if( labelValue < 0 ) {
		    labelValue = parseLabelValue( s );
		    if( labelValue >= 0 ) {
		      done = true;
		    }
		  }
		  if( !done && (labelName == null) ) {
		    labelName = parseLabelName( s, labelValue < 0 );
		  }
		}
	      }
	    }
	    if( labelValue >= 0 ) {
	      if( labels == null ) {
		labels = new TreeSet<>();
	      }
	      final String lName  = labelName;
	      final int    lValue = labelValue;
	      labels.add(
			new jkcemu.tools.Label()
			{
			  @Override
			  public int compareTo( jkcemu.tools.Label label )
			  {
			    int rv =  EmuUtil.compare(
					lName,
					label.getLabelName() );
			    if( rv == 0 ) {
			      rv = lValue - label.intValue();
			    }
			    return rv;
			  }

			  @Override
			  public String getLabelName()
			  {
			    return lName;
			  }

			  @Override
			  public int getVarSize()
			  {
			    return -1;
			  }

			  @Override
			  public int intValue()
			  {
			    return lValue;
			  }
			} );
	    }
	  }
	  line = bufReader.readLine();
	}
      }
      catch( PatternSyntaxException ex ) {}
      finally {
	EmuUtil.closeSilent( bufReader );
      }
    }
    return labels != null ?
		labels.toArray( new jkcemu.tools.Label[ labels.size() ] )
		: null;
  }


	/* --- private Methoden --- */

  private static String parseLabelName( String s, boolean trailingColon )
  {
    String rv = null;
    if( s != null ) {
      int len = s.length();
      if( len > 0 ) {
	int n = len;
	if( trailingColon && (s.charAt( len - 1 ) == ':') ) {
	  n = len - 1;
	}
	if( n > 0 ) {
	  char ch = s.charAt( 0 );
	  if( ((ch >= 'A') && (ch <= 'Z'))
	      || ((ch >= 'a') && (ch <= 'z'))
	      || (ch == '_') || (ch == '$') || (ch == '.') )
	  {
	    for( int i = 1; i < n; i++ ) {
	      ch = s.charAt( i );
	      if( ((ch < 'A') || (ch > 'Z'))
		  && ((ch < 'a') && (ch > 'z'))
		  && ((ch < '0') && (ch > '9'))
		  && (ch != '_') && (ch != '$') && (ch != '.') )
	      {
		n = 0;
		break;
	      }
	    }
	    if( n > 0 ) {
	      if( n < len ) {
		rv = s.substring( 0, n );
	      } else {
		rv = s;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private static int parseLabelValue( String s )
  {
    int rv = -1;
    if( s != null ) {
      int len = s.length();
      if( len > 1 ) {
	char ch = s.charAt( len - 1 );
	if( (ch == 'H') || (ch == 'h') ) {
	  --len;
	}
      }
      if( (len == 4) || (len == 5) ) {
	int v = 0;
	for( int i = 0; i < len; i++ ) {
	  char ch = s.charAt( i );
	  if( (ch >= '0') && (ch <= '9') ) {
	    v = (v << 4) | (ch - '0');
	  } else if( (ch >= 'A') && (ch <= 'F') ) {
	    v = (v << 4) | (ch - 'A' + 10);
	  } else if( (ch >= 'a') && (ch <= 'f') ) {
	    v = (v << 4) | (ch - 'a' + 10);
	  } else {
	    v = -1;
	    break;
	  }
	}
	if( (v >= 0) && (v <= 0xFFFF) ) {
	  rv = v;
	}
      }
    }
    return rv;
  }
}
