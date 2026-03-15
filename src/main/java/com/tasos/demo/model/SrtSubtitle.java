package com.tasos.demo.model;

public class SrtSubtitle {
    private int index;
    private String timecode; // "00:01:03,812 --> 00:01:07,248"
    private String text;
    private String originalText; // text with HTML tags preserved

    public SrtSubtitle(int index, String timecode, String text) {
        this.index = index;
        this.timecode = timecode;
        this.text = text;
        this.originalText = text;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getTimecode() {
        return timecode;
    }

    public void setTimecode(String timecode) {
        this.timecode = timecode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    @Override
    public String toString() {
        return "SrtSubtitle{" +
                "index=" + index +
                ", timecode='" + timecode + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}

