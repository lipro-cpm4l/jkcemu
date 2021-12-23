/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Datei laden
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import jkcemu.Main;
import jkcemu.audio.AudioFrm;
import jkcemu.audio.AudioUtil;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.HexDocument;
import jkcemu.base.ScreenFrm;
import jkcemu.emusys.A5105;
import jkcemu.emusys.AC1;
import jkcemu.emusys.HueblerGraphicsMC;
import jkcemu.emusys.KC85;
import jkcemu.emusys.KramerMC;
import jkcemu.emusys.LLC2;
import jkcemu.emusys.Z1013;
import jkcemu.emusys.Z9001;
import jkcemu.file.FileUtil;


public class LoadDlg extends BaseDlg implements DocumentListener
{
  private static final String HELP_PAGE = "/help/loadsave.htm";

  private static final FileFormat[] fileFormats = {
					FileFormat.KCB,
					FileFormat.KCB_BLKN,
					FileFormat.KCB_BLKN_CKS,
					FileFormat.KCC,
					FileFormat.KCC_BLKN,
					FileFormat.KCC_BLKN_CKS,
					FileFormat.KCTAP_SYS,
					FileFormat.KCTAP_BASIC_PRG,
					FileFormat.KCBASIC_PRG,
					FileFormat.KCBASIC_HEAD_PRG,
					FileFormat.KCBASIC_HEAD_PRG_BLKN,
					FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS,
					FileFormat.RBASIC_PRG,
					FileFormat.RMC,
					FileFormat.BASIC_PRG,
					FileFormat.HEADERSAVE,
					FileFormat.INTELHEX,
					FileFormat.BIN,
					FileFormat.COM };

  private static final String textSelectFmt = "--- Bitte ausw\u00E4hlen ---";
  private static final String errMsgEmptyFile =
	"Die Datei enth\u00E4lt keine Daten,\n"
			+ "die geladen werden k\u00F6nnten.";

  private ScreenFrm         screenFrm;
  private boolean           startEnabled;
  private File              file;
  private String            fileName;
  private byte[]            fileBuf;
  private JLabel            labelInfoBegAddr;
  private JLabel            labelInfoEndAddr;
  private JLabel            labelInfoStartAddr;
  private JLabel            labelInfoType;
  private JLabel            labelInfoDesc;
  private JLabel            labelLoadBasicAddr;
  private JLabel            labelLoadBegAddr;
  private JLabel            labelLoadEndAddr;
  private JTextField        fldInfoBegAddr;
  private JTextField        fldInfoEndAddr;
  private JTextField        fldInfoStartAddr;
  private JTextField        fldInfoType;
  private JTextField        fldInfoDesc;
  private JTextField        fldLoadBegAddr;
  private JTextField        fldLoadEndAddr;
  private JRadioButton      rbLoadForRAMBasic;
  private JRadioButton      rbLoadForROMBasic;
  private HexDocument       docLoadBegAddr;
  private HexDocument       docLoadEndAddr;
  private JComboBox<Object> comboFileFmt;
  private JButton           btnLoad;
  private JButton           btnStart;
  private JButton           btnHelp;
  private JButton           btnCancel;


