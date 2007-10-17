//----------------------------------------------------------------------------
// Main.java
//----------------------------------------------------------------------------

package net.sf.gogui.tools.convert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.gamefile.GameReader;
import net.sf.gogui.sgf.SgfError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.tex.TexWriter;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.Options;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;
import net.sf.gogui.xml.XmlWriter;

/** Convert SGF and Jago XML Go game files to other formats. */
public final class Main
{
    /** Main function. */
    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "config:",
                "force",
                "format:",
                "help",
                "title:",
                "version"
            };
            Options opt = Options.parse(args, options);
            if (opt.contains("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            if (opt.contains("version"))
            {
                System.out.println("GoGuiConvert " + Version.get());
                System.exit(0);
            }
            boolean force = opt.contains("force");
            String title = opt.get("title", "");
            ArrayList<String> arguments = opt.getArguments();
            if (arguments.size() != 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            File in = new File(arguments.get(0));
            File out = new File(arguments.get(1));
            String format;
            if (opt.contains("format"))
                format = opt.get("format");
            else
                format =
                    FileUtil.getExtension(out).toLowerCase(Locale.ENGLISH);
            if (! format.equals("sgf")
                && ! format.equals("tex")
                && ! format.equals("xml"))
                throw new Exception("Unknown format");
            if (! in.exists())
                throw new Exception("File \"" + in + "\" not found");
            if (out.exists() && ! force)
                throw new Exception("File \"" + out + "\" already exists");
            GameReader reader = new GameReader(in);
            ConstGameTree tree = reader.getTree();
            String warnings = reader.getWarnings();
            if (warnings != null)
                System.err.println(warnings);
            String version = Version.get();
            if (format.equals("xml"))
                new XmlWriter(new FileOutputStream(out), tree,
                              "GoGuiConvert:" + version);
            else if (format.equals("sgf"))
                new SgfWriter(new FileOutputStream(out), tree, "GoGuiConvert",
                              version);
            else if (format.equals("tex"))
                new TexWriter(title, new FileOutputStream(out), tree);
            else
                assert false; // check above
        }
        catch (Throwable t)
        {
            StringUtil.printException(t);
            System.exit(-1);
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Main()
    {
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: gogui-convert infile outfile\n" +
                  "\n" +
                  "-config  config file\n" +
                  "-force   overwrite existing files\n" +
                  "-format  output format (sgf,tex,xml)\n" +
                  "-help    display this help and exit\n" +
                  "-title   use title\n" +
                  "-version print version and exit\n");
    }
}