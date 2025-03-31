# 数字人（[演示](https://121.28.146.202:15003/index.html)）

![演示](h5.gif)

## 设置

> 项目中使用的[语音识别](https://ai.baidu.com/tech/speech)、[语音合成](https://ai.baidu.com/tech/speech/tts)是[百度AI开放平台](https://ai.baidu.com/)的产品，需要申请对应的[AK/SK](https://ai.baidu.com/ai-doc/REFERENCE/Ck3dwjhhu)。  
```
human:
  baidu:
    api-key: ************************  
    secret-key: ********************************
```
> 项目中包含一个通用的对话接口,您可以使用所有厂家符合OpenAI接口规范的产品，如：[DeepSeek](https://api-docs.deepseek.com/zh-cn/)等。
```
human:
  chat:
    api-key: ********************************
    base-url: https://api.deepseek.com/v1
    models:
      - title: DeepSeek-V3
        value: deepseek-chat
      - title: DeepSeek-R1
        value: deepseek-reasoner
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

[短语音识别标准版](https://ai.baidu.com/ai-doc/SPEECH/Jlbxdezuf)  
[语音识别极速版](https://ai.baidu.com/ai-doc/SPEECH/4lbxdz34z)  
[实时语音识别](https://ai.baidu.com/ai-doc/SPEECH/jlbxejt2i)  

## 语音合成
 
[短文本在线合成](https://ai.baidu.com/ai-doc/SPEECH/mlbxh7xie)  
[长文本在线合成](https://ai.baidu.com/ai-doc/SPEECH/ulbxh8rbu)  
[流式文本在线合成](https://ai.baidu.com/ai-doc/SPEECH/lm5xd63rn)  

## 对话Chat

[千帆大模型](https://cloud.baidu.com/doc/WENXINWORKSHOP/s/Fm2vrveyu)  
[DeepSeek](https://api-docs.deepseek.com/zh-cn/guides/multi_round_chat)  
