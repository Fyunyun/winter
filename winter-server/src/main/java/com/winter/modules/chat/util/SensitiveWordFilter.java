package com.winter.modules.chat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Collection;

import org.springframework.stereotype.Service;

@Service
public class SensitiveWordFilter {

    private final ACNode root = new ACNode();

    private static class ACNode {
        Map<Character, ACNode> children = new HashMap<>();
        ACNode fail; // 失败指针
        List<String> output = new ArrayList<>(); // 输出链接：此节点结束的词
    }

    /**
     * 添加敏感词
     */
    public void addWord(String word) {
        if (word == null || word.isEmpty())
            return;
        // 可选：转小写忽略大小写
        word = word.toLowerCase();

        ACNode current = root;
        for (char c : word.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new ACNode());
        }
        current.output.add(word); // 标记词结束
    }

    /**
     * 批量添加词库
     */
    public void addWords(Collection<String> words) {
        for (String word : words) {
            addWord(word);
        }
    }

    /**
     * 构建失败指针（BFS）
     */
    public void buildFailPointers() {
        Queue<ACNode> queue = new LinkedList<>();
        root.fail = root; // 根的fail指向自己

        // 第一层：直接子节点fail指向root
        for (ACNode child : root.children.values()) {
            child.fail = root;
            queue.offer(child);
        }

        while (!queue.isEmpty()) {
            ACNode current = queue.poll();

            for (Map.Entry<Character, ACNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                ACNode child = entry.getValue();

                // 找fail：从current.fail开始沿c转移
                ACNode failNode = current.fail;
                while (failNode != root && failNode.children.get(c) == null) {
                    failNode = failNode.fail;
                }
                child.fail = (failNode.children.get(c) != null) ? failNode.children.get(c) : root;

                // 输出链接合并：继承fail节点的output
                child.output.addAll(child.fail.output);

                queue.offer(child);
            }
        }
    }

    /**
     * 匹配结果
     */
    public static class Match {
        int start; // 起始位置（包含）
        int end; // 结束位置（不包含）
        String word; // 匹配词

        public Match(int start, int end, String word) {
            this.start = start;
            this.end = end;
            this.word = word;
        }

        @Override
        public String toString() {
            return "Match{" + "start=" + start + ", end=" + end + ", word='" + word + '\'' + '}';
        }
    }

    /**
     * 搜索文本，返回所有匹配（可重叠）
     */
    public List<Match> search(String text) {
        if (text == null || text.isEmpty())
            return Collections.emptyList();
        text = text.toLowerCase(); // 忽略大小写

        List<Match> matches = new ArrayList<>();
        ACNode current = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 沿fail转移直到找到c或root
            while (current != root && current.children.get(c) == null) {
                current = current.fail;
            }
            if (current.children.get(c) != null) {
                current = current.children.get(c);
            } else {
                current = root; // root无转移，回root
            }

            // 收集所有output（包括继承的）
            for (String word : current.output) {
                matches.add(new Match(i - word.length() + 1, i + 1, word));
            }
        }
        return matches;
    }

    /**
     * 过滤文本：替换所有敏感词为*
     * 支持重叠匹配（贪婪替换最长？这里简单从左到右）
     */
    public String filter(String text) {
        if (text == null || text.isEmpty())
            return text;
        text = text.toLowerCase();

        StringBuilder sb = new StringBuilder();
        List<Match> matches = search(text);

        // 按起始位置排序，处理重叠（从左到右替换）
        matches.sort(Comparator.comparingInt(m -> m.start));

        int lastEnd = 0;
        for (Match match : matches) {
            // 加非敏感部分
            sb.append(text, lastEnd, match.start);
            // 替换敏感词
            sb.append("*".repeat(match.word.length()));
            lastEnd = match.end;
        }
        // 加剩余
        sb.append(text.substring(lastEnd));

        return sb.toString();
    }
}
