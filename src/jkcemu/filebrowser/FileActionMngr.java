/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Manager fuer Aktionen mit Dateien
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.audio.AudioPlayer;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.ac1_llc2.*;
import jkcemu.emusys.kc85.KCAudioDataStream;
import jkcemu.emusys.z1013.Z1013AudioDataStream;
import jkcemu.image.ImageFrm;
import jkcemu.text.TextEditFrm;
import jkcemu.tools.fileconverter.FileConvertFrm;
import jkcemu.tools.hexdiff.HexDiffFrm;
import jkcemu.tools.hexedit.HexEditFrm;


public class FileActionMngr
{
  public enum FileActionResult {
				NONE,
				DONE,
				FILE_RENAMED,
				FILES_CHANGED };

  public interface FileObject
  {
    public File            getFile();
    public Path            getPath();
    public FileCheckResult getCheckResult();
    public void            setPath( Path path );
  };

  public static final String ACTION_AUDIO_IN       = "audio.in";
  public static final String ACTION_CHECKSUM       = "checksum";
  public static final String ACTION_CONVERT        = "convert";
  public static final String ACTION_COPY           = "copy";
  public static final String ACTION_COPY_PATH      = "copy.path";
  public static final String ACTION_COPY_URL       = "copy.url";
  public static final String ACTION_DELETE         = "delete";
  public static final String ACTION_DISK_VIEW      = "disk.view";
  public static final String ACTION_EMU_LOAD_OPT   = "emu.load_opt";
  public static final String ACTION_EMU_LOAD       = "emu.load";
  public static final String ACTION_EMU_START      = "emu.start";
  public static final String ACTION_HEX_DIFF       = "hex.diff";
  public static final String ACTION_HEX_EDIT       = "hex.edit";
  public static final String ACTION_IMAGE_VIEW     = "image.view";
  public static final String ACTION_LAST_MODIFIED  = "last_modified";
  public static final String ACTION_PACK_GZIP      = "pack.gzip";
  public static final String ACTION_PACK_TAR       = "pack.tar";
  public static final String ACTION_PACK_TGZ       = "pack.tgz";
  public static final String ACTION_PACK_ZIP       = "pack.zip";
  public static final String ACTION_PLAY           = "play";
  public static final String ACTION_PLAY_AC1       = "play.ac1";
  public static final String ACTION_PLAY_AC1BASIC  = "play.ac1basic";
  public static final String ACTION_PLAY_KC85      = "play.kc85";
  public static final String ACTION_PLAY_SCCH      = "play.scch";
  public static final String ACTION_PLAY_Z1013     = "play.z1013";
  public static final String ACTION_PLAY_Z1013HS   = "play.z1013hs";
  public static final String ACTION_PLAY_Z9001     = "play.z9001";
  public static final String ACTION_PROPERTIES     = "properties";
  public static final String ACTION_RENAME         = "rename";
  public static final String ACTION_RF1_LOAD       = "rf1.load";
  public static final String ACTION_RF2_LOAD       = "rf2.load";
  public static final String ACTION_TEXT_EDIT      = "text.edit";
  public static final String ACTION_UNPACK         = "unpack";

  private BasicFrm                               owner;
  private ScreenFrm                              screenFrm;
  private AbstractFileWorker.PathListener        pathListener;
  private Map<String,Collection<AbstractButton>> actionCmd2Btn;
  private java.util.List<AbstractFileWorker>     fileWorkers;


  public FileActionMngr(
		BasicFrm                        owner,
		ScreenFrm                       screenFrm,
		AbstractFileWorker.PathListener pathListener )
  {
    this.owner         = owner;
    this.screenFrm     = screenFrm;
    this.pathListener  = pathListener;
    this.actionCmd2Btn = new HashMap<>();
    this.fileWorkers   = new ArrayList<>();
  }


  public FileActionResult actionPerformed(
				String                     actionCmd,
				java.util.List<FileObject> files )
							throws IOException
  {
    FileActionResult rv = FileActionResult.NONE;
    if( (actionCmd != null) && (files != null) ) {
      if( !files.isEmpty() ) {
	rv = FileActionResult.DONE;
	if( actionCmd.equals( ACTION_COPY_PATH ) ) {
	  doEditPathCopy( files );
	} else if( actionCmd.equals( ACTION_COPY_URL ) ) {
	  doEditURLCopy( files );
	} else if( actionCmd.equals( ACTION_COPY ) ) {
	  doEditFileCopy( files );
	} else if( actionCmd.equals( ACTION_EMU_LOAD_OPT ) ) {
	  doFileLoadIntoEmu( files, true, false );
	} else if( actionCmd.equals( ACTION_EMU_LOAD ) ) {
	  doFileLoadIntoEmu( files, false, false );
	} else if( actionCmd.equals( ACTION_EMU_START ) ) {
	  doFileLoadIntoEmu( files, false, true );
	} else if( actionCmd.equals( ACTION_RF1_LOAD ) ) {
	  if( this.screenFrm != null ) {
	    EmuThread emuThread = this.screenFrm.getEmuThread();
	    if( emuThread != null ) {
	      doFileRAMFloppyLoad( files, emuThread.getRAMFloppy1() );
	    }
	  }
	} else if( actionCmd.equals( ACTION_RF2_LOAD ) ) {
	  if( this.screenFrm != null ) {
	    EmuThread emuThread = this.screenFrm.getEmuThread();
	    if( emuThread != null ) {
	      doFileRAMFloppyLoad( files, emuThread.getRAMFloppy2() );
	    }
	  }
	} else if( actionCmd.equals( ACTION_TEXT_EDIT ) ) {
	  doFileEditText( files );
	} else if( actionCmd.equals( ACTION_IMAGE_VIEW ) ) {
	  doFileShowImage( files );
	} else if( actionCmd.equals( ACTION_DISK_VIEW ) ) {
	  doFileShowDisk( files );
	} else if( actionCmd.equals( ACTION_HEX_EDIT ) ) {
	  doFileEditHex( files );
	} else if( actionCmd.equals( ACTION_HEX_DIFF ) ) {
	  doFileDiffHex( files );
	} else if( actionCmd.equals( ACTION_CONVERT ) ) {
	  doFileConvert( files );
	} else if( actionCmd.equals( ACTION_AUDIO_IN ) ) {
	  doFileAudioIn( files );
	} else if( actionCmd.equals( ACTION_PLAY ) ) {
	  doFilePlay( files );
	} else if( actionCmd.equals( ACTION_PLAY_AC1 ) ) {
	  doFilePlayAC1( files );
	} else if( actionCmd.equals( ACTION_PLAY_AC1BASIC ) ) {
	  doFilePlayAC1Basic( files );
	} else if( actionCmd.equals( ACTION_PLAY_SCCH ) ) {
	  doFilePlaySCCH( files );
	} else if( actionCmd.equals( ACTION_PLAY_KC85 ) ) {
	  doFilePlayKC( files, 1 );
	} else if( actionCmd.equals( ACTION_PLAY_Z1013 ) ) {
	  doFilePlayZ1013( files, false );
	} else if( actionCmd.equals( ACTION_PLAY_Z1013HS ) ) {
	  doFilePlayZ1013( files, true );
	} else if( actionCmd.equals( ACTION_PLAY_Z9001 ) ) {
	  doFilePlayKC( files, 0 );
	} else if( actionCmd.equals( ACTION_PACK_TAR ) ) {
	  doFilePackTar( files, false );
	} else if( actionCmd.equals( ACTION_PACK_TGZ ) ) {
	  doFilePackTar( files, true );
	} else if( actionCmd.equals( ACTION_PACK_ZIP ) ) {
	  doFilePackZip( files );
	} else if( actionCmd.equals( ACTION_PACK_GZIP ) ) {
	  doFilePackGZip( files );
	} else if( actionCmd.equals( ACTION_UNPACK ) ) {
	  doFileUnpack( files );
	} else if( actionCmd.equals( ACTION_CHECKSUM ) ) {
	  doFileChecksum( files );
	} else if( actionCmd.equals( ACTION_LAST_MODIFIED ) ) {
	  if( LastModifiedDlg.open(
				this.owner,
				getFiles( files, false ) ) )
	  {
	    rv = FileActionResult.FILES_CHANGED;
	  }
	} else if( actionCmd.equals( ACTION_RENAME ) ) {
	  if( doFileRename( files ) ) {
	    rv = FileActionResult.FILE_RENAMED;
	  }
	} else if( actionCmd.equals( ACTION_DELETE ) ) {
	  doFileDelete( files );
	} else if( actionCmd.equals( ACTION_PROPERTIES ) ) {
	  doFileProp( files );
	} else {
	  rv = FileActionResult.NONE;
	}
      }
    }
    return rv;
  }


