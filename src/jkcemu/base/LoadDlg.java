/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Datei laden
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.emusys.*;


public class LoadDlg extends BasicDlg implements DocumentListener
{
  private static final String[] fileFormats = {
					FileInfo.KCB,
					FileInfo.KCC,
					FileInfo.KCTAP_SYS,
					FileInfo.KCTAP_BASIC_PRG,
					FileInfo.KCBASIC_HEAD_PRG,
					FileInfo.KCBASIC_PRG,
					FileInfo.RBASIC,
					FileInfo.HEADERSAVE,
					FileInfo.INTELHEX,
					FileInfo.BIN };

  private static final String textSelectFmt = "--- Bitte ausw\u00E4hlen ---";
  private static final String errMsgEmptyFile =
	"Die Datei enth\u00E4lt keine Daten,\n"
			+ "die geladen werden k\u00F6nnten.";

  private Frame        owner;
  private ScreenFrm    screenFrm;
  private boolean      startEnabled;
  private File         file;
  private byte[]       fileBuf;
  private JLabel       labelInfoBegAddr;
  private JLabel       labelInfoEndAddr;
  private JLabel       labelInfoStartAddr;
  private JLabel       labelInfoType;
  private JLabel       labelInfoDesc;
  private JLabel       labelLoadBasicAddr;
  private JLabel       labelLoadBegAddr;
  private JLabel       labelLoadEndAddr;
  private JTextField   fldInfoBegAddr;
  private JTextField   fldInfoEndAddr;
  private JTextField   fldInfoStartAddr;
  private JTextField   fldInfoType;
  private JTextField   fldInfoDesc;
  private JTextField   fldLoadBegAddr;
  private JTextField   fldLoadEndAddr;
  private JRadioButton btnLoadForRAMBasic;
  private JRadioButton btnLoadForROMBasic;
  private HexDocument  docLoadBegAddr;
  private HexDocument  docLoadEndAddr;
  private JCheckBox    btnKeepHeader;
  private JComboBox    comboFileFmt;
  private JButton      btnLoad;
  private JButton      btnStart;
  private JButton      btnHelp;
  private JButton      btnCancel;


