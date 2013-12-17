/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Datei speichern
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import jkcemu.Main;
import jkcemu.emusys.*;
import z80emu.Z80Memory;


public class SaveDlg extends BasicDlg implements
					DocumentListener,
					FocusListener
{
  private static String[] hsFileTypes = {
				"A Assemblerquelltext",
				"B BASIC-Programm",
				"b Mini-/Tiny-BASIC-Programm",
				"C Ausf\u00FChrbares MC-Programm",
				"E EPROM-Inhalt",
				"I Information (Text)",
				"T Text" };

  private ScreenFrm       screenFrm;
  private Z80Memory       memory;
  private boolean         kcbasic;
  private boolean         rbasic;
  private JTextField      autoFillTarget;
  private JTextField      fldMemBegAddr;
  private JTextField      fldMemEndAddr;
  private JTextField      fldHeadBegAddr;
  private JTextField      fldHeadStartAddr;
  private JTextField      fldHeadFileDesc;
  private JComboBox       comboHeadFileType;
  private JLabel          labelHeadBegAddr;
  private JLabel          labelHeadStartAddr;
  private JLabel          labelHeadFileType;
  private JLabel          labelHeadFileDesc;
  private JCheckBox       btnKeepHeader;
  private JRadioButton    btnFileFmtBIN;
  private JRadioButton    btnFileFmtKCC;
  private JRadioButton    btnFileFmtTAP;
  private JRadioButton    btnFileFmtSSS;
  private JRadioButton    btnFileFmtRBAS;
  private JRadioButton    btnFileFmtRMC;
  private JRadioButton    btnFileFmtHS;
  private JRadioButton    btnFileFmtHEX;
  private JRadioButton    btnBegBlkNum0;
  private JRadioButton    btnBegBlkNum1;
  private JButton         btnSave;
  private JButton         btnHelp;
  private JButton         btnCancel;
  private HexDocument     docMemBegAddr;
  private HexDocument     docMemEndAddr;
  private HexDocument     docHeadBegAddr;
  private HexDocument     docHeadStartAddr;
  private LimitedDocument docFileDesc;


  public SaveDlg(
		ScreenFrm screenFrm,
		int       begAddr,
		int       endAddr,
		int       hsFileType,
		boolean   kcbasic,
		boolean   rbasic,
		String    title )
  {
    super( screenFrm, title );
    this.screenFrm      = screenFrm;
    this.memory         = screenFrm.getEmuThread();
    this.kcbasic        = kcbasic;
    this.rbasic         = rbasic;
    this.autoFillTarget = null;


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
    this.fldMemBegAddr.addActionListener( this );
    this.fldMemBegAddr.addFocusListener( this );
    this.docMemBegAddr.addDocumentListener( this );
    if( begAddr >= 0 ) {
      this.fldMemBegAddr.setText( String.format( "%04X", begAddr ) );
      this.fldMemBegAddr.setEditable( false );
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
    this.fldMemEndAddr.addActionListener( this );
    this.docMemEndAddr.addDocumentListener( this );
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
    this.btnFileFmtBIN.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtBIN );
    panelFileFmt.add( this.btnFileFmtBIN, gbcFileFmt );

    this.btnFileFmtKCC = new JRadioButton( "KC-Systemdatei (*.kcc)", false );
    this.btnFileFmtKCC.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtKCC );
    gbcFileFmt.insets.top = 0;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtKCC, gbcFileFmt );

    this.btnFileFmtTAP = new JRadioButton( "KC-TAP-Datei (*.tap)", false );
    this.btnFileFmtTAP.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtTAP );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtTAP, gbcFileFmt );

    ButtonGroup grpBegBlkNum = new ButtonGroup();

    this.btnBegBlkNum0 = new JRadioButton(
		"Erster Block hat Nr. 0 (KC85/1, KC87, Z9001)",
		false );
    grpBegBlkNum.add( this.btnBegBlkNum0 );
    gbcFileFmt.insets.left = 50;
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnBegBlkNum0, gbcFileFmt );

    this.btnBegBlkNum1 = new JRadioButton(
		"Erster Block hat Nr. 1 (HC900, KC85/2-5, KC-BASIC)",
		true );
    grpBegBlkNum.add( this.btnBegBlkNum1 );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnBegBlkNum1, gbcFileFmt );

    gbcFileFmt.insets.left = 5;

    this.btnFileFmtSSS = new JRadioButton(
			"KC-BASIC-Programmdatei (*.sss)",
			false );
    this.btnFileFmtSSS.setEnabled( kcbasic );
    this.btnFileFmtSSS.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtSSS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtSSS, gbcFileFmt );

    this.btnFileFmtRBAS = new JRadioButton(
			"RBASIC-Programmdatei (*.bas)",
			false );
    this.btnFileFmtRBAS.setEnabled( rbasic );
    this.btnFileFmtRBAS.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtRBAS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtRBAS, gbcFileFmt );

    this.btnFileFmtRMC = new JRadioButton(
			"RBASIC-Maschinencodedatei (*.rmc)",
			false );
    this.btnFileFmtRMC.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtRMC );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtRMC, gbcFileFmt );

    this.btnFileFmtHS = new JRadioButton( "Headersave-Datei (*.z80)", false );
    this.btnFileFmtHS.addActionListener( this );
    grpFileFmt.add( this.btnFileFmtHS );
    gbcFileFmt.gridy++;
    panelFileFmt.add( this.btnFileFmtHS, gbcFileFmt );

    this.btnFileFmtHEX = new JRadioButton( "Intel-HEX-Datei (*.hex)", false );
    this.btnFileFmtHEX.addActionListener( this );
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

    // Anfangsadresse
    this.labelHeadBegAddr = new JLabel( "Anfangsadresse:" );
    gbcFileHead.gridwidth  = 1;
    gbcFileHead.insets.top = 5;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadBegAddr, gbcFileHead );

    this.docHeadBegAddr = new HexDocument(
				4,
				"Anfangsadresse in den Kopfdaten" );
    this.fldHeadBegAddr = new JTextField( this.docHeadBegAddr, "", 5 );
    this.fldHeadBegAddr.addActionListener( this );
    if( begAddr >= 0 ) {
      this.fldHeadBegAddr.setText( String.format( "%04X", begAddr ) );
    }
    gbcFileHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx = 0.5;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadBegAddr, gbcFileHead );

    // Startadresse
    this.labelHeadStartAddr = new JLabel( "Startadresse:" );
    gbcFileHead.fill    = GridBagConstraints.NONE;
    gbcFileHead.weightx = 0.0;
    gbcFileHead.gridx++;
    panelFileHead.add( this.labelHeadStartAddr, gbcFileHead );

    this.docHeadStartAddr = new HexDocument(
				4,
				"Startadresse in den Kopfdaten" );
    this.fldHeadStartAddr = new JTextField( this.docHeadStartAddr, "", 5 );
    this.fldHeadStartAddr.addActionListener( this );
    gbcFileHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx = 0.5;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadStartAddr, gbcFileHead );

    /*
     * Wenn die Adressen vollstaendig vorgegeben sind,
     * sollen sie in den Kopfdaten nicht aenderbar sein,
     * Da eine Startadresse nicht immer vorgegeben wird,
     * gelten vorgegebene Anfangs- und Endadressen als vollstaendig
     * vorgegebene Adressen.
     */
    if( (begAddr >= 0) && (endAddr >= begAddr) ) {
      this.fldHeadBegAddr.setEditable( false );
      this.fldHeadStartAddr.setEditable( false );
    }

    // Dateityp
    this.labelHeadFileType = new JLabel( "Dateityp (1. Zeichen):" );
    gbcFileHead.fill       = GridBagConstraints.NONE;
    gbcFileHead.weightx    = 0.0;
    gbcFileHead.gridx      = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadFileType, gbcFileHead );

    this.comboHeadFileType = new JComboBox();
    this.comboHeadFileType.addItem( "" );
    for( int i = 0; i < hsFileTypes.length; i++ ) {
      this.comboHeadFileType.addItem( hsFileTypes[ i ] );
    }
    this.comboHeadFileType.setEnabled( true );
    this.comboHeadFileType.setEditable( true );

    Font font = this.fldHeadBegAddr.getFont();
    if( font != null ) {
      this.comboHeadFileType.setFont( font );
    }

    if( (hsFileType > '\u0020') && (hsFileType <= '\u007E') ) {
      this.btnFileFmtHS.setSelected( true );
      for( int k = 0; k < hsFileTypes.length; k++ ) {
	String item = hsFileTypes[ k ];
	if( item.length() > 0 ) {
	  if( item.charAt( 0 ) == hsFileType ) {
	    this.comboHeadFileType.setSelectedItem( item );
	    this.comboHeadFileType.setEditable( false );
	    this.comboHeadFileType.setEnabled( false );
	    break;
	  }
	}
      }
    }
    gbcFileHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx   = 1.0;
    gbcFileHead.gridwidth = GridBagConstraints.REMAINDER;
    gbcFileHead.gridx++;
    panelFileHead.add( this.comboHeadFileType, gbcFileHead );

    // Dateibeschreibung
    this.labelHeadFileDesc = new JLabel( "Bezeichnung:" );
    gbcFileHead.fill       = GridBagConstraints.NONE;
    gbcFileHead.weightx    = 0.0;
    gbcFileHead.gridwidth  = 1;
    gbcFileHead.gridx      = 0;
    gbcFileHead.gridy++;
    panelFileHead.add( this.labelHeadFileDesc, gbcFileHead );

    this.docFileDesc = new LimitedDocument(
				FileSaver.getMaxFileDescLength() );
    this.fldHeadFileDesc = new JTextField( this.docFileDesc, "", 0 );
    this.fldHeadFileDesc.addActionListener( this );
    gbcFileHead.fill      = GridBagConstraints.HORIZONTAL;
    gbcFileHead.weightx   = 1.0;
    gbcFileHead.gridwidth = 3;
    gbcFileHead.gridx++;
    panelFileHead.add( this.fldHeadFileDesc, gbcFileHead );

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
    this.btnSave.addActionListener( this );
    this.btnSave.addKeyListener( this );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    this.btnHelp.addKeyListener( this );
    panelBtn.add( this.btnHelp );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );


    // Vorbelegungen
    boolean a5105  = false;
    boolean kc85   = false;
    boolean z1013  = false;
    boolean z9001  = false;
    EmuSys  emuSys = this.screenFrm.getEmuThread().getEmuSys();
    if( emuSys != null ) {
      a5105 = (emuSys instanceof A5105);
      kc85  = (emuSys instanceof KC85);
      z1013 = (emuSys instanceof Z1013);
      z9001 = (emuSys instanceof Z9001);
    }
    if( a5105 && !rbasic ) {
      this.btnFileFmtRMC.setSelected( true );
    } else if( z1013 ) {
      this.btnFileFmtHS.setSelected( true );
      this.btnKeepHeader.setSelected(
		EmuUtil.parseBoolean(
			Main.getProperty( "jkcemu.loadsave.header.keep" ),
			false ) );
    } else {
      if( kcbasic ) {
	this.btnFileFmtSSS.setSelected( true );
      } else if( rbasic ) {
	this.btnFileFmtRBAS.setSelected( true );
      } else if( kc85 || z9001 ) {
	this.btnFileFmtKCC.setSelected( true );
      } else if( (hsFileType > '\u0020') && (hsFileType <= '\u007E') ) {
	this.btnFileFmtHS.setSelected( true );
      } else {
	this.btnFileFmtBIN.setSelected( true );
      }
    }
    if( kcbasic || kc85 ) {
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


	/* --- Methoden fuer FocusListener --- */

  /*
   * Wenn das Feld "Anfangsadresse Speicherbereich" den Focus bekommen,
   * wird geprueft, ob das Feld "Anfangsadresse Kopfdaten" leer ist.
   * Wenn ja, wird das automatische Fuellen des zweiten Feldes aktiviert.
   */
  @Override
  public void focusGained( FocusEvent e )
  {
    if( !e.isTemporary() ) {
      Component c = e.getComponent();
      if( c == this.fldMemBegAddr ) {
	this.autoFillTarget = (this.docHeadBegAddr.getLength() < 1 ?
						this.fldHeadBegAddr : null);
      }
    }
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    if( !e.isTemporary() )
      this.autoFillTarget = null;
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
	  || (src == this.btnFileFmtSSS)
	  || (src == this.btnFileFmtRBAS)
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
	HelpFrm.open( "/help/loadsave.htm" );
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
    boolean saved   = false;
    String  fileFmt = getSelectedFileFmt();
    try {
      FileSaver.checkFileDesc(
			fileFmt,
			this.kcbasic,
			this.fldHeadFileDesc.getText() );

      boolean isBIN  = this.btnFileFmtBIN.isSelected();
      boolean isKCC  = this.btnFileFmtKCC.isSelected();
      boolean isTAP  = this.btnFileFmtTAP.isSelected();
      boolean isSSS  = this.btnFileFmtTAP.isSelected();
      boolean isRBAS = this.btnFileFmtRBAS.isSelected();
      boolean isRMC  = this.btnFileFmtRMC.isSelected();
      boolean isHS   = this.btnFileFmtHS.isSelected();
      boolean isHEX  = this.btnFileFmtHEX.isSelected();

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
	fileFilter = EmuUtil.getTapFileFilter();
      }
      else if( isSSS ) {
	title      = "KC-BASIC-Programmdatei speichern";
	fileFilter = EmuUtil.getKCBasicFileFilter();
      }
      else if( isRBAS ) {
	title      = "RBASIC-Programmdatei speichern";
	fileFilter = EmuUtil.getRBasicFileFilter();
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
      File file = EmuUtil.showFileSaveDlg(
				this.screenFrm,
				title,
				Main.getLastPathFile( "software" ),
				fileFilter );
      if( file != null ) {
	try {
	  int     begAddr       = this.docMemBegAddr.intValue() & 0xFFFF;
	  int     endAddr       = this.docMemEndAddr.intValue() & 0xFFFF;
	  int     headBegAddr   = begAddr;
	  Integer headStartAddr = null;
	  int     headFileType  = '\u0020';
	  if( isKCC || (isTAP && !this.kcbasic) || isHS || isHEX ) {
	    String headBegAddrText = this.fldHeadBegAddr.getText();
	    if( headBegAddrText != null ) {
	      if( !headBegAddrText.trim().isEmpty() ) {
		headBegAddr = this.docHeadBegAddr.intValue() & 0xFFFF;
	      }
	    }
	  }
	  if( isKCC || (isTAP && !this.kcbasic) || isHS ) {
	    String s = this.fldHeadStartAddr.getText();
	    if( s != null ) {
	      if( !s.trim().isEmpty() ) {
		headStartAddr = new Integer(
			this.docHeadStartAddr.intValue() & 0xFFFF );
	      }
	    }
	  }
	  if( isHS ) {
	    Object o = this.comboHeadFileType.getSelectedItem();
	    if( o != null ) {
	      String s = o.toString();
	      if( s != null ) {
		if( s.length() > 0 ) {
		  headFileType = s.charAt( 0 );
		}
	      }
	    }
	  }
	  FileSaver.saveFile(
			file,
			fileFmt,
			this.memory,
			begAddr,
			endAddr,
			this.kcbasic,
			this.rbasic,
			headBegAddr,
			headStartAddr,
			headFileType,
			this.fldHeadFileDesc.getText(),
			this.btnKeepHeader.isSelected() ? this.memory : null );
	  saved = true;
	  Main.setLastFile( file, "software" );
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
    catch( UserInputException ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
    if( saved )
      doClose();
  }


  private void docContentChanged( DocumentEvent e )
  {
    if( this.autoFillTarget != null ) {
      Document doc = e.getDocument();
      if( (doc != null) && (doc != this.autoFillTarget.getDocument()) ) {
	try {
	  this.autoFillTarget.setText( doc.getText( 0, doc.getLength() ) );
	}
	catch( BadLocationException ex ) {}
      }
    }
    updSaveBtn();
  }


  private String getSelectedFileFmt()
  {
    String rv = FileSaver.BIN;
    if( this.btnFileFmtKCC.isSelected() ) {
      rv = FileSaver.KCC;
    }
    else if( this.btnFileFmtTAP.isSelected() ) {
      rv = this.btnBegBlkNum0.isSelected() ?
				FileSaver.KCTAP_0
				: FileSaver.KCTAP_1;
    }
    else if( this.btnFileFmtSSS.isSelected() ) {
      rv = FileSaver.KCBASIC;
    }
    else if( this.btnFileFmtRBAS.isSelected() ) {
      rv = FileSaver.RBASIC;
    }
    else if( this.btnFileFmtRMC.isSelected() ) {
      rv = FileSaver.RMC;
    }
    else if( this.btnFileFmtHS.isSelected() ) {
      rv = FileSaver.HEADERSAVE;
    }
    else if( this.btnFileFmtHEX.isSelected() ) {
      rv = FileSaver.INTELHEX;
    }
    return rv;
  }


  private void updHeadFields()
  {
    boolean stateKCC  = this.btnFileFmtKCC.isSelected();
    boolean stateTAP  = this.btnFileFmtTAP.isSelected();
    boolean stateSSS  = this.btnFileFmtSSS.isSelected();
    boolean stateRBAS = this.btnFileFmtRBAS.isSelected();
    boolean stateRMC  = this.btnFileFmtRMC.isSelected();
    boolean stateHS   = this.btnFileFmtHS.isSelected();
    boolean stateHEX  = this.btnFileFmtHEX.isSelected();

    this.btnBegBlkNum0.setEnabled( stateTAP && !this.kcbasic );
    this.btnBegBlkNum1.setEnabled( stateTAP && !this.kcbasic );

    boolean stateBegAddr = (stateKCC || (stateTAP && !this.kcbasic)
				|| stateHS || stateRMC || stateHEX);
    this.labelHeadBegAddr.setEnabled( stateBegAddr );
    this.fldHeadBegAddr.setEnabled( stateBegAddr );

    boolean stateStartAddr = (stateKCC || (stateTAP && !this.kcbasic)
				|| stateHS || stateRMC);
    this.labelHeadStartAddr.setEnabled( stateStartAddr );
    this.fldHeadStartAddr.setEnabled( stateStartAddr );

    this.labelHeadFileType.setEnabled( stateHS );
    this.comboHeadFileType.setEnabled( stateHS );

    boolean stateFileDesc = (stateKCC || stateTAP || stateHS);
    this.labelHeadFileDesc.setEnabled( stateFileDesc );
    this.fldHeadFileDesc.setEnabled( stateFileDesc );
    if( stateFileDesc ) {
      this.docFileDesc.setMaxLength(
		FileSaver.getMaxFileDescLength(
					getSelectedFileFmt(),
					false ) );
    }

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

