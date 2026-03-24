package com.approvalflow.engine.service;

import com.approvalflow.engine.dto.request.CreateApprovalRequestDto;
import com.approvalflow.engine.dto.request.CreateApprovalStepDto;
import com.approvalflow.engine.dto.response.ApprovalRequestResponse;
import com.approvalflow.engine.entity.ApprovalRequest;
import com.approvalflow.engine.entity.Role;
import com.approvalflow.engine.entity.User;
import com.approvalflow.engine.enums.RequestStatus;
import com.approvalflow.engine.enums.RequestType;
import com.approvalflow.engine.enums.RoleName;
import com.approvalflow.engine.enums.StepStatus;
import com.approvalflow.engine.exception.InvalidStateException;
import com.approvalflow.engine.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {
    
    @Mock
    private ApprovalRequestRepository requestRepository;
    
    @Mock
    private ApprovalStepRepository stepRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private AuditLogService auditLogService;
    
    @InjectMocks
    private ApprovalRequestService approvalRequestService;
    
    private User testUser;
    private Role testRole;
    
    @BeforeEach
    void setUp() {
        testRole = Role.builder()
                .id(4L)
                .name(RoleName.ROLE_USER)
                .description("Regular user")
                .build();
        
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(roles)
                .build();
    }
    
    @Test
    void testCreateRequest() {
        CreateApprovalStepDto stepDto = CreateApprovalStepDto.builder()
                .stepName("Manager Approval")
                .stepOrder(1)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedUserIds(Arrays.asList(2L))
                .build();
        
        CreateApprovalRequestDto dto = CreateApprovalRequestDto.builder()
                .title("Test Request")
                .description("Test Description")
                .requestType(RequestType.ACCESS_REQUEST)
                .dueDate(LocalDate.now().plusDays(14))
                .steps(Arrays.asList(stepDto))
                .build();
        
        ApprovalRequest savedRequest = ApprovalRequest.builder()
                .id(1L)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .requestType(dto.getRequestType())
                .status(RequestStatus.DRAFT)
                .requester(testUser)
                .currentStepOrder(0)
                .dueDate(dto.getDueDate())
                .steps(new ArrayList<>())
                .build();
        
        User assignedUser = User.builder()
                .id(2L)
                .username("approver")
                .email("approver@example.com")
                .build();
        
        when(requestRepository.save(any(ApprovalRequest.class))).thenReturn(savedRequest);
        when(stepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignedUser));
        
        ApprovalRequestResponse response = approvalRequestService.createRequest(dto, testUser);
        
        assertNotNull(response);
        assertEquals("Test Request", response.getTitle());
        assertEquals(RequestType.ACCESS_REQUEST, response.getRequestType());
        assertEquals(RequestStatus.DRAFT, response.getStatus());
        assertEquals("testuser", response.getRequesterUsername());
        
        verify(requestRepository, atLeast(1)).save(any(ApprovalRequest.class));
        verify(auditLogService).logAction(any(), any(), any(), any(), any());
    }
    
    @Test
    void testSubmitRequest() {
        ApprovalRequest request = ApprovalRequest.builder()
                .id(1L)
                .title("Test Request")
                .status(RequestStatus.DRAFT)
                .requester(testUser)
                .currentStepOrder(0)
                .steps(new ArrayList<>())
                .build();
        
        com.approvalflow.engine.entity.ApprovalStep step = com.approvalflow.engine.entity.ApprovalStep.builder()
                .id(1L)
                .approvalRequest(request)
                .stepOrder(1)
                .stepName("Step 1")
                .status(StepStatus.PENDING)
                .build();
        
        request.getSteps().add(step);
        
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ApprovalRequest.class))).thenReturn(request);
        
        ApprovalRequestResponse response = approvalRequestService.submitRequest(1L, testUser);
        
        assertNotNull(response);
        assertEquals(RequestStatus.IN_REVIEW, request.getStatus());
        assertEquals(StepStatus.IN_PROGRESS, step.getStatus());
        assertEquals(1, request.getCurrentStepOrder());
        
        verify(requestRepository).save(any(ApprovalRequest.class));
        verify(auditLogService).logAction(any(), any(), any(), any(), any());
    }
    
    @Test
    void testSubmitRequestThrowsExceptionWhenNotDraft() {
        ApprovalRequest request = ApprovalRequest.builder()
                .id(1L)
                .title("Test Request")
                .status(RequestStatus.IN_REVIEW)
                .requester(testUser)
                .build();
        
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        
        assertThrows(InvalidStateException.class, () -> {
            approvalRequestService.submitRequest(1L, testUser);
        });
    }
}
