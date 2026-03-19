/**
 * main.js（前端交互主脚本）
 * 作用：处理页面初始化、地理定位、与后端接口通信（搜索/历史/推荐），
 * 并把返回的数据渲染为列表项；同时提供收藏的增删操作。
 */
(() => {
    // —— 全局基础状态：用户信息、默认经纬度，用于接口请求与展示 ——
    // =========================
    // 基本状态
    // =========================
    const USER_ID = "1111";
    const USER_FULLNAME = "John";
    let lat = 37.38;
    let lng = -122.08;

    // —— DOM 工具函数 ——
    // $：按 id 获取元素；el：创建带属性与子节点的元素，简化节点构建与渲染
    // 简化 DOM 操作
    const $ = (id) => document.getElementById(id);
    const el = (tag, props = {}, children = []) => {
        const node = document.createElement(tag);
        Object.entries(props).forEach(([k, v]) => {
            if (k === "className") node.className = v;
            else if (k === "dataset") Object.assign(node.dataset, v);
            else if (k in node) node[k] = v;
            else node.setAttribute(k, v);
        });
        (Array.isArray(children) ? children : [children]).forEach((c) => {
            if (c == null) return;
            if (typeof c === "string") node.appendChild(document.createTextNode(c));
            else node.appendChild(c);
        });
        return node;
    };

    // —— 初始化入口 ——
    // 注册顶部导航按钮事件、显示欢迎语、并发起地理定位流程
    // =========================
    // 初始化
    // =========================
    function init() {
        // 顶部按钮事件
        $("nearby-btn")?.addEventListener("click", loadNearbyItems);
        $("fav-btn")?.addEventListener("click", loadFavoriteItems);
        $("recommend-btn")?.addEventListener("click", loadRecommendedItems);

        // 欢迎语
        const welcome = $("welcome-msg");
        if (welcome) {
            welcome.classList.remove("visually-hidden");
            welcome.textContent = `Welcome, ${USER_FULLNAME}`;
        }

        // 获取地理位置
        initGeoLocation();
    }

    // —— 地理定位：优先使用浏览器 Geolocation，失败则走 IP 定位兜底 ——
    function initGeoLocation() {
        if (navigator.geolocation) {
            showLoadingMessage("Retrieving your location...");
            navigator.geolocation.getCurrentPosition(
                onPositionUpdated,
                onLoadPositionFailed,
                { enableHighAccuracy: true, maximumAge: 60000, timeout: 8000 }
            );
        } else {
            onLoadPositionFailed();
        }
    }

    // —— 成功拿到定位回调：更新经纬度并拉取“附近”数据 ——
    function onPositionUpdated(position) {
        lat = position.coords.latitude;
        lng = position.coords.longitude;
        loadNearbyItems();
    }

    // —— 定位失败回调：输出警告并改用 IP 定位 ——
    function onLoadPositionFailed() {
        console.warn("navigator.geolocation is not available or denied");
        getLocationFromIP();
    }

    // —— 基于 IP 的定位兜底（HTTPS，避免混合内容），定位完成后继续拉取附近数据 ——
    function getLocationFromIP() {
        showLoadingMessage("Retrieving location by IP...");
        fetch("https://ipinfo.io/json?token=") // 如有 token 可填上；没有也能返回城市级近似
            .then((r) => r.ok ? r.json() : Promise.reject())
            .then((data) => {
                if (data && typeof data.loc === "string") {
                    const [la, lo] = data.loc.split(",").map(parseFloat);
                    if (!Number.isNaN(la) && !Number.isNaN(lo)) {
                        lat = la;
                        lng = lo;
                    }
                } else {
                    console.warn("IP geolocation failed.");
                }
            })
            .catch(() => {
                console.warn("IP geolocation request error.");
            })
            .finally(loadNearbyItems);
    }

    // —— UI：激活左侧功能按钮（高亮当前选项） ——
    function activeBtn(btnId) {
        document.querySelectorAll(".main-nav-btn").forEach((b) => {
            b.classList.remove("active");
        });
        const btn = $(btnId);
        if (btn) btn.classList.add("active");
    }

    // —— UI：写入列表容器的 HTML 片段 ——
    function setListHTML(html) {
        const list = $("item-list");
        if (list) list.innerHTML = html;
    }

    // —— UI：显示“加载中”提示（带图标） ——
    function showLoadingMessage(msg) {
        setListHTML(
            `<p class="notice"><i class="fa-solid fa-spinner fa-spin"></i> ${msg}</p>`
        );
    }
    // —— UI：显示“空结果/提醒”提示（带图标） ——
    function showWarningMessage(msg) {
        setListHTML(
            `<p class="notice"><i class="fa-solid fa-triangle-exclamation"></i> ${msg}</p>`
        );
    }
    // —— UI：显示“错误”提示（带图标） ——
    function showErrorMessage(msg) {
        setListHTML(
            `<p class="notice"><i class="fa-solid fa-circle-exclamation"></i> ${msg}</p>`
        );
    }

    // —— 网络封装：GET JSON（基于 fetch），非 2xx 认为失败 ——
    async function httpGetJSON(url) {
        const res = await fetch(url, { method: "GET", credentials: "same-origin" });
        if (!res.ok) throw new Error(res.statusText);
        return res.json();
    }

    // —— 网络封装：发送 JSON（POST/DELETE 等），带同源凭证 ——
    async function httpSendJSON(method, url, bodyObj) {
        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json;charset=utf-8" },
            body: JSON.stringify(bodyObj),
            credentials: "same-origin",
        });
        if (!res.ok) throw new Error(res.statusText);
        return res.json();
    }

    // —— API#1：加载“附近”数据 ——
    // 请求：./search?user_id=xxx&amp;lat=...&amp;lon=...；成功则渲染列表，否则提示
    async function loadNearbyItems() {
        activeBtn("nearby-btn");
        showLoadingMessage("Loading nearby items...");
        try {
            const url = `./search?user_id=${encodeURIComponent(
                USER_ID
            )}&lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lng)}`;
            const items = await httpGetJSON(url);
            if (!items || items.length === 0) {
                showWarningMessage("No nearby item.");
            } else {
                listItems(items);
            }
        } catch (e) {
            console.error(e);
            showErrorMessage("Cannot load nearby items.");
        }
    }

    // —— API#2：加载“我的收藏/历史” ——
    // 请求：./history?user_id=xxx；成功则渲染列表，否则提示
    async function loadFavoriteItems() {
        activeBtn("fav-btn");
        showLoadingMessage("Loading favorite items...");
        try {
            const url = `./history?user_id=${encodeURIComponent(USER_ID)}`;
            const items = await httpGetJSON(url);
            if (!items || items.length === 0) {
                showWarningMessage("No favorite item.");
            } else {
                listItems(items);
            }
        } catch (e) {
            console.error(e);
            showErrorMessage("Cannot load favorite items.");
        }
    }

    // —— API#3：加载“推荐”数据 ——
    // 请求：./recommendation?user_id=xxx&amp;lat=...&amp;lon=...；需已有收藏作为偏好
    async function loadRecommendedItems() {
        activeBtn("recommend-btn");
        showLoadingMessage("Loading recommended items...");
        try {
            const url = `./recommendation?user_id=${encodeURIComponent(
                USER_ID
            )}&lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lng)}`;
            const items = await httpGetJSON(url);
            if (!items || items.length === 0) {
                showWarningMessage("No recommended item. Make sure you have favorites.");
            } else {
                listItems(items);
            }
        } catch (e) {
            console.error(e);
            showErrorMessage("Cannot load recommended items.");
        }
    }

    // —— API#4：切换收藏状态 ——
    // 根据目标状态选择 POST（收藏）或 DELETE（取消），成功后更新 UI 状态
    async function changeFavoriteItem(itemId, nextFavorite) {
        const method = nextFavorite ? "POST" : "DELETE";
        try {
            const res = await httpSendJSON(method, "./history", {
                user_id: USER_ID,
                favorite: [itemId],
            });
            return res && res.result === "SUCCESS";
        } catch (e) {
            console.error(e);
            return false;
        }
    }

    // —— 渲染：清空并批量添加条目到列表容器 ——
    function listItems(items) {
        const list = $("item-list");
        if (!list) return;
        list.innerHTML = "";

        items.forEach((item) => list.appendChild(renderItem(item)));
    }

    // —— 渲染：把单条 item 数据转成一个 &lt;li class="item"&gt; DOM 结构 ——
    // 包含封面图、名称/分类/评分、地址、收藏按钮等
    function renderItem(item) {
        const itemId = item.item_id;

        const li = el("li", {
            id: `item-${itemId}`,
            className: "item",
            dataset: { item_id: itemId, favorite: !!item.favorite },
            role: "article",
        });

        // 封面图
        const imgUrl =
            item.image_url ||
            "https://upload.wikimedia.org/wikipedia/commons/a/ac/No_image_available.svg";
        li.appendChild(
            el("img", {
                src: imgUrl,
                alt: item.name || "item image",
                width: 80,
                height: 80,
                loading: "lazy",
                decoding: "async",
            })
        );

        // 主信息块
        const meta = el("div", { className: "item-meta" });

        const title = el("a", {
            className: "item-name",
            href: item.url || "#",
            target: item.url ? "_blank" : "",
            rel: item.url ? "noopener noreferrer" : "",
            dataset: { id: itemId },
        }, item.name || "Item");

        const cat = el(
            "p",
            { className: "item-category" },
            item.categories && item.categories.length
                ? `Category: ${item.categories.join(", ")}`
                : "Category: N/A"
        );

        const stars = renderStars(item.rating);

        meta.appendChild(title);
        meta.appendChild(cat);
        meta.appendChild(stars);
        li.appendChild(meta);

        // 地址
        const address = el("address", { className: "item-address" });
        address.innerHTML = (item.address || "N/A")
            .replace(/,/g, "<br/>")
            .replace(/\"/g, "");
        li.appendChild(address);

        // 收藏按钮（button + 图标，aria-pressed 反映状态）
        const favWrap = el("div", { className: "fav-link" });
        const isFav = !!item.favorite;
        const favBtn = el("button", {
            type: "button",
            className: "fav-btn",
            "aria-pressed": String(isFav),
            "aria-label": isFav ? "Remove from favorites" : "Add to favorites",
        });
        const favIcon = el("i", {
            className: isFav ? "fa-solid fa-heart" : "fa-regular fa-heart",
        });
        favBtn.appendChild(favIcon);

        favBtn.addEventListener("click", async () => {
            const next = favBtn.getAttribute("aria-pressed") !== "true";
            // 先乐观更新 UI
            applyFavState(favBtn, favIcon, next);
            const ok = await changeFavoriteItem(itemId, next);
            if (!ok) {
                // 回滚
                applyFavState(favBtn, favIcon, !next);
                alert("Update favorite failed. Please try again.");
            } else {
                li.dataset.favorite = String(next);
            }
        });

        favWrap.appendChild(favBtn);
        li.appendChild(favWrap);

        return li;
    }

    // —— UI：应用收藏态（切换图标与 aria-pressed/aria-label） ——
    function applyFavState(btn, icon, fav) {
        btn.setAttribute("aria-pressed", String(fav));
        btn.setAttribute(
            "aria-label",
            fav ? "Remove from favorites" : "Add to favorites"
        );
        icon.className = fav ? "fa-solid fa-heart" : "fa-regular fa-heart";
    }

    // —— UI：星级评分渲染（Font Awesome 6：实星/半星/空星），含可访问性说明 ——
    function renderStars(rating) {
        const wrap = el("div", {
            className: "stars",
            "aria-label": `Rating: ${rating ?? 0} out of 5`,
        });

        const r = Number(rating) || 0;
        const full = Math.floor(r);
        const half = String(r).match(/\.5$/) ? 1 : 0;
        const empty = Math.max(0, 5 - full - half);

        for (let i = 0; i < full; i++) {
            wrap.appendChild(el("i", { className: "fa-solid fa-star" }));
        }
        if (half) {
            wrap.appendChild(el("i", { className: "fa-solid fa-star-half-stroke" }));
        }
        for (let i = 0; i < empty; i++) {
            wrap.appendChild(el("i", { className: "fa-regular fa-star" }));
        }
        return wrap;
    }

    // —— 启动应用 ——
    // 绑定事件 &amp; 地理定位 &amp; 首次加载“附近”列表
    init();
})();