package com.proconnect.dto;

import com.proconnect.entity.BookingInquiry;
import com.proconnect.entity.JobPost;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingDTO {

    private Long id;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private Double customerLat;
    private Double customerLng;
    private String preferredDate;
    private String preferredTime;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private Long serviceId;
    private String cancellationToken;

    /**
     * "INQUIRY" for direct bookings, "JOB_POST" for accepted broadcast jobs.
     * Lets the frontend (and future cancel/status endpoints) route the action
     * to the correct table without relying on id sign tricks.
     */
    private String sourceType = "INQUIRY";

    /** The real primary-key in whichever table sourceType points to. */
    private Long sourceId;

    public static BookingDTO from(BookingInquiry b) {
        BookingDTO dto = new BookingDTO();
        dto.id            = b.getId();
        dto.sourceType    = "INQUIRY";
        dto.sourceId      = b.getId();
        dto.customerName  = b.getCustomerName();
        dto.customerEmail = b.getCustomerEmail();
        dto.customerPhone   = b.getCustomerPhone();
        dto.customerAddress = b.getCustomerAddress();
        dto.customerLat     = b.getCustomerLat();
        dto.customerLng     = b.getCustomerLng();
        dto.preferredDate   = b.getPreferredDate();
        dto.preferredTime = b.getPreferredTime();
        dto.note          = b.getNote();
        dto.status        = b.getStatus();
        dto.createdAt     = b.getCreatedAt();
        dto.serviceId     = b.getServiceId();
        dto.cancellationToken = b.getCancellationToken();
        return dto;
    }

    /**
     * Converts an accepted JobPost into a BookingDTO so it appears in the
     * pro's dashboard Booking Requests panel alongside direct bookings.
     * Uses a negative id offset (-(jobPost.id)) to avoid collisions with
     * BookingInquiry ids.
     */
    public static BookingDTO fromJobPost(JobPost j) {
        BookingDTO dto = new BookingDTO();
        dto.id              = j.getId();   // real job_posts id — safe to pass back to /api/jobs endpoints
        dto.sourceType      = "JOB_POST";
        dto.sourceId        = j.getId();
        dto.customerName    = j.getCustomerName();
        dto.customerEmail   = j.getCustomerEmail();
        dto.customerPhone   = j.getCustomerPhone();
        dto.customerAddress = j.getAddress();
        dto.customerLat     = j.getLat();
        dto.customerLng     = j.getLng();
        dto.note            = j.getDescription();    // description → shows in the note field
        dto.status          = "ACCEPTED";
        dto.createdAt       = j.getCreatedAt();
        return dto;
    }
}
