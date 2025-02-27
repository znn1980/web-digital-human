layui.define(function (exports) {
    const $human = {
        //画布
        ctx: null,
        canvas: null,
        //待机与说话动画ID
        id: {standby: null, speak: null},
        //待机与说话动画当前帧
        frame: {standby: 0, speak: 0},
        //待机与说话动画全部帧
        frames: {standby: [], speak: []},
        //数字人形象【名称、位置、待机帧数与间隔、说话帧数与间隔】
        human: [
            {
                title: '小川', value: 'human/xiaochuan',
                standby: {frame: 113, interval: 20},
                speak: {frame: 186, interval: 30}
            }
        ],
        me: null,
        //场景
        scene: [
            {title: '场景1', value: 'human/background/bg_1.png'},
            {title: '场景2', value: 'human/background/bg_2.png'},
            {title: '场景3', value: 'human/background/bg_3.png'},
            {title: '场景4', value: 'human/background/bg_4.png'},
            {title: '场景5', value: 'human/background/bg_5.png'},
            {title: '场景6', value: 'human/background/bg_6.png'},
            {title: '场景7', value: 'human/background/bg_7.png'}
        ],
        //大模型
        model: [
            {title: 'DeepSeek-V3', value: 'deepseek-v3'},
            {title: 'DeepSeek-R1', value: 'deepseek-r1'}
        ],
        //大模型请求
        request: {model: 'deepseek-v3', stream: true, messages: []},
        //大模型应答
        response: {messages: []},
        //初始化画布，并加载第一个数字人形象
        init: function () {
            const human = document.querySelector('#human');
            console.log(human.clientWidth + 'x' + human.clientHeight);
            $human.canvas = document.querySelector('#canvas');
            $human.canvas.width = human.clientWidth;
            $human.canvas.height = human.clientHeight;
            $human.ctx = $human.canvas.getContext('2d');
            $human.load($human.human[0]);
        },
        //加载数字人形象
        load: function (human) {
            $human.me = human;
            $human.stop();
            const load = layui.layer.msg('数字人，加载中...', {
                icon: 16, shade: 0.3, time: 0,
                content: '<span id="loading">数字人，加载中...<span>'
            });
            //加载数字人待机形象
            $human.frames.standby = [];
            for (let i = 1; i <= human.standby.frame; i++) {
                const image = new Image();
                image.src = `${human.value}/standby/standby (${i}).png`;
                $human.frames.standby.push(image);
            }
            //加载数字人说话形象
            $human.frames.speak = [];
            for (let i = 1; i <= human.speak.frame; i++) {
                const image = new Image();
                image.src = `${human.value}/speak/speak (${i}).png`;
                $human.frames.speak.push(image);
            }
            //加载全部图像
            const images = [...$human.frames.standby, ...$human.frames.speak];
            let length = 0;
            images.forEach(function (image) {
                $human.onload(image, function () {
                    length++;
                    const total = human.standby.frame + human.speak.frame;
                    layui.$('#loading').html('数字人，加载中...（' + Math.round(length / total * 100) + '%）');
                    if (length === total) {
                        $human.draw($human.frames.standby[0]);
                        layui.layer.close(load);
                        $human.standby();
                    }
                });
            });
        },
        //图像加载
        onload: function (image, callback) {
            if (image.complete) {
                typeof callback === 'function' && callback();
            } else {
                image.onload = function () {
                    typeof callback === 'function' && callback();
                };
                image.onerror = function () {
                    typeof callback === 'function' && callback();
                };
            }
        },
        //画一帧图像
        draw: function (image) {
            if (image && image.width > 0 && image.height > 0) {
                $human.ctx.clearRect(0, 0, $human.canvas.width, $human.canvas.height);
                const scale = Math.min($human.canvas.width / image.width, $human.canvas.height / image.height);
                const width = image.width * scale;
                const height = image.height * scale;
                const x = ($human.canvas.width - width) / 2;
                const y = ($human.canvas.height - height) / 2;
                $human.ctx.drawImage(image, x, y, width, height);
            }
        },
        //停止待机与说话动画
        stop: function () {
            cancelAnimationFrame($human.id.standby);
            cancelAnimationFrame($human.id.speak);
            $human.frame.standby = 0;
            $human.frame.speak = 0;
        },
        timestamp: null,
        //待机动画
        standby: function () {
            $human.stop();
            $human.id.standby = requestAnimationFrame($human._standby);
        },
        _standby: function (timestamp) {
            if (timestamp - $human.timestamp >= $human.me.standby.interval) {
                $human.draw($human.frames.standby[$human.frame.standby]);
                $human.frame.standby = ($human.frame.standby + 1) % $human.frames.standby.length;
                $human.timestamp = timestamp;
            }
            $human.id.standby = requestAnimationFrame($human._standby);
        },
        //说话动画
        speak: function () {
            $human.stop();
            $human.id.speak = requestAnimationFrame($human._speak);
        },
        _speak: function (timestamp) {
            if (timestamp - $human.timestamp >= $human.me.speak.interval) {
                $human.draw($human.frames.speak[$human.frame.speak]);
                $human.frame.speak = ($human.frame.speak + 1) % $human.frames.speak.length;
                $human.timestamp = timestamp;
            }
            $human.id.speak = requestAnimationFrame($human._speak);
        },
        //说话
        send: function (text, audio, callback) {
            layui.tts.init(audio, function () {
                $human.request.messages.push({role: 'user', content: text});
                console.log($human.request);
                fetch('api/chat/completions', {
                    method: 'POST', body: JSON.stringify($human.request)
                }).then(function (response) {
                    if (response.ok) {
                        return response.body;
                    } else {
                        throw new Error(response.status + '-' + response.statusText);
                    }
                }).then(function (data) {
                    $human.response.messages = [];
                    $human.stream(data.getReader(), callback);
                }).catch(function (e) {
                    typeof callback === 'function' && callback(true, null);
                    $human.request.messages.pop();
                    layui.layer.msg('发送数据失败！（' + e + '）');
                });
            });
        },
        stream: function (reader, callback) {
            return reader.read().then(function ({done, value}) {
                console.log(done);
                if (done) {
                    const message = $human.response.messages.join('');
                    if (message !== '') {
                        $human.request.messages.push({role: 'assistant', content: message});
                    }
                } else {
                    const data = new TextDecoder().decode(value);
                    console.log(data);
                    const content = JSON.parse(data).choices[0].delta.content || '';
                    console.log(content);
                    layui.tts.send(content);
                    $human.response.messages.push(content);
                    $human.stream(reader, callback);
                }
                const message = $human.response.messages.join('');
                typeof callback === 'function' && callback(done, message);
            });
        },
        //语音转文字
        asr: function (base64, callback) {
            let load = layui.layer.load(0);
            layui.$.post('api/speech/recognitions', base64, function (data) {
                console.log(data);
                if (data.err_no !== 0) {
                    layui.layer.msg('语音识别失败！（' + data.err_no + ':' + data.err_msg + '）');
                } else {
                    typeof callback === 'function' && callback(data.result[0]);
                }
                layui.layer.close(load);
            }).error(function (xhr, status, error) {
                layui.layer.close(load);
                layui.layer.msg('语音识别请求异常，请重试！（' + (error || status) + '）');
            });
        },
        //麦克风录音
        mike: function (audio, duration, callback) {
            audio.start().then(() => {
                console.log('开始录音...');
                const load = layui.layer.msg('录音中...', {
                    time: 0, icon: 16, shade: 0.3, shadeClose: true,
                    content: '<span id="loading">录音中...（30秒）<span>',
                    end: function () {
                        console.log('停止录音...');
                        const reader = new FileReader();
                        reader.onload = function () {
                            typeof callback === 'function' && callback(reader.result.split(',')[1]);
                        };
                        reader.onerror = function () {
                            layui.layer.msg('录音失败！（' + reader.error + '）');
                        };
                        reader.readAsDataURL(audio.getPCMBlob());
                    }
                });
                audio.onprogress = function (params) {
                    console.log(params);
                    layui.$('#loading').html('录音中...（' + (duration - parseInt(params.duration)) + '秒）按下停止');
                    if (params.duration > duration) {
                        layui.layer.close(load);
                    }
                };
            }, (error) => {
                layui.layer.msg(`麦克风打开失败！（${error.name}:${error.message}）`);
            });
        },
        qa: function (text, uuid) {
            if (uuid && uuid !== '') {
                layui.$(`#CHAT-${uuid}`).html(text);
            } else {
                const _date = new Date();
                const _uuid = _date.getTime();
                const _time = layui.util.toDateString(_date, 'yyyy-MM-dd HH:mm:ss');
                layui.$('.layim-chat-main').children().append([
                    '<li class="' + (typeof uuid === 'undefined' ? 'layim-chat-role-user' : '') + '">',
                    ' <div class="layim-chat-userinfo">',
                    '  <img src="' + (typeof uuid === 'undefined' ? 'logo.png' : `${$human.me.value}/human.png`) + '" alt="">',
                    '  <cite>' + (typeof uuid === 'undefined' ? '<i>' + _time + '</i>我' : $human.me.title + '<i>' + _time + '</i>') + '</cite>',
                    ' </div>',
                    ' <div id="CHAT-' + _uuid + '" class="layim-chat-text layui-text">' + text + '</div>',
                    '</li>'
                ].join(''));
                uuid = _uuid;
            }
            const dom = document.querySelector('.layim-chat-main');
            dom.scrollTop = dom.scrollHeight;
            return uuid;
        }
    };
    exports('human', $human);
});

