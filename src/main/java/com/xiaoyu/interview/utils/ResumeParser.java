package com.xiaoyu.interview.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResumeParser {

    public static Map<String, Object> parseParagraphs(List<String> paragraphs){
        List<Map<String,String>> educationList = new ArrayList<>();
        List<Map<String,String>> workList = new ArrayList<>();
        List<Map<String,String>> projectList = new ArrayList<>();
        List<String> skillList = new ArrayList<>();

        for(String p : paragraphs){
            if(p.contains("教育") || p.matches(".*大学.*")) {
                Map<String,String> edu = new HashMap<>();
                edu.put("text", p);
                educationList.add(edu);
            } else if(p.contains("工作经历") || p.contains("公司") || p.contains("职位")) {
                Map<String,String> work = new HashMap<>();
                work.put("text", p);
                workList.add(work);
            } else if(p.contains("项目") || p.contains("项目经验")) {
                Map<String,String> proj = new HashMap<>();
                proj.put("text", p);
                projectList.add(proj);
            } else if(p.contains("技能") || p.contains("证书")) {
                skillList.add(p);
            }
        }

        Map<String,Object> result = new HashMap<>();
        result.put("education", educationList);
        result.put("work", workList);
        result.put("projects", projectList);
        result.put("skills", skillList);

        return result;
    }
}
