package org.apache.pig.test.pigmix.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueTextInputFormat;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.mapred.jobcontrol.JobControl;
import org.apache.hadoop.mapred.lib.IdentityMapper;

import org.apache.pig.test.pigmix.mapreduce.Library;

public class L8 {

    public static class ReadPageViews extends MapReduceBase
        implements Mapper<LongWritable, Text, Text, Text> {

        public void map(
                LongWritable k,
                Text val,
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {

            // Split the line
            List<Text> fields = Library.splitLine(val, '');
            if (fields.size() != 9) return;

            StringBuffer sb = new StringBuffer();
            sb.append(fields.get(2).toString());
            sb.append("");
            sb.append(fields.get(6).toString());
            oc.collect(new Text("all"), new Text(sb.toString()));
        }
    }

    public static class Combiner extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            int tsSum = 0, erCnt = 0;
            double erSum = 0.0;
            while (iter.hasNext()) {
                List<Text> vals = Library.splitLine(iter.next(), '');
                try {
                    tsSum += Integer.valueOf(vals.get(0).toString());
                    erSum += Double.valueOf(vals.get(1).toString());
                    erCnt++;
                } catch (NumberFormatException nfe) {
                }
            }
            StringBuffer sb = new StringBuffer();
            sb.append((new Integer(tsSum)).toString());
            sb.append("");
            sb.append((new Double(erSum)).toString());
            sb.append("");
            sb.append((new Integer(erCnt)).toString());
            oc.collect(key, new Text(sb.toString()));
            reporter.setStatus("OK");
        }
    }
    public static class Group extends MapReduceBase
        implements Reducer<Text, Text, Text, Text> {

        public void reduce(
                Text key,
                Iterator<Text> iter, 
                OutputCollector<Text, Text> oc,
                Reporter reporter) throws IOException {
            int tsSum = 0, erCnt = 0;
            double erSum = 0.0;
            while (iter.hasNext()) {
                List<Text> vals = Library.splitLine(iter.next(), '');
                try {
                    tsSum += Integer.valueOf(vals.get(0).toString());
                    erSum += Double.valueOf(vals.get(1).toString());
                    erCnt++;
                } catch (NumberFormatException nfe) {
                }
            }
            double erAvg = erSum / erCnt;
            StringBuffer sb = new StringBuffer();
            sb.append((new Integer(tsSum)).toString());
            sb.append("");
            sb.append((new Double(erAvg)).toString());
            oc.collect(key, new Text(sb.toString()));
            reporter.setStatus("OK");
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length!=3) {
            System.out.println("Parameters: inputDir outputDir parallel");
            System.exit(1);
        }
        String inputDir = args[0];
        String outputDir = args[1];
        String parallel = args[2];
        JobConf lp = new JobConf(L8.class);
        lp.setJobName("L8 Load Page Views");
        lp.setInputFormat(TextInputFormat.class);
        lp.setOutputKeyClass(Text.class);
        lp.setOutputValueClass(Text.class);
        lp.setMapperClass(ReadPageViews.class);
        lp.setCombinerClass(Combiner.class);
        lp.setReducerClass(Group.class);
        Properties props = System.getProperties();
        for (Map.Entry<Object,Object> entry : props.entrySet()) {
            lp.set((String)entry.getKey(), (String)entry.getValue());
        }
        FileInputFormat.addInputPath(lp, new Path(inputDir + "/page_views"));
        FileOutputFormat.setOutputPath(lp, new Path(outputDir + "/L8out"));
        lp.setNumReduceTasks(1);
        Job group = new Job(lp);

        JobControl jc = new JobControl("L8 join");
        jc.addJob(group);

        new Thread(jc).start();
   
        int i = 0;
        while(!jc.allFinished()){
            ArrayList<Job> failures = jc.getFailedJobs();
            if (failures != null && failures.size() > 0) {
                for (Job failure : failures) {
                    System.err.println(failure.getMessage());
                }
                break;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}

            if (i % 10000 == 0) {
                System.out.println("Running jobs");
                ArrayList<Job> running = jc.getRunningJobs();
                if (running != null && running.size() > 0) {
                    for (Job r : running) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Ready jobs");
                ArrayList<Job> ready = jc.getReadyJobs();
                if (ready != null && ready.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Waiting jobs");
                ArrayList<Job> waiting = jc.getWaitingJobs();
                if (waiting != null && waiting.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
                System.out.println("Successful jobs");
                ArrayList<Job> success = jc.getSuccessfulJobs();
                if (success != null && success.size() > 0) {
                    for (Job r : ready) {
                        System.out.println(r.getJobName());
                    }
                }
            }
            i++;
        }
        ArrayList<Job> failures = jc.getFailedJobs();
        if (failures != null && failures.size() > 0) {
            for (Job failure : failures) {
                System.err.println(failure.getMessage());
            }
        }
        jc.stop();
    }

}
