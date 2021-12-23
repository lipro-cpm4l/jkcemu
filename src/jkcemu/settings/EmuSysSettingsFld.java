/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen der emulierten Systeme
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.emusys.A5105;
import jkcemu.emusys.AC1;
import jkcemu.emusys.BCS3;
import jkcemu.emusys.C80;
import jkcemu.emusys.CustomSys;
import jkcemu.emusys.HueblerEvertMC;
import jkcemu.emusys.HueblerGraphicsMC;
import jkcemu.emusys.KC85;
import jkcemu.emusys.KCcompact;
import jkcemu.emusys.KramerMC;
import jkcemu.emusys.LC80;
import jkcemu.emusys.LLC1;
import jkcemu.emusys.LLC2;
import jkcemu.emusys.NANOS;
import jkcemu.emusys.PCM;
import jkcemu.emusys.Poly880;
import jkcemu.emusys.SC2;
import jkcemu.emusys.SLC1;
import jkcemu.emusys.VCS80;
import jkcemu.emusys.Z1013;
import jkcemu.emusys.Z9001;
import jkcemu.emusys.ZXSpectrum;
import jkcemu.emusys.a5105.A5105SettingsFld;
import jkcemu.emusys.ac1_llc2.AC1SettingsFld;
import jkcemu.emusys.ac1_llc2.LLC2SettingsFld;
import jkcemu.emusys.bcs3.BCS3SettingsFld;
import jkcemu.emusys.customsys.CustomSysSettingsFld;
import jkcemu.emusys.etc.KramerMCSettingsFld;
import jkcemu.emusys.etc.NANOSSettingsFld;
import jkcemu.emusys.etc.PCMSettingsFld;
import jkcemu.emusys.huebler.HueblerEvertMCSettingsFld;
import jkcemu.emusys.huebler.HueblerGraphicsMCSettingsFld;
import jkcemu.emusys.kc85.KC85SettingsFld;
import jkcemu.emusys.kccompact.KCcompactSettingsFld;
import jkcemu.emusys.lc80.LC80SettingsFld;
import jkcemu.emusys.llc1.LLC1SettingsFld;
import jkcemu.emusys.poly880.Poly880SettingsFld;
import jkcemu.emusys.z1013.Z1013SettingsFld;
import jkcemu.emusys.z9001.Z9001SettingsFld;
import jkcemu.emusys.zxspectrum.ZXSpectrumSettingsFld;


public class EmuSysSettingsFld extends AbstractSettingsFld
{
  private static final String CARD_EMPTY = "empty";

  private JPanel                       panelOpt;
  private CardLayout                   cardLayoutSysOpt;
  private String                       curSysOptCard;
  private JRadioButton                 rbA5105;
  private JRadioButton                 rbAC1;
  private JRadioButton                 rbBCS3;
  private JRadioButton                 rbC80;
  private JRadioButton                 rbCustomSys;
  private JRadioButton                 rbHC900;
  private JRadioButton                 rbHEMC;
  private JRadioButton                 rbHGMC;
  private JRadioButton                 rbKC85_1;
  private JRadioButton                 rbKC85_2;
  private JRadioButton                 rbKC85_3;
  private JRadioButton                 rbKC85_4;
  private JRadioButton                 rbKC85_5;
  private JRadioButton                 rbKC87;
  private JRadioButton                 rbKCcompact;
  private JRadioButton                 rbKramerMC;
  private JRadioButton                 rbLC80;
  private JRadioButton                 rbLLC1;
  private JRadioButton                 rbLLC2;
  private JRadioButton                 rbNANOS;
  private JRadioButton                 rbPCM;
  private JRadioButton                 rbPoly880;
  private JRadioButton                 rbSC2;
  private JRadioButton                 rbSLC1;
  private JRadioButton                 rbVCS80;
  private JRadioButton                 rbZ1013;
  private JRadioButton                 rbZ9001;
  private JRadioButton                 rbZXSpectrum;
  private A5105SettingsFld             a5105SettingsFld;
  private AC1SettingsFld               ac1SettingsFld;
  private CustomSysSettingsFld         customSysSettingsFld;
  private BCS3SettingsFld              bcs3SettingsFld;
  private HueblerEvertMCSettingsFld    hemcSettingsFld;
  private HueblerGraphicsMCSettingsFld hgmcSettingsFld;
  private KC85SettingsFld              hc900SettingsFld;
  private LLC1SettingsFld              llc1SettingsFld;
  private LLC2SettingsFld              llc2SettingsFld;
  private Z9001SettingsFld             kc85_1_SettingsFld;
  private KC85SettingsFld              kc85_2_SettingsFld;
  private KC85SettingsFld              kc85_3_SettingsFld;
  private KC85SettingsFld              kc85_4_SettingsFld;
  private KC85SettingsFld              kc85_5_SettingsFld;
  private KCcompactSettingsFld         kcCompactSettingsFld;
  private KramerMCSettingsFld          kramerMCSettingsFld;
  private LC80SettingsFld              lc80SettingsFld;
  private NANOSSettingsFld             nanosSettingsFld;
  private PCMSettingsFld               pcmSettingsFld;
  private Poly880SettingsFld           poly880SettingsFld;
  private Z1013SettingsFld             z1013SettingsFld;
  private Z9001SettingsFld             kc87SettingsFld;
  private Z9001SettingsFld             z9001SettingsFld;
  private ZXSpectrumSettingsFld        zxSpectrumSettingsFld;


