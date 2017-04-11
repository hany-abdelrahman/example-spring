package com.lab;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import com.lab.models.Player;
import com.lab.models.Team;
import com.lab.repositories.TeamRepository;

@SpringBootApplication
public class LabFirstApplication extends SpringBootServletInitializer {

    @Autowired
    TeamRepository teamRepository;
    
    public static void main(String[] args) {
        SpringApplication.run(LabFirstApplication.class, args);
    }
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(LabFirstApplication.class);
    }
    
    @PostConstruct
    public void init() {
        Player p1 = new Player("Christiano Ronaldo", 32);
        Player p2 = new Player("Iniesta", 34);
        Player p3 = new Player("Tutti", 41);
        Player p4 = new Player("Luis Figo", 42);
        Player p5 = new Player("Wayne Roney", 38);
        
        Set<Player> teamOnePlayers = new HashSet<>(Arrays.asList(p1, p2));
        Set<Player> teamTwoPlayers = new HashSet<>(Arrays.asList(p3, p4, p5));
        
        Team t1 = new Team("Manchester United", "Coca Cola", teamOnePlayers);
        Team t2 = new Team("Chelse", "BMW", teamTwoPlayers);
       
        teamRepository.save(Arrays.asList(t1, t2));
    }
}
