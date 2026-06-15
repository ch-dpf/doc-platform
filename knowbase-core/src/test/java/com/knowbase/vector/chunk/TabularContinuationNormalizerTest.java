package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabularContinuationNormalizerTest {

    @Test
    void mergesMultilineCellAfterTabularRow() {
        String input = """
                2\t海图项目\t开发接口\t\t45868\t杜鹏飞\t要求\t\t未完成\t第一点，部分功能接口仍在持续优化；

                第二点，国遥接口会进行调整与新开发。

                3\t海图项目\t部署 s57\t\t45868\t杜鹏飞\t要求\t\t已完成""";

        String merged = TabularContinuationNormalizer.joinContinuations(input);

        assertTrue(merged.contains(
                "2\t海图项目\t开发接口\t\t45868\t杜鹏飞\t要求\t\t未完成\t第一点，部分功能接口仍在持续优化； 第二点，国遥接口会进行调整与新开发。"));
        assertTrue(merged.contains("3\t海图项目\t部署 s57"));
        assertFalse(merged.contains("第一点，部分功能接口仍在持续优化；\n\n第二点"));
    }

    @Test
    void doesNotMergeUnrelatedProseParagraphs() {
        String input = "第一段说明。\n\n第二段说明。";
        String merged = TabularContinuationNormalizer.joinContinuations(input);
        assertEquals("第一段说明。\n\n第二段说明。", merged);
    }

    @Test
    void keepsSectionHeadersSeparate() {
        String input = """
                部门\t软件事业部\t\t姓名\t杜鹏飞

                2025年7月28日--8月03日

                序号\t类别\t工作内容""";
        String merged = TabularContinuationNormalizer.joinContinuations(input);
        assertTrue(merged.contains("部门\t软件事业部"));
        assertTrue(merged.contains("2025年7月28日--8月03日"));
        assertTrue(merged.contains("序号\t类别"));
    }
}
