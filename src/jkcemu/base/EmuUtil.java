/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen
 */

package jkcemu.base;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import jkcemu.Main;
import jkcemu.file.FileUtil;


public class EmuUtil
{
  public static class Cell
  {
    public int row;
    public int col;

    public Cell( int row, int col )
    {
      this.row = row;
      this.col = col;
    }
  };


  public static final String TEXT_APPLY      = "\u00DCbernehmen";
  public static final String TEXT_CANCEL     = "Abbrechen";
  public static final String TEXT_CLOSE      = "Schlie\u00DFen";
  public static final String TEXT_CONFIRM    = "Best\u00E4tigung";
  public static final String TEXT_DEFAULT    = "Standard";
  public static final String TEXT_DELETE     = "L\u00F6schen";
  public static final String TEXT_ERROR      = "Fehler";
  public static final String TEXT_OPEN       = "\u00D6ffnen";
  public static final String TEXT_LOAD       = "Laden";
  public static final String TEXT_SAVE       = "Speichern";
  public static final String TEXT_PLAY       = "Wiedergabe";
  public static final String TEXT_CUT        = "Ausschneiden";
  public static final String TEXT_COPY       = "Kopieren";
  public static final String TEXT_PASTE      = "Einf\u00FCgen";
  public static final String TEXT_RECORD     = "Aufnehmen";
  public static final String TEXT_SELECT     = "Ausw\u00E4hlen";
  public static final String TEXT_SELECT_ALL = "Alles ausw\u00E4hlen";
  public static final String TEXT_HELP       = "Hilfe";
  public static final String TEXT_SETTINGS   = "Einstellungen";
  public static final String TEXT_ON         = "ein";
  public static final String TEXT_OFF        = "aus";
  public static final String TEXT_OPEN_LOAD  = "Laden...";
  public static final String TEXT_OPEN_SAVE  = "Speichern...";
  public static final String TEXT_OPEN_OPEN  = "\u00D6ffnen...";
  public static final String TEXT_OPEN_PRINT = "Drucken...";

  public static final String LABEL_SEARCH_FOR      = "Suchen nach:";
  public static final String TEXT_FIND             = "Suchen";
  public static final String TEXT_FIND_AND_REPLACE = "Suchen und ersetzen";
  public static final String TEXT_FIND_NEXT = "Weitersuchen";
  public static final String TEXT_FIND_PREV = "R\u00FCckw\u00E4rts suchen";
  public static final String TEXT_REPLACE   = "Ersetzen";
  public static final String TEXT_OPEN_FIND = "Suchen...";
  public static final String TEXT_OPEN_FIND_AND_REPLACE
					= "Suchen und ersetzen...";
  public static final String TEXT_ADD_FILE    = "Datei hinzuf\u00FCgen";
  public static final String TEXT_SELECT_DIR  = "Verzeichnis ausw\u00E4hlen";
  public static final String TEXT_SELECT_FILE = "Datei ausw\u00E4hlen";
  public static final String TEXT_SELECT_ROM_FILE
					= "ROM-Datei ausw\u00E4hlen";
  public static final String TEXT_REMOVE_ROM_FILE
					= "ROM-Datei entfernen";

  public static final String VALUE_FALSE = Boolean.FALSE.toString();
  public static final String VALUE_TRUE  = Boolean.TRUE.toString();

  public static final String PROP_SRAM_INIT         = "jkcemu.sram.init";
  public static final String VALUE_SRAM_INIT_00     = "00";
  public static final String VALUE_SRAM_INIT_RANDOM = "random";

  private static DecimalFormat decFmtFix1          = null;
  private static DecimalFormat decFmtMax1          = null;
  private static NumberFormat  intFmt              = null;
  private static Integer       menuShortcutKeyMask = null;
  private static Random        random              = null;


