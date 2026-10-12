package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.mudkip.moememos.data.api.ListMemosResponse
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.api.MemosV1CreateMemoRequest
import me.mudkip.moememos.data.api.MemosV1Memo
import me.mudkip.moememos.data.api.UpdateMemoRequest
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemoLocation
import me.mudkip.moememos.data.model.MemoLocationChange
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.MemosAccount
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class MemosV1LocationTest {
    private val location = MemoLocation("Home", -1.234567, 2.345678)
    private var createRequest: MemosV1CreateMemoRequest? = null
    private var updateRequest: UpdateMemoRequest? = null
    private var updateMask: String? = null
    private val api = Proxy.newProxyInstance(MemosV1Api::class.java.classLoader, arrayOf(MemosV1Api::class.java)) { _, method, args ->
        when (method.name) {
            "createMemo" -> {
                createRequest = args[0] as MemosV1CreateMemoRequest
                ApiResponse.Success(MemosV1Memo(name = "memos/a", location = createRequest?.location))
            }
            "updateMemo" -> {
                updateRequest = args[1] as UpdateMemoRequest
                updateMask = args[2] as String?
                ApiResponse.Success(MemosV1Memo(name = "memos/a", location = updateRequest?.location))
            }
            "listMemos" -> ApiResponse.Success(ListMemosResponse(listOf(MemosV1Memo(name = "memos/a", location = location)), null))
            else -> error("Unexpected call: ${method.name}")
        }
    } as MemosV1Api
    private val repository = MemosV1Repository(api, Account.MemosV1(MemosAccount(remoteIdentifier = "users/a")))

    @Test fun createAndPullPreserveLocation() = runTest {
        val created = repository.createMemo("note", MemoVisibility.PRIVATE, emptyList(), location = location)
        assertEquals(location, createRequest?.location)
        assertEquals(location, (created as ApiResponse.Success).data.location)
        assertEquals(location, (repository.listMemos() as ApiResponse.Success).data.single().location)
    }

    @Test fun removingLocationUsesAnExplicitMaskDespiteOmittedNulls() = runTest {
        repository.updateMemo("memos/a", content = "changed", pinned = false, resourceRemoteIds = emptyList(), location = MemoLocationChange(null))
        assertNull(updateRequest?.location)
        assertEquals(setOf("content", "pinned", "attachments", "update_time", "location"), updateMask!!.split(',').toSet())
        val networkJson = Json { explicitNulls = false }
        assertFalse(networkJson.encodeToString(requireNotNull(updateRequest)).contains("\"location\""))
        assertTrue(networkJson.encodeToString(requireNotNull(updateRequest)).contains("\"pinned\":false"))
    }

    @Test fun locationEditsUseMaskWhileUnrelatedUpdatesLeaveLocationAlone() = runTest {
        repository.updateMemo("memos/a", location = MemoLocationChange(location))
        assertEquals(location, updateRequest?.location)
        assertEquals(setOf("update_time", "location"), updateMask!!.split(',').toSet())
        repository.updateMemo("memos/a", pinned = true)
        assertNull(updateRequest?.location)
        assertNull(updateMask)
    }
}
