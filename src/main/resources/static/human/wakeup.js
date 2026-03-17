layui.define(function (exports) {

    const $wakeup = {
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
            $wakeup.recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
            $wakeup.recognition.continuous = true;
            $wakeup.recognition.interimResults = false;
            $wakeup.recognition.maxAlternatives = 1;
            $wakeup.recognition.lang = $wakeup.lang;
            $wakeup.recognition.onresult = (e) => {
                const result = e.results[e.results.length - 1][0].transcript.trim();
                console.log('语音识别：', result);
                if (!result) return;
                //唤醒词匹配 1.其中一个匹配 2.全部匹配
                //keywords.some(keyword => result.includes(keyword))
                //keywords.every(keyword => result.includes(keyword))
                if (!$wakeup.listening && keywords.every(keyword => result.includes(keyword))) {//唤醒词
                    $wakeup.listening = true;
                    layui.layer.load(2, {
                        time: timeout, shade: 0.6, shadeClose: true
                        , content: '<span style="font-weight:bold;color:white;' +
                            'position:absolute;left:-20px;width:120px;">我在听...</span>'
                        , end: function () {
                            $wakeup.listening = false;
                            console.log('我在听，超时...');
                        }
                    });
                } else if (this.listening) {
                    layui.layer.closeLast('loading', function () {
                        $wakeup.listening = true;
                        typeof callback === 'function' && callback(result);
                    });
                }
            };
            $wakeup.recognition.onstart = () => {
                console.log('开始语音识别...');
            };
            $wakeup.recognition.onend = () => {
                console.log('结束语音识别...');
                $wakeup.resume();
            };
            $wakeup.recognition.onerror = (e) => {
                console.error('语音识别错误:', e.error);
                $wakeup.resume();
            };
            $wakeup.recognition.start();
        },
        speak: function (done, text, start, end) {
            $wakeup.listening = false;
            $wakeup.buffer += text ? text : '';
            if (done) {
                $wakeup.speaks.push(this.buffer);
                if (!$wakeup.speaking) $wakeup.play(start, end);
                return $wakeup.buffer = '';
            }
            let index;
            while ((index = $wakeup.buffer.search($wakeup.reg)) !== -1) {
                $wakeup.speaks.push($wakeup.buffer.substring(0, index + 1));
                if (!$wakeup.speaking) $wakeup.play(start, end);
                $wakeup.buffer = $wakeup.buffer.substring(index + 1);
            }
        },
        play: function (start, end) {
            if ($wakeup.speaks.length === 0) {
                $wakeup.speaking = false;
                if ($wakeup.recognition.continuous) {
                    $wakeup.resume();
                }
                return typeof end === 'function' && end();
            }
            $wakeup.speaking = true;
            $wakeup.listening = false;
            $wakeup.recognition.stop();
            const text = $wakeup.speaks.shift();
            const utterance = new SpeechSynthesisUtterance();
            utterance.text = text;
            utterance.lang = $wakeup.recognition.lang;
            if ($wakeup.voice) {
                utterance.voice = $wakeup.voice;
            }
            utterance.onstart = () => {
                console.log('~~~开始语音合成~~~');
                console.log('语音合成：', text, $wakeup.speaks.length);
                typeof start === 'function' && start();
            }
            utterance.onend = () => {
                console.log('~~~结束语音合成~~~');
                $wakeup.play(start, end);
            }
            utterance.onerror = (e) => {
                console.error('语音合成错误：', e.error);
                $wakeup.play(start, end);
            }
            window.speechSynthesis.speak(utterance);
        },
        resume: function () {
            $wakeup.recognition.stop();
            if ($wakeup.speaking) return;
            window.setTimeout(function () {
                try {
                    $wakeup.recognition.start();
                } catch (e) {
                    console.error('重启语音识别失败：', e);
                }
            }, 100);
        },
        cancel: function () {
            $wakeup.buffer = '';
            $wakeup.speaks = [];
            $wakeup.listening = false;
            window.speechSynthesis.cancel();
        },
        getVoices: function (callback) {
            window.speechSynthesis.onvoiceschanged = () => {
                window.speechSynthesis.getVoices().forEach(voice => {
                    if (voice.localService && voice.lang === $wakeup.lang) {
                        if (!$wakeup.voices.some(value => value.name === voice.name)) {
                            $wakeup.voices.push(voice);
                        }
                    }
                });
                const sound = $wakeup.voices.map(voice => ({title: voice.name, value: voice.name}));
                typeof callback === 'function' && callback(sound);
            }
        }
    };

    exports('wakeup', $wakeup);
});