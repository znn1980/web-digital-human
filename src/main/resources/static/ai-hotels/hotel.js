const allMap = new BMapGL.Map("all-map");

function addHotels(hotels) {
    const points = [];
    allMap.clearOverlays();
    hotels.forEach(function (data) {
        let sum = 0;
        data.rooms.forEach(function (room) {
            sum += room.price = asPrice(100.00, 500.00);
            room.price = `￥${room.price.toFixed(2)}`;
        });
        data.price = `￥${(sum / data.rooms.length).toFixed(2)}`;
        points.push(addMarker(data).getPosition());
    });
    allMap.setViewport(points);
}

function addMarker(data) {
    const marker = new BMapGL.Marker(new BMapGL.Point(data.point.lng, data.point.lat), {title: data.address});
    marker.setIcon(new BMapGL.Icon("hotel.png", new BMapGL.Size(32, 32)));
    marker.setLabel(new BMapGL.Label(`${data.name}（参考价: ${data.price}）`, {offset: new BMapGL.Size(20, -10)}));
    marker.addEventListener("mouseover", function () {
        marker.setTop(true);
    });
    marker.addEventListener("mouseout", function () {
        marker.setTop(false);
    });
    marker.addEventListener("click", function () {
        const msg = [];
        msg.push(`类型：${data.type}<br>地址：${data.address}`);
        msg.push(`<table class="layui-table" lay-size="sm">`);
        msg.push(`<thead><tr><th>房型</th><th>价格</th></tr></thead><tbody>`);
        data.rooms.forEach(function (room) {
            msg.push(`<tr><td>${room.name}</td><td>${room.price}</td></tr>`);
        });
        msg.push(`</tbody></table>`);
        allMap.openInfoWindow(new BMapGL.InfoWindow(msg.join(''), {title: `${data.name}（参考价: ${data.price}）`})
            , new BMapGL.Point(data.point.lng, data.point.lat));
    });
    allMap.addOverlay(marker);
    return marker;
}

function asPrice(min, max) {
    return Math.random() * (max - min) + min;
}

function asUuid() {
    if (crypto.randomUUID) return crypto.randomUUID();
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
}