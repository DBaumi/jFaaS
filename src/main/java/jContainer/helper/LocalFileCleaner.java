package jContainer.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes all created files in the process of execution of jContainer.
 */
public class LocalFileCleaner {

    private static List<File> directories;
    private ExecutionType executionType;
    private final static Logger logger = LoggerFactory.getLogger(LocalFileCleaner.class);

    public LocalFileCleaner(final ExecutionType executionType) {
        this.executionType = executionType;
        this.directories = new ArrayList<>();
    }

    /**
     * Cleans used directories of the execution and logs the deletion process.
     */
    public void cleanDirectories() {
        LocalFileCleaner.fillDirectories(this.executionType);
        for (final File directory : this.directories) {
            if (directory.exists()) {
                final Boolean success = LocalFileCleaner.deleteDirectoryWithSubFiles(directory);

                if (success) {
                    LocalFileCleaner.logger.info("Successfully deleted all files in directory '{}'", directory.getName());
                } else {
                    LocalFileCleaner.logger.error("Could not delete delete directory '{}'", directory.getName());
                }
            } else {
                LocalFileCleaner.logger.error("Error with deleting because directory '{}' does not exist!", directory.getName());
            }
        }
    }

    /**
     * Deletes all sub files in a directory and the directory itself.
     *
     * @param directory to be deleted
     * @return Boolean of the deletion success
     */
    private static Boolean deleteDirectoryWithSubFiles(final File directory) {
        for (final File fileInDirectory : directory.listFiles()) {
            if (fileInDirectory.isDirectory()) {
                LocalFileCleaner.deleteDirectoryWithSubFiles(fileInDirectory);
            } else {
                fileInDirectory.delete();
            }
        }
        return directory.delete();
    }

    /**
     * Statically fill directories for the different execution types.
     *
     * @param provider with ExecutionType
     */
    private static void fillDirectories(final ExecutionType provider) {
        if (provider == ExecutionType.LOCAL_DOCKER) {
            LocalFileCleaner.directories.add(new File(Constants.Paths.localFunctionDocker));
        } else {
            LocalFileCleaner.directories.add(new File(Constants.Paths.localTerraformDocker));
            LocalFileCleaner.directories.add(new File(Constants.Paths.scriptFolder));
        }
    }

    public ExecutionType getExecutionType() {
        return this.executionType;
    }

    public void setExecutionType(final ExecutionType executionType) {
        this.executionType = executionType;
    }
}
