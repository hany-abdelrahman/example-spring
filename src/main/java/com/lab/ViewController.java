package com.lab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.lab.models.View;

@RestController
public class ViewController {

    private static final String GET_VIEWS_COUNT_METRIC = "get-views-metric";
    private static final int BATCH_SIZE = 1;

    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);
    private MetricRegistry metrics;
    private Configuration conf;

    private List<View> buffer;

    // @Autowired
    // ViewRepository viewRepository;

    @Value("${graphite.hostname}")
    private String graphiteHostName;

    @Value("${graphite.port}")
    private int graphitePort;

    @Value("${hdfs.uri}")
    private String hdfsUri;
    //
    // @RequestMapping("count/{videoId}")
    // public String getViewCount(@PathVariable long videoId) {
    // Meter meter = metrics.meter(GET_VIEWS_COUNT_METRIC);
    // meter.mark();
    // return "" + viewRepository.countByVideoId(videoId);
    // }
    //
    // @RequestMapping(value = "view", method = RequestMethod.POST)
    // public String saveView(@RequestBody View view){
    // viewRepository.save(view);
    // return "" + view.getViewId();
    // }

    @RequestMapping(value = "view", method = RequestMethod.POST)
    public String saveRequest(@RequestBody View view) {
        logger.info("Received view request");
        buffer.add(view);
        
        if (buffer.size() >= BATCH_SIZE) {
            try {
                flushBuffer();
            } 
            catch (Exception e) {
                logger.error(e.getMessage());
            } 
            finally {
                buffer.clear();
            }
        }
        return "SUCCESS";
    }
    
    private boolean flushBuffer() throws IOException {
        synchronized (buffer) {
            FSDataOutputStream outputStream = null;
            FileSystem fs = null;
            try {
                fs = FileSystem.get(URI.create(hdfsUri), conf);
                
                String path = "/home/db/";
                String fileName = "PART-001.csv";
                
                Path newFolderPath = new Path(path);
                
                if (!fs.exists(newFolderPath)) {
                    // Create new Directory
                    fs.mkdirs(newFolderPath);
                }
                // ==== Write file
                // Create a path
                Path hdfswritepath = new Path(newFolderPath + "/" + fileName);
                // Init output stream
                if (!fs.exists(hdfswritepath)) {
                    outputStream = fs.create(hdfswritepath);
                } else {
                    outputStream = fs.append(hdfswritepath);
                }
                
                for (View view : buffer) {
                    outputStream.writeBytes(getViewAsString(view));
                }
                
                logger.info("End Write file into hdfs");
            } 
            catch (Exception e) {
                logger.info(e.getMessage());
                return false;
            } 
            finally {
                outputStream.close();
                fs.close();
                buffer.clear();
            }
        }
        return true;
    }

    private String getViewAsString(View view) {
        final String SEPARATOR = ",";
        final String LINE_TERMINATOR = "\n";
        String id = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        
        sb.append(id);
        sb.append(SEPARATOR);
        sb.append(view.getVideoId());
        sb.append(SEPARATOR);
        sb.append(view.getUserId());
        sb.append(SEPARATOR);
        sb.append(view.getDevice());
        sb.append(SEPARATOR);
        sb.append(view.getLocation());
        sb.append(SEPARATOR);
        sb.append(view.getTimeStamp());
        sb.append(LINE_TERMINATOR);

        return sb.toString();
    }

    @PostConstruct
    public void init() {
        // metrics = new MetricRegistry();
        //
        // final Graphite graphite = new Graphite(new
        // InetSocketAddress(graphiteHostName, graphitePort));
        // final GraphiteReporter reporter =
        // GraphiteReporter.forRegistry(metrics)
        // .prefixedWith("web1.example.com")
        // .convertRatesTo(TimeUnit.SECONDS)
        // .convertDurationsTo(TimeUnit.MILLISECONDS)
        // .filter(MetricFilter.ALL)
        // .build(graphite);
        // reporter.start(1, TimeUnit.SECONDS);
        
        conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        conf.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", LocalFileSystem.class.getName());
        conf.set("dfs.replication", "1");
        System.setProperty("HADOOP_USER_NAME", "hdfs");
        System.setProperty("hadoop.home.dir", "/");
        
        buffer = Collections.synchronizedList(new ArrayList<>(BATCH_SIZE));
    }
}
