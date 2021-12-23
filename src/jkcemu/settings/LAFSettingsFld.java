/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen des Erscheinungsbildes
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.FontMngr;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;


public class LAFSettingsFld extends AbstractSettingsFld
{
  private Map<String,AbstractButton>  lafClass2Button;
  private boolean                     lafChanged;
  private JCheckBox                   cbScreenMenuBar;
  private ButtonGroup                 grpLAF;
  private UIManager.LookAndFeelInfo[] lafs;


  public LAFSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    this.lafClass2Button = new HashMap<>();
    this.lafChanged      = false;
    this.cbScreenMenuBar = null;
    this.grpLAF          = new ButtonGroup();
    this.lafs            = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length < 2 ) {
	this.lafs = null;
      }
    }
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

    if( this.lafs != null ) {
      panel.add(
	GUIFactory.createLabel(
			"Das Aussehen von JKCEMU k\u00F6nnen Sie durch"
				+ " Auswahl eines Erscheinungsbildes" ),
	gbc );

      gbc.insets.top    = 0;
      gbc.insets.bottom = 5;
      gbc.gridy++;
      panel.add(
	GUIFactory.createLabel( "an Ihren pers\u00F6nlichen"
				+ " Geschmack anpassen:" ),
	gbc );

      gbc.insets.left   = 50;
      gbc.insets.bottom = 0;
      for( int i = 0; i < this.lafs.length; i++ ) {
	String clName = this.lafs[ i ].getClassName();
	if( clName != null ) {
	  JRadioButton rb = GUIFactory.createRadioButton(
					this.lafs[ i ].getName() );
	  this.grpLAF.add( rb );
	  rb.setActionCommand( clName );
	  rb.addActionListener( this );
	  if( i == this.lafs.length - 1 ) {
	    gbc.insets.bottom = 5;
	  }
	  gbc.gridy++;
	  panel.add( rb, gbc );
	  this.lafClass2Button.put( clName, rb );
	}
      }
      gbc.insets.top  = 10;
      gbc.insets.left = 5;
      gbc.gridy++;
    }
    if( Main.isMacOS() ) {
      this.cbScreenMenuBar = GUIFactory.createCheckBox(
		"Men\u00FCleiste am oberen Bildschirmrand fixieren" );
      this.cbScreenMenuBar.addActionListener( this );
      gbc.insets.bottom = 5;
      panel.add( this.cbScreenMenuBar, gbc );
    }
  }


  public void checkShowRestartMsg()
  {
    if( this.cbScreenMenuBar != null ) {
      if( this.cbScreenMenuBar.isSelected()
			!= Main.isScreenMenuBarEnabled() )
      {
	BaseDlg.showInfoDlg(
		this,
		"Die \u00C4nderung an der Option \'"
			+ this.cbScreenMenuBar.getText()
			+ "\' ist nur wirksam,\n"
			+ "wenn Sie die Einstellungen als Profil speichern"
			+ " und anschlie\u00DFend "
			+ Main.APPNAME
			+ " mit diesem Profil neu starten." );
      }
    }
  }


  public boolean containsLAFSettings()
  {
    return (this.cbScreenMenuBar != null) || (this.lafs != null);
  }


  public boolean getAndResetLookAndFeelChanged()
  {
    boolean rv      = this.lafChanged;
    this.lafChanged = false;
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
	fireDataChanged();
      }
    }
  }


  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    ButtonModel bm = this.grpLAF.getSelection();
    if( bm != null ) {
      String lafClassName = bm.getActionCommand();
      if( lafClassName != null ) {
	if( lafClassName.length() > 0 ) {
	  boolean     lafChanged = true;
	  LookAndFeel oldLAF     = UIManager.getLookAndFeel();
	  if( oldLAF != null ) {
	    if( lafClassName.equals( oldLAF.getClass().getName() ) ) {
	      lafChanged = false;
	    }
	  }
	  if( lafChanged ) {
	    FontMngr.clearDefaultFontMap();
	    try {
	      UIManager.setLookAndFeel( lafClassName );
	      this.lafChanged = true;
	      props.setProperty(
			Main.PROP_LAF_CLASSNAME,
			lafClassName );
	    }
	    catch( Exception ex ) {
	      throw new UserInputException(
		"Das Erscheinungsbild kann nicht eingestellt werden." );
	    }
	  }
	  props.setProperty( Main.PROP_LAF_CLASSNAME, lafClassName );
	}
      }
    }
    if( this.cbScreenMenuBar != null ) {
      EmuUtil.setProperty(
			props,
			Main.PROP_SCREEN_MENUBAR,
			this.cbScreenMenuBar.isSelected() );
    }
  }


  @Override
  public void updFields( Properties props )
  {
    String lafClassName = EmuUtil.getProperty(
					props,
					Main.PROP_LAF_CLASSNAME );
    if( lafClassName.isEmpty() ) {
      LookAndFeel laf = UIManager.getLookAndFeel();
      if( laf != null ) {
	lafClassName = laf.getClass().getName();
      }
    }
    if( lafClassName != null ) {
      AbstractButton btn = this.lafClass2Button.get( lafClassName );
      if( btn != null ) {
	btn.setSelected( true );
      }
    }
    if( this.cbScreenMenuBar != null ) {
      this.cbScreenMenuBar.setSelected(
			EmuUtil.getBooleanProperty(
					props,
					Main.PROP_SCREEN_MENUBAR,
					false ) );
    }
  }
}
