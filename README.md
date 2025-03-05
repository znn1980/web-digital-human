# 数字人（[演示](https://121.28.146.202:15003/index.html)）

![演示](h5.gif)

## 设置

> 项目中使用的语音识别、语音合成的是百度AI开放平台的产品，需要申请对应的[AK/SK](https://ai.baidu.com/ai-doc/REFERENCE/Ck3dwjhhu)。  
```
human:
  baidu:
    api-key: ************************
    secret-key: ********************************
```
> 项目中使用的智能对话是千帆大模型的产品，需要申请对应的[API Key](https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Um2wxbaps)。
```
human:
  baidu:
    qianfan:
      api-key: ***************************************************************************
      access-key: ********************************
      secret-key: ********************************
```
> 项目中包含一个通用的智能对话接口,您可以使用所有厂家符合OpenAI标准的产品，如：DeepSeek等。
```
human:
  openai:
    base-url: https://api.deepseek.com
    api-key: ********************************
```
> 在网页使用麦克风录音时需使用HTTPS安全协议。  
> 项目自带一个测试用的证书，您在使用中可以替换成您的证书或关闭server.ssl.enabled=false使用Nginx代理。
```
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.jks
    key-store-password: 123456
```

## 语音识别

[短语音识别极速版](https://ai.baidu.com/tech/speech/asrpro)  
[语音识别极速版API](https://ai.baidu.com/ai-doc/SPEECH/jkhq0ohzz)


## 语音合成

[短文本在线合成](https://ai.baidu.com/tech/speech/tts_online)  
[短文本在线合成API](https://ai.baidu.com/ai-doc/SPEECH/mlbxh7xie)  
[流式文本在线合成](https://ai.baidu.com/ai-doc/SPEECH/lm5xd63rn)

## 千帆大模型

[对话Chat](https://cloud.baidu.com/doc/WENXINWORKSHOP/s/om5aq2brc)  
[对话Chat V2](https://cloud.baidu.com/doc/WENXINWORKSHOP/s/em4tsqo3v)