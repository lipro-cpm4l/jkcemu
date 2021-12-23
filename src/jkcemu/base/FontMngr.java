/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Verwaltung der Schriftarten
 */

package jkcemu.base;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import jkcemu.base.EmuUtil;


public class FontMngr
{
  public static enum FontUsage { GENERAL, INPUT, CODE, HTML, MENU };

  public static final int DEFAULT_FONT_SIZE = 13;

  private static Properties properties = null;

  private static Map<FontUsage,Font> defaultFontMap = new HashMap<>();
  private static Map<FontUsage,Font> userFontMap    = new HashMap<>();
  private static Object              fontLock       = new Object();


  public static void clearDefaultFontMap()
  {
    defaultFontMap.clear();
  }


  public static Font getDefaultFont( FontUsage fontUsage )
  {
    Font font = defaultFontMap.get( fontUsage );
    if( font == null ) {
      switch( fontUsage ) {
	case GENERAL:
	  font = (new JLabel()).getFont();
	  break;
	case INPUT:
	  font = (new JTextField()).getFont();
	  break;
	case CODE:
	  font = (new JTextArea()).getFont();
	  if( font != null ) {
	    font = new Font(
			Font.MONOSPACED,
			font.getStyle(),
			font.getSize() );
	  }
	  break;
	case HTML:
	  font = (new JEditorPane()).getFont();
	  break;
	case MENU:
	  font = (new JMenu()).getFont();
	  break;
      }
      if( font == null ) {
	font = new Font(
		fontUsage.equals( FontUsage.CODE ) ?
				Font.MONOSPACED : Font.SANS_SERIF,
		Font.PLAIN,
		DEFAULT_FONT_SIZE );
      }
      defaultFontMap.put( fontUsage, font );
    }
    return font;
  }


  public static Font getFont(
			FontUsage fontUsage,
			boolean   enableDefaultFont )
  {
    Font font = null;
    synchronized( fontLock ) {
      font = userFontMap.get( fontUsage );
      if( font == null ) {
	String family = EmuUtil.getProperty(
				properties,
				getPropNameFontFamily( fontUsage ) );
	int size = EmuUtil.getIntProperty(
				properties,
				getPropNameFontSize( fontUsage ),
				0 );
	boolean isBold = EmuUtil.getBooleanProperty(
				properties,
				getPropNameFontBold( fontUsage ),
				false );
	boolean isItalic = EmuUtil.getBooleanProperty(
				properties,
				getPropNameFontItalic( fontUsage ),
				false );
	if( !family.isEmpty() && (size > 0) ) {
	  int style = 0;
	  if( isBold ) {
	    style = Font.BOLD;
	  }
	  if( isItalic ) {
	    style = Font.ITALIC;
	  }
	  font = new Font(
			family,
			style != 0 ? style : Font.PLAIN,
			size );
	  userFontMap.put( fontUsage, font );
	}
      }
    }
    if( (font == null) && enableDefaultFont ) {
      font = getDefaultFont( fontUsage );
    }
    return font;
  }


  public static String getPropNameFontBold( FontUsage fontUsage )
  {
    return getPropPrefix( fontUsage ) + "bold";
  }


  public static String getPropNameFontFamily( FontUsage fontUsage )
  {
    return getPropPrefix( fontUsage ) + "family";
  }


  public static String getPropNameFontItalic( FontUsage fontUsage )
  {
    return getPropPrefix( fontUsage ) + "italic";
  }


  public static String getPropNameFontSize( FontUsage fontUsage )
  {
    return getPropPrefix( fontUsage ) + "size";
  }


  public static void putProperties( Properties props )
  {
    synchronized( fontLock ) {
      properties = props;
      userFontMap.clear();
    }
  }


	/* --- private Methoden --- */

  private static String getPropPrefix( FontUsage fontUsage )
  {
    String text = "*";
    switch( fontUsage ) {
      case GENERAL:
	text = "general";
	break;
      case INPUT:
	text = "input";
	break;
      case CODE:
	text = "code";
	break;
      case HTML:
	text = "html";
	break;
      case MENU:
	text = "menu";
	break;
    }
    return "jkcemu.font." + text + ".";
  }
}
