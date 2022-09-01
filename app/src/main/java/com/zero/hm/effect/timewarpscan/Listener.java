package com.zero.hm.effect.timewarpscan;

public interface Listener {
    public void initDone(int stage);

    public void startAnimation();

    public void imageSavedSuccessfully(String filePath);
}
