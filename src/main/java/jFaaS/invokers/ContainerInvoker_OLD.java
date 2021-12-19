package jFaaS.invokers;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ContainerInvoker_OLD implements FaaSInvoker {
    protected static HashMap<String, String> images;
    protected final static String jarsFilePath = "jarFiles/";

    public ContainerInvoker_OLD() {
        ContainerInvoker_OLD.images = new HashMap<>();
        ContainerInvoker_OLD.images.put("java", "openjdk:14-jdk-oraclelinux7");
        ContainerInvoker_OLD.images.put("py", "python:3.8");
    }

    @Override
    public JsonObject invokeFunction(final String function, final Map<String, Object> functionInputs) throws IOException {
        final String dockerfile;
        final String[] functionName = function.split(":");
        // TODO: if functionName contains a link to a image of a container then take existing container else:
        if (function.contains("java:")) {
            dockerfile = this.createDockerfile(functionName[functionName.length - 1], "java");
        } else if (function.contains("py:")) {
            dockerfile = this.createDockerfile(functionName[functionName.length - 1], "py");
        } else {
            throw new IOException("Invalid language for function.");
        }

        // TODO: create class to run commands on local terminal, dependent on operating system
        // TODO: create docker container with commands on terminal, connect to container, give inputs, run function, return output

        return null;
    }

    //TODO: functionInputs have to be inserted to dockerfile??
    private String createDockerfile(final String functionName, final String language) {
        final StringBuilder builder = new StringBuilder();
        builder.append("FROM " + ContainerInvoker_OLD.images.get(language));
        // run is called remotely
        builder.append("COPY " + ContainerInvoker_OLD.jarsFilePath + functionName + ".jar .");

        return builder.toString();
    }

}
