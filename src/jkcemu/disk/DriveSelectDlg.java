/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Auswahl eines Laufwerks
 */

package jkcemu.disk;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;
import jkcemu.Main;
import jkcemu.base.BaseDlg;


public class DriveSelectDlg extends BaseDlg
{
  private static final String[] unixDevFiles = {
					"/dev/floppy",
					"/dev/cdrom",
					"/dev/dvd",
					"/dev/fd0",
					"/dev/fd1",
					"/dev/sdb",
					"/dev/sdc",
					"/dev/sr0" };

  private class DriveItem
  {
    private String itemText;
    private String fileName;

    private DriveItem( String itemText, String fileName )
    {
      this.itemText = itemText;
      this.fileName = fileName;
    }

    @Override
    public String toString()
    {
      return this.itemText;
    }
  };


  private String            driveFileName;
  private JComboBox<Object> comboDrive;
  private JCheckBox btnReadOnly;
  private JButton   btnOK;
  private JButton   btnCancel;


  public static String selectDriveFileName( Window owner )
  {
    DriveSelectDlg dlg = new DriveSelectDlg( owner, false );
    dlg.setVisible( true );
    return dlg.getSelectedDriveFileName();
  }


  public DriveSelectDlg( Window owner, boolean askReadOnly )
  {
    super( owner, "Auswahl Laufwerk" );
    this.driveFileName = null;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    String lastDriveFileName = Main.getLastDriveFileName();
    this.comboDrive = new JComboBox<>();
    if( Main.isUnixLikeOS() ) {
      add( new JLabel( "Ger\u00E4tedatei:" ), gbc );
      this.comboDrive.setEditable( true );
      for( String s : unixDevFiles ) {
	if( (new File( s )).exists() ) {
	  this.comboDrive.addItem( s );
	}
      }
      if( lastDriveFileName != null ) {
	if( !lastDriveFileName.isEmpty() ) {
	  this.comboDrive.setSelectedItem( lastDriveFileName );
	}
      }
    } else {
      add( new JLabel( "Laufwerk:" ), gbc );
      this.comboDrive.setEditable( false );
      boolean        done      = false;
      int            presetIdx = -1;

      // vorhandene Laufwerke ermitteln
      FileSystemView fsv = FileSystemView.getFileSystemView();
      if( fsv != null ) {
	File[] roots = File.listRoots();
	if( roots != null ) {
	  int floppyIdx = -1;
	  for( File f : roots ) {
	    if( fsv.isDrive( f ) ) {
	      String drive = f.getPath();
	      if( drive != null ) {
		int len = drive.length();
		if( len >= 2 ) {
		  if( drive.charAt( 1 ) == ':' ) {
		    if( len > 2 ) {
		      drive = drive.substring( 0, 2 );
		    }
		    String text = fsv.getSystemDisplayName( f );
		    if( text != null ) {
		      if( text.isEmpty() ) {
			text = null;
		      }
		    }
		    if( text == null ) {
		      text = drive;
		    }
		    String fName = "\\\\.\\" + drive;
		    if( (presetIdx < 0) && (lastDriveFileName != null) ) {
		      if( fName.equals( lastDriveFileName ) ) {
			presetIdx = this.comboDrive.getItemCount();
		      }
		    }
		    if( fsv.isFloppyDrive( f ) ) {
		      floppyIdx = this.comboDrive.getItemCount();
		    }
		    this.comboDrive.addItem( new DriveItem( text, fName ) );
		    done = true;
		  }
		}
	      }
	    }
	  }
	  if( presetIdx < 0 ) {
	    if( floppyIdx >= 0 ) {
	      presetIdx = floppyIdx;
	    } else {
	      /*
	       * Wenn kein Laufwerk vorausgewaehlt und
	       * kein Diskettenlaufwerk erkannt wurde,
	       * soll moeglichst ein Laufwerk mit einem
	       * Wechselspeichermedium vorausgewaehlt werden.
	       * Mit einer gewissen Wahrscheinlichkeit wird ein
	       * Wechselspeicherlaufwerk hinter dem Systemlaufwerk liegen.
	       * Aus diesem Grund wird hier einfach das zweite Laufwerk
	       * vorausgewaehlt.
	       */
	      presetIdx = 1;
	    }
	  }
	}
      }
      if( !done ) {
	/*
	 * Es konnten keine physischen Laufwerke ermittelt werden.
	 * Aus diesem Grund werden alle moeglichen Laufwerksbuchstaben
	 * angeboten.
	 */
	for( char ch = 'A'; ch <= 'Z'; ch++ ) {
	  String fName = String.format( "\\\\.\\%c:", ch );
	  if( (presetIdx < 0) && (lastDriveFileName != null) ) {
	    if( fName.equals( lastDriveFileName ) ) {
	      presetIdx = this.comboDrive.getItemCount();
	    }
	  }
	}
      }
      if( (presetIdx >= 0)
	  && (presetIdx < this.comboDrive.getItemCount()) )
      {
	try {
	  this.comboDrive.setSelectedIndex( presetIdx );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    this.comboDrive.addKeyListener( this );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.comboDrive, gbc );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;

    if( askReadOnly ) {
      this.btnReadOnly = new JCheckBox( "Nur lesen", true );
      add( this.btnReadOnly, gbc );
      gbc.gridy++;
    } else {
      this.btnReadOnly = null;
    }

    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    pack();
    setParentCentered();
  }


  public String getSelectedDriveFileName()
  {
    return this.driveFileName;
  }


  public boolean isReadOnlySelected()
  {
    return this.btnReadOnly != null ? this.btnReadOnly.isSelected() : false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnOK) || (src == this.comboDrive) ) {
	Object obj = this.comboDrive.getSelectedItem();
	if( obj != null ) {
	  if( obj instanceof DriveItem ) {
	    this.driveFileName = ((DriveItem) obj).fileName;
	    doClose();
	  } else {
	    String s = obj.toString();
	    if( s != null ) {
	      if( !s.isEmpty() ) {
		this.driveFileName = s;
		doClose();
	      }
	    }
	  }
	}
        rv = true;
      }
      else if( src == this.btnCancel ) {
	doClose();
        rv = true;
      }
    }
    return rv;
  }


  /*
   * Focus auf OK-Knopf setzen,
   * damit man einfach Enter druecken kann.
   */
  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.btnOK != null) )
      this.btnOK.requestFocus();
  }
}