  public void addCopyFileNameMenuItemsTo( JPopupMenu popup, JMenu menu )
  {
    addJMenuItem(
		"Vollst\u00E4ndige Datei-/Verzeichnisnamen kopieren",
		ACTION_COPY_PATH,
		popup,
		menu );

    addJMenuItem(
		"Datei-/Verzeichnisnamen als URL kopieren",
		ACTION_COPY_URL,
		popup,
		menu );
  }


  public void addCopyFileMenuItemTo( JPopupMenu popup, JMenu menu )
  {
    addJMenuItem(
		"Dateien/Verzeichnisse kopieren",
		ACTION_COPY,
		popup,
		menu );
  }


  public void addFileMenuItemsTo( JPopupMenu popup, JMenu menu )
  {
    addJMenuItem(
		"Im Texteditor \u00F6ffnen...",
		ACTION_TEXT_EDIT,
		KeyEvent.VK_E, Event.CTRL_MASK,
		popup,
		menu );

    addJMenuItem(
		"Im Bildbetrachter anzeigen...",
		ACTION_IMAGE_VIEW,
		KeyEvent.VK_B, Event.CTRL_MASK,
		popup,
		menu );

    addJMenuItem(
		"Im Diskettenabbilddatei-Insprektor anzeigen...",
		ACTION_DISK_VIEW,
		popup,
		menu );

    addJMenuItem(
		"Im Hex-Editor \u00F6ffnen...",
		ACTION_HEX_EDIT,
		popup,
		menu );

    addJMenuItem(
		"Im Hex-Dateivergleicher \u00F6ffnen...",
		ACTION_HEX_DIFF,
		popup,
		menu );

    addJMenuItem(
		"Im Dateikonverter \u00F6ffnen...",
		ACTION_CONVERT,
		popup,
		menu );

    addJMenuItem(
		"In Audio/Kassette \u00F6ffnen...",
		ACTION_AUDIO_IN,
		popup,
		menu );

    addJMenuItem(
		"Wiedergeben",
		ACTION_PLAY,
		popup,
		menu );

    JMenu menuPlayAs  = null;
    JMenu popupPlayAs = null;
    if( menu != null ) {
      menuPlayAs = new JMenu( "Wiedergeben im" );
      menu.add( menuPlayAs );
    }
    if( popup != null ) {
      popupPlayAs = new JMenu( "Wiedergeben im" );
      popup.add( popupPlayAs );
    }

    addJMenuItem(
		"AC1-Format",
		ACTION_PLAY_AC1,
		null,
		popupPlayAs,
		menuPlayAs );

    addJMenuItem(
		"AC1-BASIC-Format",
		ACTION_PLAY_AC1,
		null,
		popupPlayAs,
		menuPlayAs );

    addJMenuItem(
		"AC1/LLC2-TurboSave-Format",
		ACTION_PLAY_SCCH,
		null,
		popupPlayAs,
		menuPlayAs );

    addJMenuItem(
		"KC-Format (HC900, KC85/2..5, KC-BASIC)",
		ACTION_PLAY_KC85,
		null,
		popupPlayAs,
		menuPlayAs );

    addJMenuItem(
		"KC-Format (KC85/1, KC87, Z9001)",
		ACTION_PLAY_Z9001,
		null,
		popupPlayAs,
		menuPlayAs );

    addJMenuItem(
		"Z1013-Format",
		ACTION_PLAY_Z1013,
		null,
		popupPlayAs,
		menuPlayAs );

    addJMenuItem(
		"Z1013-Headersave-Format",
		ACTION_PLAY_Z1013HS,
		null,
		popupPlayAs,
		menuPlayAs );

    JMenu menuPack  = null;
    JMenu popupPack = null;
    if( menu != null ) {
      menuPack = new JMenu( "Packen in" );
      menu.add( menuPack );
    }
    if( popup != null ) {
      popupPack = new JMenu( "Packen in" );
      popup.add( popupPack );
    }
    addJMenuItem(
		"TAR-Archiv...",
		ACTION_PACK_TAR,
		null,
		popupPack,
		menuPack );

    addJMenuItem(
		"TGZ-Archiv...",
		ACTION_PACK_TGZ,
		null,
		popupPack,
		menuPack );

    addJMenuItem(
		"ZIP-Archiv...",
		ACTION_PACK_ZIP,
		null,
		popupPack,
		menuPack );
    addSeparator( null, popupPack, menuPack );

    addJMenuItem(
		"GZip-Datei...",
		ACTION_PACK_GZIP,
		null,
		popupPack,
		menuPack );

    addJMenuItem(
		"Entpacken...",
		ACTION_UNPACK,
		popup,
		menu );
    addSeparator( popup, menu );

    addJMenuItem(
		"Pr\u00FCfsumme/Hash-Wert berechnen...",
		ACTION_CHECKSUM,
		popup,
		menu );

    addJMenuItem(
		"\u00C4nderungszeitpunkt setzen...",
		ACTION_LAST_MODIFIED,
		popup,
		menu );

    addJMenuItem(
		"Umbenennen...",
		ACTION_RENAME,
		popup,
		menu );

    addJMenuItem(
		"L\u00F6schen",
		ACTION_DELETE,
		popup,
		menu );
    addSeparator( popup, menu );

    addJMenuItem(
		"Eigenschaften...",
		ACTION_PROPERTIES,
		popup,
		menu );
  }


