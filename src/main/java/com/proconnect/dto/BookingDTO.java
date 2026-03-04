package com.proconnect.dto;

import com.proconnect.entity.BookingInquiry;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingDTO {

    private Long id;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String preferredDate;
    private String preferredTime;
    private String note;
    private LocalDateTime createdAt;

    public static BookingDTO from(BookingInquiry b) {
        BookingDTO dto = new BookingDTO();
        dto.id            = b.getId();
        dto.customerName  = b.getCustomerName();
        dto.customerEmail = b.getCustomerEmail();
        dto.customerPhone = b.getCustomerPhone();
        dto.preferredDate = b.getPreferredDate();
        dto.preferredTime = b.getPreferredTime();
        dto.note          = b.getNote();
        dto.createdAt     = b.getCreatedAt();
        return dto;
    }
}
