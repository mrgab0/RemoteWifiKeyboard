#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod keyboard;

use axum::{
    extract::ws::{Message, WebSocket, WebSocketUpgrade},
    extract::State,
    response::{Html, IntoResponse, Json},
    routing::{get, post},
    Router,
};
use futures_util::{sink::SinkExt, stream::StreamExt};
use local_ip_address::local_ip;
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::broadcast;
use tower_http::cors::{Any, CorsLayer};

use tao::{
    dpi::LogicalSize,
    event::{Event, WindowEvent},
    event_loop::{ControlFlow, EventLoopBuilder},
    window::WindowBuilder,
};
use tray_icon::{
    menu::{Menu, MenuEvent, MenuItem, PredefinedMenuItem},
    Icon, MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent,
};
use wry::WebViewBuilder;

const HTML_CONTENT: &str = include_str!("web/index.html");

#[derive(Clone)]
struct AppState {
    tx: broadcast::Sender<String>,
}

#[derive(Deserialize, Serialize, Debug)]
struct TypePayload {
    text: String,
}

#[derive(Deserialize, Serialize, Debug)]
struct KeyPayload {
    key: String,
}

#[derive(Serialize)]
struct PingResponse {
    status: String,
    engine: String,
    version: String,
}

fn create_tray_icon() -> Icon {
    // Generar un icono 32x32 con diseño de teclado moderno en memoria
    let width = 32;
    let height = 32;
    let mut rgba = Vec::with_capacity((width * height * 4) as usize);

    for y in 0..height {
        for x in 0..width {
            // Marco redondeado con degradado índigo (#6366f1)
            let is_border = x == 2 || x == 29 || y == 4 || y == 27;
            let is_inside = x > 2 && x < 29 && y > 4 && y < 27;
            
            // Dibujar teclas simuladas
            let is_key1 = (x >= 6 && x <= 11) && (y >= 8 && y <= 13);
            let is_key2 = (x >= 14 && x <= 19) && (y >= 8 && y <= 13);
            let is_key3 = (x >= 22 && x <= 27) && (y >= 8 && y <= 13);
            let is_space = (x >= 8 && x <= 24) && (y >= 18 && y <= 23);

            if is_key1 || is_key2 || is_key3 || is_space {
                rgba.extend_from_slice(&[255, 255, 255, 255]); // Blanco brillante
            } else if is_inside {
                rgba.extend_from_slice(&[99, 102, 241, 230]); // Púrpura/Indigo
            } else if is_border {
                rgba.extend_from_slice(&[139, 92, 246, 255]); // Violeta
            } else {
                rgba.extend_from_slice(&[0, 0, 0, 0]); // Transparente
            }
        }
    }

    Icon::from_rgba(rgba, width, height).expect("Error creando icono de la bandeja")
}

fn start_background_server(tx: broadcast::Sender<String>) {
    std::thread::spawn(move || {
        let rt = tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("No se pudo iniciar el runtime de Tokio");

        rt.block_on(async move {
            let app_state = Arc::new(AppState { tx });

            let cors = CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any);

            let app = Router::new()
                .route("/", get(serve_index))
                .route("/api/ping", get(handle_ping))
                .route("/api/type", post(handle_type))
                .route("/api/key", post(handle_key))
                .route("/ws", get(handle_ws))
                .layer(cors)
                .with_state(app_state);

            let port = 8080;
            let addr = SocketAddr::from(([0, 0, 0, 0], port));

            if let Ok(listener) = tokio::net::TcpListener::bind(addr).await {
                let _ = axum::serve(listener, app).await;
            }
        });
    });
}

