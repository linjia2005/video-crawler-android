import json
import re
from kivy.app import App
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.textinput import TextInput
from kivy.uix.progressbar import ProgressBar
from kivy.uix.popup import Popup
from kivy.uix.image import AsyncImage
from kivy.uix.spinner import Spinner
from kivy.uix.checkbox import CheckBox
from kivy.uix.behaviors import ButtonBehavior
from kivy.clock import Clock
from kivy.network.urlrequest import UrlRequest
from kivy.metrics import dp
from kivy.core.window import Window
from kivy.utils import platform
from kivy.properties import StringProperty, NumericProperty, ObjectProperty


API = "http://192.168.1.100:5000"


def extract_url(text):
    m = re.search(r"https?://[^\s]+", text.strip())
    return m.group(0) if m else ""


# ─── Server API Client ────────────────────────────────
class APIClient:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")

    def _url(self, path):
        return f"{self.base_url}{path}"

    def info(self, on_success, on_failure):
        UrlRequest(self._url("/api/info"), on_success=on_success, on_failure=on_failure)

    def parse(self, url, on_success, on_failure):
        data = json.dumps({"url": url})
        headers = {"Content-Type": "application/json"}
        UrlRequest(self._url("/api/parse"), req_body=data, req_headers=headers,
                   on_success=on_success, on_failure=on_failure, method="POST")

    def download(self, url, fmt, save_dir, max_count, playlist, on_success, on_failure):
        data = json.dumps({"url": url, "format": fmt, "save_dir": save_dir,
                          "max_count": max_count, "playlist": playlist})
        headers = {"Content-Type": "application/json"}
        UrlRequest(self._url("/api/download"), req_body=data, req_headers=headers,
                   on_success=on_success, on_failure=on_failure, method="POST")

    def tasks(self, on_success, on_failure):
        UrlRequest(self._url("/api/tasks"), on_success=on_success, on_failure=on_failure)

    def action(self, task_id, action, on_success, on_failure):
        UrlRequest(self._url(f"/api/{action}/{task_id}"),
                   on_success=on_success, on_failure=on_failure, method="POST")


