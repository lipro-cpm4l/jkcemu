/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Aenderung der Druckoptionen
 */

package jkcemu.print;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class PrintOptionsDlg extends BasicDlg
{
  private boolean   applied;
  private JComboBox comboFontSize;
  private JCheckBox btnFileName;
  private JCheckBox btnPageNum;
  private JButton   btnOK;
  private JButton   btnCancel;


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
    return dlg.optionsApplied();
  }


  public boolean optionsApplied()
  {
    return this.applied;
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
      EmuUtil.exitSysError( this, null, ex );
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
    JPanel panelOpt = new JPanel( new GridBagLayout() );
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
      panelOpt.add( new JLabel( "Schriftgr\u00F6\u00DFe:" ), gbcOpt );
      this.comboFontSize = new JComboBox();
      this.comboFontSize.setEditable( false );
      this.comboFontSize.addItem( "6" );
      this.comboFontSize.addItem( "7" );
      this.comboFontSize.addItem( "8" );
      this.comboFontSize.addItem( "9" );
      this.comboFontSize.addItem( "10" );
      this.comboFontSize.addItem( "11" );
      this.comboFontSize.addItem( "12" );
      this.comboFontSize.addItem( "14" );
      this.comboFontSize.addItem( "16" );
      this.comboFontSize.addItem( "18" );
      this.comboFontSize.addItem( "20" );
      this.comboFontSize.addItem( "22" );
      this.comboFontSize.addItem( "24" );
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
    this.btnFileName = null;
    if( askPrintFileName ) {
      this.btnFileName = new JCheckBox(
				"Dateiname drucken",
				Main.getPrintFileName() );
      panelOpt.add( this.btnFileName, gbcOpt );
      gbcOpt.insets.top = 0;
      gbcOpt.gridwidth  = 2;
      gbcOpt.gridy++;
    }

    // Seitennummer
    this.btnPageNum = new JCheckBox(
				"Seitennummer drucken",
				Main.getPrintPageNum() );
    gbcOpt.insets.bottom = 5;
    panelOpt.add( this.btnPageNum, gbcOpt );

    gbc.gridy++;
    add( panelOpt, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
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
    if( this.btnFileName != null ) {
      Main.setPrintFileName( this.btnFileName.isSelected() );
    }
    Main.setPrintPageNum( this.btnPageNum.isSelected() );

    this.applied = true;
    doClose();
  }
}

