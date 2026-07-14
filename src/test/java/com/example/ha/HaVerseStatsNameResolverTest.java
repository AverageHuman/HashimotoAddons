package com.example.ha;

import org.junit.Assert;
import org.junit.Test;

public final class HaVerseStatsNameResolverTest {
    @Test
    public void resolvesAllCanonicalVerseNames() {
        assertResolved("ヴァース(極小)");
        assertResolved("ヴァース(極小)+");
        assertResolved("ヴァース(小)");
        assertResolved("ヴァース(小)+");
        assertResolved("ヴァース(中)");
        assertResolved("ヴァース(中)+");
        assertResolved("ヴァース(大)");
        assertResolved("ヴァース(大)+");
        assertResolved("ヴァース(特大)");
        assertResolved("ヴァース(特大)+");
    }

    @Test
    public void extractsCanonicalNameFromDecoratedDisplayName() {
        Assert.assertEquals(
            "ヴァース(小)",
            HaVerseStatsNameResolver.resolve("Lv3氷属性防御 超技撃 ヴァース(小)")
        );
        Assert.assertEquals(
            "ヴァース(特大)+",
            HaVerseStatsNameResolver.resolve("前置き \u00a7bヴァース(特大)+\u00a7r 後置き")
        );
    }

    @Test
    public void keepsPlusSuffixWhenShorterCandidateAlsoMatches() {
        Assert.assertEquals("ヴァース(極小)+", HaVerseStatsNameResolver.resolve("ヴァース(極小)+"));
        Assert.assertEquals("ヴァース(小)+", HaVerseStatsNameResolver.resolve("ヴァース(小)+"));
        Assert.assertEquals("ヴァース(中)+", HaVerseStatsNameResolver.resolve("ヴァース(中)+"));
        Assert.assertEquals("ヴァース(大)+", HaVerseStatsNameResolver.resolve("ヴァース(大)+"));
        Assert.assertEquals("ヴァース(特大)+", HaVerseStatsNameResolver.resolve("ヴァース(特大)+"));
    }

    @Test
    public void ignoresNamesWithoutCanonicalVerseToken() {
        Assert.assertEquals("", HaVerseStatsNameResolver.resolve("ビーコン"));
        Assert.assertEquals("", HaVerseStatsNameResolver.resolve("ヴァース"));
        Assert.assertEquals("", HaVerseStatsNameResolver.resolve(""));
        Assert.assertEquals("", HaVerseStatsNameResolver.resolve(null));
    }

    private static void assertResolved(String canonicalName) {
        Assert.assertEquals(canonicalName, HaVerseStatsNameResolver.resolve(canonicalName));
    }
}
