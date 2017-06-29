package com.lab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.tracing.SpanReceiverHost;
import org.htrace.Sampler;
import org.htrace.Trace;
import org.htrace.TraceScope;
import org.htrace.impl.ProbabilitySampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.lab.models.View;

@RestController
public class ViewController {
    private static final String TOTAL_VIEWS_TABLE = "views_total_count";

    @Value("${graphite.metricname}")
    private String metricName;

    @Value("${hdfs.batch.size}")
    private int BATCH_SIZE;

    @Value("${graphite.hostname}")
    private String graphiteHostName;

    @Value("${graphite.port}")
    private int graphitePort;

    @Value("${hdfs.uri}")
    private String hdfsUri;

    @Value("${instance.id}")
    private int INSTANCE_ID;

    private static final Logger logger = LoggerFactory.getLogger(ViewController.class);
    private MetricRegistry metrics;

    private FileSystem fs;
    private HTable viewsHTable;

    private List<View> buffer;

    @RequestMapping(value = "count", method = RequestMethod.GET)
    public String getViewsCount(int itemId) {
        Meter meter = metrics.meter(metricName + "-read");
        meter.mark();

        try {
            Get get = new Get(Bytes.toBytes(itemId));
            get.addColumn(Bytes.toBytes("item"), Bytes.toBytes("views_count"));
            Result result = viewsHTable.get(get);
            byte[] count = result.getValue(Bytes.toBytes("item"), Bytes.toBytes("views_count"));
            if (count != null) {
                return Bytes.toString(count);
            }
        }
        catch (Exception e) {

        }
        return "-1";
    }

    @RequestMapping(value = "view", method = RequestMethod.POST)
    public String saveRequest(@RequestBody View view) {
        Meter meter = metrics.meter(metricName + "-write");
        meter.mark();

        logger.info("Received view request");
        SpanReceiverHost.getInstance(new HdfsConfiguration());
        buffer.add(view);
        TraceScope ts = null;
        try {
            ts = Trace.startSpan("Backend Received Request", new ProbabilitySampler(0.001));
            if (buffer.size() >= BATCH_SIZE) {
                flushBuffer();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
        finally {
            if (ts != null) {
                ts.close();
            }
        }
        return "SUCCESS";
    }

    private boolean flushBuffer() throws IOException {
        List<View> bufferCopy = null;
        synchronized (buffer) {
            bufferCopy = new ArrayList<>(buffer);
            buffer.clear();
        }
        FSDataOutputStream outputStream = null;
        TraceScope ts = null;
        try {
            ts = Trace.startSpan("Write to HDFS", Sampler.ALWAYS);
            String part_name = String.format("%03d", INSTANCE_ID);
            String path = "/home/db/";
            String fileName = MessageFormat.format("PART-{0}.csv", part_name);
            Path newFolderPath = new Path(path);

            if (!fs.exists(newFolderPath)) {
                fs.mkdirs(newFolderPath);
            }

            // Create a path
            Path hdfswritepath = new Path(newFolderPath + "/" + fileName);

            // Init output stream
            if (!fs.exists(hdfswritepath)) {
                outputStream = fs.create(hdfswritepath);
            } else {
                outputStream = fs.append(hdfswritepath);
            }

            for (View view : bufferCopy) {
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
            bufferCopy.clear();
            if (ts != null) {
                ts.close();
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
    public void init() throws IOException {
        metrics = new MetricRegistry();

         final Graphite graphite = new Graphite(new
         InetSocketAddress(graphiteHostName, graphitePort));
         final GraphiteReporter reporter =
         GraphiteReporter.forRegistry(metrics)
             .prefixedWith("Metrics")
             .convertRatesTo(TimeUnit.SECONDS)
             .convertDurationsTo(TimeUnit.MILLISECONDS)
             .filter(MetricFilter.ALL)
             .build(graphite);
         reporter.start(1, TimeUnit.SECONDS);

        Configuration conf = new Configuration();
        conf.setQuietMode(false);
        fs = FileSystem.get(URI.create(hdfsUri), conf);
        viewsHTable = new HTable(HBaseConfiguration.create(), TOTAL_VIEWS_TABLE);

        System.setProperty("HADOOP_USER_NAME", "hdfs");
        System.setProperty("hadoop.home.dir", "/");

        buffer = Collections.synchronizedList(new ArrayList<View>(BATCH_SIZE));
    }
}
