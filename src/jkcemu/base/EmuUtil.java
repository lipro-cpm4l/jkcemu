/*
 * (c) 2008-2013 Jens Mueller
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
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import jkcemu.Main;
import jkcemu.text.TextUtil;


public class EmuUtil
{
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
	buf.append( " (" );
	buf.append( getIntegerFormat().format( size ) );
	buf.append( " Bytes" );
	buf.append( (char) ')' );
      }
    } else {
      buf.append( getIntegerFormat().format( size ) );
      buf.append( " Bytes" );
    }
  }


  public static boolean applyWindowSettings(
					Properties props,
					BasicFrm   frm,
					boolean    resizable )
  {
    boolean rv = false;
    if( (frm != null) && (props != null) ) {
      String prefix = frm.getSettingsPrefix();

      int x = parseInt(
			props.getProperty( prefix + ".window.x" ),
			-1,
			-1 );
      int y = parseInt(
			props.getProperty( prefix + ".window.y" ),
			-1,
			-1 );
      if( (x >= 0) && (y >= 0) ) {
	if( resizable ) {
	  int w = parseInt(
			props.getProperty( prefix + ".window.width" ),
			-1,
			-1 );
	  int h = parseInt(
			props.getProperty( prefix + ".window.height" ),
			-1,
			-1 );
	  if( frm.isVisible() ) {
	    frm.setSize( w, h );
	  } else {
	    frm.setBounds( x, y, w, h );
	  }
	  rv = true;
	} else {
	  if( !frm.isVisible() ) {
	    frm.setLocation( x, y );
	    rv = true;
	  }
	}
      }
    }
    return rv;
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


  public static boolean copyFile(
				File srcFile,
				File dstFile ) throws IOException
  {
    boolean      done    = false;
    boolean      created = false;
    long         millis  = srcFile.lastModified();
    InputStream  in      = null;
    OutputStream out     = null;
    try {
      in      = new BufferedInputStream( new FileInputStream( srcFile ) );
      out     = new BufferedOutputStream( new FileOutputStream( dstFile ) );
      created = true;

      int b = in.read();
      while( b >= 0 ) {
	out.write( b );
	b = in.read();
      }
      out.close();
      dstFile.setLastModified( millis );
      done = true;
    }
    finally {
      EmuUtil.doClose( in );
      EmuUtil.doClose( out );
    }
    if( created && !done ) {
      dstFile.delete();
    }
    return done;
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
				Window  window,
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
	if( file != null ) {
	  if( file.isDirectory() ) {
	    aDir = file;
	    nDirs++;
	  } else {
	    aFile = file;
	    nFiles++;
	  }
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


  public static void doClose( Closeable stream )
  {
    if( stream != null ) {
      try {
	stream.close();
      }
      catch( IOException ex ) {}
    }
  }


  public static void doClose( ServerSocket serverSocket )
  {
    if( serverSocket != null ) {
      try {
	serverSocket.close();
      }
      catch( IOException ex ) {}
    }
  }


  public static void doClose( Socket socket )
  {
    if( socket != null ) {
      try {
	socket.close();
      }
      catch( IOException ex ) {}
    }
  }


  public static void doClose( ZipFile zip )
  {
    if( zip != null ) {
      try {
	zip.close();
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
      String fDir  = System.getProperty( "user.home" );
      if( fDir == null ) {
	fDir = "";
      }
      errFile = (fDir.length() > 0 ?
				new File( fDir, fName )
				: new File( fName ) );
      errWriter = new PrintWriter( new FileWriter( errFile ) );

      errWriter.println( "Bitte senden Sie diese Datei an"
						+ " info@jens-mueller.org" );
      errWriter.println( "Please send this file to info@jens-mueller.org" );
      errWriter.println();
      errWriter.println( "--- Program ---" );
      errWriter.write( Main.VERSION );
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
	boolean hex   = true;
	long    value = 0;
	for( int i = 0; i < 4; i++ ) {
	  char ch = fileName.charAt( pos + i );
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
	if( (pos + 4) < len ) {
	  char ch = fileName.charAt( pos + 4 );
	  if( ((ch >= '0') && (ch <= '9'))
	      || ((ch >= 'A') && (ch <= 'Z'))
	      || ((ch >= 'a') && (ch <= 'z')) )
	  {
	    hex = false;
	  }
	}
	if( hex ) {
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
      e.acceptDrop( DnDConstants.ACTION_COPY );    // Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
	try {
	  Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof Collection ) {
	      Iterator iter = ((Collection) o).iterator();
	      if( iter != null ) {
		while( iter.hasNext() ) {
		  o = iter.next();
		  if( o != null ) {
		    File tmpFile = null;
		    if( o instanceof File ) {
		      String path = ((File) o).getPath();
		      if( path != null ) {
			if( !path.isEmpty() ) {
			  tmpFile = (File) o;
			}
		      }
		    }
		    else if( o instanceof String ) {
		      String s = (String) o;
		      if( !s.isEmpty() ) {
			tmpFile = new File( s );
		      }
		    }
		    if( tmpFile != null ) {
		      if( file == null ) {
			file = tmpFile;
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
	}
	catch( Exception ex ) {}
      }
      e.dropComplete( true );
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
	Frame f = (Frame) c;
	if( f.isVisible() ) {
	  int frameState = f.getExtendedState();
	  if( (frameState & Frame.ICONIFIED) != 0 ) {
	    f.setExtendedState( frameState & ~Frame.ICONIFIED );
	  }
	  f.toFront();
	} else {
	  f.setVisible( true );
	}
	break;
      }
      c = c.getParent();
    }
  }


  public static Frame getAncestorFrame( Component c )
  {
    Frame rv = null;
    while( c != null ) {
      if( c instanceof Frame ) {
	rv = (Frame) c;
	break;
      }
      c = c.getParent();
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


  public static javax.swing.filechooser.FileFilter getAnaDiskFileFilter()
  {
    return getFileFilter( "AnaDisk-Dateien (*.dump)", "dump" );
  }


  public static javax.swing.filechooser.FileFilter getBinaryFileFilter()
  {
    return getFileFilter( "Einfache Speicherabbilddateien (*.bin)", "bin" );
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


  public static javax.swing.filechooser.FileFilter getPlainDiskFileFilter()
  {
    return getFileFilter(
		"Einfache Diskettenabbilddateien (*.img; *.image, *.raw)",
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


  public static javax.swing.filechooser.FileFilter getRBasicFileFilter()
  {
    return getFileFilter( "RBASIC-Dateien (*.bas)", "bas" );
  }


  public static javax.swing.filechooser.FileFilter getRMCFileFilter()
  {
    return getFileFilter( "RBASIC-Maschinencodedateien (*.rmc)", "rmc" );
  }


  public static javax.swing.filechooser.FileFilter getROMFileFilter()
  {
    return getFileFilter( "ROM-Dateien (*.bin; *.rom)", "rom", "bin" );
  }


  public static javax.swing.filechooser.FileFilter getTapFileFilter()
  {
    return getFileFilter( "TAP-Dateien (*.tap)", "tap" );
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
    return TextUtil.equalsIgnoreCase(
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
    } else {
      System.out.print( text );
    }
  }


  public static void printlnErr()
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println();
    } else {
      System.err.println();
    }
  }


  public static void printlnOut()
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println();
    } else {
      System.out.println();
    }
  }


  public static void printlnErr( String text )
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println( text );
    } else {
      System.err.println( text );
    }
  }


  public static void printlnOut( String text )
  {
    if( Main.consoleWriter != null ) {
      Main.consoleWriter.println( text );
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
				int       maxLen,
				String    objName )
  {
    byte[] rv = null;
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	try {
	  rv = EmuUtil.readFile( new File( fileName ), maxLen );
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


  public static String readTextFile( File file ) throws IOException
  {
    String         rv = "";
    BufferedReader in = null;
    try {
      in = new BufferedReader( new FileReader( file ) );

      StringBuilder buf  = new StringBuilder( 0x8000 );
      String        line = in.readLine();
      while( line != null ) {
	buf.append( line );
	buf.append( (char) '\n' );
	line = in.readLine();
      }
      rv = buf.toString();
    }
    finally {
      doClose( in );
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
	if( !preSelection.exists() || preSelection.isFile() ) {
	  dlg.setFile( preSelection.getName() );
	}
      }
    }
    BasicDlg.setParentCentered( dlg );
    dlg.setVisible( true );
    String fileName = dlg.getFile();
    if( fileName != null ) {
      String dirName = dlg.getDirectory();
      if( dirName != null ) {
	file = new File( new File( dirName ), fileName );
      } else {
        file = new File( fileName );
      }
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
    File file = null;
    if( isNativeFileDialogSelected() ) {
      file = showNativeFileDlg(
			owner,
			true,
			title,
			preSelection );
    } else {
      FileSelectDlg dlg = new FileSelectDlg(
				owner,
				true,		// fuer Speichern
				false,		// kein Startknopf
				false,		// kein "Laden mit..."-Knopf
				title,
				preSelection,
				fileFilters );
      dlg.setVisible( true );
      file = dlg.getSelectedFile();
    }

    // Dateiname aufbereiten
    if( file != null ) {
      String fName = file.getName();
      if( fName != null ) {
	if( fName.startsWith( "\"" ) && fName.endsWith( "\"" ) ) {
	  if( fName.length() > 2 ) {
	    fName = fName.substring( 1, fName.length() - 1 );
	  } else {
	    fName = "";
	  }
	  file = replaceName( file, fName );
	} else {
	  // ggf. Dateiendung anhaengen
	  if( fileFilters != null ) {
	    if( fileFilters.length == 1 ) {
	      javax.swing.filechooser.FileFilter ff = fileFilters[ 0 ];
	      if( ff != null ) {
		if( ff instanceof FileNameExtensionFilter ) {
		  String[] extensions = ((FileNameExtensionFilter) ff)
							.getExtensions();
		  if( extensions != null ) {
		    if( extensions.length == 1 ) {
		      String ext = extensions[ 0 ];
		      if( ext != null ) {
			if( !fName.toUpperCase().endsWith(
						ext.toUpperCase() ) )
			{
			  file = replaceName(
					file,
					String.format( "%s.%s", fName, ext ) );
			}
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    return file;
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
			String       text ) throws IOException
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	out.write( text.charAt( i ) & 0x7F );
      }
    }
  }


  public static void writeFixLengthASCII(
			OutputStream out,
			String       text,
			int          len,
			int          filler ) throws IOException
  {
    if( text != null ) {
      int srcLen = text.length();
      for( int i = 0; i < len; i++ ) {
	int b = filler;
	if( i < srcLen ) {
	  b = text.charAt( i ) & 0x7F;
	}
	out.write( b );
      }
    }
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
	      /*
	       * Sollte durch einen symbolischen Link eine Ringkettung
	       * bestehen, wuerde der Stack ueberlaufen.
	       * Um das zu verhindern, wird versucht, * das Verzeichnis
	       * respektive dem ggf. verhandenen symbolischen Link
	       * direkt zu loeschen,
	       * was bei einem symbolischen Link auch moeglich ist.
	       * Damit wird eine evtl. vorhandene Ringkettung unterbrochen.
	       */
	      if( !files[ i ].delete() ) {
		// direktes Loeschen fehlgeschlagen -> rekursiv versuchen
		if( !deleteFile( owner, files[ i ], runStatus ) ) {
		  failed = true;
		}
	      }
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
	  fmt2FileFilter.put( formats[ 0 ], rv );
	}
      }
    }
    return rv;
  }


  private static File replaceName( File file, String fName )
  {
    if( file != null ) {
      File parent = file.getParentFile();
      if( parent != null ) {
	file = new File( parent, fName );
      } else {
	file = new File( fName );
      }
    }
    return file;
  }
}
