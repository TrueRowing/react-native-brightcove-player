import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ReactNative, {
    View,
    requireNativeComponent,
    NativeModules,
    ViewPropTypes,
    Platform,
    UIManager
} from 'react-native';

class BrightcovePlayer extends Component {

    state = {
        androidFullscreen: false
    };

    setNativeProps = nativeProps => {
        if (this._root) {
            this._root.setNativeProps(nativeProps);
        }
    };

    onChange = (event) => {
        if (event.nativeEvent.audioTracks && this.props.onAudioTracks) {
            this.props.onAudioTracks(new Set(event.nativeEvent.audioTracks));
        }
    }

    render() {
        return (
            <NativeBrightcovePlayer
                ref={e => this._root = e}
                {...this.props}
                style={[
                    this.props.style,
                    this.state.androidFullscreen && {
                        position: 'absolute',
                        zIndex: 9999,
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: '100%'
                    }
                ]}
                onReady={event =>
                    this.props.onReady && this.props.onReady(event.nativeEvent)
                }
                onPlay={event =>
                    this.props.onPlay && this.props.onPlay(event.nativeEvent)
                }
                onPause={event =>
                    this.props.onPause && this.props.onPause(event.nativeEvent)
                }
                onEnd={event => this.props.onEnd && this.props.onEnd(event.nativeEvent)}
                onProgress={event =>
                    this.props.onProgress && this.props.onProgress(event.nativeEvent)
                }
                onChangeDuration={event =>
                    this.props.onChangeDuration &&
                    this.props.onChangeDuration(event.nativeEvent)
                }
                onUpdateBufferProgress={event =>
                    this.props.onUpdateBufferProgress &&
                    this.props.onUpdateBufferProgress(event.nativeEvent)
                }
                onEnterFullscreen={event =>
                    this.props.onEnterFullscreen &&
                    this.props.onEnterFullscreen(event.nativeEvent)
                }
                onExitFullscreen={event =>
                    this.props.onExitFullscreen &&
                    this.props.onExitFullscreen(event.nativeEvent)
                }
                onToggleAndroidFullscreen={event => {
                    const fullscreen =
                        typeof event.nativeEvent.fullscreen === 'boolean'
                            ? event.nativeEvent.fullscreen
                            : !this.state.androidFullscreen;
                    if (fullscreen === this.state.androidFullscreen) return;
                    this.setState({ androidFullscreen: fullscreen });
                    if (fullscreen) {
                        this.props.onEnterFullscreen &&
                        this.props.onEnterFullscreen(event.nativeEvent);
                    } else {
                        this.props.onExitFullscreen &&
                        this.props.onExitFullscreen(event.nativeEvent);
                    }
                }}
                onBitrateUpdate={event =>
                    this.props.onBitrateUpdate &&
                    this.props.onBitrateUpdate(event.nativeEvent)
                }
                onStatusEvent={event =>
                    this.props.onStatusEvent &&
                    this.props.onStatusEvent(event.nativeEvent)
                }
                onCuePoint={event =>
                    this.props.onCuePoint &&
                    this.props.onCuePoint(event.nativeEvent)
                }
                onID3Metadata={event =>
                    this.props.onID3Metadata &&
                    this.props.onID3Metadata(event.nativeEvent)
                }
                onChange={this.onChange}
            />
        );
    }
}

BrightcovePlayer.prototype.seekTo = Platform.select({
    ios: function (seconds) {
        NativeModules.BrightcovePlayerManager.seekTo(
            ReactNative.findNodeHandle(this),
            seconds
        );
    },
    android: function (seconds) {
        UIManager.dispatchViewManagerCommand(
            ReactNative.findNodeHandle(this._root),
            UIManager.BrightcovePlayer.Commands.seekTo,
            [seconds]
        );
    }
});

BrightcovePlayer.prototype.getAudioTracks = function() {
    UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager.BrightcovePlayer.Commands.getAudioTracks,
        []
    );
}

BrightcovePlayer.prototype.selectAudioTrack = function(trackName) {
    UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager.BrightcovePlayer.Commands.selectAudioTrack,
        [trackName]
    );
};

BrightcovePlayer.propTypes = {
    ...(ViewPropTypes || View.propTypes),
    policyKey: PropTypes.string,
    accountId: PropTypes.string,
    referenceId: PropTypes.string,
    videoId: PropTypes.string,
    playbackUrl: PropTypes.string,
    autoPlay: PropTypes.bool,
    play: PropTypes.bool,
    fullscreen: PropTypes.bool,
    disableDefaultControl: PropTypes.bool,
    volume: PropTypes.number,
    onReady: PropTypes.func,
    onPlay: PropTypes.func,
    onPause: PropTypes.func,
    onEnd: PropTypes.func,
    onProgress: PropTypes.func,
    onChangeDuration: PropTypes.func,
    onUpdateBufferProgress: PropTypes.func,
    onBitrateUpdate: PropTypes.func,
    onEnterFullscreen: PropTypes.func,
    onExitFullscreen: PropTypes.func,
    resizeAspectFill: PropTypes.bool,
    onStatusEvent: PropTypes.func,
    onCuePoint: PropTypes.func,
    onID3Metadata: PropTypes.func,
    onAudioTracks: PropTypes.func,
};

BrightcovePlayer.defaultProps = {};

const NativeBrightcovePlayer = requireNativeComponent(
    'BrightcovePlayer',
    BrightcovePlayer
);

module.exports = BrightcovePlayer;
