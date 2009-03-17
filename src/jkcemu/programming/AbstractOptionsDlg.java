/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Grundgeruest fuer einen Optionen-Dialog
 */

package jkcemu.programming;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.PrgOptions;
import jkcemu.system.*;


public abstract class AbstractOptionsDlg extends BasicDlg
{
  protected PrgOptions appliedOptions;
  protected EmuThread  emuThread;

  private static String[] fileFmtItems = {
				FileSaver.KCC,
				FileSaver.KCTAP_0,
				FileSaver.KCTAP_1,
				FileSaver.HEADERSAVE,
				FileSaver.INTELHEX,
				FileSaver.BIN };

  private Frame  owner;
  private String codeFileName;

  /*
   * Attribute sind nur gesetzt,
   * wenn die entsprechenden add...-Methoden aufgerufen werden
   */
  private JCheckBox       btnCodeToEmu;
  private JCheckBox       btnCodeToFile;
  private JComboBox       comboFileFmt;
  private JTextField      fldFileName;
  private JTextField      fldFileType;
  private JTextField      fldFileDesc;
  private JLabel          labelFileFmt;
  private JLabel          labelFileName;
  private JLabel          labelFileType;
  private JLabel          labelFileDesc;
  private JButton         btnFileSelect;
  private JButton         btnApply;
  private JButton         btnCancel;
  private LimitedDocument docFileDesc;


  public PrgOptions getAppliedOptions()
  {
    return this.appliedOptions;
  }


  protected AbstractOptionsDlg(
			Frame     owner,
			EmuThread emuThread,
			String    title )
  {
    super( owner, title );
    this.owner          = owner;
    this.emuThread      = emuThread;
    this.appliedOptions = null;
    this.codeFileName   = null;
  }


  protected JPanel createButtons( String applyText )
  {
    JPanel panel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnApply = new JButton( applyText );
    this.btnApply.addActionListener( this );
    this.btnApply.addKeyListener( this );
    panel.add( this.btnApply );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panel.add( this.btnCancel );

    return panel;
  }


  protected JPanel createCodeDestOptions()
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnCodeToEmu = new JCheckBox( "Programmcode in Emulator laden" );
    panel.add( this.btnCodeToEmu, gbc );

    this.btnCodeToFile = new JCheckBox( "Programmcode in Datei speichern" );
    this.btnCodeToFile.addActionListener( this );
    gbc.insets.bottom = 0;
    gbc.gridy++;
    panel.add( this.btnCodeToFile, gbc );

    this.labelFileFmt  = new JLabel( "Dateiformat:" );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    panel.add( this.labelFileFmt, gbc );

    this.comboFileFmt = new JComboBox();
    this.comboFileFmt.setEditable( false );
    for( int i = 0; i < fileFmtItems.length; i++ ) {
      this.comboFileFmt.addItem(
		FileSaver.getFormatText( fileFmtItems[ i ] ) );
    }
    this.comboFileFmt.addActionListener( this );
    gbc.insets.left = 5;
    gbc.gridwidth   = 3;
    gbc.gridx++;
    panel.add( this.comboFileFmt, gbc );

    this.labelFileName  = new JLabel( "Dateiname:" );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridx       = 0;
    gbc.gridy++;
    panel.add( this.labelFileName, gbc );

    this.fldFileName = new JTextField();
    this.fldFileName.setEditable( false );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 5;
    gbc.gridwidth   = 3;
    gbc.gridx++;
    panel.add( this.fldFileName, gbc );

    this.btnFileSelect = createImageButton(
				"/images/file/open.png",
				"\u00D6ffnen..." );
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx += 3;
    panel.add( this.btnFileSelect, gbc );

    this.labelFileType = new JLabel( "Dateityp:" );
    gbc.insets.left   = 50;
    gbc.insets.bottom = 5;
    gbc.gridx         = 0;
    gbc.gridy++;
    panel.add( this.labelFileType, gbc );

    this.fldFileType = new JTextField( new LimitedDocument( 1 ), "", 2 );
    gbc.insets.left = 5;
    gbc.gridx ++;
    panel.add( this.fldFileType, gbc );

    this.labelFileDesc = new JLabel( "Beschreibung:" );
    gbc.gridx++;
    panel.add( this.labelFileDesc, gbc );

    this.docFileDesc = new LimitedDocument( FileSaver.getMaxFileDescLength() );
    this.fldFileDesc = new JTextField( this.docFileDesc, "", 0 );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    panel.add( this.fldFileDesc, gbc );

