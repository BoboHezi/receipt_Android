package com.example.receipt;

import java.util.List;

public class BillInfo {

    public static final int TYPE_MEITUAN = 0b001;
    public static final int TYPE_ELE = 0b010;
    public static final int TYPE_MARKET = 0b011;
    public static final int TYPE_OTHER = 0b100;

    //模板
    int type;
    //商品详情
    List<GoodoDetail> goodoDetail;
    //实付金额
    String payAmount;
    //支付日期
    String payDate;
    //商店名
    String shopName;
    //总金额
    String totalAmount;
    //总数量
    int totalNum;

    class GoodoDetail {
        //商品名
        String goodsName;
        //数量
        String amount;
        //金额
        String number;
    }
}