  public EmuSysSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    setLayout( new BorderLayout( 5, 5 ) );

    JPanel panelSys = GUIFactory.createPanel( new GridBagLayout() );
    add( GUIFactory.createScrollPane( panelSys ), BorderLayout.WEST );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.rbA5105 = GUIFactory.createRadioButton( A5105.SYSTEXT, true );
    this.rbA5105.addActionListener( this );
    grpSys.add( this.rbA5105 );
    panelSys.add( this.rbA5105, gbc );

    this.rbAC1 = GUIFactory.createRadioButton( AC1.SYSNAME );
    this.rbAC1.addActionListener( this );
    grpSys.add( this.rbAC1 );
    gbc.insets.top = 0;
    gbc.gridy++;
    panelSys.add( this.rbAC1, gbc );

    this.rbBCS3 = GUIFactory.createRadioButton( BCS3.SYSNAME );
    this.rbBCS3.addActionListener( this );
    grpSys.add( this.rbBCS3 );
    gbc.gridy++;
    panelSys.add( this.rbBCS3, gbc );

    this.rbC80 = GUIFactory.createRadioButton( C80.SYSTEXT );
    this.rbC80.addActionListener( this );
    grpSys.add( this.rbC80 );
    gbc.gridy++;
    panelSys.add( this.rbC80, gbc );

    this.rbHC900 = GUIFactory.createRadioButton( KC85.SYSTEXT_HC900 );
    this.rbHC900.addActionListener( this );
    grpSys.add( this.rbHC900 );
    gbc.gridy++;
    panelSys.add( this.rbHC900, gbc );

    this.rbHEMC = GUIFactory.createRadioButton( HueblerEvertMC.SYSTEXT );
    this.rbHEMC.addActionListener( this );
    grpSys.add( this.rbHEMC );
    gbc.gridy++;
    panelSys.add( this.rbHEMC, gbc );

    this.rbHGMC = GUIFactory.createRadioButton(
					HueblerGraphicsMC.SYSTEXT );
    this.rbHGMC.addActionListener( this );
    grpSys.add( this.rbHGMC );
    gbc.gridy++;
    panelSys.add( this.rbHGMC, gbc );

    this.rbKC85_1 = GUIFactory.createRadioButton( Z9001.SYSTEXT_KC85_1 );
    this.rbKC85_1.addActionListener( this );
    grpSys.add( this.rbKC85_1 );
    gbc.gridy++;
    panelSys.add( this.rbKC85_1, gbc );

    this.rbKC85_2 = GUIFactory.createRadioButton( KC85.SYSTEXT_KC85_2 );
    this.rbKC85_2.addActionListener( this );
    grpSys.add( this.rbKC85_2 );
    gbc.gridy++;
    panelSys.add( this.rbKC85_2, gbc );

    this.rbKC85_3 = GUIFactory.createRadioButton( KC85.SYSTEXT_KC85_3 );
    this.rbKC85_3.addActionListener( this );
    grpSys.add( this.rbKC85_3 );
    gbc.gridy++;
    panelSys.add( this.rbKC85_3, gbc );

    this.rbKC85_4 = GUIFactory.createRadioButton( KC85.SYSTEXT_KC85_4 );
    this.rbKC85_4.addActionListener( this );
    grpSys.add( this.rbKC85_4 );
    gbc.gridy++;
    panelSys.add( this.rbKC85_4, gbc );

    this.rbKC85_5 = GUIFactory.createRadioButton( KC85.SYSTEXT_KC85_5 );
    this.rbKC85_5.addActionListener( this );
    grpSys.add( this.rbKC85_5 );
    gbc.gridy++;
    panelSys.add( this.rbKC85_5, gbc );

    this.rbKC87 = GUIFactory.createRadioButton( Z9001.SYSNAME_KC87 );
    this.rbKC87.addActionListener( this );
    grpSys.add( this.rbKC87 );
    gbc.gridy++;
    panelSys.add( this.rbKC87, gbc );

