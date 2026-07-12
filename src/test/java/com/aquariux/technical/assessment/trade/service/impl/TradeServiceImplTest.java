package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TradeServiceImplTest {
    @Mock
    private TradeMapper tradeMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private CryptoPriceMapper cryptoPriceMapper;

    @Mock
    private CryptoPairMapper cryptoPairMapper;

    @InjectMocks
    private TradeServiceImpl tradeService;

    private TradeRequest tradeRequest;
    private CryptoPair cryptoPair;
    private CryptoPrice cryptoPrice;
    private UserWalletDto usdtWallet;
    private UserWalletDto btcWallet;
    @BeforeEach
    void setUp() {

        tradeRequest = new TradeRequest();
        tradeRequest.setUserId(1L);
        tradeRequest.setCryptoPairId(1L); // BTCUSDT
        tradeRequest.setTradeType(TradeType.BUY);
        tradeRequest.setQuantity(new BigDecimal("0.1"));

        cryptoPair = new CryptoPair();
        cryptoPair.setId(1L);
        cryptoPair.setBaseSymbolId(1L);      // BTC
        cryptoPair.setQuoteSymbolId(3L);     // USDT
        cryptoPair.setPairName("BTCUSDT");
        cryptoPair.setActive(true);

        cryptoPrice = new CryptoPrice();
        cryptoPrice.setCryptoPairId(1L);
        cryptoPrice.setBidPrice(new BigDecimal("49900"));
        cryptoPrice.setAskPrice(new BigDecimal("50000"));
        cryptoPrice.setBidSource("BINANCE");
        cryptoPrice.setAskSource("BINANCE");
        cryptoPrice.setCreatedAt(LocalDateTime.now());

        usdtWallet = new UserWalletDto();
        usdtWallet.setId(1L);
        usdtWallet.setUserId(1L);
        usdtWallet.setSymbolId(3L);
        usdtWallet.setSymbol("USDT");
        usdtWallet.setName("Tether");
        usdtWallet.setBalance(new BigDecimal("100000"));
        usdtWallet.setUpdatedAt(LocalDateTime.now());

        btcWallet = new UserWalletDto();
        btcWallet.setId(2L);
        btcWallet.setUserId(1L);
        btcWallet.setSymbolId(1L);
        btcWallet.setSymbol("BTC");
        btcWallet.setName("Bitcoin");
        btcWallet.setBalance(new BigDecimal("1"));
        btcWallet.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void executeTrade_Buy_ShouldReturnTradeResponse() {

        when(cryptoPairMapper.findById(1L)).thenReturn(cryptoPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(1L)).thenReturn(cryptoPrice);

        when(userWalletMapper.findByUserIdAndSymbolId(1L, 3L))
                .thenReturn(usdtWallet);

        when(userWalletMapper.findByUserIdAndSymbolId(1L, 1L))
                .thenReturn(btcWallet);

        TradeResponse response = tradeService.executeTrade(tradeRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(response.getExecutionPrice()).isEqualTo(cryptoPrice.getAskPrice());

        verify(userWalletMapper).updateWalletBalance(usdtWallet);
        verify(userWalletMapper).updateWalletBalance(btcWallet);
        verify(tradeMapper).insertTrade(any(Trade.class));
    }

    @Test
    void executeTrade_WithInvalidQuantity_ShouldThrowException() {

        tradeRequest.setQuantity(BigDecimal.ZERO);

        assertThatThrownBy(() ->
                tradeService.executeTrade(tradeRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void executeTrade_WithUnsupportedPair_ShouldThrowException() {

        when(cryptoPairMapper.findById(1L))
                .thenReturn(null);

        assertThatThrownBy(() ->
                tradeService.executeTrade(tradeRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported trading pair.");
    }

    @Test
    void executeTrade_WithStalePrice_ShouldThrowException() {

        cryptoPrice.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        when(cryptoPairMapper.findById(1L))
                .thenReturn(cryptoPair);

        when(cryptoPriceMapper.findLatestPriceByPairId(1L))
                .thenReturn(cryptoPrice);

        assertThatThrownBy(() ->
                tradeService.executeTrade(tradeRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Latest market price is updating. Please try again shortly.");
    }

    @Test
    void executeTrade_WithInsufficientBalance_ShouldThrowException() {

        usdtWallet.setBalance(BigDecimal.ONE);

        when(cryptoPairMapper.findById(1L)).thenReturn(cryptoPair);
        when(cryptoPriceMapper.findLatestPriceByPairId(1L)).thenReturn(cryptoPrice);

        when(userWalletMapper.findByUserIdAndSymbolId(1L, 3L))
                .thenReturn(usdtWallet);

        when(userWalletMapper.findByUserIdAndSymbolId(1L, 1L))
                .thenReturn(btcWallet);

        assertThatThrownBy(() ->
                tradeService.executeTrade(tradeRequest))
                .isInstanceOf(IllegalStateException.class);
    }
}
