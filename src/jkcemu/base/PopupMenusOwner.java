/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface fuer Klassen, die mehrere Popup-Menues bieten
 */

package jkcemu.base;

import javax.swing.JPopupMenu;


public interface PopupMenusOwner
{
  public JPopupMenu[] getPopupMenus();
}
