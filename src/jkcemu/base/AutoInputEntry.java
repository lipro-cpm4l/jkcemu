/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Element einer automatischen Tastatureingabe
 */

package jkcemu.base;

import java.io.UnsupportedEncodingException;
import java.lang.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Properties;


public class AutoInputEntry
{
  public static final String PROP_COUNT       = "count";
  public static final String PROP_INPUT_TEXT  = "input_text";
  public static final String PROP_REMARK      = "remark";
  public static final String PROP_WAIT_MILLIS = "wait.millis";

  private int    millisToWait;
  private String inputText;
  private String remark;


  public AutoInputEntry( int millisToWait, String inputText, String remark )
  {
    this.millisToWait = millisToWait;
    this.inputText    = inputText;
    this.remark       = remark;
  }


  public int getMillisToWait()
  {
    return this.millisToWait;
  }


  public String getInputText()
  {
    return this.inputText;
  }


  public String getRemark()
  {
    return this.remark;
  }


  public static java.util.List<AutoInputEntry> readEntries(
						Properties props,
						String     propPrefix )
  {
    java.util.List<AutoInputEntry> rv = null;
    if( (props != null) && (propPrefix != null) ) {
      int n = EmuUtil.getIntProperty( props, propPrefix + PROP_COUNT, 0 );
      if( n > 0 ) {
	rv = new ArrayList<>();
	for( int i = 0; i < n; i++ ) {
	  String prefix    = String.format( "%s%d.", propPrefix, i );
	  String inputText = props.getProperty( prefix + PROP_INPUT_TEXT );
	  if( inputText != null ) {
	    if( !inputText.isEmpty() ) {
	      try {
		rv.add( new AutoInputEntry(
				EmuUtil.getIntProperty(
					props,
					prefix + PROP_WAIT_MILLIS,
					0 ),
				URLDecoder.decode( inputText, "UTF-8" ),
				props.getProperty( prefix + PROP_REMARK ) ) );
	      }
	      catch( UnsupportedEncodingException ex ) {}
	    }
	  }
	}
      }
    }
    return rv;
  }
}