# ─── Screens ─────────────────────────────────────────
class HomeScreen(Screen):
    def __init__(self, **kw):
        super().__init__(**kw)
        self.api = None
        self.parse_result = None
        self.build_ui()

    def build_ui(self):
        root = BoxLayout(orientation="vertical", spacing=dp(8), padding=dp(12))
        root.add_widget(Label(text="Video Crawler Client", size_hint_y=None, height=dp(40),
                              font_size=dp(18), bold=True, color=(0.306, 0.788, 0.690, 1)))

        # Server status
        self.server_label = Label(text="服务器: 未连接", size_hint_y=None, height=dp(24),
                                  font_size=dp(12), color=(0.6, 0.6, 0.6, 1))
        root.add_widget(self.server_label)

        # URL input
        url_box = BoxLayout(size_hint_y=None, height=dp(48), spacing=dp(6))
        self.url_input = TextInput(hint_text="粘贴视频链接或分享文字", multiline=False,
                                   font_size=dp(14), size_hint_x=0.7,
                                   background_color=(0.15, 0.15, 0.15, 1),
                                   foreground_color=(0.83, 0.83, 0.83, 1))
        self.parse_btn = Button(text="解析", size_hint_x=0.3, font_size=dp(14),
                                background_color=(0.306, 0.788, 0.690, 1),
                                color=(0.1, 0.1, 0.1, 1), on_release=self.do_parse)
        url_box.add_widget(self.url_input)
        url_box.add_widget(self.parse_btn)
        root.add_widget(url_box)

        # Hint
        root.add_widget(Label(text="支持: Bilibili, Twitter, TikTok, 抖音, 微博 等 (不支持 YouTube)",
                              size_hint_y=None, height=dp(20), font_size=dp(9),
                              color=(0.878, 0.424, 0.459, 1)))

        # Preview area
        self.preview_box = BoxLayout(orientation="vertical", size_hint_y=None, height=dp(280))
        self.preview_box.opacity = 0
        self.thumb = AsyncImage(size_hint_y=None, height=dp(100), allow_stretch=True, keep_ratio=True)
        self.meta_title = Label(text="标题: -", font_size=dp(12), bold=True, color=(0.83, 0.83, 0.83, 1),
                                size_hint_y=None, height=dp(20))
        self.meta_uploader = Label(text="上传者: -", font_size=dp(11), color=(0.6, 0.6, 0.6, 1),
                                   size_hint_y=None, height=dp(18))
        self.meta_dur = Label(text="时长: -", font_size=dp(11), size_hint_y=None, height=dp(18))
        self.meta_res = Label(text="分辨率: -", font_size=dp(11), size_hint_y=None, height=dp(18))
        self.meta_size = Label(text="大小: -", font_size=dp(11), size_hint_y=None, height=dp(18))

        self.preview_box.add_widget(self.thumb)
        meta_box = BoxLayout(orientation="vertical", padding=(dp(4), 0))
        meta_box.add_widget(self.meta_title)
        meta_box.add_widget(self.meta_uploader)
        meta_box.add_widget(self.meta_dur)
        meta_box.add_widget(self.meta_res)
        meta_box.add_widget(self.meta_size)
        self.preview_box.add_widget(meta_box)

        # Quality & download row
        opt_box = BoxLayout(size_hint_y=None, height=dp(44), spacing=dp(6))
        self.quality_spinner = Spinner(text="最佳画质", values=(
            "最佳画质", "1080p", "720p", "480p", "360p", "仅音频"),
            size_hint_x=0.5, font_size=dp(13),
            background_color=(0.2, 0.2, 0.2, 1), color=(0.83, 0.83, 0.83, 1))
        self.dl_btn = Button(text="开始下载", size_hint_x=0.5, font_size=dp(14),
                             background_color=(0.306, 0.788, 0.690, 1),
                             color=(0.1, 0.1, 0.1, 1), on_release=self.do_download)
        opt_box.add_widget(self.quality_spinner)
        opt_box.add_widget(self.dl_btn)
        self.preview_box.add_widget(opt_box)

        root.add_widget(self.preview_box)
        root.add_widget(BoxLayout())  # spacer
        self.add_widget(root)

    def set_api(self, api):
        self.api = api
        api.info(on_success=self._on_info, on_failure=lambda *a: setattr(self.server_label, "text", "服务器: 连接失败"))

    def _on_info(self, req, result):
        try:
            d = json.loads(result) if isinstance(result, str) else result
            if d.get("success"):
                self.server_label.text = f"服务器: {d['ip']}:{d['port']} ✅"
        except:
            self.server_label.text = "服务器: 连接失败"

    def _show_popup(self, title, msg):
        popup = Popup(title=title, content=Label(text=msg, color=(0.83, 0.83, 0.83, 1)),
                      size_hint=(0.8, 0.4), background_color=(0.15, 0.15, 0.15, 1),
                      title_color=(0.306, 0.788, 0.690, 1))
        popup.open()

    def do_parse(self, btn):
        if not self.api:
            self._show_popup("错误", "请先在设置中配置服务器地址")
            return
        raw = self.url_input.text.strip()
        url = extract_url(raw)
        if not url:
            self._show_popup("提示", "未检测到链接")
            return
        self.url_input.text = url
        self.parse_btn.text = "解析中..."
        self.parse_btn.disabled = True
        self.api.parse(url, on_success=self._on_parse_ok, on_failure=self._on_parse_fail)

    def _on_parse_ok(self, req, result):
        self.parse_btn.text = "解析"
        self.parse_btn.disabled = False
        try:
            d = json.loads(result) if isinstance(result, str) else result
        except:
            d = result
        if not d.get("success"):
            self._show_popup("解析失败", d.get("error", "未知错误"))
            return
        self.parse_result = d
        self.meta_title.text = f"标题: {d.get('title', '-')}"
        self.meta_uploader.text = f"上传者: {d.get('uploader', '-')}"
        self.meta_dur.text = f"时长: {d.get('duration', '-')}"
        self.meta_res.text = f"分辨率: {d.get('resolution', '-')}"
        self.meta_size.text = f"大小: {d.get('size', '-')}"
        if d.get("thumbnail"):
            self.thumb.source = d["thumbnail"]
        self.preview_box.opacity = 1

    def _on_parse_fail(self, req, result):
        self.parse_btn.text = "解析"
        self.parse_btn.disabled = False
        self._show_popup("网络错误", "无法连接到服务器，请检查设置")

    def do_download(self, btn):
        if not self.api or not self.parse_result:
            self._show_popup("提示", "请先解析视频")
            return
        raw = self.url_input.text.strip()
        url = extract_url(raw)
        if not url:
            self._show_popup("提示", "链接无效")
            return
        fmt_map = {"最佳画质": "best", "1080p": "best[height<=1080]", "720p": "best[height<=720]",
                    "480p": "best[height<=480]", "360p": "best[height<=360]", "仅音频": "bestaudio/best"}
        fmt = fmt_map.get(self.quality_spinner.text, "best")
        self.dl_btn.text = "提交中..."
        self.dl_btn.disabled = True
        self.api.download(url, fmt, "Downloads/VideoCrawler", 5, True,
                          on_success=self._on_dl_ok, on_failure=self._on_dl_fail)

    def _on_dl_ok(self, req, result):
        self.dl_btn.text = "开始下载"
        self.dl_btn.disabled = False
        try:
            d = json.loads(result) if isinstance(result, str) else result
        except:
            d = result
        if d.get("success"):
            self._show_popup("成功", f"下载已开始\n任务ID: {d.get('task_id', '')}")
            app = App.get_running_app()
            if app:
                app.switch_to("tasks")
        else:
            self._show_popup("失败", d.get("error", "启动下载失败"))

    def _on_dl_fail(self, req, result):
        self.dl_btn.text = "开始下载"
        self.dl_btn.disabled = False
        self._show_popup("网络错误", "无法连接到服务器")