  public static void loadFile(
			Frame     owner,
			ScreenFrm screenFrm,
			File      file,
			boolean   interactive,
			boolean   startEnabled,
			boolean   startSelected )
  {
    EmuThread emuThread = screenFrm.getEmuThread();
    EmuSys    emuSys    = screenFrm.getEmuSys();
    byte[]    fileBuf   = readFile( owner, file );
    if( (emuThread != null) && (emuSys != null) && (fileBuf != null) ) {

      /*
       * pruefen, ob das gerade emulierte System eine Ladeadresse vorgibt,
       * Wenn ja, dann Dialog mit der eingetragenen Ladeadresse anzeigen
       */
      Integer begAddr = null;
      if( emuSys != null ) {
	begAddr = emuSys.getLoadAddr();
      }

      // ggf. muss Dialog mit den Ladeoptionen angezeigt werden
      boolean  done     = false;
      String   fileFmt  = null;
      FileInfo fileInfo = FileInfo.analyzeFile( fileBuf, file );
      if( fileInfo != null ) {
	fileFmt = fileInfo.getFileFormat();
	if( (begAddr == null) && !interactive && (fileFmt != null) ) {
	  LoadData loadData = null;
	  try {
	    loadData = fileInfo.createLoadData( fileBuf, fileFmt );
	  }
	  catch( IOException ex ) {}
	  if( loadData != null ) {
	    int originBegAddr = loadData.getBegAddr();
	    if( originBegAddr >= 0 ) {
	      boolean isAC1      = (emuSys instanceof AC1);
	      boolean isLLC2     = (emuSys instanceof LLC2);
	      boolean isHGMC     = (emuSys instanceof HueblerGraphicsMC);
	      boolean isKC85     = (emuSys instanceof KC85);
	      boolean isKramerMC = (emuSys instanceof KramerMC);
	      boolean isZ1013    = (emuSys instanceof Z1013);
	      boolean isZ9001    = (emuSys instanceof Z9001);

	      if( !startSelected ) {
		loadData.setStartAddr( -1 );
	      }
	      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
		if( fileInfo.getFileType() != 'C' ) {
		  loadData.setStartAddr( -1 );
		}
	      }

	      /*
	       * Warnung, wenn Dateityp unueblich beim
	       * gerade emulierten System ist
	       */
	      if( loadData.getStartAddr() >= 0 ) {
		if( ((isAC1 || isHGMC || isKramerMC || isLLC2 || isZ1013)
				&& !fileFmt.equals( FileInfo.HEADERSAVE ))
		    || (isKC85 && !fileFmt.equals( FileInfo.KCC )
				&& !fileFmt.equals( FileInfo.KCTAP_KC85 )
				&& !fileFmt.equals( FileInfo.KCTAP_SYS ))
		    || (isZ9001 && !fileFmt.equals( FileInfo.KCC )
				&& !fileFmt.equals( FileInfo.KCTAP_Z9001 )
				&& !fileFmt.equals( FileInfo.KCTAP_SYS )) )
		{
		  String[] options = {
				"Laden und Starten",
				"Nur Laden",
				"Abbrechen" };
		  JOptionPane pane = new JOptionPane(
			"Der Dateityp enth\u00E4lt \u00FCblicherweise"
				+ " keine Programme f\u00FCr das gerade"
				+ " emulierte System.\n"
				+ "M\u00F6chten Sie trotzdem das in der Datei"
				+ " enthaltene Programm laden und starten?",
			JOptionPane.WARNING_MESSAGE );
		  pane.setOptions( options );
		  pane.createDialog( owner, "Warnung" ).setVisible( true );

		  done         = true;
		  Object value = pane.getValue();
		  if( value != null ) {
		    if( value.equals( options[ 0 ] ) ) {
		      done = false;
		    }
		    else if( value.equals( options[ 1 ] ) ) {
		      loadData.setStartAddr( -1 );
		      done = false;
		    }
		  }
		}
	      }
	      if( !done ) {

		// Datei in Arbeitsspeicher laden und ggf. starten
		if( confirmLoadDataInfo( owner, loadData ) ) {
		  emuThread.loadIntoMemory( loadData );
		  Main.setLastFile( file, "software" );

		  // ggf. Dateikopf in Arbeitsspeicher kopieren
		  if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
		    if( isZ1013
			&& EmuUtil.parseBoolean(
			    Main.getProperty( "jkcemu.loadsave.header.keep" ),
			    false )
			&& (loadData.getOffset() == 32) )
		    {
		      for( int i = 0; i < 32; i++ ) {
			emuThread.setMemByte(
					Z1013.MEM_HEAD + i,
					loadData.getAbsoluteByte( i ) );
		      }
		    }
		  }

		  // ggf. Meldung anzeigen und Multi-TAP-Handling
		  showLoadMsg( screenFrm, loadData );
		  checkMultiTAPHandling(
				owner,
				screenFrm,
				file,
				fileBuf,
				fileInfo.getNextTAPOffset() );
		}
		done = true;
	      }
	    }
	  }
	}
      }
      if( !done ) {
	LoadDlg dlg = new LoadDlg(
				owner,
				screenFrm,
				file,
				fileBuf,
				fileFmt != null ? fileFmt : FileInfo.BIN,
				begAddr,
				startEnabled );
	dlg.setVisible( true );
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
	  this.screenFrm.showHelp( "/help/loadsave.htm" );
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


	/* --- private Konstruktoren und Methoden --- */

  private LoadDlg(
		Frame     owner,
		ScreenFrm screenFrm,
		File      file,
		byte[]    fileBuf,
		String    fileFmt,
		Integer   begAddr,
		boolean   startEnabled )
  {
    super( owner, "Datei laden" );
    if( file != null ) {
      String fileName = file.getName();
      if( fileName != null ) {
	setTitle( getTitle() + ": " + fileName );
      }
    }
    this.owner        = owner;
    this.screenFrm    = screenFrm;
    this.file         = file;
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
    JPanel panelFmt = new JPanel( new GridBagLayout() );
    panelFmt.setBorder( BorderFactory.createTitledBorder(
					"Dateitformat und Kopfdaten" ) );

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
      if( fileFmt.equals( FileInfo.KCTAP_KC85 )
	  || fileFmt.equals( FileInfo.KCTAP_Z9001 ) )
      {
	fileFmt      = FileInfo.KCTAP_SYS;
	fmtSupported = true;
      } else {
	for( String s : fileFormats ) {
	  if( s.equals( fileFmt ) ) {
	    fmtSupported = true;
	    break;
	  }
	}
      }
    }
    this.comboFileFmt = new JComboBox();
    this.comboFileFmt.setEditable( false );
    if( !fmtSupported ) {
      this.comboFileFmt.addItem( textSelectFmt );
    }
    for( String s : fileFormats ) {
      this.comboFileFmt.addItem( s );
    }
    if( fmtSupported ) {
      this.comboFileFmt.setSelectedItem( fileFmt );
    } else {
      this.comboFileFmt.setSelectedItem( textSelectFmt );
    }
    this.comboFileFmt.addActionListener( this );
    panelFmt.add( this.comboFileFmt, gbcFmt );

    this.labelInfoBegAddr = new JLabel( "Anfangsadresse:" );
    gbcFmt.anchor        = GridBagConstraints.EAST;
    gbcFmt.gridwidth     = 1;
    gbcFmt.insets.bottom = 2;
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoBegAddr, gbcFmt );

