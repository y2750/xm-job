package com.example.mapper;

import com.example.entity.Enterprise;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EnterpriseMapper {

    int insert(Enterprise enterprise);

    void updateById(Enterprise enterprise);

    void deleteById(Integer id);

    @Select("select enterprise.*, " +
            "employ.name as employName, employ.avatar as employAvatar, " +
            "employ.city as employCity, employ.address as employAddress " +
            "from enterprise " +
            "left join employ on enterprise.employ_id = employ.id " +
            "where enterprise.id = #{id}")
    Enterprise selectById(Integer id);

    @Select("select enterprise.*, " +
            "employ.name as employName, employ.avatar as employAvatar, " +
            "employ.city as employCity, employ.address as employAddress " +
            "from enterprise " +
            "left join employ on enterprise.employ_id = employ.id " +
            "where enterprise.employ_id = #{employId}")
    Enterprise selectByEmployId(Integer employId);

    List<Enterprise> selectAll(Enterprise enterprise);
}

