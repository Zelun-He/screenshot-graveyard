package com.zelun.screenshotgraveyard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zelun.screenshotgraveyard.data.*
import com.zelun.screenshotgraveyard.domain.ArchiveRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveDatabaseTest {
    @Test fun ftsTracksInsertEditsStatusAndDelete() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ArchiveDatabase::class.java).build()
        try {
            val dao = db.screenshots()
            val item = ScreenshotEntity(imageName = "test.img", sha256 = "test-hash", title = "A forgotten recipe", bytes = 12)
            val id = dao.insert(item)
            dao.completeOcr(id, "Midnight miso ramen ingredients", item.title, item.title, "food")
            val expression = ArchiveRules.ftsExpression("miso ram")!!
            assertEquals("Query '$expression' over '${dao.get(id)?.extractedText}'", listOf(id), dao.searchIds(expression).first())
            assertTrue(dao.searchIds(ArchiveRules.ftsExpression("miso docker")!!).first().isEmpty())
            dao.edit(id, "Docker survival kit", "code")
            assertEquals(listOf(id), dao.searchIds(ArchiveRules.ftsExpression("docker")!!).first())
            assertTrue(dao.searchIds(ArchiveRules.ftsExpression("forgotten")!!).first().isEmpty())
            dao.completeOcr(id, "Late arriving OCR", item.title, "Automatic title", "food")
            assertEquals("Docker survival kit", dao.get(id)!!.title)
            assertEquals("code", dao.get(id)!!.tags)
            dao.edit(id, "My custom title", "")
            dao.completeOcr(id, "Retry OCR", "My custom title", "Automatic title", "food")
            assertEquals("My custom title", dao.get(id)!!.title)
            assertEquals("", dao.get(id)!!.tags) // Intentionally cleared tags stay cleared.
            assertEquals(1, dao.transition(id, "PENDING", "BURIED"))
            assertEquals(0, dao.transition(id, "PENDING", "KEPT")) // Stale actions cannot overwrite a later action.
            assertEquals(1, dao.transition(id, "BURIED", "PENDING"))
            dao.delete(dao.get(id)!!)
            assertTrue(dao.searchIds(ArchiveRules.ftsExpression("miso")!!).first().isEmpty())
        } finally { db.close() }
    }

    @Test fun identicalHashesAreUniqueAndSurviveReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "persistence-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, ArchiveDatabase::class.java, name).build()
        try {
            val item = ScreenshotEntity(imageName = "same.img", sha256 = "same-content", title = "Persist me", bytes = 42)
            val id = db.screenshots().insert(item)
            assertEquals(-1L, db.screenshots().insert(item.copy(title = "A duplicate")))
            db.screenshots().transition(id, "PENDING", "KEPT")
            db.close()
            db = Room.databaseBuilder(context, ArchiveDatabase::class.java, name).build()
            assertEquals("KEPT", db.screenshots().get(id)!!.status)
            assertEquals(listOf(id), db.screenshots().searchIds(ArchiveRules.ftsExpression("persist")!!).first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
