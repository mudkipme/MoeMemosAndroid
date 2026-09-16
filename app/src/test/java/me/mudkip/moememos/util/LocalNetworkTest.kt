package me.mudkip.moememos.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class LocalNetworkTest {
    @Test
    fun privateAndLinkLocalAddressesRequirePermission() {
        listOf("10.0.0.1", "172.16.0.1", "172.31.255.254", "192.168.1.1", "169.254.1.1",
            "fc00::1", "fd12:3456::1", "fe80::1").forEach {
            assertTrue(it, isLocalNetworkAddress(InetAddress.getByName(it)))
        }
    }

    @Test
    fun publicAndLoopbackAddressesDoNotRequirePermission() {
        listOf("8.8.8.8", "172.15.0.1", "172.32.0.1", "127.0.0.1", "::1", "2001:4860:4860::8888").forEach {
            assertFalse(it, isLocalNetworkAddress(InetAddress.getByName(it)))
        }
    }

    @Test
    fun localNamesAreDetectedBeforeMdnsResolution() = runTest {
        assertTrue(usesLocalNetwork("http://memos.local:5230"))
        assertTrue(usesLocalNetwork("https://MEMOS.LOCAL./memos"))
        assertTrue(usesLocalNetwork("http://192.168.1.2:5230"))
        assertTrue(usesLocalNetwork("https://[fd12:3456::1]"))
        assertFalse(usesLocalNetwork("http://127.0.0.1:5230"))
        assertFalse(usesLocalNetwork("invalid host"))
    }
}
