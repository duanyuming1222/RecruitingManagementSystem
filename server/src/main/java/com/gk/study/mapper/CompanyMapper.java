package com.gk.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.study.entity.Company;
import com.gk.study.entity.Thing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CompanyMapper extends BaseMapper<Company> {

    @Select("SELECT user_id FROM b_company WHERE id = #{companyId}")
    List<String> selectUserIdsByCompanyId(String companyId);

}
