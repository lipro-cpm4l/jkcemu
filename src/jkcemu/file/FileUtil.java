/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Allgemeine Hilfsfunktionen
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.base.ErrorMsg;
import jkcemu.base.GUIFactory;
import jkcemu.base.ObjectByStringComparator;
import jkcemu.base.ReplyDirDlg;
import jkcemu.base.ReplyTextDlg;
import jkcemu.base.UserInputException;
import jkcemu.text.TextUtil;


public class FileUtil
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

  public static final String LABEL_SEARCH_IN = "Suchen in:";

  public static final String PROP_FILEDIALOG         = "jkcemu.filedialog";
  public static final String VALUE_FILEDIALOG_NATIVE = "native";
  public static final String VALUE_FILEDIALOG_JKCEMU = "jkcemu";
  public static final String VALUE_FILEDIALOG_SWING  = "swing";

  /*
   * Wenn im Dateisystem Netzwerklaufwerke oder Netzwerkpfade
   * eingehaengt sind, die nicht erreichbar sind, kann das dazu fuehren,
   * das Aufrufe von Methoden an der Klasse FileSystemView
   * solange blockieren, bis die Netzwerk-Shares wieder erreichbar sind.
   * Aus diesem Grund wird die Klasse FileSystemView nur hier verwendet
   * und bei Methodenaufrufen an dieser Klasse vorher das Argument
   * auf Erreichbarkeit geprueft.
   */
  private static final FileSystemView fsv
				= FileSystemView.getFileSystemView();

  private static Map<String,FileFilter> fmt2FileFilter = null;

  private static Icon    unreachableIcon     = null;
  private static boolean unreachableIconDone = false;


  public static boolean accept( File file, String... suffixes )
  {
    boolean rv = false;
    if( (file != null) && (suffixes != null) ) {
      String fName = file.getName();
      if( fName != null ) {
	fName = fName.toLowerCase();
	for( String s : suffixes ) {
	  if( s != null ) {
	    if( !s.isEmpty() ) {
	      if( !s.startsWith( "." ) ) {
		s = "." + s;
	      }
	      if( fName.endsWith( s ) ) {
		rv = true;
		break;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  public static void addHeadersaveFileTypeItemsTo( JComboBox<String> combo )
  {
    for( String s : headersaveFileTypeItems ) {
      combo.addItem( s );
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
	      if( !BaseDlg.showYesNoDlg( owner, buf.toString() ) ) {
		dirFile = null;
	      }
	    } else {
	      BaseDlg.showErrorDlg(
			owner,
			dirFile.getPath() + " existiert bereits\n"
				+ "und kann nicht als Verzeichnis"
				+ " angelegt werden." );
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


  public static boolean checkFileDesc(
				Component owner,
				String    desc,
				int       maxLen ) throws UserInputException
  {
    boolean rv = true;
    if( desc == null ) {
      desc = "";
    }
    int curLen = desc.length();
    if( curLen > maxLen ) {
      if( maxLen > 0 ) {
	throw new UserInputException(
		String.format(
			"Die in den Kopfdaten angegebene Bezeichnung\n"
				+ "ist zu lang (max. %d Zeichen).",
			maxLen ) );
      } else {
	throw new UserInputException(
		"Das Dateiformat unterst\u00FCtzt die Angabe\n"
			+ " einer Bezeichnung in den Kopfdaten nicht." );
      }
    } else if( (curLen < 1) && (maxLen > 0) ) {
      rv = BaseDlg.showYesNoWarningDlg(
		owner,
		"Sie haben in den Kopfdaten keine Bezeichnung angegeben.\n"
			+ "Das kann beim Einlesen der Datei"
			+ " zu Problemen f\u00FChren."
			+ "\n\nM\u00F6chten Sie trotzdem fortsetzen?",
		"Warnung" );
    }
    return rv;
  }


  public static boolean checkTreeNodeUsable( JTree tree, Object node )
  {
    boolean rv = false;
    if( node != null ) {
      rv = true;
      if( node instanceof FileNode ) {
	FileNode fileNode = (FileNode) node;
	if( !isUsable( fileNode.getFile() ) ) {
	  TreeModel tm = tree.getModel();
	  if( tm != null ) {
	    if( tm instanceof DefaultTreeModel ) {
	      DefaultTreeModel model = (DefaultTreeModel) tm;
	      model.nodeStructureChanged( fileNode );
	      TreeNode[] path = model.getPathToRoot( fileNode );
	      if( path != null ) {
		fireCollapseTreePath( tree, new TreePath( path ) );
	      }
	    }
	  }
	  rv = false;
	}
      }
    }
    return rv;
  }


  public static void checkTreeWillExpand( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    if( e != null ) {
      final TreePath tp = e.getPath();
      if( tp != null ) {
	Object o = tp.getLastPathComponent();
	if( o != null ) {
	  if( o instanceof FileNode ) {
	    FileNode node = (FileNode) o;
	    if( !FileUtil.isUsable( node.getFile() ) ) {
	      Object src = e.getSource();
	      if( src != null ) {
		if( src instanceof JTree ) {
		  JTree     tree = (JTree) src;
		  TreeModel tm   = tree.getModel();
		  if( tm != null ) {
		    if( tm instanceof DefaultTreeModel ) {
		      ((DefaultTreeModel) tm).nodeStructureChanged( node );
		    }
		  }
		  fireCollapseTreePath( tree, tp );
		}
	      }
	      throw new ExpandVetoException( e );
	    }
	  }
	}
      }
    }
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
              buf.append( '.' );
              break;
            default:
              if( specialChars.indexOf( ch ) >= 0 ) {
                buf.append( '\\' );
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


  public static File completeFileExtension( File file, FileFilter filter )
  {
    if( (file != null) && (filter != null) ) {
      if( filter instanceof FileNameExtensionFilter ) {
	String[] exts = ((FileNameExtensionFilter) filter).getExtensions();
	String  fName = file.getName();
	if( (exts != null) && (fName != null) ) {
	  int dotPos = fName.indexOf( '.' );
	  if( (exts.length == 1)
	      && ((dotPos < 0) || (dotPos == (fName.length() - 1))) )
	  {
	    if( dotPos < 0 ) {
	      fName += ".";
	    }
	    fName += exts[ 0 ];
	    File parent = file.getParentFile();
	    if( parent != null ) {
	      file = new File( parent, fName );
	    } else {
	      file = new File( fName );
	    }
	  }
	}
      }
    }
    return file;
  }


  public static boolean confirmFileOverwrite( Component owner, File file )
  {
    boolean rv = false;
    if( file != null ) {
      if( file.exists() ) {
	rv = BaseDlg.showYesNoWarningDlg(
		owner,
		"Die Datei \'" + file.getName() + "\' existiert bereits.\n"
			+ "M\u00F6chten Sie die Datei \u00FCberschreiben?",
		"Best\u00E4tigung" );
      } else {
	rv = true;
      }
    }
    return rv;
  }


  public static File createDir( Component owner, File parent )
  {
    File rvFile = null;
    if( parent != null ) {
      String dirName = ReplyTextDlg.showDlg(
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
	    BaseDlg.showErrorDlg(
			owner,
			"Verzeichnis konnte nicht erstellt werden." );
	  }
	}
      }
    }
    return rvFile;
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
	EmuUtil.closeSilently( out );
	throw ex;
      }
    }
    return out;
  }


  public static Set<Path> createPathSet()
  {
    return new TreeSet<>( createPathComparator() );
  }


  public static boolean equals( File f1, File f2 )
  {
    boolean rv = false;
    if( (f1 != null) && (f2 != null) ) {
      rv = f1.equals( f2 );
    } else {
      if( (f1 == null) && (f2 == null) ) {
	rv = true;
      }
    }
    return rv;
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
	long value = EmuUtil.getHex4( fileName, pos );
	if( (pos + 4) < len ) {
	  if( EmuUtil.isHexChar( fileName.charAt( pos + 4 ) ) ) {
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


  public static File fileDrop( final Component owner, DropTargetDropEvent e )
  {
    File    file          = null;
    boolean multiFileInfo = false;
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
		      multiFileInfo = true;
		      file          = null;
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
    if( multiFileInfo ) {
      ErrorMsg.showLater(
		owner,
		"Bitte nur eine Datei hier hineinziehen!",
		null );
    }
    return file;
  }


  public static void fireCollapseTreePath(
				final JTree tree,
				final TreePath treePath )
  {
    if( (tree != null) && (treePath != null) ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    tree.collapsePath( treePath );
		  }
		} );
    }
  }


  public static File getDirectory( File file )
  {
    if( file != null ) {
      if( !file.isDirectory() ) {
	file = file.getParentFile();
      }
    }
    return file;
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


  public static File getHomeDirFile()
  {
    File   dirFile = null;
    String homeDir = System.getProperty( "user.home" );
    if( homeDir != null ) {
      if( !homeDir.isEmpty() ) {
	dirFile = new File( homeDir );
      }
    }
    return dirFile;
  }


  public static String getLowerFileSuffix( Path path )
  {
    String suffix = null;
    if( path != null ) {
      Path namePath = path.getFileName();
      if( namePath != null ) {
	String fileName = namePath.toString();
	if( fileName != null ) {
	  int pos = fileName.lastIndexOf( '.' );
	  if( (pos > 0) && ((pos + 1) < fileName.length()) ) {
	    suffix = fileName.substring( pos + 1 ).toLowerCase();
	  }
	}
      }
    }
    return suffix;
  }


  public static File getParent( File file )
  {
    File rv = null;
    if( isUsable( file ) ) {
      if( fsv != null ) {
	try {
	  rv = fsv.getParentDirectory( file );
	}
	catch( InvalidPathException ex ) {}
      } else {
	rv = file.getParentFile();
      }
    }
    return rv;
  }


  public static FileFilter getAC1Basic6FileFilter()
  {
    return getFileFilter( "AC1-BASIC6-Dateien (*.abc)", "abc" );
  }


  public static FileFilter getAnaDiskFileFilter()
  {
    return getFileFilter( "AnaDisk-Dateien (*.dump)", "dump" );
  }


  public static FileFilter getBasicFileFilter()
  {
    return getFileFilter( "BASIC-Dateien (*.bas)", "bas" );
  }


  public static FileFilter getBasicOrRBasicFileFilter()
  {
    return getFileFilter( "BASIC-/RBASIC-Dateien (*.bas)", "bas" );
  }


  public static FileFilter getBinaryFileFilter()
  {
    return getFileFilter( "Einfache Speicherabbilddateien (*.bin)", "bin" );
  }


  public static FileFilter getCdtFileFilter()
  {
    return getFileFilter( "CPC-Tape-Dateien (*.cdt)", "cdt" );
  }


  public static FileFilter getCommandFileFilter()
  {
    return getFileFilter( "CP/M-kompatible Programmdateien (*.com)", "com" );
  }


  public static FileFilter getCswFileFilter()
  {
    return getFileFilter( "CSW-Dateien (*.csw)", "csw" );
  }


  public static FileFilter getComFileFilter()
  {
    return getFileFilter( "CP/M-Programmdateien (*.com)", "com" );
  }


  public static FileFilter getCopyQMFileFilter()
  {
    return getFileFilter( "CopyQM-Dateien (*.cqm; *.qm)", "cqm", "qm" );
  }


  public static FileFilter getDskFileFilter()
  {
    return getFileFilter( "CPC-Disk-Dateien (*.dsk)", "dsk" );
  }


  public static FileFilter getGIFFileFilter()
  {
    return getFileFilter( "GIF-Dateien (*.gif)", "gif" );
  }


  public static FileFilter getHeadersaveFileFilter()
  {
    return getFileFilter( "Headersave-Dateien (*.z80)", "z80" );
  }


  public static FileFilter getHexFileFilter()
  {
    return getFileFilter( "HEX-Dateien (*.hex)", "hex" );
  }


  public static FileFilter getImageDiskFileFilter()
  {
    return getFileFilter( "ImageDisk-Dateien (*.imd)", "imd" );
  }


  public static FileFilter getISOFileFilter()
  {
    return getFileFilter( "CD-/DVD-Abbilddateien (*.iso)", "iso" );
  }


  public static FileFilter getKCBasicFileFilter()
  {
    return getFileFilter( "KC-BASIC-Dateien (*.sss)", "sss" );
  }


  public static FileFilter getKCBasicSystemFileFilter()
  {
    return getFileFilter( "KC-BASIC-Systemdateien (*.kcb)", "kcb" );
  }


  public static FileFilter getKCSystemFileFilter()
  {
    return getFileFilter( "KC-Systemdateien (*.kcc)", "kcc" );
  }


  public static FileFilter getKCTapFileFilter()
  {
    return getFileFilter( "KC-TAP-Dateien (*.tap)", "tap" );
  }


  public static FileFilter getPlainDiskFileFilter()
  {
    return getFileFilter(
		"Einfache Abbilddateien (*.dd; *.img; *.image, *.raw)",
		"dd", "img", "image", "raw" );
  }


  public static FileFilter getProjectFileFilter()
  {
    return getFileFilter( "Projekdateien (*.prj)", "prj" );
  }


  public static FileFilter getRMCFileFilter()
  {
    return getFileFilter( "RBASIC-Maschinencodedateien (*.rmc)", "rmc" );
  }


  public static FileFilter getROMFileFilter()
  {
    return getFileFilter( "ROM-Dateien (*.bin; *.rom)", "rom", "bin" );
  }


  public static FileFilter getTapeFileFilter()
  {
    return getFileFilter(
			"Tape-Dateien (*.cdt; *.csw; *.tap; *.tzx)",
			"cdt", "csw", "tap", "tzx" );
  }


  public static FileFilter getTeleDiskFileFilter()
  {
    return getFileFilter( "TeleDisk-Dateien (*.td0)", "td0" );
  }


  public static FileFilter getTextFileFilter()
  {
    return getFileFilter(
			"Textdateien (*.asc; *.log; *.txt)",
			"asc", "log", "txt" );
  }


  public static FileFilter getTzxFileFilter()
  {
    return getFileFilter( "ZX-Tape-Dateien (*.tzx)", "tzx" );
  }


  public static FileFilter getXMLFileFilter()
  {
    return getFileFilter( "XML-Dateien (*.xml)", "xml" );
  }


  /*
   * Ermitteln des Anzeigenamens und des Icons
   * einer Datei oder eines Verzeichnis
   * Gibt es unter Windows nicht erreichbare Netzwerklaufwerke,
   * dann bleibt schon beim ersten Laufwerk,
   * auch wenn es kein Netzwerklaufwerk ist (z.B. C:\),
   * das Lesen des Icons und des Systemnames haengen.
   */
  public static String getSystemDisplayName( File file )
  {
    String text = null;
    if( file != null ) {
      if( isUsable( file ) ) {
	if( fsv != null ) {
	  if( (file.getParent() != null)
	      || !DeviceIO.hasUnreachableNetPaths() )
	  {
	    try {
	      text = TextUtil.emptyToNull(
				fsv.getSystemDisplayName( file ) );
	    }
	    catch( InvalidPathException ex ) {}
	  }
	}
      }
      if( text == null ) {
	text = TextUtil.emptyToNull( file.getName() );
      }
      if( text == null ) {
	text = file.getPath();
      }
    }
    return text;
  }


  public static Icon getSystemIcon( File file )
  {
    Icon icon = null;
    if( isUsable( file ) ) {
      if( fsv != null ) {
	if( (file.getParent() != null)
	    || !DeviceIO.hasUnreachableNetPaths() )
	{
	  try {
	    icon = fsv.getSystemIcon( file );
	  }
	  catch( InvalidPathException ex ) {}
	}
      }
    } else {
      icon = getUnreachablePathIcon();
    }
    return icon;
  }


  public static Icon getUnreachablePathIcon()
  {
    if( !unreachableIconDone ) {
      unreachableIconDone = true;
      Image image         = Main.getLoadedImage(
				null,
				"/images/etc/unreachable.png" );
      if( image != null ) {
	unreachableIcon = new ImageIcon( image );
      }
    }
    return unreachableIcon;
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


  public static boolean isJKCEMUFileDialogSelected()
  {
    String s = Main.getProperty( PROP_FILEDIALOG );
    return !TextUtil.equalsIgnoreCase( s, VALUE_FILEDIALOG_NATIVE )
		&& !TextUtil.equalsIgnoreCase( s, VALUE_FILEDIALOG_SWING );
  }


  public static boolean isUsable( File file )
  {
    boolean rv = false;
    if( file != null ) {
      rv = true;
      String             path  = file.getPath();
      Collection<String> paths = DeviceIO.getUnreachableNetPaths();
      if( paths != null ) {
	for( String p : paths ) {
	  if( path.startsWith( p ) ) {
	    rv = false;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public static File[] listFiles( File dirFile )
  {
    File[] files = null;
    if( isUsable( dirFile ) ) {
      if( fsv != null ) {
	try {
	  files = fsv.getFiles( dirFile, true );
	}
	catch( InvalidPathException ex ) {}
      }
      if( files == null ) {
	files = dirFile.listFiles();
      }
    }
    return files;
  }


  public static File[] listRoots()
  {
    return fsv != null ?
		fsv.getRoots()
		: DeviceIO.listRoots();
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
   * Die Methode oeffnet eine Datei und
   * gibt einen BufferedInputStream zurueck.
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
	EmuUtil.closeSilently( in );
	in = null;
      }
    }
    if( in == null ) {
      in = new FileInputStream( file );
    }
    return new BufferedInputStream( in );
  }


  public static byte[] readFile(
			File    file,
			boolean allowUncompress ) throws IOException
  {
    return readFile( file, allowUncompress, -1 );
  }


  public static byte[] readFile(
			File    file,
			boolean allowUncompress,
			int     maxLen ) throws IOException
  {
    byte[] rv = null;
    if( file != null ) {
      long        len = -1;
      InputStream in  = null;
      try {
	if( allowUncompress && isGZipFile( file ) ) {
	  in = new GZIPInputStream( new FileInputStream( file ) );
	} else {
	  len = file.length();
	  in  = new FileInputStream( file );
	}
	if( len > 0 ) {
	  if( maxLen < 1 ) {
	    maxLen = Integer.MAX_VALUE;
	  }
	  rv = new byte[ (int) Math.min( len, maxLen ) ];
	  if( len > 0 ) {
	    int n = EmuUtil.read( in, rv );
	    if( (n > 0) && (n < rv.length) ) {
	      rv = Arrays.copyOf( rv, n );
	    }
	  }
	} else {
	  ByteArrayOutputStream buf = new ByteArrayOutputStream( 0x10000 );
	  int b = in.read();
	  while( (b != -1) && (maxLen != 0) ) {
	    buf.write( b );
	    b = in.read();
	    --maxLen;
	  }
	  rv = buf.toByteArray();
	}
      }
      finally {
	EmuUtil.closeSilently( in );
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
	  rv = readFile( new File( fileName ), allowUncompress, maxLen );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  BaseDlg.showErrorDlg(
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


  public static URL readInternetShortcutURL( File file ) throws IOException
  {
    URL    url   = null;
    String fName = file.getName();
    if( fName != null ) {
      if( fName.toLowerCase().endsWith( ".url" ) ) {
	BufferedReader in  = null;
	try {
	  in             = new BufferedReader( new FileReader( file ) );
	  boolean header = false;
	  String  line   = in.readLine();
	  while( line != null ) {
	    if( line.trim().equals( "[InternetShortcut]" ) ) {
	      header = true;
	    } else {
	      if( header && (line.length() > 4)
		  && line.startsWith( "URL=" ) )
	      {
		url = new URL( line.substring( 4 ) );
		break;
	      }
	    }
	    line = in.readLine();
	  }
	}
	finally {
	  EmuUtil.closeSilently( in );
	}
      }
    }
    return url;
  }


  public static void releaseSilent( FileLock fileLock )
  {
    if( fileLock != null ) {
      try {
	fileLock.release();
      }
      catch( IOException ex ) {}
    }
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
	BaseDlg.showErrorDlg(
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
      String newName = ReplyTextDlg.showDlg(
				owner,
				"Neuer Name:",
				title,
				oldName != null ? oldName : "" );
      if( newName != null ) {
	try {
	  newPath = Files.move( path, path.resolveSibling( newName ) );
	}
	catch( Exception ex ) {
	  BaseDlg.showErrorDlg( owner, ex );
	}
      }
    }
    return newPath;
  }


  public static File replaceExtension( File srcFile, String ext )
  {
    File outFile = null;
    if( (srcFile != null) && (ext != null) ) {
      String fName = srcFile.getName();
      if( fName != null ) {
	int pos = fName.lastIndexOf( '.' );
	if( pos >= 0 ) {
	  if( ext.startsWith( "." ) ) {
	    fName = fName.substring( 0, pos ) + ext;
	  } else {
	    fName = fName.substring( 0, pos + 1 ) + ext;
	  }
	  File dirFile = srcFile.getParentFile();
	  if( dirFile != null ) {
	    outFile = new File( dirFile, fName );
	  } else {
	    outFile = new File( fName );
	  }
	}
      }
    }
    return outFile;
  }


  public static void setFileWritable( File file, boolean state )
  {
    if( file != null ) {
      if( file.setWritable( state, false ) ) {
	file.setWritable( state, true );
      }
    }
  }


  public static boolean setSelectedHeadersaveFileTypeItem(
						JComboBox<?> combo,
						int          fileType )
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


  public static File showFileOpenDlg(
			Window        owner,
			String        title,
			File          preSelection,
			FileFilter... fileFilters )
  {
    File file = null;
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
      File[] files = null;
      if( isSwingFileDialogSelected() ) {
	files = showSwingFileDlg(
				owner,
				false,
				false,
				title,
				preSelection,
				fileFilters );
      } else {
	files = showNativeFileDlg(
				owner,
				false,
				false,
				title,
				preSelection );
      }
      if( files != null ) {
	if( files.length > 0 ) {
	  file = files[ 0 ];
	}
      }
    }
    return file;
  }


  public static File showFileSaveDlg(
			Window        owner,
			String        title,
			File          preSelection,
			FileFilter... fileFilters )
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
      File[] files = null;
      if( isSwingFileDialogSelected() ) {
	files = showSwingFileDlg(
				owner,
				true,
				false,
				title,
				preSelection,
				fileFilters );
      } else {
	files = showNativeFileDlg(
				owner,
				true,
				false,
				title,
				preSelection );
      }
      if( files != null ) {
	if( files.length > 0 ) {
	  file = files[ 0 ];
	}
      }
    }
    return file;
  }


  public static java.util.List<File> showMultiFileOpenDlg(
			Window        owner,
			String        title,
			File          preSelection,
			FileFilter... fileFilters )
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
      File[] tmpFiles = null;
      if( isSwingFileDialogSelected() ) {
	tmpFiles = showSwingFileDlg(
				owner,
				false,
				true,
				title,
				preSelection,
				fileFilters );
      } else {
	tmpFiles = showNativeFileDlg(
				owner,
				false,
				true,
				title,
				preSelection );
      }
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
	Arrays.sort( files, FileComparator.getInstance() );
      }
      catch( ClassCastException ex ) {}
    }
  }


  public static void throwUnsupportedFileFormat() throws IOException
  {
    throw new IOException( "Das Dateiformat wird nicht unterst\u00FCtzt\n"
			+ "oder die Datei ist besch\u00E4digt." );
  }


	/* --- private Methoden --- */

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


  private static FileFilter getFileFilter( String text, String... formats )
  {
    FileFilter rv = null;
    if( (formats != null) && (text != null) ) {
      if( formats.length > 0 ) {
	if( fmt2FileFilter == null ) {
	  fmt2FileFilter = new HashMap<>();
	}
	rv = fmt2FileFilter.get( text );
	if( rv == null ) {
	  rv = new FileNameExtensionFilter( text, formats );
	  fmt2FileFilter.put( text, rv );
	}
      }
    }
    return rv;
  }


  private static boolean isSwingFileDialogSelected()
  {
    return TextUtil.equalsIgnoreCase(
			Main.getProperty( PROP_FILEDIALOG ),
			VALUE_FILEDIALOG_SWING );
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
      BaseDlg.setParentCentered( dlg );
      dlg.setVisible( true );
      files = dlg.getFiles();
    }
    return files;
  }


  private static File[] showSwingFileDlg(
				Window        owner,
				boolean       forSave,
				boolean       multiMode,
				String        title,
				File          preSelection,
				FileFilter... fileFilters )
  {
    File[]       files       = null;
    JFileChooser fileChooser = new JFileChooser()
	{
	  @Override
	  protected JDialog createDialog( Component parent )
	  {
	    JDialog dlg = super.createDialog( parent );
	    dlg.setModalityType( Dialog.ModalityType.DOCUMENT_MODAL );
	    return dlg;
	  }
	};
    GUIFactory.initFont( fileChooser );
    fileChooser.setAcceptAllFileFilterUsed( true );
    fileChooser.setControlButtonsAreShown( true );
    if( preSelection != null ) {
      File dirFile = null;
      if( preSelection.isDirectory() ) {
	dirFile = preSelection;
      } else {
	dirFile = preSelection.getParentFile();
      }
      if( dirFile != null ) {
	if( forSave && !preSelection.equals( dirFile ) ) {
	  fileChooser.setSelectedFile( preSelection );
	} else {
	  fileChooser.setCurrentDirectory( dirFile );
	}
      }
    }
    fileChooser.setDialogTitle( title );
    /*
     * Bei Save CUSTOM_DIALOG nehmen,
     * damit JFileChooser selbst keine Warnung wegen Ueberschreiben
     * der Datei bringt.
     * Eine solche Warnung wird weiter unten ausgegeben,
     * nachdem evtl. eine Dateiendung angehaengt wurde.
     */
    fileChooser.setDialogType( forSave ?
				JFileChooser.CUSTOM_DIALOG
				: JFileChooser.OPEN_DIALOG );
    fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
    fileChooser.setMultiSelectionEnabled( multiMode );

    /*
     * Wenn es exakt einen Dateifilter gibt, dann wird diese aktiviert.
     * In allen anderen Faellen wird explizit der Accept-All-Filter
     * aktviert, damit nicht automatisch der erste Filter aktiv gesetzt wird,
     * was sonst in bestimmten Konstellationen passieren kann.
     */
    boolean filterDone = false;
    if( fileFilters != null ) {
      for( javax.swing.filechooser.FileFilter fileFilter : fileFilters ) {
	if( fileFilter != null ) {
	  fileChooser.addChoosableFileFilter( fileFilter );
	}
      }
      if( fileFilters.length == 1 ) {
	if( fileFilters[ 0 ] != null ) {
	  fileChooser.setFileFilter( fileFilters[ 0 ] );
	  filterDone = true;
	}
      }
    }
    if( !filterDone ) {
      FileFilter allFilter = fileChooser.getAcceptAllFileFilter();
      if( allFilter != null ) {
	fileChooser.setFileFilter( allFilter );
      }
    }

    // Dialog anzeigen
    if( fileChooser.showDialog(
			owner,
			forSave ? EmuUtil.TEXT_SAVE : EmuUtil.TEXT_OPEN )
		== JFileChooser.APPROVE_OPTION )
    {
      if( multiMode ) {
	files = fileChooser.getSelectedFiles();
      } else {
	File file = completeFileExtension(
				fileChooser.getSelectedFile(),
				fileChooser.getFileFilter() );
	if( file != null ) {
	  if( forSave ) {
	    if( !confirmFileOverwrite( owner, file ) ) {
	      file = null;
	    }
	  }
	}
	if( file != null ) {
	  files = new File[] { file };
	}
      }
    }
    return files;
  }


	/* --- Konstruktor --- */

  private FileUtil()
  {
    // Klasse nicht instanziierbar
  }
}
