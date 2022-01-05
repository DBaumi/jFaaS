package jContainer.management;

import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.*;
import com.google.gson.JsonObject;
import jContainer.helper.Constants;
import jContainer.helper.CredentialsProperties;
import jContainer.helper.FunctionDefinition;
import jContainer.helper.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CloudWatchLogsManager {
    private final String logGroupName;
    private static final String LOG_GROUP_PREFIX = Constants.CloudWatch.log_group_prefix + "/";
    private List<FilteredLogEvent> events;
    private final List<LogStream> streams;
    private FilteredLogEvent resultEvent;
    private Long executionTime;

    private static final AWSLogsClient client;
    final static Logger logger = LoggerFactory.getLogger(CloudWatchLogsManager.class);

    static {
        client = CredentialsProperties.awsLogsClient;
    }

    public CloudWatchLogsManager(final FunctionDefinition functionDefinition) {
        this.logGroupName = Constants.CloudWatch.log_group_name + "_" + functionDefinition.getFunctionName().toLowerCase() + "_" + CredentialsProperties.localUser.toLowerCase();
        this.streams = new ArrayList<>();
    }

    private List<LogGroup> getLogGroups(final String logGroupName) {
        final DescribeLogGroupsRequest request = new DescribeLogGroupsRequest();
        Utils.sleep(Constants.utils.sleepTimer);
        return CloudWatchLogsManager.client.describeLogGroups(request).getLogGroups();
    }

    /**
     * Retrieve the result of the function invocation inside the container from generated CloudWatchLogs resource
     * over the existing function name and convert it to json.
     *
     * @param functionName name of the invoked function
     * @return json object representing the result of function invocation inside the container
     */
    public JsonObject resultFromLogStreamPrefix(final String functionName) {
        if(!functionName.isEmpty()) {
            this.getLogEvents(CloudWatchLogsManager.LOG_GROUP_PREFIX + functionName);
        } else {
            CloudWatchLogsManager.logger.error("No LogEvents found to the given function name!");
        }

        final JsonObject result = Utils.generateJsonOutput(this.resultEvent.getMessage());

        return result;
    }

    /**
     * Retrieves log events from the log stream prefix.
     * @param logStreamNamePrefix
     */
    private void getLogEvents(final String logStreamNamePrefix) {
        final FilterLogEventsRequest filterLogEventsRequest = new FilterLogEventsRequest()
                .withLogGroupName(this.getLogGroupName())
                .withLogStreamNamePrefix(logStreamNamePrefix);

        final FilterLogEventsResult filterLogEventsResult = CloudWatchLogsManager.client.filterLogEvents(filterLogEventsRequest);

        this.events = filterLogEventsResult.getEvents();

        if (this.getEvents().size() == 0) {
            Utils.sleep(Constants.utils.sleepTimer);
            this.getLogEvents(logStreamNamePrefix);
        } else {
            this.resultEvent = this.getEvents().get(0);
        }
    }

    /**
     * Log execution time of the container execution.
     */
    public Long executionTimeFromLogEvent(){
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT+2"));

        final Long execTime = this.resultEvent.getIngestionTime() - this.resultEvent.getTimestamp(); // in milliseconds
        return execTime;
    }

    private String getLogGroupName() {
        return this.logGroupName;
    }

    private List<FilteredLogEvent> getEvents() {
        return this.events;
    }

    public void setEvents(final List<FilteredLogEvent> events) {
        this.events = events;
    }

    private List<LogStream> getStreams() {
        return this.streams;
    }

}
