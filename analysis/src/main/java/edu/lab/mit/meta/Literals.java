package edu.lab.mit.meta;

/**
 * <p>Title: MIT Lab Project</p>
 * <p>Description: edu.lab.mit.attributes.Literals</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * <p>Company: MIT Labs Co., Inc</p>
 *
 * @author <chao.deng@mit.lab>
 * @version 1.0
 * @since 1/11/2017
 */
public interface Literals {

    String DEFAULT_PATH = "defaultPath";
    String ERROR_START_ID = "error.start.id";
    String ERROR_END_ID = "error.end.id";
    String USER_ID = "user.id";
    String SOURCE_FILE_PATH = "source.file.path";
    String TARGET_FILE_PATH = "target.file.path";
    String SEPARATE_MULTIPLE_OPERATOR = "Separate multiple operator with |, if necessary.";
    String ERROR_START_WITH_REGEXP =
        "Support error start with regular express, such as ^\\d{4}(?:-\\d{2}){2}\\s+\\d{2}(?::\\d{2}){2}\\s+(?=\\bERROR\\b)";
    String ERROR_END_WITH_REGEXP =
        "Support error end with regular express, such as ^\\d{4}(?:-\\d{2}){2}\\s+\\d{2}(?::\\d{2}){2}";
}
