package com.winter.common.model;

public class SensitiveWordsModel {

    private String word; // 敏感词文本
    private int level; // 敏感词等级，0-普通敏感词，1-中等敏感词，2-严重敏感词
    private String category;

    public SensitiveWordsModel(String word, int level, String category) {
        this.word = word;
        this.level = level;
        this.category = category;
    }

    public String getWord() {
        return word;
    }

    public int getLevel() {
        return level;
    }

    public String getCategory() {
        return category;
    }
    
}
