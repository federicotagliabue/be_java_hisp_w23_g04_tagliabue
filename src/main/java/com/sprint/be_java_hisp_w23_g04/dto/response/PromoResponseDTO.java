package com.sprint.be_java_hisp_w23_g04.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PromoResponseDTO extends PostResponseDTO {
    @JsonProperty("has_promo")
    private boolean hasPromo;
    private double discount;

    public PromoResponseDTO(int userId, int postId, LocalDate date, ProductDTO product, int category, double price, boolean hasPromo, double discount) {
        super(userId, postId, date, product, category, price);
        this.hasPromo = hasPromo;
        this.discount = discount;
    }
}
