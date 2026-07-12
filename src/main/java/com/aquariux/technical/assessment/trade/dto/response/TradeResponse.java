package com.aquariux.technical.assessment.trade.dto.response;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeResponse {
    // TODO: What should you return after a trade is executed?
    private Long tradeId;
    private Long userId;
    private Long cryptoPairId;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal executionPrice;
    private BigDecimal totalAmount;
    private String priceSource;
    private LocalDateTime tradeTime;
}