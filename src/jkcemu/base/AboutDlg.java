/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Informationen ueber JKCEMU
 */

package jkcemu.base;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.lang.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.NoSuchElementException;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import jkcemu.Main;


public class AboutDlg extends BaseDlg
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

    JLabel label = new JLabel( Main.APPINFO );
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
	new JLabel( "(c) 2008-2016 Jens M\u00FCller" ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Lizenz: GNU General Public License Version 3" ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Im JKCEMU sind ROM- und Disketteninhalte enthaltenen," ),
	gbcGeneral );

    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "die nicht der GNU General Public License unterliegen." ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	new JLabel( "Lesen Sie dazu bitte die Hinweise zu den"
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


    // Tab Java
    Properties props = System.getProperties();
    if( props != null ) {
      int n = props.size();
      if( n > 0 ) {
	java.util.List<String> propNames = new ArrayList<String>( n );
	try {
	  Enumeration<?> keys = props.propertyNames();
	  while( keys.hasMoreElements() ) {
	    Object o = keys.nextElement();
	    if( o != null ) {
	      String s = o.toString();
	      if( s != null ) {
		propNames.add( s );
	      }
	    }
	  }
	}
	catch( NoSuchElementException ex ) {}
	if( !propNames.isEmpty() ) {
	  try {
	    Collections.sort( propNames );
	  }
	  catch( ClassCastException ex1 ) {}
	  catch( IllegalArgumentException ex2 ) {}
	  try {
	    StringBuilder buf = new StringBuilder( 2048 );
	    buf.append( "<html>\n"
		+ "<h2>Eigenschaften der Java-Laufzeitumgebung</h2>\n"
		+ "<table border=\"1\">\n"
		+ "<tr><th align=\"left\">Eigenschaft</th>"
		+ "<th align=\"left\">Wert</th></tr>\n" );
	    for( String propName : propNames ) {
	      buf.append( "<tr><td align=\"left\" nowrap=\"nowrap\">" );
	      EmuUtil.appendHTML( buf, propName );
	      buf.append( "</td><td align=\"left\" nowrap=\"nowrap\">" );
	      String s = props.getProperty( propName );
	      if( s != null ) {
		s = s.replace( "\t", "\\t" );
		s = s.replace( "\r", "\\r" );
		s = s.replace( "\n", "\\n" );
		EmuUtil.appendHTML( buf, s );
	      }
	      buf.append( "</td></tr>\n" );
	    }
	    buf.append( "</table>\n"
			+ "</html>\n" );
	    JEditorPane editorPane = createJEditorPane( null );
	    editorPane.setContentType( "text/html" );
	    editorPane.setText( buf.toString() );
	    try {
	      editorPane.setCaretPosition( 0 );
	    }
	    catch( IllegalArgumentException ex ) {}
	    JPanel panel= new JPanel( new BorderLayout() );
	    panel.add( new JScrollPane( editorPane ), BorderLayout.CENTER );
	    tabbedPane.addTab( "Java", panel );
	  }
	  catch( IOException ex ) {}
	}
      }
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

  @Override
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
    if( url != null ) {
      fld.setPage( url );
    }
    fld.setPreferredSize( new Dimension( 1, 1 ) );
    return fld;
  }
}
