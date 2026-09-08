import base64, json, os, secrets, time, urllib.request, urllib.error, urllib.parse
from email.message import EmailMessage
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST=os.getenv("JARVIS_BACKEND_HOST","0.0.0.0")
PORT=int(os.getenv("JARVIS_BACKEND_PORT") or os.getenv("PORT") or "8080")
CLIENT_TOKEN=os.getenv("JARVIS_BACKEND_TOKEN","")
OPENAI_KEY=os.getenv("OPENAI_API_KEY","")
OPENAI_URL="https://api.openai.com/v1/responses"
GOOGLE_CLIENT_ID=os.getenv("GOOGLE_CLIENT_ID","")
GOOGLE_CLIENT_SECRET=os.getenv("GOOGLE_CLIENT_SECRET","")
GOOGLE_REDIRECT_URI=os.getenv("GOOGLE_REDIRECT_URI","")
TOKEN_FILE=os.getenv("JARVIS_GOOGLE_TOKEN_FILE","/tmp/jarvis_google_token.json")
GOOGLE_SCOPES=[
    "https://www.googleapis.com/auth/gmail.readonly",
    "https://www.googleapis.com/auth/gmail.compose",
    "https://www.googleapis.com/auth/calendar.events",
    "https://www.googleapis.com/auth/calendar.readonly",
]
OAUTH_STATES={}

def jdump(x): return json.dumps(x,ensure_ascii=False,separators=(",",":"))
def load_token():
    try:
        with open(TOKEN_FILE,"r",encoding="utf-8") as f:return json.load(f)
    except Exception:return {}
def save_token(t):
    try:
        d=os.path.dirname(TOKEN_FILE)
        if d: os.makedirs(d,exist_ok=True)
        with open(TOKEN_FILE,"w",encoding="utf-8") as f:json.dump(t,f)
        try: os.chmod(TOKEN_FILE,0o600)
        except Exception: pass
    except Exception: pass

def google_configured(): return bool(GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET and GOOGLE_REDIRECT_URI)
def form_post(url,data,headers=None,timeout=25):
    raw=urllib.parse.urlencode(data).encode()
    req=urllib.request.Request(url,data=raw,method="POST",headers=headers or {})
    with urllib.request.urlopen(req,timeout=timeout) as r:return r.status,json.loads(r.read().decode("utf-8"))
def google_access_token():
    t=load_token()
    if t.get("access_token") and float(t.get("expires_at",0))>time.time()+60:return t["access_token"]
    if not t.get("refresh_token") or not google_configured():return ""
    try:
        _,x=form_post("https://oauth2.googleapis.com/token",{
            "client_id":GOOGLE_CLIENT_ID,"client_secret":GOOGLE_CLIENT_SECRET,
            "refresh_token":t["refresh_token"],"grant_type":"refresh_token"})
        if not x.get("access_token"):return ""
        t["access_token"]=x["access_token"];t["expires_at"]=time.time()+int(x.get("expires_in",3600));save_token(t);return t["access_token"]
    except Exception:return ""
def google_call(method,url,body=None):
    token=google_access_token()
    if not token: raise RuntimeError("google_not_connected")
    data=None if body is None else jdump(body).encode("utf-8")
    h={"Authorization":"Bearer "+token,"Accept":"application/json"}
    if data is not None:h["Content-Type"]="application/json"
    req=urllib.request.Request(url,data=data,method=method,headers=h)
    with urllib.request.urlopen(req,timeout=30) as r:return json.loads(r.read().decode("utf-8") or "{}")
def header_value(headers,name):
    for h in headers or []:
        if h.get("name","").lower()==name.lower():return h.get("value","")
    return ""

