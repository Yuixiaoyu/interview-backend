package com.xiaoyu.interview.model.entity.aifileresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) //序列化时将null值去掉
public class Resume {
    private String job_target;
    private BasicInfo basic_info;
    private List<Education> education;
    private TechnicalSkills technical_skills;
    private List<Project> projects;
    private List<Internship> internships;
    private List<String> certifications;
    private AdditionalInfo additional_info;

}