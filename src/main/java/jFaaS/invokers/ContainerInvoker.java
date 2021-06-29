package jFaaS.invokers;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ContainerInvoker implements FaaSInvoker {
    protected static HashMap<String, String> images;
    protected final static String jarsFilePath = "resources/jarFiles/";

    public ContainerInvoker() {
        images = new HashMap<>();
        images.put("java", "openjdk:14-jdk-oraclelinux7");
        images.put("py", "python:3.8");
    }

    @Override
    public JsonObject invokeFunction(String function, Map<String, Object> functionInputs) throws IOException {
        String dockerfile;
        String[] functionName = function.split(":");
        if (function.contains("java:")) {
            dockerfile = createDockerfile(functionName[functionName.length - 1], "java");
        } else if (function.contains("py:")) {
            dockerfile = createDockerfile(functionName[functionName.length - 1], "py");
        } else {
            throw new IOException("Invalid language for function.");
        }

        // TODO: create class to run commands on local terminal, dependent on operating system
        // TODO: create docker container with commands on terminal, connect to container, give inputs, run function, return output

        return null;
    }

    //TODO: functionInputs have to be inserted to dockerfile??
    private String createDockerfile(String functionName, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("FROM " + images.get(language));
        // run is called remotely
        builder.append("COPY " + jarsFilePath + functionName + ".jar .");

        return builder.toString();
    }

}
