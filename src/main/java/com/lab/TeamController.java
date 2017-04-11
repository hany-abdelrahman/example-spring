package com.lab;

import java.text.MessageFormat;

import javax.annotation.PostConstruct;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.hibernate.cache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lab.models.Team;
import com.lab.repositories.TeamRepository;

@RestController
public class TeamController {
    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);
    
    private CacheAccess<Long, Team> cache = null;
    
    @Autowired
    TeamRepository teamRepository;
    
    @RequestMapping("/teams")
    Iterable<Team> getTeams() {
        return teamRepository.findAll();
    }
    
    @RequestMapping("/teams/{id}")
    Team getTeam(@PathVariable long id) throws InterruptedException {
        logger.info(MessageFormat.format("Fetching team with id {0}", id));
        
        Team team = null;
        ICacheElement<Long, Team> pair = cache.getCacheElement(id);
        if (pair == null) {
            logger.info(MessageFormat.format("Item with id {0} not found in cache. Fetching from database", id));
            team = teamRepository.findOne(id);
            Thread.sleep(1000);
            if (team != null) {
                cache.put(id, team);
            }
            return team;
        } else {
            team = pair.getVal();
        }
        return team;
    }
    
    @PostConstruct
    public void init() {
        try {
            cache = JCS.getInstance("default");
        } catch (CacheException e) {
            e.printStackTrace();
        }
    }
}
