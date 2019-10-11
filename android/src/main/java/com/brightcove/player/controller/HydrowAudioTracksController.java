package com.brightcove.player.controller;

public class HydrowAudioTracksController {

    private BrightcoveAudioTracksController controller;

    public HydrowAudioTracksController(BrightcoveAudioTracksController controller) {
        this.controller = controller;
    }

    public void selectAudioTrack(int index) {
        controller.selectAudioTrack(index);
    }
}
