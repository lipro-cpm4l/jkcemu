/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Datei speichern
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
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
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.HexDocument;
import jkcemu.base.LimitedDocument;
import jkcemu.base.ScreenFrm;
import jkcemu.base.UserInputException;
import jkcemu.emusys.A5105;
import jkcemu.emusys.KC85;
import jkcemu.emusys.NANOS;
import jkcemu.emusys.Z1013;
import jkcemu.emusys.Z9001;
import jkcemu.file.FileUtil;


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
  private JRadioButton                       rbFileFmtBIN;
  private JRadioButton                       rbFileFmtKCC;
  private JRadioButton                       rbFileFmtTAP;
  private JRadioButton                       rbFileFmtSSS;
  private JRadioButton                       rbFileFmtBAS;
  private JRadioButton                       rbFileFmtRMC;
  private JRadioButton                       rbFileFmtHS;
  private JRadioButton                       rbFileFmtHEX;
  private JRadioButton                       rbFileFmtCOM;
  private JRadioButton                       rbBegBlkNum0;
  private JRadioButton                       rbBegBlkNum1;
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
    JPanel panelMem = GUIFactory.createPanel( new GridBagLayout() );
    panelMem.setBorder( GUIFactory.createTitledBorder(
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
    panelMem.add( GUIFactory.createLabel( "Anfangsadresse:" ), gbcMem );

    this.docMemBegAddr = new HexDocument(
				4,
				"Anfangsadresse des Speicherbereichs"  );
    this.fldMemBegAddr = GUIFactory.createTextField( this.docMemBegAddr, 5 );
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
    panelMem.add( GUIFactory.createLabel( "Endadresse:" ), gbcMem );

    this.docMemEndAddr = new HexDocument(
				4,
				"Endadresse des Speicherbereichs"  );
    this.fldMemEndAddr = GUIFactory.createTextField( this.docMemEndAddr, 5 );
    if( (begAddr >= 0) && (endAddr >= 0) ) {
      this.fldMemEndAddr.setText( String.format( "%04X", endAddr ) );
    }
    gbcMem.fill    = GridBagConstraints.HORIZONTAL;
    gbcMem.weightx = 1.0;
    gbcMem.gridx++;
    panelMem.add( this.fldMemEndAddr, gbcMem );


    // Bereich Dateiformat
    JPanel panelFileFmt = GUIFactory.createPanel( new GridBagLayout() );
    panelFileFmt.setBorder( GUIFactory.createTitledBorder(
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

    this.rbFileFmtBIN = GUIFactory.createRadioButton(
			"Speicherabbilddatei ohne Kopfdaten (*.bin)" );
    grpFileFmt.add( this.rbFileFmtBIN );
    panelFileFmt.add( this.rbFileFmtBIN, gbcFileFmt );

    this.rbFileFmtKCC = GUIFactory.createRadioButton(
					"KC-Systemdatei (*.kcc)" );
    grpFileFmt.add( this.rbFileFmtKCC );
    gbcFileFmt.insets.top = 0;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtKCC, gbcFileFmt );

    this.rbFileFmtTAP = GUIFactory.createRadioButton(
					"KC-TAP-Datei (*.tap)" );
    grpFileFmt.add( this.rbFileFmtTAP );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtTAP, gbcFileFmt );

    ButtonGroup grpBegBlkNum = new ButtonGroup();

    this.rbBegBlkNum0 = GUIFactory.createRadioButton(
		"Erster Block hat Nr. 0 (KC85/1, KC87, Z9001)" );
    grpBegBlkNum.add( this.rbBegBlkNum0 );
    gbcFileFmt.insets.left = 50;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbBegBlkNum0, gbcFileFmt );

    this.rbBegBlkNum1 = GUIFactory.createRadioButton(
		"Erster Block hat Nr. 1 (HC900, KC85/2-5, KC-BASIC)" );
    grpBegBlkNum.add( this.rbBegBlkNum1 );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbBegBlkNum1, gbcFileFmt );

    gbcFileFmt.insets.left = 5;

    this.rbFileFmtSSS = GUIFactory.createRadioButton(
				"KC-BASIC-Programmdatei (*.sss)" );
    grpFileFmt.add( this.rbFileFmtSSS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtSSS, gbcFileFmt );

    this.rbFileFmtBAS = GUIFactory.createRadioButton(
	"BASIC-/RBASIC-Programmdatei (*.bas; *.abc)" );
    grpFileFmt.add( this.rbFileFmtBAS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtBAS, gbcFileFmt );

    this.rbFileFmtRMC = GUIFactory.createRadioButton(
				"RBASIC-Maschinencodedatei (*.rmc)" );
    grpFileFmt.add( this.rbFileFmtRMC );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtRMC, gbcFileFmt );

    this.rbFileFmtHS = GUIFactory.createRadioButton(
				"Headersave-Datei (*.z80)" );
    grpFileFmt.add( this.rbFileFmtHS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtHS, gbcFileFmt );

    this.rbFileFmtHEX = GUIFactory.createRadioButton(
				"Intel-HEX-Datei (*.hex)" );
    grpFileFmt.add( this.rbFileFmtHEX );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtHEX, gbcFileFmt );

    this.rbFileFmtCOM = GUIFactory.createRadioButton(
			"CP/M-kompatible Programmdatei (*.com)" );
    grpFileFmt.add( this.rbFileFmtCOM );
    gbcFileFmt.insets.bottom = 5;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.rbFileFmtCOM, gbcFileFmt );


    // Bereich Kopfdaten
    JPanel panelFileHead = GUIFactory.createPanel( new GridBagLayout() );
    panelFileHead.setBorder( GUIFactory.createTitledBorder( "Kopfdaten" ) );
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
    this.labelHeadFileDesc = GUIFactory.createLabel( "Bezeichnung:" );
    panelFileHead.add( this.labelHeadFileDesc, gbcFileHead );

    this.docHeadFileDesc = new LimitedDocument(
				FileFormat.getTotalMaxFileDescLength() );
    this.docHeadFileDesc.setAsciiOnly( true );
    this.fldHeadFileDesc  = GUIFactory.createTextField(
						this.docHeadFileDesc,
						0 );
    gbcFileHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx   = 1.0;
    gbcFileHead.gridwidth = GridBagConstraints.REMAINDER;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadFileDesc, gbcFileHead );

    // Dateityp
    this.labelHeadFileType = GUIFactory.createLabel( "Typ:" );
    gbcFileHead.fill       = GridBagConstraints.NONE;
    gbcFileHead.weightx    = 0.0;
    gbcFileHead.insets.top = 5;
    gbcFileHead.gridx      = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadFileType, gbcFileHead );

    this.comboHeadFileType = GUIFactory.createComboBox();
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
    this.labelHeadBegAddr = GUIFactory.createLabel( "Anfangsadresse:" );
    gbcFileHead.fill      = GridBagConstraints.NONE;
    gbcFileHead.weightx   = 0.0;
    gbcFileHead.gridwidth = 1;
    gbcFileHead.gridx     = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadBegAddr, gbcFileHead );

    this.docHeadBegAddr = new HexDocument(
				4,
				"Anfangsadresse in den Kopfdaten" );
    this.fldHeadBegAddr = GUIFactory.createTextField(
						this.docHeadBegAddr,
						5 );
    if( begAddr >= 0 ) {
      this.fldHeadBegAddr.setText( String.format( "%04X", begAddr ) );
    }
    gbcFileHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx = 0.5;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadBegAddr, gbcFileHead );

    // Startadresse
    this.labelHeadStartAddr = GUIFactory.createLabel( "Startadresse:" );
    gbcFileHead.fill        = GridBagConstraints.NONE;
    gbcFileHead.weightx     = 0.0;
    gbcFileHead.gridx++;
    panelFileHead.add( this.labelHeadStartAddr, gbcFileHead );

    this.docHeadStartAddr = new HexDocument(
				4,
				"Startadresse in den Kopfdaten" );
    this.fldHeadStartAddr = GUIFactory.createTextField(
						this.docHeadStartAddr,
						5 );
    gbcFileHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx = 0.5;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadStartAddr, gbcFileHead );


    // Bereich Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 3, 1, 5, 5 ) );
    gbc.anchor     = GridBagConstraints.NORTHWEST;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weightx    = 0.0;
    gbc.gridx      = 1;
    gbc.gridy      = 0;
    add( panelBtn, gbc );

    this.btnSave = GUIFactory.createButtonSave();
    panelBtn.add( this.btnSave );

    this.btnHelp = GUIFactory.createButtonHelp();
    panelBtn.add( this.btnHelp );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Vorbelegungen
    boolean basEnabled = false;
    boolean sssEnabled = false;
    boolean kc85       = false;
    boolean basic      = false;
    int hsFileType     = -1;
    switch( basicType ) {
      case TINYBASIC:
	this.rbFileFmtHS.setSelected( true );
	hsFileType = 'b';
	basic      = true;
	break;
      case MS_DERIVED_BASIC:
	this.rbFileFmtBAS.setSelected( true );
	hsFileType = 'B';
	basEnabled = true;
	basic      = true;
	break;
      case MS_DERIVED_BASIC_HS:
	this.rbFileFmtHS.setSelected( true );
	hsFileType = 'B';
	basEnabled = true;
	basic      = true;
	break;
      case KCBASIC:
	this.rbFileFmtSSS.setSelected( true );
	this.rbBegBlkNum1.setSelected( true );
	this.kcbasic = true;
	hsFileType   = 'B';
	sssEnabled   = true;
	basic        = true;
	break;
      case RBASIC:
	this.rbFileFmtBAS.setSelected( true );
	hsFileType = 'B';
	basEnabled = true;
	basic      = true;
	break;
    }
    this.rbFileFmtBAS.setEnabled( basEnabled );
    this.rbFileFmtSSS.setEnabled( sssEnabled );
    if( hsFileType > 0 ) {
      this.fileTypeFixed = true;
      if( FileUtil.setSelectedHeadersaveFileTypeItem(
					this.comboHeadFileType,
					hsFileType ) )
      {
	this.comboHeadFileType.setEditable( false );
	this.comboHeadFileType.setEnabled( false );
      }
    }
    if( !basic ) {
      boolean a5105  = false;
      boolean nanos  = false;
      boolean z1013  = false;
      EmuSys  emuSys = this.screenFrm.getEmuThread().getEmuSys();
      if( emuSys != null ) {
	a5105      = (emuSys instanceof A5105);
	kc85       = (emuSys instanceof KC85);
	nanos      = (emuSys instanceof NANOS);
	z1013      = (emuSys instanceof Z1013);
	this.z9001 = (emuSys instanceof Z9001);
      }
      if( a5105 ) {
	this.rbFileFmtRMC.setSelected( true );
      } else if( kc85 || this.z9001 ) {
	this.rbFileFmtKCC.setSelected( true );
      } else if( nanos ) {
	this.rbFileFmtCOM.setSelected( true );
      } else if( z1013 ) {
	this.rbFileFmtHS.setSelected( true );
      } else {
	this.rbFileFmtBIN.setSelected( true );
      }
    }
    if( basic || kc85 ) {
      this.rbBegBlkNum1.setSelected( true );
    } else {
      this.rbBegBlkNum0.setSelected( true );
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
    this.rbFileFmtBIN.addActionListener( this );
    this.rbFileFmtKCC.addActionListener( this );
    this.rbFileFmtTAP.addActionListener( this );
    this.rbBegBlkNum0.addActionListener( this );
    this.rbBegBlkNum1.addActionListener( this );
    this.rbFileFmtSSS.addActionListener( this );
    this.rbFileFmtBAS.addActionListener( this );
    this.rbFileFmtRMC.addActionListener( this );
    this.rbFileFmtHS.addActionListener( this );
    this.rbFileFmtHEX.addActionListener( this );
    this.rbFileFmtCOM.addActionListener( this );
    this.fldHeadFileDesc.addActionListener( this );
    this.fldHeadBegAddr.addActionListener( this );
    this.docHeadBegAddr.addDocumentListener( this );
    this.fldHeadStartAddr.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnCancel.addActionListener( this );
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
      if( (src == this.rbFileFmtBIN)
	  || (src == this.rbFileFmtKCC)
	  || (src == this.rbFileFmtTAP)
	  || (src == this.rbBegBlkNum0)
	  || (src == this.rbBegBlkNum1)
 	  || (src == this.rbFileFmtSSS)
	  || (src == this.rbFileFmtBAS)
	  || (src == this.rbFileFmtRMC)
	  || (src == this.rbFileFmtHS)
	  || (src == this.rbFileFmtHEX)
	  || (src == this.rbFileFmtCOM) )
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
    return rv;
  }


	/* --- private Methoden --- */

  private void doSave()
  {
    boolean saved = false;
    try {
      javax.swing.filechooser.FileFilter fileFilter = null;

      boolean isBIN = this.rbFileFmtBIN.isSelected();
      boolean isKCC = this.rbFileFmtKCC.isSelected();
      boolean isTAP = this.rbFileFmtTAP.isSelected();
      boolean isSSS = this.rbFileFmtSSS.isSelected();
      boolean isBAS = this.rbFileFmtBAS.isSelected();
      boolean isRMC = this.rbFileFmtRMC.isSelected();
      boolean isHS  = this.rbFileFmtHS.isSelected();
      boolean isHEX = this.rbFileFmtHEX.isSelected();
      boolean isCOM = this.rbFileFmtCOM.isSelected();

      String     title   = "Datei speichern";
      FileFormat fileFmt = getSelectedFileFmt();
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
      String  headFileDesc = this.fldHeadFileDesc.getText();
      boolean status       = FileUtil.checkFileDesc(
				this,
				headFileDesc,
				this.docHeadFileDesc.getMaxLength() );

      // Dateiformat
      if( status ) {
	if( isBIN ) {
	  title      = "Bin\u00E4rdatei speichern";
	  fileFilter = FileUtil.getBinaryFileFilter();
	}
	else if( isKCC ) {
	  title      = "KC-System-Datei speichern";
	  fileFilter = FileUtil.getKCSystemFileFilter();
	}
	else if( isTAP ) {
	  title      = "KC-TAP-Datei speichern";
	  fileFilter = FileUtil.getKCTapFileFilter();
	}
	else if( isSSS ) {
	  title      = "KC-BASIC-Programmdatei speichern";
	  fileFilter = FileUtil.getKCBasicFileFilter();
	}
	else if( isBAS ) {
	  if( this.basicType.equals( BasicType.RBASIC ) ) {
	    title      = "RBASIC-Programmdatei speichern";
	    fileFilter = FileUtil.getBasicOrRBasicFileFilter();
	  } else {
	    title      = "BASIC-Programmdatei speichern";
	    fileFilter = FileUtil.getBasicFileFilter();
	  }
	  if( this.basicFileFilter != null ) {
	    fileFilter = this.basicFileFilter;
	  }
	}
	else if( isRMC ) {
	  title      = "RBASIC-Maschinencodedatei speichern";
	  fileFilter = FileUtil.getRMCFileFilter();
	}
	else if( isHS ) {
	  title      = "Headersave-Datei speichern";
	  fileFilter = FileUtil.getHeadersaveFileFilter();
	}
	else if( isHEX ) {
	  title      = "Intel-HEX-Datei speichern";
	  fileFilter = FileUtil.getHexFileFilter();
	}
	if( isCOM ) {
	  title      = "CP/M-kompatible Programmdatei speichern";
	  fileFilter = FileUtil.getCommandFileFilter();
	}
	String prefBasicFmt = null;
	switch( this.basicType ) {
	  case MS_DERIVED_BASIC:
	  case MS_DERIVED_BASIC_HS:
	  case RBASIC:
	    if( !isBAS && !isHS ) {
	      prefBasicFmt = this.rbFileFmtBAS.getText();
	    }
	    break;
	  case KCBASIC:
	    if( !isTAP && !isSSS && !isHS ) {
	      prefBasicFmt = this.rbFileFmtSSS.getText();
	    }
	    break;
	}
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
      }

      // eigentliches Speichern
      if( status ) {

	// Dateiauswahldialog
	File file = FileUtil.showFileSaveDlg(
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
			headFileType );
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
    if( this.rbFileFmtBIN.isSelected() ) {
      rv = FileFormat.BIN;
    }
    else if( this.rbFileFmtKCC.isSelected() ) {
      rv = FileFormat.KCC;
    }
    else if( this.rbFileFmtTAP.isSelected() ) {
      if( this.kcbasic ) {
	rv = FileFormat.KCTAP_BASIC_PRG;
      } else {
	rv = (this.rbBegBlkNum0.isSelected() ?
				FileFormat.KCTAP_Z9001
				: FileFormat.KCTAP_KC85);
      }
    }
    else if( this.rbFileFmtSSS.isSelected() ) {
      rv = FileFormat.KCBASIC_PRG;
    }
    else if( this.rbFileFmtBAS.isSelected() ) {
      if( this.basicType == BasicType.RBASIC ) {
	rv = FileFormat.RBASIC_PRG;
      } else {
	rv = FileFormat.BASIC_PRG;
      }
    }
    else if( this.rbFileFmtRMC.isSelected() ) {
      rv = FileFormat.RMC;
    }
    else if( this.rbFileFmtHS.isSelected() ) {
      rv = FileFormat.HEADERSAVE;
    }
    else if( this.rbFileFmtHEX.isSelected() ) {
      rv = FileFormat.INTELHEX;
    }
    else if( this.rbFileFmtCOM.isSelected() ) {
      rv = FileFormat.COM;
    }
    return rv;
  }


  private void updHeadFields()
  {
    boolean stateKCC = this.rbFileFmtKCC.isSelected();
    boolean stateTAP = this.rbFileFmtTAP.isSelected();
    boolean stateSSS = this.rbFileFmtSSS.isSelected();
    boolean stateBAS = this.rbFileFmtBAS.isSelected();
    boolean stateRMC = this.rbFileFmtRMC.isSelected();
    boolean stateHS  = this.rbFileFmtHS.isSelected();
    boolean stateHEX = this.rbFileFmtHEX.isSelected();

    this.rbBegBlkNum0.setEnabled( stateTAP && !this.kcbasic );
    this.rbBegBlkNum1.setEnabled( stateTAP && !this.kcbasic );

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
	FileUtil.addHeadersaveFileTypeItemsTo( this.comboHeadFileType );
	maxDescLen = 16;
	maxTypeLen = 1;
      } else if( stateKCC || stateTAP ) {
	if( (this.z9001 && stateKCC)
	    || (stateTAP && this.rbBegBlkNum0.isSelected()) )
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
