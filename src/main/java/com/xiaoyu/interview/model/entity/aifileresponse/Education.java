package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class Education {
    private String degree;
    private String major;
    private String university;
    private String period;
    private String gpa;
    private List<String> relevant_courses;

}