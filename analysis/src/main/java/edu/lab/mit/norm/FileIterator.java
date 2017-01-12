package edu.lab.mit.norm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Project: KEWILL FORWARD ENTERPRISE</p>
 * <p>File: edu.lab.mit.norm.FileIterator</p>
 * <p>Copyright: Copyright � 2015 Kewill Co., Ltd. All Rights Reserved.</p>
 * <p>Company: Kewill Co., Ltd</p>
 *
 * @author <chao.deng@kewill.com>
 * @version 1.0
 * @since 7/29/2015
 */
public class FileIterator implements Iterator<String> {

    private final static long BUFFER_SIZE = (2 << 12);
    private String currentLine;
    private BufferedReader reader;
    private BufferedWriter writer;
    private StringBuilder buffer;

    public FileIterator(String toReadFilePath, String toWriteFilePath)
        throws FileNotFoundException, UnsupportedEncodingException {
        build(toReadFilePath, toWriteFilePath);
    }

    public void build(String toReadFilePath, String toWriteFilePath)
        throws UnsupportedEncodingException, FileNotFoundException {
        buffer = new StringBuilder();
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(toReadFilePath), "UTF-8"));
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(toWriteFilePath, true), "UTF-8"));
    }

    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    public void appendContentToFile(String content, Boolean complete) {
        try {
            if (complete || doesWrite(content)) {
                buffer.append(content);
                writer.write(buffer.toString());
                writer.flush();
                buffer.delete(0, buffer.length());
            } else {
                buffer.append(content);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean doesWrite(String content) throws UnsupportedEncodingException {
        int bytes = String.valueOf(buffer.toString()).getBytes(StandardCharsets.UTF_8).length;
        return bytes < BUFFER_SIZE && bytes + content.getBytes(StandardCharsets.UTF_8).length >= BUFFER_SIZE;
    }

    @Override
    public boolean hasNext() {
        try {
            currentLine = reader.readLine();
        } catch (Exception ex) {
            currentLine = null;
            System.err.println(ex.getMessage());
        }

        return currentLine != null;
    }

    @Override
    public String next() {
        if (currentLine == null) {
            throw new NoSuchElementException("No content will be read");
        }
        return currentLine;
    }

    @Override
    public void remove() {
    }

    public void cleanBuffer() {
        buffer.delete(0, buffer.length());
    }
}
