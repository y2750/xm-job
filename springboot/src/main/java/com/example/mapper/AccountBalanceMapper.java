package com.example.mapper;

import com.example.entity.AccountBalance;
import org.apache.ibatis.annotations.Param;

public interface AccountBalanceMapper {
    int insert(AccountBalance accountBalance);
    void updateById(AccountBalance accountBalance);
    AccountBalance selectByUserIdAndType(@Param("userId") Integer userId, @Param("userType") String userType);
}

