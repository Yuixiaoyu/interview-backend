package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 主类
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class ResumeDocument {
    private Resume resume;
}













