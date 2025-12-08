package com.example.mapper;

import com.example.entity.PaymentRecord;
import java.util.List;

public interface PaymentRecordMapper {
    int insert(PaymentRecord paymentRecord);
    void updateById(PaymentRecord paymentRecord);
    PaymentRecord selectById(Integer id);
    List<PaymentRecord> selectByUserIdAndType(Integer userId, String userType);
    PaymentRecord selectByProjectIdAndType(Integer projectId, String paymentType);
}

