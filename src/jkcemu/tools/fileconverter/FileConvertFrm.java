/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateikonverter
 */

package jkcemu.tools.fileconverter;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.audio.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.text.TextUtil;


public class FileConvertFrm extends BasicFrm implements
						DropTargetListener,
						ListSelectionListener
{
  private static final int MAX_MEM_FILE_SIZE  = 0x40000;	// 256 KByte
  private static final int MAX_DISK_FILE_SIZE = 0x200000;	// 2 MByte

  private static final String LABEL_BEG_ADDR   = "Anfangsadresse:";
  private static final String LABEL_START_ADDR = "Startadresse:";

  private static FileConvertFrm instance = null;

  private String                        orgFileDesc;
  private int                           orgFileType;
  private int                           orgStartAddr;
  private boolean                       orgIsBasicPrg;
  private FileNameFld                   fldSrcFile;
  private JButton                       btnSrcSelect;
  private JButton                       btnSrcRemove;
  private JTextField                    fldSrcInfo;
  private Vector<AbstractConvertTarget> targets;
  private JList                         listTarget;
  private JLabel                        labelBegAddr;
  private JLabel                        labelStartAddr;
  private JLabel                        labelFileType;
  private JLabel                        labelFileDesc;
  private JTextField                    fldBegAddr;
  private JTextField                    fldStartAddr;
  private JTextField                    fldFileDesc;
  private JComboBox                     comboFileType;
  private HexDocument                   docBegAddr;
  private HexDocument                   docStartAddr;
  private LimitedDocument               docFileDesc;
  private JButton                       btnConvert;
  private JButton                       btnPlay;
  private JButton                       btnHelp;
  private JButton                       btnClose;


  public static void open()
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
  }


  public static void open( File file )
  {
    open();
    if( file != null ) {
      instance.openFile( file );
    }
  }



  public boolean getOrgIsBasicPrg()
  {
    return this.orgIsBasicPrg;
  }


  public int getOrgFileType()
  {
    return this.orgFileType;
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


  public String getFileDesc( boolean mandatory ) throws UserInputException
  {
    String s = this.fldFileDesc.getText();
    if( s != null ) {
      s = s.trim();
      if( s.isEmpty() ) {
	s = null;
      }
    }
    if( (s == null) && mandatory ) {
      throw new UserInputException( "Beschreibung nicht angegeben" );
    }
    return s;
  }


  public int getFileType( boolean mandatory ) throws UserInputException
  {
    int    rv = -1;
    Object o  = this.comboFileType.getSelectedItem();
    if( o != null ) {
      String s = o.toString();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  rv = s.charAt( 0 );
	  if( (rv <= 0x20) || (rv >= 0x7F) ) {
	    rv = -1;
	  }
	}
      }
    }
    if( (rv < 0) && mandatory ) {
      throw new UserInputException(
			"Dateityp nicht angegeben oder ung\u00FCltig" );
    }
    return rv;
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
	this.orgFileDesc   = null;
	this.orgFileType   = -1;
	this.orgStartAddr  = -1;
	this.orgIsBasicPrg = false;

	String             infoMsg       = null;
	AbstractFloppyDisk disk          = null;
	int                begAddr       = -1;
	int                kcbasicOffs   = 0;
	int                kcbasicLen    = 0;
	int                dataOffs      = 0;
	int                dataLen       = 0;
	byte[]             dataBytes     = null;
	byte[]             kcbasicBytes  = null;
	String             fileFmt       = null;
	boolean            multiTAP      = false;

	// Dateiname
	String fExt  = null;
	String fName = file.getName();
	if( fName != null ) {
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
	AudioInputStream ais = null;
	try {
	  ais = AudioSystem.getAudioInputStream( file );
	  if( ais != null ) {
	    infoBuf.append( "Sound-Datei" );
	    AudioFormat auFmt = ais.getFormat();
	    if( auFmt != null ) {
	      infoBuf.append( ", " );
	      AudioUtil.appendAudioFormatText( infoBuf, auFmt );
	    }

	    /*
	     * Dateitypen ermitteln, in die konverttiert werden kann,
	     * den eigenen Typ dabeo ausblenden
	     */
	    String[] extensions = AudioUtil.getAudioOutFileExtensions(
							      ais,
							      fExt );
	    if( extensions != null ) {
	      StringBuilder buf = new StringBuilder( 256 );
	      AudioUtil.appendAudioFileExtensionText( buf, 3, extensions );
	      this.targets.add(
			new AudioFileTarget(
					this,
					file,
					extensions,
					buf.toString() ) );
	    }
	    done = true;
	  }
	}
	catch( UnsupportedAudioFileException ex1 ) {}
	catch( IOException ex2 ) {}
	finally {
	  EmuUtil.doClose( ais );
	}

	// Dateiextension pruefen
	if( !done && (fName != null) ) {
	  if( fName.endsWith( ".bin" ) || fName.endsWith( ".rom" ) ) {
	    infoBuf.append( "Einfache Speicherabbilddatei" );
	    int[] addrs = EmuUtil.extractAddressesFromFileName( fName );
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
	    fileFmt          = FileInfo.BIN;
	    byte[] fileBytes = EmuUtil.readFile( file, MAX_MEM_FILE_SIZE );
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
	  disk = DiskUtil.readNonPlainDiskFile( this, file );
	  if( disk != null ) {
	    String fileFmtText = disk.getFileFormatText();
	    if( fileFmtText == null ) {
	      fileFmtText = "Diskettenabbilddatei";
	    }
	    infoBuf.append( fileFmtText );
	    if( fName.endsWith( ".gz" ) ) {
	      infoBuf.append( " (GZip-komprimiert)" );
	    }
	    done = true;
	  }
	}

	// Speicherabbilddatei pruefen
	if( !done ) {
	  FileInfo fileInfo = FileInfo.analyzeFile( file );
	  if( fileInfo != null ) {
	    fileFmt = fileInfo.getFileFormat();
	    if( fileFmt != null ) {
	      infoBuf.append( fileInfo.getInfoText() );
	      this.orgFileDesc = fileInfo.getFileDesc();
	      this.orgFileType = fileInfo.getFileType();
	      begAddr          = fileInfo.getBegAddr();
	      if( (fileFmt.equals( FileInfo.HEADERSAVE )
					&& (this.orgFileType == 'B')
					&& (begAddr == 0x0401))
		  || fileFmt.equals( FileInfo.KCB )
		  || fileFmt.equals( FileInfo.KCTAP_BASIC_PRG )
		  || fileFmt.equals( FileInfo.KCBASIC_HEAD_PRG )
		  || fileFmt.equals( FileInfo.KCBASIC_PRG ) )
	      {
		byte[] fileBytes = EmuUtil.readFile( file, MAX_MEM_FILE_SIZE );
		if( fileBytes != null ) {
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
	      }
	      if( fileFmt.equals( FileInfo.KCB ) ) {
		fileFmt = FileInfo.KCC;
	      }
	      if( fileFmt.equals( FileInfo.HEADERSAVE )
		  || fileFmt.equals( FileInfo.INTELHEX )
		  || fileFmt.equals( FileInfo.KCC )
		  || fileFmt.equals( FileInfo.KCTAP_SYS )
		  || fileFmt.equals( FileInfo.KCTAP_Z9001 )
		  || fileFmt.equals( FileInfo.KCTAP_KC85 ) )
	      {
		byte[] fileBytes = EmuUtil.readFile( file, MAX_MEM_FILE_SIZE );
		if( fileBytes != null ) {
		  try {
		    LoadData loadData = fileInfo.createLoadData(
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
	      }
	      if( (fileInfo.getNextTAPOffset() > 0)
		  && ((kcbasicBytes != null) || (dataBytes != null)) )
	      {
		infoMsg = "Die Quelldatei ist eine Mutli-TAP-Datei.\n"
				+ "Es wird nur die erste Teildatei"
				+ " konvertiert.";
	      }
	    }
	    done = true;
	  }
	}
	if( (this.orgFileDesc == null) && (fName != null) ) {
	  this.orgFileDesc = fName.toUpperCase();
	  int pos = this.orgFileDesc.indexOf( '.' );
	  if( pos > 0 ) {
	    this.orgFileDesc = this.orgFileDesc.substring( 0, pos );
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
	  if( !(disk instanceof CPCDisk) ) {
	    this.targets.add( new CPCDiskFileTarget( this, disk ) );
	  }
	  if( !(disk instanceof ImageDisk) ) {
	    this.targets.add( new ImageDiskFileTarget( this, disk ) );
	  }
	}
	if( fileFmt != null ) {
	  if( kcbasicBytes != null ) {
	    if( !fileFmt.equals( FileInfo.KCBASIC_PRG ) ) {
	      this.targets.add(
			new KCBasicFileTarget(
					this,
					kcbasicBytes,
					kcbasicOffs,
					kcbasicLen ) );
	    }
	    if( !fileFmt.equals( FileInfo.KCB ) ) {
	      this.targets.add(
			new KCBasicSystemFileTarget(
					this,
					kcbasicBytes,
					kcbasicOffs,
					kcbasicLen ) );
	    }
	    if( !fileFmt.equals( FileInfo.KCTAP_BASIC_PRG ) ) {
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
	    if( !fileFmt.equals( FileInfo.BIN ) ) {
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
	    if( !fileFmt.equals( FileInfo.HEADERSAVE ) ) {
	      this.targets.add(
			new HeadersaveFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    }
	    if( !fileFmt.equals( FileInfo.INTELHEX ) ) {
	      this.targets.add(
			new IntelHexFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    }
	    if( !fileFmt.equals( FileInfo.KCC ) ) {
	      this.targets.add(
			new KCSystemFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen ) );
	    }
	    if( !fileFmt.equals( FileInfo.KCTAP_Z9001 ) ) {
	      this.targets.add(
			new KCTapSystemFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					true ) );
	    }
	    if( !fileFmt.equals( FileInfo.KCTAP_KC85 ) ) {
	      this.targets.add(
			new KCTapSystemFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					false ) );
	    }
	    this.targets.add(
			new AC1AudioFileTarget(
					this,
					dataBytes,
					dataOffs,
					dataLen,
					false ) );
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
	}
	if( this.targets.size() > 0 ) {
	  try {
	    Collections.sort( this.targets );
	  }
	  catch( ClassCastException ex ) {}
	  this.listTarget.setListData( this.targets );
	  EmuUtil.fireSelectRow( this.listTarget, 0 );
	} else {
	  if( infoBuf.length() == 0 ) {
	    infoBuf.append( "unbekannt" );
	  }
	  infoBuf.append( " (nicht konvertierbar)" );
	}
	this.fldSrcFile.setFile( file );
	this.fldSrcInfo.setText( infoBuf.toString() );
	this.btnSrcRemove.setEnabled( true );
	Main.setLastFile( file, "fileconverter.in" );
	if( infoMsg != null ) {
	  BasicDlg.showInfoDlg( this, infoMsg );
	}
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // empty
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = EmuUtil.fileDrop( this, e );
    if( file != null )
      openFile( file );
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
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
    if( e != null ) {
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
	HelpFrm.open( "/help/tools/fileconverter.htm" );
      }
      else if( src == this.comboFileType ) {
	rv = true;
	updStartAddrEnabled();
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      Main.checkQuit( this );
    } else {
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

  private void doPlay()
  {
    AbstractConvertTarget target = getSelectedTarget();
    if( target != null ) {
      if( target.canPlay() ) {
	try {
	  AudioPlayer.play(
			this,
			target.getAudioInputStream(),
			"Wiedergabe..." );
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg( this, ex );
	}
	catch( UserInputException ex ) {
	  BasicDlg.showErrorDlg( this, ex );
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
      file = Main.getLastPathFile( "fileconverter.in" );
    }
    file = EmuUtil.showFileOpenDlg(
			this,
			"Quelldatei ausw\u00E4hlen",
			file,
			EmuUtil.getBinaryFileFilter(),
			EmuUtil.getHeadersaveFileFilter(),
			EmuUtil.getHexFileFilter(),
			EmuUtil.getKCBasicFileFilter(),
			EmuUtil.getKCSystemFileFilter(),
			EmuUtil.getTapFileFilter(),
			EmuUtil.getPlainDiskFileFilter(),
			EmuUtil.getAnaDiskFileFilter(),
			EmuUtil.getCopyQMFileFilter(),
			EmuUtil.getDskFileFilter(),
			EmuUtil.getImageDiskFileFilter(),
			EmuUtil.getTeleDiskFileFilter() );
    if( file != null ) {
      openFile( file );
    }
  }


  private void doSave()
  {
    AbstractConvertTarget target = getSelectedTarget();
    if( target != null ) {
      File file = target.getSuggestedOutFile( this.fldSrcFile.getFile() );
      if( file != null ) {
	File dirFile = Main.getLastPathFile( "fileconverter.out" );
	if( (dirFile != null) && (file != null) ) {
	  String fName = file.getName();
	  if( fName != null ) {
	    if( !fName.isEmpty() ) {
	      file = new File( dirFile, fName );
	    }
	  }
	}
      }
      file = EmuUtil.showFileSaveDlg(
				this,
				"Konvertierte Datei speichern",
				file,
				target.getFileFilters() );
      if( file != null ) {
	try {
	  target.save( file );
	  Main.setLastFile( file, "fileconverter.out" );
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg( this, ex );
	}
	catch( UserInputException ex ) {
	  BasicDlg.showErrorDlg( this, ex );
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private FileConvertFrm()
  {
    this.orgFileDesc   = null;
    this.orgFileType   = -1;
    this.orgStartAddr  = -1;
    this.orgIsBasicPrg = false;
    setTitle( "JKCEMU Dateikonverter" );
    Main.updIcon( this );


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
    JPanel panelSrc = new JPanel( new GridBagLayout() );
    add( panelSrc, gbc );

    panelSrc.setBorder( BorderFactory.createTitledBorder( "Quelldatei" ) );

    GridBagConstraints gbcSrc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelSrc.add( new JLabel( "Datei:" ), gbcSrc );

    this.fldSrcFile    = new FileNameFld();
    gbcSrc.weightx     = 1.0;
    gbcSrc.fill        = GridBagConstraints.HORIZONTAL;
    gbcSrc.insets.left = 0;
    gbcSrc.gridx++;
    panelSrc.add( this.fldSrcFile, gbcSrc );

    this.btnSrcSelect = createImageButton(
				"/images/file/open.png",
				"Quelldatei ausw\u00E4hlen" );
    gbcSrc.weightx = 0.0;
    gbcSrc.fill    = GridBagConstraints.NONE;
    gbcSrc.gridx++;
    panelSrc.add( this.btnSrcSelect, gbcSrc );

    this.btnSrcRemove = createImageButton(
				"/images/file/delete.png",
				"Quelldatei entfernen" );
    this.btnSrcRemove.setEnabled( false );
    gbcSrc.gridx++;
    panelSrc.add( this.btnSrcRemove, gbcSrc );

    gbcSrc.insets.left = 5;
    gbcSrc.gridx       = 0;
    gbcSrc.gridy++;
    panelSrc.add( new JLabel( "Typ:" ), gbcSrc );

    this.fldSrcInfo = new JTextField();
    this.fldSrcInfo.setEditable( false );
    gbcSrc.weightx     = 1.0;
    gbcSrc.fill        = GridBagConstraints.HORIZONTAL;
    gbcSrc.insets.left = 0;
    gbcSrc.gridwidth   = GridBagConstraints.REMAINDER;
    gbcSrc.gridx++;
    panelSrc.add( this.fldSrcInfo, gbcSrc );


    // Bereich Ausgabedatei
    JPanel panelOut = new JPanel( new GridBagLayout() );
    gbc.fill        = GridBagConstraints.BOTH;
    gbc.weighty     = 1.0;
    gbc.gridy++;
    add( panelOut, gbc );

    panelOut.setBorder( BorderFactory.createTitledBorder( "Ausgabedatei" ) );

    GridBagConstraints gbcOut = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelOut.add( new JLabel( "Dateiformat:" ), gbcOut );

    this.targets = new Vector<AbstractConvertTarget>();

    this.listTarget = new JList();
    this.listTarget.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    this.listTarget.addListSelectionListener( this );
    gbcOut.fill    = GridBagConstraints.BOTH;
    gbcOut.weightx = 1.0;
    gbcOut.weighty = 1.0;
    gbcOut.gridy++;
    panelOut.add( new JScrollPane( this.listTarget ), gbcOut );

    gbcOut.fill       = GridBagConstraints.NONE;
    gbcOut.weightx    = 0.0;
    gbcOut.weighty    = 0.0;
    gbcOut.insets.top = 15;
    gbcOut.gridy++;
    panelOut.add( new JLabel( "Kopfdaten:" ), gbcOut );

    JPanel panelHead = new JPanel( new GridBagLayout() );
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

    this.labelFileType = new JLabel( "Dateityp:" );
    panelHead.add( this.labelFileType, gbcHead );

    this.comboFileType = new JComboBox();
    this.comboFileType.setEditable( true );
    this.comboFileType.addActionListener( this );
    gbcHead.fill        = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx     = 1.0;
    gbcHead.insets.left = 0;
    gbcHead.gridwidth   = 3;
    gbcHead.gridx++;
    panelHead.add( this.comboFileType, gbcHead );

    this.labelBegAddr   = new JLabel( LABEL_BEG_ADDR );
    gbcHead.fill        = GridBagConstraints.NONE;
    gbcHead.weightx     = 0.0;
    gbcHead.insets.left = 5;
    gbcHead.gridwidth   = 1;
    gbcHead.gridx       = 0;
    gbcHead.gridy++;
    panelHead.add( this.labelBegAddr, gbcHead );

    this.docBegAddr     = new HexDocument( 4, LABEL_BEG_ADDR );
    this.fldBegAddr     = new JTextField( this.docBegAddr, "", 0 );
    gbcHead.fill        = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx     = 0.5;
    gbcHead.insets.left = 0;
    gbcHead.gridx++;
    panelHead.add( this.fldBegAddr, gbcHead );

    Font font = this.fldBegAddr.getFont();
    if( font != null ) {
      this.comboFileType.setFont( font );
    }

    this.labelStartAddr = new JLabel( LABEL_START_ADDR );
    gbcHead.fill        = GridBagConstraints.NONE;
    gbcHead.weightx     = 0.0;
    gbcHead.insets.left = 5;
    gbcHead.gridx++;
    panelHead.add( this.labelStartAddr, gbcHead );

    this.docStartAddr   = new HexDocument( 4, LABEL_START_ADDR );
    this.fldStartAddr   = new JTextField( this.docStartAddr, "", 0 );
    gbcHead.fill        = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx     = 0.5;
    gbcHead.insets.left = 0;
    gbcHead.gridx++;
    panelHead.add( this.fldStartAddr, gbcHead );

    this.labelFileDesc    = new JLabel( "Beschreibung:" );
    gbcHead.insets.left   = 5;
    gbcHead.insets.bottom = 5;
    gbcHead.gridx         = 0;
    gbcHead.gridy++;
    panelHead.add( this.labelFileDesc, gbcHead );

    this.docFileDesc    = new LimitedDocument( 0, false );
    this.fldFileDesc    = new JTextField( this.docFileDesc, "", 0 );
    gbcHead.fill        = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx     = 1.0;
    gbcHead.insets.left = 0;
    gbcHead.gridwidth   = 3;
    gbcHead.gridx++;
    panelHead.add( this.fldFileDesc, gbcHead );

    JPanel panelOutFile = new JPanel( new GridBagLayout() );
    gbcOut.fill      = GridBagConstraints.HORIZONTAL;
    gbcOut.weightx   = 1.0;
    gbcOut.gridwidth = GridBagConstraints.REMAINDER;
    gbcOut.gridx     = 0;
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
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 4, 5, 5 ) );
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnConvert = new JButton( "Konvertieren" );
    this.btnConvert.addActionListener( this );
    panelBtn.add( this.btnConvert );

    this.btnPlay = new JButton( "Wiedergeben" );
    this.btnPlay.addActionListener( this );
    panelBtn.add( this.btnPlay );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    panelBtn.add( this.btnClose );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      this.listTarget.setVisibleRowCount( 7 );
      pack();
      this.listTarget.setVisibleRowCount( 1 );
      setScreenCentered();
    }
    setResizable( true );


    // sonstiges
    (new DropTarget( this.fldSrcFile, this )).setActive( true );
    clearOutFields();
  }


	/* --- private Methoden --- */

  private void clearOutFields()
  {
    this.targets.clear();
    this.listTarget.setListData( this.targets );

    this.labelBegAddr.setEnabled( false );
    this.labelStartAddr.setEnabled( false );
    this.labelFileDesc.setEnabled( false );
    this.labelFileType.setEnabled( false );

    this.fldBegAddr.setText( "" );
    this.fldStartAddr.setText( "" );
    this.fldFileDesc.setText( "" );
    this.comboFileType.setSelectedItem( "" );

    this.fldBegAddr.setEnabled( false );
    this.fldStartAddr.setEnabled( false );
    this.fldFileDesc.setEnabled( false );
    this.comboFileType.setEnabled( false );

    this.btnConvert.setEnabled( false );
    this.btnPlay.setEnabled( false );
  }


  public int getAddr( HexDocument hexDoc ) throws UserInputException
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
    byte[] fileBytes = EmuUtil.readFile( file, MAX_DISK_FILE_SIZE );
    if( fileBytes != null ) {
      FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
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


  private AbstractConvertTarget getSelectedTarget()
  {
    AbstractConvertTarget target = null;
    Object                value  = this.listTarget.getSelectedValue();
    if( value != null ) {
      if( value instanceof AbstractConvertTarget ) {
	target = (AbstractConvertTarget) value;
      }
    }
    return target;
  }


  private void setAddr( JTextField textFld, int addr )
  {
    if( addr >= 0 ) {
      textFld.setText( String.format( "%04X", addr ) );
    } else {
      textFld.setText( "" );
    }
  }


  private void targetSelectionChanged()
  {
    AbstractConvertTarget target = getSelectedTarget();
    if( target != null ) {
      this.labelBegAddr.setEnabled( target.usesBegAddr() );
      this.fldBegAddr.setEnabled( target.usesBegAddr() );

      int maxLen = target.getMaxFileDescLen();
      this.docFileDesc.setMaxLength( maxLen );
      this.labelFileDesc.setEnabled( maxLen > 0 );
      this.fldFileDesc.setEnabled( maxLen > 0 );
      this.fldFileDesc.setText( this.orgFileDesc );

      target.setFileTypesTo( this.comboFileType );
      this.labelFileType.setEnabled( this.comboFileType.isEnabled() );

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

      this.btnConvert.setEnabled( false );
      this.btnPlay.setEnabled( false );
    }
    updStartAddrEnabled();
  }


  private void updStartAddrEnabled()
  {
    boolean state = false;
    try {
      AbstractConvertTarget target = getSelectedTarget();
      if( target != null ) {
	state = target.usesStartAddr( getFileType( false ) );
      }
    }
    catch( UserInputException ex ) {}
    this.labelStartAddr.setEnabled( state );
    this.fldStartAddr.setEnabled( state );
    this.fldStartAddr.setEditable( state );
  }
}
