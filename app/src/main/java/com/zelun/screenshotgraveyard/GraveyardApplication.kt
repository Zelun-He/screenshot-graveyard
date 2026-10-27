package com.zelun.screenshotgraveyard

import android.app.Application
import androidx.room.Room
import com.zelun.screenshotgraveyard.data.ArchiveDatabase
import com.zelun.screenshotgraveyard.data.ArchiveRepository
import kotlinx.coroutines.*

class GraveyardApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, ArchiveDatabase::class.java, "graveyard.db").build() }
    val repository by lazy { ArchiveRepository(this, database.screenshots()) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { repository.recoverOcr() }
    }
}
