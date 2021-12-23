/*
 * (c) 2018-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Suchen und Ersetzen von Text in einer JTextArea
 */

package jkcemu.text;

import java.awt.Component;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class TextFinder
{
  private String  searchText;
  private String  internalSearchText;
  private String  replaceText;
  private boolean ignoreCase;


  public TextFinder(
		String  searchText,
		boolean ignoreCase,
		String  replaceText )
  {
    this.searchText         = searchText;
    this.ignoreCase         = ignoreCase;
    this.replaceText        = replaceText;
    this.internalSearchText = searchText;
    if( ignoreCase ) {
      this.internalSearchText = searchText.toUpperCase();
    }
  }


  public TextFinder(
		String  searchText,
		boolean ignoreCase )
  {
    this( searchText, ignoreCase, null );
  }


  public boolean findNext( JTextArea textArea )
  {
    return find( textArea, -1, false, true );
  }


  public boolean findPrev( JTextArea textArea )
  {
    return find( textArea, -1, true, true );
  }


  public boolean getIgnoreCase()
  {
    return this.ignoreCase;
  }


  public String getReplaceText()
  {
    return this.replaceText;
  }


  public String getSearchText()
  {
    return this.searchText;
  }


  public static TextFinder openFindDlg(
				JTextArea  textArea,
				TextFinder oldTextFinder )
  {
    TextFinder textFinder = oldTextFinder;
    String     searchText = getPresetSearchText( textArea, oldTextFinder );

    String[]    options = { EmuUtil.TEXT_FIND, EmuUtil.TEXT_CANCEL };
    JOptionPane pane    = new JOptionPane(
				EmuUtil.LABEL_SEARCH_FOR,
				JOptionPane.PLAIN_MESSAGE );
    pane.setOptions( options );
    pane.setWantsInput( true );
    if( searchText != null ) {
      pane.setInitialSelectionValue( searchText );
    }
    pane.setInitialValue( options[ 0 ] );
    JDialog dlg = pane.createDialog( textArea, EmuUtil.TEXT_FIND );
    BaseDlg.setParentCentered( dlg );
    dlg.setVisible( true );
    Object value = pane.getValue();
    if( value != null ) {
      if( value.equals( options[ 0 ] ) ) {
	value = pane.getInputValue();
	if( value != null ) {
	  searchText = value.toString();
	  if( searchText != null ) {
	    if( !searchText.isEmpty() ) {
	      textFinder = new TextFinder( searchText, true );
	      textFinder.find( textArea, -1, false, true );
	    }
	  }
	}
      }
    }
    return textFinder;
  }


  public static TextFinder openFindAndReplaceDlg(
				JTextArea  textArea,
				TextFinder oldTextFinder )
  {
    TextFinder textFinder = oldTextFinder;
    Window     window     = EmuUtil.getWindow( textArea );
    if( window != null ) {
      FindTextDlg dlg = new FindTextDlg(
				window,
				getPresetSearchText(
					textArea,
					oldTextFinder ),
				oldTextFinder != null ?
					oldTextFinder.getReplaceText()
					: null,
				oldTextFinder != null ?
					oldTextFinder.getIgnoreCase()
					: true );
      dlg.setVisible( true );

      String searchText = dlg.getSearchText();
      if( searchText != null ) {
	if( !searchText.isEmpty() ) {
	  switch( dlg.getAction() ) {
	    case FIND_NEXT:
	      textFinder = new TextFinder(
				searchText,
				dlg.getIgnoreCase(),
				dlg.getReplaceText() );
	      textFinder.find( textArea, -1, false, true );
	      break;

	    case REPLACE_ALL:
	      textFinder = new TextFinder(
				searchText,
				dlg.getIgnoreCase(),
				dlg.getReplaceText() );
	      int     n     = 0;
	      boolean found = textFinder.find( textArea, 0, false, false );
	      while( found ) {
		if( textFinder.replaceSelection( textArea ) ) {
		  n++;
		}
		found = textFinder.find( textArea, -1, false, false );
	      }
	      if( n == 0 ) {
		showTextNotFound( textArea );
	      } else {
		BaseDlg.showInfoDlg(
			textArea,
			String.format(
				"%d Textersetzung%s durchgef\u00FChrt.",
				n,
				n == 1 ? "" : "en" ),
			"Text ersetzen" );
	      }
	      break;
	  }
	}
      }
    }
    return textFinder;
  }


  public boolean replaceSelection( JTextArea textArea )
  {
    boolean done   = false;
    int     selBeg = textArea.getSelectionStart();
    int     selEnd = textArea.getSelectionEnd();
    if( (selBeg >= 0) && (selBeg < selEnd) ) {
      textArea.replaceSelection(
			this.replaceText != null ? this.replaceText : "" );
      done = true;
    }
    return done;
  }


  public static void showTextNotFound( Component owner )
  {
    BaseDlg.showInfoDlg(
                owner,
                "Text nicht gefunden!",
                "Text suchen" );
  }


	/* --- private Methoden --- */

  /*
   * Eigentliche Suche,
   * Bei startPos < 0 wird die Suche am ausgewaehlten Text bzw.
   * an der aktuellen Cursor-Position fortgesetzt.
   */
  private boolean find(
			JTextArea textArea,
			int       startPos,
			boolean   backward,
			boolean   interactive )
  {
    boolean rv       = false;
    String  baseText = textArea.getText();
    if( baseText == null ) {
      baseText = "";
    }
    if( this.ignoreCase ) {
      baseText = baseText.toUpperCase();
    }
    if( startPos < 0 ) {
      startPos = textArea.getCaretPosition();
    }
    int len      = baseText.length();
    int foundPos = -1;
    if( backward ) {
      if( TextUtil.isTextSelected( textArea ) ) {
	startPos = Math.min(
			textArea.getSelectionStart(),
			textArea.getSelectionEnd() );
      }
      --startPos;
      for( int i = 0; i < 2; i++ ) {
	if( startPos < 1 ) {
	  startPos = len - 1;
	} else if( startPos > (len - 1) ) {
	  startPos = len - 1;
	}
	if( startPos < 0 ) {
	  startPos = 0;
	}
	foundPos = baseText.lastIndexOf(
				this.internalSearchText,
				startPos );
	if( foundPos >= 0 ) {
	  break;
	}
	startPos = len - 1;
      }
    } else {
      if( TextUtil.isTextSelected( textArea ) ) {
	startPos = Math.max(
			textArea.getSelectionStart(),
			textArea.getSelectionEnd() );
      }
      for( int i = 0; i < 2; i++ ) {
	if( (startPos < 0) || (startPos >= len) ) {
	  startPos = 0;
	}
	foundPos = baseText.indexOf(
				this.internalSearchText,
				startPos );
	if( foundPos >= 0 ) {
	  break;
	}
	startPos = 0;
      }
    }
    if( (foundPos >= 0) && (foundPos < len) ) {
      Window window = EmuUtil.getWindow( textArea );
      if( window != null ) {
	window.toFront();
      }
      textArea.requestFocus();
      if( interactive ) {
	textArea.setCaretPosition( foundPos );
      }
      textArea.select( foundPos, foundPos + this.searchText.length() );
      rv = true;
    } else {
      if( interactive ) {
	showTextNotFound( textArea );
      }
    }
    return rv;
  }


  private static String getPresetSearchText(
					JTextArea  textArea,
					TextFinder oldTextFinder )
  {
    String text = textArea.getSelectedText();
    if( text != null ) {
      if( text.isEmpty() ) {
	text = null;
      }
    }
    if( (text == null) && (oldTextFinder != null) ) {
      text = oldTextFinder.getSearchText();
    }
    return text;
  }


  private void replaceAll( JTextArea textArea )
  {
    boolean found = find( textArea, 0, false, false );
    int     n     = 0;
    while( found ) {
      if( replaceSelection( textArea ) ) {
	n++;
      }
      found = find( textArea, -1, false, false );
    }
    if( n == 0 ) {
      showTextNotFound( textArea );
    } else {
      BaseDlg.showInfoDlg(
		textArea,
		String.valueOf( n ) + " Textersetzungen durchgef\u00FChrt.",
		"Text ersetzen" );
    }
  }
}
