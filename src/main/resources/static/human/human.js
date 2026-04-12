layui.define(['assert'], function (exports) {
    exports('human', {
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
            },
            {
                title: '关关', value: 'human/guanguan',
                standby: {frame: 152, interval: 80},
                speak: {frame: 114, interval: 60}
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
        model: [],
        //大模型请求
        request: {
            model: '',
            stream: true,
            messages: [],
            enable_thinking: false,
            thinking: {type: 'disabled'},
            enable_search: false,
            web_search: {enable: false}
        },
        //大模型应答
        response: {messages: []},
        //初始化画布，并加载第一个数字人形象
        init: function (human, canvas, callback) {
            console.log(human.clientWidth + 'x' + human.clientHeight);
            this.canvas = canvas;
            this.canvas.width = human.clientWidth;
            this.canvas.height = human.clientHeight;
            this.ctx = this.canvas.getContext('2d');
            this.load(this.human[0], callback);
        },
        //加载数字人形象
        load: function (human, callback) {
            this.request.messages = [{
                role: 'system', content: `你的名字叫${human.title}，是一位智能助手。`
            }];
            this.me = human;
            this.stop();
            const loading = layui.layer.load(2, {
                time: 0, shade: 0.6, shadeClose: false,
                content: '<span id="loading" style="font-weight:bold;color:white;' +
                    'position:absolute;left:-30px;width:150px;">加载中...<span>'
            });
            //加载数字人待机形象
            this.frames.standby = [];
            for (let i = 1; i <= human.standby.frame; i++) {
                const image = new Image();
                image.src = `${human.value}/standby/standby (${i}).png`;
                this.frames.standby.push(image);
            }
            //加载数字人说话形象
            this.frames.speak = [];
            for (let i = 1; i <= human.speak.frame; i++) {
                const image = new Image();
                image.src = `${human.value}/speak/speak (${i}).png`;
                this.frames.speak.push(image);
            }
            //加载全部图像
            let length = 0, frames = [...this.frames.standby, ...this.frames.speak];
            frames.forEach((value) => {
                this.onload(value, () => {
                    length++;
                    layui.$('#loading').html(`加载中...（${Math.round(length / frames.length * 100)}%）`);
                    if (length === frames.length) {
                        this.draw(this.frames.standby[0]);
                        this.standby();
                        layui.layer.close(loading);
                        typeof callback === 'function' && callback();
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
                this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
                const scale = Math.min(this.canvas.width / image.width, this.canvas.height / image.height);
                const width = image.width * scale;
                const height = image.height * scale;
                const x = (this.canvas.width - width) / 2;
                const y = (this.canvas.height - height) / 2;
                this.ctx.drawImage(image, x, y, width, height);
            }
        },
        //停止待机与说话动画
        stop: function () {
            cancelAnimationFrame(this.id.standby);
            cancelAnimationFrame(this.id.speak);
            this.frame.standby = 0;
            this.frame.speak = 0;
        },
        timestamp: null,
        //待机动画
        standby: function () {
            this.stop();
            this.id.standby = requestAnimationFrame((timestamp) => {
                this._standby(timestamp);
            });
        },
        _standby: function (timestamp) {
            if (timestamp - this.timestamp >= this.me.standby.interval) {
                this.draw(this.frames.standby[this.frame.standby]);
                this.frame.standby = (this.frame.standby + 1) % this.frames.standby.length;
                this.timestamp = timestamp;
            }
            this.id.standby = requestAnimationFrame((timestamp) => {
                this._standby(timestamp);
            });
        },
        //说话动画
        speak: function () {
            this.stop();
            this.id.speak = requestAnimationFrame((timestamp) => {
                this._speak(timestamp);
            });
        },
        _speak: function (timestamp) {
            if (timestamp - this.timestamp >= this.me.speak.interval) {
                this.draw(this.frames.speak[this.frame.speak]);
                this.frame.speak = (this.frame.speak + 1) % this.frames.speak.length;
                this.timestamp = timestamp;
            }
            this.id.speak = requestAnimationFrame((timestamp) => {
                this._speak(timestamp);
            });
        },
        think: 0,
        chat: function (text, uuid) {
            text = text
                .replaceAll('<table>', '<table class="layui-table" lay-size="sm">')
                .replaceAll('<think>', `
                  <div class="layui-timeline">
                    <div class="layui-timeline-item">
                      <i class="layui-icon layui-timeline-axis layui-icon-component"></i>
                      <div class="layui-timeline-content layui-text">
                        <h6 class="layui-timeline-title">深度思考</h6>
                `)
                .replaceAll('</think>', `
                      </div>
                    </div>
                    <div class="layui-timeline-item">
                      <i class="layui-icon layui-timeline-axis layui-icon-component"></i>
                      <div class="layui-timeline-content layui-text">
                        <h6 class="layui-timeline-title">已思考（用时${(this.think / 1000).toFixed(2)}秒）</h6>
                      </div>
                    </div>
                  </div>
                `);
            if (layui.$(`#CHAT-${uuid}`).length > 0) {
                layui.$(`#CHAT-${uuid}`).html(text);
            } else {
                const ymd = layui.util.toDateString(Date.now(), 'yyyy-MM-dd HH:mm:ss');
                layui.$('.layim-chat-main').children().append(`
                      <li class="${uuid ? '' : 'layim-chat-role-user'}">
                        <div class="layim-chat-userinfo">
                          <img src="${uuid ? `${this.me.value}/me.png` : 'logo.png'}" alt="">
                          <cite>${uuid ? `${this.me.title}<i>${ymd}</i>` : `<i>${ymd}</i>我`}</cite>
                        </div>
                        <div id="CHAT-${uuid || Date.now()}" class="layim-chat-text layui-text">${text}</div>
                      </li>
                `);
            }
            const dom = document.querySelector('.layim-chat-main');
            dom.scrollTop = dom.scrollHeight;
        },
        sse: {
            abort: null,
            close: function () {
                if (this.abort) {
                    this.abort.abort();
                }
            }
        },
        //说话
        send: function (text, callback) {
            layui.assert.limit('chat');
            this.response.messages = [];
            this.request.messages.push({role: 'user', content: text});
            console.log(this.request);
            this.sse.abort = new AbortController();
            let thinking = false;
            const loading = layui.layer.load(2);
            SSE.fetchEventSource('chat/completions', {
                method: 'POST', signal: this.sse.abort.signal,
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(this.request),
                onopen: (response) => {
                    layui.layer.close(loading);
                    console.log(response);
                    if (!response.ok) throw new Error(response.status + '-' + response.statusText);
                },
                onmessage: (msg) => {
                    console.log(msg);
                    if (msg.data === '[DONE]') {
                        if (layui.$.trim(this.response.messages.join(''))) {
                            this.request.messages.push({
                                role: 'assistant',
                                content: this.response.messages.join('')
                            });
                        }
                    }
                    if (msg.data.startsWith('{') && msg.data.endsWith('}')) {
                        const data = JSON.parse(msg.data);
                        if (data.choices && data.choices[0] && data.choices[0].delta) {
                            const reasoningContent = data.choices[0].delta.reasoning_content || '';
                            const content = data.choices[0].delta.content || '';
                            const text = [];
                            if (layui.$.trim(reasoningContent)) {
                                if (thinking === false) {
                                    thinking = true;
                                    this.think = Date.now();
                                    text.push('<think>');
                                }
                                text.push(reasoningContent);
                            }
                            if (layui.$.trim(content)) {
                                if (thinking === true) {
                                    thinking = false;
                                    this.think = Date.now() - this.think;
                                    text.push('</think>');
                                }
                                text.push(content);
                                this.response.messages.push(content);
                            }
                            typeof callback === 'function' && callback(false, text.join(''));
                        }
                    }
                },
                onclose: () => {
                    layui.layer.close(loading);
                    console.log('SSE关闭！');
                    typeof callback === 'function' && callback(true, null);
                },
                onerror: (error) => {
                    layui.layer.close(loading);
                    console.log('SSE异常！', error);
                    this.request.messages.pop();
                    layui.layer.msg(`发送数据失败！（${error}）`);
                    typeof callback === 'function' && callback(true, null);
                    throw error;
                }
            });
        },
        models: function (callback) {
            layui.$.get('chat/models', (data) => {
                this.model = data;
                this.request.model = data[0].value;
                typeof callback === 'function' && callback();
            }).fail((xhr, status, error) => {
                layui.layer.msg(`模型请求异常，请重试！（${error || status}）`);
            });
        }
    });
});

