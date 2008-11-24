/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hex-Editor
 */

package jkcemu.tools.hexeditor;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.naming.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;


public class HexEditFrm extends BasicFrm implements
						AdjustmentListener,
						ComponentListener,
						DropTargetListener,
						MouseMotionListener,
						MouseWheelListener
{
  private static final int BUF_EXTEND = 0x2000;

  private ScreenFrm                 screenFrm;
  private ReplyBytesDlg.InputFormat lastInputFmt;
  private boolean                   lastBigEndian;
  private String                    lastFindText;
  private int                       findPos;
  private byte[]                    findBytes;
  private byte[]                    fileBytes;
  private int                       fileLen;
  private File                      file;
  private boolean                   asciiSelected;
  private boolean                   dataChanged;
  private JMenuItem                 mnuNew;
  private JMenuItem                 mnuOpen;
  private JMenuItem                 mnuSave;
  private JMenuItem                 mnuSaveAs;
  private JMenuItem                 mnuClose;
  private JMenuItem                 mnuAppend;
  private JMenuItem                 mnuInsert;
  private JMenuItem                 mnuOverwrite;
  private JMenuItem                 mnuRemove;
  private JMenuItem                 mnuFind;
  private JMenuItem                 mnuFindNext;
  private JMenuItem                 mnuHelpContent;
  private JButton                   btnNew;
  private JButton                   btnOpen;
  private JButton                   btnSave;
  private JButton                   btnFind;
  private HexCharFld                hexCharFld;
  private JScrollBar                hScrollBar;
  private JScrollBar                vScrollBar;
  private JTextField                fldCaretDec;
  private JTextField                fldCaretHex;
  private JTextField                fldValue8;
  private JTextField                fldValue16;
  private JTextField                fldValue32;
  private JLabel                    labelValue8;
  private JLabel                    labelValue16;
  private JLabel                    labelValue32;
  private JCheckBox                 btnValueSigned;
  private JCheckBox                 btnLittleEndian;


  public HexEditFrm( ScreenFrm screenFrm )
  {
    this.screenFrm     = screenFrm;
    this.lastInputFmt  = null;
    this.lastBigEndian = false;
    this.lastFindText  = null;
    this.findPos       = 0;
    this.findBytes     = null;
    this.fileLen       = 0;
    this.fileBytes     = new byte[ 0x100 ];
    this.file          = null;
    this.asciiSelected = false;
    this.dataChanged   = false;
    Main.updIcon( this );
    updTitle();


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuNew = createJMenuItem( "Neu" );
    mnuFile.add( this.mnuNew );

    this.mnuOpen = createJMenuItem( "\u00D6ffnen..." );
    mnuFile.add( this.mnuOpen );
    mnuFile.addSeparator();

    this.mnuSave = createJMenuItem(
		"Speichern",
		KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.CTRL_MASK ) );
    this.mnuSave.setEnabled( false );
    mnuFile.add( this.mnuSave );

    this.mnuSaveAs = createJMenuItem( "Speichern unter..." );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuInsert = createJMenuItem(
		"Bytes einf\u00FCgen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_I, Event.CTRL_MASK ) );
    this.mnuInsert.setEnabled( false );
    mnuEdit.add( this.mnuInsert );

    this.mnuOverwrite = createJMenuItem(
		"Bytes \u00FCberschreiben...",
		KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK ) );
    this.mnuOverwrite.setEnabled( false );
    mnuEdit.add( this.mnuOverwrite );

    this.mnuAppend = createJMenuItem(
		"Bytes am Ende anh\u00E4ngen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_E, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuAppend );
    mnuEdit.addSeparator();

    this.mnuRemove = createJMenuItem(
		"Bytes entfernen",
		KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    this.mnuRemove.setEnabled( false );
    mnuEdit.add( this.mnuRemove );
    mnuEdit.addSeparator();

    this.mnuFind = createJMenuItem(
		"Suchen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_F, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke(
			KeyEvent.VK_F,
			Event.CTRL_MASK | Event.SHIFT_MASK ) );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    this.btnNew = createImageButton( "/images/file/new.png", "Neu" );
    toolBar.add( this.btnNew );

    this.btnOpen = createImageButton( "/images/file/open.png", "\u00D6ffnen" );
    toolBar.add( this.btnOpen );

    this.btnSave = createImageButton( "/images/file/save.png", "Speichern" );
    this.btnSave.setEnabled( false );
    toolBar.add( this.btnSave );
    toolBar.addSeparator();

    this.btnFind = createImageButton( "/images/edit/find.png", "Suchen" );
    toolBar.add( this.btnFind );


    // Dateianzeige
    JPanel panelDetails = new JPanel( new GridBagLayout() );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( panelDetails, gbc );

    GridBagConstraints gbcDetails = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.BOTH,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    this.hexCharFld = new HexCharFld();
    this.hexCharFld.setBorder( BorderFactory.createEtchedBorder() );
    this.hexCharFld.addKeyListener( this );
    panelDetails.add( this.hexCharFld, gbcDetails );

    this.vScrollBar = new JScrollBar( JScrollBar.VERTICAL );
    this.vScrollBar.setMinimum( 0 );
    this.vScrollBar.setUnitIncrement( this.hexCharFld.getRowHeight() );
    this.vScrollBar.addAdjustmentListener( this );
    this.vScrollBar.addComponentListener( this );
    gbcDetails.anchor  = GridBagConstraints.WEST;
    gbcDetails.fill    = GridBagConstraints.VERTICAL;
    gbcDetails.weightx = 0.0;
    gbcDetails.gridx++;
    panelDetails.add( this.vScrollBar, gbcDetails );

    this.hScrollBar = new JScrollBar( JScrollBar.HORIZONTAL );
    this.hScrollBar.setMinimum( 0 );
    this.hScrollBar.setUnitIncrement( this.hexCharFld.getCharWidth() );
    this.hScrollBar.addAdjustmentListener( this );
    this.hScrollBar.addComponentListener( this );
    gbcDetails.anchor  = GridBagConstraints.NORTH;
    gbcDetails.fill    = GridBagConstraints.HORIZONTAL;
    gbcDetails.weightx = 1.0;
    gbcDetails.weighty = 0.0;
    gbcDetails.gridx   = 0;
    gbcDetails.gridy++;
    panelDetails.add( this.hScrollBar, gbcDetails );


    // Anzeige der Cursor-Position
    JPanel panelPos = new JPanel( new GridBagLayout() );
    panelPos.setBorder( BorderFactory.createTitledBorder(
						"Cursor-Position" ) );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( panelPos, gbc );

    GridBagConstraints gbcPos = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panelPos.add( new JLabel( "Hexadezimal:" ), gbcPos );

    this.fldCaretHex = new JTextField();
    this.fldCaretHex.addActionListener( this );
    gbcPos.fill    = GridBagConstraints.HORIZONTAL;
    gbcPos.weightx = 0.5;
    gbcPos.gridx++;
    panelPos.add( this.fldCaretHex, gbcPos );

    gbcPos.fill    = GridBagConstraints.NONE;
    gbcPos.weightx = 0.0;
    gbcPos.gridx++;
    panelPos.add( new JLabel( "Dezimal:" ), gbcPos );

    this.fldCaretDec = new JTextField();
    this.fldCaretDec.addActionListener( this );
    gbcPos.fill    = GridBagConstraints.HORIZONTAL;
    gbcPos.weightx = 0.5;
    gbcPos.gridx++;
    panelPos.add( this.fldCaretDec, gbcPos );


    // Anzeige der Dezimalwerte der Bytes ab Cursor-Position
    JPanel panelValue = new JPanel( new GridBagLayout() );
    panelValue.setBorder( BorderFactory.createTitledBorder(
			"Dezimalwerte der Bytes ab Cursor-Position" ) );
    gbc.gridy++;
    add( panelValue, gbc );

    GridBagConstraints gbcValue = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    this.labelValue8 = new JLabel( "8 Bit:" );
    this.labelValue8.setEnabled( false );
    panelValue.add( this.labelValue8, gbcValue );

    this.fldValue8 = new JTextField();
    this.fldValue8.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panelValue.add( this.fldValue8, gbcValue );

    this.labelValue16 = new JLabel( "16 Bit:" );
    this.labelValue16.setEnabled( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.labelValue16, gbcValue );

    this.fldValue16 = new JTextField();
    this.fldValue16.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panelValue.add( this.fldValue16, gbcValue );

    this.labelValue32 = new JLabel( "32 Bit:" );
    this.labelValue32.setEnabled( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.labelValue32, gbcValue );

    this.fldValue32 = new JTextField();
    this.fldValue32.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panelValue.add( this.fldValue32, gbcValue );

    JPanel panelValueOpt = new JPanel(
			new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbcValue.weightx   = 1.0;
    gbcValue.gridwidth = GridBagConstraints.REMAINDER;
    gbcValue.gridx     = 0;
    gbcValue.gridy++;
    panelValue.add( panelValueOpt, gbcValue );

    this.btnValueSigned = new JCheckBox( "Vorzeichenbehaftet", true );
    this.btnValueSigned.addActionListener( this );
    this.btnValueSigned.setEnabled( false );
    panelValueOpt.add( this.btnValueSigned, gbcValue );

    this.btnLittleEndian = new JCheckBox( "Little Endian", true );
    this.btnLittleEndian.addActionListener( this );
    this.btnLittleEndian.setEnabled( false );
    panelValueOpt.add( this.btnLittleEndian, gbcValue );


    // Listener
    this.hexCharFld.addMouseListener( this );
    this.hexCharFld.addMouseMotionListener( this );
    this.hexCharFld.addMouseWheelListener( this );


    // Drag&Drop aktivieren
    (new DropTarget( this.hexCharFld, this )).setActive( true );


    // sonstiges
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );
  }


  public void openFile( File file )
  {
    if( file != null ) {
      if( confirmDataSaved() )
	openFileInternal( file );
    }
  }


	/* --- AdjustmentListener --- */

  public void adjustmentValueChanged( AdjustmentEvent e )
  {
    if( e.getSource() == this.vScrollBar ) {
      int yOffset  = 0;
      int yOffsMax = this.hexCharFld.getContentHeight()
				- this.hexCharFld.getHeight();
      if( yOffsMax > 0 ) {
	int maxValue = this.vScrollBar.getMaximum()
				- this.vScrollBar.getVisibleAmount();
	if( maxValue > 0 ) {
	  yOffset = (int) Math.round( (double) this.vScrollBar.getValue()
						/ (double) maxValue
						* (double) yOffsMax );
	}
      }
      this.hexCharFld.setYOffset( yOffset );
    }
    else if( e.getSource() == this.hScrollBar ) {
      int xOffset  = 0;
      int xOffsMax = this.hexCharFld.getContentWidth()
				- this.hexCharFld.getWidth();
      if( xOffsMax > 0 ) {
	int maxValue = this.hScrollBar.getMaximum()
				- this.hScrollBar.getVisibleAmount();
	if( maxValue > 0 ) {
	  xOffset = (int) Math.round( (double) this.hScrollBar.getValue()
						/ (double) maxValue
						* (double) xOffsMax );
	}
      }
      this.hexCharFld.setXOffset( xOffset );
    }
  }


	/* --- ComponentListener --- */

  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  public void componentResized( ComponentEvent e )
  {
    updScrollBar( e.getComponent() );
  }


  public void componentShown( ComponentEvent e )
  {
    updScrollBar( e.getComponent() );
  }


	/* --- DropTargetListener --- */

  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  public void dragExit( DropTargetEvent e )
  {
    // empty
  }


  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  public void drop( DropTargetDropEvent e )
  {
    if( EmuUtil.isFileDrop( e ) ) {
      e.acceptDrop( DnDConstants.ACTION_COPY );    // Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
	try {
	  Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( (o != null) && (o instanceof Collection) ) {
	    Iterator iter = ((Collection) o).iterator();
	    if( iter.hasNext() ) {
	      o = iter.next();
	      if( o instanceof File ) {
		openFile( (File) o );
	      } else {
		String s = o.toString();
		if( s != null )
		  openFile( new File( s ) );
	      }
	    }
	  }
	}
	catch( Exception ex ) {}
      }
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- MouseMotionListener --- */

  public void mouseDragged( MouseEvent e )
  {
    if( (this.fileLen > 0) && (e.getComponent() == this.hexCharFld) ) {
      int pos = this.hexCharFld.getDataIndexAt( e.getX(), e.getY() );
      if( pos >= 0 ) {
	setCaretPosition( pos, true );
      }
      e.consume();
    }
  }


  public void mouseMoved( MouseEvent e )
  {
    // leer
  }


	/* --- MouseWheelListener --- */

  public void mouseWheelMoved( MouseWheelEvent e )
  {
    if( e.getComponent() == this.hexCharFld ) {
      int diffValue = 0;
      switch( e.getScrollType() ) {
	case MouseWheelEvent.WHEEL_UNIT_SCROLL:
	  diffValue = this.vScrollBar.getUnitIncrement();
	  break;

	case MouseWheelEvent.WHEEL_BLOCK_SCROLL:
	  diffValue = this.vScrollBar.getBlockIncrement();
	  break;
      }
      if( diffValue > 0 ) {
	diffValue *= e.getWheelRotation();
      }
      changeVScrollBar( diffValue );
      e.consume();
    }
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnNew) || (src == this.mnuNew) ) {
	rv = true;
	if( confirmDataSaved() ) {
	  newFile();
	}
      }
      else if( (src == this.btnOpen) || (src == this.mnuOpen) ) {
	rv = true;
	doOpen();
      }
      else if( (src == this.btnSave) || (src == this.mnuSave) ) {
	rv = true;
	doSave( false );
      }
      else if( src == this.mnuSaveAs ) {
	rv = true;
	doSave( true );
      }
      else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuAppend ) {
	rv = true;
	doBytesAppend();
      }
      else if( src == this.mnuInsert ) {
	rv = true;
	doBytesInsert();
      }
      else if( src == this.mnuOverwrite ) {
	rv = true;
	doBytesOverwrite();
      }
      else if( src == this.mnuRemove ) {
	rv = true;
	doBytesRemove();
      }
      else if( (src == this.btnFind) || (src == this.mnuFind) ) {
	rv = true;
	doFind();
      }
      else if( src == this.mnuFindNext ) {
	rv = true;
	doFindNext();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/tools/hexeditor.htm" );
      }
      else if( src == this.fldCaretHex ) {
	rv = true;
	setCaretPosition( this.fldCaretHex, 16 );
      }
      else if( src == this.fldCaretDec ) {
	rv = true;
	setCaretPosition( this.fldCaretDec, 10 );
      }
      else if( (src == this.btnValueSigned)
	       || (src == this.btnLittleEndian) )
      {
	rv = true;
	updValueFields();
      }
    }
    return rv;
  }


  public boolean doClose()
  {
    boolean rv = confirmDataSaved();
    if( rv ) {
      rv = super.doClose();
    }
    if( rv ) {
      newFile();	// damit beim erneuten Oeffnen der Editor leer ist
    }
    return rv;
  }


  public void keyPressed( KeyEvent e )
  {
    if( e.getSource() == this.hexCharFld ) {
      int hRow     = 0;
      int caretPos = this.hexCharFld.getCaretPosition();
      if( caretPos >= 0 ) {
	switch( e.getKeyCode() ) {
	  case KeyEvent.VK_LEFT:
	    --caretPos;
	    e.consume();
	    break;

	  case KeyEvent.VK_RIGHT:
	    caretPos++;
	    e.consume();
	    break;

	  case KeyEvent.VK_UP:
	    caretPos -= HexCharFld.BYTES_PER_ROW;
	    e.consume();
	    break;

	  case KeyEvent.VK_DOWN:
	    caretPos += HexCharFld.BYTES_PER_ROW;
	    e.consume();
	    break;

	  case KeyEvent.VK_PAGE_UP:
	    hRow = this.hexCharFld.getRowHeight();
	    if( hRow > 0 ) {
	      caretPos -= ((this.hexCharFld.getHeight() / hRow)
						* HexCharFld.BYTES_PER_ROW);
	      while( caretPos < 0 ) {
		caretPos += HexCharFld.BYTES_PER_ROW;
	      }
	    }
	    e.consume();
	    break;

	  case KeyEvent.VK_PAGE_DOWN:
	    hRow = this.hexCharFld.getRowHeight();
	    if( hRow > 0 ) {
	      caretPos += ((this.hexCharFld.getHeight() / hRow)
						* HexCharFld.BYTES_PER_ROW);
	      while( caretPos >= this.fileLen ) {
		caretPos += HexCharFld.BYTES_PER_ROW;
	      }
	    }
	    e.consume();
	    break;

	  case KeyEvent.VK_BEGIN:
	    caretPos = 0;
	    e.consume();
	    break;

	  case KeyEvent.VK_END:
	    caretPos = this.fileLen - 1;
	    e.consume();
	    break;
	}
	if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
	  setCaretPosition( caretPos, e.isShiftDown() );
	}
      }
    }
  }


  public void lookAndFeelChanged()
  {
    this.hexCharFld.repaint();
  }


  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this.hexCharFld ) {
      if( this.fileLen > 0 ) {
	int pos = this.hexCharFld.getDataIndexAt( e.getX(), e.getY() );
	if( pos >= 0 ) {
	  setCaretPosition( pos, e.isShiftDown() );
	}
      }
      this.hexCharFld.requestFocus();
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


	/* --- Aktionen --- */

  private void doBytesAppend()
  {
    ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes anh\u00E4ngen",
					this.lastInputFmt,
					this.lastBigEndian,
					null );
    dlg.setVisible( true );
    byte[] a = dlg.getApprovedBytes();
    if( a != null ) {
      if( a.length > 0 ) {
	this.lastInputFmt  = dlg.getApprovedInputFormat();
	this.lastBigEndian = dlg.getApprovedBigEndian();
	try {
	  insertBytes( this.fileLen, a, 0 );
	}
	catch( SizeLimitExceededException ex ) {
	  BasicDlg.showErrorDlg( this, ex.getMessage() );
	}
	setDataChanged( true );
	updView();
	setCaretPosition( this.fileLen - 1, true );
      }
    }
  }


  private void doBytesInsert()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes einf\u00FCgen",
					this.lastInputFmt,
					this.lastBigEndian,
					null );
      dlg.setVisible( true );
      byte[] a = dlg.getApprovedBytes();
      if( a != null ) {
	if( a.length > 0 ) {
	  this.lastInputFmt  = dlg.getApprovedInputFormat();
	  this.lastBigEndian = dlg.getApprovedBigEndian();
	  try {
	    insertBytes( caretPos, a, 0 );
	  }
	  catch( SizeLimitExceededException ex ) {
	    BasicDlg.showErrorDlg( this, ex.getMessage() );
	  }
	  setDataChanged( true );
	  updView();
	  setCaretPosition( caretPos + a.length, true );
	}
      }
    }
  }


  private void doBytesOverwrite()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes \u00FCberschreiben",
					this.lastInputFmt,
					this.lastBigEndian,
					null );
      dlg.setVisible( true );
      byte[] a = dlg.getApprovedBytes();
      if( a != null ) {
	if( a.length > 0 ) {
	  this.lastInputFmt  = dlg.getApprovedInputFormat();
	  this.lastBigEndian = dlg.getApprovedBigEndian();
	  try {
	    int src = 0;
	    int dst = caretPos;
	    while( (src < a.length) && (dst < this.fileLen) ) {
	      this.fileBytes[ dst++ ] = a[ src++ ];
	    }
	    if( src < a.length ) {
	      insertBytes( dst, a, src );
	    }
	  }
	  catch( SizeLimitExceededException ex ) {
	    BasicDlg.showErrorDlg( this, ex.getMessage() );
	  }
	  setDataChanged( true );
	  updView();
	  setCaretPosition( caretPos + a.length, true );
	}
      }
    }
  }


  private void doBytesRemove()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= this.fileLen ) {
      m2 = this.fileLen - 1;
    }
    if( m1 >= 0 ) {
      String msg = null;
      if( m2 > m1 ) {
	msg = String.format(
		"M\u00F6chten Sie die %d markierten Bytes entfernen?",
		m2 - m1 + 1);
      }
      else if( m2 == m1 ) {
	msg = String.format(
		"M\u00F6chten das markierte Byte mit dem hexadezimalen"
			+ " Wert %02X entfernen?",
		this.fileBytes[ m1 ] );
      }
      if( msg != null ) {
	if( BasicDlg.showYesNoDlg( this, msg ) ) {
	  if( m2 + 1 < this.fileLen ) {
	    m2++;
	    while( m2 < this.fileLen ) {
	      this.fileBytes[ m1++ ] = this.fileBytes[ m2++ ];
	    }
	  }
	  this.fileLen = m1;
	  setDataChanged( true );
	  updView();
	  setCaretPosition( m1, false );
	}
      }
    }
  }


  private void doFind()
  {
    ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes suchen",
					this.lastInputFmt,
					this.lastBigEndian,
					this.lastFindText );
    dlg.setVisible( true );
    byte[] a = dlg.getApprovedBytes();
    if( a != null ) {
      if( a.length > 0 ) {
	this.lastInputFmt  = dlg.getApprovedInputFormat();
	this.lastBigEndian = dlg.getApprovedBigEndian();
	this.lastFindText  = dlg.getApprovedText();
	this.findBytes     = a;
	this.findPos       = 0;
	int caretPos       = this.hexCharFld.getCaretPosition();
	if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
	  this.findPos = caretPos;
	}
	doFindNext();
	this.mnuFindNext.setEnabled( true );
      }
    }
  }


  private void doFindNext()
  {
    if( this.findBytes != null ) {
      if( this.findBytes.length > 0 ) {
	int foundAt = -1;
	if( this.findPos < 0 ) {
	  this.findPos = 0;
	}
	for( int i = this.findPos; i < this.fileLen; i++ ) {
	  boolean found = true;
	  for( int k = 0; k < this.findBytes.length; k++ ) {
	    int idx = i + k;
	    if( idx < this.fileLen ) {
	      if( this.fileBytes[ idx ] != this.findBytes[ k ] ) {
		found = false;
		break;
	      }
	    } else {
	      found = false;
	      break;
	    }
	  }
	  if( found ) {
	    foundAt = i;
	    break;
	  }
	}
	if( foundAt >= 0 ) {
	  this.hexCharFld.setSelection(
				foundAt + this.findBytes.length - 1,
				foundAt );
	  this.findPos = foundAt + 1;
	  updCaretPosFields();
	} else {
	  if( this.findPos > 0 ) {
	    this.findPos = 0;
	    doFindNext();
	  } else {
	    BasicDlg.showInfoDlg( this, "Byte-Folge nicht gefunden" );
	  }
	}
      }
    }
  }


  private void doOpen()
  {
    if( confirmDataSaved() ) {
      File file = EmuUtil.showFileOpenDlg(
				this,
				"Datei \u00F6ffnen",
				Main.getLastPathFile( "software" ) );
      if( file != null )
	openFileInternal( file );
    }
  }


  private boolean doSave( boolean forceFileDlg )
  {
    boolean rv   = false;
    File    file = this.file;
    if( forceFileDlg || (file == null) ) {
      file = EmuUtil.showFileSaveDlg(
		this,
		"Datei speichern",
		file != null ? file : Main.getLastPathFile( "software" ) );
    }
    if( file != null ) {
      try {
	OutputStream out = null;
	try {
	  out = new FileOutputStream( file );
	  if( (this.fileLen > 0) && (this.fileBytes.length > 0) ) {
	    out.write(
		this.fileBytes,
		0,
		Math.min( this.fileLen, this.fileBytes.length) );
	  }
	  out.close();
	  out       = null;
	  this.file = file;
	  rv        = true;
	  if( !setDataChanged( false ) ) {
	    updTitle();
	  }
	}
	finally {
	  EmuUtil.doClose( out );
	}
	Main.setLastFile( file, "software" );
      }
      catch( Exception ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void changeVScrollBar( int diffValue )
  {
    if( diffValue != 0 ) {
      int oldValue = this.vScrollBar.getValue();
      int newValue = oldValue + diffValue;
      if( newValue < this.vScrollBar.getMinimum() ) {
	newValue = this.vScrollBar.getMinimum();
      }
      else if( newValue > this.vScrollBar.getMaximum() ) {
	newValue = this.vScrollBar.getMaximum();
      }
      if( newValue != oldValue ) {
	this.vScrollBar.setValue( newValue );
      }
    }
  }


  private boolean confirmDataSaved()
  {
    boolean rv = true;
    if( this.dataChanged ) {
      setState( Frame.NORMAL );
      toFront();
      String[] options = { "Speichern", "Verwerfen", "Abbrechen" };
      int      selOpt  = JOptionPane.showOptionDialog(
				this,
				"Die Datei wurde ge\u00E4ndert und nicht"
					+" gespeichert.\n"
					+ "M\u00F6chten Sie jetzt speichern?",
				"Daten ge\u00E4ndert",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE,
				null,
				options,
				"Speichern" );
      if( selOpt == 0 ) {
	rv = doSave( false );
      }
      else if( selOpt != 1 ) {
	rv = false;
      }
    }
    return rv;
  }


  private Long getLong( int pos, int len, boolean littleEndian )
  {
    Long rv = null;
    if( (pos >= 0) && (pos + len <= this.fileLen) ) {
      long value = 0L;
      if( littleEndian ) {
	for( int i = pos + len - 1; i >= pos; --i ) {
	  value = (value << 8) | ((int) (this.fileBytes[ i ]) & 0xFF);
	}
      } else {
	for( int i = 0; i < len; i++ ) {
	  value = (value << 8) | ((int) (this.fileBytes[ pos + i ]) & 0xFF);
	}
      }
      rv = new Long( value );
    }
    return rv;
  }


  private void insertBytes(
			int    dstPos,
			byte[] srcBuf,
			int    srcPos ) throws SizeLimitExceededException
  {
    if( (srcPos >= 0) && (srcPos < srcBuf.length) && (srcBuf.length > 0) ) {
      int diffLen = srcBuf.length - srcPos;
      int reqLen  = this.fileLen + diffLen;
      if( reqLen >= this.fileBytes.length ) {
	int n = Math.min( this.fileLen + BUF_EXTEND, Integer.MAX_VALUE );
	if( n < reqLen) {
	  throw new SizeLimitExceededException( "Die max. zul\u00E4ssige"
			+ " Dateigr\u00F6\u00DFe wurde erreicht." );
	}
	byte[] tmpBuf = new byte[ n ];
	if( dstPos > 0 ) {
	  System.arraycopy( this.fileBytes, 0, tmpBuf, 0, dstPos );
	}
	System.arraycopy( srcBuf, srcPos, tmpBuf, dstPos, diffLen );
	if( dstPos < this.fileLen ) {
	  System.arraycopy(
			this.fileBytes,
			dstPos,
			tmpBuf,
			dstPos + diffLen,
			this.fileLen - dstPos );
	}
	this.fileBytes = tmpBuf;
      } else {
	for( int i = this.fileLen - 1; i >= dstPos; --i ) {
	  this.fileBytes[ i + diffLen ] = this.fileBytes[ i ];
	}
	System.arraycopy( srcBuf, srcPos, this.fileBytes, dstPos, diffLen );
      }
      this.fileLen += diffLen;
    }
  }


  private void newFile()
  {
    this.file      = null;
    this.fileBytes = new byte[ 0x100 ];
    this.fileLen   = 0;
    setDataChanged( false );
    updView();
    setCaretPosition( -1, false );
    this.hexCharFld.setYOffset( 0 );
  }


  private void openFileInternal( File file )
  {
    try {
      InputStream in = null;
      try {
	long len = file.length();
	if( len > Integer.MAX_VALUE ) {
	  throwFileTooBig();
	}
	if( len > 0 ) {
	  len = len * 10L / 9L;
	}
	if( len < BUF_EXTEND ) {
	  len = BUF_EXTEND;
	} else if( len > Integer.MAX_VALUE ) {
	  len = Integer.MAX_VALUE;
	}
	byte[] fileBytes = new byte[ (int) len ];
	int    fileLen   = 0;

	in = new FileInputStream( file );
	while( fileLen < fileBytes.length ) {
	  int n = in.read( fileBytes, fileLen, fileBytes.length - fileLen );
	  if( n <= 0 ) {
	    break;
	  }
	  fileLen += n;
	}
	if( fileLen >= fileBytes.length ) {
	  int b = in.read();
	  while( b != -1 ) {
	    if( fileLen >= fileBytes.length ) {
	      int n = Math.min( fileLen + BUF_EXTEND, Integer.MAX_VALUE );
	      if( fileLen >= n ) {
		throwFileTooBig();
	      }
	      byte[] a = new byte[ n ];
	      System.arraycopy( fileBytes, 0, a, 0, fileLen );
	      fileBytes = a;
	    }
	    fileBytes[ fileLen++ ] = (byte) b;
	    b = in.read();
	  }
	}
	in.close();

	this.file      = file;
	this.fileBytes = fileBytes;
	this.fileLen   = fileLen;
	if( !setDataChanged( false ) ) {
	  updTitle();
	}
	updView();
	setCaretPosition( -1, false );
	this.hexCharFld.setYOffset( 0 );
	Main.setLastFile( file, "software" );
      }
      finally {
	EmuUtil.doClose( in );
      }
    }
    catch( IOException ex ) {
      BasicDlg.showErrorDlg( this, ex );
    }
  }


  private void setCaretPosition( int pos, boolean moveOp )
  {
    this.hexCharFld.setCaretPosition( pos, moveOp );
    updCaretPosFields();
  }


  private void setCaretPosition( JTextField textFld, int radix )
  {
    boolean done = false;
    String  text = textFld.getText();
    if( text != null ) {
      try {
	int pos = Integer.parseInt( text, radix );
	if( pos >= 0 ) {
	  if( pos >= this.fileLen ) {
	    pos = this.fileLen - 1;
	  }
	  setCaretPosition( pos, false );
	  done = true;
	}
      }
      catch( NumberFormatException ex ) {}
    }
    if( !done ) {
      BasicDlg.showErrorDlg( this, "Ung\u00FCltige Eingabe" );
    }
  }


  private void updCaretPosFields()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      int row = (caretPos + HexCharFld.BYTES_PER_ROW - 1)
					/ HexCharFld.BYTES_PER_ROW;
      int hRow   = this.hexCharFld.getRowHeight();
      int hFld   = this.hexCharFld.getHeight();
      int yCaret = HexCharFld.MARGIN + (row * hRow);
      int yOffs  = this.hexCharFld.getYOffset();

      if( yCaret < yOffs + hRow ) {
	changeVScrollBar( -(yOffs + hRow - yCaret) );
      }
      else if( yCaret > yOffs + hFld - hRow ) {
	changeVScrollBar( yCaret - (yOffs + hFld - hRow ) );
      }
      this.fldCaretDec.setText( Integer.toString( caretPos ) );
      this.fldCaretHex.setText( Integer.toHexString( caretPos ).toUpperCase() );
    } else {
      this.fldCaretDec.setText( "" );
      this.fldCaretHex.setText( "" );
    }
    updValueFields();
  }


  private boolean setDataChanged( boolean state )
  {
    boolean rv = false;
    if( state != this.dataChanged ) {
      this.dataChanged = state;
      updTitle();
      this.mnuSave.setEnabled( this.dataChanged );
      this.btnSave.setEnabled( this.dataChanged );
      rv = true;
    }
    return rv;
  }


  private static void throwFileTooBig() throws IOException
  {
    throw new IOException( "Datei ist zu gro\u00DF!" );
  }


  private void updScrollBar(
			JScrollBar scrollBar,
			int        visibleSize,
			int        fullSize )
  {
    if( (visibleSize > 0) && (fullSize > 0) ) {
      if( visibleSize > fullSize ) {
	visibleSize = fullSize;
      }
      scrollBar.setMaximum( fullSize );
      scrollBar.setBlockIncrement( visibleSize );
      scrollBar.setVisibleAmount( visibleSize );
    }
  }


  private void updScrollBar( Component c )
  {
    if( c == this.hScrollBar ) {
      updScrollBar(
		this.hScrollBar,
		this.hScrollBar.getWidth(),
		this.hexCharFld.getContentWidth() );
    }
    else if( c == this.vScrollBar ) {
      updScrollBar(
		this.vScrollBar,
		this.vScrollBar.getHeight(),
		this.hexCharFld.getContentHeight() );
    }
  }


  private void updTitle()
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "JKCEMU Hex-Editor: " );
    if( this.file != null ) {
      buf.append( file.getPath() );
    } else {
      buf.append( "Neue Datei" );
    }
    setTitle( buf.toString() );
  }


  private void updValueFields()
  {
    String  text8    = null;
    String  text16   = null;
    String  text32   = null;
    boolean state8   = false;
    boolean state16  = false;
    int     caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      state8 = true;

      boolean valueSigned  = this.btnValueSigned.isSelected();
      boolean littleEndian = this.btnLittleEndian.isSelected();

      if( valueSigned ) {
	text8 = Integer.toString( this.fileBytes[ caretPos ] );
      } else {
	text8 = Integer.toString( ((int) this.fileBytes[ caretPos ]) & 0xFF );
      }

      Long value = getLong( caretPos, 2, littleEndian );
      if( value != null ) {
	state16 = true;
	if( valueSigned ) {
	  text16 = Integer.toString( (int) value.shortValue() );
	} else {
	  text16 = value.toString();
	}
      }

      value = getLong( caretPos, 4, littleEndian );
      if( value != null ) {
	if( valueSigned ) {
	  text32 = Integer.toString( value.intValue() );
	} else {
	  text32 = value.toString();
	}
      }
    }
    this.fldValue8.setText( text8 );
    this.fldValue16.setText( text16 );
    this.fldValue32.setText( text32 );

    this.labelValue8.setEnabled( text8 != null );
    this.labelValue16.setEnabled( text16 != null );
    this.labelValue32.setEnabled( text32 != null );

    this.mnuInsert.setEnabled( state8 );
    this.mnuOverwrite.setEnabled( state8 );
    this.mnuRemove.setEnabled( state8 );
    this.btnValueSigned.setEnabled( state8 );
    this.btnLittleEndian.setEnabled( state16 );
  }


  private void updView()
  {
    this.hexCharFld.setDataBytes( this.fileBytes, this.fileLen );
    updScrollBar( this.vScrollBar );
    updScrollBar( this.hScrollBar );
  }
}

