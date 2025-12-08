package com.example.mapper;

import com.example.entity.Freelancer;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FreelancerMapper {

    int insert(Freelancer freelancer);

    void updateById(Freelancer freelancer);

    void deleteById(Integer id);

    @Select("select freelancer.*, " +
            "user.name as userName, user.avatar as userAvatar, user.email as userEmail, user.phone as userPhone " +
            "from freelancer " +
            "left join user on freelancer.user_id = user.id " +
            "where freelancer.id = #{id}")
    Freelancer selectById(Integer id);

    @Select("select freelancer.*, " +
            "user.name as userName, user.avatar as userAvatar, user.email as userEmail, user.phone as userPhone " +
            "from freelancer " +
            "left join user on freelancer.user_id = user.id " +
            "where freelancer.user_id = #{userId}")
    Freelancer selectByUserId(Integer userId);

    List<Freelancer> selectAll(Freelancer freelancer);
}

