//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.gui.Clock;
import net.sf.gogui.utils.Platform;

//----------------------------------------------------------------------------

/** Menu bar for GoGui. */
public class GoGuiMenuBar
    implements ActionListener
{
    public GoGuiMenuBar(ActionListener listener)
    {
        m_listener = listener;
        m_menuBar = new JMenuBar();
        m_menuFile = createFileMenu();
        m_menuBar.add(m_menuFile);
        m_menuGame = createGameMenu();
        m_menuBar.add(m_menuGame);
        m_menuVariation = createVariationMenu();
        m_menuBar.add(m_menuVariation);
        m_menuSetup = createSetupMenu();
        m_menuBar.add(m_menuSetup);
        m_menuSettings = createSettingsMenu();
        m_menuBar.add(m_menuSettings);
        m_menuWindow = createWindowMenu();
        m_menuBar.add(m_menuWindow);
        m_menuHelp = createHelpMenu();
        m_menuBar.add(m_menuHelp);
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.startsWith("open-recent-"))
        {
            try
            {
                String indexString =
                    command.substring("open-recent-".length());
                int index = Integer.parseInt(indexString);
                m_selectedRecent = m_recent[index];
                assert(m_selectedRecent != null);
                m_listener.actionPerformed(new ActionEvent(this, 0,
                                                           "open-recent"));
            }
            catch (NumberFormatException e)
            {
                assert(false);
                return;
            }
        }
        else
            assert(false);
    }

    public void addRecent(File file)
    {
        try
        {
            File canonicalFile = file.getCanonicalFile();
            if (canonicalFile.exists())
                file = canonicalFile;
        }
        catch (IOException e)
        {
        }
        for (int i = 0; i < m_maxRecent; ++i)
        {
            if (m_recent[i] == null)
                break;
            if (m_recent[i].equals(file))
            {
                for (int j = i; j > 0; --j)
                    m_recent[j] = m_recent[j - 1];
                m_recent[0] = file;
                updateRecentMenu();
                return;
            }
        }
        for (int i = m_maxRecent - 1; i > 0; --i)
            m_recent[i] = m_recent[i - 1];
        m_recent[0] = file;
        updateRecentMenu();
    }

    public void enableFindNext(boolean enable)
    {
        m_findNextEnabled = enable;
        m_itemFindNext.setEnabled(enable);
    }

    public boolean getBeepAfterMove()
    {
        return m_itemBeepAfterMove.isSelected();
    }

    public JMenuBar getMenuBar()
    {
        return m_menuBar;
    }

    public File getSelectedRecent()
    {
        return m_selectedRecent;
    }

    public boolean getShowCursor()
    {
        return m_itemShowCursor.isSelected();
    }

    public boolean getShowGrid()
    {
        return m_itemShowGrid.isSelected();
    }

    public boolean getShowInfoPanel()
    {
        return m_itemShowInfoPanel.isSelected();
    }

    public boolean getShowLastMove()
    {
        return m_itemShowLastMove.isSelected();
    }

    public boolean getShowToolbar()
    {
        return m_itemShowToolbar.isSelected();
    }

    public boolean getShowVariations()
    {
        return m_itemShowVariations.isSelected();
    }

    public void saveRecent()
    {
        File file = getRecentFile();
        PrintStream out;
        try
        {
            out = new PrintStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Could not save " + file.toString());
            return;
        }
        for (int i = 0; i < m_maxRecent; ++i)
            if (m_recent[i] != null)
                out.println(m_recent[i].toString());
        out.close();
    }

    public void setComputerBlack()
    {
        m_itemComputerBlack.setSelected(true);
    }

    public void setComputerBoth()
    {
        m_itemComputerBoth.setSelected(true);
    }

    public void setComputerEnabled(boolean enabled)
    {
        m_isComputerDisabled = ! enabled;
        m_menuComputerColor.setEnabled(enabled);
        m_itemComputerPlay.setEnabled(enabled);
        m_itemBeepAfterMove.setEnabled(enabled);
        m_itemDetachProgram.setEnabled(enabled);
        m_itemGtpShell.setEnabled(enabled);
        m_itemAnalyze.setEnabled(enabled);
    }

    public void setComputerNone()
    {
        m_itemComputerNone.setSelected(true);
    }

    public void setComputerWhite()
    {
        m_itemComputerWhite.setSelected(true);
    }

    public void selectBoardSizeItem(int size)
    {
        for (int i = 0; i < m_possibleBoardSizes.length; ++i)
            if (m_possibleBoardSizes[i] == size)
            {
                m_itemBoardSize[i].setSelected(true);
                return;
            }
        m_itemBoardSizeOther.setSelected(true);
    }

    public void setBeepAfterMove(boolean enable)
    {
        m_itemBeepAfterMove.setSelected(enable);
    }

    public void setCommandInProgress()
    {
        assert(! m_isComputerDisabled);
        disableAll();
        m_menuFile.setEnabled(true);
        m_itemDetachProgram.setEnabled(true);
        m_itemExit.setEnabled(true);
        m_menuGame.setEnabled(true);
        m_itemInterrupt.setEnabled(true);
        m_menuComputerColor.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemHelp.setEnabled(true);
        m_menuSettings.setEnabled(true);
        m_itemBeepAfterMove.setEnabled(true);
        m_itemShowCursor.setEnabled(true);
        m_itemShowGrid.setEnabled(true);
        m_itemShowInfoPanel.setEnabled(true);
        m_itemShowLastMove.setEnabled(true);
        m_itemShowToolbar.setEnabled(true);
        m_itemShowVariations.setEnabled(true);
        m_menuWindow.setEnabled(true);
        m_itemGtpShell.setEnabled(true);
    }

    public void setNormalMode()
    {
        enableAll();
        m_itemInterrupt.setEnabled(false);
        m_itemSetup.setSelected(false);
        m_itemSetupBlack.setEnabled(false);
        m_itemSetupWhite.setEnabled(false);
        m_itemFindNext.setEnabled(m_findNextEnabled);
        if (m_isComputerDisabled)
        {
            m_menuComputerColor.setEnabled(false);
            m_itemComputerPlay.setEnabled(false);
            m_itemBeepAfterMove.setEnabled(false);
            m_itemDetachProgram.setEnabled(false);
            m_itemGtpShell.setEnabled(false);
            m_itemAnalyze.setEnabled(false);
        }
    }

    public void setSetupMode()
    {
        disableAll();
        m_menuSetup.setEnabled(true);
        m_itemSetup.setEnabled(true);
        m_itemSetupBlack.setEnabled(true);
        m_itemSetupWhite.setEnabled(true);
        m_itemSetupBlack.setSelected(true);
        m_menuFile.setEnabled(true);
        m_menuWindow.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemExit.setEnabled(true);
        m_itemGtpShell.setEnabled(true);
        m_itemHelp.setEnabled(true);
    }

    public void setScoreMode()
    {
        disableAll();
        m_menuFile.setEnabled(true);
        m_menuWindow.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemExit.setEnabled(true);
        m_itemGtpShell.setEnabled(true);
        m_itemHelp.setEnabled(true);
    }

    public void setShowCursor(boolean enable)
    {
        m_itemShowCursor.setSelected(enable);
    }

    public void setShowGrid(boolean enable)
    {
        m_itemShowGrid.setSelected(enable);
    }

    public void setShowInfoPanel(boolean enable)
    {
        m_itemShowInfoPanel.setSelected(enable);
    }

    public void setShowLastMove(boolean enable)
    {
        m_itemShowLastMove.setSelected(enable);
    }

    public void setShowToolbar(boolean enable)
    {
        m_itemShowToolbar.setSelected(enable);
    }

    public void setShowVariations(boolean enable)
    {
        m_itemShowVariations.setSelected(enable);
    }

    /** Enable/disable items according to current position. */
    public void update(GameTree gameTree, Node node, Clock clock)
    {
        Node father = node.getFather();
        boolean hasFather = (father != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtils.getNextVariation(node) != null);
        boolean hasNextEarlierVariation =
            (NodeUtils.getNextEarlierVariation(node) != null);
        boolean hasPrevEarlierVariation =
            (NodeUtils.getPreviousEarlierVariation(node) != null);
        boolean hasPrevVariation =
            (NodeUtils.getPreviousVariation(node) != null);
        boolean isInMain = NodeUtils.isInMainVariation(node);
        boolean treeHasVariations = gameTree.hasVariations();
        m_itemBeginning.setEnabled(hasFather);
        m_itemBackward.setEnabled(hasFather);
        m_itemBackward10.setEnabled(hasFather);
        m_itemForward.setEnabled(hasChildren);
        m_itemForward10.setEnabled(hasChildren);
        m_itemEnd.setEnabled(hasChildren);
        m_itemGoto.setEnabled(hasFather || hasChildren);
        m_itemGotoVar.setEnabled(hasFather || hasChildren);
        m_itemNextVariation.setEnabled(hasNextVariation);
        m_itemPreviousVariation.setEnabled(hasPrevVariation);
        m_itemNextEarlierVariation.setEnabled(hasNextEarlierVariation);
        m_itemPreviousEarlierBackward.setEnabled(hasPrevEarlierVariation);
        m_itemBackToMainVar.setEnabled(! isInMain);
        m_itemTruncate.setEnabled(hasFather);
        m_itemMakeMainVar.setEnabled(! isInMain);
        m_itemKeepOnlyMainVar.setEnabled(isInMain && treeHasVariations);
        m_itemKeepOnlyPosition.setEnabled(hasFather || hasChildren);
        m_itemClockHalt.setEnabled(clock.isRunning());
        m_itemClockResume.setEnabled(! clock.isRunning());
        boolean canRestoreClock = clock.isInitialized()
            && (canRestoreTime(node)
                || (father != null && canRestoreTime(father)));
        m_itemClockRestore.setEnabled(canRestoreClock);
    }

    private boolean m_findNextEnabled;

    private boolean m_isComputerDisabled;

    private static final int m_maxRecent = 20;

    private int m_numberRecent;

    private static int m_possibleBoardSizes[] = { 9, 11, 13, 15, 17, 19 };

    private static int m_possibleHandicaps[] = { 0, 2, 3, 4, 5, 6, 7, 8, 9 };

    private static final int m_shortcutKeyMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private final ActionListener m_listener;

    private File[] m_recent = new File[m_maxRecent];

    private File m_selectedRecent;

    private JCheckBoxMenuItem m_itemBeepAfterMove;

    private JCheckBoxMenuItem m_itemShowCursor;

    private JCheckBoxMenuItem m_itemShowGrid;

    private JCheckBoxMenuItem m_itemShowLastMove;

    private JCheckBoxMenuItem m_itemShowVariations;

    private JCheckBoxMenuItem m_itemSetup;

    private JMenu m_menuComputerColor;

    private final JMenu m_menuFile;

    private final JMenu m_menuGame;

    private final JMenu m_menuHelp;

    private JMenu m_menuRecent;

    private final JMenu m_menuSettings;

    private final JMenu m_menuSetup;

    private final JMenu m_menuVariation;

    private final JMenuBar m_menuBar;

    private final JMenu m_menuWindow;

    private JMenuItem m_itemAbout;

    private JMenuItem m_itemAnalyze;

    private JMenuItem m_itemBackToMainVar;

    private JMenuItem m_itemBackward;

    private JMenuItem m_itemBackward10;

    private JMenuItem m_itemBeginning;

    private JMenuItem[] m_itemBoardSize;

    private JMenuItem m_itemBoardSizeOther;

    private JMenuItem m_itemClockHalt;

    private JMenuItem m_itemClockRestore;

    private JMenuItem m_itemClockResume;

    private JMenuItem m_itemComputerBlack;

    private JMenuItem m_itemComputerBoth;

    private JMenuItem m_itemComputerNone;

    private JMenuItem m_itemComputerPlay;

    private JMenuItem m_itemComputerWhite;

    private JMenuItem m_itemDetachProgram;

    private JMenuItem m_itemEnd;

    private JMenuItem m_itemExit;

    private JMenuItem m_itemFindNext;

    private JMenuItem m_itemForward;

    private JMenuItem m_itemForward10;

    private JMenuItem m_itemGoto;

    private JMenuItem m_itemGotoVar;

    private JMenuItem m_itemGtpShell;

    private JMenuItem m_itemHelp;

    private JMenuItem m_itemInterrupt;

    private JMenuItem m_itemKeepOnlyMainVar;

    private JMenuItem m_itemKeepOnlyPosition;

    private JMenuItem m_itemMakeMainVar;

    private JMenuItem m_itemNextVariation;

    private JMenuItem m_itemNextEarlierVariation;

    private JMenuItem m_itemPreviousVariation;

    private JMenuItem m_itemPreviousEarlierBackward;

    private JMenuItem m_itemSetupBlack;

    private JMenuItem m_itemSetupWhite;

    private JMenuItem m_itemShowInfoPanel;

    private JMenuItem m_itemShowToolbar;

    private JMenuItem m_itemTruncate;

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, String command)
    {
        item.addActionListener(m_listener);
        item.setActionCommand(command);
        menu.add(item);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, int mnemonic,
                                  String command)
    {
        item.setMnemonic(mnemonic);
        return addMenuItem(menu, item, command);
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addMenuItem(menu, item, mnemonic, command);        
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  int accel, int modifier, String command)
    {
        JMenuItem item = new JMenuItem(label);
        KeyStroke k = KeyStroke.getKeyStroke(accel, modifier); 
        item.setAccelerator(k);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private JMenuItem addRadioItem(JMenu menu, ButtonGroup group,
                                   String label, String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addMenuItem(menu, item, command);
    }

    private JMenuItem addRadioItem(JMenu menu, ButtonGroup group,
                                   String label, int mnemonic, String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private JMenu createBoardSizeMenu()
    {
        JMenu menu = createMenu("Size", KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        int n = m_possibleBoardSizes.length;
        m_itemBoardSize = new JMenuItem[n];
        for (int i = 0; i < n; ++i)
        {
            String s = Integer.toString(m_possibleBoardSizes[i]);
            JMenuItem item = addRadioItem(menu, group, s, "board-size-" + s);
            m_itemBoardSize[i] = item;
        }
        menu.addSeparator();
        JMenuItem item = addRadioItem(menu, group, "Other",
                                      "board-size-other");
        m_itemBoardSizeOther = item;
        return menu;
    }

    private JMenu createClockMenu()
    {
        JMenu menu = createMenu("Clock", KeyEvent.VK_K);
        m_itemClockHalt =
            addMenuItem(menu, "Halt", KeyEvent.VK_H, "clock-halt");
        m_itemClockResume =
            addMenuItem(menu, "Resume", KeyEvent.VK_R, "clock-resume");
        m_itemClockRestore =
            addMenuItem(menu, "Restore", KeyEvent.VK_S, "clock-restore");
        return menu;
    }

    private JMenu createComputerColorMenu()
    {
        ButtonGroup group = new ButtonGroup();
        JMenu menu = createMenu("Computer Color", KeyEvent.VK_C);
        m_itemComputerBlack = addRadioItem(menu, group, "Black",
                                           KeyEvent.VK_B, "computer-black");
        m_itemComputerWhite = addRadioItem(menu, group, "White",
                                           KeyEvent.VK_W, "computer-white");
        m_itemComputerBoth = addRadioItem(menu, group, "Both", KeyEvent.VK_T,
                                          "computer-both");
        m_itemComputerNone = addRadioItem(menu, group, "None", KeyEvent.VK_N,
                                          "computer-none");
        return menu;
    }

    private JMenu createFileMenu()
    {
        JMenu menu = createMenu("File", KeyEvent.VK_F);
        addMenuItem(menu, "Open...", KeyEvent.VK_O, KeyEvent.VK_O,
                    m_shortcutKeyMask, "open");
        menu.add(createRecentMenu());
        addMenuItem(menu, "Save...", KeyEvent.VK_S, KeyEvent.VK_S,
                    m_shortcutKeyMask, "save");
        addMenuItem(menu, "Save Position...", KeyEvent.VK_T, "save-position");
        menu.addSeparator();
        addMenuItem(menu, "Print...", KeyEvent.VK_P, KeyEvent.VK_P,
                    m_shortcutKeyMask, "print");
        menu.addSeparator();
        addMenuItem(menu, "Attach Program...", KeyEvent.VK_A,
                    "attach-program");
        m_itemDetachProgram = addMenuItem(menu, "Detach Program",
                                          KeyEvent.VK_D, "detach-program");
        menu.addSeparator();
        m_itemExit = addMenuItem(menu, "Quit", KeyEvent.VK_Q, KeyEvent.VK_Q,
                                 m_shortcutKeyMask, "exit");
        return menu;
    }

    private JMenu createGameMenu()
    {
        JMenu menu = createMenu("Game", KeyEvent.VK_G);
        addMenuItem(menu, "New Game", KeyEvent.VK_N, "new-game");
        addMenuItem(menu, "Info", KeyEvent.VK_I, KeyEvent.VK_I,
                    m_shortcutKeyMask, "game-info");
        menu.add(createBoardSizeMenu());
        menu.add(createHandicapMenu());
        m_menuComputerColor = createComputerColorMenu();
        menu.add(m_menuComputerColor);
        menu.add(createClockMenu());
        menu.addSeparator();
        int shortcutKeyMask = 0;
        if (Platform.isMac())
            shortcutKeyMask = m_shortcutKeyMask;
        addMenuItem(menu, "Pass", KeyEvent.VK_P, KeyEvent.VK_F2,
                    shortcutKeyMask, "pass");
        m_itemComputerPlay = addMenuItem(menu, "Computer Play", KeyEvent.VK_L,
                                         KeyEvent.VK_F5, shortcutKeyMask,
                                         "play");
        m_itemInterrupt =
            addMenuItem(menu, "Interrupt", KeyEvent.VK_T, KeyEvent.VK_ESCAPE,
                        0, "interrupt");
        menu.addSeparator();
        m_itemBeginning =
            addMenuItem(menu, "Beginning", KeyEvent.VK_N, KeyEvent.VK_HOME,
                        m_shortcutKeyMask, "beginning");
        m_itemBackward10 =
            addMenuItem(menu, "Backward 10", KeyEvent.VK_D, KeyEvent.VK_LEFT,
                        m_shortcutKeyMask | ActionEvent.SHIFT_MASK,
                        "backward-10");
        m_itemBackward =
            addMenuItem(menu, "Backward", KeyEvent.VK_B, KeyEvent.VK_LEFT,
                        m_shortcutKeyMask, "backward");
        m_itemForward =
            addMenuItem(menu, "Forward", KeyEvent.VK_F, KeyEvent.VK_RIGHT,
                        m_shortcutKeyMask, "forward");
        m_itemForward10 =
            addMenuItem(menu, "Forward 10", KeyEvent.VK_O, KeyEvent.VK_RIGHT,
                        m_shortcutKeyMask | ActionEvent.SHIFT_MASK,
                        "forward-10");
        m_itemEnd =
            addMenuItem(menu, "End", KeyEvent.VK_E, KeyEvent.VK_END,
                        m_shortcutKeyMask, "end");
        m_itemGoto =
            addMenuItem(menu, "Go To...", KeyEvent.VK_G, KeyEvent.VK_G,
                        m_shortcutKeyMask, "goto");
        menu.addSeparator();
        addMenuItem(menu, "Score", KeyEvent.VK_R, "score");
        return menu;
    }

    private JMenu createHandicapMenu()
    {
        JMenu menu = createMenu("Handicap", KeyEvent.VK_H);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < m_possibleHandicaps.length; ++i)
        {
            String s = Integer.toString(m_possibleHandicaps[i]);
            JMenuItem item = addRadioItem(menu, group, s, "handicap-" + s);
            if (m_possibleHandicaps[i] == 0)
                item.setSelected(true);
        }
        return menu;
    }

    private JMenu createHelpMenu()
    {
        int shortcutKeyMask = 0;
        if (Platform.isMac())
            shortcutKeyMask = m_shortcutKeyMask;
        JMenu menu = createMenu("Help", KeyEvent.VK_H);
        JMenuItem itemHelp =
            addMenuItem(menu, "GoGui Documentation", KeyEvent.VK_C,
                        KeyEvent.VK_F1, shortcutKeyMask, "help");
        JMenuItem itemAbout = addMenuItem(menu, "About", KeyEvent.VK_A,
                                          "about");
        m_itemHelp = itemHelp;
        m_itemAbout = itemAbout;
        return menu;
    }

    private JMenu createMenu(String name, int mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private JMenu createRecentMenu()
    {
        m_menuRecent = createMenu("Open Recent", KeyEvent.VK_R);
        loadRecent();
        updateRecentMenu();
        return m_menuRecent;
    }

    private JMenu createSettingsMenu()
    {
        JMenu menu = createMenu("Settings", KeyEvent.VK_S);
        m_itemBeepAfterMove = new JCheckBoxMenuItem("Beep After Move");
        addMenuItem(menu, m_itemBeepAfterMove, KeyEvent.VK_B,
                    "beep-after-move");
        m_itemShowCursor = new JCheckBoxMenuItem("Show Cursor");
        m_itemShowCursor.setSelected(true);
        addMenuItem(menu, m_itemShowCursor, KeyEvent.VK_C, "show-cursor");
        m_itemShowGrid = new JCheckBoxMenuItem("Show Grid");
        m_itemShowGrid.setSelected(true);
        addMenuItem(menu, m_itemShowGrid, KeyEvent.VK_G, "show-grid");
        m_itemShowInfoPanel = new JCheckBoxMenuItem("Show Info Panel");
        addMenuItem(menu, m_itemShowInfoPanel, KeyEvent.VK_I,
                    "show-info-panel");
        m_itemShowLastMove = new JCheckBoxMenuItem("Show Last Move");
        m_itemShowLastMove.setSelected(true);
        addMenuItem(menu, m_itemShowLastMove, KeyEvent.VK_L,
                    "show-last-move");
        m_itemShowToolbar = new JCheckBoxMenuItem("Show Toolbar");
        addMenuItem(menu, m_itemShowToolbar, KeyEvent.VK_T,
                    "show-toolbar");
        m_itemShowVariations = new JCheckBoxMenuItem("Show Variations");
        m_itemShowVariations.setSelected(true);
        addMenuItem(menu, m_itemShowVariations, KeyEvent.VK_V,
                    "show-variations");
        return menu;
    }

    private JMenu createSetupMenu()
    {
        JMenu menu = createMenu("Setup", KeyEvent.VK_E);
        m_itemSetup = new JCheckBoxMenuItem("Setup Mode");
        addMenuItem(menu, m_itemSetup, KeyEvent.VK_S, "setup");
        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();
        m_itemSetupBlack =
            addRadioItem(menu, group, "Black", KeyEvent.VK_B, "setup-black");
        m_itemSetupBlack.setSelected(true);
        m_itemSetupWhite =
            addRadioItem(menu, group, "White", KeyEvent.VK_W, "setup-white");
        return menu;
    }

    private JMenu createVariationMenu()
    {
        int shiftMask = java.awt.event.InputEvent.SHIFT_MASK;
        JMenu menu = createMenu("Variation", KeyEvent.VK_V);
        m_itemNextVariation =
            addMenuItem(menu, "Next Variation", KeyEvent.VK_N,
                        KeyEvent.VK_DOWN, m_shortcutKeyMask,
                        "next-variation");
        m_itemPreviousVariation =
            addMenuItem(menu, "Previous Variation", KeyEvent.VK_P,
                        KeyEvent.VK_UP, m_shortcutKeyMask,
                        "previous-variation");
        m_itemNextEarlierVariation =
            addMenuItem(menu, "Next Earlier Variation", KeyEvent.VK_E,
                        KeyEvent.VK_DOWN, m_shortcutKeyMask | shiftMask,
                        "next-earlier-variation");
        m_itemPreviousEarlierBackward =
            addMenuItem(menu, "Previous Earlier Variation", KeyEvent.VK_R,
                        KeyEvent.VK_UP, m_shortcutKeyMask | shiftMask,
                        "previous-earlier-variation");
        m_itemBackToMainVar =
            addMenuItem(menu, "Back to Main Variation", KeyEvent.VK_B,
                        KeyEvent.VK_M, m_shortcutKeyMask,
                        "back-to-main-variation");
        m_itemGotoVar =
            addMenuItem(menu, "Go to Variation...",
                        KeyEvent.VK_V, "goto-variation");
        menu.addSeparator();
        m_itemMakeMainVar = addMenuItem(menu, "Make Main Variation",
                                        KeyEvent.VK_M, "make-main-variation");
        m_itemKeepOnlyMainVar = addMenuItem(menu,
                                            "Delete Side Variations",
                                            KeyEvent.VK_D,
                                            "keep-only-main-variation");
        m_itemKeepOnlyPosition = addMenuItem(menu,
                                             "Keep Only Position",
                                             KeyEvent.VK_K,
                                             "keep-only-position");
        m_itemTruncate = addMenuItem(menu, "Truncate", KeyEvent.VK_T,
                                     "truncate");
        menu.addSeparator();
        addMenuItem(menu, "Find in Comments...", KeyEvent.VK_F, KeyEvent.VK_F,
                    m_shortcutKeyMask, "find-in-comments");
        m_itemFindNext = addMenuItem(menu, "Find Next", KeyEvent.VK_X,
                                     KeyEvent.VK_F3, 0, "find-next");
        m_itemFindNext.setEnabled(false);
        return menu;
    }

    private JMenu createWindowMenu()
    {
        int shortcutKeyMask = 0;
        if (Platform.isMac())
            shortcutKeyMask = m_shortcutKeyMask;
        JMenu menu = createMenu("Window", KeyEvent.VK_W);
        addMenuItem(menu, "Game Tree", KeyEvent.VK_T, KeyEvent.VK_F7,
                    shortcutKeyMask, "show-gametree");
        m_itemAnalyze = addMenuItem(menu, "Analyze", KeyEvent.VK_A,
                                    KeyEvent.VK_F8, shortcutKeyMask,
                                    "analyze");
        m_itemGtpShell = addMenuItem(menu, "GTP Shell", KeyEvent.VK_G,
                                     KeyEvent.VK_F9, shortcutKeyMask,
                                     "gtp-shell");
        return menu;
    }

    private void disableAll()
    {
        for (int i = 0; i < m_menuBar.getMenuCount(); ++i)
        {
            JMenu menu = m_menuBar.getMenu(i);
            if (menu != null)
                disableMenu(menu);
        }
    }

    private void disableMenu(JMenu menu)
    {
        menu.setEnabled(false);
        for (int i = 0; i < menu.getItemCount(); ++i)
            if (menu.getItem(i) != null)
                menu.getItem(i).setEnabled(false);
    }

    private void enableAll()
    {
        for (int i = 0; i < m_menuBar.getMenuCount(); ++i)
        {
            JMenu menu = m_menuBar.getMenu(i);
            if (menu != null)
            {
                menu.setEnabled(true);
                for (int j = 0; j < menu.getItemCount(); ++j)
                    if (menu.getItem(j) != null)
                        menu.getItem(j).setEnabled(true);
            }
        }
        m_menuRecent.setEnabled(m_numberRecent > 0);
    }

    private File getRecentFile()
    {
        String home = System.getProperty("user.home");
        return new File(new File(home, ".gogui"), "recent-files");
    }

    private boolean canRestoreTime(Node node)
    {
        return ! Double.isNaN(node.getTimeLeft(GoColor.BLACK))
            || ! Double.isNaN(node.getTimeLeft(GoColor.WHITE))
            || node.getFather() == null;
    }

    private void loadRecent()
    {
        m_numberRecent = 0;
        File file = getRecentFile();
        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                if (m_numberRecent >= m_maxRecent - 1)
                    break;
                File recent = new File(line);
                if (! recent.exists())
                    continue;
                m_recent[m_numberRecent] = recent;
                ++m_numberRecent;
            }
        }
        catch (IOException e)
        {
        }
        try
        {
            reader.close();
        }
        catch (IOException e)
        {
        }
    }

    private void updateRecentMenu()
    {
        m_menuRecent.removeAll();
        m_numberRecent = 0;
        for (int i = 0; i < m_maxRecent; ++i)
        {
            File file = m_recent[i];
            if (file == null)
                break;
            ++m_numberRecent;
            JMenuItem item = new JMenuItem(file.getName());
            item.addActionListener(this);
            item.setActionCommand("open-recent-" + i);
            m_menuRecent.add(item);
        }
        m_menuRecent.setEnabled(m_numberRecent > 0);
    }
}

//----------------------------------------------------------------------------