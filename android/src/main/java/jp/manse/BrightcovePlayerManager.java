package jp.manse;

import android.support.annotation.Nullable;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.HashMap;
import java.util.Map;


public class BrightcovePlayerManager extends SimpleViewManager<BrightcovePlayerView> {

    private static final String REACT_CLASS = "BrightcovePlayer";
    private static final int COMMAND_SEEK_TO = 1;
    public static final String EVENT_READY = "ready";
    public static final String EVENT_PLAY = "play";
    public static final String EVENT_PAUSE = "pause";
    public static final String EVENT_END = "end";
    public static final String EVENT_PROGRESS = "progress";
    public static final String EVENT_TOGGLE_ANDROID_FULLSCREEN = "toggle_android_fullscreen";
    public static final String EVENT_CHANGE_DURATION = "change_duration";
    public static final String EVENT_UPDATE_BUFFER_PROGRESS = "update_buffer_progress";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public BrightcovePlayerView createViewInstance(ThemedReactContext ctx) {
        return new BrightcovePlayerView(ctx);
    }

    @ReactProp(name = "policyKey")
    public void setPolicyKey(BrightcovePlayerView view, String policyKey) {
        view.setPolicyKey(policyKey);
    }

    @ReactProp(name = "accountId")
    public void setAccountId(BrightcovePlayerView view, String accountId) {
        view.setAccountId(accountId);
    }

    @ReactProp(name = "videoId")
    public void setVideoId(BrightcovePlayerView view, String videoId) {
        view.setVideoId(videoId);
    }

    @ReactProp(name = "referenceId")
    public void setReferenceId(BrightcovePlayerView view, String referenceId) {
        view.setReferenceId(referenceId);
    }

    @ReactProp(name = "autoPlay")
    public void setAutoPlay(BrightcovePlayerView view, boolean autoPlay) {
        view.setAutoPlay(autoPlay);
    }

    @ReactProp(name = "play")
    public void setPlay(BrightcovePlayerView view, boolean play) {
        view.setPlay(play);
    }

    @ReactProp(name = "disableDefaultControl")
    public void setDefaultControlDisabled(BrightcovePlayerView view, boolean disableDefaultControl) {
        view.setDefaultControlDisabled(disableDefaultControl);
    }

    @ReactProp(name = "fullscreen")
    public void setFullscreen(BrightcovePlayerView view, boolean fullscreen) {
        view.setFullscreen(fullscreen);
    }

    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "seekTo",
                COMMAND_SEEK_TO
        );
    }

    @Override
    public void receiveCommand(BrightcovePlayerView view, int commandType, @Nullable ReadableArray args) {
        Assertions.assertNotNull(view);
        Assertions.assertNotNull(args);
        switch (commandType) {
            case COMMAND_SEEK_TO: {
                assert args != null;
                view.seekTo((int)(args.getDouble(0) * 1000));
            }
        }
    }

    @Override
    public @Nullable Map <String,Object> getExportedCustomDirectEventTypeConstants() {
        Map<String, Object> map = new HashMap<>();
        map.put(EVENT_READY, MapBuilder.of("registrationName", "onReady"));
        map.put(EVENT_PLAY, MapBuilder.of("registrationName", "onPlay"));
        map.put(EVENT_PAUSE, MapBuilder.of("registrationName", "onPause"));
        map.put(EVENT_END, MapBuilder.of("registrationName", "onEnd"));
        map.put(EVENT_PROGRESS, MapBuilder.of("registrationName", "onProgress"));
        map.put(EVENT_CHANGE_DURATION, MapBuilder.of("registrationName", "onChangeDuration"));
        map.put(EVENT_UPDATE_BUFFER_PROGRESS, MapBuilder.of("registrationName", "onUpdateBufferProgress"));
        map.put(EVENT_TOGGLE_ANDROID_FULLSCREEN, MapBuilder.of("registrationName", "onToggleAndroidFullscreen"));
        return map;
    }
}
