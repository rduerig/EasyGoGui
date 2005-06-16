//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.ProgressShow;

//----------------------------------------------------------------------------

class ByteCountInputStream
    extends InputStream
{
    public ByteCountInputStream(InputStream in)
    {
        m_in = in;
    }

    public long getCount()
    {
        return m_byteCount;
    }

    public int read() throws IOException
    {
        int result = m_in.read();
        if (result > 0)
            ++m_byteCount;
        return result;
    }

    public int read(byte[] b) throws IOException
    {
        int result = m_in.read(b);
        if (result > 0)
            m_byteCount += result;
        return result;
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        int result = m_in.read(b, off, len);
        if (result > 0)
            m_byteCount += result;
        return result;
    }

    private long m_byteCount;

    private final InputStream m_in;
}

//----------------------------------------------------------------------------

/** SGF reader.
    @bug The error messages sometimes contain wrong line numbers, because of
    problems in StreamTokenizer.lineno(). The implementation should be
    replaced not using StreamTokenizer, because this class is a legacy class.
*/
public class SgfReader
{
    /** SGF read error. */
    public static class SgfError
        extends ErrorMessage
    {
        public SgfError(String s)
        {
            super(s);
        }

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }    

    /** Read SGF file from stream.
        Default charset is ISO-8859-1.
        The charset property is only respected if the stream is a
        FileInputStream, because it has to be reopened with a different
        encoding.
        @param in Stream to read from.
        @param name Name prepended to error messages (must be the filename
        for FileInputStream to allow reopening the stream after a charset
        change)
        @param progressShow Callback to show progress, can be null
        @param size Size of stream if progressShow != null
        @throws SgfError If reading fails.
    */
    public SgfReader(InputStream in, String name, ProgressShow progressShow,
                     long size)
        throws SgfError
    {
        m_name = name;
        m_progressShow = progressShow;
        m_size = size;
        m_isFile = (in instanceof FileInputStream && name != null);
        if (progressShow != null)
            progressShow.showProgress(0);
        try
        {
            readSgf(in, "ISO-8859-1");
        }
        catch (SgfCharsetChanged e1)
        {
            try
            {
                in = new FileInputStream(new File(name));
            }
            catch (IOException e2)
            {
                throw new SgfError("Could not reset SGF stream after"
                                   + " charset change");
            }
            try
            {
                readSgf(in, m_newCharset);
            }
            catch (SgfCharsetChanged e3)
            {
                assert(false);
            }
        }
    }

    /** Get game tree of loaded SGF file. */
    public GameTree getGameTree()
    {
        return m_gameTree;
    }

    /** Get warnings that occurred during loading SGF file.
        @return String with warning messages or null if no warnings.
    */
    public String getWarnings()
    {
        if (m_warnings.isEmpty())
            return null;
        StringBuffer result = new StringBuffer(m_warnings.size() * 80);
        Iterator iter = m_warnings.iterator();
        while (iter.hasNext())
        {
            String s = (String)iter.next();
            result.append(s);
            result.append('\n');
        }
        return result.toString();
    }

