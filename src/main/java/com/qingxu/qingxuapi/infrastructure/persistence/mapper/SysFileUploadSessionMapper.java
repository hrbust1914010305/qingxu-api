package com.qingxu.qingxuapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileUploadSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysFileUploadSessionMapper extends BaseMapper<SysFileUploadSessionEntity> {

    @Update("update sys_file_upload_session set status = #{newStatus}, updated_at = current_timestamp where upload_id = #{uploadId} and status = #{currentStatus}")
    int updateStatusIfCurrent(@Param("uploadId") String uploadId,
                              @Param("currentStatus") String currentStatus,
                              @Param("newStatus") String newStatus);
}
