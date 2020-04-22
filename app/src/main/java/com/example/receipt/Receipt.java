package com.example.receipt;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

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

    public static final String GOODS_COUNT_PATTERN = "[×|x|X][0-9]*";

    public static final String TOTAL_PRICE_PATTERN = "总计:￥|总额|总计";

    public int mType;

    public long mLogId;

    public String mTime;

    public String mShop;

    public Map<Goods, Integer> mGoods = new HashMap<>();

    public float mActualPrice;

    public float mTotalPrice;

    public int mTotalGoods;

    class Goods {

        public String mName;

        public float price;
    }

    public static String StringType(int type) {
        if (type == TYPE_MEITUAN) {
            return "TYPE_MEITUAN";
        } else if (type == TYPE_ELE) {
            return "TYPE_ELE";
        } else if (type == TYPE_WALMART) {
            return "TYPE_WALMART";
        } else if (type == TYPE_OTHER) {
            return "TYPE_OTHER";
        }
        return "UNKNOWN_TYPE";
    }

    @NonNull
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("mLogId=");
        sb.append(mLogId);
        sb.append(",mType=");
        sb.append(StringType(mType));
        sb.append(",mTime=");
        sb.append(mTime);
        sb.append(",mShop=");
        sb.append(mShop);
        sb.append(",mActualPrice=");
        sb.append(mActualPrice);
        sb.append(",mActualPrice=");
        sb.append(mActualPrice);
        sb.append(",mTotalPrice=");
        sb.append(mTotalPrice);
        sb.append(",mTotalGoods=");
        sb.append(mTotalGoods);

        sb.append(", Goods: ");
        for (Goods goods : mGoods.keySet()) {
            sb.append("[mName: ");
            sb.append(goods.mName);
            sb.append(", price: ");
            sb.append(goods.price);
            sb.append(", count: ");
            sb.append(mGoods.get(goods));
            sb.append("],");
        }

        return sb.toString();
    }
}
