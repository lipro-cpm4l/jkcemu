/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen fuer die Bestaetigungen
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Properties;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.ScreenFrm;


public class ConfirmSettingsFld extends AbstractSettingsFld
{
  private JCheckBox cbConfirmNMI;
  private JCheckBox cbConfirmReset;
  private JCheckBox cbConfirmPowerOn;
  private JCheckBox cbConfirmQuit;


  public ConfirmSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    setLayout( new BorderLayout() );

    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    add( GUIFactory.createScrollPane( panel ), BorderLayout.CENTER );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panel.add(
	GUIFactory.createLabel( "Folgende Aktionen m\u00FCssen"
			+ " in einem Dialog best\u00E4tigt werden:" ),
	gbc );

    this.cbConfirmNMI = GUIFactory.createCheckBox(
		"Nicht maskierbarer Interrupt (NMI)",
		true );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    panel.add( this.cbConfirmNMI, gbc );

    this.cbConfirmReset = GUIFactory.createCheckBox(
		"Emulator zur\u00FCcksetzen (RESET)" );
    gbc.gridy++;
    panel.add( this.cbConfirmReset, gbc );

    this.cbConfirmPowerOn = GUIFactory.createCheckBox(
		"Einschalten emulieren (Arbeitsspeicher l\u00F6schen)" );
    gbc.gridy++;
    panel.add( this.cbConfirmPowerOn, gbc );

    this.cbConfirmQuit = GUIFactory.createCheckBox( "Emulator beenden" );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    panel.add( this.cbConfirmQuit, gbc );

    // Listener
    this.cbConfirmNMI.addActionListener( this );
    this.cbConfirmReset.addActionListener( this );
    this.cbConfirmPowerOn.addActionListener( this );
    this.cbConfirmQuit.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JCheckBox ) {
	fireDataChanged();
      }
    }
  }


  @Override
  public void applyInput( Properties props, boolean selected )
  {
    EmuUtil.setProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_NMI,
		this.cbConfirmNMI.isSelected() );
    EmuUtil.setProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_RESET,
		this.cbConfirmReset.isSelected() );
    EmuUtil.setProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_POWER_ON,
		this.cbConfirmPowerOn.isSelected() );
    EmuUtil.setProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_QUIT,
		this.cbConfirmQuit.isSelected() );
  }


  @Override
  public void updFields( Properties props )
  {
    this.cbConfirmNMI.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_NMI,
		ScreenFrm.DEFAULT_CONFIRM_NMI ) );
    this.cbConfirmReset.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_RESET,
		ScreenFrm.DEFAULT_CONFIRM_RESET ) );
    this.cbConfirmPowerOn.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_POWER_ON,
		ScreenFrm.DEFAULT_CONFIRM_POWER_ON ) );
    this.cbConfirmQuit.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_CONFIRM_QUIT,
		ScreenFrm.DEFAULT_CONFIRM_QUIT ) );
  }
}