fn main() {
    let (tx, _rx) = broadcast::channel(100);

    // 1. Iniciar el servidor web/websocket de ultra-baja latencia en segundo plano
    start_background_server(tx.clone());

    // 2. Iniciar el bucle de eventos nativo de Windows (Tao + Wry + Tray)
    let event_loop = EventLoopBuilder::new().build();

    // Crear ventana gráfica de configuración
    let window = WindowBuilder::new()
        .with_title("Remote WiFi Keyboard • Windows Control Center")
        .with_inner_size(LogicalSize::new(760.0, 680.0))
        .with_min_inner_size(LogicalSize::new(500.0, 520.0))
        .with_visible(true)
        .build(&event_loop)
        .expect("No se pudo crear la ventana de Windows");

    let _webview = WebViewBuilder::new()
        .with_html(HTML_CONTENT)
        .build(&window)
        .expect("No se pudo inicializar WebView2");

    // 3. Crear el icono y menú de la bandeja del sistema (System Tray)
    let tray_menu = Menu::new();
    let show_item = MenuItem::new("📱 Abrir Panel de Control", true, None);
    let separator = PredefinedMenuItem::separator();
    let quit_item = MenuItem::new("❌ Salir", true, None);

    let _ = tray_menu.append(&show_item);
    let _ = tray_menu.append(&separator);
    let _ = tray_menu.append(&quit_item);

    let _tray_icon = TrayIconBuilder::new()
        .with_menu(Box::new(tray_menu))
        .with_tooltip("Remote WiFi Keyboard - Activo")
        .with_icon(create_tray_icon())
        .build()
        .expect("No se pudo crear el icono en la bandeja");

    let show_id = show_item.id().clone();
    let quit_id = quit_item.id().clone();

    let menu_channel = MenuEvent::receiver();
    let tray_channel = TrayIconEvent::receiver();

    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Wait;

        // Escuchar clics en el menú contextual del System Tray
        if let Ok(menu_event) = menu_channel.try_recv() {
            if menu_event.id == show_id {
                window.set_visible(true);
                window.set_focus();
            } else if menu_event.id == quit_id {
                *control_flow = ControlFlow::Exit;
            }
        }

        // Escuchar clics e interacción en el icono de la bandeja
        if let Ok(tray_event) = tray_channel.try_recv() {
            match tray_event {
                // Solo alternar con clic izquierdo al soltar el botón (evita interferir con el menú contextual derecho)
                TrayIconEvent::Click {
                    button: MouseButton::Left,
                    button_state: MouseButtonState::Up,
                    ..
                } => {
                    let is_vis = window.is_visible();
                    window.set_visible(!is_vis);
                    if !is_vis {
                        window.set_focus();
                    }
                }
                // Doble clic izquierdo también muestra y enfoca la ventana
                TrayIconEvent::DoubleClick {
                    button: MouseButton::Left,
                    ..
                } => {
                    window.set_visible(true);
                    window.set_focus();
                }
                _ => {}
            }
        }

        // Manejar eventos de la ventana
        if let Event::WindowEvent { event, .. } = event {
            match event {
                // Al presionar la 'X' de cerrar, solo ocultamos a la bandeja
                WindowEvent::CloseRequested => {
                    window.set_visible(false);
                }
                _ => {}
            }
        }
    });
}

// --- Endpoints del Servidor Axum ---

async fn serve_index() -> Html<&'static str> {
    Html(HTML_CONTENT)
}

async fn handle_ping() -> Json<PingResponse> {
    Json(PingResponse {
        status: "ok".to_string(),
        engine: "Rust Win32 Native".to_string(),
        version: "1.0.0".to_string(),
    })
}

async fn handle_type(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<TypePayload>,
) -> Json<serde_json::Value> {
    // ⚡ Inyección nativa directa de texto en Windows
    keyboard::type_text(&payload.text);

    let msg = serde_json::json!({
        "type": "type",
        "text": payload.text
    })
    .to_string();
    let _ = state.tx.send(msg);

    Json(serde_json::json!({ "status": "ok" }))
}

async fn handle_key(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<KeyPayload>,
) -> Json<serde_json::Value> {
    // ⚡ Inyección nativa directa de teclas/atajos en Windows
    keyboard::handle_key_command(&payload.key);

    let msg = serde_json::json!({
        "type": "key",
        "key": payload.key
    })
    .to_string();
    let _ = state.tx.send(msg);

    Json(serde_json::json!({ "status": "ok" }))
}

async fn handle_ws(
    ws: WebSocketUpgrade,
    State(state): State<Arc<AppState>>,
) -> impl IntoResponse {
    ws.on_upgrade(|socket| websocket_handler(socket, state))
}

async fn websocket_handler(socket: WebSocket, state: Arc<AppState>) {
    let (mut sender, mut receiver) = socket.split();
    let mut rx = state.tx.subscribe();

    let ip_str = local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());

    let welcome = serde_json::json!({
        "type": "welcome",
        "ip": ip_str,
        "engine": "Rust Win32 Server",
        "message": "Conectado al servidor de ultra-baja latencia"
    })
    .to_string();
    let _ = sender.send(Message::Text(welcome)).await;

    let mut send_task = tokio::spawn(async move {
        while let Ok(msg) = rx.recv().await {
            if sender.send(Message::Text(msg)).await.is_err() {
                break;
            }
        }
    });

    let tx_clone = state.tx.clone();
    let mut recv_task = tokio::spawn(async move {
        while let Some(Ok(msg)) = receiver.next().await {
            if let Message::Text(text) = msg {
                if let Ok(val) = serde_json::from_str::<serde_json::Value>(&text) {
                    if let Some(t) = val.get("type").and_then(|v| v.as_str()) {
                        match t {
                            "type" => {
                                if let Some(text_content) = val.get("text").and_then(|v| v.as_str()) {
                                    keyboard::type_text(text_content);
                                }
                            }
                            "key" => {
                                if let Some(key_content) = val.get("key").and_then(|v| v.as_str()) {
                                    keyboard::handle_key_command(key_content);
                                }
                            }
                            _ => {}
                        }
                    }
                }
                let _ = tx_clone.send(text);
            }
        }
    });

    tokio::select! {
        _ = (&mut send_task) => recv_task.abort(),
        _ = (&mut recv_task) => send_task.abort(),
    };
}
