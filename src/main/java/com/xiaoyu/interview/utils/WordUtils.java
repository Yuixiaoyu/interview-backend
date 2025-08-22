package com.xiaoyu.interview.utils;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WordUtils {

    public static List<String> extractParagraphs(InputStream docStream) throws IOException {
        List<String> paragraphs = new ArrayList<>();
        XWPFDocument doc = new XWPFDocument(docStream);
        for (XWPFParagraph para : doc.getParagraphs()) {
            String text = para.getText().trim();
            if(!text.isEmpty()) paragraphs.add(text);
        }
        doc.close();
        return paragraphs;
    }
}
