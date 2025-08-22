package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class BasicInfo {
    private String name;
    private String email;
    private String phone;
    private String github;
    private String linkedin;
    private String location;

}