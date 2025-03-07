layui.define(function (exports) {
    const $tts = {
        ws: null, id: null, key: null,
        open: function (callback) {
            const loading = layui.layer.load(0);
            layui.$.get('', function (data) {
                $tts.id = 'TASK-' + Date.now();
                $tts.key = data.key;
                $tts.ws = new WebSocket(`wss://nls-gateway-cn-beijing.aliyuncs.com/ws/v1?token=${data.token}`);
                $tts.ws.binaryType = "arraybuffer";
                $tts.ws.onopen = function () {
                    if ($tts.ws.readyState === WebSocket.OPEN) {
                        $tts.start();
                    }
                };
                $tts.ws.onclose = function () {
                    layui.layer.close(loading);
                    console.log('WebSocket关闭！');
                    $tts.ws = null;
                };
                $tts.ws.onerror = function () {
                    layui.layer.close(loading);
                    layui.layer.msg('语音合成失败！（WebSocket）');
                    $tts.ws = null;
                };
                $tts.ws.onmessage = function (e) {
                    console.log(e.data);
                    if (e.data instanceof ArrayBuffer) {
                        typeof callback === 'function' && callback(e.data);
                    } else {
                        const data = JSON.parse(e.data);
                        if (data.header.name === 'SynthesisStarted' && data.header.status === 20000000) {
                            typeof callback === 'function' && callback(null);
                        }
                        if (data.header.name === 'SynthesisCompleted' && data.header.status === 20000000) {
                            $tts.ws.close();
                        }
                        if (data.header.status !== 20000000) {
                            $tts.ws.close();
                            layui.layer.msg(`语音合成失败！（${data.header.status}:${data.header.status_message}）`);
                        }
                    }
                };
            }).error(function (xhr, status, error) {
                layui.layer.close(loading);
                layui.layer.msg('语音合成请求异常，请重试！（' + (error || status) + '）');
            });
        },
        header: function (name) {
            return {
                message_id: 'MESSAGE-' + Date.now(),
                task_id: $tts.id,
                namespace: 'FlowingSpeechSynthesizer',
                name: name,
                appkey: $tts.key
            };
        },
        //voice【发音人，默认是xiaoyun。】
        //format【音频编码格式，支持pcm、wav和mp3格式，默认值：pcm。】
        //sample_rate【音频采样率，默认值：16000Hz。】
        //volume【音量，取值范围：0～100。默认值：50。】
        //speech_rate【语速，取值范围：-500～500，默认值：0。】
        //pitch_rate【语调，取值范围：-500～500，默认值：0。】
        payload: {voice: 'xiaoyun', format: 'PCM', sample_rate: 16000, volume: 100, speech_rate: 0, pitch_rate: 0},
        start: function () {
            if ($tts.ws) {
                $tts.ws.send(JSON.stringify({header: $tts.header('StartSynthesis'), payload: $tts.payload}));
            }
        },
        stop: function () {
            if ($tts.ws) {
                $tts.ws.send(JSON.stringify({header: $tts.header('StopSynthesis')}));
            }
        },
        send: function (text) {
            if ($tts.ws) {
                $tts.ws.send(JSON.stringify({header: $tts.header('RunSynthesis'), payload: {text: text}}));
            }
        }
    };
    exports('tts', $tts);
});