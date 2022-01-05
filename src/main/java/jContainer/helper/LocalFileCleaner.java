package jContainer.helper;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes all created files in the process of execution of jContainer.
 */
/**
 * Deletes all created files in the process of execution of jContainer.
 */
public class LocalFileCleaner {

    private static List<File> directories;
    private final static Logger logger = LoggerFactory.getLogger(LocalFileCleaner.class);

    public LocalFileCleaner(){
        this.directories = new ArrayList<>();
    }

    /**
     * Cleans used directories of the execution and logs the deletion process.
     */
    public void cleanDirectories() {
        directories.add(new File(Constants.Paths.jContainerResourceFolder));
        for(File directory : this.directories){
            if(directory.exists()){
                Boolean success = deleteDirectoryWithSubFiles(directory);

                if (success) {
                    logger.info("Successfully deleted all files in directory '{}'!", directory.getName());
                } else {
                    logger.error("Could not delete delete directory '{}', please delete it after the execution!", directory.getName());
                }
            } else {
                logger.error("Error with deleting because directory '{}' does not exist!", directory.getName());
            }
        }
    }

    /**
     * Deletes all sub files in a directory and the directory itself.
     * @param directory to be deleted
     * @return Boolean of the deletion success
     */
    private static Boolean deleteDirectoryWithSubFiles(File directory){
        Utils.sleep(Constants.utils.sleepTimer);
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            logger.error("Cleaning directory '{}' was not successful!", directory.getName());
        }
        return directory.delete();
    }
}
