/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Z1013-Einstellungen
 */

package jkcemu.emusys.z1013;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class Z1013SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private JRadioButton           btnZ1013_01;
  private JRadioButton           btnZ1013_12;
  private JRadioButton           btnZ1013_16;
  private JRadioButton           btnZ1013_64;
  private JRadioButton           btnMon202;
  private JRadioButton           btnMonA2;
  private JRadioButton           btnMonRB_K7659;
  private JRadioButton           btnMonRB_S6009;
  private JRadioButton           btnMonJM_1992;
  private JRadioButton           btnBL4_K7659;
  private JRadioButton           btnPortNone;
  private JRadioButton           btnPortJoyJuTe_6_87;
  private JRadioButton           btnPortJoyPractic_4_87;
  private JRadioButton           btnPortJoyPractic_1_88;
  private JRadioButton           btnPortCentr7Practic_2_89;
  private JRadioButton           btnPortCentr8FA_10_90;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnRTC;
  private JCheckBox              btnGraphCCJ;
  private JCheckBox              btnGraphic;
  private JCheckBox              btnCatchPrintCalls;
  private JCheckBox              btnCatchJoyCalls;
  private JCheckBox              btnPasteFast;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
  private ROMFileSettingsFld     fldAltGCCJFont;
  private JPanel                 tabModel;
  private JPanel                 tabMon;
  private JPanel                 tabUserPort;
  private RAMFloppiesSettingsFld tabRF;
  private JPanel                 tabExt;
  private JPanel                 tabEtc;


  public Z1013SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Modell
    this.tabModel = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Modell", this.tabModel );

    GridBagConstraints gbcModel = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpModel = new ButtonGroup();

    this.btnZ1013_01 = new JRadioButton(
				"Z1013.01 (1 MHz, 16 KByte RAM)",
				false );
    this.btnZ1013_01.addActionListener( this );
    grpModel.add( this.btnZ1013_01 );
    this.tabModel.add( this.btnZ1013_01, gbcModel );

    this.btnZ1013_12 = new JRadioButton(
				"Z1013.12 (2 MHz, 1 KByte RAM)",
				false );
    this.btnZ1013_12.addActionListener( this );
    grpModel.add( this.btnZ1013_12 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.btnZ1013_12, gbcModel );

    this.btnZ1013_16 = new JRadioButton(
				"Z1013.16 (2 MHz, 16 KByte RAM)",
				false );
    this.btnZ1013_16.addActionListener( this );
    grpModel.add( this.btnZ1013_16 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnZ1013_16, gbcModel );

    this.btnZ1013_64 = new JRadioButton(
				"Z1013.64 (2 MHz, 64 KByte RAM)",
				false );
    this.btnZ1013_64.addActionListener( this );
    grpModel.add( this.btnZ1013_64 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnZ1013_64, gbcModel );


    // Tab Monitorprogramm / Tastatur
    this.tabMon = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Monitorprogramm", this.tabMon );

    GridBagConstraints gbcMon = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpMon = new ButtonGroup();

    this.btnMon202 = new JRadioButton(
		"Monitorprogramm 2.02 / Folienflachtastatur",
		true );
    this.btnMon202.addActionListener( this );
    grpMon.add( this.btnMon202 );
    this.tabMon.add( this.btnMon202, gbcMon );

    this.btnMonA2 = new JRadioButton(
		"Monitorprogramm A.2 / Alpha-Tastatur",
		false );
    this.btnMonA2.addActionListener( this );
    grpMon.add( this.btnMonA2 );
    gbcMon.insets.top = 0;
    gbcMon.gridy++;
    this.tabMon.add( this.btnMonA2, gbcMon );

    this.btnMonRB_K7659 = new JRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur K7659",
		false );
    this.btnMonRB_K7659.addActionListener( this );
    grpMon.add( this.btnMonRB_K7659 );
    gbcMon.gridy++;
    this.tabMon.add( this.btnMonRB_K7659, gbcMon );

    this.btnMonRB_S6009 = new JRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur S6009",
		false );
    this.btnMonRB_S6009.addActionListener( this );
    grpMon.add( this.btnMonRB_S6009 );
    gbcMon.gridy++;
    this.tabMon.add( this.btnMonRB_S6009, gbcMon );

    this.btnMonJM_1992 = new JRadioButton(
		"M\u00FCller-Monitorprogramm 1992 / Folienflachtastatur",
		false );
    this.btnMonJM_1992.addActionListener( this );
    grpMon.add( this.btnMonJM_1992 );
    gbcMon.gridy++;
    this.tabMon.add( this.btnMonJM_1992, gbcMon );

    this.btnBL4_K7659 = new JRadioButton(
		"Boot Lader 4 / Tastatur K7659 (Boot-Diskette einlegen!)",
		false );
    this.btnBL4_K7659.addActionListener( this );
    grpMon.add( this.btnBL4_K7659 );
    gbcMon.insets.bottom = 5;
    gbcMon.gridy++;
    this.tabMon.add( this.btnBL4_K7659, gbcMon );


    // Tab Anwendertor
    this.tabUserPort = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Anwendertor", this.tabUserPort );

    GridBagConstraints gbcUserPort = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabUserPort.add(
	new JLabel( "Am Anwendertor (User Port) emulierte Hardware:" ),
	gbcUserPort );

    ButtonGroup grpPort = new ButtonGroup();

    this.btnPortNone = new JRadioButton(
		"Keine angeschlossene Hardware emulieren",
		true );
    this.btnPortNone.addActionListener( this );
    grpPort.add( this.btnPortNone );
    gbcUserPort.insets.left   = 50;
    gbcUserPort.insets.top    = 0;
    gbcUserPort.insets.bottom = 0;
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.btnPortNone, gbcUserPort );

    this.btnPortJoyJuTe_6_87 = new JRadioButton(
		"1 Spielhebel nach JU+TE 06/1987",
		false );
    this.btnPortJoyJuTe_6_87.addActionListener( this );
    grpPort.add( this.btnPortJoyJuTe_6_87 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.btnPortJoyJuTe_6_87, gbcUserPort );

    this.btnPortJoyPractic_4_87 = new JRadioButton(
		"2 Spielhebel nach practic 04/1987",
		false );
    this.btnPortJoyPractic_4_87.addActionListener( this );
    grpPort.add( this.btnPortJoyPractic_4_87 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.btnPortJoyPractic_4_87, gbcUserPort );

    this.btnPortJoyPractic_1_88 = new JRadioButton(
		"2 Spielhebel nach practic 01/1988",
		false );
    this.btnPortJoyPractic_1_88.addActionListener( this );
    grpPort.add( this.btnPortJoyPractic_1_88 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.btnPortJoyPractic_1_88, gbcUserPort );

    this.btnPortCentr7Practic_2_89 = new JRadioButton(
		"Drucker an 7-Bit-Centronics-Anschluss"
					+ " nach practic 02/1989",
		false );
    this.btnPortCentr7Practic_2_89.addActionListener( this );
    grpPort.add( this.btnPortCentr7Practic_2_89 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.btnPortCentr7Practic_2_89, gbcUserPort );

    this.btnPortCentr8FA_10_90 = new JRadioButton(
		"Drucker an 8-Bit-Centronics-Anschluss nach FA 10/1990",
		false );
    this.btnPortCentr8FA_10_90.addActionListener( this );
    grpPort.add( this.btnPortCentr8FA_10_90 );
    gbcUserPort.insets.bottom = 5;
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.btnPortCentr8FA_10_90, gbcUserPort );


    // Tab RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
		settingsFrm,
		propPrefix,
		"RAM-Floppy nach MP 3/88 (256 KByte) an IO-Adressen 98h-9Fh",
		RAMFloppy.RFType.MP_3_1988,
		"RAM-Floppy nach MP 3/88 (256 KByte) an IO-Adressen 58h-5Fh",
		RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // Tab Erweiterungen
    this.tabExt = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnFloppyDisk = new JCheckBox( "Floppy-Disk-Modul", false );
    this.btnFloppyDisk.addActionListener( this );
    this.tabExt.add( this.btnFloppyDisk, gbcExt );

    this.btnRTC = new JCheckBox(
		"Echtzeituhr RTC-72421 an IO-Adressen 70h-7Fh",
		false );
    this.btnRTC.addActionListener( this );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.btnRTC, gbcExt );

    this.btnGraphic = new JCheckBox(
		"Vollgrafikerweiterung nach KRT 11 und FA 7/91",
		false );
    this.btnGraphic.addActionListener( this );
    gbcExt.gridy++;
    this.tabExt.add( this.btnGraphic, gbcExt );

    this.btnGraphCCJ = new JCheckBox(
		"Grafikkarte des CC Jena (80x25 Zeichen)",
		false );
    this.btnGraphCCJ.addActionListener( this );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.btnGraphCCJ, gbcExt );

    gbcExt.fill       = GridBagConstraints.HORIZONTAL;
    gbcExt.weightx    = 1.0;
    gbcExt.insets.top = 5;
    gbcExt.gridy++;
    this.tabExt.add( new JSeparator(), gbcExt );

    this.fldAltGCCJFont = new ROMFileSettingsFld(
		settingsFrm,
		this.propPrefix + "graph_ccj.font.",
		"Alternativer Zeichensatz f\u00FCr Grafikkarte"
			+ " des CC Jena (80x25 Zeichen):" );
    this.fldAltGCCJFont.setEnabled( false );
    gbcExt.gridy++;
    this.tabExt.add( this.fldAltGCCJFont, gbcExt );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnCatchPrintCalls = new JCheckBox(
		"Sprungverteileraufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    this.btnCatchPrintCalls.addActionListener( this );
    this.tabEtc.add( this.btnCatchPrintCalls, gbcEtc );

    this.btnCatchJoyCalls = new JCheckBox(
		"Sprungverteileraufrufe f\u00FCr Joystick-Abfragen abfangen",
		true );
    this.btnCatchJoyCalls.addActionListener( this );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnCatchJoyCalls, gbcEtc );

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs",
		true );
    this.btnPasteFast.addActionListener( this );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnPasteFast, gbcEtc );

    gbcEtc.fill       = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx    = 1.0;
    gbcEtc.insets.top = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "os.",
                "Alternatives Monitorprogramm (F000h-FFFFh):" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "font.",
				"Alternativer Zeichensatz:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );
  }


  public String getModelText()
  {
    String rv = "Z1013.64";
    if( this.btnZ1013_01.isSelected() ) {
      rv = "Z1013.01";
    } else if( this.btnZ1013_12.isSelected() ) {
      rv = "Z1013.12";
    } else if( this.btnZ1013_16.isSelected() ) {
      rv = "Z1013.16";
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    Component tab  = null;
    String    text = null;
    try {

      // Tab Monitorprogramm / Tastatur
      tab  = this.tabModel;
      text = "2.02";
      if( this.btnMonA2.isSelected() ) {
	text = "A.2";
      }
      else if( this.btnMonRB_K7659.isSelected() ) {
	text = "RB_K7659";
      }
      else if( this.btnMonRB_S6009.isSelected() ) {
	text = "RB_S6009";
      }
      else if( this.btnMonJM_1992.isSelected() ) {
	text = "JM_1992";
      }
      else if( this.btnBL4_K7659.isSelected() ) {
	text = "BL4_K7659";
      }
      props.setProperty( this.propPrefix + "monitor", text );

      // Tab Anwendertor
      tab  = this.tabUserPort;
      text = "none";
      if( this.btnPortJoyJuTe_6_87.isSelected() ) {
	text = "joystick:jute0687";
      } else if( this.btnPortJoyPractic_4_87.isSelected() ) {
	text = "joystick:practic0487";
      } else if( this.btnPortJoyPractic_1_88.isSelected() ) {
	text = "joystick:practic0188";
      } else if( this.btnPortJoyPractic_1_88.isSelected() ) {
	text = "joystick:practic0188";
      } else if( this.btnPortCentr7Practic_2_89.isSelected() ) {
	text = "centronics7:practic0289";
      } else if( this.btnPortCentr8FA_10_90.isSelected() ) {
	text = "centronics8:fa1090";
      }
      props.setProperty( this.propPrefix + "userport", text );

      // Tab RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab Erweiterungen
      tab = this.tabExt;
      props.setProperty(
		this.propPrefix + "floppydisk.enabled",
		Boolean.toString( this.btnFloppyDisk.isSelected() ) );
      props.setProperty(
		this.propPrefix + "rtc.enabled",
		Boolean.toString( this.btnRTC.isSelected() ) );
      props.setProperty(
		this.propPrefix + "graphic.enabled",
		Boolean.toString( this.btnGraphic.isSelected() ) );
      props.setProperty(
		this.propPrefix + "graph_ccj.enabled",
		Boolean.toString( this.btnGraphCCJ.isSelected() ) );
      this.fldAltGCCJFont.applyInput( props, selected );

      // Tab Sonstiges
      tab = this.tabEtc;
      props.setProperty(
		this.propPrefix + "catch_print_calls",
		Boolean.toString( this.btnCatchPrintCalls.isSelected() ) );
      props.setProperty(
		this.propPrefix + "catch_joystick_calls",
		Boolean.toString( this.btnCatchJoyCalls.isSelected() ) );
      props.setProperty(
		this.propPrefix + "paste.fast",
		Boolean.toString( this.btnPasteFast.isSelected() ) );
      this.fldAltOS.applyInput( props, selected );
      this.fldAltFont.applyInput( props, selected );
    }
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnGraphCCJ ) {
	rv = true;
	this.fldAltGCCJFont.setEnabled( this.btnGraphCCJ.isSelected() );
	fireDataChanged();
      }
      else if( src instanceof AbstractButton ) {
	rv = true;
	fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "Z1013.01" ) ) {
      this.btnZ1013_01.setSelected( true );
    } else if( sysName.startsWith( "Z1013.12" ) ) {
      this.btnZ1013_12.setSelected( true );
    } else if( sysName.startsWith( "Z1013.16" ) ) {
      this.btnZ1013_16.setSelected( true );
    } else {
      this.btnZ1013_64.setSelected( true );
    }

    String mon = EmuUtil.getProperty( props, this.propPrefix + "monitor" );
    if( mon.equals( "A.2" ) ) {
      this.btnMonA2.setSelected( true );
    } else if( mon.equals( "RB_K7659" ) ) {
      this.btnMonRB_K7659.setSelected( true );
    } else if( mon.equals( "RB_S6009" ) ) {
      this.btnMonRB_S6009.setSelected( true );
    } else if( mon.equals( "JM_1992" ) ) {
      this.btnMonJM_1992.setSelected( true );
    } else if( mon.equals( "BL4_K7659" ) ) {
      this.btnBL4_K7659.setSelected( true );
    } else {
      this.btnMon202.setSelected( true );
    }

    String port = EmuUtil.getProperty( props, this.propPrefix + "userport" );
    if( port.equals( "joystick:jute0687" ) ) {
      this.btnPortJoyJuTe_6_87.setSelected( true );
    } else if( port.equals( "joystick:practic0487" ) ) {
      this.btnPortJoyPractic_4_87.setSelected( true );
    } else if( port.equals( "joystick:practic0188" ) ) {
      this.btnPortJoyPractic_1_88.setSelected( true );
    } else if( port.equals( "centronics7:practic0289" ) ) {
      this.btnPortCentr7Practic_2_89.setSelected( true );
    } else if( port.equals( "centronics8:fa1090" ) ) {
      this.btnPortCentr8FA_10_90.setSelected( true );
    } else {
      this.btnPortNone.setSelected( true );
    }

    this.tabRF.updFields( props );

    this.btnFloppyDisk.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled", false ) );
    this.btnRTC.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "rtc.enabled", false ) );
    this.btnGraphic.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "graphic.enabled", false ) );
    this.btnGraphCCJ.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "graph_ccj.enabled", false ) );
    this.fldAltGCCJFont.updFields( props );

    this.btnCatchPrintCalls.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "catch_print_calls",
			true ) );
    this.btnCatchJoyCalls.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "catch_joystick_calls",
			true ) );
    this.btnPasteFast.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "paste.fast",
			true ) );
    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );
  }
}

