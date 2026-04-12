layui.define(['assert'], function (exports) {
    exports('asr', {
        ws: null,
        task_id: null,
        app_key: '',
        open: function (callback) {
            layui.assert.limit('asr');
            const loading = layui.layer.load(2);
            layui.$.get('aliyun/credentials', (data) => {
                console.log(data);
                this.ws = new WebSocket(`wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1?token=${data}`);
                this.ws.onopen = () => {
                    layui.layer.close(loading);
                    this.task_id = this.uuid();
                    this.start();
                };
                this.ws.onclose = () => {
                    layui.layer.close(loading);
                    console.log('WebSocket关闭！！！');
                    this.ws = null;
                };
                this.ws.onerror = () => {
                    layui.layer.close(loading);
                    layui.layer.msg('语音识别失败！！！（WebSocket）');
                    this.ws = null;
                };
                this.ws.onmessage = (e) => {
                    console.log(e.data);
                    const data = JSON.parse(e.data);
                    if (data.header.name === "TranscriptionStarted" && data.header.status === 20000000) {
                        typeof callback === 'function' && callback(null, null);
                    }
                    if (data.header.name === 'SentenceBegin' && data.header.status === 20000000) {
                    }
                    if (data.header.name === 'TranscriptionResultChanged' && data.header.status === 20000000) {
                        typeof callback === 'function' && callback(false, data.payload.result);
                    }
                    if (data.header.name === 'SentenceEnd' && data.header.status === 20000000) {
                        typeof callback === 'function' && callback(true, data.payload.result);
                    }
                    if (data.header.name === 'TranscriptionCompleted' && data.header.status === 20000000) {
                        this.ws.close();
                    }
                    if (data.header.status !== 20000000) {
                        this.ws.close();
                        console.log('语音识别失败:', data);
                        //layui.layer.msg(`语音识别失败！（${data.header.status}:${data.header.status_text}）`);
                    }
                };
            }).fail((xhr, status, error) => {
                layui.layer.close(loading);
                layui.layer.msg(`语音识别请求异常，请重试！（${error || status}）`);
            });
        },
        close: function () {
            if (this.ws) {
                this.ws.close();
            }
        },
        uuid: function () {
            return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
                (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
            ).replace(/-/g, '');
        },
        header: function (name) {
            return {
                message_id: this.uuid(),
                task_id: this.task_id,
                namespace: 'SpeechTranscriber',
                name: name,
                appkey: this.app_key
            };
        },
        start: function () {
            if (this.ws) {
                this.ws.send(JSON.stringify({
                    header: this.header('StartTranscription'),
                    payload: {
                        format: 'PCM',
                        sample_rate: 16000,
                        enable_intermediate_result: true,
                        enable_punctuation_prediction: true,
                        enable_inverse_text_normalization: true
                    }
                }));
            }
        },
        stop: function () {
            if (this.ws) {
                this.ws.send(JSON.stringify({header: this.header('StopTranscription')}));
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
                layui.$.post(`aliyun/speech/recognitions`, {
                    appKey: this.app_key,
                    vop: encodeURIComponent(e.target.result.split(',')[1])
                }, (data) => {
                    console.log(data);
                    if (data.status !== 20000000) {
                        layui.layer.msg(`语音识别失败！（${data.status}:${data.message}）`);
                    } else {
                        typeof callback === 'function' && callback(data.result);
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