  public static void appendHTML( StringBuilder buf, String text )
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	char ch = text.charAt( i );
	switch( ch ) {
	  case '<':
	    buf.append( "&lt;" );
	    break;
	  case '>':
	    buf.append( "&gt;" );
	    break;
	  case '\"':
	    buf.append( "&quot;" );
	    break;
	  case '&':
	    buf.append( "&amp;" );
	    break;
	  case '\u00C4':
	    buf.append( "&Auml;" );
	    break;
	  case '\u00D6':
	    buf.append( "&Ouml;" );
	    break;
	  case '\u00DC':
	    buf.append( "&Uuml;" );
	    break;
	  case '\u00E4':
	    buf.append( "&auml;" );
	    break;
	  case '\u00F6':
	    buf.append( "&ouml;" );
	    break;
	  case '\u00FC':
	    buf.append( "&uuml;" );
	    break;
	  case '\u00DF':
	    buf.append( "&szlig;" );
	    break;
	  default:
	    if( ch < '\u0020' ) {
	      buf.append( '\u0020' );
	    }
	    else if( ch > '\u007E' ) {
	      buf.append( "&#" );
	      buf.append( (int) ch );
	      buf.append( ';' );
	    } else {
	      buf.append( ch );
	    }
	}
      }
    }
  }


  public static void appendOnOffText( StringBuilder buf, boolean state )
  {
    buf.append( state ? TEXT_ON : TEXT_OFF );
  }


  public static void appendSizeText(
			StringBuilder buf,
			long          size,
			boolean       forceFixLen,
			boolean       forceSizeInBytes )
  {
    DecimalFormat decFmt = forceFixLen ?
				getDecimalFormatFix1()
				: getDecimalFormatMax1();
    final long kb = 1024L;
    final long mb = kb * 1024L;
    final long gb = mb * 1024L;
    if( size >= gb ) {
      buf.append( decFmt.format( (double) size / (double) gb ) );
      buf.append( " GByte" );
    }
    else if( size >= mb ) {
      buf.append( decFmt.format( (double) size / (double) mb ) );
      buf.append( " MByte" );
    }
    else if( size >= kb ) {
      buf.append( decFmt.format( (double) size / (double) kb ) );
      buf.append( " KByte" );
    }
    if( buf.length() > 0 ) {
      if( forceSizeInBytes ) {
	if( size == 1 ) {
	  buf.append( "1 Byte" );
	} else {
	  buf.append( " (" );
	  buf.append( getIntegerFormat().format( size ) );
	  buf.append( " Bytes" );
	  buf.append( ')' );
	}
      }
    } else {
      if( size == 1 ) {
	buf.append( "1 Byte" );
      } else {
	buf.append( getIntegerFormat().format( size ) );
	buf.append( " Bytes" );
      }
    }
  }


  public static void checkAndShowError(
				Component owner,
				String    msg,
				Exception ex )
  {
    if( ex != null ) {
      if( ex instanceof IOException ) {
	BaseDlg.showErrorDlg( owner, msg, ex );
      } else {
	logSysError( owner, msg, ex );
      }
    }
  }


  public static boolean closeOtherFrames( Frame frm )
  {
    boolean rv   = true;
    Frame[] frms = Frame.getFrames();
    for( Frame f : frms ) {
      if( f != frm ) {
	if( f instanceof BaseFrm ) {
	  rv = ((BaseFrm) f).doClose();
	  if( !rv ) {
	    break;
	  }
	} else {
	  f.setVisible( false );
	  f.dispose();
	}
      }
    }
    return rv;
  }


  /*
   * Beim Schliessen eines GZIPOutputStreams tritt eine
   * NullPointerException auf,
   * wenn intern der Deflater bereits geschlossen wurde.
   * Aus diesem Grund wird hier nicht nur IOException,
   * sondern allgemein Exception abgefangen.
   */
  public static void closeSilently( Closeable stream )
  {
    if( stream != null ) {
      try {
	stream.close();
      }
      catch( Exception ex ) {}
    }
  }


  public static int compare( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return s1.compareTo( s2 != null ? s2 : "" );
  }


  public static boolean copyToClipboard( Component owner, String text )
  {
    boolean success = false;
    try {
      if( text != null ) {
	if( !text.isEmpty() ) {
	  Toolkit tk = getToolkit( owner );
	  if( tk != null ) {
	    Clipboard clp = getToolkit( owner ).getSystemClipboard();
	    if( clp != null ) {
	      StringSelection ss = new StringSelection( text );
	      clp.setContents( ss, ss );
	      success = true;
	    }
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
    return success;
  }


  public static String createErrorMsg( String msg, Exception ex )
  {
    String exMsg  = null;
    String exName = null;
    if( ex != null ) {
      exMsg  = ex.getMessage();
      exName = ex.getClass().getName();
      if( exMsg != null ) {
	if( exMsg.trim().isEmpty() ) {
	  exMsg = null;
	}
      }
    }
    if( msg != null ) {
      if( exMsg != null ) {
	msg = msg + "\n\n" + exMsg;
      }
    } else {
      if( exMsg != null ) {
	msg = exMsg;
      } else {
	msg = exName;
      }
    }
    return msg != null ? msg : "Unbekannter Fehler";
  }


  public static boolean equals( Object o1, Object o2 )
  {
    boolean rv = false;
    if( (o1 != null) && (o2 != null) ) {
      rv = o1.equals( o2 );
    } else if( (o1 == null) && (o2 == null) ) {
      rv = true;
    }
    return rv;
  }


  public static boolean equalsLookAndFeel( String className )
  {
    boolean rv = false;
    if( className != null ) {
      if( className.length() > 0 ) {
        LookAndFeel lf = UIManager.getLookAndFeel();
        if( lf != null ) {
          rv = className.equals( lf.getClass().getName() );
	}
      }
    }
    return rv;
  }


  public static boolean equalsRegion(
				byte[] a1,
				int    idx1,
				byte[] a2,
				int    idx2,
				int    len )
  {
    boolean rv = false;
    if( (a1 != null) && (a2 != null)
	&& (idx1 >= 0) && (idx2 >= 0) )
    {
      if( ((idx1 + len) <= a1.length)
	  && ((idx2 + len) <= a2.length) )
      {
	rv = true;
	for( int i = 0; i < len; i++ ) {
	  if( a1[ idx1++ ] != a2[ idx2++ ] ) {
	    rv = false;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public static String extractSingleAsciiLine(
					byte[] buf,
					int    pos,
					int    len )
  {
    String rv = null;
    if( buf != null ) {
      StringBuilder tmpBuf  = null;
      int           nSp     = 0;
      int           nRemain = len;
      while( (pos < buf.length) && (nRemain > 0) ) {
	int ch = (int) buf[ pos++ ] & 0xFF;
	if( (ch == 0) || (ch == '\r') || (ch == '\n') ) {
	  break;
	}
	if( ch <= 0x20 ) {
	  nSp++;
	} else {
	  if( ch > 0x7E ) {
	    ch = '_';
	  }
	  if( tmpBuf == null ) {
	    tmpBuf = new StringBuilder( len );
	  } else {
	    for( int i = 0; i < nSp; i++ ) {
	      tmpBuf.append( '\u0020' );
	    }
	  }
	  tmpBuf.append( (char) ch );
	  nSp = 0;
	}
	--nRemain;
      }
      if( tmpBuf != null ) {
	rv = tmpBuf.toString();
      }
    }
    return rv;	
  }


  public static void fillRandom( byte[] a, int begIdx )
  {
    if( a != null ) {
      Random random = getRandom();
      for( int i = begIdx; i < a.length; i++ ) {
	a[ i ] = (byte) (random.nextInt() & 0xFF);
      }
    }
  }


  public static void fireExitSysError(
				final Component owner,
				final String    msg,
				final Exception ex )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    exitSysError( owner, msg, ex );
		  }
		} );
  }


  public static void fireSelectRow( final JList<?> list, final Object value )
  {
    if( value != null ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    list.setSelectedValue( value, true );
		  }
		} );
    }
  }


  public static void fireSelectRow( final JList<?> list, final int row )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    list.setSelectedIndex( row );
		  }
		} );
  }


  public static void fireSelectRow( final JTable table, final int row )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    if( (row >= 0) && (row < table.getRowCount()) ) {
		      table.clearSelection();
		      table.setRowSelectionInterval( row, row );
		    }
		  }
		} );
  }


  public static void fireSelectRows( final JList<?> list, final int[] rows )
  {
    if( rows != null ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    try {
		      list.setSelectedIndices( rows );
		    }
		    catch( Exception ex ) {}
		  }
		} );
    }
  }


  public static void fireShowErrorDlg(
			final Component owner,
			final String    msg,
			final Exception ex )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    BaseDlg.showErrorDlg( owner, msg, ex );
		  }
		} );
  }


  public static void fireShowInfoDlg(
			final Component owner,
			final String    msg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    BaseDlg.showInfoDlg( owner, msg );
		  }
		} );
  }


  public static String formatInt( int value )
  {
    return getIntegerFormat().format( value );
  }


  public static String formatSize(
				long    size,
				boolean forceFixLen,
				boolean forceSizeInBytes )
  {
    StringBuilder buf = new StringBuilder( 64 );
    appendSizeText( buf, size, forceFixLen, forceSizeInBytes );
    return buf.toString();
  }


  public static void frameToFront( Component c )
  {
    while( c != null ) {
      if( c instanceof Frame ) {
	showFrame( (Frame) c );
	break;
      }
      c = c.getParent();
    }
  }


  public static int getBasicMemWord( EmuMemView memory, int addr )
  {
    return (memory.getBasicMemByte( addr + 1 ) << 8)
			| memory.getBasicMemByte( addr );
  }


  public static boolean getBooleanProperty(
				Properties props,
				String     keyword,
				boolean    defaultValue )
  {
    boolean rv = defaultValue;
    if( props != null ) {
      String s = props.getProperty( keyword );
      if( s != null ) {
	s = s.trim();
	if( s.equals( "1" )
	    || s.equalsIgnoreCase( "Y" )
	    || s.equalsIgnoreCase( VALUE_TRUE )
	    || Boolean.parseBoolean( s ) )
	{
	  rv = true;
	}
	if( s.equals( "0" ) || s.equals( "N" ) || s.equals( VALUE_FALSE ) ) {
	  rv = false;
	}
      }
    }
    return rv;
  }


  public static Component[] getChildren( Component c )
  {
    Component[] children = null;
    if( c instanceof JMenu ) {
      children = ((JMenu) c).getMenuComponents();
    } else if( c instanceof Container ) {
      children = ((Container) c).getComponents();
    }
    return children;
  }


  public static Clipboard getClipboard( Component owner ) throws IOException
  {
    Clipboard clipboard = null;
    try {
      Toolkit tk = getToolkit( owner );
      if( tk != null ) {
	clipboard = getToolkit( owner ).getSystemClipboard();
      }
    }
    catch( Exception ex ) {}
    if( clipboard == null ) {
      throw new IOException(
		"Auf die Zwischenablage kann nicht zugegriffen werden." );
    }
    return clipboard;
  }


  public static Object getClipboardData(
				Component  owner,
				DataFlavor dataFlavor )
  {
    Object rv = null;
    try {
      Clipboard clipboard = getClipboard( owner );
      if( clipboard.isDataFlavorAvailable( dataFlavor ) ) {
	rv = clipboard.getData( dataFlavor );
      }
    }
    catch( Exception ex ) {}
    return rv;
  }


  public static String getClipboardText( Component owner )
  {
    Object o = getClipboardData( owner, DataFlavor.stringFlavor );
    return o != null ? o.toString() : null;
  }


  // DecimalFormat mit einer Nachkommastelle liefern
  public static DecimalFormat getDecimalFormatFix1()
  {
    if( decFmtFix1 == null ) {
      NumberFormat numFmt = NumberFormat.getNumberInstance();
      if( numFmt instanceof DecimalFormat ) {
	decFmtFix1 = (DecimalFormat) numFmt;
      } else {
	decFmtFix1 = new DecimalFormat();
      }
      decFmtFix1.applyPattern( "###,##0.0" );
    }
    return decFmtFix1;
  }


  // DecimalFormat mit einer Nachkommastelle liefern
  public static DecimalFormat getDecimalFormatMax1()
  {
    if( decFmtMax1 == null ) {
      NumberFormat numFmt = NumberFormat.getNumberInstance();
      if( numFmt instanceof DecimalFormat ) {
	decFmtMax1 = (DecimalFormat) numFmt;
      } else {
	decFmtMax1 = new DecimalFormat();
      }
      decFmtMax1.setMaximumFractionDigits( 1 );
    }
    return decFmtMax1;
  }


  /*
   * Die Methode liest aus einem Text an der angegebenen Stelle eine
   * vierstellige Hexadezimalzahl.
   * Die Gross-/Kleinschreibung ist egal.
   *
   * Rueckgabewert:
   *   >= 0: gelesene Hexadezimalzahl
   *   -1:   keine vierstellige Hexadezimalzahl an der Stelle vorhanden
   */
  public static int getHex4( String text, int pos )
  {
    int rv = -1;
    if( text != null ) {
      if( (pos + 3) < text.length() ) {
	boolean hex   = true;
	int     value = 0;
	for( int i = 0; i < 4; i++ ) {
	  char ch = text.charAt( pos + i );
	  if( (ch >= '0') && (ch <= '9') ) {
	    value = (value << 4) | (ch - '0');
	  } else if( (ch >= 'A') && (ch <= 'F') ) {
	    value = (value << 4) | (ch - 'A' + 10);
	  } else if( (ch >= 'a') && (ch <= 'f') ) {
	    value = (value << 4) | (ch - 'a' + 10);
	  } else {
	    hex = false;
	    break;
	  }
	}
	if( hex ) {
	  rv = value;
	}
      }
    }
    return rv;
  }


  public static char getHexChar( int value )
  {
    value &= 0x0F;
    return value <= 9 ? (char) (value + '0') : (char) (value - 10 + 'A');
  }


  public static float getFloat( JSpinner spinner )
  {
    float  rv = 0;
    Object o  = spinner.getValue();
    if( o != null ) {
      if( o instanceof Number ) {
	rv = ((Number) o).floatValue();
      }
    }
    return rv;
  }


  public static int getInt( JComboBox<?> combo )
  {
    int    rv = 0;
    Object o  = combo.getSelectedItem();
    if( o != null ) {
      if( o instanceof Number ) {
	rv = ((Number) o).intValue();
      } else {
	String s = o.toString();
	if( s != null ) {
	  try {
	    rv = Integer.parseInt( s.trim() );
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }


  public static int getInt( JSpinner spinner )
  {
    int    rv = 0;
    Object o  = spinner.getValue();
    if( o != null ) {
      if( o instanceof Number ) {
	rv = ((Number) o).intValue();
      }
    }
    return rv;
  }


  public static NumberFormat getIntegerFormat()
  {
    if( intFmt == null ) {
      intFmt = NumberFormat.getIntegerInstance();
      intFmt.setGroupingUsed( true );
    }
    return intFmt;
  }


  public static int getMenuShortcutKeyMask( Component c )
  {
    if( menuShortcutKeyMask == null ) {
      Toolkit tk = c.getToolkit();
      for( String methodName : new String[] {
					"getMenuShortcutKeyMaskEx",
					"getMenuShortcutKeyMask" } )
      {
	try {
	  Object v = tk.getClass().getMethod( methodName ).invoke( tk );
	  if( v != null ) {
	    if( v instanceof Number ) {
	      menuShortcutKeyMask = Integer.valueOf(
					((Number) v).intValue() );
	      break;
	    }
	  }
	}
	catch( Exception ex ) {}
      }
    }
    if( menuShortcutKeyMask == null ) {
      menuShortcutKeyMask = Integer.valueOf( InputEvent.CTRL_DOWN_MASK );
    }
    return menuShortcutKeyMask.intValue();
  }


  public static int getIntProperty(
				Properties props,
				String     keyword,
				int        defaultValue )
  {
    int rv = defaultValue;
    if( props != null ) {
      String s = props.getProperty( keyword );
      if( s != null ) {
	s = s.trim();
	if( !s.isEmpty() ) {
	  try {
	    rv = Integer.parseInt( s );
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }


  public synchronized static Random getRandom()
  {
    if( random == null ) {
      random = new Random();
      random.setSeed( System.currentTimeMillis() );
    }
    return random;
  }


  public static int getInt2BE( byte[] buf, int pos )
  {
    int rv = -1;
    if( buf != null ) {
      if( (pos >= 0) && (pos + 1 < buf.length) ) {
	rv = (((int) buf[ pos ] << 8) & 0xFF00)
		| ((int) buf[ pos + 1 ] & 0x00FF);
      }
    }
    return rv;
  }


  public static int getInt2LE( byte[] buf, int pos )
  {
    int rv = -1;
    if( buf != null ) {
      if( (pos >= 0) && (pos + 1 < buf.length) ) {
	rv = (((int) buf[ pos + 1 ] << 8) & 0xFF00)
		| ((int) buf[ pos ] & 0x00FF);
      }
    }
    return rv;
  }


  public static long getInt4BE( byte[] buf, int pos )
  {
    long rv = -1;
    if( buf != null ) {
      if( (pos >= 0) && (pos + 3 < buf.length) ) {
	rv = 0;
	for( int i = 0; i < 4; i++ ) {
	  rv <<= 8;
	  rv |= ((int) buf[ pos++ ] & 0xFF);
	}
      }
    }
    return rv;
  }


  public static long getInt4LE( byte[] buf, int pos )
  {
    long rv = -1;
    if( buf != null ) {
      if( (pos >= 0) && (pos + 3 < buf.length) )
	rv = ((buf[ pos + 3 ] << 24) & 0xFF000000)
		| ((buf[ pos + 2 ] << 16) & 0x00FF0000)
		| ((buf[ pos + 1 ] << 8) & 0x0000FF00)
		| (buf[ pos ] & 0x000000FF);
    }
    return rv;
  }


  public static Cell getModelCellAt( JTable table, Point point )
  {
    Cell cell = null;
    if( (table != null) && (point != null) ) {
      int col = table.columnAtPoint( point );
      int row = table.rowAtPoint( point );
      if( (col >= 0) && (row >= 0) ) {
	col = table.convertColumnIndexToModel( col );
	row = table.convertRowIndexToModel( row );
	if( (col >= 0) && (row >= 0) ) {
	  cell = new Cell( row, col );
	}
      }
    }
    return cell;
  }


  // Die Methode liefert niemals null zurueck
  public static String getProperty( Properties props, String keyword )
  {
    String rv = null;
    if( props != null ) {
      rv = props.getProperty( keyword );
    }
    if( rv != null ) {
      if( rv.trim().isEmpty() ) {
	rv = "";
      }
    } else {
      rv = "";
    }
    return rv;
  }


  public static Toolkit getToolkit( Component owner )
  {
    Toolkit tk = null;
    try {
      if( owner != null ) {
	tk = owner.getToolkit();
      }
      if( tk == null ) {
	tk = Toolkit.getDefaultToolkit();
      }
    }
    catch( AWTError e ) {}
    return tk;
  }


  public static Window getWindow( Component c )
  {
    while( c != null ) {
      if( c instanceof Window ) {
        return (Window) c;
      }
      c = c.getParent();
    }
    return null;
  }


  public static int getWord( byte[] buf, int pos )
  {
    int rv = -1;
    if( buf != null ) {
      if( (pos >= 0) && (pos + 1 < buf.length) )
	rv = ((buf[ pos + 1 ] << 8) & 0xFF00) | (buf[ pos ] & 0xFF);
    }
    return rv;
  }


  public static void initDRAM( byte[] ram )
  {
    if( ram != null ) {
      for( int i = 0; i < ram.length; i++ ) {
	ram[ i ] = (byte) ((i & 0x01) == 0 ? 0xFF : 0);
      }
    }
  }


  public static void initSRAM( byte[] ram, Properties props )
  {
    if( ram != null ) {
      if( isSRAMInit00( props ) ) {
	Arrays.fill( ram, (byte) 0x00 );
      } else {
	fillRandom( ram, 0 );
      }
    }
  }


  public static boolean isHexChar( int ch )
  {
    return ((ch >= '0') && (ch <= '9'))
		|| ((ch >= 'A') && (ch <= 'F'))
		|| ((ch >= 'a') && (ch <= 'f'));
  }


  public static boolean isSRAMInit00( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		PROP_SRAM_INIT ).equalsIgnoreCase( VALUE_SRAM_INIT_00 );
  }


  public static boolean isTextAt(
				CharSequence text,
				byte[]       fileBytes,
				int          offs )
  {
    boolean rv = false;
    if( (text != null) && (fileBytes != null) ) {
      int textLen = text.length();
      if( (offs + textLen) <= fileBytes.length ) {
	rv = true;
	for( int i = 0; i < textLen; i++ ) {
	  if( ((char) fileBytes[ offs + i ] & 0xFF) != text.charAt( i ) ) {
	    rv = false;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public static void logSysError(
				Component owner,
				String    msg,
				Exception ex )
  {
    File          errFile = null;
    StringBuilder errBuf  = new StringBuilder( 512 );
    errBuf.append( "Es ist ein interner Applikationsfehler"
				+ " aufgetreten." );

    // Fehlerprotokoll in Datei schreiben
    PrintWriter errWriter = null;
    try {
      String fName = "jkcemu_err.log";
      File   fDir  = FileUtil.getHomeDirFile();
      errFile = (fDir != null ?
			new File( fDir, fName )
			: new File( fName ));
      errWriter = new PrintWriter( new FileWriter( errFile ) );

      errWriter.println( "Bitte senden Sie diese Datei an"
					+ " info@jens-mueller.org" );
      errWriter.println( "Please send this file to"
					+ " info@jens-mueller.org" );
      errWriter.println();
      errWriter.println( "--- Program ---" );
      errWriter.write( Main.APPINFO );
      errWriter.println();

      if( msg != null ) {
	errWriter.println();
	errWriter.println( "--- Fehlermeldung ---" );
	errWriter.println( msg );
	errWriter.println();
      }

      if( ex != null ) {
	errWriter.println();
	errWriter.println( "--- Stack Trace ---" );
	ex.printStackTrace( errWriter );
	errWriter.println();
      }

      Properties props = System.getProperties();
      if( props != null ) {
	errWriter.println();
	errWriter.println( "--- Properties ---" );
	props.list( errWriter );
	errWriter.println();
      }

      if( errWriter.checkError() ) {
	errFile = null;
      }
      errWriter.close();
      errWriter = null;
    }
    catch( Exception ex2 ) {
      errFile = null;
    }
    finally {
      if( errWriter != null ) {
	errWriter.close();
      }
    }

    // Fehlerausschrift
    if( errFile != null ) {
      errBuf.append( "\nEin Protokoll des Fehlers wurde"
			+ " in die Textdatei\n\'" );
      errBuf.append( errFile.getPath() );
      errBuf.append( "\' geschrieben.\n"
			+ "Bitte senden Sie diese Textdatei" );
    } else {
      errBuf.append( "\nBitte melden Sie diesen Fehler" );
    }
    errBuf.append( " einschlie\u00DFlich einer\n"
		+ "kurzen Beschreibung Ihrer letzten Aktionen"
		+ " per E-Mail an:\n"
		+ "info@jens-mueller.org\n\n"
		+ "Vielen Dank!" );
    BaseDlg.showErrorDlg(
		owner != null ? owner : new Frame(),
		errBuf.toString(),
		"Applikationsfehler" );
  }


  public static Long parseOctalNumber( byte[] buf, int pos, int endPos )
  {
    while( (pos < endPos) && (buf[ pos ] == '\u0020') ) {
      pos++;
    }
    long    value = 0;
    boolean valid = false;
    while( pos < endPos ) {
      int b = buf[ pos++ ] & 0xFF;
      if( (b < '0') || (b > '7') ) {
        break;
      }
      value = (value << 3) | (b - '0');
      valid = true;
    }
    return valid ? Long.valueOf( value ) : null;
  }


  /*
   * Diese Methode liesst von einem Stream solange Daten,
   * bis das Ziel-Array voll ist oder das Streamende erreicht wurde.
   * Beim Lesen eines Arrays aus einer im JAR-Archiv befindlichen Datei
   * oder aus einem GZIP-Stream wird haeufig
   * nur ein Teil der Bytes gelesen.
   * -> Solange "read" aufrufen, bis alle Bytes gelesen wurden
   *
   * Rueckgabewert:
   *	Anzahl der gelesenen Bytes
   */
  public static int read( InputStream in, byte[] buf ) throws IOException
  {
    int rv  = 0;
    int pos = 0;
    while( pos < buf.length ) {
      int n = in.read( buf, pos, buf.length - pos );
      if( n > 0 ) {
	pos += n;
	rv += n;
      } else {
	break;
      }
    }
    return rv;
  }


  public static byte[] readResource( Component owner, String resource )
  {
    ByteArrayOutputStream buf  = new ByteArrayOutputStream( 0x0800 );
    boolean               done = false;
    InputStream           is   = null;
    InputStream           in   = null;
    Exception             ex   = null;
    try {
      in = owner.getClass().getResourceAsStream( resource );
      if( in != null ) {
	if( resource.endsWith( ".gz" ) ) {
	  is = in;
	  in = new GZIPInputStream( in );
	}
	int b = in.read();
	while( b != -1 ) {
	  buf.write( b );
	  b = in.read();
	}
	done = true;
      }
    }
    catch( IOException ioEx ) {
      ex = ioEx;
    }
    finally {
      closeSilently( in );
      closeSilently( is );
    }
    if( !done ) {
      fireExitSysError(
		owner,
		String.format(
			"Resource %s kann nicht geladen werden",
			resource ),
		ex );
    }
    return buf.toByteArray();
  }


  public static void removePropertiesByPrefix(
				Properties props,
				String     prefix )
  {
    if( props != null ) {
      for( Object o : props.keySet().toArray() ) {
	String s = o.toString();
	if( s.startsWith( prefix ) ) {
	  props.remove( o );
	}
      }
    }
  }


  public static void setDirectAccelerator(
					JMenuItem item,
					int       keyCode,
					boolean   shiftDown )
  {
    item.setAccelerator(
		KeyStroke.getKeyStroke(
			keyCode,
			shiftDown ? InputEvent.SHIFT_DOWN_MASK : 0 ) );
  }


  public static void setProperty(
			Properties props,
			String     keyword,
			boolean    value )
  {
    if( props != null )
      props.setProperty( keyword, Boolean.toString( value ) );
  }


  public static void setProperty(
			Properties props,
			String     keyword,
			int        value )
  {
    if( props != null )
      props.setProperty( keyword, Integer.toString( value ) );
  }


  public static void setProperty(
			Properties props,
			String     keyword,
			Object     value )
  {
    if( props != null ) {
      String s = (value != null ? value.toString() : null);
      props.setProperty( keyword, s != null ? s : "" );
    }
  }


  public static void setSelectedItem( JComboBox<?> combo, String text )
  {
    int idx = 0;
    if( text != null ) {
      if( !text.isEmpty() ) {
	int n = combo.getItemCount();
	for( int i = 0; i < n; i++ ) {
	  Object o = combo.getItemAt( i );
	  if( o != null ) {
	    String s = o.toString();
	    if( s != null ) {
	      if( s.equals( text ) ) {
		idx = i;
		break;
	      }
	    }
	  }
	}
      }
    }
    try {
      combo.setSelectedIndex( idx );
    }
    catch( IllegalArgumentException ex ) {}
  }


  public static void setStandardAccelerator(
					JMenuItem item,
					int       keyCode,
					boolean   shiftDown )
  {
    try {
      int modifiers = getMenuShortcutKeyMask( item );
      if( shiftDown ) {
	modifiers |= InputEvent.SHIFT_DOWN_MASK;
      }
      item.setAccelerator( KeyStroke.getKeyStroke( keyCode, modifiers ) );
    }
    catch( AWTError e ) {}
  }


  public static void setTableColWidths( JTable table, int... colWidths )
  {
    if( (table != null) && (colWidths != null) ) {
      TableColumnModel tcm = table.getColumnModel();
      if( tcm != null ) {
	int n = Math.min( tcm.getColumnCount(), colWidths.length );
	for( int i = 0; i < n; i++ ) {
	  TableColumn c = tcm.getColumn( i );
	  if( c != null ) {
	    c.setPreferredWidth( colWidths[ i ] );
	  }
	}
      }
    }
  }


  public static void setValue( JSpinner spinner, int value )
  {
    try {
      spinner.setValue( value );
    }
    catch( IllegalArgumentException ex ) {
      logSysError( null, null, ex );
    }
  }


  public static <T extends Frame> T showFrame( final T frame )
  {
    if( frame != null ) {
      if( frame.isVisible() ) {
	int frameState = frame.getExtendedState();
	if( (frameState & Frame.ICONIFIED) != 0 ) {
	  frame.setExtendedState( frameState & ~Frame.ICONIFIED );
	}
      } else {
	frame.setVisible( true );
	frame.setExtendedState( Frame.NORMAL );
      }
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    frame.toFront();
		  }
		} );
    }
    return frame;
  }


  public static void stopCellEditing( JTable table )
  {
    if( table != null ) {
      TableCellEditor editor = table.getCellEditor();
      if( editor != null ) {
	if( editor instanceof DefaultCellEditor ) {
	  ((DefaultCellEditor) editor).stopCellEditing();
	}
      }
    }
  }


  public static void throwMysteriousData() throws IOException
  {
    throw new IOException( "Datei enth\u00E4lt mysteri\u00F6se Daten." );
  }


  public static void updComponentTreeUI(
				Component  c,
				Properties props,
				boolean    updLAF,
				boolean    updFonts,
				boolean    updIcons )
  {
    if( (c != null) && (updLAF || updFonts || updIcons) ) {
      updComponentTreeUIInternal( c, updLAF, updFonts, updIcons );
      if( c instanceof BaseFrm ) {
	((BaseFrm) c).updUI( props, updLAF, updFonts, updIcons );
      }
      c.revalidate();
      c.repaint();
    }
  }


  public static void writeASCII(
			OutputStream out,
			CharSequence text ) throws IOException
  {
    if( text != null ) {
      checkASCII( text );
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	out.write( text.charAt( i ) );
      }
    }
  }


  public static void writeFixLengthASCII(
			OutputStream out,
			CharSequence text,
			int          len,
			int          filler ) throws IOException
  {
    if( text != null ) {
      checkASCII( text );
      int srcLen = text.length();
      int srcIdx = 0;
      while( (len > 0) && (srcIdx < srcLen) ) {
	out.write( text.charAt( srcIdx++ ) );
	--len;
      }
    }
    while( len > 0 ) {
      out.write( 0 );
      --len;
    }
  }


  public static void writeInt2BE(
				OutputStream out,
				int          v ) throws IOException
  {
    out.write( (v >> 8) & 0xFF );
    out.write( v & 0xFF );
  }


  public static void writeInt2LE(
				OutputStream out,
				int          v ) throws IOException
  {
    out.write( v & 0xFF );
    out.write( (v >> 8) & 0xFF );
  }


  public static void writeInt4BE(
				OutputStream out,
				long         v ) throws IOException
  {
    out.write( (int) ((v >> 24) & 0xFF) );
    out.write( (int) ((v >> 16) & 0xFF) );
    out.write( (int) ((v >> 8) & 0xFF) );
    out.write( (int) (v & 0xFF) );
  }


  public static void writeInt4LE(
				OutputStream out,
				long         v ) throws IOException
  {
    writeInt2LE( out, (int) v );
    writeInt2LE( out, (int) (v >> 16) );
  }


	/* --- private Methoden --- */

  private static void checkASCII( CharSequence text )
					throws CharConversionException
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	char ch = text.charAt( i );
	if( ((ch < '\u0020') || (ch > '\u007E'))
	    && (ch != '\n') && (ch != '\r') && (ch != '\u001A') )
	{
	  StringBuilder buf = new StringBuilder( 256 );
	  buf.append( "Nicht-ASCII-Zeichen" );
	  if( (ch > '\u0020')
	      && !Character.isSpaceChar( ch )
	      && !Character.isWhitespace( ch )
	      && !Character.isISOControl( ch ) )
	  {
	    buf.append( " \'" );
	    buf.append( ch );
	    buf.append( '\'' );
	  }
	  buf.append( " im Text enthalten" );
	  if( len > 1 ) {
	    buf.append( "\n\nText:\n" );
	    buf.append( text );
	  }
	  throw new CharConversionException( buf.toString() );
	}
      }
    }
  }


  private static void exitSysError(
				Component owner,
				String    msg,
				Exception ex )
  {
    ScreenFrm screenFrm = Main.getScreenFrm();
    if( screenFrm != null ) {
      EmuThread emuThread = screenFrm.getEmuThread();
      if( emuThread != null ) {
	emuThread.stopEmulator();
      }
    }
    logSysError( owner, msg, ex );
    Main.exitFailure();
  }


  private static void updComponentTreeUIInternal(
					Component c,
					boolean   updLAF,
					boolean   updFonts,
					boolean   updIcons )
  {
    if( c != null ) {
      if( c instanceof JComponent ) {
	if( updLAF ) {
	  ((JComponent) c).updateUI();
	}
	if( updFonts ) {
	  GUIFactory.updFont( c );
	}
	if( updIcons ) {
	  GUIFactory.updIcon( c );
	}
      }
      Component[] children = getChildren( c );
      if( children != null ) {
	for( Component child : children ) {
	  updComponentTreeUIInternal( child, updLAF, updFonts, updIcons );
	}
      }
      if( c instanceof PopupMenusOwner ) {
	JPopupMenu[] menus = ((PopupMenusOwner) c).getPopupMenus();
	if( menus != null ) {
	  for( JPopupMenu menu : menus ) {
	    updComponentTreeUIInternal( menu, updLAF, updFonts, updIcons );
	  }
	}
      }
      if( c instanceof PopupMenuOwner ) {
	updComponentTreeUIInternal(
			((PopupMenuOwner) c).getPopupMenu(),
			updLAF,
			updFonts,
			updIcons );
      }
    }
  }


	/* --- Konstruktor --- */

  private EmuUtil()
  {
    // Klasse nicht instanziierbar
  }
}