  public static void loadFile(
			Frame     owner,
			ScreenFrm screenFrm,
			File      file,
			String    fileName,
			byte[]    fileBytes,
			boolean   interactive,
			boolean   startEnabled,
			boolean   startSelected )
  {
    /*
     * pruefen, ob die Datei nur ueber die emulierte
     * Kassettenschnittstelle geladen werden kann
     */
    FileInfo   fileInfo  = null;
    FileFormat fileFmt   = null;
    boolean    tapeFile  = false;
    boolean    audioFile = false;
    if( file != null ) {
      audioFile = AudioUtil.isAudioFile( file );
    } else if( fileBytes != null ) {
      audioFile = AudioUtil.isAudioFile( fileBytes );
    }
    if( !audioFile ) {
      if( (fileBytes == null) && (file != null) ) {
	fileBytes = readFile( owner, file );
      }
      fileInfo = FileInfo.analyzeFile( fileBytes, file, fileName );
      if( fileInfo != null ) {
	fileFmt  = fileInfo.getFileFormat();
	tapeFile = fileInfo.isTapeFile();
      }
    }
    if( audioFile || tapeFile ) {
      if( showSuppressableConfirmDlg(
		owner,
		"Die Datei kann nur \u00FCber eine Audiofunktion"
			+ " geladen werden.\n"
			+ "Dazu m\u00FCssen Sie im emulierten System"
			+ " das Laden von Kassette\n"
			+ "und im Fenster Audio/Kassette das Abspielden"
			+ " der Datei starten." ) )
      {
	AudioFrm.open( screenFrm ).openFile( file, fileBytes, 0 );
      }
    } else {
      EmuThread emuThread = screenFrm.getEmuThread();
      EmuSys    emuSys    = screenFrm.getEmuSys();
      if( (emuThread != null) && (emuSys != null) && (fileBytes != null) ) {

	/*
	 * pruefen, ob das gerade emulierte System eine Ladeadresse vorgibt,
	 * Wenn ja, dann Dialog mit der eingetragenen Ladeadresse anzeigen
	 */
	Integer begAddr = null;
	if( emuSys != null ) {
	  begAddr = emuSys.getLoadAddr();
	}

	// ggf. muss Dialog mit den Ladeoptionen angezeigt werden
	boolean done = false;
	if( (begAddr == null) && !interactive && (fileFmt != null) ) {
	  LoadData loadData = null;
	  try {
	    loadData = FileInfo.createLoadData( fileBytes, fileFmt );
	  }
	  catch( IOException ex ) {}
	  if( loadData != null ) {
	    int originBegAddr = loadData.getBegAddr();
	    if( originBegAddr >= 0 ) {
	      if( !startSelected ) {
		loadData.setStartAddr( -1 );
	      }
	      if( fileFmt.equals( FileFormat.HEADERSAVE ) ) {
		if( fileInfo.getFileType() != 'C' ) {
		  loadData.setStartAddr( -1 );
		}
	      }

	      // Datei in Arbeitsspeicher laden und ggf. starten
	      if( confirmLoadDataInfo( owner, loadData ) ) {
		StringBuilder statusMsg = new StringBuilder();
		emuThread.loadIntoMemory( loadData, statusMsg );
		if( file != null ) {
		  Main.setLastFile( file, Main.FILE_GROUP_SOFTWARE );
		}

		// ggf. Meldung anzeigen und Multi-TAP-Handling
		if( statusMsg.length() > 0 ) {
		  screenFrm.showStatusText( statusMsg.toString() );
		} else {
		  showLoadMsg( screenFrm, loadData );
		}
		checkMultiTAPHandling(
				owner,
				screenFrm,
				file,
				fileBytes,
				fileInfo.getNextTAPOffset() );
		done = true;
	      }
	    }
	  }
	}
	if( !done ) {
	  LoadDlg dlg = new LoadDlg(
				owner,
				screenFrm,
				file,
				fileName,
				fileBytes,
				fileFmt != null ? fileFmt : FileFormat.BIN,
				begAddr,
				startEnabled );
	  dlg.setVisible( true );
	}
      }
    }
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    updStartButton();
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    updStartButton();
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    updStartButton();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.comboFileFmt ) {
	  rv = true;
	  updFields();
	}
	else if( (src == this.btnLoad)
		 || (src == fldLoadEndAddr) )
	{
	  rv = true;
	  doLoad( false );
	}
	else if( src == this.btnStart ) {
	  rv = true;
	  doLoad( true );
	}
	else if( src == this.btnHelp ) {
	  rv = true;
	  HelpFrm.openPage( HELP_PAGE );
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
	else if( src instanceof JTextField ) {
	  rv = true;
	  ((JTextField) src).transferFocus();
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.comboFileFmt.removeActionListener( this );
      this.docLoadBegAddr.removeDocumentListener( this );
      this.fldLoadBegAddr.removeActionListener( this );
      this.docLoadEndAddr.removeDocumentListener( this );
      this.fldLoadEndAddr.removeActionListener( this );
      this.btnLoad.removeActionListener( this );
      if( this.btnStart != null ) {
	this.btnStart.removeActionListener( this );
      }
      this.btnHelp.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private LoadDlg(
		Frame      owner,
		ScreenFrm  screenFrm,
		File       file,
		String     fileName,	// falls nicht im Dateisystem vorh.
		byte[]     fileBuf,
		FileFormat fileFmt,
		Integer    begAddr,
		boolean    startEnabled )
  {
    super( owner, "Datei laden" );
    if( (fileName == null) && (file != null) ) {
      fileName = file.getName();
    }
    if( fileName != null ) {
      setTitle( getTitle() + ": " + fileName );
    }
    this.screenFrm    = screenFrm;
    this.file         = file;
    this.fileName     = fileName;
    this.fileBuf      = fileBuf;
    this.startEnabled = startEnabled;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Bereich Dateiformat und Kopdaten
    JPanel panelFmt = GUIFactory.createPanel( new GridBagLayout() );
    panelFmt.setBorder( GUIFactory.createTitledBorder(
					"Dateitformat und Kopfdaten" ) );
    add( panelFmt, gbc );

    GridBagConstraints gbcFmt = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    boolean fmtSupported = false;
    if( fileFmt != null ) {
      if( fileFmt.equals( FileFormat.KCTAP_KC85 )
	  || fileFmt.equals( FileFormat.KCTAP_Z9001 ) )
      {
	fileFmt      = FileFormat.KCTAP_SYS;
	fmtSupported = true;
      } else {
	for( FileFormat fmt : fileFormats ) {
	  if( fmt.equals( fileFmt ) ) {
	    fmtSupported = true;
	    break;
	  }
	}
      }
    }
    this.comboFileFmt = GUIFactory.createComboBox();
    this.comboFileFmt.setEditable( false );
    if( !fmtSupported ) {
      this.comboFileFmt.addItem( textSelectFmt );
    }
    for( FileFormat fmt : fileFormats ) {
      this.comboFileFmt.addItem( fmt );
    }
    if( fmtSupported ) {
      this.comboFileFmt.setSelectedItem( fileFmt );
    } else {
      this.comboFileFmt.setSelectedItem( textSelectFmt );
    }
    panelFmt.add( this.comboFileFmt, gbcFmt );

    this.labelInfoBegAddr = GUIFactory.createLabel( "Anfangsadresse:" );
    gbcFmt.anchor        = GridBagConstraints.EAST;
    gbcFmt.gridwidth     = 1;
    gbcFmt.insets.bottom = 2;
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoBegAddr, gbcFmt );

    this.labelInfoEndAddr = GUIFactory.createLabel( "Endadresse:" );
    gbcFmt.insets.top = 2;
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoEndAddr, gbcFmt );

    this.labelInfoStartAddr = GUIFactory.createLabel( "Startadresse:" );
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoStartAddr, gbcFmt );

