package com.codepliot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codepliot.entity.PatchReviewRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 补丁评审记录数据访问接口，对应数据库表 patch_review_record。
 * <p>用于记录 AI 对代码补丁的评审结果，包括使用的评审模型、是否通过、评分、风险等级、
 * 评审摘要、发现的问题、改进建议及原始响应等信息。继承 MyBatis-Plus 的 BaseMapper，提供基本的 CRUD 操作。</p>
 *
 * @author CodePilot
 * @see com.codepliot.entity.PatchReviewRecord
 */
@Mapper
public interface PatchReviewRecordMapper extends BaseMapper<PatchReviewRecord> {
}