  public void addLoadIntoEmuMenuItemsTo( JPopupMenu popup, JMenu menu )
  {
    addJMenuItem(
		"In Emulator laden mit...",
		ACTION_EMU_LOAD_OPT,
		KeyEvent.VK_L,
		Event.CTRL_MASK,
		popup,
		menu );

    addJMenuItem(
		"In Emulator laden",
		ACTION_EMU_LOAD,
		KeyEvent.VK_L,
		Event.CTRL_MASK | Event.SHIFT_MASK,
		popup,
		menu );

    addJMenuItem(
		"Im Emulator starten",
		ACTION_EMU_START,
		KeyEvent.VK_R,
		Event.CTRL_MASK,
		popup,
		menu );
    addSeparator( popup, menu );

    addJMenuItem(
		"In RAM-Floppy 1 laden",
		ACTION_RF1_LOAD,
		popup,
		menu );

    addJMenuItem(
		"In RAM-Floppy 2 laden",
		ACTION_RF2_LOAD,
		popup,
		menu );
  }


  public JButton createEditTextButton()
  {
    return createAndRegisterImageButton(
			"/images/file/edit.png",
			"Im Texteditor \u00F6ffnen",
			ACTION_TEXT_EDIT );
  }


  public JButton createLoadIntoEmuButton()
  {
    return createAndRegisterImageButton(
			"/images/file/load.png",
			"In Emulator laden",
			ACTION_EMU_LOAD );
  }


  public JButton createPlayButton()
  {
    return createAndRegisterImageButton(
			"/images/file/play.png",
			"Wiedergeben",
			ACTION_PLAY );
  }


  public JButton createStartInEmuButton()
  {
    return createAndRegisterImageButton(
			"/images/file/start.png",
			"Im Emulator starten",
			ACTION_EMU_START );
  }


  public JButton createViewImageButton()
  {
    return createAndRegisterImageButton(
			"/images/file/image.png",
			"Im Bildbetrachter anzeigen",
			ACTION_IMAGE_VIEW );
  }


  public boolean doFileAction( FileObject fObj ) throws IOException
  {
    boolean done = false;
    if( fObj != null ) {
      File file = fObj.getFile();
      if( file != null ) {
	if( file.isFile() ) {
	  FileCheckResult checkResult = fObj.getCheckResult();
	  if( checkResult != null ) {
	    if( checkResult.isArchiveFile()
		|| checkResult.isCompressedFile() )
	    {
	      doFileUnpack( fObj );
	      done = true;
	    } else if( checkResult.isAudioFile() ) {
	      AudioPlayer.play( this.owner, file );
	      done = true;
	    } else if( checkResult.isImageFile() ) {
	      ImageFrm.open( file );
	      done = true;
	    } else if( checkResult.isTextFile() ) {
	      EmuThread emuThread = null;
	      if( this.screenFrm != null ) {
		emuThread = this.screenFrm.getEmuThread();
	      }
	      TextEditFrm.open( emuThread ).openFile( file );
	      done = true;
	    }
	  }
	  String fName = file.getName();
	  if( fName != null ) {
	    fName = fName.toLowerCase();
	  } else {
	    fName = "";
	  }
	  if( !done ) {
	    if( fName.endsWith( ".prj" ) ) {
	      Properties props = TextEditFrm.loadProject( file );
	      if( props != null ) {
		EmuThread emuThread = null;
		if( this.screenFrm != null ) {
		  emuThread = this.screenFrm.getEmuThread();
		}
		TextEditFrm.open( emuThread ).openProject( file, props );
		done = true;
	      } else {
		  throw new IOException(
			"Die PRJ-Datei ist keine JKCEMU-Projektdatei." );
	      }
	    }
	  }
	  if( !done && (checkResult != null) ) {
	    boolean  loadable = false;
	    FileInfo fileInfo = checkResult.getFileInfo();
	    if( fileInfo != null ) {
	      FileFormat fileFmt = fileInfo.getFileFormat();
	      if( fileFmt != null ) {
		if( !fileFmt.equals( FileFormat.BIN ) ) {
		  loadable = true;
		}
	      }
	    }
	    if( fName.endsWith( ".bin" ) ) {
	      loadable = true;
	    }
	    if( loadable ) {
	      if( this.screenFrm != null ) {
		LoadDlg.loadFile(
			this.owner,
			this.screenFrm,
			file,
			true,		// interactive
			true,		// startEnabled
			checkResult.isStartableFile() );
		done = true;
	      }
	    }
	  }
	  if( !done ) {
	    if( file.canRead() ) {
	      try {
		if( Desktop.isDesktopSupported() ) {
		  Desktop desktop = Desktop.getDesktop();
		  if( desktop != null ) {
		    if( desktop.isSupported( Desktop.Action.OPEN ) ) {
		      desktop.open( file );
		      done = true;
		    }
		  }
		}
	      }
	      catch( Exception ex ) {
		BasicDlg.showErrorDlg( this.owner, ex );
	      }
	    }
	  }
	}
      }
    }
    return done;
  }


  public java.util.List<AbstractFileWorker> getFileWorkers()
  {
    return this.fileWorkers;
  }


