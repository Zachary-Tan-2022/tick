package com.tick.ticketsservice.service;

import java.util.*;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import com.tick.ticketsservice.model.Ticket;
import com.tick.ticketsservice.model.RecaptchaObject;
import com.tick.ticketsservice.model.Ticket.CompositeKey;
import com.tick.ticketsservice.repository.TicketRepository;

@Service
@AllArgsConstructor
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private final WebClient webClient;

    public List<Ticket> getAllTickets(){
        return ticketRepository.findAll();
    }

    public Ticket addTicket(Ticket ticket) {
        // check that event date ID is valid
        return ticketRepository.save(ticket);
    }

    public List<Ticket> getTicketByUserId(String userId) {
        return ticketRepository.findByUser(userId);
    }

    public Ticket getTicketByKey(CompositeKey key){
        return ticketRepository.findByKey(key).orElse(null);
    }

    //if user transfers ticket
    public Ticket updateTicket(Ticket updatedTicket) {
        return ticketRepository.save(
                ticketRepository.findByKey(updatedTicket.getKey()).map(
                    ticket -> updatedTicket).orElse(null));
    }

    //if user deactivates account
    public List<Ticket> releaseTicket(String userId) {
         List<Ticket> tickets = ticketRepository.findByUser(userId);
         tickets.forEach(ticket -> ticket.setUser(null));

         return ticketRepository.saveAll(tickets);
    }

    //if event is cancelled
    public String deleteTicketByEvent(String eventId){
        List<Ticket> tickets = ticketRepository.findAll();
        tickets.forEach(ticket -> {
            CompositeKey key = ticket.getKey();
            if (eventId.equals(key.getEventDate())) ticketRepository.deleteByKey(key);
        });
        return "event " + eventId + "'s tickets have been deleted";
    }

    //if user transfers ticket
    //is it possible for user to get a refund?
    public String deleteTicketByTicketId(CompositeKey key) {
        ticketRepository.deleteByKey(key);
        return key + "ticket has been deleted";
    }

    // [{category: "CAT1", section: "PC1", row: "C", quantity: 2}, {category: "CAT2", section: "140", row: "B", quantity: 1}]
    public List<Ticket> allocateSeats(String eventDateID, List<SelectedRow> selectedRows, String token) {
        EventDate eventDate = ticketRepository.findById(eventDateID).get();
        if (eventDate == null)
            throw new Error("Event date not found");

        Map<String, Map<String, Map<String, Integer>>> seatAvailability = eventDate.getSeatAvailability();

        Event event = findEventById(eventDate.getEventID()).block();
        if (event == null)
            throw new Error("Event not found");

        Map<String, Map<String, Map<String, Integer>>> seatMap = event.getSeatMap();

        Integer totalQuantity = 0;
        for (SelectedRow rowObj : selectedRows) {
            totalQuantity += rowObj.getQuantity();
        }
        if (totalQuantity >= purchaseLimit)
            throw new Error("Selected quantity is above purchase limit");

        List<Ticket> allocatedTickets = new ArrayList<>();

        for (SelectedRow rowObj : selectedRows) {

            String category = rowObj.getCategory();
            String section = rowObj.getSection();
            String row = rowObj.getRow();
            Integer quantity = rowObj.getQuantity();

            Integer currentAvailable = seatAvailability.get(category).get(section).get(row);
            if (currentAvailable == null)
                throw new Error("Invalid seat selection");

            Integer maxCapacity = seatMap.get(category).get(section).get(row);
            if (maxCapacity == null)
                throw new Error("Invalid seat selection");

            if (currentAvailable == 0)
                throw new Error("No more seats available");

            if (currentAvailable - quantity < 0)
                throw new Error("Invalid quantity");

            for (int i = 0; i < quantity; i++) {
                Integer seatNumber = maxCapacity - currentAvailable + 1;

                Ticket t = new Ticket(eventDateID, category, section, row, seatNumber, "Peter Pan");
                createTicket(t);
                allocatedTickets.add(t);
                currentAvailable--;
            }

            Map<String, Map<String, Integer>> categoryMap = seatAvailability.get(category);
            Map<String, Integer> sectionMap = categoryMap.get(section);
            sectionMap.put(row, currentAvailable);
        }

        eventDate.setSeatAvailability(seatAvailability);
        eventDateRepository.save(eventDate);

        return allocatedTickets;
    }

    public Mono<Object> verifyRecaptcha(String recaptchaToken) {
        return webClient.post()
            .uri("https://www.google.com/recaptcha/api/siteverify?secret={secret}&response={response}",
                RecaptchaObject.getSecret(), recaptchaToken
            )
            .retrieve()
            .toEntity(Object.class)
            .flatMap(responseEntity -> {
                System.out.println("Verified Recaptcha: " + responseEntity.getBody());
                return Mono.just(responseEntity.getBody());
            }
        ); 
    }
}