class TasksScreen(Screen):
    def __init__(self, **kw):
        super().__init__(**kw)
        self.api = None
        self.poll_event = None
        self.build_ui()

    def build_ui(self):
        root = BoxLayout(orientation="vertical", spacing=dp(4), padding=dp(8))
        root.add_widget(Label(text="下载任务", size_hint_y=None, height=dp(36),
                              font_size=dp(16), bold=True, color=(0.306, 0.788, 0.690, 1)))
        self.scroll = ScrollView()
        self.task_list = GridLayout(cols=1, spacing=dp(6), size_hint_y=None)
        self.task_list.bind(minimum_height=self.task_list.setter("height"))
        self.scroll.add_widget(self.task_list)
        root.add_widget(self.scroll)
        self.add_widget(root)

    def set_api(self, api):
        self.api = api

    def on_enter(self):
        self.start_poll()

    def on_leave(self):
        self.stop_poll()

    def start_poll(self):
        self.stop_poll()
        self.poll_event = Clock.schedule_interval(lambda dt: self.refresh(), 2)

    def stop_poll(self):
        if self.poll_event:
            self.poll_event.cancel()
            self.poll_event = None

    def refresh(self):
        if self.api:
            self.api.tasks(on_success=self._on_tasks, on_failure=lambda *a: None)

    def _on_tasks(self, req, result):
        try:
            d = json.loads(result) if isinstance(result, str) else result
        except:
            d = result
        if not d.get("success"):
            return
        tasks = d.get("tasks", [])
        self.task_list.clear_widgets()
        if not tasks:
            self.task_list.add_widget(Label(text="暂无下载任务", size_hint_y=None,
                                      height=dp(60), color=(0.5, 0.5, 0.5, 1)))
            return
        for t in tasks:
            self.task_list.add_widget(TaskCard(t, self.api))

    def _on_action_ok(self, req, result):
        self.refresh()


