layui.define(function (exports) {
    const $wakeup = {
        lang: 'zh-CN',
        listening: false,
        speaking: false,
        synthesis: null,
        recognition: new (window.SpeechRecognition || window.webkitSpeechRecognition)(),
        listen: function (wakeup, callback) {
            $wakeup.recognition.continuous = true;
            $wakeup.recognition.interimResults = false;
            $wakeup.recognition.lang = $wakeup.lang;
            $wakeup.recognition.onresult = (e) => {
                const result = e.results[e.results.length - 1][0].transcript.trim();
                console.log('语音识别：', result);
                if (!result) return;
                if (result.includes(wakeup)) {//唤醒词
                    if (!$wakeup.listening) {
                        $wakeup.listening = true;
                        layui.layer.msg('我在听，请说出您的问题。', {
                            time: 1000 * 15, icon: 16, shade: 0.3, shadeClose: false
                            , end: function () {
                                $wakeup.listening = false;
                                console.log('我在听，超时...');
                            }
                        });
                    }
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
            if ($wakeup.synthesis === null && text && text !== '') {
                if ($wakeup.listening) {
                    window.speechSynthesis.cancel();
                }
                $wakeup.recognition.stop();
                $wakeup.synthesis = new SpeechSynthesisUtterance(text);
                $wakeup.synthesis.lang = $wakeup.lang;
                $wakeup.synthesis.onstart = () => {
                    console.log('~~~开始语音合成~~~');
                    typeof start === 'function' && start();
                    $wakeup.speaking = true;
                };
                $wakeup.synthesis.onend = () => {
                    console.log('~~~结束语音合成~~~');
                    typeof end === 'function' && end();
                    $wakeup.speaking = false;
                    $wakeup.synthesis = null;
                    $wakeup.resume();
                };
                $wakeup.synthesis.onerror = (e) => {
                    console.error('语音合成错误:', e.error);
                    typeof end === 'function' && end();
                    $wakeup.speaking = false;
                    $wakeup.synthesis = null;
                    $wakeup.resume();
                };
                window.speechSynthesis.speak($wakeup.synthesis);
            }
        },
        resume: function () {
            if ($wakeup.speaking) return;
            $wakeup.recognition.stop();
            if (!$wakeup.speaking) {
                $wakeup.recognition.start();
            }
        }
    };

    exports('wakeup', $wakeup);
});