    return panel;
  }


  protected void applyCodeDestOptionsTo( PrgOptions options )
						throws UserInputException
  {
    char fileType = '\u0020';
    String text = this.fldFileType.getText();
    if( text != null ) {
      if( text.length() > 0 ) {
	fileType = text.charAt( 0 );
	if( (fileType < '\u0020') || (fileType >= '\u007E') )
	  fileType = '\u0020';
      }
    }
    FileSaver.checkFileDesc(
			getSelectedFileFmt(),
			false,
			this.fldFileDesc.getText() );
    options.setCodeToFile(
			this.btnCodeToFile.isSelected(),
			this.codeFileName,
			getSelectedFileFmt(),
			fileType,
			this.fldFileDesc.getText() );
    options.setCodeToEmu( this.btnCodeToEmu.isSelected() );
  }


  protected abstract void doApply();


  protected void updCodeDestFields( PrgOptions options )
  {
    if( options != null ) {
      this.btnCodeToEmu.setSelected( options.getCodeToEmu() );
      this.btnCodeToFile.setSelected( options.getCodeToFile() );
      this.codeFileName = options.getCodeFileName();
      updFileNameField();

      String fileFmt = options.getCodeFileFormat();
      if( fileFmt != null ) {
	String text = FileSaver.getFormatText( fileFmt );
	if( text != null )
	  this.comboFileFmt.setSelectedItem( text );
      }
      char fileType = options.getCodeFileType();
      if( (fileType > '\u0020') && (fileType <= '\u007E') ) {
	this.fldFileType.setText( String.valueOf( fileType ) );
      }
      String fileDesc = options.getCodeFileDesc();
      if( fileDesc != null ) {
        this.fldFileDesc.setText( fileDesc );
      }
    } else {
      this.btnCodeToEmu.setSelected( false );
      this.btnCodeToFile.setSelected( false );

      EmuSys emuSys = this.emuThread.getEmuSys();
      if( (emuSys instanceof AC1) || (emuSys instanceof Z1013) ) {
	this.comboFileFmt.setSelectedItem(
			FileSaver.getFormatText( FileSaver.HEADERSAVE ) );
      } else {
	this.comboFileFmt.setSelectedItem(
			FileSaver.getFormatText( FileSaver.KCC ) );
      }
      this.fldFileType.setText( "C" );
    }
    updCodeDestFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnCodeToFile) || (src == this.comboFileFmt) ) {
        rv = true;
        updCodeDestFieldsEnabled();
      }
      else if( (src == this.fldFileName) || (src == this.btnFileSelect) ) {
        rv = true;
        doSelectFile();
      }
      else if( src == this.fldFileType ) {
        rv = true;
        this.fldFileDesc.requestFocus();
      }
      else if( (src == this.fldFileDesc) || (src == this.btnApply) ) {
        rv = true;
        doApply();
      }
      else if( src == this.btnCancel ) {
        rv = true;
        doClose();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doSelectFile()
  {
    File file = null;
    if( this.codeFileName != null ) {
      if( this.codeFileName.length() > 0 )
	file = new File( this.codeFileName );
    }
    file = EmuUtil.showFileSaveDlg(
				this.owner,
				"Programmcode speichern",
				file,
				EmuUtil.getKCSystemFileFilter(),
				EmuUtil.getTapFileFilter(),
				EmuUtil.getHeadersaveFileFilter(),
				EmuUtil.getHexFileFilter(),
				EmuUtil.getBinaryFileFilter() );
    if( file != null ) {
      this.codeFileName = file.getPath();
      updFileNameField();
      this.fldFileType.requestFocus();
    }
  }


  private String getSelectedFileFmt()
  {
    int idx = this.comboFileFmt.getSelectedIndex();
    return (idx >= 0) && (idx < fileFmtItems.length) ?
					fileFmtItems[ idx ]
					: FileSaver.BIN;
  }


  private void updCodeDestFieldsEnabled()
  {
    boolean state = this.btnCodeToFile.isSelected();
    this.labelFileFmt.setEnabled( state );
    this.comboFileFmt.setEnabled( state );
    this.labelFileName.setEnabled( state );
    this.fldFileName.setEnabled( state );
    this.btnFileSelect.setEnabled( state );
    if( state ) {
      String fileFmt = getSelectedFileFmt();

      state = fileFmt.equals( FileSaver.HEADERSAVE );
      this.labelFileType.setEnabled( state );
      this.fldFileType.setEnabled( state );

      state = state
		|| fileFmt.equals( FileSaver.KCC )
		|| fileFmt.equals( FileSaver.KCTAP_0 )
		|| fileFmt.equals( FileSaver.KCTAP_1 );
      this.labelFileDesc.setEnabled( state );
      this.fldFileDesc.setEnabled( state );
      this.docFileDesc.setMaxLength(
		FileSaver.getMaxFileDescLength( fileFmt, false ) );
    } else {
      this.labelFileType.setEnabled( state );
      this.fldFileType.setEnabled( state );
      this.labelFileDesc.setEnabled( state );
      this.fldFileDesc.setEnabled( state );
    }
  }


  private void updFileNameField()
  {
    if( this.codeFileName != null ) {
      if( this.codeFileName.length() > 0 ) {
	int    n  = 0;
	String p1 = null;
	String p2 = null;
	String s  = this.codeFileName;

	do {
	  s = (new File( s )).getParent();
	  if( s == null ) {
	    break;
	  }
	  if( s.length() < 1 ) {
	    break;
	  }
	  p1 = p2;
	  p2 = s;
	  n++;
	} while( s != null );

	if( (n > 2) && (p1 != null) ) {
	  StringBuilder buf = new StringBuilder();
	  buf.append( p1 );
	  if( !p1.endsWith( File.separator ) ) {
	    buf.append( File.separator );
	  }
	  buf.append( "..." );
	  buf.append( File.separator );
	  buf.append( (new File( this.codeFileName )).getName() );

	  this.fldFileName.setText( buf.toString() );
	} else {
	  this.fldFileName.setText( this.codeFileName );
	}
      } else {
	this.fldFileName.setText( "" );
      }
    } else {
      this.fldFileName.setText( "" );
    }
  }
}

