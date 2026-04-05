class WebSpeech {
    /**
     * 语音识别与合成类构造函数
     * 初始化语音识别服务、配置参数及相关状态
     * @param {string} [lang='zh-CN'] - 语音识别语言代码，默认使用中文（zh-CN）
     * @returns {void}
     */
    constructor(lang) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) return alert('当前浏览器不支持语音识别');
        // 创建浏览器语音识别实例，兼容 Chrome 的 webkit 前缀版本
        this.speechRecognition = new SpeechRecognition();
        // 配置为不返回中间结果，仅在识别完成后返回最终结果
        this.speechRecognition.interimResults = false;
        // 设置语音识别的语言类型
        this.speechRecognition.lang = lang || 'zh-CN';
        // 设置最大备选结果数量为 1，只返回最匹配的识别结果
        this.speechRecognition.maxAlternatives = 1;
        // 语音识别启动时的日志记录
        this.speechRecognition.onstart = () => {
            console.log('开始语音识别...');
        }
        this.voice = null;
        // 初始化语音状态标志
        this.speaking = false;
        this.listening = false;
        // 初始化语音文本缓冲区和语音队列
        this.buffer = '';
        this.queue = [];
        this.reg = /[。！？：；.!?:;\n]/;
        // 创建加载动画控制器
        this.loader = new WebSpeechLoader();
    }

    /**
     * 语音唤醒监听函数
     * 持续监听用户语音，当检测到关键词后进入监听模式，在超时时间内等待用户指令
     * @param {Array<string>} keywords - 唤醒关键词数组，需包含所有触发词
     * @param {number} timeout - 监听超时时间（毫秒），超时后自动退出监听模式
     * @param {function(string): void} callback - 识别成功后的回调函数，参数为识别结果文本
     * @returns {void}
     */
    wakeup(keywords, timeout, callback) {
        // 配置为持续监听模式，自动重启识别会话
        this.speechRecognition.continuous = true;
        // 处理语音识别结果事件
        this.speechRecognition.onresult = (e) => {
            const result = e.results[e.results.length - 1][0].transcript.trim();
            console.log('语音识别：', result);
            if (!result) return;
            // 首次匹配到唤醒词时，进入监听模式并显示加载动画
            if (!this.listening && keywords.every(keyword => result.includes(keyword))) {
                this.listening = true;
                this.loader.show(timeout, () => {
                    this.listening = false;
                });
            } else if (this.listening) {
                // 已在监听模式时，执行用户指令回调
                this.loader.close();
                this.listening = true;
                typeof callback === 'function' && callback(result);
            }
        }
        // 语音识别结束时自动重启，保持持续监听
        this.speechRecognition.onend = () => {
            console.log('结束语音识别...');
            this.resume();
        }
        // 处理语音识别错误，自动尝试恢复
        this.speechRecognition.onerror = (e) => {
            console.error('语音识别错误：', e.error);
            this.resume();
        }
        // 启动语音识别服务，捕获可能的异常
        try {
            this.speechRecognition.start();
        } catch (e) {
            console.error('启动语音识别失败：', e);
        }
    }

    /**
     * 单次语音识别监听函数
     * 启动一次性的语音识别，识别完成后自动停止并返回结果
     * @param {function(string): void} callback - 识别成功后的回调函数，参数为识别结果文本
     * @param end
     * @returns {void}
     */
    listen(callback, end) {
        // 配置为单次识别模式，识别完成后自动停止
        this.speechRecognition.continuous = false;
        // 处理语音识别结果事件
        this.speechRecognition.onresult = (e) => {
            const result = e.results[e.results.length - 1][0].transcript.trim();
            console.log('语音识别：', result);
            // 识别完成后立即停止识别服务
            this.speechRecognition.stop();
            // 执行回调函数返回识别结果
            typeof callback === 'function' && callback(result);
        }
        // 语音识别结束时关闭加载动画
        this.speechRecognition.onend = () => {
            console.log('结束语音识别...');
            typeof end === 'function' && end();
            this.loader.close();
        }
        // 处理语音识别错误，关闭加载动画
        this.speechRecognition.onerror = (e) => {
            console.error('语音识别错误：', e.error);
            typeof end === 'function' && end();
            this.loader.close();
        }
        // 启动语音识别服务并显示加载动画，捕获可能的异常
        try {
            this.speechRecognition.start();
            this.loader.show();
        } catch (e) {
            console.error('启动语音识别失败：', e);
            typeof end === 'function' && end();
        }
    }

    /**
     * 恢复语音识别服务
     * 在识别中断或出错后自动重启识别服务，确保监听持续进行
     * @returns {void}
     */
    resume() {
        // 停止当前识别服务
        this.speechRecognition.stop();
        // 正在说话时不重启识别，避免干扰语音合成
        if (this.speaking) return;
        // 延迟 100ms 后重新启动识别，避免频繁启停
        window.setTimeout(() => {
            try {
                this.speechRecognition.start();
            } catch (e) {
                console.error('重启语音识别失败：', e);
            }
        }, 100);
    }

    /**
     * 执行语音合成播放
     * 将文本累加到缓冲区并按标点符号分句播放，实现自然停顿效果
     * @param {boolean} done - 是否表示文本输入结束，为 true 时立即播放缓冲区所有内容
     * @param {string} text - 要播放的文本内容，为空时仅处理缓冲区现有内容
     * @param {Object} [callback] - 回调配置对象
     * @param {function} [callback.play] - 开始播放时的回调函数
     * @param {function} [callback.end] - 播放结束时的回调函数
     * @returns {void}
     */
    speak(done, text, callback) {
        // 初始化默认配置
        callback = callback || {};
        this.listening = false;
        // 累加文本到缓冲区
        this.buffer += text ? text : '';
        // 文本结束时，立即播放缓冲区所有内容
        if (done) {
            this.queue.push(this.buffer);
            if (!this.speaking) this.play(callback);
            this.buffer = '';
            return;
        }
        // 按标点符号分句播放，实现自然停顿
        let index;
        while ((index = this.buffer.search(this.reg)) !== -1) {
            this.queue.push(this.buffer.substring(0, index + 1));
            if (!this.speaking) this.play(callback);
            this.buffer = this.buffer.substring(index + 1);
        }
    }

    /**
     * 播放语音队列中的文本
     * 从队列中取出文本进行语音合成播放，递归处理队列中的所有文本
     * @param {Object} [callback] - 回调配置对象
     * @param {function} [callback.play] - 开始播放时的回调函数
     * @param {function} [callback.end] - 播放结束时的回调函数
     * @returns {void}
     */
    play(callback) {
        // 队列为空时，重置状态并执行结束回调
        if (this.queue.length === 0) {
            this.speaking = false;
            if (this.speechRecognition.continuous) {
                this.resume();
            }
            typeof callback.end === 'function' && callback.end();
            return;
        }
        // 设置说话状态并停止语音识别，避免干扰
        this.speaking = true;
        this.listening = false;
        this.speechRecognition.stop();
        // 创建语音合成实例并配置参数
        const text = this.queue.shift();
        const utterance = new SpeechSynthesisUtterance();
        utterance.text = text;
        utterance.lang = this.speechRecognition.lang;
        if (this.voice) {
            utterance.voice = this.voice;
        }
        // 绑定语音合成开始事件
        utterance.onstart = () => {
            console.log('~~~开始语音合成~~~');
            console.log('语音合成：', text, this.queue.length);
            typeof callback.play === 'function' && callback.play();
        }
        // 绑定语音合成结束事件，递归播放下一句
        utterance.onend = () => {
            console.log('~~~结束语音合成~~~');
            this.play(callback);
        }
        // 绑定语音合成错误事件，继续播放下一句
        utterance.onerror = (e) => {
            console.error('语音合成错误：', e.error);
            this.play(callback);
        }
        window.speechSynthesis.speak(utterance);
    }

    /**
     * 取消当前语音播放
     * 清空缓冲区、播放队列和监听状态，停止所有正在进行的语音合成
     * @returns {void}
     */
    cancel() {
        this.buffer = '';// 清空文本缓冲区
        this.queue = [];// 清空播放队列
        this.listening = false;// 重置监听状态
        // 停止浏览器语音合成服务
        window.speechSynthesis.cancel();
    }

    getVoices(callback) {
        this.voices = [];
        window.speechSynthesis.onvoiceschanged = () => {
            window.speechSynthesis.getVoices().forEach(voice => {
                if (voice.localService && voice.lang === this.speechRecognition.lang) {
                    if (!this.voices.some(value => value.name === voice.name)) {
                        this.voices.push(voice);
                    }
                }
            });
            typeof callback === 'function'
            && callback(this.voices.map(voice => ({title: voice.name, value: voice.name})));
        }
    }
}

