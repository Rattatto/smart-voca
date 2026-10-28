// 2026-10-28 - Smart Voca - Room 데이터베이스 정의 (기기 내 저장, 서버 없음, ADR-002)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/data/db/SmartVocaDatabase.kt

package com.rattatto.smartvoca.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WordEntity::class],
    version = 1,
    // 스키마 파일은 1차에서 만들지 않는다. 버전이 바뀌는 시점에 켜고 마이그레이션을 정한다
    exportSchema = false,
)
abstract class SmartVocaDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {
        private const val DATABASE_FILE_NAME = "smart_voca.db"

        fun create(context: Context): SmartVocaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SmartVocaDatabase::class.java,
                DATABASE_FILE_NAME,
            ).build()
    }
}
