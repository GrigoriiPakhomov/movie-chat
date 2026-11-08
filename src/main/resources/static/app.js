
const loginScreen = document.getElementById("loginScreen");
const chatLayout = document.getElementById("chatLayout");

const loginForm = document.getElementById("loginForm");
const usernameInput = document.getElementById("usernameInput");
const loginError = document.getElementById("loginError");

const messageForm = document.getElementById("messageForm");
const messageInput = document.getElementById("messageInput");

const messages = document.getElementById("messages");
const emptyState = document.getElementById("emptyState");

const usersList = document.getElementById("usersList");
const onlineCount = document.getElementById("onlineCount");

const currentUsername = document.getElementById("currentUsername");
const currentAvatar = document.getElementById("currentAvatar");

const logoutButton = document.getElementById("logoutButton");

let socket = null;
let username = null;

loginForm.addEventListener("submit", (event) => {
    event.preventDefault();

    const value = usernameInput.value.trim();

    if (!value) {
        loginError.textContent = "Введи имя пользователя";
        return;
    }

    connect(value);
});


function connect(name) {
    username = name;

    const protocol = location.protocol === "https:" ? "wss:" : "ws:";
    const wsUrl = `${protocol}//${location.host}/ws/chat?username=${encodeURIComponent(username)}`;

    console.log("[WS] Подключение:", wsUrl);

    socket = new WebSocket(wsUrl);

    socket.addEventListener("open", () => {
        console.log("[WS] Соединение открыто");

        loginScreen.classList.add("hidden");
        chatLayout.classList.remove("hidden");

        currentUsername.textContent = username;
        currentAvatar.textContent = username.charAt(0).toUpperCase();

        loginError.textContent = "";
        messageInput.focus();
    });

    socket.addEventListener("message", (event) => {
        console.log("[WS] Получено сообщение:", event.data);

        try {
            handleMessage(JSON.parse(event.data));
        } catch (error) {
            console.error("[WS] Ошибка обработки:", error);
        }
    });

    socket.addEventListener("close", (event) => {
        console.error("[WS] Соединение закрыто:", {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean
        });

        chatLayout.classList.add("hidden");
        loginScreen.classList.remove("hidden");

        loginError.textContent =
            `Соединение закрыто. Код: ${event.code}`;

        socket = null;
    });

    socket.addEventListener("error", (event) => {
        console.error("[WS] Ошибка:", event);
    });
}


function handleMessage(message) {
    switch (message.type) {
        case "CHAT":
            renderChatMessage(message);
            break;

        case "USER_JOINED":
            renderSystemMessage(`${message.username} присоединился к чату`);
            break;

        case "USER_LEFT":
            renderSystemMessage(`${message.username} покинул чат`);
            break;

        case "USER_LIST":
            renderUsers(message.users || []);
            break;

        case "MOVIE":
            renderMovieCard(message);
            break;

        default:
            console.warn("Неизвестный тип сообщения:", message.type);
    }
}

function renderChatMessage(message) {
    emptyState.classList.add("hidden");

    const element = document.createElement("div");
    element.className = "message";

    if (message.username === username) {
        element.classList.add("own");
    }

    const meta = document.createElement("div");
    meta.className = "message-meta";

    const sender = document.createElement("strong");
    sender.textContent = message.username || "Гость";

    const time = document.createElement("span");
    time.textContent = formatTime(message.timestamp);

    const content = document.createElement("div");
    content.className = "message-content";
    content.textContent = message.content || "";

    meta.append(sender, time);
    element.append(meta, content);

    messages.appendChild(element);
    scrollToBottom();
}


function renderMovieCard(message) {
    emptyState.classList.add("hidden");

    const movie = message.movie;

    if (!movie) {
        return;
    }

    const card = document.createElement("div");
    card.className = "movie-card";

    const poster = document.createElement("img");
    poster.className = "movie-poster";
    poster.alt = movie.title || "Постер фильма";

    if (movie.poster && movie.poster !== "—") {
        poster.src = movie.poster;
    } else {
        poster.classList.add("hidden");
    }

    const info = document.createElement("div");
    info.className = "movie-info";

    const title = document.createElement("h3");
    title.textContent = movie.title || "Без названия";

    const year = document.createElement("p");
    year.textContent = `Год: ${movie.year || "—"}`;

    const genre = document.createElement("p");
    genre.textContent = `Жанр: ${movie.genre || "—"}`;

    const rating = document.createElement("p");
    rating.textContent = `⭐ IMDb: ${movie.rating || "—"}`;

    const plot = document.createElement("p");
    plot.className = "movie-plot";
    plot.textContent = movie.plot || "Описание отсутствует";

    const author = document.createElement("small");
    author.textContent = `Добавил: ${message.username || "Система"}`;

    info.append(title, year, genre, rating, plot, author);
    card.append(poster, info);

    messages.appendChild(card);
    scrollToBottom();
}

function renderSystemMessage(text) {
    emptyState.classList.add("hidden");

    const element = document.createElement("div");
    element.className = "system-message";
    element.textContent = text;

    messages.appendChild(element);
    scrollToBottom();
}

function renderUsers(users) {
    usersList.replaceChildren();

    users.forEach((user) => {
        const item = document.createElement("li");

        const indicator = document.createElement("span");
        indicator.className = "user-online";

        const name = document.createElement("span");
        name.textContent = user;

        item.append(indicator, name);
        usersList.appendChild(item);
    });

    onlineCount.textContent = users.length;
}

function formatTime(timestamp) {
    if (!timestamp) {
        return "";
    }

    const date = new Date(timestamp);

    return date.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit"
    });
}

function scrollToBottom() {
    messages.scrollTop = messages.scrollHeight;
}

messageForm.addEventListener("submit", (event) => {
    event.preventDefault();

    const content = messageInput.value.trim();

    if (!content || !socket || socket.readyState !== WebSocket.OPEN) {
        return;
    }

    socket.send(content);
    messageInput.value = "";
    messageInput.focus();
});

logoutButton.addEventListener("click", () => {
    if (socket) {
        socket.close();
    }
});
