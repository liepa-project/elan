package mpi.eudico.client.annotator.gui;

/**
 * @(#)MenuScroller.java 1.5.0 04/02/12
 * @author Darryl Burke from http://tips4java.wordpress.com/2009/02/01/menu-scroller/ . Some additional comments from
 *     that page have also been used for modifications. such as Comment 6 from Chris Yates, Comment 25 reply 1 from Vincent,
 *     Comment 10 reply 2 from Anonymous, Comment 16 item 1 and 2 from Huw.
 */

import mpi.eudico.client.annotator.Constants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * A class that provides scrolling capabilities to a long menu dropdown or
 * popup menu.  A number of items can optionally be frozen at the top and/or
 * bottom of the menu.
 *
 * <p><B>Implementation note:</B>  The default number of items to display
 * at a time is 20, and the default scrolling interval is 125 milliseconds.
 *
 * <p>This version is adapted from the file available at tips4java.
 *
 * @version 1.5.0 04/05/12
 * @author Darryl Burke
 */
public class MenuScroller {

    //private JMenu menu;
    private JPopupMenu menu;
    private Component[] menuItems;
    private final MenuScrollItem upItem;
    private final MenuScrollItem downItem;
    private final MenuScrollListener menuListener = new MenuScrollListener();
    private final MouseWheelListener mouseWheelListener = new MouseScrollListener();
    private int scrollCount;
    private int interval;
    private int topFixedCount;
    private int bottomFixedCount;
    private int firstIndex = 0;
    private int keepVisibleIndex = -1;

    /**
     * Registers a menu to be scrolled with the default number of items to
     * display at a time and the default scrolling interval.
     *
     * @param menu the menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a popup menu to be scrolled with the default number of items to
     * display at a time and the default scrolling interval.
     *
     * @param menu the popup menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a menu to be scrolled with the default number of items to
     * display at a time and the specified scrolling interval.
     *
     * @param menu the menu
     * @param scrollCount the number of items to display at a time
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public static MenuScroller setScrollerFor(JMenu menu, int scrollCount) {
        return new MenuScroller(menu, scrollCount);
    }

    /**
     * Registers a popup menu to be scrolled with the default number of items to
     * display at a time and the specified scrolling interval.
     *
     * @param menu the popup menu
     * @param scrollCount the number of items to display at a time
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int scrollCount) {
        return new MenuScroller(menu, scrollCount);
    }

    /**
     * Registers a menu to be scrolled, with the specified number of items to
     * display at a time and the specified scrolling interval.
     *
     * @param menu the menu
     * @param scrollCount the number of items to be displayed at a time
     * @param interval the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public static MenuScroller setScrollerFor(JMenu menu, int scrollCount, int interval) {
        return new MenuScroller(menu, scrollCount, interval);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified number of items to
     * display at a time and the specified scrolling interval.
     *
     * @param menu the popup menu
     * @param scrollCount the number of items to be displayed at a time
     * @param interval the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int scrollCount, int interval) {
        return new MenuScroller(menu, scrollCount, interval);
    }

    /**
     * Registers a menu to be scrolled, with the specified number of items
     * to display in the scrolling region, the specified scrolling interval,
     * and the specified numbers of items fixed at the top and bottom of the
     * menu.
     *
     * @param menu the menu
     * @param scrollCount the number of items to display in the scrolling portion
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0.
     * @param bottomFixedCount the number of items to fix at the bottom. May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount or bottomFixedCount is negative
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JMenu menu, int scrollCount, int interval, int topFixedCount,
                                              int bottomFixedCount) {
        return new MenuScroller(menu, scrollCount, interval, topFixedCount, bottomFixedCount);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified number of items
     * to display in the scrolling region, the specified scrolling interval,
     * and the specified numbers of items fixed at the top and bottom of the
     * popup menu.
     *
     * @param menu the popup menu
     * @param scrollCount the number of items to display in the scrolling portion
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @param bottomFixedCount the number of items to fix at the bottom.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount or bottomFixedCount is negative
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int scrollCount, int interval, int topFixedCount,
                                              int bottomFixedCount) {
        return new MenuScroller(menu, scrollCount, interval, topFixedCount, bottomFixedCount);
    }

    /**
     * Calculates the number for scrollCount such that the menu fills the available
     * vertical space from the point (mouse press) to the bottom of the screen.
     *
     * @param c   The component on which the point parameter is based
     * @param pt  The point at which the top of the menu will appear (in component coordinate space)
     * @param item  A menuitem of prototypical height off of which the average height is determined
     * @param bottomFixedCount  Needed to offset the returned scrollCount
     * @return the scrollCount
     */
    public static int scrollCountForScreen(Component c, Point pt, JMenuItem item, int bottomFixedCount) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point ptScreen = new Point(pt);
        SwingUtilities.convertPointToScreen(ptScreen, c);
        int height = screenSize.height - ptScreen.y;