class TaskCard(BoxLayout):
    def __init__(self, task, api, **kw):
        super().__init__(**kw)
        self.api = api
        self.task_id = task.get("id", "")
        self.orientation = "vertical"
        self.size_hint_y = None
        self.height = dp(120)
        self.padding = dp(8)
        self.spacing = dp(4)
        self.build_ui(task)

    def build_ui(self, task):
        title = task.get("title", "未知")
        status = task.get("status", "idle")
        pct = task.get("percent", 0)
        speed = task.get("speed", "")
        eta = task.get("eta", "")
        count = task.get("downloaded_count", 0)

        status_colors = {
            "downloading": (0.306, 0.788, 0.690, 1),
            "paused": (0.863, 0.863, 0.663, 1),
            "completed": (0.306, 0.788, 0.690, 1),
            "error": (0.957, 0.278, 0.278, 1),
            "cancelled": (0.6, 0.6, 0.6, 1),
            "parsing": (0.306, 0.788, 0.690, 1),
        }
        sc = status_colors.get(status, (0.6, 0.6, 0.6, 1))
        status_labels = {"idle": "等待", "parsing": "解析中", "ready": "就绪",
                         "downloading": "下载中", "paused": "已暂停",
                         "completed": "已完成", "error": "错误", "cancelled": "已取消"}

        title_label = Label(text=title[:40], font_size=dp(12), bold=True,
                            color=(0.83, 0.83, 0.83, 1), size_hint_y=None, height=dp(20),
                            halign="left", text_size=(Window.width - dp(60), None))
        status_label = Label(text=status_labels.get(status, status), font_size=dp(10),
                             color=sc, size_hint_y=None, height=dp(16))

        header = BoxLayout(size_hint_y=None, height=dp(36))
        header.add_widget(title_label)
        header.add_widget(status_label)

        self.add_widget(header)

        # Progress bar
        pb = ProgressBar(max=100, value=pct, size_hint_y=None, height=dp(6))
        self.add_widget(pb)

        # Info
        info = f"{pct:.0f}%  {speed}  {eta}  {count}个"
        self.add_widget(Label(text=info, font_size=dp(10), color=(0.6, 0.6, 0.6, 1),
                              size_hint_y=None, height=dp(18)))

        # Actions
        act_box = BoxLayout(size_hint_y=None, height=dp(32), spacing=dp(6))
        if status == "downloading":
            btn = Button(text="暂停", font_size=dp(11), size_hint_x=0.3,
                         background_color=(0.863, 0.863, 0.663, 1), color=(0.1, 0.1, 0.1, 1))
            btn.bind(on_release=lambda b: self._do("pause"))
            act_box.add_widget(btn)
        if status == "paused":
            btn = Button(text="继续", font_size=dp(11), size_hint_x=0.3,
                         background_color=(0.306, 0.788, 0.690, 1), color=(0.1, 0.1, 0.1, 1))
            btn.bind(on_release=lambda b: self._do("resume"))
            act_box.add_widget(btn)
        if status in ("downloading", "paused", "ready", "parsing"):
            btn = Button(text="取消", font_size=dp(11), size_hint_x=0.3,
                         background_color=(0.957, 0.278, 0.278, 1), color=(1, 1, 1, 1))
            btn.bind(on_release=lambda b: self._do("cancel"))
            act_box.add_widget(btn)
        self.add_widget(act_box)

    def _do(self, action):
        if self.api and self.task_id:
            self.api.action(self.task_id, action,
                            on_success=lambda *a: None, on_failure=lambda *a: None)


