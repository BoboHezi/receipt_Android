package com.example.receipt;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecodeTask extends AsyncTask<Object, ViewGroup, Receipt> {

    private static final String TAG = "DecodeTask";

    private Context mContext;

    private String mFilePath;

    private ViewGroup mContainer;

    @Override
    protected Receipt doInBackground(Object... params) {
        if (params.length >= 3) {
            mContext = (Context) params[0];
            mFilePath = (String) params[1];
            mContainer = (ViewGroup) params[2];
            return decodeReceipt(mContext, mFilePath);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Receipt receipt) {
        if (receipt != null && mContainer != null) {
            Log.i(TAG, receipt.toString());

            StringBuilder sb = new StringBuilder();
            sb.append(mContext.getString(R.string.shop_name, receipt.mShop));
            sb.append("\n");
            sb.append(mContext.getString(R.string.receipt_time, receipt.mTime));
            sb.append("\n");
            sb.append(mContext.getString(R.string.goods));
            for (Receipt.Goods goods : receipt.mGoods.keySet()) {
                sb.append("\n    ");
                sb.append(goods.mName);
                sb.append("    ");
                sb.append(receipt.mGoods.get(goods));
                sb.append("    ");
                sb.append(goods.price > 0 ? goods.price : 0);
            }
            sb.append("\n\n");
            sb.append(mContext.getString(R.string.actual_price, receipt.mActualPrice));
            sb.append("\n");
            sb.append(mContext.getString(R.string.total_price, receipt.mTotalPrice));
            sb.append("\n");
            sb.append(mContext.getString(R.string.goods_count, receipt.mTotalGoods));

            mContainer.removeAllViews();
            TextView tv = new TextView(mContext);
            tv.setText(sb.toString());
            tv.setTextSize(23);
            tv.setLineSpacing(10, 1.3f);
            mContainer.addView(tv);
        }
    }

    private Receipt decodeReceipt(Context context, String path) {
        ReceiptBean bean = Utils.decodeJson(path);
        if (bean == null || (bean.type & 0b100) != 0b100) {
            return null;
        }

        Receipt receipt = new Receipt();
        receipt.mType = bean.type;
        receipt.mLogId = bean.log_id;

        final int type = bean.type;

        String datePattern = null;
        String timePattern = null;

        String shopPattern = null;
        int shopOffset = 0;

        int totalPriceOffset = 0;

        if (type == Receipt.TYPE_MEITUAN) {
            datePattern = Receipt.DATE_PATTERN_1;
            timePattern = Receipt.TIME_PATTERN_5;
            shopPattern = "美团外卖";
            shopOffset = 1;
        } else if (type == Receipt.TYPE_ELE) {
            datePattern = Receipt.DATE_PATTERN_2;
            timePattern = Receipt.TIME_PATTERN_5;
            shopPattern = "饿了么订单|饿了么";
            shopOffset = 1;
        } else if (type == Receipt.TYPE_WALMART) {
            datePattern = Receipt.DATE_PATTERN_3;
            timePattern = Receipt.TIME_PATTERN_5;
            shopPattern = "本店受理";
            shopOffset = 1;
        } else if (type == Receipt.TYPE_OTHER) {
            datePattern = Receipt.DATE_PATTERN_4;
            timePattern = Receipt.TIME_PATTERN_6;
            shopPattern = "欢迎光临";
            shopOffset = 0;
            totalPriceOffset = 1;
        }

        //小票时间
        ReceiptBean.WordBean dateWord = match(bean, datePattern, 0);
        ReceiptBean.WordBean timeWord = match(bean, timePattern, 0);
        final String dateStr = dateWord != null ? match(datePattern, dateWord.words) : "";
        final String timeStr = timeWord != null ? match(timePattern, timeWord.words) : "";
        receipt.mTime = ((dateStr != null) ? dateStr + " " : "") + ((timeStr != null) ? timeStr : "");
        //店铺名
        ReceiptBean.WordBean shopBean = match(bean, shopPattern, shopOffset);
        receipt.mShop = shopBean == null ? "" : (type != Receipt.TYPE_OTHER ? shopBean.words : shopBean.words.substring(4));
        //总计金额
        ReceiptBean.WordBean totalPriceBean = match(bean, Receipt.TOTAL_PRICE_PATTERN, totalPriceOffset);
        final String priceTitle = totalPriceBean != null ? match(Receipt.TOTAL_PRICE_PATTERN, totalPriceBean.words) : "";
        final String totalPrice = totalPriceBean == null ? "" : priceTitle != null ? totalPriceBean.words.substring(priceTitle.length()) : totalPriceBean.words;
        if (!TextUtils.isEmpty(totalPrice)) {
            receipt.mActualPrice = matchNumber(totalPrice);
        }
        //商品
        if (type == Receipt.TYPE_MEITUAN || type == Receipt.TYPE_ELE) {
            List<ReceiptBean.WordBean> goodsTemp = matches(bean, Receipt.GOODS_COUNT_PATTERN);
            if (goodsTemp == null || goodsTemp.isEmpty()) {
                int start = bean.words_result.indexOf(match(bean, "号篮", 1));
                int end = bean.words_result.indexOf(match(bean, "单品折扣", -1));
                if (start > 0 && end < bean.words_result.size()) {
                    String goodsName = null;
                    int count = -1;
                    float price = -1;
                    int index = 0;
                    for (; start <= end; start++) {
                        ReceiptBean.WordBean word = bean.words_result.get(start);
                        if (index % 3 == 0) {
                            goodsName = word.words;
                        } else if (index % 3 == 1) {
                            String cntStr = match(Receipt.GOODS_COUNT_PATTERN, word.words);
                            if (!TextUtils.isEmpty(cntStr)) {
                                count = (int) matchNumber(cntStr);
                                count = count > 0 ? count : 1;
                            } else {
                                index++;
                                count = 1;
                                price = matchNumber(word.words);
                                price = price > 0 ? price : 0;

                                if (!TextUtils.isEmpty(goodsName) && count > 0 && price >= 0) {
                                    Receipt.Goods goods = receipt.new Goods();
                                    goods.mName = goodsName;
                                    goods.price = price;

                                    receipt.mGoods.put(goods, count);

                                    goodsName = null;
                                    count = -1;
                                    price = -1;
                                }
                            }
                        } else if (index % 3 == 2) {
                            price = matchNumber(word.words);
                            price = price > 0 ? price : 0;

                            if (!TextUtils.isEmpty(goodsName) && count > 0 && price >= 0) {
                                Receipt.Goods goods = receipt.new Goods();
                                goods.mName = goodsName;
                                goods.price = price;

                                receipt.mGoods.put(goods, count);

                                goodsName = null;
                                count = -1;
                                price = -1;
                            }
                        }

                        if (start == end && !TextUtils.isEmpty(goodsName)) {
                            Receipt.Goods goods = receipt.new Goods();
                            goods.mName = goodsName;
                            goods.price = price > 0 ? price : 0;

                            receipt.mGoods.put(goods, count > 0 ? count : 1);

                            goodsName = null;
                            count = -1;
                            price = -1;
                        }

                        index++;
                    }
                }
            }
            for (ReceiptBean.WordBean goodsCountWord : goodsTemp) {
                int index = bean.words_result.indexOf(goodsCountWord);
                if (index >= 1 && index <= bean.words_result.size() - 2) {
                    ReceiptBean.WordBean goodsNameWord = bean.words_result.get(index - 1);
                    ReceiptBean.WordBean goodsPriceWord = bean.words_result.get(index + 1);

                    Receipt.Goods goods = receipt.new Goods();
                    goods.mName = goodsNameWord.words;
                    goods.price = matchNumber(goodsPriceWord.words);
                    int count = Integer.parseInt(goodsCountWord.words.substring(1));

                    receipt.mGoods.put(goods, count);
                }
            }
        } else if (type == Receipt.TYPE_WALMART) {
            int start = bean.words_result.indexOf(match(bean, "商店[0-9]*店员[0-9]*", 1));
            int end = bean.words_result.indexOf(totalPriceBean) - 1;

            if (start > 0 && end < bean.words_result.size()) {
                for (; start <= end; start++) {
                    ReceiptBean.WordBean word = bean.words_result.get(start);
                    String serial = match("[0-9]{10,}", word.words);
                    if (!TextUtils.isEmpty(serial)) {
                        Receipt.Goods goods = receipt.new Goods();
                        goods.price = matchNumber(bean.words_result.get(start + 1).words);
                        if (serial.equals(word.words)) {
                            goods.mName = bean.words_result.get(start - 1).words;
                        } else {
                            goods.mName = word.words.substring(0, word.words.indexOf(serial));
                        }
                        receipt.mGoods.put(goods, 1);
                    }
                }
            }
        } else if (type == Receipt.TYPE_OTHER) {
            for (ReceiptBean.WordBean titleBean : matches(bean, "商品号[0-9]*")) {
                ReceiptBean.WordBean goodsName = bean.words_result.get(bean.words_result.indexOf(titleBean) + 1);
                ReceiptBean.WordBean goodsPrice = bean.words_result.get(bean.words_result.indexOf(titleBean) + 2);

                Receipt.Goods goods = receipt.new Goods();
                goods.mName = goodsName.words;
                goods.price = matchNumber(goodsPrice.words);

                receipt.mGoods.put(goods, 1);
            }
        }

        if (receipt.mGoods.size() > 0) {
            int count = 0;
            float price = 0;
            for (Receipt.Goods goods : receipt.mGoods.keySet()) {
                int cnt = receipt.mGoods.get(goods);
                float pce = goods.price;
                if (cnt > 0 && pce > 0) {
                    count += cnt;
                    price += pce * cnt;
                }
            }

            receipt.mTotalPrice = price;
            receipt.mTotalGoods = count;
        }

        return receipt;
    }

    private ReceiptBean.WordBean match(ReceiptBean bean, String pattern, int offset) {
        if (bean.words_result_num > 0) {
            Pattern ptn = Pattern.compile(pattern);
            for (ReceiptBean.WordBean word : bean.words_result) {
                Matcher matcher = ptn.matcher(word.words);
                if (matcher.find()) {
                    int matchIndex = bean.words_result.indexOf(word);
                    if (matchIndex + offset >= 0 && matchIndex + offset < bean.words_result.size()) {
                        return bean.words_result.get(matchIndex + offset);
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private List<ReceiptBean.WordBean> matches(ReceiptBean bean, String pattern) {
        List<ReceiptBean.WordBean> beans = new ArrayList<>();
        if (bean.words_result_num > 0) {
            Pattern ptn = Pattern.compile(pattern);
            for (ReceiptBean.WordBean word : bean.words_result) {
                Matcher matcher = ptn.matcher(word.words);
                if (matcher.find()) {
                    beans.add(word);
                }
            }
        }
        return beans;
    }

    private String match(String pattern, String input) {
        Pattern ptn = Pattern.compile(pattern);
        Matcher matcher = ptn.matcher(input);
        if (matcher.find()) {
            return input.substring(matcher.start(), matcher.end());
        }

        return null;
    }

    private float matchNumber(String input) {
        if (TextUtils.isEmpty(input)) {
            return -1;
        }
        Pattern ptn = Pattern.compile("^[0-9]*[0-9]?(\\.[0-9]{1,2})?$");
        Matcher mth = ptn.matcher(input);
        if (mth.find()) {
            return Float.parseFloat(input.substring(mth.start(), mth.end()));
        }

        return -1;
    }
}
