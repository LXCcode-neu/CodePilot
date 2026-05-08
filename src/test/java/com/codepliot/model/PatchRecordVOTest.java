package com.codepliot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.entity.PatchRecord;
import org.junit.jupiter.api.Test;

class PatchRecordVOTest {

    @Test
    void shouldExposeStructuredDiffAndPullRequestPreview() {
        PatchRecord patchRecord = new PatchRecord();
        patchRecord.setId(1L);
        patchRecord.setTaskId(42L);
        patchRecord.setAnalysis("验证码生成使用 5 位。");
        patchRecord.setSolution("将验证码长度从 5 位改为 6 位。");
        patchRecord.setRisk("低风险。");
        patchRecord.setPatch("""
                --- a/src/main/java/com/hmdp/service/impl/UserServiceImpl.java
                +++ b/src/main/java/com/hmdp/service/impl/UserServiceImpl.java
                @@ -62,7 +62,7 @@
                     //3. 符合，生成验证码
                -    String code = RandomUtil.randomNumbers(5);
                +    String code = RandomUtil.randomNumbers(6);
                     //4. 保存验证码到 session
                """);

        PatchRecordVO vo = PatchRecordVO.from(patchRecord);

        assertEquals(1, vo.fileChanges().size());
        PatchFileChange fileChange = vo.fileChanges().get(0);
        assertEquals("src/main/java/com/hmdp/service/impl/UserServiceImpl.java", fileChange.filePath());
        assertEquals(1, fileChange.addedLines());
        assertEquals(1, fileChange.removedLines());
        assertEquals("removed", fileChange.hunks().get(0).lines().get(1).type());
        assertEquals("added", fileChange.hunks().get(0).lines().get(2).type());
        assertTrue(vo.pullRequest().ready());
        assertEquals("codepilot/task-42", vo.pullRequest().branchName());
        assertTrue(vo.pullRequest().body().contains("UserServiceImpl.java"));
    }

    @Test
    void shouldMarkPrPreviewNotReadyForEmptyPatch() {
        PatchRecord patchRecord = new PatchRecord();
        patchRecord.setTaskId(42L);
        patchRecord.setPatch("");

        PatchRecordVO vo = PatchRecordVO.from(patchRecord);

        assertTrue(vo.fileChanges().isEmpty());
        assertFalse(vo.pullRequest().ready());
    }
}
