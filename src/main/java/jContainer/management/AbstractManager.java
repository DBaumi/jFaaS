package jContainer.management;

import jContainer.helper.Constants;
import jContainer.helper.FunctionDefinition;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class AbstractManager {

    private final FunctionDefinition functionDefinition;
    private String workingDirectory;

    public AbstractManager(final FunctionDefinition functionDefinition) {
        this.functionDefinition = functionDefinition;
        this.workingDirectory = Constants.Paths.scriptFolder + functionDefinition.getFunctionName() + "/";
    }

    /**
     * creates a file in a given path with a given content
     *
     * @param path    given path for the file
     * @param content given content to insert into the file
     * @return true if file created, otherwise false
     */
    protected Boolean createFile(final String path, final String content) {
        boolean result = false;

        try {

            final File file = new File(path);
            final File parentDir = new File(file.getParent());
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            result = file.createNewFile();

            final FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.flush();
            fileWriter.close();

        } catch (final IOException e) {
            System.err.println("Error in creating file.\n" + e.getMessage());
        }
        return result;

    }

    // TODO: implement, if needed
    public String readFile(final String path) {
        return null;
    }

    public FunctionDefinition getFunctionDefinition() {
        return this.functionDefinition;
    }

    public String getWorkingDirectory() {
        return this.workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * @return name of image with hub_user name, repo name and tag name
     */
    protected String getCreatedImageNameDockerHub() {
        return Constants.Docker.hub_user_and_repo_name + this.getFunctionDefinition().getFunctionName();
    }

    protected void runCommandInWorkingDirectory(final String... commands) {
        TerminalManager.executeCommand(this.getWorkingDirectory(), false, commands);
    }


}
