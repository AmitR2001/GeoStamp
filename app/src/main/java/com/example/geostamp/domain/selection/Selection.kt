package com.example.geostamp.domain.selection

/** Which photos are ticked in the gallery. Immutable: every change gives a new Selection. Unit tested. */
data class Selection(val ids: Set<String> = emptySet()) {
    val count: Int get() = ids.size
    val isActive: Boolean get() = ids.isNotEmpty()

    fun contains(id: String): Boolean = id in ids
    fun toggle(id: String): Selection = if (id in ids) Selection(ids - id) else Selection(ids + id)
    fun selectAll(all: List<String>): Selection = Selection(all.toSet())
    fun clear(): Selection = Selection()

    /** Forget ticked photos that no longer exist (for example after a delete). */
    fun retainOnly(existing: Collection<String>): Selection = Selection(ids.intersect(existing.toSet()))
}
