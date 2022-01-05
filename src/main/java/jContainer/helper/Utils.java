package jContainer.helper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Helper functions for the system to run properly.
 */
public class Utils {

    /**
     * Sets a sleep timer in milliseconds for the executing thread and catches any occurring exceptions.
     *
     * @param milliseconds
     * @throws InterruptedException
     */
    public static void sleep(final Integer milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert an incoming string to a JsonObject.
     *
     * @param stringToConvert output string from the LogEvent
     * @return json object representing the output of the container
     */
    public static JsonObject generateJsonOutput(String stringToConvert) {
        Gson parser = new Gson();
        JsonObject output = parser.fromJson(stringToConvert, JsonObject.class);

        return output;
    }

    /**
     * Check a string for line breaks and tabs to replace them with empty strings.
     *
     * @param stringToCheck
     * @return replacedString
     */
    public static String checkForLinebreak(final String stringToCheck) {
        String replacedString = stringToCheck;
        if (stringToCheck.contains("\n")) {
            replacedString = replacedString.replaceAll("(\\n|\\r|\\s)", "");
        }
        return replacedString;
    }

    /**
     * Check for special characters.
     *
     * @param stringToCheck
     * @return replacedString
     */
    public static String checkForSpecialCharacter(final String stringToCheck) {
        String replacedString = stringToCheck;
        if (!replacedString.isEmpty()) {
            replacedString = replacedString.replaceAll("[^A-Za-z]", "").replaceAll("\\s", "");
        }
        return replacedString;
    }

    /**
     * Extract function name from dockerhub link to image with tag.
     *
     * @param dockerhubImageLink
     * @return function name
     */
    public static String getFunctionNameFromDockerhubLink(final String dockerhubImageLink) {
        String function = dockerhubImageLink;
        if (!dockerhubImageLink.isEmpty()) {
            function = function.substring(function.lastIndexOf(":") + 1);
        }

        return function;
    }

    /**
     * Extract function name from AWS ECR link to image with tag.
     *
     * @param awsEcrImageLink
     * @return function name
     */
    public static String getFunctionNameFromAwsEcrLink(final String awsEcrImageLink) {
        String function = awsEcrImageLink;
        if (!awsEcrImageLink.isEmpty()) {
            function = function.substring(function.lastIndexOf(":") + 1);
        }

        return function;
    }

    /**
     * Extract  image name from dockerhub link.
     *
     * @param dockerhubImageLink link to the image on the public dockerhub repository
     * @return image name
     */
    public static String getImageNameWithoutTagFromDockerhubLink(final String dockerhubImageLink) {
        String imageName = dockerhubImageLink;
        if (!dockerhubImageLink.isEmpty()) {
            imageName = imageName.substring(0, imageName.lastIndexOf(":"));
        }

        return imageName;
    }

    /**
     * Extracts the dockerhub link from the public repository.
     *
     * @param functionInput
     * @return
     */
    public static String extractResourceLinkForFunction(final String functionInput) {
        String function = functionInput;
        if (!function.isEmpty()) {
            function = function.substring(function.indexOf("_") + 1, function.lastIndexOf("_"));
        }
        return function;
    }

    /**
     * @param link
     * @return
     */
    public static Boolean isDockerhubRepoLink(final String link) {
        return (link.contains(":") && link.contains("/"));
    }

    /**
     * @param link
     * @return
     */
    public static Boolean isAwsEcrRepoLink(final String link) {
        return (link.contains(".dkr.ecr.") && link.contains(".amazonaws.com/"));
    }

    /**
     * Checks a string it is empty or null.
     *
     * @param input as a String
     * @return True if string is either null or empty, False otherwise
     */
    public static Boolean isNullOrEmpty(final String input) {
        return input == null || input.trim().isEmpty();
    }

    /**
     * Extracts the java version from the function.
     *
     * @param function in form "function:8"
     * @return java version as string
     */
    public static String extractJDKVersionFromFunction(final String function) {
        String s = function;
        if (!s.isEmpty()) {
            s = (s.contains(":")) ? s.substring(function.indexOf(":") + 1) : "8";
        }
        return s;
    }

    /**
     * Extracts the function name from the function.
     *
     * @param function in form "function:8"
     * @return java version as string
     */
    public static String extractFunctionNameFromFunction(final String function) {
        String s = function;
        if (!s.isEmpty()) {
            s = s.substring(0, s.indexOf(":"));
        }
        return s;
    }
}