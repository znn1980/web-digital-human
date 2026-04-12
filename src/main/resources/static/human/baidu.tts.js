layui.define(['assert'], function (exports) {
    exports('tts', {
        ws: null,
        sound: [
            {title: '度小美', value: 0},
            {title: '度小宇', value: 1},
            {title: '度逍遥', value: 3},
            {title: '度丫丫', value: 4}
        ],
        voice: 0,
        open: function (callback) {
            layui.assert.limit('tts');
            const loading = layui.layer.load(2);
            layui.$.get('baidu/credentials', (data) => {
                this.ws = new WebSocket(`wss://aip.baidubce.com/ws/2.0/speech/publiccloudspeech/v1/tts?access_token=${data}&per=${this.voice}`);
                this.ws.onopen = () => {
                    layui.layer.close(loading);
                    this.start();
                };
                this.ws.onclose = () => {
                    layui.layer.close(loading);
                    console.log('WebSocket关闭！');
                    this.ws = null;
                };
                this.ws.onerror = () => {
                    layui.layer.close(loading);
                    layui.layer.msg('语音合成失败！（WebSocket）');
                    this.ws = null;
                };
                this.ws.onmessage = (e) => {
                    console.log(e.data);
                    if (e.data instanceof Blob) {
                        const fileReader = new FileReader();
                        fileReader.onload = (e) => {
                            typeof callback === 'function' && callback(e.target.result);
                        };
                        fileReader.onerror = () => {
                            console.log('读取语音失败:', fileReader.error);
                        };
                        fileReader.readAsArrayBuffer(e.data);
                    } else {
                        const data = JSON.parse(e.data);
                        if (data.type === 'system.started' && data.code === 0) {
                            typeof callback === 'function' && callback(null);
                        }
                        if (data.type === 'system.finished' && data.code === 0) {
                            this.ws.close();
                        }
                        if (data.code !== 0) {
                            this.ws.close();
                            console.log('语音合成失败:', data);
                            //layui.layer.msg(`语音合成失败！（${data.code}:${data.message}）`);
                        }
                    }
                };
            }).fail((xhr, status, error) => {
                layui.layer.close(loading);
                layui.layer.msg(`语音合成请求异常，请重试！（${error || status}）`);
            });
        },
        close: function () {
            if (this.ws) {
                this.ws.close();
            }
        },
        //spd【语速，取值 0-15，默认为 5】
        //pid【音调，取值 0-15，默认为 5】
        //vol【音量，基础音库取值0-9，其他音库取值 0-15，默认为 5】
        //aue【音频格式，3=mp3，4=pcm-16k，5=pcm-8k，6=wav，默认为3】
        payload: {spd: 5, pid: 5, vol: 5, aue: 4},
        start: function () {
            if (this.ws) {
                this.ws.send(JSON.stringify({type: 'system.start', payload: this.payload}));
            }
        },
        stop: function () {
            if (this.ws) {
                this.ws.send(JSON.stringify({type: 'system.finish'}));
            }
        },
        send: function (text) {
            if (this.ws) {
                this.ws.send(JSON.stringify({type: "text", payload: {text: text}}));
            }
        }
    });
});