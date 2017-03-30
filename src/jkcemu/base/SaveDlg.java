/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Datei speichern
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.emusys.A5105;
import jkcemu.emusys.KC85;
import jkcemu.emusys.Z1013;
import jkcemu.emusys.Z9001;


public class SaveDlg extends BaseDlg implements DocumentListener
{
  private static final String HELP_PAGE = "/help/loadsave.htm";

  public static enum BasicType {
				NO_BASIC,
				TINYBASIC,
				MS_DERIVED_BASIC,
				MS_DERIVED_BASIC_HS,
				KCBASIC,
				RBASIC,
				OTHER_BASIC };

  private ScreenFrm                          screenFrm;
  private int                                begAddr;
  private int                                endAddr;
  private BasicType                          basicType;
  private javax.swing.filechooser.FileFilter basicFileFilter;
  private boolean                            kcbasic;
  private boolean                            fileTypeFixed;
  private boolean                            z9001;
  private boolean                            autoFillHeadBegAddr;
  private JTextField                         fldMemBegAddr;
  private JTextField                         fldMemEndAddr;
  private JTextField                         fldHeadBegAddr;
  private JTextField                         fldHeadStartAddr;
  private JTextField                         fldHeadFileDesc;
  private JComboBox<String>                  comboHeadFileType;
  private JLabel                             labelHeadBegAddr;
  private JLabel                             labelHeadStartAddr;
  private JLabel                             labelHeadFileType;
  private JLabel                             labelHeadFileDesc;
  private JCheckBox                          btnKeepHeader;
  private JRadioButton                       btnFileFmtBIN;
  private JRadioButton                       btnFileFmtKCC;
  private JRadioButton                       btnFileFmtTAP;
  private JRadioButton                       btnFileFmtSSS;
  private JRadioButton                       btnFileFmtBAS;
  private JRadioButton                       btnFileFmtRMC;
  private JRadioButton                       btnFileFmtHS;
  private JRadioButton                       btnFileFmtHEX;
  private JRadioButton                       btnBegBlkNum0;
  private JRadioButton                       btnBegBlkNum1;
  private JButton                            btnSave;
  private JButton                            btnHelp;
  private JButton                            btnCancel;
  private HexDocument                        docMemBegAddr;
  private HexDocument                        docMemEndAddr;
  private HexDocument                        docHeadBegAddr;
  private HexDocument                        docHeadStartAddr;
  private LimitedDocument                    docHeadFileDesc;
  private LimitedDocument                    docHeadFileType;


  public SaveDlg(
		ScreenFrm                          screenFrm,
		int                                begAddr,
		int                                endAddr,
		String                             title,
		BasicType                          basicType,
		javax.swing.filechooser.FileFilter basicFileFilter )
  {
    super( screenFrm, title );
    this.screenFrm           = screenFrm;
    this.begAddr             = begAddr;
    this.endAddr             = endAddr;
    this.basicType           = basicType;
    this.basicFileFilter     = basicFileFilter;
    this.kcbasic             = false;
    this.fileTypeFixed       = false;
    this.z9001               = false;
    this.autoFillHeadBegAddr = true;


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


    // Bereich Speicheradressen
    JPanel panelMem = new JPanel( new GridBagLayout() );
    panelMem.setBorder( BorderFactory.createTitledBorder(
					"Zu speichernder Bereich" ) );
    add( panelMem, gbc );

    GridBagConstraints gbcMem = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    // Anfangsadresse
    panelMem.add( new JLabel( "Anfangsadresse:" ), gbcMem );

    this.docMemBegAddr = new HexDocument(
				4,
				"Anfangsadresse des Speicherbereichs"  );
    this.fldMemBegAddr = new JTextField( this.docMemBegAddr, "", 5 );
    if( begAddr >= 0 ) {
      this.docMemBegAddr.setValue( begAddr, 4 );
    }
    gbcMem.fill    = GridBagConstraints.HORIZONTAL;
    gbcMem.weightx = 1.0;
    gbcMem.gridx++;
    panelMem.add( this.fldMemBegAddr, gbcMem );

    // Endadresse
    gbcMem.fill    = GridBagConstraints.NONE;
    gbcMem.weightx = 0.0;
    gbcMem.gridx++;
    panelMem.add( new JLabel( "Endadresse:" ), gbcMem );

    this.docMemEndAddr = new HexDocument(
				4,
				"Endadresse des Speicherbereichs"  );
    this.fldMemEndAddr = new JTextField( this.docMemEndAddr, "", 5 );
    if( (begAddr >= 0) && (endAddr >= 0) ) {
      this.fldMemEndAddr.setText( String.format( "%04X", endAddr ) );
    }
    gbcMem.fill    = GridBagConstraints.HORIZONTAL;
    gbcMem.weightx = 1.0;
    gbcMem.gridx++;
    panelMem.add( this.fldMemEndAddr, gbcMem );


    // Bereich Dateiformat
    JPanel panelFileFmt = new JPanel( new GridBagLayout() );
    panelFileFmt.setBorder( BorderFactory.createTitledBorder(
							"Dateiformat" ) );
    gbc.gridy++;
    add( panelFileFmt, gbc );

    GridBagConstraints gbcFileFmt = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpFileFmt = new ButtonGroup();

    this.btnFileFmtBIN = new JRadioButton(
			"Speicherabbilddatei ohne Kopfdaten (*.bin)",
			false );
    grpFileFmt.add( this.btnFileFmtBIN );
    panelFileFmt.add( this.btnFileFmtBIN, gbcFileFmt );

    this.btnFileFmtKCC = new JRadioButton( "KC-Systemdatei (*.kcc)", false );
    grpFileFmt.add( this.btnFileFmtKCC );
    gbcFileFmt.insets.top = 0;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtKCC, gbcFileFmt );

