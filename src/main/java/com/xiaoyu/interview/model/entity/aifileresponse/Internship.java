package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class Internship {
    private String company;
    private String position;
    private String period;
    private List<String> responsibilities;
}