    this.labelInfoEndAddr = new JLabel( "Endadresse:" );
    gbcFmt.insets.top = 2;
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoEndAddr, gbcFmt );

    this.labelInfoStartAddr = new JLabel( "Startadresse:" );
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoStartAddr, gbcFmt );

    this.labelInfoType = new JLabel( "Typ:" );
    gbcFmt.gridy++;
    panelFmt.add( this.labelInfoType, gbcFmt );

    this.labelInfoDesc = new JLabel( "Beschreibung:" );
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
    this.fldInfoBegAddr = new JTextField();
    this.fldInfoBegAddr.setEditable( false );
    panelFmt.add( this.fldInfoBegAddr, gbcFmt );

    this.fldInfoEndAddr = new JTextField();
    this.fldInfoEndAddr.setEditable( false );
    gbcFmt.insets.top = 2;
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoEndAddr, gbcFmt );

    this.fldInfoStartAddr = new JTextField();
    this.fldInfoStartAddr.setEditable( false );
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoStartAddr, gbcFmt );

    this.fldInfoType = new JTextField();
    this.fldInfoType.setEditable( false );
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoType, gbcFmt );

    this.fldInfoDesc = new JTextField();
    this.fldInfoDesc.setEditable( false );
    gbcFmt.insets.bottom = 5;
    gbcFmt.gridy++;
    panelFmt.add( this.fldInfoDesc, gbcFmt );

    add( panelFmt, gbc );


    // Bereich Ladeoptionen
    JPanel panelLoad = new JPanel( new GridBagLayout() );
    panelLoad.setBorder( BorderFactory.createTitledBorder( "Ladeadressen" ) );

    GridBagConstraints gbcLoad = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    this.labelLoadBegAddr = new JLabel( "Anfangsadresse:" );
    panelLoad.add( labelLoadBegAddr, gbcLoad );

    this.fldLoadBegAddr = new JTextField();
    this.docLoadBegAddr = new HexDocument(
				this.fldLoadBegAddr, 4, "Anfangsadresse" );
    this.docLoadBegAddr.addDocumentListener( this );
    this.fldLoadBegAddr.addActionListener( this );
    this.fldLoadBegAddr.setEditable( true );
    gbcLoad.anchor = GridBagConstraints.WEST;
    gbcLoad.fill   = GridBagConstraints.HORIZONTAL;
    gbcLoad.gridx++;
    panelLoad.add( this.fldLoadBegAddr, gbcLoad );

    if( begAddr != null ) {
      this.docLoadBegAddr.setValue( begAddr.intValue(), 4 );
    }

    this.labelLoadEndAddr = new JLabel( "Endadresse:" );
    gbcLoad.anchor        = GridBagConstraints.EAST;
    gbcLoad.fill          = GridBagConstraints.NONE;
    gbcLoad.insets.top    = 2;
    gbcLoad.insets.bottom = 5;
    gbcLoad.gridx         = 0;
    gbcLoad.gridy++;
    panelLoad.add( this.labelLoadEndAddr, gbcLoad );

    this.fldLoadEndAddr = new JTextField();
    this.docLoadEndAddr = new HexDocument(
				this.fldLoadEndAddr, 4, "Endadresse" );
    this.docLoadEndAddr.addDocumentListener( this );
    this.fldLoadEndAddr.addActionListener( this );
    this.fldLoadEndAddr.setEditable( true );
    gbcLoad.anchor = GridBagConstraints.WEST;
    gbcLoad.fill   = GridBagConstraints.HORIZONTAL;
    gbcLoad.gridx++;
    panelLoad.add( this.fldLoadEndAddr, gbcLoad );

    this.labelLoadBasicAddr = new JLabel( "KC-BASIC-Programm laden nach:" );
    gbcLoad.fill          = GridBagConstraints.NONE;
    gbcLoad.insets.top    = 5;
    gbcLoad.insets.bottom = 2;
    gbcLoad.gridwidth     = GridBagConstraints.REMAINDER;
    gbcLoad.gridx         = 0;
    gbcLoad.gridy++;
    panelLoad.add( this.labelLoadBasicAddr, gbcLoad );

    ButtonGroup grpLoadBasic = new ButtonGroup();

    this.btnLoadForROMBasic = new JRadioButton(
					"0401h f\u00FCr ROM-BASIC",
					false );
    grpLoadBasic.add( this.btnLoadForROMBasic );
    gbcLoad.insets.top    = 2;
    gbcLoad.insets.bottom = 0;
    gbcLoad.insets.left   = 40;
    gbcLoad.gridy++;
    panelLoad.add( this.btnLoadForROMBasic, gbcLoad );

    this.btnLoadForRAMBasic = new JRadioButton(
					"2C01h f\u00FCr RAM-BASIC",
					true );
    grpLoadBasic.add( this.btnLoadForRAMBasic );
    gbcLoad.insets.top    = 0;
    gbcLoad.insets.bottom = 5;
    gbcLoad.gridy++;
    panelLoad.add( this.btnLoadForRAMBasic, gbcLoad );

    this.btnKeepHeader = new JCheckBox( "Kopfdaten nach 00E0-00FF laden" );
    gbcLoad.insets.top  = 5;
    gbcLoad.insets.left = 5;
    gbcLoad.gridy++;
    panelLoad.add( this.btnKeepHeader, gbcLoad );

    gbc.gridy++;
    add( panelLoad, gbc );


    // Bereich Knoepfe
    JPanel panelBtn = new JPanel(
		new GridLayout( this.startEnabled ? 4 : 3, 1, 5, 5 ) );

    this.btnLoad = new JButton( "Laden" );
    this.btnLoad.addActionListener( this );
    this.btnLoad.addKeyListener( this );
    this.btnLoad.setEnabled( false );
    panelBtn.add( this.btnLoad );

    this.btnStart = null;
    if( this.startEnabled ) {
      this.btnStart = new JButton( "Starten" );
      this.btnStart.addActionListener( this );
      this.btnStart.addKeyListener( this );
      this.btnStart.setEnabled( false );
      panelBtn.add( this.btnStart );
    }

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    this.btnHelp.addKeyListener( this );
    panelBtn.add( this.btnHelp );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.anchor     = GridBagConstraints.NORTHWEST;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridx      = 1;
    gbc.gridy      = 0;
    add( panelBtn, gbc );


    // Felder aktualisieren
    EmuSys emuSys = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      this.btnKeepHeader.setSelected(
		(emuSys instanceof Z1013)
		&& EmuUtil.parseBoolean(
			Main.getProperty( "jkcemu.loadsave.header.keep" ),
			false ) );
    }
    updFields();


    /*
     * Bei einer BIN-Datei versuchen,
     * die Ladeadressen aus dem Dateinamen zu ermitteln
     */
    if( (file != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.BIN ) ) {
	int[] addrs = EmuUtil.extractAddressesFromFileName(
							file.getName() );
	if( addrs != null ) {
	  if( addrs.length > 0 ) {
	    this.fldLoadBegAddr.setText( String.format( "%04X", addrs[ 0 ] ) );
	  }
	  if( addrs.length > 1 ) {
	    this.fldLoadEndAddr.setText( String.format( "%04X", addrs[ 1 ] ) );
	  }
	}
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
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
				Component owner,
				LoadData  loadData )
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
    EmuThread emuThread = this.screenFrm.getEmuThread();
    Object    fileFmt   = this.comboFileFmt.getSelectedItem();
    if( (emuThread != null) && (fileFmt != null) ) {
      try {

	// Eingaben pruefen
	int begAddr = -1;
	int endAddr = -1;
	int len     = endAddr - begAddr;
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
		this.btnLoadForRAMBasic.isSelected() ? 0x2C01 : 0x0401 );
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
	  emuThread.loadIntoMemory( loadData );
	  Main.setLastFile( this.file, "software" );

	  // ggf. Dateikopf in Arbeitsspeicher kopieren
	  if( (emuThread.getEmuSys() instanceof Z1013)
	      && fileFmt.equals( FileInfo.HEADERSAVE )
	      && this.btnKeepHeader.isSelected()
	      && (loadData.getOffset() == 32) )
	  {
	    for( int i = 0; i < 32; i++ ) {
	      emuThread.setMemByte(
				Z1013.MEM_HEAD + i,
				loadData.getAbsoluteByte( i ) );
	    }
	  }

	  // ggf. Meldung anzeigen
	  showLoadMsg( this.screenFrm, loadData );

	  // Multi-TAP-Handling
	  if( fileFmt.equals( FileInfo.KCTAP_SYS )
	      || fileFmt.equals( FileInfo.KCTAP_Z9001 )
	      || fileFmt.equals( FileInfo.KCTAP_KC85 )
	      || fileFmt.equals( FileInfo.KCTAP_BASIC_PRG ) )
	  {
	    FileInfo fileInfo = FileInfo.analyzeFile(
						this.fileBuf,
						this.fileBuf.length,
						this.file );
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
	BasicDlg.showErrorDlg( this, ex );
      }
      catch( NumberFormatException ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }


  private static int parseHex( InputStream in, int cnt ) throws IOException
  {
    int value = 0;
    while( cnt > 0 ) {
      int ch = in.read();
      if( (ch >= '0') && (ch <= '9') ) {
	value = (value << 4) | ((ch - '0') & 0x0F);
      } else if( (ch >= 'A') && (ch <= 'F') ) {
	value = (value << 4) | ((ch - 'A' + 10) & 0x0F);
      } else if( (ch >= 'a') && (ch <= 'f') ) {
	value = (value << 4) | ((ch - 'a' + 10) & 0x0F);
      } else {
	throw new IOException(
		"Datei entspricht nicht dem erwarteten Hex-Format." );
      }
      --cnt;
    }
    return value;
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
      screenFrm.toFront();
      screenFrm.showStatusText(
		String.format(
			"Datei nach %04X-%04X geladen",
			loadData.getBegAddr(),
			endAddr ) );
    }
  }


  private void updFields()
  {
    boolean isHS                = false;
    boolean isKCB               = false;
    boolean isKCC               = false;
    boolean isKCTAP_SYS         = false;
    boolean isKCTAP_BASIC_PRG   = false;
    boolean isKCTAP_BASIC_DATA  = false;
    boolean isKCTAP_BASIC_ASC   = false;
    boolean isKCBASIC_HEAD_PRG  = false;
    boolean isKCBASIC_HEAD_DATA = false;
    boolean isKCBASIC_HEAD_ASC  = false;
    boolean isKCBASIC_PRG       = false;
    boolean isRBASIC            = false;
    boolean isINTELHEX          = false;
    boolean loadable            = false;
    int     begAddr             = -1;
    int     endAddr             = -1;
    int     startAddr           = -1;
    int     fileType            = -1;
    String  fileDesc            = null;

    Object fileFmt = this.comboFileFmt.getSelectedItem();
    if( fileFmt != null ) {
      isHS               = fileFmt.equals( FileInfo.HEADERSAVE );
      isKCB              = fileFmt.equals( FileInfo.KCB );
      isKCC              = fileFmt.equals( FileInfo.KCC );
      isKCTAP_SYS        = fileFmt.equals( FileInfo.KCTAP_SYS )
				|| fileFmt.equals( FileInfo.KCTAP_Z9001 )
				|| fileFmt.equals( FileInfo.KCTAP_KC85 );
      isKCTAP_BASIC_PRG  = fileFmt.equals( FileInfo.KCTAP_BASIC_PRG );
      isKCBASIC_HEAD_PRG = fileFmt.equals( FileInfo.KCBASIC_HEAD_PRG );
      isKCBASIC_PRG      = fileFmt.equals( FileInfo.KCBASIC_PRG );
      isRBASIC           = fileFmt.equals( FileInfo.RBASIC );
      isINTELHEX         = fileFmt.equals( FileInfo.INTELHEX );
      loadable           = !fileFmt.equals( textSelectFmt );

      begAddr = FileInfo.getBegAddr( this.fileBuf, fileFmt );
      if( begAddr >= 0 ) {
	endAddr   = FileInfo.getEndAddr( this.fileBuf, fileFmt );
	startAddr = FileInfo.getStartAddr( this.fileBuf, fileFmt );
      }
      fileType = FileInfo.getFileType( this.fileBuf, fileFmt );
      fileDesc = FileInfo.getFileDesc( this.fileBuf, fileFmt );
    }

    boolean stateBegAddr = (isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG || isKCBASIC_PRG
				|| isRBASIC || isINTELHEX);
    this.labelInfoBegAddr.setEnabled( stateBegAddr );
    if( stateBegAddr && (begAddr >= 0) ) {
      this.fldInfoBegAddr.setText( String.format( "%04X", begAddr ) );
    } else {
      this.fldInfoBegAddr.setText( "" );
    }

    boolean stateEndAddr = (isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG || isKCBASIC_PRG);
    this.labelInfoEndAddr.setEnabled( stateEndAddr );
    if( stateEndAddr && (endAddr >= 0) ) {
      this.fldInfoEndAddr.setText( String.format( "%04X", endAddr ) );
    } else {
      this.fldInfoEndAddr.setText( "" );
    }

    boolean stateStartAddr = (isHS || isKCC || isKCTAP_SYS);
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
				|| isKCTAP_SYS || isKCTAP_BASIC_PRG);
    this.labelInfoDesc.setEnabled( stateInfoDesc );
    if( stateInfoDesc && (fileDesc != null) ) {
      this.fldInfoDesc.setText( fileDesc );
    } else {
      this.fldInfoDesc.setText( "" );
    }

    boolean stateKCBasicPrg = (isKCB || isKCTAP_BASIC_PRG
				|| isKCBASIC_HEAD_PRG || isKCBASIC_PRG);
    boolean stateLoadAddr = loadable && !stateKCBasicPrg && !isRBASIC;
    this.labelLoadBegAddr.setEnabled( stateLoadAddr );
    this.labelLoadEndAddr.setEnabled( stateLoadAddr );
    this.fldLoadBegAddr.setEditable( stateLoadAddr );
    this.fldLoadEndAddr.setEditable( stateLoadAddr );
    this.labelLoadBasicAddr.setEnabled( stateKCBasicPrg );
    this.btnLoadForRAMBasic.setEnabled( stateKCBasicPrg );
    this.btnLoadForROMBasic.setEnabled( stateKCBasicPrg );
    if( stateKCBasicPrg ) {
      if( this.screenFrm.getEmuSys().hasKCBasicInROM() ) {
	this.btnLoadForROMBasic.setSelected( true );
      } else {
	this.btnLoadForRAMBasic.setSelected( true );
      }
    }

    EmuSys emuSys = this.screenFrm.getEmuSys();
    if( emuSys != null ) {
      this.btnKeepHeader.setEnabled(
			(emuSys instanceof Z1013)
			&& fileFmt.equals( FileInfo.HEADERSAVE ) );
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
      boolean loadable  = false;
      int     begAddr   = -1;
      int     endAddr   = -1;
      int     startAddr = -1;
      Object fileFmt   = this.comboFileFmt.getSelectedIndex();
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

