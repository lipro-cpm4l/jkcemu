/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateikonverter
 */

package jkcemu.tools.fileconverter;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EventObject;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.audio.AudioFile;
import jkcemu.audio.AudioPlayFrm;
import jkcemu.audio.AudioUtil;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.audio.PCMDataInfo;
import jkcemu.audio.CSWFile;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.HexDocument;
import jkcemu.base.LimitedDocument;
import jkcemu.base.UserInputException;
import jkcemu.disk.AbstractFloppyDisk;
import jkcemu.disk.AnaDisk;
import jkcemu.disk.CPCDisk;
import jkcemu.disk.CopyQMDisk;
import jkcemu.disk.DiskUtil;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.FloppyDiskFormatDlg;
import jkcemu.disk.ImageDisk;
import jkcemu.disk.PlainDisk;
import jkcemu.disk.TeleDisk;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioCreator;
import jkcemu.file.FileFormat;
import jkcemu.file.FileInfo;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;
import jkcemu.file.LoadData;
import jkcemu.text.TextUtil;


public class FileConvertFrm extends BaseFrm implements
						DropTargetListener,
						ListSelectionListener
{
  public static final String TITLE = Main.APPNAME + " Dateikonverter";

  private static final int MAX_MEM_FILE_SIZE  = 0x40000;	// 256 KByte
  private static final int MAX_DISK_FILE_SIZE = 0x200000;	// 2 MByte
  private static final int MAX_TAPE_FILE_SIZE = 0x100000;	// 1 MByte

  private static final String HELP_PAGE = "/help/tools/fileconverter.htm";

  private static final String LABEL_BEG_ADDR   = "Anfangsadresse:";
  private static final String LABEL_START_ADDR = "Startadresse:";

  private static FileConvertFrm instance = null;

  private String                        orgFileDesc;
  private int                           orgFileTypeChar;
  private int                           orgStartAddr;
  private String                        orgRemark;
  private boolean                       orgIsBasicPrg;
  private boolean                       lastOutDirAsInDir;
  private String                        lastSavedTargetText;
  private FileNameFld                   fldSrcFile;
  private JButton                       btnSrcSelect;
  private JButton                       btnSrcRemove;
  private JTextField                    fldSrcInfo;
  private Vector<AbstractConvertTarget> targets;
  private JList<AbstractConvertTarget>  listTarget;
  private JLabel                        labelBegAddr;
  private JLabel                        labelStartAddr;
  private JLabel                        labelFileDesc;
  private JLabel                        labelFileType;
  private JLabel                        labelRemark;
  private JTextField                    fldFileDesc;
  private JTextField                    fldBegAddr;
  private JTextField                    fldStartAddr;
  private JTextField                    fldRemark;
  private JComboBox<String>             comboFileType;
  private HexDocument                   docBegAddr;
  private HexDocument                   docStartAddr;
  private LimitedDocument               docFileDesc;
  private LimitedDocument               docFileType;
  private LimitedDocument               docRemark;
  private JButton                       btnConvert;
  private JButton                       btnPlay;
  private JButton                       btnHelp;
  private JButton                       btnClose;


  public static FileConvertFrm open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
        instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new FileConvertFrm();
    }
    instance.toFront();
    instance.setVisible( true );
    return instance;
  }


  public static FileConvertFrm open( File file )
  {
    open();
    if( file != null ) {
      instance.openFile( file );
    }
    return instance;
  }


  public boolean getOrgIsBasicPrg()
  {
    return this.orgIsBasicPrg;
  }


  public int getOrgFileTypeChar()
  {
    return this.orgFileTypeChar;
  }


  public int getOrgStartAddr()
  {
    return this.orgStartAddr;
  }


  public int getBegAddr( boolean mandatory ) throws UserInputException
  {
    int rv = getAddr( this.docBegAddr );
    if( (rv < 0) && mandatory ) {
      throw new UserInputException( "Anfangsadresse nicht angegeben" );
    }
    return rv;
  }


  public int getStartAddr( boolean mandatory ) throws UserInputException
  {
    int rv = getAddr( this.docStartAddr );
    if( (rv < 0) && mandatory ) {
      throw new UserInputException( "Startadresse nicht angegeben" );
    }
    return rv;
  }


  public String getFileDesc()
  {
    return strip( this.fldFileDesc.getText() );
  }


  public String getFileDesc( boolean mandatory ) throws UserInputException
  {
    String s = getFileDesc();
    if( (s == null) && mandatory ) {
      throw new UserInputException( "Bezeichnung nicht angegeben" );
    }
    return s;
  }


  public String getFileType()
  {
    Object o = this.comboFileType.getSelectedItem();
    return o != null ? o.toString() : null;
  }


  public int getFileTypeChar( boolean mandatory ) throws UserInputException
  {
    int    rv = -1;
    String s  = getFileType();
    if( s != null ) {
      if( !s.isEmpty() ) {
	rv = s.charAt( 0 );
	if( (rv <= 0x20) || (rv >= 0x7F) ) {
	  rv = -1;
	}
      }
    }
    if( (rv < 0) && mandatory ) {
      throw new UserInputException( "Typ nicht angegeben oder ung\u00FCltig" );
    }
    return rv;
  }


  public String getRemark()
  {
    return strip( this.fldRemark.getText() );
  }


  public void openFile( File file )
  {
    if( file != null ) {
      try {
	if( !file.isFile() || !file.canRead() ) {
	  throw new IOException(
		file.getPath() +  ": Nicht gefunden oder nicht lesbar" );
	}
	clearOutFields();
	this.orgFileDesc     = null;
	this.orgFileTypeChar = -1;
	this.orgStartAddr    = -1;
	this.orgIsBasicPrg   = false;
	this.orgRemark       = null;

	String             infoMsg      = null;
	AbstractFloppyDisk disk         = null;
	int                begAddr      = -1;
	int                kcbasicOffs  = 0;
	int                kcbasicLen   = 0;
	int                dataOffs     = 0;
	int                dataLen      = 0;
	byte[]             dataBytes    = null;
	byte[]             kcbasicBytes = null;
	byte[]             fileBytes    = null;
	FileFormat         fileFmt      = null;
	boolean            multiTAP     = false;

	// Dateiname
	String fExt  = null;
	String fName = file.getName();
	if( fName != null ) {
	  if( !fName.isEmpty() ) {
	    /*
	     * Dateibasisname als Bezeichnung uebernehmen,
	     * Wenn ein Grossbuchstabe enthalten ist,
	     * dann die Gross-/Kleinschreibung des Basisnamen beibehalten,
	     * anderenfalls alles gross wandeln,
	     */
	    String tmpName = fName;
	    int pos = tmpName.lastIndexOf( '.' );
	    if( pos >= 0 ) {
	      tmpName = tmpName.substring( 0, pos );
	    }
	    int len = tmpName.length();
	    if( len > 0 ) {
	      boolean hasUpper = false;
	      for( int i = 0; i < len; i++ ) {
		if( Character.isUpperCase( tmpName.charAt( i ) ) ) {
		  hasUpper = true;
		  break;
		}
	      }
	      if( hasUpper ) {
		this.orgFileDesc = tmpName;
	      } else {
		this.orgFileDesc = tmpName.toUpperCase();
	      }
	    }
	  }
	  fName = fName.toLowerCase();
	  if( fName != null ) {
	    int pos = fName.lastIndexOf( '.' );
	    if( (pos > 0) && ((pos + 1) < fName.length()) ) {
	      fExt = fName.substring( pos + 1 );
	    }
	  }
	}

	// Datei analysieren
	StringBuilder infoBuf = new StringBuilder( 128 );
	boolean       done    = false;

	// Sound-Datei pruefen
	try {
	  PCMDataInfo info = AudioFile.getInfo( file );
	  infoBuf.append( "Sound-Datei" );
	  infoBuf.append( ", " );
	  AudioUtil.appendAudioFormatText( infoBuf, info );

	  /*
	   * Ausgabeformate ermitteln, in die konverttiert werden kann,
	   * das eigene Format dabei ausblenden
	   */
	  this.targets.add( new AudioFileTarget( this, file ) );
	  done = true;
	}
	catch( IOException ex2 ) {}

	// Dateiextension pruefen
	if( !done && (fName != null) ) {
	  if( fName.endsWith( ".bin" ) || fName.endsWith( ".rom" ) ) {
	    infoBuf.append( "Einfache Speicherabbilddatei" );
	    int[] addrs = FileUtil.extractAddressesFromFileName( fName );
	    if( addrs != null ) {
	      if( addrs.length > 0 ) {
		begAddr = addrs[ 0 ];
		setAddr( this.fldBegAddr, begAddr );
	      }
	      if( addrs.length > 2 ) {
		this.orgStartAddr = addrs[ 2 ];
		setAddr( this.fldStartAddr, this.orgStartAddr );
	      }
	    }
	    fileFmt   = FileFormat.BIN;
	    fileBytes = FileUtil.readFile(
					file,
					false,
					MAX_MEM_FILE_SIZE );
	    if( fileBytes != null ) {
	      dataBytes = fileBytes;
	      dataLen   = fileBytes.length;
	    }
	    done = true;
	  }
	  else if( TextUtil.endsWith( fName, DiskUtil.plainDiskFileExt ) ) {
	    disk = getPlainFloppyDisk( file );
	    if( disk != null ) {
	      infoBuf.append( "Einfache Diskettenabbildddtei" );
	    }
	    done = true;
	  }
	  else if( TextUtil.endsWith( fName, DiskUtil.gzPlainDiskFileExt ) ) {
	    disk = getPlainFloppyDisk( file );
	    if( disk != null ) {
	      infoBuf.append( "Komprimierte einfache Diskettenabbildddtei" );
	    }
	    done = true;
	  }
	}

	// Diskettenabbilddatei pruefen
	if( !done ) {
	  disk = DiskUtil.readNonPlainDiskFile( this, file, true );
	  if( disk != null ) {
	    String fileFmtText = disk.getFileFormatText();
	    if( fileFmtText == null ) {
	      fileFmtText = "Diskettenabbilddatei";
	    }
	    infoBuf.append( fileFmtText );
	    if( fName.endsWith( ".gz" ) ) {
	      infoBuf.append( " (GZip-komprimiert)" );
	    }
	    this.orgRemark = strip( disk.getRemark() );
	    done           = true;
	  }
	}

	// Speicherabbilddatei pruefen
	if( !done ) {
	  if( fileBytes == null ) {
	    fileBytes = FileUtil.readFile( file, false, MAX_MEM_FILE_SIZE );
	  }
	  if( fileBytes != null ) {
	    FileInfo fileInfo = FileInfo.analyzeFile( fileBytes, file );
	    if( fileInfo != null ) {
	      fileFmt = fileInfo.getFileFormat();
	      if( fileFmt != null ) {
		infoBuf.append( fileInfo.getInfoText() );
		String fileDesc = fileInfo.getFileDesc();
		if( fileDesc != null ) {
		  if( !fileDesc.isEmpty() ) {
		    this.orgFileDesc = fileDesc;
		  }
		}
		this.orgFileTypeChar = fileInfo.getFileType();
		begAddr              = fileInfo.getBegAddr();
		if( (fileFmt.equals( FileFormat.HEADERSAVE )
					&& (this.orgFileTypeChar == 'B')
					&& (begAddr == 0x0401))
		    || fileFmt.equals( FileFormat.KCB )
		    || fileFmt.equals( FileFormat.KCB_BLKN )
		    || fileFmt.equals( FileFormat.KCB_BLKN_CKS )
		    || fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
		    || fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
		    || fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN )
		    || fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS )
		    || fileFmt.equals( FileFormat.KCBASIC_PRG ) )
		{
		  try {
		    LoadData loadData = fileInfo.createLoadData( fileBytes );
		    if( loadData != null ) {
		      // Info-Message bei KC-BASIC ignorieren!
		      kcbasicBytes = loadData.getByteArray();
		      kcbasicOffs  = loadData.getOffset();
		      kcbasicLen   = loadData.getLength();
		      if( (kcbasicBytes != null) && (kcbasicLen > 0) ) {
			this.orgIsBasicPrg = true;
		      }
		    }
		  }
		  catch( IOException ex ) {}
		}
		if( fileFmt.equals( FileFormat.KCB ) ) {
		  fileFmt = FileFormat.KCC;
		}
		else if( fileFmt.equals( FileFormat.KCB_BLKN ) ) {
		  fileFmt = FileFormat.KCC_BLKN;
		}
		else if( fileFmt.equals( FileFormat.KCB_BLKN_CKS ) ) {
		  fileFmt = FileFormat.KCC_BLKN_CKS;
		}
		if( fileFmt.equals( FileFormat.HEADERSAVE )
		    || fileFmt.equals( FileFormat.INTELHEX )
		    || fileFmt.equals( FileFormat.KCC )
		    || fileFmt.equals( FileFormat.KCC_BLKN )
		    || fileFmt.equals( FileFormat.KCC_BLKN_CKS )
		    || fileFmt.equals( FileFormat.KCTAP_SYS )
		    || fileFmt.equals( FileFormat.KCTAP_Z9001 )
		    || fileFmt.equals( FileFormat.KCTAP_KC85 ) )
		{
		  try {
		    LoadData loadData = FileInfo.createLoadData(
							fileBytes,
							fileFmt );
		    if( loadData != null ) {
		      infoMsg           = loadData.getInfoMsg();
		      dataBytes         = loadData.getByteArray();
		      dataOffs          = loadData.getOffset();
		      dataLen           = loadData.getLength();
		      begAddr           = loadData.getBegAddr();
		      this.orgStartAddr = loadData.getStartAddr();
		      setAddr( this.fldBegAddr, begAddr );
		      setAddr( this.fldStartAddr, this.orgStartAddr );
		    }
		  }
		  catch( IOException ex ) {}
		}
		if( (fileInfo.getNextTAPOffset() > 0)
		    && ((kcbasicBytes != null) || (dataBytes != null)) )
		{
		  infoMsg = "Die Quelldatei ist eine Mutli-KC-TAP-Datei."
			+ " Au\u00DFer bei\n\'"
			+ KCAudioMultiFileTarget.INFO_TEXT
			+ "\'\nwird bei allen anderen Ausgangsformaten"
			+ " nur die erste Teildatei verarbeitet.";
		}
	      }
	    }
	    done = true;
	  }
	}
	if( !done ) {
	  int pos = fName.lastIndexOf( "." );
	  if( (pos >= 0) && ((pos + 1) < fName.length()) ) {
	    infoBuf.append( fName.substring( pos + 1 ).toUpperCase() );
	    infoBuf.append( "-Datei" );
	  }
	}
	if( disk != null ) {
	  infoMsg = disk.getWarningText();
	  if( !(disk instanceof PlainDisk) ) {
	    this.targets.add( new PlainDiskFileTarget( this, disk ) );
	  }
	  if( !(disk instanceof AnaDisk) ) {
	    this.targets.add( new AnaDiskFileTarget( this, disk ) );
	  }
	  if( !(disk instanceof CopyQMDisk) ) {
	    this.targets.add( new CopyQMFileTarget( this, disk ) );
	  }
	  if( !(disk instanceof CPCDisk) ) {
	    this.targets.add( new CPCDiskFileTarget( this, disk ) );
	  }
	  if( !(disk instanceof ImageDisk) ) {
	    this.targets.add( new ImageDiskFileTarget( this, disk ) );
	  }
	  if( !(disk instanceof TeleDisk) ) {
	    this.targets.add( new TeleDiskFileTarget( this, disk ) );
	  }
	}
	if( fileFmt != null ) {
	  if( kcbasicBytes != null ) {
	    if( !fileFmt.equals( FileFormat.KCBASIC_PRG ) ) {
	      this.targets.add(
			new KCBasicFileTarget(
					this,
					kcbasicBytes,
					kcbasicOffs,
					kcbasicLen ) );
	    }
	    if( !fileFmt.equals( FileFormat.KCB ) ) {
	      this.targets.add(
			new KCBasicSystemFileTarget(
					this,
					kcbasicBytes,
					kcbasicOffs,
					kcbasicLen ) );
	    }
	    if( !fileFmt.equals( FileFormat.KCTAP_BASIC_PRG ) ) {
	      this.targets.add(
			new KCTapBasicFileTarget(
					this,
					kcbasicBytes,
					kcbasicOffs,
					kcbasicLen ) );
	    }
	    this.targets.add(
			new KCAudioFileTarget(
				this,
				kcbasicBytes,
				kcbasicOffs,
				kcbasicLen,
				KCAudioFileTarget.Target.KCBASIC_PRG ) );
	  }
	  if( dataBytes != null ) {
	    if( !fileFmt.equals( FileFormat.BIN ) ) {
	      this.targets.add(
			new BinFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					begAddr,
					(begAddr + dataLen - 1) & 0xFFFF,
					this.orgStartAddr ) );
	    }
	    if( !fileFmt.equals( FileFormat.HEADERSAVE ) ) {
	      this.targets.add(
			new HeadersaveFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    }
	    if( !fileFmt.equals( FileFormat.INTELHEX ) ) {
	      this.targets.add(
			new IntelHexFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    }
	    if( !fileFmt.equals( FileFormat.KCC ) ) {
	      this.targets.add(
			new KCSystemFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    }
	    if( !fileFmt.equals( FileFormat.KCTAP_Z9001 ) ) {
	      this.targets.add(
			new KCTapSystemFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					true ) );
	    }
	    if( !fileFmt.equals( FileFormat.KCTAP_KC85 ) ) {
	      this.targets.add(
			new KCTapSystemFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					false ) );
	    }
	    boolean basic = false;
	    if( fileFmt.equals( FileFormat.BASIC_PRG ) ) {
	      basic = true;
	    }
	    else if( fileFmt.equals( FileFormat.HEADERSAVE )
		     && (this.orgFileTypeChar == 'B') )
	    {
	      basic = true;
	    }
	    this.targets.add(
			new AC1AudioFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					basic ) );
	    this.targets.add(
			new SCCHAudioFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    this.targets.add(
			new KCAudioFileTarget(
				this,
				dataBytes,
				dataOffs,
				dataLen,
				KCAudioFileTarget.Target.Z9001 ) );
	    this.targets.add(
			new KCAudioFileTarget(
				this,
				dataBytes,
				dataOffs,
				dataLen,
				KCAudioFileTarget.Target.KC85 ) );
	    this.targets.add(
			new Z1013AudioFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					false ) );
	    this.targets.add(
			new Z1013AudioFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					true ) );
	  }
	  if( fileBytes != null ) {
	    if( FileInfo.isKCTapMagicAt( fileBytes, 0 ) ) {
	      this.targets.add(
			new KCAudioMultiFileTarget( this, fileBytes ) );
	    }
	  }
	  if( fileFmt.equals( FileFormat.CSW ) ) {
	    if( fileBytes == null ) {
	      fileBytes = FileUtil.readFile(
					file,
					false,
					MAX_TAPE_FILE_SIZE );
	    }
	    if( fileBytes != null ) {
	      BitSampleBuffer samples = CSWFile.getBitSampleBuffer(
								fileBytes,
								0 );
	      this.targets.add( new AudioFileTarget( this, samples ) );
	      this.targets.add(	new TZXFileTarget( this, samples ) );
	    }
	  }
	  if( fileFmt.equals( FileFormat.CDT )
	      || fileFmt.equals( FileFormat.TZX )
	      || fileFmt.equals( FileFormat.ZXTAP ) )
	  {
	    if( fileBytes == null ) {
	      fileBytes = FileUtil.readFile(
					file,
					false,
					MAX_TAPE_FILE_SIZE );
	    }
	    if( fileBytes != null ) {
	      BitSampleBuffer samples = new ZXSpectrumAudioCreator(
							fileBytes,
							0,
							fileBytes.length );
	      this.targets.add( new AudioFileTarget( this, samples ) );
	      this.targets.add(	new CSWFileTarget( this, samples ) );
	    }
	  }
	}
	if( this.targets.size() > 0 ) {
	  try {
	    Collections.sort( this.targets );
	  }
	  catch( ClassCastException ex ) {}
	  this.listTarget.setListData( this.targets );
	  int rowToSelect = 0;
	  if( this.lastSavedTargetText != null ) {
	    int n = this.targets.size();
	    for( int i = 0; i < n; i++ ) {
	      String s = this.targets.get( i ).toString();
	      if( s != null ) {
		if( s.equals( this.lastSavedTargetText ) ) {
		  rowToSelect = i;
		  break;
		}
	      }
	    }
	  }
	  EmuUtil.fireSelectRow( this.listTarget, rowToSelect );
	} else {
	  if( infoBuf.length() == 0 ) {
	    infoBuf.append( "unbekannt" );
	  }
	  infoBuf.append( " (nicht konvertierbar)" );
	}
	this.fldSrcFile.setFile( file );
	this.fldSrcInfo.setText( infoBuf.toString() );
	this.btnSrcRemove.setEnabled( true );
	Main.setLastFile( file, Main.FILE_GROUP_FC_IN );
	if( infoMsg != null ) {
	  BaseDlg.showInfoDlg( this, infoMsg );
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !FileUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    final File file = FileUtil.fileDrop( this, e );
    if( file != null ) {
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    openFile( file );
		  }
		} );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.listTarget ) {
      targetSelectionChanged();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object src = e.getSource();
      if( src == this.btnSrcSelect ) {
	rv = true;
	doSelectSrcFile();
      }
      if( src == this.btnSrcRemove ) {
	rv = true;
	doRemoveSrcFile();
      }
      else if( src == this.btnConvert ) {
	rv = true;
	doSave();
      }
      else if( src == this.btnPlay ) {
	rv = true;
	doPlay();
      }
      else if( src == this.btnClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.btnHelp ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
      }
    }
    catch( UserInputException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( Main.isTopFrm( this ) ) {
      rv = EmuUtil.closeOtherFrames( this );
      if( rv ) {
	rv = super.doClose();
      }
      if( rv ) {
	Main.exitSuccess();
      }
    } else {
      rv = super.doClose();
    }
    if( rv ) {
      // damit beim erneuten Oeffnen das Fenster leer ist
      doRemoveSrcFile();
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.btnSrcSelect.requestFocus();
  }


	/* --- Aktionen --- */

  private void doPlay() throws UserInputException
  {
    AbstractConvertTarget target = this.listTarget.getSelectedValue();
    if( target != null ) {
      if( target.canPlay() ) {
	if( checkFileDesc( target ) ) {
	  try {
	    AudioPlayFrm.open(
			target.createPCMDataSource(),
			"Wiedergabe..." );
	  }
	  catch( IOException ex ) {
	    BaseDlg.showErrorDlg( this, ex );
	  }
	  catch( UserInputException ex ) {
	    BaseDlg.showErrorDlg( this, ex );
	  }
	}
      }
    }
  }


  private void doRemoveSrcFile()
  {
    this.fldSrcFile.setFile( null );
    this.fldSrcInfo.setText( "" );
    this.btnSrcRemove.setEnabled( false );
    clearOutFields();
  }


  private void doSelectSrcFile()
  {
    File file = this.fldSrcFile.getFile();
    if( file != null ) {
      file = file.getParentFile();
    }
    if( file == null ) {
      file = Main.getLastDirFile( Main.FILE_GROUP_FC_IN );
    }
    file = FileUtil.showFileOpenDlg(
			this,
			"Quelldatei ausw\u00E4hlen",
			FileUtil.getDirectory( file ),
			AudioFile.getFileFilter(),
			FileUtil.getBinaryFileFilter(),
			FileUtil.getHeadersaveFileFilter(),
			FileUtil.getHexFileFilter(),
			FileUtil.getKCBasicFileFilter(),
			FileUtil.getKCSystemFileFilter(),
			FileUtil.getTapeFileFilter(),
			FileUtil.getPlainDiskFileFilter(),
			FileUtil.getAnaDiskFileFilter(),
			FileUtil.getCopyQMFileFilter(),
			FileUtil.getDskFileFilter(),
			FileUtil.getImageDiskFileFilter(),
			FileUtil.getTeleDiskFileFilter() );
    if( file != null ) {
      openFile( file );
    }
  }


  private void doSave() throws UserInputException
  {
    AbstractConvertTarget target = this.listTarget.getSelectedValue();
    if( (target != null) && checkFileDesc( target ) ) {
      File inFile  = this.fldSrcFile.getFile();
      File outFile = target.getSuggestedOutFile( inFile );
      if( (outFile != null) && !this.lastOutDirAsInDir ) {
	File dirFile = Main.getLastDirFile( Main.FILE_GROUP_FC_OUT );
	if( (dirFile != null) && (outFile != null) ) {
	  String fName = outFile.getName();
	  if( fName != null ) {
	    if( !fName.isEmpty() ) {
	      outFile = new File( dirFile, fName );
	    }
	  }
	}
      }
      outFile = FileUtil.showFileSaveDlg(
				this,
				"Konvertierte Datei speichern",
				outFile,
				target.getFileFilters() );
      if( outFile != null ) {
	try {
	  String logText = target.save( outFile );
	  Main.setLastFile( outFile, Main.FILE_GROUP_FC_OUT );
	  this.lastOutDirAsInDir = false;
	  if( inFile != null ) {
	    File inDir  = inFile.getParentFile();
	    File outDir = outFile.getParentFile();
	    if( (inDir != null) && (outDir != null) ) {
	      this.lastOutDirAsInDir = outDir.equals( inDir );
	    } else {
	      if( (inDir == null) && (outDir == null) ) {
		this.lastOutDirAsInDir = true;
	      }
	    }
	  }
	  if( logText != null ) {
	    if( !logText.isEmpty() ) {
	      LogDlg.showDlg(
			this,
			logText,
			"Hinweise",
			FileUtil.replaceExtension( outFile, "log" ) );
	    }
	  }
	  this.lastSavedTargetText = target.toString();
	}
	catch( IOException ex ) {
	  BaseDlg.showErrorDlg( this, ex );
	}
	catch( UserInputException ex ) {
	  BaseDlg.showErrorDlg( this, ex );
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private FileConvertFrm()
  {
    this.orgFileDesc         = null;
    this.orgFileTypeChar     = -1;
    this.orgStartAddr        = -1;
    this.orgRemark           = null;
    this.orgIsBasicPrg       = false;
    this.lastOutDirAsInDir   = false;
    this.lastSavedTargetText = null;
    setTitle( TITLE );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Bereich Quelldatei
    JPanel panelSrc = GUIFactory.createPanel( new GridBagLayout() );
    add( panelSrc, gbc );

    panelSrc.setBorder( GUIFactory.createTitledBorder( "Quelldatei" ) );

    GridBagConstraints gbcSrc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelSrc.add( GUIFactory.createLabel( "Datei:" ), gbcSrc );

    this.fldSrcFile    = new FileNameFld();
    gbcSrc.weightx     = 1.0;
    gbcSrc.fill        = GridBagConstraints.HORIZONTAL;
    gbcSrc.insets.left = 0;
    gbcSrc.gridx++;
    panelSrc.add( this.fldSrcFile, gbcSrc );

    this.btnSrcSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					"Quelldatei ausw\u00E4hlen" );
    gbcSrc.weightx = 0.0;
    gbcSrc.fill    = GridBagConstraints.NONE;
    gbcSrc.gridx++;
    panelSrc.add( this.btnSrcSelect, gbcSrc );

    this.btnSrcRemove = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					"Quelldatei entfernen" );
    this.btnSrcRemove.setEnabled( false );
    gbcSrc.gridx++;
    panelSrc.add( this.btnSrcRemove, gbcSrc );

    gbcSrc.insets.left = 5;
    gbcSrc.gridx       = 0;
    gbcSrc.gridy++;
    panelSrc.add( GUIFactory.createLabel( "Typ:" ), gbcSrc );

    this.fldSrcInfo = GUIFactory.createTextField();
    this.fldSrcInfo.setEditable( false );
    gbcSrc.weightx     = 1.0;
    gbcSrc.fill        = GridBagConstraints.HORIZONTAL;
    gbcSrc.insets.left = 0;
    gbcSrc.gridwidth   = GridBagConstraints.REMAINDER;
    gbcSrc.gridx++;
    panelSrc.add( this.fldSrcInfo, gbcSrc );


    // Bereich Ausgabedatei
    JPanel panelOut = GUIFactory.createPanel( new GridBagLayout() );
    gbc.fill        = GridBagConstraints.BOTH;
    gbc.weighty     = 1.0;
    gbc.gridy++;
    add( panelOut, gbc );

    panelOut.setBorder( GUIFactory.createTitledBorder( "Ausgabedatei" ) );

    GridBagConstraints gbcOut = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelOut.add( GUIFactory.createLabel( "Dateiformat:" ), gbcOut );

    this.targets    = new Vector<>();
    this.listTarget = GUIFactory.createList();
    this.listTarget.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    gbcOut.fill    = GridBagConstraints.BOTH;
    gbcOut.weightx = 1.0;
    gbcOut.weighty = 1.0;
    gbcOut.gridy++;
    panelOut.add( GUIFactory.createScrollPane( this.listTarget ), gbcOut );

    gbcOut.fill       = GridBagConstraints.NONE;
    gbcOut.weightx    = 0.0;
    gbcOut.weighty    = 0.0;
    gbcOut.insets.top = 15;
    gbcOut.gridy++;
    panelOut.add( GUIFactory.createLabel( "Kopfdaten:" ), gbcOut );

    JPanel panelHead = GUIFactory.createPanel( new GridBagLayout() );
    panelHead.setBorder( BorderFactory.createEtchedBorder() );
    gbcOut.fill          = GridBagConstraints.HORIZONTAL;
    gbcOut.weightx       = 1.0;
    gbcOut.insets.top    = 0;
    gbcOut.insets.bottom = 5;
    gbcOut.gridy++;
    panelOut.add( panelHead, gbcOut );

    GridBagConstraints gbcHead = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelFileDesc = GUIFactory.createLabel( "Bezeichnung:" );
    panelHead.add( this.labelFileDesc, gbcHead );

    this.docFileDesc  = new LimitedDocument();
    this.fldFileDesc  = GUIFactory.createTextField( this.docFileDesc, 0 );
    gbcHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx   = 1.0;
    gbcHead.gridwidth = 3;
    gbcHead.gridx++;
    panelHead.add( this.fldFileDesc, gbcHead );

    this.labelFileType = GUIFactory.createLabel( "Typ:" );
    gbcHead.fill       = GridBagConstraints.NONE;
    gbcHead.weightx    = 0.0;
    gbcHead.gridwidth  = 1;
    gbcHead.gridx      = 0;
    gbcHead.gridy++;
    panelHead.add( this.labelFileType, gbcHead );

    this.comboFileType = GUIFactory.createComboBox();
    this.comboFileType.setEditable( true );
    gbcHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx   = 1.0;
    gbcHead.gridwidth = 3;
    gbcHead.gridx++;
    panelHead.add( this.comboFileType, gbcHead );

    this.docFileType = new LimitedDocument();
    this.docFileType.setAsciiOnly( true );
    setFileTypeDocument();

    Font font = this.fldFileDesc.getFont();
    if( font != null ) {
      this.comboFileType.setFont( font );
    }

    this.labelBegAddr = GUIFactory.createLabel( LABEL_BEG_ADDR );
    gbcHead.fill      = GridBagConstraints.NONE;
    gbcHead.weightx   = 0.0;
    gbcHead.gridwidth = 1;
    gbcHead.gridx     = 0;
    gbcHead.gridy++;
    panelHead.add( this.labelBegAddr, gbcHead );

    this.docBegAddr = new HexDocument( 4, LABEL_BEG_ADDR );
    this.fldBegAddr = GUIFactory.createTextField( this.docBegAddr, 0 );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldBegAddr, gbcHead );

    this.labelStartAddr = GUIFactory.createLabel( LABEL_START_ADDR );
    gbcHead.fill        = GridBagConstraints.NONE;
    gbcHead.weightx     = 0.0;
    gbcHead.gridx++;
    panelHead.add( this.labelStartAddr, gbcHead );

    this.docStartAddr   = new HexDocument( 4, LABEL_START_ADDR );
    this.fldStartAddr   = GUIFactory.createTextField( this.docStartAddr, 0 );
    gbcHead.fill        = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx     = 0.5;
    gbcHead.insets.left = 0;
    gbcHead.gridx++;
    panelHead.add( this.fldStartAddr, gbcHead );

    this.labelRemark      = GUIFactory.createLabel( "Kommentar:" );
    gbcHead.fill          = GridBagConstraints.NONE;
    gbcHead.weightx       = 0.0;
    gbcHead.insets.left   = 5;
    gbcHead.insets.bottom = 5;
    gbcHead.gridx         = 0;
    gbcHead.gridy++;
    panelHead.add( this.labelRemark, gbcHead );

    this.docRemark    = new LimitedDocument();
    this.fldRemark    = GUIFactory.createTextField( this.docRemark, 0 );
    gbcHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx   = 1.0;
    gbcHead.gridwidth = 3;
    gbcHead.gridx++;
    panelHead.add( this.fldRemark, gbcHead );

    JPanel panelOutFile = GUIFactory.createPanel( new GridBagLayout() );
    gbcOut.fill         = GridBagConstraints.HORIZONTAL;
    gbcOut.weightx      = 1.0;
    gbcOut.gridwidth    = GridBagConstraints.REMAINDER;
    gbcOut.gridx        = 0;
    gbcOut.gridy++;
    panelOut.add( panelOutFile, gbcOut );

    GridBagConstraints gbcOutFile = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 4, 5, 5 ) );
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnConvert = GUIFactory.createButton( "Konvertieren" );
    panelBtn.add( this.btnConvert );

    this.btnPlay = GUIFactory.createButton( "Wiedergeben" );
    panelBtn.add( this.btnPlay );

    this.btnHelp = GUIFactory.createButtonHelp();
    panelBtn.add( this.btnHelp );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( this.btnClose );


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      this.listTarget.setVisibleRowCount( 7 );
      pack();
      this.listTarget.setVisibleRowCount( 1 );
      setScreenCentered();
    }


    // Listener
    this.btnSrcSelect.addActionListener( this );
    this.btnSrcRemove.addActionListener( this );
    this.listTarget.addListSelectionListener( this );
    this.btnConvert.addActionListener( this );
    this.btnPlay.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnClose.addActionListener( this );


    // sonstiges
    (new DropTarget( this.fldSrcFile, this )).setActive( true );
    clearOutFields();
  }


	/* --- private Methoden --- */

  private void clearOutFields()
  {
    this.targets.clear();
    this.listTarget.setListData( this.targets );

    this.labelFileDesc.setEnabled( false );
    this.labelFileType.setEnabled( false );
    this.labelBegAddr.setEnabled( false );
    this.labelStartAddr.setEnabled( false );
    this.labelRemark.setEnabled( false );

    this.fldFileDesc.setText( "" );
    this.comboFileType.setSelectedItem( "" );
    this.fldBegAddr.setText( "" );
    this.fldStartAddr.setText( "" );
    this.fldRemark.setText( "" );

    this.fldFileDesc.setEnabled( false );
    this.comboFileType.setEnabled( false );
    this.fldBegAddr.setEnabled( false );
    this.fldStartAddr.setEnabled( false );
    this.fldRemark.setEnabled( false );

    this.btnConvert.setEnabled( false );
    this.btnPlay.setEnabled( false );
  }


  private boolean checkFileDesc( AbstractConvertTarget target )
					throws UserInputException
  {
    boolean rv = false;
    if( target != null ) {
      int maxFileDescLen = target.getMaxFileDescLength();
      if( maxFileDescLen > 0 ) {
	rv = FileUtil.checkFileDesc( this, getFileDesc(), maxFileDescLen );
      } else {
	/*
	 * Wenn das Zielformat keine Bezeichnung unterstuetzt
	 * und somit das entsprechende Eingabefeld gedimmt ist,
	 * soll kein Fehler erscheinen,
	 * wenn dort schon als Vorbelegung etwas drin steht.
	 */
	rv = true;
      }
    }
    return rv;
  }


  private int getAddr( HexDocument hexDoc ) throws UserInputException
  {
    int rv = -1;
    try {
      Integer value = hexDoc.getInteger();
      if( value != null ) {
	rv = value.intValue() & 0xFFFF;
      }
    }
    catch( NumberFormatException ex ) {
      throw new UserInputException( ex.getMessage() );
    }
    return rv;
  }


  private AbstractFloppyDisk getPlainFloppyDisk( File file )
						throws IOException
  {
    AbstractFloppyDisk disk = null;
    byte[] fileBytes = FileUtil.readFile( file, false, MAX_DISK_FILE_SIZE );
    if( fileBytes != null ) {
      FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			true,
			FloppyDiskFormat.getFormatByDiskSize( file.length() ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
      dlg.setVisible( true );
      FloppyDiskFormat fmt = dlg.getFormat();
      if( fmt != null ) {
	disk = PlainDisk.createForByteArray(
					this,
					file.getPath(),
					fileBytes,
					fmt );
      }
    }
    return disk;
  }


  private void setAddr( JTextField textFld, int addr )
  {
    if( addr >= 0 ) {
      textFld.setText( String.format( "%04X", addr ) );
    } else {
      textFld.setText( "" );
    }
  }


  private void setFileTypeDocument()
  {
    ComboBoxEditor editor = this.comboFileType.getEditor();
    if( editor != null ) {
      Component c = editor.getEditorComponent();
      if( c != null ) {
	if( c instanceof JTextComponent ) {
	  ((JTextComponent) c).setDocument( this.docFileType );
	}
      }
    }
  }


  private static String strip( String text )
  {
    if( text != null ) {
      text = text.trim();
      if( text.isEmpty() ) {
	text = null;
      }
    }
    return text;
  }


  private void targetSelectionChanged()
  {
    AbstractConvertTarget target = this.listTarget.getSelectedValue();
    if( target != null ) {
      this.labelBegAddr.setEnabled( target.usesBegAddr() );
      this.fldBegAddr.setEnabled( target.usesBegAddr() );

      int    oldMaxLen = this.docFileDesc.getMaxLength();
      String fileDesc  = this.fldFileDesc.getText();
      if( fileDesc == null ) {
	fileDesc = "";
      }
      int oldLen = fileDesc.length();
      int maxLen = target.getMaxFileDescLength();
      this.docFileDesc.setMaxLength( maxLen );
      this.labelFileDesc.setEnabled( maxLen > 0 );
      this.fldFileDesc.setEnabled( maxLen > 0 );

      if( this.orgFileDesc == null ) {
	this.orgFileDesc = "";
      }
      if( oldLen > 0 ) {
	/*
	 * Da sich die maximale Laenge geaendert haben kann,
	 * muss das Feld gesetzt werden.
	 * Allerdings wir der alte Text (bis zur. max. Laenge) beibehalten,
	 * wenn es sich vom urspruenglichen Wert unterscheidet
	 * und somit durch den Anwender geaendert wurde.
	 */
	if( ((oldLen < oldMaxLen) && this.orgFileDesc.equals( fileDesc ))
	    || ((oldLen == oldMaxLen)
		&& this.orgFileDesc.startsWith( fileDesc )) )
	{
	  fileDesc = this.orgFileDesc;
	}
      } else {
	fileDesc = this.orgFileDesc;
      }
      this.fldFileDesc.setText( fileDesc );

      maxLen = target.getMaxFileTypeLength();
      this.docFileType.setMaxLength( maxLen );
      target.setFileTypesTo( this.comboFileType );
      this.labelFileType.setEnabled( this.comboFileType.isEnabled() );

      if( (this.orgFileDesc != null) && (getOrgFileTypeChar() < 0) ) {
	Object typeItem = this.comboFileType.getSelectedItem();
	if( typeItem != null ) {
	  String typeText = typeItem.toString();
	  if( typeText != null ) {
	    String upperExt = "." + typeText.toUpperCase();
	    if( this.orgFileDesc.toUpperCase().endsWith( upperExt ) ) {
	      int len = this.orgFileDesc.length() - upperExt.length();
	      if( len > 0 ) {
		this.fldFileDesc.setText(
			this.orgFileDesc.substring( 0, len ) );
	      }
	    }
	  }
	}
      }

      maxLen = target.getMaxRemarkLength();
      this.docRemark.setMaxLength( maxLen );
      this.labelRemark.setEnabled( maxLen > 0 );
      this.fldRemark.setEnabled( maxLen > 0 );
      this.fldRemark.setText( this.orgRemark );

      this.btnConvert.setEnabled( target != null );
      this.btnPlay.setEnabled( target.canPlay() );

    } else {

      this.labelBegAddr.setEnabled( false );
      this.fldBegAddr.setEnabled( false );

      this.labelFileDesc.setEnabled( false );
      this.docFileDesc.setMaxLength( 0 );
      this.fldFileDesc.setEnabled( false );
      this.fldFileDesc.setText( this.orgFileDesc );

      this.labelFileType.setEnabled( false );
      this.comboFileType.setEnabled( false );
      this.comboFileType.removeAllItems();

      this.labelRemark.setEnabled( false );
      this.docRemark.setMaxLength( 0 );
      this.fldRemark.setEnabled( false );
      this.fldRemark.setText( this.orgRemark );

      this.btnConvert.setEnabled( false );
      this.btnPlay.setEnabled( false );
    }

    // Startadresse
    boolean state = false;
    if( target != null ) {
      try {
	state = target.usesStartAddr( getFileTypeChar( false ) );
      }
      catch( UserInputException ex ) {}
    }
    this.labelStartAddr.setEnabled( state );
    this.fldStartAddr.setEnabled( state );
    this.fldStartAddr.setEditable( state );
  }
}
