package com.qingxu.qingxuapi.application.department;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysDepartmentCategoryEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysDepartmentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserDepartmentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysDepartmentCategoryMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysDepartmentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserDepartmentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.interfaces.department.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentApplicationService {

    private final SysDepartmentMapper deptMapper;
    private final SysDepartmentCategoryMapper categoryMapper;
    private final SysUserDepartmentMapper userDeptMapper;
    private final SysUserMapper userMapper;

    @Autowired
    @Lazy
    private DepartmentApplicationService self;

    // ========== 部门分类业务 ==========

    public List<DeptCategoryResponse> listCategory() {
        LambdaQueryWrapper<SysDepartmentCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentCategoryEntity::getDeleted, 0)
               .orderByAsc(SysDepartmentCategoryEntity::getSortOrder);
        
        return categoryMapper.selectList(wrapper).stream()
                .map(this::convertCategoryToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createCategory(CreateDeptCategoryRequest request) {
        LambdaQueryWrapper<SysDepartmentCategoryEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysDepartmentCategoryEntity::getCode, request.code())
                   .eq(SysDepartmentCategoryEntity::getDeleted, 0);
        
        if (categoryMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(ErrorCode.DEPT_CATEGORY_CODE_DUPLICATE, "部门分类编码已存在");
        }

        SysDepartmentCategoryEntity entity = new SysDepartmentCategoryEntity();
        entity.setTenantId("default");
        entity.setName(request.name());
        entity.setCode(request.code());
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        entity.setStatus(request.status() != null ? request.status() : "ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);

        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void updateCategory(Long id, UpdateDeptCategoryRequest request) {
        SysDepartmentCategoryEntity entity = categoryMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPT_CATEGORY_NOT_FOUND, "部门分类不存在");
        }

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        categoryMapper.updateById(entity);
    }

    @Transactional
    public void deleteCategory(Long id) {
        SysDepartmentCategoryEntity entity = categoryMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPT_CATEGORY_NOT_FOUND, "部门分类不存在");
        }

        LambdaQueryWrapper<SysDepartmentEntity> deptCheckWrapper = new LambdaQueryWrapper<>();
        deptCheckWrapper.eq(SysDepartmentEntity::getCategoryId, id)
                       .eq(SysDepartmentEntity::getDeleted, 0);
        
        if (deptMapper.selectCount(deptCheckWrapper) > 0) {
            throw new BusinessException(ErrorCode.DEPT_CATEGORY_HAS_DEPTS, "该分类下有部门，无法删除");
        }

        entity.setDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(entity);
    }

    // ========== 部门业务 ==========

    public List<DepartmentTreeResponse> getTree(String name, String status) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getDeleted, 0)
               .eq(SysDepartmentEntity::getTenantId, "default");

        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(SysDepartmentEntity::getName, name.trim());
            log.info("部门树筛选 - 名称模糊搜索: {}", name.trim());
        }

        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(SysDepartmentEntity::getStatus, status.trim().toUpperCase());
            log.info("部门树筛选 - 状态精确匹配: {}", status.trim().toUpperCase());
        }

        wrapper.orderByAsc(SysDepartmentEntity::getSortOrder);

        List<SysDepartmentEntity> allDepts = deptMapper.selectList(wrapper);

        if (allDepts.isEmpty()) {
            log.info("部门树查询结果为空");
            return new ArrayList<>();
        }

        log.info("部门树查询完成，共{}个节点", allDepts.size());

        Map<Long, List<LeaderUser>> leaderMap = buildLeaderMap(allDepts);
        return buildTree(allDepts, 0L, leaderMap);
    }

    public DepartmentResponse getDetail(Long id) {
        SysDepartmentEntity entity = deptMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在");
        }
        return convertToResponse(entity);
    }

    @Transactional
    public Long create(CreateDepartmentRequest request) {
        if (request.parentId() != 0) {
            SysDepartmentEntity parent = deptMapper.selectById(request.parentId());
            if (parent == null || parent.getDeleted() == 1) {
                throw new BusinessException(ErrorCode.PARENT_DEPT_NOT_FOUND, "上级部门不存在");
            }
            
            checkAndConvertParentToDirectory(parent);
        }

        LambdaQueryWrapper<SysDepartmentEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysDepartmentEntity::getParentId, request.parentId())
                   .eq(SysDepartmentEntity::getName, request.name());
        
        if (deptMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(ErrorCode.DEPT_NAME_DUPLICATE, "同级部门名称已存在");
        }

        SysDepartmentEntity entity = new SysDepartmentEntity();
        entity.setTenantId("default");
        entity.setParentId(request.parentId());
        entity.setName(request.name());
        entity.setDeptType("DEPARTMENT");
        entity.setLeaderId(request.leaderId());
        entity.setLeader(request.leader());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        entity.setStatus(request.status() != null ? request.status() : "ACTIVE");
        entity.setDescription(request.description());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setDeleted(0);

        deptMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void update(Long id, UpdateDepartmentRequest request) {
        SysDepartmentEntity entity = deptMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在");
        }

        boolean isRootTemp = isRootTempDepartment(entity);

        if (isRootTemp) {
            log.info("编辑根目录临时部门[{}]，将应用特殊规则", id);
        }

        if (isRootTemp && request.name() != null && !request.name().equals(entity.getName())) {
            throw new BusinessException(ErrorCode.DEPT_ROOT_TEMP_CANNOT_RENAME,
                                     "根目录临时部门不允许修改名称");
        }

        if (!isRootTemp && request.name() != null && !request.name().equals(entity.getName())) {
            LambdaQueryWrapper<SysDepartmentEntity> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(SysDepartmentEntity::getParentId, entity.getParentId())
                       .eq(SysDepartmentEntity::getName, request.name())
                       .eq(SysDepartmentEntity::getDeleted, 0)
                       .ne(SysDepartmentEntity::getId, id);

            if (deptMapper.selectCount(checkWrapper) > 0) {
                throw new BusinessException(ErrorCode.DEPT_NAME_DUPLICATE, "同级部门名称已存在");
            }
            entity.setName(request.name());
        }

        if (isRootTemp && request.status() != null && !request.status().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.DEPT_ROOT_TEMP_CANNOT_CHANGE_STATUS,
                                     "根目录临时部门不允许修改状态");
        }

        if (!isRootTemp && request.status() != null) {
            entity.setStatus(request.status());
        }

        if (request.leaderId() != null) {
            entity.setLeaderId(request.leaderId());
        }
        if (request.leader() != null) {
            entity.setLeader(request.leader());
        }
        if (request.phone() != null) {
            entity.setPhone(request.phone());
        }
        if (request.email() != null) {
            entity.setEmail(request.email());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(entity);

        if (isRootTemp) {
            log.info("根目录临时部门[{}]编辑成功（仅允许修改非关键字段）", id);
        } else {
            log.info("部门[{}]编辑成功 (类型: {})", id, entity.getDeptType());
        }
    }

    public DeleteDepartmentResponse delete(List<Long> ids) {
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (Long id : ids) {
            try {
                Long parentId = self.doDeleteInNewTransaction(id);
                successCount++;
                log.info("成功删除部门[{}]，父节点ID: {}", id, parentId);

                if (parentId != null && parentId > 0) {
                    tryOptimizeParentType(parentId);
                }
            } catch (BusinessException e) {
                failCount++;
                String reason = "部门ID " + id + ": " + e.getMessage();
                failReasons.add(reason);
                log.warn("删除部门失败: {}", reason);
            }
        }

        if (successCount == 0 && failCount > 0) {
            log.error("批量删除部门全部失败: {}", String.join("; ", failReasons));
            throw new BusinessException(ErrorCode.DEPT_DELETE_ALL_FAILED, 
                                     "删除失败: " + String.join("; ", failReasons));
        }

        if (failCount > 0) {
            log.warn("批量删除部门部分成功: 成功{}个, 失败{}个", successCount, failCount);
        } else {
            log.info("批量删除部门全部成功: 共{}个", successCount);
        }

        return new DeleteDepartmentResponse(successCount, failCount, failReasons);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long doDeleteInNewTransaction(Long id) {
        return deleteSingle(id);
    }

    private void tryOptimizeParentType(Long parentId) {
        try {
            checkAndConvertParentBackToDepartment(parentId);
            log.info("成功优化父节点[{}]类型", parentId);
        } catch (Exception e) {
            log.warn("自动优化父节点[{}]类型失败（不影响删除结果）: {}", parentId, e.getMessage());
        }
    }

    public void updateStatus(Long id, UpdateDepartmentStatusRequest request) {
        SysDepartmentEntity entity = deptMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在");
        }

        entity.setStatus(request.status());
        entity.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(entity);
    }

    // ========== 私有方法：核心业务逻辑 ==========

    private void checkAndConvertParentToDirectory(SysDepartmentEntity parent) {
        if ("DEPARTMENT".equals(parent.getDeptType())) {
            handleTypeConvertToDirectory(parent);
        }
    }

    private void handleTypeConvertToDirectory(SysDepartmentEntity dept) {
        List<SysUserDepartmentEntity> userDepts = userDeptMapper.selectByDeptId(dept.getId());

        if (!userDepts.isEmpty()) {
            log.info("父部门[{}]({})有{}个用户，需创建临时部门", 
                    dept.getId(), dept.getName(), userDepts.size());

            SysDepartmentEntity tempDept = new SysDepartmentEntity();
            tempDept.setTenantId(dept.getTenantId());
            tempDept.setParentId(dept.getId());
            tempDept.setName(dept.getName() + "-临时");
            tempDept.setDeptType("TEMPORARY");
            tempDept.setSortOrder(-1);
            tempDept.setStatus("ACTIVE");
            tempDept.setDescription("系统自动创建的临时部门");
            tempDept.setCreatedAt(LocalDateTime.now());
            tempDept.setDeleted(0);
            deptMapper.insert(tempDept);

            transferUsersToDept(dept.getId(), tempDept.getId());
            
            log.info("已创建临时部门[{}]并转移{}个用户", tempDept.getId(), userDepts.size());
        } else {
            log.info("父部门[{}]({})无用户，无需创建临时部门", dept.getId(), dept.getName());
        }

        dept.setDeptType("DIRECTORY");
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        
        log.info("父部门[{}]类型已转换为DIRECTORY", dept.getId());
    }

    private Long deleteSingle(Long id) {
        SysDepartmentEntity entity = deptMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在");
        }

        if (isRootTempDepartment(entity)) {
            throw new BusinessException(ErrorCode.DEPT_ROOT_TEMP_CANNOT_DELETE, "根目录临时部门不允许删除");
        }

        LambdaQueryWrapper<SysDepartmentEntity> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysDepartmentEntity::getParentId, id)
                   .eq(SysDepartmentEntity::getDeleted, 0);

        if (deptMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN, "存在子部门无法删除");
        }

        Long parentId = entity.getParentId();
        String deptName = entity.getName();

        List<SysUserDepartmentEntity> userDepts = userDeptMapper.selectByDeptId(id);
        if (!userDepts.isEmpty()) {
            createTempDepartmentAndTransferUsers(parentId, deptName, userDepts);
        }

        int rows = deptMapper.logicDeleteById(id);
        log.info("逻辑删除部门[{}]，影响行数: {}", id, rows);

        if (rows == 0) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "部门不存在或已被删除");
        }

        return parentId;
    }

    private void createTempDepartmentAndTransferUsers(Long parentId, String originalDeptName, List<SysUserDepartmentEntity> userDepts) {
        SysDepartmentEntity tempDept = new SysDepartmentEntity();
        tempDept.setTenantId("default");
        tempDept.setParentId(parentId);
        tempDept.setName("原" + originalDeptName + "-临时");
        tempDept.setDeptType("TEMPORARY");
        tempDept.setSortOrder(-1);
        tempDept.setStatus("ACTIVE");
        tempDept.setDescription("删除部门时自动创建的临时部门，用于暂存员工");
        tempDept.setCreatedAt(LocalDateTime.now());
        tempDept.setDeleted(0);
        deptMapper.insert(tempDept);

        log.info("为删除的部门[{}]创建临时部门[{}], ID: {}, 暂存用户数: {}", 
                 originalDeptName, tempDept.getName(), tempDept.getId(), userDepts.size());

        for (SysUserDepartmentEntity userDept : userDepts) {
            userDeptMapper.deleteById(userDept.getId());

            SysUserDepartmentEntity newUserDept = new SysUserDepartmentEntity();
            newUserDept.setUserId(userDept.getUserId());
            newUserDept.setDepartmentId(tempDept.getId());
            newUserDept.setCreatedAt(LocalDateTime.now());
            userDeptMapper.insert(newUserDept);

            log.debug("转移用户[{}]从原部门到临时部门[{}]", userDept.getUserId(), tempDept.getId());
        }
    }

    private void checkAndConvertParentBackToDepartment(Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }

        SysDepartmentEntity parent = deptMapper.selectById(parentId);
        if (parent == null || parent.getDeleted() == 1 || !"DIRECTORY".equals(parent.getDeptType())) {
            return;
        }

        long childCount = getChildCount(parentId);
        if (childCount == 0) {
            parent.setDeptType("DEPARTMENT");
            parent.setUpdatedAt(LocalDateTime.now());
            deptMapper.updateById(parent);

            cleanupTemporaryChildDepts(parentId);
        }
    }

    private long getChildCount(Long parentId) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getParentId, parentId)
              .eq(SysDepartmentEntity::getDeleted, 0)
              .ne(SysDepartmentEntity::getDeptType, "TEMPORARY");
        
        return deptMapper.selectCount(wrapper);
    }

    private void cleanupTemporaryChildDepts(Long parentId) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getParentId, parentId)
              .eq(SysDepartmentEntity::getDeptType, "TEMPORARY")
              .eq(SysDepartmentEntity::getDeleted, 0);
        
        List<SysDepartmentEntity> tempDepts = deptMapper.selectList(wrapper);
        
        for (SysDepartmentEntity tempDept : tempDepts) {
            transferUsersFromTempDept(tempDept.getId(), parentId);
            
            tempDept.setDeleted(1);
            tempDept.setUpdatedAt(LocalDateTime.now());
            deptMapper.updateById(tempDept);
        }
    }

    private void transferUsersFromTempDept(Long fromTempDeptId, Long toDeptId) {
        List<SysUserDepartmentEntity> userDepts = userDeptMapper.selectByDeptId(fromTempDeptId);
        for (SysUserDepartmentEntity userDept : userDepts) {
            boolean alreadyExists = checkUserInDept(userDept.getUserId(), toDeptId);
            if (!alreadyExists) {
                userDeptMapper.deleteById(userDept.getId());
                
                SysUserDepartmentEntity newUserDept = new SysUserDepartmentEntity();
                newUserDept.setUserId(userDept.getUserId());
                newUserDept.setDepartmentId(toDeptId);
                newUserDept.setCreatedAt(LocalDateTime.now());
                userDeptMapper.insert(newUserDept);
            } else {
                userDeptMapper.deleteById(userDept.getId());
            }
        }
    }

    private boolean checkUserInDept(Long userId, Long deptId) {
        LambdaQueryWrapper<SysUserDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDepartmentEntity::getUserId, userId)
              .eq(SysUserDepartmentEntity::getDepartmentId, deptId);
        
        return userDeptMapper.selectCount(wrapper) > 0;
    }

    // ========== 私有方法：基础工具方法 ==========

    private List<DepartmentTreeResponse> buildTree(List<SysDepartmentEntity> allDepts, Long parentId, Map<Long, List<LeaderUser>> leaderMap) {
        List<DepartmentTreeResponse> result = new ArrayList<>();

        for (SysDepartmentEntity dept : allDepts) {
            if (dept.getParentId().equals(parentId)) {
                List<DepartmentTreeResponse> children = buildTree(allDepts, dept.getId(), leaderMap);

                DepartmentTreeResponse treeNode = new DepartmentTreeResponse(
                        dept.getId(),
                        dept.getTenantId(),
                        dept.getParentId(),
                        dept.getName(),
                        dept.getDeptType(),
                        dept.getCategoryId(),
                        dept.getLeader(),
                        dept.getLeaderId(),
                        leaderMap.getOrDefault(dept.getId(), List.of()),
                        dept.getPhone(),
                        dept.getEmail(),
                        dept.getSortOrder(),
                        dept.getStatus(),
                        dept.getDescription(),
                        dept.getCreatedAt(),
                        children
                );
                result.add(treeNode);
            }
        }

        return result;
    }

    private Map<Long, List<LeaderUser>> buildLeaderMap(List<SysDepartmentEntity> allDepts) {
        List<Long> leaderIds = allDepts.stream()
                .map(SysDepartmentEntity::getLeaderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (leaderIds.isEmpty()) {
            return Map.of();
        }

        List<SysUserEntity> users = userMapper.selectBatchIds(leaderIds);
        Map<Long, SysUserEntity> userMap = users.stream()
                .collect(Collectors.toMap(SysUserEntity::getId, u -> u));

        Map<Long, List<LeaderUser>> result = new HashMap<>();
        for (SysDepartmentEntity dept : allDepts) {
            if (dept.getLeaderId() != null) {
                SysUserEntity user = userMap.get(dept.getLeaderId());
                if (user != null) {
                    result.put(dept.getId(), List.of(new LeaderUser(
                            user.getId(),
                            user.getUsername(),
                            user.getRealname(),
                            user.getNickname(),
                            user.getPhone(),
                            user.getEmail()
                    )));
                }
            }
        }
        return result;
    }

    private boolean isRootTempDepartment(SysDepartmentEntity entity) {
        return entity.getParentId().equals(0L) && "TEMPORARY".equals(entity.getDeptType());
    }

    private void transferUsersToRootTempDept(Long fromDeptId) {
        SysDepartmentEntity rootTempDept = getRootTempDepartment();
        if (rootTempDept != null) {
            transferUsersToDept(fromDeptId, rootTempDept.getId());
        }
    }

    private SysDepartmentEntity getRootTempDepartment() {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getParentId, 0)
              .eq(SysDepartmentEntity::getDeptType, "TEMPORARY")
              .eq(SysDepartmentEntity::getDeleted, 0);
        
        return deptMapper.selectOne(wrapper);
    }

    private void transferUsersToDept(Long fromDeptId, Long toDeptId) {
        List<SysUserDepartmentEntity> userDepts = userDeptMapper.selectByDeptId(fromDeptId);
        for (SysUserDepartmentEntity userDept : userDepts) {
            userDeptMapper.deleteById(userDept.getId());
            
            SysUserDepartmentEntity newUserDept = new SysUserDepartmentEntity();
            newUserDept.setUserId(userDept.getUserId());
            newUserDept.setDepartmentId(toDeptId);
            newUserDept.setCreatedAt(LocalDateTime.now());
            userDeptMapper.insert(newUserDept);
        }
    }

    private DeptCategoryResponse convertCategoryToResponse(SysDepartmentCategoryEntity entity) {
        return new DeptCategoryResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getCode(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DepartmentResponse convertToResponse(SysDepartmentEntity entity) {
        List<LeaderUser> leaderUsers = List.of();
        if (entity.getLeaderId() != null) {
            SysUserEntity user = userMapper.selectById(entity.getLeaderId());
            if (user != null) {
                leaderUsers = List.of(new LeaderUser(
                        user.getId(),
                        user.getUsername(),
                        user.getRealname(),
                        user.getNickname(),
                        user.getPhone(),
                        user.getEmail()
                ));
            }
        }

        return new DepartmentResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getParentId(),
                entity.getName(),
                entity.getDeptType(),
                entity.getCategoryId(),
                entity.getLeader(),
                entity.getLeaderId(),
                leaderUsers,
                entity.getPhone(),
                entity.getEmail(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}