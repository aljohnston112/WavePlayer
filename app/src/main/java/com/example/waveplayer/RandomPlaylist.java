package com.example.waveplayer;

public abstract class RandomPlaylist {

    // The ProbFun that randomly picks the media to play
    ProbFunTree<AudioURI> probabilityFunction;

    String name;

    public void setMaxPercent(double maxPercent) {
        probabilityFunction.setMaxPercent(maxPercent);
    }

    /**
     * @return the ProbFunTree that controls the probability of which media gets played.
     */
    public ProbFunTree<AudioURI> getProbFun() {
        return probabilityFunction;
    }

    public String getName(){
        return name;
    }

}
