#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

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

fn create_tray_icon() -> Icon {
    let width = 32;
    let height = 32;
    let mut rgba = Vec::with_capacity((width * height * 4) as usize);

    for y in 0..height {
        for x in 0..width {
            let is_border = x == 2 || x == 29 || y == 4 || y == 27;
            let is_inside = x > 2 && x < 29 && y > 4 && y < 27;
            
            let is_key1 = (x >= 6 && x <= 11) && (y >= 8 && y <= 13);
            let is_key2 = (x >= 14 && x <= 19) && (y >= 8 && y <= 13);
            let is_key3 = (x >= 22 && x <= 27) && (y >= 8 && y <= 13);
            let is_space = (x >= 8 && x <= 24) && (y >= 18 && y <= 23);

            if is_key1 || is_key2 || is_key3 || is_space {
                rgba.extend_from_slice(&[255, 255, 255, 255]);
            } else if is_inside {
                rgba.extend_from_slice(&[99, 102, 241, 230]);
            } else if is_border {
                rgba.extend_from_slice(&[139, 92, 246, 255]);
            } else {
                rgba.extend_from_slice(&[0, 0, 0, 0]);
            }
        }
    }

    Icon::from_rgba(rgba, width, height).expect("Error creando icono de la bandeja")
}

fn main() {
    let event_loop = EventLoopBuilder::new().build();

    let window = WindowBuilder::new()
        .with_title("Remote WiFi Keyboard • PC Client")
        .with_inner_size(LogicalSize::new(720.0, 680.0))
        .with_min_inner_size(LogicalSize::new(480.0, 480.0))
        .with_visible(true)
        .build(&event_loop)
        .expect("No se pudo crear la ventana de Windows");

    let _webview = WebViewBuilder::new()
        .with_html(HTML_CONTENT)
        .build(&window)
        .expect("No se pudo inicializar WebView2");

    let tray_menu = Menu::new();
    let show_item = MenuItem::new("📱 Abrir Panel de Control", true, None);
    let on_top_item = MenuItem::new("📌 Fijar Siempre Visible", true, None);
    let separator = PredefinedMenuItem::separator();
    let quit_item = MenuItem::new("❌ Salir", true, None);

    let _ = tray_menu.append(&show_item);
    let _ = tray_menu.append(&on_top_item);
    let _ = tray_menu.append(&separator);
    let _ = tray_menu.append(&quit_item);

    let _tray_icon = TrayIconBuilder::new()
        .with_menu(Box::new(tray_menu))
        .with_tooltip("Remote WiFi Keyboard - PC Client")
        .with_icon(create_tray_icon())
        .build()
        .expect("No se pudo crear el icono en la bandeja");

    let show_id = show_item.id().clone();
    let on_top_id = on_top_item.id().clone();
    let quit_id = quit_item.id().clone();

    let menu_channel = MenuEvent::receiver();
    let tray_channel = TrayIconEvent::receiver();
    let mut is_on_top = false;

    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Wait;

        if let Ok(menu_event) = menu_channel.try_recv() {
            if menu_event.id == show_id {
                window.set_visible(true);
                window.set_focus();
            } else if menu_event.id == on_top_id {
                is_on_top = !is_on_top;
                window.set_always_on_top(is_on_top);
            } else if menu_event.id == quit_id {
                *control_flow = ControlFlow::Exit;
            }
        }

        if let Ok(tray_event) = tray_channel.try_recv() {
            match tray_event {
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

        if let Event::WindowEvent { event, .. } = event {
            match event {
                WindowEvent::CloseRequested => {
                    window.set_visible(false);
                }
                _ => {}
            }
        }
    });
}

