layui.define(['assert'], function (exports) {
    exports('asr', {
        ws: null,
        app_id: '',
        app_key: '',
        open: function (callback) {
            layui.assert.limit('asr');
            const loading = layui.layer.load(2);
            this.ws = new WebSocket(`wss://vop.baidu.com/realtime_asr?sn=${Date.now()}`);
            this.ws.onopen = () => {
                layui.layer.close(loading);
                this.start();
                typeof callback === 'function' && callback(null, null);
            };
            this.ws.onclose = () => {
                layui.layer.close(loading);
                console.log('WebSocket关闭！！');
                this.ws = null;
            };
            this.ws.onerror = () => {
                layui.layer.close(loading);
                layui.layer.msg('语音识别失败！！（WebSocket）');
                this.ws = null;
            };
            this.ws.onmessage = (e) => {
                console.log(e.data);
                const data = JSON.parse(e.data);
                if (data.type === 'MID_TEXT' && data.err_no === 0) {
                    typeof callback === 'function' && callback(false, data.result);
                }
                if (data.type === 'FIN_TEXT' && data.err_no === 0) {
                    typeof callback === 'function' && callback(true, data.result);
                }
                if (data.err_no !== 0) {
                    this.ws.close()
                    console.log('语音识别失败:', data);
                    //layui.layer.msg(`语音识别失败！（${data.err_no}:${data.err_msg}）`);
                }
            };
        },
        close: function () {
            if (this.ws) {
                this.ws.close();
            }
        },
        start: function () {
            if (this.ws) {
                this.ws.send(JSON.stringify({
                    type: 'START',
                    data: {
                        appid: this.app_id,
                        appkey: this.app_key,
                        dev_pid: 15372,
                        cuid: 'SC1234567890',
                        format: 'pcm',
                        sample: 16000
                    }
                }));
            }
        },
        stop: function () {
            if (this.ws) {
                this.ws.send(JSON.stringify({type: 'CANCEL'}));
            }
        },
        send: function (buffer) {
            if (this.ws) {
                this.ws.send(buffer);
            }
        },
        asr: function (blob, callback) {
            layui.assert.limit('asr');
            const loading = layui.layer.load(2);
            const fileReader = new FileReader();
            fileReader.onload = (e) => {
                console.log(e.target.result);
                layui.$.post('baidu/speech/recognitions', {
                    vop: encodeURIComponent(e.target.result.split(',')[1])
                }, (data) => {
                    console.log(data);
                    if (data.err_no !== 0) {
                        layui.layer.msg(`语音识别失败！（${data.err_no}:${data.err_msg}）`);
                    } else {
                        typeof callback === 'function' && callback(data.result[0]);
                    }
                    layui.layer.close(loading);
                }).fail((xhr, status, error) => {
                    layui.layer.close(loading);
                    layui.layer.msg(`语音识别请求异常，请重试！（${error || status}）`);
                });
            };
            fileReader.onerror = () => {
                layui.layer.close(loading);
                layui.layer.msg(`读取录音文件失败！（${fileReader.error}）`);
            };
            fileReader.readAsDataURL(blob);
        }
    });
});