import json, os, sys, webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = os.getenv("JARVIS_PC_HOST", "0.0.0.0")
PORT = int(os.getenv("JARVIS_PC_PORT", "8765"))
TOKEN = os.getenv("JARVIS_PC_TOKEN", "")

def run_command(text: str):
    q=(text or "").strip()
    low=q.lower()
    if low.startswith("browser "):
        webbrowser.open(q[8:].strip()); return "مرورگر کامپیوتر باز شد."
    if low.startswith("open "):
        target=q[5:].strip()
        if not target: return "مسیر خالی است."
        if os.name=="nt": os.startfile(target)
        else: return "این Bridge برای Windows طراحی شده است."
        return "مسیر روی کامپیوتر باز شد."
    if low in ("downloads","open downloads","دانلودها"):
        target=os.path.join(os.path.expanduser("~"),"Downloads")
        if os.name=="nt": os.startfile(target); return "Downloads باز شد."
    if low in ("documents","open documents","اسناد"):
        target=os.path.join(os.path.expanduser("~"),"Documents")
        if os.name=="nt": os.startfile(target); return "Documents باز شد."
    if low in ("notepad","نوت پد","دفترچه یادداشت"):
        if os.name=="nt": os.startfile("notepad.exe"); return "Notepad باز شد."
    return "فرمان در لیست امن PC Bridge نیست. از browser URL، open PATH، downloads، documents یا notepad استفاده کن."

class Handler(BaseHTTPRequestHandler):
    def send_json(self, code, data):
        raw=json.dumps(data,ensure_ascii=False).encode("utf-8")
        self.send_response(code); self.send_header("Content-Type","application/json; charset=utf-8"); self.send_header("Content-Length",str(len(raw))); self.end_headers(); self.wfile.write(raw)
    def do_GET(self):
        if self.path=="/health": return self.send_json(200,{"ok":True,"service":"jarvis-pc-bridge"})
        self.send_json(404,{"ok":False})
    def do_POST(self):
        if self.path!="/jarvis/command": return self.send_json(404,{"ok":False})
        if not TOKEN or self.headers.get("Authorization","")!="Bearer "+TOKEN: return self.send_json(401,{"ok":False,"message":"unauthorized"})
        try:
            n=int(self.headers.get("Content-Length","0")); body=json.loads(self.rfile.read(n).decode("utf-8")); msg=run_command(body.get("command","")); self.send_json(200,{"ok":True,"message":msg})
        except Exception as e: self.send_json(500,{"ok":False,"message":type(e).__name__})
    def log_message(self, fmt, *args): pass

if __name__=="__main__":
    if not TOKEN:
        print("Set JARVIS_PC_TOKEN before starting."); sys.exit(2)
    print(f"JARVIS PC Bridge listening on {HOST}:{PORT}")
    ThreadingHTTPServer((HOST,PORT),Handler).serve_forever()
