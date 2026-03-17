layui.define(function (exports) {
    exports('assert', {
        limit: function () {
            if (layui.url().search.assert === 'no-limit') return;
            const key = layui.util.toDateString(Date.now(), 'yyyyMMdd');
            const limit = layui.data('assert-limit').limit || {key: key, value: 0};
            layui.data('assert-limit', {
                key: 'limit', value: (limit.key !== key)
                    ? {key: key, value: 1} : {key: limit.key, value: limit.value + 1}
            });
            if (layui.data('assert-limit').limit.value > 10) {
                layui.layer.msg('您今天使用的次数太多了，让我休息一会吧！');
                throw Error('您今天使用的次数太多了，让我休息一会吧！');
            }
        }
    });
});