package com.lab.models;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Player implements Serializable {
    
    private static final long serialVersionUID = 1947087677205353674L;
    
    @GeneratedValue @Id
    Long id;
    String name;
    int age;
    
    public Player() {
        super();
    }
    
    public Player(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
}