/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen
 */

package jkcemu.base;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import jkcemu.Main;


public class EmuUtil
{
  private static Map<String,javax.swing.filechooser.FileFilter>
							fmt2FileFilter = null;


  public static boolean applyWindowSettings(
					Properties props,
					BasicFrm   frm,
					boolean    resizable )
  {
    boolean rv = false;
    if( (frm != null) && (props != null) ) {
      String prefix = frm.getSettingsPrefix();

      int x = EmuUtil.parseInt(
			props.getProperty( prefix + ".window.x" ),
			-1,
			-1 );
      int y = EmuUtil.parseInt(
			props.getProperty( prefix + ".window.y" ),
			-1,
			-1 );
      if( (x >= 0) && (y >= 0) ) {
	if( resizable ) {
	  int w = EmuUtil.parseInt(
			props.getProperty( prefix + ".window.width" ),
			-1,
			-1 );
	  int h = EmuUtil.parseInt(
			props.getProperty( prefix + ".window.height" ),
			-1,
			-1 );

	  if( (w > 0) && (h > 0) ) {
	    frm.setBounds( x, y, w, h );
	    rv = true;
	  }
	} else {
	  frm.setLocation( x, y );
	  rv = true;
	}
      }
    }
    return rv;
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


  /*
   * Rueckgabewert:
   *  true:  Aktion ausgefuehrt
   *  false: Aktion abgebrochen
   */
  public static boolean deleteFiles( Component owner, File[] files )
  {
    boolean rv = false;
    if( files != null ) {
      File aFile  = null;
      File aDir   = null;
      int  nDirs  = 0;
      int  nFiles = 0;
      for( int i = 0; i < files.length; i++ ) {
	File file = files[ i ];
	if( file.isDirectory() ) {
	  aDir = file;
	  nDirs++;
	} else {
	  aFile = file;
	  nFiles++;
	}
      }
      if( (nDirs > 0) || (nFiles > 0) ) {
	StringBuilder buf = new StringBuilder( 128 );
	buf.append( "M\u00F6chten Sie " );
	if( nDirs > 0 ) {
	  if( nDirs == 1 ) {
	    if( aDir != null ) {
	      buf.append( "das Verzeichnis\n\'" );
	      buf.append( aDir.getPath() );
	      buf.append( "\'\n" );
	    } else {
	      buf.append( "ein Verzeichnis " );
	    }
	  } else {
	    buf.append( nDirs );
	    buf.append( " Verzeichnisse " );
	  }
	}
	if( nFiles > 0 ) {
	  if( nDirs > 0 ) {
	    buf.append( "und " );
	  }
	  if( nFiles == 1 ) {
	    if( aFile != null ) {
	      buf.append( "die Datei\n\'" );
	      buf.append( aFile.getPath() );
	      buf.append( "\'\n" );
	    } else {
	      buf.append( "eine Datei " );
	    }
	  } else {
	    buf.append( nFiles );
	    buf.append( " Dateien " );
	  }
	}
	buf.append( "l\u00F6schen?" );
	if( BasicDlg.showYesNoDlg( owner, buf.toString() ) ) {
	  RunStatus runStatus = new RunStatus();
	  for(
	    int k = 0;
	    (k < files.length) && !runStatus.wasCancelled();
	    k++ )
	  {
	    deleteFile( owner, files[ k ], runStatus );
	  }
	  if( runStatus.isQuiet()
	      && runStatus.wasFailed()
	      && !runStatus.wasCancelled() )
	  {
	    BasicDlg.showErrorDlg(
			owner,
			"Es konnten nicht alle Dateien/Verzeichnisse\n"
				+ " gel\u00F6scht werden." );
	  }
	  rv = true;
	}
      }
    }
    return rv;
  }


  public static JButton createImageButton( String imgName, String text )
  {
    JButton btn = null;
    Image   img = Main.getImage( imgName );
    if( img != null ) {
      btn = new JButton( new ImageIcon( img ) );
      btn.setToolTipText( text );
    } else {
      btn = new JButton( text );
    }
    return btn;
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


  public static String extractText(
				byte[] data,
				int    offs,
				int    nRows,
				int    nCols,
				int    colDist )
  {
    String rv = null;
    if( data != null ) {
      StringBuilder buf = new StringBuilder( nRows + (nCols + 1) );
      for( int i = 0; i < nRows; i++ ) {
	int rowIdx = offs + (i * colDist);
	int nSpaces = 0;
	for( int k = 0; k < nCols; k++ ) {
	  int b = 0;
	  int p = rowIdx + k;
	  if( (p >= 0) && (p < data.length) ) {
	    b = (int) (data[ p ] & 0xFF);
	  }
	  if( (b == 0) || b == 0x20 ) {
	    nSpaces++;
	  }
	  else if( (b > 0x20) && (b < 0x7F) ) {
	    while( nSpaces > 0 ) {
	      buf.append( (char) '\u0020' );
	      --nSpaces;
	    }
	    buf.append( (char) b );
	  }
	}
	buf.append( (char) '\n' );
      }
      rv = buf.toString();
    }
    return rv;
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
	if( s.length() > 0 ) {
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


  public static javax.swing.filechooser.FileFilter getBinaryFileFilter()
  {
    return getFileFilter( "Speicherabbilddateien (*.bin)", "bin" );
  }


  public static javax.swing.filechooser.FileFilter getHeadersaveFileFilter()
  {
    return getFileFilter( "Headersave-Dateien (*.z80)", "z80" );
  }


  public static javax.swing.filechooser.FileFilter getHexFileFilter()
  {
    return getFileFilter( "HEX-Dateien (*.hex)", "hex" );
  }


  public static javax.swing.filechooser.FileFilter getKCBasicFileFilter()
  {
    return getFileFilter( "KC-BASIC-Dateien (*.sss)", "sss" );
  }


  public static javax.swing.filechooser.FileFilter getKCSystemFileFilter()
  {
    return getFileFilter( "KC-Systemdateien (*.kcc)", "kcc" );
  }


  public static javax.swing.filechooser.FileFilter getProjectFileFilter()
  {
    return getFileFilter( "Projekdateien (*.prj)", "prj" );
  }


  public static javax.swing.filechooser.FileFilter getTapFileFilter()
  {
    return getFileFilter( "TAP-Dateien (*.tap)", "tap" );
  }


  public static javax.swing.filechooser.FileFilter getTextFileFilter()
  {
    return getFileFilter(
			"Textdateien (*.asc; *.log; *.txt)",
			"asc", "log", "txt" );
  }


  // Die Methode liefert niemals null zurueck
  public static String getProperty( Properties props, String keyword )
  {
    String rv = null;
    if( props != null ) {
      rv = props.getProperty( keyword );
    }
    return rv != null ? rv.trim() : "";
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


  /*
   * Die beiden Methode pruefen, ob das uebergebene Drag&Drop-Event
   * fuer das Laden einer Datei geeignet ist.
   */
  public static boolean isFileDrop( DropTargetDragEvent e )
  {
    boolean rv = false;
    int action = e.getDropAction();
    if( (action == DnDConstants.ACTION_COPY)
	|| (action == DnDConstants.ACTION_MOVE)
	|| (action == DnDConstants.ACTION_COPY_OR_MOVE)
	|| (action == DnDConstants.ACTION_LINK) )
    {
      rv = e.isDataFlavorSupported( DataFlavor.javaFileListFlavor );
    }
    return rv;
  }


  public static boolean isFileDrop( DropTargetDropEvent e )
  {
    boolean rv = false;
    int action = e.getDropAction();
    if( (action == DnDConstants.ACTION_COPY)
	|| (action == DnDConstants.ACTION_MOVE)
	|| (action == DnDConstants.ACTION_COPY_OR_MOVE)
	|| (action == DnDConstants.ACTION_LINK) )
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


  public static boolean isNativeFileDialogSelected()
  {
    return isEqualIgnoreCase(
			Main.getProperty( "jkcemu.filedialog" ),
			"native" );
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


  /*
   * Diese Methode liesst von einem Stream solange Daten,
   * bis das Ziel-Array voll ist oder das Streamende erreicht wurde.
   * Beim Laden einer im JAR-Archiv befindlichen Datei
   * wird beim Aufruf der Methode "read" nur ein Teil gelesen.
   * -> Solange "read" aufrufen, bis alle Bytes geladen sind
   *
   * Rueckgabewert:
   *	Anzahl der Bytes im Buffer
   */
  public static int read( InputStream inStream, byte[] buf, int begPos )
							throws IOException
  {
    int nRead  = 0;
    int nTotal = begPos;
    while( (nRead >= 0) && (nTotal < buf.length) ) {
      nRead = inStream.read( buf, nTotal, buf.length - nTotal );
      if( nRead > 0 )
	nTotal += nRead;
    }
    return nTotal;
  }


  public static ExtFile readExtFont( Window owner, Properties props )
  {
    ExtFile extFont = null;
    if( props != null ) {
      String  fileName = props.getProperty( "jkcemu.font.file.name" );
      if( fileName != null ) {
	if( fileName.length() > 0 ) {
	  try {
	    extFont = new ExtFile( new File( fileName ) );
	  }
	  catch( IOException ex ) {
	    String msg = ex.getMessage();
	    BasicDlg.showErrorDlg(
		owner,
		String.format(
			"Zeichensatz kann nicht geladen werden\n%s",
			msg != null ? msg : "" ) );
	  }
	}
      }
    }
    return extFont;
  }


  public static ExtROM[] readExtROMs( Window owner, Properties props )
  {
    ExtROM[] extROMs = null;
    int      n       = EmuUtil.getIntProperty( props, "jkcemu.rom.count", 0 );
    if( n > 0 ) {
      java.util.List<ExtROM> romList = new ArrayList<ExtROM>( n );
      for( int i = 0; i < n; i++ ) {
	String addrText = props.getProperty(
			String.format( "jkcemu.rom.%d.address", i + 1 ) );
	String fileName = props.getProperty(
			String.format( "jkcemu.rom.%d.file", i + 1 ) );
	if( (addrText != null) && (fileName != null) ) {
	  if( (addrText.length() > 0) && (fileName.length() > 0) ) {
	    try {
	      int addr = Integer.parseInt( addrText, 16 );
	      if( (addr >= 0) && (addr <= 0xFFFF) ) {
		try {
		  ExtROM extROM = new ExtROM( new File( fileName ) );
		  extROM.setBegAddress( addr );
		  romList.add( extROM );
		}
		catch( IOException ex ) {
		  String msg = ex.getMessage();
		  BasicDlg.showErrorDlg(
			owner,
			String.format(
				"ROM an Adresse %04X kann nicht"
					+ " geladen werden\n%s",
				addr,
				msg != null ? msg : "" ) );
		}
	      }
	    }
	    catch( Exception ex ) {};
	  }
	}
      }
      n = romList.size();
      if( n > 0 ) {
	try {
	  extROMs = romList.toArray( new ExtROM[ n ] );
	  if( extROMs != null ) {
	    n = extROMs.length;
	  }
	}
	catch( ArrayStoreException ex ) {}
      }
    }
    return extROMs;
  }


  public static byte[] readFile( File file, int maxLen ) throws IOException
  {
    byte[] rv = null;
    if( file != null ) {
      long        len = file.length();
      InputStream in  = null;
      try {
	in = new FileInputStream( file );
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
	  int n = read( in, rv, 0 );
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


  public static File renameFile( Component owner, File file )
  {
    File rvFile = null;
    if( file != null ) {
      boolean isDir    = file.isDirectory();
      String  fileName = ReplyTextDlg.showReplyTextDlg(
				owner,
				isDir ? "Verzeichnisname:" : "Dateiname:",
				isDir ? "Verzeichnis umbenennen"
						: "Datei umbenennen",
				file.getName() );
      if( fileName != null ) {
	fileName = fileName.trim();
	if( fileName.length() > 0 ) {
	  File replyFile = new File( fileName );
	  if( replyFile.isAbsolute() || (replyFile.getParent() != null) ) {
	    BasicDlg.showErrorDlg(
		owner,
		"Mit dieser Funktion k\u00F6nnen Dateien und Verzeichnisse\n"
			+ "nur umbenannt, nicht aber verschoben werden." );
	  } else {
	    File parent = file.getParentFile();
	    if( parent != null )
	      replyFile = new File( parent, fileName );

	    if( file.renameTo( replyFile ) ) {
	      rvFile = replyFile;
	    } else {
	      BasicDlg.showErrorDlg(
			owner,
			(isDir ? "Verzeichnis" : "Datei")
				+ " konnte nicht umbenannt werden." );
	    }
	  }
	}
      }
    }
    return rvFile;
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


  public static File showNativeFileDlg(
				Frame   owner,
				boolean forSave,
				String  title,
				File    preSelection )
  {
    File       file = null;
    FileDialog dlg  = new FileDialog(
				owner,
				title,
				forSave ? FileDialog.SAVE : FileDialog.LOAD );
    dlg.setModalityType( Dialog.ModalityType.DOCUMENT_MODAL );
    dlg.setResizable( true );
    if( preSelection != null ) {
      if( preSelection.isDirectory() ) {
	dlg.setDirectory( preSelection.getPath() );
      } else {
	String dirName = preSelection.getParent();
	if( dirName != null ) {
	  dlg.setDirectory( dirName );
	}
	if( !preSelection.exists() || preSelection.isFile() )
	  dlg.setFile( preSelection.getName() );
      }
    }
    BasicDlg.setParentCentered( dlg );
    dlg.setVisible( true );
    String fileName = dlg.getFile();
    if( fileName != null ) {
      String dirName = dlg.getDirectory();
      if( dirName != null )
	file = new File( new File( dirName ), fileName );
      else
        file = new File( fileName );
    }
    return file;
  }


  public static File showFileOpenDlg(
			Frame                                 owner,
			String                                title,
			File                                  preSelection,
			javax.swing.filechooser.FileFilter... fileFilters )
  {
    if( isNativeFileDialogSelected() ) {
      return showNativeFileDlg(
			owner,
			false,
			title,
			preSelection );
    }
    FileSelectDlg dlg = new FileSelectDlg(
				owner,
				false,		// fuer Laden
				false,		// kein Startknopf
				false,		// kein "Laden mit..."-Knopf
				title,
				preSelection,
				fileFilters );
    dlg.setVisible( true );
    return dlg.getSelectedFile();
  }


  public static File showFileSaveDlg(
			Frame                                 owner,
			String                                title,
			File                                  preSelection,
			javax.swing.filechooser.FileFilter... fileFilters )
  {
    if( isNativeFileDialogSelected() ) {
      return showNativeFileDlg(
			owner,
			true,
			title,
			preSelection );
    }
    FileSelectDlg dlg = new FileSelectDlg(
				owner,
				true,		// fuer Speichern
				false,		// kein Startknopf
				false,		// kein "Laden mit..."-Knopf
				title,
				preSelection,
				fileFilters );
    dlg.setVisible( true );
    return dlg.getSelectedFile();
  }


  public static void showSysError(
				Component parent,
				String    msg,
				Exception ex )
  {
    File errFile = null;

    // Fehlerprotokoll in Datei schreiben
    PrintWriter errWriter = null;
    try {
      String fName = "jkcemu_err.log";
      String fDir  = System.getProperty( "user.home" );
      if( fDir == null ) {
	fDir = "";
      }
      errFile = (fDir.length() > 0 ?  new File( fDir, fName )
					      : new File( fName ) );
      errWriter = new PrintWriter( new FileWriter( errFile ) );

      errWriter.println( "Bitte senden Sie diese Datei an"
						+ " info@jens-mueller.org" );
      errWriter.println( "Please send this file to info@jens-mueller.org" );
      errWriter.println();
      errWriter.println( "--- Program ---" );
      errWriter.write( Main.getVersion() );
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

      if( errWriter.checkError() )
	errFile = null;

      errWriter.close();
      errWriter = null;
    }
    catch( Exception ex2 ) {
      errFile = null;
    }
    finally {
      if( errWriter != null )
	errWriter.close();
    }

    // Fehlerausschrift
    if( parent == null ) {
      parent = new Frame();
    }
    if( errFile != null ) {
      BasicDlg.showErrorDlg(
	parent,
	"Es ist ein interner Applikationsfehler aufgetreten.\n"
	  + "Ein Protokoll des Fehlers wurde in die Textdatei\n\'"
	  + errFile.getPath() + "\' geschrieben.\n"
	  + "Bitte senden Sie diese Textdatei einschlie\u00DFlich einer\n"
	  + "kurzen Beschreibung Ihrer letzten Aktionen per E-Mail an:\n"
	  + "info@jens-mueller.org\n\n"
	  + "Schreiben Sie bitte in den Betreff der E-Mail unbedingt\n"
	  + "das Wort JKCEMU hinein!\n\n"
	  + "Vielen Dank!",
	"Applikationsfehler" );

    } else {

      BasicDlg.showErrorDlg(
	parent,
	"Es ist ein interner Applikationsfehler aufgetreten.\n"
	  + "Bitte melden Sie diesen Fehler einschlie\u00DFlich einer\n"
	  + "kurzen Beschreibung Ihrer letzten Aktionen per E-Mail an:\n"
	  + "info@jens-mueller.org\n\n"
	  + "Schreiben Sie bitte in den Betreff der E-Mail unbedingt\n"
	  + "das Wort JKCEMU hinein!\n\n"
	  + "Vielen Dank!",
	"Applikationsfehler" );
    }
  }


  public static char toISO646DE( char ch )
  {
    switch( ch ) {
      case '\u00A7':  // Paragraf-Zeichen
	ch = '@';
	break;

      case '\u00C4':  // Ae
	ch = '[';
	break;

      case '\u00D6':  // Oe
	ch = '\\';
	break;

      case '\u00DC':  // Ue
	ch = ']';
	break;

      case '\u00E4':  // ae
	ch = '{';
	break;

      case '\u00F6':  // oe
	ch = '|';
	break;

      case '\u00FC':  // ue
	ch = '}';
	break;

      case '\u00DF':  // sz
	ch = '~';
	break;
    }
    return ch;
  }


	/* --- private Methoden --- */

  private static boolean deleteFile(
				Component owner,
				File      file,
				RunStatus runStatus )
  {
    boolean rv = true;
    if( file != null ) {
      if( file.exists() ) {
	boolean failed = false;
	boolean isDir  = file.isDirectory();
	if( isDir ) {
	  File[] files = file.listFiles();
	  if( files != null ) {
	    for(
		int i = 0;
		(i < files.length) && !runStatus.wasCancelled();
		i++ )
	    {
	      if( !deleteFile( owner, files[ i ], runStatus ) )
		failed = true;
	    }
	  }
	}
	rv = false;
	if( !failed && !runStatus.wasCancelled() ) {
	  boolean deleteEnabled = true;
	  if( !runStatus.getForce() && !file.canWrite() ) {
	    String[] options = {
				"\u00DCberspringen",
				"L\u00F6schen",
				"Alle l\u00F6schen",
				"Abbrechen" };

	    StringBuilder buf = new StringBuilder( 512 );
	    buf.append( "Sie besitzen keine Schreibrechte f\u00FCr " );
	    if( isDir ) {
	      buf.append( "die Datei" );
	    } else {
	      buf.append( "das Verzeichnis" );
	    }
	    buf.append( "\n\'" );
	    buf.append( file.getPath() );
	    buf.append( "\'.\nM\u00F6chten Sie trotzem"
			+ " das L\u00F6schen versuchen?" );

	    JOptionPane pane = new JOptionPane(
					buf.toString(),
					JOptionPane.ERROR_MESSAGE );
	    pane.setOptions( options );
	    pane.setWantsInput( false );
	    pane.createDialog( owner, "Fehler" ).setVisible( true );
	    Object value = pane.getValue();
	    if( value != null ) {
	      if( value.equals( options[ 0 ] ) ) {
		deleteEnabled = false;
	      }
	      else if( value.equals( options[ 2 ] ) ) {
		runStatus.setForce( true );
	      }
	      else if( value.equals( options[ 3 ] ) ) {
		runStatus.setCancelled( true );
	      }
	    } else {
	      runStatus.setCancelled( true );
	    }
	  }
	  if( deleteEnabled && !runStatus.wasCancelled() ) {
	    rv = file.delete();
	    if( !rv ) {
	      if( runStatus.isQuiet() ) {
		runStatus.setFailed( true );
	      } else {
		String[] options = {
				"Ignorieren",
				"Alle ignorieren",
				"Abbrechen" };

		JOptionPane pane = new JOptionPane();
		if( isDir ) {
		  pane.setMessage( "Verzeichnis \'"
			+ file.getPath()
			+ "\'\nkonnte nicht gel\u00F6scht werden." );
		} else {
		  pane.setMessage( "Datei \'"
			+ file.getPath()
			+ "\'\nkonnte nicht gel\u00F6scht werden." );
		}
		pane.setMessageType( JOptionPane.ERROR_MESSAGE );
		pane.setOptions( options );
		pane.setWantsInput( false );
		pane.createDialog( owner, "Fehler" ).setVisible( true );
		Object value = pane.getValue();
		if( value != null ) {
		  if( value.equals( options[ 1 ] ) ) {
		    runStatus.setQuiet( true );
		  }
		  else if( value.equals( options[ 2 ] ) ) {
		    runStatus.setCancelled( true );
		  }
		} else {
		  runStatus.setCancelled( true );
		}
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private static javax.swing.filechooser.FileFilter getFileFilter(
							String    text,
							String... formats )
  {
    javax.swing.filechooser.FileFilter rv = null;
    if( formats != null ) {
      if( formats.length > 0 ) {
	if( fmt2FileFilter == null ) {
	  fmt2FileFilter = new Hashtable<
				String,
				javax.swing.filechooser.FileFilter>();
	}
	rv = fmt2FileFilter.get( formats[ 0 ] );
	if( rv == null ) {
	  rv = new javax.swing.filechooser.FileNameExtensionFilter(
								text,
								formats );
	  fmt2FileFilter.put( formats[ 0 ], rv ); }
      }
    }
    return rv;
  }


  private static boolean isEqualIgnoreCase( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return s1.equalsIgnoreCase( s2 != null ? s2 : "" );
  }
}

