package dev.andikune.keyboardio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeKeyGeometryTest {
    private val width = 1000f
    private val rowHeight = 50f

    @Test fun mapsTopRow() {
        assertEquals('q', SwipeKeyGeometry.letterAt(50f, 25f, width, rowHeight))
        assertEquals('p', SwipeKeyGeometry.letterAt(950f, 25f, width, rowHeight))
    }

    @Test fun mapsIndentedMiddleRow() {
        assertNull(SwipeKeyGeometry.letterAt(25f, 75f, width, rowHeight))
        assertEquals('a', SwipeKeyGeometry.letterAt(100f, 75f, width, rowHeight))
        assertEquals('l', SwipeKeyGeometry.letterAt(925f, 75f, width, rowHeight))
    }

    @Test fun mapsBottomRowWithWideActionKeys() {
        assertNull(SwipeKeyGeometry.letterAt(50f, 125f, width, rowHeight))
        assertEquals('z', SwipeKeyGeometry.letterAt(200f, 125f, width, rowHeight))
        assertEquals('m', SwipeKeyGeometry.letterAt(800f, 125f, width, rowHeight))
        assertNull(SwipeKeyGeometry.letterAt(950f, 125f, width, rowHeight))
    }

    @Test fun rejectsInvalidAndOutOfBoundsCoordinates() {
        assertNull(SwipeKeyGeometry.letterAt(-1f, 25f, width, rowHeight))
        assertNull(SwipeKeyGeometry.letterAt(1000f, 25f, width, rowHeight))
        assertNull(SwipeKeyGeometry.letterAt(500f, 175f, width, rowHeight))
        assertNull(SwipeKeyGeometry.letterAt(500f, 25f, 0f, rowHeight))
    }
    @Test fun scalesWithActualKeyboardWidth() {
        assertEquals('q', SwipeKeyGeometry.letterAt(60f, 30f, 1200f, 60f))
        assertEquals('a', SwipeKeyGeometry.letterAt(120f, 90f, 1200f, 60f))
        assertEquals('z', SwipeKeyGeometry.letterAt(240f, 150f, 1200f, 60f))
    }

}
