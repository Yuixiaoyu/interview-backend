package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class Project {
    private String name;
    private String period;
    private List<String> technologies;
    private String description;
    private List<String> achievements;

}