package edu.lab.mit.cell;

import edu.lab.mit.norm.Criterion;
import edu.lab.mit.norm.ErrorMeta;
import edu.lab.mit.norm.FileIterator;
import edu.lab.mit.utils.StringSimilarity;
import edu.lab.mit.utils.Utilities;
import org.ehcache.Cache;
import org.ehcache.CachePersistenceException;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

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

    private FileIterator iterator;
    private final static Pattern pattern = Pattern.compile(
        "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29-)) (20|21|22|23|[0-1]?\\d):[0-5]?\\d:[0-5]?\\d",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static Handler instance;
    private PersistentCacheManager cacheManager;
    private Cache<String, String> identifiedErrorCache;

    public static Handler getInstance(String from, String to) throws Exception {
        if (instance == null) {
            instance = new Handler(from, to);
        }
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
            .with(CacheManagerBuilder.persistence(Utilities.finalStorePath("identifiedErrorCache")))
            .withCache(
                "identifiedError", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(10, EntryUnit.ENTRIES)
                        .offheap(1, MemoryUnit.MB)
                        .disk(10, MemoryUnit.MB, true))
            ).build(true);
        identifiedErrorCache = cacheManager.getCache("identifiedError", String.class, String.class);
        identifiedErrorCache.clear();
    }

    public Iterator<String> getIterator() {
        return iterator;
    }

    public BlockingQueue<ErrorMeta> analyzeUniqueError(Criterion instance, Cache<String, String> ignoredErrorCache) {
        Boolean errorOccurred = false;
        Boolean successiveError = false;
        Boolean newErrorFollowed = false;
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
        Pattern errorPattern = Pattern.compile(instance.getErrorStartID(), Pattern.CASE_INSENSITIVE);
        while (iterator.hasNext()) {
            String content = iterator.next();
            if (content != null && content.trim().length() != 0) {
                Matcher matcher = errorPattern.matcher(content);
                if (matcher.find() && (lstUserID == null || lstUserID.stream().anyMatch(content::contains))) {
                    errorOccurred = true;
                    if (!successiveError) {
                        successiveError = true;
                        newErrorFollowed = false;
                        error.append(content).append("\r\n");
                        tempError.append(content.substring(matcher.end()));
                        currDate = content.substring(0, matcher.end()).trim();
                    } else {
                        successiveError = false;
                        newErrorFollowed = true;
                    }
                } else {
                    if (errorOccurred) {
                        if (successiveError = !pattern.matcher(content).find()) {
                            error.append(content).append("\r\n");
                            tempError.append(content);
                        }
                    }
                }
                if (errorOccurred && !successiveError) {
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
                    errorOccurred = newErrorFollowed;
                    if (newErrorFollowed) {
                        newErrorFollowed = false;
                        successiveError = true;
                        error.append(content).append("\r\n");
                        tempError.append(content.substring(matcher.end()));
                        currDate = content.substring(0, matcher.end()).trim();
                    }
                }
            }
        }
        iterator.appendContentToFile("Found " + errorCounter + " errors!");
        iterator.close();
        return uniqueErrorLogQueue;
    }

    private boolean isMismatched(Cache<String, String> ignoredErrorCache, String currentErrorContent) {
        boolean noneIgnoredMatched = false;
        Iterator<Cache.Entry<String, String>> errorIterator = ignoredErrorCache.iterator();
        while (!noneIgnoredMatched && errorIterator.hasNext()) {
            Cache.Entry<String, String> entry = errorIterator.next();
            noneIgnoredMatched = entry.getValue().length() > currentErrorContent.length() * 0.9
                && entry.getValue().length() < currentErrorContent.length() * 1.1
                && StringSimilarity.similarity(entry.getValue(), currentErrorContent) > 0.9;
        }
        return noneIgnoredMatched;
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
        if (content != null && content.length() > 0) {
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
        if (operators != null && operators.trim().length() > 0) {
            lstOperator = new ArrayList<>();
            final List<String> finalLstOperator = lstOperator;
            Arrays.stream(operators.split("\\|")).forEach(item -> finalLstOperator.add("[" + item + "]"));
        }
        return lstOperator;
    }

    public void cleanUp() throws CachePersistenceException {
        cacheManager.close();
        cacheManager.destroy();
    }
}
