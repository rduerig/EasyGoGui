//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.EditorKit;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** About dialog for GoGui. */
public class AboutDialog
    extends JOptionPane
{
    public static void show(Component parent, String name, String version,
                            String protocolVersion, String command)
    {
        AboutDialog aboutDialog =
            new AboutDialog(name, version, protocolVersion, command);
        JDialog dialog = aboutDialog.createDialog(parent, "About");
        dialog.setVisible(true);
        dialog.dispose();
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private AboutDialog(String name, String version, String protocolVersion,
                        String command)
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        boolean isProgramAvailable = (name != null && ! name.equals(""));
        int tabIndex = 0;
        JPanel programPanel;
        if (isProgramAvailable)
        {
            String versionInfo = "";
            if (version != null && ! version.equals(""))
                versionInfo = "<p align=\"center\">Version " + version
                    + "</p>";
            int width = GuiUtils.getDefaultMonoFontSize() * 25;
            programPanel =
                createPanel("<p align=\"center\"><b>" + name + "</b></p>" +
                            versionInfo +
                            "<p align=\"center\" width=\"" + width + "\">" +
                            "GTP protocol version " + protocolVersion
                            + "<br>" +
                            "Command: " +
                            "<tt>" + command + "</tt></p>");
            tabbedPane.add("Program", programPanel);
            tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_P);
            tabbedPane.setSelectedIndex(tabIndex);
            ++tabIndex;
        }
        tabbedPane.add("GoGui", createPanelGoGui());
        tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_G);
        ++tabIndex;
        tabbedPane.add("Java", createPanelJava());
        tabbedPane.setMnemonicAt(tabIndex, KeyEvent.VK_J);
        ++tabIndex;
        setMessage(tabbedPane);
        setOptionType(DEFAULT_OPTION);
    }

    private static JPanel createPanel(String text)
    {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBorder(GuiUtils.createEmptyBorder());        
        editorPane.setEditable(false);
        if (Platform.isMac())
        {
            Color color = UIManager.getColor("Label.background");
            if (color != null)
                editorPane.setBackground(color);
        }
        panel.add(editorPane);
        EditorKit editorKit =
            JEditorPane.createEditorKitForContentType("text/html");
        editorPane.setEditorKit(editorKit);
        editorPane.setText(text);
        editorPane.addHyperlinkListener(new HyperlinkListener()
            {
                public void hyperlinkUpdate(HyperlinkEvent event)
                {
                    HyperlinkEvent.EventType type = event.getEventType();
                    if (type == HyperlinkEvent.EventType.ACTIVATED)
                    {
                        URL url = event.getURL();
                        if (! Platform.openInExternalBrowser(url))
                            SimpleDialogs.showError(null,
                                                    "Could not open URL"
                                                    + " in external browser");
                    }
                }
            });
        return panel;
    }

    private JPanel createPanelGoGui()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        String imageName = "net/sf/gogui/images/project-support.png";
        URL imageUrl = classLoader.getResource(imageName);
        String projectUrl = "http://gogui.sourceforge.net";
        String supportUrl =
            "http://sourceforge.net/donate/index.php?group_id=59117";
        return createPanel("<p align=\"center\"><b>GoGui</b></p>" +
                           "<p align=\"center\">" +
                           "Version " + Version.get() + "</p>" +
                           "<p align=\"center\">" +
                           "Graphical interface to Go programs<br>" +
                           "&copy; 2005 Markus Enzenberger" +
                           "<br>" +
                           "<tt><a href=\"" + projectUrl + "\">"
                           + projectUrl + "</a></tt>" +
                           "</p>" +
                           "<p align=\"center\">" +
                           "<a href=\"" + supportUrl + "\">"
                           + "<img src=\"" + imageUrl
                           + "\" border=\"0\"></a>" + "</p>");
    }

    private JPanel createPanelJava()
    {
        StringBuffer buffer = new StringBuffer(256);
        String name = System.getProperty("java.vm.name");
        if (name == null)
            buffer.append("<p>Unknown Java VM</p>");
        else
        {
            buffer.append("<p align=\"center\"><b>");
            buffer.append(name);
            buffer.append("</b></p>");
            String version = System.getProperty("java.vm.version");
            if (version != null)
            {
                buffer.append("<p align=\"center\">Version ");
                buffer.append(version);
                buffer.append("</p>");
            }
            buffer.append("<p align=\"center\">");
            String vendor = System.getProperty("java.vm.vendor");
            if (vendor != null)
            {
                buffer.append(vendor);
            }
            String info = System.getProperty("java.vm.info");
            if (info != null)
            {
                buffer.append("<br>");
                buffer.append(info);
            }
            buffer.append("</p>");
        }
        return createPanel(buffer.toString());
    }
}

//----------------------------------------------------------------------------