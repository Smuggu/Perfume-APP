package com.scentvault.app

import android.app.Application
import com.scentvault.app.data.AppDatabase
import com.scentvault.app.data.FragranceRepository

class ScentVaultApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { FragranceRepository(database.fragranceDao(), database.tagDao()) }
}
