package com.bbva.kyof.vega.util;

import java.util.regex.Pattern;

/**
 * Pattern with equals to be used on hashmaps
 */
public class PatternEquals
{
    /**
     * Original pattern wrapped
     */
    private final Pattern origPattern;

    /**
     * Constructor
     */
    public PatternEquals(Pattern origPattern)
    {
        this.origPattern = origPattern;
    }

    /**
     * Compiles the given regular expression and attempts to match the given
     * input against it.
     *
     * @param input The input sequence
     * @return true if it matches TOTALLY, false otherwise
     */
    public boolean matches(CharSequence input)
    {
        return origPattern.matcher(input).matches();
    }

    /**
     * Returns hashcode
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        return origPattern.hashCode();
    }

    /**
     * Java equals
     */
    @Override
    public boolean equals(Object o)
    {
        boolean ret = false;
        if (o instanceof PatternEquals)
        {
            return origPattern.pattern().equals(((PatternEquals) o).origPattern.pattern());
        }
        return ret;
    }
}
