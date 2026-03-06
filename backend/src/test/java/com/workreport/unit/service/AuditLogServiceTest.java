package com.workreport.unit.service;

import com.workreport.entity.AuditLog;
import com.workreport.enums.AuditActionType;
import com.workreport.repository.AuditLogRepository;
import com.workreport.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Nested
    @DisplayName("log")
    class Log {

        @Test
        @DisplayName("呼叫時 save 被呼叫一次且傳入的 AuditLog 欄位正確")
        void whenCalled_savesAuditLogWithCorrectFields() {
            AuditActionType action = AuditActionType.PROJECT_CLOSED;
            Long actorId = 10L;
            String targetEntity = "Project";
            Long targetId = 5L;
            String summary = "Project closed by PM";

            auditLogService.log(action, actorId, targetEntity, targetId, summary);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getActionType()).isEqualTo(action);
            assertThat(saved.getActorId()).isEqualTo(actorId);
            assertThat(saved.getTargetEntity()).isEqualTo(targetEntity);
            assertThat(saved.getTargetId()).isEqualTo(targetId);
            assertThat(saved.getSummary()).isEqualTo(summary);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("actorId 為 null 時仍正確建立並儲存 AuditLog")
        void whenActorIdIsNull_savesAuditLogWithNullActorId() {
            AuditActionType action = AuditActionType.HOURS_REQUEST_APPROVED;
            String targetEntity = "HoursRequest";
            Long targetId = 3L;
            String summary = "Auto-approved by system";

            auditLogService.log(action, null, targetEntity, targetId, summary);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getActionType()).isEqualTo(action);
            assertThat(saved.getActorId()).isNull();
            assertThat(saved.getTargetEntity()).isEqualTo(targetEntity);
            assertThat(saved.getTargetId()).isEqualTo(targetId);
            assertThat(saved.getSummary()).isEqualTo(summary);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("不同 actionType 皆正確寫入")
        void withDifferentActionTypes_savesCorrectActionType() {
            auditLogService.log(AuditActionType.ROLE_CHANGE, 1L, "User", 2L, "Role updated");
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getActionType()).isEqualTo(AuditActionType.ROLE_CHANGE);
        }
    }
}