class SettingsScreen(Screen):
    def __init__(self, **kw):
        super().__init__(**kw)
        self.on_save = None
        self.build_ui()

    def build_ui(self):
        root = BoxLayout(orientation="vertical", spacing=dp(10), padding=dp(16))

        root.add_widget(Label(text="服务器设置", size_hint_y=None, height=dp(40),
                              font_size=dp(16), bold=True, color=(0.306, 0.788, 0.690, 1)))

        root.add_widget(Label(text="服务器地址", size_hint_y=None, height=dp(20),
                              font_size=dp(12), color=(0.6, 0.6, 0.6, 1), halign="left"))
        self.ip_input = TextInput(text=API, multiline=False, font_size=dp(14),
                                  size_hint_y=None, height=dp(44),
                                  background_color=(0.15, 0.15, 0.15, 1),
                                  foreground_color=(0.83, 0.83, 0.83, 1))
        root.add_widget(self.ip_input)

        root.add_widget(Label(text="格式: http://192.168.x.x:5000", size_hint_y=None, height=dp(20),
                              font_size=dp(10), color=(0.5, 0.5, 0.5, 1)))

        root.add_widget(BoxLayout(size_hint_y=None, height=dp(20)))

        save_btn = Button(text="保存并连接", size_hint_y=None, height=dp(48), font_size=dp(15),
                          background_color=(0.306, 0.788, 0.690, 1), color=(0.1, 0.1, 0.1, 1))
        save_btn.bind(on_release=self.save)
        root.add_widget(save_btn)

        root.add_widget(Label(text="确保手机和 PC 在同一 Wi-Fi 网络", size_hint_y=None, height=dp(24),
                              font_size=dp(10), color=(0.5, 0.5, 0.5, 1)))

        root.add_widget(BoxLayout())
        self.add_widget(root)

    def save(self, btn):
        url = self.ip_input.text.strip().rstrip("/")
        if self.on_save:
            self.on_save(url)


# ─── Main App ─────────────────────────────────────────
class VideoClientApp(App):
    def build(self):
        self.title = "Video Crawler"
        self.api_client = APIClient(API)

        self.sm = ScreenManager()
        self.home = HomeScreen(name="home")
        self.tasks = TasksScreen(name="tasks")
        self.settings = SettingsScreen(name="settings")

        self.home.set_api(self.api_client)
        self.tasks.set_api(self.api_client)
        self.settings.on_save = self._on_save

        self.sm.add_widget(self.home)
        self.sm.add_widget(self.tasks)
        self.sm.add_widget(self.settings)

        # Build bottom nav
        root = BoxLayout(orientation="vertical")
        root.add_widget(self.sm)
        nav = BoxLayout(size_hint_y=None, height=dp(56), spacing=dp(2),
                        padding=(dp(4), dp(4)))
        nav.add_widget(self._nav_btn("📥 下载", "home"))
        nav.add_widget(self._nav_btn("📋 任务", "tasks"))
        nav.add_widget(self._nav_btn("⚙ 设置", "settings"))
        root.add_widget(nav)

        return root

    def _nav_btn(self, text, screen):
        btn = Button(text=text, font_size=dp(12), background_color=(0.2, 0.2, 0.2, 1),
                     color=(0.83, 0.83, 0.83, 1))
        btn.bind(on_release=lambda b: self.switch_to(screen))
        return btn

    def switch_to(self, name):
        self.sm.current = name

    def _on_save(self, url):
        self.api_client = APIClient(url)
        self.home.set_api(self.api_client)
        self.tasks.set_api(self.api_client)
        self.home.server_label.text = "服务器: 连接中..."
        self.home.api.info(on_success=self.home._on_info,
                           on_failure=lambda *a: setattr(self.home.server_label, "text", "服务器: 连接失败"))
        self.switch_to("home")


if __name__ == "__main__":
    VideoClientApp().run()