    this.labelInfoType = GUIFactory.createLabel( "Typ:" );
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoType, gbcFmt );

    this.labelInfoDesc = GUIFactory.createLabel( "Beschreibung:" );
    gbcFmt.insets.bottom = 5;
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoDesc, gbcFmt );

    gbcFmt.anchor        = GridBagConstraints.WEST;
    gbcFmt.fill          = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx       = 1.0;
    gbcFmt.insets.top    = 5;
    gbcFmt.insets.bottom = 2;
    gbcFmt.gridy         = 1;
    gbcFmt.gridx++;
    this.fldInfoBegAddr = GUIFactory.createTextField();
    this.fldInfoBegAddr.setEditable( false );
    panelFmt.add( this.fldInfoBegAddr, gbcFmt );

    this.fldInfoEndAddr = GUIFactory.createTextField();
    this.fldInfoEndAddr.setEditable( false );
    gbcFmt.insets.top = 2;
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoEndAddr, gbcFmt );

    this.fldInfoStartAddr = GUIFactory.createTextField();
    this.fldInfoStartAddr.setEditable( false );
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoStartAddr, gbcFmt );

    this.fldInfoType = GUIFactory.createTextField();
    this.fldInfoType.setEditable( false );
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoType, gbcFmt );

    this.fldInfoDesc = GUIFactory.createTextField();
    this.fldInfoDesc.setEditable( false );
    gbcFmt.insets.bottom = 5;
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoDesc, gbcFmt );


    // Bereich Ladeoptionen
    JPanel panelLoad = GUIFactory.createPanel( new GridBagLayout() );
    panelLoad.setBorder( GUIFactory.createTitledBorder( "Ladeadressen" ) );
    gbc.gridy++;
    add( panelLoad, gbc );

    GridBagConstraints gbcLoad = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    this.labelLoadBegAddr = GUIFactory.createLabel( "Anfangsadresse:" );
    panelLoad.add( labelLoadBegAddr, gbcLoad );

    this.docLoadBegAddr = new HexDocument( 4, "Anfangsadresse" );
    this.fldLoadBegAddr = GUIFactory.createTextField(
						this.docLoadBegAddr,
						0 );
    this.fldLoadBegAddr.setEditable( true );
    gbcLoad.anchor = GridBagConstraints.WEST;
    gbcLoad.fill   = GridBagConstraints.HORIZONTAL;
    gbcLoad.gridx++;
    panelLoad.add( this.fldLoadBegAddr, gbcLoad );

    if( begAddr != null ) {
      this.docLoadBegAddr.setValue( begAddr.intValue(), 4 );
    }

    this.labelLoadEndAddr = GUIFactory.createLabel( "Endadresse:" );
    gbcLoad.anchor        = GridBagConstraints.EAST;
    gbcLoad.fill          = GridBagConstraints.NONE;
    gbcLoad.insets.top    = 2;
    gbcLoad.insets.bottom = 5;
    gbcLoad.gridx         = 0;
    gbcLoad.gridy++;
    panelLoad.add( this.labelLoadEndAddr, gbcLoad );

    this.docLoadEndAddr = new HexDocument( 4, "Endadresse" );
    this.fldLoadEndAddr = GUIFactory.createTextField(
						this.docLoadEndAddr,
						0 );
    this.fldLoadEndAddr.setEditable( true );
    gbcLoad.anchor = GridBagConstraints.WEST;
    gbcLoad.fill   = GridBagConstraints.HORIZONTAL;
    gbcLoad.gridx++;
    panelLoad.add( this.fldLoadEndAddr, gbcLoad );

    this.labelLoadBasicAddr = GUIFactory.createLabel(
					"KC-BASIC-Programm laden nach:" );
    gbcLoad.fill          = GridBagConstraints.NONE;
    gbcLoad.insets.top    = 5;
    gbcLoad.insets.bottom = 2;
    gbcLoad.gridwidth     = GridBagConstraints.REMAINDER;
    gbcLoad.gridx         = 0;
    gbcLoad.gridy++;
    panelLoad.add( this.labelLoadBasicAddr, gbcLoad );

    ButtonGroup grpLoadBasic = new ButtonGroup();

    this.rbLoadForROMBasic = GUIFactory.createRadioButton(
					"0401h f\u00FCr ROM-BASIC" );
    grpLoadBasic.add( this.rbLoadForROMBasic );
    gbcLoad.insets.top    = 2;
    gbcLoad.insets.bottom = 0;
    gbcLoad.insets.left   = 40;
    gbcLoad.gridy++;
    panelLoad.add( this.rbLoadForROMBasic, gbcLoad );

    this.rbLoadForRAMBasic = GUIFactory.createRadioButton(
					"2C01h f\u00FCr RAM-BASIC",
					true );
    grpLoadBasic.add( this.rbLoadForRAMBasic );
    gbcLoad.insets.top    = 0;
    gbcLoad.insets.bottom = 5;
    gbcLoad.gridy++;
    panelLoad.add( this.rbLoadForRAMBasic, gbcLoad );


    // Bereich Knoepfe
    JPanel panelBtn = GUIFactory.createPanel(
		new GridLayout( this.startEnabled ? 4 : 3, 1, 5, 5 ) );
    gbc.anchor     = GridBagConstraints.NORTHWEST;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridx      = 1;
    gbc.gridy      = 0;
    add( panelBtn, gbc );

    this.btnLoad = GUIFactory.createButton( EmuUtil.TEXT_LOAD );
    this.btnLoad.setEnabled( false );
    panelBtn.add( this.btnLoad );

    this.btnStart = null;
    if( this.startEnabled ) {
      this.btnStart = GUIFactory.createButton( "Starten" );
      this.btnStart.setEnabled( false );
      panelBtn.add( this.btnStart );
    }

    this.btnHelp = GUIFactory.createButtonHelp();
    panelBtn.add( this.btnHelp );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Felder aktualisieren
    updFields();


    /*
     * Bei einer BIN-Datei versuchen,
     * die Ladeadressen aus dem Dateinamen zu ermitteln
     */
    if( (file != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileFormat.BIN ) ) {
	int[] addrs = FileUtil.extractAddressesFromFileName( file.getName() );
	if( addrs != null ) {
	  if( addrs.length > 0 ) {
	    this.fldLoadBegAddr.setText(
			String.format( "%04X", addrs[ 0 ] ) );
	  }
	  if( addrs.length > 1 ) {
	    this.fldLoadEndAddr.setText(
			String.format( "%04X", addrs[ 1 ] ) );
	  }
	}
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Listener
    this.comboFileFmt.addActionListener( this );
    this.docLoadBegAddr.addDocumentListener( this );
    this.fldLoadBegAddr.addActionListener( this );
    this.docLoadEndAddr.addDocumentListener( this );
    this.fldLoadEndAddr.addActionListener( this );
    this.btnLoad.addActionListener( this );
    if( this.btnStart != null ) {
      this.btnStart.addActionListener( this );
    }
    this.btnHelp.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


  private static void checkMultiTAPHandling(
				Window    owner,
				ScreenFrm screenFrm,
				File      file,
				byte[]    fileBytes,
				int       nextTAPOffs )
  {
    if( (file != null) && (fileBytes != null) && (nextTAPOffs > 0) ) {
      if( JOptionPane.showConfirmDialog(
		owner,
		"Die Datei ist eine Multi-TAP-Datei,"
			+ " d.h., sie enth\u00E4lt mehrere Teildateien.\n"
			+ "Es wurde aber nur die erste Teildatei in den"
			+ " Arbeitsspeicher geladen.\n\n"
			+ "H\u00E4ufig versucht das in der ersten Teildatei"
			+ " enthaltene Programm,\n"
			+ "die restlichen Teildateien von Kassette"
			+ " nachzuladen.\n"
			+ "Sie k\u00F6nnen jetzt die Emulation"
			+ " des Kassettenrecorderanschlusses\n"
			+ "mit den restlichen Teildateien aktivieren,\n"
			+ "sodass das Nachladen auch im Emulator"
			+ " funktioniert.\n\n"
			+ "M\u00F6chten Sie das jetzt tun?",
		"Multi-TAP-Datei",
		JOptionPane.YES_NO_OPTION,
		JOptionPane.WARNING_MESSAGE ) == JOptionPane.YES_OPTION )
      {
	screenFrm.openAudioInFile( file, fileBytes, nextTAPOffs );
      }
    }
  }


  private static boolean confirmLoadDataInfo(
				Window   owner,
				LoadData loadData )
  {
    boolean rv      = true;
    String  infoMsg = loadData.getInfoMsg();
    if( infoMsg != null ) {
      if( !infoMsg.isEmpty() ) {
	if( JOptionPane.showConfirmDialog(
		owner,
		infoMsg,
		"Achtung",
		JOptionPane.WARNING_MESSAGE,
		JOptionPane.OK_CANCEL_OPTION ) != JOptionPane.OK_OPTION )
	{
	  rv = false;
	}
      }
    }
    return rv;
  }


  private void doLoad( boolean startSelected )
  {
    EmuThread  emuThread = this.screenFrm.getEmuThread();
    FileFormat fileFmt   = getSelectedFileFormat();
    if( (emuThread != null) && (fileFmt != null) ) {
      try {

	// Eingaben pruefen
	int begAddr = -1;
	int endAddr = -1;
	if( this.docLoadBegAddr.getLength() > 0 ) {
	  begAddr = (this.docLoadBegAddr.intValue() & 0xFFFF);
	}
	if( this.docLoadEndAddr.getLength() > 0 ) {
	  endAddr = (this.docLoadEndAddr.intValue() & 0xFFFF);
	}

	// Datei laden
	LoadData loadData = FileInfo.createLoadData( this.fileBuf, fileFmt );

	// Ladeadressen ermitteln, dabei ggf. KC-BASIC-Programm relozieren
	if( FileInfo.isKCBasicProgramFormat( fileFmt ) ) {
	  loadData.relocateKCBasicProgram(
		this.rbLoadForRAMBasic.isSelected() ? 0x2C01 : 0x0401 );
	} else {
	  if( begAddr >= 0 ) {
	    if( begAddr != loadData.getBegAddr() ) {
	      loadData.setStartAddr( -1 );
	    }
	    loadData.setBegAddr( begAddr );
	    if( endAddr >= 0 ) {
	      int bytesToLoad = endAddr - begAddr + 1;
	      if( bytesToLoad < loadData.getLength() ) {
		loadData.setLength( bytesToLoad );
		loadData.setStartAddr( -1 );
	      }
	    }
	  }
	}
	if( loadData.getLength() < 1 ) {
	  throw new IOException( errMsgEmptyFile );
	}
	if( begAddr < 0 ) {
	  begAddr = loadData.getBegAddr();
	}
	if( begAddr < 0 ) {
	  throw new IOException(
		"Es ist nicht klar, wohin die Datei geladen werden soll.\n"
			+ "Bitte Anfangsadresse im Bereich Ladeadressen"
			+ " ausf\u00FCllen!" );
	}

	// ggf. Info bestaetigen
	if( confirmLoadDataInfo( this, loadData ) ) {

	  // Datei in Arbeitsspeicher laden und ggf. starten
	  if( !startEnabled || !startSelected ) {
	    loadData.setStartAddr( -1 );
	  }
	  StringBuilder statusMsg = new StringBuilder();
	  emuThread.loadIntoMemory( loadData, statusMsg );
	  Main.setLastFile( this.file, Main.FILE_GROUP_SOFTWARE );

	  // ggf. Meldung anzeigen
	  if( statusMsg.length() > 0 ) {
	    screenFrm.showStatusText( statusMsg.toString() );
	  } else {
	    showLoadMsg( this.screenFrm, loadData );
	  }

	  // Multi-TAP-Handling
	  if( fileFmt.equals( FileFormat.KCTAP_SYS )
	      || fileFmt.equals( FileFormat.KCTAP_Z9001 )
	      || fileFmt.equals( FileFormat.KCTAP_KC85 )
	      || fileFmt.equals( FileFormat.KCTAP_BASIC_PRG ) )
	  {
	    FileInfo fileInfo = FileInfo.analyzeFile(
						this.fileBuf,
						this.file,
						this.fileName );
	    if( fileInfo != null ) {
	      if( fileFmt.equals( fileInfo.getFileFormat() ) ) {
		checkMultiTAPHandling(
				this,
				this.screenFrm,
				this.file,
				this.fileBuf,
				fileInfo.getNextTAPOffset() );
	      }
	    }
	  }
	  doClose();
	}
      }
      catch( IOException ex ) {
	showErrorDlg( this, ex );
      }
      catch( NumberFormatException ex ) {
	showErrorDlg( this, ex );
      }
    }
  }


  private FileFormat getSelectedFileFormat()
  {
    FileFormat fmt = null;
    Object     obj = this.comboFileFmt.getSelectedItem();
    if( obj != null ) {
      if( obj instanceof FileFormat ) {
	fmt = (FileFormat) obj;
      }
    }
    return fmt;
  }


  /*
   * Diese Methode liest eine Datei und gibt den Inhalt
   * als byte-Array zurueck.
   * Um bei einer sehr grossen Datei einen Speicherueberlauf zu verhindern,
   * werden nur soviele Bytes gelesen,
   * dass sich bis zu 64 KByte Nutzbytes extrahieren lassen.
   */
  private static byte[] readFile( Component owner, File file )
  {
    byte[] rv = null;
    try {
      rv = FileInfo.readFile( file );
    }
    catch( IOException ex ) {
      showErrorDlg(
		owner,
		"Datei kann nicht geladen werden.\n\n" + ex.getMessage() );
    }
    return rv;
  }


  private static void showLoadMsg( ScreenFrm screenFrm, LoadData loadData )
  {
    if( loadData != null ) {
      int endAddr = loadData.getEndAddr();
      if( endAddr > 0xFFFF ) {
	endAddr = 0xFFFF;
      }
      screenFrm.showStatusText(
		String.format(
			"Datei nach %04X-%04X geladen",
			loadData.getBegAddr(),
			endAddr ) );
    }
  }


  private void updFields()
  {
    boolean isCOM               = false;
    boolean isHS                = false;
    boolean isKCB               = false;
    boolean isKCC               = false;
    boolean isKCTAP_SYS         = false;
    boolean isKCTAP_BASIC_PRG   = false;
    boolean isKCBASIC_HEAD_PRG  = false;
    boolean isKCBASIC_PRG       = false;
    boolean isRMC               = false;
    boolean isBASIC             = false;
    boolean isINTELHEX          = false;
    boolean loadable            = false;
    int     begAddr             = -1;
    int     endAddr             = -1;
    int     startAddr           = -1;
    int     fileType            = -1;
    String  fileDesc            = null;

    FileFormat fileFmt = getSelectedFileFormat();
    if( fileFmt != null ) {
      isCOM              = fileFmt.equals( FileFormat.COM );
      isHS               = fileFmt.equals( FileFormat.HEADERSAVE );
      isKCB              = fileFmt.equals( FileFormat.KCB );
      isKCC              = fileFmt.equals( FileFormat.KCC );
      isKCTAP_SYS        = fileFmt.equals( FileFormat.KCTAP_SYS )
				|| fileFmt.equals( FileFormat.KCTAP_Z9001 )
				|| fileFmt.equals( FileFormat.KCTAP_KC85 );
      isKCTAP_BASIC_PRG  = fileFmt.equals( FileFormat.KCTAP_BASIC_PRG );
      isKCBASIC_PRG      = fileFmt.equals( FileFormat.KCBASIC_PRG );
      isKCBASIC_HEAD_PRG = fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN )
		|| fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG_BLKN_CKS );
      isRMC              = fileFmt.equals( FileFormat.RMC );
      isBASIC            = fileFmt.equals( FileFormat.BASIC_PRG )
				|| fileFmt.equals( FileFormat.RBASIC_PRG );
      isINTELHEX         = fileFmt.equals( FileFormat.INTELHEX );
      loadable           = !fileFmt.equals( textSelectFmt );

      begAddr = FileInfo.getBegAddr( this.fileBuf, fileFmt );
      if( begAddr >= 0 ) {
	endAddr   = FileInfo.getEndAddr( this.fileBuf, fileFmt );
	startAddr = FileInfo.getStartAddr( this.fileBuf, fileFmt );
      }
      fileType = FileInfo.getFileType( this.fileBuf, fileFmt );
      fileDesc = FileInfo.getFileDesc( this.fileBuf, fileFmt );
    }

    boolean stateBegAddr = (isCOM || isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG || isKCBASIC_PRG
				|| isBASIC || isRMC || isINTELHEX);
    this.labelInfoBegAddr.setEnabled( stateBegAddr );
    if( stateBegAddr && (begAddr >= 0) ) {
      this.fldInfoBegAddr.setText( String.format( "%04X", begAddr ) );
    } else {
      this.fldInfoBegAddr.setText( "" );
    }

    boolean stateEndAddr = (isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG || isKCBASIC_PRG
				|| isRMC);
    this.labelInfoEndAddr.setEnabled( stateEndAddr );
    if( stateEndAddr && (endAddr >= 0) ) {
      this.fldInfoEndAddr.setText( String.format( "%04X", endAddr ) );
    } else {
      this.fldInfoEndAddr.setText( "" );
    }

    boolean stateStartAddr = (isHS || isKCC || isKCTAP_SYS || isRMC);
    this.labelInfoStartAddr.setEnabled( stateStartAddr );
    if( stateStartAddr && (startAddr >= 0) ) {
      this.fldInfoStartAddr.setText( String.format( "%04X", startAddr ) );
    } else {
      this.fldInfoStartAddr.setText( "" );
    }

    this.labelInfoType.setEnabled( isHS );
    if( isHS && (fileType >= 0x20) && (fileType < 0x7F) ) {
      this.fldInfoType.setText( Character.toString( (char) fileType ) );
    } else {
      this.fldInfoType.setText( "" );
    }

    boolean stateInfoDesc = (isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG);
    this.labelInfoDesc.setEnabled( stateInfoDesc );
    if( stateInfoDesc && (fileDesc != null) ) {
      this.fldInfoDesc.setText( fileDesc );
    } else {
      this.fldInfoDesc.setText( "" );
    }

    boolean stateKCBasicPrg = (isKCB || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG || isKCBASIC_PRG);
    boolean stateLoadAddr = loadable && !stateKCBasicPrg && !isBASIC;
    this.labelLoadBegAddr.setEnabled( stateLoadAddr );
    this.labelLoadEndAddr.setEnabled( stateLoadAddr );
    this.fldLoadBegAddr.setEditable( stateLoadAddr );
    this.fldLoadEndAddr.setEditable( stateLoadAddr );
    this.labelLoadBasicAddr.setEnabled( stateKCBasicPrg );
    this.rbLoadForRAMBasic.setEnabled( stateKCBasicPrg );
    this.rbLoadForROMBasic.setEnabled( stateKCBasicPrg );
    if( stateKCBasicPrg ) {
      if( this.screenFrm.getEmuSys().hasKCBasicInROM() ) {
	this.rbLoadForROMBasic.setSelected( true );
      } else {
	this.rbLoadForRAMBasic.setSelected( true );
      }
    }
    this.btnLoad.setEnabled( loadable && (this.fileBuf.length > 0) );
    updStartButton( loadable, begAddr, endAddr, startAddr );
  }


  /*
   * Autostart nur ermoeglichen, wenn:
   *  1. Startadresse vorhanden ist
   *  2. Ladeadresse nicht angegeben oder
   *     mit der originalen Ladeadresse uebereinstimmt
   */
  private void updStartButton(
			boolean loadable,
			int     begAddr,
			int     endAddr,
			int     startAddr )
  {
    if( this.btnStart != null ) {
      boolean state = false;
      if( loadable && this.startEnabled ) {
	if( (startAddr >= 0) && (startAddr >= begAddr) ) {
	  state = true;
	}

	// Anfangsadresse muss mit der originalen uebereinstimmen
	String text = this.fldLoadBegAddr.getText();
	if( text != null ) {
	  if( !text.isEmpty() ) {
	    if( (this.docLoadBegAddr.intValue() & 0xFFFF) != begAddr ) {
	      state = false;
	    }
	  }
	}

	// Endadresse muss gleich oder groesser der originalen sein
	text = this.fldLoadEndAddr.getText();
	if( text != null ) {
	  if( !text.isEmpty() ) {
	    if( (this.docLoadEndAddr.intValue() & 0xFFFF) < endAddr ) {
	      state = false;
	    }
	  }
	}
      }
      this.btnStart.setEnabled( state );
    }
  }


  private void updStartButton()
  {
    if( this.btnStart != null ) {
      boolean    loadable  = false;
      int        begAddr   = -1;
      int        endAddr   = -1;
      int        startAddr = -1;
      FileFormat fileFmt   = getSelectedFileFormat();
      if( fileFmt != null ) {
	loadable = !fileFmt.equals( textSelectFmt );
	if( loadable ) {
	  begAddr = FileInfo.getBegAddr( this.fileBuf, fileFmt );
	  if( begAddr >= 0 ) {
	    endAddr   = FileInfo.getEndAddr( this.fileBuf, fileFmt );
	    startAddr = FileInfo.getStartAddr( this.fileBuf, fileFmt );
	  }
	}
      }
      updStartButton( loadable, begAddr, endAddr, startAddr );
    }
  }
}
