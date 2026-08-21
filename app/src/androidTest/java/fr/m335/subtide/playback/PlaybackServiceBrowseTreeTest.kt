package fr.m335.subtide.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [PlaybackService]'s Android Auto browse tree via a real [MediaBrowser] connection.
 * Google's Desktop Head Unit is currently broken by an Android Auto 17.4+ server-mode regression
 * and no physical head unit is available, so this is the only way to verify the tree without a
 * car — see the request-preset kdoc in [PlaybackService] for the platform context.
 *
 * Every [MediaBrowser] call must run on the looper it was built with (the main looper here), so
 * [onMain] hops there for each call before awaiting the resulting future from this test thread.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceBrowseTreeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var browser: MediaBrowser

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    @Before
    fun connect() {
        val context: Context = instrumentation.targetContext
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        browser = MediaBrowser.Builder(context, token).buildAsync().get(10, TimeUnit.SECONDS)
    }

    @After
    fun disconnect() {
        onMain { browser.release() }
    }

    /**
     * This shares the real app's on-disk server config (see class kdoc), so it can't assume
     * either state deterministically — a wiped/never-onboarded device is expected to show the
     * "not configured" fallback instead of the folder tree, which is itself the behavior under
     * test, not a failure.
     */
    @Test
    fun rootReflectsConfigurationState() {
        val root = onMain { browser.getLibraryRoot(null) }.get(10, TimeUnit.SECONDS).value!!
        val children = onMain { browser.getChildren(root.mediaId, 0, 10, null) }.get(10, TimeUnit.SECONDS).value!!
        val ids = children.map { it.mediaId }
        if (ids == listOf("not-configured")) return
        assertTrue("expected a 'history' folder, got $ids", ids.contains("history"))
        assertTrue("expected a 'requests' folder, got $ids", ids.contains("requests"))
    }

    @Test
    fun requestsFolderExposesPlayablePresetShortcuts() {
        val children = onMain { browser.getChildren("requests", 0, 10, null) }.get(10, TimeUnit.SECONDS).value!!
        assertEquals(9, children.size)
        assertTrue(children.all { it.mediaId.startsWith("request:") })
        assertTrue(children.all { it.mediaMetadata.isPlayable == true })
    }

    @Test
    fun tappingAPresetResolvesWithoutCrashingTheSession() {
        val preset = onMain { browser.getItem("request:calmer") }.get(10, TimeUnit.SECONDS).value!!
        // Exercises MediaSession.Callback.onAddMediaItems — the same path Android Auto's
        // "tap to play" sends. No server is configured in this test, so PlaybackService's
        // currentApi stays null and submitPresetRequest() is a no-op (see its early return).
        onMain { browser.setMediaItem(preset) }
        onMain { browser.prepare() }
        val root = onMain { browser.getLibraryRoot(null) }.get(10, TimeUnit.SECONDS).value!!
        assertEquals("root", root.mediaId)
    }
}
