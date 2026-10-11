package com.whattocook.app.data.repository

import com.whattocook.app.domain.model.PantryGroup
import com.whattocook.app.domain.model.PantryItem
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observeItems(): Flow<List<PantryItem>>
    fun observeGroups(): Flow<List<PantryGroup>>
    suspend fun pantryNames(): List<String>
    /** Returns false when the ingredient is already in the pantry. */
    suspend fun addItem(name: String, groupId: Long?): Boolean
    suspend fun removeItem(id: Long)
    suspend fun createGroup(name: String): Long
    /** Deletes the group; its ingredients stay in the pantry. */
    suspend fun deleteGroup(id: Long)
}
