package edu.lab.mit.cell;

import edu.lab.mit.norm.Criterion;
import edu.lab.mit.norm.ErrorMeta;
import edu.lab.mit.norm.FileIterator;
import edu.lab.mit.utils.StringSimilarity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Project: KEWILL FORWARD ENTERPRISE</p>
 * <p>File: edu.lab.mit.cell.Handler</p>
 * <p>Copyright: Copyright � 2015 Kewill Co., Ltd. All Rights Reserved.</p>
 * <p>Company: Kewill Co., Ltd</p>
 *
 * @author <chao.deng@kewill.com>
 * @version 1.0
 * @since 7/29/2015
 */
public class Handler {

    private static FileIterator iterator;

    @SuppressWarnings(value = {"unused"})
    private final static Pattern pattern = Pattern.compile(
        "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29-)) (20|21|22|23|[0-1]?\\d):[0-5]?\\d:[0-5]?\\d",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static Handler instance;
    private CacheManager cacheManager;
    private Cache<String, String> identifiedErrorCache;

    public static Handler getInstance(String from, String to) throws Exception {
        if (instance == null) {
            instance = new Handler(from, to);
        }
        iterator.build(from, to);
        return instance;
    }

    @SuppressWarnings(value = {"UnusedDeclaration"})
    private Handler(String fromFilePath, String toFilePath) throws Exception {
        super();
        initCache();
        iterator = new FileIterator(fromFilePath, toFilePath);
    }

    private void initCache() {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache(
                "identifiedError", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
                    ResourcePoolsBuilder.heap(100)).build()
            ).build(true);
        identifiedErrorCache = cacheManager.getCache("identifiedError", String.class, String.class);
    }

    public Iterator<String> getIterator() {
        return iterator;
    }

    public BlockingQueue<ErrorMeta> analyzeUniqueError(Criterion instance, Cache<String, String> ignoredErrorCache) {
        Boolean errorOccurred = false;
        StringBuilder error = new StringBuilder();
        StringBuilder tempError = new StringBuilder();
        List<String> lstUserID = operators(instance.getUserID());
        BlockingQueue<ErrorMeta> uniqueErrorLogQueue = new LinkedBlockingQueue<>();
        String currDate = null;
        int errorCounter = 0;
        iterator.appendContentToFile(
            "\r\n##############################" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(GregorianCalendar.getInstance().getTime())
                + "##############################\r\n");
        Pattern startPattern = Pattern.compile(instance.getErrorStartID(), Pattern.CASE_INSENSITIVE);
        Pattern endPattern = Pattern.compile(instance.getErrorEndID());
        while (iterator.hasNext()) {
            String content = iterator.next();
            if (StringUtils.isNotEmpty(content)) {
                Matcher startMatcher = startPattern.matcher(errorOccurred ? "" : content);
                //If not detect the error start point, cancel the error indicator match.
                Matcher endMatcher = endPattern.matcher(errorOccurred ? content : "");
                if (startMatcher.find(0) && (lstUserID == null || lstUserID.stream().anyMatch(content::contains))) {
                    errorOccurred = true;
                    error.append(content).append("\r\n");
                    tempError.append(content.substring(startMatcher.end()));
                    currDate = content.substring(0, startMatcher.end()).trim();
                } else {
                    if (errorOccurred && !endMatcher.find(0)) {
                        error.append(content).append("\r\n");
                        tempError.append(content);
                    }
                }
                if (endMatcher.find(0)) {
                    errorOccurred = false;
                    String currentErrorContent = refineErrorContents(tempError, lstUserID);
                    String errorMD5 = genContentMD5(currentErrorContent);
                    if (ignoredErrorCache.get(errorMD5) == null) {
                        boolean ignoredMatched = isMismatched(ignoredErrorCache, currentErrorContent);
                        boolean identifiedMatched = isMismatched(identifiedErrorCache, currentErrorContent);
                        if (!ignoredMatched && !identifiedMatched) {
                            iterator
                                .appendContentToFile("[No." + errorCounter + "]" + error.toString() + "\r\n");
                            uniqueErrorLogQueue
                                .add(new ErrorMeta(errorCounter, currDate, errorMD5, error.toString()));
                            identifiedErrorCache.put(errorMD5, refineErrorContents(tempError, lstUserID));
                            errorCounter++;
                        }
                    }

                    error.delete(0, error.length());
                    tempError.delete(0, tempError.length());
                    if (startMatcher.find(0) && startPattern.matcher(content).find(0)) {
                        errorOccurred = true;
                        error.append(content).append("\r\n");
                        tempError.append(content.substring(startMatcher.end()));
                        currDate = content.substring(0, startMatcher.end()).trim();
                    }
                }
            }
        }
        iterator.appendContentToFile("Found " + errorCounter + " errors!");
        return uniqueErrorLogQueue;
    }

    private boolean isMismatched(Cache<String, String> errorCache, String currentErrorContent) {
        boolean matched = false;
        StandardDeviation sd = new StandardDeviation(false);
        Iterator<Cache.Entry<String, String>> errorIterator = errorCache.iterator();
        while (!matched && errorIterator.hasNext()) {
            Cache.Entry<String, String> entry = errorIterator.next();
            double[] lengths = {entry.getValue().length(), currentErrorContent.length()};
            matched = sd.evaluate(lengths) / entry.getValue().length() < 0.1
                && StringSimilarity.similarity(entry.getValue(), currentErrorContent) > 0.8;
        }
        return matched;
    }

    public String refineErrorContents(StringBuilder tempError, List<String> lstUserID) {
        final String[] temp = {tempError.toString()};
        if (lstUserID != null && lstUserID.size() > 0) {
            lstUserID.stream().filter(temp[0]::contains)
                .mapToInt(id -> temp[0].indexOf(id) + id.length()).min()
                .ifPresent(pos -> temp[0] = temp[0].substring(pos));
        }
        return temp[0];
    }

    private String genContentMD5(String content) {
        if (StringUtils.isNotEmpty(content)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(content.getBytes(StandardCharsets.UTF_8));
                return new BigInteger(1, digest.digest()).toString();
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            }
        }
        return "";
    }

    public List<String> operators(String operators) {
        List<String> lstOperator = null;
        if (StringUtils.isNotEmpty(operators)) {
            lstOperator = new ArrayList<>();
            final List<String> finalLstOperator = lstOperator;
            Arrays.stream(operators.split("\\|")).forEach(item -> finalLstOperator.add("[" + item + "]"));
        }
        return lstOperator;
    }

    public void cleanUp() {
        iterator.close();
        cacheManager.close();
    }
}
