package com.itda.backend.job.event;

import com.itda.backend.job.controller.dto.JobResponse;

/**
 * Job 이벤트 메시지
 *
 * @param event 이벤트 타입 (job.done, job.failed)
 * @param data  이벤트 데이터
 */
public record JobEventMessage(
        String event,
        JobResponse data
) {
}
