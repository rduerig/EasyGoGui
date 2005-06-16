//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

//----------------------------------------------------------------------------

/** Time settings.
    Unit is milliseconds.
*/
public final class TimeSettings
{
    public TimeSettings(long totalTime)
    {
        assert(totalTime > 0);
        m_preByoyomi = totalTime;
        m_byoyomi = 0;
        m_byoyomiMoves = -1;
    }

    public TimeSettings(long preByoyomi, long byoyomi, int byoyomiMoves)
    {
        assert(preByoyomi > 0);
        assert(byoyomi > 0);
        assert(byoyomiMoves > 0);
        m_preByoyomi = preByoyomi;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
    }

    public TimeSettings(TimeSettings timeSettings)
    {
        m_preByoyomi = timeSettings.m_preByoyomi;
        m_byoyomi = timeSettings.m_byoyomi;
        m_byoyomiMoves = timeSettings.m_byoyomiMoves;
    }

    public long getByoyomi()
    {
        assert(getUseByoyomi());
        return m_byoyomi;
    }

    public int getByoyomiMoves()
    {
        assert(getUseByoyomi());
        return m_byoyomiMoves;
    }

    public long getPreByoyomi()
    {
        return m_preByoyomi;
    }

    public boolean getUseByoyomi()
    {
        return (m_byoyomiMoves > 0);
    }

    public static TimeSettings parse(String s) throws Error
    {
        boolean useByoyomi = false;
        long preByoyomi = 0;
        long byoyomi = 0;
        int byoyomiMoves = 0;
        try
        {
            int idx = s.indexOf('+');
            if (idx < 0)
            {
                preByoyomi = Long.parseLong(s) * 60000L;
            }
            else
            {
                useByoyomi = true;
                preByoyomi = Long.parseLong(s.substring(0, idx)) * 60000L;
                int idx2 = s.indexOf('/');
                if (idx2 <= idx)
                    throw new Error("Invalid time specification");
                byoyomi = Long.parseLong(s.substring(idx + 1, idx2)) * 60000L;
                byoyomiMoves = Integer.parseInt(s.substring(idx2 + 1));
            }
        }
        catch (NumberFormatException e)
        {
            throw new Error("Invalid time specification");
        }
        if (preByoyomi <= 0)
            throw new Error("Pre byoyomi time must be positive");
        if (byoyomi <= 0)
            throw new Error("Byoyomi time must be positive");
        if (byoyomiMoves <= 0)
            throw new Error("Moves for byoyomi time must be positive");
        if (useByoyomi)
            return new TimeSettings(preByoyomi, byoyomi, byoyomiMoves);
        else
            return new TimeSettings(preByoyomi);
    }

    private final long m_preByoyomi;

    private final long m_byoyomi;

    private final int m_byoyomiMoves;
}

//----------------------------------------------------------------------------