package com.example.mapper;

import com.example.entity.FreelancerCertificate;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FreelancerCertificateMapper {
    int insert(FreelancerCertificate certificate);
    
    void updateById(FreelancerCertificate certificate);
    
    void deleteById(Integer id);
    
    @Select("select * from freelancer_certificate where freelancer_id = #{freelancerId} order by created_at desc")
    List<FreelancerCertificate> selectByFreelancerId(Integer freelancerId);
    
    @Select("select * from freelancer_certificate where id = #{id}")
    FreelancerCertificate selectById(Integer id);
    
    @Select("select * from freelancer_certificate order by created_at desc")
    List<FreelancerCertificate> selectAll();
}





