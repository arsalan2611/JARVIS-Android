package com.jarvis.fa;

public class VoiceStateMachine {
    public enum State { IDLE, LISTENING, THINKING, EXECUTING, SPEAKING, ERROR }
    public interface Listener { void onState(State state, String label); }
    private State state=State.IDLE; private final Listener listener;
    public VoiceStateMachine(Listener l){listener=l;emit();}
    public synchronized void set(State s){state=s;emit();}
    public synchronized State get(){return state;}
    private void emit(){if(listener!=null)listener.onState(state,label(state));}
    private String label(State s){switch(s){case LISTENING:return "LISTENING • گوش می‌دهم";case THINKING:return "THINKING • در حال تحلیل";case EXECUTING:return "EXECUTING • در حال اجرا";case SPEAKING:return "SPEAKING • در حال پاسخ";case ERROR:return "ERROR • نیاز به بررسی";default:return "READY • سیستم آماده";}}
}