        int miHeight = item.getPreferredSize().height;
        int scrollCount =
            (height / miHeight) - bottomFixedCount - 2;  // 2 just takes the menu up a bit from the bottom which looks nicer

        return scrollCount;
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * default number of items to display at a time, and default scrolling
     * interval.
     *
     * @param menu the menu
     */
    public MenuScroller(JMenu menu) {
        this(menu, Constants.VISIBLE_MENUITEMS);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * default number of items to display at a time, and default scrolling
     * interval.
     *
     * @param menu the popup menu
     */
    public MenuScroller(JPopupMenu menu) {
        this(menu, Constants.VISIBLE_MENUITEMS);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * specified number of items to display at a time, and default scrolling
     * interval.
     *
     * @param menu the menu
     * @param scrollCount the number of items to display at a time
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public MenuScroller(JMenu menu, int scrollCount) {
        this(menu, scrollCount, 150);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * specified number of items to display at a time, and default scrolling
     * interval.
     *
     * @param menu the popup menu
     * @param scrollCount the number of items to display at a time
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public MenuScroller(JPopupMenu menu, int scrollCount) {
        this(menu, scrollCount, 150);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * specified number of items to display at a time, and specified scrolling
     * interval.
     *
     * @param menu the menu
     * @param scrollCount the number of items to display at a time
     * @param interval the scroll interval, in milliseconds
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public MenuScroller(JMenu menu, int scrollCount, int interval) {
        this(menu, scrollCount, interval, 0, 0);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * specified number of items to display at a time, and specified scrolling
     * interval.
     *
     * @param menu the popup menu
     * @param scrollCount the number of items to display at a time
     * @param interval the scroll interval, in milliseconds
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     */
    public MenuScroller(JPopupMenu menu, int scrollCount, int interval) {
        this(menu, scrollCount, interval, 0, 0);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * specified number of items to display in the scrolling region, the
     * specified scrolling interval, and the specified numbers of items fixed at
     * the top and bottom of the menu.
     *
     * @param menu the menu
     * @param scrollCount the number of items to display in the scrolling portion
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @param bottomFixedCount the number of items to fix at the bottom.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount or bottomFixedCount is negative
     */
    public MenuScroller(JMenu menu, int scrollCount, int interval, int topFixedCount, int bottomFixedCount) {
        this(menu.getPopupMenu(), scrollCount, interval, topFixedCount, bottomFixedCount);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * specified number of items to display in the scrolling region, the
     * specified scrolling interval, and the specified numbers of items fixed at
     * the top and bottom of the popup menu.
     *
     * @param menu the popup menu
     * @param scrollCount the number of items to display in the scrolling portion
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @param bottomFixedCount the number of items to fix at the bottom.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount or bottomFixedCount is negative
     */
    public MenuScroller(JPopupMenu menu, int scrollCount, int interval, int topFixedCount, int bottomFixedCount) {
        if (scrollCount <= 0 || interval <= 0) {
            throw new IllegalArgumentException("scrollCount and interval must be greater than 0");
        }
        if (topFixedCount < 0 || bottomFixedCount < 0) {
            throw new IllegalArgumentException("topFixedCount and bottomFixedCount cannot be negative");
        }

        upItem = new MenuScrollItem(MenuIcon.UP, -1);
        downItem = new MenuScrollItem(MenuIcon.DOWN, +1);
        setScrollCount(scrollCount);
        setInterval(interval);
        setTopFixedCount(topFixedCount);
        setBottomFixedCount(bottomFixedCount);

        this.menu = menu;
        menu.addPopupMenuListener(menuListener);
        menu.addMouseWheelListener(mouseWheelListener);
    }

    /**
     * Returns the scroll interval in milliseconds
     *
     * @return the scroll interval in milliseconds
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Sets the scroll interval in milliseconds
     *
     * @param interval the scroll interval in milliseconds
     * @throws IllegalArgumentException if interval is 0 or negative
     */
    public void setInterval(int interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        }
        upItem.setInterval(interval);
        downItem.setInterval(interval);
        this.interval = interval;
    }

    /**
     * Returns the number of items in the scrolling portion of the menu.
     *
     * @return the number of items to display at a time
     */
    public int getscrollCount() {
        return scrollCount;
    }

    /**
     * Sets the number of items in the scrolling portion of the menu.
     *
     * @param scrollCount the number of items to display at a time
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public void setScrollCount(int scrollCount) {
        if (scrollCount <= 0) {
            throw new IllegalArgumentException("scrollCount must be greater than 0");
        }
        this.scrollCount = scrollCount;
        MenuSelectionManager.defaultManager().clearSelectedPath();
    }

    /**
     * Returns the number of items fixed at the top of the menu or popup menu.
     *
     * @return the number of items
     */
    public int getTopFixedCount() {
        return topFixedCount;
    }

    /**
     * Sets the number of items to fix at the top of the menu or popup menu.
     *
     * @param topFixedCount the number of items
     */
    public void setTopFixedCount(int topFixedCount) {
        if (firstIndex <= topFixedCount) {
            firstIndex = topFixedCount;
        } else {
            firstIndex += (topFixedCount - this.topFixedCount);
        }
        this.topFixedCount = topFixedCount;
    }

    /**
     * Returns the number of items fixed at the bottom of the menu or popup menu.
     *
     * @return the number of items
     */
    public int getBottomFixedCount() {
        return bottomFixedCount;
    }

    /**
     * Sets the number of items to fix at the bottom of the menu or popup menu.
     *
     * @param bottomFixedCount the number of items
     */
    public void setBottomFixedCount(int bottomFixedCount) {
        this.bottomFixedCount = bottomFixedCount;
    }

    /**
     * Scrolls the specified item into view each time the menu is opened.  Call this method with
     * <code>null</code> to restore the default behavior, which is to show the menu as it last
     * appeared.
     *
     * @param item the item to keep visible
     * @see #keepVisible(int)
     */
    public void keepVisible(JMenuItem item) {
        if (item == null) {
            keepVisibleIndex = -1;
        } else {
            int index = menu.getComponentIndex(item);
            keepVisibleIndex = index;
        }
    }

    /**
     * Scrolls the item at the specified index into view each time the menu is opened.  Call this
     * method with <code>-1</code> to restore the default behavior, which is to show the menu as
     * it last appeared.
     *
     * @param index the index of the item to keep visible
     * @see #keepVisible(javax.swing.JMenuItem)
     */
    public void keepVisible(int index) {
        keepVisibleIndex = index;
    }

    /**
     * Removes this MenuScroller from the associated menu and restores the
     * default behavior of the menu.
     */
    public void dispose() {
        if (menu != null) {
            menu.removePopupMenuListener(menuListener);
            menu.removeMouseWheelListener(mouseWheelListener);
            menu = null;
        }
    }

    /**
     * Ensures that the <code>dispose</code> method of this MenuScroller is
     * called when there are no more refrences to it.
     *
     * @exception Throwable if an error occurs.
     * @see MenuScroller#dispose()
     */
    @Override
    protected void finalize() throws
                              Throwable {
        dispose();
    }

    private void refreshMenu() {
        if (menuItems != null && menuItems.length > 0) {
            firstIndex = Math.max(topFixedCount, firstIndex);
            firstIndex = Math.min(menuItems.length - bottomFixedCount - scrollCount, firstIndex);

            upItem.setEnabled(firstIndex > topFixedCount);
            downItem.setEnabled(firstIndex + scrollCount < menuItems.length - bottomFixedCount);

            menu.removeAll();
            for (int i = 0; i < topFixedCount; i++) {
                menu.add(menuItems[i]);
            }
            if (topFixedCount > 0) {
                menu.addSeparator();
            }

            menu.add(upItem);
            for (int i = firstIndex; i < scrollCount + firstIndex; i++) {
                menu.add(menuItems[i]);
            }
            menu.add(downItem);

            if (bottomFixedCount > 0) {
                menu.addSeparator();
            }
            for (int i = menuItems.length - bottomFixedCount; i < menuItems.length; i++) {
                menu.add(menuItems[i]);
            }

            //      JComponent parent = (JComponent) upItem.getParent();
            //      parent.revalidate();
            //      parent.repaint();
            menu.pack();
        }
    }

    private class MenuScrollListener implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            setMenuItems();
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            restoreMenuItems();
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            restoreMenuItems();
        }

        private void setMenuItems() {
            menuItems = menu.getComponents();
            if (keepVisibleIndex >= topFixedCount && keepVisibleIndex <= menuItems.length - bottomFixedCount && (
                keepVisibleIndex > firstIndex + scrollCount
                || keepVisibleIndex < firstIndex)) {
                firstIndex = Math.min(firstIndex, keepVisibleIndex);
                firstIndex = Math.max(firstIndex, keepVisibleIndex - scrollCount + 1);
            }
            if (menuItems.length > topFixedCount + scrollCount + bottomFixedCount) {
                refreshMenu();
            }
        }

        private void restoreMenuItems() {
            menu.removeAll();
            for (Component component : menuItems) {
                menu.add(component);
            }
        }
    }

    private class MouseScrollListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent mwe) {
            firstIndex += mwe.getWheelRotation();
            mwe.consume();
            refreshMenu();
        }
    }

    @SuppressWarnings("serial")
    private class MenuScrollTimer extends Timer {

        public MenuScrollTimer(final int increment, int interval) {
            super(interval, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    firstIndex += increment;
                    refreshMenu();
                }
            });
        }
    }

    @SuppressWarnings("serial")
    private class MenuScrollItem extends JMenuItem implements ChangeListener {

        private final MenuScrollTimer timer;

        public MenuScrollItem(MenuIcon icon, int increment) {
            setIcon(icon);
            setDisabledIcon(icon);
            timer = new MenuScrollTimer(increment, interval);
            addChangeListener(this);
        }

        public void setInterval(int interval) {
            timer.setDelay(interval);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (isArmed() && !timer.isRunning()) {
                timer.start();
            }
            if (!isArmed() && timer.isRunning()) {
                timer.stop();
            }
        }
    }

    private enum MenuIcon implements Icon {

        UP(9, 1, 9),
        DOWN(1, 9, 1);
        final int[] xPoints = {1, 5, 9};
        final int[] yPoints;

        MenuIcon(int... yPoints) {
            this.yPoints = yPoints;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Dimension size = c.getSize();
            Graphics g2 = g.create(size.width / 2 - 5, size.height / 2 - 5, 10, 10);
            g2.setColor(Color.GRAY);
            g2.drawPolygon(xPoints, yPoints, 3);
            if (c.isEnabled()) {
                g2.setColor(Color.BLACK);
                g2.fillPolygon(xPoints, yPoints, 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 0;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }
}
