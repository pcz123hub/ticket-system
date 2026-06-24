package com.example.ticket.controller.admin;

import com.example.ticket.dto.request.TicketSearchRequest;
import com.example.ticket.dto.request.TicketTransferRequest;
import com.example.ticket.dto.response.PageResult;
import com.example.ticket.dto.response.TicketVO;
import com.example.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class TicketAdminController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<PageResult<TicketVO>> list(TicketSearchRequest req) {
        return ResponseEntity.ok(ticketService.searchTickets(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketVO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TicketTransferRequest req,
                                          @RequestParam Long operatorId) {
        ticketService.transferTicket(req.getTicketId(), req.getTargetAgentId(), req.getReason(), operatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resolve")
    public ResponseEntity<Void> resolve(@RequestParam Long ticketId, @RequestParam Long operatorId) {
        ticketService.resolveTicket(ticketId, operatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/close")
    public ResponseEntity<Void> close(@RequestParam Long ticketId, @RequestParam Long operatorId) {
        ticketService.closeTicket(ticketId, operatorId, null);
        return ResponseEntity.ok().build();
    }
}
