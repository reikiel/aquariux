package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import com.aquariux.technical.assessment.trade.entity.UserWallet;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserWalletMapper {
    
    @Select("""
            SELECT s.symbol, s.name, uw.balance 
            FROM symbols s 
            INNER JOIN user_wallets uw ON s.id = uw.symbol_id AND uw.user_id = #{userId} 
            ORDER BY s.symbol
            """)
    List<UserWalletDto> findByUserId(Long userId);

    @Select("""
            SELECT uw.id,
                   uw.user_id AS userId,
                   uw.symbol_id AS symbolId,
                   uw.balance,
                   uw.updated_at AS updatedAt,
                   s.symbol,
                   s.name
            FROM user_wallets uw
            INNER JOIN symbols s
                ON uw.symbol_id = s.id
            WHERE uw.user_id = #{userId}
              AND uw.symbol_id = #{symbolId}
            """)
    UserWalletDto findByUserIdAndSymbolId(Long userId, Long symbolId);

    // Update balance
    @Update("""
        UPDATE user_wallets
        SET balance = #{balance},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    void updateWalletBalance(UserWalletDto wallet);

    // Create Wallet
    @Insert("""
            INSERT INTO user_wallets
                (user_id, symbol_id, balance)
            VALUES
                (#{userId}, #{symbolId}, #{balance})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertWallet(UserWallet wallet);
}