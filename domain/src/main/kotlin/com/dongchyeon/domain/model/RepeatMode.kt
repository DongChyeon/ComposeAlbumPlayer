package com.dongchyeon.domain.model

/**
 * 음악 플레이어의 반복 모드
 */
enum class RepeatMode {
    /**
     * 반복 없음 - 플레이리스트가 끝나면 정지
     */
    NONE,

    /**
     * 전체 반복 - 플레이리스트가 끝나면 처음부터 다시 재생
     */
    ALL,

    /**
     * 한 곡 반복 - 현재 곡만 계속 반복
     */
    ONE,
}
