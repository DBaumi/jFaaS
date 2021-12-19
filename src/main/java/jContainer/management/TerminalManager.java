package jContainer.management;

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
                final String output = TerminalManager.executeOneCommand(command, workingDirectory, newProcess);
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
     * @param command          command that gets executed
     * @param workingDirectory directory where the command gets executed
     * @return output of the command
     */
    private static String executeOneCommand(final String command, final String workingDirectory, final boolean newProcess) {
        try {
            final ProcessBuilder processBuilder = newProcess ? new ProcessBuilder() : TerminalManager.processBuilder;
            processBuilder.directory(new File(workingDirectory));
            TerminalManager.printCommand(processBuilder.directory().getCanonicalPath(), command);
            processBuilder.command("bash", "-c", command);

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
        } catch (final InterruptedException e) {
            System.err.println("Process got interrupted.\nSee stack trace below for more details.");
            e.printStackTrace();
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

        if (outputBuilder != null) {
            System.out.println(outputBuilder);
        }

        if (!errorBuilder.toString().startsWith("null")) {
            System.err.println(errorBuilder);
        }

        return outputBuilder.toString();
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
