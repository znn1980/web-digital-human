layui.define(function (exports) {
    const $wakeup = {
        lang: 'zh-CN',
        voices: [],
        voice: null,
        listening: false,
        speaking: false,
        synthesis: new SpeechSynthesisUtterance(),
        recognition: new (window.SpeechRecognition || window.webkitSpeechRecognition)(),
        listen: function (keywords, callback) {
            $wakeup.recognition.continuous = true;
            $wakeup.recognition.interimResults = false;
            $wakeup.recognition.lang = $wakeup.lang;
            $wakeup.recognition.onresult = (e) => {
                const result = e.results[e.results.length - 1][0].transcript.trim();
                console.log('语音识别：', result);
                if (!result) return;
                //唤醒词匹配 1.其中一个匹配 2.全部匹配
                //keywords.some(keyword => result.includes(keyword))
                //keywords.every(keyword => result.includes(keyword))
                if (!$wakeup.listening && keywords.some(keyword => result.includes(keyword))) {//唤醒词
                    $wakeup.listening = true;
                    layui.layer.msg('我在听，请说出您的问题。', {
                        time: 1000 * 15, icon: 16, shade: 0.3, shadeClose: false
                        , end: function () {
                            $wakeup.listening = false;
                            console.log('我在听，超时...');
                        }
                    });
                } else if (this.listening) {
                    layui.layer.closeLast('dialog', function () {
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
        speak: function (text, start, end) {
            if ($wakeup.speaking || !text || text === '') return;
            if ($wakeup.listening) window.speechSynthesis.cancel();
            $wakeup.speaking = true;
            $wakeup.listening = false;
            $wakeup.recognition.stop();
            $wakeup.synthesis.text = text;
            $wakeup.synthesis.lang = $wakeup.lang;
            if ($wakeup.voice) $wakeup.synthesis.voice = $wakeup.voice;
            $wakeup.synthesis.onstart = () => {
                console.log('~~~开始语音合成~~~');
                $wakeup.speaking = true;
                $wakeup.listening = false;
                typeof start === 'function' && start();
            };
            $wakeup.synthesis.onend = () => {
                console.log('~~~结束语音合成~~~');
                $wakeup.speaking = false;
                $wakeup.resume();
                typeof end === 'function' && end();
            };
            $wakeup.synthesis.onerror = (e) => {
                console.error('语音合成错误:', e.error);
                $wakeup.speaking = false;
                $wakeup.resume();
                typeof end === 'function' && end();
            };
            window.speechSynthesis.speak($wakeup.synthesis);
        },
        buffer: '',
        reg: /[。！？：；.!?:;]/,
        say: function (done, text, start, end) {
            $wakeup.buffer += text ? text : '';
            if (done) {
                $wakeup.push($wakeup.buffer, start, end);
                $wakeup.buffer = '';
            } else {
                let index;
                while ((index = $wakeup.buffer.search($wakeup.reg)) !== -1) {
                    const value = $wakeup.buffer.substring(0, index + 1);
                    $wakeup.push(value, start, end);
                    $wakeup.buffer = $wakeup.buffer.substring(index + 1);
                }
            }
        },
        speaks: [],
        push: function (text, start, end) {
            if (!text || text === '') return;
            $wakeup.speaks.push(text);
            $wakeup.speaking = true;
            $wakeup.listening = false;
            $wakeup.recognition.stop();
            const synthesis = new SpeechSynthesisUtterance();
            synthesis.text = text;
            synthesis.lang = $wakeup.lang;
            if ($wakeup.voice) synthesis.voice = $wakeup.voice;
            synthesis.onstart = () => {
                console.log('~~~开始语音合成~~~');
                const value = $wakeup.speaks.shift();
                console.log('语音合成:', value);
                typeof start === 'function' && start();
            };
            synthesis.onend = () => {
                console.log('~~~结束语音合成~~~');
                if ($wakeup.speaks.length === 0) {
                    $wakeup.speaking = false;
                    $wakeup.resume();
                    typeof end === 'function' && end();
                }
            };
            synthesis.onerror = (e) => {
                console.error('语音合成错误:', e.error);
                if ($wakeup.speaks.length === 0) {
                    $wakeup.speaking = false;
                    $wakeup.resume();
                    typeof end === 'function' && end();
                }
            };
            window.speechSynthesis.speak(synthesis);
        },
        resume: function () {
            if ($wakeup.speaking) return;
            $wakeup.recognition.stop();
            if (!$wakeup.speaking) {
                $wakeup.recognition.start();
            }
        },
        cancel: function () {
            $wakeup.buffer = '';
            $wakeup.speaks = [];
            window.speechSynthesis.cancel();
            $wakeup.listening = false;
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