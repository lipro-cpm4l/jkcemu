/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen
 */

package jkcemu.base;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.*;
import jkcemu.Main;
import jkcemu.text.TextUtil;


public class EmuUtil
{
  public static String[] headersaveFileTypeItems = {
				"",
				"A - Assemblerquelltext",
				"B - BASIC-Programm",
				"b - Tiny-BASIC-Programm",
				"C - MC-Programm, selbststartend",
				"M - MC-Programm",
				"E - EPROM-Inhalt",
				"I - Information (Text)",
				"T - Text" };

  public static final String[] archiveFileExtensions  = {
					".jar", ".tar.gz", ".tar", ".tgz",
					".zip" };

  public static final String[] textFileExtensions  = {
					".asc", ".asm", ".bas", ".bat",
					".c", ".cc", ".cmd", ".cpp", ".csh",
					".h", ".java", ".log", ".sh", ".txt" };

  private static DecimalFormat decFmtFix1 = null;
  private static DecimalFormat decFmtMax1 = null;
  private static NumberFormat  intFmt     = null;
  private static Random        random     = null;

  private static Map<String,javax.swing.filechooser.FileFilter>
							fmt2FileFilter = null;


  public static void addHeadersaveFileTypeItemsTo( JComboBox<String> combo )
  {
    for( String s : headersaveFileTypeItems ) {
      combo.addItem( s );
    }
  }


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
	      buf.append( (char) '\u0020' );
	    }
	    else if( ch > '\u007E' ) {
	      buf.append( "&#" );
	      buf.append( String.valueOf( ch ) );
	      buf.append( (char) ';' );
	    } else {
	      buf.append( (char) ch );
	    }
	}
      }
    }
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
	  buf.append( (char) ')' );
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


  public static File askForOutputDir(
				Window owner,
				File   presetDir,
				String presetName,
				String msg,
				String title )
  {
    File dirFile = null;
    if( (presetDir != null) && (presetName != null) ) {
      if( !presetName.isEmpty() ) {
	presetName = (new File( presetDir, presetName )).getPath();
      }
    }
    String dirName = ReplyDirDlg.showReplyDirDlg(
						owner,
						msg,
						title,
						presetName );
    if( dirName != null ) {
      if( !dirName.isEmpty() ) {
	dirFile = new File( dirName );
	if( (presetDir != null) && !dirFile.isAbsolute() ) {
	  dirFile = new File( presetDir, dirName );
	}
	if( dirFile != null ) {
	  if( dirFile.exists() ) {
	    if( dirFile.isDirectory() ) {
	      StringBuilder buf = new StringBuilder( 256 );
	      buf.append( dirFile.getPath() );
	      buf.append( "\nexistiert bereits" );
	      File[] tmpEntries = dirFile.listFiles();
	      if( tmpEntries != null ) {
		if( tmpEntries.length > 0 ) {
		  buf.append( " und enth\u00E4lt Dateien,\n"
			+ "die m\u00F6glicherweise \u00FCberschrieben"
			+ " werden" );
		}
	      }
	      buf.append( ".\nM\u00F6chten Sie das Verzeichnis verwenden?" );
	      if( !BasicDlg.showYesNoDlg( owner, buf.toString() ) ) {
		dirFile = null;
	      }
	    } else {
	      BasicDlg.showErrorDlg(
		owner,
		dirFile.getPath() + " existiert bereits\n"
			+ "und kann nicht als Verzeichnis angelegt werden." );
	      dirFile = null;
	    }
	  }
	}
      }
    }
    return dirFile;
  }


  public static File askForOutputDir(
				Window owner,
				File   srcFile,
				String msg,
				String title )
  {
    String presetName = null;
    File   parentFile = null;
    if( srcFile != null ) {
      String srcFileName = srcFile.getName();
      if( srcFileName != null ) {
	if( !srcFileName.isEmpty() ) {
	  int pos = srcFileName.lastIndexOf( "." );
	  if( pos > 0 ) {
	    presetName = srcFileName.substring( 0, pos );
	  } else {
	    presetName = srcFileName + ".d";
	  }
	}
      }
      parentFile = srcFile.getParentFile();
    }
    return askForOutputDir( owner, parentFile, presetName, msg, title );
  }


  public static int compare( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return s1.compareTo( s2 != null ? s2 : "" );
  }


  public static Pattern compileFileNameMask( String text )
					throws PatternSyntaxException
  {
    final String specialChars = "\\.[]()^$";
    Pattern      pattern      = null;
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
        StringBuilder buf = new StringBuilder( len + 10 );
        for( int i = 0; i < len; i++ ) {
          char ch = text.charAt( i );
          switch( ch ) {
            case '*':
              buf.append( ".*" );
              break;
            case '?':
              buf.append( (char) '.' );
              break;
            default:
              if( specialChars.indexOf( ch ) >= 0 ) {
                buf.append( (char) '\\' );
              }
              buf.append( ch );
          }
        }
	pattern = Pattern.compile(
			buf.toString(),
			Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
      }
    }
    return pattern;
  }


  public static void copyToClipboard( Component owner, String text )
  {
    try {
      if( text != null ) {
	if( !text.isEmpty() ) {
	  Toolkit tk = owner.getToolkit();
	  if( tk != null ) {
	    Clipboard clp = tk.getSystemClipboard();
	    if( clp != null ) {
	      StringSelection ss = new StringSelection( text );
	      clp.setContents( ss, ss );
	    }
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  public static File createDir( Component owner, File parent )
  {
    File rvFile = null;
    if( parent != null ) {
      String dirName = ReplyTextDlg.showReplyTextDlg(
				owner,
				"Verzeichnisname:",
				"Verzeichnis erstellen",
				null );
      if( dirName != null ) {
	dirName = dirName.trim();
	if( dirName.length() > 0 ) {
	  File replyFile = new File( dirName );
	  if( !replyFile.isAbsolute() ) {
	    replyFile = new File( parent, dirName );
	  }
	  if( replyFile.mkdirs() ) {
	    rvFile = replyFile;
	  } else {
	    BasicDlg.showErrorDlg(
			owner,
			"Verzeichnis konnte nicht erstellt werden." );
	  }
	}
      }
    }
    return rvFile;
  }


  public static JButton createImageButton(
				Window window,
				String imgName,
				String text )
  {
    JButton btn = null;
    Image   img = Main.getImage( window, imgName );
    if( img != null ) {
      btn = new JButton( new ImageIcon( img ) );
      btn.setToolTipText( text );
    } else {
      btn = new JButton( text );
    }
    return btn;
  }


  public static InputStream createInputStream( final RandomAccessFile raf )
  {
    return new InputStream()
		{
		  @Override
		  public void close() throws IOException
		  {
		    raf.close();
		  }

		  @Override
		  public int read() throws IOException
		  {
		    return raf.read();
		  }

		  @Override
		  public int read( byte[] buf ) throws IOException
		  {
		    return raf.read( buf );
		  }

		  @Override
		  public int read( byte[] buf, int pos, int len )
							throws IOException
		  {
		    return raf.read( buf, pos, len );
		  }

		  @Override
		  public long skip( long n ) throws IOException
		  {
		    long rv = 0L;
		    if( n > Integer.MAX_VALUE ) {
		      long filePos = raf.getFilePointer();
		      long fileLen = raf.length();
		      if( (filePos + n) > fileLen ) {
			raf.seek( fileLen );
			rv = fileLen - filePos;
		      } else {
			raf.seek( filePos + n );
			rv = n;
		      }
		    } else {
		      rv = raf.skipBytes( (int) n );
		    }
		    return rv;
		  }
		};
  }


  public static OutputStream createOptionalGZipOutputStream( File file )
							throws IOException
  {
    boolean      gzip = isGZipFile( file );
    OutputStream out  = new FileOutputStream( file );
    if( gzip ) {
      try {
	out = new GZIPOutputStream( out );
      }
      catch( IOException ex ) {
	doClose( out );
	throw ex;
      }
    }
    return out;
  }


  public static Set<Path> createPathSet()
  {
    return new TreeSet<>( createPathComparator() );
  }


  public static void doClose( Closeable stream )
  {
    if( stream != null ) {
      try {
	stream.close();
      }
      catch( IOException ex ) {}
    }
  }


  public static void doRelease( FileLock fileLock )
  {
    if( fileLock != null ) {
      try {
	fileLock.release();
      }
      catch( IOException ex ) {}
    }
  }


  public static boolean equals( File f1, File f2 )
  {
    boolean rv = false;
    if( (f1 != null) && (f2 != null) ) {
      rv = TextUtil.equals( f1.getPath(), f2.getPath() );
    } else {
      if( (f1 == null) && (f2 == null) ) {
	rv = true;
      }
    }
    return rv;
  }


  public static boolean equalsLookAndFeel( String className )
  {
    boolean rv = false;
    if( className != null ) {
      if( className.length() > 0 ) {
        LookAndFeel lf = UIManager.getLookAndFeel();
        if( lf != null )
          rv = className.equals( lf.getClass().getName() );
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



  public static void exitSysError(
				Component parent,
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

    File          errFile = null;
    StringBuilder errBuf  = new StringBuilder( 512 );
    errBuf.append( "Es ist ein interner Applikationsfehler aufgetreten." );

    // Fehlerprotokoll in Datei schreiben
    PrintWriter errWriter = null;
    try {
      String fName = "jkcemu_err.log";
      File   fDir  = getHomeDirFile();
      errFile = (fDir != null ?
			new File( fDir, fName )
			: new File( fName ));
      errWriter = new PrintWriter( new FileWriter( errFile ) );

      errWriter.println( "Bitte senden Sie diese Datei an"
						+ " info@jens-mueller.org" );
      errWriter.println( "Please send this file to info@jens-mueller.org" );
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
    if( parent == null ) {
      parent = new Frame();
    }
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
		+ "kurzen Beschreibung Ihrer letzten Aktionen per E-Mail an:\n"
		+ "info@jens-mueller.org\n\n"
		+ "Vielen Dank!" );
    BasicDlg.showErrorDlg( parent, errBuf.toString(), "Applikationsfehler" );
    Main.exitFailure();
  }


  public static int[] extractAddressesFromFileName( String fileName )
  {
    int[] rv = null;
    if( fileName != null ) {
      long m   = 0L;
      int  n   = 0;
      int  len = fileName.length();
      int  pos = fileName.indexOf( '_' );
      while( (n < 3) && (pos >= 0) ) {
	if( (pos + 4) >= len ) {
	  break;
	}
	pos++;
	long value = getHex4( fileName, pos );
	if( (pos + 4) < len ) {
	  if( isHexChar( fileName.charAt( pos + 4 ) ) ) {
	    value = -1;
	  }
	}
	if( value >= 0 ) {
	  m = (m << 16) | value;
	  n++;
	  pos += 4;
	}
	if( pos >= len ) {
	  break;
	}
	pos = fileName.indexOf( '_', pos );
      }
      if( n > 0 ) {
	rv = new int[ n ];
	for( int i = n - 1; i >= 0; --i ) {
	  rv[ i ] = (int) (m & 0xFFFFL);
	  m >>= 16;
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
	      tmpBuf.append( (char) '\u0020' );
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


  public static File fileDrop( Component owner, DropTargetDropEvent e )
  {
    File file = null;
    if( isFileDrop( e ) ) {
      e.acceptDrop( DnDConstants.ACTION_COPY );	// Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
	try {
	  Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof Collection ) {
	      for( Object f : (Collection) o ) {
		if( f != null ) {
		  if( f instanceof File ) {
		    if( file == null ) {
		      file = (File) f;
		    } else {
		      BasicDlg.showErrorDlg(
				owner,
				"Bitte nur eine Datei hier hineinziehen!" );
		      file = null;
		      break;
		    }
		  }
		}
	      }
	    }
	  }
	}
	catch( Exception ex ) {}
      }
      e.dropComplete( file != null );
    } else {
      e.rejectDrop();
    }
    return file;
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


  public static void fireSelectRow( final JList list, final Object value )
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


  public static void fireSelectRow( final JList list, final int row )
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


  public static void fireSelectRows( final JList list, final int[] rows )
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


  public static void fireShowError(
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
		    BasicDlg.showErrorDlg( owner, msg, ex );
		  }
		} );
  }


  public static void fireShowInfo(
			final Component owner,
			final String    msg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    BasicDlg.showInfoDlg( owner, msg );
		  }
		} );
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
	s = s.trim().toUpperCase();
	if( s.equals( "1" )
	    || s.equals( "Y" )
	    || s.equals( "TRUE" )
	    || Boolean.parseBoolean( s ) )
	{
	  rv = true;
	}
	if( s.equals( "0" ) || s.equals( "N" ) || s.equals( "FALSE" ) ) {
	  rv = false;
	}
      }
    }
    return rv;
  }


  public static String getClipboardText( Component owner )
  {
    String rv = null;
    try {
      Toolkit tk = owner.getToolkit();
      if( tk != null ) {
	Clipboard clipboard = tk.getSystemClipboard();
	if( clipboard != null ) {
	  if( clipboard.isDataFlavorAvailable( DataFlavor.stringFlavor ) ) {
	    Object o = clipboard.getData( DataFlavor.stringFlavor );
	    if( o != null ) {
	      rv = o.toString();
	      if( rv != null ) {
		if( rv.isEmpty() ) {
		  rv = null;
		}
	      }
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}
    return rv;
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


  public static File getDestFile(
			File   srcFile,
			String extension,
			File   dirFile )
  {
    File rv = null;
    if( srcFile != null ) {
      if( dirFile != null ) {
	if( !dirFile.isDirectory() ) {
	  dirFile = dirFile.getParentFile();
	}
      }
      if( dirFile == null ) {
	dirFile = srcFile.getParentFile();
      }
      String fName = srcFile.getName();
      if( fName != null ) {
	int pos = fName.lastIndexOf( '.' );
	if( pos > 0 ) {
	  fName = fName.substring( 0, pos ) + extension;
	  if( dirFile != null ) {
	    rv = new File( dirFile, fName );
	  } else {
	    rv = new File( fName );
	  }
	}
      }
    }
    return rv;
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


  public static File getHomeDirFile()
  {
    File           rv  = null;
    FileSystemView fsv = FileSystemView.getFileSystemView();
    if( fsv != null ) {
      rv = fsv.getHomeDirectory();
    }
    if( rv == null ) {
      String homeDir = System.getProperty( "user.home" );
      if( homeDir != null ) {
	rv = new File( homeDir );
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


  public static char getHexChar( int value )
  {
    value &= 0x0F;
    return value <= 9 ? (char) (value + '0') : (char) (value - 10 + 'A');
  }


  public static javax.swing.filechooser.FileFilter getAC1Basic6FileFilter()
  {
    return getFileFilter( "AC1-BASIC6-Dateien (*.abc)", "abc" );
  }


  public static javax.swing.filechooser.FileFilter getAnaDiskFileFilter()
  {
    return getFileFilter( "AnaDisk-Dateien (*.dump)", "dump" );
  }


  public static javax.swing.filechooser.FileFilter getBasicFileFilter()
  {
    return getFileFilter( "BASIC-/RBASIC-Dateien (*.bas)", "bas" );
  }


  public static javax.swing.filechooser.FileFilter getBinaryFileFilter()
  {
    return getFileFilter( "Einfache Speicherabbilddateien (*.bin)", "bin" );
  }


  public static javax.swing.filechooser.FileFilter getCdtFileFilter()
  {
    return getFileFilter( "CPC-Tape-Dateien (*.cdt)", "cdt" );
  }


  public static javax.swing.filechooser.FileFilter getCswFileFilter()
  {
    return getFileFilter( "CSW-Dateien (*.csw)", "csw" );
  }


  public static javax.swing.filechooser.FileFilter getComFileFilter()
  {
    return getFileFilter( "CP/M-Programmdateien (*.com)", "com" );
  }


  public static javax.swing.filechooser.FileFilter getCopyQMFileFilter()
  {
    return getFileFilter( "CopyQM-Dateien (*.cqm; *.qm)", "cqm", "qm" );
  }


  public static javax.swing.filechooser.FileFilter getDskFileFilter()
  {
    return getFileFilter( "CPC-Disk-Dateien (*.dsk)", "dsk" );
  }


  public static javax.swing.filechooser.FileFilter getGIFFileFilter()
  {
    return getFileFilter( "GIF-Dateien (*.gif)", "gif" );
  }


  public static javax.swing.filechooser.FileFilter getHeadersaveFileFilter()
  {
    return getFileFilter( "Headersave-Dateien (*.z80)", "z80" );
  }


  public static javax.swing.filechooser.FileFilter getHexFileFilter()
  {
    return getFileFilter( "HEX-Dateien (*.hex)", "hex" );
  }


  public static javax.swing.filechooser.FileFilter getImageDiskFileFilter()
  {
    return getFileFilter( "ImageDisk-Dateien (*.imd)", "imd" );
  }


  public static javax.swing.filechooser.FileFilter getISOFileFilter()
  {
    return getFileFilter( "CD-/DVD-Abbilddateien (*.iso)", "iso" );
  }


  public static javax.swing.filechooser.FileFilter getKCBasicFileFilter()
  {
    return getFileFilter( "KC-BASIC-Dateien (*.sss)", "sss" );
  }


  public static javax.swing.filechooser.FileFilter getKCBasicSystemFileFilter()
  {
    return getFileFilter( "KC-BASIC-Systemdateien (*.kcb)", "kcb" );
  }


  public static javax.swing.filechooser.FileFilter getKCSystemFileFilter()
  {
    return getFileFilter( "KC-Systemdateien (*.kcc)", "kcc" );
  }


  public static javax.swing.filechooser.FileFilter getKCTapFileFilter()
  {
    return getFileFilter( "KC-TAP-Dateien (*.tap)", "tap" );
  }


  public static javax.swing.filechooser.FileFilter getPlainDiskFileFilter()
  {
    return getFileFilter(
		"Einfache Abbilddateien (*.img; *.image, *.raw)",
		"img", "image", "raw" );
  }


  public static javax.swing.filechooser.FileFilter getProjectFileFilter()
  {
    return getFileFilter( "Projekdateien (*.prj)", "prj" );
  }


  public synchronized static Random getRandom()
  {
    if( random == null ) {
      random = new Random();
      random.setSeed( System.currentTimeMillis() );
    }
    return random;
  }


  public static javax.swing.filechooser.FileFilter getRMCFileFilter()
  {
    return getFileFilter( "RBASIC-Maschinencodedateien (*.rmc)", "rmc" );
  }


  public static javax.swing.filechooser.FileFilter getROMFileFilter()
  {
    return getFileFilter( "ROM-Dateien (*.bin; *.rom)", "rom", "bin" );
  }


  public static javax.swing.filechooser.FileFilter getTapeFileFilter()
  {
    return getFileFilter(
			"Tape-Dateien (*.cdt; *.csw; *.tap; *.tzx)",
			"cdt", "csw", "tap", "tzx" );
  }


  public static javax.swing.filechooser.FileFilter getTeleDiskFileFilter()
  {
    return getFileFilter( "TeleDisk-Dateien (*.td0)", "td0" );
  }


  public static javax.swing.filechooser.FileFilter getTextFileFilter()
  {
    return getFileFilter(
			"Textdateien (*.asc; *.log; *.txt)",
			"asc", "log", "txt" );
  }


  public static javax.swing.filechooser.FileFilter getTzxFileFilter()
  {
    return getFileFilter( "ZX-Tape-Dateien (*.tzx)", "tzx" );
  }


  public static File getDirectory( File file )
  {
    if( file != null ) {
      FileSystemView fsv = FileSystemView.getFileSystemView();
      while( file != null ) {
	if( file.isDirectory() ) {
	  break;
	}
	if( fsv != null ) {
	  file = fsv.getParentDirectory( file );
	} else {
	  file = file.getParentFile();
	}
      }
    }
    return file;
  }


  public static long getInt4( byte[] buf, int pos )
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


  public static boolean isGZipFile( File file )
  {
    boolean rv = false;
    if( file != null ) {
      String fileName = file.getName();
      if( fileName != null ) {
	if( fileName.toLowerCase().endsWith( ".gz" ) ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  /*
   * Diese beiden Methoden pruefen, ob das uebergebene Drag&Drop-Event
   * fuer das Laden einer Datei geeignet ist.
   */
  public static boolean isFileDrop( DropTargetDragEvent e )
  {
    boolean rv = false;
    if( (e.getDropAction()
	 & (DnDConstants.ACTION_COPY
			| DnDConstants.ACTION_MOVE
			| DnDConstants.ACTION_LINK)) != 0 )
    {
      rv = e.isDataFlavorSupported( DataFlavor.javaFileListFlavor );
    }
    return rv;
  }


  public static boolean isFileDrop( DropTargetDropEvent e )
  {
    boolean rv = false;
    if( (e.getDropAction()
	 & (DnDConstants.ACTION_COPY
			| DnDConstants.ACTION_MOVE
			| DnDConstants.ACTION_LINK)) != 0 )
    {
      rv = e.isDataFlavorSupported( DataFlavor.javaFileListFlavor );
    }
    return rv;
  }


  public static boolean isHexChar( int ch )
  {
    return ((ch >= '0') && (ch <= '9'))
		|| ((ch >= 'A') && (ch <= 'F'))
		|| ((ch >= 'a') && (ch <= 'f'));
  }


  public static boolean isJKCEMUFileDialogSelected()
  {
    return !TextUtil.equalsIgnoreCase(
			Main.getProperty( "jkcemu.filedialog" ),
			"native" );
  }


  public static FileLock lockFile(
				File             file,
				RandomAccessFile raf ) throws IOException
  {
    FileLock    fl = null;
    FileChannel fc = raf.getChannel();
    if( fc != null ) {
      try {
        fl = fc.tryLock();
      }
      catch( OverlappingFileLockException ex ) {
	throw new IOException( file.getPath()
		+ ":\nDatei ist gesperrt.\n"
		+ "Bitte schlie\u00DFen Sie die Datei in dem Programm,\n"
		+ "in dem sie ge\u00F6ffnet ist." );
      }
      catch( IOException ex ) {
	throw new IOException( file.getPath()
			+ ":\nDatei kann nicht gesperrt werden." );
      }
    }
    return fl;
  }


  /*
   * Die Methode oeffnet eine Datei und gibt einen BufferedInputStream zurueck.
   * Wenn derDateiname auf ".gz" endet,
   * wird die Datei entsprechend entpackt.
   */
  public static BufferedInputStream openBufferedOptionalGZipFile( File file )
							throws IOException
  {
    InputStream in = null;
    if( isGZipFile( file ) ) {
      in = new FileInputStream( file );
      try {
	in = new GZIPInputStream( in );
      }
      catch( IOException ex ) {
	doClose( in );
	in = null;
      }
    }
    if( in == null ) {
      in = new FileInputStream( file );
    }
    return new BufferedInputStream( in );
  }


  public static boolean parseBoolean( String text, boolean defaultValue )
  {
    Boolean value = null;
    if( text != null ) {
      value = Boolean.valueOf( text );
    }
    return value != null ? value.booleanValue() : defaultValue;
  }


  public static boolean parseBooleanProperty(
				Properties props,
				String     keyword,
				boolean    defaultValue )
  {
    return parseBoolean(
		props != null ? props.getProperty( keyword ) : null,
		defaultValue );
  }


  public static int parseInt( String text, int minValue, int defaultValue )
  {
    int value = defaultValue;
    if( text != null ) {
      try {
	value = Integer.parseInt( text );
      }
      catch( NumberFormatException ex ) {}
    }
    return value >= minValue ? value : minValue;
  }


  public static int parseIntProperty(
				Properties props,
				String     keyword,
				int        minValue,
				int        defaultValue )
  {
    return parseInt(
		props != null ? props.getProperty( keyword ) : null,
		minValue,
		defaultValue );
  }


  public static void printErr( String text )
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.print( text );
      Main.consoleWriter.flush();
    } else {
      System.err.print( text );
    }
  }


  public static void printOut( String text )
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.print( text );
      Main.consoleWriter.flush();
    } else {
      System.out.print( text );
    }
  }


  public static void printlnErr()
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println();
      Main.consoleWriter.flush();
    } else {
      System.err.println();
    }
  }


  public static void printlnOut()
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println();
      Main.consoleWriter.flush();
    } else {
      System.out.println();
    }
  }


  public static void printlnErr( String text )
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println( text );
      Main.consoleWriter.flush();
    } else {
      System.err.println( text );
    }
  }


  public static void printlnOut( String text )
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println( text );
      Main.consoleWriter.flush();
    } else {
      System.out.println( text );
    }
  }


  /*
   * Diese Methode liesst von einem Stream solange Daten,
   * bis das Ziel-Array voll ist oder das Streamende erreicht wurde.
   * Beim Laden einer im JAR-Archiv befindlichen Datei
   * oder beim Lesen aus einem GZIP-Stream wird haeufig
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


  public static byte[] readFile(
			File    file,
			boolean allowUncompress,
			int     maxLen ) throws IOException
  {
    byte[] rv = null;
    if( file != null ) {
      long        len = file.length();
      InputStream in  = null;
      try {
	if( allowUncompress && EmuUtil.isGZipFile( file ) ) {
	  in = new GZIPInputStream( new FileInputStream( file ) );
	} else {
	  in = new FileInputStream( file );
	}
	if( len < 0 ) {
	  ByteArrayOutputStream buf = new ByteArrayOutputStream( 0x10000 );
	  int b = in.read();
	  while( (b != -1) && (maxLen > 0) ) {
	    buf.write( b );
	    b = in.read();
	    --maxLen;
	  }
	  rv = buf.toByteArray();
	}
	else if( len > 0 ) {
	  if( maxLen < 1 ) {
	    maxLen = Integer.MAX_VALUE;
	  }
	  rv    = new byte[ (int) Math.min( len, maxLen ) ];
	  int n = read( in, rv );
	  if( n < rv.length ) {
	    if( n > 0 ) {
	      byte[] a = new byte[ n ];
	      System.arraycopy( rv, 0, a, 0, a.length );
	      rv = a;
	    } else {
	      rv = null;
	    }
	  }
	}
      }
      finally {
	doClose( in );
      }
    }
    return rv;
  }


  public static byte[] readFile(
				Component owner,
				String    fileName,
				boolean   allowUncompress,
				int       maxLen,
				String    objName )
  {
    byte[] rv = null;
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	try {
	  rv = EmuUtil.readFile(
			new File( fileName ),
			allowUncompress,
			maxLen );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  BasicDlg.showErrorDlg(
			owner,
			String.format(
				"%s kann nicht geladen werden%s%s",
				objName,
				msg != null ? ":\n" : ".",
				msg != null ? msg : "" ) );
	  rv = null;
	}
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
      doClose( in );
      doClose( is );
    }
    if( !done ) {
      exitSysError(
		owner,
		String.format(
			"Resource %s kann nicht geladen werden",
			resource ),
		ex );
    }
    return buf.toByteArray();
  }


  public static File renameFile( Component owner, File file )
  {
    File newFile = null;
    if( file != null ) {
      try {
	Path newPath = renamePath( owner, file.toPath() );
	if( newPath != null ) {
	  try {
	    newFile = newPath.toFile();
	  }
	  catch( InvalidPathException ex ) {}
	}
      }
      catch( UnsupportedOperationException ex ) {
	BasicDlg.showErrorDlg(
		owner,
		"Umbenennen der Datei wird nicht unterst\u00FCtzt." );
      }
    }
    return newFile;
  }


  public static Path renamePath( Component owner, Path path )
  {
    Path newPath = null;
    if( path != null ) {
      String oldName  = null;
      Path   namePath = path.getFileName();
      if( namePath != null ) {
	oldName = namePath.toString();
      }
      String title     = "Datei umbenennen";
      String msgPrefix = "Die Datei";
      if( Files.isSymbolicLink( path ) ) {
	title     = "Symbolischer Link umbenennen";
	msgPrefix = "Der symbolische Link";
      } else if( Files.isDirectory( path ) ) {
	title     = "Verzeichnis umbenennen";
	msgPrefix = "Das Verzeichnis";
      }
      String newName = ReplyTextDlg.showReplyTextDlg(
				owner,
				"Neuer Name:",
				title,
				oldName != null ? oldName : "" );
      if( newName != null ) {
	try {
	  newPath = Files.move( path, path.resolveSibling( newName ) );
	}
	catch( Exception ex ) {
	  BasicDlg.showErrorDlg( owner, ex );
	}
      }
    }
    return newPath;
  }


  public static void setFileWritable( File file, boolean state )
  {
    if( file != null ) {
      if( file.setWritable( state, false ) ) {
	file.setWritable( state, true );
      }
    }
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


  public static void setProperty(
			Properties props,
			String     keyword,
			boolean    value )
  {
    if( props != null )
      props.setProperty( keyword, Boolean.toString( value ) );
  }


  public static boolean setSelectedHeadersaveFileTypeItem(
						JComboBox combo,
						int       fileType )
  {
    boolean rv = false;
    for( String s : headersaveFileTypeItems ) {
      if( !s.isEmpty() ) {
	if( s.charAt( 0 ) == fileType ) {
	  combo.setSelectedItem( s );
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


  public static void setTableColWidths( JTable table, int... colWidths )
  {
    if( (table != null) && (colWidths != null) ) {
      TableColumnModel tcm = table.getColumnModel();
      if( tcm != null ) {
	int n = Math.min( tcm.getColumnCount(), colWidths.length );
	for( int i = 0; i < n; i++ ) {
	  TableColumn c = tcm.getColumn( i );
	  if( c != null )
	    c.setPreferredWidth( colWidths[ i ] );
	}
      }
    }
  }


  public static File showFileOpenDlg(
			Window                                owner,
			String                                title,
			File                                  preSelection,
			javax.swing.filechooser.FileFilter... fileFilters )
  {
    preSelection       = getDirectory( preSelection );
    File  file         = null;
    if( isJKCEMUFileDialogSelected() ) {
      FileSelectDlg dlg = new FileSelectDlg(
				owner,
				FileSelectDlg.Mode.LOAD,
				false,		// kein Startknopf
				false,		// kein "Laden mit..."-Knopf
				title,
				preSelection,
				fileFilters );
      dlg.setVisible( true );
      file = dlg.getSelectedFile();
    } else {
      File[] files = showNativeFileDlg(
				owner,
				false,
				false,
				title,
				preSelection );
      if( files != null ) {
	if( files.length > 0 ) {
	  file = files[ 0 ];
	}
      }
    }
    return file;
  }


  public static void showFrame( final Frame frame )
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
  }


  public static File showFileSaveDlg(
			Window                                owner,
			String                                title,
			File                                  preSelection,
			javax.swing.filechooser.FileFilter... fileFilters )
  {
    File file = null;
    if( isJKCEMUFileDialogSelected() ) {
      FileSelectDlg dlg = new FileSelectDlg(
				owner,
				FileSelectDlg.Mode.SAVE,
				false,		// kein Startknopf
				false,		// kein "Laden mit..."-Knopf
				title,
				preSelection,
				fileFilters );
      dlg.setVisible( true );
      file = dlg.getSelectedFile();
    } else {
      File[] files = showNativeFileDlg(
				owner,
				true,
				false,
				title,
				preSelection );
      if( files != null ) {
	if( files.length > 0 ) {
	  file = files[ 0 ];
	}
      }
    }
    return file;
  }


  public static java.util.List<File> showMultiFileOpenDlg(
			Window                                owner,
			String                                title,
			File                                  preSelection,
			javax.swing.filechooser.FileFilter... fileFilters )
  {
    java.util.List<File> files = null;
    preSelection               = getDirectory( preSelection );
    if( isJKCEMUFileDialogSelected() ) {
      FileSelectDlg dlg = new FileSelectDlg(
				owner,
				FileSelectDlg.Mode.MULTIPLE_LOAD,
				false,		// kein Startknopf
				false,		// kein "Laden mit..."-Knopf
				title,
				preSelection,
				fileFilters );
      dlg.setVisible( true );
      files = dlg.getSelectedFiles();
    } else {
      File[] tmpFiles = showNativeFileDlg(
				owner,
				false,
				true,
				title,
				preSelection );
      if( tmpFiles != null ) {
	if( tmpFiles.length > 0 ) {
	  files = Arrays.asList( tmpFiles );
	}
      }
    }
    return files;
  }


  /*
   * Diese Methode sortiert ein File-Array anhand des logische Namens.
   * Zwar implementiert die Klasse File das Comparable-Interface,
   * jedoch wird dort der Pfad verglichen, was bei virtuellen Verzeichnissen
   * "Computer" oder "Netzwerk" nicht zu den gewuenschten Ergebnissen fuehrt.
   */
  public static void sortFilesByName( File[] files )
  {
    if( files != null ) {
      try {
	Arrays.sort(
		files,
		File.separatorChar == '/' ?
			ObjectByStringComparator.getInstance()
			: ObjectByStringComparator.getIgnoreCaseInstance() );
      }
      catch( ClassCastException ex ) {}
    }
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
	    buf.append( (char) '\'' );
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


  private static Comparator<Path> createPathComparator()
  {
    return new Comparator<Path>()
	{
	  @Override
	  public int compare( Path p1, Path p2 )
	  {
	    String s1 = (p1 != null ? p1.toString() : null);
	    String s2 = (p2 != null ? p2.toString() : null);
	    if( s1 == null ) {
	      s1 = "";
	    }
	    return s1.compareTo( s2 != null ? s2 : "" );
	  }
	};
  }


  private static javax.swing.filechooser.FileFilter getFileFilter(
							String    text,
							String... formats )
  {
    javax.swing.filechooser.FileFilter rv = null;
    if( (formats != null) && (text != null) ) {
      if( formats.length > 0 ) {
	if( fmt2FileFilter == null ) {
	  fmt2FileFilter = new HashMap<>();
	}
	rv = fmt2FileFilter.get( text );
	if( rv == null ) {
	  rv = new javax.swing.filechooser.FileNameExtensionFilter(
								text,
								formats );
	  fmt2FileFilter.put( text, rv );
	}
      }
    }
    return rv;
  }


  private static File[] showNativeFileDlg(
					Window  owner,
					boolean forSave,
					boolean multiMode,
					String  title,
					File    preSelection )
  {
    File[] files    = null;
    Dialog ownerDlg = null;
    Frame  ownerFrm = null;
    while( owner != null ) {
      if( owner instanceof Dialog ) {
	ownerDlg = (Dialog) owner;
	break;
      }
      if( owner instanceof Frame ) {
	ownerFrm = (Frame) owner;
	break;
      }
      owner = owner.getOwner();
    }
    FileDialog dlg = null;
    if( ownerDlg != null ) {
      dlg = new FileDialog(
			ownerDlg,
			title,
			forSave ? FileDialog.SAVE : FileDialog.LOAD );
    } else if( ownerFrm != null ) {
      dlg = new FileDialog(
			ownerFrm,
			title,
			forSave ? FileDialog.SAVE : FileDialog.LOAD );
    }
    if( dlg != null ) {
      dlg.setModalityType( Dialog.ModalityType.DOCUMENT_MODAL );
      dlg.setMultipleMode( !forSave && multiMode );
      dlg.setResizable( true );
      if( preSelection != null ) {
	if( preSelection.isDirectory() ) {
	  dlg.setDirectory( preSelection.getPath() );
	} else {
	  String dirName = preSelection.getParent();
	  if( dirName != null ) {
	    dlg.setDirectory( dirName );
	  }
	  if( !preSelection.exists() || preSelection.isFile() ) {
	    dlg.setFile( preSelection.getName() );
	  }
	}
      }
      BasicDlg.setParentCentered( dlg );
      dlg.setVisible( true );
      files = dlg.getFiles();
    }
    return files;
  }
}
