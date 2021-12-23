/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Schaerfen eines Bildes
 */

package jkcemu.image;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public class SharpenDlg extends BaseDlg implements ListSelectionListener
{
  private ImageFld               imgFld;
  private BufferedImage          appliedImg;
  private BufferedImage          curImg;
  private BufferedImage          orgImg;
  private java.util.List<Kernel> kernels;
  private JList<String>          listGrade;
  private JButton                btnApply;
  private JButton                btnCancel;


  public static BufferedImage showDlg( ImageFrm imageFrm )
  {
    SharpenDlg dlg = new SharpenDlg( imageFrm );
    dlg.setVisible( true );
    return dlg.appliedImg;
  }


	/* --- ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    Object src = e.getSource();
    if( src == this.listGrade ) {
      gradeChanged();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnApply ) {
      rv = true;
      doApply();
    }
    else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.listGrade.removeListSelectionListener( this );
      this.listGrade.removeKeyListener( this );
      this.btnApply.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    if( (e.getSource() == this.listGrade)
	&& (e.getKeyCode() == KeyEvent.VK_ENTER) )
    {
      doApply();
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (this.listGrade != null) && (e.getComponent() == this) ) {
      this.listGrade.setSelectedIndex( 0 );
      this.listGrade.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private SharpenDlg( ImageFrm imageFrm )
  {
    super( imageFrm, "Sch\u00E4rfen" );
    this.imgFld     = imageFrm.getImageFld();
    this.orgImg     = this.imgFld.getImage();
    this.curImg     = null;
    this.appliedImg = null;


    // Schaerfegrade und zugehoerige Kernel
    this.kernels = new ArrayList<>();
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				 0.0F, -0.3F,  0.0F,
				-0.3F,  2.2F, -0.3F,
				 0.0F, -0.3F,  0.0F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				 0.0F, -0.5F,  0.0F,
				-0.5F,  3.0F, -0.5F,
				 0.0F, -0.5F,  0.0F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				 0.0F, -0.7F,  0.0F,
				-0.7F,  3.8F, -0.7F,
				 0.0F, -0.7F,  0.0F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				 0F, -1F,  0F,
				-1F,  5F, -1F,
				 0F, -1F,  0F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				-0.25F, -1.0F, -0.25F,
				-1.00F,  6.0F, -1.00F,
				-0.25F, -1.0F, -0.25F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				-0.5F, -1.0F, -0.5F,
				-1.0F,  7.0F, -1.0F,
				-0.5F, -1.0F, -0.5F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				-0.7F, -1.0F, -0.7F,
				-1.0F,  7.8F, -1.0F,
				-0.7F, -1.0F, -0.7F } ) );
    this.kernels.add(
		new Kernel(
			3, 3,
			new float[] {
				-1F, -1F, -1F,
				-1F,  9F, -1F,
				-1F, -1F, -1F } ) );
    this.kernels.add(
		new Kernel(
			5, 5,
			new float[] {
				 0.0F,  0.0F, -0.5F,  0.0F,  0.0F,
				 0.0F, -0.5F, -1.0F, -0.5F,  0.0F,
				-0.5F, -1.0F,  9.0F, -1.0F, -0.5F,
				 0.0F, -0.5F, -1.0F, -0.5F,  0.0F,
				 0.0F,  0.0F, -0.5F,  0.0F,  0.0F } ) );
    this.kernels.add(
		new Kernel(
			5, 5,
			new float[] {
				 0.0F,   0.0F, -0.7F,  0.0F,  0.0F,
				 0.0F,  -0.7F, -1.0F, -0.7F,  0.0F,
				-0.7F,  -1.0F, 10.6F, -1.0F, -0.7F,
				 0.0F,  -0.7F, -1.0F, -0.7F,  0.0F,
				 0.0F,   0.0F, -0.7F,  0.0F,  0.0F } ) );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "Sch\u00E4rfegrad:" ), gbc );

    DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement( "0 - nicht gesch\u00E4rft" );
    listModel.addElement( "1 - leicht gesch\u00E4rft" );
    int n = this.kernels.size();
    for( int i = 2; i < n; i++ ) {
      listModel.addElement( String.format( "%d", i ) );
    }
    listModel.addElement( String.format( "%d - stark gesch\u00E4rft", n ) );
    this.listGrade = GUIFactory.createList( listModel );
    this.listGrade.setVisibleRowCount( listModel.getSize() );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( GUIFactory.createScrollPane( this.listGrade ), gbc );


    // Schaltflaechen
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.insets.top  = 10;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButtonOK();
    panelBtn.add( this.btnApply );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Bild anzeigen
    gradeChanged();


    // Listener
    this.listGrade.addListSelectionListener( this );
    this.listGrade.addKeyListener( this );
    this.btnApply.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    if( this.curImg != this.orgImg ) {
      this.appliedImg = this.curImg;
    }
    doClose();
  }


  private void gradeChanged()
  {
    if( this.orgImg != null ) {
      int kernelIdx = this.listGrade.getSelectedIndex() - 1;
      if( (kernelIdx >= 0) && (kernelIdx < this.kernels.size()) ) {
	RenderingHints hints = new RenderingHints(
					RenderingHints.KEY_DITHERING,
					RenderingHints.VALUE_DITHER_ENABLE );
	hints.put(
		RenderingHints.KEY_COLOR_RENDERING,
		RenderingHints.VALUE_COLOR_RENDER_QUALITY );
	hints.put(
		RenderingHints.KEY_RENDERING,
		RenderingHints.VALUE_RENDER_QUALITY );
	this.curImg = (new ConvolveOp(
				this.kernels.get( kernelIdx ),
				ConvolveOp.EDGE_NO_OP,
				hints )).filter( this.orgImg, null );
      } else {
	this.curImg = this.orgImg;
      }
      this.imgFld.setImage( this.curImg );
      this.imgFld.repaint();
    }
  }
}