    this.rbKCcompact = GUIFactory.createRadioButton( KCcompact.SYSTEXT );
    this.rbKCcompact.addActionListener( this );
    grpSys.add( this.rbKCcompact );
    gbc.gridy++;
    panelSys.add( this.rbKCcompact, gbc );

    this.rbKramerMC = GUIFactory.createRadioButton( KramerMC.SYSTEXT );
    this.rbKramerMC.addActionListener( this );
    grpSys.add( this.rbKramerMC );
    gbc.insets.top = 5;
    gbc.gridy      = 0;
    gbc.gridx++;
    panelSys.add( this.rbKramerMC, gbc );

    this.rbLC80 = GUIFactory.createRadioButton( LC80.SYSTEXT );
    this.rbLC80.addActionListener( this );
    grpSys.add( this.rbLC80 );
    gbc.insets.top = 0;
    gbc.gridy++;
    panelSys.add( this.rbLC80, gbc );

    this.rbLLC1 = GUIFactory.createRadioButton( LLC1.SYSNAME );
    this.rbLLC1.addActionListener( this );
    grpSys.add( this.rbLLC1 );
    gbc.gridy++;
    panelSys.add( this.rbLLC1, gbc );

    this.rbLLC2 = GUIFactory.createRadioButton( LLC2.SYSNAME );
    this.rbLLC2.addActionListener( this );
    grpSys.add( this.rbLLC2 );
    gbc.gridy++;
    panelSys.add( this.rbLLC2, gbc );

    this.rbNANOS = GUIFactory.createRadioButton( NANOS.SYSNAME );
    this.rbNANOS.addActionListener( this );
    grpSys.add( this.rbNANOS );
    gbc.gridy++;
    panelSys.add( this.rbNANOS, gbc );

    this.rbPCM = GUIFactory.createRadioButton( PCM.SYSTEXT );
    this.rbPCM.addActionListener( this );
    grpSys.add( this.rbPCM );
    gbc.gridy++;
    panelSys.add( this.rbPCM, gbc );

    this.rbPoly880 = GUIFactory.createRadioButton( Poly880.SYSTEXT );
    this.rbPoly880.addActionListener( this );
    grpSys.add( this.rbPoly880 );
    gbc.gridy++;
    panelSys.add( this.rbPoly880, gbc );

    this.rbSC2 = GUIFactory.createRadioButton( SC2.SYSNAME );
    this.rbSC2.addActionListener( this );
    grpSys.add( this.rbSC2 );
    gbc.gridy++;
    panelSys.add( this.rbSC2, gbc );

    this.rbSLC1 = GUIFactory.createRadioButton( SLC1.SYSNAME );
    this.rbSLC1.addActionListener( this );
    grpSys.add( this.rbSLC1 );
    gbc.gridy++;
    panelSys.add( this.rbSLC1, gbc );

    this.rbVCS80 = GUIFactory.createRadioButton( VCS80.SYSNAME );
    this.rbVCS80.addActionListener( this );
    grpSys.add( this.rbVCS80 );
    gbc.gridy++;
    panelSys.add( this.rbVCS80, gbc );

    this.rbZ1013 = GUIFactory.createRadioButton( Z1013.SYSNAME );
    this.rbZ1013.addActionListener( this );
    grpSys.add( this.rbZ1013 );
    gbc.gridy++;
    panelSys.add( this.rbZ1013, gbc );

    this.rbZ9001 = GUIFactory.createRadioButton( Z9001.SYSNAME_Z9001 );
    this.rbZ9001.addActionListener( this );
    grpSys.add( this.rbZ9001 );
    gbc.gridy++;
    panelSys.add( this.rbZ9001, gbc );

    this.rbZXSpectrum = GUIFactory.createRadioButton(
						ZXSpectrum.SYSTEXT );
    this.rbZXSpectrum.addActionListener( this );
    grpSys.add( this.rbZXSpectrum );
    gbc.gridy++;
    panelSys.add( this.rbZXSpectrum, gbc );

    this.rbCustomSys = GUIFactory.createRadioButton( CustomSys.SYSTEXT );
    this.rbCustomSys.addActionListener( this );
    grpSys.add( this.rbCustomSys );
    gbc.insets.top    = 10;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy += 2;
    panelSys.add( this.rbCustomSys, gbc );


    // Optionen
    this.cardLayoutSysOpt = new CardLayout( 5, 5);
    this.curSysOptCard    = null;

    this.panelOpt = GUIFactory.createPanel( this.cardLayoutSysOpt );
    this.panelOpt.setBorder(
		GUIFactory.createTitledBorder( "Optionen" ) );
    gbc.anchor     = GridBagConstraints.CENTER;
    gbc.fill       = GridBagConstraints.BOTH;
    gbc.weightx    = 1.0;
    gbc.weighty    = 1.0;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( GUIFactory.createScrollPane( this.panelOpt ), BorderLayout.CENTER );


