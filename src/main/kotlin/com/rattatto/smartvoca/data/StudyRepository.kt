// 2026-11-18 - Smart Voca - 학습 완료 기록과 재노출 일정 저장 (FR-020, FR-022, PR-03)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/StudyRepository.kt

package com.rattatto.smartvoca.data

import androidx.room.withTransaction
import com.rattatto.smartvoca.data.db.ReviewScheduleEntity
import com.rattatto.smartvoca.data.db.SmartVocaDatabase
import com.rattatto.smartvoca.data.db.StudyRecordEntity
import com.rattatto.smartvoca.domain.ReviewScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

class StudyRepository(private val database: SmartVocaDatabase) {

    private val studyDao = database.studyDao()
    private val reviewDao = database.reviewDao()

    /**
     * 학습 완료를 기록한다.
     * 학습 행은 매번 추가한다. 재노출 일정은 같은 날 같은 단어가 처음 완료될 때만 올린다 (PR-03).
     * 시험 결과는 이 함수와 관계없다 (FR-022, FR-037).
     */
    suspend fun markStudied(wordId: Long, date: LocalDate, now: Instant = Instant.now()) = withContext(Dispatchers.IO) {
        val dateText = date.toString()
        database.withTransaction {
            val alreadyStudiedToday = studyDao.countOn(wordId, dateText) > 0
            studyDao.insert(StudyRecordEntity(wordId = wordId, studiedAt = now.toString(), studyDate = dateText))
            if (!alreadyStudiedToday) {
                val step = ReviewScheduler.nextStep(reviewDao.find(wordId)?.step)
                val nextDate = ReviewScheduler.nextReviewDate(date, step)
                reviewDao.save(
                    ReviewScheduleEntity(
                        wordId = wordId,
                        step = step,
                        nextReviewDate = nextDate?.toString(),
                        updatedAt = now.toString(),
                    ),
                )
            }
        }
    }

    /** 날짜에 학습 완료한 단어 id 들. 목록의 완료 표시에 쓴다. */
    suspend fun studiedIdsOn(date: LocalDate): Set<Long> = withContext(Dispatchers.IO) {
        studyDao.studiedWordIdsOn(date.toString()).toSet()
    }
}
