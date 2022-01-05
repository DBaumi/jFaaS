package jContainer.management;

import jContainer.helper.CredentialsProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Caroline Haller, Christoph Abenthung
 */
public class TerminalManager {

    private static final ProcessBuilder processBuilder = new ProcessBuilder();
    private static final String sensitiveDataRegex = TerminalManager.getSensitiveDataRegex();

    /**
     * create Regex String of all sensitive data that can occur in terminal output
     *
     * @return Regex String
     */
    private static String getSensitiveDataRegex() {
        final List<String> sensitiveData = new ArrayList<>();

        sensitiveData.add(".*aws_access_key_id.*");
        sensitiveData.add(".*" + CredentialsProperties.awsAccessKey + ".*");
        sensitiveData.add(".*aws_secret_access_key.*");
        sensitiveData.add(".*" + CredentialsProperties.awsSecretKey + ".*");

        return String.join("|", sensitiveData);

    }

    /**
     * executes one or more commands
     *
     * @param workingDirectory directory where the command gets executed
     * @param commands         commands that get executed
     * @return List of command outputs
     */
    public static List<String> executeCommand(final String workingDirectory, final boolean newProcess, final String... commands) {
        if (commands.length != 0) {
            final List<String> outputs = new ArrayList<>();
            for (final String command : commands) {

                final boolean containsSensitiveData = command.matches(TerminalManager.sensitiveDataRegex);

                final String output = TerminalManager.executeOneCommand(command, workingDirectory, newProcess, containsSensitiveData);
                if (output != null && !output.isEmpty()) {
                    outputs.add(output);
                }
            }
            return outputs;
        } else {
            System.err.println("No commands found!");
        }

        return Arrays.asList("Error");
    }

    /**
     * executes one command at the previously set working directory
     * before every command bash -c needs to be set else the command might get executed with the wrong terminal
     *
     * @param command               command that gets executed
     * @param workingDirectory      directory where the command gets executed
     * @param newProcess            if new ProcessBuilder is started
     * @param containsSensitiveData is command is printed or replaced
     * @return output of the command
     */
    private static String executeOneCommand(final String command, final String workingDirectory, final boolean newProcess, final boolean containsSensitiveData) {
        try {
            final ProcessBuilder processBuilder = newProcess ? new ProcessBuilder() : TerminalManager.processBuilder;
            final String commandToPrint = containsSensitiveData
                    ? command
                    .replace(CredentialsProperties.awsAccessKey, "*****")
                    .replace(CredentialsProperties.awsSecretKey, "*****")
                    : command;
            if (workingDirectory != null && !workingDirectory.equals("")) {
                processBuilder.directory(new File(workingDirectory));
                TerminalManager.printCommand(processBuilder.directory().getCanonicalPath(), commandToPrint);
            } else {
                TerminalManager.printCommand("/", commandToPrint);
            }
            processBuilder.command("bash", "-c", command);
//            processBuilder.command("cmd", "/c", command);

            final Process process = processBuilder.start();

            String output = TerminalManager.handleOutput(process);

            final int exitVal = process.waitFor();
            if (exitVal != 0) {
                System.err.println("Error with the Process!");
                System.err.println("Process terminated with ExitCode " + exitVal);
                output += "Error: Something happened here :D";
            }

            return output;
        } catch (final IOException e) {
            System.err.println("Error starting the process!");
            e.printStackTrace();
            System.exit(0);
        } catch (final InterruptedException e) {
            System.err.println("Process got interrupted.\nSee stack trace below for more details.");
            e.printStackTrace();
            System.exit(0);
        }
        return null;
    }

    /**
     * Handles output of a certain process
     *
     * @param process process to handle output of
     * @return output of the process
     */
    private static String handleOutput(final Process process) {
        final StringBuilder outputBuilder = new StringBuilder();
        final StringBuilder errorBuilder = new StringBuilder();

        String outputLine = "";
        String errorLine = "";

        final BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

        try {
            while (((outputLine = outputReader.readLine()) != null) || ((errorLine = errorReader.readLine()) != null)) {
                if (outputLine != null) {
                    outputBuilder.append(outputLine + "\n");
                }
                errorBuilder.append(errorLine + "\n");
            }
        } catch (final IOException e) {
            System.err.println("Error reading line!");
        }

        if (outputBuilder != null && !outputBuilder.toString().equals("")) {
            System.out.println(outputBuilder);
        }

        if (!errorBuilder.toString().startsWith("null")) {
            System.err.println(errorBuilder);
        }

        return outputBuilder.toString().equals("") ? errorBuilder.toString() : outputBuilder.toString();
    }

    /**
     * better commandline output -> looks more like an actual shell command
     *
     * @param workingDirectory working directory where  the command gets executed
     * @param command          command that gets executed
     */
    private static void printCommand(final String workingDirectory, final String command) {
        System.out.println(workingDirectory + "$ " + command);
    }
}