    JPanel panelEmpty = GUIFactory.createPanel( new GridBagLayout() );
    this.panelOpt.add( panelEmpty, CARD_EMPTY );

    GridBagConstraints gbcEmpty = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelEmpty.add(
		GUIFactory.createLabel( "Keine Optionen verf\u00FCgbar" ),
		gbcEmpty );


    // Optionen fuer A5105
    this.a5105SettingsFld = new A5105SettingsFld(
					this.settingsFrm,
					A5105.PROP_PREFIX );
    this.panelOpt.add( this.a5105SettingsFld, A5105.SYSNAME );


    // Optionen fuer AC1
    this.ac1SettingsFld = new AC1SettingsFld(
					this.settingsFrm,
					AC1.PROP_PREFIX );
    this.panelOpt.add( this.ac1SettingsFld, AC1.SYSNAME );


    // Optionen fuer BCS3
    this.bcs3SettingsFld = new BCS3SettingsFld(
					this.settingsFrm,
					BCS3.PROP_PREFIX );
    this.panelOpt.add( this.bcs3SettingsFld, BCS3.SYSNAME );


    // Optionen fuer A5105
    this.customSysSettingsFld = new CustomSysSettingsFld(
					this.settingsFrm,
					CustomSys.PROP_PREFIX );
    this.panelOpt.add( this.customSysSettingsFld, CustomSys.SYSNAME );



    // Optionen fuer HC900
    this.hc900SettingsFld = new KC85SettingsFld(
					this.settingsFrm,
					KC85.PROP_PREFIX_HC900,
					2 );
    this.panelOpt.add( this.hc900SettingsFld, KC85.SYSNAME_HC900 );


    // Optionen fuer Huebler/Evert-MC
    this.hemcSettingsFld = new HueblerEvertMCSettingsFld(
					this.settingsFrm,
					HueblerEvertMC.PROP_PREFIX );
    this.panelOpt.add( this.hemcSettingsFld, HueblerEvertMC.SYSNAME );


    // Optionen fuer Huebler-Grafik-MC
    this.hgmcSettingsFld = new HueblerGraphicsMCSettingsFld(
					this.settingsFrm,
					HueblerGraphicsMC.PROP_PREFIX );
    this.panelOpt.add( this.hgmcSettingsFld, HueblerGraphicsMC.SYSNAME );


    // Optionen fuer KC85/1..5
    this.kc85_1_SettingsFld = new Z9001SettingsFld(
					this.settingsFrm,
					Z9001.PROP_PREFIX_KC85_1,
					false );
    this.panelOpt.add( this.kc85_1_SettingsFld, Z9001.SYSNAME_KC85_1 );

    this.kc85_2_SettingsFld = new KC85SettingsFld(
					this.settingsFrm,
					KC85.PROP_PREFIX_KC85_2,
					2 );
    this.panelOpt.add( this.kc85_2_SettingsFld, KC85.SYSNAME_KC85_2 );

    this.kc85_3_SettingsFld = new KC85SettingsFld(
					this.settingsFrm,
					KC85.PROP_PREFIX_KC85_3,
					3 );
    this.panelOpt.add( this.kc85_3_SettingsFld, KC85.SYSNAME_KC85_3 );

    this.kc85_4_SettingsFld = new KC85SettingsFld(
					this.settingsFrm,
					KC85.PROP_PREFIX_KC85_4,
					4 );
    this.panelOpt.add( this.kc85_4_SettingsFld, KC85.SYSNAME_KC85_4 );

    this.kc85_5_SettingsFld = new KC85SettingsFld(
					this.settingsFrm,
					KC85.PROP_PREFIX_KC85_5,
					5 );
    this.panelOpt.add( this.kc85_5_SettingsFld, KC85.SYSNAME_KC85_5 );


    // Optionen fuer KC87
    this.kc87SettingsFld = new Z9001SettingsFld(
					this.settingsFrm,
					Z9001.PROP_PREFIX_KC87,
					true );
    this.panelOpt.add( this.kc87SettingsFld, Z9001.SYSNAME_KC87 );


    // Optionen fuer KC compact
    this.kcCompactSettingsFld = new KCcompactSettingsFld(
					this.settingsFrm,
					KCcompact.PROP_PREFIX );
    this.panelOpt.add( this.kcCompactSettingsFld, KCcompact.SYSNAME );


    // Optionen fuer Kramer-MC
    this.kramerMCSettingsFld = new KramerMCSettingsFld(
					this.settingsFrm,
					KramerMC.PROP_PREFIX );
    this.panelOpt.add( this.kramerMCSettingsFld, KramerMC.SYSNAME );


