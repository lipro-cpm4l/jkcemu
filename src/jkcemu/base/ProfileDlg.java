/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Auswahl eines Profils
 */

package jkcemu.base;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import jkcemu.Main;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;


public class ProfileDlg extends BaseDlg implements
						DocumentListener,
						ListSelectionListener
{
  private static final String PROFILE_PREFIX = "prf_";
  private static final String PROFILE_SUFFIX = ".xml";

  private boolean                  notified;
  private File                     selectedProfileFile;
  private String                   selectedProfileName;
  private Properties               selectedProfileProps;
  private int                      lastSelectedIdx;
  private DefaultListModel<String> listModel;
  private JList<String>            list;
  private Document                 docProfileName;
  private JTextField               fldProfileName;
  private JCheckBox                cbUseDefaults;
  private JButton                  btnOK;
  private JButton                  btnImport;
  private JButton                  btnExport;
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
    this.notified             = false;
    this.selectedProfileFile  = null;
    this.selectedProfileName  = null;
    this.selectedProfileProps = null;
    this.lastSelectedIdx      = -1;


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

    add( GUIFactory.createLabel( "Profile:" ), gbc );

    this.listModel = new DefaultListModel<>();
    this.list      = GUIFactory.createList( this.listModel );
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
	GUIFactory.createScrollPane(
		this.list,
		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ),
	gbc );

    this.fldProfileName = null;
    this.docProfileName = null;
    if( forSave ) {
      this.fldProfileName = GUIFactory.createTextField();
      gbc.fill            = GridBagConstraints.HORIZONTAL;
      gbc.insets.top      = 5;
      gbc.weighty         = 0.0;
      gbc.gridy++;
      add( this.fldProfileName, gbc );
      this.fldProfileName.setText( Main.DEFAULT_PROFILE );
    }
    int lastGridY = gbc.gridy;


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel(
			new GridLayout( forSave ? 3 : 5, 1, 5, 5 ) );

    this.btnOK = GUIFactory.createButton( approveLabel );
    panelBtn.add( this.btnOK );

    this.btnExport = null;
    this.btnImport = null;
    if( !forSave ) {
      this.btnExport = GUIFactory.createButton( "Exportieren..." );
      this.btnExport.setEnabled( false );
      panelBtn.add( this.btnExport );

      this.btnImport = GUIFactory.createButton( "Importieren..." );
      panelBtn.add( this.btnImport );
    }

    this.btnDelete = GUIFactory.createButton( EmuUtil.TEXT_DELETE );
    this.btnDelete.setEnabled( false );
    panelBtn.add( this.btnDelete );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );

    gbc.anchor     = GridBagConstraints.NORTHEAST;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.insets.top = 5;
    gbc.weightx    = 0.0;
    gbc.weighty    = 0.0;
    gbc.gridheight = 2;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelBtn, gbc );


    // CheckBox fuer Standardeinstellungen
    this.cbUseDefaults = null;
    if( !forSave ) {
      this.cbUseDefaults = GUIFactory.createCheckBox(
		"Kein Profil laden und Standardeinstellungen verwenden" );
      gbc.anchor        = GridBagConstraints.WEST;
      gbc.fill          = GridBagConstraints.NONE;
      gbc.insets.bottom = 10;
      gbc.gridwidth     = GridBagConstraints.REMAINDER;
      gbc.gridheight    = 1;
      gbc.gridx         = 0;
      gbc.gridy         = lastGridY + 1;
      add( this.cbUseDefaults, gbc );
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();


    // Aktivierung/Deaktivierung der Aktionsknoepfe ermoeglichen
    if( this.fldProfileName != null ) {
      this.docProfileName = this.fldProfileName.getDocument();
    }
    updOKButton();


    // Profile laden
    int idx = loadProfiles( preSelection );
    if( (this.fldProfileName != null)
	&& (idx >= 0) && (idx < this.listModel.getSize()) )
    {
      this.fldProfileName.setText( this.listModel.getElementAt( idx ) );
    }
  }


  public File getSelectedProfileFile()
  {
    return this.selectedProfileFile;
  }


  public String getSelectedProfileName()
  {
    return this.selectedProfileName;
  }


  public Properties getSelectedProfileProps()
  {
    return this.selectedProfileProps;
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
    if( e.getSource() == this.list )
      listSelectionChanged();
  }


	/* --- ueberschriebene Methoden fuer MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.list) && (e.getClickCount() > 1) ) {
      e.consume();
      if( this.fldProfileName == null ) {
	try {
	  doApply();
	}
	catch( IOException | UserInputException ex ) {
	  showErrorDlg( this, ex );
	}
      }
    } else {
      super.mouseClicked( e );
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
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      if( this.docProfileName != null ) {
	this.docProfileName.addDocumentListener( this );
      }
      if( this.fldProfileName != null ) {
	this.fldProfileName.addActionListener( this );
      }
      if( this.cbUseDefaults != null ) {
	this.cbUseDefaults.addActionListener( this );
      }
      this.list.addListSelectionListener( this );
      this.list.addKeyListener( this );
      this.list.addMouseListener( this );
      this.btnOK.addActionListener( this );
      if( this.btnImport != null ) {
	this.btnImport.addActionListener( this );
      }
      if( this.btnExport != null ) {
	this.btnExport.addActionListener( this );
      }
      this.btnDelete.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object  src = e.getSource();
      if( src != null ) {
	if( (src == this.btnOK)
	    || (src == this.list)
	    || (src == this.fldProfileName) )
	{
	  rv = true;
	  doApply();
	}
	else if( src == this.btnExport ) {
	  rv = true;
	  doExport();
	}
	else if( src == this.btnImport ) {
	  rv = true;
	  doImport();
	}
	else if( src == this.btnDelete ) {
	  rv = true;
	  doDelete();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
	else if( (src == this.cbUseDefaults)
		 && (this.cbUseDefaults != null) )
	{
	  rv = true;
	  useDefaultsChanged();
	}
      }
    }
    catch( IOException | UserInputException ex ) {
      showErrorDlg( this, ex );
    }
    return rv;
 }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      if( this.docProfileName != null ) {
	this.docProfileName.removeDocumentListener( this );
      }
      if( this.fldProfileName != null ) {
	this.fldProfileName.removeActionListener( this );
      }
      if( this.cbUseDefaults != null ) {
	this.cbUseDefaults.removeActionListener( this );
      }
      this.list.removeListSelectionListener( this );
      this.list.removeKeyListener( this );
      this.list.removeMouseListener( this );
      this.btnOK.removeActionListener( this );
      if( this.btnImport != null ) {
	this.btnImport.removeActionListener( this );
      }
      if( this.btnExport != null ) {
	this.btnExport.removeActionListener( this );
      }
      this.btnDelete.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
  }


	/* --- Aktionen --- */

  private void doApply() throws IOException, UserInputException
  {
    this.selectedProfileName  = null;
    this.selectedProfileFile  = null;
    this.selectedProfileProps = null;
    if( this.fldProfileName != null ) {
      // fuer Speichern
      this.selectedProfileName = this.fldProfileName.getText();
      this.selectedProfileFile = getProfileFile(
			checkName( this.selectedProfileName ) );
    } else {
      // fuer Laden/Anwenden
      if( this.cbUseDefaults != null ) {
	if( this.cbUseDefaults.isSelected() ) {
	  this.selectedProfileName  = Main.DEFAULT_PROFILE;
	  this.selectedProfileFile  = getProfileFile(
					this.selectedProfileName );
	  this.selectedProfileProps = new Properties();
	}
      }
      if( this.selectedProfileProps == null ) {
	String     pfName = getSelectedListProfileName();
	File       pfFile = getProfileFile( pfName );
	Properties pfProps = Main.loadProperties( pfFile );
	if( pfProps != null ) {
	  if( Main.checkAndConfirmProfileCompatibility( this, pfProps ) ) {
	    this.selectedProfileName  = pfName;
	    this.selectedProfileFile  = pfFile;
	    this.selectedProfileProps = pfProps;
	  }
	}
      }
    }
    if( this.selectedProfileFile != null ) {
      doClose();
    }
  }


  private void doDelete()
  {
    File file = getSelectedListProfileFile();
    if( file != null ) {
      if( showYesNoDlg(
		this,
		"M\u00F6chten Sie das Profil l\u00F6schen?" ) )
      {
	if( file.delete() ) {
	  loadProfiles( null );
	} else {
	  showErrorDlg(
		this,
		"Das Profil kann nicht gel\u00F6scht werden." );
	}
      }
    }
  }


  private void doExport() throws IOException
  {
    File srcFile = getSelectedListProfileFile();
    if( srcFile != null ) {
      File   initialFile = null;
      String fileName    = srcFile.getName();
      if( fileName != null ) {
	if( !fileName.isEmpty() ) {
	  File dirFile = Main.getLastDirFile( Main.FILE_GROUP_PROFILE );
	  if( dirFile != null ) {
	    initialFile = new File( dirFile, fileName );
	  } else {
	    initialFile = new File( fileName );
	  }
	}
      }
      File dstFile = FileUtil.showFileSaveDlg(
				this,
				"Profil exportieren",
				initialFile,
				FileUtil.getXMLFileFilter() );
      if( dstFile != null ) {
	try {
	  Files.copy(
		srcFile.toPath(),
		dstFile.toPath(),
		StandardCopyOption.REPLACE_EXISTING );
	  Main.setLastFile( dstFile, Main.FILE_GROUP_PROFILE );
	}
	catch( InvalidPathException ex ) {
	  throw new IOException( "Exportieren nicht m\u00FCglich" );
	}
      }
    }
  }


  private void doImport() throws IOException, UserInputException
  {
    File srcFile = FileUtil.showFileOpenDlg(
			this,
			"Profil importieren",
			Main.getLastDirFile( Main.FILE_GROUP_PROFILE ),
			FileUtil.getXMLFileFilter() );
    if( srcFile != null ) {
      Properties props = Main.loadProperties( srcFile );
      if( props != null ) {
	String text = srcFile.getName();
	if( text == null ) {
	  text = "";
	}
	if( TextUtil.startsWithIgnoreCase( text, PROFILE_PREFIX ) ) {
	  text = text.substring( PROFILE_PREFIX.length() );
	}
	if( TextUtil.endsWithIgnoreCase( text, PROFILE_SUFFIX ) ) {
	  text = text.substring( 0, text.length() - PROFILE_SUFFIX.length() );
	}
	for(;;) {
	  text = ReplyTextDlg.showDlg( this, "Profilname:", "Eingabe", text );
	  if( text == null ) {
	    break;
	  }
	  try {
	    boolean state   = true;
	    File    dstFile = getProfileFile( checkName( text ) );
	    if( dstFile.exists() ) {
	      state = showYesNoDlg(
			this,
			"Das Profil gibt es bereits.\n"
				+ "M\u00F6chten Sie es \u00FCberschreiben?" );
	    }
	    if( state ) {
	      Files.copy(
		  srcFile.toPath(),
		  getProfileFile( checkName( text ) ).toPath(),
		  StandardCopyOption.REPLACE_EXISTING );
	      Main.setLastFile( srcFile, Main.FILE_GROUP_PROFILE );
	      loadProfiles( dstFile );
	      break;
	    }
	  }
	  catch( InvalidPathException ex ) {
	    throw new IOException(
		  srcFile.getPath() + ":\nImportieren nicht m\u00FCglich" );
	  }
	  catch( UserInputException ex ) {
	    showErrorDlg( this, ex );
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static String checkName( String text ) throws UserInputException
  {
    if( text == null ) {
      text = "";
    }
    text = text.trim();
    int len = text.length();
    if( len < 1 ) {
      throw new UserInputException(
			"Sie m\u00FCssen einen Namen eingeben!" );
    }

    boolean status = false;
    char    ch     = text.charAt( 0 );
    if( (ch == '_')
	|| ((ch >= 'A') && (ch <= 'Z'))
	|| ((ch >= 'a') && (ch <= 'z')) )
    {
      status = true;
      for( int i = 1; i < len; i++ ) {
	ch = text.charAt( i );
	if( (ch != '_') && (ch != '+') && (ch != '-')
	    && (ch != '.') && (ch != '\u0020')
	    && ((ch < 'A') || (ch > 'Z'))
	    && ((ch < 'a') || (ch > 'z'))
	    && ((ch < '0') || (ch > '9')) )
	{
	  status = false;
	  break;
	}
      }
    }
    if( !status ) {
      throw new UserInputException(
		"Der Name enth\u00E4lt ung\u00FCltige"
			+ "Zeichen.\n"
			+ "Das erste Zeichen muss ein Buchstabe"
			+ " oder Unterstrich sein.\n"
			+ "Ab dem zweiten Zeichen sind zus\u00E4tzlich"
			+ " Ziffern, Punkt\n"
			+ "Plus, Minus und Leerzeichen erlaubt." );
    }
    return text;
  }


  private static File getProfileFile( String profileName )
  {
    String fileName = PROFILE_PREFIX + profileName + PROFILE_SUFFIX;
    File configDir = Main.getConfigDir();
    return configDir != null ?
		new File( configDir, fileName )
		: new File( fileName );
  }


  private File getSelectedListProfileFile()
  {
    String s = getSelectedListProfileName();
    return s != null ? getProfileFile( s ) : null;
  }


  private String getSelectedListProfileName()
  {
    String rv  = null;
    Object obj = this.list.getSelectedValue();
    if( obj != null ) {
      String s = obj.toString();
      if( s != null ) {
	if( !s.isEmpty() ) {
	  rv = s;
	}
      }
    }
    return rv;
  }


  private void listSelectionChanged()
  {
    boolean state = false;
    Object  obj   = this.list.getSelectedValue();
    if( this.cbUseDefaults != null ) {
      if( this.cbUseDefaults.isSelected() ) {
	obj = null;
      }
    }
    if( obj != null ) {
      if( this.fldProfileName != null ) {
	this.fldProfileName.setText( obj.toString() );
      }
      state = true;
    }
    this.btnDelete.setEnabled( state );
    if( this.btnExport != null ) {
      this.btnExport.setEnabled( state );
    }
    updOKButton();
  }


  private int loadProfiles( File fileToSelect )
  {
    this.listModel.clear();

    int  idxToSelect = -1;
    File configDir   = Main.getConfigDir();
    if( configDir != null ) {
      File[] entries = configDir.listFiles();
      if( entries != null ) {
	int prefixLen = PROFILE_PREFIX.length();
	int suffixLen = PROFILE_SUFFIX.length();
	Arrays.sort( entries );
	for( int i = 0; i < entries.length; i++ ) {
	  if( entries[ i ].isFile() ) {
	    String fileName = entries[ i ].getName();
	    if( fileName != null ) {
	      int len = fileName.length();
	      if( (len > (prefixLen + suffixLen) )
		  && fileName.startsWith( PROFILE_PREFIX )
		  && fileName.endsWith( PROFILE_SUFFIX ) )
	      {
		String itemText = fileName.substring(
					      prefixLen,
					      len - suffixLen );
		if( fileToSelect != null ) {
		  if( fileToSelect.equals( entries[ i ] ) ) {
		    idxToSelect = this.listModel.getSize();
		  }
		}
		this.listModel.addElement( itemText );
	      }
	    }
	  }
	}
      }
    }
    if( idxToSelect >= 0 ) {
      this.list.setSelectedIndex( idxToSelect );
    }
    return idxToSelect;
  }


  private void useDefaultsChanged()
  {
    if( this.cbUseDefaults != null ) {
      boolean state = !this.cbUseDefaults.isSelected();
      if( !state ) {
	this.lastSelectedIdx = this.list.getSelectedIndex();
	this.list.clearSelection();
      }
      this.list.setEnabled( state );
      if( state && (this.lastSelectedIdx >= 0) ) {
	this.list.setSelectedIndex( this.lastSelectedIdx );
      }
      if( this.fldProfileName != null ) {
	this.fldProfileName.setEnabled( state );
      }
      if( this.btnImport != null ) {
	this.btnImport.setEnabled( state );
      }
      listSelectionChanged();
    }
  }


  private void updOKButton()
  {
    boolean state = false;
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
      if( this.cbUseDefaults != null ) {
	state = this.cbUseDefaults.isSelected();
      }
      if( !state ) {
	state = (this.list.getSelectedIndex() >= 0);
      }
    }
    this.btnOK.setEnabled( state );
  }
}
