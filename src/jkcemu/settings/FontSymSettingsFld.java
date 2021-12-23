/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen der Schriftarten und Symbole
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import jkcemu.base.EmuUtil;
import jkcemu.base.FontMngr;
import jkcemu.base.GUIFactory;


public class FontSymSettingsFld extends AbstractSettingsFld
{
  private boolean          oldSymLarge;
  private FontSelectionFld fontSelectionCode;
  private FontSelectionFld fontSelectionGeneral;
  private FontSelectionFld fontSelectionHtml;
  private FontSelectionFld fontSelectionInput;
  private FontSelectionFld fontSelectionMenu;
  private JRadioButton     rbSymSmall;
  private JRadioButton     rbSymLarge;


  public FontSymSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    this.oldSymLarge = false;
    setLayout( new BorderLayout() );

    JPanel panelAll = GUIFactory.createPanel( new GridBagLayout() );
    add( GUIFactory.createScrollPane( panelAll ), BorderLayout.CENTER );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Bereich Schriftarten
    JPanel panelFont = GUIFactory.createPanel( new GridBagLayout() );
    panelFont.setBorder( GUIFactory.createTitledBorder( "Schriftarten" ) );
    panelAll.add( panelFont, gbc );

    GridBagConstraints gbcFont = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.fontSelectionGeneral = new FontSelectionFld(
			this.settingsFrm,
			"Schrift f\u00FCr allgemeine Beschriftungen:",
			FontMngr.FontUsage.GENERAL );
    panelFont.add( this.fontSelectionGeneral, gbcFont );

    gbcFont.insets.top = 10;
    gbcFont.gridy++;
    this.fontSelectionMenu = new FontSelectionFld(
					this.settingsFrm,
					"Schrift f\u00FCr Men\u00FCs:",
					FontMngr.FontUsage.MENU );
    panelFont.add( this.fontSelectionMenu, gbcFont );

    this.fontSelectionInput = new FontSelectionFld(
					this.settingsFrm,
					"Schrift f\u00FCr Eingabefelder:",
					FontMngr.FontUsage.INPUT );
    gbcFont.gridy++;
    panelFont.add( this.fontSelectionInput, gbcFont );

    this.fontSelectionCode = new FontSelectionFld(
					this.settingsFrm,
					"Schrift f\u00FCr Code-Bereiche:",
					FontMngr.FontUsage.CODE );
    gbcFont.gridy++;
    panelFont.add( this.fontSelectionCode, gbcFont );

    this.fontSelectionHtml = new FontSelectionFld(
			this.settingsFrm,
			"Schrift f\u00FCr Hilfeseiten und HTML-Ausgaben:",
			FontMngr.FontUsage.HTML );
    gbcFont.gridy++;
    panelFont.add( this.fontSelectionHtml, gbcFont );


    // Bereich Symbols
    JPanel panelSym = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelSym.setBorder( GUIFactory.createTitledBorder( "Symbole" ) );
    gbc.insets.top = 10;
    gbc.gridy++;
    panelAll.add( panelSym, gbc );

    ButtonGroup grpSym = new ButtonGroup();

    this.rbSymSmall = GUIFactory.createRadioButton( "Kleine Symbole", true );
    grpSym.add( this.rbSymSmall );
    panelSym.add( this.rbSymSmall );

    this.rbSymLarge = GUIFactory.createRadioButton( "Gro\u00DFe Symbole" );
    grpSym.add( this.rbSymLarge );
    panelSym.add( this.rbSymLarge );


    // Listener
    this.rbSymSmall.addActionListener( this );
    this.rbSymLarge.addActionListener( this );
  }


  public boolean hasFontsChanged()
  {
    return this.fontSelectionMenu.hasFontChanged()
		|| this.fontSelectionGeneral.hasFontChanged()
		|| this.fontSelectionInput.hasFontChanged()
		|| this.fontSelectionCode.hasFontChanged()
		|| this.fontSelectionHtml.hasFontChanged();
  }


  public boolean hasSymbolsChanged()
  {
    return (this.rbSymLarge.isSelected() != this.oldSymLarge);
  }


  public void resetChanged()
  {
    this.fontSelectionMenu.resetChanged();
    this.fontSelectionGeneral.resetChanged();
    this.fontSelectionInput.resetChanged();
    this.fontSelectionCode.resetChanged();
    this.fontSelectionHtml.resetChanged();
    this.oldSymLarge = this.rbSymLarge.isSelected();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JRadioButton ) {
	fireDataChanged();
      }
    }
  }


  @Override
  public void applyInput( Properties props, boolean selected )
  {
    this.fontSelectionMenu.applySelectedFont( props );
    this.fontSelectionGeneral.applySelectedFont( props );
    this.fontSelectionInput.applySelectedFont( props );
    this.fontSelectionCode.applySelectedFont( props );
    this.fontSelectionHtml.applySelectedFont( props );
    EmuUtil.setProperty(
		props,
		GUIFactory.PROP_LARGE_SYMBOLS,
		this.rbSymLarge.isSelected() );
  }


  @Override
  public void updFields( Properties props )
  {
    this.fontSelectionMenu.updFields( props );
    this.fontSelectionGeneral.updFields( props );
    this.fontSelectionInput.updFields( props );
    this.fontSelectionCode.updFields( props );
    this.fontSelectionHtml.updFields( props );

    this.oldSymLarge = GUIFactory.getLargeSymbolsEnabled( props );
    if( this.oldSymLarge ) {
      this.rbSymLarge.setSelected( true );
    } else {
      this.rbSymSmall.setSelected( true );
    }
  }
}
