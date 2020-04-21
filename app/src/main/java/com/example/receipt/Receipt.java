package com.example.receipt;

import java.util.HashMap;
import java.util.Map;

public class Receipt {

    public static final int TYPE_MEITUAN = 0b100;
    public static final int TYPE_ELE = 0b101;
    public static final int TYPE_WALMART = 0b110;
    public static final int TYPE_OTHER = 0b111;

    public static final String DATE_PATTERN_1 = "[0-9]{1,4}年[0-9]{1,2}月[0-9]{1,2}日";
    public static final String DATE_PATTERN_2 = "[0-9]{1,4}-[0-9]{1,2}-[0-9]{1,2}";
    public static final String DATE_PATTERN_3 = "[0-9]{1,4}\\.[0-9]{1,2}\\.[0-9]{1,2}";
    public static final String DATE_PATTERN_4 = "[0-9]{1,4}/[0-9]{1,2}/[0-9]{1,2}";
    public static final String TIME_PATTERN_5 = "[0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2}";
    public static final String TIME_PATTERN_6 = "[0-9]{1,2}:[0-9]{1,2}";

    public int mType;

    public String mTime;

    public String mShop;

    public Map<Goods, Integer> mGoods = new HashMap<>();

    public float mActurlPrice;

    public float mTotalPrice;

    public int mTotalGoods;

    class Goods {

        public String mName;

        public float price;
    }
}
