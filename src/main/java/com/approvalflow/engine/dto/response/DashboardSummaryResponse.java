package com.approvalflow.engine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    
    private long pendingMyApprovals;
    private long overdueApprovals;
    private long approvedThisWeek;
    private long rejectedThisWeek;
}
