package com.example.gemma.rag

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class LocalVectorStoreTest {

    // Simple mock context that provides a temporary directory for filesDir
    private val mockContext = object : android.content.Context() {
        override fun getFilesDir(): File {
            val dir = File(System.getProperty("java.io.tmpdir"), "rag_test_${UUID.randomUUID()}")
            dir.mkdirs()
            return dir
        }
        // Throw NotImplementedError for everything else
        override fun getAssets() = throw NotImplementedError()
        override fun getResources() = throw NotImplementedError()
        override fun getPackageManager() = throw NotImplementedError()
        override fun getContentResolver() = throw NotImplementedError()
        override fun getMainLooper() = throw NotImplementedError()
        override fun getApplicationContext() = throw NotImplementedError()
        override fun setTheme(resid: Int) = throw NotImplementedError()
        override fun getTheme() = throw NotImplementedError()
        override fun getClassLoader() = throw NotImplementedError()
        override fun getPackageName() = throw NotImplementedError()
        override fun getApplicationInfo() = throw NotImplementedError()
        override fun getPackageResourcePath() = throw NotImplementedError()
        override fun getPackageCodePath() = throw NotImplementedError()
        override fun getSharedPreferences(name: String?, mode: Int) = throw NotImplementedError()
        override fun moveSharedPreferencesFrom(sourceContext: android.content.Context?, name: String?) = throw NotImplementedError()
        override fun deleteSharedPreferences(name: String?) = throw NotImplementedError()
        override fun openFileInput(name: String?) = throw NotImplementedError()
        override fun openFileOutput(name: String?, mode: Int) = throw NotImplementedError()
        override fun deleteFile(name: String?) = throw NotImplementedError()
        override fun getFileStreamPath(name: String?) = throw NotImplementedError()
        override fun getDataDir() = throw NotImplementedError()
        override fun getNoBackupFilesDir() = throw NotImplementedError()
        override fun getExternalFilesDir(type: String?) = throw NotImplementedError()
        override fun getExternalFilesDirs(type: String?) = throw NotImplementedError()
        override fun getObbDir() = throw NotImplementedError()
        override fun getObbDirs() = throw NotImplementedError()
        override fun getCacheDir() = throw NotImplementedError()
        override fun getCodeCacheDir() = throw NotImplementedError()
        override fun getExternalCacheDir() = throw NotImplementedError()
        override fun getExternalCacheDirs() = throw NotImplementedError()
        override fun getExternalMediaDirs() = throw NotImplementedError()
        override fun fileList() = throw NotImplementedError()
        override fun getDir(name: String?, mode: Int) = throw NotImplementedError()
        override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?) = throw NotImplementedError()
        override fun openOrCreateDatabase(name: String?, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?) = throw NotImplementedError()
        override fun moveDatabaseFrom(sourceContext: android.content.Context?, name: String?) = throw NotImplementedError()
        override fun deleteDatabase(name: String?) = throw NotImplementedError()
        override fun getDatabasePath(name: String?) = throw NotImplementedError()
        override fun databaseList() = throw NotImplementedError()
        override fun getWallpaper() = throw NotImplementedError()
        override fun peekWallpaper() = throw NotImplementedError()
        override fun getWallpaperDesiredMinimumWidth() = throw NotImplementedError()
        override fun getWallpaperDesiredMinimumHeight() = throw NotImplementedError()
        override fun setWallpaper(bitmap: android.graphics.Bitmap?) = throw NotImplementedError()
        override fun setWallpaper(data: java.io.InputStream?) = throw NotImplementedError()
        override fun clearWallpaper() = throw NotImplementedError()
        override fun startActivity(intent: android.content.Intent?) = throw NotImplementedError()
        override fun startActivity(intent: android.content.Intent?, options: android.os.Bundle?) = throw NotImplementedError()
        override fun startActivities(intents: Array<out android.content.Intent>?) = throw NotImplementedError()
        override fun startActivities(intents: Array<out android.content.Intent>?, options: android.os.Bundle?) = throw NotImplementedError()
        override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int) = throw NotImplementedError()
        override fun startIntentSender(intent: android.content.IntentSender?, fillInIntent: android.content.Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: android.os.Bundle?) = throw NotImplementedError()
        override fun sendBroadcast(intent: android.content.Intent?) = throw NotImplementedError()
        override fun sendBroadcast(intent: android.content.Intent?, receiverPermission: String?) = throw NotImplementedError()
        override fun sendOrderedBroadcast(intent: android.content.Intent?, receiverPermission: String?) = throw NotImplementedError()
        override fun sendOrderedBroadcast(intent: android.content.Intent, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw NotImplementedError()
        override fun sendBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = throw NotImplementedError()
        override fun sendBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?) = throw NotImplementedError()
        override fun sendOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, receiverPermission: String?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw NotImplementedError()
        override fun sendStickyBroadcast(intent: android.content.Intent?) = throw NotImplementedError()
        override fun sendStickyOrderedBroadcast(intent: android.content.Intent?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw NotImplementedError()
        override fun removeStickyBroadcast(intent: android.content.Intent?) = throw NotImplementedError()
        override fun sendStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = throw NotImplementedError()
        override fun sendStickyOrderedBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?, resultReceiver: android.content.BroadcastReceiver?, scheduler: android.os.Handler?, initialCode: Int, initialData: String?, initialExtras: android.os.Bundle?) = throw NotImplementedError()
        override fun removeStickyBroadcastAsUser(intent: android.content.Intent?, user: android.os.UserHandle?) = throw NotImplementedError()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?) = throw NotImplementedError()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, flags: Int) = throw NotImplementedError()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?) = throw NotImplementedError()
        override fun registerReceiver(receiver: android.content.BroadcastReceiver?, filter: android.content.IntentFilter?, broadcastPermission: String?, scheduler: android.os.Handler?, flags: Int) = throw NotImplementedError()
        override fun unregisterReceiver(receiver: android.content.BroadcastReceiver?) = throw NotImplementedError()
        override fun startService(service: android.content.Intent?) = throw NotImplementedError()
        override fun startForegroundService(service: android.content.Intent?) = throw NotImplementedError()
        override fun stopService(name: android.content.Intent?) = throw NotImplementedError()
        override fun bindService(service: android.content.Intent, conn: android.content.ServiceConnection, flags: Int) = throw NotImplementedError()
        override fun unbindService(conn: android.content.ServiceConnection) = throw NotImplementedError()
        override fun startInstrumentation(className: android.content.ComponentName, profileFile: String?, arguments: android.os.Bundle?) = throw NotImplementedError()
        override fun getSystemService(name: String) = throw NotImplementedError()
        override fun getSystemServiceName(serviceClass: Class<*>) = throw NotImplementedError()
        override fun checkPermission(permission: String, pid: Int, uid: Int) = throw NotImplementedError()
        override fun checkCallingPermission(permission: String) = throw NotImplementedError()
        override fun checkCallingOrSelfPermission(permission: String) = throw NotImplementedError()
        override fun checkSelfPermission(permission: String) = throw NotImplementedError()
        override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) = throw NotImplementedError()
        override fun enforceCallingPermission(permission: String, message: String?) = throw NotImplementedError()
        override fun enforceCallingOrSelfPermission(permission: String, message: String?) = throw NotImplementedError()
        override fun grantUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) = throw NotImplementedError()
        override fun revokeUriPermission(uri: android.net.Uri?, modeFlags: Int) = throw NotImplementedError()
        override fun revokeUriPermission(toPackage: String?, uri: android.net.Uri?, modeFlags: Int) = throw NotImplementedError()
        override fun checkUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int) = throw NotImplementedError()
        override fun checkCallingUriPermission(uri: android.net.Uri?, modeFlags: Int) = throw NotImplementedError()
        override fun checkCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int) = throw NotImplementedError()
        override fun checkUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int) = throw NotImplementedError()
        override fun enforceUriPermission(uri: android.net.Uri?, pid: Int, uid: Int, modeFlags: Int, message: String?) = throw NotImplementedError()
        override fun enforceCallingUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) = throw NotImplementedError()
        override fun enforceCallingOrSelfUriPermission(uri: android.net.Uri?, modeFlags: Int, message: String?) = throw NotImplementedError()
        override fun enforceUriPermission(uri: android.net.Uri?, readPermission: String?, writePermission: String?, pid: Int, uid: Int, modeFlags: Int, message: String?) = throw NotImplementedError()
        override fun createPackageContext(packageName: String?, flags: Int) = throw NotImplementedError()
        override fun createContextForSplit(splitName: String?) = throw NotImplementedError()
        override fun createConfigurationContext(overrideConfiguration: android.content.res.Configuration) = throw NotImplementedError()
        override fun createDisplayContext(display: android.view.Display) = throw NotImplementedError()
        override fun createDeviceProtectedStorageContext() = throw NotImplementedError()
        override fun isDeviceProtectedStorage() = throw NotImplementedError()
    }
    
    private lateinit var vectorStore: LocalVectorStore

    @Before
    fun setup() {
        vectorStore = LocalVectorStore(mockContext)
    }

    @Test
    fun testEmptyStore() = runBlocking {
        val results = vectorStore.search(floatArrayOf(1f, 0f), "doc1")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testDocumentScoping() = runBlocking {
        val chunkA = DocumentChunk("c1", "docA", "Content A")
        val chunkB = DocumentChunk("c2", "docB", "Content B")
        
        vectorStore.addChunk(chunkA, floatArrayOf(1f, 0f))
        vectorStore.addChunk(chunkB, floatArrayOf(1f, 0f))
        
        val resultsA = vectorStore.search(floatArrayOf(1f, 0f), "docA")
        assertEquals(1, resultsA.size)
        assertEquals("docA", resultsA[0].documentId)
        
        val resultsB = vectorStore.search(floatArrayOf(1f, 0f), "docB")
        assertEquals(1, resultsB.size)
        assertEquals("docB", resultsB[0].documentId)
        
        val resultsC = vectorStore.search(floatArrayOf(1f, 0f), "docC")
        assertTrue(resultsC.isEmpty())
    }

    @Test
    fun testCosineSimilarityRanking() = runBlocking {
        // Query is [1, 0]
        val chunkPerfect = DocumentChunk("c1", "doc", "Perfect")
        val chunkGood = DocumentChunk("c2", "doc", "Good")
        val chunkIrrelevant = DocumentChunk("c3", "doc", "Irrelevant")
        
        // Perfect match: [1, 0]
        vectorStore.addChunk(chunkPerfect, floatArrayOf(1f, 0f))
        
        // Irrelevant match: [0, 1]
        vectorStore.addChunk(chunkIrrelevant, floatArrayOf(0f, 1f))
        
        // Good match: [0.707, 0.707]
        vectorStore.addChunk(chunkGood, floatArrayOf(0.707f, 0.707f))
        
        val results = vectorStore.search(floatArrayOf(1f, 0f), "doc", 3)
        assertEquals(3, results.size)
        assertEquals("c1", results[0].chunkId) // Perfect
        assertEquals("c2", results[1].chunkId) // Good
        assertEquals("c3", results[2].chunkId) // Irrelevant
    }
}
