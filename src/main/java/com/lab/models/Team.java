package com.lab.models;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Entity
public class Team implements Serializable {
    
    private static final long serialVersionUID = -1944135602492564424L;
    
    @GeneratedValue @Id
    Long id;
    String name;
    String sponsor;
    
    @OneToMany(cascade=CascadeType.ALL) @JoinColumn(name="teamId")
    Set<Player> players;
    
    public Team() {
        super();
    }
    
    public Team(String name, String sponsor, Set<Player> players) {
        this.name = name;
        this.sponsor = sponsor;
        this.players = players;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSponsor() {
        return sponsor;
    }
    
    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }
    
    public Set<Player> getPlayers() {
        return players;
    }
    
    public void setPlayers(Set<Player> players) {
        this.players = players;
    }
}
