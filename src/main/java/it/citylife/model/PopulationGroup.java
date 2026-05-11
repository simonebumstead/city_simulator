package it.citylife.model;

public class PopulationGroup {

    private double jobSatisfaction    = 50.0;
    private double healthSatisfaction = 50.0;
    private double safetySatisfaction = 50.0;

    public double getJobSatisfaction()    { return jobSatisfaction; }
    public double getHealthSatisfaction() { return healthSatisfaction; }
    public double getSafetySatisfaction() { return safetySatisfaction; }

    public void setJobSatisfaction(double v)    { jobSatisfaction    = Math.max(0, Math.min(100, v)); }
    public void setHealthSatisfaction(double v) { healthSatisfaction = Math.max(0, Math.min(100, v)); }
    public void setSafetySatisfaction(double v) { safetySatisfaction = Math.max(0, Math.min(100, v)); }
}
