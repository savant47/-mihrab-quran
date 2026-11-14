package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UnifiedDhikrItem(
    val uniqueKey: String,
    val scopeKey: String, // "favorites", "morning", "evening", "tasbeeh", or "hisn_ch_<id>"
    val title: String,
    val categoryTitle: String,
    val arabicText: String,
    val benefit: String = "",
    val targetCount: Int = 1,
    val isCustomAdded: Boolean = false
)

data class DhikrOverrideData(
    val uniqueKey: String,
    val title: String,
    val arabicText: String,
    val benefit: String,
    val targetCount: Int
)

/**
 * Central persistent manager for:
 * 1. Favorite Adhkar & Duas across the entire app (Morning/Evening, Hisn Al-Muslim 132 chapters, Tasbeeh, and Manual Adhkar).
 * 2. Favorite Chapters (أبواب حصن المسلم المفضلة).
 * 3. Long-press customizations on any Dhikr (editing text, title, benefit, or changing repeat count e.g. 1 -> 3).
 * 4. Deleting/hiding any Dhikr or adding new manual Adhkar to Favorites or any Chapter/Section.
 */
object AdhkarCustomizationManager {
    const val PREFS_NAME = "adhkar_customizations_prefs"

    private const val KEY_FAVORITE_ITEMS_JSON = "favorite_items_json_v1"
    private const val KEY_FAVORITE_CHAPTER_IDS = "favorite_chapter_ids_v1"
    private const val KEY_FAVORITE_CHAPTER_ORDER_JSON = "favorite_chapter_order_json_v1"
    private const val KEY_OVERRIDES_JSON = "dhikr_overrides_json_v1"
    private const val KEY_DELETED_KEYS = "deleted_dhikr_keys_v1"
    private const val KEY_CUSTOM_ADDED_JSON = "custom_added_adhkar_json_v1"
    private const val KEY_SCOPE_ORDER_JSON = "scope_custom_order_json_v1"

    private val _versionFlow = MutableStateFlow(0)
    val versionFlow: StateFlow<Int> = _versionFlow.asStateFlow()

