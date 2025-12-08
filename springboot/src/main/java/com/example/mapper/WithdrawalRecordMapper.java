package com.example.mapper;

import com.example.entity.WithdrawalRecord;
import java.util.List;

public interface WithdrawalRecordMapper {
    int insert(WithdrawalRecord withdrawalRecord);
    void updateById(WithdrawalRecord withdrawalRecord);
    WithdrawalRecord selectById(Integer id);
    List<WithdrawalRecord> selectByUserIdAndType(Integer userId, String userType);
}

