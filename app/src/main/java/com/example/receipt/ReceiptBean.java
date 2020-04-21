package com.example.receipt;

import java.util.List;

public class ReceiptBean {

    public int type;

    public long log_id;

    public int words_result_num;

    public List<WordBean> words_result;

    class WordBean {

        public Location location;

        public String words;

        class Location {
            int width;
            int height;
            int top;
            int left;
        }
    }
}
