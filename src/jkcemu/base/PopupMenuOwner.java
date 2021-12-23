/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface fuer Klassen, die ein Popup-Menue bieten
 */

package jkcemu.base;

import javax.swing.JPopupMenu;


public interface PopupMenuOwner
{
  public JPopupMenu getPopupMenu();
}
