layui.define(function (exports) {
    exports('wakeup', {
        lang: 'zh-CN',
        voices: [],
        voice: null,
        listening: false,
        speaking: false,
        recognition: null,
        buffer: '',
        reg: /[。！？：；.!?:;\n]/,
        speaks: [],
        listen: function (keywords, timeout, callback) {
            if (!window.speechSynthesis) layui.layer.msg('当前浏览器不支持语音合成！');
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            if (!SpeechRecognition) return layui.layer.msg('当前浏览器不支持语音识别！');
            this.recognition = new SpeechRecognition();
            this.recognition.continuous = true;
            this.recognition.interimResults = false;
            this.recognition.maxAlternatives = 1;
            this.recognition.lang = this.lang;
            let loading;
            this.recognition.onresult = (e) => {
                const result = e.results[e.results.length - 1][0].transcript.trim();
                console.log('语音识别：', result);
                if (!result) return;
                //唤醒词匹配 1.其中一个匹配 2.全部匹配
                //keywords.some(keyword => result.includes(keyword))
                //keywords.every(keyword => result.includes(keyword))
                if (!this.listening && keywords.every(keyword => result.includes(keyword))) {//唤醒词
                    this.listening = true;
                    loading = layui.layer.load(2, {
                        time: timeout, shade: 0.6, shadeClose: true
                        , content: '<span style="font-weight:bold;color:white;' +
                            'position:absolute;left:-30px;width:150px;">我在听...</span>'
                        , end: () => {
                            this.listening = false;
                            console.log('我在听，超时...');
                        }
                    });
                } else if (this.listening) {
                    layui.layer.close(loading, () => {
                        this.listening = true;
                        typeof callback === 'function' && callback(result);
                    });
                }
            };
            this.recognition.onstart = () => {
                console.log('开始语音识别...');
            };
            this.recognition.onend = () => {
                console.log('结束语音识别...');
                this.resume();
            };
            this.recognition.onerror = (e) => {
                console.error('语音识别错误:', e.error);
                this.resume();
            };
            this.recognition.start();
        },
        speak: function (done, text, callback) {
            this.listening = false;
            this.buffer += text ? text : '';
            if (done) {
                this.speaks.push(this.buffer);
                if (!this.speaking) this.play(callback);
                return this.buffer = '';
            }
            let index;
            while ((index = this.buffer.search(this.reg)) !== -1) {
                this.speaks.push(this.buffer.substring(0, index + 1));
                if (!this.speaking) this.play(callback);
                this.buffer = this.buffer.substring(index + 1);
            }
        },
        play: function (callback) {
            if (this.speaks.length === 0) {
                this.speaking = false;
                if (this.recognition && this.recognition.continuous) {
                    this.resume();
                }
                return callback && typeof callback.end === 'function' && callback.end();
            }
            this.speaking = true;
            this.listening = false;
            this.recognition && this.recognition.stop();
            const utterance = new SpeechSynthesisUtterance();
            utterance.text = this.speaks.shift();
            utterance.lang = this.lang;
            if (this.voice) utterance.voice = this.voice;
            utterance.onstart = () => {
                console.log('~~~开始语音合成~~~');
                console.log('语音合成：', utterance.text, this.speaks.length);
                callback && typeof callback.play === 'function' && callback.play();
            }
            utterance.onend = () => {
                console.log('~~~结束语音合成~~~');
                this.play(callback);
            }
            utterance.onerror = (e) => {
                console.error('语音合成错误：', e.error);
                this.play(callback);
            }
            window.speechSynthesis && window.speechSynthesis.speak(utterance);
        },
        resume: function () {
            this.recognition && this.recognition.stop();
            if (this.speaking) return;
            window.setTimeout(() => {
                try {
                    this.recognition && this.recognition.start();
                } catch (e) {
                    console.error('重启语音识别失败：', e);
                }
            }, 100);
        },
        cancel: function () {
            this.buffer = '';
            this.speaks = [];
            this.listening = false;
            window.speechSynthesis && window.speechSynthesis.cancel();
        },
        _getVoices: function (callback) {
            window.speechSynthesis.getVoices().forEach(voice => {
                console.log('语音：', voice)
                if (voice.localService && voice.lang === this.lang) {
                    if (!this.voices.some(value => value.name === voice.name)) {
                        this.voices.push(voice);
                    }
                }
            });
            const sound = this.voices.map(voice => ({title: voice.name, value: voice.name}));
            if (sound.length > 0) typeof callback === 'function' && callback(sound);
        },
        getVoices: function (callback) {
            if (typeof speechSynthesis === 'undefined'
                && speechSynthesis.onvoiceschanged === undefined) return;
            this._getVoices(callback);
            window.speechSynthesis.onvoiceschanged = () => {
                this._getVoices(callback);
            }
        }
    });
});