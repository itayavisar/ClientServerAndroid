package com.example.cogntivclientv2;

/* @breif Calculated Satistics for a given series of samples
* support currently mean and std calculation in O(1) in each call of "addSample(float sample)"
 */
public class Statistics {
    private float sample;
    private float mean;
    private float std;
    private long numSamples;

    public Statistics()
    {
        this.sample = 0.0f;
        this.mean = 0.0f;
        this.std = 0.0f;
        this.numSamples = 0;
    }

    public Statistics(Statistics stat)
    {
        this.sample = stat.sample;
        this.mean = stat.mean;
        this.std = stat.std;
        this.numSamples = stat.numSamples;
    }

    public float getSample() {return this.sample;}
    public float getMean() {return this.mean;}
    public float getStd() {return this.std;}

    static public float calcMean(float prevMean, float numSamples, float newSample)
    {
        float mean = prevMean;
        mean *= numSamples;
        mean += newSample;
        mean /= (numSamples + 1);

        return mean;
    }

    static public float calcStd(float prevStd, float prevMean, float curMean, float numSamples, float newSample)
    {
        float std = (float)Math.pow(prevStd, 2);
        std += (float)Math.pow(prevMean, 2);
        std *= numSamples;
        std += (float)Math.pow(newSample, 2);
        std /= (numSamples + 1);
        std -= (float)Math.pow(curMean, 2);
        std = (float)Math.sqrt((double)std);

        return std;
    }

    public void addSample(float newSample)
    {
        this.sample = newSample;
        float prevMean = mean;
        this.mean = calcMean(prevMean, numSamples, newSample);
        this.std = calcStd(this.std, prevMean, this.mean, numSamples, newSample);
        ++numSamples;
    }

    public void reset()
    {
        this.sample = 0.0f;
        this.mean = 0.0f;
        this.std = 0.0f;
        this.numSamples = 0;
    }
}
