package com.swiss_stage.domain.participant;

import java.util.regex.Pattern;

/**
 * 段級位を表す値オブジェクト
 * 
 * 有効な段級位:
 * - 段位: 初段、2段、3段、...、9段
 * - 級位: 1級、2級、3級、...、20級
 * 
 * level値の変換ルール:
 * - 段位: 正の整数（初段=1、2段=2、...、9段=9）
 * - 級位: 負の整数（1級=-1、2級=-2、...、20級=-20）
 * 
 * ソート順: level値降順（9段 > 5段 > ... > 初段 > 1級 > ... > 20級）
 */
public record Rank(int level, String displayName) implements Comparable<Rank> {
    
    /**
     * 段級位の正規表現パターン
     * - 初段: 初段（1段は初段のみ有効）
     * - 2-9段: 2段、3段、...、9段
     * - 1-20級: 1級、2級、...、19級、20級
     */
    private static final Pattern RANK_PATTERN = 
        Pattern.compile("^(初段|[2-9]段|[1-9]級|1[0-9]級|20級)$");

    /**
     * コンストラクタ（不変条件チェック）
     */
    public Rank {
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("displayNameは必須です");
        }
    }

    /**
     * 文字列から段級位をパースする
     * 
     * @param input 段級位の文字列（例: "初段", "3段", "5級"）
     * @return Rankインスタンス
     * @throws IllegalArgumentException 不正なフォーマットの場合
     */
    public static Rank parse(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("段級位は必須です");
        }

        if (!RANK_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("段級位の形式が不正です: " + input);
        }

        // 初段の特別処理
        if ("初段".equals(input)) {
            return new Rank(1, "初段");
        }

        // 数値部分を抽出
        String numberPart = input.substring(0, input.length() - 1);
        int number = Integer.parseInt(numberPart);

        // 段位または級位の判定
        if (input.endsWith("段")) {
            // 段位: 正の整数
            return new Rank(number, input);
        } else {
            // 級位: 負の整数
            return new Rank(-number, input);
        }
    }

    /**
     * 段級位を比較する（降順: level値が大きい方が強い）
     * 
     * @param other 比較対象のRank
     * @return 負の値（this > other）、0（等しい）、正の値（this < other）
     */
    @Override
    public int compareTo(Rank other) {
        // 降順: level値が大きい方が先に来る
        return Integer.compare(other.level, this.level);
    }

    @Override
    public String toString() {
        return "Rank[level=" + level + ", displayName='" + displayName + "']";
    }
}