  public void updActionButtonsEnabled(
			java.util.List<FileObject> files )
  {
    boolean         isAudio         = false;
    boolean         isDisk          = false;
    boolean         isImage         = false;
    boolean         isStartable     = false;
    boolean         isUnpackable    = false;
    boolean         isHS            = false;
    boolean         isBin           = false;
    boolean         isKCBasicHead   = false;
    boolean         isKCBasic       = false;
    boolean         isKCSys         = false;
    boolean         isKC85Tap       = false;
    boolean         isZ9001Tap      = false;
    boolean         isBasic60F7     = false;
    boolean         stateOneEntry   = false;
    boolean         stateOneFile    = false;
    boolean         stateEntries    = false;
    boolean         stateStartable  = false;
    boolean         stateAudio      = false;
    boolean         stateDisk       = false;
    boolean         stateImage      = false;
    boolean         stateText       = false;
    boolean         stateUnpackable = false;
    FileCheckResult checkResult     = null;

    if( files != null ) {
      int n = files.size();
      if( n == 1 ) {
	FileObject fObj = files.get( 0 );
	File       file = fObj.getFile();
	if( file != null ) {
	  stateOneFile = file.isFile();
	}
	checkResult   = fObj.getCheckResult();
	stateOneEntry = true;
      }
      if( n > 0 ) {
	stateEntries = true;
      }
    }
    if( checkResult != null ) {
      isAudio       = checkResult.isAudioFile();
      isDisk        = (checkResult.isNonPlainDiskFile()
				|| checkResult.isPlainDiskFile());
      isImage       = checkResult.isImageFile();
      isStartable   = checkResult.isStartableFile();
      isUnpackable  = (checkResult.isArchiveFile()
				|| checkResult.isCompressedFile()
				|| isDisk);
      isHS          = checkResult.isHeadersaveFile();
      isBin         = checkResult.isBinFile();
      isKCBasicHead = checkResult.isKCBasicHeadFile();
      isKCBasic     = checkResult.isKCBasicFile();
      isKCSys       = checkResult.isKCSysFile();
      isKC85Tap     = checkResult.isKC85TapFile();
      isZ9001Tap    = checkResult.isZ9001TapFile();
      isBasic60F7   = false;
      if( isHS ) {
	FileInfo info = checkResult.getFileInfo();
	if( info != null ) {
	  if( (info.getBegAddr() == 0x60F7)
	      && (info.getFileType() == 'B') )
	  {
	    isBasic60F7 = true;
	  }
	}
      }
      stateStartable  = (stateOneFile && isStartable);
      stateAudio      = (stateOneFile && isAudio);
      stateDisk       = (stateOneFile && isDisk);
      stateImage      = (stateOneFile && isImage);
      stateText       = (stateOneFile && !stateImage && !stateAudio);
      stateUnpackable = (stateOneFile && isUnpackable);
    }

    setActionBtnsEnabled( ACTION_COPY_PATH, stateEntries );
    setActionBtnsEnabled( ACTION_COPY_URL, stateEntries );
    setActionBtnsEnabled( ACTION_COPY, stateEntries );

    setActionBtnsEnabled( ACTION_EMU_LOAD_OPT, stateOneFile );
    setActionBtnsEnabled( ACTION_EMU_LOAD, stateOneFile );
    setActionBtnsEnabled( ACTION_EMU_START, stateOneFile );

    setActionBtnsEnabled( ACTION_RF1_LOAD, stateOneFile );
    setActionBtnsEnabled( ACTION_RF2_LOAD, stateOneFile );

    setActionBtnsEnabled( ACTION_TEXT_EDIT, stateText );
    setActionBtnsEnabled( ACTION_IMAGE_VIEW, stateImage );
    setActionBtnsEnabled( ACTION_DISK_VIEW, stateDisk );
    setActionBtnsEnabled( ACTION_HEX_EDIT, stateOneFile );
    setActionBtnsEnabled( ACTION_HEX_DIFF, stateEntries );

    setActionBtnsEnabled(
		ACTION_AUDIO_IN,
		isAudio || isKC85Tap || isZ9001Tap );
    setActionBtnsEnabled( ACTION_PLAY, stateAudio );
    setActionBtnsEnabled( ACTION_PLAY_AC1, isBin );
    setActionBtnsEnabled( ACTION_PLAY_AC1BASIC, isBasic60F7 );
    setActionBtnsEnabled( ACTION_PLAY_SCCH, isBin || isBasic60F7 );
    setActionBtnsEnabled(
		ACTION_PLAY_KC85,
		isKCBasicHead || isKCBasic || isKCSys || isKC85Tap );
    setActionBtnsEnabled( ACTION_PLAY_Z9001, isKCSys || isZ9001Tap );
    setActionBtnsEnabled( ACTION_PLAY_Z1013, isBin );
    setActionBtnsEnabled( ACTION_PLAY_Z1013HS, isHS );

    setActionBtnsEnabled( ACTION_PACK_TAR, stateEntries );
    setActionBtnsEnabled( ACTION_PACK_TGZ, stateEntries );
    setActionBtnsEnabled( ACTION_PACK_ZIP, stateEntries );
    setActionBtnsEnabled( ACTION_PACK_GZIP, stateOneFile );
    setActionBtnsEnabled( ACTION_UNPACK, stateUnpackable );
    setActionBtnsEnabled( ACTION_CONVERT, stateOneFile );
    setActionBtnsEnabled( ACTION_CHECKSUM, stateEntries );
    setActionBtnsEnabled( ACTION_LAST_MODIFIED, stateEntries );
    setActionBtnsEnabled( ACTION_RENAME, stateOneEntry );
    setActionBtnsEnabled( ACTION_DELETE, stateEntries );
    setActionBtnsEnabled( ACTION_PROPERTIES, stateOneEntry );
  }


	/* --- Aktionen --- */

