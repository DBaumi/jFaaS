package jContainer.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple stop watch to log difference of start and end time.
 */
public class Stopwatch {
    private long start;
    private long end;
    private Boolean withCheckpoints;
    private List<Long> checkpointTimes;
    private List<String> checkpointMessages;

    /**
     * set functionality for checkpoints
     * @param withCheckpoints
     */
    public Stopwatch(Boolean withCheckpoints){
        this.start = System.nanoTime();
        this.withCheckpoints = withCheckpoints;

        if(withCheckpoints){
            this.checkpointTimes = new ArrayList<>();
            this.checkpointMessages = new ArrayList<>();
        }
    }

    /**
     * Sets end to now or already set end and calculates the difference between start and end in milliseconds.
     * @return elapsed time
     */
    public double getElapsedTime(){
        this.end = System.nanoTime();

        return inMilliSeconds(this.end - this.start);
    }

    public void stopTime(){
        this.end = System.nanoTime();
    }

    public void addCheckpoint(String msg){
        if(this.withCheckpoints){
            this.checkpointTimes.add(System.nanoTime());
            this.checkpointMessages.add(msg);
        } else {
            System.out.println("Checkpoints are not allowed, please use different constructor!");
        }
    }

    /**
     * Show timings between checkpoint and start of the stopwatch in milliseconds.
     * @return list of checkpoint times
     */
    public String showCheckpointTimes(){
        if(this.withCheckpoints){
            StringBuilder checkpoints = new StringBuilder("\n");
            for(int i = 1; i < this.checkpointTimes.size()+1; i++){
                checkpoints.append(i + ". Checkpoint: '" + this.checkpointMessages.get(i-1) + "' " + inMilliSeconds(this.checkpointTimes.get(i-1)-this.start) + "ms difference to start\n");
            }

            return checkpoints.toString();
        } else {
            return "Checkpoints are not allowed, please use different constructor!";
        }
    }

    private double inMilliSeconds(long timeInNanoseconds){
        return (double) timeInNanoseconds / 1000000;
    }
}
