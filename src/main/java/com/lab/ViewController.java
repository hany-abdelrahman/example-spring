package com.lab;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.lab.models.View;
import com.lab.repositories.ViewRepository;

@RestController
public class ViewController {
    
    private static final String GET_VIEWS_COUNT_METRIC = "get-views-metric";
    
    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);
    private MetricRegistry metrics;
    
    @Autowired
    ViewRepository viewRepository;
    
    @RequestMapping("count/{videoId}")
    public String getViewCount(@PathVariable long videoId) {
        Meter meter = metrics.meter(GET_VIEWS_COUNT_METRIC);
        meter.mark();
        return "" + viewRepository.countByVideoId(videoId);
    }
    
    @RequestMapping(value = "view", method = RequestMethod.POST)
    public String saveView(@RequestBody View view){
        viewRepository.save(view);
        return "" + view.getViewId();
    }
    
    @PostConstruct
    public void init() {
        metrics = new MetricRegistry();
        
        final Slf4jReporter reporter = Slf4jReporter.forRegistry(metrics)
                .outputTo(LoggerFactory.getLogger(ViewController.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.SECONDS);
    }
}