class Handler(BaseHTTPRequestHandler):
    def reply(self,code,body,ctype="application/json; charset=utf-8"):
        raw=body if isinstance(body,bytes) else body.encode("utf-8")
        self.send_response(code);self.send_header("Content-Type",ctype);self.send_header("Content-Length",str(len(raw)));self.send_header("Cache-Control","no-store");self.send_header("X-Content-Type-Options","nosniff");self.end_headers();self.wfile.write(raw)
    def json_reply(self,code,obj): self.reply(code,jdump(obj))
    def authorized(self): return bool(CLIENT_TOKEN) and self.headers.get("Authorization","")=="Bearer "+CLIENT_TOKEN
    def read_json(self,max_bytes=500000):
        n=int(self.headers.get("Content-Length","0"));
        if n<=0 or n>max_bytes: raise ValueError("invalid_payload_size")
        return json.loads(self.rfile.read(n).decode("utf-8"))
    def do_GET(self):
        p=urllib.parse.urlparse(self.path)
        if p.path=="/health":
            return self.json_reply(200,{"ok":True,"service":"jarvis-backend","openai_configured":bool(OPENAI_KEY),"google_configured":google_configured(),"google_connected":bool(google_access_token())})
        if p.path=="/oauth/google/callback": return self.google_callback(p)
        if p.path=="/integrations/status":
            if not self.authorized():return self.json_reply(401,{"error":"unauthorized"})
            return self.json_reply(200,{"ok":True,"google":{"configured":google_configured(),"connected":bool(google_access_token()),"gmail":bool(google_access_token()),"calendar":bool(google_access_token())}})
        return self.json_reply(404,{"ok":False})
    def google_callback(self,p):
        q=urllib.parse.parse_qs(p.query);state=(q.get("state") or [""])[0];code=(q.get("code") or [""])[0]
        if not state or state not in OAUTH_STATES or not code:return self.reply(400,"Google authorization failed.","text/plain; charset=utf-8")
        OAUTH_STATES.pop(state,None)
        try:
            _,t=form_post("https://oauth2.googleapis.com/token",{"code":code,"client_id":GOOGLE_CLIENT_ID,"client_secret":GOOGLE_CLIENT_SECRET,"redirect_uri":GOOGLE_REDIRECT_URI,"grant_type":"authorization_code"})
            if not t.get("access_token"):raise RuntimeError("missing_access_token")
            old=load_token();
            if not t.get("refresh_token") and old.get("refresh_token"):t["refresh_token"]=old["refresh_token"]
            t["expires_at"]=time.time()+int(t.get("expires_in",3600));save_token(t)
            return self.reply(200,"JARVIS connected to Google successfully. You can return to the app.","text/plain; charset=utf-8")
        except Exception as e:return self.reply(502,"Google authorization could not be completed.","text/plain; charset=utf-8")
    def do_POST(self):
        p=urllib.parse.urlparse(self.path)
        if p.path=="/v1/responses": return self.proxy_openai()
        if not self.authorized():return self.json_reply(401,{"error":"unauthorized"})
        try:
            if p.path=="/integrations/google/start": return self.start_google()
            if p.path=="/gmail/search": return self.gmail_search()
            if p.path=="/gmail/draft": return self.gmail_draft()
            if p.path=="/calendar/upcoming": return self.calendar_upcoming()
            if p.path=="/calendar/events": return self.calendar_create()
            return self.json_reply(404,{"ok":False})
        except urllib.error.HTTPError as e:
            try:detail=e.read().decode("utf-8")[:1200]
            except Exception:detail=""
            return self.json_reply(e.code,{"ok":False,"error":"upstream_http","detail":detail})
        except Exception as e:return self.json_reply(502,{"ok":False,"error":str(e)[:200] or type(e).__name__})
    def proxy_openai(self):
        if not self.authorized():return self.json_reply(401,{"error":"unauthorized"})
        if not OPENAI_KEY:return self.json_reply(503,{"error":"OPENAI_API_KEY not configured"})
        try:
            n=int(self.headers.get("Content-Length","0"))
            if n<=0 or n>2_000_000:return self.json_reply(413,{"error":"invalid_payload_size"})
            payload=self.rfile.read(n);req=urllib.request.Request(OPENAI_URL,data=payload,method="POST",headers={"Content-Type":"application/json","Authorization":"Bearer "+OPENAI_KEY})
            with urllib.request.urlopen(req,timeout=95) as r:self.reply(r.status,r.read(),r.headers.get("Content-Type","application/json"))
        except urllib.error.HTTPError as e:self.reply(e.code,e.read(),e.headers.get("Content-Type","application/json"))
        except Exception as e:self.json_reply(502,{"error":type(e).__name__})
    def start_google(self):
        if not google_configured():return self.json_reply(503,{"ok":False,"error":"google_oauth_not_configured"})
        state=secrets.token_urlsafe(32);OAUTH_STATES[state]=time.time()
        for k,v in list(OAUTH_STATES.items()):
            if time.time()-v>900:OAUTH_STATES.pop(k,None)
        params={"client_id":GOOGLE_CLIENT_ID,"redirect_uri":GOOGLE_REDIRECT_URI,"response_type":"code","scope":" ".join(GOOGLE_SCOPES),"access_type":"offline","prompt":"consent","include_granted_scopes":"true","state":state}
        return self.json_reply(200,{"ok":True,"auth_url":"https://accounts.google.com/o/oauth2/v2/auth?"+urllib.parse.urlencode(params)})
    def gmail_search(self):
        x=self.read_json();q=str(x.get("query","")).strip();mx=max(1,min(int(x.get("max_results",8)),15))
        url="https://gmail.googleapis.com/gmail/v1/users/me/messages?"+urllib.parse.urlencode({"q":q,"maxResults":mx})
        data=google_call("GET",url);items=[]
        for m in data.get("messages",[])[:mx]:
            mid=m.get("id","");meta=google_call("GET","https://gmail.googleapis.com/gmail/v1/users/me/messages/"+urllib.parse.quote(mid)+"?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date")
            h=meta.get("payload",{}).get("headers",[]);items.append({"id":mid,"thread_id":meta.get("threadId",""),"from":header_value(h,"From"),"subject":header_value(h,"Subject"),"date":header_value(h,"Date"),"snippet":meta.get("snippet","")})
        return self.json_reply(200,{"ok":True,"messages":items})
    def gmail_draft(self):
        x=self.read_json();to=str(x.get("to","")).strip();subject=str(x.get("subject","")).strip();body=str(x.get("body","")).strip()
        if not to:return self.json_reply(400,{"ok":False,"error":"recipient_required"})
        msg=EmailMessage();msg["To"]=to;msg["Subject"]=subject;msg.set_content(body)
        raw=base64.urlsafe_b64encode(msg.as_bytes()).decode("ascii").rstrip("=")
        d=google_call("POST","https://gmail.googleapis.com/gmail/v1/users/me/drafts",{"message":{"raw":raw}})
        return self.json_reply(200,{"ok":True,"draft_id":d.get("id","")})
    def calendar_upcoming(self):
        x=self.read_json();mx=max(1,min(int(x.get("max_results",10)),20));now=time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime())
        qs=urllib.parse.urlencode({"timeMin":now,"maxResults":mx,"singleEvents":"true","orderBy":"startTime"})
        d=google_call("GET","https://www.googleapis.com/calendar/v3/calendars/primary/events?"+qs)
        out=[]
        for e in d.get("items",[]):out.append({"id":e.get("id",""),"summary":e.get("summary",""),"start":e.get("start",{}).get("dateTime") or e.get("start",{}).get("date",""),"end":e.get("end",{}).get("dateTime") or e.get("end",{}).get("date",""),"location":e.get("location","")})
        return self.json_reply(200,{"ok":True,"events":out})
    def calendar_create(self):
        x=self.read_json();title=str(x.get("title","")).strip();start=str(x.get("start","")).strip();end=str(x.get("end","")).strip();tz=str(x.get("timezone","Asia/Baku")).strip() or "Asia/Baku"
        if not title or not start or not end:return self.json_reply(400,{"ok":False,"error":"title_start_end_required"})
        body={"summary":title,"start":{"dateTime":start,"timeZone":tz},"end":{"dateTime":end,"timeZone":tz}}
        if x.get("location"):body["location"]=str(x.get("location"))
        if x.get("description"):body["description"]=str(x.get("description"))
        e=google_call("POST","https://www.googleapis.com/calendar/v3/calendars/primary/events",body)
        return self.json_reply(200,{"ok":True,"event_id":e.get("id",""),"html_link":e.get("htmlLink","")})
    def log_message(self,fmt,*args):pass

if __name__=="__main__":
    if not CLIENT_TOKEN:raise SystemExit("Set JARVIS_BACKEND_TOKEN")
    if not OPENAI_KEY:print("Warning: OPENAI_API_KEY is not set yet")
    if not google_configured():print("Google OAuth disabled until GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI are set")
    print(f"JARVIS backend listening on {HOST}:{PORT}")
    ThreadingHTTPServer((HOST,PORT),Handler).serve_forever()
