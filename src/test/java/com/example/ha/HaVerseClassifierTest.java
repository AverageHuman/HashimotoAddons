package com.example.ha;

import org.junit.Assert;
import org.junit.Test;

public final class HaVerseClassifierTest {
    @Test
    public void matchesAllVerseNamesAndKeepsPlusSuffix() {
        assertMatch("ヴァース(小)+", HaVerseClassifier.Kind.SMALL, 1, "ヴァース(小)+");
        assertMatch("ヴァース(小)", HaVerseClassifier.Kind.SMALL, 1, "ヴァース(小)");
        assertMatch("ヴァース(中)+", HaVerseClassifier.Kind.MEDIUM, 2, "ヴァース(中)+");
        assertMatch("ヴァース(中)", HaVerseClassifier.Kind.MEDIUM, 2, "ヴァース(中)");
        assertMatch("ヴァース(大)+", HaVerseClassifier.Kind.LARGE, 3, "ヴァース(大)+");
        assertMatch("ヴァース(大)", HaVerseClassifier.Kind.LARGE, 3, "ヴァース(大)");
        assertMatch("ヴァース(特大)+", HaVerseClassifier.Kind.EXTRA_LARGE, 4, "ヴァース(特大)+");
        assertMatch("ヴァース(特大)", HaVerseClassifier.Kind.EXTRA_LARGE, 4, "ヴァース(特大)");
    }

    @Test
    public void prefersPlusCandidateBeforeShorterCandidate() {
        HaVerseClassifier.Result result = HaVerseClassifier.classify("\u00a7bヴァース(特大)+");
        Assert.assertNotNull(result);
        Assert.assertEquals("ヴァース(特大)+", result.displayName);
        Assert.assertTrue(result.isProtectable());
        Assert.assertEquals(4, result.overlayNumber());
    }

    @Test
    public void ignoresUnrelatedNames() {
        Assert.assertNull(HaVerseClassifier.classify("ビーコン"));
        Assert.assertNull(HaVerseClassifier.classify(""));
        Assert.assertNull(HaVerseClassifier.classify(null));
    }

    private static void assertMatch(String name, HaVerseClassifier.Kind kind, int overlayNumber, String displayName) {
        HaVerseClassifier.Result result = HaVerseClassifier.classify(name);
        Assert.assertNotNull(result);
        Assert.assertEquals(kind, result.kind);
        Assert.assertEquals(overlayNumber, result.overlayNumber());
        Assert.assertEquals(displayName, result.displayName);
    }
}
