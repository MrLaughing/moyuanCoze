package com.mrlaughing.moyuan.engine.wither

import com.mrlaughing.moyuan.data.model.GrowthLevel
import com.mrlaughing.moyuan.data.model.Plant
import com.mrlaughing.moyuan.data.model.WitherStage
import com.mrlaughing.moyuan.engine.GrowthLevelResult
import com.mrlaughing.moyuan.engine.WitherCountdown
import com.mrlaughing.moyuan.engine.WitherResult
import com.mrlaughing.moyuan.engine.RecoveryResult
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 枯萎引擎
 *
 * 枯萎阶段：正常(0天) → 初淡(2天) → 渐枯(4天) → 将枯(7天) → 枯寂(14天)
 * 恢复保留比例：初淡90%、渐枯75%、将枯50%、枯寂30%
 */
object WitherEngine {

    /**
     * 各枯萎阶段的保留比例
     */
    private val RETENTION_RATIOS = mapOf(
        WitherStage.NONE to 1.0f,
        WitherStage.FADE to 0.9f,
        WitherStage.WITHER to 0.75f,
        WitherStage.SEVERE to 0.5f,
        WitherStage.DEAD to 0.3f
    )

    /**
     * 计算枯萎状态
     *
     * @param lastReadDate 最后一次阅读日期
     * @param currentDate 当前日期
     * @param currentWitherStage 当前枯萎阶段（用于判断 justDied）
     * @return WitherResult
     */
    fun calculateWither(
        lastReadDate: LocalDate,
        currentDate: LocalDate,
        currentWitherStage: WitherStage
    ): WitherResult {
        val daysSinceLastRead = ChronoUnit.DAYS.between(lastReadDate, currentDate).toInt()
            .coerceAtLeast(0)

        val newStage = WitherStage.fromDays(daysSinceLastRead)
        val isWithering = newStage != WitherStage.NONE
        val justDied = currentWitherStage != WitherStage.DEAD && newStage == WitherStage.DEAD

        return WitherResult(
            stage = newStage,
            daysSinceLastRead = daysSinceLastRead,
            isWithering = isWithering,
            justDied = justDied
        )
    }

    /**
     * 计算恢复后的状态
     *
     * 当用户恢复阅读时，根据枯萎程度扣除部分累计分钟数，保留对应比例。
     * 如果枯寂(DEAD)阶段的植物恢复阅读，则标记 justRevived=true（触发彼岸花解锁判定）。
     *
     * @param witherStage 恢复前的枯萎阶段
     * @param accumulatedMinutes 恢复前的累计分钟数
     * @param level 恢复前的等级
     * @return RecoveryResult
     */
    fun calculateRecovery(
        witherStage: WitherStage,
        accumulatedMinutes: Int,
        level: GrowthLevel
    ): RecoveryResult {
        val retentionRatio = RETENTION_RATIOS[witherStage] ?: 1.0f
        val recoveredMinutes = (accumulatedMinutes * retentionRatio).roundToInt().coerceAtLeast(0)
        val recoveredLevel = GrowthLevel.fromMinutes(recoveredMinutes)
        val justRevived = witherStage == WitherStage.DEAD

        return RecoveryResult(
            recoveredMinutes = recoveredMinutes,
            recoveredLevel = GrowthLevelResult(
                level = recoveredLevel.level,
                label = recoveredLevel.label
            ),
            justRevived = justRevived,
            retentionRatio = retentionRatio
        )
    }

    /**
     * 获取保留比例
     */
    fun getRetentionRatio(witherStage: WitherStage): Float {
        return RETENTION_RATIOS[witherStage] ?: 1.0f
    }

    /**
     * 计算枯萎倒计时
     *
     * @param plant 植物定义
     * @param lastReadDate 最后阅读日期
     * @param today 当前日期
     * @return WitherCountdown 包含当前阶段、下一阶段、距下一阶段天数、距枯寂天数
     */
    fun calculateWitherCountdown(
        plant: Plant,
        lastReadDate: LocalDate,
        today: LocalDate
    ): WitherCountdown {
        val daysSinceRead = ChronoUnit.DAYS.between(lastReadDate, today).toInt().coerceAtLeast(0)
        val currentStage = WitherStage.fromDays(daysSinceRead)

        // 计算下一阶段
        val nextStage = WitherStage.entries
            .filter { it.thresholdDays > currentStage.thresholdDays }
            .minByOrNull { it.thresholdDays }

        // 计算距下一阶段的天数
        val daysToNext = if (nextStage != null) {
            (nextStage.thresholdDays - daysSinceRead).coerceAtLeast(0)
        } else {
            0 // 已是最终阶段
        }

        // 计算距枯寂的天数
        val daysToDeath = if (currentStage == WitherStage.DEAD) {
            0
        } else {
            (WitherStage.DEAD.thresholdDays - daysSinceRead).coerceAtLeast(0)
        }

        return WitherCountdown(
            currentStage = currentStage,
            nextStage = nextStage,
            daysToNext = daysToNext,
            daysToDeath = daysToDeath
        )
    }
}
