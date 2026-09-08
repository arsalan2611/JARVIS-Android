import json, os, urllib.request, urllib.error
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST=os.getenv("JARVIS_BACKEND_HOST","0.0.0.0")
PORT=int(os.getenv("JARVIS_BACKEND_PORT","8080"))
CLIENT_TOKEN=os.getenv("JARVIS_BACKEND_TOKEN","")
OPENAI_KEY=os.getenv("OPENAI_API_KEY","")
OPENAI_URL="https://api.openai.com/v1/responses"

class Handler(BaseHTTPRequestHandler):
    def reply(self,code,body,ctype="application/json; charset=utf-8"):
        raw=body if isinstance(body,bytes) else body.encode("utf-8");self.send_response(code);self.send_header("Content-Type",ctype);self.send_header("Content-Length",str(len(raw)));self.end_headers();self.wfile.write(raw)
    def do_GET(self):
        if self.path=="/health": return self.reply(200,json.dumps({"ok":True,"openai_configured":bool(OPENAI_KEY)}))
        return self.reply(404,'{"ok":false}')
    def do_POST(self):
        if self.path!="/v1/responses": return self.reply(404,'{"ok":false}')
        if not CLIENT_TOKEN or self.headers.get("Authorization","")!="Bearer "+CLIENT_TOKEN: return self.reply(401,'{"error":"unauthorized"}')
        if not OPENAI_KEY: return self.reply(503,'{"error":"OPENAI_API_KEY not configured"}')
        try:
            n=int(self.headers.get("Content-Length","0"));payload=self.rfile.read(n)
            req=urllib.request.Request(OPENAI_URL,data=payload,method="POST",headers={"Content-Type":"application/json","Authorization":"Bearer "+OPENAI_KEY})
            with urllib.request.urlopen(req,timeout=95) as r: self.reply(r.status,r.read(),r.headers.get("Content-Type","application/json"))
        except urllib.error.HTTPError as e: self.reply(e.code,e.read(),e.headers.get("Content-Type","application/json"))
        except Exception as e: self.reply(502,json.dumps({"error":type(e).__name__}))
    def log_message(self,fmt,*args): pass

if __name__=="__main__":
    if not CLIENT_TOKEN: raise SystemExit("Set JARVIS_BACKEND_TOKEN")
    if not OPENAI_KEY: print("Warning: OPENAI_API_KEY is not set yet")
    print(f"JARVIS backend listening on {HOST}:{PORT}")
    ThreadingHTTPServer((HOST,PORT),Handler).serve_forever()
