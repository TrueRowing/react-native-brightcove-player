package jp.manse;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CircularProgressDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.brightcove.player.controller.HydrowAudioTracksController;
import com.brightcove.player.display.ExoPlayerVideoDisplayComponent;
import com.brightcove.player.edge.Catalog;
import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.mediacontroller.BrightcoveMediaController;
import com.brightcove.player.model.Video;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;
import com.facebook.react.bridge.WritableArray;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.BinaryFrame;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrightcovePlayerView extends RelativeLayout {

    private static String TAG = "BrightcovePlayerView";
    private static String BITRATE = "BITRATE";
    private final int INITIAL_BITRATE = 3500000;
    private HydrowAudioTracksController audioTracksController;
    private Map<String, Integer> audioTrackMap = new HashMap<>();
    private BrightcoveExoPlayerVideoView playerVideoView;
    private BrightcoveMediaController mediaController;
    private String policyKey;
    private String accountId;
    private String videoId;
    private String playbackUrl;
    private String referenceId;
    private Double currentTime = 0.0;
    private Catalog catalog;
    private boolean autoPlay = true;
    private boolean playing = false;
    private DefaultBandwidthMeter defaultBandwidthMeter;
    private ProgressBar progressBar;

    public BrightcovePlayerView(ThemedReactContext context) {
        this(context, null);
    }

    @SuppressLint("NewApi")
    public BrightcovePlayerView(ThemedReactContext context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(Color.BLACK);
        this.playerVideoView = new BrightcoveExoPlayerVideoView(context);
        this.setupProgress(context);
        defaultBandwidthMeter = new DefaultBandwidthMeter.Builder(context)
                .setInitialBitrateEstimate(getBitrate())
                .build();
        this.playerVideoView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.playerVideoView.finishInitialization();
        this.audioTracksController = new HydrowAudioTracksController(this.playerVideoView.getAudioTracksController());
        this.mediaController = new BrightcoveMediaController(this.playerVideoView);
        this.playerVideoView.setMediaController(this.mediaController);
        ViewCompat.setTranslationZ(this, 9999);

    }

    private void setupProgress(ThemedReactContext context){
        this.progressBar = new ProgressBar(context);
        this.progressBar.setIndeterminate(true);

        int RADIUS_DP = 16;
        int WIDTH_DP = 4;

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final float screenDensity = metrics.density;
        CircularProgressDrawable drawable = new CircularProgressDrawable(context);
        drawable.setColorSchemeColors(Color.WHITE);
        drawable.setCenterRadius(RADIUS_DP * screenDensity);
        drawable.setStrokeWidth(WIDTH_DP * screenDensity);
        this.progressBar.setIndeterminateDrawable(drawable);
        this.progressBar.setVisibility(VISIBLE);
    }

    private int getBitrate(){
        SharedPreferences pref = getContext().getSharedPreferences(TAG, 0);
        if (pref.contains(BITRATE)) {
            return pref.getInt(BITRATE,INITIAL_BITRATE);
        } else {
            return INITIAL_BITRATE;
        }
    }

    private void setBitrate(int bitrate){
        SharedPreferences pref = getContext().getSharedPreferences(TAG, 0);
        pref.edit().putInt(BITRATE, bitrate).apply();
    }

    @Override
    protected void onAttachedToWindow() {
        final BrightcovePlayerView that = this;
        super.onAttachedToWindow();
        this.addView(this.playerVideoView);
        this.addView(this.progressBar);

        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) progressBar.getLayoutParams();
        param.addRule(RelativeLayout.CENTER_HORIZONTAL);
        param.addRule(RelativeLayout.CENTER_VERTICAL);
        this.progressBar.setLayoutParams(param);

        this.requestLayout();

        EventEmitter eventEmitter = this.playerVideoView.getEventEmitter();
        final ExoPlayerVideoDisplayComponent exoPlayerVideoDisplayComponent =
                (ExoPlayerVideoDisplayComponent) this.playerVideoView.getVideoDisplay();
        exoPlayerVideoDisplayComponent.setBandwidthMeter(defaultBandwidthMeter);

        eventEmitter.on(EventType.DID_SET_SOURCE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                exoPlayerVideoDisplayComponent.setMetadataListener(new ExoPlayerVideoDisplayComponent.MetadataListener() {
                    @Override
                    public void onMetadata(Metadata metadata) {
                        for(int i = 0; i < metadata.length(); i++) {
                            Metadata.Entry entry = metadata.get(i);
                            if (entry instanceof Id3Frame) {
                                BinaryFrame binaryFrame = (BinaryFrame) entry;
                                sendEvent(binaryFrame);
                            }
                        }
                    }
                });
            }
        });

        eventEmitter.on(EventType.VIDEO_SIZE_KNOWN, new EventListener() {
            @Override
            public void processEvent(Event e) {
                fixVideoLayout();
            }
        });
        eventEmitter.on(EventType.READY_TO_PLAY, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_READY, event);
                that.sendStatus("ready");
            }
        });
        eventEmitter.on(EventType.PLAY, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("playRequested");
            }
        });
        eventEmitter.on(EventType.DID_PLAY, new EventListener() {
            @Override
            public void processEvent(Event e) {
                BrightcovePlayerView.this.playing = true;
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_PLAY, event);
                that.sendStatus("play");
            }
        });
        eventEmitter.on(EventType.PAUSE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("pauseRequested");
            }
        });
        eventEmitter.on(EventType.STOP, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("stopRequested");
            }
        });
        eventEmitter.on(EventType.DID_STOP, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("stop");
            }
        });
        eventEmitter.on(EventType.DID_PAUSE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                BrightcovePlayerView.this.playing = false;
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_PAUSE, event);
                that.sendStatus("pause");
            }
        });
        eventEmitter.on(EventType.COMPLETED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_END, event);
                that.sendStatus("end");
            }
        });
        eventEmitter.on(EventType.PROGRESS, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                int playhead = (int) e.properties.get(Event.PLAYHEAD_POSITION);
                currentTime = playhead / 1000d;
                event.putDouble("currentTime", currentTime);
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_PROGRESS, event);
            }
        });
        eventEmitter.on(EventType.ENTER_FULL_SCREEN, new EventListener() {
            @Override
            public void processEvent(Event e) {
                mediaController.show();
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_TOGGLE_ANDROID_FULLSCREEN, event);
            }
        });
        eventEmitter.on(EventType.EXIT_FULL_SCREEN, new EventListener() {
            @Override
            public void processEvent(Event e) {
                mediaController.show();
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_TOGGLE_ANDROID_FULLSCREEN, event);
            }
        });
        eventEmitter.on(EventType.VIDEO_DURATION_CHANGED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                Integer duration = (Integer)e.properties.get(Event.VIDEO_DURATION);
                WritableMap event = Arguments.createMap();
                event.putDouble("duration", duration / 1000d);
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_CHANGE_DURATION, event);
            }
        });
        eventEmitter.on(EventType.BUFFERING_STARTED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("bufferingStarted");
                BrightcovePlayerView.this.progressBar.setVisibility(VISIBLE);
            }
        });
        eventEmitter.on(EventType.BUFFERED_UPDATE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                Integer percentComplete = (Integer)e.properties.get(Event.PERCENT_COMPLETE);
                WritableMap event = Arguments.createMap();
                event.putDouble("bufferProgress", percentComplete / 100d);
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_UPDATE_BUFFER_PROGRESS, event);
            }
        });
        eventEmitter.on(ExoPlayerVideoDisplayComponent.RENDITION_CHANGED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                com.google.android.exoplayer2.Format format =
                        (com.google.android.exoplayer2.Format) e.properties.get(ExoPlayerVideoDisplayComponent.EXOPLAYER_FORMAT);
                WritableMap event = Arguments.createMap();
                event.putInt("bitrate", format.bitrate);
                event.putDouble("currentTime", currentTime);
                Log.d(TAG, "Bitrate : " + format.bitrate + " currentTime : " + currentTime);
                setBitrate(format.bitrate);
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_BITRATE_UPDATE, event);
            }
        });
        eventEmitter.on(EventType.BUFFERING_COMPLETED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("bufferingCompleted");
                BrightcovePlayerView.this.progressBar.setVisibility(GONE);
            }
        });
        eventEmitter.on(EventType.WILL_INTERRUPT_CONTENT, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("willInterruptContent");
            }
        });
        eventEmitter.on(EventType.DID_INTERRUPT_CONTENT, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("didInterruptContent");
            }
        });
        eventEmitter.on(EventType.WILL_RESUME_CONTENT, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("willResumeContent");
            }
        });
        eventEmitter.on(EventType.DID_RESUME_CONTENT, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("didResumeContent");
            }
        });
        eventEmitter.on(EventType.SOURCE_NOT_PLAYABLE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("sourceNotPlayable");
            }
        });
        eventEmitter.on(EventType.DID_SEEK_TO, new EventListener() {
            @Override
            public void processEvent(Event e) {
                that.sendStatus("didSeekTo");
            }
        });
        eventEmitter.on(EventType.ERROR, new EventListener() {
            @Override
            public void processEvent(Event e) {
                if ( BrightcovePlayerView.this.progressBar.getVisibility() == View.VISIBLE) {
                    BrightcovePlayerView.this.progressBar.setVisibility(GONE);
                }
                Error error = e.properties.containsKey(Event.ERROR)
                        ? (Error)e.properties.get(Event.ERROR)
                        : null;
                that.sendStatus("fail", error == null ? null : error.getLocalizedMessage());
            }
        });
        eventEmitter.on(EventType.AUDIO_TRACKS, new EventListener() {
            @Override
            public void processEvent(Event e) {
                if (e.properties.containsKey(Event.TRACKS)) {
                    List<String> audioTracks = (List)e.properties.get(Event.TRACKS);
                    for (int trackNumber = 0 ; trackNumber < audioTracks.size() ; trackNumber++) {
                        that.audioTrackMap.put(audioTracks.get(trackNumber), trackNumber);
                    }
                }
            }
        });
    }

    public void selectAudioTrack(String name) {
        if (this.audioTrackMap.containsKey(name)) {
            Integer index = this.audioTrackMap.get(name);
            if (index != null) {
                this.audioTracksController.selectAudioTrack(index);
            }
        }
    }

    public void getAudioTracks() {
        WritableArray audioTracks = Arguments.createArray();
        for(String audioTrack : this.audioTrackMap.keySet()) {
            audioTracks.pushString(audioTrack);
        }
        WritableMap event = Arguments.createMap();
        event.putArray("audioTracks", audioTracks);
        ReactContext context = (ReactContext)getContext();
        context.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                BrightcovePlayerManager.TOP_CHANGE,
                event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        playerVideoView.setMediaController((BrightcoveMediaController) null);
        playerVideoView.setOnTouchListener(null);
        playerVideoView.clear();
        EventEmitter eventEmitter = this.playerVideoView.getEventEmitter();
        eventEmitter.off();

        final ExoPlayerVideoDisplayComponent exoPlayerVideoDisplayComponent =
                (ExoPlayerVideoDisplayComponent) this.playerVideoView.getVideoDisplay();
        playerVideoView.removeListeners();
        if (exoPlayerVideoDisplayComponent.getExoPlayer() != null) {
            exoPlayerVideoDisplayComponent.getExoPlayer().release();
        }
        exoPlayerVideoDisplayComponent.removeListeners();
        exoPlayerVideoDisplayComponent.setMetadataListener((ExoPlayerVideoDisplayComponent.MetadataListener)null);

        removeView(playerVideoView);
    }

    private void sendEvent(BinaryFrame binaryFrame) {
        WritableMap event = Arguments.createMap();
        event.putString("key", binaryFrame.id);
        event.putString("value", new String(binaryFrame.data));
        event.putString("type", "metadata");
        ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_ID3_METADATA, event);
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
        this.setupCatalog();
        this.loadMovie();
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
        this.setupCatalog();
        this.loadMovie();
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
        this.referenceId = null;
        this.playbackUrl = null;
        this.setupCatalog();
        this.loadMovie();
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
        this.videoId = null;
        this.playbackUrl = null;
        this.setupCatalog();
        this.loadMovie();
    }

    public void setPlaybackUrl(String playbackUrl) {
        this.playbackUrl = playbackUrl;
        this.videoId = null;
        this.referenceId = null;
        this.setupCatalog();
        this.loadMovie();
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public void setPlay(boolean play) {
        if (this.playing == play) return;
        if (play) {
            this.playerVideoView.start();
        } else {
            this.playerVideoView.pause();
        }
    }

    public void setDefaultControlDisabled(boolean disabled) {
        this.mediaController.hide();
        this.mediaController.setShowHideTimeout(disabled ? 1 : 4000);
    }

    public void setFullscreen(boolean fullscreen) {
        this.mediaController.show();
        WritableMap event = Arguments.createMap();
        event.putBoolean("fullscreen", fullscreen);
        ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_TOGGLE_ANDROID_FULLSCREEN, event);
    }

    public void setVolume(float volume) {
        Map<String, Object> details = new HashMap<>();
        details.put(Event.VOLUME, volume);
        this.playerVideoView.getEventEmitter().emit(EventType.SET_VOLUME, details);
    }

    public void seekTo(int time) {
        this.playerVideoView.seekTo(time);
    }

    private void setupCatalog() {
        if (this.catalog != null || this.policyKey == null || this.accountId == null) return;
        this.catalog = new Catalog(this.playerVideoView.getEventEmitter(), this.accountId, this.policyKey);
    }

    private void loadMovie() {
        if (this.catalog == null) return;
        if (this.playbackUrl != null) {
            this.playerVideoView.clear();
            Video video = Video.createVideo(this.playbackUrl);
            this.playerVideoView.add(video);
            if (BrightcovePlayerView.this.autoPlay) {
                BrightcovePlayerView.this.playerVideoView.start();
            }
        } else {
            VideoListener listener = new VideoListener() {

                @Override
                public void onVideo(Video video) {
                    BrightcovePlayerView.this.playerVideoView.clear();
                    BrightcovePlayerView.this.playerVideoView.add(video);
                    if (BrightcovePlayerView.this.autoPlay) {
                        BrightcovePlayerView.this.playerVideoView.start();
                    }
                }
            };
            if (this.videoId != null) {
                this.catalog.findVideoByID(this.videoId, listener);
            } else if (this.referenceId != null) {
                this.catalog.findVideoByReferenceID(this.referenceId, listener);
            }
        }
    }

    private void fixVideoLayout() {
        int viewWidth = this.getMeasuredWidth();
        int viewHeight = this.getMeasuredHeight();
        SurfaceView surfaceView = (SurfaceView) this.playerVideoView.getRenderView();
        surfaceView.measure(viewWidth, viewHeight);
        int surfaceWidth = surfaceView.getMeasuredWidth();
        int surfaceHeight = surfaceView.getMeasuredHeight();
        int leftOffset = (viewWidth - surfaceWidth) / 2;
        int topOffset = (viewHeight - surfaceHeight) / 2;
        surfaceView.layout(leftOffset, topOffset, leftOffset + surfaceWidth, topOffset + surfaceHeight);
    }

    private void printKeys(Map<String, Object> map) {
        Log.d("debug", "-----------");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Log.d("debug", entry.getKey());
        }
    }

    private void sendStatus(String type) {
        this.sendStatus(type, null);
    }

    private void sendStatus(String type, String error) {
        WritableMap event = Arguments.createMap();
        event.putString("type", type);
        if (error != null) {
            event.putString("error", error);
        }
        ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_STATUS, event);
    }
}
