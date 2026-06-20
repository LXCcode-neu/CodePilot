package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.PatchVerificationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 补丁验证记录数据访问接口，对应数据库表 patch_verification_record。
 * <p>用于记录代码补丁的验证执行结果，包括执行的命令名称、命令文本、工作目录、退出码、
 * 是否通过、是否超时及输出摘要等信息，用于验证补丁的正确性和可靠性。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.PatchVerificationRecord
 */
@Mapper
public interface PatchVerificationRecordMapper extends BaseMapper<PatchVerificationRecord> {
}
