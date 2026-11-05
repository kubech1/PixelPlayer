package com.theveloper.pixelplay.ui.glancewidget

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WidgetBitmapPolicyTest {
    @Test fun aggregateAllocationFitsAllSizeBuckets() {
        for (buckets in listOf(0, 1, 2, 16, 1000)) for (slots in listOf(1,5)) {
            val budget = WidgetBitmapBudget(4 * 1024 * 1024, buckets, artworkSlots = slots)
            val used = (budget.artworkBytes.toLong() * slots + budget.backgroundBytes + budget.progressBytes) * buckets.coerceAtLeast(2)
            assertThat(used).isAtMost(budget.payloadBytes * 9 / 10)
        }
    }
    @Test fun hugeAndNarrowArtworkStayWithinBudgetWithoutUpscaling() {
        for ((w,h) in listOf(20000 to 20000, 20000 to 1, 1 to 20000, 79 to 53)) {
            for (bytes in listOf(4, 1024, 100_000)) {
                val size = checkNotNull(fitWidgetImage(w,h,bytes))
                assertThat(size.first.toLong() * size.second * 4).isAtMost(bytes.toLong())
                assertThat(size.first).isAtMost(w)
                assertThat(size.second).isAtMost(h)
            }
        }
    }
    @Test fun absentBudgetOrInvalidDimensionsRequirePlaceholder() {
        assertThat(fitWidgetImage(100,100,3)).isNull()
        assertThat(fitWidgetImage(0,100,1000)).isNull()
        assertThat(fitWidgetImage(Int.MAX_VALUE,Int.MAX_VALUE,4096)).isNotNull()
    }
}
