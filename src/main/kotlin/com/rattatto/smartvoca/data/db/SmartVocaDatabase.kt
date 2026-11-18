// 2026-10-28 - Smart Voca - Room 데이터베이스 정의 (기기 내 저장, 서버 없음, ADR-002)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/SmartVocaDatabase.kt

package com.rattatto.smartvoca.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: 일일 목록 테이블 `daily_list` 추가 (2026-11-11, #2).
 * 기존 `word` 테이블과 데이터는 그대로 둔다. SQL 은 [DailyWordEntity] 정의와 같아야 Room 검증을 통과한다.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_list` (" +
                "`date` TEXT NOT NULL, " +
                "`word_id` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`daily_count` INTEGER NOT NULL, " +
                "`level_filter` TEXT NOT NULL, " +
                "`shown_at` TEXT NOT NULL, " +
                "PRIMARY KEY(`date`, `word_id`), " +
                "FOREIGN KEY(`word_id`) REFERENCES `word`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION" +
                ")",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_list_word_id` ON `daily_list` (`word_id`)")
    }
}

/**
 * v2 → v3: 학습 기록 `study_record` 과 재노출 일정 `review_schedule` 추가 (2026-11-18, #3).
 * SQL 은 [StudyRecordEntity], [ReviewScheduleEntity] 정의와 같아야 Room 검증을 통과한다.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `study_record` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`word_id` INTEGER NOT NULL, " +
                "`studied_at` TEXT NOT NULL, " +
                "`study_date` TEXT NOT NULL, " +
                "FOREIGN KEY(`word_id`) REFERENCES `word`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION" +
                ")",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_record_word_id` ON `study_record` (`word_id`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_schedule` (" +
                "`word_id` INTEGER NOT NULL, " +
                "`step` INTEGER NOT NULL, " +
                "`next_review_date` TEXT, " +
                "`updated_at` TEXT NOT NULL, " +
                "PRIMARY KEY(`word_id`), " +
                "FOREIGN KEY(`word_id`) REFERENCES `word`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION" +
                ")",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_review_schedule_next_review_date` ON `review_schedule` (`next_review_date`)",
        )
    }
}

/**
 * v3 → v4: 시험 이력 `exam_attempt` 추가 (2026-11-18, #4). SQL 은 [ExamAttemptEntity] 정의와 같아야 한다.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exam_attempt` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`word_id` INTEGER NOT NULL, " +
                "`attempt_date` TEXT NOT NULL, " +
                "`attempted_at` TEXT NOT NULL, " +
                "`shown_meaning` TEXT NOT NULL, " +
                "`user_input` TEXT NOT NULL, " +
                "`is_correct` INTEGER NOT NULL, " +
                "FOREIGN KEY(`word_id`) REFERENCES `word`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION" +
                ")",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exam_attempt_word_id` ON `exam_attempt` (`word_id`)")
    }
}

@Database(
    entities = [
        WordEntity::class,
        DailyWordEntity::class,
        StudyRecordEntity::class,
        ReviewScheduleEntity::class,
        ExamAttemptEntity::class,
    ],
    version = 4,
    // 스키마 파일은 1차에서 만들지 않는다. 마이그레이션은 MIGRATION_*_* 로 직접 정의한다
    exportSchema = false,
)
abstract class SmartVocaDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun dailyWordDao(): DailyWordDao

    abstract fun studyDao(): StudyDao

    abstract fun reviewDao(): ReviewDao

    abstract fun examDao(): ExamDao

    companion object {
        private const val DATABASE_FILE_NAME = "smart_voca.db"

        fun create(context: Context): SmartVocaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SmartVocaDatabase::class.java,
                DATABASE_FILE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}
