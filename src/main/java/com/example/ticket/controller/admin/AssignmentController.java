package com.example.ticket.controller.admin;

import com.example.ticket.dto.request.TicketAssignRequest;
import com.example.ticket.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/assign")
    public ResponseEntity<Void> assign(@Valid @RequestBody TicketAssignRequest req) {
        assignmentService.assign(req.getTicketId(), req.getAgentId());
        return ResponseEntity.ok().build();
    }
}
