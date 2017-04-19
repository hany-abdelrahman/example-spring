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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.lab.models.View;
import com.lab.repositories.ViewRepository;

@RestController
public class ViewController {
    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);
    
    private CacheAccess<Long, View> cache = null;
    
    @Autowired
    ViewRepository viewRepository;
    
    @RequestMapping("/view")
    Iterable<View> getTeams() {
        return viewRepository.findAll();
    }
    
    @RequestMapping("/view/{videoId}/{userId}")
    View handleView(@PathVariable long id) throws InterruptedException {
        logger.info(MessageFormat.format("Fetching team with id {0}", id));
        View item = null;
        ICacheElement<Long, View> pair = cache.getCacheElement(id);
        if (pair == null) {
            logger.info(MessageFormat.format("Item with id {0} not found in cache. Fetching from database", id));
            item = viewRepository.findOne(id);
            if (item != null) {
                cache.put(id, item);
            }
            return item;
        } else {
            item = pair.getVal();
        }
        return item;
    }
    
    @RequestMapping("count/{videoId}")
    public String getViewCount(@PathVariable long videoId){
        return "" + viewRepository.countByVideoId(videoId);
    }
    
    @RequestMapping(value = "view", method = RequestMethod.POST)
    public String saveView(@RequestBody View view){
        viewRepository.save(view);
        return "" + view.getViewId();
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