    fun notifyUpdated() {
        _versionFlow.value += 1
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // =========================================================================
    // 0. Custom Ordering & Reordering (Move Up / Down / Top / Bottom in any section)
    // =========================================================================

    fun getScopeOrderList(context: Context, scopeKey: String): List<String> {
        val raw = prefs(context).getString(KEY_SCOPE_ORDER_JSON, null) ?: return emptyList()
        return try {
            val root = JSONObject(raw)
            val arr = root.optJSONArray(scopeKey) ?: return emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val k = arr.optString(i, "")
                if (k.isNotBlank()) result.add(k)
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveScopeOrderList(context: Context, scopeKey: String, orderedKeys: List<String>) {
        val raw = prefs(context).getString(KEY_SCOPE_ORDER_JSON, null)
        val root = try {
            if (raw != null) JSONObject(raw) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
        val arr = JSONArray()
        orderedKeys.distinct().forEach { arr.put(it) }
        root.put(scopeKey, arr)
        prefs(context).edit().putString(KEY_SCOPE_ORDER_JSON, root.toString()).apply()
    }

    fun <T> applyScopeOrder(
        context: Context,
        scopeKey: String,
        items: List<T>,
        keySelector: (T) -> String
    ): List<T> {
        val savedOrder = getScopeOrderList(context, scopeKey)
        if (savedOrder.isEmpty()) return items
        val indexMap = savedOrder.withIndex().associate { it.value to it.index }
        val inSaved = mutableListOf<Pair<Int, T>>()
        val notInSaved = mutableListOf<T>()
        items.forEach { item ->
            val k = keySelector(item)
            val idx = indexMap[k]
            if (idx != null) {
                inSaved.add(idx to item)
            } else {
                notInSaved.add(item)
            }
        }
        inSaved.sortBy { it.first }
        return inSaved.map { it.second } + notInSaved
    }

    /**
     * Moves an item up (`direction = -1`) or down (`direction = +1`) within `currentOrderedKeys` for `scopeKey`.
     */
    fun moveItemInScope(
        context: Context,
        scopeKey: String,
        currentOrderedKeys: List<String>,
        targetKey: String,
        direction: Int
    ): Boolean {
        val list = currentOrderedKeys.toMutableList()
        val idx = list.indexOf(targetKey)
        if (idx < 0) return false
        val newIdx = idx + direction
        if (newIdx !in list.indices) return false
        val temp = list[idx]
        list[idx] = list[newIdx]
        list[newIdx] = temp
        saveScopeOrderList(context, scopeKey, list)

        if (scopeKey == "favorites") {
            val favs = getFavoriteItemsRawOrdered(context, list)
            persistFavoriteItems(context, favs)
        }
        notifyUpdated()
        return true
    }

    /**
     * Moves an item directly to the start (`toStart = true`) or end (`toStart = false`) of `scopeKey`.
     */
    fun moveItemToEdgeInScope(
        context: Context,
        scopeKey: String,
        currentOrderedKeys: List<String>,
        targetKey: String,
        toStart: Boolean
    ): Boolean {
        val list = currentOrderedKeys.toMutableList()
        val idx = list.indexOf(targetKey)
        if (idx < 0) return false
        if (toStart && idx == 0) return false
        if (!toStart && idx == list.lastIndex) return false
        list.removeAt(idx)
        if (toStart) {
            list.add(0, targetKey)
        } else {
            list.add(targetKey)
        }
        saveScopeOrderList(context, scopeKey, list)

        if (scopeKey == "favorites") {
            val favs = getFavoriteItemsRawOrdered(context, list)
            persistFavoriteItems(context, favs)
        }
        notifyUpdated()
        return true
    }

    private fun getFavoriteItemsRawOrdered(context: Context, orderedKeys: List<String>): List<UnifiedDhikrItem> {
        val current = getFavoriteItems(context)
        val byKey = current.associateBy { it.uniqueKey }
        val result = mutableListOf<UnifiedDhikrItem>()
        orderedKeys.forEach { k ->
            byKey[k]?.let { result.add(it) }
        }
        current.forEach { item ->
            if (result.none { it.uniqueKey == item.uniqueKey }) {
                result.add(item)
            }
        }
        return result
    }

    // =========================================================================
    // 1. Overrides (Long-press edit of text, title, benefit, or target repeat count)
    // =========================================================================

    fun getOverridesMap(context: Context): Map<String, DhikrOverrideData> {
        val raw = prefs(context).getString(KEY_OVERRIDES_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, DhikrOverrideData>()
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val itemObj = obj.optJSONObject(k) ?: continue
                result[k] = DhikrOverrideData(
                    uniqueKey = k,
                    title = itemObj.optString("title", ""),
                    arabicText = itemObj.optString("arabicText", ""),
                    benefit = itemObj.optString("benefit", ""),
                    targetCount = itemObj.optInt("targetCount", 1).coerceAtLeast(1)
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun saveOverride(
        context: Context,
        uniqueKey: String,
        title: String,
        arabicText: String,
        benefit: String,
        targetCount: Int
    ) {
        val current = getOverridesMap(context).toMutableMap()
        val cleanCount = targetCount.coerceIn(1, 10000)
        current[uniqueKey] = DhikrOverrideData(
            uniqueKey = uniqueKey,
            title = title.trim(),
            arabicText = arabicText.trim(),
            benefit = benefit.trim(),
            targetCount = cleanCount
        )
        persistOverridesMap(context, current)

        // Also update inside custom added list if it's a custom item
        val customList = getCustomAddedAdhkar(context).toMutableList()
        val customIdx = customList.indexOfFirst { it.uniqueKey == uniqueKey }
        if (customIdx >= 0) {
            val old = customList[customIdx]
            customList[customIdx] = old.copy(
                title = title.trim().ifBlank { old.title },
                arabicText = arabicText.trim(),
                benefit = benefit.trim(),
                targetCount = cleanCount
            )
            persistCustomAddedAdhkar(context, customList)
        }

        // Also update inside favorites list if this item is favorited
        val favList = getFavoriteItems(context).toMutableList()
        val favIdx = favList.indexOfFirst { it.uniqueKey == uniqueKey }
        if (favIdx >= 0) {
            val oldFav = favList[favIdx]
            favList[favIdx] = oldFav.copy(
                title = title.trim().ifBlank { oldFav.title },
                arabicText = arabicText.trim(),
                benefit = benefit.trim(),
                targetCount = cleanCount
            )
            persistFavoriteItems(context, favList)
        }

        notifyUpdated()
    }

    private fun persistOverridesMap(context: Context, map: Map<String, DhikrOverrideData>) {
        val root = JSONObject()
        map.forEach { (k, v) ->
            val o = JSONObject().apply {
                put("title", v.title)
                put("arabicText", v.arabicText)
                put("benefit", v.benefit)
                put("targetCount", v.targetCount)
            }
            root.put(k, o)
        }
        prefs(context).edit().putString(KEY_OVERRIDES_JSON, root.toString()).apply()
    }

    // =========================================================================
    // 2. Deleted / Hidden Adhkar & Reset to Default
    // =========================================================================

    fun getDeletedKeys(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_DELETED_KEYS, emptySet())?.toSet() ?: emptySet()
    }

    fun deleteDhikr(context: Context, uniqueKey: String) {
        // Remove from custom added if present
        val customList = getCustomAddedAdhkar(context).toMutableList()
        val removedCustom = customList.removeAll { it.uniqueKey == uniqueKey }
        if (removedCustom) {
            persistCustomAddedAdhkar(context, customList)
        } else {
            val updatedDeleted = getDeletedKeys(context).toMutableSet()
            updatedDeleted.add(uniqueKey)
            prefs(context).edit().putStringSet(KEY_DELETED_KEYS, updatedDeleted).apply()
        }

        // Also remove from favorites if present
        val favList = getFavoriteItems(context).toMutableList()
        if (favList.removeAll { it.uniqueKey == uniqueKey }) {
            persistFavoriteItems(context, favList)
        }

        notifyUpdated()
    }

    fun restoreDefaultForScope(context: Context, scopePrefix: String) {
        val deleted = getDeletedKeys(context).toMutableSet()
        deleted.removeAll { it.startsWith(scopePrefix) }
        prefs(context).edit().putStringSet(KEY_DELETED_KEYS, deleted).apply()

        val overrides = getOverridesMap(context).toMutableMap()
        val keysToRemove = overrides.keys.filter { it.startsWith(scopePrefix) }
        keysToRemove.forEach { overrides.remove(it) }
        persistOverridesMap(context, overrides)

        notifyUpdated()
    }

    // =========================================================================
    // 3. Custom Added Adhkar (Manual additions in Favorites or any Section/Chapter)
    // =========================================================================

    fun getCustomAddedAdhkar(context: Context, scopeFilter: String? = null): List<UnifiedDhikrItem> {
        val raw = prefs(context).getString(KEY_CUSTOM_ADDED_JSON, null) ?: return emptyList()
        val list = mutableListOf<UnifiedDhikrItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val item = UnifiedDhikrItem(
                    uniqueKey = o.optString("uniqueKey", ""),
                    scopeKey = o.optString("scopeKey", "favorites"),
                    title = o.optString("title", "ذكر مضاف"),
                    categoryTitle = o.optString("categoryTitle", "أذكاري المفضلة"),
                    arabicText = o.optString("arabicText", ""),
                    benefit = o.optString("benefit", ""),
                    targetCount = o.optInt("targetCount", 1).coerceAtLeast(1),
                    isCustomAdded = true
                )
                if (item.uniqueKey.isNotBlank() && item.arabicText.isNotBlank()) {
                    if (scopeFilter == null || item.scopeKey == scopeFilter) {
                        list.add(item)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun addCustomDhikr(
        context: Context,
        scopeKey: String,
        title: String,
        categoryTitle: String,
        arabicText: String,
        benefit: String,
        targetCount: Int,
        alsoAddToFavorites: Boolean = true,
        insertAtStart: Boolean = false
    ): UnifiedDhikrItem {
        val cleanText = arabicText.trim()
        val cleanTitle = title.trim().ifBlank { "ذكر ودعاء مضاف" }
        val cleanCount = targetCount.coerceIn(1, 10000)
        val newItem = UnifiedDhikrItem(
            uniqueKey = "custom_${scopeKey}_${System.currentTimeMillis()}",
            scopeKey = scopeKey,
            title = cleanTitle,
            categoryTitle = categoryTitle.ifBlank { "أذكاري المفضلة" },
            arabicText = cleanText,
            benefit = benefit.trim(),
            targetCount = cleanCount,
            isCustomAdded = true
        )

        val current = getCustomAddedAdhkar(context).toMutableList()
        if (insertAtStart) {
            current.add(0, newItem)
        } else {
            current.add(newItem)
        }
        persistCustomAddedAdhkar(context, current)

        // Also update the saved scope order if one exists or if insertAtStart is specified
        val existingOrder = getScopeOrderList(context, scopeKey).toMutableList()
        if (existingOrder.isNotEmpty()) {
            if (insertAtStart) {
                existingOrder.add(0, newItem.uniqueKey)
            } else {
                existingOrder.add(newItem.uniqueKey)
            }
            saveScopeOrderList(context, scopeKey, existingOrder)
        }

        if (alsoAddToFavorites || scopeKey == "favorites") {
            val favs = getFavoriteItems(context).toMutableList()
            if (favs.none { it.uniqueKey == newItem.uniqueKey }) {
                if (insertAtStart) {
                    favs.add(0, newItem)
                } else {
                    favs.add(newItem)
                }
                persistFavoriteItems(context, favs)
                saveScopeOrderList(context, "favorites", favs.map { it.uniqueKey })
            }
        }

        notifyUpdated()
        return newItem
    }

    private fun persistCustomAddedAdhkar(context: Context, list: List<UnifiedDhikrItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val o = JSONObject().apply {
                put("uniqueKey", item.uniqueKey)
                put("scopeKey", item.scopeKey)
                put("title", item.title)
                put("categoryTitle", item.categoryTitle)
                put("arabicText", item.arabicText)
                put("benefit", item.benefit)
                put("targetCount", item.targetCount)
            }
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_CUSTOM_ADDED_JSON, arr.toString()).apply()
    }

    // =========================================================================
    // 4. Favorite Individual Adhkar & Favorite Hisn Chapters (الأبواب والأذكار المفضلة)
    // =========================================================================

    fun getFavoriteItems(context: Context): List<UnifiedDhikrItem> {
        val raw = prefs(context).getString(KEY_FAVORITE_ITEMS_JSON, null) ?: return emptyList()
        val overrides = getOverridesMap(context)
        val deleted = getDeletedKeys(context)
        val list = mutableListOf<UnifiedDhikrItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val key = o.optString("uniqueKey", "")
                if (key.isBlank() || deleted.contains(key)) continue
                val ov = overrides[key]
                val item = UnifiedDhikrItem(
                    uniqueKey = key,
                    scopeKey = o.optString("scopeKey", "favorites"),
                    title = ov?.title?.ifBlank { o.optString("title", "") } ?: o.optString("title", ""),
                    categoryTitle = o.optString("categoryTitle", ""),
                    arabicText = ov?.arabicText?.ifBlank { o.optString("arabicText", "") } ?: o.optString("arabicText", ""),
                    benefit = ov?.benefit ?: o.optString("benefit", ""),
                    targetCount = ov?.targetCount ?: o.optInt("targetCount", 1).coerceAtLeast(1),
                    isCustomAdded = o.optBoolean("isCustomAdded", false)
                )
                if (item.arabicText.isNotBlank()) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {
        }
        return applyScopeOrder(context, "favorites", list) { it.uniqueKey }
    }

    fun isDhikrFavorite(context: Context, uniqueKey: String): Boolean {
        return getFavoriteItems(context).any { it.uniqueKey == uniqueKey }
    }

    fun toggleDhikrFavorite(context: Context, item: UnifiedDhikrItem): Boolean {
        val current = getFavoriteItems(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.uniqueKey == item.uniqueKey }
        val nowFav = if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            false
        } else {
            current.add(item)
            true
        }
        persistFavoriteItems(context, current)
        saveScopeOrderList(context, "favorites", current.map { it.uniqueKey })
        notifyUpdated()
        return nowFav
    }

    private fun persistFavoriteItems(context: Context, list: List<UnifiedDhikrItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val o = JSONObject().apply {
                put("uniqueKey", item.uniqueKey)
                put("scopeKey", item.scopeKey)
                put("title", item.title)
                put("categoryTitle", item.categoryTitle)
                put("arabicText", item.arabicText)
                put("benefit", item.benefit)
                put("targetCount", item.targetCount)
                put("isCustomAdded", item.isCustomAdded)
            }
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_FAVORITE_ITEMS_JSON, arr.toString()).apply()
    }

    fun getFavoriteChapterIds(context: Context): Set<Int> {
        val rawSet = prefs(context).getStringSet(KEY_FAVORITE_CHAPTER_IDS, emptySet()) ?: emptySet()
        return rawSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun getOrderedFavoriteChapterIds(context: Context): List<Int> {
        val set = getFavoriteChapterIds(context)
        if (set.isEmpty()) return emptyList()
        val rawOrder = prefs(context).getString(KEY_FAVORITE_CHAPTER_ORDER_JSON, null)
        val savedOrder = mutableListOf<Int>()
        if (rawOrder != null) {
            try {
                val arr = JSONArray(rawOrder)
                for (i in 0 until arr.length()) {
                    val id = arr.optInt(i, -1)
                    if (id > 0 && set.contains(id)) {
                        savedOrder.add(id)
                    }
                }
            } catch (_: Exception) {
            }
        }
        val remaining = set.filter { !savedOrder.contains(it) }.sorted()
        return savedOrder + remaining
    }

    private fun saveOrderedFavoriteChapterIds(context: Context, orderedIds: List<Int>) {
        val arr = JSONArray()
        orderedIds.distinct().forEach { arr.put(it) }
        prefs(context).edit()
            .putString(KEY_FAVORITE_CHAPTER_ORDER_JSON, arr.toString())
            .apply()
    }

    fun moveFavoriteChapter(context: Context, chapterId: Int, direction: Int): Boolean {
        val ordered = getOrderedFavoriteChapterIds(context).toMutableList()
        val idx = ordered.indexOf(chapterId)
        if (idx < 0) return false
        val newIdx = idx + direction
        if (newIdx !in ordered.indices) return false
        val tmp = ordered[idx]
        ordered[idx] = ordered[newIdx]
        ordered[newIdx] = tmp
        saveOrderedFavoriteChapterIds(context, ordered)
        notifyUpdated()
        return true
    }

    fun isChapterFavorite(context: Context, chapterId: Int): Boolean {
        return getFavoriteChapterIds(context).contains(chapterId)
    }

    fun toggleChapterFavorite(context: Context, chapterId: Int): Boolean {
        val current = getFavoriteChapterIds(context).toMutableSet()
        val ordered = getOrderedFavoriteChapterIds(context).toMutableList()
        val added = if (current.contains(chapterId)) {
            current.remove(chapterId)
            ordered.remove(chapterId)
            false
        } else {
            current.add(chapterId)
            if (!ordered.contains(chapterId)) ordered.add(chapterId)
            true
        }
        prefs(context).edit()
            .putStringSet(KEY_FAVORITE_CHAPTER_IDS, current.map { it.toString() }.toSet())
            .apply()
        saveOrderedFavoriteChapterIds(context, ordered)
        notifyUpdated()
        return added
    }

    fun getTotalFavoritesCount(context: Context): Int {
        return getFavoriteItems(context).size + getFavoriteChapterIds(context).size
    }
}
