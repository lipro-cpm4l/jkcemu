/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer einen AutoLoad-Eintrag
 */

package jkcemu.base;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.lang.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class AutoLoadEntryDlg extends BaseDlg
{
  private static final String LABEL_LOAD_ADDR  = "Ladeadresse (optional):";
  private static final String LABEL_WAIT_TIME  = "Wartezeit vor dem Laden:";

  private static int[] waitMillis = {
				0, 200, 500, 1000, 1500,
				2000, 3000, 4000, 5000,
				6000, 7000, 8000, 9000 };

  private static NumberFormat waitFmt = null;

  private boolean           checkLoadAddr;
  private AutoLoadEntry     appliedAutoLoadEntry;
  private FileNameFld       fldFile;
  private JComboBox<String> comboWaitSeconds;
  private JLabel            labelLoadAddr;
  private HexDocument       docLoadAddr;
  private JTextField        fldLoadAddr;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static AutoLoadEntry openNewEntryDlg(
				Window  owner,
				File    file,
				int     defaultMillisToWait,
				boolean checkLoadAddr )
  {
    AutoLoadEntryDlg dlg = new AutoLoadEntryDlg(
				owner,
				"Neuer AutoLoad-Eintrag",
				checkLoadAddr );
    dlg.fldFile.setFile( file );
    dlg.setMillisToWait( defaultMillisToWait );
    dlg.setVisible( true );
    return dlg.appliedAutoLoadEntry;
  }


  public static AutoLoadEntry openEditEntryDlg(
				Window        owner,
				AutoLoadEntry entry,
				boolean       checkLoadAddr )
  {
    AutoLoadEntryDlg dlg = new AutoLoadEntryDlg(
				owner,
				"AutoLoad-Eintrag bearbeiten",
				checkLoadAddr );
    dlg.fldFile.setFileName( entry.getFileName() );
    dlg.setMillisToWait( entry.getMillisToWait() );
    dlg.setLoadAddr( entry.getLoadAddr() );
    dlg.setVisible( true );
    return dlg.appliedAutoLoadEntry;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnOK ) {
	rv = true;
	doApply();
      } else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private AutoLoadEntryDlg(
			Window  owner,
			String  title,
			boolean checkLoadAddr )
  {
    super( owner, title );
    this.checkLoadAddr        = checkLoadAddr;
    this.appliedAutoLoadEntry = null;

    // Format fuer Wartezeit
    if( waitFmt == null ) {
      waitFmt = NumberFormat.getNumberInstance();
      if( waitFmt instanceof DecimalFormat ) {
	((DecimalFormat) waitFmt).applyPattern( "#0.0" );
      }
    }

    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( new JLabel( "Datei:" ), gbc );

    this.fldFile  = new FileNameFld();
    gbc.weightx   = 1.0;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.fldFile, gbc );

    gbc.weightx   = 0.0;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JLabel( LABEL_WAIT_TIME ), gbc );

    this.comboWaitSeconds = new JComboBox<>();
    for( int millis : waitMillis ) {
      this.comboWaitSeconds.addItem(
			waitFmt.format( (double) millis / 1000.0 ) );
    }
    Font font = this.fldFile.getFont();
    if( font != null ) {
      this.comboWaitSeconds.setFont( font );
    }
    this.comboWaitSeconds.setEditable( true );
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx++;
    add( this.comboWaitSeconds, gbc );

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx++;
    add( new JLabel( "Sekunden" ), gbc );

    gbc.gridx = 0;
    gbc.gridy++;
    add( new JLabel( LABEL_LOAD_ADDR ), gbc );

    this.docLoadAddr = new HexDocument( 4, LABEL_LOAD_ADDR );
    this.fldLoadAddr = new JTextField( this.docLoadAddr, "", 5 );
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.gridx++;
    add( this.fldLoadAddr, gbc );

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx++;
    add( new JLabel( "hex" ), gbc );

    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );

    // Listeners
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- Aktionen --- */

  private void doApply()
  {
    try {
      File file = this.fldFile.getFile();
      if( file != null ) {
	int  millis = 0;
	try {
	  Object o = this.comboWaitSeconds.getSelectedItem();
	  if( o != null ) {
	    String s = o.toString();
	    if( s != null ) {
	      Number value = waitFmt.parse( s );
	      if( value != null ) {
		millis = (int) Math.round( value.doubleValue() * 1000.0 );
	      }
	    }
	  }
	  boolean state     = true;
	  Integer loadAddr  = this.docLoadAddr.getInteger();
	  if( this.checkLoadAddr && (loadAddr == null) ) {
	    FileInfo fileInfo = FileInfo.analyzeFile( file );
	    if( fileInfo != null ) {
	      if( fileInfo.getBegAddr() < 0 ) {
		state = showYesNoWarningDlg(
				this,
				"Das automatiche Laden wird nicht"
					+ " funktionieren,\n"
					+ "da Sie keine Ladeadresse"
					+ " angegeben haben\n"
					+ "und in der Datei keine"
					+ " enthalten ist.\n"
					+ "\nM\u00F6chten Sie trotzdem"
					+ " fortsetzen?",
				 "Ladeadresse" );
	      }
	    }
	  }
	  if( state ) {
	    this.appliedAutoLoadEntry = new AutoLoadEntry(
							millis,
							file.getPath(),
							loadAddr );
	    doClose();
	  }
	}
	catch( ParseException ex ) {
	  throw new NumberFormatException(
			LABEL_WAIT_TIME + ": Ung\u00FCltiges Format" );
	}
      }
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, ex );
    }
  }


	/* --- private Methoden --- */

  private void setMillisToWait( int millis )
  {
    this.comboWaitSeconds.setSelectedItem(
			waitFmt.format( (double) millis / 1000.0 ) );
  }


  private void setLoadAddr( Integer addr )
  {
    if( addr != null ) {
      this.docLoadAddr.setValue( addr.intValue(), 4 );
    } else {
      this.fldLoadAddr.setText( "" );
    }
  }
}
