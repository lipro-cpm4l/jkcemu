/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Manager fuer das Ereignis "Drueckens der Enter-Taste" bei JComboBoxen
 *
 * Da ComboBoxEditor und die zugehoerige Editorkomponente
 * im Java/Swing geshared genutzt werden koennten,
 * bedarf das Registrieren eines Listener einer speziellen Verwaltung,
 * damit es keine Seiteneffekte gibt.
 * Diese Verwaltung uebernimmt die Klasse.
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class ComboBoxEnterActionMngr implements ActionListener
{
  private static ComboBoxEnterActionMngr instance = null;


  public static interface EnterListener
  {
    public void comboBoxEnterAction( JComboBox<?> comboBox );
  }


  public static void addListener(
				JComboBox<?>  comboBox,
				EnterListener listener )
  {
    getInstance().addListenerInternal( comboBox, listener );
  }


  public static void removeListener(
				JComboBox<?>  comboBox,
				EnterListener listener )
  {
    getInstance().removeListenerInternal( comboBox, listener );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src        = e.getSource();
    Entry  foundEntry = null;
    synchronized( this ) {
      for( Entry entry : this.entries ) {
	if( (entry.editorFld == src)
	    && SwingUtilities.isDescendingFrom(
					entry.editorFld,
					entry.comboBox ) )
	{
	  foundEntry = entry;
	  break;
	}
      }
    }
    if( foundEntry != null ) {
      final Entry usedEntry = foundEntry;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    usedEntry.listener.comboBoxEnterAction(
						usedEntry.comboBox );
		  }
		} );
    }
  }


	/* --- private Klassen, Attribute und Methoden --- */

  private static class Entry
  {
    private JComboBox<?>  comboBox;
    private JTextField    editorFld;
    private EnterListener listener;

    private Entry(
		JComboBox<?>  comboBox,
		JTextField    editorFld,
		EnterListener listener )
    {
      this.comboBox  = comboBox;
      this.editorFld = editorFld;
      this.listener  = listener;
    }
  };


  private java.util.List<Entry> entries;


  private static ComboBoxEnterActionMngr getInstance()
  {
    synchronized( ComboBoxEnterActionMngr.class ) {
      if( instance == null ) {
	instance = new ComboBoxEnterActionMngr();
      }
    }
    return instance;
  }


  private void addListenerInternal(
				JComboBox<?>  comboBox,
				EnterListener listener )
  {
    ComboBoxEditor e = comboBox.getEditor();
    if( e != null ) {
      Component c = e.getEditorComponent();
      if( c != null ) {
	if( c instanceof JTextField ) {
	  synchronized( this ) {
	    boolean registered = false;
	    for( Entry entry : this.entries ) {
	      if( entry.editorFld == c ) {
		registered = true;
		break;
	      }
	    }
	    if( !registered ) {
	      ((JTextField) c).addActionListener( this );
	    }
	    this.entries.add(
		new Entry( comboBox, (JTextField) c, listener ) );
	  }
	}
      }
    }
  }


  private void removeListenerInternal(
				JComboBox<?>  comboBox,
				EnterListener listener )
  {
    int n = this.entries.size();
    if( n > 0 ) {
      synchronized( this ) {
	JTextField editorFld  = null;
	boolean    registered = false;
	for( int i = n - 1; i >= 0; --i ) {
	  Entry entry = this.entries.get( i );
	  if( editorFld != null ) {
	    if( entry.editorFld == editorFld ) {
	      registered = true;
	      break;
	    }
	  } else {
	    if( (entry.comboBox == comboBox)
		&& (entry.listener == listener) )
	    {
	      editorFld = entry.editorFld;
	      this.entries.remove( i );
	    }
	  }
	}
	if( !registered && (editorFld != null) ) {
	  editorFld.removeActionListener( this );
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private ComboBoxEnterActionMngr()
  {
    this.entries = new ArrayList<>();
  }
}
