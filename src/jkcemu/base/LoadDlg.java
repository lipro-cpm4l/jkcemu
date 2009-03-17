/*
 * (c) 2008-2009 Jens Mueller
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
import jkcemu.system.*;


public class LoadDlg extends BasicDlg implements DocumentListener
{
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
    byte[] fileBuf = readFile( owner, file );
    if( fileBuf != null ) {

      /*
       * pruefen, ob das gerade emulierte System eine Ladeadresse vorgibt,
       * Wenn ja, dann Dialog mit der eingetragenen Ladeadresse anzeigen
       */
      Integer begAddr = screenFrm.getEmuThread().getEmuSys().getLoadAddr();

      // ggf. muss Dialog mit den Ladeoptionen angezeigt muss
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
	      EmuThread emuThread  = screenFrm.getEmuThread();
	      EmuSys    emuSys     = emuThread.getEmuSys();
	      boolean   isAC1      = (emuSys instanceof AC1);
	      boolean   isHGMC     = (emuSys instanceof HueblerGraphicsMC);
	      boolean   isKramerMC = (emuSys instanceof KramerMC);
	      boolean   isZ1013    = (emuSys instanceof Z1013);
	      boolean   isKC       = ((emuSys instanceof KC85)
					|| (emuSys instanceof Z9001));
	      if( !startSelected ) {
		loadData.setStartAddr( -1 );
	      }
	      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
		if( fileInfo.getFileType() != 'C' ) {
		  loadData.setStartAddr( -1 );
		}

		// ggf. Dateikopf in Arbeitsspeicher kopieren
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

	      /*
	       * Warnung, wenn Dateityp unueblich beim
	       * gerade emulierten System ist
	       */
	      if( loadData.getStartAddr() >= 0 ) {
		if( ((isAC1 || isHGMC || isKramerMC || isZ1013)
				&& !fileFmt.equals( FileInfo.HEADERSAVE ))
		    || (isKC && !fileFmt.equals( FileInfo.KCC )
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

	      // Datei in Arbeitsspeicher laden und ggf. starten
	      if( !done ) {
		if( confirmLoadDataInfo( owner, loadData ) ) {
		  emuThread.loadIntoMemory( loadData );
		  showLoadMsg( screenFrm, loadData );
		  Main.setLastFile( file, "software" );
		  done = true;
		}
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

  public void changedUpdate( DocumentEvent e )
  {
    updStartButton();
  }


  public void insertUpdate( DocumentEvent e )
  {
    updStartButton();
  }


  public void removeUpdate( DocumentEvent e )
  {
    updStartButton();
  }


	/* --- ueberschriebene Methoden --- */

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
      if( fileName != null )
	setTitle( getTitle() + ": " + fileName );
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

    this.comboFileFmt = new JComboBox();
    this.comboFileFmt.setEditable( false );
    this.comboFileFmt.addItem( FileInfo.KCB );
    this.comboFileFmt.addItem( FileInfo.KCC );
    this.comboFileFmt.addItem( FileInfo.KCTAP_SYS );
    this.comboFileFmt.addItem( FileInfo.KCTAP_BASIC );
    this.comboFileFmt.addItem( FileInfo.KCBASIC_HEAD );
    this.comboFileFmt.addItem( FileInfo.KCBASIC_PURE );
    this.comboFileFmt.addItem( FileInfo.HEADERSAVE );
    this.comboFileFmt.addItem( FileInfo.INTELHEX );
    this.comboFileFmt.addItem( FileInfo.BIN );
    this.comboFileFmt.setSelectedItem( fileFmt );
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

    this.btnLoadForRAMBasic = new JRadioButton(
					"2C01h f\u00FCr RAM-BASIC",
					true );
    grpLoadBasic.add( this.btnLoadForRAMBasic );
    gbcLoad.insets.top    = 2;
    gbcLoad.insets.bottom = 0;
    gbcLoad.insets.left   = 40;
    gbcLoad.gridy++;
    panelLoad.add( this.btnLoadForRAMBasic, gbcLoad );

    this.btnLoadForROMBasic = new JRadioButton(
					"0401h f\u00FCr ROM-BASIC",
					false );
    grpLoadBasic.add( this.btnLoadForROMBasic );
    gbcLoad.insets.top    = 0;
    gbcLoad.insets.bottom = 5;
    gbcLoad.gridy++;
    panelLoad.add( this.btnLoadForROMBasic, gbcLoad );

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
    EmuSys emuSys = this.screenFrm.getEmuThread().getEmuSys();
    this.btnKeepHeader.setSelected(
		(emuSys instanceof Z1013)
		&& EmuUtil.parseBoolean(
			Main.getProperty( "jkcemu.loadsave.header.keep" ),
			false ) );
    updFields();


    /*
     * Bei einer BIN-Datei versuchen,
     * die Ladeadressen aus dem Dateinamen zu ermitteln
     */
    if( (file != null) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.BIN ) ) {
	String fileName = file.getName();
	if( fileName != null ) {
	  int len = fileName.length();
	  int pos = fileName.indexOf( '_' );
	  while( (pos >= 0) && ((pos + 4) < len) ) {
	    if( EmuUtil.isHexChar( fileName.charAt( pos + 1 ) )
		&& EmuUtil.isHexChar( fileName.charAt( pos + 2 ) )
		&& EmuUtil.isHexChar( fileName.charAt( pos + 3 ) )
		&& EmuUtil.isHexChar( fileName.charAt( pos + 4 ) ) )
	    {
	      this.fldLoadBegAddr.setText(
				fileName.substring( pos + 1, pos + 5 ) );
	      if( (pos + 9) < len ) {
		char ch = fileName.charAt( pos + 5 );
		if( ((ch == '_') || (ch == '-'))
		    && EmuUtil.isHexChar( fileName.charAt( pos + 6 ) )
		    && EmuUtil.isHexChar( fileName.charAt( pos + 7 ) )
		    && EmuUtil.isHexChar( fileName.charAt( pos + 8 ) )
		    && EmuUtil.isHexChar( fileName.charAt( pos + 9 ) ) )
		{
		  this.fldLoadEndAddr.setText(
				fileName.substring( pos + 6, pos + 10 ) );
		}
	      }
	      break;
	    }
	    if( pos + 5 < len ) {
	      pos = fileName.indexOf( '_', pos + 1 );
	    } else {
	      pos = -1;
	    }
	  }
	}
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  private static boolean confirmLoadDataInfo(
				Component owner,
				LoadData  loadData )
  {
    boolean rv      = true;
    String  infoMsg = loadData.getInfoMsg();
    if( infoMsg != null ) {
      if( infoMsg.length() > 0 ) {
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
    Object fileFmt = this.comboFileFmt.getSelectedItem();
    if( fileFmt != null ) {
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
	  EmuThread emuThread = this.screenFrm.getEmuThread();

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

	  // Datei in Arbeitsspeicher laden und ggf. starten
	  if( !startEnabled || !startSelected ) {
	    loadData.setStartAddr( -1 );
	  }
	  emuThread.loadIntoMemory( loadData );
	  showLoadMsg( this.screenFrm, loadData );
	  Main.setLastFile( this.file, "software" );
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
      screenFrm.toFront();
      screenFrm.showStatusText(
		String.format(
			"Datei nach %04X-%04X geladen",
			loadData.getBegAddr(),
			loadData.getEndAddr() ) );
    }
  }


  private void updFields()
  {
    boolean isHS           = false;
    boolean isKCB          = false;
    boolean isKCC          = false;
    boolean isKCTAP_SYS    = false;
    boolean isKCTAP_BASIC  = false;
    boolean isKCBASIC_HEAD = false;
    boolean isKCBASIC_PURE = false;
    boolean isINTELHEX     = false;
    int     begAddr        = -1;
    int     endAddr        = -1;
    int     startAddr      = -1;
    int     fileType       = -1;
    String  fileDesc       = null;

    Object fileFmt = this.comboFileFmt.getSelectedItem();
    if( fileFmt != null ) {
      isHS           = fileFmt.equals( FileInfo.HEADERSAVE );
      isKCB          = fileFmt.equals( FileInfo.KCB );
      isKCC          = fileFmt.equals( FileInfo.KCC );
      isKCTAP_SYS    = fileFmt.equals( FileInfo.KCTAP_SYS );
      isKCTAP_BASIC  = fileFmt.equals( FileInfo.KCTAP_BASIC );
      isKCBASIC_HEAD = fileFmt.equals( FileInfo.KCBASIC_HEAD );
      isKCBASIC_PURE = fileFmt.equals( FileInfo.KCBASIC_PURE );
      isINTELHEX     = fileFmt.equals( FileInfo.INTELHEX );

      begAddr = FileInfo.getBegAddr( this.fileBuf, fileFmt );
      if( begAddr >= 0 ) {
	endAddr   = FileInfo.getEndAddr( this.fileBuf, fileFmt );
	startAddr = FileInfo.getStartAddr( this.fileBuf, fileFmt );
      }
      fileType = FileInfo.getFileType( this.fileBuf, fileFmt );
      fileDesc = FileInfo.getFileDesc( this.fileBuf, fileFmt );
    }

    boolean stateBegAddr = (isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC
				|| isKCBASIC_HEAD || isKCBASIC_PURE
				|| isINTELHEX);
    this.labelInfoBegAddr.setEnabled( stateBegAddr );
    if( stateBegAddr && (begAddr >= 0) ) {
      this.fldInfoBegAddr.setText( String.format( "%04X", begAddr ) );
    } else {
      this.fldInfoBegAddr.setText( "" );
    }

    boolean stateEndAddr = (isHS || isKCB || isKCC
				|| isKCTAP_SYS || isKCTAP_BASIC
				|| isKCBASIC_HEAD || isKCBASIC_PURE);
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
				|| isKCTAP_SYS || isKCTAP_BASIC);
    this.labelInfoDesc.setEnabled( stateInfoDesc );
    if( stateInfoDesc && (fileDesc != null) ) {
      this.fldInfoDesc.setText( fileDesc );
    } else {
      this.fldInfoDesc.setText( "" );
    }

    boolean stateKCBasic = (isKCB || isKCTAP_BASIC
				|| isKCBASIC_HEAD || isKCBASIC_PURE);
    this.labelLoadBegAddr.setEnabled( !stateKCBasic );
    this.labelLoadEndAddr.setEnabled( !stateKCBasic );
    this.fldLoadBegAddr.setEditable( !stateKCBasic );
    this.fldLoadEndAddr.setEditable( !stateKCBasic );
    this.labelLoadBasicAddr.setEnabled( stateKCBasic );
    this.btnLoadForRAMBasic.setEnabled( stateKCBasic );
    this.btnLoadForROMBasic.setEnabled( stateKCBasic );
    if( stateKCBasic ) {
      if( this.screenFrm.getEmuThread().getEmuSys().hasKCBasicInROM() ) {
	this.btnLoadForROMBasic.setSelected( true );
      } else {
	this.btnLoadForRAMBasic.setSelected( true );
      }
    }

    EmuSys emuSys = this.screenFrm.getEmuThread().getEmuSys();
    this.btnKeepHeader.setEnabled(
			(emuSys instanceof Z1013)
			&& fileFmt.equals( FileInfo.HEADERSAVE ) );
    this.btnLoad.setEnabled( this.fileBuf.length > 0 );
    updStartButton( begAddr, endAddr, startAddr );
  }


  /*
   * Autostart nur ermoeglichen, wenn:
   *  1. Startadresse vorhanden ist
   *  2. Ladeadresse nicht angegeben oder
   *     mit der originalen Ladeadresse uebereinstimmt
   */
  private void updStartButton( int begAddr, int endAddr, int startAddr )
  {
    if( this.btnStart != null ) {
      boolean state = false;
      if( this.startEnabled ) {
	if( (startAddr >= 0) && (startAddr >= begAddr) ) {
	  state = true;
	}

	// Anfangsadresse muss mit der originalen uebereinstimmen
	String text = this.fldLoadBegAddr.getText();
	if( text != null ) {
	  if( text.length() > 0 ) {
	    if( (this.docLoadBegAddr.intValue() & 0xFFFF) != begAddr )
	      state = false;
	  }
	}

	// Endadresse muss gleich oder groesser der originalen sein
	text = this.fldLoadEndAddr.getText();
	if( text != null ) {
	  if( text.length() > 0 ) {
	    if( (this.docLoadEndAddr.intValue() & 0xFFFF) < endAddr )
	      state = false;
	  }
	}
      }
      this.btnStart.setEnabled( state );
    }
  }


  private void updStartButton()
  {
    if( this.btnStart != null ) {
      int    begAddr   = -1;
      int    endAddr   = -1;
      int    startAddr = -1;
      Object fileFmt   = this.comboFileFmt.getSelectedIndex();
      if( fileFmt != null ) {
	begAddr = FileInfo.getBegAddr( this.fileBuf, fileFmt );
	if( begAddr >= 0 ) {
	  endAddr   = FileInfo.getEndAddr( this.fileBuf, fileFmt );
	  startAddr = FileInfo.getStartAddr( this.fileBuf, fileFmt );
	}
      }
      updStartButton( begAddr, endAddr, startAddr );
    }
  }
}

