package com.mkyuan.fountainbase.knowledge.bean;

public class ImportStatusBean {

    private long totalCount=0;
    private long hasProcessCount=0;
    private double processPercentage=0.0;
    private boolean running=false;
    private long recordCounts=0;
    private String taskDescription="";
    private String repoId="";
    private boolean stopFlag=false;
    private String currentStep="";//display current handle step

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public boolean isStopFlag() {
        return stopFlag;
    }

    public void setStopFlag(boolean stopFlag) {
        this.stopFlag = stopFlag;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getHasProcessCount() {
        return hasProcessCount;
    }

    public void setHasProcessCount(long hasProcessCount) {
        this.hasProcessCount = hasProcessCount;
    }

    public double getProcessPercentage() {
        return processPercentage;
    }

    public void setProcessPercentage(double processPercentage) {
        this.processPercentage = processPercentage;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getRecordCounts() {
        return recordCounts;
    }

    public void setRecordCounts(long recordCounts) {
        this.recordCounts = recordCounts;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }
}