class WebSpeechLoader {
    constructor() {
        this.id = `loading-${Date.now()}`;
        if (document.getElementById(`${this.id}-style`)) return;
        this.style = document.createElement("style");
        this.style.id = `${this.id}-style`;
        this.style.textContent = `
        @keyframes wave {from {transform: scaleY(1);}to {transform: scaleY(3);}}`;
        document.head.appendChild(this.style);
    }

    show(timeout, callback) {
        if (document.getElementById(this.id)) return;
        this.loader = document.createElement('div');
        this.loader.id = this.id;
        this.loader.style.cssText = `
        display: flex;position: fixed;
        justify-content: center;align-items: center;
        top: 0;left: 0;width: 100vw;height: 100vh;
        background-color: rgba(0, 0, 0, 0.3);z-index: 999999999;gap: 5px;`;
        for (let i = 1; i < 10; i++) {
            const bar = document.createElement('div');
            bar.style.cssText = `
            width: 5px;height: 10px;
            background-color: #16b777;border-radius: 2px;
            animation: wave 0.8s ease-in-out infinite alternate;
            animation-delay: -0.${i === 5 ? 0 : i}s;`;
            this.loader.appendChild(bar);
        }
        window.document.body.appendChild(this.loader);
        if (timeout && timeout > 0) {
            this.timer = window.setTimeout(() => {
                this.close();
                typeof callback === 'function' && callback();
            }, timeout);
        }
    }

    close() {
        window.clearTimeout(this.timer);
        if (this.loader && this.loader.parentNode) {
            this.loader.parentNode.removeChild(this.loader);
            this.loader = null;
        }
    }
}