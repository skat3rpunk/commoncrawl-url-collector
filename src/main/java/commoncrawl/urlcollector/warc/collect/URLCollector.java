package commoncrawl.urlcollector.warc.collect;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.LongSumReducer;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import commoncrawl.urlcollector.warc.WARCFileInputFormat;

public class URLCollector extends Configured implements Tool {

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] xArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (xArgs.length != 2) {
			System.err.println("Usage: URLCollector <in> <out>");
			System.exit(1);
		}
		int res = ToolRunner.run(conf, new URLCollector(), xArgs);
		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		// for aws credentials
		Job job = new Job(conf);
		job.setJarByClass(URLCollector.class);
		job.setNumReduceTasks(1);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setInputFormatClass(WARCFileInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		job.setMapperClass(URLMapper.class);
		job.setReducerClass(LongSumReducer.class);
		return job.waitForCompletion(true) ? 0 : -1;
	}
}