  private void doEditFileCopy( java.util.List<FileObject> files )
  {
    try {
      Toolkit tk = this.owner.getToolkit();
      if( tk != null ) {
	Clipboard clipboard = tk.getSystemClipboard();
	if( clipboard != null ) {
	  java.util.List<File> fileList = getFiles( files, false );
	  if( !fileList.isEmpty() ) {
	    FileListSelection fls = new FileListSelection( fileList );
	    clipboard.setContents( fls, fls );
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doEditPathCopy( java.util.List<FileObject> files )
  {
    int n = files.size();
    if( n > 0 ) {
      try {
	Toolkit tk = this.owner.getToolkit();
	if( tk != null ) {
	  Clipboard clipboard = tk.getSystemClipboard();
	  if( clipboard != null ) {
	    boolean       multi = false;
	    StringBuilder buf   = new StringBuilder( n * 256 );
	    for( FileObject fObj : files ) {
	      Path path = fObj.getPath();
	      if( path != null ) {
		String fName = path.toAbsolutePath().normalize().toString();
		if( fName != null ) {
		  if( !fName.isEmpty() ) {
		    if( buf.length() > 0 ) {
		      buf.append( (char) '\n' );
		      multi = true;
		    }
		    buf.append( fName );
		  }
		}
	      }
	    }
	    if( buf.length() > 0 ) {
	      if( multi ) {
		buf.append( (char) '\n' );
	      }
	      StringSelection ss = new StringSelection( buf.toString() );
	      clipboard.setContents( ss, ss );
	    }
	  }
	}
      }
      catch( IllegalStateException ex ) {}
    }
  }


  private void doEditURLCopy( java.util.List<FileObject> files )
  {
    int n = files.size();
    if( n > 0 ) {
      try {
	Toolkit tk = this.owner.getToolkit();
	if( tk != null ) {
	  Clipboard clipboard = tk.getSystemClipboard();
	  if( clipboard != null ) {
	    boolean       multi = false;
	    StringBuilder buf   = new StringBuilder( n * 256 );
	    for( FileObject fObj : files ) {
	      File file = fObj.getFile();
	      if( file != null ) {
		URI uri = file.toURI();
		if( uri != null ) {
		  URL url = uri.toURL();
		  if( url != null ) {
		    String urlText = uri.toString();
		    if( urlText != null ) {
		      if( !urlText.isEmpty() ) {
			if( buf.length() > 0 ) {
			  buf.append( (char) '\n' );
			  multi = true;
			}
			buf.append( urlText );
		      }
		    }
		  }
		}
	      }
	    }
	    if( buf.length() > 0 ) {
	      if( multi ) {
		buf.append( (char) '\n' );
	      }
	      StringSelection ss = new StringSelection( buf.toString() );
	      clipboard.setContents( ss, ss );
	    }
	  }
	}
      }
      catch( IllegalStateException ex ) {}
      catch( MalformedURLException ex ) {
	BasicDlg.showErrorDlg( this.owner, ex );
      }
    }
  }


  private void doFileAudioIn(
		java.util.List<FileActionMngr.FileObject> files )
  {
    if( (this.screenFrm != null) && (files.size() == 1) ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isAudioFile() || checkResult.isTapeFile() ) {
	  this.screenFrm.openAudioInFile( file );
	}
      }
    }
  }


  private void doFileChecksum(
		java.util.List<FileActionMngr.FileObject> files )
  {
    java.util.List<File> fileList = getFiles( files, true );
    if( !fileList.isEmpty() ) {
      FileChecksumFrm.open( fileList );
    }
  }


  private void doFileConvert(
		java.util.List<FileActionMngr.FileObject> files )
  {
    File file = getFile( files, true );
    if( file != null ) {
      if( file.isFile() ) {
	FileConvertFrm.open( file );
      }
    }
  }


  private void doFileDelete(
		java.util.List<FileActionMngr.FileObject> files )
  {
    java.util.List<Path> paths = getPaths( files );
    if( !paths.isEmpty() ) {
      FileRemover.startRemove(
			this.owner,
			paths,
			this.pathListener,
			this.fileWorkers );
    }
  }


  private void doFileDiffHex(
		java.util.List<FileActionMngr.FileObject> files )
  {
    java.util.List<File> fileList = getFiles( files, true );
    if( !fileList.isEmpty() ) {
      HexDiffFrm.open().addFiles( fileList );
    }
  }


  private void doFileEditHex(
		java.util.List<FileActionMngr.FileObject> files )
  {
    File file = getFile( files, true );
    if( file != null ) {
      if( file.isFile() ) {
	HexEditFrm.open( file );
      }
    }
  }


  private void doFileEditText(
		java.util.List<FileActionMngr.FileObject> files )
  {
    File file = getFile( files, true );
    if( file != null ) {
      if( file.isFile() ) {
	TextEditFrm.open( null ).openFile( file );
      }
    }
  }


  private void doFileLoadIntoEmu(
		java.util.List<FileActionMngr.FileObject> files,
		boolean                                   interactive,
		boolean                                   startSelected )
  {
    if( this.screenFrm != null ) {
      File file = getFile( files, true );
      if( file != null ) {
	if( file.isFile() ) {
	  LoadDlg.loadFile(
			this.owner,
			this.screenFrm,
			file,
			interactive,
			true,		// startEnabled
			startSelected );
	}
      }
    }
  }


  private void doFilePackGZip(
		java.util.List<FileActionMngr.FileObject> files )
  {
    File file = getFile( files, true );
    if( file != null ) {
      String fileName = file.getName();
      if( fileName != null ) {
	fileName += ".gz";
      }
      File outFile = askForOutputFile(
				file,
				"GZip-Datei speichern",
				fileName );
      if( outFile != null ) {
	GZipPacker.packFile( this.owner, file, outFile );
      }
    }
  }


  private void doFilePackTar(
		java.util.List<FileActionMngr.FileObject> files,
		boolean                                   compression )
  {
    java.util.List<Path> paths = getPaths( files );
    if( !paths.isEmpty() ) {
      Path firstPath = paths.get( 0 );
      Path namePath  = firstPath.getFileName();
      if( namePath != null ) {
	String fileName = namePath.toString();
	if( fileName != null ) {
	  int pos = fileName.indexOf( '.' );
	  if( (pos == 0) && (fileName.length() > 1) ) {
	    pos = fileName.indexOf( '.', 1 );
	  }
	  if( pos >= 0 ) {
	    fileName = fileName.substring( 0, pos );
	  }
	  if( !fileName.isEmpty() ) {
	    if( compression ) {
	      fileName += ".tgz";
	    } else {
	      fileName += ".tar";
	    }
	  }
	  try {
	    File outFile = askForOutputFile(
				firstPath.toFile(),
				compression ?
					"TGZ-Datei speichern"
					: "TAR-Datei speichern",
				fileName );
	    if( outFile != null ) {
	      TarPacker.packFiles( this.owner, paths, outFile, compression );
	    }
	  }
	  catch( UnsupportedOperationException ex ) {}
	}
      }
    }
  }


  private void doFilePackZip(
		java.util.List<FileActionMngr.FileObject> files )
  {
    java.util.List<Path> paths = getPaths( files );
    if( !paths.isEmpty() ) {
      Path firstPath = paths.get( 0 );
      Path namePath  = firstPath.getFileName();
      if( namePath != null ) {
	String fileName = namePath.toString();
	if( fileName != null ) {
	  int pos = fileName.indexOf( '.' );
	  if( (pos == 0) && (fileName.length() > 1) ) {
	    pos = fileName.indexOf( '.', 1 );
	  }
	  if( pos >= 0 ) {
	    fileName = fileName.substring( 0, pos );
	  }
	  if( !fileName.isEmpty() ) {
	    fileName += ".zip";
	  }
	  try {
	    File outFile = askForOutputFile(
				firstPath.toFile(),
				"ZIP-Datei speichern",
				fileName );
	    if( outFile != null ) {
	      ZipPacker.packFiles( this.owner, paths, outFile );
	    }
	  }
	  catch( UnsupportedOperationException ex ) {}
	}
      }
    }
  }


  private void doFilePlay(
		java.util.List<FileActionMngr.FileObject> files )
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isAudioFile() ) {
	  AudioPlayer.play( this.owner, file );
	}
      }
    }
  }


  private void doFilePlayAC1(
		java.util.List<FileActionMngr.FileObject> files )
							throws IOException
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isBinFile() ) {
	  String           title = "AC1-Wiedergabe von " + file.getName();
	  ReplyFileHeadDlg dlg   = new ReplyFileHeadDlg(
			this.owner,
			file.getName(),
			"Wiedergeben",
			title,
			ReplyFileHeadDlg.Option.BEGIN_ADDRESS,
			ReplyFileHeadDlg.Option.START_ADDRESS,
			ReplyFileHeadDlg.Option.FILE_NAME_16 );
	  dlg.setVisible( true );
	  if( dlg.wasApproved() ) {
	    int startAddr = dlg.getApprovedStartAddress();
	    if( startAddr < 0 ) {
	      startAddr = 0;
	    }
	    AudioPlayer.play(
		this.owner,
		new AC1AudioDataStream(
			false,
			EmuUtil.readFile( file, true, 0x10000 ),
			dlg.getApprovedFileName(),
			dlg.getApprovedBeginAddress(),
			startAddr ),
		title + "..." );
	  }
	}
      }
    }
  }


  private void doFilePlayAC1Basic(
		java.util.List<FileActionMngr.FileObject> files )
							throws IOException
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isBinFile() || checkResult.isHeadersaveFile() ) {
	  byte[] buf  = EmuUtil.readFile( file, true, 0x10000 );
	  int    len  = buf.length;
	  if( len > 0 ) {
	    String fileName = file.getName();
	    int    offs     = 0;
	    if( len > 32 ) {
	      if( (buf[ 13 ] == (byte) 0xD3)
		  && (buf[ 14 ] == (byte) 0xD3)
		  && (buf[ 15 ] == (byte) 0xD3) )
	      {
		offs += 32;
		len  -= 32;
		String s = EmuUtil.extractSingleAsciiLine( buf, 16, 16 );
		if( s != null ) {
		  fileName = s;
		}
	      }
	    }
	    String title = "AC1-BASIC-Wiedergabe von " + file.getName();
	    ReplyFileHeadDlg dlg = new ReplyFileHeadDlg(
			this.owner,
			fileName,
			"Wiedergeben",
			title,
			ReplyFileHeadDlg.Option.FILE_NAME_6 );
	    dlg.setVisible( true );
	    if( dlg.wasApproved() ) {
	      AudioPlayer.play(
			this.owner,
			new AC1AudioDataStream(
				true,
				buf,
				offs,
				len,
				dlg.getApprovedFileName(),
				-1,
				-1 ),
			title + "..." );
	    }
	  }
	}
      }
    }
  }


  private void doFilePlayKC(
		java.util.List<FileActionMngr.FileObject> files,
		int                                       firstBlkNum )
						throws IOException
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	String title = "KC-Wiedergabe von " + file.getName() + "...";
	if( checkResult.isKCBasicHeadFile() ) {
	  AudioPlayer.play(
		this.owner,
		new KCAudioDataStream(
			false,
			1,
			EmuUtil.readFile( file, true, 0x10000 ) ),
		title );
	}
	else if( checkResult.isKCBasicFile() ) {
	  byte[] fileBytes = EmuUtil.readFile( file, true, 0x10000 );
	  if( fileBytes != null ) {
	    if( fileBytes.length > 0 ) {
	      ReplyFileHeadDlg dlg = new ReplyFileHeadDlg(
			this.owner,
			file.getName(),
			"Wiedergeben",
			title,
			ReplyFileHeadDlg.Option.FILE_NAME_8 );
	      dlg.setVisible( true );
	      if( dlg.wasApproved() ) {
		String name  = dlg.getApprovedFileName();
		byte[] buf   = new byte[ fileBytes.length + 11 ];
		int    dst   = 0;
		buf[ dst++ ] = (byte) 0xD3;
		buf[ dst++ ] = (byte) 0xD3;
		buf[ dst++ ] = (byte) 0xD3;
		if( name != null ) {
		  int len = name.length();
		  int src = 0;
		  while( (dst < 11) && (src < len) ) {
		    buf[ dst++ ] = (byte) (name.charAt( src++ ) & 0x7F);
		  }
		}
		while( dst < 11 ) {
		  buf[ dst++ ] = (byte) 0x20;
		}
		AudioPlayer.play(
			this.owner,
			new KCAudioDataStream( false, 1, buf ),
			title );
	      }
	    }
	  }
	}
	else if( checkResult.isKCSysFile() ) {
	  AudioPlayer.play(
		this.owner,
		new KCAudioDataStream(
			false,
			firstBlkNum,
			EmuUtil.readFile( file, true, 0x10000 ) ),
		title );
	}
	else if( (checkResult.isKC85TapFile() && (firstBlkNum == 1))
		 || (checkResult.isZ9001TapFile() && (firstBlkNum == 0)) )
	{
	  AudioPlayer.play(
		this.owner,
		new KCAudioDataStream(
			true,
			0,
			EmuUtil.readFile( file, true, 0x10110 ) ),
		title );
	}
      }
    }
  }


  private void doFilePlaySCCH(
		java.util.List<FileActionMngr.FileObject> files )
							throws IOException
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isBinFile() || checkResult.isHeadersaveFile() ) {
	  byte[] buf = EmuUtil.readFile( file, true, 0x10000 );
	  int    len = buf.length;
	  if( len > 0 ) {
	    ReplyFileHeadDlg.Option[] options = null;
	    String fileName = file.getName();
	    int    offs     = 0;
	    int    begAddr  = -1;
	    int    endAddr  = -1;
	    int    fType    = -1;
	    if( len > 32 ) {
	      if( (buf[ 13 ] == (byte) 0xD3)
		  && (buf[ 14 ] == (byte) 0xD3)
		  && (buf[ 15 ] == (byte) 0xD3) )
	      {
		offs += 32;
		len  -= 32;
		begAddr = EmuUtil.getWord( buf, 0 );
		endAddr = EmuUtil.getWord( buf, 2 );
		if( (begAddr == 0x60F7) && (buf[ 12 ] == (byte) 'B') ) {
		  fType   = 'B';
		  options = new ReplyFileHeadDlg.Option[] {
			ReplyFileHeadDlg.Option.FILE_NAME_16 };
		} else {
		  options = new ReplyFileHeadDlg.Option[] {
			ReplyFileHeadDlg.Option.FILE_NAME_16,
			ReplyFileHeadDlg.Option.SCCH_FILE_TYPE };
		}
		String s = EmuUtil.extractSingleAsciiLine( buf, 16, 16 );
		if( s != null ) {
		  fileName = s;
		}
	      }
	    }
	    if( options == null ) {
	      options = new ReplyFileHeadDlg.Option[] {
				ReplyFileHeadDlg.Option.BEGIN_ADDRESS,
				ReplyFileHeadDlg.Option.END_ADDRESS,
				ReplyFileHeadDlg.Option.FILE_NAME_16,
				ReplyFileHeadDlg.Option.SCCH_FILE_TYPE };
	    }
	    String title = "AC1/LLC2-TurboSave-Wiedergabe von "
							+ file.getName();
	    ReplyFileHeadDlg dlg = new ReplyFileHeadDlg(
						this.owner,
						fileName,
						"Wiedergeben",
						title,
						options );
	    dlg.setVisible( true );
	    if( dlg.wasApproved() ) {
	      if( begAddr < 0 ) {
		begAddr = dlg.getApprovedBeginAddress();
	      }
	      if( endAddr < 0 ) {
		endAddr = dlg.getApprovedEndAddress();
		if( endAddr < 0 ) {
		  endAddr = begAddr + len - 1;
		}
	      }
	      if( fType < 0 ) {
		fType = dlg.getApprovedSCCHFileType();
	      }
	      AudioPlayer.play(
			this.owner,
			new SCCHAudioDataStream(
				buf,
				offs,
				len,
				dlg.getApprovedFileName(),
				(char) fType,
				begAddr,
				endAddr ),
			title + "..." );
	    }
	  }
	}
      }
    }
  }


  private void doFilePlayZ1013(
		java.util.List<FileActionMngr.FileObject> files,
		boolean                                   headersave )
						throws IOException
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isBinFile()
		|| (headersave && checkResult.isHeadersaveFile()) )
	{
	  AudioPlayer.play(
		this.owner,
		new Z1013AudioDataStream(
			headersave,
			EmuUtil.readFile( file, true, 0x10020 ) ),
		String.format(
			"Z1013%s-Wiedergabe von %s...",
			headersave ? "-Headersave" : "",
			file.getName() ) );
	}
      }
    }
  }


  private void doFileProp(
		java.util.List<FileActionMngr.FileObject> files )
						throws IOException
  {
    Path path = getPath( files );
    if( path != null ) {
      (new FilePropDlg( this.owner, path )).setVisible( true );
    }
  }


  private void doFileRAMFloppyLoad(
		java.util.List<FileActionMngr.FileObject> files,
		RAMFloppy                                 ramFloppy )
  {
    File file = getFile( files, true );
    if( file != null ) {
      try {
	ramFloppy.load( file );
	Main.setLastFile( file, "ramfloppy" );
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg(
		this.owner,
		"Die RAM-Floppy kann nicht geladen werden.\n\n"
						+ ex.getMessage() );
      }
    }
  }


  private boolean doFileRename(
		java.util.List<FileActionMngr.FileObject> files )
  {
    boolean rv = false;
    if( files.size() == 1 ) {
      FileObject fObj = files.get( 0 );
      Path       path = fObj.getPath();
      if( path != null ) {
	path = EmuUtil.renamePath( this.owner, path );
	if( path != null ) {
	  fObj.setPath( path );
	  rv = true;
	}
      }
    }
    return rv;
  }


  private void doFileShowDisk(
		java.util.List<FileActionMngr.FileObject> files )
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isNonPlainDiskFile()
		|| checkResult.isPlainDiskFile() )
	{
	  DiskImgViewFrm.open( file );
	}
      }
    }
  }


  private void doFileShowImage(
		java.util.List<FileActionMngr.FileObject> files )
  {
    if( files.size() == 1 ) {
      FileObject      fObj        = files.get( 0 );
      File            file        = fObj.getFile();
      FileCheckResult checkResult = fObj.getCheckResult();
      if( (file != null) && (checkResult != null) ) {
	if( checkResult.isImageFile() ) {
	  ImageFrm.open( file );
	}
      }
    }
  }


  private void doFileUnpack( FileObject fObj ) throws IOException
  {
    if( fObj != null ) {
      File file = fObj.getFile();
      if( file != null ) {
	String fileName = file.getName();
	if( (fileName != null) && file.isFile() ) {
	  String upperName = fileName.toUpperCase();
	  if( upperName.endsWith( ".GZ" ) ) {
	    File outFile = askForOutputFile(
			file,
			"Entpackte Datei speichern",
			fileName.substring( 0, fileName.length() - 3 ) );
	    if( outFile != null ) {
	      GZipUnpacker.unpackFile( this.owner, file, outFile );
	    }
	  }
	  else if( upperName.endsWith( ".TAR" )
		   || upperName.endsWith( ".TGZ" ) )
	  {
	    File outDir = EmuUtil.askForOutputDir(
				this.owner,
				file,
				"Entpacken nach:",
				"Archiv-Datei entpacken" );
	    if( outDir != null ) {
	      TarUnpacker.unpackFile(
				this.owner,
				file,
				outDir,
				upperName.endsWith( ".TGZ" ) );
	    }
	  }
	  else if( upperName.endsWith( ".JAR" )
		   || upperName.endsWith( ".ZIP" ) )
	  {
	    File outDir = EmuUtil.askForOutputDir(
				this.owner,
				file,
				"Entpacken nach:",
				"Archiv-Datei entpacken" );
	    if( outDir != null ) {
	      ZipUnpacker.unpackFile( this.owner, file, outDir );
	    }
	  } else {
	    FileCheckResult checkResult = fObj.getCheckResult();
	    if( checkResult != null ) {
	      if( checkResult.isPlainDiskFile() ) {
		DiskUtil.unpackPlainDiskFile( this.owner, file );
	      }
	      else if( checkResult.isNonPlainDiskFile() ) {
		AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this.owner,
								file,
								true );
		if( disk != null ) {
		  if( DiskUtil.checkAndConfirmWarning( this.owner, disk ) ) {
		    DiskUtil.unpackDisk( this.owner, file, disk, true );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }


  private void doFileUnpack(
		java.util.List<FileActionMngr.FileObject> files )
						throws IOException
  {
    if( files.size() == 1 ) {
      doFileUnpack( files.get( 0 ) );
    }
  }


	/* --- private Methoden --- */

  private File askForOutputFile(
			File   srcFile,
			String title,
			String presetName )
  {
    File preSelection = null;
    File parentFile   = srcFile.getParentFile();
    if( presetName != null ) {
      if( parentFile != null ) {
	preSelection = new File( parentFile, presetName );
      } else {
	preSelection = new File( presetName );
      }
    } else {
      preSelection = parentFile;
    }
    File file = EmuUtil.showFileSaveDlg( this.owner, title, preSelection );
    if( file != null ) {
      if( file.exists() ) {
	if( file.equals( srcFile ) ) {
	  BasicDlg.showErrorDlg(
		this.owner,
		"Die Ausgabedatei kann nicht\n"
			+ "mit der Quelldatei identisch sein." );
	  file = null;
	}
	else if( !file.isFile() ) {
	  BasicDlg.showErrorDlg(
		this.owner,
		file.getPath() + " existiert bereits\n"
			+ "und kann nicht als Datei angelegt werden." );
	  file = null;
	}
      }
    }
    return file;
  }


  private void addJMenuItem(
			String     text,
			String     actionCmd,
			JPopupMenu popup,
			JMenu...   menus )
  {
    addJMenuItem( text, actionCmd, null, popup, menus );
  }


  private void addJMenuItem(
			String     text,
			String     actionCmd,
			int        keyCode,
			int        modifiers,
			JPopupMenu popup,
			JMenu...   menus )
  {
    addJMenuItem(
		text,
		actionCmd,
		KeyStroke.getKeyStroke( keyCode, modifiers ),
		popup,
		menus );
  }


  private void addJMenuItem(
			String     text,
			String     actionCmd,
			KeyStroke  keyStroke,
			JPopupMenu popup,
			JMenu...   menus )
  {
    if( popup != null ) {
      popup.add( createAndRegisterJMenuItem( text, actionCmd, keyStroke ) );
    }
    if( menus != null ) {
      for( JMenu menu : menus ) {
	if( menu != null ) {
	  menu.add(
		createAndRegisterJMenuItem( text, actionCmd, keyStroke ) );
	}
      }
    }
  }


  private static void addSeparator( JPopupMenu popup, JMenu... menus )
  {
    if( popup != null ) {
      popup.addSeparator();
    }
    if( menus != null ) {
      for( JMenu menu : menus ) {
	if( menu != null ) {
	  menu.addSeparator();
	}
      }
    }
  }


  private JButton createAndRegisterImageButton(
				String resource,
				String text,
				String actionCmd )
  {
    JButton button = EmuUtil.createImageButton( this.owner, resource, text );
    button.setActionCommand( actionCmd );
    button.addActionListener( this.owner );
    registerButton( button, actionCmd );
    return button;
  }


  private JMenuItem createAndRegisterJMenuItem(
				String    text,
				String    actionCmd,
				KeyStroke keyStroke )
  {
    JMenuItem item = new JMenuItem( text );
    if( keyStroke != null ) {
      item.setAccelerator( keyStroke );
    }
    item.setActionCommand( actionCmd );
    item.addActionListener( this.owner );
    registerButton( item, actionCmd );
    return item;
  }


  private File getFile(
		java.util.List<FileObject> files,
		boolean                    regularFileOnly )
  {
    File file = null;
    if( files.size() == 1 ) {
      file = files.get( 0 ).getFile();
      if( file != null ) {
	if( regularFileOnly && !file.isFile() ) {
	  file = null;
	}
      }
    }
    return file;
  }


  private java.util.List<File> getFiles(
		java.util.List<FileObject> files,
		boolean                    regularFilesOnly )
  {
    int                  n  = files.size();
    java.util.List<File> rv = new ArrayList<>( n > 0 ? n : 1 );
    for( FileObject o : files ) {
      File file = o.getFile();
      if( file != null ) {
	if( !regularFilesOnly || file.isFile() ) {
	  rv.add( file );
	}
      }
    }
    return rv;
  }


  private Path getPath( java.util.List<FileObject> files )
  {
    Path path = null;
    if( files.size() == 1 ) {
      path = files.get( 0 ).getPath();
    }
    return path;
  }


  private java.util.List<Path> getPaths(
			java.util.List<FileObject> files )
  {
    int                  n  = files.size();
    java.util.List<Path> rv = new ArrayList<>( n > 0 ? n : 1 );
    for( FileObject o : files ) {
      Path path = o.getPath();
      if( path != null ) {
	rv.add( path );
      }
    }
    return rv;
  }


  private void registerButton( AbstractButton button, String actionCmd )
  {
    Collection<AbstractButton> c = this.actionCmd2Btn.get( actionCmd );
    if( c == null ) {
      c = new ArrayList<>();
      this.actionCmd2Btn.put( actionCmd, c );
    }
    c.add( button );
  }


  private void setActionBtnsEnabled( String actionCmd, boolean state )
  {
    Collection<AbstractButton> c = this.actionCmd2Btn.get( actionCmd );
    if( c != null ) {
      for( AbstractButton b : c ) {
	b.setEnabled( state );
      }
    }
  }
}
