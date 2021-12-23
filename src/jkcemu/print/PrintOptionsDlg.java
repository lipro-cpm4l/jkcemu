/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Aenderung der Druckoptionen
 */

package jkcemu.print;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class PrintOptionsDlg extends BaseDlg
{
  private static final String[] fontSizes = {
			"6", "7", "8", "9", "10", "11", "12", "13",
			"14", "16", "18", "20", "22", "24" };

  private boolean           applied;
  private JComboBox<String> comboFontSize;
  private JCheckBox         cbFileName;
  private JCheckBox         cbPageNum;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static boolean showPrintOptionsDlg(
				Frame   owner,
				boolean askFontSize,
				boolean askFileNamePrinted )
  {
    PrintOptionsDlg dlg = new PrintOptionsDlg(
					owner,
					askFontSize,
					askFileNamePrinted );
    dlg.setVisible( true );
    return dlg.applied;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      if( e != null ) {
	Object src = e.getSource();
	if( src == this.btnOK ) {
	  rv = true;
	  doApply();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    catch( Exception ex ) {
      EmuUtil.checkAndShowError( this, null, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private PrintOptionsDlg(
		Frame   owner,
		boolean askFontSize,
		boolean askPrintFileName )
  {
    super( owner, "Druckoptionen" );
    this.applied = false;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Bereich Optionen
    JPanel panelOpt = GUIFactory.createPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createEmptyBorder() );

    GridBagConstraints gbcOpt = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Schriftgroesse
    this.comboFontSize = null;
    if( askFontSize ) {
      panelOpt.add(
		GUIFactory.createLabel( "Schriftgr\u00F6\u00DFe:" ),
		gbcOpt );
      this.comboFontSize = GUIFactory.createComboBox( fontSizes );
      this.comboFontSize.setEditable( false );
      gbcOpt.gridx++;
      panelOpt.add( this.comboFontSize, gbcOpt );
      gbcOpt.insets.top = 0;
      gbcOpt.gridwidth  = 2;
      gbcOpt.gridx      = 0;
      gbcOpt.gridy++;
      this.comboFontSize.setSelectedItem(
			String.valueOf( Main.getPrintFontSize() ) );
    }

    // Dateiname
    this.cbFileName = null;
    if( askPrintFileName ) {
      this.cbFileName = GUIFactory.createCheckBox(
				"Dateiname drucken",
				Main.getPrintFileName() );
      panelOpt.add( this.cbFileName, gbcOpt );
      gbcOpt.insets.top = 0;
      gbcOpt.gridwidth  = 2;
      gbcOpt.gridy++;
    }

    // Seitennummer
    this.cbPageNum = GUIFactory.createCheckBox(
				"Seitennummer drucken",
				Main.getPrintPageNum() );
    gbcOpt.insets.bottom = 5;
    panelOpt.add( this.cbPageNum, gbcOpt );

    gbc.gridy++;
    add( panelOpt, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill   = GridBagConstraints.NONE;
    gbc.gridx  = 0;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Listener
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


  private void doApply()
  {
    if( this.comboFontSize != null ) {
      Object o = this.comboFontSize.getSelectedItem();
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  try {
	    int fontSize = Integer.parseInt( s );
	    if( fontSize > 0 ) {
	      Main.setPrintFontSize( fontSize );
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    if( this.cbFileName != null ) {
      Main.setPrintFileName( this.cbFileName.isSelected() );
    }
    Main.setPrintPageNum( this.cbPageNum.isSelected() );

    this.applied = true;
    doClose();
  }
}
