package com.example.ha;

import org.junit.Assert;
import org.junit.Test;

public final class HaVerseClassifierTest {
    @Test
    public void matchesAllVerseNamesAndKeepsPlusSuffix() {
        assertMatch("ヴァース(小)+", HaVerseClassifier.Kind.SMALL, "ヴァース(小)+");
        assertMatch("ヴァース(小)", HaVerseClassifier.Kind.SMALL, "ヴァース(小)");
        assertMatch("ヴァース(中)+", HaVerseClassifier.Kind.MEDIUM, "ヴァース(中)+");
        assertMatch("ヴァース(中)", HaVerseClassifier.Kind.MEDIUM, "ヴァース(中)");
        assertMatch("ヴァース(大)+", HaVerseClassifier.Kind.LARGE, "ヴァース(大)+");
        assertMatch("ヴァース(大)", HaVerseClassifier.Kind.LARGE, "ヴァース(大)");
        assertMatch("ヴァース(特大)+", HaVerseClassifier.Kind.EXTRA_LARGE, "ヴァース(特大)+");
        assertMatch("ヴァース(特大)", HaVerseClassifier.Kind.EXTRA_LARGE, "ヴァース(特大)");
    }

    @Test
    public void prefersPlusCandidateBeforeShorterCandidate() {
        HaVerseClassifier.Result result = HaVerseClassifier.classify("\u00a7bヴァース(特大)+");
        Assert.assertNotNull(result);
        Assert.assertEquals("ヴァース(特大)+", result.displayName);
        Assert.assertTrue(result.isProtectable());
    }

    @Test
    public void ignoresUnrelatedNames() {
        Assert.assertNull(HaVerseClassifier.classify("ビーコン"));
        Assert.assertNull(HaVerseClassifier.classify(""));
        Assert.assertNull(HaVerseClassifier.classify(null));
    }

    private static void assertMatch(String name, HaVerseClassifier.Kind kind, String displayName) {
        HaVerseClassifier.Result result = HaVerseClassifier.classify(name);
        Assert.assertNotNull(result);
        Assert.assertEquals(kind, result.kind);
        Assert.assertEquals(displayName, result.displayName);
    }
}
