package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Employ;
import com.example.entity.Position;
import com.example.exception.CustomException;
import com.example.mapper.EmployMapper;
import com.example.mapper.PositionMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 企业信息业务层方法
 */
@Service
public class EmployService {

    @Resource
    private EmployMapper employMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private com.example.mapper.EnterpriseMapper enterpriseMapper;
    @Resource
    private com.example.service.MessageService messageService;
    @Resource
    private NotificationService notificationService;
    @Resource
    private com.example.mapper.AdminMapper adminMapper;

    public void add(Employ employ) {
        Employ dbEmploy = employMapper.selectByUsername(employ.getUsername());
        if (ObjectUtil.isNotNull(dbEmploy)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(employ.getPassword())) {
            employ.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        if (ObjectUtil.isEmpty(employ.getName())) {
            employ.setName(employ.getUsername());
        }
        employ.setRole(RoleEnum.EMPLOY.name());
        employMapper.insert(employ);
    }

    public void updateById(Employ employ) {
        // 获取更新前的企业信息
        Employ dbEmploy = employMapper.selectById(employ.getId());
        if (dbEmploy == null) {
            return;
        }
        
        // 检查是否修改了企业基本信息
        boolean infoChanged = false;
        if (employ.getName() != null && !employ.getName().equals(dbEmploy.getName())) {
            infoChanged = true;
        } else if (employ.getAvatar() != null && !employ.getAvatar().equals(dbEmploy.getAvatar())) {
            infoChanged = true;
        } else if (employ.getCity() != null && !employ.getCity().equals(dbEmploy.getCity())) {
            infoChanged = true;
        } else if (employ.getAddress() != null && !employ.getAddress().equals(dbEmploy.getAddress())) {
            infoChanged = true;
        } else if (employ.getIndustryId() != null && !employ.getIndustryId().equals(dbEmploy.getIndustryId())) {
            infoChanged = true;
        } else if (employ.getScale() != null && !employ.getScale().equals(dbEmploy.getScale())) {
            infoChanged = true;
        } else if (employ.getStage() != null && !employ.getStage().equals(dbEmploy.getStage())) {
            infoChanged = true;
        }
        
        employMapper.updateById(employ);
        
        // 如果企业已认证，且修改了基本信息，取消认证状态并通知管理员
        if (infoChanged) {
            com.example.entity.Enterprise enterprise = enterpriseMapper.selectByEmployId(employ.getId());
            if (enterprise != null && enterprise.getVerified() != null && enterprise.getVerified()) {
                // 取消认证状态
                enterprise.setVerified(false);
                enterprise.setVerifiedAt(null);
                enterpriseMapper.updateById(enterprise);
                
                // 通知所有管理员
                List<com.example.entity.Admin> admins = adminMapper.selectAll(new com.example.entity.Admin());
                String enterpriseName = employ.getName() != null ? employ.getName() : "未知企业";
                String notificationContent = String.format("企业《%s》修改了企业资料，认证状态已自动取消，请审核是否重新认证。", enterpriseName);
                
                for (com.example.entity.Admin admin : admins) {
                    // 管理员通知使用ADMIN作为recipientType
                    notificationService.sendIndividualNotification("CERTIFICATION", admin.getId(), "ADMIN",
                            "企业资料修改", notificationContent, null, null, null);
                }
            }
        }
        
        if ("审核通过".equals(employ.getStatus())) {
            // 更新一下该企业岗位对应的新的行业
            List<Position> positions = positionMapper.selectByEmployId(employ.getId());
            for (Position position : positions) {
                if (ObjectUtil.isNotEmpty(position)) {
                    position.setIndustryId(employ.getIndustryId());
                    positionMapper.updateById(position);
                }
            }
        }
    }

    public void deleteById(Integer id) {
        employMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            employMapper.deleteById(id);
        }
    }

    public Employ selectById(Integer id) {
        Employ employ = employMapper.selectById(id);
        // 生成token
        String token = TokenUtils.createToken(employ.getId() + "-" + employ.getRole(), employ.getPassword());
        employ.setToken(token);
        return employ;
    }

    public List<Employ> selectAll(Employ employ) {
        return employMapper.selectAll(employ);
    }

    public PageInfo<Employ> selectPage(Employ employ, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Employ> list = employMapper.selectAll(employ);
        return PageInfo.of(list);
    }

    /**
     * 登录
     */
    public Employ login(Account account) {
        Employ dbEmploy = employMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbEmploy)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!dbEmploy.getPassword().equals(account.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        // 生成token
        String token = TokenUtils.createToken(dbEmploy.getId() + "-" + dbEmploy.getRole(), dbEmploy.getPassword());
        dbEmploy.setToken(token);
        return dbEmploy;
    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Employ dbEmploy = employMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbEmploy)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPassword().equals(dbEmploy.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbEmploy.setPassword(account.getNewPassword());
        employMapper.updateById(dbEmploy);
    }

    public void register(Account account) {
        Employ employ = new Employ();
        BeanUtils.copyProperties(account, employ);
        employ.setStatus("待审核");
        add(employ);
    }
}
