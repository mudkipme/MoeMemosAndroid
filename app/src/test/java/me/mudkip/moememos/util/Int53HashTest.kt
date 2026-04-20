package me.mudkip.moememos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Int53HashTest {
    @Test
    fun toInt53Hash_isStableForKnownMemosV1Identifiers() {
        assertEquals(6855389868995905L, "users/123".toInt53Hash())
        assertEquals(4553422447568558L, "users/default".toInt53Hash())
        assertEquals(4044015508077841L, "users/jane.doe".toInt53Hash())
    }

    @Test
    fun toInt53Hash_returnsPositive53BitValue() {
        val hash = "users/default".toInt53Hash()

        assertTrue(hash >= 0L)
        assertTrue(hash <= 0x1FFFFFFFFFFFFFL)
    }

    @Test
    fun toInt53Hash_changesWhenIdentifierChanges() {
        assertNotEquals("users/default".toInt53Hash(), "users/default-2".toInt53Hash())
    }
}