    // Optionen fuer LC80
    this.lc80SettingsFld = new LC80SettingsFld(
					this.settingsFrm,
					LC80.PROP_PREFIX );
    this.panelOpt.add( this.lc80SettingsFld, LC80.SYSNAME );


    // Optionen fuer LLC1
    this.llc1SettingsFld = new LLC1SettingsFld(
					this.settingsFrm,
					LLC1.PROP_PREFIX );
    this.panelOpt.add( this.llc1SettingsFld, LLC1.SYSNAME );


    // Optionen fuer LLC2
    this.llc2SettingsFld = new LLC2SettingsFld(
					this.settingsFrm,
					LLC2.PROP_PREFIX );
    this.panelOpt.add( this.llc2SettingsFld, LLC2.SYSNAME );


    // Optionen fuer NANOS
    this.nanosSettingsFld = new NANOSSettingsFld(
					this.settingsFrm,
					NANOS.PROP_PREFIX );
    this.panelOpt.add( this.nanosSettingsFld, NANOS.SYSNAME );


    // Optionen fuer PC/M
    this.pcmSettingsFld = new PCMSettingsFld(
					this.settingsFrm,
					PCM.PROP_PREFIX );
    this.panelOpt.add( this.pcmSettingsFld, PCM.SYSNAME );


    // Optionen fuer Poly880
    this.poly880SettingsFld = new Poly880SettingsFld(
					this.settingsFrm,
					Poly880.PROP_PREFIX );
    this.panelOpt.add( this.poly880SettingsFld, Poly880.SYSNAME );


    // Optionen fuer Z1013
    this.z1013SettingsFld = new Z1013SettingsFld(
					this.settingsFrm,
					Z1013.PROP_PREFIX );
    this.panelOpt.add( this.z1013SettingsFld, Z1013.SYSNAME );


    // Optionen fuer Z9001
    this.z9001SettingsFld = new Z9001SettingsFld(
					this.settingsFrm,
					Z9001.PROP_PREFIX_Z9001,
					false );
    this.panelOpt.add( this.z9001SettingsFld, Z9001.SYSNAME_Z9001 );


