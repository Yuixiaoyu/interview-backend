package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class TechnicalSkills {
    private List<String> programming_languages;
    private List<String> web_development;
    private List<String> database;
    private List<String> devops;
    private List<String> others;
}