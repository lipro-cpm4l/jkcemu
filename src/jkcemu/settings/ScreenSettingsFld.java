/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen fuer die Bildschirmausgabe
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Properties;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.ScreenFld;
import jkcemu.base.ScreenFrm;


public class ScreenSettingsFld
			extends AbstractSettingsFld
			implements ChangeListener
{
  private static final int MAX_MARGIN = 199;

  private JCheckBox          cbDirectCopyPaste;
  private JComboBox<String>  comboScreenRefresh;
  private JSlider            sliderBrightness;
  private JSpinner           spinnerMargin;
  private SpinnerNumberModel spinnerModelMargin;


  public ScreenSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    setLayout( new BorderLayout() );

    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    add( GUIFactory.createScrollPane( panel ), BorderLayout.CENTER );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 10, 5, 10, 5 ),
					0, 0 );

    panel.add( GUIFactory.createLabel( "Helligkeit [%]:" ), gbc );

    this.sliderBrightness = GUIFactory.createSlider(
					SwingConstants.HORIZONTAL,
					0,
					100,
					ScreenFld.DEFAULT_BRIGHTNESS );
    this.sliderBrightness.setMajorTickSpacing( 20 );
    this.sliderBrightness.setPaintLabels( true );
    this.sliderBrightness.setPaintTrack( true );
    this.sliderBrightness.setSnapToTicks( false );
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    panel.add( this.sliderBrightness, gbc );

    gbc.anchor    = GridBagConstraints.EAST;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    panel.add( GUIFactory.createLabel( "Rand:" ), gbc );

    this.spinnerModelMargin = new SpinnerNumberModel(
					ScreenFld.DEFAULT_MARGIN,
					0,
					MAX_MARGIN,
					1 );
    this.spinnerMargin = GUIFactory.createSpinner( this.spinnerModelMargin );

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill   = GridBagConstraints.HORIZONTAL;
    gbc.gridx++;
    panel.add( this.spinnerMargin, gbc );

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx++;
    panel.add( GUIFactory.createLabel( "Pixel" ), gbc );

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridx  = 0;
    gbc.gridy++;
    panel.add( GUIFactory.createLabel( "Aktualisierungszyklus:" ), gbc );

    this.comboScreenRefresh = GUIFactory.createComboBox();
    this.comboScreenRefresh.setEditable( false );
    this.comboScreenRefresh.addItem( "10" );
    this.comboScreenRefresh.addItem( "20" );
    this.comboScreenRefresh.addItem( "30" );
    this.comboScreenRefresh.addItem( "50" );
    this.comboScreenRefresh.addItem( "100" );
    this.comboScreenRefresh.addItem( "200" );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill   = GridBagConstraints.HORIZONTAL;
    gbc.gridx++;
    panel.add( this.comboScreenRefresh, gbc );

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx++;
    panel.add( GUIFactory.createLabel( "ms" ), gbc );

    this.cbDirectCopyPaste = GUIFactory.createCheckBox(
		"Direktes \"Kopieren & Einf\u00FCgen\" durch Dr\u00FCcken"
			+ " der mittleren Maustaste",
		true );
    gbc.anchor     = GridBagConstraints.CENTER;
    gbc.insets.top = 10;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    panel.add( this.cbDirectCopyPaste, gbc );

    // gleicher Font von JSpinner und JComboBox
    Font font = this.spinnerMargin.getFont();
    if( font != null ) {
      this.comboScreenRefresh.setFont( font );
    }

    // Listener
    this.sliderBrightness.addChangeListener( this );
    this.spinnerMargin.addChangeListener( this );
    this.comboScreenRefresh.addActionListener( this );
    this.cbDirectCopyPaste.addActionListener( this );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( (src == this.sliderBrightness) || (src == this.spinnerMargin) ) {
      fireDataChanged();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( (src == this.comboScreenRefresh)
	|| (src == this.cbDirectCopyPaste) )
    {
      fireDataChanged();
    }
  }


  @Override
  public void applyInput( Properties props, boolean selected )
  {
    props.setProperty(
		ScreenFld.PROP_BRIGHTNESS,
		String.valueOf( this.sliderBrightness.getValue() ) );

    Object obj = this.spinnerMargin.getValue();
    props.setProperty(
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_SCREEN_MARGIN,
		obj != null ? obj.toString() : "0" );

    obj = this.comboScreenRefresh.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null ) {
	props.setProperty(
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_SCREEN_REFRESH_MS,
		text );
      }
    }

    props.setProperty(
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_COPY_AND_PASTE_DIRECT,
		Boolean.toString( this.cbDirectCopyPaste.isSelected() ) );
  }


  @Override
  public void updFields( Properties props )
  {
    int brightness = EmuUtil.getIntProperty(
					props,
					ScreenFld.PROP_BRIGHTNESS,
					ScreenFld.DEFAULT_BRIGHTNESS );
    if( (brightness >= 0) && (brightness <= 100) ) {
      this.sliderBrightness.setValue( brightness );
    }
    try {
      int margin = EmuUtil.getIntProperty(
			props,
			ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_SCREEN_MARGIN,
			ScreenFld.DEFAULT_MARGIN );
      if( (margin >= 0) && (margin <= MAX_MARGIN) ) {
	this.spinnerModelMargin.setValue( margin );
      }
    }
    catch( IllegalArgumentException ex ) {}

    String screenRefreshMillis = EmuUtil.getProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_SCREEN_REFRESH_MS );
    if( screenRefreshMillis.isEmpty() ) {
      int       millis    = 0;
      ScreenFrm screenFrm = Main.getScreenFrm();
      if( screenFrm != null ) {
	millis = screenFrm.getScreenRefreshMillis();
      }
      if( millis < 1 ) {
	millis = ScreenFrm.getDefaultScreenRefreshMillis();
      }
      screenRefreshMillis = String.valueOf( millis );
    }
    this.comboScreenRefresh.setSelectedItem( screenRefreshMillis );

    this.cbDirectCopyPaste.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		ScreenFrm.PROP_PREFIX + ScreenFrm.PROP_COPY_AND_PASTE_DIRECT,
		ScreenFrm.DEFAULT_COPY_AND_PASTE_DIRECT ) );
  }
}
