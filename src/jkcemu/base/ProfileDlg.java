/*
 * (c) 2008-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Auswahl eines Profils
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import jkcemu.Main;


public class ProfileDlg extends BasicDlg implements
						DocumentListener,
						ListSelectionListener
{
  private File                     selectedProfile;
  private DefaultListModel<String> listModel;
  private JList<String>            list;
  private Document                 docProfileName;
  private JTextField               fldProfileName;
  private JButton                  btnOK;
  private JButton                  btnDelete;
  private JButton                  btnCancel;


  public ProfileDlg(
		Frame   owner,
		String  title,
		String  approveLabel,
		File    preSelection,
		boolean forSave )
  {
    super( owner, title );
    this.selectedProfile = null;


    // Profile laden
    int    preSelectedIdx  = -1;
    String preSelectedName = null;
    this.listModel         = new DefaultListModel<>();
    try {
      File configDir = Main.getConfigDir();
      if( configDir != null ) {
	File[] entries = configDir.listFiles();
	if( entries != null ) {
	  Arrays.sort( entries );
	  for( int i = 0; i < entries.length; i++ ) {
	    if( entries[ i ].isFile() ) {
	      String fileName = entries[ i ].getName();
	      if( fileName != null ) {
		int len = fileName.length();
		if( (len > 8)
		    && fileName.startsWith( "prf_" )
		    && fileName.endsWith( ".xml" ) )
		{
		  String itemText = fileName.substring( 4, len - 4 );
		  if( preSelection != null ) {
		    if( preSelection.equals( entries[ i ] ) ) {
		      preSelectedIdx  = this.listModel.getSize();
		      preSelectedName = itemText;
		    }
		  }
		  this.listModel.addElement( itemText );
		}
	      }
	    }
	  }
	}
      }
    }
    catch( SecurityException ex ) {}


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    add( new JLabel( "Profile:" ), gbc );

    this.list = new JList<>( this.listModel );
    this.list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    this.list.setVisibleRowCount( 10 );
    this.list.setPrototypeCellValue( "123456789012345678901234567890" );
    gbc.fill          = GridBagConstraints.BOTH;
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.weightx       = 1.0;
    gbc.weighty       = 1.0;
    gbc.gridy++;
    add(
	new JScrollPane(
		this.list,
		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ),
	gbc );

    this.fldProfileName = null;
    this.docProfileName = null;
    if( forSave ) {
      this.fldProfileName = new JTextField();
      this.fldProfileName.addActionListener( this );
      gbc.fill       = GridBagConstraints.HORIZONTAL;
      gbc.insets.top = 5;
      gbc.weighty    = 0.0;
      gbc.gridy++;
      add( this.fldProfileName, gbc );
      if( preSelectedName != null ) {
	this.fldProfileName.setText( preSelectedName );
      } else {
	this.fldProfileName.setText( "standard" );
      }
    }

    if( preSelectedIdx >= 0 ) {
      this.list.setSelectedIndex( preSelectedIdx );
    }


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 4, 1, 5, 5 ) );

    this.btnOK = new JButton( approveLabel );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnDelete = new JButton( "L\u00F6schen" );
    this.btnDelete.addActionListener( this );
    this.btnDelete.addKeyListener( this );
    panelBtn.add( this.btnDelete );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.anchor     = GridBagConstraints.NORTHEAST;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.insets.top = 5;
    gbc.weightx    = 0.0;
    gbc.weighty    = 0.0;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelBtn, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();


    // Aktivierung/Deaktivierung der Aktionsknoepfe ermoeglichen
    if( this.fldProfileName != null ) {
      this.docProfileName = this.fldProfileName.getDocument();
      if( this.docProfileName != null ) {
	this.docProfileName.addDocumentListener( this );
      }
    }
    updOKButton();
    if( preSelectedIdx < 0 ) {
      this.btnDelete.setEnabled( false );
    }


    // sonstige Listener
    this.list.addListSelectionListener( this );
    this.list.addKeyListener( this );
    this.list.addMouseListener( this );
  }


  public File getSelectedProfile()
  {
    return this.selectedProfile;
  }


	/* --- Methoden fuer DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    updOKButton();
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    updOKButton();
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    updOKButton();
  }


	/* --- Methoden fuer ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    Object o = this.list.getSelectedValue();
    if( o != null ) {
      if( this.fldProfileName != null ) {
	this.fldProfileName.setText( o.toString() );
      }
      this.btnDelete.setEnabled( true );
    } else {
      this.btnDelete.setEnabled( false );
    }
    updOKButton();
  }


	/* --- ueberschriebene Methoden fuer MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( e != null ) {
      if( (e.getClickCount() > 1)
	  && (e.getComponent() == this.list) )
      {
	e.consume();
	doApply();
      } else {
	super.mouseClicked( e );
      }
    }
  }


	/* --- ueberschriebene Methoden fuer WindowListener --- */

  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldProfileName != null ) {
      this.fldProfileName.requestFocus();
    } else {
      this.list.requestFocus();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( (src == this.btnOK)
	    || (src == this.list)
	    || (src == this.fldProfileName) )
	{
	  rv = true;
	  doApply();
	}
	else if( src == this.btnDelete ) {
	  rv = true;
	  doDelete();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
 }


	/* --- private Methoden --- */

  private void doApply()
  {
    if( this.fldProfileName != null ) {
      String s = this.fldProfileName.getText();
      if( s != null ) {
	int len = s.length();
	if( len > 0 ) {

	  // Gueltigkeit des Namens pruefen
	  boolean status = true;
	  char    ch     = s.charAt( 0 );
	  if( (ch != '_')
	      && (ch >= 'A') && (ch <= 'Z')
	      && (ch >= 'a') && (ch <= 'z') )
	  {
	    status = false;
	  } else {
	    for( int i = 1; i < len; i++ ) {
	      ch = s.charAt( i );
	      if( (ch != '_')
		  && (ch >= 'A') && (ch <= 'Z')
		  && (ch >= 'a') && (ch <= 'z')
		  && (ch >= '0') && (ch <= '9') )
	      {
		status = false;
	      }
	    }
	  }

	  // Profilname zu Dateiname erweitern oder Fehlermeldung
	  if( status ) {
	    this.selectedProfile = getProfile( s );
	  } else {
	    BasicDlg.showErrorDlg(
		this,
		"Der ausgew\u00E4hlte Name enth\u00E4lt ung\u00FCltige"
			+ "Zeichen.\n"
			+ "Das erste Zeichen muss ein Buchstabe"
			+ " oder Unterstrich sein.\n"
			+ "Ab dem zweiten Zeichen sind zus\u00E4tzlich"
			+ " Ziffern erlaubt.",
		"Ung\u00FCltiger Profilname" );
	  }
	}
      }
    } else {
      Object o = this.list.getSelectedValue();
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    this.selectedProfile = getProfile( s );
	  }
	}
      }
    }
    if( this.selectedProfile != null ) {
      doClose();
    }
  }


  private void doDelete()
  {
    Object o = this.list.getSelectedValue();
    if( o != null ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie das Profil l\u00F6schen?" ) )
      {
	File file = getProfile( o.toString() );
	if( file.delete() ) {
	  this.listModel.removeElement( o );
	  this.btnDelete.setEnabled( false );
	} else {
	  BasicDlg.showErrorDlg(
		this,
		"Das Profil kann nicht gel\u00F6scht werden." );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static File getProfile( String profileName )
  {
    String fileName = "prf_" + profileName + ".xml";
    File configDir = Main.getConfigDir();
    return configDir != null ?
		new File( configDir, fileName )
		: new File( fileName );
  }


  private void updOKButton()
  {
    boolean state = true;
    if( this.docProfileName != null ) {
      int len = this.docProfileName.getLength();
      if( len > 0 ) {
	try {
	  String s = this.docProfileName.getText( 0, len );
	  if( s != null ) {
	    if( !s.trim().isEmpty() ) {
	      state = true;
	    }
	  }
	}
	catch( BadLocationException ex ) {}
      }
    } else {
      state = (this.list.getSelectedIndex() >= 0);
    }
    this.btnOK.setEnabled( state );
  }
}

