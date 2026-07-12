package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.entity.UserWallet;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeServiceInterface {

    private final TradeMapper tradeMapper;
    private final UserWalletMapper userWalletMapper;
    private final CryptoPriceMapper cryptoPriceMapper;
    private final CryptoPairMapper cryptoPairMapper;
    // Add additional beans here if needed for your implementation

    @Override
    public TradeResponse executeTrade(TradeRequest tradeRequest) {
        // TODO: Implement the core trading engine
        // What should happen when a user executes a trade?

        // validate req
        if (tradeRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        CryptoPair cryptoPair = cryptoPairMapper.findById(tradeRequest.getCryptoPairId());
        if (cryptoPair == null || !cryptoPair.getActive()) {
            throw new IllegalArgumentException("Unsupported trading pair.");
        }

        // get latest mkt price
        // reject stale prices (>30seconds ago)
        CryptoPrice cryptoPrice = cryptoPriceMapper.findLatestPriceByPairId(tradeRequest.getCryptoPairId());
        if (cryptoPrice == null) {
            throw new IllegalStateException("No market price available.");
        }

        if (cryptoPrice.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(30))) {
            throw new IllegalStateException("Latest market price is updating. Please try again shortly.");
        }

        // Get price and source
        // BUY = ASK
        // SELL = BID
        BigDecimal executionPrice;
        String executionSource;
        if (tradeRequest.getTradeType() == TradeType.BUY) {
            executionPrice = cryptoPrice.getAskPrice();
            executionSource = cryptoPrice.getAskSource();
        } else {
            executionPrice = cryptoPrice.getBidPrice();
            executionSource = cryptoPrice.getBidSource();
        }

        BigDecimal totalAmount = executionPrice.multiply(tradeRequest.getQuantity());

        // Check user wallet
        UserWalletDto quoteWalletDto = userWalletMapper.findByUserIdAndSymbolId(
                        tradeRequest.getUserId(),
                        cryptoPair.getQuoteSymbolId());

        UserWalletDto baseWalletDto = userWalletMapper.findByUserIdAndSymbolId(
                        tradeRequest.getUserId(),
                        cryptoPair.getBaseSymbolId());

        switch (tradeRequest.getTradeType()) {
            case BUY -> {
                if (quoteWalletDto == null) {
                    throw new IllegalStateException("Wallet not found.");
                }

                if (quoteWalletDto.getBalance().compareTo(totalAmount) < 0) {
                    throw new IllegalStateException(
                            "Insufficient " + quoteWalletDto.getSymbol() + " balance.");
                }
                quoteWalletDto.setBalance(quoteWalletDto.getBalance().subtract(totalAmount));
                userWalletMapper.updateWalletBalance(quoteWalletDto);
                if (baseWalletDto == null) {
                    UserWallet baseWallet = new UserWallet();
                    baseWallet.setUserId(tradeRequest.getUserId());
                    baseWallet.setSymbolId(cryptoPair.getBaseSymbolId());
                    baseWallet.setBalance(tradeRequest.getQuantity());

                    userWalletMapper.insertWallet(baseWallet);
                } else {
                    baseWalletDto.setBalance(
                            baseWalletDto.getBalance().add(tradeRequest.getQuantity()));
                    userWalletMapper.updateWalletBalance(baseWalletDto);
                }
            }

            case SELL -> {
                if (baseWalletDto == null) {
                    throw new IllegalStateException("Wallet not found.");
                }

                if (baseWalletDto.getBalance().compareTo(tradeRequest.getQuantity()) < 0) {
                    throw new IllegalStateException(
                            "Insufficient " + quoteWalletDto.getSymbol() + " balance.");
                }

                baseWalletDto.setBalance(
                        baseWalletDto.getBalance().subtract(tradeRequest.getQuantity()));
                userWalletMapper.updateWalletBalance(baseWalletDto);

                if (quoteWalletDto == null) {
                    UserWallet quoteWallet = new UserWallet();
                    quoteWallet.setUserId(tradeRequest.getUserId());
                    quoteWallet.setSymbolId(cryptoPair.getQuoteSymbolId());
                    quoteWallet.setBalance(totalAmount);

                    userWalletMapper.insertWallet(quoteWallet);
                } else {
                    quoteWalletDto.setBalance(
                            quoteWalletDto.getBalance().add(totalAmount));
                    userWalletMapper.updateWalletBalance(quoteWalletDto);
                }
            }

            default -> throw new UnsupportedOperationException("Unsupported trade type.");
        }

        // Save Trade and return TradeResponse
        Trade trade = new Trade();
        trade.setUserId(tradeRequest.getUserId());
        trade.setCryptoPairId(tradeRequest.getCryptoPairId());
        trade.setTradeType(tradeRequest.getTradeType().name());
        trade.setQuantity(tradeRequest.getQuantity());
        trade.setPrice(executionPrice);
        trade.setTotalAmount(totalAmount);
        trade.setTradeTime(LocalDateTime.now());
        tradeMapper.insertTrade(trade);

        TradeResponse response = new TradeResponse();

        response.setTradeId(trade.getId());
        response.setUserId(trade.getUserId());
        response.setCryptoPairId(trade.getCryptoPairId());
        response.setTradeType(tradeRequest.getTradeType());
        response.setQuantity(trade.getQuantity());
        response.setExecutionPrice(trade.getPrice());
        response.setTotalAmount(trade.getTotalAmount());
        response.setPriceSource(executionSource);
        response.setTradeTime(trade.getTradeTime());

        return response;
    }
}