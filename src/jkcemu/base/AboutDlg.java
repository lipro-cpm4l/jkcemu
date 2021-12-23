/*
 * (c) 2009-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Informationen ueber JKCEMU
 */

package jkcemu.base;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
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
  private static Rectangle lastBounds = null;

  private JButton btnOK;


  public static void fireOpen( final Window owner )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    open( owner );
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e.getSource() == this.btnOK ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    lastBounds = getBounds();
    boolean rv = super.doClose();
    if( rv ) {
      this.btnOK.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowClosing( WindowEvent e )
  {
    lastBounds = getBounds();
    super.windowClosing( e );
  }


	/* --- Konstruktor --- */

  private AboutDlg( Window owner )
  {
    super( owner, Dialog.ModalityType.MODELESS );
    setTitle( "\u00DCber JKCEMU..." );


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

    JTabbedPane tabbedPane = GUIFactory.createTabbedPane();
    add( tabbedPane, gbc );


    // Tab Allgemein
    JPanel panelGeneral = GUIFactory.createPanel( new GridBagLayout() );
    tabbedPane.addTab( "Allgemein", panelGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, GridBagConstraints.REMAINDER,
					1.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 10, 10, 10, 10 ),
                                       	0, 0 );

    URL url = getClass().getResource( "/images/icon/jkcemu_48x48.png" );
    if( url != null ) {
      panelGeneral.add(
		GUIFactory.createLabel( new ImageIcon( url ) ),
		gbcGeneral );
      gbcGeneral.gridx++;
    }

    gbcGeneral.insets.bottom = 0;
    gbcGeneral.gridheight    = 1;
    panelGeneral.add(
	GUIFactory.createLabel(
			Main.APPINFO,
			Font.SANS_SERIF,
			Font.BOLD,
			1.5F ),
	gbcGeneral );

    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel(
		"...ein in Java geschriebener Kleincomputer-Emulator" ),
	gbcGeneral );

    gbcGeneral.insets.top = 12;
    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel( Main.COPYRIGHT ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel(
		"Lizenz: GNU General Public License Version 3" ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel(
		"Im JKCEMU sind ROM- und Disketteninhalte enthaltenen," ),
	gbcGeneral );

    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel(
		"die nicht der GNU General Public License unterliegen." ),
	gbcGeneral );

    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel( "Lesen Sie dazu bitte die Hinweise zu den"
			+ " Urheberschaften!" ),
	gbcGeneral );
    gbcGeneral.insets.top = 12;
    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel( "Die Anwendung dieser Software erfolgt"
			+ " ausschlie\u00DFlich auf eigenes Risiko." ),
	gbcGeneral );

    gbcGeneral.insets.top    = 0;
    gbcGeneral.insets.bottom = 10;
    gbcGeneral.gridy++;
    panelGeneral.add(
	GUIFactory.createLabel( "Jegliche Gew\u00E4hrleistung und Haftung"
			+ " ist ausgeschlossen!" ),
	gbcGeneral );


    // Tab Urheberschaften
    url = getClass().getResource( "/help/copyright.htm" );
    if( url != null ) {
      try {
	JPanel panel = GUIFactory.createPanel( new BorderLayout() );
	panel.add(
		GUIFactory.createScrollPane( createJEditorPane( url ) ),
		BorderLayout.CENTER );
	tabbedPane.addTab( "Urheberschaften", panel );
      }
      catch( IOException ex ) {}
    }


    // Tab Dank
    url = getClass().getResource( "/help/thanks.htm" );
    if( url != null ) {
      try {
	JPanel panel = GUIFactory.createPanel( new BorderLayout() );
	panel.add(
		GUIFactory.createScrollPane( createJEditorPane( url ) ),
		BorderLayout.CENTER );
	tabbedPane.addTab( "Danksagung", panel );
      }
      catch( IOException ex ) {}
    }


    // Tab Java
    try {
      StringBuilder buf = new StringBuilder( 2048 );

      // Eigenschaften
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

	    buf.append( "<html>\n"
			+ "<h2>Eigenschaften der"
			+ " Java-Laufzeitumgebung</h2>\n"
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
	    buf.append( "</table>\n" );
	  }
	}
      }

      // Bibliothek
      DeviceIO.LibInfo libInfo = DeviceIO.getLibInfo();
      if( libInfo != null ) {
	int recognizedVersion = libInfo.getRecognizedVersion();
	int requiredVersion   = libInfo.getRequiredVersion();
	if( buf.length() > 0 ) {
	  buf.append( "<br/><br/>\n" );
	} else {
	  buf.append( "<html>\n" );
	}
	buf.append( "<h2>Bibliothek mit nativem Programmcode</h2>\n"
		+ "<table border=\"1\">\n"
		+ "<tr><td align=\"left\">Status:</td><td align=\"left\">" );
	switch( libInfo.getStatus() ) {
	  case NOT_USED:
	    buf.append( "Bibliothek nicht geladen,"
		+ " da noch nicht ben\u00F6tigt" );
	    break;
	  case LOADED:
	    buf.append( "Bibliothek geladen" );
	    if( (recognizedVersion >= 0) && (requiredVersion >= 0) ) {
	      if( recognizedVersion >= requiredVersion ) {
		buf.append( " und in Verwendnung" );
	      } else {
		buf.append( ", aber nicht in Verwendnung" );
	      }
	    }
	    break;
	  case LOAD_ERROR:
	    buf.append( "Bibliothek konnte nicht geladen werden." );
	    break;
	  case INSTALL_ERROR:
	    buf.append( "Bibliothek konnte nicht installiert werden." );
	    break;
	}
	buf.append( "</td></tr>\n" );
	File file = libInfo.getFile();
	if( file != null ) {
	  buf.append( "<tr><td align=\"left\">Datei:</td>"
					+ "<td align=\"left\">" );
	  EmuUtil.appendHTML( buf, file.getPath() );
	  buf.append( "</td></tr>\n" );
	  if( libInfo.getStatus() == DeviceIO.LibStatus.LOADED ) {
	    buf.append( "<tr><td align=\"left\">Version:</td>"
					+ "<td align=\"left\">" );
	    EmuUtil.appendHTML(
			buf,
			String.valueOf( recognizedVersion ) );
	    buf.append( "</td></tr>\n" );
	    if( (recognizedVersion != requiredVersion)
		&& (requiredVersion > 0) )
	    {
	      buf.append( "<tr><td align=\"left\">"
					+ "Ben\u00F6tigte Version:</td>"
					+ "<td align=\"left\">" );
	      EmuUtil.appendHTML(
			buf,
			String.valueOf( requiredVersion ) );
	      buf.append( "</td></tr>\n" );
	    }
	  }
	}
	buf.append( "</table>\n" );
	if( (file != null) && libInfo.isUpdateRequested() ) {
	  buf.append( "<br/>\n"
		+ "Die Bibliothek wird beim n&auml;chsten Start"
		+ " von JKCEMU aktualisiert." );
	}
      }

      // Tab
      if( buf.length() > 0 ) {
	buf.append( "</html>\n" );
	JEditorPane editorPane = createJEditorPane( null );
	editorPane.setContentType( "text/html" );
	editorPane.setText( buf.toString() );
	try {
	  editorPane.setCaretPosition( 0 );
	}
	catch( IllegalArgumentException ex ) {}
	JPanel panel= GUIFactory.createPanel( new BorderLayout() );
	panel.add(
		GUIFactory.createScrollPane( editorPane ),
		BorderLayout.CENTER );
	tabbedPane.addTab( "Java", panel );
      }
    }
    catch( IOException ex ) {}


    // Knopf
    this.btnOK        = GUIFactory.createButtonOK();
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( this.btnOK, gbc );


    // Listener
    this.btnOK.addActionListener( this );


    // Sonstiges
    setResizable( true );
  }


	/* --- private Methoden --- */

  private JEditorPane createJEditorPane( URL url ) throws IOException
  {
    JEditorPane fld = GUIFactory.createEditorPane();
    fld.setMargin( new Insets( 5, 5, 5, 5 ) );
    fld.setEditable( false );
    if( url != null ) {
      fld.setPage( url );
    }
    fld.setPreferredSize( new Dimension( 1, 1 ) );
    return fld;
  }


  private static void open( Window owner )
  {
    AboutDlg dlg = new AboutDlg( owner );
    if( lastBounds != null ) {
      dlg.setBounds( lastBounds );
    } else {
      dlg.pack();
      BaseDlg.setParentCentered( dlg );
    }
    dlg.setVisible( true );
  }
}
