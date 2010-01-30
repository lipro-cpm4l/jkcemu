/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Informationen ueber JKCEMU
 */

package jkcemu.base;

import java.awt.*;
import java.io.IOException;
import java.lang.*;
import java.net.URL;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;


public class AboutDlg extends BasicDlg
{
  private JButton btnOK;


  public AboutDlg( Window owner )
  {
    super( owner, "\u00DCber JKCEMU..." );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 5, 5 ),
                                        	0, 0 );

    JTabbedPane tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( tabbedPane, gbc );


    // Tab Allgemein
    JPanel panelGeneral = new JPanel( new GridBagLayout() );
    tabbedPane.addTab( "Allgemein", panelGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, GridBagConstraints.REMAINDER,
					1.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 10, 10, 10, 10 ),
                                       	0, 0 );

    URL url = getClass().getResource( "/images/icon/jkcemu50x50.png" );
    if( url != null ) {
      panelGeneral.add( new JLabel( new ImageIcon( url ) ), gbcGeneral );
      gbcGeneral.gridx++;
    }

    JLabel label = new JLabel( Main.getVersion() );
    label.setFont( new Font( "SansSerif", Font.BOLD, 18 ) );
    gbcGeneral.insets.bottom = 0;
    gbcGeneral.gridheight    = 1;
    panelGeneral.add( label, gbcGeneral );

    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "...ein in Java geschriebener Kleincomputer-Emulator" ),
	gbcGeneral );

    gbcGeneral.insets.top = 12;
    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "(c) 2008-2010 Jens M\u00FCller" ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Lizenz: GNU General Public License Version 3" ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "In JKCEMU sind ROM- und Disketteninhalte enthaltenen," ),
	gbcGeneral );

    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "die nicht der GNU General Public License unterliegen." ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Lesen Sie dazu bitte auch die Hinweise zu den"
			+ " Urheberschaften!" ),
	gbcGeneral );
    gbcGeneral.insets.top = 12;
    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Die Anwendung dieser Software erfolgt"
			+ " ausschlie\u00DFlich auf eigenes Risiko." ),
	gbcGeneral );

    gbcGeneral.insets.top    = 0;
    gbcGeneral.insets.bottom = 10;
    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Jegliche Gew\u00E4hrleistung und Haftung"
			+ " ist ausgeschlossen!" ),
	gbcGeneral );


    // Tab Urheberschaften
    url = getClass().getResource( "/help/copyright.htm" );
    if( url != null ) {
      try {
	JPanel panel = new JPanel( new BorderLayout() );
	panel.add(
		new JScrollPane( createJEditorPane( url ) ),
		BorderLayout.CENTER );
	tabbedPane.addTab( "Urheberschaften", panel );
      }
      catch( IOException ex ) {}
    }


    // Tab Dank
    url = getClass().getResource( "/help/thanks.htm" );
    if( url != null ) {
      try {
	JPanel panel = new JPanel( new BorderLayout() );
	panel.add(
		new JScrollPane( createJEditorPane( url ) ),
		BorderLayout.CENTER );
	tabbedPane.addTab( "Danksagung", panel );
      }
      catch( IOException ex ) {}
    }


    // Knopf
    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( this.btnOK, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
        if( src == this.btnOK ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private JEditorPane createJEditorPane( URL url ) throws IOException
  {
    JEditorPane fld = new JEditorPane();
    fld.setMargin( new Insets( 5, 5, 5, 5 ) );
    fld.setEditable( false );
    fld.setPage( url );
    fld.setPreferredSize( new Dimension( 1, 1 ) );
    return fld;
  }
}

