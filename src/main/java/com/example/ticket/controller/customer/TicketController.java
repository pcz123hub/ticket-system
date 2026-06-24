package com.example.ticket.controller.customer;

import com.example.ticket.dto.request.TicketCreateRequest;
import com.example.ticket.dto.request.TicketSearchRequest;
import com.example.ticket.dto.response.PageResult;
import com.example.ticket.dto.response.TicketVO;
import com.example.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/customer/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketVO> create(
            @Valid @RequestBody TicketCreateRequest req,
            @RequestHeader(value = "Idempotent-Key", required = false) String idempotentKey) {
        return ResponseEntity.ok(ticketService.createTicket(req, idempotentKey));
    }

    @GetMapping
    public ResponseEntity<PageResult<TicketVO>> list(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ticketService.listCustomerTickets(customerId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketVO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> close(
            @PathVariable Long id,
            @RequestParam Long customerId,
            @RequestParam(required = false) Integer satisfaction) {
        ticketService.closeTicket(id, customerId, satisfaction);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PageResult<TicketVO>> search(TicketSearchRequest req) {
        return ResponseEntity.ok(ticketService.searchTickets(req));
    }
}
