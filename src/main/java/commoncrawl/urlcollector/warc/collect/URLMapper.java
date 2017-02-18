package commoncrawl.urlcollector.warc.collect;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;

public class URLMapper extends Mapper<Text, ArchiveReader, Text, LongWritable> {
	private Text outKey = new Text();
	public static String URL_KEYWORD = "";
	private final String FORM_HTML_TAG = "<form(\"[^\"]*\"|'[^']*'|[^'\">])*>";
	private final String HTML_LANG_TAG = "<html.+?\\slang=[\'\"](.+?)[\'\"]\\s.+?>";
	private static final Logger LOG = Logger.getLogger(URLMapper.class);
	private Pattern formTagPattern;
	private Matcher formTagMatcher;
	private Pattern langTagPattern;
	private Matcher langTagMatcher;

	protected void map(Text key, ArchiveReader value, Context context) throws IOException, InterruptedException {
		formTagPattern = Pattern.compile(FORM_HTML_TAG, Pattern.CASE_INSENSITIVE);
		langTagPattern = Pattern.compile(HTML_LANG_TAG, Pattern.CASE_INSENSITIVE);
		for (ArchiveRecord record : value) {
			try {
				ArchiveRecordHeader header = record.getHeader();
				if (header.getMimetype().equals("application/http; msgtype=response")) {
					String targetUrl = header.getUrl();
					if (null == targetUrl) {
						continue;
					}
					String decodedUrl = URLDecoder.decode(targetUrl, "UTF-8");
					String host = new URL(decodedUrl).getHost();
					if (decodedUrl.toLowerCase().contains(URL_KEYWORD)
							&& host.replaceAll("[\\.a-zA-Z0-9]", "").length() < 2) {
						LOG.info("WARC-Target-URI: " + decodedUrl);
						byte[] rawData = IOUtils.toByteArray(record, record.available());
						String content = new String(rawData);
						String headerText = content.substring(0, content.indexOf("\r\n\r\n"));
						LOG.debug("Headers: " + headerText);
						if (headerText.contains("Content-Type: text/html")) {
							String body = content.substring(content.indexOf("\r\n\r\n") + 4);
							langTagMatcher = langTagPattern.matcher(body);
							if ((langTagMatcher.matches() && langTagMatcher.group(1).toLowerCase().contains("en"))
									|| (!langTagMatcher.matches())) {
								formTagMatcher = formTagPattern.matcher(body);
								int formTags = 0;
								while (formTagMatcher.find()) {
									formTags++;
								}
								outKey.set(decodedUrl);
								context.write(outKey, new LongWritable(formTags));
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Caught Exception", e);
			}
		}
	}
}
