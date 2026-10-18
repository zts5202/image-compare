package com.zts.imagecompare

import android.content.Context
import com.zts.imagecompare.data.ComparisonRepository

object Graph {
    lateinit var repository: ComparisonRepository
        private set

    fun init(context: Context) {
        if (!::repository.isInitialized) {
            repository = ComparisonRepository(context.applicationContext)
            repository.clearStaging()
        }
    }
}
