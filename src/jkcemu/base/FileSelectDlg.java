/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateiauswahldialog
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.beans.*;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;


public class FileSelectDlg extends BasicDlg implements PropertyChangeListener
{
  private boolean      forSave;
  private boolean      startSelected;
  private boolean      loadWithOptionsSelected;
  private File         curFile;
  private File         selectedFile;
  private JLabel       labelStatus;
  private JButton      btnOpen;
  private JButton      btnStart;
  private JButton      btnLoadWithOptions;
  private JButton      btnCancel;
  private JFileChooser fileChooser;


  public FileSelectDlg(
		Window                                owner,
		boolean                               forSave,
		boolean                               startEnabled,
		boolean                               loadWithOptionsEnabled,
		String                                title,
		File                                  preSelection,
		javax.swing.filechooser.FileFilter... fileFilters )
  {
    super( owner, title );
    this.forSave                 = forSave;
    this.startSelected           = false;
    this.loadWithOptionsSelected = false;
    this.curFile                 = null;
    this.selectedFile            = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.WEST,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Dateiliste
    this.fileChooser = new JFileChooser();
    this.fileChooser.setControlButtonsAreShown( false );
    this.fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
    this.fileChooser.setMultiSelectionEnabled( false );
    add( this.fileChooser, gbc );

    if( fileFilters != null ) {
      if( fileFilters.length > 0 ) {
	javax.swing.filechooser.FileFilter fileFilter = null;
	int                                nFilters   = 0;
	for( int i = 0; i < fileFilters.length; i++ ) {
	  if( fileFilters[ i ] != null ) {
	    fileFilter = fileFilters[ i ];
	    this.fileChooser.addChoosableFileFilter( fileFilter );
	    nFilters++;
	  }
	}
	if( (fileFilter != null) && (nFilters != 1) ) {
	  fileFilter = this.fileChooser.getAcceptAllFileFilter();
	}
	if( fileFilter != null )
	  this.fileChooser.setFileFilter( fileFilter );
      }
    }


    // Statuszeile
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weighty   = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.labelStatus = new JLabel();
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Knoepfe
    int numBtns = 2;
    if( startEnabled ) {
      numBtns++;
    }
    if( loadWithOptionsEnabled ) {
      numBtns++;
    }

    JPanel panelBtn = new JPanel( new GridLayout( numBtns, 1, 5, 5 ) );

    String openBtnText = "OK";
    if( forSave ) {
      openBtnText = "Speichern";
    } else {
      if( loadWithOptionsEnabled ) {
	openBtnText = "Laden";
      }
    }
    this.btnOpen = new JButton( openBtnText );
    this.btnOpen.addActionListener( this );
    this.btnOpen.addKeyListener( this );
    panelBtn.add( this.btnOpen );

    this.btnStart = null;
    if( startEnabled ) {
      this.btnStart = new JButton( "Starten" );
      this.btnStart.addActionListener( this );
      this.btnStart.addKeyListener( this );
      this.btnStart.setEnabled( false );
      panelBtn.add( this.btnStart );
    }

    this.btnLoadWithOptions = null;
    if( loadWithOptionsEnabled ) {
      this.btnLoadWithOptions = new JButton( "Laden mit..." );
      this.btnLoadWithOptions.addActionListener( this );
      this.btnLoadWithOptions.addKeyListener( this );
      panelBtn.add( this.btnLoadWithOptions );
    }

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.anchor       = GridBagConstraints.NORTHEAST;
    gbc.fill         = GridBagConstraints.NONE;
    gbc.weightx      = 0.0;
    gbc.insets.left  = 0;
    gbc.insets.top   = 5;
    gbc.insets.right = 5;
    gbc.gridwidth    = 1;
    gbc.gridheight   = GridBagConstraints.REMAINDER;
    gbc.gridy        = 0;
    gbc.gridx++;
    add( panelBtn, gbc );


    // Vorbelegungen
    if( preSelection != null ) {
      if( preSelection.isDirectory() ) {
	this.fileChooser.setCurrentDirectory( preSelection );
      } else {
	File parentFile = preSelection.getParentFile();
	if( parentFile != null ) {
	  this.fileChooser.setCurrentDirectory( parentFile );
	}
	if( !preSelection.exists() || preSelection.isFile() ) {
	  this.fileChooser.setSelectedFile( preSelection );
	}
      }
    }
    this.fileChooser.addPropertyChangeListener( this );
    updFields();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


  public File getSelectedFile()
  {
    return this.selectedFile;
  }


  public boolean isLoadWithOptionsSelected()
  {
    return this.loadWithOptionsSelected;
  }


  public boolean isStartSelected()
  {
    return this.startSelected;
  }


	/* --- PropertyChangeListener --- */

  public void propertyChange( PropertyChangeEvent e )
  {
    if( e.getSource() == this.fileChooser ) {
      String prop = e.getPropertyName();
      if( prop != null ) {
	if( prop.equals( JFileChooser.DIRECTORY_CHANGED_PROPERTY ) ) {
	  this.curFile = null;
	  updFields();
	}
	else if( prop.equals( JFileChooser.SELECTED_FILE_CHANGED_PROPERTY ) ) {
	  this.curFile = null;
	  Object value = e.getNewValue();
	  if( value != null ) {
	    if( value instanceof File )
	      this.curFile = (File) value;
	  }
	  updFields();
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnOpen ) {
	  rv = true;
	  doOpenFile( false, false );
	}
	else if( src == this.btnStart ) {
	  rv = true;
	  doOpenFile( true, false );
	}
	else if( src == this.btnLoadWithOptions ) {
	  rv = true;
	  doOpenFile( false, true );
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.fileChooser.requestFocus();
  }


	/* --- private Methoden --- */

  private void doOpenFile(
			boolean startSelected,
			boolean loadWithOptionsSelected )
  {
    File file = this.fileChooser.getSelectedFile();
    if( file != null ) {
      if( this.forSave && file.exists() ) {
	if( !BasicDlg.showYesNoDlg(
			this,
			"Die Datei \'"
				+ file.getName()
				+ "\' existiert bereits.\n"
			 	+ "M\u00F6chten Sie die Datei"
				+ " \u00FCberschreiben?" ) )
	{
	  return;
	}
      }
      this.selectedFile            = file;
      this.startSelected           = startSelected;
      this.loadWithOptionsSelected = loadWithOptionsSelected;
      doClose();
    }
  }


  private void updFields()
  {
    boolean loadable   = false;
    boolean startable  = false;
    String  statusText = null;
    if( this.curFile != null ) {
      if( this.curFile.isFile() && this.curFile.canRead() ) {
	FileInfo fileInfo = FileInfo.analyzeFile( this.curFile );
	if( fileInfo != null ) {
	  if( !fileInfo.isKCBasicProgramFormat()
	      && (fileInfo.getBegAddr() >= 0) )
	  {
	    loadable = true;
	    if( fileInfo.getStartAddr() >= 0 )
	      startable = true;
	  }
	  statusText = fileInfo.getInfoText();
	}
      }
    }
    if( this.btnOpen != null ) {
      this.btnOpen.setEnabled( (!this.forSave && loadable)
				|| (this.btnLoadWithOptions == null) );
    }
    if( this.btnStart != null ) {
      this.btnStart.setEnabled( !this.forSave && startable );
    }
    if( this.btnLoadWithOptions != null ) {
      this.btnLoadWithOptions.setEnabled(
			!this.forSave && (this.curFile != null) );
    }
    if( statusText != null ) {
      if( statusText.length() < 1 )
	statusText = null;
    }
    this.labelStatus.setText( statusText != null ? statusText : "Bereit" );
  }
}

