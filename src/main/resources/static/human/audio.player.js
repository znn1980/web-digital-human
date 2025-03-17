const AudioPlayer = function (sampleRate, onPlay, onDone) {
    this.sampleRate = sampleRate;
    this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    this.audioQueue = [];
    this.isPlaying = false;
    this.isDoneing = true;
    this.bufferSource = null;
    this.onPlay = onPlay;
    this.onDone = onDone;
    this.onPause = function () {
        if (this.bufferSource) {
            this.bufferSource.stop();
        }
        this.audioQueue = [];
        this.isPlaying = false;
        this.isDoneing = true;
    };
    this.onPush = function (buffer) {
        this.audioQueue.push(buffer);
        this.onPlayNextAudio();
    };
    //将arrayBuffer转为audioBuffer
    this.onBufferPCMData = function (buffer) {
        const length = buffer.byteLength / 2; // 假设 PCM 数据为 16 位，需除以 2
        const audioBuffer = this.audioContext.createBuffer(1, length, this.sampleRate);
        const channelData = audioBuffer.getChannelData(0);
        const bufferArray = new Int16Array(buffer);// 将 PCM 数据转换为 Int16Array
        for (let i = 0; i < length; i++) {
            // 将 16 位 PCM 转换为浮点数 (-1.0 到 1.0)
            channelData[i] = bufferArray[i] / 32768;// 16 位数据转换范围
        }
        return audioBuffer;
    };

    this.onPlayAudio = function (buffer) {
        if (this.audioContext.state === 'suspended') {
            this.audioContext.resume();
        }
        this.bufferSource = this.audioContext.createBufferSource();
        this.bufferSource.buffer = buffer;
        this.bufferSource.connect(this.audioContext.destination);
        this.bufferSource.onended = () => {
            this.isPlaying = false;
            this.bufferSource = null;
            this.onPlayNextAudio();
        };
        this.bufferSource.start();
        if (this.isDoneing) {
            this.isDoneing = false;
            this.onPlay();
        }
        this.isPlaying = true;
    };
    // Play the next audio in the queue
    this.onPlayNextAudio = function () {
        if (this.audioQueue.length > 0 && !this.isPlaying) {
            // 计算总的字节长度
            const totalLength = this.audioQueue.reduce((acc, buffer) => acc + buffer.byteLength, 0);
            const bufferArray = new Uint8Array(totalLength);
            let offset = 0;
            // 将所有 audioQueue 中的 buffer 拼接到一个新的 Uint8Array 中
            for (const buffer of this.audioQueue) {
                bufferArray.set(new Uint8Array(buffer), offset);
                offset += buffer.byteLength;
            }
            // 清空 audioQueue，因为我们已经拼接完所有数据
            this.audioQueue = [];
            // 发送拼接的 audio 数据给 PlayAudio
            this.onPlayAudio(this.onBufferPCMData(bufferArray.buffer));
        }
        if (this.audioQueue.length === 0 && !this.isPlaying) {
            this.isDoneing = true;
            this.onDone();
        }
    };
};