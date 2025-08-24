package com.xiaoyu.interview.model.entity;

import lombok.Data;
import java.util.List;

@Data
public class ResumeDocument {
    private Resume resume;

    @Data
    public static class Resume {
        private String job_target;

        // Basic info fields
        private String basic_info_name;
        private String basic_info_email;
        private String basic_info_phone;
        private String basic_info_github;
        private String basic_info_linkedin;
        private String basic_info_location;

        // Education
        private List<Education> education;

        // Technical skills
        private List<String> technical_skills_programming_languages;
        private List<String> technical_skills_web_development;
        private List<String> technical_skills_database;
        private List<String> technical_skills_devops;
        private List<String> technical_skills_others;

        // Projects
        private List<Project> projects;

        // Internships
        private List<Internship> internships;

        // Certifications
        private List<String> certifications;

        // Additional info
        private List<String> additional_info_languages;
        private List<String> additional_info_interests;
    }

    @Data
    public static class Education {
        private String degree;
        private String major;
        private String university;
        private String period;
        private String gpa;
        private List<String> relevant_courses;
    }

    @Data
    public static class Project {
        private String name;
        private String period;
        private List<String> technologies;
        private String description;
        private List<String> achievements;
    }

    @Data
    public static class Internship {
        private String company;
        private String position;
        private String period;
        private List<String> responsibilities;
    }
}
