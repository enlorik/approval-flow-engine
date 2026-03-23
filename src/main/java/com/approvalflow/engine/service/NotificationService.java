package com.approvalflow.engine.service;

import com.approvalflow.engine.entity.NotificationLog;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.NotificationType;
import com.approvalflow.engine.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    
    private final NotificationLogRepository notificationLogRepository;
    
    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }
    
    @Transactional
    public void createNotification(User recipient, String subject, String body) {
        NotificationLog notification = NotificationLog.builder()
                .recipient(recipient)
                .notificationType(NotificationType.IN_APP)
                .subject(subject)
                .body(body)
                .sent(false)
                .build();
        
        notificationLogRepository.save(notification);
    }
}
