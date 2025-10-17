package com.tasos.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "course", schema = "appdb")
public class Course {

    @Id
    @Column(name = "courseid")
    private Integer courseId;

    @Column(name = "course_name", length = 1000)
    private String courseName;

    @Column(name = "rating")
    private Double rating;

    // Constructors
    public Course() {}

    public Course(Integer courseId, String courseName, Double rating) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.rating = rating;
    }

    // Getters and setters
    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}