    // Optionen fuer ZX Spectrum
    this.zxSpectrumSettingsFld = new ZXSpectrumSettingsFld(
					this.settingsFrm,
					ZXSpectrum.PROP_PREFIX );
    this.panelOpt.add( this.zxSpectrumSettingsFld, ZXSpectrum.SYSNAME );
  }


  public Properties getCurSettingsSilently()
  {
    Properties props = new Properties();
    try {
      applyInput( props, false );
    }
    catch( UserCancelException ex1 ) {}
    catch( UserInputException ex2 ) {}
    return props;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws
						UserCancelException,
						UserInputException
  {
    // Emuliertes System
    String valueSys = "";
    if( this.rbA5105.isSelected() ) {
      valueSys = A5105.SYSNAME;
    }
    else if( this.rbAC1.isSelected() ) {
      valueSys = AC1.SYSNAME;
    }
    else if( this.rbBCS3.isSelected() ) {
      valueSys = BCS3.SYSNAME;
    }
    else if( this.rbC80.isSelected() ) {
      valueSys = C80.SYSNAME;
    }
    else if( this.rbCustomSys.isSelected() ) {
      valueSys = CustomSys.SYSNAME;
    }
    else if( this.rbHC900.isSelected() ) {
      valueSys = KC85.SYSNAME_HC900;
    }
    else if( this.rbHEMC.isSelected() ) {
      valueSys = HueblerEvertMC.SYSNAME;
    }
    else if( this.rbHGMC.isSelected() ) {
      valueSys = HueblerGraphicsMC.SYSNAME;
    }
    else if( this.rbKC85_1.isSelected() ) {
      valueSys = Z9001.SYSNAME_KC85_1;
    }
    else if( this.rbKC85_2.isSelected() ) {
      valueSys = KC85.SYSNAME_KC85_2;
    }
    else if( this.rbKC85_3.isSelected() ) {
      valueSys = KC85.SYSNAME_KC85_3;
    }
    else if( this.rbKC85_4.isSelected() ) {
      valueSys = KC85.SYSNAME_KC85_4;
    }
    else if( this.rbKC85_5.isSelected() ) {
      valueSys = KC85.SYSNAME_KC85_5;
    }
    else if( this.rbKC87.isSelected() ) {
      valueSys = Z9001.SYSNAME_KC87;
    }
    else if( this.rbKCcompact.isSelected() ) {
      valueSys = KCcompact.SYSNAME;
    }
    else if( this.rbKramerMC.isSelected() ) {
      valueSys = KramerMC.SYSNAME;
    }
    else if( this.rbLC80.isSelected() ) {
      valueSys = this.lc80SettingsFld.getModelSysName();
    }
    else if( this.rbLLC1.isSelected() ) {
      valueSys = LLC1.SYSNAME;
    }
    else if( this.rbLLC2.isSelected() ) {
      valueSys = LLC2.SYSNAME;
    }
    else if( this.rbNANOS.isSelected() ) {
      valueSys = NANOS.SYSNAME;
    }
    else if( this.rbPCM.isSelected() ) {
      valueSys = PCM.SYSNAME;
    }
    else if( this.rbPoly880.isSelected() ) {
      valueSys = Poly880.SYSNAME;
    }
    else if( this.rbSC2.isSelected() ) {
      valueSys = SC2.SYSNAME;
    }
    else if( this.rbSLC1.isSelected() ) {
      valueSys = SLC1.SYSNAME;
    }
    else if( this.rbVCS80.isSelected() ) {
      valueSys = VCS80.SYSNAME;
    }
    else if( this.rbZ1013.isSelected() ) {
      valueSys = this.z1013SettingsFld.getModelSysName();
    }
    else if( this.rbZ9001.isSelected() ) {
      valueSys = Z9001.SYSNAME_Z9001;
    }
    else if( this.rbZXSpectrum.isSelected() ) {
      valueSys = ZXSpectrum.SYSNAME;
    }
    props.setProperty( EmuThread.PROP_SYSNAME, valueSys );

    // Optionen fuer A5105
    this.a5105SettingsFld.applyInput(
		props,
		selected && valueSys.equals( A5105.SYSNAME ) );

    // Optionen fuer AC1
    this.ac1SettingsFld.applyInput(
		props,
		selected && valueSys.equals( AC1.SYSNAME ) );

    // Optionen fuer BCS3
    this.bcs3SettingsFld.applyInput(
		props,
		selected && valueSys.equals( BCS3.SYSNAME ) );

    // Optionen fuer benutzerdefinierten Computer
    this.customSysSettingsFld.applyInput(
		props,
		selected && valueSys.equals( CustomSys.SYSNAME ) );

    // Optionen fuer HC900
    this.hc900SettingsFld.applyInput(
		props,
		selected && valueSys.equals( KC85.SYSNAME_HC900 ) );

    // Optionen fuer Huebler/Evert-MC
    this.hemcSettingsFld.applyInput(
		props,
		selected && valueSys.equals( HueblerEvertMC.SYSNAME ) );

    // Optionen fuer Huebler-Grafik-MC
    this.hgmcSettingsFld.applyInput(
		props,
		selected && valueSys.equals( HueblerGraphicsMC.SYSNAME ) );

    // Optionen fuer KC85/1..5
    this.kc85_1_SettingsFld.applyInput(
		props,
		selected && valueSys.equals( Z9001.SYSNAME_KC85_1 ) );
    this.kc85_2_SettingsFld.applyInput(
		props,
		selected && valueSys.equals( KC85.SYSNAME_KC85_2 ) );
    this.kc85_3_SettingsFld.applyInput(
		props,
		selected && valueSys.equals( KC85.SYSNAME_KC85_3 ) );
    this.kc85_4_SettingsFld.applyInput(
		props,
		selected && valueSys.equals( KC85.SYSNAME_KC85_4 ) );
    this.kc85_5_SettingsFld.applyInput(
		props,
		selected && valueSys.equals( KC85.SYSNAME_KC85_5 ) );

    // Optionen fuer KC87
    this.kc87SettingsFld.applyInput(
		props,
		selected && valueSys.equals( Z9001.SYSNAME_KC87 ) );

    // Optionen fuer KC compact
    this.kcCompactSettingsFld.applyInput(
		props,
		selected && valueSys.equals( KCcompact.SYSNAME ) );

    // Optionen fuer Kramer-MC
    this.kramerMCSettingsFld.applyInput(
		props,
		selected && valueSys.equals( KramerMC.SYSNAME ) );

    // Optionen fuer LC80
    this.lc80SettingsFld.applyInput(
		props,
		selected && valueSys.startsWith( LC80.SYSNAME_LC80 ) );

    // Optionen fuer LLC1
    this.llc1SettingsFld.applyInput(
		props,
		selected && valueSys.equals( LLC1.SYSNAME ) );

    // Optionen fuer LLC2
    this.llc2SettingsFld.applyInput(
		props,
		selected && valueSys.equals( LLC2.SYSNAME ) );

    // Optionen fuer NANOS
    this.nanosSettingsFld.applyInput(
		props,
		selected && valueSys.equals( NANOS.SYSNAME ) );

    // Optionen fuer PC/M
    this.pcmSettingsFld.applyInput(
		props,
		selected && valueSys.equals( PCM.SYSNAME ) );

    // Optionen fuer Poly880
    this.poly880SettingsFld.applyInput(
		props,
		selected && valueSys.equals( Poly880.SYSNAME ) );

    // Optionen fuer Z1013
    this.z1013SettingsFld.applyInput(
		props,
		selected && valueSys.startsWith( Z1013.SYSNAME ) );

    // Optionen fuer Z9001
    this.z9001SettingsFld.applyInput(
		props,
		selected && valueSys.equals( Z9001.SYSNAME_Z9001 ) );

    // Optionen fuer ZX Spectrum
    this.zxSpectrumSettingsFld.applyInput(
		props,
		selected && valueSys.equals( ZXSpectrum.SYSNAME ) );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.rbA5105)
	 || (src == this.rbAC1)
	 || (src == this.rbBCS3)
	 || (src == this.rbC80)
	 || (src == this.rbCustomSys)
	 || (src == this.rbHC900)
	 || (src == this.rbHEMC)
	 || (src == this.rbHGMC)
	 || (src == this.rbKC85_1)
	 || (src == this.rbKC85_2)
	 || (src == this.rbKC85_3)
	 || (src == this.rbKC85_4)
	 || (src == this.rbKC85_5)
	 || (src == this.rbKC87)
	 || (src == this.rbKCcompact)
	 || (src == this.rbKramerMC)
	 || (src == this.rbLC80)
	 || (src == this.rbLLC1)
	 || (src == this.rbLLC2)
	 || (src == this.rbNANOS)
	 || (src == this.rbPCM)
	 || (src == this.rbPoly880)
	 || (src == this.rbSC2)
	 || (src == this.rbSLC1)
	 || (src == this.rbVCS80)
	 || (src == this.rbZ1013)
	 || (src == this.rbZ9001)
	 || (src == this.rbZXSpectrum) )
    {
      rv = true;
      updOptCard();
      fireDataChanged();
      this.settingsFrm.fireUpdSpeedTab();
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    // Auswahl des emulierten System
    switch( EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME ) ) {
      case AC1.SYSNAME:
	this.rbAC1.setSelected( true );
	break;
      case BCS3.SYSNAME:
	this.rbBCS3.setSelected( true );
	break;
      case C80.SYSNAME:
	this.rbC80.setSelected( true );
	break;
      case CustomSys.SYSNAME:
	this.rbCustomSys.setSelected( true );
	break;
      case KC85.SYSNAME_HC900:
	this.rbHC900.setSelected( true );
	break;
      case HueblerEvertMC.SYSNAME:
	this.rbHEMC.setSelected( true );
	break;
      case HueblerGraphicsMC.SYSNAME:
	this.rbHGMC.setSelected( true );
	break;
      case Z9001.SYSNAME_KC85_1:
	this.rbKC85_1.setSelected( true );
	break;
      case KC85.SYSNAME_KC85_2:
	this.rbKC85_2.setSelected( true );
	break;
      case KC85.SYSNAME_KC85_3:
	this.rbKC85_3.setSelected( true );
	break;
      case KC85.SYSNAME_KC85_4:
	this.rbKC85_4.setSelected( true );
	break;
      case KC85.SYSNAME_KC85_5:
	this.rbKC85_5.setSelected( true );
	break;
      case Z9001.SYSNAME_KC87:
	this.rbKC87.setSelected( true );
	break;
      case KCcompact.SYSNAME:
	this.rbKCcompact.setSelected( true );
	break;
      case KramerMC.SYSNAME:
	this.rbKramerMC.setSelected( true );
	break;
      case LC80.SYSNAME_LC80_U505:
      case LC80.SYSNAME_LC80_2716:
      case LC80.SYSNAME_LC80_2:
      case LC80.SYSNAME_LC80_E:
      case LC80.SYSNAME_LC80_EX:
	this.rbLC80.setSelected( true );
	break;
      case LLC1.SYSNAME:
	this.rbLLC1.setSelected( true );
	break;
      case LLC2.SYSNAME:
	this.rbLLC2.setSelected( true );
	break;
      case NANOS.SYSNAME:
	this.rbNANOS.setSelected( true );
	break;
      case PCM.SYSNAME:
	this.rbPCM.setSelected( true );
	break;
      case Poly880.SYSNAME:
	this.rbPoly880.setSelected( true );
	break;
      case SC2.SYSNAME:
	this.rbSC2.setSelected( true );
	break;
      case SLC1.SYSNAME:
	this.rbSLC1.setSelected( true );
	break;
      case VCS80.SYSNAME:
	this.rbVCS80.setSelected( true );
	break;
      case Z1013.SYSNAME_Z1013_01:
      case Z1013.SYSNAME_Z1013_12:
      case Z1013.SYSNAME_Z1013_16:
      case Z1013.SYSNAME_Z1013_64:
	this.rbZ1013.setSelected( true );
	break;
      case Z9001.SYSNAME_Z9001:
	this.rbZ9001.setSelected( true );
	break;
      case ZXSpectrum.SYSNAME:
	this.rbZXSpectrum.setSelected( true );
	break;
      default:
	this.rbA5105.setSelected( true );
    }

    // Optionen der einzelnen Systeme
    this.a5105SettingsFld.updFields( props );
    this.ac1SettingsFld.updFields( props );
    this.bcs3SettingsFld.updFields( props );
    this.customSysSettingsFld.updFields( props );
    this.hc900SettingsFld.updFields( props );
    this.hemcSettingsFld.updFields( props );
    this.hgmcSettingsFld.updFields( props );
    this.kramerMCSettingsFld.updFields( props );
    this.kc85_1_SettingsFld.updFields( props );
    this.kc85_2_SettingsFld.updFields( props );
    this.kc85_3_SettingsFld.updFields( props );
    this.kc85_4_SettingsFld.updFields( props );
    this.kc85_5_SettingsFld.updFields( props );
    this.kc87SettingsFld.updFields( props );
    this.kcCompactSettingsFld.updFields( props );
    this.llc1SettingsFld.updFields( props );
    this.llc2SettingsFld.updFields( props );
    this.nanosSettingsFld.updFields( props );
    this.pcmSettingsFld.updFields( props );
    this.poly880SettingsFld.updFields( props );
    this.lc80SettingsFld.updFields( props );
    this.z1013SettingsFld.updFields( props );
    this.z9001SettingsFld.updFields( props );
    this.zxSpectrumSettingsFld.updFields( props );

    // Optionen anpassen
    updOptCard();
  }


	/* --- private Methoden --- */

  private void updOptCard()
  {
    String cardName = CARD_EMPTY;
    if( this.rbA5105.isSelected() ) {
      cardName = A5105.SYSNAME;
    }
    else if( this.rbAC1.isSelected() ) {
      cardName = AC1.SYSNAME;
    }
    else if( this.rbBCS3.isSelected() ) {
      cardName = BCS3.SYSNAME;
    }
    else if( this.rbCustomSys.isSelected() ) {
      cardName = CustomSys.SYSNAME;
    }
    else if( this.rbHC900.isSelected() ) {
      cardName = KC85.SYSNAME_HC900;
    }
    else if( this.rbHEMC.isSelected() ) {
      cardName = HueblerEvertMC.SYSNAME;
    }
    else if( this.rbHGMC.isSelected() ) {
      cardName = HueblerGraphicsMC.SYSNAME;
    }
    else if( this.rbKC85_1.isSelected() ) {
      cardName = Z9001.SYSNAME_KC85_1;
    }
    else if( this.rbKC85_2.isSelected() ) {
      cardName = KC85.SYSNAME_KC85_2;
    }
    else if( this.rbKC85_3.isSelected() ) {
      cardName = KC85.SYSNAME_KC85_3;
    }
    else if( this.rbKC85_4.isSelected() ) {
      cardName = KC85.SYSNAME_KC85_4;
    }
    else if( this.rbKC85_5.isSelected() ) {
      cardName = KC85.SYSNAME_KC85_5;
    }
    else if( this.rbKC87.isSelected() ) {
      cardName = Z9001.SYSNAME_KC87;
    }
    else if( this.rbKCcompact.isSelected() ) {
      cardName = KCcompact.SYSNAME;
    }
    else if( this.rbKramerMC.isSelected() ) {
      cardName = KramerMC.SYSNAME;
    }
    else if( this.rbLC80.isSelected() ) {
      cardName = LC80.SYSNAME;
    }
    else if( this.rbNANOS.isSelected() ) {
      cardName = NANOS.SYSNAME;
    }
    else if( this.rbPCM.isSelected() ) {
      cardName = PCM.SYSNAME;
    }
    else if( this.rbPoly880.isSelected() ) {
      cardName = Poly880.SYSNAME;
    }
    else if( this.rbLLC1.isSelected() ) {
      cardName = LLC1.SYSNAME;
    }
    else if( this.rbLLC2.isSelected() ) {
      cardName = LLC2.SYSNAME;
    }
    else if( this.rbZ1013.isSelected() ) {
      cardName = Z1013.SYSNAME;
    }
    else if( this.rbZ9001.isSelected() ) {
      cardName = Z9001.SYSNAME_Z9001;
    }
    else if( this.rbZXSpectrum.isSelected() ) {
      cardName = ZXSpectrum.SYSNAME;
    }
    this.cardLayoutSysOpt.show( this.panelOpt, cardName );
    this.curSysOptCard = cardName;
  }
}
