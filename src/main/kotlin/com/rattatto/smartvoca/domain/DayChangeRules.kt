// 2026-11-18 - Smart Voca - 날짜 변경 감지 규칙 (앱을 켤 때 마지막으로 본 날짜와 비교, 팝업 알림 여부)
// 파일 위치: src/main/kotlin/com/rattatto/smartvoca/domain/DayChangeRules.kt

package com.rattatto.smartvoca.domain

import java.time.LocalDate

object DayChangeRules {

    /**
     * 날짜가 바뀌었는지 본다. 마지막으로 본 날짜가 없으면 (처음 실행) 알리지 않는다.
     * 같은 날이면 알리지 않는다.
     */
    fun shouldNotify(lastSeen: LocalDate?, today: LocalDate): Boolean =
        lastSeen != null && lastSeen != today
}