    private static class SgfCharsetChanged
        extends Exception
    {
        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    private static final int CACHE_SIZE = 30;

    private boolean m_ignoreTimeSettings;

    private boolean m_isFile;

    private boolean m_sizeFixed;

    private int m_byoyomiMoves = -1;

    private int m_lastPercent;

    private long m_byoyomi = -1;

    private long m_preByoyomi = -1;

    private long m_size;

    private ByteCountInputStream m_byteCountInputStream;

    private java.io.Reader m_reader;

    private GameInformation m_gameInformation;

    private GameTree m_gameTree;

    private Move[][] m_moveBlackCache = new Move[CACHE_SIZE][CACHE_SIZE];

    private Move[][] m_moveWhiteCache = new Move[CACHE_SIZE][CACHE_SIZE];

    private final Move m_passBlackCache = new Move(null, GoColor.BLACK);

    private final Move m_passWhiteCache = new Move(null, GoColor.WHITE);

    private GoPoint[][] m_pointCache = new GoPoint[CACHE_SIZE][CACHE_SIZE];

    private ProgressShow m_progressShow;

    /** Contains strings with warnings. */
    private final TreeSet m_warnings = new TreeSet();

    private StreamTokenizer m_tokenizer;

    private String m_name;

    private String m_newCharset;

    private final StringBuffer m_valueBuffer = new StringBuffer(512);

    private final Vector m_valueVector = new Vector();

    /** Apply some fixes for broken SGF files. */
    private void applyFixes()
    {
        Node root = m_gameTree.getRoot();
        GameInformation gameInformation = m_gameTree.getGameInformation();
        if ((root.getNumberAddWhite() + root.getNumberAddBlack() > 0)
            && root.getPlayer() == GoColor.EMPTY)
        {
            if (gameInformation.m_handicap > 0)
            {
                root.setPlayer(GoColor.WHITE);
            }
            else
            {
                boolean hasBlackChildMoves = false;
                boolean hasWhiteChildMoves = false;
                for (int i = 0; i < root.getNumberChildren(); ++i)
                {
                    Move move = root.getChild(i).getMove();
                    if (move == null)
                        continue;
                    if (move.getColor() == GoColor.BLACK)
                        hasBlackChildMoves = true;
                    if (move.getColor() == GoColor.WHITE)
                        hasWhiteChildMoves = true;
                }
                if (hasBlackChildMoves && ! hasWhiteChildMoves)
                    root.setPlayer(GoColor.BLACK);
                if (hasWhiteChildMoves && ! hasBlackChildMoves)
                    root.setPlayer(GoColor.WHITE);
            }
        }
    }

    /** Check for obsolete long names for standard properties.
        @param property Property name (must have been retrieved with
        String.intern() because comparisons are done with ==
        @return Short standard version of the property or original property
    */
    private String checkForObsoleteLongProps(String property)
    {
        assert(property == property.intern());
        if (property.length() <= 2)
            return property;
        String shortName = null;
        if (property == "ADDBLACK")
            shortName = "AB";
        else if (property == "ADDEMPTY")
            shortName = "AE";
        else if (property == "ADDWHITE")
            shortName = "AW";
        else if (property == "BLACK")
            shortName = "B";
        else if (property == "COMMENT")
            shortName = "C";
        else if (property == "DATE")
            shortName = "DT";
        else if (property == "GAME")
            shortName = "GM";
        else if (property == "HANDICAP")
            shortName = "HA";
        else if (property == "KOMI")
            shortName = "KM";
        else if (property == "PLAYERBLACK")
            shortName = "PB";
        else if (property == "PLAYERWHITE")
            shortName = "PW";
        else if (property == "PLAYER")
            shortName = "PL";
        else if (property == "RESULT")
            shortName = "RE";
        else if (property == "RULES")
            shortName = "RU";
        else if (property == "SIZE")
            shortName = "SZ";
        else if (property == "WHITE")
            shortName = "W";
        if (shortName != null)
        {
            setWarning("Verbose names for standard properties");
            return shortName;
        }
        return property;
    }

    private void findRoot() throws SgfError, IOException
    {
        while (true)
        {
            m_tokenizer.nextToken();
            int t = m_tokenizer.ttype;
            if (t == '(')
            {
                // Better make sure that ( is followed by a node
                m_tokenizer.nextToken();
                t = m_tokenizer.ttype;
                if (t == ';')
                {
                    m_tokenizer.pushBack();
                    return;
                }
                else
                    setWarning("Extra text before SGF tree");
            }
            else if (t == StreamTokenizer.TT_EOF)
                throw getError("No root tree found");
            else
                setWarning("Extra text before SGF tree");
        }
    }

    private SgfError getError(String message)
    {
        int lineNumber = m_tokenizer.lineno();
        if (m_name == null)
            return new SgfError(lineNumber + ": " + message);
        else
        {
            String s = m_name + ":" + lineNumber + ": " + message;
            return new SgfError(s);
        }
    }

    private Move getMove(GoPoint point, GoColor color)
    {
        if (point == null)
        {
            if (color == GoColor.BLACK)
                return m_passBlackCache;
            else
            {
                assert(color == GoColor.WHITE);
                return m_passWhiteCache;
            }
        }
        int x = point.getX();
        int y = point.getY();
        if (x < CACHE_SIZE && y < CACHE_SIZE)
        {
            if (color == GoColor.BLACK)
            {
                if (m_moveBlackCache[x][y] == null)
                    m_moveBlackCache[x][y] = new Move(point, color);
                return m_moveBlackCache[x][y];
            }
            else
            {
                assert(color == GoColor.WHITE);
                if (m_moveWhiteCache[x][y] == null)
                    m_moveWhiteCache[x][y] = new Move(point, color);
                return m_moveWhiteCache[x][y];
            }
        }
        else
            return new Move(point, color);
    }

    private GoColor parseColor(String s) throws SgfError
    {
        GoColor color;
        s = s.trim().toLowerCase();
        if (s.equals("b") || s.equals("1"))
            color = GoColor.BLACK;
        else if (s.equals("w") || s.equals("2"))
            color = GoColor.WHITE;
        else
            throw getError("Invalid color value");
        return color;
    }

    private int parseInt(String s) throws SgfError
    {
        int i = -1;
        try
        {
            i = Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            throw getError("Number expected");
        }
        return i;
    }

    private void parseOverTimeProp(String value)
    {
        /* Used by SgfWriter */
        if (parseOverTimeProp(value,
                              "\\s*(\\d+)\\s*moves\\s*/\\s*(\\d+)\\s*sec\\s*",
                              true, 1000L))
            return;
        /* Used by Smart Go */
        if (parseOverTimeProp(value,
                              "\\s*(\\d+)\\s*moves\\s*/\\s*(\\d+)\\s*min\\s*",
                              true, 60000L))
            return;
        /* Used by Kiseido Game Server, CGoban 2 */
        if (parseOverTimeProp(value,
                              "\\s*(\\d+)x(\\d+)\\s*byo-yomi\\s*",
                              true, 1000L))
            return;
        /* Used Quarry, CGoban 2*/
        if (parseOverTimeProp(value,
                              "\\s*(\\d+)/(\\d+)\\s*canadian\\s*",
                              true, 1000L))
            return;
        m_ignoreTimeSettings = true;
    }

    private boolean parseOverTimeProp(String value, String regex,
                                      boolean byoyomiMovesFirst,
                                      long timeUnitFactor)
    {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches())
        {
            assert(matcher.groupCount() == 2);
            try
            {
                String group1;
                String group2;
                if (byoyomiMovesFirst)
                {
                    group1 = matcher.group(1);
                    group2 = matcher.group(2);
                }
                else
                {
                    group1 = matcher.group(2);
                    group2 = matcher.group(1);
                }
                m_byoyomiMoves = Integer.parseInt(group1);
                m_byoyomi
                    = (long)(Double.parseDouble(group2) * timeUnitFactor);
            }
            catch (NumberFormatException e)
            {
                setWarning("Invalid byoyomi values");
                m_ignoreTimeSettings = true;
                return false;
            }
        }
        else
            return false;
        return true;
    }

