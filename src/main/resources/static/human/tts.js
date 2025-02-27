layui.define(function (exports) {
    const $tts = {
        ws: null,
        url: 'wss://aip.baidubce.com/ws/2.0/speech/publiccloudspeech/v1/tts',
        sound: [
            {title: '度小美', value: 0},
            {title: '度小宇', value: 1},
            {title: '度逍遥', value: 3},
            {title: '度丫丫', value: 4}
        ],
        per: 0,
        init: function (audio, callback) {
            const load = layui.layer.load(0);
            audio.onPause();
            audio.onPlay = function () {
                layui.human.speak();
                layui.layer.close(load);
            }
            audio.onDone = function () {
                audio.onPause();
                layui.human.standby();
                if ($tts.ws) {
                    $tts.ws.close();
                }
            }
            layui.$.get('api/credentials', function (data) {
                $tts.ws = new WebSocket(`${$tts.url}?access_token=${data}&per=${$tts.per}`);
                $tts.ws.onopen = function () {
                    $tts.start();
                };
                $tts.ws.onclose = function () {
                    layui.layer.close(load);
                    console.log('WebSocket关闭！');
                    $tts.ws = null;
                };
                $tts.ws.onerror = function () {
                    layui.layer.close(load);
                    layui.layer.msg('语音合成失败！（WebSocket）');
                    $tts.ws = null;
                };
                $tts.ws.onmessage = function (e) {
                    if (typeof e.data === 'string') {
                        console.log(e.data);
                        const data = JSON.parse(e.data);
                        if (data.type === 'system.started' && data.code === 0) {
                            typeof callback === 'function' && callback();
                        }
                        if (data.type === 'system.finished' && data.code === 0) {
                        }
                        if (data.code !== 0) {
                            $tts.ws.close();
                            layui.layer.close(load);
                            layui.layer.msg(`语音合成失败！（${data.code}:${data.message}）`);
                        }
                    }
                    if (e.data instanceof Blob) {
                        audio.onPush(e.data);
                    }
                };
            }).error(function (xhr, status, error) {
                layui.layer.close(load);
                layui.layer.msg('语音合成请求异常，请重试！（' + (error || status) + '）');
            });
        },
        //spd【语速，取值 0-15，默认为 5】
        //pid【音调，取值 0-15，默认为 5】
        //vol【音量，基础音库取值0-9，其他音库取值 0-15，默认为 5】
        //aue【音频格式，3=mp3，4=pcm-16k，5=pcm-8k，6=wav，默认为3】
        payload: {spd: 5, pid: 5, vol: 5, aue: 4},
        start: function () {
            if ($tts.ws) {
                $tts.ws.send(JSON.stringify({type: 'system.start', payload: $tts.payload}));
            }
        },
        finish: function () {
            if ($tts.ws) {
                $tts.ws.send(JSON.stringify({type: 'system.finish'}));
            }
        },
        send: function (text) {
            if ($tts.ws) {
                $tts.ws.send(JSON.stringify({type: "text", payload: {text: text}}));
            }
        }
    };
    exports('tts', $tts);
});