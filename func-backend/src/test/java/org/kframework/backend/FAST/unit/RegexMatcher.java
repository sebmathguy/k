// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST.unit;

import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.Description;

/**
 * @author Sebastian Conybeare
 */

public class RegexMatcher extends TypeSafeMatcher<String> {

    private final String regex;

    public RegexMatcher(final String regex) {
        this.regex = regex;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("matches regex=\"" + regex + "\"");
    }

    @Override
    public boolean matchesSafely(final String string) {
        return string.matches(regex);
    }

    public static RegexMatcher matchesRegex(final String regex) {
        return new RegexMatcher(regex);
    }
}