    private GoPoint parsePoint(String s) throws SgfError
    {
        s = s.trim().toLowerCase();
        if (s.equals(""))
            return null;
        if (s.length() < 2)
            throw getError("Invalid coordinates: " + s);
        int boardSize = m_gameInformation.m_boardSize;
        if (s.equals("tt") && boardSize <= 19)
            return null;
        int x = s.charAt(0) - 'a';
        int y = boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
        {
            if (x == boardSize && y == -1)
            {
                setWarning("Non-standard pass move encoding");
                return null;
            }
            throw getError("Invalid coordinates: " + s);
        }
        if (x < CACHE_SIZE && y < CACHE_SIZE)
        {
            if (m_pointCache[x][y] == null)
                m_pointCache[x][y] = new GoPoint(x, y);
            return m_pointCache[x][y];
        }
        else
            return new GoPoint(x, y);
    }

    private Node readNext(Node father, boolean isRoot)
        throws IOException, SgfError, SgfCharsetChanged
    {
        if (m_progressShow != null)
        {
            int percent;
            if (m_size > 0)
            {
                long count = m_byteCountInputStream.getCount();
                percent = (int)(count * 100 / m_size);
            }
            else
                percent = 100;
            if (percent != m_lastPercent)
                m_progressShow.showProgress(percent);
            m_lastPercent = percent;
        }
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == '(')
        {
            Node node = father;
            while (node != null)
                node = readNext(node, false);
            return father;
        }
        if (ttype == ')')
            return null;
        if (ttype == StreamTokenizer.TT_EOF)
        {
            setWarning("Game tree not closed");
            return null;
        }
        if (ttype != ';')
            throw getError("Next node expected");
        Node son = new Node();
        father.append(son);
        while (readProp(son, isRoot));
        return son;
    }
    
    private boolean readProp(Node node, boolean isRoot)
        throws IOException, SgfError, SgfCharsetChanged
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == StreamTokenizer.TT_WORD)
        {
            // Use intern() to allow fast comparsion with ==
            String p = m_tokenizer.sval.toUpperCase().intern();
            m_valueVector.clear();
            String s;
            while ((s = readValue()) != null)
                m_valueVector.add(s);
            if (m_valueVector.size() == 0)
                throw getError("Property '" + p + "' has no value");
            String v = (String)m_valueVector.get(0);
            p = checkForObsoleteLongProps(p);
            if (p == "AB")
            {
                for (int i = 0; i < m_valueVector.size(); ++i)
                    node.addBlack(parsePoint((String)m_valueVector.get(i)));
                m_sizeFixed = true;
            }
            else if (p == "AE")
            {
                throw getError("Add empty not supported");
            }
            else if (p == "AW")
            {
                for (int i = 0; i < m_valueVector.size(); ++i)
                    node.addWhite(parsePoint((String)m_valueVector.get(i)));
                m_sizeFixed = true;
            }
            else if (p == "B")
            {
                node.setMove(getMove(parsePoint(v), GoColor.BLACK));
                m_sizeFixed = true;
            }
            else if (p == "BL")
            {
                try
                {
                    node.setTimeLeftBlack(Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "BR")
                m_gameInformation.m_blackRank = v;
            else if (p == "C")
            {
                String comment;
                if (node.getComment() == null)
                    comment = v.trim();
                else
                    comment = node.getComment() + "\n" + v.trim();
                node.setComment(comment);
            }
            else if (p == "CA")
            {
                if (isRoot && m_isFile && m_newCharset == null)
                {
                    m_newCharset = v.trim();
                    if (Charset.isSupported(m_newCharset))
                        throw new SgfCharsetChanged();
                }
            }
            else if (p == "DT")
            {
                m_gameInformation.m_date = v;
            }
            else if (p == "FF")
            {
                int format = -1;
                try
                {
                    format = Integer.parseInt(v);
                }
                catch (NumberFormatException e)
                {
                }
                if (format < 1 || format > 4)
                    setWarning("Unknown SGF file format version");
            }
            else if (p == "GM")
            {
                v = v.trim();
                if (v.equals(""))
                    setWarning("Empty value for game type");
                else if (! v.equals("1"))
                    throw getError("Not a Go game");
                
            }
            else if (p == "HA")
            {
                try
                {
                    m_gameInformation.m_handicap = Integer.parseInt(v);
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid handicap value");
                }
            }
            else if (p == "KM")
            {
                try
                {
                    m_gameInformation.m_komi = Double.parseDouble(v);
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid value for komi");
                }
            }
            else if (p == "OB")
            {
                try
                {
                    node.setMovesLeftBlack(Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "OM")
            {
                try
                {
                    m_byoyomiMoves = Integer.parseInt(v);
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid value for byoyomi moves");
                    m_ignoreTimeSettings = true;
                }
            }
            else if (p == "OP")
            {
                try
                {
                    m_byoyomi = (long)(Double.parseDouble(v) * 1000);
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid value for byoyomi time");
                    m_ignoreTimeSettings = true;
                }
            }
            else if (p == "OT")
            {
                parseOverTimeProp(v);
            }
            else if (p == "OW")
            {
                try
                {
                    node.setMovesLeftWhite(Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "PB")
            {
                m_gameInformation.m_playerBlack = v;
            }
            else if (p == "PW")
            {
                m_gameInformation.m_playerWhite = v;
            }
            else if (p == "PL")
            {
                node.setPlayer(parseColor(v));
            }
            else if (p == "RE")
            {
                m_gameInformation.m_result = v;
            }
            else if (p == "RU")
            {
                m_gameInformation.m_rules = v;
            }
            else if (p == "SZ")
            {
                if (! isRoot)
                {
                    if (m_sizeFixed)
                        throw getError("Size property outside root node");
                    setWarning("Size property not in root node");
                }
                try
                {
                    m_gameInformation.m_boardSize = parseInt(v);
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid board size value");
                }
                m_sizeFixed = true;
            }
            else if (p == "TM")
            {
                try
                {
                    m_preByoyomi = (long)(Double.parseDouble(v) * 1000);
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid value for time");
                    m_ignoreTimeSettings = true;
                }
            }
            else if (p == "W")
            {
                node.setMove(getMove(parsePoint(v), GoColor.WHITE));
                m_sizeFixed = true;
            }
            else if (p == "WL")
            {
                try
                {
                    node.setTimeLeftWhite(Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "WR")
                m_gameInformation.m_whiteRank = v;
            else if (p != "FF" && p != "GN" && p != "AP")
                node.addSgfProperty(p, v);
            return true;
        }
        if (ttype != '\n')
            // Don't pushBack newline, will confuse lineno() (Bug 4942853)
            m_tokenizer.pushBack();
        return false;
    }

    private void readSgf(InputStream in, String charset)
        throws SgfError, SgfCharsetChanged
    {
        try
        {
            m_gameInformation = new GameInformation(19);
            m_sizeFixed = false;
            if (m_progressShow != null)
            {
                m_byteCountInputStream = new ByteCountInputStream(in);
                in = m_byteCountInputStream;
            }
            InputStreamReader reader;
            try
            {
                reader = new InputStreamReader(in, charset);
            }
            catch (UnsupportedEncodingException e)
            {
                reader = new InputStreamReader(in);
            }
            m_reader = new BufferedReader(reader);
            m_tokenizer = new StreamTokenizer(m_reader);
            findRoot();
            Node root = new Node();
            Node node = readNext(root, true);
            while (node != null)
                node = readNext(node, false);
            if (root.getNumberChildren() == 1)
            {
                root = root.getChild();
                root.setFather(null);
            }
            setTimeSettings();
            m_gameTree = new GameTree(m_gameInformation, root);
            applyFixes();
        }
        catch (FileNotFoundException e)
        {
            throw new SgfError("File not found");
        }
        catch (IOException e)
        {
            throw new SgfError("IO error");
        }
        catch (OutOfMemoryError e)
        {
            throw new SgfError("Out of memory");
        }
    }

    private String readValue() throws IOException, SgfError
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype != '[')
        {
            if (ttype != '\n')
                // Don't pushBack newline, will confuse lineno() (Bug 4942853)
                m_tokenizer.pushBack();
            return null;
        }
        m_valueBuffer.setLength(0);
        boolean quoted = false;
        while (true)
        {
            int c = m_reader.read();
            if (c < 0)
                throw getError("Property value incomplete");
            if (! quoted && c == ']')
                break;
            quoted = (c == '\\');
            if (! quoted)
                m_valueBuffer.append((char)c);
        }
        return m_valueBuffer.toString();
    }

    private void setTimeSettings()
    {
        if (m_ignoreTimeSettings || m_preByoyomi <= 0)
            return;
        if (m_byoyomi <= 0 || m_byoyomiMoves <= 0)
        {
            m_gameInformation.m_timeSettings = new TimeSettings(m_preByoyomi);
            return;
        }
        m_gameInformation.m_timeSettings
            = new TimeSettings(m_preByoyomi, m_byoyomi, m_byoyomiMoves);
    }

    private void setWarning(String message)
    {
        m_warnings.add(message);
    }
}

//----------------------------------------------------------------------------