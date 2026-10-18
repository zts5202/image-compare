package com.zts.imagecompare.core

data class CropInput(
    val key: String,
    val label: String,
    val imageW: Int,
    val imageH: Int,
    val saved: CropSpec? = null,
)

data class CropTargetState(
    val key: String,
    val label: String,
    val imageW: Int,
    val imageH: Int,
    val preset: AspectPreset,
    val norm: NormRect,
    val dirty: Boolean,
    val cleared: Boolean,
    val saved: CropSpec?,
)

data class CropSnapshot(
    val index: Int,
    val sync: Boolean,
    val unify: Boolean,
    val targets: List<CropTargetState>,
) {
    val active: CropTargetState get() = targets[index]
}

sealed interface CropDecision {
    val key: String

    data class Unchanged(override val key: String) : CropDecision
    data class Clear(override val key: String) : CropDecision
    data class Apply(override val key: String, val spec: CropSpec) : CropDecision
}

/**
 * Crop editor state for one original plus the generated images being compared.
 * Index 0 is always the original. 「同步裁剪」 copies the active normalized rect and
 * preset onto the other image of the pair. 「统一画幅」 locks that pair to one pixel aspect.
 */
class CropSession(
    inputs: List<CropInput>,
    startIndex: Int,
) {
    private class Target(
        val key: String,
        val label: String,
        val imageW: Int,
        val imageH: Int,
        val saved: CropSpec?,
        var preset: AspectPreset,
        var norm: NormRect,
        var dirty: Boolean,
        var cleared: Boolean,
    )

    private val targets: List<Target> = inputs.map { input ->
        val saved = input.saved
        Target(
            key = input.key,
            label = input.label,
            imageW = input.imageW,
            imageH = input.imageH,
            saved = saved,
            preset = saved?.preset ?: AspectPreset.SOURCE,
            norm = saved?.norm?.let(CropMath::clamp) ?: NormRect.Full,
            dirty = false,
            cleared = false,
        )
    }
    var index: Int = startIndex.coerceIn(0, (targets.size - 1).coerceAtLeast(0))
        private set
    var sync: Boolean = false
        private set
    var unify: Boolean = true
        private set

    init {
        require(targets.isNotEmpty())
        suggestUnifiedFrame()
    }

    fun snapshot(): CropSnapshot = CropSnapshot(
        index = index,
        sync = sync,
        unify = unify,
        targets = targets.map {
            CropTargetState(it.key, it.label, it.imageW, it.imageH, it.preset, it.norm, it.dirty, it.cleared, it.saved)
        },
    )

    fun select(newIndex: Int) {
        if (newIndex !in targets.indices || newIndex == index) return
        index = newIndex
        val target = active()
        if (unify && target.saved == null && !target.dirty && !target.cleared) {
            applyPreset(target, original().preset, markDirty = true)
        }
    }

    fun setSync(enabled: Boolean) {
        sync = enabled
        if (enabled) pushActiveRect()
    }

    fun setUnify(enabled: Boolean) {
        unify = enabled
        if (enabled) {
            val preset = active().preset
            pair().forEach { applyPreset(it, preset, markDirty = true) }
            if (sync) pushActiveRect()
        }
    }

    fun setPreset(preset: AspectPreset) {
        val receivers = if (sync || unify) pair() else listOf(active())
        receivers.forEach { applyPreset(it, preset, markDirty = true) }
        if (sync) pushActiveRect()
    }

    fun onWindow(norm: NormRect) {
        val clamped = CropMath.clamp(norm)
        val current = active()
        if (CropMath.sameRect(current.norm, clamped) && !current.cleared) return
        current.norm = clamped
        current.dirty = true
        current.cleared = false
        if (sync) pushActiveRect()
    }

    fun restore() {
        val receivers = if (sync) pair() else listOf(active())
        receivers.forEach { target ->
            target.preset = AspectPreset.SOURCE
            target.norm = NormRect.Full
            target.cleared = true
            target.dirty = true
        }
    }

    fun decisions(): List<CropDecision> = targets.map { target ->
        when {
            !target.dirty && !target.cleared -> CropDecision.Unchanged(target.key)
            target.cleared || (target.norm.coversImage() && target.preset == AspectPreset.SOURCE) ->
                CropDecision.Clear(target.key)
            else -> CropDecision.Apply(target.key, CropSpec(target.norm, target.preset))
        }
    }

    private fun suggestUnifiedFrame() {
        if (!unify) return
        val preset = AspectPreset.SOURCE
        pair().forEach { target ->
            if (target.saved != null) return@forEach
            applyPreset(target, preset, markDirty = true)
        }
    }

    private fun applyPreset(target: Target, preset: AspectPreset, markDirty: Boolean) {
        target.preset = preset
        target.cleared = false
        val ratio = preset.ratio(
            imageW = target.imageW,
            imageH = target.imageH,
            sourceW = original().imageW,
            sourceH = original().imageH,
            unify = unify,
        )
        val anchor = target.norm
        target.norm = if (ratio == null) anchor else CropMath.fitRatio(anchor, target.imageW, target.imageH, ratio.first, ratio.second)
        if (markDirty) {
            target.dirty = changedFromSaved(target)
        }
    }

    /** 「同步裁剪」: the other image of the pair receives the same normalized rect and preset. */
    private fun pushActiveRect() {
        val source = active()
        pair().forEach { target ->
            if (target === source) return@forEach
            target.preset = source.preset
            target.norm = source.norm
            target.cleared = source.cleared
            target.dirty = true
        }
    }

    private fun changedFromSaved(target: Target): Boolean {
        val saved = target.saved
        if (saved == null) {
            return !(target.norm.coversImage() && target.preset == AspectPreset.SOURCE)
        }
        return saved.preset != target.preset || !CropMath.sameRect(saved.norm, target.norm)
    }

    private fun active(): Target = targets[index]

    private fun original(): Target = targets.first()

    private fun pair(): List<Target> {
        val original = original()
        val current = active()
        return if (current === original || targets.size == 1) listOf(original) else listOf(original, current)
    }
}
