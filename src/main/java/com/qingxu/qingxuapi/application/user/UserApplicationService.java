package com.qingxu.qingxuapi.application.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.audit.AuditEventType;
import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.permissionchange.PermissionChangeDispatcher;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysDepartmentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserDepartmentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserPreferenceEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserRoleEntity;
import com.alibaba.excel.EasyExcel;
import com.qingxu.qingxuapi.infrastructure.security.QingxuUserPrincipal;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysDepartmentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserDepartmentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserPreferenceMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserRoleMapper;
import com.qingxu.qingxuapi.interfaces.user.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final SysUserPreferenceMapper preferenceMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SysUserDepartmentMapper userDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDepartmentMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final AuditService auditService;
    private final SessionRegistry sessionRegistry;
    private final PermissionChangeDispatcher permissionChangeDispatcher;

    // ========== 用户偏好 & 个人设置 ==========

    public UserPreferenceResponse getUserPreference(Long userId) {
        SysUserPreferenceEntity preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            return getDefaultPreference(userId);
        }
        return convertToResponse(preference);
    }

    public void saveUserPreference(Long userId, SavePreferenceRequest request) {
        SysUserPreferenceEntity preference = preferenceMapper.selectByUserId(userId);
        if (preference == null) {
            preference = new SysUserPreferenceEntity();
            preference.setUserId(userId);
            copySystemSettings(request.systemSettings(), preference);
            copyThemeConfig(request.themeConfig(), preference);
            preference.setCreatedAt(LocalDateTime.now());
            preference.setUpdatedAt(LocalDateTime.now());
            preferenceMapper.insert(preference);
        } else {
            copySystemSettings(request.systemSettings(), preference);
            copyThemeConfig(request.themeConfig(), preference);
            preference.setUpdatedAt(LocalDateTime.now());
            preferenceMapper.updateById(preference);
        }
    }

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setNickname(request.nickname());
        if (request.realname() != null) {
            user.setRealname(request.realname());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        } else {
            user.setEmail(null);
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        } else {
            user.setPhone(null);
        }
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request, Long currentUserId, String currentUsername, HttpServletRequest servletRequest) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_INCORRECT, "旧密码错误");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(encodedPassword);
        user.setNeedPasswordChange(false);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        expireUserSessions(userId);

        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.removeAttribute(com.qingxu.qingxuapi.application.auth.AuthApplicationService.SESSION_CURRENT_USER);
            session.removeAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            session.invalidate();
        }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        auditService.record(AuditEventType.USER_PASSWORD_CHANGE, true, currentUsername, currentUserId, servletRequest);
    }

    // ========== 用户管理（管理员） ==========

    public PageResponse<UserVO> list(UserListRequest request) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.username()), SysUserEntity::getUsername, request.username())
               .like(StringUtils.hasText(request.phone()), SysUserEntity::getPhone, request.phone())
               .eq(StringUtils.hasText(request.userType()), SysUserEntity::getUserType, request.userType())
               .eq(StringUtils.hasText(request.status()), SysUserEntity::getStatus, request.status())
               .orderByDesc(SysUserEntity::getCreatedAt);

        if (request.deptId() != null) {
            List<Long> deptIds = getDeptIdsByDeptId(request.deptId());
            if (!deptIds.isEmpty()) {
                List<Long> userIds = getUserIdsByDeptIds(deptIds);
                if (userIds.isEmpty()) {
                    return new PageResponse<>(List.of(), 0, request.getPage(), request.getPageSize());
                }
                wrapper.in(SysUserEntity::getId, userIds);
            }
        }

        Page<SysUserEntity> page = userMapper.selectPage(
                new Page<>(request.getPage(), request.getPageSize()), wrapper);

        List<SysUserEntity> users = page.getRecords();
        List<UserVO> records = batchConvertToUserVO(users);

        return new PageResponse<>(records, page.getTotal(), request.getPage(), request.getPageSize());
    }

    public UserVO detail(Long id) {
        SysUserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToUserVO(user);
    }

    @Transactional
    public Long create(CreateUserRequest request, Long currentUserId, String currentUsername, HttpServletRequest servletRequest) {
        checkUsernameUnique(request.username());
        if (StringUtils.hasText(request.phone())) {
            checkPhoneUnique(request.phone(), null);
        }
        if (StringUtils.hasText(request.email())) {
            checkEmailUnique(request.email(), null);
        }
        SysUserEntity user = new SysUserEntity();
        user.setTenantId("default");
        user.setUsername(request.username());
        user.setNickname(request.nickname() != null ? request.nickname() : request.username());
        user.setRealname(request.realname());
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone() : null);
        user.setEmail(StringUtils.hasText(request.email()) ? request.email() : null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(request.userType());
        user.setStatus("ACTIVE");
        user.setNeedPasswordChange(true);
        user.setFailedLoginCount(0);
        user.setCreatedBy(currentUserId);
        user.setUpdatedBy(currentUserId);
        user.setDeleted(0);
        userMapper.insert(user);

        List<Long> realDeptIds = filterOutTempDepts(request.deptIds());
        if (!realDeptIds.isEmpty()) {
            validateDeptSelection(realDeptIds);
            saveUserDeptRelations(user.getId(), realDeptIds);
        } else {
            joinRootTempDept(user.getId());
        }

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            saveUserRoleRelations(user.getId(), request.roleIds());
        }

        auditService.record(AuditEventType.USER_CREATE, true, currentUsername, currentUserId, servletRequest);
        return user.getId();
    }

    @Transactional
    public void update(Long id, UpdateUserRequest request, Long currentUserId, String currentUsername, HttpServletRequest servletRequest) {
        SysUserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(request.phone())) {
            checkPhoneUnique(request.phone(), id);
        }
        if (StringUtils.hasText(request.email())) {
            checkEmailUnique(request.email(), id);
        }
        List<Long> realDeptIds = filterOutTempDepts(request.deptIds());
        if (!realDeptIds.isEmpty()) {
            validateDeptSelection(realDeptIds);
        }

        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.realname() != null) {
            user.setRealname(request.realname());
        }
        if (request.phone() != null) {
            user.setPhone(StringUtils.hasText(request.phone()) ? request.phone() : null);
        }
        if (request.email() != null) {
            user.setEmail(StringUtils.hasText(request.email()) ? request.email() : null);
        }
        if (request.userType() != null) {
            user.setUserType(request.userType());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        if (request.deptIds() != null) {
            LambdaQueryWrapper<SysUserDepartmentEntity> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserDepartmentEntity::getUserId, id);
            userDeptMapper.delete(deleteWrapper);

            if (!realDeptIds.isEmpty()) {
                saveUserDeptRelations(id, realDeptIds);
            } else {
                joinRootTempDept(id);
            }
        }

        if (request.roleIds() != null) {
            LambdaQueryWrapper<SysUserRoleEntity> deleteRoleWrapper = new LambdaQueryWrapper<>();
            deleteRoleWrapper.eq(SysUserRoleEntity::getUserId, id);
            userRoleMapper.delete(deleteRoleWrapper);

            saveUserRoleRelations(id, request.roleIds());
        }

        auditService.record(AuditEventType.USER_UPDATE, true, currentUsername, currentUserId, servletRequest);
    }

    @Transactional
    public void delete(String ids, Long currentUserId, String currentUsername, HttpServletRequest servletRequest) {
        List<Long> idList = parseIds(ids);
        for (Long id : idList) {
            SysUserEntity user = userMapper.selectById(id);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户ID " + id + " 不存在");
            }
            if (id.equals(currentUserId)) {
                throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF, "不能删除当前登录用户");
            }

            LambdaQueryWrapper<SysUserRoleEntity> deleteRoleWrapper = new LambdaQueryWrapper<>();
            deleteRoleWrapper.eq(SysUserRoleEntity::getUserId, id);
            userRoleMapper.delete(deleteRoleWrapper);

            LambdaQueryWrapper<SysUserDepartmentEntity> deleteDeptWrapper = new LambdaQueryWrapper<>();
            deleteDeptWrapper.eq(SysUserDepartmentEntity::getUserId, id);
            userDeptMapper.delete(deleteDeptWrapper);

            permissionChangeDispatcher.fireUserDeleteChange(id, currentUserId, currentUsername, "用户被删除");
            userMapper.deleteById(id);
        }
        auditService.record(AuditEventType.USER_DELETE, true, currentUsername, currentUserId, servletRequest);
    }

    @Transactional
    public void toggleStatus(Long id, ToggleStatusRequest request, Long currentUserId, String currentUsername, HttpServletRequest servletRequest) {
        SysUserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
        }

        user.setStatus(request.status());
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        if ("DISABLED".equals(request.status())) {
            expireUserSessions(id);
            permissionChangeDispatcher.fireUserStatusChange(id, currentUserId, currentUsername, "用户被禁用");
        }

        auditService.record(AuditEventType.USER_STATUS_CHANGE, true, currentUsername, currentUserId, servletRequest);
    }

    /**
     * 批量为一组用户分配角色（或清除角色）。
     *
     * @param userIds        需要分配角色的用户 ID 列表（不能为空）
     * @param roleIds        角色 ID 列表，若为空则表示清除所有角色
     * @param currentUserId  操作人的用户 ID
     * @param currentUsername 操作人用户名（审计用）
     * @param servletRequest 当前请求对象（审计用）
     */
    @Transactional
    public void assignRolesBatch(List<Long> userIds,
                                 List<Long> roleIds,
                                 Long currentUserId,
                                 String currentUsername,
                                 HttpServletRequest servletRequest) {
        // 1. 参数校验
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户ID列表不能为空");
        }
        // 2. 校验所有用户是否存在
        List<SysUserEntity> users = userMapper.selectBatchIds(userIds);
        if (users.size() != userIds.size()) {
            // 找出不存在的 ID，提供更友好的错误信息
            List<Long> found = users.stream().map(SysUserEntity::getId).toList();
            List<Long> missing = userIds.stream().filter(id -> !found.contains(id)).toList();
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在: " + missing);
        }
        // 3. 若提供了角色列表，校验角色是否全部存在
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                    .in(SysRoleEntity::getId, roleIds));
            if (roles.size() != roleIds.size()) {
                List<Long> found = roles.stream().map(SysRoleEntity::getId).toList();
                List<Long> missing = roleIds.stream().filter(r -> !found.contains(r)).toList();
                throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在: " + missing);
            }
        }
        // 4. 删除旧的用户‑角色关联（一次性删除所有目标用户的记录）
        LambdaQueryWrapper<SysUserRoleEntity> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.in(SysUserRoleEntity::getUserId, userIds);
        userRoleMapper.delete(deleteWrapper);
        // 5. 如果 roleIds 为非空，则批量插入新关联
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long uid : userIds) {
                for (Long rid : roleIds) {
                    SysUserRoleEntity userRole = new SysUserRoleEntity();
                    userRole.setUserId(uid);
                    userRole.setRoleId(rid);
                    userRole.setCreatedAt(LocalDateTime.now());
                    userRoleMapper.insert(userRole);
                }
            }
        }
        for (Long uid : userIds) {
            permissionChangeDispatcher.fireUserRoleChange(uid, currentUserId, currentUsername, "用户角色分配变更");
        }
        // 6. 记录审计日志（使用 USER_UPDATE 统一审计）
        auditService.record(AuditEventType.USER_UPDATE, true, currentUsername, currentUserId, servletRequest);
    }

    // ========== 用户偏好私有方法 =========

    @Transactional
    public ResetPasswordResult resetPassword(Long id, Long currentUserId, String currentUsername, HttpServletRequest servletRequest) {
        SysUserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setNeedPasswordChange(true);
        user.setUpdatedBy(currentUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        expireUserSessions(id);

        auditService.record(AuditEventType.USER_PASSWORD_RESET, true, currentUsername, currentUserId, servletRequest);
        return new ResetPasswordResult(newPassword);
    }

    public void export(UserExportRequest request, HttpServletResponse response) {
        log.info("开始导出用户数据，请求参数: deptId={}, ids={}, username={}, status={}",
                request.deptId(), request.ids(), request.username(), request.status());

        List<UserExportData> dataList;

        if (request.deptId() != null) {
            List<Long> deptIds = getDeptIdsByDeptId(request.deptId());
            log.info("查询到的部门ID列表: {}", deptIds);

            if (deptIds.isEmpty()) {
                log.warn("导出用户时指定的部门[{}]不存在", request.deptId());
                writeEmptyExportFile(response);
                return;
            }

            List<Long> userIds = getUserIdsByDeptIds(deptIds);
            log.info("部门[{}]下的用户ID列表: {}", request.deptId(), userIds);

            if (userIds.isEmpty()) {
                log.info("部门[{}]及其子部门下没有用户，导出空文件", request.deptId());
                writeEmptyExportFile(response);
                return;
            }

            LambdaQueryWrapper<SysUserEntity> wrapper = buildExportQueryWrapper(request);

            if (request.ids() != null && !request.ids().isEmpty()) {
                List<Long> filteredIds = userIds.stream()
                        .filter(request.ids()::contains)
                        .collect(Collectors.toList());
                log.info("部门用户与指定IDs的交集: {}", filteredIds);
                if (filteredIds.isEmpty()) {
                    log.info("指定IDs在部门中不存在，导出空文件");
                    writeEmptyExportFile(response);
                    return;
                }
                wrapper.in(SysUserEntity::getId, filteredIds);
            } else {
                wrapper.in(SysUserEntity::getId, userIds);
            }

            dataList = executeExportQuery(wrapper);
        } else {
            LambdaQueryWrapper<SysUserEntity> wrapper = buildExportQueryWrapper(request);

            if (request.ids() != null && !request.ids().isEmpty()) {
                log.info("使用指定用户ID列表进行导出: {}", request.ids());
                wrapper.in(SysUserEntity::getId, request.ids());
            } else {
                log.info("无筛选条件，导出所有用户");
            }

            dataList = executeExportQuery(wrapper);
        }

        log.info("查询到的用户数量: {}", dataList.size());
        writeExportFile(response, dataList);
    }

    private LambdaQueryWrapper<SysUserEntity> buildExportQueryWrapper(UserExportRequest request) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.username()), SysUserEntity::getUsername, request.username())
               .like(StringUtils.hasText(request.phone()), SysUserEntity::getPhone, request.phone())
               .eq(StringUtils.hasText(request.userType()), SysUserEntity::getUserType, request.userType())
               .eq(StringUtils.hasText(request.status()), SysUserEntity::getStatus, request.status())
               .orderByDesc(SysUserEntity::getCreatedAt);
        return wrapper;
    }

    private List<UserExportData> executeExportQuery(LambdaQueryWrapper<SysUserEntity> wrapper) {
        List<SysUserEntity> users = userMapper.selectList(wrapper);
        log.info("executeExportQuery: 从数据库查询到 {} 个用户实体", users.size());

        List<UserExportData> dataList = users.stream().map(user -> {
            UserVO vo = convertToUserVO(user);
            UserExportData data = new UserExportData(
                    vo.username(),
                    vo.nickname(),
                    vo.realname(),
                    vo.userType(),
                    String.join("/", vo.deptNames()),
                    vo.phone(),
                    vo.email(),
                    vo.status(),
                    vo.createdAt() != null ? vo.createdAt().toString() : ""
            );
            log.debug("转换用户导出数据: id={}, username={}, nickname={}",
                    user.getId(), data.getUsername(), data.getNickname());
            return data;
        }).collect(Collectors.toList());

        log.info("executeExportQuery: 转换完成，共 {} 条导出数据", dataList.size());

        if (!dataList.isEmpty()) {
            log.info("第一条数据示例: username={}, nickname={}, status={}",
                    dataList.get(0).getUsername(), dataList.get(0).getNickname(), dataList.get(0).getStatus());
        }

        return dataList;
    }

    private void writeEmptyExportFile(HttpServletResponse response) {
        log.info("writeEmptyExportFile: 开始写入空Excel文件");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");

        try {
            OutputStream outputStream = response.getOutputStream();
            log.info("writeEmptyExportFile: 获取输出流成功，准备写入空数据");
            EasyExcel.write(outputStream, UserExportData.class)
                    .sheet("用户数据")
                    .doWrite(Collections.emptyList());
            log.info("writeEmptyExportFile: 空文件写入完成");
            outputStream.flush();
        } catch (Exception e) {
            log.error("writeEmptyExportFile: 导出空文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出失败");
        }
    }

    private void writeExportFile(HttpServletResponse response, List<UserExportData> dataList) {
        log.info("writeExportFile: 开始写入Excel文件，数据条数: {}", dataList.size());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");

        try {
            OutputStream outputStream = response.getOutputStream();
            log.info("writeExportFile: 获取输出流成功");
            log.info("writeExportFile: UserExportData类信息: {}", UserExportData.class.getName());

            log.info("writeExportFile: 准备写入Excel...");
            EasyExcel.write(outputStream, UserExportData.class)
                    .sheet("用户数据")
                    .doWrite(dataList);
            log.info("writeExportFile: doWrite调用完成，数据已写入");

            outputStream.flush();
            log.info("writeExportFile: 输出流刷新完成");
        } catch (Exception e) {
            log.error("writeExportFile: 导出用户失败，异常类型: {}, 异常消息: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出失败: " + e.getMessage());
        }
    }

    public record ResetPasswordResult(String newPassword) {
    }

    public static class UserExportData {
        @com.alibaba.excel.annotation.ExcelProperty("用户名")
        private String username;

        @com.alibaba.excel.annotation.ExcelProperty("昵称")
        private String nickname;

        @com.alibaba.excel.annotation.ExcelProperty("真实姓名")
        private String realname;

        @com.alibaba.excel.annotation.ExcelProperty("用户类型")
        private String userType;

        @com.alibaba.excel.annotation.ExcelProperty("所属部门")
        private String deptNames;

        @com.alibaba.excel.annotation.ExcelProperty("手机号")
        private String phone;

        @com.alibaba.excel.annotation.ExcelProperty("邮箱")
        private String email;

        @com.alibaba.excel.annotation.ExcelProperty("状态")
        private String status;

        @com.alibaba.excel.annotation.ExcelProperty("创建时间")
        private String createdAt;

        public UserExportData() {
        }

        public UserExportData(String username, String nickname, String realname, String userType,
                               String deptNames, String phone, String email, String status, String createdAt) {
            this.username = username;
            this.nickname = nickname;
            this.realname = realname;
            this.userType = userType;
            this.deptNames = deptNames;
            this.phone = phone;
            this.email = email;
            this.status = status;
            this.createdAt = createdAt;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getRealname() { return realname; }
        public void setRealname(String realname) { this.realname = realname; }

        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }

        public String getDeptNames() { return deptNames; }
        public void setDeptNames(String deptNames) { this.deptNames = deptNames; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ========== 私有方法：校验 ==========

    private void checkUsernameUnique(String username) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserEntity::getUsername, username)
               .eq(SysUserEntity::getDeleted, 0);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }
    }

    private void checkPhoneUnique(String phone, Long excludeUserId) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserEntity::getPhone, phone)
               .eq(SysUserEntity::getDeleted, 0);
        if (excludeUserId != null) {
            wrapper.ne(SysUserEntity::getId, excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PHONE_DUPLICATE);
        }
    }

    private void checkEmailUnique(String email, Long excludeUserId) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserEntity::getEmail, email)
               .eq(SysUserEntity::getDeleted, 0);
        if (excludeUserId != null) {
            wrapper.ne(SysUserEntity::getId, excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }
    }

    private void validateDeptSelection(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            throw new BusinessException(ErrorCode.DEPT_NOT_SELECTED);
        }

        for (Long deptId : deptIds) {
            SysDepartmentEntity dept = deptMapper.selectById(deptId);
            if (dept == null) {
                continue;
            }
            if ("DIRECTORY".equals(dept.getDeptType())) {
                throw new BusinessException(ErrorCode.DEPT_IS_DIRECTORY, "不能选择目录类型部门: " + dept.getName());
            }
        }
    }

    // ========== 私有方法：部门处理 ==========

    private List<Long> getDeptIdsByDeptId(Long deptId) {
        SysDepartmentEntity dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return Collections.emptyList();
        }
        if ("DIRECTORY".equals(dept.getDeptType())) {
            return getAllChildDeptIds(deptId);
        }
        return Collections.singletonList(deptId);
    }

    private List<Long> getAllChildDeptIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        List<SysDepartmentEntity> children = deptMapper.selectList(
                new LambdaQueryWrapper<SysDepartmentEntity>()
                        .eq(SysDepartmentEntity::getParentId, parentId)
                        .eq(SysDepartmentEntity::getDeleted, 0));
        for (SysDepartmentEntity child : children) {
            ids.addAll(getAllChildDeptIds(child.getId()));
        }
        return ids;
    }

    private List<Long> getUserIdsByDeptIds(List<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUserDepartmentEntity> userDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDepartmentEntity>()
                        .in(SysUserDepartmentEntity::getDepartmentId, deptIds));
        return userDepts.stream()
                .map(SysUserDepartmentEntity::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    public void joinRootTempDept(Long userId) {
        SysDepartmentEntity rootTempDept = deptMapper.selectOne(
                new LambdaQueryWrapper<SysDepartmentEntity>()
                        .eq(SysDepartmentEntity::getParentId, 0)
                        .eq(SysDepartmentEntity::getDeptType, "TEMPORARY")
                        .eq(SysDepartmentEntity::getDeleted, 0));

        if (rootTempDept != null) {
            boolean alreadyExists = userDeptMapper.selectCount(
                    new LambdaQueryWrapper<SysUserDepartmentEntity>()
                            .eq(SysUserDepartmentEntity::getUserId, userId)
                            .eq(SysUserDepartmentEntity::getDepartmentId, rootTempDept.getId())) > 0;
            if (!alreadyExists) {
                SysUserDepartmentEntity userDept = new SysUserDepartmentEntity();
                userDept.setUserId(userId);
                userDept.setDepartmentId(rootTempDept.getId());
                userDept.setCreatedAt(LocalDateTime.now());
                userDeptMapper.insert(userDept);
            }
        }
    }

    private List<Long> filterOutTempDepts(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<SysDepartmentEntity> depts = deptMapper.selectBatchIds(deptIds);
        return depts.stream()
                .filter(d -> !"TEMPORARY".equals(d.getDeptType()))
                .map(SysDepartmentEntity::getId)
                .collect(Collectors.toList());
    }

    // ========== 私有方法：关联关系保存 ==========

    private void saveUserDeptRelations(Long userId, List<Long> deptIds) {
        for (Long deptId : deptIds) {
            SysUserDepartmentEntity userDept = new SysUserDepartmentEntity();
            userDept.setUserId(userId);
            userDept.setDepartmentId(deptId);
            userDept.setCreatedAt(LocalDateTime.now());
            userDeptMapper.insert(userDept);
        }
    }

    private void saveUserRoleRelations(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRoleEntity userRole = new SysUserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreatedAt(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    // ========== 私有方法：转换 & 工具 ==========

    private UserVO convertToUserVO(SysUserEntity user) {
        List<SysUserDepartmentEntity> userDepts = userDeptMapper.selectByUserId(user.getId());
        List<Long> deptIds = userDepts.stream()
                .map(SysUserDepartmentEntity::getDepartmentId)
                .collect(Collectors.toList());
        List<String> deptNames = deptIds.stream()
                .map(this::getDeptNameById)
                .filter(name -> name != null)
                .collect(Collectors.toList());

        List<SysRoleEntity> roles = roleMapper.selectByUserId(user.getId());
        List<Long> roleIds = roles.stream().map(SysRoleEntity::getId).collect(Collectors.toList());
        List<String> roleNames = roles.stream().map(SysRoleEntity::getName).collect(Collectors.toList());

        return buildUserVO(user, deptIds, deptNames, roleIds, roleNames);
    }

    private List<UserVO> batchConvertToUserVO(List<SysUserEntity> users) {
        if (users.isEmpty()) return List.of();

        List<Long> userIds = users.stream().map(SysUserEntity::getId).collect(Collectors.toList());

        List<SysUserDepartmentEntity> allUserDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDepartmentEntity>().in(SysUserDepartmentEntity::getUserId, userIds));
        List<Long> allDeptIds = allUserDepts.stream()
                .map(SysUserDepartmentEntity::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> deptNameMap = new HashMap<>();
        if (!allDeptIds.isEmpty()) {
            deptMapper.selectList(
                    new LambdaQueryWrapper<SysDepartmentEntity>().in(SysDepartmentEntity::getId, allDeptIds))
                    .forEach(dept -> deptNameMap.put(dept.getId(), dept.getName()));
        }

        Map<Long, List<Long>> deptIdsByUser = allUserDepts.stream()
                .collect(Collectors.groupingBy(
                        SysUserDepartmentEntity::getUserId,
                        Collectors.mapping(SysUserDepartmentEntity::getDepartmentId, Collectors.toList())));
        Map<Long, List<String>> deptNamesByUser = new HashMap<>();
        deptIdsByUser.forEach((uid, dids) -> deptNamesByUser.put(uid,
                dids.stream().map(id -> deptNameMap.getOrDefault(id, null))
                        .filter(n -> n != null).collect(Collectors.toList())));

        List<SysUserRoleEntity> allUserRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleEntity>().in(SysUserRoleEntity::getUserId, userIds));
        List<Long> allRoleIds = allUserRoles.stream()
                .map(SysUserRoleEntity::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> roleNameMap = new HashMap<>();
        if (!allRoleIds.isEmpty()) {
            roleMapper.selectList(
                    new LambdaQueryWrapper<SysRoleEntity>().in(SysRoleEntity::getId, allRoleIds))
                    .forEach(role -> roleNameMap.put(role.getId(), role.getName()));
        }

        Map<Long, List<Long>> roleIdsByUser = allUserRoles.stream()
                .collect(Collectors.groupingBy(
                        SysUserRoleEntity::getUserId,
                        Collectors.mapping(SysUserRoleEntity::getRoleId, Collectors.toList())));
        Map<Long, List<String>> roleNamesByUser = new HashMap<>();
        roleIdsByUser.forEach((uid, rids) -> roleNamesByUser.put(uid,
                rids.stream().map(id -> roleNameMap.getOrDefault(id, null))
                        .filter(n -> n != null).collect(Collectors.toList())));

        return users.stream().map(user -> buildUserVO(
                user,
                deptIdsByUser.getOrDefault(user.getId(), List.of()),
                deptNamesByUser.getOrDefault(user.getId(), List.of()),
                roleIdsByUser.getOrDefault(user.getId(), List.of()),
                roleNamesByUser.getOrDefault(user.getId(), List.of())
        )).collect(Collectors.toList());
    }

    private UserVO buildUserVO(SysUserEntity user, List<Long> deptIds, List<String> deptNames,
                                List<Long> roleIds, List<String> roleNames) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getRealname(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType(),
                user.getStatus(),
                deptIds,
                deptNames,
                roleIds,
                roleNames,
                user.getNeedPasswordChange(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String getDeptNameById(Long deptId) {
        SysDepartmentEntity dept = deptMapper.selectById(deptId);
        return dept != null ? dept.getName() : null;
    }

    private List<Long> parseIds(String ids) {
        List<Long> idList = new ArrayList<>();
        for (String s : ids.split(",")) {
            try {
                idList.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return idList;
    }

    private String generateRandomPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%";
        String all = upper + lower + digits + special;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        for (int i = 0; i < 4; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        List<Character> chars = sb.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(chars, random);
        return chars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    private void expireUserSessions(Long userId) {
        try {
            List<Object> principals = sessionRegistry.getAllPrincipals();
            for (Object principal : principals) {
                if (principal instanceof QingxuUserPrincipal userPrincipal
                        && userPrincipal.getId().equals(userId)) {
                    sessionRegistry.getAllSessions(principal, false)
                            .forEach(session -> session.expireNow());
                    log.info("已失效用户[{}]的所有Session", userId);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("失效用户[{}]的Session时发生异常: {}", userId, e.getMessage());
        }
    }

    // ========== 用户偏好私有方法 ==========

    private UserPreferenceResponse getDefaultPreference(Long userId) {
        return new UserPreferenceResponse(
                userId,
                getDefaultSystemSettings(),
                getDefaultThemeConfig()
        );
    }

    private SystemSettings getDefaultSystemSettings() {
        return new SystemSettings(
                false,
                true,
                true,
                true,
                true,
                "",
                "rgba(0, 0, 0, 0.15)",
                14,
                -20,
                List.of(100, 100)
        );
    }

    private ThemeConfig getDefaultThemeConfig() {
        return new ThemeConfig(
                "layoutDefaults",
                "#165DFF",
                false,
                false,
                false,
                false,
                "fadeInOut"
        );
    }

    private UserPreferenceResponse convertToResponse(SysUserPreferenceEntity entity) {
        return new UserPreferenceResponse(
                entity.getUserId(),
                convertToSystemSettings(entity),
                convertToThemeConfig(entity)
        );
    }

    private SystemSettings convertToSystemSettings(SysUserPreferenceEntity entity) {
        List<Integer> watermarkGapList = parseWatermarkGap(entity.getWatermarkGap());
        return new SystemSettings(
                entity.getCollapsed(),
                entity.getIsAccordion(),
                entity.getIsBreadcrumb(),
                entity.getIsTabs(),
                entity.getIsFooter(),
                entity.getWatermark(),
                entity.getWatermarkColor(),
                entity.getWatermarkFontSize(),
                entity.getWatermarkRotate(),
                watermarkGapList
        );
    }

    private ThemeConfig convertToThemeConfig(SysUserPreferenceEntity entity) {
        return new ThemeConfig(
                entity.getLayoutType(),
                entity.getThemeColor(),
                entity.getColorWeakMode(),
                entity.getGrayMode(),
                entity.getAsideDark(),
                entity.getDarkMode(),
                entity.getTransitionPage()
        );
    }

    private void copySystemSettings(SystemSettingsRequest request, SysUserPreferenceEntity entity) {
        if (request.collapsed() != null) {
            entity.setCollapsed(request.collapsed());
        }
        if (request.isAccordion() != null) {
            entity.setIsAccordion(request.isAccordion());
        }
        if (request.isBreadcrumb() != null) {
            entity.setIsBreadcrumb(request.isBreadcrumb());
        }
        if (request.isTabs() != null) {
            entity.setIsTabs(request.isTabs());
        }
        if (request.isFooter() != null) {
            entity.setIsFooter(request.isFooter());
        }
        if (request.watermark() != null) {
            entity.setWatermark(request.watermark());
        }
        if (request.watermarkColor() != null) {
            entity.setWatermarkColor(request.watermarkColor());
        }
        if (request.watermarkFontSize() != null) {
            entity.setWatermarkFontSize(request.watermarkFontSize());
        }
        if (request.watermarkRotate() != null) {
            entity.setWatermarkRotate(request.watermarkRotate());
        }
        if (request.watermarkGap() != null) {
            try {
                entity.setWatermarkGap(objectMapper.writeValueAsString(request.watermarkGap()));
            } catch (Exception e) {
                entity.setWatermarkGap("[100, 100]");
            }
        }
    }

    private void copyThemeConfig(ThemeConfigRequest request, SysUserPreferenceEntity entity) {
        if (request.layoutType() != null) {
            entity.setLayoutType(request.layoutType());
        }
        if (request.themeColor() != null) {
            entity.setThemeColor(request.themeColor());
        }
        if (request.colorWeakMode() != null) {
            entity.setColorWeakMode(request.colorWeakMode());
        }
        if (request.grayMode() != null) {
            entity.setGrayMode(request.grayMode());
        }
        if (request.asideDark() != null) {
            entity.setAsideDark(request.asideDark());
        }
        if (request.darkMode() != null) {
            entity.setDarkMode(request.darkMode());
        }
        if (request.transitionPage() != null) {
            entity.setTransitionPage(request.transitionPage());
        }
    }

    private List<Integer> parseWatermarkGap(String watermarkGapStr) {
        if (watermarkGapStr == null || watermarkGapStr.isBlank()) {
            return List.of(100, 100);
        }
        try {
            return objectMapper.readValue(watermarkGapStr, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return List.of(100, 100);
        }
    }
}