    this.btnFileFmtTAP = new JRadioButton( "KC-TAP-Datei (*.tap)", false );
    grpFileFmt.add( this.btnFileFmtTAP );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtTAP, gbcFileFmt );

    ButtonGroup grpBegBlkNum = new ButtonGroup();

    this.btnBegBlkNum0 = new JRadioButton(
		"Erster Block hat Nr. 0 (KC85/1, KC87, Z9001)" );
    grpBegBlkNum.add( this.btnBegBlkNum0 );
    gbcFileFmt.insets.left = 50;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnBegBlkNum0, gbcFileFmt );

    this.btnBegBlkNum1 = new JRadioButton(
		"Erster Block hat Nr. 1 (HC900, KC85/2-5, KC-BASIC)" );
    grpBegBlkNum.add( this.btnBegBlkNum1 );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnBegBlkNum1, gbcFileFmt );

    gbcFileFmt.insets.left = 5;

    this.btnFileFmtSSS = new JRadioButton(
				"KC-BASIC-Programmdatei (*.sss)" );
    grpFileFmt.add( this.btnFileFmtSSS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtSSS, gbcFileFmt );

    this.btnFileFmtBAS = new JRadioButton(
	"BASIC-/RBASIC-Programmdatei (*.bas; *.abc)" );
    grpFileFmt.add( this.btnFileFmtBAS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtBAS, gbcFileFmt );

    this.btnFileFmtRMC = new JRadioButton(
				"RBASIC-Maschinencodedatei (*.rmc)" );
    grpFileFmt.add( this.btnFileFmtRMC );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtRMC, gbcFileFmt );

    this.btnFileFmtHS = new JRadioButton( "Headersave-Datei (*.z80)" );
    grpFileFmt.add( this.btnFileFmtHS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtHS, gbcFileFmt );

    this.btnFileFmtHEX = new JRadioButton( "Intel-HEX-Datei (*.hex)" );
    grpFileFmt.add( this.btnFileFmtHEX );
    gbcFileFmt.insets.bottom = 5;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtHEX, gbcFileFmt );


    // Bereich Kopfdaten
    JPanel panelFileHead = new JPanel( new GridBagLayout() );
    panelFileHead.setBorder( BorderFactory.createTitledBorder( "Kopfdaten" ) );
    gbc.gridy++;
    add( panelFileHead, gbc );

    GridBagConstraints gbcFileHead = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Dateibezeichnung
    this.labelHeadFileDesc = new JLabel( "Bezeichnung:" );
    panelFileHead.add( this.labelHeadFileDesc, gbcFileHead );

    this.docHeadFileDesc = new LimitedDocument(
				FileFormat.getTotalMaxFileDescLength() );
    this.docHeadFileDesc.setAsciiOnly( true );
    this.fldHeadFileDesc  = new JTextField( this.docHeadFileDesc, "", 0 );
    gbcFileHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx   = 1.0;
    gbcFileHead.gridwidth = GridBagConstraints.REMAINDER;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadFileDesc, gbcFileHead );

    // Dateityp
    this.labelHeadFileType = new JLabel( "Typ:" );
    gbcFileHead.fill       = GridBagConstraints.NONE;
    gbcFileHead.weightx    = 0.0;
    gbcFileHead.insets.top = 5;
    gbcFileHead.gridx      = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadFileType, gbcFileHead );

    this.comboHeadFileType = new JComboBox<>();
    this.comboHeadFileType.setEditable( true );
    gbcFileHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx   = 1.0;
    gbcFileHead.gridwidth = GridBagConstraints.REMAINDER;
    gbcFileHead.gridx++;
    panelFileHead.add( this.comboHeadFileType, gbcFileHead );

    this.docHeadFileType = new LimitedDocument();
    this.docHeadFileType.setAsciiOnly( true );
    ComboBoxEditor editor = this.comboHeadFileType.getEditor();
    if( editor != null ) {
      Component c = editor.getEditorComponent();
      if( c != null ) {
	if( c instanceof JTextComponent ) {
	  ((JTextComponent) c).setDocument( this.docHeadFileType );
	}
      }
    }

    Font font = this.fldHeadFileDesc.getFont();
    if( font != null ) {
      this.comboHeadFileType.setFont( font );
    }

    // Anfangsadresse
    this.labelHeadBegAddr = new JLabel( "Anfangsadresse:" );
    gbcFileHead.fill      = GridBagConstraints.NONE;
    gbcFileHead.weightx   = 0.0;
    gbcFileHead.gridwidth = 1;
    gbcFileHead.gridx     = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadBegAddr, gbcFileHead );

    this.docHeadBegAddr = new HexDocument(
				4,
				"Anfangsadresse in den Kopfdaten" );
    this.fldHeadBegAddr = new JTextField( this.docHeadBegAddr, "", 5 );
    if( begAddr >= 0 ) {
      this.fldHeadBegAddr.setText( String.format( "%04X", begAddr ) );
    }
    gbcFileHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx = 0.5;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadBegAddr, gbcFileHead );

    // Startadresse
    this.labelHeadStartAddr = new JLabel( "Startadresse:" );
    gbcFileHead.fill        = GridBagConstraints.NONE;
    gbcFileHead.weightx     = 0.0;
    gbcFileHead.gridx++;
    panelFileHead.add( this.labelHeadStartAddr, gbcFileHead );

    this.docHeadStartAddr = new HexDocument(
				4,
				"Startadresse in den Kopfdaten" );
    this.fldHeadStartAddr = new JTextField( this.docHeadStartAddr, "", 5 );
    gbcFileHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx = 0.5;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadStartAddr, gbcFileHead );

    // Schalter fuer Kopfdaten in Arbeitsspeicher kopieren
    this.btnKeepHeader = new JCheckBox(
			"Headersave-Kopfdaten nach 00E0-00FF kopieren" );
    gbcFileHead.fill      = GridBagConstraints.NONE;
    gbcFileHead.gridwidth = GridBagConstraints.REMAINDER;
    gbcFileHead.gridx     = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.btnKeepHeader, gbcFileHead );


    // Bereich Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 3, 1, 5, 5 ) );
    gbc.anchor     = GridBagConstraints.NORTHWEST;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weightx    = 0.0;
    gbc.gridx      = 1;
    gbc.gridy      = 0;
    add( panelBtn, gbc );

    this.btnSave = new JButton( "Speichern" );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe" );
    panelBtn.add( this.btnHelp );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Vorbelegungen
    boolean basEnabled = false;
    boolean sssEnabled = false;
    boolean kc85       = false;
    boolean basic      = false;
    int hsFileType     = -1;
    switch( basicType ) {
      case TINYBASIC:
	this.btnFileFmtHS.setSelected( true );
	hsFileType = 'b';
	basic      = true;
	break;
      case MS_DERIVED_BASIC:
	this.btnFileFmtBAS.setSelected( true );
	hsFileType = 'B';
	basEnabled = true;
	basic      = true;
	break;
      case MS_DERIVED_BASIC_HS:
	this.btnFileFmtHS.setSelected( true );
	hsFileType = 'B';
	basEnabled = true;
	basic      = true;
	break;
      case KCBASIC:
	this.btnFileFmtSSS.setSelected( true );
	this.btnBegBlkNum1.setSelected( true );
	this.kcbasic = true;
	hsFileType   = 'B';
	sssEnabled   = true;
	basic        = true;
	break;
      case RBASIC:
	this.btnFileFmtBAS.setSelected( true );
	hsFileType = 'B';
	basEnabled = true;
	basic      = true;
	break;
    }
    this.btnFileFmtBAS.setEnabled( basEnabled );
    this.btnFileFmtSSS.setEnabled( sssEnabled );
    if( hsFileType > 0 ) {
      this.fileTypeFixed = true;
      if( EmuUtil.setSelectedHeadersaveFileTypeItem(
					this.comboHeadFileType,
					hsFileType ) )
      {
	this.comboHeadFileType.setEditable( false );
	this.comboHeadFileType.setEnabled( false );
      }
    }
    if( !basic ) {
      boolean a5105  = false;
      boolean z1013  = false;
      EmuSys  emuSys = this.screenFrm.getEmuThread().getEmuSys();
      if( emuSys != null ) {
	a5105      = (emuSys instanceof A5105);
	kc85       = (emuSys instanceof KC85);
	z1013      = (emuSys instanceof Z1013);
	this.z9001 = (emuSys instanceof Z9001);
      }
      if( a5105 ) {
	this.btnFileFmtRMC.setSelected( true );
      } else if( kc85 || this.z9001 ) {
	this.btnFileFmtKCC.setSelected( true );
      } else if( z1013 ) {
	this.btnFileFmtHS.setSelected( true );
	this.btnKeepHeader.setSelected(
		Main.getBooleanProperty( LoadDlg.PROP_KEEP_HEADER, false ) );
      } else {
	this.btnFileFmtBIN.setSelected( true );
      }
    }
    if( basic || kc85 ) {
      this.btnBegBlkNum1.setSelected( true );
    } else {
      this.btnBegBlkNum0.setSelected( true );
    }
    updHeadFields();


    // Fenstergroesse und -position
    pack();
    this.fldMemBegAddr.setColumns( 0 );
    this.fldMemEndAddr.setColumns( 0 );
    this.fldMemEndAddr.setColumns( 0 );
    this.fldHeadBegAddr.setColumns( 0 );
    this.fldHeadStartAddr.setColumns( 0 );
    setParentCentered();
    setResizable( true );


    // sonstiges
    updSaveBtn();
    this.fldMemBegAddr.addActionListener( this );
    this.docMemBegAddr.addDocumentListener( this );
    this.fldMemEndAddr.addActionListener( this );
    this.docMemEndAddr.addDocumentListener( this );
    this.btnFileFmtBIN.addActionListener( this );
    this.btnFileFmtKCC.addActionListener( this );
    this.btnFileFmtTAP.addActionListener( this );
    this.btnBegBlkNum0.addActionListener( this );
    this.btnBegBlkNum1.addActionListener( this );
    this.btnFileFmtSSS.addActionListener( this );
    this.btnFileFmtBAS.addActionListener( this );
    this.btnFileFmtRMC.addActionListener( this );
    this.btnFileFmtHS.addActionListener( this );
    this.btnFileFmtHEX.addActionListener( this );
    this.fldHeadFileDesc.addActionListener( this );
    this.fldHeadBegAddr.addActionListener( this );
    this.docHeadBegAddr.addDocumentListener( this );
    this.fldHeadStartAddr.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnSave.addKeyListener( this );
    this.btnHelp.addActionListener( this );
    this.btnHelp.addKeyListener( this );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
  }


	/* --- Methoden fuer DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docContentChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docContentChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docContentChanged( e );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void windowOpened( WindowEvent e )
  {
    this.fldMemBegAddr.requestFocus();
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnFileFmtBIN)
	  || (src == this.btnFileFmtKCC)
	  || (src == this.btnFileFmtTAP)
	  || (src == this.btnBegBlkNum0)
	  || (src == this.btnBegBlkNum1)
 	  || (src == this.btnFileFmtSSS)
	  || (src == this.btnFileFmtBAS)
	  || (src == this.btnFileFmtRMC)
	  || (src == this.btnFileFmtHS)
	  || (src == this.btnFileFmtHEX) )
     {
	rv = true;
	updHeadFields();
      }
      else if( (src == this.fldHeadFileDesc)
	       || (src == this.btnSave) )
      {
	rv = true;
	doSave();
      }
      else if( src == this.btnHelp ) {
	rv = true;
	HelpFrm.open( HELP_PAGE );
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
    return rv;
  }


	/* --- private Methoden --- */

  private void doSave()
  {
    boolean    saved   = false;
    FileFormat fileFmt = getSelectedFileFmt();
    try {
      if( fileFmt == null ) {
	throw new UserInputException( "Dateiformat nicht ausgew\u00E4hlt" );
      }

      // Adressbereich pruefen
      int begAddr = this.docMemBegAddr.intValue() & 0xFFFF;
      int endAddr = this.docMemEndAddr.intValue() & 0xFFFF;
      if( (this.begAddr >= 0) && (this.endAddr >= 0)
	  && ((begAddr > this.begAddr) || (endAddr < this.endAddr)) )
      {
	throw new UserInputException(
		String.format(
			"Der angegebene Adressbereich beinhaltet nicht"
				+ " vollst\u00E4ndig\n"
				+ "das zu speichernde Programm (%04X-%04X).",
			this.begAddr,
			this.endAddr ) );
      }

      // Dateibezeichnung pruefen
      String headFileDesc = this.fldHeadFileDesc.getText();
      if( headFileDesc != null ) {
	int m = fileFmt.getMaxFileDescLength();
	if( m > 0 ) {
	  if( headFileDesc.length() > m ) {
	    throw new UserInputException(
			String.format(
				"Die Bezeichnung der Datei ist zu lang"
					+ "(max. %d Zeichen).",
				m ) );
	  }
	}
      }

      // Dateiformat
      boolean isBIN = this.btnFileFmtBIN.isSelected();
      boolean isKCC = this.btnFileFmtKCC.isSelected();
      boolean isTAP = this.btnFileFmtTAP.isSelected();
      boolean isSSS = this.btnFileFmtSSS.isSelected();
      boolean isBAS = this.btnFileFmtBAS.isSelected();
      boolean isRMC = this.btnFileFmtRMC.isSelected();
      boolean isHS  = this.btnFileFmtHS.isSelected();
      boolean isHEX = this.btnFileFmtHEX.isSelected();

      String                             title      = "Datei speichern";
      javax.swing.filechooser.FileFilter fileFilter = null;
      if( isBIN ) {
	title      = "Bin\u00E4rdatei speichern";
	fileFilter = EmuUtil.getBinaryFileFilter();
      }
      else if( isKCC ) {
	title      = "KC-System-Datei speichern";
	fileFilter = EmuUtil.getKCSystemFileFilter();
      }
      else if( isTAP ) {
	title      = "KC-TAP-Datei speichern";
	fileFilter = EmuUtil.getKCTapFileFilter();
      }
      else if( isSSS ) {
	title      = "KC-BASIC-Programmdatei speichern";
	fileFilter = EmuUtil.getKCBasicFileFilter();
      }
      else if( isBAS ) {
	if( this.basicType.equals( BasicType.RBASIC ) ) {
	  title = "RBASIC-Programmdatei speichern";
	} else {
	  title = "BASIC-Programmdatei speichern";
	}
	if( this.basicFileFilter != null ) {
	  fileFilter = this.basicFileFilter;
	} else {
	  fileFilter = EmuUtil.getBasicFileFilter();
	}
      }
      else if( isRMC ) {
	title      = "RBASIC-Maschinencodedatei speichern";
	fileFilter = EmuUtil.getRMCFileFilter();
      }
      else if( isHS ) {
	title      = "Headersave-Datei speichern";
	fileFilter = EmuUtil.getHeadersaveFileFilter();
      }
      else if( isHEX ) {
	title      = "Intel-HEX-Datei speichern";
	fileFilter = EmuUtil.getHexFileFilter();
      }
      String prefBasicFmt = null;
      switch( this.basicType ) {
	case MS_DERIVED_BASIC:
	case MS_DERIVED_BASIC_HS:
	case RBASIC:
	  if( !isBAS && !isHS ) {
	    prefBasicFmt = this.btnFileFmtBAS.getText();
	  }
	  break;
	case KCBASIC:
	  if( !isTAP && !isSSS && !isHS ) {
	    prefBasicFmt = this.btnFileFmtSSS.getText();
	  }
	  break;
      }
      boolean status = true;
      if( prefBasicFmt != null ) {
	status = showYesNoWarningDlg(
			this,
			"Das von Ihnen ausgew\u00E4hlte Dateiformat wird"
				+ " \u00FCblicherweise\n"
				+ "nicht zum Speichern von BASIC-Programmen"
				+ " verwendet.\n"
				+ "Zwar k\u00F6nnen Sie das Programm"
				+ " in dem Format speichern, jedoch\n"
				+ "wird JKCEMU beim Laden der Datei"
				+ " das BASIC-Programm nicht erkennen\n"
				+ "und somit die Systemzellen des"
				+ " BASIC-Interpreters nicht anpassen.\n"
				+ "Dadurch l\u00E4sst sich das Programm"
				+ " dann nicht nutzen.\n\n"
				+ "Das bevorzugte Dateiformat zum Speichern"
				+ " von BASIC-Programmen ist:\n\n    "
				+ prefBasicFmt
				+ "\n\nM\u00F6chten Sie trotzdem in dem"
				+ " von Ihnen gew\u00E4hlten Dateiformat"
				+ " speichern?",
			"Warnung" );
      }
      if( status ) {

	// Dateiauswahldialog
	File file = EmuUtil.showFileSaveDlg(
			this.screenFrm,
			title,
			Main.getLastDirFile( Main.FILE_GROUP_SOFTWARE ),
			fileFilter );
	if( file != null ) {
	  try {
	    int     headBegAddr   = begAddr;
	    Integer headStartAddr = null;
	    String  headFileType  = null;
	    if( isKCC || (isTAP && !this.kcbasic) || isHS || isHEX ) {
	      String headBegAddrText = this.fldHeadBegAddr.getText();
	      if( headBegAddrText != null ) {
		if( !headBegAddrText.trim().isEmpty() ) {
		  headBegAddr = this.docHeadBegAddr.intValue() & 0xFFFF;
		}
	      }
	    }
	    if( isKCC || (isTAP && !this.kcbasic) || isRMC || isHS ) {
	      String s = this.fldHeadStartAddr.getText();
	      if( s != null ) {
		if( !s.trim().isEmpty() ) {
		  headStartAddr = (this.docHeadStartAddr.intValue() & 0xFFFF);
		}
	      }
	    }
	    Object o = this.comboHeadFileType.getSelectedItem();
	    if( o != null ) {
	      headFileType = o.toString();
	    }
	    EmuThread emuThread = this.screenFrm.getEmuThread();
	    FileSaver.saveFile(
			file,
			fileFmt,
			emuThread,
			begAddr,
			endAddr,
			!this.basicType.equals( BasicType.NO_BASIC ),
			headBegAddr,
			headStartAddr,
			headFileDesc,
			headFileType,
			this.btnKeepHeader.isSelected() ? emuThread : null );
	    saved = true;
	    Main.setLastFile( file, Main.FILE_GROUP_SOFTWARE );
	    this.screenFrm.showStatusText( "Datei gespeichert" );
	  }
	  catch( IOException ex ) {
	    showErrorDlg(
		  this,
		  "Datei kann nicht gespeichert werden.\n\n"
			  + ex.getMessage() );
	  }
	  catch( Exception ex ) {
	    showErrorDlg( this, ex.getMessage() );
	  }
	}
      }
    }
    catch( UserInputException ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
    if( saved ) {
      doClose();
    }
  }


  private void docContentChanged( DocumentEvent e )
  {
    if( this.autoFillHeadBegAddr ) {
      Document doc = e.getDocument();
      if( doc == this.docMemBegAddr ) {
	this.fldHeadBegAddr.setText( this.fldMemBegAddr.getText() );
      }
      else if( (doc == this.docHeadBegAddr)
	       && this.fldHeadBegAddr.hasFocus() )
      {
	this.autoFillHeadBegAddr = false;
      }
    }
    updSaveBtn();
  }


  private FileFormat getSelectedFileFmt()
  {
    FileFormat rv = null;
    if( this.btnFileFmtBIN.isSelected() ) {
      rv = FileFormat.BIN;
    }
    else if( this.btnFileFmtKCC.isSelected() ) {
      rv = FileFormat.KCC;
    }
    else if( this.btnFileFmtTAP.isSelected() ) {
      if( this.kcbasic ) {
	rv = FileFormat.KCTAP_BASIC_PRG;
      } else {
	rv = (this.btnBegBlkNum0.isSelected() ?
				FileFormat.KCTAP_Z9001
				: FileFormat.KCTAP_KC85);
      }
    }
    else if( this.btnFileFmtSSS.isSelected() ) {
      rv = FileFormat.KCBASIC_PRG;
    }
    else if( this.btnFileFmtBAS.isSelected() ) {
      if( this.basicType == BasicType.RBASIC ) {
	rv = FileFormat.RBASIC_PRG;
      } else {
	rv = FileFormat.BASIC_PRG;
      }
    }
    else if( this.btnFileFmtRMC.isSelected() ) {
      rv = FileFormat.RMC;
    }
    else if( this.btnFileFmtHS.isSelected() ) {
      rv = FileFormat.HEADERSAVE;
    }
    else if( this.btnFileFmtHEX.isSelected() ) {
      rv = FileFormat.INTELHEX;
    }
    return rv;
  }


  private void updHeadFields()
  {
    boolean stateKCC = this.btnFileFmtKCC.isSelected();
    boolean stateTAP = this.btnFileFmtTAP.isSelected();
    boolean stateSSS = this.btnFileFmtSSS.isSelected();
    boolean stateBAS = this.btnFileFmtBAS.isSelected();
    boolean stateRMC = this.btnFileFmtRMC.isSelected();
    boolean stateHS  = this.btnFileFmtHS.isSelected();
    boolean stateHEX = this.btnFileFmtHEX.isSelected();

    this.btnBegBlkNum0.setEnabled( stateTAP && !this.kcbasic );
    this.btnBegBlkNum1.setEnabled( stateTAP && !this.kcbasic );

    boolean stateMemBegAddr = true;
    if( (this.begAddr >= 0)
	&& ((stateTAP && this.kcbasic) || stateSSS || stateBAS) )
    {
      stateMemBegAddr = false;
      this.docMemBegAddr.setValue( this.begAddr, 4 );
    }
    this.fldMemBegAddr.setEnabled( stateMemBegAddr );

    boolean stateHeadBegAddr = stateMemBegAddr
	&& (stateKCC || (stateTAP && !this.kcbasic)
		|| stateHS || stateRMC || stateHEX);
    this.labelHeadBegAddr.setEnabled( stateHeadBegAddr );
    this.fldHeadBegAddr.setEnabled( stateHeadBegAddr );

    boolean stateHeadStartAddr = stateMemBegAddr
	&& (stateKCC || (stateTAP && !this.kcbasic) || stateHS || stateRMC);
    this.labelHeadStartAddr.setEnabled( stateHeadStartAddr );
    this.fldHeadStartAddr.setEnabled( stateHeadStartAddr );

    String oldDesc    = this.fldHeadFileDesc.getText();
    int    maxDescLen = 0;
    int    maxTypeLen = 0;
    if( !this.fileTypeFixed ) {
      this.comboHeadFileType.removeAllItems();
      if( stateHS ) {
	EmuUtil.addHeadersaveFileTypeItemsTo( this.comboHeadFileType );
	maxDescLen = 16;
	maxTypeLen = 1;
      } else if( stateKCC || stateTAP ) {
	if( (this.z9001 && stateKCC)
	    || (stateTAP && this.btnBegBlkNum0.isSelected()) )
	{
	  maxDescLen = 8;
	  maxTypeLen = 3;
	  this.comboHeadFileType.addItem( "" );
	  this.comboHeadFileType.addItem( "COM" );
	} else {
	  maxDescLen = 11;
	}
      }
    }
    this.docHeadFileDesc.setMaxLength( maxDescLen );
    this.fldHeadFileDesc.setText( oldDesc );
    this.docHeadFileType.setMaxLength( maxTypeLen );
    this.labelHeadFileType.setEnabled( maxTypeLen > 0 );
    this.comboHeadFileType.setEnabled( maxTypeLen > 0 );

    boolean stateFileDesc = (stateKCC || stateTAP || stateHS);
    this.labelHeadFileDesc.setEnabled( stateFileDesc );
    this.fldHeadFileDesc.setEnabled( stateFileDesc );

    this.btnKeepHeader.setEnabled(
	stateHS
	&& (this.screenFrm.getEmuThread().getEmuSys() instanceof Z1013) );
  }


  private void updSaveBtn()
  {
    if( btnSave != null ) {
      boolean state = false;
      if( (this.docMemBegAddr.getLength() > 0)
	  && (this.docMemEndAddr.getLength() > 0) )
      {
	try {
	  if( this.docMemBegAddr.intValue()
			<= this.docMemEndAddr.intValue() )
	  {
	    state = true;
	  }
	}
	catch( NumberFormatException ex ) {}
      }
      this.btnSave.setEnabled( state );
    }
  }
}
