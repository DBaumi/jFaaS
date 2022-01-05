package jContainer.helper;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * stores the name, input, output and java-version of a function
 */
public class FunctionDefinition {
    final String functionName;
    final Map<String, Object> functionInputs;
    private String javaVersion;
    final String jarFileName;
    private JsonObject functionOutputs;

    /**
     * For invocation with docker image.
     * @param functionName
     * @param functionInputs
     */
    public FunctionDefinition(final String functionName, final Map<String, Object> functionInputs) {
        this.functionName = functionName;
        this.functionInputs = functionInputs;
        this.jarFileName = functionName + ".jar";
    }

    /**
     * For invocation with JAR file.
     * @param functionName
     * @param functionInputs
     * @param javaVersion
     */
    public FunctionDefinition(final String functionName, final Map<String, Object> functionInputs, final String javaVersion) {
        this.functionName = functionName;
        this.functionInputs = functionInputs;
        this.jarFileName = functionName + ".jar";
        this.javaVersion = javaVersion;
    }

    public String getFunctionName() {
        return this.functionName;
    }

    public Map<String, Object> getFunctionInputs() {
        return this.functionInputs;
    }

    public String getJavaVersion() {
        return this.javaVersion;
    }

    public JsonObject getFunctionOutputs() {
        return this.functionOutputs;
    }

    public void setFunctionOutputs(final JsonObject functionOutputs) {
        this.functionOutputs = functionOutputs;
    }

    public String getJarFileName() {
        return this.jarFileName;
    }
}
